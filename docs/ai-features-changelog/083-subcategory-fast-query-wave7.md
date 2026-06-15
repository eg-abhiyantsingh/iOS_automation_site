# 083 — Subcategory query made FAST, not just budget-bounded (wave 7)

**Context:** final validation run 27571754122 (all waves 1-6), Issues P2 re-run.
Wave-4's 45s budget PARTIALLY worked: `TC_ISS_130` now **passes** (273s) where it used
to hang-and-die — the bail fired ("Subcategory tap abandoned after 45s budget"). But 3
multi-step OSHA tests (`TC_ISS_131/133/134`) **still hit the 360s cap** and wedged WDA
(260 death markers), and every OSHA test was slow (~7-8 min).

## Why the budget wasn't enough
The budget is checked BETWEEN strategies but **cannot interrupt a single in-flight WDA
command**. `tapSubcategoryField` Strategy 3 ran a whole-tree `findElements` whose predicate
included `XCUIElementTypeOther`. On the OSHA Issue-Details screen (previous-screen
bleed-through) matching `Other` resolves a massive a11y snapshot that blocks the full 90s
`readTimeout` — `withImplicitWait(0)` sets the implicit wait to 0 but does NOT bound query
EXECUTION. So each subcategory tap cost ~90s; multi-step tests (search + compare) stacked
several of those past 360s.

## Fix
Dropped `XCUIElementTypeOther` from the Strategy 3 whole-tree scan (kept TextField /
Button / TextView / ComboBox). The candidate-scoring already treated `Other` as a last
resort (+1000 penalty), so it was almost never chosen — but its mere inclusion forced the
expensive full-tree resolution. Real dropdowns are TextField/Button/ComboBox; Strategy 4
(tap the label) covers the custom-view case. The query is now fast (no 90s block), so the
45s budget rarely triggers and multi-step OSHA tests complete well under the cap.

The other two `Other`-inclusive subcategory scans were checked and are SAFE — both carry an
`AND label == '...'` filter (scoped → fast), not whole-tree.

## Validation
- `mvn -o -DskipTests test-compile` clean; `testng-verify-selftest.xml` 21/21.
- Real proof is the next run: expect Issues P1/P2/P3 OSHA tests to drop from ~7-8 min to
  seconds and the 3 residual 360s hangs to clear.

## Lesson (recorded)
`withImplicitWait(0)` bounds the WAIT for an element, not the COST of resolving a broad
predicate. On bleed-through SwiftUI screens, a whole-tree predicate that includes
`XCUIElementTypeOther` is intrinsically slow regardless of implicit wait — narrow the
predicate (drop `Other` / scope by region / add a label filter) rather than relying on a
wall-clock budget to rescue it.
