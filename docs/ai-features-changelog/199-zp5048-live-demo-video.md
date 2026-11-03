# 199 — ZP-5048 shown live on the simulator + screen recording (3 Nov 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-5048 show me live"

Ran the ticket's steps slowly on the visible simulator "iPhone 17 - ZP 1.70" (B9AA2053, iOS 26.2, app 1.74), with
`xcrun simctl io … recordVideo` running. Each step was posted to the chat as it happened (17:23–17:26 IST).
Site: QA-DEMO ZP-4800 SLD canvas v2.4. View: "QA-DEMO ZP-4816 view". Asset: QA PNL1 q.

| Time (IST) | Step | Result |
|---|---|---|
| 17:23:30 | View opened | 12 assets / 10 connections, bends on QA SWBD1 > QA CB2, 1 hidden connection, badge 1 (correct) |
| 17:24:21 | Asset renamed "QA PNL1 q ZP-5048", Save Changes | saved |
| 17:24:30 | Back to the SLD tab | **131 / 60, no bends, nothing hidden, badge 19.** `window.updateData` got 131 nodes / 60 edges with view-specific points on 0 (bug) |
| 17:24:57 | Same view picked again | 131 / 60, badge 19 (still the bug) |
| 17:25:30 | All Assets, then the view | 12 / 10, bends + hidden connection back, badge 1 (correct) |
| 17:26:30 | Asset name put back to "QA PNL1 q" | done |

The app's Views sheet itself says "Views open read-only on mobile and stay live". After an asset save, the view
no longer stays the view.

Video: `docs/bug-evidence/2026-11-03-zp5048/QA-03Nov-ZP-5048-live-demo-ios174.mp4`.
- 3 min 56 s, 590 px wide, 1.8 MB.
- Shrunk with ffmpeg from the 62 MB full-size recording.

Script: `docs/bug-evidence/2026-11-03-zp5048/scripts/demo.js` (lane A runner).

The app was left on the ZP-4800 site with the view correct, so the owner can repeat the steps by hand. No Jira change.
