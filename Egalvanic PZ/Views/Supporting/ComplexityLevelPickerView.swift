//
//  ComplexityLevelPickerView.swift
//  Egalvanic PZ
//

import SwiftUI

struct ComplexityLevelPickerView: View {
    let selectedLevel: ComplexityLevel?
    let onSelect: (ComplexityLevel) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 0) {
                // Tooltip / subtitle
                Text(AppStrings.Site.complexityTooltip)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .padding(.bottom, 4)

                List {
                    ForEach(ComplexityLevel.allCases) { level in
                        Button {
                            onSelect(level)
                            dismiss()
                        } label: {
                            HStack(alignment: .top, spacing: 14) {
                                // Multiplier chip
                                Text(level.multiplierLabel)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(level.color)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(
                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(level.color.opacity(0.15))
                                    )
                                    .frame(width: 56)

                                // Title + description
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(level.displayName)
                                        .font(.body.weight(.medium))
                                        .foregroundColor(.primary)
                                    Text(level.description)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                if selectedLevel == level {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                        .padding(.top, 4)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle(AppStrings.Site.complexityLevel)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.done) { dismiss() }
                }
            }
        }
    }
}
