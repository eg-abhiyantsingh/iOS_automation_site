//
//  IRPhotoService.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftData

class IRPhotoService {
    static func syncIRPhoto(irPhoto: IRPhoto, networkState: NetworkState, modelContext: ModelContext) async {
        do {
            _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
        } catch {
            let op = SyncOp(
                target: .irPhoto,
                operation: .create,
                irPhoto: irPhoto
            )
            await MainActor.run {
                networkState.enqueue(op)
            }
        }
    }
    
    @MainActor
    static func createIRPhoto(
        session: IRSession,
        node: NodeV2,
        sld: SLDV2,
        visualPhotoKey: String,
        irPhotoKey: String,
        modelContext: ModelContext
    ) -> IRPhoto {
        let irPhoto = IRPhoto(
            id: UUID(),
            ir_session: session,
            node: node,
            sld: sld,
            visual_photo_key: visualPhotoKey,
            ir_photo_key: irPhotoKey,
            date_created: Date()
        )
        
        modelContext.insert(irPhoto)
        node.ir_photos.append(irPhoto)
        
        // Update the app state with the entered values
        if session.photo_type == "FLIR-SEP" {
            AppStateManager.shared.setLastCreatedVisualPhotoKey(visualPhotoKey)
            AppStateManager.shared.setLastCreatedIRPhotoKey(irPhotoKey)
        } else if session.photo_type == "FLIR-IND" {
            AppStateManager.shared.setLastCreatedIRPhotoKey(irPhotoKey)
        }
        
        return irPhoto
    }
    
    static func syncPendingIRPhotos(
        for node: NodeV2,
        stagedIRPhotos: [IRPhoto],
        networkState: NetworkState,
        modelContext: ModelContext
    ) async {
        guard networkState.canDirectSync else {
            // Queue for offline sync
            for irPhoto in stagedIRPhotos {
                let op = SyncOp(
                    target: .irPhoto,
                    operation: .create,
                    irPhoto: irPhoto
                )
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
            return
        }
        
        // Sync online
        let irPhotosToSync = node.ir_photos.filter { irPhoto in
            stagedIRPhotos.contains { $0.id == irPhoto.id }
        }
        
        for irPhoto in irPhotosToSync {
            if irPhoto.isSynced { continue }
            
            do {
                _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                await MainActor.run {
                    irPhoto.isSynced = true
                    try? modelContext.save()
                }
            } catch {
                let op = SyncOp(
                    target: .irPhoto,
                    operation: .create,
                    irPhoto: irPhoto
                )
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
        }
    }
    
    // MARK: - IR Photo Sync After Linking Changes
    
    /// Sync IR photos after EntityLinkedList makes linking/unlinking changes
    /// This method handles both newly linked and unlinked photos
    static func syncIRPhotosAfterLinking(
        for issue: Issue,
        stagedIRPhotos: [IRPhoto],
        modelContext: ModelContext,
        networkState: NetworkState,
        previouslyLinkedPhotoIds: Set<UUID>? = nil
    ) async throws {
        guard networkState.canDirectSync else {
            // Queue operations for offline sync
            await queueIRPhotoSync(for: issue, stagedIRPhotos: stagedIRPhotos, networkState: networkState, modelContext: modelContext)
            return
        }
        
        
        // 1. Create staged IR photos first (new ones that don't exist on server)
        for irPhoto in stagedIRPhotos {
            if !irPhoto.isSynced {
                do {
                    _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                    await MainActor.run {
                        irPhoto.isSynced = true
                    }
                } catch {
                    // Queue for later sync
                    let op = SyncOp(target: .irPhoto, operation: .create, irPhoto: irPhoto)
                    await MainActor.run {
                        networkState.enqueue(op)
                    }
                }
            }
        }
        
        // 2. Update all IR photos currently linked to this issue
        for irPhoto in issue.ir_photos {
            do {
                _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                await MainActor.run {
                    irPhoto.isSynced = true
                    try? modelContext.save()
                }
            } catch {
                // Queue for later sync
                let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
        }
        
        // 3. Find and update ALL IR photos from the same node
        // This catches both linked and unlinked photos
        if let node = issue.node {
            let descriptor = FetchDescriptor<IRPhoto>()
            let allPhotos = try modelContext.fetch(descriptor)
            
            // Find all IR photos from the same node
            // Include all photos so we can find recently unlinked ones
            let nodePhotos = allPhotos.filter { irPhoto in
                irPhoto.node.id == node.id &&
                !irPhoto.is_deleted
            }
            
            
            // Sync ALL photos from this node to ensure foreign keys are correct
            // This handles both newly unlinked (issue = nil) and linked photos
            var unlinkedCount = 0
            for irPhoto in nodePhotos {
                // Skip if already processed as linked photo
                if issue.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                    continue // Already processed above
                }
                
                // This is either an unlinked photo or one linked to another issue
                if irPhoto.issue == nil {
                    unlinkedCount += 1
                    
                    // Only sync if the photo exists on the server
                    if irPhoto.isSynced {
                        do {
                            _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                            await MainActor.run {
                                irPhoto.isSynced = true
                                try? modelContext.save()
                            }
                        } catch {
                            // Queue for later sync
                            let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
                            await MainActor.run {
                                networkState.enqueue(op)
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    /// Queue IR photo sync operations for offline mode
    @MainActor
    private static func queueIRPhotoSync(
        for issue: Issue,
        stagedIRPhotos: [IRPhoto],
        networkState: NetworkState,
        modelContext: ModelContext
    ) {
        
        // Queue staged photos for creation
        for irPhoto in stagedIRPhotos {
            if !irPhoto.isSynced {
                let op = SyncOp(target: .irPhoto, operation: .create, irPhoto: irPhoto)
                networkState.enqueue(op)
            }
        }
        
        // Queue linked photos for update
        for irPhoto in issue.ir_photos {
            let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
            networkState.enqueue(op)
        }
        
        // Queue all node photos to ensure foreign keys are synced correctly
        if let node = issue.node {
            let descriptor = FetchDescriptor<IRPhoto>()
            if let allPhotos = try? modelContext.fetch(descriptor) {
                // Get all photos from this node
                let nodePhotos = allPhotos.filter { irPhoto in
                    irPhoto.node.id == node.id &&
                    irPhoto.isSynced &&
                    !irPhoto.is_deleted
                }
                
                // Queue all photos that aren't already queued as linked
                for irPhoto in nodePhotos {
                    // Skip if already queued as linked photo
                    if issue.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                        continue
                    }
                    
                    // Queue unlinked photos for update
                    if irPhoto.issue == nil {
                        let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
                        networkState.enqueue(op)
                    }
                }
            }
        }
        
    }
    
    // MARK: - Generic IR Photo Management for Any Entity
    
    /// Process staged IR photos for any entity (Node or Issue)
    @MainActor
    static func processStagedIRPhotos<T>(
        for entity: T,
        stagedIRPhotos: [IRPhoto],
        modelContext: ModelContext
    ) where T: AnyObject {
        // Add staged IR photos to the context and entity
        for irPhoto in stagedIRPhotos {
            modelContext.insert(irPhoto)
            
            if let node = entity as? NodeV2 {
                node.ir_photos.append(irPhoto)
            } else if let issue = entity as? Issue {
                issue.ir_photos.append(irPhoto)
            }
        }
    }
    
    /// Sync IR photos for issues with linking/unlinking support
    static func syncIssueIRPhotos(
        issue: Issue,
        linkedIRPhotos: [IRPhoto],
        irPhotosToUnlink: Set<UUID>,
        stagedIRPhotos: [IRPhoto],
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        guard networkState.canDirectSync else {
            // Queue operations for offline sync
            for irPhoto in linkedIRPhotos {
                let operation: SyncOperation = stagedIRPhotos.contains(where: { $0.id == irPhoto.id }) ? .create : .update
                let op = SyncOp(
                    target: .irPhoto,
                    operation: operation,
                    irPhoto: irPhoto
                )
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
            return
        }
        
        // Handle unlinking first
        if !irPhotosToUnlink.isEmpty {
            
            if let session = issue.session, let nodeId = issue.node?.id {
                let sessionId = session.id
                let descriptor = FetchDescriptor<IRPhoto>(
                    predicate: #Predicate { irPhoto in
                        irPhoto.ir_session?.id == sessionId &&
                        irPhoto.node.id == nodeId &&
                        !irPhoto.is_deleted
                    }
                )
                
                let availablePhotos = try modelContext.fetch(descriptor)
                for photoId in irPhotosToUnlink {
                    if let irPhoto = availablePhotos.first(where: { $0.id == photoId }) {
                        _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                    }
                }
            }
        }
        
        // Handle linking and creating
        if !linkedIRPhotos.isEmpty {
            
            for (index, irPhoto) in linkedIRPhotos.enumerated() {
                
                // Check if this is a staged photo that needs to be created first
                if stagedIRPhotos.contains(where: { $0.id == irPhoto.id }) {
                    
                    // Create the IR photo on the backend first
                    _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                    
                    // Mark it as synced
                    await MainActor.run {
                        irPhoto.isSynced = true
                    }
                    
                    // Update with the issue link if needed
                    if irPhoto.issue != nil {
                        _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                    }
                } else {
                    // This is an existing photo, just update it
                    _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                }
            }
        }
    }
    
    /// Generic sync method for any entity with IR photos
    static func syncEntityIRPhotos<T>(
        for entity: T,
        stagedIRPhotos: [IRPhoto],
        networkState: NetworkState,
        modelContext: ModelContext
    ) async where T: AnyObject {
        // Get the IR photos for the entity
        let irPhotos: [IRPhoto]
        if let node = entity as? NodeV2 {
            irPhotos = node.ir_photos
        } else if let issue = entity as? Issue {
            irPhotos = issue.ir_photos
        } else {
            return
        }
        
        guard networkState.canDirectSync else {
            // Queue for offline sync
            for irPhoto in stagedIRPhotos {
                let op = SyncOp(
                    target: .irPhoto,
                    operation: .create,
                    irPhoto: irPhoto
                )
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
            return
        }
        
        // Sync online
        let irPhotosToSync = irPhotos.filter { irPhoto in
            stagedIRPhotos.contains { $0.id == irPhoto.id }
        }
        
        for irPhoto in irPhotosToSync {
            if irPhoto.isSynced { continue }
            
            do {
                _ = try await APIClient.shared.createIRPhoto(irPhoto: irPhoto)
                await MainActor.run {
                    irPhoto.isSynced = true
                    try? modelContext.save()
                }
            } catch {
                let op = SyncOp(
                    target: .irPhoto,
                    operation: .create,
                    irPhoto: irPhoto
                )
                await MainActor.run {
                    networkState.enqueue(op)
                }
            }
        }
    }
    
    static func incrementPhotoKey(_ key: String, by increment: Int = 1) -> String? {
        let trimmedKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Find the last continuous sequence of digits at the end of the string
        var digitStartIndex: String.Index? = nil
        
        // Iterate from the end to find where the trailing number begins
        for index in trimmedKey.indices.reversed() {
            if trimmedKey[index].isNumber {
                digitStartIndex = index
            } else {
                // Stop when we hit a non-digit character
                break
            }
        }
        
        // If no trailing digits found, return nil
        guard let startIndex = digitStartIndex else { return nil }
        
        // Extract the prefix and the number part
        let prefix = String(trimmedKey[..<startIndex])
        let numberPart = String(trimmedKey[startIndex...])
        
        // Convert to integer, increment, and format back with same number of digits
        guard let number = Int(numberPart) else { return nil }
        let incrementedNumber = number + increment
        let formattedNumber = String(format: "%0\(numberPart.count)d", incrementedNumber)
        
        return prefix + formattedNumber
    }
    
    static func setupDefaultPhotoKeys(
        for session: IRSession?,
        lastVisualKey: String? = nil,
        lastIRKey: String? = nil
    ) -> (visualKey: String, irKey: String) {
        guard let session = session else {
            return ("", "")
        }
        
        let lastVisual = lastVisualKey ?? AppStateManager.shared.lastCreatedVisualPhotoKey
        let lastIR = lastIRKey ?? AppStateManager.shared.lastCreatedIRPhotoKey
        
        if session.photo_type == "FLIR-SEP" {
            // For FLIR-SEP, try to increment both keys by 2
            let visualKey = lastVisual.flatMap { incrementPhotoKey($0, by: 2) } ?? ""
            let irKey = lastIR.flatMap { incrementPhotoKey($0, by: 2) } ?? ""
            return (visualKey, irKey)
        } else if session.photo_type == "FLIR-IND" {
            // For FLIR-IND, only increment IR key by 1
            let irKey = lastIR.flatMap { incrementPhotoKey($0, by: 1) } ?? ""
            return ("", irKey)
        }
        
        return ("", "")
    }
    
    @MainActor
    static func deleteIRPhoto(
        _ irPhoto: IRPhoto,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        irPhoto.is_deleted = true
        
        do {
            try modelContext.save()
            
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        _ = try await APIClient.shared.updateIRPhoto(irPhoto)
                        await MainActor.run {
                            onCompletion?(true, nil)
                        }
                    } catch {
                        let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
                        await MainActor.run {
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "IR Photo deleted locally, will sync when online")
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .irPhoto, operation: .update, irPhoto: irPhoto)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "IR Photo deleted locally, will sync when online")
            }
        } catch {
            onCompletion?(false, error.localizedDescription)
        }
    }
}
