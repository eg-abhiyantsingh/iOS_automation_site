//
//  CreateSiteInput.swift
//  Egalvanic PZ
//

import Foundation

struct CreateSiteInput {
    let siteName: String
    let addressLine1: String
    let addressLine2: String
    let city: String
    let stateProvince: String
    let postalCode: String
    let countryCode: String
    let accountId: String?
    let complexityLevel: Double?
    let laborUnionId: String?
    let officeId: String?
    let latitude: Double?
    let longitude: Double?
}
