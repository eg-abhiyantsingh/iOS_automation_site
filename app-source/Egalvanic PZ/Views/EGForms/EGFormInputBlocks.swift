//
//  EGFormInputBlocks.swift
//  Egalvanic PZ
//
//  ZP-1723: interactive blocks for V2 form rendering — input, select,
//  multiselect, radio, switch, checkbox, button. Each block resolves its
//  data path via `EGBlockContent.key` walked under the parent dataPath,
//  and writes back into the form view model on change.
//
//  IMPORTANT — instant-snap pattern for interactive controls.
//
//  Every checkbox / radio / switch segment in here goes through two
//  helpers — `instantUpdate { ... }` around the data change, and
//  `.snapStateChanges(on:)` on the visual element. Together they
//  suppress *all* SwiftUI implicit animations on state changes for
//  these controls.
//
//  This is deliberate. Field inspectors are tapping these dozens of
//  times per form, and SwiftUI's default `.contentTransition` on SF
//  Symbols plus inherited animation context from container collapse
//  animations made checkmark toggles take a quarter to half a second
//  to visually update — feeling like the app froze. Snapping
//  instantly is much better UX here than any animation.
//
//  When adding a new interactive block: copy the pattern. Do not
//  remove these modifiers thinking "iOS toggles look better animated"
//  — they do not, for forms.
//

import SwiftUI
import UIKit

// MARK: - Debounced text input
//
// Every text keystroke that writes straight to the view model causes
// the entire form to re-evaluate visibility / calc expressions because
// `formData` is `@Observable`. For a non-trivial form that means each
// character pays the cost of 50+ dispatcher bodies re-rendering.
//
// We use a UIKit-backed UITextField wrapped via UIViewRepresentable
// rather than SwiftUI's TextField. Why: SwiftUI's TextField re-evaluates
// its enclosing view's body on every keystroke (the @State + onChange
// pattern), and even a "thin wrapper" view body has measurable per-
// keystroke cost in a heavy form. UITextField talks to UIKit directly,
// debounces in its coordinator, and writes upstream once per 250 ms
// idle. Result: typing in a 30-cell data_table feels native.

enum EGTextFieldVisual { case plain, roundedBorder }

struct EGDebouncedTextField: UIViewRepresentable {
    @Binding var upstream: String
    let placeholder: String
    var keyboard: UIKeyboardType = .default
    var visual: EGTextFieldVisual = .plain
    var debounce: TimeInterval = 0.25

    init(_ placeholder: String,
         text: Binding<String>,
         keyboard: UIKeyboardType = .default,
         visual: EGTextFieldVisual = .plain,
         debounce: TimeInterval = 0.25) {
        self.placeholder = placeholder
        self._upstream = text
        self.keyboard = keyboard
        self.visual = visual
        self.debounce = debounce
    }

    func makeUIView(context: Context) -> UITextField {
        let tf = UITextField()
        tf.text = upstream
        tf.placeholder = placeholder
        tf.keyboardType = keyboard
        tf.borderStyle = (visual == .roundedBorder) ? .roundedRect : .none
        tf.autocorrectionType = .no
        tf.autocapitalizationType = .none
        tf.spellCheckingType = .no
        tf.smartDashesType = .no
        tf.smartQuotesType = .no
        tf.font = .systemFont(ofSize: 15)
        // No clear button — the "X" that appears while editing shifts
        // the field's content area and reads as the row "expanding"
        // visually. Form fields don't need it; users can backspace.
        tf.clearButtonMode = .never

        // Keyboard dismiss bar. Required for the numeric keypad
        // (no system Return key) and a nice-to-have for every other
        // text input. Wired to a coordinator-owned selector so the
        // toolbar's Done button doesn't depend on the responder
        // chain accidentally hitting the right target.
        let bar = UIToolbar()
        bar.sizeToFit()
        bar.items = [
            UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil),
            UIBarButtonItem(barButtonSystemItem: .done,
                            target: context.coordinator,
                            action: #selector(Coordinator.dismissKeyboard)),
        ]
        tf.inputAccessoryView = bar
        tf.delegate = context.coordinator
        tf.addTarget(context.coordinator, action: #selector(Coordinator.changed(_:)), for: .editingChanged)
        // Width must come from the SwiftUI parent, not from the
        // UITextField's intrinsic content size — otherwise the cell
        // grows / shrinks as the user types. Low hugging priority
        // tells autolayout the field is happy to stretch; low
        // compression resistance lets it shrink past its content size
        // when the parent is narrower than the text.
        tf.setContentHuggingPriority(.defaultLow, for: .horizontal)
        tf.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        // Pin vertical sizing so an active TextField doesn't push the
        // row height around between focused / unfocused states.
        tf.setContentHuggingPriority(.required, for: .vertical)
        tf.setContentCompressionResistancePriority(.required, for: .vertical)
        return tf
    }

    func updateUIView(_ uiView: UITextField, context: Context) {
        // Avoid clobbering the user's active edit. We only push the
        // upstream value back into the field when the field isn't
        // currently the source of truth (i.e., no pending debounce
        // and the user isn't typing right now).
        if !context.coordinator.hasPendingPush, uiView.text != upstream {
            uiView.text = upstream
        }
        // Cheaper-to-update props don't need the guard.
        if uiView.placeholder != placeholder { uiView.placeholder = placeholder }
        if uiView.keyboardType != keyboard { uiView.keyboardType = keyboard }
        let target: UITextField.BorderStyle = (visual == .roundedBorder) ? .roundedRect : .none
        if uiView.borderStyle != target { uiView.borderStyle = target }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(upstream: $upstream, debounce: debounce)
    }

    final class Coordinator: NSObject, UITextFieldDelegate {
        @Binding var upstream: String
        let debounce: TimeInterval
        private var workItem: DispatchWorkItem?

        var hasPendingPush: Bool { workItem != nil }

        init(upstream: Binding<String>, debounce: TimeInterval) {
            self._upstream = upstream
            self.debounce = debounce
        }

        @objc func dismissKeyboard() {
            // Resign whichever field is first responder. Using the
            // shared application target rather than the text field
            // itself means this works for any input wired up to the
            // same accessory toolbar.
            UIApplication.shared.sendAction(
                #selector(UIResponder.resignFirstResponder),
                to: nil, from: nil, for: nil
            )
        }

        @objc func changed(_ tf: UITextField) {
            let text = tf.text ?? ""
            workItem?.cancel()
            let item = DispatchWorkItem { [weak self] in
                guard let self = self else { return }
                if self.upstream != text { self.upstream = text }
                self.workItem = nil
            }
            workItem = item
            DispatchQueue.main.asyncAfter(deadline: .now() + debounce, execute: item)
        }

        func textFieldDidEndEditing(_ textField: UITextField) {
            // Flush so a quick tap-away doesn't drop the last keystroke.
            workItem?.cancel()
            let text = textField.text ?? ""
            if upstream != text { upstream = text }
            workItem = nil
        }
    }
}

// MARK: - Instant-snap helpers (see file-level comment for rationale)

/// Run a state-changing block with all implicit SwiftUI animations
/// disabled. Use around `viewModel.setValue(...)` calls inside
/// interactive form controls so the UI snaps to the new state.
@MainActor
func instantUpdate(_ block: () -> Void) {
    var tx = Transaction()
    tx.disablesAnimations = true
    withTransaction(tx, block)
}

extension View {
    /// Suppress every implicit animation tied to `value` on this view.
    /// Layered: `.contentTransition(.identity)` kills SF Symbol's
    /// content-change cross-fade, `.animation(nil, value:)` cancels any
    /// state-driven animation pegged to this value, and the
    /// `.transaction { $0.animation = nil }` belt-and-suspenders clears
    /// any animation inherited from an ancestor (e.g. a container that
    /// collapses with `withAnimation`).
    func snapStateChanges<Value: Equatable>(on value: Value) -> some View {
        self
            .contentTransition(.identity)
            .animation(nil, value: value)
            .transaction { $0.animation = nil }
    }
}

// MARK: - Path helper

@MainActor
private func resolvedPath(parent: [String], content: EGBlockContent?) -> [String]? {
    guard let key = content?.key, !key.isEmpty else { return nil }
    return parent + [key]
}

// MARK: - input  (text / number / textarea / date — value as String)

struct EGInputBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let label = block.content?.label ?? "Field"
        let inputType = (block.content?.inputType ?? "text").lowercased()
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }

        let calcExpr = block.content?.calculated
        let canOverwrite = block.content?.can_overwrite ?? false
        let calcValue = viewModel.evalCalculated(calcExpr)
        let hasCalc = calcExpr != nil && calcValue != nil
        let isCalcReadOnly = hasCalc && !canOverwrite

        return AnyView(VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            HStack(spacing: 6) {
                Text(label).font(EGFormStyle.labelFont).foregroundStyle(.primary)
                if hasCalc {
                    Text("calc")
                        .font(.system(size: 10, weight: .semibold))
                        .padding(.horizontal, 4).padding(.vertical, 1)
                        .background(EGFormStyle.calcChipBg)
                        .foregroundStyle(EGFormStyle.calcChipFg)
                        .clipShape(Capsule())
                }
            }
            inputBody(path: path,
                      inputType: inputType,
                      calcDisplay: isCalcReadOnly ? stringify(calcValue) : nil)
                .disabled(readOnly || isCalcReadOnly)
        })
    }

    @ViewBuilder
    private func inputBody(path: [String], inputType: String, calcDisplay: String?) -> some View {
        let prefix = block.content?.prefix
        let suffix = block.content?.suffix
        let binding: Binding<String> = calcDisplay.map { v in
            // calc & no overwrite — display-only, no write-back
            Binding(get: { v }, set: { _ in })
        } ?? viewModel.stringBinding(at: path)

        switch inputType {
        case "textarea":
            // TextEditor doesn't honor `prompt:` and the debounced wrapper
            // is for single-line TextField — for textareas we accept the
            // direct binding write, which is rare enough not to matter.
            TextEditor(text: binding)
                .frame(minHeight: 80)
                .padding(6)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(.systemGray4), lineWidth: 1))
        case "number":
            HStack(spacing: 6) {
                if let prefix = prefix { Text(prefix).foregroundStyle(.secondary) }
                EGDebouncedTextField(block.content?.placeholder ?? "0",
                                     text: binding,
                                     keyboard: .decimalPad,
                                     visual: .roundedBorder)
                    .frame(height: 36)
                if let suffix = suffix { Text(suffix).foregroundStyle(.secondary) }
            }
        case "date":
            DatePicker("", selection: dateBinding(stringBinding: binding), displayedComponents: .date)
                .labelsHidden()
        default:
            HStack(spacing: 6) {
                if let prefix = prefix { Text(prefix).foregroundStyle(.secondary) }
                EGDebouncedTextField(block.content?.placeholder ?? (block.content?.label ?? "Value"),
                                     text: binding,
                                     visual: .roundedBorder)
                    .frame(height: 36)
                if let suffix = suffix { Text(suffix).foregroundStyle(.secondary) }
            }
        }
    }

    private func dateBinding(stringBinding: Binding<String>) -> Binding<Date> {
        let formatter = ISO8601DateFormatter()
        let dayOnly = DateFormatter()
        dayOnly.dateFormat = "yyyy-MM-dd"
        return Binding(
            get: {
                if let d = formatter.date(from: stringBinding.wrappedValue) { return d }
                if let d = dayOnly.date(from: stringBinding.wrappedValue) { return d }
                return Date()
            },
            set: { stringBinding.wrappedValue = dayOnly.string(from: $0) }
        )
    }
}

// MARK: - select  (single-choice from `options` or external `options_source`)

struct EGSelectBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        let label = block.content?.label ?? "Select"
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }
        let externalSource = block.content?.options_source
        let opts: [EGBlockOption] = externalSource.map {
            EGExternalOptions.resolve(source: $0, modelContext: modelContext)
        } ?? (block.content?.options ?? [])
        let current = EGExternalOptions.unwrap(viewModel.value(at: path))

        return AnyView(VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            Text(label).font(EGFormStyle.labelFont).foregroundStyle(.primary)
            Menu {
                Button("Select…") { instantUpdate { viewModel.setValue(nil, at: path) } }
                ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                    if let key = opt.key {
                        Button(opt.label ?? key) {
                            instantUpdate {
                                viewModel.setValue(EGExternalOptions.wrap(value: key, source: externalSource), at: path)
                            }
                        }
                    }
                }
            } label: {
                HStack {
                    Text(displayLabel(for: current, options: opts))
                        .foregroundStyle(current.isEmpty ? .secondary : .primary)
                    Spacer()
                    Image(systemName: "chevron.up.chevron.down").font(.caption)
                }
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(.systemGray4)))
            }
            .disabled(readOnly)
        })
    }

    private func displayLabel(for key: String, options: [EGBlockOption]) -> String {
        if key.isEmpty { return "Select…" }
        return options.first(where: { $0.key == key })?.label ?? key
    }
}

// MARK: - multiselect

struct EGMultiselectBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let label = block.content?.label ?? "Multi Select"
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }
        let opts = block.content?.options ?? []
        let displayMode = block.content?.display ?? "dropdown"
        let map = viewModel.multiselectMap(at: path)

        let toggle: (String) -> Void = { key in
            var next = map
            next[key] = !(map[key] ?? false)
            viewModel.setValue(next, at: path)
        }

        return AnyView(VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            Text(label).font(.caption).fontWeight(.medium)
            if displayMode == "checkbox_list" {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                        if let key = opt.key {
                            let checked = map[key] ?? false
                            Button {
                                if !readOnly { instantUpdate { toggle(key) } }
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: checked ? "checkmark.square.fill" : "square")
                                        .foregroundStyle(checked ? Color.accentColor : Color(.systemGray3))
                                        .snapStateChanges(on: checked)
                                    Text(opt.label ?? key)
                                        .foregroundStyle(.primary)
                                    Spacer()
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(readOnly)
                        }
                    }
                }
            } else {
                Menu {
                    ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                        if let key = opt.key {
                            Button {
                                toggle(key)
                            } label: {
                                Label(opt.label ?? key, systemImage: (map[key] ?? false) ? "checkmark" : "")
                            }
                        }
                    }
                } label: {
                    HStack {
                        Text(summary(opts: opts, map: map))
                            .foregroundStyle(.primary)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down").font(.caption)
                    }
                    .padding(8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(.systemGray4)))
                }
                .disabled(readOnly)
            }
        })
    }

    private func summary(opts: [EGBlockOption], map: [String: Bool]) -> String {
        let chosen = opts.compactMap { opt -> String? in
            guard let key = opt.key, map[key] == true else { return nil }
            return opt.label ?? key
        }
        return chosen.isEmpty ? "Select…" : chosen.joined(separator: ", ")
    }
}

// MARK: - radio

struct EGRadioBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let label = block.content?.label ?? "Radio"
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }
        let opts = block.content?.options ?? []
        let current = viewModel.stringValue(at: path)

        return AnyView(VStack(alignment: .leading, spacing: 6) {
            Text(label).font(EGFormStyle.labelFont).foregroundStyle(.primary)
            ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                if let key = opt.key {
                    let isActive = current == key
                    Button {
                        if !readOnly {
                            instantUpdate { viewModel.setValue(key, at: path) }
                        }
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: isActive ? "largecircle.fill.circle" : "circle")
                                .foregroundStyle(isActive ? Color.accentColor : Color(.systemGray3))
                                .snapStateChanges(on: isActive)
                            Text(opt.label ?? key)
                                .foregroundStyle(.primary)
                            Spacer()
                        }
                    }
                    .buttonStyle(.plain)
                    .disabled(readOnly)
                }
            }
        })
    }
}

// MARK: - switch  (segmented control style)

struct EGSwitchBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let label = block.content?.label ?? "Switch"
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }
        let opts = block.content?.options ?? []
        let current = viewModel.stringValue(at: path)

        return AnyView(VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            Text(label).font(.caption).fontWeight(.medium)
            HStack(spacing: 4) {
                ForEach(Array(opts.enumerated()), id: \.offset) { _, opt in
                    if let key = opt.key {
                        let isActive = current == key
                        Button {
                            if !readOnly {
                                instantUpdate { viewModel.setValue(key, at: path) }
                            }
                        } label: {
                            Text(opt.label ?? key)
                                .font(.caption)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 10).padding(.vertical, 6)
                                .frame(maxWidth: (block.content?.fullWidth ?? false) ? .infinity : nil)
                                .background(isActive ? activeColor(for: opt) : Color.clear)
                                .foregroundStyle(isActive ? Color.white : .primary)
                                .overlay(RoundedRectangle(cornerRadius: 6).stroke(isActive ? activeColor(for: opt) : Color(.systemGray4), lineWidth: 1))
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                                .snapStateChanges(on: isActive)
                        }
                        .buttonStyle(.plain)
                        .disabled(readOnly)
                    }
                }
            }
        })
    }

    private func activeColor(for opt: EGBlockOption) -> Color {
        // Option `color` from form definitions is normally a CSS hex
        // (e.g. "#4caf50"); occasionally an SF-native name. Parse hex
        // directly through the EGFormStyle helper so every authored
        // color comes through faithfully — the previous version only
        // matched a small allow-list of preset hex strings and fell
        // through to accent color for everything else (notably the
        // Material green/red used by pass/fail verdicts).
        let raw = (opt.color ?? "").trimmingCharacters(in: .whitespaces)
        guard !raw.isEmpty else { return .accentColor }
        if raw.hasPrefix("#") { return Color(hex: raw) }
        switch raw.lowercased() {
        case "green":  return .green
        case "red":    return .red
        case "yellow": return .yellow
        case "orange": return .orange
        case "blue":   return .blue
        default:       return .accentColor
        }
    }
}

// MARK: - checkbox

struct EGCheckboxBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let label = block.content?.label ?? "Checkbox"
        guard let path = resolvedPath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }

        let calcExpr = block.content?.calculated
        let canOverwrite = block.content?.can_overwrite ?? false
        let calcValue = viewModel.evalCalculated(calcExpr)
        let hasCalc = calcExpr != nil && calcValue != nil
        let isCalcReadOnly = hasCalc && !canOverwrite

        let checked: Bool = {
            if isCalcReadOnly { return (calcValue as? Bool) ?? false }
            return viewModel.boolValue(at: path)
        }()

        let binding = Binding<Bool>(
            get: { checked },
            set: { newValue in
                if !readOnly && !isCalcReadOnly {
                    viewModel.setValue(newValue, at: path)
                }
            }
        )

        let isDisabled = readOnly || isCalcReadOnly

        return AnyView(
            Button {
                if !isDisabled {
                    instantUpdate { binding.wrappedValue.toggle() }
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: checked ? "checkmark.square.fill" : "square")
                        .foregroundStyle(checked ? Color.accentColor : Color(.systemGray3))
                        .snapStateChanges(on: checked)
                    Text(label).foregroundStyle(.primary)
                    if hasCalc {
                        Text("calc")
                            .font(.system(size: 10, weight: .semibold))
                            .padding(.horizontal, 4).padding(.vertical, 1)
                            .background(EGFormStyle.calcChipBg)
                            .foregroundStyle(EGFormStyle.calcChipFg)
                            .clipShape(Capsule())
                    }
                    Spacer()
                }
            }
            .buttonStyle(.plain)
            .disabled(isDisabled)
        )
    }
}

// MARK: - button (action — for now, only "submit" semantics surface)

struct EGButtonBlockView: View {
    let block: EGBlock
    let onSubmit: () -> Void

    var body: some View {
        Button(action: onSubmit) {
            Text(block.content?.label ?? "Submit")
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
        }
        .buttonStyle(.borderedProminent)
    }
}

// data_table lives in its own file — see EGFormDataTableBlock.swift.

// MARK: - shared helpers

struct MissingKeyHint: View {
    let label: String
    var body: some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(.orange)
            Text("\(label): missing `key` in form definition")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(8)
        .background(Color.orange.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

private func stringify(_ v: Any?) -> String {
    guard let v = v else { return "" }
    if let s = v as? String { return s }
    if let n = v as? NSNumber { return n.stringValue }
    if let b = v as? Bool { return b ? "true" : "false" }
    return "\(v)"
}
