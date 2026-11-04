# 208 — ZP-5202: Android Forms opens the first form, with no form list or visible Add Form (4 Nov 2026)

**Prompt:** "the issue is thier for android". This confirms the one difference from the 207 re-check is the issue Mukul meant.

## What was done
1. **Recorded the Android repro** on `quacky_test`, Android 1.39 (53):
   - Captured with `adb emu screenrecord`; 16:28–16:31 IST, 148 s.
   - Script: `scripts/and_forms_demo.py`. Each result was logged only after reading the screen.
2. **Logged results** (`forms-steps.txt`):
   - The asset sheet shows Infrared / Forms / Full Asset Page.
   - Forms opens Edit Form on the first form. The chip row shows `[De-Energized Visual Inspection, Demo EG form]`, and Add Form is not on screen.
   - After 2 swipes, the chips are `[…, test kd, Add Form]`.
   - Tapping + opens the Add Form sheet (Procedure / Standalone Form).
   - Long-press › `Forms (3)` gives the same result: the first form opens, with no Add Form on screen.
3. **Built the video** with `scripts/bug-video` (render.swift + build.py):
   - Caption times were set from frame contact sheets, not from the log.
   - Red boxes mark the chip row and the +.
   - An iOS 1.74 control is appended: the "Select a Form" and "Add Form › Standalone" screenshots from 16:07–16:09, in green.
   - Joined with the concat filter and `setsar=1`.
   - Result: 2:43, 1.16 MB.
4. **Filed ZP-5202** from Abhiyant's account (accountId checked first):
   - Created with REST v2 wiki: Bug, labels `Android`, sprint Triage 778, assignee Abhiyant, no fixVersion. It was created in Backlog and moved to To Do with transition 2.
   - Attached 4 files: the video and 3 jpgs.
   - Re-PUT the description so the embeds resolve.
   - Checked with `/rest/api/1.0/render`: 0 errors, 7 imgs, video present.
   - Read the issue back: all fields correct.
5. **Artifact v4:** https://claude.ai/artifact/JofrZkn1JDC2hQmgUhSd2Q now shows the "Android bug · ZP-5202" verdict and the link.
6. **Duplicate check:** none found.
   - ZP-5154 (iOS, single form, no Add Form) is related.
   - ZP-4308 (Android, Add Form missing after creating an asset with multiple services) is related and Done.

## Test data
- The active work order was switched to `Job - Nov 2, 6:28 PM` for the recording, then back to `QA-DEMO ZP-5135` on Android. Both devices show ZP-5135 active.
- Nothing was added or saved.

## Coordination
- From 16:21 IST, another session (jira-chrome-reconnect-tasks) holds the iOS simulator and `/tmp/ios170-lane-A.lock` for the iOS 1.75 re-checks.
- My lock was deleted at 16:19:48, before theirs.
- I ended my lingering Appium session 5707f7e3, so the iOS control in the video uses the 1.74 screenshots taken before the 1.75 install.

## Files
`docs/bug-evidence/2026-11-04-android-forms-no-list/` holds:
- the video;
- 3 jpgs;
- `scripts/`: the demo script, the step log, and both caption specs.
