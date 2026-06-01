import SwiftUI
import SwiftData

@MainActor
@Observable
final class IssueDetailViewModel {
    // The Issue being edited (reference type, SwiftData model)
    let issue: Issue
    let stagedIRPhotos_init: [IRPhoto]
    let hideNavigationBar: Bool

    // MARK: - Draft Fields
    var draftTitle: String { didSet { recomputeHasChanges() } }
    var draftDescription: String { didSet { recomputeHasChanges() } }
    var draftType: String { didSet { recomputeHasChanges() } }
    var draftSubtype: String { didSet { recomputeHasChanges() } }
    var draftStatus: String { didSet { recomputeHasChanges() } }
    var draftProposedResolution: String { didSet { recomputeHasChanges() } }
    var draftPriority: String { didSet { recomputeHasChanges() } }
    var draftImmediateHazard: Bool { didSet { recomputeHasChanges() } }
    var draftCustomerNotified: Bool { didSet { recomputeHasChanges() } }

    // MARK: - Core Attributes State
    var draftCoreAttributes: [UUID: String] = [:] { didSet { recomputeHasChanges() } }
    var draftUnits: [UUID: String] = [:] { didSet { recomputeHasChanges() } }
    var overriddenFields: Set<UUID> = []
    var selectedIssueClass: IssueClass? { didSet { recomputeHasChanges() } }
    var showOnlyRequiredAttributes = false
    var autoFilledFields: Set<UUID> = []
    var autoFillPinnedFields: Set<UUID> = []
    var draftCoreAttributeDescriptions: [UUID: String] = [:] { didSet { recomputeHasChanges() } }

    // MARK: - Photo State
    var issuePhotos: [Photo] = []
    var stagedPhotoAdditions: [Photo] = [] { didSet { recomputeHasChanges() } }
    var stagedPhotoDeletions: Set<UUID> = [] { didSet { recomputeHasChanges() } }
    var linkedIRPhotos: [IRPhoto] = []
    var irPhotosHaveChanged = false { didSet { recomputeHasChanges() } }
    var selectedPhoto: Photo? = nil

    // MARK: - UI State
    var showingDeleteAlert = false
    var isSaving = false
    var saveError: Error? = nil
    var showingStatusHistory = false
    var showingDiscardChangesAlert = false

    // MARK: - Change Tracking (stored to avoid @Observable dependency on all draft fields)
    var hasChanges: Bool = false

    // MARK: - Original Values (for change detection)
    let originalTitle: String
    let originalDescription: String
    let originalType: String
    let originalSubtype: String
    let originalStatus: String
    let originalProposedResolution: String
    let originalPriority: String
    let originalCoreAttributes: [UUID: String]
    let originalUnits: [UUID: String]
    let originalIssueClass: IssueClass?
    let originalImmediateHazard: Bool
    let originalCustomerNotified: Bool
    let originalCoreAttributeDescriptions: [UUID: String]

    // MARK: - Init
    init(issue: Issue, stagedIRPhotos: [IRPhoto] = [], hideNavigationBar: Bool = false) {
        self.issue = issue
        self.stagedIRPhotos_init = stagedIRPhotos
        self.hideNavigationBar = hideNavigationBar

        // Snapshot original values (avoids reading live SwiftData model in hasChanges)
        self.originalTitle = issue.title ?? ""
        self.originalDescription = issue.issueDescription ?? ""
        self.originalType = issue.issue_type ?? ""
        self.originalSubtype = issue.issue_subtype ?? ""
        self.originalStatus = issue.status ?? "Open"
        self.originalProposedResolution = issue.proposed_resolution ?? ""
        self.originalPriority = issue.priority ?? ""
        self.originalImmediateHazard = issue.immediateHazard
        self.originalCustomerNotified = issue.customerNotified

        self.draftTitle = issue.title ?? ""
        self.draftDescription = issue.issueDescription ?? ""
        self.draftType = issue.issue_type ?? ""
        self.draftSubtype = issue.issue_subtype ?? ""
        self.draftStatus = issue.status ?? "Open"
        self.draftProposedResolution = issue.proposed_resolution ?? ""
        self.draftPriority = issue.priority ?? ""
        self.draftImmediateHazard = issue.immediateHazard
        self.draftCustomerNotified = issue.customerNotified

        // Initialize per-property descriptions from existing details
        var initialDescriptions: [UUID: String] = [:]
        for detail in issue.details {
            if let classPropId = detail.issue_class_property?.id, let desc = detail.attributeNotes, !desc.isEmpty {
                initialDescriptions[classPropId] = desc
            }
        }
        self.originalCoreAttributeDescriptions = initialDescriptions
        self.draftCoreAttributeDescriptions = initialDescriptions

        // Initialize core attributes
        self.originalIssueClass = issue.issue_class

        // Key by issue_class_property ID (definition ID) so it matches the view lookup
        var originalAttrs = Dictionary(
            issue.details.compactMap { detail -> (UUID, String)? in
                guard let classPropId = detail.issue_class_property?.id else {
                    return (detail.id, detail.value)
                }
                return (classPropId, detail.value)
            },
            uniquingKeysWith: { first, _ in first }
        )

        // Initialize units from existing details
        var initialUnits: [UUID: String] = [:]
        for detail in issue.details {
            if let classPropId = detail.issue_class_property?.id, let unit = detail.unit {
                initialUnits[classPropId] = unit
            }
        }
        // Default temperature fields to °F if no unit stored yet
        if let issueClass = issue.issue_class {
            for prop in issueClass.definition where prop.type == "temperature" {
                if initialUnits[prop.id] == nil {
                    initialUnits[prop.id] = "\u{00B0}F"
                }
            }
        }
        self.originalUnits = initialUnits

        // Pre-compute calculated field values so they're included in original snapshot
        var initialOverrides: Set<UUID> = []
        if let issueClass = issue.issue_class {
            // Build unit values for expression evaluation
            var initUnitValues: [String: String] = [:]
            for prop in issueClass.definition {
                if let unit = initialUnits[prop.id] {
                    let fieldName = prop.name.trimmingCharacters(in: .whitespaces)
                    initUnitValues["\(fieldName) Unit"] = unit
                }
            }

            let calculatedValues = ExpressionEvaluator.evaluateAllCalculatedFields(
                properties: issueClass.definition,
                currentValues: originalAttrs,
                additionalValues: initUnitValues
            )

            // Seed overrides: mark calculated fields whose stored value differs from computed
            for (propId, computedValue) in calculatedValues {
                let storedValue = originalAttrs[propId] ?? ""
                if !storedValue.isEmpty && storedValue != computedValue {
                    initialOverrides.insert(propId)
                } else {
                    originalAttrs[propId] = computedValue
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
                            initialUnits[prop.id] = initialUnits[tempProp.id] ?? "\u{00B0}F"
                            break
                        }
                    }
                }
            }
        }

        self.overriddenFields = initialOverrides
        self.originalCoreAttributes = originalAttrs
        self.draftCoreAttributes = originalAttrs
        self.draftUnits = initialUnits
        self.selectedIssueClass = issue.issue_class
    }

    // MARK: - Change Detection

    /// Recomputes `hasChanges` from snapshots (no live SwiftData reads).
    /// Called from didSet on all draft properties.
    private func recomputeHasChanges() {
        let newValue =
            draftTitle != originalTitle ||
            draftDescription != originalDescription ||
            draftType != originalType ||
            draftSubtype != originalSubtype ||
            draftStatus != originalStatus ||
            draftProposedResolution != originalProposedResolution ||
            draftPriority != originalPriority ||
            draftImmediateHazard != originalImmediateHazard ||
            draftCustomerNotified != originalCustomerNotified ||
            !stagedPhotoAdditions.isEmpty ||
            !stagedPhotoDeletions.isEmpty ||
            irPhotosHaveChanged ||
            draftCoreAttributes != originalCoreAttributes ||
            draftUnits != originalUnits ||
            draftCoreAttributeDescriptions != originalCoreAttributeDescriptions ||
            selectedIssueClass?.id != originalIssueClass?.id
        // Only notify observers if the value actually changed
        if newValue != hasChanges {
            hasChanges = newValue
        }
    }

    var visiblePhotos: [Photo] {
        let existingPhotos = issuePhotos.filter { photo in
            !photo.is_deleted && !stagedPhotoDeletions.contains(photo.id)
        }
        return existingPhotos + stagedPhotoAdditions
    }

    var statusColor: Color {
        switch draftStatus.lowercased() {
        case "open", "new":
            return .blue
        case "in progress", "pending":
            return .blue
        case "resolved", "closed":
            return .green
        default:
            return .gray
        }
    }

    var priorityDisplayColor: Color {
        switch draftPriority.lowercased() {
        case "critical": return .purple
        case "high": return .red
        case "medium": return .orange
        case "low": return .blue
        default: return .gray
        }
    }

    var issueTypes: [MappedPickerOption] {
        [
            MappedPickerOption(apiValue: "Safety", displayName: AppStrings.Issues.typeSafety),
            MappedPickerOption(apiValue: "Maintenance", displayName: AppStrings.Issues.typeMaintenance),
            MappedPickerOption(apiValue: "Electrical", displayName: AppStrings.Issues.typeElectrical),
            MappedPickerOption(apiValue: "Structural", displayName: AppStrings.Issues.typeStructural),
            MappedPickerOption(apiValue: "Environmental", displayName: AppStrings.Issues.typeEnvironmental),
            MappedPickerOption(apiValue: "Operational", displayName: AppStrings.Issues.typeOperational),
            MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther),
        ]
    }

    var issueSubtypes: [MappedPickerOption] {
        switch draftType {
        case "Safety":
            return [MappedPickerOption(apiValue: "Fall Hazard", displayName: AppStrings.Issues.fallHazard), MappedPickerOption(apiValue: "Fire Hazard", displayName: AppStrings.Issues.fireHazard), MappedPickerOption(apiValue: "Electrical Hazard", displayName: AppStrings.Issues.electricalHazard), MappedPickerOption(apiValue: "Chemical Hazard", displayName: AppStrings.Issues.chemicalHazard), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        case "Maintenance":
            return [MappedPickerOption(apiValue: "Routine", displayName: AppStrings.Issues.routine), MappedPickerOption(apiValue: "Preventive", displayName: AppStrings.Issues.preventive), MappedPickerOption(apiValue: "Corrective", displayName: AppStrings.Issues.corrective), MappedPickerOption(apiValue: "Emergency", displayName: AppStrings.Issues.emergency), MappedPickerOption(apiValue: "Deferred", displayName: AppStrings.Issues.deferred)]
        case "Electrical":
            return [MappedPickerOption(apiValue: "Wiring", displayName: AppStrings.Issues.wiring), MappedPickerOption(apiValue: "Circuit", displayName: AppStrings.Issues.circuit), MappedPickerOption(apiValue: "Equipment", displayName: AppStrings.Issues.equipment), MappedPickerOption(apiValue: "Lighting", displayName: AppStrings.Issues.lighting), MappedPickerOption(apiValue: "Power Supply", displayName: AppStrings.Issues.powerSupply), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        case "Structural":
            return [MappedPickerOption(apiValue: "Damage", displayName: AppStrings.Issues.damage), MappedPickerOption(apiValue: "Deterioration", displayName: AppStrings.Issues.deterioration), MappedPickerOption(apiValue: "Defect", displayName: AppStrings.Issues.defect), MappedPickerOption(apiValue: "Modification Required", displayName: AppStrings.Issues.modificationRequired), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        case "Environmental":
            return [MappedPickerOption(apiValue: "Contamination", displayName: AppStrings.Issues.contamination), MappedPickerOption(apiValue: "Waste", displayName: AppStrings.Issues.waste), MappedPickerOption(apiValue: "Emissions", displayName: AppStrings.Issues.emissions), MappedPickerOption(apiValue: "Noise", displayName: AppStrings.Issues.noise), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        case "Operational":
            return [MappedPickerOption(apiValue: "Performance", displayName: AppStrings.Issues.performance), MappedPickerOption(apiValue: "Efficiency", displayName: AppStrings.Issues.efficiency), MappedPickerOption(apiValue: "Reliability", displayName: AppStrings.Issues.reliability), MappedPickerOption(apiValue: "Capacity", displayName: AppStrings.Issues.capacity), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        default:
            return [MappedPickerOption(apiValue: "General", displayName: AppStrings.Issues.generalSubtype), MappedPickerOption(apiValue: "Specific", displayName: AppStrings.Issues.specificSubtype), MappedPickerOption(apiValue: "Other", displayName: AppStrings.Issues.typeOther)]
        }
    }

    var priorityOptions: [MappedPickerOption] {
        [
            MappedPickerOption(apiValue: "Critical", displayName: AppStrings.Issues.priorityCritical),
            MappedPickerOption(apiValue: "High", displayName: AppStrings.Issues.priorityHigh),
            MappedPickerOption(apiValue: "Medium", displayName: AppStrings.Issues.priorityMedium),
            MappedPickerOption(apiValue: "Low", displayName: AppStrings.Issues.priorityLow),
        ]
    }

    var statusOptions: [MappedPickerOption] {
        [
            MappedPickerOption(apiValue: "Open", displayName: AppStrings.Issues.statusOpen),
            MappedPickerOption(apiValue: "In Progress", displayName: AppStrings.Issues.statusInProgress),
            MappedPickerOption(apiValue: "Pending", displayName: AppStrings.Issues.statusPending),
            MappedPickerOption(apiValue: "Resolved", displayName: AppStrings.Issues.statusResolved),
            MappedPickerOption(apiValue: "Closed", displayName: AppStrings.Issues.statusClosed),
        ]
    }

    func sortedIssueClasses(from allIssueClasses: [IssueClass]) -> [IssueClass] {
        allIssueClasses
            .filter { !$0.is_deleted }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    // MARK: - Methods

    func handleIssueClassChange(to newIssueClass: IssueClass) {
        // Use CoreAttributesService to preserve attribute values when switching classes
        draftCoreAttributes = CoreAttributesService.preserveAttributeValues(
            from: selectedIssueClass,
            to: newIssueClass,
            currentAttributes: draftCoreAttributes
        )

        // Preserve temperature units for matching fields across class change
        var newUnits: [UUID: String] = [:]
        if let oldClass = selectedIssueClass {
            for newProp in newIssueClass.definition where newProp.type == "temperature" {
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
        for prop in newIssueClass.definition where prop.type == "temperature" {
            if newUnits[prop.id] == nil {
                newUnits[prop.id] = "\u{00B0}F"
            }
        }
        draftUnits = newUnits

        selectedIssueClass = newIssueClass
        draftCoreAttributeDescriptions = [:]
        overriddenFields = []
        autoFilledFields = []
        autoFillPinnedFields = []
        recalculateFields()

        // Trigger auto-fill in case preserved values match rules in the new class
        triggerAutoFillForAllFields()
    }

    func triggerAutoFillForAllFields() {
        guard let issueClass = selectedIssueClass else { return }
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

    func triggerAutoFill(changedFieldId: UUID) {
        guard let issueClass = selectedIssueClass else { return }
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

    func recalculateFields() {
        guard let issueClass = selectedIssueClass else { return }

        let hasCalculated = issueClass.definition.contains { $0.type == "calculated" && $0.calculationExpression != nil }
        guard hasCalculated else { return }

        // Build unit values so expressions like [Problem Temp Unit] can resolve
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

        // Only update values that actually changed to prevent infinite .onChange loop
        // Skip overridden fields to respect user's manual edit
        for (propId, newValue) in calculatedValues {
            guard !overriddenFields.contains(propId) else { continue }
            if draftCoreAttributes[propId] != newValue {
                draftCoreAttributes[propId] = newValue
            }
        }

        // Resolve inherited units for calculated fields from temperature references
        for prop in issueClass.definition where prop.type == "calculated" {
            if let expression = prop.calculationExpression {
                let refs = ExpressionEvaluator.extractFieldReferences(from: expression)
                for ref in refs {
                    if let tempProp = issueClass.definition.first(where: {
                        $0.name.trimmingCharacters(in: .whitespaces) == ref && $0.type == "temperature"
                    }) {
                        let inheritedUnit = draftUnits[tempProp.id] ?? "\u{00B0}F"
                        draftUnits[prop.id] = inheritedUnit
                        break
                    }
                }
            }
        }
    }

    func refreshSingleCalculatedField(_ propId: UUID) {
        guard let issueClass = selectedIssueClass,
              let prop = issueClass.definition.first(where: { $0.id == propId }),
              let expression = prop.calculationExpression else { return }

        var fieldValues: [String: String] = [:]
        for p in issueClass.definition {
            if let val = draftCoreAttributes[p.id], !val.isEmpty {
                fieldValues[p.name.trimmingCharacters(in: .whitespaces)] = val
            }
            // Include unit values for expression evaluation
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

    func colorForStatus(_ status: String) -> Color {
        switch status.lowercased() {
        case "open", "new":
            return .blue
        case "in progress", "pending":
            return .blue
        case "resolved", "closed":
            return .green
        default:
            return .gray
        }
    }

    func loadIssueData() {
        issuePhotos = issue.photos.filter { !$0.is_deleted }
        linkedIRPhotos = issue.ir_photos.filter { !$0.is_deleted }
    }

    // MARK: - Save & Delete

    func saveChanges(
        modelContext: ModelContext,
        networkState: NetworkState,
        dismiss: DismissAction,
        onDismiss: (() -> Void)?
    ) {
        guard !isSaving else { return }
        isSaving = true

        let updateData = IssueUpdateData(
            title: draftTitle,
            description: draftDescription,
            type: draftType,
            subtype: draftSubtype,
            status: draftStatus,
            proposedResolution: draftProposedResolution,
            priority: draftPriority.isEmpty ? nil : draftPriority,
            selectedIssueClass: selectedIssueClass,
            originalIssueClass: originalIssueClass,
            draftCoreAttributes: draftCoreAttributes,
            draftUnits: draftUnits,
            stagedPhotoAdditions: stagedPhotoAdditions,
            stagedPhotoDeletions: stagedPhotoDeletions,
            originalPhotos: issuePhotos,
            stagedIRPhotos: linkedIRPhotos,
            immediateHazard: draftImmediateHazard,
            customerNotified: draftCustomerNotified,
            draftCoreAttributeDescriptions: draftCoreAttributeDescriptions
        )

        Task { @MainActor in
            do {
                try await IssueService.updateIssueComprehensive(
                    issue,
                    with: updateData,
                    networkState: networkState,
                    modelContext: modelContext
                )

                AppLogger.log(.info, "Save completed successfully - dismissing view", category: .issue)
                isSaving = false
                irPhotosHaveChanged = false

                if let onDismiss = onDismiss {
                    onDismiss()
                } else {
                    dismiss()
                }
            } catch {
                AppLogger.log(.error, "Save failed: \(error) - \(error.localizedDescription)", category: .issue)
                isSaving = false
                saveError = error
            }
        }
    }

    func deleteIssue(
        modelContext: ModelContext,
        dismiss: DismissAction
    ) {
        IssueService.deleteIssue(
            issue,
            modelContext: modelContext
        ) { [weak self] success, message in
            if success {
                dismiss()
                if let errorMessage = message {
                    AppLogger.log(.notice, errorMessage, category: .issue)
                }
            } else {
                self?.saveError = NSError(
                    domain: "IssueDeletion",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: message ?? "Failed to delete issue"]
                )
            }
        }
    }

    // MARK: - Photo Helpers

    func handlePhotoAdded(_ photo: Photo) {
        stagedPhotoAdditions.append(photo)
    }

    func handlePhotoDeleted(_ photo: Photo) {
        if let index = stagedPhotoAdditions.firstIndex(where: { $0.id == photo.id }) {
            let removed = stagedPhotoAdditions.remove(at: index)
            if let path = removed.local_filepath {
                try? FileManager.default.removeItem(atPath: path)
            }
        } else {
            stagedPhotoDeletions.insert(photo.id)
        }
    }

    func cleanupStagedPhotos() {
        for photo in stagedPhotoAdditions {
            if let path = photo.local_filepath {
                let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let fileURL = documentsURL.appendingPathComponent(path)
                try? FileManager.default.removeItem(at: fileURL)
            }
        }
    }
}
