#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════
# SMOKE TEST DASHBOARD v5 — S3 Drift + Login + Site + CRUD Operations
# ═══════════════════════════════════════════════════════════════════════
# Runs 6 modules individually with LIVE per-test progress updates.
# Module 0 (S3 Drift Detection) runs FIRST without Appium.
# Modules 1-5: Login → Site Selection → Asset CRUD → Location CRUD → Connection CRUD.
#
# Architecture:
#   1. Maven runs in background → output to temp log file
#   2. Foreground monitors log for test completions (1s polling)
#   3. Per-test: prints test name + global progress counter
#   4. Per-module: prints module summary
#   5. After all: prints final dashboard + banner
#   6. Raw Maven output → collapsed ::group:: blocks
#
# Required env vars:
#   DEVICE_NAME, PLATFORM_VERSION, SIMULATOR_UDID, APP_PATH
#   (AWS credentials for S3 drift: either AWS profile or env vars)
# ═══════════════════════════════════════════════════════════════════════

set +e  # Don't exit on error — we handle failures ourselves

# ─────────────────────────────────────────────────────
# MODULE DEFINITIONS (S3 drift is DYNAMIC based on branch)
# ─────────────────────────────────────────────────────
# S3 environment is detected by the workflow and passed via env vars:
#   S3_ENV   = dev|qa|staging|prod|production|bces-iq|all
#   S3_XML   = path to per-env XML file
#   S3_TESTS = number of S3 tests (10 per single env, 18 for production, 42 for all)
#   S3_LABEL = display label (e.g. "QA only", "PRODUCTION (prod + bces-iq)")
#
# Branch mapping:
#   dev/develop       → dev (10 tests)
#   qa                → qa (10 tests)
#   staging/stage     → staging (10 tests)
#   prod              → prod (10 tests)
#   production/main   → prod + bces-iq (18 tests)
#   bces-iq           → bces-iq (10 tests)
#   unknown           → all (42 tests)
# ─────────────────────────────────────────────────────

# Dynamic S3 config from workflow env vars (defaults to "all" for local runs)
S3_ENV_ACTUAL="${S3_ENV:-all}"
S3_XML_ACTUAL="${S3_XML:-src/test/resources/smoke/testng-smoke-s3drift.xml}"
S3_TESTS_ACTUAL="${S3_TESTS:-42}"
S3_LABEL_ACTUAL="${S3_LABEL:-ALL (5 environments)}"

# Build S3 module name dynamically
S3_MODULE_NAME="S3 Drift [${S3_LABEL_ACTUAL}]"

MODULES=("s3drift" "login" "site-selection" "asset-crud" "location-crud" "connection-crud")
MODULE_NAMES=("${S3_MODULE_NAME}" "Login" "Site Selection" "Asset CRUD" "Location CRUD" "Connection CRUD")
MODULE_TESTS=(${S3_TESTS_ACTUAL} 1 1 4 4 3)
MODULE_XMLS=(
  "${S3_XML_ACTUAL}"
  "src/test/resources/smoke/testng-smoke-login.xml"
  "src/test/resources/smoke/testng-smoke-site-selection.xml"
  "src/test/resources/smoke/testng-smoke-asset-crud.xml"
  "src/test/resources/smoke/testng-smoke-location-crud.xml"
  "src/test/resources/smoke/testng-smoke-connection-crud.xml"
)

# Total = S3 (dynamic) + 13 mobile tests (1 Login + 1 Site + 4 Asset + 4 Location + 3 Connection)
TOTAL_TESTS=$((S3_TESTS_ACTUAL + 13))
TOTAL_MODULES=6

# ─────────────────────────────────────────────────────
# STATE TRACKING
# ─────────────────────────────────────────────────────
STATUS=("pending" "pending" "pending" "pending" "pending" "pending")
M_PASSED=(0 0 0 0 0 0)
M_FAILED=(0 0 0 0 0 0)
M_SKIPPED=(0 0 0 0 0 0)
M_DURATION=(0 0 0 0 0 0)

SUITE_START=$(date +%s)
GLOBAL_COMPLETED=0
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0
HAS_FAILURE=0

# ─────────────────────────────────────────────────────
# FORMAT DURATION
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
# PRINT PER-TEST PROGRESS LINE
# ─────────────────────────────────────────────────────
print_test_progress() {
  local status_icon="$1"
  local test_name="$2"
  local duration="$3"
  local error_reason="$4"
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)

  if [ -n "$error_reason" ]; then
    printf "    %s  %s\n" "$status_icon" "$test_name"
    printf "         Reason for fail: %s\n" "$error_reason"
  else
    printf "    %s  %-55s %ss\n" "$status_icon" "$test_name" "$duration"
  fi
  printf "    %d/%d completed   ⏱️ %s\n\n" "$GLOBAL_COMPLETED" "$TOTAL_TESTS" "$elapsed_fmt"
}

# ─────────────────────────────────────────────────────
# PRINT MODULE HEADER
# ─────────────────────────────────────────────────────
print_module_header() {
  local idx=$1
  local name="${MODULE_NAMES[$idx]}"
  local tc="${MODULE_TESTS[$idx]}"
  local num=$idx

  # Module 0 is S3 Drift, then 1-5 are mobile tests
  if [ $idx -eq 0 ]; then
    echo ""
    echo "  ── Module 0/${TOTAL_MODULES}: ${name} (${tc} tests) [NO APPIUM] ──────────────────────"
    echo "  ── Infrastructure health check — runs before mobile tests"
    echo ""
  else
    echo ""
    echo "  ── Module ${num}/${TOTAL_MODULES}: ${name} (${tc} tests) ──────────────────────────────────────"
    echo ""
  fi
}

# ─────────────────────────────────────────────────────
# PRINT MODULE COMPLETION LINE
# ─────────────────────────────────────────────────────
print_module_complete() {
  local idx=$1
  local dur=$2
  local num=$idx
  local name="${MODULE_NAMES[$idx]}"
  local p=${M_PASSED[$idx]}
  local f=${M_FAILED[$idx]}
  local s=${M_SKIPPED[$idx]}
  local tc=${MODULE_TESTS[$idx]}
  local dur_fmt
  dur_fmt=$(fmt_duration $dur)

  if [ "$f" -gt 0 ]; then
    echo "  ── ❌ Module ${num}: ${name}  ${p} passed, ${f} failed  (${dur_fmt}) ─────────────────"
  elif [ "$s" -gt 0 ]; then
    echo "  ── ⚠️  Module ${num}: ${name}  ${p}/${tc} passed, ${s} skipped  (${dur_fmt}) ─────────────"
  else
    echo "  ── ✅ Module ${num}: ${name}  ${p}/${tc} passed  (${dur_fmt}) ──────────────────────"
  fi
  echo ""
}

# ─────────────────────────────────────────────────────
# DRAW FINAL DASHBOARD
# ─────────────────────────────────────────────────────
draw_final_dashboard() {
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)
  local completed=$((TOTAL_PASSED + TOTAL_FAILED + TOTAL_SKIPPED))
  local LINE="══════════════════════════════════════════════════════════════════════════════"

  echo ""
  echo ""
  echo "  ╔${LINE}"
  echo "  ║"
  echo "  ║   🔥  S M O K E   T E S T   D A S H B O A R D   —   F I N A L"
  echo "  ║"
  echo "  ║   📱  ${DEVICE_NAME} · iOS ${PLATFORM_VERSION}          ${TOTAL_TESTS} tests · ${TOTAL_MODULES} modules"
  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"

  for i in 0 1 2 3 4 5; do
    local num=$i
    local name="${MODULE_NAMES[$i]}"
    local st="${STATUS[$i]}"
    local tc="${MODULE_TESTS[$i]}"
    local dur_fmt
    dur_fmt=$(fmt_duration "${M_DURATION[$i]}")

    # Special label for Module 0
    local label=""
    [ $i -eq 0 ] && label=" [Infra]"

    case "$st" in
      passed)
        printf "  ║   ✅  Module %d │ %-20s    %d/%d passed%s              %s\n" \
          "$num" "$name" "${M_PASSED[$i]}" "$tc" "$label" "$dur_fmt"
        ;;
      failed)
        printf "  ║   ❌  Module %d │ %-20s    %d passed, %d failed%s     %s\n" \
          "$num" "$name" "${M_PASSED[$i]}" "${M_FAILED[$i]}" "$label" "$dur_fmt"
        ;;
      *)
        printf "  ║   ⚠️   Module %d │ %-20s    Did not complete%s\n" \
          "$num" "$name" "$label"
        ;;
    esac
  done

  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"
  printf "  ║   %d/%d tests completed\n" "$completed" "$TOTAL_TESTS"
  echo "  ║"
  printf "  ║   ✅ %d passed   ❌ %d failed   ⏭️  %d skipped    ⏱️  %s elapsed\n" \
    "$TOTAL_PASSED" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
  echo "  ║"
  echo "  ╚${LINE}"
  echo ""
}

# ─────────────────────────────────────────────────────
# DRAW FINAL BANNER
# ─────────────────────────────────────────────────────
draw_final_banner() {
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)
  local LINE="══════════════════════════════════════════════════════════════════════════════"

  echo ""
  if [ $HAS_FAILURE -eq 0 ]; then
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ║     ✅  A L L   S M O K E   T E S T S   P A S S E D  !"
    echo "  ║"
    echo "  ║     ${TOTAL_PASSED}/${TOTAL_TESTS} tests passed in ${elapsed_fmt}"
    echo "  ║     All ${TOTAL_MODULES} modules verified successfully"
    echo "  ║     (Infrastructure ✅ + Mobile ✅)"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ╚${LINE}"
  else
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║"
    echo "  ║     ❌  S O M E   T E S T S   F A I L E D"
    echo "  ║"
    printf "  ║     %d/%d passed, %d failed, %d skipped in %s\n" \
      "$TOTAL_PASSED" "$TOTAL_TESTS" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
    echo "  ║"
    echo "  ║     Failed modules:"
    for i in 0 1 2 3 4 5; do
      if [ "${STATUS[$i]}" = "failed" ]; then
        echo "  ║       ❌ Module ${i}: ${MODULE_NAMES[$i]} (${M_FAILED[$i]} failed)"
      fi
    done
    echo "  ║"
    echo "  ╚${LINE}"
  fi
  echo ""
}

# ─────────────────────────────────────────────────────
# PARSE RESULTS FROM SUREFIRE XML (fallback)
# ─────────────────────────────────────────────────────
parse_results_xml() {
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

# ── Print Header ──
echo ""
echo "  ┌──────────────────────────────────────────────────────────────────────────"
echo "  │  🚀  Smoke Test Dashboard v5"
echo "  │  📱  ${DEVICE_NAME} · iOS ${PLATFORM_VERSION}"
echo "  │  📦  ${TOTAL_TESTS} tests across ${TOTAL_MODULES} modules"
echo "  │  🪣  S3 Drift: ${S3_LABEL_ACTUAL} (${S3_TESTS_ACTUAL} tests)"
echo "  │  📲  Modules 1-5: Login → Site → Asset → Location → Connection CRUD"
echo "  │  ⏰  $(date '+%Y-%m-%d %H:%M:%S')"
echo "  └──────────────────────────────────────────────────────────────────────────"
echo ""


# ── Run each module ─────────────────────────────────
for i in 0 1 2 3 4 5; do
  MODULE_IDX=$i
  MODULE_KEY="${MODULES[$i]}"
  MODULE_NAME="${MODULE_NAMES[$i]}"
  MODULE_XML="${MODULE_XMLS[$i]}"
  TEST_COUNT="${MODULE_TESTS[$i]}"

  # Mark as running
  STATUS[$i]="running"

  # Print module header
  print_module_header $i

  # Clean previous reports to get fresh results
  rm -rf target/surefire-reports 2>/dev/null || true

  MODULE_START=$(date +%s)
  LOG_FILE="/tmp/smoke_module_${i}.log"
  > "$LOG_FILE"

  # ── Build Maven command ──
  # Module 0 (S3 Drift) does NOT need Appium env vars
  if [ $i -eq 0 ]; then
    mvn test -B -q \
      -DsuiteXmlFile="${MODULE_XML}" \
      > "$LOG_FILE" 2>&1 &
    MVN_PID=$!
  else
    mvn test -B -q \
      -DsuiteXmlFile="${MODULE_XML}" \
      -DDEVICE_NAME="${DEVICE_NAME}" \
      -DPLATFORM_VERSION="${PLATFORM_VERSION}" \
      -DSIMULATOR_UDID="${SIMULATOR_UDID}" \
      -DAPP_PATH="${APP_PATH}" \
      > "$LOG_FILE" 2>&1 &
    MVN_PID=$!
  fi

  # ── Monitor for per-test completions ──
  LAST_COUNT=0
  MOD_PASSED=0
  MOD_FAILED=0
  MOD_SKIPPED=0

  while kill -0 $MVN_PID 2>/dev/null; do
    # Count completed tests in log
    # S3 drift tests use [SMOKE] prefix, mobile tests use ConsoleProgressListener
    CURRENT=$(grep -cE '(PASSED|FAILED|SKIPPED)' "$LOG_FILE" 2>/dev/null | head -1)
    
    # More precise: match either ConsoleProgressListener format OR TestNG format
    CURRENT=$(grep -cE '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)|Tests run: [0-9]+' "$LOG_FILE" 2>/dev/null)
    CURRENT=${CURRENT:-0}

    if [ "$CURRENT" -gt "$LAST_COUNT" ]; then
      while [ "$LAST_COUNT" -lt "$CURRENT" ]; do
        LAST_COUNT=$((LAST_COUNT + 1))

        LINE=$(grep -E '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" | sed -n "${LAST_COUNT}p")

        if [ -z "$LINE" ]; then
          continue
        fi

        if echo "$LINE" | grep -q " PASSED: "; then
          ICON="✅"; MOD_PASSED=$((MOD_PASSED + 1)); TOTAL_PASSED=$((TOTAL_PASSED + 1))
        elif echo "$LINE" | grep -q " FAILED: "; then
          ICON="❌"; MOD_FAILED=$((MOD_FAILED + 1)); TOTAL_FAILED=$((TOTAL_FAILED + 1))
        else
          ICON="⏭️"; MOD_SKIPPED=$((MOD_SKIPPED + 1)); TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
        fi

        GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + 1))

        TEST_NAME=$(echo "$LINE" | sed 's/.*: [A-Za-z0-9_]*\.//' | sed 's/ (.*//')
        DURATION=$(echo "$LINE" | sed 's/.*(\([0-9]*\)s).*/\1/')
        [ -z "$DURATION" ] || [ "$DURATION" = "$LINE" ] && DURATION="?"

        ERROR_REASON=""
        if [ "$ICON" = "❌" ]; then
          NEARBY=$(grep -F -A5 "$LINE" "$LOG_FILE" 2>/dev/null | tail -n +2)
          ERROR_REASON=$(echo "$NEARBY" | grep -i "Error:" | sed 's/.*Error: //' | head -1)
          [ -z "$ERROR_REASON" ] && ERROR_REASON=$(echo "$NEARBY" | grep -i "assert\|exception\|timeout\|not found\|not visible\|NoSuchElement\|could not be located\|drift" | sed 's/^[[:space:]]*//' | head -1)
          [ -z "$ERROR_REASON" ] && ERROR_REASON="Test failed (check raw output for details)"
          ERROR_REASON=$(echo "$ERROR_REASON" | cut -c1-120)
        fi

        print_test_progress "$ICON" "$TEST_NAME" "$DURATION" "$ERROR_REASON"
      done
    fi

    sleep 1
  done

  # Wait for Maven to finish
  wait $MVN_PID
  MVN_EXIT=$?

  # ── Final check: catch any tests we missed ──
  FINAL_COUNT=$(grep -cE '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" 2>/dev/null)
  FINAL_COUNT=${FINAL_COUNT:-0}
  while [ "$LAST_COUNT" -lt "$FINAL_COUNT" ]; do
    LAST_COUNT=$((LAST_COUNT + 1))
    LINE=$(grep -E '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" | sed -n "${LAST_COUNT}p")

    if echo "$LINE" | grep -q " PASSED: "; then
      ICON="✅"; MOD_PASSED=$((MOD_PASSED + 1)); TOTAL_PASSED=$((TOTAL_PASSED + 1))
    elif echo "$LINE" | grep -q " FAILED: "; then
      ICON="❌"; MOD_FAILED=$((MOD_FAILED + 1)); TOTAL_FAILED=$((TOTAL_FAILED + 1))
    else
      ICON="⏭️"; MOD_SKIPPED=$((MOD_SKIPPED + 1)); TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
    fi

    GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + 1))
    TEST_NAME=$(echo "$LINE" | sed 's/.*: [A-Za-z0-9_]*\.//' | sed 's/ (.*//')
    DURATION=$(echo "$LINE" | sed 's/.*(\([0-9]*\)s).*/\1/')
    [ -z "$DURATION" ] || [ "$DURATION" = "$LINE" ] && DURATION="?"

    ERROR_REASON=""
    if [ "$ICON" = "❌" ]; then
      NEARBY=$(grep -F -A5 "$LINE" "$LOG_FILE" 2>/dev/null | tail -n +2)
      ERROR_REASON=$(echo "$NEARBY" | grep -i "Error:" | sed 's/.*Error: //' | head -1)
      [ -z "$ERROR_REASON" ] && ERROR_REASON=$(echo "$NEARBY" | grep -i "assert\|exception\|timeout\|not found\|not visible\|NoSuchElement\|could not be located\|drift" | sed 's/^[[:space:]]*//' | head -1)
      [ -z "$ERROR_REASON" ] && ERROR_REASON="Test failed (check raw output for details)"
      ERROR_REASON=$(echo "$ERROR_REASON" | cut -c1-120)
    fi

    print_test_progress "$ICON" "$TEST_NAME" "$DURATION" "$ERROR_REASON"
  done

  MODULE_END=$(date +%s)
  M_DURATION[$i]=$((MODULE_END - MODULE_START))

  # ── If real-time monitoring caught nothing, fall back to XML parsing ──
  DETECTED=$((MOD_PASSED + MOD_FAILED + MOD_SKIPPED))
  if [ "$DETECTED" -eq 0 ]; then
    RESULTS=$(parse_results_xml)
    read -r P F S <<< "$RESULTS"
    MOD_PASSED=$P; MOD_FAILED=$F; MOD_SKIPPED=$S
    TOTAL_PASSED=$((TOTAL_PASSED + P))
    TOTAL_FAILED=$((TOTAL_FAILED + F))
    TOTAL_SKIPPED=$((TOTAL_SKIPPED + S))
    GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + P + F + S))

    if [ "$((P + F + S))" -gt 0 ]; then
      local_elapsed=$(( $(date +%s) - SUITE_START ))
      local_elapsed_fmt=$(fmt_duration $local_elapsed)
      echo "    (parsed from results XML — real-time output was not available)"
      printf "    %d/%d completed   ⏱️ %s\n\n" "$GLOBAL_COMPLETED" "$TOTAL_TESTS" "$local_elapsed_fmt"
    fi
  fi

  M_PASSED[$i]=$MOD_PASSED
  M_FAILED[$i]=$MOD_FAILED
  M_SKIPPED[$i]=$MOD_SKIPPED

  # Determine module status
  if [ "$MOD_FAILED" -gt 0 ] || [ $MVN_EXIT -ne 0 ]; then
    STATUS[$i]="failed"
    HAS_FAILURE=1

    if [ "$DETECTED" -eq 0 ] && [ "$MOD_PASSED" -eq 0 ] && [ "$MOD_FAILED" -eq 0 ]; then
      M_FAILED[$i]=$TEST_COUNT
      TOTAL_FAILED=$((TOTAL_FAILED + TEST_COUNT))
      GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + TEST_COUNT))
    fi
  else
    STATUS[$i]="passed"
  fi

  # Print module completion summary
  print_module_complete $i ${M_DURATION[$i]}

  # ── Collapse raw Maven output ──
  echo "::group::${MODULE_NAME} — raw output (click to expand)"
  cat "$LOG_FILE" 2>/dev/null || echo "(no output)"
  echo "::endgroup::"

  # ── Save module reports ──
  mkdir -p "reports/modules/module-${MODULE_IDX}-${MODULE_KEY}"
  cp -r target/surefire-reports/* "reports/modules/module-${MODULE_IDX}-${MODULE_KEY}/" 2>/dev/null || true

done

# ── Final Results ──────────────────────────────────
draw_final_dashboard
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
