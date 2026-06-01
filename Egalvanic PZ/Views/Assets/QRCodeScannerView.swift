//
//  QRCodeScannerView.swift
//  Egalvanic PZ
//
//  QR Code scanner for asset identification
//

import SwiftUI
import AVFoundation

/// A view that presents a QR code scanner using the device camera
struct QRCodeScannerView: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var scannedCode: String
    @State private var isScanning = true
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var hasCameraPermission = false

    var onScanComplete: ((String) -> Void)?

    var body: some View {
        NavigationStack {
            ZStack {
                // Camera preview
                if hasCameraPermission {
                    QRCodeCameraView(
                        scannedCode: $scannedCode,
                        isScanning: $isScanning,
                        onCodeScanned: { code in
                            handleScannedCode(code)
                        }
                    )
                } else {
                    VStack(spacing: 20) {
                        Image(systemName: "camera.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)

                        Text(AppStrings.AssetsExtra.cameraAccessRequired)
                            .font(.title2)
                            .fontWeight(.semibold)

                        Text(AppStrings.AssetsExtra.enableCameraForQR)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)

                        Button(AppStrings.AssetsExtra.openSettings) {
                            if let url = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(url)
                            }
                        }
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                }

                // Scanning frame overlay
                if hasCameraPermission && isScanning {
                    VStack {
                        Spacer()

                        // Scanning frame
                        ZStack {
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(Color.green, lineWidth: 3)
                                .frame(width: 250, height: 250)

                            // Corner brackets
                            VStack {
                                HStack {
                                    ScannerCorner()
                                    Spacer()
                                    ScannerCorner()
                                        .rotationEffect(.degrees(90))
                                }
                                Spacer()
                                HStack {
                                    ScannerCorner()
                                        .rotationEffect(.degrees(-90))
                                    Spacer()
                                    ScannerCorner()
                                        .rotationEffect(.degrees(180))
                                }
                            }
                            .frame(width: 250, height: 250)
                        }

                        Spacer()

                        // Instructions
                        VStack(spacing: 8) {
                            Text(AppStrings.AssetsExtra.alignQRCode)
                                .font(.headline)
                                .foregroundColor(.white)

                            Text(AppStrings.AssetsExtra.cameraWillScan)
                                .font(.caption)
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .padding()
                        .background(Color.black.opacity(0.7))
                        .cornerRadius(10)
                        .padding(.bottom, 40)
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.scanQRCode)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                    .foregroundColor(.white)
                }
            }
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.8), for: .navigationBar)
        }
        .onAppear {
            checkCameraPermission()
        }
        .alert(AppStrings.AssetsExtra.scanError, isPresented: $showError) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(errorMessage)
        }
    }

    private func checkCameraPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            hasCameraPermission = true
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    hasCameraPermission = granted
                }
            }
        default:
            hasCameraPermission = false
        }
    }

    private func handleScannedCode(_ code: String) {
        // Guard against rapid duplicate scans: a second metadata frame can
        // arrive before SwiftUI propagates isScanning = false to the
        // Coordinator's binding. Without this, dismiss() fires twice and
        // can cascade to the presenting screen.
        guard isScanning else { return }

        // Stop scanning
        isScanning = false
        
        // Haptic feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
        
        // Update the binding
        scannedCode = code
        
        // Call completion handler if provided
        dismiss()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            onScanComplete?(code)
        }
    }
}

/// Corner decoration for the scanner frame
struct ScannerCorner: View {
    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.green)
                .frame(width: 4, height: 30)

            Rectangle()
                .fill(Color.green)
                .frame(width: 30, height: 4)
        }
    }
}

/// Custom UIView that uses AVCaptureVideoPreviewLayer as its backing layer.
/// Uses AVCaptureDevice.RotationCoordinator (iOS 17+) to track device rotation
/// via KVO — this works in ALL presentation contexts including sheets, where
/// layoutSubviews() does not fire on iPad rotation because the sheet frame
/// doesn't change significantly.
class QRPreviewView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

    /// Reference to the metadata output so we can sync its rotation too.
    /// QR detection coordinates must match the video orientation, otherwise
    /// codes may not be detected reliably in landscape.
    var metadataOutput: AVCaptureMetadataOutput?

    /// Tracks physical device rotation relative to the camera sensor.
    /// Must be held as a strong reference for KVO observation to work.
    private var rotationCoordinator: AVCaptureDevice.RotationCoordinator?
    private var rotationObservation: NSKeyValueObservation?

    /// Sets up rotation tracking using RotationCoordinator.
    /// Call this after assigning the session to the preview layer.
    func configureRotationTracking(for device: AVCaptureDevice) {
        rotationCoordinator = AVCaptureDevice.RotationCoordinator(
            device: device,
            previewLayer: previewLayer
        )

        guard let coordinator = rotationCoordinator else { return }

        // Apply the initial rotation angle
        applyRotationAngle(coordinator.videoRotationAngleForHorizonLevelPreview)

        // Observe rotation changes — fires on physical device rotation
        // regardless of whether the view resizes (critical for sheets on iPad)
        rotationObservation = coordinator.observe(
            \.videoRotationAngleForHorizonLevelPreview,
            options: .new
        ) { [weak self] coord, _ in
            DispatchQueue.main.async {
                self?.applyRotationAngle(coord.videoRotationAngleForHorizonLevelPreview)
            }
        }
    }

    private func applyRotationAngle(_ angle: CGFloat) {
        // Sync the preview layer connection — rotates the camera feed on screen
        if let previewConnection = previewLayer.connection,
           previewConnection.isVideoRotationAngleSupported(angle) {
            previewConnection.videoRotationAngle = angle
        }

        // Sync the metadata output connection — aligns QR detection coordinates
        // with the rotated preview, so QR codes are detected correctly in landscape
        if let metadataConnection = metadataOutput?.connection(with: .video),
           metadataConnection.isVideoRotationAngleSupported(angle) {
            metadataConnection.videoRotationAngle = angle
        }
    }

    deinit {
        rotationObservation?.invalidate()
    }
}

/// Camera view that handles QR code detection.
/// Uses QRPreviewView for rotation-aware camera preview on iPad.
struct QRCodeCameraView: UIViewRepresentable {
    @Binding var scannedCode: String
    @Binding var isScanning: Bool
    var onCodeScanned: (String) -> Void

    func makeUIView(context: Context) -> QRPreviewView {
        let view = QRPreviewView()
        view.backgroundColor = .black

        let captureSession = AVCaptureSession()
        context.coordinator.captureSession = captureSession

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            return view
        }

        let videoInput: AVCaptureDeviceInput

        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            return view
        }

        if captureSession.canAddInput(videoInput) {
            captureSession.addInput(videoInput)
        } else {
            return view
        }

        let metadataOutput = AVCaptureMetadataOutput()

        if captureSession.canAddOutput(metadataOutput) {
            captureSession.addOutput(metadataOutput)

            metadataOutput.setMetadataObjectsDelegate(context.coordinator, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            return view
        }

        // Assign the configured session to the preview layer (via layerClass).
        // The session must be assigned before configureRotationTracking() so the
        // preview layer's connection exists for the initial rotation angle.
        view.previewLayer.session = captureSession
        view.previewLayer.videoGravity = .resizeAspectFill

        // Give QRPreviewView a reference to the metadata output so it can
        // sync the metadata connection's rotation on device orientation changes
        view.metadataOutput = metadataOutput

        // Set up RotationCoordinator to track device rotation via KVO.
        // This works in sheets on iPad where layoutSubviews() doesn't fire on rotation.
        view.configureRotationTracking(for: videoCaptureDevice)

        DispatchQueue.global(qos: .userInitiated).async {
            captureSession.startRunning()
        }

        return view
    }

    func updateUIView(_ uiView: QRPreviewView, context: Context) {
        // Stop/start scanning based on state
        if !isScanning {
            context.coordinator.captureSession?.stopRunning()
        } else if context.coordinator.captureSession?.isRunning == false {
            DispatchQueue.global(qos: .userInitiated).async {
                context.coordinator.captureSession?.startRunning()
            }
        }
    }

    static func dismantleUIView(_ uiView: QRPreviewView, coordinator: Coordinator) {
        coordinator.captureSession?.stopRunning()
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(scannedCode: $scannedCode, isScanning: $isScanning, onCodeScanned: onCodeScanned)
    }

    class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        @Binding var scannedCode: String
        @Binding var isScanning: Bool
        var onCodeScanned: (String) -> Void
        var captureSession: AVCaptureSession?

        init(scannedCode: Binding<String>, isScanning: Binding<Bool>, onCodeScanned: @escaping (String) -> Void) {
            self._scannedCode = scannedCode
            self._isScanning = isScanning
            self.onCodeScanned = onCodeScanned
        }

        func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
            guard isScanning else { return }

            if let metadataObject = metadataObjects.first {
                guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
                guard let stringValue = readableObject.stringValue else { return }

                if isScanning {
                    AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
                    onCodeScanned(stringValue)
                }
            }
        }
    }
}

#Preview {
    QRCodeScannerView(scannedCode: .constant(""))
}
