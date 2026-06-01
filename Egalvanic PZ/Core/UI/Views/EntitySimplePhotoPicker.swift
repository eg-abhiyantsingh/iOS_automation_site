//
//  EntitySimplePhotoPicker.swift
//  SwiftDataTutorial
//
//  Generic photo picker view for any entity conforming to EntityWithPhotos
//

import SwiftUI
import PhotosUI
import SwiftData
import AVFoundation

struct EntitySimplePhotoPicker<Entity: EntityWithPhotos>: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    
    let entity: Entity
    let photoType: String
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool
    
    // Callbacks for photo operations
    var onPhotoAdded: ((Photo) -> Void)?
    var onPhotoDeleted: ((Photo) -> Void)?
    var onUploadRequested: ((Photo) async throws -> Void)?
    
    // Optional customization
    var sectionTitle: String = "Photos"
    var sectionIcon: String = "photo.on.rectangle"
    var showUploadIndicators: Bool = true
    var maxPhotosToDisplay: Int? = nil
    // ZP-2230: every capture site sits inside an SLD context. The
    // entity's own ``.sld`` relationship is usually set, but for
    // placeholder / draft entities (creation flows) it can be nil —
    // callers thread the active SLD here so the stamp's location
    // breadcrumb always names the SLD.
    var overrideSLD: SLDV2? = nil
    
    // Local UI state
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var showingCamera = false
    @State private var photoToDelete: Photo? = nil
    @State private var showingDeleteConfirmation = false
    @State private var selectedPhoto: Photo? = nil
    @State private var capturedImage: UIImage? = nil
    // ZP-2230: surfaces capture errors (permission denied, save
    // failed) so the user knows the photo did NOT save anywhere.
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false
    
    private var visiblePhotos: [Photo] {
        let filtered = displayedPhotos.filter { !$0.is_deleted }
        if let max = maxPhotosToDisplay {
            return Array(filtered.prefix(max))
        }
        return filtered
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header
            HStack {
                SectionHeader(title: sectionTitle, systemImage: sectionIcon)
                
                Spacer()
                
                if !visiblePhotos.isEmpty {
                    Text("\(visiblePhotos.count)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(.systemGray5))
                        .cornerRadius(6)
                }
            }
            
            // Photo gallery
            if !visiblePhotos.isEmpty && !isSaving {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(visiblePhotos) { photo in
                            PhotoThumbnailView(
                                photo: photo,
                                showUploadIndicator: showUploadIndicators,
                                onTap: { selectedPhoto = photo },
                                onDelete: {
                                    photoToDelete = photo
                                    showingDeleteConfirmation = true
                                }
                            )
                        }
                    }
                    .padding(.horizontal)
                }
                .frame(height: 120)
            }
            
            // Add photo buttons
            HStack(spacing: 16) {
                // Gallery picker
                PhotosPicker(
                    selection: $selectedItem,
                    matching: .images
                ) {
                    Label(AppStrings.Photos.gallery, systemImage: "photo.on.rectangle")
                        .font(.subheadline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.blue.opacity(0.1))
                        .foregroundColor(.blue)
                        .cornerRadius(8)
                }
                .buttonStyle(.plain)
                .disabled(isSaving)
                
                // Camera button
                Button {
                    // ZP-2230: require Photos-library write access
                    // before opening the camera. Captured photos are
                    // saved both in-app and to the user's library;
                    // we refuse to capture if we can't durably stash
                    // the bytes off-app.
                    Task {
                        let granted = await PhotoCaptureService.ensureLibraryWriteAccess()
                        if granted {
                            showingCamera = true
                        } else {
                            showPermissionPrompt = true
                        }
                    }
                } label: {
                    Label(AppStrings.Photos.camera, systemImage: "camera.fill")
                        .font(.subheadline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.green.opacity(0.1))
                        .foregroundColor(.green)
                        .cornerRadius(8)
                }
                .buttonStyle(.plain)
                .disabled(isSaving || !UIImagePickerController.isSourceTypeAvailable(.camera))
            }
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(.vertical, 8)
        }
        .confirmationDialog(
            AppStrings.Photos.deleteThisPhoto,
            isPresented: $showingDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.Common.delete, role: .destructive) {
                if let photo = photoToDelete {
                    handlePhotoDelete(photo)
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        }
        .fullScreenCover(item: $selectedPhoto) { photo in
            FullImageView(selected: photo, allPhotos: displayedPhotos)
        }
        .background(
            NavigationLink(
                destination: EntityCameraView { image in
                    capturedImage = image
                    showingCamera = false
                },
                isActive: $showingCamera
            ) {
                EmptyView()
            }
            .hidden()
        )
        .onChange(of: capturedImage) { _, newImage in
            if let image = newImage {
                Task { await handleCapturedImage(image) }
                capturedImage = nil
            }
        }
        .onChange(of: selectedItem) { _, newItem in
            Task { await handleSelectedItem(newItem) }
        }
        // ZP-2230: surface capture failures + permission denials.
        .alert("Could not save photo", isPresented: $showCaptureError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(captureErrorMessage)
        }
        .alert("Photos library access required", isPresented: $showPermissionPrompt) {
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Egalvanic PZ saves a stamped copy of every captured photo to your Photos library as an audit-recovery copy. Enable “Add Photos Only” access in Settings → Privacy → Photos → Egalvanic PZ to use the camera.")
        }
    }
    
    // MARK: - Photo Handling

    private func handleCapturedImage(_ image: UIImage) async {
        await persistCapture(image, fromGallery: false)
    }

    private func handleSelectedItem(_ item: PhotosPickerItem?) async {
        guard let item = item,
              let data = try? await item.loadTransferable(type: Data.self),
              let uiImage = UIImage(data: data) else { return }
        await persistCapture(uiImage, fromGallery: true)
    }

    /// ZP-2230: unified capture path. Resize, stamp, write to Photos
    /// library + Documents/photos atomically. The bookkeeping
    /// (``modelContext.insert``, ``entity.associatePhoto``) stays in
    /// the parent view via ``onPhotoAdded``.
    ///
    /// ``fromGallery`` skips the audit-copy save back to the Photos
    /// library because gallery-imported bytes already exist in the
    /// user's library — re-saving a stamped duplicate would force
    /// an addOnly permission grant the user shouldn't need.
    private func persistCapture(_ image: UIImage, fromGallery: Bool) async {
        do {
            var ctx = PhotoStampContext.from(entity: entity, photoType: photoType)
            if ctx.sld == nil, let override = overrideSLD {
                ctx.sld = override
            }
            let photo = try await PhotoCaptureService.captureAndPersistPending(
                image: image,
                context: ctx,
                saveAuditCopy: !fromGallery
            )
            onPhotoAdded?(photo)
        } catch {
            AppLogger.log(.error,
                "[PhotoCapture] capture failed: \(error.localizedDescription)",
                category: .photo)
            captureErrorMessage = error.localizedDescription
            showCaptureError = true
        }
    }

    private func handlePhotoDelete(_ photo: Photo) {
//        photo.is_deleted = true
//        entity.disassociatePhoto(photo)
        onPhotoDeleted?(photo)
    }
    
}

// MARK: - Photo Thumbnail View
struct PhotoThumbnailView: View {
    let photo: Photo
    let showUploadIndicator: Bool
    let onTap: () -> Void
    let onDelete: () -> Void

    @State private var showCaptionEditor = false
    @State private var editingCaption = ""
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Photo image
            Group {
                // Priority 1: Check local file first for instant loading
                if let localURL = photo.localFileURL,
                   FileManager.default.fileExists(atPath: localURL.path),
                   let uiImage = UIImage(contentsOfFile: localURL.path) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                }
                // Priority 2: Use presigned URL for remote photos
                else if photo.url != nil {
                    PresignedPhotoImage(
                        photo: photo,
                        content: { image in
                            image
                                .resizable()
                                .scaledToFill()
                        },
                        placeholder: {
                            ZStack {
                                Color.gray.opacity(0.1)
                                ProgressView()
                                    .scaleEffect(0.7)
                            }
                        },
                        onFailure: {
                            ZStack {
                                Color.gray.opacity(0.1)
                                Image(systemName: "photo")
                                    .foregroundColor(.gray)
                            }
                        }
                    )
                }
                // Priority 3: Show placeholder if neither local nor remote available
                else {
                    ZStack {
                        Color.gray.opacity(0.1)
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                    }
                }
            }
            .frame(width: 100, height: 100)
            .clipped()
            .cornerRadius(8)
            .onTapGesture(perform: onTap)
            .onLongPressGesture {
                editingCaption = photo.caption ?? ""
                showCaptionEditor = true
            }
            .overlay(alignment: .bottomLeading) {
                // Caption badge indicator (bottom-left)
                if let caption = photo.caption, !caption.isEmpty {
                    PhotoCaptionBadge()
                        .offset(x: 5, y: -5)
                }
            }

            // Upload indicator
            if showUploadIndicator && photo.upload_needed {
                Image(systemName: "icloud.and.arrow.up")
                    .font(.caption2)
                    .padding(4)
                    .background(Color.orange)
                    .foregroundColor(.white)
                    .clipShape(Circle())
                    .offset(x: -5, y: 75)
            }

            // 3-dot menu button
            Menu {
                // Add/Edit Caption option (dynamic text)
                Button(action: {
                    editingCaption = photo.caption ?? ""
                    showCaptionEditor = true
                }) {
                    Label(
                        photo.caption?.isEmpty ?? true ? AppStrings.Photos.addCaption : AppStrings.Photos.editCaption,
                        systemImage: "text.bubble"
                    )
                }

                // Delete option
                Button(role: .destructive, action: onDelete) {
                    Label(AppStrings.Common.delete, systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle.fill")
                    .font(.title3)
                    .foregroundColor(.white)
                    .background(Color.gray.opacity(0.8))
                    .clipShape(Circle())
                    .shadow(radius: 2)
            }
            .padding(4)
        }
        .sheet(isPresented: $showCaptionEditor) {
            CaptionEditingView(
                caption: $editingCaption,
                onSave: {
                    PhotoService.updatePhotoCaption(
                        photo,
                        caption: editingCaption,
                        modelContext: modelContext
                    ) { success, message in
                        if success {
                            AppLogger.log(.info, "Caption update: \(message ?? "Success")", category: .photo)
                        } else {
                            AppLogger.log(.error, "Caption update failed: \(message ?? "Unknown error")", category: .photo)
                        }
                    }
                },
                onCancel: {
                    // Revert changes
                    editingCaption = photo.caption ?? ""
                }
            )
        }
    }
}

// MARK: - Camera View
//
// On iPad: Uses a custom AVCaptureSession camera that NEVER initializes flash hardware.
// This prevents the iOS system "Flash is Disabled" alert entirely, because
// UIImagePickerController's internal session teardown triggers the alert even when
// cameraFlashMode = .off. The custom camera bypasses UIImagePickerController completely.
//
// On iPhone: Uses UIImagePickerController normally (flash works fine on iPhones).

struct EntityCameraView: View {
    let onImageCaptured: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        if UIDevice.current.userInterfaceIdiom == .pad {
            IPadCameraView(
                onImageCaptured: { image in
                    onImageCaptured(image)
                    dismiss()
                },
                onDismiss: {
                    dismiss()
                }
            )
            .ignoresSafeArea()
            .toolbar(.hidden, for: .navigationBar)
        } else {
            EntityCameraViewRepresentable(
                onImageCaptured: { image in
                    onImageCaptured(image)
                    dismiss()
                },
                onDismiss: {
                    dismiss()
                }
            )
            .ignoresSafeArea()
            .toolbar(.hidden, for: .navigationBar)
        }
    }
}

// MARK: - iPad Custom Camera (AVCaptureSession)
//
// Uses AVCaptureSession + AVCapturePhotoOutput directly instead of UIImagePickerController.
// This avoids the iOS system "Flash is Disabled" alert that UIImagePickerController triggers
// on iPads during session teardown. Supports zoom, camera switch, and flash (hardware + screen).

class IPadCameraManager: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {
    let session = AVCaptureSession()
    private let photoOutput = AVCapturePhotoOutput()
    @Published var capturedImage: UIImage?
    @Published var isSessionRunning = false
    @Published var isCapturing = false
    @Published var currentCameraPosition: AVCaptureDevice.Position = .back
    @Published var flashMode: AVCaptureDevice.FlashMode = .off
    @Published var currentZoomFactor: CGFloat = 1.0
    @Published var showScreenFlash = false
    /// True when the front camera is in its wider-FOV format (the iOS-style "0.5x" view).
    @Published var isFrontCameraExpanded = false
    private(set) var currentDevice: AVCaptureDevice?

    /// The default (narrower-FOV) front-camera capture format, cached on switch.
    private var frontCameraDefaultFormat: AVCaptureDevice.Format?
    /// The widest-FOV front-camera format, cached on switch.
    private var frontCameraWidestFormat: AVCaptureDevice.Format?

    /// Set by the preview view so capture can read the hardware-accurate rotation angle
    /// (the preview already uses this — capture must use it too, or images come out flipped
    /// in landscape on devices like iPad whose sensor orientation differs from iPhone).
    ///
    /// Ownership: the strong reference lives on `IPadPreviewUIView.rotationCoordinator`,
    /// which is recreated whenever `configureRotationTracking` runs against a new device.
    /// We keep this `weak` so a preview view deallocation/recreation cycle drops the
    /// manager's pointer cleanly instead of leaving a stale coordinator observing a
    /// freed preview layer. `takePhoto` already falls back to `CameraOrientationHelper`
    /// when this is nil, so a transient nil between view recreation and re-assignment
    /// is safe.
    ///
    /// Threading contract: written from `IPadPreviewUIView.configureRotationTracking`
    /// (main thread — UIViewRepresentable lifecycle) and read from `takePhoto`
    /// (main thread — SwiftUI button action). The capture-delegate callback runs on
    /// AVFoundation's session queue but does not touch this property. Single-thread
    /// (main) access; no synchronization needed.
    weak var rotationCoordinator: AVCaptureDevice.RotationCoordinator?

    var hasHardwareFlash: Bool { currentDevice?.hasFlash ?? false }

    var isFlashAvailable: Bool {
        if currentCameraPosition == .front { return true }
        return hasHardwareFlash
    }

    /// True when the rear ultra-wide lens exists on this iPad. An iPad's lens set is
    /// fixed at manufacture, so this is `static let` — resolved once per process the
    /// first time any IPadCameraManager is created. Drives whether the 0.5x preset
    /// chip is shown.
    private static let hasRearUltraWide: Bool = AVCaptureDevice.default(
        .builtInUltraWideCamera, for: .video, position: .back
    ) != nil

    /// Rear wide-angle max zoom, resolved once per process so `maxZoomFactor` doesn't
    /// depend on whichever lens is currently mounted. Falls back to 5x if the device
    /// is unreadable (should never happen — every iPad has a wide rear camera).
    private static let rearWideMaxZoom: CGFloat = {
        guard let device = AVCaptureDevice.default(
            .builtInWideAngleCamera, for: .video, position: .back
        ) else { return 5.0 }
        return CGFloat(device.maxAvailableVideoZoomFactor)
    }()

    /// True when the currently active rear input is the ultra-wide lens. Drives the
    /// display↔device zoom mapping: when ultra-wide is active, display 0.5x = device 1.0x
    /// (ultra-wide's own 1x is the widest FOV the hardware exposes, surfaced as 0.5x in
    /// UI to match iOS Camera). The lens is swapped in/out of the session as the user
    /// crosses the 1.0x display boundary.
    private var isUsingUltraWide: Bool = false

    /// Display-space minimum (what the user can pinch/cycle down to).
    /// 0.5 on rear cameras with an ultra-wide; otherwise the current device's own min.
    var minZoomFactor: CGFloat {
        if currentCameraPosition == .back && Self.hasRearUltraWide { return 0.5 }
        return CGFloat(currentDevice?.minAvailableVideoZoomFactor ?? 1.0)
    }

    /// Display-space maximum, capped at 10x. For the rear path we always use the wide
    /// lens's max (cached) — when ultra-wide is mounted, the lens-swap at the 1.0x
    /// boundary hands off to wide before zooming further out.
    var maxZoomFactor: CGFloat {
        if currentCameraPosition == .back { return min(Self.rearWideMaxZoom, 10.0) }
        return min(CGFloat(currentDevice?.maxAvailableVideoZoomFactor ?? 5.0), 10.0)
    }

    override init() {
        super.init()
        setupSession(position: .back)
    }

    private func setupSession(position: AVCaptureDevice.Position) {
        session.beginConfiguration()
        session.sessionPreset = .photo

        for input in session.inputs { session.removeInput(input) }

        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
              let input = try? AVCaptureDeviceInput(device: device) else {
            session.commitConfiguration()
            return
        }

        currentDevice = device
        currentCameraPosition = position

        if session.canAddInput(input) { session.addInput(input) }
        if !session.outputs.contains(photoOutput), session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
        }

        session.commitConfiguration()
    }

    func startSession() {
        guard !session.isRunning else { return }
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.session.startRunning()
            DispatchQueue.main.async {
                self?.isSessionRunning = self?.session.isRunning ?? false
            }
        }
    }

    func stopSession() {
        guard session.isRunning else { return }
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.session.stopRunning()
            DispatchQueue.main.async {
                self?.isSessionRunning = false
            }
        }
    }

    // MARK: Camera Switch

    func switchCamera() {
        let newPosition: AVCaptureDevice.Position = (currentCameraPosition == .back) ? .front : .back

        guard let newDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: newPosition),
              let newInput = try? AVCaptureDeviceInput(device: newDevice) else { return }

        session.beginConfiguration()
        for input in session.inputs { session.removeInput(input) }

        // Re-assert .photo preset on every switch. toggleFrontExpand sets
        // device.activeFormat directly, which silently downgrades the session to
        // .inputPriority. AVCaptureDevice.default(...) returns a singleton, so
        // without this re-assert the widest format stays sticky across switches —
        // back→front→back→front would cache the widest format as "default" and
        // the expand toggle would silently no-op on the second front visit.
        if session.sessionPreset != .photo {
            session.sessionPreset = .photo
        }

        if session.canAddInput(newInput) {
            session.addInput(newInput)
            currentDevice = newDevice
            currentCameraPosition = newPosition
            currentZoomFactor = 1.0
            // We always add the wide-angle on flip — make sure the ultra-wide flag
            // doesn't survive a back→front→back cycle.
            isUsingUltraWide = false
        }
        session.commitConfiguration()

        // Cache the front-camera formats so the expand button can flip between the default
        // (narrower-FOV) format and the widest-FOV format — the same trick the iOS system
        // camera uses to surface "0.5x" on devices with no ultra-wide front lens.
        //
        // Subtle dependency: this trusts `newDevice.activeFormat` to be the .photo-preset
        // default rather than a sticky widest-FOV format left over from a prior expand.
        // The `session.sessionPreset = .photo` re-assert above is what makes that true —
        // do not remove it without rethinking this cache.
        if newPosition == .front {
            let defaultFormat = newDevice.activeFormat
            frontCameraDefaultFormat = defaultFormat
            frontCameraWidestFormat = Self.bestExpandFormat(from: newDevice.formats,
                                                            matching: defaultFormat)
            isFrontCameraExpanded = false
        } else {
            frontCameraDefaultFormat = nil
            frontCameraWidestFormat = nil
            isFrontCameraExpanded = false
        }
    }

    /// Picks the widest-FOV format that's compatible with the photo path.
    /// Naive `.max(by: videoFieldOfView)` could return a slow-mo or oddly-shaped
    /// format; this constrains the search to formats matching the default's aspect
    /// ratio and tie-breaks ties in FOV by highest pixel area, so capture quality
    /// in expanded mode stays close to the default format.
    private static func bestExpandFormat(from formats: [AVCaptureDevice.Format],
                                         matching defaultFormat: AVCaptureDevice.Format)
        -> AVCaptureDevice.Format?
    {
        let defaultDims = CMVideoFormatDescriptionGetDimensions(defaultFormat.formatDescription)
        guard defaultDims.height > 0 else {
            return formats.max { $0.videoFieldOfView < $1.videoFieldOfView }
        }
        let defaultRatio = CGFloat(defaultDims.width) / CGFloat(defaultDims.height)

        let candidates = formats.filter { fmt in
            let dims = CMVideoFormatDescriptionGetDimensions(fmt.formatDescription)
            guard dims.height > 0 else { return false }
            let ratio = CGFloat(dims.width) / CGFloat(dims.height)
            return abs(ratio - defaultRatio) < 0.05
        }
        let pool = candidates.isEmpty ? formats : candidates

        guard let maxFov = pool.map(\.videoFieldOfView).max() else { return nil }
        let widest = pool.filter { abs($0.videoFieldOfView - maxFov) < 0.5 }

        return widest.max { lhs, rhs in
            let l = CMVideoFormatDescriptionGetDimensions(lhs.formatDescription)
            let r = CMVideoFormatDescriptionGetDimensions(rhs.formatDescription)
            return Int(l.width) * Int(l.height) < Int(r.width) * Int(r.height)
        }
    }

    /// Toggles the front camera between its default (narrower-FOV) format and the widest
    /// available format. Mirrors the iOS Camera app's expand button behavior.
    func toggleFrontExpand() {
        guard let device = currentDevice,
              currentCameraPosition == .front,
              let defaultFormat = frontCameraDefaultFormat,
              let widestFormat = frontCameraWidestFormat,
              defaultFormat !== widestFormat else { return }

        let willExpand = !isFrontCameraExpanded
        let targetFormat = willExpand ? widestFormat : defaultFormat

        session.beginConfiguration()
        do {
            try device.lockForConfiguration()
            if device.activeFormat !== targetFormat {
                device.activeFormat = targetFormat
            }
            // In expanded mode, push zoom all the way to the device's lowest factor on the
            // newly-active format (gives the absolute widest field of view the hardware allows).
            // On collapse, snap back to 1.0x.
            let targetZoom: CGFloat = willExpand
                ? CGFloat(device.minAvailableVideoZoomFactor)
                : max(CGFloat(device.minAvailableVideoZoomFactor),
                      min(1.0, CGFloat(device.maxAvailableVideoZoomFactor)))
            device.videoZoomFactor = targetZoom
            device.unlockForConfiguration()

            DispatchQueue.main.async {
                self.currentZoomFactor = targetZoom
                self.isFrontCameraExpanded = willExpand
            }
        } catch {
            AppLogger.log(.error, "Failed to toggle front-camera expand: \(error)", category: .photo)
        }
        session.commitConfiguration()
    }

    // MARK: Flash

    func toggleFlash() {
        switch flashMode {
        case .off:  flashMode = .on
        case .on:   flashMode = .auto
        case .auto: flashMode = .off
        @unknown default: flashMode = .off
        }
    }

    // MARK: Zoom
    //
    // Zoom is tracked in two spaces:
    //   - display: what the user sees / labels show (".5x", "1x", "2x"). Pinch and
    //     preset chips operate here. `currentZoomFactor` is published in this space.
    //   - device: the hardware `videoZoomFactor` on whichever lens is active. The
    //     ultra-wide reports its own widest FOV as device 1.0x, but UI labels that
    //     as 0.5x, so a 2× factor sits between them on the rear path.

    /// Preset chips shown to the user. Rear cameras get 0.5x only when the iPad has
    /// a rear ultra-wide lens; the front camera uses `toggleFrontExpand` instead and
    /// returns no presets here.
    var availableZoomPresets: [CGFloat] {
        guard currentCameraPosition == .back else { return [] }
        return Self.hasRearUltraWide ? [0.5, 1.0, 2.0] : [1.0, 2.0]
    }

    /// display↔device factor conversions. Identity except when the rear ultra-wide
    /// lens is mounted, where display = device × 0.5.
    private func deviceZoom(forDisplay display: CGFloat) -> CGFloat {
        isUsingUltraWide ? display * 2.0 : display
    }
    private func displayZoom(forDevice device: CGFloat) -> CGFloat {
        isUsingUltraWide ? device * 0.5 : device
    }

    /// Half-width of the dead-band around the 1.0x boundary, in display space. Used
    /// only when `setZoom` is called from a continuous gesture (pinch), where rapid
    /// crossings would otherwise thrash the lens swap. Preset taps bypass this band.
    private static let lensSwapHysteresis: CGFloat = 0.05

    /// `factor` is in display space. Swaps rear lenses at the 1.0x boundary when the
    /// device has an ultra-wide, so a single pinch or preset tap can cross 0.5↔1.0
    /// without the caller knowing which lens is mounted.
    ///
    /// `applyHysteresis: true` widens the swap boundary by ±`lensSwapHysteresis` to
    /// damp pinch oscillation at 1.0x. Preset chip taps must leave this `false` —
    /// when the user taps "1x" while on ultra-wide, they expect to swap immediately,
    /// not stay on ultra at a remapped 1.0x.
    func setZoom(factor: CGFloat, applyHysteresis: Bool = false) {
        let clampedDisplay = max(minZoomFactor, min(factor, maxZoomFactor))

        // Decide which rear lens this display factor lives on, and swap if needed.
        // Without hysteresis: < 1.0 → ultra-wide, >= 1.0 → wide.
        // With hysteresis: the active lens "sticks" through a ±band around 1.0, so a
        // pinch hovering at the boundary doesn't reconfigure the session 30×/sec.
        let wantsUltraWide: Bool
        if currentCameraPosition == .back && Self.hasRearUltraWide {
            if applyHysteresis {
                let band = Self.lensSwapHysteresis
                wantsUltraWide = isUsingUltraWide
                    ? clampedDisplay < 1.0 + band   // stay on ultra until we clear the upper edge
                    : clampedDisplay < 1.0 - band   // stay on wide until we clear the lower edge
            } else {
                wantsUltraWide = clampedDisplay < 1.0
            }
        } else {
            wantsUltraWide = false
        }
        if wantsUltraWide != isUsingUltraWide && currentCameraPosition == .back {
            swapBackLens(toUltraWide: wantsUltraWide)
        }

        guard let device = currentDevice else { return }
        let deviceFactor = deviceZoom(forDisplay: clampedDisplay)
        let clampedDevice = max(
            CGFloat(device.minAvailableVideoZoomFactor),
            min(deviceFactor, CGFloat(device.maxAvailableVideoZoomFactor))
        )
        do {
            try device.lockForConfiguration()
            device.videoZoomFactor = clampedDevice
            device.unlockForConfiguration()
            let publishedDisplay = displayZoom(forDevice: clampedDevice)
            DispatchQueue.main.async { self.currentZoomFactor = publishedDisplay }
        } catch {
            AppLogger.log(.error, "Failed to set zoom: \(error)", category: .photo)
        }
    }

    /// Swaps the rear input between the wide-angle and ultra-wide physical cameras
    /// without tearing down the session. Same pattern as `switchCamera` but stays on
    /// `.back` — the preview layer is wired to the session, not a specific device,
    /// so it picks up the new input automatically. The rotation coordinator is left
    /// pointing at the prior device; on iPad both rear cameras share sensor
    /// orientation, so the published rotation angle stays correct.
    private func swapBackLens(toUltraWide: Bool) {
        let deviceType: AVCaptureDevice.DeviceType = toUltraWide
            ? .builtInUltraWideCamera
            : .builtInWideAngleCamera
        guard let newDevice = AVCaptureDevice.default(deviceType, for: .video, position: .back),
              let newInput = try? AVCaptureDeviceInput(device: newDevice) else { return }

        // Snapshot the current video inputs so we can restore them if `canAddInput`
        // rejects the new device. We filter to video-only to avoid disturbing any
        // future audio input — the photo path doesn't use audio today, but pulling
        // it out unconditionally would be a footgun if that ever changes.
        let priorVideoInputs = session.inputs.compactMap { $0 as? AVCaptureDeviceInput }
            .filter { $0.device.hasMediaType(.video) }

        session.beginConfiguration()
        for input in priorVideoInputs { session.removeInput(input) }
        // Re-assert .photo preset — `toggleFrontExpand` can silently downgrade the
        // session to .inputPriority. Same defensive re-assert as `switchCamera`.
        if session.sessionPreset != .photo { session.sessionPreset = .photo }

        if session.canAddInput(newInput) {
            session.addInput(newInput)
            currentDevice = newDevice
            isUsingUltraWide = toUltraWide
        } else {
            // Adding the new input failed (rare — same singleton lens AVFoundation
            // accepted moments ago). Restore the prior video input(s) in the same
            // config transaction so the session doesn't go black.
            AppLogger.log(.error, "swapBackLens: canAddInput rejected \(deviceType); restoring prior input", category: .photo)
            for input in priorVideoInputs where session.canAddInput(input) {
                session.addInput(input)
            }
        }
        session.commitConfiguration()
    }

    /// Cycles through 0.5x → 1x → 2x (skipping presets outside device range)
    func cycleZoomPreset() {
        let presets = availableZoomPresets
        guard !presets.isEmpty else { return }
        // Pick the next preset strictly above the current level, or wrap around
        if let next = presets.first(where: { $0 > currentZoomFactor + 0.05 }) {
            setZoom(factor: next)
        } else {
            setZoom(factor: presets[0])
        }
    }

    // MARK: Photo Capture

    func takePhoto() {
        guard !isCapturing else { return }
        isCapturing = true

        let settings = AVCapturePhotoSettings()

        // Hardware flash for back camera; screen flash handled by the view layer
        if currentCameraPosition == .back && hasHardwareFlash {
            settings.flashMode = flashMode
        } else {
            settings.flashMode = .off
        }

        // Set the photo output connection's rotation angle to match device orientation.
        // Prefer the rotation coordinator (hardware-accurate, used by preview) — falling back
        // to the interface-orientation helper only when no coordinator is wired up yet.
        if let connection = photoOutput.connection(with: .video) {
            let angle = rotationCoordinator?.videoRotationAngleForHorizonLevelCapture
                ?? CameraOrientationHelper.currentRotationAngle()
            if connection.isVideoRotationAngleSupported(angle) {
                connection.videoRotationAngle = angle
            }
        }

        let needsScreenFlash = currentCameraPosition == .front && flashMode != .off

        if needsScreenFlash {
            showScreenFlash = true
            // Delay capture so the bright screen illuminates the subject
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                guard let self else { return }
                self.photoOutput.capturePhoto(with: settings, delegate: self)
            }
        } else {
            photoOutput.capturePhoto(with: settings, delegate: self)
        }
    }

    func photoOutput(_ output: AVCapturePhotoOutput,
                     didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        defer {
            DispatchQueue.main.async {
                self.isCapturing = false
                self.showScreenFlash = false
            }
        }

        if let error = error {
            AppLogger.log(.error, "iPad camera capture failed: \(error.localizedDescription)", category: .photo)
            return
        }

        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else {
            AppLogger.log(.error, "iPad camera: failed to create image from capture data", category: .photo)
            return
        }

        // Always un-mirror front-camera captures — equivalent to iOS Settings ›
        // Camera › "Mirror Front Camera" being OFF, which is the product default we
        // want here so text/badges in selfies read correctly when reviewed later.
        // (The user-facing setting is not exposed via a public API, so this is a
        // fixed choice, not a runtime check; revisit if the app ever surfaces its
        // own toggle.) The flip is applied only via the imageOrientation flag, not
        // by re-rendering pixels — the downstream resize step honors orientation,
        // so the flip propagates through the pipeline at the resized (~1MB) size
        // instead of allocating a ~50MB full-res copy here.
        let finalImage: UIImage = (currentCameraPosition == .front)
            ? image.withMirroredOrientation()
            : image

        DispatchQueue.main.async {
            self.capturedImage = finalImage
            self.stopSession()
        }
    }
}

// MARK: - Camera Orientation Helper
// Shared utility to map current interface orientation to AVCaptureConnection rotation angles.

enum CameraOrientationHelper {
    /// Returns the videoRotationAngle (in degrees) matching the current interface orientation.
    static func currentRotationAngle() -> CGFloat {
        guard let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first else {
            return 90 // portrait default
        }
        return rotationAngle(for: windowScene.interfaceOrientation)
    }

    /// Maps UIInterfaceOrientation to AVCaptureConnection.videoRotationAngle (degrees).
    /// The camera sensor is physically mounted in landscape-right, so:
    /// - portrait = 90° from sensor
    /// - landscapeLeft = 180° from sensor
    /// - landscapeRight = 0° (matches sensor)
    static func rotationAngle(for orientation: UIInterfaceOrientation) -> CGFloat {
        switch orientation {
        case .portrait:            return 90
        case .portraitUpsideDown:  return 270
        case .landscapeRight:      return 0
        case .landscapeLeft:       return 180
        @unknown default:          return 90
        }
    }
}

// MARK: - iPad Camera Preview

struct IPadCameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    let captureDevice: AVCaptureDevice?
    let cameraManager: IPadCameraManager
    /// Triggers updateUIView when camera switches (AVCaptureDevice is a class, so SwiftUI
    /// needs an Equatable value to detect the change reliably).
    let cameraPosition: AVCaptureDevice.Position

    func makeCoordinator() -> Coordinator {
        Coordinator(cameraManager: cameraManager)
    }

    func makeUIView(context: Context) -> IPadPreviewUIView {
        let view = IPadPreviewUIView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill

        let pinch = UIPinchGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handlePinch(_:))
        )
        view.addGestureRecognizer(pinch)

        if let device = captureDevice {
            view.configureRotationTracking(for: device, manager: cameraManager)
        }

        return view
    }

    func updateUIView(_ uiView: IPadPreviewUIView, context: Context) {
        // Re-configure rotation tracking when the camera device changes
        if let device = captureDevice {
            uiView.configureRotationTracking(for: device, manager: cameraManager)
        }
    }

    // MARK: Pinch-to-zoom coordinator

    class Coordinator: NSObject {
        let cameraManager: IPadCameraManager
        private var initialZoomFactor: CGFloat = 1.0
        private var didTriggerFrontExpand = false

        init(cameraManager: IPadCameraManager) {
            self.cameraManager = cameraManager
        }

        @objc func handlePinch(_ gesture: UIPinchGestureRecognizer) {
            switch gesture.state {
            case .began:
                initialZoomFactor = cameraManager.currentZoomFactor
                didTriggerFrontExpand = false
            case .changed:
                // Front camera: pinch toggles between default and widest-FOV formats
                // (matches iOS Camera — there's no usable sub-1x digital zoom on iPad
                // front cams). Each gesture fires the toggle at most once.
                if cameraManager.currentCameraPosition == .front {
                    guard !didTriggerFrontExpand else { return }
                    let scale = gesture.scale
                    if !cameraManager.isFrontCameraExpanded && scale < 0.85 {
                        // Pinch in → switch to widest format
                        cameraManager.toggleFrontExpand()
                        didTriggerFrontExpand = true
                    } else if cameraManager.isFrontCameraExpanded && scale > 1.15 {
                        // Pinch out → return to default format
                        cameraManager.toggleFrontExpand()
                        didTriggerFrontExpand = true
                    }
                } else {
                    cameraManager.setZoom(
                        factor: initialZoomFactor * gesture.scale,
                        applyHysteresis: true
                    )
                }
            default:
                break
            }
        }
    }

    /// Uses AVCaptureVideoPreviewLayer as the backing layer — auto-resizes with the view.
    /// Uses AVCaptureDevice.RotationCoordinator to track device rotation via KVO,
    /// which works in ALL presentation contexts including sheets where layoutSubviews()
    /// does not fire on iPad rotation.
    class IPadPreviewUIView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

        private var rotationCoordinator: AVCaptureDevice.RotationCoordinator?
        private var rotationObservation: NSKeyValueObservation?
        private weak var trackedDevice: AVCaptureDevice?

        func configureRotationTracking(for device: AVCaptureDevice, manager: IPadCameraManager? = nil) {
            guard device !== trackedDevice else { return }
            trackedDevice = device

            rotationObservation?.invalidate()
            rotationObservation = nil
            rotationCoordinator = nil

            rotationCoordinator = AVCaptureDevice.RotationCoordinator(
                device: device,
                previewLayer: previewLayer
            )

            // Share the coordinator with the manager so photo capture uses the same
            // hardware-accurate rotation angle the preview is using.
            manager?.rotationCoordinator = rotationCoordinator

            guard let coordinator = rotationCoordinator else { return }

            let initialAngle = coordinator.videoRotationAngleForHorizonLevelPreview
            if let connection = previewLayer.connection,
               connection.isVideoRotationAngleSupported(initialAngle) {
                connection.videoRotationAngle = initialAngle
            }

            rotationObservation = coordinator.observe(
                \.videoRotationAngleForHorizonLevelPreview,
                options: .new
            ) { [weak self] coord, _ in
                DispatchQueue.main.async {
                    guard let self,
                          let connection = self.previewLayer.connection else { return }
                    let angle = coord.videoRotationAngleForHorizonLevelPreview
                    if connection.isVideoRotationAngleSupported(angle) {
                        connection.videoRotationAngle = angle
                    }
                }
            }
        }

        deinit {
            rotationObservation?.invalidate()
        }
    }
}

// MARK: - iPad Camera View

struct IPadCameraView: View {
    let onImageCaptured: (UIImage) -> Void
    let onDismiss: () -> Void
    @StateObject private var cameraManager = IPadCameraManager()
    @State private var showReview = false
    @State private var previousBrightness: CGFloat = 0.5
    @State private var showFlashOptions = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let image = cameraManager.capturedImage, showReview {
                // Photo review screen (matches UIImagePickerController's Retake/Use Photo)
                VStack(spacing: 0) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                    HStack {
                        Button(AppStrings.Photos.retake) {
                            cameraManager.capturedImage = nil
                            showReview = false
                            cameraManager.startSession()
                        }
                        .font(.body)
                        .foregroundColor(.white)

                        Spacer()

                        Button(AppStrings.Photos.usePhoto) {
                            cameraManager.stopSession()
                            onImageCaptured(image)
                        }
                        .font(.body)
                        .foregroundColor(.white)
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 16)
                    .background(Color.black.opacity(0.85))
                }
            } else {
                // Live camera preview with pinch-to-zoom
                IPadCameraPreview(
                    session: cameraManager.session,
                    captureDevice: cameraManager.currentDevice,
                    cameraManager: cameraManager,
                    cameraPosition: cameraManager.currentCameraPosition
                )
                .ignoresSafeArea()

                // Screen flash overlay (front camera flash substitute)
                if cameraManager.showScreenFlash {
                    Color.white
                        .ignoresSafeArea()
                }

                // Outside-tap catcher: dismisses the flash options popup when the user taps
                // anywhere else. Sits below the controls overlay so the controls themselves
                // still receive taps.
                if showFlashOptions {
                    Color.clear
                        .contentShape(Rectangle())
                        .ignoresSafeArea()
                        .onTapGesture {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                showFlashOptions = false
                            }
                        }
                }

                // Camera controls overlay (matches iPad system camera layout):
                // - Top-right: close (X)
                // - Left-center: zoom preset (1x)
                // - Right-center, stacked top→bottom: flash, switch camera, shutter
                ZStack {
                    // Top bar: close (X) only, top-right
                    VStack {
                        HStack {
                            Spacer()

                            Button(action: {
                                cameraManager.stopSession()
                                onDismiss()
                            }) {
                                Image(systemName: "xmark")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 48, height: 48)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Circle())
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 40)

                        Spacer()
                    }

                    // Left-edge, vertically centered.
                    // Back camera: zoom preset (0.5x / 1x / 2x).
                    // Front camera: expand toggle that flips the active capture format between
                    // the default (narrower-FOV) and widest-FOV format on the same wide-angle
                    // lens — the same trick iOS Camera uses to surface a "0.5x" affordance on
                    // devices with no front ultra-wide.
                    HStack {
                        if cameraManager.currentCameraPosition == .back {
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    cameraManager.cycleZoomPreset()
                                }
                            }) {
                                Text(zoomButtonLabel)
                                    .font(.system(size: 14, weight: .semibold, design: .rounded))
                                    .foregroundColor(isAtDefaultZoom ? .white : .yellow)
                                    .frame(width: 48, height: 48)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Circle())
                            }
                            .padding(.leading, 40)
                        } else {
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    cameraManager.toggleFrontExpand()
                                }
                            }) {
                                Image(systemName: isFrontCameraWidened ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.white)
                                    .frame(width: 48, height: 48)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Circle())
                            }
                            .padding(.leading, 40)
                        }

                        Spacer()
                    }

                    // Right-edge: flash + switch camera + shutter, vertically centered
                    HStack {
                        Spacer()

                        VStack(spacing: 24) {
                            // Flash control: tap icon to expand a single pill containing Auto/On/Off
                            // (mirrors the iOS Camera app). Inner HStack overflows leftward when
                            // expanded; outer frame pins the VStack width so the switch/shutter
                            // buttons don't shift.
                            if cameraManager.isFlashAvailable {
                                HStack(spacing: 8) {
                                    if showFlashOptions {
                                        HStack(spacing: 0) {
                                            flashOptionChip(mode: .auto, label: AppStrings.Photos.flashAuto)
                                            flashOptionChip(mode: .on,   label: AppStrings.Photos.flashOn)
                                            flashOptionChip(mode: .off,  label: AppStrings.Photos.flashOff)
                                        }
                                        .padding(.horizontal, 6)
                                        .frame(height: 44)
                                        .background(Color.black.opacity(0.5))
                                        .clipShape(Capsule())
                                    }

                                    Button(action: {
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            showFlashOptions.toggle()
                                        }
                                    }) {
                                        Image(systemName: flashIconName)
                                            .font(.title2)
                                            .foregroundColor(cameraManager.flashMode == .off ? .white : .yellow)
                                            .shadow(radius: 4)
                                            .frame(width: 48, height: 48)
                                            .background(Color.black.opacity(0.5))
                                            .clipShape(Circle())
                                    }
                                }
                                .fixedSize()
                                .frame(width: 48, height: 48, alignment: .trailing)
                            }

                            // Switch camera (front/back)
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.3)) {
                                    cameraManager.switchCamera()
                                }
                            }) {
                                Image(systemName: "camera.rotate.fill")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                    .shadow(radius: 4)
                                    .frame(width: 48, height: 48)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Circle())
                            }

                            // Shutter button
                            Button(action: {
                                cameraManager.takePhoto()
                            }) {
                                ZStack {
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width: 70, height: 70)
                                    Circle()
                                        .stroke(Color.white, lineWidth: 4)
                                        .frame(width: 80, height: 80)
                                }
                            }
                            .disabled(cameraManager.isCapturing)
                            .opacity(cameraManager.isCapturing ? 0.5 : 1.0)
                        }
                        .padding(.trailing, 40)
                    }
                }
            }
        }
        .onAppear {
            cameraManager.startSession()
        }
        .onDisappear {
            cameraManager.stopSession()
            // Restore brightness if screen flash was active
            if cameraManager.showScreenFlash {
                UIScreen.main.brightness = previousBrightness
            }
        }
        .onChange(of: cameraManager.capturedImage) { _, newImage in
            if newImage != nil {
                showReview = true
            }
        }
        .onChange(of: cameraManager.showScreenFlash) { _, show in
            if show {
                previousBrightness = UIScreen.main.brightness
                UIScreen.main.brightness = 1.0
            } else {
                UIScreen.main.brightness = previousBrightness
            }
        }
    }

    private var flashIconName: String {
        switch cameraManager.flashMode {
        case .off:  return "bolt.slash.fill"
        case .on:   return "bolt.fill"
        case .auto: return "bolt.badge.automatic.fill"
        @unknown default: return "bolt.slash.fill"
        }
    }

    @ViewBuilder
    private func flashOptionChip(mode: AVCaptureDevice.FlashMode, label: String) -> some View {
        let isSelected = cameraManager.flashMode == mode
        Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) {
                cameraManager.flashMode = mode
                showFlashOptions = false
            }
        }) {
            Text(label)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundColor(isSelected ? .yellow : .white)
                .frame(minWidth: 44, minHeight: 32)
                .padding(.horizontal, 4)
        }
        .buttonStyle(.plain)
    }

    private var zoomButtonLabel: String {
        let zoom = cameraManager.currentZoomFactor
        if abs(zoom - 0.5) < 0.05 { return ".5x" }
        if abs(zoom - floor(zoom)) < 0.05 {
            return String(format: "%.0fx", zoom)
        }
        return String(format: "%.1fx", zoom)
    }

    private var isAtDefaultZoom: Bool {
        abs(cameraManager.currentZoomFactor - 1.0) < 0.05
    }

    /// True when the front camera is currently in its wider-FOV format.
    private var isFrontCameraWidened: Bool {
        cameraManager.currentCameraPosition == .front && cameraManager.isFrontCameraExpanded
    }
}

// MARK: - iPhone Camera (UIImagePickerController)
// Used on iPhones where flash works correctly and the system alert doesn't appear.

struct EntityCameraViewRepresentable: UIViewControllerRepresentable {
    let onImageCaptured: (UIImage) -> Void
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        picker.modalPresentationStyle = .overFullScreen
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: EntityCameraViewRepresentable

        init(_ parent: EntityCameraViewRepresentable) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImageCaptured(image)
            }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onDismiss()
        }
    }
}

extension UIImage {
    /// Returns a UIImage that displays as a horizontal flip of self, sharing the
    /// underlying cgImage — only the imageOrientation flag is toggled. This is
    /// effectively free vs. `UIGraphicsImageRenderer`-based flipping, which would
    /// allocate a full-resolution pixel buffer (~50MB for 12MP iPad captures).
    /// `UIGraphicsImageRenderer.draw(in:)`, `UIImageView`, and `jpegData()` all
    /// honor imageOrientation, so the flip propagates correctly through resize/encode.
    ///
    /// Mirror table: a horizontal flip in *display* space, expressed as a single
    /// `UIImage.Orientation`, swaps within these pairs:
    /// - `.up ↔ .upMirrored`, `.down ↔ .downMirrored`
    /// - `.right ↔ .leftMirrored`, `.left ↔ .rightMirrored`
    ///
    /// The diagonal pairs are non-obvious: a 90°-rotated display, flipped
    /// horizontally, is a *transpose* of the cgImage, not an anti-transpose —
    /// so `.right` (R90 CW) maps to `.leftMirrored` (transpose), not `.rightMirrored`
    /// (anti-transpose). Mixing them up rotates the result by 180° (the
    /// portrait-front-cam-upside-down regression — see ZP-2002).
    func withMirroredOrientation() -> UIImage {
        guard let cg = cgImage else { return self }
        let mirrored: UIImage.Orientation
        switch imageOrientation {
        case .up:            mirrored = .upMirrored
        case .down:          mirrored = .downMirrored
        case .left:          mirrored = .rightMirrored
        case .right:         mirrored = .leftMirrored
        case .upMirrored:    mirrored = .up
        case .downMirrored:  mirrored = .down
        case .leftMirrored:  mirrored = .right
        case .rightMirrored: mirrored = .left
        @unknown default:    mirrored = imageOrientation
        }
        return UIImage(cgImage: cg, scale: scale, orientation: mirrored)
    }

    func resized(maxDimension: CGFloat) -> UIImage? {
        let aspect = size.width / size.height
        let targetSize: CGSize
        if size.width > size.height {
            targetSize = CGSize(width: maxDimension, height: maxDimension / aspect)
        } else {
            targetSize = CGSize(width: maxDimension * aspect, height: maxDimension)
        }
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
}
