//
//  APIEndpoints.swift
//  Egalvanic PZ
//
//  Created by Kush Jadia on 13/11/25.
//

import Foundation

/// Centralized repository for all API endpoints used in the application.
/// Using enum to prevent instantiation and provide a clean namespace for endpoint constants.
enum APIEndpoints {

    // MARK: - Authentication Endpoints

    enum Auth {
        static let login = "/auth/v2/login"
        static let logout = "/auth/v2/logout"
        static let refresh = "/auth/v2/refresh"
        static let me = "/auth/v2/me"
        // Challenge / forgot-password / reset-password flows remain on V1 —
        // the backend has not shipped V2 equivalents for these. Mirrors Android (ZP-2086).
        static let respondToChallenge = "/auth/respond-to-challenge"
        static let forgotPassword = "/auth/forgot-password"
        static let resetPassword = "/auth/reset-password"
    }

    // MARK: - Company Configuration

    enum Company {
        static let config = "/company/alliance-config"

        /// Get company config by code (e.g., "/company/config/acme")
        static func config(for companyCode: String) -> String {
            return "\(config)/\(companyCode.lowercased())"
        }
    }

    // MARK: - Account Endpoints

    enum Account {
        /// Get accounts for a company (e.g., "/account/by-company/company-123")
        static func byCompany(companyId: String) -> String {
            return "/account/by-company/\(companyId)"
        }
    }

    // MARK: - Labor Union Endpoints

    enum LaborUnion {
        static let list = "/labor-unions"
    }

    // MARK: - Office Endpoints

    enum Office {
        static let list = "/offices"
    }

    // MARK: - User Endpoints

    enum User {
        /// Get all users for a company (e.g., "/users/company/company-123")
        static func companyUsers(companyId: String) -> String {
            return "/users/company/\(companyId)"
        }
    }

    // MARK: - SLD (Single Line Diagram) Endpoints

    enum SLD {
        static let base = "/sld"
        static let create = "/sld/"

        /// Get user's SLDs (e.g., "/users/123/slds")
        static func userSLDs(userId: String) -> String {
            return "/users/\(userId)/slds"
        }

        /// Get SLD details v3 (e.g., "/sld/v3/sld-123")
        static func details(sldId: String) -> String {
            return "/sld/v3/\(sldId)"
        }

        /// Update SLD (e.g., "/sld/update/sld-123")
        static func update(sldId: String) -> String {
            return "/sld/update/\(sldId)"
        }
    }

    // MARK: - Node Endpoints

    enum Node {
        static let create = "/node/create"

        /// Update node (e.g., "/node/update/node-123")
        static func update(nodeId: String) -> String {
            return "/node/update/\(nodeId)"
        }
    }

    // MARK: - Edge Endpoints

    enum Edge {
        static let create = "/edge/create"

        /// Update edge (e.g., "/edge/update/edge-123")
        static func update(edgeId: String) -> String {
            return "/edge/update/\(edgeId)"
        }
    }

    // MARK: - Photo Endpoints

    enum Photo {
        static let create = "/photo/create"

        /// Update photo (e.g., "/photo/update/photo-123")
        static func update(photoId: String) -> String {
            return "/photo/update/\(photoId)"
        }
    }

    // MARK: - Task Endpoints

    enum Task {
        static let create = "/task/create"

        /// Update task (e.g., "/task/update/task-123")
        static func update(taskId: String) -> String {
            return "/task/update/\(taskId)"
        }
    }

    // MARK: - Form Endpoints

    enum Form {
        static let list = "/forms"
    }

    // MARK: - Lookup Endpoints

    enum Lookup {
        /// Flat list of procedures. The iOS client caches all of them
        /// during refresh and filters locally by node_class_id so the
        /// picker keeps working offline.
        static let procedures = "/lookup/procedures"
    }

    // MARK: - Form Instance Endpoints

    enum FormInstance {
        static let create = "/form-instance/"

        /// Get form instance (e.g., "/form-instance/instance-123")
        static func get(instanceId: String) -> String {
            return "/form-instance/\(instanceId)"
        }

        /// Update form instance (e.g., "/form-instance/instance-123")
        static func update(instanceId: String) -> String {
            return "/form-instance/\(instanceId)"
        }
    }

    // MARK: - EG Form Instance Endpoints (ZP-1723)

    enum EGFormInstance {
        static let create = "/eg-form-instance/"

        static func get(instanceId: String) -> String {
            return "/eg-form-instance/\(instanceId)"
        }

        static func update(instanceId: String) -> String {
            return "/eg-form-instance/\(instanceId)"
        }
    }

    // MARK: - IR Session Endpoints

    enum IRSession {
        static let create = "/ir_session/create"

        /// Update IR session (e.g., "/ir_session/update/session-123")
        static func update(sessionId: String) -> String {
            return "/ir_session/update/\(sessionId)"
        }

        /// Get full session details including photos (e.g., "/ir_session/session-123/full")
        static func full(sessionId: String) -> String {
            return "/ir_session/\(sessionId)/full"
        }
    }

    // MARK: - IR Photo Endpoints

    enum IRPhoto {
        static let create = "/ir_photo/create"

        /// Update IR photo (e.g., "/ir_photo/update/photo-123")
        static func update(photoId: String) -> String {
            return "/ir_photo/update/\(photoId)"
        }

        /// Batch-extract embedded visual images from uploaded FLIR-IND radiometric JPEGs.
        static let extractVisualBatch = "/ir_photo/extract_visual/batch"
    }

    // MARK: - Issue Endpoints

    enum Issue {
        static let create = "/issue/create"

        /// Update issue (e.g., "/issue/update/issue-123")
        static func update(issueId: String) -> String {
            return "/issue/update/\(issueId)"
        }

        /// Get status history for an issue (e.g., "/issue/issue-123/status-history")
        static func statusHistory(issueId: String) -> String {
            return "/issue/\(issueId)/status-history"
        }
    }

    // MARK: - Quote Endpoints

    enum Quote {
        static let create = "/quote/create"

        /// Update quote (e.g., "/quote/update/quote-123")
        static func update(quoteId: String) -> String {
            return "/quote/update/\(quoteId)"
        }
    }

    // MARK: - Class Definition Endpoints

    enum Classes {
        // Edge Classes
        static let edgeClasses = "/edge_classes"

        /// Get edge classes for user (e.g., "/edge_classes/user/user-123")
        static func edgeClasses(userId: String) -> String {
            return "/edge_classes/user/\(userId)"
        }

        // Issue Classes
        static let issueClasses = "/issue_classes"

        /// Get issue classes for user (e.g., "/issue_classes/user/user-123")
        static func issueClasses(userId: String) -> String {
            return "/issue_classes/user/\(userId)"
        }

        // Node Classes
        static let nodeClasses = "/node_classes"

        /// Get node classes for user (e.g., "/node_classes/user/user-123")
        static func nodeClasses(userId: String) -> String {
            return "/node_classes/user/\(userId)"
        }
    }

    // MARK: - Shortcut Endpoints

    enum Shortcuts {
        /// Get shortcuts for user (e.g., "/shortcuts/user/user-123")
        static func userShortcuts(userId: String) -> String {
            return "/shortcuts/user/\(userId)"
        }
    }

    // MARK: - Mapping Endpoints

    enum Mapping {
        // Issue-Task Mappings
        static let issueTaskCreate = "/mapping/issue-task/create"

        /// Update issue-task mapping
        static func issueTaskUpdate(issueId: String, taskId: String) -> String {
            return "/mapping/issue-task/update/\(issueId)/\(taskId)"
        }

        // Task-Session Mappings
        static let taskSessionCreate = "/mapping/task-session/create"

        /// Update task-session mapping
        static func taskSessionUpdate(taskId: String, sessionId: String) -> String {
            return "/mapping/task-session/update/\(taskId)/\(sessionId)"
        }

        // Quote-Task Mappings
        static let quoteTaskCreate = "/mapping/quote-task/create"

        /// Update quote-task mapping
        static func quoteTaskUpdate(quoteId: String, taskId: String) -> String {
            return "/mapping/quote-task/update/\(quoteId)/\(taskId)"
        }

        // User-Task Mappings
        static let userTaskCreate = "/mapping/user-task/create"

        /// Update user-task mapping
        static func userTaskUpdate(userId: String, taskId: String) -> String {
            return "/mapping/user-task/update/\(userId)/\(taskId)"
        }

        // Task-Node Mappings
        static let taskNodeCreate = "/mapping/task-node/create"
        static let taskNodeBatch = "/mapping/task-node/batch"
        static let taskNodeCompletion = "/mapping/task-node/completion"

        /// Update task-node mapping
        static func taskNodeUpdate(taskId: String, nodeId: String) -> String {
            return "/mapping/task-node/update/\(taskId)/\(nodeId)"
        }

        /// Get task's node mappings
        static func taskNodes(taskId: String) -> String {
            return "/mapping/task/\(taskId)/nodes"
        }

        // Node-Session Mappings
        static let nodeSessionCreate = "/mapping/node-session/create"

        /// Update node-session mapping
        static func nodeSessionUpdate(nodeId: String, sessionId: String) -> String {
            return "/mapping/node-session/update/\(nodeId)/\(sessionId)"
        }

        // User-Session Mappings
        static let userSessionCreate = "/mapping/user-session/create"

        /// Update user-session mapping by mapping ID
        static func userSessionUpdate(mappingId: String) -> String {
            return "/mapping/user-session/update/\(mappingId)"
        }

        // Task-Form Mappings
        static let taskFormCreate = "/mapping/task-form/create"
        static let taskFormBatch = "/mapping/task-form/batch"

        /// Update task-form mapping
        static func taskFormUpdate(taskId: String, formId: String) -> String {
            return "/mapping/task-form/update/\(taskId)/\(formId)"
        }

        /// Get task's form mappings
        static func taskForms(taskId: String) -> String {
            return "/mapping/task/\(taskId)/forms"
        }

        // Form Instance Mappings
        static let formInstanceNodeCreate = "/mapping/form-instance-node/"

        /// Update form instance-node mapping
        static func formInstanceNodeUpdate(formInstanceId: String, nodeId: String) -> String {
            return "/mapping/form-instance-node/\(formInstanceId)/\(nodeId)"
        }

        /// Get form instance's node mappings
        static func formInstanceNodes(formInstanceId: String) -> String {
            return "/mapping/form-instance-node/\(formInstanceId)"
        }

        // Task-Form Instance Mappings
        static let taskFormInstanceCreate = "/mapping/task-form-instance/"
        static let taskFormInstanceBatch = "/mapping/task-form-instance/batch"
        static let taskFormInstanceCreateAndLink = "/mapping/task-form-instance/create-and-link"

        /// Update task-form instance mapping
        static func taskFormInstanceUpdate(taskId: String, formInstanceId: String) -> String {
            return "/mapping/task-form-instance/\(taskId)/\(formInstanceId)"
        }

        /// Get task's form instance mappings
        static func taskFormInstances(taskId: String) -> String {
            return "/mapping/task-form-instance/\(taskId)"
        }
    }

    // MARK: - Location Hierarchy Endpoints

    enum Location {
        // Building Operations
        static let buildingCreate = "/location/building/"

        /// Update building
        static func buildingUpdate(buildingId: String) -> String {
            return "/location/building/\(buildingId)"
        }

        // Floor Operations
        static let floorCreate = "/location/floor/"

        /// Update floor
        static func floorUpdate(floorId: String) -> String {
            return "/location/floor/\(floorId)"
        }

        // Room Operations
        static let roomCreate = "/location/room/"

        /// Update room
        static func roomUpdate(roomId: String) -> String {
            return "/location/room/\(roomId)"
        }

        // Location Queries
        /// Get all locations for an SLD
        static func sldLocations(sldId: String) -> String {
            return "/location/sld/\(sldId)"
        }
    }

    // MARK: - Reporting Endpoints

    enum Reporting {
        static let generate = "/reporting/generate"
    }

    // MARK: - Device Management

    enum Device {
        static let register = "/device/register"
    }

    // MARK: - DevRev Endpoints

    enum DevRev {
        static let sessionToken = "/devrev/session-token"
    }

    // MARK: - Legal Endpoints

    enum Legal {
        static let checkAcceptance = "/legal/acceptance/check"
        static let accept = "/legal/acceptance/accept"
    }

    // MARK: - Version Check Endpoints

    enum Version {
        static let check = "/version-check"
    }

    // MARK: - S3 Presigned URL Endpoints

    enum S3 {
        /// Single-item presigned URL (for asset photos, attachments, etc.)
        static let url = "/s3/url"

        /// Batch presigned URLs with prefix matching (for IR photos)
        static let urlsBatch = "/s3/urls/batch"
    }

    // MARK: - External / Utility Endpoints

    enum External {
        /// Network connectivity test endpoint
        static let connectivityTest = "https://www.apple.com/library/test/success.html"
    }

    // MARK: - SLD View Endpoints

    enum SLDView {
        /// Create a new SLD view
        static let create = "/sld-view/"

        /// Update an SLD view
        static func update(viewId: String) -> String {
            return "/sld-view/\(viewId)"
        }

        /// Add nodes to a view (creates mappings)
        static func addNodes(viewId: String) -> String {
            return "/sld-view/\(viewId)/nodes/add"
        }

        /// Remove nodes from a view
        static func removeNodes(viewId: String) -> String {
            return "/sld-view/\(viewId)/nodes/remove"
        }

        /// Update node position in a view
        static func updateNodePosition(viewId: String, nodeId: String) -> String {
            return "/sld-view/\(viewId)/nodes/\(nodeId)/position"
        }

        /// Update edge points/routing in a view
        static func updateEdgePoints(viewId: String, edgeId: String) -> String {
            return "/sld-view/\(viewId)/edges/\(edgeId)/points"
        }

        /// Update node collapse state in a view
        static func updateNodeCollapseState(viewId: String, nodeId: String) -> String {
            return "/sld-view/\(viewId)/nodes/\(nodeId)/collapse"
        }
    }

    // MARK: - Schedule Endpoints

    enum Schedule {
        /// Get current user's work blocks across all SLDs
        static let userSchedule = "/user/schedule"

        /// Get user schedule with date filters
        static func userSchedule(startDate: String?, endDate: String?, includePast: Bool = false) -> String {
            var queryParams: [String] = []
            if let start = startDate {
                queryParams.append("start_date=\(start)")
            }
            if let end = endDate {
                queryParams.append("end_date=\(end)")
            }
            if includePast {
                queryParams.append("include_past=true")
            }
            if queryParams.isEmpty {
                return userSchedule
            }
            return "\(userSchedule)?\(queryParams.joined(separator: "&"))"
        }
    }

    // MARK: - Attachment Endpoints

    enum Attachment {
        /// Get presigned URL for uploading attachment to S3
        static let presignedUpload = "/attachment/presigned-upload"

        /// Create attachment record
        static let create = "/attachment/create"

        /// Create attachment-node mapping
        static let mappingNode = "/attachment/mapping/node"

        /// Get list of attachments for a session
        static func list(sessionId: String) -> String {
            return "/attachment/?session_id=\(sessionId)"
        }

        /// Get presigned URL for downloading attachment
        static func presignedDownload(attachmentId: String) -> String {
            return "/attachment/presigned-download/\(attachmentId)"
        }

        /// Update attachment-node mapping
        static func mappingNodeUpdate(attachmentId: String, nodeId: String) -> String {
            return "/attachment/mapping/node/\(attachmentId)/\(nodeId)"
        }

        /// Delete attachment
        static func delete(attachmentId: String) -> String {
            return "/attachment/delete/\(attachmentId)"
        }

        /// Update attachment (e.g. visibility)
        static func update(attachmentId: String) -> String {
            return "/attachment/update/\(attachmentId)"
        }

        /// Bulk update attachment visibility
        static let bulkVisibility = "/attachment/bulk-visibility"
    }

    // MARK: - Extraction Endpoints

    enum Extraction {
        static let extractNameplateData = "/extraction/extract-nameplate-data"
        static let extractTempNameplateData = "/extraction/extract-temp-nameplate-data"
    }

    // MARK: - Equipment Endpoints

    enum Equipment {
        static let list = "/test-equipment"
    }

    // MARK: - Equipment Library Endpoints

    enum EquipmentLibrary {
        static let bulk = "/equipment-library/bulk"

        static func suggest(nodeId: String) -> String {
            return "/equipment-library/suggest/\(nodeId)"
        }
    }

    // MARK: - SLD Viewer Endpoints

    enum SldViewer {
        static let latest = "/sld-viewer/latest"
    }

    // MARK: - Sync Job Endpoints

    enum SyncJob {
        static let create = "/sync-jobs"
    }

    // MARK: - Graph Endpoints

    enum Graph {
        /// Get enriched node details (e.g., "/graph/nodes/node-123/enriched")
        static func enrichedNode(nodeId: String) -> String {
            return "/graph/nodes/\(nodeId)/enriched"
        }
    }
}
