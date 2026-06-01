//
//  AppStrings.swift
//  Egalvanic PZ
//
//  Centralized localization strings for the app.
//  All user-facing strings should be defined here.
//  Use `static var` (computed) so String(localized:) evaluates at runtime,
//  respecting the user's current locale.
//

import Foundation

enum AppStrings {

    // MARK: - Common
    enum Common {
        static var cancel: String { String(localized: "common.cancel", defaultValue: "Cancel", bundle: LanguageManager.shared.bundle) }
        static var save: String { String(localized: "common.save", defaultValue: "Save", bundle: LanguageManager.shared.bundle) }
        static var create: String { String(localized: "common.create", defaultValue: "Create", bundle: LanguageManager.shared.bundle) }
        static var delete: String { String(localized: "common.delete", defaultValue: "Delete", bundle: LanguageManager.shared.bundle) }
        static var ok: String { String(localized: "common.ok", defaultValue: "OK", bundle: LanguageManager.shared.bundle) }
        static var done: String { String(localized: "common.done", defaultValue: "Done", bundle: LanguageManager.shared.bundle) }
        static var submit: String { String(localized: "common.submit", defaultValue: "Submit", bundle: LanguageManager.shared.bundle) }
        static var back: String { String(localized: "common.back", defaultValue: "Back", bundle: LanguageManager.shared.bundle) }
        static var discard: String { String(localized: "common.discard", defaultValue: "Discard", bundle: LanguageManager.shared.bundle) }
        static var loading: String { String(localized: "common.loading", defaultValue: "Loading...", bundle: LanguageManager.shared.bundle) }
        static var search: String { String(localized: "common.search", defaultValue: "Search...", bundle: LanguageManager.shared.bundle) }
        static var none: String { String(localized: "common.none", defaultValue: "None", bundle: LanguageManager.shared.bundle) }
        static var version: String { String(localized: "common.version", defaultValue: "Version", bundle: LanguageManager.shared.bundle) }
        static var email: String { String(localized: "common.email", defaultValue: "Email", bundle: LanguageManager.shared.bundle) }
        static var password: String { String(localized: "common.password", defaultValue: "Password", bundle: LanguageManager.shared.bundle) }
        static var name: String { String(localized: "common.name", defaultValue: "Name", bundle: LanguageManager.shared.bundle) }
        static var keepEditing: String { String(localized: "common.keepEditing", defaultValue: "Keep Editing", bundle: LanguageManager.shared.bundle) }
        static var verifying: String { String(localized: "common.verifying", defaultValue: "Verifying...", bundle: LanguageManager.shared.bundle) }
        static var continueAction: String { String(localized: "common.continue", defaultValue: "Continue", bundle: LanguageManager.shared.bundle) }
        static var disable: String { String(localized: "common.disable", defaultValue: "Disable", bundle: LanguageManager.shared.bundle) }
        static var enable: String { String(localized: "common.enable", defaultValue: "Enable", bundle: LanguageManager.shared.bundle) }
        static var notNow: String { String(localized: "common.notNow", defaultValue: "Not Now", bundle: LanguageManager.shared.bundle) }
        static var logout: String { String(localized: "common.logout", defaultValue: "Logout", bundle: LanguageManager.shared.bundle) }
        static var pending: String { String(localized: "common.pending", defaultValue: "Pending", bundle: LanguageManager.shared.bundle) }
        static var clear: String { String(localized: "common.clear", defaultValue: "Clear", bundle: LanguageManager.shared.bundle) }
        static var all: String { String(localized: "common.all", defaultValue: "All", bundle: LanguageManager.shared.bundle) }
        static var yes: String { String(localized: "common.yes", defaultValue: "Yes", bundle: LanguageManager.shared.bundle) }
        static var no: String { String(localized: "common.no", defaultValue: "No", bundle: LanguageManager.shared.bundle) }
        static var close: String { String(localized: "common.close", defaultValue: "Close", bundle: LanguageManager.shared.bundle) }
        static var retry: String { String(localized: "common.retry", defaultValue: "Retry", bundle: LanguageManager.shared.bundle) }
        static var success: String { String(localized: "common.success", defaultValue: "Success", bundle: LanguageManager.shared.bundle) }
        static var noImage: String { String(localized: "common.noImage", defaultValue: "No Image", bundle: LanguageManager.shared.bundle) }
        static var notes: String { String(localized: "common.notes", defaultValue: "Notes", bundle: LanguageManager.shared.bundle) }
        static var description: String { String(localized: "common.description", defaultValue: "Description", bundle: LanguageManager.shared.bundle) }
        static var label: String { String(localized: "common.label", defaultValue: "Label", bundle: LanguageManager.shared.bundle) }
        static var apply: String { String(localized: "common.apply", defaultValue: "Apply", bundle: LanguageManager.shared.bundle) }
        static var gallery: String { String(localized: "common.gallery", defaultValue: "Gallery", bundle: LanguageManager.shared.bundle) }
        static var camera: String { String(localized: "common.camera", defaultValue: "Camera", bundle: LanguageManager.shared.bundle) }
        static var equipment: String { String(localized: "common.equipment", defaultValue: "Equipment", bundle: LanguageManager.shared.bundle) }
        static var quantity: String { String(localized: "common.quantity", defaultValue: "Quantity", bundle: LanguageManager.shared.bundle) }
        static var title: String { String(localized: "common.title", defaultValue: "Title", bundle: LanguageManager.shared.bundle) }
        static var status: String { String(localized: "common.status", defaultValue: "Status", bundle: LanguageManager.shared.bundle) }
        static var reopen: String { String(localized: "common.reopen", defaultValue: "Reopen", bundle: LanguageManager.shared.bundle) }
        static var resolve: String { String(localized: "common.resolve", defaultValue: "Resolve", bundle: LanguageManager.shared.bundle) }
        static var complete: String { String(localized: "common.complete", defaultValue: "Complete", bundle: LanguageManager.shared.bundle) }
        static var dueDate: String { String(localized: "common.dueDate", defaultValue: "Due Date", bundle: LanguageManager.shared.bundle) }
        static var incomplete: String { String(localized: "common.incomplete", defaultValue: "Incomplete", bundle: LanguageManager.shared.bundle) }
        static var overdue: String { String(localized: "common.overdue", defaultValue: "Overdue", bundle: LanguageManager.shared.bundle) }
        static var createdDate: String { String(localized: "common.createdDate", defaultValue: "Created Date", bundle: LanguageManager.shared.bundle) }
        static var modifiedDate: String { String(localized: "common.modifiedDate", defaultValue: "Modified Date", bundle: LanguageManager.shared.bundle) }
        static var selectOptions: String { String(localized: "common.selectOptions", defaultValue: "Select options...", bundle: LanguageManager.shared.bundle) }
        static var missingRequiredFields: String { String(localized: "common.missingRequiredFields", defaultValue: "Missing required fields", bundle: LanguageManager.shared.bundle) }
        static var noLocation: String { String(localized: "common.noLocation", defaultValue: "No Location", bundle: LanguageManager.shared.bundle) }
        static var noPhotosYet: String { String(localized: "common.noPhotosYet", defaultValue: "No photos yet", bundle: LanguageManager.shared.bundle) }
        static var room: String { String(localized: "common.room", defaultValue: "Room", bundle: LanguageManager.shared.bundle) }
        static var noRoom: String { String(localized: "common.noRoom", defaultValue: "No Room", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Tabs
    enum Tabs {
        static var site: String { String(localized: "tabs.site", defaultValue: "Site", bundle: LanguageManager.shared.bundle) }
        static var assets: String { String(localized: "tabs.assets", defaultValue: "Assets", bundle: LanguageManager.shared.bundle) }
        static var connections: String { String(localized: "tabs.connections", defaultValue: "Connections", bundle: LanguageManager.shared.bundle) }
        static var sld: String { String(localized: "tabs.sld", defaultValue: "SLD", bundle: LanguageManager.shared.bundle) }
        static var settings: String { String(localized: "tabs.settings", defaultValue: "Settings", bundle: LanguageManager.shared.bundle) }

        static var siteInfo: String { String(localized: "tabs.siteInfo", defaultValue: "Site Info", bundle: LanguageManager.shared.bundle) }
        static var assetsList: String { String(localized: "tabs.assetsList", defaultValue: "Assets List", bundle: LanguageManager.shared.bundle) }
        static var connectionsList: String { String(localized: "tabs.connectionsList", defaultValue: "Connections List", bundle: LanguageManager.shared.bundle) }
        static var singleLineDiagram: String { String(localized: "tabs.singleLineDiagram", defaultValue: "Single-Line Diagram", bundle: LanguageManager.shared.bundle) }

        static var loadingSiteInfo: String { String(localized: "tabs.loadingSiteInfo", defaultValue: "Loading site information...", bundle: LanguageManager.shared.bundle) }
        static var loadingAssets: String { String(localized: "tabs.loadingAssets", defaultValue: "Loading assets...", bundle: LanguageManager.shared.bundle) }
        static var loadingConnections: String { String(localized: "tabs.loadingConnections", defaultValue: "Loading connections...", bundle: LanguageManager.shared.bundle) }
        static var loadingDiagram: String { String(localized: "tabs.loadingDiagram", defaultValue: "Loading diagram...", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Home / Schedule
    enum Home {
        static var goodMorning: String { String(localized: "home.goodMorning", defaultValue: "Good Morning", bundle: LanguageManager.shared.bundle) }
        static var goodAfternoon: String { String(localized: "home.goodAfternoon", defaultValue: "Good Afternoon", bundle: LanguageManager.shared.bundle) }
        static var goodEvening: String { String(localized: "home.goodEvening", defaultValue: "Good Evening", bundle: LanguageManager.shared.bundle) }
        static var schedule: String { String(localized: "home.schedule", defaultValue: "Schedule", bundle: LanguageManager.shared.bundle) }
        static var calendar: String { String(localized: "home.calendar", defaultValue: "Calendar", bundle: LanguageManager.shared.bundle) }
        static var today: String { String(localized: "home.today", defaultValue: "Today", bundle: LanguageManager.shared.bundle) }
        static var thisWeek: String { String(localized: "home.thisWeek", defaultValue: "This Week", bundle: LanguageManager.shared.bundle) }
        static var nextWeek: String { String(localized: "home.nextWeek", defaultValue: "Next Week", bundle: LanguageManager.shared.bundle) }
        static var later: String { String(localized: "home.later", defaultValue: "Later", bundle: LanguageManager.shared.bundle) }
        static var noScheduledWorkToday: String { String(localized: "home.noScheduledWorkToday", defaultValue: "No scheduled work today", bundle: LanguageManager.shared.bundle) }
        static var noScheduledWork: String { String(localized: "home.noScheduledWork", defaultValue: "No scheduled work", bundle: LanguageManager.shared.bundle) }
        static var viewSites: String { String(localized: "home.viewSites", defaultValue: "View Sites", bundle: LanguageManager.shared.bundle) }
        static var viewSitesPrompt: String { String(localized: "home.viewSitesPrompt", defaultValue: "Tap a scheduled work block above to go to that site, or browse all your sites:", bundle: LanguageManager.shared.bundle) }
        static var switchSite: String { String(localized: "home.switchSite", defaultValue: "Switch Site", bundle: LanguageManager.shared.bundle) }
        static var switchAction: String { String(localized: "home.switchAction", defaultValue: "Switch", bundle: LanguageManager.shared.bundle) }
        static var currentSite: String { String(localized: "home.currentSite", defaultValue: "Current Site", bundle: LanguageManager.shared.bundle) }
        static var calendarToday: String { String(localized: "home.calendarToday", defaultValue: "Today", bundle: LanguageManager.shared.bundle) }
        static var calendarDay: String { String(localized: "home.calendarDay", defaultValue: "Day", bundle: LanguageManager.shared.bundle) }
        static var calendarWeek: String { String(localized: "home.calendarWeek", defaultValue: "Week", bundle: LanguageManager.shared.bundle) }
        static var calendarMonth: String { String(localized: "home.calendarMonth", defaultValue: "Month", bundle: LanguageManager.shared.bundle) }
        static var calendarView: String { String(localized: "home.calendarView", defaultValue: "View", bundle: LanguageManager.shared.bundle) }

        static func switchToSitePrompt(siteName: String) -> String {
            String(localized: "home.switchToSitePrompt", defaultValue: "Would you like to switch to \(siteName)?", bundle: LanguageManager.shared.bundle)
        }
        static var switchToSitePromptGeneric: String { String(localized: "home.switchToSitePromptGeneric", defaultValue: "Would you like to switch to this site?", bundle: LanguageManager.shared.bundle) }
        static func alreadyOnSite(siteName: String) -> String {
            String(localized: "home.alreadyOnSite", defaultValue: "You're already on \(siteName)", bundle: LanguageManager.shared.bundle)
        }
        static var alreadyOnSiteGeneric: String { String(localized: "home.alreadyOnSiteGeneric", defaultValue: "You're already on this site", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Settings
    enum Settings {
        static var title: String { String(localized: "settings.title", defaultValue: "Settings", bundle: LanguageManager.shared.bundle) }
        static var account: String { String(localized: "settings.account", defaultValue: "Account", bundle: LanguageManager.shared.bundle) }
        static var userID: String { String(localized: "settings.userID", defaultValue: "User ID", bundle: LanguageManager.shared.bundle) }
        static var security: String { String(localized: "settings.security", defaultValue: "Security", bundle: LanguageManager.shared.bundle) }
        static var syncAndNetwork: String { String(localized: "settings.syncAndNetwork", defaultValue: "Sync & Network", bundle: LanguageManager.shared.bundle) }
        static var diagnostics: String { String(localized: "settings.diagnostics", defaultValue: "Diagnostics", bundle: LanguageManager.shared.bundle) }
        static var about: String { String(localized: "settings.about", defaultValue: "About", bundle: LanguageManager.shared.bundle) }
        static var language: String { String(localized: "settings.language", defaultValue: "Language", bundle: LanguageManager.shared.bundle) }
        static func switchLanguageMessage(_ language: String) -> String {
            String(localized: "settings.switchLanguageMessage", defaultValue: "Switch language to \(language)?", bundle: LanguageManager.shared.bundle)
        }
        static var equipmentLibrary: String { String(localized: "settings.equipmentLibrary", defaultValue: "Equipment Library", bundle: LanguageManager.shared.bundle) }
        static var loadLatestEquipmentLibrary: String { String(localized: "settings.loadLatestEquipmentLibrary", defaultValue: "Load Latest Equipment Library", bundle: LanguageManager.shared.bundle) }
        static var downloadEquipmentLibraryConfirmation: String { String(localized: "settings.downloadEquipmentLibraryConfirmation", defaultValue: "Download Equipment Library?", bundle: LanguageManager.shared.bundle) }
        static var downloadEquipmentLibraryMessage: String { String(localized: "settings.downloadEquipmentLibraryMessage", defaultValue: "This will download the latest equipment library data. The download may take a few minutes depending on your connection.", bundle: LanguageManager.shared.bundle) }
        static var download: String { String(localized: "settings.download", defaultValue: "Download", bundle: LanguageManager.shared.bundle) }
        static var goOnlineToDownload: String { String(localized: "settings.goOnlineToDownload", defaultValue: "Go online to download the equipment library", bundle: LanguageManager.shared.bundle) }
        static var notYetDownloaded: String { String(localized: "settings.notYetDownloaded", defaultValue: "Not yet downloaded", bundle: LanguageManager.shared.bundle) }
        static func lastUpdated(_ relativeDate: String) -> String {
            String(localized: "settings.lastUpdated", defaultValue: "Last updated \(relativeDate)", bundle: LanguageManager.shared.bundle)
        }
        static var syncQueueAnalyzer: String { String(localized: "settings.syncQueueAnalyzer", defaultValue: "Sync Queue Analyzer", bundle: LanguageManager.shared.bundle) }
        static var managePendingSyncOps: String { String(localized: "settings.managePendingSyncOps", defaultValue: "Manage pending sync operations", bundle: LanguageManager.shared.bundle) }
        static var networkMode: String { String(localized: "settings.networkMode", defaultValue: "Network Mode", bundle: LanguageManager.shared.bundle) }
        static var connected: String { String(localized: "settings.connected", defaultValue: "Connected", bundle: LanguageManager.shared.bundle) }
        static var offlineMode: String { String(localized: "settings.offlineMode", defaultValue: "Offline Mode", bundle: LanguageManager.shared.bundle) }
        static var syncing: String { String(localized: "settings.syncing", defaultValue: "Syncing...", bundle: LanguageManager.shared.bundle) }
        static var photoStorageDiagnostics: String { String(localized: "settings.photoStorageDiagnostics", defaultValue: "Photo Storage Diagnostics", bundle: LanguageManager.shared.bundle) }
        static var checkPhotoStorageHealth: String { String(localized: "settings.checkPhotoStorageHealth", defaultValue: "Check photo storage health", bundle: LanguageManager.shared.bundle) }
        static var logoutConfirmation: String { String(localized: "settings.logoutConfirmation", defaultValue: "Logout Confirmation", bundle: LanguageManager.shared.bundle) }
        static var logoutMessage: String { String(localized: "settings.logoutMessage", defaultValue: "Are you sure you want to logout? You will need to sign in again to access your data.", bundle: LanguageManager.shared.bundle) }
        static var logoutError: String { String(localized: "settings.logoutError", defaultValue: "Logout Error", bundle: LanguageManager.shared.bundle) }
        static var loggingOut: String { String(localized: "settings.loggingOut", defaultValue: "Logging out...", bundle: LanguageManager.shared.bundle) }
        static var useBiometricForLogin: String { String(localized: "settings.useBiometricForLogin", defaultValue: "Use biometric authentication for quick login", bundle: LanguageManager.shared.bundle) }
        static var enterPasswordToSignIn: String { String(localized: "settings.enterPasswordToSignIn", defaultValue: "You will need to enter your email and password to sign in next time.", bundle: LanguageManager.shared.bundle) }
        static var invalidPassword: String { String(localized: "settings.invalidPassword", defaultValue: "Invalid password. Please try again.", bundle: LanguageManager.shared.bundle) }
        static var enterYourPassword: String { String(localized: "settings.enterYourPassword", defaultValue: "Enter your password", bundle: LanguageManager.shared.bundle) }

        static func biometricLogin(type: String) -> String {
            String(localized: "settings.biometricLogin", defaultValue: "\(type) Login", bundle: LanguageManager.shared.bundle)
        }
        static func enableBiometric(type: String) -> String {
            String(localized: "settings.enableBiometric", defaultValue: "Enable \(type)", bundle: LanguageManager.shared.bundle)
        }
        static func disableBiometric(type: String) -> String {
            String(localized: "settings.disableBiometric", defaultValue: "Disable \(type)?", bundle: LanguageManager.shared.bundle)
        }
        static func enterPasswordForBiometric(type: String) -> String {
            String(localized: "settings.enterPasswordForBiometric", defaultValue: "Enter your password to enable \(type) for quick login.", bundle: LanguageManager.shared.bundle)
        }
        static func syncPendingWarning(count: Int) -> String {
            String(localized: "settings.syncPendingWarning", defaultValue: "Please sync \(count) pending \(count == 1 ? "operation" : "operations") before logging out", bundle: LanguageManager.shared.bundle)
        }
        // ZP-1847: pending queue alone does not block logout — message is informational only.
        static func syncPendingNote(count: Int) -> String {
            String(localized: "settings.syncPendingNote", defaultValue: "\(count) \(count == 1 ? "change is" : "changes are") still queued. They will sync after you log back in.", bundle: LanguageManager.shared.bundle)
        }
        static var logoutRequiresOnline: String { String(localized: "settings.logoutRequiresOnline", defaultValue: "You must be online to log out.", bundle: LanguageManager.shared.bundle) }
        static var logoutBlockedSyncing: String { String(localized: "settings.logoutBlockedSyncing", defaultValue: "A sync is in progress. Please wait until it finishes.", bundle: LanguageManager.shared.bundle) }
        static var storage: String { String(localized: "settings.storage", defaultValue: "Storage", bundle: LanguageManager.shared.bundle) }
        static var clearImageCache: String { String(localized: "settings.clearImageCache", defaultValue: "Clear Image Cache", bundle: LanguageManager.shared.bundle) }
        static func cacheSize(_ size: String) -> String {
            String(localized: "settings.cacheSize", defaultValue: "Cache size: \(size)", bundle: LanguageManager.shared.bundle)
        }
        static var cacheClearedSuccessfully: String { String(localized: "settings.cacheClearedSuccessfully", defaultValue: "Cache cleared successfully", bundle: LanguageManager.shared.bundle) }
        static var clearImageCacheMessage: String { String(localized: "settings.clearImageCacheMessage", defaultValue: "This will clear all cached images. They will be re-downloaded when needed.", bundle: LanguageManager.shared.bundle) }
        static var clearImageCacheError: String { String(localized: "settings.clearImageCacheError", defaultValue: "Failed to clear image cache", bundle: LanguageManager.shared.bundle) }
        static var sldViewerVersion: String { String(localized: "settings.sldViewerVersion", defaultValue: "SLD Viewer Version", bundle: LanguageManager.shared.bundle) }

        static var sessionAnalytics: String { String(localized: "settings.sessionAnalytics", defaultValue: "Session Analytics", bundle: LanguageManager.shared.bundle) }
        static var sessionRecording: String { String(localized: "settings.sessionRecording", defaultValue: "Session Recording", bundle: LanguageManager.shared.bundle) }
        static var sessionRecordingHint: String { String(localized: "settings.sessionRecordingHint", defaultValue: "Keeping this off improves performance and reduces battery usage.", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Auth / Login
    enum Auth {
        static var welcome: String { String(localized: "auth.welcome", defaultValue: "Welcome", bundle: LanguageManager.shared.bundle) }
        static var enterCompanyCode: String { String(localized: "auth.enterCompanyCode", defaultValue: "Enter your company code to get started", bundle: LanguageManager.shared.bundle) }
        static var companyCode: String { String(localized: "auth.companyCode", defaultValue: "Company Code", bundle: LanguageManager.shared.bundle) }
        static var companyCodeHelp: String { String(localized: "auth.companyCodeHelp", defaultValue: "Company Code Help", bundle: LanguageManager.shared.bundle) }
        static var companyCodeExplanation: String { String(localized: "auth.companyCodeExplanation", defaultValue: "Your company code is the subdomain from your web login URL.", bundle: LanguageManager.shared.bundle) }
        static var companyCodeFormat: String { String(localized: "auth.companyCodeFormat", defaultValue: "Your login format is:", bundle: LanguageManager.shared.bundle) }
        static var companyCodeTemplate: String { String(localized: "auth.companyCodeTemplate", defaultValue: "<company-code>.<alliance-partner-key>", bundle: LanguageManager.shared.bundle) }
        static var companyCodeDefaultPartner: String { String(localized: "auth.companyCodeDefaultPartner", defaultValue: "If the partner key is missing, we use egalvanic by default.", bundle: LanguageManager.shared.bundle) }
        static var companyCodeExample: String { String(localized: "auth.companyCodeExample", defaultValue: "Example:", bundle: LanguageManager.shared.bundle) }
        static var companyCodeExampleText: String { String(localized: "auth.companyCodeExampleText", defaultValue: "If your code is acme, your login becomes:", bundle: LanguageManager.shared.bundle) }
        static var companyCodeExampleValue: String { String(localized: "auth.companyCodeExampleValue", defaultValue: "acme.egalvanic", bundle: LanguageManager.shared.bundle) }
        static var companyCodeNote: String { String(localized: "auth.companyCodeNote", defaultValue: "Note: Company codes are not case-sensitive.", bundle: LanguageManager.shared.bundle) }
        static var companyCodePlaceholder: String { String(localized: "auth.companyCodePlaceholder", defaultValue: "(e.g. acme.egalvanic)", bundle: LanguageManager.shared.bundle) }
        static var signIn: String { String(localized: "auth.signIn", defaultValue: "Sign In", bundle: LanguageManager.shared.bundle) }
        static var signInToContinue: String { String(localized: "auth.signInToContinue", defaultValue: "Sign in to continue", bundle: LanguageManager.shared.bundle) }
        static var changeCompany: String { String(localized: "auth.changeCompany", defaultValue: "Change Company", bundle: LanguageManager.shared.bundle) }
        static var enterYourEmail: String { String(localized: "auth.enterYourEmail", defaultValue: "Enter your email", bundle: LanguageManager.shared.bundle) }
        static var enterYourPassword: String { String(localized: "auth.enterYourPassword", defaultValue: "Enter your password", bundle: LanguageManager.shared.bundle) }

        static func signInWithBiometric(type: String) -> String {
            String(localized: "auth.signInWithBiometric", defaultValue: "Sign in with \(type)", bundle: LanguageManager.shared.bundle)
        }
        static func enableBiometricForLogin(type: String) -> String {
            String(localized: "auth.enableBiometricForLogin", defaultValue: "Enable \(type) for Login?", bundle: LanguageManager.shared.bundle)
        }
        static func biometricQuickSignIn(type: String) -> String {
            String(localized: "auth.biometricQuickSignIn", defaultValue: "Use \(type) to quickly sign in next time without entering your password.", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Assets
    enum Assets {
        static var newAsset: String { String(localized: "assets.newAsset", defaultValue: "New Asset", bundle: LanguageManager.shared.bundle) }
        static var createAsset: String { String(localized: "assets.createAsset", defaultValue: "Create Asset", bundle: LanguageManager.shared.bundle) }
        static var creatingAsset: String { String(localized: "assets.creatingAsset", defaultValue: "Creating Asset...", bundle: LanguageManager.shared.bundle) }
        static var assetDetails: String { String(localized: "assets.assetDetails", defaultValue: "Asset Details", bundle: LanguageManager.shared.bundle) }
        static var assetClass: String { String(localized: "assets.assetClass", defaultValue: "Asset Class", bundle: LanguageManager.shared.bundle) }
        static var selectAssetClass: String { String(localized: "assets.selectAssetClass", defaultValue: "Select asset class", bundle: LanguageManager.shared.bundle) }
        static var assetSubtype: String { String(localized: "assets.assetSubtype", defaultValue: "Asset Subtype (Optional)", bundle: LanguageManager.shared.bundle) }
        static var selectAssetSubtype: String { String(localized: "assets.selectAssetSubtype", defaultValue: "Select asset subtype", bundle: LanguageManager.shared.bundle) }
        static var qrCode: String { String(localized: "assets.qrCode", defaultValue: "QR Code", bundle: LanguageManager.shared.bundle) }
        static var qrCodePreview: String { String(localized: "assets.qrCodePreview", defaultValue: "QR Code Preview", bundle: LanguageManager.shared.bundle) }
        static var enterOrScanQR: String { String(localized: "assets.enterOrScanQR", defaultValue: "Enter or scan QR code", bundle: LanguageManager.shared.bundle) }
        static var scan: String { String(localized: "assets.scan", defaultValue: "Scan", bundle: LanguageManager.shared.bundle) }
        static var assetPhotos: String { String(localized: "assets.assetPhotos", defaultValue: "Asset Photos", bundle: LanguageManager.shared.bundle) }
        static var infraredPhotos: String { String(localized: "assets.infraredPhotos", defaultValue: "Infrared Photos", bundle: LanguageManager.shared.bundle) }
        static var irPhotoFilename: String { String(localized: "assets.irPhotoFilename", defaultValue: "IR Photo Filename", bundle: LanguageManager.shared.bundle) }
        static var visualPhotoFilename: String { String(localized: "assets.visualPhotoFilename", defaultValue: "Visual Photo Filename", bundle: LanguageManager.shared.bundle) }
        static var visualPhotoNotRequired: String { String(localized: "assets.visualPhotoNotRequired", defaultValue: "Visual Photo (Not required for FLIR-IND)", bundle: LanguageManager.shared.bundle) }
        static var addIRPhotoPair: String { String(localized: "assets.addIRPhotoPair", defaultValue: "Add IR Photo Pair", bundle: LanguageManager.shared.bundle) }
        static var newIRPhotos: String { String(localized: "assets.newIRPhotos", defaultValue: "New IR Photos", bundle: LanguageManager.shared.bundle) }
        static var quick: String { String(localized: "assets.quick", defaultValue: "Quick", bundle: LanguageManager.shared.bundle) }
        static var detailed: String { String(localized: "assets.detailed", defaultValue: "Detailed", bundle: LanguageManager.shared.bundle) }
        static var conditionOfMaintenance: String { String(localized: "assets.conditionOfMaintenance", defaultValue: "Condition of Maintenance", bundle: LanguageManager.shared.bundle) }
        static var requiredFields: String { String(localized: "assets.requiredFields", defaultValue: "Required Fields", bundle: LanguageManager.shared.bundle) }
        static var assetType: String { String(localized: "assets.assetType", defaultValue: "Asset Type", bundle: LanguageManager.shared.bundle) }
        static var calculator: String { String(localized: "assets.calculator", defaultValue: "Calculator", bundle: LanguageManager.shared.bundle) }
        static var serviceability: String { String(localized: "assets.serviceability", defaultValue: "Serviceability (Optional)", bundle: LanguageManager.shared.bundle) }
        static var physicalCOMLabelStatus: String { String(localized: "assets.physicalCOMLabelStatus", defaultValue: "Physical COM label status", bundle: LanguageManager.shared.bundle) }
        static var selectServiceability: String { String(localized: "assets.selectServiceability", defaultValue: "Select serviceability status", bundle: LanguageManager.shared.bundle) }
        static var autoLinkTasks: String { String(localized: "assets.autoLinkTasks", defaultValue: "Auto-Link Tasks", bundle: LanguageManager.shared.bundle) }
        static var coreAttributes: String { String(localized: "assets.coreAttributes", defaultValue: "Core Attributes", bundle: LanguageManager.shared.bundle) }
        static var customAttributes: String { String(localized: "assets.customAttributes", defaultValue: "Custom Attributes", bundle: LanguageManager.shared.bundle) }
        static var saveFailed: String { String(localized: "assets.saveFailed", defaultValue: "Save Failed", bundle: LanguageManager.shared.bundle) }
        static var irOnly: String { String(localized: "assets.irOnly", defaultValue: "IR Only", bundle: LanguageManager.shared.bundle) }
        static var creationMode: String { String(localized: "assets.creationMode", defaultValue: "Creation Mode", bundle: LanguageManager.shared.bundle) }
        // ZP-2415: Commercial section grouping COM, Suggested PM Plan, Replacement Cost.
        static var commercial: String { String(localized: "assets.commercial", defaultValue: "Commercial", bundle: LanguageManager.shared.bundle) }
        static var replacementCost: String { String(localized: "assets.replacementCost", defaultValue: "Replacement Cost", bundle: LanguageManager.shared.bundle) }
        static var replacementCostPlaceholder: String { String(localized: "assets.replacementCostPlaceholder", defaultValue: "Enter cost", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Common (extended)
    // Additional common strings used across multiple views
    enum CommonExtra {
        static var error: String { String(localized: "common.error", defaultValue: "Error", bundle: LanguageManager.shared.bundle) }
        static var close: String { String(localized: "common.close", defaultValue: "Close", bundle: LanguageManager.shared.bundle) }
        static var update: String { String(localized: "common.update", defaultValue: "Update", bundle: LanguageManager.shared.bundle) }
        static var remove: String { String(localized: "common.remove", defaultValue: "Remove", bundle: LanguageManager.shared.bundle) }
        static var upload: String { String(localized: "common.upload", defaultValue: "Upload", bundle: LanguageManager.shared.bundle) }
        static var updating: String { String(localized: "common.updating", defaultValue: "Updating...", bundle: LanguageManager.shared.bundle) }
        static var total: String { String(localized: "common.total", defaultValue: "Total", bundle: LanguageManager.shared.bundle) }
        static var open: String { String(localized: "common.open", defaultValue: "Open", bundle: LanguageManager.shared.bundle) }
        static var closed: String { String(localized: "common.closed", defaultValue: "Closed", bundle: LanguageManager.shared.bundle) }
        static var online: String { String(localized: "common.online", defaultValue: "Online", bundle: LanguageManager.shared.bundle) }
        static var offline: String { String(localized: "common.offline", defaultValue: "Offline", bundle: LanguageManager.shared.bundle) }
        static var noLocation: String { String(localized: "common.noLocation", defaultValue: "No Location", bundle: LanguageManager.shared.bundle) }
        static var searchAssets: String { String(localized: "common.searchAssets", defaultValue: "Search assets...", bundle: LanguageManager.shared.bundle) }
        static var existingAsset: String { String(localized: "common.existingAsset", defaultValue: "Existing Asset", bundle: LanguageManager.shared.bundle) }
        static var createNewAsset: String { String(localized: "common.createNewAsset", defaultValue: "Create New Asset", bundle: LanguageManager.shared.bundle) }
        static var selectAsset: String { String(localized: "common.selectAsset", defaultValue: "Select Asset", bundle: LanguageManager.shared.bundle) }
        static var anErrorOccurred: String { String(localized: "common.anErrorOccurred", defaultValue: "An error occurred", bundle: LanguageManager.shared.bundle) }
        static var untitledIssue: String { String(localized: "common.untitledIssue", defaultValue: "Untitled Issue", bundle: LanguageManager.shared.bundle) }
        static var add: String { String(localized: "common.add", defaultValue: "Add", bundle: LanguageManager.shared.bundle) }
        static func addCount(_ count: Int) -> String {
            String(localized: "common.addCount", defaultValue: "Add (\(count))", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Alerts
    enum Alerts {
        static var networkConnectionRequired: String { String(localized: "alerts.networkConnectionRequired", defaultValue: "Network Connection Required", bundle: LanguageManager.shared.bundle) }
        static var discardChanges: String { String(localized: "alerts.discardChanges", defaultValue: "Discard Changes?", bundle: LanguageManager.shared.bundle) }
        static var discardChangesMessage: String { String(localized: "alerts.discardChangesMessage", defaultValue: "You have unsaved changes. Are you sure you want to discard them?", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Sessions / Work Orders
    enum Sessions {
        // Work Order general
        static var workOrders: String { String(localized: "sessions.workOrders", defaultValue: "Work Orders", bundle: LanguageManager.shared.bundle) }
        static var newWorkOrder: String { String(localized: "sessions.newWorkOrder", defaultValue: "New Work Order", bundle: LanguageManager.shared.bundle) }
        static var startNewWorkOrder: String { String(localized: "sessions.startNewWorkOrder", defaultValue: "Start New Work Order", bundle: LanguageManager.shared.bundle) }
        static var endCurrentFirst: String { String(localized: "sessions.endCurrentFirst", defaultValue: "End current work order session first", bundle: LanguageManager.shared.bundle) }
        static var beginCapturing: String { String(localized: "sessions.beginCapturing", defaultValue: "Begin capturing IR photos, issues, and tasks", bundle: LanguageManager.shared.bundle) }
        static var noWorkOrdersYet: String { String(localized: "sessions.noWorkOrdersYet", defaultValue: "No Work Orders Yet", bundle: LanguageManager.shared.bundle) }
        static var startFirstWorkOrder: String { String(localized: "sessions.startFirstWorkOrder", defaultValue: "Start your first work order", bundle: LanguageManager.shared.bundle) }
        static var createFirstWorkOrder: String { String(localized: "sessions.createFirstWorkOrder", defaultValue: "Create First Work Order", bundle: LanguageManager.shared.bundle) }
        static var allWorkOrders: String { String(localized: "sessions.allWorkOrders", defaultValue: "All Work Orders", bundle: LanguageManager.shared.bundle) }
        static var availableWorkOrders: String { String(localized: "sessions.availableWorkOrders", defaultValue: "Available Work Orders", bundle: LanguageManager.shared.bundle) }
        static var showAvailableOnly: String { String(localized: "sessions.showAvailableOnly", defaultValue: "Show Available Only", bundle: LanguageManager.shared.bundle) }
        static var showAll: String { String(localized: "sessions.showAll", defaultValue: "Show All", bundle: LanguageManager.shared.bundle) }
        static var activeBadge: String { String(localized: "sessions.activeBadge", defaultValue: "ACTIVE", bundle: LanguageManager.shared.bundle) }
        static var completed: String { String(localized: "sessions.completed", defaultValue: "Completed", bundle: LanguageManager.shared.bundle) }
        static var end: String { String(localized: "sessions.end", defaultValue: "End", bundle: LanguageManager.shared.bundle) }
        static var start: String { String(localized: "sessions.start", defaultValue: "Start", bundle: LanguageManager.shared.bundle) }

        // Close/End Work Order
        static var closeWorkOrder: String { String(localized: "sessions.closeWorkOrder", defaultValue: "Close Work Order?", bundle: LanguageManager.shared.bundle) }
        static var closeWorkOrderButton: String { String(localized: "sessions.closeWorkOrderButton", defaultValue: "Close Work Order", bundle: LanguageManager.shared.bundle) }
        static var closingWorkOrder: String { String(localized: "sessions.closingWorkOrder", defaultValue: "Closing work order...", bundle: LanguageManager.shared.bundle) }
        static var creatingWorkOrder: String { String(localized: "sessions.creatingWorkOrder", defaultValue: "Creating work order...", bundle: LanguageManager.shared.bundle) }
        static var closeWorkOrderDescription: String { String(localized: "sessions.closeWorkOrderDescription", defaultValue: "This will end the current work order and prevent new photos from being added to it.", bundle: LanguageManager.shared.bundle) }
        static var changesWillSyncWhenOnline: String { String(localized: "sessions.changesWillSyncWhenOnline", defaultValue: "Changes will sync when online", bundle: LanguageManager.shared.bundle) }
        static var workOrderWillBeSynced: String { String(localized: "sessions.workOrderWillBeSynced", defaultValue: "Work order will be synced when online", bundle: LanguageManager.shared.bundle) }
        static var quickCountLabel: String { String(localized: "sessions.quickCount", defaultValue: "Quick Count", bundle: LanguageManager.shared.bundle) }
        static var photoWalkthroughLabel: String { String(localized: "sessions.photoWalkthrough", defaultValue: "Photo Walkthrough", bundle: LanguageManager.shared.bundle) }
        static var linkExistingAssetLabel: String { String(localized: "sessions.linkExistingAsset", defaultValue: "Link Existing Asset", bundle: LanguageManager.shared.bundle) }
        static var collectAFData: String { String(localized: "sessions.collectAFData", defaultValue: "Collect AF Data", bundle: LanguageManager.shared.bundle) }
        static var collectCOMData: String { String(localized: "sessions.collectCOMData", defaultValue: "Collect COM Data", bundle: LanguageManager.shared.bundle) }
        static var removeFromWorkOrderLabel: String { String(localized: "sessions.removeFromWorkOrder", defaultValue: "Remove from Work Order", bundle: LanguageManager.shared.bundle) }
        static var tryClearingFilter: String { String(localized: "sessions.tryClearingFilter", defaultValue: "Try clearing the filter to see all tasks", bundle: LanguageManager.shared.bundle) }
        static var noEquipmentAvailable: String { String(localized: "sessions.noEquipmentAvailable", defaultValue: "No equipment available", bundle: LanguageManager.shared.bundle) }
        static var workOrderActiveDescription: String { String(localized: "sessions.workOrderActiveDescription", defaultValue: "This work order will remain active until you explicitly close it. All IR photos, issues and tasks added will be associated with this work order.", bundle: LanguageManager.shared.bundle) }
        static var endWorkOrderSession: String { String(localized: "sessions.endWorkOrderSession", defaultValue: "End Work Order Session?", bundle: LanguageManager.shared.bundle) }
        static var endSession: String { String(localized: "sessions.endSession", defaultValue: "End Session", bundle: LanguageManager.shared.bundle) }
        static var startWorkOrderSession: String { String(localized: "sessions.startWorkOrderSession", defaultValue: "Start Work Order Session?", bundle: LanguageManager.shared.bundle) }
        static var startSession: String { String(localized: "sessions.startSession", defaultValue: "Start Session", bundle: LanguageManager.shared.bundle) }
        static var closingSession: String { String(localized: "sessions.closingSession", defaultValue: "Closing session...", bundle: LanguageManager.shared.bundle) }
        static var closeSession: String { String(localized: "sessions.closeSession", defaultValue: "Close Session", bundle: LanguageManager.shared.bundle) }
        static var closeSessionDescription: String { String(localized: "sessions.closeSessionDescription", defaultValue: "This will end the session and prevent new photos from being added. This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static var changesWillSyncNote: String { String(localized: "sessions.changesWillSyncNote", defaultValue: "\nNote: Changes will be synced when you're back online.", bundle: LanguageManager.shared.bundle) }
        static func endSessionConfirm(name: String) -> String {
            String(localized: "sessions.endSessionConfirm", defaultValue: "Are you sure you want to end the '\(name)' session?", bundle: LanguageManager.shared.bundle)
        }
        static func startSessionConfirm(name: String) -> String {
            String(localized: "sessions.startSessionConfirm", defaultValue: "Start '\(name)' as the active work order session?", bundle: LanguageManager.shared.bundle)
        }
        static func workOrderName(name: String) -> String {
            String(localized: "sessions.workOrderName", defaultValue: "Work Order: \(name)", bundle: LanguageManager.shared.bundle)
        }
        static func closeSessionTitle(name: String) -> String {
            String(localized: "sessions.closeSessionTitle", defaultValue: "Close \(name)?", bundle: LanguageManager.shared.bundle)
        }

        // Session Details
        static var workOrderDetailsHeader: String { String(localized: "sessions.workOrderDetailsHeader", defaultValue: "WORK ORDER DETAILS", bundle: LanguageManager.shared.bundle) }
        static var activeWorkOrder: String { String(localized: "sessions.activeWorkOrder", defaultValue: "Active Work Order", bundle: LanguageManager.shared.bundle) }
        static var informationHeader: String { String(localized: "sessions.informationHeader", defaultValue: "INFORMATION", bundle: LanguageManager.shared.bundle) }
        static var irPhotoType: String { String(localized: "sessions.irPhotoType", defaultValue: "IR Photo Type", bundle: LanguageManager.shared.bundle) }
        static var started: String { String(localized: "sessions.started", defaultValue: "Started", bundle: LanguageManager.shared.bundle) }
        static var quickQRAction: String { String(localized: "sessions.quickQRAction", defaultValue: "Quick QR Action", bundle: LanguageManager.shared.bundle) }
        static var refreshFailed: String { String(localized: "sessions.refreshFailed", defaultValue: "Refresh Failed", bundle: LanguageManager.shared.bundle) }
        static var goOnline: String { String(localized: "sessions.goOnline", defaultValue: "Go Online", bundle: LanguageManager.shared.bundle) }
        static var regenerateReport: String { String(localized: "sessions.regenerateReport", defaultValue: "Regenerate Report?", bundle: LanguageManager.shared.bundle) }
        static var regenerate: String { String(localized: "sessions.regenerate", defaultValue: "Regenerate", bundle: LanguageManager.shared.bundle) }
        static var regenerateDescription: String { String(localized: "sessions.regenerateDescription", defaultValue: "This will generate a new report with the latest data. The previous report will be replaced.", bundle: LanguageManager.shared.bundle) }
        static var incompleteLabel: String { String(localized: "sessions.incomplete", defaultValue: "Incomplete", bundle: LanguageManager.shared.bundle) }
        static var openIssues: String { String(localized: "sessions.openIssues", defaultValue: "Open Issues", bundle: LanguageManager.shared.bundle) }

        // Detail tabs
        static var details: String { String(localized: "sessions.details", defaultValue: "Details", bundle: LanguageManager.shared.bundle) }
        static var tasks: String { String(localized: "sessions.tasks", defaultValue: "Tasks", bundle: LanguageManager.shared.bundle) }
        static var issues: String { String(localized: "sessions.issues", defaultValue: "Issues", bundle: LanguageManager.shared.bundle) }
        static var files: String { String(localized: "sessions.files", defaultValue: "Files", bundle: LanguageManager.shared.bundle) }

        // Creation form
        static var workOrderConfiguration: String { String(localized: "sessions.workOrderConfiguration", defaultValue: "Work Order Configuration", bundle: LanguageManager.shared.bundle) }
        static var workOrderNameField: String { String(localized: "sessions.workOrderNameField", defaultValue: "Work Order Name", bundle: LanguageManager.shared.bundle) }
        static var photoType: String { String(localized: "sessions.photoType", defaultValue: "Photo Type", bundle: LanguageManager.shared.bundle) }
        static var team: String { String(localized: "sessions.team", defaultValue: "Team", bundle: LanguageManager.shared.bundle) }
        static var assignee: String { String(localized: "sessions.assignee", defaultValue: "Field technician", bundle: LanguageManager.shared.bundle) }
        static var reporter: String { String(localized: "sessions.reporter", defaultValue: "Back office", bundle: LanguageManager.shared.bundle) }
        static var loadingUsers: String { String(localized: "sessions.loadingUsers", defaultValue: "Loading users...", bundle: LanguageManager.shared.bundle) }
        static var teamMembersOnline: String { String(localized: "sessions.teamMembersOnline", defaultValue: "Team members can be added when online", bundle: LanguageManager.shared.bundle) }
        static var searchUsers: String { String(localized: "sessions.searchUsers", defaultValue: "Search users...", bundle: LanguageManager.shared.bundle) }
        static var role: String { String(localized: "sessions.role", defaultValue: "Role", bundle: LanguageManager.shared.bundle) }

        // Assets in session
        static var assetsInRoom: String { String(localized: "sessions.assetsInRoom", defaultValue: "Assets in Room", bundle: LanguageManager.shared.bundle) }
        static var noAssets: String { String(localized: "sessions.noAssets", defaultValue: "No Assets", bundle: LanguageManager.shared.bundle) }
        static var tapPlusToAddAssets: String { String(localized: "sessions.tapPlusToAddAssets", defaultValue: "Tap the + button to add assets to this room", bundle: LanguageManager.shared.bundle) }
        static var tapPlusToLinkAssets: String { String(localized: "sessions.tapPlusToLinkAssets", defaultValue: "Tap the + button to link assets to this session", bundle: LanguageManager.shared.bundle) }
        static var noAvailableAssets: String { String(localized: "sessions.noAvailableAssets", defaultValue: "No Available Assets", bundle: LanguageManager.shared.bundle) }
        static var allAssetsLinked: String { String(localized: "sessions.allAssetsLinked", defaultValue: "All assets in this room are already linked to this session", bundle: LanguageManager.shared.bundle) }
        static var noMatchingAssets: String { String(localized: "sessions.noMatchingAssets", defaultValue: "No matching assets found in this room", bundle: LanguageManager.shared.bundle) }
        static var allAssetsLinkedToSession: String { String(localized: "sessions.allAssetsLinkedToSession", defaultValue: "All assets are already linked to this session", bundle: LanguageManager.shared.bundle) }
        static var linkExistingAssets: String { String(localized: "sessions.linkExistingAssets", defaultValue: "Link Existing Assets", bundle: LanguageManager.shared.bundle) }
        static var addAssets: String { String(localized: "sessions.addAssets", defaultValue: "Add Assets", bundle: LanguageManager.shared.bundle) }
        static var createAndLinkAsset: String { String(localized: "sessions.createAndLinkAsset", defaultValue: "Create a new asset and link it to this session:", bundle: LanguageManager.shared.bundle) }
        static var addingAssets: String { String(localized: "sessions.addingAssets", defaultValue: "Adding assets...", bundle: LanguageManager.shared.bundle) }
        static var removeFromSession: String { String(localized: "sessions.removeFromSession", defaultValue: "Remove from Session", bundle: LanguageManager.shared.bundle) }
        static var removeAssetConfirm: String { String(localized: "sessions.removeAssetConfirm", defaultValue: "Remove this asset from the session?", bundle: LanguageManager.shared.bundle) }
        static var removeAssetMessage: String { String(localized: "sessions.removeAssetMessage", defaultValue: "The asset will remain in your library but won't be linked to this session.", bundle: LanguageManager.shared.bundle) }

        // Context menu actions
        static var collectData: String { String(localized: "sessions.collectData", defaultValue: "Collect Data", bundle: LanguageManager.shared.bundle) }
        static var addTask: String { String(localized: "sessions.addTask", defaultValue: "Add Task", bundle: LanguageManager.shared.bundle) }
        static var addIRPhotos: String { String(localized: "sessions.addIRPhotos", defaultValue: "Add IR Photos", bundle: LanguageManager.shared.bundle) }
        static var addIssue: String { String(localized: "sessions.addIssue", defaultValue: "Add Issue", bundle: LanguageManager.shared.bundle) }
        static var linkTask: String { String(localized: "sessions.linkTask", defaultValue: "Link Task", bundle: LanguageManager.shared.bundle) }
        static var editOCP: String { String(localized: "sessions.editOCP", defaultValue: "Edit OCP", bundle: LanguageManager.shared.bundle) }
        static var editConnections: String { String(localized: "sessions.editConnections", defaultValue: "Edit Connections", bundle: LanguageManager.shared.bundle) }
        static var searchEquipment: String { String(localized: "sessions.searchEquipment", defaultValue: "Search equipment", bundle: LanguageManager.shared.bundle) }

        // Asset metric filters
        static var metricIRCompletion: String { String(localized: "sessions.metricIRCompletion", defaultValue: "IR Completion", bundle: LanguageManager.shared.bundle) }
        static var metricInboundConnection: String { String(localized: "sessions.metricInboundConnection", defaultValue: "Inbound Connection", bundle: LanguageManager.shared.bundle) }
        static var metricArcFlashReadiness: String { String(localized: "sessions.metricArcFlashReadiness", defaultValue: "Arc Flash Readiness", bundle: LanguageManager.shared.bundle) }
        static var metricChildren: String { String(localized: "sessions.metricChildren", defaultValue: "Children", bundle: LanguageManager.shared.bundle) }
        static var markComplete: String { String(localized: "sessions.markComplete", defaultValue: "Mark Complete", bundle: LanguageManager.shared.bundle) }
        static var markTasksComplete: String { String(localized: "sessions.markTasksComplete", defaultValue: "Mark Tasks Complete", bundle: LanguageManager.shared.bundle) }
        static var uncomplete: String { String(localized: "sessions.uncomplete", defaultValue: "Uncomplete", bundle: LanguageManager.shared.bundle) }
        static var uncompleteTasks: String { String(localized: "sessions.uncompleteTasks", defaultValue: "Uncomplete Tasks", bundle: LanguageManager.shared.bundle) }
        static var resolve: String { String(localized: "sessions.resolve", defaultValue: "Resolve", bundle: LanguageManager.shared.bundle) }
        static var resolveIssues: String { String(localized: "sessions.resolveIssues", defaultValue: "Resolve Issues", bundle: LanguageManager.shared.bundle) }
        static var reopen: String { String(localized: "sessions.reopen", defaultValue: "Reopen", bundle: LanguageManager.shared.bundle) }
        static var reopenIssues: String { String(localized: "sessions.reopenIssues", defaultValue: "Reopen Issues", bundle: LanguageManager.shared.bundle) }
        static func completeTask(_ title: String) -> String {
            String(localized: "sessions.completeTask", defaultValue: "Complete: \(title)", bundle: LanguageManager.shared.bundle)
        }
        static func uncompleteTask(_ title: String) -> String {
            String(localized: "sessions.uncompleteTask", defaultValue: "Uncomplete: \(title)", bundle: LanguageManager.shared.bundle)
        }
        static func resolveIssue(_ title: String) -> String {
            String(localized: "sessions.resolveIssue", defaultValue: "Resolve: \(title)", bundle: LanguageManager.shared.bundle)
        }
        static func reopenIssue(_ title: String) -> String {
            String(localized: "sessions.reopenIssue", defaultValue: "Reopen: \(title)", bundle: LanguageManager.shared.bundle)
        }
        static func allCount(_ count: Int) -> String {
            String(localized: "sessions.allCount", defaultValue: "All (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static func assetsCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "sessions.assetsSingular", defaultValue: "\(count) asset", bundle: LanguageManager.shared.bundle)
                : String(localized: "sessions.assetsPlural", defaultValue: "\(count) assets", bundle: LanguageManager.shared.bundle)
        }

        // IR Photos
        static var irPhotos: String { String(localized: "sessions.irPhotos", defaultValue: "IR Photos", bundle: LanguageManager.shared.bundle) }
        static var noIRPhotos: String { String(localized: "sessions.noIRPhotos", defaultValue: "No IR Photos", bundle: LanguageManager.shared.bundle) }
        static var tapPlusToAddIRPhotos: String { String(localized: "sessions.tapPlusToAddIRPhotos", defaultValue: "Tap the + button to add IR photo sets to assets", bundle: LanguageManager.shared.bundle) }
        static var addIRPhotosNav: String { String(localized: "sessions.addIRPhotosNav", defaultValue: "Add IR Photos", bundle: LanguageManager.shared.bundle) }
        static var selectAssetForIR: String { String(localized: "sessions.selectAssetForIR", defaultValue: "Select an asset to add IR photo sets to:", bundle: LanguageManager.shared.bundle) }
        static var createAssetWithIR: String { String(localized: "sessions.createAssetWithIR", defaultValue: "Create a new asset with IR photo sets:", bundle: LanguageManager.shared.bundle) }
        static var saveIRPhotos: String { String(localized: "sessions.saveIRPhotos", defaultValue: "Save IR Photos", bundle: LanguageManager.shared.bundle) }
        static var uploadIRPhotos: String { String(localized: "sessions.uploadIRPhotos", defaultValue: "Upload IR Photos", bundle: LanguageManager.shared.bundle) }
        static var editIRPhoto: String { String(localized: "sessions.editIRPhoto", defaultValue: "Edit IR Photo", bundle: LanguageManager.shared.bundle) }
        static var extractingVisual: String { String(localized: "sessions.extractingVisual", defaultValue: "Extracting visual images from FLIR photos…", bundle: LanguageManager.shared.bundle) }
        static var extractVisualPhoto: String { String(localized: "sessions.extractVisualPhoto", defaultValue: "Extract Visual Photo", bundle: LanguageManager.shared.bundle) }
        static var extractVisualPhotoFailed: String { String(localized: "sessions.extractVisualPhotoFailed", defaultValue: "Could not extract a visual image from this file", bundle: LanguageManager.shared.bundle) }
        static var deleteIRPhoto: String { String(localized: "sessions.deleteIRPhoto", defaultValue: "Delete IR Photo?", bundle: LanguageManager.shared.bundle) }
        static var deleteIRPhotoConfirm: String { String(localized: "sessions.deleteIRPhotoConfirm", defaultValue: "Are you sure you want to delete this IR photo? This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static var irPhotoKey: String { String(localized: "sessions.irPhotoKey", defaultValue: "IR Photo Key", bundle: LanguageManager.shared.bundle) }
        static var visualPhotoKey: String { String(localized: "sessions.visualPhotoKey", defaultValue: "Visual Photo Key", bundle: LanguageManager.shared.bundle) }
        static var uploadingIRPhotos: String { String(localized: "sessions.uploadingIRPhotos", defaultValue: "Uploading IR Photos...", bundle: LanguageManager.shared.bundle) }
        static func irPhotoCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "sessions.irPhotoCountSingular", defaultValue: "\(count) IR Photo", bundle: LanguageManager.shared.bundle)
                : String(localized: "sessions.irPhotoCountPlural", defaultValue: "\(count) IR Photos", bundle: LanguageManager.shared.bundle)
        }
        static var fromPhotos: String { String(localized: "sessions.fromPhotos", defaultValue: "From Photos", bundle: LanguageManager.shared.bundle) }
        static var fromFiles: String { String(localized: "sessions.fromFiles", defaultValue: "From Files", bundle: LanguageManager.shared.bundle) }
        static var irPhotoFileNameNote: String { String(localized: "sessions.irPhotoFileNameNote", defaultValue: "File names are used as IR & visual photo keys.", bundle: LanguageManager.shared.bundle) }
        static var irPhotoFileNameNoteFlirInd: String { String(localized: "sessions.irPhotoFileNameNoteFlirInd", defaultValue: "File names are used as IR & visual photo keys. For FLIR-IND files, visual photo auto-generates — select from Files only, as Photo Library compresses metadata and is not supported.", bundle: LanguageManager.shared.bundle) }
        static var removeIRPhotoSet: String { String(localized: "sessions.removeIRPhotoSet", defaultValue: "Remove this IR photo set?", bundle: LanguageManager.shared.bundle) }
        static var removeIRPhotoMessage: String { String(localized: "sessions.removeIRPhotoMessage", defaultValue: "This will remove the IR photo pair from the session.", bundle: LanguageManager.shared.bundle) }
        static var removePhotoSet: String { String(localized: "sessions.removePhotoSet", defaultValue: "Remove this photo set?", bundle: LanguageManager.shared.bundle) }
        static var removePhotoFromSession: String { String(localized: "sessions.removePhotoFromSession", defaultValue: "Remove this photo from the session?", bundle: LanguageManager.shared.bundle) }
        static var removePhotoMessage: String { String(localized: "sessions.removePhotoMessage", defaultValue: "The photo will remain in your library but won't be linked to this session.", bundle: LanguageManager.shared.bundle) }
        static var noAvailablePhotos: String { String(localized: "sessions.noAvailablePhotos", defaultValue: "No Available Photos", bundle: LanguageManager.shared.bundle) }
        static var allPhotosLinked: String { String(localized: "sessions.allPhotosLinked", defaultValue: "All photos are already linked to sessions", bundle: LanguageManager.shared.bundle) }
        static var addPhotos: String { String(localized: "sessions.addPhotos", defaultValue: "Add Photos", bundle: LanguageManager.shared.bundle) }
        static func photoSetsAcrossAssets(sets: Int, assets: Int) -> String {
            let setsText = sets == 1 ? "set" : "sets"
            let assetsText = assets == 1 ? "asset" : "assets"
            return String(localized: "sessions.photoSetsAcrossAssets", defaultValue: "\(sets) photo \(setsText) across \(assets) \(assetsText)", bundle: LanguageManager.shared.bundle)
        }
        static func setsCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "sessions.setSingular", defaultValue: "\(count) set", bundle: LanguageManager.shared.bundle)
                : String(localized: "sessions.setPlural", defaultValue: "\(count) sets", bundle: LanguageManager.shared.bundle)
        }
        static func irPhotoLabel(_ count: Int) -> String {
            count == 1
                ? String(localized: "sessions.irPhotoSingular", defaultValue: "\(count) IR photo", bundle: LanguageManager.shared.bundle)
                : String(localized: "sessions.irPhotoPlural", defaultValue: "\(count) IR photos", bundle: LanguageManager.shared.bundle)
        }
        static func photoLabel(_ count: Int) -> String {
            count == 1
                ? String(localized: "sessions.photoSingular", defaultValue: "\(count) photo", bundle: LanguageManager.shared.bundle)
                : String(localized: "sessions.photoPlural", defaultValue: "\(count) photos", bundle: LanguageManager.shared.bundle)
        }
        static func typeLabel(_ type: String) -> String {
            String(localized: "sessions.typeLabel", defaultValue: "Type: \(type)", bundle: LanguageManager.shared.bundle)
        }

        // Tasks
        static var noTasks: String { String(localized: "sessions.noTasks", defaultValue: "No Tasks", bundle: LanguageManager.shared.bundle) }
        static var linkTasksDescription: String { String(localized: "sessions.linkTasksDescription", defaultValue: "Link tasks to this session using the button above", bundle: LanguageManager.shared.bundle) }
        static var manageTasks: String { String(localized: "sessions.manageTasks", defaultValue: "Manage Tasks", bundle: LanguageManager.shared.bundle) }
        static var manage: String { String(localized: "sessions.manage", defaultValue: "Manage", bundle: LanguageManager.shared.bundle) }
        static var pendingTasks: String { String(localized: "sessions.pendingTasks", defaultValue: "Pending Tasks", bundle: LanguageManager.shared.bundle) }
        static var completedTasks: String { String(localized: "sessions.completedTasks", defaultValue: "Completed Tasks", bundle: LanguageManager.shared.bundle) }
        static var getStartedTasks: String { String(localized: "sessions.getStartedTasks", defaultValue: "Get started by linking existing tasks or creating new ones", bundle: LanguageManager.shared.bundle) }
        static var linkTasks: String { String(localized: "sessions.linkTasks", defaultValue: "Link Tasks", bundle: LanguageManager.shared.bundle) }
        static var selectTasksToLink: String { String(localized: "sessions.selectTasksToLink", defaultValue: "Select tasks to link to this session", bundle: LanguageManager.shared.bundle) }
        static var searchTasks: String { String(localized: "sessions.searchTasks", defaultValue: "Search tasks", bundle: LanguageManager.shared.bundle) }
        static var searchTasksPlaceholder: String { String(localized: "sessions.searchTasksPlaceholder", defaultValue: "Search tasks...", bundle: LanguageManager.shared.bundle) }
        static var stopListening: String { String(localized: "sessions.stopListening", defaultValue: "Stop Listening", bundle: LanguageManager.shared.bundle) }
        static var listenForAssets: String { String(localized: "sessions.listenForAssets", defaultValue: "Listen for Assets", bundle: LanguageManager.shared.bundle) }
        static var noMatchingTasks: String { String(localized: "sessions.noMatchingTasks", defaultValue: "No Matching Tasks", bundle: LanguageManager.shared.bundle) }
        static var sessionHasNoTasks: String { String(localized: "sessions.sessionHasNoTasks", defaultValue: "This session has no tasks to link", bundle: LanguageManager.shared.bundle) }
        static func noTasksMatch(_ search: String) -> String {
            String(localized: "sessions.noTasksMatch", defaultValue: "No tasks match \"\(search)\"", bundle: LanguageManager.shared.bundle)
        }

        // Issues
        static var noIssues: String { String(localized: "sessions.noIssues", defaultValue: "No Issues", bundle: LanguageManager.shared.bundle) }
        static var getStartedIssues: String { String(localized: "sessions.getStartedIssues", defaultValue: "Get started by linking existing issues or creating new ones", bundle: LanguageManager.shared.bundle) }
        static var manageIssues: String { String(localized: "sessions.manageIssues", defaultValue: "Manage Issues", bundle: LanguageManager.shared.bundle) }
        static var linkIssues: String { String(localized: "sessions.linkIssues", defaultValue: "Link Issues", bundle: LanguageManager.shared.bundle) }
        static var selectIssuesToLink: String { String(localized: "sessions.selectIssuesToLink", defaultValue: "Select issues to link to this session", bundle: LanguageManager.shared.bundle) }
        static var issuesExclusivityWarning: String { String(localized: "sessions.issuesExclusivityWarning", defaultValue: "Issues can only be linked to one session at a time", bundle: LanguageManager.shared.bundle) }
        static var searchIssues: String { String(localized: "sessions.searchIssues", defaultValue: "Search issues", bundle: LanguageManager.shared.bundle) }
        static func linkedTo(_ name: String) -> String {
            String(localized: "sessions.linkedTo", defaultValue: "Linked to: \(name)", bundle: LanguageManager.shared.bundle)
        }

        // QR Filter
        static func filteredByAsset(_ name: String) -> String {
            String(localized: "sessions.filteredByAsset", defaultValue: "Filtered by: \(name)", bundle: LanguageManager.shared.bundle)
        }
        static var noTasksForAsset: String { String(localized: "sessions.noTasksForAsset", defaultValue: "No tasks linked to this asset", bundle: LanguageManager.shared.bundle) }
        static var noIssuesForAsset: String { String(localized: "sessions.noIssuesForAsset", defaultValue: "No issues linked to this asset", bundle: LanguageManager.shared.bundle) }

        // Grouped tasks
        static var noGrouping: String { String(localized: "sessions.noGrouping", defaultValue: "No Grouping", bundle: LanguageManager.shared.bundle) }
        static var byLocation: String { String(localized: "sessions.byLocation", defaultValue: "By Location", bundle: LanguageManager.shared.bundle) }
        static var byParentNode: String { String(localized: "sessions.byParentNode", defaultValue: "By Parent Node", bundle: LanguageManager.shared.bundle) }
        static var multiNodeTasks: String { String(localized: "sessions.multiNodeTasks", defaultValue: "Multi-Node Tasks", bundle: LanguageManager.shared.bundle) }
        static var noParentNode: String { String(localized: "sessions.noParentNode", defaultValue: "No Parent Node", bundle: LanguageManager.shared.bundle) }
        static var unknownParent: String { String(localized: "sessions.unknownParent", defaultValue: "Unknown Parent", bundle: LanguageManager.shared.bundle) }

        // Task type selection
        static var createTask: String { String(localized: "sessions.createTask", defaultValue: "Create Task", bundle: LanguageManager.shared.bundle) }
        static var chooseTaskType: String { String(localized: "sessions.chooseTaskType", defaultValue: "Choose task type", bundle: LanguageManager.shared.bundle) }
        static var simpleTask: String { String(localized: "sessions.simpleTask", defaultValue: "Simple Task", bundle: LanguageManager.shared.bundle) }
        static var oneTaskOneAsset: String { String(localized: "sessions.oneTaskOneAsset", defaultValue: "One task for one asset", bundle: LanguageManager.shared.bundle) }
        static var complexTask: String { String(localized: "sessions.complexTask", defaultValue: "Complex Task", bundle: LanguageManager.shared.bundle) }
        static var oneTaskMultipleAssets: String { String(localized: "sessions.oneTaskMultipleAssets", defaultValue: "One task for multiple assets or forms", bundle: LanguageManager.shared.bundle) }

        // Attachments
        static var attachments: String { String(localized: "sessions.attachments", defaultValue: "Attachments", bundle: LanguageManager.shared.bundle) }
        static var addFile: String { String(localized: "sessions.addFile", defaultValue: "Add File", bundle: LanguageManager.shared.bundle) }
        static var noAttachments: String { String(localized: "sessions.noAttachments", defaultValue: "No Attachments", bundle: LanguageManager.shared.bundle) }
        static var addFilesDescription: String { String(localized: "sessions.addFilesDescription", defaultValue: "Add documents, images, or other files to this session", bundle: LanguageManager.shared.bundle) }
        static var browseFiles: String { String(localized: "sessions.browseFiles", defaultValue: "Browse Files", bundle: LanguageManager.shared.bundle) }
        static var deleteAttachment: String { String(localized: "sessions.deleteAttachment", defaultValue: "Delete Attachment?", bundle: LanguageManager.shared.bundle) }
        static var deleteAttachmentConfirm: String { String(localized: "sessions.deleteAttachmentConfirm", defaultValue: "Are you sure you want to delete this attachment? This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static var file: String { String(localized: "sessions.file", defaultValue: "File", bundle: LanguageManager.shared.bundle) }
        static var attachmentType: String { String(localized: "sessions.attachmentType", defaultValue: "Attachment Type", bundle: LanguageManager.shared.bundle) }
        static var associatedTask: String { String(localized: "sessions.associatedTask", defaultValue: "Associated Task (Optional)", bundle: LanguageManager.shared.bundle) }
        static var linkToTask: String { String(localized: "sessions.linkToTask", defaultValue: "Link this attachment to a specific task", bundle: LanguageManager.shared.bundle) }
        static var associatedAssets: String { String(localized: "sessions.associatedAssets", defaultValue: "Associated Assets (Optional)", bundle: LanguageManager.shared.bundle) }
        static var attachmentAssetsFooter: String { String(localized: "sessions.attachmentAssetsFooter", defaultValue: "The attachment will appear in selected assets' attachment list", bundle: LanguageManager.shared.bundle) }
        static var selectAssets: String { String(localized: "sessions.selectAssets", defaultValue: "Select Assets", bundle: LanguageManager.shared.bundle) }
        static var uploading: String { String(localized: "sessions.uploading", defaultValue: "Uploading...", bundle: LanguageManager.shared.bundle) }
        static var uploadFile: String { String(localized: "sessions.uploadFile", defaultValue: "Upload File", bundle: LanguageManager.shared.bundle) }
        static var cancelUpload: String { String(localized: "sessions.cancelUpload", defaultValue: "Cancel Upload?", bundle: LanguageManager.shared.bundle) }
        static var continueUpload: String { String(localized: "sessions.continueUpload", defaultValue: "Continue Upload", bundle: LanguageManager.shared.bundle) }
        static var cancelUploadButton: String { String(localized: "sessions.cancelUploadButton", defaultValue: "Cancel Upload", bundle: LanguageManager.shared.bundle) }
        static var uploadInProgress: String { String(localized: "sessions.uploadInProgress", defaultValue: "The upload is in progress. Are you sure you want to cancel?", bundle: LanguageManager.shared.bundle) }
        static var uploadError: String { String(localized: "sessions.uploadError", defaultValue: "Upload Error", bundle: LanguageManager.shared.bundle) }
        static var fileTooLarge: String { String(localized: "sessions.fileTooLarge", defaultValue: "File too large. Maximum size is 100MB.", bundle: LanguageManager.shared.bundle) }
        static var unableToGetCompanyInfo: String { String(localized: "sessions.unableToGetCompanyInfo", defaultValue: "Unable to get company information", bundle: LanguageManager.shared.bundle) }
        static func selectedOfTotal(_ selected: Int, _ total: Int) -> String {
            String(localized: "sessions.selectedOfTotal", defaultValue: "\(selected) of \(total) selected", bundle: LanguageManager.shared.bundle)
        }

        // Attachment Visibility
        static var visibility: String { String(localized: "sessions.visibility", defaultValue: "Visibility", bundle: LanguageManager.shared.bundle) }
        static var visibilityInternal: String { String(localized: "sessions.visibilityInternal", defaultValue: "Internal", bundle: LanguageManager.shared.bundle) }
        static var visibilityPublic: String { String(localized: "sessions.visibilityPublic", defaultValue: "Public", bundle: LanguageManager.shared.bundle) }
        static var visibilityFooter: String { String(localized: "sessions.visibilityFooter", defaultValue: "Internal files are only visible to team members. Public files may be shared with clients.", bundle: LanguageManager.shared.bundle) }
        static var makePublicTitle: String { String(localized: "sessions.makePublicTitle", defaultValue: "Make Public?", bundle: LanguageManager.shared.bundle) }
        static var makePublicButton: String { String(localized: "sessions.makePublicButton", defaultValue: "Make Public", bundle: LanguageManager.shared.bundle) }
        static var makePublicMessage: String { String(localized: "sessions.makePublicMessage", defaultValue: "This attachment will be visible to clients. Continue?", bundle: LanguageManager.shared.bundle) }
        static func makeVisibility(_ name: String) -> String {
            String(localized: "sessions.makeVisibility", defaultValue: "Make \(name)", bundle: LanguageManager.shared.bundle)
        }

        // IR Session Summary Card
        static var irSessions: String { String(localized: "sessions.irSessions", defaultValue: "IR Sessions", bundle: LanguageManager.shared.bundle) }
        static func activeCount(_ count: Int) -> String {
            String(localized: "sessions.activeCount", defaultValue: "\(count) active", bundle: LanguageManager.shared.bundle)
        }
        static func totalCount(_ count: Int) -> String {
            String(localized: "sessions.totalCount", defaultValue: "\(count) total", bundle: LanguageManager.shared.bundle)
        }
        static var photos: String { String(localized: "sessions.photos", defaultValue: "photos", bundle: LanguageManager.shared.bundle) }
        static func moreActive(_ count: Int) -> String {
            String(localized: "sessions.moreActive", defaultValue: "+ \(count) more active", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Assets (extended)
    enum AssetsExtra {
        // Basic info
        static var basicInformation: String { String(localized: "assets.basicInformation", defaultValue: "Basic Information", bundle: LanguageManager.shared.bundle) }
        static var assetName: String { String(localized: "assets.assetName", defaultValue: "Asset Name", bundle: LanguageManager.shared.bundle) }
        static var enterAssetName: String { String(localized: "assets.enterAssetName", defaultValue: "Enter asset name", bundle: LanguageManager.shared.bundle) }
        static var enclosure: String { String(localized: "assets.enclosure", defaultValue: "Enclosure", bundle: LanguageManager.shared.bundle) }
        static var unknownType: String { String(localized: "assets.unknownType", defaultValue: "Unknown Type", bundle: LanguageManager.shared.bundle) }
        static var unknown: String { String(localized: "assets.unknown", defaultValue: "Unknown", bundle: LanguageManager.shared.bundle) }

        // Save states
        static var saving: String { String(localized: "assets.saving", defaultValue: "Saving...", bundle: LanguageManager.shared.bundle) }
        static var saveChanges: String { String(localized: "assets.saveChanges", defaultValue: "Save Changes", bundle: LanguageManager.shared.bundle) }
        static var savingAsset: String { String(localized: "assets.savingAsset", defaultValue: "Saving asset...", bundle: LanguageManager.shared.bundle) }
        static var savingConnection: String { String(localized: "assets.savingConnection", defaultValue: "Saving connection...", bundle: LanguageManager.shared.bundle) }
        static var unknownError: String { String(localized: "assets.unknownError", defaultValue: "Unknown error", bundle: LanguageManager.shared.bundle) }

        // IR Photos section in detail
        static var noActiveIRSession: String { String(localized: "assets.noActiveIRSession", defaultValue: "No active IR session", bundle: LanguageManager.shared.bundle) }
        static var selectSessionFromSessions: String { String(localized: "assets.selectSessionFromSessions", defaultValue: "Select a session from the Sessions view", bundle: LanguageManager.shared.bundle) }
        static var existingIRPhotos: String { String(localized: "assets.existingIRPhotos", defaultValue: "Existing IR Photos", bundle: LanguageManager.shared.bundle) }
        static func irPrefix(_ key: String) -> String {
            String(localized: "assets.irPrefix", defaultValue: "IR: \(key)", bundle: LanguageManager.shared.bundle)
        }
        static func visualPrefix(_ key: String) -> String {
            String(localized: "assets.visualPrefix", defaultValue: "Visual: \(key)", bundle: LanguageManager.shared.bundle)
        }

        // Asset grouping (AssetsTabView)
        static var noGrouping: String { String(localized: "assets.noGrouping", defaultValue: "No Grouping", bundle: LanguageManager.shared.bundle) }
        static var groupByLocation: String { String(localized: "assets.groupByLocation", defaultValue: "Group by Location", bundle: LanguageManager.shared.bundle) }
        static var groupByEnclosure: String { String(localized: "assets.groupByEnclosure", defaultValue: "Group by Enclosure", bundle: LanguageManager.shared.bundle) }
        static var grouping: String { String(localized: "assets.grouping", defaultValue: "Grouping", bundle: LanguageManager.shared.bundle) }
        static var topLevel: String { String(localized: "assets.topLevel", defaultValue: "Top-Level", bundle: LanguageManager.shared.bundle) }
        static func selectedCount(_ count: Int) -> String {
            String(localized: "assets.selectedCount", defaultValue: "\(count) Selected", bundle: LanguageManager.shared.bundle)
        }
        static var searchByNameTypeLocation: String { String(localized: "assets.searchByNameTypeLocation", defaultValue: "Search by name, type, location, or QR code", bundle: LanguageManager.shared.bundle) }
        static var selectAll: String { String(localized: "assets.selectAll", defaultValue: "Select All", bundle: LanguageManager.shared.bundle) }
        static var deleteSelected: String { String(localized: "assets.deleteSelected", defaultValue: "Delete Selected", bundle: LanguageManager.shared.bundle) }
        static var selectMultiple: String { String(localized: "assets.selectMultiple", defaultValue: "Select Multiple", bundle: LanguageManager.shared.bundle) }
        static var hideAFPunchlist: String { String(localized: "assets.hideAFPunchlist", defaultValue: "Hide AF Punchlist", bundle: LanguageManager.shared.bundle) }
        static var showAFPunchlist: String { String(localized: "assets.showAFPunchlist", defaultValue: "Show AF Punchlist", bundle: LanguageManager.shared.bundle) }

        // Delete alerts
        static var deleteAsset: String { String(localized: "assets.deleteAsset", defaultValue: "Delete Asset", bundle: LanguageManager.shared.bundle) }
        static func deleteAssetConfirm(_ name: String) -> String {
            String(localized: "assets.deleteAssetConfirm", defaultValue: "Are you sure you want to delete \"\(name)\"? This action cannot be undone.", bundle: LanguageManager.shared.bundle)
        }
        static func deleteAssetsConfirm(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "assets.deleteAssetsConfirm", defaultValue: "Are you sure you want to delete \(count) asset\(s)? This action cannot be undone.", bundle: LanguageManager.shared.bundle)
        }
        static var deleteFailed: String { String(localized: "assets.deleteFailed", defaultValue: "Delete Failed", bundle: LanguageManager.shared.bundle) }

        // Asset class change confirmation
        static var changeAssetClassTitle: String { String(localized: "assets.changeAssetClassTitle", defaultValue: "Change Asset Class?", bundle: LanguageManager.shared.bundle) }
        static var changeAssetClassMessage: String { String(localized: "assets.changeAssetClassMessage", defaultValue: "Changing the asset class will clear the following fields:\n\n• Manufacturer & Model\n• Trip Configuration values\n• Nameplate fields\n• Condition of Maintenance\n• Linked Suggested PM Plans\n• Engineering custom attributes", bundle: LanguageManager.shared.bundle) }
        static var changeAssetClassConfirm: String { String(localized: "assets.changeAssetClassConfirm", defaultValue: "Change Class", bundle: LanguageManager.shared.bundle) }
        static var multipleAssetsFound: String { String(localized: "assets.multipleAssetsFound", defaultValue: "Multiple Assets Found", bundle: LanguageManager.shared.bundle) }
        static var viewAllInSearch: String { String(localized: "assets.viewAllInSearch", defaultValue: "View All in Search", bundle: LanguageManager.shared.bundle) }
        static var frameConfiguration: String { String(localized: "assets.frameConfiguration", defaultValue: "Frame / Trip Configuration", bundle: LanguageManager.shared.bundle) }
        static var downloadEqpLibraryFirst: String { String(localized: "assets.downloadEqpLibraryFirst", defaultValue: "Download the equipment library from Settings first", bundle: LanguageManager.shared.bundle) }
        static var clearSelection: String { String(localized: "assets.clearSelection", defaultValue: "Clear Selection", bundle: LanguageManager.shared.bundle) }
        static var selectEquipment: String { String(localized: "assets.selectEquipment", defaultValue: "Select Equipment", bundle: LanguageManager.shared.bundle) }
        static var chooseCategory: String { String(localized: "assets.chooseCategory", defaultValue: "Choose an equipment category above", bundle: LanguageManager.shared.bundle) }
        static var noSensorData: String { String(localized: "assets.noSensorData", defaultValue: "No sensor data available", bundle: LanguageManager.shared.bundle) }
        static var createMultipleOCPTitle: String { String(localized: "assets.createMultipleOCP", defaultValue: "Create Multiple OCP", bundle: LanguageManager.shared.bundle) }
        static var labelsTitle: String { String(localized: "assets.labels", defaultValue: "Labels", bundle: LanguageManager.shared.bundle) }
        static var serviceabilityNote: String { String(localized: "assets.serviceabilityNote", defaultValue: "Serviceability Note", bundle: LanguageManager.shared.bundle) }
        static var explainNotServiceable: String { String(localized: "assets.explainNotServiceable", defaultValue: "Explain why this equipment is not fully serviceable", bundle: LanguageManager.shared.bundle) }
        static var serviceabilityLabelPrefix: String { String(localized: "assets.serviceabilityLabelPrefix", defaultValue: "Serviceability:", bundle: LanguageManager.shared.bundle) }
        static var serviceabilitySetByCOM: String { String(localized: "assets.serviceabilitySetByCOM", defaultValue: "(set by COM Calculator)", bundle: LanguageManager.shared.bundle) }
        static var addNotesAboutAsset: String { String(localized: "assets.addNotesAboutAsset", defaultValue: "Add notes about this asset...", bundle: LanguageManager.shared.bundle) }
        static var nonserviceable: String { String(localized: "assets.nonserviceable", defaultValue: "Nonserviceable", bundle: LanguageManager.shared.bundle) }
        static var copyDetailsFrom: String { String(localized: "assets.copyDetailsFrom", defaultValue: "Copy Details From...", bundle: LanguageManager.shared.bundle) }
        static var copyDetailsTo: String { String(localized: "assets.copyDetailsTo", defaultValue: "Copy Details To...", bundle: LanguageManager.shared.bundle) }
        static var copyCompleteAlert: String { String(localized: "assets.copyComplete", defaultValue: "Copy Complete", bundle: LanguageManager.shared.bundle) }
        static var noSameClassAssets: String { String(localized: "assets.noSameClassAssets", defaultValue: "No Same-Class Assets", bundle: LanguageManager.shared.bundle) }
        static var searchByLabelRoomQR: String { String(localized: "assets.searchByLabelRoomQR", defaultValue: "Search by label, room, or QR code", bundle: LanguageManager.shared.bundle) }
        static var qrScanAlert: String { String(localized: "assets.qrScan", defaultValue: "QR Scan", bundle: LanguageManager.shared.bundle) }
        static var selectFields: String { String(localized: "assets.selectFields", defaultValue: "Select Fields", bundle: LanguageManager.shared.bundle) }
        static var engineeringCategory: String { String(localized: "assets.engineeringCategory", defaultValue: "Engineering", bundle: LanguageManager.shared.bundle) }
        static var searchStyles: String { String(localized: "assets.searchStyles", defaultValue: "Search styles...", bundle: LanguageManager.shared.bundle) }
        static var missingLabel: String { String(localized: "assets.missing", defaultValue: "Missing", bundle: LanguageManager.shared.bundle) }
        static var edgeProperties: String { String(localized: "assets.edgeProperties", defaultValue: "Edge Properties", bundle: LanguageManager.shared.bundle) }
        static var issuePropertiesTitle: String { String(localized: "assets.issueProperties", defaultValue: "Issue Properties", bundle: LanguageManager.shared.bundle) }
        static func foundAssetsWithQR(_ count: Int) -> String {
            String(localized: "assets.foundAssetsWithQR", defaultValue: "Found \(count) assets with this QR code. Select which one to open:", bundle: LanguageManager.shared.bundle)
        }
        static var editAsset: String { String(localized: "assets.editAsset", defaultValue: "Edit Asset", bundle: LanguageManager.shared.bundle) }

        // Voltage
        static var voltage: String { String(localized: "assets.voltage", defaultValue: "Voltage", bundle: LanguageManager.shared.bundle) }
        static var primaryVoltage: String { String(localized: "assets.primaryVoltage", defaultValue: "Primary Voltage", bundle: LanguageManager.shared.bundle) }
        static var secondaryVoltage: String { String(localized: "assets.secondaryVoltage", defaultValue: "Secondary Voltage", bundle: LanguageManager.shared.bundle) }

        // IR Photo labels
        static var visLabel: String { String(localized: "assets.visLabel", defaultValue: "VIS", bundle: LanguageManager.shared.bundle) }
        static var irLabel: String { String(localized: "assets.irLabel", defaultValue: "IR", bundle: LanguageManager.shared.bundle) }

        // Copy Details nav titles (without ellipsis)
        static var copyDetailsFromTitle: String { String(localized: "assets.copyDetailsFromTitle", defaultValue: "Copy Details From", bundle: LanguageManager.shared.bundle) }
        static var copyDetailsToTitle: String { String(localized: "assets.copyDetailsToTitle", defaultValue: "Copy Details To", bundle: LanguageManager.shared.bundle) }

        // OCP / Child nodes
        static var ocp: String { String(localized: "assets.ocp", defaultValue: "OCP", bundle: LanguageManager.shared.bundle) }
        static var createNewChild: String { String(localized: "assets.createNewChild", defaultValue: "Create New Child", bundle: LanguageManager.shared.bundle) }
        static var linkExistingNode: String { String(localized: "assets.linkExistingNode", defaultValue: "Link Existing Node", bundle: LanguageManager.shared.bundle) }
        static var noChildAssets: String { String(localized: "assets.noChildAssets", defaultValue: "No child assets", bundle: LanguageManager.shared.bundle) }
        static var addOCPDevices: String { String(localized: "assets.addOCPDevices", defaultValue: "Add OCP devices to this enclosure", bundle: LanguageManager.shared.bundle) }
        static var deleteChildAsset: String { String(localized: "assets.deleteChildAsset", defaultValue: "Delete Child Asset?", bundle: LanguageManager.shared.bundle) }
        static func deleteChildConfirm(_ name: String) -> String {
            String(localized: "assets.deleteChildConfirm", defaultValue: "Are you sure you want to delete '\(name)'? This action cannot be undone.", bundle: LanguageManager.shared.bundle)
        }
        static var unlinkChildAsset: String { String(localized: "assets.unlinkChildAsset", defaultValue: "Unlink Child Asset?", bundle: LanguageManager.shared.bundle) }
        static var unlink: String { String(localized: "assets.unlink", defaultValue: "Unlink", bundle: LanguageManager.shared.bundle) }
        static func unlinkMessage(_ name: String) -> String {
            String(localized: "assets.unlinkMessage", defaultValue: "'\(name)' will be removed from this enclosure but not deleted. It will be moved outside the enclosure.", bundle: LanguageManager.shared.bundle)
        }
        static var viewDetails: String { String(localized: "assets.viewDetails", defaultValue: "View Details", bundle: LanguageManager.shared.bundle) }
        static var unlinkFromParent: String { String(localized: "assets.unlinkFromParent", defaultValue: "Unlink from Parent", bundle: LanguageManager.shared.bundle) }
        static var newChildAsset: String { String(localized: "assets.newChildAsset", defaultValue: "New Child Asset", bundle: LanguageManager.shared.bundle) }
        static var createChildAsset: String { String(localized: "assets.createChildAsset", defaultValue: "Create Child Asset", bundle: LanguageManager.shared.bundle) }
        static var parentEnclosure: String { String(localized: "assets.parentEnclosure", defaultValue: "Parent Enclosure", bundle: LanguageManager.shared.bundle) }
        static var selectClass: String { String(localized: "assets.selectClass", defaultValue: "Select class", bundle: LanguageManager.shared.bundle) }
        static var onlyOCPClasses: String { String(localized: "assets.onlyOCPClasses", defaultValue: "Only OCP (Over-Current Protection) classes can be added as child assets", bundle: LanguageManager.shared.bundle) }
        static var creating: String { String(localized: "assets.creating", defaultValue: "Creating...", bundle: LanguageManager.shared.bundle) }

        // Link existing nodes
        static var linkExistingNodes: String { String(localized: "assets.linkExistingNodes", defaultValue: "Link Existing Nodes", bundle: LanguageManager.shared.bundle) }
        static var noEligibleNodes: String { String(localized: "assets.noEligibleNodes", defaultValue: "No Eligible Nodes", bundle: LanguageManager.shared.bundle) }
        static var noEligibleNodesDescription: String { String(localized: "assets.noEligibleNodesDescription", defaultValue: "There are no available OCP nodes without a parent that can be linked to this enclosure.", bundle: LanguageManager.shared.bundle) }
        static func selectedLabel(_ count: Int) -> String {
            String(localized: "assets.selectedLabel", defaultValue: "\(count) selected", bundle: LanguageManager.shared.bundle)
        }
        static var clearAll: String { String(localized: "assets.clearAll", defaultValue: "Clear All", bundle: LanguageManager.shared.bundle) }
        static var searchByLabelClass: String { String(localized: "assets.searchByLabelClass", defaultValue: "Search by label, class, or location...", bundle: LanguageManager.shared.bundle) }
        static func linkCountButton(_ count: Int) -> String {
            count > 0
                ? String(localized: "assets.linkCountButton", defaultValue: "Link (\(count))", bundle: LanguageManager.shared.bundle)
                : String(localized: "assets.linkButton", defaultValue: "Link", bundle: LanguageManager.shared.bundle)
        }
        static var linking: String { String(localized: "assets.linking", defaultValue: "Linking...", bundle: LanguageManager.shared.bundle) }

        // Connections
        static var connections: String { String(localized: "assets.connections", defaultValue: "Connections", bundle: LanguageManager.shared.bundle) }
        static var newConnection: String { String(localized: "assets.newConnection", defaultValue: "New Connection", bundle: LanguageManager.shared.bundle) }
        static var connectionDetails: String { String(localized: "assets.connectionDetails", defaultValue: "Connection Details", bundle: LanguageManager.shared.bundle) }
        static var connectionTypeOptional: String { String(localized: "assets.connectionTypeOptional", defaultValue: "Connection Type (Optional)", bundle: LanguageManager.shared.bundle) }
        static var sourceNode: String { String(localized: "assets.sourceNode", defaultValue: "Source Node", bundle: LanguageManager.shared.bundle) }
        static var targetNode: String { String(localized: "assets.targetNode", defaultValue: "Target Node", bundle: LanguageManager.shared.bundle) }
        static var edgeClass: String { String(localized: "assets.edgeClass", defaultValue: "Edge Class", bundle: LanguageManager.shared.bundle) }
        static var selectSource: String { String(localized: "assets.selectSource", defaultValue: "Select source", bundle: LanguageManager.shared.bundle) }
        static var selectTarget: String { String(localized: "assets.selectTarget", defaultValue: "Select target", bundle: LanguageManager.shared.bundle) }
        static var sameNodeValidation: String { String(localized: "assets.sameNodeValidation", defaultValue: "Source and target cannot be the same node", bundle: LanguageManager.shared.bundle) }
        static var duplicateConnectionExists: String { String(localized: "assets.duplicateConnectionExists", defaultValue: "A connection already exists between these nodes", bundle: LanguageManager.shared.bundle) }
        static var linesideIncoming: String { String(localized: "assets.linesideIncoming", defaultValue: "Lineside (Incoming)", bundle: LanguageManager.shared.bundle) }
        static var loadsideOutgoing: String { String(localized: "assets.loadsideOutgoing", defaultValue: "Loadside (Outgoing)", bundle: LanguageManager.shared.bundle) }
        static var powerFlowsIn: String { String(localized: "assets.powerFlowsIn", defaultValue: "Power flows INTO this asset", bundle: LanguageManager.shared.bundle) }
        static var powerFlowsOut: String { String(localized: "assets.powerFlowsOut", defaultValue: "Power flows OUT OF this asset", bundle: LanguageManager.shared.bundle) }
        static var newLinesideConnection: String { String(localized: "assets.newLinesideConnection", defaultValue: "New Lineside Connection", bundle: LanguageManager.shared.bundle) }
        static var newLoadsideConnection: String { String(localized: "assets.newLoadsideConnection", defaultValue: "New Loadside Connection", bundle: LanguageManager.shared.bundle) }
        static var noConnections: String { String(localized: "assets.noConnections", defaultValue: "No connections", bundle: LanguageManager.shared.bundle) }
        static var addConnections: String { String(localized: "assets.addConnections", defaultValue: "Add lineside or loadside connections", bundle: LanguageManager.shared.bundle) }
        static var deleteConnection: String { String(localized: "assets.deleteConnection", defaultValue: "Delete Connection?", bundle: LanguageManager.shared.bundle) }
        static func deleteConnectionConfirm(_ name: String) -> String {
            String(localized: "assets.deleteConnectionConfirm", defaultValue: "Are you sure you want to delete the connection '\(name)'?", bundle: LanguageManager.shared.bundle)
        }
        static var internalDirection: String { String(localized: "assets.internalDirection", defaultValue: "Internal", bundle: LanguageManager.shared.bundle) }
        static var lineside: String { String(localized: "assets.lineside", defaultValue: "Lineside", bundle: LanguageManager.shared.bundle) }
        static var loadside: String { String(localized: "assets.loadside", defaultValue: "Loadside", bundle: LanguageManager.shared.bundle) }

        // QR Scanner
        static var cameraAccessRequired: String { String(localized: "assets.cameraAccessRequired", defaultValue: "Camera Access Required", bundle: LanguageManager.shared.bundle) }
        static var enableCameraForQR: String { String(localized: "assets.enableCameraForQR", defaultValue: "Please enable camera access in Settings to scan QR codes", bundle: LanguageManager.shared.bundle) }
        static var openSettings: String { String(localized: "assets.openSettings", defaultValue: "Open Settings", bundle: LanguageManager.shared.bundle) }
        static var alignQRCode: String { String(localized: "assets.alignQRCode", defaultValue: "Align QR code within frame", bundle: LanguageManager.shared.bundle) }
        static var cameraWillScan: String { String(localized: "assets.cameraWillScan", defaultValue: "The camera will scan automatically", bundle: LanguageManager.shared.bundle) }
        static var scanQRCode: String { String(localized: "assets.scanQRCode", defaultValue: "Scan QR Code", bundle: LanguageManager.shared.bundle) }
        static var scanError: String { String(localized: "assets.scanError", defaultValue: "Scan Error", bundle: LanguageManager.shared.bundle) }

        // COM Calculator
        static var comCalculator: String { String(localized: "assets.comCalculator", defaultValue: "COM Calculator", bundle: LanguageManager.shared.bundle) }
        static var comInfoBanner: String { String(localized: "assets.comInfoBanner", defaultValue: "Select the appropriate rating for each factor below. Your final COM rating will be the **highest value** among the three factors.", bundle: LanguageManager.shared.bundle) }
        static var assetCriticality: String { String(localized: "assets.assetCriticality", defaultValue: "Asset Criticality", bundle: LanguageManager.shared.bundle) }
        static var impactIfFails: String { String(localized: "assets.impactIfFails", defaultValue: "Impact if asset fails", bundle: LanguageManager.shared.bundle) }
        static var operatingEnvironment: String { String(localized: "assets.operatingEnvironment", defaultValue: "Operating Environment", bundle: LanguageManager.shared.bundle) }
        static var environmentalStress: String { String(localized: "assets.environmentalStress", defaultValue: "Environmental stress factors", bundle: LanguageManager.shared.bundle) }
        static var maintenanceHistory: String { String(localized: "assets.maintenanceHistory", defaultValue: "Maintenance History", bundle: LanguageManager.shared.bundle) }
        static var nfpa70bSchedule: String { String(localized: "assets.nfpa70bSchedule", defaultValue: "Adherence to NFPA 70B maintenance schedule", bundle: LanguageManager.shared.bundle) }
        static var comStandard: String { String(localized: "assets.comStandard", defaultValue: "Standard", bundle: LanguageManager.shared.bundle) }
        static var comStandardDesc: String { String(localized: "assets.comStandardDesc", defaultValue: "Routine equipment with backup or minimal business impact", bundle: LanguageManager.shared.bundle) }
        static var comBusinessCritical: String { String(localized: "assets.comBusinessCritical", defaultValue: "Business Critical", bundle: LanguageManager.shared.bundle) }
        static var comBusinessCriticalDesc: String { String(localized: "assets.comBusinessCriticalDesc", defaultValue: "Essential for operations, significant downtime cost", bundle: LanguageManager.shared.bundle) }
        static var comLifeSafety: String { String(localized: "assets.comLifeSafety", defaultValue: "Life Safety", bundle: LanguageManager.shared.bundle) }
        static var comLifeSafetyDesc: String { String(localized: "assets.comLifeSafetyDesc", defaultValue: "Failure creates immediate safety hazard or life risk", bundle: LanguageManager.shared.bundle) }
        static var comIndoorStandard: String { String(localized: "assets.comIndoorStandard", defaultValue: "Indoor / Standard", bundle: LanguageManager.shared.bundle) }
        static var comIndoorStandardDesc: String { String(localized: "assets.comIndoorStandardDesc", defaultValue: "Climate-controlled, clean, low-stress conditions", bundle: LanguageManager.shared.bundle) }
        static var comModerateExposure: String { String(localized: "assets.comModerateExposure", defaultValue: "Moderate Exposure", bundle: LanguageManager.shared.bundle) }
        static var comModerateExposureDesc: String { String(localized: "assets.comModerateExposureDesc", defaultValue: "Outdoor, temperature extremes, or mild contamination", bundle: LanguageManager.shared.bundle) }
        static var comHarshConditions: String { String(localized: "assets.comHarshConditions", defaultValue: "Harsh Conditions", bundle: LanguageManager.shared.bundle) }
        static var comHarshConditionsDesc: String { String(localized: "assets.comHarshConditionsDesc", defaultValue: "Corrosive, dusty, wet, or otherwise aggressive environment", bundle: LanguageManager.shared.bundle) }
        static var comWellMaintained: String { String(localized: "assets.comWellMaintained", defaultValue: "Well Maintained", bundle: LanguageManager.shared.bundle) }
        static var comWellMaintainedDesc: String { String(localized: "assets.comWellMaintainedDesc", defaultValue: "Current with NFPA 70B for past 5+ years, complete records", bundle: LanguageManager.shared.bundle) }
        static var comGapsInService: String { String(localized: "assets.comGapsInService", defaultValue: "Gaps in Service", bundle: LanguageManager.shared.bundle) }
        static var comGapsInServiceDesc: String { String(localized: "assets.comGapsInServiceDesc", defaultValue: "Missed 1 maintenance cycle, incomplete records", bundle: LanguageManager.shared.bundle) }
        static var comPoorUnknown: String { String(localized: "assets.comPoorUnknown", defaultValue: "Poor / Unknown", bundle: LanguageManager.shared.bundle) }
        static var comPoorUnknownDesc: String { String(localized: "assets.comPoorUnknownDesc", defaultValue: "Missed 2+ cycles, no records, or maintenance status unknown", bundle: LanguageManager.shared.bundle) }
        static var calculatedCOMRating: String { String(localized: "assets.calculatedCOMRating", defaultValue: "CALCULATED COM RATING", bundle: LanguageManager.shared.bundle) }
        static func comMaxExplanation(criticality: Int, environment: Int, maintenance: Int) -> String {
            String(localized: "assets.comMaxExplanation", defaultValue: "Maximum of: Criticality (\(criticality)), Environment (\(environment)), Maintenance (\(maintenance))", bundle: LanguageManager.shared.bundle)
        }
        static var reset: String { String(localized: "assets.reset", defaultValue: "Reset", bundle: LanguageManager.shared.bundle) }
        static func applyRating(_ rating: Int) -> String {
            String(localized: "assets.applyRating", defaultValue: "Apply Rating (\(rating))", bundle: LanguageManager.shared.bundle)
        }

        // COM Calculator — Maintenance criteria group labels
        static var comNonserviceable: String { String(localized: "assets.comNonserviceable", defaultValue: "Nonserviceable", bundle: LanguageManager.shared.bundle) }
        static var comLevel3Poor: String { String(localized: "assets.comLevel3Poor", defaultValue: "Level 3 — Poor", bundle: LanguageManager.shared.bundle) }
        static var comLevel2Fair: String { String(localized: "assets.comLevel2Fair", defaultValue: "Level 2 — Fair", bundle: LanguageManager.shared.bundle) }
        static func comLevelN(_ n: Int) -> String {
            String(localized: "assets.comLevelN", defaultValue: "Level \(n)", bundle: LanguageManager.shared.bundle)
        }
        static var comApplyNonserviceable: String { String(localized: "assets.comApplyNonserviceable", defaultValue: "Apply — Nonserviceable", bundle: LanguageManager.shared.bundle) }
        static func comNonserviceableExplanation(criticality: Int, environment: Int) -> String {
            String(localized: "assets.comNonserviceableExplanation", defaultValue: "Criticality (\(criticality)), Environment (\(environment)), Maintenance (Nonserviceable)", bundle: LanguageManager.shared.bundle)
        }

        // COM Calculator — Nonserviceable criteria
        static var comNsExceededLife: String { String(localized: "assets.comNsExceededLife", defaultValue: "Equipment has exceeded expected service life and needs replaced (or fully refurbished)", bundle: LanguageManager.shared.bundle) }
        static var comNsImminentRisk: String { String(localized: "assets.comNsImminentRisk", defaultValue: "Equipment poses an imminent risk of injury or other negative health effect", bundle: LanguageManager.shared.bundle) }

        // COM Calculator — Level 3 criteria
        static var comL3NotMaintained: String { String(localized: "assets.comL3NotMaintained", defaultValue: "Equipment does not appear to have been maintained or has missed the last two successive maintenance cycles per EMP", bundle: LanguageManager.shared.bundle) }
        static var comL3TwoCyclesMajor: String { String(localized: "assets.comL3TwoCyclesMajor", defaultValue: "Previous two maintenance cycles revealed issues requiring repair or replacement of major components", bundle: LanguageManager.shared.bundle) }
        static var comL3NearEndLife: String { String(localized: "assets.comL3NearEndLife", defaultValue: "Equipment is near end of expected service life", bundle: LanguageManager.shared.bundle) }
        static var comL3UrgentAction: String { String(localized: "assets.comL3UrgentAction", defaultValue: "Equipment requires urgent action to resolve deficiencies; urgent actions identified from predictive techniques", bundle: LanguageManager.shared.bundle) }

        // COM Calculator — Level 2 criteria
        static var comL2ResultsDeviate: String { String(localized: "assets.comL2ResultsDeviate", defaultValue: "Maintenance results deviate from past results or indicate more frequent maintenance per manufacturer data", bundle: LanguageManager.shared.bundle) }
        static var comL2PrevCycleMajor: String { String(localized: "assets.comL2PrevCycleMajor", defaultValue: "Previous maintenance cycle revealed issues requiring repair or replacement of major equipment components", bundle: LanguageManager.shared.bundle) }
        static var comL2MonitoringNotifications: String { String(localized: "assets.comL2MonitoringNotifications", defaultValue: "Notifications from the continuous monitoring system since the prior assessment", bundle: LanguageManager.shared.bundle) }
        static var comL2ActivePredictive: String { String(localized: "assets.comL2ActivePredictive", defaultValue: "Active recommendations from predictive techniques", bundle: LanguageManager.shared.bundle) }
        static var comL2MissedCycle: String { String(localized: "assets.comL2MissedCycle", defaultValue: "Equipment may have missed the most recent maintenance cycle", bundle: LanguageManager.shared.bundle) }
        static var comL2MinorReconditioning: String { String(localized: "assets.comL2MinorReconditioning", defaultValue: "Enclosure may require minor reconditioning (replacing screws, cutouts, minor cleaning)", bundle: LanguageManager.shared.bundle) }
        static var comL2NonUrgentRecommendations: String { String(localized: "assets.comL2NonUrgentRecommendations", defaultValue: "Non-urgent unresolved recommendations on this equipment", bundle: LanguageManager.shared.bundle) }

        // Location picker
        static var searchLocations: String { String(localized: "assets.searchLocations", defaultValue: "Search locations", bundle: LanguageManager.shared.bundle) }
        static var selectLocation: String { String(localized: "assets.selectLocation", defaultValue: "Select Location", bundle: LanguageManager.shared.bundle) }
        static var location: String { String(localized: "assets.location", defaultValue: "Location", bundle: LanguageManager.shared.bundle) }
        static var selectLocationPlaceholder: String { String(localized: "assets.selectLocationPlaceholder", defaultValue: "Select location", bundle: LanguageManager.shared.bundle) }

        // Search
        static var searchPlaceholder: String { String(localized: "assets.searchPlaceholder", defaultValue: "Search...", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Locations
    enum Locations {
        static var locations: String { String(localized: "locations.locations", defaultValue: "Locations", bundle: LanguageManager.shared.bundle) }
        static var noBuildings: String { String(localized: "locations.noBuildings", defaultValue: "No Buildings", bundle: LanguageManager.shared.bundle) }
        static var tapPlusToAddBuilding: String { String(localized: "locations.tapPlusToAddBuilding", defaultValue: "Tap the + button to add a building", bundle: LanguageManager.shared.bundle) }
        static var processing: String { String(localized: "locations.processing", defaultValue: "Processing...", bundle: LanguageManager.shared.bundle) }

        // QR scan alerts
        static var assetFound: String { String(localized: "locations.assetFound", defaultValue: "Asset Found", bundle: LanguageManager.shared.bundle) }
        static func assetFoundMessage(_ name: String) -> String {
            String(localized: "locations.assetFoundMessage", defaultValue: "Asset '\(name)' has been added to the session and IR photo screen will open.", bundle: LanguageManager.shared.bundle)
        }
        static var assetNotFound: String { String(localized: "locations.assetNotFound", defaultValue: "Asset Not Found", bundle: LanguageManager.shared.bundle) }
        static func assetNotFoundMessage(_ code: String) -> String {
            String(localized: "locations.assetNotFoundMessage", defaultValue: "No asset found with QR code: \(code)", bundle: LanguageManager.shared.bundle)
        }
        static var assetAlreadyInSession: String { String(localized: "locations.assetAlreadyInSession", defaultValue: "Asset Already in Session", bundle: LanguageManager.shared.bundle) }
        static func assetAlreadyMessage(_ name: String) -> String {
            String(localized: "locations.assetAlreadyMessage", defaultValue: "Asset '\(name)' is already linked to this session.", bundle: LanguageManager.shared.bundle)
        }
        static var assetNotAssignedToRoom: String { String(localized: "locations.assetNotAssignedToRoom", defaultValue: "Asset Not Assigned to Room", bundle: LanguageManager.shared.bundle) }
        static func assetNotAssignedMessage(_ name: String) -> String {
            String(localized: "locations.assetNotAssignedMessage", defaultValue: "Asset '\(name)' must be assigned to a room before adding to session.", bundle: LanguageManager.shared.bundle)
        }

        // Context menus
        static var editBuilding: String { String(localized: "locations.editBuilding", defaultValue: "Edit Building", bundle: LanguageManager.shared.bundle) }
        static var deleteBuilding: String { String(localized: "locations.deleteBuilding", defaultValue: "Delete Building", bundle: LanguageManager.shared.bundle) }
        static var editFloor: String { String(localized: "locations.editFloor", defaultValue: "Edit Floor", bundle: LanguageManager.shared.bundle) }
        static var deleteFloor: String { String(localized: "locations.deleteFloor", defaultValue: "Delete Floor", bundle: LanguageManager.shared.bundle) }
        static var editRoom: String { String(localized: "locations.editRoom", defaultValue: "Edit Room", bundle: LanguageManager.shared.bundle) }
        static var deleteRoom: String { String(localized: "locations.deleteRoom", defaultValue: "Delete Room", bundle: LanguageManager.shared.bundle) }

        // Count labels
        static func floorsCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "locations.floorSingular", defaultValue: "\(count) floor", bundle: LanguageManager.shared.bundle)
                : String(localized: "locations.floorPlural", defaultValue: "\(count) floors", bundle: LanguageManager.shared.bundle)
        }
        static func roomsCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "locations.roomSingular", defaultValue: "\(count) room", bundle: LanguageManager.shared.bundle)
                : String(localized: "locations.roomPlural", defaultValue: "\(count) rooms", bundle: LanguageManager.shared.bundle)
        }
        static func unassignedAssetsCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "locations.unassignedAssetSingular", defaultValue: "\(count) unassigned asset", bundle: LanguageManager.shared.bundle)
                : String(localized: "locations.unassignedAssetPlural", defaultValue: "\(count) unassigned assets", bundle: LanguageManager.shared.bundle)
        }
        static func nodesCount(_ count: Int) -> String {
            count == 1
                ? String(localized: "locations.nodeSingular", defaultValue: "\(count) node", bundle: LanguageManager.shared.bundle)
                : String(localized: "locations.nodePlural", defaultValue: "\(count) nodes", bundle: LanguageManager.shared.bundle)
        }
        static func childrenCount(_ count: Int) -> String {
            String(localized: "locations.childrenCount", defaultValue: "\(count) children", bundle: LanguageManager.shared.bundle)
        }

        // No-location views
        static var noUnassignedAssets: String { String(localized: "locations.noUnassignedAssets", defaultValue: "No Unassigned Assets", bundle: LanguageManager.shared.bundle) }
        static var allAssetsAssigned: String { String(localized: "locations.allAssetsAssigned", defaultValue: "All assets have been assigned to rooms", bundle: LanguageManager.shared.bundle) }
        static var allSessionAssetsAssigned: String { String(localized: "locations.allSessionAssetsAssigned", defaultValue: "All session assets have been assigned to rooms", bundle: LanguageManager.shared.bundle) }

        // Building editor
        static var buildingInformation: String { String(localized: "locations.buildingInformation", defaultValue: "Building Information", bundle: LanguageManager.shared.bundle) }
        static var buildingName: String { String(localized: "locations.buildingName", defaultValue: "Building Name", bundle: LanguageManager.shared.bundle) }
        static var newBuilding: String { String(localized: "locations.newBuilding", defaultValue: "New Building", bundle: LanguageManager.shared.bundle) }
        static var editBuildingTitle: String { String(localized: "locations.editBuildingTitle", defaultValue: "Edit Building", bundle: LanguageManager.shared.bundle) }
        static var accessNotes: String { String(localized: "locations.accessNotes", defaultValue: "Access Notes", bundle: LanguageManager.shared.bundle) }
        static var showAccessNotes: String { String(localized: "locations.showAccessNotes", defaultValue: "Show Access Notes", bundle: LanguageManager.shared.bundle) }

        // Floor editor
        static var floorInformation: String { String(localized: "locations.floorInformation", defaultValue: "Floor Information", bundle: LanguageManager.shared.bundle) }
        static var floorName: String { String(localized: "locations.floorName", defaultValue: "Floor Name", bundle: LanguageManager.shared.bundle) }
        static var building: String { String(localized: "locations.building", defaultValue: "Building", bundle: LanguageManager.shared.bundle) }
        static var newFloor: String { String(localized: "locations.newFloor", defaultValue: "New Floor", bundle: LanguageManager.shared.bundle) }
        static var editFloorTitle: String { String(localized: "locations.editFloorTitle", defaultValue: "Edit Floor", bundle: LanguageManager.shared.bundle) }

        // Room editor
        static var roomInformation: String { String(localized: "locations.roomInformation", defaultValue: "Room Information", bundle: LanguageManager.shared.bundle) }
        static var roomName: String { String(localized: "locations.roomName", defaultValue: "Room Name", bundle: LanguageManager.shared.bundle) }
        static var floor: String { String(localized: "locations.floor", defaultValue: "Floor", bundle: LanguageManager.shared.bundle) }
        static var newRoom: String { String(localized: "locations.newRoom", defaultValue: "New Room", bundle: LanguageManager.shared.bundle) }
        static var editRoomTitle: String { String(localized: "locations.editRoomTitle", defaultValue: "Edit Room", bundle: LanguageManager.shared.bundle) }

        // Hierarchical picker
        static var noLocations: String { String(localized: "locations.noLocations", defaultValue: "No Locations", bundle: LanguageManager.shared.bundle) }
        static var createBuildingToStart: String { String(localized: "locations.createBuildingToStart", defaultValue: "Create a building to get started", bundle: LanguageManager.shared.bundle) }

        // Move location (parent reassignment)
        static var selectBuilding: String { String(localized: "locations.selectBuilding", defaultValue: "Select Building", bundle: LanguageManager.shared.bundle) }
        static var selectFloor: String { String(localized: "locations.selectFloor", defaultValue: "Select Floor", bundle: LanguageManager.shared.bundle) }
        static var searchBuildings: String { String(localized: "locations.searchBuildings", defaultValue: "Search buildings", bundle: LanguageManager.shared.bundle) }
        static var searchFloors: String { String(localized: "locations.searchFloors", defaultValue: "Search floors", bundle: LanguageManager.shared.bundle) }
        static var noOtherBuildings: String { String(localized: "locations.noOtherBuildings", defaultValue: "No other buildings available", bundle: LanguageManager.shared.bundle) }
        static var noOtherFloors: String { String(localized: "locations.noOtherFloors", defaultValue: "No other floors available in this building", bundle: LanguageManager.shared.bundle) }
        static var noBuildingsFound: String { String(localized: "locations.noBuildingsFound", defaultValue: "No buildings found", bundle: LanguageManager.shared.bundle) }
        static var noFloorsFound: String { String(localized: "locations.noFloorsFound", defaultValue: "No floors found", bundle: LanguageManager.shared.bundle) }
        static var moveFloorTitle: String { String(localized: "locations.moveFloorTitle", defaultValue: "Move floor?", bundle: LanguageManager.shared.bundle) }
        static func moveFloorMessage(name: String, from: String, to: String) -> String {
            String(localized: "locations.moveFloorMessage", defaultValue: "Move \(name) from \(from) to \(to)? Rooms, photos, and linked data will move with it.", bundle: LanguageManager.shared.bundle)
        }
        static var moveRoomTitle: String { String(localized: "locations.moveRoomTitle", defaultValue: "Move room?", bundle: LanguageManager.shared.bundle) }
        static func moveRoomMessage(name: String, floor: String, building: String) -> String {
            String(localized: "locations.moveRoomMessage", defaultValue: "Move \(name) to \(floor) in \(building)? Photos and linked data are preserved.", bundle: LanguageManager.shared.bundle)
        }
        static var moveConfirm: String { String(localized: "locations.moveConfirm", defaultValue: "Move", bundle: LanguageManager.shared.bundle) }

        // Room detail
        static var noAssets: String { String(localized: "locations.noAssets", defaultValue: "No Assets", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Tasks
    enum Tasks {
        static var taskDetails: String { String(localized: "tasks.taskDetails", defaultValue: "Task Details", bundle: LanguageManager.shared.bundle) }
        static var newTask: String { String(localized: "tasks.newTask", defaultValue: "New Task", bundle: LanguageManager.shared.bundle) }
        static var newSimpleTask: String { String(localized: "tasks.newSimpleTask", defaultValue: "New Simple Task", bundle: LanguageManager.shared.bundle) }
        static var deleteTask: String { String(localized: "tasks.deleteTask", defaultValue: "Delete Task", bundle: LanguageManager.shared.bundle) }
        static var deleteTaskConfirm: String { String(localized: "tasks.deleteTaskConfirm", defaultValue: "Are you sure you want to delete this task? This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static var completeTask: String { String(localized: "tasks.completeTask", defaultValue: "Complete Task", bundle: LanguageManager.shared.bundle) }
        static var complete: String { String(localized: "tasks.complete", defaultValue: "Complete", bundle: LanguageManager.shared.bundle) }
        static var completeTaskMessage: String { String(localized: "tasks.completeTaskMessage", defaultValue: "By completing this task, all linked assets will be marked as complete.", bundle: LanguageManager.shared.bundle) }
        static var title: String { String(localized: "tasks.title", defaultValue: "Title", bundle: LanguageManager.shared.bundle) }
        static var enterTaskTitle: String { String(localized: "tasks.enterTaskTitle", defaultValue: "Enter task title", bundle: LanguageManager.shared.bundle) }
        static var description_: String { String(localized: "tasks.description", defaultValue: "Description", bundle: LanguageManager.shared.bundle) }
        static var describeTask: String { String(localized: "tasks.describeTask", defaultValue: "Describe the task...", bundle: LanguageManager.shared.bundle) }
        static var resolution: String { String(localized: "tasks.resolution", defaultValue: "Resolution", bundle: LanguageManager.shared.bundle) }
        static var linkedAssets: String { String(localized: "tasks.linkedAssets", defaultValue: "Linked Assets", bundle: LanguageManager.shared.bundle) }
        static var linkAssets: String { String(localized: "tasks.linkAssets", defaultValue: "Link Assets", bundle: LanguageManager.shared.bundle) }
        static var noLinkedAssets: String { String(localized: "tasks.noLinkedAssets", defaultValue: "No linked assets", bundle: LanguageManager.shared.bundle) }
        static var dueDate: String { String(localized: "tasks.dueDate", defaultValue: "Due Date", bundle: LanguageManager.shared.bundle) }
        static var setDueDate: String { String(localized: "tasks.setDueDate", defaultValue: "Set Due Date", bundle: LanguageManager.shared.bundle) }
        static var selectDate: String { String(localized: "tasks.selectDate", defaultValue: "Select Date", bundle: LanguageManager.shared.bundle) }
        static var schedule: String { String(localized: "tasks.schedule", defaultValue: "Schedule", bundle: LanguageManager.shared.bundle) }
        static var optionalDueDate: String { String(localized: "tasks.optionalDueDate", defaultValue: "Optional: Set a due date for this task", bundle: LanguageManager.shared.bundle) }
        static var assignmentOptional: String { String(localized: "tasks.assignmentOptional", defaultValue: "Assignment (Optional)", bundle: LanguageManager.shared.bundle) }
        static var assignment: String { String(localized: "tasks.assignment", defaultValue: "Assignment", bundle: LanguageManager.shared.bundle) }
        static var assignToAsset: String { String(localized: "tasks.assignToAsset", defaultValue: "Assign to Asset", bundle: LanguageManager.shared.bundle) }
        static var asset: String { String(localized: "tasks.asset", defaultValue: "Asset", bundle: LanguageManager.shared.bundle) }
        static var noAsset: String { String(localized: "tasks.noAsset", defaultValue: "No Asset", bundle: LanguageManager.shared.bundle) }
        static var optionalSelectAsset: String { String(localized: "tasks.optionalSelectAsset", defaultValue: "Optional: Select one asset for this task", bundle: LanguageManager.shared.bundle) }
        static var linkedToActiveSession: String { String(localized: "tasks.linkedToActiveSession", defaultValue: "Linked to active session:", bundle: LanguageManager.shared.bundle) }
        static func taskAssignedTo(_ name: String) -> String {
            String(localized: "tasks.taskAssignedTo", defaultValue: "This task will be assigned to:", bundle: LanguageManager.shared.bundle)
        }
        static var formOptional: String { String(localized: "tasks.formOptional", defaultValue: "Form (Optional)", bundle: LanguageManager.shared.bundle) }
        static var attachForm: String { String(localized: "tasks.attachForm", defaultValue: "Attach Form", bundle: LanguageManager.shared.bundle) }
        static var noForm: String { String(localized: "tasks.noForm", defaultValue: "No Form", bundle: LanguageManager.shared.bundle) }
        static var markAsCompleted: String { String(localized: "tasks.markAsCompleted", defaultValue: "Mark as Completed", bundle: LanguageManager.shared.bundle) }
        static var status: String { String(localized: "tasks.status", defaultValue: "Status", bundle: LanguageManager.shared.bundle) }
        static var titleIsRequired: String { String(localized: "tasks.titleIsRequired", defaultValue: "Title is required", bundle: LanguageManager.shared.bundle) }
        static var descriptionIsRequired: String { String(localized: "tasks.descriptionIsRequired", defaultValue: "Description is required", bundle: LanguageManager.shared.bundle) }
        static var dueDateCannotBeInPast: String { String(localized: "tasks.dueDateCannotBeInPast", defaultValue: "Due date cannot be in the past", bundle: LanguageManager.shared.bundle) }

        // Task Type
        static var taskType: String { String(localized: "tasks.taskType", defaultValue: "Task Type", bundle: LanguageManager.shared.bundle) }
        static var selectTaskType: String { String(localized: "tasks.selectTaskType", defaultValue: "Select task type", bundle: LanguageManager.shared.bundle) }
        static var taskTypeIsRequired: String { String(localized: "tasks.taskTypeIsRequired", defaultValue: "Task type is required", bundle: LanguageManager.shared.bundle) }
        static var taskTypePM: String { String(localized: "tasks.taskType.PM", defaultValue: "PM", bundle: LanguageManager.shared.bundle) }
        static var taskTypeInspection: String { String(localized: "tasks.taskType.INSPECTION", defaultValue: "Inspection", bundle: LanguageManager.shared.bundle) }
        static var taskTypeRepair: String { String(localized: "tasks.taskType.REPAIR", defaultValue: "Repair", bundle: LanguageManager.shared.bundle) }
        static var taskTypeTest: String { String(localized: "tasks.taskType.TEST", defaultValue: "Test", bundle: LanguageManager.shared.bundle) }
        static var taskTypeDocumentation: String { String(localized: "tasks.taskType.DOCUMENTATION", defaultValue: "Documentation", bundle: LanguageManager.shared.bundle) }
        static var taskTypeOther: String { String(localized: "tasks.taskType.OTHER", defaultValue: "Other", bundle: LanguageManager.shared.bundle) }

        // Procedure
        static var procedure: String { String(localized: "tasks.procedure", defaultValue: "Procedure", bundle: LanguageManager.shared.bundle) }
        static var selectProcedure: String { String(localized: "tasks.selectProcedure", defaultValue: "Select a procedure", bundle: LanguageManager.shared.bundle) }
        static var noProcedure: String { String(localized: "tasks.noProcedure", defaultValue: "No procedure", bundle: LanguageManager.shared.bundle) }
        static var noProceduresAvailable: String { String(localized: "tasks.noProceduresAvailable", defaultValue: "No procedures available", bundle: LanguageManager.shared.bundle) }
        static var searchProcedures: String { String(localized: "tasks.searchProcedures", defaultValue: "Search procedures", bundle: LanguageManager.shared.bundle) }
        static var procedureGroupGeneral: String { String(localized: "tasks.procedure.groupGeneral", defaultValue: "General", bundle: LanguageManager.shared.bundle) }
        static var procedureOptional: String { String(localized: "tasks.procedureOptional", defaultValue: "Optional: Attach a procedure for this task", bundle: LanguageManager.shared.bundle) }
        static var procedureDisabledMultipleAssets: String { String(localized: "tasks.procedureDisabledMultipleAssets", defaultValue: "A procedure can't be set when multiple assets are linked", bundle: LanguageManager.shared.bundle) }

        static func othersCount(_ count: Int) -> String {
            String(localized: "tasks.othersCount", defaultValue: "+ \(count) others", bundle: LanguageManager.shared.bundle)
        }

        // Forms
        static var forms: String { String(localized: "tasks.forms", defaultValue: "Forms", bundle: LanguageManager.shared.bundle) }
        static var taskPhotos: String { String(localized: "tasks.taskPhotos", defaultValue: "Task Photos", bundle: LanguageManager.shared.bundle) }
        static var select: String { String(localized: "tasks.select", defaultValue: "Select", bundle: LanguageManager.shared.bundle) }
        static var formsFooter: String { String(localized: "tasks.formsFooter", defaultValue: "Attach forms to this task. Each form instance can be linked to specific assets.", bundle: LanguageManager.shared.bundle) }
        static var manageForms: String { String(localized: "tasks.manageForms", defaultValue: "Manage Forms", bundle: LanguageManager.shared.bundle) }
        static var noFormsSelected: String { String(localized: "tasks.noFormsSelected", defaultValue: "No forms selected", bundle: LanguageManager.shared.bundle) }
        static func instanceLabel(_ current: Int, _ total: Int) -> String {
            String(localized: "tasks.instanceLabel", defaultValue: "(Instance \(current) of \(total))", bundle: LanguageManager.shared.bundle)
        }
        static var noNodesLinked: String { String(localized: "tasks.noNodesLinked", defaultValue: "No nodes linked", bundle: LanguageManager.shared.bundle) }
        static func moreNodes(_ count: Int) -> String {
            String(localized: "tasks.moreNodes", defaultValue: "+ \(count) more", bundle: LanguageManager.shared.bundle)
        }
        static var searchForms: String { String(localized: "tasks.searchForms", defaultValue: "Search forms...", bundle: LanguageManager.shared.bundle) }
        static var noFormsForAssets: String { String(localized: "tasks.noFormsForAssets", defaultValue: "No forms available for the selected assets", bundle: LanguageManager.shared.bundle) }
        static var noGeneralForms: String { String(localized: "tasks.noGeneralForms", defaultValue: "No general forms available", bundle: LanguageManager.shared.bundle) }
        static var selectForm: String { String(localized: "tasks.selectForm", defaultValue: "Select Form", bundle: LanguageManager.shared.bundle) }
        static var general: String { String(localized: "tasks.general", defaultValue: "General", bundle: LanguageManager.shared.bundle) }

        // Node selection for forms
        static func assetsSelected(_ count: Int) -> String {
            count == 1
                ? String(localized: "tasks.assetSelectedSingular", defaultValue: "\(count) asset selected", bundle: LanguageManager.shared.bundle)
                : String(localized: "tasks.assetsSelectedPlural", defaultValue: "\(count) assets selected", bundle: LanguageManager.shared.bundle)
        }
        static var assetsLinkedToTask: String { String(localized: "tasks.assetsLinkedToTask", defaultValue: "Assets Linked to Task", bundle: LanguageManager.shared.bundle) }
        static var availableAssets: String { String(localized: "tasks.availableAssets", defaultValue: "Available Assets", bundle: LanguageManager.shared.bundle) }
        static var otherAvailableAssets: String { String(localized: "tasks.otherAvailableAssets", defaultValue: "Other Available Assets", bundle: LanguageManager.shared.bundle) }
        static var noAssetsAvailable: String { String(localized: "tasks.noAssetsAvailable", defaultValue: "No assets available", bundle: LanguageManager.shared.bundle) }
        static var noAssetsLinkedYet: String { String(localized: "tasks.noAssetsLinkedYet", defaultValue: "No assets linked yet", bundle: LanguageManager.shared.bundle) }
        static var linkAssetsToForm: String { String(localized: "tasks.linkAssetsToForm", defaultValue: "Link Assets to Form", bundle: LanguageManager.shared.bundle) }

        // Form instance nodes
        static var formInstance: String { String(localized: "tasks.formInstance", defaultValue: "Form Instance", bundle: LanguageManager.shared.bundle) }
        static var submitted: String { String(localized: "tasks.submitted", defaultValue: "Submitted", bundle: LanguageManager.shared.bundle) }
        static var formInstanceNodes: String { String(localized: "tasks.formInstanceNodes", defaultValue: "Form Instance Nodes", bundle: LanguageManager.shared.bundle) }
        static var addNode: String { String(localized: "tasks.addNode", defaultValue: "Add Node", bundle: LanguageManager.shared.bundle) }
        static var noNodesLinkedToForm: String { String(localized: "tasks.noNodesLinkedToForm", defaultValue: "No nodes linked to this form", bundle: LanguageManager.shared.bundle) }
        static var linkNodesDescription: String { String(localized: "tasks.linkNodesDescription", defaultValue: "Link nodes to specify which equipment this form applies to", bundle: LanguageManager.shared.bundle) }
        static var linkNode: String { String(localized: "tasks.linkNode", defaultValue: "Link Node", bundle: LanguageManager.shared.bundle) }
        static func linkedNodesCount(_ count: Int) -> String {
            String(localized: "tasks.linkedNodesCount", defaultValue: "Linked Nodes (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static var linkedNodesFooter: String { String(localized: "tasks.linkedNodesFooter", defaultValue: "These nodes specify which equipment this form instance applies to", bundle: LanguageManager.shared.bundle) }
        static var searchNodes: String { String(localized: "tasks.searchNodes", defaultValue: "Search nodes...", bundle: LanguageManager.shared.bundle) }
        static var noAvailableNodes: String { String(localized: "tasks.noAvailableNodes", defaultValue: "No available nodes", bundle: LanguageManager.shared.bundle) }
        static var noNodesFound: String { String(localized: "tasks.noNodesFound", defaultValue: "No nodes found", bundle: LanguageManager.shared.bundle) }
        static var tryDifferentSearch: String { String(localized: "tasks.tryDifferentSearch", defaultValue: "Try a different search term", bundle: LanguageManager.shared.bundle) }
        static var selectNode: String { String(localized: "tasks.selectNode", defaultValue: "Select Node", bundle: LanguageManager.shared.bundle) }

        // Task creation configuration
        static var task: String { String(localized: "tasks.task", defaultValue: "Task", bundle: LanguageManager.shared.bundle) }
        static var tasks: String { String(localized: "tasks.tasks", defaultValue: "Tasks", bundle: LanguageManager.shared.bundle) }
        static var completed: String { String(localized: "tasks.completed", defaultValue: "Completed", bundle: LanguageManager.shared.bundle) }
        static var assignedToMe: String { String(localized: "tasks.assignedToMe", defaultValue: "Assigned to Me", bundle: LanguageManager.shared.bundle) }
        static var ownedByMe: String { String(localized: "tasks.ownedByMe", defaultValue: "Owned by Me", bundle: LanguageManager.shared.bundle) }
        static var overdue: String { String(localized: "tasks.overdue", defaultValue: "Overdue", bundle: LanguageManager.shared.bundle) }
        static var dueToday: String { String(localized: "tasks.dueToday", defaultValue: "Due Today", bundle: LanguageManager.shared.bundle) }
        static var dueThisWeek: String { String(localized: "tasks.dueThisWeek", defaultValue: "Due This Week", bundle: LanguageManager.shared.bundle) }
        static var listenForNewAssets: String { String(localized: "tasks.listenForNewAssets", defaultValue: "Listen for New Assets", bundle: LanguageManager.shared.bundle) }
        static var listening: String { String(localized: "tasks.listening", defaultValue: "Listening", bundle: LanguageManager.shared.bundle) }
        static var unableToLoadSLD: String { String(localized: "tasks.unableToLoadSLD", defaultValue: "Unable to load SLD", bundle: LanguageManager.shared.bundle) }
        static var noTasks: String { String(localized: "tasks.noTasks", defaultValue: "No tasks", bundle: LanguageManager.shared.bundle) }
        static var showAllTasks: String { String(localized: "tasks.showAllTasks", defaultValue: "Show All Tasks", bundle: LanguageManager.shared.bundle) }
        static var showIncompleteOnly: String { String(localized: "tasks.showIncompleteOnly", defaultValue: "Show Incomplete Only", bundle: LanguageManager.shared.bundle) }
        static var showCompletedOnly: String { String(localized: "tasks.showCompletedOnly", defaultValue: "Show Completed Only", bundle: LanguageManager.shared.bundle) }
        static var createNewTaskMessage: String { String(localized: "tasks.createNewTaskMessage", defaultValue: "Create a new task to track work items", bundle: LanguageManager.shared.bundle) }
        static var before: String { String(localized: "tasks.before", defaultValue: "Before", bundle: LanguageManager.shared.bundle) }
        static var after: String { String(localized: "tasks.after", defaultValue: "After", bundle: LanguageManager.shared.bundle) }
        static var creatingTaskFor: String { String(localized: "tasks.creatingTaskFor", defaultValue: "Creating task for:", bundle: LanguageManager.shared.bundle) }
        static func typeLabel(_ type: String) -> String {
            String(localized: "tasks.typeLabel", defaultValue: "Type: \(type)", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Connections
    enum Connections {
        static var connectionClass: String { String(localized: "connections.connectionClass", defaultValue: "Connection Class", bundle: LanguageManager.shared.bundle) }
        static var selectConnectionClass: String { String(localized: "connections.selectConnectionClass", defaultValue: "Select connection class", bundle: LanguageManager.shared.bundle) }
        static var edgeDetails: String { String(localized: "connections.edgeDetails", defaultValue: "Edge Details", bundle: LanguageManager.shared.bundle) }
        static var connection: String { String(localized: "connections.connection", defaultValue: "Connection", bundle: LanguageManager.shared.bundle) }
        static var createConnection: String { String(localized: "connections.createConnection", defaultValue: "Create Connection", bundle: LanguageManager.shared.bundle) }
        static var connectionType: String { String(localized: "connections.connectionType", defaultValue: "Connection Type", bundle: LanguageManager.shared.bundle) }
        static var selectType: String { String(localized: "connections.selectType", defaultValue: "Select type", bundle: LanguageManager.shared.bundle) }
        static var selectSourceNode: String { String(localized: "connections.selectSourceNode", defaultValue: "Select source node", bundle: LanguageManager.shared.bundle) }
        static var selectTargetNode: String { String(localized: "connections.selectTargetNode", defaultValue: "Select target node", bundle: LanguageManager.shared.bundle) }
        static var selectTerminal: String { String(localized: "connections.selectTerminal", defaultValue: "Select terminal", bundle: LanguageManager.shared.bundle) }
        static var terminal: String { String(localized: "connections.terminal", defaultValue: "Terminal", bundle: LanguageManager.shared.bundle) }
        static var preview: String { String(localized: "connections.preview", defaultValue: "Preview", bundle: LanguageManager.shared.bundle) }
        static var missingNode: String { String(localized: "connections.missingNode", defaultValue: "Missing Node", bundle: LanguageManager.shared.bundle) }
        static var notAssigned: String { String(localized: "connections.notAssigned", defaultValue: "Not Assigned", bundle: LanguageManager.shared.bundle) }
        static var unknownNode: String { String(localized: "connections.unknownNode", defaultValue: "Unknown Node", bundle: LanguageManager.shared.bundle) }
        static var selectNode: String { String(localized: "connections.selectNode", defaultValue: "Select node", bundle: LanguageManager.shared.bundle) }
        static var searchByLabelOrClass: String { String(localized: "connections.searchByLabelOrClass", defaultValue: "Search by label or class", bundle: LanguageManager.shared.bundle) }
        static var searchConnections: String { String(localized: "connections.searchConnections", defaultValue: "Search connections...", bundle: LanguageManager.shared.bundle) }
        static var from: String { String(localized: "connections.from", defaultValue: "From", bundle: LanguageManager.shared.bundle) }
        static var to: String { String(localized: "connections.to", defaultValue: "To", bundle: LanguageManager.shared.bundle) }
        static func selectedCount(_ count: Int) -> String {
            String(localized: "connections.selectedCount", defaultValue: "\(count) Selected", bundle: LanguageManager.shared.bundle)
        }
        static var sourceTargetMustDiffer: String { String(localized: "connections.sourceTargetMustDiffer", defaultValue: "Source and target must be different nodes", bundle: LanguageManager.shared.bundle) }
        static var parentCannotConnectChild: String { String(localized: "connections.parentCannotConnectChild", defaultValue: "Parent cannot connect to its child node", bundle: LanguageManager.shared.bundle) }
        static var duplicateConnectionExists: String { String(localized: "connections.duplicateConnectionExists", defaultValue: "A connection already exists between these nodes", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectSourceNode: String { String(localized: "connections.pleaseSelectSourceNode", defaultValue: "Please select a source node", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectTargetNode: String { String(localized: "connections.pleaseSelectTargetNode", defaultValue: "Please select a target node", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectConnectionType: String { String(localized: "connections.pleaseSelectConnectionType", defaultValue: "Please select a connection type", bundle: LanguageManager.shared.bundle) }
        static var deleteConnectionConfirmSingle: String { String(localized: "connections.deleteConnectionConfirmSingle", defaultValue: "Are you sure you want to delete this connection? This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static func deleteConnectionsConfirm(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "connections.deleteConnectionsConfirm", defaultValue: "Are you sure you want to delete \(count) connection\(s)? This action cannot be undone.", bundle: LanguageManager.shared.bundle)
        }
        // Terminal validation
        static var noSourceTerminals: String { String(localized: "connections.noSourceTerminals", defaultValue: "Source node has no source-eligible terminals", bundle: LanguageManager.shared.bundle) }
        static var noTargetTerminals: String { String(localized: "connections.noTargetTerminals", defaultValue: "Target node has no target-eligible terminals", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectSourceTerminal: String { String(localized: "connections.pleaseSelectSourceTerminal", defaultValue: "Please select a source terminal", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectTargetTerminal: String { String(localized: "connections.pleaseSelectTargetTerminal", defaultValue: "Please select a target terminal", bundle: LanguageManager.shared.bundle) }
        // Swap
        static var swapSourceAndTarget: String { String(localized: "connections.swapSourceAndTarget", defaultValue: "Swap source and target", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Photos
    enum Photos {
        static var gallery: String { String(localized: "photos.gallery", defaultValue: "Gallery", bundle: LanguageManager.shared.bundle) }
        static var camera: String { String(localized: "photos.camera", defaultValue: "Camera", bundle: LanguageManager.shared.bundle) }
        static var retake: String { String(localized: "photos.retake", defaultValue: "Retake", bundle: LanguageManager.shared.bundle) }
        static var usePhoto: String { String(localized: "photos.usePhoto", defaultValue: "Use Photo", bundle: LanguageManager.shared.bundle) }
        static var deleteThisPhoto: String { String(localized: "photos.deleteThisPhoto", defaultValue: "Delete this photo?", bundle: LanguageManager.shared.bundle) }
        static var setAsDefault: String { String(localized: "photos.setAsDefault", defaultValue: "Set as Default", bundle: LanguageManager.shared.bundle) }
        static var addCaption: String { String(localized: "photos.addCaption", defaultValue: "Add Caption", bundle: LanguageManager.shared.bundle) }
        static var editCaption: String { String(localized: "photos.editCaption", defaultValue: "Edit Caption", bundle: LanguageManager.shared.bundle) }
        static var addCaptionPlaceholder: String { String(localized: "photos.addCaptionPlaceholder", defaultValue: "Add a caption...", bundle: LanguageManager.shared.bundle) }

        // Camera flash modes (iPad custom camera flash chip)
        static var flashAuto: String { String(localized: "photos.flashAuto", defaultValue: "Auto", bundle: LanguageManager.shared.bundle) }
        static var flashOn: String { String(localized: "photos.flashOn", defaultValue: "On", bundle: LanguageManager.shared.bundle) }
        static var flashOff: String { String(localized: "photos.flashOff", defaultValue: "Off", bundle: LanguageManager.shared.bundle) }

        // Photo type categories - Node
        static var typeProfile: String { String(localized: "photos.typeProfile", defaultValue: "Profile", bundle: LanguageManager.shared.bundle) }
        static var typeNameplate: String { String(localized: "photos.typeNameplate", defaultValue: "Nameplate", bundle: LanguageManager.shared.bundle) }
        static var typePanelSchedule: String { String(localized: "photos.typePanelSchedule", defaultValue: "Panel Schedule", bundle: LanguageManager.shared.bundle) }
        static var typeArcFlashSticker: String { String(localized: "photos.typeArcFlashSticker", defaultValue: "Arc Flash Sticker", bundle: LanguageManager.shared.bundle) }

        // Photo type categories - Issue
        static var typeIssue: String { String(localized: "photos.typeIssue", defaultValue: "Issue", bundle: LanguageManager.shared.bundle) }
        static var typeContext: String { String(localized: "photos.typeContext", defaultValue: "Context", bundle: LanguageManager.shared.bundle) }
        static var typeResolution: String { String(localized: "photos.typeResolution", defaultValue: "Resolution", bundle: LanguageManager.shared.bundle) }

        // Photo type categories - Task
        static var typeGeneral: String { String(localized: "photos.typeGeneral", defaultValue: "General", bundle: LanguageManager.shared.bundle) }
        static var typeBefore: String { String(localized: "photos.typeBefore", defaultValue: "Before", bundle: LanguageManager.shared.bundle) }
        static var typeAfter: String { String(localized: "photos.typeAfter", defaultValue: "After", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Forms / Entity Creation
    enum Forms {
        // Entity creation framework
        static var validationError: String { String(localized: "forms.validationError", defaultValue: "Validation Error", bundle: LanguageManager.shared.bundle) }
        static var qrCodeError: String { String(localized: "forms.qrCodeError", defaultValue: "QR Code Error", bundle: LanguageManager.shared.bundle) }
        static var assetIsRequired: String { String(localized: "forms.assetIsRequired", defaultValue: "Asset is required", bundle: LanguageManager.shared.bundle) }
        static var viaForm: String { String(localized: "forms.viaForm", defaultValue: "(via form)", bundle: LanguageManager.shared.bundle) }
        static var associatedForms: String { String(localized: "forms.associatedForms", defaultValue: "Associated Forms", bundle: LanguageManager.shared.bundle) }
        static var noFormsAttached: String { String(localized: "forms.noFormsAttached", defaultValue: "No forms attached", bundle: LanguageManager.shared.bundle) }
        static var allForms: String { String(localized: "forms.allForms", defaultValue: "All Forms", bundle: LanguageManager.shared.bundle) }
        static var submittedForms: String { String(localized: "forms.submittedForms", defaultValue: "Submitted Forms", bundle: LanguageManager.shared.bundle) }
        static var pendingForms: String { String(localized: "forms.pendingForms", defaultValue: "Pending Forms", bundle: LanguageManager.shared.bundle) }
        static var globalForm: String { String(localized: "forms.globalForm", defaultValue: "Global Form", bundle: LanguageManager.shared.bundle) }
        static var inTask: String { String(localized: "forms.inTask", defaultValue: "In Task", bundle: LanguageManager.shared.bundle) }
        static var taskAssets: String { String(localized: "forms.taskAssets", defaultValue: "Task Assets", bundle: LanguageManager.shared.bundle) }
        static var linkAssets: String { String(localized: "forms.linkAssets", defaultValue: "Link Assets", bundle: LanguageManager.shared.bundle) }
        static var linkForms: String { String(localized: "forms.linkForms", defaultValue: "Link Forms", bundle: LanguageManager.shared.bundle) }
        static var linkNodes: String { String(localized: "forms.linkNodes", defaultValue: "Link Nodes", bundle: LanguageManager.shared.bundle) }

        // Form source picker sheet (Legacy vs EG Forms)
        static var linkAForm: String { String(localized: "forms.linkAForm", defaultValue: "Link a Form", bundle: LanguageManager.shared.bundle) }
        static var linkAFormSubtitle: String { String(localized: "forms.linkAFormSubtitle", defaultValue: "Choose which form system to pick from.", bundle: LanguageManager.shared.bundle) }
        static var legacyForms: String { String(localized: "forms.legacyForms", defaultValue: "Legacy Forms", bundle: LanguageManager.shared.bundle) }
        static var legacyFormsSubtitle: String { String(localized: "forms.legacyFormsSubtitle", defaultValue: "Existing form library on this site.", bundle: LanguageManager.shared.bundle) }
        static var egForms: String { String(localized: "forms.egForms", defaultValue: "EG Forms", bundle: LanguageManager.shared.bundle) }
        static var egFormsCatalog: String { String(localized: "forms.egFormsCatalog", defaultValue: "70B, Safety, NETA", bundle: LanguageManager.shared.bundle) }
        static var egFormsNotEnabled: String { String(localized: "forms.egFormsNotEnabled", defaultValue: "Not enabled for your company. Contact your admin.", bundle: LanguageManager.shared.bundle) }

        // EG Form picker sheet
        static var linkEGForm: String { String(localized: "forms.linkEGForm", defaultValue: "Link EG Form", bundle: LanguageManager.shared.bundle) }
        static var pickFormForTask: String { String(localized: "forms.pickFormForTask", defaultValue: "Pick a form to add to this task.", bundle: LanguageManager.shared.bundle) }

        // EG Form cancel-confirmation dialog
        static var saveChangesBeforeClosing: String { String(localized: "forms.saveChangesBeforeClosing", defaultValue: "Save changes before closing?", bundle: LanguageManager.shared.bundle) }
        static var saveAndClose: String { String(localized: "forms.saveAndClose", defaultValue: "Save & Close", bundle: LanguageManager.shared.bundle) }
        static var discardChanges: String { String(localized: "forms.discardChanges", defaultValue: "Discard Changes", bundle: LanguageManager.shared.bundle) }
        static var allAvailableAssets: String { String(localized: "forms.allAvailableAssets", defaultValue: "All Available Assets", bundle: LanguageManager.shared.bundle) }
        static var theseNodesWillBeLinked: String { String(localized: "forms.theseNodesWillBeLinked", defaultValue: "These nodes will be linked to this specific form instance", bundle: LanguageManager.shared.bundle) }
        static var noAdditionalNodesAvailable: String { String(localized: "forms.noAdditionalNodesAvailable", defaultValue: "No additional nodes available", bundle: LanguageManager.shared.bundle) }
        static func noNodesFoundMatching(_ query: String) -> String {
            String(localized: "forms.noNodesFoundMatching", defaultValue: "No nodes found matching \"\(query)\"", bundle: LanguageManager.shared.bundle)
        }
        static func fieldIsRequired(_ fieldName: String) -> String {
            String(localized: "forms.fieldIsRequired", defaultValue: "\(fieldName) is required", bundle: LanguageManager.shared.bundle)
        }

        // Core attributes
        static var requiredFieldsOnly: String { String(localized: "forms.requiredFieldsOnly", defaultValue: "Required fields only", bundle: LanguageManager.shared.bundle) }
        static var noRequiredFields: String { String(localized: "forms.noRequiredFields", defaultValue: "No required fields", bundle: LanguageManager.shared.bundle) }
        static func noEntityAttributes(_ entityType: String) -> String {
            String(localized: "forms.noEntityAttributes", defaultValue: "No \(entityType)", bundle: LanguageManager.shared.bundle)
        }

        // Attribute row inputs
        static func enterField(_ name: String) -> String {
            String(localized: "forms.enterField", defaultValue: "Enter \(name)", bundle: LanguageManager.shared.bundle)
        }
        static var enterNumber: String { String(localized: "forms.enterNumber", defaultValue: "Enter number", bundle: LanguageManager.shared.bundle) }
        static var enterDecimal: String { String(localized: "forms.enterDecimal", defaultValue: "Enter decimal", bundle: LanguageManager.shared.bundle) }
        static var enterCustomValue: String { String(localized: "forms.enterCustomValue", defaultValue: "Enter custom value...", bundle: LanguageManager.shared.bundle) }
        static var custom: String { String(localized: "forms.custom", defaultValue: "Custom", bundle: LanguageManager.shared.bundle) }
        static var selectPlaceholder: String { String(localized: "forms.selectPlaceholder", defaultValue: "Select...", bundle: LanguageManager.shared.bundle) }

        // Entity linking
        static var linkedBadge: String { String(localized: "forms.linkedBadge", defaultValue: "LINKED", bundle: LanguageManager.shared.bundle) }
        static var selectAll: String { String(localized: "forms.selectAll", defaultValue: "Select All", bundle: LanguageManager.shared.bundle) }
        static var clearAllSelection: String { String(localized: "forms.clearAllSelection", defaultValue: "Clear All", bundle: LanguageManager.shared.bundle) }
        static func selectedCount(_ count: Int) -> String {
            String(localized: "forms.selectedCount", defaultValue: "\(count) Selected", bundle: LanguageManager.shared.bundle)
        }
        static func searchEntities(_ entityName: String) -> String {
            String(localized: "forms.searchEntities", defaultValue: "Search \(entityName)", bundle: LanguageManager.shared.bundle)
        }
        static func noEntitiesFound(_ entityName: String) -> String {
            String(localized: "forms.noEntitiesFound", defaultValue: "No \(entityName) Found", bundle: LanguageManager.shared.bundle)
        }

        // Entity list
        static var clearFilters: String { String(localized: "forms.clearFilters", defaultValue: "Clear Filters", bundle: LanguageManager.shared.bundle) }

        // Nameplate extraction
        static var extractNameplateData: String { String(localized: "forms.extractNameplateData", defaultValue: "Extract Nameplate Data", bundle: LanguageManager.shared.bundle) }
        static var keepExistingValues: String { String(localized: "forms.keepExistingValues", defaultValue: "Keep existing values", bundle: LanguageManager.shared.bundle) }
        static var overwriteExistingValues: String { String(localized: "forms.overwriteExistingValues", defaultValue: "Overwrite existing values", bundle: LanguageManager.shared.bundle) }
        static var extractionComplete: String { String(localized: "forms.extractionComplete", defaultValue: "Extraction Complete", bundle: LanguageManager.shared.bundle) }
        static var extractionFailed: String { String(localized: "forms.extractionFailed", defaultValue: "Extraction Failed", bundle: LanguageManager.shared.bundle) }
        static var extractionNotSuccessful: String { String(localized: "forms.extractionNotSuccessful", defaultValue: "Extraction was not successful.", bundle: LanguageManager.shared.bundle) }
        static var noPhotosForExtraction: String { String(localized: "forms.noPhotosForExtraction", defaultValue: "No photos found for extraction", bundle: LanguageManager.shared.bundle) }
        static var extractionCompletedSuccessfully: String { String(localized: "forms.extractionCompletedSuccessfully", defaultValue: "Extraction completed successfully.", bundle: LanguageManager.shared.bundle) }
        static var extractWillPopulateAttributes: String { String(localized: "forms.extractWillPopulateAttributes", defaultValue: "This will extract nameplate data from photos and populate core attributes.", bundle: LanguageManager.shared.bundle) }
        static func attributesUpdated(_ count: Int) -> String {
            String(localized: "forms.attributesUpdated", defaultValue: "\(count) attribute(s) updated", bundle: LanguageManager.shared.bundle)
        }
        static func skippedCount(_ count: Int) -> String {
            String(localized: "forms.skippedCount", defaultValue: "\(count) skipped", bundle: LanguageManager.shared.bundle)
        }

        // Active session badge
        static var wo: String { String(localized: "forms.wo", defaultValue: "WO", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Issues
    enum Issues {
        static var issueDetails: String { String(localized: "issues.issueDetails", defaultValue: "Issue Details", bundle: LanguageManager.shared.bundle) }
        static var newIssue: String { String(localized: "issues.newIssue", defaultValue: "New Issue", bundle: LanguageManager.shared.bundle) }
        static var deleteIssue: String { String(localized: "issues.deleteIssue", defaultValue: "Delete Issue", bundle: LanguageManager.shared.bundle) }
        static var deleteIssueConfirm: String { String(localized: "issues.deleteIssueConfirm", defaultValue: "Are you sure you want to delete this issue? This action cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static var enterIssueTitle: String { String(localized: "issues.enterIssueTitle", defaultValue: "Enter issue title", bundle: LanguageManager.shared.bundle) }
        static var priority: String { String(localized: "issues.priority", defaultValue: "Priority", bundle: LanguageManager.shared.bundle) }
        static var selectPriority: String { String(localized: "issues.selectPriority", defaultValue: "Select Priority", bundle: LanguageManager.shared.bundle) }
        static var subtype: String { String(localized: "issues.subtype", defaultValue: "Subtype", bundle: LanguageManager.shared.bundle) }
        static var selectSubtype: String { String(localized: "issues.selectSubtype", defaultValue: "Select Subtype", bundle: LanguageManager.shared.bundle) }
        static var issueClass: String { String(localized: "issues.issueClass", defaultValue: "Issue Class", bundle: LanguageManager.shared.bundle) }
        static var selectIssueClass: String { String(localized: "issues.selectIssueClass", defaultValue: "Select issue class", bundle: LanguageManager.shared.bundle) }
        static var describeIssue: String { String(localized: "issues.describeIssue", defaultValue: "Describe the issue...", bundle: LanguageManager.shared.bundle) }
        static var proposedResolution: String { String(localized: "issues.proposedResolution", defaultValue: "Proposed Resolution", bundle: LanguageManager.shared.bundle) }
        static var suggestResolution: String { String(localized: "issues.suggestResolution", defaultValue: "Suggest a resolution...", bundle: LanguageManager.shared.bundle) }
        static var issuePhotos: String { String(localized: "issues.issuePhotos", defaultValue: "Issue Photos", bundle: LanguageManager.shared.bundle) }
        static var resolved: String { String(localized: "issues.resolved", defaultValue: "Resolved", bundle: LanguageManager.shared.bundle) }

        // Issue types
        static var typeSafety: String { String(localized: "issues.typeSafety", defaultValue: "Safety", bundle: LanguageManager.shared.bundle) }
        static var typeMaintenance: String { String(localized: "issues.typeMaintenance", defaultValue: "Maintenance", bundle: LanguageManager.shared.bundle) }
        static var typeElectrical: String { String(localized: "issues.typeElectrical", defaultValue: "Electrical", bundle: LanguageManager.shared.bundle) }
        static var typeStructural: String { String(localized: "issues.typeStructural", defaultValue: "Structural", bundle: LanguageManager.shared.bundle) }
        static var typeEnvironmental: String { String(localized: "issues.typeEnvironmental", defaultValue: "Environmental", bundle: LanguageManager.shared.bundle) }
        static var typeOperational: String { String(localized: "issues.typeOperational", defaultValue: "Operational", bundle: LanguageManager.shared.bundle) }
        static var typeOther: String { String(localized: "issues.typeOther", defaultValue: "Other", bundle: LanguageManager.shared.bundle) }

        // Priority
        static var priorityCritical: String { String(localized: "issues.priorityCritical", defaultValue: "Critical", bundle: LanguageManager.shared.bundle) }
        static var priorityHigh: String { String(localized: "issues.priorityHigh", defaultValue: "High", bundle: LanguageManager.shared.bundle) }
        static var priorityMedium: String { String(localized: "issues.priorityMedium", defaultValue: "Medium", bundle: LanguageManager.shared.bundle) }
        static var priorityLow: String { String(localized: "issues.priorityLow", defaultValue: "Low", bundle: LanguageManager.shared.bundle) }

        // Status
        static var status: String { String(localized: "issues.status", defaultValue: "Status", bundle: LanguageManager.shared.bundle) }
        static var selectStatus: String { String(localized: "issues.selectStatus", defaultValue: "Select Status", bundle: LanguageManager.shared.bundle) }
        static var statusOpen: String { String(localized: "issues.statusOpen", defaultValue: "Open", bundle: LanguageManager.shared.bundle) }
        static var statusInProgress: String { String(localized: "issues.statusInProgress", defaultValue: "In Progress", bundle: LanguageManager.shared.bundle) }
        static var statusPending: String { String(localized: "issues.statusPending", defaultValue: "Pending", bundle: LanguageManager.shared.bundle) }
        static var statusResolved: String { String(localized: "issues.statusResolved", defaultValue: "Resolved", bundle: LanguageManager.shared.bundle) }
        static var statusClosed: String { String(localized: "issues.statusClosed", defaultValue: "Closed", bundle: LanguageManager.shared.bundle) }
        static var withPhotos: String { String(localized: "issues.withPhotos", defaultValue: "With Photos", bundle: LanguageManager.shared.bundle) }
        static var mySession: String { String(localized: "issues.mySession", defaultValue: "My Session", bundle: LanguageManager.shared.bundle) }
        static var safetyAndNotification: String { String(localized: "issues.safetyAndNotification", defaultValue: "Safety & Notification", bundle: LanguageManager.shared.bundle) }
        static var immediateHazard: String { String(localized: "issues.immediateHazard", defaultValue: "Immediate Hazard", bundle: LanguageManager.shared.bundle) }
        static var customerNotified: String { String(localized: "issues.customerNotified", defaultValue: "Customer Notified", bundle: LanguageManager.shared.bundle) }
        static var noStatusChanges: String { String(localized: "issues.noStatusChanges", defaultValue: "No status changes recorded", bundle: LanguageManager.shared.bundle) }
        static var createdAs: String { String(localized: "issues.createdAs", defaultValue: "Created as", bundle: LanguageManager.shared.bundle) }
        static var statusHistoryTitle: String { String(localized: "issues.statusHistory", defaultValue: "Status History", bundle: LanguageManager.shared.bundle) }
        static var noIssues: String { String(localized: "issues.noIssues", defaultValue: "No issues", bundle: LanguageManager.shared.bundle) }
        static var showAllIssues: String { String(localized: "issues.showAllIssues", defaultValue: "Show All Issues", bundle: LanguageManager.shared.bundle) }
        static var showUnresolvedOnly: String { String(localized: "issues.showUnresolvedOnly", defaultValue: "Show Unresolved Only", bundle: LanguageManager.shared.bundle) }
        static var showResolvedOnly: String { String(localized: "issues.showResolvedOnly", defaultValue: "Show Resolved Only", bundle: LanguageManager.shared.bundle) }
        static var createNewIssueMessage: String { String(localized: "issues.createNewIssueMessage", defaultValue: "Create a new issue to track problems or concerns", bundle: LanguageManager.shared.bundle) }

        // Safety subtypes
        static var fallHazard: String { String(localized: "issues.fallHazard", defaultValue: "Fall Hazard", bundle: LanguageManager.shared.bundle) }
        static var fireHazard: String { String(localized: "issues.fireHazard", defaultValue: "Fire Hazard", bundle: LanguageManager.shared.bundle) }
        static var electricalHazard: String { String(localized: "issues.electricalHazard", defaultValue: "Electrical Hazard", bundle: LanguageManager.shared.bundle) }
        static var chemicalHazard: String { String(localized: "issues.chemicalHazard", defaultValue: "Chemical Hazard", bundle: LanguageManager.shared.bundle) }

        // Maintenance subtypes
        static var routine: String { String(localized: "issues.routine", defaultValue: "Routine", bundle: LanguageManager.shared.bundle) }
        static var preventive: String { String(localized: "issues.preventive", defaultValue: "Preventive", bundle: LanguageManager.shared.bundle) }
        static var corrective: String { String(localized: "issues.corrective", defaultValue: "Corrective", bundle: LanguageManager.shared.bundle) }
        static var emergency: String { String(localized: "issues.emergency", defaultValue: "Emergency", bundle: LanguageManager.shared.bundle) }
        static var deferred: String { String(localized: "issues.deferred", defaultValue: "Deferred", bundle: LanguageManager.shared.bundle) }

        // Electrical subtypes
        static var wiring: String { String(localized: "issues.wiring", defaultValue: "Wiring", bundle: LanguageManager.shared.bundle) }
        static var circuit: String { String(localized: "issues.circuit", defaultValue: "Circuit", bundle: LanguageManager.shared.bundle) }
        static var equipment: String { String(localized: "issues.equipment", defaultValue: "Equipment", bundle: LanguageManager.shared.bundle) }
        static var lighting: String { String(localized: "issues.lighting", defaultValue: "Lighting", bundle: LanguageManager.shared.bundle) }
        static var powerSupply: String { String(localized: "issues.powerSupply", defaultValue: "Power Supply", bundle: LanguageManager.shared.bundle) }

        // Structural subtypes
        static var damage: String { String(localized: "issues.damage", defaultValue: "Damage", bundle: LanguageManager.shared.bundle) }
        static var deterioration: String { String(localized: "issues.deterioration", defaultValue: "Deterioration", bundle: LanguageManager.shared.bundle) }
        static var defect: String { String(localized: "issues.defect", defaultValue: "Defect", bundle: LanguageManager.shared.bundle) }
        static var modificationRequired: String { String(localized: "issues.modificationRequired", defaultValue: "Modification Required", bundle: LanguageManager.shared.bundle) }

        // Environmental subtypes
        static var contamination: String { String(localized: "issues.contamination", defaultValue: "Contamination", bundle: LanguageManager.shared.bundle) }
        static var waste: String { String(localized: "issues.waste", defaultValue: "Waste", bundle: LanguageManager.shared.bundle) }
        static var emissions: String { String(localized: "issues.emissions", defaultValue: "Emissions", bundle: LanguageManager.shared.bundle) }
        static var noise: String { String(localized: "issues.noise", defaultValue: "Noise", bundle: LanguageManager.shared.bundle) }

        // Operational subtypes
        static var performance: String { String(localized: "issues.performance", defaultValue: "Performance", bundle: LanguageManager.shared.bundle) }
        static var efficiency: String { String(localized: "issues.efficiency", defaultValue: "Efficiency", bundle: LanguageManager.shared.bundle) }
        static var reliability: String { String(localized: "issues.reliability", defaultValue: "Reliability", bundle: LanguageManager.shared.bundle) }
        static var capacity: String { String(localized: "issues.capacity", defaultValue: "Capacity", bundle: LanguageManager.shared.bundle) }

        // Default subtypes
        static var generalSubtype: String { String(localized: "issues.generalSubtype", defaultValue: "General", bundle: LanguageManager.shared.bundle) }
        static var specificSubtype: String { String(localized: "issues.specificSubtype", defaultValue: "Specific", bundle: LanguageManager.shared.bundle) }

        // Issue creation configuration
        static var issue: String { String(localized: "issues.issue", defaultValue: "Issue", bundle: LanguageManager.shared.bundle) }
        static var issues: String { String(localized: "issues.issues", defaultValue: "Issues", bundle: LanguageManager.shared.bundle) }
        static var createIssue: String { String(localized: "issues.createIssue", defaultValue: "Create Issue", bundle: LanguageManager.shared.bundle) }
        static var classification: String { String(localized: "issues.classification", defaultValue: "Classification", bundle: LanguageManager.shared.bundle) }
        static var selectIssueType: String { String(localized: "issues.selectIssueType", defaultValue: "Select the type of issue", bundle: LanguageManager.shared.bundle) }
        static var selectIssuePriorityLevel: String { String(localized: "issues.selectIssuePriorityLevel", defaultValue: "Select issue priority level", bundle: LanguageManager.shared.bundle) }
        static var selectAssetForIssue: String { String(localized: "issues.selectAssetForIssue", defaultValue: "Select the asset this issue relates to", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectIssueClass: String { String(localized: "issues.pleaseSelectIssueClass", defaultValue: "Please select an issue class", bundle: LanguageManager.shared.bundle) }
        static var pleaseSelectAsset: String { String(localized: "issues.pleaseSelectAsset", defaultValue: "Please select an asset", bundle: LanguageManager.shared.bundle) }
        static var cannotCreateWithoutSLD: String { String(localized: "issues.cannotCreateWithoutSLD", defaultValue: "Cannot create issue without SLD", bundle: LanguageManager.shared.bundle) }
        static var creatingIssueFor: String { String(localized: "issues.creatingIssueFor", defaultValue: "Creating issue for:", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Quick Count
    enum QuickCount {
        static var quickCount: String { String(localized: "quickCount.quickCount", defaultValue: "Quick Count", bundle: LanguageManager.shared.bundle) }
        static var noAssetTypesAdded: String { String(localized: "quickCount.noAssetTypesAdded", defaultValue: "No Asset Types Added", bundle: LanguageManager.shared.bundle) }
        static var tapAddAssetTypeToStart: String { String(localized: "quickCount.tapAddAssetTypeToStart", defaultValue: "Tap '+ Add Asset Type' to start counting assets", bundle: LanguageManager.shared.bundle) }
        static var addAssetType: String { String(localized: "quickCount.addAssetType", defaultValue: "Add Asset Type", bundle: LanguageManager.shared.bundle) }
        static var summary: String { String(localized: "quickCount.summary", defaultValue: "Summary", bundle: LanguageManager.shared.bundle) }
        static func summaryCount(assets: Int, photos: Int) -> String {
            String(localized: "quickCount.summaryCount", defaultValue: "\(assets) assets, \(photos) photos", bundle: LanguageManager.shared.bundle)
        }
        static func createAssets(_ count: Int) -> String {
            String(localized: "quickCount.createAssets", defaultValue: "Create \(count) Assets", bundle: LanguageManager.shared.bundle)
        }
        static var selectAssetType: String { String(localized: "quickCount.selectAssetType", defaultValue: "Select Asset Type", bundle: LanguageManager.shared.bundle) }
        static var success: String { String(localized: "quickCount.success", defaultValue: "Success", bundle: LanguageManager.shared.bundle) }
        static func successfullyCreatedAssets(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "quickCount.successfullyCreatedAssets", defaultValue: "Successfully created \(count) asset\(s)", bundle: LanguageManager.shared.bundle)
        }
        static var discardAssetsMessage: String { String(localized: "quickCount.discardAssetsMessage", defaultValue: "You have unsaved assets. Are you sure you want to discard them?", bundle: LanguageManager.shared.bundle) }
        static func creatingProgress(_ current: Int, _ total: Int) -> String {
            String(localized: "quickCount.creatingProgress", defaultValue: "Creating \(current) of \(total)...", bundle: LanguageManager.shared.bundle)
        }
        static func photosCount(_ count: Int) -> String {
            String(localized: "quickCount.photosCount", defaultValue: "\(count) photos", bundle: LanguageManager.shared.bundle)
        }
        static func ocpsCount(_ count: Int) -> String {
            String(localized: "quickCount.ocpsCount", defaultValue: "• \(count) OCPs", bundle: LanguageManager.shared.bundle)
        }
        static var photosets: String { String(localized: "quickCount.photosets", defaultValue: "Photosets", bundle: LanguageManager.shared.bundle) }
        static func addPhotosetFor(_ className: String, _ index: Int) -> String {
            String(localized: "quickCount.addPhotosetFor", defaultValue: "Add Photoset for \(className) \(index)", bundle: LanguageManager.shared.bundle)
        }
        static func allAssetsHavePhotosets(_ count: Int) -> String {
            String(localized: "quickCount.allAssetsHavePhotosets", defaultValue: "All \(count) assets have photosets", bundle: LanguageManager.shared.bundle)
        }
        static var addByPhoto: String { String(localized: "quickCount.addByPhoto", defaultValue: "Add by Photo", bundle: LanguageManager.shared.bundle) }
        static var addByCount: String { String(localized: "quickCount.addByCount", defaultValue: "Add by Count", bundle: LanguageManager.shared.bundle) }
        static var ocpdsQuestion: String { String(localized: "quickCount.ocpdsQuestion", defaultValue: "OCPDs?", bundle: LanguageManager.shared.bundle) }
        static var addOcpds: String { String(localized: "quickCount.addOcpds", defaultValue: "Add OCPDs", bundle: LanguageManager.shared.bundle) }
        static func addOcpdsFor(_ assetLabel: String) -> String {
            String(localized: "quickCount.addOcpdsFor", defaultValue: "Would you like to document Overcurrent Protection Devices (OCPDs) for \(assetLabel)?", bundle: LanguageManager.shared.bundle)
        }
        static var noSkip: String { String(localized: "quickCount.noSkip", defaultValue: "No, Skip", bundle: LanguageManager.shared.bundle) }
        static func photosForAsset(_ count: Int, _ label: String) -> String {
            String(localized: "quickCount.photosForAsset", defaultValue: "\(count) photos for \(label)", bundle: LanguageManager.shared.bundle)
        }
        static func ocpdCount(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "quickCount.ocpdCount", defaultValue: "• \(count) OCPD\(s)", bundle: LanguageManager.shared.bundle)
        }
        static func ocpPhotosCount(_ count: Int) -> String {
            String(localized: "quickCount.ocpPhotosCount", defaultValue: "• \(count) photos", bundle: LanguageManager.shared.bundle)
        }
        static func countNoPhotos(_ quantity: Int) -> String {
            String(localized: "quickCount.countNoPhotos", defaultValue: "• \(quantity)x (no photos)", bundle: LanguageManager.shared.bundle)
        }
        static func countWithPhotos(_ quantity: Int, _ photoCount: Int) -> String {
            String(localized: "quickCount.countWithPhotos", defaultValue: "• \(quantity)x • \(photoCount) photos", bundle: LanguageManager.shared.bundle)
        }
        static func takePhotosFor(_ label: String) -> String {
            String(localized: "quickCount.takePhotosFor", defaultValue: "Take photos for \(label)", bundle: LanguageManager.shared.bundle)
        }
        static var noPhotosYet: String { String(localized: "quickCount.noPhotosYet", defaultValue: "No photos yet", bundle: LanguageManager.shared.bundle) }
        static var addPhotos: String { String(localized: "quickCount.addPhotos", defaultValue: "Add Photos", bundle: LanguageManager.shared.bundle) }
        static var ocpdPhotos: String { String(localized: "quickCount.ocpdPhotos", defaultValue: "OCPD Photos", bundle: LanguageManager.shared.bundle) }
        static var classifyOcpd: String { String(localized: "quickCount.classifyOcpd", defaultValue: "Classify OCPD", bundle: LanguageManager.shared.bundle) }
        static func takePhotosOfOcpd(_ ordinal: String) -> String {
            String(localized: "quickCount.takePhotosOfOcpd", defaultValue: "Take photos of the \(ordinal) OCPD", bundle: LanguageManager.shared.bundle)
        }
        static var next: String { String(localized: "quickCount.next", defaultValue: "Next", bundle: LanguageManager.shared.bundle) }
        static var selectOcpdType: String { String(localized: "quickCount.selectOcpdType", defaultValue: "Select OCPD Type", bundle: LanguageManager.shared.bundle) }
        static var ocpdType: String { String(localized: "quickCount.ocpdType", defaultValue: "OCPD Type", bundle: LanguageManager.shared.bundle) }
        static var subtypeOptional: String { String(localized: "quickCount.subtypeOptional", defaultValue: "Subtype (Optional)", bundle: LanguageManager.shared.bundle) }
        static var selectSubtype: String { String(localized: "quickCount.selectSubtype", defaultValue: "Select Subtype", bundle: LanguageManager.shared.bundle) }
        static func chooseSubtype(_ className: String) -> String {
            String(localized: "quickCount.chooseSubtype", defaultValue: "Choose a subtype for \(className)", bundle: LanguageManager.shared.bundle)
        }
        static var skipNoSubtype: String { String(localized: "quickCount.skipNoSubtype", defaultValue: "Skip - No Subtype", bundle: LanguageManager.shared.bundle) }
        static var addOcpdsByCount: String { String(localized: "quickCount.addOcpdsByCount", defaultValue: "Add OCPDs by Count", bundle: LanguageManager.shared.bundle) }
        static var addOcpdByCountSubtitle: String { String(localized: "quickCount.addOcpdByCountSubtitle", defaultValue: "Quickly add multiple OCPDs with optional photos", bundle: LanguageManager.shared.bundle) }
        static var noOcpdsAddedYet: String { String(localized: "quickCount.noOcpdsAddedYet", defaultValue: "No OCPDs added yet", bundle: LanguageManager.shared.bundle) }
        static var addOcpdType: String { String(localized: "quickCount.addOcpdType", defaultValue: "Add OCPD Type", bundle: LanguageManager.shared.bundle) }
        static func doneOcpdCount(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "quickCount.doneOcpdCount", defaultValue: "Done (\(count) OCPD\(s))", bundle: LanguageManager.shared.bundle)
        }
        static var enterQuantity: String { String(localized: "quickCount.enterQuantity", defaultValue: "Enter Quantity", bundle: LanguageManager.shared.bundle) }
        static var quantity: String { String(localized: "quickCount.quantity", defaultValue: "Quantity", bundle: LanguageManager.shared.bundle) }
        static var noPhotosParens: String { String(localized: "quickCount.noPhotosParens", defaultValue: "(no photos)", bundle: LanguageManager.shared.bundle) }
        static func allInstancesHavePhotosets(_ count: Int) -> String {
            String(localized: "quickCount.allInstancesHavePhotosets", defaultValue: "All \(count) instances have photosets", bundle: LanguageManager.shared.bundle)
        }
        static func photosForInstance(_ count: Int, _ className: String, _ index: Int) -> String {
            String(localized: "quickCount.photosForInstance", defaultValue: "\(count) photos for \(className) \(index)", bundle: LanguageManager.shared.bundle)
        }
        static func photographInstance(_ className: String, _ index: Int) -> String {
            String(localized: "quickCount.photographInstance", defaultValue: "Photograph \(className) \(index)", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Photo Walkthrough
    enum PhotoWalkthrough {
        static var photoWalkthrough: String { String(localized: "photoWalkthrough.photoWalkthrough", defaultValue: "Photo Walkthrough", bundle: LanguageManager.shared.bundle) }
        static var classifyAsset: String { String(localized: "photoWalkthrough.classifyAsset", defaultValue: "Classify Asset", bundle: LanguageManager.shared.bundle) }
        static var addOcpds: String { String(localized: "photoWalkthrough.addOcpds", defaultValue: "Add OCPDs?", bundle: LanguageManager.shared.bundle) }
        static var photographOcpd: String { String(localized: "photoWalkthrough.photographOcpd", defaultValue: "Photograph OCPD", bundle: LanguageManager.shared.bundle) }
        static var classifyOcpd: String { String(localized: "photoWalkthrough.classifyOcpd", defaultValue: "Classify OCPD", bundle: LanguageManager.shared.bundle) }
        static var moreOcpds: String { String(localized: "photoWalkthrough.moreOcpds", defaultValue: "More OCPDs?", bundle: LanguageManager.shared.bundle) }
        static var whatsNext: String { String(localized: "photoWalkthrough.whatsNext", defaultValue: "What's Next?", bundle: LanguageManager.shared.bundle) }
        static var reviewAssets: String { String(localized: "photoWalkthrough.reviewAssets", defaultValue: "Review Assets", bundle: LanguageManager.shared.bundle) }
        static var creating: String { String(localized: "photoWalkthrough.creating", defaultValue: "Creating...", bundle: LanguageManager.shared.bundle) }
        static func createdAssetsWithPhotos(_ assetCount: Int, _ photoCount: Int) -> String {
            String(localized: "photoWalkthrough.createdAssetsWithPhotos", defaultValue: "Created \(assetCount) assets with \(photoCount) photos", bundle: LanguageManager.shared.bundle)
        }
        static var discardAssetsPhotosMessage: String { String(localized: "photoWalkthrough.discardAssetsPhotosMessage", defaultValue: "You have unsaved assets and photos. Are you sure you want to discard them?", bundle: LanguageManager.shared.bundle) }
        static func takePhotosOfOcpd(_ ordinal: String) -> String {
            String(localized: "photoWalkthrough.takePhotosOfOcpd", defaultValue: "Take photos of the \(ordinal) OCPD", bundle: LanguageManager.shared.bundle)
        }
        static var takePhotosOfAsset: String { String(localized: "photoWalkthrough.takePhotosOfAsset", defaultValue: "Take photos of the asset", bundle: LanguageManager.shared.bundle) }
        static var canTakeMultiplePhotos: String { String(localized: "photoWalkthrough.canTakeMultiplePhotos", defaultValue: "You can take multiple photos", bundle: LanguageManager.shared.bundle) }
        static var noPhotosYet: String { String(localized: "photoWalkthrough.noPhotosYet", defaultValue: "No photos yet", bundle: LanguageManager.shared.bundle) }
        static var doneWithThisOcpd: String { String(localized: "photoWalkthrough.doneWithThisOcpd", defaultValue: "Done with this OCPD", bundle: LanguageManager.shared.bundle) }
        static var doneWithThisAsset: String { String(localized: "photoWalkthrough.doneWithThisAsset", defaultValue: "Done with this asset", bundle: LanguageManager.shared.bundle) }
        static var assetType: String { String(localized: "photoWalkthrough.assetType", defaultValue: "Asset Type", bundle: LanguageManager.shared.bundle) }
        static var selectAssetType: String { String(localized: "photoWalkthrough.selectAssetType", defaultValue: "Select Asset Type", bundle: LanguageManager.shared.bundle) }
        static var subtypeOptional: String { String(localized: "photoWalkthrough.subtypeOptional", defaultValue: "Subtype (Optional)", bundle: LanguageManager.shared.bundle) }
        static var ocpdsDescription: String { String(localized: "photoWalkthrough.ocpdsDescription", defaultValue: "This asset type can contain Overcurrent Protection Devices (OCPDs). Would you like to photograph any?", bundle: LanguageManager.shared.bundle) }
        static var yesAddOcpdPhotos: String { String(localized: "photoWalkthrough.yesAddOcpdPhotos", defaultValue: "Yes, Add OCPD Photos", bundle: LanguageManager.shared.bundle) }
        static var addSubComponents: String { String(localized: "photoWalkthrough.addSubComponents", defaultValue: "Add Sub-Components?", bundle: LanguageManager.shared.bundle) }
        static var subComponentsDescription: String { String(localized: "photoWalkthrough.subComponentsDescription", defaultValue: "This asset type can contain sub-components & OCPDs. Would you like to photograph any?", bundle: LanguageManager.shared.bundle) }
        static var yesAddPhotos: String { String(localized: "photoWalkthrough.yesAddPhotos", defaultValue: "Yes, Add Photos", bundle: LanguageManager.shared.bundle) }
        static var addAnotherSubComponent: String { String(localized: "photoWalkthrough.addAnotherSubComponent", defaultValue: "Add Another Sub-Component", bundle: LanguageManager.shared.bundle) }
        static var doneWithSubComponents: String { String(localized: "photoWalkthrough.doneWithSubComponents", defaultValue: "Done with Sub-Components", bundle: LanguageManager.shared.bundle) }
        static var maintenanceState: String { String(localized: "photoWalkthrough.maintenanceState", defaultValue: "Maintenance State", bundle: LanguageManager.shared.bundle) }
        static var maintenanceCheckDescription: String { String(localized: "photoWalkthrough.maintenanceCheckDescription", defaultValue: "Check all statements that apply. If none apply, equipment is Level 1.", bundle: LanguageManager.shared.bundle) }
        static var derivedLevel: String { String(localized: "photoWalkthrough.derivedLevel", defaultValue: "Derived level:", bundle: LanguageManager.shared.bundle) }
        static var takePhotosOfSubComponent: String { String(localized: "photoWalkthrough.takePhotosOfSubComponent", defaultValue: "Take photos of sub-component", bundle: LanguageManager.shared.bundle) }
        static var multiplePhotosPerSub: String { String(localized: "photoWalkthrough.multiplePhotosPerSub", defaultValue: "You can take multiple photos per sub-component", bundle: LanguageManager.shared.bundle) }
        static var doneWithThisSubComponent: String { String(localized: "photoWalkthrough.doneWithThisSubComponent", defaultValue: "Done with this sub-component", bundle: LanguageManager.shared.bundle) }
        static var saveAndContinue: String { String(localized: "photoWalkthrough.saveAndContinue", defaultValue: "Save & Continue", bundle: LanguageManager.shared.bundle) }
        static var creatingSubComponents: String { String(localized: "photoWalkthrough.creatingSubComponents", defaultValue: "Creating sub-components...", bundle: LanguageManager.shared.bundle) }
        static var reviewAndCreate: String { String(localized: "photoWalkthrough.reviewAndCreate", defaultValue: "Review & Create", bundle: LanguageManager.shared.bundle) }
        static var createMultiple: String { String(localized: "photoWalkthrough.createMultiple", defaultValue: "Create Multiple", bundle: LanguageManager.shared.bundle) }
        static var noSkip: String { String(localized: "photoWalkthrough.noSkip", defaultValue: "No, Skip", bundle: LanguageManager.shared.bundle) }
        static func ocpdCountAdded(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "photoWalkthrough.ocpdCountAdded", defaultValue: "\(count) OCPD\(s) added", bundle: LanguageManager.shared.bundle)
        }
        static var addAnotherOcpd: String { String(localized: "photoWalkthrough.addAnotherOcpd", defaultValue: "Add Another OCPD", bundle: LanguageManager.shared.bundle) }
        static var doneWithOcp: String { String(localized: "photoWalkthrough.doneWithOcp", defaultValue: "Done with OCP", bundle: LanguageManager.shared.bundle) }
        static var assetCaptured: String { String(localized: "photoWalkthrough.assetCaptured", defaultValue: "Asset Captured", bundle: LanguageManager.shared.bundle) }
        static var assetsCaptured: String { String(localized: "photoWalkthrough.assetsCaptured", defaultValue: "Assets Captured", bundle: LanguageManager.shared.bundle) }
        static func photosTotalCount(_ count: Int) -> String {
            String(localized: "photoWalkthrough.photosTotalCount", defaultValue: "\(count) photos total", bundle: LanguageManager.shared.bundle)
        }
        static var addAnotherAsset: String { String(localized: "photoWalkthrough.addAnotherAsset", defaultValue: "Add Another Asset", bundle: LanguageManager.shared.bundle) }
        static var finishWalkthrough: String { String(localized: "photoWalkthrough.finishWalkthrough", defaultValue: "Finish Walkthrough", bundle: LanguageManager.shared.bundle) }
        static func assetsCount(_ count: Int) -> String {
            String(localized: "photoWalkthrough.assetsCount", defaultValue: "\(count) assets", bundle: LanguageManager.shared.bundle)
        }
        static func photosCount(_ count: Int) -> String {
            String(localized: "photoWalkthrough.photosCount", defaultValue: "\(count) photos", bundle: LanguageManager.shared.bundle)
        }
        static var addMore: String { String(localized: "photoWalkthrough.addMore", defaultValue: "Add More", bundle: LanguageManager.shared.bundle) }
        static func createAll(_ count: Int) -> String {
            String(localized: "photoWalkthrough.createAll", defaultValue: "Create All (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static var creatingAssets: String { String(localized: "photoWalkthrough.creatingAssets", defaultValue: "Creating assets...", bundle: LanguageManager.shared.bundle) }
        static func progressOfTotal(_ current: Int, _ total: Int) -> String {
            String(localized: "photoWalkthrough.progressOfTotal", defaultValue: "\(current) of \(total)", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Quick Inventory
    enum QuickInventory {
        static var inventoryMode: String { String(localized: "quickInventory.inventoryMode", defaultValue: "Inventory Mode", bundle: LanguageManager.shared.bundle) }
        static var quickInventory: String { String(localized: "quickInventory.quickInventory", defaultValue: "Quick Inventory", bundle: LanguageManager.shared.bundle) }
        static func creatingProgress(_ current: Int, _ total: Int) -> String {
            String(localized: "quickInventory.creatingProgress", defaultValue: "Creating \(current) of \(total)...", bundle: LanguageManager.shared.bundle)
        }
        static var creationFailed: String { String(localized: "quickInventory.creationFailed", defaultValue: "Creation Failed", bundle: LanguageManager.shared.bundle) }
        static var success: String { String(localized: "quickInventory.success", defaultValue: "Success", bundle: LanguageManager.shared.bundle) }
        static func successfullyCreatedAssets(_ count: Int) -> String {
            let s = count == 1 ? "" : "s"
            return String(localized: "quickInventory.successfullyCreatedAssets", defaultValue: "Successfully created \(count) asset\(s)", bundle: LanguageManager.shared.bundle)
        }
        static var assetLimitExceeded: String { String(localized: "quickInventory.assetLimitExceeded", defaultValue: "Asset Limit Exceeded", bundle: LanguageManager.shared.bundle) }
        static func assetLimitExceededMessage(_ max: Int, _ selected: Int) -> String {
            String(localized: "quickInventory.assetLimitExceededMessage", defaultValue: "You cannot create more than \(max) assets at once. You have selected \(selected) assets. Please reduce your selection.", bundle: LanguageManager.shared.bundle)
        }
        static var selectQuantityDescription: String { String(localized: "quickInventory.selectQuantityDescription", defaultValue: "Select the quantity of each asset type to create", bundle: LanguageManager.shared.bundle) }
        static var assetTypes: String { String(localized: "quickInventory.assetTypes", defaultValue: "Asset Types", bundle: LanguageManager.shared.bundle) }
        static var createByLocation: String { String(localized: "quickInventory.createByLocation", defaultValue: "Create assets organized by location", bundle: LanguageManager.shared.bundle) }
        static var addLocation: String { String(localized: "quickInventory.addLocation", defaultValue: "Add Location", bundle: LanguageManager.shared.bundle) }
        static var noLocationsAdded: String { String(localized: "quickInventory.noLocationsAdded", defaultValue: "No locations added", bundle: LanguageManager.shared.bundle) }
        static var addFirstLocation: String { String(localized: "quickInventory.addFirstLocation", defaultValue: "Add First Location", bundle: LanguageManager.shared.bundle) }
        static func itemsCount(_ count: Int) -> String {
            String(localized: "quickInventory.itemsCount", defaultValue: "(\(count) items)", bundle: LanguageManager.shared.bundle)
        }
        static var editName: String { String(localized: "quickInventory.editName", defaultValue: "Edit Name", bundle: LanguageManager.shared.bundle) }
        static var duplicate: String { String(localized: "quickInventory.duplicate", defaultValue: "Duplicate", bundle: LanguageManager.shared.bundle) }
        static var locationName: String { String(localized: "quickInventory.locationName", defaultValue: "Location Name", bundle: LanguageManager.shared.bundle) }
        static var locationNamePlaceholder: String { String(localized: "quickInventory.locationNamePlaceholder", defaultValue: "e.g., Building A, Floor 2, Room 101", bundle: LanguageManager.shared.bundle) }
        static var orSelectFromExisting: String { String(localized: "quickInventory.orSelectFromExisting", defaultValue: "Or Select From Existing", bundle: LanguageManager.shared.bundle) }
        static var editLocation: String { String(localized: "quickInventory.editLocation", defaultValue: "Edit Location", bundle: LanguageManager.shared.bundle) }
        static var summary: String { String(localized: "quickInventory.summary", defaultValue: "Summary", bundle: LanguageManager.shared.bundle) }
        static func totalAssetsToCreate(_ count: Int) -> String {
            String(localized: "quickInventory.totalAssetsToCreate", defaultValue: "Total assets to create: \(count)", bundle: LanguageManager.shared.bundle)
        }
        static func maximumAssetsAllowed(_ max: Int) -> String {
            String(localized: "quickInventory.maximumAssetsAllowed", defaultValue: "Maximum \(max) assets allowed", bundle: LanguageManager.shared.bundle)
        }
        static var assetsQueuedForSync: String { String(localized: "quickInventory.assetsQueuedForSync", defaultValue: "Assets will be queued for sync when online", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Arc Flash
    enum ArcFlash {
        static var assetDetails: String { String(localized: "arcFlash.assetDetails", defaultValue: "Asset Details", bundle: LanguageManager.shared.bundle) }
        static var sourceTarget: String { String(localized: "arcFlash.sourceTarget", defaultValue: "Source/Target", bundle: LanguageManager.shared.bundle) }
        static var connectionDetails: String { String(localized: "arcFlash.connectionDetails", defaultValue: "Connection Details", bundle: LanguageManager.shared.bundle) }
        static var readinessScore: String { String(localized: "arcFlash.readinessScore", defaultValue: "Readiness Score", bundle: LanguageManager.shared.bundle) }
        static var overall: String { String(localized: "arcFlash.overall", defaultValue: "Overall", bundle: LanguageManager.shared.bundle) }
        static func completedCount(_ count: Int) -> String {
            String(localized: "arcFlash.completedCount", defaultValue: "\(count) Completed", bundle: LanguageManager.shared.bundle)
        }
        static func remainingCount(_ count: Int) -> String {
            String(localized: "arcFlash.remainingCount", defaultValue: "\(count) Remaining", bundle: LanguageManager.shared.bundle)
        }
        static func totalItems(_ count: Int) -> String {
            String(localized: "arcFlash.totalItems", defaultValue: "\(count) Total Items", bundle: LanguageManager.shared.bundle)
        }
        static func breakdown(_ title: String) -> String {
            String(localized: "arcFlash.breakdown", defaultValue: "\(title) Breakdown", bundle: LanguageManager.shared.bundle)
        }
        static func assetLabel(_ count: Int) -> String {
            count == 1
                ? String(localized: "arcFlash.assetSingular", defaultValue: "asset", bundle: LanguageManager.shared.bundle)
                : String(localized: "arcFlash.assetPlural", defaultValue: "assets", bundle: LanguageManager.shared.bundle)
        }
        static func connectionLabel(_ count: Int) -> String {
            count == 1
                ? String(localized: "arcFlash.connectionSingular", defaultValue: "connection", bundle: LanguageManager.shared.bundle)
                : String(localized: "arcFlash.connectionPlural", defaultValue: "connections", bundle: LanguageManager.shared.bundle)
        }
        static var connection: String { String(localized: "arcFlash.connection", defaultValue: "Connection", bundle: LanguageManager.shared.bundle) }
        static var statusConnected: String { String(localized: "arcFlash.statusConnected", defaultValue: "Connected", bundle: LanguageManager.shared.bundle) }
        static var statusMissingSource: String { String(localized: "arcFlash.statusMissingSource", defaultValue: "Missing Source", bundle: LanguageManager.shared.bundle) }
        static var completeLabel: String { String(localized: "arcFlash.completeLabel", defaultValue: "Complete", bundle: LanguageManager.shared.bundle) }
        static var doneLabel: String { String(localized: "arcFlash.doneLabel", defaultValue: "Done", bundle: LanguageManager.shared.bundle) }
        static var totalLabel: String { String(localized: "arcFlash.totalLabel", defaultValue: "Total", bundle: LanguageManager.shared.bundle) }
        static var loadingArcFlashAnalysis: String { String(localized: "arcFlash.loadingArcFlashAnalysis", defaultValue: "Loading Arc Flash Analysis...", bundle: LanguageManager.shared.bundle) }
        static var arcFlashAnalysis: String { String(localized: "arcFlash.arcFlashAnalysis", defaultValue: "Arc Flash Analysis", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Quotes
    enum Quotes {
        static var searchQuotes: String { String(localized: "quotes.searchQuotes", defaultValue: "Search quotes...", bundle: LanguageManager.shared.bundle) }
        static var totalQuotes: String { String(localized: "quotes.totalQuotes", defaultValue: "Total Quotes", bundle: LanguageManager.shared.bundle) }
        static var approved: String { String(localized: "quotes.approved", defaultValue: "Approved", bundle: LanguageManager.shared.bundle) }
        static var noQuotes: String { String(localized: "quotes.noQuotes", defaultValue: "No Quotes", bundle: LanguageManager.shared.bundle) }
        static var noResults: String { String(localized: "quotes.noResults", defaultValue: "No Results", bundle: LanguageManager.shared.bundle) }
        static var quotesWillAppear: String { String(localized: "quotes.quotesWillAppear", defaultValue: "Quotes will appear here when created", bundle: LanguageManager.shared.bundle) }
        static var tryAdjustingSearch: String { String(localized: "quotes.tryAdjustingSearch", defaultValue: "Try adjusting your search or filters", bundle: LanguageManager.shared.bundle) }
        static var quotes: String { String(localized: "quotes.quotes", defaultValue: "Quotes", bundle: LanguageManager.shared.bundle) }
        static var untitledQuote: String { String(localized: "quotes.untitledQuote", defaultValue: "Untitled Quote", bundle: LanguageManager.shared.bundle) }
        static var sow: String { String(localized: "quotes.sow", defaultValue: "SOW", bundle: LanguageManager.shared.bundle) }
        static var tm: String { String(localized: "quotes.tm", defaultValue: "T&M", bundle: LanguageManager.shared.bundle) }
        static var draft: String { String(localized: "quotes.draft", defaultValue: "Draft", bundle: LanguageManager.shared.bundle) }
        static var rejected: String { String(localized: "quotes.rejected", defaultValue: "Rejected", bundle: LanguageManager.shared.bundle) }
        static var quoteDetailsPlaceholder: String { String(localized: "quotes.quoteDetailsPlaceholder", defaultValue: "Quote details would appear here...", bundle: LanguageManager.shared.bundle) }
        static var quoteDetails: String { String(localized: "quotes.quoteDetails", defaultValue: "Quote Details", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Sync / Force Update
    enum Sync {
        static var syncingYourData: String { String(localized: "sync.syncingYourData", defaultValue: "Syncing Your Data", bundle: LanguageManager.shared.bundle) }
        static func itemsProgress(_ current: Int, _ total: Int) -> String {
            String(localized: "sync.itemsProgress", defaultValue: "\(current) of \(total) items", bundle: LanguageManager.shared.bundle)
        }
        static var pleaseWaitWhileWeSave: String { String(localized: "sync.pleaseWaitWhileWeSave", defaultValue: "Please wait while we save your data.", bundle: LanguageManager.shared.bundle) }
        static var syncIncomplete: String { String(localized: "sync.syncIncomplete", defaultValue: "Sync Incomplete", bundle: LanguageManager.shared.bundle) }
        static var canSyncLaterFromSettings: String { String(localized: "sync.canSyncLaterFromSettings", defaultValue: "You can sync later from Settings.", bundle: LanguageManager.shared.bundle) }
        static var retrySync: String { String(localized: "sync.retrySync", defaultValue: "Retry Sync", bundle: LanguageManager.shared.bundle) }
        static var updateAnyway: String { String(localized: "sync.updateAnyway", defaultValue: "Update Anyway", bundle: LanguageManager.shared.bundle) }
        static var offlineSyncQueue: String { String(localized: "sync.offlineSyncQueue", defaultValue: "OFFLINE SYNC QUEUE", bundle: LanguageManager.shared.bundle) }
        static var syncInProgressLabel: String { String(localized: "sync.syncInProgress", defaultValue: "SYNC IN PROGRESS", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Sync Steps
    enum SyncStep {
        static var fetching: String { String(localized: "syncStep.fetching", defaultValue: "Fetching data from server", bundle: LanguageManager.shared.bundle) }
        static var nodeClasses: String { String(localized: "syncStep.nodeClasses", defaultValue: "Processing equipment classes", bundle: LanguageManager.shared.bundle) }
        static var edgeClasses: String { String(localized: "syncStep.edgeClasses", defaultValue: "Processing connection classes", bundle: LanguageManager.shared.bundle) }
        static var issueClasses: String { String(localized: "syncStep.issueClasses", defaultValue: "Processing issue classes", bundle: LanguageManager.shared.bundle) }
        static var locations: String { String(localized: "syncStep.locations", defaultValue: "Processing locations", bundle: LanguageManager.shared.bundle) }
        static var nodes: String { String(localized: "syncStep.nodes", defaultValue: "Processing equipment", bundle: LanguageManager.shared.bundle) }
        static var edges: String { String(localized: "syncStep.edges", defaultValue: "Processing connections", bundle: LanguageManager.shared.bundle) }
        static var sessions: String { String(localized: "syncStep.sessions", defaultValue: "Processing sessions", bundle: LanguageManager.shared.bundle) }
        static var tasks: String { String(localized: "syncStep.tasks", defaultValue: "Processing tasks", bundle: LanguageManager.shared.bundle) }
        static var formInstances: String { String(localized: "syncStep.formInstances", defaultValue: "Processing form instances", bundle: LanguageManager.shared.bundle) }
        static var quotes: String { String(localized: "syncStep.quotes", defaultValue: "Processing quotes", bundle: LanguageManager.shared.bundle) }
        static var issues: String { String(localized: "syncStep.issues", defaultValue: "Processing issues", bundle: LanguageManager.shared.bundle) }
        static var mappings: String { String(localized: "syncStep.mappings", defaultValue: "Processing relationships", bundle: LanguageManager.shared.bundle) }
        static var photos: String { String(localized: "syncStep.photos", defaultValue: "Processing photos", bundle: LanguageManager.shared.bundle) }
        static var irPhotos: String { String(localized: "syncStep.irPhotos", defaultValue: "Processing IR photos", bundle: LanguageManager.shared.bundle) }
        static var comments: String { String(localized: "syncStep.comments", defaultValue: "Processing comments", bundle: LanguageManager.shared.bundle) }
        static var saving: String { String(localized: "syncStep.saving", defaultValue: "Saving data", bundle: LanguageManager.shared.bundle) }
        static var complete: String { String(localized: "syncStep.complete", defaultValue: "Complete", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Agent
    enum Agent {
        static var agent: String { String(localized: "agent.agent", defaultValue: "Agent", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Site
    enum Site {
        static func hiName(_ name: String) -> String {
            String(localized: "site.hiName", defaultValue: "Hi, \(name)!", bundle: LanguageManager.shared.bundle)
        }
        static var hi: String { String(localized: "site.hi", defaultValue: "Hi!", bundle: LanguageManager.shared.bundle) }
        static func welcomeTo(_ name: String) -> String {
            String(localized: "site.welcomeTo", defaultValue: "Welcome to \(name)", bundle: LanguageManager.shared.bundle)
        }
        static var activeWorkOrder: String { String(localized: "site.activeWorkOrder", defaultValue: "Active Work Order", bundle: LanguageManager.shared.bundle) }
        static var noActiveWorkOrder: String { String(localized: "site.noActiveWorkOrder", defaultValue: "No Active Work Order", bundle: LanguageManager.shared.bundle) }
        static var tapToSelectWorkOrder: String { String(localized: "site.tapToSelectWorkOrder", defaultValue: "Tap to select a work order", bundle: LanguageManager.shared.bundle) }
        static var quickActions: String { String(localized: "site.quickActions", defaultValue: "Quick Actions", bundle: LanguageManager.shared.bundle) }
        static var myTasks: String { String(localized: "site.myTasks", defaultValue: "My Tasks", bundle: LanguageManager.shared.bundle) }
        static var arcFlash: String { String(localized: "site.arcFlash", defaultValue: "Arc Flash", bundle: LanguageManager.shared.bundle) }
        static var refresh: String { String(localized: "site.refresh", defaultValue: "Refresh", bundle: LanguageManager.shared.bundle) }
        static var sites: String { String(localized: "site.sites", defaultValue: "Sites", bundle: LanguageManager.shared.bundle) }
        static var syncNow: String { String(localized: "site.syncNow", defaultValue: "Sync Now", bundle: LanguageManager.shared.bundle) }
        static var showFirstFiveOnly: String { String(localized: "site.showFirstFiveOnly", defaultValue: "Show First 5 Only", bundle: LanguageManager.shared.bundle) }
        static var updateAvailable: String { String(localized: "site.updateAvailable", defaultValue: "Update Available", bundle: LanguageManager.shared.bundle) }
        static var update: String { String(localized: "site.update", defaultValue: "Update", bundle: LanguageManager.shared.bundle) }
        static var remindMeLater: String { String(localized: "site.remindMeLater", defaultValue: "Remind Me Later", bundle: LanguageManager.shared.bundle) }
        static var newVersionAvailable: String { String(localized: "site.newVersionAvailable", defaultValue: "A new version is available.", bundle: LanguageManager.shared.bundle) }
        static var updateRequired: String { String(localized: "site.updateRequired", defaultValue: "Update Required", bundle: LanguageManager.shared.bundle) }
        static var pleaseUpdateToContinue: String { String(localized: "site.pleaseUpdateToContinue", defaultValue: "Please update to continue using the app.", bundle: LanguageManager.shared.bundle) }

        // Site form shared
        static var siteInformation: String { String(localized: "site.siteInformation", defaultValue: "Site Information", bundle: LanguageManager.shared.bundle) }
        static var siteName: String { String(localized: "site.siteName", defaultValue: "Site Name", bundle: LanguageManager.shared.bundle) }
        static var useCurrentLocation: String { String(localized: "site.useCurrentLocation", defaultValue: "Use Current Location", bundle: LanguageManager.shared.bundle) }
        static var setLocation: String { String(localized: "site.setLocation", defaultValue: "Set Location", bundle: LanguageManager.shared.bundle) }
        static var locationOptional: String { String(localized: "site.locationOptional", defaultValue: "Location (Optional)", bundle: LanguageManager.shared.bundle) }
        static var location: String { String(localized: "site.location", defaultValue: "Location", bundle: LanguageManager.shared.bundle) }
        static var locationHelpText: String { String(localized: "site.locationHelpText", defaultValue: "Location helps auto-fill your address. You can also enter it manually below.", bundle: LanguageManager.shared.bundle) }
        static var address: String { String(localized: "site.address", defaultValue: "Address", bundle: LanguageManager.shared.bundle) }
        static var addressLine1: String { String(localized: "site.addressLine1", defaultValue: "Address Line 1", bundle: LanguageManager.shared.bundle) }
        static var addressLine2Optional: String { String(localized: "site.addressLine2Optional", defaultValue: "Address Line 2 (Optional)", bundle: LanguageManager.shared.bundle) }
        static var city: String { String(localized: "site.city", defaultValue: "City", bundle: LanguageManager.shared.bundle) }
        static var country: String { String(localized: "site.country", defaultValue: "Country", bundle: LanguageManager.shared.bundle) }
        static var state: String { String(localized: "site.state", defaultValue: "State", bundle: LanguageManager.shared.bundle) }
        static var province: String { String(localized: "site.province", defaultValue: "Province", bundle: LanguageManager.shared.bundle) }
        static var region: String { String(localized: "site.region", defaultValue: "Region", bundle: LanguageManager.shared.bundle) }
        static var stateProvince: String { String(localized: "site.stateProvince", defaultValue: "State/Province", bundle: LanguageManager.shared.bundle) }
        static var zipCode: String { String(localized: "site.zipCode", defaultValue: "ZIP Code", bundle: LanguageManager.shared.bundle) }
        static var postalCode: String { String(localized: "site.postalCode", defaultValue: "Postal Code", bundle: LanguageManager.shared.bundle) }
        static func selectStateProvince(_ label: String) -> String {
            String(localized: "site.selectStateProvince", defaultValue: "Select \(label)", bundle: LanguageManager.shared.bundle)
        }
        static var editSite: String { String(localized: "site.editSite", defaultValue: "Edit Site", bundle: LanguageManager.shared.bundle) }
        static var createNewSite: String { String(localized: "site.createNewSite", defaultValue: "Create New Site", bundle: LanguageManager.shared.bundle) }
        static var updatingSite: String { String(localized: "site.updatingSite", defaultValue: "Updating site...", bundle: LanguageManager.shared.bundle) }
        static var creatingSite: String { String(localized: "site.creatingSite", defaultValue: "Creating site...", bundle: LanguageManager.shared.bundle) }
        static var companyIdNotFound: String { String(localized: "site.companyIdNotFound", defaultValue: "Company ID not found. Please log in again.", bundle: LanguageManager.shared.bundle) }

        // Account, Complexity, Labor Union
        static var siteDetails: String { String(localized: "site.siteDetails", defaultValue: "Site Details", bundle: LanguageManager.shared.bundle) }
        static var account: String { String(localized: "site.account", defaultValue: "Account", bundle: LanguageManager.shared.bundle) }
        static var selectAccount: String { String(localized: "site.selectAccount", defaultValue: "Select Account", bundle: LanguageManager.shared.bundle) }
        static var searchAccounts: String { String(localized: "site.searchAccounts", defaultValue: "Search accounts...", bundle: LanguageManager.shared.bundle) }
        static var complexityLevel: String { String(localized: "site.complexityLevel", defaultValue: "Access Complexity", bundle: LanguageManager.shared.bundle) }
        static var selectComplexityLevel: String { String(localized: "site.selectComplexityLevel", defaultValue: "Select level", bundle: LanguageManager.shared.bundle) }
        static var complexityStandard: String { String(localized: "site.complexityStandard", defaultValue: "Standard", bundle: LanguageManager.shared.bundle) }
        static var complexityModerate: String { String(localized: "site.complexityModerate", defaultValue: "Moderate", bundle: LanguageManager.shared.bundle) }
        static var complexityHigh: String { String(localized: "site.complexityHigh", defaultValue: "High", bundle: LanguageManager.shared.bundle) }
        static var complexityCritical: String { String(localized: "site.complexityCritical", defaultValue: "Critical", bundle: LanguageManager.shared.bundle) }
        static var complexityStandardDesc: String { String(localized: "site.complexityStandardDesc", defaultValue: "Normal access, no special requirements", bundle: LanguageManager.shared.bundle) }
        static var complexityModerateDesc: String { String(localized: "site.complexityModerateDesc", defaultValue: "Minor access restrictions or remote location", bundle: LanguageManager.shared.bundle) }
        static var complexityHighDesc: String { String(localized: "site.complexityHighDesc", defaultValue: "Security clearance, escorted access, or limited hours", bundle: LanguageManager.shared.bundle) }
        static var complexityCriticalDesc: String { String(localized: "site.complexityCriticalDesc", defaultValue: "Extreme restrictions, specialized equipment needed", bundle: LanguageManager.shared.bundle) }
        static var complexityTooltip: String { String(localized: "site.complexityTooltip", defaultValue: "Time multiplier based on site access difficulty", bundle: LanguageManager.shared.bundle) }
        static var accountInfo: String { String(localized: "site.accountInfo", defaultValue: "Associate this site with a customer account", bundle: LanguageManager.shared.bundle) }
        static var laborUnionInfo: String { String(localized: "site.laborUnionInfo", defaultValue: "Labor union assigned to this site for rate lookups", bundle: LanguageManager.shared.bundle) }
        static var laborUnion: String { String(localized: "site.laborUnion", defaultValue: "Labor Union", bundle: LanguageManager.shared.bundle) }
        static var selectLaborUnion: String { String(localized: "site.selectLaborUnion", defaultValue: "Select Labor Union", bundle: LanguageManager.shared.bundle) }
        static var searchLaborUnions: String { String(localized: "site.searchLaborUnions", defaultValue: "Search labor unions...", bundle: LanguageManager.shared.bundle) }
        static var noneOptional: String { String(localized: "site.noneOptional", defaultValue: "None (Optional)", bundle: LanguageManager.shared.bundle) }
        // ZP-2061: Office picker
        static var office: String { String(localized: "site.office", defaultValue: "Office", bundle: LanguageManager.shared.bundle) }
        static var selectOffice: String { String(localized: "site.selectOffice", defaultValue: "Select Office", bundle: LanguageManager.shared.bundle) }
        static var searchOffices: String { String(localized: "site.searchOffices", defaultValue: "Search offices...", bundle: LanguageManager.shared.bundle) }
        static var officeOptional: String { String(localized: "site.officeOptional", defaultValue: "Office (Optional)", bundle: LanguageManager.shared.bundle) }
        static var officeInfo: String { String(localized: "site.officeInfo", defaultValue: "When set, this language will be used as the default for all sites, accounts, and users in this office", bundle: LanguageManager.shared.bundle) }
        static var searchStateProvince: String { String(localized: "site.searchStateProvince", defaultValue: "Search...", bundle: LanguageManager.shared.bundle) }

        // Country names
        static var unitedStates: String { String(localized: "site.unitedStates", defaultValue: "United States", bundle: LanguageManager.shared.bundle) }
        static var canada: String { String(localized: "site.canada", defaultValue: "Canada", bundle: LanguageManager.shared.bundle) }
        static var unitedKingdom: String { String(localized: "site.unitedKingdom", defaultValue: "United Kingdom", bundle: LanguageManager.shared.bundle) }
        static var australia: String { String(localized: "site.australia", defaultValue: "Australia", bundle: LanguageManager.shared.bundle) }

        // Site selector
        static var selectSite: String { String(localized: "site.selectSite", defaultValue: "Select Site", bundle: LanguageManager.shared.bundle) }
        static var searchSites: String { String(localized: "site.searchSites", defaultValue: "Search sites...", bundle: LanguageManager.shared.bundle) }
        static var loadingAvailableSites: String { String(localized: "site.loadingAvailableSites", defaultValue: "Loading available sites...", bundle: LanguageManager.shared.bundle) }
        static var noSitesAvailable: String { String(localized: "site.noSitesAvailable", defaultValue: "No sites available", bundle: LanguageManager.shared.bundle) }
        static func noSitesMatch(_ search: String) -> String {
            String(localized: "site.noSitesMatch", defaultValue: "No sites match '\(search)'", bundle: LanguageManager.shared.bundle)
        }
        static var clearSearch: String { String(localized: "site.clearSearch", defaultValue: "Clear Search", bundle: LanguageManager.shared.bundle) }
        static var active: String { String(localized: "site.active", defaultValue: "Active", bundle: LanguageManager.shared.bundle) }
        static func pendingSyncCount(_ count: Int) -> String {
            String(localized: "site.pendingSyncCount", defaultValue: "\(count) pending", bundle: LanguageManager.shared.bundle)
        }

        // SLD Loading
        static var switchingSites: String { String(localized: "site.switchingSites", defaultValue: "Switching Sites", bundle: LanguageManager.shared.bundle) }
        static func loadingSite(_ name: String) -> String {
            String(localized: "site.loadingSite", defaultValue: "Loading \(name)", bundle: LanguageManager.shared.bundle)
        }
        static var loadingSiteGeneric: String { String(localized: "site.loadingSiteGeneric", defaultValue: "Loading Site", bundle: LanguageManager.shared.bundle) }
        static var syncFailed: String { String(localized: "site.syncFailed", defaultValue: "Sync Failed", bundle: LanguageManager.shared.bundle) }
        static var returnToSiteSelection: String { String(localized: "site.returnToSiteSelection", defaultValue: "Return to Site Selection", bundle: LanguageManager.shared.bundle) }
        static var preparing: String { String(localized: "site.preparing", defaultValue: "Preparing...", bundle: LanguageManager.shared.bundle) }
        static func itemsProcessed(_ current: Int, _ total: Int) -> String {
            String(localized: "site.itemsProcessed", defaultValue: "\(current) of \(total) items processed", bundle: LanguageManager.shared.bundle)
        }
        static func itemsCleared(_ current: Int, _ total: Int) -> String {
            String(localized: "site.itemsCleared", defaultValue: "\(current) of \(total) items cleared", bundle: LanguageManager.shared.bundle)
        }

        // Entity labels for loading
        static var equipment: String { String(localized: "site.equipment", defaultValue: "Equipment", bundle: LanguageManager.shared.bundle) }
        static var properties: String { String(localized: "site.properties", defaultValue: "Properties", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - SLD
    enum SLD {
        static var selectView: String { String(localized: "sld.selectView", defaultValue: "Select View", bundle: LanguageManager.shared.bundle) }
        static var errorNoViewSelected: String { String(localized: "sld.errorNoViewSelected", defaultValue: "Error: No view selected", bundle: LanguageManager.shared.bundle) }
        static var noViewsAvailable: String { String(localized: "sld.noViewsAvailable", defaultValue: "No Views Available", bundle: LanguageManager.shared.bundle) }
        static var noViewsDescription: String { String(localized: "sld.noViewsDescription", defaultValue: "This SLD doesn't have any views configured yet.", bundle: LanguageManager.shared.bundle) }
        static var allAssets: String { String(localized: "sld.allAssets", defaultValue: "All Assets", bundle: LanguageManager.shared.bundle) }
        static var defaultLabel: String { String(localized: "sld.defaultLabel", defaultValue: "Default", bundle: LanguageManager.shared.bundle) }
        static var edit: String { String(localized: "sld.edit", defaultValue: "Edit", bundle: LanguageManager.shared.bundle) }
        static var defaultView: String { String(localized: "sld.defaultView", defaultValue: "Default View", bundle: LanguageManager.shared.bundle) }
        static var locationBasedView: String { String(localized: "sld.locationBasedView", defaultValue: "Location-Based View", bundle: LanguageManager.shared.bundle) }
        static var views: String { String(localized: "sld.views", defaultValue: "Views", bundle: LanguageManager.shared.bundle) }
        static var confirmDelete: String { String(localized: "sld.confirmDelete", defaultValue: "Confirm Delete", bundle: LanguageManager.shared.bundle) }
        static func deleteNodeConfirm(_ name: String) -> String {
            String(localized: "sld.deleteNodeConfirm", defaultValue: "Are you sure you want to delete \(name)?", bundle: LanguageManager.shared.bundle)
        }
        static var deleteConnectionConfirm: String { String(localized: "sld.deleteConnectionConfirm", defaultValue: "Are you sure you want to delete this connection?", bundle: LanguageManager.shared.bundle) }
        static var viewDetails: String { String(localized: "sld.viewDetails", defaultValue: "View Details", bundle: LanguageManager.shared.bundle) }
        static var descriptionOptional: String { String(localized: "sld.descriptionOptional", defaultValue: "Description (optional)", bundle: LanguageManager.shared.bundle) }
        static var editView: String { String(localized: "sld.editView", defaultValue: "Edit View", bundle: LanguageManager.shared.bundle) }
        static var newView: String { String(localized: "sld.newView", defaultValue: "New View", bundle: LanguageManager.shared.bundle) }
        static var nameIsRequired: String { String(localized: "sld.nameIsRequired", defaultValue: "Name is required", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Diagnostics (Settings extensions)
    enum Diagnostics {
        // Sync Queue
        static func pendingCount(_ count: Int) -> String {
            String(localized: "diagnostics.pendingCount", defaultValue: "Pending (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static func historyCount(_ count: Int) -> String {
            String(localized: "diagnostics.historyCount", defaultValue: "History (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static var exportQueueAsJson: String { String(localized: "diagnostics.exportQueueAsJson", defaultValue: "Export Queue as JSON", bundle: LanguageManager.shared.bundle) }
        static var flushQueueNow: String { String(localized: "diagnostics.flushQueueNow", defaultValue: "Flush Queue Now", bundle: LanguageManager.shared.bundle) }
        static var reQueueAllFailed: String { String(localized: "diagnostics.reQueueAllFailed", defaultValue: "Re-queue All Failed", bundle: LanguageManager.shared.bundle) }
        static var clearAllHistory: String { String(localized: "diagnostics.clearAllHistory", defaultValue: "Clear All History", bundle: LanguageManager.shared.bundle) }
        static var clearSyncHistory: String { String(localized: "diagnostics.clearSyncHistory", defaultValue: "Clear Sync History?", bundle: LanguageManager.shared.bundle) }
        static var clearSyncHistoryMessage: String { String(localized: "diagnostics.clearSyncHistoryMessage", defaultValue: "This will permanently delete all sync history logs. Pending items will not be affected.", bundle: LanguageManager.shared.bundle) }
        static var noPendingOperations: String { String(localized: "diagnostics.noPendingOperations", defaultValue: "No Pending Operations", bundle: LanguageManager.shared.bundle) }
        static var allSyncProcessed: String { String(localized: "diagnostics.allSyncProcessed", defaultValue: "All sync operations have been processed", bundle: LanguageManager.shared.bundle) }
        static var noSyncHistory: String { String(localized: "diagnostics.noSyncHistory", defaultValue: "No Sync History", bundle: LanguageManager.shared.bundle) }
        static var completedSyncWillAppear: String { String(localized: "diagnostics.completedSyncWillAppear", defaultValue: "Completed sync operations will appear here", bundle: LanguageManager.shared.bundle) }
        static func deleteBuildingQuestion(_ name: String) -> String {
            String(localized: "diagnostics.deleteBuildingQuestion", defaultValue: "Delete Building?", bundle: LanguageManager.shared.bundle)
        }
        static var deleteFloorQuestion: String { String(localized: "diagnostics.deleteFloorQuestion", defaultValue: "Delete Floor?", bundle: LanguageManager.shared.bundle) }
        static var deleteRoomQuestion: String { String(localized: "diagnostics.deleteRoomQuestion", defaultValue: "Delete Room?", bundle: LanguageManager.shared.bundle) }
        static var deleteLocationQuestion: String { String(localized: "diagnostics.deleteLocationQuestion", defaultValue: "Delete Location?", bundle: LanguageManager.shared.bundle) }
        static func entityWillBeDeleted(_ type: String) -> String {
            String(localized: "diagnostics.entityWillBeDeleted", defaultValue: "This \(type) will be deleted.", bundle: LanguageManager.shared.bundle)
        }
        static func entityHasDescendants(_ type: String, _ counts: String) -> String {
            String(localized: "diagnostics.entityHasDescendants", defaultValue: "This \(type) has \(counts). Deleting will unassign these assets from their location.", bundle: LanguageManager.shared.bundle)
        }
        static var today: String { String(localized: "diagnostics.today", defaultValue: "Today", bundle: LanguageManager.shared.bundle) }
        static var yesterday: String { String(localized: "diagnostics.yesterday", defaultValue: "Yesterday", bundle: LanguageManager.shared.bundle) }
        static var totalFiles: String { String(localized: "diagnostics.totalFiles", defaultValue: "Total Files", bundle: LanguageManager.shared.bundle) }
        static var totalSize: String { String(localized: "diagnostics.totalSize", defaultValue: "Total Size", bundle: LanguageManager.shared.bundle) }
        static var noPhotosFound: String { String(localized: "diagnostics.noPhotosFound", defaultValue: "No photos found", bundle: LanguageManager.shared.bundle) }
        static var photoSavedToGalleryMsg: String { String(localized: "diagnostics.photoSavedToGallery", defaultValue: "Photo saved to gallery", bundle: LanguageManager.shared.bundle) }
        static var photoDiagnosticsTitle: String { String(localized: "diagnostics.photoDiagnostics", defaultValue: "Photo Diagnostics", bundle: LanguageManager.shared.bundle) }
        static var photoDetailsTitle: String { String(localized: "diagnostics.photoDetails", defaultValue: "Photo Details", bundle: LanguageManager.shared.bundle) }
        static var deletePhotosQuestion: String { String(localized: "diagnostics.deletePhotosQuestion", defaultValue: "Delete Photos?", bundle: LanguageManager.shared.bundle) }
        static var originalPath: String { String(localized: "diagnostics.originalPath", defaultValue: "Original Path:", bundle: LanguageManager.shared.bundle) }
        static var attemptedPath: String { String(localized: "diagnostics.attemptedPath", defaultValue: "Attempted Path:", bundle: LanguageManager.shared.bundle) }
        static var errorLabel: String { String(localized: "diagnostics.errorLabel", defaultValue: "Error:", bundle: LanguageManager.shared.bundle) }
        static var extraData: String { String(localized: "diagnostics.extraData", defaultValue: "Extra Data", bundle: LanguageManager.shared.bundle) }
        static func retries(_ count: Int) -> String {
            String(localized: "diagnostics.retries", defaultValue: "Retries: \(count)", bundle: LanguageManager.shared.bundle)
        }
        static var retry: String { String(localized: "diagnostics.retry", defaultValue: "Retry", bundle: LanguageManager.shared.bundle) }
        static var queued: String { String(localized: "diagnostics.queued", defaultValue: "Queued", bundle: LanguageManager.shared.bundle) }

        // ZP-2097 — per-item statuses, inline retry/delete on Pending.
        // ZP-1265 (aa123c2) — paused-banner copy removed; replaced by
        // network-lost dialog + persistent offline banner copy below.
        static var statusSyncing: String { String(localized: "diagnostics.statusSyncing", defaultValue: "Syncing…", bundle: LanguageManager.shared.bundle) }
        static var statusFailed: String { String(localized: "diagnostics.statusFailed", defaultValue: "Failed", bundle: LanguageManager.shared.bundle) }
        static var retryAction: String { String(localized: "diagnostics.retryAction", defaultValue: "Retry", bundle: LanguageManager.shared.bundle) }
        static var deleteAction: String { String(localized: "diagnostics.deleteAction", defaultValue: "Delete", bundle: LanguageManager.shared.bundle) }
        static func failureCode(_ code: String) -> String {
            String(localized: "diagnostics.failureCode", defaultValue: "Code: \(code)", bundle: LanguageManager.shared.bundle)
        }
        static func retriedNTimes(_ count: Int) -> String {
            String(localized: "diagnostics.retriedNTimes", defaultValue: "Retried \(count) time(s)", bundle: LanguageManager.shared.bundle)
        }
        static func failedAt(_ when: String) -> String {
            String(localized: "diagnostics.failedAt", defaultValue: "Failed: \(when)", bundle: LanguageManager.shared.bundle)
        }
        // Surfaced inline on Pending rows when the sync layer detects a CREATE
        // whose entity and snapshot are both gone — typically a v1.30 queue
        // item that survived an upgrade. Tells the user the item can't be
        // recovered automatically and points them at the Delete action.
        static var unrecoverableLegacyItemMessage: String {
            String(localized: "diagnostics.unrecoverableLegacyItem",
                   defaultValue: "This item was created in an older version of the app and its local data is no longer available. Tap Delete to remove it.",
                   bundle: LanguageManager.shared.bundle)
        }
        // ZP-2173 — single shared copy reused across every destructive surface
        // gated by `NetworkState.hasLegacySyncItems` (logout, site switch,
        // schedule, pull-to-refresh, IR session refresh). Tells the user
        // *why* the action is blocked and where to resolve the blockers.
        static var legacyItemsBlockingAction: String {
            String(localized: "diagnostics.legacyItemsBlockingAction",
                   defaultValue: "You have items from an older app version that need to be resolved in Sync Queue Analyzer before this action.",
                   bundle: LanguageManager.shared.bundle)
        }
        // ZP-1265 (aa123c2) — Network-lost dialog + offline banner copy.
        static var networkLostDialogTitle: String { String(localized: "diagnostics.networkLostDialogTitle", defaultValue: "Internet connection lost", bundle: LanguageManager.shared.bundle) }
        static var networkLostDialogBody: String { String(localized: "diagnostics.networkLostDialogBody", defaultValue: "Sync stopped because the internet connection was lost. Reconnect and tap Sync Now to continue.", bundle: LanguageManager.shared.bundle) }
        static var offlineBannerNoNetwork: String { String(localized: "diagnostics.offlineBannerNoNetwork", defaultValue: "No internet connection", bundle: LanguageManager.shared.bundle) }
        static var offlineBannerOfflineMode: String { String(localized: "diagnostics.offlineBannerOfflineMode", defaultValue: "Your offline mode is on", bundle: LanguageManager.shared.bundle) }

        // Photo Diagnostics
        static var photoStorage: String { String(localized: "diagnostics.photoStorage", defaultValue: "Photo Storage", bundle: LanguageManager.shared.bundle) }
        static var files: String { String(localized: "diagnostics.files", defaultValue: "Files", bundle: LanguageManager.shared.bundle) }
        static func deleteCount(_ count: Int) -> String {
            String(localized: "diagnostics.deleteCount", defaultValue: "Delete \(count)", bundle: LanguageManager.shared.bundle)
        }
        static var saveToGallery: String { String(localized: "diagnostics.saveToGallery", defaultValue: "Save to Gallery", bundle: LanguageManager.shared.bundle) }
        static var photoDiagnostics: String { String(localized: "diagnostics.photoDiagnostics", defaultValue: "Photo Diagnostics", bundle: LanguageManager.shared.bundle) }
        static var selectLabel: String { String(localized: "diagnostics.selectLabel", defaultValue: "Select", bundle: LanguageManager.shared.bundle) }
        static var deletePhotos: String { String(localized: "diagnostics.deletePhotos", defaultValue: "Delete Photos?", bundle: LanguageManager.shared.bundle) }
        static func deletePhotosMessage(_ count: Int) -> String {
            String(localized: "diagnostics.deletePhotosMessage", defaultValue: "This will permanently delete \(count) photo(s) from local storage.", bundle: LanguageManager.shared.bundle)
        }
        static func savedToGallery(_ saved: Int, _ total: Int) -> String {
            String(localized: "diagnostics.savedToGallery", defaultValue: "Saved \(saved) of \(total) photos to gallery", bundle: LanguageManager.shared.bundle)
        }
        static var pendingLabel: String { String(localized: "diagnostics.pendingLabel", defaultValue: "Pending", bundle: LanguageManager.shared.bundle) }
        static var syncedLabel: String { String(localized: "diagnostics.syncedLabel", defaultValue: "Synced", bundle: LanguageManager.shared.bundle) }
        static var fileInformation: String { String(localized: "diagnostics.fileInformation", defaultValue: "File Information", bundle: LanguageManager.shared.bundle) }
        static var filename: String { String(localized: "diagnostics.filename", defaultValue: "Filename", bundle: LanguageManager.shared.bundle) }
        static var size: String { String(localized: "diagnostics.size", defaultValue: "Size", bundle: LanguageManager.shared.bundle) }
        static var path: String { String(localized: "diagnostics.path", defaultValue: "Path", bundle: LanguageManager.shared.bundle) }
        static var databaseInformation: String { String(localized: "diagnostics.databaseInformation", defaultValue: "Database Information", bundle: LanguageManager.shared.bundle) }
        static var photoId: String { String(localized: "diagnostics.photoId", defaultValue: "Photo ID", bundle: LanguageManager.shared.bundle) }
        static var uploadStatus: String { String(localized: "diagnostics.uploadStatus", defaultValue: "Upload Status", bundle: LanguageManager.shared.bundle) }
        static var pendingUpload: String { String(localized: "diagnostics.pendingUpload", defaultValue: "Pending Upload", bundle: LanguageManager.shared.bundle) }
        static var remoteUrl: String { String(localized: "diagnostics.remoteUrl", defaultValue: "Remote URL:", bundle: LanguageManager.shared.bundle) }
        static var associatedNode: String { String(localized: "diagnostics.associatedNode", defaultValue: "Associated Node", bundle: LanguageManager.shared.bundle) }
        static var nodeId: String { String(localized: "diagnostics.nodeId", defaultValue: "Node ID", bundle: LanguageManager.shared.bundle) }
        static var label: String { String(localized: "diagnostics.label", defaultValue: "Label", bundle: LanguageManager.shared.bundle) }
        static var type: String { String(localized: "diagnostics.type", defaultValue: "Type", bundle: LanguageManager.shared.bundle) }
        static var viewNode: String { String(localized: "diagnostics.viewNode", defaultValue: "View Node", bundle: LanguageManager.shared.bundle) }
        static var saveToPhotoGallery: String { String(localized: "diagnostics.saveToPhotoGallery", defaultValue: "Save to Photo Gallery", bundle: LanguageManager.shared.bundle) }
        static var viewRemotePhoto: String { String(localized: "diagnostics.viewRemotePhoto", defaultValue: "View Remote Photo", bundle: LanguageManager.shared.bundle) }
        static var photoDetails: String { String(localized: "diagnostics.photoDetails", defaultValue: "Photo Details", bundle: LanguageManager.shared.bundle) }
        static var photoSavedToGallery: String { String(localized: "diagnostics.photoSavedToGallery", defaultValue: "Photo saved to gallery", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Platform Access
    enum PlatformAccess {
        static var checkingAccess: String { String(localized: "platformAccess.checkingAccess", defaultValue: "Checking Access", bundle: LanguageManager.shared.bundle) }
        static var verifyingPermissions: String { String(localized: "platformAccess.verifyingPermissions", defaultValue: "Verifying your permissions...", bundle: LanguageManager.shared.bundle) }
        static var mobileAccessRestricted: String { String(localized: "platformAccess.mobileAccessRestricted", defaultValue: "Mobile Access Restricted", bundle: LanguageManager.shared.bundle) }
        static var noPermissionMessage: String { String(localized: "platformAccess.noPermissionMessage", defaultValue: "Your account does not have permission to access the mobile platform.", bundle: LanguageManager.shared.bundle) }
        static var contactAdminMessage: String { String(localized: "platformAccess.contactAdminMessage", defaultValue: "This application is designed for field technicians and managers. If you believe this is an error, please contact your administrator to update your role permissions.", bundle: LanguageManager.shared.bundle) }
        static func currentRole(_ role: String) -> String {
            String(localized: "platformAccess.currentRole", defaultValue: "Current Role: **\(role)**", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Supporting Views
    enum Supporting {
        // IRSessionPickerButton
        static var noActiveSessions: String { String(localized: "supporting.noActiveSessions", defaultValue: "No Active Sessions", bundle: LanguageManager.shared.bundle) }
        static var unnamedSession: String { String(localized: "supporting.unnamedSession", defaultValue: "Unnamed Session", bundle: LanguageManager.shared.bundle) }

        // NodeMultiSelectionView
        static func selectedCount(_ count: Int) -> String {
            String(localized: "supporting.selectedCount", defaultValue: "\(count) selected", bundle: LanguageManager.shared.bundle)
        }
        static var noAssetsFound: String { String(localized: "supporting.noAssetsFound", defaultValue: "No assets found", bundle: LanguageManager.shared.bundle) }
        static var selectAssets: String { String(localized: "supporting.selectAssets", defaultValue: "Select Assets", bundle: LanguageManager.shared.bundle) }

        // ShortcutPickerView (renamed user-facing → "PM Plan(s)")
        static var suggestedShortcuts: String { String(localized: "supporting.suggestedShortcuts", defaultValue: "Suggested PM Plan (Optional)", bundle: LanguageManager.shared.bundle) }
        static var loadingShortcuts: String { String(localized: "supporting.loadingShortcuts", defaultValue: "Loading PM plans...", bundle: LanguageManager.shared.bundle) }
        static var selectAssetClassFirst: String { String(localized: "supporting.selectAssetClassFirst", defaultValue: "Select asset class first", bundle: LanguageManager.shared.bundle) }
        static var noShortcutsAvailable: String { String(localized: "supporting.noShortcutsAvailable", defaultValue: "No PM plans available", bundle: LanguageManager.shared.bundle) }
        static var selectShortcut: String { String(localized: "supporting.selectShortcut", defaultValue: "Select a PM plan", bundle: LanguageManager.shared.bundle) }

        // NetworkStatusButton
        static func syncRecords(_ count: Int) -> String {
            String(localized: "supporting.syncRecords", defaultValue: "Sync \(count) records", bundle: LanguageManager.shared.bundle)
        }
        static var goOffline: String { String(localized: "supporting.goOffline", defaultValue: "Go Offline", bundle: LanguageManager.shared.bundle) }
        static var goOnline: String { String(localized: "supporting.goOnline", defaultValue: "Go Online", bundle: LanguageManager.shared.bundle) }

        // CommonUIComponents
        static func enterTitle(_ title: String) -> String {
            String(localized: "supporting.enterTitle", defaultValue: "Enter \(title)", bundle: LanguageManager.shared.bundle)
        }

        // TerminalPicker
        static var sourceTerminal: String { String(localized: "supporting.sourceTerminal", defaultValue: "Source Terminal", bundle: LanguageManager.shared.bundle) }
        static var targetTerminal: String { String(localized: "supporting.targetTerminal", defaultValue: "Target Terminal", bundle: LanguageManager.shared.bundle) }

        // Debug
        static var debugLog: String { String(localized: "supporting.debugLog", defaultValue: "Debug Log", bundle: LanguageManager.shared.bundle) }

        // Refresh overlay
        static var syncing: String { String(localized: "supporting.syncing", defaultValue: "Syncing...", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Auth (extended)
    enum AuthExtra {
        static var setYourPassword: String { String(localized: "auth.setYourPassword", defaultValue: "Set Your Password", bundle: LanguageManager.shared.bundle) }
        static var setNewPasswordDescription: String { String(localized: "auth.setNewPasswordDescription", defaultValue: "Please set a new password and complete your profile", bundle: LanguageManager.shared.bundle) }
        static var passwordLabel: String { String(localized: "auth.passwordLabel", defaultValue: "PASSWORD", bundle: LanguageManager.shared.bundle) }
        static var newPassword: String { String(localized: "auth.newPassword", defaultValue: "New Password", bundle: LanguageManager.shared.bundle) }
        static var confirmPassword: String { String(localized: "auth.confirmPassword", defaultValue: "Confirm Password", bundle: LanguageManager.shared.bundle) }
        static var atLeast8Characters: String { String(localized: "auth.atLeast8Characters", defaultValue: "At least 8 characters", bundle: LanguageManager.shared.bundle) }
        static var containsUpperAndLower: String { String(localized: "auth.containsUpperAndLower", defaultValue: "Contains uppercase and lowercase", bundle: LanguageManager.shared.bundle) }
        static var containsNumberAndSpecial: String { String(localized: "auth.containsNumberAndSpecial", defaultValue: "Contains number and special character", bundle: LanguageManager.shared.bundle) }
        static var passwordsMatch: String { String(localized: "auth.passwordsMatch", defaultValue: "Passwords match", bundle: LanguageManager.shared.bundle) }
        static var profileInformation: String { String(localized: "auth.profileInformation", defaultValue: "PROFILE INFORMATION", bundle: LanguageManager.shared.bundle) }
        static var firstName: String { String(localized: "auth.firstName", defaultValue: "First Name", bundle: LanguageManager.shared.bundle) }
        static var lastName: String { String(localized: "auth.lastName", defaultValue: "Last Name", bundle: LanguageManager.shared.bundle) }
        static var timezone: String { String(localized: "auth.timezone", defaultValue: "Timezone", bundle: LanguageManager.shared.bundle) }
        static var completeSetup: String { String(localized: "auth.completeSetup", defaultValue: "Complete Setup", bundle: LanguageManager.shared.bundle) }
        static var offlineDataWarning: String { String(localized: "auth.offlineDataWarning", defaultValue: "Unsynced Data", bundle: LanguageManager.shared.bundle) }
        static var switchUser: String { String(localized: "auth.switchUser", defaultValue: "Switch User", bundle: LanguageManager.shared.bundle) }
        static func pendingOperationsWarning(_ count: Int) -> String {
            String(localized: "auth.pendingOperationsWarning", defaultValue: "You have \(count) pending operations that have not been synchronized. You may proceed to switch users; however, you will need to sign in again to sync these records. Please ensure the app is not uninstalled until all pending data has been successfully synchronized.", bundle: LanguageManager.shared.bundle)
        }
        static var sessionExpired: String { String(localized: "auth.sessionExpired", defaultValue: "Session Expired", bundle: LanguageManager.shared.bundle) }
        static var sessionExpiredMessage: String { String(localized: "auth.sessionExpiredMessage", defaultValue: "Your session has expired. Please sign in again to continue.", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Legal
    enum Legal {
        static var bySigningIn: String { String(localized: "legal.bySigningIn", defaultValue: "By signing in, you agree to our", bundle: LanguageManager.shared.bundle) }
        static var termsAndConditions: String { String(localized: "legal.termsAndConditions", defaultValue: "Terms & Conditions", bundle: LanguageManager.shared.bundle) }
        static var and: String { String(localized: "legal.and", defaultValue: "and", bundle: LanguageManager.shared.bundle) }
        static var privacyPolicy: String { String(localized: "legal.privacyPolicy", defaultValue: "Privacy Policy", bundle: LanguageManager.shared.bundle) }
        static var policyUpdate: String { String(localized: "legal.policyUpdate", defaultValue: "Policy Update", bundle: LanguageManager.shared.bundle) }
        static var pleaseReviewUpdated: String { String(localized: "legal.pleaseReviewUpdated", defaultValue: "Please review the updated policies to continue using the app.", bundle: LanguageManager.shared.bundle) }
        static var acceptAndContinue: String { String(localized: "legal.acceptAndContinue", defaultValue: "Accept & Continue", bundle: LanguageManager.shared.bundle) }
        static var acceptanceFailed: String { String(localized: "legal.acceptanceFailed", defaultValue: "Failed to record acceptance. Please try again.", bundle: LanguageManager.shared.bundle) }
        static var changes: String { String(localized: "legal.changes", defaultValue: "Changes:", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Forgot Password
    enum ForgotPassword {
        static var forgotPassword: String { String(localized: "forgotPassword.link", defaultValue: "Forgot your password?", bundle: LanguageManager.shared.bundle) }
        static var title: String { String(localized: "forgotPassword.title", defaultValue: "Forgot Password", bundle: LanguageManager.shared.bundle) }
        static var description: String { String(localized: "forgotPassword.description", defaultValue: "Enter your email address and we'll send you a reset code", bundle: LanguageManager.shared.bundle) }
        static var sendResetCode: String { String(localized: "forgotPassword.sendResetCode", defaultValue: "Send Reset Code", bundle: LanguageManager.shared.bundle) }
        static var sendingResetCode: String { String(localized: "forgotPassword.sendingResetCode", defaultValue: "Sending Reset Code...", bundle: LanguageManager.shared.bundle) }
        static var resetCodeSent: String { String(localized: "forgotPassword.resetCodeSent", defaultValue: "Reset code sent! Check your email.", bundle: LanguageManager.shared.bundle) }
        static var resetPasswordTitle: String { String(localized: "forgotPassword.resetPasswordTitle", defaultValue: "Reset Password", bundle: LanguageManager.shared.bundle) }
        static var resetPasswordSubtitle: String { String(localized: "forgotPassword.resetPasswordSubtitle", defaultValue: "Enter the reset code sent to your email and set a new password", bundle: LanguageManager.shared.bundle) }
        static var resetCode: String { String(localized: "forgotPassword.resetCode", defaultValue: "RESET CODE", bundle: LanguageManager.shared.bundle) }
        static var resetCodePlaceholder: String { String(localized: "forgotPassword.resetCodePlaceholder", defaultValue: "Enter reset code", bundle: LanguageManager.shared.bundle) }
        static var resetPasswordButton: String { String(localized: "forgotPassword.resetPasswordButton", defaultValue: "Reset Password", bundle: LanguageManager.shared.bundle) }
        static var resettingPassword: String { String(localized: "forgotPassword.resettingPassword", defaultValue: "Resetting Password...", bundle: LanguageManager.shared.bundle) }
        static var passwordResetSuccess: String { String(localized: "forgotPassword.passwordResetSuccess", defaultValue: "Password reset successfully! Returning to login...", bundle: LanguageManager.shared.bundle) }
        static var backToLogin: String { String(localized: "forgotPassword.backToLogin", defaultValue: "Back to Login", bundle: LanguageManager.shared.bundle) }
        static var requestNewCode: String { String(localized: "forgotPassword.requestNewCode", defaultValue: "Request New Code", bundle: LanguageManager.shared.bundle) }
        static var invalidEmailFormat: String { String(localized: "auth.invalidEmailFormat", defaultValue: "Please enter a valid email address", bundle: LanguageManager.shared.bundle) }
        static var invalidCredentials: String { String(localized: "auth.invalidCredentials", defaultValue: "Invalid email or password", bundle: LanguageManager.shared.bundle) }
        static var confirmEmail: String { String(localized: "auth.confirmEmail", defaultValue: "Please confirm your email address", bundle: LanguageManager.shared.bundle) }
        static var networkError: String { String(localized: "auth.networkError", defaultValue: "Network error. Please check your connection", bundle: LanguageManager.shared.bundle) }
        static var enterCompanyCodeError: String { String(localized: "auth.enterCompanyCodeError", defaultValue: "Please enter a company code", bundle: LanguageManager.shared.bundle) }
        static var fetchConfigFailed: String { String(localized: "auth.fetchConfigFailed", defaultValue: "Failed to fetch company configuration. Please check your internet connection and try again.", bundle: LanguageManager.shared.bundle) }
    }

    // MARK: - Copy Assets to Rooms (ZP-2198)
    enum CopyAssetsRooms {
        static var fabLabel: String { String(localized: "copyAssetsRooms.fabLabel", defaultValue: "Copy assets to other rooms", bundle: LanguageManager.shared.bundle) }
        static var title: String { String(localized: "copyAssetsRooms.title", defaultValue: "Copy assets to other rooms", bundle: LanguageManager.shared.bundle) }
        static var stepTargetRooms: String { String(localized: "copyAssetsRooms.stepTargetRooms", defaultValue: "Target rooms", bundle: LanguageManager.shared.bundle) }
        static var stepStrategy: String { String(localized: "copyAssetsRooms.stepStrategy", defaultValue: "Conflict", bundle: LanguageManager.shared.bundle) }
        static var stepConfirm: String { String(localized: "copyAssetsRooms.stepConfirm", defaultValue: "Confirm", bundle: LanguageManager.shared.bundle) }
        static var targetsHint: String { String(localized: "copyAssetsRooms.targetsHint", defaultValue: "Pick one or more target rooms. The source room is excluded.", bundle: LanguageManager.shared.bundle) }
        static var searchRoomsHint: String { String(localized: "copyAssetsRooms.searchRoomsHint", defaultValue: "Search rooms…", bundle: LanguageManager.shared.bundle) }
        static var selectAll: String { String(localized: "copyAssetsRooms.selectAll", defaultValue: "Select all", bundle: LanguageManager.shared.bundle) }
        static var noOtherRooms: String { String(localized: "copyAssetsRooms.noOtherRooms", defaultValue: "No other rooms available in this site.", bundle: LanguageManager.shared.bundle) }
        static func targetCountSelected(_ count: Int) -> String {
            String(localized: "copyAssetsRooms.targetCountSelected", defaultValue: "\(count) target room(s) selected", bundle: LanguageManager.shared.bundle)
        }
        static var noAssets: String { String(localized: "copyAssetsRooms.noAssets", defaultValue: "No assets in this room for the current session.", bundle: LanguageManager.shared.bundle) }
        static var strategyHint: String { String(localized: "copyAssetsRooms.strategyHint", defaultValue: "Choose how to handle target rooms that already have assets in this session.", bundle: LanguageManager.shared.bundle) }
        static var strategyTitle: String { String(localized: "copyAssetsRooms.strategyTitle", defaultValue: "When a target room already has assets in this session", bundle: LanguageManager.shared.bundle) }
        static var append: String { String(localized: "copyAssetsRooms.append", defaultValue: "Append", bundle: LanguageManager.shared.bundle) }
        static var appendDesc: String { String(localized: "copyAssetsRooms.appendDesc", defaultValue: "Add new copies alongside any existing assets in the target room.", bundle: LanguageManager.shared.bundle) }
        static var overwrite: String { String(localized: "copyAssetsRooms.overwrite", defaultValue: "Overwrite", bundle: LanguageManager.shared.bundle) }
        static var overwriteDesc: String { String(localized: "copyAssetsRooms.overwriteDesc", defaultValue: "Remove existing session assets in the target room before copying. This cannot be undone.", bundle: LanguageManager.shared.bundle) }
        static func confirmMessage(assetCount: Int, source: String, roomCount: Int) -> String {
            String(localized: "copyAssetsRooms.confirmMessage", defaultValue: "Copy \(assetCount) asset(s) from \(source) to \(roomCount) room(s)?", bundle: LanguageManager.shared.bundle)
        }
        static var targetsLabel: String { String(localized: "copyAssetsRooms.targetsLabel", defaultValue: "Target rooms", bundle: LanguageManager.shared.bundle) }
        static var strategyLabel: String { String(localized: "copyAssetsRooms.strategyLabel", defaultValue: "Strategy", bundle: LanguageManager.shared.bundle) }
        static var overwriteWarningTitle: String { String(localized: "copyAssetsRooms.overwriteWarningTitle", defaultValue: "Overwrite", bundle: LanguageManager.shared.bundle) }
        static func overwriteWarningBody(_ count: Int) -> String {
            String(localized: "copyAssetsRooms.overwriteWarningBody", defaultValue: "\(count) existing asset(s) will be unlinked from this session in the target room(s).", bundle: LanguageManager.shared.bundle)
        }
        static var run: String { String(localized: "copyAssetsRooms.run", defaultValue: "Start Copy", bundle: LanguageManager.shared.bundle) }
        static func runningProgress(current: Int, total: Int) -> String {
            String(localized: "copyAssetsRooms.runningProgress", defaultValue: "Copying to room \(current) of \(total)…", bundle: LanguageManager.shared.bundle)
        }
        /// Shown in the Done step when the user was offline during the copy. The trailing
        /// sentence calls out that the work is queued for sync.
        static func successMessageOffline(assetCount: Int, roomCount: Int) -> String {
            String(localized: "copyAssetsRooms.successMessage", defaultValue: "Copied \(assetCount) asset(s) into \(roomCount) room(s).\n\nAssets saved locally and will sync when online.", bundle: LanguageManager.shared.bundle)
        }

        /// Shown in the Done step when the user was online during the copy. Drops the
        /// "saved locally" caveat since the writes either reached the server or are already
        /// in the queue and will drain immediately.
        static func successMessageOnline(assetCount: Int, roomCount: Int) -> String {
            String(localized: "copyAssetsRooms.successMessageOnline", defaultValue: "Copied \(assetCount) asset(s) into \(roomCount) room(s).", bundle: LanguageManager.shared.bundle)
        }
        static func partialMessage(created: Int, failed: Int) -> String {
            String(localized: "copyAssetsRooms.partialMessage", defaultValue: "Copied \(created). \(failed) failed.", bundle: LanguageManager.shared.bundle)
        }
        static func perRoomResult(created: Int, failed: Int) -> String {
            String(localized: "copyAssetsRooms.perRoomResult", defaultValue: "Created: \(created) • Failed: \(failed)", bundle: LanguageManager.shared.bundle)
        }
        static var perRoomSkipped: String { String(localized: "copyAssetsRooms.perRoomSkipped", defaultValue: "Skipped", bundle: LanguageManager.shared.bundle) }
        static var back: String { String(localized: "copyAssetsRooms.back", defaultValue: "Back", bundle: LanguageManager.shared.bundle) }
        static var next: String { String(localized: "copyAssetsRooms.next", defaultValue: "Next", bundle: LanguageManager.shared.bundle) }
        static var cancel: String { String(localized: "copyAssetsRooms.cancel", defaultValue: "Cancel", bundle: LanguageManager.shared.bundle) }
        static var done: String { String(localized: "copyAssetsRooms.done", defaultValue: "Done", bundle: LanguageManager.shared.bundle) }
        static func loadFailed(_ message: String) -> String {
            String(localized: "copyAssetsRooms.loadFailed", defaultValue: "Failed to load: \(message)", bundle: LanguageManager.shared.bundle)
        }
        static func existingAssetsBadge(_ count: Int) -> String {
            String(localized: "copyAssetsRooms.existingAssetsBadge", defaultValue: "\(count) asset(s)", bundle: LanguageManager.shared.bundle)
        }
    }

    // MARK: - Engineering (ZP-2398)
    enum Engineering {
        // Section + bound library
        static var subtype: String { String(localized: "engineering.subtype", defaultValue: "Subtype", bundle: LanguageManager.shared.bundle) }
        static var libraryMatched: String { String(localized: "engineering.libraryMatched", defaultValue: "Library Matched", bundle: LanguageManager.shared.bundle) }

        // Transformer / OCP
        static var tertiaryVoltage: String { String(localized: "engineering.tertiaryVoltage", defaultValue: "Tertiary Voltage", bundle: LanguageManager.shared.bundle) }
        static var kvaRating: String { String(localized: "engineering.kvaRating", defaultValue: "kVA Rating", bundle: LanguageManager.shared.bundle) }
        static var percentImpedance: String { String(localized: "engineering.percentImpedance", defaultValue: "% Impedance", bundle: LanguageManager.shared.bundle) }
        static var type: String { String(localized: "engineering.type", defaultValue: "Type", bundle: LanguageManager.shared.bundle) }
        static var manufacturer: String { String(localized: "engineering.manufacturer", defaultValue: "Manufacturer", bundle: LanguageManager.shared.bundle) }
        static var mainsType: String { String(localized: "engineering.mainsType", defaultValue: "Mains Type", bundle: LanguageManager.shared.bundle) }
        static var phaseConfiguration: String { String(localized: "engineering.phaseConfiguration", defaultValue: "Phase Configuration", bundle: LanguageManager.shared.bundle) }
        static var circuitVoltage: String { String(localized: "engineering.circuitVoltage", defaultValue: "Circuit Voltage", bundle: LanguageManager.shared.bundle) }
        static var fuseCount: String { String(localized: "engineering.fuseCount", defaultValue: "Fuse Count", bundle: LanguageManager.shared.bundle) }
        static var fuseAmperage: String { String(localized: "engineering.fuseAmperage", defaultValue: "Fuse Amperage", bundle: LanguageManager.shared.bundle) }
        static var poleCount: String { String(localized: "engineering.poleCount", defaultValue: "Pole Count", bundle: LanguageManager.shared.bundle) }
        static var frameAmps: String { String(localized: "engineering.frameAmps", defaultValue: "Frame Amps", bundle: LanguageManager.shared.bundle) }
        static var sensorAmps: String { String(localized: "engineering.sensorAmps", defaultValue: "Sensor Amps", bundle: LanguageManager.shared.bundle) }
        static var plugAmps: String { String(localized: "engineering.plugAmps", defaultValue: "Plug Amps", bundle: LanguageManager.shared.bundle) }
        static var tripAmps: String { String(localized: "engineering.tripAmps", defaultValue: "Trip Amps", bundle: LanguageManager.shared.bundle) }
        static var ampereRating: String { String(localized: "engineering.ampereRating", defaultValue: "Ampere Rating", bundle: LanguageManager.shared.bundle) }

        // Cable / Busway
        static var length: String { String(localized: "engineering.length", defaultValue: "Length", bundle: LanguageManager.shared.bundle) }
        static var conductorMaterial: String { String(localized: "engineering.conductorMaterial", defaultValue: "Conductor Material", bundle: LanguageManager.shared.bundle) }
        static var conductorDescription: String { String(localized: "engineering.conductorDescription", defaultValue: "Conductor Description", bundle: LanguageManager.shared.bundle) }
        static var size: String { String(localized: "engineering.size", defaultValue: "Size", bundle: LanguageManager.shared.bundle) }
        static var insulationClass: String { String(localized: "engineering.insulationClass", defaultValue: "Insulation Class", bundle: LanguageManager.shared.bundle) }
        static var insulationType: String { String(localized: "engineering.insulationType", defaultValue: "Insulation Type", bundle: LanguageManager.shared.bundle) }
        static var installation: String { String(localized: "engineering.installation", defaultValue: "Installation", bundle: LanguageManager.shared.bundle) }
        static var ductMaterial: String { String(localized: "engineering.ductMaterial", defaultValue: "Duct Material", bundle: LanguageManager.shared.bundle) }
        static var buswaySizeAmps: String { String(localized: "engineering.buswaySizeAmps", defaultValue: "Busway Size (Amps)", bundle: LanguageManager.shared.bundle) }
        static var insulation: String { String(localized: "engineering.insulation", defaultValue: "Insulation", bundle: LanguageManager.shared.bundle) }
        static var construction: String { String(localized: "engineering.construction", defaultValue: "Construction", bundle: LanguageManager.shared.bundle) }

        // Pickers & generic
        static var selectEllipsis: String { String(localized: "engineering.selectEllipsis", defaultValue: "Select…", bundle: LanguageManager.shared.bundle) }
        static var select: String { String(localized: "engineering.select", defaultValue: "Select", bundle: LanguageManager.shared.bundle) }

        // Transformer config card
        static var transformerConfiguration: String { String(localized: "engineering.transformerConfiguration", defaultValue: "Transformer Configuration", bundle: LanguageManager.shared.bundle) }
        static var kva: String { String(localized: "engineering.kva", defaultValue: "kVA", bundle: LanguageManager.shared.bundle) }
        static var selectKva: String { String(localized: "engineering.selectKva", defaultValue: "Select kVA", bundle: LanguageManager.shared.bundle) }
        static var primaryConnection: String { String(localized: "engineering.primaryConnection", defaultValue: "Primary Connection", bundle: LanguageManager.shared.bundle) }
        static var secondaryConnection: String { String(localized: "engineering.secondaryConnection", defaultValue: "Secondary Connection", bundle: LanguageManager.shared.bundle) }

        // Cable / busway config card
        static var cableConfiguration: String { String(localized: "engineering.cableConfiguration", defaultValue: "Cable Configuration", bundle: LanguageManager.shared.bundle) }
        static var buswayConfiguration: String { String(localized: "engineering.buswayConfiguration", defaultValue: "Busway Configuration", bundle: LanguageManager.shared.bundle) }
        static var qtyPerPhase: String { String(localized: "engineering.qtyPerPhase", defaultValue: "Qty per Phase", bundle: LanguageManager.shared.bundle) }
        static var groundSize: String { String(localized: "engineering.groundSize", defaultValue: "Ground Size", bundle: LanguageManager.shared.bundle) }
        static var groundSizePlaceholder: String { String(localized: "engineering.groundSizePlaceholder", defaultValue: "e.g. 2/0", bundle: LanguageManager.shared.bundle) }
        static var connectionType: String { String(localized: "engineering.connectionType", defaultValue: "Connection Type", bundle: LanguageManager.shared.bundle) }

        // Trip config card
        static var tripConfiguration: String { String(localized: "engineering.tripConfiguration", defaultValue: "Trip Configuration", bundle: LanguageManager.shared.bundle) }
        static var fuseRating: String { String(localized: "engineering.fuseRating", defaultValue: "Fuse Rating", bundle: LanguageManager.shared.bundle) }
        static var frame: String { String(localized: "engineering.frame", defaultValue: "Frame", bundle: LanguageManager.shared.bundle) }
        static var sensor: String { String(localized: "engineering.sensor", defaultValue: "Sensor", bundle: LanguageManager.shared.bundle) }
        static var trip: String { String(localized: "engineering.trip", defaultValue: "Trip", bundle: LanguageManager.shared.bundle) }
        static var plug: String { String(localized: "engineering.plug", defaultValue: "Plug", bundle: LanguageManager.shared.bundle) }
        static var tripUnit: String { String(localized: "engineering.tripUnit", defaultValue: "Trip Unit", bundle: LanguageManager.shared.bundle) }
        static func selectLowercased(_ field: String) -> String {
            String(localized: "engineering.selectField", defaultValue: "Select \(field)", bundle: LanguageManager.shared.bundle)
        }

        // Ground Fault section
        static var addGroundFaultSettings: String { String(localized: "engineering.addGroundFaultSettings", defaultValue: "Add Ground Fault Settings", bundle: LanguageManager.shared.bundle) }
        static var autoPairedGfLibrary: String { String(localized: "engineering.autoPairedGfLibrary", defaultValue: "Auto-paired GF library:", bundle: LanguageManager.shared.bundle) }
        static var manuallyPickedGfLibrary: String { String(localized: "engineering.manuallyPickedGfLibrary", defaultValue: "Manually picked GF library:", bundle: LanguageManager.shared.bundle) }
        static var changeEllipsis: String { String(localized: "engineering.changeEllipsis", defaultValue: "Change…", bundle: LanguageManager.shared.bundle) }
        static var groundFaultSettings: String { String(localized: "engineering.groundFaultSettings", defaultValue: "Ground Fault Settings", bundle: LanguageManager.shared.bundle) }
        static var noGfPairFound: String { String(localized: "engineering.noGfPairFound", defaultValue: "No GF setting pair found for this device.", bundle: LanguageManager.shared.bundle) }
        static var addAnyway: String { String(localized: "engineering.addAnyway", defaultValue: "Add anyway", bundle: LanguageManager.shared.bundle) }

        // Segments
        static var settings: String { String(localized: "engineering.settings", defaultValue: "Settings", bundle: LanguageManager.shared.bundle) }
        static var selectValue: String { String(localized: "engineering.selectValue", defaultValue: "Select value", bundle: LanguageManager.shared.bundle) }
        static var on: String { String(localized: "engineering.on", defaultValue: "On", bundle: LanguageManager.shared.bundle) }
        static var off: String { String(localized: "engineering.off", defaultValue: "Off", bundle: LanguageManager.shared.bundle) }
        static var locked: String { String(localized: "engineering.locked", defaultValue: "locked", bundle: LanguageManager.shared.bundle) }
        static var fixed: String { String(localized: "engineering.fixed", defaultValue: "Fixed", bundle: LanguageManager.shared.bundle) }
        static var dial: String { String(localized: "engineering.dial", defaultValue: "Dial", bundle: LanguageManager.shared.bundle) }

        // Match results panel
        static var matchSearchPlaceholder: String { String(localized: "engineering.matchSearchPlaceholder", defaultValue: "e.g. \"QD\" or \"Formula\"", bundle: LanguageManager.shared.bundle) }
        static var noMatchesRefine: String { String(localized: "engineering.noMatchesRefine", defaultValue: "No matches — refine the filters above or load the SKM library from Settings.", bundle: LanguageManager.shared.bundle) }
        static var noMatchesSearch: String { String(localized: "engineering.noMatchesSearch", defaultValue: "No matches for that search.", bundle: LanguageManager.shared.bundle) }
        static func loadMore(_ count: Int) -> String {
            String(localized: "engineering.loadMore", defaultValue: "Load more (\(count))", bundle: LanguageManager.shared.bundle)
        }
        static var noPossibleMatches: String { String(localized: "engineering.noPossibleMatches", defaultValue: "No possible matches", bundle: LanguageManager.shared.bundle) }
        static func possibleMatchesCount(_ count: Int, truncated: Bool) -> String {
            let suffix = truncated ? "+" : ""
            if count == 1 {
                return String(localized: "engineering.possibleMatchSingular", defaultValue: "\(count)\(suffix) possible match", bundle: LanguageManager.shared.bundle)
            }
            return String(localized: "engineering.possibleMatchPlural", defaultValue: "\(count)\(suffix) possible matches", bundle: LanguageManager.shared.bundle)
        }

        // Ground Fault picker sheet
        static var pickGroundFaultLibrary: String { String(localized: "engineering.pickGroundFaultLibrary", defaultValue: "Pick Ground Fault Library", bundle: LanguageManager.shared.bundle) }
        static func gfManufacturerHint(_ manufacturer: String) -> String {
            String(localized: "engineering.gfManufacturerHint", defaultValue: "\(manufacturer) — search the SKM library for the GF protection variant.", bundle: LanguageManager.shared.bundle)
        }
        static var gfSearchPlaceholder: String { String(localized: "engineering.gfSearchPlaceholder", defaultValue: "Search (type / description / catalog)", bundle: LanguageManager.shared.bundle) }
        static var gfCatalog: String { String(localized: "engineering.gfCatalog", defaultValue: "Catalog", bundle: LanguageManager.shared.bundle) }
        static var gfTypeContains: String { String(localized: "engineering.gfTypeContains", defaultValue: "Type contains", bundle: LanguageManager.shared.bundle) }
        static var gfSensorA: String { String(localized: "engineering.gfSensorA", defaultValue: "Sensor A", bundle: LanguageManager.shared.bundle) }
        static var gfPlugA: String { String(localized: "engineering.gfPlugA", defaultValue: "Plug A", bundle: LanguageManager.shared.bundle) }
        static var gfFrameA: String { String(localized: "engineering.gfFrameA", defaultValue: "Frame A", bundle: LanguageManager.shared.bundle) }
        static var gfVoltage: String { String(localized: "engineering.gfVoltage", defaultValue: "Voltage", bundle: LanguageManager.shared.bundle) }
        static var gfNoMatches: String { String(localized: "engineering.gfNoMatches", defaultValue: "No matches. Try clearing some filters.", bundle: LanguageManager.shared.bundle) }
        static var gfMoreResults: String { String(localized: "engineering.gfMoreResults", defaultValue: "More results available — refine your filters.", bundle: LanguageManager.shared.bundle) }

        // ZP-2267: Add Custom sheet + bound-card "Custom Entry" UX
        static var addCustom: String { String(localized: "engineering.addCustom", defaultValue: "Add Custom", bundle: LanguageManager.shared.bundle) }
        static var addCustomEquipment: String { String(localized: "engineering.addCustomEquipment", defaultValue: "Add Custom Equipment", bundle: LanguageManager.shared.bundle) }
        static var editCustomEquipment: String { String(localized: "engineering.editCustomEquipment", defaultValue: "Edit Custom Equipment", bundle: LanguageManager.shared.bundle) }
        static var customEntry: String { String(localized: "engineering.customEntry", defaultValue: "Custom Entry", bundle: LanguageManager.shared.bundle) }
        static var edit: String { String(localized: "engineering.edit", defaultValue: "Edit", bundle: LanguageManager.shared.bundle) }
        static var identity: String { String(localized: "engineering.identity", defaultValue: "Identity", bundle: LanguageManager.shared.bundle) }
        static var typeCatalog: String { String(localized: "engineering.typeCatalog", defaultValue: "Type / Catalog", bundle: LanguageManager.shared.bundle) }
        static var styleModel: String { String(localized: "engineering.styleModel", defaultValue: "Style / Model", bundle: LanguageManager.shared.bundle) }
        static var amps: String { String(localized: "engineering.amps", defaultValue: "Amps", bundle: LanguageManager.shared.bundle) }
        static var cableSpecifications: String { String(localized: "engineering.cableSpecifications", defaultValue: "Cable Specifications", bundle: LanguageManager.shared.bundle) }
        static var transformerSpecifications: String { String(localized: "engineering.transformerSpecifications", defaultValue: "Transformer Specifications", bundle: LanguageManager.shared.bundle) }
        static var conductorType: String { String(localized: "engineering.conductorType", defaultValue: "Conductor Type", bundle: LanguageManager.shared.bundle) }
        static var cableSize: String { String(localized: "engineering.cableSize", defaultValue: "Cable Size", bundle: LanguageManager.shared.bundle) }
        static var sizeAmps: String { String(localized: "engineering.sizeAmps", defaultValue: "Size (Amps)", bundle: LanguageManager.shared.bundle) }
        static var lengthFt: String { String(localized: "engineering.lengthFt", defaultValue: "Length (ft)", bundle: LanguageManager.shared.bundle) }
        static var kvaRatingLabel: String { String(localized: "engineering.kvaRatingLabel", defaultValue: "kVA Rating", bundle: LanguageManager.shared.bundle) }
        static var zPercent: String { String(localized: "engineering.zPercent", defaultValue: "Z (%)", bundle: LanguageManager.shared.bundle) }
        static var primaryVoltageField: String { String(localized: "engineering.primaryVoltageField", defaultValue: "Primary Voltage", bundle: LanguageManager.shared.bundle) }
        static var secondaryVoltageField: String { String(localized: "engineering.secondaryVoltageField", defaultValue: "Secondary Voltage", bundle: LanguageManager.shared.bundle) }
        static var priConnection: String { String(localized: "engineering.priConnection", defaultValue: "Pri Connection", bundle: LanguageManager.shared.bundle) }
        static var secConnection: String { String(localized: "engineering.secConnection", defaultValue: "Sec Connection", bundle: LanguageManager.shared.bundle) }
        static var tripSettings: String { String(localized: "engineering.tripSettings", defaultValue: "Trip Settings", bundle: LanguageManager.shared.bundle) }
        static var groundFaultSettingsHeader: String { String(localized: "engineering.groundFaultSettingsHeader", defaultValue: "Ground Fault Settings", bundle: LanguageManager.shared.bundle) }
        static var includeGroundFault: String { String(localized: "engineering.includeGroundFault", defaultValue: "Include Ground Fault settings", bundle: LanguageManager.shared.bundle) }
        static var additionalSettings: String { String(localized: "engineering.additionalSettings", defaultValue: "Additional Settings", bundle: LanguageManager.shared.bundle) }
        static var addSetting: String { String(localized: "engineering.addSetting", defaultValue: "Add Setting", bundle: LanguageManager.shared.bundle) }
        static var function: String { String(localized: "engineering.function", defaultValue: "Function", bundle: LanguageManager.shared.bundle) }
        static var settingLabel: String { String(localized: "engineering.settingLabel", defaultValue: "Label", bundle: LanguageManager.shared.bundle) }
        static var settingValue: String { String(localized: "engineering.settingValue", defaultValue: "Value", bundle: LanguageManager.shared.bundle) }
        static var settingPlaceholderValue: String { String(localized: "engineering.settingPlaceholderValue", defaultValue: "Value (e.g. 0.8 x Sensor)", bundle: LanguageManager.shared.bundle) }
        static var settingPlaceholderLabel: String { String(localized: "engineering.settingPlaceholderLabel", defaultValue: "e.g. \"INST (Ii)\"", bundle: LanguageManager.shared.bundle) }
        static var iSquaredT: String { String(localized: "engineering.iSquaredT", defaultValue: "I²t", bundle: LanguageManager.shared.bundle) }
        static var dialPickerLabel: String { String(localized: "engineering.dialPickerLabel", defaultValue: "Dial", bundle: LanguageManager.shared.bundle) }
        // Friendly subtype labels — mirror the JSX SUBTYPE_FRIENDLY_LABELS array.
        static var subtypePickup: String { String(localized: "engineering.subtypePickup", defaultValue: "Pickup", bundle: LanguageManager.shared.bundle) }
        static var subtypeTimeDelay: String { String(localized: "engineering.subtypeTimeDelay", defaultValue: "Time Delay", bundle: LanguageManager.shared.bundle) }
        static var subtypeInst: String { String(localized: "engineering.subtypeInst", defaultValue: "INST", bundle: LanguageManager.shared.bundle) }
        static var subtypeInstDelay: String { String(localized: "engineering.subtypeInstDelay", defaultValue: "INST Delay", bundle: LanguageManager.shared.bundle) }
        static var subtypeInstBands: String { String(localized: "engineering.subtypeInstBands", defaultValue: "INST Bands", bundle: LanguageManager.shared.bundle) }
        static var subtypeInstTolerance: String { String(localized: "engineering.subtypeInstTolerance", defaultValue: "INST Tolerance", bundle: LanguageManager.shared.bundle) }
        static var subtypeStDelayI2t: String { String(localized: "engineering.subtypeStDelayI2t", defaultValue: "ST Delay (I²t)", bundle: LanguageManager.shared.bundle) }
        static var subtypeClearingSlope: String { String(localized: "engineering.subtypeClearingSlope", defaultValue: "Clearing Slope", bundle: LanguageManager.shared.bundle) }
        static var subtypeClearingBands: String { String(localized: "engineering.subtypeClearingBands", defaultValue: "Clearing Bands", bundle: LanguageManager.shared.bundle) }
        static var subtypeIPowerT: String { String(localized: "engineering.subtypeIPowerT", defaultValue: "Iᵖt", bundle: LanguageManager.shared.bundle) }
        static var subtypeCurrentSet: String { String(localized: "engineering.subtypeCurrentSet", defaultValue: "Current Set", bundle: LanguageManager.shared.bundle) }
        static var subtypeTccBands: String { String(localized: "engineering.subtypeTccBands", defaultValue: "TCC Bands", bundle: LanguageManager.shared.bundle) }
        static var subtypeEquation1: String { String(localized: "engineering.subtypeEquation1", defaultValue: "Equation 1", bundle: LanguageManager.shared.bundle) }
        static var subtypeEquation2: String { String(localized: "engineering.subtypeEquation2", defaultValue: "Equation 2", bundle: LanguageManager.shared.bundle) }
        static var subtypeEquation3: String { String(localized: "engineering.subtypeEquation3", defaultValue: "Equation 3", bundle: LanguageManager.shared.bundle) }
        // Trip-setting fixed-slot labels — match the web SST_FIXED_SLOTS / GF_FIXED_SLOTS labels.
        static var ltPickup: String { String(localized: "engineering.ltPickup", defaultValue: "LT Pickup", bundle: LanguageManager.shared.bundle) }
        static var ltDelay: String { String(localized: "engineering.ltDelay", defaultValue: "LT Delay", bundle: LanguageManager.shared.bundle) }
        static var stPickup: String { String(localized: "engineering.stPickup", defaultValue: "ST Pickup", bundle: LanguageManager.shared.bundle) }
        static var stDelay: String { String(localized: "engineering.stDelay", defaultValue: "ST Delay", bundle: LanguageManager.shared.bundle) }
        static var inst: String { String(localized: "engineering.inst", defaultValue: "Inst", bundle: LanguageManager.shared.bundle) }
        static var gfPickup: String { String(localized: "engineering.gfPickup", defaultValue: "GF Pickup", bundle: LanguageManager.shared.bundle) }
        static var gfDelay: String { String(localized: "engineering.gfDelay", defaultValue: "GF Delay", bundle: LanguageManager.shared.bundle) }
        // Settings — Engineering Library
        static var engineeringLibraryDisabled: String { String(localized: "engineering.libraryDisabled", defaultValue: "Engineering Library isn't enabled for your company. Contact your admin to enable.", bundle: LanguageManager.shared.bundle) }
        static var loadDeviceLibraryConfirmation: String { String(localized: "engineering.loadDeviceLibraryConfirmation", defaultValue: "Load Device Library?", bundle: LanguageManager.shared.bundle) }
        static var loadDeviceLibraryMessage: String { String(localized: "engineering.loadDeviceLibraryMessage", defaultValue: "Downloads the full device library (~5 MB) to enable offline matching in the Engineering section. Replaces any prior cached library.", bundle: LanguageManager.shared.bundle) }
        static var starting: String { String(localized: "engineering.starting", defaultValue: "Starting…", bundle: LanguageManager.shared.bundle) }
        static func downloadFailed(_ message: String) -> String {
            String(localized: "engineering.downloadFailed", defaultValue: "Download failed: \(message)", bundle: LanguageManager.shared.bundle)
        }
    }
}
