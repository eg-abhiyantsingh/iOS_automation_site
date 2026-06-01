//
//  CreateSiteUseCase.swift
//  Egalvanic PZ
//

import Foundation

struct CreateSiteUseCase {
    private let repository: SiteRepository

    init(repository: SiteRepository) {
        self.repository = repository
    }

    func execute(input: CreateSiteInput) async throws -> SLDChoiceDTO {
        let companyId = try await AuthService.shared.requireCompanyId()
        let formattedAddress = AddressFormatter.format(input)

        return try await repository.createSite(
            input: input,
            companyId: companyId,
            formattedAddress: formattedAddress
        )
    }
}
