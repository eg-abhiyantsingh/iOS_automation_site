//
//  DropdownEdgeClassPicker.swift
//  Egalvanic PZ
//
//  Sheet-based picker for edge class selection (Cable, Busway, DC Cable, None).
//  Follows the same pattern as DropdownNodePicker for consistent UX.
//

import SwiftUI

/// A button that opens a bottom sheet for edge class selection
struct DropdownEdgeClassPicker: View {
    @Binding var selectedEdgeClassId: UUID?
    let edgeClasses: [EdgeClass]
    let placeholder: String

    @State private var isSheetPresented = false

    private var selectedClassName: String? {
        guard let id = selectedEdgeClassId else { return nil }
        return edgeClasses.first { $0.id == id }?.name
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(AppStrings.AssetsExtra.edgeClass)
                .font(.caption)
                .foregroundColor(.secondary)

            Button(action: { isSheetPresented = true }) {
                HStack {
                    Text(selectedClassName ?? placeholder)
                        .foregroundColor(selectedClassName != nil ? .primary : .secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Image(systemName: "chevron.right")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
            .buttonStyle(PlainButtonStyle())
        }
        .sheet(isPresented: $isSheetPresented) {
            EdgeClassPickerSheet(
                selectedEdgeClassId: $selectedEdgeClassId,
                edgeClasses: edgeClasses,
                isPresented: $isSheetPresented
            )
            .presentationDetents([.medium])
        }
    }
}

// MARK: - Edge Class Picker Sheet

private struct EdgeClassPickerSheet: View {
    @Binding var selectedEdgeClassId: UUID?
    let edgeClasses: [EdgeClass]
    @Binding var isPresented: Bool

    var body: some View {
        NavigationView {
            List {
                // None option
                Button(action: {
                    selectedEdgeClassId = nil
                    isPresented = false
                }) {
                    HStack {
                        Text(AppStrings.Common.none)
                            .foregroundColor(.secondary)

                        Spacer()

                        if selectedEdgeClassId == nil {
                            Image(systemName: "checkmark")
                                .foregroundColor(.blue)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(PlainButtonStyle())

                // Edge class options
                ForEach(edgeClasses, id: \.id) { edgeClass in
                    Button(action: {
                        selectedEdgeClassId = edgeClass.id
                        isPresented = false
                    }) {
                        HStack {
                            Text(edgeClass.name)
                                .foregroundColor(.primary)

                            Spacer()

                            if selectedEdgeClassId == edgeClass.id {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .transaction { $0.animation = nil }
            .navigationTitle(AppStrings.AssetsExtra.edgeClass)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.cancel) {
                        isPresented = false
                    }
                }
            }
        }
    }
}
