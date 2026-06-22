# 091 — Picker/field WDA-wedge hardening, batch 1 (2026-06-22)

Multi-agent investigation of run #27834873516 (90.4% pass; 0 skips — the prior mass-skip
cascade is gone) pinpointed the residual 73 failures: ~67% are the Issues subcategory/class
pickers (42) + Loadcenter/MCC edit (7), all one mechanism — **unscoped whole-tree queries at
the 5s implicit wait with no budget and no open-menu verification**, which wedge WDA on
bleed-through DOMs → 6m per-test hangs. Full plan + blockers:
[picker-wedge-fix-plan-2026-06-22.md](../picker-wedge-fix-plan-2026-06-22.md).

## Shipped this batch (safe, behavior-preserving)
| Method | Was | Now |
|---|---|---|
| `IssuePage.changeIssueClassOnDetails` | 2 whole-tree finds at the **5s default**, no budget; runs first in all 10 subcategory tests + the class tests | both finds wrapped in `withImplicitWait(0)`; `SUBCAT_BUDGET_MS` (45s) deadline on the retry loop; bleed-through exclusion kept |
| `AssetPage.isCoreAttributesSectionVisible` | 3 scrolls × 5 whole-tree CONTAINS at 5s each (LC_EAD_02 6m0s) | every probe wrapped at implicit-wait 0 |

These convert hangs into fast, honest outcomes without changing what's matched or any return
contract — the subset the regression pass rated low/medium and "behavior-preserving on the
happy path."

## Explicitly NOT done (regression blockers — CI-validate batch 1 first)
- No text-field locator replacement (seq 11b — ~94 passing call-sites).
- No deletion of the legacy dropdown-detection branch (seq 12 — ~16 passing sites).
- Open-menu probes stay advisory, never a hard abort (seq 2/5).
These need iOS-18.5 CI validation before trusting; that's the next batch, informed by this run.

## Validation
- `mvn -o -DskipTests test-compile` → exit 0.
- `testng-verify-selftest.xml` → **21 run / 0 failures / 0 skipped**.
- Real validation on device: dispatching the `issues` + `assets` CI jobs (iOS 18.5). The
  budgets mean any test the hang was masking will now surface as a fast, triageable
  failure rather than a 6m wedge — expect wall-clock to drop sharply even where a test
  still fails.
