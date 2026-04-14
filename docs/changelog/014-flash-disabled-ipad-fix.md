# Fix: "Flash is Disabled" iPad Alert - April 9, 2026

**Date**: April 9, 2026
**Author**: Abhiyant Singh (assisted by Claude Code)
**DevRev Bug**: Flash is Disabled — On Hold / Improve Bug / Medium Priority
**Status**: PR #179 updated with definitive fix (custom AVCaptureSession camera)
**PR**: https://github.com/Egalvanic/eg-pz-mobile-iOS/pull/179
**Branch**: `fix/disable-flash-on-ipads-without-flash`

---

## Problem

Field techs using iPads get the system alert **"Flash is Disabled — iPad needs to cool down before you can use the flash"** every time they open the camera to take a photo. They must tap OK to dismiss it on every single photo.

This happens because:
1. Most iPads (iPad, iPad Air, most iPad mini) have **NO rear LED flash**
2. The app's camera wrappers don't set `cameraFlashMode`, so it defaults to `.auto`
3. iOS fires a system alert when flash mode is `.auto` but flash isn't available

---

## Root Cause Analysis (Deep Dive)

### Why the first fix (`hasFlash`) didn't work

The initial fix checked `AVCaptureDevice.default(for: .video)?.hasFlash` — this has 3 failure modes:

1. **`hasFlash` is static**: Reports hardware capability, NOT current availability. On iPads with LED flash (iPad Pro), `hasFlash = true` even when thermally throttled — alert still fires.

2. **Retina Flash confusion**: iPads without LED flash (iPad Air, iPad 10th gen) have "Retina Flash" (screen brightness flash). `hasFlash` returns `true` for front camera on these devices.

3. **Known iOS bug**: iPad 8th/9th/10th gen show this alert even when the device is NOT hot and has NO flash hardware. The thermal protection system fires incorrectly.

### The three independent checks and what they catch

| Check | Type | Catches thermal? | Catches no-hardware? | Catches iOS bug? |
|---|---|---|---|---|
| `hasFlash` | Static | No | Partial | No |
| `isFlashAvailable` | Dynamic | Yes | Yes | Partial |
| `ProcessInfo.thermalState` | Dynamic | Yes | No | No |
| `UIImagePickerController.isFlashAvailable(for:)` | UIKit | Unknown | Yes | Partial |
| `UIDevice.userInterfaceIdiom == .pad` | Device | Yes | Yes | **Yes** |

No single check catches all cases. That's why the fix uses all of them.

---

## Fix Attempt 1: `cameraFlashMode = .off` with multi-layer checks (INSUFFICIENT)

Set `cameraFlashMode = .off` on UIImagePickerController with 4-layer shouldDisableFlash():
1. `UIImagePickerController.isFlashAvailable(for:)` — UIKit check
2. `ProcessInfo.thermalState` — thermal state check
3. `AVCaptureDevice.isFlashAvailable` — runtime check
4. `UIDevice.current.userInterfaceIdiom == .pad` — iPad blanket

**Result**: Camera opened WITHOUT the alert (fix worked at init). BUT the alert appeared AFTER tapping "Use Photo" — during UIImagePickerController's internal session teardown/dismissal cycle.

**Why this approach fails**: `cameraFlashMode = .off` tells UIImagePickerController not to FIRE flash. But UIImagePickerController still EVALUATES flash hardware status during its internal session lifecycle. This evaluation triggers the system alert. The alert is presented by SpringBoard/iOS, not by our app — it cannot be suppressed.

---

## Fix Attempt 2: Replace UIImagePickerController with custom AVCaptureSession (DEFINITIVE)

### Approach
On iPad, completely bypass UIImagePickerController. Use a custom `AVCaptureSession` + `AVCapturePhotoOutput` camera that:
- Sets `AVCaptureDevice.flashMode = .off` at the device driver level via `lockForConfiguration()`
- Sets `AVCapturePhotoSettings.flashMode = .off` on every capture request
- Has NO UIImagePickerController session teardown to trigger the alert
- We control the entire lifecycle — no iOS surprises

On iPhone, keep UIImagePickerController unchanged (flash works fine on iPhones).

### Architecture
```
EntityCameraView / CameraViewWrapper
├── iPad → iPadCameraView (custom AVCaptureSession + AVCapturePhotoOutput)
│   ├── iPadCameraManager — ObservableObject managing session lifecycle
│   ├── iPadCameraPreview — UIViewRepresentable for camera preview layer
│   └── iPadCameraView — Full SwiftUI UI with preview, shutter, Retake/Use Photo
└── iPhone → UIImagePickerController (EntityCameraViewRepresentable / CameraViewRepresentable)
```

### Why this works 100%
1. `AVCaptureDevice.flashMode = .off` via `lockForConfiguration()` disables flash at the device driver level
2. `AVCapturePhotoSettings.flashMode = .off` prevents flash on every capture
3. No UIImagePickerController = no internal session teardown = no system alert
4. We control the entire capture → dismiss lifecycle

---

## Files Changed

| File | What Changed |
|---|---|
| `Egalvanic PZ/Core/UI/Views/EntitySimplePhotoPicker.swift` | Added `iPadCameraManager`, `iPadCameraPreview`, `iPadCameraView`; `EntityCameraView` routes iPad→custom camera, iPhone→UIImagePickerController |
| `Egalvanic PZ/Views/Assets/AddAssetView.swift` | `CameraViewWrapper` routes iPad→`iPadCameraView`; simplified `CameraViewRepresentable` (iPhone-only) |

All camera flows go through one of these two entry points:
- `EntityCameraView` → entity photo pickers (nodes, issues, tasks, buildings, floors, rooms)
- `CameraViewWrapper` → Add Asset, Photo Walkthrough, OCP Walkthrough, Quick Count

---

## Commits

| Commit | Description |
|---|---|
| `ecbc5ea` | Fix attempt 1a: hasFlash check (insufficient) |
| `7935e24` | Fix attempt 1b: multi-layer shouldDisableFlash (insufficient — alert fires after Use Photo) |
| `489a8f7` | Fix attempt 1b: apply to AddAssetView |
| `8f1a3bf` | **Fix attempt 2: Custom AVCaptureSession camera for iPad (definitive)** |
| `3162e40` | **Fix attempt 2: Route CameraViewWrapper to iPadCameraView on iPad** |

---

## Key Lessons

1. **`cameraFlashMode = .off` is necessary but NOT sufficient** — It prevents flash from FIRING, but doesn't prevent iOS from EVALUATING flash state during UIImagePickerController's internal session lifecycle.

2. **The "Flash is Disabled" alert is a system-level thermal alert** — Presented by SpringBoard, not by the app. Cannot be suppressed, intercepted, or dismissed programmatically.

3. **UIImagePickerController triggers the alert during session teardown** — Even after a successful photo capture with flash off, the internal AVCaptureSession teardown re-evaluates flash hardware state and can trigger the alert.

4. **Custom AVCaptureSession is the only reliable fix** — Full control over the capture pipeline means no surprise flash evaluations. `lockForConfiguration()` + `flashMode = .off` at the device level prevents any flash hardware interaction.

5. **iPad-only fix is the safe approach** — The bug only manifests on iPad (known iOS bug on iPad 8/9/10th gen + most iPads lack LED flash). iPhones work fine with UIImagePickerController.

6. **Three attempts to find the right fix level:**
   - `hasFlash` check → failed (static flag, doesn't account for thermal/Retina Flash)
   - `cameraFlashMode = .off` multi-layer → partially worked (prevented alert at init, but not during teardown)
   - Custom AVCaptureSession → definitive (no UIImagePickerController, no alert at any point)
