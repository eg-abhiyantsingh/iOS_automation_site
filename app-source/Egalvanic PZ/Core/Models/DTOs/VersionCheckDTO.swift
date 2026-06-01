//
//  VersionCheckDTO.swift
//  Egalvanic PZ
//
//  DTOs for version check API
//

import Foundation

// MARK: - Request
struct VersionCheckRequest: Encodable {
    let platform: String
    let version_code: String

    init(versionCode: String) {
        self.platform = "ios"
        self.version_code = versionCode
    }
}

// MARK: - Response
struct VersionCheckResponse: Decodable {
    let force_update: Bool
    let requires_update: Bool
    let success: Bool
    let update_info: UpdateInfo?
}

struct UpdateInfo: Decodable {
    let force_update: Bool
    let message: String?
    let min_supported_version_code: Double?
    let recommended_version_code: Double?
    let remind_later_seconds: Int?
    let requires_update: Bool
    let store_url: String?
}
