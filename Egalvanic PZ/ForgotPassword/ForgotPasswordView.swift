//
//  ForgotPasswordView.swift
//  Egalvanic PZ
//

import SwiftUI

struct ForgotPasswordView: View {
    let email: String
    let subdomain: String
    let onBackToLogin: () -> Void
    let onNavigateToReset: (String) -> Void

    @StateObject private var viewModel: ForgotPasswordViewModel
    @State private var formOpacity: Double = 0
    @State private var formOffset: CGFloat = 30
    @Environment(\.verticalSizeClass) var verticalSizeClass

    init(email: String, subdomain: String, onBackToLogin: @escaping () -> Void, onNavigateToReset: @escaping (String) -> Void) {
        self.email = email
        self.subdomain = subdomain
        self.onBackToLogin = onBackToLogin
        self.onNavigateToReset = onNavigateToReset
        self._viewModel = StateObject(wrappedValue: ForgotPasswordViewModel(email: email, subdomain: subdomain))
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

                    Text(AppStrings.ForgotPassword.title)
                        .font(.system(isCompact ? .title2 : .largeTitle, design: .rounded, weight: .bold))

                    Text(AppStrings.ForgotPassword.description)
                        .font(.system(isCompact ? .caption : .subheadline, design: .rounded))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                .padding(.top, isCompact ? 20 : 60)

                // Form
                VStack(spacing: isCompact ? 16 : 20) {
                    // Email field
                    VStack(alignment: .leading, spacing: isCompact ? 8 : 10) {
                        Text(AppStrings.Common.email)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.secondary)

                        TextField(AppStrings.Auth.enterYourEmail, text: $viewModel.email)
                            .textFieldStyle(ModernTextFieldStyle())
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                    }

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
                    // Send Reset Code button
                    Button(action: {
                        viewModel.sendResetCode()
                    }) {
                        HStack(spacing: 8) {
                            if viewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.9)
                            } else {
                                Text(AppStrings.ForgotPassword.sendResetCode)
                                    .font(.system(isCompact ? .callout : .body, design: .rounded, weight: .semibold))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: isCompact ? 48 : 56)
                        .background(
                            RoundedRectangle(cornerRadius: isCompact ? 14 : 16)
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(colors: viewModel.isSendDisabled ?
                                            [Color.gray.opacity(0.6), Color.gray.opacity(0.4)] :
                                            [Color.blue, Color.blue.opacity(0.8)]
                                        ),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                        )
                        .foregroundColor(.white)
                        .shadow(color: viewModel.isSendDisabled ? .clear : .blue.opacity(0.3), radius: isCompact ? 6 : 8, x: 0, y: isCompact ? 3 : 4)
                    }
                    .disabled(viewModel.isSendDisabled)

                    // Back to Login
                    Button(action: onBackToLogin) {
                        Text(AppStrings.ForgotPassword.backToLogin)
                            .font(.system(isCompact ? .caption : .subheadline, design: .rounded, weight: .medium))
                            .foregroundColor(.blue)
                    }
                    .padding(.top, 4)
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
        .onChange(of: viewModel.shouldNavigateToReset) { _, shouldNavigate in
            if shouldNavigate {
                onNavigateToReset(viewModel.email)
            }
        }
    }
}
