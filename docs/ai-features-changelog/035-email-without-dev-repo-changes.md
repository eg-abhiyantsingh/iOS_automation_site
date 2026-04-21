＃ 035 — Email Fix Without Dev Repo Changes (Cross-Repo Email Sender)

**Date**: 2026-04-21
**Prompt**: "can we do this without making any change in developer repo"
**Context**: After repeated email failures (runs #24667540029, #24673078422) on dev repo's `release/qa` branch, and a firm rule to never push to the dev repo, we needed a way to deliver the test-result emails entirely from this QA repo.

---

## Summary

Created [.github/workflows/send-smoke-email-from-dev-run.yml](../../.github/workflows/send-smoke-email-from-dev-run.yml) — a `workflow_dispatch` workflow in YOUR QA repo that:

1. Takes a dev repo run ID/URL as input
2. Fetches run metadata from the dev repo via GitHub API
3. Downloads artifacts (test reports, TestNG XML) using `gh run download`
4. Parses test counts from `testng-results.xml`
5. Reads `EMAIL_TO` directly from [AppConstants.java line 210](../../src/main/java/com/egalvanic/constants/AppConstants.java#L210) — **no EMAIL_TO secret**
6. Sends the email with the Client Report HTML attached

Zero changes to `Egalvanic/eg-pz-mobile-iOS`. The broken email step on the dev repo side can be safely ignored — your real email now arrives via this new workflow.

---

## Part 1 — Why This Approach

You explicitly said: *"dont want secret key... use directly AppConstants.java line 210"* and *"can we do this without making any change in developer repo"*.

| Constraint | Solution |
|---|---|
| No push to dev repo | Workflow lives entirely in YOUR QA repo |
| No GitHub `EMAIL_TO` secret | Reads EMAIL_TO from AppConstants.java line 210 via `grep` |
| No ongoing file-level maintenance | Change line 210, next run picks it up automatically |

**One-time setup** (explained in Part 3): you do need a **PAT (Personal Access Token)** added as a secret called `DEV_REPO_TOKEN` — because GitHub's default `GITHUB_TOKEN` can only see the repository it runs in, not the dev repo. This is NOT an `EMAIL_TO` secret — it's a read-only cross-repo access token, set once and forgotten.

---

## Part 2 — User Flow

After any dev repo CI run finishes:

1. Copy the run URL or run ID from the dev repo Actions page (e.g. `https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24673078422`)
2. Go to **your QA repo** → Actions tab → "Send Email for Dev Repo Smoke Run"
3. Click **Run workflow**, paste the URL or ID, click Run
4. Email arrives within ~60 seconds

The dev repo's own email step will still fail (that's its YAML, we don't touch it) — just **ignore that error**. Your actual email comes from this workflow.

---

## Part 3 — One-Time Setup (PAT)

GitHub Actions' auto-issued `GITHUB_TOKEN` only scopes to the current repo. Reading artifacts from `Egalvanic/eg-pz-mobile-iOS` needs a PAT:

1. Go to https://github.com/settings/personal-access-tokens/new
2. Choose **Fine-grained personal access token**
3. **Resource owner**: Egalvanic
4. **Repository access**: Only select repositories → `eg-pz-mobile-iOS`
5. **Permissions** (Repository permissions):
   - `Actions` → **Read-only**
   - `Contents` → **Read-only**
   - `Metadata` → **Read-only** (auto-selected)
6. Generate the token, copy it
7. In YOUR QA repo (`eg-abhiyantsingh/iOS_automation_site`) → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
   - Name: `DEV_REPO_TOKEN`
   - Value: paste the PAT

That's it. The PAT is scoped to read-only, one repo, so blast radius is minimal.

---

## Part 4 — Security Hardening Applied

The `PreToolUse` security hook flagged command injection risk on the first draft where I interpolated `${{ github.event.inputs.* }}` directly inside `run:` blocks. Fixed with:

1. **Inputs piped through `env:`** block, then accessed as shell vars (`$INPUT_RUN`, `$INPUT_DEV_REPO`)
2. **Regex validation** on `dev_repo`:
   ```bash
   if ! echo "$INPUT_DEV_REPO" | grep -qE '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$'; then
     echo "ERROR: dev_repo must be in owner/repo format" >&2
     exit 1
   fi
   ```
   Rejects anything with shell metacharacters — no way to smuggle `; rm -rf /` into a `gh api` call
3. **Numeric-only extraction** of RUN_ID: `grep -oE '[0-9]{8,}'` — strips anything non-numeric from input

Without these, a malicious `workflow_dispatch` invocation could inject shell commands via the input fields. With them, the workflow is safe even from a compromised trigger source.

---

## Part 5 — Alternative: Automatic Polling (Not Implemented)

Instead of manual dispatch, the workflow could run on `schedule: - cron: '*/15 * * * *'` and auto-email any completed dev repo run it hasn't emailed yet. This needs a state file (tracked runs) to avoid duplicate emails. Skipped for now — the manual `workflow_dispatch` model is simpler and more predictable.

If you want automatic polling later, say the word and I'll add:
- Cron-based trigger
- State persistence (via repo artifact or a committed `last-emailed-run-id.txt`)
- Dedup logic

---

## Part 6 — What This Does NOT Do

- **Doesn't silence the broken email step on the dev repo.** That step will keep failing on every dev repo CI run because its YAML uses `${{ secrets.EMAIL_TO }}` (empty). The only way to fix that is to touch the dev repo. This workflow sends the REAL email from our side — ignore the dev repo step's red X.

- **Doesn't auto-trigger.** You manually click "Run workflow" after a dev repo run finishes. That's the trade-off for not touching the dev repo — we can't have it notify us.

---

## Files Changed

- **New**: [.github/workflows/send-smoke-email-from-dev-run.yml](../../.github/workflows/send-smoke-email-from-dev-run.yml) — the cross-repo email sender
- **New**: [docs/ai-features-changelog/035-email-without-dev-repo-changes.md](035-email-without-dev-repo-changes.md) — this changelog

No existing files modified.
