//
//  FetchLaborUnionsUseCase.swift
//  Egalvanic PZ
//

import Foundation

struct FetchLaborUnionsUseCase {
    private let repository: SiteRepository

    init(repository: SiteRepository) {
        self.repository = repository
    }

    func execute() async throws -> [LaborUnionDTO] {
        return try await repository.fetchLaborUnions()
    }
}
