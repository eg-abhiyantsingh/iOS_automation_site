# Suite 2 (developer repo) — add the ZP-ticket job

**Why this is a patch and not a commit:** "Automation suite 2" is `ios-tests-repodeveloper-parallel.yml` in the
developer repo `Egalvanic/eg-pz-mobile-iOS`. It builds the app from the chosen branch and runs THIS repo's suite
XMLs (checked out with `QA_REPO_TOKEN`). QA automation never pushes to the developer repo, so the change is
prepared here for a developer (or the repo owner) to apply.

The copy of `ios-tests-repodeveloper-parallel.yml` in this repo is NOT the one GitHub runs for suite 2 — it ran
once (May 18) and has drifted. Editing it would change nothing.

## What the patch adds

| Change | Detail |
|---|---|
| New job `zp-tickets-tests` | 5-shard matrix, same suites as suite 1's `zp3927-3928-tests` job: `zp3927`, `zp3928`, `zp3928-forms`, `zp3914-am`, `zp3914-forms` |
| `job_selection` options | `zp-tickets-only` (all 5 shards) and `zp3914-only` (the 2 ZP-3914 shards) |
| `all` | now also runs the ZP-ticket job |
| Summary | waits for the new job, counts it (16 jobs instead of 15), adds a "ZP Tickets" row to the email table |

The job is a copy of the existing `sitevisit-tests` job (same build-app artifact, simulator boot, WDA warm-up,
report upload), so nothing new is needed in secrets or runners.

## How to apply (in a clone of eg-pz-mobile-iOS)

```bash
git checkout release/qa
git apply /path/to/iOS_automation_site/docs/developer-repo-patches/2026-10-21-suite2-zp-tickets-job.patch
git diff --stat   # .github/workflows/ios-tests-repodeveloper-parallel.yml | ~270 lines
```

Checked on 2026-10-21: `git apply --check` is clean against `release/qa` at `2396c69` (v1.70 promote).

## How to run after it is merged

Actions → "Automation Test - Parallel" → branch `release/qa` → `job_selection` = `zp3914-only` (or
`zp-tickets-only`). Because suite 2 builds the app from source, this is the run that exercises v1.70 (double-tap
zoom in TC_PM_07 un-skips itself on 1.70+).
