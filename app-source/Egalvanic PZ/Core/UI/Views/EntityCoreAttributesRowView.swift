//
//  EntityAttributeRowView.swift
//  SwiftDataTutorial
//
//  Generic row view for displaying and editing individual entity attributes
//

import SwiftUI
import UIKit

struct EntityAttributeRowView<PropertyDef: EntityPropertyDefinition>: View {
    let classProperty: PropertyDef
    let currentValue: String
    let onValueChange: (String) -> Void

    // Unit support (for temperature and calculated fields)
    var currentUnit: String?
    var onUnitChange: ((String) -> Void)?
    var unitIsReadOnly: Bool = false

    // Calculated field override support
    var isOverridden: Bool = false
    var onRefreshCalculation: (() -> Void)?

    // Per-property description support
    var attributeDescription: String = ""
    var onAttributeDescriptionChange: ((String) -> Void)?

    @State private var localValue: String
    @State private var tableData: [String: String] = [:]
    @State private var isCustomMode: Bool = false
    @State private var showSelectSheet: Bool = false
    @FocusState private var isTextFieldFocused: Bool
    @State private var showDescriptionPopover: Bool = false
    @State private var localDescription: String
    @State private var showMultiSelectSheet: Bool = false

    // PERFORMANCE: Debounce timer to avoid propagating changes on every keystroke
    @State private var debounceTask: Task<Void, Never>?
    @State private var descriptionDebounceTask: Task<Void, Never>?

    init(
        classProperty: PropertyDef,
        currentValue: String,
        onValueChange: @escaping (String) -> Void,
        currentUnit: String? = nil,
        onUnitChange: ((String) -> Void)? = nil,
        unitIsReadOnly: Bool = false,
        isOverridden: Bool = false,
        onRefreshCalculation: (() -> Void)? = nil,
        attributeDescription: String = "",
        onAttributeDescriptionChange: ((String) -> Void)? = nil
    ) {
        self.classProperty = classProperty
        self.currentValue = currentValue
        self.onValueChange = onValueChange
        self.currentUnit = currentUnit
        self.onUnitChange = onUnitChange
        self.unitIsReadOnly = unitIsReadOnly
        self.isOverridden = isOverridden
        self.onRefreshCalculation = onRefreshCalculation
        self.attributeDescription = attributeDescription
        self.onAttributeDescriptionChange = onAttributeDescriptionChange
        self._localValue = State(initialValue: currentValue)
        self._localDescription = State(initialValue: attributeDescription)

        // Determine if current value is a custom value (not in options list) for select types
        if classProperty.type == "select" || classProperty.type == "dropdown" {
            let options = classProperty.options.compactMap { $0 }
            let isCustom = !currentValue.isEmpty && !options.contains(currentValue)
            self._isCustomMode = State(initialValue: isCustom)
        }

        // Initialize table data from JSON if it's a table type
        if classProperty.type == "table_with_column_headers" || classProperty.type == "table_with_row_headers" {
            var initialTableData: [String: String] = [:]

            if !currentValue.isEmpty,
               let data = currentValue.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: String] {
                initialTableData = json
            }

            self._tableData = State(initialValue: initialTableData)
        }
    }

    // PERFORMANCE: Debounced value change to avoid propagating on every keystroke
    private func debouncedValueChange(_ newValue: String) {
        debounceTask?.cancel()
        debounceTask = Task {
            try? await Task.sleep(nanoseconds: 150_000_000) // 150ms debounce
            if !Task.isCancelled {
                await MainActor.run {
                    onValueChange(newValue)
                }
            }
        }
    }

    // PERFORMANCE: Immediate value change for select/dropdown (no debounce needed)
    private func immediateValueChange(_ newValue: String) {
        debounceTask?.cancel()
        onValueChange(newValue)
    }

    /// Dismiss the keyboard so it doesn't reappear after a sheet is closed.
    private func dismissKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }

    @ViewBuilder
    private var afRequiredBadge: some View {
        // Only show for required fields
        if classProperty.af_required {
            let isEmpty: Bool = {
                switch classProperty.type {
                case "table_with_column_headers", "table_with_row_headers":
                    return tableData.values.allSatisfy { $0.isEmpty }
                default:
                    return localValue.isEmpty
                }
            }()

            Image(systemName: isEmpty
                  ? "exclamationmark.circle.fill"
                  : "checkmark.circle.fill")
                .font(.caption2)
                .foregroundColor(isEmpty ? .red : .green)
        }
    }

    @ViewBuilder
    private var descriptionInfoButton: some View {
        if let desc = classProperty.fieldDescription, !desc.isEmpty {
            Button(action: {
                showDescriptionPopover = true
            }) {
                Image(systemName: "info.circle")
                    .font(.caption2)
                    .foregroundColor(.blue)
            }
            .buttonStyle(.borderless)
            .popover(isPresented: $showDescriptionPopover) {
                Text(desc)
                    .font(.system(.subheadline, design: .rounded))
                    .padding()
                    .frame(width: 250)
                    .presentationCompactAdaptation(.popover)
            }
        }
    }

    private func updateTableValue() {
        // Convert table data to JSON string
        if let jsonData = try? JSONSerialization.data(withJSONObject: tableData),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            // PERFORMANCE: Debounce table value changes (JSON serialization is expensive)
            debouncedValueChange(jsonString)
        }
    }

    var body: some View {
        Group {
            switch classProperty.type {
            case "text", "string", "textfield":
                textFieldView

            case "number", "integer":
                numberFieldView

            case "decimal", "float":
                decimalFieldView

            case "temperature":
                temperatureFieldView

            case "select", "dropdown":
                selectFieldView

            case "boolean", "bool":
                booleanFieldView

            case "date":
                dateFieldView

            case "table_with_column_headers":
                tableWithColumnHeadersView

            case "table_with_row_headers":
                tableWithRowHeadersView

            case "multi_select":
                multiSelectFieldView

            case "calculated":
                calculatedFieldView

            default:
                textFieldView // Fallback to text field
            }

            // Per-property description field (when allowed and handler provided)
            if classProperty.allowDescription, onAttributeDescriptionChange != nil {
                VStack(alignment: .leading, spacing: 4) {
                    Text(AppStrings.Common.description)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    TextField(AppStrings.AssetsExtra.addNotesAboutAsset, text: $localDescription)
                        .font(.subheadline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .onChange(of: localDescription) { _, newValue in
                            descriptionDebounceTask?.cancel()
                            descriptionDebounceTask = Task {
                                try? await Task.sleep(nanoseconds: 150_000_000)
                                if !Task.isCancelled {
                                    await MainActor.run {
                                        onAttributeDescriptionChange?(newValue)
                                    }
                                }
                            }
                        }
                }
            }
        }
        // Sync from external value changes (e.g. voice input)
        .onChange(of: currentValue) { _, newValue in
            if localValue != newValue {
                localValue = newValue
                // For select fields, check if the new value requires switching modes
                if classProperty.type == "select" || classProperty.type == "dropdown" {
                    let options = classProperty.options.compactMap { $0 }
                    isCustomMode = !newValue.isEmpty && !options.contains(newValue)
                }
            }
        }
        // Sync from external description changes (e.g. auto-fill)
        .onChange(of: attributeDescription) { _, newValue in
            if localDescription != newValue {
                localDescription = newValue
            }
        }
    }

    // MARK: - Field Type Views

    private var textFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            TextField(AppStrings.Forms.enterField(classProperty.name.lowercased()), text: $localValue)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .onChange(of: localValue) { _, newValue in
                    // PERFORMANCE: Debounce to avoid re-rendering entire form on every keystroke
                    debouncedValueChange(newValue)
                }
        }
    }

    private var numberFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            TextField(AppStrings.Forms.enterNumber, text: $localValue)
                .keyboardType(.numberPad)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .onChange(of: localValue) { _, newValue in
                    if newValue.isEmpty || newValue.allSatisfy(\.isNumber) {
                        // PERFORMANCE: Debounce to avoid re-rendering entire form on every keystroke
                        debouncedValueChange(newValue)
                    }
                }
        }
    }

    private var decimalFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            TextField(AppStrings.Forms.enterDecimal, text: $localValue)
                .keyboardType(.decimalPad)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .onChange(of: localValue) { _, newValue in
                    let parts = newValue.split(separator: ".")
                    if newValue.isEmpty ||
                       (parts.count <= 2 && parts.allSatisfy { $0.allSatisfy(\.isNumber) }) {
                        // PERFORMANCE: Debounce to avoid re-rendering entire form on every keystroke
                        debouncedValueChange(newValue)
                    }
                }
        }
    }

    // MARK: - Temperature Field (decimal input + F/C unit picker)

    private var temperatureFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            HStack(spacing: 8) {
                TextField(AppStrings.Forms.enterDecimal, text: $localValue)
                    .keyboardType(.decimalPad)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .onChange(of: localValue) { _, newValue in
                        // Allow negative values, decimals
                        let cleaned = newValue.replacingOccurrences(of: " ", with: "")
                        let isValid = cleaned.isEmpty || {
                            let withoutLeadingMinus = cleaned.hasPrefix("-") ? String(cleaned.dropFirst()) : cleaned
                            let parts = withoutLeadingMinus.split(separator: ".")
                            return parts.count <= 2 && parts.allSatisfy { $0.allSatisfy(\.isNumber) }
                        }()
                        if isValid {
                            debouncedValueChange(newValue)
                        }
                    }

                Picker("", selection: Binding(
                    get: { currentUnit ?? "\u{00B0}F" },
                    set: { newUnit in onUnitChange?(newUnit) }
                )) {
                    Text("\u{00B0}F").tag("\u{00B0}F")
                    Text("\u{00B0}C").tag("\u{00B0}C")
                }
                .pickerStyle(.segmented)
                .frame(width: 100)
            }
        }
    }

    private var selectFieldView: some View {
        let options = classProperty.options.compactMap { $0 }

        return VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            if isCustomMode {
                // Custom text input mode
                HStack {
                    TextField(AppStrings.Forms.enterCustomValue, text: $localValue)
                        .focused($isTextFieldFocused)
                        .onChange(of: localValue) { _, newValue in
                            debouncedValueChange(newValue)
                        }

                    // Clear button - clears and returns to select mode
                    Button(action: {
                        localValue = ""
                        immediateValueChange("")
                        isCustomMode = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                            .font(.caption)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            } else {
                // Select mode with sheet picker (avoids Menu lag with many options)
                HStack {
                    Text(localValue.isEmpty ? AppStrings.Forms.selectPlaceholder : localValue)
                        .foregroundColor(localValue.isEmpty ? .secondary : .primary)

                    Spacer()

                    // Clear button (only when value exists)
                    if !localValue.isEmpty {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                            .font(.body)
                            .frame(width: 30, height: 30)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                localValue = ""
                                immediateValueChange("")
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
                    dismissKeyboard()
                    showSelectSheet = true
                }
                .sheet(isPresented: $showSelectSheet) {
                    NavigationStack {
                        List {
                            ForEach(options, id: \.self) { option in
                                Button {
                                    localValue = option
                                    immediateValueChange(option)
                                    showSelectSheet = false
                                } label: {
                                    HStack {
                                        Text(option)
                                            .foregroundColor(.primary)
                                        Spacer()
                                        if localValue == option {
                                            Image(systemName: "checkmark")
                                                .foregroundColor(.blue)
                                        }
                                    }
                                }
                            }

                            // Custom option
                            Button {
                                isCustomMode = true
                                localValue = ""
                                immediateValueChange("")
                                showSelectSheet = false
                            } label: {
                                HStack {
                                    Text(AppStrings.Forms.custom)
                                        .italic()
                                        .foregroundColor(.primary)
                                    Image(systemName: "pencil")
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .navigationTitle(classProperty.name)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button(AppStrings.Common.cancel) {
                                    showSelectSheet = false
                                }
                            }
                        }
                    }
                    .presentationDetents([.medium, .large])
                }
            }
        }
    }

    private var booleanFieldView: some View {
        Toggle(isOn: Binding(
            get: { localValue.lowercased() == "true" },
            set: {
                localValue = $0 ? "true" : "false"
                // PERFORMANCE: Immediate for toggle (should be responsive)
                immediateValueChange(localValue)
            }
        )) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }
        }
    }

    private var dateFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            DatePicker(
                "",
                selection: Binding(
                    get: {
                        ISO8601DateFormatter().date(from: localValue) ?? Date()
                    },
                    set: { newDate in
                        localValue = ISO8601DateFormatter().string(from: newDate)
                        // PERFORMANCE: Immediate for date picker (should be responsive)
                        immediateValueChange(localValue)
                    }
                ),
                displayedComponents: .date
            )
            .datePickerStyle(.compact)
        }
    }

    private var tableWithColumnHeadersView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            // Table with column headers
            VStack(spacing: 0) {
                // Header row
                HStack(spacing: 1) {
                    ForEach(classProperty.columns?.compactMap { $0 } ?? [], id: \.self) { column in
                        Text(column)
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(Color(.systemGray5))
                    }
                }

                // Data row
                HStack(spacing: 1) {
                    ForEach(Array((classProperty.columns?.compactMap { $0 } ?? []).enumerated()), id: \.offset) { index, column in
                        let internalType = (classProperty.internal_type?.count ?? 0) > index
                            ? classProperty.internal_type?[index] ?? "text"
                            : "text"

                        let keyboardType: UIKeyboardType = getKeyboardType(for: internalType)

                        TextField("", text: Binding(
                            get: { tableData[column] ?? "" },
                            set: { newValue in
                                if validateInput(newValue, type: internalType) {
                                    tableData[column] = newValue
                                    updateTableValue()
                                }
                            }
                        ))
                        .keyboardType(keyboardType)
                        .multilineTextAlignment(.center)
                        .textFieldStyle(.plain)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 4)
                        .background(Color(.systemBackground))
                    }
                }
            }
            .overlay(
                Rectangle()
                    .stroke(Color(.systemGray4), lineWidth: 1)
            )
        }
    }

    private var tableWithRowHeadersView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            // Table with row headers
            VStack(spacing: 1) {
                ForEach(Array((classProperty.columns?.compactMap { $0 } ?? []).enumerated()), id: \.offset) { index, row in
                    let internalType = (classProperty.internal_type?.count ?? 0) > index
                        ? classProperty.internal_type?[index] ?? "text"
                        : "text"

                    let keyboardType: UIKeyboardType = getKeyboardType(for: internalType)

                    HStack(spacing: 1) {
                        // Row header
                        Text(row)
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                            .frame(width: 60, alignment: .leading)
                            .padding(.vertical, 8)
                            .padding(.horizontal, 8)
                            .background(Color(.systemGray5))

                        // Data cell
                        TextField("", text: Binding(
                            get: { tableData[row] ?? "" },
                            set: { newValue in
                                if validateInput(newValue, type: internalType) {
                                    tableData[row] = newValue
                                    updateTableValue()
                                }
                            }
                        ))
                        .keyboardType(keyboardType)
                        .textFieldStyle(.plain)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 8)
                        .background(Color(.systemBackground))
                    }
                }
            }
            .overlay(
                Rectangle()
                    .stroke(Color(.systemGray4), lineWidth: 1)
            )
        }
    }

    // MARK: - Multi-Select Field

    private var multiSelectFieldView: some View {
        let options = classProperty.options.compactMap { $0 }

        // Parse selected values from JSON array string
        let selectedValues: [String] = {
            guard !localValue.isEmpty,
                  let data = localValue.data(using: .utf8),
                  let arr = try? JSONSerialization.jsonObject(with: data) as? [String] else {
                return []
            }
            return arr
        }()

        return VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()
            }

            // Display selected items as chips
            HStack {
                if selectedValues.isEmpty {
                    Text(AppStrings.Common.selectOptions)
                        .foregroundColor(.secondary)
                } else {
                    // Wrap chips
                    FlowLayout(spacing: 4) {
                        ForEach(selectedValues, id: \.self) { val in
                            Text(val)
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.blue.opacity(0.15))
                                .foregroundColor(.blue)
                                .cornerRadius(6)
                        }
                    }
                }

                Spacer()

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
                dismissKeyboard()
                showMultiSelectSheet = true
            }
            .sheet(isPresented: $showMultiSelectSheet) {
                NavigationStack {
                    List {
                        ForEach(options, id: \.self) { option in
                            Button {
                                var current = selectedValues
                                if current.contains(option) {
                                    current.removeAll { $0 == option }
                                } else {
                                    current.append(option)
                                }
                                // Serialize back to JSON array string
                                if let data = try? JSONSerialization.data(withJSONObject: current),
                                   let str = String(data: data, encoding: .utf8) {
                                    localValue = str
                                    immediateValueChange(str)
                                }
                            } label: {
                                HStack {
                                    Text(option)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selectedValues.contains(option) {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle(classProperty.name)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") {
                                showMultiSelectSheet = false
                            }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
        }
    }

    // MARK: - Calculated Field (editable with override + refresh)

    private var calculatedFieldView: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: "function")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Text(classProperty.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
                descriptionInfoButton
                afRequiredBadge
                Spacer()

                // Read-only unit label for calculated fields with inherited unit
                if let unit = currentUnit, !unit.isEmpty {
                    Text(unit)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(.systemGray5))
                        .cornerRadius(4)
                }
            }

            HStack(spacing: 8) {
                TextField("Waiting for inputs...", text: $localValue)
                    .keyboardType(.decimalPad)
                    .foregroundColor(isOverridden ? .orange : .primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.systemGray5))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(
                                isOverridden ? Color.orange : Color(.systemGray3),
                                style: isOverridden
                                    ? StrokeStyle(lineWidth: 1)
                                    : StrokeStyle(lineWidth: 1, dash: [4])
                            )
                    )
                    .onChange(of: localValue) { _, newValue in
                        debouncedValueChange(newValue)
                    }

                // Refresh button (only visible when overridden)
                if isOverridden {
                    Button(action: {
                        onRefreshCalculation?()
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.body)
                            .foregroundColor(.orange)
                    }
                    .buttonStyle(.borderless)
                }
            }
        }
    }

    // MARK: - Helper Functions

    private func getKeyboardType(for internalType: String) -> UIKeyboardType {
        switch internalType {
        case "number", "integer":
            return .numberPad
        case "decimal", "float":
            return .decimalPad
        default:
            return .default
        }
    }

    private func validateInput(_ value: String, type: String) -> Bool {
        switch type {
        case "number", "integer":
            return value.isEmpty || value.allSatisfy(\.isNumber)
        case "decimal", "float":
            let parts = value.split(separator: ".")
            return value.isEmpty || (parts.count <= 2 && parts.allSatisfy { $0.allSatisfy(\.isNumber) })
        default:
            return true
        }
    }
}
