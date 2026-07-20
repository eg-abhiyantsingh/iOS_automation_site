# 138 — App v1.51 check: breadth smoke + Issues priority remap + Critical level

**Prompt (2026-07-20):** "i have updated the qa app check" — new build at
`~/Downloads/Z Platform-QA.app` is **v1.51** (was 1.50; same bundle id).

## Breadth-first smoke on v1.51 (local sim, one test at a time)
| Surface | Test | Result |
|---|---|---|
| Assets list cell composite + AF details form | TC_AF_133 transformer | PASS (contract unchanged) |
| Class picker + verified readback | BUS_EAD_01 | PASS ("readback confirmed") |
| Issues list/header | TC_ISS_001 | PASS |
| Connections nav | TC_CONN_001 | PASS |
| Work Orders reachable | TC_WOP_001 | PASS |
| SiteVisit dashboard | TC_JOB_001 | PASS |
| Issues DETAILS priority | TC_ISS_038 | **FAILED → fixed → PASS** |
| Issues details status / class sheets | TC_ISS_055, TC_ISS_059 | PASS (v1.50 sheet primitives survived) |

## v1.51 Issue Details redesign (probed live)
- New anatomy: hero header (icon + title + status chip), then an "Issue Details"
  CARD of inline rows — `'<Label>' StaticText + icon/value ~30px below +
  chevron.down` for Status/Priority/Issue Class + Title text field — plus a new
  **"Safety & Notification"** card (Immediate Hazard, Customer Notified — Yes/No
  segmented buttons).
- Row tap opens a **titled bottom sheet**: NavigationBar named after the row
  ('Priority', y≈446) + Cancel + full-width option Buttons.
- **Priority options are now Critical / High / Medium / Low** — new 'Critical'
  level; element-clicks on the row's value text do NOT fire the SwiftUI row
  (W3C coordinate tap on the row does).

## Fixes (`IssuePage`)
- `selectPriority` remapped: primary path = verified v1.50 sheet primitives
  (`openDetailsRowSheet("Priority")` + `pressSheetOptionButton`), which match the
  v1.51 sheet exactly.
- **Root defect killed:** every legacy open-strategy set `pickerOpened = true`
  WITHOUT verifying — the select loop then hunted options on a sheetless screen
  (exact TC_ISS_038 failure). All strategies now verify via new
  `isChoiceSurfaceOpenNow()` (Cancel below y=300 / MenuItem / Sheet Grabber).

## New coverage
- **TC_ISS_238_verifySelectingCriticalPriority** — the v1.51 'Critical' level;
  PASS live (readback 'Critical').

## Validation
Compile PASS; verifier self-tests 34/34; live PASS: ISS_038/039/040/238 (priority
family), ISS_055/059 (status/class sheets), plus the 6-module breadth table above.
Recommend a full `run_all` CI dispatch once the CI runners' app artifact is
updated to v1.51 — local sim used the new build; CI validates at scale.
