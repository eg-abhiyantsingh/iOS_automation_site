//
//  AddConnectionManager.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftUI

@MainActor
class AddConnectionManager: ObservableObject {
    // MARK: - Published Properties
    @Published var sourceId: UUID? = nil
    @Published var targetId: UUID? = nil
    @Published var sourceTerminalId: UUID?
    @Published var targetTerminalId: UUID?
    @Published var selectedEdgeClassId: UUID?
    @Published var searchText = ""
    @Published var draftCoreAttributes: [UUID: String] = [:]
    @Published var showOnlyRequired: Bool = true
    
    // MARK: - Properties
    let availableNodes: [NodeV2]
    let availableEdgeClasses: [EdgeClass]
    let diagram: SLDV2
    
    // MARK: - Computed Properties
    var nodeOptions: [(id: UUID, displayLabel: String, subtitle: String?, qrCode: String?)] {
        availableNodes.map { node in
            (
                id: node.id,
                displayLabel: node.label,
                subtitle: node.location ?? node.type,
                qrCode: (node.qr_code?.isEmpty == false) ? node.qr_code : nil
            )
        }
    }
    
    var sourceNodeLabel: String {
        guard let sourceId else { return AppStrings.Connections.notAssigned }
        return availableNodes.first(where: { $0.id == sourceId })?.label ?? AppStrings.Connections.missingNode
    }

    var targetNodeLabel: String {
        guard let targetId else { return AppStrings.Connections.notAssigned }
        return availableNodes.first(where: { $0.id == targetId })?.label ?? AppStrings.Connections.missingNode
    }

    /// Get the source node object
    var sourceNode: NodeV2? {
        guard let sourceId else { return nil }
        return availableNodes.first(where: { $0.id == sourceId })
    }

    /// Get the target node object
    var targetNode: NodeV2? {
        guard let targetId else { return nil }
        return availableNodes.first(where: { $0.id == targetId })
    }
    
    var selectedEdgeClass: EdgeClass? {
        guard let id = selectedEdgeClassId else { return nil }
        return availableEdgeClasses.first(where: { $0.id == id })
    }

    var selectedEdgeClassName: String {
        availableEdgeClasses.first(where: { $0.id == selectedEdgeClassId })?.name ?? "Select type"
    }
    
    var isSelfConnection: Bool {
        guard let s = sourceId, let t = targetId else { return false }
        return s == t && availableNodes.contains(where: { $0.id == s })
    }

    var isParentChildConnection: Bool {
        guard let sourceId, let targetId,
              let sourceNode = availableNodes.first(where: { $0.id == sourceId }),
              let targetNode = availableNodes.first(where: { $0.id == targetId }) else {
            return false
        }
        return targetNode.parent_id == sourceId || sourceNode.parent_id == targetId
    }

    var hasValidSource: Bool {
        guard let sourceId else { return false }
        return availableNodes.contains(where: { $0.id == sourceId })
    }

    var hasValidTarget: Bool {
        guard let targetId else { return false }
        return availableNodes.contains(where: { $0.id == targetId })
    }

    var hasValidEdgeClass: Bool {
        selectedEdgeClassId != nil &&
        availableEdgeClasses.contains(where: { $0.id == selectedEdgeClassId })
    }
    
    var isMissingSourceTerminal: Bool {
        guard let node = sourceNode else { return false }
        let allTerminals = node.node_terminals.filter { !$0.is_deleted }
        if allTerminals.isEmpty { return false }
        return node.sourceEligibleTerminals.isEmpty || sourceTerminalId == nil
    }

    var isMissingTargetTerminal: Bool {
        guard let node = targetNode else { return false }
        let allTerminals = node.node_terminals.filter { !$0.is_deleted }
        if allTerminals.isEmpty { return false }
        return node.targetEligibleTerminals.isEmpty || targetTerminalId == nil
    }

    var canSave: Bool {
        let hasAtLeastOneField = hasValidSource || hasValidTarget || hasValidEdgeClass
        let sourceOk = sourceId == nil || !isMissingSourceTerminal
        let targetOk = targetId == nil || !isMissingTargetTerminal
        return hasAtLeastOneField &&
            sourceOk && targetOk &&
            !isSelfConnection &&
            !isParentChildConnection &&
            !isDuplicateConnection
    }

    var isDuplicateConnection: Bool {
        guard let s = sourceId, let t = targetId else { return false }
        return diagram.edges.contains { edge in
            !edge.is_deleted &&
            edge.source == s &&
            edge.target == t
        }
    }
    
    var validationMessage: String? {
        if isSelfConnection {
            return AppStrings.Connections.sourceTargetMustDiffer
        }
        if isParentChildConnection {
            return AppStrings.Connections.parentCannotConnectChild
        }
        if isDuplicateConnection {
            return AppStrings.Connections.duplicateConnectionExists
        }
        if let node = sourceNode, !node.node_terminals.filter({ !$0.is_deleted }).isEmpty,
           node.sourceEligibleTerminals.isEmpty {
            return AppStrings.Connections.noSourceTerminals
        }
        if let node = targetNode, !node.node_terminals.filter({ !$0.is_deleted }).isEmpty,
           node.targetEligibleTerminals.isEmpty {
            return AppStrings.Connections.noTargetTerminals
        }
        if isMissingSourceTerminal {
            return AppStrings.Connections.pleaseSelectSourceTerminal
        }
        if isMissingTargetTerminal {
            return AppStrings.Connections.pleaseSelectTargetTerminal
        }
        return nil
    }
    
    var connectionSummary: String? {
        let sourceLabel = sourceNodeLabel
        let targetLabel = targetNodeLabel
        let edgeClassName = selectedEdgeClassName

        // Show summary when at least one meaningful field is set
        guard sourceId != nil || targetId != nil || selectedEdgeClassId != nil else { return nil }

        if let selectedEdgeClassId, availableEdgeClasses.contains(where: { $0.id == selectedEdgeClassId }) {
            return "\(sourceLabel) → \(targetLabel) (\(edgeClassName))"
        }
        return "\(sourceLabel) → \(targetLabel)"
    }
    
    // MARK: - Initialization
    init(availableNodes: [NodeV2], availableEdgeClasses: [EdgeClass], diagram: SLDV2) {
        self.availableNodes = availableNodes
        self.availableEdgeClasses = availableEdgeClasses
        self.diagram = diagram
    }
    
    // MARK: - QR Code Lookup

    func findNodes(byQRCode code: String) -> [NodeV2] {
        let needle = code.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !needle.isEmpty else { return [] }
        return availableNodes.filter { node in
            !node.is_deleted && node.qr_code?.lowercased() == needle
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
    }

    func onTargetNodeChanged() {
        targetTerminalId = nil
        if let node = targetNode {
            let eligible = node.targetEligibleTerminals
            if eligible.count == 1 {
                targetTerminalId = eligible[0].id
            }
        }
    }

    // MARK: - Edge Class Change Handling

    func handleEdgeClassChange(from oldId: UUID?, to newId: UUID?) {
        let oldClass = oldId.flatMap { id in availableEdgeClasses.first { $0.id == id } }
        guard let newId = newId,
              let newClass = availableEdgeClasses.first(where: { $0.id == newId }) else {
            draftCoreAttributes = [:]
            return
        }
        draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
            from: oldClass,
            to: newClass,
            currentAttributes: draftCoreAttributes
        )
        // Apply class-defined defaults (ZP-2251) to any fields still empty
        // after preservation.
        CoreAttributesService.applyDefaultValues(from: newClass, into: &draftCoreAttributes)
    }

    // MARK: - Core Attributes Builder

    private func buildCoreAttributes(edgeClass: EdgeClass) -> [EdgeProperty] {
        edgeClass.definition.compactMap { classProperty in
            guard let value = draftCoreAttributes[classProperty.id], !value.isEmpty else { return nil }
            return EdgeProperty(
                id: classProperty.id,
                edge_class_property: classProperty,
                name: classProperty.name,
                value: value
            )
        }
    }

    // MARK: - Methods
    func prepareEdge() -> EdgeV2? {
        guard canSave else { return nil }

        let edgeClass = selectedEdgeClass
        let coreAttributes = edgeClass.map { buildCoreAttributes(edgeClass: $0) } ?? []

        let edge = EdgeV2(
            id: UUID(),
            source: sourceId,
            target: targetId,
            sld: nil,
            is_deleted: false,
            sourceNodeTerminalId: sourceTerminalId,
            targetNodeTerminalId: targetTerminalId,
            edge_class: edgeClass,
            core_attributes: coreAttributes
        )

        // Set sync flags for offline sync support
        edge.needsSync = true
        edge.lastSyncedAt = nil

        return edge
    }

    func reset() {
        sourceId = nil
        targetId = nil
        sourceTerminalId = nil
        targetTerminalId = nil
        selectedEdgeClassId = nil
        searchText = ""
        draftCoreAttributes = [:]
        showOnlyRequired = true
    }
}
