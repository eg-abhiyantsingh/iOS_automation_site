//
//  CompanyCodeViewModel.swift
//  Egalvanic PZ
//
//  ViewModel for company code entry screen
//

import SwiftUI
import Combine

@MainActor
class CompanyCodeViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var companyCode = ""
    @Published var isLoading = false
    @Published var errorMessage: String?

    // MARK: - Private Properties
    private let configService = CompanyConfigService.shared

    // Enable logging only in DEBUG builds
    #if DEBUG
    private let enableLogging = true
    #else
    private let enableLogging = false
    #endif

    // MARK: - Initialization
    init() {
        // Load previously saved company code if available
        if let storedConfig = configService.loadStoredConfig() {
            companyCode = storedConfig.companyCode
            log("📖 Loaded saved company code: \(companyCode)")
        }
    }

    // MARK: - Logging Helper
    private func log(_ message: String) {
        AppLogger.log(.debug, "[CompanyCodeViewModel] \(message)", category: .auth)
    }

    // MARK: - Computed Properties
    var isSubmitDisabled: Bool {
        isLoading || companyCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    // MARK: - Public Methods

    /// Fetch company configuration and proceed to login
    func submitCompanyCode(onSuccess: @escaping (CompanyConfig) -> Void) {
        let trimmedCode = companyCode.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedCode.isEmpty else {
            errorMessage = AppStrings.ForgotPassword.enterCompanyCodeError
            return
        }

        isLoading = true
        errorMessage = nil
        log("🔄 Fetching config for company code: \(trimmedCode)")

        Task {
            do {
                let config = try await configService.fetchCompanyConfig(companyCode: trimmedCode)

                // Save the config
                configService.saveConfig(config)

                log("✅ Successfully fetched and saved config")
                isLoading = false

                // Call success callback on main thread
                await MainActor.run {
                    onSuccess(config)
                }
            } catch let error as CompanyConfigError {
                await MainActor.run {
                    isLoading = false
                    errorMessage = error.localizedDescription
                    log("❌ Company config error: \(error.localizedDescription)")
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = AppStrings.ForgotPassword.fetchConfigFailed
                    log("❌ Network error: \(error.localizedDescription)")
                }
            }
        }
    }

    /// Dismiss keyboard
    func dismissKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}
