import SwiftUI
import SwiftData
import UIKit

struct EntityCreationView<Config: EntityCreationConfiguration, AdditionalContent: View>: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @StateObject private var viewModel = EntityCreationViewModel()
    @StateObject private var storage = StandardFieldValueStorage()

    let configuration: Config
    let additionalContent: (StandardFieldValueStorage) -> AdditionalContent

    @FocusState private var isTextFieldFocused: Bool

    init(configuration: Config, @ViewBuilder additionalContent: @escaping (StandardFieldValueStorage) -> AdditionalContent) {
        self.configuration = configuration
        self.additionalContent = additionalContent
    }

    var body: some View {
        NavigationView {
            Form {
                // Context view if provided
                if let contextView = configuration.contextView() {
                    Section {
                        contextView
                    }
                }

                // Fields grouped by sections
                if let sections = configuration.sections() {
                    ForEach(sections, id: \.self) { section in
                        Section {
                            fieldsForSection(section)
                        } header: {
                            Text(section)
                        }
                    }
                } else {
                    // All fields in one section
                    fieldsForSection(nil)
                }

                // Additional content (e.g., core attributes for issues)
                additionalContent(storage)
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(configuration.navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        configuration.onCancel()
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(configuration.createButtonTitle) {
                        if viewModel.handleCreate(
                            configuration: configuration,
                            storage: storage,
                            modelContext: modelContext
                        ) {
                            dismiss()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(!configuration.canSave(storage: storage) || viewModel.isCreating)
                }

                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button(AppStrings.Common.done) {
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    }
                }
            }
            .alert(AppStrings.Forms.validationError, isPresented: $viewModel.showingValidationError) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(viewModel.validationErrorMessage)
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
    
    @ViewBuilder
    private func fieldsForSection(_ section: String?) -> some View {
        let fields = configuration.fields().filter { field in
            field.section == section
        }
        
        ForEach(fields, id: \.id) { field in
            fieldView(for: field)
        }
    }
    
    @ViewBuilder
    private func fieldView(for field: EntityFieldConfiguration) -> some View {
        switch field.type {
        case .text(let placeholder):
            textFieldView(field: field, placeholder: placeholder)
            
        case .textArea(let placeholder, let minHeight):
            textAreaView(field: field, placeholder: placeholder, minHeight: minHeight)
            
        case .date(let includeTime):
            datePickerView(field: field, includeTime: includeTime)
            
        case .picker(let options):
            pickerView(field: field, options: options)
            
        case .nodeV2Picker:
            NodeV2PickerField(field: field, storage: storage)

        case .nodeV2MultiPicker:
            NodeV2MultiPickerField(field: field, storage: storage)

        case .issueClassPicker:
            IssueClassPickerField(field: field, storage: storage)

        case .userTaskFormPicker:
            UserTaskFormPickerField(field: field, storage: storage)

        case .userTaskFormMultiPicker:
            UserTaskFormMultiPickerField(field: field, storage: storage)
            
        case .edgeV2Picker:
            EdgeV2PickerField(field: field, storage: storage)
            
        case .sldV2Picker:
            SLDV2PickerField(field: field, storage: storage)
            
        case .userTaskPicker:
            UserTaskPickerField(field: field, storage: storage)
            
        case .issuePicker:
            IssuePickerField(field: field, storage: storage)
            
        case .irPhotoPicker:
            IRPhotoPickerField(field: field, storage: storage)
            
        case .irSessionPicker:
            IRSessionPickerField(field: field, storage: storage)
            
        case .toggle:
            toggleView(field: field)
            
        case .number(let formatter):
            numberFieldView(field: field, formatter: formatter)

        case .taskTypePicker:
            TaskTypePickerField(field: field, storage: storage)

        case .procedurePicker:
            ProcedurePickerField(field: field, storage: storage)

        case .custom(let view):
            view
        }
    }
    
    // MARK: - Field Views
    
    private func textFieldView(field: EntityFieldConfiguration, placeholder: String?) -> some View {
        HStack {
            if let icon = field.icon {
                Label(field.label, systemImage: icon)
            } else {
                Text(field.label)
            }
            
            TextField(
                placeholder ?? field.label,
                text: Binding(
                    get: { storage.getValue(for: field.id) as? String ?? "" },
                    set: { storage.setValue($0, for: field.id) }
                )
            )
            .focused($isTextFieldFocused, equals: viewModel.focusedField == field.id)
            .multilineTextAlignment(.trailing)
        }
    }
    
    private func textAreaView(field: EntityFieldConfiguration, placeholder: String?, minHeight: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                if let icon = field.icon {
                    Label(field.label, systemImage: icon)
                } else {
                    Text(field.label)
                }
                
                if field.isRequired {
                    Text("*")
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
            .font(.caption)
            .foregroundColor(.secondary)
            
            TextEditor(
                text: Binding(
                    get: { storage.getValue(for: field.id) as? String ?? "" },
                    set: { storage.setValue($0, for: field.id) }
                )
            )
            .frame(minHeight: minHeight)
            .overlay(
                Group {
                    if let text = storage.getValue(for: field.id) as? String, text.isEmpty {
                        Text(placeholder ?? "Enter \(field.label.lowercased())...")
                            .foregroundColor(.secondary.opacity(0.5))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 8)
                            .allowsHitTesting(false)
                    }
                },
                alignment: .topLeading
            )
            
            if let helpText = field.helpText {
                Text(helpText)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }
    
    private func datePickerView(field: EntityFieldConfiguration, includeTime: Bool) -> some View {
        DatePicker(
            field.label,
            selection: Binding(
                get: { storage.getValue(for: field.id) as? Date ?? Date() },
                set: { storage.setValue($0, for: field.id) }
            ),
            displayedComponents: includeTime ? [.date, .hourAndMinute] : [.date]
        )
    }
    
    private func pickerView(field: EntityFieldConfiguration, options: [PickerOption]) -> some View {
        PickerSheetField(
            field: field,
            options: options,
            storage: storage
        )
    }
    
    
    private func toggleView(field: EntityFieldConfiguration) -> some View {
        Toggle(
            field.label,
            isOn: Binding(
                get: { storage.getValue(for: field.id) as? Bool ?? false },
                set: { storage.setValue($0, for: field.id) }
            )
        )
    }
    
    private func numberFieldView(field: EntityFieldConfiguration, formatter: NumberFormatter?) -> some View {
        HStack {
            Text(field.label)
            
            TextField(
                field.label,
                value: Binding(
                    get: { storage.getValue(for: field.id) as? Double ?? 0 },
                    set: { storage.setValue($0, for: field.id) }
                ),
                formatter: formatter ?? NumberFormatter()
            )
            .keyboardType(.decimalPad)
            .multilineTextAlignment(.trailing)
        }
    }
    
}

// MARK: - Sheet-based Picker Field

private struct PickerSheetField: View {
    let field: EntityFieldConfiguration
    let options: [PickerOption]
    @ObservedObject var storage: StandardFieldValueStorage

    @State private var showSheet = false

    private var currentValue: String {
        storage.getValue(for: field.id) as? String ?? ""
    }

    private var displayText: String {
        if currentValue.isEmpty { return field.label }
        return options.first(where: { $0.id == currentValue })?.displayName ?? currentValue
    }

    private var displayColor: Color? {
        options.first(where: { $0.id == currentValue })?.color
    }

    var body: some View {
        HStack {
            if let icon = field.icon {
                Image(systemName: icon)
                    .foregroundColor(displayColor ?? .secondary)
                    .frame(width: 20)
            }

            Text(displayText)
                .foregroundColor(currentValue.isEmpty ? .secondary : (displayColor ?? .primary))

            Spacer()

            if !currentValue.isEmpty {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
                    .font(.body)
                    .frame(width: 30, height: 30)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        storage.setValue("", for: field.id)
                    }
            }

            Image(systemName: "chevron.down")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
        .contentShape(Rectangle())
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            showSheet = true
        }
        .sheet(isPresented: $showSheet) {
            NavigationStack {
                List {
                    ForEach(options) { option in
                        Button {
                            storage.setValue(option.id, for: field.id)
                            showSheet = false
                        } label: {
                            HStack {
                                if let icon = option.icon {
                                    Image(systemName: icon)
                                        .foregroundColor(option.color)
                                        .frame(width: 20)
                                }
                                Text(option.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if currentValue == option.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                .navigationTitle(field.label)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(AppStrings.Common.cancel) {
                            showSheet = false
                        }
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }
}

// MARK: - Model-Specific Picker Fields

// MARK: - Node Selector Row (extracted for SwiftUI rendering performance)
private struct NodeSelectorRow: View {
    let node: NodeV2
    let isSelected: Bool

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(node.label)
                    .font(.body)
                    .foregroundColor(.primary)
                if let location = node.location {
                    Text(location)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.blue)
            }
        }
    }
}

private struct MultiPickerNodeRow: View {
    let node: NodeV2
    let isSelected: Bool

    var body: some View {
        HStack {
            Image(systemName: "cube")
                .foregroundColor(.gray)
            VStack(alignment: .leading) {
                Text(node.label)
                if let location = node.location {
                    Text(location)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.blue)
            }
        }
    }
}

// MARK: - Single Node Selector View
struct NodeV2SingleSelectorView: View {
    @Binding var selectedNode: NodeV2?
    let nodes: [NodeV2]
    let onAddAsset: () -> Void

    @State private var searchText = ""
    @State private var sortedNodes: [NodeV2] = []
    @State private var displayedNodes: [NodeV2] = []
    @State private var showingQRScanner = false
    @State private var scannedQRCode = ""
    @State private var showingQRError = false
    @State private var qrErrorMessage = ""
    @State private var showDuplicateQRAlert = false
    @State private var duplicateNodes: [NodeV2] = []
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                List {
                    ForEach(displayedNodes) { node in
                        NodeSelectorRow(node: node, isSelected: selectedNode?.id == node.id)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedNode = node
                                dismiss()
                            }
                    }
                }
                .searchable(text: $searchText, prompt: AppStrings.CommonExtra.searchAssets)

                // Floating Action Buttons (QR Scanner + Plus)
                VStack {
                    Spacer()
                    HStack {
                        Spacer()

                        // QR Scanner Button
                        Button(action: {
                            showingQRScanner = true
                        }) {
                            Image(systemName: "qrcode.viewfinder")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.green)
                                .clipShape(Circle())
                                .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                        }
                        .padding(.trailing, 8)

                        // Add Asset Button
                        Button(action: {
                            dismiss()
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                                onAddAsset()
                            }
                        }) {
                            Image(systemName: "plus")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(width: 56, height: 56)
                                .background(Color.blue)
                                .clipShape(Circle())
                                .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                        }
                        .padding(.trailing, 20)
                    }
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle(AppStrings.CommonExtra.selectAsset)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
            }
            .fullScreenCover(isPresented: $showingQRScanner) {
                QRCodeScannerView(
                    scannedCode: $scannedQRCode,
                    onScanComplete: { code in
                        handleQRScan(code)
                    }
                )
            }
            .alert(AppStrings.Forms.qrCodeError, isPresented: $showingQRError) {
                Button(AppStrings.Common.ok, role: .cancel) { }
            } message: {
                Text(qrErrorMessage)
            }
            .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
                ForEach(duplicateNodes.prefix(5), id: \.id) { node in
                    Button(node.label) {
                        selectedNode = node
                        dismiss()
                    }
                }
                if duplicateNodes.count > 5 {
                    Button(AppStrings.Site.showFirstFiveOnly) {
                        duplicateNodes = []
                    }
                }
                Button(AppStrings.Common.cancel, role: .cancel) {
                    duplicateNodes = []
                }
            } message: {
                Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateNodes.count))
            }
            .onAppear {
                sortedNodes = nodes.sorted { $0.label.localizedCaseInsensitiveCompare($1.label) == .orderedAscending }
                displayedNodes = sortedNodes
            }
            .task(id: searchText) {
                if searchText.isEmpty {
                    displayedNodes = sortedNodes
                    return
                }
                try? await Task.sleep(for: .milliseconds(200))
                guard !Task.isCancelled else { return }
                let query = searchText.lowercased()
                displayedNodes = sortedNodes.filter { node in
                    node.label.lowercased().contains(query) ||
                    (node.location?.lowercased().contains(query) ?? false)
                }
            }
        }
    }

    private func handleQRScan(_ code: String) {
        let matchingNodes = nodes.filter { $0.qr_code?.lowercased() == code.lowercased() }

        if matchingNodes.count == 1 {
            selectedNode = matchingNodes[0]
            dismiss()
        } else if matchingNodes.count > 1 {
            duplicateNodes = matchingNodes
            showDuplicateQRAlert = true
        } else {
            qrErrorMessage = "No asset found with QR code: \(code)"
            showingQRError = true
        }
    }
}

struct NodeV2PickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var allNodes: [NodeV2]
    @EnvironmentObject var appState: AppStateManager
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    @State private var showingNodeSelector = false
    @State private var showingAddAsset = false

    private var selectedNode: NodeV2? {
        storage.getValue(for: field.id) as? NodeV2
    }

    private var currentSLD: SLDV2? {
        // Get the current SLD from appState
        let sldId = appState.activeSLDId
        return allNodes.first?.sld ?? allNodes.first(where: { $0.sld?.id == sldId })?.sld
    }

    private var availableNodes: [NodeV2] {
        if let sld = currentSLD {
            return allNodes.filter { $0.sld?.id == sld.id }
        }
        return allNodes
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(field.label)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)

            Button(action: {
                showingNodeSelector = true
            }) {
                HStack {
                    if let node = selectedNode {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(node.label)
                                .font(.body)
                                .foregroundColor(.primary)
                            if let location = node.location {
                                Text(location)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    } else {
                        Text(AppStrings.CommonExtra.selectAsset)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color(UIColor.systemGray6))
                .cornerRadius(10)
            }

            if field.isRequired && selectedNode == nil {
                Text(AppStrings.Forms.assetIsRequired)
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
        .sheet(isPresented: $showingNodeSelector) {
            NodeV2SingleSelectorView(
                selectedNode: Binding(
                    get: { selectedNode },
                    set: { node in
                        storage.setValue(node, for: field.id)
                    }
                ),
                nodes: availableNodes,
                onAddAsset: {
                    showingAddAsset = true
                }
            )
        }
        .fullScreenCover(isPresented: $showingAddAsset) {
            if let sld = currentSLD {
                AddAssetViewV2(
                    availableLocations: Array(Set(availableNodes.compactMap(\.location))).sorted(),
                    availableNodeClasses: {
                        let query = FetchDescriptor<NodeClass>(
                            predicate: #Predicate { nodeClass in
                                !nodeClass.is_deleted
                            }
                        )
                        return (try? modelContext.fetch(query)) ?? []
                    }(),
                    sld: sld,
                    onSave: { node, photos, irPhotos in
                        // Add node to diagram's nodes array immediately
                        if !sld.nodes.contains(where: { $0.id == node.id }) {
                            sld.nodes.append(node)
                        }

                        // Insert into model context
                        modelContext.insert(node)
                        photos.forEach { modelContext.insert($0) }
                        irPhotos.forEach { modelContext.insert($0) }

                        // Save context
                        do {
                            try modelContext.save()
                        } catch {
                            AppLogger.log(.error, "Failed to save node locally: \(error)", category: .ui)
                        }

                        // Set as selected
                        storage.setValue(node, for: field.id)

                        // Close the add asset view
                        showingAddAsset = false

                        // Handle full service operations in background
                        Task {
                            await NodeService.createNewNodeWithPhotosAndIR(
                                node: node,
                                photos: photos,
                                irPhotos: irPhotos,
                                networkState: networkState,
                                modelContext: modelContext
                            )
                        }
                    },
                    onCancel: {
                        showingAddAsset = false
                    }
                )
                .environmentObject(appState)
                .environmentObject(networkState)
            } else {
                Text(AppStrings.Tasks.unableToLoadSLD)
                    .onAppear {
                        showingAddAsset = false
                    }
            }
        }
    }
}

struct IssueClassPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<IssueClass> { !$0.is_deleted }) private var issueClasses: [IssueClass]

    @State private var showSheet = false

    private var selectedClass: IssueClass? {
        storage.getValue(for: field.id) as? IssueClass
    }

    private var sortedClasses: [IssueClass] {
        issueClasses.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        HStack {
            if let icon = field.icon {
                Image(systemName: icon)
                    .foregroundColor(.secondary)
                    .frame(width: 20)
            }

            Text(selectedClass?.displayName ?? field.label)
                .foregroundColor(selectedClass != nil ? .primary : .secondary)

            Spacer()

            if selectedClass != nil {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
                    .font(.body)
                    .frame(width: 30, height: 30)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectIssueClass(nil)
                    }
            }

            Image(systemName: "chevron.down")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
        .contentShape(Rectangle())
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            showSheet = true
        }
        .sheet(isPresented: $showSheet) {
            NavigationStack {
                List {
                    ForEach(sortedClasses) { issueClass in
                        Button {
                            selectIssueClass(issueClass)
                            showSheet = false
                        } label: {
                            HStack {
                                Text(issueClass.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedClass?.id == issueClass.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                .navigationTitle(field.label)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(AppStrings.Common.cancel) {
                            showSheet = false
                        }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private func selectIssueClass(_ newValue: IssueClass?) {
        storage.setValue(newValue, for: field.id)

        // Prepopulate title when issue class is selected
        if let issueClass = newValue,
           let fixedNode = field.metadata["fixedNode"] as? NodeV2 {
            let generatedTitle = "\(issueClass.name) on \(fixedNode.label)"

            let currentTitle = storage.getValue(for: "title") as? String ?? ""
            let shouldRegenerate = currentTitle.isEmpty || isAutoGeneratedTitle(currentTitle, for: fixedNode)

            if shouldRegenerate {
                storage.setValue(generatedTitle, for: "title")
            }
        }
    }

    private func isAutoGeneratedTitle(_ title: String, for node: NodeV2) -> Bool {
        return title.hasSuffix(" on \(node.label)")
    }
}

struct UserTaskFormPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query private var forms: [UserTaskForm]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? UserTaskForm },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as UserTaskForm?)
            ForEach(forms.sorted { $0.title < $1.title }) { form in
                Text(form.displayName).tag(form as UserTaskForm?)
            }
        }
    }
}

struct EdgeV2PickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<EdgeV2> { !$0.is_deleted }) private var edges: [EdgeV2]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? EdgeV2 },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as EdgeV2?)
            ForEach(edges) { edge in
                Text(edge.displayName).tag(edge as EdgeV2?)
            }
        }
    }
}

struct SLDV2PickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query private var slds: [SLDV2]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? SLDV2 },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as SLDV2?)
            ForEach(slds.sorted { $0.name < $1.name }) { sld in
                Text(sld.name).tag(sld as SLDV2?)
            }
        }
    }
}

struct UserTaskPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<UserTask> { !$0.is_deleted }) private var tasks: [UserTask]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? UserTask },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as UserTask?)
            ForEach(tasks.sorted { $0.title < $1.title }) { task in
                Text(task.title).tag(task as UserTask?)
            }
        }
    }
}

struct IssuePickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<Issue> { !$0.is_deleted }) private var issues: [Issue]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? Issue },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as Issue?)
            ForEach(issues) { issue in
                Text(issue.title ?? "Untitled Issue").tag(issue as Issue?)
            }
        }
    }
}

struct IRPhotoPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<IRPhoto> { !$0.is_deleted }) private var irPhotos: [IRPhoto]
    
    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? IRPhoto },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as IRPhoto?)
            ForEach(irPhotos) { photo in
                Text(photo.displayName).tag(photo as IRPhoto?)
            }
        }
    }
}

struct IRSessionPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query private var sessions: [IRSession]

    private var availableSessions: [IRSession] {
        sessions.filter { !$0.is_deleted }
    }

    var body: some View {
        Picker(field.label, selection: Binding(
            get: { storage.getValue(for: field.id) as? IRSession },
            set: { storage.setValue($0, for: field.id) }
        )) {
            Text(AppStrings.Common.none).tag(nil as IRSession?)
            ForEach(availableSessions.sorted { $0.name < $1.name }) { session in
                Text(session.name).tag(session as IRSession?)
            }
        }
    }
}

// MARK: - Multi-Picker Fields

struct NodeV2MultiPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @StateObject private var viewModel = NodeV2MultiPickerViewModel()
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var nodes: [NodeV2]
    @Query private var slds: [SLDV2]

    private var currentSLD: SLDV2? {
        return slds.first
    }

    var body: some View {
        let formNodes = viewModel.nodesFromForms(storage)

        VStack(alignment: .leading, spacing: 0) {
            Button(action: {
                viewModel.showingSelector = true
            }) {
                HStack {
                    Label(AppStrings.Tabs.assets, systemImage: "cube")
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()

                    if !viewModel.selectedNodes.isEmpty || !formNodes.isEmpty {
                        Text("\(viewModel.selectedNodes.count + formNodes.count)")
                            .font(.callout)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue)
                            .cornerRadius(10)
                    }

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            }
            .buttonStyle(PlainButtonStyle())

            if !viewModel.selectedNodes.isEmpty || !formNodes.isEmpty {
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        // Show directly selected nodes
                        ForEach(Array(viewModel.selectedNodes).sorted { $0.label < $1.label }) { node in
                            HStack {
                                Image(systemName: "cube")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(node.label)
                                    .font(.subheadline)
                                if let location = node.location {
                                    Text("• \(location)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Button {
                                    viewModel.removeNode(node, storage: storage, fieldId: field.id)
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding(.vertical, 8)
                            .padding(.horizontal, 12)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                        }

                        // Show nodes linked through forms (if not already directly selected)
                        ForEach(formNodes.filter { node in
                            !viewModel.selectedNodes.contains(where: { $0.id == node.id })
                        }.sorted { $0.label < $1.label }) { node in
                            HStack {
                                Image(systemName: "cube")
                                    .font(.caption)
                                    .foregroundColor(.blue)
                                Text(node.label)
                                    .font(.subheadline)
                                Text(AppStrings.Forms.viaForm)
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                                if let location = node.location {
                                    Text("• \(location)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                            }
                            .padding(.vertical, 8)
                            .padding(.horizontal, 12)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(8)
                        }
                    }
                }
                .frame(maxHeight: 200)
            }
        }
        .fullScreenCover(isPresented: $viewModel.showingSelector) {
            NavigationStack {
                ZStack {
                    VStack(spacing: 0) {
                        // Search bar
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.gray)
                            TextField(AppStrings.CommonExtra.searchAssets, text: $viewModel.searchText)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)

                        List {
                            ForEach(viewModel.displayedNodes) { node in
                                MultiPickerNodeRow(node: node, isSelected: viewModel.selectedNodes.contains(node))
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        viewModel.toggleNode(node)
                                    }
                            }
                        }
                    }

                    // Floating Action Buttons (QR Scanner + Plus)
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()

                            // QR Scanner Button
                            Button(action: {
                                viewModel.showingQRScanner = true
                            }) {
                                Image(systemName: "qrcode.viewfinder")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .frame(width: 56, height: 56)
                                    .background(Color.green)
                                    .clipShape(Circle())
                                    .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                            }
                            .padding(.trailing, 8)

                            // Add Asset Button
                            Button(action: {
                                // Small delay to ensure view hierarchy is stable
                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                    viewModel.showingAddAsset = true
                                }
                            }) {
                                Image(systemName: "plus")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                                    .frame(width: 56, height: 56)
                                    .background(Color.blue)
                                    .clipShape(Circle())
                                    .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                            }
                            .padding(.trailing, 20)
                        }
                        .padding(.bottom, 20)
                    }
                }
                .navigationTitle(AppStrings.Tabs.assets)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.cancel) {
                            viewModel.dismissSelector()
                        }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(AppStrings.Common.done) {
                            viewModel.confirmSelection(storage: storage, fieldId: field.id)
                        }
                        .fontWeight(.semibold)
                    }
                }
                .fullScreenCover(isPresented: $viewModel.showingQRScanner) {
                    QRCodeScannerView(
                        scannedCode: $viewModel.scannedQRCode,
                        onScanComplete: { code in
                            viewModel.handleQRScan(code, nodes: nodes, storage: storage, fieldId: field.id)
                        }
                    )
                }
                .alert(AppStrings.Forms.qrCodeError, isPresented: $viewModel.showingQRError) {
                    Button(AppStrings.Common.ok, role: .cancel) { }
                } message: {
                    Text(viewModel.qrErrorMessage)
                }
                .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $viewModel.showDuplicateQRAlert) {
                    ForEach(viewModel.duplicateNodes.prefix(5), id: \.id) { node in
                        Button(node.label) {
                            viewModel.selectDuplicateNode(node)
                        }
                    }
                    if viewModel.duplicateNodes.count > 5 {
                        Button(AppStrings.Site.showFirstFiveOnly) {
                            viewModel.clearDuplicateQRState()
                        }
                    }
                    Button(AppStrings.Common.cancel, role: .cancel) {
                        viewModel.clearDuplicateQRState()
                    }
                } message: {
                    Text(AppStrings.AssetsExtra.foundAssetsWithQR(viewModel.duplicateNodes.count))
                }
            }
            .fullScreenCover(isPresented: $viewModel.showingAddAsset) {
                if let sld = currentSLD {
                    AddAssetViewV2(
                        availableLocations: viewModel.cachedLocations,
                        availableNodeClasses: viewModel.cachedNodeClasses,
                        sld: sld,
                        onSave: { node, photos, irPhotos in
                            viewModel.handleAssetSaved(
                                node: node,
                                photos: photos,
                                irPhotos: irPhotos,
                                sld: sld,
                                modelContext: modelContext,
                                networkState: networkState,
                                storage: storage,
                                fieldId: field.id,
                                slds: slds
                            )
                        },
                        onCancel: {
                            viewModel.showingAddAsset = false
                        }
                    )
                    .environmentObject(networkState)
                    .environmentObject(AppStateManager.shared)
                    .environment(\.modelContext, modelContext)
                    .interactiveDismissDisabled()
                }
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
            viewModel.loadInitialValues(from: storage, fieldId: field.id)
            viewModel.refreshCachedData(slds: slds)
            viewModel.updateSortedNodes(from: nodes)
        }
        .onChange(of: nodes) { _, newNodes in
            viewModel.updateSortedNodes(from: newNodes)
        }
    }
}

// Helper struct to track form selections with unique IDs and linked nodes
private struct FormSelection: Identifiable, Equatable {
    let id: UUID
    let form: UserTaskForm
    var linkedNodes: [NodeV2]  // Nodes to link to this form instance

    init(form: UserTaskForm, linkedNodes: [NodeV2] = []) {
        self.id = UUID()
        self.form = form
        self.linkedNodes = linkedNodes
    }

    static func == (lhs: FormSelection, rhs: FormSelection) -> Bool {
        lhs.id == rhs.id &&
        lhs.form.id == rhs.form.id &&
        lhs.linkedNodes.map { $0.id } == rhs.linkedNodes.map { $0.id }
    }
}

struct UserTaskFormMultiPickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<UserTaskForm> { !$0.is_deleted }) private var forms: [UserTaskForm]
    @Query(filter: #Predicate<NodeV2> { !$0.is_deleted }) private var allNodes: [NodeV2]
    @State private var selectedFormSelections: [FormSelection] = []  // Track selections with unique IDs
    @State private var showingSelector = false
    @State private var editingSelection: FormSelection? = nil  // For node selection sheet
    @State private var searchText = ""

    // Use available nodes from metadata if provided, otherwise all nodes
    private var availableNodes: [NodeV2] {
        if let nodes = field.metadata["availableNodes"] as? [NodeV2] {
            AppLogger.log(.debug, "Form field has \(nodes.count) available nodes from metadata", category: .ui)
            return nodes
        }
        AppLogger.log(.debug, "Form field using all nodes (\(allNodes.count) total)", category: .ui)
        return allNodes
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: {
                showingSelector = true
            }) {
                HStack {
                    Label(AppStrings.Tasks.forms, systemImage: "doc.text")
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()

                    if !selectedFormSelections.isEmpty {
                        Text("\(selectedFormSelections.count)")
                            .font(.callout)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue)
                            .cornerRadius(10)
                    }

                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            }
            .buttonStyle(PlainButtonStyle())

            if !selectedFormSelections.isEmpty {
                ScrollView {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(selectedFormSelections) { selection in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Image(systemName: selection.form.is_global ? "globe" : "doc.text")
                                            .font(.caption)
                                            .foregroundColor(selection.form.is_global ? .blue : .secondary)
                                        Text(selection.form.title)
                                            .font(.subheadline)
                                        // Show count if there are duplicates
                                        let count = selectedFormSelections.filter { $0.form.id == selection.form.id }.count
                                        if count > 1 {
                                            let instanceNum = selectedFormSelections.filter { $0.form.id == selection.form.id && $0.id <= selection.id }.count
                                            Text("(Instance \(instanceNum) of \(count))")
                                                .font(.caption2)
                                                .foregroundColor(.secondary)
                                        }
                                        Spacer()
                                        Button {
                                            // Remove this specific selection by its unique ID
                                            selectedFormSelections.removeAll { $0.id == selection.id }
                                            updateStorage()
                                        } label: {
                                            Image(systemName: "xmark.circle.fill")
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }

                                    // Node info for this form instance
                                    if !selection.linkedNodes.isEmpty {
                                        HStack {
                                            Image(systemName: "cube")
                                                .font(.caption2)
                                                .foregroundColor(.secondary)
                                            Text("\(selection.linkedNodes.count) node\(selection.linkedNodes.count == 1 ? "" : "s") linked")
                                                .font(.caption)
                                                .foregroundColor(.blue)
                                        }
                                        .padding(.leading, 20)
                                    }
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)

                                // Node link button on the right
                                Button {
                                    editingSelection = selection
                                } label: {
                                    Image(systemName: "cube.fill")
                                        .font(.body)
                                        .foregroundColor(.blue)
                                        .frame(width: 30, height: 30)
                                }
                                .buttonStyle(BorderlessButtonStyle())
                            }
                            .padding(.vertical, 8)
                            .padding(.horizontal, 12)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                        }
                    }
                }
                .frame(maxHeight: 200) // Limit height
            }
        }
        .fullScreenCover(isPresented: $showingSelector) {
            NavigationStack {
                VStack(spacing: 0) {
                    // Search bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField(AppStrings.Tasks.searchForms, text: $searchText)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)

                    List {
                        ForEach(formsGroupedBySubtype, id: \.0) { subtype, forms in
                            Section {
                                ForEach(forms) { form in
                                    HStack {
                                        Image(systemName: form.is_global ? "globe" : "doc.text")
                                            .foregroundColor(form.is_global ? .blue : .gray)
                                        VStack(alignment: .leading) {
                                            Text(form.title)
                                            if form.is_global {
                                                Text(AppStrings.Forms.globalForm)
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                        Spacer()
                                        // Show count of how many times this form is selected
                                        let count = selectedFormSelections.filter { $0.form.id == form.id }.count
                                        if count > 0 {
                                            HStack(spacing: 4) {
                                                if count > 1 {
                                                    Text("\(count)")
                                                        .font(.caption)
                                                        .foregroundColor(.white)
                                                        .padding(.horizontal, 6)
                                                        .padding(.vertical, 2)
                                                        .background(Color.blue)
                                                        .cornerRadius(10)
                                                }
                                                Image(systemName: "checkmark.circle.fill")
                                                    .foregroundColor(.blue)
                                            }
                                        }
                                    }
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        // Always add a new instance when tapped
                                        // Users can tap multiple times to add multiple instances
                                        selectedFormSelections.append(FormSelection(form: form, linkedNodes: []))
                                    }
                                    .onLongPressGesture {
                                        // Long press to remove all instances of this form
                                        selectedFormSelections.removeAll { $0.form.id == form.id }
                                    }
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
                .navigationTitle(AppStrings.Tasks.forms)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.cancel) {
                            searchText = ""
                            showingSelector = false
                        }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button(AppStrings.Common.done) {
                            updateStorage()
                            searchText = ""
                            showingSelector = false
                        }
                        .fontWeight(.semibold)
                    }
                }
            }
        }
        .fullScreenCover(item: $editingSelection) { selection in
            NodeSelectionSheet(
                selection: selection,
                availableNodes: availableNodes,
                taskNodes: getTaskNodes(),
                onSave: { updatedSelection in
                    if let index = selectedFormSelections.firstIndex(where: { $0.id == updatedSelection.id }) {
                        selectedFormSelections[index] = updatedSelection
                        updateStorage()
                    }
                }
            )
        }
        .onAppear {
            // Load initial values from storage
            if let storedData = storage.getValue(for: field.id) as? [[String: Any]] {
                selectedFormSelections = storedData.compactMap { data in
                    guard let form = data["form"] as? UserTaskForm else { return nil }
                    let nodes = data["nodes"] as? [NodeV2] ?? []
                    return FormSelection(form: form, linkedNodes: nodes)
                }
            }
        }
    }

    // Get node class IDs from staged nodes (nodes in storage)
    private var stagedNodeClassIds: Set<UUID> {
        var classIds = Set<UUID>()

        // Get nodes from storage (staged for linking to task)
        if let stagedNodes = storage.getValue(for: "nodes") as? [NodeV2] {
            for node in stagedNodes where !node.is_deleted {
                if let nodeClass = node.node_class {
                    classIds.insert(nodeClass.id)
                }
            }
        }

        return classIds
    }

    private var hasStagedNodes: Bool {
        if let stagedNodes = storage.getValue(for: "nodes") as? [NodeV2] {
            return !stagedNodes.isEmpty
        }
        return false
    }

    private var filteredForms: [UserTaskForm] {
        let nodeForms: [UserTaskForm]

        if hasStagedNodes {
            // Show forms that match the staged nodes' classes AND general forms
            nodeForms = forms.filter { form in
                // Include forms that match the node class
                if let formNodeClassId = form.node_class_id {
                    return stagedNodeClassIds.contains(formNodeClassId)
                }
                // Also include general forms (no node_class_id)
                return true
            }
        } else {
            // No staged nodes - show only general forms (no node_class_id)
            nodeForms = forms.filter { form in
                form.node_class_id == nil
            }
        }

        // Apply search filter if search text is present
        if searchText.isEmpty {
            return nodeForms.sorted { $0.title < $1.title }
        } else {
            return nodeForms.filter { form in
                form.title.localizedCaseInsensitiveContains(searchText)
            }.sorted { $0.title < $1.title }
        }
    }

    // Group forms by node_subtype for better organization
    private var formsGroupedBySubtype: [(String, [UserTaskForm])] {
        let grouped = Dictionary(grouping: filteredForms) { form in
            form.node_subtype ?? "General"
        }

        // Sort by subtype name, with "General" last
        return grouped.sorted { lhs, rhs in
            if lhs.key == "General" { return false }
            if rhs.key == "General" { return true }
            return lhs.key < rhs.key
        }.map { (key, forms) in
            (key, forms.sorted { $0.title < $1.title })
        }
    }

    private func updateStorage() {
        // Store both forms and their linked nodes
        let data = selectedFormSelections.map { selection in
            [
                "form": selection.form,
                "nodes": selection.linkedNodes
            ] as [String: Any]
        }
        storage.setValue(data, for: field.id)

        // Also update the nodes field to include all nodes linked through forms
        updateNodesField()
    }

    private func updateNodesField() {
        // Get currently selected nodes
        var currentNodes = Set(storage.getValue(for: "nodes") as? [NodeV2] ?? [])

        // Add all nodes that are linked to form instances
        for selection in selectedFormSelections {
            for node in selection.linkedNodes {
                currentNodes.insert(node)
            }
        }

        // Update the nodes field
        storage.setValue(Array(currentNodes), for: "nodes")
    }

    private func getTaskNodes() -> [NodeV2] {
        // Get nodes that are already selected for the task
        // The nodes field in TaskCreationConfiguration has ID "nodes"
        if let nodes = storage.getValue(for: "nodes") as? [NodeV2] {
            return nodes
        }
        return []
    }
}

// MARK: - Node Selection Sheet for Form Instances
private struct NodeSelectionSheet: View {
    let selection: FormSelection
    let availableNodes: [NodeV2]
    let taskNodes: [NodeV2]  // Nodes already selected for the task
    let onSave: (FormSelection) -> Void

    @State private var selectedNodeIds: Set<UUID> = []
    @State private var searchText = ""
    @Environment(\.dismiss) private var dismiss

    private var filteredNodes: [NodeV2] {
        if searchText.isEmpty {
            return availableNodes
        } else {
            return availableNodes.filter { node in
                node.label.localizedCaseInsensitiveContains(searchText) ||
                (node.location?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                (node.sld?.name.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }
    }

    private var taskNodesFiltered: [NodeV2] {
        taskNodes.filter { taskNode in
            filteredNodes.contains { $0.id == taskNode.id }
        }
    }

    private var otherNodesFiltered: [NodeV2] {
        filteredNodes.filter { node in
            !taskNodes.contains { $0.id == node.id }
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
                .padding(.horizontal)
                .padding(.vertical, 8)

                List {
                    Section {
                        HStack {
                            Image(systemName: selection.form.is_global ? "globe" : "doc.text")
                                .foregroundColor(selection.form.is_global ? .blue : .secondary)
                            Text(selection.form.title)
                                .font(.headline)
                        }
                    } header: {
                        Text(AppStrings.Tasks.formInstance)
                    }

                    // Task nodes section (if any)
                    if !taskNodesFiltered.isEmpty {
                        Section {
                            ForEach(taskNodesFiltered.sorted { $0.label < $1.label }) { node in
                                HStack {
                                    Image(systemName: "cube")
                                        .foregroundColor(.blue)
                                    VStack(alignment: .leading) {
                                        Text(node.label)
                                        HStack(spacing: 4) {
                                            Image(systemName: "checkmark.circle.fill")
                                                .font(.caption2)
                                                .foregroundColor(.blue)
                                            Text(AppStrings.Forms.inTask)
                                                .font(.caption)
                                                .foregroundColor(.blue)
                                            if let sldName = node.sld?.name {
                                                Text("• \(sldName)")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                    }
                                    Spacer()
                                    if selectedNodeIds.contains(node.id) {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(.blue)
                                    }
                                }
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    if selectedNodeIds.contains(node.id) {
                                        selectedNodeIds.remove(node.id)
                                    } else {
                                        selectedNodeIds.insert(node.id)
                                    }
                                }
                            }
                        } header: {
                            Text(AppStrings.Forms.taskAssets)
                        }
                    }

                    // Other available nodes section
                    Section {
                        if otherNodesFiltered.isEmpty && !searchText.isEmpty {
                            Text(AppStrings.Forms.noNodesFoundMatching(searchText))
                                .foregroundColor(.secondary)
                                .italic()
                        } else if otherNodesFiltered.isEmpty {
                            Text(AppStrings.Forms.noAdditionalNodesAvailable)
                                .foregroundColor(.secondary)
                                .italic()
                        } else {
                            ForEach(otherNodesFiltered.sorted { $0.label < $1.label }) { node in
                                HStack {
                                    Image(systemName: "cube")
                                        .foregroundColor(.gray)
                                    VStack(alignment: .leading) {
                                        Text(node.label)
                                        if let sldName = node.sld?.name {
                                            Text(sldName)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                    Spacer()
                                    if selectedNodeIds.contains(node.id) {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(.blue)
                                    }
                                }
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    if selectedNodeIds.contains(node.id) {
                                        selectedNodeIds.remove(node.id)
                                    } else {
                                        selectedNodeIds.insert(node.id)
                                    }
                                }
                            }
                        }
                    } header: {
                        Text(AppStrings.Forms.allAvailableAssets)
                    } footer: {
                        Text(AppStrings.Forms.theseNodesWillBeLinked)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(AppStrings.Forms.linkNodes)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.save) {
                        var updatedSelection = selection
                        // Convert selected IDs back to nodes
                        updatedSelection.linkedNodes = availableNodes.filter { selectedNodeIds.contains($0.id) }
                        onSave(updatedSelection)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .onAppear {
            selectedNodeIds = Set(selection.linkedNodes.map { $0.id })
        }
    }
}

// MARK: - Convenience initializer when no additional content is needed
extension EntityCreationView where AdditionalContent == EmptyView {
    init(configuration: Config) {
        self.configuration = configuration
        self.additionalContent = { _ in EmptyView() }
    }
}
