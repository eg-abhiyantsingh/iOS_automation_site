//
//  WebAppContainerViewWithView.swift
//  Egalvanic PZ
//
//  Extended WebAppContainerView that supports SLD Views
//

import SwiftUI
import SwiftData
import WebKit

struct WebAppContainerViewWithView: View {
    var sld: SLDV2
    var selectedViewId: UUID?  // nil = "All Nodes" mode
    var sldLinks: [SLDLinkV2]
    var onBack: () -> Void
    var onNavigateToView: (UUID) -> Void

    init(sld: SLDV2, selectedViewId: UUID?, sldLinks: [SLDLinkV2], onBack: @escaping () -> Void, onNavigateToView: @escaping (UUID) -> Void) {
        AppLogger.log(.debug, "WebAppContainerViewWithView init - sld: \(sld.id), viewId: \(selectedViewId?.uuidString ?? "ALL NODES")", category: .webBridge)
        self.sld = sld
        self.selectedViewId = selectedViewId
        self.sldLinks = sldLinks
        self.onBack = onBack
        self.onNavigateToView = onNavigateToView
    }

    // Query nodes, edges, and view mappings
    @Query private var allNodes: [NodeV2]
    @Query private var allEdges: [EdgeV2]
    @Query private var nodeClasses: [NodeClass]
    @Query private var edgeClasses: [EdgeClass]
    @Query private var allOrientations: [NodeOrientation]
    @Query private var nodeViewMappings: [MappingNodeSLDView]
    @Query private var edgeViewMappings: [MappingEdgeSLDView]

    // Populate node_orientation relationships (SwiftData doesn't auto-load relationships)
    private var nodeClassesWithOrientations: [NodeClass] {
        let orientationsById = Dictionary(uniqueKeysWithValues: allOrientations.map { ($0.id, $0) })

        for nodeClass in nodeClasses {
            if let orientationId = nodeClass.node_orientation_id,
               let orientation = orientationsById[orientationId] {
                nodeClass.node_orientation = orientation
            }
        }

        return nodeClasses
    }

    // Filter nodes for this view based on mappings
    // When selectedViewId is nil ("All Nodes" mode), return all non-deleted nodes
    private var nodesInView: [NodeV2] {
        guard let viewId = selectedViewId else {
            // All Nodes mode: return all non-deleted nodes for this SLD
            let filtered = allNodes.filter { $0.sld?.id == sld.id && !$0.is_deleted }
            AppLogger.log(.debug, "nodesInView (ALL NODES mode): \(filtered.count) nodes", category: .webBridge)
            return filtered
        }

        // View-specific mode: existing logic
        let nodeIdsInView = Set(
            nodeViewMappings
                .filter { $0.sld_view_id == viewId && !$0.is_deleted }
                .map { $0.node_id }
        )
        let filtered = allNodes.filter { nodeIdsInView.contains($0.id) && !$0.is_deleted }
        AppLogger.log(.debug, "nodesInView: \(filtered.count) nodes (mappings: \(nodeViewMappings.count), nodeIds in view: \(nodeIdsInView.count))", category: .webBridge)
        return filtered
    }

    // Filter edges: include any edge where BOTH source and target nodes are in the view
    private var edgesInView: [EdgeV2] {
        let nodeIdsInView = Set(nodesInView.map { $0.id })
        let filtered = allEdges.filter { edge in
            !edge.is_deleted &&
            (edge.source.map { nodeIdsInView.contains($0) } ?? false) &&
            (edge.target.map { nodeIdsInView.contains($0) } ?? false)
        }
        AppLogger.log(.debug, "edgesInView: \(filtered.count) edges (nodes in view: \(nodeIdsInView.count))", category: .webBridge)
        return filtered
    }

    // Get node position overrides from view mappings
    // When selectedViewId is nil ("All Nodes" mode), return empty dict (use node's native x/y)
    private var nodePositionOverrides: [UUID: (x: Double, y: Double, width: Double?, height: Double?, isCollapsed: Bool)] {
        guard let viewId = selectedViewId else {
            // All Nodes mode: no position overrides, use node's native x/y
            return [:]
        }

        var overrides: [UUID: (x: Double, y: Double, width: Double?, height: Double?, isCollapsed: Bool)] = [:]
        for mapping in nodeViewMappings where mapping.sld_view_id == viewId && !mapping.is_deleted {
            if let x = mapping.x, let y = mapping.y {
                overrides[mapping.node_id] = (x: x, y: y, width: mapping.width, height: mapping.height, isCollapsed: mapping.is_collapsed)
            }
        }
        return overrides
    }

    // Get edge routing overrides from view mappings
    // When selectedViewId is nil ("All Nodes" mode), return empty dict (use edge's native points/algorithm)
    private var edgeRoutingOverrides: [UUID: (points: [EdgePoint]?, algorithm: String?)] {
        guard let viewId = selectedViewId else {
            // All Nodes mode: no routing overrides, use edge's native points/algorithm
            return [:]
        }

        var overrides: [UUID: (points: [EdgePoint]?, algorithm: String?)] = [:]
        for mapping in edgeViewMappings where mapping.sld_view_id == viewId && !mapping.is_deleted {
            overrides[mapping.edge_id] = (points: mapping.points, algorithm: mapping.algorithm)
        }
        return overrides
    }

    // Deletion alert controller state (nodes)
    @State private var showDeleteConfirmation = false
    @State private var pendingDeleteNode: NodeV2?

    // Deletion alert controller state (edges)
    @State private var showEdgeDeleteConfirmation = false
    @State private var showDuplicateConnectionAlert = false
    @State private var showSameNodeConnectionAlert = false
    @State private var pendingDeleteEdge: EdgeV2?

    // ZP-1042: Debounce API calls per node to prevent race conditions
    // When distribute children fires both nodePositionChanged and enclosureSizeUpdated
    // for the same node, we consolidate into a single API call with the final state.
    @State private var pendingNodeAPITasks: [UUID: Task<Void, Never>] = [:]

    // Environment for saving & syncing
    @Environment(\.modelContext) private var context
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Drive the fullScreenCover presentation with enum
    enum DetailSelection: Identifiable {
        case node(NodeV2)
        case edge(EdgeV2)

        var id: String {
            switch self {
            case .node(let node): return "node-\(node.id)"
            case .edge(let edge): return "edge-\(edge.id)"
            }
        }
    }

    @State private var detailSelection: DetailSelection? = nil

    var body: some View {
        let _ = AppLogger.log(.debug, "WebAppContainerViewWithView body evaluated - nodes: \(nodesInView.count), edges: \(edgesInView.count), nodeClasses: \(nodeClasses.count), orientations: \(allOrientations.count)", category: .webBridge)
        WebViewBridge(
            initialNodes: nodesInView,
            initialEdges: edgesInView,
            initialNodeClasses: nodeClassesWithOrientations.filter { !$0.is_deleted },
            initialEdgeClasses: edgeClasses.filter { !$0.is_deleted },
            activeViewId: selectedViewId,
            sldLinks: sldLinks,
            nodePositionOverrides: nodePositionOverrides,
            edgeRoutingOverrides: edgeRoutingOverrides,

            onNodeClicked: { id, _ in
                if let node = allNodes.first(where: { $0.id == id }) {
                    detailSelection = .node(node)
                }
            },

            onEdgeClicked: { id in
                if let edge = allEdges.first(where: { $0.id == id }) {
                    detailSelection = .edge(edge)
                }
            },

            onNodePositionChanged: { id, x, y in
                if selectedViewId == nil {
                    // All Nodes mode: update node's master position directly
                    updateNodePosition(nodeId: id, x: x, y: y)
                } else {
                    // View-specific mode: update the mapping, not the node
                    updateNodeViewMapping(nodeId: id, x: x, y: y)
                }
            },

            onNodeParentChanged: { id, newParent, relX, relY in
                guard let node = allNodes.first(where: { $0.id == id }) else {
                    AppLogger.log(.notice, "onNodeParentChanged: node not found \(id)", category: .webBridge)
                    return
                }
                node.parent_id = newParent
                // ZP-1956: when reparenting into a box, inherit its room so children
                // match the parent's location. On unlink (newParent == nil) or when the
                // new parent has no room, leave the child's existing room untouched.
                if let newParent, let parent = allNodes.first(where: { $0.id == newParent }) {
                    node.room = parent.room ?? node.room
                }
                if selectedViewId == nil {
                    // All Nodes mode: update node's master position directly
                    node.x = relX
                    node.y = relY
                } else {
                    // View-specific mode: update view mapping position
                    updateNodeViewMapping(nodeId: id, x: relX, y: relY)
                }
                persist(node)
            },

            onNodeCreated: { id, label, nodeType, parentId, x, y, nodeClassId, terminals in
                createNodeWithClass(
                    id: id,
                    label: label,
                    nodeType: nodeType,
                    parentId: parentId,
                    x: x,
                    y: y,
                    nodeClassId: nodeClassId,
                    terminals: terminals
                )
            },

            onEnclosureCreated: { id, label, nodeType, x, y, width, height, nodeClassId, terminals in
                createEnclosureWithClass(
                    id: id,
                    label: label,
                    nodeType: nodeType,
                    x: x,
                    y: y,
                    width: width,
                    height: height,
                    nodeClassId: nodeClassId,
                    terminals: terminals
                )
            },

            onEnclosureSizeUpdated: { id, width, height in
                if selectedViewId == nil {
                    // All Nodes mode: update node's master size directly
                    updateNodeSize(nodeId: id, width: width, height: height)
                } else {
                    // View-specific mode: update view mapping size
                    updateNodeViewMappingSize(nodeId: id, width: width, height: height)
                }
            },

            onNodeRemovedFromParent: { id, x, y in
                guard let node = allNodes.first(where: { $0.id == id }) else { return }
                node.parent_id = nil
                if selectedViewId == nil {
                    // All Nodes mode: update node's master position directly
                    node.x = x
                    node.y = y
                } else {
                    // View-specific mode: update view mapping position
                    updateNodeViewMapping(nodeId: id, x: x, y: y)
                }
                persist(node)
            },

            onNodeDeleted: { id in
                guard let node = allNodes.first(where: { $0.id == id }) else { return }
                pendingDeleteNode = node
                showDeleteConfirmation = true
            },

            onNodeTypeChanged: { id, newType, width, height in
                guard let node = allNodes.first(where: { $0.id == id }) else { return }
                node.type = newType
                if let w = width { node.width = w }
                if let h = height { node.height = h }
                persist(node, forceTypeChange: true)
            },

            onEdgeCreated: { id, source, target, sourceTerminalId, targetTerminalId, points, algorithm in
                // Prevent same-node connections
                if source == target {
                    showSameNodeConnectionAlert = true
                    return
                }

                // Prevent duplicate connections
                let isDuplicate = allEdges.contains { edge in
                    !edge.is_deleted &&
                    edge.sld?.id == sld.id &&
                    edge.source == source &&
                    edge.target == target
                }
                if isDuplicate {
                    showDuplicateConnectionAlert = true
                    return
                }

                // Terminal UUIDs are authoritative identifiers for edge connections
                let e = EdgeV2(
                    id: id,
                    source: source,
                    target: target,
                    sld: sld,
                    is_deleted: false,
                    sourceNodeTerminalId: sourceTerminalId,
                    targetNodeTerminalId: targetTerminalId,
                    points: points,
                    algorithm: algorithm
                )
                createEdge(e)
            },

            onEdgeDeleted: { id in
                guard let edge = allEdges.first(where: { $0.id == id }) else { return }
                pendingDeleteEdge = edge
                showEdgeDeleteConfirmation = true
            },

            onEdgeUpdated: { id, points, algorithm in
                if selectedViewId == nil {
                    // All Nodes mode: update edge's master routing directly
                    updateEdgeRouting(edgeId: id, points: points, algorithm: algorithm)
                } else {
                    // View-specific mode: update edge view mapping
                    updateEdgeViewMapping(edgeId: id, points: points, algorithm: algorithm)
                }
            },

            onViewBack: {
                onBack()
            },

            onNavigateToView: { targetViewId in
                onNavigateToView(targetViewId)
            },

            onNodeCollapseStateChanged: { nodeId, isCollapsed in
                // Update is_collapsed in the view mapping
                updateNodeViewMappingCollapseState(nodeId: nodeId, isCollapsed: isCollapsed)
            }
        )
        .fullScreenCover(item: $detailSelection, onDismiss: {
            // Fallback: ensure graph is updated with view-filtered nodes after dismiss
            // This handles cases like Cancel after creating child nodes
            updateWebViewGraph()
        }) { selection in
            switch selection {
            case .node(let node):
                EditNodeDetailViewV3(
                    node: node,
                    sld: sld,
                    onSaveCompleted: {
                        // Update graph BEFORE dismiss animation starts to prevent flash
                        // This ensures view-filtered nodes are displayed immediately
                        updateWebViewGraph()
                    },
                    activeViewId: selectedViewId
                )
                    .environmentObject(networkState)
                    .environmentObject(appState)
            case .edge(let edge):
                EditEdgeDetailViewV3(edge: edge)
                    .environmentObject(networkState)
                    .environmentObject(appState)
            }
        }
        .alert(AppStrings.SLD.confirmDelete,
               isPresented: $showDeleteConfirmation,
               presenting: pendingDeleteNode) { node in
            Button(AppStrings.Common.cancel, role: .cancel) {
                pendingDeleteNode = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let nodeToDelete = pendingDeleteNode {
                    nodeToDelete.is_deleted = true
                    persist(nodeToDelete)
                }
                pendingDeleteNode = nil
            }
        } message: { node in
            Text(AppStrings.SLD.deleteNodeConfirm(node.label))
        }
        .alert(AppStrings.SLD.confirmDelete,
               isPresented: $showEdgeDeleteConfirmation,
               presenting: pendingDeleteEdge) { edge in
            Button(AppStrings.Common.cancel, role: .cancel) {
                pendingDeleteEdge = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let edgeToDelete = pendingDeleteEdge {
                    edgeToDelete.is_deleted = true
                    persistEdge(edgeToDelete)
                }
                pendingDeleteEdge = nil
            }
        } message: { edge in
            Text(AppStrings.SLD.deleteConnectionConfirm)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showDuplicateConnectionAlert) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(AppStrings.AssetsExtra.duplicateConnectionExists)
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showSameNodeConnectionAlert) {
            Button(AppStrings.Common.ok, role: .cancel) { }
        } message: {
            Text(AppStrings.AssetsExtra.sameNodeValidation)
        }
        .environmentObject(networkState)
        .onDisappear {
            // ZP-1042: Flush pending debounced API calls immediately when leaving the view
            // so the backend has the latest data before any refresh can occur
            flushPendingNodeSyncs()
        }
    }

    // MARK: - All Nodes Mode Updates (Master Position/Size/Routing)

    /// ZP-1042: Debounced API sync for a node. Cancels any pending API call for the same node
    /// and schedules a new one after a short delay, ensuring only the final state is sent.
    /// This prevents race conditions when distribute children fires both position and size
    /// updates for the same node nearly simultaneously.
    private func scheduleDebouncedNodeSync(node: NodeV2) {
        let nodeId = node.id

        // Cancel any in-flight debounce for this node
        pendingNodeAPITasks[nodeId]?.cancel()

        if networkState.mode == .online {
            // Pre-build API payload to avoid holding @Model reference across await points in Task
            guard let payload = try? APIClient.shared.buildNodeUpdatePayload(node) else { return }

            pendingNodeAPITasks[nodeId] = Task {
                // Wait briefly to let any other updates for the same node arrive
                try? await Task.sleep(nanoseconds: 150_000_000) // 150ms

                guard !Task.isCancelled else { return }

                do {
                    _ = try await APIClient.shared.sendNodeUpdate(nodeId: nodeId, payload: payload)
                } catch {
                    guard !Task.isCancelled else { return }
                    AppLogger.log(.error, "Debounced API sync failed for nodeId=\(nodeId): \(error)", category: .webBridge)
                    if let freshNode = allNodes.first(where: { $0.id == nodeId }) {
                        let op = SyncOp(
                            target: .node,
                            operation: .update,
                            node: freshNode,
                            edge: nil,
                            photo: nil
                        )
                        networkState.enqueue(op)
                    }
                }

                pendingNodeAPITasks.removeValue(forKey: nodeId)
            }
        } else {
            let op = SyncOp(
                target: .node,
                operation: .update,
                node: node,
                edge: nil,
                photo: nil
            )
            networkState.enqueue(op)
        }
    }

    /// ZP-1042: Immediately flush all pending debounced API calls.
    /// Called when navigating away so the backend receives updates before any refresh.
    private func flushPendingNodeSyncs() {
        guard !pendingNodeAPITasks.isEmpty else { return }

        for (nodeId, task) in pendingNodeAPITasks {
            task.cancel()

            if let node = allNodes.first(where: { $0.id == nodeId }) {
                // Pre-build API payload to avoid holding @Model reference in async Task
                guard let payload = try? APIClient.shared.buildNodeUpdatePayload(node) else { continue }
                Task {
                    do {
                        _ = try await APIClient.shared.sendNodeUpdate(nodeId: nodeId, payload: payload)
                    } catch {
                        AppLogger.log(.error, "Flush sync failed for nodeId=\(nodeId): \(error)", category: .webBridge)
                        if let freshNode = allNodes.first(where: { $0.id == nodeId }) {
                            let op = SyncOp(
                                target: .node,
                                operation: .update,
                                node: freshNode,
                                edge: nil,
                                photo: nil
                            )
                            networkState.enqueue(op)
                        }
                    }
                }
            }
        }
        pendingNodeAPITasks.removeAll()
    }

    /// Updates a node's master position directly (used in "All Nodes" mode)
    private func updateNodePosition(nodeId: UUID, x: Double, y: Double) {
        guard let node = allNodes.first(where: { $0.id == nodeId }) else {
            AppLogger.log(.notice, "updateNodePosition: node not found \(nodeId)", category: .webBridge)
            return
        }

        node.x = x
        node.y = y

        do {
            try context.save()
        } catch {
            AppLogger.log(.error, "Failed to save node position: \(error)", category: .webBridge)
        }

        scheduleDebouncedNodeSync(node: node)
    }

    /// Updates a node's master size directly (used in "All Nodes" mode for enclosures)
    private func updateNodeSize(nodeId: UUID, width: Double, height: Double) {
        guard let node = allNodes.first(where: { $0.id == nodeId }) else {
            AppLogger.log(.notice, "updateNodeSize: node not found \(nodeId)", category: .webBridge)
            return
        }

        AppLogger.log(.debug, "updateNodeSize: nodeId=\(nodeId), width=\(width), height=\(height)", category: .webBridge)
        node.width = width
        node.height = height

        do {
            try context.save()
        } catch {
            AppLogger.log(.error, "Failed to save node size: \(error)", category: .webBridge)
        }

        scheduleDebouncedNodeSync(node: node)
    }

    /// Updates an edge's master routing directly (used in "All Nodes" mode)
    private func updateEdgeRouting(edgeId: UUID, points: [EdgePoint]?, algorithm: String?) {
        guard let edge = allEdges.first(where: { $0.id == edgeId }) else {
            AppLogger.log(.notice, "updateEdgeRouting: edge not found \(edgeId)", category: .webBridge)
            return
        }

        AppLogger.log(.debug, "updateEdgeRouting: edgeId=\(edgeId), pointsCount=\(points?.count ?? 0), algorithm=\(algorithm ?? "nil")", category: .webBridge)
        edge.points = points
        edge.algorithm = algorithm

        do {
            try context.save()
        } catch {
            AppLogger.log(.error, "Failed to save edge routing: \(error)", category: .webBridge)
        }

        // Sync to backend — pre-build payload to avoid holding @Model in async Task
        if networkState.mode == .online {
            if let payload = try? APIClient.shared.buildEdgeUpdatePayload(edge) {
                Task {
                    do {
                        _ = try await APIClient.shared.sendEdgeUpdate(edgeId: edgeId, payload: payload)
                        AppLogger.log(.debug, "Edge routing update succeeded", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "Edge routing update failed: \(error)", category: .webBridge)
                        if let freshEdge = allEdges.first(where: { $0.id == edgeId }) {
                            let op = SyncOp(
                                target: .edge,
                                operation: .update,
                                node: nil,
                                edge: freshEdge,
                                photo: nil
                            )
                            networkState.enqueue(op)
                        }
                    }
                }
            }
        } else {
            let op = SyncOp(
                target: .edge,
                operation: .update,
                node: nil,
                edge: edge,
                photo: nil
            )
            networkState.enqueue(op)
        }
    }

    // MARK: - View Mapping Updates

    private func updateNodeViewMapping(nodeId: UUID, x: Double, y: Double) {
        // This function should only be called in view-specific mode
        guard let viewId = selectedViewId else {
            AppLogger.log(.notice, "updateNodeViewMapping called in All Nodes mode - should not happen", category: .webBridge)
            return
        }

        // Debug logging to diagnose mapping lookup issues
        let matchingMappings = nodeViewMappings.filter { $0.node_id == nodeId && $0.sld_view_id == viewId }
        AppLogger.log(.debug, "updateNodeViewMapping: nodeId=\(nodeId), viewId=\(viewId), x=\(x), y=\(y), totalMappings=\(nodeViewMappings.count), matching=\(matchingMappings.count), mode=\(networkState.mode)", category: .webBridge)

        // Find existing non-deleted mapping or create new one
        if let existingMapping = nodeViewMappings.first(where: {
            $0.node_id == nodeId && $0.sld_view_id == viewId && !$0.is_deleted
        }) {
            AppLogger.log(.debug, "Found existing mapping: id=\(existingMapping.id)", category: .webBridge)
            existingMapping.x = x
            existingMapping.y = y
            existingMapping.modified_at = Date()
            persistViewMapping()

            // Sync mapping to backend
            if networkState.mode == .online {
                Task {
                    do {
                        let response = try await APIClient.shared.updateNodePositionInView(
                            viewId: viewId,
                            nodeId: nodeId,
                            x: x,
                            y: y
                        )
                        AppLogger.log(.debug, "Position update succeeded: \(response)", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "Position update failed: \(error)", category: .webBridge)
                        // If API call fails (e.g., 404), fall back to offline sync queue
                        networkState.enqueueNodeSLDViewUpdate(
                            mappingId: existingMapping.id,
                            nodeId: nodeId,
                            viewId: viewId,
                            x: x,
                            y: y,
                            width: existingMapping.width,
                            height: existingMapping.height,
                            isCollapsed: existingMapping.is_collapsed
                        )
                    }
                }
            } else {
                networkState.enqueueNodeSLDViewUpdate(
                    mappingId: existingMapping.id,
                    nodeId: nodeId,
                    viewId: viewId,
                    x: x,
                    y: y,
                    width: existingMapping.width,
                    height: existingMapping.height,
                    isCollapsed: existingMapping.is_collapsed
                )
            }
        } else {
            // Create new mapping
            AppLogger.log(.debug, "No existing mapping found, creating new one", category: .webBridge)
            let mappingId = UUID()
            let mapping = MappingNodeSLDView(
                id: mappingId,
                node_id: nodeId,
                sld_view_id: viewId,
                x: x,
                y: y,
                width: nil,
                height: nil,
                is_collapsed: false,
                is_deleted: false,
                created_at: Date(),
                modified_at: Date()
            )
            context.insert(mapping)
            persistViewMapping()

            // Sync mapping creation to backend
            if networkState.mode == .online {
                Task {
                    do {
                        let response = try await APIClient.shared.addNodeToView(
                            viewId: viewId,
                            nodeId: nodeId,
                            x: x,
                            y: y,
                            width: nil,
                            height: nil
                        )
                        AppLogger.log(.debug, "Add node to view succeeded: \(response)", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "Add node to view failed: \(error)", category: .webBridge)
                        // Fall back to offline sync queue
                        networkState.enqueueNodeSLDViewCreate(
                            mappingId: mappingId,
                            nodeId: nodeId,
                            viewId: viewId,
                            x: x,
                            y: y,
                            width: nil,
                            height: nil
                        )
                    }
                }
            } else {
                networkState.enqueueNodeSLDViewCreate(
                    mappingId: mappingId,
                    nodeId: nodeId,
                    viewId: viewId,
                    x: x,
                    y: y,
                    width: nil,
                    height: nil
                )
            }
        }
    }

    private func updateNodeViewMappingSize(nodeId: UUID, width: Double, height: Double) {
        guard let viewId = selectedViewId else {
            AppLogger.log(.notice, "updateNodeViewMappingSize called in All Nodes mode - should not happen", category: .webBridge)
            return
        }

        AppLogger.log(.debug, "updateNodeViewMappingSize: nodeId=\(nodeId), width=\(width), height=\(height)", category: .webBridge)

        if let existingMapping = nodeViewMappings.first(where: {
            $0.node_id == nodeId && $0.sld_view_id == viewId && !$0.is_deleted
        }) {
            AppLogger.log(.debug, "Found existing mapping for size update: id=\(existingMapping.id)", category: .webBridge)
            existingMapping.width = width
            existingMapping.height = height
            existingMapping.modified_at = Date()
            persistViewMapping()

            // Sync size update to backend (using position endpoint which updates all fields)
            if networkState.mode == .online {
                Task {
                    do {
                        let response = try await APIClient.shared.updateNodePositionInView(
                            viewId: viewId,
                            nodeId: nodeId,
                            x: existingMapping.x ?? 0,
                            y: existingMapping.y ?? 0,
                            width: width,
                            height: height
                        )
                        AppLogger.log(.debug, "Size update succeeded: \(response)", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "Size update failed: \(error)", category: .webBridge)
                        // Fall back to offline sync queue
                        networkState.enqueueNodeSLDViewUpdate(
                            mappingId: existingMapping.id,
                            nodeId: nodeId,
                            viewId: viewId,
                            x: existingMapping.x ?? 0,
                            y: existingMapping.y ?? 0,
                            width: width,
                            height: height,
                            isCollapsed: existingMapping.is_collapsed
                        )
                    }
                }
            } else {
                networkState.enqueueNodeSLDViewUpdate(
                    mappingId: existingMapping.id,
                    nodeId: nodeId,
                    viewId: viewId,
                    x: existingMapping.x ?? 0,
                    y: existingMapping.y ?? 0,
                    width: width,
                    height: height,
                    isCollapsed: existingMapping.is_collapsed
                )
            }
        } else {
            AppLogger.log(.notice, "No existing mapping found for size update", category: .webBridge)
        }
    }

    private func updateNodeViewMappingCollapseState(nodeId: UUID, isCollapsed: Bool) {
        guard let viewId = selectedViewId else {
            AppLogger.log(.notice, "updateNodeViewMappingCollapseState called in All Nodes mode - should not happen", category: .webBridge)
            return
        }

        // Find ALL matching non-deleted mappings (there may be duplicates)
        let matchingMappings = nodeViewMappings.filter {
            $0.node_id == nodeId && $0.sld_view_id == viewId && !$0.is_deleted
        }

        guard !matchingMappings.isEmpty else {
            return
        }

        // Update ALL matching mappings to ensure consistency
        for mapping in matchingMappings {
            mapping.is_collapsed = isCollapsed
            mapping.modified_at = Date()
        }

        persistViewMapping()

        // Use the first mapping for sync (they should all have same node/view)
        let primaryMapping = matchingMappings[0]

        // Sync collapse state to backend
        if networkState.mode == .online {
            Task {
                do {
                    _ = try await APIClient.shared.updateNodeCollapseStateInView(
                        viewId: viewId,
                        nodeId: nodeId,
                        isCollapsed: isCollapsed
                    )
                } catch {
                    // Fall back to offline sync queue
                    networkState.enqueueNodeSLDViewUpdate(
                        mappingId: primaryMapping.id,
                        nodeId: nodeId,
                        viewId: viewId,
                        x: primaryMapping.x ?? 0,
                        y: primaryMapping.y ?? 0,
                        width: primaryMapping.width,
                        height: primaryMapping.height,
                        isCollapsed: isCollapsed
                    )
                }
            }
        } else {
            networkState.enqueueNodeSLDViewUpdate(
                mappingId: primaryMapping.id,
                nodeId: nodeId,
                viewId: viewId,
                x: primaryMapping.x ?? 0,
                y: primaryMapping.y ?? 0,
                width: primaryMapping.width,
                height: primaryMapping.height,
                isCollapsed: isCollapsed
            )
        }
    }

    private func updateEdgeViewMapping(edgeId: UUID, points: [EdgePoint]?, algorithm: String?) {
        guard let viewId = selectedViewId else {
            AppLogger.log(.notice, "updateEdgeViewMapping called in All Nodes mode - should not happen", category: .webBridge)
            return
        }

        AppLogger.log(.debug, "updateEdgeViewMapping: edgeId=\(edgeId), pointsCount=\(points?.count ?? 0)", category: .webBridge)

        // Convert EdgePoint array to dictionary format for API
        let pointsAsDict: [[String: Double]]? = points?.map { ["x": $0.x, "y": $0.y] }

        // Check if there's actually custom routing data worth storing
        let hasCustomRouting = (points != nil && !points!.isEmpty) || (algorithm != nil && !algorithm!.isEmpty)

        if let existingMapping = edgeViewMappings.first(where: {
            $0.edge_id == edgeId && $0.sld_view_id == viewId
        }) {
            AppLogger.log(.debug, "Found existing edge mapping: id=\(existingMapping.id)", category: .webBridge)
            existingMapping.points = points
            existingMapping.algorithm = algorithm
            persistViewMapping()

            // Sync edge view mapping to backend
            if networkState.mode == .online {
                Task {
                    do {
                        let response = try await APIClient.shared.updateEdgePointsInView(
                            viewId: viewId,
                            edgeId: edgeId,
                            points: pointsAsDict,
                            algorithm: algorithm
                        )
                        AppLogger.log(.debug, "Edge points update succeeded: \(response)", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "Edge points update failed: \(error)", category: .webBridge)
                        // Fall back to offline sync queue
                        networkState.enqueueEdgeSLDViewUpdate(
                            mappingId: existingMapping.id,
                            edgeId: edgeId,
                            viewId: viewId,
                            points: pointsAsDict,
                            algorithm: algorithm
                        )
                    }
                }
            } else {
                networkState.enqueueEdgeSLDViewUpdate(
                    mappingId: existingMapping.id,
                    edgeId: edgeId,
                    viewId: viewId,
                    points: pointsAsDict,
                    algorithm: algorithm
                )
            }
        } else if hasCustomRouting {
            // Only create a new mapping if there's actual custom routing data
            // Edge-view mappings are ONLY for storing custom breakpoints/routing overrides
            AppLogger.log(.debug, "No existing edge mapping found, creating new one with custom routing", category: .webBridge)
            let mappingId = UUID()
            let mapping = MappingEdgeSLDView(
                id: mappingId,
                edge_id: edgeId,
                sld_view_id: viewId,
                points: points,
                algorithm: algorithm,
                is_deleted: false
            )
            context.insert(mapping)
            persistViewMapping()

            // Sync new edge view mapping to backend
            if networkState.mode == .online {
                Task {
                    do {
                        let response = try await APIClient.shared.updateEdgePointsInView(
                            viewId: viewId,
                            edgeId: edgeId,
                            points: pointsAsDict,
                            algorithm: algorithm
                        )
                        AppLogger.log(.debug, "New edge mapping sync succeeded: \(response)", category: .webBridge)
                    } catch {
                        AppLogger.log(.error, "New edge mapping sync failed: \(error)", category: .webBridge)
                        // Fall back to offline sync queue
                        networkState.enqueueEdgeSLDViewUpdate(
                            mappingId: mappingId,
                            edgeId: edgeId,
                            viewId: viewId,
                            points: pointsAsDict,
                            algorithm: algorithm
                        )
                    }
                }
            } else {
                networkState.enqueueEdgeSLDViewUpdate(
                    mappingId: mappingId,
                    edgeId: edgeId,
                    viewId: viewId,
                    points: pointsAsDict,
                    algorithm: algorithm
                )
            }
        } else {
            // No existing mapping and no custom routing data - nothing to do
            AppLogger.log(.debug, "No existing edge mapping and no custom routing data - skipping", category: .webBridge)
        }
    }

    private func persistViewMapping() {
        do {
            try context.save()
        } catch {
            AppLogger.log(.error, "Failed to save view mapping: \(error)", category: .webBridge)
        }
    }

    // MARK: - Node Methods

    private func persist(_ node: NodeV2, forceTypeChange: Bool = false) {
        do {
            try context.save()
            updateWebViewGraph()
        }
        catch { AppLogger.log(.error, "SwiftData save failed: \(error)", category: .webBridge) }

        if networkState.mode == .online {
            let extraData: [String: Any] = forceTypeChange ? ["force_type_change": true] : [:]
            // Pre-build API payload to avoid holding @Model reference in async Task
            if let payload = try? APIClient.shared.buildNodeUpdatePayload(node, extraData: extraData) {
                let nodeId = node.id
                Task { _ = try? await APIClient.shared.sendNodeUpdate(nodeId: nodeId, payload: payload) }
            }
        } else {
            let extraData: [String: Any]? = forceTypeChange ? ["force_type_change": true] : nil
            let op = SyncOp(
                target: .node,
                operation: .update,
                node: node,
                edge: nil,
                photo: nil,
                extraData: extraData
            )
            networkState.enqueue(op)
        }
    }

    private func create(_ node: NodeV2) {
        let viewId = selectedViewId  // May be nil in "All Nodes" mode
        let mappingId = UUID()

        do {
            // Mark node as needing sync - required for offline sync to work
            node.needsSync = true

            context.insert(node)
            if let sld = node.sld {
                if !sld.nodes.contains(where: { $0.id == node.id }) {
                    sld.nodes.append(node)
                }
            }
            try context.save()

            // Create view mapping for new node (only in view-specific mode)
            if let viewId = viewId {
                let mapping = MappingNodeSLDView(
                    id: mappingId,
                    node_id: node.id,
                    sld_view_id: viewId,
                    x: node.x,
                    y: node.y,
                    width: node.width,
                    height: node.height,
                    is_collapsed: false,
                    is_deleted: false,
                    created_at: Date(),
                    modified_at: Date()
                )
                context.insert(mapping)
                try context.save()
            }
        }
        catch { AppLogger.log(.error, "SwiftData save failed: \(error)", category: .webBridge) }

        if networkState.mode == .online {
            Task {
                // Create node first
                _ = try? await APIClient.shared.createNode(node: node)
                // Then add node to the view (only in view-specific mode)
                if let viewId = viewId {
                    _ = try? await APIClient.shared.addNodeToView(
                        viewId: viewId,
                        nodeId: node.id,
                        x: node.x,
                        y: node.y,
                        width: node.width,
                        height: node.height
                    )
                }
            }
        } else {
            // Queue node creation
            let op = SyncOp(
                target: .node,
                operation: .create,
                node: node,
                edge: nil,
                photo: nil
            )
            networkState.enqueue(op)

            // Queue mapping creation (only in view-specific mode)
            if let viewId = viewId {
                networkState.enqueueNodeSLDViewCreate(
                    mappingId: mappingId,
                    nodeId: node.id,
                    viewId: viewId,
                    x: node.x,
                    y: node.y,
                    width: node.width,
                    height: node.height
                )
            }
        }
    }

    // MARK: - Edge Methods

    private func createEdge(_ edge: EdgeV2) {
        do {
            // Mark edge as needing sync - required for offline sync to work
            edge.needsSync = true

            context.insert(edge)
            try context.save()

            // NOTE: We do NOT create a MappingEdgeSLDView here.
            // Edges automatically appear in views when both connected nodes are in the view.
            // MappingEdgeSLDView is ONLY needed when the edge has custom breakpoints/routing
            // overrides for a specific view. If the user later adds custom routing, the
            // updateEdgeViewMapping() function will create the mapping at that time.
        }
        catch { AppLogger.log(.error, "SwiftData save failed: \(error)", category: .webBridge) }

        if networkState.mode == .online {
            Task {
                // Create edge - no view mapping needed, edges appear based on node presence
                _ = try? await APIClient.shared.createEdge(edge: edge)
            }
        } else {
            // Queue edge creation
            let op = SyncOp(
                target: .edge,
                operation: .create,
                node: nil,
                edge: edge,
                photo: nil
            )
            networkState.enqueue(op)
        }
    }

    private func persistEdge(_ edge: EdgeV2) {
        AppLogger.log(.debug, "persistEdge called - edge_id=\(edge.id), is_deleted=\(edge.is_deleted), mode=\(networkState.mode)", category: .webBridge)

        do {
            try context.save()
            updateWebViewGraph()
        }
        catch { AppLogger.log(.error, "SwiftData save failed: \(error)", category: .webBridge) }

        if networkState.mode == .online {
            // Pre-build API payload to avoid holding @Model reference in async Task
            if let payload = try? APIClient.shared.buildEdgeUpdatePayload(edge) {
                let edgeId = edge.id
                Task { _ = try? await APIClient.shared.sendEdgeUpdate(edgeId: edgeId, payload: payload) }
            }
        } else {
            AppLogger.log(.debug, "Queueing UPDATE operation for edge \(edge.id), is_deleted=\(edge.is_deleted)", category: .webBridge)
            let op = SyncOp(
                target: .edge,
                operation: .update,
                node: nil,
                edge: edge,
                photo: nil
            )
            networkState.enqueue(op)
        }
    }

    private func updateWebViewGraph() {
        let dto = SLDService.shared.createDTO(
            forSLDId: sld.id,
            nodes: nodesInView,
            edges: edgesInView,
            customName: "updated-bridge-view",
            nodePositionOverrides: nodePositionOverrides,
            edgeRoutingOverrides: edgeRoutingOverrides
        )

        WebViewBridge.updateGraph(with: dto, animated: true)
    }
}

extension WebAppContainerViewWithView {
    // MARK: - Node Creation Helpers

    private func createNodeWithClass(
        id: UUID,
        label: String,
        nodeType: String,
        parentId: UUID?,
        x: Double,
        y: Double,
        nodeClassId: UUID?,
        terminals: [(id: UUID, orientationTerminalId: UUID)]
    ) {
        let nodeClass = findOrCreateNodeClass(nodeClassId)

        let node = NodeV2(
            id: id,
            label: label,
            type: nodeType,
            sld: sld,
            parent_id: parentId,
            x: x,
            y: y,
            width: nodeClass.width,
            height: nodeClass.height,
            photos: [],
            is_deleted: false,
            location: nil,
            node_class: nodeClass,
            core_attributes: []
        )

        // Set default COM and serviceability values
        node.com = 1
        node.serviceability = Serviceability.serviceable.rawValue

        // ZP-1956: when the new node is dropped into a box, inherit the box's room
        // so children always match the parent's location.
        if let parentId, let parent = allNodes.first(where: { $0.id == parentId }) {
            node.room = parent.room
        }

        // Mark node as needing sync - required for offline sync to work
        node.needsSync = true

        context.insert(node)

        // Append to SLD's nodes array to trigger SwiftUI observation
        if let sld = node.sld {
            if !sld.nodes.contains(where: { $0.id == node.id }) {
                sld.nodes.append(node)
            }
        }

        // Create NodeTerminal entities with frontend-provided UUIDs
        // Copy rendering properties from orientation terminal blueprints
        for terminalData in terminals {
            // Find the blueprint to copy rendering properties
            if let blueprint = allOrientations
                .flatMap({ $0.orientation_terminals })
                .first(where: { $0.id == terminalData.orientationTerminalId }) {

                let terminal = NodeTerminal(
                    id: terminalData.id,
                    node: node,
                    node_orientation_terminal_id: terminalData.orientationTerminalId,
                    handle_code: blueprint.handle_code,
                    label: blueprint.label,
                    side: blueprint.side,
                    position: blueprint.position,
                    offset_percent: blueprint.offset_percent,
                    color: blueprint.color,
                    show_label: blueprint.show_label
                )
                context.insert(terminal)
                node.node_terminals.append(terminal)
            } else {
                AppLogger.log(.notice, "Could not find orientation terminal blueprint for \(terminalData.orientationTerminalId)", category: .webBridge)
            }
        }

        try? context.save()

        // Create node in backend FIRST, then add to view (only in view-specific mode)
        if networkState.mode == .online {
            Task {
                do {
                    // Create node first
                    _ = try await APIClient.shared.createNode(node: node)
                    // Then create view mapping (only in view-specific mode)
                    if selectedViewId != nil {
                        updateNodeViewMapping(nodeId: id, x: x, y: y)
                    }
                } catch {
                    AppLogger.log(.error, "Failed to create node in backend: \(error)", category: .webBridge)
                    // Queue for offline sync
                    let op = SyncOp(target: .node, operation: .create, node: node)
                    networkState.enqueue(op)
                }
            }
        } else {
            // Queue node creation for offline sync
            let op = SyncOp(target: .node, operation: .create, node: node)
            networkState.enqueue(op)
            // Create local view mapping (only in view-specific mode)
            if selectedViewId != nil {
                updateNodeViewMapping(nodeId: id, x: x, y: y)
            }
        }
    }

    private func createEnclosureWithClass(
        id: UUID,
        label: String,
        nodeType: String,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        nodeClassId: UUID?,
        terminals: [(id: UUID, orientationTerminalId: UUID)]
    ) {
        let nodeClass = findOrCreateNodeClass(nodeClassId)

        let node = NodeV2(
            id: id,
            label: label,
            type: nodeType,
            sld: sld,
            parent_id: nil,
            x: x,
            y: y,
            width: width,
            height: height,
            photos: [],
            is_deleted: false,
            location: nil,
            node_class: nodeClass,
            core_attributes: []
        )

        // Set default COM and serviceability values
        node.com = 1
        node.serviceability = Serviceability.serviceable.rawValue

        create(node)

        // Create terminal entities from frontend-provided UUIDs
        // Copy rendering properties from orientation terminal blueprints
        for terminalData in terminals {
            // Find the blueprint to copy rendering properties
            if let blueprint = allOrientations
                .flatMap({ $0.orientation_terminals })
                .first(where: { $0.id == terminalData.orientationTerminalId }) {

                let terminal = NodeTerminal(
                    id: terminalData.id,
                    node: node,
                    node_orientation_terminal_id: terminalData.orientationTerminalId,
                    handle_code: blueprint.handle_code,
                    label: blueprint.label,
                    side: blueprint.side,
                    position: blueprint.position,
                    offset_percent: blueprint.offset_percent,
                    color: blueprint.color,
                    show_label: blueprint.show_label
                )
                context.insert(terminal)
                node.node_terminals.append(terminal)
            } else {
                AppLogger.log(.notice, "Could not find orientation terminal blueprint for \(terminalData.orientationTerminalId)", category: .webBridge)
            }
        }

        // Save terminals along with the node
        try? context.save()

        // Create view mapping for the new enclosure (only in view-specific mode)
        if selectedViewId != nil {
            updateNodeViewMapping(nodeId: id, x: x, y: y)
            updateNodeViewMappingSize(nodeId: id, width: width, height: height)
        }
    }

    private func findOrCreateNodeClass(_ nodeClassId: UUID?) -> NodeClass {
        if let classId = nodeClassId,
           let existingClass = nodeClasses.first(where: { $0.id == classId }) {
            return existingClass
        }

        let defaultClass = NodeClass(
            id: UUID(),
            name: "Default Class",
            style: "default",
            box: false,
            definition: [],
            ocp: false,
            width: 150,
            height: 60,
            color: "b0bec5"
        )
        context.insert(defaultClass)
        return defaultClass
    }
}
