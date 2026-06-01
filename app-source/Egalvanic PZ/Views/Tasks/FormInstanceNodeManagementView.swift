import SwiftUI
import SwiftData

struct FormInstanceNodeManagementView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    let formInstance: FormInstance
    let task: UserTask

    @State private var linkedNodes: [NodeV2]
    @State private var hasChanges = false
    @State private var isSaving = false
    @State private var showingNodeSelector = false

    init(formInstance: FormInstance, task: UserTask) {
        self.formInstance = formInstance
        self.task = task
        _linkedNodes = State(initialValue: formInstance.linkedNodes.filter { !$0.is_deleted })
    }

    var availableNodes: [NodeV2] {
        // Only show nodes that are linked to the task
        task.linkedNodes.filter { !$0.is_deleted }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Form info header
                formInfoSection

                // Linked nodes list
                if linkedNodes.isEmpty {
                    emptyStateView
                } else {
                    nodesList
                }
            }
            .navigationTitle(AppStrings.Tasks.formInstanceNodes)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    if hasChanges {
                        Button(AppStrings.Common.save) {
                            saveChanges()
                        }
                        .fontWeight(.semibold)
                    } else {
                        Button(AppStrings.Tasks.addNode) {
                            showingNodeSelector = true
                        }
                    }
                }
            }
            .sheet(isPresented: $showingNodeSelector) {
                NodeSelectorForFormInstance(
                    formInstance: formInstance,
                    availableNodes: availableNodes.filter { node in
                        // Only show nodes not already linked
                        !linkedNodes.contains(where: { $0.id == node.id })
                    },
                    onSelect: { node in
                        addNode(node)
                    }
                )
            }
        }
    }

    private var formInfoSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: formInstance.formMaster?.is_global == true ? "globe" : "doc.text")
                    .foregroundColor(formInstance.formMaster?.is_global == true ? .blue : .secondary)
                VStack(alignment: .leading) {
                    Text(formInstance.formMaster?.title ?? AppStrings.Tasks.formInstance)
                        .font(.headline)
                    Text(formInstance.submitted ? AppStrings.Tasks.submitted : AppStrings.Common.pending)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding()
            .background(Color(.systemGray6))
        }
    }

    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "cube.transparent")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            Text(AppStrings.Tasks.noNodesLinkedToForm)
                .font(.headline)
                .foregroundColor(.secondary)
            Text(AppStrings.Tasks.linkNodesDescription)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(action: { showingNodeSelector = true }) {
                Label(AppStrings.Tasks.linkNode, systemImage: "plus.circle.fill")
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .buttonStyle(.borderedProminent)
            Spacer()
        }
        .padding()
    }

    private var nodesList: some View {
        List {
            Section {
                ForEach(linkedNodes) { node in
                    HStack {
                        Image(systemName: "cube")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading) {
                            Text(node.label)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            if let location = node.location {
                                Text(location)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        Spacer()
                    }
                    .padding(.vertical, 4)
                }
                .onDelete { indexSet in
                    removeNodes(at: indexSet)
                }
            } header: {
                Text(AppStrings.Tasks.linkedNodesCount(linkedNodes.count))
            } footer: {
                Text(AppStrings.Tasks.linkedNodesFooter)
                    .font(.caption)
            }
        }
    }

    private func addNode(_ node: NodeV2) {
        if !linkedNodes.contains(where: { $0.id == node.id }) {
            linkedNodes.append(node)
            hasChanges = true
        }
    }

    private func removeNodes(at offsets: IndexSet) {
        linkedNodes.remove(atOffsets: offsets)
        hasChanges = true
    }

    private func saveChanges() {
        guard !isSaving else { return }
        isSaving = true

        Task {
            do {
                // Update local relationships
                formInstance.linkedNodes = linkedNodes
                try modelContext.save()

                // Use TaskMappingService to handle both online and offline modes
                let nodeIds = linkedNodes.map { $0.id }

                try await TaskMappingService.shared.updateFormInstanceNodes(
                    formInstanceId: formInstance.id,
                    nodeIds: nodeIds
                )

                AppLogger.log(.info, "Form instance node mappings updated via TaskMappingService", category: .form)

                await MainActor.run {
                    isSaving = false
                    hasChanges = false
                    dismiss()
                }
            } catch {
                AppLogger.log(.error, "Failed to save form instance node changes: \(error)", category: .form)
                await MainActor.run {
                    isSaving = false
                }
            }
        }
    }
}

// MARK: - Node Selector Sheet
struct NodeSelectorForFormInstance: View {
    @Environment(\.dismiss) private var dismiss

    let formInstance: FormInstance
    let availableNodes: [NodeV2]
    let onSelect: (NodeV2) -> Void

    @State private var searchText = ""

    private var filteredNodes: [NodeV2] {
        if searchText.isEmpty {
            return availableNodes
        }
        return availableNodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            (node.location?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField(AppStrings.Tasks.searchNodes, text: $searchText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                .padding()

                if filteredNodes.isEmpty {
                    VStack {
                        Spacer()
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text(searchText.isEmpty ? AppStrings.Tasks.noAvailableNodes : AppStrings.Tasks.noNodesFound)
                            .font(.headline)
                            .foregroundColor(.secondary)
                        if !searchText.isEmpty {
                            Text(AppStrings.Tasks.tryDifferentSearch)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                } else {
                    List(filteredNodes) { node in
                        Button(action: {
                            onSelect(node)
                            dismiss()
                        }) {
                            HStack {
                                Image(systemName: "cube")
                                    .foregroundColor(.blue)
                                VStack(alignment: .leading) {
                                    Text(node.label)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                        .foregroundColor(.primary)
                                    if let location = node.location {
                                        Text(location)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                Spacer()
                                Image(systemName: "plus.circle")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Tasks.selectNode)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
            }
        }
    }
}