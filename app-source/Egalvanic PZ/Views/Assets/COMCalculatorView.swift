//
//  COMCalculatorView.swift
//  Egalvanic PZ
//
//  COM Calculator for determining Condition of Maintenance rating
//

import SwiftUI

// MARK: - Maintenance Criteria Definitions

struct MaintenanceCriterion: Identifiable {
    let id: String // key for storage
    let text: String
}

struct MaintenanceCriteriaGroup {
    let level: String // "ns", "3", "2"
    let label: String
    let color: Color
    let items: [MaintenanceCriterion]
}

private var maintenanceCriteria: [MaintenanceCriteriaGroup] {
    [
        MaintenanceCriteriaGroup(level: "ns", label: AppStrings.AssetsExtra.comNonserviceable, color: Color(.darkGray), items: [
            MaintenanceCriterion(id: "ns_exceeded_life", text: AppStrings.AssetsExtra.comNsExceededLife),
            MaintenanceCriterion(id: "ns_imminent_risk", text: AppStrings.AssetsExtra.comNsImminentRisk),
        ]),
        MaintenanceCriteriaGroup(level: "3", label: AppStrings.AssetsExtra.comLevel3Poor, color: .red, items: [
            MaintenanceCriterion(id: "l3_not_maintained", text: AppStrings.AssetsExtra.comL3NotMaintained),
            MaintenanceCriterion(id: "l3_two_cycles_major", text: AppStrings.AssetsExtra.comL3TwoCyclesMajor),
            MaintenanceCriterion(id: "l3_near_end_life", text: AppStrings.AssetsExtra.comL3NearEndLife),
            MaintenanceCriterion(id: "l3_urgent_action", text: AppStrings.AssetsExtra.comL3UrgentAction),
        ]),
        MaintenanceCriteriaGroup(level: "2", label: AppStrings.AssetsExtra.comLevel2Fair, color: .orange, items: [
            MaintenanceCriterion(id: "l2_results_deviate", text: AppStrings.AssetsExtra.comL2ResultsDeviate),
            MaintenanceCriterion(id: "l2_prev_cycle_major", text: AppStrings.AssetsExtra.comL2PrevCycleMajor),
            MaintenanceCriterion(id: "l2_monitoring_notifications", text: AppStrings.AssetsExtra.comL2MonitoringNotifications),
            MaintenanceCriterion(id: "l2_active_predictive", text: AppStrings.AssetsExtra.comL2ActivePredictive),
            MaintenanceCriterion(id: "l2_missed_cycle", text: AppStrings.AssetsExtra.comL2MissedCycle),
            MaintenanceCriterion(id: "l2_minor_reconditioning", text: AppStrings.AssetsExtra.comL2MinorReconditioning),
            MaintenanceCriterion(id: "l2_non_urgent_recommendations", text: AppStrings.AssetsExtra.comL2NonUrgentRecommendations),
        ]),
    ]
}

private let nsKeys: Set<String> = ["ns_exceeded_life", "ns_imminent_risk"]
private let l3Keys: Set<String> = ["l3_not_maintained", "l3_two_cycles_major", "l3_near_end_life", "l3_urgent_action"]
private let l2Keys: Set<String> = ["l2_results_deviate", "l2_prev_cycle_major", "l2_monitoring_notifications", "l2_active_predictive", "l2_missed_cycle", "l2_minor_reconditioning", "l2_non_urgent_recommendations"]

private func deriveMaintenanceValue(from answers: Set<String>) -> (value: Int?, isNonserviceable: Bool) {
    if answers.contains(where: { nsKeys.contains($0) }) { return (nil, true) }
    if answers.contains(where: { l3Keys.contains($0) }) { return (3, false) }
    if answers.contains(where: { l2Keys.contains($0) }) { return (2, false) }
    return (1, false)
}

// MARK: - Main View

struct COMCalculatorView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var assetCriticality: Int
    @State private var operatingEnvironment: Int
    @State private var maintenanceAnswers: Set<String>

    let onApply: (Int?, COMCalculation) -> Void

    init(initialCalculation: COMCalculation? = nil, onApply: @escaping (Int?, COMCalculation) -> Void) {
        let calculation = initialCalculation ?? COMCalculation()
        _assetCriticality = State(initialValue: calculation.criticalityValue)
        _operatingEnvironment = State(initialValue: calculation.operatingConditionsValue)
        _maintenanceAnswers = State(initialValue: Set(calculation.maintenanceAnswers ?? []))
        self.onApply = onApply
    }

    private var maintenanceResult: (value: Int?, isNonserviceable: Bool) {
        deriveMaintenanceValue(from: maintenanceAnswers)
    }

    private var isNonserviceable: Bool { maintenanceResult.isNonserviceable }

    private var calculatedCOM: Int? {
        guard let mv = maintenanceResult.value else { return nil }
        return max(assetCriticality, operatingEnvironment, mv)
    }

    private var calculatedCOMLabel: String {
        guard let com = calculatedCOM else { return AppStrings.AssetsExtra.comNonserviceable }
        switch com {
        case 1: return AppStrings.AssetsExtra.comWellMaintained
        case 2: return AppStrings.AssetsExtra.comGapsInService
        case 3: return AppStrings.AssetsExtra.comPoorUnknown
        default: return AppStrings.AssetsExtra.unknown
        }
    }

    private var calculatedCOMColor: Color {
        guard let com = calculatedCOM else { return Color(.darkGray) }
        switch com {
        case 1: return .green
        case 2: return .yellow
        case 3: return .red
        default: return .gray
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView(.vertical) {
                VStack(spacing: 24) {
                    // Info Banner
                    HStack(spacing: 12) {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(.blue)
                            .font(.title3)

                        Text(AppStrings.AssetsExtra.comInfoBanner)
                            .font(.subheadline)
                            .foregroundColor(.primary)
                    }
                    .padding()
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(12)

                    // Asset Criticality Section
                    COMCategorySection(
                        title: AppStrings.AssetsExtra.assetCriticality,
                        subtitle: AppStrings.AssetsExtra.impactIfFails,
                        selectedValue: $assetCriticality,
                        options: [
                            COMOption(value: 1, title: AppStrings.AssetsExtra.comStandard, description: AppStrings.AssetsExtra.comStandardDesc, color: .green),
                            COMOption(value: 2, title: AppStrings.AssetsExtra.comBusinessCritical, description: AppStrings.AssetsExtra.comBusinessCriticalDesc, color: .yellow),
                            COMOption(value: 3, title: AppStrings.AssetsExtra.comLifeSafety, description: AppStrings.AssetsExtra.comLifeSafetyDesc, color: .red)
                        ]
                    )

                    // Operating Environment Section
                    COMCategorySection(
                        title: AppStrings.AssetsExtra.operatingEnvironment,
                        subtitle: AppStrings.AssetsExtra.environmentalStress,
                        selectedValue: $operatingEnvironment,
                        options: [
                            COMOption(value: 1, title: AppStrings.AssetsExtra.comIndoorStandard, description: AppStrings.AssetsExtra.comIndoorStandardDesc, color: .green),
                            COMOption(value: 2, title: AppStrings.AssetsExtra.comModerateExposure, description: AppStrings.AssetsExtra.comModerateExposureDesc, color: .yellow),
                            COMOption(value: 3, title: AppStrings.AssetsExtra.comHarshConditions, description: AppStrings.AssetsExtra.comHarshConditionsDesc, color: .red)
                        ]
                    )

                    // Maintenance State — Checklist
                    VStack(alignment: .leading, spacing: 12) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(AppStrings.PhotoWalkthrough.maintenanceState)
                                .font(.headline)
                                .fontWeight(.bold)

                            Text(AppStrings.PhotoWalkthrough.maintenanceCheckDescription)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        ForEach(maintenanceCriteria, id: \.level) { group in
                            VStack(alignment: .leading, spacing: 0) {
                                Text(group.label)
                                    .font(.caption2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(group.color)
                                    .textCase(.uppercase)
                                    .padding(.bottom, 4)

                                VStack(spacing: 0) {
                                    ForEach(group.items) { item in
                                        Button {
                                            withAnimation(.easeInOut(duration: 0.15)) {
                                                if maintenanceAnswers.contains(item.id) {
                                                    maintenanceAnswers.remove(item.id)
                                                } else {
                                                    maintenanceAnswers.insert(item.id)
                                                }
                                            }
                                        } label: {
                                            HStack(alignment: .top, spacing: 10) {
                                                Image(systemName: maintenanceAnswers.contains(item.id) ? "checkmark.square.fill" : "square")
                                                    .foregroundColor(maintenanceAnswers.contains(item.id) ? group.color : .secondary)
                                                    .font(.body)

                                                Text(item.text)
                                                    .font(.caption)
                                                    .foregroundColor(.primary)
                                                    .multilineTextAlignment(.leading)

                                                Spacer()
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(maintenanceAnswers.contains(item.id) ? group.color.opacity(0.05) : Color.clear)
                                        }
                                        .buttonStyle(.plain)

                                        if item.id != group.items.last?.id {
                                            Divider().padding(.leading, 40)
                                        }
                                    }
                                }
                                .background(Color(.systemBackground))
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(
                                            group.items.contains(where: { maintenanceAnswers.contains($0.id) }) ? group.color : Color(.systemGray4),
                                            lineWidth: 1
                                        )
                                )
                            }
                        }

                        // Derived level indicator
                        HStack(spacing: 6) {
                            Text(AppStrings.PhotoWalkthrough.derivedLevel)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(isNonserviceable ? AppStrings.AssetsExtra.comNonserviceable : AppStrings.AssetsExtra.comLevelN(maintenanceResult.value ?? 1))
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(isNonserviceable ? Color(.darkGray) : maintenanceResult.value == 3 ? .red : maintenanceResult.value == 2 ? .orange : .green)
                        }
                    }

                    // Calculated COM Rating
                    VStack(spacing: 12) {
                        Text(AppStrings.AssetsExtra.calculatedCOMRating)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)

                        HStack(spacing: 16) {
                            Circle()
                                .fill(calculatedCOMColor)
                                .frame(width: 44, height: 44)
                                .overlay(
                                    Text(isNonserviceable ? "N/S" : "\(calculatedCOM ?? 0)")
                                        .font(isNonserviceable ? .caption2 : .headline)
                                        .fontWeight(.bold)
                                        .foregroundColor(calculatedCOM == 2 ? .black : .white)
                                )

                            VStack(alignment: .leading, spacing: 4) {
                                Text(calculatedCOMLabel)
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                if isNonserviceable {
                                    Text(AppStrings.AssetsExtra.comNonserviceableExplanation(criticality: assetCriticality, environment: operatingEnvironment))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Text(AppStrings.AssetsExtra.comMaxExplanation(criticality: assetCriticality, environment: operatingEnvironment, maintenance: maintenanceResult.value ?? 1))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(calculatedCOMColor, lineWidth: 2)
                        )
                    }
                }
                .padding()
                .padding(.bottom, 16)
                .containerRelativeFrame(.horizontal)
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            .background(Color(.systemGray6))
            .navigationTitle(AppStrings.AssetsExtra.comCalculator)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                            .font(.body.weight(.medium))
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.AssetsExtra.reset) {
                        withAnimation {
                            assetCriticality = 1
                            operatingEnvironment = 1
                            maintenanceAnswers = []
                        }
                    }
                    .foregroundColor(.red)
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(action: {
                    let calculation = COMCalculation(
                        criticalityValue: assetCriticality,
                        maintenanceValue: maintenanceResult.value,
                        operatingConditionsValue: operatingEnvironment,
                        maintenanceAnswers: Array(maintenanceAnswers)
                    )
                    onApply(calculatedCOM, calculation)
                    dismiss()
                }) {
                    HStack {
                        Image(systemName: "checkmark.square.fill")
                        Text(isNonserviceable ? AppStrings.AssetsExtra.comApplyNonserviceable : AppStrings.AssetsExtra.applyRating(calculatedCOM ?? 0))
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
                .background(Color(.systemGray6))
            }
        }
    }
}

// MARK: - Supporting Types

struct COMOption {
    let value: Int
    let title: String
    let description: String
    let color: Color
}

struct COMCategorySection: View {
    let title: String
    let subtitle: String
    @Binding var selectedValue: Int
    let options: [COMOption]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)

                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            VStack(spacing: 8) {
                ForEach(options, id: \.value) { option in
                    COMOptionRow(
                        option: option,
                        isSelected: selectedValue == option.value,
                        onTap: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedValue = option.value
                            }
                        }
                    )
                }
            }
        }
    }
}

struct COMOptionRow: View {
    let option: COMOption
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Circle()
                    .fill(option.color)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text("\(option.value)")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(option.value == 2 ? .black : .white)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(option.title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)

                    Text(option.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(option.color)
                        .font(.title3)
                }
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? option.color : Color(.systemGray4), lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}
