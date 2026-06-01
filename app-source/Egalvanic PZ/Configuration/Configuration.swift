//
//  Configuration.swift
//  Egalvanic PZ
//
//  Created by Kush Jadia on 13/11/25.
//


import Foundation

enum Configuration {

    // MARK: - API Configuration

    /// The base URL for API requests, configured per environment
    static let apiBaseURL: URL = {
        // Try to get from Info.plist (will be set when xcconfig is properly configured)
        if let urlString = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String,
           !urlString.isEmpty,
           !urlString.contains("$(") {  // Check if it's not the unresolved variable
            // Remove the $() workaround used in xcconfig if present
            let cleanURLString = urlString.replacingOccurrences(of: "$()", with: "")

            if let url = URL(string: cleanURLString) {
                AppLogger.log(.info, "Configuration: Using API URL from xcconfig: \(cleanURLString)", category: .general)
                return url
            }
        }

        // Fallback to hardcoded URL if xcconfig not set up yet
        // This allows the app to run before Xcode configuration is complete
        let fallbackURL = "https://eg-pz.egalvanic.ai/api"
        AppLogger.log(.notice, "Configuration: Using fallback API URL (xcconfig not configured): \(fallbackURL). Configure xcconfig files in Xcode.", category: .general)

        return URL(string: fallbackURL)!
    }()

    /// The base URL string for API requests (for compatibility with existing code)
    static let apiBaseURLString: String = {
        // Try to get from Info.plist (will be set when xcconfig is properly configured)
        if let urlString = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String,
           !urlString.isEmpty,
           !urlString.contains("$(") {  // Check if it's not the unresolved variable
            // Remove the $() workaround used in xcconfig if present
            let cleanURLString = urlString.replacingOccurrences(of: "$()", with: "")
            AppLogger.log(.info, "Configuration: Using API URL string from xcconfig: \(cleanURLString)", category: .general)
            return cleanURLString
        }

        // Fallback to hardcoded URL if xcconfig not set up yet
        let fallbackURL = "https://eg-pz.egalvanic.ai/api"
        AppLogger.log(.notice, "Configuration: Using fallback API URL string (xcconfig not configured): \(fallbackURL)", category: .general)

        return fallbackURL
    }()

    // MARK: - Environment Information

    /// The current environment name (Debug, Staging, or Production)
    static let environmentName: String = {
        guard let environment = Bundle.main.object(forInfoDictionaryKey: "ENVIRONMENT_NAME") as? String else {
            return "Unknown"
        }
        return environment
    }()

    /// Check if running in production environment
    static var isProduction: Bool {
        return environmentName == "Production"
    }

    /// Check if running in debug environment
    static var isDebug: Bool {
        return environmentName == "Debug"
    }

    /// Check if running in staging environment
    static var isStaging: Bool {
        return environmentName == "Staging"
    }
    
    /// Check if running in staging environment
    static var isQA: Bool {
        return environmentName == "QA"
    }

    // MARK: - Sentry Configuration

    /// The Sentry DSN for crash reporting, configured per environment
    static let sentryDSN: String? = {
        guard let dsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String,
              !dsn.isEmpty,
              !dsn.contains("YOUR_") else {
            return nil
        }
        // Remove the $() workaround used in xcconfig
        return dsn.replacingOccurrences(of: "$()", with: "")
    }()

    // MARK: - DevRev Configuration

    /// The DevRev App ID for in-app support, configured per environment
    static let devRevAppID: String? = {
        guard let appID = Bundle.main.object(forInfoDictionaryKey: "DEVREV_APP_ID") as? String,
              !appID.isEmpty,
              !appID.contains("$("),
              !appID.contains("YOUR_") else {
            return nil
        }
        return appID
    }()

    // MARK: - Dynamic URL Helpers

    /// Get the current dynamic API URL (company-specific invoke URL or fallback)
    /// This should be used for all API calls except the initial company config fetch
    static var dynamicAPIURL: URL {
        let urlString = CompanyConfigService.shared.getCurrentInvokeURL()
        return URL(string: urlString) ?? apiBaseURL
    }

    /// Get the current dynamic API URL as string
    static var dynamicAPIURLString: String {
        CompanyConfigService.shared.getCurrentInvokeURL()
    }

    // MARK: - Debug Helpers

    /// Print current configuration (for debugging)
    static func printConfiguration() {
        let companyName = CompanyConfigService.shared.getCurrentCompanyName() ?? "N/A"
        AppLogger.log(.info, "Configuration: env=\(environmentName), staticURL=\(apiBaseURLString), dynamicURL=\(dynamicAPIURLString), production=\(isProduction), company=\(companyName)", category: .general)
    }
}
