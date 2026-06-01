//
//  CommonUIComponents.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/7/25.
//

import SwiftUI
import UIKit

struct BigBottomButton: View {
    let title: String
    let action: () -> Void
    var isDisabled: Bool = false
    var isLoading: Bool = false
    var loadingText: String? = nil
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isDisabled || isLoading)
        .padding()
        .overlay {
            if isLoading {
                HStack(spacing: 8) {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .scaleEffect(0.8)
                    if let loadingText = loadingText {
                        Text(loadingText)
                            .font(.callout)
                    }
                }
                .padding(8)
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
            }
        }
    }
}

struct DetailSection: View {
    let title: String
    let icon: String
    var spacing: CGFloat = 12
    var cardStyle: CardStyle = .elevated
    @ViewBuilder let content: () -> AnyView  // Force AnyView to simplify type inference
    
    enum CardStyle {
        case elevated
        case flat
        case bordered
        
        var backgroundColor: Color {
            switch self {
            case .elevated, .bordered:
                return Color(.systemBackground)
            case .flat:
                return Color(.secondarySystemBackground)
            }
        }
        
        var shadowRadius: CGFloat {
            switch self {
            case .elevated: return 10
            case .flat, .bordered: return 0
            }
        }
        
        var shadowOpacity: Double {
            switch self {
            case .elevated: return 0.05
            case .flat, .bordered: return 0
            }
        }
        
        var borderColor: Color? {
            switch self {
            case .bordered: return Color(.systemGray4)
            default: return nil
            }
        }
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Header
            HStack {
                Label(title, systemImage: icon)
                    .font(.headline)
                    .foregroundColor(.primary)
                Spacer()
            }
            
            // Content
            VStack(spacing: spacing) {
                content()
            }
        }
        .padding()
        .background(cardStyle.backgroundColor)
        .cornerRadius(16)
        .overlay(
            cardStyle.borderColor.map { color in
                RoundedRectangle(cornerRadius: 16)
                    .stroke(color, lineWidth: 1)
            }
        )
        .shadow(
            color: .black.opacity(cardStyle.shadowOpacity),
            radius: cardStyle.shadowRadius,
            y: 5
        )
    }
}

struct SectionHeader: View {
    let title: String
    let systemImage: String

    var body: some View {
        HStack {
            // ZP-2161: passing ``systemImage: ""`` drops the icon and
            // shrinks to a subheadline subtitle — used by the renamed
            // "Custom Attributes" header inside the engineering card.
            if systemImage.isEmpty {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
            } else {
                Label(title, systemImage: systemImage)
                    .font(.headline)
                    .foregroundColor(.primary)
            }
            Spacer()
        }
    }
}

struct ModernTextField: View {
    let title: String
    @Binding var text: String
    let icon: String
    var trailingContent: (() -> AnyView)? = nil
    /// Keyboard hint passed through to the inner ``TextField``. Default
    /// stays ``.default`` so the bulk of existing callers (Slack-style
    /// labels, notes, etc.) are unaffected. Numeric callers (ZP-2161
    /// engineering pickers) override to ``.numberPad`` / ``.decimalPad``.
    var keyboardType: UIKeyboardType = .default
    /// Optional override for the placeholder. Defaults to the existing
    /// ``"Enter <title>"`` template used everywhere else.
    var placeholder: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)

            HStack {
                // ZP-2161: pass ``icon: ""`` to suppress the leading
                // SF Symbol entirely (engineering pickers that don't
                // have a natural-fit symbol). Existing callers keep
                // their icons unchanged.
                if !icon.isEmpty {
                    Image(systemName: icon)
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                }

                TextField(
                    placeholder ?? AppStrings.Supporting.enterTitle(title.lowercased()),
                    text: $text
                )
                .keyboardType(keyboardType)

                if let trailing = trailingContent {
                    trailing()
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
    }
}

struct ModernPicker<T: Identifiable>: View where T: Hashable {
    let title: String
    let icon: String
    let placeholder: String
    let items: [T]
    @Binding var selection: T?
    let displayName: (T) -> String
    var allowClear: Bool = true
    var clearLabel: String = AppStrings.Common.none
    var onSelectionChange: ((T?) -> Void)?
    /// When true, opens a sheet instead of a Menu to avoid iOS text truncation on long option names
    var useSheet: Bool = false

    @State private var showSheet = false
    @State private var searchText = ""

    private var filteredItems: [T] {
        let trimmed = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return items }
        return items.filter { displayName($0).localizedCaseInsensitiveContains(trimmed) }
    }

    private var triggerButton: some View {
        HStack {
            // Same empty-icon escape hatch as ModernTextField (ZP-2161).
            if !icon.isEmpty {
                Image(systemName: icon)
                    .foregroundColor(.secondary)
                    .frame(width: 20)
            }

            Text(selection != nil ? displayName(selection!) : placeholder)
                .foregroundColor(selection != nil ? .primary : .secondary)
                .lineLimit(useSheet ? nil : 1)
                .multilineTextAlignment(.leading)
                .if(useSheet) { $0.fixedSize(horizontal: false, vertical: true) }

            Spacer()

            Image(systemName: "chevron.down")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)

            if useSheet {
                Button {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    showSheet = true
                } label: { triggerButton }
                    .buttonStyle(.plain)
                    .sheet(isPresented: $showSheet) {
                        NavigationStack {
                            List {
                                if allowClear && searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                    Button {
                                        selection = nil
                                        onSelectionChange?(nil)
                                        showSheet = false
                                    } label: {
                                        HStack {
                                            Text(clearLabel)
                                                .foregroundColor(.primary)
                                            Spacer()
                                            if selection == nil {
                                                Image(systemName: "checkmark")
                                                    .foregroundColor(.blue)
                                            }
                                        }
                                    }
                                }

                                ForEach(filteredItems) { item in
                                    Button {
                                        selection = item
                                        onSelectionChange?(item)
                                        showSheet = false
                                    } label: {
                                        HStack {
                                            Text(displayName(item))
                                                .foregroundColor(.primary)
                                            Spacer()
                                            if selection?.id == item.id {
                                                Image(systemName: "checkmark")
                                                    .foregroundColor(.blue)
                                            }
                                        }
                                    }
                                }
                            }
                            .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .always), prompt: AppStrings.Common.search)
                            .navigationTitle(title)
                            .navigationBarTitleDisplayMode(.inline)
                            .toolbar {
                                ToolbarItem(placement: .navigationBarTrailing) {
                                    Button(AppStrings.Common.done) { showSheet = false }
                                }
                            }
                            .onDisappear { searchText = "" }
                        }
                        .presentationDetents([.medium, .large])
                    }
            } else {
                Menu {
                    if allowClear {
                        Button(action: {
                            selection = nil
                            onSelectionChange?(nil)
                        }) {
                            HStack {
                                Text(clearLabel)
                                if selection == nil {
                                    Spacer()
                                    Image(systemName: "checkmark")
                                }
                            }
                        }

                        Divider()
                    }

                    ForEach(items) { item in
                        Button(action: {
                            selection = item
                            onSelectionChange?(item)
                        }) {
                            HStack {
                                Text(displayName(item))
                                if selection?.id == item.id {
                                    Spacer()
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    triggerButton
                }
            }
        }
    }
}

// Conditional modifier helper
extension View {
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition { transform(self) } else { self }
    }
}

struct ModernSegmentedPicker<T: Hashable>: View {
    let title: String
    let items: [T]
    @Binding var selection: T
    let displayContent: (T) -> SegmentContent
    var columns: Int? = nil
    
    struct SegmentContent {
        let icon: SegmentIcon?
        let label: String
        let color: Color
        let textColor: Color
        
        enum SegmentIcon {
            case systemImage(String)
            case text(String)
            case number(Int)
            case circle(Color)
        }
        
        init(
            icon: SegmentIcon? = nil,
            label: String,
            color: Color = .blue,
            textColor: Color = .white
        ) {
            self.icon = icon
            self.label = label
            self.color = color
            self.textColor = textColor
        }
    }
    
    private var itemsPerRow: Int {
        columns ?? items.count
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            
            if items.count <= 5 && columns == nil {
                // Horizontal layout for small number of items
                HStack(spacing: 0) {
                    ForEach(items, id: \.self) { item in
                        segmentButton(for: item)
                    }
                }
                .padding(4)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            } else {
                // Grid layout for many items or when columns specified
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: itemsPerRow), spacing: 0) {
                    ForEach(items, id: \.self) { item in
                        segmentButton(for: item)
                    }
                }
                .padding(4)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
        }
    }
    
    @ViewBuilder
    private func segmentButton(for item: T) -> some View {
        let content = displayContent(item)
        let isSelected = selection == item
        
        Button(action: {
            // ZP-2161: dropped the ``withAnimation`` wrap. It caused
            // SwiftUI to animate every cascading state change inside
            // the binding's setter (e.g. trip-type → nulls 3 other
            // amp fields → animates view destruction/creation). The
            // resulting frame-rate drop was visible even on simple
            // toggles (condition-of-maintenance etc.). Instant tap
            // feedback also reads as faster.
            selection = item
        }) {
            VStack(spacing: 4) {
                // Icon/Badge
                if let icon = content.icon {
                    switch icon {
                    case .systemImage(let name):
                        Image(systemName: name)
                            .font(.title3)
                            .foregroundColor(isSelected ? content.textColor : content.color)
                    case .text(let text):
                        Text(text)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(isSelected ? content.textColor : content.color)
                    case .number(let num):
                        Circle()
                            .fill(isSelected ? content.color : Color.clear)
                            .overlay(
                                Circle()
                                    .stroke(content.color, lineWidth: isSelected ? 0 : 2)
                            )
                            .frame(width: 30, height: 30)
                            .overlay(
                                Text("\(num)")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(isSelected ? content.textColor : content.color)
                            )
                    case .circle(let color):
                        Circle()
                            .fill(color)
                            .frame(width: 30, height: 30)
                            .overlay(
                                Circle()
                                    .stroke(Color.white, lineWidth: isSelected ? 2 : 0)
                            )
                    }
                }
                
                // Label
                Text(content.label)
                    .font(.caption2)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                    .foregroundColor(isSelected ? .primary : .secondary)
            }
            // ZP-2161: ``maxHeight: .infinity`` makes every segment fill
            // the row's full height, so a 2-line label (e.g.
            // "Electromechanical Relay") doesn't leave 1-line segments
            // top-aligned. With centered VStack content, all labels sit
            // on the same baseline regardless of wrap.
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(isSelected ? Color(.systemGray5) : Color.clear)
            )
            // ZP-2161: ``Color.clear`` backgrounds don't register taps
            // — without this, only the Text glyphs were hit-testable
            // on unselected segments. ``contentShape`` declares the
            // full frame as the hit area regardless of background opacity.
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Login Text Field Styles

/// Modern text field style used in login screens
struct ModernTextFieldStyle: TextFieldStyle {
    @FocusState var isFocused: Bool

    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(UIColor.systemGray6))
                    .stroke(isFocused ? Color.blue : Color.clear, lineWidth: 2)
            )
            .focused($isFocused)
    }
}

/// Modern secure field style with visibility toggle.
///
/// Backed by a single UITextField so toggling visibility flips
/// `isSecureTextEntry` on the same first-responder instance — the keyboard
/// stays up and there's no view-swap flicker.
struct ModernSecureFieldStyle: View {
    @Binding var text: String
    let placeholder: String
    @Binding var isSecure: Bool
    var textContentType: UITextContentType? = .password
    /// Focus state must live on a view that survives layout rebuilds
    /// (e.g., orientation changes that swap landscape/portrait branches).
    /// Otherwise rotation tears down the UITextField and the keyboard closes.
    @Binding var isFocused: Bool

    var body: some View {
        HStack {
            SecureTextFieldRepresentable(
                text: $text,
                placeholder: placeholder,
                isSecure: isSecure,
                textContentType: textContentType,
                isFocused: $isFocused
            )

            Button(action: {
                withAnimation(.easeInOut(duration: 0.2)) {
                    isSecure.toggle()
                }
            }) {
                Image(systemName: isSecure ? "eye.slash" : "eye")
                    .font(.system(.body, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(width: 20, height: 20)
            }
            .buttonStyle(PlainButtonStyle())
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(UIColor.systemGray6))
                .stroke(isFocused ? Color.blue : Color.clear, lineWidth: 2)
        )
    }
}

/// Plain (non-secure) field counterpart to `ModernSecureFieldStyle`,
/// backed by the same UITextField representable so focus survives
/// parent view rebuilds (e.g., iPhone rotation between landscape and
/// portrait layouts in LoginViewWithConfig).
struct ModernPlainFieldStyle: View {
    @Binding var text: String
    let placeholder: String
    var keyboardType: UIKeyboardType = .default
    var textContentType: UITextContentType? = nil
    var autocapitalizationType: UITextAutocapitalizationType = .sentences
    var autocorrectionType: UITextAutocorrectionType = .default
    @Binding var isFocused: Bool

    var body: some View {
        SecureTextFieldRepresentable(
            text: $text,
            placeholder: placeholder,
            isSecure: false,
            textContentType: textContentType,
            keyboardType: keyboardType,
            autocapitalizationType: autocapitalizationType,
            autocorrectionType: autocorrectionType,
            isFocused: $isFocused
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(UIColor.systemGray6))
                .stroke(isFocused ? Color.blue : Color.clear, lineWidth: 2)
        )
    }
}

private struct SecureTextFieldRepresentable: UIViewRepresentable {
    @Binding var text: String
    let placeholder: String
    var isSecure: Bool
    var textContentType: UITextContentType?
    var keyboardType: UIKeyboardType = .default
    var autocapitalizationType: UITextAutocapitalizationType = .none
    var autocorrectionType: UITextAutocorrectionType = .no
    @Binding var isFocused: Bool

    func makeUIView(context: Context) -> UITextField {
        let tf = UITextField()
        tf.placeholder = placeholder
        tf.delegate = context.coordinator
        tf.addTarget(context.coordinator,
                     action: #selector(Coordinator.textChanged(_:)),
                     for: .editingChanged)
        tf.autocapitalizationType = autocapitalizationType
        tf.autocorrectionType = autocorrectionType
        tf.keyboardType = keyboardType
        tf.textContentType = textContentType
        tf.isSecureTextEntry = isSecure
        tf.text = text
        tf.font = UIFont.preferredFont(forTextStyle: .body)
        tf.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return tf
    }

    func updateUIView(_ tf: UITextField, context: Context) {
        context.coordinator.parent = self
        if tf.text != text {
            tf.text = text
        }
        if tf.placeholder != placeholder {
            tf.placeholder = placeholder
        }
        if tf.textContentType != textContentType {
            tf.textContentType = textContentType
        }
        if tf.isSecureTextEntry != isSecure {
            // iOS can drop the buffer when toggling isSecureTextEntry on a
            // focused field; save and restore to keep what the user typed.
            let saved = tf.text
            tf.isSecureTextEntry = isSecure
            tf.text = saved
        }
        // Sync first responder with the binding so focus survives parent
        // view rebuilds (e.g., orientation changes that swap layout branches).
        if isFocused && !tf.isFirstResponder {
            DispatchQueue.main.async {
                // Re-check live state — between scheduling and firing,
                // the binding may have changed or the view may have
                // detached from the window.
                if tf.window != nil && self.isFocused && !tf.isFirstResponder {
                    tf.becomeFirstResponder()
                }
            }
        } else if !isFocused && tf.isFirstResponder {
            tf.resignFirstResponder()
        }
    }

    static func dismantleUIView(_ tf: UITextField, coordinator: Coordinator) {
        coordinator.isDismantling = true
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextFieldDelegate {
        var parent: SecureTextFieldRepresentable
        var isDismantling = false
        init(_ parent: SecureTextFieldRepresentable) { self.parent = parent }

        @objc func textChanged(_ tf: UITextField) {
            parent.text = tf.text ?? ""
        }

        func textFieldDidBeginEditing(_ tf: UITextField) {
            if !parent.isFocused {
                parent.isFocused = true
            }
        }

        func textFieldDidEndEditing(_ tf: UITextField) {
            // Don't clear the binding if the field is being torn down
            // (orientation rebuild, view dismissal). Otherwise the new
            // field created during the rebuild won't auto-restore focus.
            guard !isDismantling, tf.window != nil else { return }
            if parent.isFocused {
                parent.isFocused = false
            }
        }

        func textFieldShouldReturn(_ tf: UITextField) -> Bool {
            tf.resignFirstResponder()
            return true
        }
    }
}
