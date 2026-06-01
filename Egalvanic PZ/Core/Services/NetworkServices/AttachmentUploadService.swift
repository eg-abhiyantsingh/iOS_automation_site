//
//  AttachmentUploadService.swift
//  Egalvanic PZ
//
//  Service for handling attachment uploads to S3 and creating attachment records.
//

import Foundation
import SwiftData

/// Service responsible for handling attachment uploads to S3
@MainActor
final class AttachmentUploadService: ObservableObject {
    private let api = APIClient.shared
    private var modelContext: ModelContext?

    /// Upload progress (0.0 to 1.0)
    @Published var uploadProgress: Double = 0.0

    /// Whether an upload is currently in progress
    @Published var isUploading: Bool = false

    /// Current upload task for cancellation
    private var currentUploadTask: Task<Void, Error>?

    init(modelContext: ModelContext? = nil) {
        self.modelContext = modelContext
    }

    /// Set or update the model context
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    // MARK: - Main Upload Flow

    /// Upload an attachment to S3 and create the record
    /// - Parameters:
    ///   - attachment: The attachment to upload
    ///   - nodeIds: Optional list of node IDs to map the attachment to
    /// - Returns: The updated attachment with S3 key
    func uploadAttachment(_ attachment: Attachment, nodeIds: [UUID] = []) async throws {
        guard !isUploading else {
            throw AttachmentUploadError.uploadInProgress
        }

        isUploading = true
        uploadProgress = 0.0

        defer {
            isUploading = false
        }

        // Validate local file exists
        guard let localFilePath = attachment.localFilePath else {
            throw AttachmentUploadError.fileNotFound("No local file path for attachment")
        }

        let fileURL = URL(fileURLWithPath: localFilePath)
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            throw AttachmentUploadError.fileNotFound(localFilePath)
        }

        // Validate file size
        let fileAttributes = try FileManager.default.attributesOfItem(atPath: fileURL.path)
        let fileSize = fileAttributes[.size] as? Int64 ?? 0
        guard fileSize <= Attachment.MAX_FILE_SIZE else {
            throw AttachmentUploadError.fileTooLarge(fileSize)
        }

        // Load file data
        let data = try Data(contentsOf: fileURL)

        // Step 1: Get presigned upload URL
        uploadProgress = 0.1
        let presignedResponse = try await getPresignedUploadURL(
            filename: attachment.filename,
            fileSize: attachment.fileSize
        )

        // Update attachment with S3 key
        attachment.key = presignedResponse.key

        // Step 2: Upload to S3
        uploadProgress = 0.2
        guard let presignedURL = URL(string: presignedResponse.url) else {
            throw AttachmentUploadError.invalidPresignedURL
        }

        try await uploadToS3(data: data, presignedURL: presignedURL, contentType: attachment.contentType)
        uploadProgress = 0.7

        // Step 3: Create attachment record in backend
        try await createAttachmentRecord(attachment)
        uploadProgress = 0.8

        // Step 4: Create node mappings if any
        if !nodeIds.isEmpty {
            try await createNodeMappings(attachmentId: attachment.id, nodeIds: nodeIds)
        }
        uploadProgress = 0.9

        // Step 5: Mark as synced and clean up local file
        attachment.uploadNeeded = false
        attachment.modifiedAt = Date()

        // Delete local file after successful upload
        try? FileManager.default.removeItem(at: fileURL)
        attachment.localFilePath = nil

        // Save changes
        if let context = modelContext {
            do {
                try context.save()
                slog("Saved attachment uploadNeeded=false", category: .sync, data: [
                    "attachment_id": attachment.id.uuidString,
                    "filename": attachment.filename
                ])
            } catch {
                slog("Failed to save attachment after upload", category: .sync, level: .error, data: [
                    "attachment_id": attachment.id.uuidString,
                    "error": error.localizedDescription
                ])
            }
        } else {
            slog("No modelContext available to save attachment", category: .sync, level: .error, data: [
                "attachment_id": attachment.id.uuidString
            ])
        }

        uploadProgress = 1.0
        slog("Successfully uploaded attachment", category: .sync, data: [
            "attachment_id": attachment.id.uuidString,
            "filename": attachment.filename,
            "key": attachment.key
        ])
    }

    // MARK: - Presigned URL

    /// Get presigned URL for uploading attachment
    private func getPresignedUploadURL(filename: String, fileSize: Int64) async throws -> AttachmentPresignedUploadResponse {
        return try await api.getAttachmentPresignedUploadURL(filename: filename, fileSize: fileSize)
    }

    // MARK: - S3 Upload

    /// Upload data to S3 using presigned URL
    private func uploadToS3(data: Data, presignedURL: URL, contentType: String) async throws {
        var request = URLRequest(url: presignedURL)
        request.httpMethod = "PUT"
        request.setValue(contentType, forHTTPHeaderField: "Content-Type")

        let (_, response) = try await URLSession.shared.upload(for: request, from: data)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AttachmentUploadError.invalidResponse
        }

        guard httpResponse.statusCode == 200 else {
            throw AttachmentUploadError.s3UploadFailed(httpResponse.statusCode)
        }
    }

    // MARK: - Backend API

    /// Create attachment record in backend
    private func createAttachmentRecord(_ attachment: Attachment) async throws {
        _ = try await api.createAttachment(attachment: attachment)
    }

    /// Create node mappings for attachment
    private func createNodeMappings(attachmentId: UUID, nodeIds: [UUID]) async throws {
        for nodeId in nodeIds {
            do {
                _ = try await api.createAttachmentNodeMapping(attachmentId: attachmentId, nodeId: nodeId)
                await saveAttachmentNodeMappingLocally(attachmentId: attachmentId, nodeId: nodeId)
            } catch {
                slog("Failed to create attachment-node mapping", category: .sync, level: .error, data: [
                    "attachment_id": attachmentId.uuidString,
                    "node_id": nodeId.uuidString,
                    "error": error.localizedDescription
                ])
                // Queue for retry
                await enqueueAttachmentNodeMapping(attachmentId: attachmentId, nodeId: nodeId)
            }
        }
    }

    // MARK: - Sync Queue Support

    /// Enqueue attachment node mapping for sync
    private func enqueueAttachmentNodeMapping(attachmentId: UUID, nodeId: UUID) async {
        let mappingData = MappingData.attachmentNode(attachmentId: attachmentId, nodeId: nodeId, isDeleted: false)
        let syncOp = SyncOp(target: .mappingAttachmentNode, operation: .create, mappingData: mappingData)
        await NetworkState.shared.enqueue(syncOp)
    }

    /// Save attachment node mapping locally
    private func saveAttachmentNodeMappingLocally(attachmentId: UUID, nodeId: UUID) async {
        guard let context = modelContext else { return }

        let mapping = AttachmentNodeMapping(
            attachmentId: attachmentId,
            nodeId: nodeId
        )
        context.insert(mapping)
        try? context.save()
    }

    // MARK: - Cancel Upload

    /// Cancel current upload and cleanup
    func cancelUploadAndCleanup(attachmentId: UUID) async {
        currentUploadTask?.cancel()
        isUploading = false
        uploadProgress = 0.0

        guard let context = modelContext else { return }

        // Delete attachment from local database
        let descriptor = FetchDescriptor<Attachment>(
            predicate: #Predicate<Attachment> { $0.id == attachmentId }
        )
        if let attachment = try? context.fetch(descriptor).first {
            // Delete local file if exists
            if let localPath = attachment.localFilePath {
                try? FileManager.default.removeItem(atPath: localPath)
            }
            context.delete(attachment)
        }

        // Delete associated mappings
        let mappingDescriptor = FetchDescriptor<AttachmentNodeMapping>(
            predicate: #Predicate<AttachmentNodeMapping> { $0.attachmentId == attachmentId }
        )
        if let mappings = try? context.fetch(mappingDescriptor) {
            for mapping in mappings {
                context.delete(mapping)
            }
        }

        // Delete sync queue items
        let syncDescriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { $0.attachmentId == attachmentId }
        )
        if let queueItems = try? context.fetch(syncDescriptor) {
            for item in queueItems {
                context.delete(item)
            }
        }

        try? context.save()
    }

    // MARK: - Download

    /// Get presigned download URL for attachment
    func getPresignedDownloadURL(attachmentId: UUID) async throws -> AttachmentPresignedDownloadResponse {
        return try await api.getAttachmentPresignedDownloadURL(attachmentId: attachmentId)
    }

    // MARK: - Fetch Attachments

    /// Fetch attachments for a session from server
    func fetchSessionAttachments(sessionId: UUID) async throws -> [AttachmentDTO] {
        return try await api.getSessionAttachments(sessionId: sessionId)
    }

    // MARK: - File Management

    /// Copy file to app's cache directory for upload
    func copyFileToCache(from sourceURL: URL, filename: String) throws -> URL {
        let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let attachmentsDir = cacheDir.appendingPathComponent("attachments")

        // Create attachments directory if needed
        if !FileManager.default.fileExists(atPath: attachmentsDir.path) {
            try FileManager.default.createDirectory(at: attachmentsDir, withIntermediateDirectories: true)
        }

        // Create unique filename
        let uniqueFilename = "\(UUID().uuidString)_\(filename)"
        let destinationURL = attachmentsDir.appendingPathComponent(uniqueFilename)

        // Start accessing security-scoped resource
        let didStartAccessing = sourceURL.startAccessingSecurityScopedResource()
        defer {
            if didStartAccessing {
                sourceURL.stopAccessingSecurityScopedResource()
            }
        }

        // Copy file
        try FileManager.default.copyItem(at: sourceURL, to: destinationURL)

        return destinationURL
    }

    /// Get file size from URL
    func getFileSize(from url: URL) -> Int64 {
        let didStartAccessing = url.startAccessingSecurityScopedResource()
        defer {
            if didStartAccessing {
                url.stopAccessingSecurityScopedResource()
            }
        }

        do {
            let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
            return attributes[.size] as? Int64 ?? 0
        } catch {
            return 0
        }
    }
}

// MARK: - Attachment Upload Error

enum AttachmentUploadError: LocalizedError {
    case uploadInProgress
    case fileNotFound(String)
    case fileTooLarge(Int64)
    case invalidPresignedURL
    case invalidResponse
    case presignedURLFailed(Int, String)
    case s3UploadFailed(Int)
    case createRecordFailed(Int, String)
    case mappingFailed(Int, String)
    case downloadURLFailed(Int, String)
    case fetchFailed(Int, String)

    var errorDescription: String? {
        switch self {
        case .uploadInProgress:
            return "An upload is already in progress"
        case .fileNotFound(let path):
            return "File not found: \(path)"
        case .fileTooLarge(let size):
            return "File too large: \(ByteCountFormatter.string(fromByteCount: size, countStyle: .file)). Maximum size is 100MB."
        case .invalidPresignedURL:
            return "Invalid presigned URL received"
        case .invalidResponse:
            return "Invalid response from server"
        case .presignedURLFailed(let code, let message):
            return "Failed to get upload URL (code: \(code)): \(message)"
        case .s3UploadFailed(let code):
            return "File upload failed (code: \(code))"
        case .createRecordFailed(let code, let message):
            return "Failed to create attachment record (code: \(code)): \(message)"
        case .mappingFailed(let code, let message):
            return "Failed to map attachment to node (code: \(code)): \(message)"
        case .downloadURLFailed(let code, let message):
            return "Failed to get download URL (code: \(code)): \(message)"
        case .fetchFailed(let code, let message):
            return "Failed to fetch attachments (code: \(code)): \(message)"
        }
    }
}
