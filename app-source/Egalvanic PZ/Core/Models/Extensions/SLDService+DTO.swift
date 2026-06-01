//
//  SLDService+DTO.swift
//  SwiftDataTutorial
//
//  Extension to centralize SLDDTO creation
//

import SwiftUI
import SwiftData

// ZP-2161: helper to copy the engineering fields off a NodeV2 entity
// onto an SLDDTONode after construction. Used by every createDTO
// overload below — the DTO's primary init() pre-dates these fields and
// we'd rather not bloat its parameter list with another 30+ args.
private extension SLDDTONode {
    mutating func applyEngineering(from node: NodeV2) {
        self.tertiary_voltage = node.tertiary_voltage
        self.tertiary_voltage_id = node.tertiary_voltage_id
        self.system_voltage_id = node.system_voltage_id
        self.circuit_voltage_id = node.circuit_voltage_id
        self.voltage_user_overridden = node.voltage_user_overridden
        self.applied_shortcut = node.applied_shortcut_id
        self.eqp_lib_suggested = node.eqp_lib_suggested
        self.eqp_note = node.eqp_note
        self.eqp_engineering_approved = node.eqp_engineering_approved
        self.skm_lib_name = node.skm_lib_name
        self.skm_lib_name_suggested = node.skm_lib_name_suggested
        self.ocr_signature = node.ocr_signature
        self.kva_rating = node.kva_rating
        self.percent_impedance = node.percent_impedance
        self.mains_type_id = node.mains_type_id
        self.phase_configuration_id = node.phase_configuration_id
        self.ampere_rating = node.ampere_rating
        self.pole_count = node.pole_count
        self.manufacturer_id = node.manufacturer_id
        self.has_trip_unit = node.has_trip_unit
        self.trip_type_id = node.trip_type_id
        self.frame_amps = node.frame_amps
        self.sensor_amps = node.sensor_amps
        self.plug_amps = node.plug_amps
        self.length = node.length
        self.conductor_material = node.conductor_material
        self.cable_size_id = node.cable_size_id
        self.conductor_configuration_id = node.conductor_configuration_id
        self.duct_material_id = node.duct_material_id
        self.conductor_description_id = node.conductor_description_id
        self.insulation_class_id = node.insulation_class_id
        self.insulation_type_id = node.insulation_type_id
        self.installation_id = node.installation_id
        self.busway_ampere_rating = node.busway_ampere_rating
        self.replacement_cost = node.replacement_cost
        self.panel_schedule_status = node.panel_schedule_status
        self.rotation = node.rotation
        self.locked = node.locked
    }
}

extension SLDService {
    /// Creates an SLDDTO from an SLDV2 with proper handling of relationships
    /// - Parameters:
    ///   - sld: The SLDV2 to convert
    ///   - customName: Optional custom name for the DTO (defaults to sld.name)
    ///   - includeArcFlashCompletion: Whether to include arc flash completion status
    ///   - includeLocation: Whether to include location information
    ///   - includeQRCode: Whether to include QR code information
    ///   - includeCOM: Whether to include COM information
    /// - Returns: A complete SLDDTO
    @MainActor
    func createDTO(
        from sld: SLDV2,
        customName: String? = nil,
        includeArcFlashCompletion: Bool = true,
        includeLocation: Bool = true,
        includeQRCode: Bool = true,
        includeCOM: Bool = true
    ) -> SLDDTO {
        // Build a lookup map of terminal UUIDs to handle_codes
        // Terminal UUIDs are authoritative; handle_codes are derived for rendering
        var terminalHandleLookup: [UUID: String] = [:]
        for node in sld.nodes where !node.is_deleted {
            for terminal in node.node_terminals where !terminal.is_deleted {
                if let handleCode = terminal.handle_code {
                    terminalHandleLookup[terminal.id] = handleCode
                }
            }
        }

        return SLDDTO(
            id: sld.id,
            name: customName ?? sld.name,
            nodes: sld.nodes
                .filter { !$0.is_deleted }
                .map { node -> SLDDTONode in
                    var dto = SLDDTONode(
                        id: node.id,
                        type: node.type,
                        label: node.label,
                        sld_id: sld.id,
                        parent_id: node.parent_id,
                        x: node.x,
                        y: node.y,
                        width: node.width,
                        height: node.height,
                        is_deleted: node.is_deleted,
                        location: includeLocation ? node.location : nil,
                        node_class: node.node_class?.id,
                        core_attributes: node.core_attributes.map { attr in
                            NodePropertyDTO(
                                id: attr.id,
                                node_class_property: attr.node_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
                        com: includeCOM ? node.com : nil,
                        com_calculation: includeCOM ? node.com_calculation.map { COMCalculationDTO(from: $0) } : nil,
                        qr_code: includeQRCode ? node.qr_code : nil,
                        serviceability: node.serviceability,
                        serviceability_note: node.serviceability_note,
                        af_completion: includeArcFlashCompletion ? node.af_isComplete : nil,
                        eqp_lib: node.eqp_lib
                    )
                    dto.applyEngineering(from: node)
                    return dto
                },
            edges: sld.edges
                .filter { !$0.is_deleted }
                .map { edge in
                    // Derive handle codes from terminal UUIDs (authoritative)
                    // Fall back to stored handle codes for backward compatibility
                    let sourceHandle: String? = edge.sourceNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.sourceHandle
                    let targetHandle: String? = edge.targetNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.targetHandle

                    return SLDDTOEdge(
                        id: edge.id,
                        source: edge.source,
                        target: edge.target,
                        sld_id: sld.id,
                        source_handle: sourceHandle,
                        target_handle: targetHandle,
                        source_node_terminal_id: edge.sourceNodeTerminalId,
                        target_node_terminal_id: edge.targetNodeTerminalId,
                        edge_class: edge.edge_class?.id,
                        core_attributes: edge.core_attributes.map { attr in
                            EdgePropertyDTO(
                                id: attr.id,
                                edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        af_completion: includeArcFlashCompletion ? edge.af_isComplete : nil,
                        points: edge.points,
                        algorithm: edge.algorithm
                    )
                },
            photos: [],
            tasks: [],
            ir_photos: [],
            ir_sessions: [],
            issues: [],
            quotes: [],
            comments: sld.comments.map { $0.toDTO() }
        )
    }
    
    /// Creates an SLDDTO by fetching nodes and edges from ModelContext
    /// Used when SLD relationships might not be fully loaded
    /// - Parameters:
    ///   - sldId: The ID of the SLD
    ///   - modelContext: The ModelContext to fetch from
    ///   - customName: Optional custom name for the DTO
    /// - Returns: A complete SLDDTO
    @MainActor
    func createDTO(
        forSLDId sldId: UUID,
        modelContext: ModelContext,
        customName: String? = nil
    ) -> SLDDTO {
        // Fetch nodes
        let nodeDescriptor = FetchDescriptor<NodeV2>(
            predicate: #Predicate { node in
                if let nodeSld = node.sld {
                    return nodeSld.id == sldId && !node.is_deleted
                } else {
                    return false
                }
            }
        )
        let nodes = (try? modelContext.fetch(nodeDescriptor)) ?? []
        
        // Fetch edges
        let edgeDescriptor = FetchDescriptor<EdgeV2>(
            predicate: #Predicate { edge in
                if let edgeSld = edge.sld {
                    return edgeSld.id == sldId && !edge.is_deleted
                } else {
                    return false
                }
            }
        )
        let edges = (try? modelContext.fetch(edgeDescriptor)) ?? []

        // Build a lookup map of terminal UUIDs to handle_codes
        // Terminal UUIDs are authoritative; handle_codes are derived for rendering
        var terminalHandleLookup: [UUID: String] = [:]
        for node in nodes {
            for terminal in node.node_terminals where !terminal.is_deleted {
                if let handleCode = terminal.handle_code {
                    terminalHandleLookup[terminal.id] = handleCode
                }
            }
        }

        return SLDDTO(
            id: sldId,
            name: customName ?? "sld-update",
            nodes: nodes.map { node -> SLDDTONode in
                var dto = SLDDTONode(
                    id: node.id,
                    type: node.type,
                    label: node.label,
                    sld_id: sldId,
                    parent_id: node.parent_id,
                    x: node.x,
                    y: node.y,
                    width: node.width,
                    height: node.height,
                    is_deleted: node.is_deleted,
                    location: node.location,
                    node_class: node.node_class?.id,
                    core_attributes: node.core_attributes.map { attr in
                        NodePropertyDTO(
                            id: attr.id,
                            node_class_property: attr.node_class_property?.id.uuidString ?? "",
                            name: attr.name,
                            value: attr.value
                        )
                    },
                    node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
                    com: node.com,
                    com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
                    qr_code: node.qr_code,
                    serviceability: node.serviceability,
                    serviceability_note: node.serviceability_note,
                    af_completion: node.af_isComplete,
                    eqp_lib: node.eqp_lib
                )
                dto.applyEngineering(from: node)
                return dto
            },
            edges: edges.map { edge in
                // Derive handle codes from terminal UUIDs (authoritative)
                // Fall back to stored handle codes for backward compatibility
                let sourceHandle: String? = edge.sourceNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.sourceHandle
                let targetHandle: String? = edge.targetNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.targetHandle

                return SLDDTOEdge(
                    id: edge.id,
                    source: edge.source,
                    target: edge.target,
                    sld_id: sldId,
                    source_handle: sourceHandle,
                    target_handle: targetHandle,
                    source_node_terminal_id: edge.sourceNodeTerminalId,
                    target_node_terminal_id: edge.targetNodeTerminalId,
                    edge_class: edge.edge_class?.id,
                    core_attributes: edge.core_attributes.map { attr in
                        EdgePropertyDTO(
                            id: attr.id,
                            edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                            name: attr.name,
                            value: attr.value
                        )
                    },
                    af_completion: edge.af_isComplete,
                    points: edge.points,
                    algorithm: edge.algorithm
                )
            },
            photos: [],
            tasks: [],
            ir_photos: [],
            ir_sessions: [],
            issues: [],
            quotes: [],
            comments: []
        )
    }

    /// Creates an SLDDTO from custom node and edge arrays
    /// Used in edit views where nodes/edges are modified in memory before saving
    /// - Parameters:
    ///   - sldId: The ID of the SLD
    ///   - nodes: Array of nodes to include
    ///   - edges: Array of edges to include
    ///   - customName: Optional custom name for the DTO
    /// - Returns: A complete SLDDTO
    @MainActor
    func createDTO(
        forSLDId sldId: UUID,
        nodes: [NodeV2],
        edges: [EdgeV2],
        customName: String? = nil
    ) -> SLDDTO {
        // Build a lookup map of terminal UUIDs to handle_codes
        // Terminal UUIDs are authoritative; handle_codes are derived for rendering
        var terminalHandleLookup: [UUID: String] = [:]
        for node in nodes where !node.is_deleted {
            for terminal in node.node_terminals where !terminal.is_deleted {
                if let handleCode = terminal.handle_code {
                    terminalHandleLookup[terminal.id] = handleCode
                }
            }
        }

        return SLDDTO(
            id: sldId,
            name: customName ?? "custom-update",
            nodes: nodes
                .filter { !$0.is_deleted }
                .map { node -> SLDDTONode in
                    var dto = SLDDTONode(
                        id: node.id,
                        type: node.type,
                        label: node.label,
                        sld_id: sldId,
                        parent_id: node.parent_id,
                        x: node.x,
                        y: node.y,
                        width: node.width,
                        height: node.height,
                        is_deleted: node.is_deleted,
                        location: node.location,
                        node_class: node.node_class?.id,
                        core_attributes: node.core_attributes.map { attr in
                            NodePropertyDTO(
                                id: attr.id,
                                node_class_property: attr.node_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
                        com: node.com,
                        com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
                        qr_code: node.qr_code,
                        serviceability: node.serviceability,
                        serviceability_note: node.serviceability_note,
                        af_completion: node.af_isComplete,
                        eqp_lib: node.eqp_lib
                    )
                    dto.applyEngineering(from: node)
                    return dto
                },
            edges: edges
                .filter { !$0.is_deleted }
                .map { edge in
                    // Derive handle codes from terminal UUIDs (authoritative)
                    // Fall back to stored handle codes for backward compatibility
                    let sourceHandle: String? = edge.sourceNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.sourceHandle
                    let targetHandle: String? = edge.targetNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.targetHandle

                    return SLDDTOEdge(
                        id: edge.id,
                        source: edge.source,
                        target: edge.target,
                        sld_id: sldId,
                        source_handle: sourceHandle,
                        target_handle: targetHandle,
                        source_node_terminal_id: edge.sourceNodeTerminalId,
                        target_node_terminal_id: edge.targetNodeTerminalId,
                        edge_class: edge.edge_class?.id,
                        core_attributes: edge.core_attributes.map { attr in
                            EdgePropertyDTO(
                                id: attr.id,
                                edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        af_completion: edge.af_isComplete,
                        points: edge.points,
                        algorithm: edge.algorithm
                    )
                },
            photos: [],
            tasks: [],
            ir_photos: [],
            ir_sessions: [],
            issues: [],
            quotes: [],
            comments: []
        )
    }

    /// Creates an SLDDTO from custom node and edge arrays with view-specific position/routing overrides
    /// Used in WebViewBridge when displaying a specific view with view-specific positions
    /// - Parameters:
    ///   - sldId: The ID of the SLD
    ///   - nodes: Array of nodes to include
    ///   - edges: Array of edges to include
    ///   - customName: Optional custom name for the DTO
    ///   - nodePositionOverrides: Dictionary of node ID to view-specific position data
    ///   - edgeRoutingOverrides: Dictionary of edge ID to view-specific routing data
    /// - Returns: A complete SLDDTO with view-specific positions applied
    @MainActor
    func createDTO(
        forSLDId sldId: UUID,
        nodes: [NodeV2],
        edges: [EdgeV2],
        customName: String? = nil,
        nodePositionOverrides: [UUID: (x: Double, y: Double, width: Double?, height: Double?, isCollapsed: Bool)],
        edgeRoutingOverrides: [UUID: (points: [EdgePoint]?, algorithm: String?)]
    ) -> SLDDTO {
        // Build a lookup map of terminal UUIDs to handle_codes
        // Terminal UUIDs are authoritative; handle_codes are derived for rendering
        var terminalHandleLookup: [UUID: String] = [:]
        for node in nodes where !node.is_deleted {
            for terminal in node.node_terminals where !terminal.is_deleted {
                if let handleCode = terminal.handle_code {
                    terminalHandleLookup[terminal.id] = handleCode
                }
            }
        }

        return SLDDTO(
            id: sldId,
            name: customName ?? "view-specific-update",
            nodes: nodes
                .filter { !$0.is_deleted }
                .map { node -> SLDDTONode in
                    // Apply view-specific position if available
                    let override = nodePositionOverrides[node.id]
                    var dto = SLDDTONode(
                        id: node.id,
                        type: node.type,
                        label: node.label,
                        sld_id: sldId,
                        parent_id: node.parent_id,
                        x: override?.x ?? node.x,
                        y: override?.y ?? node.y,
                        width: override?.width ?? node.width,
                        height: override?.height ?? node.height,
                        is_deleted: node.is_deleted,
                        location: node.location,
                        node_class: node.node_class?.id,
                        core_attributes: node.core_attributes.map { attr in
                            NodePropertyDTO(
                                id: attr.id,
                                node_class_property: attr.node_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        node_terminals: node.node_terminals.filter { !$0.is_deleted }.map { $0.toDTO() },
                        com: node.com,
                        com_calculation: node.com_calculation.map { COMCalculationDTO(from: $0) },
                        qr_code: node.qr_code,
                        serviceability: node.serviceability,
                        serviceability_note: node.serviceability_note,
                        af_completion: node.af_isComplete,
                        is_collapsed: override?.isCollapsed,
                        eqp_lib: node.eqp_lib
                    )
                    dto.applyEngineering(from: node)
                    return dto
                },
            edges: edges
                .filter { !$0.is_deleted }
                .map { edge in
                    // Apply view-specific routing if available
                    let override = edgeRoutingOverrides[edge.id]
                    // Derive handle codes from terminal UUIDs (authoritative)
                    // Fall back to stored handle codes for backward compatibility
                    let sourceHandle: String? = edge.sourceNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.sourceHandle
                    let targetHandle: String? = edge.targetNodeTerminalId.flatMap { terminalHandleLookup[$0] } ?? edge.targetHandle

                    return SLDDTOEdge(
                        id: edge.id,
                        source: edge.source,
                        target: edge.target,
                        sld_id: sldId,
                        source_handle: sourceHandle,
                        target_handle: targetHandle,
                        source_node_terminal_id: edge.sourceNodeTerminalId,
                        target_node_terminal_id: edge.targetNodeTerminalId,
                        edge_class: edge.edge_class?.id,
                        core_attributes: edge.core_attributes.map { attr in
                            EdgePropertyDTO(
                                id: attr.id,
                                edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        af_completion: edge.af_isComplete,
                        points: override?.points ?? edge.points,
                        algorithm: override?.algorithm ?? edge.algorithm
                    )
                },
            photos: [],
            tasks: [],
            ir_photos: [],
            ir_sessions: [],
            issues: [],
            quotes: [],
            comments: []
        )
    }
}