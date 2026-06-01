//
// NodeService.swift
// SwiftDataTutorial
//
// Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftData

/// Data structure for node updates
struct NodeUpdateData {
    let label: String
    let type: String
    let location: String? // Deprecated: Use room instead
    let room: Room?
    let com: Int?
    let comCalculation: COMCalculation?
    let replacementCost: Double?
    let qrCode: String?
    let serviceability: String?
    let serviceabilityNote: String?
    let voltage: Double?
    let voltageId: Int?
    let secondaryVoltage: Double?
    let secondaryVoltageId: Int?
    let notes: String?
    let nodeClass: NodeClass?
    let nodeSubtype: NodeSubtype?
    let originalNodeClass: NodeClass?
    let coreAttributes: [UUID: String]
    let stagedIRPhotos: [IRPhoto]
    let stagedPhotoAdditions: [Photo]
    let stagedPhotoDeletions: Set<UUID>
    let originalPhotos: [Photo]
    let displayedPhotos: [Photo]
    // Performance optimization: Pass SLD directly instead of copying all nodes/edges arrays
    let sld: SLDV2?
    let defaultPhotoId: UUID?
    let suggestedShortcutId: UUID?
    let eqpLibSelection: EqpLibSelection?
}

class NodeService {
    
    // MARK: - Terminal Creation
    
    /// Creates NodeTerminal entities for a newly created node based on its NodeClass's orientation.
    /// This mirrors the terminal creation logic in WebAppContainerViewWithView.swift for React-initiated nodes.
    ///
    /// - Parameters:
    /// - node: The node to create terminals for
    /// - allOrientations: All available NodeOrientation entities to look up terminal blueprints
    /// - modelContext: The SwiftData model context
    static func createTerminalsForNode(
        _ node: NodeV2,
        allOrientations: [NodeOrientation],
        modelContext: ModelContext
    ) {
        // Get the node's class and orientation
        guard let nodeClass = node.node_class,
              let orientationId = nodeClass.node_orientation_id else {
            AppLogger.log(.debug, "Node \(node.id) has no node_class or orientation - skipping terminal creation", category: .node)
            return
        }
        
        // Find the orientation that matches the node class's orientation ID
        guard let orientation = allOrientations.first(where: { $0.id == orientationId }) else {
            AppLogger.log(.debug, "Could not find orientation \(orientationId) for node \(node.id)", category: .node)
            return
        }
        
        // Create a terminal for each blueprint in the orientation
        for blueprint in orientation.orientation_terminals where !blueprint.is_deleted {
            let terminal = NodeTerminal(
                id: UUID(),
                node: node,
                node_orientation_terminal_id: blueprint.id,
                handle_code: blueprint.handle_code,
                label: blueprint.label,
                side: blueprint.side,
                position: blueprint.position,
                offset_percent: blueprint.offset_percent,
                color: blueprint.color,
                show_label: blueprint.show_label
            )
            terminal.needsSync = true
            terminal.created_at = Date()
            modelContext.insert(terminal)
            node.node_terminals.append(terminal)
            AppLogger.log(.debug, "Created terminal \(terminal.id) for node \(node.id) from blueprint \(blueprint.id)", category: .node)
        }
        
        AppLogger.log(.debug, "Created \(orientation.orientation_terminals.filter { !$0.is_deleted }.count) terminals for node \(node.id)", category: .node)
    }
    
    /// Fetches all NodeOrientation entities from the context for terminal creation
    static func fetchAllOrientations(modelContext: ModelContext) -> [NodeOrientation] {
        let descriptor = FetchDescriptor<NodeOrientation>()
        return (try? modelContext.fetch(descriptor)) ?? []
    }
    
    /// Reassigns terminals when a node's class changes.
    /// Returns edges that were modified (for sync queueing).
    @MainActor
    static func reassignTerminalsForNode(
        _ node: NodeV2,
        oldNodeClass: NodeClass?,
        newNodeClass: NodeClass?,
        allOrientations: [NodeOrientation],
        modelContext: ModelContext
    ) -> [EdgeV2] {
        let oldTerminals = node.node_terminals.filter { !$0.is_deleted }
        
        // If no old terminals, just create new ones
        guard !oldTerminals.isEmpty else {
            if newNodeClass != nil {
                createTerminalsForNode(node, allOrientations: allOrientations, modelContext: modelContext)
            }
            return []
        }
        
        // Find edges connected to this node's terminals
        guard let sld = node.sld else { return [] }
        let oldTerminalIds = Set(oldTerminals.map { $0.id })
        
        var sourceEdges: [EdgeV2] = [] // Edges where this node is source
        var targetEdges: [EdgeV2] = [] // Edges where this node is target
        
        for edge in sld.edges where !edge.is_deleted {
            if let id = edge.sourceNodeTerminalId, oldTerminalIds.contains(id) {
                sourceEdges.append(edge)
            }
            if let id = edge.targetNodeTerminalId, oldTerminalIds.contains(id) {
                targetEdges.append(edge)
            }
        }
        
        // Soft-delete old terminals
        for terminal in oldTerminals {
            terminal.is_deleted = true
            terminal.needsSync = true
        }
        
        // Create new terminals from new class
        if newNodeClass != nil {
            createTerminalsForNode(node, allOrientations: allOrientations, modelContext: modelContext)
        }
        
        // Group new terminals by side: "LOAD" = source (output), "LINE" = target (input)
        // Terminals without LINE or LOAD side are ignored (not used for edge reassignment)
        let newTerminals = node.node_terminals.filter { !$0.is_deleted }
        var newSourceTerminals: [NodeTerminal] = []
        var newTargetTerminals: [NodeTerminal] = []
        
        for terminal in newTerminals {
            let side = terminal.side ?? ""
            if side == "LOAD" {
                newSourceTerminals.append(terminal)
            } else if side == "LINE" {
                newTargetTerminals.append(terminal)
            }
            // Terminals with other/empty side values are intentionally ignored
        }
        
        // Reassign source edges (round-robin with fallback to last)
        var modifiedEdges: [EdgeV2] = []
        for (i, edge) in sourceEdges.enumerated() {
            if !newSourceTerminals.isEmpty {
                let idx = min(i, newSourceTerminals.count - 1)
                let newTerminal = newSourceTerminals[idx]
                edge.sourceNodeTerminalId = newTerminal.id
                edge.sourceHandle = newTerminal.handle_code
            } else {
                edge.sourceNodeTerminalId = nil
                edge.sourceHandle = nil
            }
            edge.needsSync = true
            modifiedEdges.append(edge)
        }
        
        // Reassign target edges
        for (i, edge) in targetEdges.enumerated() {
            if !newTargetTerminals.isEmpty {
                let idx = min(i, newTargetTerminals.count - 1)
                let newTerminal = newTargetTerminals[idx]
                edge.targetNodeTerminalId = newTerminal.id
                edge.targetHandle = newTerminal.handle_code
            } else {
                edge.targetNodeTerminalId = nil
                edge.targetHandle = nil
            }
            edge.needsSync = true
            if !modifiedEdges.contains(where: { $0.id == edge.id }) {
                modifiedEdges.append(edge)
            }
        }
        
        AppLogger.log(.info, "Reassigned \(modifiedEdges.count) edges for node \(node.id) class change", category: .node)
        return modifiedEdges
    }
    
    // MARK: - Node Updates
    
    /// Updates a node with all its properties, photos, and IR photos
    @MainActor
    static func updateNode(
        _ node: NodeV2,
        with updates: NodeUpdateData,
        sldService: SLDService,
        networkState: NetworkState,
        modelContext: ModelContext,
        skipGraphUpdate: Bool = false
    ) async throws {
        // Capture original room before overwrite so we can detect a change and cascade
        // the new room to descendants (ZP-2056 parent-child location sync).
        let oldRoom = node.room

        // Apply basic property updates
        node.label = updates.label
        // Preserve "group" type - don't overwrite it
        if node.type != "group" {
            node.type = updates.type
        }
        node.location = updates.location?.isEmpty == true ? nil : updates.location // Deprecated: keeping for backward compatibility
        node.room = updates.room // New: room-based location
        node.com = updates.com
        node.com_calculation = updates.comCalculation
        // ZP-2415: Replacement Cost is part of the new Commercial section.
        node.replacement_cost = updates.replacementCost
        node.qr_code = updates.qrCode?.isEmpty == true ? nil : updates.qrCode
        node.serviceability = updates.serviceability
        node.serviceability_note = updates.serviceabilityNote
        node.voltage = updates.voltage
        node.voltage_id = updates.voltageId
        node.secondary_voltage = updates.secondaryVoltage
        node.secondary_voltage_id = updates.secondaryVoltageId
        node.notes = updates.notes
        node.default_photo_id = updates.defaultPhotoId
        node.suggested_shortcut_id = updates.suggestedShortcutId
        node.eqpLibSelection = updates.eqpLibSelection
        node.lastModifiedAt = Date()
        node.needsSync = true

        // ZP-2056: If the parent's room changed, cascade the new room to all descendants
        // so the invariant "child.room == parent.room" holds throughout the subtree.
        // Only `room` is cascaded; the deprecated `location` string is intentionally left alone.
        var cascadedDescendants: [NodeV2] = []
        let roomChanged = oldRoom?.id != updates.room?.id
        if roomChanged {
            let sldForScan = updates.sld ?? node.sld
            if let sld = sldForScan {
                let descendants = findAllDescendants(of: node.id, in: sld.nodes)
                for child in descendants where child.room?.id != updates.room?.id {
                    child.room = updates.room
                    child.needsSync = true
                    child.lastModifiedAt = Date()
                    cascadedDescendants.append(child)
                }
                if !cascadedDescendants.isEmpty {
                    AppLogger.log(.info,
                                  "Cascaded room to \(cascadedDescendants.count) descendants of node \(node.id)",
                                  category: .node)
                }
            } else {
                AppLogger.log(.notice,
                              "Cannot cascade room change - no SLD reference for node \(node.id)",
                              category: .node)
            }
        }

        // Detect class change and reassign terminals if needed
        let oldNodeClass = node.node_class
        let classChanged = oldNodeClass?.id != updates.nodeClass?.id
        node.node_class = updates.nodeClass
        node.node_subtype = updates.nodeSubtype
        
        var modifiedEdges: [EdgeV2] = []
        if classChanged {
            let allOrientations = fetchAllOrientations(modelContext: modelContext)
            modifiedEdges = reassignTerminalsForNode(
                node,
                oldNodeClass: oldNodeClass,
                newNodeClass: updates.nodeClass,
                allOrientations: allOrientations,
                modelContext: modelContext
            )
            AppLogger.log(.info, "Class changed: reassigned \(modifiedEdges.count) edges", category: .node)
        }
        
        // Apply core attributes changes
        CoreAttributesService.applyCoreAttributeChanges(
            to: node,
            selectedClass: updates.nodeClass,
            originalClass: updates.originalNodeClass,
            draftAttributes: updates.coreAttributes,
            modelContext: modelContext
        )
        
        // Process staged IR photos
        if !updates.stagedIRPhotos.isEmpty {
            IRPhotoService.processStagedIRPhotos(
                for: node,
                stagedIRPhotos: updates.stagedIRPhotos,
                modelContext: modelContext
            )
        }
        
        // Save context
        try modelContext.save()
        
        // Process photo changes
        await PhotoService.processPhotoChanges(
            stagedAdditions: updates.stagedPhotoAdditions,
            stagedDeletions: updates.stagedPhotoDeletions,
            originalPhotos: updates.originalPhotos,
            entity: node,
            modelContext: modelContext,
            networkState: networkState
        )
        
        // Performance optimization: Update graph asynchronously so it doesn't block save/dismiss
        // The DTO creation and JSON encoding can be slow with large graphs (1000+ nodes)
        // Skip graph update when caller will handle it (e.g., view-filtered contexts)
        if !skipGraphUpdate, let sld = updates.sld {
            let sldService = sldService // Capture for async block
            Task { @MainActor in
                let dto = sldService.createDTO(
                    from: sld,
                    customName: "updated-bridge-node"
                )
                WebViewBridge.updateGraph(with: dto, animated: true)
            }
        }
        
        // Check if default_photo_id references a NEW photo (in staged additions)
        // If so, we must upload photos FIRST before sending the node update
        // This ensures the server can validate the default_photo_id reference
        let hasNewDefaultPhoto = updates.defaultPhotoId != nil &&
        updates.stagedPhotoAdditions.contains(where: { $0.id == updates.defaultPhotoId })
        
        // Handle sync based on network mode
        if networkState.canDirectSync {
            // STEP 1: If default photo is NEW, upload ALL photos FIRST and wait
            if hasNewDefaultPhoto {
                AppLogger.log(.info, "📷 Uploading photos FIRST (default photo is new)...", category: .node)
                await PhotoService.uploadPendingPhotos(
                    for: node,
                    stagedDeletions: updates.stagedPhotoDeletions,
                    displayedPhotos: updates.displayedPhotos,
                    modelContext: modelContext,
                    networkState: networkState
                )
                AppLogger.log(.info, "Photos uploaded, now updating node...", category: .node)
            }
            
            // STEP 2: Send node update to server
            // Pre-build API payload synchronously to avoid holding @Model references in async Task
            let nodeId = node.id
            if let nodePayload = try? APIClient.shared.buildNodeUpdatePayload(node) {
                Task {
                    do {
                        _ = try await APIClient.shared.sendNodeUpdate(nodeId: nodeId, payload: nodePayload)
                        let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == nodeId })
                        if let freshNode = try? modelContext.fetch(descriptor).first {
                            freshNode.lastSyncedAt = Date()
                            freshNode.needsSync = false
                        }
                    } catch {
                        AppLogger.log(.info, "Node update failed, queuing for retry: \(error)", category: .node)
                        let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == nodeId })
                        if let freshNode = try? modelContext.fetch(descriptor).first {
                            let op = SyncOp(target: .node, operation: .update, node: freshNode)
                            networkState.enqueue(op)
                        }
                    }
                }
            }
            
            // STEP 3: Upload remaining photos in background (if not already uploaded)
            if !hasNewDefaultPhoto {
                Task.detached {
                    await PhotoService.uploadPendingPhotos(
                        for: node,
                        stagedDeletions: updates.stagedPhotoDeletions,
                        displayedPhotos: updates.displayedPhotos,
                        modelContext: modelContext,
                        networkState: networkState
                    )
                }
            }
            
            // Upload IR photos in background (not affected by default_photo_id)
            Task.detached {
                await IRPhotoService.syncEntityIRPhotos(
                    for: node,
                    stagedIRPhotos: updates.stagedIRPhotos,
                    networkState: networkState,
                    modelContext: modelContext
                )
            }
        } else {
            // Offline mode: queue node update for later sync
            // Photos are already enqueued by PhotoService.processPhotoChanges() (line 265)
            // which handles both additions (.create) and deletions (.update) when offline.

            let op = SyncOp(
                target: .node,
                operation: .update,
                node: node
            )
            networkState.enqueue(op)
        }

        // Sync cascaded descendants (ZP-2056) after the parent's own sync is dispatched.
        // Online: each descendant is sent via APIClient.sendNodeUpdate; failures are enqueued.
        // Offline: each descendant is enqueued via SyncOp.
        syncCascadedDescendants(
            cascadedDescendants,
            networkState: networkState,
            modelContext: modelContext
        )

        // Queue edge updates for modified edges from terminal reassignment
        if !modifiedEdges.isEmpty {
            if networkState.canDirectSync {
                // Pre-build edge payloads synchronously to avoid holding @Model references in async Task
                var edgePayloads: [(edgeId: UUID, payload: Data)] = []
                for edge in modifiedEdges {
                    if let payload = try? APIClient.shared.buildEdgeUpdatePayload(edge) {
                        edgePayloads.append((edgeId: edge.id, payload: payload))
                    }
                }
                if !edgePayloads.isEmpty {
                    Task {
                        for (edgeId, payload) in edgePayloads {
                            do {
                                _ = try await APIClient.shared.sendEdgeUpdate(edgeId: edgeId, payload: payload)
                                let descriptor = FetchDescriptor<EdgeV2>(predicate: #Predicate<EdgeV2> { $0.id == edgeId })
                                if let freshEdge = try? modelContext.fetch(descriptor).first {
                                    freshEdge.lastSyncedAt = Date()
                                    freshEdge.needsSync = false
                                }
                            } catch {
                                let descriptor = FetchDescriptor<EdgeV2>(predicate: #Predicate<EdgeV2> { $0.id == edgeId })
                                if let freshEdge = try? modelContext.fetch(descriptor).first {
                                    let op = SyncOp(target: .edge, operation: .update, edge: freshEdge)
                                    networkState.enqueue(op)
                                }
                            }
                        }
                    }
                }
            } else {
                for edge in modifiedEdges {
                    let op = SyncOp(target: .edge, operation: .update, edge: edge)
                    networkState.enqueue(op)
                }
            }
        }
        
        // Upload photos and IR photos in background
        Task.detached {
            await PhotoService.uploadPendingPhotos(
                for: node,
                stagedDeletions: updates.stagedPhotoDeletions,
                displayedPhotos: updates.displayedPhotos,
                modelContext: modelContext,
                networkState: networkState
            )
            
            await IRPhotoService.syncEntityIRPhotos(
                for: node,
                stagedIRPhotos: updates.stagedIRPhotos,
                networkState: networkState,
                modelContext: modelContext
            )
        }
    }
    
    @MainActor
    static func createNewNodeWithPhotosAndIR(
        node: NodeV2,
        photos: [Photo],
        irPhotos: [IRPhoto],
        networkState: NetworkState,
        modelContext: ModelContext,
        skipGraphUpdate: Bool = false
    ) async {
        AppLogger.log(.info, "createNewNodeWithPhotosAndIR: node=\(node.id), photos=\(photos.count), irPhotos=\(irPhotos.count)", category: .node)

        // GUARD: Check if node already exists in context to prevent duplicates
        let nodeId = node.id // Capture the ID in a local variable
        let existingNodeCheck = try? modelContext.fetch(FetchDescriptor<NodeV2>(
            predicate: #Predicate<NodeV2> { n in n.id == nodeId }
        ))

        // Set sync flags before insertion (same pattern as AddAssetView)
        node.needsSync = true
        node.lastSyncedAt = nil

        if existingNodeCheck?.first != nil {
            AppLogger.log(.notice, "Node \(nodeId) already exists in context, skipping insert", category: .node)
        } else {
            modelContext.insert(node)
        }

        // Explicitly add to SLD's nodes array
        if let sld = node.sld {
            if !sld.nodes.contains(where: { $0.id == node.id }) {
                sld.nodes.append(node)
            }
        } else {
            AppLogger.log(.notice, "Node has no SLD reference", category: .node)
        }

        // Create terminals for the node based on its NodeClass orientation
        // This ensures terminals are created locally for Swift-initiated nodes (AddAssetView)
        // React-initiated nodes already have terminals created by the frontend
        if node.node_terminals.isEmpty {
            let allOrientations = fetchAllOrientations(modelContext: modelContext)
            createTerminalsForNode(node, allOrientations: allOrientations, modelContext: modelContext)
        }

        // Process regular photos
        for photo in photos {
            modelContext.insert(photo)
            node.photos.append(photo)
        }

        // Process IR photos
        for irPhoto in irPhotos {
            // Set the node relationship before inserting
            irPhoto.node = node

            modelContext.insert(irPhoto)
            node.ir_photos.append(irPhoto)

            // IMPORTANT: Ensure the session relationship is properly established
            if let sessionId = irPhoto.ir_session?.id {
                let descriptor = FetchDescriptor<IRSession>(
                    predicate: #Predicate { $0.id == sessionId }
                )

                if let session = try? modelContext.fetch(descriptor).first {
                    if !session.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                        session.ir_photos.append(irPhoto)
                    }
                    irPhoto.ir_session = session
                } else {
                    AppLogger.log(.error, "Could not find session \(sessionId) in context", category: .node)
                }
            }
        }

        do {
            try modelContext.save()

            #if DEBUG
            // Comprehensive state check — only in debug builds
            AppLogger.log(.debug, "State check: node.ir_photos=\(node.ir_photos.count), node.photos=\(node.photos.count)", category: .node)
            for irPhoto in irPhotos {
                if !node.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                    AppLogger.log(.notice, "IR Photo \(irPhoto.id) not in node.ir_photos", category: .node)
                }
                if let session = irPhoto.ir_session,
                   !session.ir_photos.contains(where: { $0.id == irPhoto.id }) {
                    AppLogger.log(.notice, "IR Photo \(irPhoto.id) not in session \(session.id)", category: .node)
                }
            }
            #endif

            // Update the WebView graph to show the new node immediately
            // Skip when caller will handle it (e.g., view-filtered contexts)
            if !skipGraphUpdate {
                WebViewBridge.updateGraphFromSLD(node.sld!.id, in: modelContext, animated: true)
            }

            // Handle server sync
            if networkState.canDirectSync {
                // STEP 1: Upload ALL photos FIRST and wait for completion
                // This ensures default_photo_id references a valid photo on server
                // CRITICAL: Must upload photos BEFORE creating node to prevent sync failure
                let photosToUpload = photos.filter { $0.upload_needed }
                for photo in photosToUpload {
                    await PhotoService.uploadPhoto(
                        photo: photo,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                }

                // STEP 2: THEN create node on server (with valid default_photo_id)
                do {
                    _ = try await APIClient.shared.createNode(node: node)
                    node.needsSync = false
                    node.lastSyncedAt = Date()
                    try? modelContext.save()
                } catch {
                    AppLogger.log(.error, "Failed to sync node to server: \(error)", category: .node)
                    let op = SyncOp(
                        target: .node,
                        operation: .create,
                        node: node
                    )
                    networkState.enqueue(op)
                }

                // STEP 3: Upload IR photos in background (not affected by default_photo_id)
                let irPhotoIds = irPhotos.map { $0.id }
                if !irPhotoIds.isEmpty {
                    Task.detached {
                        let container = modelContext.container
                        let backgroundContext = ModelContext(container)

                        for irPhotoId in irPhotoIds {
                            let descriptor = FetchDescriptor<IRPhoto>(
                                predicate: #Predicate { $0.id == irPhotoId }
                            )
                            if let irPhoto = try? backgroundContext.fetch(descriptor).first {
                                await IRPhotoService.syncIRPhoto(
                                    irPhoto: irPhoto,
                                    networkState: networkState,
                                    modelContext: backgroundContext
                                )
                            }
                        }
                    }
                }
            } else {
                // IMPORTANT: Queue photos FIRST, then node
                // This ensures photos exist on server before node references them via default_photo_id
                for photo in photos {
                    let op = SyncOp(
                        target: .photo,
                        operation: .create,
                        photo: photo
                    )
                    networkState.enqueue(op)
                }

                // Queue the node AFTER photos
                let nodeOp = SyncOp(
                    target: .node,
                    operation: .create,
                    node: node
                )
                networkState.enqueue(nodeOp)

                // Queue IR photos for sync (order doesn't matter for these)
                for irPhoto in irPhotos {
                    let op = SyncOp(
                        target: .irPhoto,
                        operation: .create,
                        irPhoto: irPhoto
                    )
                    networkState.enqueue(op)
                }
            }
        } catch {
            AppLogger.log(.error, "Failed to save new node: \(error)", category: .node)
        }
    }
    
    /// Finds all child nodes recursively for the given parent node IDs
    private static func findAllChildNodes(parentIds: Set<UUID>, in nodes: [NodeV2]) -> Set<UUID> {
        var allChildIds = Set<UUID>()
        var currentLevelIds = parentIds

        while !currentLevelIds.isEmpty {
            var nextLevelIds = Set<UUID>()

            for node in nodes where !node.is_deleted {
                if let parentId = node.parent_id, currentLevelIds.contains(parentId) {
                    allChildIds.insert(node.id)
                    nextLevelIds.insert(node.id)
                }
            }

            currentLevelIds = nextLevelIds
        }

        return allChildIds
    }

    /// BFS descendants of `parentId` within `nodes`. Excludes soft-deleted nodes.
    /// Returns NodeV2 objects; sibling to findAllChildNodes which returns Set<UUID>.
    private static func findAllDescendants(of parentId: UUID, in nodes: [NodeV2]) -> [NodeV2] {
        var descendants: [NodeV2] = []
        var currentLevelIds: Set<UUID> = [parentId]

        while !currentLevelIds.isEmpty {
            var nextLevelIds = Set<UUID>()
            for node in nodes where !node.is_deleted {
                if let pid = node.parent_id, currentLevelIds.contains(pid) {
                    descendants.append(node)
                    nextLevelIds.insert(node.id)
                }
            }
            currentLevelIds = nextLevelIds
        }

        return descendants
    }

    /// Syncs cascaded descendant nodes to the server (online) or enqueues them for later sync (offline).
    /// Mirrors the parent-node sync pattern in `updateNode`: pre-builds payloads on MainActor to
    /// avoid holding @Model references across actor hops, then dispatches a single Task to sync
    /// sequentially. On failure, the descendant is queued via SyncOp for retry.
    @MainActor
    private static func syncCascadedDescendants(
        _ descendants: [NodeV2],
        networkState: NetworkState,
        modelContext: ModelContext
    ) {
        guard !descendants.isEmpty else { return }

        if networkState.canDirectSync {
            var childPayloads: [(childId: UUID, payload: Data)] = []
            for child in descendants {
                if let payload = try? APIClient.shared.buildNodeUpdatePayload(child) {
                    childPayloads.append((childId: child.id, payload: payload))
                }
            }
            guard !childPayloads.isEmpty else { return }

            Task {
                for (childId, payload) in childPayloads {
                    do {
                        _ = try await APIClient.shared.sendNodeUpdate(nodeId: childId, payload: payload)
                        let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == childId })
                        if let freshChild = try? modelContext.fetch(descriptor).first {
                            freshChild.lastSyncedAt = Date()
                            freshChild.needsSync = false
                        }
                    } catch {
                        AppLogger.log(.info,
                                      "Cascade descendant sync failed for \(childId), queuing: \(error)",
                                      category: .node)
                        let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == childId })
                        if let freshChild = try? modelContext.fetch(descriptor).first {
                            networkState.enqueue(SyncOp(target: .node, operation: .update, node: freshChild))
                        }
                    }
                }
            }
        } else {
            for child in descendants {
                networkState.enqueue(SyncOp(target: .node, operation: .update, node: child))
            }
        }
    }

    static func deleteNodes(
        nodeIds: Set<UUID>,
        diagram: SLDV2,
        networkState: NetworkState,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        // Find all child nodes that need to be deleted as well
        let allChildIds = findAllChildNodes(parentIds: nodeIds, in: diagram.nodes)
        let allNodeIds = nodeIds.union(allChildIds)
        
        let nodesToDelete = diagram.nodes.filter { allNodeIds.contains($0.id) && !$0.is_deleted }
        
        guard !nodesToDelete.isEmpty else {
            onCompletion?(false, "No nodes to delete")
            return
        }
        
        // Mark nodes as deleted (soft delete only)
        for node in nodesToDelete {
            node.is_deleted = true
            node.needsSync = true
            node.lastModifiedAt = Date()
        }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Marked \(nodesToDelete.count) nodes as deleted", category: .node)
            
            // Update the graph immediately (toDTO already filters out is_deleted nodes)
            let dto = diagram.toDTO()
            WebViewBridge.updateGraph(with: dto, animated: true)
            
            // Handle server sync
            if networkState.canDirectSync {
                // Pre-build all payloads synchronously to avoid holding @Model references in async Task
                var deletePayloads: [(nodeId: UUID, payload: Data)] = []
                for node in nodesToDelete {
                    if let payload = try? APIClient.shared.buildNodeUpdatePayload(node) {
                        deletePayloads.append((nodeId: node.id, payload: payload))
                    }
                }

                // Sync deletions to server asynchronously
                Task {
                    var failedCount = 0

                    for (deletedNodeId, payload) in deletePayloads {
                        do {
                            _ = try await APIClient.shared.sendNodeUpdate(nodeId: deletedNodeId, payload: payload)
                            AppLogger.log(.info, "Node \(deletedNodeId) deletion synced to server", category: .node)

                            await MainActor.run {
                                let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == deletedNodeId })
                                if let freshNode = try? modelContext.fetch(descriptor).first {
                                    freshNode.lastSyncedAt = Date()
                                    freshNode.needsSync = false
                                }
                            }
                        } catch {
                            AppLogger.log(.error, "Failed to sync node deletion to server: \(error)", category: .node)
                            failedCount += 1

                            await MainActor.run {
                                let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == deletedNodeId })
                                if let freshNode = try? modelContext.fetch(descriptor).first {
                                    let op = SyncOp(target: .node, operation: .update, node: freshNode)
                                    networkState.enqueue(op)
                                }
                            }
                        }
                    }

                    await MainActor.run {
                        if failedCount == 0 {
                            onCompletion?(true, nil)
                        } else {
                            onCompletion?(true, "\(failedCount) deletion(s) will sync when connection improves")
                        }
                    }
                }
            } else {
                // Offline mode - queue for sync
                Task { @MainActor in
                    for node in nodesToDelete {
                        let op = SyncOp(
                            target: .node,
                            operation: .update,
                            node: node
                        )
                        networkState.enqueue(op)
                        AppLogger.log(.info, "Delete of node \(node.id) queued for sync when online", category: .node)
                    }
                    onCompletion?(true, "Nodes deleted locally. Will sync when online.")
                }
            }
            
        } catch {
            AppLogger.log(.error, "Failed to save deletions: \(error)", category: .node)
            onCompletion?(false, "Failed to delete nodes: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Child Node Operations
    
    /// Links an existing node as a child of a parent node
    /// - Sets child's parent_id to parent's id
    /// - Moves child's position to (0, 0) relative to parent
    @MainActor
    static func linkNodeAsChild(
        child: NodeV2,
        parent: NodeV2,
        networkState: NetworkState,
        modelContext: ModelContext,
        skipGraphUpdate: Bool = false
    ) async {
        // Update child's parent reference and position
        child.parent_id = parent.id
        child.x = 0
        child.y = 0

        // ZP-2056: child always inherits parent's room (including nil) to maintain the
        // parent-cascade invariant. This supersedes ZP-1956's conditional fallback; linking
        // under a parent with nil room intentionally clears the child's prior room.
        child.room = parent.room

        child.needsSync = true
        child.lastModifiedAt = Date()

        // ZP-2056: cascade the new room to the linked child's existing descendants so a
        // subtree moved under a new parent stays consistent at every level.
        var cascadedDescendants: [NodeV2] = []
        if let sld = child.sld {
            let descendants = findAllDescendants(of: child.id, in: sld.nodes)
            for grandChild in descendants where grandChild.room?.id != parent.room?.id {
                grandChild.room = parent.room
                grandChild.needsSync = true
                grandChild.lastModifiedAt = Date()
                cascadedDescendants.append(grandChild)
            }
            if !cascadedDescendants.isEmpty {
                AppLogger.log(.info,
                              "Linked child subtree: cascaded room to \(cascadedDescendants.count) descendants",
                              category: .node)
            }
        }

        // Update parent to group style if not already
        let parentNeedsSync = parent.type != "group"
        if parentNeedsSync {
            parent.type = "group"
            parent.needsSync = true
            parent.lastModifiedAt = Date()
        }
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Linked node '\(child.label)' as child of '\(parent.label)'", category: .node)
            
            // Update WebView graph immediately (skip when caller will handle view-filtered refresh)
            if !skipGraphUpdate, let sldId = child.sld?.id {
                WebViewBridge.updateGraphFromSLD(sldId, in: modelContext, animated: true)
            }
            
            // Handle sync for child node
            if networkState.canDirectSync {
                do {
                    _ = try await APIClient.shared.updateNode(child)
                    child.lastSyncedAt = Date()
                    child.needsSync = false
                    try? modelContext.save()
                    AppLogger.log(.info, "Child node link synced to server", category: .node)
                } catch {
                    AppLogger.log(.error, "Failed to sync child node link: \(error)", category: .node)
                    let op = SyncOp(target: .node, operation: .update, node: child)
                    networkState.enqueue(op)
                }
            } else {
                let op = SyncOp(target: .node, operation: .update, node: child)
                networkState.enqueue(op)
                AppLogger.log(.info, "Child node link queued for sync when online", category: .node)
            }
            
            // Sync parent if its type was changed to group
            if parentNeedsSync {
                if networkState.canDirectSync {
                    do {
                        _ = try await APIClient.shared.updateNode(parent)
                        parent.lastSyncedAt = Date()
                        parent.needsSync = false
                        try? modelContext.save()
                        AppLogger.log(.info, "Parent node type changed to group and synced", category: .node)
                    } catch {
                        AppLogger.log(.error, "Failed to sync parent node group change: \(error)", category: .node)
                        let op = SyncOp(target: .node, operation: .update, node: parent)
                        networkState.enqueue(op)
                    }
                } else {
                    let op = SyncOp(target: .node, operation: .update, node: parent)
                    networkState.enqueue(op)
                    AppLogger.log(.info, "Parent node group change queued for sync when online", category: .node)
                }
            }

            // Sync cascaded descendants (ZP-2056) if any grand-descendants inherited the new room.
            syncCascadedDescendants(
                cascadedDescendants,
                networkState: networkState,
                modelContext: modelContext
            )
        } catch {
            AppLogger.log(.error, "Failed to link node as child: \(error)", category: .node)
        }
    }
    
    /// Unlinks a child node from its parent
    /// - Clears child's parent_id
    /// - Moves child's position outside parent bounds
    @MainActor
    static func unlinkChildNode(
        child: NodeV2,
        parent: NodeV2,
        networkState: NetworkState,
        modelContext: ModelContext,
        skipGraphUpdate: Bool = false
    ) async {
        // Move position outside parent bounds (to the right of parent)
        child.x = parent.x + parent.width + 50
        child.y = parent.y
        child.parent_id = nil
        child.needsSync = true
        child.lastModifiedAt = Date()
        
        do {
            try modelContext.save()
            AppLogger.log(.info, "Unlinked node '\(child.label)' from parent '\(parent.label)'", category: .node)
            
            // Update WebView graph immediately (skip when caller will handle view-filtered refresh)
            if !skipGraphUpdate, let sldId = child.sld?.id {
                WebViewBridge.updateGraphFromSLD(sldId, in: modelContext, animated: true)
            }
            
            // Handle sync
            if networkState.canDirectSync {
                do {
                    _ = try await APIClient.shared.updateNode(child)
                    child.lastSyncedAt = Date()
                    child.needsSync = false
                    try? modelContext.save()
                    AppLogger.log(.info, "Child node unlink synced to server", category: .node)
                } catch {
                    AppLogger.log(.error, "Failed to sync child node unlink: \(error)", category: .node)
                    let op = SyncOp(target: .node, operation: .update, node: child)
                    networkState.enqueue(op)
                }
            } else {
                let op = SyncOp(target: .node, operation: .update, node: child)
                networkState.enqueue(op)
                AppLogger.log(.info, "Child node unlink queued for sync when online", category: .node)
            }
        } catch {
            AppLogger.log(.error, "Failed to unlink child node: \(error)", category: .node)
        }
    }
    
    /// Creates a new child node with simplified parameters
    @MainActor
    static func createChildNode(
        label: String,
        nodeClass: NodeClass,
        nodeSubtype: NodeSubtype?,
        parent: NodeV2,
        sld: SLDV2,
        suggestedShortcutId: UUID? = nil,
        networkState: NetworkState,
        modelContext: ModelContext,
        skipGraphUpdate: Bool = false
    ) async -> NodeV2? {
        // Update parent to group style if not already
        // Note: No immediate modelContext.save() here — the save in createNewNodeWithPhotosAndIR
        // will persist both the parent change and the new node together, avoiding a redundant
        // synchronous save on the main thread.
        if parent.type != "group" {
            parent.type = "group"
            parent.needsSync = true
            parent.lastModifiedAt = Date()

            // Sync parent change — pre-build payload to avoid holding @Model in Task
            if networkState.canDirectSync {
                let parentId = parent.id
                if let parentPayload = try? APIClient.shared.buildNodeUpdatePayload(parent) {
                    Task {
                        do {
                            _ = try await APIClient.shared.sendNodeUpdate(nodeId: parentId, payload: parentPayload)
                            let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == parentId })
                            if let freshParent = try? modelContext.fetch(descriptor).first {
                                freshParent.lastSyncedAt = Date()
                                freshParent.needsSync = false
                            }
                        } catch {
                            AppLogger.log(.error, "Failed to sync parent node group change: \(error)", category: .node)
                            let descriptor = FetchDescriptor<NodeV2>(predicate: #Predicate<NodeV2> { $0.id == parentId })
                            if let freshParent = try? modelContext.fetch(descriptor).first {
                                let op = SyncOp(target: .node, operation: .update, node: freshParent)
                                networkState.enqueue(op)
                            }
                        }
                    }
                }
            } else {
                let op = SyncOp(target: .node, operation: .update, node: parent)
                networkState.enqueue(op)
            }
        }
        
        // Create new node with parent relationship
        let newNode = NodeV2(
            id: UUID(),
            label: label,
            type: nodeClass.style,
            sld: sld,
            parent_id: parent.id,
            x: 0, // Relative position within parent
            y: 0,
            width: nodeClass.width,
            height: nodeClass.height,
            photos: [],
            is_deleted: false,
            location: parent.location,
            room: parent.room,
            node_class: nodeClass,
            node_subtype: nodeSubtype,
            core_attributes: [],
            node_tasks: [],
            issues: [],
            ir_photos: [],
            ir_sessions: [],
            com: 1,
            qr_code: nil,
            suggested_shortcut_id: suggestedShortcutId
        )
        
        // Use existing createNewNodeWithPhotosAndIR for consistent sync handling
        await createNewNodeWithPhotosAndIR(
            node: newNode,
            photos: [],
            irPhotos: [],
            networkState: networkState,
            modelContext: modelContext,
            skipGraphUpdate: skipGraphUpdate
        )
        
        return newNode
    }
}
