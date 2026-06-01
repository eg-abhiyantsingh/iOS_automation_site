//
//  AddressFormatter.swift
//  Egalvanic PZ
//

import Foundation

enum AddressFormatter {

    // Hardcoded English names for the API payload (not localized)
    private static let countries = [
        ("US", "United States"),
        ("CA", "Canada"),
        ("GB", "United Kingdom"),
        ("AU", "Australia")
    ]

    static func format(_ input: CreateSiteInput) -> String? {
        guard !input.addressLine1.trimmed.isEmpty || !input.city.trimmed.isEmpty else { return nil }

        var parts: [String] = []
        if let line1 = input.addressLine1.nilIfEmpty { parts.append(line1) }
        if let line2 = input.addressLine2.nilIfEmpty { parts.append(line2) }

        var cityLine: [String] = []
        if let city = input.city.nilIfEmpty { cityLine.append(city) }
        if let state = input.stateProvince.nilIfEmpty { cityLine.append(state) }
        if let postal = input.postalCode.nilIfEmpty { cityLine.append(postal) }
        if !cityLine.isEmpty { parts.append(cityLine.joined(separator: ", ")) }

        if let countryName = countries.first(where: { $0.0 == input.countryCode })?.1 {
            parts.append(countryName)
        }

        return parts.joined(separator: "\n")
    }
}
