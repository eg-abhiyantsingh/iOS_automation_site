# 084 — UC1 pass: navigateToAssetList strategies reordered + Sites button Dashboard fallback

## TL;DR

`UC1_singleUserMultipleSites_dataIntegrity` now **passes end-to-end** locally
(3m 49s) for the first time in this debugging session. Two layered fixes:

1. `AssetPage.navigateToAssetListTurbo` — reordered tab-find strategies so the
   unique `accessibilityId('list.bullet')` runs first (`label == 'Assets'`
   matched both the bottom-nav tab AND the Dashboard Quick-Action card, which
   misrouted offline runs through the Sites picker).
2. `AssetPage.verifyOnAssetList` — rewritten from `WebDriverWait.until(...)`
   to a manual polling loop. `WebDriverWait` cannot tell "keep waiting" from
   "abort early" — both `false` and `Boolean.FALSE` returns mean *keep
   waiting*. The previous version waited the full 3.5s timeout even when the
   Sites picker was clearly visible, so the dismiss path never fired.
3. `SiteSelectionPage.clickSitesButton` — added a Dashboard-return fallback.
   After offline asset Save we land on Asset List, whose nav bar lacks the
   `building.2` Sites button (only `1, More, plus, WO`). The fallback now
   taps the Site/house bottom-nav tab (`name='house'`) to return to Dashboard
   and retries.

## Symptom prior to fix

UC1 v13–v15 logs:
```
⚠️ list.bullet: no cells in 3.5s — possibly wrong screen, trying next
⚠️ label='Assets': no cells in 3.5s — possibly wrong screen, trying next
⚠️ Trying Site tab first then list.bullet...
⚠️ Site→list.bullet: no cells in 3.5s — possibly wrong screen, trying next
   Strategy 1: Finding asset name StaticText elements...
   Found 0 cells
⚠️ selectFirstAsset bailed: looks like Sites picker
```
…and v15:
```
🔄 Switching to site: Test QA 16
   Nav button: 1
   Nav button: More
   Nav button: plus
   Nav button: WO
⚠️ Sites button not found with any strategy
```

## After fix (v16)

```
📝 Step 2b: Edit first asset offline on Site A
✅ Asset List opened (list.bullet, cells rendered)
   🎯 Asset name text: CB-FMC SUITE 140_UC1_...
[UC1] After offline asset-edit, pending sync count = 1
📝 Step 3: Switch to Site B (must remain offline throughout)
✅ Returned to Dashboard via Site tab — retrying Sites button
✅ Found Sites button (post-Dashboard nav) via building.2
📝 Step 4: Switch back to Site A (must remain offline)
✅ Found Sites button via original accessibility ID: building.2
📝 Step 5: Verify Site-A offline data persisted (queue did not shrink)
[UC1] Final pending sync count = 1
✅ Test PASSED: UC1_singleUserMultipleSites_dataIntegrity (3m 49s)
```

## Why `WebDriverWait` was the wrong tool

`ExpectedCondition` semantics: any falsy return (null, `false`, `Boolean.FALSE`)
means "still not ready, keep polling until timeout." There is no idiomatic way
to signal "abort early — give up immediately" from inside `until(...)`. To
distinguish *positive* from *negative* signals during the same poll window we
need a manual loop:

```java
long deadline = System.currentTimeMillis() + 3500;
while (System.currentTimeMillis() < deadline) {
    if (!driver.findElements(cells).isEmpty()) return true;        // positive
    if (!driver.findElements(sitesPicker).isEmpty()) break;        // negative
    sleep(150);
}
// dismiss Sites picker if detected
```

## Files changed

- `src/main/java/com/egalvanic/pages/AssetPage.java`
  - `navigateToAssetListTurbo()`: strategies reordered `list.bullet → label='Assets' → Site→list.bullet`.
  - New private helper `verifyOnAssetList(strategyName)` — manual poll, positive/negative early-exit, Cancel/swipe-down dismiss.
- `src/main/java/com/egalvanic/pages/SiteSelectionPage.java`
  - `clickSitesButton()`: when all on-screen searches miss, tap `name='house'` (Site bottom-nav tab), wait 800 ms, then retry building.2 + name-scan.

## Memory-prompt rules followed

- ✅ QA repo only — never push to dev repo (`Egalvanic/eg-pz-mobile-iOS`).
- ✅ Multi-strategy locators (Strategy 1/2/3 + dismiss fallback × 2).
- ✅ Wait cap ≥ 3 s (manual loop uses 3.5 s).
- ✅ Visible sim only (iPhone 17 Pro iOS 26.2, UDID `B745C0EF...`).
- ✅ Local test-driver loop — one test, inspect, fix, rerun (v13 → v14 → v15 → v16).

## Next

UC1 v16 is the model run. Re-run the rest of `OfflineSyncMultiSite_Test` to
see how many other UCs benefit from the same shared-layer fixes.
