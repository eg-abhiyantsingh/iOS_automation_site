# 192 — App v1.70 check (device build, not bumped) + ZP-4746 Doubt 5 screenshots

**Date:** 2026-10-19, ~16:30–16:55 IST
**Code changes:** none (no Java, workflow or app-zip changes)

## 1. "Check I have shared you latest .app file 1.70"

| | `~/Downloads/Z Platform-QA.app` | CI `apps/Z-Platform-QA.zip` |
|---|---|---|
| Version | 1.70 | 1.69 (commit 084942b) |
| Built | 2026-10-19 15:36 IST (re-signed 15:49) | 2026-10-05 |
| Platform (`DTPlatformName`) | **iphoneos** (SDK iphoneos26.4, Xcode 26.4) | iphonesimulator (SDK 18.5) |
| Mach-O build platform (`vtool`) | IOS | IOSSIMULATOR |

**Decision: CI was NOT bumped to v1.70.** Every CI job and every local run installs the
app on an iOS **simulator**. An `iphoneos` (real-device) build cannot be installed on a
simulator, so every job would fail at the install step. Earlier deliveries (v1.63/1.67/1.69)
were all simulator builds.

**Side effect to know:** local runs read `AppConstants.APP_PATH` =
`~/Downloads/Z Platform-QA.app`, which is now the device build, so local installs will fail
until the v1.70 **simulator** build is placed there (or `APP_PATH` is pointed at the v1.69
app extracted from the CI zip).

**Next step:** get the v1.70 *iphonesimulator* build, then the usual bump:
`ditto -c -k --norsrc --keepParent "Z Platform-QA.app" apps/Z-Platform-QA.zip` and commit
`chore(app): update Z Platform-QA to v1.70 for CI (2026-10-19 QA build)`.

Memory `feedback_always_latest_app_version` now says to check `DTPlatformName` before any bump.

## 2. "In doubt 5 add more screenshot" (QA group = Google Chat space eg-pz-qa)

- Doubt 5 = **ZP-4746**: an arc-flash label number (`AF_LabelNumber`) that comes in with an
  SKM import is not kept on SKM export. It already had 1 image (16 Oct).
- Today's live retest by the web session saved 9 annotated screenshots in
  `Egalvanic_Web-main/docs/bug-evidence/2026-10-19-zp4746-live/`.
- Posted as a **thread reply under Doubt 5** (Google Chat cannot add images by editing a
  message), from Abhiyant's account (verified before posting). 7 images, renamed
  `QA-19Oct-ZP-4746-1…7-*.jpg`:
  1–4 import a file whose bus has label `QA-DEMO-4746-PRESET-R9` → import done →
  export with "Generate arc-flash label numbers from location" ON → "Before you export"
  acknowledged; 5 exported bus has no label (control bus in the same file does);
  6 imported asset has no label field (the import drops it); 7 clean single-bus test with
  no errors still loses the label.
- Left out: #7 (export not blocked by errors) and #8 (clean-test import done), because the
  clean-test image already shows the errors are not the cause.
- **Follow-up (user: "instead of reply add that screenshot in that doubt only"):** Google
  Chat's *Edit message* composer DOES have its own "Upload file" input, so the 7 images were
  added to the Doubt 5 message itself (Edit → upload → Update). Text unchanged; Doubt 5 now
  carries 8 images (16 Oct + 7 × 19 Oct), verified after saving. The earlier thread reply
  still exists. Deleting a message is permanent, so the user removes it themselves
  (hover the reply → More actions → Delete).

## 3. Web v2.4 Module Tracker refreshed (same session, ~17:30 IST)
- "check this is updated or not? dharmesh sir just want to see module wise no need to add ticket number etc"
- It was not updated (still "Jira as of 14 Oct"). Rebuilt from Jira (19 Oct) as a module-wise page with no ticket
  numbers and republished to the same link: https://claude.ai/artifact/WZu2omtQJGAZbeN43iiPS3 (50 of 129 done).
- Full notes: `Egalvanic_Web-main/docs/changelogs/2026-10-19-v24-module-tracker-module-wise.md`.
- Follow-up: tracker also delivered as Excel (`~/Downloads/Web v2.4 Module Tracker.xlsx`, formula-driven Summary +
  Items tab) and as a Google Sheet (https://docs.google.com/spreadsheets/d/1XHH1QcV05LLkbhsUp74dAzIciCjZI84LAsubdAJ8KmI/edit).
- Refresh 2 (~20:15 IST): Jira moved 18 items (16 → Ready for QA); page, Excel and Google Sheet all now read
  50 done / 45 problems / 22 waiting / 7 in testing / 5 not delivered. Google Sheet edited in place (same link).
