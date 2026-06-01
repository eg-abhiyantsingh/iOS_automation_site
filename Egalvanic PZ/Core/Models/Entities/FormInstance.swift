import Foundation
import SwiftData

@Model
final class FormInstance {
    @Attribute(.unique) var id: UUID
    var form_master_id: UUID
    var created_at: Date
    var modified_at: Date?
    var form_submission: String?  // JSON string
    var submitted: Bool
    var is_deleted: Bool

    // Relationships
    var formMaster: UserTaskForm?  // Reference to the form template
    var linkedTasks: [UserTask] = []  // Tasks using this instance
    var linkedNodes: [NodeV2] = []  // Nodes for context

    init(
        id: UUID = UUID(),
        form_master_id: UUID,
        created_at: Date = Date(),
        modified_at: Date? = nil,
        form_submission: String? = nil,
        submitted: Bool = false,
        is_deleted: Bool = false,
        formMaster: UserTaskForm? = nil,
        linkedTasks: [UserTask] = [],
        linkedNodes: [NodeV2] = []
    ) {
        self.id = id
        self.form_master_id = form_master_id
        self.created_at = created_at
        self.modified_at = modified_at
        self.form_submission = form_submission
        self.submitted = submitted
        self.is_deleted = is_deleted
        self.formMaster = formMaster
        self.linkedTasks = linkedTasks
        self.linkedNodes = linkedNodes
    }

    // Computed properties
    var isValidJSON: Bool {
        guard let submission = form_submission,
              !submission.isEmpty else {
            return true  // Empty or nil is valid (not submitted yet)
        }

        // Try to parse as JSON
        if let data = submission.data(using: .utf8) {
            do {
                _ = try JSONSerialization.jsonObject(with: data, options: [])
                return true
            } catch {
                return false
            }
        }
        return false
    }

    var formTitle: String {
        formMaster?.title ?? "Untitled Form"
    }

    var statusDescription: String {
        if submitted {
            return "Submitted"
        } else if form_submission != nil && !form_submission!.isEmpty {
            return "In Progress"
        } else {
            return "Not Started"
        }
    }
}

// MARK: - DTO for API Communication
struct FormInstanceDTO: Codable {
    let id: String
    let form_master_id: String
    let created_at: String?
    let modified_at: String?
    let form_submission: AnyCodable?  // Can be dict or string
    let submitted: Bool?
    let is_deleted: Bool?

    // Helper to get form_submission as JSON string
    var formSubmissionString: String? {
        guard let submission = form_submission else { return nil }

        // If it's already a string, return it
        if let stringValue = submission.value as? String {
            return stringValue
        }

        // If it's a dictionary, convert to JSON string
        if let dictValue = submission.value as? [String: Any] {
            if let jsonData = try? JSONSerialization.data(withJSONObject: dictValue, options: []),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                return jsonString
            }
        }

        // Try to encode it as JSON
        if let encoded = try? JSONEncoder().encode(submission),
           let jsonString = String(data: encoded, encoding: .utf8) {
            return jsonString
        }

        return nil
    }
}

// MARK: - Extensions for conversion
extension FormInstance {
    static func fromDTO(_ dto: FormInstanceDTO, modelContext: ModelContext) -> FormInstance? {
        guard let id = UUID(uuidString: dto.id),
              let formMasterId = UUID(uuidString: dto.form_master_id) else {
            AppLogger.log(.error, "Invalid UUID in FormInstanceDTO", category: .form)
            return nil
        }

        // Parse dates
        let dateFormatter = ISO8601DateFormatter()
        let createdAt = dto.created_at.flatMap { dateFormatter.date(from: $0) } ?? Date()
        let modifiedAt = dto.modified_at.flatMap { dateFormatter.date(from: $0) }

        // form_submission can be a dict or string - convert to JSON string
        let submissionString = dto.formSubmissionString

        // Try to find the form master
        let descriptor = FetchDescriptor<UserTaskForm>(
            predicate: #Predicate<UserTaskForm> { form in
                form.id == formMasterId
            }
        )

        let formMaster = try? modelContext.fetch(descriptor).first

        return FormInstance(
            id: id,
            form_master_id: formMasterId,
            created_at: createdAt,
            modified_at: modifiedAt,
            form_submission: submissionString,
            submitted: dto.submitted ?? false,
            is_deleted: dto.is_deleted ?? false,
            formMaster: formMaster
        )
    }

    func toDTO() -> FormInstanceDTO {
        let dateFormatter = ISO8601DateFormatter()

        // Parse JSON string back to dictionary for DTO
        var submissionData: AnyCodable? = nil

        AppLogger.log(.debug, "[FormInstance.toDTO] Starting conversion for form instance: \(id), form_submission is nil: \(form_submission == nil)", category: .form)

        if let jsonString = form_submission {
            AppLogger.log(.debug, "[FormInstance.toDTO] form_submission length: \(jsonString.count), preview: \(String(jsonString.prefix(200)))", category: .form)

            if let data = jsonString.data(using: .utf8) {
                AppLogger.log(.debug, "[FormInstance.toDTO] Converted to Data, size: \(data.count) bytes", category: .form)

                do {
                    let json = try JSONSerialization.jsonObject(with: data, options: [])
                    submissionData = AnyCodable(json)
                    AppLogger.log(.debug, "[FormInstance.toDTO] Parsed JSON and wrapped in AnyCodable", category: .form)
                } catch {
                    AppLogger.log(.error, "[FormInstance.toDTO] JSON parsing failed: \(error), falling back to string", category: .form)
                    submissionData = AnyCodable(jsonString)
                }
            } else {
                AppLogger.log(.error, "[FormInstance.toDTO] Failed to convert string to Data", category: .form)
                submissionData = AnyCodable(jsonString)
            }
        } else {
            AppLogger.log(.debug, "[FormInstance.toDTO] form_submission is nil, sending null", category: .form)
        }

        return FormInstanceDTO(
            id: id.uuidString,
            form_master_id: form_master_id.uuidString,
            created_at: dateFormatter.string(from: created_at),
            modified_at: modified_at.map { dateFormatter.string(from: $0) },
            form_submission: submissionData,
            submitted: submitted,
            is_deleted: is_deleted
        )
    }
}