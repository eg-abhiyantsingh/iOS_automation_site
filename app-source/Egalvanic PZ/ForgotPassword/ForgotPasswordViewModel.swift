//
//  ForgotPasswordViewModel.swift
//  Egalvanic PZ
//

import Foundation

@MainActor
class ForgotPasswordViewModel: ObservableObject {
    @Published var email: String
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var shouldNavigateToReset = false

    private let subdomain: String

    init(email: String, subdomain: String) {
        self.email = email
        self.subdomain = subdomain
    }

    var isValidEmail: Bool {
        let emailRegex = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }

    var isSendDisabled: Bool {
        !isValidEmail || isLoading
    }

    func sendResetCode() {
        guard isValidEmail else { return }

        errorMessage = nil
        successMessage = nil
        isLoading = true

        Task {
            defer { isLoading = false }

            do {
                _ = try await AuthService.shared.forgotPassword(email: email, subdomain: subdomain)
                successMessage = AppStrings.ForgotPassword.resetCodeSent
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    self.shouldNavigateToReset = true
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}
