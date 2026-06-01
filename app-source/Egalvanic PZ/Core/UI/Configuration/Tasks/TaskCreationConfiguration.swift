import SwiftUI
import SwiftData

struct TaskCreationConfiguration: EntityCreationConfiguration {
    typealias Entity = UserTask
    
    // Dependencies
    let appState: AppStateManager
    let networkState: NetworkState
    let onComplete: ((UserTask?) -> Void)?
    
    // Optional fixed context
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?
    let availableNodesForForms: [NodeV2]?  // Passed in when creating from node

    // Query for available data
    @Query private var allNodesUnfiltered: [NodeV2]
    @Query private var availableForms: [UserTaskForm]
    @Query private var availableSLDs: [SLDV2]

    // Filter nodes by current SLD context
    private var allNodes: [NodeV2] {
        // If we have a fixed SLD, only show nodes from that SLD
        if let sld = fixedSLD {
            return allNodesUnfiltered.filter { $0.sld?.id == sld.id && !$0.is_deleted }
        }
        // Otherwise use active SLD
        let activeSLDId = appState.activeSLDId
        return allNodesUnfiltered.filter { $0.sld?.id == activeSLDId && !$0.is_deleted }
    }

    // Computed property for available nodes based on context
    var availableNodes: [NodeV2] {
        // If nodes were passed in (from node detail context), use those
        if let passedNodes = availableNodesForForms {
            AppLogger.log(.debug, "Using \(passedNodes.count) passed nodes for forms", category: .task)
            return passedNodes
        }

        // Otherwise, filter based on fixed node
        if let fixed = fixedNode {
            AppLogger.log(.debug, "Fixed node: \(fixed.label) (ID: \(fixed.id)), Total nodes: \(allNodes.count)", category: .task)

            // When creating from a node, only allow the node itself and its children
            let filtered = allNodes.filter { node in
                let isSelf = node.id == fixed.id
                let isChild = node.parent_id == fixed.id
                return isSelf || isChild
            }
            AppLogger.log(.debug, "Filtered to \(filtered.count) nodes (self + children)", category: .task)
            return filtered
        } else {
            // When creating generally, all nodes are available
            return allNodes
        }
    }
    
    init(
        appState: AppStateManager,
        networkState: NetworkState,
        fixedNode: NodeV2? = nil,
        fixedSLD: SLDV2? = nil,
        availableNodesForForms: [NodeV2]? = nil,
        onComplete: ((UserTask?) -> Void)? = nil
    ) {
        self.appState = appState
        self.networkState = networkState
        self.fixedNode = fixedNode
        self.fixedSLD = fixedSLD
        self.availableNodesForForms = availableNodesForForms
        self.onComplete = onComplete
    }
    
    // MARK: - Basic Info
    var entityName: String { AppStrings.Tasks.task }
    var navigationTitle: String { AppStrings.Tasks.newTask }
    var createButtonTitle: String { AppStrings.Sessions.createTask }
    
    // MARK: - Field Configuration
    func fields() -> [EntityFieldConfiguration] {
        var fields: [EntityFieldConfiguration] = []

        // Task type — matches the frontend's hard-coded set, default PM.
        // Lives in the Assignment section alongside asset + procedure.
        fields.append(EntityFieldConfiguration(
            id: "taskType",
            label: AppStrings.Tasks.taskType,
            icon: "tag",
            type: .taskTypePicker,
            isRequired: true,
            section: AppStrings.Tasks.assignment,
            validationRules: [
                ValidationRule { value in
                    if let raw = value as? String,
                       !raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        return .valid
                    }
                    return .invalid(AppStrings.Tasks.taskTypeIsRequired)
                }
            ]
        ))

        // Title field
        fields.append(EntityFieldConfiguration(
            id: "title",
            label: AppStrings.Tasks.title,
            icon: "pencil",
            type: .text(placeholder: AppStrings.Tasks.enterTaskTitle),
            isRequired: true,
            section: AppStrings.Tasks.taskDetails,
            validationRules: [
                ValidationRule { value in
                    guard let text = value as? String,
                          !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                        return .invalid(AppStrings.Tasks.titleIsRequired)
                    }
                    return .valid
                }
            ]
        ))
        
        // Description field
        fields.append(EntityFieldConfiguration(
            id: "description",
            label: AppStrings.Tasks.description_,
            icon: "text.alignleft",
            type: .text(placeholder: AppStrings.Tasks.describeTask),
            isRequired: true,
            section: AppStrings.Tasks.taskDetails,
            validationRules: [
                ValidationRule { value in
                    guard let text = value as? String,
                          !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                        return .invalid(AppStrings.Tasks.descriptionIsRequired)
                    }
                    return .valid
                }
            ]
        ))
        
        // Due date field
        fields.append(EntityFieldConfiguration(
            id: "dueDate",
            label: AppStrings.Tasks.dueDate,
            icon: "calendar",
            type: .date(includeTime: false),
            isRequired: false,
            section: AppStrings.Tasks.schedule,
            helpText: AppStrings.Tasks.optionalDueDate
        ))
        
        // Node picker - use multi-picker if not fixed
        if fixedNode == nil {
            fields.append(EntityFieldConfiguration(
                id: "nodes",
                label: AppStrings.Tabs.assets,
                icon: "cube",
                type: .nodeV2MultiPicker,
                isRequired: false,
                section: AppStrings.Tasks.assignment,
                helpText: nil
            ))
        }

        // Procedure — filtered locally by the first picked node's class
        // (matches the web client's nodeClassId filter). When `fixedNode`
        // is set, seed it via metadata so the picker can derive the
        // class without reading from storage.
        var procedureField = EntityFieldConfiguration(
            id: "procedure",
            label: AppStrings.Tasks.procedure,
            icon: "list.bullet.clipboard",
            type: .procedurePicker,
            isRequired: false,
            section: AppStrings.Tasks.assignment,
            helpText: AppStrings.Tasks.procedureOptional,
            metadata: ["nodeFieldId": "nodes"]
        )
        if let fixed = fixedNode {
            procedureField.metadata["fixedNode"] = fixed
        }
        fields.append(procedureField)

        // Listen for new assets toggle
        fields.append(EntityFieldConfiguration(
            id: "listening",
            label: AppStrings.Tasks.listenForNewAssets,
            icon: "ear",
            type: .toggle,
            isRequired: false,
            section: AppStrings.Tasks.assignment
        ))

        // TEMPORARILY HIDDEN: Form attachment - always use multi-picker
        // Pass available nodes through metadata for form-node linking
//        var formFieldConfig = EntityFieldConfiguration(
//            id: "forms",
//            label: "Forms",
//            icon: "doc.text",
//            type: .userTaskFormMultiPicker,
//            isRequired: false,
//            section: "Forms",
//            helpText: nil
//        )
//        formFieldConfig.metadata = ["availableNodes": availableNodes]
//        fields.append(formFieldConfig)

        // Completed toggle
        fields.append(EntityFieldConfiguration(
            id: "completed",
            label: AppStrings.Tasks.markAsCompleted,
            icon: "checkmark.circle",
            type: .toggle,
            isRequired: false,
            section: AppStrings.Tasks.status
        ))
        
        return fields
    }
    
    func sections() -> [String]? {
        // Assignment first (task type, asset, procedure, listen), then the
        // descriptive details, schedule, and finally status.
        return [
            AppStrings.Tasks.assignment,
            AppStrings.Tasks.taskDetails,
            AppStrings.Tasks.schedule,
            AppStrings.Tasks.status
        ]
    }
    
    // MARK: - SwiftData Models
    func swiftDataModels() -> [any PersistentModel.Type] {
        [NodeV2.self, UserTaskForm.self, SLDV2.self, Procedure.self]
    }
    
    // MARK: - Validation
    func validateForm(storage: FieldValueStorage) -> ValidationResult {
        // Title is required
        guard let title = storage.getValue(for: "title") as? String,
              !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return .invalid(AppStrings.Tasks.titleIsRequired)
        }

        // Description is required
        guard let description = storage.getValue(for: "description") as? String,
              !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return .invalid(AppStrings.Tasks.descriptionIsRequired)
        }

        // If due date is set, ensure it's not in the past
        if let dueDate = storage.getValue(for: "dueDate") as? Date {
            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())
            let dueDateStart = calendar.startOfDay(for: dueDate)

            if dueDateStart < today {
                return .invalid(AppStrings.Tasks.dueDateCannotBeInPast)
            }
        }

        return .valid
    }
    
    // MARK: - Actions
    @MainActor
    func onCreate(storage: FieldValueStorage, modelContext: ModelContext) -> UserTask? {
        // Get values from storage
        let title = storage.getValue(for: "title") as? String ?? ""
        let description = storage.getValue(for: "description") as? String ?? ""
        let dueDate = storage.getValue(for: "dueDate") as? Date
        // Task type defaults to PM (matches frontend) so a user who never
        // taps the picker still ends up with a valid backend code.
        let taskType = (storage.getValue(for: "taskType") as? String).flatMap {
            $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : $0
        } ?? TaskType.pm.rawValue
        let procedureId = storage.getValue(for: "procedure") as? UUID

        // Get nodes - either fixed node or multi-selected nodes
        var nodes: [NodeV2] = []
        if let fixed = fixedNode {
            nodes = [fixed]
        } else if let selectedNodes = storage.getValue(for: "nodes") as? [NodeV2] {
            nodes = selectedNodes
        }

        // Get forms with their linked nodes
        let formData = storage.getValue(for: "forms") as? [[String: Any]] ?? []
        let formSelections: [(form: UserTaskForm, nodes: [NodeV2])] = formData.compactMap { data in
            guard let form = data["form"] as? UserTaskForm else { return nil }
            let nodes = data["nodes"] as? [NodeV2] ?? []
            return (form: form, nodes: nodes)
        }
        let forms = formSelections.map { $0.form }  // Extract just the forms for backward compatibility
        let completed = storage.getValue(for: "completed") as? Bool ?? false
        
        // Get SLD
        let sld: SLDV2?
        if let fixed = fixedSLD {
            sld = fixed
        } else if let firstNode = nodes.first {
            sld = firstNode.sld
        } else if let activeSession = appState.activeSession {
            // Try to get SLD from active session
            sld = activeSession.sld
        } else {
            // Try to get the active SLD
            sld = availableSLDs.first { $0.id == appState.activeSLDId }
        }

        guard let finalSLD = sld else {
            AppLogger.log(.notice, "Cannot create task without SLD", category: .task)
            return nil
        }
        
        // Create task using TaskService
        let createdTask = TaskService.createTask(
            title: title,
            description: description,
            form: nil,  // Legacy single form - handled by forms array
            node: nil,  // Legacy single node - handled by nodes array
            nodes: nodes,
            forms: forms,
            formSelections: formSelections,  // Pass form-node mappings
            sld: finalSLD,
            dueDate: dueDate,
            taskType: taskType,
            procedureId: procedureId,
            modelContext: modelContext,
            activeSession: appState.activeSession
        ) { success, _, message in
            if success {
                if let errorMessage = message {
                    AppLogger.log(.notice, errorMessage, category: .task)
                }
            } else {
                AppLogger.log(.error, "Failed to create task: \(message ?? "Unknown error")", category: .task)
            }
        }

        if let createdTask = createdTask {
            if completed {
                createdTask.completed = true
                createdTask.submitted_at = Date()
                try? modelContext.save()
            }
            let listening = storage.getValue(for: "listening") as? Bool ?? false
            if listening {
                networkState.toggleListening(createdTask.id)
            }
            onComplete?(createdTask)
        }

        return createdTask
    }
    
    func onCancel() {
        onComplete?(nil)
    }
    
    // MARK: - Context View
    func contextView() -> AnyView? {
        guard fixedNode != nil || appState.activeSession != nil else { return nil }
        
        return AnyView(
            VStack(spacing: 12) {
                // Fixed node context
                if let node = fixedNode {
                    VStack(alignment: .leading, spacing: 8) {
                        Label(AppStrings.Tasks.creatingTaskFor, systemImage: "cube")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(node.label)
                            .font(.headline)

                        if let location = node.location {
                            Text(location)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }

                // Active session display
                if let session = appState.activeSession {
                    HStack {
                        ActiveSessionBadge()

                        VStack(alignment: .leading, spacing: 2) {
                            Text(AppStrings.Tasks.linkedToActiveSession)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(session.name)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text(AppStrings.Tasks.typeLabel(session.photo_type))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }

                        Spacer()
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                }
            }
        )
    }
    
}
