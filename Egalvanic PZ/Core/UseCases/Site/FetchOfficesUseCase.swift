//
//  FetchOfficesUseCase.swift
//  Egalvanic PZ
//

import Foundation

struct FetchOfficesUseCase {
    private let repository: SiteRepository

    init(repository: SiteRepository) {
        self.repository = repository
    }

    func execute() async throws -> [OfficeDTO] {
        return try await repository.fetchOffices()
    }
}
