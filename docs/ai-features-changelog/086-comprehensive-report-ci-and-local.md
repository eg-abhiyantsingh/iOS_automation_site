# 086 — Comprehensive report: CI/CD + local (2026-06-17)

Per request: fix what's fixable, with a proper report covering **both CI and local**.

## Executive summary
- **Local automation is unblocked** — Xcode 26.3 reinstalled + license accepted; a real
  test runs end-to-end (driver→WDA→app). This was the multi-day blocker.
- **A real, validated UI fix shipped**: the asset-class picker no longer does a per-element
  round-trip storm — `GEN_01`, a prior CI **6-minute hang**, now **passes in 1m55s** locally.
- **Proper API testing now exists**: 18 pure-HTTP tests (auth + data-contract + integrity),
  **0 failures in 30s, no simulator** — the reliable, fast path that sidesteps the flaky UI.
- **Root cause of the worst UI failures reframed**: the Assets/Issues subtype hangs are
  largely a **navigation bug** (the test stays on the Asset List, never opens the edit
  screen) + WDA wedging — *not* purely a "giant-DOM slow query."

## Local validation (complete)
| Check | Result |
|---|---|
| Toolchain | ✅ Xcode 26.3, `simctl`+`xcodebuild` work, license accepted |
| Auth smoke | ✅ `TC01_verifyWelcomeScreenUILoads` PASS (13.5s) |
| Class-picker fix | ✅ `GEN_01` PASS 1m55s (was a 6m0s CI hang); no picker-button budget bail |
| **API suite** | ✅ **18 run / 0 fail / 3 skip in 30s** (auth + data-contract + integrity) |

## CI baseline (run 27584402854, pre-fix — the giant-DOM/WDA-wedge era)
~581 PASS / ~370 FAIL / ~580 SKIP. Dominant mechanism: heavy a11y queries wedge WDA →
6m0s hangs → session death → mass-skip cascades.

| Module | P | F | S | deaths | Module | P | F | S | deaths |
|---|--|--|--|--|---|--|--|--|--|
| Authentication | 44 | 4 | 0 | 0 | Issues P1 | 42 | 26 | 47 | 1 |
| Site Selection | 43 | 1 | 8 | 0 | Issues P2 | 2 | 8 | 44 | 40 |
| Assets P1 | 39 | 16 | 46 | 34 | Issues P3 | 55 | 23 | 9 | 47 |
| Assets P2 | 21 | 11 | 90 | 7 | Connections | 32 | 56 | 9 | 2 |
| Assets P3 | 83 | 14 | 0 | 0 | Location p1 | 1 | 12 | 29 | 30 |
| Assets P4 | 69 | 5 | 29 | 7 | Location p2 | 2 | 29 | 14 | 43 |
| Assets P5 | 37 | 12 | 52 | 12 | Offline | 15 | 5 | 58 | 3 |
| Assets P6 | 22 | 19 | 86 | 18 | Site Visit p1 | 57 | 4 | 39 | 6 |
| ZP-323 | 9 | 7 | 12 | 0 | Site Visit p2/p3 | 1/0 | 63/55 | 0 | 3/66 |
| Work Order | 7 | 0 | 8 | 0 | | | | | |

Healthy (0 deaths): Auth, Site Selection, Assets P3, Work Order, ZP-323. Worst: Location +
Site Visit p2/p3 (near-total wipeouts), Issues P2.

A **fresh CI run (27700096140)** on the class-picker fix is in progress to measure broad
impact across the 6 Asset suites (all do class changes; GEN_01 was one of many such hangs).

## Fixes shipped this session
| Commit | Fix | Evidence |
|---|---|---|
| `b3960a5` | Class-picker scoped `IN` predicate (kills per-element round-trip storm) | GEN_01 6min→1m55s local |
| `ce21a37` | `ASSET_CLASSES` aligned to live 43 (typo `Lightning`→`Lighting`, +missing) | web ground-truth |
| `1903e87` | **API data-contract+integrity tests** + `TestDataApi` SLD-source fix | 18/0/30s local |

## Key discoveries (deep-debug findings)
1. **Navigation bug, not giant-DOM**: page-source dumps proved the subtype tests run while
   the app is on the **Asset List** (never opened an asset) — `openSharedAssetForEditOrFallback`
   silently fails to open the edit screen on iOS 26.2, and the code proceeds anyway. The 44s
   "giant query" was a transient bleed-through state, not the common case.
2. **API SLD-source bug**: `firstSldId()` used `/users/{id}/slds` (returns `[]` for admin);
   real source is `/me`'s `accessible_sld_ids`. Fixed → data layer usable.
3. **`ASSET_CLASSES` drift**: iOS set was missing ~13 live classes + had a typo; the
   picker-button locator silently failed for those classes.
4. **Ground-truth values** (Playwright on the web app): 43 asset classes, 4 Motor subtypes
   (incl. `Low-Voltage Machine (<= 200hp)`), 11 OSHA subcategories — now available verbatim.

## Recommendation (the strategic pivot)
**Shift the data-integrity / contract assertions to the API layer** (done — `ApiDataContractTest`):
fast (30s), deterministic, no macOS-runner contention. Keep the iOS UI suite for genuine
interaction/rendering, and fix its **navigation reliability** (open-edit-screen verification +
the iOS-26.2 splash/login flakiness) rather than chasing per-screen query costs. The
giant-DOM bleed-through itself is an app-side SwiftUI artifact (separate prod repo).

## Remaining work
- Verify the open-edit-screen contract in `openSharedAssetForEditOrFallback` (fail-fast/retry
  if the asset didn't open) — the real fix for the Assets/Issues subtype cluster.
- Expand API tests to write-paths (create/update asset+issue) once those endpoints are mapped.
- Measure run 27700096140 to quantify the class-picker fix's broad impact.
