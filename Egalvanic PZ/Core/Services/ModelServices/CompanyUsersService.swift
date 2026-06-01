//
//  CompanyUsersService.swift
//  Egalvanic PZ
//

import Foundation

@MainActor
class CompanyUsersService: ObservableObject {
    static let shared = CompanyUsersService()

    @Published var users: [CompanyUser] = []
    @Published var isLoading = false

    private var cachedCompanyId: String?

    func fetchUsers(companyId: String) async {
        // Return cached if same company
        if cachedCompanyId == companyId && !users.isEmpty {
            return
        }

        isLoading = true
        do {
            let fetched = try await APIClient.shared.fetchCompanyUsers(companyId: companyId)
            users = fetched.filter { $0.email != nil }
            cachedCompanyId = companyId
        } catch {
            AppLogger.log(.error, "Failed to fetch company users: \(error)", category: .sync)
        }
        isLoading = false
    }

    func clearCache() {
        users = []
        cachedCompanyId = nil
    }
}
