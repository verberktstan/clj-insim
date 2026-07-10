# Taking Over AI Cars via InSim — Recap

**Bottom line:** there is exactly one packet for this — `IS_AIC` (type 68). It lets you inject raw
control inputs into any AI-driven car by its `PLID`. There's no separate "take over" handshake; you
just start sending inputs and LFS applies them to that AI car every tick you send one.

## 1. The packet shape (`insim.txt:2515-2523`)

```
IS_AIC { Size, Type=68, ReqI, PLID, Inputs[<=20] }
AIInputVal { Input(byte), Time(byte, hundredths-sec), Value(word) }
```

You send an array of `(input, time, value)` triples in one packet. `Time` auto-releases the input
after that many hundredths of a second — no need for a second "release" packet for momentary
controls.

## 2. The input channels (0-19)

Steer(0), Throttle(1), Brake(2), ChangeUp(3), ChangeDown(4), Ignition(5), ExtraLight(6),
Headlights(7), Siren(8), Horn(9), Flash(10), Clutch(11), Handbrake(12), Indicators(13), Gear(14),
Look(15), PitSpeed(16), TCDisable(17), FogRear(18), FogFront(19).

- Steer/throttle/brake/clutch/handbrake are 0-65535 analog (steer: 1=hard left, 32768=centre,
  65535=hard right).
- "Toggle" ones (ignition, extralight, pitspeed, tcdisable, fog) take `1`=toggle / `2`=off /
  `3`=on.
- "Hold" ones (chup/chdn/siren/horn/flash) need `Time` set, or a follow-up packet with `Value=0`.

## 3. Special commands (in the `Input` byte, 240-255)

- `240` `CS_SEND_AI_INFO` — request one `IS_AII` telemetry reply.
- `241` `CS_REPEAT_AI_INFO` — stream telemetry, `Value`=interval (0=stop).
- `253` `CS_SET_HELP_FLAGS` — e.g. force auto-gears/auto-clutch on that AI.
- `254` `CS_RESET_INPUTS` — snap back to neutral (steer centre, gear=255/auto).
- `255` `CS_STOP_CONTROL` — car just stops (simplest full takeover kill switch).

## 4. Simplest concrete recipe

1. Get the `PLID` of the AI car (from `IS_NPL` on join, or `TINY_NPL` to list all players — human
   vs AI isn't flagged explicitly in the doc; you generally know because you spawned it as AI via
   `/ai add` or race setup).
2. Send `IS_AIC` with e.g. `Input=1 (throttle), Value=65535` -> full throttle, and
   `Input=0 (steer), Value=32768` -> straight, repeated each tick you want it driven.
3. To read back what it's doing, send `SMALL_AII` with `UVal=PLID` (or `CS_REPEAT_AI_INFO`) and
   parse `IS_AII` (position, velocity, heading, RPM, gear).

## 5. Already half-built in this repo

`clj-insim` already implements the packet: `packets/aic` in `src/clj_insim/packets.clj:21` builds
the header+body, `enum.clj` has `AI_CONTROL_INPUTS`/`AI_CONTROL_SPECIAL`, and codecs/parsing are
wired per `AI_PACKETS_PLAN.md` (only the receive-only `aii`/`small-aii` convenience constructors
were skipped as unnecessary). So the fastest path to actually taking over an AI car today is:

```clojure
(packets/aic {:player-id plid
              :inputs [{:ai-input/input 1 :ai-input/time 0 :ai-input/value 65535} ; full throttle
                       {:ai-input/input 0 :ai-input/time 0 :ai-input/value 32768}]}) ; steer centre
```

Then write/send that packet over the existing client. Repeat/update on each control tick for
continuous driving; send `Input=255 (CS_STOP_CONTROL)` to release it.
