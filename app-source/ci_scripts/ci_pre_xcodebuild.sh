#!/bin/sh
set -eu

log() { echo "[ci-pre] $1"; }

# -------------------------------
# Validate App Store Connect API env vars
# -------------------------------
MISSING=""
[ -z "${ASC_KEY_ID:-}" ]    && MISSING="$MISSING ASC_KEY_ID"
[ -z "${ASC_ISSUER_ID:-}" ] && MISSING="$MISSING ASC_ISSUER_ID"
[ -z "${ASC_KEY_P8:-}" ]    && MISSING="$MISSING ASC_KEY_P8"
[ -z "${ASC_APP_ID:-}" ]    && MISSING="$MISSING ASC_APP_ID"

if [ -n "$MISSING" ]; then
    log "Missing env vars:$MISSING — skipping build number sync, falling back to Xcode Cloud default"
    exit 0
fi

# Trim whitespace on short fields only (NOT ASC_KEY_P8 — would corrupt PEM newlines)
ASC_KEY_ID=$(printf '%s' "$ASC_KEY_ID" | tr -d '[:space:]')
ASC_ISSUER_ID=$(printf '%s' "$ASC_ISSUER_ID" | tr -d '[:space:]')
ASC_APP_ID=$(printf '%s' "$ASC_APP_ID" | tr -d '[:space:]')

# -------------------------------
# Materialize .p8 — auto-detect raw PEM vs base64-encoded
# -------------------------------
P8_FILE=$(mktemp /tmp/asc_key.XXXXXX.p8)
trap 'rm -f "$P8_FILE"' EXIT INT TERM

if printf '%s' "$ASC_KEY_P8" | grep -q "BEGIN PRIVATE KEY"; then
    log "ASC_KEY_P8 detected as raw PEM — writing as-is"
    printf '%s' "$ASC_KEY_P8" > "$P8_FILE"
else
    log "ASC_KEY_P8 detected as base64 — decoding"
    if ! printf '%s' "$ASC_KEY_P8" | tr -d '[:space:]' | base64 -D > "$P8_FILE" 2>/dev/null; then
        log "Failed to base64-decode ASC_KEY_P8 — check the value in Xcode Cloud env vars"
        log "  → Paste either the raw .p8 file contents OR  base64 -i AuthKey_XXX.p8  output"
        exit 1
    fi
fi

if ! grep -q "BEGIN PRIVATE KEY" "$P8_FILE"; then
    BYTES=$(wc -c < "$P8_FILE" | tr -d ' ')
    HEAD_HEX=$(head -c 8 "$P8_FILE" | xxd -p)
    log "Resulting key file isn't a valid .p8 — aborting"
    log "  Decoded $BYTES bytes, first 8 in hex: $HEAD_HEX  (no key content shown)"
    exit 1
fi

# -------------------------------
# Locate project + read MARKETING_VERSION
# -------------------------------
REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(pwd)}"
PBXPROJ=$(find "$REPO_ROOT" -maxdepth 3 -name "project.pbxproj" 2>/dev/null | head -1)

if [ -z "${PBXPROJ:-}" ] || [ ! -f "$PBXPROJ" ]; then
    log "Could not find project.pbxproj — aborting"
    exit 1
fi

MARKETING_VERSION=$(grep -m1 "MARKETING_VERSION = " "$PBXPROJ" \
    | sed -E 's/.*MARKETING_VERSION = ([^;]+);.*/\1/' \
    | tr -d ' ')

if [ -z "${MARKETING_VERSION:-}" ]; then
    log "Could not read MARKETING_VERSION from $PBXPROJ — aborting"
    exit 1
fi

log "App ID: $ASC_APP_ID"
log "Marketing version: $MARKETING_VERSION"

# -------------------------------
# Generate ES256 JWT (Ruby — uses stdlib only, available on macOS)
# -------------------------------
JWT=$(ASC_KEY_ID="$ASC_KEY_ID" ASC_ISSUER_ID="$ASC_ISSUER_ID" P8_FILE="$P8_FILE" ruby -e '
require "openssl"
require "json"
require "base64"

key = OpenSSL::PKey::EC.new(File.read(ENV["P8_FILE"]))
header  = { alg: "ES256", kid: ENV["ASC_KEY_ID"], typ: "JWT" }
payload = { iss: ENV["ASC_ISSUER_ID"], exp: Time.now.to_i + 1200, aud: "appstoreconnect-v1" }

def b64(d); Base64.urlsafe_encode64(d, padding: false); end

signing_input = "#{b64(header.to_json)}.#{b64(payload.to_json)}"
der = key.sign(OpenSSL::Digest::SHA256.new, signing_input)

asn1 = OpenSSL::ASN1.decode(der)
r = asn1.value[0].value.to_s(2).rjust(32, "\x00".b)
s = asn1.value[1].value.to_s(2).rjust(32, "\x00".b)

print "#{signing_input}.#{b64(r + s)}"
' 2>/dev/null)

if [ -z "${JWT:-}" ]; then
    log "Failed to generate JWT — check ASC_KEY_ID, ASC_ISSUER_ID, and that ASC_KEY_P8 contains a valid .p8 key"
    exit 1
fi

# -------------------------------
# Query App Store Connect for builds matching this marketing version
# -------------------------------
# Sort by upload date desc + limit 1 → just the latest uploaded build
API_URL="https://api.appstoreconnect.apple.com/v1/builds?filter%5Bapp%5D=${ASC_APP_ID}&filter%5BpreReleaseVersion.version%5D=${MARKETING_VERSION}&sort=-uploadedDate&limit=1&fields%5Bbuilds%5D=version"

HTTP_CODE=$(curl -sS -o /tmp/asc_response.json -w '%{http_code}' \
    -H "Authorization: Bearer $JWT" \
    "$API_URL" || echo "000")

if [ "$HTTP_CODE" != "200" ]; then
    BODY=$(head -c 500 /tmp/asc_response.json 2>/dev/null || true)
    log "App Store Connect API error: HTTP $HTTP_CODE — $BODY"
    rm -f /tmp/asc_response.json
    exit 1
fi

# Pull build number from the single returned record (or 0 if no prior builds for this version)
LATEST_BUILD=$(python3 -c '
import json
try:
    with open("/tmp/asc_response.json") as f:
        data = json.load(f)
    builds = data.get("data", [])
    if builds:
        v = builds[0].get("attributes", {}).get("version", "")
        print(int(v) if str(v).isdigit() else 0)
    else:
        print(0)
except Exception:
    print(0)
')

rm -f /tmp/asc_response.json

NEW_BUILD=$((LATEST_BUILD + 1))
log "Latest build for v${MARKETING_VERSION}: ${LATEST_BUILD} → new build: ${NEW_BUILD}"

# -------------------------------
# Update CURRENT_PROJECT_VERSION in pbxproj (all configs)
# -------------------------------
# macOS sed requires '' after -i for in-place edit
sed -i '' -E "s/CURRENT_PROJECT_VERSION = [^;]+;/CURRENT_PROJECT_VERSION = ${NEW_BUILD};/g" "$PBXPROJ"

# Verify the update took effect
UPDATED=$(grep -m1 "CURRENT_PROJECT_VERSION = " "$PBXPROJ" \
    | sed -E 's/.*CURRENT_PROJECT_VERSION = ([^;]+);.*/\1/' \
    | tr -d ' ')

if [ "$UPDATED" != "$NEW_BUILD" ]; then
    log "Failed to update CURRENT_PROJECT_VERSION (got '$UPDATED', expected '$NEW_BUILD')"
    exit 1
fi

log "Build number set: CURRENT_PROJECT_VERSION = $NEW_BUILD (for v$MARKETING_VERSION)"
