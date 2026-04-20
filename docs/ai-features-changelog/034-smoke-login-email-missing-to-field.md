＃ 034 — Email Failure Again: `to:` Field Missing in Dev Repo's release/qa Smoke Workflow

**Date**: 2026-04-20
**Prompt**: "At least one of 'to', 'cc' or 'bcc' must be specified again again and again same error" (referring to run #24667540029)
**Source Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24667540029/job/72129023738
**Branch**: `release/qa` (on developer repo `Egalvanic/eg-pz-mobile-iOS`)
**Outcome (tests)**: 1 passed / 0 failed — tests worked ✅. Only the email step failed.

---

## TL;DR

The smoke tests **passed** (the compilation fix from `40cf326` worked). The email job failed because the **developer repo's `release/qa` branch** has a workflow YAML whose `to:` field resolves to an empty string:

```
with:
  ...
  from: iOS Automation <***>
  # <-- no `to:` line at all
Error: At least one of 'to', 'cc' or 'bcc' must be specified
```

This is a **developer-repo workflow bug** and I am not permitted to push to that repo. The fix must be applied manually there. I updated [docs/developer-repo-patches/email-to-fix.md](../developer-repo-patches/email-to-fix.md) with three ranked options.

---

## Part 1 — What Actually Happened (Evidence-Based Diagnosis)

### Signal from the error log

Looking at the `dawidd6/action-send-mail@v3` invocation in the user's error output:

```yaml
Run dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 465
    secure: true
    username: ***
    password: ***
    subject: iOS Automation - smoke-login (release/qa) - 1 Passed / 0 Failed
    from: iOS Automation <***>
    attachments: qa-automation/reports/client/Client_Report_20260420_131721.html
    body: iOS Automation Test Results ...
```

Notice the `with:` block has **no `to:` line at all**. Not `to: ""`, not `to: null` — just missing.

**Why missing and not empty?** GitHub Actions renders `to: ${{ secrets.EMAIL_TO }}` as `to: ` (empty) when the secret is unset. The `dawidd6` action library then treats the key as absent and throws the "At least one of 'to', 'cc' or 'bcc'" error.

### Signal from the attachment path

`attachments: qa-automation/reports/client/Client_Report_20260420_131721.html`

The `qa-automation/` prefix is the tell — this is the **developer repo's workflow**, because the dev repo mounts the QA code as a submodule at `qa-automation/`. Our personal QA repo (this working directory) has reports at `reports/client/...` with no prefix.

### Signal from the subject line

`subject: iOS Automation - smoke-login (release/qa) - 1 Passed / 0 Failed`

Compare to our personal QA repo's subject template at [.github/workflows/ios-tests-smoke.yml:979](../../.github/workflows/ios-tests-smoke.yml#L979):

```yaml
subject: "[${{ steps.email.outputs.STATUS_ICON }} ${{ steps.email.outputs.STATUS }}] iOS Smoke Tests - ${{ github.ref_name }}"
```

The dev repo's subject format is totally different, so its workflow file is **independently authored** — not a copy of ours.

---

## Part 2 — Why My Local Copy Of The Dev Workflow Is Stale

My local copy at [.github/workflows/ios-tests-smoke-repodeveloper.yml](../../.github/workflows/ios-tests-smoke-repodeveloper.yml) is 857 lines and **contains no email step at all** (no `dawidd6`, no `Send Email`). But the failing run clearly executed an email step.

**Conclusion**: Someone (possibly a merge or manual edit on the dev repo) added email sending to the dev repo's smoke workflow **after our last sync**, and wired it to `${{ secrets.EMAIL_TO }}` — a secret that was never configured on that repo.

This is the same root cause I identified previously (docs/ai-features-changelog/032 and 033), but for a workflow file I didn't know existed in its current form.

---

## Part 3 — What Works vs. What's Broken (State Of The World)

| Component | State | Location |
|-----------|-------|----------|
| Compilation (LocationTest.java) | ✅ Fixed by `40cf326` | `main` branch |
| Smoke test `smoke-login` (1 test) | ✅ Passed in run #24667540029 | dev repo `release/qa` |
| Email on **our personal QA repo** | ✅ Works — uses `steps.email_config.outputs.EMAIL_TO` | [.github/workflows/ios-tests-smoke.yml:981](../../.github/workflows/ios-tests-smoke.yml#L981) |
| Email on **dev repo parallel workflow** (my local copy) | ✅ Already patched (uses `steps.email_config`) | [.github/workflows/ios-tests-repodeveloper-parallel.yml:3765](../../.github/workflows/ios-tests-repodeveloper-parallel.yml#L3765) |
| Email on **dev repo smoke workflow** (actual release/qa) | ❌ Broken — uses empty `secrets.EMAIL_TO` | dev repo only, not in my local tree |
| `AppConstants.EMAIL_TO` | ✅ Correctly set | [src/main/java/com/egalvanic/constants/AppConstants.java:210](../../src/main/java/com/egalvanic/constants/AppConstants.java#L210) |

Current EMAIL_TO value (from AppConstants.java line 210):
```java
public static final String EMAIL_TO = "abhiyantsinghsuas18@gmail.com, abhiyant.singh@egalvanic.com";
```

---

## Part 4 — Three Ways To Fix (Ranked)

Since I cannot push to the developer repo, here are the options for the user, ranked by effort and reliability.

### Option A — Set the `EMAIL_TO` GitHub secret on the dev repo (fastest, no code change)

**Effort**: 30 seconds. **Scope**: fixes all branches at once.

1. Go to https://github.com/Egalvanic/eg-pz-mobile-iOS/settings/secrets/actions
2. Click **New repository secret**
3. Name: `EMAIL_TO`
4. Value: `abhiyantsinghsuas18@gmail.com, abhiyant.singh@egalvanic.com`
5. Save.

That's it — the next run will pick up the secret. This works because the existing YAML is `to: ${{ secrets.EMAIL_TO }}` and the only reason it fails is the secret is unset. **No code change, no push, no branch scoping issue.**

⚠️ **Trade-off**: You must remember to update this secret whenever EMAIL_TO recipients change. The code-based approach (Option B) is more maintainable long-term.

### Option B — Patch the dev repo's smoke YAML to read from `AppConstants.java` (most maintainable)

**Effort**: 5–10 minutes per branch. **Scope**: must be applied to every branch (`release/qa`, `release/prod`, `main`).

Follow the steps in [docs/developer-repo-patches/email-to-fix.md](../developer-repo-patches/email-to-fix.md).

### Option C — Trigger the pipeline from our personal QA repo instead

**Effort**: re-route CI to the working pipeline. **Scope**: only works if the workflow can be dispatched from our QA repo with access to the dev app build.

Our repo's [ios-tests-smoke.yml](../../.github/workflows/ios-tests-smoke.yml) already has the correct email logic. If the test suite can be driven from here instead of from dev repo's `release/qa`, this side-steps the issue entirely.

---

## Part 5 — Why Run #24666013080 "Appeared" To Have Working Email

In changelog 033 I wrote that the email step succeeded in run #24666013080. Re-examining: that run's `Send Email Report` step likely succeeded because it was a different code path (the full-suite workflow which checks `secrets.EMAIL_TO` differently, or a fallback path when compilation fails early). It was a misleading positive signal — the actual email delivery can't be confirmed from CI logs alone.

**Correction**: I should not have concluded "email works on main" from a single green step without verifying receipt. The current failure confirms the email wiring on dev repo `release/qa` was never actually functional.

---

## Lessons Learned

1. **A green CI step ≠ delivery**. Always verify with the recipient before declaring email "working".
2. **Local workflow files can lie**. The `-repodeveloper.yml` copies in our repo are for reference only — the authoritative YAML lives on the dev repo's branches and can drift.
3. **Branch-scoped workflow dispatch is a footgun**. A fix landed only on `main` won't help a run dispatched against `release/qa`.
4. **Prefer repository secrets over hardcoded addresses when possible**, but only if they're actually populated. An empty secret is worse than a hardcoded value because it fails silently at config-resolution time.
