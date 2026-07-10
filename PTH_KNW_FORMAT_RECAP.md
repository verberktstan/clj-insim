# `.pth` and `.knw` Format Recap

Follow-up to [AI_TAKEOVER_RECAP.md](AI_TAKEOVER_RECAP.md). That doc covered taking control of an AI
car via `IS_AIC`/`IS_AII` telemetry. This one covers the two on-disk formats that describe the
track itself and LFS's own AI knowledge about driving it, and whether they can sharpen the earlier
"predict braking before it happens" plan.

Samples used: `resources/BL1.pth` (Blackwood GP, forward layout) and `resources/BL1_XFG.knw`
(AI knowledge for the XFG on that same track/layout).

## Correction from the earlier chat pass

Earlier in this conversation I claimed the `.knw` header's `(402, 24)` field pair was "confirmed"
by matching a `.pth` header field at file offset 16. That match was a **coincidence, not a real
field** — offset 16 in `BL1.pth` actually falls inside path node 0's position data (see below), not
inside a "node count" header field. The real header is only 12 bytes, and node 0's data happens to
contain a value that read as 402 by luck. The `(402, 24)` pair in `.knw` is still real (see below),
just not validated the way I originally said — it's validated below by a much stronger method
(geometric consistency), not a numeric coincidence.

## `.pth` — track path

Not officially documented in byte detail either, but broadly covered by community write-ups (e.g.
the [LFS Manual wiki](https://en.lfsmanual.net/wiki/File_Formats)). Rather than trust that
transcription, I derived the real layout empirically and it disagreed with the wiki summary on
header size — here's what's actually verified against the file:

**Method:** brute-force every plausible `(header size, node size)` pair, decode `position` and
`direction` at each node, and score how well `direction[i]` aligns (cosine similarity) with the
position delta to `node[i+1]`. A real path's stored tangent vector should closely match the
direction you'd actually travel between consecutive nodes. The correct layout scores ~1.0; wrong
guesses score near 0.

**Result:** header = **12 bytes**, node = **44 bytes**, average alignment **0.996** across 548
consecutive node pairs (549 nodes total for `BL1.pth`).

```
Header (12 bytes):
  0   6   char   magic       "SRPATH"
  6   1   byte   version
  7   1   byte   revision
  8   4   int32  flags       (unresolved)

Node (44 bytes, repeated (filesize - 12) / 44 times):
  0   12  3×int32  position    fixed-point, 1 metre = 65536
  12  12  3×float  direction   unit tangent vector at this node
  24  20  ?        tail        unresolved (presumed: node flags + track width limits)
```

Node count isn't (yet) a field we've located in the header — it's derived from file size. Axis
labels (which int32 is "X" vs "Y" vs "Z") are unconfirmed; the geometric check is invariant to axis
permutation, so curvature/distance math built on this doesn't need that resolved, but don't trust
printed X/Y/Z labels as ground-truth world axes yet.

## `.knw` — AI knowledge

Not publicly documented at all — general descriptions online just say it's "AI knowledge, generated
per car per track" (matches what you described: `BL1_XFG.knw` = XFG-specific racing line on BL1).
No byte-level spec exists that I could find. Layout below is reverse-engineered from this one file,
cross-checked against `BL1.pth`.

```
Header (36 bytes):
  0   6   char    magic            "LFSKNW"
  6   1   byte    version          0
  7   1   byte    revision         7
  8   4   int32   unknown-1        65536 (= 1.0 in Q16.16 fixed point - purpose unclear)
  12  4   int32   unknown-2        104785
  16  4   float   unknown-3        0.4934
  20  4   float   top-speed-hyp    52.06  - plausible: XFG top speed in m/s (187 km/h - fits a
                                             low-power hot-hatch on Blackwood)
  24  4   float   unknown-4        97.78
  28  4   float   unknown-5        45.0   - suspiciously round; maybe a skill or lap-count param
  32  2   uint16  path-node-count  402
  34  2   uint16  segment-count    24     - matches the actual record count below

Segment record (24 bytes x segment-count):
  0   4   int32   reserved      always 0 in this sample
  4   4   int32   flags         bit-packed, semantics unresolved
  8   4   int32   start-node    path-node-array index
  12  4   int32   end-node      path-node-array index
  16  4   float   curvature-hyp hypothesis: segment curvature - NOT confirmed, see below
  20  4   float   unknown       repeats similar values (-1.5x, -0.4, -1.0x) across dissimilar
                                 corners; possibly a quantized camber/banking bucket
```

**Segment chaining is solid:** record `i`'s `end-node` equals record `i+1`'s `start-node`, forming
a closed loop across all 24 records (last record's end wraps to the first record's start). So LFS
has divided BL1's lap into 24 driving segments (roughly one per corner + the straight after it).
This is a hard finding, not a hypothesis - it's just arithmetic chaining across the real byte
values.

**`curvature-hyp` is not confirmed.** I tested it directly: computed real geometric curvature
(turn-angle between consecutive `.pth` node directions, divided by distance - a standard dθ/ds
curvature estimate) for every node, averaged it per segment's node range, and correlated against
`|curvature-hyp|`.

```
Pearson  |curvature-hyp| vs avg computed |curvature|:  0.337
Spearman |curvature-hyp| vs avg computed |curvature|:  0.195
Spearman |curvature-hyp| vs max computed |curvature|:  0.094
```

That's weak - not enough to say this field is curvature. Treat its meaning (and `unknown`'s) as
still open. Confirming or refuting further would need more samples: same car on a different track,
or a different car on the same track, to see whether the field varies the way a given hypothesis
predicts.

## Code

`src/clj_insim/formats/pth.clj` and `src/clj_insim/formats/knw.clj` implement the layouts above.
Both read via `java.nio.ByteBuffer` rather than the project's usual `marshal.core` codecs -
`marshal`'s `m/float` doesn't sign-mask the top byte before calling `Float/intBitsToFloat`, so it
throws `Value out of range for int` on any negative float, which this data has constantly (see
docstrings for detail). `src/clj_insim/formats/validate.clj` reproduces the curvature check above
(`clj-insim.formats.validate/report!`).

## Relevance to the braking-override plan

The segment table is directly useful even with `curvature-hyp` unconfirmed: it tells you *where the
corners are* (24 segment boundaries around the lap) independent of how tight each one is. Combined
with `.pth` node positions, you can map a live AI car's `IS_AII` position to "which segment is it in
/ how far to the next segment boundary" and use that as the predictive trigger for the brake
override, instead of (or alongside) the original reactive deceleration-threshold detector. Ranking
segments by tightness to prioritize which corners to target still needs a working curvature signal -
which this data doesn't yet give us with confidence.
