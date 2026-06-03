# COVERAGE MATRIX (Phase 0)

Built from a read-only parallel audit (5 agents) + this session's live work.
"Covered" = exercised **and** a strong assertion proves the real outcome —
not just "element present." Gaps listed honestly.

## Modules (22 test classes, ~1,950 @Test)

| Module | Tests | Screens/flows | Verification | Notes |
|---|---|---|---|---|
| Authentication | 46 | Welcome, Login, Schedule, session security | MIXED | login + token contract; lockout/empty paths thin |
| Site Selection | 52 | site list, search, sync badge, perf (TC_SS_041) | MIXED | offline/sync partial |
| Asset P1–P6 | 652 | Create/Edit/Detail per class+subtype, CRUD edge | MIXED→STRONG | strong CREATE edge (emoji/dup/boundary); DELETE thin (1/8) |
| Issues P1–P3 | 251 | List/filter/search/create/class/subcat, swipe-delete | MIXED | CREATE title edge gaps; concurrent/offline-conflict missing |
| Location | 82 | Building/Floor/Room CRUD, background-restore (TC_NB_013) | MIXED→STRONG | good required/whitespace/maxlen + double-tap; cascade/dup gaps |
| Connections | 101 | Asset connections (tab hidden in build) | n/a | disabled in build |
| Offline / OfflineSync | 74 | go-offline, queue, multi-site sync, token-exp, DB migration v20→v21 | STRONG(domain) | 18 E2E + 12 partial + 10 scaffolds |
| **Site Visit P1–P3** | 334 | WO list, Session Details, Issues tab, Link Issues, **Locations/Assets-in-Room**, Add Assets, Photo Walkthrough, Files | MIXED + **1 OPEN** | Session Details fixed this session; Locations giant-tree OPEN (B6) |
| ZP-323 | 28 | AI Extract, IR Photo upload | MIXED | now CI-wired this session |
| WorkOrderPlanning | 14 | Plan CRUD/search/totals | STRONG | uses StateIntegrityChecker (real 3-layer-ish check) |

## Test types (Phase 5 A–M)

| Type | Status | Evidence | Gap |
|---|---|---|---|
| A Functional smoke/regression | ✅ REAL | parallel suite, ~1,190+ tests, 20 jobs | — |
| B Test-design rigor (negative/BVA/EP) | ✅ REAL | Asset/Location validation tests | decision-table/state-transition light |
| C Data-driven @DataProvider | ✅ REAL | `ApiContractTest` badLogins (5 variants) + @Parameters | few UI data-providers |
| D API/contract (REST Assured) | ✅ REAL | `ApiContractTest` (7 tests) + `api-contract.yml` + json-schema-validator | schema pins partial |
| E Persistence (on-device + JDBC) | 🟡 PARTIAL | `PersistenceVerifier` (pullFile sqlite/plist) + background-restore tests | **not called by real tests**; no JDBC backend read |
| F Visual regression | ❌ GAP | ScreenshotUtil (diagnostic only) | no baselines/diff (Applitools/pixelmatch); no light/dark |
| G Cross-device/OS matrix | ❌ GAP | single iPhone 16 Pro / iOS 18.x | no iPad/SE, no iOS 16/17, no orientation matrix |
| H Accessibility | 🟡 PARTIAL | `A11yVerifier` (label checks) | **0 real test calls**; no Dynamic Type/contrast |
| I Performance | 🟡 PARTIAL | `PerfVerifier` + scroll tests (TC_SS_041, TC_ISS_090) | **0 real PerfVerifier calls**; no memory/jank/Instruments |
| J Mobile security (MASVS) | 🟡 PARTIAL | `PersistenceVerifier.assertNoPlaintextSecret` + auth token test | **0 real calls**; no MobSF/ATS/pinning |
| K Localization / RTL | ❌ GAP | none | zero locale/RTL/.strings coverage |
| L Interruption/lifecycle | ✅ REAL | OfflineSync (40 UCs), background-restore, permission dialogs, fresh-install | rotation not explicit |
| M Exploratory | 🟡 PARTIAL | `ExploratoryCrawlTest` + Oracle (verifiers) | disabled by default, not in a suite |

## CRUD edge coverage (per entity)

| Entity | CREATE | UPDATE | READ/LIST | DELETE |
|---|---|---|---|---|
| Asset | 7/10 | 3/7 | 5/6 | **1/8** |
| Issue | 6/9 | 2/5 | 6/7 | 4/7 |
| Location/Building | 7/9 | 3/5 | 3/5 | 4/7 |
| Work Order/Session | 3/6 | 2/4 | 6/7 | **0/6** |

Cross-entity GAPS: concurrent/lost-update (none), offline-sync-conflict (minimal),
delete-then-recreate (none), cascade/orphan (none), bulk ops (minimal),
emoji/RTL (minimal outside Asset).

## Biggest honest gaps
1. **CI has no test gate** — failures don't fail the build (fixed this pass).
2. **3 verifiers wired but unused** (A11y/Perf/Persistence) → their bug classes go undetected.
3. **Visual regression, cross-device/OS matrix, localization/RTL** — 0 coverage.
4. **DELETE + concurrency + sync-conflict** edge cases thin across entities.
5. **Site Visit Locations** content detection OPEN (giant-tree, B6).
