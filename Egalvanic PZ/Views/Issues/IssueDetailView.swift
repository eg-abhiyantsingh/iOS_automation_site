import SwiftUI
import SwiftData
import PhotosUI
import UIKit

struct IssueDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    @State private var vm: IssueDetailViewModel
    @State private var showIssueClassSheet = false
    @Query private var allIssueClasses: [IssueClass]

    let onDismiss: (() -> Void)?

    init(issue: Issue, stagedIRPhotos: [IRPhoto] = [], hideNavigationBar: Bool = false, onDismiss: (() -> Void)? = nil) {
        self.onDismiss = onDismiss
        _vm = State(initialValue: IssueDetailViewModel(
            issue: issue,
            stagedIRPhotos: stagedIRPhotos,
            hideNavigationBar: hideNavigationBar
        ))
    }

    var body: some View {
        @Bindable var vm = vm
        NavigationStack {
            ZStack {
                ScrollView {
                    VStack(spacing: 0) {
                        // Profile Section
                        IssueHeaderSection(
                            title: vm.draftTitle.isEmpty ? AppStrings.Issues.newIssue : vm.draftTitle,
                            status: LanguageManager.localizedStatus(vm.draftStatus),
                            statusColor: vm.statusColor,
                            nodeLabel: vm.issue.node?.label,
                            networkMode: networkState.mode
                        )

                        // Main Content
                        VStack(spacing: 20) {
                            // Basic Information Card
                            VStack(spacing: 16) {
                                SectionHeader(title: AppStrings.Issues.issueDetails, systemImage: "exclamationmark.triangle")

                                VStack(spacing: 12) {
                                    // Title field
                                    IssueModernTextField(
                                        label: AppStrings.Tasks.title,
                                        text: $vm.draftTitle,
                                        placeholder: AppStrings.Issues.enterIssueTitle,
                                        isRequired: true
                                    )

                                    // Status selector
                                    ModernMappedPicker(
                                        label: AppStrings.Issues.status,
                                        selection: $vm.draftStatus,
                                        options: vm.statusOptions,
                                        placeholder: AppStrings.Issues.selectStatus,
                                        displayColor: vm.statusColor,
                                        icon: "flag.fill",
                                        allowClear: false
                                    )

                                    // Priority selector
                                    ModernMappedPicker(
                                        label: AppStrings.Issues.priority,
                                        selection: $vm.draftPriority,
                                        options: vm.priorityOptions,
                                        placeholder: AppStrings.Issues.selectPriority,
                                        displayColor: vm.priorityDisplayColor,
                                        icon: "exclamationmark.circle"
                                    )

                                    // Subtype selector (conditional)
                                    if !vm.draftType.isEmpty {
                                        ModernMappedPicker(
                                            label: AppStrings.Issues.subtype,
                                            selection: $vm.draftSubtype,
                                            options: vm.issueSubtypes,
                                            placeholder: AppStrings.Issues.selectSubtype,
                                            icon: "tag"
                                        )
                                    }

                                    // Issue Class Picker (sheet for performance)
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(AppStrings.Issues.issueClass)
                                            .font(.caption)
                                            .foregroundColor(.secondary)

                                        HStack {
                                            Image(systemName: "doc.text.fill")
                                                .foregroundColor(.secondary)
                                                .frame(width: 20)

                                            Text(vm.selectedIssueClass?.name ?? AppStrings.Issues.selectIssueClass)
                                                .foregroundColor(vm.selectedIssueClass != nil ? .primary : .secondary)

                                            Spacer()

                                            if vm.selectedIssueClass != nil {
                                                Image(systemName: "xmark.circle.fill")
                                                    .foregroundColor(.secondary)
                                                    .font(.body)
                                                    .frame(width: 30, height: 30)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        var transaction = Transaction()
                                                        transaction.disablesAnimations = true
                                                        withTransaction(transaction) {
                                                            vm.selectedIssueClass = nil
                                                            vm.draftCoreAttributes = [:]
                                                            vm.draftUnits = [:]
                                                            vm.draftCoreAttributeDescriptions = [:]
                                                            vm.overriddenFields = []
                                                            vm.autoFilledFields = []
                                                            vm.autoFillPinnedFields = []
                                                        }
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
                                            showIssueClassSheet = true
                                        }
                                        .sheet(isPresented: $showIssueClassSheet) {
                                            NavigationStack {
                                                List {
                                                    ForEach(vm.sortedIssueClasses(from: allIssueClasses), id: \.id) { issueClass in
                                                        Button {
                                                            vm.handleIssueClassChange(to: issueClass)
                                                            showIssueClassSheet = false
                                                        } label: {
                                                            HStack {
                                                                Text(issueClass.name)
                                                                    .foregroundColor(.primary)
                                                                Spacer()
                                                                if vm.selectedIssueClass?.id == issueClass.id {
                                                                    Image(systemName: "checkmark")
                                                                        .foregroundColor(.blue)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .navigationTitle(AppStrings.Issues.issueClass)
                                                .navigationBarTitleDisplayMode(.inline)
                                                .toolbar {
                                                    ToolbarItem(placement: .cancellationAction) {
                                                        Button(AppStrings.Common.cancel) {
                                                            showIssueClassSheet = false
                                                        }
                                                    }
                                                }
                                            }
                                            .presentationDetents([.medium, .large])
                                        }
                                    }
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)

                            // Safety & Notification Section (isolated to prevent full body re-render)
                            IssueSafetyCard(vm: vm)

                            // Core Attributes Section (isolated to prevent full body re-render on typing)
                            IssueCoreAttributesCard(vm: vm)

                            // Description Card (isolated to prevent full body re-render on typing)
                            IssueDescriptionCard(vm: vm)

                            // Proposed Resolution Card (isolated to prevent full body re-render on typing)
                            IssueProposedResolutionCard(vm: vm)

                            // IR Photos Card (if session exists)
                            if vm.issue.session != nil {
                                vm.issue.irPhotoListView(
                                    modelContext: modelContext,
                                    hasChanges: $vm.irPhotosHaveChanged,
                                    onLinkedPhotosChanged: { photos in
                                        vm.linkedIRPhotos = photos
                                    }
                                )
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(16)
                                .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
                            }

                            // Regular Photos Card
                            VStack(spacing: 16) {
                                EntitySimplePhotoPicker(
                                    entity: vm.issue,
                                    photoType: "issue",
                                    displayedPhotos: Binding(
                                        get: { vm.visiblePhotos },
                                        set: { _ in }
                                    ),
                                    isSaving: $vm.isSaving,
                                    onPhotoAdded: { photo in
                                        vm.handlePhotoAdded(photo)
                                    },
                                    onPhotoDeleted: { photo in
                                        vm.handlePhotoDeleted(photo)
                                    },
                                    sectionTitle: AppStrings.Issues.issuePhotos,
                                    sectionIcon: "photo.stack"
                                )
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)

                            // Delete Button
                            Button(role: .destructive) {
                                vm.showingDeleteAlert = true
                            } label: {
                                HStack {
                                    Image(systemName: "trash")
                                    Text(AppStrings.Issues.deleteIssue)
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
                        .padding()
                        .padding(.bottom, vm.hasChanges ? 80 : 20)
                    }
                }
                .background(Color(.systemGray6))
                .scrollDismissesKeyboard(.interactively)

                // Bottom Action Bar
                if vm.hasChanges {
                    VStack {
                        Spacer()

                        VStack(spacing: 0) {
                            Rectangle()
                                .fill(Color(.systemBackground))
                                .frame(height: 0)
                                .shadow(color: .black.opacity(0.1), radius: 10, y: -5)

                            Button(action: {
                                vm.saveChanges(modelContext: modelContext, networkState: networkState, dismiss: dismiss, onDismiss: onDismiss)
                            }) {
                                if vm.isSaving {
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
                            .disabled(vm.isSaving || vm.draftTitle.isEmpty)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                            .padding(.bottom, 18)
                            .background(Color(.systemBackground))
                        }
                    }
                    .ignoresSafeArea()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(vm.hasChanges || vm.hideNavigationBar)
            .navigationTitle(vm.hideNavigationBar ? "" : AppStrings.Issues.issueDetails)
            .navigationBarHidden(vm.hideNavigationBar)
            .toolbar(vm.hideNavigationBar ? .hidden : .visible, for: .navigationBar)
            .toolbar {
                if !vm.hideNavigationBar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        if vm.hasChanges {
                            Button(AppStrings.Common.cancel) {
                                vm.showingDiscardChangesAlert = true
                            }
                            .foregroundColor(.red)
                        } else {
                            Button(AppStrings.CommonExtra.close) {
                                dismiss()
                            }
                        }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            vm.showingStatusHistory = true
                        } label: {
                            Image(systemName: "clock.arrow.circlepath")
                        }
                    }
                }
            }
            .sheet(isPresented: $vm.showingStatusHistory) {
                IssueStatusHistoryView(issue: vm.issue)
                    .environmentObject(networkState)
            }
        }
        .alert(AppStrings.Issues.deleteIssue, isPresented: $vm.showingDeleteAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.delete, role: .destructive) {
                vm.deleteIssue(modelContext: modelContext, dismiss: dismiss)
            }
        } message: {
            Text(AppStrings.Issues.deleteIssueConfirm)
        }
        .alert(AppStrings.Assets.saveFailed, isPresented: Binding(
            get: { vm.saveError != nil },
            set: { if !$0 { vm.saveError = nil } }
        )) {
            Button(AppStrings.Common.ok) { vm.saveError = nil }
        } message: {
            Text(vm.saveError?.localizedDescription ?? AppStrings.AssetsExtra.unknownError)
        }
        .alert(AppStrings.Alerts.discardChanges, isPresented: $vm.showingDiscardChangesAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.discard, role: .destructive) {
                vm.cleanupStagedPhotos()
                dismiss()
            }
        } message: {
            Text(AppStrings.Alerts.discardChangesMessage)
        }
        .overlay {
            if vm.isSaving {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.AssetsExtra.saving)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .fullScreenCover(item: $vm.selectedPhoto) { photo in
            FullImageView(selected: photo, allPhotos: vm.visiblePhotos)
        }
        .onAppear {
            vm.loadIssueData()
        }
    }
}

// MARK: - Isolated Sub-Views (prevent full body re-render on typing)

/// Isolated safety toggles — only re-renders when hazard/notification state changes.
private struct IssueSafetyCard: View {
    @Bindable var vm: IssueDetailViewModel

    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(title: AppStrings.Issues.safetyAndNotification, systemImage: "exclamationmark.shield")

            YesNoToggleRow(
                label: AppStrings.Issues.immediateHazard,
                value: $vm.draftImmediateHazard,
                yesColor: .red,
                noColor: .green
            )

            YesNoToggleRow(
                label: AppStrings.Issues.customerNotified,
                value: $vm.draftCustomerNotified,
                yesColor: .green,
                noColor: .red
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}

/// Isolated core attributes section — only re-renders when core attribute state changes,
/// preventing EntityLinkedList and other heavy siblings from re-rendering on each keystroke.
private struct IssueCoreAttributesCard: View {
    @Bindable var vm: IssueDetailViewModel

    var body: some View {
        if vm.selectedIssueClass != nil {
            VStack(spacing: 16) {
                IssueEntityCoreAttributesSection(
                    issue: vm.issue,
                    selectedIssueClass: vm.selectedIssueClass,
                    draftAttributes: $vm.draftCoreAttributes,
                    draftUnits: $vm.draftUnits,
                    overriddenFields: $vm.overriddenFields,
                    draftDescriptions: $vm.draftCoreAttributeDescriptions,
                    onAttributeChanged: { vm.recalculateFields() },
                    onRefreshCalculation: { propId in
                        vm.refreshSingleCalculatedField(propId)
                    },
                    onFieldValueChanged: { fieldId in
                        if vm.autoFilledFields.contains(fieldId) {
                            vm.autoFillPinnedFields.insert(fieldId)
                        }
                        vm.triggerAutoFill(changedFieldId: fieldId)
                    },
                    onFieldDescriptionChanged: { fieldId in
                        if vm.autoFilledFields.contains(fieldId) {
                            vm.autoFillPinnedFields.insert(fieldId)
                        }
                    }
                )
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        }
    }
}

/// Isolated description editor — only re-renders when draftDescription changes,
/// preventing EntityLinkedList and other heavy siblings from re-rendering on each keystroke.
private struct IssueDescriptionCard: View {
    @Bindable var vm: IssueDetailViewModel

    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(title: AppStrings.Tasks.description_, systemImage: "text.alignleft")

            ModernTextEditor(
                text: $vm.draftDescription,
                placeholder: AppStrings.Issues.describeIssue,
                minHeight: 100
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}

/// Isolated proposed resolution editor — same isolation benefit as IssueDescriptionCard.
private struct IssueProposedResolutionCard: View {
    @Bindable var vm: IssueDetailViewModel

    var body: some View {
        VStack(spacing: 16) {
            IssueSectionHeader(title: AppStrings.Issues.proposedResolution, systemImage: "lightbulb")

            ModernTextEditor(
                text: $vm.draftProposedResolution,
                placeholder: AppStrings.Issues.suggestResolution,
                minHeight: 80
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
    }
}

// MARK: - Modern UI Components

struct IssueHeaderSection: View {
    let title: String
    let status: String
    let statusColor: Color
    let nodeLabel: String?
    let networkMode: NetworkMode
    
    var body: some View {
        VStack(spacing: 16) {
            // Issue Icon
            ZStack {
                Circle()
                    .fill(LinearGradient(
                        colors: [statusColor.opacity(0.2), statusColor.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .frame(width: 100, height: 100)
                
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 50))
                    .foregroundColor(statusColor)
            }
            .shadow(color: statusColor.opacity(0.3), radius: 10, y: 5)
            
            // Issue Info
            VStack(spacing: 8) {
                Text(title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Label(status, systemImage: "flag.fill")
                    .font(.subheadline)
                    .foregroundColor(statusColor)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(statusColor.opacity(0.1))
                    .cornerRadius(20)
                
                if let nodeLabel = nodeLabel {
                    Label(nodeLabel, systemImage: "cube")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if networkMode == .offline {
                    Label(AppStrings.CommonExtra.offline, systemImage: "wifi.slash")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
        }
        .padding(.top, 20)
        .padding(.bottom, 30)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [statusColor.opacity(0.1), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}

struct IssueSectionHeader: View {
    let title: String
    let systemImage: String
    
    var body: some View {
        HStack {
            Label(title, systemImage: systemImage)
                .font(.headline)
                .foregroundColor(.primary)
            Spacer()
        }
    }
}

struct IssueModernTextField: View {
    let label: String
    @Binding var text: String
    let placeholder: String
    var isRequired: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                if isRequired && text.isEmpty {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption2)
                        .foregroundColor(.red)
                }
            }
            
            TextField(placeholder, text: $text)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
        }
    }
}

struct ModernTextEditor: View {
    @Binding var text: String
    let placeholder: String
    let minHeight: CGFloat
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            if text.isEmpty {
                Text(placeholder)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 8)
            }
            
            TextEditor(text: $text)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
        }
        .frame(minHeight: minHeight)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

/// A picker option that separates the API value (sent to server) from the display name (shown to user).
struct MappedPickerOption: Identifiable, Hashable {
    let apiValue: String
    let displayName: String
    var id: String { apiValue }
}

/// A picker that stores API values internally but shows localized display names.
/// Uses a bottom sheet instead of Menu for better performance with many options.
struct ModernMappedPicker: View {
    let label: String
    @Binding var selection: String
    let options: [MappedPickerOption]
    var placeholder: String = ""
    var displayColor: Color? = nil
    var icon: String = ""
    var allowClear: Bool = true

    @State private var showSheet = false

    private var displayText: String {
        if selection.isEmpty { return placeholder }
        return options.first(where: { $0.apiValue == selection })?.displayName ?? selection
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            HStack {
                if !icon.isEmpty {
                    Image(systemName: icon)
                        .foregroundColor(displayColor ?? .secondary)
                        .frame(width: 20)
                }

                Text(displayText)
                    .foregroundColor(selection.isEmpty ? .secondary : (displayColor ?? .primary))

                Spacer()

                if allowClear && !selection.isEmpty {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                        .font(.body)
                        .frame(width: 30, height: 30)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            var transaction = Transaction()
                            transaction.disablesAnimations = true
                            withTransaction(transaction) {
                                selection = ""
                            }
                        }
                }

                Image(systemName: "chevron.down")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity)
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
                                var transaction = Transaction()
                                transaction.disablesAnimations = true
                                withTransaction(transaction) {
                                    selection = option.apiValue
                                }
                                showSheet = false
                            } label: {
                                HStack {
                                    Text(option.displayName)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selection == option.apiValue {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle(label)
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
}

struct ModernButton: View {
    let title: String
    let icon: String
    var style: ButtonStyle = .primary
    let action: () -> Void
    
    enum ButtonStyle {
        case primary, secondary, destructive
        
        var backgroundColor: Color {
            switch self {
            case .primary: return .blue
            case .secondary: return Color(.systemGray5)
            case .destructive: return .red
            }
        }
        
        var foregroundColor: Color {
            switch self {
            case .primary: return .white
            case .secondary: return .primary
            case .destructive: return .white
            }
        }
    }
    
    var body: some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(style.backgroundColor)
                .foregroundColor(style.foregroundColor)
                .cornerRadius(10)
        }
    }
}

struct ModernActionButton: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

struct EmptyStateView: View {
    let icon: String
    let message: String
    let subMessage: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.secondary)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Text(subMessage)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity)
    }
}
