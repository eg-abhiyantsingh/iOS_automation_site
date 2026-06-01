//
//  CompanyCodeView.swift
//  Egalvanic PZ
//
//  Initial login screen for entering company code
//

import SwiftUI

struct CompanyCodeView: View {
    @EnvironmentObject var languageManager: LanguageManager
    @StateObject private var viewModel = CompanyCodeViewModel()
    @State private var navigateToLogin = false
    @State private var companyConfig: CompanyConfig?
    @State private var formOpacity: Double = 0
    @State private var formOffset: CGFloat = 30
    @State private var logoOpacity: Double = 0
    @State private var logoScale: CGFloat = 0.8
    @State private var welcomeOpacity: Double = 0
    @State private var welcomeOffset: CGFloat = -20
    @State private var submitButtonScale: CGFloat = 1.0
    @State private var showCompanyCodeTooltip: Bool = false
    @FocusState private var companyCodeFocused: Bool

    let onLoginSuccess: (String, String) -> Void

    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                let isLandscape = geometry.size.width > geometry.size.height

                ScrollView {
                    VStack(spacing: 0) {
                        // Language Picker
                        HStack {
                            Spacer()
                            Menu {
                                ForEach(AppLanguage.allCases, id: \.self) { language in
                                    Button(action: { languageManager.setLanguage(language) }) {
                                        HStack {
                                            Text("\(language.flagEmoji) \(language.displayName)")
                                            if languageManager.currentLanguage == language {
                                                Image(systemName: "checkmark")
                                            }
                                        }
                                    }
                                }
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "globe")
                                        .font(.system(.subheadline))
                                    Text(languageManager.currentLanguage.displayName)
                                        .font(.system(.caption, design: .rounded, weight: .medium))
                                }
                                .foregroundColor(.blue)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color.blue.opacity(0.1))
                                )
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.top, 8)

                        // Top section with logo and welcome message
                        VStack(spacing: isLandscape ? 12 : 20) {
                            // Logo
                            Image("Logo")
                                .resizable()
                                .scaledToFit()
                                .frame(width: isLandscape ? 80 : 120, height: isLandscape ? 80 : 120)
                                .padding(.bottom, isLandscape ? 5 : 10)
                                .opacity(logoOpacity)
                                .scaleEffect(logoScale)

                            // Welcome message
                            VStack(spacing: isLandscape ? 4 : 8) {
                                Text(AppStrings.Auth.welcome)
                                    .font(.system(isLandscape ? .title2 : .largeTitle, design: .rounded, weight: .bold))

                                Text(AppStrings.Auth.enterCompanyCode)
                                    .font(.system(isLandscape ? .caption : .subheadline, design: .rounded))
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                            }
                            .opacity(welcomeOpacity)
                            .offset(y: welcomeOffset)
                        }
                        .padding(.top, isLandscape ? 20 : 40)
                        .padding(.bottom, isLandscape ? 20 : 30)

                        // Company code form section
                        VStack(spacing: 24) {
                            VStack(spacing: 20) {
                                // Company code field
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack(spacing: 4) {
                                        Text(AppStrings.Auth.companyCode)
                                            .font(.system(.subheadline, design: .rounded, weight: .medium))
                                            .foregroundColor(.secondary)

                                        Button(action: {
                                            companyCodeFocused = false
                                            showCompanyCodeTooltip = true
                                        }) {
                                            Image(systemName: "info.circle")
                                                .font(.system(.caption2))
                                                .foregroundColor(.blue)
                                        }
                                        .popover(isPresented: $showCompanyCodeTooltip) {
                                            VStack(alignment: .leading, spacing: 12) {
                                                Text(AppStrings.Auth.companyCodeHelp)
                                                    .font(.system(.headline, design: .rounded, weight: .semibold))
                                                    .fixedSize(horizontal: false, vertical: true)

                                                Text(AppStrings.Auth.companyCodeExplanation)
                                                    .font(.system(.subheadline, design: .rounded))
                                                    .fixedSize(horizontal: false, vertical: true)

                                                VStack(alignment: .leading, spacing: 4) {
                                                    Text(AppStrings.Auth.companyCodeFormat)
                                                        .font(.system(.subheadline, design: .rounded))
                                                    Text(AppStrings.Auth.companyCodeTemplate)
                                                        .font(.system(.subheadline, design: .rounded, weight: .semibold))
                                                        .foregroundColor(.blue)
                                                        .fixedSize(horizontal: false, vertical: true)
                                                    Text(AppStrings.Auth.companyCodeDefaultPartner)
                                                        .font(.system(.subheadline, design: .rounded))
                                                }
                                                .fixedSize(horizontal: false, vertical: true)

                                                VStack(alignment: .leading, spacing: 4) {
                                                    Text(AppStrings.Auth.companyCodeExample)
                                                        .font(.system(.subheadline, design: .rounded, weight: .medium))
                                                    Text(AppStrings.Auth.companyCodeExampleText)
                                                        .font(.system(.subheadline, design: .rounded))
                                                    Text(AppStrings.Auth.companyCodeExampleValue)
                                                        .font(.system(.subheadline, design: .rounded, weight: .semibold))
                                                        .foregroundColor(.blue)
                                                }
                                                .fixedSize(horizontal: false, vertical: true)

                                                Text(AppStrings.Auth.companyCodeNote)
                                                    .font(.system(.caption, design: .rounded))
                                                    .foregroundColor(.secondary)
                                                    .fixedSize(horizontal: false, vertical: true)
                                            }
                                            .frame(width: 280)
                                            .padding()
                                            .presentationCompactAdaptation(.popover)
                                        }
                                    }

                                    TextField(AppStrings.Auth.companyCodePlaceholder, text: $viewModel.companyCode)
                                        .textFieldStyle(ModernTextFieldStyle())
                                        .autocapitalization(.none)
                                        .disableAutocorrection(true)
                                        .focused($companyCodeFocused)
                                        .submitLabel(.go)
                                        .onSubmit {
                                            submitCompanyCode()
                                        }
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

                            // Continue button
                            Button(action: {
                                withAnimation(.easeInOut(duration: 0.1)) {
                                    submitButtonScale = 0.95
                                }

                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                    withAnimation(.easeInOut(duration: 0.1)) {
                                        submitButtonScale = 1.0
                                    }
                                }

                                submitCompanyCode()
                            }) {
                                HStack(spacing: 8) {
                                    if viewModel.isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.9)
                                    } else {
                                        Text(AppStrings.Common.continueAction)
                                            .font(.system(.body, design: .rounded, weight: .semibold))
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(
                                            LinearGradient(
                                                gradient: Gradient(colors: viewModel.isSubmitDisabled ?
                                                    [Color.gray.opacity(0.6), Color.gray.opacity(0.4)] :
                                                    [Color.blue, Color.blue.opacity(0.8)]
                                                ),
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                )
                                .foregroundColor(.white)
                                .shadow(color: viewModel.isSubmitDisabled ? .clear : .blue.opacity(0.3), radius: 8, x: 0, y: 4)
                                .scaleEffect(submitButtonScale)
                            }
                            .disabled(viewModel.isSubmitDisabled)
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 30)
                        .opacity(formOpacity)
                        .offset(y: formOffset)
                    }
                }
            }
            .background(Color(UIColor.systemBackground))
            .onTapGesture {
                viewModel.dismissKeyboard()
            }
            .onAppear {
                // Animate logo appearance
                withAnimation(.easeOut(duration: 0.6)) {
                    logoOpacity = 1
                    logoScale = 1.0
                }

                // Animate welcome text
                withAnimation(.easeOut(duration: 0.6).delay(0.1)) {
                    welcomeOpacity = 1
                    welcomeOffset = 0
                }

                // Animate form appearance
                withAnimation(.easeOut(duration: 0.8).delay(0.3)) {
                    formOpacity = 1
                    formOffset = 0
                }
            }
            .navigationDestination(isPresented: $navigateToLogin) {
                if let config = companyConfig {
                    LoginViewWithConfig(
                        companyConfig: config,
                        onLoginSuccess: onLoginSuccess,
                        onBackToCompanyCode: {
                            navigateToLogin = false
                            companyConfig = nil
                        }
                    )
                    .navigationBarBackButtonHidden(true)
                }
            }
        }
    }

    // MARK: - Private Methods

    private func submitCompanyCode() {
        viewModel.submitCompanyCode { config in
            companyConfig = config
            navigateToLogin = true
        }
    }
}
