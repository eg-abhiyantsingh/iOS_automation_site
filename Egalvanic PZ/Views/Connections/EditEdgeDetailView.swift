//
//  EditEdgeDetailViewV3.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/5/25.
//

import SwiftUI
import SwiftData

struct EditEdgeDetailViewV3: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var sldService: SLDService

    let edge: EdgeV2
    /// Optional context node - when editing from a node detail view, this restricts source/target options
    let contextNode: NodeV2?
    /// Optional callback when save starts - allows parent to show loading UI
    let onSaveStarted: (() -> Void)?
    /// Optional callback when save completes - allows parent to hide loading UI
    let onSaveCompleted: (() -> Void)?

    @State private var isSaving = false
    @State private var apiError: Error?
    @State private var showingDiscardChangesAlert = false

    // PERFORMANCE: Removed @Query for currentNodes/currentEdges which caused re-renders on any change
    // Use edge.sld.nodes and edge.sld.edges directly instead

    private let api = APIClient.shared

    // Query for all edge classes
    @Query private var allEdgeClasses: [EdgeClass]

    // Original values
    private let originalSourceId: UUID?
    private let originalTargetId: UUID?
    private let originalSourceTerminal: UUID?
    private let originalTargetTerminal: UUID?
    private let originalCoreAttributes: [UUID: String]
    private let originalEdgeClass: EdgeClass?

    // Draft states
    @State private var selectedSourceId: UUID?
    @State private var selectedTargetId: UUID?
    @State private var selectedSourceTerminal: UUID?
    @State private var selectedTargetTerminal: UUID?
    @State private var draftCoreAttributes: [UUID: String] = [:]
    @State private var selectedEdgeClass: EdgeClass?

    // Core Attributes state
    @State private var showOnlyRequiredAttributes = true
    @State private var showingSourcePicker = false
    @State private var showingTargetPicker = false

    // PERFORMANCE: Cache source/target nodes — set once in init, updated when picker changes selection
    @State private var sourceNodeObj: NodeV2?
    @State private var targetNodeObj: NodeV2?

    init(edge: EdgeV2, contextNode: NodeV2? = nil, onSaveStarted: (() -> Void)? = nil, onSaveCompleted: (() -> Void)? = nil) {
        self.edge = edge
        self.contextNode = contextNode
        self.onSaveStarted = onSaveStarted
        self.onSaveCompleted = onSaveCompleted
        self.originalSourceId = edge.source
        self.originalTargetId = edge.target
        self.originalSourceTerminal = edge.sourceNodeTerminalId
        self.originalTargetTerminal = edge.targetNodeTerminalId
        self.originalEdgeClass = edge.edge_class

        // Build original attributes dictionary (handle duplicates gracefully)
        var originalAttrs: [UUID: String] = [:]
        for attr in edge.core_attributes {
            originalAttrs[attr.id] = attr.value
        }
        self.originalCoreAttributes = originalAttrs

        self._selectedSourceId = State(initialValue: edge.source)
        self._selectedTargetId = State(initialValue: edge.target)
        self._selectedSourceTerminal = State(initialValue: edge.sourceNodeTerminalId)
        self._selectedTargetTerminal = State(initialValue: edge.targetNodeTerminalId)
        self._draftCoreAttributes = State(initialValue: originalAttrs)
        self._selectedEdgeClass = State(initialValue: edge.edge_class)

        // Cache source/target node objects once — no SLD traversal needed during body
        let sourceNode = edge.source.flatMap { srcId in edge.sld?.nodes.first { $0.id == srcId && !$0.is_deleted } }
        let targetNode = edge.target.flatMap { tgtId in edge.sld?.nodes.first { $0.id == tgtId && !$0.is_deleted } }
        self._sourceNodeObj = State(initialValue: sourceNode)
        self._targetNodeObj = State(initialValue: targetNode)
    }
    
    // Computed property to check if there are changes
    private var hasChanges: Bool {
        selectedSourceId != originalSourceId ||
        selectedTargetId != originalTargetId ||
        selectedSourceTerminal != originalSourceTerminal ||
        selectedTargetTerminal != originalTargetTerminal ||
        draftCoreAttributes != originalCoreAttributes ||
        selectedEdgeClass?.id != originalEdgeClass?.id
    }

    private var isSameNode: Bool {
        guard let s = selectedSourceId, let t = selectedTargetId else { return false }
        return s == t
    }

    private var isDuplicateConnection: Bool {
        guard let s = selectedSourceId, let t = selectedTargetId,
              let edges = edge.sld?.edges else { return false }
        return edges.contains { e in
            e.id != edge.id &&
            !e.is_deleted &&
            e.source == s &&
            e.target == t
        }
    }

    /// True when the source node has terminals but none are LOAD-side (source-eligible)
    private var hasNoSourceTerminals: Bool {
        guard let source = sourceNodeObj else { return false }
        let allTerminals = source.node_terminals.filter { !$0.is_deleted }
        return !allTerminals.isEmpty && source.sourceEligibleTerminals.isEmpty
    }

    /// True when the target node has terminals but none are LINE-side (target-eligible)
    private var hasNoTargetTerminals: Bool {
        guard let target = targetNodeObj else { return false }
        let allTerminals = target.node_terminals.filter { !$0.is_deleted }
        return !allTerminals.isEmpty && target.targetEligibleTerminals.isEmpty
    }

    /// True when source has LOAD terminals but none is selected
    private var isMissingSourceTerminal: Bool {
        guard let source = sourceNodeObj else { return false }
        return !source.sourceEligibleTerminals.isEmpty && selectedSourceTerminal == nil
    }

    /// True when target has LINE terminals but none is selected
    private var isMissingTargetTerminal: Bool {
        guard let target = targetNodeObj else { return false }
        return !target.targetEligibleTerminals.isEmpty && selectedTargetTerminal == nil
    }

    private var canSwap: Bool {
        if selectedSourceId == nil && selectedTargetId == nil { return false }
        if isSameNode || isSaving { return false }
        // After swap, current target becomes source (needs LOAD) and current source becomes target (needs LINE)
        if let target = targetNodeObj,
           !target.node_terminals.filter({ !$0.is_deleted }).isEmpty,
           target.sourceEligibleTerminals.isEmpty {
            return false
        }
        if let source = sourceNodeObj,
           !source.node_terminals.filter({ !$0.is_deleted }).isEmpty,
           source.targetEligibleTerminals.isEmpty {
            return false
        }
        return true
    }

    private var canSave: Bool {
        hasChanges && !isSameNode && !isDuplicateConnection &&
        !isMissingSourceTerminal && !isMissingTargetTerminal &&
        !hasNoSourceTerminals && !hasNoTargetTerminals
    }

    /// Edge label from cached source/target nodes — three-state display
    private var edgeLabelText: String {
        let sourceLabel: String
        if let _ = sourceNodeObj {
            sourceLabel = sourceNodeObj!.label
        } else if selectedSourceId != nil {
            sourceLabel = AppStrings.Connections.missingNode
        } else {
            sourceLabel = AppStrings.Connections.notAssigned
        }
        let targetLabel: String
        if let _ = targetNodeObj {
            targetLabel = targetNodeObj!.label
        } else if selectedTargetId != nil {
            targetLabel = AppStrings.Connections.missingNode
        } else {
            targetLabel = AppStrings.Connections.notAssigned
        }
        return "\(sourceLabel) → \(targetLabel)"
    }

    /// Compute eligible nodes for a picker — only called when picker opens (inside fullScreenCover)
    private func pickerNodes(forSide side: TerminalPicker.TerminalSide) -> [NodeV2] {
        let sldNodes = (edge.sld?.nodes ?? []).filter { !$0.is_deleted }
        guard let contextNode = contextNode else { return sldNodes }
        let contextIds = Set([contextNode.id] + sldNodes.filter { $0.parent_id == contextNode.id }.map { $0.id })
        let edgeNodeId = side == .source ? edge.source : edge.target
        if let edgeNodeId, contextIds.contains(edgeNodeId) {
            return sldNodes.filter { contextIds.contains($0.id) }
        }
        return sldNodes
    }
    
    // Sorted edge classes for the picker
    private var sortedEdgeClasses: [EdgeClass] {
        allEdgeClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                ScrollView {
                    VStack(spacing: 0) {
                        // Profile Section
                        EdgeHeaderSection(
                            edgeLabel: edgeLabelText,
                            edgeType: selectedEdgeClass?.name ?? AppStrings.Connections.connection,
                            networkMode: networkState.mode
                        )

                        // Main Content
                        VStack(spacing: 20) {
                            // Basic Information Card
                            VStack(spacing: 16) {
                                HStack {
                                    Label(AppStrings.AssetsExtra.connectionDetails, systemImage: "link")
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    Button(action: swapSourceAndTarget) {
                                        Image(systemName: "arrow.up.arrow.down")
                                            .font(.system(size: 14))
                                            .foregroundColor(canSwap ? .blue : .gray.opacity(0.5))
                                            .padding(8)
                                            .background(
                                                Circle()
                                                    .fill(canSwap ? Color.blue.opacity(0.1) : Color.gray.opacity(0.08))
                                            )
                                    }
                                    .disabled(!canSwap)
                                    .accessibilityLabel(AppStrings.Connections.swapSourceAndTarget)
                                }

                                VStack(spacing: 12) {
                                    // Source node row
                                    ConnectionNodeRow(
                                        label: AppStrings.AssetsExtra.sourceNode,
                                        node: sourceNodeObj,
                                        nodeId: selectedSourceId,
                                        onTap: { showingSourcePicker = true }
                                    )

                                    // Source terminal picker
                                    TerminalDropdownPicker(
                                        node: sourceNodeObj,
                                        side: .source,
                                        selectedTerminalId: $selectedSourceTerminal
                                    )

                                    // Visual connector
                                    HStack {
                                        Spacer()
                                        Image(systemName: "arrow.down")
                                            .font(.title3)
                                            .foregroundColor(.blue)
                                        Spacer()
                                    }
                                    .padding(.vertical, 4)

                                    // Target node row
                                    ConnectionNodeRow(
                                        label: AppStrings.AssetsExtra.targetNode,
                                        node: targetNodeObj,
                                        nodeId: selectedTargetId,
                                        onTap: { showingTargetPicker = true }
                                    )

                                    // Target terminal picker
                                    TerminalDropdownPicker(
                                        node: targetNodeObj,
                                        side: .target,
                                        selectedTerminalId: $selectedTargetTerminal
                                    )

                                    // Edge Class Picker
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(AppStrings.Connections.connectionClass)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        
                                        Menu {
                                            // Option to clear selection
                                            Button(action: {
                                                selectedEdgeClass = nil
                                                draftCoreAttributes = [:]
                                            }) {
                                                HStack {
                                                    Text(AppStrings.Common.none)
                                                    if selectedEdgeClass == nil {
                                                        Spacer()
                                                        Image(systemName: "checkmark")
                                                    }
                                                }
                                            }
                                            
                                            Divider()
                                            
                                            // Edge class options
                                            ForEach(sortedEdgeClasses, id: \.id) { edgeClass in
                                                Button(action: {
                                                    handleEdgeClassChange(to: edgeClass)
                                                }) {
                                                    HStack {
                                                        Text(edgeClass.name)
                                                        if selectedEdgeClass?.id == edgeClass.id {
                                                            Spacer()
                                                            Image(systemName: "checkmark")
                                                        }
                                                    }
                                                }
                                            }
                                        } label: {
                                            HStack {
                                                Image(systemName: "link.circle")
                                                    .foregroundColor(.secondary)
                                                    .frame(width: 20)
                                                
                                                Text(selectedEdgeClass?.name ?? AppStrings.Connections.selectConnectionClass)
                                                    .foregroundColor(selectedEdgeClass != nil ? .primary : .secondary)
                                                
                                                Spacer()
                                                
                                                Image(systemName: "chevron.down")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 12)
                                            .background(Color(.systemGray6))
                                            .cornerRadius(10)
                                        }
                                    }
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)

                            // Validation warnings
                            if isSameNode {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(.orange)
                                    Text(AppStrings.AssetsExtra.sameNodeValidation)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            if isDuplicateConnection {
                                HStack {
                                    Image(systemName: "exclamationmark.circle.fill")
                                        .foregroundColor(.red)
                                    Text(AppStrings.AssetsExtra.duplicateConnectionExists)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            if hasNoSourceTerminals {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(.orange)
                                    Text(AppStrings.Connections.noSourceTerminals)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            if hasNoTargetTerminals {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(.orange)
                                    Text(AppStrings.Connections.noTargetTerminals)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            if isMissingSourceTerminal {
                                HStack {
                                    Image(systemName: "exclamationmark.circle.fill")
                                        .foregroundColor(.orange)
                                    Text(AppStrings.Connections.pleaseSelectSourceTerminal)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            if isMissingTargetTerminal {
                                HStack {
                                    Image(systemName: "exclamationmark.circle.fill")
                                        .foregroundColor(.orange)
                                    Text(AppStrings.Connections.pleaseSelectTargetTerminal)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            // Core Attributes Section
                            if selectedEdgeClass != nil {
                                EdgeCoreAttributesSection(
                                    edge: edge,
                                    selectedEdgeClass: selectedEdgeClass,
                                    draftAttributes: $draftCoreAttributes
                                )
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }
                        }
                        .padding()
                        .padding(.bottom, hasChanges ? 80 : 20)
                    }
                }
                .scrollDismissesKeyboard(.interactively)
                .background(Color(.systemGray6))
                .onTapGesture {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                }

                // Bottom Action Bar
                if hasChanges {
                    VStack {
                        Spacer()
                        
                        VStack(spacing: 0) {
                            Rectangle()
                                .fill(Color(.systemBackground))
                                .frame(height: 0)
                                .shadow(color: .black.opacity(0.1), radius: 10, y: -5)
                            
                            Button(action: saveAndSync) {
                                if isSaving {
                                    HStack {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.8)
                                        Text(AppStrings.AssetsExtra.saving)
                                            .foregroundColor(.white)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(Color.blue.opacity(0.6))
                                    .cornerRadius(12)
                                } else {
                                    Text(AppStrings.AssetsExtra.saveChanges)
                                        .fontWeight(.semibold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 16)
                                        .background(canSave ? Color.blue : Color.blue.opacity(0.4))
                                        .foregroundColor(.white)
                                        .cornerRadius(12)
                                }
                            }
                            .disabled(isSaving || !canSave)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                            .padding(.bottom, 18)
                            .background(Color(.systemBackground))
                        }
                    }
                    .ignoresSafeArea()
                }

                // Loading overlay for save operations
                if isSaving {
                    Color.black.opacity(0.5)
                        .ignoresSafeArea()
                        .overlay(
                            VStack(spacing: 16) {
                                ProgressView()
                                    .scaleEffect(1.8)
                                    .tint(.blue)
                                Text(AppStrings.AssetsExtra.saving)
                                    .font(.headline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.primary)
                            }
                            .padding(32)
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.2), radius: 10, y: 5)
                        )
                        .transition(.opacity)
                        .animation(.easeInOut(duration: 0.2), value: isSaving)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(hasChanges)
            .navigationTitle(AppStrings.Connections.edgeDetails)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if hasChanges {
                            showingDiscardChangesAlert = true
                        } else {
                            dismiss()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 16, weight: .medium))
                            Text(AppStrings.Common.back)
                        }
                    }
                }
            }
        }
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { apiError != nil },
            set: { if !$0 { apiError = nil } }
        )) {
            Button(AppStrings.Common.ok) { apiError = nil }
        } message: {
            Text(apiError?.localizedDescription ?? AppStrings.AssetsExtra.unknownError)
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showingDiscardChangesAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                dismiss()
            }
        } message: {
            Text(AppStrings.Alerts.discardChangesMessage)
        }
        .fullScreenCover(isPresented: $showingSourcePicker) {
            ConnectionNodePickerView(
                title: AppStrings.AssetsExtra.sourceNode,
                nodes: pickerNodes(forSide: .source),
                sld: edge.sld!,
                selectedNodeId: $selectedSourceId,
                onDismiss: {
                    showingSourcePicker = false
                    // Update cached source node
                    sourceNodeObj = selectedSourceId.flatMap { srcId in edge.sld?.nodes.first { $0.id == srcId && !$0.is_deleted } }
                }
            )
        }
        .fullScreenCover(isPresented: $showingTargetPicker) {
            ConnectionNodePickerView(
                title: AppStrings.AssetsExtra.targetNode,
                nodes: pickerNodes(forSide: .target),
                sld: edge.sld!,
                selectedNodeId: $selectedTargetId,
                onDismiss: {
                    showingTargetPicker = false
                    // Update cached target node
                    targetNodeObj = selectedTargetId.flatMap { tgtId in edge.sld?.nodes.first { $0.id == tgtId && !$0.is_deleted } }
                }
            )
        }
    }
    
    private func handleEdgeClassChange(to newEdgeClass: EdgeClass) {
        // Use the CoreAttributesService to preserve attributes
        draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
            from: selectedEdgeClass,
            to: newEdgeClass,
            currentAttributes: draftCoreAttributes
        )

        selectedEdgeClass = newEdgeClass
    }

    private func swapSourceAndTarget() {
        let oldSourceId = selectedSourceId
        let oldTargetId = selectedTargetId
        let oldSourceNode = sourceNodeObj
        let oldTargetNode = targetNodeObj

        // Swap node IDs and cached objects
        selectedSourceId = oldTargetId
        selectedTargetId = oldSourceId
        sourceNodeObj = oldTargetNode
        targetNodeObj = oldSourceNode

        // Terminal logic: if swapping back to original positions, restore originals
        if selectedSourceId == originalSourceId && selectedTargetId == originalTargetId {
            selectedSourceTerminal = originalSourceTerminal
            selectedTargetTerminal = originalTargetTerminal
        } else {
            // Auto-select terminal for new source (needs LOAD)
            if let newSource = sourceNodeObj {
                let eligible = newSource.sourceEligibleTerminals
                selectedSourceTerminal = eligible.count == 1 ? eligible[0].id : nil
            } else {
                selectedSourceTerminal = nil
            }

            // Auto-select terminal for new target (needs LINE)
            if let newTarget = targetNodeObj {
                let eligible = newTarget.targetEligibleTerminals
                selectedTargetTerminal = eligible.count == 1 ? eligible[0].id : nil
            } else {
                selectedTargetTerminal = nil
            }
        }
    }

    private func saveAndSync() {
        Task {
            await performSave()
        }
    }
    
    private func performSave() async {
        isSaving = true

        // Notify parent that save is starting (allows showing loading overlay)
        onSaveStarted?()

        // Apply changes to the model
        edge.source = selectedSourceId
        edge.target = selectedTargetId
        edge.sourceNodeTerminalId = selectedSourceTerminal
        edge.targetNodeTerminalId = selectedTargetTerminal
        // Update handle codes to match the selected terminals (sent to server in API payload)
        edge.sourceHandle = sourceNodeObj?.node_terminals.first(where: { $0.id == selectedSourceTerminal })?.handle_code
        edge.targetHandle = targetNodeObj?.node_terminals.first(where: { $0.id == selectedTargetTerminal })?.handle_code
        edge.edge_class = selectedEdgeClass

        // Apply core attribute changes
        applyCoreAttributeChanges()

        // Dismiss the sheet FIRST before triggering heavy operations
        // This provides better UX - user sees the sheet close immediately
        await MainActor.run {
            dismiss()
        }

        // Small delay to let the dismiss animation complete
        try? await Task.sleep(nanoseconds: 100_000_000) // 100ms

        // Now do the heavy work (WebView update and server sync) AFTER dismiss
        await MainActor.run {
            // Update the graph - use edge.sld's nodes/edges directly instead of @Query
            let sldNodes = edge.sld?.nodes ?? []
            let sldEdges = edge.sld?.edges ?? []
            let dto = sldService.createDTO(forSLDId: edge.sld?.id ?? UUID(), nodes: sldNodes, edges: sldEdges, customName: "updated-bridge-edge")
            WebViewBridge.updateGraph(with: dto, animated: true)

            // Use EdgeService to handle the save
            EdgeService.updateEdge(
                edge,
                modelContext: context
            ) { success, message in
                // Notify parent that save is complete
                onSaveCompleted?()
                isSaving = false

                if success {
                    if let errorMessage = message {
                        AppLogger.log(.info, errorMessage, category: .node)
                    }
                } else {
                    AppLogger.log(.error, "Edge update failed: \(message ?? "Unknown error")", category: .node)
                }
            }
        }
    }
    
    private func applyCoreAttributeChanges() {
        // Use the CoreAttributesService for core attributes management
        CoreAttributesService.applyCoreAttributeChanges(
            to: edge,
            selectedClass: selectedEdgeClass,
            originalClass: originalEdgeClass,
            draftAttributes: draftCoreAttributes,
            modelContext: context
        )
    }
    
}

// Edge Header Section
struct EdgeHeaderSection: View {
    let edgeLabel: String
    let edgeType: String
    let networkMode: NetworkMode
    
    var body: some View {
        VStack(spacing: 16) {
            // Edge Icon
            ZStack {
                Circle()
                    .fill(LinearGradient(
                        colors: [Color.blue.opacity(0.2), Color.blue.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .frame(width: 100, height: 100)
                
                Image(systemName: "link.circle.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.blue)
            }
            .shadow(color: .blue.opacity(0.3), radius: 10, y: 5)
            
            // Edge Info
            VStack(spacing: 4) {
                Text(edgeLabel)
                    .font(.title3)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text(edgeType)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                if networkMode == .offline {
                    Label(AppStrings.CommonExtra.offline, systemImage: "wifi.slash")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
        }
        .padding(.top, 20)
        .padding(.bottom, 30)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.1), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}

// Keep the existing SearchableNodePicker components from the original file
struct SearchableNodePicker2: View {
    @Binding var selectedNodeId: UUID
    let nodes: [(id: UUID, displayLabel: String)]
    let placeholder: String
    
    @State private var searchText = ""
    @State private var isExpanded = false
    
    private var selectedNodeLabel: String {
        nodes.first { $0.id == selectedNodeId }?.displayLabel ?? placeholder
    }
    
    private var filteredNodes: [(id: UUID, displayLabel: String)] {
        if searchText.isEmpty {
            return nodes
        } else {
            return nodes.filter { $0.displayLabel.localizedCaseInsensitiveContains(searchText) }
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Main picker button
            Button(action: {
                isExpanded.toggle()
                if isExpanded {
                    searchText = ""
                }
            }) {
                HStack {
                    Text(selectedNodeLabel)
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
            .buttonStyle(PlainButtonStyle())
            
            // Expandable search and options
            if isExpanded {
                VStack(spacing: 0) {
                    // Search field
                    TextField(AppStrings.Tasks.searchNodes, text: $searchText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .padding(.top, 8)
                    
                    // Options list
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 0) {
                            ForEach(filteredNodes, id: \.id) { node in
                                Button(action: {
                                    selectedNodeId = node.id
                                    isExpanded = false
                                    searchText = ""
                                }) {
                                    HStack {
                                        Text(node.displayLabel)
                                            .foregroundColor(.primary)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        if node.id == selectedNodeId {
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                                .font(.caption)
                                        }
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 12)
                                    .background(node.id == selectedNodeId ? Color.blue.opacity(0.1) : Color.clear)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(PlainButtonStyle())
                                
                                if node.id != filteredNodes.last?.id {
                                    Divider()
                                        .padding(.horizontal, 12)
                                }
                            }
                        }
                    }
                    .frame(maxHeight: 200)
                    .background(Color(.systemBackground))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray4), lineWidth: 1)
                    )
                    .padding(.top, 4)
                }
            }
        }
        .onTapGesture {
            // Close dropdown when tapping outside
            if isExpanded {
                isExpanded = false
                searchText = ""
            }
        }
    }
}

// MARK: - Terminal Dropdown Picker

/// Dropdown-style terminal picker matching the style of other pickers in EditEdgeDetailView
struct TerminalDropdownPicker: View {
    let node: NodeV2?
    let side: TerminalPicker.TerminalSide
    @Binding var selectedTerminalId: UUID?

    @State private var isExpanded = false

    /// Filtered terminals for this side
    private var availableTerminals: [NodeTerminal] {
        guard let node = node else { return [] }
        return node.node_terminals.filter { terminal in
            !terminal.is_deleted && terminal.side == side.terminalFilter
        }.sorted { ($0.label ?? "") < ($1.label ?? "") }
    }

    /// Selected terminal object
    private var selectedTerminal: NodeTerminal? {
        guard let selectedId = selectedTerminalId else { return nil }
        // First check in filtered terminals
        if let terminal = availableTerminals.first(where: { $0.id == selectedId }) {
            return terminal
        }
        // Fallback: check all terminals (handles edge cases where terminal side doesn't match)
        return node?.node_terminals.first { $0.id == selectedId && !$0.is_deleted }
    }

    /// Display label for selected terminal
    private var selectedLabel: String {
        selectedTerminal?.label ?? AppStrings.Connections.selectTerminal
    }

    /// Whether a terminal is selected
    private var hasSelection: Bool {
        selectedTerminal != nil
    }

    var body: some View {
        // Don't show if no terminals available
        if availableTerminals.isEmpty {
            EmptyView()
        } else if availableTerminals.count == 1 {
            // Single terminal - show as read-only, auto-select
            let terminal = availableTerminals[0]
            VStack(alignment: .leading, spacing: 8) {
                Text(side.label)
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack {
                    Text(terminal.label ?? AppStrings.Connections.terminal)
                        .foregroundColor(.primary)
                    Spacer()
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                        .font(.caption)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            .onAppear {
                if selectedTerminalId == nil {
                    selectedTerminalId = terminal.id
                }
            }
        } else {
            // Multiple terminals - show dropdown
            VStack(alignment: .leading, spacing: 8) {
                Text(side.label)
                    .font(.caption)
                    .foregroundColor(.secondary)

                VStack(alignment: .leading, spacing: 0) {
                    // Dropdown button
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            isExpanded.toggle()
                        }
                    }) {
                        HStack {
                            Text(selectedLabel)
                                .foregroundColor(hasSelection ? .primary : .secondary)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                                .foregroundColor(.secondary)
                                .font(.caption)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                    }
                    .buttonStyle(PlainButtonStyle())

                    // Expandable options
                    if isExpanded {
                        VStack(spacing: 0) {
                            ForEach(availableTerminals, id: \.id) { terminal in
                                Button(action: {
                                    selectedTerminalId = terminal.id
                                    withAnimation(.easeInOut(duration: 0.2)) {
                                        isExpanded = false
                                    }
                                }) {
                                    HStack {
                                        Text(terminal.label ?? AppStrings.Connections.terminal)
                                            .foregroundColor(.primary)
                                            .frame(maxWidth: .infinity, alignment: .leading)

                                        if terminal.id == selectedTerminalId {
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                                .font(.caption)
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(terminal.id == selectedTerminalId ? Color.blue.opacity(0.1) : Color.clear)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(PlainButtonStyle())

                                if terminal.id != availableTerminals.last?.id {
                                    Divider()
                                        .padding(.horizontal, 16)
                                }
                            }
                        }
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color(.systemGray4), lineWidth: 1)
                        )
                        .padding(.top, 4)
                    }
                }
            }
        }
    }
}

// MARK: - Connection Node Row (tappable summary row)

struct ConnectionNodeRow: View {
    let label: String
    let node: NodeV2?
    let nodeId: UUID?
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            Button(action: onTap) {
                HStack(spacing: 12) {
                    if let node {
                        NodeTypeIconCircle(
                            style: node.node_class?.style,
                            size: 32,
                            iconSize: 16,
                            backgroundColor: Color.blue.opacity(0.1),
                            iconColor: .blue
                        )

                        VStack(alignment: .leading, spacing: 2) {
                            Text(node.label)
                                .font(.body)
                                .fontWeight(.medium)
                                .foregroundColor(.primary)

                            if let className = node.node_class?.name {
                                Text(className)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    } else if nodeId != nil {
                        // Node ID exists but node not found — Missing Node
                        Image(systemName: "exclamationmark.triangle")
                            .foregroundColor(.red)
                        Text(AppStrings.Connections.missingNode)
                            .foregroundColor(.red)
                            .italic()
                    } else {
                        // No node ID — Not Assigned
                        Image(systemName: "minus.circle")
                            .foregroundColor(.secondary)
                        Text(AppStrings.Connections.notAssigned)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
            .buttonStyle(PlainButtonStyle())
        }
    }
}

// MARK: - Connection Node Picker (fullScreenCover)

struct ConnectionNodePickerView: View {
    let title: String
    let nodes: [NodeV2]
    let sld: SLDV2
    @Binding var selectedNodeId: UUID?
    let onDismiss: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    @State private var showingLocationPicker = false
    @State private var filterRoom: Room?

    private var filteredNodes: [NodeV2] {
        var results = nodes

        if let roomId = filterRoom?.id {
            results = results.filter { $0.room?.id == roomId }
        }

        if !searchText.isEmpty {
            results = results.filter { node in
                node.label.localizedCaseInsensitiveContains(searchText) ||
                (node.node_class?.name.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }

        return results.sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField(AppStrings.Connections.searchByLabelOrClass, text: $searchText)
                        .textFieldStyle(PlainTextFieldStyle())

                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }

                    Button(action: { showingLocationPicker = true }) {
                        Image(systemName: "building.2")
                            .foregroundColor(filterRoom != nil ? .blue : .secondary)
                    }
                }
                .padding(8)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(8)
                .padding(.horizontal)
                .padding(.vertical, 8)

                // Active room filter chip
                if let room = filterRoom {
                    HStack(spacing: 6) {
                        Image(systemName: "building.2")
                            .font(.caption2)
                        Text(room.fullPath)
                            .font(.caption)
                        Button(action: { filterRoom = nil }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption2)
                        }
                    }
                    .foregroundColor(.blue)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(16)
                    .padding(.horizontal)
                    .padding(.bottom, 4)
                }

                // Node list
                if filteredNodes.isEmpty {
                    ContentUnavailableView(
                        "No matching assets",
                        systemImage: "cube.box",
                        description: Text(searchText.isEmpty && filterRoom == nil ? "No assets available" : "Try adjusting your search or filter")
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    List {
                        // "Not Assigned" option to clear selection
                        Button(action: {
                            selectedNodeId = nil
                            onDismiss()
                            dismiss()
                        }) {
                            HStack(spacing: 12) {
                                Image(systemName: "minus.circle")
                                    .font(.system(size: 16))
                                    .foregroundColor(.secondary)
                                    .frame(width: 32, height: 32)

                                Text(AppStrings.Connections.notAssigned)
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundColor(.secondary)

                                Spacer()

                                if selectedNodeId == nil {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())

                        ForEach(filteredNodes) { node in
                            Button(action: {
                                selectedNodeId = node.id
                                onDismiss()
                                dismiss()
                            }) {
                                HStack(spacing: 12) {
                                    NodeTypeIconCircle(
                                        style: node.node_class?.style,
                                        size: 32,
                                        iconSize: 16,
                                        backgroundColor: Color.blue.opacity(0.1),
                                        iconColor: .blue
                                    )

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(node.label)
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(.primary)

                                        if let className = node.node_class?.name {
                                            Text(className)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }

                                        if let roomName = node.room?.name {
                                            Text(roomName)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }

                                    Spacer()

                                    if node.id == selectedNodeId {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(.blue)
                                    }
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        onDismiss()
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingLocationPicker) {
                HierarchicalLocationPickerView(
                    sld: sld,
                    selectedRoom: $filterRoom,
                    readOnly: true,
                    onDismiss: { showingLocationPicker = false }
                )
            }
        }
    }
}
