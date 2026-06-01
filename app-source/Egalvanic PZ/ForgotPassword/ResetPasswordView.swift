//
//  ResetPasswordView.swift
//  Egalvanic PZ
//

import SwiftUI

struct ResetPasswordView: View {
    let email: String
    let subdomain: String
    let onBackToLogin: () -> Void
    let onRequestNewCode: () -> Void

    @StateObject private var viewModel: ResetPasswordViewModel
    @State private var formOpacity: Double = 0
    @State private var formOffset: CGFloat = 30
    @State private var newPasswordFocused: Bool = false
    @State private var confirmPasswordFocused: Bool = false
    @Environment(\.verticalSizeClass) var verticalSizeClass

    init(email: String, subdomain: String, onBackToLogin: @escaping () -> Void, onRequestNewCode: @escaping () -> Void) {
        self.email = email
        self.subdomain = subdomain
        self.onBackToLogin = onBackToLogin
        self.onRequestNewCode = onRequestNewCode
        self._viewModel = StateObject(wrappedValue: ResetPasswordViewModel(email: email, subdomain: subdomain))
    }

    var body: some View {
        let isCompact = verticalSizeClass == .compact

        ScrollView {
            VStack(spacing: isCompact ? 20 : 32) {
                // Header
                VStack(spacing: 12) {
                    Image(systemName: "lock.rotation")
                        .font(.system(size: isCompact ? 40 : 56))
                        .foregroundColor(.blue)
                        .padding(.bottom, 4)

                    Text(AppStrings.ForgotPassword.resetPasswordTitle)
                        .font(.system(isCompact ? .title2 : .largeTitle, design: .rounded, weight: .bold))

                    Text(AppStrings.ForgotPassword.resetPasswordSubtitle)
                        .font(.system(isCompact ? .caption : .subheadline, design: .rounded))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                .padding(.top, isCompact ? 20 : 60)

                // Form
                VStack(spacing: isCompact ? 16 : 20) {
                    // Reset code field
                    VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                        Text(AppStrings.ForgotPassword.resetCode)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.secondary)

                        TextField(AppStrings.ForgotPassword.resetCodePlaceholder, text: $viewModel.resetCode)
                            .textFieldStyle(ModernTextFieldStyle())
                            .autocapitalization(.none)
                            .textContentType(.oneTimeCode)
                    }

                    // New password field
                    VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                        Text(AppStrings.AuthExtra.newPassword)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.secondary)

                        ModernSecureFieldStyle(
                            text: $viewModel.newPassword,
                            placeholder: AppStrings.AuthExtra.newPassword,
                            isSecure: $viewModel.isPasswordSecure,
                            textContentType: .newPassword,
                            isFocused: $newPasswordFocused
                        )
                    }

                    // Confirm password field
                    VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                        Text(AppStrings.AuthExtra.confirmPassword)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.secondary)

                        ModernSecureFieldStyle(
                            text: $viewModel.confirmPassword,
                            placeholder: AppStrings.AuthExtra.confirmPassword,
                            isSecure: $viewModel.isConfirmPasswordSecure,
                            textContentType: .newPassword,
                            isFocused: $confirmPasswordFocused
                        )
                    }

                    // Password requirements checklist
                    VStack(alignment: .leading, spacing: 6) {
                        Label(AppStrings.AuthExtra.atLeast8Characters, systemImage: viewModel.passwordLengthValid ? "checkmark.circle.fill" : "circle")
                            .font(.caption)
                            .foregroundColor(viewModel.passwordLengthValid ? .green : .secondary)

                        Label(AppStrings.AuthExtra.containsUpperAndLower, systemImage: viewModel.passwordCaseValid ? "checkmark.circle.fill" : "circle")
                            .font(.caption)
                            .foregroundColor(viewModel.passwordCaseValid ? .green : .secondary)

                        Label(AppStrings.AuthExtra.containsNumberAndSpecial, systemImage: viewModel.passwordSpecialValid ? "checkmark.circle.fill" : "circle")
                            .font(.caption)
                            .foregroundColor(viewModel.passwordSpecialValid ? .green : .secondary)

                        Label(AppStrings.AuthExtra.passwordsMatch, systemImage: viewModel.passwordsMatch ? "checkmark.circle.fill" : "circle")
                            .font(.caption)
                            .foregroundColor(viewModel.passwordsMatch ? .green : .secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 8)

                    // Success message
                    if let successMessage = viewModel.successMessage {
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(.caption, weight: .medium))
                            Text(successMessage)
                                .font(.system(.caption, design: .rounded))
                        }
                        .foregroundColor(.green)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 4)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    // Error message
                    if let errorMessage = viewModel.errorMessage {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .font(.system(.caption, weight: .medium))
                            Text(errorMessage)
                                .font(.system(.caption, design: .rounded))
                        }
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 4)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }

                // Buttons
                VStack(spacing: isCompact ? 12 : 16) {
                    // Reset Password button
                    Button(action: {
                        viewModel.resetPassword()
                    }) {
                        HStack(spacing: 8) {
                            if viewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.9)
                            } else {
                                Text(AppStrings.ForgotPassword.resetPasswordButton)
                                    .font(.system(isCompact ? .callout : .body, design: .rounded, weight: .semibold))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: isCompact ? 48 : 56)
                        .background(
                            RoundedRectangle(cornerRadius: isCompact ? 14 : 16)
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(colors: viewModel.isResetDisabled ?
                                            [Color.gray.opacity(0.6), Color.gray.opacity(0.4)] :
                                            [Color.blue, Color.blue.opacity(0.8)]
                                        ),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .foregroundColor(.white)
                        .shadow(color: viewModel.isResetDisabled ? .clear : .blue.opacity(0.3), radius: isCompact ? 6 : 8, x: 0, y: isCompact ? 3 : 4)
                    }
                    .disabled(viewModel.isResetDisabled)

                    // Request New Code
                    Button(action: onRequestNewCode) {
                        Text(AppStrings.ForgotPassword.requestNewCode)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.blue)
                    }
                    .padding(.top, 4)

                    // Back to Login
                    Button(action: onBackToLogin) {
                        Text(AppStrings.ForgotPassword.backToLogin)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 30)
            .opacity(formOpacity)
            .offset(y: formOffset)
        }
        .background(Color(UIColor.systemBackground))
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.8).delay(0.1)) {
                formOpacity = 1
                formOffset = 0
            }
        }
        .onChange(of: viewModel.shouldNavigateToLogin) { _, shouldNavigate in
            if shouldNavigate {
                onBackToLogin()
            }
        }
    }
}
