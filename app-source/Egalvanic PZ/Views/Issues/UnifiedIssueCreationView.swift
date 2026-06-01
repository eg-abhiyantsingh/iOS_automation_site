import SwiftUI
import SwiftData

// MARK: - Issue Creation State (for core attributes + safety fields)

class IssueCreationState: ObservableObject {
    @Published var immediateHazard: Bool = false
    @Published var customerNotified: Bool = false
    @Published var draftCoreAttributes: [UUID: String] = [:]
    @Published var draftUnits: [UUID: String] = [:]
    @Published var overriddenFields: Set<UUID> = []
    @Published var draftCoreAttributeDescriptions: [UUID: String] = [:]
    @Published var autoFilledFields: Set<UUID> = []
    @Published var autoFillPinnedFields: Set<UUID> = []

    /// Track the current issue class to detect changes and preserve attributes across switches
    var currentIssueClass: IssueClass?

    func handleIssueClassChange(to newIssueClass: IssueClass?) {
        let oldClass = currentIssueClass

        if let newClass = newIssueClass {
            draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
                from: oldClass,
                to: newClass,
                currentAttributes: draftCoreAttributes
            )
            // Apply class-defined defaults (ZP-2251) to fields still empty
            // after preservation. Runs before recalculate/auto-fill below so
            // those operate on the post-default state.
            CoreAttributesService.applyDefaultValues(from: newClass, into: &draftCoreAttributes)
        } else {
            draftCoreAttributes = [:]
        }

        draftCoreAttributeDescriptions = [:]
        overriddenFields = []
        autoFilledFields = []
        autoFillPinnedFields = []

        // Preserve temperature units for matching fields across class change
        if let newClass = newIssueClass {
            var newUnits: [UUID: String] = [:]
            if let oldClass = oldClass {
                for newProp in newClass.definition where newProp.type == "temperature" {
                    // Try matching by ID first, then by name
                    if let existingUnit = draftUnits[newProp.id] {
                        newUnits[newProp.id] = existingUnit
                    } else if let matchingOldProp = oldClass.definition.first(where: {
                        $0.name == newProp.name && $0.type == "temperature"
                    }), let existingUnit = draftUnits[matchingOldProp.id] {
                        newUnits[newProp.id] = existingUnit
                    }
                }
            }
            // Default unset temperature fields to °F
            for prop in newClass.definition where prop.type == "temperature" {
                if newUnits[prop.id] == nil {
                    newUnits[prop.id] = "\u{00B0}F"
                }
            }
            draftUnits = newUnits
        } else {
            draftUnits = [:]
        }

        currentIssueClass = newIssueClass
        recalculateFields(issueClass: newIssueClass)

        // Trigger auto-fill in case preserved values match rules in the new class
        triggerAutoFillForAllFields(issueClass: newIssueClass)
    }

    /// Trigger auto-fill from all select/multi_select fields (used after class change with preserved values)
    func triggerAutoFillForAllFields(issueClass: IssueClass?) {
        guard let issueClass = issueClass else { return }
        let properties = issueClass.definition
        DispatchQueue.main.async { [self] in
            let fills = AutoFillEngine.computeAutoFills(
                properties: properties,
                currentValues: draftCoreAttributes
            )
            var newAutoFilledFields: Set<UUID> = []
            for (targetId, fillResult) in fills {
                if draftCoreAttributes[targetId] != fillResult.value {
                    draftCoreAttributes[targetId] = fillResult.value
                }
                // Apply description if present
                if let desc = fillResult.description {
                    draftCoreAttributeDescriptions[targetId] = desc
                }
                newAutoFilledFields.insert(targetId)
            }
            autoFilledFields = newAutoFilledFields
        }
    }

    func triggerAutoFill(changedFieldId: UUID, issueClass: IssueClass?) {
        guard let issueClass = issueClass else { return }
        // Defer auto-fill to next run loop to ensure the triggering field's
        // binding update has fully committed before we modify more fields
        let properties = issueClass.definition
        DispatchQueue.main.async { [self] in
            let fills = AutoFillEngine.computeAutoFills(
                properties: properties,
                currentValues: draftCoreAttributes
            )
            // Clear previously auto-filled fields that are no longer in the new fills
            for fieldId in autoFilledFields {
                guard !autoFillPinnedFields.contains(fieldId) else { continue }
                if fills[fieldId] == nil {
                    draftCoreAttributes.removeValue(forKey: fieldId)
                    draftCoreAttributeDescriptions.removeValue(forKey: fieldId)
                }
            }
            // Apply new fills
            var newAutoFilledFields: Set<UUID> = []
            for (targetId, fillResult) in fills {
                guard !autoFillPinnedFields.contains(targetId) else { continue }
                guard targetId != changedFieldId else { continue }
                if draftCoreAttributes[targetId] != fillResult.value {
                    draftCoreAttributes[targetId] = fillResult.value
                }
                // Apply description if present
                if let desc = fillResult.description {
                    draftCoreAttributeDescriptions[targetId] = desc
                }
                newAutoFilledFields.insert(targetId)
            }
            autoFilledFields = newAutoFilledFields
        }
    }

    func recalculateFields(issueClass: IssueClass?) {
        guard let issueClass = issueClass else { return }

        let hasCalculated = issueClass.definition.contains { $0.type == "calculated" && $0.calculationExpression != nil }
        guard hasCalculated else { return }

        var unitValues: [String: String] = [:]
        for prop in issueClass.definition {
            if let unit = draftUnits[prop.id] {
                let fieldName = prop.name.trimmingCharacters(in: .whitespaces)
                unitValues["\(fieldName) Unit"] = unit
            }
        }

        let calculatedValues = ExpressionEvaluator.evaluateAllCalculatedFields(
            properties: issueClass.definition,
            currentValues: draftCoreAttributes,
            additionalValues: unitValues
        )

        for (propId, newValue) in calculatedValues {
            guard !overriddenFields.contains(propId) else { continue }
            if draftCoreAttributes[propId] != newValue {
                draftCoreAttributes[propId] = newValue
            }
        }

        // Resolve inherited units for calculated fields
        for prop in issueClass.definition where prop.type == "calculated" {
            if let expression = prop.calculationExpression {
                let refs = ExpressionEvaluator.extractFieldReferences(from: expression)
                for ref in refs {
                    if let tempProp = issueClass.definition.first(where: {
                        $0.name.trimmingCharacters(in: .whitespaces) == ref && $0.type == "temperature"
                    }) {
                        draftUnits[prop.id] = draftUnits[tempProp.id] ?? "\u{00B0}F"
                        break
                    }
                }
            }
        }
    }

    func refreshSingleCalculatedField(_ propId: UUID, issueClass: IssueClass?) {
        guard let issueClass = issueClass,
              let prop = issueClass.definition.first(where: { $0.id == propId }),
              let expression = prop.calculationExpression else { return }

        var fieldValues: [String: String] = [:]
        for p in issueClass.definition {
            if let val = draftCoreAttributes[p.id], !val.isEmpty {
                fieldValues[p.name.trimmingCharacters(in: .whitespaces)] = val
            }
            if let unit = draftUnits[p.id] {
                let fieldName = p.name.trimmingCharacters(in: .whitespaces)
                fieldValues["\(fieldName) Unit"] = unit
            }
        }

        if let result = ExpressionEvaluator.evaluate(
            expression: expression,
            fieldValues: fieldValues,
            precision: prop.calculationPrecision
        ) {
            draftCoreAttributes[propId] = result
        }
    }
}

// MARK: - Issue Additional Sections View (safety toggles + core attributes)

struct IssueAdditionalSectionsView: View {
    @ObservedObject var storage: StandardFieldValueStorage
    @ObservedObject var issueState: IssueCreationState
    let fixedNode: NodeV2?

    private var selectedIssueClass: IssueClass? {
        storage.getValue(for: "issueClass") as? IssueClass
    }

    var body: some View {
        // Safety & Notification section
        Section {
            YesNoToggleRow(
                label: AppStrings.Issues.immediateHazard,
                value: $issueState.immediateHazard,
                yesColor: .red,
                noColor: .green
            )

            YesNoToggleRow(
                label: AppStrings.Issues.customerNotified,
                value: $issueState.customerNotified,
                yesColor: .green,
                noColor: .red
            )
        } header: {
            Text(AppStrings.Issues.safetyAndNotification)
        }

        // Core Attributes section (when issue class has definition)
        if let issueClass = selectedIssueClass, !issueClass.definition.isEmpty {
            Section {
                IssueEntityCoreAttributesSection(
                    issue: nil,
                    selectedIssueClass: selectedIssueClass,
                    draftAttributes: $issueState.draftCoreAttributes,
                    draftUnits: $issueState.draftUnits,
                    overriddenFields: $issueState.overriddenFields,
                    draftDescriptions: $issueState.draftCoreAttributeDescriptions,
                    onAttributeChanged: {
                        issueState.recalculateFields(issueClass: selectedIssueClass)
                    },
                    onRefreshCalculation: { propId in
                        issueState.refreshSingleCalculatedField(propId, issueClass: selectedIssueClass)
                    },
                    onFieldValueChanged: { fieldId in
                        if issueState.autoFilledFields.contains(fieldId) {
                            issueState.autoFillPinnedFields.insert(fieldId)
                        }
                        issueState.triggerAutoFill(changedFieldId: fieldId, issueClass: selectedIssueClass)
                    },
                    onFieldDescriptionChanged: { fieldId in
                        if issueState.autoFilledFields.contains(fieldId) {
                            issueState.autoFillPinnedFields.insert(fieldId)
                        }
                    }
                )
            }
        }

        // Assignment section (at the bottom, only when no fixed node)
        if fixedNode == nil {
            Section {
                NodeV2PickerField(
                    field: EntityFieldConfiguration(
                        id: "node",
                        label: AppStrings.Tasks.asset,
                        icon: "cube",
                        type: .nodeV2Picker,
                        isRequired: true,
                        helpText: AppStrings.Issues.selectAssetForIssue
                    ),
                    storage: storage
                )
            } header: {
                Text(AppStrings.Tasks.assignment)
            }
        }
    }
}

// MARK: - Unified Issue Creation View

struct UnifiedIssueCreationView: View {
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState

    // Optional fixed context (when creating from node detail)
    let fixedNode: NodeV2?
    let fixedSLD: SLDV2?

    // Callback when complete
    let onComplete: ((Issue?) -> Void)?

    @StateObject private var issueState = IssueCreationState()

    init(
        fixedNode: NodeV2? = nil,
        fixedSLD: SLDV2? = nil,
        onComplete: ((Issue?) -> Void)? = nil
    ) {
        self.fixedNode = fixedNode
        self.fixedSLD = fixedSLD
        self.onComplete = onComplete
    }

    var body: some View {
        EntityCreationView(
            configuration: IssueCreationConfiguration(
                appState: appState,
                networkState: networkState,
                fixedNode: fixedNode,
                fixedSLD: fixedSLD,
                issueState: issueState,
                onComplete: { issue in
                    onComplete?(issue)
                }
            )
        ) { storage in
            IssueAdditionalSectionsView(
                storage: storage,
                issueState: issueState,
                fixedNode: fixedNode
            )
            .onChange(of: (storage.getValue(for: "issueClass") as? IssueClass)?.id) { _, newVal in
                guard newVal != issueState.currentIssueClass?.id else { return }
                let newClass = storage.getValue(for: "issueClass") as? IssueClass
                issueState.handleIssueClassChange(to: newClass)
            }
        }
        .environmentObject(appState)
        .environmentObject(networkState)
    }
}

// MARK: - Convenience Views for Different Contexts

// View for creating issue from Issues list (no fixed node)
struct UnifiedIssueCreationFromListView: View {
    @EnvironmentObject var appState: AppStateManager

    @Query private var allSLDs: [SLDV2]

    let onIssueCreated: ((Issue) -> Void)?

    var body: some View {
        UnifiedIssueCreationView(
            fixedNode: nil,
            fixedSLD: allSLDs.first { $0.id == appState.activeSLDId },
            onComplete: { issue in
                if let issue = issue {
                    onIssueCreated?(issue)
                }
            }
        )
    }
}

// View for creating issue from node detail (fixed node)
struct UnifiedIssueCreationFromNodeView: View {
    let node: NodeV2
    let sld: SLDV2
    let onIssueCreated: ((Issue) -> Void)?

    var body: some View {
        UnifiedIssueCreationView(
            fixedNode: node,
            fixedSLD: sld,
            onComplete: { issue in
                if let issue = issue {
                    onIssueCreated?(issue)
                }
            }
        )
    }
}
