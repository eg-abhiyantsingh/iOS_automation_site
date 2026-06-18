# 090 — Add S3 bucket-policy drift as a selectable option in the parallel suite

- **Date:** 2026-06-18
- **Prompt:** "i cant see any option to run s3 bucket test case in ios parallel suite"
- **Workflow:** `.github/workflows/ios-tests-parallel.yml`
- **Type:** CI ergonomics (expose an existing test as a dispatch option).

---

## Ask

The parallel suite's Run-workflow form had no way to run the S3 bucket-policy drift test
(`S3BucketPolicyDriftTest`, ZP-774). It only ran bundled inside the Smoke suite — there was no
checkbox to run it on its own.

## Constraint

`workflow_dispatch` allows at most **10 inputs**, and the parallel suite was already at 10
(`run_all`, `run_smoke`, `run_auth_site`, `run_assets`, `run_issues`, `run_connections_location`,
`run_sitevisit_workorders`, `run_offline_zp323`, `run_api`, `send_email`). No room for an 11th.

## Change

S3 drift is a **no-simulator, pure-AWS** check — exactly like the existing `api-contract-tests`
job (no-sim HTTP/cloud contract checks). So it's grouped with that toggle rather than spending a
new input slot:

- **Renamed the `run_api` checkbox** → *"API contract + S3 bucket-policy drift (no simulator,
  ~1 min)"*. Ticking it (or `run_all`) now runs both no-sim check jobs.
- **Added a dedicated `s3-drift-tests` job** (`ubuntu-latest`, no simulator), gated
  `if: ${{ inputs.run_all || inputs.run_api }}`. It mirrors the proven AWS pattern from
  `ios-tests-smoke.yml`: `aws-actions/configure-aws-credentials@v4` (secrets `AWS_ACCESS_KEY_ID` /
  `AWS_SECRET_ACCESS_KEY`, region `us-east-2`) → `aws sts get-caller-identity` verify →
  `mvn test -DsuiteXmlFile=src/test/resources/smoke/testng-smoke-s3drift.xml` (all 5 environments,
  42 checks) → upload report. `S3PolicyChecker` already uses the default credential chain when
  `AWS_ACCESS_KEY_ID` is set (no named profile needed in CI).

The S3 job is its own job with its own pass/fail + `s3-drift-report` artifact; a real drift fails
the overall run. The summary/email job is intentionally **not** touched — it covers only the
simulator-based modules and already excludes the sibling `api-contract-tests`, so S3 is treated
consistently.

## Why not a standalone `run_s3` checkbox

That would be an 11th input (over the GitHub cap). Folding S3 under the no-simulator `run_api`
toggle is logical (both are non-app cloud/contract checks) and keeps it within the limit. To make
S3 fully standalone later, another toggle would have to be merged to free a slot.

## Graceful on missing/invalid AWS creds

`aws-actions/configure-aws-credentials` self-verifies via STS and would **hard-fail** the job (and
the whole run) when the repo's AWS creds are missing/expired. Selecting S3 shouldn't break the run
over an environment-creds issue, so: the configure step is `continue-on-error`, a `Verify AWS
Access` step does the real `aws sts get-caller-identity` check, and the mvn step runs **only if it
passes**. If creds are invalid, the job **succeeds with a loud `::warning::`** ("add valid
AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY") instead of a cryptic red — consistent with not failing
the run spuriously. With valid creds, S3 runs for real and a true drift fails the run.

## Validation (live dispatch)

- YAML parses; 10 inputs (≤ cap); `s3-drift-tests` gated on `run_all || run_api`.
- Suite XML confirmed to target `S3BucketPolicyDriftTest` (group `smoke`, all 42 checks, no Appium).
- **Live `run_api=true` dispatch:** the S3 job ran (option works) and **all 20 simulator modules
  correctly skipped** — selection wiring proven. The S3 job initially failed at the AWS step with
  *"The security token included in the request is invalid"* — the repo's `AWS_ACCESS_KEY_ID` /
  `AWS_SECRET_ACCESS_KEY` secrets are **invalid/expired** (same class of issue as changelog 072).
  That's an env fix the owner must make (rotate the read-only cicd creds); the graceful-skip change
  above stops it from hard-failing the run in the meantime.
- (The pre-existing `api-contract-tests` job also failed — separate/unrelated to this change.)
