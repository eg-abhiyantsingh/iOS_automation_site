//
//  CommercialSection.swift
//  Egalvanic PZ
//
//  ZP-2415 — Commercial card grouping Condition of Maintenance,
//  Suggested PM Plan, and Replacement Cost. Mirrors the web
//  AssetFormFields "Commercial" section so Create Asset and Edit
//  Asset share the same three-field bundle in one card.
//

import SwiftUI

struct CommercialSection: View {
    @Binding var draftCOM: Int?
    @Binding var draftCOMCalculation: COMCalculation?
    @Binding var selectedShortcut: NodeShortcut?
    @Binding var draftReplacementCost: Double?

    let nodeClass: NodeClass?
    let nodeSubtype: NodeSubtype?
    let sld: SLDV2
    let onShowCOMCalculator: () -> Void
    /// Web hides Suggested PM Plan under certain modes (notably
    /// Edit Asset's `.collectAFData` focus). Surface as a flag so the
    /// section stays a dumb renderer.
    var showSuggestedPMPlan: Bool = true

    @State private var replacementCostText: String = ""

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 16) {
            SectionHeader(
                title: AppStrings.Assets.commercial,
                systemImage: "dollarsign.circle"
            )

            VStack(spacing: 12) {
                comSection
                if showSuggestedPMPlan {
                    ShortcutPickerView(
                        selectedShortcut: $selectedShortcut,
                        nodeClass: nodeClass,
                        nodeSubtype: nodeSubtype,
                        sld: sld
                    )
                }
                replacementCostField
            }
        }
        .padding()
        .background(colorScheme == .dark ? Color(.secondarySystemBackground) : Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 10, y: 5)
        .onAppear {
            replacementCostText = formattedCost(draftReplacementCost)
        }
        .onChange(of: draftReplacementCost) { _, newValue in
            let formatted = formattedCost(newValue)
            if formatted != replacementCostText {
                replacementCostText = formatted
            }
        }
    }

    // MARK: - COM

    @ViewBuilder
    private var comSection: some View {
        let isDisabled: Bool = draftCOM == nil
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(AppStrings.Assets.conditionOfMaintenance)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                Button(action: {
                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    onShowCOMCalculator()
                }) {
                    Text(AppStrings.Assets.calculator)
                        .font(.caption)
                        .foregroundColor(.blue)
                }
            }
            ZStack {
                HStack(spacing: 0) {
                    comLevelButton(1, isSelected: draftCOM == 1, isDisabled: isDisabled)
                    comLevelButton(2, isSelected: draftCOM == 2, isDisabled: isDisabled)
                    comLevelButton(3, isSelected: draftCOM == 3, isDisabled: isDisabled)
                }
                .padding(4).background(Color(.systemGray6)).cornerRadius(10)
                .opacity(isDisabled ? 0.3 : 1)

                if isDisabled {
                    Text(AppStrings.AssetsExtra.nonserviceable).font(.subheadline).fontWeight(.semibold)
                        .foregroundColor(.white).padding(.horizontal, 16).padding(.vertical, 8)
                        .background(Color(.darkGray)).cornerRadius(8)
                }
            }
        }
    }

    private func comLevelButton(_ level: Int, isSelected: Bool, isDisabled: Bool) -> some View {
        let color: Color = level == 1 ? .green : level == 2 ? .yellow : .red
        return Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) { draftCOM = level }
        }) {
            VStack(spacing: 4) {
                Circle().fill(color).frame(width: 30, height: 30)
                    .overlay(Circle().stroke(Color.white, lineWidth: isSelected ? 2 : 0))
                Text("\(level)").font(.caption2)
                    .foregroundColor(isSelected ? .primary : .secondary)
            }
            .frame(maxWidth: .infinity).padding(.vertical, 8)
            .background(RoundedRectangle(cornerRadius: 8).fill(isSelected ? Color(.systemGray5) : Color.clear))
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(isDisabled)
    }

    // MARK: - Replacement Cost

    @ViewBuilder
    private var replacementCostField: some View {
        ModernTextField(
            title: AppStrings.Assets.replacementCost,
            text: Binding(
                get: { replacementCostText },
                set: { newValue in
                    replacementCostText = newValue
                    let trimmed = newValue.filter { $0.isNumber || $0 == "." }
                    draftReplacementCost = trimmed.isEmpty ? nil : Double(trimmed)
                }
            ),
            icon: "dollarsign.circle",
            keyboardType: .decimalPad,
            placeholder: AppStrings.Assets.replacementCostPlaceholder
        )
    }

    private func formattedCost(_ value: Double?) -> String {
        guard let value else { return "" }
        if value.truncatingRemainder(dividingBy: 1) == 0 {
            return String(Int(value))
        }
        return String(value)
    }
}
