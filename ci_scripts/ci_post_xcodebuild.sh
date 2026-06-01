#!/bin/sh
set -euo pipefail

log() { echo "[ci-script] $1"; }

# -------------------------------
# ENV label
# -------------------------------
ENV_LABEL=$(printf '%s' "${CI_WORKFLOW:-}" | grep -oE '(Dev|QA|Stag|Prod)' | head -1 || true)
[ -z "${ENV_LABEL:-}" ] && ENV_LABEL="Unknown"

# -------------------------------
# Product formatting
# -------------------------------
PRODUCT_FORMATTED=$(printf '%s' "${CI_PRODUCT:-}" | sed -E 's/-([A-Za-z]+)$/ (\1)/')

# -------------------------------
# Version resolution (deterministic)
# -------------------------------
VERSION=""

if [ -n "${CI_ARCHIVE_PATH:-}" ] && [ -d "${CI_ARCHIVE_PATH}" ]; then
    APP_PATH=$(find "${CI_ARCHIVE_PATH}/Products/Applications" -maxdepth 1 -name "*.app" -type d 2>/dev/null | head -1)

    if [ -n "${APP_PATH:-}" ]; then
        if [ -f "$APP_PATH/Info.plist" ]; then
            APP_PLIST="$APP_PATH/Info.plist"
        elif [ -f "$APP_PATH/Contents/Info.plist" ]; then
            APP_PLIST="$APP_PATH/Contents/Info.plist"
        else
            APP_PLIST=""
        fi

        if [ -n "${APP_PLIST:-}" ]; then
            VERSION=$(plutil -extract CFBundleShortVersionString raw -o - "$APP_PLIST" 2>/dev/null || true)
        fi
    fi
fi

# fallback
if [ -z "${VERSION:-}" ]; then
    REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(pwd)}"
    PBXPROJ=$(find "$REPO_ROOT" -maxdepth 3 -name "project.pbxproj" 2>/dev/null | head -1)

    if [ -n "${PBXPROJ:-}" ]; then
        VERSION=$(grep -m1 "MARKETING_VERSION = " "$PBXPROJ" 2>/dev/null \
            | sed -E 's/.*MARKETING_VERSION = ([^;]+);.*/\1/' \
            | tr -d ' ')
    fi
fi

[ -z "${VERSION:-}" ] && VERSION="unknown"
log "Resolved version: $VERSION"

BUILD_NUM="${CI_BUILD_NUMBER:-unknown}"

# -------------------------------
# Build status
# -------------------------------
if [ -n "${CI_XCODEBUILD_EXITCODE:-}" ] && [ "$CI_XCODEBUILD_EXITCODE" != "0" ]; then
    STATUS_EMOJI="❌"
    STATUS_TEXT="Build Failed"
    STATUS_DETAIL="The latest ${ENV_LABEL} build failed."
    LEAD_EMOJI="🚨"
    BUILD_OK=0
else
    STATUS_EMOJI="✅"
    STATUS_TEXT="Build Successful"
    STATUS_DETAIL="The latest ${ENV_LABEL} build has been completed successfully."
    LEAD_EMOJI="🚀"
    BUILD_OK=1
fi

# -------------------------------
# Extract failure reason from xcresult bundle (failure path only)
# Shows first error + total count if multiple errors exist
# -------------------------------
FAILURE_REASON=""
if [ "$BUILD_OK" = "0" ] && [ -n "${CI_RESULT_BUNDLE_PATH:-}" ] && [ -d "$CI_RESULT_BUNDLE_PATH" ]; then
    FAILURE_REASON=$(xcrun xcresulttool get build-results \
        --path "$CI_RESULT_BUNDLE_PATH" --format json 2>/dev/null \
        | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    errs = d.get("errors", []) or d.get("issues", {}).get("errorSummaries", [])
    if errs:
        count = len(errs)
        first = (errs[0].get("message") or errs[0].get("title") or "").strip()
        first = " ".join(first.split())[:180]
        if count > 1:
            print(f"{first}  ({count} errors total — see build logs)")
        else:
            print(first)
except Exception:
    pass
' 2>/dev/null)
fi

BAR="▎ "

# -------------------------------
# Jira config
# -------------------------------
JIRA_BROWSE_URL="${JIRA_BROWSE_URL:-https://egalvanic.atlassian.net/browse/}"
JIRA_API_BASE="${JIRA_API_BASE:-https://egalvanic.atlassian.net/rest/api/3}"
JIRA_DEV_TRANSITION_ID="${JIRA_DEV_TRANSITION_ID:-8}"
JIRA_QA_TRANSITION_ID="${JIRA_QA_TRANSITION_ID:-5}"

# Defensive trim — env vars pasted into Xcode Cloud UI may carry trailing whitespace/newlines
JIRA_EMAIL=$(printf '%s' "${JIRA_EMAIL:-}" | tr -d '[:space:]')
JIRA_TOKEN=$(printf '%s' "${JIRA_TOKEN:-}" | tr -d '[:space:]')
JIRA_API_BASE=$(printf '%s' "${JIRA_API_BASE}" | tr -d '[:space:]')
JIRA_API_BASE="${JIRA_API_BASE%/}"

# -------------------------------
# Git parsing
# -------------------------------
REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(pwd)}"
TICKET_NUMBERS=""
TICKET_DETAILS=""
COMMIT_SUBJECTS=""
AUTHOR_DISPLAY=""

if [ -d "$REPO_ROOT/.git" ]; then
    cd "$REPO_ROOT" || exit 0

    AUTHOR_NAME=$(git log -1 --pretty=%an 2>/dev/null || true)
    AUTHOR_EMAIL=$(git log -1 --pretty=%ae 2>/dev/null || true)

    AUTHOR_LOCAL=$(printf '%s' "$AUTHOR_EMAIL" | cut -d'@' -f1)

    if printf '%s' "$AUTHOR_LOCAL" | grep -q '\.'; then
        AUTHOR_DISPLAY=$(printf '%s' "$AUTHOR_LOCAL" \
            | tr '._-' '   ' \
            | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2))}1')
    else
        AUTHOR_DISPLAY="$AUTHOR_NAME"
    fi

    git fetch --deepen=50 2>/dev/null || true

    RANGE="HEAD~1..HEAD"
    git log "$RANGE" --pretty=%s 2>/dev/null > /tmp/ci_commits.txt || git log -1 --pretty=%s > /tmp/ci_commits.txt

    sed -E 's/ \(#[0-9]+\)$//' /tmp/ci_commits.txt | awk '!seen[$0]++' > /tmp/ci_commits_dedup.txt

    if [ -s /tmp/ci_commits_dedup.txt ]; then
        COMMIT_SUBJECTS=$(cat /tmp/ci_commits_dedup.txt)
        TICKET_DETAILS=$(grep -E '^ZP-[0-9]+' /tmp/ci_commits_dedup.txt || true)
        TICKET_NUMBERS=$(grep -oE '^ZP-[0-9]+' /tmp/ci_commits_dedup.txt | awk '!seen[$0]++' || true)
    fi

    rm -f /tmp/ci_commits.txt /tmp/ci_commits_dedup.txt
fi

# -------------------------------
# TestFlight notes
# -------------------------------
mkdir -p "$REPO_ROOT/TestFlight"

if [ -n "$TICKET_DETAILS" ]; then
    TF_BODY="$TICKET_DETAILS"
else
    TF_BODY="$COMMIT_SUBJECTS"
fi

if [ -n "${TF_BODY:-}" ]; then
    {
        echo "What's new in this build:"
        echo ""
        printf '%s\n' "$TF_BODY" | sed 's/^/- /'
    } > "$REPO_ROOT/TestFlight/WhatToTest.en-US.txt"
else
    echo "Build ${BUILD_NUM}" > "$REPO_ROOT/TestFlight/WhatToTest.en-US.txt"
fi

# -------------------------------
# What to Test section (no subshell bug)
# -------------------------------
WHAT_TO_TEST_SECTION=""

if [ -n "$TICKET_NUMBERS" ]; then
    COUNT=$(printf '%s\n' "$TICKET_NUMBERS" | grep -c '^ZP-' || true)

    if [ "$COUNT" -gt 3 ]; then
        INLINE=$(printf '%s\n' "$TICKET_NUMBERS" \
            | awk -v base="$JIRA_BROWSE_URL" '{printf "%s<%s%s|%s>", (NR>1?", ":""), base, $0, $0}')
        WHAT_TO_TEST_SECTION="
${BAR}
${BAR}─────────────────────────
${BAR}*What to Test*
${BAR}─────────────────────────
${BAR}${INLINE}"
    else
        BULLETS=$(printf '%s\n' "$TICKET_NUMBERS" \
            | awk -v bar="$BAR" -v base="$JIRA_BROWSE_URL" '{printf "\n%s• <%s%s|%s>", bar, base, $0, $0}')
        WHAT_TO_TEST_SECTION="
${BAR}
${BAR}─────────────────────────
${BAR}*What to Test*
${BAR}─────────────────────────${BULLETS}"
    fi
fi

AUTHOR_ROW=""
[ -n "$AUTHOR_DISPLAY" ] && AUTHOR_ROW="
${BAR}*Author:*      ${AUTHOR_DISPLAY}"

# -------------------------------
# Jira transition
# -------------------------------
case "$ENV_LABEL" in
    Dev) TRANSITION_ID="$JIRA_DEV_TRANSITION_ID" ;;
    QA)  TRANSITION_ID="$JIRA_QA_TRANSITION_ID" ;;
    *)   TRANSITION_ID="" ;;
esac

TICKET_COUNT=$(printf '%s' "$TICKET_NUMBERS" | grep -c '^ZP-' || true)
log "Jira: env=${ENV_LABEL} transition_id=${TRANSITION_ID:-<none>} tickets=${TICKET_COUNT} email=${JIRA_EMAIL:+set} token=${JIRA_TOKEN:+set}"

if [ "$BUILD_OK" = "1" ] && [ -n "$TRANSITION_ID" ] && [ -n "$TICKET_NUMBERS" ]; then
    if [ -n "$JIRA_EMAIL" ] && [ -n "$JIRA_TOKEN" ]; then
        OK_COUNT=0
        FAIL_COUNT=0
        for KEY in $TICKET_NUMBERS; do
            HTTP=$(curl -sS -o /tmp/jira_resp.txt -w '%{http_code}' \
                -u "${JIRA_EMAIL}:${JIRA_TOKEN}" \
                -X POST \
                -H "Accept: application/json" \
                -H "Content-Type: application/json" \
                -d "{\"transition\":{\"id\":\"${TRANSITION_ID}\"}}" \
                "${JIRA_API_BASE}/issue/${KEY}/transitions" 2>/dev/null || echo "000")

            if [ "$HTTP" = "204" ]; then
                log "  ${KEY} -> transitioned (204)"
                OK_COUNT=$((OK_COUNT + 1))
            else
                BODY=$(head -c 300 /tmp/jira_resp.txt 2>/dev/null || true)
                log "  ${KEY} -> HTTP ${HTTP}: ${BODY}"
                FAIL_COUNT=$((FAIL_COUNT + 1))
            fi
        done
        log "Jira: done — ok=${OK_COUNT} fail=${FAIL_COUNT}"
        rm -f /tmp/jira_resp.txt
    else
        log "Jira: skipped — JIRA_EMAIL or JIRA_TOKEN not set (after trim)"
    fi
else
    log "Jira: skipped — build_ok=${BUILD_OK} transition_id=${TRANSITION_ID:-<empty>} tickets=${TICKET_COUNT}"
fi

# -------------------------------
# Proper JSON (no \n issue)
# -------------------------------
if [ -z "${CHAT_WEBHOOK_URL:-}" ]; then
    log "No webhook URL"
    exit 0
fi

REASON_BLOCK=""
if [ -n "$FAILURE_REASON" ]; then
    # Put "(N errors total)" suffix on its own bar-prefixed line so it aligns left
    if echo "$FAILURE_REASON" | grep -qE '\([0-9]+ errors total'; then
        REASON_MAIN=$(echo "$FAILURE_REASON" | sed -E 's/[[:space:]]+\([0-9]+ errors total[^)]*\)//')
        REASON_HINT=$(echo "$FAILURE_REASON" | grep -oE '\([0-9]+ errors total[^)]*\)')
        REASON_BLOCK="
${BAR}*Reason:* ${REASON_MAIN}
${BAR}${REASON_HINT}
${BAR}"
    else
        REASON_BLOCK="
${BAR}*Reason:* ${FAILURE_REASON}
${BAR}"
    fi
fi

MESSAGE="${BAR}━━━━━━━━━━━━━━━━━━━━━━━━━
${BAR}${LEAD_EMOJI} *${STATUS_TEXT} — ${ENV_LABEL}* ${STATUS_EMOJI}
${BAR}━━━━━━━━━━━━━━━━━━━━━━━━━
${BAR}
${BAR}${STATUS_DETAIL}
${BAR}${REASON_BLOCK}
${BAR}*Product:*     ${PRODUCT_FORMATTED}
${BAR}*Workflow:*    ${CI_WORKFLOW:-unknown}
${BAR}*Branch:*      ${CI_BRANCH:-unknown}${AUTHOR_ROW}
${BAR}*Version:*     ${VERSION} (${BUILD_NUM})${WHAT_TO_TEST_SECTION}"

ESCAPED=$(printf '%s' "$MESSAGE" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')

curl -sS -X POST "$CHAT_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{\"text\": ${ESCAPED}}"

log "Notification sent"
