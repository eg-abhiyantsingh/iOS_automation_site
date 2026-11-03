# Bug video: simulator recording with step captions and red/green highlight boxes

Use this to make a bug video for a developer: the real screen recording, a caption band on top (step + result) and
boxes drawn around the wrong part. It was first used for ZP-5048 on 3 Nov 2026; see
`docs/bug-evidence/2026-11-03-zp5048/`.

## 1. Record
```
xcrun simctl io <udid> recordVideo --codec=h264 --force raw.mp4 &   # note the PID
# ...run the steps slowly (5-8 s pause on every result screen), log each step with its time
kill -INT <pid>
```

## 2. Rebuild at 24 fps from each frame's real time
`simctl` writes a variable-frame-rate file with odd timestamps:
- Some frames carry a decode/"best effort" time about 11 s off.
- ffmpeg's `fps` filter and `-r` then reorder or stretch the timeline.

Only the frame **pts_time** matches the clock, and `vfr2cfr.py` uses it:
```
python3 -I vfr2cfr.py raw.mp4 cfr.mp4 /tmp/frames 590 1282 ["127.9:+6"]   # optional: hold a frame 6 s longer
```
Use the optional hold when the recorder dropped frames during a still screen and a result flashes by too quickly.

## 3. Find where each state is on screen
```
python3 -I classify.py cfr.mp4 590 1282 <t of a view frame> <t of an All Assets frame>
```
It prints runs of OK / BUG / ALL / DIM (a sheet is open) / OTHER, using two signals:
- the badge colour: orange means the warning badge (correct view), red means the bug count;
- the header title, compared with the two reference frames.

Use the runs to set the caption times. Do not take them from the step log.

## 4. Captions and boxes
Write a spec (example: `docs/bug-evidence/2026-11-03-zp5048/scripts/video-site1-captions.json`):
- `segments`: each has `[a, b)` in seconds, `step`, `result`, `color` (green or red) and `boxes`.
- Box coordinates are in points of the 402 × 874 pt screen. Take them from a screenshot of that state.

Then:
```
mkdir ov && swift render.swift spec.json ov && python3 -I build.py spec.json cfr.mp4 out.mp4 ov
```
`render.swift` draws one full-canvas PNG per segment: a white band with the title, step and result, plus the boxes.
`build.py` pads the band onto the video and overlays each PNG over `[a, b)`.

The local ffmpeg has no drawtext or subtitles filter, which is why the captions are drawn as images.

Check the result by pulling frames with `-vf "select='gte(t,T)'"`. Don't trust `-ss` seeking alone.
