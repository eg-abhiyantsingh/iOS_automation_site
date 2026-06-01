//
//  NodeEdgesViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
final class NodeEdgesViewModel: ObservableObject {

    // MARK: - Models

    struct EdgeDisplayModel: Identifiable {
        let id: UUID
        let edge: EdgeV2
        let connectedNode: NodeV2?
        let title: String
        let subtitle: String?
        let isComplete: Bool
    }

    // MARK: - Dependencies

    let node: NodeV2
    let sld: SLDV2

    // MARK: - Output

    @Published private(set) var linesideItems: [EdgeDisplayModel] = []
    @Published private(set) var loadsideItems: [EdgeDisplayModel] = []
    @Published private(set) var isMissingSource: Bool = false

    // MARK: - Cache

    private var cachedEdgeClasses: [EdgeClass]?

    // MARK: - Init

    init(node: NodeV2, sld: SLDV2) {
        self.node = node
        self.sld = sld
    }

    // MARK: - Public API

    /// Recomputes cached edge lists off the main thread.
    /// Snapshots SwiftData properties on main (fast reads), sorts/filters off main, assigns results back on main.
    func recompute() {
        // Step 1: Snapshot SwiftData properties on main thread (safe reads, no computation)
        let nodeId = node.id
        let needsSource = node.node_class?.needs_source == true
        let nodeRefs = Dictionary(uniqueKeysWithValues: sld.nodes.map { ($0.id, $0) })
        let edgeRefs = Dictionary(uniqueKeysWithValues: sld.edges.map { ($0.id, $0) })

        struct NodeSnap { let id: UUID; let parentId: UUID?; let isDeleted: Bool; let label: String }
        struct EdgeSnap { let id: UUID; let source: UUID?; let target: UUID?; let isDeleted: Bool }

        let nodeSnaps = sld.nodes.map { NodeSnap(id: $0.id, parentId: $0.parent_id, isDeleted: $0.is_deleted, label: $0.label) }
        let edgeSnaps = sld.edges.map { EdgeSnap(id: $0.id, source: $0.source, target: $0.target, isDeleted: $0.is_deleted) }

        // Snapshot per-edge display data needed for EdgeDisplayModel (must read @Model on main)
        struct EdgeDisplaySnap {
            let id: UUID
            let lengthValue: String?
            let edgeClassName: String?
            let isComplete: Bool
        }
        let edgeDisplaySnaps: [UUID: EdgeDisplaySnap] = Dictionary(uniqueKeysWithValues: sld.edges.compactMap { edge -> (UUID, EdgeDisplaySnap)? in
            guard !edge.is_deleted else { return nil }
            let length = edge.core_attributes.first(where: { $0.name.localizedCaseInsensitiveContains("length") })?.value.trimmingCharacters(in: .whitespacesAndNewlines)
            return (edge.id, EdgeDisplaySnap(
                id: edge.id,
                lengthValue: (length?.isEmpty == false) ? length : nil,
                edgeClassName: edge.edge_class?.name,
                isComplete: edge.af_isComplete
            ))
        })

        // Step 2: Heavy filtering + sorting off main thread
        Task.detached(priority: .userInitiated) {
            let nodeLookup = Dictionary(uniqueKeysWithValues: nodeSnaps.map { ($0.id, $0) })

            let childIds = nodeSnaps.filter { $0.parentId == nodeId && !$0.isDeleted }.map { $0.id }
            let ids = Set([nodeId] + childIds)

            let connected = edgeSnaps.filter { snap in
                !snap.isDeleted && (snap.source.map { ids.contains($0) } ?? false || snap.target.map { ids.contains($0) } ?? false)
            }

            let labelCache = Dictionary(uniqueKeysWithValues: connected.map { edge in
                let sourceInIds = edge.source.map { ids.contains($0) } ?? false
                let otherId: UUID? = sourceInIds ? edge.target : edge.source
                let label = otherId.flatMap { nodeLookup[$0]?.label } ?? AppStrings.Connections.notAssigned
                return (edge.id, label)
            })

            let linesideIds = connected
                .filter { $0.target.map { ids.contains($0) } ?? false }
                .sorted { (labelCache[$0.id] ?? "").localizedCaseInsensitiveCompare(labelCache[$1.id] ?? "") == .orderedAscending }
                .map { $0.id }

            let loadsideIds = connected
                .filter { snap in
                    (snap.source.map { ids.contains($0) } ?? false) && !(snap.target.map { ids.contains($0) } ?? false)
                }
                .sorted { (labelCache[$0.id] ?? "").localizedCaseInsensitiveCompare(labelCache[$1.id] ?? "") == .orderedAscending }
                .map { $0.id }

            let missingSource = needsSource && linesideIds.isEmpty

            // Step 3: Build display models on main thread using original @Model references
            await MainActor.run { [nodeRefs, edgeRefs, edgeDisplaySnaps, labelCache] in
                func buildItem(edgeId: UUID, ids: Set<UUID>) -> EdgeDisplayModel? {
                    guard let edge = edgeRefs[edgeId] else { return nil }
                    let sourceInIds = edge.source.map { ids.contains($0) } ?? false
                    let otherId: UUID? = sourceInIds ? edge.target : edge.source
                    let snap = edgeDisplaySnaps[edgeId]

                    var subtitleParts: [String] = []
                    if let length = snap?.lengthValue { subtitleParts.append("\(length) ft") }
                    if let className = snap?.edgeClassName { subtitleParts.append(className) }

                    return EdgeDisplayModel(
                        id: edge.id,
                        edge: edge,
                        connectedNode: otherId.flatMap { nodeRefs[$0] },
                        title: labelCache[edgeId] ?? AppStrings.Connections.notAssigned,
                        subtitle: subtitleParts.isEmpty ? nil : subtitleParts.joined(separator: " "),
                        isComplete: snap?.isComplete ?? false
                    )
                }

                self.linesideItems = linesideIds.compactMap { buildItem(edgeId: $0, ids: ids) }
                self.loadsideItems = loadsideIds.compactMap { buildItem(edgeId: $0, ids: ids) }
                self.isMissingSource = missingSource
            }
        }
    }

    /// Preloads edge classes into cache on appear — so sheet opens instantly
    func preloadEdgeClasses(modelContext: ModelContext) {
        guard cachedEdgeClasses == nil else { return }
        let descriptor = FetchDescriptor<EdgeClass>()
        let results = (try? modelContext.fetch(descriptor)) ?? []
        cachedEdgeClasses = results.filter { !$0.is_deleted }
    }

    /// Returns cached edge classes — call preloadEdgeClasses first
    func edgeClasses() -> [EdgeClass] {
        cachedEdgeClasses ?? []
    }

    /// Soft-deletes an edge and recomputes cached state on success
    func deleteEdge(_ edge: EdgeV2, networkState: NetworkState, modelContext: ModelContext) {
        EdgeService.deleteEdges(
            edgeIds: Set([edge.id]),
            diagram: sld,
            networkState: networkState,
            modelContext: modelContext
        ) { [weak self] success, message in
            if let msg = message {
                AppLogger.log(success ? .info : .error, msg, category: .node)
            }
            if success {
                DispatchQueue.main.async {
                    self?.recompute()
                }
            }
        }
    }
}
