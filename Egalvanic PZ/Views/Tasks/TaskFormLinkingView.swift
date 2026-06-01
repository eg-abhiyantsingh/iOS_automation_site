import SwiftUI
import SwiftData

// MARK: - Form Selection Model (matching creation view)
struct TaskFormSelection: Identifiable, Equatable {
    let id = UUID()  // Unique ID for each selection instance
    let form: UserTaskForm
    var linkedNodes: [NodeV2] = []
    var existingFormInstanceId: UUID? = nil  // Track if this is an existing instance

    static func == (lhs: TaskFormSelection, rhs: TaskFormSelection) -> Bool {
        lhs.id == rhs.id &&
        lhs.form.id == rhs.form.id &&
        lhs.linkedNodes.map { $0.id } == rhs.linkedNodes.map { $0.id }
    }
}

// MARK: - Task Form Linking View (matching creation UX)
struct TaskFormLinkingView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    let task: UserTask
    let contextNode: NodeV2?  // If opened from asset detail, this is the asset
    let onComplete: () -> Void

    @Query(filter: #Predicate<UserTaskForm> { !$0.is_deleted }) private var availableForms: [UserTaskForm]
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var allNodes: [NodeV2]

    @State private var selectedFormSelections: [TaskFormSelection] = []
    @State private var showingFormSelector = false
    @State private var editingSelection: TaskFormSelection? = nil
    @State private var isSaving = false
    @State private var saveError: Error?

    // Get nodes available for form linking based on task context
    private var availableNodes: [NodeV2] {
        // If task has a specific SLD, filter nodes by that
        if let sld = task.sld {
            let sldNodes = allNodes.filter { $0.sld?.id == sld.id }

            // If opened from an asset detail view, limit to that asset and its children
            if let contextNode = contextNode {
                return sldNodes.filter { node in
                    node.id == contextNode.id || node.parent_id == contextNode.id
                }
            }

            // If opened from task list or session list, all SLD nodes are available
            return sldNodes
        }

        // Fallback to all nodes if no SLD (shouldn't happen normally)
        return allNodes
    }

    // Separate the nodes into two groups for better UX
    private var linkedNodesFirst: [NodeV2] {
        let linkedNodeIds = Set(task.linkedNodes.map { $0.id })
        return availableNodes.filter { linkedNodeIds.contains($0.id) }
    }

    private var unlinkedNodes: [NodeV2] {
        let linkedNodeIds = Set(task.linkedNodes.map { $0.id })
        return availableNodes.filter { !linkedNodeIds.contains($0.id) }
    }

    var body: some View {
        NavigationStack {
            Form {
                // Forms Section - matching creation view style
                Section {
                    formSelectionContent
                } header: {
                    HStack {
                        Label(AppStrings.Tasks.forms, systemImage: "doc.text")
                            .font(.headline)
                        Spacer()
                        Button(AppStrings.Tasks.select) {
                            showingFormSelector = true
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                    }
                } footer: {
                    Text(AppStrings.Tasks.formsFooter)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle(AppStrings.Tasks.manageForms)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.save) {
                        saveFormLinks()
                    }
                    .fontWeight(.semibold)
                    .disabled(isSaving)
                }
            }
        }
        .sheet(isPresented: $showingFormSelector) {
            FormSelectorView(
                task: task,
                availableForms: availableForms,
                selectedFormSelections: $selectedFormSelections
            )
        }
        .sheet(item: $editingSelection) { selection in
            NodeSelectionForFormView(
                task: task,
                formSelection: selection,
                availableNodes: availableNodes,
                onSave: { updatedSelection in
                    // Update the selection with new nodes
                    if let index = selectedFormSelections.firstIndex(where: { $0.id == updatedSelection.id }) {
                        selectedFormSelections[index] = updatedSelection
                    }
                }
            )
        }
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { saveError != nil },
            set: { if !$0 { saveError = nil } }
        )) {
            Button(AppStrings.Common.ok) { saveError = nil }
        } message: {
            Text(saveError?.localizedDescription ?? AppStrings.AssetsExtra.unknownError)
        }
        .overlay {
            if isSaving {
                savingOverlay
            }
        }
        .onAppear {
            loadExistingFormInstances()
        }
    }

    @ViewBuilder
    private var formSelectionContent: some View {
        if selectedFormSelections.isEmpty {
            Text(AppStrings.Tasks.noFormsSelected)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .italic()
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            ForEach(selectedFormSelections) { selection in
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            // Form title with instance number
                            HStack {
                                Image(systemName: selection.form.is_global ? "globe" : "doc.text")
                                    .font(.caption)
                                    .foregroundColor(selection.form.is_global ? .blue : .secondary)
                                Text(selection.form.title)
                                    .font(.subheadline)

                                // Show instance number if there are duplicates
                                let count = selectedFormSelections.filter { $0.form.id == selection.form.id }.count
                                if count > 1 {
                                    let instanceNum = selectedFormSelections
                                        .filter { $0.form.id == selection.form.id && $0.id <= selection.id }
                                        .count
                                    Text(AppStrings.Tasks.instanceLabel(instanceNum, count))
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }

                            // Node info for this form instance
                            if !selection.linkedNodes.isEmpty {
                                HStack {
                                    Image(systemName: "cube")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                    Text(nodesSummary(selection.linkedNodes))
                                        .font(.caption)
                                        .foregroundColor(.blue)
                                        .lineLimit(1)
                                }
                            }
                        }

                        Spacer()

                        // Action buttons
                        HStack(spacing: 12) {
                            // Node link button
                            Button {
                                editingSelection = selection
                            } label: {
                                Image(systemName: "cube.fill")
                                    .font(.body)
                                    .foregroundColor(.blue)
                                    .frame(width: 30, height: 30)
                            }
                            .buttonStyle(BorderlessButtonStyle())

                            // Remove button
                            Button {
                                selectedFormSelections.removeAll { $0.id == selection.id }
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                                    .frame(width: 24, height: 24)
                            }
                            .buttonStyle(BorderlessButtonStyle())
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    private var savingOverlay: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
            VStack(spacing: 12) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
                    .scaleEffect(1.2)
                Text(AppStrings.AssetsExtra.saving)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(24)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func loadExistingFormInstances() {
        // Load existing form instances linked to this task
        for formInstance in task.linkedFormInstances where !formInstance.is_deleted {
            if let form = formInstance.formMaster {
                var selection = TaskFormSelection(
                    form: form,
                    linkedNodes: formInstance.linkedNodes
                )
                selection.existingFormInstanceId = formInstance.id  // Track the instance ID
                selectedFormSelections.append(selection)
            }
        }
    }

    private func nodesSummary(_ nodes: [NodeV2]) -> String {
        if nodes.isEmpty {
            return AppStrings.Tasks.noNodesLinked
        } else if nodes.count == 1 {
            return nodes[0].label
        } else {
            return "\(nodes[0].label) \(AppStrings.Tasks.moreNodes(nodes.count - 1))"
        }
    }

    private func saveFormLinks() {
        isSaving = true

        Task {
            do {
                // Step 1: Handle deletions - find form instances that were removed
                // Get the IDs of form instances that are still selected
                let selectedInstanceIds = Set(selectedFormSelections.compactMap { $0.existingFormInstanceId })

                // Find instances that exist in the task but are not in the selected list
                let formInstancesToDelete = task.linkedFormInstances.filter { instance in
                    !instance.is_deleted &&
                    !selectedInstanceIds.contains(instance.id)
                }

                // Delete removed form instances
                if !formInstancesToDelete.isEmpty {
                    AppLogger.log(.info, "Removing \(formInstancesToDelete.count) form instances: \(formInstancesToDelete.map { $0.id.uuidString }.joined(separator: ", "))", category: .form)

                    // Mark all as deleted locally
                    for formInstance in formInstancesToDelete {
                        formInstance.is_deleted = true
                    }

                    // Batch remove all at once
                    let idsToDelete = formInstancesToDelete.map { $0.id }
                    try await TaskMappingService.shared.removeTaskFormInstances(
                        taskId: task.id,
                        formInstanceIds: idsToDelete
                    )
                }

                // Step 2: Handle additions - only process selections without existing instances
                let newSelections = selectedFormSelections.filter { selection in
                    selection.existingFormInstanceId == nil
                }

                // Create new form instances and mappings only for new selections
                for selection in newSelections {
                    // Use TaskMappingService to properly handle online/offline modes
                    let nodeIds = selection.linkedNodes.map { $0.id }

                    AppLogger.log(.info, "Creating form instance for '\(selection.form.title)' with \(nodeIds.count) linked nodes: \(nodeIds.map { $0.uuidString }.joined(separator: ", "))", category: .form)

                    // This method handles both online API calls and offline sync properly
                    let formInstance = try await TaskMappingService.shared.createFormInstanceAndLink(
                        taskId: task.id,
                        formMasterId: selection.form.id,
                        nodeIds: nodeIds,
                        modelContext: modelContext
                    )

                    // Update the local form instance with the linked nodes for UI
                    formInstance.linkedNodes = selection.linkedNodes
                    formInstance.formMaster = selection.form

                    // The TaskMappingService already added it to task.linkedFormInstances
                }

                // Save context
                try modelContext.save()

                await MainActor.run {
                    isSaving = false
                    onComplete()
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isSaving = false
                    saveError = error
                }
            }
        }
    }
}

// MARK: - Form Selector View (matching creation style)
struct FormSelectorView: View {
    @Environment(\.dismiss) private var dismiss

    let task: UserTask
    let availableForms: [UserTaskForm]
    @Binding var selectedFormSelections: [TaskFormSelection]

    @State private var searchText = ""

    // Get node class IDs from both legacy and linkedNodes properties
    private var taskNodeClassIds: Set<UUID> {
        var classIds = Set<UUID>()

        // From legacy single node
        if let nodeClass = task.node?.node_class {
            classIds.insert(nodeClass.id)
        }

        // From linkedNodes array
        for node in task.linkedNodes where !node.is_deleted {
            if let nodeClass = node.node_class {
                classIds.insert(nodeClass.id)
            }
        }

        return classIds
    }

    private var hasLinkedNodes: Bool {
        task.node != nil || !task.linkedNodes.isEmpty
    }

    // Filter forms based on node classes, then apply search
    private var filteredForms: [UserTaskForm] {
        let nodeForms: [UserTaskForm]

        if hasLinkedNodes {
            // Show forms that match the linked nodes' classes AND general forms
            nodeForms = availableForms.filter { form in
                // Include forms that match the node class
                if let formNodeClassId = form.node_class_id {
                    return taskNodeClassIds.contains(formNodeClassId)
                }
                // Also include general forms (no node_class_id)
                return true
            }
        } else {
            // No linked nodes - show only general forms (no node_class_id)
            nodeForms = availableForms.filter { form in
                form.node_class_id == nil
            }
        }

        // Apply search filter if search text is present
        if searchText.isEmpty {
            return nodeForms
        } else {
            return nodeForms.filter { form in
                form.title.localizedCaseInsensitiveContains(searchText)
            }
        }
    }

    // Group forms by node_subtype
    private var formsGroupedBySubtype: [(String, [UserTaskForm])] {
        let grouped = Dictionary(grouping: filteredForms) { form in
            form.node_subtype ?? AppStrings.Tasks.general
        }

        // Sort by subtype name, with "General" last
        return grouped.sorted { lhs, rhs in
            if lhs.key == AppStrings.Tasks.general { return false }
            if rhs.key == AppStrings.Tasks.general { return true }
            return lhs.key < rhs.key
        }.map { (key, forms) in
            (key, forms.sorted { $0.title < $1.title })
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.Tasks.searchForms, text: $searchText)
                        .textFieldStyle(.plain)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                // Forms list grouped by subtype
                if formsGroupedBySubtype.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "doc.text.magnifyingglass")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)

                        Text(hasLinkedNodes ? AppStrings.Tasks.noFormsForAssets : AppStrings.Tasks.noGeneralForms)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(formsGroupedBySubtype, id: \.0) { subtype, forms in
                            Section {
                                ForEach(forms) { form in
                                    Button {
                                        // Add a new selection instance for this form
                                        let selection = TaskFormSelection(form: form, linkedNodes: [])
                                        selectedFormSelections.append(selection)
                                        dismiss()
                                    } label: {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                HStack {
                                                    Image(systemName: form.is_global ? "globe" : "doc.text")
                                                        .font(.caption)
                                                        .foregroundColor(form.is_global ? .blue : .secondary)
                                                    Text(form.title)
                                                        .font(.subheadline)
                                                        .foregroundColor(.primary)
                                                }
                                            }

                                            Spacer()

                                            // Show count if already selected
                                            let selectionCount = selectedFormSelections.filter { $0.form.id == form.id }.count
                                            if selectionCount > 0 {
                                                Text("\(selectionCount)")
                                                    .font(.caption)
                                                    .foregroundColor(.white)
                                                    .padding(.horizontal, 8)
                                                    .padding(.vertical, 2)
                                                    .background(Color.blue)
                                                    .clipShape(Capsule())
                                            }

                                            Image(systemName: "plus.circle.fill")
                                                .foregroundColor(.blue)
                                        }
                                        .padding(.vertical, 4)
                                    }
                                    .buttonStyle(.plain)
                                }
                            } header: {
                                Text(subtype)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle(AppStrings.Tasks.selectForm)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Node Selection for Form View (matching creation style)
struct NodeSelectionForFormView: View {
    @Environment(\.dismiss) private var dismiss

    let task: UserTask
    @State var formSelection: TaskFormSelection
    let availableNodes: [NodeV2]
    let onSave: (TaskFormSelection) -> Void

    @State private var selectedNodeIds: Set<UUID>
    @State private var searchText = ""

    init(task: UserTask, formSelection: TaskFormSelection, availableNodes: [NodeV2], onSave: @escaping (TaskFormSelection) -> Void) {
        self.task = task
        self.formSelection = formSelection
        self.availableNodes = availableNodes
        self.onSave = onSave
        self._selectedNodeIds = State(initialValue: Set(formSelection.linkedNodes.map { $0.id }))
    }

    // Separate nodes into two groups
    private var taskLinkedNodes: [NodeV2] {
        let linkedIds = Set(task.linkedNodes.map { $0.id })
        let filtered = searchText.isEmpty ? availableNodes : availableNodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            (node.location?.localizedCaseInsensitiveContains(searchText) ?? false) ||
            (node.qr_code?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
        return filtered.filter { linkedIds.contains($0.id) }.sorted { $0.label < $1.label }
    }

    private var otherNodes: [NodeV2] {
        let linkedIds = Set(task.linkedNodes.map { $0.id })
        let filtered = searchText.isEmpty ? availableNodes : availableNodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            (node.location?.localizedCaseInsensitiveContains(searchText) ?? false) ||
            (node.qr_code?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
        return filtered.filter { !linkedIds.contains($0.id) }.sorted { $0.label < $1.label }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.CommonExtra.searchAssets, text: $searchText)
                        .textFieldStyle(.plain)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                // Selection count
                if !selectedNodeIds.isEmpty {
                    HStack {
                        Text(AppStrings.Tasks.assetsSelected(selectedNodeIds.count))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button(AppStrings.AssetsExtra.clearAll) {
                            selectedNodeIds.removeAll()
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }

                // Nodes list with sections
                List {
                    // Task-linked nodes section (if any)
                    if !taskLinkedNodes.isEmpty {
                        Section {
                            ForEach(taskLinkedNodes) { node in
                                nodeRow(node)
                            }
                        } header: {
                            Text(AppStrings.Tasks.assetsLinkedToTask)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    // Other available nodes section
                    if !otherNodes.isEmpty {
                        Section {
                            ForEach(otherNodes) { node in
                                nodeRow(node)
                            }
                        } header: {
                            Text(taskLinkedNodes.isEmpty ? AppStrings.Tasks.availableAssets : AppStrings.Tasks.otherAvailableAssets)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    // Show message if no nodes available
                    if taskLinkedNodes.isEmpty && otherNodes.isEmpty {
                        Text(AppStrings.Tasks.noAssetsAvailable)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .italic()
                            .padding()
                    }
                }
                .listStyle(.insetGrouped)
            }
            .navigationTitle(AppStrings.Tasks.linkAssetsToForm)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        // Update the form selection with selected nodes
                        formSelection.linkedNodes = availableNodes.filter { selectedNodeIds.contains($0.id) }
                        onSave(formSelection)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }

    @ViewBuilder
    private func nodeRow(_ node: NodeV2) -> some View {
        Button {
            if selectedNodeIds.contains(node.id) {
                selectedNodeIds.remove(node.id)
            } else {
                selectedNodeIds.insert(node.id)
            }
        } label: {
            HStack {
                Image(systemName: selectedNodeIds.contains(node.id) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(selectedNodeIds.contains(node.id) ? .blue : .gray)
                    .imageScale(.large)

                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                        .foregroundColor(.primary)

                    HStack(spacing: 8) {
                        if let location = node.location {
                            Label(location, systemImage: "location")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        if let qrCode = node.qr_code {
                            Label(qrCode, systemImage: "qrcode")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                // Show indicator if this is a task-linked node
                if task.linkedNodes.contains(where: { $0.id == node.id }) {
                    Image(systemName: "link")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}