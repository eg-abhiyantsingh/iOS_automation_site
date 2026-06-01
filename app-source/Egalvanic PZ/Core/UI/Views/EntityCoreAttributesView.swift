//
//  EntityCoreAttributesView.swift
//  SwiftDataTutorial
//
//  Generic view for displaying and editing core attributes of any entity
//

import SwiftUI
import SwiftData

struct EntityCoreAttributesView<Entity: EntityWithCoreAttributes, HeaderTrailing: View>: View {
    let entity: Entity?
    let selectedEntityClass: Entity.ClassType?
    @Binding var draftAttributes: [UUID: String]
    @Binding var showOnlyRequired: Bool

    // Unit and override support (optional, defaults to empty for Node/Edge)
    @Binding var draftUnits: [UUID: String]
    @Binding var overriddenFields: Set<UUID>

    // Per-property description support
    @Binding var draftDescriptions: [UUID: String]

    // Optional customization
    var sectionTitle: String = AppStrings.Assets.coreAttributes
    var sectionIcon: String = "slider.horizontal.3"

    // Optional trailing content in the header (e.g., action buttons)
    let headerTrailingContent: HeaderTrailing

    // Callback when user edits a non-calculated attribute (for triggering recalculation)
    var onAttributeChanged: (() -> Void)? = nil
    // Callback to refresh a single calculated field
    var onRefreshCalculation: ((UUID) -> Void)? = nil
    // Callback with field ID when a non-calculated attribute value changes (for auto-fill)
    var onFieldValueChanged: ((UUID) -> Void)? = nil
    // Callback with field ID when a description is manually edited (for auto-fill pinning)
    var onFieldDescriptionChanged: ((UUID) -> Void)? = nil

    init(
        entity: Entity?,
        selectedEntityClass: Entity.ClassType?,
        draftAttributes: Binding<[UUID: String]>,
        showOnlyRequired: Binding<Bool>,
        draftUnits: Binding<[UUID: String]> = .constant([:]),
        overriddenFields: Binding<Set<UUID>> = .constant([]),
        draftDescriptions: Binding<[UUID: String]> = .constant([:]),
        sectionTitle: String = AppStrings.Assets.coreAttributes,
        sectionIcon: String = "slider.horizontal.3",
        @ViewBuilder headerTrailingContent: () -> HeaderTrailing,
        onAttributeChanged: (() -> Void)? = nil,
        onRefreshCalculation: ((UUID) -> Void)? = nil,
        onFieldValueChanged: ((UUID) -> Void)? = nil,
        onFieldDescriptionChanged: ((UUID) -> Void)? = nil
    ) {
        self.entity = entity
        self.selectedEntityClass = selectedEntityClass
        self._draftAttributes = draftAttributes
        self._showOnlyRequired = showOnlyRequired
        self._draftUnits = draftUnits
        self._overriddenFields = overriddenFields
        self._draftDescriptions = draftDescriptions
        self.sectionTitle = sectionTitle
        self.sectionIcon = sectionIcon
        self.headerTrailingContent = headerTrailingContent()
        self.onAttributeChanged = onAttributeChanged
        self.onRefreshCalculation = onRefreshCalculation
        self.onFieldValueChanged = onFieldValueChanged
        self.onFieldDescriptionChanged = onFieldDescriptionChanged
    }

    // PERFORMANCE: Pre-compute sorted properties once per render instead of computed property
    private func computeSortedProperties(entityClass: Entity.ClassType?, showRequired: Bool) -> [Entity.ClassType.PropertyDefinitionType] {
        guard let entityClass = entityClass else { return [] }

        let filtered = showRequired
            ? entityClass.definition.filter { $0.af_required }
            : entityClass.definition

        // Sort by index if available (for Issues), otherwise alphabetically
        return filtered.sorted { prop1, prop2 in
            // If both have index, sort by index
            if let index1 = prop1.index, let index2 = prop2.index {
                return index1 < index2
            }
            // If only one has index, prioritize it
            if prop1.index != nil { return true }
            if prop2.index != nil { return false }
            // Fallback to alphabetical sorting
            return prop1.name.localizedCaseInsensitiveCompare(prop2.name) == .orderedAscending
        }
    }

    // PERFORMANCE: Pre-compute required properties count once
    private func computeRequiredCount(entityClass: Entity.ClassType?) -> Int {
        entityClass?.definition.filter { $0.af_required }.count ?? 0
    }

    // PERFORMANCE: Pre-compute completed required count - this was expensive due to JSON parsing
    private func computeCompletedRequiredCount(entityClass: Entity.ClassType?, attributes: [UUID: String]) -> Int {
        guard let entityClass = entityClass else { return 0 }

        return entityClass.definition
            .filter { $0.af_required }
            .filter { property in
                let value = attributes[property.id] ?? ""
                // For table types, check if the JSON contains any non-empty values
                if property.type == "table_with_column_headers" || property.type == "table_with_row_headers" {
                    if !value.isEmpty,
                       let data = value.data(using: .utf8),
                       let json = try? JSONSerialization.jsonObject(with: data) as? [String: String] {
                        return !json.values.allSatisfy { $0.isEmpty }
                    }
                    return false
                }
                return !value.isEmpty
            }
            .count
    }

    /// Resolve the inherited unit for a calculated field by finding the first temperature
    /// field referenced in its expression.
    private func resolveCalculatedFieldUnit(_ calcProperty: Entity.ClassType.PropertyDefinitionType) -> String? {
        guard let expression = calcProperty.calculationExpression else { return nil }
        let refs = ExpressionEvaluator.extractFieldReferences(from: expression)
        guard let entityClass = selectedEntityClass else { return nil }

        for ref in refs {
            if let tempProp = entityClass.definition.first(where: {
                $0.name.trimmingCharacters(in: .whitespaces) == ref && $0.type == "temperature"
            }) {
                return draftUnits[tempProp.id] ?? "\u{00B0}F"
            }
        }
        return nil
    }

    var body: some View {
        // PERFORMANCE: Compute all derived values ONCE at the start of body
        let reqCount = computeRequiredCount(entityClass: selectedEntityClass)
        let completedReqCount = computeCompletedRequiredCount(entityClass: selectedEntityClass, attributes: draftAttributes)
        let sortedProps = computeSortedProperties(entityClass: selectedEntityClass, showRequired: showOnlyRequired)

        VStack(spacing: 16) {
            // Header with progress indicator
            HStack {
                SectionHeader(title: sectionTitle, systemImage: sectionIcon)

                if reqCount > 0 {
                    let percentage = Int((Double(completedReqCount) / Double(reqCount)) * 100)

                    HStack(spacing: 4) {
                        Circle()
                            .fill(percentage == 100 ? Color.green : Color.orange)
                            .frame(width: 8, height: 8)

                        Text("\(percentage)%")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(percentage == 100 ? .green : .orange)
                    }
                }

                headerTrailingContent
            }

            if selectedEntityClass != nil {
                // Toggle for showing only required fields
                if reqCount > 0 {
                    Toggle(isOn: $showOnlyRequired) {
                        HStack {
                            Text(AppStrings.Forms.requiredFieldsOnly)
                                .font(.subheadline)
                            Spacer()
                            Text("\(completedReqCount)/\(reqCount)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .tint(.blue)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                }

                // Attribute rows
                VStack(spacing: 12) {
                    ForEach(sortedProps, id: \.id) { classProperty in
                        let isCalculated = classProperty.type == "calculated"
                        let isTemperature = classProperty.type == "temperature"
                        let resolvedUnit: String? = {
                            if isTemperature {
                                return draftUnits[classProperty.id] ?? "\u{00B0}F"
                            } else if isCalculated {
                                return resolveCalculatedFieldUnit(classProperty)
                            }
                            return nil
                        }()

                        EntityAttributeRowView(
                            classProperty: classProperty,
                            currentValue: draftAttributes[classProperty.id] ?? "",
                            onValueChange: { newValue in
                                let previousValue = draftAttributes[classProperty.id]
                                draftAttributes[classProperty.id] = newValue
                                if isCalculated {
                                    // Only mark as overridden if user actually changed the value,
                                    // not when recalculation echoes the same value back
                                    if previousValue != newValue {
                                        overriddenFields.insert(classProperty.id)
                                    }
                                } else {
                                    onAttributeChanged?()
                                    onFieldValueChanged?(classProperty.id)
                                }
                            },
                            currentUnit: resolvedUnit,
                            onUnitChange: isTemperature ? { newUnit in
                                draftUnits[classProperty.id] = newUnit
                                onAttributeChanged?()
                            } : nil,
                            unitIsReadOnly: isCalculated,
                            isOverridden: overriddenFields.contains(classProperty.id),
                            onRefreshCalculation: isCalculated ? {
                                overriddenFields.remove(classProperty.id)
                                onRefreshCalculation?(classProperty.id)
                            } : nil,
                            attributeDescription: draftDescriptions[classProperty.id] ?? "",
                            onAttributeDescriptionChange: classProperty.allowDescription ? { newDesc in
                                let oldValue = draftDescriptions[classProperty.id] ?? ""
                                guard newDesc != oldValue else { return }
                                draftDescriptions[classProperty.id] = newDesc
                                onFieldDescriptionChanged?(classProperty.id)
                            } : nil
                        )
                    }
                }

                // Empty state for required fields filter
                if sortedProps.isEmpty && showOnlyRequired {
                    Text(AppStrings.Forms.noRequiredFields)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                }
            } else {
                // No entity class selected
                Text(AppStrings.Forms.noEntityAttributes("\(getEntityTypeName()) class defined"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            }
        }
    }

    private func getEntityTypeName() -> String {
        switch Entity.self {
        case is NodeV2.Type:
            return "node"
        case is EdgeV2.Type:
            return "edge"
        case is Issue.Type:
            return "issue"
        default:
            return "entity"
        }
    }
}

// MARK: - Convenience initializer when no header trailing content is needed
extension EntityCoreAttributesView where HeaderTrailing == EmptyView {
    init(
        entity: Entity?,
        selectedEntityClass: Entity.ClassType?,
        draftAttributes: Binding<[UUID: String]>,
        showOnlyRequired: Binding<Bool>,
        draftUnits: Binding<[UUID: String]> = .constant([:]),
        overriddenFields: Binding<Set<UUID>> = .constant([]),
        draftDescriptions: Binding<[UUID: String]> = .constant([:]),
        sectionTitle: String = AppStrings.Assets.coreAttributes,
        sectionIcon: String = "slider.horizontal.3",
        onAttributeChanged: (() -> Void)? = nil,
        onRefreshCalculation: ((UUID) -> Void)? = nil,
        onFieldValueChanged: ((UUID) -> Void)? = nil,
        onFieldDescriptionChanged: ((UUID) -> Void)? = nil
    ) {
        self.entity = entity
        self.selectedEntityClass = selectedEntityClass
        self._draftAttributes = draftAttributes
        self._showOnlyRequired = showOnlyRequired
        self._draftUnits = draftUnits
        self._overriddenFields = overriddenFields
        self._draftDescriptions = draftDescriptions
        self.sectionTitle = sectionTitle
        self.sectionIcon = sectionIcon
        self.headerTrailingContent = EmptyView()
        self.onAttributeChanged = onAttributeChanged
        self.onRefreshCalculation = onRefreshCalculation
        self.onFieldValueChanged = onFieldValueChanged
        self.onFieldDescriptionChanged = onFieldDescriptionChanged
    }
}
