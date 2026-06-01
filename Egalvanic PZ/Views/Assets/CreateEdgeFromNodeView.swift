//
//  CreateEdgeFromNodeView.swift
//  Egalvanic PZ
//
//  Created by Claude on 2025.
//

import SwiftUI
import SwiftData

/// Sheet form for creating an edge from a node context
struct CreateEdgeFromNodeView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @StateObject private var viewModel: CreateEdgeFromNodeViewModel
    let onCreated: (EdgeV2) -> Void

    @State private var showQRScanner = false
    @State private var duplicateQRNodes: [NodeV2] = []
    @State private var showDuplicateQRAlert = false
    @State private var notFoundQRCode = ""
    @State private var showAssetNotFound = false

    init(
        contextNode: NodeV2,
        sld: SLDV2,
        connectionType: EdgeConnectionType,
        activeViewId: UUID? = nil,
        allEdgeClasses: [EdgeClass] = [],
        networkState: NetworkState,
        onCreated: @escaping (EdgeV2) -> Void
    ) {
        self._viewModel = StateObject(wrappedValue: CreateEdgeFromNodeViewModel(
            contextNode: contextNode,
            sld: sld,
            connectionType: connectionType,
            activeViewId: activeViewId,
            allEdgeClasses: allEdgeClasses,
            networkState: networkState
        ))
        self.onCreated = onCreated
    }

    var body: some View {
        NavigationView {
            Form {
                // Connection type info
                Section {
                    HStack {
                        Image(systemName: viewModel.connectionType == .lineside ? "arrow.right.to.line" : "arrow.left.to.line")
                            .foregroundColor(viewModel.connectionType == .lineside ? .blue : .orange)
                            .font(.title2)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(viewModel.connectionTypeLabel)
                                .font(.headline)
                            Text(viewModel.connectionTypeDescription)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                // Source/Target selection
                Section(AppStrings.AssetsExtra.connectionDetails) {
                    VStack(alignment: .leading, spacing: 16) {
                        // Source picker
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.AssetsExtra.sourceNode)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            if viewModel.connectionType == .loadside {
                                DropdownNodePicker(
                                    selectedNodeId: $viewModel.sourceId,
                                    nodes: viewModel.contextNodeOptions,
                                    placeholder: AppStrings.AssetsExtra.selectSource
                                )
                                .onChange(of: viewModel.sourceId) { _, _ in
                                    viewModel.onSourceNodeChanged()
                                }
                            } else {
                                HStack(spacing: 8) {
                                    DropdownNodePicker(
                                        selectedNodeId: $viewModel.sourceId,
                                        nodes: viewModel.allExternalNodeOptions,
                                        placeholder: AppStrings.AssetsExtra.selectSource
                                    )
                                    .onChange(of: viewModel.sourceId) { _, _ in
                                        viewModel.onSourceNodeChanged()
                                    }

                                    qrScanButton()
                                }
                            }
                        }

                        // Source terminal picker
                        DropdownTerminalPicker(
                            node: viewModel.sourceNode,
                            side: .source,
                            selectedTerminalId: $viewModel.sourceTerminalId
                        )
                        .onChange(of: viewModel.sourceTerminalId) { _, _ in
                            viewModel.revalidate()
                        }

                        // Arrow indicator
                        HStack {
                            Spacer()
                            Image(systemName: "arrow.down")
                                .foregroundColor(.secondary)
                            Spacer()
                        }

                        // Target picker
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.AssetsExtra.targetNode)
                                .font(.caption)
                                .foregroundColor(.secondary)

                            if viewModel.connectionType == .lineside {
                                DropdownNodePicker(
                                    selectedNodeId: $viewModel.targetId,
                                    nodes: viewModel.contextNodeOptions,
                                    placeholder: AppStrings.AssetsExtra.selectTarget
                                )
                                .onChange(of: viewModel.targetId) { _, _ in
                                    viewModel.onTargetNodeChanged()
                                }
                            } else {
                                HStack(spacing: 8) {
                                    DropdownNodePicker(
                                        selectedNodeId: $viewModel.targetId,
                                        nodes: viewModel.allExternalNodeOptions,
                                        placeholder: AppStrings.AssetsExtra.selectTarget
                                    )
                                    .onChange(of: viewModel.targetId) { _, _ in
                                        viewModel.onTargetNodeChanged()
                                    }

                                    qrScanButton()
                                }
                            }
                        }

                        // Target terminal picker
                        DropdownTerminalPicker(
                            node: viewModel.targetNode,
                            side: .target,
                            selectedTerminalId: $viewModel.targetTerminalId
                        )
                        .onChange(of: viewModel.targetTerminalId) { _, _ in
                            viewModel.revalidate()
                        }
                    }
                    .padding(.vertical, 8)
                }

                // Edge class picker (optional)
                Section(AppStrings.Connections.connectionType) {
                    DropdownEdgeClassPicker(
                        selectedEdgeClassId: $viewModel.selectedEdgeClassId,
                        edgeClasses: viewModel.availableEdgeClasses,
                        placeholder: AppStrings.Common.none
                    )
                    .onChange(of: viewModel.selectedEdgeClassId) { oldId, newId in
                        viewModel.handleEdgeClassChange(from: oldId, to: newId)
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

                // Validation message
                if viewModel.sourceId == viewModel.targetId {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            Text(AppStrings.AssetsExtra.sameNodeValidation)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                // Duplicate connection warning
                if viewModel.isDuplicateConnection {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(.red)
                            Text(AppStrings.AssetsExtra.duplicateConnectionExists)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                // Terminal validation messages
                if viewModel.isMissingSourceTerminal {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            Text(viewModel.sourceNode?.sourceEligibleTerminals.isEmpty == true
                                 ? AppStrings.Connections.noSourceTerminals
                                 : AppStrings.Connections.pleaseSelectSourceTerminal)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                if viewModel.isMissingTargetTerminal {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            Text(viewModel.targetNode?.targetEligibleTerminals.isEmpty == true
                                 ? AppStrings.Connections.noTargetTerminals
                                 : AppStrings.Connections.pleaseSelectTargetTerminal)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                // Error message
                if let error = viewModel.errorMessage {
                    Section {
                        HStack {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.newConnection)
            .navigationBarTitleDisplayMode(.inline)
            .fullScreenCover(isPresented: $showQRScanner) {
                QRCodeScannerView(scannedCode: .constant("")) { code in
                    handleQRScan(code)
                }
            }
            .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
                ForEach(duplicateQRNodes.prefix(5), id: \.id) { node in
                    Button("\(node.label) (\(node.type))") {
                        applyScanResult(node)
                    }
                }
                Button(AppStrings.Common.cancel, role: .cancel) {
                    duplicateQRNodes = []
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
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        viewModel.createEdge(onCreated: onCreated)
                    }
                    .fontWeight(.semibold)
                    .disabled(!viewModel.canCreate || viewModel.isCreating)
                }
            }
            .disabled(viewModel.isCreating)
            .overlay {
                if viewModel.isCreating {
                    ProgressView(AppStrings.AssetsExtra.creating)
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 10)
                }
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
            AppLogger.log(.debug, "[CreateEdgeFromNodeView] onAppear — edgeClasses: \(viewModel.availableEdgeClasses.count), currentSelection: \(viewModel.selectedEdgeClassId?.uuidString.prefix(8) ?? "nil")", category: .ui)
        }
    }

    @ViewBuilder
    private func qrScanButton() -> some View {
        Button {
            showQRScanner = true
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

    private func handleQRScan(_ code: String) {
        let matches = viewModel.findExternalNodes(byQRCode: code)
        if matches.count == 1 {
            applyScanResult(matches[0])
        } else if matches.count > 1 {
            duplicateQRNodes = matches
            showDuplicateQRAlert = true
        } else {
            notFoundQRCode = code
            showAssetNotFound = true
        }
    }

    private func applyScanResult(_ node: NodeV2) {
        switch viewModel.connectionType {
        case .lineside:
            viewModel.sourceId = node.id
        case .loadside:
            viewModel.targetId = node.id
        }
        duplicateQRNodes = []
    }
}

// MARK: - Dropdown Node Picker (Bottom Sheet style)

/// A node picker that opens a bottom sheet with search functionality
struct DropdownNodePicker: View {
    @Binding var selectedNodeId: UUID?
    let placeholder: String

    @State private var isSheetPresented = false
    private let nodeItems: [NodePickerSheetViewModel.NodeItem]

    init(
        selectedNodeId: Binding<UUID?>,
        nodes: [CreateEdgeFromNodeViewModel.NodeOption],
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
            DropdownNodePickerSheet(
                selectedNodeId: $selectedNodeId,
                nodes: nodeItems,
                placeholder: placeholder,
                isPresented: $isSheetPresented
            )
            .presentationDetents([.large])
        }
    }
}

// MARK: - Dropdown Node Picker Bottom Sheet

private struct DropdownNodePickerSheet: View {
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
