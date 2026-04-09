# CI Build Fix Changelog - April 9, 2026

**Date**: April 9, 2026
**Author**: Abhiyant Singh (assisted by Claude Code)
**Prompt Title**: Fix iOS App Build Failure on CI - Swift Type-Checker Timeout & SIGPIPE Crashes
**Status**: Completed

---

## Problem Summary

The iOS app (`Egalvanic PZ-QA`) was failing to build on GitHub Actions CI, even though the developer could build it successfully on their local machine.

**Error Message (from CI logs):**
```
AddAssetView.swift:78:25: error: the compiler is unable to type-check
this expression in reasonable time; try breaking up the expression into
distinct sub-expressions
```

**Affected CI Runs:**
- https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24071824908
- https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24082086043

---

## Root Cause Analysis

### Issue 1: Swift Type-Checker Timeout (PRIMARY)

The Swift compiler has a per-expression **time threshold** for type checking. When a SwiftUI view body contains deeply nested views with complex generic type inference, the compiler's constraint solver must explore many possible type combinations.

| Factor | Developer Machine | CI Runner (macos-15) |
|---|---|---|
| CPU | M2/M3 Pro (8-16 cores) | M1 (3 cores) |
| RAM | 16-64 GB | 7 GB |
| Build Type | Incremental (cached) | Clean (fresh every run) |
| Result | Type-checks within ~15s default | Exceeds ~15s default |

The expression at `AddAssetView.swift:78` is valid Swift code. It just takes longer to type-check on the slower CI hardware, exceeding the compiler's default timeout.

### Issue 2: SIGPIPE / Broken Pipe Crashes

Multiple workflow steps piped `xcodebuild` output through `tee`, `grep`, or `tail`:
```bash
# BEFORE (broken):
xcodebuild build ... 2>&1 | tee build.log
xcodebuild -resolvePackageDependencies ... 2>&1 | tail -3
```

When the downstream command (`tee`/`tail`) reads enough data and closes its stdin, the OS sends `SIGPIPE` to `xcodebuild`. On macOS, this crashes xcodebuild with:
```
NSFileHandleOperationException: Broken pipe
```

### Issue 3: Wrong Workflow File Being Fixed

We initially fixed `ios-tests-smoke-repodeveloper.yml` (in our QA repo), but the developer was running `build-app-only.yml` (in the developer repo `Egalvanic/eg-pz-mobile-iOS`). These are completely separate workflow files.

---

## Code Changes

### Change 1: Swift Type-Checker Timeout via xcconfig

**Files Modified:**
- `Egalvanic/eg-pz-mobile-iOS/.github/workflows/build-app-only.yml`
- `eg-abhiyantsingh/iOS_automation_site/.github/workflows/ios-tests-smoke-repodeveloper.yml`
- `eg-abhiyantsingh/iOS_automation_site/.github/workflows/ios-tests-repodeveloper-parallel.yml`

**What was added (inside the "Build iOS App for Simulator" step):**
```yaml
# Create xcconfig to increase Swift type-checker timeout
cat > /tmp/ci-overrides.xcconfig << 'XCCONFIG'
// CI build overrides - increase Swift type-checker timeout for slower CI hardware
OTHER_SWIFT_FLAGS = $(inherited) -Xfrontend -solver-expression-time-threshold -Xfrontend 240
XCCONFIG

# Pass xcconfig to xcodebuild
xcodebuild build \
  ...
  -xcconfig /tmp/ci-overrides.xcconfig \
  ...
```

**Why xcconfig instead of command-line flags:**
- `OTHER_SWIFT_FLAGS='$(inherited) ...'` on the command line has shell quoting issues
- `$(inherited)` looks like a shell subcommand to bash, but it's an Xcode build setting macro
- An xcconfig file lets Xcode's parser handle `$(inherited)` correctly
- `$(inherited)` ensures we ADD to existing project flags, not override them

**What `-solver-expression-time-threshold 240` does:**
- Tells the Swift compiler's constraint solver to spend up to 240 seconds (4 minutes) per expression
- Default is ~15 seconds, which is too short for the CI's slower M1 chip
- This does NOT hide bugs - the expression is valid, it just needs more compute time

### Change 2: Remove SIGPIPE-Causing Pipes

**Before:**
```bash
xcodebuild build ... 2>&1 | tee build.log
BUILD_RESULT=${PIPESTATUS[0]}
```

**After:**
```bash
set +e
xcodebuild build ... > /tmp/xcodebuild.log 2>&1
BUILD_RESULT=$?
set -e
```

**Why this matters:**
- `| tee` creates a pipe. When `tee` exits, the pipe breaks.
- xcodebuild gets SIGPIPE and crashes with `NSFileHandleOperationException`
- Redirecting to file (`> file 2>&1`) avoids pipes entirely
- `set +e` / `set -e` lets us capture the exit code without bash exiting early

**Same fix applied to:**
- SPM dependency resolution step (`| tee resolve.log` -> `> /tmp/spm-resolve.log 2>&1`)
- WDA pre-build step (`| tail -3` -> `> /tmp/wda-build.log 2>&1`)
- Appium install step (`| tail -1` -> `> /dev/null 2>&1`)

### Change 3: Better Error Reporting

**Before:** Generic error messages, no log uploads
**After:**
```yaml
if [ "$BUILD_RESULT" -ne 0 ]; then
  echo "--- Swift Compilation Errors ---"
  grep "error:" /tmp/xcodebuild.log | head -40 || echo "(no errors found)"
  echo "--- Build Summary (last 30 lines) ---"
  tail -30 /tmp/xcodebuild.log
  exit 1
fi
```

Also added artifact upload for both `xcodebuild-error.log` and `spm-error.log` on failure.

---

## Commits

| Date/Time (UTC) | Repo | Commit | Description |
|---|---|---|---|
| 2026-04-09 ~05:30 | eg-abhiyantsingh/iOS_automation_site | `f8f6bf8` | Remove all SIGPIPE-causing pipes in workflow files |
| 2026-04-09 ~05:45 | eg-abhiyantsingh/iOS_automation_site | `9654a57` | Add Swift type-checker timeout via xcconfig |
| 2026-04-09 ~07:50 | Egalvanic/eg-pz-mobile-iOS | `58ae1f8` | Fix build-app-only.yml with xcconfig + pipe removal |

---

## Files Changed (Summary)

### In QA Repo (eg-abhiyantsingh/iOS_automation_site)
| File | Changes |
|---|---|
| `.github/workflows/ios-tests-smoke-repodeveloper.yml` | xcconfig, pipe fixes, SPM error handling, build log upload |
| `.github/workflows/ios-tests-repodeveloper-parallel.yml` | Same xcconfig + pipe fixes (all 16 job sections) |
| `.github/workflows/ios-tests-parallel.yml` | Pipe fixes for appium install + WDA build |

### In Developer Repo (Egalvanic/eg-pz-mobile-iOS)
| File | Changes |
|---|---|
| `.github/workflows/build-app-only.yml` | xcconfig, pipe fixes, better error output |

---

## How to Verify

1. Go to https://github.com/Egalvanic/eg-pz-mobile-iOS/actions
2. Run "Internal - Generate Build" workflow with:
   - Environment: `qa`
   - Branch: `release/qa`
3. The build step should now succeed (type-checker has 240s instead of 15s)
4. If it still fails, the log will now show clear error messages instead of SIGPIPE crashes

---

## Key Lessons

1. **CI runners are weaker than dev machines** - `macos-15` GitHub runner has only M1/3-core/7GB. Always account for this when CI builds complex Swift projects.

2. **Never pipe xcodebuild output** - On macOS, piping through `tee`, `grep`, or `tail` causes SIGPIPE crashes. Always redirect to a file.

3. **Two repos = two workflow files** - The QA automation repo and the developer repo each have their own workflow files. Fixing one doesn't fix the other.

4. **`$(inherited)` in xcconfig** - This Xcode macro means "inherit from the previous level." In an xcconfig passed to xcodebuild, it inherits from the project's build settings, ensuring we append flags rather than override them.
