//
//  ComplexityLevel.swift
//  Egalvanic PZ
//

import SwiftUI

enum ComplexityLevel: Double, CaseIterable, Identifiable {
    case standard = 1.0
    case moderate = 1.5
    case high = 2.0
    case critical = 3.0

    var id: Double { rawValue }

    var displayName: String {
        switch self {
        case .standard: return AppStrings.Site.complexityStandard
        case .moderate: return AppStrings.Site.complexityModerate
        case .high: return AppStrings.Site.complexityHigh
        case .critical: return AppStrings.Site.complexityCritical
        }
    }

    var multiplierLabel: String {
        if rawValue == rawValue.rounded() {
            return "\(Int(rawValue)).0x"
        }
        return "\(rawValue)x"
    }

    var description: String {
        switch self {
        case .standard: return AppStrings.Site.complexityStandardDesc
        case .moderate: return AppStrings.Site.complexityModerateDesc
        case .high: return AppStrings.Site.complexityHighDesc
        case .critical: return AppStrings.Site.complexityCriticalDesc
        }
    }

    var color: Color {
        switch self {
        case .standard: return .green
        case .moderate: return .yellow
        case .high: return .orange
        case .critical: return .red
        }
    }
}
