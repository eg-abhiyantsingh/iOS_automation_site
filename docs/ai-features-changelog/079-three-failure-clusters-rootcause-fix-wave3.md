# 079 — Root-cause + fix the three CI failure clusters (wave 3)

**Prompt:** "cover all the test cases, run script in ci/cd and check fail test case why lots of test case are failing."

Dispatched the **full suite** (`job_selection=all`, run 27556334111 — all 20 jobs /
~1690 tests) AND root-caused the three failure clusters from the targeted
validation runs (Connections 27539297288, Assets P6 27539284048, Offline
27539301468). A 6-agent workflow (3 diagnose + 3 fix) produced the fixes below.
**All failures are CODE (our automation), not app bugs.**

## Cluster 1 — Connections: 50 fail, all fast (~1 min) v1.43 locator drift

The suite was dark for a month (false-green, 0 tests) and locators drifted. The
screen renders fine (TC_CONN_002 saw the header icons) — the misses are:
- **`+` add button (39 of 50 failures):** `isAddButtonDisplayed`/`tapOnAddButton`
  only matched `name/label ∈ {+,add,plus}` at y<150. v1.43's `+` is an SF-Symbol
  (`plus.circle`/`plus.circle.fill`) Image/Button. → widened to SF-symbol names +
  `add connection` label + a positional right-most-header fallback, all on
  0-implicit probes (the miss was burning 25s).
- **Search field (5):** v1.43 search is not a `SearchField`/`TextField` →
  rewrote with 3 strategies + reveal-via-magnifier-icon. Removed the TC_CONN_010
  `0==0` tautology (it "passed" while never typing).
- **List rows (6 + 31 skips):** `getConnectionCount` counted `XCUIElementTypeCell`;
  v1.43 rows are `XCUIElementTypeOther` (the documented v1.36+ SwiftUI shift) →
  added Other/Button row detection. (Residual: this site is genuinely near-empty
  for connections — needs API seeding; flagged as follow-up.)
- **TC_CONN_062 (the one true hang):** rapid-create loop spun on the broken `+` to
  the 360 s cap → added a fast-bail attempt cap. Resolved by the `+` fix anyway.
- `ConnectionsPage` doesn't extend BasePage → added local 0-implicit probe helpers.

## Cluster 2 — Assets P6: 120 skip + 4 hang — the REAL root cause

Not subtype-option selection (wave 2 already fixed that). The hang is
**`changeAssetClassInternal` → `findAssetClassPickerButton`**: a full
`XCUIElementTypeButton` enumeration with per-button `getLocation/getSize/getAttribute`
round-trips over the Edit screen's **asset-list bleed-through** DOM, at the global
5 s implicit wait = **60-113 s per step**. It ran twice for the class read + picker
open, plus `findClassSearchField` twice = **~5.5 min per class change** → 360 s cap
→ **WDA wedged and died** → the 120 `Could not start a new session` skips.

Key insight: the `DeadSessionCircuitBreaker` **never tripped** — failures were
recover-then-redie (372 failed inits interleaved with 123 successful), never
5-consecutive, so the counter kept resetting. The fix is to make the hangs
**fast-fail so WDA never wedges**:
- `findAssetClassPickerButton`: `withImplicitWait(0)` + 8 s enumeration budget +
  cache the button (was enumerated twice per change).
- `getCurrentAssetClassValue` / `openAssetClassPicker`: 0-implicit + reuse the cache.
- `selectClassViaSearch` / `tapAssetClassItem`: eliminated the double
  `findClassSearchField` probe (the 113 s step) via a cached tri-state flag.
- `changeAssetClassInternal`: end-to-end **60 s wall-clock budget** → clean
  `VerificationError` instead of a 6-7 min hang.

## Cluster 3 — Offline: 8 hang at ~7 min (offline = highest priority)

- **Teardown burned 60 s on CI:** `perTestTeardown`'s `goOnline()` ran a 5-strategy
  popup search against a Wi-Fi popup that never opens on CI sims (no real toggle).
  → new `canToggleWifi()` capability probe (0-implicit, no side effect) gates an
  early-return in setup+teardown; `findAndClickPopupOption` entry wait 1500ms→0;
  teardown budget 60s→30s.
- **`enterAssetName` (the 228 s body sink — biggest single contributor):**
  Strategy 1's `findElement(value=='Enter name')` missed on a pre-filled offline
  field and burned the full 5 s implicit wait × retries. → both paths now
  0-implicit; locate the first `TextField` directly on miss (not the stale
  PageFactory cache). *(applied by orchestrator — cross-file follow-up the offline
  agent flagged but couldn't make under its file ownership.)*
- **`getTextFieldValue` (TC_E2E discover-attribute loop):** unconditional
  `scrollFormDown()+sleep` + 5 s-implicit label miss × 10 labels. → probe-first at
  0-implicit, scroll only when absent. *(orchestrator-applied.)*
- 57 skips are **all legitimate infra** (no real Wi-Fi toggle on CI sims, 2nd test
  user, token expiry, failure injection) — zero cascade-skips, confirming wave-1's
  capped teardown + `alwaysRun` re-init already killed the old 840 s cascade.

## The meta-cause (answer to "why lots failing")
`continue-on-error` masked failures, hangs, AND a disabled suite as green for weeks.
Wave-1's result-gate makes them visible — so the count looks worse but is finally
honest. Every cluster is our automation drifting against app v1.43, not app bugs.

## Validation
- `mvn -o -DskipTests test-compile` — clean. `testng-verify-selftest.xml` 21/21.
- Files: `ConnectionsPage`, `AssetPage`, `SiteSelectionPage`, `Connections_Test`,
  `OfflineSyncMultiSite_Test`.

## Follow-ups (noted, not yet done)
- `BaseTest` teardown screenshot: single bounded attempt on session-death (the
  compressed→full-PNG retry storm padded UC4 to 6m36s).
- `DeadSessionCircuitBreaker`: sliding-window (count non-consecutive deaths) +
  WDA hard-restart on 2nd init failure in `DriverManager` — for recover-then-redie.
- Seed connections + a 2nd site via `TestDataApi` so the ~31 "no data" skips
  become real assertions.
