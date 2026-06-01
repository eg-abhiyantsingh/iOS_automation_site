//
//  LegalAcceptanceDialogView.swift
//  Egalvanic PZ
//
//  Non-dismissable bottom sheet shown when returning users have pending legal document acceptance.
//  Follows the same pattern as ReAuthenticationView.
//

import SwiftUI

struct LegalAcceptanceDialogView: View {
    @ObservedObject var legalService: LegalAcceptanceService
    @State private var safariURL: IdentifiableURL?

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Icon
                    iconSection

                    // Title and description
                    titleSection

                    // Pending documents
                    documentsSection

                    // Error message
                    if let error = legalService.acceptanceError {
                        errorCard(error)
                    }

                    // Accept button
                    acceptButton
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .sheet(item: $safariURL) { item in
            SafariView(url: item.url)
        }
    }

    // MARK: - View Components

    private var iconSection: some View {
        ZStack {
            Circle()
                .fill(Color.blue.opacity(0.15))
                .frame(width: 64, height: 64)

            Image(systemName: "doc.text.fill")
                .font(.system(size: 28))
                .foregroundColor(.blue)
        }
        .padding(.top, 8)
    }

    private var titleSection: some View {
        VStack(spacing: 8) {
            Text(AppStrings.Legal.policyUpdate)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)

            Text(AppStrings.Legal.pleaseReviewUpdated)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var documentsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            ForEach(legalService.pendingDocuments) { doc in
                VStack(alignment: .leading, spacing: 4) {
                    Button(action: {
                        if let url = URL(string: doc.contentUrl) {
                            safariURL = IdentifiableURL(url: url)
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "doc.text")
                                .font(.body)
                            Text(doc.title)
                                .font(.body)
                                .fontWeight(.medium)
                                .underline()
                        }
                        .foregroundColor(.blue)
                    }
                    .buttonStyle(PlainButtonStyle())

                    if let summary = doc.summaryOfChanges, !summary.isEmpty {
                        Text("\(AppStrings.Legal.changes) \(summary)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func errorCard(_ error: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            Text(error)
                .font(.subheadline)
                .foregroundColor(.red)
            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.red.opacity(0.1))
        )
    }

    private var acceptButton: some View {
        Button(action: {
            Task {
                await legalService.acceptPendingDocuments()
            }
        }) {
            HStack {
                if legalService.isAccepting {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(0.9)
                } else {
                    Text(AppStrings.Legal.acceptAndContinue)
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(legalService.isAccepting ? Color.blue.opacity(0.5) : Color.blue)
            )
            .foregroundColor(.white)
        }
        .disabled(legalService.isAccepting)
    }
}

// MARK: - Identifiable URL Wrapper

private struct IdentifiableURL: Identifiable {
    let id = UUID()
    let url: URL
}
