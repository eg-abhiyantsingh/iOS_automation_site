# 200 — ZP-5048 videos with captions and highlight boxes; reusable bug-video tools (3 Nov 2026)

**Prompts:**
- "can you record also video of your testing is that possible because devloper ask me recording"
- "save in memory to send video next time for the ticket so developer can reproduce the bug easily"
- "and also try to highlight issue also in video if possible or simple video will also be good"

## Delivered
| File | Content |
|---|---|
| `docs/bug-evidence/2026-11-03-zp5048/QA-03Nov-ZP-5048-site1-video-highlighted.mp4` | Site QA-DEMO ZP-4800, view "QA-DEMO ZP-4816 view". Recorded 17:22–17:26 IST. 3 min 52 s, 2.3 MB. |
| `docs/bug-evidence/2026-11-03-zp5048/QA-03Nov-ZP-5048-site2-video-highlighted.mp4` | Android Site 2, view "new 1". Recorded 17:34–17:38 IST. 4 min, 1.9 MB. |

Each video has:
- a caption band with the step and its result (green = correct, red = BUG);
- green boxes on the correct state: badge 1, the bends, the hidden connection, the view's assets;
- red boxes on the bug: badge 19 or 32, and site assets that are not in the view.

| Step | Site 1 | Site 2 |
|---|---|---|
| View opened | correct (badge 1) | correct (badge 1) |
| After asset Save | 131 / 60, badge 19 | 568 / 198, badge 32 |
| Same view picked again | still wrong | still wrong |
| All Assets › view | correct again | correct again |

Asset names were set back: "QA PNL1 q" and "Panelboard-2". The app was switched back to "(s) Wild Goose Brewery".

## Tools added: `scripts/bug-video/` (README)
The simulator's `recordVideo` output has broken best-effort and decode timestamps, up to 11 s off. ffmpeg's `fps`
filter used them and reordered frames, so a Views sheet showed up twice in the first site-2 cut and the captions drifted.
- `vfr2cfr.py`: rebuilds the video at 24 fps from each frame's `pts_time`, with optional frame holds.
- `classify.py`: finds where the OK / BUG / ALL / DIM / OTHER states are on screen (badge colour + header title).
- `render.swift` + `build.py`: draw the caption band and the boxes, then overlay them over each time range.

Checks:
- Site 2, step 4: the recorder kept only part of that still screen, until frames were rebuilt from `pts_time`.
- Both final videos were checked at the step 1, 3, 4 and 5 results. Every caption and box matches the screen.

## Memory
- `feedback_record_video_for_tickets`: every ticket test now gets a recorded video sent to the owner. Highlight the
  issue when practical; a plain recording is acceptable. The note points to these tools.
- Jira upload of the videos still needs the owner's go-ahead.
