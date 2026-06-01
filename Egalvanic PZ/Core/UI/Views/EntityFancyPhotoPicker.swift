//
//  EntityFancyPhotoPicker.swift
//  SwiftDataTutorial
//
//  Advanced photo picker view supporting multiple photo types for a single entity
//

import SwiftUI
import PhotosUI
import SwiftData

struct EntityFancyPhotoPicker<Entity: EntityWithMultiTypePhotos>: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    let entity: Entity
    @Binding var displayedPhotos: [Photo]
    @Binding var isSaving: Bool

    // Selected photo type
    @State private var selectedPhotoType: PhotoTypeConfiguration

    // Callbacks for photo operations
    var onPhotoAdded: ((Photo) -> Void)?
    var onPhotoDeleted: ((Photo) -> Void)?
    var onUploadRequested: ((Photo) async throws -> Void)?
    var onSetDefaultPhoto: ((UUID) -> Void)?

    // Optional draft state for immediate UI feedback
    var draftDefaultPhotoId: UUID?

    // Optional customization
    var sectionTitle: String = "Photos"
    var sectionIcon: String = "photo.stack"
    var showUploadIndicators: Bool = true
    var showPhotoTypeSelector: Bool = true
    var maxPhotosPerType: Int? = nil
    var skipEntityAssociation: Bool = false  // For AddAssetView - don't associate with temp node
    var overrideSLD: SLDV2? = nil  // Optional SLD override for temp entities
    /// ZP-2230: for create flows where the user has typed an asset
    /// name into the form but the placeholder entity's ``.label`` is
    /// still the generic stub. Passing the live form text here keeps
    /// the stamp label in sync with what the user has typed.
    var overrideLabel: String? = nil
    /// ZP-2230: similarly, the user usually picks a room before
    /// capturing photos. The placeholder entity's room relationship
    /// isn't set until save, so the stamp would have no breadcrumb;
    /// pass the currently-selected room here.
    var overrideRoom: Room? = nil
    
    // Local UI state
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var showingCamera = false
    @State private var selectedPhoto: Photo? = nil
    @State private var capturedImage: UIImage? = nil
    @State private var showAllTypes = false
    // ZP-2230
    @State private var showCaptureError = false
    @State private var captureErrorMessage = ""
    @State private var showPermissionPrompt = false
    
    init(entity: Entity,
         displayedPhotos: Binding<[Photo]>,
         isSaving: Binding<Bool>,
         onPhotoAdded: ((Photo) -> Void)? = nil,
         onPhotoDeleted: ((Photo) -> Void)? = nil,
         onUploadRequested: ((Photo) async throws -> Void)? = nil,
         sectionTitle: String = "Photos",
         sectionIcon: String = "photo.stack",
         showUploadIndicators: Bool = true,
         showPhotoTypeSelector: Bool = true,
         maxPhotosPerType: Int? = nil,
         skipEntityAssociation: Bool = false,
         overrideSLD: SLDV2? = nil,
         overrideLabel: String? = nil,
         overrideRoom: Room? = nil,
         onSetDefaultPhoto: ((UUID) -> Void)? = nil,
         draftDefaultPhotoId: UUID? = nil) {

        self.entity = entity
        self._displayedPhotos = displayedPhotos
        self._isSaving = isSaving
        self.onPhotoAdded = onPhotoAdded
        self.onPhotoDeleted = onPhotoDeleted
        self.onUploadRequested = onUploadRequested
        self.sectionTitle = sectionTitle
        self.sectionIcon = sectionIcon
        self.showUploadIndicators = showUploadIndicators
        self.showPhotoTypeSelector = showPhotoTypeSelector
        self.maxPhotosPerType = maxPhotosPerType
        self.skipEntityAssociation = skipEntityAssociation
        self.overrideSLD = overrideSLD
        self.overrideLabel = overrideLabel
        self.overrideRoom = overrideRoom
        self.onSetDefaultPhoto = onSetDefaultPhoto
        self.draftDefaultPhotoId = draftDefaultPhotoId
        
        // Initialize with default photo type
        self._selectedPhotoType = State(initialValue: entity.defaultPhotoType ?? entity.supportedPhotoTypes.first ?? PhotoTypeConfiguration(type: "general", displayName: "General"))
    }
    
    private var visiblePhotos: [Photo] {
        let filtered = displayedPhotos.filter { !$0.is_deleted }
        
        // Filter by selected type if not showing all
        let typeFiltered = showAllTypes ? filtered : filtered.filter { $0.type == selectedPhotoType.type }
        
        // Apply max limit if specified
        if let max = maxPhotosPerType {
            return Array(typeFiltered.prefix(max))
        }
        return typeFiltered
    }
    
    private var photoCountByType: [String: Int] {
        let photos = displayedPhotos.filter { !$0.is_deleted }
        var counts: [String: Int] = [:]
        for photo in photos {
            counts[photo.type, default: 0] += 1
        }
        return counts
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header with type selector
            VStack(spacing: 12) {
                HStack {
                    SectionHeader(title: sectionTitle, systemImage: sectionIcon)
                    
                    Spacer()
                    
                    // Toggle to show all types
                    Button(action: { showAllTypes.toggle() }) {
                        Image(systemName: showAllTypes ? "square.grid.2x2.fill" : "square.grid.2x2")
                            .foregroundColor(.blue)
                            .font(.title3)
                    }
                }
                
                // Photo type selector
                if showPhotoTypeSelector && !entity.supportedPhotoTypes.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(entity.supportedPhotoTypes, id: \.type) { photoType in
                                PhotoTypeChip(
                                    config: photoType,
                                    isSelected: selectedPhotoType.type == photoType.type && !showAllTypes,
                                    count: photoCountByType[photoType.type] ?? 0,
                                    onTap: {
                                        selectedPhotoType = photoType
                                        showAllTypes = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Photo gallery
            if !visiblePhotos.isEmpty && !isSaving {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(visiblePhotos) { photo in
                            FancyPhotoThumbnailView(
                                photo: photo,
                                photoType: entity.supportedPhotoTypes.first { $0.type == photo.type },
                                showUploadIndicator: showUploadIndicators,
                                showTypeLabel: showAllTypes,  // Only show type label when viewing all types
                                onTap: { selectedPhoto = photo },
                                onDelete: {
                                    onPhotoDeleted?(photo)
                                },
                                isDefault: draftDefaultPhotoId != nil ? (draftDefaultPhotoId == photo.id) : ((entity as? NodeV2)?.default_photo_id == photo.id),
                                onSetDefault: onSetDefaultPhoto != nil ? {
                                    onSetDefaultPhoto?(photo.id)
                                } : nil
                            )
                            .id(photo.id) // Maintain stable view identity to preserve cache
                        }
                    }
                    .padding(.horizontal)
                }
                .frame(height: 130) // Standard height for photo thumbnails
            } else if !isSaving {
                // Empty state
                VStack(spacing: 8) {
                    Image(systemName: selectedPhotoType.icon)
                        .font(.system(size: 40))
                        .foregroundColor(.gray.opacity(0.3))
                    Text(AppStrings.Forms.noEntityAttributes("\(selectedPhotoType.displayName) photos"))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 100)
                .background(Color(.systemGray6))
                .cornerRadius(12)
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
                
                // Camera button — ZP-2230: gate on Photos write access.
                Button {
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
            .frame(maxWidth: .infinity)
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
        // ZP-2230
        .alert("Could not save photo", isPresented: $showCaptureError) {
            Button("OK", role: .cancel) {}
        } message: { Text(captureErrorMessage) }
        .alert("Photos library access required", isPresented: $showPermissionPrompt) {
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Egalvanic PZ saves a stamped copy of every captured photo to your Photos library as an audit-recovery copy. Enable “Add Photos Only” in Settings → Privacy → Photos → Egalvanic PZ to use the camera.")
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

    /// ZP-2230: route every capture through PhotoCaptureService.
    /// The fancy picker selects a ``photoType`` via segmented control
    /// before snapping the photo — pass the currently selected type
    /// into the stamp. When the caller threaded an ``overrideSLD``
    /// (e.g. AddAssetView with a placeholder node whose .sld
    /// relationship isn't wired up yet) we honor that override so
    /// the stamp's location breadcrumb still names the SLD.
    ///
    /// ``fromGallery`` skips the audit-copy save back to the Photos
    /// library because gallery-imported bytes already exist in the
    /// user's library — re-saving a stamped duplicate would force
    /// an addOnly permission grant the user shouldn't need.
    private func persistCapture(_ image: UIImage, fromGallery: Bool) async {
        do {
            var ctx = PhotoStampContext.from(entity: entity, photoType: selectedPhotoType.type)
            // ZP-2230: apply overrides from the form / current
            // selection. For create flows the entity is a placeholder
            // with a stub label and no room/sld wired up yet — the
            // form already knows the real values, so we honor them.
            if let override = overrideLabel,
               !override.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                ctx.entityLabel = override
            }
            if ctx.sld == nil, let override = overrideSLD {
                ctx.sld = override
            }
            if let r = overrideRoom {
                ctx.room = r
                if ctx.floor == nil { ctx.floor = r.floor }
                if ctx.building == nil { ctx.building = r.floor?.building }
                if ctx.sld == nil { ctx.sld = r.floor?.building?.sld }
            }
            let photo = try await PhotoCaptureService.captureAndPersistPending(
                image: image,
                context: ctx,
                saveAuditCopy: !fromGallery
            )
            onPhotoAdded?(photo)
        } catch {
            AppLogger.log(.error,
                "[PhotoCapture/fancy] capture failed: \(error.localizedDescription)",
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
    
    
    // Helper function to convert color strings to SwiftUI colors
    private func colorFromString(_ colorName: String) -> Color {
        switch colorName.lowercased() {
        case "blue":
            return .blue
        case "green":
            return .green
        case "red":
            return .red
        case "orange":
            return .orange
        case "purple":
            return .purple
        case "yellow":
            return .yellow
        case "gray", "grey":
            return .gray
        case "pink":
            return .pink
        default:
            return .blue
        }
    }
}

// MARK: - Photo Type Chip
struct PhotoTypeChip: View {
    let config: PhotoTypeConfiguration
    let isSelected: Bool
    let count: Int
    let onTap: () -> Void
    
    private func colorFromString(_ colorName: String) -> Color {
        switch colorName.lowercased() {
        case "blue":
            return .blue
        case "green":
            return .green
        case "red":
            return .red
        case "orange":
            return .orange
        case "purple":
            return .purple
        case "yellow":
            return .yellow
        case "gray", "grey":
            return .gray
        case "pink":
            return .pink
        default:
            return .blue
        }
    }
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Image(systemName: config.icon)
                    .font(.caption)
                Text(config.displayName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                if count > 0 {
                    Text("(\(count))")
                        .font(.caption)
                        .foregroundColor(isSelected ? .white.opacity(0.8) : .secondary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? colorFromString(config.color) : Color(.systemGray6))
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(20)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Fancy Photo Thumbnail View
struct FancyPhotoThumbnailView: View {
    let photo: Photo
    let photoType: PhotoTypeConfiguration?
    let showUploadIndicator: Bool
    let showTypeLabel: Bool  // Whether to show the type badge
    let onTap: () -> Void
    let onDelete: () -> Void
    let isDefault: Bool
    let onSetDefault: (() -> Void)?

    @State private var showCaptionEditor = false
    @State private var editingCaption = ""
    @State private var showDeleteAlert = false
    @Environment(\.modelContext) private var modelContext

    private func colorFromString(_ colorName: String) -> Color {
        switch colorName.lowercased() {
        case "blue":
            return .blue
        case "green":
            return .green
        case "red":
            return .red
        case "orange":
            return .orange
        case "purple":
            return .purple
        case "yellow":
            return .yellow
        case "gray", "grey":
            return .gray
        case "pink":
            return .pink
        default:
            return .blue
        }
    }
    
    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                // Photo image (bottom layer - receives tap gesture)
                Group {
                    // Priority 1: Check local file first for instant loading
                    if let localURL = photo.localFileURL,
                       FileManager.default.fileExists(atPath: localURL.path),
                       let uiImage = UIImage(contentsOfFile: localURL.path) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                    }
                    // Priority 2: Fallback to remote URL if local file doesn't exist (with presigned URL)
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
                                // Show photo icon when load fails (no internet/not cached)
                                ZStack {
                                    Color.gray.opacity(0.1)
                                    Image(systemName: "photo")
                                        .foregroundColor(.gray)
                                }
                            },
                            retryButtonAlignment: .center
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
                .contentShape(Rectangle())
                .onTapGesture(perform: onTap)
                .onLongPressGesture {
                    editingCaption = photo.caption ?? ""
                    showCaptionEditor = true
                }

                // Overlay indicators (non-interactive, pass through taps)
                VStack {
                    HStack {
                        // Default photo star indicator (top-left)
                        if isDefault {
                            Image(systemName: "star.fill")
                                .font(.caption2)
                                .padding(4)
                                .background(Color.yellow)
                                .foregroundColor(.white)
                                .clipShape(Circle())
                        }
                        Spacer()
                    }
                    Spacer()
                    HStack {
                        // Caption badge indicator (bottom-left)
                        if let caption = photo.caption, !caption.isEmpty {
                            PhotoCaptionBadge()
                        }
                        Spacer()
                        // Upload indicator (bottom-right)
                        if showUploadIndicator && photo.upload_needed {
                            Image(systemName: "icloud.and.arrow.up")
                                .font(.caption2)
                                .padding(4)
                                .background(Color.orange)
                                .foregroundColor(.white)
                                .clipShape(Circle())
                        }
                    }
                }
                .padding(5)
                .frame(width: 100, height: 100)
                .allowsHitTesting(false)  // Pass through taps to image below

                // 3-dot menu button (top layer - interactive)
                VStack {
                    HStack {
                        Spacer()
                        Menu {
                            // Set as Default option (only show if not already default and callback exists)
                            if !isDefault, let onSetDefault = onSetDefault {
                                Button(action: onSetDefault) {
                                    Label(AppStrings.Photos.setAsDefault, systemImage: "star.fill")
                                }
                            }

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

                            // Delete option — opens a native alert at the root view level
                            Button(role: .destructive) {
                                showDeleteAlert = true
                            } label: {
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
                    }
                    Spacer()
                }
                .padding(4)
                .frame(width: 100, height: 100)
            }
            
            // Type badge - only show when showTypeLabel is true
            if showTypeLabel, let type = photoType {
                HStack(spacing: 3) {
                    Image(systemName: type.icon)
                        .font(.system(size: 10))
                    Text(type.displayName)
                        .font(.system(size: 10, weight: .medium))
                }
                .foregroundColor(colorFromString(type.color))
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(colorFromString(type.color).opacity(0.1))
                .cornerRadius(4)
            }
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
        .alert(AppStrings.Photos.deleteThisPhoto, isPresented: $showDeleteAlert) {
            Button(AppStrings.Common.delete, role: .destructive) {
                onDelete()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        }
    }
}
