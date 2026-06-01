//
//  AttachmentVisibility.swift
//  Egalvanic PZ
//

import Foundation
import SwiftUI

/// Visibility level for attachments - controls who can see the file
enum AttachmentVisibility: String, CaseIterable, Codable, Identifiable {
    case `internal` = "internal"
    case `public` = "public"

    var id: String { rawValue }

    /// Human-readable display name for UI
    var displayName: String {
        switch self {
        case .internal: return AppStrings.Sessions.visibilityInternal
        case .public: return AppStrings.Sessions.visibilityPublic
        }
    }

    /// Color used for the visibility chip/tag
    var color: Color {
        switch self {
        case .internal: return .orange
        case .public: return .green
        }
    }
}
