//
//  SyncQueueExportService.swift
//  Egalvanic PZ
//
//  Service for exporting sync queue items as API request JSON
//

import Foundation
import SwiftData
import UIKit

class SyncQueueExportService {

    // MARK: - Export Data Structure

    struct ExportedQueueItem: Codable {
        let queueItemId: String
        let target: String
        let operation: String
        let createdAt: String
        let retryCount: Int
        let httpMethod: String
        let endpoint: String
        let headers: [String: String]
        let bodyJSON: String?
        let entityId: String?
        let entityStatus: String
        // ZP-1847 — per-row snapshot context. `snapshotJSONBase64` carries the
        // captured request body bytes so support can replay an item from an
        // export even after the device's entity tables were wiped.
        let userId: String?
        let siteId: String?
        let snapshotTimestamp: String?
        let snapshotJSONBase64: String?
        let photoFilePath: String?
    }

    struct DeviceInfo: Codable {
        let deviceModel: String
        let osVersion: String
        let appVersion: String
        let userId: String?
    }

    struct ExportReport: Codable {
        let exportDate: String
        let totalItems: Int
        let deviceInfo: DeviceInfo
        let queueItems: [ExportedQueueItem]
    }

    // MARK: - Public Methods

    /// Generate export report for all pending sync queue items
    static func generateExportReport(
        queueItems: [SyncQueueItem],
        modelContext: ModelContext
    ) -> ExportReport {
        let exportedItems = queueItems.compactMap { item in
            generateExportedItem(for: item, modelContext: modelContext)
        }

        let dateFormatter = ISO8601DateFormatter()

        return ExportReport(
            exportDate: dateFormatter.string(from: Date()),
            totalItems: exportedItems.count,
            deviceInfo: DeviceInfo(
                deviceModel: UIDevice.current.model,
                osVersion: UIDevice.current.systemVersion,
                appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "Unknown",
                userId: AppStateManager.shared.userId.uuidString
            ),
            queueItems: exportedItems
        )
    }

    /// Generate JSON string from export report
    static func generateJSONString(from report: ExportReport) -> String? {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]

        guard let data = try? encoder.encode(report),
              let jsonString = String(data: data, encoding: .utf8) else {
            return nil
        }

        return jsonString
    }

    /// Save export report to temporary file and return URL for sharing
    static func saveExportToTempFile(jsonString: String) -> URL? {
        let tempDir = FileManager.default.temporaryDirectory
        let timestamp = DateFormatter.localizedString(from: Date(), dateStyle: .short, timeStyle: .short)
            .replacingOccurrences(of: "/", with: "-")
            .replacingOccurrences(of: ":", with: "-")
            .replacingOccurrences(of: " ", with: "_")

        let filename = "sync_queue_export_\(timestamp).json"
        let fileURL = tempDir.appendingPathComponent(filename)

        do {
            try jsonString.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            AppLogger.log(.error, "Failed to save export file: \(error)", category: .sync)
            return nil
        }
    }

    // MARK: - Private Methods

    private static func generateExportedItem(
        for queueItem: SyncQueueItem,
        modelContext: ModelContext
    ) -> ExportedQueueItem? {
        // Fetch the entity from the database
        let entity = fetchEntity(for: queueItem, context: modelContext)

        // Determine entity status
        let entityStatus: String
        if entity == nil {
            entityStatus = "MISSING - Entity not found in local database"
        } else {
            entityStatus = "Found in local database"
        }

        // Get entity ID
        let entityId = getEntityId(for: queueItem)

        // Build the API request details
        guard let (httpMethod, endpoint, body) = buildAPIRequest(for: queueItem, entity: entity) else {
            return nil
        }

        // Get headers
        let headers = getHeaders()

        // Convert body to JSON string
        let bodyJSON: String?
        if let body = body {
            if let jsonData = try? JSONSerialization.data(withJSONObject: body, options: [.prettyPrinted, .sortedKeys]),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                bodyJSON = jsonString
            } else {
                bodyJSON = "\(body)"
            }
        } else {
            bodyJSON = nil
        }

        let dateFormatter = ISO8601DateFormatter()

        return ExportedQueueItem(
            queueItemId: queueItem.id.uuidString,
            target: targetToString(queueItem.target),
            operation: operationToString(queueItem.operation),
            createdAt: dateFormatter.string(from: queueItem.createdAt),
            retryCount: queueItem.retryCount,
            httpMethod: httpMethod,
            endpoint: endpoint,
            headers: headers,
            bodyJSON: bodyJSON,
            entityId: entityId?.uuidString,
            entityStatus: entityStatus,
            userId: queueItem.userId?.uuidString,
            siteId: queueItem.siteId?.uuidString,
            snapshotTimestamp: queueItem.snapshotTimestamp.map { dateFormatter.string(from: $0) },
            snapshotJSONBase64: queueItem.snapshotJSON?.base64EncodedString(),
            photoFilePath: queueItem.photoFilePath
        )
    }

    private static func targetToString(_ target: SyncTarget) -> String {
        switch target {
        case .node: return "node"
        case .edge: return "edge"
        case .photo: return "photo"
        case .userTask: return "userTask"
        case .irPhoto: return "irPhoto"
        case .irSession: return "irSession"
        case .issue: return "issue"
        case .quote: return "quote"
        case .mappingIssueTask: return "mappingIssueTask"
        case .mappingTaskSession: return "mappingTaskSession"
        case .mappingQuoteTask: return "mappingQuoteTask"
        case .mappingUserTask: return "mappingUserTask"
        case .mappingTaskNode: return "mappingTaskNode"
        case .mappingTaskForm: return "mappingTaskForm"
        case .formInstance: return "formInstance"
        case .mappingTaskFormInstance: return "mappingTaskFormInstance"
        case .mappingFormInstanceNode: return "mappingFormInstanceNode"
        case .mappingNodeSession: return "mappingNodeSession"
        case .building: return "building"
        case .floor: return "floor"
        case .room: return "room"
        case .attachment: return "attachment"
        case .mappingAttachmentNode: return "mappingAttachmentNode"
        case .mappingNodeSLDView: return "mappingNodeSLDView"
        case .mappingEdgeSLDView: return "mappingEdgeSLDView"
        case .sldView: return "sldView"
        case .mappingUserSession: return "mappingUserSession"
        case .mappingTaskNodeBulkCompletion: return "mappingTaskNodeBulkCompletion"
        case .egFormInstance: return "egFormInstance"
        case .mappingTaskEGFormInstance: return "mappingTaskEGFormInstance"
        case .mappingEGFormInstanceNode: return "mappingEGFormInstanceNode"
        }
    }

    private static func operationToString(_ operation: SyncOperation) -> String {
        switch operation {
        case .create: return "create"
        case .read: return "read"
        case .update: return "update"
        case .delete: return "delete"
        }
    }

    private static func getEntityId(for queueItem: SyncQueueItem) -> UUID? {
        switch queueItem.target {
        case .node: return queueItem.nodeId
        case .edge: return queueItem.edgeId
        case .photo: return queueItem.photoId
        case .userTask: return queueItem.userTaskId
        case .irPhoto: return queueItem.irPhotoId
        case .irSession: return queueItem.irSessionId
        case .issue: return queueItem.issueId
        case .quote: return queueItem.quoteId
        case .formInstance: return queueItem.formInstanceId
        case .egFormInstance: return queueItem.egFormInstanceId
        case .building: return queueItem.buildingId
        case .floor: return queueItem.floorId
        case .room: return queueItem.roomId
        case .attachment: return queueItem.attachmentId
        case .sldView: return queueItem.sldViewId
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask,
             .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode,
             .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView,
             .mappingUserSession, .mappingTaskNodeBulkCompletion,
             .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            // Mappings don't have a single ID - they use composite keys from mappingData
            return nil
        }
    }

    private static func buildAPIRequest(
        for queueItem: SyncQueueItem,
        entity: Any?
    ) -> (method: String, endpoint: String, body: [String: Any]?)? {

        let baseURL = CompanyConfigService.shared.getCurrentInvokeURL()

        switch queueItem.target {
        case .node:
            return buildNodeRequest(queueItem: queueItem, entity: entity as? NodeV2, baseURL: baseURL)

        case .edge:
            return buildEdgeRequest(queueItem: queueItem, entity: entity as? EdgeV2, baseURL: baseURL)

        case .photo:
            return buildPhotoRequest(queueItem: queueItem, entity: entity as? Photo, baseURL: baseURL)

        case .userTask:
            return buildTaskRequest(queueItem: queueItem, entity: entity as? UserTask, baseURL: baseURL)

        case .irPhoto:
            return buildIRPhotoRequest(queueItem: queueItem, entity: entity as? IRPhoto, baseURL: baseURL)

        case .irSession:
            return buildIRSessionRequest(queueItem: queueItem, entity: entity as? IRSession, baseURL: baseURL)

        case .issue:
            return buildIssueRequest(queueItem: queueItem, entity: entity as? Issue, baseURL: baseURL)

        case .quote:
            return buildQuoteRequest(queueItem: queueItem, entity: entity as? Quote, baseURL: baseURL)

        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask,
             .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession:
            return buildMappingRequest(queueItem: queueItem, baseURL: baseURL)

        case .attachment:
            return buildAttachmentRequest(queueItem: queueItem, entity: entity as? Attachment, baseURL: baseURL)

        case .mappingAttachmentNode:
            return buildMappingRequest(queueItem: queueItem, baseURL: baseURL)

        case .mappingTaskNodeBulkCompletion:
            return buildBulkCompletionRequest(queueItem: queueItem, baseURL: baseURL)

        default:
            return nil
        }
    }

    private static func buildBulkCompletionRequest(queueItem: SyncQueueItem, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let mappingData = queueItem.mappingData,
              let taskId = mappingData.taskId,
              let bulkCompletionsJson = mappingData.bulkCompletions,
              let jsonData = bulkCompletionsJson.data(using: .utf8),
              let completionsArray = try? JSONSerialization.jsonObject(with: jsonData) as? [[String: Any]] else {
            return nil
        }

        let endpoint = "\(baseURL)/mapping/task-node/completion"
        let body: [String: Any] = [
            "task_id": taskId.uuidString,
            "completions": completionsArray
        ]

        return ("PUT", endpoint, body)
    }

    private static func buildNodeRequest(queueItem: SyncQueueItem, entity: NodeV2?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let node = entity else {
            return ("PUT", "\(baseURL)/node/update/\(queueItem.nodeId?.uuidString ?? "MISSING")", ["error": "Node not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/node/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/node/update/\(node.id.uuidString)"
        case .delete:
            method = "DELETE"
            endpoint = "\(baseURL)/node/delete/\(node.id.uuidString)"
        default:
            return nil
        }

        // Build node DTO payload
        var body: [String: Any] = [
            "id": node.id.uuidString,
            "type": node.type,
            "label": node.label ?? "",
            "sld_id": node.sld?.id.uuidString ?? "",
            "x": node.x,
            "y": node.y,
            "width": node.width,
            "height": node.height,
            "is_deleted": node.is_deleted
        ]

        if let parentId = node.parent_id {
            body["parent_id"] = parentId.uuidString
        }
        if let location = node.location {
            body["location"] = location
        }
        if let nodeClass = node.node_class {
            body["node_class"] = nodeClass.id.uuidString
        }
        if let nodeSubtype = node.node_subtype {
            body["node_subtype"] = nodeSubtype.id.uuidString
        }
        if let com = node.com {
            body["com"] = com
        }
        if let qrCode = node.qr_code {
            body["qr_code"] = qrCode
        }
        if let serviceability = node.serviceability {
            body["serviceability"] = serviceability
        }
        if let serviceabilityNote = node.serviceability_note {
            body["serviceability_note"] = serviceabilityNote
        }
        if let room = node.room {
            body["room_id"] = room.id.uuidString
        }

        // Core attributes
        let coreAttrs = node.core_attributes.map { attr -> [String: Any] in
            return [
                "id": attr.id.uuidString,
                "name": attr.name,
                "value": attr.value,
                "node_class_property": attr.node_class_property?.id.uuidString ?? ""
            ]
        }
        body["core_attributes"] = coreAttrs

        return (method, endpoint, body)
    }

    private static func buildEdgeRequest(queueItem: SyncQueueItem, entity: EdgeV2?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let edge = entity else {
            return ("PUT", "\(baseURL)/edge/update/\(queueItem.edgeId?.uuidString ?? "MISSING")", ["error": "Edge not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/edge/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/edge/update/\(edge.id.uuidString)"
        case .delete:
            method = "DELETE"
            endpoint = "\(baseURL)/edge/delete/\(edge.id.uuidString)"
        default:
            return nil
        }

        var body: [String: Any] = [
            "id": edge.id.uuidString,
            "source": edge.source?.uuidString as Any,
            "target": edge.target?.uuidString as Any,
            "sld_id": edge.sld?.id.uuidString ?? "",
            "is_deleted": edge.is_deleted
        ]

        // Add edge class if available
        if let edgeClass = edge.edge_class {
            body["edge_class"] = edgeClass.id.uuidString
        }

        // Add core attributes
        if !edge.core_attributes.isEmpty {
            let coreAttrs = edge.core_attributes.map { attr -> [String: Any] in
                return [
                    "id": attr.id.uuidString,
                    "name": attr.name,
                    "value": attr.value
                ]
            }
            body["core_attributes"] = coreAttrs
        }

        return (method, endpoint, body)
    }

    private static func buildPhotoRequest(queueItem: SyncQueueItem, entity: Photo?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let photo = entity else {
            return ("POST", "\(baseURL)/photo/create", ["error": "Photo not found in local database"])
        }

        let method = "POST"
        let endpoint = "\(baseURL)/photo/create"

        var body: [String: Any] = [
            "id": photo.id.uuidString,
            "type": photo.type,
            "sld_id": photo.sld?.id.uuidString ?? "",
            "is_deleted": photo.is_deleted,
            "upload_needed": photo.upload_needed
        ]

        if let entityId = photo.node?.id ?? photo.userTask?.id ?? photo.issue?.id {
            body["entity_id"] = entityId.uuidString
        }
        if let url = photo.url {
            body["url"] = url
        }
        if let filename = photo.filename {
            body["filename"] = filename
        }
        if let localPath = photo.local_filepath {
            body["local_filepath"] = localPath
        }

        return (method, endpoint, body)
    }

    private static func buildTaskRequest(queueItem: SyncQueueItem, entity: UserTask?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let task = entity else {
            return ("PUT", "\(baseURL)/task/update/\(queueItem.userTaskId?.uuidString ?? "MISSING")", ["error": "Task not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/task/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/task/update/\(task.id.uuidString)"
        default:
            return nil
        }

        var body: [String: Any] = [
            "id": task.id.uuidString,
            "title": task.title,
            "task_description": task.task_description,
            "sld_id": task.sld?.id.uuidString ?? "",
            "is_deleted": task.is_deleted,
            "completed": task.completed
        ]

        if let dueDate = task.due_date {
            let formatter = ISO8601DateFormatter()
            body["due_date"] = formatter.string(from: dueDate)
        }

        return (method, endpoint, body)
    }

    private static func buildIRPhotoRequest(queueItem: SyncQueueItem, entity: IRPhoto?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let irPhoto = entity else {
            return ("POST", "\(baseURL)/ir-photo/create", ["error": "IR Photo not found in local database"])
        }

        let method = "POST"
        let endpoint = "\(baseURL)/ir-photo/create"

        let body: [String: Any] = [
            "id": irPhoto.id.uuidString,
            "visual_photo_key": irPhoto.visual_photo_key,
            "ir_photo_key": irPhoto.ir_photo_key,
            "ir_session_id": irPhoto.ir_session?.id.uuidString ?? "",
            "sld_id": irPhoto.sld.id.uuidString,
            "is_deleted": irPhoto.is_deleted,
            "node_id": irPhoto.node.id.uuidString
        ]

        return (method, endpoint, body)
    }

    private static func buildIRSessionRequest(queueItem: SyncQueueItem, entity: IRSession?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let session = entity else {
            return ("POST", "\(baseURL)/ir-session/create", ["error": "IR Session not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/ir-session/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/ir-session/update/\(session.id.uuidString)"
        default:
            return nil
        }

        var body: [String: Any] = [
            "id": session.id.uuidString,
            "name": session.name,
            "sld_id": session.sld.id.uuidString,
            "active": session.active,
            "photo_type": session.photo_type,
            "active_visual_prefix": session.active_visual_prefix,
            "active_ir_prefix": session.active_ir_prefix
        ]

        // Add date_created
        let dateFormatter = ISO8601DateFormatter()
        body["date_created"] = dateFormatter.string(from: session.date_created)

        // Add date_closed if available
        if let dateClosed = session.date_closed {
            body["date_closed"] = dateFormatter.string(from: dateClosed)
        }

        // Add equipment_ids
        body["equipment_ids"] = session.equipmentIds.map { $0.uuidString.lowercased() }

        return (method, endpoint, body)
    }

    private static func buildIssueRequest(queueItem: SyncQueueItem, entity: Issue?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let issue = entity else {
            return ("PUT", "\(baseURL)/issue/update/\(queueItem.issueId?.uuidString ?? "MISSING")", ["error": "Issue not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/issue/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/issue/update/\(issue.id.uuidString)"
        default:
            return nil
        }

        var body: [String: Any] = [
            "id": issue.id.uuidString,
            "title": issue.title ?? "",
            "issue_description": issue.issueDescription ?? "",
            "sld_id": issue.sld?.id.uuidString ?? "",
            "is_deleted": issue.is_deleted
        ]

        // Add optional fields
        if let priority = issue.priority {
            body["priority"] = priority
        }
        if let status = issue.status {
            body["status"] = status
        }
        if let issueType = issue.issue_type {
            body["issue_type"] = issueType
        }
        if let issueSubtype = issue.issue_subtype {
            body["issue_subtype"] = issueSubtype
        }
        if let proposedResolution = issue.proposed_resolution {
            body["proposed_resolution"] = proposedResolution
        }

        if let nodeId = issue.node?.id {
            body["node_id"] = nodeId.uuidString
        }
        if let issueClassId = issue.issue_class?.id {
            body["issue_class"] = issueClassId.uuidString
        }

        // Safety & notification fields
        body["immediate_hazard"] = issue.immediateHazard
        body["customer_notified"] = issue.customerNotified

        // Issue details (core attributes)
        if !issue.details.isEmpty {
            let details = issue.details.map { attr -> [String: Any] in
                var detail: [String: Any] = [
                    "id": attr.id.uuidString,
                    "issue_class_property": attr.issue_class_property?.id.uuidString ?? "",
                    "name": attr.name,
                    "value": attr.value
                ]
                if let unit = attr.unit {
                    detail["unit"] = unit
                }
                if let notes = attr.attributeNotes {
                    detail["description"] = notes
                }
                return detail
            }
            body["details"] = details
        }

        return (method, endpoint, body)
    }

    private static func buildQuoteRequest(queueItem: SyncQueueItem, entity: Quote?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let quote = entity else {
            return ("PUT", "\(baseURL)/quote/update/\(queueItem.quoteId?.uuidString ?? "MISSING")", ["error": "Quote not found in local database"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/quote/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/quote/update/\(quote.id.uuidString)"
        default:
            return nil
        }

        let body: [String: Any] = [
            "id": quote.id.uuidString,
            "sld_id": quote.sld?.id.uuidString ?? "",
            "is_deleted": quote.is_deleted
        ]

        return (method, endpoint, body)
    }

    private static func buildMappingRequest(queueItem: SyncQueueItem, baseURL: String) -> (String, String, [String: Any]?)? {
        let method: String
        let endpoint: String
        var body: [String: Any] = [:]

        // Get mapping type string for endpoint (e.g., "attachmentnode" -> "attachment-node")
        let mappingTypeStr: String
        switch queueItem.target {
        case .mappingAttachmentNode:
            mappingTypeStr = "attachment-node"
        case .mappingIssueTask:
            mappingTypeStr = "issue-task"
        case .mappingTaskSession:
            mappingTypeStr = "task-session"
        case .mappingQuoteTask:
            mappingTypeStr = "quote-task"
        case .mappingUserTask:
            mappingTypeStr = "user-task"
        case .mappingTaskNode:
            mappingTypeStr = "task-node"
        case .mappingTaskForm:
            mappingTypeStr = "task-form"
        case .mappingTaskFormInstance:
            mappingTypeStr = "task-forminstance"
        case .mappingFormInstanceNode:
            mappingTypeStr = "forminstance-node"
        case .mappingNodeSession:
            mappingTypeStr = "node-session"
        default:
            mappingTypeStr = targetToString(queueItem.target).replacingOccurrences(of: "mapping", with: "").lowercased()
        }

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/mapping/\(mappingTypeStr)/"
            body["is_deleted"] = false
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/mapping/\(mappingTypeStr)/"
            body["is_deleted"] = queueItem.mappingData?.isDeleted ?? false
        default:
            return nil
        }

        // Add mapping-specific IDs from mappingData (using Android format: b=nodeId, d=attachmentId, e=isDeleted)
        if let mappingData = queueItem.mappingData {
            body["is_deleted"] = mappingData.isDeleted
            body["e"] = mappingData.isDeleted

            // Add IDs based on mapping type
            if let nodeId = mappingData.nodeId {
                body["b"] = nodeId.uuidString.lowercased()
                body["node_id"] = nodeId.uuidString.lowercased()
            }
            if let attachmentId = mappingData.attachmentId {
                body["d"] = attachmentId.uuidString.lowercased()
                body["attachment_id"] = attachmentId.uuidString.lowercased()
            }
            if let taskId = mappingData.taskId {
                body["task_id"] = taskId.uuidString.lowercased()
            }
            if let sessionId = mappingData.sessionId {
                body["session_id"] = sessionId.uuidString.lowercased()
            }
            if let issueId = mappingData.issueId {
                body["issue_id"] = issueId.uuidString.lowercased()
            }
            if let quoteId = mappingData.quoteId {
                body["quote_id"] = quoteId.uuidString.lowercased()
            }
            if let userId = mappingData.userId {
                body["user_id"] = userId.uuidString.lowercased()
            }
            if let formId = mappingData.formId {
                body["form_id"] = formId.uuidString.lowercased()
            }
            if let formInstanceId = mappingData.formInstanceId {
                body["form_instance_id"] = formInstanceId.uuidString.lowercased()
            }
            if let mappingType = mappingData.mappingType {
                body["mapping_type"] = mappingType
            }
        }

        return (method, endpoint, body)
    }

    private static func buildAttachmentRequest(queueItem: SyncQueueItem, entity: Attachment?, baseURL: String) -> (String, String, [String: Any]?)? {
        guard let attachment = entity else {
            return ("POST", "\(baseURL)/attachment/create", ["error": "Attachment not found in local database", "attachmentId": queueItem.attachmentId?.uuidString ?? "MISSING"])
        }

        let method: String
        let endpoint: String

        switch queueItem.operation {
        case .create:
            method = "POST"
            endpoint = "\(baseURL)/attachment/create"
        case .update:
            method = "PUT"
            endpoint = "\(baseURL)/attachment/update/\(attachment.id.uuidString)"
        case .delete:
            method = "DELETE"
            endpoint = "\(baseURL)/attachment/delete/\(attachment.id.uuidString)"
        default:
            return nil
        }

        var body: [String: Any] = [
            "id": attachment.id.uuidString,
            "company_id": attachment.companyId.uuidString,
            "sld_id": attachment.sldId.uuidString,
            "type": attachment.type,
            "filename": attachment.filename,
            "is_deleted": attachment.isDeleted,
            "upload_needed": attachment.uploadNeeded,
            "visibility": attachment.visibility
        ]

        if let sessionId = attachment.sessionId {
            body["session_id"] = sessionId.uuidString
        }
        if let taskId = attachment.taskId {
            body["task_id"] = taskId.uuidString
        }
        body["file_size"] = attachment.fileSize
        body["key"] = attachment.key
        if let localFilePath = attachment.localFilePath {
            body["local_file_path"] = localFilePath
        }

        return (method, endpoint, body)
    }

    private static func getHeaders() -> [String: String] {
        var headers = [
            "Content-Type": "application/json",
            "Accept": "application/json"
        ]

        // Add company name if available
        if let companyName = CompanyConfigService.shared.getCurrentCompanyName() {
            headers["X-Company-Name"] = companyName
        }

        // Note: Authorization token would be added at runtime
        headers["Authorization"] = "Bearer <ACCESS_TOKEN>"

        return headers
    }

    private static func fetchEntity(for queueItem: SyncQueueItem, context: ModelContext) -> Any? {
        do {
            switch queueItem.target {
            case .node:
                if let nodeId = queueItem.nodeId {
                    let descriptor = FetchDescriptor<NodeV2>(
                        predicate: #Predicate<NodeV2> { n in n.id == nodeId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .edge:
                if let edgeId = queueItem.edgeId {
                    let descriptor = FetchDescriptor<EdgeV2>(
                        predicate: #Predicate<EdgeV2> { e in e.id == edgeId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .photo:
                if let photoId = queueItem.photoId {
                    let descriptor = FetchDescriptor<Photo>(
                        predicate: #Predicate<Photo> { p in p.id == photoId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .userTask:
                if let taskId = queueItem.userTaskId {
                    let descriptor = FetchDescriptor<UserTask>(
                        predicate: #Predicate<UserTask> { t in t.id == taskId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .irPhoto:
                if let irPhotoId = queueItem.irPhotoId {
                    let descriptor = FetchDescriptor<IRPhoto>(
                        predicate: #Predicate<IRPhoto> { ip in ip.id == irPhotoId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .irSession:
                if let sessionId = queueItem.irSessionId {
                    let descriptor = FetchDescriptor<IRSession>(
                        predicate: #Predicate<IRSession> { s in s.id == sessionId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .issue:
                if let issueId = queueItem.issueId {
                    let descriptor = FetchDescriptor<Issue>(
                        predicate: #Predicate<Issue> { i in i.id == issueId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .quote:
                if let quoteId = queueItem.quoteId {
                    let descriptor = FetchDescriptor<Quote>(
                        predicate: #Predicate<Quote> { q in q.id == quoteId }
                    )
                    return try context.fetch(descriptor).first
                }

            case .attachment:
                if let attachmentId = queueItem.attachmentId {
                    let descriptor = FetchDescriptor<Attachment>(
                        predicate: #Predicate<Attachment> { a in a.id == attachmentId }
                    )
                    return try context.fetch(descriptor).first
                }

            default:
                // Mappings don't have entities to fetch
                return nil
            }
        } catch {
            AppLogger.log(.error, "Error fetching entity: \(error)", category: .sync)
        }

        return nil
    }
}
