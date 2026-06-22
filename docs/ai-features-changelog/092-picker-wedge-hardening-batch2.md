# 092 — Picker/field WDA-wedge hardening, batch 2 (2026-06-22)

Continues [091](091-picker-wedge-hardening-batch1.md). Same safe, behavior-preserving pattern
(`withImplicitWait(0)` + wall-clock budget on unscoped whole-tree finds), extended to the next
two highest-leverage helpers — chosen for low regression risk per the investigation's
regression pass.

## Shipped
| Method | Was | Now |
|---|---|---|
| `AssetPage.selectDropdownOption` | locate loop: 3 attempts × whole-tree finds (label re-find, `Select…`/value button scan) at the **5s default**; on Loadcenter (Manufacturer/Voltage absent) it burned minutes (LC_EAD_16/20/22, MCC_EAD_20) | `DROPDOWN_BUDGET_MS` (25s) on the locate loop + the 3 locate finds wrapped at implicit-wait 0. **Post-open option-selection finds left untouched** (they run after the picker opens and need the render wait — touching them is the riskier part) |
| `IssuePage.readIssueClassOptions` | two whole-tree `className(Button/StaticText)` finds at the 5s default (TC_ISS_SAFETY_01) | both wrapped at implicit-wait 0 |

25s × 3-calls-per-test = 75s, safely under the 360s per-test cap (the budget-aggregation
concern the regression pass flagged for LC_EAD_22/MCC_EAD_20).

## Deferred to a CI-gated batch (deliberately NOT done here)
- **seq 11a `editTextField`/`fillTextField` wrap-only** — even wrap-only touches ~94 currently
  -green Asset_Phase1-5 call-sites; the regression pass named it the single biggest regression
  vector. Hold until batch-1/2 CI (run 27931604528 + next) confirms the pattern is safe on
  iOS 18.5, then ship with a fresh run.
- seq 13 Issues header/probe speed-ups; the regression-blocked items (advisory open-probes,
  scoped branch); and the Busway/Loadcenter **structural** drift (Busway lost its subtypes,
  Loadcenter down to 2 fields) — these change test intent and need device confirmation first.

## Validation
- `mvn -o -DskipTests test-compile` → exit 0.
- `testng-verify-selftest.xml` → **21 run / 0 failures**.
- Rides on `main` with batch 1; validated together in the next CI dispatch (avoiding a second
  concurrent multi-hour iOS run while 27931604528 is still in flight on batch 1).
