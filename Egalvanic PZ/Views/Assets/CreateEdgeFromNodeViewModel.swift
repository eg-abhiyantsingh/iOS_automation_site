//
//  CreateEdgeFromNodeViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
class CreateEdgeFromNodeViewModel: ObservableObject {

    // MARK: - Display Data

    struct NodeOption: Identifiable, Equatable {
        let id: UUID
        let displayLabel: String
        let subtitle: String?
        let qrCode: String?
    }

    // MARK: - Published Properties (UI State)

    @Published var sourceId: UUID?
    @Published var targetId: UUID?
    @Published var sourceTerminalId: UUID?
    @Published var targetTerminalId: UUID?
    @Published var selectedEdgeClassId: UUID?
    @Published var isCreating = false
    @Published var errorMessage: String?
    @Published var draftCoreAttributes: [UUID: String] = [:]
    @Published var showOnlyRequired: Bool = true

    // Cached validation state — updated only when source/target changes, not on every body eval
    @Published private(set) var isDuplicateConnection: Bool = false
    @Published private(set) var isMissingSourceTerminal: Bool = false
    @Published private(set) var isMissingTargetTerminal: Bool = false

    // MARK: - Pre-computed Data (set once in init)

    let connectionType: EdgeConnectionType
    let activeViewId: UUID?
    let contextNodeOptions: [NodeOption]
    let allExternalNodeOptions: [NodeOption]
    let availableEdgeClasses: [EdgeClass]

    private let nodeById: [UUID: NodeV2]
    private let sld: SLDV2

    // MARK: - Dependencies

    private var modelContext: ModelContext?
    let networkState: NetworkState  // Stored as let — not observed, no re-render subscription

    // MARK: - Init

    init(
        contextNode: NodeV2,
        sld: SLDV2,
        connectionType: EdgeConnectionType,
        activeViewId: UUID?,
        allEdgeClasses: [EdgeClass],
        networkState: NetworkState
    ) {
        self.networkState = networkState
        self.sld = sld
        self.connectionType = connectionType
        self.activeViewId = activeViewId

        // 1. Single pass: build O(1) lookup map
        var nodeMap: [UUID: NodeV2] = [:]
        for node in sld.nodes where !node.is_deleted {
            nodeMap[node.id] = node
        }
        self.nodeById = nodeMap

        // 2. Child nodes of context node
        let children = sld.nodes.filter { $0.parent_id == contextNode.id && !$0.is_deleted }

        // 3. Context node options (context + children)
        var ctxOptions: [NodeOption] = [
            NodeOption(
                id: contextNode.id,
                displayLabel: contextNode.label,
                subtitle: contextNode.node_class?.name,
                qrCode: (contextNode.qr_code?.isEmpty == false) ? contextNode.qr_code : nil
            )
        ]
        for child in children {
            ctxOptions.append(NodeOption(
                id: child.id,
                displayLabel: "\(contextNode.label) > \(child.label)",
                subtitle: child.node_class?.name,
                qrCode: (child.qr_code?.isEmpty == false) ? child.qr_code : nil
            ))
        }
        self.contextNodeOptions = ctxOptions

        // 4. External node options (all other nodes, sorted, with parent labels via O(1) lookup)
        let contextNodeIds = Set([contextNode.id] + children.map { $0.id })
        self.allExternalNodeOptions = nodeMap.values
            .filter { !contextNodeIds.contains($0.id) }
            .sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
            .map { node in
                let qr: String? = (node.qr_code?.isEmpty == false) ? node.qr_code : nil
                if let parentId = node.parent_id, let parent = nodeMap[parentId] {
                    return NodeOption(
                        id: node.id,
                        displayLabel: "\(parent.label) > \(node.label)",
                        subtitle: node.node_class?.name,
                        qrCode: qr
                    )
                } else {
                    return NodeOption(
                        id: node.id,
                        displayLabel: node.label,
                        subtitle: node.node_class?.name,
                        qrCode: qr
                    )
                }
            }

        // 5. Edge classes: filtered & sorted
        self.availableEdgeClasses = allEdgeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }

        // 6. Initial source/target based on connection type
        switch connectionType {
        case .lineside:
            self.sourceId = nil
            self.targetId = contextNode.id
        case .loadside:
            self.sourceId = contextNode.id
            self.targetId = nil
        }
    }

    // MARK: - Configure Dependencies

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
        revalidate()
    }

    // MARK: - Cheap Computed Properties

    var sourceNode: NodeV2? { sourceId.flatMap { nodeById[$0] } }
    var targetNode: NodeV2? { targetId.flatMap { nodeById[$0] } }

    /// Find external (non-context) nodes whose qr_code matches the scanned value.
    /// Restricted to externals because the QR scan button only appears on the external picker.
    func findExternalNodes(byQRCode code: String) -> [NodeV2] {
        let needle = code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !needle.isEmpty else { return [] }
        return allExternalNodeOptions.compactMap { option in
            guard let node = nodeById[option.id], node.qr_code?.lowercased() == needle else { return nil }
            return node
        }
    }

    /// Derives the selected EdgeClass from the UUID — stable value-type comparison for Picker
    var selectedEdgeClass: EdgeClass? {
        guard let id = selectedEdgeClassId else { return nil }
        let resolved = availableEdgeClasses.first { $0.id == id }
        if resolved == nil {
            AppLogger.log(.notice, "[EdgeClassPicker] Failed to resolve EdgeClass for id: \(id.uuidString.prefix(8)) — not found in \(availableEdgeClasses.count) classes", category: .ui)
        }
        return resolved
    }

    var canCreate: Bool {
        !(sourceId != nil && targetId != nil && sourceId == targetId) &&
        !isDuplicateConnection &&
        !isMissingSourceTerminal &&
        !isMissingTargetTerminal
    }

    /// Recomputes validation state — call when sourceId, targetId, or terminal selection changes
    func revalidate() {
        if let s = sourceId, let t = targetId {
            isDuplicateConnection = sld.edges.contains { edge in
                !edge.is_deleted &&
                edge.source == s &&
                edge.target == t
            }
        } else {
            isDuplicateConnection = false
        }

        if let node = sourceNode {
            let allTerminals = node.node_terminals.filter { !$0.is_deleted }
            isMissingSourceTerminal = !allTerminals.isEmpty && (node.sourceEligibleTerminals.isEmpty || sourceTerminalId == nil)
        } else {
            isMissingSourceTerminal = false
        }

        if let node = targetNode {
            let allTerminals = node.node_terminals.filter { !$0.is_deleted }
            isMissingTargetTerminal = !allTerminals.isEmpty && (node.targetEligibleTerminals.isEmpty || targetTerminalId == nil)
        } else {
            isMissingTargetTerminal = false
        }
    }

    var connectionTypeLabel: String {
        switch connectionType {
        case .lineside: return AppStrings.AssetsExtra.linesideIncoming
        case .loadside: return AppStrings.AssetsExtra.loadsideOutgoing
        }
    }

    var connectionTypeDescription: String {
        switch connectionType {
        case .lineside: return AppStrings.AssetsExtra.powerFlowsIn
        case .loadside: return AppStrings.AssetsExtra.powerFlowsOut
        }
    }

    // MARK: - Node Selection Handlers

    func onSourceNodeChanged() {
        sourceTerminalId = nil
        if let node = sourceNode {
            let eligible = node.sourceEligibleTerminals
            if eligible.count == 1 {
                sourceTerminalId = eligible[0].id
            }
        }
        revalidate()
    }

    func onTargetNodeChanged() {
        targetTerminalId = nil
        if let node = targetNode {
            let eligible = node.targetEligibleTerminals
            if eligible.count == 1 {
                targetTerminalId = eligible[0].id
            }
        }
        revalidate()
    }

    // MARK: - Edge Class Change Handling

    func handleEdgeClassChange(from oldId: UUID?, to newId: UUID?) {
        AppLogger.log(.debug, "[EdgeClassPicker] Selection changed: \(oldId?.uuidString.prefix(8) ?? "nil") → \(newId?.uuidString.prefix(8) ?? "nil")", category: .ui)

        let oldClass = oldId.flatMap { id in availableEdgeClasses.first { $0.id == id } }
        guard let newId, let newClass = availableEdgeClasses.first(where: { $0.id == newId }) else {
            AppLogger.log(.debug, "[EdgeClassPicker] Cleared selection (set to None)", category: .ui)
            draftCoreAttributes = [:]
            return
        }

        AppLogger.log(.debug, "[EdgeClassPicker] Resolved: \(oldClass?.name ?? "None") → \(newClass.name) (id: \(newId.uuidString.prefix(8)))", category: .ui)
        draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
            from: oldClass,
            to: newClass,
            currentAttributes: draftCoreAttributes
        )
        // Apply class-defined defaults (ZP-2251) to any fields still empty
        // after preservation.
        CoreAttributesService.applyDefaultValues(from: newClass, into: &draftCoreAttributes)
        AppLogger.log(.debug, "[EdgeClassPicker] Core attributes preserved: \(draftCoreAttributes.count) values", category: .ui)
    }

    // MARK: - Core Attributes Builder

    private func buildCoreAttributes() -> [EdgeProperty] {
        guard let edgeClass = selectedEdgeClass else { return [] }
        return edgeClass.definition.compactMap { classProperty in
            guard let value = draftCoreAttributes[classProperty.id], !value.isEmpty else { return nil }
            return EdgeProperty(
                id: classProperty.id,
                edge_class_property: classProperty,
                name: classProperty.name,
                value: value
            )
        }
    }

    // MARK: - Edge Creation

    func createEdge(onCreated: @escaping (EdgeV2) -> Void) {
        guard let modelContext else { return }

        isCreating = true
        errorMessage = nil

        let coreAttributes = buildCoreAttributes()
        let newEdge = EdgeV2(
            id: UUID(),
            source: sourceId,
            target: targetId,
            sld: sld,
            is_deleted: false,
            sourceNodeTerminalId: sourceTerminalId,
            targetNodeTerminalId: targetTerminalId,
            edge_class: selectedEdgeClass,
            core_attributes: coreAttributes,
            points: nil,
            algorithm: nil
        )
        newEdge.sld = sld
        newEdge.needsSync = true
        newEdge.lastSyncedAt = nil
        modelContext.insert(newEdge)
        for attr in coreAttributes {
            modelContext.insert(attr)
        }

        do {
            try modelContext.save()
            AppLogger.log(.info, "Created edge \(newEdge.id) locally", category: .node)

            // Update graph immediately (skip when in view context - parent will handle)
            if activeViewId == nil {
                WebViewBridge.updateGraphFromSLD(sld.id, in: modelContext, animated: true)
            } else {
                AppLogger.log(.debug, "Skipping graph update - parent will handle with view-filtered nodes", category: .node)
            }

            // Dismiss immediately after local success (offline-first pattern)
            isCreating = false
            onCreated(newEdge)

            // Sync in background
            syncEdgeInBackground(newEdge, networkState: networkState, modelContext: modelContext)
        } catch {
            isCreating = false
            errorMessage = "Failed to save edge locally: \(error.localizedDescription)"
        }
    }

    // MARK: - Private Helpers

    private func syncEdgeInBackground(_ edge: EdgeV2, networkState: NetworkState, modelContext: ModelContext) {
        Task {
            if networkState.mode == .online {
                do {
                    AppLogger.log(.info, "Attempting to sync edge \(edge.id) to server", category: .node)
                    _ = try await APIClient.shared.createEdge(edge: edge)
                    edge.needsSync = false
                    edge.lastSyncedAt = Date()
                    try? modelContext.save()
                    AppLogger.log(.info, "Edge synced to server successfully", category: .node)
                } catch {
                    AppLogger.log(.error, "Failed to sync edge to server: \(error)", category: .node)
                    let op = SyncOp(target: .edge, operation: .create, edge: edge)
                    networkState.enqueue(op)
                    AppLogger.log(.info, "Edge queued for later sync", category: .node)
                }
            } else {
                AppLogger.log(.info, "Offline mode - queueing edge for later sync", category: .node)
                let op = SyncOp(target: .edge, operation: .create, edge: edge)
                networkState.enqueue(op)
            }
        }
    }
}
