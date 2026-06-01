//
//  EditSiteViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
final class EditSiteViewModel: ObservableObject {

    // MARK: - Dependencies

    private let updateSiteUseCase: UpdateSiteUseCase
    private let fetchAccountsUseCase: FetchAccountsUseCase
    private let fetchLaborUnionsUseCase: FetchLaborUnionsUseCase
    private let fetchOfficesUseCase: FetchOfficesUseCase
    let locationService: LocationService

    // MARK: - Configuration

    let site: SLDChoice

    // MARK: - Init

    init(
        site: SLDChoice,
        updateSiteUseCase: UpdateSiteUseCase,
        fetchAccountsUseCase: FetchAccountsUseCase,
        fetchLaborUnionsUseCase: FetchLaborUnionsUseCase,
        fetchOfficesUseCase: FetchOfficesUseCase,
        locationService: LocationService
    ) {
        self.site = site
        self.updateSiteUseCase = updateSiteUseCase
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
    @Published var isUpdating = false
    @Published var errorMessage: String?
    @Published var didUpdateSite = false

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

    // MARK: - Load Existing Data

    func loadSiteData() {
        siteName = site.name
        addressLine1 = site.address_line_1 ?? ""
        addressLine2 = site.address_line_2 ?? ""
        city = site.city ?? ""
        postalCode = site.postal_code ?? ""
        // Set countryCode BEFORE stateProvince to prevent onChange from wiping it
        countryCode = site.country_code ?? "US"
        stateProvince = site.state_province ?? ""

        // Set complexity level from local data (no API needed)
        if let level = site.complexity_level {
            selectedComplexityLevel = ComplexityLevel(rawValue: level)
        }

        // Match account, labor union, office against already-fetched lists
        matchPickers()
    }

    /// Matches site.account_id / site.labor_union_id / site.office_id against the fetched lists.
    /// Called after loadSiteData() and again after loadPickerData() completes.
    private func matchPickers() {
        if let accountId = site.account_id, selectedAccount == nil {
            selectedAccount = accounts.first(where: { $0.id.uuidString.lowercased() == accountId.lowercased() })
        }
        if let laborUnionId = site.labor_union_id, selectedLaborUnion == nil {
            selectedLaborUnion = laborUnions.first(where: { $0.id.lowercased() == laborUnionId.lowercased() })
        }
        if let officeId = site.office_id, selectedOffice == nil {
            selectedOffice = offices.first(where: { $0.id.lowercased() == officeId.lowercased() })
        }
    }

    // MARK: - Data Loading

    func loadPickerData() async {
        async let accountsTask: () = loadAccounts()
        async let unionsTask: () = loadLaborUnions()
        async let officesTask: () = loadOffices()
        _ = await (accountsTask, unionsTask, officesTask)

        // Re-match after lists are fetched from API
        matchPickers()
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

    // MARK: - Update Site

    func updateSite(modelContext: ModelContext) async {
        isUpdating = true
        errorMessage = nil
        defer { isUpdating = false }

        do {
            let input = buildInput()
            let updatedSLD = try await updateSiteUseCase.execute(sldId: site.id, input: input)

            AppLogger.log(.info, "Successfully updated SLD: \(updatedSLD.id)", category: .ui)

            _ = try await SLDService.shared.fetchAvailableSLDs(for: AppStateManager.shared.userId, modelContext: modelContext)

            // ZP-2061: if the user just edited the *currently-active* site, the cached
            // office_language may have changed. Re-evaluate the UI language. Editing a
            // non-active site is a no-op here — its office language will apply when the
            // user next selects it.
            if AppStateManager.shared.activeSLDId == site.id {
                SiteLanguageController.shared.onActiveSiteOfficeChanged(siteId: site.id, modelContext: modelContext)
            }

            didUpdateSite = true

        } catch {
            handleError(error)
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
