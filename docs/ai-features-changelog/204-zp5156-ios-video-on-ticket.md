# 204 — ZP-5156: step-by-step iOS video for the developer, added to the ticket (3 Nov 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-5156 add video for this too. try to add video in such a way that devloper can reproduce this and add that video in ticket too for ios"

## Recording
- **Device:** iPhone 17 simulator (B9AA2053), iOS 1.74 Debug-QA (2 Nov build). The DEBUG mock camera ran with `EG_MOCK_FLIR_ROLL` = `Documents/zp5156-roll`.
- **New visuals:** FLIR7432 and FLIR7442, made with `scripts/flir_mock_roll.py visual`.
- **Setup fix:**
  - The work order "QA-DEMO ZP-5135 FLIR-SEP iOS 1.74 2 Nov" had IR Photo Type **FOTRIC-SEP**; the ticket's finding 3 steps switch it.
  - It was set back to FLIR-SEP in the app, and the app was reopened so the mock connected.
  - "Mock FLIR E96 · Connected" shows on the camera card inside Add IR Photos. The work order Details card says Not Connected.
- **Take 1 (19:22–19:31, not used):**
  - "mobile: hideKeyboard" pressed the keyboard tick, which adds the pair, so 2 pairs were staged. They were discarded with Cancel › Discard, and nothing was saved.
  - The redo (FLIR7431 + FLIR7432) showed the empty tile only in the open sheet. The IR tab, first opened *after* the Link, showed the picture straight away.
- **Take 2 (19:34–19:44, the video):** FLIR7441 + FLIR7442, done in this morning's order.
  1. Add the pair, then open the IR tab before linking.
  2. Browse Camera › Link at 19:38.
  3. Sheet row: empty. IR tab: empty. Refresh at 19:40 and 19:41: still empty.
  4. Close and reopen the app (19:43): the picture shows.
- **Finding:** only a screen that showed the row before the Link stays empty. A newly opened sheet or tab shows the picture.
  - This fits the possible cause already in the ticket: IRPhotoThumbnail keeps its "not found" state.
- **Video:** `docs/bug-evidence/2026-11-03-zp5156/QA-03Nov-ZP-5156-ios-video.mp4`, 9:55, 5.3 MB.
  - Each step is captioned. Red boxes mark the empty tile (sheet, IR tab, both Refreshes), and a green box marks the picture after reopening.
  - Caption times come from the step log and were checked against frames. Two boundaries were moved, because the tab opened about 13 s later than logged.

## Jira ZP-5156 (Abhiyant's account, REST v2 wiki, preview 0 errors, read back)
- **Attached:** `QA-03Nov-ZP-5156-ios-video.mp4` and `QA-03Nov-ZP-5156-repro-pack-v2.zip`. The HOW-TO is v2 and adds "open the IR tab once before linking" and the FLIR-SEP check.
- **Description, section 1:** adds the screen-recorded re-test and the "important for reproducing" note.
- **Description, steps:**
  - Step 1: check that IR Photo Type is FLIR-SEP.
  - Finding 1: rewritten as sub-steps, with the IR-tab-first step, "Add IR Photo Pair *or* the keyboard tick, not both", and a pointer to the v2 zip and the video.
- **Comment 45834 (How to reproduce):** the same step changes.
- **New comment 45926:** the embedded video (the player works, 9:55), a timeline table, and the reproduce note.
- **Left for the owner** (a delete is permanent): the old `QA-03Nov-ZP-5156-repro-pack.zip`, now replaced by v2, plus the earlier items in changelog 197.

## Test data left on QA
On WO "QA-DEMO ZP-5135 FLIR-SEP iOS 1.74 2 Nov" › QA-DEMO ZP-4874 SEP asset 27 Oct:
- new rows FLIR7431/7432 and FLIR7441/7442;
- IR Photo Type is back to FLIR-SEP.

FLIR7421/7422 is still free for the developer.
