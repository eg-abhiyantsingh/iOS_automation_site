//
//  SLDMappingsDTO.swift
//  SwiftDataTutorial
//
//  Mapping DTOs for relationship management
//
import Foundation

// MARK: - Individual Mapping DTOs

struct MappingIssueTaskDTO: Codable {
    var issue_id: String
    var task_id: String
    var is_deleted: Bool
}

struct MappingTaskSessionDTO: Codable {
    var task_id: String
    var session_id: String
    var is_deleted: Bool
}

struct MappingQuoteTaskDTO: Codable {
    var quote_id: String
    var task_id: String
    var is_deleted: Bool
}

struct MappingUserTaskDTO: Codable {
    var task_id: String
    var user_id: String
    var mapping_type: String?
    var is_deleted: Bool
}

struct MappingTaskNodeDTO: Codable {
    var id: String
    var task_id: String
    var node_id: String
    var is_deleted: Bool
    var is_completed: Bool?
}

struct MappingNodeSessionDTO: Codable {
    var id: String
    var node_id: String
    var session_id: String
    var is_deleted: Bool
}

struct MappingUserSessionDTO: Codable {
    var id: String
    var user_id: String
    var session_id: String
    var mapping_type: String
    var is_deleted: Bool
}

struct MappingTaskFormDTO: Codable {
    var id: String
    var task_id: String?
    var form_id: String?
    var is_deleted: Bool
}

struct MappingTaskFormInstanceDTO: Codable {
    var id: String
    var task_id: String
    var form_instance_id: String
    var is_deleted: Bool?
}

struct MappingFormInstanceNodeDTO: Codable {
    var id: String
    var form_instance_id: String
    var node_id: String
    var is_deleted: Bool?
}

// EG Forms — new form system (ZP-1723). Parallel to legacy mappings above.

struct MappingTaskEGFormInstanceDTO: Codable {
    var id: String
    var task_id: String
    var eg_form_instance_id: String
    var is_deleted: Bool?
}

struct MappingEGFormInstanceNodeDTO: Codable {
    var id: String
    var eg_form_instance_id: String
    var node_id: String
    var is_deleted: Bool?
}

struct MappingNodeSLDViewDTO: Codable {
    var id: String
    var node_id: String
    var sld_view_id: String
    var x: Double?
    var y: Double?
    var width: Double?
    var height: Double?
    var is_collapsed: Bool?
    var is_deleted: Bool
    var created_at: String?
    var modified_at: String?
}

struct MappingEdgeSLDViewDTO: Codable {
    var id: String
    var edge_id: String
    var sld_view_id: String
    var points: [EdgePoint]?
    var algorithm: String?
    var is_deleted: Bool
}

// MARK: - Combined Mappings DTO

struct SLDMappingsDTO: Codable {
    var issue_task: [MappingIssueTaskDTO]?  // Made optional for resilient decoding
    var task_session: [MappingTaskSessionDTO]?  // Made optional for resilient decoding
    var quote_task: [MappingQuoteTaskDTO]?  // Made optional for resilient decoding
    var user_task: [MappingUserTaskDTO]?  // Made optional for resilient decoding
    var task_node: [MappingTaskNodeDTO]?  // Optional for backward compatibility
    var task_form: [MappingTaskFormDTO]?  // Optional for backward compatibility
    var task_form_instance: [MappingTaskFormInstanceDTO]?  // Legacy form instance mappings
    var form_instance_node: [MappingFormInstanceNodeDTO]?  // Legacy form instance to node mappings
    var task_eg_form_instance: [MappingTaskEGFormInstanceDTO]?  // EG form instance task mappings (ZP-1723)
    var eg_form_instance_node: [MappingEGFormInstanceNodeDTO]?  // EG form instance node mappings (ZP-1723)
    var node_session: [MappingNodeSessionDTO]?  // New node-session mappings
    var user_session: [MappingUserSessionDTO]?  // User-session assignment mappings
    var attachment_node: [MappingAttachmentNodeDTO]?  // Attachment to node mappings
    var node_sld_view: [MappingNodeSLDViewDTO]?  // Node position mappings per view
    var edge_sld_view: [MappingEdgeSLDViewDTO]?  // Edge routing mappings per view

    // Computed properties to provide default empty arrays
    var issueTaskMappings: [MappingIssueTaskDTO] {
        issue_task ?? []
    }

    var taskSessionMappings: [MappingTaskSessionDTO] {
        task_session ?? []
    }

    var quoteTaskMappings: [MappingQuoteTaskDTO] {
        quote_task ?? []
    }

    var userTaskMappings: [MappingUserTaskDTO] {
        user_task ?? []
    }

    var taskNodeMappings: [MappingTaskNodeDTO] {
        task_node ?? []
    }

    var taskFormMappings: [MappingTaskFormDTO] {
        task_form ?? []
    }

    var taskFormInstanceMappings: [MappingTaskFormInstanceDTO] {
        task_form_instance ?? []
    }

    var formInstanceNodeMappings: [MappingFormInstanceNodeDTO] {
        form_instance_node ?? []
    }

    var taskEGFormInstanceMappings: [MappingTaskEGFormInstanceDTO] {
        task_eg_form_instance ?? []
    }

    var egFormInstanceNodeMappings: [MappingEGFormInstanceNodeDTO] {
        eg_form_instance_node ?? []
    }

    var nodeSessionMappings: [MappingNodeSessionDTO] {
        node_session ?? []
    }

    var userSessionMappings: [MappingUserSessionDTO] {
        user_session ?? []
    }

    var attachmentNodeMappings: [MappingAttachmentNodeDTO] {
        attachment_node ?? []
    }

    var nodeSLDViewMappings: [MappingNodeSLDViewDTO] {
        node_sld_view ?? []
    }

    var edgeSLDViewMappings: [MappingEdgeSLDViewDTO] {
        edge_sld_view ?? []
    }
}