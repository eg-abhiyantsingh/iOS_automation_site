//
//  CopyAssetFieldSelectionView.swift
//  Egalvanic PZ
//
//  Field selection sheet for copy asset details
//

import SwiftUI

struct CopyAssetFieldSelectionView: View {
    @Environment(\.dismiss) private var dismiss

    let sourceNodeLabel: String
    let direction: CopyDirection
    let onConfirm: (Set<CopyableField>) -> Void

    @State private var selectedFields: Set<CopyableField> = Set(CopyableField.allCases)

    private var headerText: String {
        switch direction {
        case .from:
            return "Copy details from \(sourceNodeLabel)"
        case .to:
            return "Copy details to \(sourceNodeLabel)"
        }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                Text(headerText)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.top, 12)
                    .padding(.bottom, 8)

                List {
                    ForEach(CopyableField.allCases) { field in
                        Button {
                            if selectedFields.contains(field) {
                                selectedFields.remove(field)
                            } else {
                                selectedFields.insert(field)
                            }
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: selectedFields.contains(field) ? "checkmark.circle.fill" : "circle")
                                    .font(.title2)
                                    .foregroundColor(selectedFields.contains(field) ? .blue : .gray)

                                Text(field.displayName)
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundColor(.primary)

                                Spacer()
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
            }
            .navigationTitle(AppStrings.AssetsExtra.selectFields)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.apply) {
                        onConfirm(selectedFields)
                        dismiss()
                    }
                    .disabled(selectedFields.isEmpty)
                    .fontWeight(.semibold)
                }
            }
        }
    }
}
