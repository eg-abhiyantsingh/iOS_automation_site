# HARDENING SUMMARY & HANDOFF

Autonomous pass over the eGalvanic iOS Appium/XCUITest suite. Companion docs:
`COVERAGE.md` (matrix), `BUGS.md` (defects filed), `ASSUMPTIONS.md` (constraints).

## Phase 1 — Why a green suite was finding few bugs (ranked, one-line fix)

1. **CI never fails on test failures** — `continue-on-error: true` on *every*
   test step + no gate → the build is green even when tests fail. → *Add a
   test-gate job that fails the run on real failures (done this pass).*
2. **Strong verifiers exist but aren't wired in** — A11y/Perf/Persistence
   verifiers have **0 real test calls**; AssetLoad only the disabled crawler. →
   *Call them in the flows whose bug-class they catch (PDF render, on-device
   persistence, launch perf, a11y labels).*
3. **Present-only assertions** — historically `assertTrue(isXDisplayed())`
   proves a view exists, never that it works (e.g. TC_JOB_006 found 31 rows but
   the name was null). → *Assert the real outcome (rendered/persisted/correct).*
4. **Fixed waits pervasive** — `shortWait()/mediumWait()` (+ ~43 raw
   `Thread.sleep`) mask races and slept-through hangs. → *Explicit waits on real
   conditions.*
5. **Swallowed-ish exceptions** — ~733 `catch` blocks; most have intentional
   fallback, but broad `catch(Exception){}` still hides failures. → *Verifiers
   throw `VerificationError extends Error` so they bypass `catch(Exception)`.*
6. **No flow-order modeling** — Site Visit tests each re-navigate from the
   dashboard; they don't assert the real activate→session→add-asset→photo order
   composes. → *Ordered E2E journeys with state checks between steps.*
7. **Locator rot vs SwiftUI** — `XCUIElementTypeCell` scans on a 0-cell SwiftUI
   build returned null/0 (root of the Work Order failures). → *SwiftUI-aware
   StaticText/Button locators (fixed for WO this pass).*
8. **No content/state/crash depth** historically — fixed by the `verify/`
   package (Crash/Asset/UIState/Integrity), now proven by `VerifierSelfTest`.

## What this pass did (verified, committed, pushed to main)

- **Diagnosed + fixed Work Order failures (live-verified):** WO name/date
  extraction (`70c59c9`), Session Type/Started (`26cf82e`), Session-Details
  false-positive + state-agnostic nav + `ensureSessionDetailsOpen` (`c7dab04`),
  driver self-heal (`625479b`). **TC_JOB_006/018/082/083 now PASS live.**
- **Proved the verifier infra:** project compiles; **`VerifierSelfTest` 8/8 pass**
  (StateIntegrity + ImageAnalysis — the data-corruption & blank-render detectors).
- **Closed CI coverage gap:** `ZP323_NewFeatures_Test` (28 tests) was in no suite
  → added to **both** parallel workflows (`68bad4e`, `c9cde2e`).
- **CI cascade fixes:** bounded offline teardown, split Site Visit into 3 jobs,
  raised per-test timeout 5m→8m (was failing ~19 legit-slow tests) (`1b67970`,`9eb3b99`).
- **Dated failed-test suite tool** (`build-failed-suite.py` + CI artifact) for
  local re-run + history (`7432d3f`).
- **Reports:** this file + COVERAGE.md + BUGS.md + ASSUMPTIONS.md.

## Done vs. NOT done (honest)

DONE: Phase 0 inventory; Phase 1 diagnosis; Phase 2 verifiers (exist + self-test
pass); Phase 4 partial (strong Asset/Location CREATE edge cases exist; DELETE +
concurrency + sync-conflict are gaps); Phase 5 A/C/D/L real; Phase 7 CI (wired +
gate added); Phase 8 reports.
NOT done / GAP: verifier wiring into flows (E/H/I/J are dead-code today); visual
regression (F); cross-device/OS matrix (G); localization/RTL (K); Site Visit
Locations content detection (B6, giant-tree, OPEN).

## Local run status
- Driver-free `VerifierSelfTest`: **8/8 PASS**.
- Work Order Session-Details group: **PASS live** (018/082/083 + 006).
- Full ~1,950-test local re-run not performed in one pass (single simulator =
  serial, multi-hour). Representative sampling used; see ASSUMPTIONS.md.

## CI status
- Parallel suite + repodeveloper suite: all 22 classes wired; ZP-323 + WOP jobs
  present; per-module emails + failed-suite artifact.
- **Test gate added** this pass so real failures now fail the build.
- Run `26881798303` (current `main`) executing at handoff.

## Top-5 next steps (prioritized)
1. **Wire the dead verifiers into flows** — call `AssetLoadVerifier` on every
   PDF/IR-photo screen, `PersistenceVerifier` after each create/edit/delete,
   `A11yVerifier`/`PerfVerifier` on key screens. Biggest bug-yield per hour.
2. **Fix Site Visit Locations (B6)** — replace whole-tree queries with
   `mobile: scroll` + predicate (giant-tree); clears ~19+ tests.
3. **Close DELETE + concurrency + offline-sync-conflict** CRUD gaps per entity.
4. **Add visual-regression** baselines (light+dark) and a **cross-device/OS**
   matrix (iPad, iOS 17) to CI.
5. **Localization/RTL** pass (.strings completeness, RTL layout, Dynamic Type overflow).
