import Foundation

/// ZP-1847 — Self-contained sync queue snapshots.
///
/// Two responsibilities live in this extension:
///
/// 1. `buildSnapshotPayload(for:)` — synchronously build the HTTP request body
///    that the corresponding `APIClient.create*` / `update*` call would send,
///    without sending it. Called at enqueue time; the resulting bytes are
///    persisted on `SyncQueueItem.snapshotJSON`.
///
/// 2. `replaySnapshotPayload(target:operation:entityId:payload:)` — at flush
///    time, replay a previously captured payload to the original endpoint.
///    Wire format is byte-identical to a live call because the bytes are
///    literally what the live call would have produced.
///
/// The existing `create*`/`update*` methods are unchanged — they keep doing
/// their own inline build+send. The snapshot helpers here mirror the same
/// build code so that the snapshot bytes match what the live methods send.
@MainActor
extension APIClient {

    // MARK: - Public entry points

    /// Build the request body that would be sent for `op`. Returns nil for
    /// mappings (handled by `mappingDataJSON` instead) and for ops that have
    /// no body (e.g. attachment hard-delete) or unsupported targets.
    func buildSnapshotPayload(for op: SyncOp) throws -> Data? {
        guard !op.target.isMapping else { return nil }

        switch op.target {
        case .node:
            guard let node = op.node else { return nil }
            switch op.operation {
            case .create:           return try snapshotNodeCreateBody(node)
            case .update, .delete:  return try buildNodeUpdatePayload(node, extraData: op.extraData ?? [:])
            default:                return nil
            }

        case .edge:
            guard let edge = op.edge else { return nil }
            switch op.operation {
            case .create:           return try snapshotEdgeCreateBody(edge)
            case .update, .delete:  return try buildEdgeUpdatePayload(edge)
            default:                return nil
            }

        case .photo:
            guard let photo = op.photo else { return nil }
            switch op.operation {
            case .create:           return try snapshotPhotoCreateBody(photo)
            case .update, .delete:  return try snapshotPhotoUpdateBody(photo)
            default:                return nil
            }

        case .userTask:
            guard let task = op.userTask else { return nil }
            switch op.operation {
            case .create:           return try snapshotUserTaskCreateBody(task)
            case .update, .delete:  return try snapshotUserTaskUpdateBody(task)
            default:                return nil
            }

        case .irSession:
            guard let s = op.irSession else { return nil }
            switch op.operation {
            case .create:           return try snapshotIRSessionCreateBody(s)
            case .update, .delete:  return try snapshotIRSessionUpdateBody(s)
            default:                return nil
            }

        case .irPhoto:
            guard let p = op.irPhoto else { return nil }
            switch op.operation {
            case .create:           return try snapshotIRPhotoCreateBody(p)
            case .update, .delete:  return try snapshotIRPhotoUpdateBody(p)
            default:                return nil
            }

        case .issue:
            guard let issue = op.issue else { return nil }
            switch op.operation {
            case .create:           return try snapshotIssueCreateBody(issue)
            case .update, .delete:  return try snapshotIssueUpdateBody(issue)
            default:                return nil
            }

        case .quote:
            guard let q = op.quote else { return nil }
            switch op.operation {
            case .create:           return try snapshotQuoteCreateBody(q)
            case .update, .delete:  return try snapshotQuoteUpdateBody(q)
            default:                return nil
            }

        case .formInstance:
            guard let fi = op.formInstance else { return nil }
            // FormInstance already exposes a clean toDTO() — encode it the
            // same way the live createFormInstance does.
            let dto = fi.toDTO()
            return try JSONEncoder().encode(dto)

        case .egFormInstance:
            guard let inst = op.egFormInstance else { return nil }
            // Build the same wire body the live flow uses so flush can
            // replay it even if the entity has been wiped from SwiftData
            // (ZP-1847: site switch / logout / fresh install scenarios).
            let submissionAny: AnyCodable? = {
                guard let s = inst.form_submission, !s.isEmpty,
                      let d = s.data(using: .utf8),
                      let parsed = try? JSONSerialization.jsonObject(with: d) else { return nil }
                return AnyCodable(parsed)
            }()
            switch op.operation {
            case .create:
                // ZP-2425: prefer task_id/node_id captured on extraData at
                // enqueue time. `inst.linkedTasks` reflects the implicit
                // SwiftData inverse of `task.linkedEGFormInstances` and can
                // be stale or empty when the linkage is established on the
                // task side. Fallback keeps older queue rows working.
                let taskId = (op.extraData?["task_id"] as? String)
                    ?? inst.linkedTasks.first?.id.uuidString
                let nodeId = (op.extraData?["node_id"] as? String)
                    ?? inst.linkedNodes.first?.id.uuidString
                let body = EGFormInstanceCreateBody(
                    id: inst.id.uuidString,
                    eg_form_id: inst.eg_form_id.uuidString,
                    form_submission: submissionAny,
                    submitted: inst.submitted,
                    task_id: taskId,
                    node_id: nodeId
                )
                return try JSONEncoder().encode(body)
            case .update, .delete:
                let nodeIds = inst.linkedNodes.map { $0.id.uuidString }
                let body = EGFormInstanceUpdateBody(
                    form_submission: submissionAny,
                    submitted: inst.submitted,
                    is_deleted: (op.operation == .delete) ? true : inst.is_deleted,
                    // ZP-2363: send the current set verbatim, including `[]`.
                    // This snapshot is the body replayed by the fast-path, so
                    // collapsing empty to nil ("no change") would silently drop
                    // an offline "remove all linked assets". `inst.linkedNodes`
                    // is authoritative (create never diverges node_id from it),
                    // so `[]` is a correct full-replace — matching online/web.
                    node_ids: nodeIds
                )
                return try JSONEncoder().encode(body)
            default:
                return nil
            }

        case .building:
            guard let b = op.building else { return nil }
            return op.operation == .create
                ? try snapshotBuildingCreateBody(b)
                : try snapshotBuildingUpdateBody(b)

        case .floor:
            guard let f = op.floor else { return nil }
            return op.operation == .create
                ? try snapshotFloorCreateBody(f)
                : try snapshotFloorUpdateBody(f)

        case .room:
            guard let r = op.room else { return nil }
            return op.operation == .create
                ? try snapshotRoomCreateBody(r)
                : try snapshotRoomUpdateBody(r)

        case .attachment:
            guard let a = op.attachment else { return nil }
            switch op.operation {
            case .create:
                let payload = AttachmentCreateRequest(attachment: a)
                return try JSONEncoder().encode(payload)
            case .delete:
                // Hard delete — no body, the URL alone identifies the row.
                return nil
            default:
                return nil
            }

        case .sldView:
            guard let v = op.sldView else { return nil }
            return op.operation == .create
                ? try snapshotSLDViewCreateBody(v)
                : try snapshotSLDViewUpdateBody(v)

        default:
            return nil
        }
    }

    /// Replay a previously captured payload by re-sending it to the original
    /// endpoint. `payload` may be nil for ops that send no body (e.g.
    /// attachment hard-delete).
    func replaySnapshotPayload(target: SyncTarget,
                               operation: SyncOperation,
                               entityId: UUID,
                               payload: Data?) async throws -> URLResponse {
        let endpoint = try snapshotEndpoint(target: target, operation: operation, entityId: entityId)
        var request = await createAuthorizedRequest(url: endpoint.url, method: endpoint.method)
        if let payload = payload, endpoint.method != "DELETE" {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = payload
        }
        return try await executeRequestRaw(request)
    }

    // MARK: - URL + method resolver

    private struct SnapshotEndpoint {
        let url: URL
        let method: String
    }

    private func snapshotEndpoint(target: SyncTarget,
                                  operation: SyncOperation,
                                  entityId: UUID) throws -> SnapshotEndpoint {
        let id = entityId.uuidString

        // For most targets, .delete is implemented as PUT /update with
        // is_deleted=true on the body. Attachment is the only hard-delete.
        switch (target, operation) {
        case (.node, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Node.create), method: "POST")
        case (.node, .update), (.node, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Node.update(nodeId: id)), method: "PUT")

        case (.edge, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Edge.create), method: "POST")
        case (.edge, .update), (.edge, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Edge.update(edgeId: id)), method: "PUT")

        case (.photo, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Photo.create), method: "POST")
        case (.photo, .update), (.photo, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Photo.update(photoId: id)), method: "PUT")

        case (.userTask, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Task.create), method: "POST")
        case (.userTask, .update), (.userTask, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Task.update(taskId: id)), method: "PUT")

        case (.irSession, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.IRSession.create), method: "POST")
        case (.irSession, .update), (.irSession, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.IRSession.update(sessionId: id)), method: "PUT")

        case (.irPhoto, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.IRPhoto.create), method: "POST")
        case (.irPhoto, .update), (.irPhoto, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.IRPhoto.update(photoId: id)), method: "PUT")

        case (.issue, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Issue.create), method: "POST")
        case (.issue, .update), (.issue, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Issue.update(issueId: id)), method: "PUT")

        case (.quote, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Quote.create), method: "POST")
        case (.quote, .update), (.quote, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Quote.update(quoteId: id)), method: "PUT")

        case (.formInstance, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.FormInstance.create), method: "POST")
        case (.formInstance, .update), (.formInstance, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.FormInstance.update(instanceId: id)), method: "PUT")

        case (.egFormInstance, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.EGFormInstance.create), method: "POST")
        case (.egFormInstance, .update), (.egFormInstance, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.EGFormInstance.update(instanceId: id)), method: "PUT")

        case (.building, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.buildingCreate), method: "POST")
        case (.building, .update), (.building, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.buildingUpdate(buildingId: id)), method: "PUT")

        case (.floor, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.floorCreate), method: "POST")
        case (.floor, .update), (.floor, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.floorUpdate(floorId: id)), method: "PUT")

        case (.room, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.roomCreate), method: "POST")
        case (.room, .update), (.room, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Location.roomUpdate(roomId: id)), method: "PUT")

        case (.attachment, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Attachment.create), method: "POST")
        case (.attachment, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.Attachment.delete(attachmentId: id)), method: "DELETE")

        case (.sldView, .create):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.SLDView.create), method: "POST")
        case (.sldView, .update), (.sldView, .delete):
            return .init(url: baseURL.appendingPathComponent(APIEndpoints.SLDView.update(viewId: id)), method: "PUT")

        default:
            throw NSError(domain: "APIClient.snapshotEndpoint", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Unsupported target/operation for snapshot replay: \(target)/\(operation)"
            ])
        }
    }

    // MARK: - Per-entity body builders
    //
    // Each helper mirrors the inline body-build code in the corresponding
    // create*/update* method. They are kept in lockstep with those methods —
    // any change to a wire field there must be mirrored here so the snapshot
    // bytes match a live call.

    // MARK: Node

    private func snapshotNodeCreateBody(_ node: NodeV2) throws -> Data {
        guard let sldId = node.sld?.id else {
            throw NSError(domain: "APIClient.snapshot", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Node \(node.id) has no SLD relationship"
            ])
        }

        var payload = SLDDTONode(
            id: node.id, type: node.type, label: node.label, sld_id: sldId,
            parent_id: node.parent_id,
            x: node.x, y: node.y, width: node.width, height: node.height,
            is_deleted: node.is_deleted,
            location: node.location,
            node_class: node.node_class?.id,
            node_subtype: node.node_subtype?.id,
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
            voltage: node.voltage,
            voltage_id: node.voltage_id,
            secondary_voltage: node.secondary_voltage,
            secondary_voltage_id: node.secondary_voltage_id,
            notes: node.notes,
            room_id: node.room?.id,
            default_photo_id: node.default_photo_id,
            suggested_shortcut: node.suggested_shortcut_id,
            eqp_lib: node.eqp_lib
        )

        // ZP-2419: thread every engineering field through to the snapshot
        // create body so offline-queued asset creates persist server-side.
        // Mirrors the live createNode/updateNode payload.
        payload.tertiary_voltage = node.tertiary_voltage
        payload.tertiary_voltage_id = node.tertiary_voltage_id
        payload.system_voltage_id = node.system_voltage_id
        payload.circuit_voltage_id = node.circuit_voltage_id
        payload.voltage_user_overridden = node.voltage_user_overridden
        payload.applied_shortcut = node.applied_shortcut_id
        payload.eqp_lib_suggested = node.eqp_lib_suggested
        payload.eqp_note = node.eqp_note
        payload.eqp_engineering_approved = node.eqp_engineering_approved
        payload.skm_lib_name = node.skm_lib_name
        payload.skm_lib_name_suggested = node.skm_lib_name_suggested
        payload.ocr_signature = node.ocr_signature
        payload.kva_rating = node.kva_rating
        payload.percent_impedance = node.percent_impedance
        payload.mains_type_id = node.mains_type_id
        payload.phase_configuration_id = node.phase_configuration_id
        payload.ampere_rating = node.ampere_rating
        payload.pole_count = node.pole_count
        payload.manufacturer_id = node.manufacturer_id
        payload.has_trip_unit = node.has_trip_unit
        payload.trip_type_id = node.trip_type_id
        payload.frame_amps = node.frame_amps
        payload.sensor_amps = node.sensor_amps
        payload.plug_amps = node.plug_amps
        payload.length = node.length
        payload.conductor_material = node.conductor_material
        payload.cable_size_id = node.cable_size_id
        payload.conductor_configuration_id = node.conductor_configuration_id
        payload.duct_material_id = node.duct_material_id
        payload.conductor_description_id = node.conductor_description_id
        payload.insulation_class_id = node.insulation_class_id
        payload.insulation_type_id = node.insulation_type_id
        payload.installation_id = node.installation_id
        payload.busway_ampere_rating = node.busway_ampere_rating
        payload.replacement_cost = node.replacement_cost
        payload.panel_schedule_status = node.panel_schedule_status
        payload.rotation = node.rotation
        payload.locked = node.locked

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let encoded = try encoder.encode(payload)
        var jsonObj = (try JSONSerialization.jsonObject(with: encoded) as? [String: Any]) ?? [:]
        if node.com == nil { jsonObj["com"] = NSNull() }
        if node.com_calculation == nil { jsonObj["com_calculation"] = NSNull() }
        if let s = node.eqp_lib, let d = s.data(using: .utf8),
           let parsed = try? JSONSerialization.jsonObject(with: d) {
            jsonObj["eqp_lib"] = parsed
        }
        return try JSONSerialization.data(withJSONObject: jsonObj)
    }

    // MARK: Edge

    private func snapshotEdgeCreateBody(_ edge: EdgeV2) throws -> Data {
        guard let sldId = edge.sld?.id else {
            throw NSError(domain: "APIClient.snapshot", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Edge \(edge.id) has no SLD relationship"
            ])
        }
        let payload = SLDDTOEdge(
            id: edge.id, source: edge.source, target: edge.target,
            sld_id: sldId,
            source_handle: edge.sourceHandle,
            target_handle: edge.targetHandle,
            source_node_terminal_id: edge.sourceNodeTerminalId,
            target_node_terminal_id: edge.targetNodeTerminalId,
            is_deleted: edge.is_deleted,
            edge_class: edge.edge_class?.id,
            core_attributes: edge.core_attributes.map { attr in
                EdgePropertyDTO(
                    id: attr.id,
                    edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value
                )
            },
            points: edge.points,
            algorithm: edge.algorithm
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    // MARK: Photo

    private func resolvePhotoEntityId(_ photo: Photo) -> UUID? {
        if photo.type.hasPrefix("task_") { return photo.userTask?.id }
        if photo.type.hasPrefix("issue") { return photo.issue?.id }
        switch photo.type {
        case "building": return photo.building?.id
        case "floor": return photo.floor?.id
        case "room": return photo.room?.id
        default: return photo.node?.id
        }
    }

    private func snapshotPhotoCreateBody(_ photo: Photo) throws -> Data {
        let payload = SLDDTOPhoto(
            id: photo.id,
            entity_id: resolvePhotoEntityId(photo),
            url: photo.url,
            type: photo.type,
            sld_id: photo.sld?.id,
            filename: photo.filename,
            local_filepath: photo.local_filepath,
            upload_needed: false,
            is_deleted: false,
            caption: photo.caption
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotPhotoUpdateBody(_ photo: Photo) throws -> Data {
        let payload = SLDDTOPhoto(
            id: photo.id,
            entity_id: resolvePhotoEntityId(photo),
            url: photo.url,
            type: photo.type,
            filename: photo.filename,
            local_filepath: photo.local_filepath,
            upload_needed: false,
            is_deleted: photo.is_deleted,
            caption: photo.caption
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    // MARK: UserTask

    private func snapshotUserTaskCreateBody(_ task: UserTask) throws -> Data {
        guard let sldId = task.sld?.id else {
            throw NSError(domain: "APIClient.snapshot", code: 400, userInfo: [
                NSLocalizedDescriptionKey: "Task \(task.id) has no SLD relationship"
            ])
        }
        let payload = UserTaskDTO(
            id: task.id,
            title: task.title,
            task_description: task.task_description,
            completed: task.completed,
            form_id: task.form?.id,
            node_id: task.node?.id,
            sld_id: sldId,
            is_deleted: task.is_deleted,
            submission: task.submission,
            submitted_at: task.submitted_at,
            due_date: task.due_date,
            created_at: task.created_at,
            task_type: task.task_type,
            interval: task.interval,
            recurring: task.recurring,
            procedure_id: task.procedure_id,
            shortcut_id: task.shortcut_id
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotUserTaskUpdateBody(_ task: UserTask) throws -> Data {
        // updateTask routes through JSONSerialization round-trip; keep the
        // shape identical here.
        return try snapshotUserTaskCreateBody(task)
    }

    // MARK: IR Session

    private func snapshotIRSessionCreateBody(_ s: IRSession) throws -> Data {
        let equipmentIdStrings = s.equipmentIds.map { $0.uuidString.lowercased() }
        let payload = IRSessionDTO(
            id: s.id,
            name: s.name,
            photo_type: s.photo_type,
            active_visual_prefix: s.active_visual_prefix,
            active_ir_prefix: s.active_ir_prefix,
            date_created: s.date_created,
            date_closed: s.date_closed,
            active: s.active,
            sld_id: s.sld.id,
            is_deleted: s.is_deleted,
            equipment_ids: equipmentIdStrings
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotIRSessionUpdateBody(_ s: IRSession) throws -> Data {
        return try snapshotIRSessionCreateBody(s)
    }

    // MARK: IR Photo

    private func snapshotIRPhotoCreateBody(_ p: IRPhoto) throws -> Data {
        let payload = IRPhotoDTO(
            id: p.id,
            ir_session_id: p.ir_session?.id,
            node_id: p.node.id,
            visual_photo_key: p.visual_photo_key,
            ir_photo_key: p.ir_photo_key,
            date_created: p.date_created,
            sld_id: p.sld.id,
            issue_id: p.issue?.id,
            is_deleted: p.is_deleted
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotIRPhotoUpdateBody(_ p: IRPhoto) throws -> Data {
        // updateIRPhoto deliberately uses a raw dict so cleared FKs are sent
        // as JSON null. Mirror that here so snapshot bytes match.
        let payload: [String: Any] = [
            "id": p.id.uuidString,
            "ir_session_id": p.ir_session?.id.uuidString ?? NSNull(),
            "issue_id": p.issue?.id.uuidString ?? NSNull(),
            "node_id": p.node.id.uuidString,
            "visual_photo_key": p.visual_photo_key,
            "ir_photo_key": p.ir_photo_key,
            "date_created": ISO8601DateFormatter().string(from: p.date_created),
            "sld_id": p.sld.id.uuidString,
            "is_deleted": p.is_deleted
        ]
        return try JSONSerialization.data(withJSONObject: payload, options: [])
    }

    // MARK: Issue

    private func snapshotIssueCreateBody(_ issue: Issue) throws -> Data {
        let payload = IssueDTO(
            id: issue.id,
            title: issue.title,
            description: issue.issueDescription,
            created_date: issue.created_date,
            node_id: issue.node?.id,
            issue_class: issue.issue_class?.id,
            issue_type: issue.issue_type,
            issue_subtype: issue.issue_subtype,
            is_deleted: issue.is_deleted,
            session_id: issue.session?.id,
            sld_id: issue.sld?.id,
            details: issue.details.map { attr in
                IssuePropertyDTO(
                    id: attr.id,
                    issue_class_property: attr.issue_class_property?.id.uuidString ?? "",
                    name: attr.name,
                    value: attr.value,
                    unit: attr.unit,
                    description: attr.attributeNotes
                )
            },
            status: issue.status,
            proposed_resolution: issue.proposed_resolution,
            modified_date: issue.modified_date,
            priority: issue.priority,
            immediate_hazard: issue.immediateHazard,
            customer_notified: issue.customerNotified
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotIssueUpdateBody(_ issue: Issue) throws -> Data {
        return try snapshotIssueCreateBody(issue)
    }

    // MARK: Quote

    private func snapshotQuoteCreateBody(_ q: Quote) throws -> Data {
        let payload = QuoteDTO(
            id: q.id,
            title: q.title,
            sow: q.sow,
            tnm: q.tnm,
            sld_id: q.sld?.id,
            description: q.quoteDescription,
            status: q.status,
            is_deleted: q.is_deleted
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(payload)
    }

    private func snapshotQuoteUpdateBody(_ q: Quote) throws -> Data {
        return try snapshotQuoteCreateBody(q)
    }

    // MARK: Building / Floor / Room

    private func snapshotBuildingCreateBody(_ b: Building) throws -> Data {
        let payload: [String: Any] = [
            "id": b.id.uuidString,
            "name": b.name,
            "sld_id": b.sld?.id.uuidString ?? "",
            "is_deleted": b.is_deleted
        ]
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotBuildingUpdateBody(_ b: Building) throws -> Data {
        var payload: [String: Any] = [
            "id": b.id.uuidString,
            "name": b.name,
            "is_deleted": b.is_deleted
        ]
        if let notes = b.access_notes { payload["access_notes"] = notes }
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotFloorCreateBody(_ f: Floor) throws -> Data {
        let payload: [String: Any] = [
            "id": f.id.uuidString,
            "name": f.name,
            "building_id": f.building?.id.uuidString ?? "",
            "is_deleted": f.is_deleted
        ]
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotFloorUpdateBody(_ f: Floor) throws -> Data {
        var payload: [String: Any] = [
            "id": f.id.uuidString,
            "name": f.name,
            "building_id": f.building?.id.uuidString ?? "",
            "is_deleted": f.is_deleted
        ]
        if let notes = f.access_notes { payload["access_notes"] = notes }
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotRoomCreateBody(_ r: Room) throws -> Data {
        let payload: [String: Any] = [
            "id": r.id.uuidString,
            "name": r.name,
            "floor_id": r.floor?.id.uuidString ?? "",
            "is_deleted": r.is_deleted
        ]
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotRoomUpdateBody(_ r: Room) throws -> Data {
        var payload: [String: Any] = [
            "id": r.id.uuidString,
            "name": r.name,
            "floor_id": r.floor?.id.uuidString ?? "",
            "is_deleted": r.is_deleted
        ]
        if let notes = r.access_notes { payload["access_notes"] = notes }
        return try JSONSerialization.data(withJSONObject: payload)
    }

    // MARK: SLDView

    private func snapshotSLDViewCreateBody(_ v: SLDViewV2) throws -> Data {
        let payload: [String: Any] = [
            "id": v.id.uuidString,
            "sld_id": v.sld_id.uuidString,
            "name": v.name,
            "description": v.viewDescription ?? "",
            "view_type": v.view_type
        ]
        return try JSONSerialization.data(withJSONObject: payload)
    }

    private func snapshotSLDViewUpdateBody(_ v: SLDViewV2) throws -> Data {
        let payload: [String: Any] = [
            "name": v.name,
            "description": v.viewDescription ?? ""
        ]
        return try JSONSerialization.data(withJSONObject: payload)
    }
}
