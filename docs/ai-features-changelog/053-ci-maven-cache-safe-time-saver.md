# 053 — CI Speed-up: Maven Dependency Cache (Safe, Zero Test Risk)

**Date**: 2026-04-30
**Trigger**: User asked to choose the time-saving fix that won't break the test flow.

---

## Context

Investigation in changelog-discussion identified 5 levers for cutting iOS CI runtime:

| Lever | Savings | Blast radius |
|---|---|---|
| #1 `quitDriver` → `@AfterClass` | ~20 min/job | **5 test classes break** (Connections, SiteSelection chained tests, Issue_Phase1-3, Location, SiteVisit_phase1-3) |
| #2 `NO_RESET = true` default | ~6-10 min/job | **Breaks Auth + SiteSelection** — they explicitly need clean install |
| #3 Shard issues-p1 | ~50% wall clock | New testng XML + workflow input — moderate |
| #4 **CI dependency caches** | **~3 min/job startup** | **Zero — pure GitHub Actions config** |
| #5 Reduce implicit waits + sleeps | ~3-5 min/job | Test stability — needs measurement |

User asked for the **safest time-saver**. #1 was vetoed (5-class blast radius confirmed by lifecycle audit). #2 was vetoed after I checked: every test class except AuthenticationTest + SiteSelectionTest already calls `setNoReset(true)` in `@BeforeClass`, and those two are explicitly documented as needing clean install. Flipping the default would break them.

**#4 ships today** — workflow YAML only, no test code touched.

---

## Change

Added `cache: 'maven'` to **every** `actions/setup-java@v4` step across all 6 iOS workflows. The setup-java action's built-in Maven cache:

- Keys the cache on `pom.xml` hash automatically
- Caches `~/.m2/repository` (typically 50-80 MB of Appium Java Client + Selenium + TestNG + dependencies)
- Restores transparently before `mvn test-compile` / `mvn test`
- Falls back gracefully on cache miss (first run after pom change)

| Workflow | setup-java steps cached |
|---|---|
| `ios-tests-parallel.yml` | 17 |
| `ios-tests-quick-verify.yml` | 7 |
| `ios-tests-repodeveloper-parallel.yml` | 16 |
| `ios-tests-smoke-repodeveloper.yml` | 1 |
| `ios-tests-smoke.yml` | 1 |
| `ios-tests.yml` | 1 |
| **Total** | **43** |

All 6 YAML files validated via `yaml.safe_load()`.

---

## Expected Impact

- **First run after this commit**: cache miss on every job — same time as before, populates cache
- **Subsequent runs (assuming pom.xml unchanged)**: each job saves ~30-60s on Maven dep resolution
- **Matrix parallel runs**: savings multiply by job count (17 jobs in `ios-tests-parallel.yml`) — but wall clock impact depends on which jobs are on the critical path

For a single targeted job (assets-p1 or issues-p1): ~30-60s saved per run.
For a `job_selection=all` parallel run: ~3-5 min wall clock saved.

---

## What Was Deliberately NOT Changed

- **`quitDriver` lifecycle** — too risky per the audit. Will revisit as Option B (single-class proof-of-concept on Asset_Phase1) only after this lower-risk change is confirmed in CI.
- **`NO_RESET` default** — would break Auth + SiteSelection.
- **Implicit wait constant** — could mask real slowness in flexible-locator code paths (e.g., `tapOnIssue` 134-fallback chain). Needs measurement before reducing.
- **Appium / WDA caches** — more complex (need a stable cache key for the xcuitest driver version), deferred.

---

## Validation

- `mvn -q test-compile` → clean
- All 6 workflow YAMLs parse via Python `yaml.safe_load`
- `git diff` shows only `cache: 'maven'` additions, no other config drift

Next CI run on `main` will populate the caches. The run AFTER that will demonstrate the time savings.
