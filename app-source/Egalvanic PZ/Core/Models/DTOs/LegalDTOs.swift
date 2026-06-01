//
//  LegalDTOs.swift
//  Egalvanic PZ
//
//  DTOs for legal document consent API endpoints
//

import Foundation

// MARK: - Legal Document Detail

struct LegalDocumentDetail: Codable, Identifiable {
    let id: String
    let version: String
    let title: String
    let contentUrl: String
    let summaryOfChanges: String?

    enum CodingKeys: String, CodingKey {
        case id, version, title
        case contentUrl = "content_url"
        case summaryOfChanges = "summary_of_changes"
    }
}

// MARK: - Legal Documents Container

/// Shared between CompanyConfig response and acceptance check response
struct LegalDocumentsContainer: Codable {
    let termsAndConditions: LegalDocumentDetail?
    let privacyPolicy: LegalDocumentDetail?

    enum CodingKeys: String, CodingKey {
        case termsAndConditions = "terms_and_conditions"
        case privacyPolicy = "privacy_policy"
    }
}

// MARK: - Acceptance Check Response

struct LegalAcceptanceCheckResponse: Decodable {
    let allAccepted: Bool
    let pending: LegalDocumentsContainer?

    enum CodingKeys: String, CodingKey {
        case allAccepted = "all_accepted"
        case pending
    }
}

// MARK: - Acceptance Request

struct LegalAcceptanceRequest: Encodable {
    let versionId: String
    let acceptedVia: String
    let acceptanceMethod: String
    let sessionContext: SessionContext?

    enum CodingKeys: String, CodingKey {
        case versionId = "version_id"
        case acceptedVia = "accepted_via"
        case acceptanceMethod = "acceptance_method"
        case sessionContext = "session_context"
    }
}

struct SessionContext: Encodable {
    let deviceId: String

    enum CodingKeys: String, CodingKey {
        case deviceId = "device_id"
    }
}
