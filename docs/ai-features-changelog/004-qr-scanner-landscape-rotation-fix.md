# 004 — QR Code Scanner Landscape Rotation Fix

**Date:** 2026-04-13  
**Branch:** `fix/disable-flash-on-ipads-without-flash`  
**Repo:** `Egalvanic/eg-pz-mobile-iOS`  
**Commit:** `5ef34e4`  
**File Modified:** `Egalvanic PZ/Views/Assets/QRCodeScannerView.swift` (300 → 354 lines)

---

## Problem

The QR code scanner worked correctly in portrait mode but displayed a sideways camera preview when the iPad was in landscape mode. Additionally, QR code detection could be unreliable in landscape because the metadata output's coordinate system didn't match the rotated screen.

## Root Cause

Identical root cause to the photo camera (see changelog 003): the camera sensor is physically mounted in landscape-right orientation, and `AVCaptureVideoPreviewLayer` requires explicit `videoOrientation` mapping to display correctly in other orientations.

The QR scanner had **additional** problems beyond the photo camera:
1. Used a plain `UIView` with a sublayer — no `layoutSubviews()` hook for rotation
2. Manual `previewLayer.frame = view.layer.bounds` in `updateUIView` — frame was `.zero` at creation, and async dispatch caused lag during rotation
3. `AVCaptureMetadataOutput` connection was never oriented — QR detection coordinates didn't match the screen in landscape

## What is `AVCaptureMetadataOutput` and Why Does It Need Orientation?

When iOS detects a QR code in the camera feed, it returns the code's position as coordinates (e.g., "QR code is at rectangle (0.3, 0.2, 0.4, 0.4)"). These coordinates are relative to the **video orientation**, not the screen.

If the preview shows landscape but the metadata thinks it's portrait, the detected QR region is rotated 90° from where the code actually appears on screen. This means:
- QR codes near the edges of the frame might not be detected
- The scanning frame overlay (the green square) doesn't match the actual detection region

Setting `metadataConnection.videoOrientation` aligns the detection coordinate system with what the user sees.

## Code Changes (Detailed)

### New: `QRPreviewView` Class

A custom `UIView` subclass that uses `AVCaptureVideoPreviewLayer` as its backing layer, with automatic rotation handling.

```swift
class QRPreviewView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    var metadataOutput: AVCaptureMetadataOutput?

    override func layoutSubviews() {
        super.layoutSubviews()
        let videoOrientation = Self.mapOrientation(windowScene.interfaceOrientation)
        
        // Sync BOTH connections:
        previewConnection.videoOrientation = videoOrientation   // camera feed rotation
        metadataConnection.videoOrientation = videoOrientation  // QR detection coordinates
    }
}
```

**Why a separate class from PreviewUIView (iPad camera)?**
- `PreviewUIView` only manages the preview layer connection
- `QRPreviewView` manages TWO connections: preview AND metadata
- They have different responsibilities — sharing a base class would be premature abstraction

### Modified: `QRCodeCameraView` (UIViewRepresentable)

**Before:**
```swift
func makeUIView(context: Context) -> UIView {
    let view = UIView(frame: .zero)
    // ...session setup...
    let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
    previewLayer.frame = view.layer.bounds  // .zero at this point!
    view.layer.addSublayer(previewLayer)
    context.coordinator.previewLayer = previewLayer
    return view
}

func updateUIView(_ uiView: UIView, context: Context) {
    // Manual frame update via async dispatch
    if let previewLayer = context.coordinator.previewLayer {
        DispatchQueue.main.async {
            previewLayer.frame = uiView.bounds
        }
    }
    // ...
}
```

**After:**
```swift
func makeUIView(context: Context) -> QRPreviewView {
    let view = QRPreviewView()
    // ...session setup...
    view.previewLayer.session = captureSession     // layerClass handles sizing
    view.previewLayer.videoGravity = .resizeAspectFill
    view.metadataOutput = metadataOutput           // for orientation sync
    return view
}

func updateUIView(_ uiView: QRPreviewView, context: Context) {
    // Only scan state management — no manual frame handling needed
}
```

### What Was Removed

| Removed | Why |
|---------|-----|
| `previewLayer.frame = view.layer.bounds` in makeUIView | Was `.zero` at creation time — useless line |
| `DispatchQueue.main.async { previewLayer.frame = uiView.bounds }` in updateUIView | `layerClass` override makes the preview layer auto-resize — no manual frame needed |
| `coordinator.previewLayer` property | Preview layer is now managed by `QRPreviewView`, not the coordinator |
| Manual `view.layer.addSublayer(previewLayer)` | `layerClass` override means the preview layer IS the view's layer — no sublayer needed |

## How `layerClass` Override Works

Normal UIView:
```
UIView.layer = CALayer (generic)
   └── AVCaptureVideoPreviewLayer (added as sublayer, needs manual frame sync)
```

With `layerClass` override:
```
QRPreviewView.layer = AVCaptureVideoPreviewLayer (IS the layer)
   └── Frame managed by Auto Layout automatically
```

By making `AVCaptureVideoPreviewLayer` the view's own backing layer, UIKit manages its frame automatically — same as how UIKit manages any view's `layer.frame`. No manual frame code needed.

## Execution Flow (When Scanner Opens in Landscape)

```
1. SwiftUI creates QRCodeCameraView → makeUIView()
2. QRPreviewView created, session configured with inputs/outputs
3. view.previewLayer.session = captureSession → connection established
4. SwiftUI adds view to hierarchy → first layoutSubviews() fires
5. layoutSubviews() reads interface orientation → landscape
6. Sets BOTH connections to .landscapeLeft (or .landscapeRight)
7. startRunning() dispatched to background queue
8. Camera feed appears in correct landscape orientation ✓
9. QR detection coordinates aligned with screen ✓
```

## Key Concepts for Manager Explanation

1. **Same root cause as photo camera** — camera sensor is fixed in landscape-right, software must rotate
2. **QR scanner has an EXTRA orientation requirement** — the QR detection coordinate system must also match the screen orientation, not just the preview
3. **`layerClass` override** — standard iOS pattern that replaces the view's backing layer. Eliminates all manual frame management code
4. **The scanning frame overlay (green square)** — is a SwiftUI overlay that rotates automatically with the device. Only the underlying camera preview and detection coordinates needed fixing

## Files on Branch After This Commit

| File | Lines | Status |
|------|-------|--------|
| `EntitySimplePhotoPicker.swift` | 853 | Modified (earlier — camera rotation) |
| `AddAssetView.swift` | 1081 | Modified (earlier — iPad camera routing) |
| `QRCodeScannerView.swift` | 354 | Modified (this commit — QR scanner rotation) |

## Testing Checklist

- [ ] Open QR scanner in **portrait** → preview should show correctly, QR codes detected
- [ ] Open QR scanner **already in landscape** → preview should immediately show in landscape
- [ ] Rotate iPad to **landscape** while scanner is open → preview should rotate smoothly
- [ ] Scan a QR code in **portrait** → should detect and dismiss correctly
- [ ] Scan a QR code in **landscape** → should detect and dismiss correctly
- [ ] Scan a QR code near the **edge of the frame** in landscape → should still detect (metadata orientation fix)
- [ ] **Green scanning frame** should stay centered in both orientations
- [ ] Cancel button should work in both orientations
