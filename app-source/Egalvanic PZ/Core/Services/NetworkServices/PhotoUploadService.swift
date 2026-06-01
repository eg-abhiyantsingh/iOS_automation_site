import Foundation
import SwiftData

/// Service responsible for handling photo uploads to S3.
///
/// Upload state machine (encoded in Photo fields):
///   - `upload_needed=true,  url=nil`        → nothing uploaded yet; do full upload
///   - `upload_needed=true,  url=<s3_url>`   → S3 done, server record NOT yet created; resume at createPhoto
///   - `upload_needed=false, url=<s3_url>`   → fully synced; nothing to do
///
/// Invariant: `upload_needed=false` is committed ONLY after `api.createPhoto` succeeds.
/// Persisting `upload_needed=false` before server confirmation was the data-loss bug (ZP photo sync).
@MainActor
final class PhotoUploadService {
    private let api = APIClient.shared
    private var modelContext: ModelContext?

    init(modelContext: ModelContext? = nil) {
        self.modelContext = modelContext
    }

    /// Set or update the model context
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    /// Upload a photo to S3 and create its server record.
    ///
    /// Safe to retry after any failure:
    ///   - If S3 upload already succeeded in a prior attempt (`photo.url != nil`), this skips
    ///     the S3 PUT and goes directly to `api.createPhoto`, avoiding orphan S3 blobs.
    ///   - If `api.createPhoto` returns a duplicate/409 (record already exists because a prior
    ///     attempt's response was lost in flight), this is treated as success.
    func uploadPhoto(_ photo: Photo) async throws {
        guard let filename = photo.filename else {
            throw SyncError.fileNotFound("No filename for photo")
        }

        // Already fully synced — nothing to do.
        if !photo.upload_needed {
            slog("Photo already synced, skipping upload",
                 category: .photo,
                 data: ["photo_id": photo.id.uuidString])
            return
        }

        // Resume path: if a prior attempt already pushed bytes to S3, skip the S3 PUT and
        // go directly to createPhoto. `photo.url` being non-empty is our signal that S3 is done.
        let s3AlreadyDone = !(photo.url?.isEmpty ?? true)

        if !s3AlreadyDone {
            // --- Full upload path ---
            let fileURL = try getPhotoFileURL(for: filename)
            try validatePhotoFileAtURL(fileURL)

            let data = try Data(contentsOf: fileURL)

            let presignedResponse = try await S3PresignedURLService.shared.getPresignedUploadURL(
                filename: filename,
                photoType: photo.type
            )

            guard let presignedURL = URL(string: presignedResponse.url) else {
                throw NSError(domain: "PhotoUpload", code: 1,
                              userInfo: [NSLocalizedDescriptionKey: "Invalid presigned URL"])
            }

            try await uploadToS3(data: data, presignedURL: presignedURL)

            // Persist the S3 URL so a later retry can resume at createPhoto instead of
            // re-uploading the same bytes. upload_needed stays TRUE — we are NOT yet fully synced.
            try persistS3URL(on: photo, url: presignedResponse.public_url)
        } else {
            slog("S3 upload already complete for photo, skipping to createPhoto",
                 category: .photo,
                 data: ["photo_id": photo.id.uuidString, "s3_url": photo.url ?? ""])
        }

        // Create server record. If this throws, upload_needed stays true and photo.url remains
        // set, so the retry will skip S3 and try createPhoto again.
        do {
            _ = try await api.createPhoto(photo: photo)
        } catch {
            if isPhotoAlreadyExistsError(error) {
                slog("Photo already exists on server, treating as synced",
                     category: .photo,
                     data: ["photo_id": photo.id.uuidString])
                try markPhotoAsSynced(photo)
                return
            }
            throw error
        }

        // Only now — after the server has confirmed — is it safe to mark fully synced.
        try markPhotoAsSynced(photo)

        slog("Successfully uploaded photo",
             category: .photo,
             data: ["filename": filename, "photo_id": photo.id.uuidString])
    }
    
    /// Upload photo with timeout protection
    func uploadPhotoWithTimeout(_ photo: Photo, timeout: TimeInterval = SyncConfiguration.photoUploadTimeout) async throws {
        try await withThrowingTaskGroup(of: Void.self) { group in
            group.addTask {
                try await self.uploadPhoto(photo)
            }
            
            group.addTask {
                try await Task.sleep(nanoseconds: UInt64(timeout * 1_000_000_000))
                throw SyncError.timeout
            }
            
            // Wait for the first task to complete (either upload or timeout)
            try await group.next()
            group.cancelAll()
        }
    }
    
    /// Validate that a photo file exists at the expected location
    func validatePhotoFile(_ photo: Photo) -> Bool {
        guard let filename = photo.filename else { return false }
        
        do {
            let fileURL = try getPhotoFileURL(for: filename)
            return FileManager.default.fileExists(atPath: fileURL.path)
        } catch {
            return false
        }
    }
    
    /// Get the file URL for a photo
    private func getPhotoFileURL(for filename: String) throws -> URL {
        guard let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            throw SyncError.fileNotFound("Could not access documents directory")
        }
        
        return documentsURL.appendingPathComponent("photos").appendingPathComponent(filename)
    }
    
    /// Validate photo file exists and check size limits
    private func validatePhotoFileAtURL(_ fileURL: URL) throws {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            throw SyncError.fileNotFound(fileURL.path)
        }
        
        // Check file size
        let fileAttributes = try FileManager.default.attributesOfItem(atPath: fileURL.path)
        let fileSize = fileAttributes[.size] as? Int64 ?? 0
        
        guard fileSize < SyncConfiguration.maxPhotoSize else {
            throw SyncError.fileTooLarge(fileSize)
        }
    }
    
    /// Upload data to S3 using presigned URL
    private func uploadToS3(data: Data, presignedURL: URL) async throws {
        var request = URLRequest(url: presignedURL)
        request.httpMethod = "PUT"
        request.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
        
        let (_, response) = try await URLSession.shared.upload(for: request, from: data)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "PhotoUpload", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid response from S3"])
        }
        
        guard httpResponse.statusCode == 200 else {
            throw NSError(domain: "PhotoUpload", code: httpResponse.statusCode, 
                         userInfo: [NSLocalizedDescriptionKey: "S3 upload failed with status: \(httpResponse.statusCode)"])
        }
    }
    
    /// Persist the S3 public URL for this photo without flipping `upload_needed`.
    /// Throws if the save fails so the caller can react, rather than silently swallowing.
    private func persistS3URL(on photo: Photo, url: String) throws {
        photo.url = url
        guard let context = modelContext else { return }
        do {
            try context.save()
        } catch {
            slog("Failed to persist S3 URL",
                 category: .photo, level: .error,
                 data: ["photo_id": photo.id.uuidString, "error": "\(error)"])
            throw error
        }
    }

    /// Mark the photo as fully synced (S3 + server record) in local storage.
    /// Call ONLY after `api.createPhoto` has succeeded (or returned a duplicate).
    private func markPhotoAsSynced(_ photo: Photo) throws {
        photo.upload_needed = false
        guard let context = modelContext else { return }
        do {
            try context.save()
        } catch {
            slog("Failed to persist upload_needed=false",
                 category: .photo, level: .error,
                 data: ["photo_id": photo.id.uuidString, "error": "\(error)"])
            throw error
        }
    }

    /// Upload a photo from a captured snapshot + preserved file path.
    ///
    /// Called by `SyncExecutionService` for `.photo + .create` ops drained from the
    /// queue. Mirrors Android's `PhotoService.uploadPhotoFromSnapshot`: the snapshot
    /// captures only metadata, so the binary still has to be PUT to S3 before the
    /// metadata POST is meaningful. The metadata-only fast-path (just replaying the
    /// snapshot JSON) was the bug — it created server records pointing at no S3 object.
    ///
    /// Mutates SwiftData *best-effort* via `syncLocalPhotoAfterSnapshotUpload` so
    /// `upload_needed` flips to false on the live entity (clears the Photo Storage
    /// Diagnostics pending state). Silently no-ops when the entity is gone after a
    /// site switch / logout — that's the case this snapshot path was designed for.
    /// The queue row + preserved file are deleted by the caller on success.
    ///
    /// Resume note: the `s3AlreadyDone` check inspects `dto.url` decoded from the
    /// captured snapshot. Snapshots taken on the offline-create path always have
    /// `url == nil` (the live online path is the only one that pre-populates `url`
    /// before enqueue). So in practice retries on the offline-create path re-PUT
    /// to S3 — wasteful but safe (same `photo_<filename>` key → S3 overwrite, no
    /// orphan blobs). The check still guards the rare case where a snapshot was
    /// captured after a partial live upload.
    func uploadPhotoFromSnapshot(snapshotJSON: Data, filePath: String) async throws {
        var dto: SLDDTOPhoto
        do {
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            dto = try decoder.decode(SLDDTOPhoto.self, from: snapshotJSON)
        } catch {
            throw NSError(domain: "PhotoUpload", code: 10, userInfo: [
                NSLocalizedDescriptionKey: "Failed to parse photo snapshot JSON: \(error.localizedDescription)"
            ])
        }

        let fileURL = URL(fileURLWithPath: filePath)
        let s3AlreadyDone = !(dto.url?.isEmpty ?? true)

        if !s3AlreadyDone {
            try validatePhotoFileAtURL(fileURL)
            let data = try Data(contentsOf: fileURL)

            guard let filename = dto.filename else {
                throw SyncError.fileNotFound("Snapshot has no filename for photo \(dto.id.uuidString)")
            }

            let presignedResponse = try await S3PresignedURLService.shared.getPresignedUploadURL(
                filename: filename,
                photoType: dto.type
            )

            guard let presignedURL = URL(string: presignedResponse.url) else {
                throw NSError(domain: "PhotoUpload", code: 11,
                              userInfo: [NSLocalizedDescriptionKey: "Invalid presigned URL"])
            }

            try await uploadToS3(data: data, presignedURL: presignedURL)
            dto.url = presignedResponse.public_url
        } else {
            slog("S3 already complete on snapshot replay (resuming at metadata POST)",
                 category: .photo,
                 data: ["photo_id": dto.id.uuidString, "s3_url": dto.url ?? ""])
        }

        // Defense-in-depth: `snapshotPhotoCreateBody` already hard-codes
        // `upload_needed: false`, but explicitly re-stamping here means the wire
        // payload stays correct even if the snapshot encoder ever changes default.
        dto.upload_needed = false

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let payload = try encoder.encode(dto)

        do {
            _ = try await api.replaySnapshotPayload(
                target: .photo,
                operation: .create,
                entityId: dto.id,
                payload: payload
            )
        } catch {
            if isPhotoAlreadyExistsError(error) {
                slog("Photo already exists on server (snapshot path), treating as synced",
                     category: .photo,
                     data: ["photo_id": dto.id.uuidString])
                syncLocalPhotoAfterSnapshotUpload(id: dto.id, s3URL: dto.url)
                return
            }
            throw error
        }

        // Best-effort local DB sync — flips `upload_needed` and persists the S3
        // URL on the live Photo entity if it still exists. After a site switch
        // the entity is gone, in which case there's nothing to update. Without
        // this, the upload succeeds on the server but the Photo Storage
        // Diagnostics view (which reads `photo.upload_needed`) keeps showing
        // it as pending.
        syncLocalPhotoAfterSnapshotUpload(id: dto.id, s3URL: dto.url)

        slog("Successfully uploaded photo from snapshot",
             category: .photo,
             data: ["photo_id": dto.id.uuidString, "filename": dto.filename ?? "nil"])
    }

    /// Mirror server state onto the local Photo entity after a successful
    /// snapshot-path upload. Silently skips when the entity is gone (site
    /// switch / wipe) — that's the case the snapshot path was designed for.
    private func syncLocalPhotoAfterSnapshotUpload(id: UUID, s3URL: String?) {
        guard let context = modelContext else { return }
        let descriptor = FetchDescriptor<Photo>(predicate: #Predicate<Photo> { $0.id == id })
        guard let photo = try? context.fetch(descriptor).first else { return }
        photo.url = s3URL
        photo.upload_needed = false
        do {
            try context.save()
        } catch {
            slog("Failed to persist local Photo state after snapshot upload",
                 category: .photo, level: .warning,
                 data: ["photo_id": id.uuidString, "error": "\(error)"])
        }
    }

    /// Detect duplicate-record errors from the server so we can resolve idempotently after
    /// a prior attempt's response was lost in flight (e.g., S3 PUT succeeded, createPhoto
    /// succeeded on the server but the response never reached the device).
    ///
    /// PRE-EXISTING OBSERVABILITY GAP: `APIClient.executeRequestRaw` collapses every
    /// non-2xx response into bare `URLError(.badServerResponse)` (code -1011), so
    /// none of the substring checks below fire on a real 409. A genuine duplicate
    /// today gets retried, then surfaces as a generic failure in Sentry. Worth
    /// fixing system-wide by preserving HTTP status + body on the thrown error,
    /// but that refactor is out of scope here. Keeping this branch in place so
    /// future status-aware errors (or the legacy `uploadPhoto` path's NSError
    /// shape) flow through correctly.
    private func isPhotoAlreadyExistsError(_ error: Error) -> Bool {
        let errorString = String(describing: error).lowercased()
        if errorString.contains("uniqueviolation") ||
           errorString.contains("duplicate key") ||
           errorString.contains("already exists") ||
           errorString.contains("photos_pk") {
            return true
        }
        if let nsError = error as NSError?, nsError.code == 409 {
            return true
        }
        return false
    }


    /// Create metadata for photo upload logging
    func createPhotoUploadMetadata(for photo: Photo, attemptedPath: String? = nil) -> (nodeId: UUID?, originalPath: String?, attemptedPath: String?, remoteURL: String?) {
        let nodeId = photo.node?.id
        let originalPath = photo.local_filepath
        let remoteURL = photo.url
        
        let finalAttemptedPath: String?
        if let provided = attemptedPath {
            finalAttemptedPath = provided
        } else if let filename = photo.filename {
            finalAttemptedPath = try? getPhotoFileURL(for: filename).path
        } else {
            finalAttemptedPath = nil
        }
        
        return (nodeId: nodeId, originalPath: originalPath, attemptedPath: finalAttemptedPath, remoteURL: remoteURL)
    }
}