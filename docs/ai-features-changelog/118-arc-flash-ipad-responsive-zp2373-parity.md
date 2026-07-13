# 118 — Arc Flash Readiness on iPhone + iPad: responsive tests, iPad CI job, ZP-2373 parity

**Date:** 2026-07-13
**Ticket:** extend Arc Flash Readiness QA automation (web ZP-2373, Done) to iOS
iPhone and iPad — NFPA 70E per-asset-class progress dashboard.

## What was added

1. **`ArcFlashResponsive_Test` (TC_AF_040-044)** — form-factor contract that
   runs UNCHANGED on iPhone and iPad (assertions against live window size,
   never absolute coordinates; `[iPhone]`/`[iPad]` stamped into every report
   title via a points-based form-factor probe, min-dimension ≥700pt = iPad):
   - 040 dashboard fully inside viewport (score card bounds + 3 metric cards)
   - 041 touch-scroll round-trip leaves readiness percent unchanged
   - 042 per-asset-class buckets render readable (web AF_09 parity)
   - 043 touch drill into a class bucket reveals per-asset rows (AF_13 parity)
   - 044 every metric card switches the breakdown by touch (AF_06 parity)
   Wired into `testng-arc-flash.xml` → suite is now 39 cases.

2. **iPad CI job** — `run_arc_flash_ipad` workflow input +
   `arc-flash-ipad-tests` job (clone of the arc-flash job with an
   iPad Pro 13-inch → iPad Pro → any-iPad simulator fallback chain,
   `arc-flash-ipad-report` artifact, `arc-flash-ipad` module email).
   Added to both downstream `needs:` lists.

3. **Parity map** — `docs/arc-flash-zp2373-ios-parity.md`: all 45 web cases
   mapped to iOS coverage or justified N/A (rows-per-page grids, role
   selector, tooltips, report generation are web-only surfaces).

4. **ArcFlashPage** additions: `readinessScoreCardElement()` (viewport
   assertions), `scrollBreakdownDown()/Up()` (public touch-scroll wrappers).

## Validation

- `mvn -o test-compile` green; YAML validated.
- Local iPhone spot-check of TC_AF_040 + CI dispatch on both form factors —
  results recorded in the session summary / next changelog.


## CLOSED 2026-07-13 — iPad validation GREEN (run 29240891572)

Three CI iterations to a green iPad job: (1) 240s boot budget too small,
(2) runner ships NO bootable iPad for iOS 18 → create-if-missing,
(3) newest device type (M5) incompatible with the iOS 18 runtime (exit 147)
→ runtime-compatible candidate loop. Final result: **iPad 35/39 — exact
parity with iPhone** (same 2 pre-existing fails TC_AF_002/014, same 2 fixture
skips), all five responsive cases PASSED with [iPad] stamps in the report.
All ticket acceptance criteria met.
