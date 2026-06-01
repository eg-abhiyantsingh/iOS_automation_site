import Foundation
import SwiftData
import Combine

/// Service responsible for executing sync operations with the backend
@MainActor
final class SyncExecutionService {
    private let api = APIClient.shared
    private let photoUploadService: PhotoUploadService
    private var modelContext: ModelContext?
    
    // Dependencies for orchestration
    private var deduplicationService: SyncDeduplicationService?
    private var syncQueueService: SyncQueueService?
    
    // Sync progress tracking
    @Published var isSyncing: Bool = false
    @Published var syncProgress: Int = 0
    @Published var syncTotal: Int = 0
    
    init(modelContext: ModelContext? = nil, photoUploadService: PhotoUploadService? = nil) {
        self.modelContext = modelContext
        self.photoUploadService = photoUploadService ?? PhotoUploadService()
    }
    
    /// Set or update the model context
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
        photoUploadService.setModelContext(context)
    }
    
    /// Set service dependencies for orchestration
    func setDependencies(deduplicationService: SyncDeduplicationService?, syncQueueService: SyncQueueService?) {
        self.deduplicationService = deduplicationService
        self.syncQueueService = syncQueueService
    }
    
    /// Process a single sync operation with retry logic
    func processSyncOperationWithRetry(_ op: SyncOp, maxRetries: Int = SyncConfiguration.maxRetryAttempts) async -> SyncResult {
        _ = Date()
        var lastError: Error?
        var resolvedEntityId: UUID?

        // Photo creates require a binary PUT to S3 *before* the metadata POST is
        // meaningful. The plain snapshot fast-path (replay metadata JSON only)
        // would post `url: null`, leaving cross-platform with broken thumbnails.
        // Mirrors Android's `FlushSyncQueueUseCase.processPhotoItem` snapshot
        // branch: snapshot + preserved file path → presigned URL → S3 PUT →
        // metadata POST with `url` filled in. Survives site switch because
        // neither the live Photo entity nor the original `Documents/photos/`
        // file is read here — only the queue's snapshot + preserved copy.
        if op.target == .photo,
           op.operation == .create,
           let payload = op.snapshotJSON,
           let storedPath = op.photoFilePath {
            // Resolve the stored path (relative or legacy-absolute) against
            // the current Data container. Legacy absolute paths written by
            // v1.33 and earlier point at a defunct container UUID after an
            // app update; the resolver extracts the `sync_pending_attachments/…`
            // suffix and re-binds it to the current Documents directory.
            guard let resolved = SyncFileManager.shared.resolveStoredPath(storedPath) else {
                let error = SyncError.fileNotFound(storedPath)
                slog("Photo upload: preserved file not found after path resolution",
                     category: .sync, level: .error,
                     data: ["queueItemId": op.queueItemId?.uuidString ?? "nil",
                            "stored": storedPath])
                return SyncResult(success: false, operation: op, error: error, entityId: op.entityId, attemptsRun: 1)
            }

            // Self-heal: rewrite the queue row to relative form so future
            // attempts don't have to re-extract.
            //
            // Belt-and-suspenders with `migrateLegacyPhotoFilePaths` (launch
            // migration in SyncQueueService): the startup migration handles
            // the bulk case, but a queue flush can race ahead of it on the
            // first launch (or the migration save can fail transiently),
            // and a SyncLog→SyncQueueItem rebuild can re-introduce a legacy
            // absolute path after migration. This branch makes recovery
            // self-sufficient regardless of those edge cases.
            if resolved.recoveredFromLegacyPath,
               let queueItemId = op.queueItemId,
               let context = modelContext {
                let relative = "\(SyncFileManager.directoryName)/\(queueItemId.uuidString)/\(resolved.url.lastPathComponent)"
                let descriptor = FetchDescriptor<SyncQueueItem>(
                    predicate: #Predicate<SyncQueueItem> { $0.id == queueItemId }
                )
                if let item = try? context.fetch(descriptor).first {
                    item.photoFilePath = relative
                    do {
                        try context.save()
                    } catch {
                        // Non-fatal: idempotent — the next flush will recover
                        // and rewrite again. Logged for forensics.
                        slog("Self-heal: failed to persist relative path rewrite",
                             category: .sync, level: .warning,
                             data: ["queueItemId": queueItemId.uuidString,
                                    "error": "\(error)"])
                    }
                }
            }

            let filePath = resolved.url.path
            var attemptsRun = 0
            for attempt in 1...maxRetries {
                attemptsRun = attempt
                do {
                    try await photoUploadService.uploadPhotoFromSnapshot(
                        snapshotJSON: payload,
                        filePath: filePath
                    )
                    return SyncResult(success: true, operation: op, error: nil, entityId: op.entityId, attemptsRun: attemptsRun)
                } catch {
                    lastError = error

                    if error is AuthError { break }
                    if let urlError = error as? URLError {
                        if urlError.code == .userAuthenticationRequired { break }
                        if (400..<500).contains(urlError.code.rawValue) { break }
                        if urlError.code == .fileDoesNotExist
                            || urlError.code == .unsupportedURL
                            || urlError.code == .cannotFindHost { break }
                    }
                    // Preserved file vanished — no point retrying.
                    if case SyncError.fileNotFound = error { break }
                    if case SyncError.fileTooLarge = error { break }
                    if attempt < maxRetries {
                        let delay = SyncConfiguration.retryDelay * Double(attempt)
                        try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                    }
                }
            }

            if let error = lastError {
                SentryLogger.shared.captureSyncFailure(
                    target: op.target,
                    operation: op.operation,
                    entityId: op.entityId,
                    error: error,
                    retryCount: maxRetries,
                    httpStatusCode: extractHttpStatus(from: error),
                    queueItemId: op.queueItemId,
                    userId: op.userId,
                    siteId: op.siteId
                )
            }
            return SyncResult(success: false, operation: op, error: lastError, entityId: op.entityId, attemptsRun: attemptsRun)
        }

        // ZP-1847 — Snapshot fast-path. When the queue row carries a captured
        // request body, replay it directly to the original endpoint. No live
        // SwiftData fetch required, so the op succeeds even if the local
        // entity tables were wiped after a site switch / logout / account
        // switch.
        //
        // Excluded: `.photo + .create`. The metadata-only replay would post
        // `url: null` because the binary still has to be PUT to S3. That op
        // is handled by the dedicated photo branch above when a preserved
        // file is available; otherwise it falls through to the legacy
        // entity-fetch path which calls `PhotoUploadService.uploadPhoto`.
        let isPhotoCreate = op.target == .photo && op.operation == .create
        if let payload = op.snapshotJSON, let entityId = op.entityId, !op.target.isMapping, !isPhotoCreate {
            var attemptsRun = 0
            for attempt in 1...maxRetries {
                attemptsRun = attempt
                do {
                    _ = try await api.replaySnapshotPayload(
                        target: op.target,
                        operation: op.operation,
                        entityId: entityId,
                        payload: payload
                    )
                    return SyncResult(success: true, operation: op, error: nil, entityId: entityId, attemptsRun: attemptsRun)
                } catch {
                    lastError = error

                    if error is AuthError { break }
                    if let urlError = error as? URLError {
                        if urlError.code == .userAuthenticationRequired { break }
                        if (400..<500).contains(urlError.code.rawValue) { break }
                        if urlError.code == .fileDoesNotExist
                            || urlError.code == .unsupportedURL
                            || urlError.code == .cannotFindHost { break }
                    }
                    if attempt < maxRetries {
                        let delay = SyncConfiguration.retryDelay * Double(attempt)
                        try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                    }
                }
            }

            if let error = lastError {
                SentryLogger.shared.captureSyncFailure(
                    target: op.target,
                    operation: op.operation,
                    entityId: entityId,
                    error: error,
                    retryCount: maxRetries,
                    httpStatusCode: extractHttpStatus(from: error),
                    queueItemId: op.queueItemId,
                    userId: op.userId,
                    siteId: op.siteId
                )
            }
            return SyncResult(success: false, operation: op, error: lastError, entityId: entityId, attemptsRun: attemptsRun)
        }

        var attemptsRunLegacy = 0
        for attempt in 1...maxRetries {
            attemptsRunLegacy = attempt
            do {
                // Fetch and validate entity just before use
                let (entity, isValid, entityId) = await fetchEntityForSync(op)
                resolvedEntityId = entityId

                guard isValid else {
                    // For CREATE operations, missing entity is a failure - the entity must exist in SwiftData to be created on server
                    // For UPDATE/DELETE operations, missing entity means it was already deleted locally, which is acceptable
                    if op.operation == .create {
                        slog("CREATE operation failed - entity not found in SwiftData", category: .sync, level: .error, data: [
                            "target": "\(op.target)",
                            "queueItemId": op.queueItemId?.uuidString ?? "nil"
                        ])
                        return SyncResult(success: false, operation: op, error: SyncError.missingEntity, entityId: resolvedEntityId, attemptsRun: attemptsRunLegacy)
                    }
                    // Entity doesn't exist but operation is update/delete - consider it successful (already deleted)
                    return SyncResult(success: true, operation: op, error: nil, entityId: resolvedEntityId, attemptsRun: attemptsRunLegacy)
                }

                // Perform the sync with the fetched entity
                try await performSyncOperation(op, entity: entity)

                // Success
                return SyncResult(success: true, operation: op, error: nil, entityId: resolvedEntityId, attemptsRun: attemptsRunLegacy)

            } catch {
                lastError = error
                slog("Sync attempt failed", category: .sync, level: .warning, data: [
                    "attempt": attempt,
                    "max_retries": maxRetries,
                    "target": "\(op.target)",
                    "error": error.localizedDescription
                ])

                // Don't retry on authentication errors - session needs re-auth, retrying won't help
                if error is AuthError {
                    slog("Authentication error - stopping retries, session needs re-authentication", category: .sync, level: .error)
                    break
                }

                // Don't retry on client errors (4xx) or specific permanent failures
                if let urlError = error as? URLError {
                    // Authentication-related URLErrors should also not be retried
                    if urlError.code == .userAuthenticationRequired {
                        slog("Authentication required - stopping retries", category: .sync, level: .error)
                        break
                    }
                    // Client errors that shouldn't be retried
                    if (400..<500).contains(urlError.code.rawValue) {
                        slog("Client error (4xx), skipping remaining retries", category: .sync, level: .warning)
                        break
                    }
                    // Other permanent failures
                    if urlError.code == .fileDoesNotExist ||
                       urlError.code == .unsupportedURL ||
                       urlError.code == .cannotFindHost {
                        slog("Permanent failure, skipping remaining retries", category: .sync, level: .warning)
                        break
                    }
                }

                // Wait before retry (exponential backoff)
                if attempt < maxRetries {
                    let delay = SyncConfiguration.retryDelay * Double(attempt)
                    try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                }
            }
        }

        // All attempts failed - capture to Sentry for debugging
        if let error = lastError {
            SentryLogger.shared.captureSyncFailure(
                target: op.target,
                operation: op.operation,
                entityId: resolvedEntityId ?? getEntityId(from: op),
                error: error,
                retryCount: maxRetries,
                httpStatusCode: extractHttpStatus(from: error),
                queueItemId: op.queueItemId
            )
        }

        return SyncResult(success: false, operation: op, error: lastError, entityId: resolvedEntityId, attemptsRun: attemptsRunLegacy)
    }

    /// Extract entity ID from sync operation
    private func getEntityId(from op: SyncOp) -> UUID? {
        if let id = op.node?.id { return id }
        if let id = op.edge?.id { return id }
        if let id = op.photo?.id { return id }
        if let id = op.userTask?.id { return id }
        if let id = op.irPhoto?.id { return id }
        if let id = op.irSession?.id { return id }
        if let id = op.issue?.id { return id }
        if let id = op.quote?.id { return id }
        if let id = op.formInstance?.id { return id }
        if let id = op.building?.id { return id }
        if let id = op.floor?.id { return id }
        if let id = op.room?.id { return id }
        if let id = op.sldView?.id { return id }
        return nil
    }

    /// Extract HTTP status code from error if available
    private func extractHttpStatus(from error: Error?) -> Int? {
        guard let error = error else { return nil }

        if let urlError = error as? URLError {
            let code = urlError.code.rawValue
            return (400...599).contains(code) ? code : nil
        }

        if let nsError = error as NSError? {
            let code = nsError.code
            return (400...599).contains(code) ? code : nil
        }

        return nil
    }
    
    /// Fetch entity for sync operation
    private func fetchEntityForSync(_ op: SyncOp) async -> (entity: Any?, isValid: Bool, entityId: UUID?) {
        guard let context = modelContext else {
            slog("fetchEntityForSync failed - modelContext is nil", category: .sync, level: .error, data: [
                "target": "\(op.target)",
                "operation": "\(op.operation)"
            ])
            return (nil, false, nil)
        }

        guard let queueItemId = op.queueItemId else {
            slog("fetchEntityForSync failed - queueItemId is nil", category: .sync, level: .error, data: [
                "target": "\(op.target)",
                "operation": "\(op.operation)"
            ])
            return (nil, false, nil)
        }

        // First check if the queue item still exists
        let queueItemIdLocal = queueItemId
        let queueDescriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate<SyncQueueItem> { item in
                item.id == queueItemIdLocal
            }
        )

        do {
            guard let queueItem = try context.fetch(queueDescriptor).first else {
                slog("Queue item no longer exists", category: .sync, level: .warning)
                return (nil, false, nil)
            }

            // Extract entity ID from queue item for logging purposes
            let entityId: UUID? = {
                switch op.target {
                case .node: return queueItem.nodeId
                case .edge: return queueItem.edgeId
                case .photo: return queueItem.photoId
                case .userTask: return queueItem.userTaskId
                case .irPhoto: return queueItem.irPhotoId
                case .irSession: return queueItem.irSessionId
                case .issue: return queueItem.issueId
                case .quote: return queueItem.quoteId
                case .formInstance: return queueItem.formInstanceId
                case .egFormInstance: return queueItem.egFormInstanceId
                case .building: return queueItem.buildingId
                case .floor: return queueItem.floorId
                case .room: return queueItem.roomId
                case .attachment: return queueItem.attachmentId
                case .sldView: return queueItem.sldViewId
                case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
                    return nil
                }
            }()
            
            // Now fetch the actual entity based on stored ID
            switch op.target {
            case .node:
                if let nodeId = queueItem.nodeId {
                    let descriptor = FetchDescriptor<NodeV2>(
                        predicate: #Predicate<NodeV2> { n in
                            n.id == nodeId
                        }
                    )
                    if let node = try context.fetch(descriptor).first {
                        return (node, true, entityId)
                    } else {
                        slog("Node not found in SwiftData", category: .sync, level: .error, data: [
                            "nodeId": nodeId.uuidString,
                            "queueItemId": queueItem.id.uuidString
                        ])
                    }
                } else {
                    slog("SyncQueueItem has no nodeId", category: .sync, level: .error, data: [
                        "queueItemId": queueItem.id.uuidString,
                        "target": "\(queueItem.target)"
                    ])
                }

            case .edge:
                if let edgeId = queueItem.edgeId {
                    let descriptor = FetchDescriptor<EdgeV2>(
                        predicate: #Predicate<EdgeV2> { e in
                            e.id == edgeId
                        }
                    )
                    if let edge = try context.fetch(descriptor).first {
                        return (edge, true, entityId)
                    }
                }

            case .photo:
                if let photoId = queueItem.photoId {
                    let descriptor = FetchDescriptor<Photo>(
                        predicate: #Predicate<Photo> { p in
                            p.id == photoId
                        }
                    )
                    if let photo = try context.fetch(descriptor).first {
                        // For create operations, verify file exists
                        if op.operation == .create {
                            if !photoUploadService.validatePhotoFile(photo) {
                                slog("Photo file missing from disk", category: .photo, level: .error, data: [
                                    "photo_id": photo.id.uuidString,
                                    "filename": photo.filename ?? "nil",
                                    "local_filepath": photo.local_filepath ?? "nil",
                                    "queueItemId": queueItem.id.uuidString
                                ])
                                return (nil, false, entityId)
                            }
                        }
                        return (photo, true, entityId)
                    } else {
                        slog("Photo not found in SwiftData", category: .sync, level: .error, data: [
                            "photoId": photoId.uuidString,
                            "queueItemId": queueItem.id.uuidString
                        ])
                    }
                } else {
                    slog("SyncQueueItem has no photoId", category: .sync, level: .error, data: [
                        "queueItemId": queueItem.id.uuidString,
                        "target": "\(queueItem.target)"
                    ])
                }

            case .userTask:
                if let taskId = queueItem.userTaskId {
                    let descriptor = FetchDescriptor<UserTask>(
                        predicate: #Predicate<UserTask> { t in
                            t.id == taskId
                        }
                    )
                    if let task = try context.fetch(descriptor).first {
                        return (task, true, entityId)
                    }
                }

            case .irPhoto:
                if let irPhotoId = queueItem.irPhotoId {
                    let descriptor = FetchDescriptor<IRPhoto>(
                        predicate: #Predicate<IRPhoto> { ip in
                            ip.id == irPhotoId
                        }
                    )
                    if let irPhoto = try context.fetch(descriptor).first {
                        return (irPhoto, true, entityId)
                    }
                }

            case .irSession:
                if let sessionId = queueItem.irSessionId {
                    let descriptor = FetchDescriptor<IRSession>(
                        predicate: #Predicate<IRSession> { s in
                            s.id == sessionId
                        }
                    )
                    if let session = try context.fetch(descriptor).first {
                        return (session, true, entityId)
                    }
                }

            case .issue:
                if let issueId = queueItem.issueId {
                    let descriptor = FetchDescriptor<Issue>(
                        predicate: #Predicate<Issue> { i in
                            i.id == issueId
                        }
                    )
                    if let issue = try context.fetch(descriptor).first {
                        return (issue, true, entityId)
                    }
                }

            case .quote:
                if let quoteId = queueItem.quoteId {
                    let descriptor = FetchDescriptor<Quote>(
                        predicate: #Predicate<Quote> { q in
                            q.id == quoteId
                        }
                    )
                    if let quote = try context.fetch(descriptor).first {
                        return (quote, true, entityId)
                    }
                }

            case .formInstance:
                if let formInstanceId = queueItem.formInstanceId {
                    let descriptor = FetchDescriptor<FormInstance>(
                        predicate: #Predicate<FormInstance> { fi in
                            fi.id == formInstanceId
                        }
                    )
                    if let formInstance = try context.fetch(descriptor).first {
                        return (formInstance, true, entityId)
                    }
                }

            case .egFormInstance:
                if let egFormInstanceId = queueItem.egFormInstanceId {
                    let descriptor = FetchDescriptor<EGFormInstance>(
                        predicate: #Predicate<EGFormInstance> { fi in
                            fi.id == egFormInstanceId
                        }
                    )
                    if let egFormInstance = try context.fetch(descriptor).first {
                        return (egFormInstance, true, entityId)
                    }
                }

            case .building:
                slog("fetchEntityForSync - case .building", category: .sync, level: .debug)
                if let buildingId = queueItem.buildingId {
                    slog("Queue item has buildingId", category: .sync, level: .debug, data: ["building_id": buildingId.uuidString])
                    let descriptor = FetchDescriptor<Building>(
                        predicate: #Predicate<Building> { b in
                            b.id == buildingId
                        }
                    )
                    if let building = try context.fetch(descriptor).first {
                        slog("Found building", category: .sync, level: .debug, data: ["name": building.name])
                        return (building, true, entityId)
                    } else {
                        slog("Building not found in context", category: .sync, level: .warning, data: ["building_id": buildingId.uuidString])
                    }
                } else {
                    slog("Queue item has no buildingId", category: .sync, level: .warning)
                }

            case .floor:
                if let floorId = queueItem.floorId {
                    let descriptor = FetchDescriptor<Floor>(
                        predicate: #Predicate<Floor> { f in
                            f.id == floorId
                        }
                    )
                    if let floor = try context.fetch(descriptor).first {
                        return (floor, true, entityId)
                    }
                }

            case .room:
                if let roomId = queueItem.roomId {
                    let descriptor = FetchDescriptor<Room>(
                        predicate: #Predicate<Room> { r in
                            r.id == roomId
                        }
                    )
                    if let room = try context.fetch(descriptor).first {
                        return (room, true, entityId)
                    }
                }

            case .attachment:
                if let attachmentId = queueItem.attachmentId {
                    let descriptor = FetchDescriptor<Attachment>(
                        predicate: #Predicate<Attachment> { a in
                            a.id == attachmentId
                        }
                    )
                    if let attachment = try context.fetch(descriptor).first {
                        // For create operations, verify file exists
                        if op.operation == .create {
                            if attachment.uploadNeeded, let localPath = attachment.localFilePath {
                                if !FileManager.default.fileExists(atPath: localPath) {
                                    slog("Attachment file missing from disk", category: .sync, level: .error, data: [
                                        "attachment_id": attachment.id.uuidString,
                                        "filename": attachment.filename,
                                        "local_filepath": localPath
                                    ])
                                    return (nil, false, entityId)
                                }
                            }
                        }
                        return (attachment, true, entityId)
                    }
                }

            case .sldView:
                if let sldViewId = queueItem.sldViewId {
                    let descriptor = FetchDescriptor<SLDViewV2>(
                        predicate: #Predicate<SLDViewV2> { v in
                            v.id == sldViewId
                        }
                    )
                    if let sldView = try context.fetch(descriptor).first {
                        return (sldView, true, entityId)
                    }
                }

            case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
                // For mappings, just return the mapping data
                if let mappingData = queueItem.mappingData {
                    return (mappingData, true, entityId)
                }
            }

            return (nil, false, entityId)

        } catch {
            slog("Error fetching entity", category: .sync, level: .error, data: ["error": error.localizedDescription])
            return (nil, false, nil)
        }
    }
    
    /// Perform the actual sync operation with pre-fetched entity
    private func performSyncOperation(_ op: SyncOp, entity: Any?) async throws {
        guard let entity = entity else {
            throw SyncError.missingEntity
        }
        
        switch op.target {
        case .node:
            guard let node = entity as? NodeV2 else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                // Skip create if node is already synced (needsSync = false)
                // This prevents retrying creates for nodes that were already successfully synced
                if !node.needsSync {
                    slog("Node already synced (needsSync=false), skipping create", category: .sync, data: [
                        "node_id": node.id.uuidString,
                        "label": node.label
                    ])
                    return  // Success - node already synced, queue item will be removed
                }
                _ = try await api.createNode(node: node)
            case .update:
                // Pass extraData if present (e.g., force_type_change flag)
                let extraData = op.extraData ?? [:]
                if !extraData.isEmpty {
                    slog("Updating node with extraData", category: .sync, data: ["node_id": node.id.uuidString, "extra_data": "\(extraData)"])
                }
                _ = try await api.updateNode(node, extraData: extraData)
            default:
                break
            }

        case .edge:
            guard let edge = entity as? EdgeV2 else { throw SyncError.missingEntity }
            // DEBUG: Track is_deleted value before API call
            slog("EDGE SYNC DEBUG - before API", category: .sync, data: [
                "edge_id": edge.id.uuidString,
                "operation": "\(op.operation)",
                "is_deleted": edge.is_deleted,
                "needsSync": edge.needsSync
            ])
            switch op.operation {
            case .create:
                // Skip create if edge is already synced (needsSync = false)
                if !edge.needsSync {
                    slog("Edge already synced (needsSync=false), skipping create", category: .sync, data: [
                        "edge_id": edge.id.uuidString
                    ])
                    return  // Success - edge already synced, queue item will be removed
                }
                _ = try await api.createEdge(edge: edge)
            case .update:
                _ = try await api.updateEdge(edge)
            default:
                break
            }
            
        case .photo:
            guard let photo = entity as? Photo else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                // Skip upload if photo is already synced (upload_needed = false)
                // This prevents retrying uploads for photos that were already successfully uploaded
                if !photo.upload_needed {
                    slog("Photo already synced (upload_needed=false), skipping upload", category: .photo, data: [
                        "photo_id": photo.id.uuidString,
                        "filename": photo.filename ?? "nil"
                    ])
                    return  // Success - photo already uploaded, queue item will be removed
                }

                do {
                    try await photoUploadService.uploadPhoto(photo)
                } catch {
                    // If photo already exists on server (duplicate), treat as success
                    if isPhotoAlreadyExistsError(error) {
                        slog("Photo already exists on server, marking as synced", category: .photo, data: [
                            "photo_id": photo.id.uuidString,
                            "filename": photo.filename ?? "nil"
                        ])
                        photo.upload_needed = false
                        if let context = modelContext {
                            try? context.save()
                        }
                        return  // Success - photo is already on server
                    }
                    throw error  // Re-throw other errors
                }
            case .update:
                _ = try await api.updatePhoto(photo)
                if photo.is_deleted {
                    await deleteFromContext(photo)
                }
            default:
                break
            }
            
        case .userTask:
            guard let userTask = entity as? UserTask else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createTask(task: userTask)
                await MainActor.run { userTask.lastSyncedAt = Date() }
            case .update:
                _ = try await api.updateTask(userTask)
                await MainActor.run { userTask.lastSyncedAt = Date() }
            default:
                break
            }
            
        case .irPhoto:
            guard let irPhoto = entity as? IRPhoto else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createIRPhoto(irPhoto: irPhoto)
            case .update:
                _ = try await api.updateIRPhoto(irPhoto)
                if irPhoto.is_deleted {
                    await deleteFromContext(irPhoto)
                }
            default:
                break
            }
            
        case .irSession:
            guard let irSession = entity as? IRSession else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createIRSession(irSession: irSession)
            case .update:
                _ = try await api.updateIRSession(irSession)
            default:
                break
            }
            
        case .issue:
            guard let issue = entity as? Issue else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createIssue(issue: issue)
                await MainActor.run { issue.lastSyncedAt = Date() }
            case .update:
                _ = try await api.updateIssue(issue)
                await MainActor.run { issue.lastSyncedAt = Date() }
                if issue.is_deleted {
                    await deleteFromContext(issue)
                }
            default:
                break
            }
            
        case .quote:
            guard let quote = entity as? Quote else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createQuote(quote: quote)
            case .update:
                _ = try await api.updateQuote(quote)
                if quote.is_deleted {
                    await deleteFromContext(quote)
                }
            default:
                break
            }

        case .formInstance:
            guard let formInstance = entity as? FormInstance else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                let dto = formInstance.toDTO()
                _ = try await api.createFormInstance(dto)
            case .update:
                let dto = formInstance.toDTO()
                _ = try await api.updateFormInstance(id: formInstance.id, dto: dto)
                if formInstance.is_deleted {
                    await deleteFromContext(formInstance)
                }
            default:
                break
            }

        case .egFormInstance:
            guard let inst = entity as? EGFormInstance else { throw SyncError.missingEntity }
            let submissionAny: AnyCodable? = {
                guard let s = inst.form_submission, !s.isEmpty,
                      let d = s.data(using: .utf8),
                      let parsed = try? JSONSerialization.jsonObject(with: d) else { return nil }
                return AnyCodable(parsed)
            }()
            let nodeIds = inst.linkedNodes.map { $0.id.uuidString }
            switch op.operation {
            case .create:
                // ZP-2425: same precedence as the snapshot encoder — prefer
                // extraData captured at enqueue time over the stale-prone
                // SwiftData inverse on `inst.linkedTasks`.
                let taskId = (op.extraData?["task_id"] as? String)
                    ?? inst.linkedTasks.first?.id.uuidString
                let nodeId = (op.extraData?["node_id"] as? String)
                    ?? inst.linkedNodes.first?.id.uuidString
                let body = EGFormInstanceCreateBody(
                    id: inst.id.uuidString,
                    eg_form_id: inst.eg_form_id.uuidString,
                    form_submission: submissionAny,
                    submitted: inst.submitted,
                    task_id: taskId,
                    node_id: nodeId
                )
                _ = try await api.createEGFormInstance(body)
            case .update:
                let body = EGFormInstanceUpdateBody(
                    form_submission: submissionAny,
                    submitted: inst.submitted,
                    is_deleted: inst.is_deleted,
                    // ZP-2363: send the current set verbatim, including `[]`.
                    // `inst.linkedNodes` is the authoritative full set (create
                    // never sets a node_id that diverges from it), so this is a
                    // full-replace matching the online path and web. Collapsing
                    // empty to nil would mean "no change", silently dropping an
                    // offline "remove all linked assets" once back online.
                    node_ids: nodeIds
                )
                _ = try await api.updateEGFormInstance(id: inst.id, body: body)
                if inst.is_deleted { await deleteFromContext(inst) }
            default:
                break
            }

        case .building:
            guard let building = entity as? Building else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createBuilding(building: building)
            case .update:
                _ = try await api.updateBuilding(building)
            default:
                break
            }

        case .floor:
            guard let floor = entity as? Floor else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createFloor(floor: floor)
            case .update:
                _ = try await api.updateFloor(floor)
            default:
                break
            }

        case .room:
            guard let room = entity as? Room else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createRoom(room: room)
            case .update:
                _ = try await api.updateRoom(room)
            default:
                break
            }

        case .mappingIssueTask:
            guard let mappingData = entity as? MappingData,
                  let issueId = mappingData.issueId,
                  let taskId = mappingData.taskId else { throw SyncError.missingEntity }
            
            if mappingData.isDeleted {
                _ = try await api.updateIssueTaskMapping(issueId: issueId, taskId: taskId, isDeleted: true)
            } else {
                _ = try await api.createIssueTaskMapping(issueId: issueId, taskId: taskId)
            }
            
        case .mappingTaskSession:
            guard let mappingData = entity as? MappingData,
                  let taskId = mappingData.taskId,
                  let sessionId = mappingData.sessionId else { throw SyncError.missingEntity }
            
            if mappingData.isDeleted {
                _ = try await api.updateTaskSessionMapping(taskId: taskId, sessionId: sessionId, isDeleted: true)
            } else {
                _ = try await api.createTaskSessionMapping(taskId: taskId, sessionId: sessionId)
            }
            
        case .mappingQuoteTask:
            guard let mappingData = entity as? MappingData,
                  let quoteId = mappingData.quoteId,
                  let taskId = mappingData.taskId else { throw SyncError.missingEntity }
            
            if mappingData.isDeleted {
                _ = try await api.updateQuoteTaskMapping(quoteId: quoteId, taskId: taskId, isDeleted: true)
            } else {
                _ = try await api.createQuoteTaskMapping(quoteId: quoteId, taskId: taskId)
            }
            
        case .mappingUserTask:
            guard let mappingData = entity as? MappingData,
                  let userId = mappingData.userId,
                  let taskId = mappingData.taskId else { throw SyncError.missingEntity }
            let mappingType = mappingData.mappingType ?? "owner"

            if mappingData.isDeleted {
                _ = try await api.updateUserTaskMapping(userId: userId, taskId: taskId, isDeleted: true)
            } else {
                _ = try await api.createUserTaskMapping(userId: userId, taskId: taskId, mappingType: mappingType)
            }

        case .mappingTaskNode:
            guard let mappingData = entity as? MappingData,
                  let taskId = mappingData.taskId,
                  let nodeId = mappingData.nodeId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                _ = try await api.updateTaskNodeMapping(taskId: taskId, nodeId: nodeId, isDeleted: true, isCompleted: mappingData.isCompleted)
            } else {
                _ = try await api.createTaskNodeMapping(taskId: taskId, nodeId: nodeId, isCompleted: mappingData.isCompleted)
            }

        case .mappingTaskNodeBulkCompletion:
            guard let mappingData = entity as? MappingData,
                  let taskId = mappingData.taskId,
                  let bulkCompletionsJson = mappingData.bulkCompletions,
                  let jsonData = bulkCompletionsJson.data(using: .utf8),
                  let completionsArray = try? JSONSerialization.jsonObject(with: jsonData) as? [[String: Any]] else {
                throw SyncError.missingEntity
            }

            // Parse completions from JSON
            let completions: [(nodeId: UUID, isCompleted: Bool)] = completionsArray.compactMap { dict in
                guard let nodeIdString = dict["node_id"] as? String,
                      let nodeId = UUID(uuidString: nodeIdString),
                      let isCompleted = dict["is_completed"] as? Bool else {
                    return nil
                }
                return (nodeId: nodeId, isCompleted: isCompleted)
            }

            _ = try await api.bulkUpdateTaskNodeCompletions(taskId: taskId, completions: completions)

        case .mappingTaskForm:
            guard let mappingData = entity as? MappingData,
                  let taskId = mappingData.taskId,
                  let formId = mappingData.formId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                _ = try await api.updateTaskFormMapping(taskId: taskId, formId: formId, isDeleted: true)
            } else {
                _ = try await api.createTaskFormMapping(taskId: taskId, formId: formId)
            }

        case .mappingTaskFormInstance:
            guard let mappingData = entity as? MappingData,
                  let taskId = mappingData.taskId,
                  let formInstanceId = mappingData.formInstanceId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                _ = try await api.updateTaskFormInstanceMapping(taskId: taskId, formInstanceId: formInstanceId, isDeleted: true)
            } else {
                _ = try await api.createTaskFormInstanceMapping(taskId: taskId, formInstanceId: formInstanceId)
            }

        case .mappingFormInstanceNode:
            guard let mappingData = entity as? MappingData,
                  let formInstanceId = mappingData.formInstanceId,
                  let nodeId = mappingData.nodeId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                // TODO: Add update/delete endpoint when needed
                slog("FormInstance-Node mapping deletion not implemented yet", category: .sync, level: .warning)
            } else {
                _ = try await api.createFormInstanceNodeMapping(formInstanceId: formInstanceId, nodeId: nodeId)
            }

        case .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            // ZP-1723: EG form linking happens server-side inside the
            // POST /eg-form-instance/ create body (task_id + node_id) and
            // the PUT body's node_ids array. Standalone mapping queue
            // items aren't currently enqueued by any client flow — log
            // and skip if one shows up.
            slog("EG form mapping queue item encountered but no standalone endpoint is used; skipping", category: .sync, level: .warning, data: ["target": "\(op.target)"])

        case .mappingNodeSession:
            guard let mappingData = entity as? MappingData,
                  let nodeId = mappingData.nodeId,
                  let sessionId = mappingData.sessionId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                _ = try await api.updateNodeSessionMapping(nodeId: nodeId, sessionId: sessionId, isDeleted: true)
            } else {
                _ = try await api.createNodeSessionMapping(nodeId: nodeId, sessionId: sessionId)
            }

        case .mappingUserSession:
            guard let mappingData = entity as? MappingData,
                  let userId = mappingData.userId,
                  let sessionId = mappingData.sessionId,
                  let mappingType = mappingData.mappingType else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                if let mappingId = mappingData.id {
                    _ = try await api.updateUserSessionMapping(mappingId: mappingId, mappingType: nil, isDeleted: true)
                }
            } else {
                let id = mappingData.id ?? UUID()
                _ = try await api.createUserSessionMapping(id: id, userId: userId, sessionId: sessionId, mappingType: mappingType)
            }

        case .attachment:
            guard let attachment = entity as? Attachment else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                // Skip upload if attachment is already synced
                if !attachment.uploadNeeded {
                    slog("Attachment already synced (uploadNeeded=false), skipping upload", category: .sync, data: [
                        "attachment_id": attachment.id.uuidString,
                        "filename": attachment.filename
                    ])
                    return
                }

                // Upload attachment using AttachmentUploadService
                let attachmentUploadService = AttachmentUploadService(modelContext: modelContext)
                try await attachmentUploadService.uploadAttachment(attachment)
            case .update:
                _ = try await api.updateAttachmentVisibility(
                    attachmentId: attachment.id,
                    visibility: attachment.visibility
                )
            case .delete:
                _ = try await api.deleteAttachment(attachmentId: attachment.id)
                if let localPath = attachment.localFilePath {
                    try? FileManager.default.removeItem(atPath: localPath)
                }
                await deleteFromContext(attachment)
            default:
                break
            }

        case .sldView:
            guard let sldView = entity as? SLDViewV2 else { throw SyncError.missingEntity }
            switch op.operation {
            case .create:
                _ = try await api.createSLDView(sldView: sldView)
            case .update:
                _ = try await api.updateSLDView(sldView)
            default:
                break
            }

        case .mappingAttachmentNode:
            guard let mappingData = entity as? MappingData,
                  let attachmentId = mappingData.attachmentId,
                  let nodeId = mappingData.nodeId else { throw SyncError.missingEntity }

            if mappingData.isDeleted {
                _ = try await api.updateAttachmentNodeMapping(attachmentId: attachmentId, nodeId: nodeId, isDeleted: true)
            } else {
                _ = try await api.createAttachmentNodeMapping(attachmentId: attachmentId, nodeId: nodeId)
            }

        case .mappingNodeSLDView:
            guard let mappingData = entity as? MappingData,
                  let nodeId = mappingData.nodeId,
                  let viewId = mappingData.sldViewId else {
                throw SyncError.missingEntityWithMessage("MappingNodeSLDView missing required data")
            }

            switch op.operation {
            case .create:
                // Add node to view with position
                _ = try await api.addNodeToView(
                    viewId: viewId,
                    nodeId: nodeId,
                    x: mappingData.x ?? 0,
                    y: mappingData.y ?? 0,
                    width: mappingData.width,
                    height: mappingData.height
                )
            case .update:
                // Update node position in view
                _ = try await api.updateNodePositionInView(
                    viewId: viewId,
                    nodeId: nodeId,
                    x: mappingData.x ?? 0,
                    y: mappingData.y ?? 0
                )
                // Also update collapse state if provided
                if let isCollapsed = mappingData.isCollapsed {
                    _ = try await api.updateNodeCollapseStateInView(
                        viewId: viewId,
                        nodeId: nodeId,
                        isCollapsed: isCollapsed
                    )
                }
            case .delete:
                // Remove node from view
                _ = try await api.removeNodeFromView(viewId: viewId, nodeId: nodeId)
            default:
                break
            }

        case .mappingEdgeSLDView:
            guard let mappingData = entity as? MappingData,
                  let edgeId = mappingData.edgeId,
                  let viewId = mappingData.sldViewId else {
                throw SyncError.missingEntityWithMessage("MappingEdgeSLDView missing required data")
            }

            switch op.operation {
            case .create, .update:
                // Create or update edge points in view
                _ = try await api.updateEdgePointsInView(
                    viewId: viewId,
                    edgeId: edgeId,
                    points: mappingData.points,
                    algorithm: mappingData.algorithm
                )
            case .delete:
                // Clear edge points (soft delete)
                _ = try await api.updateEdgePointsInView(
                    viewId: viewId,
                    edgeId: edgeId,
                    points: nil,
                    algorithm: nil
                )
            default:
                break
            }
        }
    }
    
    /// Helper to delete from context after successful sync
    private func deleteFromContext<T: PersistentModel>(_ model: T) async {
        if let context = modelContext {
            context.delete(model)
            try? context.save()
            slog("Deleted entity from local storage after successful sync", category: .sync, data: ["type": "\(type(of: model))"])
        }
    }

    /// Check if error indicates photo already exists on server (duplicate)
    /// This handles cases where upload succeeded but response was lost, causing retry
    private func isPhotoAlreadyExistsError(_ error: Error) -> Bool {
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

    /// Check if error indicates entity already exists on server (duplicate key violation)
    /// This handles cases where create/update succeeded but response was lost, causing retry
    private func isDuplicateEntityError(_ error: Error) -> Bool {
        let errorString = String(describing: error).lowercased()

        // Check for PostgreSQL unique violation / duplicate key errors
        // Using specific patterns to avoid false positives
        if errorString.contains("uniqueviolation") ||
           errorString.contains("duplicate key value violates") ||
           errorString.contains("unique constraint") ||
           errorString.contains("violates unique constraint") {
            return true
        }

        // Check for HTTP 409 Conflict (explicit duplicate)
        if let nsError = error as NSError? {
            if nsError.code == 409 {
                return true
            }
        }

        return false
    }

    // MARK: - Sync Orchestration

    /// Callback for updating sync queue count
    var onQueueCountUpdate: ((Int) -> Void)?
    
    /// Callback for logging sync operations (async to ensure deterministic context.save() ordering)
    var onSyncLog: ((SyncLog) async -> Void)?
    
    /// Main orchestration method for flushing the sync queue
    func orchestrateSyncFlush() async -> (successful: Int, failed: Int) {
        slog("SYNC ORCHESTRATION STARTED", category: .sync, data: ["model_context_available": modelContext != nil])

        guard let context = modelContext,
              let syncQueueService = syncQueueService else {
            slog("Missing dependencies for sync orchestration", category: .sync, level: .error)
            return (0, 0)
        }

        // Reserve the sync slot before any await — two MainActor Tasks
        // (e.g. back-to-back flushQueue calls) could otherwise both pass
        // the guard during an in-flight suspension and double-process the
        // same queue items, racing over the protected file copy.
        guard !isSyncing else {
            slog("Sync already in progress, skipping", category: .sync, level: .warning)
            return (0, 0)
        }
        isSyncing = true
        syncProgress = 0
        syncTotal = 0
        defer {
            isSyncing = false
            syncProgress = 0
            syncTotal = 0
        }

        // Step 0: Check network connectivity
        slog("Step 0: Checking network connectivity...", category: .sync)
        let reachability = NetworkReachability.shared

        if !reachability.isConnected {
            slog("No network connection detected", category: .sync, level: .error)
            
            // Try to wait for connectivity briefly
            let connected = await reachability.waitForConnectivity(timeout: 3)
            if !connected {
                slog("Network still unavailable after waiting - sync aborted", category: .sync, level: .error)
                
                // Notify NetworkState to show alert
                await MainActor.run {
                    NetworkState.shared.networkAlertMessage = "Unable to sync: No network connection available. Your changes are saved locally and will sync when connection is restored."
                    NetworkState.shared.showNetworkAlert = true
                }
                
                await onSyncLog?(SyncLog(
                    target: .node,  // Using a default target
                    operation: .create,  // Using a default operation
                    entityId: nil,
                    entityLabel: "SYNC_FLUSH",
                    success: false,
                    errorMessage: "No network connection available"
                ))
                return (0, 0)
            }
        }

        // Double-check with actual connectivity test
        slog("Testing actual connectivity...", category: .sync)
        let canReachServer = await reachability.testConnectivity()
        if !canReachServer {
            slog("Cannot reach server - sync aborted", category: .sync, level: .error)

            // Notify NetworkState to show alert
            await MainActor.run {
                NetworkState.shared.networkAlertMessage = "Unable to reach server. Please check your internet connection or try again later."
                NetworkState.shared.showNetworkAlert = true
            }

            await onSyncLog?(SyncLog(
                target: .node,  // Using a default target
                operation: .create,  // Using a default operation
                entityId: nil,
                entityLabel: "SYNC_FLUSH",
                success: false,
                errorMessage: "Cannot reach server"
            ))
            return (0, 0)
        }

        slog("Network connectivity confirmed", category: .sync, data: ["connection_type": reachability.connectionTypeString])
        if reachability.isExpensive {
            slog("Using expensive connection (cellular/hotspot)", category: .sync, level: .warning)
        }

        // Step 1: Snapshot pre-dedup queue, deduplicate, snapshot post-dedup, store sync job
        slog("Step 1: Capturing sync queue snapshot and deduplicating...", category: .sync)

        // Snapshot pre-dedup
        let preDedupPayload = serializeSyncQueue(context: context)
        let preDedupCount = syncQueueService.getQueueCount()

        // Deduplicate
        if let deduplicationService = deduplicationService {
            await deduplicationService.deduplicateSyncQueue(context: context)
            let queueCount = syncQueueService.getQueueCount()
            onQueueCountUpdate?(queueCount)
        }

        // Snapshot post-dedup
        let postDedupPayload = serializeSyncQueue(context: context)
        let postDedupCount = syncQueueService.getQueueCount()

        // Store sync job to backend — only when there are items to sync
        if postDedupCount > 0 {
            let userId = AppStateManager.shared.userId.uuidString
            let sldId = AppStateManager.shared.activeSLDId.uuidString
            let syncJobPayload: [String: Any] = [
                "user_id": userId,
                "sld_id": sldId,
                "payload": [
                    "pre_dedup_payload": preDedupPayload,
                    "post_dedup_payload": postDedupPayload,
                    "queue_count_pre": preDedupCount,
                    "queue_count_post": postDedupCount
                ] as [String: Any]
            ]

            do {
                let _ = try await api.createSyncJob(payload: syncJobPayload)
                slog("Sync job stored successfully", category: .sync, data: ["pre_count": preDedupCount, "post_count": postDedupCount])
            } catch {
                slog("Failed to store sync job — aborting sync", category: .sync, level: .error, data: ["error": error.localizedDescription])

                // Don't show alert for auth errors — the re-auth flow handles these
                let isAuthError = error is AuthError
                    || (error as? URLError)?.code == .userAuthenticationRequired
                if !isAuthError {
                    await MainActor.run {
                        NetworkState.shared.networkAlertTitle = "Sync Failed"
                        NetworkState.shared.networkAlertMessage = error.localizedDescription
                        NetworkState.shared.showNetworkAlert = true
                    }
                }
                return (0, 0)
            }
        } else {
            slog("Sync queue empty — skipping sync job audit", category: .sync)
        }

        slog("Step 2: Starting sync process...", category: .sync)
        
        // Process in batches
        var hasMoreItems = true
        var totalSuccessful = 0
        var totalFailed = 0
        var batchNumber = 0
        
        while hasMoreItems {
            // Stop sync loop if session has expired - items stay in queue for retry after re-auth
            if await AuthService.shared.isSessionExpired {
                slog("Session expired - stopping sync loop, items remain in queue for retry after re-auth", category: .sync, level: .warning)
                break
            }

            batchNumber += 1
            slog("Processing batch", category: .sync, data: ["batch_number": batchNumber])

            // Fetch a batch of items
            let toSync = await syncQueueService.getPendingSyncOps(limit: SyncConfiguration.batchFetchLimit)

            if toSync.isEmpty {
                slog("No more items to sync", category: .sync)
                hasMoreItems = false
                break
            }

            slog("Found operations to sync", category: .sync, data: ["count": toSync.count])

            // Update UI state
            self.syncProgress = 0
            self.syncTotal = toSync.count

            // Process this batch in chunks
            let chunks = toSync.chunked(into: SyncConfiguration.chunkSize)
            slog("Batch divided into chunks", category: .sync, data: ["chunk_count": chunks.count, "chunk_size": SyncConfiguration.chunkSize])

            for (chunkIndex, chunk) in chunks.enumerated() {
                // Stop processing chunks if session has expired
                if await AuthService.shared.isSessionExpired {
                    slog("Session expired during chunk processing - stopping", category: .sync, level: .warning)
                    break
                }

                slog("Processing chunk", category: .sync, data: ["chunk": chunkIndex + 1, "total_chunks": chunks.count, "operations": chunk.count])

                // Process chunk
                let results = await processChunkWithConcurrencyLimit(chunk)

                let successful = results.filter { $0.success }.count
                let failed = results.count - successful
                totalSuccessful += successful
                totalFailed += failed

                slog("Chunk results", category: .sync, data: ["chunk": chunkIndex + 1, "successful": successful, "failed": failed])

                // ZP-1265 (aa123c2) — Per-item failures no longer halt the
                // loop. Each item gets up to N retries and then either
                // succeeds, gets recorded as failed (and the loop moves on),
                // or — if reachability dropped mid-flush — is left untouched
                // and the chunk processor sets `networkLostDuringSync`. Only
                // network loss aborts the outer batch loop.
                if NetworkState.shared.networkLostDuringSync {
                    slog("Network lost during chunk - aborting remaining batches", category: .sync, level: .warning)
                    hasMoreItems = false
                    break
                }

                // Brief pause between chunks
                if chunkIndex < chunks.count - 1 {
                    try? await Task.sleep(nanoseconds: 500_000_000)
                }
            }

            // Stop if session expired during processing
            if await AuthService.shared.isSessionExpired {
                slog("Session expired - exiting sync loop", category: .sync, level: .warning)
                break
            }

            // ZP-1265 (aa123c2) — if a chunk reported network loss, stop here.
            if !hasMoreItems { break }
            let remainingCount = await syncQueueService.getRemainingQueueCount()
            hasMoreItems = remainingCount > 0

            if hasMoreItems {
                slog("Items remaining in queue, processing next batch", category: .sync, data: ["remaining": remainingCount])
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            } else {
                slog("All items processed in this sync session", category: .sync)
            }
        }

        slog("SYNC COMPLETE", category: .sync, data: ["successful": totalSuccessful, "failed": totalFailed])
        
        return (totalSuccessful, totalFailed)
    }

    // MARK: - Sync Job Snapshot

    /// Serialize all current SyncQueueItems into an array of dictionaries for sync job storage
    private func serializeSyncQueue(context: ModelContext) -> [[String: Any]] {
        let descriptor = FetchDescriptor<SyncQueueItem>(
            sortBy: [SortDescriptor(\.createdAt)]
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601

        do {
            let items = try context.fetch(descriptor)
            return items.map { item in
                var dict: [String: Any] = [
                    "id": item.id.uuidString,
                    "target": syncTargetName(item.target),
                    "operation": syncOperationName(item.operation),
                    "created_at": ISO8601DateFormatter().string(from: item.createdAt),
                    "retry_count": item.retryCount
                ]

                if let mappingData = item.mappingData,
                   let encoded = try? encoder.encode(mappingData),
                   let jsonObj = try? JSONSerialization.jsonObject(with: encoded) {
                    dict["mapping_data"] = jsonObj
                }
                if let extraData = item.extraData {
                    dict["extra_data"] = extraData
                }

                dict["payload"] = buildEntityPayload(for: item, context: context, encoder: encoder)

                return dict
            }
        } catch {
            slog("Failed to serialize sync queue", category: .sync, level: .error, data: ["error": error.localizedDescription])
            return []
        }
    }

    private func syncTargetName(_ target: SyncTarget) -> String {
        switch target {
        case .node: return "node"
        case .edge: return "edge"
        case .photo: return "photo"
        case .userTask: return "userTask"
        case .irPhoto: return "irPhoto"
        case .irSession: return "irSession"
        case .issue: return "issue"
        case .quote: return "quote"
        case .formInstance: return "formInstance"
        case .building: return "building"
        case .floor: return "floor"
        case .room: return "room"
        case .attachment: return "attachment"
        case .sldView: return "sldView"
        case .mappingIssueTask: return "mappingIssueTask"
        case .mappingTaskSession: return "mappingTaskSession"
        case .mappingQuoteTask: return "mappingQuoteTask"
        case .mappingUserTask: return "mappingUserTask"
        case .mappingTaskNode: return "mappingTaskNode"
        case .mappingTaskForm: return "mappingTaskForm"
        case .mappingTaskFormInstance: return "mappingTaskFormInstance"
        case .mappingFormInstanceNode: return "mappingFormInstanceNode"
        case .mappingNodeSession: return "mappingNodeSession"
        case .mappingAttachmentNode: return "mappingAttachmentNode"
        case .mappingNodeSLDView: return "mappingNodeSLDView"
        case .mappingEdgeSLDView: return "mappingEdgeSLDView"
        case .mappingUserSession: return "mappingUserSession"
        case .mappingTaskNodeBulkCompletion: return "mappingTaskNodeBulkCompletion"
        case .egFormInstance: return "egFormInstance"
        case .mappingTaskEGFormInstance: return "mappingTaskEGFormInstance"
        case .mappingEGFormInstanceNode: return "mappingEGFormInstanceNode"
        }
    }

    private func syncOperationName(_ operation: SyncOperation) -> String {
        switch operation {
        case .create: return "create"
        case .read: return "read"
        case .update: return "update"
        case .delete: return "delete"
        }
    }

    /// Build the DTO payload for a sync queue item by fetching the entity from SwiftData
    private func buildEntityPayload(for item: SyncQueueItem, context: ModelContext, encoder: JSONEncoder) -> Any {
        do {
            switch item.target {
            case .node:
                guard let nodeId = item.nodeId else { return ["error": "no node_id on queue item"] }
                let desc = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { n in n.id == nodeId })
                guard let node = try context.fetch(desc).first else { return ["error": "node not found", "node_id": nodeId.uuidString] }
                return safeNodeToDict(node, encoder: encoder)

            case .edge:
                guard let edgeId = item.edgeId else { return ["error": "no edge_id on queue item"] }
                let desc = FetchDescriptor<EdgeV2>(predicate: #Predicate<EdgeV2> { e in e.id == edgeId })
                guard let edge = try context.fetch(desc).first else { return ["error": "edge not found", "edge_id": edgeId.uuidString] }
                return safeEdgeToDict(edge, encoder: encoder)

            case .photo:
                guard let photoId = item.photoId else { return ["error": "no photo_id on queue item"] }
                let desc = FetchDescriptor<Photo>(predicate: #Predicate<Photo> { p in p.id == photoId })
                guard let photo = try context.fetch(desc).first else { return ["error": "photo not found", "photo_id": photoId.uuidString] }
                let entityId: UUID? = {
                    if photo.type.hasPrefix("task_") { return photo.userTask?.id }
                    if photo.type.hasPrefix("issue") { return photo.issue?.id }
                    if photo.type == "building" { return photo.building?.id }
                    if photo.type == "floor" { return photo.floor?.id }
                    if photo.type == "room" { return photo.room?.id }
                    return photo.node?.id
                }()
                let payload = SLDDTOPhoto(
                    id: photo.id, entity_id: entityId, url: photo.url, type: photo.type,
                    sld_id: photo.sld?.id, filename: photo.filename,
                    local_filepath: photo.local_filepath,
                    upload_needed: false, is_deleted: photo.is_deleted, caption: photo.caption
                )
                return encodableToDict(payload, encoder: encoder)

            case .userTask:
                guard let taskId = item.userTaskId else { return ["error": "no user_task_id on queue item"] }
                let desc = FetchDescriptor<UserTask>(predicate: #Predicate<UserTask> { t in t.id == taskId })
                guard let task = try context.fetch(desc).first else { return ["error": "task not found", "user_task_id": taskId.uuidString] }
                return safeUserTaskToDict(task, encoder: encoder)

            case .irSession:
                guard let sessionId = item.irSessionId else { return ["error": "no ir_session_id on queue item"] }
                let desc = FetchDescriptor<IRSession>(predicate: #Predicate<IRSession> { s in s.id == sessionId })
                guard let irSession = try context.fetch(desc).first else { return ["error": "ir_session not found", "ir_session_id": sessionId.uuidString] }
                let payload = IRSessionDTO(
                    id: irSession.id, name: irSession.name,
                    photo_type: irSession.photo_type,
                    active_visual_prefix: irSession.active_visual_prefix,
                    active_ir_prefix: irSession.active_ir_prefix,
                    date_created: irSession.date_created, date_closed: irSession.date_closed,
                    active: irSession.active, sld_id: irSession.sld.id,
                    is_deleted: irSession.is_deleted
                )
                return encodableToDict(payload, encoder: encoder)

            case .irPhoto:
                guard let irPhotoId = item.irPhotoId else { return ["error": "no ir_photo_id on queue item"] }
                let desc = FetchDescriptor<IRPhoto>(predicate: #Predicate<IRPhoto> { ip in ip.id == irPhotoId })
                guard let irPhoto = try context.fetch(desc).first else { return ["error": "ir_photo not found", "ir_photo_id": irPhotoId.uuidString] }
                let payload = IRPhotoDTO(
                    id: irPhoto.id, ir_session_id: irPhoto.ir_session?.id,
                    node_id: irPhoto.node.id, visual_photo_key: irPhoto.visual_photo_key,
                    ir_photo_key: irPhoto.ir_photo_key, date_created: irPhoto.date_created,
                    sld_id: irPhoto.sld.id, issue_id: irPhoto.issue?.id,
                    is_deleted: irPhoto.is_deleted
                )
                return encodableToDict(payload, encoder: encoder)

            case .issue:
                guard let issueId = item.issueId else { return ["error": "no issue_id on queue item"] }
                let desc = FetchDescriptor<Issue>(predicate: #Predicate<Issue> { i in i.id == issueId })
                guard let issue = try context.fetch(desc).first else { return ["error": "issue not found", "issue_id": issueId.uuidString] }
                let payload = IssueDTO(
                    id: issue.id, title: issue.title, description: issue.issueDescription,
                    created_date: issue.created_date, node_id: issue.node?.id,
                    issue_class: issue.issue_class?.id, issue_type: issue.issue_type,
                    issue_subtype: issue.issue_subtype, is_deleted: issue.is_deleted,
                    session_id: issue.session?.id, sld_id: issue.sld?.id,
                    details: issue.details.map { attr in
                        IssuePropertyDTO(id: attr.id, issue_class_property: attr.issue_class_property?.id.uuidString ?? "", name: attr.name, value: attr.value, unit: attr.unit, description: attr.attributeNotes)
                    },
                    status: issue.status, proposed_resolution: issue.proposed_resolution,
                    modified_date: issue.modified_date, priority: issue.priority,
                    immediate_hazard: issue.immediateHazard, customer_notified: issue.customerNotified
                )
                return encodableToDict(payload, encoder: encoder)

            case .quote:
                guard let quoteId = item.quoteId else { return ["error": "no quote_id on queue item"] }
                let desc = FetchDescriptor<Quote>(predicate: #Predicate<Quote> { q in q.id == quoteId })
                guard let quote = try context.fetch(desc).first else { return ["error": "quote not found", "quote_id": quoteId.uuidString] }
                let payload = QuoteDTO(
                    id: quote.id, title: quote.title, sow: quote.sow, tnm: quote.tnm,
                    sld_id: quote.sld?.id, description: quote.quoteDescription,
                    status: quote.status, is_deleted: quote.is_deleted
                )
                return encodableToDict(payload, encoder: encoder)

            case .building:
                guard let buildingId = item.buildingId else { return ["error": "no building_id on queue item"] }
                let desc = FetchDescriptor<Building>(predicate: #Predicate<Building> { b in b.id == buildingId })
                guard let building = try context.fetch(desc).first else { return ["error": "building not found", "building_id": buildingId.uuidString] }
                var payload: [String: Any] = [
                    "id": building.id.uuidString, "name": building.name,
                    "sld_id": building.sld?.id.uuidString ?? "", "is_deleted": building.is_deleted
                ]
                if let accessNotes = building.access_notes { payload["access_notes"] = accessNotes }
                return payload

            case .floor:
                guard let floorId = item.floorId else { return ["error": "no floor_id on queue item"] }
                let desc = FetchDescriptor<Floor>(predicate: #Predicate<Floor> { f in f.id == floorId })
                guard let floor = try context.fetch(desc).first else { return ["error": "floor not found", "floor_id": floorId.uuidString] }
                var payload: [String: Any] = [
                    "id": floor.id.uuidString, "name": floor.name,
                    "building_id": floor.building?.id.uuidString ?? "", "is_deleted": floor.is_deleted
                ]
                if let accessNotes = floor.access_notes { payload["access_notes"] = accessNotes }
                return payload

            case .room:
                guard let roomId = item.roomId else { return ["error": "no room_id on queue item"] }
                let desc = FetchDescriptor<Room>(predicate: #Predicate<Room> { r in r.id == roomId })
                guard let room = try context.fetch(desc).first else { return ["error": "room not found", "room_id": roomId.uuidString] }
                var payload: [String: Any] = [
                    "id": room.id.uuidString, "name": room.name,
                    "floor_id": room.floor?.id.uuidString ?? "", "is_deleted": room.is_deleted
                ]
                if let accessNotes = room.access_notes { payload["access_notes"] = accessNotes }
                return payload

            case .sldView:
                guard let viewId = item.sldViewId else { return ["error": "no sld_view_id on queue item"] }
                let desc = FetchDescriptor<SLDViewV2>(predicate: #Predicate<SLDViewV2> { v in v.id == viewId })
                guard let sldView = try context.fetch(desc).first else { return ["error": "sld_view not found", "sld_view_id": viewId.uuidString] }
                return [
                    "id": sldView.id.uuidString, "sld_id": sldView.sld_id.uuidString,
                    "name": sldView.name, "description": sldView.viewDescription ?? "",
                    "view_type": sldView.view_type
                ] as [String: Any]

            case .attachment:
                guard let attachmentId = item.attachmentId else { return ["error": "no attachment_id on queue item"] }
                let desc = FetchDescriptor<Attachment>(predicate: #Predicate<Attachment> { a in a.id == attachmentId })
                guard let attachment = try context.fetch(desc).first else { return ["error": "attachment not found", "attachment_id": attachmentId.uuidString] }
                return [
                    "id": attachment.id.uuidString, "company_id": attachment.companyId.uuidString,
                    "sld_id": attachment.sldId.uuidString, "type": attachment.type,
                    "filename": attachment.filename, "file_size": attachment.fileSize,
                    "key": attachment.key, "is_deleted": attachment.isDeleted,
                    "visibility": attachment.visibility
                ] as [String: Any]

            case .formInstance:
                return ["message": "payload omitted due to size constraints", "form_instance_id": item.formInstanceId?.uuidString ?? "nil"]

            case .egFormInstance:
                return ["message": "payload omitted due to size constraints", "eg_form_instance_id": item.egFormInstanceId?.uuidString ?? "nil"]

            case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask,
                 .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode,
                 .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView,
                 .mappingUserSession, .mappingTaskNodeBulkCompletion,
                 .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
                return ["message": "see mapping_data field"]
            }
        } catch {
            return ["error": "failed to fetch entity: \(error.localizedDescription)"]
        }
    }

    // MARK: Safe DTO Fallbacks

    /// Safely convert a Node to a dictionary, falling back to a manual map if DTO encoding fails
    private func safeNodeToDict(_ node: NodeV2, encoder: JSONEncoder) -> Any {
        do {
            let payload = SLDDTONode(
                id: node.id, type: node.type, label: node.label,
                sld_id: node.sld?.id ?? UUID(), parent_id: node.parent_id,
                x: node.x, y: node.y, width: node.width, height: node.height,
                is_deleted: node.is_deleted, location: node.location,
                node_class: node.node_class?.id, node_subtype: node.node_subtype?.id,
                core_attributes: node.core_attributes.map { attr in
                    NodePropertyDTO(id: attr.id, node_class_property: attr.node_class_property?.id.uuidString ?? "", name: attr.name, value: attr.value)
                },
                node_terminals: node.node_terminals.map { $0.toDTO() },
                com: node.com,
                com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
                qr_code: node.qr_code, serviceability: node.serviceability,
                room_id: node.room?.id, default_photo_id: node.default_photo_id,
                suggested_shortcut: node.suggested_shortcut_id
            )
            return encodableToDict(payload, encoder: encoder)
        } catch {
            // Fallback: manually build a map with essential fields
            var dict: [String: Any] = [
                "id": node.id.uuidString, "type": node.type, "label": node.label,
                "sld_id": node.sld?.id.uuidString ?? "", "x": node.x, "y": node.y,
                "width": node.width, "height": node.height, "is_deleted": node.is_deleted
            ]
            if let parentId = node.parent_id { dict["parent_id"] = parentId.uuidString }
            if let location = node.location { dict["location"] = location }
            if let nodeClass = node.node_class?.id { dict["node_class"] = nodeClass.uuidString }
            if let nodeSubtype = node.node_subtype?.id { dict["node_subtype"] = nodeSubtype.uuidString }
            if let com = node.com { dict["com"] = com }
            if let qrCode = node.qr_code { dict["qr_code"] = qrCode }
            if let roomId = node.room?.id { dict["room_id"] = roomId.uuidString }
            return dict
        }
    }

    /// Safely convert an Edge to a dictionary, falling back to a manual map if DTO encoding fails
    private func safeEdgeToDict(_ edge: EdgeV2, encoder: JSONEncoder) -> Any {
        do {
            let payload = SLDDTOEdge(
                id: edge.id, source: edge.source, target: edge.target,
                sld_id: edge.sld?.id ?? UUID(),
                source_handle: edge.sourceHandle, target_handle: edge.targetHandle,
                source_node_terminal_id: edge.sourceNodeTerminalId,
                target_node_terminal_id: edge.targetNodeTerminalId,
                is_deleted: edge.is_deleted, edge_class: edge.edge_class?.id,
                core_attributes: edge.core_attributes.map { attr in
                    EdgePropertyDTO(id: attr.id, edge_class_property: attr.edge_class_property?.id.uuidString ?? "", name: attr.name, value: attr.value)
                },
                points: edge.points, algorithm: edge.algorithm
            )
            return encodableToDict(payload, encoder: encoder)
        } catch {
            var dict: [String: Any] = [
                "id": edge.id.uuidString, "source": edge.source?.uuidString ?? NSNull(),
                "target": edge.target?.uuidString ?? NSNull(),
                "sld_id": edge.sld?.id.uuidString ?? "", "is_deleted": edge.is_deleted
            ]
            if let edgeClass = edge.edge_class?.id { dict["edge_class"] = edgeClass.uuidString }
            if let algorithm = edge.algorithm { dict["algorithm"] = algorithm }
            return dict
        }
    }

    /// Safely convert a UserTask to a dictionary, falling back to a manual map if DTO encoding fails
    private func safeUserTaskToDict(_ task: UserTask, encoder: JSONEncoder) -> Any {
        do {
            let payload = UserTaskDTO(
                id: task.id, title: task.title, task_description: task.task_description,
                completed: task.completed, form_id: task.form?.id, node_id: task.node?.id,
                sld_id: task.sld?.id ?? UUID(), is_deleted: task.is_deleted,
                submission: task.submission, submitted_at: task.submitted_at,
                due_date: task.due_date, created_at: task.created_at,
                task_type: task.task_type, interval: task.interval,
                recurring: task.recurring, procedure_id: task.procedure_id,
                shortcut_id: task.shortcut_id
            )
            return encodableToDict(payload, encoder: encoder)
        } catch {
            var dict: [String: Any] = [
                "id": task.id.uuidString, "title": task.title,
                "task_description": task.task_description,
                "sld_id": task.sld?.id.uuidString ?? "",
                "is_deleted": task.is_deleted, "completed": task.completed
            ]
            if let dueDate = task.due_date { dict["due_date"] = ISO8601DateFormatter().string(from: dueDate) }
            return dict
        }
    }

    /// Encode a Codable value to a JSON dictionary for inclusion in sync job payload
    private func encodableToDict<T: Encodable>(_ value: T, encoder: JSONEncoder) -> Any {
        guard let data = try? encoder.encode(value),
              let dict = try? JSONSerialization.jsonObject(with: data) else {
            return ["error": "failed to encode payload"]
        }
        return dict
    }

    /// Process a chunk with limited concurrency
    /// Mark previous failed SyncLog entries for the same entity as requeued
    /// so the retry button is hidden (the entity has already been synced successfully)
    private func markPreviousFailedLogsAsRequeued(entityId: UUID, target: SyncTarget) {
        guard let context = modelContext else { return }

        let targetValue = target.rawValue
        // Use a simple predicate to avoid type-checker complexity, then filter in memory
        let descriptor = FetchDescriptor<SyncLog>(
            predicate: #Predicate<SyncLog> { log in
                log.entityId == entityId &&
                log.targetRawValue == targetValue
            }
        )

        do {
            let matchingLogs = try context.fetch(descriptor)
            let staleFailedLogs = matchingLogs.filter { !$0.success && !$0.isRequeued }
            if !staleFailedLogs.isEmpty {
                for log in staleFailedLogs {
                    log.isRequeued = true
                }
                try context.save()
                slog("Marked previous failed logs as resolved", category: .sync, data: [
                    "entity_id": entityId.uuidString,
                    "target": "\(target)",
                    "count": staleFailedLogs.count
                ])
            }
        } catch {
            slog("Failed to mark previous failed logs as resolved", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }

    /// Mapping targets always log with entityId=nil (there's no local SwiftData entity to
    /// reference), so entityId-based matching can't auto-resolve stale failures. Match on
    /// mappingIds + target instead, populated from MappingData.identityKey(for:).
    private func markPreviousFailedMappingLogsAsRequeued(mappingIds: String, target: SyncTarget) {
        guard let context = modelContext else { return }
        guard target.isMapping else { return }

        let targetValue = target.rawValue
        let descriptor = FetchDescriptor<SyncLog>(
            predicate: #Predicate<SyncLog> { log in
                log.mappingIds == mappingIds &&
                log.targetRawValue == targetValue
            }
        )

        do {
            let matchingLogs = try context.fetch(descriptor)
            let staleFailedLogs = matchingLogs.filter { !$0.success && !$0.isRequeued }
            if !staleFailedLogs.isEmpty {
                for log in staleFailedLogs {
                    log.isRequeued = true
                }
                try context.save()
                slog("Marked previous failed mapping logs as resolved", category: .sync, data: [
                    "mapping_ids": mappingIds,
                    "target": "\(target)",
                    "count": staleFailedLogs.count
                ])
            }
        } catch {
            slog("Failed to mark previous failed mapping logs as resolved", category: .sync, level: .error, data: ["error": "\(error)"])
        }
    }

    /// Check if an error is an authentication error that requires re-authentication
    private func isAuthenticationError(_ error: Error?) -> Bool {
        guard let error = error else { return false }
        if error is AuthError { return true }
        if let urlError = error as? URLError, urlError.code == .userAuthenticationRequired { return true }
        return false
    }

    private func processChunkWithConcurrencyLimit(_ chunk: [SyncOp]) async -> [SyncResult] {
        slog("processChunkWithConcurrencyLimit started", category: .sync, level: .debug)

        // ZP-1265 (aa123c2) — Pre-chunk network gate. If reachability is
        // already false, leave every queued item untouched, surface the
        // network-lost flag, and return empty. Mirrors Android's "in-flight
        // item left untouched on network loss" guarantee — applied here at
        // the chunk boundary too so we don't even start.
        if !NetworkReachability.shared.isConnected {
            slog("Network lost before chunk - leaving all items untouched", category: .sync, level: .warning)
            await MainActor.run {
                NetworkState.shared.networkLostDuringSync = true
            }
            return []
        }
        slog("Processing operations sequentially", category: .sync, level: .debug, data: ["count": chunk.count])

        var results: [SyncResult] = []

        for op in chunk {
            // Stop processing if session has expired and needs re-authentication
            if await AuthService.shared.isSessionExpired {
                slog("Session expired - stopping chunk processing, items remain in queue for retry after re-auth", category: .sync, level: .warning)
                break
            }

            // ZP-1265 (aa123c2) — Pre-iteration network gate. If reachability
            // dropped between items, halt cleanly without touching this row.
            // The orchestrator reads `NetworkState.shared.networkLostDuringSync`
            // (set just below) to abort the outer batch loop.
            if !NetworkReachability.shared.isConnected {
                slog("Network lost between items - halting chunk", category: .sync, level: .warning)
                await MainActor.run {
                    NetworkState.shared.networkLostDuringSync = true
                }
                break
            }

            slog("Processing operation", category: .sync, level: .debug, data: ["target": "\(op.target)", "operation": "\(op.operation)"])
            let operationStartTime = Date()

            // ZP-2097 — publish "this item is in flight" so the analyzer UI
            // can show a rotating sync icon on its row. Cleared after the
            // op completes (either branch).
            await MainActor.run {
                NetworkState.shared.currentSyncItemId = op.queueItemId
            }

            // Process the operation
            let result = await processSyncOperationWithRetry(op)
            results.append(result)

            let duration = Date().timeIntervalSince(operationStartTime)

            await MainActor.run {
                NetworkState.shared.currentSyncItemId = nil
            }

            // Handle post-processing
            if result.success {
                // ZP-1847: release the protected on-disk file before the
                // queue row is dropped. Idempotent — safe even when no file
                // was preserved (e.g. non-photo/attachment ops).
                if let queueItemId = op.queueItemId {
                    SyncFileManager.shared.releaseFile(for: queueItemId)
                }

                // Remove from queue
                if let queueItemId = op.queueItemId {
                    await syncQueueService?.removeSyncQueueItem(withId: queueItemId)

                    // Update queue count via callback
                    if let queueService = syncQueueService {
                        let queueCount = queueService.getQueueCount()
                        onQueueCountUpdate?(queueCount)
                    }
                }

                // Compute a stable mapping identity key (for mapping targets) so success
                // and failure logs share a matchable key — mirrors the entityId-based
                // self-heal used for node/photo/etc.
                let mappingIdentityKey: String? = op.target.isMapping
                    ? op.mappingData?.identityKey(for: op.target)
                    : nil

                // Mark any previous failed logs for this operation as resolved
                // so the retry button is hidden (prevents duplicate sync attempts).
                if let entityId = result.entityId {
                    markPreviousFailedLogsAsRequeued(entityId: entityId, target: op.target)
                } else if let key = mappingIdentityKey {
                    markPreviousFailedMappingLogsAsRequeued(mappingIds: key, target: op.target)
                }

                // Log success
                let log = SyncLog(
                    target: op.target,
                    operation: op.operation,
                    entityId: result.entityId,
                    entityLabel: "\(op.target)",
                    success: true,
                    duration: duration,
                    userId: op.userId,
                    siteId: op.siteId,
                    snapshotJSON: op.snapshotJSON,
                    photoFilePath: op.photoFilePath
                )
                if let key = mappingIdentityKey {
                    log.setMappingMetadata(type: "\(op.target)", ids: key)
                }
                await onSyncLog?(log)

                slog("Operation completed", category: .sync, data: ["target": "\(result.operation.target)", "operation": "\(result.operation.operation)"])
            } else {
                // ZP-1265 (aa123c2) — Network-loss check FIRST, before any
                // logging or DB writes. If reachability dropped during the
                // inner retry, leave this item COMPLETELY untouched: no
                // SyncLog row, no `recordFailure`, no retryCount bump. The
                // user-visible row stays in idle/pending state; next Sync
                // Now picks it up cleanly. Mirrors Android's guarantee.
                if !NetworkReachability.shared.isConnected {
                    slog("Network lost mid-flush - leaving in-flight item untouched and halting", category: .sync, level: .warning, data: [
                        "target": "\(op.target)",
                        "operation": "\(op.operation)",
                        "queueItemId": op.queueItemId?.uuidString ?? "nil"
                    ])
                    await MainActor.run {
                        NetworkState.shared.networkLostDuringSync = true
                    }
                    return results
                }

                // Log failure with detailed error info
                var errorMessage = result.error?.localizedDescription ?? "Unknown error"
                var httpStatusCode: Int? = nil

                // Extract HTTP status code if available
                if let urlError = result.error as? URLError {
                    httpStatusCode = urlError.code.rawValue
                } else if let nsError = result.error as NSError? {
                    httpStatusCode = nsError.code
                }

                // Detect rows that can never succeed: a CREATE whose live entity
                // is missing AND whose queue row carries no captured snapshot to
                // replay. Mostly v1.30 queue items that survived the upgrade —
                // the snapshot fast-path (ZP-1847) wasn't shipped yet so there's
                // nothing to replay, and the entity tables have since been wiped
                // (account / site switch, schema migration, etc.). Mark them with
                // the unrecoverable-legacy sentinel + a clearer message; the
                // queue layer will skip them on subsequent flushes so the chip
                // stops climbing on every Sync tap. The user can still resolve
                // them inline via Retry (clears the sentinel) or Delete.
                let isMissingEntityFailure: Bool = {
                    if case SyncError.missingEntity? = result.error { return true }
                    if case SyncError.missingEntityWithMessage? = result.error { return true }
                    return false
                }()
                let isUnrecoverableLegacy = isMissingEntityFailure
                    && op.operation == .create
                    && op.snapshotJSON == nil
                    && !op.target.isMapping
                if isUnrecoverableLegacy {
                    httpStatusCode = SyncConfiguration.unrecoverableLegacyFailureCode
                    errorMessage = AppStrings.Diagnostics.unrecoverableLegacyItemMessage
                }

                let log = SyncLog(
                    target: op.target,
                    operation: op.operation,
                    entityId: result.entityId,
                    entityLabel: "\(op.target)",
                    success: false,
                    errorMessage: errorMessage,
                    httpStatusCode: httpStatusCode,
                    duration: duration,
                    userId: op.userId,
                    siteId: op.siteId,
                    snapshotJSON: op.snapshotJSON,
                    photoFilePath: op.photoFilePath
                )
                if op.target.isMapping, let key = op.mappingData?.identityKey(for: op.target) {
                    log.setMappingMetadata(type: "\(op.target)", ids: key)
                }
                await onSyncLog?(log)

                slog("Operation failed", category: .sync, level: .error, data: [
                    "target": "\(result.operation.target)",
                    "operation": "\(result.operation.operation)",
                    "error": errorMessage,
                    "http_status": httpStatusCode ?? -1
                ])

                // Authentication errors: keep items in queue WITHOUT incrementing retry count.
                // The items will be retried automatically after the user re-authenticates.
                // Distinct from network-loss; auth errors halt the chunk for the re-auth flow
                // but do not fall under the network-lost UX.
                if isAuthenticationError(result.error) {
                    slog("Authentication error - keeping item in queue without incrementing retry count, stopping remaining operations", category: .sync, level: .warning, data: [
                        "target": "\(op.target)",
                        "operation": "\(op.operation)",
                        "queueItemId": op.queueItemId?.uuidString ?? "nil"
                    ])
                    break
                }

                // Duplicate-key 500s mean the row is already on the server —
                // treat as success: drop the row, advance progress, move on.
                if let error = result.error, isDuplicateEntityError(error) {
                    slog("Duplicate key error - data already exists on server, treating as success", category: .sync, level: .info, data: [
                        "target": "\(op.target)",
                        "operation": "\(op.operation)"
                    ])
                    if let queueItemId = op.queueItemId {
                        SyncFileManager.shared.releaseFile(for: queueItemId)
                        await syncQueueService?.removeSyncQueueItem(withId: queueItemId)
                        if let queueService = syncQueueService {
                            onQueueCountUpdate?(queueService.getQueueCount())
                        }
                    }
                    // Update progress and continue with the next item.
                    self.syncProgress += 1
                    continue
                }

                // ZP-1265 (aa123c2) — Per-item move-on. Persist the failure
                // on the row (Pending UI shows it inline) and continue to
                // the next item rather than halting the FIFO loop.
                if let queueItemId = op.queueItemId {
                    await syncQueueService?.recordFailure(
                        for: queueItemId,
                        message: errorMessage,
                        code: httpStatusCode,
                        attemptsRun: result.attemptsRun
                    )
                }

                slog("Item failed permanently after retry budget — moving on", category: .sync, level: .warning, data: [
                    "target": "\(op.target)",
                    "operation": "\(op.operation)",
                    "queueItemId": op.queueItemId?.uuidString ?? "nil",
                    "attempts": result.attemptsRun,
                    "http_status": httpStatusCode ?? -1
                ])

                self.syncProgress += 1
                // Fall through to the end-of-iteration so the next item runs.
            }

            // Update progress
            self.syncProgress += 1
        }

        slog("Chunk processing complete", category: .sync, level: .debug)
        return results
    }
}

// Extension for array chunking
extension Array {
    func chunked(into size: Int) -> [[Element]] {
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0 ..< Swift.min($0 + size, count)])
        }
    }
}
