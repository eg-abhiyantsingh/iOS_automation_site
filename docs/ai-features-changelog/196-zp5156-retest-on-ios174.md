# 196 — ZP-5156 re-test on iOS 1.74 (3 Nov 2026)

**Prompt:** "test this ticket is valid or not. if valid then tell me how i can reproduce so share me the picture so
that i can uploaded it on camera and tell devloper to upload."

**Ticket:** ZP-5156 "[iOS] FLIR camera import: Add Last Photo offers a visual-only file and does nothing; a visual linked
from the camera shows only after reopening the app (plus 2 small items)". To Do, assigned Kush.

## Build and setup
- App: Z Platform-QA **1.74** (Debug-QA, built 2 Nov 12:57 IST). Same build as the CI zip `c92f24a`; developer repo
  `release/qa` has no commit after 2 Nov 06:04 UTC and no branch or PR for ZP-5156.
- Simulator "iPhone 17 - ZP 1.70" (B9AA2053), iOS 26.2, signed in to QA acme. Mock camera:
  `EG_MOCK_FLIR_ROLL=<app Documents>/zp5156-roll` with FLIR7373 (thermal), FLIR7374 (visual), FLIR7377 (thermal),
  FLIR7380 (visual, nothing before it) from the ticket's zip; FLIR7412 (visual) added for finding 2.
- Work order "QA-DEMO ZP-5135 FLIR-SEP iOS 1.74 2 Nov", asset "QA-DEMO ZP-4874 SEP asset 27 Oct" (room Test).
  The camera path is asset long-press › Add IR Photos (the IR tab's + only offers the Files paths).

## Results
| Finding (ticket description order) | Result | Evidence |
|---|---|---|
| 1. Add Last Photo offers a lone visual and "does nothing, no message" | **Not reproduced.** 4 of 4 taps showed the toast "FLIR7380 not added. No IR photo is waiting for this visual." (also with FLIR7379 typed in the field, as in the ticket screenshot). Offering the newest file on FLIR-SEP is documented in the code (`FLIRCameraCaptureStrip.lastPhotoImages`). | `f1-*.png`, toast in page source |
| 2. Visual linked from Browse Camera shows only after reopening the app | **Reproduced.** Typed pair FLIR7411 + FLIR7412, Save, Browse Camera, tick only FLIR7412, Link (12:18 IST). VIS tile empty in the sheet, on the IR tab, after Refresh (12:19) and a second Refresh (12:21). After terminate + relaunch (12:25) the picture shows. | `ZP-5156-finding2-visual-needs-app-restart.png` |
| 3. Marker names 1–6 on iOS vs Sp1/Bx2 on web | Design question. iOS numbers every shape in one sequence on purpose ("ACE numbers every shape in one sequence", `ThermalEditorModels.swift`). | source |
| 4. Name hint FLIR0041 on FOTRIC work orders | **Valid (wording).** `IRPhotosTab.swift` hard-codes `irPlaceholder = "FLIR0041"` / `visualPlaceholder = "FLIR0041_visual"` for every brand. | source |

Lead for the developer on finding 2 (not verified): `IRPhotoThumbnail` keeps `isNotFound` / `loadFailed` in view state
and only resets on a cache-key or presigned-URL change; linking a visual to an existing visual key re-uses the same key,
so the empty state from before the upload may stick until the view state is rebuilt by a relaunch.

## Deliverable
`ZP-5156-repro-pack.zip`: `photos-for-the-camera/FLIR7422.jpg` (an E96-style visual), `HOW-TO-REPRODUCE.txt` (real
camera and simulator steps, pair FLIR7421 + FLIR7422), and the two screenshots.

## Repo change
- `scripts/flir_mock_roll.py visual <thermal.jpg> <out.jpg>`: makes one E96-style visual with any name (the
  visual key a tester typed). Verified: output is byte-identical to the ticket's FLIR7376 and `info` reads VISUAL.
- `docs/thermal-camera-test-setup.md`: one line for the new command.

## Test data left on QA
Row FLIR7411 + FLIR7412 on the ZP-5135 work order (asset count 11 → 12). Kept as evidence beside the 2 Nov rows.
