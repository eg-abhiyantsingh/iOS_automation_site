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
— all methods resolve. Then a real CI simulator run (dispatch 29313356507, fresh
iPhone 16 Pro / iOS 18.5 container) validated the picks live.

## Live-run verdict (same day): TC_ENG_070 left the suite

Run 29313356507: 23/26 invocations passed — the only failures were TC_ENG_070's
data-provider rows 1–2 (`ThreadTimeoutException` at the 360s cap: the Panelboard
draft fixture hung on the fresh container, and the thread-kill wedged the Appium
session, cascading into row 3's skip; the next class recovered with a new session).
TC_ENG_100 (15s), TC_ENG_101 (18s), TC_ENG_040 (46s) all passed.

Per the suite's own flake-twice-and-leave rule, 070 was removed the same day
(suite is now 23 tests). Its clean failure history came from module-suite runs
where sibling Engineering classes had already warmed the app state — a
**composition effect**: a test's stability record only transfers to a new suite
if its fixture path is exercised the same way. Exactly why new smoke picks get
validated with a real dispatch before the suite is trusted for demos.

## Follow-up (same day): v1.50 dashboard redesign broke the TC_JOB trio

Validation runs 2–3 (29317875087, 29320492369) failed a DIFFERENT trio —
TC_JOB_002/003/004 (changelog-122 picks), each after a full 180s wait +
fallback (~213s). Triage sequence:

1. First hypothesis (shared-tenant collision with the concurrent full-suite
   run) was **falsified** by run 3: identical failure on a quiet tenant.
2. The real cause was version drift: `chore(app): update Z Platform-QA to
   v1.50` landed at 08:15 UTC — after run 1 (v1.49, trio PASSED) and before
   runs 2–3 (v1.50, trio FAILED). The v1.50 dashboard redesign replaced the
   No-Active-Job card with a 'Work Orders' quick-action tile.
3. The failure screenshot (auto-attached) shows the tile RENDERED with its
   badge — so `clickWorkOrderCard()`'s v1.50 predicate was wrong, not the
   screen: `type == Button/Other AND visible == 1` never matched (SwiftUI
   tiles surface as other element types and report visible=false).

**Fix (SiteSelectionPage.clickWorkOrderCard):** match the tile on name/label
only (`name CONTAINS 'Work Orders' OR label CONTAINS 'Work Orders'`), keep the
y>120 + height>40 geometry guard (excludes the top "WO" status badge and the
tile's inner label), and emit a one-shot `🔎 v1.50 tile probe` line listing
type/geometry of every name match so any residual mismatch is diagnosable from
the CI log in a single run. No-match behavior falls through to the legacy
v1.49 path unchanged. This fixes every SiteVisit test that routes through the
dashboard WO card, not just the smoke trio.

**Triage lesson:** the same red (3 tests, ~213s each) had three candidate
causes in one afternoon — composition effect, tenant collision, app-version
drift. Only artifacts distinguished them: per-run failed-suite git records,
run timestamps vs the app-bump commit, and the failure screenshot.

## Final verdict: GREEN

Run 4 (29323264367, with the locator fix): **23/23 passed, 0 failed, 0
skipped**. The CI log confirms all three TC_JOB tests took the new tile path
("✅ Tapping v1.50 'Work Orders' quick-action tile: 107, Work Orders"),
completing in ~12–16s each vs the previous 213s timeouts. The demo smoke
suite is demo-ready on app v1.50.

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
