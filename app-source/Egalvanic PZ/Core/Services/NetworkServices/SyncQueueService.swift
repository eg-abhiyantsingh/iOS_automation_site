import Foundation
import SwiftData

/// Service responsible for managing the sync queue persistence
@MainActor
final class SyncQueueService {
    private var modelContext: ModelContext?
    
    init(modelContext: ModelContext? = nil) {
        self.modelContext = modelContext
    }
    
    /// Set or update the model context
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }
    
    /// Enqueue a sync operation
    func enqueue(_ op: SyncOp) {
        guard let context = modelContext else {
            slog("No model context available for enqueue", category: .sync, level: .error)
            return
        }

        // Skip if a pending CREATE already exists for this entity. Prevents duplicate
        // uploads when multiple paths (direct upload error handler, stuck-photo sweep,
        // batch enqueue) land a second CREATE for the same entity before sync runs.
        // UPDATE/DELETE/mapping ops are intentionally NOT deduped here — they need
        // latest-wins / state-change semantics preserved.
        if let existing = findExistingPendingCreateItem(for: op, in: context) {
            slog("Enqueue skipped - CREATE already pending for this entity", category: .sync, level: .debug, data: [
                "target": "\(op.target)",
                "existingQueueItemId": existing.id.uuidString
            ])
            return
        }

        // ZP-1847: coalesce UPDATE-after-UPDATE for the same entity. The latest
        // edit always wins; we just refresh the existing row's snapshot in
        // place rather than appending a new row. Keeps the queue bounded under
        // repeated offline edits.
        if let existing = findExistingPendingUpdateItem(for: op, in: context) {
            refreshSnapshot(on: existing, from: op)
            do {
                try context.save()
                slog("UPDATE coalesced into existing pending item",
                     category: .sync, level: .debug,
                     data: ["target": "\(op.target)",
                            "queueItemId": existing.id.uuidString])
            } catch {
                slog("Failed to save coalesced UPDATE",
                     category: .sync, level: .error,
                     data: ["error": "\(error)"])
            }
            return
        }

        // Create persistent queue item
        let queueItem = SyncQueueItem(
            target: op.target,
            operation: op.operation,
            nodeId: op.node?.id,
            edgeId: op.edge?.id,
            photoId: op.photo?.id,
            userTaskId: op.userTask?.id,
            irPhotoId: op.irPhoto?.id,
            irSessionId: op.irSession?.id,
            issueId: op.issue?.id,
            quoteId: op.quote?.id,
            formInstanceId: op.formInstance?.id,
            egFormInstanceId: op.egFormInstance?.id,
            buildingId: op.building?.id,
            floorId: op.floor?.id,
            roomId: op.room?.id,
            attachmentId: op.attachment?.id,
            sldViewId: op.sldView?.id,
            mappingData: op.mappingData,
            extraData: op.extraData
        )

        // ZP-1847: stamp self-contained context (user, site, snapshot, file
        // path) so this item can sync after the originating site's tables
        // have been wiped.
        applySnapshotContext(to: queueItem, from: op)

        context.insert(queueItem)

        do {
            try context.save()

            // Log with entity ID for better debugging
            var logData: [String: Any] = [
                "target": "\(op.target)",
                "operation": "\(op.operation)",
                "queueItemId": queueItem.id.uuidString
            ]

            // Add entity-specific ID to log
            switch op.target {
            case .node:
                logData["nodeId"] = queueItem.nodeId?.uuidString ?? "nil"
                if queueItem.nodeId == nil {
                    slog("WARNING: Node enqueued without nodeId!", category: .sync, level: .warning)
                }
            case .photo:
                logData["photoId"] = queueItem.photoId?.uuidString ?? "nil"
                if queueItem.photoId == nil {
                    slog("WARNING: Photo enqueued without photoId!", category: .sync, level: .warning)
                }
            case .edge:
                logData["edgeId"] = queueItem.edgeId?.uuidString ?? "nil"
            case .userTask:
                logData["userTaskId"] = queueItem.userTaskId?.uuidString ?? "nil"
            case .irSession:
                logData["irSessionId"] = queueItem.irSessionId?.uuidString ?? "nil"
            case .irPhoto:
                logData["irPhotoId"] = queueItem.irPhotoId?.uuidString ?? "nil"
            case .issue:
                logData["issueId"] = queueItem.issueId?.uuidString ?? "nil"
            case .quote:
                logData["quoteId"] = queueItem.quoteId?.uuidString ?? "nil"
            case .attachment:
                logData["attachmentId"] = queueItem.attachmentId?.uuidString ?? "nil"
                if queueItem.attachmentId == nil {
                    slog("WARNING: Attachment enqueued without attachmentId!", category: .sync, level: .warning)
                }
            case .sldView:
                logData["sldViewId"] = queueItem.sldViewId?.uuidString ?? "nil"
                if queueItem.sldViewId == nil {
                    slog("WARNING: SLDView enqueued without sldViewId!", category: .sync, level: .warning)
                }
            default:
                break
            }

            slog("Enqueued sync operation", category: .sync, data: logData)
        } catch {
            slog("Failed to save sync queue item", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }
    
    /// Enqueue multiple sync operations with a single context.save()
    func enqueueBatch(_ ops: [SyncOp]) {
        guard let context = modelContext else {
            slog("No model context available for batch enqueue", category: .sync, level: .error)
            return
        }
        guard !ops.isEmpty else { return }

        var insertedCount = 0
        var skippedCount = 0

        for op in ops {
            // Skip if a pending CREATE already exists for this entity. Fetches within
            // the same context also see pending inserts from earlier iterations, so
            // duplicates inside the batch itself are caught as well.
            if let existing = findExistingPendingCreateItem(for: op, in: context) {
                slog("Batch enqueue skipped - CREATE already pending for this entity", category: .sync, level: .debug, data: [
                    "target": "\(op.target)",
                    "existingQueueItemId": existing.id.uuidString
                ])
                skippedCount += 1
                continue
            }

            let queueItem = SyncQueueItem(
                target: op.target,
                operation: op.operation,
                nodeId: op.node?.id,
                edgeId: op.edge?.id,
                photoId: op.photo?.id,
                userTaskId: op.userTask?.id,
                irPhotoId: op.irPhoto?.id,
                irSessionId: op.irSession?.id,
                issueId: op.issue?.id,
                quoteId: op.quote?.id,
                formInstanceId: op.formInstance?.id,
                egFormInstanceId: op.egFormInstance?.id,
                buildingId: op.building?.id,
                floorId: op.floor?.id,
                roomId: op.room?.id,
                attachmentId: op.attachment?.id,
                sldViewId: op.sldView?.id,
                mappingData: op.mappingData,
                extraData: op.extraData
            )
            applySnapshotContext(to: queueItem, from: op)
            context.insert(queueItem)
            insertedCount += 1
        }

        guard insertedCount > 0 else {
            slog("Batch enqueue: all \(ops.count) ops deduped", category: .sync, level: .debug)
            return
        }

        do {
            try context.save()
            slog("Batch enqueued sync operations", category: .sync, data: [
                "inserted": insertedCount,
                "skipped_duplicate": skippedCount,
                "total": ops.count
            ])
        } catch {
            slog("Failed to save batch sync queue items", category: .sync, level: .error,
                 data: ["error": "\(error)", "count": ops.count])
        }
    }

    /// Look up a pending CREATE queue item for the same entity, if one already exists.
    ///
    /// Applies only to CREATE operations on non-mapping targets — UPDATE/DELETE need
    /// latest-wins semantics, and mapping ops can validly repeat (link/unlink/relink).
    /// Follows the same fetch-then-filter-in-Swift pattern used by requeueFailedOperation
    /// below to keep the SwiftData predicate simple.
    ///
    /// Returns nil when the op has no entity id (e.g., mapping ops or data-less ops),
    /// when the fetch fails, or when no existing pending CREATE matches — any of which
    /// fall through to the normal insert path so we never lose data due to a lookup error.
    private func findExistingPendingCreateItem(for op: SyncOp, in context: ModelContext) -> SyncQueueItem? {
        guard op.operation == .create, !op.target.isMapping else { return nil }

        let entityId: UUID?
        switch op.target {
        case .node: entityId = op.node?.id
        case .edge: entityId = op.edge?.id
        case .photo: entityId = op.photo?.id
        case .userTask: entityId = op.userTask?.id
        case .irPhoto: entityId = op.irPhoto?.id
        case .irSession: entityId = op.irSession?.id
        case .issue: entityId = op.issue?.id
        case .quote: entityId = op.quote?.id
        case .formInstance: entityId = op.formInstance?.id
        case .egFormInstance: entityId = op.egFormInstance?.id
        case .building: entityId = op.building?.id
        case .floor: entityId = op.floor?.id
        case .room: entityId = op.room?.id
        case .attachment: entityId = op.attachment?.id
        case .sldView: entityId = op.sldView?.id
        default: return nil
        }

        guard let entityId = entityId else { return nil }

        let targetRaw = op.target.rawValue
        let createRaw = SyncOperation.create.rawValue
        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { item in
                item.targetRawValue == targetRaw &&
                item.operationRawValue == createRaw
            }
        )

        do {
            let candidates = try context.fetch(descriptor)
            return candidates.first { item in
                switch op.target {
                case .node: return item.nodeId == entityId
                case .edge: return item.edgeId == entityId
                case .photo: return item.photoId == entityId
                case .userTask: return item.userTaskId == entityId
                case .irPhoto: return item.irPhotoId == entityId
                case .irSession: return item.irSessionId == entityId
                case .issue: return item.issueId == entityId
                case .quote: return item.quoteId == entityId
                case .formInstance: return item.formInstanceId == entityId
                case .egFormInstance: return item.egFormInstanceId == entityId
                case .building: return item.buildingId == entityId
                case .floor: return item.floorId == entityId
                case .room: return item.roomId == entityId
                case .attachment: return item.attachmentId == entityId
                case .sldView: return item.sldViewId == entityId
                default: return false
                }
            }
        } catch {
            slog("Dedup fetch failed - falling through to insert", category: .sync, level: .warning, data: ["error": "\(error)"])
            return nil
        }
    }

    // MARK: - ZP-1847 snapshot context

    /// Stamp `userId`, `siteId`, `snapshotJSON`, `snapshotTimestamp`, and
    /// `photoFilePath` on a freshly built queue item so it is self-contained
    /// and can sync after the originating site's tables are wiped.
    ///
    /// Photos and attachments have their on-disk binaries copied into the
    /// protected directory keyed by the queue item's id. The original file
    /// stays in place so in-app rendering keeps working until the upload
    /// succeeds.
    private func applySnapshotContext(to item: SyncQueueItem, from op: SyncOp) {
        item.userId = currentUserId()
        item.siteId = resolveSiteId(for: op)
        item.snapshotJSON = SyncSnapshotEncoder.encode(op)
        item.snapshotTimestamp = Date()
        // Store a Documents-relative path (not the absolute URL.path) so the
        // queue row survives the iOS Data container UUID changing across app
        // updates / restores. See SyncFileManager.relativePathForPreservedFile.
        if preserveFile(for: op, queueItemId: item.id) != nil {
            item.photoFilePath = SyncFileManager.shared.relativePathForPreservedFile(queueItemId: item.id)
        } else {
            item.photoFilePath = nil
        }
    }

    /// Refresh an existing pending UPDATE in place (coalesce). Reuses the
    /// queue row's id so any preserved photo file stays correctly keyed.
    private func refreshSnapshot(on item: SyncQueueItem, from op: SyncOp) {
        item.snapshotJSON = SyncSnapshotEncoder.encode(op) ?? item.snapshotJSON
        item.snapshotTimestamp = Date()
        item.extraData = op.extraData ?? item.extraData
        if preserveFile(for: op, queueItemId: item.id) != nil,
           let preservedRelative = SyncFileManager.shared.relativePathForPreservedFile(queueItemId: item.id) {
            item.photoFilePath = preservedRelative
        }
        // userId/siteId stay frozen at first-enqueue values.
    }

    private func currentUserId() -> UUID? {
        let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        let id = AppStateManager.shared.userId
        return id == nullSentinel ? nil : id
    }

    /// Prefer the entity's own SLD relationship — captures the true origin
    /// site even when the user has navigated away. Falls back to
    /// `AppStateManager.activeSLDId` for ops without an entity reference
    /// (mappings, anonymous helpers).
    private func resolveSiteId(for op: SyncOp) -> UUID? {
        let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        if let id = entitySiteId(for: op) { return id }
        let active = AppStateManager.shared.activeSLDId
        return active == nullSentinel ? nil : active
    }

    private func entitySiteId(for op: SyncOp) -> UUID? {
        if let id = op.node?.sld?.id { return id }
        if let id = op.edge?.sld?.id { return id }
        if let id = op.photo?.sld?.id { return id }
        if let id = op.userTask?.sld?.id { return id }
        if let id = op.irSession?.sld.id { return id }
        if let id = op.irPhoto?.sld.id { return id }
        if let id = op.issue?.sld?.id { return id }
        if let id = op.quote?.sld?.id { return id }
        if let id = op.building?.sld?.id { return id }
        if let id = op.attachment?.sldId { return id }
        if let id = op.sldView?.sld_id { return id }
        return nil
    }

    /// Copy photo / attachment binaries to the protected directory so the
    /// upload survives a site switch. Returns the protected URL or nil if
    /// the op carries no file.
    private func preserveFile(for op: SyncOp, queueItemId: UUID) -> URL? {
        switch op.target {
        case .photo:
            guard let photo = op.photo, let source = photo.localFileURL else { return nil }
            return try? SyncFileManager.shared.preserveFile(at: source, queueItemId: queueItemId)
        case .attachment:
            guard let attachment = op.attachment,
                  let path = attachment.localFilePath else { return nil }
            return try? SyncFileManager.shared.preserveFile(
                at: URL(fileURLWithPath: path), queueItemId: queueItemId)
        default:
            // IRPhoto stores keys only — no local file to preserve.
            return nil
        }
    }

    /// Look up a pending UPDATE for the same entity on the same target so
    /// repeated edits coalesce into a single queue row with the latest
    /// snapshot.
    private func findExistingPendingUpdateItem(for op: SyncOp, in context: ModelContext) -> SyncQueueItem? {
        guard op.operation == .update, !op.target.isMapping else { return nil }

        let entityId: UUID?
        switch op.target {
        case .node: entityId = op.node?.id
        case .edge: entityId = op.edge?.id
        case .photo: entityId = op.photo?.id
        case .userTask: entityId = op.userTask?.id
        case .irPhoto: entityId = op.irPhoto?.id
        case .irSession: entityId = op.irSession?.id
        case .issue: entityId = op.issue?.id
        case .quote: entityId = op.quote?.id
        case .formInstance: entityId = op.formInstance?.id
        case .egFormInstance: entityId = op.egFormInstance?.id
        case .building: entityId = op.building?.id
        case .floor: entityId = op.floor?.id
        case .room: entityId = op.room?.id
        case .attachment: entityId = op.attachment?.id
        case .sldView: entityId = op.sldView?.id
        default: return nil
        }
        guard let entityId = entityId else { return nil }

        let targetRaw = op.target.rawValue
        let updateRaw = SyncOperation.update.rawValue
        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { item in
                item.targetRawValue == targetRaw &&
                item.operationRawValue == updateRaw
            }
        )

        do {
            let candidates = try context.fetch(descriptor)
            return candidates.first { item in
                switch op.target {
                case .node: return item.nodeId == entityId
                case .edge: return item.edgeId == entityId
                case .photo: return item.photoId == entityId
                case .userTask: return item.userTaskId == entityId
                case .irPhoto: return item.irPhotoId == entityId
                case .irSession: return item.irSessionId == entityId
                case .issue: return item.issueId == entityId
                case .quote: return item.quoteId == entityId
                case .formInstance: return item.formInstanceId == entityId
                case .egFormInstance: return item.egFormInstanceId == entityId
                case .building: return item.buildingId == entityId
                case .floor: return item.floorId == entityId
                case .room: return item.roomId == entityId
                case .attachment: return item.attachmentId == entityId
                case .sldView: return item.sldViewId == entityId
                default: return false
                }
            }
        } catch {
            slog("UPDATE coalesce fetch failed - falling through to insert",
                 category: .sync, level: .warning, data: ["error": "\(error)"])
            return nil
        }
    }

    /// Convenience methods for enqueueing mapping operations
    func enqueueIssueTaskMapping(issueId: UUID, taskId: UUID, isDeleted: Bool) {
        let mappingData = MappingData.issueTask(issueId: issueId, taskId: taskId, isDeleted: isDeleted)
        let op = SyncOp(
            target: .mappingIssueTask,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        enqueue(op)
    }
    
    func enqueueTaskSessionMapping(taskId: UUID, sessionId: UUID, isDeleted: Bool) {
        let mappingData = MappingData.taskSession(taskId: taskId, sessionId: sessionId, isDeleted: isDeleted)
        let op = SyncOp(
            target: .mappingTaskSession,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        enqueue(op)
    }
    
    func enqueueQuoteTaskMapping(quoteId: UUID, taskId: UUID, isDeleted: Bool) {
        let mappingData = MappingData.quoteTask(quoteId: quoteId, taskId: taskId, isDeleted: isDeleted)
        let op = SyncOp(
            target: .mappingQuoteTask,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        enqueue(op)
    }
    
    func enqueueUserTaskMapping(userId: UUID, taskId: UUID, mappingType: String, isDeleted: Bool) {
        let mappingData = MappingData.userTask(userId: userId, taskId: taskId, mappingType: mappingType, isDeleted: isDeleted)
        let op = SyncOp(
            target: .mappingUserTask,
            operation: isDeleted ? .update : .create,
            mappingData: mappingData
        )
        enqueue(op)
    }
    
    /// Maximum number of retries before an item is considered permanently failed
    private let maxRetryCount = 3

    /// Get pending sync operations with optional limit.
    ///
    /// ZP-1847: items are filtered by the current user. Items belonging to a
    /// different user (after a non-destructive account switch) are skipped —
    /// they remain in the DB for that user to flush when they log back in.
    /// Legacy items with `userId == nil` (pre-migration) are eligible for the
    /// current user; the server independently rejects cross-user mutations
    /// as a defense in depth.
    func getPendingSyncOps(limit: Int? = nil) async -> [SyncOp] {
        guard let context = modelContext else { return [] }

        // ZP-2097 — strict-stop: retryCount no longer gates fetch. Items
        // live forever until the user explicitly resolves them via Retry
        // or Delete. The flush itself halts on first failure and surfaces
        // the failed row inline.
        var descriptor = FetchDescriptor<SyncQueueItem>(
            sortBy: [SortDescriptor(\.createdAt)]
        )

        // Don't apply fetchLimit yet — the predicate can't easily express
        // "userId == currentUser || userId == nil" so we filter in memory.
        // We'll apply the limit after filtering.

        do {
            let queueItems = try context.fetch(descriptor)
            let activeUserId = currentUserId()

            var syncOps: [SyncOp] = []

            for item in queueItems {
                // User-scoped filter (ZP-1847). nil userId means legacy item
                // pre-migration; allow it through under the current user.
                if let itemUser = item.userId, let activeUser = activeUserId, itemUser != activeUser {
                    continue
                }

                // Skip rows that previously failed because both the captured
                // snapshot and the live SwiftData entity were missing — typically
                // pre-ZP-1847 v1.30 queue items that survived the upgrade but
                // whose entity tables were wiped. Re-flushing them would just
                // produce another "Entity no longer exists" error and bump the
                // retry chip on every Sync tap. They stay visible in the Pending
                // tab; the user can resolve via Retry (clears the sentinel and
                // gives them another chance) or Delete.
                if item.lastFailureCode == SyncConfiguration.unrecoverableLegacyFailureCode {
                    continue
                }

                let entityId: UUID? = {
                    switch item.target {
                    case .node: return item.nodeId
                    case .edge: return item.edgeId
                    case .photo: return item.photoId
                    case .userTask: return item.userTaskId
                    case .irPhoto: return item.irPhotoId
                    case .irSession: return item.irSessionId
                    case .issue: return item.issueId
                    case .quote: return item.quoteId
                    case .formInstance: return item.formInstanceId
                    case .egFormInstance: return item.egFormInstanceId
                    case .building: return item.buildingId
                    case .floor: return item.floorId
                    case .room: return item.roomId
                    case .attachment: return item.attachmentId
                    case .sldView: return item.sldViewId
                    default: return nil
                    }
                }()

                let op = SyncOp(
                    target: item.target,
                    operation: item.operation,
                    node: nil,
                    edge: nil,
                    photo: nil,
                    userTask: nil,
                    irPhoto: nil,
                    irSession: nil,
                    issue: nil,
                    quote: nil,
                    formInstance: nil,
                    mappingData: item.mappingData,
                    queueItemId: item.id,
                    extraData: item.extraData,
                    snapshotJSON: item.snapshotJSON,
                    photoFilePath: item.photoFilePath,
                    userId: item.userId,
                    siteId: item.siteId,
                    entityId: entityId
                )

                syncOps.append(op)
                if let limit = limit, syncOps.count >= limit { break }
            }

            return syncOps
        } catch {
            slog("Failed to fetch pending sync ops", category: .sync, level: .error, data: ["error": "\(error)"])
            return []
        }
    }
    
    /// Remove a successfully synced item from the queue.
    func removeSyncQueueItem(withId id: UUID) async {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.id == id }
        )

        do {
            if let item = try context.fetch(descriptor).first {
                context.delete(item)
                try context.save()
            }
        } catch {
            slog("Failed to remove sync queue item", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }
    
    /// Remove all queued operations for a given attachment (e.g. when deleting an unsynced attachment)
    func removeQueueItems(forAttachmentId attachmentId: UUID) {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.attachmentId == attachmentId }
        )

        do {
            let items = try context.fetch(descriptor)
            for item in items {
                context.delete(item)
            }
            if !items.isEmpty {
                try context.save()
                slog("Removed \(items.count) queue items for attachment \(attachmentId)", category: .sync)
            }
        } catch {
            slog("Failed to remove queue items for attachment", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }

    /// Get current queue count for the current user across all sites.
    ///
    /// ZP-1847: only items owned by the current user (or legacy items with
    /// `userId == nil`) are counted. After a non-destructive account
    /// switch, the previous user's items still live in the DB but don't
    /// show up in this count — so the badge and `canDirectSync` rule see
    /// the user's own queue, never another user's.
    func getQueueCount() -> Int {
        guard let context = modelContext else { return 0 }

        let descriptor = FetchDescriptor<SyncQueueItem>()

        do {
            let items = try context.fetch(descriptor)
            let activeUserId = currentUserId()
            return items.reduce(0) { acc, item in
                if let itemUser = item.userId, let activeUser = activeUserId, itemUser != activeUser {
                    return acc
                }
                return acc + 1
            }
        } catch {
            return 0
        }
    }

    /// Total queue count including items belonging to other users on this
    /// device. Used only by orphan-cleanup paths that must see every row;
    /// UI / sync orchestration should always use `getQueueCount()`.
    func getQueueCountAllUsers() -> Int {
        guard let context = modelContext else { return 0 }
        let descriptor = FetchDescriptor<SyncQueueItem>()
        return (try? context.fetchCount(descriptor)) ?? 0
    }

    /// ZP-2173 (Android parity) — count rows that predate user/site/snapshot
    /// stamping (`userId == nil`). These rows depend on the live SwiftData
    /// entity tables to flush, so they can't survive a logout, site switch,
    /// or pull-to-refresh that overwrites those tables. `NetworkState` reads
    /// this count to drive `hasLegacySyncItems`, which gates the destructive
    /// UI surfaces (logout / site switch / schedule / refresh) until the
    /// user resolves the rows from the Sync Queue Analyzer.
    func getLegacyItemCount() -> Int {
        guard let context = modelContext else { return 0 }
        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { $0.userId == nil }
        )
        return (try? context.fetchCount(descriptor)) ?? 0
    }
    
    /// Get remaining queue count
    func getRemainingQueueCount() async -> Int {
        return getQueueCount()
    }

    /// UserDefaults key — set to true once the legacy-path migration has run
    /// to completion on this install. Lets the migration short-circuit on
    /// every subsequent launch so it doesn't fetch queue rows for nothing
    /// once the v1.33→v1.34 window has passed. Versioned suffix leaves room
    /// for future one-shot migrations to use their own keys.
    private static let migrationDoneKey = "didMigrateLegacyPhotoFilePathsV1"

    /// One-shot migration: rewrite any legacy absolute `photoFilePath` values
    /// (written by v1.33 and earlier) to Documents-relative form.
    ///
    /// Background: legacy paths embed the iOS Data container UUID, e.g.
    /// `/var/mobile/Containers/Data/Application/<UUID>/Documents/sync_pending_attachments/…`.
    /// That UUID changes across app updates and backup restores, so the
    /// stored path becomes stale even though the file itself migrated to the
    /// new container. Rewriting to `sync_pending_attachments/<id>/<file>` makes
    /// the row resolve correctly via `SyncFileManager.resolveStoredPath`.
    ///
    /// Self-gating via a UserDefaults flag — safe to call on every launch;
    /// short-circuits immediately once migration has succeeded once. The
    /// flag is only set after a successful save (or when there is nothing
    /// to migrate), so a transient fetch/save failure retries next launch.
    /// Returns the count rewritten on this call.
    @discardableResult
    @MainActor
    func migrateLegacyPhotoFilePaths() -> Int {
        let defaults = UserDefaults.standard
        if defaults.bool(forKey: Self.migrationDoneKey) { return 0 }

        guard let context = modelContext else { return 0 }

        // Predicate-filtered fetch: skip the vast majority of queue rows
        // (non-photo creates, mappings, deletes) that have no photoFilePath
        // at all. SwiftData can't express string-prefix matching cleanly, so
        // the `hasPrefix("/")` guard stays in the loop.
        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { $0.photoFilePath != nil }
        )
        let candidateItems: [SyncQueueItem]
        do {
            candidateItems = try context.fetch(descriptor)
        } catch {
            slog("Failed to fetch queue for path migration",
                 category: .sync, level: .error, data: ["error": "\(error)"])
            // Don't set the done flag — retry next launch.
            return 0
        }

        var migratedCount = 0
        var unrecoverableCount = 0
        let marker = "/\(SyncFileManager.directoryName)/"
        for item in candidateItems {
            guard let stored = item.photoFilePath, stored.hasPrefix("/") else { continue }

            // Extract the `sync_pending_attachments/<id>/<filename>` suffix.
            // Don't trust that the absolute path still resolves on disk — we
            // only need to extract the suffix; the resolver will verify
            // existence at flush time.
            guard let markerRange = stored.range(of: marker) else {
                unrecoverableCount += 1
                slog("Path migration: legacy absolute path has no protected-dir marker",
                     category: .sync, level: .warning,
                     data: ["queueItemId": item.id.uuidString, "stored": stored])
                continue
            }
            let relative = String(stored[markerRange.lowerBound...].dropFirst())
            item.photoFilePath = relative
            migratedCount += 1
        }

        if migratedCount > 0 || unrecoverableCount > 0 {
            do {
                try context.save()
                slog("Migrated legacy photoFilePath entries to relative form",
                     category: .sync,
                     data: ["migrated": migratedCount,
                            "unrecoverable": unrecoverableCount,
                            "candidates": candidateItems.count])
            } catch {
                slog("Failed to save photoFilePath migration",
                     category: .sync, level: .error, data: ["error": "\(error)"])
                // Don't set the done flag — retry next launch.
                return migratedCount
            }
        }

        // Migration completed successfully (even if nothing needed rewriting).
        // Record so we skip the fetch on subsequent launches.
        defaults.set(true, forKey: Self.migrationDoneKey)
        return migratedCount
    }

    /// Remove queue items that can never succeed because they are missing the
    /// identifier (or mapping payload) needed to fetch the local entity. These
    /// orphans would otherwise loop through "Entity no longer exists" failures
    /// until max-retry deletes them — polluting the sync history in the meantime.
    ///
    /// Safe because:
    ///   * Entity orphans (nil nodeId/photoId/etc.) can't find a local row to sync.
    ///     StuckPhotoRecoveryService will re-enqueue photos that still need upload
    ///     with a proper photoId on the next online transition.
    ///   * Mapping orphans (nil mappingData) have no payload to send; the mapping
    ///     was either synced via SLDSyncService's bulk path or is already lost.
    ///
    /// Intended to run once per app process; the caller is responsible for gating.
    /// Returns the number of items removed.
    @discardableResult
    @MainActor
    func cleanupOrphanQueueItems() -> Int {
        guard let context = modelContext else { return 0 }

        let descriptor = FetchDescriptor<SyncQueueItem>()
        let allItems: [SyncQueueItem]
        do {
            allItems = try context.fetch(descriptor)
        } catch {
            slog("Failed to fetch queue for orphan cleanup", category: .sync, level: .error, data: ["error": "\(error)"])
            return 0
        }

        guard !allItems.isEmpty else { return 0 }

        var removedCount = 0
        for item in allItems where isOrphanQueueItem(item) {
            context.delete(item)
            removedCount += 1
        }

        if removedCount > 0 {
            do {
                try context.save()
                slog("Cleaned up orphan sync queue items", category: .sync, data: [
                    "removed_count": removedCount,
                    "remaining_count": allItems.count - removedCount
                ])
            } catch {
                slog("Failed to save orphan cleanup", category: .sync, level: .error, data: ["error": "\(error)"])
            }
        }
        return removedCount
    }

    /// True if the queue item is missing the identity fields required for its target.
    private func isOrphanQueueItem(_ item: SyncQueueItem) -> Bool {
        let target = item.target
        if target.isMapping {
            // Mapping payload is required; nil or un-parseable JSON means the item can
            // never be sent to the server.
            return item.mappingData == nil
        }
        switch target {
        case .node: return item.nodeId == nil
        case .edge: return item.edgeId == nil
        case .photo: return item.photoId == nil
        case .userTask: return item.userTaskId == nil
        case .irPhoto: return item.irPhotoId == nil
        case .irSession: return item.irSessionId == nil
        case .issue: return item.issueId == nil
        case .quote: return item.quoteId == nil
        case .formInstance: return item.formInstanceId == nil
        case .egFormInstance: return item.egFormInstanceId == nil
        case .building: return item.buildingId == nil
        case .floor: return item.floorId == nil
        case .room: return item.roomId == nil
        case .attachment: return item.attachmentId == nil
        case .sldView: return item.sldViewId == nil
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask,
             .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance,
             .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode,
             .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession,
             .mappingTaskNodeBulkCompletion,
             .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            // Handled above via target.isMapping — this branch is unreachable
            // but the switch must be exhaustive.
            return false
        }
    }

    /// Re-queue a failed operation from sync log
    @MainActor
    func requeueFailedOperation(from log: SyncLog) -> Bool {
        guard let context = modelContext else {
            slog("No model context available for re-queuing", category: .sync, level: .error)
            return false
        }
        
        // Check if this operation already exists in the queue
        if let entityId = log.entityId {
            // Simplify the predicate to avoid compiler timeout
            let targetValue = log.targetRawValue
            let operationValue = log.operationRawValue
            
            let descriptor = FetchDescriptor<SyncQueueItem>(
                predicate: #Predicate { item in
                    item.targetRawValue == targetValue &&
                    item.operationRawValue == operationValue
                }
            )
            
            do {
                let existingItems = try context.fetch(descriptor)
                // Filter further in memory to check entityId
                let matching = existingItems.filter { item in
                    // Check the appropriate entity ID based on target
                    switch log.target {
                    case .node:
                        return item.nodeId == entityId
                    case .edge:
                        return item.edgeId == entityId
                    case .photo:
                        return item.photoId == entityId
                    case .userTask:
                        return item.userTaskId == entityId
                    case .irPhoto:
                        return item.irPhotoId == entityId
                    case .irSession:
                        return item.irSessionId == entityId
                    case .issue:
                        return item.issueId == entityId
                    case .quote:
                        return item.quoteId == entityId
                    case .formInstance:
                        return item.formInstanceId == entityId
                    case .egFormInstance:
                        return item.egFormInstanceId == entityId
                    case .building:
                        return item.buildingId == entityId
                    case .floor:
                        return item.floorId == entityId
                    case .room:
                        return item.roomId == entityId
                    case .attachment:
                        return item.attachmentId == entityId
                    case .sldView:
                        return item.sldViewId == entityId
                    case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
                        return false // Mappings handled differently
                    }
                }

                if !matching.isEmpty {
                    slog("Operation already exists in queue, skipping re-queue", category: .sync, level: .warning)
                    return false
                }
            } catch {
                slog("Error checking for existing queue items", category: .sync, level: .error, data: ["error": "\(error)"])
            }
        }
        
        // Create new queue item with proper entity ID.
        // ZP-1847: the failed log carries the original snapshot context
        // (snapshotJSON, photoFilePath, userId, siteId). Preserve it on the
        // re-queued row so the next flush can use the snapshot fast-path
        // even if the local entity has since been wiped.
        var queueItem: SyncQueueItem

        switch log.target {
        case .node:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, nodeId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .edge:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, edgeId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .photo:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, photoId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .userTask:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, userTaskId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .irPhoto:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, irPhotoId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .irSession:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, irSessionId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .issue:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, issueId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .quote:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, quoteId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .formInstance:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, formInstanceId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .egFormInstance:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, egFormInstanceId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .building:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, buildingId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .floor:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, floorId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .room:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, roomId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .attachment:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, attachmentId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .sldView:
            queueItem = SyncQueueItem(target: log.target, operation: log.operation, sldViewId: log.entityId, snapshotJSON: log.snapshotJSON, snapshotTimestamp: log.timestamp, photoFilePath: log.photoFilePath)
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            // Mapping payload cannot be reconstructed from a SyncLog alone. Refuse rather than
            // insert an orphan SyncQueueItem with nil mappingData, which would immediately fail
            // with "Entity no longer exists" and pollute the history.
            slog("Cannot re-queue mapping operation: payload not recoverable from log", category: .sync, level: .warning, data: [
                "target": "\(log.target)",
                "operation": "\(log.operation)"
            ])
            return false
        }
        queueItem.retryCount = 0 // Reset retry count for re-queued items
        // ZP-1847: copy user/site so per-user filter at flush still sees this item.
        queueItem.userId = log.userId
        queueItem.siteId = log.siteId

        context.insert(queueItem)
        
        do {
            try context.save()
            slog("Re-queued failed operation", category: .sync, data: [
                "target": "\(log.target)",
                "operation": "\(log.operation)"
            ])
            return true
        } catch {
            slog("Failed to save re-queued operation", category: .sync, level: .error, data: ["error": "\(error)"])
            return false
        }
    }
    
    /// Re-queue multiple failed operations
    @MainActor
    func requeueFailedOperations(from logs: [SyncLog]) -> (successful: Int, failed: Int) {
        var successful = 0
        var failed = 0
        
        for log in logs {
            if requeueFailedOperation(from: log) {
                successful += 1
            } else {
                failed += 1
            }
        }
        
        slog("Re-queue results", category: .sync, data: ["successful": successful, "failed": failed])
        return (successful, failed)
    }
    
    /// Increment retry count for a failed sync operation.
    ///
    /// ZP-2097 — strict-stop. retryCount is now an informational counter
    /// only ("Retried N times" chip in the UI). It no longer triggers
    /// auto-deletion at any threshold; items live in the queue until the
    /// user explicitly resolves them via Retry or Delete.
    func incrementRetryCount(for queueItemId: UUID) async {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.id == queueItemId }
        )

        do {
            if let item = try context.fetch(descriptor).first {
                item.retryCount += 1
                try context.save()
            }
        } catch {
            slog("Failed to increment retry count", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }

    /// ZP-2097 / ZP-1265 (aa123c2) — record a failed flush attempt on the
    /// queue row itself so the Pending UI can render Retry / Delete inline.
    /// Sets `lastFailureMessage` / `lastFailureAt` / `lastFailureCode` and
    /// bumps `retryCount` by `attemptsRun` so the "Retried N times" chip
    /// reflects every individual HTTP attempt (matching Android's
    /// `persistFinalFailure`). Never deletes — items live until the user
    /// resolves them via Retry or Delete.
    ///
    /// The bumped retry count is clamped to `SyncConfiguration.maxRetryAttempts`
    /// so the chip plateaus at 3 instead of climbing every time the user taps
    /// Sync — matches Android's `persistFinalFailure`, which writes
    /// `retryCount = maxRetryAttempts` on terminal failure.
    func recordFailure(for queueItemId: UUID, message: String, code: Int?, attemptsRun: Int = 1) async {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.id == queueItemId }
        )

        do {
            if let item = try context.fetch(descriptor).first {
                // Mirror Android's 2000-char truncation so the UI doesn't
                // try to lay out massive payloads as inline failure text.
                item.lastFailureMessage = String(message.prefix(2000))
                item.lastFailureAt = Date()
                item.lastFailureCode = code
                let bumped = item.retryCount + max(1, attemptsRun)
                item.retryCount = min(bumped, SyncConfiguration.maxRetryAttempts)
                try context.save()
            }
        } catch {
            slog("Failed to record sync failure", category: .sync, level: .error,
                 data: ["queue_item_id": queueItemId.uuidString, "error": "\(error)"])
        }
    }

    /// ZP-2097 — used by the per-item Retry action. Clears strict-stop
    /// failure fields so the UI doesn't show stale failure text while the
    /// new attempt is in flight. retryCount is preserved as a historical
    /// signal.
    func clearFailureFields(for queueItemId: UUID) async {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.id == queueItemId }
        )

        do {
            if let item = try context.fetch(descriptor).first {
                item.lastFailureMessage = nil
                item.lastFailureAt = nil
                item.lastFailureCode = nil
                try context.save()
            }
        } catch {
            slog("Failed to clear failure fields", category: .sync, level: .error,
                 data: ["queue_item_id": queueItemId.uuidString, "error": "\(error)"])
        }
    }
    
    /// Fetch the actual entity for a queue item
    func fetchEntityForQueueItem(_ queueItem: SyncQueueItem) -> Any? {
        guard let context = modelContext else { return nil }
        
        do {
            switch queueItem.target {
            case .node:
                if let nodeId = queueItem.nodeId {
                    let descriptor = FetchDescriptor<NodeV2>(
                        predicate: #Predicate<NodeV2> { n in
                            n.id == nodeId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .edge:
                if let edgeId = queueItem.edgeId {
                    let descriptor = FetchDescriptor<EdgeV2>(
                        predicate: #Predicate<EdgeV2> { e in
                            e.id == edgeId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .photo:
                if let photoId = queueItem.photoId {
                    let descriptor = FetchDescriptor<Photo>(
                        predicate: #Predicate<Photo> { p in
                            p.id == photoId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .userTask:
                if let taskId = queueItem.userTaskId {
                    let descriptor = FetchDescriptor<UserTask>(
                        predicate: #Predicate<UserTask> { t in
                            t.id == taskId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .irPhoto:
                if let irPhotoId = queueItem.irPhotoId {
                    let descriptor = FetchDescriptor<IRPhoto>(
                        predicate: #Predicate<IRPhoto> { ip in
                            ip.id == irPhotoId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .irSession:
                if let sessionId = queueItem.irSessionId {
                    let descriptor = FetchDescriptor<IRSession>(
                        predicate: #Predicate<IRSession> { s in
                            s.id == sessionId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .issue:
                if let issueId = queueItem.issueId {
                    let descriptor = FetchDescriptor<Issue>(
                        predicate: #Predicate<Issue> { i in
                            i.id == issueId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .quote:
                if let quoteId = queueItem.quoteId {
                    let descriptor = FetchDescriptor<Quote>(
                        predicate: #Predicate<Quote> { q in
                            q.id == quoteId
                        }
                    )
                    return try context.fetch(descriptor).first
                }
                
            case .formInstance:
                if let formInstanceId = queueItem.formInstanceId {
                    let descriptor = FetchDescriptor<FormInstance>(
                        predicate: #Predicate<FormInstance> { fi in
                            fi.id == formInstanceId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .egFormInstance:
                if let egFormInstanceId = queueItem.egFormInstanceId {
                    let descriptor = FetchDescriptor<EGFormInstance>(
                        predicate: #Predicate<EGFormInstance> { fi in
                            fi.id == egFormInstanceId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .building:
                if let buildingId = queueItem.buildingId {
                    let descriptor = FetchDescriptor<Building>(
                        predicate: #Predicate<Building> { b in
                            b.id == buildingId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .floor:
                if let floorId = queueItem.floorId {
                    let descriptor = FetchDescriptor<Floor>(
                        predicate: #Predicate<Floor> { f in
                            f.id == floorId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .room:
                if let roomId = queueItem.roomId {
                    let descriptor = FetchDescriptor<Room>(
                        predicate: #Predicate<Room> { r in
                            r.id == roomId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .attachment:
                if let attachmentId = queueItem.attachmentId {
                    let descriptor = FetchDescriptor<Attachment>(
                        predicate: #Predicate<Attachment> { a in
                            a.id == attachmentId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .sldView:
                if let sldViewId = queueItem.sldViewId {
                    let descriptor = FetchDescriptor<SLDViewV2>(
                        predicate: #Predicate<SLDViewV2> { v in
                            v.id == sldViewId
                        }
                    )
                    return try context.fetch(descriptor).first
                }

            case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
                // For mappings, return the mapping data itself
                return queueItem.mappingData
            }

            return nil

        } catch {
            slog("Error fetching entity", category: .sync, level: .error, data: ["error": "\(error)"])
            return nil
        }
    }

    // MARK: - Convenience Enqueue Methods for SLD View Mappings

    /// Enqueue a node-view position update
    func enqueueNodeSLDViewUpdate(
        mappingId: UUID,
        nodeId: UUID,
        viewId: UUID,
        x: Double,
        y: Double,
        width: Double?,
        height: Double?,
        isCollapsed: Bool = false
    ) {
        let mappingData = MappingData.nodeSLDView(
            mappingId: mappingId,
            nodeId: nodeId,
            viewId: viewId,
            x: x,
            y: y,
            width: width,
            height: height,
            isCollapsed: isCollapsed,
            isDeleted: false
        )
        let op = SyncOp(
            target: .mappingNodeSLDView,
            operation: .update,
            mappingData: mappingData
        )
        enqueue(op)
    }

    /// Enqueue a node-view mapping creation
    func enqueueNodeSLDViewCreate(
        mappingId: UUID,
        nodeId: UUID,
        viewId: UUID,
        x: Double,
        y: Double,
        width: Double?,
        height: Double?
    ) {
        let mappingData = MappingData.nodeSLDView(
            mappingId: mappingId,
            nodeId: nodeId,
            viewId: viewId,
            x: x,
            y: y,
            width: width,
            height: height,
            isCollapsed: false,
            isDeleted: false
        )
        let op = SyncOp(
            target: .mappingNodeSLDView,
            operation: .create,
            mappingData: mappingData
        )
        enqueue(op)
    }

    /// Enqueue an edge-view points update
    func enqueueEdgeSLDViewUpdate(
        mappingId: UUID,
        edgeId: UUID,
        viewId: UUID,
        points: [[String: Double]]?,
        algorithm: String?
    ) {
        let mappingData = MappingData.edgeSLDView(
            mappingId: mappingId,
            edgeId: edgeId,
            viewId: viewId,
            points: points,
            algorithm: algorithm,
            isDeleted: false
        )
        let op = SyncOp(
            target: .mappingEdgeSLDView,
            operation: .update,
            mappingData: mappingData
        )
        enqueue(op)
    }

    // MARK: - Task Deletion Methods

    /// Delete a task queue item with proper cascade cleanup of all related queue items and entities
    func deleteTaskQueueItem(_ queueItem: SyncQueueItem) throws {
        guard queueItem.target == .userTask else {
            throw NSError(domain: "SyncQueueService", code: 1,
                         userInfo: [NSLocalizedDescriptionKey: "Not a task entity"])
        }

        guard let context = modelContext else {
            throw NSError(domain: "SyncQueueService", code: 2,
                         userInfo: [NSLocalizedDescriptionKey: "No model context available"])
        }

        guard let taskId = queueItem.userTaskId else {
            throw NSError(domain: "SyncQueueService", code: 3,
                         userInfo: [NSLocalizedDescriptionKey: "No task ID on queue item"])
        }

        // Fetch the task entity
        let task = fetchEntityForQueueItem(queueItem) as? UserTask

        // Determine if hard delete needed (CREATE operation + never synced)
        let shouldHardDelete = queueItem.operation == .create && (task?.lastSyncedAt == nil)

        // Collect form instance IDs from the task (for cascade cleanup)
        let formInstanceIds = task?.linkedFormInstances.map { $0.id } ?? []
        let photoIds = task?.photos.map { $0.id } ?? []

        // Find and delete all related sync queue items
        let relatedItemsDeleted = try deleteRelatedTaskQueueItems(
            taskId: taskId,
            formInstanceIds: formInstanceIds,
            photoIds: photoIds,
            excludingItemId: queueItem.id,
            context: context
        )

        if shouldHardDelete {
            // Hard delete form instances created with this task
            if let task = task {
                for formInstance in task.linkedFormInstances {
                    context.delete(formInstance)
                }
                for photo in task.photos {
                    context.delete(photo)
                }
                // Hard delete the task entity itself
                context.delete(task)
            }
        }

        // Delete the main queue item
        context.delete(queueItem)

        // Atomic save
        try context.save()

        slog("Deleted task queue item", category: .sync, data: [
            "taskId": taskId.uuidString,
            "relatedItemsDeleted": relatedItemsDeleted,
            "hardDeleted": shouldHardDelete
        ])
    }

    /// Private: Find and delete all sync queue items related to a task
    private func deleteRelatedTaskQueueItems(
        taskId: UUID,
        formInstanceIds: [UUID],
        photoIds: [UUID],
        excludingItemId: UUID,
        context: ModelContext
    ) throws -> Int {
        let allItems = try context.fetch(FetchDescriptor<SyncQueueItem>())
        var deletedCount = 0

        for item in allItems {
            if item.id == excludingItemId { continue }

            if shouldDeleteQueueItemForTask(item, taskId: taskId, formInstanceIds: formInstanceIds, photoIds: photoIds) {
                context.delete(item)
                deletedCount += 1
            }
        }

        return deletedCount
    }

    /// Private: Determine if a queue item is related to the given task
    private func shouldDeleteQueueItemForTask(
        _ item: SyncQueueItem,
        taskId: UUID,
        formInstanceIds: [UUID],
        photoIds: [UUID]
    ) -> Bool {
        // Match by userTaskId (other .userTask operations for the same task)
        if item.userTaskId == taskId {
            return true
        }

        // Match photo queue items for photos belonging to this task
        if let photoId = item.photoId, photoIds.contains(photoId) {
            return true
        }

        // Match formInstance queue items for form instances belonging to this task
        if let fiId = item.formInstanceId, formInstanceIds.contains(fiId) {
            return true
        }

        // Match mapping queue items where taskId is in mappingData
        if let mappingData = item.mappingData {
            if mappingData.taskId == taskId {
                return true
            }
            // Match formInstance-node mappings for form instances of this task
            if let fiId = mappingData.formInstanceId, formInstanceIds.contains(fiId) {
                return true
            }
        }

        return false
    }

    /// Count related queue items and entities for a task (used by UI for confirmation)
    func countTaskDescendants(for queueItem: SyncQueueItem) -> TaskDescendantCounts? {
        guard queueItem.target == .userTask,
              let taskId = queueItem.userTaskId else { return nil }

        let task = fetchEntityForQueueItem(queueItem) as? UserTask
        guard let task = task else { return nil }

        var counts = TaskDescendantCounts()
        counts.nodes = task.linkedNodes.filter { !$0.is_deleted }.count
        counts.formInstances = task.linkedFormInstances.count
        counts.photos = task.photos.count

        // Count related queue items
        if let context = modelContext {
            let allItems = (try? context.fetch(FetchDescriptor<SyncQueueItem>())) ?? []
            let formInstanceIds = task.linkedFormInstances.map { $0.id }
            let photoIds = task.photos.map { $0.id }

            for item in allItems {
                if item.id == queueItem.id { continue }
                if shouldDeleteQueueItemForTask(item, taskId: taskId, formInstanceIds: formInstanceIds, photoIds: photoIds) {
                    counts.relatedQueueItems += 1
                }
            }
        }

        return counts
    }

    // MARK: - Attachment Deletion Methods

    /// Delete an attachment queue item with cleanup of the local entity and related mapping queue items
    func deleteAttachmentQueueItem(_ queueItem: SyncQueueItem) throws {
        guard queueItem.target == .attachment else {
            throw NSError(domain: "SyncQueueService", code: 1,
                         userInfo: [NSLocalizedDescriptionKey: "Not an attachment entity"])
        }

        guard let context = modelContext else {
            throw NSError(domain: "SyncQueueService", code: 2,
                         userInfo: [NSLocalizedDescriptionKey: "No model context available"])
        }

        guard let attachmentId = queueItem.attachmentId else {
            throw NSError(domain: "SyncQueueService", code: 3,
                         userInfo: [NSLocalizedDescriptionKey: "No attachment ID on queue item"])
        }

        // Fetch the attachment entity
        let attachment = fetchEntityForQueueItem(queueItem) as? Attachment

        // Hard delete if this is a create operation (never synced to server)
        let shouldHardDelete = queueItem.operation == .create

        // Find and delete related attachment-node mapping queue items
        let allItems = try context.fetch(FetchDescriptor<SyncQueueItem>())
        var relatedItemsDeleted = 0

        for item in allItems {
            if item.id == queueItem.id { continue }

            // Remove mapping queue items that reference this attachment
            if item.target == .mappingAttachmentNode,
               let mappingData = item.mappingData,
               mappingData.attachmentId == attachmentId {
                context.delete(item)
                relatedItemsDeleted += 1
            }
        }

        if shouldHardDelete, let attachment = attachment {
            // Delete local file
            if let localPath = attachment.localFilePath {
                try? FileManager.default.removeItem(atPath: localPath)
            }

            // Delete related AttachmentNodeMapping entities
            let mappingDescriptor = FetchDescriptor<AttachmentNodeMapping>(
                predicate: #Predicate<AttachmentNodeMapping> { m in
                    m.attachmentId == attachmentId
                }
            )
            let mappings = try context.fetch(mappingDescriptor)
            for mapping in mappings {
                context.delete(mapping)
            }

            // Hard delete the attachment entity
            context.delete(attachment)
        }

        // Delete the main queue item
        context.delete(queueItem)

        // Atomic save
        try context.save()

        slog("Deleted attachment queue item", category: .sync, data: [
            "attachmentId": attachmentId.uuidString,
            "relatedItemsDeleted": relatedItemsDeleted,
            "hardDeleted": shouldHardDelete
        ])
    }

    // MARK: - Issue Deletion Methods

    /// Delete an issue queue item with proper cascade cleanup of all related queue items and entities
    func deleteIssueQueueItem(_ queueItem: SyncQueueItem) throws {
        guard queueItem.target == .issue else {
            throw NSError(domain: "SyncQueueService", code: 1,
                         userInfo: [NSLocalizedDescriptionKey: "Not an issue entity"])
        }

        guard let context = modelContext else {
            throw NSError(domain: "SyncQueueService", code: 2,
                         userInfo: [NSLocalizedDescriptionKey: "No model context available"])
        }

        guard let issueId = queueItem.issueId else {
            throw NSError(domain: "SyncQueueService", code: 3,
                         userInfo: [NSLocalizedDescriptionKey: "No issue ID on queue item"])
        }

        // Fetch the issue entity
        let issue = fetchEntityForQueueItem(queueItem) as? Issue

        // Determine if hard delete needed (CREATE operation + never synced)
        let shouldHardDelete = queueItem.operation == .create && (issue?.lastSyncedAt == nil)

        // Collect related entity IDs for cascade cleanup
        let photoIds = issue?.photos.map { $0.id } ?? []
        let irPhotoIds = issue?.ir_photos.map { $0.id } ?? []

        // Find and delete all related sync queue items
        let relatedItemsDeleted = try deleteRelatedIssueQueueItems(
            issueId: issueId,
            photoIds: photoIds,
            irPhotoIds: irPhotoIds,
            excludingItemId: queueItem.id,
            context: context
        )

        if shouldHardDelete {
            if let issue = issue {
                // Hard delete photos created with this issue
                for photo in issue.photos {
                    context.delete(photo)
                }
                // Unlink IR photos (don't delete — they belong to the session)
                for irPhoto in issue.ir_photos {
                    irPhoto.issue = nil
                }
                // Hard delete the issue entity itself
                context.delete(issue)
            }
        }

        // Delete the main queue item
        context.delete(queueItem)

        // Atomic save
        try context.save()

        slog("Deleted issue queue item", category: .sync, data: [
            "issueId": issueId.uuidString,
            "relatedItemsDeleted": relatedItemsDeleted,
            "hardDeleted": shouldHardDelete
        ])
    }

    /// Private: Find and delete all sync queue items related to an issue
    private func deleteRelatedIssueQueueItems(
        issueId: UUID,
        photoIds: [UUID],
        irPhotoIds: [UUID],
        excludingItemId: UUID,
        context: ModelContext
    ) throws -> Int {
        let allItems = try context.fetch(FetchDescriptor<SyncQueueItem>())
        var deletedCount = 0

        for item in allItems {
            if item.id == excludingItemId { continue }

            if shouldDeleteQueueItemForIssue(item, issueId: issueId, photoIds: photoIds, irPhotoIds: irPhotoIds) {
                context.delete(item)
                deletedCount += 1
            }
        }

        return deletedCount
    }

    /// Private: Determine if a queue item is related to the given issue
    private func shouldDeleteQueueItemForIssue(
        _ item: SyncQueueItem,
        issueId: UUID,
        photoIds: [UUID],
        irPhotoIds: [UUID]
    ) -> Bool {
        // Match by issueId (other .issue operations for the same issue)
        if item.issueId == issueId {
            return true
        }

        // Match photo queue items for photos belonging to this issue
        if let photoId = item.photoId, photoIds.contains(photoId) {
            return true
        }

        // Match IR photo queue items for IR photos belonging to this issue
        if let irPhotoId = item.irPhotoId, irPhotoIds.contains(irPhotoId) {
            return true
        }

        // Match mapping queue items where issueId is in mappingData
        if let mappingData = item.mappingData, mappingData.issueId == issueId {
            return true
        }

        return false
    }

    /// Count related queue items and entities for an issue (used by UI for confirmation)
    func countIssueDescendants(for queueItem: SyncQueueItem) -> IssueDescendantCounts? {
        guard queueItem.target == .issue,
              let issueId = queueItem.issueId else { return nil }

        let issue = fetchEntityForQueueItem(queueItem) as? Issue
        guard let issue = issue else { return nil }

        var counts = IssueDescendantCounts()
        counts.photos = issue.photos.count
        counts.irPhotos = issue.ir_photos.count
        counts.tasks = issue.tasks.count

        // Count related queue items
        if let context = modelContext {
            let allItems = (try? context.fetch(FetchDescriptor<SyncQueueItem>())) ?? []
            let photoIds = issue.photos.map { $0.id }
            let irPhotoIds = issue.ir_photos.map { $0.id }

            for item in allItems {
                if item.id == queueItem.id { continue }
                if shouldDeleteQueueItemForIssue(item, issueId: issueId, photoIds: photoIds, irPhotoIds: irPhotoIds) {
                    counts.relatedQueueItems += 1
                }
            }
        }

        return counts
    }

    // MARK: - Location Deletion Methods

    /// Delete a location queue item with proper entity and relationship handling
    func deleteLocationQueueItem(_ queueItem: SyncQueueItem) throws {
        guard [.building, .floor, .room].contains(queueItem.target) else {
            throw NSError(domain: "SyncQueueService", code: 1,
                         userInfo: [NSLocalizedDescriptionKey: "Not a location entity"])
        }

        guard let context = modelContext else {
            throw NSError(domain: "SyncQueueService", code: 2,
                         userInfo: [NSLocalizedDescriptionKey: "No model context available"])
        }

        // Handle dependent child CREATE operations first (recursive)
        try deleteDependentQueueItems(for: queueItem, context: context)

        // Handle the single queue item
        try deleteSingleLocationQueueItem(queueItem, context: context)
    }

    /// Private: Handle a single location queue item deletion
    private func deleteSingleLocationQueueItem(_ queueItem: SyncQueueItem, context: ModelContext) throws {
        // Fetch entity
        let entity = fetchEntityForQueueItem(queueItem)

        // Determine if hard delete needed (CREATE operation + never synced)
        let shouldHardDelete = shouldPerformHardDelete(queueItem: queueItem, entity: entity)

        if shouldHardDelete {
            // Unlink relationships
            unlinkLocationRelationships(entity: entity, context: context)

            // Hard delete entity
            if let building = entity as? Building {
                context.delete(building)
            } else if let floor = entity as? Floor {
                context.delete(floor)
            } else if let room = entity as? Room {
                context.delete(room)
            }
        }

        // Delete queue item
        context.delete(queueItem)

        // Atomic save
        try context.save()

        slog("Deleted location queue item", category: .sync, data: [
            "target": "\(queueItem.target)",
            "operation": "\(queueItem.operation)",
            "hardDeleted": shouldHardDelete
        ])
    }

    /// Private: Check if entity should be hard deleted
    private func shouldPerformHardDelete(queueItem: SyncQueueItem, entity: Any?) -> Bool {
        // Only CREATE operations are candidates for hard delete
        guard queueItem.operation == .create else {
            return false
        }

        // Verify entity was never synced
        if let building = entity as? Building {
            return building.lastSyncedAt == nil
        } else if let floor = entity as? Floor {
            return floor.lastSyncedAt == nil
        } else if let room = entity as? Room {
            return room.lastSyncedAt == nil
        }

        return false
    }

    /// Private: Unlink all asset relationships for a location entity
    private func unlinkLocationRelationships(entity: Any?, context: ModelContext) {
        guard let entity = entity else { return }

        var affectedAssets: [NodeV2] = []

        // Collect affected assets based on entity type
        if let building = entity as? Building {
            for floor in building.floors {
                for room in floor.rooms {
                    affectedAssets.append(contentsOf: room.nodes)
                }
            }
        } else if let floor = entity as? Floor {
            for room in floor.rooms {
                affectedAssets.append(contentsOf: room.nodes)
            }
        } else if let room = entity as? Room {
            affectedAssets.append(contentsOf: room.nodes)
        }

        // Unlink all affected assets
        for asset in affectedAssets {
            asset.room = nil
            asset.needsSync = true
            asset.lastModifiedAt = Date()
        }

        if !affectedAssets.isEmpty {
            slog("Unlinked assets from location", category: .sync, data: [
                "assetCount": affectedAssets.count
            ])
        }
    }

    /// Private: Delete dependent child queue items (for nested CREATE hierarchies)
    private func deleteDependentQueueItems(for queueItem: SyncQueueItem, context: ModelContext) throws {
        // Only for CREATE operations on buildings/floors
        guard queueItem.operation == .create else { return }

        if queueItem.target == .building, let buildingId = queueItem.buildingId {
            // Find all CREATE operations for child floors
            let floorTargetRaw = SyncTarget.floor.rawValue
            let createOpRaw = SyncOperation.create.rawValue
            let floorDescriptor = FetchDescriptor<SyncQueueItem>(
                predicate: #Predicate { item in
                    item.targetRawValue == floorTargetRaw &&
                    item.operationRawValue == createOpRaw
                }
            )
            let floorItems = try context.fetch(floorDescriptor)

            // Delete floors belonging to this building
            for floorItem in floorItems {
                if let floor = fetchEntityForQueueItem(floorItem) as? Floor,
                   floor.building?.id == buildingId {
                    try deleteLocationQueueItem(floorItem) // Recursive
                }
            }
        } else if queueItem.target == .floor, let floorId = queueItem.floorId {
            // Find all CREATE operations for child rooms
            let roomTargetRaw = SyncTarget.room.rawValue
            let createOpRaw = SyncOperation.create.rawValue
            let roomDescriptor = FetchDescriptor<SyncQueueItem>(
                predicate: #Predicate { item in
                    item.targetRawValue == roomTargetRaw &&
                    item.operationRawValue == createOpRaw
                }
            )
            let roomItems = try context.fetch(roomDescriptor)

            // Delete rooms belonging to this floor
            for roomItem in roomItems {
                if let room = fetchEntityForQueueItem(roomItem) as? Room,
                   room.floor?.id == floorId {
                    try deleteLocationQueueItem(roomItem) // Recursive
                }
            }
        }
    }
}
