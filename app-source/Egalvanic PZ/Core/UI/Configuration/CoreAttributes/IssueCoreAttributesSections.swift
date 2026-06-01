import SwiftUI

struct IssueEntityCoreAttributesSection: View {
    let issue: Issue?
    let selectedIssueClass: IssueClass?
    @Binding var draftAttributes: [UUID: String]
    @Binding var draftUnits: [UUID: String]
    @Binding var overriddenFields: Set<UUID>
    @Binding var draftDescriptions: [UUID: String]
    @State private var showOnlyRequired = false
    var onAttributeChanged: (() -> Void)? = nil
    var onRefreshCalculation: ((UUID) -> Void)? = nil
    var onFieldValueChanged: ((UUID) -> Void)? = nil
    var onFieldDescriptionChanged: ((UUID) -> Void)? = nil

    init(
        issue: Issue? = nil,
        selectedIssueClass: IssueClass?,
        draftAttributes: Binding<[UUID: String]>,
        draftUnits: Binding<[UUID: String]>,
        overriddenFields: Binding<Set<UUID>>,
        draftDescriptions: Binding<[UUID: String]> = .constant([:]),
        onAttributeChanged: (() -> Void)? = nil,
        onRefreshCalculation: ((UUID) -> Void)? = nil,
        onFieldValueChanged: ((UUID) -> Void)? = nil,
        onFieldDescriptionChanged: ((UUID) -> Void)? = nil
    ) {
        self.issue = issue
        self.selectedIssueClass = selectedIssueClass
        self._draftAttributes = draftAttributes
        self._draftUnits = draftUnits
        self._overriddenFields = overriddenFields
        self._draftDescriptions = draftDescriptions
        self.onAttributeChanged = onAttributeChanged
        self.onRefreshCalculation = onRefreshCalculation
        self.onFieldValueChanged = onFieldValueChanged
        self.onFieldDescriptionChanged = onFieldDescriptionChanged
    }

    var body: some View {
        EntityCoreAttributesView(
            entity: issue,
            selectedEntityClass: selectedIssueClass,
            draftAttributes: $draftAttributes,
            showOnlyRequired: $showOnlyRequired,
            draftUnits: $draftUnits,
            overriddenFields: $overriddenFields,
            draftDescriptions: $draftDescriptions,
            sectionTitle: AppStrings.AssetsExtra.issuePropertiesTitle,
            sectionIcon: "doc.text",
            onAttributeChanged: onAttributeChanged,
            onRefreshCalculation: onRefreshCalculation,
            onFieldValueChanged: onFieldValueChanged,
            onFieldDescriptionChanged: onFieldDescriptionChanged
        )
    }
}
