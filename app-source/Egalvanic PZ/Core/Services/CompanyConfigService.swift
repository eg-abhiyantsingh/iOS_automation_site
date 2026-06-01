//
//  CompanyConfigService.swift
//  Egalvanic PZ
//
//  Service for fetching and managing company configuration
//

import Foundation

class CompanyConfigService {
    static let shared = CompanyConfigService()

    private let userDefaultsKey = "storedCompanyConfig"
    private let configBaseURL = Configuration.apiBaseURLString

    // Enable logging only in DEBUG builds
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    private init() {}

    // MARK: - Logging Helper
    private func log(_ message: String) {
        if enableLogging {
            AppLogger.log(.debug, "[CompanyConfigService] \(message)", category: .auth)
        }
    }

    // MARK: - Public Methods

    /// Fetch company configuration from backend
    func fetchCompanyConfig(companyCode: String) async throws -> CompanyConfig {
        let endpoint = "\(configBaseURL)\(APIEndpoints.Company.config(for: companyCode))"

        guard let url = URL(string: endpoint) else {
            throw URLError(.badURL)
        }

        log("📡 Fetching company config from: \(endpoint)")

        let (data, response) = try await URLSession.shared.data(from: url)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            log("❌ HTTP error: \(httpResponse.statusCode)")
            if httpResponse.statusCode == 400 {
                throw CompanyConfigError.companyNotFound
            }
            throw CompanyConfigError.invalidResponse
        }

        let decoder = JSONDecoder()
        let config = try decoder.decode(CompanyConfig.self, from: data)

        log("✅ Successfully fetched config for company: \(config.company.name)")
        log("   Invoke URL: \(config.alliancePartner?.invokeUrl ?? "none")")

        return config
    }

    /// Save company configuration to UserDefaults
    func saveConfig(_ config: CompanyConfig) {
        let storedConfig = StoredCompanyConfig(from: config)

        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(storedConfig)
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
            log("💾 Saved company config to UserDefaults")
            log("   Company: \(storedConfig.companyName)")
            log("   Invoke URL: \(storedConfig.invokeUrl)")
        } catch {
            log("❌ Failed to save config: \(error.localizedDescription)")
        }
    }

    /// Load stored company configuration from UserDefaults
    func loadStoredConfig() -> StoredCompanyConfig? {
        guard let data = UserDefaults.standard.data(forKey: userDefaultsKey) else {
            log("⚠️ No stored config found")
            return nil
        }

        do {
            let decoder = JSONDecoder()
            let config = try decoder.decode(StoredCompanyConfig.self, from: data)
            log("📖 Loaded stored config for company: \(config.companyName)")
            return config
        } catch {
            log("❌ Failed to load stored config: \(error.localizedDescription)")
            return nil
        }
    }

    /// Clear stored company configuration
    func clearConfig() {
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
        log("🗑️ Cleared stored company config")
    }

    /// Get the current API invoke URL (from stored config or fallback)
    func getCurrentInvokeURL() -> String {
        if let storedConfig = loadStoredConfig() {
            return storedConfig.invokeUrl
        }
        return Configuration.apiBaseURLString
    }

    /// Get the current company logo URL
    func getCurrentLogoURL() -> String? {
        return loadStoredConfig()?.logoUrl
    }

    /// Get the current company name
    func getCurrentCompanyName() -> String? {
        return loadStoredConfig()?.companyName
    }
}

// MARK: - Errors

enum CompanyConfigError: LocalizedError {
    case companyNotFound
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .companyNotFound:
            return "Company not found. Please check your company code and try again."
        case .invalidResponse:
            return "Invalid response from server. Please try again."
        }
    }
}
