# 133 — Arc Flash by Asset: 100% class coverage, simple per-class test names

**Prompt (2026-07-20):** "for arc flash cover all asset and test case name should be
simple like fuse arc flash … to 100% check" (naming style of `TC_AF_095_punchlistOptionAlwaysListed`).

## What changed

### `ArcFlashAssetMatrix_Test.java` — rewritten
- **Before:** one data-driven test (`TC_AFA_arcFlashDataByAssetClass[…]`) over a
  14-class matrix. Fuse, Circuit Breaker, Cable, Battery, Relay, PDU, VFD and 17
  other classes were uncovered, and report/rerun granularity was per-provider,
  not per-class.
- **After:** **38 individually-named tests, TC_AF_101–138 — one per asset class**
  of the node-classes gold spec / live picker (same class census as
  `AssetEngineerClassMatrix_Test`, which is app-truth-first). Names are simple and
  read like the report line the user wants:
  `TC_AF_110_fuseArcFlash`, `TC_AF_101_atsArcFlash`, `TC_AF_133_transformerArcFlash`, …
- Numbers start at 101 because TC_AF_100 was the highest existing AF case.

### Per-test contract (unchanged logic, now shared via `checkArcFlash`)
1. Find an asset of the class by token search; honest **SKIP naming the missing
   fixture** if the site has none.
2. Open its details, `verifyAppAlive`.
3. Assert ≥1 arc-flash-relevant field label renders (Voltage heads every list;
   class-specific: Ampere/Interrupting Rating, K V A, R P M, Fuse Count, …).
4. Read each present label's **value from page source** (query layer lies on
   heavy detail DOMs) and log it into the report.

### New correctness guard: sibling-class exclusion
Ambiguous tokens could let the wrong fixture satisfy a test ("Main Motor
Starter 1" passing the **Motor** test; "Loadcenter A" passing **Load**;
"MCC Bucket 2" passing **MCC**; "VFD Panel" passing **VFD**). New
`belongsToSiblingClass()` rejects any candidate whose name contains a *longer*
class name that embeds the search token — derived from the `ALL_CLASSES` census,
no per-class exclude lists to maintain.

### Suite wiring
- `src/test/resources/parallel/testng-arc-flash.xml`: new test block
  *"Arc Flash - By Asset (100% class coverage, TC_AF_101-138)"*; suite renamed
  34 → 72 tests. `testng.xml` untouched (it carries only the AF smoke classes,
  same as the other deep AF suites).

## Validation
- `mvn -o -DskipTests test-compile` — PASS.
- `testng-verify-selftest.xml` — 34/34 PASS.
- Device validation: dispatch the **arc-flash** CI job; expect fixture-gap SKIPs
  for rare classes (Series/Shunt Reactor, MCC Bucket, Transformer (3-Winding),
  Other (OCP)…) — each SKIP message names the fixture to add to reach live 100%.

## Coverage ledger update
Arc-flash module: 34 → 72 cases (38 new per-asset checks). Full AF-by-asset
class coverage: 38/38 gold classes have a named test.
