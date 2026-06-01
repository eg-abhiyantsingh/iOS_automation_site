import SwiftUI
import SwiftData

struct TaskCreationView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var appState: AppStateManager
    
    // Required properties
    let sld: SLDV2
    @Binding var taskTitle: String
    @Binding var taskDescription: String
    let onCancel: () -> Void
    
    // Optional fixed node (when creating from node detail)
    let fixedNode: NodeV2?
    
    // Callback variants based on context
    let onSaveWithNode: ((NodeV2?, UserTaskForm?, Date?) -> Void)?
    let onSaveWithForm: ((UserTaskForm?, Date?) -> Void)?
    
    // State
    @State private var selectedNode: NodeV2?
    @State private var selectedForm: UserTaskForm?
    @State private var showingDueDatePicker = false
    @State private var selectedDueDate: Date?
    
    // Queries
    @Query private var allNodes: [NodeV2]
    @Query private var allForms: [UserTaskForm]
    
    @FocusState private var titleFocused: Bool
    
    // Computed properties
    private var availableNodes: [NodeV2] {
        allNodes
            .filter { !$0.is_deleted }
            .sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
    }
    
    private var sortedForms: [UserTaskForm] {
        allForms.sorted { $0.title < $1.title }
    }
    
    private var effectiveNode: NodeV2? {
        fixedNode ?? selectedNode
    }
    
    private var canSave: Bool {
        !taskTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !taskDescription.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
    
    // MARK: - Initializers
    
    // Initializer for flexible node selection (from task list)
    init(sld: SLDV2,
         taskTitle: Binding<String>,
         taskDescription: Binding<String>,
         onSave: @escaping (NodeV2?, UserTaskForm?, Date?) -> Void,
         onCancel: @escaping () -> Void) {
        self.sld = sld
        self._taskTitle = taskTitle
        self._taskDescription = taskDescription
        self.fixedNode = nil
        self.onSaveWithNode = onSave
        self.onSaveWithForm = nil
        self.onCancel = onCancel
    }
    
    // Initializer for fixed node (from node detail)
    init(node: NodeV2,
         sld: SLDV2,
         taskTitle: Binding<String>,
         taskDescription: Binding<String>,
         onSave: @escaping (UserTaskForm?, Date?) -> Void,
         onCancel: @escaping () -> Void) {
        self.sld = sld
        self._taskTitle = taskTitle
        self._taskDescription = taskDescription
        self.fixedNode = node
        self.onSaveWithNode = nil
        self.onSaveWithForm = onSave
        self.onCancel = onCancel
    }
    
    
    var body: some View {
        NavigationView {
            Form {
                // Task Details Section
                Section(AppStrings.Tasks.taskDetails) {
                    TextField(AppStrings.Tasks.title, text: $taskTitle)
                        .focused($titleFocused)
                    
                    VStack(alignment: .leading) {
                        Text(AppStrings.Tasks.description_)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        TextEditor(text: $taskDescription)
                            .frame(minHeight: 100)
                    }
                }
                
                // Due Date Section (Optional)
                Section {
                    HStack {
                        Label(AppStrings.Tasks.dueDate, systemImage: "calendar")
                        Spacer()
                        if let dueDate = selectedDueDate {
                            Text(dueDate, style: .date)
                                .foregroundColor(.blue)
                            Button {
                                selectedDueDate = nil
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                                    .imageScale(.small)
                            }
                        } else {
                            Button(AppStrings.Tasks.setDueDate) {
                                showingDueDatePicker = true
                                if selectedDueDate == nil {
                                    selectedDueDate = Date().addingTimeInterval(86400) // Tomorrow by default
                                }
                            }
                            .font(.subheadline)
                        }
                    }
                    
                    if showingDueDatePicker {
                        DatePicker(
                            AppStrings.Tasks.selectDate,
                            selection: Binding(
                                get: { selectedDueDate ?? Date() },
                                set: { selectedDueDate = $0 }
                            ),
                            displayedComponents: [.date]
                        )
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                    }
                }
                
                // Node Assignment Section
                if fixedNode != nil {
                    // Fixed node - just display it
                    Section {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(AppStrings.Tasks.taskAssignedTo(""))
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack {
                                Image(systemName: "cube")
                                    .foregroundColor(.blue)
                                Text(fixedNode!.label)
                                    .font(.body)
                            }
                            if let location = fixedNode!.location {
                                Text(location)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                } else {
                    // Flexible node selection
                    Section(AppStrings.Tasks.assignmentOptional) {
                        Picker(AppStrings.Tasks.assignToAsset, selection: $selectedNode) {
                            Text(AppStrings.Tasks.noAsset).tag(Optional<NodeV2>.none)
                            ForEach(availableNodes) { node in
                                Text(node.label).tag(Optional(node))
                            }
                        }
                        
                        if let node = selectedNode {
                            VStack(alignment: .leading, spacing: 4) {
                                Label(node.label, systemImage: "cube")
                                    .font(.subheadline)
                                    .foregroundColor(.blue)
                                if let location = node.location {
                                    Text(location)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                
                // Active Session Display
                if let activeSession = appState.activeSession {
                    Section {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                ActiveSessionBadge()
                                Text(AppStrings.Tasks.linkedToActiveSession)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Text(activeSession.name)
                                .font(.body)
                            Text("Type: \(activeSession.photo_type)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // Form Attachment Section - Simple Picker like the working version
                Section(AppStrings.Tasks.formOptional) {
                    Picker(AppStrings.Tasks.attachForm, selection: $selectedForm) {
                        Text(AppStrings.Tasks.noForm).tag(Optional<UserTaskForm>.none)
                        ForEach(allForms) { form in
                            Text(form.title).tag(Optional(form))
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Tasks.newTask)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel, action: onCancel)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        handleSave()
                    }
                    .fontWeight(.semibold)
                    .disabled(!canSave)
                }
            }
        }
        .onAppear {
            titleFocused = true
            // If we have a fixed node, set it as selected for consistency
            if let node = fixedNode {
                selectedNode = node
            }
        }
    }
    
    private func handleSave() {
        // Call the appropriate callback based on context
        if let onSaveWithNode = onSaveWithNode {
            // Flexible mode - pass the selected node, form, and due date
            onSaveWithNode(effectiveNode, selectedForm, selectedDueDate)
        } else if let onSaveWithForm = onSaveWithForm {
            // Fixed node mode - just pass the form and due date
            onSaveWithForm(selectedForm, selectedDueDate)
        }
    }
}

// MARK: - Preview Provider
//#Preview("Task Creation - Flexible Node") {
//    let container = try! ModelContainer(for: SLDV2.self, NodeV2.self, UserTaskForm.self)
//    let sld = SLDV2(id: UUID(), name: "Test SLD", is_deleted: false)
//    container.mainContext.insert(sld)
//    
//    TaskCreationView(
//        sld: sld,
//        taskTitle: .constant(""),
//        taskDescription: .constant(""),
//        onSave: { _, _ in },
//        onCancel: { }
//    )
//    .modelContainer(container)
//    .environmentObject(AppStateManager())
//}
//
//#Preview("Task Creation - Fixed Node") {
//    let container = try! ModelContainer(for: SLDV2.self, NodeV2.self, UserTaskForm.self)
//    let sld = SLDV2(id: UUID(), name: "Test SLD", is_deleted: false)
//    let node = NodeV2(id: UUID(), label: "Test Node", sld: sld)
//    container.mainContext.insert(sld)
//    container.mainContext.insert(node)
//    
//    TaskCreationView(
//        node: node,
//        sld: sld,
//        taskTitle: .constant(""),
//        taskDescription: .constant(""),
//        onSave: { _ in },
//        onCancel: { }
//    )
//    .modelContainer(container)
//    .environmentObject(AppStateManager())
//}
