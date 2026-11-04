# 206 — IR Photo Type: iOS vs Android check, artifact, and ZP-5200 (4 Nov 2026)

**Prompt (Hinglish):** whenever an IR or energized-maintenance work order is done, the IR scan comes in several forms. iOS has a dropdown that shows FLIR and FOTRIC; Android has nothing like it. "share me artificat of this finding". Then: "but sir told that this issue was in android right?". Then, answering where the gap is: "share me finding in artificat with screenshot and video".

## Builds and data
- **iOS:** 1.74 Debug-QA (2 Nov) on the iPhone 17 simulator B9AA2053.
- **Android:** QA 1.38.1 (52) on the `quacky_test` emulator.
  - 1.39 (53) has been on Firebase since 3 Nov but is not installed.
  - The emulator was booted from a snapshot, unlocked; no PIN was typed.
- **Tenant:** QA, EG-ACME, site (s) Wild Goose Brewery.
- **No "Energized Maintenance" work type in QA:** none of the 28 services from `/api/procedures-v2/services` has that name. The maintenance case was checked on "Job - Nov 2, 6:28 PM" (Cleaning + IR Checklist + Engineering Data).

## Side-by-side result: the dropdown is NOT missing on Android
| Screen | iOS 1.74 | Android 1.38.1 |
|---|---|---|
| New Work Order › Photo Type | 5 options, default FLIR-IND | same |
| WO Details › Capture Settings › IR Photo Type (Infrared Thermography WO) | dropdown, 5 options | same |
| same row on Cleaning + IR Checklist WO | FOTRIC-SEP + FOTRIC camera | same |
| De-Energized Visual Inspection WO | no IR Photo Type | same |
| Asset page › Maintenance › + › Add IR Photo | By Photo Number / Via Camera | same |
| Capture Settings › Work Type row | shown, editable | not shown (label at top only) |

The 5 options are FLIR-IND, FLIR-SEP, FLUKE, FOTRIC-SEP and FOTRIC-IND.

## Android bug found: ZP-5200
**What happens:** the IR Photo Type changed on another device still shows the old type on Android after Refresh.

**Evidence:**
- **Before the check:** at 14:01 IST Android showed FOTRIC-SEP for ZP-5135. The server had FLIR-SEP since 3 Nov 19:15 IST. Closing and reopening the app fixed it.
- **Round 1:** at 14:18:54 the type was changed to FOTRIC-SEP on iOS. Two Android Refreshes still showed FLIR-SEP. Reopening the WO showed FOTRIC-SEP.
- **Round 2:** at 14:22:02 the type was set back to FLIR-SEP on iOS. An Android Refresh still showed FOTRIC-SEP. Reopening the WO showed FLIR-SEP.
- **Round 3:** reopening the WO twice did NOT help. Only the Work Orders list Refresh plus reopening the WO showed FLIR-SEP.
- **Reverse check:** a change made on Android shows on iOS after one Refresh, so iOS is fine.
- **Server values:** read at every step from the web's `POST /api/company/{id}/workorders/v2` (field `photo_type`).

**Jira ZP-5200** (Abhiyant's account, REST v2 wiki, 0 render errors, read back):
- Bug, To Do, Triage 778, assignee Abhiyant, label Android, no fixVersion.
- Attachments: the captioned Android video and 4 screenshots, all embedded.

## Artifact
https://claude.ai/artifact/JofrZkn1JDC2hQmgUhSd2Q ("IR Photo Type: iOS vs Android"). It is private until shared, and has:
- the comparison table;
- 4 side-by-side screenshot pairs;
- the bug timeline and screenshots;
- both videos.

## Videos
- **`QA-04Nov-iOS-IR-photo-type-dropdown.mp4`** (2:15): the IR WO with the dropdown open, the Cleaning + IR WO, and the non-IR WO.
- **`QA-04Nov-Android-IR-photo-type-stale-after-refresh.mp4`** (4:30):
  - Both rounds, with a still of the iOS change between them.
  - Two idle stretches were cut.
  - Caption times were set from brightness runs of the "Refreshing" overlay and checked against frames.

## Lessons
- **iOS 26 keyboard:** it has no hide key, so `mobile: hideKeyboard` does nothing on the WO search field. Pressing **return** on the empty search closes the keyboard.
- **Joining video parts:** concat with `-c copy` gave bad timestamps when a still-image clip was in the middle. Use the concat filter with `setsar=1`.

## Test data
- **ZP-5135:** its IR Photo Type was changed 4 times. It is back to FLIR-SEP (server 14:25:57 IST), and both apps show FLIR-SEP.
- **Active work order:** ZP-5135 is active again on both devices.
- **Nothing created:** the New Work Order forms were cancelled.

## Files
`docs/bug-evidence/2026-11-04-ir-photo-type/` holds:
- the videos;
- the screens/ folder;
- the Jira screenshots;
- the artifact HTML;
- the scripts/ folder (ios_demo.py, and_demo.py, ad.py adb helper, caption specs, step logs).
