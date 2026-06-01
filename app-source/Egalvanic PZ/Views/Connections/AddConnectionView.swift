//
//  AddConnectionView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import SwiftUI
import SwiftData

struct AddConnectionViewV2: View {
    private enum ConnectionSide: String, Identifiable {
        case source, target
        var id: String { rawValue }
    }

    @StateObject private var viewModel: AddConnectionManager
    @Environment(\.dismiss) private var dismiss

    @State private var scanningSide: ConnectionSide?
    @State private var duplicateQRNodes: [NodeV2] = []
    @State private var pendingDuplicateSide: ConnectionSide?
    @State private var showDuplicateQRAlert = false
    @State private var notFoundQRCode = ""
    @State private var showAssetNotFound = false

    let onSave: (EdgeV2) -> Void
    let onCancel: () -> Void
    
    init(
        availableNodes: [NodeV2],
        availableEdgeClasses: [EdgeClass],
        sld: SLDV2,
        onSave: @escaping (EdgeV2) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self._viewModel = StateObject(wrappedValue: AddConnectionManager(
            availableNodes: availableNodes,
            availableEdgeClasses: availableEdgeClasses,
            diagram: sld
        ))
        self.onSave = onSave
        self.onCancel = onCancel
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section(AppStrings.AssetsExtra.connectionDetails) {
                    VStack(alignment: .leading, spacing: 16) {
                        // Source Node Picker
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.AssetsExtra.sourceNode)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack(spacing: 8) {
                                SearchableNodePicker3(
                                    selectedNodeId: $viewModel.sourceId,
                                    nodes: viewModel.nodeOptions,
                                    placeholder: AppStrings.Connections.selectSourceNode
                                )
                                .onChange(of: viewModel.sourceId) { _, _ in
                                    viewModel.onSourceNodeChanged()
                                }

                                qrScanButton(for: .source)
                            }
                        }

                        // Source Terminal Picker
                        TerminalPicker(
                            node: viewModel.sourceNode,
                            side: .source,
                            selectedTerminalId: $viewModel.sourceTerminalId
                        )

                        // Target Node Picker
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.AssetsExtra.targetNode)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack(spacing: 8) {
                                SearchableNodePicker3(
                                    selectedNodeId: $viewModel.targetId,
                                    nodes: viewModel.nodeOptions,
                                    placeholder: AppStrings.Connections.selectTargetNode
                                )
                                .onChange(of: viewModel.targetId) { _, _ in
                                    viewModel.onTargetNodeChanged()
                                }

                                qrScanButton(for: .target)
                            }
                        }

                        // Target Terminal Picker
                        TerminalPicker(
                            node: viewModel.targetNode,
                            side: .target,
                            selectedTerminalId: $viewModel.targetTerminalId
                        )

                        // Edge Class Picker
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.Connections.connectionType)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Picker(AppStrings.Connections.connectionType, selection: $viewModel.selectedEdgeClassId) {
                                Text(AppStrings.Connections.selectType)
                                    .foregroundColor(.secondary)
                                    .tag(nil as UUID?)
                                ForEach(viewModel.availableEdgeClasses, id: \.id) { edgeClass in
                                    Text(edgeClass.name).tag(edgeClass.id as UUID?)
                                }
                            }
                            .pickerStyle(.menu)
                            .labelsHidden()
                            .onChange(of: viewModel.selectedEdgeClassId) { oldId, newId in
                                viewModel.handleEdgeClassChange(from: oldId, to: newId)
                            }
                        }
                    }
                    .padding(.vertical, 8)
                }

                // Validation Message
                if let validationMessage = viewModel.validationMessage {
                    Section {
                        HStack {
                            Image(systemName: viewModel.isDuplicateConnection ? "exclamationmark.circle.fill" : "exclamationmark.triangle.fill")
                                .foregroundColor(viewModel.isDuplicateConnection ? .red : .orange)
                            Text(validationMessage)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                // Connection Preview
                if let summary = viewModel.connectionSummary {
                    Section(AppStrings.Connections.preview) {
                        Text(summary)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                    }
                }

                // Core Attributes Section
                if let edgeClass = viewModel.selectedEdgeClass, !edgeClass.definition.isEmpty {
                    Section {
                        EdgeCoreAttributesSection(
                            edge: nil,
                            selectedEdgeClass: edgeClass,
                            draftAttributes: $viewModel.draftCoreAttributes
                        )
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.newConnection)
            .navigationBarTitleDisplayMode(.inline)
            .fullScreenCover(item: $scanningSide) { side in
                QRCodeScannerView(scannedCode: .constant("")) { code in
                    handleQRScan(code, side: side)
                }
            }
            .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
                ForEach(duplicateQRNodes.prefix(5), id: \.id) { node in
                    Button("\(node.label) (\(node.type))") {
                        applyScanResult(node, side: pendingDuplicateSide)
                    }
                }
                Button(AppStrings.Common.cancel, role: .cancel) {
                    duplicateQRNodes = []
                    pendingDuplicateSide = nil
                }
            } message: {
                Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateQRNodes.count))
            }
            .alert(AppStrings.Locations.assetNotFound, isPresented: $showAssetNotFound) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(AppStrings.Locations.assetNotFoundMessage(notFoundQRCode))
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        handleCancel()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        handleSave()
                    }
                    .fontWeight(.semibold)
                    .disabled(!viewModel.canSave)
                }
            }
        }
    }
    
    private func handleSave() {
        guard let edge = viewModel.prepareEdge() else { return }
        onSave(edge)
        dismiss()
    }
    
    private func handleCancel() {
        onCancel()
        dismiss()
    }

    @ViewBuilder
    private func qrScanButton(for side: ConnectionSide) -> some View {
        Button {
            scanningSide = side
        } label: {
            Image(systemName: "qrcode.viewfinder")
                .font(.title3)
                .foregroundColor(.blue)
                .frame(width: 44, height: 44)
                .background(Color(.systemGray6))
                .cornerRadius(8)
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(AppStrings.AssetsExtra.scanQRCode)
    }

    private func handleQRScan(_ code: String, side: ConnectionSide) {
        let matches = viewModel.findNodes(byQRCode: code)
        if matches.count == 1 {
            applyScanResult(matches[0], side: side)
        } else if matches.count > 1 {
            duplicateQRNodes = matches
            pendingDuplicateSide = side
            showDuplicateQRAlert = true
        } else {
            notFoundQRCode = code
            showAssetNotFound = true
        }
    }

    private func applyScanResult(_ node: NodeV2, side: ConnectionSide?) {
        guard let side else { return }
        switch side {
        case .source:
            viewModel.sourceId = node.id
        case .target:
            viewModel.targetId = node.id
        }
        duplicateQRNodes = []
        pendingDuplicateSide = nil
    }
}

// MARK: - SearchableNodePicker Component (Bottom Sheet style)

/// A node picker that opens a bottom sheet with search functionality
struct SearchableNodePicker3: View {
    @Binding var selectedNodeId: UUID?
    let placeholder: String

    @State private var isSheetPresented = false
    private let nodeItems: [NodePickerSheetViewModel.NodeItem]

    init(
        selectedNodeId: Binding<UUID?>,
        nodes: [(id: UUID, displayLabel: String, subtitle: String?, qrCode: String?)],
        placeholder: String
    ) {
        self._selectedNodeId = selectedNodeId
        self.placeholder = placeholder
        self.nodeItems = nodes.map {
            NodePickerSheetViewModel.NodeItem(id: $0.id, displayLabel: $0.displayLabel, subtitle: $0.subtitle, qrCode: $0.qrCode)
        }
    }

    private var selectedNode: NodePickerSheetViewModel.NodeItem? {
        guard let selectedNodeId else { return nil }
        return nodeItems.first { $0.id == selectedNodeId }
    }

    var body: some View {
        Button(action: {
            isSheetPresented = true
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    if let node = selectedNode {
                        Text(node.displayLabel)
                            .foregroundColor(.primary)
                        if let subtitle = node.subtitle {
                            Text(subtitle)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        if let qrCode = node.qrCode {
                            HStack(spacing: 4) {
                                Image(systemName: "qrcode")
                                Text(qrCode)
                            }
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        }
                    } else if selectedNodeId != nil {
                        Text(AppStrings.Connections.missingNode)
                            .foregroundColor(.red)
                            .italic()
                    } else {
                        Text(AppStrings.Connections.notAssigned)
                            .foregroundColor(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
                    .font(.caption)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $isSheetPresented) {
            NodePickerSheet(
                selectedNodeId: $selectedNodeId,
                nodes: nodeItems,
                placeholder: placeholder,
                isPresented: $isSheetPresented
            )
            .presentationDetents([.large])
        }
    }
}

// MARK: - Node Picker Bottom Sheet

struct NodePickerSheet: View {
    @Binding var selectedNodeId: UUID?
    let placeholder: String
    @Binding var isPresented: Bool

    @StateObject private var viewModel: NodePickerSheetViewModel

    init(
        selectedNodeId: Binding<UUID?>,
        nodes: [NodePickerSheetViewModel.NodeItem],
        placeholder: String,
        isPresented: Binding<Bool>
    ) {
        self._selectedNodeId = selectedNodeId
        self.placeholder = placeholder
        self._isPresented = isPresented
        self._viewModel = StateObject(wrappedValue: NodePickerSheetViewModel(nodes: nodes))
    }

    var body: some View {
        NavigationView {
            List {
                // "Not Assigned" option
                Button(action: {
                    selectedNodeId = nil
                    isPresented = false
                }) {
                    HStack {
                        Text(AppStrings.Connections.notAssigned)
                            .foregroundColor(.secondary)
                        Spacer()
                        if selectedNodeId == nil {
                            Image(systemName: "checkmark")
                                .foregroundColor(.blue)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())

                ForEach(viewModel.filteredNodes) { node in
                    Button(action: {
                        selectedNodeId = node.id
                        isPresented = false
                    }) {
                        rowView(node)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .transaction { $0.animation = nil }
            .searchable(text: $viewModel.searchText, prompt: AppStrings.AssetsExtra.searchPlaceholder)
            .navigationTitle(placeholder)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.cancel) {
                        isPresented = false
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func rowView(_ node: NodePickerSheetViewModel.NodeItem) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(node.displayLabel)
                    .foregroundColor(.primary)
                if let subtitle = node.subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let qrCode = node.qrCode {
                    HStack(spacing: 4) {
                        Image(systemName: "qrcode")
                        Text(qrCode)
                    }
                    .font(.caption2)
                    .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if node.id == selectedNodeId {
                Image(systemName: "checkmark")
                    .foregroundColor(.blue)
            }
        }
        .contentShape(Rectangle())
    }
}
