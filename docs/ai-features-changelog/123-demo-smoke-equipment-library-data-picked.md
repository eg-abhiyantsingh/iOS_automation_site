# 123 — Demo Smoke: Equipment Library block re-picked from failure-history data

**Date:** 2026-07-14 · **Time:** ~13:15 IST
**Files:** `src/test/resources/parallel/testng-smoke-demo.xml`, `.github/workflows/ios-tests-parallel.yml`

## What changed

The demo smoke suite's "Smoke - Equipment Library" block (changelog 122) was replaced.
Out: `TC_ENG_130` (FlagCanary) and `TC_ENG_004` (downloaded-state subtitle).
In: `TC_ENG_100` + `TC_ENG_101` (AssetEngineerSettings_Test), `TC_ENG_040`
(AssetEngineer_Test), `TC_ENG_070` (AssetEngineerMenus_Test). Suite: 22 → 24 tests.

## Why (the evidence, step by step)

1. **All 85 TC_ENG tests across 13 AssetEngineer classes were inventoried**
   (per-test purpose, fixture cost, mutation, fragility notes from javadocs).
2. **Failure history was reconstructed per test**, including same-day CI runs whose
   `failed-suites/failed-YYYY-MM-DD.xml` had been overwritten — recovered via
   `git show <commit>:failed-suites/...` on the per-run `[skip ci]` commits.
   Five recent failing runs: 07-08 (systemic wipeout, 47 Engineering failures),
   07-09 ×2, 07-13 ×2.
3. **The old picks fail the suite's own charter** ("flakes twice → leaves"):
   - `TC_ENG_130` is a *canary*: deliberately NOT gated on the eng-lib feature flag so
     it fails loudly on flag drift. It fired on 07-09 (run 29006089840) and 07-13
     (run 29242294346). Correct behavior — but a must-be-green demo suite is the wrong
     home for an alarm. It stays in the daily monitor/module suites.
   - `TC_ENG_004` asserts the *downloaded* library subtitle, so it inherits every
     library/flag environment problem; it failed on the 07-09 drift day.
4. **Library-match tests are excluded from demo smoke *by design*, not by taste:**
   the SKM library download takes 2–10 min on a fresh CI container, while
   `GlobalTestTimeout` caps un-annotated tests at **360 s**. A cold-container
   match-panel test can be killed mid-download — unstable by construction.
   The match journey stays covered daily in `testng-asset-engineer.xml`
   (`AssetEngineerMatchPanel_Test` etc. — added 07-08, clean record so far but too
   young for "proven-stable" claims).
5. **The four new picks have clean records across all 5 failing runs, including the
   07-08 wipeout**, need no library download, run on cancelled drafts / read-only
   surfaces, and still demo real Engineering value:
   - `TC_ENG_100` — Engineering settings opens; Equipment Library card + state subtitle
     render (valid in any download state).
   - `TC_ENG_101` — "Load Device Library?" dialog opens/cancels twice with zero state
     drift (the cancel gate).
   - `TC_ENG_040` — Add Asset offers Quick/Detailed modes and the class picker lists
     Circuit Breaker; everything cancelled.
   - `TC_ENG_070` — Mains Type enum picker sets the chip (MLO/FDS/NFDS ×3) on a
     Panelboard draft. Note: it never touches the class-SWITCH path whose fragile
     chip locator makes siblings 071–074 repeat offenders.

## Validation

`mvn test -Dtestng.mode.dryrun=true -DsuiteXmlFile=src/test/resources/parallel/testng-smoke-demo.xml`
— all 24 methods resolve; the 4 Engineering includes run (070 enumerates its 3 rows).
Real-device/simulator run happens on the next `run_smoke_demo` CI dispatch.

## Depth: what this teaches

- **A canary and a smoke test are opposites.** A canary maximizes sensitivity to
  environment change; a smoke test maximizes robustness to it. Putting a canary in a
  demo suite converts "environment drifted" into "the product looks broken."
- **Absence from a failure log is only evidence if the test actually ran.** MatchPanel/
  CustomSave have zero failures but were added 6 days ago — young classes need
  5–10 clean runs before earning "proven-stable" slots.
- **Timeout budgets are selection criteria.** 360 s default cap vs a 2–10 min library
  download means the exclusion is structural; no amount of retries fixes it.
- **Overwritten CI artifacts aren't gone if they're committed** — per-run `[skip ci]`
  commits let `git show` recover any same-day run's failure list.
