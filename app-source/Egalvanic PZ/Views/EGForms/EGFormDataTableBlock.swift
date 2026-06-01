//
//  EGFormDataTableBlock.swift
//  Egalvanic PZ
//
//  ZP-1723: the `data_table` block — workhorse for 70B test readings.
//  Four layouts, all mapped from the web V2 renderer:
//
//    • key_value — list of (row_heading.label → cell) pairs. Row
//      headings carry the field_type. One cell per row.
//    • row      — row-major matrix. Each row_heading is a row, with
//      `data_columns` (default 3) identical cells across. Cells under
//      a row share the row_heading's field_type.
//    • hybrid   — full matrix. row_headings × column_headings. Cell
//      field_type comes from the column_heading.
//    • column   — default. Each row is a record. Columns defined by
//      column_headings (each owns its field_type). Row count is fixed
//      via `rows` (default 3) or dynamic via `dynamic_rows`.
//
//  Cell data lives under the block's `key` as a flat dict — every
//  cell has its own composite data key:
//    • key_value:  rh.key
//    • row:        "{rh.key}_{colIndex}"
//    • hybrid:     "{rh.key}_{ch.key}"
//    • column:     "{ch.key}_{rowIndex}"
//
//  Same composite keying as the web renderer so submissions
//  round-trip with web byte-for-byte.
//

import SwiftUI

struct EGFormDataTableBlock: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel
    @Environment(\.modelContext) private var modelContext

    private var content: EGBlockContent? { block.content }
    private var tableType: String { content?.table_type ?? "column" }
    private var basePath: [String]? {
        guard let key = content?.key, !key.isEmpty else { return nil }
        return dataPath + [key]
    }

    var body: some View {
        if let label = content?.label, !label.isEmpty {
            VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
                Text(label).font(EGFormStyle.labelFont)
                tableBody
            }
        } else {
            tableBody
        }
    }

    // MARK: - Layout dispatch

    @ViewBuilder
    private var tableBody: some View {
        if let basePath = basePath {
            switch tableType {
            case "key_value": keyValueLayout(basePath: basePath)
            case "row":       rowLayout(basePath: basePath)
            case "hybrid":    hybridLayout(basePath: basePath)
            default:          columnLayout(basePath: basePath)
            }
        } else {
            MissingKeyHint(label: content?.label ?? "Data Table")
        }
    }

    // MARK: - key_value

    @ViewBuilder
    private func keyValueLayout(basePath: [String]) -> some View {
        let rows = content?.row_headings ?? []
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.offset) { idx, rh in
                if let key = rh.key {
                    HStack(spacing: 0) {
                        Text(rh.label ?? key)
                            .font(EGFormStyle.bodyFont)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 10).padding(.vertical, 8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .frame(width: 160, alignment: .leading)
                            .background(EGFormStyle.panelHdrBg)
                        Divider()
                        cell(heading: rh, dataKey: key, basePath: basePath)
                            .frame(maxWidth: .infinity)
                    }
                    if idx < rows.count - 1 { Divider() }
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(EGFormStyle.panelBorder, lineWidth: 1))
    }

    // MARK: - row (row-major, data_columns wide)

    @ViewBuilder
    private func rowLayout(basePath: [String]) -> some View {
        let rows = content?.row_headings ?? []
        let cols = max(1, content?.data_columns ?? 3)
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Color.clear.frame(width: 160)
                    Divider()
                    ForEach(0..<cols, id: \.self) { ci in
                        if ci > 0 { Divider() }
                        Text("\(ci + 1)")
                            .font(EGFormStyle.smallStrong)
                            .padding(.horizontal, 10).padding(.vertical, 8)
                            .frame(width: cellWidth)
                            .background(EGFormStyle.panelHdrBg)
                    }
                }
                .frame(height: 36)
                Divider()
                ForEach(Array(rows.enumerated()), id: \.offset) { idx, rh in
                    if let rhKey = rh.key {
                        HStack(spacing: 0) {
                            Text(rh.label ?? rhKey)
                                .font(EGFormStyle.bodyFont)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 10).padding(.vertical, 8)
                                .frame(width: 160, alignment: .leading)
                                .background(EGFormStyle.panelHdrBg)
                            Divider()
                            ForEach(0..<cols, id: \.self) { ci in
                                if ci > 0 { Divider() }
                                cell(heading: rh, dataKey: "\(rhKey)_\(ci)", basePath: basePath)
                                    .frame(width: cellWidth)
                            }
                        }
                        if idx < rows.count - 1 { Divider() }
                    }
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(EGFormStyle.panelBorder, lineWidth: 1))
    }

    // MARK: - hybrid (row × column matrix)

    @ViewBuilder
    private func hybridLayout(basePath: [String]) -> some View {
        let rows = content?.row_headings ?? []
        let cols = content?.column_headings ?? []
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Color.clear.frame(width: 160)
                    Divider()
                    ForEach(Array(cols.enumerated()), id: \.offset) { ci, ch in
                        if let chKey = ch.key {
                            if ci > 0 { Divider() }
                            Text(ch.label ?? chKey)
                                .font(EGFormStyle.smallStrong)
                                .padding(.horizontal, 10).padding(.vertical, 8)
                                .frame(width: cellWidth)
                                .background(EGFormStyle.panelHdrBg)
                        }
                    }
                }
                .frame(height: 36)
                Divider()
                ForEach(Array(rows.enumerated()), id: \.offset) { idx, rh in
                    if let rhKey = rh.key {
                        HStack(spacing: 0) {
                            Text(rh.label ?? rhKey)
                                .font(EGFormStyle.bodyFont)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 10).padding(.vertical, 8)
                                .frame(width: 160, alignment: .leading)
                                .background(EGFormStyle.panelHdrBg)
                            Divider()
                            ForEach(Array(cols.enumerated()), id: \.offset) { ci, ch in
                                if let chKey = ch.key {
                                    if ci > 0 { Divider() }
                                    cell(heading: ch, dataKey: "\(rhKey)_\(chKey)", basePath: basePath)
                                        .frame(width: cellWidth)
                                }
                            }
                        }
                        if idx < rows.count - 1 { Divider() }
                    }
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(EGFormStyle.panelBorder, lineWidth: 1))
    }

    // MARK: - column (default — records-by-column)

    @ViewBuilder
    private func columnLayout(basePath: [String]) -> some View {
        let cols = content?.column_headings ?? []
        let rowCount = effectiveRowCount(cols: cols, basePath: basePath)
        let canAdd = (content?.dynamic_rows == true) && !readOnly

        ScrollView(.horizontal, showsIndicators: false) {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(Array(cols.enumerated()), id: \.offset) { ci, ch in
                        if let chKey = ch.key {
                            if ci > 0 { Divider() }
                            Text(ch.label ?? chKey)
                                .font(EGFormStyle.smallStrong)
                                .padding(.horizontal, 10).padding(.vertical, 8)
                                .frame(width: cellWidth)
                                .background(EGFormStyle.panelHdrBg)
                        }
                    }
                }
                .frame(height: 36)
                Divider()
                ForEach(0..<rowCount, id: \.self) { ri in
                    HStack(spacing: 0) {
                        ForEach(Array(cols.enumerated()), id: \.offset) { ci, ch in
                            if let chKey = ch.key {
                                if ci > 0 { Divider() }
                                cell(heading: ch, dataKey: "\(chKey)_\(ri)", basePath: basePath)
                                    .frame(width: cellWidth)
                            }
                        }
                    }
                    if ri < rowCount - 1 { Divider() }
                }
                if canAdd {
                    Divider()
                    Button {
                        instantUpdate { addDynamicRow(cols: cols, rowCount: rowCount, basePath: basePath) }
                    } label: {
                        Text("+ Add Row")
                            .font(EGFormStyle.smallStrong)
                            .foregroundStyle(Color.accentColor)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color(.systemGray6))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(EGFormStyle.panelBorder, lineWidth: 1))
    }

    private func effectiveRowCount(cols: [EGTableHeading], basePath: [String]) -> Int {
        if content?.dynamic_rows == true {
            // Web inspects existing keys and finds the max `_<N>` suffix.
            // Mirror that so a previously-saved dynamic table comes back
            // with the same number of rows.
            let map = (viewModel.value(at: basePath) as? [String: Any]) ?? [:]
            let suffixes: [Int] = map.keys.compactMap { key in
                guard let underscore = key.lastIndex(of: "_") else { return nil }
                return Int(key[key.index(after: underscore)...])
            }
            return max((suffixes.max() ?? 0) + 1, 1)
        }
        return max(content?.rows ?? 3, 1)
    }

    private func addDynamicRow(cols: [EGTableHeading], rowCount: Int, basePath: [String]) {
        // Seed empty values for every column at the next index so the
        // row materializes in the UI even before the user types.
        for ch in cols {
            if let chKey = ch.key {
                viewModel.setValue("", at: basePath + ["\(chKey)_\(rowCount)"])
            }
        }
    }

    private let cellWidth: CGFloat = 140

    // MARK: - cell dispatch

    @ViewBuilder
    private func cell(heading: EGTableHeading, dataKey: String, basePath: [String]) -> some View {
        let cellPath = basePath + [dataKey]
        let calcExpr = heading.calculated
        let canOverwrite = heading.can_overwrite ?? false
        let calcValue = viewModel.evalCalculated(calcExpr)
        let hasCalc = calcExpr != nil && calcValue != nil
        let isCalcReadOnly = hasCalc && !canOverwrite
        let isDisabled = readOnly || isCalcReadOnly

        let bg: Color = isCalcReadOnly
            ? EGFormStyle.calcChipBg.opacity(0.45)
            : (readOnly ? Color(.systemGray6) : Color.clear)

        Group {
            switch (heading.field_type ?? "text").lowercased() {
            case "checkbox":
                cellCheckbox(path: cellPath, disabled: isDisabled,
                             calcValue: isCalcReadOnly ? (calcValue as? Bool) : nil)
            case "select":
                cellSelect(path: cellPath, opts: heading.options ?? [],
                           source: heading.options_source, disabled: isDisabled)
            case "switch":
                cellSwitch(path: cellPath, opts: heading.options ?? [], disabled: isDisabled)
            case "number":
                cellText(path: cellPath, keyboard: .decimalPad, disabled: isDisabled,
                         calcDisplay: isCalcReadOnly ? stringify(calcValue) : nil)
            case "date":
                cellDate(path: cellPath, disabled: isDisabled)
            default:
                cellText(path: cellPath, keyboard: .default, disabled: isDisabled,
                         calcDisplay: isCalcReadOnly ? stringify(calcValue) : nil)
            }
        }
        .frame(height: 40)
        .padding(.horizontal, 4)
        .background(bg)
    }

    // MARK: - cell variants

    @ViewBuilder
    private func cellText(path: [String], keyboard: UIKeyboardType,
                          disabled: Bool, calcDisplay: String?) -> some View {
        // Calc-locked cells: read-only, display the formula result.
        // Editable cells: debounced — typing in a 30-row table without
        // this would re-render the whole form on every keystroke.
        if let v = calcDisplay {
            Text(v)
                .font(EGFormStyle.bodyFont)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 4)
        } else {
            EGDebouncedTextField("", text: viewModel.stringBinding(at: path),
                                 keyboard: keyboard, visual: .plain)
                .padding(.horizontal, 4)
                .allowsHitTesting(!disabled)
                .opacity(disabled ? 0.5 : 1)
        }
    }

    @ViewBuilder
    private func cellCheckbox(path: [String], disabled: Bool, calcValue: Bool?) -> some View {
        let checked = calcValue ?? viewModel.boolValue(at: path)
        Button {
            if !disabled {
                instantUpdate { viewModel.setValue(!checked, at: path) }
            }
        } label: {
            Image(systemName: checked ? "checkmark.square.fill" : "square")
                .foregroundStyle(checked ? Color.accentColor : Color(.systemGray3))
                .snapStateChanges(on: checked)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }

    @ViewBuilder
    private func cellSelect(path: [String], opts inlineOpts: [EGBlockOption],
                            source: String?, disabled: Bool) -> some View {
        let opts: [EGBlockOption] = source.map {
            EGExternalOptions.resolve(source: $0, modelContext: modelContext)
        } ?? inlineOpts
        let current = EGExternalOptions.unwrap(viewModel.value(at: path))
        Menu {
            Button("—") { instantUpdate { viewModel.setValue(nil, at: path) } }
            ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                if let key = opt.key {
                    Button(opt.label ?? key) {
                        instantUpdate {
                            viewModel.setValue(EGExternalOptions.wrap(value: key, source: source), at: path)
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(displayLabel(for: current, options: opts))
                    .font(EGFormStyle.bodyFont)
                    .foregroundStyle(current.isEmpty ? .secondary : .primary)
                    .lineLimit(1)
                Spacer(minLength: 0)
                Image(systemName: "chevron.down")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 6)
        }
        .disabled(disabled)
    }

    @ViewBuilder
    private func cellSwitch(path: [String], opts: [EGBlockOption], disabled: Bool) -> some View {
        let current = viewModel.stringValue(at: path)
        HStack(spacing: 2) {
            ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                if let key = opt.key {
                    let isActive = current == key
                    Button {
                        if !disabled {
                            instantUpdate { viewModel.setValue(key, at: path) }
                        }
                    } label: {
                        Text(opt.label ?? key)
                            .font(.system(size: 11, weight: .semibold))
                            .padding(.horizontal, 6).padding(.vertical, 3)
                            .background(isActive ? Color.accentColor : Color.clear)
                            .foregroundStyle(isActive ? .white : .primary)
                            .overlay(
                                RoundedRectangle(cornerRadius: 4)
                                    .stroke(isActive ? Color.accentColor : Color(.systemGray4), lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                            .snapStateChanges(on: isActive)
                    }
                    .buttonStyle(.plain)
                    .disabled(disabled)
                }
            }
        }
    }

    @ViewBuilder
    private func cellDate(path: [String], disabled: Bool) -> some View {
        let binding = viewModel.stringBinding(at: path)
        DatePicker("", selection: dateBinding(from: binding), displayedComponents: .date)
            .labelsHidden()
            .scaleEffect(0.85)
            .disabled(disabled)
    }

    // MARK: - helpers

    private func displayLabel(for key: String, options: [EGBlockOption]) -> String {
        if key.isEmpty { return "—" }
        return options.first(where: { $0.key == key })?.label ?? key
    }

    private func dateBinding(from string: Binding<String>) -> Binding<Date> {
        let day = DateFormatter()
        day.dateFormat = "yyyy-MM-dd"
        let iso = ISO8601DateFormatter()
        return Binding(
            get: {
                if let d = day.date(from: string.wrappedValue) { return d }
                if let d = iso.date(from: string.wrappedValue) { return d }
                return Date()
            },
            set: { string.wrappedValue = day.string(from: $0) }
        )
    }

    private func stringify(_ v: Any?) -> String {
        guard let v = v else { return "" }
        if let s = v as? String { return s }
        if let n = v as? NSNumber { return n.stringValue }
        if let b = v as? Bool { return b ? "true" : "false" }
        return "\(v)"
    }
}
