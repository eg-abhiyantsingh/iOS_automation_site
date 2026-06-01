//
//  PhotoService.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftData

class PhotoService {
    /// Upload a photo directly (online path used by UI save flows).
    ///
    /// State machine (see PhotoUploadService for the canonical description):
    ///   - `upload_needed=true,  url=nil`      → full upload
    ///   - `upload_needed=true,  url=<s3_url>` → resume at createPhoto (S3 already done)
    ///   - `upload_needed=false`               → already synced, no-op
    ///
    /// On any failure, enqueues a retry op. The queue processor will pick up where we left off.
    static func uploadPhoto(photo: Photo, networkState: NetworkState, modelContext: ModelContext) async {
        AppLogger.log(.debug,
                      "Starting photo upload for \(photo.id) - Caption: \(photo.caption ?? "nil"), Upload needed: \(photo.upload_needed)",
                      category: .photo)

        guard let filename = photo.filename,
              let fileURL = photo.localFileURL,
              FileManager.default.fileExists(atPath: fileURL.path) else {
            AppLogger.log(.error, "Photo upload failed: Missing filename or file on disk", category: .photo)
            return
        }

        // Already fully synced — nothing to do.
        if !photo.upload_needed {
            return
        }

        let s3AlreadyDone = !(photo.url?.isEmpty ?? true)

        do {
            if !s3AlreadyDone {
                // --- Full upload path ---
                guard let data = try? Data(contentsOf: fileURL) else {
                    throw NSError(domain: "PhotoUpload", code: 3,
                                  userInfo: [NSLocalizedDescriptionKey: "Could not read file data"])
                }

                let presignedResponse = try await S3PresignedURLService.shared.getPresignedUploadURL(
                    filename: filename,
                    photoType: photo.type
                )

                guard let presignedURL = URL(string: presignedResponse.url) else {
                    throw NSError(domain: "PhotoUpload", code: 1,
                                  userInfo: [NSLocalizedDescriptionKey: "Invalid presigned URL"])
                }

                var req = URLRequest(url: presignedURL)
                req.httpMethod = "PUT"
                req.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")

                let (_, resp) = try await URLSession.shared.upload(for: req, from: data)
                guard let http = resp as? HTTPURLResponse else {
                    // Do NOT silently return — throw so the catch path queues a retry.
                    throw NSError(domain: "PhotoUpload", code: 2,
                                  userInfo: [NSLocalizedDescriptionKey: "Invalid HTTP response from S3"])
                }
                guard http.statusCode == 200 else {
                    throw NSError(domain: "PhotoUpload", code: http.statusCode,
                                  userInfo: [NSLocalizedDescriptionKey: "S3 upload failed with status: \(http.statusCode)"])
                }

                // Persist S3 URL — but DO NOT flip upload_needed yet. We are not fully synced
                // until the server has created the record.
                try await MainActor.run {
                    photo.url = presignedResponse.public_url
                    try modelContext.save()
                }
            } else {
                AppLogger.log(.info,
                              "S3 upload already complete for photo \(photo.id), resuming at createPhoto",
                              category: .photo)
            }

            // Create server record. This is the step that makes the photo "real" on the server.
            _ = try await APIClient.shared.createPhoto(photo: photo)

            // Only after server confirmation do we mark the photo synced.
            try await MainActor.run {
                photo.upload_needed = false
                try modelContext.save()
            }

            AppLogger.log(.info, "Photo uploaded successfully: \(photo.id)", category: .photo)
        } catch {
            // Duplicate / already exists — a prior attempt's response was lost in flight.
            // Treat as success: the record is on the server.
            if isPhotoAlreadyExistsError(error) {
                AppLogger.log(.info, "Photo already exists on server, marking as synced: \(photo.id)", category: .photo)
                try? await MainActor.run {
                    photo.upload_needed = false
                    try modelContext.save()
                }
                return
            }

            AppLogger.log(.error, "Photo upload error: \(error)", category: .photo)

            // Queue for retry. The queue processor will pick up where we left off —
            // if S3 succeeded above, photo.url is set and the retry will skip S3.
            let op = SyncOp(
                target: .photo,
                operation: .create,
                photo: photo
            )
            await MainActor.run {
                networkState.enqueue(op)
            }
        }
    }

    /// Check if error indicates photo already exists on server (duplicate)
    private static func isPhotoAlreadyExistsError(_ error: Error) -> Bool {
        let errorString = String(describing: error).lowercased()

        // Check for PostgreSQL unique violation error
        if errorString.contains("uniqueviolation") ||
           errorString.contains("duplicate key") ||
           errorString.contains("already exists") ||
           errorString.contains("photos_pk") {
            return true
        }

        // Check for HTTP 409 Conflict
        if let nsError = error as NSError? {
            if nsError.code == 409 {
                return true
            }
        }

        return false
    }
    
    @MainActor
    static func processPhotoChanges(
        stagedAdditions: [Photo],
        stagedDeletions: Set<UUID>,
        originalPhotos: [Photo],
        entity: any EntityWithPhotos,
        modelContext: ModelContext,
        networkState: NetworkState
    ) {
        // Process deletions
        for photoId in stagedDeletions {
            guard let photo = originalPhotos.first(where: { $0.id == photoId }) else { continue }
            
            photo.is_deleted = true
            
            if networkState.mode == .offline {
                let op = SyncOp(target: .photo, operation: .update, photo: photo)
                networkState.enqueue(op)
            }
        }
        
        // Process additions
        for photo in stagedAdditions {
            modelContext.insert(photo)
            entity.associatePhoto(photo)
            
            if networkState.mode == .offline {
                let op = SyncOp(target: .photo, operation: .create, photo: photo)
                networkState.enqueue(op)
            }
        }
        
        try? modelContext.save()
    }
    
    static func uploadPendingPhotos(
        for entity: any EntityWithPhotos,
        stagedDeletions: Set<UUID>,
        displayedPhotos: [Photo],
        modelContext: ModelContext,
        networkState: NetworkState
    ) async {
        guard networkState.canDirectSync else { return }

        // Upload photos needing upload
        let photosToUpload = entity.getAssociatedPhotos(ofType: nil).filter { $0.upload_needed }

        for photo in photosToUpload {
            if let fileURL = photo.localFileURL,
               let _ = try? Data(contentsOf: fileURL),
               let _ = photo.filename {
                await uploadPhoto(photo: photo, networkState: networkState, modelContext: modelContext)
            }
        }
        
        // Process deletions
        for photoId in stagedDeletions {
            guard let photo = displayedPhotos.first(where: { $0.id == photoId }) else { continue }

            // Mark photo as deleted before updating
            photo.is_deleted = true

            do {
                _ = try await APIClient.shared.updatePhoto(photo)
                await MainActor.run {
                    modelContext.delete(photo)
                    try? modelContext.save()
                }
            } catch {
                await MainActor.run {
                    networkState.enqueue(SyncOp(target: .photo, operation: .update, photo: photo))
                }
            }
        }
    }
    
    @MainActor
    static func updatePhotoCaption(
        _ photo: Photo,
        caption: String,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        photo.caption = caption
        photo.upload_needed = true

        do {
            try modelContext.save()
            AppLogger.log(.info, "Photo caption updated locally: '\(caption)' for photo \(photo.id) - URL: \(photo.url ?? "nil (not uploaded yet)"), Upload needed: \(photo.upload_needed)", category: .photo)

            if NetworkState.shared.canDirectSync {
                Task {
                    // Check if photo has been uploaded to server
                    // Photo must have a URL (set after S3 upload) to exist on server
                    guard let photoUrl = photo.url, !photoUrl.isEmpty else {
                        // Photo not uploaded yet - caption will be included in createPhoto()
                        // Just save locally and let the upload process include it
                        AppLogger.log(.notice, "Photo not uploaded yet, caption saved locally and will sync during upload", category: .photo)
                        await MainActor.run {
                            onCompletion?(true, "Caption saved")
                        }
                        return
                    }

                    // Photo exists on server, safe to update
                    do {
                        _ = try await APIClient.shared.updatePhoto(photo)
                        AppLogger.log(.info, "Photo caption synced to server", category: .photo)
                        await MainActor.run {
                            photo.upload_needed = false
                            try? modelContext.save()
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync photo caption: \(error)", category: .photo)
                        let op = SyncOp(target: .photo, operation: .update, photo: photo)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Caption saved locally, will sync when online")
                        }
                    }
                }
            } else {
                // Offline mode - only queue update if photo exists on server
                if let photoUrl = photo.url, !photoUrl.isEmpty {
                    // Photo was previously uploaded, queue update operation
                    let op = SyncOp(target: .photo, operation: .update, photo: photo)
                    NetworkState.shared.enqueue(op)
                    onCompletion?(true, "Caption saved locally, will sync when online")
                } else {
                    // Photo not uploaded yet - caption will be included in create operation
                    AppLogger.log(.notice, "Photo not uploaded yet, caption will sync during photo creation", category: .photo)
                    onCompletion?(true, "Caption saved locally")
                }
            }
        } catch {
            AppLogger.log(.error, "Failed to save photo caption: \(error)", category: .photo)
            onCompletion?(false, error.localizedDescription)
        }
    }

    @MainActor
    static func deletePhoto(
        _ photo: Photo,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        photo.is_deleted = true
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Photo marked as deleted locally", category: .photo)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updatePhoto(photo)
                        AppLogger.log(.info, "Photo deletion synced to server", category: .photo)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync photo deletion: \(error)", category: .photo)
                        let op = SyncOp(target: .photo, operation: .update, photo: photo)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Photo deleted locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .photo, operation: .update, photo: photo)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Photo deleted locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to delete photo: \(error)", category: .photo)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    @MainActor
    static func createPhoto(
        for entity: any EntityWithPhotos,
        type: String,
        localFilepath: String,
        filename: String,
        modelContext: ModelContext,
        caption: String? = nil
    ) -> Photo {
        // Extract photo ID from filename (format: "uuid.jpg")
        // This assumes filename follows the pattern "{photoId}.jpg"
        let photoId: UUID
        if let uuidString = filename.components(separatedBy: ".").first,
           let extractedId = UUID(uuidString: uuidString) {
            photoId = extractedId
        } else {
            // Fallback: generate new UUID if filename doesn't contain valid UUID
            photoId = UUID()
        }

        // URL will be set after successful upload with public_url from backend
        let photo = Photo(
            id: photoId,  // Use extracted or generated photo ID
            node: entity.entityType == .node ? entity as? NodeV2 : nil,
            userTask: entity.entityType == .userTask ? entity as? UserTask : nil,
            issue: entity.entityType == .issue ? entity as? Issue : nil,
            url: nil,  // Will be populated after upload
            type: type,
            sld: (entity as? NodeV2)?.sld ?? (entity as? Issue)?.sld ?? (entity as? UserTask)?.sld,
            upload_needed: true,
            local_filepath: localFilepath,
            filename: filename,
            is_deleted: false,
            caption: caption
        )

        return photo
    }
}
