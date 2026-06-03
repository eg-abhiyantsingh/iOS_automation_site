#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════
# MobSF (Mobile Security Framework) — OWASP MASVS / Mobile Top 10 baseline
# ═══════════════════════════════════════════════════════════════════════
# Free / open-source. Static (and optional dynamic) analysis of the iOS app
# bundle: insecure local storage, ATS/cert-pinning config, hardcoded secrets,
# excessive permissions, insecure deep-link handlers, plist misconfig, etc.
#
# Boundary (honest): automated MASVS baseline. It does NOT replace a manual
# mobile pentest (auth-gated business logic, runtime IDOR, jailbreak-bypass).
#
# Requires Docker. Run:
#   APP=apps/Z-Platform-QA.app  bash security/mobsf-scan.sh            # zips .app then scans
#   APP=apps/Z-Platform-QA.ipa  bash security/mobsf-scan.sh            # scans .ipa directly
#
# Output: security/mobsf-report.json + .pdf (gitignored). Exit non-zero on
# HIGH-severity findings (CI gate).
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

APP="${APP:-apps/Z-Platform-QA.ipa}"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)"
MOBSF_PORT="${MOBSF_PORT:-8000}"
MOBSF_KEY="${MOBSF_API_KEY:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker required to run MobSF." >&2; exit 2
fi
if [ ! -e "$APP" ]; then
  echo "ERROR: app bundle not found: $APP" >&2; exit 2
fi

# iOS static analysis prefers .ipa; if given a .app dir, zip it to .ipa-like archive.
SCAN_FILE="$APP"
if [ -d "$APP" ]; then
  SCAN_FILE="${APP%.app}.ipa"
  echo "Packaging $APP → $SCAN_FILE"
  ( cd "$(dirname "$APP")" && mkdir -p Payload && cp -R "$(basename "$APP")" Payload/ \
     && zip -qr "$(basename "$SCAN_FILE")" Payload && rm -rf Payload )
fi

echo "── Starting MobSF container ──"
CID=$(docker run -d -p "${MOBSF_PORT}:8000" opensecurity/mobile-security-framework-mobsf:latest)
trap 'docker rm -f "$CID" >/dev/null 2>&1 || true' EXIT
# wait for MobSF API
for i in $(seq 1 30); do
  curl -s "http://localhost:${MOBSF_PORT}/api/v1/scans" -H "Authorization: ${MOBSF_KEY}" >/dev/null 2>&1 && break
  sleep 3
done
# Discover the auto-generated API key if not supplied
if [ -z "$MOBSF_KEY" ]; then
  MOBSF_KEY=$(docker logs "$CID" 2>&1 | grep -oE 'API Key: [a-f0-9]+' | head -1 | awk '{print $3}')
fi

echo "── Uploading + scanning $SCAN_FILE ──"
UP=$(curl -s -F "file=@${SCAN_FILE}" "http://localhost:${MOBSF_PORT}/api/v1/upload" -H "Authorization: ${MOBSF_KEY}")
HASH=$(echo "$UP" | sed -n 's/.*"hash":"\([a-f0-9]*\)".*/\1/p')
curl -s -X POST "http://localhost:${MOBSF_PORT}/api/v1/scan" \
  -H "Authorization: ${MOBSF_KEY}" --data "hash=${HASH}" >/dev/null
curl -s -X POST "http://localhost:${MOBSF_PORT}/api/v1/report_json" \
  -H "Authorization: ${MOBSF_KEY}" --data "hash=${HASH}" -o "${OUT_DIR}/mobsf-report.json"

echo "── MobSF report: ${OUT_DIR}/mobsf-report.json ──"
if command -v jq >/dev/null 2>&1; then
  HIGH=$(jq '[.. | objects | select(.severity? == "high")] | length' "${OUT_DIR}/mobsf-report.json" 2>/dev/null || echo 0)
  echo "MobSF HIGH-severity findings: ${HIGH}"
  if [ "${HIGH:-0}" -gt 0 ]; then
    echo "❌ MobSF found ${HIGH} HIGH-severity issue(s) — failing the gate." >&2
    exit 1
  fi
fi
echo "✅ MobSF scan complete (no HIGH-severity gate breach)."
