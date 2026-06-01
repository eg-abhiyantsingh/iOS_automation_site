//
//  CreateChildNodeView.swift
//  Egalvanic PZ
//
//  Created by Claude on 2025.
//

import SwiftUI
import SwiftData

/// Simplified form for creating a new child node
struct CreateChildNodeView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    let parent: NodeV2
    let sld: SLDV2
    let activeSession: IRSession?  // Optional active session for linking new child to session
    let activeViewId: UUID?  // Optional active SLD view ID for adding child to view mapping
    let onCreated: (NodeV2) -> Void

    @State private var label = ""
    @State private var selectedNodeClass: NodeClass?
    @State private var selectedNodeSubtype: NodeSubtype?
    @State private var selectedShortcut: NodeShortcut? = nil
    @State private var isCreating = false
    @State private var errorMessage: String?

    // Fetched once on appear — no @Query observation, so SwiftData changes elsewhere
    // won't trigger re-renders during typing or picker interaction.
    @State private var cachedNodeClasses: [NodeClass] = []
    @State private var cachedNodeSubtypes: [NodeSubtype] = []
    // Snapshot of all subtypes fetched once, used to recompute filtered subtypes on class change
    @State private var allSubtypesSnapshot: [NodeSubtype] = []

    private func loadInitialData() {
        let classFetch = FetchDescriptor<NodeClass>()
        let subtypeFetch = FetchDescriptor<NodeSubtype>()
        let allClasses = (try? modelContext.fetch(classFetch)) ?? []
        allSubtypesSnapshot = (try? modelContext.fetch(subtypeFetch)) ?? []
        cachedNodeClasses = allClasses
            .filter { !$0.is_deleted && $0.ocp }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private func recomputeNodeSubtypes() {
        guard let nodeClass = selectedNodeClass else {
            cachedNodeSubtypes = []
            return
        }
        cachedNodeSubtypes = allSubtypesSnapshot
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var canCreate: Bool {
        !label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        selectedNodeClass != nil
    }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 16) {
                        // Parent info (read-only)
                        HStack {
                            Image(systemName: "square.stack")
                                .foregroundColor(.secondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(AppStrings.AssetsExtra.parentEnclosure)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(parent.label)
                                    .font(.body)
                            }
                        }
                        .padding(.vertical, 4)

                        Divider()

                        // Label field
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.AssetsExtra.assetName)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            TextField(AppStrings.AssetsExtra.enterAssetName, text: $label)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }

                        // Node Class picker
                        ModernPicker(
                            title: AppStrings.Assets.assetClass,
                            icon: "cube",
                            placeholder: AppStrings.AssetsExtra.selectClass,
                            items: cachedNodeClasses,
                            selection: $selectedNodeClass,
                            displayName: { $0.name },
                            allowClear: false,
                            onSelectionChange: { _ in
                                DispatchQueue.main.async {
                                    selectedNodeSubtype = nil
                                    recomputeNodeSubtypes()
                                }
                            },
                            useSheet: true
                        )

                        Text(AppStrings.AssetsExtra.onlyOCPClasses)
                            .font(.caption2)
                            .foregroundColor(.secondary)

                        // Node Subtype picker (optional)
                        if !cachedNodeSubtypes.isEmpty {
                            ModernPicker(
                                title: "Asset Subtype (Optional)",
                                icon: "tag",
                                placeholder: "Select asset subtype",
                                items: cachedNodeSubtypes,
                                selection: $selectedNodeSubtype,
                                displayName: { $0.name },
                                useSheet: true
                            )
                        }

                        // Shortcut Picker
                        ShortcutPickerView(
                            selectedShortcut: $selectedShortcut,
                            nodeClass: selectedNodeClass,
                            nodeSubtype: selectedNodeSubtype,
                            sld: sld
                        )
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text(AppStrings.AssetsExtra.newChildAsset)
                }


                // Error message
                if let error = errorMessage {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.createChildAsset)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        createChild()
                    }
                    .fontWeight(.semibold)
                    .disabled(!canCreate || isCreating)
                }
            }
            .disabled(isCreating)
            .overlay {
                if isCreating {
                    ProgressView(AppStrings.AssetsExtra.creating)
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 10)
                }
            }
            .onAppear {
                loadInitialData()
            }
        }
    }

    private func createChild() {
        guard let nodeClass = selectedNodeClass else { return }

        isCreating = true
        errorMessage = nil

        Task {
            if let newNode = await NodeService.createChildNode(
                label: label.trimmingCharacters(in: .whitespacesAndNewlines),
                nodeClass: nodeClass,
                nodeSubtype: selectedNodeSubtype,
                parent: parent,
                sld: sld,
                suggestedShortcutId: selectedShortcut?.id,
                networkState: networkState,
                modelContext: modelContext,
                skipGraphUpdate: activeViewId != nil  // Parent will handle graph update with view-filtered nodes
            ) {
                // Link to active session if present (including parent hierarchy)
                if let session = activeSession {
                    await linkNodeAndAncestorsToSession(node: newNode, session: session)
                }

                // Link to active SLD view if present
                if let viewId = activeViewId {
                    await linkNodeToView(node: newNode, viewId: viewId)
                }

                await MainActor.run {
                    isCreating = false
                    onCreated(newNode)
                }
            } else {
                await MainActor.run {
                    isCreating = false
                    errorMessage = "Failed to create child asset"
                }
            }
        }
    }

    /// Links the node AND its parent hierarchy to the session via mapping_node_session
    /// This ensures that when a child asset is created, all ancestors are also visible in the session
    private func linkNodeAndAncestorsToSession(node: NodeV2, session: IRSession) async {
        // Collect all nodes to link: the node itself plus all ancestors
        var nodesToLink: [NodeV2] = [node]

        // Add the parent node and walk up the hierarchy
        nodesToLink.append(parent)
        var currentNode: NodeV2? = parent
        while let parentId = currentNode?.parent_id,
              let ancestorNode = sld.nodes.first(where: { $0.id == parentId && !$0.is_deleted }) {
            nodesToLink.append(ancestorNode)
            currentNode = ancestorNode
        }

        AppLogger.log(.info, "Linking child node and \(nodesToLink.count - 1) ancestor(s) to session", category: .node)

        // Link each node in the hierarchy
        for nodeToLink in nodesToLink {
            await linkSingleNodeToSession(node: nodeToLink, session: session)
        }
    }

    /// Links a single node to the session via mapping_node_session
    private func linkSingleNodeToSession(node: NodeV2, session: IRSession) async {
        // Check if already linked
        if session.nodes.contains(where: { $0.id == node.id }) {
            AppLogger.log(.debug, "Node \(node.id) already linked to session, skipping", category: .node)
            return
        }

        // Update bidirectional relationships
        await MainActor.run {
            if !session.nodes.contains(where: { $0.id == node.id }) {
                session.nodes.append(node)
            }
            if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                node.ir_sessions.append(session)
            }
            try? modelContext.save()
        }

        // Sync mapping to server
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

    /// Links the node to the SLD view via mapping_node_sld_view
    private func linkNodeToView(node: NodeV2, viewId: UUID) async {
        let mappingId = UUID()

        // Create local mapping
        await MainActor.run {
            let mapping = MappingNodeSLDView(
                id: mappingId,
                node_id: node.id,
                sld_view_id: viewId,
                x: 0,  // Child nodes have relative position within parent
                y: 0,
                width: node.width,
                height: node.height,
                is_collapsed: false,
                is_deleted: false,
                created_at: Date(),
                modified_at: Date()
            )
            modelContext.insert(mapping)
            try? modelContext.save()
        }

        // Sync to server (online) or queue (offline)
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
                AppLogger.log(.info, "Created node-view mapping for child node \(node.id) in view \(viewId)", category: .node)
            } catch {
                AppLogger.log(.error, "Failed to create node-view mapping, queueing: \(error)", category: .node)
                // Fall back to offline sync queue
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
            AppLogger.log(.info, "Offline - queued node-view mapping for child node \(node.id)", category: .node)
        }
    }
}
