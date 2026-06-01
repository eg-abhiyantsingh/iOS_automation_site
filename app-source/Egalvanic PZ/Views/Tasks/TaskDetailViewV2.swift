import SwiftUI
import SwiftData
import PhotosUI
import UIKit

struct TaskDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    let task: UserTask
    let showNodesSection: Bool
    let contextNode: NodeV2?  // The node we came from (if opened from asset detail)
    let hideNavigationBar: Bool
    let onDismiss: (() -> Void)?
    /// When true the view collapses to only the Photos card. Used by the
    /// task-list long-press "Photos" action so the user lands directly
    /// on photo management instead of scrolling past details.
    let photosOnly: Bool

    @State private var isCompleted: Bool
    @State private var draftTitle: String
    @State private var draftDescription: String
    @State private var draftTaskType: String?       // backend code, e.g. "PM"
    @State private var draftProcedureId: UUID?
    @State private var showingDeleteAlert = false
    @State private var isSaving = false
    @State private var saveError: Error?
    @State private var showingDiscardChangesAlert = false
    @State private var showingCompleteConfirmation = false
    @State private var showingTaskTypeSheet = false
    @State private var showingProcedureSheet = false
    @State private var procedureSearchText = ""
    // Query the local Procedure cache so the picker stays available
    // offline. Same pattern as the creation form.
    @Query(filter: #Predicate<Procedure> { !$0.is_deleted }) private var allProcedures: [Procedure]
    
    // Photo management
    @State private var taskPhotos: [Photo] = []
    @State private var stagedPhotoAdditions: [Photo] = []
    @State private var stagedPhotoDeletions: Set<UUID> = []
    // Photo type selection and deletion are handled by EntityFancyPhotoPicker
    @State private var selectedPhoto: Photo? = nil  // Still needed for full-screen viewer
    
    // Form presentation state
    @State private var selectedFormInstance: FormInstance?
    @State private var selectedEGFormInstance: EGFormInstance?
    @State private var linkedEGInstances: [EGFormInstance] = []
    @State private var showingFormSourcePicker = false
    @State private var showingEGFormPicker = false
    @State private var egLinkError: String? = nil
    // Pending soft-delete from long-press → confirmation
    @State private var pendingLegacyDelete: FormInstance? = nil
    @State private var pendingEGDelete: EGFormInstance? = nil

    // Node presentation state
    @State private var selectedNode: NodeV2?

    // Track changes to linked entities
    @State private var linkedNodesHaveChanges = false
    @State private var linkedFormsHaveChanges = false
    @State private var showingNodeLinkingView = false
    @State private var showingFormLinkingView = false
    @State private var selectedNodeIdsForLinking: Set<UUID> = []

    init(task: UserTask, showNodesSection: Bool = true, contextNode: NodeV2? = nil, hideNavigationBar: Bool = false, onDismiss: (() -> Void)? = nil, photosOnly: Bool = false) {
        self.task = task
        self.showNodesSection = showNodesSection
        self.contextNode = contextNode
        self.hideNavigationBar = hideNavigationBar
        self.onDismiss = onDismiss
        self.photosOnly = photosOnly
        _isCompleted = State(initialValue: task.completed)
        _draftTitle = State(initialValue: task.title)
        _draftDescription = State(initialValue: task.task_description)
        _draftTaskType = State(initialValue: task.task_type)
        _draftProcedureId = State(initialValue: task.procedure_id)
    }
    
    private var hasChanges: Bool {
        isCompleted != task.completed ||
        draftTitle != task.title ||
        draftDescription != task.task_description ||
        draftTaskType != task.task_type ||
        draftProcedureId != task.procedure_id ||
        !stagedPhotoAdditions.isEmpty ||
        !stagedPhotoDeletions.isEmpty ||
        linkedNodesHaveChanges ||
        linkedFormsHaveChanges
    }

    private var selectedProcedure: Procedure? {
        guard let id = draftProcedureId else { return nil }
        return allProcedures.first { $0.id == id }
    }

    private var draftTaskTypeDisplay: String {
        TaskType.from(rawValue: draftTaskType)?.displayName
            ?? (draftTaskType?.isEmpty == false ? draftTaskType! : AppStrings.Tasks.selectTaskType)
    }
    
    private var visiblePhotos: [Photo] {
        let existingPhotos = taskPhotos.filter { photo in
            !photo.is_deleted && !stagedPhotoDeletions.contains(photo.id)
        }
        return existingPhotos + stagedPhotoAdditions
    }
    
    private var photosByType: [PhotoType: [Photo]] {
        Dictionary(grouping: visiblePhotos) { photo in
            PhotoType(rawValue: photo.type) ?? .general
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                mainContent
                bottomActionBar
            }
            .navigationTitle(hideNavigationBar ? "" : AppStrings.Tasks.taskDetails)
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(hasChanges || hideNavigationBar)
            .navigationBarHidden(hideNavigationBar)
            .toolbar(hideNavigationBar ? .hidden : .visible, for: .navigationBar)
            .toolbar {
                if !hideNavigationBar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        if hasChanges {
                            Button(AppStrings.Common.cancel) {
                                showingDiscardChangesAlert = true
                            }
                            .foregroundColor(.red)
                        } else {
                            Button(AppStrings.CommonExtra.close) {
                                dismiss()
                            }
                        }
                    }
                }
            }
        }
        .alert(AppStrings.Tasks.deleteTask, isPresented: $showingDeleteAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.delete, role: .destructive) {
                deleteTask()
            }
        } message: {
            Text(AppStrings.Tasks.deleteTaskConfirm)
        }
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { saveError != nil },
            set: { if !$0 { saveError = nil } }
        )) {
            Button(AppStrings.Common.ok) { saveError = nil }
        } message: {
            Text(saveError?.localizedDescription ?? AppStrings.AssetsExtra.unknownError)
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $showingDiscardChangesAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                discardChanges()
            }
        } message: {
            Text(AppStrings.Alerts.discardChangesMessage)
        }
        .alert(AppStrings.Tasks.completeTask, isPresented: $showingCompleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Tasks.complete) {
                saveChanges()
            }
        } message: {
            Text(AppStrings.Tasks.completeTaskMessage)
        }
        // Photo deletion confirmation is handled by EntityFancyPhotoPicker
        .overlay {
            if isSaving {
                savingOverlay
            }
        }
        .fullScreenCover(item: $selectedPhoto) { photo in
            FullImageView(selected: photo, allPhotos: visiblePhotos)
        }
        .fullScreenCover(item: $selectedNode) { node in
            if let sld = task.sld {
                EditNodeDetailViewV3(node: node, sld: sld, showTasksSection: false)
            }
        }
        .sheet(isPresented: $showingNodeLinkingView) {
            let config = TaskLinkedNodesList(
                modelContext: modelContext,
                appState: appState,
                contextNode: contextNode,
                onSelectNode: { _ in },
                onDeleteNode: { _ in },
                onAddNode: { }
            )
            if let linkConfig = config.linkingConfiguration() {
                EntityLinkingView(
                    parent: task,
                    configuration: linkConfig,
                    selectedEntities: $selectedNodeIdsForLinking,
                    onSave: {
                        let availableEntities = linkConfig.fetchAvailable(task)
                        linkConfig.applyLinking(task, selectedNodeIdsForLinking, availableEntities)
                        showingNodeLinkingView = false
                    }
                )
            }
        }
        .sheet(isPresented: $showingFormLinkingView) {
            TaskFormLinkingView(
                task: task,
                contextNode: contextNode,  // Pass the context node to limit available nodes if needed
                onComplete: {
                    linkedFormsHaveChanges = true
                    showingFormLinkingView = false
                }
            )
            .environmentObject(networkState)
        }
        .fullScreenCover(item: $selectedFormInstance) { formInstance in
            NavigationStack {
                FormWebAppContainerView(task: task, formInstance: formInstance, onSubmit: {
                    // Dismiss the webview after successful form submission
                    selectedFormInstance = nil
                })
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.cancel) {
                            selectedFormInstance = nil
                        }
                    }
                }
            }
            .environmentObject(networkState)
        }
        .fullScreenCover(item: $selectedEGFormInstance) { instance in
            // ZP-1723: native SwiftUI renderer for the new EG Forms system.
            // No NavigationStack wrapper here — nesting one inside the
            // cover when the presenter already owns a NavigationStack
            // throws an NSInternalInconsistencyException about top items
            // belonging to a different navigation bar. EGFormView
            // renders its own header when `onCancel` is provided.
            EGFormView(
                instance: instance,
                task: task,
                onCancel: { selectedEGFormInstance = nil }
            )
        }
        // Camera handling is now managed by EntityFancyPhotoPicker
        // Photo capture onChange handlers removed - now handled by EntityFancyPhotoPicker
        .onAppear {
            loadTaskPhotos()
            loadLinkedEGFormInstances()
            debugLogTaskState()
        }
    }
    
    private var mainContent: some View {
        ScrollView {
            VStack(spacing: 0) {
                headerSection
                contentSections
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color(.systemGray6))
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
    }
    
    private var nodeDisplayLabel: String? {
        let linkedNodes = task.linkedNodes.filter { !$0.is_deleted }

        if linkedNodes.isEmpty {
            return nil
        } else if linkedNodes.count == 1 {
            return linkedNodes[0].label
        } else {
            // Show first node + count of others
            return "\(linkedNodes[0].label) \(AppStrings.Tasks.othersCount(linkedNodes.count - 1))"
        }
    }

    private var headerSection: some View {
        TaskHeaderSection(
            title: draftTitle.isEmpty ? AppStrings.Tasks.newTask : draftTitle,
            isCompleted: isCompleted,
            nodeLabel: nodeDisplayLabel,
            networkMode: networkState.mode
        )
    }
    
    private var contentSections: some View {
        VStack(spacing: 20) {
            if photosOnly {
                // Long-press "Photos" mode — surface just the photo
                // management so the user lands where they're going
                // without scrolling past details / nodes / forms.
                photosCard
            } else {
                detailsCard
                if showNodesSection {
                    linkedNodeCard
                    // "Listen for Assets" is now an action row inside
                    // linkedNodeCard alongside "Link Assets".
                }
                formsCard
                photosCard
                resolutionSection
                deleteButton
            }
        }
        .padding()
        .padding(.bottom, hasChanges ? 80 : 20)
    }
    
    private var detailsCard: some View {
        VStack(spacing: 16) {
            TaskSectionHeader(title: AppStrings.Tasks.taskDetails, systemImage: "checklist")

            TaskModernTextField(
                label: AppStrings.Tasks.title,
                text: $draftTitle,
                placeholder: AppStrings.Tasks.enterTaskTitle,
                isRequired: true
            )

            // Description folded into the details card — 3-line text
            // area that expands as the user types. `TaskModernTextEditor`
            // grows past its `minHeight` automatically because the
            // underlying SwiftUI `TextEditor` is unconstrained vertically.
            VStack(alignment: .leading, spacing: 4) {
                Text(AppStrings.Tasks.description_)
                    .font(.caption)
                    .foregroundColor(.secondary)
                TaskModernTextEditor(
                    text: $draftDescription,
                    placeholder: AppStrings.Tasks.describeTask,
                    minHeight: 80
                )
            }

            taskTypeRow
            procedureRow
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        .sheet(isPresented: $showingTaskTypeSheet) {
            taskTypeSheet
        }
        .sheet(isPresented: $showingProcedureSheet) {
            procedureSheet
        }
    }

    private var taskTypeRow: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(AppStrings.Tasks.taskType)
                .font(.caption)
                .foregroundColor(.secondary)
            Button {
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                showingTaskTypeSheet = true
            } label: {
                HStack {
                    Image(systemName: "tag")
                        .foregroundColor(.secondary)
                    Text(draftTaskTypeDisplay)
                        .foregroundColor(draftTaskType?.isEmpty == false ? .primary : .secondary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
            .buttonStyle(.plain)
        }
    }

    /// A single procedure can't span multiple assets, so editing is locked
    /// once 2+ assets are linked — mirrors the web (`linked_nodes.length > 1`).
    private var procedureEditingDisabled: Bool {
        task.linkedNodes.filter { !$0.is_deleted }.count > 1
    }

    private var procedureRow: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(AppStrings.Tasks.procedure)
                .font(.caption)
                .foregroundColor(.secondary)
            Button {
                guard !procedureEditingDisabled else { return }
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                procedureSearchText = ""
                showingProcedureSheet = true
            } label: {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "list.bullet.clipboard")
                        .foregroundColor(.secondary)
                        .padding(.top, 2)
                    Text(selectedProcedure?.name ?? AppStrings.Tasks.noProcedure)
                        .foregroundColor(selectedProcedure != nil ? .primary : .secondary)
                    Spacer()
                    if draftProcedureId != nil && !procedureEditingDisabled {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                            .font(.body)
                            .frame(width: 30, height: 30)
                            .contentShape(Rectangle())
                            .onTapGesture { draftProcedureId = nil }
                    }
                    Image(systemName: "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.top, 4)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .opacity(procedureEditingDisabled ? 0.5 : 1)
            }
            .buttonStyle(.plain)
            .disabled(procedureEditingDisabled)

            if procedureEditingDisabled {
                Text(AppStrings.Tasks.procedureDisabledMultipleAssets)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    /// Filter procedures by the task's first linked node's class so the
    /// editing picker matches the creation-time filter. Falls back to
    /// the General-only bucket when no node is linked.
    private var detailProcedures: [Procedure] {
        let nodeClassId = task.linkedNodes
            .first(where: { !$0.is_deleted })?.node_class?.id
        if let nodeClassId {
            return allProcedures.filter { $0.node_class_id == nil || $0.node_class_id == nodeClassId }
        }
        return allProcedures.filter { $0.node_class_id == nil }
    }

    /// `detailProcedures` narrowed by the sheet's search field (name,
    /// description, or subtype). Mirrors the creation picker's search.
    private var filteredDetailProcedures: [Procedure] {
        let query = procedureSearchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !query.isEmpty else { return detailProcedures }
        return detailProcedures.filter { proc in
            proc.name.lowercased().contains(query) ||
            (proc.procedure_description?.lowercased().contains(query) ?? false) ||
            (proc.node_subtype?.lowercased().contains(query) ?? false)
        }
    }

    private var groupedDetailProcedures: [(group: String, procedures: [Procedure])] {
        let generalLabel = AppStrings.Tasks.procedureGroupGeneral
        let buckets = Dictionary(grouping: filteredDetailProcedures) { proc -> String in
            let raw = proc.node_subtype?.trimmingCharacters(in: .whitespacesAndNewlines)
            return (raw?.isEmpty == false ? raw! : generalLabel)
        }
        return buckets
            .map { (group: $0.key, procedures: $0.value.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }) }
            .sorted { lhs, rhs in
                if lhs.group == generalLabel { return false }
                if rhs.group == generalLabel { return true }
                return lhs.group.localizedCaseInsensitiveCompare(rhs.group) == .orderedAscending
            }
    }

    private var taskTypeSheet: some View {
        NavigationStack {
            List {
                ForEach(TaskType.allCases) { option in
                    Button {
                        draftTaskType = option.rawValue
                        showingTaskTypeSheet = false
                    } label: {
                        HStack {
                            Text(option.displayName).foregroundColor(.primary)
                            Spacer()
                            if TaskType.from(rawValue: draftTaskType) == option {
                                Image(systemName: "checkmark").foregroundColor(.blue)
                            }
                        }
                    }
                }
            }
            .navigationTitle(AppStrings.Tasks.taskType)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) { showingTaskTypeSheet = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private var procedureSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if detailProcedures.isEmpty {
                    Spacer()
                    Text(AppStrings.Tasks.noProceduresAvailable).foregroundColor(.secondary)
                    Spacer()
                } else {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField(AppStrings.Tasks.searchProcedures, text: $procedureSearchText)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)

                    List {
                        Button {
                            draftProcedureId = nil
                            showingProcedureSheet = false
                        } label: {
                            HStack {
                                Text(AppStrings.Tasks.noProcedure).foregroundColor(.primary)
                                Spacer()
                                if draftProcedureId == nil {
                                    Image(systemName: "checkmark").foregroundColor(.blue)
                                }
                            }
                        }
                        ForEach(groupedDetailProcedures, id: \.group) { group in
                            Section {
                                ForEach(group.procedures) { proc in
                                    Button {
                                        draftProcedureId = proc.id
                                        showingProcedureSheet = false
                                    } label: {
                                        HStack {
                                            Text(proc.name).foregroundColor(.primary)
                                            Spacer()
                                            if draftProcedureId == proc.id {
                                                Image(systemName: "checkmark").foregroundColor(.blue)
                                            }
                                        }
                                    }
                                }
                            } header: {
                                Text(group.group).font(.subheadline).fontWeight(.semibold)
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle(AppStrings.Tasks.procedure)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        procedureSearchText = ""
                        showingProcedureSheet = false
                    }
                }
            }
        }
        .presentationDetents([.large])
    }
    
    private var resolutionSection: some View {
        VStack(spacing: 16) {
            TaskSectionHeader(title: AppStrings.Tasks.resolution, systemImage: "checkmark.seal")

            HStack(spacing: 0) {
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        isCompleted = false
                    }
                } label: {
                    HStack {
                        Image(systemName: "circle")
                            .font(.body)
                        Text(AppStrings.Common.pending)
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(!isCompleted ? Color.blue : Color.clear)
                    .foregroundColor(!isCompleted ? .white : .secondary)
                }

                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        isCompleted = true
                    }
                } label: {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.body)
                        Text(AppStrings.Tasks.complete)
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(isCompleted ? Color.green : Color.clear)
                    .foregroundColor(isCompleted ? .white : .secondary)
                }
            }
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
    
    private var linkedNodeCard: some View {
        VStack(spacing: 16) {
            // Header — title only; the Link button moved to the bottom
            // of the card alongside "Listen for Assets" to mirror the
            // Forms section's "Link Forms" affordance.
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "cpu")
                        .foregroundColor(.primary)
                    Text(AppStrings.Tasks.linkedAssets)
                        .font(.headline)
                }
                Spacer()
            }

            // Node rows
            let activeNodes = task.linkedNodes.filter { !$0.is_deleted }.sorted { $0.label < $1.label }
            if activeNodes.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "cpu")
                        .font(.system(size: 40))
                        .foregroundColor(.gray.opacity(0.5))
                    Text(AppStrings.Tasks.noLinkedAssets)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 30)
            } else {
                VStack(spacing: 8) {
                    ForEach(activeNodes, id: \.id) { node in
                        // Wrap as HStack with a separate tap gesture so the
                        // inner completion-circle Button captures taps for
                        // toggling per-node completion, while the rest of
                        // the row opens the node detail.
                        let isNodeCompleted = task.nodeCompletions[node.id.uuidString] ?? false
                        HStack(spacing: 12) {
                            NodeTypeIcon(style: node.node_class?.style, size: 22, color: .secondary)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(node.label)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.primary)
                                    .lineLimit(1)

                                if let className = node.node_class?.name, !className.isEmpty {
                                    Text(className)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                }
                            }
                            // Tapping the label area falls through to the
                            // outer onTapGesture (opens node detail).
                            .contentShape(Rectangle())
                            .onTapGesture {
                                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                                selectedNode = node
                            }

                            Spacer()
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    selectedNode = node
                                }

                            // Tappable completion toggle for this node's
                            // mapping_task_node row. Independent of the
                            // task-level "complete" — toggles only the
                            // per-asset portion of the task.
                            Button {
                                instantUpdateLocal { toggleNodeCompletion(node) }
                            } label: {
                                Image(systemName: isNodeCompleted ? "checkmark.circle.fill" : "circle")
                                    .foregroundColor(isNodeCompleted ? .green : .gray)
                                    .font(.title3)
                                    .contentShape(Rectangle())
                                    .frame(width: 32, height: 32)
                            }
                            .buttonStyle(.plain)

                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(Color.secondary.opacity(0.5))
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    selectedNode = node
                                }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                    }
                }
            }

            // Action row — Link Assets + Listen for Assets in one line,
            // same pill style as the Forms section's Link Forms button.
            HStack(spacing: 8) {
                Button {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    let config = TaskLinkedNodesList(
                        modelContext: modelContext,
                        appState: appState,
                        contextNode: contextNode,
                        onSelectNode: { _ in },
                        onDeleteNode: { _ in },
                        onAddNode: { }
                    )
                    if let linkConfig = config.linkingConfiguration() {
                        selectedNodeIdsForLinking = linkConfig.currentlyLinked(task)
                    }
                    showingNodeLinkingView = true
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "link")
                        Text(AppStrings.Tasks.linkAssets).fontWeight(.semibold)
                        Spacer()
                    }
                    .padding(.vertical, 10)
                    .padding(.horizontal, 12)
                    .background(Color(.secondarySystemBackground))
                    .foregroundStyle(isCompleted ? Color.secondary : Color.accentColor)
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)
                .disabled(isCompleted)

                let listening = networkState.isListening(task.id)
                Button {
                    networkState.toggleListening(task.id)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: listening ? "ear.fill" : "ear")
                        Text(listening ? AppStrings.Tasks.listening : AppStrings.Sessions.listenForAssets)
                            .fontWeight(.semibold)
                            .lineLimit(1)
                        Spacer()
                    }
                    .padding(.vertical, 10)
                    .padding(.horizontal, 12)
                    .background(listening ? Color.orange.opacity(0.15) : Color(.secondarySystemBackground))
                    .foregroundStyle(listening ? Color.orange : Color.accentColor)
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
    
    private var photosCard: some View {
        // Using TaskPhotoPickerSection configuration wrapper
        TaskPhotoPickerSection(
            task: task,
            displayedPhotos: Binding(
                get: { visiblePhotos },
                set: { _ in }
            ),
            isSaving: .constant(false),
            onPhotoAdded: { photo in
                stagedPhotoAdditions.append(photo)
            },
            onPhotoDeleted: { photo in
                deletePhoto(photo)
            }
        )
    }

    // Unified "Forms" section. Combines legacy `FormInstance` and the
    // newer `EGFormInstance` rows in one list. Tapping a row opens the
    // appropriate viewer (FormApp WebView for legacy, EGFormView for
    // EG). The "Link Forms" button at the bottom presents an action
    // sheet to choose which system to link from — EG Forms entry is
    // present but no-op until task #12 builds the EG picker; tapping
    // it does nothing for now.
    private var formsCard: some View {
        let legacy = task.linkedFormInstances.filter { !$0.is_deleted }
        let eg = linkedEGInstances.filter { !$0.is_deleted }

        return VStack(alignment: .leading, spacing: 12) {
            TaskSectionHeader(title: AppStrings.Tasks.forms, systemImage: "doc.text.fill")

            if legacy.isEmpty && eg.isEmpty {
                Text("No forms linked to this task.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 4)
            } else {
                VStack(spacing: 8) {
                    ForEach(legacy) { instance in
                        formRow(
                            title: instance.formMaster?.title ?? "Untitled form",
                            statusLabel: instance.statusDescription,
                            typeLabel: "LEGACY"
                        ) {
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            selectedFormInstance = instance
                        }
                        .contextMenu {
                            Button(role: .destructive) {
                                pendingLegacyDelete = instance
                            } label: {
                                Label("Remove from task", systemImage: "trash")
                            }
                        }
                    }
                    ForEach(eg) { instance in
                        formRow(
                            title: instance.egForm?.title ?? "Untitled form",
                            statusLabel: instance.statusDescription,
                            typeLabel: instance.egForm?.displayTypeLabel ?? "EG"
                        ) {
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            selectedEGFormInstance = instance
                        }
                        .contextMenu {
                            Button(role: .destructive) {
                                pendingEGDelete = instance
                            } label: {
                                Label("Remove from task", systemImage: "trash")
                            }
                        }
                    }
                }
            }

            Button {
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                showingFormSourcePicker = true
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus.circle.fill")
                    Text(AppStrings.Forms.linkForms).fontWeight(.semibold)
                    Spacer()
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(Color(.secondarySystemBackground))
                .foregroundStyle(.tint)
                .cornerRadius(8)
            }
            .buttonStyle(.plain)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        .sheet(isPresented: $showingFormSourcePicker) {
            FormSourcePickerSheet(
                onPickLegacy: {
                    showingFormSourcePicker = false
                    showingFormLinkingView = true
                },
                onPickEG: {
                    showingFormSourcePicker = false
                    showingEGFormPicker = true
                },
                onCancel: { showingFormSourcePicker = false }
            )
            .presentationDetents([.height(240), .medium])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $showingEGFormPicker) {
            NavigationStack {
                EGFormPickerSheet(
                    onPick: { form in
                        showingEGFormPicker = false
                        createAndLinkEGInstance(form: form)
                    },
                    onCancel: { showingEGFormPicker = false }
                )
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .alert("Couldn't Link EG Form", isPresented: Binding(
            get: { egLinkError != nil },
            set: { if !$0 { egLinkError = nil } }
        )) {
            Button("OK", role: .cancel) { egLinkError = nil }
        } message: {
            Text(egLinkError ?? "")
        }
        .alert("Remove this form?", isPresented: Binding(
            get: { pendingEGDelete != nil },
            set: { if !$0 { pendingEGDelete = nil } }
        )) {
            Button("Cancel", role: .cancel) { pendingEGDelete = nil }
            Button("Remove", role: .destructive) {
                if let inst = pendingEGDelete {
                    softDeleteEGForm(inst)
                }
                pendingEGDelete = nil
            }
        } message: {
            Text("This form won't appear on the task anymore. Submissions and history are kept in case you need them later.")
        }
        .alert("Remove this form?", isPresented: Binding(
            get: { pendingLegacyDelete != nil },
            set: { if !$0 { pendingLegacyDelete = nil } }
        )) {
            Button("Cancel", role: .cancel) { pendingLegacyDelete = nil }
            Button("Remove", role: .destructive) {
                if let inst = pendingLegacyDelete {
                    softDeleteLegacyForm(inst)
                }
                pendingLegacyDelete = nil
            }
        } message: {
            Text("This form won't appear on the task anymore. Submissions and history are kept in case you need them later.")
        }
    }

    // MARK: - Soft delete handlers

    private func softDeleteEGForm(_ inst: EGFormInstance) {
        // Unlink locally via the canonical task side so the UI updates
        // immediately; the service handles the model context save +
        // server PUT (or offline queue).
        task.linkedEGFormInstances.removeAll { $0.id == inst.id }
        EGFormInstanceService.softDeleteEGFormInstance(
            inst,
            modelContext: modelContext,
            onCompletion: { _, _ in loadLinkedEGFormInstances() }
        )
    }

    private func softDeleteLegacyForm(_ inst: FormInstance) {
        // Mirror the legacy flow used by TaskLinkedFormInstancesList:
        // remove from the relationship, mark is_deleted, sync via
        // TaskMappingService.
        task.linkedFormInstances.removeAll { $0.id == inst.id }
        inst.is_deleted = true
        try? modelContext.save()
        Task {
            do {
                try await TaskMappingService.shared.removeTaskFormInstances(
                    taskId: task.id,
                    formInstanceIds: [inst.id]
                )
                linkedFormsHaveChanges = true
            } catch {
                AppLogger.log(.error, "Failed to soft-delete legacy form: \(error)", category: .task)
            }
        }
    }

    /// Create an EGFormInstance for the chosen template and link it
    /// to this task. The service handles online direct-sync vs. offline
    /// queue automatically. We re-fetch the linked-instances list once
    /// the local insert completes so the Forms section reflects the
    /// new row immediately.
    private func createAndLinkEGInstance(form: EGForm) {
        AppLogger.log(.info, "[EGForm DEBUG] createAndLinkEGInstance form=\(form.title ?? "?") task=\(task.id.uuidString.prefix(8))", category: .form)
        // Don't pass `linkedTasks: [task]` to the service. Setting it
        // on the instance side races with SwiftData's auto-synthesized
        // single-valued reverse and steals the task away from other
        // instances. Instead, after the instance is created, attach
        // it via the task side (`task.linkedEGFormInstances`) — same
        // pattern legacy uses for `task.linkedFormInstances`.
        EGFormInstanceService.createEGFormInstance(
            egFormId: form.id,
            submission: nil,
            submitted: false,
            taskId: task.id,
            nodeId: nil,
            linkedTasks: [],
            linkedNodes: [],
            egForm: form,
            modelContext: modelContext,
            onCompletion: { ok, inst, message in
                if let inst = inst {
                    if !task.linkedEGFormInstances.contains(where: { $0.id == inst.id }) {
                        task.linkedEGFormInstances.append(inst)
                    }
                    try? modelContext.save()
                    AppLogger.log(.info, "[EGForm DEBUG] createEGFormInstance returned ok=\(ok) instId=\(inst.id.uuidString.prefix(8)) message=\(message ?? "nil") taskLinkedCount=\(task.linkedEGFormInstances.count)", category: .form)
                } else {
                    AppLogger.log(.info, "[EGForm DEBUG] createEGFormInstance returned ok=\(ok) inst=nil message=\(message ?? "nil")", category: .form)
                }
                if ok {
                    loadLinkedEGFormInstances()
                } else if let message = message {
                    egLinkError = message
                }
            }
        )
    }

    @ViewBuilder
    private func formRow(
        title: String,
        statusLabel: String,
        typeLabel: String,
        onTap: @escaping () -> Void
    ) -> some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Image(systemName: "doc.text.fill")
                    .foregroundStyle(.cyan)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline).fontWeight(.medium)
                        .foregroundStyle(.primary)
                    HStack(spacing: 8) {
                        Text(statusLabel)
                            .font(.caption2)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(statusColor(statusLabel).opacity(0.15))
                            .foregroundStyle(statusColor(statusLabel))
                            .clipShape(Capsule())
                        Text(typeLabel)
                            .font(.caption2).foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(12)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Per-node completion toggle

    /// Toggle the completion state of a single (task ↔ node) mapping.
    /// Saves locally, then either fires the bulk-completion API (which
    /// the backend uses for per-node completion) or queues it offline.
    private func toggleNodeCompletion(_ node: NodeV2) {
        let key = node.id.uuidString
        let newValue = !(task.nodeCompletions[key] ?? false)
        task.nodeCompletions[key] = newValue
        do { try modelContext.save() } catch {
            AppLogger.log(.error, "Failed to save node completion: \(error)", category: .task)
        }
        Task {
            if networkState.mode == .online {
                do {
                    try await TaskMappingService.shared.bulkUpdateNodeCompletions(
                        taskId: task.id,
                        completions: [(nodeId: node.id, isCompleted: newValue)]
                    )
                } catch {
                    AppLogger.log(.error, "Failed to sync node completion: \(error)", category: .task)
                    // No dedicated queue target for per-node completion;
                    // fall back to queuing a task update so the change
                    // doesn't get lost.
                    let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                    networkState.enqueue(op)
                }
            } else {
                let op = SyncOp(target: .userTask, operation: .update, userTask: task)
                networkState.enqueue(op)
            }
        }
    }

    /// Wrap a state mutation so SwiftUI doesn't insert an implicit
    /// animation on the icon swap when toggling the completion circle.
    /// Mirrors `instantUpdate` from the EG form input blocks.
    private func instantUpdateLocal(_ block: () -> Void) {
        var tx = Transaction()
        tx.disablesAnimations = true
        withTransaction(tx, block)
    }

    private func statusColor(_ status: String) -> Color {
        switch status {
        case "Submitted":   return .green
        case "In Progress": return .orange
        default:            return .secondary
        }
    }

    /// Refresh the list of EG form instances linked to this task. Called
    /// in onAppear because we don't have a back-reference on UserTask —
    /// we walk EGFormInstance.linkedTasks instead.
    private func loadLinkedEGFormInstances() {
        let taskId = task.id
        // Canonical side is `task.linkedEGFormInstances` — mirrors the
        // legacy `task.linkedFormInstances` pattern. The instance-side
        // array (`inst.linkedTasks`) can be stale because SwiftData's
        // auto-synthesized single-valued reverse moves between
        // instances when each one is written from the instance side.
        linkedEGInstances = task.linkedEGFormInstances
            .filter { !$0.is_deleted }
            .sorted { $0.created_at > $1.created_at }
        let all = (try? modelContext.fetch(FetchDescriptor<EGFormInstance>())) ?? []

        // ZP-1723 diagnostic dump — verify whether (a) the local
        // store actually has every instance we expect, (b) each
        // instance's `linkedTasks` array contains this task, and
        // (c) the inverse `task.linkedEGFormInstances` matches.
        // If the counts disagree, the relationship sides are out of
        // sync — surface that here so the next step is informed.
        let viaInstance = all.filter { $0.linkedTasks.contains(where: { $0.id == taskId }) }
        let viaTask = task.linkedEGFormInstances.filter { !$0.is_deleted }
        AppLogger.log(.info, """
        [EGForm DEBUG] TaskDetailView load
          task=\(task.title) id=\(taskId.uuidString.prefix(8))
          totalEGInstancesInStore=\(all.count)
          matchingViaInstance.linkedTasks=\(viaInstance.count)
          matchingViaTask.linkedEGFormInstances=\(viaTask.count)
          renderingCount=\(linkedEGInstances.count)
        """, category: .form)

        for inst in all {
            let containsTask = inst.linkedTasks.contains(where: { $0.id == taskId })
            let taskIds = inst.linkedTasks.map { $0.id.uuidString.prefix(8) }.joined(separator: ",")
            AppLogger.log(.info, """
            [EGForm DEBUG]   inst=\(inst.id.uuidString.prefix(8)) \
            form=\(inst.egForm?.title ?? "?") \
            submitted=\(inst.submitted) \
            deleted=\(inst.is_deleted) \
            containsTask=\(containsTask) \
            linkedTaskIds=[\(taskIds)]
            """, category: .form)
        }
    }

    // formCard removed — superseded by `formsCard` which renders both
    // legacy and EG form instances in one unified section.
    
    private var deleteButton: some View {
        Button(role: .destructive) {
            showingDeleteAlert = true
        } label: {
            HStack {
                Image(systemName: "trash")
                Text(AppStrings.Tasks.deleteTask)
            }
            .font(.subheadline)
            .fontWeight(.medium)
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color.red.opacity(0.1))
            .cornerRadius(10)
        }
        .padding(.horizontal)
    }
    
    @ViewBuilder
    private var bottomActionBar: some View {
        if hasChanges {
            VStack {
                Spacer()
                
                VStack(spacing: 0) {
                    Rectangle()
                        .fill(Color(.systemBackground))
                        .frame(height: 0)
                        .shadow(color: .black.opacity(0.1), radius: 10, y: -5)
                    
                    saveButton
                }
            }
            .ignoresSafeArea()
        }
    }
    
    private var saveButton: some View {
        Button(action: attemptSave) {
            if isSaving {
                HStack {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(0.8)
                    Text(AppStrings.AssetsExtra.saving)
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(Color.blue.opacity(0.6))
                .cornerRadius(12)
            } else {
                Text(AppStrings.AssetsExtra.saveChanges)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
            }
        }
        .disabled(isSaving || draftTitle.isEmpty)
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 18)
        .background(Color(.systemBackground))
    }
    
    private var backButton: some View {
        Button(action: {
            if hasChanges {
                showingDiscardChangesAlert = true
            } else {
                dismiss()
            }
        }) {
            HStack(spacing: 4) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .medium))
                Text(AppStrings.Common.back)
            }
        }
    }
    
    private var savingOverlay: some View {
        Color.black.opacity(0.3)
            .ignoresSafeArea()
            .overlay {
                ProgressView(AppStrings.AssetsExtra.saving)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
    }
    
        
    // MARK: - Helper Methods
    
    private func discardChanges() {
        for photo in stagedPhotoAdditions {
            if let path = photo.local_filepath {
                let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let fileURL = documentsURL.appendingPathComponent(path)
                try? FileManager.default.removeItem(at: fileURL)
            }
        }
        dismiss()
    }
    
    private func loadTaskPhotos() {
        taskPhotos = task.photos.filter { !$0.is_deleted }
    }

    private func debugLogTaskState() {
        let allNodes = task.linkedNodes + (task.node != nil ? [task.node!] : [])
        let uniqueNodes = Array(Set(allNodes))
        let allForms = task.linkedForms + (task.form != nil ? [task.form!] : [])
        let uniqueForms = Array(Set(allForms))

        AppLogger.log(.debug, "[TaskDetailView] Task: \(task.title) (ID: \(task.id)), completed: \(task.completed), legacyNode: \(task.node?.label ?? "nil"), legacyForm: \(task.form?.title ?? "nil"), linkedNodes: \(task.linkedNodes.count), linkedForms: \(task.linkedForms.count), uniqueNodes: \(uniqueNodes.count), uniqueForms: \(uniqueForms.count)", category: .task)
    }

    private func deletePhoto(_ photo: Photo) {
        if let index = stagedPhotoAdditions.firstIndex(where: { $0.id == photo.id }) {
            stagedPhotoAdditions.remove(at: index)
        } else {
            stagedPhotoDeletions.insert(photo.id)
        }
    }
    
    private func attemptSave() {
        // Check if we're marking task as complete and there are incomplete linked nodes
        let isMarkingComplete = isCompleted && !task.completed
        let activeNodes = task.linkedNodes.filter { !$0.is_deleted }
        let hasIncompleteNodes = activeNodes.contains { node in
            !(task.nodeCompletions[node.id.uuidString] ?? false)
        }

        // Show confirmation if marking complete with incomplete linked nodes
        if isMarkingComplete && hasIncompleteNodes {
            showingCompleteConfirmation = true
        } else {
            saveChanges()
        }
    }

    private func saveChanges() {
        guard !isSaving else { return }
        isSaving = true

        // Create update data structure
        let updateData = TaskUpdateData(
            title: draftTitle,
            description: draftDescription,
            isCompleted: isCompleted,
            wasCompleted: task.completed,
            stagedPhotoAdditions: stagedPhotoAdditions,
            stagedPhotoDeletions: stagedPhotoDeletions,
            originalPhotos: taskPhotos,
            taskType: draftTaskType,
            procedureId: draftProcedureId,
            procedureIdSet: draftProcedureId != task.procedure_id
        )
        
        // Use TaskService for comprehensive update
        Task {
            do {
                try await TaskService.updateTaskComprehensive(
                    task,
                    with: updateData,
                    networkState: networkState,
                    modelContext: modelContext
                )
                
                await MainActor.run {
                    AppLogger.log(.info, "Save completed successfully - dismissing view", category: .task)
                    isSaving = false

                    // Call custom dismiss if provided (for fullScreenCover), otherwise use environment dismiss
                    if let onDismiss = onDismiss {
                        onDismiss()
                    } else {
                        dismiss()
                    }
                }
            } catch {
                AppLogger.log(.error, "Save failed: \(error) - \(error.localizedDescription)", category: .task)
                
                await MainActor.run {
                    isSaving = false
                    saveError = error
                }
            }
        }
    }
    
    // Photo operations are now handled by TaskService.updateTaskComprehensive
    // which uses PhotoService internally
        
    private func deleteTask() {
        TaskService.deleteTask(
            task,
            modelContext: modelContext
        ) { success, message in
            if success {
                dismiss()
                if let errorMessage = message {
                    // Task deleted locally but will sync later
                    AppLogger.log(.info, errorMessage, category: .task)
                }
            } else {
                saveError = NSError(
                    domain: "TaskService",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: message ?? "Failed to delete task"]
                )
            }
        }
    }
}


