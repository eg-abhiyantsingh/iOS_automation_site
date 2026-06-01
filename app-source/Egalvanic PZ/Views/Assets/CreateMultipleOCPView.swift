//
//  CreateMultipleOCPView.swift
//  Egalvanic PZ
//
//  Creates multiple OCP child nodes at once for a parent asset.
//

import SwiftUI
import SwiftData

struct CreateMultipleOCPView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    let parent: NodeV2
    let sld: SLDV2
    let activeSession: IRSession?
    let activeViewId: UUID?
    let onComplete: () -> Void

    @Query private var allNodeClasses: [NodeClass]
    @Query private var allNodeSubtypes: [NodeSubtype]

    @State private var selectedNodeClass: NodeClass?
    @State private var selectedNodeSubtype: NodeSubtype?
    @State private var quantity: Int = 1
    @State private var labels: [String] = [""]
    @State private var isCreating = false
    @State private var createdCount = 0

    private var availableNodeClasses: [NodeClass] {
        allNodeClasses
            .filter { !$0.is_deleted && $0.ocp }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var availableNodeSubtypes: [NodeSubtype] {
        guard let nodeClass = selectedNodeClass else { return [] }
        return allNodeSubtypes
            .filter { !$0.is_deleted && $0.node_class_id == nodeClass.id }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var canCreate: Bool {
        selectedNodeClass != nil && !isCreating && labels.allSatisfy { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    /// Rebuilds the labels array, preserving any user edits for existing indices
    private func rebuildLabels() {
        guard let nodeClass = selectedNodeClass else {
            labels = Array(repeating: "", count: quantity)
            return
        }
        let existingCount = sld.nodes.filter {
            $0.parent_id == parent.id && !$0.is_deleted && $0.node_class?.id == nodeClass.id
        }.count

        var newLabels: [String] = []
        for i in 0..<quantity {
            if i < labels.count && !labels[i].isEmpty {
                // Preserve user-edited label
                newLabels.append(labels[i])
            } else {
                newLabels.append("\(nodeClass.name) \(existingCount + 1 + i)")
            }
        }
        labels = newLabels
    }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    // Parent info
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

                    // Node Class picker
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AppStrings.Assets.assetClass)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Picker(AppStrings.Assets.assetClass, selection: $selectedNodeClass) {
                            Text(AppStrings.AssetsExtra.selectClass)
                                .foregroundColor(.secondary)
                                .tag(nil as NodeClass?)
                            ForEach(availableNodeClasses) { nodeClass in
                                Text(nodeClass.name).tag(nodeClass as NodeClass?)
                            }
                        }
                        .pickerStyle(.menu)
                        .labelsHidden()
                        .onChange(of: selectedNodeClass) { _, _ in
                            selectedNodeSubtype = nil
                            rebuildLabels()
                        }
                    }

                    // Node Subtype picker (optional)
                    if !availableNodeSubtypes.isEmpty {
                        ModernPicker(
                            title: "Asset Subtype (Optional)",
                            icon: "tag",
                            placeholder: "Select asset subtype",
                            items: availableNodeSubtypes,
                            selection: $selectedNodeSubtype,
                            displayName: { $0.name },
                            useSheet: true
                        )
                    }

                    // Quantity
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AppStrings.Common.quantity)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Stepper("\(quantity)", value: $quantity, in: 1...10)
                            .onChange(of: quantity) { _, _ in rebuildLabels() }
                    }
                } header: {
                    Text("Create Multiple OCP")
                }

                // Editable labels
                if let nodeClass = selectedNodeClass {
                    Section {
                        ForEach(labels.indices, id: \.self) { index in
                            HStack(spacing: 12) {
                                NodeTypeIconCircle(
                                    style: nodeClass.style,
                                    size: 28,
                                    iconSize: 14,
                                    backgroundColor: Color.blue.opacity(0.1),
                                    iconColor: .blue
                                )
                                TextField(AppStrings.Common.label, text: $labels[index])
                                    .font(.subheadline)
                            }
                        }
                    } header: {
                        Text(AppStrings.AssetsExtra.labelsTitle)
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.createMultipleOCPTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create \(quantity)") {
                        createMultiple()
                    }
                    .fontWeight(.semibold)
                    .disabled(!canCreate)
                }
            }
            .disabled(isCreating)
            .overlay {
                if isCreating {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text("Creating \(createdCount)/\(quantity)...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 10)
                }
            }
        }
    }

    private func createMultiple() {
        guard let nodeClass = selectedNodeClass else { return }

        isCreating = true
        createdCount = 0

        Task {
            for i in 0..<quantity {
                let label = labels[i].trimmingCharacters(in: .whitespacesAndNewlines)

                if let newNode = await NodeService.createChildNode(
                    label: label,
                    nodeClass: nodeClass,
                    nodeSubtype: selectedNodeSubtype,
                    parent: parent,
                    sld: sld,
                    networkState: networkState,
                    modelContext: modelContext,
                    skipGraphUpdate: activeViewId != nil
                ) {
                    if let session = activeSession {
                        await linkNodeAndAncestorsToSession(node: newNode, session: session)
                    }
                    if let viewId = activeViewId {
                        await linkNodeToView(node: newNode, viewId: viewId)
                    }
                }

                await MainActor.run {
                    createdCount = i + 1
                }
            }

            await MainActor.run {
                isCreating = false
                onComplete()
            }
        }
    }

    // MARK: - Session & View Linking (same as CreateChildNodeView)

    private func linkNodeAndAncestorsToSession(node: NodeV2, session: IRSession) async {
        var nodesToLink: [NodeV2] = [node]
        nodesToLink.append(parent)
        var currentNode: NodeV2? = parent
        while let parentId = currentNode?.parent_id,
              let ancestorNode = sld.nodes.first(where: { $0.id == parentId && !$0.is_deleted }) {
            nodesToLink.append(ancestorNode)
            currentNode = ancestorNode
        }

        for nodeToLink in nodesToLink {
            await linkSingleNodeToSession(node: nodeToLink, session: session)
        }
    }

    private func linkSingleNodeToSession(node: NodeV2, session: IRSession) async {
        if session.nodes.contains(where: { $0.id == node.id }) { return }

        await MainActor.run {
            if !session.nodes.contains(where: { $0.id == node.id }) {
                session.nodes.append(node)
            }
            if !node.ir_sessions.contains(where: { $0.id == session.id }) {
                node.ir_sessions.append(session)
            }
            try? modelContext.save()
        }

        if networkState.mode == .online {
            do {
                _ = try await APIClient.shared.createNodeSessionMapping(
                    nodeId: node.id,
                    sessionId: session.id
                )
            } catch {
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
        }
    }

    private func linkNodeToView(node: NodeV2, viewId: UUID) async {
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
            try? modelContext.save()
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
            } catch {
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
        }
    }
}
