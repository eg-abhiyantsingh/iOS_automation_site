# Changelog 026 — Centralize Email Recipients from AppConstants.java

**Date:** 2026-04-17
**Scope:** All 6 workflow files + AppConstants.java as single source of truth

---

## Problem

Email recipients were **hardcoded** in every workflow file. Adding or removing a recipient meant editing 5-6 `.yml` files manually. Easy to miss one and end up with inconsistent recipient lists.

## Solution

All workflow files now **extract `EMAIL_TO` at runtime** from a single source of truth:

```
src/main/java/com/egalvanic/constants/AppConstants.java — line 211
```

```java
public static final String EMAIL_TO = "dharmesh.avaiya@egalvanic.com, abhiyant.singh@egalvanic.com, ";
```

### How It Works

Each workflow's email-sending job now has two new steps:

1. **Checkout Code (for email config)** — lightweight `sparse-checkout` of just `AppConstants.java`
2. **Extract Email Recipients** — `grep` + `sed` extracts the value, sets it as `$GITHUB_OUTPUT`

The `to:` field then uses `${{ steps.email_config.outputs.EMAIL_TO }}`.

### Extraction Command

```bash
EMAIL_TO=$(grep 'public static final String EMAIL_TO' \
  src/main/java/com/egalvanic/constants/AppConstants.java \
  | sed 's/.*"\(.*\)".*/\1/' \
  | sed 's/,[[:space:]]*$//')
```

- Finds the uncommented `EMAIL_TO` line
- Extracts the string between quotes
- Trims trailing comma/whitespace

---

## Files Changed

| Workflow | Job | Change |
|----------|-----|--------|
| `ios-tests.yml` | `ios-test` | Added extraction step (checkout already existed) |
| `ios-tests-smoke.yml` | `send-email` | Added checkout + extraction |
| `ios-tests-quick-verify.yml` | `send-email` | Added checkout + extraction |
| `ios-tests-parallel.yml` | `send-email` | Added checkout + extraction |
| `ios-tests-repodeveloper-parallel.yml` | `summary` | Added QA repo checkout + extraction |
| `test-email.yml` | `send-test-email` | Added checkout + extraction |

### Developer Repo Special Case

The developer-repo workflow runs in `Egalvanic/eg-pz-mobile-iOS`, not the QA repo. It checks out the QA repo into `qa-config/` using `QA_REPO_TOKEN`:

```yaml
- name: Checkout QA Repo (for email config)
  uses: actions/checkout@v4
  with:
    repository: eg-abhiyantsingh/iOS_automation_site
    token: ${{ secrets.QA_REPO_TOKEN }}
    path: qa-config
    sparse-checkout: src/main/java/com/egalvanic/constants/AppConstants.java
```

---

## How to Update Recipients

**One line, one place:**

Edit `src/main/java/com/egalvanic/constants/AppConstants.java` line 211:

```java
public static final String EMAIL_TO = "person1@egalvanic.com, person2@egalvanic.com";
```

Commit and push. All workflows (smoke, full, quick-verify, developer-repo, test-email) will automatically pick up the new recipients on their next run.

---

## Diagnostic Lines Updated

All `echo "To: ..."` lines in email summaries now show the dynamically extracted value instead of a hardcoded address. Zero hardcoded `@egalvanic.com` addresses remain in any `.yml` file.
