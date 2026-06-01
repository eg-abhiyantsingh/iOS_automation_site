//
//  FetchAccountsUseCase.swift
//  Egalvanic PZ
//

import Foundation

struct FetchAccountsUseCase {
    private let repository: SiteRepository

    init(repository: SiteRepository) {
        self.repository = repository
    }

    func execute() async throws -> [AccountDTO] {
        let companyId = try await AuthService.shared.requireCompanyId()
        return try await repository.fetchAccounts(companyId: companyId)
    }
}
