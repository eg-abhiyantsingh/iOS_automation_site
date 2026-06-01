import Foundation

// MARK: - Mapping Data Structures
struct MappingData: Codable {
    let id: UUID?  // Mapping ID itself (for node-view and edge-view mappings)
    let issueId: UUID?
    let taskId: UUID?
    let sessionId: UUID?
    let quoteId: UUID?
    let userId: UUID?
    let nodeId: UUID?
    let formId: UUID?
    let formInstanceId: UUID?
    let attachmentId: UUID?
    let mappingType: String?
    let isDeleted: Bool

    // Node-SLD View mapping fields
    let sldViewId: UUID?
    let x: Double?
    let y: Double?
    let width: Double?
    let height: Double?
    let isCollapsed: Bool?

    // Task-Node completion field
    let isCompleted: Bool?

    // Bulk Task-Node completion field (JSON-encoded array of completions)
    let bulkCompletions: String?

    // Edge-SLD View mapping fields
    let edgeId: UUID?
    let points: [[String: Double]]?
    let algorithm: String?

    // Custom initializer with default values
    init(
        id: UUID? = nil,
        issueId: UUID? = nil,
        taskId: UUID? = nil,
        sessionId: UUID? = nil,
        quoteId: UUID? = nil,
        userId: UUID? = nil,
        nodeId: UUID? = nil,
        formId: UUID? = nil,
        formInstanceId: UUID? = nil,
        attachmentId: UUID? = nil,
        mappingType: String? = nil,
        isDeleted: Bool,
        isCompleted: Bool? = nil,
        bulkCompletions: String? = nil,
        sldViewId: UUID? = nil,
        x: Double? = nil,
        y: Double? = nil,
        width: Double? = nil,
        height: Double? = nil,
        isCollapsed: Bool? = nil,
        edgeId: UUID? = nil,
        points: [[String: Double]]? = nil,
        algorithm: String? = nil
    ) {
        self.id = id
        self.issueId = issueId
        self.taskId = taskId
        self.sessionId = sessionId
        self.quoteId = quoteId
        self.userId = userId
        self.nodeId = nodeId
        self.formId = formId
        self.formInstanceId = formInstanceId
        self.attachmentId = attachmentId
        self.mappingType = mappingType
        self.isDeleted = isDeleted
        self.isCompleted = isCompleted
        self.bulkCompletions = bulkCompletions
        self.sldViewId = sldViewId
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.isCollapsed = isCollapsed
        self.edgeId = edgeId
        self.points = points
        self.algorithm = algorithm
    }

    // For Issue-Task mapping
    static func issueTask(issueId: UUID, taskId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: issueId, taskId: taskId, sessionId: nil, quoteId: nil, userId: nil, nodeId: nil, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For Task-Session mapping
    static func taskSession(taskId: UUID, sessionId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: sessionId, quoteId: nil, userId: nil, nodeId: nil, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For Quote-Task mapping
    static func quoteTask(quoteId: UUID, taskId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: nil, quoteId: quoteId, userId: nil, nodeId: nil, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For User-Task mapping
    static func userTask(userId: UUID, taskId: UUID, mappingType: String, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: nil, quoteId: nil, userId: userId, nodeId: nil, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: mappingType, isDeleted: isDeleted)
    }

    // For Task-Node mapping
    static func taskNode(taskId: UUID, nodeId: UUID, isDeleted: Bool, isCompleted: Bool? = nil) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: nil, quoteId: nil, userId: nil, nodeId: nodeId, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted, isCompleted: isCompleted)
    }

    // For Bulk Task-Node completion mapping
    static func taskNodeBulkCompletion(taskId: UUID, completions: [(nodeId: UUID, isCompleted: Bool)]) -> MappingData {
        // Encode completions as JSON string
        let completionsArray = completions.map { ["node_id": $0.nodeId.uuidString, "is_completed": $0.isCompleted] as [String: Any] }
        let jsonData = try? JSONSerialization.data(withJSONObject: completionsArray)
        let jsonString = jsonData.flatMap { String(data: $0, encoding: .utf8) }

        return MappingData(taskId: taskId, isDeleted: false, bulkCompletions: jsonString)
    }

    // For Task-Form mapping
    static func taskForm(taskId: UUID, formId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: nil, quoteId: nil, userId: nil, nodeId: nil, formId: formId, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For Task-FormInstance mapping
    static func taskFormInstance(taskId: UUID, formInstanceId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: taskId, sessionId: nil, quoteId: nil, userId: nil, nodeId: nil, formId: nil, formInstanceId: formInstanceId, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For FormInstance-Node mapping
    static func formInstanceNode(formInstanceId: UUID, nodeId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: nil, sessionId: nil, quoteId: nil, userId: nil, nodeId: nodeId, formId: nil, formInstanceId: formInstanceId, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For Node-Session mapping
    static func nodeSession(nodeId: UUID, sessionId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: nil, sessionId: sessionId, quoteId: nil, userId: nil, nodeId: nodeId, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: nil, isDeleted: isDeleted)
    }

    // For User-Session mapping
    static func userSession(userId: UUID, sessionId: UUID, mappingType: String, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: nil, sessionId: sessionId, quoteId: nil, userId: userId, nodeId: nil, formId: nil, formInstanceId: nil, attachmentId: nil, mappingType: mappingType, isDeleted: isDeleted)
    }

    // For Attachment-Node mapping
    static func attachmentNode(attachmentId: UUID, nodeId: UUID, isDeleted: Bool) -> MappingData {
        MappingData(issueId: nil, taskId: nil, sessionId: nil, quoteId: nil, userId: nil, nodeId: nodeId, formId: nil, formInstanceId: nil, attachmentId: attachmentId, mappingType: nil, isDeleted: isDeleted)
    }

    // For Node-SLD View mapping
    static func nodeSLDView(
        mappingId: UUID,
        nodeId: UUID,
        viewId: UUID,
        x: Double,
        y: Double,
        width: Double?,
        height: Double?,
        isCollapsed: Bool,
        isDeleted: Bool
    ) -> MappingData {
        MappingData(
            id: mappingId,
            nodeId: nodeId,
            isDeleted: isDeleted,
            sldViewId: viewId,
            x: x,
            y: y,
            width: width,
            height: height,
            isCollapsed: isCollapsed
        )
    }

    // For Edge-SLD View mapping
    static func edgeSLDView(
        mappingId: UUID,
        edgeId: UUID,
        viewId: UUID,
        points: [[String: Double]]?,
        algorithm: String?,
        isDeleted: Bool
    ) -> MappingData {
        MappingData(
            id: mappingId,
            isDeleted: isDeleted,
            sldViewId: viewId,
            edgeId: edgeId,
            points: points,
            algorithm: algorithm
        )
    }

    /// Stable identity key for a given mapping target. Used on SyncLog.mappingIds so
    /// that a later success for the same mapping can mark earlier failure logs as
    /// resolved (mirrors entityId-based matching used for node/photo/etc.).
    /// Returns nil if the mapping data lacks the fields needed to identify uniquely.
    func identityKey(for target: SyncTarget) -> String? {
        switch target {
        case .mappingIssueTask:
            guard let issueId = issueId, let taskId = taskId else { return nil }
            return "issueTask-\(issueId.uuidString)-\(taskId.uuidString)"
        case .mappingTaskSession:
            guard let taskId = taskId, let sessionId = sessionId else { return nil }
            return "taskSession-\(taskId.uuidString)-\(sessionId.uuidString)"
        case .mappingQuoteTask:
            guard let quoteId = quoteId, let taskId = taskId else { return nil }
            return "quoteTask-\(quoteId.uuidString)-\(taskId.uuidString)"
        case .mappingUserTask:
            guard let userId = userId, let taskId = taskId else { return nil }
            return "userTask-\(userId.uuidString)-\(taskId.uuidString)-\(mappingType ?? "")"
        case .mappingTaskNode:
            guard let taskId = taskId, let nodeId = nodeId else { return nil }
            return "taskNode-\(taskId.uuidString)-\(nodeId.uuidString)"
        case .mappingTaskForm:
            guard let taskId = taskId, let formId = formId else { return nil }
            return "taskForm-\(taskId.uuidString)-\(formId.uuidString)"
        case .mappingTaskFormInstance:
            guard let taskId = taskId, let formInstanceId = formInstanceId else { return nil }
            return "taskFormInstance-\(taskId.uuidString)-\(formInstanceId.uuidString)"
        case .mappingFormInstanceNode:
            guard let formInstanceId = formInstanceId, let nodeId = nodeId else { return nil }
            return "formInstanceNode-\(formInstanceId.uuidString)-\(nodeId.uuidString)"
        case .mappingNodeSession:
            guard let nodeId = nodeId, let sessionId = sessionId else { return nil }
            return "nodeSession-\(nodeId.uuidString)-\(sessionId.uuidString)"
        case .mappingUserSession:
            guard let userId = userId, let sessionId = sessionId else { return nil }
            return "userSession-\(userId.uuidString)-\(sessionId.uuidString)-\(mappingType ?? "")"
        case .mappingAttachmentNode:
            guard let attachmentId = attachmentId, let nodeId = nodeId else { return nil }
            return "attachmentNode-\(attachmentId.uuidString)-\(nodeId.uuidString)"
        case .mappingNodeSLDView:
            if let id = id { return "nodeSLDView-\(id.uuidString)" }
            guard let nodeId = nodeId, let sldViewId = sldViewId else { return nil }
            return "nodeSLDView-\(nodeId.uuidString)-\(sldViewId.uuidString)"
        case .mappingEdgeSLDView:
            if let id = id { return "edgeSLDView-\(id.uuidString)" }
            guard let edgeId = edgeId, let sldViewId = sldViewId else { return nil }
            return "edgeSLDView-\(edgeId.uuidString)-\(sldViewId.uuidString)"
        case .mappingTaskNodeBulkCompletion:
            guard let taskId = taskId else { return nil }
            return "taskNodeBulkCompletion-\(taskId.uuidString)"
        case .mappingTaskEGFormInstance:
            // EG form mappings reuse formInstanceId for eg_form_instance_id
            // to avoid widening MappingData. The "egForm" prefix in the key
            // keeps them distinct from legacy task↔formInstance mappings.
            guard let taskId = taskId, let formInstanceId = formInstanceId else { return nil }
            return "taskEgFormInstance-\(taskId.uuidString)-\(formInstanceId.uuidString)"
        case .mappingEGFormInstanceNode:
            guard let formInstanceId = formInstanceId, let nodeId = nodeId else { return nil }
            return "egFormInstanceNode-\(formInstanceId.uuidString)-\(nodeId.uuidString)"
        case .node, .edge, .photo, .userTask, .irPhoto, .irSession, .issue, .quote,
             .formInstance, .egFormInstance, .building, .floor, .room, .attachment, .sldView:
            return nil
        }
    }
}
