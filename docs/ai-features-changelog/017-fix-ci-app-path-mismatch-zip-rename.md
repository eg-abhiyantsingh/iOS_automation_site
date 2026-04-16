# Changelog 017 — Fix CI Failure: App Bundle Name Mismatch (Hyphen vs Space)

**Date**: 2026-04-16  
**Time**: ~13:30 IST  
**Prompt**: Fix failing CI run — TC_ISS_118 failure traced to WDA warm-up crash  
**CI Run**: https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/runs/24507538401

---

## Summary

All CI jobs were failing because the app binary couldn't be found. The zip file extracts to `Z-Platform-QA.app` (hyphen) but every workflow references `Z Platform-QA.app` (space). Added a safe rename step after every unzip across all 4 workflow files.

---

## Root Cause Investigation

### What the CI Logs Showed

The "Warm-up WDA Session" step failed on all 5 attempts with:

```
The application at '/Users/runner/work/iOS_automation_site/iOS_automation_site/apps/Z Platform-QA.app'
does not exist or is not accessible
```

The simulator was booted (iPhone 16 Pro, iOS 18.5, UDID 2BA77BF1) and Appium was running — only the app path was wrong.

### The Mismatch

| Source | Name |
|--------|------|
| Zip file contents | `Z-Platform-QA.app/` (hyphen between Z and Platform) |
| Workflow `APP_PATH` | `Z Platform-QA.app` (space between Z and Platform) |

The `unzip -o apps/Z-Platform-QA.zip -d apps/` command extracts the bundle as `apps/Z-Platform-QA.app/`, but all workflow steps reference `apps/Z Platform-QA.app`.

### Why the Names Differ

In iOS app bundles, these are independent values in `Info.plist`:
- **`CFBundleName`** (display name): `Z Platform-QA` (with space)
- **Bundle directory**: `Z-Platform-QA.app` (with hyphen)
- **Executable inside bundle**: `Z Platform-QA` (with space)

The developer's build system uses a hyphen in the bundle directory name but a space in the display/executable name. The workflow was written to match the executable name, not the bundle directory name.

---

## Fix: Safe Rename After Every Unzip

Added this block after every `unzip` command:

```bash
if [ -d "apps/Z-Platform-QA.app" ] && [ ! -d "apps/Z Platform-QA.app" ]; then
  mv "apps/Z-Platform-QA.app" "apps/Z Platform-QA.app"
  echo "Renamed Z-Platform-QA.app -> Z Platform-QA.app"
fi
```

### Why a Guard (`if`) Instead of Bare `mv`

The `if` check makes this safe in two scenarios:
1. **Current state**: zip extracts with hyphen → rename happens
2. **Future state**: if the developer rebuilds the zip with a space in the name → rename is skipped (both conditions fail)

Without the guard, a bare `mv` would fail if the source doesn't exist (e.g., developer fixes the zip name).

---

## Files Changed

| File | Unzip Steps Fixed |
|------|-------------------|
| `.github/workflows/ios-tests-smoke.yml` | 1 |
| `.github/workflows/ios-tests-parallel.yml` | 17 (16 from zip + 1 from S3 download) |
| `.github/workflows/ios-tests-quick-verify.yml` | 7 (1 multi-line + 6 single-line) |
| `.github/workflows/ios-tests.yml` | 1 |
| **Total** | **26 unzip steps** |

### Pattern 1: Multi-line with `ls` (smoke, parallel-first-jobs, ios-tests)

```yaml
# Before
- name: Unzip App
  run: |
    unzip -o apps/Z-Platform-QA.zip -d apps/
    ls -la apps/

# After
- name: Unzip App
  run: |
    unzip -o apps/Z-Platform-QA.zip -d apps/
    if [ -d "apps/Z-Platform-QA.app" ] && [ ! -d "apps/Z Platform-QA.app" ]; then
      mv "apps/Z-Platform-QA.app" "apps/Z Platform-QA.app"
      echo "Renamed Z-Platform-QA.app -> Z Platform-QA.app"
    fi
    ls -la apps/
```

### Pattern 2: Single-line (most parallel jobs, quick-verify)

```yaml
# Before
- name: Unzip App
  run: unzip -o apps/Z-Platform-QA.zip -d apps/

# After
- name: Unzip App
  run: |
    unzip -o apps/Z-Platform-QA.zip -d apps/
    if [ -d "apps/Z-Platform-QA.app" ] && [ ! -d "apps/Z Platform-QA.app" ]; then
      mv "apps/Z-Platform-QA.app" "apps/Z Platform-QA.app"
      echo "Renamed Z-Platform-QA.app -> Z Platform-QA.app"
    fi
```

### Pattern 3: S3 Download (parallel workflow, s3-drift-appium job)

```yaml
# Before
cd apps && unzip -q app.zip && rm app.zip
echo "App downloaded for env: ${TARGET_ENV}"

# After
cd apps && unzip -q app.zip && rm app.zip
cd ..
if [ -d "apps/Z-Platform-QA.app" ] && [ ! -d "apps/Z Platform-QA.app" ]; then
  mv "apps/Z-Platform-QA.app" "apps/Z Platform-QA.app"
  echo "Renamed Z-Platform-QA.app -> Z Platform-QA.app"
fi
echo "App downloaded for env: ${TARGET_ENV}"
```

---

## Additional Finding: Appium Version Warning

The CI logs also showed:

```
[XCUITest] appium-xcuitest-driver requires appium version ^2.5.4
(the current version is 2.0.1)
```

The workflow installs `appium@next` (line 194 in smoke), which resolves to v2.0.1. The xcuitest driver expects v2.5.4+. This is a **warning only** — it doesn't block execution — but may cause subtle compatibility issues. A future fix could pin the Appium version: `npm install -g appium@2.5.4`.

---

## Key Concepts

### iOS App Bundle Structure

An iOS `.app` bundle is a **directory** (not a single file) with a specific structure:

```
Z-Platform-QA.app/           <- Bundle directory name (can have any name)
  Info.plist                  <- Metadata (CFBundleName, CFBundleIdentifier, etc.)
  Z Platform-QA               <- Executable binary (name from CFBundleExecutable)
  Z Platform-QA.debug.dylib   <- Debug symbols
  Assets.car                  <- Compiled asset catalog
  Frameworks/                 <- Embedded frameworks
  _CodeSignature/             <- Code signing data
```

The bundle directory name (`Z-Platform-QA.app`) and the executable name (`Z Platform-QA`) are independent — they're set in different places in Xcode's build settings. This is why the mismatch exists.

### `mobile: tap` vs `.click()` for WDA Warm-up

The WDA warm-up step doesn't test taps — it creates an Appium session to verify:
1. WebDriverAgent can be built and launched on the simulator
2. The app binary exists and can be installed
3. The simulator is responsive

If step 2 fails (app not found), no session is created, and all subsequent tests fail with "no active session" errors.

### Why This Wasn't Caught Earlier

The zip file was committed to the repo as `Z-Platform-QA.zip`. The workflows were written with `Z Platform-QA.app` (space) as the `APP_PATH`. These two facts were never connected — the `Unzip App` step ran, the zip extracted, but no one verified the extracted directory name matched the `APP_PATH`.

The `ls -la apps/` in the unzip step would have shown `Z-Platform-QA.app/` (hyphen) in the CI logs, but it wasn't inspected because the failure appeared much later in the "Warm-up WDA Session" step.

---

## Expected Impact

| Before | After |
|--------|-------|
| ALL CI jobs fail — WDA warm-up can't find app | App is found at expected path |
| 5 warm-up retries × 30s each = 2.5 min wasted before hard failure | Rename happens in <1s during unzip step |
| Error appears in WDA step (misleading) | If rename fails, error appears in Unzip step (clear) |

---

## Status

- App path mismatch: **FIXED** (26 unzip steps across 4 workflow files)
- Appium version warning: **IDENTIFIED** (not fixed — cosmetic warning, not blocking)
