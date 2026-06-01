//
//  Serviceability.swift
//  Egalvanic PZ
//

import Foundation

/// Serviceability status for an asset - represents physical COM label status
enum Serviceability: String, CaseIterable, Codable, Identifiable {
    case serviceable = "serviceable"
    case limitedService = "limited_service"
    case nonServiceable = "non_serviceable"

    var id: String { rawValue }

    /// Human-readable display name for UI
    var displayName: String {
        switch self {
        case .serviceable: return "Serviceable"
        case .limitedService: return "Limited Service"
        case .nonServiceable: return "Non-Serviceable"
        }
    }
}
