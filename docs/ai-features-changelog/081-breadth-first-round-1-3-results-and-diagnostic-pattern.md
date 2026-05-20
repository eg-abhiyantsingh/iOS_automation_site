# 081 — Breadth-First Round 1–3 Results + Diagnostic Pattern for Issues Nav

**Date**: 2026-05-20
**Time**: 18:30 IST
**Trigger**: User: *"so many test case are failing first understand the code and check the issues"* → then guidance on breadth-first strategy across modules with 2-failed-attempts cutoff. Later they shared a dev-repo CI log showing 58 Issues tests failing identically.

---

## Summary

Per the user's instruction to perfect ONE test before moving on, and to cycle breadth-first across modules when stuck, I ran 8 tests this session across Round 1–3. Three new commits landed; the dev-repo CI now has the shared-layer Welcome-page auto-advance plus locale-aware New Connection detection plus a diagnostic dump for the Issues nav bug.

---

## Tests Run This Session

### Round 1 (one per module from the April 31-failure list)

| Test | Result | Wall time | Outcome |
|---|---|---|---:|
| TC_CONN_004 | ✅ **PASS** | 3m 26s | Cold-Welcome auto-advance fix worked |
| TC_ISS_092 | ❌ FAIL | 6m 8s | Auto-advance OK; Appium session died mid-test |
| ATS_ECR_07 | ❌ FAIL | 2m 31s | Auto-advance OK; Asset Class dropdown SwiftUI-Other bug |

### Round 2

| Test | Result | Wall time | Outcome |
|---|---|---|---:|
| TC_CONN_005 | ✅ **PASS** | 52s | Auto-advance inherited Dashboard state cleanly |
| TC_ISS_130 | ❌ FAIL | 3m 12s | Same Appium session-death pattern |
| ATS_ECR_10 | ❌ FAIL | 3m 8s | Auto-advance OK; same Asset Class SwiftUI-Other bug |

### Round 3 (Connections sweep per user redirect)

| Test | Result | Wall time | Outcome |
|---|---|---|---:|
| TC_CONN_036 | ❌ Timeout | 13m 21s | Local Appium degraded; fixes in code, CI will validate |
| TC_CONN_041 | ❌ Timeout | 13m 41s | French i18n fix added; CI will validate |
| TC_CONN_059 | ✅ **PASS** | 15m 36s | Was 420s TIMEOUT in April CI; **now PASSES** |
| TC_CONN_062 | ❌ FAIL | 45m 20s | Different bug — "Initial: 2, Final: 0" — needs standalone investigation |

**Verified PASS this session: 3 tests (TC_CONN_004, TC_CONN_005, TC_CONN_059)** — all three were April CI failures.

---

## Fixes Pushed

### `278c030` — BaseTest auto-advance (changelog 080)

`BaseTest.@BeforeMethod` now drives Welcome → Login → Site Selection → Dashboard via direct `findElement` calls. Fixes the cross-cutting bug that broke every test when run in isolation (or as the first test of a fresh CI job).

Also includes:
- `SiteSelectionPage.navigateToDashboardFromAnyScreen` Strategy 0 (Welcome handler — defensive fallback)
- `IssuePage.tapOnIssuesButton` Strategy 2c (SwiftUI Quick Action StaticText + coordinate tap)
- `DriverManager.initDriver` force-English via `simctl spawn defaults write appLanguage=en` + `AppleLanguages` process arg

### `e9feccb` — New Connection screen French i18n

`ConnectionsPage.isNewConnectionScreenDisplayed` widened to match French:
- Nav bar / title: `Nouvelle connexion`
- Create button: `Créer`
- Source Node field: `Nœud source` / `Noeud source` (accent-insensitive)
- Section header: `Détails de la connexion` / `Details de la connexion`

This was the smoking-gun finding from TC_CONN_041 run 1 — the test successfully tapped `+`, opened the form, but couldn't recognize the (French-rendered) New Connection screen.

### `6ddc974` — Diagnostic DOM dump for Issues button

When all 4 strategies in `IssuePage.tapOnIssuesButton` fail, dumps:
- Top 20 buttons (Y / name / label)
- Top 30 StaticTexts in dashboard region (Y 80–900)
- Top 15 Other/Image/Cell elements with name or label

Critical because the CI log showed every Issues test failing identically (`Tab bar Issues button not found` / `No Issues button found in tab bar area` / `Coordinate tap did not reach Issues screen`), but no clue what the Issues element actually IS. Next CI run will reveal it.

---

## Parallel Work by User (Other Claude Session)

The user ran another Claude session concurrently and pushed three i18n commits during my work:

| Commit | Scope |
|---|---|
| `480bf4a` | Locations / New Building French (`Emplacements` / `Nouveau bâtiment`) |
| `8499a61` | NewBuilding title detection across types + **ConnectionsPage Add button Strategy 4+5** (independent convergence with my fix — identical code) |
| `29a9835` | AssetPage French (`Nouvel actif` / `Détails de l'actif` / `Sélectionnez une classe d'actif`) |

The convergence of independent Claude sessions on the exact same Add-button Strategy 4+5 (`mobile: tap` on SwiftUI Other element by coordinates) is strong evidence the fix shape is correct.

---

## Open Issues (Not Fixed This Session)

### 1. Issues nav on cold Dashboard (HIGH IMPACT — blocks 192 Issues tests)

Dev-repo CI shows every Issues test failing at `tapOnIssuesButton` with all 4 strategies missing. My Strategy 2c (StaticText `Issues`/`Problèmes`/`Problemes`) is in code but also silently fails — the label format is something we haven't matched. Diagnostic dump (`6ddc974`) will reveal the truth on next CI run.

### 2. Asset Class dropdown SwiftUI-Other (BLOCKS ATS_ECR_07, ATS_ECR_10, BUG_SPECIAL_01, ATS_EAD_13/17)

`AssetPage.clickSelectAssetClass` has 5 strategies, all filter to `XCUIElementTypeButton`. When the screen renders the dropdown as SwiftUI custom view, all strategies fail and the test sees "Found 0 buttons on screen". Same fix pattern as `tapOnIssuesButton` Strategy 2c needed.

### 3. TC_CONN_062 — connection count mismatch

"Initial: 2, Final: 0" — the rapid-creation flow ended with fewer connections than it started. Distinct from the April CI timeout. Needs standalone investigation — likely `selectRandomSiblingAsset` is hitting a stale-element issue mid-flight, or cleanup logic in the test is deleting more than it created.

### 4. Local Appium degradation after many back-to-back tests

iPhone 17 Pro / iOS 26.2 / Appium 3.1.2 — after ~8 test runs, the session repeatedly throws `Session does not exist` mid-test, and individual operations take 30+ seconds. CI uses fresh Appium per job and doesn't have this issue. For local validation in future sessions: restart Appium + sims every ~5 tests, OR use the user's existing parallel-sims setup (DriverManager already supports per-test UDID via `-DUDID=...`).

---

## Lessons Saved to Memory This Session

(Implicit — not yet saved as memory files):

- **Lesson 1**: When agent hypotheses about test failures don't match real stack traces (and the test code on `main` has moved since the report was generated), the right move is to pull fresh stack traces from the actual log/CI output, not trust the agent's narrative. Cost me ~30 minutes early in this session.
- **Lesson 2**: PageFactory's `isElementDisplayed(...)` catches ALL exceptions and returns `false`, even on screens where the element is visible. For initial state detection in `BaseTest.@BeforeMethod`, use direct `findElement` calls instead — same idiom that `waitForAppReadyFast()` already uses.
- **Lesson 3**: For SwiftUI custom views that render as `XCUIElementTypeOther`, the predicate `type == 'XCUIElementTypeButton' AND label CONTAINS '...'` silently misses them. The fix pattern is: find the inner `XCUIElementTypeStaticText` with the expected label, then `mobile: tap` at its center coordinates — SwiftUI hit-test routes the tap to the parent custom view.

---

## TL;DR

- **3 PASSES this session**: TC_CONN_004, TC_CONN_005, TC_CONN_059 (all were April CI failures; TC_CONN_059 was a 420s timeout, now passes in ~16 min)
- **3 fixes pushed**: BaseTest auto-advance (changelog 080) → ConnectionsPage French i18n → IssuesPage diagnostic dump
- **High-impact next step**: review the CI Issues-button DOM dump after the next dev-repo run, write the correct Strategy 2d from observed reality (unblocks 192 Issues tests)
- **Push target**: QA repo `main` only — propagates to dev-repo CI automatically
