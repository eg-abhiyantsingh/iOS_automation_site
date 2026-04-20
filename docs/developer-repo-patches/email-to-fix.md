# Developer Repo — EMAIL_TO Fix (Both Smoke + Parallel Workflows)

**Problem**: Both developer repo workflows use `${{ secrets.EMAIL_TO }}` but the secret was never configured, so emails never send.

**Confirmed failing runs**:
- `release/qa` smoke-login run #24667540029 (2026-04-20) — `Error: At least one of 'to', 'cc' or 'bcc' must be specified`
- Previous: release/prod full smoke (2026-04-19)

**Fastest fix (no code change)**: Set the `EMAIL_TO` secret directly on the dev repo:
1. Go to https://github.com/Egalvanic/eg-pz-mobile-iOS/settings/secrets/actions
2. New repository secret → Name: `EMAIL_TO` → Value: `abhiyantsinghsuas18@gmail.com, abhiyant.singh@egalvanic.com`
3. Save. Next run will pick it up — all branches, no YAML change needed.

**Long-term fix (below)**: Replace `secrets.EMAIL_TO` with a step that reads EMAIL_TO from `AppConstants.java` (your QA code). This way you only maintain email addresses in ONE place — `AppConstants.java` in the QA automation repo.

---

## Fix for: `ios-tests-smoke-repodeveloper.yml`

### Step 1: Add this step BEFORE "Send Email Report" (before line ~888)

```yaml
      - name: Extract Email Recipients
        id: email_config
        run: |
          APPCONST="qa-automation/src/main/java/com/egalvanic/constants/AppConstants.java"
          if [ -f "$APPCONST" ]; then
            EMAIL_TO=$(grep 'public static final String EMAIL_TO' "$APPCONST" | grep -v '^\s*//' | sed 's/.*"\(.*\)".*/\1/' | sed 's/,[[:space:]]*$//')
            echo "EMAIL_TO=$EMAIL_TO" >> $GITHUB_OUTPUT
            echo "Email recipients: $EMAIL_TO"
          else
            echo "⚠️ AppConstants.java not found, using fallback"
            echo "EMAIL_TO=abhiyant.singh@egalvanic.com" >> $GITHUB_OUTPUT
          fi
```

### Step 2: Change line 898 from:
```yaml
          to: ${{ secrets.EMAIL_TO }}
```
### To:
```yaml
          to: ${{ steps.email_config.outputs.EMAIL_TO }}
```

---

## Fix for: `ios-tests-repodeveloper-parallel.yml`

### Step 1: Add this step BEFORE "Send Email" (before line ~3715)

```yaml
      - name: Extract Email Recipients
        id: email_config
        run: |
          APPCONST="qa-automation/src/main/java/com/egalvanic/constants/AppConstants.java"
          if [ -f "$APPCONST" ]; then
            EMAIL_TO=$(grep 'public static final String EMAIL_TO' "$APPCONST" | grep -v '^\s*//' | sed 's/.*"\(.*\)".*/\1/' | sed 's/,[[:space:]]*$//')
            echo "EMAIL_TO=$EMAIL_TO" >> $GITHUB_OUTPUT
            echo "Email recipients: $EMAIL_TO"
          else
            echo "⚠️ AppConstants.java not found, using fallback"
            echo "EMAIL_TO=abhiyant.singh@egalvanic.com" >> $GITHUB_OUTPUT
          fi
```

### Step 2: Change line 3726 from:
```yaml
          to: ${{ secrets.EMAIL_TO }}
```
### To:
```yaml
          to: ${{ steps.email_config.outputs.EMAIL_TO }}
```

---

## Why This Approach?

1. **Single source of truth**: Email addresses only need to be changed in `AppConstants.java`
2. **No secrets needed**: Email addresses aren't sensitive — they shouldn't be GitHub secrets
3. **Same pattern as QA repo**: Your QA repo workflows already do this (see `ios-tests-smoke.yml` line 900)
4. **Works on all branches**: No need to update secrets per branch — the code carries the config

## How to Apply

These changes need to go into the developer repo (`Egalvanic/eg-pz-mobile-iOS`).
The workflow files must be updated on the branch the workflow is dispatched from (typically `release/prod`).

**Note**: PR #199 was already created for the smoke workflow fix (hardcoded approach). 
The approach above (reading from AppConstants.java) is better long-term.
