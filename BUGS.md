# BUGS & DEFECTS LOG

Triage categories (per the hardening mandate):
**(1) flaky/env**, **(2) framework/test defect**, **(3) REAL PRODUCT BUG.**
Project convention (memory `feedback_our_code_not_dev_app`): when a test fails,
default to **our automation** having the defect, not the production app —
borne out below: this session's failures were overwhelmingly **category 2**
(locator/detection defects), fixed at root. No test was ever made to pass by
weakening assertions.

---

## Category 2 — framework/test defects (FIXED at root cause, verified live)

### B1 — Work Order entry name/date always null  ✅ fixed `70c59c9`/`26cf82e`
- **Symptom:** TC_JOB_006 failed `"Work order entry should have a name displayed - Object is null"` despite 31 entries on screen.
- **Root cause:** `getWorkOrderName`/`getWorkOrderDate` scanned `XCUIElementTypeCell`; the v1.36 SwiftUI build renders **zero** cells (rows are `Other`/`StaticText`). Title+date are one combined label, e.g. `"Work Order - Jun 2, 4:03 AM"`.
- **Fix:** SwiftUI-aware title extraction (`getWorkOrderTitleElements`) + date parsed from the combined label. **Verified live: name/date extracted.**

### B2 — Session Type / Started always null  ✅ fixed `26cf82e`
- **Symptom:** TC_JOB_082/083 `Got: null`.
- **Root cause:** `getSessionType` searched label `"Session Type"`, but the app labels it **`"IR Photo Type"`** (value e.g. `FOTRIC`), in a SwiftUI *stacked* row (value ~21px below the label), not same-row.
- **Fix:** `valueStackedBelowLabel()` + correct label aliases. **Verified live: `FOTRIC`, `2 June 2026 at 8:41:55 PM`.**

### B3 — Session Details detection false-positive on Dashboard  ✅ fixed `c7dab04`
- **Symptom:** session tests "passed"/ran on the wrong screen; `ensureOnSessionDetailsScreen()` returned early on the dashboard.
- **Root cause:** `isSessionDetailsScreenDisplayed()` matched `label=='Tasks'` && `label=='Issues'` — but the **dashboard Quick-Action tiles are literally "Tasks" and "Issues."**
- **Fix:** require markers UNIQUE to Session Details (`WORK ORDER DETAILS` / `IR Photo Type` / `Quick QR Action`). **Verified live: TC_JOB_018/082/083 PASS.**

### B4 — Site Visit suites: "Driver not initialized" before any test  ✅ fixed `625479b`
- **Root cause:** `initPageObjects()` (@BeforeMethod) built page objects with no self-heal; on IDE single-test runs the driver wasn't up yet → `IllegalStateException` in `BasePage`'s constructor.
- **Fix:** catch + `DriverManager.initDriver()` + retry (the LocationTest pattern), in all 3 phases.

### B5 — State-dependent dashboard navigation  ✅ fixed `c7dab04`
- **Root cause:** `clickNoActiveJobCard()` only matched "No Active"/"Tap to select" text — failed once a job was active (card text changes).
- **Fix:** state-agnostic `clickWorkOrderCard()` (tall `WO`-prefixed card) + `ensureSessionDetailsOpen()` (activate+open to a known state).

---

## Category 2 — OPEN (quarantined, real fix scoped, not masked)

### B6 — Locations/Assets tab: building/floor/room detection returns 0  ⚠️ OPEN
- **Affects:** ~19 phase-1 tests (TC_JOB_076/077/078/080 + the 084–099 Assets-in-Room cascade) **and** phase-2 Add-Assets (TC_JOB_100+) — all share this code path.
- **Root cause (measured live):** the Assets/Locations tab renders a **giant, deeply-nested SwiftUI tree**. Whole-tree queries don't work: `getPageSource` and tree-wide `findElements` hit the 90s HTTP cap. Capping `snapshotMaxDepth`: depth ≤20 completes (31s) but building rows are nested **deeper than 20**; depth 30/35/45 all **time out**. No depth both completes and reaches the buildings; buildings are also below ~6 pages of scroll.
- **Status:** navigation half FIXED (`c7dab04`/`0d7915a` — reaches the tab in a session). Content detection needs a non-snapshot technique: **`mobile: scroll` + `predicateString`** (the project's documented deep-element approach) or a raised HTTP readTimeout. Documented in memory `sitevisit-locations-giant-tree`.
- **NOT masked:** these tests legitimately fail today; tracked here + in the dated failed-suite, never weakened to green.

---

### B11 — App picker has no "Loadcenter" class → LC tests retargeted to "Load"  ✅ mostly resolved (`dd90c78`)
- **Resolution (per product decision):** the picker offers **"Load"** (no "Loadcenter").
  `changeAssetClassToLoadcenter()` now selects "Load" via the searchable picker
  (`selectClassViaSearch`). Live-verified the "Load" class has **Size** but NOT
  Ampere Rating / Fault Withstand / Serial Number / Columns / Manufacturer / Voltage
  (those were Panelboard/MCC copy-paste errors). Those LC_EAD tests became truthful
  absence assertions.
- **6/8 LC tests PASS live:** LC_EAD_10/12/14/18 (absence) + LC_EAD_19 (Size edit) +
  LC_EAD_25.
- **2 remaining (minor follow-up):** `LC_EAD_02` — the "Load" edit form has no
  "Core Attributes" section (`isCoreAttributesSectionVisible` false); `LC_EAD_23` —
  the Size-only "save all required" is state-flaky (random Size can equal the current
  value → no Save). Both are Load-class shape mismatches, not the class-change bug.

---

### B11-orig (superseded) — original note below for history
- **Affects:** all 7 Loadcenter tests (`LC_EAD_10/12/14/18/19/23/25`) — they fail at
  `changeAssetClassToLoadcenter()`, before any field logic.
- **Root cause (measured live 2026-06-05):** the Asset Class picker is a SEARCHABLE
  dropdown (confirmed via screenshot + live). Selection now types into the
  "Search..." field (`selectClassViaSearch`, commit `d9ce693`). Searching the live
  picker shows a **"Load"** class but **no "Loadcenter" / "Load Center"** option:
  typing `center` surfaces nothing; typing `Load` surfaces only `Load`. So the
  app's picker does not offer "Loadcenter" — the LC tests target a class name the
  app doesn't have.
- **Gold vs app:** the node_classes gold lists BOTH `Load` (no core attrs; subtypes
  General/Resistive Load) and `Loadcenter` (Size, Mains Type) as distinct classes,
  but the live picker only offers `Load`. Either the class was renamed/merged to
  `Load` (→ retarget the 7 LC tests + `changeAssetClassToLoadcenter` to `Load`) or
  `Loadcenter` is genuinely missing from the build (→ real product bug). **Needs
  product confirmation before retargeting.**
- **CORRECTION:** an earlier note claimed "gold is incomplete for Loadcenter (lists
  2 of 7 live fields)" — that was WRONG: that field dump ran right after a *failed*
  class-change, so it read a different class. Disregard it.
- **Status:** NOT masked — `LC_EAD_12` carries the truthful "Columns is not a
  Loadcenter field" assertion but is RED here (blocked by the missing class).

## Category 2 — audit-surfaced framework gaps

### B9 — Strong verifiers exist but are NEVER called by real tests  ⚠️ OPEN
- **Finding (verifier-usage audit):** `A11yVerifier`, `PerfVerifier`,
  `PersistenceVerifier` have **0 real test call-sites** (only unused `BaseTest`
  wrappers); `AssetLoadVerifier` is used only by the disabled `ExploratoryCrawlTest`.
- **Impact:** the bug classes these catch — unlabeled controls (a11y), launch/
  watchdog regressions (perf), on-device data corruption + MASVS plaintext
  secrets (persistence), blank/failed PDF/IR renders (asset) — are **invisible**
  to the regular suite despite the detectors existing.
- **Fix (scoped, top-1 next step):** call them in the flows whose outcome they
  prove — `AssetLoadVerifier` on PDF/IR-photo screens, `PersistenceVerifier`
  after create/edit/delete, `A11yVerifier`/`PerfVerifier` on key screens.

### B10 — CI build stays green even when tests fail  ✅ fixed this pass
- **Finding (CI audit):** `continue-on-error: true` on every test step + no gate
  → real test failures never fail the workflow (email is the only signal). This
  is the "green suite finds no bugs" problem at the pipeline level.
- **Fix:** added a `test-gate` job that parses all module `testng-results.xml`
  after the run and **fails the workflow if total failures > 0** — while keeping
  `continue-on-error` on the run steps so reports/emails still upload.

## Category 2 — coverage/CI defects (FIXED)

### B7 — 28 tests never ran in CI  ✅ fixed `68bad4e`/`c9cde2e`
- `ZP323_NewFeatures_Test` (28 tests) was in **no** parallel suite / no CI job → never executed. Added `testng-zp323.xml` + a `zp323-tests` job to **both** parallel workflows.

### B8 — CI cascade-skips + 6h platform cancel  ✅ fixed `1b67970`/`9eb3b99`
- Offline `perTestTeardown` hung 840s on a dead session → 39 cascade-skips (bounded now); Site Visit 344-test mega-suite exceeded the 6h cap (split into 3 parallel phase jobs); a blanket 5-min per-test cap would have failed ~19 legitimately-slow passing tests (raised to 8 min).

---

## Category 1 — environment (resolved, not product/test defects)
- **E1:** Xcode was in the Trash → `xcode-select` fell back to Command-Line Tools → "Unknown device or simulator UDID". Recovered without sudo (rescued Xcode to `~/Applications`, ran Appium with `DEVELOPER_DIR`, persisted in `~/.zshrc`). Not a product/test bug.

---

## Category 3 — REAL PRODUCT BUGS

### ATS-VAL-01 — Create button enabled for spaces-only asset name (confirmed 2026-07-02, v1.48)
- **Repro:** New Asset → Name = `"     "` (spaces only) → fill class + location →
  Create stays **ENABLED** (and creates the asset).
- **Expected:** blank/whitespace-only names rejected (Create disabled or validation error).
- **Test:** `ATS_ECR_07_verifyNameWithOnlySpaces` — SKIPs with this bug id per QA
  direction (2026-07-02); auto-passes once fixed.

### ATS-VAL-02 — Asset name stored untrimmed (confirmed 2026-07-02, v1.48)
- **Repro:** create asset named `"  TrimNNN  "` → stored/displayed with the leading
  and trailing spaces intact (CI evidence: `Actual: '  Trim595015  '`); also breaks
  exact-name search/lookup.
- **Expected:** leading/trailing whitespace trimmed on save.
- **Test:** `ATS_ECR_10_verifyNameTrimming` — SKIPs with this bug id per QA direction
  (2026-07-02); auto-passes once fixed.

### SLD source audit 2026-06-11 — 8 confirmed bugs (SLD-1..8) + 7 suspicions
Full write-up with file:line evidence and repro steps:
`docs/sld-bug-audit-2026-06-11.md`. Highlights:
- **SLD-1 (HIGH):** view→view link navigation never re-renders the WKWebView
  (`WebViewBridge.updateUIView` is empty; coordinator closures frozen) → stale
  diagram + node drags written to the WRONG view's mapping.
- **SLD-2 (HIGH):** offline enclosure resize dropped in sync replay —
  `SyncExecutionService.swift:1110` omits width/height the queue carries.
- **SLD-3 (HIGH):** site switch never deletes `Mapping*SLDView` rows →
  duplicate mappings accumulate; nodes snap back to stale positions.
- **SLD-4/5/6 (MED):** ghost edge after rejected connection; unfiltered
  full-site graph pushed into view-filtered webview during edits; webview
  search hook is English-only.
- **SLD-7 (MED, security):** Agent web app calls a hardcoded unauthenticated
  API Gateway endpoint with no tenant scoping.
- **SLD-8 (LOW, security):** `webView.isInspectable = true` in release builds.

### Prior session note
None **confirmed** this session — failures traced to automation defects (per
the convention above). One item to **watch** (possible product concern, not
yet confirmed a bug): the Assets/Locations session tab's element tree is
pathologically large/deep (see B6); if that reflects the real view hierarchy
it may be a UI-performance smell worth a dev look, but it could equally be
normal SwiftUI nesting — **not filed as a product bug without stronger evidence.**
