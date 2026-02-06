#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════
# SMOKE TEST DASHBOARD ENGINE
# ═══════════════════════════════════════════════════════════════════════
# Professional CI dashboard for module-by-module smoke test execution.
# Shows live-updating status board + progress bar in GitHub Actions.
#
# Each module runs individually → dashboard redraws after each → gives
# a visual "task tracker" experience in CI logs.
#
# Required env vars:
#   DEVICE_NAME, PLATFORM_VERSION, SIMULATOR_UDID, APP_PATH
# ═══════════════════════════════════════════════════════════════════════

set +e  # Don't exit on error — we handle failures ourselves

# ─────────────────────────────────────────────────────
# MODULE DEFINITIONS
# ─────────────────────────────────────────────────────
MODULES=("auth" "site" "asset" "location" "connections")
MODULE_NAMES=("Authentication" "Site Selection" "Asset Management" "Location" "Connections")
MODULE_TESTS=(4 3 3 3 3)
MODULE_XMLS=(
  "src/test/resources/smoke/testng-smoke-auth.xml"
  "src/test/resources/smoke/testng-smoke-site.xml"
  "src/test/resources/smoke/testng-smoke-asset.xml"
  "src/test/resources/smoke/testng-smoke-location.xml"
  "src/test/resources/smoke/testng-smoke-connections.xml"
)
TOTAL_TESTS=16
TOTAL_MODULES=5

# ─────────────────────────────────────────────────────
# STATE TRACKING
# ─────────────────────────────────────────────────────
STATUS=("pending" "pending" "pending" "pending" "pending")
M_PASSED=(0 0 0 0 0)
M_FAILED=(0 0 0 0 0)
M_SKIPPED=(0 0 0 0 0)
M_DURATION=(0 0 0 0 0)

SUITE_START=$(date +%s)
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0
HAS_FAILURE=0

# ─────────────────────────────────────────────────────
# PROGRESS BAR BUILDER
# Builds a Unicode bar: ████████░░░░░░░░ for given %
# Args: $1 = percentage (0-100), $2 = width (chars)
# ─────────────────────────────────────────────────────
build_bar() {
  local pct=$1
  local width=${2:-50}
  local filled=$((pct * width / 100))
  local empty=$((width - filled))
  local bar=""
  local i

  for ((i=0; i<filled; i++)); do bar+="█"; done
  for ((i=0; i<empty; i++)); do bar+="░"; done
  echo "$bar"
}

# ─────────────────────────────────────────────────────
# FORMAT DURATION
# Converts seconds to human-readable
# ─────────────────────────────────────────────────────
fmt_duration() {
  local secs=$1
  if [ "$secs" -ge 60 ]; then
    printf "%dm %ds" $((secs / 60)) $((secs % 60))
  else
    printf "%ds" "$secs"
  fi
}

# ─────────────────────────────────────────────────────
# DRAW DASHBOARD
# Prints the full dashboard to stdout. Called after
# each module status change for a "live" effect.
# ─────────────────────────────────────────────────────
draw_dashboard() {
  local completed=$((TOTAL_PASSED + TOTAL_FAILED + TOTAL_SKIPPED))
  local pct=0
  [ $TOTAL_TESTS -gt 0 ] && pct=$((completed * 100 / TOTAL_TESTS))
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)

  local bar
  bar=$(build_bar $pct 50)

  local LINE="══════════════════════════════════════════════════════════════════════════════"

  echo ""
  echo ""
  echo "  ╔${LINE}"
  echo "  ║"
  echo "  ║   🔥  S M O K E   T E S T   D A S H B O A R D"
  echo "  ║"
  echo "  ║   📱  ${DEVICE_NAME} · iOS ${PLATFORM_VERSION}          ${TOTAL_TESTS} tests · ${TOTAL_MODULES} modules"
  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"

  for i in 0 1 2 3 4; do
    local idx=$((i + 1))
    local name="${MODULE_NAMES[$i]}"
    local st="${STATUS[$i]}"
    local tc="${MODULE_TESTS[$i]}"
    local dur_fmt
    dur_fmt=$(fmt_duration "${M_DURATION[$i]}")

    case "$st" in
      passed)
        printf "  ║   ✅  Module %d │ %-20s    %d/%d passed                %s\n" \
          "$idx" "$name" "${M_PASSED[$i]}" "$tc" "$dur_fmt"
        ;;
      failed)
        printf "  ║   ❌  Module %d │ %-20s    %d passed, %d failed       %s\n" \
          "$idx" "$name" "${M_PASSED[$i]}" "${M_FAILED[$i]}" "$dur_fmt"
        ;;
      running)
        printf "  ║   🔄  Module %d │ %-20s    Running...\n" \
          "$idx" "$name"
        ;;
      pending)
        printf "  ║   ⏳  Module %d │ %-20s    Pending\n" \
          "$idx" "$name"
        ;;
    esac
  done

  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"
  printf "  ║   %s   %3d%%    %d/%d tests\n" "$bar" "$pct" "$completed" "$TOTAL_TESTS"
  echo "  ║   ╰────────────┼────────────┼────────────┼────────────╯"
  echo "  ║   0%          25%          50%          75%         100%"
  echo "  ║"
  printf "  ║   ✅ %d passed   ❌ %d failed   ⏭️  %d skipped    ⏱️  %s elapsed\n" \
    "$TOTAL_PASSED" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
  echo "  ║"
  echo "  ╚${LINE}"
  echo ""
}

# ─────────────────────────────────────────────────────
# DRAW FINAL BANNER
# Shows the completion banner after all modules done
# ─────────────────────────────────────────────────────
draw_final_banner() {
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)

  local LINE="══════════════════════════════════════════════════════════════════════════════"
  local DASHES="──────────────────────────────────────────────────────────────────────────────"

  echo ""
  if [ $HAS_FAILURE -eq 0 ]; then
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ║     ✅  A L L   S M O K E   T E S T S   P A S S E D  !"
    echo "  ║"
    echo "  ║     ${TOTAL_PASSED}/${TOTAL_TESTS} tests passed in ${elapsed_fmt}"
    echo "  ║     All 5 critical modules verified successfully"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ╚${LINE}"
  else
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║   ⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
    echo "  ║"
    echo "  ║     ❌  S O M E   T E S T S   F A I L E D"
    echo "  ║"
    printf "  ║     %d/%d passed, %d failed, %d skipped in %s\n" \
      "$TOTAL_PASSED" "$TOTAL_TESTS" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
    echo "  ║"
    echo "  ║     Failed modules:"
    for i in 0 1 2 3 4; do
      if [ "${STATUS[$i]}" = "failed" ]; then
        echo "  ║       ❌ Module $((i+1)): ${MODULE_NAMES[$i]} (${M_FAILED[$i]} failed)"
      fi
    done
    echo "  ║"
    echo "  ║   ⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
    echo "  ║"
    echo "  ╚${LINE}"
  fi
  echo ""
}

# ─────────────────────────────────────────────────────
# PARSE TESTNG RESULTS
# Reads target/surefire-reports/testng-results.xml
# Returns: "passed failed skipped"
# ─────────────────────────────────────────────────────
parse_results() {
  local xml="target/surefire-reports/testng-results.xml"
  if [ -f "$xml" ]; then
    local p f s
    p=$(sed -n 's/.*passed="\([^"]*\)".*/\1/p' "$xml" | head -1)
    f=$(sed -n 's/.*failed="\([^"]*\)".*/\1/p' "$xml" | head -1)
    s=$(sed -n 's/.*skipped="\([^"]*\)".*/\1/p' "$xml" | head -1)
    echo "${p:-0} ${f:-0} ${s:-0}"
  else
    echo "0 0 0"
  fi
}

# ═══════════════════════════════════════════════════════
# MAIN EXECUTION
# ═══════════════════════════════════════════════════════

echo ""
echo "  ┌──────────────────────────────────────────────────────────────────────────"
echo "  │  🚀  Starting Smoke Test Suite"
echo "  │  📱  ${DEVICE_NAME} · iOS ${PLATFORM_VERSION}"
echo "  │  📦  ${TOTAL_TESTS} tests across ${TOTAL_MODULES} modules"
echo "  │  ⏰  $(date '+%Y-%m-%d %H:%M:%S')"
echo "  └──────────────────────────────────────────────────────────────────────────"

# Show initial dashboard (all pending)
draw_dashboard

# ── Run each module ─────────────────────────────────
for i in 0 1 2 3 4; do
  MODULE_IDX=$((i + 1))
  MODULE_KEY="${MODULES[$i]}"
  MODULE_NAME="${MODULE_NAMES[$i]}"
  MODULE_XML="${MODULE_XMLS[$i]}"
  TEST_COUNT="${MODULE_TESTS[$i]}"

  # Mark as running and redraw
  STATUS[$i]="running"
  draw_dashboard

  # Clean previous reports to get fresh results
  rm -rf target/surefire-reports 2>/dev/null || true

  MODULE_START=$(date +%s)

  # ── Run Maven inside a collapsed group ──
  echo "::group::📋 Module ${MODULE_IDX}: ${MODULE_NAME} — ${TEST_COUNT} tests (click to expand)"
  echo ""
  echo "  Running: mvn test -DsuiteXmlFile=${MODULE_XML}"
  echo "  Tests:   ${TEST_COUNT}"
  echo "  Time:    $(date '+%H:%M:%S')"
  echo ""

  mvn test -B -q \
    -DsuiteXmlFile="${MODULE_XML}" \
    -DDEVICE_NAME="${DEVICE_NAME}" \
    -DPLATFORM_VERSION="${PLATFORM_VERSION}" \
    -DSIMULATOR_UDID="${SIMULATOR_UDID}" \
    -DAPP_PATH="${APP_PATH}" \
    2>&1
  MVN_EXIT=$?

  echo ""
  echo "  Exit code: ${MVN_EXIT}"
  echo "::endgroup::"

  MODULE_END=$(date +%s)
  M_DURATION[$i]=$((MODULE_END - MODULE_START))

  # ── Parse results ──
  RESULTS=$(parse_results)
  read -r P F S <<< "$RESULTS"

  M_PASSED[$i]=$P
  M_FAILED[$i]=$F
  M_SKIPPED[$i]=$S

  TOTAL_PASSED=$((TOTAL_PASSED + P))
  TOTAL_FAILED=$((TOTAL_FAILED + F))
  TOTAL_SKIPPED=$((TOTAL_SKIPPED + S))

  # Determine module status
  if [ "$F" -gt 0 ] || [ $MVN_EXIT -ne 0 ]; then
    STATUS[$i]="failed"
    HAS_FAILURE=1

    # If Maven failed but no results, count expected tests as failed
    if [ "$P" -eq 0 ] && [ "$F" -eq 0 ] && [ "$S" -eq 0 ]; then
      M_FAILED[$i]=$TEST_COUNT
      TOTAL_FAILED=$((TOTAL_FAILED + TEST_COUNT))
    fi
  else
    STATUS[$i]="passed"
  fi

  # ── Save this module's reports before next run overwrites them ──
  mkdir -p "reports/modules/module-${MODULE_IDX}-${MODULE_KEY}"
  cp -r target/surefire-reports/* "reports/modules/module-${MODULE_IDX}-${MODULE_KEY}/" 2>/dev/null || true

  # Redraw dashboard with updated status
  draw_dashboard
done

# ── Final Results ──────────────────────────────────
draw_final_banner

# ── Write summary for downstream steps ──
echo "SMOKE_PASSED=${TOTAL_PASSED}" >> "$GITHUB_ENV"
echo "SMOKE_FAILED=${TOTAL_FAILED}" >> "$GITHUB_ENV"
echo "SMOKE_SKIPPED=${TOTAL_SKIPPED}" >> "$GITHUB_ENV"
echo "SMOKE_TOTAL=${TOTAL_TESTS}" >> "$GITHUB_ENV"
echo "SMOKE_DURATION=$(( $(date +%s) - SUITE_START ))" >> "$GITHUB_ENV"

if [ $HAS_FAILURE -eq 1 ]; then
  echo "SMOKE_RESULT=failed" >> "$GITHUB_ENV"
  exit 1
else
  echo "SMOKE_RESULT=passed" >> "$GITHUB_ENV"
  exit 0
fi
