import SwiftUI
import SwiftData

struct IssueCreationConfiguration: EntityCreationConfiguration {
    typealias Entity = Issue
    
    // Dependencies
    let appState: AppStateManager
    let networkState: NetworkState
    let onComplete: ((Issue?) -> Void)?

    // Optional fixed context
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?

    // Issue-specific state for core attributes and safety fields
    let issueState: IssueCreationState

    init(
        appState: AppStateManager,
        networkState: NetworkState,
        fixedNode: NodeV2? = nil,
        fixedSLD: SLDV2? = nil,
        issueState: IssueCreationState = IssueCreationState(),
        onComplete: ((Issue?) -> Void)? = nil
    ) {
        self.appState = appState
        self.networkState = networkState
        self.fixedNode = fixedNode
        self.fixedSLD = fixedSLD
        self.issueState = issueState
        self.onComplete = onComplete
    }
    
    // MARK: - Basic Info
    var entityName: String { AppStrings.Issues.issue }
    var navigationTitle: String { AppStrings.Issues.newIssue }
    var createButtonTitle: String { AppStrings.Issues.createIssue }
    
    // MARK: - Field Configuration
    func fields() -> [EntityFieldConfiguration] {
        var fields: [EntityFieldConfiguration] = []
        
        // Issue Class picker
        var issueClassMetadata: [String: Any] = [:]
        if let node = fixedNode {
            issueClassMetadata["fixedNode"] = node
        }

        fields.append(EntityFieldConfiguration(
            id: "issueClass",
            label: AppStrings.Issues.issueClass,
            icon: "tag",
            type: .issueClassPicker,
            isRequired: true,
            section: AppStrings.Issues.classification,
            helpText: AppStrings.Issues.selectIssueType,
            metadata: issueClassMetadata
        ))
        
        // Title field
        fields.append(EntityFieldConfiguration(
            id: "title",
            label: AppStrings.Tasks.title,
            icon: "pencil",
            type: .text(placeholder: AppStrings.Issues.enterIssueTitle),
            isRequired: true,
            section: AppStrings.Issues.issueDetails,
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

        // Priority picker
        fields.append(EntityFieldConfiguration(
            id: "priority",
            label: AppStrings.Issues.priority,
            icon: "exclamationmark.circle",
            type: .picker(options: [
                PickerOption(id: "Critical", displayName: AppStrings.Issues.priorityCritical, icon: "exclamationmark.triangle.fill", color: .purple),
                PickerOption(id: "High", displayName: AppStrings.Issues.priorityHigh, icon: "exclamationmark.3", color: .red),
                PickerOption(id: "Medium", displayName: AppStrings.Issues.priorityMedium, icon: "exclamationmark.2", color: .orange),
                PickerOption(id: "Low", displayName: AppStrings.Issues.priorityLow, icon: "exclamationmark", color: .blue)
            ]),
            isRequired: false,
            section: AppStrings.Issues.issueDetails,
            helpText: AppStrings.Issues.selectIssuePriorityLevel
        ))
        
        return fields
    }

    func sections() -> [String]? {
        return [AppStrings.Issues.classification, AppStrings.Issues.issueDetails]
    }
    
    // MARK: - SwiftData Models
    func swiftDataModels() -> [any PersistentModel.Type] {
        [NodeV2.self, IssueClass.self, SLDV2.self]
    }
    
    // MARK: - Validation
    func validateForm(storage: FieldValueStorage) -> ValidationResult {
        // Title is required
        guard let title = storage.getValue(for: "title") as? String,
              !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return .invalid(AppStrings.Tasks.titleIsRequired)
        }

        // Issue class is required
        guard storage.getValue(for: "issueClass") != nil else {
            return .invalid(AppStrings.Issues.pleaseSelectIssueClass)
        }

        // Node is required (either fixed or selected)
        if fixedNode == nil {
            guard storage.getValue(for: "node") != nil else {
                return .invalid(AppStrings.Issues.pleaseSelectAsset)
            }
        }

        return .valid
    }
    
    // MARK: - Actions
    @MainActor
    func onCreate(storage: FieldValueStorage, modelContext: ModelContext) -> Issue? {
        // Get values from storage
        let title = storage.getValue(for: "title") as? String ?? ""
        let priority = storage.getValue(for: "priority") as? String
        let issueClass = storage.getValue(for: "issueClass") as? IssueClass
        let node = fixedNode ?? (storage.getValue(for: "node") as? NodeV2)

        // Ensure node and SLD exist
        guard let node = node else {
            AppLogger.log(.notice, AppStrings.Issues.pleaseSelectAsset, category: .issue)
            return nil
        }
        guard let sld = fixedSLD ?? node.sld else {
            AppLogger.log(.notice, AppStrings.Issues.cannotCreateWithoutSLD, category: .issue)
            return nil
        }

        // Build IssueProperty array from core attributes state
        var details: [IssueProperty] = []
        if let issueClass = issueClass {
            for classProperty in issueClass.definition {
                let propId = classProperty.id
                guard let value = issueState.draftCoreAttributes[propId], !value.isEmpty else { continue }

                let detail = IssueProperty(
                    id: propId,
                    issue_class_property: classProperty,
                    name: classProperty.name,
                    value: value,
                    unit: issueState.draftUnits[propId],
                    attributeNotes: issueState.draftCoreAttributeDescriptions[propId]
                )
                modelContext.insert(detail)
                details.append(detail)
            }
        }

        // Create issue using IssueService
        let createdIssue = IssueService.createIssue(
            title: title,
            description: "",
            proposedResolution: nil,
            priority: priority,
            issueClass: issueClass,
            node: node,
            sld: sld,
            modelContext: modelContext,
            activeSession: appState.activeSession,
            immediateHazard: issueState.immediateHazard,
            customerNotified: issueState.customerNotified,
            details: details
        ) { success, _, message in
            if success {
                if let errorMessage = message {
                    AppLogger.log(.notice, errorMessage, category: .issue)
                }
            } else {
                AppLogger.log(.error, "Failed to create issue: \(message ?? "Unknown error")", category: .issue)
            }
        }

        if let createdIssue = createdIssue {
            onComplete?(createdIssue)
        }

        return createdIssue
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
                        Label(AppStrings.Issues.creatingIssueFor, systemImage: "cube")
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
