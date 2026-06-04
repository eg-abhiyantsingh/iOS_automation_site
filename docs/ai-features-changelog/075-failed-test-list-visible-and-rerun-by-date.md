# 075 — Make the failed-test list visible, and re-runnable by date in CI

**Date:** 2026-06-04
**Trigger:** *"i cant see failed test case yml file or any way to get the list of
all fail test cases"* → then: *"a dropdown with a specific date in GitHub Actions
[to] run all the failed test cases of that run directly, and track real-fail vs
script-fail."*

## Why you couldn't see it (root cause)
The failed-suite mechanism existed but was **invisible and never ran**:
1. It only produced a **30-day downloadable artifact** (`failed-suite`) — never a
   file in the repo, so browsing the repo/runs showed nothing.
2. **No completed run had exercised it** (added Jun 3 evening; every completed run
   predated it).
3. Worse — it lived in the `send-email` job (`if: always()`), but a **job-level
   `if:` is overridden by a run-level cancellation**. Your runs are often
   manually/6h-cancelled, so it would have been skipped anyway.

## What shipped
### 1. The list is now committed to the repo (visible + tracked)
`failed-suites/` now holds, after every run:
- `latest.xml` — newest run's failures (stable file to run)
- `failed-<YYYY-MM-DD>.xml` — that day's failures
- `history.md` — date | count | run link | per-class breakdown (authoritative date list)

**Seeded now** from run `26881798303`: **113 failures on 2026-06-03 across 13
classes** (SiteVisit_phase1:40, SiteSelection:16, Asset_P3:13, Issue_P1:12, …).
So there's a real list to see/run today.

### 2. `failed-suite-collector.yml` — cancel-proof commit-back (NEW)
A `workflow_run: completed` workflow on the parallel suite. It fires **after the
run finishes regardless of conclusion — including cancelled** (the fix for the
job-level-`if` problem). It downloads the finished run's artifacts, regenerates
the failed-suite, and commits `failed-suites/` to `main`.
- `concurrency` group serializes collectors; commit is conflict-safe (reset to
  `origin/main` + regenerate + retry — the generator is the merge logic).
- Loop-safe: **no workflow triggers on `push`** (verified), so committing can't
  re-fire anything.

### 3. `rerun-failed-by-date.yml` — re-run a date's failures in CI (NEW)
Actions → "Rerun Failed Tests (by date)":
- **`failed_date`** dropdown (`latest` + recent dates) and **`custom_date`** free
  text for any date in `history.md`.
- A fast ubuntu `resolve` job validates the date (strict `YYYY-MM-DD`, via `env:`
  — no injection), counts tests, and **skips the macOS job if 0 failures** (no sim
  spin-up). The macOS `rerun` job boots the sim and runs only that suite, then
  uploads a report (stack traces + screenshots) to triage **real vs script** fails.

### 4. Generator hardening (`build-failed-suite.py`)
- A **green run no longer clobbers `latest.xml`** (keeps the last real list).
- `--run` sets the history label in `--from` mode; **history is idempotent** per
  run id (re-processing replaces, never duplicates).

## Adversarial verification (6-lens workflow) — 6 blockers caught & fixed
Before committing I ran a 6-agent review. It caught, and I fixed:
1. **Cancellation skips commit-back** → moved to the `workflow_run` collector.
2. **`COUNT` garbled to a multi-line `$GITHUB_OUTPUT`** (`grep -c … || echo 0`
   double-prints on 0 matches) → `grep -c … || true`.
3. **Same-day concurrent commit race silently dropped data** → `concurrency` group
   + reset-and-regenerate commit loop.
4. **First use blocked (no files yet)** → seeded real data + graceful resolve.
5. **Static dropdown goes stale** → reframed dropdown as convenience; `history.md`
   + `custom_date` are the supported path.
6. **Empty suite reported false success** → skip the run on 0 failures; preserve
   `latest.xml`.

## How to use
- **See the list:** open `failed-suites/history.md` or `failed-suites/latest.xml`.
- **Run all failures locally:** `mvn test -DsuiteXmlFile=failed-suites/latest.xml`
  (or `failed-suites/failed-2026-06-03.xml`).
- **Run a date's failures in CI:** Actions → "Rerun Failed Tests (by date)" → pick
  `latest` / a date / type `custom_date` → review the report to triage real-vs-script.
