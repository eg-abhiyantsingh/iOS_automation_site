//
//  LinkExistingNodeViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData
import Combine

// MARK: - Filter engine (dedicated actor — no GCD thread contention)

private actor NodeFilterEngine {
    typealias NodeItem = LinkExistingNodeViewModel.NodeItem

    func filter(nodes: [NodeItem], query: String, roomId: UUID?) -> [NodeItem] {
        Array(nodes.lazy.filter { item in
            (roomId == nil || item.roomId == roomId) &&
            (query.isEmpty || item.matches(query))
        })
    }
}

final class LinkExistingNodeViewModel: ObservableObject {

    // MARK: - Lightweight display item (avoids repeated SwiftData property access)

    struct NodeItem: Identifiable, Sendable {
        let id: UUID
        let label: String
        let className: String?
        let classStyle: String?
        let roomId: UUID?
        let roomName: String?
        private let searchableText: String

        init(node: NodeV2) {
            self.id = node.id
            self.label = node.label
            self.className = node.node_class?.name
            self.classStyle = node.node_class?.style
            self.roomId = node.room?.id
            self.roomName = node.room?.name
            self.searchableText = (node.label + " " + (node.node_class?.name ?? "")).lowercased()
        }

        func matches(_ query: String) -> Bool {
            searchableText.contains(query)
        }
    }

    // MARK: - Published state

    @Published var searchText = ""
    @Published var filterRoom: Room?
    @Published var selectedNodeIds: Set<UUID> = []
    @Published private(set) var filteredNodes: [NodeItem] = []
    @Published private(set) var isLinking = false
    @Published private(set) var isLoading = true
    @Published var showingLocationPicker = false

    // MARK: - Read-only

    let sld: SLDV2
    let parent: NodeV2

    var hasEligibleNodes: Bool {
        !isLoading && !allEligibleNodes.isEmpty
    }

    var isAllFilteredSelected: Bool {
        !cachedFilteredIds.isEmpty && cachedFilteredIds.isSubset(of: selectedNodeIds)
    }

    // MARK: - Private

    private var allEligibleNodes: [NodeItem] = []
    private var nodeLookup: [UUID: NodeV2] = [:]
    private var cachedFilteredIds: Set<UUID> = []
    private let filterEngine = NodeFilterEngine()
    private var debounceTask: Task<Void, Never>?

    // MARK: - Lightweight init (no SwiftData faulting here)

    init(sld: SLDV2, parent: NodeV2) {
        self.sld = sld
        self.parent = parent
    }

    // MARK: - Deferred loading (called from .task so first frame renders immediately)

    @MainActor
    func loadEligibleNodes() {
        guard isLoading else { return }

        let parentId = parent.id

        // Single pass: build lookup + filter eligible (faults SwiftData objects once)
        var eligible: [NodeItem] = []
        var lookup: [UUID: NodeV2] = [:]
        lookup.reserveCapacity(sld.nodes.count)

        for node in sld.nodes {
            lookup[node.id] = node
            guard !node.is_deleted,
                  node.parent_id == nil,
                  node.id != parentId,
                  node.node_class?.ocp == true else { continue }
            eligible.append(NodeItem(node: node))
        }

        eligible.sort { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }

        allEligibleNodes = eligible
        nodeLookup = lookup
        filteredNodes = eligible
        cachedFilteredIds = Set(eligible.map(\.id))
        isLoading = false
    }

    // MARK: - Filter (Task-based debounce + actor, no Combine pipeline)

    /// Called by the view via .onChange — debounces search, runs filter on dedicated actor
    @MainActor
    func onFilterInputChanged(debounce: Bool = true) {
        debounceTask?.cancel()
        // Skip debounce when search is cleared — instant feedback like the clear X button
        let shouldDebounce = debounce && !searchText.isEmpty
        debounceTask = Task { [weak self] in
            if shouldDebounce {
                try? await Task.sleep(nanoseconds: 250_000_000)
                guard !Task.isCancelled else { return }
            }
            await self?.runFilter()
        }
    }

    @MainActor
    private func runFilter() async {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let roomId = filterRoom?.id
        let snapshot = allEligibleNodes

        let results = await filterEngine.filter(
            nodes: snapshot,
            query: query,
            roomId: roomId
        )

        guard !Task.isCancelled else { return }

        // Cheap heuristic to skip redundant updates
        let current = filteredNodes
        guard results.count != current.count ||
              results.first?.id != current.first?.id ||
              results.last?.id != current.last?.id else { return }
        filteredNodes = results
        cachedFilteredIds = Set(results.map(\.id))
    }

    // MARK: - Selection

    func toggleSelection(_ nodeId: UUID) {
        if selectedNodeIds.contains(nodeId) {
            selectedNodeIds.remove(nodeId)
        } else {
            selectedNodeIds.insert(nodeId)
        }
    }

    func toggleSelectAll() {
        if isAllFilteredSelected {
            selectedNodeIds.subtract(cachedFilteredIds)
        } else {
            selectedNodeIds.formUnion(cachedFilteredIds)
        }
    }

    // MARK: - Linking

    func linkSelectedNodes(
        activeSession: IRSession?,
        activeViewId: UUID?,
        networkState: NetworkState,
        modelContext: ModelContext,
        onComplete: @escaping () -> Void
    ) {
        guard !isLinking else { return }
        isLinking = true

        let selectedIds = selectedNodeIds

        Task {
            // Pre-compute ancestor chain once (shared across all selected nodes)
            let ancestorChain = buildAncestorChain()

            // Link parent + ancestors to session once (deduplicated)
            if let session = activeSession {
                for node in [parent] + ancestorChain {
                    await linkSingleNodeToSession(
                        node: node,
                        session: session,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                }
            }

            for nodeId in selectedIds {
                guard let node = nodeLookup[nodeId] else { continue }

                await NodeService.linkNodeAsChild(
                    child: node,
                    parent: parent,
                    networkState: networkState,
                    modelContext: modelContext,
                    skipGraphUpdate: activeViewId != nil
                )

                // Only link the child itself (parent+ancestors already linked above)
                if let session = activeSession {
                    await linkSingleNodeToSession(
                        node: node,
                        session: session,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                }

                if let viewId = activeViewId {
                    await linkNodeToView(
                        node: node,
                        viewId: viewId,
                        networkState: networkState,
                        modelContext: modelContext
                    )
                }
            }

            // Single batch save for all session + view mapping mutations
            await MainActor.run {
                try? modelContext.save()
                isLinking = false
                onComplete()
            }
        }
    }

    // MARK: - Private helpers

    /// Builds parent ancestor chain using O(1) dictionary lookup instead of O(N) linear scan
    private func buildAncestorChain() -> [NodeV2] {
        var ancestors: [NodeV2] = []
        var current: NodeV2? = parent
        while let parentId = current?.parent_id,
              let ancestor = nodeLookup[parentId],
              !ancestor.is_deleted {
            ancestors.append(ancestor)
            current = ancestor
        }
        return ancestors
    }

    private func linkSingleNodeToSession(
        node: NodeV2,
        session: IRSession,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async {
        if session.nodes.contains(where: { $0.id == node.id }) {
            return
        }

        await MainActor.run {
            if !session.nodes.contains(where: { $0.id == node.id }) {
                session.nodes.append(node)
            }
            if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                node.ir_sessions.append(session)
            }
            // Save deferred to batch in linkSelectedNodes
        }

        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.createNodeSessionMapping(
                    nodeId: node.id,
                    sessionId: session.id
                )
                AppLogger.log(.info, "Created node-session mapping for node \(node.id)", category: .node)
            } catch {
                AppLogger.log(.error, "Failed to create node-session mapping, queueing: \(error)", category: .node)
                await MainActor.run {
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
        } else {
            await MainActor.run {
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
            AppLogger.log(.info, "Offline - queued node-session mapping for node \(node.id)", category: .node)
        }
    }

    private func linkNodeToView(
        node: NodeV2,
        viewId: UUID,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async {
        let mappingId = UUID()

        await MainActor.run {
            let mapping = MappingNodeSLDView(
                id: mappingId,
                node_id: node.id,
                sld_view_id: viewId,
                x: 0,
                y: 0,
                width: node.width,
                height: node.height,
                is_collapsed: false,
                is_deleted: false,
                created_at: Date(),
                modified_at: Date()
            )
            modelContext.insert(mapping)
            // Save deferred to batch in linkSelectedNodes
        }

        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.addNodeToView(
                    viewId: viewId,
                    nodeId: node.id,
                    x: 0,
                    y: 0,
                    width: node.width,
                    height: node.height
                )
                AppLogger.log(.info, "Created node-view mapping for linked node \(node.id) in view \(viewId)", category: .node)
            } catch {
                AppLogger.log(.error, "Failed to create node-view mapping, queueing: \(error)", category: .node)
                await MainActor.run {
                    networkState.enqueueNodeSLDViewCreate(
                        mappingId: mappingId,
                        nodeId: node.id,
                        viewId: viewId,
                        x: 0,
                        y: 0,
                        width: node.width,
                        height: node.height
                    )
                }
            }
        } else {
            await MainActor.run {
                networkState.enqueueNodeSLDViewCreate(
                    mappingId: mappingId,
                    nodeId: node.id,
                    viewId: viewId,
                    x: 0,
                    y: 0,
                    width: node.width,
                    height: node.height
                )
            }
            AppLogger.log(.info, "Offline - queued node-view mapping for linked node \(node.id)", category: .node)
        }
    }
}
