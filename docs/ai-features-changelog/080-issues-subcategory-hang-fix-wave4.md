# 080 — Issues OSHA-Subcategory hang fix (wave 4)

**Context:** full-suite run 27557701204 (wave-3) — first completed job, **Issues P2**:
0 passed / 6 failed / 44 skipped. A NEW cluster, same death-spiral family as Assets P6.

## Root cause (CODE, not app)

The OSHA Subcategory dropdown selection (`TC_ISS_130-135`) hung **7-13 minutes per
test**, throwing `java.util.concurrent.TimeoutException` — a CLIENT-SIDE command
timeout: a single WDA a11y query over the complex OSHA Issue-Details SwiftUI DOM
(with the documented previous-screen bleed-through) blocked until `newCommandTimeout`.
The wedged query **killed the WDA session** ("Error communicating with the remote
browser. It may have died."), which cascaded into **44 SKIPPED** tests ("Could not
start a new session"). 295 session-death markers in the one job log.

This is the same shape as the Assets P6 class-change hang (wave 3): a heavy
enumeration on a complex SwiftUI screen, run at the global 5 s implicit wait, wedges
WDA → mass-skip cascade. The `DeadSessionCircuitBreaker` doesn't trip (recover-then-redie,
not 5-consecutive).

## Fix — `IssuePage.java` only (shared across all 3 Issues suites, ~240 tests)

Applied the proven AssetPage pattern: 0-implicit probes + wall-clock budgets.
- New constants `SUBCAT_BUDGET_MS=45s`, `SUBCAT_LABEL_BUDGET_MS=20s`.
- `ensureSubcategoryLabelPositioned`, `tapSubcategoryField`, `selectSubcategory`,
  `searchSubcategory`, `selectSubcategoryAndGetValue`, `searchSubcategoryAndCountResults`:
  wall-clock deadline checked between/inside strategies; every scroll loop gated by
  BOTH an iteration cap AND the deadline; all whole-tree `findElements` wrapped in
  `withImplicitWait(0, …)`. All multi-strategy locators kept — only the per-miss cost
  drops from 5 s to ~ms, and a runaway loop can no longer reach the 360 s cap.
- `getSubcategoryValue`, `getVisibleSubcategoryOptions`, `isSubcategoryOptionDisplayed`,
  `verifyNECSubcategoryOptions`, `getFilteredSubcategoryOptions`: bare full-implicit
  whole-tree scans → `withImplicitWait(0, …)`.

**Worst case now:** `tapSubcategoryField` ~45-65 s, others ≤45 s — all well under the
360 s cap, so a wedged query can't hang the test long enough to kill WDA, which
removes the upstream cause of the 44-skip cascade.

## Validation
- `mvn -o -DskipTests test-compile` — clean. `testng-verify-selftest.xml` 21/21.

## Note on the broader pattern
Three clusters now share one root cause: heavy a11y queries on complex SwiftUI
screens wedge WDA → session death → mass-skip (Assets P6 class-change, Issues
subcategory, and the offline screenshot-retry storm). Per-method bounding (waves 3-4)
fixes each as found. The durable cross-cutting hardening — flagged as a follow-up —
is in `DriverManager`/`DeadSessionCircuitBreaker`: a sliding-window breaker (count
non-consecutive deaths) + WDA hard-restart on repeated init failure + a lower
per-command timeout so any wedged query fails in ~90 s, not 360 s. Held for now
because DriverManager session-creation changes are high-risk and only CI (iOS 18.5)
can validate them — being staged carefully rather than rushed into the discovery run.
