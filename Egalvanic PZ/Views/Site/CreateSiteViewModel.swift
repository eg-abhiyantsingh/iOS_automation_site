//
//  CreateSiteViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
final class CreateSiteViewModel: ObservableObject {

    // MARK: - Dependencies

    private let createSiteUseCase: CreateSiteUseCase
    private let fetchAccountsUseCase: FetchAccountsUseCase
    private let fetchLaborUnionsUseCase: FetchLaborUnionsUseCase
    private let fetchOfficesUseCase: FetchOfficesUseCase
    let locationService: LocationService

    // MARK: - Init

    init(
        createSiteUseCase: CreateSiteUseCase,
        fetchAccountsUseCase: FetchAccountsUseCase,
        fetchLaborUnionsUseCase: FetchLaborUnionsUseCase,
        fetchOfficesUseCase: FetchOfficesUseCase,
        locationService: LocationService
    ) {
        self.createSiteUseCase = createSiteUseCase
        self.fetchAccountsUseCase = fetchAccountsUseCase
        self.fetchLaborUnionsUseCase = fetchLaborUnionsUseCase
        self.fetchOfficesUseCase = fetchOfficesUseCase
        self.locationService = locationService
    }

    // MARK: - Form State

    @Published var siteName = ""
    @Published var addressLine1 = ""
    @Published var addressLine2 = ""
    @Published var city = ""
    @Published var stateProvince = ""
    @Published var postalCode = ""
    @Published var countryCode = "US"

    // MARK: - Picker State

    @Published var accounts: [AccountDTO] = []
    @Published var laborUnions: [LaborUnionDTO] = []
    @Published var offices: [OfficeDTO] = []
    @Published var isLoadingAccounts = false
    @Published var isLoadingLaborUnions = false
    @Published var isLoadingOffices = false
    @Published var selectedAccount: AccountDTO? = nil
    @Published var selectedComplexityLevel: ComplexityLevel? = nil
    @Published var selectedLaborUnion: LaborUnionDTO? = nil
    @Published var selectedOffice: OfficeDTO? = nil

    // MARK: - UI State

    @Published var showAccountPicker = false
    @Published var showComplexityPicker = false
    @Published var showCountryPicker = false
    @Published var showStateProvincePicker = false
    @Published var showLaborUnionPicker = false
    @Published var showOfficePicker = false
    @Published var isCreating = false
    @Published var errorMessage: String?
    @Published var didCreateSite = false

    // MARK: - Static Data

    let countries = [
        ("US", AppStrings.Site.unitedStates),
        ("CA", AppStrings.Site.canada),
        ("GB", AppStrings.Site.unitedKingdom),
        ("AU", AppStrings.Site.australia)
    ]

    // MARK: - Computed Properties

    var isFormValid: Bool {
        !siteName.trimmed.isEmpty
        && selectedAccount != nil
        && selectedComplexityLevel != nil
        && !addressLine1.trimmed.isEmpty
        && !city.trimmed.isEmpty
        && !stateProvince.isEmpty
        && !postalCode.trimmed.isEmpty
    }

    var stateProvinceOptions: [String] {
        switch countryCode {
        case "US":
            return ["AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
                    "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
                    "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
                    "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
                    "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"]
        case "CA":
            return ["AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT"]
        case "GB":
            return ["England", "Scotland", "Wales", "Northern Ireland"]
        case "AU":
            return ["NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT"]
        default:
            return []
        }
    }

    var stateProvinceLabel: String {
        switch countryCode {
        case "US", "AU": return AppStrings.Site.state
        case "CA": return AppStrings.Site.province
        case "GB": return AppStrings.Site.region
        default: return AppStrings.Site.stateProvince
        }
    }

    var postalCodeLabel: String {
        countryCode == "US" ? AppStrings.Site.zipCode : AppStrings.Site.postalCode
    }

    var postalCodePlaceholder: String {
        switch countryCode {
        case "US": return "12345"
        case "CA": return "A1A 1A1"
        case "GB": return "SW1A 1AA"
        case "AU": return "2000"
        default: return ""
        }
    }

    // MARK: - Lifecycle

    func onAppear() async {
        await loadInitialData()
    }

    // MARK: - Data Loading

    func loadInitialData() async {
        async let accountsTask: () = loadAccounts()
        async let unionsTask: () = loadLaborUnions()
        async let officesTask: () = loadOffices()
        _ = await (accountsTask, unionsTask, officesTask)
    }

    private func loadAccounts() async {
        isLoadingAccounts = true
        defer { isLoadingAccounts = false }
        do {
            accounts = try await fetchAccountsUseCase.execute()
        } catch {
            handleError(error)
        }
    }

    private func loadLaborUnions() async {
        isLoadingLaborUnions = true
        defer { isLoadingLaborUnions = false }
        do {
            laborUnions = try await fetchLaborUnionsUseCase.execute()
        } catch {
            handleError(error)
        }
    }

    private func loadOffices() async {
        isLoadingOffices = true
        defer { isLoadingOffices = false }
        do {
            offices = try await fetchOfficesUseCase.execute()
        } catch {
            handleError(error)
        }
    }

    // MARK: - Location

    func fetchCurrentLocation() async {
        do {
            let components = try await locationService.getCurrentAddressComponents()
            addressLine1 = components.addressLine1 ?? addressLine1
            city = components.city ?? city
            stateProvince = components.stateProvince ?? stateProvince
            postalCode = components.postalCode ?? postalCode
            countryCode = components.countryCode ?? countryCode
        } catch {
            handleError(error)
        }
    }

    func resetStateProvince() {
        stateProvince = ""
    }

    // MARK: - Create Site

    func createSite(modelContext: ModelContext) async -> (UUID, String)? {
        guard isFormValid else { return nil }

        isCreating = true
        errorMessage = nil
        defer { isCreating = false }

        do {
            let input = buildInput()
            let createdSLD = try await createSiteUseCase.execute(input: input)

            AppLogger.log(.info, "Successfully created SLD: \(createdSLD.id)", category: .ui)

            _ = try await SLDService.shared.fetchAvailableSLDs(for: AppStateManager.shared.userId, modelContext: modelContext)

            didCreateSite = true
            return (createdSLD.id, createdSLD.name)

        } catch {
            handleError(error)
            return nil
        }
    }

    // MARK: - Helpers

    private func buildInput() -> CreateSiteInput {
        CreateSiteInput(
            siteName: siteName,
            addressLine1: addressLine1,
            addressLine2: addressLine2,
            city: city,
            stateProvince: stateProvince,
            postalCode: postalCode,
            countryCode: countryCode,
            accountId: selectedAccount?.id.uuidString.lowercased(),
            complexityLevel: selectedComplexityLevel?.rawValue,
            laborUnionId: selectedLaborUnion?.id,
            officeId: selectedOffice?.id,
            latitude: locationService.currentLocation?.coordinate.latitude,
            longitude: locationService.currentLocation?.coordinate.longitude
        )
    }

    private func handleError(_ error: Error) {
        guard !AuthError.isAuthError(error) else { return }
        errorMessage = error.localizedDescription
    }
}
