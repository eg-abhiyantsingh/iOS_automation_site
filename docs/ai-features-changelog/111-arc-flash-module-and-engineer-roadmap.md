# 111 — Arc Flash module (19 tests, live-validated) + engineer-module completion roadmap

**Date:** 2026-07-08
**Prompt:** "complete arc flash module and full engineer module too" + "check in ci cd
what test cases are failing for engineer library" (under the standing deep/divided/
Playwright/heat-and-trial working method).

## CI answer first (Asset Engineer / Equipment Library, run 28933339687)

Job "Asset Engineer (105 tests)" completed FAILURE on iOS 18.5:
- **PASSED (3):** TC_ENG_001 (card presence), TC_ENG_040 (Add-Asset modes/class
  picker), TC_ENG_100 (card a11y-name contract) — exactly the tests that don't
  need the library.
- **FAILED (47 methods ≈ 102 cases):** TC_ENG_002-004, 010-016, 020-023, 030,
  050-051 (39-row class matrix), 060-063, 070-074, 080-091, 101-104, 110-114 —
  every test that taps the library card or needs the Engineering section.
- **Single root cause:** `eng-lib` flag still absent from acme QA
  `company_features` (re-verified live 3× today; a NEW `emp-v2` flag appeared,
  so platform IS editing flags — eng-lib just hasn't returned). The run started
  09:46 UTC, BEFORE the changelog-110 gate landed (10:15 UTC), so it ran
  ungated. Future dispatches skip these in ~0s with `BLOCKED (environment)`.

## Domain research (web, Playwright headed — per working method)

Web `/arc-flash` "Arc Flash Readiness" = NFPA 70E data-completeness dashboard:
3 ring metrics (Asset Details = % of required asset fields filled;
Source/Target = % of source-requiring assets with an inbound connection;
Connection Details = % of required connection fields filled) + per-class
completion cards + per-asset tables (ATS requires Interrupting Rating, Ampere
Rating, Mains Type, Voltage). Captured live: "24 assets • 14 of 96 required
fields completed = 15%", "116 require source • 22 connected • 94 missing = 19%".
Sidebar also live-confirms the eng-lib blocker: "Equipment Designations" is
disabled with the exact same "Engineering Library isn't enabled" tooltip while
"Arc Flash Readiness" is enabled → **arc flash is NOT eng-lib gated**.

## iOS Arc Flash surface (recon workflow over app-source; NONE flag-gated)

1. Site tab "Arc Flash" quick action → full-screen **"Arc Flash Analysis"**
   (ArcFlashCompletionView): Readiness Score ring, "{n} Completed/{n}
   Remaining/{n} Total Items", 3 metric cards ("X of Y" + %), "<Metric>
   Breakdown" disclosure buckets (closed label set 0%, 1-25%, 26-50%, 51-75%,
   76-99%, 100%), drill-through to asset/edge editors, Done.
2. Assets tab ellipsis menu "Show/Hide AF Punchlist" → bolt badges per
   `node.af_isComplete`.
3. Connections tab — same toggle → check/x badges per `edge.af_isComplete`.
4. Session room detail: overlay filter None/IR/Arc Flash/C.O.M. (persisted) +
   long-press "Collect AF Data" → EditNodeDetailView focus mode (legacy layout
   while eng-lib off).
5. "Arc Flash Sticker" photo category (deferred — see follow-ups).

## What was built (all compile + live-validated on iPhone 17 Pro Max)

- **`pages/ArcFlashPage.java`** — locators from exact AppStrings values;
  W3C-press tap primitive; last-visible-match rule; document-order percent
  parsing; `dumpSource()` diagnostics; punchlist/ellipsis/badge helpers;
  overlay-filter + long-press + Collect-AF-Data helpers.
- **`ArcFlash_Test` (13)** — dashboard with ARITHMETIC-INVARIANT oracles:
  Completed+Remaining==Total (live: 51+17=68), card %==round(X/Y×100) (live
  order [ring 75, 53, 9, 100]), overall==totals-weighted average, Total Items
  == Σ card totals, breakdown switching, closed bucket-label set, bucket
  expansion, Done/reopen determinism. **13/13 PASSED locally, zero locator
  fixes needed** — the app-source-derived contract was exact.
- **`ArcFlashPunchlist_Test` (6)** — toggle flips Show→Hide, badges 0→8
  (assets) and 0→6 (edges) live-verified, badge removal to exactly 0; session
  overlay + Collect AF Data guarded by skipIfPreconditionMissing (local site's
  active WO has no rooms with assets — they exercise on CI SiteVisit fixtures).
- **`AssetEngineerFlagCanary_Test` (1, TC_ENG_130)** — eng-lib flag ⇄ UI
  consistency canary, deliberately NOT gated: flag OFF → asserts the disabled
  caption + normal subtitle (PASSED live today, also first live validation of
  the changelog-110 caption locator); flag ON → asserts caption absent + alert
  opens. Screams on flag/UI drift in either direction.
- **Wiring:** `parallel/testng-arc-flash.xml`; root `testng.xml` "Arc Flash
  Tests" block; `ios-tests-parallel.yml` — new `run_arc_flash` checkbox + full
  `arc-flash-tests` job (cloned from the asset-engineer template), added to
  both `needs:` lists and both SELECTION format strings; YAML schema-validated.
- **Full-suite local run: 20 tests → 18 PASS / 2 fixture-SKIP / 0 FAIL (9m45s);
  verifier self-tests 34/34.**

## Engineer-module completion roadmap (blocked on eng-lib; execute when restored)

Gap analysis (recon workflow, ranked G1-G13; full detail in the session
transcript & summarized here for execution):
- **G1 (CRITICAL):** no engineering edit is ever SAVED — binding/save/reopen
  persistence path has zero coverage; needs one writable fixture per shape.
- **G2 (CRITICAL):** SkmTripConfigCard (breaker Frame/Sensor/Plug pickers,
  trip-unit auto-resolve, segments editor, 10-segment cap) untested; needs a
  bound Circuit Breaker fixture (create via TC_ENG_113 panel + match tap).
- **G3:** Ground Fault flows (GF toggle, sibling pair, GroundFaultPickerSheet,
  "Add Anyway"). **G4:** custom-equipment SAVE + Edit-custom hydrate round-trip.
- **G5:** match-pick backfill values (pole_count=3, has_trip_unit MCP nuance
  ZP-2478). **G6:** trip-type mutual-exclusion laws. **G7:** offline matching +
  download-failure states (offline-priority rule applies). **G8-G13:**
  manufacturer narrowing (ZP-2457), cable/busway pickers, match pagination,
  voltage pickers, configurator interactions, misc.
- Build order: G1+G2 share the bound-CB prerequisite and unlock G3/G5/G6.
- Deliberately NOT written blind today: these are deep interactive flows whose
  locators must be iterated against the live Engineering DOM (the module's own
  history proves guessing dies); with the flag off, every gated test skips and
  FEATURE_GATE_OFF only shows the disabled UI. The canary flips green-side
  automatically the day the flag returns — that is the trigger to execute this
  roadmap with the heat-and-trial loop.

## Follow-ups

1. **Platform team must re-enable eng-lib** (action item unchanged; canary +
   gate now make the state self-evident in every run).
2. TC_AF_026 "Arc Flash Sticker" photo-category test — deferred: the picker
   path is camera-adjacent (simulator UIImagePickerController crash,
   BUGS.md/app-camera-crash-simulator); needs a careful library-only path
   explored live first.
3. Session fixture: seed an active WO with rooms+assets on the first QA site so
   TC_AF_024/025 run locally, not just on CI.
4. Consider a web↔iOS metrics cross-check via TestDataApi (same site) — the
   dashboard's Asset Details % should agree with web /arc-flash for the same
   site once a shared fixture site is pinned.
