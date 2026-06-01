//
//  CompanyConfig.swift
//  Egalvanic PZ
//
//  Model for storing company configuration retrieved from the backend
//

import Foundation

// MARK: - Company Config Models

struct CompanyConfig: Codable {
    let success: Bool
    let company: CompanyInfo
    let alliancePartner: AlliancePartnerInfo?
    let legalDocuments: LegalDocumentsContainer?

    enum CodingKeys: String, CodingKey {
        case success
        case company
        case alliancePartner = "alliance_partner"
        case legalDocuments = "legal_documents"
    }
}

struct CompanyInfo: Codable {
    let id: String
    let name: String
    let subdomain: String
    let address: Address
    let branding: Branding
}

struct AlliancePartnerInfo: Codable {
    let id: String
    let name: String
    let displayName: String
    let invokeUrl: String
    let branding: Branding

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case displayName = "display_name"
        case invokeUrl = "invoke_url"
        case branding
    }
}

struct Address: Codable {
    let line1: String?
    let line2: String?
    let city: String?
    let stateProvince: String?
    let postalCode: String?
    let countryCode: String?
    let formatted: String?

    enum CodingKeys: String, CodingKey {
        case line1 = "line_1"
        case line2 = "line_2"
        case city
        case stateProvince = "state_province"
        case postalCode = "postal_code"
        case countryCode = "country_code"
        case formatted
    }
}

struct Branding: Codable {
    let smallLogo: String?
    let largeLogo: String?
    let primaryColor: String?
    let accentColor: String?

    enum CodingKeys: String, CodingKey {
        case smallLogo = "small_logo"
        case largeLogo = "large_logo"
        case primaryColor = "primary_color"
        case accentColor = "accent_color"
    }
}

// MARK: - Stored Company Config

/// Simplified config stored in UserDefaults for persistence
struct StoredCompanyConfig: Codable {
    let companyCode: String
    let companyName: String
    let invokeUrl: String
    let logoUrl: String?  // Company logo, with fallback to alliance partner logo
    let primaryColor: String?
    let accentColor: String?
    let legalDocuments: LegalDocumentsContainer?

    /// Initialize from full CompanyConfig response
    init(from config: CompanyConfig) {
        self.companyCode = config.company.subdomain
        self.companyName = config.company.name
        self.invokeUrl = config.alliancePartner?.invokeUrl ?? Configuration.apiBaseURLString

        // Prefer company logo, fallback to alliance partner logo
        self.logoUrl = config.company.branding.largeLogo ?? config.alliancePartner?.branding.largeLogo

        // Use company colors if available, otherwise alliance partner colors
        self.primaryColor = config.company.branding.primaryColor ?? config.alliancePartner?.branding.primaryColor
        self.accentColor = config.company.branding.accentColor ?? config.alliancePartner?.branding.accentColor

        self.legalDocuments = config.legalDocuments
    }
}
