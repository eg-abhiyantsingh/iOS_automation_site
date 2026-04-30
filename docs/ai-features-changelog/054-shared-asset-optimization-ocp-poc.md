# 054 — Shared-Asset PoC: Skip the Class Picker on Repeat Tests

**Date**: 2026-04-30
**Trigger**: User suggested: "create asset with class for example ats and provide unique name then open that same asset so that we don't need to change asset class again and again in every test case for edit asset."

This is a **proof-of-concept** scoped to the OCP section of `Asset_Phase4_Test` (~13 tests). If it shows real CI savings, the same pattern will be applied to the other 5 sections of Asset_Phase4 (Panelboard / PDU / Relay / Switchboard / Transformer) and to `Asset_Phase1/2/3/5/6`.

---

## What Was Slow

Every Edit Asset test followed this pattern:

```java
private void navigateToOtherOCPEditScreen() {
    assetPage.navigateToAssetListTurbo();
    assetPage.selectFirstAsset();      // picks WHATEVER asset happens to be first
    assetPage.clickEditTurbo();
}

@Test
public void OCP_EAD_XX(...) {
    navigateToOtherOCPEditScreen();
    assetPage.changeAssetClassToOtherOCP();   // <-- the slow part
    // ... actual assertions
}
```

`changeAssetClassToOtherOCP()` opens the iOS asset-class picker, scrolls to find "Other (OCP)", taps it, taps Done. Per call: ~5-10 seconds. Across the 13 OCP tests, that's **~60-120 seconds wasted per CI run** just normalizing the class on a fresh asset every time.

The existing fast-path in `AssetPage.changeAssetClassInternal`:
```java
if (isCurrentAssetClassEqualTo(className)) {
    System.out.println("✅ Already " + className);
    return;
}
```
…is correct, but the helper picks a **different** "first asset" each time the list reorders or test data drifts, so the fast-path rarely triggers.

---

## What Changed

### [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) — new public method

```java
public boolean openAssetByNameForEdit(String assetName) {
    // searchAsset → selectAssetByName → clickEditTurbo
    // returns true if the Edit Asset screen is visible after the click
}
```

Pure composition of existing helpers. Returns `false` on any failure so callers can fall back to the legacy path.

### [Asset_Phase4_Test.java](../../src/test/java/com/egalvanic/tests/Asset_Phase4_Test.java) — cache + refactored helper

- Added `static String cachedOCPAssetName = null` at class scope.
- Refactored `navigateToOtherOCPEditScreen()` to:
  1. **Cache hit** (test 2..N): call `openAssetByNameForEdit(cachedOCPAssetName)`. The asset is already OCP class from test 1, so the test body's `changeAssetClassToOtherOCP()` no-ops via fast-path.
  2. **Cache miss** (test 1, or fast-path failure): legacy `selectFirstAsset()` + `clickEditTurbo()`. Capture the asset name and cache it so test 2 can use the fast path.

The 13 OCP test bodies are **unchanged** — they still call `changeAssetClassToOtherOCP()`. We rely on its existing fast-path detection rather than removing the calls. This is the safest possible PoC: if the cache misbehaves, the test body's class normalization still fires.

---

## Why This Is Safe

| Risk | Mitigation |
|---|---|
| Cached asset deleted between tests | `openAssetByNameForEdit` returns false → fall back to legacy path → re-cache from new first asset |
| First asset is a different class | Test 1's `changeAssetClassToOtherOCP()` converts it — that's the same as today |
| Search-by-name path is broken on iOS | Fall back to legacy path; nothing degrades worse than current |
| New CI environment has no assets | Asset list is provisioned by `loginAndSelectSite`, separate from this change |

This is **purely additive** — no existing test method changed, no field mutated, no @BeforeClass touched, no class-change calls removed.

---

## Expected Savings

| Scope | Before | After (cache warm) | Saved |
|---|---|---|---|
| OCP test 1 | ~25s | ~25s | 0 |
| OCP tests 2-13 | ~25s each | ~17s each (no class picker) | ~8s × 12 = ~96s |

For Asset_Phase4 OCP section specifically: **~1.5 min cut per CI run.**

If pattern works, applying to all 6 sections of Asset_Phase4 + Asset_Phase1-3+5+6 + Issue_Phase1-3 → estimated **20-40 min CI saved across the parallel matrix**.

---

## Validation Plan

1. Push this commit.
2. Wait for in-flight CI to finish (so we get a clean baseline).
3. Dispatch `assets-p4` only — measure OCP section timing in the test output log.
4. Compare to historical Asset_Phase4 OCP section timing.
5. If savings confirmed: replicate to the other 5 sections of Asset_Phase4, then to other phases, one section per commit so each can be validated.

If savings fail to materialize or any test regresses: revert this commit. Zero damage to other suites since changes are entirely contained.
