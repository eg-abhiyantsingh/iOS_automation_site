//
//  LegalAcceptanceService.swift
//  Egalvanic PZ
//
//  Manages legal document acceptance flows:
//  - Silent acceptance at sign-in (fire-and-forget)
//  - Post-login acceptance check and dialog state
//

import Foundation
import SwiftUI

@MainActor
class LegalAcceptanceService: ObservableObject {
    static let shared = LegalAcceptanceService()

    @Published var showLegalAcceptanceDialog = false
    @Published var pendingDocuments: [LegalDocumentDetail] = []
    @Published var isAccepting = false
    @Published var acceptanceError: String?

    private var isChecking = false

    private init() {}

    // MARK: - Sign-In Acceptance (Fire-and-Forget)

    /// Accept documents silently after sign-in. Errors are logged but never block login.
    func acceptDocumentsFromSignIn(documents: LegalDocumentsContainer) {
        Task {
            if let tc = documents.termsAndConditions {
                await acceptDocument(versionId: tc.id, method: "explicit_click")
            }
            if let pp = documents.privacyPolicy {
                await acceptDocument(versionId: pp.id, method: "explicit_click")
            }
        }
    }

    // MARK: - Post-Login Check

    /// Check for pending legal acceptance. Shows dialog if documents need acceptance.
    func checkPendingAcceptance() async {
        guard !isChecking else { return }
        isChecking = true
        defer { isChecking = false }

        guard let baseURL = getBaseURL(),
              let url = URL(string: baseURL + APIEndpoints.Legal.checkAcceptance) else {
            AppLogger.log(.debug, "[LegalAcceptanceService] No base URL available, skipping check", category: .api)
            return
        }

        AppLogger.log(.debug, "[LegalAcceptanceService] Checking: \(url.absoluteString)", category: .api)

        guard let request = buildAuthorizedRequest(url: url, method: "GET") else {
            AppLogger.log(.debug, "[LegalAcceptanceService] No auth token, skipping check", category: .api)
            return
        }

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            let httpResponse = response as? HTTPURLResponse

            // Log raw response
            let rawBody = String(data: data, encoding: .utf8) ?? "<non-utf8>"
            AppLogger.log(.debug, "[LegalAcceptanceService] Status: \(httpResponse?.statusCode ?? -1), Body: \(rawBody)", category: .api)

            guard let httpResponse, httpResponse.statusCode == 200 else {
                return
            }

            let checkResponse = try JSONDecoder().decode(LegalAcceptanceCheckResponse.self, from: data)
            AppLogger.log(.debug, "[LegalAcceptanceService] allAccepted: \(checkResponse.allAccepted), hasPending: \(checkResponse.pending != nil)", category: .api)

            if !checkResponse.allAccepted, let pending = checkResponse.pending {
                var docs: [LegalDocumentDetail] = []
                if let tc = pending.termsAndConditions { docs.append(tc) }
                if let pp = pending.privacyPolicy { docs.append(pp) }

                AppLogger.log(.debug, "[LegalAcceptanceService] Pending docs: \(docs.map { $0.title })", category: .api)

                if !docs.isEmpty {
                    pendingDocuments = docs
                    showLegalAcceptanceDialog = true
                }
            }
        } catch {
            AppLogger.log(.error, "[LegalAcceptanceService] Check failed: \(error.localizedDescription)", category: .api)
        }
    }

    // MARK: - Accept Pending Documents (Dialog Flow)

    /// Accept all pending documents. On success, dismisses dialog. On failure, shows error.
    func acceptPendingDocuments() async {
        isAccepting = true
        acceptanceError = nil

        var allSucceeded = true
        for doc in pendingDocuments {
            let success = await acceptDocument(versionId: doc.id, method: "modal_prompt")
            if !success { allSucceeded = false }
        }

        if allSucceeded {
            showLegalAcceptanceDialog = false
            pendingDocuments = []
        } else {
            acceptanceError = AppStrings.Legal.acceptanceFailed
        }

        isAccepting = false
    }

    // MARK: - Private

    @discardableResult
    private func acceptDocument(versionId: String, method: String) async -> Bool {
        guard let baseURL = getBaseURL(),
              let url = URL(string: baseURL + APIEndpoints.Legal.accept) else {
            return false
        }

        guard var request = buildAuthorizedRequest(url: url, method: "POST") else { return false }
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = LegalAcceptanceRequest(
            versionId: versionId,
            acceptedVia: "mobile_ios",
            acceptanceMethod: method,
            sessionContext: SessionContext(deviceId: getDeviceId())
        )

        do {
            request.httpBody = try JSONEncoder().encode(body)
            let (_, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                AppLogger.log(.error, "[LegalAcceptanceService] Accept failed for \(versionId): bad status", category: .api)
                return false
            }
            return true
        } catch {
            AppLogger.log(.error, "[LegalAcceptanceService] Accept failed for \(versionId): \(error.localizedDescription)", category: .api)
            return false
        }
    }

    private func getBaseURL() -> String? {
        CompanyConfigService.shared.getCurrentInvokeURL()
    }

    private func buildAuthorizedRequest(url: URL, method: String) -> URLRequest? {
        var request = URLRequest(url: url)
        request.httpMethod = method

        guard let token = AuthService.shared.getAccessToken() else { return nil }
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        if let subdomain = AuthService.shared.getCurrentSubdomain() {
            request.setValue(subdomain, forHTTPHeaderField: "X-Subdomain")
        }
        request.setValue(LanguageManager.shared.currentLanguage.rawValue, forHTTPHeaderField: "X-Language")

        return request
    }

    private func getDeviceId() -> String {
        let key = "com.yourapp.deviceId"
        if let existingId = UserDefaults.standard.string(forKey: key) {
            return existingId
        } else {
            let newId = UUID().uuidString
            UserDefaults.standard.set(newId, forKey: key)
            return newId
        }
    }
}
