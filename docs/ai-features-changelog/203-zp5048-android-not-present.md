# 203 — ZP-5048 checked on Android: not present (3 Nov 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-5048 check for android too"

## Setup
- **Build:** Android QA app **1.38.1 (52)**, the newest Firebase build (email of 2 Nov 16:17 IST), installed 2 Nov.
- **Emulator:** `quacky_test`, Appium 4725.
  - The iOS simulator was shut down first to free memory (swap 14.4 / 15.4 GB).
  - The cold boot stopped on the secure lock screen. The owner unlocked it; no PIN was typed by automation.
- **Mode:** the app was in offline mode ("Go Online"). It was switched online before the test.
- **Debug channel:** the Android SLD web view is debuggable (`webview_devtools_remote_<pid>`).
  - `adb forward tcp:9333 localabstract:webview_devtools_remote_<pid>` plus a small DevTools client (`cdp.js`) reads the GoJS diagram.
  - The same client records `window.updateData`, as on iOS.

## Android route
- On Android a view opens full screen with no tab bar, so the Assets tab can't be reached while the view is open.
- The asset is therefore edited from inside the view: long-press › Edit › Asset Name › Save Changes, which returns to the view.

## Result: NOT PRESENT on either site
| Site / view / asset | View opened | After asset Save | updateData after Save | SLD Refresh |
|---|---|---|---|---|
| QA-DEMO ZP-4800 / "QA-DEMO ZP-4816 view" / QA PNL1 q | 12 / 10, 2 bends, 1 hidden, badge 1 | 12 / 10, bends + hidden kept, badge 1 | 10 nodes / 8 edges, view points on 3 | 12 / 10 |
| Android Site 2 / "new 1" / Panelboard-2 | 11 / 5, badge 1 | 11 / 5, badge 1 | 11 nodes / 5 edges, view points on 5 | 11 / 5 |

On iOS the same refresh after Save sends the whole site: 131 / 60 and 568 / 198, with view points on 0.

## Video and cleanup
- **Videos:** `adb emu screenrecord start` (webm, continuous frames), rebuilt with `vfr2cfr.py`. Captions and green boxes were added with the bug-video tools, and the frames were checked at every result.
- **Files:** `docs/bug-evidence/2026-11-03-zp5048/android/` holds 2 videos, 5 screenshots and `scripts/` (demo, cdp, state JS, caption specs).
- **Names put back:** "QA PNL1 q" and "Panelboard-2" (read from the canvas after cleanup).
- **Final state:** the app is back on (s) Wild Goose Brewery, and the emulator was shut down afterwards.
- **Jira:** nothing was changed. The ZP-5048 comment draft now has an Android line.
