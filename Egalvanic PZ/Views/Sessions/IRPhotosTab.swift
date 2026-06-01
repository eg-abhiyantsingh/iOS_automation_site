//
//  IRPhotosTab.swift
//  SwiftDataTutorial
//
//  IR Photos tab — flat list of photo pairs with upload, edit, and delete.
//

import SwiftUI
import SwiftData
import PhotosUI
import UniformTypeIdentifiers
import CoreTransferable

/// Transferable wrapper that preserves the original filename from the photo library.
/// Copies the received file to a persistent temp location so it survives after the transfer closure returns.
struct IRPhotoFileTransferable: Transferable {
    let url: URL

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .image) { received in
            let tempDir = FileManager.default.temporaryDirectory
                .appendingPathComponent("ir_photo_imports", isDirectory: true)
            try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
            let destURL = tempDir.appendingPathComponent(received.file.lastPathComponent)
            if FileManager.default.fileExists(atPath: destURL.path) {
                try FileManager.default.removeItem(at: destURL)
            }
            try FileManager.default.copyItem(at: received.file, to: destURL)
            return Self(url: destURL)
        }
    }
}

struct IRPhotosTab: View {
    let session: IRSession
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @Environment(\.modelContext) private var modelContext

    // Upload state
    @State private var showingPhotoPicker = false
    @State private var showingFilePicker = false
    @State private var selectedPhotoItems: [PhotosPickerItem] = []
    @State private var isUploading = false
    @State private var uploadProgress = ""

    // Edit / Delete state
    @State private var photoToEdit: IRPhoto?
    @State private var photoToDelete: IRPhoto?

    // Thumbnail presigned URLs
    @State private var presignedURLCache: [String: String] = [:]
    /// Changing this token forces SwiftUI to recreate thumbnail views, resetting stale @State images.
    @State private var thumbnailReloadToken = UUID()

    // Photo viewer
    @State private var selectedIRPhotoForViewer: IRPhoto?

    // Error state
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var isUpdating = false

    private var linkedPhotos: [IRPhoto] {
        session.ir_photos
            .filter { !$0.is_deleted }
            .sorted(by: { $0.date_created > $1.date_created })
    }

    private var dateFormatter: DateFormatter {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy HH:mm"
        return f
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text(AppStrings.Sessions.irPhotoCount(linkedPhotos.count))
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()

                Menu {
                    // FLIR-IND requires raw bytes (Photo Library re-encodes and strips
                    // the embedded visual + radiometric APP1 metadata, breaking server
                    // extraction), so only the document picker is offered.
                    if session.photo_type != "FLIR-IND" {
                        Button {
                            showingPhotoPicker = true
                        } label: {
                            Label(AppStrings.Sessions.fromPhotos, systemImage: "photo.on.rectangle")
                        }
                    }
                    Button {
                        showingFilePicker = true
                    } label: {
                        Label(AppStrings.Sessions.fromFiles, systemImage: "folder")
                    }
                } label: {
                    Label(AppStrings.Sessions.uploadIRPhotos, systemImage: "icloud.and.arrow.up")
                        .font(.system(size: 16, weight: .medium))
                }
                .disabled(networkState.mode == .offline || !session.active)
            }
            .padding()

            Text(session.photo_type == "FLIR-IND"
                 ? AppStrings.Sessions.irPhotoFileNameNoteFlirInd
                 : AppStrings.Sessions.irPhotoFileNameNote)
                .font(.caption)
                .foregroundColor(.orange.opacity(0.8))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
                .padding(.top, -8)
                .padding(.bottom, 8)

            Divider()

            if linkedPhotos.isEmpty {
                emptyStateView
            } else {
                photoListView
            }
        }
        .photosPicker(isPresented: $showingPhotoPicker, selection: $selectedPhotoItems, maxSelectionCount: 20, matching: .images)
        .fileImporter(
            isPresented: $showingFilePicker,
            allowedContentTypes: [.image],
            allowsMultipleSelection: true
        ) { result in
            handleFileSelection(result)
        }
        .onChange(of: selectedPhotoItems) { _, newItems in
            guard !newItems.isEmpty else { return }
            Task { await handlePhotoPickerSelection(newItems) }
            selectedPhotoItems = []
        }
        .fullScreenCover(item: $selectedIRPhotoForViewer) { photo in
            IRFullImageView(
                irPhotos: linkedPhotos,
                initialPhoto: photo,
                isOnline: networkState.mode == .online,
                irPhotoPresignedURLs: presignedURLCache
            )
        }
        .sheet(item: $photoToEdit) { photo in
            IRPhotoEditSheet(
                irKey: photo.ir_photo_key,
                visKey: photo.visual_photo_key,
                isFlirInd: photo.ir_session?.photo_type == "FLIR-IND",
                onSave: { irKey, visKey in
                    savePhotoEdit(photo: photo, irKey: irKey, visKey: visKey)
                }
            )
        }
        .alert(
            AppStrings.Sessions.deleteIRPhoto,
            isPresented: Binding(
                get: { photoToDelete != nil },
                set: { if !$0 { photoToDelete = nil } }
            )
        ) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                photoToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let photo = photoToDelete {
                    deletePhoto(photo)
                }
                photoToDelete = nil
            }
        } message: {
            Text(AppStrings.Sessions.deleteIRPhotoConfirm)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage)
        }
        .overlay {
            if isUploading {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                VStack(spacing: 12) {
                    ProgressView()
                    Text(uploadProgress.isEmpty ? AppStrings.Sessions.uploadingIRPhotos : uploadProgress)
                        .font(.subheadline)
                }
                .padding()
                .background(Color(UIColor.systemBackground))
                .cornerRadius(10)
                .shadow(radius: 5)
            }
            if isUpdating {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.CommonExtra.updating)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .task(id: linkedPhotos.map(\.id)) {
            await loadPresignedURLs()
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Spacer()

            Image(systemName: "camera.metering.multispot")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text(AppStrings.Sessions.noIRPhotos)
                .font(.title3)
                .fontWeight(.medium)
                .foregroundColor(.primary)

            Text(AppStrings.Sessions.tapPlusToAddIRPhotos)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()
        }
    }

    // MARK: - Photo List

    private var photoListView: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(linkedPhotos) { photo in
                    IRPhotoFlatRow(
                        photo: photo,
                        sessionId: session.id,
                        irPresignedURL: presignedURLCache["\(photo.id.uuidString)_ir"],
                        visPresignedURL: presignedURLCache["\(photo.id.uuidString)_vis"],
                        isOnline: networkState.mode == .online,
                        dateFormatter: dateFormatter,
                        onTap: { selectedIRPhotoForViewer = photo },
                        onEdit: { photoToEdit = photo },
                        onDelete: { photoToDelete = photo },
                        onExtractVisual: { manualExtractVisual(for: photo) }
                    )
                    Divider()
                        .padding(.leading, 16)
                }
            }
            .id(thumbnailReloadToken)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    // MARK: - Presigned URL Loading

    private func loadPresignedURLs() async {
        guard networkState.mode == .online, !linkedPhotos.isEmpty else { return }

        let requests = linkedPhotos.map { photo in
            S3PresignedURLService.IRPhotoURLRequest(
                photoId: photo.id,
                sessionId: session.id,
                irPhotoKey: photo.ir_photo_key,
                visualPhotoKey: photo.visual_photo_key
            )
        }

        let urls = await S3PresignedURLService.shared.batchGetIRPhotoPresignedURLs(requests: requests)
        await MainActor.run {
            presignedURLCache = urls
        }
    }

    // MARK: - Upload: Photo Library

    // IR photos are uploaded untouched: FLIR-IND radiometric JPEGs embed a visual
    // image and thermal metadata in APP1 segments that re-encoding via
    // UIImage.jpegData(...) would strip, breaking server-side extraction. Keeping
    // the same behavior for FLIR-SEP preserves any radiometric data the backend
    // may consume in the future.

    private func handlePhotoPickerSelection(_ items: [PhotosPickerItem]) async {
        await MainActor.run {
            isUploading = true
            uploadProgress = ""
        }

        var imageDataItems: [(fileName: String, data: Data)] = []

        for (index, item) in items.enumerated() {
            await MainActor.run {
                uploadProgress = "Loading image \(index + 1)/\(items.count)..."
            }
            // Load as file to get original filename
            if let imageFile = try? await item.loadTransferable(type: IRPhotoFileTransferable.self) {
                guard let data = try? Data(contentsOf: imageFile.url) else {
                    AppLogger.log(.error, "Failed to read photo data \(index + 1)", category: .photo)
                    continue
                }
                // Validate file size before processing
                if data.count > Attachment.MAX_FILE_SIZE {
                    await MainActor.run {
                        isUploading = false
                        uploadProgress = ""
                        errorMessage = AppStrings.Sessions.fileTooLarge
                        showError = true
                    }
                    return
                }
                // Preserve original filename (including extension) so the server
                // sees the same bytes and filename the camera produced.
                let fileName = imageFile.url.lastPathComponent
                imageDataItems.append((fileName: fileName, data: data))
                // Clean up temp file now that data is in memory
                try? FileManager.default.removeItem(at: imageFile.url)
            } else {
                AppLogger.log(.error, "Failed to load photo \(index + 1)", category: .photo)
            }
        }

        await uploadImages(imageDataItems)
    }

    // MARK: - Upload: File Picker

    private func handleFileSelection(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard !urls.isEmpty else { return }

            if urls.count > 20 {
                errorMessage = "You can upload a maximum of 20 files at once."
                showError = true
                return
            }

            // Validate file sizes before processing
            for url in urls {
                let didStart = url.startAccessingSecurityScopedResource()
                defer { if didStart { url.stopAccessingSecurityScopedResource() } }

                if let attrs = try? FileManager.default.attributesOfItem(atPath: url.path),
                   let size = attrs[.size] as? Int64,
                   size > Attachment.MAX_FILE_SIZE {
                    errorMessage = AppStrings.Sessions.fileTooLarge
                    showError = true
                    return
                }
            }

            Task {
                await MainActor.run {
                    isUploading = true
                    uploadProgress = ""
                }

                var imageDataItems: [(fileName: String, data: Data)] = []

                for url in urls {
                    let didStart = url.startAccessingSecurityScopedResource()
                    defer { if didStart { url.stopAccessingSecurityScopedResource() } }

                    if let data = try? Data(contentsOf: url) {
                        let fileName = url.lastPathComponent
                        imageDataItems.append((fileName: fileName, data: data))
                    }
                }

                await uploadImages(imageDataItems)
            }

        case .failure(let error):
            errorMessage = "Failed to select files: \(error.localizedDescription)"
            showError = true
        }
    }

    // MARK: - Upload: S3 (batch presigned URLs + concurrent upload)

    private func uploadImages(_ imageDataItems: [(fileName: String, data: Data)]) async {
        guard !imageDataItems.isEmpty else {
            await MainActor.run { isUploading = false }
            return
        }

        // 1. Batch-fetch all presigned write URLs at once
        await MainActor.run { uploadProgress = "Preparing upload..." }

        let fileNames = imageDataItems.map(\.fileName)
        let presignedURLsByName: [String: S3PresignedURLResponse]
        do {
            presignedURLsByName = try await S3PresignedURLService.shared
                .batchGetIRPhotoUploadPresignedURLs(sessionId: session.id, fileNames: fileNames)
        } catch {
            AppLogger.log(.error, "Failed to get batch presigned URLs: \(error)", category: .photo)
            await MainActor.run {
                isUploading = false
                uploadProgress = ""
                errorMessage = "Failed to prepare upload: \(error.localizedDescription)"
                showError = true
            }
            return
        }

        // 2. Upload files concurrently in batches of 5
        let concurrentBatchSize = 5
        var successCount = 0
        var failCount = 0

        for batchStart in stride(from: 0, to: imageDataItems.count, by: concurrentBatchSize) {
            let batchEnd = min(batchStart + concurrentBatchSize, imageDataItems.count)
            let batch = Array(imageDataItems[batchStart..<batchEnd])

            await MainActor.run {
                if batch.count == 1 {
                    uploadProgress = "Uploading \(batchStart + 1) of \(imageDataItems.count)..."
                } else {
                    uploadProgress = "Uploading \(batchStart + 1)-\(batchEnd) of \(imageDataItems.count)..."
                }
            }

            await withTaskGroup(of: Bool.self) { group in
                for item in batch {
                    group.addTask {
                        guard let presignedResponse = presignedURLsByName[item.fileName],
                              let uploadURL = URL(string: presignedResponse.url) else {
                            return false
                        }

                        do {
                            var request = URLRequest(url: uploadURL)
                            request.httpMethod = "PUT"
                            request.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")

                            let (_, response) = try await URLSession.shared.upload(for: request, from: item.data)
                            guard let httpResponse = response as? HTTPURLResponse,
                                  httpResponse.statusCode == 200 else {
                                return false
                            }
                            return true
                        } catch {
                            AppLogger.log(.error, "IR photo upload failed for \(item.fileName): \(error)", category: .photo)
                            return false
                        }
                    }
                }

                for await success in group {
                    if success { successCount += 1 } else { failCount += 1 }
                }
            }
        }

        // 3. Evict stale image cache for uploaded keys, then refresh from server.
        // When an uploaded file matches an existing photo's key (e.g. vis_key=IMG_0010),
        // the S3 content changes but the key stays the same, so we must explicitly
        // clear cached thumbnails to force re-fetch with the new image data.
        if successCount > 0 {
            let uploadedBaseNames = Set(imageDataItems.map {
                URL(fileURLWithPath: $0.fileName).deletingPathExtension().lastPathComponent
            })
            await MainActor.run {
                uploadProgress = "Refreshing..."
                for photo in session.ir_photos {
                    if uploadedBaseNames.contains(photo.ir_photo_key) || uploadedBaseNames.contains(photo.visual_photo_key) {
                        let key = photo.id.uuidString
                        PhotoImageCache.shared.removeImage(forKey: "\(key)_ir")
                        PhotoImageCache.shared.removeImage(forKey: "\(key)_vis")
                        presignedURLCache.removeValue(forKey: "\(key)_ir")
                        presignedURLCache.removeValue(forKey: "\(key)_vis")
                    }
                }
            }
            await refreshSessionIRPhotos()

            // For FLIR-IND sessions, ask the backend to extract the embedded
            // visual image from each uploaded radiometric JPEG. Non-blocking:
            // failures never interrupt the upload flow (matches Android + web).
            if session.photo_type == "FLIR-IND" {
                await MainActor.run { uploadProgress = AppStrings.Sessions.extractingVisual }
                await extractVisualForUploadedPhotos(fileNames: imageDataItems.map(\.fileName))
            }

            await loadPresignedURLs()
            // Force SwiftUI to recreate thumbnail views so stale @State images are discarded
            await MainActor.run { thumbnailReloadToken = UUID() }
        }

        await MainActor.run {
            isUploading = false
            uploadProgress = ""
            if failCount > 0 {
                errorMessage = "\(failCount) of \(imageDataItems.count) photos failed to upload."
                showError = true
            }
        }
    }

    // MARK: - FLIR-IND Visual Image Extraction

    /// Match uploaded filenames to local IRPhoto records (by basename, ignoring
    /// extension) and ask the backend to extract the embedded visual image for each.
    /// Non-blocking: a failure here is logged but never surfaces to the user, because
    /// extraction is a best-effort post-processing step — users can retry manually
    /// from the row menu.
    private func extractVisualForUploadedPhotos(fileNames: [String]) async {
        let uploadedBaseNames = Set(fileNames.map {
            URL(fileURLWithPath: $0).deletingPathExtension().lastPathComponent
        })
        let photosToExtract = await MainActor.run {
            session.ir_photos.filter { photo in
                guard !photo.ir_photo_key.isEmpty else { return false }
                let keyBase = URL(fileURLWithPath: photo.ir_photo_key)
                    .deletingPathExtension().lastPathComponent
                return uploadedBaseNames.contains(keyBase)
                    || uploadedBaseNames.contains(photo.ir_photo_key)
            }
        }
        guard !photosToExtract.isEmpty else {
            AppLogger.log(.notice, "FLIR extract_visual: no matching photos found", category: .photo)
            return
        }

        do {
            _ = try await APIClient.shared.extractVisualFromIRPhotos(
                photoIds: photosToExtract.map(\.id)
            )
            // Pull the backend-populated visual_photo_key values back into SwiftData.
            await refreshSessionIRPhotos()
        } catch {
            AppLogger.log(.notice, "FLIR extract_visual error (non-blocking): \(error)", category: .photo)
        }
    }

    /// Manual retry trigger for a single IR photo. Surfaces failure via the error banner
    /// because the user explicitly asked for this work, unlike auto-extraction after upload.
    private func manualExtractVisual(for photo: IRPhoto) {
        guard networkState.mode == .online else { return }
        Task {
            await MainActor.run {
                isUploading = true
                uploadProgress = AppStrings.Sessions.extractingVisual
            }
            var extractionFailedMessage: String?
            do {
                let response = try await APIClient.shared.extractVisualFromIRPhotos(
                    photoIds: [photo.id]
                )
                let succeeded = (response.summary?.successful ?? 0) > 0
                await refreshSessionIRPhotos()
                await loadPresignedURLs()
                if !succeeded {
                    extractionFailedMessage = AppStrings.Sessions.extractVisualPhotoFailed
                }
            } catch {
                AppLogger.log(.error, "Manual FLIR extract_visual failed: \(error)", category: .photo)
                extractionFailedMessage = AppStrings.Sessions.extractVisualPhotoFailed
            }
            await MainActor.run {
                isUploading = false
                uploadProgress = ""
                if let message = extractionFailedMessage {
                    errorMessage = message
                    showError = true
                }
            }
        }
    }

    // MARK: - Edit

    private func savePhotoEdit(photo: IRPhoto, irKey: String, visKey: String) {
        // Drop only the presigned URL entries — the URL signs the old S3 path which
        // is now stale. Keep the in-memory image cache: cacheKey is photo-ID-based,
        // and the image bytes are unchanged by a rename, so the cached image stays
        // valid and visible while the new presigned URL is refetched.
        presignedURLCache.removeValue(forKey: "\(photo.id.uuidString)_ir")
        presignedURLCache.removeValue(forKey: "\(photo.id.uuidString)_vis")

        // FLIR-IND: visual key is server-managed. Rewrite it from the new IR base
        // so the visual file stays paired when the user renames the IR key.
        let finalVisKey: String
        if photo.ir_session?.photo_type == "FLIR-IND", !photo.visual_photo_key.isEmpty {
            let visExt = (photo.visual_photo_key as NSString).pathExtension
            let irBase = (irKey as NSString).deletingPathExtension
            finalVisKey = visExt.isEmpty ? "\(irBase)_visual" : "\(irBase)_visual.\(visExt)"
        } else {
            finalVisKey = visKey
        }

        photo.ir_photo_key = irKey
        photo.visual_photo_key = finalVisKey

        do {
            try modelContext.save()
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = "Failed to save: \(error.localizedDescription)"
            showError = true
            return
        }

        if networkState.mode == .online {
            Task {
                do {
                    _ = try await APIClient.shared.updateIRPhoto(photo)

                    // FLIR-IND: the visual file is server-extracted from the IR
                    // file and stored at "<ir_key_base>_visual.<ext>". The IR
                    // update endpoint renames the IR S3 object but does not
                    // re-extract the visual at the new path, so trigger
                    // extraction explicitly. Best-effort — manual extract menu
                    // is still available if this fails.
                    if photo.ir_session?.photo_type == "FLIR-IND", !photo.visual_photo_key.isEmpty {
                        _ = try? await APIClient.shared.extractVisualFromIRPhotos(photoIds: [photo.id])
                    }

                    // Brief delay before refetching presigned URLs: gives S3 time to
                    // propagate the rename so the new presigned URL resolves to the
                    // freshly renamed object instead of 404-ing.
                    try? await Task.sleep(for: .seconds(2))
                    await loadPresignedURLs()
                } catch {
                    await MainActor.run {
                        networkState.enqueue(SyncOp(target: .irPhoto, operation: .update, irPhoto: photo))
                    }
                }
            }
        } else {
            networkState.enqueue(SyncOp(target: .irPhoto, operation: .update, irPhoto: photo))
        }
    }

    // MARK: - Targeted Session Refresh

    /// Fetches only this session's IR photos from the server via GET /ir_session/{id}/full
    /// and upserts them into SwiftData. Much lighter than a full SLD refresh.
    private func refreshSessionIRPhotos() async {
        do {
            let response = try await APIClient.shared.fetchIRSessionFull(sessionId: session.id)
            let photoDTOs = response.data.photos ?? []

            await MainActor.run {
                let existingPhotoIds = Set(session.ir_photos.map(\.id))

                for dto in photoDTOs {
                    if existingPhotoIds.contains(dto.id) {
                        // Update existing photo
                        if let existing = session.ir_photos.first(where: { $0.id == dto.id }) {
                            let keysChanged = existing.ir_photo_key != dto.ir_photo_key || existing.visual_photo_key != dto.visual_photo_key

                            if keysChanged {
                                // Clear stale thumbnail cache for this photo since keys are changing
                                PhotoImageCache.shared.removeImage(forKey: "\(existing.id.uuidString)_ir")
                                PhotoImageCache.shared.removeImage(forKey: "\(existing.id.uuidString)_vis")
                                presignedURLCache.removeValue(forKey: "\(existing.id.uuidString)_ir")
                                presignedURLCache.removeValue(forKey: "\(existing.id.uuidString)_vis")
                            }

                            existing.ir_photo_key = dto.ir_photo_key
                            existing.visual_photo_key = dto.visual_photo_key
                            existing.is_deleted = dto.is_deleted
                            existing.isSynced = true
                        }
                    } else {
                        // New photo from server — find node and sld to create it
                        let fetchNode = FetchDescriptor<NodeV2>(predicate: #Predicate { $0.id == dto.node_id })
                        let fetchSld = FetchDescriptor<SLDV2>(predicate: #Predicate { $0.id == dto.sld_id })

                        guard let node = try? modelContext.fetch(fetchNode).first,
                              let sld = try? modelContext.fetch(fetchSld).first else {
                            continue
                        }

                        var issue: Issue? = nil
                        if let issueId = dto.issue_id {
                            let fetchIssue = FetchDescriptor<Issue>(predicate: #Predicate { $0.id == issueId })
                            issue = try? modelContext.fetch(fetchIssue).first
                        }

                        let newPhoto = IRPhoto(
                            id: dto.id,
                            ir_session: session,
                            node: node,
                            sld: sld,
                            visual_photo_key: dto.visual_photo_key,
                            ir_photo_key: dto.ir_photo_key,
                            date_created: dto.date_created,
                            is_deleted: dto.is_deleted,
                            issue: issue
                        )
                        newPhoto.isSynced = true
                        modelContext.insert(newPhoto)
                        session.ir_photos.append(newPhoto)
                    }
                }

                try? modelContext.save()
            }
        } catch {
            // Fall back to full SLD refresh if targeted refresh fails
            AppLogger.log(.notice, "Targeted session refresh failed, falling back to full SLD refresh: \(error)", category: .sync)
            do {
                try await SLDService.shared.refreshSLD(modelContext: modelContext)
            } catch {
                AppLogger.log(.error, "Full SLD refresh also failed: \(error)", category: .sync)
            }
        }
    }

    // MARK: - Delete

    private func deletePhoto(_ photo: IRPhoto) {
        PhotoImageCache.shared.removeImage(forKey: "\(photo.id.uuidString)_ir")
        PhotoImageCache.shared.removeImage(forKey: "\(photo.id.uuidString)_vis")
        presignedURLCache.removeValue(forKey: "\(photo.id.uuidString)_ir")
        presignedURLCache.removeValue(forKey: "\(photo.id.uuidString)_vis")
        IRPhotoService.deleteIRPhoto(photo, modelContext: modelContext)
    }
}

// MARK: - IR Photo Flat Row

struct IRPhotoFlatRow: View {
    let photo: IRPhoto
    let sessionId: UUID
    let irPresignedURL: String?
    let visPresignedURL: String?
    let isOnline: Bool
    let dateFormatter: DateFormatter
    let onTap: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onExtractVisual: () -> Void

    private var isFlirInd: Bool {
        photo.ir_session?.photo_type == "FLIR-IND"
    }

    var body: some View {
        HStack(spacing: 10) {
            // Tappable area: thumbnails + metadata → opens fullscreen viewer
            Button(action: onTap) {
                HStack(spacing: 10) {
                    // Cached thumbnails with offline support
                    HStack(spacing: 5) {
                        VStack(spacing: 2) {
                            IRPhotoThumbnail(
                                photoKey: photo.ir_photo_key,
                                cacheKey: "\(photo.id.uuidString)_ir",
                                sessionId: sessionId,
                                label: "IR",
                                labelColor: .orange,
                                isOnline: isOnline,
                                size: 65,
                                presignedURL: irPresignedURL
                            )
                            Text(photo.ir_photo_key)
                                .font(.system(size: 9))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                                .truncationMode(.tail)
                                .frame(width: 65)
                        }

                        VStack(spacing: 2) {
                            IRPhotoThumbnail(
                                photoKey: photo.visual_photo_key,
                                cacheKey: "\(photo.id.uuidString)_vis",
                                sessionId: sessionId,
                                label: "VIS",
                                labelColor: .blue,
                                isOnline: isOnline,
                                size: 65,
                                presignedURL: visPresignedURL
                            )
                            Text(photo.visual_photo_key)
                                .font(.system(size: 9))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                                .truncationMode(.tail)
                                .frame(width: 65)
                        }
                    }

                    // Metadata
                    VStack(alignment: .leading, spacing: 3) {
                        Text(photo.node.label)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        if let location = photo.node.location, !location.isEmpty {
                            Text(location)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }

                        if let issue = photo.issue, let issueTitle = issue.title {
                            Text(issueTitle)
                                .font(.caption)
                                .foregroundColor(.orange)
                                .lineLimit(1)
                        }

                        Text(dateFormatter.string(from: photo.date_created))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            // Actions menu
            Menu {
                Button {
                    onEdit()
                } label: {
                    Label(AppStrings.Sessions.editIRPhoto, systemImage: "pencil")
                }
                if isFlirInd, !photo.ir_photo_key.isEmpty {
                    Button {
                        onExtractVisual()
                    } label: {
                        Label(AppStrings.Sessions.extractVisualPhoto, systemImage: "sparkles")
                    }
                    .disabled(!isOnline)
                }
                Button(role: .destructive) {
                    onDelete()
                } label: {
                    Label(AppStrings.Common.delete, systemImage: "trash")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .font(.system(size: 20))
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
    }
}

// MARK: - IR Photo Edit Sheet

struct IRPhotoEditSheet: View {
    @State var irKey: String
    @State var visKey: String
    let isFlirInd: Bool
    let onSave: (String, String) -> Void
    @Environment(\.dismiss) private var dismiss

    private var isSaveDisabled: Bool {
        if isFlirInd {
            return irKey.trimmingCharacters(in: .whitespaces).isEmpty
        }
        return irKey.trimmingCharacters(in: .whitespaces).isEmpty &&
               visKey.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(AppStrings.Sessions.irPhotoKey, text: $irKey)
                    if isFlirInd {
                        HStack {
                            Image(systemName: "photo")
                                .foregroundColor(.gray)
                            Text(AppStrings.Assets.visualPhotoNotRequired)
                                .foregroundColor(.gray)
                        }
                    } else {
                        TextField(AppStrings.Sessions.visualPhotoKey, text: $visKey)
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.editIRPhoto)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.save) {
                        onSave(irKey, visKey)
                        dismiss()
                    }
                    .disabled(isSaveDisabled)
                }
            }
        }
    }
}

// MARK: - Hierarchical View Components

struct LocationSection: View {
    let location: String
    let nodeGroups: [(node: NodeV2, photos: [IRPhoto])]
    let sessionId: UUID
    let isExpanded: Bool
    @Binding var expandedNodes: Set<UUID>
    let isOnline: Bool
    let onToggleExpand: () -> Void
    let onRemovePhoto: (IRPhoto) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Location Header with background
            Button(action: onToggleExpand) {
                HStack {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(width: 20)

                    Image(systemName: "location.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Text(location)
                        .font(.headline)
                        .foregroundColor(.primary)

                    Spacer()

                    Text(AppStrings.Sessions.assetsCount(nodeGroups.count))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(Color(UIColor.systemGray5))
                .cornerRadius(10)
            }
            .buttonStyle(.plain)

            if isExpanded {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(nodeGroups, id: \.node.id) { nodeGroup in
                        NodeSection(
                            node: nodeGroup.node,
                            photos: nodeGroup.photos,
                            sessionId: sessionId,
                            isExpanded: expandedNodes.contains(nodeGroup.node.id),
                            isOnline: isOnline,
                            onToggleExpand: {
                                if expandedNodes.contains(nodeGroup.node.id) {
                                    expandedNodes.remove(nodeGroup.node.id)
                                } else {
                                    expandedNodes.insert(nodeGroup.node.id)
                                }
                            },
                            onRemovePhoto: onRemovePhoto
                        )
                    }
                }
                .padding(.leading, 24)
            }
        }
    }
}

struct NodeSection: View {
    let node: NodeV2
    let photos: [IRPhoto]
    let sessionId: UUID
    let isExpanded: Bool
    let isOnline: Bool
    let onToggleExpand: () -> Void
    let onRemovePhoto: (IRPhoto) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Node Header - taller row
            Button(action: onToggleExpand) {
                HStack {
                    Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(width: 20)

                    Image(systemName: "cube")
                        .font(.body)
                        .foregroundColor(.secondary)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(node.label)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)

                        if !node.type.isEmpty {
                            Text(node.type)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Spacer()

                    Text(AppStrings.Sessions.setsCount(photos.count))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(8)
            }
            .buttonStyle(.plain)

            if isExpanded {
                // Text-based photo list with dividers
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                        VStack(spacing: 0) {
                            IRPhotoTextRow(
                                photo: photo,
                                sessionId: sessionId,
                                isOnline: isOnline,
                                onRemove: { onRemovePhoto(photo) }
                            )

                            // Add divider between items (not after last item)
                            if index < photos.count - 1 {
                                Divider()
                                    .background(Color.gray.opacity(0.2))
                                    .padding(.leading, 12)
                            }
                        }
                    }
                }
                .padding(.leading, 24)
                .padding(.top, 4)
            }
        }
    }
}

// MARK: - Text-based IR Photo Row
struct IRPhotoTextRow: View {
    let photo: IRPhoto
    let sessionId: UUID
    let isOnline: Bool
    let onRemove: () -> Void

    @State private var showingRemoveConfirmation = false

    var body: some View {
        HStack(spacing: 12) {
            // Photo info
            HStack(spacing: 8) {
                // Visual photo key
                if !photo.visual_photo_key.isEmpty {
                    Label(photo.visual_photo_key, systemImage: "photo")
                        .font(.caption)
                        .foregroundColor(.blue)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }

                // IR photo key
                Label(photo.ir_photo_key, systemImage: "camera.filters")
                    .font(.caption)
                    .foregroundColor(.orange)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }

            Spacer()

            // Remove button
            Button(action: { showingRemoveConfirmation = true }) {
                Image(systemName: "trash.circle")
                    .font(.body)
                    .foregroundColor(.red.opacity(0.7))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .confirmationDialog(
            AppStrings.Sessions.removeIRPhotoSet,
            isPresented: $showingRemoveConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.CommonExtra.remove, role: .destructive) {
                onRemove()
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        } message: {
            Text(AppStrings.Sessions.removeIRPhotoMessage)
        }
    }
}

struct CompactIRPhotoCard: View {
    let photo: IRPhoto
    let sessionId: UUID
    let isOnline: Bool
    let onRemove: () -> Void

    @State private var showingRemoveConfirmation = false
    @State private var visualPresignedURL: URL?
    @State private var irPresignedURL: URL?

    private func getIRPhotoKey(for key: String) -> String {
        let sessionIdLowercase = sessionId.uuidString.lowercased()
        // Add "photo_" prefix for IR photos: photo_{session_id}/{photo_key}
        return "photo_\(sessionIdLowercase)/\(key)"
    }

    private func loadPresignedURLs() async {
        guard isOnline else { return }

        // Load visual photo presigned URL
        do {
            let visualKey = getIRPhotoKey(for: photo.visual_photo_key)
            let visualResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: visualKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                visualPresignedURL = URL(string: visualResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned visual URL: \(error)", category: .photo)
        }

        // Load IR photo presigned URL
        do {
            let irKey = getIRPhotoKey(for: photo.ir_photo_key)
            let irResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: irKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                irPresignedURL = URL(string: irResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned IR URL: \(error)", category: .photo)
        }
    }

    var body: some View {
        VStack(spacing: 4) {
            // Photos
            HStack(spacing: 4) {
                PhotoThumbnail(
                    url: visualPresignedURL,
                    label: "VIS",
                    labelColor: .blue,
                    isOnline: isOnline
                )
                .frame(width: 70, height: 70)

                PhotoThumbnail(
                    url: irPresignedURL,
                    label: "IR",
                    labelColor: .orange,
                    isOnline: isOnline
                )
                .frame(width: 70, height: 70)
            }
            .task {
                await loadPresignedURLs()
            }

            // Photo keys
            Text(photo.visual_photo_key)
                .font(.system(size: 8))
                .foregroundColor(.secondary)
                .lineLimit(1)
                .truncationMode(.middle)

            // Action button
            Button(action: { showingRemoveConfirmation = true }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }
            .padding(.top, 2)
        }
        .padding(6)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(8)
        .shadow(radius: 1)
        .confirmationDialog(
            AppStrings.Sessions.removePhotoSet,
            isPresented: $showingRemoveConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.CommonExtra.remove, role: .destructive) {
                onRemove()
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        }
    }
}

// MARK: - Photo Card with Remove Button
struct IRPhotoCard: View {
    let photo: IRPhoto
    let sessionId: UUID
    let isOnline: Bool
    let onRemove: () -> Void

    @State private var showingRemoveConfirmation = false
    @State private var visualPresignedURL: URL?
    @State private var irPresignedURL: URL?

    private func getIRPhotoKey(for key: String) -> String {
        let sessionIdLowercase = sessionId.uuidString.lowercased()
        // Add "photo_" prefix for IR photos: photo_{session_id}/{photo_key}
        return "photo_\(sessionIdLowercase)/\(key)"
    }

    private func loadPresignedURLs() async {
        guard isOnline else { return }

        // Load visual photo presigned URL
        do {
            let visualKey = getIRPhotoKey(for: photo.visual_photo_key)
            let visualResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: visualKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                visualPresignedURL = URL(string: visualResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned visual URL: \(error)", category: .photo)
        }

        // Load IR photo presigned URL
        do {
            let irKey = getIRPhotoKey(for: photo.ir_photo_key)
            let irResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: irKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                irPresignedURL = URL(string: irResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned IR URL: \(error)", category: .photo)
        }
    }

    var body: some View {
        VStack(spacing: 8) {
            // Header with node label and remove button
            HStack {
                Text(photo.node.label)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                
                Spacer()
                
                Button(action: { showingRemoveConfirmation = true }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                        .font(.system(size: 16))
                }
            }
            
            // Side-by-side thumbnails
            HStack(spacing: 8) {
                PhotoThumbnail(
                    url: visualPresignedURL,
                    label: "VIS",
                    labelColor: .blue,
                    isOnline: isOnline
                )

                PhotoThumbnail(
                    url: irPresignedURL,
                    label: "IR",
                    labelColor: .orange,
                    isOnline: isOnline
                )
            }
            .task {
                await loadPresignedURLs()
            }
            
            // File info
            VStack(spacing: 2) {
                Text(photo.visual_photo_key)
                    .font(.system(size: 9))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
                
                Text(photo.date_created, style: .relative)
                    .font(.system(size: 9))
                    .foregroundColor(.secondary)
            }
        }
        .padding(8)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
        .confirmationDialog(
            AppStrings.Sessions.removePhotoFromSession,
            isPresented: $showingRemoveConfirmation,
            titleVisibility: .visible
        ) {
            Button(AppStrings.CommonExtra.remove, role: .destructive) {
                onRemove()
            }
            Button(AppStrings.Common.cancel, role: .cancel) { }
        } message: {
            Text(AppStrings.Sessions.removePhotoMessage)
        }
    }
}

// MARK: - Selectable Photo Card
struct IRPhotoSelectableCard: View {
    let photo: IRPhoto
    let sessionId: UUID
    let isOnline: Bool
    let isSelected: Bool
    let onToggle: () -> Void

    @State private var visualPresignedURL: URL?
    @State private var irPresignedURL: URL?

    private func getIRPhotoKey(for key: String) -> String {
        let sessionIdLowercase = sessionId.uuidString.lowercased()
        // Add "photo_" prefix for IR photos: photo_{session_id}/{photo_key}
        return "photo_\(sessionIdLowercase)/\(key)"
    }

    private func loadPresignedURLs() async {
        guard isOnline else { return }

        // Load visual photo presigned URL
        do {
            let visualKey = getIRPhotoKey(for: photo.visual_photo_key)
            let visualResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: visualKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                visualPresignedURL = URL(string: visualResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned visual URL: \(error)", category: .photo)
        }

        // Load IR photo presigned URL
        do {
            let irKey = getIRPhotoKey(for: photo.ir_photo_key)
            let irResponse = try await S3PresignedURLService.shared.getPresignedDownloadURL(
                key: irKey,
                photoType: "ir_metadata"
            )
            await MainActor.run {
                irPresignedURL = URL(string: irResponse.url)
            }
        } catch {
            AppLogger.log(.notice, "Failed to get presigned IR URL: \(error)", category: .photo)
        }
    }

    var body: some View {
        Button(action: onToggle) {
            VStack(spacing: 8) {
                // Header with node label and selection indicator
                HStack {
                    Text(photo.node.label)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    
                    Spacer()
                    
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .foregroundColor(isSelected ? .blue : .gray)
                        .font(.system(size: 20))
                }
                
                // Side-by-side thumbnails
                HStack(spacing: 8) {
                    PhotoThumbnail(
                        url: visualPresignedURL,
                        label: "VIS",
                        labelColor: .blue,
                        isOnline: isOnline
                    )

                    PhotoThumbnail(
                        url: irPresignedURL,
                        label: "IR",
                        labelColor: .orange,
                        isOnline: isOnline
                    )
                }
                .task {
                    await loadPresignedURLs()
                }
                
                // File info
                VStack(spacing: 2) {
                    Text(photo.visual_photo_key)
                        .font(.system(size: 9))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                    
                    Text(photo.date_created, style: .relative)
                        .font(.system(size: 9))
                        .foregroundColor(.secondary)
                }
            }
            .padding(8)
            .background(Color(UIColor.systemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
            )
            .shadow(radius: isSelected ? 4 : 2)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Photo Thumbnail Component
struct PhotoThumbnail: View {
    let url: URL?
    let label: String
    let labelColor: Color
    let isOnline: Bool
    
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.systemGray6))
                .frame(width: 80, height: 80)
            
            if isOnline {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .scaleEffect(0.5)
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 80, height: 80)
                            .clipped()
                            .cornerRadius(8)
                    case .failure(_):
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                            .font(.title3)
                    @unknown default:
                        EmptyView()
                    }
                }
            } else {
                VStack(spacing: 4) {
                    Image(systemName: "wifi.slash")
                        .foregroundColor(.gray)
                        .font(.title3)
                    Text(AppStrings.CommonExtra.offline)
                        .font(.system(size: 9))
                        .foregroundColor(.gray)
                }
            }
            
            // Label overlay
            VStack {
                Spacer()
                HStack {
                    Text(label)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(labelColor.opacity(0.8))
                        .cornerRadius(4)
                    Spacer()
                }
                .padding(4)
            }
        }
        .frame(width: 80, height: 80)
    }
}

// MARK: - Add Photos View
struct AddPhotosView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    
    let session: IRSession
    @Binding var isUpdating: Bool
    
    @State private var selectedPhotos = Set<IRPhoto>()
    @State private var showError = false
    @State private var errorMessage = ""
    
    // Query all photos in the same SLD that aren't already linked
    @Query private var availablePhotos: [IRPhoto]
    
    init(session: IRSession, isUpdating: Binding<Bool>) {
        self.session = session
        self._isUpdating = isUpdating
        
        let sldId = session.sld.id
        let sessionId = session.id
        
        _availablePhotos = Query(
            filter: #Predicate<IRPhoto> { photo in
                photo.sld.id == sldId &&
                !photo.is_deleted &&
                (photo.ir_session == nil || photo.ir_session?.id != sessionId)
            },
            sort: [SortDescriptor(\IRPhoto.date_created, order: .reverse)]
        )
    }
    
    var body: some View {
        NavigationView {
            VStack {
                if availablePhotos.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Sessions.noAvailablePhotos,
                        systemImage: "photo",
                        description: Text(AppStrings.Sessions.allPhotosLinked)
                    )
                } else {
                    List(availablePhotos) { photo in
                        PhotoSelectionRow(
                            photo: photo,
                            isSelected: selectedPhotos.contains(photo),
                            onToggle: {
                                if selectedPhotos.contains(photo) {
                                    selectedPhotos.remove(photo)
                                } else {
                                    selectedPhotos.insert(photo)
                                }
                            }
                        )
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.addPhotos)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.CommonExtra.addCount(selectedPhotos.count)) {
                        Task {
                            await addSelectedPhotos()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedPhotos.isEmpty)
                }
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage)
        }
    }

    private func addSelectedPhotos() async {
        await MainActor.run { isUpdating = true }
        dismiss()
        
        do {
            // Add all selected photos to the session
            for photo in selectedPhotos {
                session.ir_photos.append(photo)
                photo.ir_session = session
            }
            
            try modelContext.save()
            
            // Sync if online
            if networkState.mode == .online {
                for photo in selectedPhotos {
                    do {
                        _ = try await APIClient.shared.updateIRPhoto(photo)
                    } catch {
                        // Queue failed updates
                        networkState.enqueue(SyncOp(
                            target: .irPhoto,
                            operation: .update,
                            irPhoto: photo
                        ))
                    }
                }
            } else {
                // Queue all for sync when online
                for photo in selectedPhotos {
                    networkState.enqueue(SyncOp(
                        target: .irPhoto,
                        operation: .update,
                        irPhoto: photo
                    ))
                }
            }
            
            await MainActor.run { isUpdating = false }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            await MainActor.run {
                isUpdating = false
                errorMessage = "Failed to add photos: \(error.localizedDescription)"
                showError = true
            }
        }
    }
}

// MARK: - Simple Photo Selection Row
struct PhotoSelectionRow: View {
    let photo: IRPhoto
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(photo.node.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.primary)

                    HStack {
                        Text(photo.visual_photo_key)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)

                        Text("•")
                            .foregroundColor(.secondary)

                        Text(photo.date_created, style: .relative)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .blue : .gray)
                    .font(.title2)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - IR Photo Addition View
struct IRPhotoAdditionView: View {
    let session: IRSession
    let onComplete: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    @State private var selectedOption = 0 // 0 = Existing Asset, 1 = New Asset
    @State private var showingAssetSelector = false
    @State private var showingAddAsset = false
    @State private var selectedNode: NodeV2?

    // IR Photo creation states
    @State private var stagedIRPhotos: [IRPhoto] = []
    @State private var visualPhotoKey = ""
    @State private var irPhotoKey = ""
    @State private var lastUsedVisualKey: String? = nil
    @State private var lastUsedIRKey: String? = nil

    @Query private var availableNodes: [NodeV2]

    private var sldNodes: [NodeV2] {
        availableNodes.filter { node in
            !node.is_deleted && node.sld?.id == session.sld.id
        }.sorted { $0.label < $1.label }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                // Option Selector
                Picker("", selection: $selectedOption) {
                    Text(AppStrings.CommonExtra.existingAsset).tag(0)
                    Text(AppStrings.Assets.newAsset).tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                if selectedOption == 0 {
                    // Existing Asset Option
                    VStack(spacing: 16) {
                        Text(AppStrings.Sessions.selectAssetForIR)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal)

                        Button(action: {
                            showingAssetSelector = true
                        }) {
                            HStack {
                                if let node = selectedNode {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(node.label)
                                            .font(.body)
                                            .foregroundColor(.primary)
                                        if let location = node.location {
                                            Text(location)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                } else {
                                    Text(AppStrings.CommonExtra.selectAsset)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(10)
                        }
                        .padding(.horizontal)

                        if selectedNode != nil {
                            // IR Photo creation section - matching AddAssetView style
                            VStack(spacing: 16) {
                                SectionHeader(title: AppStrings.Assets.infraredPhotos, systemImage: "camera.filters")

                                // Session Display
                                VStack(spacing: 12) {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            HStack {
                                                ActiveSessionBadge()
                                                Text(session.name)
                                                    .font(.subheadline)
                                                    .fontWeight(.medium)
                                            }
                                            HStack(spacing: 8) {
                                                Text(AppStrings.Sessions.typeLabel(session.photo_type))
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)

                                                if session.photo_type == "FLIR-IND" {
                                                    Label(AppStrings.Assets.irOnly, systemImage: "info.circle")
                                                        .font(.caption2)
                                                        .foregroundColor(.blue)
                                                }
                                            }
                                        }

                                        Spacer()
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(Color(.systemGray6))
                                    .cornerRadius(10)

                                    // Input fields
                                    VStack(spacing: 12) {
                                        // Visual Photo field - disabled for FLIR-IND
                                        if session.photo_type == "FLIR-IND" {
                                            // Show disabled state for FLIR-IND
                                            VStack(alignment: .leading, spacing: 8) {
                                                HStack {
                                                    Image(systemName: "photo")
                                                        .foregroundColor(.gray)
                                                    Text(AppStrings.Assets.visualPhotoNotRequired)
                                                        .foregroundColor(.gray)
                                                }
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 12)
                                                .background(Color(.systemGray6).opacity(0.5))
                                                .cornerRadius(10)
                                            }
                                        } else {
                                            ModernTextField(
                                                title: AppStrings.Assets.visualPhotoFilename,
                                                text: $visualPhotoKey,
                                                icon: "photo"
                                            )
                                        }

                                        ModernTextField(
                                            title: AppStrings.Assets.irPhotoFilename,
                                            text: $irPhotoKey,
                                            icon: "camera.filters"
                                        )

                                        Button(action: addIRPhotoPair) {
                                            Label(AppStrings.Assets.addIRPhotoPair, systemImage: "plus.circle.fill")
                                                .frame(maxWidth: .infinity)
                                                .padding(.vertical, 12)
                                                .background(canAddIRPhoto ? Color.blue : Color.gray)
                                                .foregroundColor(.white)
                                                .cornerRadius(10)
                                        }
                                        .disabled(!canAddIRPhoto)
                                    }

                                    // Staged IR photos
                                    if !stagedIRPhotos.isEmpty {
                                        VStack(alignment: .leading, spacing: 8) {
                                            Text(AppStrings.Assets.newIRPhotos)
                                                .font(.caption)
                                                .foregroundColor(.secondary)

                                            ForEach(stagedIRPhotos) { irPhoto in
                                                IRPhotoRowForAdd(irPhoto: irPhoto, isStaged: true) {
                                                    stagedIRPhotos.removeAll { $0.id == irPhoto.id }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            .padding(.horizontal)

                            Button(action: {
                                proceedWithExistingAsset()
                            }) {
                                Text(AppStrings.Sessions.saveIRPhotos)
                                    .font(.headline)
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(!stagedIRPhotos.isEmpty ? Color.blue : Color.gray)
                                    .cornerRadius(10)
                            }
                            .disabled(stagedIRPhotos.isEmpty)
                            .padding(.horizontal)
                        }
                    }
                } else {
                    // New Asset Option
                    VStack(spacing: 16) {
                        Text(AppStrings.Sessions.createAssetWithIR)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal)

                        Button(action: {
                            showingAddAsset = true
                        }) {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                                Text(AppStrings.CommonExtra.createNewAsset)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Spacer()
                            }
                            .padding()
                            .background(Color(UIColor.systemGray6))
                            .cornerRadius(10)
                        }
                        .padding(.horizontal)
                    }
                }

                Spacer()
            }
            .navigationTitle(AppStrings.Sessions.addIRPhotosNav)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
            }
            .onAppear {
                setupInitialPhotoKeys()
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .sheet(isPresented: $showingAssetSelector) {
            AssetSelectorView(
                nodes: sldNodes,
                selectedNode: $selectedNode,
                onSelect: { node in
                    selectedNode = node
                    showingAssetSelector = false
                }
            )
        }
        .fullScreenCover(isPresented: $showingAddAsset) {
            AddAssetViewV2(
                availableLocations: Array(Set(sldNodes.compactMap(\.location))).sorted(),
                availableNodeClasses: {
                    let query = FetchDescriptor<NodeClass>(
                        predicate: #Predicate { nodeClass in
                            !nodeClass.is_deleted
                        }
                    )
                    return (try? modelContext.fetch(query)) ?? []
                }(),
                sld: session.sld,
                onSave: { node, photos, irPhotos in
                    // CRITICAL: Explicitly set the SLD relationship before insertion
                    node.sld = session.sld

                    // Insert into model context FIRST
                    modelContext.insert(node)
                    photos.forEach { modelContext.insert($0) }

                    // Add node to diagram's nodes array (after insertion)
                    if !session.sld.nodes.contains(where: { $0.id == node.id }) {
                        session.sld.nodes.append(node)
                    }

                    // Link IR photos to session
                    irPhotos.forEach { irPhoto in
                        irPhoto.ir_session = session
                        session.ir_photos.append(irPhoto)
                        modelContext.insert(irPhoto)
                    }

                    // Save context
                    do {
                        try modelContext.save()
                    } catch {
                        AppLogger.log(.error, "Failed to save: \(error)", category: .photo)
                    }

                    showingAddAsset = false
                    onComplete()
                    dismiss()

                    // Handle full service operations in background
                    Task {
                        await NodeService.createNewNodeWithPhotosAndIR(
                            node: node,
                            photos: photos,
                            irPhotos: irPhotos,
                            networkState: networkState,
                            modelContext: modelContext
                        )
                    }
                },
                onCancel: {
                    showingAddAsset = false
                }
            )
            .environmentObject(appState)
            .environmentObject(networkState)
        }
    }

    // Helper computed properties
    private var canAddIRPhoto: Bool {
        switch session.photo_type {
        case "FLIR-SEP", "FLIR":
            // Both visual and IR required
            return !visualPhotoKey.isEmpty && !irPhotoKey.isEmpty
        case "FLIR-IND":
            // Only IR required
            return !irPhotoKey.isEmpty
        default:
            // Default to requiring IR at minimum
            return !irPhotoKey.isEmpty
        }
    }

    private func setupInitialPhotoKeys() {
        // Use the IRPhotoService to get properly incremented keys based on session type
        let keys = IRPhotoService.setupDefaultPhotoKeys(
            for: session,
            lastVisualKey: lastUsedVisualKey,
            lastIRKey: lastUsedIRKey
        )

        visualPhotoKey = keys.visualKey
        irPhotoKey = keys.irKey
    }

    private func addIRPhotoPair() {
        guard let node = selectedNode else { return }

        // Determine visual photo key based on session type
        let visualKey: String
        switch session.photo_type {
        case "FLIR-IND":
            visualKey = ""  // No visual photo for FLIR-IND
        case "FLIR-SEP", "FLIR":
            visualKey = visualPhotoKey
        default:
            visualKey = visualPhotoKey
        }

        // Create IR photo
        let irPhoto = IRPhoto(
            id: UUID(),
            ir_session: session,
            node: node,
            sld: session.sld,
            visual_photo_key: visualKey,
            ir_photo_key: irPhotoKey,
            date_created: Date()
        )

        stagedIRPhotos.append(irPhoto)

        // Track last used keys locally based on session type
        switch session.photo_type {
        case "FLIR-SEP", "FLIR":
            lastUsedVisualKey = visualPhotoKey
            lastUsedIRKey = irPhotoKey
        case "FLIR-IND":
            // Only track IR key for FLIR-IND
            lastUsedIRKey = irPhotoKey
        default:
            lastUsedIRKey = irPhotoKey
        }

        // Set up next keys
        setupInitialPhotoKeys()
    }

    private func proceedWithExistingAsset() {
        guard selectedNode != nil else { return }

        // Save all staged IR photos
        for irPhoto in stagedIRPhotos {
            // Link to session
            session.ir_photos.append(irPhoto)
            modelContext.insert(irPhoto)
        }

        // Save context
        do {
            try modelContext.save()

            // Update global state with last used keys
            if let lastVisual = lastUsedVisualKey {
                appState.setLastCreatedVisualPhotoKey(lastVisual)
            }
            if let lastIR = lastUsedIRKey {
                appState.setLastCreatedIRPhotoKey(lastIR)
            }

            // Sync if online
            if networkState.mode == .online {
                Task {
                    for irPhoto in stagedIRPhotos {
                        do {
                            _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                        } catch {
                            networkState.enqueue(SyncOp(
                                target: .irPhoto,
                                operation: .create,
                                irPhoto: irPhoto
                            ))
                        }
                    }
                }
            } else {
                // Queue for sync
                for irPhoto in stagedIRPhotos {
                    networkState.enqueue(SyncOp(
                        target: .irPhoto,
                        operation: .create,
                        irPhoto: irPhoto
                    ))
                }
            }

            onComplete()
            dismiss()
        } catch {
            AppLogger.log(.error, "Failed to save IR photos: \(error)", category: .photo)
        }
    }
}

// MARK: - Asset Selector View
struct AssetSelectorView: View {
    let nodes: [NodeV2]
    @Binding var selectedNode: NodeV2?
    let onSelect: (NodeV2) -> Void

    @State private var searchText = ""
    @Environment(\.dismiss) private var dismiss

    private var filteredNodes: [NodeV2] {
        if searchText.isEmpty {
            return nodes
        } else {
            return nodes.filter { node in
                node.label.localizedCaseInsensitiveContains(searchText) ||
                (node.location?.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }
    }

    var body: some View {
        NavigationView {
            List {
                ForEach(filteredNodes) { node in
                    Button(action: {
                        onSelect(node)
                    }) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(node.label)
                                    .font(.body)
                                    .foregroundColor(.primary)
                                if let location = node.location {
                                    Text(location)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            if selectedNode?.id == node.id {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
            }
            .searchable(text: $searchText, prompt: AppStrings.CommonExtra.searchAssets)
            .navigationTitle(AppStrings.CommonExtra.selectAsset)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
