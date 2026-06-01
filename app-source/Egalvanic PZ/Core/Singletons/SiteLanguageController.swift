//
//  SiteLanguageController.swift
//  Egalvanic PZ
//
//  ZP-2061: Auto-applies the office's language when the user selects a site,
//  and lets the user manually override that auto-switch in Settings without
//  being clobbered by the next site selection.
//
//  Mirrors Android's SiteLanguageController and the frontend rules in Layout.jsx:
//   - On site change: clear the in-memory manual-override flag, then either
//     apply the office's language (saving the user's prior preference first,
//     once) or restore the user's preference if the office has no language.
//   - On manual change in Settings: persist the user's preferred language and
//     set an in-memory override that the next office-driven re-evaluation
//     respects (cleared on the next site selection).
//

import Foundation
import SwiftData

@MainActor
final class SiteLanguageController {
    static let shared = SiteLanguageController()

    private let userPreferredLanguageKey = "userPreferredLanguage"
    private var manualOverrideActive = false

    private init() {}

    /// Called after a successful site switch (SLDService.switchToSLD or post-create auto-switch).
    func onSiteSelected(siteId: UUID, modelContext: ModelContext) {
        manualOverrideActive = false
        applyOfficeLanguageOrRestore(siteId: siteId, modelContext: modelContext)
    }

    /// Called when the user edits the *currently-active* site and may have changed its office.
    /// If the user manually picked a language during this session, respect that and skip.
    func onActiveSiteOfficeChanged(siteId: UUID, modelContext: ModelContext) {
        guard !manualOverrideActive else { return }
        applyOfficeLanguageOrRestore(siteId: siteId, modelContext: modelContext)
    }

    /// Called from SettingsView.applyLanguageChange when the user manually changes language.
    func onUserManuallyPickedLanguage(_ language: AppLanguage) {
        UserDefaults.standard.set(language.rawValue, forKey: userPreferredLanguageKey)
        manualOverrideActive = true
    }

    private func applyOfficeLanguageOrRestore(siteId: UUID, modelContext: ModelContext) {
        let descriptor = FetchDescriptor<SLDChoice>(predicate: #Predicate { $0.id == siteId })
        guard let site = try? modelContext.fetch(descriptor).first else { return }

        let officeLang = site.office_language?.trimmed.nilIfEmpty
        let currentTag = LanguageManager.shared.currentLanguage.rawValue

        if let officeLang {
            // Save current language as the user's preference the first time we auto-switch
            // so flipping office assignment back-and-forth doesn't overwrite it.
            if UserDefaults.standard.string(forKey: userPreferredLanguageKey) == nil {
                UserDefaults.standard.set(currentTag, forKey: userPreferredLanguageKey)
            }
            if officeLang != currentTag, let lang = AppLanguage(rawValue: officeLang) {
                LanguageManager.shared.setLanguage(lang)
            }
        } else {
            if let pref = UserDefaults.standard.string(forKey: userPreferredLanguageKey),
               pref != currentTag,
               let lang = AppLanguage(rawValue: pref) {
                LanguageManager.shared.setLanguage(lang)
            }
        }
    }
}
