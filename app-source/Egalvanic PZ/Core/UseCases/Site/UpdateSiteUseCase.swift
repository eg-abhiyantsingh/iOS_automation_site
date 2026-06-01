//
//  UpdateSiteUseCase.swift
//  Egalvanic PZ
//

import Foundation

struct UpdateSiteUseCase {
    private let repository: SiteRepository

    init(repository: SiteRepository) {
        self.repository = repository
    }

    func execute(sldId: UUID, input: CreateSiteInput) async throws -> SLDChoiceDTO {
        let formattedAddress = AddressFormatter.format(input)

        return try await repository.updateSite(
            sldId: sldId,
            input: input,
            formattedAddress: formattedAddress
        )
    }
}
