//
//  EGFormPickerSheet.swift
//  Egalvanic PZ
//
//  ZP-1723: pick an EGForm definition to link to a task. Surfaces all
//  non-deleted, non-override forms from the local SwiftData store
//  (these were synced via SLD sync earlier). Grouped by type
//  (NETA / Safety / 70B / etc.) for quick scanning.
//

import SwiftUI
import SwiftData
import UIKit

struct EGFormPickerSheet: View {
    let onPick: (EGForm) -> Void
    let onCancel: () -> Void

    // Scope: every form definition we have locally that isn't deleted
    // and isn't an override of another form. The user can always
    // refine later — for now we keep the pool wide.
    @Query(
        filter: #Predicate<EGForm> { !$0.is_deleted && !$0.is_override },
        sort: \EGForm.title
    ) private var forms: [EGForm]

    @State private var search: String = ""

    private var filtered: [EGForm] {
        guard !search.isEmpty else { return forms }
        let needle = search.lowercased()
        return forms.filter { f in
            (f.title?.lowercased().contains(needle) ?? false) ||
            (f.eg_form_type_name?.lowercased().contains(needle) ?? false) ||
            (f.eg_form_type_key?.lowercased().contains(needle) ?? false) ||
            (f.key?.lowercased().contains(needle) ?? false)
        }
    }

    private var grouped: [(typeKey: String, forms: [EGForm])] {
        // Group by the same normalized label rows display, so a
        // "NETA_FRAGMENT" form bucket reads "NETA" — matching what
        // the inspector sees on the row badge.
        let groups = Dictionary(grouping: filtered) { $0.displayTypeLabel }
        return groups.keys.sorted().map { key in
            (typeKey: key, forms: groups[key] ?? [])
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            header

            if forms.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(grouped, id: \.typeKey) { group in
                        Section(group.typeKey.uppercased()) {
                            ForEach(group.forms) { form in
                                row(for: form)
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .searchable(text: $search, placement: .navigationBarDrawer(displayMode: .always),
                            prompt: "Search forms")
            }
        }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(AppStrings.Forms.linkEGForm)
                    .font(.headline)
                Text(AppStrings.Forms.pickFormForTask)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Button(AppStrings.Common.cancel) { onCancel() }
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "tray")
                .font(.system(size: 32))
                .foregroundStyle(.tertiary)
            Text("No EG forms synced yet.")
                .font(.callout)
                .foregroundStyle(.secondary)
            Text("Pull SLD from the server to fetch form definitions.")
                .font(.caption)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    @ViewBuilder
    private func row(for form: EGForm) -> some View {
        Button {
            onPick(form)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "doc.text.fill")
                    .foregroundStyle(.cyan)
                VStack(alignment: .leading, spacing: 4) {
                    Text(form.title ?? "Untitled form")
                        .font(.body)
                        .foregroundStyle(.primary)
                    // Asset classes this form is mapped to. For NETA
                    // fragments this is the disambiguator — same
                    // fragment title can apply to multiple switchgear
                    // / breaker / transformer classes. Rendered as a
                    // wrapping row of small chips.
                    if !form.node_class_names.isEmpty {
                        FlowChips(labels: form.node_class_names)
                            .padding(.top, 2)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Thin wrap of small chips — used for the asset-class hints on the
/// EG form picker rows. Uses LayoutInspector-style flow via
/// `FlowLayout` if available; otherwise falls back to wrapping HStack
/// via `.fixedSize` and natural wrapping in a `LazyVGrid` of one row.
private struct FlowChips: View {
    let labels: [String]
    var body: some View {
        // Use a wrapping flexible layout. SwiftUI's flow layout
        // requires iOS 16+; this approximation works on a single
        // line until natural truncation, which is acceptable for
        // the picker (rows are tall enough to scroll if needed).
        HStack(spacing: 4) {
            ForEach(Array(labels.prefix(4).enumerated()), id: \.offset) { _, name in
                Text(name)
                    .font(.system(size: 10, weight: .semibold))
                    .padding(.horizontal, 6).padding(.vertical, 2)
                    .background(Color(UIColor.systemGray6))
                    .foregroundStyle(.secondary)
                    .clipShape(Capsule())
                    .lineLimit(1)
            }
            if labels.count > 4 {
                Text("+\(labels.count - 4)")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
        }
    }
}
