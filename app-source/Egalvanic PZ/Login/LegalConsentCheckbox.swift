//
//  LegalConsentText.swift
//  Egalvanic PZ
//
//  Inline text showing Terms & Conditions and Privacy Policy links on sign-in
//

import SwiftUI

struct LegalConsentText: View {
    let termsURL: URL?
    let privacyURL: URL?

    @State private var safariURL: IdentifiableURL?

    var body: some View {
        Text(attributedConsent)
            .font(.system(.footnote, design: .rounded))
            .foregroundColor(.secondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .environment(\.openURL, OpenURLAction { url in
                safariURL = IdentifiableURL(url: url)
                return .handled
            })
            .sheet(item: $safariURL) { item in
                SafariView(url: item.url)
            }
    }

    private var attributedConsent: AttributedString {
        var result = AttributedString(AppStrings.Legal.bySigningIn + " ")

        if let termsURL {
            var terms = AttributedString(AppStrings.Legal.termsAndConditions)
            terms.link = termsURL
            terms.underlineStyle = .single
            terms.foregroundColor = .blue
            result.append(terms)
        }

        if termsURL != nil && privacyURL != nil {
            result.append(AttributedString(" \(AppStrings.Legal.and) "))
        }

        if let privacyURL {
            var privacy = AttributedString(AppStrings.Legal.privacyPolicy)
            privacy.link = privacyURL
            privacy.underlineStyle = .single
            privacy.foregroundColor = .blue
            result.append(privacy)
        }

        return result
    }
}

private struct IdentifiableURL: Identifiable {
    let id = UUID()
    let url: URL
}
