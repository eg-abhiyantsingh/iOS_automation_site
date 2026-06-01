import SwiftUI
import SwiftData

struct SimpleTaskCreationView: View {
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState

    // Optional fixed context (when creating from node detail)
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?
    let availableNodesForForms: [NodeV2]?

    // Callback when complete
    let onComplete: ((UserTask?) -> Void)?

    init(
        fixedNode: NodeV2? = nil,
        fixedSLD: SLDV2? = nil,
        availableNodesForForms: [NodeV2]? = nil,
        onComplete: ((UserTask?) -> Void)? = nil
    ) {
        self.fixedNode = fixedNode
        self.fixedSLD = fixedSLD
        self.availableNodesForForms = availableNodesForForms
        self.onComplete = onComplete
    }

    var body: some View {
        EntityCreationView(
            configuration: SimpleTaskCreationConfiguration(
                appState: appState,
                networkState: networkState,
                fixedNode: fixedNode,
                fixedSLD: fixedSLD,
                availableNodesForForms: availableNodesForForms,
                onComplete: { task in
                    onComplete?(task)
                }
            )
        )
        .environmentObject(appState)
        .environmentObject(networkState)
    }
}

// MARK: - Simple Task Creation Configuration (Single Node Selection)
struct SimpleTaskCreationConfiguration: EntityCreationConfiguration {
    typealias Entity = UserTask

    // Dependencies
    let appState: AppStateManager
    let networkState: NetworkState
    let onComplete: ((UserTask?) -> Void)?

    // Optional fixed context
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?
    let availableNodesForForms: [NodeV2]?

    // Query for available data
    @Query private var allNodesUnfiltered: [NodeV2]
    @Query private var availableForms: [UserTaskForm]
    @Query private var availableSLDs: [SLDV2]

    // Filter nodes by current SLD context
    private var allNodes: [NodeV2] {
        if let sld = fixedSLD {
            return allNodesUnfiltered.filter { $0.sld?.id == sld.id && !$0.is_deleted }
        }
        let activeSLDId = appState.activeSLDId
        return allNodesUnfiltered.filter { $0.sld?.id == activeSLDId && !$0.is_deleted }
    }

    // Computed property for available nodes
    var availableNodes: [NodeV2] {
        if let passedNodes = availableNodesForForms {
            return passedNodes
        }

        if let fixed = fixedNode {
            let filtered = allNodes.filter { node in
                node.id == fixed.id || node.parent_id == fixed.id
            }
            return filtered
        } else {
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
    var entityName: String { AppStrings.Sessions.simpleTask }
    var navigationTitle: String { AppStrings.Tasks.newSimpleTask }
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

        // SINGLE NODE PICKER - Main difference from complex task
        if fixedNode == nil {
            fields.append(EntityFieldConfiguration(
                id: "node",
                label: AppStrings.Tasks.asset,
                icon: "cube",
                type: .nodeV2Picker,  // Single picker instead of multiNodePicker
                isRequired: false,    // Optional: 0-1 nodes allowed
                section: AppStrings.Tasks.assignment,
                helpText: AppStrings.Tasks.optionalSelectAsset
            ))
        }

        // Procedure — filtered locally by the picked node's class.
        // When `fixedNode` is provided, seed metadata so the picker can
        // read the class id straight off it (no node field renders in
        // that case).
        var procedureField = EntityFieldConfiguration(
            id: "procedure",
            label: AppStrings.Tasks.procedure,
            icon: "list.bullet.clipboard",
            type: .procedurePicker,
            isRequired: false,
            section: AppStrings.Tasks.assignment,
            helpText: AppStrings.Tasks.procedureOptional
        )
        if let fixed = fixedNode {
            procedureField.metadata["fixedNode"] = fixed
        }
        fields.append(procedureField)

        // TEMPORARILY HIDDEN: Multiple forms allowed for simple tasks
//        var formFieldConfig = EntityFieldConfiguration(
//            id: "forms",
//            label: "Forms",
//            icon: "doc.text",
//            type: .userTaskFormMultiPicker,  // Multiple form picker
//            isRequired: false,
//            section: "Forms",
//            helpText: "Optional: Attach forms to this task"
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
        // Assignment always shows — it holds task type + procedure even when
        // the asset picker is hidden (fixedNode case). Order: assignment,
        // details, schedule, status.
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

        // Node is optional for simple tasks (0-1 allowed)
        // No validation needed

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

        // Get single node - either fixed or selected
        let node: NodeV2?
        if let fixed = fixedNode {
            node = fixed
        } else {
            node = storage.getValue(for: "node") as? NodeV2
        }

        // Get forms with their linked nodes (similar to complex task)
        let formData = storage.getValue(for: "forms") as? [[String: Any]] ?? []
        let formSelections: [(form: UserTaskForm, nodes: [NodeV2])] = formData.compactMap { data in
            guard let form = data["form"] as? UserTaskForm else { return nil }
            let nodes = data["nodes"] as? [NodeV2] ?? []
            return (form: form, nodes: nodes)
        }
        let forms = formSelections.map { $0.form }
        let completed = storage.getValue(for: "completed") as? Bool ?? false

        // Get SLD
        let sld: SLDV2?
        if let fixed = fixedSLD {
            sld = fixed
        } else if let selectedNode = node {
            sld = selectedNode.sld
        } else if let activeSession = appState.activeSession {
            // Try to get SLD from active session
            sld = activeSession.sld
        } else {
            sld = availableSLDs.first { $0.id == appState.activeSLDId }
        }

        guard let finalSLD = sld else {
            AppLogger.log(.notice, "Cannot create task without SLD", category: .task)
            return nil
        }

        // Node is now optional for simple tasks (0-1 allowed)

        // Create simple task with optional node (0-1 allowed)
        let createdTask = TaskService.createTask(
            title: title,
            description: description,
            form: nil,                      // Legacy single form - not used
            node: node,                     // Optional single node
            nodes: node.map { [$0] } ?? [], // Add to linkedNodes if present
            forms: forms,                   // Multiple forms allowed
            formSelections: formSelections, // Pass form-node mappings
            sld: finalSLD,
            dueDate: dueDate,
            taskType: taskType,
            procedureId: procedureId,
            modelContext: modelContext,
            activeSession: appState.activeSession
        ) { success, _, message in
            if success {
                if let errorMessage = message {
                    AppLogger.log(.info, errorMessage, category: .task)
                }
            } else {
                AppLogger.log(.error, "Failed to create task: \(message ?? "Unknown error")", category: .task)
            }
        }

        if let createdTask = createdTask {
            if completed {
                createdTask.completed = true
                try? modelContext.save()
            }
            onComplete?(createdTask)
        }
        return createdTask
    }

    @MainActor
    func handleError(_ error: Error, modelContext: ModelContext) {
        AppLogger.log(.error, "Task creation error: \(error)", category: .task)
    }

    func onCancel() {
        onComplete?(nil)
    }
}
