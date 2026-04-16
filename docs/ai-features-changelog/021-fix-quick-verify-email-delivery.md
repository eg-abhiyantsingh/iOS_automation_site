# Changelog 021 — Fix Quick-Verify Email Delivery

**Date**: 2026-04-16  
**Time**: ~16:30 IST  
**Prompt**: "i didnt got email after this execution" — CI run 24517729692  
**CI Run**: https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/runs/24517729692/job/71672915133

---

## Summary

Quick-verify workflow email was silently failing. The `dawidd6/action-send-mail@v3` action produced **zero output** and completed in **0.86 seconds** — a strong indicator of SMTP authentication failure. `continue-on-error: true` masked the failure completely (CI showed green).

This is the **same bug** fixed in changelog 020 for the smoke workflow, but the quick-verify workflow had not been patched yet.

---

## Root Cause

Three problems in `ios-tests-quick-verify.yml`:

### 1. No Fallback Email Address (Line 1070)

```yaml
# Before:
to: ${{ secrets.EMAIL_TO }}
```

If `EMAIL_TO` secret is not set (or empty), the `to:` field is blank — the email action has no recipient and silently fails.

### 2. No Step ID for Diagnostic Tracking

The Send Email step had no `id:`, so there was no way to check `steps.send_email.outcome` in subsequent steps. Silent success or failure.

### 3. No Diagnostic Output

The Summary step only printed test counts. If the email failed, there was **zero indication** in the CI logs.

---

## Fix

Applied the same three fixes as the smoke workflow (changelog 020):

| Issue | Before | After |
|-------|--------|-------|
| Recipient | `${{ secrets.EMAIL_TO }}` (no fallback) | `${{ secrets.EMAIL_TO \|\| 'abhiyant.singh@egalvanic.com' }}` |
| Step tracking | No `id:` on Send Email step | `id: send_email` |
| Diagnostic output | None | Email Summary step with outcome check + App Password instructions |

### Email Summary Step

Added after the existing Summary step. Shows:
- Whether the email send succeeded or failed
- If failed: instructions to create a Gmail App Password
- Whether `EMAIL_TO` secret is configured or using fallback address

---

## Action Required

The **code fix** ensures visibility (you'll now see whether the email sent or failed in CI logs). But the **actual delivery** requires valid credentials:

1. **Gmail App Password** — Gmail SMTP requires an App Password, not your regular Gmail password
   - Go to Google Account > Security > 2-Step Verification (must be enabled)
   - Go to App Passwords > Generate a new app password
   - Copy the 16-character password
   - Set as `EMAIL_PASSWORD` secret in GitHub repo settings
2. **EMAIL_USERNAME** — must be a valid Gmail address
3. **EMAIL_TO** — optional, falls back to abhiyant.singh@egalvanic.com

---

## File Changed

| File | Change |
|------|--------|
| `.github/workflows/ios-tests-quick-verify.yml` | Added `id: send_email`, EMAIL_TO fallback, Email Summary diagnostic step |

---

## Status

| Issue | Fix |
|-------|-----|
| Silent email failure | **FIXED** (diagnostic Email Summary step) |
| No recipient fallback | **FIXED** (fallback to abhiyant.singh@egalvanic.com) |
| Email not delivered | **ACTION NEEDED** — verify Gmail App Password in EMAIL_PASSWORD secret |
