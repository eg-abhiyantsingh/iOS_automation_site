//
//  CopyAssetsToRoomsService.swift
//  Egalvanic PZ
//
//  ZP-2198: Helpers for cloning session-mapped assets from a source room into one
//  or more target rooms (and unmapping target-room session assets when the user
//  picked the OVERWRITE strategy). Mirrors the Android CopyAssetsToRoomsViewModel
//  cloning logic from commit cea4810 (ZP-2199).
//

import Foundation
import SwiftData

@MainActor
enum CopyAssetsToRoomsService {

    /// Result of cloning all assets for a single target room.
    struct CloneCounts {
        var created: Int = 0
        var failed: Int = 0
    }

    enum CopyError: Error {
        /// One or more session unmaps failed for the target room. The caller should mark the
        /// room as skipped instead of attempting any cloning into it (halt-on-unlink-failure).
        case unmapFailed(String)
    }

    // MARK: - Clone parent + children

    /// Clones a single source parent (and its children) into the target room and maps each new
    /// node to `session`. Updates `cloneMap` with sourceId → newId mappings for both parents
    /// and children so edges can be reconstructed afterwards.
    static func cloneParentAndChildren(
        sourceParent: NodeV2,
        sourceChildren: [NodeV2],
        targetRoom: Room,
        session: IRSession,
        sld: SLDV2,
        cloneMap: inout [UUID: UUID],
        modelContext: ModelContext,
        networkState: NetworkState
    ) async -> CloneCounts {
        var counts = CloneCounts()

        // 1. Clone the parent.
        let newParent = makeClone(
            of: sourceParent,
            targetRoom: targetRoom,
            sld: sld,
            newParentId: nil,
            isParent: true
        )
        // Apply core attributes BEFORE NodeService creates the node so they're included in
        // both the local SwiftData insert and the server-side createNode payload.
        applyClonedCoreAttributes(from: sourceParent, to: newParent, modelContext: modelContext)

        await NodeService.createNewNodeWithPhotosAndIR(
            node: newParent,
            photos: [],
            irPhotos: [],
            networkState: networkState,
            modelContext: modelContext,
            skipGraphUpdate: true
        )
        await mapNodeToSession(newParent, session: session, networkState: networkState)
        cloneMap[sourceParent.id] = newParent.id
        counts.created += 1

        // 2. Clone each child under the new parent.
        for sourceChild in sourceChildren {
            let newChild = makeClone(
                of: sourceChild,
                targetRoom: targetRoom,
                sld: sld,
                newParentId: newParent.id,
                isParent: false
            )
            applyClonedCoreAttributes(from: sourceChild, to: newChild, modelContext: modelContext)

            await NodeService.createNewNodeWithPhotosAndIR(
                node: newChild,
                photos: [],
                irPhotos: [],
                networkState: networkState,
                modelContext: modelContext,
                skipGraphUpdate: true
            )
            await mapNodeToSession(newChild, session: session, networkState: networkState)
            cloneMap[sourceChild.id] = newChild.id
            counts.created += 1
        }

        return counts
    }

    /// Copies `source.core_attributes` onto `target` using the canonical iOS pattern
    /// (`CoreAttributesService.applyCoreAttributeChanges`). This guarantees:
    /// 1. each new `NodeProperty` is inserted into `modelContext` (without this, SwiftData's
    ///    auto-tracking of array relationships isn't reliable enough to persist them to disk
    ///    on save), and
    /// 2. `NodeProperty.id == NodeClassProperty.id`, which is the lookup key used by
    ///    `NodeV2.af_completion` and other places that read `attributeValues[classProperty.id]`.
    private static func applyClonedCoreAttributes(
        from source: NodeV2,
        to target: NodeV2,
        modelContext: ModelContext
    ) {
        guard let nodeClass = target.node_class else { return }
        let draft: [UUID: String] = Dictionary(uniqueKeysWithValues:
            source.core_attributes.compactMap { attr -> (UUID, String)? in
                guard let classProp = attr.node_class_property else { return nil }
                return (classProp.id, attr.value)
            }
        )
        guard !draft.isEmpty else { return }
        CoreAttributesService.applyCoreAttributeChanges(
            to: target,
            selectedClass: nodeClass,
            originalClass: nil,
            draftAttributes: draft,
            modelContext: modelContext
        )
    }

    // MARK: - Clone intra-room edges

    /// Clones edges where BOTH endpoints were cloned (i.e. both source and target node IDs
    /// are present in `cloneMap`). Resolves fresh terminal IDs by matching `handle_code`
    /// against the cloned node's auto-generated terminals.
    /// Returns the count of edges successfully created and the count that failed (e.g. when a
    /// handle code couldn't be resolved on the new node).
    static func cloneIntraRoomEdges(
        sourceEdges: [EdgeV2],
        cloneMap: [UUID: UUID],
        sld: SLDV2,
        modelContext: ModelContext,
        networkState: NetworkState
    ) async -> CloneCounts {
        var counts = CloneCounts()

        // Build a quick lookup from new-node id → NodeV2 so we can read terminals.
        let newNodeIds = Set(cloneMap.values)
        let newNodesById: [UUID: NodeV2] = Dictionary(uniqueKeysWithValues:
            sld.nodes.filter { newNodeIds.contains($0.id) }.map { ($0.id, $0) }
        )

        for sourceEdge in sourceEdges where !sourceEdge.is_deleted {
            guard
                let srcId = sourceEdge.source,
                let tgtId = sourceEdge.target,
                let newSrcId = cloneMap[srcId],
                let newTgtId = cloneMap[tgtId],
                let newSrcNode = newNodesById[newSrcId],
                let newTgtNode = newNodesById[newTgtId]
            else {
                continue
            }

            let newSrcTerminalId = newSrcNode.node_terminals
                .first(where: { !$0.is_deleted && $0.handle_code == sourceEdge.sourceHandle })?.id
            let newTgtTerminalId = newTgtNode.node_terminals
                .first(where: { !$0.is_deleted && $0.handle_code == sourceEdge.targetHandle })?.id

            // Skip if either side requires a handle but the cloned node doesn't expose it
            // (e.g. node class changed or orientation drifted between source and clone).
            if sourceEdge.sourceHandle != nil && newSrcTerminalId == nil {
                counts.failed += 1
                continue
            }
            if sourceEdge.targetHandle != nil && newTgtTerminalId == nil {
                counts.failed += 1
                continue
            }

            // EdgeProperty follows the iOS convention of EdgeProperty.id == EdgeClassProperty.id
            // so EdgeV2.af_completion's `attributeValues[classProperty.id]` lookup resolves.
            // EdgeService.createEdge handles inserting each EdgeProperty into modelContext.
            let newCoreAttrs: [EdgeProperty] = sourceEdge.core_attributes.compactMap { attr in
                guard let classProp = attr.edge_class_property else { return nil }
                return EdgeProperty(
                    id: classProp.id,
                    edge_class_property: classProp,
                    name: attr.name,
                    value: attr.value
                )
            }

            let newEdge = EdgeV2(
                id: UUID(),
                source: newSrcId,
                target: newTgtId,
                sld: sld,
                is_deleted: false,
                sourceHandle: sourceEdge.sourceHandle,
                targetHandle: sourceEdge.targetHandle,
                sourceNodeTerminalId: newSrcTerminalId,
                targetNodeTerminalId: newTgtTerminalId,
                edge_class: sourceEdge.edge_class,
                core_attributes: newCoreAttrs,
                points: sourceEdge.points,
                algorithm: sourceEdge.algorithm
            )

            await EdgeService.createEdge(
                edge: newEdge,
                diagram: sld,
                networkState: networkState,
                modelContext: modelContext
            )
            counts.created += 1
        }

        return counts
    }

    // MARK: - OVERWRITE strategy: unmap every session-mapped asset in target room

    /// Unmaps every session-mapped asset (parents AND children) in `targetRoom` from
    /// `session`. Throws on the first failure so the caller can skip the room entirely
    /// (halt-on-unlink-failure to avoid leaving the target room half-cleaned).
    ///
    /// ZP-2234: previously this walked top-level session parents → SLD descendants. That
    /// missed any session-mapped child whose parent wasn't itself in the session, leaving
    /// the child "ghost-mapped" alongside the freshly-cloned subtree. Mirroring the
    /// Android fix, we now iterate every node in `session.nodes` that lives in the target
    /// room — no parent/child distinction.
    static func unmapSessionAssets(
        targetRoom: Room,
        session: IRSession,
        modelContext: ModelContext,
        networkState: NetworkState
    ) async throws {
        let nodesToUnmap = session.nodes.filter {
            !$0.is_deleted && $0.room?.id == targetRoom.id
        }
        guard !nodesToUnmap.isEmpty else { return }

        for node in nodesToUnmap {
            do {
                session.nodes.removeAll { $0.id == node.id }
                node.ir_sessions.removeAll { $0.id == session.id }

                if networkState.mode == .online {
                    do {
                        _ = try await APIClient.shared.updateNodeSessionMapping(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: true
                        )
                    } catch {
                        networkState.enqueue(SyncOp(
                            target: .mappingNodeSession,
                            operation: .delete,
                            mappingData: MappingData.nodeSession(
                                nodeId: node.id,
                                sessionId: session.id,
                                isDeleted: true
                            )
                        ))
                    }
                } else {
                    networkState.enqueue(SyncOp(
                        target: .mappingNodeSession,
                        operation: .delete,
                        mappingData: MappingData.nodeSession(
                            nodeId: node.id,
                            sessionId: session.id,
                            isDeleted: true
                        )
                    ))
                }
            }
        }

        do {
            try modelContext.save()
        } catch {
            throw CopyError.unmapFailed(error.localizedDescription)
        }
    }

    // MARK: - Helpers

    /// Builds a fresh NodeV2 cloned from `source` for placement in `targetRoom`. The new
    /// node id is regenerated. Photos, IR photos, QR, default photo, terminals, tasks, and
    /// issues are intentionally NOT carried over (matches Android v1 scope). Terminals are
    /// auto-generated by `NodeService.createNewNodeWithPhotosAndIR` from the node class
    /// orientation; `core_attributes` are applied separately via `applyClonedCoreAttributes`
    /// after the new node has its node class set, which mirrors `AddAssetViewModel.performSave`.
    private static func makeClone(
        of source: NodeV2,
        targetRoom: Room,
        sld: SLDV2,
        newParentId: UUID?,
        isParent: Bool
    ) -> NodeV2 {
        let cloned = NodeV2(
            id: UUID(),
            label: source.label,
            type: source.type,
            sld: sld,
            parent_id: newParentId,
            x: isParent ? Double.random(in: -1000...1000) : 0,
            y: isParent ? Double.random(in: -1000...1000) : 0,
            width: source.width,
            height: source.height,
            photos: [],
            is_deleted: false,
            location: nil,
            room: targetRoom,
            node_class: source.node_class,
            node_subtype: source.node_subtype,
            core_attributes: [],
            node_terminals: [],
            node_tasks: [],
            issues: [],
            ir_photos: [],
            ir_sessions: [],
            com: source.com,
            com_calculation: source.com_calculation,
            qr_code: nil,
            serviceability: source.serviceability,
            serviceability_note: source.serviceability_note,
            voltage: source.voltage,
            voltage_id: source.voltage_id,
            secondary_voltage: source.secondary_voltage,
            secondary_voltage_id: source.secondary_voltage_id,
            notes: source.notes,
            default_photo_id: nil,
            suggested_shortcut_id: source.suggested_shortcut_id,
            eqp_lib: source.eqp_lib
        )
        cloned.lastModifiedAt = Date()
        return cloned
    }

    /// Adds the bidirectional session relationship and enqueues (or directly POSTs) a
    /// nodeSession mapping create. Mirrors `QuickCountView.createSessionMapping`.
    private static func mapNodeToSession(
        _ node: NodeV2,
        session: IRSession,
        networkState: NetworkState
    ) async {
        session.nodes.append(node)
        node.ir_sessions.append(session)

        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.createNodeSessionMapping(
                    nodeId: node.id,
                    sessionId: session.id
                )
            } catch {
                networkState.enqueue(SyncOp(
                    target: .mappingNodeSession,
                    operation: .create,
                    mappingData: MappingData.nodeSession(
                        nodeId: node.id,
                        sessionId: session.id,
                        isDeleted: false
                    )
                ))
            }
        } else {
            networkState.enqueue(SyncOp(
                target: .mappingNodeSession,
                operation: .create,
                mappingData: MappingData.nodeSession(
                    nodeId: node.id,
                    sessionId: session.id,
                    isDeleted: false
                )
            ))
        }
    }
}
