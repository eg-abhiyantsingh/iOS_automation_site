# Changelog 016 — Smoke Workflow: Email Toggle + Full Suite Branch Trigger

**Date**: 2026-04-16  
**Time**: ~12:00 IST  
**Prompt**: Add option to send email or not, and option to run full suite on my branch in GitHub Actions

---

## Summary

Enhanced the smoke and parallel workflow triggers with two capabilities:

1. **`send_email` control on parallel workflow** — The parallel workflow (`ios-tests-parallel.yml`) now has a `send_email` dropdown, matching the smoke workflow. When the smoke workflow triggers the full suite, it passes the email preference through.

2. **`run_full_suite_after` enhanced** — Changed from `true/false` to `never/if_passed/always`. Now you can run the full suite on your branch even if smoke tests fail.

---

## What Already Existed (No Changes Needed)

### `send_email` on Smoke Workflow

The `send_email` dropdown was already added in Changelog 012. It works like this:

```yaml
# In ios-tests-smoke.yml (lines 76-82)
send_email:
  description: 'Send email with test results'
  default: 'true'
  options:
    - 'true'
    - 'false'
```

The email step checks this:
```yaml
if: github.event.inputs.send_email != 'false' && secrets.EMAIL_USERNAME != ''
```

When you set `send_email` to `false`, the email step is skipped entirely.

---

## Change 1: Enhanced `run_full_suite_after` (Smoke Workflow)

### Before

```yaml
run_full_suite_after:
  description: 'Run full test suite after smoke tests pass'
  default: 'false'
  options:
    - 'true'    # Only runs if smoke PASSES
    - 'false'   # Never runs
```

**Problem**: If you wanted to run the full suite on your branch but smoke had a flaky failure, you couldn't — the full suite was gated on `smoke_result == 'passed'`.

### After

```yaml
run_full_suite_after:
  description: 'Run full parallel suite on this branch after smoke'
  default: 'never'
  options:
    - 'never'      # Don't trigger full suite
    - 'if_passed'  # Only if smoke tests pass (old 'true' behavior)
    - 'always'     # Run full suite regardless of smoke result
```

### Trigger Condition

```yaml
# Before: simple boolean check
if: needs.smoke-tests.outputs.smoke_result == 'passed' && 
    github.event.inputs.run_full_suite_after == 'true'

# After: handles all three modes
if: |
  always() &&
  github.event.inputs.run_full_suite_after != 'never' &&
  (
    github.event.inputs.run_full_suite_after == 'always' ||
    (github.event.inputs.run_full_suite_after == 'if_passed' && 
     needs.smoke-tests.outputs.smoke_result == 'passed')
  )
```

### How the Full Suite Runs on Your Branch

When you trigger the smoke workflow from GitHub Actions:

1. **Select your branch** in the GitHub UI dropdown (top of the "Run workflow" dialog)
2. Set `run_full_suite_after` to `if_passed` or `always`
3. The smoke workflow runs on your branch
4. The `trigger-full-suite` job calls `ios-tests-parallel.yml` with `ref: context.ref` — which is YOUR branch
5. The full parallel suite runs on your branch

```
GitHub Actions UI → Select branch "feature/my-work"
  → Smoke tests run on feature/my-work
  → trigger-full-suite job fires
    → createWorkflowDispatch({
        workflow_id: 'ios-tests-parallel.yml',
        ref: 'refs/heads/feature/my-work',    ← YOUR branch
        inputs: { job_selection: 'all', send_email: 'true' }
      })
  → Full suite runs on feature/my-work
```

---

## Change 2: `send_email` on Parallel Workflow

### Before

The parallel workflow (`ios-tests-parallel.yml`) had no `send_email` input. It ALWAYS sent email after every run — no way to disable it.

### After

Added `send_email` dropdown matching the smoke workflow:

```yaml
# In ios-tests-parallel.yml inputs section
send_email:
  description: 'Send email with test results'
  default: 'true'
  options:
    - 'true'
    - 'false'
```

The Send Email step now checks this:
```yaml
# Before: always sends if credentials exist
if: ${{ secrets.EMAIL_USERNAME != '' }}

# After: respects send_email preference
if: github.event.inputs.send_email != 'false' && secrets.EMAIL_USERNAME != ''
```

### Pass-Through from Smoke

When the smoke workflow triggers the full suite, it passes the `send_email` preference:

```javascript
// In trigger-full-suite job
await github.rest.actions.createWorkflowDispatch({
  workflow_id: 'ios-tests-parallel.yml',
  ref: branch,
  inputs: {
    job_selection: 'all',
    send_email: sendEmail || 'true'  // ← passed from smoke
  }
});
```

This means: if you set `send_email: false` on the smoke workflow, the full suite (if triggered) also won't send email.

---

## Files Changed

| File | Change |
|------|--------|
| `.github/workflows/ios-tests-smoke.yml` | `run_full_suite_after`: `true/false` → `never/if_passed/always`; trigger condition updated; passes `send_email` to full suite |
| `.github/workflows/ios-tests-parallel.yml` | Added `send_email` input (default: `true`); Send Email step now respects it |

---

## Usage Examples

### Run smoke + email on QA branch

```
Branch: qa
send_email: true
run_full_suite_after: never
→ Runs smoke on QA, sends email, stops
```

### Run smoke + full suite on feature branch, no email

```
Branch: feature/my-work
send_email: false
run_full_suite_after: always
→ Runs smoke on feature/my-work (no email)
→ Triggers full suite on feature/my-work (no email)
```

### Run smoke, full suite only if passed

```
Branch: qa
send_email: true
run_full_suite_after: if_passed
→ Runs smoke on QA
→ If smoke passes → triggers full suite (with email)
→ If smoke fails → no full suite triggered
```

---

## Key Concepts

### `workflow_dispatch` Branch Selection

When you click "Run workflow" in GitHub Actions, there's a **branch dropdown** at the top. This determines:
- Which branch's code is used (the `.yml` file, Java code, etc.)
- The value of `context.ref` / `github.ref` in the workflow

Any workflow with `on: workflow_dispatch` can be triggered from any branch. You don't need a special input for branch selection — GitHub provides it natively.

### `createWorkflowDispatch` API

The `trigger-full-suite` job uses the GitHub REST API to trigger another workflow:

```javascript
github.rest.actions.createWorkflowDispatch({
  owner: 'Egalvanic',
  repo: 'eg-pz-mobile-iOS',
  workflow_id: 'ios-tests-parallel.yml',
  ref: context.ref,           // The branch to run on
  inputs: { ... }             // Workflow inputs
});
```

The `ref` parameter determines which branch the triggered workflow runs on. By passing `context.ref`, the full suite runs on the same branch as the smoke workflow.

### Why `always()` is Needed in the Condition

GitHub Actions `if:` conditions on jobs default to `success()` — they only run if all dependent jobs succeeded. The `always()` function overrides this, making the condition evaluate regardless of the dependent job's result. Without it, `run_full_suite_after: 'always'` wouldn't actually work when smoke fails.

---

## Status

- Smoke `send_email`: Already existed (Changelog 012)
- Parallel `send_email`: **ADDED** (new input + condition)
- `run_full_suite_after`: **ENHANCED** (`never`/`if_passed`/`always`)
- Email pass-through: **ADDED** (smoke → parallel)
