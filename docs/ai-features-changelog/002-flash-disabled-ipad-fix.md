# 002 — Flash is Disabled iPad Alert Fix

**Date:** 2026-04-12 to 2026-04-13  
**Branch:** `fix/disable-flash-on-ipads-without-flash`  
**Repo:** `Egalvanic/eg-pz-mobile-iOS`  
**PR:** #179  
**Files Modified:**  
- `Egalvanic PZ/Core/UI/Views/EntitySimplePhotoPicker.swift`
- `Egalvanic PZ/Views/Assets/AddAssetView.swift`

---

## Problem

Field technicians using iPads would see a system alert: **"Flash is Disabled — iPad needs to cool down before you can use the flash"** every time they opened the camera to take a photo. They had to tap "OK" each time. This happened because many newer iPads have no physical flash LED, but iOS still shows thermal warnings about flash hardware.

## Investigation (3 Attempts)

### Attempt 1: hasFlash Check (FAILED)
- **Approach:** Check `AVCaptureDevice.default(for: .video)?.hasFlash` before presenting camera
- **Why it failed:** `hasFlash` is a static hardware capability check. iPads support "Retina Flash" (screen-based flash), so `hasFlash` returns `true` even on iPads without a physical flash LED. Also, `hasFlash` doesn't account for thermal state.

### Attempt 2: Multi-Layer Flash Disable (PARTIALLY WORKED)
- **Approach:** 4-layer check: UIKit flash availability, thermal state, `isFlashAvailable`, iPad blanket disable
- **Result:** Camera opened without alert (good!), "Use Photo" worked (good!), but alert appeared AFTER tapping "Use This Photo" during `UIImagePickerController` session teardown
- **Why it partially failed:** `UIImagePickerController` has internal session lifecycle management. When it tears down (dismisses), it re-evaluates flash hardware state and triggers the system alert. We can't intercept this because the alert is presented by SpringBoard (iOS system process), not by our app.

### Attempt 3: Custom AVCaptureSession Camera (DEFINITIVE FIX)
- **Approach:** Replace `UIImagePickerController` entirely on iPad with a custom camera built on `AVCaptureSession`
- **Why this works:** By never creating a `UIImagePickerController` on iPad, there is no internal session teardown to trigger the alert. We control the entire camera lifecycle: session setup, preview, capture, and dismiss.

## Code Changes (Detailed)

### New Components Added to EntitySimplePhotoPicker.swift

1. **`iPadCameraManager`** — `ObservableObject` that manages the AVCaptureSession
   - `setupSession()` — configures session with `.photo` preset, disables flash at device level
   - `startSession()` / `stopSession()` — async on background queue
   - `takePhoto()` — captures with `AVCapturePhotoSettings(flashMode: .off)`
   - Conforms to `AVCapturePhotoCaptureDelegate` for photo callback

2. **`iPadCameraPreview`** — `UIViewRepresentable` wrapping `AVCaptureVideoPreviewLayer`
   - Uses `layerClass` override for automatic frame management
   - `layoutSubviews()` syncs video orientation with device rotation (added in changelog 003)

3. **`iPadCameraView`** — Full SwiftUI camera UI
   - Live preview with overlay controls (X button, shutter button)
   - Photo review screen (Retake / Use Photo)
   - Matches the feel of UIImagePickerController's built-in UI

4. **`EntityCameraView` routing** — Routes iPad → `iPadCameraView`, iPhone → `EntityCameraViewRepresentable`

### Changes to AddAssetView.swift

5. **`CameraViewWrapper` routing** — Same iPad/iPhone routing for the second camera entry point
6. **`CameraViewRepresentable` simplified** — iPhone only, no flash checks needed

### Flash Prevention (2 Independent Levels)

```swift
// Level 1: Device driver level — during session setup
try device.lockForConfiguration()
device.flashMode = .off
device.unlockForConfiguration()

// Level 2: Per-capture level — on every photo
let settings = AVCapturePhotoSettings()
settings.flashMode = .off
```

## Architecture Decision

**Why iPad gets a custom camera but iPhone keeps UIImagePickerController:**
- iPads trigger the system alert; iPhones don't (they have working flash LEDs)
- UIImagePickerController provides a polished camera UX on iPhone (auto-focus animation, HDR, etc.)
- Building a custom camera for iPhone would lose those features for no benefit
- The `if UIDevice.current.userInterfaceIdiom == .pad` check cleanly separates the two paths

## Camera Flow Coverage

Both camera entry points in the app are covered:
1. `EntityCameraView` → used by `EntityFancyPhotoPicker` → used on Asset Detail screens
2. `CameraViewWrapper` → used by `AddAssetView`, `OCPPhotoWalkthroughView`, `PhotoWalkthroughView`, `QuickCountView`

## Key Concepts for Manager Explanation

1. **System alert vs app alert** — The "Flash is Disabled" alert comes from iOS itself (SpringBoard), not from our app. We cannot suppress, intercept, or dismiss it programmatically.
2. **UIImagePickerController is a black box** — Apple controls its internal behavior. Even with `cameraFlashMode = .off`, its session teardown re-evaluates flash state.
3. **AVCaptureSession gives full control** — We manage every aspect of the camera lifecycle, so no hidden flash evaluation happens.
4. **This is iPad-only** — iPhones are unaffected and continue using the standard Apple camera UI.
