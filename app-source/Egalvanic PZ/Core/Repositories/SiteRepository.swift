//
//  SiteRepository.swift
//  Egalvanic PZ
//

import Foundation
import CoreLocation

// MARK: - Protocol

protocol SiteRepository {
    func createSite(input: CreateSiteInput, companyId: UUID, formattedAddress: String?) async throws -> SLDChoiceDTO
    func updateSite(sldId: UUID, input: CreateSiteInput, formattedAddress: String?) async throws -> SLDChoiceDTO
    func fetchAccounts(companyId: UUID) async throws -> [AccountDTO]
    func fetchLaborUnions() async throws -> [LaborUnionDTO]
    func fetchOffices() async throws -> [OfficeDTO]
}

// MARK: - Implementation

final class SiteRepositoryImpl: SiteRepository {

    func createSite(input: CreateSiteInput, companyId: UUID, formattedAddress: String?) async throws -> SLDChoiceDTO {
        return try await APIClient.shared.createSLD(
            name: input.siteName.trimmed,
            companyId: companyId,
            addressLine1: input.addressLine1.nilIfEmpty,
            addressLine2: input.addressLine2.nilIfEmpty,
            city: input.city.nilIfEmpty,
            stateProvince: input.stateProvince.nilIfEmpty,
            postalCode: input.postalCode.nilIfEmpty,
            countryCode: input.countryCode,
            addressFormatted: formattedAddress,
            latitude: input.latitude,
            longitude: input.longitude,
            accountId: input.accountId,
            complexityLevel: input.complexityLevel,
            laborUnionId: input.laborUnionId,
            officeId: input.officeId
        )
    }

    func updateSite(sldId: UUID, input: CreateSiteInput, formattedAddress: String?) async throws -> SLDChoiceDTO {
        return try await APIClient.shared.updateSLD(
            sldId: sldId,
            name: input.siteName.trimmed,
            addressLine1: input.addressLine1.nilIfEmpty,
            addressLine2: input.addressLine2.nilIfEmpty,
            city: input.city.nilIfEmpty,
            stateProvince: input.stateProvince.nilIfEmpty,
            postalCode: input.postalCode.nilIfEmpty,
            countryCode: input.countryCode,
            addressFormatted: formattedAddress,
            latitude: input.latitude,
            longitude: input.longitude,
            accountId: input.accountId,
            complexityLevel: input.complexityLevel,
            laborUnionId: input.laborUnionId,
            officeId: input.officeId
        )
    }

    func fetchAccounts(companyId: UUID) async throws -> [AccountDTO] {
        return try await APIClient.shared.fetchAccounts(companyId: companyId)
    }

    func fetchLaborUnions() async throws -> [LaborUnionDTO] {
        return try await APIClient.shared.fetchLaborUnions()
    }

    func fetchOffices() async throws -> [OfficeDTO] {
        return try await APIClient.shared.fetchOffices()
    }
}
