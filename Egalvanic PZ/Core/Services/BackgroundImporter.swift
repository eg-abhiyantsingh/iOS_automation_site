//
//  BackgroundImporter.swift
//  Egalvanic PZ
//
//  Background actor for batch importing nodes and edges with optimized memory usage.
//  Creates ModelContext inside each method for 10x performance improvement.
//

import Foundation
import SwiftData
import os

/// Progress callback for reporting batch import progress
typealias ImportProgressCallback = @Sendable (Int, Int) async -> Void

/// Actor responsible for batch importing entities on a background thread.
actor BackgroundImporter {
    let modelContainer: ModelContainer
    private let batchSize: Int

    init(modelContainer: ModelContainer, batchSize: Int = 1000) {
        self.modelContainer = modelContainer
        self.batchSize = batchSize
    }

    // MARK: - Node Import

    /// Import nodes in batches with memory-efficient processing.
    /// Pre-caches class lookups to avoid N+1 queries.
    func importNodes(
        _ nodeDTOs: [SLDDTONode],
        sldId: UUID,
        onProgress: ImportProgressCallback? = nil
    ) async throws -> [UUID] {
        let modelContext = ModelContext(modelContainer)
        var processedIds: [UUID] = []
        let totalNodes = nodeDTOs.count

        guard totalNodes > 0 else { return [] }

        // [ZAP] Debug voltage/notes field population from API
        let withVoltage = nodeDTOs.filter { $0.voltage != nil }
        let withVoltageId = nodeDTOs.filter { $0.voltage_id != nil }
        let withSecVoltage = nodeDTOs.filter { $0.secondary_voltage != nil }
        let withSecVoltageId = nodeDTOs.filter { $0.secondary_voltage_id != nil }
        let withNotes = nodeDTOs.filter { $0.notes != nil && !($0.notes?.isEmpty ?? true) }
        AppLogger.log(.info, "[ZAP] Node sync stats — total: \(totalNodes), voltage: \(withVoltage.count), voltage_id: \(withVoltageId.count), sec_voltage: \(withSecVoltage.count), sec_voltage_id: \(withSecVoltageId.count), notes: \(withNotes.count)", category: .sync)
        for dto in withVoltage.prefix(3) {
            AppLogger.log(.info, "[ZAP] Sample — \(dto.label): voltage=\(dto.voltage ?? 0), voltage_id=\(dto.voltage_id ?? 0), sec_voltage=\(dto.secondary_voltage ?? 0), sec_voltage_id=\(dto.secondary_voltage_id ?? 0), notes=\(dto.notes ?? "nil")", category: .sync)
        }
        for dto in withNotes.prefix(3) where dto.voltage == nil {
            AppLogger.log(.info, "[ZAP] Sample (notes only) — \(dto.label): notes=\(dto.notes ?? "nil")", category: .sync)
        }

        AppLogger.log(.info, "[BatchImport] Starting node import: \(totalNodes) nodes", category: .sync)

        // Fetch SLD
        let sldDescriptor = FetchDescriptor<SLDV2>(predicate: #Predicate<SLDV2> { $0.id == sldId })
        guard let sld = try modelContext.fetch(sldDescriptor).first else {
            throw ImportError.sldNotFound(sldId)
        }

        // Pre-cache all lookups to avoid N+1 queries
        let existingNodes = try modelContext.fetch(FetchDescriptor<NodeV2>())
        var nodeLookup = Dictionary(uniqueKeysWithValues: existingNodes.map { ($0.id, $0) })

        let allNodeClasses = try modelContext.fetch(FetchDescriptor<NodeClass>())
        let nodeClassLookup = Dictionary(uniqueKeysWithValues: allNodeClasses.map { ($0.id, $0) })

        let allNodeSubtypes = try modelContext.fetch(FetchDescriptor<NodeSubtype>())
        let nodeSubtypeLookup = Dictionary(uniqueKeysWithValues: allNodeSubtypes.map { ($0.id, $0) })

        let allNodeClassProperties = try modelContext.fetch(FetchDescriptor<NodeClassProperty>())
        let nodeClassPropertyLookup = Dictionary(uniqueKeysWithValues: allNodeClassProperties.map { ($0.id, $0) })

        let allRooms = try modelContext.fetch(FetchDescriptor<Room>())
        let roomLookup = Dictionary(uniqueKeysWithValues: allRooms.map { ($0.id, $0) })

        // Pre-cache node orientation terminals for terminal sync
        let allOrientationTerminals = try modelContext.fetch(FetchDescriptor<NodeOrientationTerminal>())
        let orientationTerminalLookup = Dictionary(uniqueKeysWithValues: allOrientationTerminals.map { ($0.id, $0) })

        // Process in batches
        let chunks = nodeDTOs.chunked(into: batchSize)
        var processedCount = 0

        for chunk in chunks {
            for nodeDTO in chunk {
                let nodeClass = nodeDTO.node_class.flatMap { nodeClassLookup[$0] }
                let nodeSubtype = nodeDTO.node_subtype.flatMap { nodeSubtypeLookup[$0] }
                let room = nodeDTO.room_id.flatMap { roomLookup[$0] }

                if let existingNode = nodeLookup[nodeDTO.id] {
                    // Update existing node
                    updateNode(existingNode, from: nodeDTO, sld: sld,
                              nodeClass: nodeClass, nodeSubtype: nodeSubtype, room: room,
                              nodeClassPropertyLookup: nodeClassPropertyLookup,
                              orientationTerminalLookup: orientationTerminalLookup,
                              context: modelContext)
                } else {
                    // Create new node
                    let newNode = createNode(from: nodeDTO, sld: sld,
                                            nodeClass: nodeClass, nodeSubtype: nodeSubtype, room: room,
                                            nodeClassPropertyLookup: nodeClassPropertyLookup,
                                            orientationTerminalLookup: orientationTerminalLookup,
                                            context: modelContext)
                    modelContext.insert(newNode)
                    nodeLookup[nodeDTO.id] = newNode
                }

                processedIds.append(nodeDTO.id)
                processedCount += 1
            }

            try modelContext.save()
            await onProgress?(processedCount, totalNodes)
        }

        AppLogger.log(.info, "[BatchImport] Completed node import: \(processedIds.count) nodes", category: .sync)
        return processedIds
    }

    // MARK: - Edge Import

    /// Import edges in batches with memory-efficient processing.
    func importEdges(
        _ edgeDTOs: [SLDDTOEdge],
        sldId: UUID,
        onProgress: ImportProgressCallback? = nil
    ) async throws -> [UUID] {
        let modelContext = ModelContext(modelContainer)
        var processedIds: [UUID] = []
        let totalEdges = edgeDTOs.count

        guard totalEdges > 0 else { return [] }

        AppLogger.log(.info, "[BatchImport] Starting edge import: \(totalEdges) edges", category: .sync)

        // Fetch SLD
        let sldDescriptor = FetchDescriptor<SLDV2>(predicate: #Predicate<SLDV2> { $0.id == sldId })
        guard let sld = try modelContext.fetch(sldDescriptor).first else {
            throw ImportError.sldNotFound(sldId)
        }

        // Pre-cache all lookups
        let existingEdges = try modelContext.fetch(FetchDescriptor<EdgeV2>())
        var edgeLookup = Dictionary(uniqueKeysWithValues: existingEdges.map { ($0.id, $0) })

        let allEdgeClasses = try modelContext.fetch(FetchDescriptor<EdgeClass>())
        let edgeClassLookup = Dictionary(uniqueKeysWithValues: allEdgeClasses.map { ($0.id, $0) })

        let allEdgeClassProperties = try modelContext.fetch(FetchDescriptor<EdgeClassProperty>())
        let edgeClassPropertyLookup = Dictionary(uniqueKeysWithValues: allEdgeClassProperties.map { ($0.id, $0) })

        // Process in batches
        let chunks = edgeDTOs.chunked(into: batchSize)
        var processedCount = 0

        for chunk in chunks {
            for edgeDTO in chunk {
                let edgeClass = edgeDTO.edge_class.flatMap { edgeClassLookup[$0] }

                if let existingEdge = edgeLookup[edgeDTO.id] {
                    // Update existing edge
                    updateEdge(existingEdge, from: edgeDTO, sld: sld,
                              edgeClass: edgeClass,
                              edgeClassPropertyLookup: edgeClassPropertyLookup, context: modelContext)
                } else {
                    // Create new edge
                    let newEdge = createEdge(from: edgeDTO, sld: sld,
                                            edgeClass: edgeClass,
                                            edgeClassPropertyLookup: edgeClassPropertyLookup, context: modelContext)
                    modelContext.insert(newEdge)
                    edgeLookup[edgeDTO.id] = newEdge
                }

                processedIds.append(edgeDTO.id)
                processedCount += 1
            }

            try modelContext.save()
            await onProgress?(processedCount, totalEdges)
        }

        AppLogger.log(.info, "[BatchImport] Completed edge import: \(processedIds.count) edges", category: .sync)
        return processedIds
    }

    // MARK: - Private Node Helpers

    private func updateNode(
        _ node: NodeV2,
        from dto: SLDDTONode,
        sld: SLDV2,
        nodeClass: NodeClass?,
        nodeSubtype: NodeSubtype?,
        room: Room?,
        nodeClassPropertyLookup: [UUID: NodeClassProperty],
        orientationTerminalLookup: [UUID: NodeOrientationTerminal],
        context: ModelContext
    ) {
        node.label = dto.label
        node.type = dto.type
        node.parent_id = dto.parent_id
        node.x = dto.x
        node.y = dto.y
        node.width = dto.width
        node.height = dto.height
        node.sld = sld
        node.is_deleted = dto.is_deleted
        node.location = dto.location
        node.node_class = nodeClass
        node.node_subtype = nodeSubtype
        node.room = room
        node.com = dto.com
        node.com_calculation = dto.com_calculation?.toModel()
        node.qr_code = dto.qr_code
        node.serviceability = dto.serviceability
        node.serviceability_note = dto.serviceability_note
        node.voltage = dto.voltage
        node.voltage_id = dto.voltage_id
        node.secondary_voltage = dto.secondary_voltage
        node.secondary_voltage_id = dto.secondary_voltage_id
        node.notes = dto.notes

        // ZP-2161 engineering fields — must stay in sync with the
        // matching block in SLDSyncService.updateNode. Easy to forget
        // since this path only fires above 200 nodes/edges.
        node.tertiary_voltage = dto.tertiary_voltage
        node.tertiary_voltage_id = dto.tertiary_voltage_id
        node.system_voltage_id = dto.system_voltage_id
        node.circuit_voltage_id = dto.circuit_voltage_id
        node.voltage_user_overridden = dto.voltage_user_overridden
        node.applied_shortcut_id = dto.applied_shortcut
        node.eqp_lib = dto.eqp_lib
        node.eqp_lib_suggested = dto.eqp_lib_suggested
        node.eqp_note = dto.eqp_note
        node.eqp_engineering_approved = dto.eqp_engineering_approved
        node.skm_lib_name = dto.skm_lib_name
        node.skm_lib_name_suggested = dto.skm_lib_name_suggested
        node.ocr_signature = dto.ocr_signature
        node.kva_rating = dto.kva_rating
        node.percent_impedance = dto.percent_impedance
        node.mains_type_id = dto.mains_type_id
        node.phase_configuration_id = dto.phase_configuration_id
        node.ampere_rating = dto.ampere_rating
        node.pole_count = dto.pole_count
        node.manufacturer_id = dto.manufacturer_id
        node.has_trip_unit = dto.has_trip_unit
        node.trip_type_id = dto.trip_type_id
        node.frame_amps = dto.frame_amps
        node.sensor_amps = dto.sensor_amps
        node.plug_amps = dto.plug_amps
        node.length = dto.length
        node.conductor_material = dto.conductor_material
        node.cable_size_id = dto.cable_size_id
        node.conductor_configuration_id = dto.conductor_configuration_id
        node.duct_material_id = dto.duct_material_id
        node.conductor_description_id = dto.conductor_description_id
        node.insulation_class_id = dto.insulation_class_id
        node.insulation_type_id = dto.insulation_type_id
        node.installation_id = dto.installation_id
        node.busway_ampere_rating = dto.busway_ampere_rating
        node.replacement_cost = dto.replacement_cost
        node.panel_schedule_status = dto.panel_schedule_status
        node.rotation = dto.rotation
        node.locked = dto.locked

        // Handle core_attributes
        updateNodeCoreAttributes(node, from: dto.core_attributes,
                                nodeClassPropertyLookup: nodeClassPropertyLookup, context: context)

        // Handle node_terminals
        syncNodeTerminals(node, from: dto.node_terminals,
                         orientationTerminalLookup: orientationTerminalLookup, context: context)
    }

    private func createNode(
        from dto: SLDDTONode,
        sld: SLDV2,
        nodeClass: NodeClass?,
        nodeSubtype: NodeSubtype?,
        room: Room?,
        nodeClassPropertyLookup: [UUID: NodeClassProperty],
        orientationTerminalLookup: [UUID: NodeOrientationTerminal],
        context: ModelContext
    ) -> NodeV2 {
        let node = NodeV2(
            id: dto.id,
            label: dto.label,
            type: dto.type,
            sld: sld,
            parent_id: dto.parent_id,
            x: dto.x,
            y: dto.y,
            width: dto.width,
            height: dto.height,
            photos: [],
            is_deleted: dto.is_deleted,
            location: dto.location,
            room: room,
            node_class: nodeClass,
            node_subtype: nodeSubtype,
            core_attributes: [],
            node_terminals: [],
            node_tasks: [],
            ir_photos: [],
            com: dto.com,
            com_calculation: dto.com_calculation?.toModel(),
            qr_code: dto.qr_code,
            serviceability: dto.serviceability,
            serviceability_note: dto.serviceability_note,
            voltage: dto.voltage,
            voltage_id: dto.voltage_id,
            secondary_voltage: dto.secondary_voltage,
            secondary_voltage_id: dto.secondary_voltage_id,
            notes: dto.notes
        )

        // ZP-2161 — assign new engineering fields outside the init() so
        // we don't have to thread them through the giant NodeV2
        // initializer. Must stay in sync with SLDSyncService.createNode.
        node.tertiary_voltage = dto.tertiary_voltage
        node.tertiary_voltage_id = dto.tertiary_voltage_id
        node.system_voltage_id = dto.system_voltage_id
        node.circuit_voltage_id = dto.circuit_voltage_id
        node.voltage_user_overridden = dto.voltage_user_overridden
        node.applied_shortcut_id = dto.applied_shortcut
        node.eqp_lib = dto.eqp_lib
        node.eqp_lib_suggested = dto.eqp_lib_suggested
        node.eqp_note = dto.eqp_note
        node.eqp_engineering_approved = dto.eqp_engineering_approved
        node.skm_lib_name = dto.skm_lib_name
        node.skm_lib_name_suggested = dto.skm_lib_name_suggested
        node.ocr_signature = dto.ocr_signature
        node.kva_rating = dto.kva_rating
        node.percent_impedance = dto.percent_impedance
        node.mains_type_id = dto.mains_type_id
        node.phase_configuration_id = dto.phase_configuration_id
        node.ampere_rating = dto.ampere_rating
        node.pole_count = dto.pole_count
        node.manufacturer_id = dto.manufacturer_id
        node.has_trip_unit = dto.has_trip_unit
        node.trip_type_id = dto.trip_type_id
        node.frame_amps = dto.frame_amps
        node.sensor_amps = dto.sensor_amps
        node.plug_amps = dto.plug_amps
        node.length = dto.length
        node.conductor_material = dto.conductor_material
        node.cable_size_id = dto.cable_size_id
        node.conductor_configuration_id = dto.conductor_configuration_id
        node.duct_material_id = dto.duct_material_id
        node.conductor_description_id = dto.conductor_description_id
        node.insulation_class_id = dto.insulation_class_id
        node.insulation_type_id = dto.insulation_type_id
        node.installation_id = dto.installation_id
        node.busway_ampere_rating = dto.busway_ampere_rating
        node.replacement_cost = dto.replacement_cost
        node.panel_schedule_status = dto.panel_schedule_status
        node.rotation = dto.rotation
        node.locked = dto.locked

        // Create core_attributes
        for attrDTO in dto.core_attributes {
            guard let uuid = UUID(uuidString: attrDTO.node_class_property),
                  let nodeClassProperty = nodeClassPropertyLookup[uuid] else {
                continue
            }

            let nodeProperty = NodeProperty(
                id: attrDTO.id,
                node_class_property: nodeClassProperty,
                name: attrDTO.name,
                value: attrDTO.value ?? ""
            )
            context.insert(nodeProperty)
            node.core_attributes.append(nodeProperty)
        }

        // Create node_terminals
        syncNodeTerminals(node, from: dto.node_terminals,
                         orientationTerminalLookup: orientationTerminalLookup, context: context)

        return node
    }

    private func updateNodeCoreAttributes(
        _ node: NodeV2,
        from incomingAttributes: [NodePropertyDTO],
        nodeClassPropertyLookup: [UUID: NodeClassProperty],
        context: ModelContext
    ) {
        if !incomingAttributes.isEmpty {
            let existingLookup = Dictionary(uniqueKeysWithValues: node.core_attributes.map { ($0.id, $0) })
            var seenIds = Set<UUID>()

            for attrDTO in incomingAttributes {
                seenIds.insert(attrDTO.id)

                guard let uuid = UUID(uuidString: attrDTO.node_class_property),
                      let nodeClassProperty = nodeClassPropertyLookup[uuid] else {
                    continue
                }

                let attrValue = attrDTO.value ?? ""
                if let existing = existingLookup[attrDTO.id] {
                    existing.name = attrDTO.name
                    existing.value = attrValue
                    existing.node_class_property = nodeClassProperty
                } else {
                    let newAttr = NodeProperty(
                        id: attrDTO.id,
                        node_class_property: nodeClassProperty,
                        name: attrDTO.name,
                        value: attrValue
                    )
                    context.insert(newAttr)
                    node.core_attributes.append(newAttr)
                }
            }

            // Remove attributes not in DTO
            for attr in node.core_attributes where !seenIds.contains(attr.id) {
                node.core_attributes.removeAll { $0.id == attr.id }
                context.delete(attr)
            }
        } else {
            // Clear all attributes
            for attr in node.core_attributes {
                context.delete(attr)
            }
            node.core_attributes.removeAll()
        }
    }

    /// Sync node terminals from incoming DTOs
    private func syncNodeTerminals(
        _ node: NodeV2,
        from incomingTerminals: [NodeTerminalDTO],
        orientationTerminalLookup: [UUID: NodeOrientationTerminal],
        context: ModelContext
    ) {
        guard !incomingTerminals.isEmpty else {
            // Clear all terminals if incoming is empty
            for terminal in node.node_terminals {
                context.delete(terminal)
            }
            node.node_terminals.removeAll()
            return
        }

        let existingById = Dictionary(uniqueKeysWithValues: node.node_terminals.map { ($0.id, $0) })
        var seenIds = Set<UUID>()

        for terminalDTO in incomingTerminals {
            seenIds.insert(terminalDTO.id)

            // Get the orientation terminal for blueprint properties
            _ = orientationTerminalLookup[terminalDTO.node_orientation_terminal_id]

            if let existing = existingById[terminalDTO.id] {
                // Update existing terminal
                existing.node_orientation_terminal_id = terminalDTO.node_orientation_terminal_id
                // Properties from DTO (derived from orientation terminal on server)
                existing.handle_code = terminalDTO.handle_code
                existing.label = terminalDTO.label
                existing.side = terminalDTO.side
                existing.position = terminalDTO.position
                existing.offset_percent = terminalDTO.offset_percent
                existing.color = terminalDTO.color
                existing.show_label = terminalDTO.show_label
                // Instance-specific electrical properties
                existing.voltage_rating = terminalDTO.voltage_rating
                existing.amp_rating = terminalDTO.amp_rating
                existing.phase_count = terminalDTO.phase_count
                existing.is_deleted = terminalDTO.is_deleted
                existing.lastSyncedAt = Date()
            } else {
                // Create new terminal using the convenience initializer
                let terminal = NodeTerminal(from: terminalDTO, node: node)
                terminal.lastSyncedAt = Date()
                context.insert(terminal)
                node.node_terminals.append(terminal)
            }
        }

        // Remove terminals no longer in the incoming data
        let staleTerminals = node.node_terminals.filter { !seenIds.contains($0.id) }
        for terminal in staleTerminals {
            node.node_terminals.removeAll { $0.id == terminal.id }
            context.delete(terminal)
        }
    }

    // MARK: - Private Edge Helpers

    private func updateEdge(
        _ edge: EdgeV2,
        from dto: SLDDTOEdge,
        sld: SLDV2,
        edgeClass: EdgeClass?,
        edgeClassPropertyLookup: [UUID: EdgeClassProperty],
        context: ModelContext
    ) {
        edge.source = dto.source
        edge.target = dto.target
        edge.sourceHandle = dto.source_handle
        edge.targetHandle = dto.target_handle
        edge.sourceNodeTerminalId = dto.source_node_terminal_id
        edge.targetNodeTerminalId = dto.target_node_terminal_id
        edge.sld = sld
        edge.is_deleted = dto.is_deleted
        edge.edge_class = edgeClass
        edge.points = dto.points
        edge.algorithm = dto.algorithm

        updateEdgeCoreAttributes(edge, from: dto.core_attributes ?? [],
                                edgeClassPropertyLookup: edgeClassPropertyLookup, context: context)
    }

    private func createEdge(
        from dto: SLDDTOEdge,
        sld: SLDV2,
        edgeClass: EdgeClass?,
        edgeClassPropertyLookup: [UUID: EdgeClassProperty],
        context: ModelContext
    ) -> EdgeV2 {
        let edge = EdgeV2(
            id: dto.id,
            source: dto.source,
            target: dto.target,
            sld: sld,
            is_deleted: dto.is_deleted,
            sourceHandle: dto.source_handle,
            targetHandle: dto.target_handle,
            sourceNodeTerminalId: dto.source_node_terminal_id,
            targetNodeTerminalId: dto.target_node_terminal_id,
            edge_class: edgeClass,
            core_attributes: [],
            points: dto.points,
            algorithm: dto.algorithm
        )

        for attrDTO in dto.core_attributes ?? [] {
            guard let uuid = UUID(uuidString: attrDTO.edge_class_property),
                  let edgeClassProperty = edgeClassPropertyLookup[uuid] else {
                continue
            }

            let edgeProperty = EdgeProperty(
                id: attrDTO.id,
                edge_class_property: edgeClassProperty,
                name: attrDTO.name,
                value: attrDTO.value ?? ""
            )
            context.insert(edgeProperty)
            edge.core_attributes.append(edgeProperty)
        }

        return edge
    }

    private func updateEdgeCoreAttributes(
        _ edge: EdgeV2,
        from incomingAttributes: [EdgePropertyDTO],
        edgeClassPropertyLookup: [UUID: EdgeClassProperty],
        context: ModelContext
    ) {
        if !incomingAttributes.isEmpty {
            var existingLookup = Dictionary(uniqueKeysWithValues: edge.core_attributes.map { ($0.id, $0) })
            var seenIds = Set<UUID>()

            for attrDTO in incomingAttributes {
                seenIds.insert(attrDTO.id)

                guard let uuid = UUID(uuidString: attrDTO.edge_class_property),
                      let edgeClassProperty = edgeClassPropertyLookup[uuid] else {
                    continue
                }

                let attrValue = attrDTO.value ?? ""
                if let existing = existingLookup[attrDTO.id] {
                    existing.name = attrDTO.name
                    existing.value = attrValue
                    existing.edge_class_property = edgeClassProperty
                } else {
                    let newAttr = EdgeProperty(
                        id: attrDTO.id,
                        edge_class_property: edgeClassProperty,
                        name: attrDTO.name,
                        value: attrValue
                    )
                    context.insert(newAttr)
                    edge.core_attributes.append(newAttr)
                }
            }

            for attr in edge.core_attributes where !seenIds.contains(attr.id) {
                edge.core_attributes.removeAll { $0.id == attr.id }
                context.delete(attr)
            }
        } else {
            for attr in edge.core_attributes {
                context.delete(attr)
            }
            edge.core_attributes.removeAll()
        }
    }

    // MARK: - Photo Import

    /// Import photos in batches with saves every 500 to maintain consistent speed.
    func importPhotos(
        _ photoDTOs: [(photoDTO: SLDDTOPhoto, sldId: UUID)],
        onProgress: ImportProgressCallback? = nil
    ) async throws -> Int {
        let totalPhotos = photoDTOs.count
        guard totalPhotos > 0 else { return 0 }

        AppLogger.log(.info, "[BatchImport] Starting photo import: \(totalPhotos) photos", category: .sync)

        let context = ModelContext(modelContainer)

        // Build lookups
        let slds = try context.fetch(FetchDescriptor<SLDV2>())
        let sldLookup = Dictionary(uniqueKeysWithValues: slds.map { ($0.id, $0) })

        let nodes = try context.fetch(FetchDescriptor<NodeV2>())
        let nodeLookup = Dictionary(uniqueKeysWithValues: nodes.map { ($0.id, $0) })

        let tasks = try context.fetch(FetchDescriptor<UserTask>())
        let taskLookup = Dictionary(uniqueKeysWithValues: tasks.map { ($0.id, $0) })

        let issues = try context.fetch(FetchDescriptor<Issue>())
        let issueLookup = Dictionary(uniqueKeysWithValues: issues.map { ($0.id, $0) })

        let buildings = try context.fetch(FetchDescriptor<Building>())
        let buildingLookup = Dictionary(uniqueKeysWithValues: buildings.map { ($0.id, $0) })

        let floors = try context.fetch(FetchDescriptor<Floor>())
        let floorLookup = Dictionary(uniqueKeysWithValues: floors.map { ($0.id, $0) })

        let rooms = try context.fetch(FetchDescriptor<Room>())
        let roomLookup = Dictionary(uniqueKeysWithValues: rooms.map { ($0.id, $0) })

        let existingPhotos = try context.fetch(FetchDescriptor<Photo>())
        var photoLookup = Dictionary(uniqueKeysWithValues: existingPhotos.map { ($0.id, $0) })

        // Process in batches of 500
        let chunks = photoDTOs.chunked(into: 500)
        var processedCount = 0

        for chunk in chunks {
            for (photoDTO, sldId) in chunk {
                guard let entityID = photoDTO.entity_id,
                      let sld = sldLookup[sldId] else {
                    processedCount += 1
                    continue
                }

                if photoDTO.type.hasPrefix("node_") {
                    if let node = nodeLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: node, task: nil, issue: nil,
                                   building: nil, floor: nil, room: nil,
                                   photoLookup: &photoLookup, context: context)
                    }
                } else if photoDTO.type.hasPrefix("task_") {
                    if let task = taskLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: nil, task: task, issue: nil,
                                   building: nil, floor: nil, room: nil,
                                   photoLookup: &photoLookup, context: context)
                    }
                } else if photoDTO.type == "issue" {
                    if let issue = issueLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: nil, task: nil, issue: issue,
                                   building: nil, floor: nil, room: nil,
                                   photoLookup: &photoLookup, context: context)
                    }
                } else if photoDTO.type == "building" {
                    if let building = buildingLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: nil, task: nil, issue: nil,
                                   building: building, floor: nil, room: nil,
                                   photoLookup: &photoLookup, context: context)
                    }
                } else if photoDTO.type == "floor" {
                    if let floor = floorLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: nil, task: nil, issue: nil,
                                   building: nil, floor: floor, room: nil,
                                   photoLookup: &photoLookup, context: context)
                    }
                } else if photoDTO.type == "room" {
                    if let room = roomLookup[entityID] {
                        upsertPhoto(photoDTO: photoDTO, sld: sld, node: nil, task: nil, issue: nil,
                                   building: nil, floor: nil, room: room,
                                   photoLookup: &photoLookup, context: context)
                    }
                }

                processedCount += 1
            }

            try context.save()
            await onProgress?(processedCount, totalPhotos)
        }

        AppLogger.log(.info, "[BatchImport] Completed photo import: \(processedCount) photos", category: .sync)
        return processedCount
    }

    // MARK: - Private Photo Helpers

    private func upsertPhoto(
        photoDTO: SLDDTOPhoto,
        sld: SLDV2,
        node: NodeV2?,
        task: UserTask?,
        issue: Issue?,
        building: Building?,
        floor: Floor?,
        room: Room?,
        photoLookup: inout [UUID: Photo],
        context: ModelContext
    ) {
        if let existingPhoto = photoLookup[photoDTO.id] {
            // Update existing - preserve upload_needed if true
            if !existingPhoto.upload_needed {
                existingPhoto.url = photoDTO.url
                existingPhoto.type = photoDTO.type
                existingPhoto.is_deleted = photoDTO.is_deleted
            }
            // Always sync caption from server (metadata, not file data)
            existingPhoto.caption = photoDTO.caption
            // Only set relationships if they've changed
            if existingPhoto.node?.id != node?.id { existingPhoto.node = node }
            if existingPhoto.userTask?.id != task?.id { existingPhoto.userTask = task }
            if existingPhoto.issue?.id != issue?.id { existingPhoto.issue = issue }
            if existingPhoto.building?.id != building?.id { existingPhoto.building = building }
            if existingPhoto.floor?.id != floor?.id { existingPhoto.floor = floor }
            if existingPhoto.room?.id != room?.id { existingPhoto.room = room }
            if existingPhoto.sld?.id != sld.id { existingPhoto.sld = sld }
        } else {
            // Create new
            let photo = Photo(
                id: photoDTO.id,
                node: node,
                userTask: task,
                issue: issue,
                building: building,
                floor: floor,
                room: room,
                url: photoDTO.url,
                type: photoDTO.type,
                sld: sld,
                upload_needed: photoDTO.upload_needed,
                local_filepath: photoDTO.local_filepath,
                filename: photoDTO.filename,
                is_deleted: photoDTO.is_deleted,
                caption: photoDTO.caption
            )
            context.insert(photo)
            photoLookup[photoDTO.id] = photo
        }
    }

    // MARK: - Attachment Import

    /// Import attachments in batches.
    func importAttachments(
        _ attachmentDTOs: [(attachmentDTO: AttachmentDTO, sldId: UUID)],
        onProgress: ImportProgressCallback? = nil
    ) async throws -> Int {
        let totalAttachments = attachmentDTOs.count
        guard totalAttachments > 0 else {
            AppLogger.log(.debug, "[BatchImport] No attachments to import", category: .sync)
            return 0
        }

        AppLogger.log(.info, "[BatchImport] Starting attachment import: \(totalAttachments) attachments", category: .sync)

        let context = ModelContext(modelContainer)

        // Collect all incoming IDs for efficient lookup
        let incomingIds = Set(attachmentDTOs.compactMap { UUID(uuidString: $0.0.id) })

        // Fetch only attachments that match incoming IDs (not ALL attachments)
        let existingAttachments = try context.fetch(FetchDescriptor<Attachment>())
            .filter { incomingIds.contains($0.id) }
        var attachmentLookup = Dictionary(uniqueKeysWithValues: existingAttachments.map { ($0.id, $0) })
        AppLogger.log(.debug, "[BatchImport] Found \(existingAttachments.count) existing attachments matching incoming IDs", category: .sync)

        // Process in batches
        let chunks = attachmentDTOs.chunked(into: batchSize)
        var processedCount = 0
        var createdCount = 0
        var updatedCount = 0
        var skippedCount = 0

        for chunk in chunks {
            for (attachmentDTO, sldId) in chunk {
                guard let attachmentId = UUID(uuidString: attachmentDTO.id),
                      let companyId = UUID(uuidString: attachmentDTO.company_id) else {
                    AppLogger.log(.notice, "[BatchImport] Skipping attachment - invalid UUID: id=\(attachmentDTO.id), company_id=\(attachmentDTO.company_id)", category: .sync)
                    skippedCount += 1
                    continue
                }

                // Parse created_at from server
                let createdAt = parseDate(attachmentDTO.created_at) ?? Date()

                if let existingAttachment = attachmentLookup[attachmentId] {
                    // Update existing
                    existingAttachment.type = attachmentDTO.type
                    existingAttachment.filename = attachmentDTO.filename
                    existingAttachment.fileSize = attachmentDTO.file_size
                    existingAttachment.key = attachmentDTO.key
                    existingAttachment.isDeleted = attachmentDTO.is_deleted
                    existingAttachment.visibility = attachmentDTO.visibility ?? "internal"
                    existingAttachment.visibilityId = attachmentDTO.visibility_id
                    existingAttachment.createdAt = createdAt
                    updatedCount += 1
                } else {
                    // Create new
                    let attachment = Attachment(
                        id: attachmentId,
                        companyId: companyId,
                        sldId: sldId,
                        sessionId: attachmentDTO.session_id != nil ? UUID(uuidString: attachmentDTO.session_id!) : nil,
                        taskId: attachmentDTO.task_id != nil ? UUID(uuidString: attachmentDTO.task_id!) : nil,
                        type: attachmentDTO.type,
                        filename: attachmentDTO.filename,
                        fileSize: attachmentDTO.file_size,
                        key: attachmentDTO.key,
                        isDeleted: attachmentDTO.is_deleted,
                        uploadNeeded: false,
                        visibility: attachmentDTO.visibility ?? "internal",
                        visibilityId: attachmentDTO.visibility_id,
                        createdAt: createdAt
                    )
                    context.insert(attachment)
                    attachmentLookup[attachmentId] = attachment
                    createdCount += 1
                }

                processedCount += 1
            }

            try context.save()
            await onProgress?(processedCount, totalAttachments)
        }

        AppLogger.log(.info, "[BatchImport] Attachment import complete - Total: \(processedCount), Created: \(createdCount), Updated: \(updatedCount), Skipped: \(skippedCount)", category: .sync)

        return processedCount
    }

    /// Import attachment-node mappings.
    func importAttachmentNodeMappings(
        _ mappings: [MappingAttachmentNodeDTO],
        onProgress: ImportProgressCallback? = nil
    ) async throws -> Int {
        let totalMappings = mappings.count
        guard totalMappings > 0 else {
            AppLogger.log(.debug, "[BatchImport] No attachment-node mappings to import", category: .sync)
            return 0
        }

        AppLogger.log(.info, "[BatchImport] Starting attachment-node mapping import: \(totalMappings) mappings", category: .sync)

        let context = ModelContext(modelContainer)

        // Collect incoming keys for efficient lookup
        let incomingKeys = Set(mappings.compactMap { dto -> String? in
            guard let attachmentId = UUID(uuidString: dto.attachment_id),
                  let nodeId = UUID(uuidString: dto.node_id) else { return nil }
            return "\(attachmentId)-\(nodeId)"
        })

        // Build lookups - handle potential duplicates gracefully
        let existingMappings = try context.fetch(FetchDescriptor<AttachmentNodeMapping>())
        var mappingLookup = [String: AttachmentNodeMapping]()
        var duplicateCount = 0
        for mapping in existingMappings {
            let key = "\(mapping.attachmentId)-\(mapping.nodeId)"
            // Only add to lookup if it matches incoming data
            guard incomingKeys.contains(key) else { continue }
            if mappingLookup[key] != nil {
                duplicateCount += 1
            }
            mappingLookup[key] = mapping // Keep the latest one
        }
        AppLogger.log(.debug, "[BatchImport] Found \(mappingLookup.count) existing mappings matching incoming keys", category: .sync)
        if duplicateCount > 0 {
            AppLogger.log(.notice, "[BatchImport] Warning: \(duplicateCount) duplicate mappings found in database", category: .sync)
        }

        var processedCount = 0
        var createdCount = 0
        var updatedCount = 0
        var skippedCount = 0

        for mappingDTO in mappings {
            guard let attachmentId = UUID(uuidString: mappingDTO.attachment_id),
                  let nodeId = UUID(uuidString: mappingDTO.node_id) else {
                AppLogger.log(.notice, "[BatchImport] Skipping mapping - invalid UUID: attachment_id=\(mappingDTO.attachment_id), node_id=\(mappingDTO.node_id)", category: .sync)
                skippedCount += 1
                continue
            }

            let key = "\(attachmentId)-\(nodeId)"

            if let existingMapping = mappingLookup[key] {
                // Update existing
                existingMapping.isDeleted = mappingDTO.is_deleted
                updatedCount += 1
            } else {
                // Create new
                let mapping = AttachmentNodeMapping(
                    attachmentId: attachmentId,
                    nodeId: nodeId,
                    isDeleted: mappingDTO.is_deleted
                )
                context.insert(mapping)
                mappingLookup[key] = mapping
                createdCount += 1
            }

            processedCount += 1

            // Save periodically
            if processedCount % batchSize == 0 {
                try context.save()
                await onProgress?(processedCount, totalMappings)
            }
        }

        try context.save()
        await onProgress?(processedCount, totalMappings)

        AppLogger.log(.info, "[BatchImport] Attachment-node mapping complete - Total: \(processedCount), Created: \(createdCount), Updated: \(updatedCount), Skipped: \(skippedCount)", category: .sync)

        return processedCount
    }

    // MARK: - Batch Deletion

    /// Progress callback: (entityType, processedCount, totalCount)
    typealias DeleteProgressCallback = @Sendable (String, Int, Int) async -> Void

    /// Delete all entities in batches on background context.
    /// Uses intermediate saves to release memory after each entity type.
    /// Uses bulk delete (modelContext.delete(model:)) for entities with all-optional relationships.
    func deleteAllEntities(
        preserveSyncMetadata: Bool = false,
        onProgress: DeleteProgressCallback? = nil
    ) async throws {
        let modelContext = ModelContext(modelContainer)
        let deleteBatchSize = 500
        let progressUpdateInterval = 50

        AppLogger.log(.info, "[BatchDelete] Starting batch deletion on background context...", category: .sync)

        // Helper for bulk delete (fast, no memory loading) - for entities with all-optional relationships
        func bulkDelete<T: PersistentModel>(_ type: T.Type, entityType: String) async throws {
            let count = try modelContext.fetchCount(FetchDescriptor<T>())
            if count == 0 {
                await onProgress?(entityType, 0, 0)
                return
            }
            AppLogger.log(.debug, "[BatchDelete] Bulk deleting \(count) \(entityType)", category: .sync)
            try modelContext.delete(model: type)
            try modelContext.save()
            await onProgress?(entityType, count, count)
        }

        // Helper to delete in batches with intermediate saves (for entities with mandatory relationships)
        func batchDelete<T: PersistentModel>(_ type: T.Type, entityType: String) async throws {
            let descriptor = FetchDescriptor<T>()
            let items = try modelContext.fetch(descriptor)
            let total = items.count

            if items.isEmpty {
                await onProgress?(entityType, 0, 0)
                return
            }

            AppLogger.log(.debug, "[BatchDelete] Deleting \(total) \(entityType)", category: .sync)

            for (index, item) in items.enumerated() {
                modelContext.delete(item)

                // Report progress periodically
                if (index + 1) % progressUpdateInterval == 0 {
                    await onProgress?(entityType, index + 1, total)
                }

                // Save every batch to release memory
                if (index + 1) % deleteBatchSize == 0 {
                    try modelContext.save()
                }
            }
            // Final save and progress update
            try modelContext.save()
            await onProgress?(entityType, total, total)
        }

        // Delete in dependency order
        // Strategy: Delete entities with problematic inverse relationships first (individual delete),
        // then bulk delete remaining entities once their relationships are nullified.

        // Sync items - bulk delete (no relationships).
        // ZP-1847: a non-destructive account switch keeps the queue + log on
        // device for the prior user; they reappear on next login as that
        // user. The new user only sees their own queue (per-user filter in
        // SyncQueueService.getPendingSyncOps).
        if !preserveSyncMetadata {
            try await bulkDelete(SyncQueueItem.self, entityType: "syncItems")
            try await bulkDelete(SyncLog.self, entityType: "syncLogs")
        }

        // Properties - bulk delete (optional relationships only)
        try await bulkDelete(NodeProperty.self, entityType: "nodeProperties")
        try await bulkDelete(EdgeProperty.self, entityType: "edgeProperties")
        try await bulkDelete(IssueProperty.self, entityType: "issueProperties")

        // IRPhoto - individual delete (has mandatory node/sld relationships)
        try await batchDelete(IRPhoto.self, entityType: "irPhotos")

        try await batchDelete(FormInstance.self, entityType: "formInstances")
        try await batchDelete(UserTask.self, entityType: "tasks")
        try await batchDelete(UserTaskForm.self, entityType: "forms")

        try await batchDelete(Issue.self, entityType: "issues")
        try await batchDelete(Quote.self, entityType: "quotes")
        try await batchDelete(IRSession.self, entityType: "sessions")
        try await batchDelete(SLDComment.self, entityType: "comments")

        try await batchDelete(EdgeV2.self, entityType: "edges")

        // NodeTerminal - delete before nodes (terminals reference nodes)
        try await batchDelete(NodeTerminal.self, entityType: "nodeTerminals")

        try await batchDelete(NodeV2.self, entityType: "nodes")

        try await batchDelete(Room.self, entityType: "rooms")
        try await batchDelete(Floor.self, entityType: "floors")
        try await batchDelete(Building.self, entityType: "buildings")

        // Attachments - bulk delete (no inverse relationships)
        try await bulkDelete(AttachmentNodeMapping.self, entityType: "attachmentNodeMappings")
        try await bulkDelete(Attachment.self, entityType: "attachments")

        // SLD views and links - delete before SLDV2 (views/links reference SLDs)
        try await bulkDelete(SLDLinkV2.self, entityType: "sldLinks")
        try await bulkDelete(SLDViewV2.self, entityType: "sldViews")

        try await batchDelete(SLDV2.self, entityType: "slds")

        // Node orientation data - delete orientations first (cascade will handle terminals)
        // Note: NodeOrientation has cascade delete rule for terminals, so deleting orientations
        // will automatically delete associated terminals. Delete orientations using batchDelete
        // (individual deletion) to properly trigger cascade behavior.
        try await batchDelete(NodeOrientation.self, entityType: "nodeOrientations")
        // Terminals should now be deleted via cascade, but clean up any orphans just in case
        try await batchDelete(NodeOrientationTerminal.self, entityType: "nodeOrientationTerminals")
        try await bulkDelete(NodeIcon.self, entityType: "nodeIcons")

        // Photo - bulk delete LAST (after all related entities: node, userTask, issue, building, floor, room, sld)
        // With all inverse relationships deleted, bulk delete works
        try await bulkDelete(Photo.self, entityType: "photos")

        AppLogger.log(.info, "[BatchDelete] Batch deletion complete", category: .sync)
    }

    // MARK: - Date Parsing Helper

    /// Parse ISO8601 date string from server
    private func parseDate(_ dateString: String) -> Date? {
        // Try ISO8601 format with fractional seconds first
        let iso8601Formatter = ISO8601DateFormatter()
        iso8601Formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso8601Formatter.date(from: dateString) {
            return date
        }

        // Try without fractional seconds
        iso8601Formatter.formatOptions = [.withInternetDateTime]
        if let date = iso8601Formatter.date(from: dateString) {
            return date
        }

        // Fallback to DateFormatter
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        if let date = formatter.date(from: dateString) {
            return date
        }

        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
        return formatter.date(from: dateString)
    }
}

// MARK: - Import Errors

enum ImportError: Error, LocalizedError {
    case sldNotFound(UUID)

    var errorDescription: String? {
        switch self {
        case .sldNotFound(let id):
            return "SLD not found: \(id)"
        }
    }
}

