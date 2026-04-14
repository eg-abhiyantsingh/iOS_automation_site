# 003 — iPad Camera Landscape Rotation Fix

**Date:** 2026-04-13  
**Time:** ~afternoon session  
**Branch:** `fix/disable-flash-on-ipads-without-flash`  
**Repo:** `Egalvanic/eg-pz-mobile-iOS`  
**Commits:**  
- `50795c7` — Preview + photo orientation rotation  
- `f482498` — Init timing fix for direct-landscape-open  
**File Modified:** `Egalvanic PZ/Core/UI/Views/EntitySimplePhotoPicker.swift` (787 → 853 lines)

---

## Problem

After implementing the custom AVCaptureSession camera to fix the "Flash is Disabled" alert on iPad (see changelog 002), the camera worked correctly in **portrait** mode but did NOT rotate when the iPad was held in **landscape** orientation. The camera preview appeared sideways, and captured photos would have incorrect orientation.

## Root Cause (Why This Happens)

### How iPad camera hardware works

The camera sensor on every iPad is **physically soldered onto the logic board in landscape-right orientation** (home button on the right side). This means:

- The raw camera feed is ALWAYS in landscape-right
- When you hold the iPad in portrait, iOS must rotate the feed 90 degrees clockwise
- When you hold it in landscape-left, iOS must rotate the feed 180 degrees

### Why UIImagePickerController didn't have this problem

Apple's `UIImagePickerController` handles all of this internally. It:
1. Detects the current device orientation
2. Rotates the preview layer automatically
3. Embeds correct EXIF orientation in captured photos

### Why our custom camera had this problem

Our `iPadCameraManager` uses `AVCaptureSession` directly. This gives us full control (which is how we disable flash), but it means **we are responsible for rotation**. The original code had:

- **No `videoOrientation` on the preview layer connection** → preview appeared in raw sensor orientation (sideways in portrait, correct only in landscape-right)
- **No `videoOrientation` on the photo output connection** → captured photos had wrong EXIF metadata → appeared rotated when viewed later

## Code Changes (Detailed)

### Change 1: Preview Layer Rotation (PreviewUIView)

**What:** Added `layoutSubviews()` override to the custom `PreviewUIView` class.

**Where:** `EntitySimplePhotoPicker.swift`, inside `iPadCameraPreview.PreviewUIView` class

**Before:**
```swift
class PreviewUIView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}
```

**After:**
```swift
class PreviewUIView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let connection = previewLayer.connection else { return }

        let interfaceOrientation: UIInterfaceOrientation
        if let scene = window?.windowScene {
            interfaceOrientation = scene.interfaceOrientation
        } else {
            interfaceOrientation = .portrait
        }

        if connection.isVideoOrientationSupported {
            switch interfaceOrientation {
            case .portrait:           connection.videoOrientation = .portrait
            case .portraitUpsideDown:  connection.videoOrientation = .portraitUpsideDown
            case .landscapeLeft:      connection.videoOrientation = .landscapeLeft
            case .landscapeRight:     connection.videoOrientation = .landscapeRight
            @unknown default:         connection.videoOrientation = .portrait
            }
        }
    }
}
```

**How it works:**
- `layoutSubviews()` is called by UIKit every time the view's frame/bounds change
- When the iPad rotates, the view's bounds change (width/height swap) → `layoutSubviews()` fires
- We read the current interface orientation from `window?.windowScene?.interfaceOrientation`
- We map it to the equivalent `AVCaptureVideoOrientation` value
- We set it on the preview layer's connection, which tells AVFoundation how to rotate the camera feed

**Why `layoutSubviews()` specifically:**
- It's the perfect hook because it's guaranteed to run after the rotation animation begins
- At that point, `windowScene.interfaceOrientation` already reflects the new orientation
- It also runs on initial layout, so the first frame is correct too

### Change 2: Photo Capture Orientation (iPadCameraManager)

**What:** Added `currentVideoOrientation()` helper method and updated `takePhoto()` to set the orientation on the photo output connection before each capture.

**Where:** `EntitySimplePhotoPicker.swift`, inside `iPadCameraManager` class

**Before:**
```swift
func takePhoto() {
    let settings = AVCapturePhotoSettings()
    settings.flashMode = .off
    photoOutput.capturePhoto(with: settings, delegate: self)
}
```

**After:**
```swift
func takePhoto() {
    let settings = AVCapturePhotoSettings()
    settings.flashMode = .off

    // Set orientation on the connection before capture
    if let connection = photoOutput.connection(with: .video),
       connection.isVideoOrientationSupported {
        connection.videoOrientation = currentVideoOrientation()
    }

    photoOutput.capturePhoto(with: settings, delegate: self)
}

private func currentVideoOrientation() -> AVCaptureVideoOrientation {
    guard let windowScene = UIApplication.shared.connectedScenes
        .compactMap({ $0 as? UIWindowScene })
        .first else {
        return .portrait
    }
    switch windowScene.interfaceOrientation {
    case .portrait:            return .portrait
    case .portraitUpsideDown:   return .portraitUpsideDown
    case .landscapeLeft:        return .landscapeLeft
    case .landscapeRight:       return .landscapeRight
    @unknown default:           return .portrait
    }
}
```

**How it works:**
- Before each photo capture, we read the current device orientation
- We set `connection.videoOrientation` on the photo output connection
- AVFoundation uses this to write the correct EXIF orientation tag in the JPEG data
- When the photo is later displayed (in the app, or exported), it appears in the correct orientation

**Why we set it per-capture (not once in setupSession):**
- The user might rotate the iPad AFTER opening the camera
- Each capture should match the orientation at the moment the shutter is pressed
- Setting it once would only work for the initial orientation

### Change 3: Init Timing Fix (Commit `f482498`)

**What:** Moved `setupSession()` from `onAppear` into `iPadCameraManager.init()`.

**Problem this fixes:** If the camera opened while iPad was already in landscape, the preview could show in the wrong orientation. This happened because:
1. SwiftUI creates the preview view and calls `layoutSubviews()` BEFORE `onAppear` fires
2. `setupSession()` was in `onAppear`, so it hadn't run yet
3. Without session configuration, the preview layer has no connection
4. `layoutSubviews()` guard returns early → orientation never set for initial display

**Before:**
```swift
class iPadCameraManager: ... {
    func setupSession() { ... }  // public, called from onAppear
}

// In iPadCameraView:
.onAppear {
    cameraManager.setupSession()   // too late!
    cameraManager.startSession()
}
```

**After:**
```swift
class iPadCameraManager: ... {
    override init() {
        super.init()
        setupSession()  // configure immediately
    }
    private func setupSession() { ... }  // private, called from init only
}

// In iPadCameraView:
.onAppear {
    cameraManager.startSession()   // only start running, session already configured
}
```

**Why this is safe:**
- `setupSession()` only calls `session.beginConfiguration/commitConfiguration`, `canAddInput/Output` — all synchronous, lightweight
- `startRunning()` is what's heavy (initializes hardware pipeline) — still on background queue in `onAppear`
- Camera permission is already granted (the app uses UIImagePickerController on other screens first)
- `setupSession()` is idempotent — `canAddInput/Output` returns false if already added

## Key Concepts for Manager Explanation

1. **Camera sensor is fixed** — The physical camera doesn't rotate. All rotation is done in software by iOS.

2. **Three separate fixes needed:**
   - **Preview rotation** (`layoutSubviews`) — what the user sees live on screen while aiming the camera
   - **Photo rotation** (`takePhoto`) — the EXIF metadata embedded in the captured JPEG file
   - **Init timing** (`init` vs `onAppear`) — ensures the connection exists for the very first frame

3. **`layoutSubviews()` pattern** — This is a standard iOS pattern for responding to layout changes. It's called automatically by the system, we don't call it ourselves.

4. **`windowScene.interfaceOrientation`** — This is the modern iOS 13+ way to detect device orientation. It uses the window scene (not the deprecated `UIDevice.orientation`).

5. **`setupSession()` vs `startSession()`** — Configuration (adding inputs/outputs to the pipeline) is cheap. Starting (activating the hardware) is expensive. By separating them, we configure early and start late.

6. **No changes to AddAssetView.swift** — The `iPadCameraView` struct is defined in `EntitySimplePhotoPicker.swift` and reused by both camera entry points. So this single fix covers all camera flows in the app.

## Files on Branch After This Commit

| File | Lines | Status |
|------|-------|--------|
| `EntitySimplePhotoPicker.swift` | 853 | Modified (rotation + init timing fix) |
| `AddAssetView.swift` | 1081 | Unchanged (uses shared iPadCameraView) |

## Testing Checklist

- [ ] Open camera on iPad in **portrait** → preview should show correctly
- [ ] Open camera on iPad **already in landscape** → preview should immediately show in landscape
- [ ] Rotate iPad to **landscape** while camera is open → preview should rotate smoothly
- [ ] Rotate iPad to **landscape-left** (home button on LEFT) → preview should be correct
- [ ] Take photo in **portrait** → photo should appear upright in review screen
- [ ] Take photo in **landscape** → photo should appear upright in review screen
- [ ] Tap "Retake" → camera preview should resume with correct orientation
- [ ] Tap "Use Photo" → photo should appear correctly in the asset/entity
- [ ] **No "Flash is Disabled" alert** at any point (original fix still intact)
