# 160 — App-under-test bumped to v1.57 for CI (2026-08-14)

## Prompt
"I have updated the app use that in our automation for ci cd"

## What changed
- `apps/Z-Platform-QA.zip` replaced: **v1.55 → v1.57** (packaged from the user's
  updated local bundle `/Users/abhiyantsingh/Downloads/Z Platform-QA.app`,
  modified 2026-08-14 18:17).

## Verification done before commit
- `CFBundleShortVersionString` = **1.57**, `CFBundleIdentifier` =
  `com.egalvanic.zplatform-QA` (unchanged — no `AppConstants` change needed;
  bundle id is auto-detected from Info.plist anyway).
- Binary is a Mach-O arm64 **iphonesimulator** build (149 MB app, 44.7 MB zip;
  prior v1.55 zip was 43.8 MB — comparable).
- Zip re-extracted in scratchpad: unpacks directly to `Z Platform-QA.app`
  (space-name), which is the exact path every workflow installs
  (`xcrun simctl install … "apps/Z Platform-QA.app"`); the hyphen-name rename
  fallback in the workflows is not needed. `__MACOSX` resource-fork folder
  stripped from the zip.

## How CI picks it up
- All iOS workflows (`ios-tests-parallel.yml`, `rerun-failed-by-date.yml`, …)
  unzip the committed `apps/Z-Platform-QA.zip` — no workflow edits required.
- `generate_bug_report.py` reads the app version from `git log -1 -- apps`,
  so the commit message carries the v1.57 marker.
- Local runs are automatic: `AppConstants.APP_PATH` defaults to the same
  Downloads bundle the user replaced.

## Watchlist for the first v1.57 run
- v1.56/v1.57 release deltas are unknown to the suite; prior minor bumps
  (v1.48 Issues DOM, v1.50 details sheets, v1.55 'Use library' gate) each
  introduced DOM-contract drift. Treat first-run failure clusters as possible
  remap work, per the verify-every-fail-locally loop.
