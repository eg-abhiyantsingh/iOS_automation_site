//
//  ResetPasswordViewModel.swift
//  Egalvanic PZ
//

import Foundation

@MainActor
class ResetPasswordViewModel: ObservableObject {
    @Published var resetCode = ""
    @Published var newPassword = ""
    @Published var confirmPassword = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var shouldNavigateToLogin = false
    @Published var isPasswordSecure = true
    @Published var isConfirmPasswordSecure = true

    private let email: String
    private let subdomain: String

    init(email: String, subdomain: String) {
        self.email = email
        self.subdomain = subdomain
    }

    var passwordLengthValid: Bool {
        newPassword.count >= 8
    }

    var passwordCaseValid: Bool {
        let hasUppercase = newPassword.range(of: "[A-Z]", options: .regularExpression) != nil
        let hasLowercase = newPassword.range(of: "[a-z]", options: .regularExpression) != nil
        return hasUppercase && hasLowercase
    }

    var passwordSpecialValid: Bool {
        let hasNumber = newPassword.range(of: "[0-9]", options: .regularExpression) != nil
        let hasSpecial = newPassword.range(of: "[^A-Za-z0-9]", options: .regularExpression) != nil
        return hasNumber && hasSpecial
    }

    var passwordsMatch: Bool {
        !newPassword.isEmpty && newPassword == confirmPassword
    }

    var isResetDisabled: Bool {
        resetCode.trimmingCharacters(in: .whitespaces).isEmpty ||
        !passwordLengthValid ||
        !passwordCaseValid ||
        !passwordSpecialValid ||
        !passwordsMatch ||
        isLoading
    }

    func resetPassword() {
        guard !isResetDisabled else { return }

        errorMessage = nil
        successMessage = nil
        isLoading = true

        Task {
            defer { isLoading = false }

            do {
                _ = try await AuthService.shared.resetPassword(
                    email: email,
                    code: resetCode.trimmingCharacters(in: .whitespaces),
                    newPassword: newPassword,
                    subdomain: subdomain
                )
                successMessage = AppStrings.ForgotPassword.passwordResetSuccess
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    self.shouldNavigateToLogin = true
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}
