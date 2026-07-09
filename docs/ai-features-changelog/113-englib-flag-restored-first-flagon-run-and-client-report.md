# 113 — eng-lib flag RESTORED on acme QA; first flag-on Equipment Library run dispatched (client report)

**Date:** 2026-07-09
**Prompt:** "check equipment library — need to send report for equipment check"

## What was found

- **The `eng-lib` company feature flag is BACK on acme QA.** Verified live via
  `POST /auth/v2/login` → `GET /auth/v2/me`: `company_features` now includes
  `eng-lib` (it was removed ~2026-07-07 and still absent on 2026-07-08).
- `CompanyFeatureGate` (changelog 110) self-heals as designed: no code change
  needed — the 7 AssetEngineer* classes stop auto-skipping the moment the flag
  is present.

## What was done

- Dispatched **run 29006089840** on `main` (`ios-tests-parallel.yml`) with
  `run_asset_engineer=true` + `run_arc_flash=true` + `send_email=true`.
  - Asset Engineer suite: 12 classes (~166 cases) — 6 original live-validated
    classes + 5 changelog-112 gap classes (TC_ENG_140–205) on their **first
    ever live run** (heat-and-trial expected) + quarantined Matching last.
  - Arc Flash suite (34 cases) included because it carries the eng-lib
    **FlagCanary TC_ENG_130**, which asserts flag⇄UI consistency in both
    directions — the right tripwire on the day the flag returns.
- Confirmed report plumbing for partial runs: `send-email` job is
  `if: always()`, so per-module web-template client reports
  (`asset-engineer_client_report.html`, `arc-flash_client_report.html`) are
  generated and emailed to EMAIL_TO (abhiyant.singh@egalvanic.com) even
  without `run_all`. The 3-shard rerun job is `run_all`-only, so it correctly
  stays out of this targeted run.
- Memory updated: `englib-flag-disabled` → RESOLVED 2026-07-09 with outage
  window history preserved.

## Triage contract for this run (read before forwarding the report)

- **Original 6 classes + AssetEngineerSettings/Matching** — results are real
  signal; these were live-validated on v1.49 before the flag outage.
- **Gap classes (TripConfig 140-149, GroundFault 160-164, CustomSave 170-175,
  FieldPickers 180-192, MatchPanel 200-205)** — written app-truth-first while
  the flag was OFF and never live-run; first-run failures are presumptively
  OUR locator bugs, not app bugs. Do not put gap-class failures in a
  client-facing report without individual triage.
- **TC_ENG_130 canary** — must now PASS in the "flag present ⇒ UI enabled"
  direction; a failure here means the app UI didn't re-enable with the flag.
