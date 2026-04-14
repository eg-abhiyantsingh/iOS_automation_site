# CI Build Fix Changelog - April 9, 2026

**Date**: April 9, 2026
**Author**: Abhiyant Singh (assisted by Claude Code)
**Prompt Title**: Fix iOS App Build Failure on CI - Swift Type-Checker Timeout & SIGPIPE Crashes
**Status**: Final fix deployed (PR #175)

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

### Issue 1: Swift Constraint Solver Scope Limit (PRIMARY — ACTUAL ROOT CAUSE)

The Swift compiler's constraint solver has **THREE independent limits** that trigger `expression_too_complex`:

| Limit | Threshold | CLI Override? | What it measures |
|---|---|---|---|
| **SolverBindingThreshold** | 1,048,576 | **NO** (hardcoded) | Number of type-binding branches explored |
| ExpressionTimeoutThreshold | 600s (default) | Yes (`-solver-expression-time-threshold=N`) | Process CPU time per expression |
| SolverMemoryThreshold | 512 MB | No | Memory used by constraint solver |

**The actual bottleneck is `SolverBindingThreshold`** — the 1M branch limit is hardcoded in the Swift compiler with no CLI flag to change it.

`AddAssetView.swift` line 78 has a `var body: some View` expression spanning **519 lines** (lines 78-597) with deeply nested SwiftUI views (`NavigationStack → ZStack → ScrollView → VStack → VStack` with inline sections containing `if`/`ForEach`/closures). This generates >1M constraint solver branches.

| Factor | Developer Machine | CI Runner (macos-15) |
|---|---|---|
| CPU | M2/M3 Pro (8-16 cores) | M1 (3 cores) |
| RAM | 16-64 GB | 7 GB |
| Build Type | Incremental (cached) | Clean (fresh every run) |
| Result | May type-check OK (cached/parallel) | Exceeds 1M branches on clean build |

**Why timeout flags cannot fix this**: Even with 3600s (1 hour) timeout, the build still fails — because it hits the branch count limit first, not the time limit. This was confirmed in CI run `24181905856` where the build ran 4m16s (longer than before, proving the extended timeout WAS applied) but still failed with the same error.

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

### Change 1 (FINAL FIX): Extract @ViewBuilder properties from AddAssetView body

**File Modified:**
- `Egalvanic/eg-pz-mobile-iOS/Egalvanic PZ/Views/Assets/AddAssetView.swift`

**PR:** Egalvanic/eg-pz-mobile-iOS#175 (`fix/ci-type-checker-body-extraction` → `release/qa`)

**What was done:**
Extracted 6 heavy inline sections from the 519-line `var body: some View` into separate `@ViewBuilder` computed properties:

| Property | Lines Extracted | Description |
|---|---|---|
| `assetDetailsCard` | ~200 lines | Name, room picker, asset class, QR code, voltage |
| `equipmentLibraryCard` | ~12 lines | Equipment library section |
| `photosCard` | ~28 lines | Photo picker section |
| `notesCard` | ~29 lines | Notes text editor |
| `irPhotosCard` | ~100 lines | IR photo session & staging |
| `bottomActionBar` | ~28 lines | Create asset button bar |
| `savingOverlay` | ~22 lines | Full-screen save progress |

**Result:** Body reduced from **520 → 111 lines** (78% reduction).

**Why this works:**
Each `@ViewBuilder` computed property returns an opaque `some View` type. The constraint solver treats each as an **independent expression** — it never explores branches across property boundaries. This keeps each individual expression well under the 1M branch limit while the total code remains functionally identical.

Also added a `showBottomBar` helper property to DRY up the repeated 3-part condition (`!assetName.isEmpty && selectedNodeClass != nil && selectedRoom != nil`).

### Change 1b (SUPPLEMENTARY — no longer the fix): Swift Type-Checker Timeout via xcconfig

**Files Modified:**
- `Egalvanic/eg-pz-mobile-iOS/.github/workflows/build-app-only.yml`
- `eg-abhiyantsingh/iOS_automation_site/.github/workflows/ios-tests-smoke-repodeveloper.yml`
- `eg-abhiyantsingh/iOS_automation_site/.github/workflows/ios-tests-repodeveloper-parallel.yml`

**Note:** This xcconfig approach was our initial fix attempt. It correctly increases the *time* threshold, but the actual bottleneck is the *branch count* limit (which has no CLI override). The xcconfig is still present and acts as a safety net for other potentially slow expressions, but the real fix is Change 1.

**What was added (inside the "Build iOS App for Simulator" step):**
```yaml
cat > /tmp/ci-overrides.xcconfig << 'XCCONFIG'
// CI build overrides — increase Swift type-checker timeout for slower CI hardware
// Timer uses process CPU time (all threads), not wall clock. 3600s = 1 hour.
OTHER_SWIFT_FLAGS = $(inherited) -Xfrontend -solver-expression-time-threshold=3600
XCCONFIG
```

**Why xcconfig instead of command-line flags:**
- `OTHER_SWIFT_FLAGS='$(inherited) ...'` on the command line has shell quoting issues
- `$(inherited)` looks like a shell subcommand to bash, but it's an Xcode build setting macro
- An xcconfig file lets Xcode's parser handle `$(inherited)` correctly
- `$(inherited)` ensures we ADD to existing project flags, not override them

**Critical: `=` syntax is required (Joined option):**
- The flag is defined as `Joined<["-"], "solver-expression-time-threshold=">` in the Swift compiler
- This means the value MUST be joined with `=`: `-solver-expression-time-threshold=3600`
- Space-separated form causes: `error: unknown argument`
- Only ONE `-Xfrontend` prefix is needed: `-Xfrontend -solver-expression-time-threshold=3600`

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
| 2026-04-09 ~08:01 | Egalvanic/eg-pz-mobile-iOS | `99fd93e` | Fix shutdown command in QA automation workflow |
| 2026-04-09 ~08:35 | Egalvanic/eg-pz-mobile-iOS | `b8a660f` | Fix: Use `=` syntax for solver-expression-time-threshold flag |
| 2026-04-09 ~08:50 | Egalvanic/eg-pz-mobile-iOS | `d96d965` | Diagnostic: Increase threshold to 3600s to rule out time limit |
| 2026-04-09 ~09:30 | Egalvanic/eg-pz-mobile-iOS | `6d4fd2a` | **FINAL FIX: Extract @ViewBuilder properties from body (PR #175)** |

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

1. **Merge PR #175**: https://github.com/Egalvanic/eg-pz-mobile-iOS/pull/175
2. Go to https://github.com/Egalvanic/eg-pz-mobile-iOS/actions
3. Run "Internal - Generate Build" workflow with:
   - Environment: `qa`
   - Branch: `release/qa` (after merging PR #175)
4. The build step should now succeed — the body expression is small enough for the constraint solver
5. Test CI run on the feature branch: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24183148525

---

## Key Lessons

1. **Swift has THREE independent type-checker limits** — Time (600s default), Memory (512MB), and Binding Count (1M). The binding count is hardcoded with NO CLI override. Always check which limit you're actually hitting before trying to increase thresholds.

2. **`expression_too_complex` usually means "break up the expression"** — The compiler's error message literally tells you the fix. A 500+ line SwiftUI body will always hit the binding threshold. Extract sections into `@ViewBuilder` computed properties — each becomes an independent type-checking scope.

3. **CI runners are weaker than dev machines** — `macos-15` GitHub runner has only M1/3-core/7GB. Clean builds on CI explore more constraint solver branches than incremental builds locally.

4. **Never pipe xcodebuild output** — On macOS, piping through `tee`, `grep`, or `tail` causes SIGPIPE crashes. Always redirect to a file.

5. **Two repos = two workflow files** — The QA automation repo and the developer repo each have their own workflow files. Fixing one doesn't fix the other.

6. **`$(inherited)` in xcconfig** — This Xcode macro means "inherit from the previous level." In an xcconfig passed to xcodebuild, it inherits from the project's build settings, ensuring we append flags rather than override them.

7. **"Re-run" vs "New dispatch"** — Re-running a workflow from the GitHub Actions UI re-uses the **same workflow YAML from the original commit**. To pick up a new workflow file, you must trigger a **brand new** `workflow_dispatch` from the Actions tab (or API).

8. **Default branch matters for workflow discovery** — GitHub Actions discovers `workflow_dispatch` workflows from the **default branch** (in this repo: `release/prod`). If the workflow file only exists on `main`, GitHub shows it as "deleted" state and may not appear in the UI. It can still be triggered via API.

9. **Joined options require `=` syntax** — Swift compiler flags defined as `Joined` in LLVM (like `-solver-expression-time-threshold=N`) must use `=`. Space-separated form (`-flag value`) is treated as two separate arguments — the first with no value (error) and the second as an unknown argument.

10. **GitHub API push can create orphan commits** — When pushing via the Contents API, if the base SHA is wrong or the ref update fails silently, the commit exists in the object store but no branch points to it. Always verify with `gh api repos/OWNER/REPO/git/ref/heads/BRANCH` after pushing.

---

## Debugging Session 2: "Still Failed" Investigation (April 9, 2026 ~08:20 UTC)

### Problem
User reported the fix "still failed" and linked run `24082086043`. Investigation revealed:

1. **Run `24082086043` was triggered on April 7** (head SHA: `9ae78648c709`)
2. **Our fix was pushed on April 9** (commit `58ae1f8`)
3. The user "Re-ran" the old workflow — which reuses the OLD workflow file from the original commit
4. The xcodebuild command in that run had NO `-xcconfig` flag (confirmed from logs)

### Root Cause Chain
```
User clicks "Re-run all jobs" on old run
  → GitHub reuses workflow YAML from commit 9ae7864 (April 7)
    → That version has NO xcconfig block
      → xcodebuild uses default 15s type-checker timeout
        → AddAssetView.swift:78 exceeds 15s on M1/3-core CI
          → SAME ERROR as before
```

### Additional Discovery: Default Branch Issue
- Repo default branch: `release/prod`
- `build-app-only.yml` only exists on `main`
- GitHub workflow state: `"deleted"` (not on default branch)
- Workflow can still be triggered via API targeting `main` branch

### Fix
Triggered a new workflow dispatch via GitHub API:
```bash
gh api repos/Egalvanic/eg-pz-mobile-iOS/actions/workflows/230860064/dispatches \
  -X POST -f ref=main \
  -f 'inputs[environment]=qa' \
  -f 'inputs[branch]=release/qa'
```

New run: `24180193342` — uses commit `99fd93e7a2ef` (parent is our fix `58ae1f8`)

### Commit Ancestry (Verified)
```
58ae1f8 (our fix: xcconfig + pipe removal)
  └── 99fd93e7a2ef (current main HEAD: "Fix shutdown command in QA automation workflow")
```

---

## Debugging Session 3: "Unknown Argument" Error (April 9, 2026 ~08:30 UTC)

### Problem
CI run `24180193342` (the new dispatch) failed with a different error:
```
error: unknown argument: '-solver-expression-time-threshold'
```

### Root Cause
The `-solver-expression-time-threshold` flag is a **Joined** option in the LLVM option parser (defined as `Joined<["-"], "solver-expression-time-threshold=">`). This means:
- The value must be joined with `=`: `-solver-expression-time-threshold=240`
- Space-separated form (`-solver-expression-time-threshold 240`) is TWO separate arguments — the first has no value (error) and the second (`240`) is an unknown argument

We had:
```
-Xfrontend -solver-expression-time-threshold -Xfrontend 240
```
(Two separate `-Xfrontend`-prefixed args — the compiler sees two separate options)

### Fix
Changed to:
```
-Xfrontend -solver-expression-time-threshold=240
```
Commit `b8a660f` on `main` branch.

---

## Debugging Session 4: Ruling Out Time Limit (April 9, 2026 ~08:50 UTC)

### Problem
CI run `24180750982` (with correct `=` syntax and 240s threshold) still failed with `expression_too_complex` on `AddAssetView.swift:78`. The build failed in ~2.5 minutes.

### Investigation
- The default `ExpressionTimeoutThreshold` is **600 seconds** (not 15s as initially assumed)
- Our 240s setting was actually **lowering** the timeout from the default
- Increased to 3600s (1 hour) to definitively rule out time as a factor

### Result (Run `24181905856`)
Build ran **4 minutes 16 seconds** (longer than the 2.5min with 240s, confirming the solver used more time) but **still failed** with the same error.

### Conclusion
**The bottleneck is NOT the time limit.** It's the **SolverBindingThreshold** (1,048,576 type-binding branches). This limit is hardcoded in the Swift compiler source at `lib/Sema/CSSimplify.cpp` with no CLI override. The only fix is to modify the source code to break up the expression.

---

## Debugging Session 5: The Final Fix (April 9, 2026 ~09:00 UTC)

### Analysis of AddAssetView.swift
- `var body: some View` spans lines 78-597 (**519 lines**)
- Contains deeply nested SwiftUI hierarchy: `NavigationStack → ZStack → ScrollView → VStack → VStack`
- Inline sections include: 200-line Basic Info card, 100-line IR Photos section, 28-line Photos section, etc.
- Each `if`/`ForEach`/closure creates branching paths for the constraint solver
- Already had some extracted properties (`listeningTasksSection`, `coreAttributesSection`, etc.) — proves the pattern works

### Fix Applied
Extracted 6 heavy inline sections into `@ViewBuilder` computed properties:
1. `assetDetailsCard` (~200 lines) — the biggest offender
2. `irPhotosCard` (~100 lines)
3. `photosCard` (~28 lines)
4. `notesCard` (~29 lines)
5. `bottomActionBar` (~28 lines)
6. `savingOverlay` (~22 lines)

Plus `equipmentLibraryCard` and `showBottomBar` helper.

**Body reduced from 520 → 111 lines (78% reduction).**

### Why This Works (Technical Deep Dive)
The Swift constraint solver uses opaque return types (`some View`) as **type boundaries**. When the body calls `assetDetailsCard`, the solver knows the result type is `some View` — it does NOT descend into the property's implementation to explore its branches. Each property is type-checked independently.

Before: One expression with N₁ × N₂ × N₃ × ... branches (multiplicative)
After: Multiple expressions with N₁ + N₂ + N₃ + ... branches (additive)

This is exactly what the compiler's error message recommends: "try breaking up the expression into distinct sub-expressions."

### Verification
- File syntax verified: all braces (222/222), parens (558/558), brackets (13/13) balanced
- All view references preserved: every `ModernTextField`, `EntityFancyPhotoPicker`, etc. appears same number of times
- Struct boundaries correct: `AddAssetViewV2` closes properly, all helper structs intact
- PR created: Egalvanic/eg-pz-mobile-iOS#175
- CI run `24183148525`: **BUILD SUCCEEDED** — all 17 steps passed
- Run URL: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24183148525
