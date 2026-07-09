# 114 — local sim verification of restored eng-lib + autoAcceptAlerts alert-race fix

**Date:** 2026-07-09
**Prompt:** "check again in local run appium and check in simulator" (follow-up to changelog 113)

## Local verification (iPhone 17 Pro Max sim, iOS 26.2, app v1.49, Appium 3.1.2)

- **Equipment Library is live end-to-end with the restored flag:** Settings card
  enabled (no "isn't enabled for your company" caption), tap opens the
  "Load Device Library?" alert, Download runs the real device-library download
  (progress states observed on screen).
- Canary **TC_ENG_130 PASSED** (27s) after the framework fix below —
  flag⇄UI consistency formally verified in the flag-ON direction for the
  first time since the outage.

## Bug found in OUR framework (not the app): autoAcceptAlerts races alert polls

First canary run failed "'Load Device Library?' alert must appear" — yet the
failure screenshot showed the download RUNNING. Appium log line ~233: the alert
query returned **stale element** — WDA found the alert, then its auto-accept
monitor (from session capability `autoAcceptAlerts: true`, DriverManager L212)
pressed **Download** out from under the poll. The app behaved correctly; the
test raced WDA.

Timing-dependent: the same flows passed live 2026-07-06. Any historical
intermittent "alert never appeared" failure is suspect for this mechanism.

## Fix (4 files, compiled + canary-validated live)

- `AssetEngineerPage.withAlertsManual(Runnable)` — pauses WDA
  `defaultAlertAction` ("" during the sequence, restore "accept" in finally;
  settings are session-mutable, capabilities are not). Global capability
  untouched — post-Sign-In popup handling everywhere depends on it.
- `AssetEngineerPage.isDownloadInFlightNow()` — subtitle-based
  download-in-flight probe (defense-in-depth when pausing loses the race).
- `ensureLibraryDownloaded` — alert dance wrapped; auto-accepted download
  accepted as engagement instead of throwing "alert never appeared".
- Callers wrapped: canary TC_ENG_130 (with AUTO_ACCEPTED fallback that waits
  out the download), TC_ENG_002 (alert copy), TC_ENG_101 (open/cancel ×2),
  TC_ENG_104 (action contract).

## Triage note for CI run 29006089840

That run executes the PRE-fix code: any asset-engineer failures reading
"'Load Device Library?' alert ..." are THIS race, not app bugs — must be
re-run on the fixed code before appearing in any client-facing report.

## Local TC_ENG_003 (download end-to-end): PASSED (2m 8s)

Full download through the FIXED `ensureLibraryDownloaded` path — terminal
counts summary: `1,28,303 frames, 1,98,782 sensors, 28,486 trip units,
81,050 segments, 520 kVA entries, 16,101 cable / busway entries`.

**Local verdict: Equipment Library is fully operational on the restored
flag** — flag ⇄ UI ⇄ alert ⇄ download ⇄ persisted counts, all green.
