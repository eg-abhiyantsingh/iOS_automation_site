import Foundation
import SwiftData

// MARK: - Issue Update Data
struct IssueUpdateData {
    let title: String
    let description: String
    let type: String
    let subtype: String
    let status: String
    let proposedResolution: String
    let priority: String?
    let selectedIssueClass: IssueClass?
    let originalIssueClass: IssueClass?
    let draftCoreAttributes: [UUID: String]
    let draftUnits: [UUID: String]
    let stagedPhotoAdditions: [Photo]
    let stagedPhotoDeletions: Set<UUID>
    let originalPhotos: [Photo]
    let stagedIRPhotos: [IRPhoto]
    let immediateHazard: Bool
    let customerNotified: Bool
    let draftCoreAttributeDescriptions: [UUID: String]
}

class IssueService {
    
    @MainActor
    static func createIssue(
        title: String,
        description: String,
        proposedResolution: String?,
        priority: String? = nil,
        issueClass: IssueClass?,
        node: NodeV2,
        sld: SLDV2,
        modelContext: ModelContext,
        activeSession: IRSession? = nil,
        immediateHazard: Bool = false,
        customerNotified: Bool = false,
        details: [IssueProperty] = [],
        onCompletion: ((Bool, Issue?, String?) -> Void)? = nil
    ) -> Issue? {
        guard !title.isEmpty, issueClass != nil else {
            AppLogger.log(.notice, "Cannot create issue without title and issue class", category: .issue)
            onCompletion?(false, nil, "Title and issue class are required")
            return nil
        }
        
        let newIssue = Issue(
            id: UUID(),
            title: title,
            issueDescription: description,
            created_date: Date(),
            node: node,
            issue_class: issueClass,
            issue_type: nil,  // Deprecated - always nil
            issue_subtype: nil,  // Deprecated - always nil
            is_deleted: false,
            session: activeSession ?? AppStateManager.shared.activeSession,  // Direct 1:1 relationship
            sld: sld,
            details: [],
            status: "Open",
            proposed_resolution: proposedResolution?.isEmpty == false ? proposedResolution : nil,
            modified_date: Date(),
            priority: priority,
            tasks: [],
            ir_photos: [],
            photos: [],
            immediateHazard: immediateHazard,
            customerNotified: customerNotified
        )
        newIssue.details = details

        modelContext.insert(newIssue)
        node.issues.append(newIssue)
        
        // Add to IRSession's issues array if there's an active session
        if let session = activeSession ?? AppStateManager.shared.activeSession {
            session.issues.append(newIssue)
        }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Created issue locally: \(newIssue.title ?? "Untitled")", category: .issue)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.createIssue(issue: newIssue)
                        AppLogger.log(.info, "Issue created on server", category: .issue)
                        
                        await MainActor.run {
                            newIssue.modified_date = Date()
                            try? modelContext.save()
                            onCompletion?(true, newIssue, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue to server: \(error)", category: .issue)
                        let op = SyncOp(target: .issue, operation: .create, issue: newIssue)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, newIssue, "Issue created locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .issue, operation: .create, issue: newIssue)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, newIssue, "Issue created locally, will sync when online")
            }
            return newIssue
        } catch {
            AppLogger.log(.error, "Failed to save new issue: \(error)", category: .issue)
            onCompletion?(false, nil, error.localizedDescription)
            return nil
        }
    }
    
    @MainActor
    static func deleteIssue(
        _ issue: Issue,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.is_deleted = true
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Issue marked as deleted locally", category: .issue)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateIssue(issue)
                        AppLogger.log(.info, "Issue deletion synced to server", category: .issue)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue deletion: \(error)", category: .issue)
                        let op = SyncOp(target: .issue, operation: .update, issue: issue)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Issue deleted locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .issue, operation: .update, issue: issue)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Issue deleted locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to delete issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    @MainActor
    static func updateIssue(
        _ issue: Issue,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Issue updated locally", category: .issue)

            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateIssue(issue)
                        AppLogger.log(.info, "Issue update synced to server", category: .issue)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue update: \(error)", category: .issue)
                        let op = SyncOp(target: .issue, operation: .update, issue: issue)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Issue updated locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .issue, operation: .update, issue: issue)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Issue updated locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to update issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    // MARK: - Comprehensive Update with All Services
    @MainActor
    static func updateIssueComprehensive(
        _ issue: Issue,
        with updates: IssueUpdateData,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        AppLogger.log(.info, "Starting comprehensive issue update - Title: \(updates.title), Status: \(updates.status), Type: \(updates.type), Subtype: \(updates.subtype), Class: \(updates.selectedIssueClass?.name ?? "None"), Core Attributes: \(updates.draftCoreAttributes.count), Photo Additions: \(updates.stagedPhotoAdditions.count), Photo Deletions: \(updates.stagedPhotoDeletions.count), Network Mode: \(networkState.mode)", category: .issue)
        
        // Step 1: Update basic issue properties
        AppLogger.log(.debug, "Applying changes to issue...", category: .issue)
        issue.title = updates.title.isEmpty ? nil : updates.title
        issue.issueDescription = updates.description.isEmpty ? nil : updates.description
        issue.issue_type = updates.type.isEmpty ? nil : updates.type
        issue.issue_subtype = updates.subtype.isEmpty ? nil : updates.subtype
        issue.status = updates.status
        issue.proposed_resolution = updates.proposedResolution.isEmpty ? nil : updates.proposedResolution
        issue.priority = updates.priority
        issue.immediateHazard = updates.immediateHazard
        issue.customerNotified = updates.customerNotified
        issue.issue_class = updates.selectedIssueClass
        issue.modified_date = Date()
        AppLogger.log(.debug, "Issue properties updated", category: .issue)

        // Step 2: Apply core attribute changes using CoreAttributesService
        AppLogger.log(.debug, "Applying core attribute changes...", category: .issue)
        CoreAttributesService.applyCoreAttributeChanges(
            to: issue,
            selectedClass: updates.selectedIssueClass,
            originalClass: updates.originalIssueClass,
            draftAttributes: updates.draftCoreAttributes,
            draftUnits: updates.draftUnits,
            draftDescriptions: updates.draftCoreAttributeDescriptions,
            modelContext: modelContext
        )
        AppLogger.log(.debug, "Core attributes updated via CoreAttributesService", category: .issue)

        // Step 3: Process photo changes using PhotoService
        AppLogger.log(.debug, "Processing photo changes...", category: .issue)
        PhotoService.processPhotoChanges(
            stagedAdditions: updates.stagedPhotoAdditions,
            stagedDeletions: updates.stagedPhotoDeletions,
            originalPhotos: updates.originalPhotos,
            entity: issue,
            modelContext: modelContext,
            networkState: networkState
        )
        AppLogger.log(.debug, "Photo changes processed via PhotoService", category: .issue)

        // Step 4: Save the context
        AppLogger.log(.debug, "Attempting to save context...", category: .issue)
        try modelContext.save()
        AppLogger.log(.debug, "Context saved successfully", category: .issue)

        // Step 5: Handle network sync
        if networkState.canDirectSync {
            AppLogger.log(.debug, "Online mode - starting async sync operations...", category: .issue)
            
            do {
                // Update issue on server
                AppLogger.log(.debug, "Updating issue on server...", category: .issue)
                _ = try await APIClient.shared.updateIssue(issue)
                AppLogger.log(.info, "Issue updated on server successfully", category: .issue)
                
                // Upload photos and handle deletions
                if !updates.stagedPhotoAdditions.isEmpty || !updates.stagedPhotoDeletions.isEmpty {
                    AppLogger.log(.debug, "Processing \(updates.stagedPhotoAdditions.count) new photos and \(updates.stagedPhotoDeletions.count) deletions...", category: .issue)
                    await PhotoService.uploadPendingPhotos(
                        for: issue,
                        stagedDeletions: updates.stagedPhotoDeletions,
                        displayedPhotos: updates.originalPhotos,
                        modelContext: modelContext,
                        networkState: networkState
                    )
                    AppLogger.log(.info, "Photo operations completed", category: .issue)
                }
                
                // Sync IR photos if needed
                if !updates.stagedIRPhotos.isEmpty {
                    AppLogger.log(.debug, "Syncing IR photos...", category: .issue)
                    try await IRPhotoService.syncIRPhotosAfterLinking(
                        for: issue,
                        stagedIRPhotos: updates.stagedIRPhotos,
                        modelContext: modelContext,
                        networkState: networkState
                    )
                    AppLogger.log(.info, "IR photo sync completed", category: .issue)
                }
                
                AppLogger.log(.info, "All sync operations completed successfully", category: .issue)
                
            } catch {
                AppLogger.log(.error, "Online save failed: \(error) - \(error.localizedDescription)", category: .issue)

                // Queue for sync if online update fails
                AppLogger.log(.debug, "Queueing issue for later sync...", category: .issue)
                let op = SyncOp(
                    target: .issue,
                    operation: .update,
                    issue: issue
                )
                networkState.enqueue(op)
                
                // Photo operations are already queued by PhotoService.processPhotoChanges
                throw error
            }
        } else {
            AppLogger.log(.debug, "Offline mode - queueing all operations for sync...", category: .issue)
            
            // Queue issue for sync
            let op = SyncOp(
                target: .issue,
                operation: .update,
                issue: issue
            )
            networkState.enqueue(op)
            AppLogger.log(.info, "Issue queued for sync", category: .issue)

            // Photo operations are already queued by PhotoService.processPhotoChanges
            AppLogger.log(.debug, "Photo operations already queued by PhotoService", category: .issue)
            
            // Queue IR photo operations if needed
            if !updates.stagedIRPhotos.isEmpty {
                try await IRPhotoService.syncIRPhotosAfterLinking(
                    for: issue,
                    stagedIRPhotos: updates.stagedIRPhotos,
                    modelContext: modelContext,
                    networkState: networkState
                )
                AppLogger.log(.info, "IR photo operations queued", category: .issue)
            }
            
            AppLogger.log(.info, "Offline save completed", category: .issue)
        }
    }
    
    @MainActor
    static func updateIssueWithDetails(
        _ issue: Issue,
        title: String,
        description: String,
        type: String,
        subtype: String,
        status: String,
        proposedResolution: String,
        issueClass: IssueClass?,
        coreAttributes: [UUID: String],
        stagedPhotoAdditions: [Photo],
        stagedPhotoDeletions: Set<UUID>,
        linkedIRPhotos: [IRPhoto],
        irPhotosToUnlink: Set<UUID>,
        stagedIRPhotos: [IRPhoto],
        modelContext: ModelContext,
        onCompletion: @escaping (Bool, String?) -> Void
    ) {
        // Apply changes to the issue
        issue.title = title.isEmpty ? nil : title
        issue.issueDescription = description.isEmpty ? nil : description
        issue.issue_type = type.isEmpty ? nil : type
        issue.issue_subtype = subtype.isEmpty ? nil : subtype
        issue.status = status
        issue.proposed_resolution = proposedResolution.isEmpty ? nil : proposedResolution
        issue.issue_class = issueClass
        issue.modified_date = Date()
        
        // Apply core attribute changes inline
        if issueClass?.id != issue.issue_class?.id {
            // Delete all existing core attributes if class changed
            for attr in issue.details {
                modelContext.delete(attr)
            }
            issue.details.removeAll()
        }
        
        // Apply new attributes if there's a selected issue class
        if let issueClass = issueClass {
            let existingAttrs = Dictionary(
                uniqueKeysWithValues: issue.details.map { ($0.id, $0) }
            )
            
            for classProperty in issueClass.definition {
                let propertyId = classProperty.id
                
                if let newValue = coreAttributes[propertyId] {
                    if newValue.isEmpty && !classProperty.af_required {
                        continue
                    }
                    
                    if let existingAttr = existingAttrs[propertyId] {
                        if existingAttr.value != newValue {
                            existingAttr.value = newValue
                            existingAttr.issue_class_property = classProperty
                        }
                    } else {
                        let newAttr = IssueProperty(
                            id: propertyId,
                            issue_class_property: classProperty,
                            name: classProperty.name,
                            value: newValue
                        )
                        modelContext.insert(newAttr)
                        issue.details.append(newAttr)
                    }
                } else if let existingAttr = existingAttrs[propertyId] {
                    issue.details.removeAll { $0.id == propertyId }
                    modelContext.delete(existingAttr)
                }
            }
        }
        
        // Process photo changes
        for photoId in stagedPhotoDeletions {
            if let photo = issue.photos.first(where: { $0.id == photoId }) {
                photo.is_deleted = true
            }
        }
        
        for photo in stagedPhotoAdditions {
            modelContext.insert(photo)
        }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Issue and details saved locally", category: .issue)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        // Update issue
                        _ = try await APIClient.shared.updateIssue(issue)
                        
                        // Handle photo operations
                        for photo in stagedPhotoAdditions {
                            guard let filename = photo.filename,
                                  let fileURL = photo.localFileURL,
                                  let data = try? Data(contentsOf: fileURL) else { continue }
                            
                            await uploadPhoto(photo: photo, data: data, filename: filename, modelContext: modelContext)
                        }
                        
                        for photoId in stagedPhotoDeletions {
                            if let photo = issue.photos.first(where: { $0.id == photoId }) {
                                // Mark photo as deleted before updating
                                photo.is_deleted = true
                                _ = try? await APIClient.shared.updatePhoto(photo)
                            }
                        }
                        
                        // Handle IR photo operations
                        for irPhoto in linkedIRPhotos {
                            if stagedIRPhotos.contains(where: { $0.id == irPhoto.id }) {
                                _ = try? await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                                irPhoto.isSynced = true
                            }
                            _ = try? await APIClient.shared.updateIRPhoto(irPhoto)
                        }
                        
                        await MainActor.run {
                            onCompletion(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue updates: \(error)", category: .issue)
                        let op = SyncOp(target: .issue, operation: .update, issue: issue)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            
                            // Queue photo operations
                            for photo in stagedPhotoAdditions {
                                let photoOp = SyncOp(target: .photo, operation: .create, photo: photo)
                                NetworkState.shared.enqueue(photoOp)
                            }
                            
                            for photoId in stagedPhotoDeletions {
                                if let photo = issue.photos.first(where: { $0.id == photoId }) {
                                    let photoOp = SyncOp(target: .photo, operation: .update, photo: photo)
                                    NetworkState.shared.enqueue(photoOp)
                                }
                            }
                            
                            onCompletion(true, "Issue updated locally, will sync when online")
                        }
                    }
                }
            } else {
                // Queue everything for offline sync in a single batch save
                var ops: [SyncOp] = []
                ops.append(SyncOp(target: .issue, operation: .update, issue: issue))

                for photo in stagedPhotoAdditions {
                    ops.append(SyncOp(target: .photo, operation: .create, photo: photo))
                }

                for photoId in stagedPhotoDeletions {
                    if let photo = issue.photos.first(where: { $0.id == photoId }) {
                        ops.append(SyncOp(target: .photo, operation: .update, photo: photo))
                    }
                }

                NetworkState.shared.enqueueBatch(ops)
                onCompletion(true, "Issue updated locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to save issue updates: \(error)", category: .issue)
            onCompletion(false, error.localizedDescription)
        }
    }
    
    /// Upload a photo for an issue. Follows the same state machine as PhotoService/PhotoUploadService:
    ///   - `upload_needed=true,  url=nil`      → full upload
    ///   - `upload_needed=true,  url=<s3_url>` → resume at createPhoto (S3 already done)
    ///   - `upload_needed=false`               → already synced, no-op
    ///
    /// `upload_needed=false` is committed ONLY after `api.createPhoto` succeeds, so a crash or
    /// network failure between S3 and the server record cannot mark the photo "synced" when it
    /// isn't. The sweeper (StuckPhotoRecoveryService) will recover photos left in the intermediate
    /// state on the next boot or online transition.
    private static func uploadPhoto(photo: Photo, data: Data, filename: String, modelContext: ModelContext) async {
        // Already fully synced — nothing to do.
        if !photo.upload_needed { return }

        let s3AlreadyDone = !(photo.url?.isEmpty ?? true)

        do {
            if !s3AlreadyDone {
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
                    throw NSError(domain: "PhotoUpload", code: 2,
                                  userInfo: [NSLocalizedDescriptionKey: "Invalid HTTP response from S3"])
                }
                guard http.statusCode == 200 else {
                    // Throw — do NOT silently return. The sweeper depends on upload_needed=true
                    // being an accurate signal of "not yet synced".
                    throw NSError(domain: "PhotoUpload", code: http.statusCode,
                                  userInfo: [NSLocalizedDescriptionKey: "S3 upload failed with status: \(http.statusCode)"])
                }

                // Persist S3 URL. upload_needed stays true until the server confirms.
                await MainActor.run {
                    photo.url = presignedResponse.public_url
                    try? modelContext.save()
                }
            }

            // Create server record. This is the step that makes the photo "real" on the server.
            _ = try await APIClient.shared.createPhoto(photo: photo)

            // Only now is it safe to mark synced.
            await MainActor.run {
                photo.upload_needed = false
                try? modelContext.save()
            }
        } catch {
            AppLogger.log(.error, "Photo upload error: \(error)", category: .issue)
            // Leave state as-is (upload_needed=true, url possibly set). The sweeper or the next
            // issue-save flow will retry. DO NOT flip upload_needed here.
        }
    }
    
    @MainActor
    static func resolveIssue(
        _ issue: Issue,
        resolution: String? = nil,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.status = "Resolved"
        issue.modified_date = Date()
        if let resolution = resolution, !resolution.isEmpty {
            issue.proposed_resolution = resolution
        }
        
        updateIssue(issue, modelContext: modelContext, onCompletion: onCompletion)
    }
    
    @MainActor
    static func reopenIssue(
        _ issue: Issue,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.status = "Open"
        issue.modified_date = Date()
        
        updateIssue(issue, modelContext: modelContext, onCompletion: onCompletion)
    }
    
    @MainActor
    static func addTaskToIssue(
        _ issue: Issue,
        task: UserTask,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.tasks.append(task)
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Task added to issue locally", category: .issue)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.createIssueTaskMapping(
                            issueId: issue.id,
                            taskId: task.id
                        )
                        AppLogger.log(.info, "Issue-task mapping created on server", category: .issue)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue-task mapping: \(error)", category: .issue)
                        // Queue the mapping for later sync
                        await MainActor.run {
                            NetworkState.shared.enqueueIssueTaskMapping(
                                issueId: issue.id,
                                taskId: task.id,
                                isDeleted: false
                            )
                            onCompletion?(true, "Task added to issue locally, will sync when online")
                        }
                    }
                }
            } else {
                NetworkState.shared.enqueueIssueTaskMapping(
                    issueId: issue.id,
                    taskId: task.id,
                    isDeleted: false
                )
                onCompletion?(true, "Task added to issue locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to add task to issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    @MainActor
    static func removeTaskFromIssue(
        _ issue: Issue,
        task: UserTask,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.tasks.removeAll { $0.id == task.id }
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Task removed from issue locally", category: .issue)
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateIssueTaskMapping(
                            issueId: issue.id,
                            taskId: task.id,
                            isDeleted: true
                        )
                        AppLogger.log(.info, "Issue-task mapping removed on server", category: .issue)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        AppLogger.log(.notice, "Failed to sync issue-task mapping removal: \(error)", category: .issue)
                        await MainActor.run {
                            NetworkState.shared.enqueueIssueTaskMapping(
                                issueId: issue.id,
                                taskId: task.id,
                                isDeleted: true
                            )
                            onCompletion?(true, "Task removed from issue locally, will sync when online")
                        }
                    }
                }
            } else {
                NetworkState.shared.enqueueIssueTaskMapping(
                    issueId: issue.id,
                    taskId: task.id,
                    isDeleted: true
                )
                onCompletion?(true, "Task removed from issue locally, will sync when online")
            }
        } catch {
            AppLogger.log(.error, "Failed to remove task from issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    @MainActor
    static func addPhotoToIssue(
        _ issue: Issue,
        photo: Photo,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.photos.append(photo)
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Photo added to issue", category: .issue)
            onCompletion?(true, nil)
        } catch {
            AppLogger.log(.error, "Failed to add photo to issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
    
    @MainActor
    static func addIRPhotoToIssue(
        _ issue: Issue,
        irPhoto: IRPhoto,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        issue.ir_photos.append(irPhoto)
        issue.modified_date = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "IR Photo added to issue", category: .issue)
            onCompletion?(true, nil)
        } catch {
            AppLogger.log(.error, "Failed to add IR photo to issue: \(error)", category: .issue)
            onCompletion?(false, error.localizedDescription)
        }
    }
}
