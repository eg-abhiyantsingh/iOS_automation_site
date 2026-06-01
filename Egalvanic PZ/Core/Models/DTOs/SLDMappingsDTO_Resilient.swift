import Foundation

// Example of resilient decoding for SLDMappingsDTO
struct SLDMappingsDTO_Resilient: Codable {
    var issue_task: [MappingIssueTaskDTO]
    var task_session: [MappingTaskSessionDTO]
    var quote_task: [MappingQuoteTaskDTO]
    var user_task: [MappingUserTaskDTO]
    var task_node: [MappingTaskNodeDTO]?
    var task_form: [MappingTaskFormDTO]?

    // Define only the keys we know about in the current version
    private enum CodingKeys: String, CodingKey {
        case issue_task
        case task_session
        case quote_task
        case user_task
        case task_node
        case task_form
    }

    // Custom decoder that ignores unknown keys
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        // Decode required fields
        self.issue_task = try container.decode([MappingIssueTaskDTO].self, forKey: .issue_task)
        self.task_session = try container.decode([MappingTaskSessionDTO].self, forKey: .task_session)
        self.quote_task = try container.decode([MappingQuoteTaskDTO].self, forKey: .quote_task)
        self.user_task = try container.decode([MappingUserTaskDTO].self, forKey: .user_task)

        // Decode optional fields (won't fail if missing)
        self.task_node = try container.decodeIfPresent([MappingTaskNodeDTO].self, forKey: .task_node)
        self.task_form = try container.decodeIfPresent([MappingTaskFormDTO].self, forKey: .task_form)
    }
}