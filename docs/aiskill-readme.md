# aiskill

Automatically ramps up AI drivers' `/aiset` skill level as they complete laps
or split points, based on a difficulty preset. Connects to a running Live For
Speed (LFS) instance via InSim.

## Requirements

- Java (JRE 11+) installed and on your `PATH`.
- LFS running with InSim enabled on port 29999. In LFS, run `/insim 29999`
  (or leave it at the default if already configured).

## Running

- **Mac/Linux**: run `run-aiskill.sh`
- **Windows**: double-click `run-aiskill.bat`

Both connect to `127.0.0.1:29999` by default. To connect elsewhere, pass a
host and/or port, e.g.:

```
./run-aiskill.sh 192.168.1.10 29999
```

Leave the window open while playing - closing it stops aiskill.

## Commands

Type these in the LFS chat:

- `!ai` - show the current difficulty, volatility and skill cap.
- `!ai <difficulty>` - set the AI difficulty. Choices: `easy`, `normal`, `hard`.
- `!ai <difficulty> <volatility>` - also set how often skill randomly
  shuffles instead of just increasing. Choices: `frequent`, `balanced`, `rare`.
- `!ai <difficulty> <skill cap>` - also cap the maximum skill level.
  Choices: `pro`, `advanced`, `intermediate`, `beginner`, `newbie`.

Difficulty, volatility and skill cap can be combined in any order, e.g.:

```
!ai hard rare advanced
```

Changing difficulty applies to every AI player, both already in the race and
joining later. In multiplayer, only admins can change it; in single-player,
anyone can.

## License

Copyright (c) Stan Verberkt ([github.com/verberktstan/clj-insim](https://github.com/verberktstan/clj-insim)).
Released into the public domain under the Unlicense - see `LICENSE`.