import Foundation
import SwiftData

/// Finds photos that need upload but have no corresponding sync queue item — typically because
/// a previous upload attempt failed in a way that stranded them (max-retry exhaustion, an app
/// crash during save, or the pre-fix data-loss bug that left `upload_needed=true, url=<s3>`).
///
/// Runs on app launch (via NetworkState.setModelContext) and whenever the app transitions from
/// offline to online (via NetworkState.handleNetworkModeChange). Idempotent: if a queue item
/// already exists for a photo, it won't be re-enqueued.
@MainActor
final class StuckPhotoRecoveryService {
    private let modelContext: ModelContext
    private weak var networkState: NetworkState?
    private let photoUploadService: PhotoUploadService

    init(modelContext: ModelContext,
         networkState: NetworkState,
         photoUploadService: PhotoUploadService) {
        self.modelContext = modelContext
        self.networkState = networkState
        self.photoUploadService = photoUploadService
    }

    struct RecoveryResult {
        let enqueued: Int
        let skippedAlreadyQueued: Int
        let skippedMissingFile: Int
    }

    /// Sweep for stuck photos and enqueue them. Returns a summary for logging.
    @discardableResult
    func recoverStuckPhotos() -> RecoveryResult {
        guard let networkState = networkState else {
            return RecoveryResult(enqueued: 0, skippedAlreadyQueued: 0, skippedMissingFile: 0)
        }

        // 1. Find all photos needing upload that haven't been soft-deleted.
        let photosDescriptor = FetchDescriptor<Photo>(
            predicate: #Predicate<Photo> { photo in
                photo.upload_needed == true && photo.is_deleted == false
            }
        )

        let pendingPhotos: [Photo]
        do {
            pendingPhotos = try modelContext.fetch(photosDescriptor)
        } catch {
            slog("StuckPhotoRecovery: failed to fetch pending photos",
                 category: .photo, level: .error, data: ["error": "\(error)"])
            return RecoveryResult(enqueued: 0, skippedAlreadyQueued: 0, skippedMissingFile: 0)
        }

        guard !pendingPhotos.isEmpty else {
            return RecoveryResult(enqueued: 0, skippedAlreadyQueued: 0, skippedMissingFile: 0)
        }

        // 2. Build a set of photo IDs already in the queue. We only care whether the sync
        //    system already knows about this photo — regardless of operation (create/update/delete).
        let photoTargetRaw = SyncTarget.photo.rawValue
        let queueDescriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { item in
                item.targetRawValue == photoTargetRaw
            }
        )

        let queuedPhotoIds: Set<UUID>
        do {
            queuedPhotoIds = Set(try modelContext.fetch(queueDescriptor).compactMap { $0.photoId })
        } catch {
            slog("StuckPhotoRecovery: failed to fetch queue items",
                 category: .photo, level: .error, data: ["error": "\(error)"])
            return RecoveryResult(enqueued: 0, skippedAlreadyQueued: 0, skippedMissingFile: 0)
        }

        // 3. For each stuck photo, decide whether to re-enqueue.
        var enqueued = 0
        var skippedAlreadyQueued = 0
        var skippedMissingFile = 0

        for photo in pendingPhotos {
            if queuedPhotoIds.contains(photo.id) {
                skippedAlreadyQueued += 1
                continue
            }

            // If the local file is gone, we can't recover. Log loudly so ops can investigate;
            // don't enqueue, because retrying would just churn until max-retry deletes the item.
            if !photoUploadService.validatePhotoFile(photo) {
                slog("StuckPhotoRecovery: photo file missing from disk — cannot recover",
                     category: .photo, level: .error,
                     data: [
                        "photo_id": photo.id.uuidString,
                        "filename": photo.filename ?? "nil",
                        "has_s3_url": photo.url?.isEmpty == false
                     ])
                skippedMissingFile += 1
                continue
            }

            networkState.enqueue(SyncOp(target: .photo, operation: .create, photo: photo))
            enqueued += 1
        }

        if enqueued > 0 || skippedMissingFile > 0 {
            slog("StuckPhotoRecovery completed",
                 category: .photo,
                 data: [
                    "enqueued": enqueued,
                    "already_queued": skippedAlreadyQueued,
                    "missing_file": skippedMissingFile,
                    "total_pending": pendingPhotos.count
                 ])
        }

        return RecoveryResult(
            enqueued: enqueued,
            skippedAlreadyQueued: skippedAlreadyQueued,
            skippedMissingFile: skippedMissingFile
        )
    }
}
