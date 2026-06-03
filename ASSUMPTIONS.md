# ASSUMPTIONS & EXECUTION LOG

Autonomous hardening pass — interpretation decisions logged here (per the
"when ambiguous, pick the reasonable option and keep going" directive).

## Environment / structural constraints (shape what's verifiable)
1. **Single iOS simulator + single Appium server.** Live test execution is
   strictly **serial** — it cannot be parallelized across agents (they would
   collide on the one device). So: code authoring + static analysis + reports
   are done broadly; **live runs are sampled/representative**, not a full
   re-run of all ~1,950 tests in one pass. What was actually run live vs.
   authored-and-compiled-only is stated explicitly in SUMMARY.md.
2. **A concurrent session is actively editing this same repo** for the same
   mandate (evidence: the `verify/` package grew to 10 files and `pom.xml`
   gained REST Assured + json-schema-validator between my reads). To avoid
   the duplicate-file collisions that already happened once (my parallel
   `verify/` classes had to be discarded in favour of theirs), this pass
   **does not re-author the verifier package or test classes another session
   owns.** It focuses on non-colliding, additive value: the required reports
   (COVERAGE/SUMMARY/BUGS), verification that the existing infra actually
   runs, CI wiring, and specific gaps.
3. **External API egress is sandbox-blocked** in this harness; API tests run
   only with the sandbox override (or in CI/real terminals). Documented.
4. **Offline-first app**: the UI reads local SwiftData, so backend/API state
   does NOT immediately reflect in the UI without a sync. State-integrity
   "three-layer" checks (UI ↔ device store ↔ backend) must account for this;
   API seeding alone won't change what the UI shows. (See
   reference_qa_backend_api + sitevisit-locations-giant-tree memories.)

## What already existed before this pass (not re-built)
- `src/main/java/com/egalvanic/verify/`: CrashDetector, AssetLoadVerifier,
  UIStateValidator, StateIntegrityChecker, A11yVerifier, PerfVerifier,
  PersistenceVerifier, ImageAnalysis, VerificationError (+ VerifierSelfTest).
- `pom.xml`: REST Assured + json-schema-validator (API/contract testing) + Gson.
- `TestDataApi` (authenticated QA backend client) — login verified live.
- 22 test classes / ~1,950 @Test methods across the modules.
- `.github/workflows/ios-tests-parallel.yml` + `…-repodeveloper-parallel.yml`
  (parallel CI; ZP-323 + WorkOrderPlanning jobs; per-module emails).
- `.github/scripts/build-failed-suite.py` (dated failed-test suite + history).

## Decisions this pass
- Treat the request as: **consolidate + verify + report + close honest gaps**,
  not regenerate existing code. Deliverables: COVERAGE.md, SUMMARY.md,
  BUGS.md, this file.
- Bugs found this session are real product/automation issues and are filed in
  BUGS.md with repro — never masked.
