# eGalvanic iOS QA practices (adopted 2026-07-14, changelog 122)

## 1. Flaky test management
- The 3-shard rerun IS our flaky detector: a test that fails the run but
  passes the fresh-sim rerun gets the RECOVERED badge = flaky marker.
- **Flakiness rate = RECOVERED / total executed** (run 29135128275: 17/937
  executed ≈ 1.8% — at the <2% target; keep it there).
- Quarantine: `failed-suites/quarantine.txt` feeds the test-result gate
  ("unexpected failures" exclude quarantined ones). A test that flakes twice
  in a week goes in with a ticket reference; it leaves when fixed.
- New tests: run 5× locally (one at a time, per the local driver loop) before
  merging. One unexplained failure = not mergeable.
- iOS flake sources we've PROVEN here: click() no-ops (use W3C press),
  autoAcceptAlerts racing alert polls (withAlertsManual), cells behind the
  translucent tab bar (pressCellAboveTabBar), stacked post-Sign-In popups,
  slow site cold-cache loads. Never sleep — wait on element STATE.

## 2. Failure triage discipline
- Taxonomy (used across docs/failure-triage-*.md): APP_BUG (product),
  AUTOMATION_BUG (our code), ENV_INFRA (sim/WDA/breaker), DATA_FIXTURE,
  INVALID_TEST (wrong expectation). Every failure gets exactly one.
- Reports auto-attach failure screenshots; page-source dumps via
  ArcFlashPage.dumpSource-style helpers where DOM disputes arise.
- The client story is the ROOT-CAUSE COUNT, not the failure count
  ("230 failures, 4 root causes" — that is the honest, credible narrative).

## 3. Test pyramid
- API layer exists (`ApiContractTest`, `ApiDataContractTest`,
  `S3BucketPolicyDriftTest` — no simulator, ~1 min). Push business-logic
  checks there first; UI tests own only what renders.
- Candidate migrations: asset-class/subtype option sets, issue-class enums,
  readiness arithmetic inputs (all verifiable via the sync payload).

## 4. Smoke vs regression
- **Demo Smoke** (`testng-smoke-demo.xml`, CI checkbox `run_smoke_demo`):
  22 proven-stable critical-journey tests, <15 min. Selection rule: critical
  journey + PASSED latest full run + <90s. Flakes twice → leaves the suite.
  Run it fresh the morning of any demo — small, green, and REAL.
- **Regression**: the full `run_all` (nightly / pre-release). Failures create
  triage entries, never report edits. The test-result gate keeps jobs honest.

## 5. Metrics
- Pass-rate trend: consolidated reports (retention now 30 days) per module.
- Flakiness rate: RECOVERED/total per run.
- Mean-time-to-triage: date of failure → date of verdict in triage docs.
- Coverage of critical paths: the smoke suite + parity maps
  (docs/arc-flash-zp2373-ios-parity.md), not raw test counts.

## 6. Report integrity (non-negotiable)
- Reports are generated artifacts of real runs — never edited by hand.
- The gate (`test-result-gate.sh`) fails jobs with real failures; the
  RECOVERED/NOT-RERUN badges preserve what actually happened.
