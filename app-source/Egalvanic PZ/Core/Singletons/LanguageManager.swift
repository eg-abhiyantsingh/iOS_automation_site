//
//  LanguageManager.swift
//  Egalvanic PZ
//
//  Manages in-app language selection and provides the correct
//  localization bundle for the user's chosen language.
//

import SwiftUI

enum AppLanguage: String, CaseIterable {
    case english = "en"
    case french = "fr"

    var displayName: String {
        switch self {
        case .english: return "English"
        case .french: return "Fran\u{00E7}ais"
        }
    }

    var flagEmoji: String {
        switch self {
        case .english: return "\u{1F1FA}\u{1F1F8}"
        case .french: return "\u{1F1EB}\u{1F1F7}"
        }
    }
}

class LanguageManager: ObservableObject {
    static let shared = LanguageManager()

    @Published var currentLanguage: AppLanguage {
        didSet {
            UserDefaults.standard.set(currentLanguage.rawValue, forKey: "appLanguage")
            updateBundle()
        }
    }

    /// The bundle pointing to the current language's .lproj directory.
    /// All AppStrings lookups use this bundle.
    private(set) var bundle: Bundle = .main

    private init() {
        let saved = UserDefaults.standard.string(forKey: "appLanguage") ?? "en"
        self.currentLanguage = AppLanguage(rawValue: saved) ?? .english
        updateBundle()
    }

    private func updateBundle() {
        if let path = Bundle.main.path(forResource: currentLanguage.rawValue, ofType: "lproj"),
           let langBundle = Bundle(path: path) {
            bundle = langBundle
        } else {
            bundle = .main
        }
    }

    func setLanguage(_ language: AppLanguage) {
        currentLanguage = language
    }

    /// Maps an English API issue/task status to its localized display name.
    static func localizedStatus(_ apiStatus: String) -> String {
        switch apiStatus.lowercased() {
        case "open":        return AppStrings.Issues.statusOpen
        case "new":         return AppStrings.Issues.statusOpen
        case "in progress": return AppStrings.Issues.statusInProgress
        case "pending":     return AppStrings.Issues.statusPending
        case "resolved":    return AppStrings.Issues.statusResolved
        case "closed":      return AppStrings.Issues.statusClosed
        case "completed":   return AppStrings.Tasks.completed
        case "draft":       return AppStrings.Quotes.draft
        case "approved":    return AppStrings.Quotes.approved
        case "rejected":    return AppStrings.Quotes.rejected
        default:            return apiStatus
        }
    }

    /// Maps an English API priority to its localized display name.
    static func localizedPriority(_ apiPriority: String) -> String {
        switch apiPriority.lowercased() {
        case "critical": return AppStrings.Issues.priorityCritical
        case "high":     return AppStrings.Issues.priorityHigh
        case "medium":   return AppStrings.Issues.priorityMedium
        case "low":      return AppStrings.Issues.priorityLow
        default:         return apiPriority
        }
    }
}
