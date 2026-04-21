#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════
# One-shot script: fix email in dev repo workflow via PR.
# Safe — creates a new branch and opens a PR. Does NOT push to release/qa.
# Run this yourself in your terminal. Takes ~2 minutes.
# ═══════════════════════════════════════════════════════════════════════════

set -euo pipefail

WORKDIR="/tmp/dev-repo-email-fix"
BRANCH="fix/email-to-from-appconstants"
BASE_BRANCH="release/qa"
REPO="Egalvanic/eg-pz-mobile-iOS"

echo ">> Step 1: Cloning dev repo (read-only intent)..."
rm -rf "$WORKDIR"
git clone --depth=1 --branch="$BASE_BRANCH" "https://github.com/$REPO.git" "$WORKDIR"
cd "$WORKDIR"

echo ""
echo ">> Step 2: Finding broken workflow file(s)..."
# Find workflow files that use the empty EMAIL_TO secret
BROKEN_FILES=$(grep -l 'secrets.EMAIL_TO' .github/workflows/*.yml 2>/dev/null || true)

if [ -z "$BROKEN_FILES" ]; then
  echo "No files reference secrets.EMAIL_TO. Either already fixed, or the workflow uses a different pattern."
  echo "List of workflow files:"
  ls .github/workflows/
  exit 1
fi

echo "Files to patch:"
echo "$BROKEN_FILES"

echo ""
echo ">> Step 3: Creating branch $BRANCH"
git checkout -b "$BRANCH"

echo ""
echo ">> Step 4: Applying fix to each file..."
for FILE in $BROKEN_FILES; do
  echo "  Patching $FILE"

  # Insert the email-extraction step just before the dawidd6 email send step,
  # then swap the `to:` line to read from AppConstants.java.
  python3 << PYEOF
import re, sys

path = "$FILE"
with open(path, "r") as f:
    content = f.read()

# Replacement for the `to:` line
content = re.sub(
    r"to:\s*\$\{\{\s*secrets\.EMAIL_TO\s*\}\}",
    "to: \${{ steps.email_config.outputs.EMAIL_TO }}",
    content,
)

# Insert the Extract Email Recipients step BEFORE the dawidd6 send step,
# only if not already present
if "id: email_config" not in content:
    extract_step = """      - name: Extract Email Recipients from AppConstants.java
        id: email_config
        run: |
          APPCONST="qa-automation/src/main/java/com/egalvanic/constants/AppConstants.java"
          if [ ! -f "\$APPCONST" ]; then
            APPCONST="src/main/java/com/egalvanic/constants/AppConstants.java"
          fi
          EMAIL_TO=\$(grep 'public static final String EMAIL_TO' "\$APPCONST" | grep -v '^\\s*//' | sed 's/.*"\\(.*\\)".*/\\1/' | sed 's/,[[:space:]]*\$//')
          echo "EMAIL_TO=\$EMAIL_TO" >> \$GITHUB_OUTPUT
          echo "Email recipients: \$EMAIL_TO"

"""
    # Insert before the first `uses: dawidd6/action-send-mail`
    content = re.sub(
        r"(      - name: [^\n]*\n\s*(?:id:[^\n]*\n\s*)?(?:if:[^\n]*\n\s*)?uses:\s*dawidd6/action-send-mail@v3)",
        extract_step + r"\1",
        content,
        count=1,
    )

with open(path, "w") as f:
    f.write(content)

print(f"    patched: {path}")
PYEOF
done

echo ""
echo ">> Step 5: Review the diff"
git diff --stat
echo ""
read -p "Press Enter to continue and commit, or Ctrl+C to abort..."

echo ""
echo ">> Step 6: Committing"
git add .github/workflows/
git commit -m "fix: Read EMAIL_TO from AppConstants.java instead of empty secret

The workflow used \${{ secrets.EMAIL_TO }} which is not set,
causing dawidd6/action-send-mail to fail with:
  'At least one of to, cc, or bcc must be specified'

This change reads EMAIL_TO from qa-automation/src/main/java/com/egalvanic/constants/AppConstants.java
(single source of truth — no GitHub secret to maintain)."

echo ""
echo ">> Step 7: Pushing branch to dev repo"
git push -u origin "$BRANCH"

echo ""
echo ">> Step 8: Opening PR"
gh pr create \
  --repo "$REPO" \
  --base "$BASE_BRANCH" \
  --head "$BRANCH" \
  --title "fix: Email — read EMAIL_TO from AppConstants.java" \
  --body "## What this fixes

The CI email step fails with \`At least one of 'to', 'cc' or 'bcc' must be specified\` because \`secrets.EMAIL_TO\` is not configured on this repo.

## The change

Instead of relying on a GitHub secret, the workflow now reads \`EMAIL_TO\` directly from \`qa-automation/src/main/java/com/egalvanic/constants/AppConstants.java\` (line 210).

Single source of truth — to change recipients, edit that line in the QA repo and the next CI run picks it up. No secrets to maintain.

## Scope

- ONLY changes the email step in \`.github/workflows/\`
- Does NOT touch app code, tests, or any production logic
- Targets \`release/qa\` branch

## Test plan

- [ ] Merge this PR
- [ ] Trigger a smoke CI run on \`release/qa\`
- [ ] Verify email arrives at recipients listed in AppConstants.java line 210"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  DONE. Review the PR in your browser — do NOT auto-merge."
echo "═══════════════════════════════════════════════════════════"
