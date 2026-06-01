//
//  StringExtensions.swift
//  Egalvanic PZ
//

import Foundation

extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var nilIfEmpty: String? {
        let trimmed = self.trimmed
        return trimmed.isEmpty ? nil : trimmed
    }

    /// Converts a 2-letter country code (e.g. "US") to its flag emoji (e.g. "🇺🇸")
    var countryFlag: String {
        let base: UInt32 = 127397
        return self.uppercased().unicodeScalars.compactMap {
            UnicodeScalar(base + $0.value).map(String.init)
        }.joined()
    }
}
