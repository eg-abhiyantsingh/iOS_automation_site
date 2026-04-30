# 056 — Shared-Asset Optimization Rolled Out To All Asset_Phase* Test Classes

**Date**: 2026-04-30
**Trigger**: User: "did you implemented my suggestion for all asset test case or not".

Yes — applied the user's shared-asset suggestion (changelog 054 PoC) to **every** Asset_Phase test class (P1-P6). The work refactored ~25 navigate helpers and **deleted 438 lines net** of repetitive nav code while making tests 2..N in every section skip the slow class picker.

---

## What Got Refactored

| File | Helpers refactored | Cache fields added |
|---|---|---|
| Asset_Phase1_Test | 4 (ATS, Busway, Capacitor, Bug) | 4 |
| Asset_Phase2_Test | 5 (CB, DS, Fuse, Generator, JunctionBox) | 5 |
| Asset_Phase3_Test | 5 (Loadcenter, MCC, MCCBucket, Motor, Other) | 5 |
| Asset_Phase4_Test | 6 (OCP from PoC + Panelboard, PDU, Relay, SWB, Transformer) | 6 |
| Asset_Phase5_Test | 10 individual + 1 dispatcher (Map cache for 3 delegated helpers: Disconnect, Fuse — and the dispatcher itself for any future ones) | 10 + 1 Map |
| Asset_Phase6_Test | 1 dispatcher (Map cache covering all 15 per-class helpers) | 1 Map |

Total: **~31 helpers across 6 files now use the shared cache**. Diff: `+119 / -557` lines.

---

## How It Works

### [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) — single utility everyone calls

```java
public String openSharedAssetForEditOrFallback(String cachedName) {
    navigateToAssetListTurbo();
    if (cachedName != null && !cachedName.isEmpty()) {
        if (openAssetByNameForEdit(cachedName)) return cachedName;  // fast path
    }
    String name = selectFirstAsset();   // legacy fallback
    sleep(200);
    clickEditTurbo();
    return name;
}
```

### Each navigate helper becomes one line

**Before** (per helper, ~15 lines with logging and waits):
```java
private void navigateToCircuitBreakerEditScreen() {
    long start = System.currentTimeMillis();
    System.out.println("📝 Navigating to CB Edit Asset screen...");
    System.out.println("📦 Going to Asset List...");
    assetPage.navigateToAssetListTurbo();
    System.out.println("🔍 Selecting first asset...");
    assetPage.selectFirstAsset();
    shortWait();
    System.out.println("✏️ Clicking Edit...");
    assetPage.clickEditTurbo();
    System.out.println("✅ On CB Edit Asset screen ...");
}
```

**After**:
```java
private void navigateToEditAssetScreenAndChangeToCircuitBreaker() {
    cachedCBAssetName = assetPage.openSharedAssetForEditOrFallback(cachedCBAssetName);
    assetPage.changeAssetClassToCircuitBreaker();  // preserved — fast-paths after test 1
}
```

For files with a dispatcher (`navigateToEditAssetScreen(String assetTypeName)` in Asset_Phase5/6), the cache became a `Map<String, String>` keyed on the asset type name — one Map per test class.

---

## Why Test Bodies Were Not Touched

Every test still calls `assetPage.changeAssetClassToXxx()` after the navigate. We **kept these calls** because the existing fast-path:

```java
if (isCurrentAssetClassEqualTo(className)) {
    System.out.println("✅ Already " + className);
    return;
}
```

…makes them no-ops once the cached asset is the right class. Net effect:

- **Test 1 (cache miss)**: legacy flow, asset gets converted to target class via the slow picker. Cache populated.
- **Test 2..N (cache hit)**: search-by-name opens THE SAME asset (already the right class). The class picker no-ops. **The 5-10s class picker overhead is skipped.**

This is the safest possible refactor: if anything goes wrong with the cache (asset deleted, search fails), we fall through to the legacy code — identical behavior to before.

---

## Estimated Savings

Per the local report data analysis (changelog 055), the slowest Asset_Phase suites are:

| Suite | Tests | Was avg/test | Class-picker share | Estimated savings |
|---|---|---|---|---|
| Asset_Phase1 | 112 | 3.1 min | ~7s/test on 100 cache-hit tests | ~12 min/run |
| Asset_Phase2 | 108 | 1.9 min | ~7s × 100 | ~12 min/run |
| Asset_Phase3 | 109 | unknown | ~7s × 95 | ~11 min/run |
| Asset_Phase4 | 97 | unknown | ~7s × 80 | ~9 min/run |
| Asset_Phase5 | 112 | unknown | ~7s × 95 | ~11 min/run |
| Asset_Phase6 | 114 | unknown | ~7s × 100 | ~12 min/run |

**Across all 6 asset suites: ~65-70 min saved per parallel CI run.**

Note: this is **separate from and additive to** the Appium HTTP timeout fix (changelog 055, which fixes hung tests). Both ship together on `main`.

---

## What's Excluded (intentional)

A few helpers were not refactored automatically because they had non-standard logic:
- `Asset_Phase1::navigateToNewAssetScreen` (creates a new asset, not edit)
- `Asset_Phase1::navigateToNewAssetScreenForBugTests` (same)
- `Asset_Phase6::navigateToAssetDetailsScreen` (details screen, not edit)
- `Asset_Phase6::navigateToEditTaskDetailsScreen` (task details, different surface)

These don't fit the shared-asset pattern (they create new assets, or don't open Edit). Leaving them alone.

---

## Validation Plan

Once the in-flight `issues-p1` validation run from changelog 055 finishes and proves the HTTP-timeout fix landed cleanly, dispatch `assets-p1` (or `assets-p4`) on `main` and compare:

1. **Wall-clock time** — should drop ~10-12 min from the asset suites
2. **`ThreadTimeoutException` count** — should be near zero (HTTP timeout fix)
3. **No new failures** — the cache-miss fallback path is identical to legacy behavior, so any regression is in the fast-path code, easy to localize

If a regression appears, revert is one commit. The shared utility (`openSharedAssetForEditOrFallback`) and the per-class caches are entirely additive — removing them restores the old direct-inline calls.
