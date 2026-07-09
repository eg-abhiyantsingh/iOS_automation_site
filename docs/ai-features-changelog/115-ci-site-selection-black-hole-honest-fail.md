# 115 — CI run 29006089840 triage: site-selection black hole → honest-fail hardening

**Date:** 2026-07-09
**Context:** first flag-on equipment-library CI run (changelog 113) came back
5 FAIL / 186 SKIP / 0 PASS. Triage + fix.

## What actually happened on CI (asset-engineer job, iOS 18.5)

- Login succeeded; `selectFirstSiteFast()` tapped "(s) Wild Goose Brewery";
  `waitForDashboardReady()` timed out after 120s; **BaseTest then printed
  "✅ Site selected and loaded" unconditionally** (the timeout only logged
  "⚠️ … continuing…").
- Failure screenshots prove the app never left the **Select Site** screen.
  Every test re-attempted selection (~2-6 min each), TC_ENG_010 hit its 360s
  timeout, WDA/driver init died (~09:28), and the remaining 186 tests skipped.
- NOT the autoAcceptAlerts race (changelog 114), NOT gap-class locator heat —
  the suite never reached Settings at all.
- Same account + same site + same app (v1.49 in `apps/Z-Platform-QA.zip` ==
  local build) logged in and selected the site successfully on the LOCAL sim
  at the same clock time → backend was healthy; blocker is CI-environment
  (iOS 18.5 sim state / tap-registration / slow site load >240s). Root cause
  on the app side still open — the new diagnostics will pin it next run.

## Fix (framework honesty — the exact pass-anyway pattern this repo hunts)

- `SiteSelectionPage.waitForDashboardReady()`: `void` → `boolean`
  (all existing callers compile unchanged; timeout now visible).
- `BaseTest.loginAndSelectSite()`:
  - retry the site tap ONCE when still on SITE_SELECTION after the wait;
  - if the dashboard signals missed but `detectCurrentScreen()` proves the
    app advanced into the site context (DASHBOARD/ASSET_*), proceed with a
    warning (protects against locale/DOM drift in the 3 dashboard probes —
    advancement is the contract, not signal match);
  - otherwise throw `VerificationError` with the detected screen + a
    failure screenshot — no more laundering;
  - fail-fast guard: after 3 consecutive site-load failures, subsequent
    tests fail immediately instead of re-burning 2×120s each (prevents the
    40-minute zombie march into WDA death).

## Validation

- `mvn -o test-compile` green; verifier self-tests 34/34 green;
  canary TC_ENG_130 re-run live on the sim: PASSED (13s, fast-path).

## Follow-ups

- Fresh asset-engineer dispatch on the fixed code = the client-report run.
- Arc-flash job of 29006089840 still executing at time of writing — its
  site-selection outcome (same env) will say whether the black hole was a
  one-off sim or systemic.
