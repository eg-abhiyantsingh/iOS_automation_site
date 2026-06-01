//
//  DefaultPhotoService.swift
//  SwiftDataTutorial
//
//  Service for managing default photo selection for nodes
//

import Foundation
import SwiftData

class DefaultPhotoService {

    /// Set default photo for a node and sync to backend
    @MainActor
    static func setDefaultPhoto(
        for node: NodeV2,
        photoId: UUID?,
        modelContext: ModelContext,
        networkState: NetworkState
    ) {
        node.default_photo_id = photoId
        node.lastModifiedAt = Date()
        node.needsSync = true

        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save default photo selection: \(error)", category: .photo)
            return
        }

        // Queue for sync if offline, or sync immediately if online
        if networkState.canDirectSync {
            Task {
                do {
                    _ = try await APIClient.shared.updateNode(node)
                    await MainActor.run {
                        node.lastSyncedAt = Date()
                        node.needsSync = false
                        try? modelContext.save()
                    }
                } catch {
                    AppLogger.log(.notice, "Failed to sync default photo: \(error)", category: .photo)
                    // Queue for later sync
                    let op = SyncOp(target: .node, operation: .update, node: node)
                    await MainActor.run {
                        networkState.enqueue(op)
                    }
                }
            }
        } else {
            let op = SyncOp(target: .node, operation: .update, node: node)
            networkState.enqueue(op)
        }
    }

    /// Auto-set default photo when first photo is added
    @MainActor
    static func autoSetDefaultPhotoIfNeeded(
        for node: NodeV2,
        newPhoto: Photo,
        modelContext: ModelContext,
        networkState: NetworkState
    ) {
        // Only auto-set if no default photo exists
        guard node.default_photo_id == nil else { return }

        // Check if this is the first non-deleted photo
        let existingPhotos = node.photos.filter { !$0.is_deleted && $0.id != newPhoto.id }
        guard existingPhotos.isEmpty else { return }

        // Auto-set the new photo as default
        setDefaultPhoto(
            for: node,
            photoId: newPhoto.id,
            modelContext: modelContext,
            networkState: networkState
        )
    }

    /// Handle default photo deletion - auto-select another
    @MainActor
    static func handleDefaultPhotoDeletion(
        for node: NodeV2,
        deletedPhotoId: UUID,
        modelContext: ModelContext,
        networkState: NetworkState
    ) {
        // Only proceed if the deleted photo was the default
        guard node.default_photo_id == deletedPhotoId else { return }

        // Find replacement: prefer profile photos first
        let replacement = node.photos
            .filter { !$0.is_deleted && $0.id != deletedPhotoId }
            .sorted { photo1, photo2 in
                // Prioritize node_profile type
                if photo1.type == "node_profile" && photo2.type != "node_profile" {
                    return true
                }
                if photo1.type != "node_profile" && photo2.type == "node_profile" {
                    return false
                }
                return true
            }
            .first

        // Set new default (nil if no photos left)
        setDefaultPhoto(
            for: node,
            photoId: replacement?.id,
            modelContext: modelContext,
            networkState: networkState
        )
    }

    /// Validate that the photo ID belongs to the node
    static func validateDefaultPhoto(for node: NodeV2, photoId: UUID?) -> Bool {
        guard let photoId = photoId else { return true }  // nil is valid
        return node.photos.contains(where: { $0.id == photoId && !$0.is_deleted })
    }
}
