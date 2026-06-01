//
//  EditSiteView.swift
//  Egalvanic PZ
//
//  Created by Claude on 10/12/25.
//

import SwiftUI
import SwiftData

struct EditSiteView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    @StateObject private var viewModel: EditSiteViewModel

    @State private var showAccountInfo = false
    @State private var showComplexityInfo = false
    @State private var showLaborUnionInfo = false
    @State private var showOfficeInfo = false

    var onSiteUpdated: (() -> Void)? = nil

    @MainActor
    init(site: SLDChoice, onSiteUpdated: (() -> Void)? = nil) {
        let repo = SiteRepositoryImpl()
        _viewModel = StateObject(
            wrappedValue: EditSiteViewModel(
                site: site,
                updateSiteUseCase: UpdateSiteUseCase(repository: repo),
                fetchAccountsUseCase: FetchAccountsUseCase(repository: repo),
                fetchLaborUnionsUseCase: FetchLaborUnionsUseCase(repository: repo),
                fetchOfficesUseCase: FetchOfficesUseCase(repository: repo),
                locationService: LocationService()
            )
        )
        self.onSiteUpdated = onSiteUpdated
    }

    var body: some View {
        NavigationStack {
            Form {
                siteInformationSection
                siteDetailsSection
                locationSection
                addressSection
                errorSection
            }
            .navigationTitle(AppStrings.Site.editSite)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) { dismiss() }
                        .disabled(viewModel.isUpdating)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.save) {
                        Task {
                            await viewModel.updateSite(modelContext: modelContext)
                        }
                    }
                    .disabled(!viewModel.isFormValid || viewModel.isUpdating)
                }
            }
            .overlay { loadingOverlay }
            .onAppear {
                viewModel.loadSiteData()
            }
            .task {
                await viewModel.loadPickerData()
            }
            .onChange(of: viewModel.didUpdateSite) { _, didUpdate in
                if didUpdate {
                    onSiteUpdated?()
                    dismiss()
                }
            }
            .sheet(isPresented: $viewModel.showAccountPicker) {
                SearchableItemPickerView(
                    title: AppStrings.Site.selectAccount,
                    items: viewModel.accounts,
                    selectedId: (viewModel.selectedAccount.map { $0.id.uuidString } ?? viewModel.site.account_id)?.lowercased(),
                    idToString: { $0.id.uuidString.lowercased() },
                    nameOf: { $0.name },
                    searchPrompt: AppStrings.Site.searchAccounts,
                    onSelect: { viewModel.selectedAccount = $0 }
                )
            }
            .sheet(isPresented: $viewModel.showComplexityPicker) {
                ComplexityLevelPickerView(
                    selectedLevel: viewModel.selectedComplexityLevel,
                    onSelect: { viewModel.selectedComplexityLevel = $0 }
                )
                .presentationDetents([.medium, .large])
            }
            .sheet(isPresented: $viewModel.showCountryPicker) {
                CountryPickerView(
                    countries: viewModel.countries,
                    selectedCode: viewModel.countryCode,
                    onSelect: { code in
                        viewModel.countryCode = code
                        viewModel.resetStateProvince()
                    }
                )
                .presentationDetents([.medium])
            }
            .sheet(isPresented: $viewModel.showStateProvincePicker) {
                StateProvincePickerView(
                    title: viewModel.stateProvinceLabel,
                    options: viewModel.stateProvinceOptions,
                    selectedOption: viewModel.stateProvince,
                    onSelect: { viewModel.stateProvince = $0 }
                )
            }
            .sheet(isPresented: $viewModel.showLaborUnionPicker) {
                SearchableItemPickerView(
                    title: AppStrings.Site.selectLaborUnion,
                    items: viewModel.laborUnions,
                    selectedId: (viewModel.selectedLaborUnion.map { $0.id } ?? viewModel.site.labor_union_id)?.lowercased(),
                    idToString: { $0.id.lowercased() },
                    nameOf: { $0.name },
                    searchPrompt: AppStrings.Site.searchLaborUnions,
                    allowClear: true,
                    onSelect: { viewModel.selectedLaborUnion = $0 }
                )
            }
            .sheet(isPresented: $viewModel.showOfficePicker) {
                SearchableItemPickerView(
                    title: AppStrings.Site.selectOffice,
                    items: viewModel.offices,
                    selectedId: (viewModel.selectedOffice.map { $0.id } ?? viewModel.site.office_id)?.lowercased(),
                    idToString: { $0.id.lowercased() },
                    nameOf: { officeDisplayLabel($0) },
                    searchPrompt: AppStrings.Site.searchOffices,
                    allowClear: true,
                    onSelect: { viewModel.selectedOffice = $0 }
                )
            }
        }
    }

    // MARK: - Sections

    private var siteInformationSection: some View {
        Section(AppStrings.Site.siteInformation) {
            TextField(AppStrings.Site.siteName, text: $viewModel.siteName)
                .textContentType(.name)
        }
    }

    private var siteDetailsSection: some View {
        Section(AppStrings.Site.siteDetails) {
            accountPickerRow
            officePickerRow
            complexityPickerRow
            laborUnionPickerRow
        }
    }

    private var officePickerRow: some View {
        Button { viewModel.showOfficePicker = true } label: {
            HStack {
                Text(AppStrings.Site.office)
                    .foregroundColor(.primary)
                Button { showOfficeInfo = true } label: {
                    Image(systemName: "info.circle")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .buttonStyle(.plain)
                Spacer()
                if viewModel.isLoadingOffices {
                    ProgressView()
                } else if let office = viewModel.selectedOffice {
                    Text(officeDisplayLabel(office))
                        .foregroundColor(.secondary)
                } else {
                    Text(AppStrings.Site.selectOffice)
                        .foregroundColor(.secondary)
                }
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .disabled(viewModel.isLoadingOffices)
        .alert(AppStrings.Site.office, isPresented: $showOfficeInfo) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Site.officeInfo)
        }
    }

    private var accountPickerRow: some View {
        Button { viewModel.showAccountPicker = true } label: {
            HStack {
                Text(AppStrings.Site.account)
                    .foregroundColor(.primary)
                Button { showAccountInfo = true } label: {
                    Image(systemName: "info.circle")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .buttonStyle(.plain)
                Spacer()
                if viewModel.isLoadingAccounts {
                    ProgressView()
                } else if let account = viewModel.selectedAccount {
                    Text(account.name)
                        .foregroundColor(.secondary)
                } else {
                    Text(AppStrings.Site.selectAccount)
                        .foregroundColor(.secondary)
                }
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .disabled(viewModel.isLoadingAccounts)
        .alert(AppStrings.Site.account, isPresented: $showAccountInfo) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Site.accountInfo)
        }
    }

    private var complexityPickerRow: some View {
        Button { viewModel.showComplexityPicker = true } label: {
            HStack {
                Text(AppStrings.Site.complexityLevel)
                    .foregroundColor(.primary)
                Button { showComplexityInfo = true } label: {
                    Image(systemName: "info.circle")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .buttonStyle(.plain)
                Spacer()
                if let level = viewModel.selectedComplexityLevel {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(level.color)
                            .frame(width: 10, height: 10)
                        Text("\(level.displayName) (\(level.multiplierLabel))")
                            .foregroundColor(.secondary)
                    }
                } else {
                    Text(AppStrings.Site.selectComplexityLevel)
                        .foregroundColor(.secondary)
                }
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .alert(AppStrings.Site.complexityLevel, isPresented: $showComplexityInfo) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Site.complexityTooltip)
        }
    }

    private var laborUnionPickerRow: some View {
        Button { viewModel.showLaborUnionPicker = true } label: {
            HStack {
                Text(AppStrings.Site.laborUnion)
                    .foregroundColor(.primary)
                Button { showLaborUnionInfo = true } label: {
                    Image(systemName: "info.circle")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .buttonStyle(.plain)
                Spacer()
                if viewModel.isLoadingLaborUnions {
                    ProgressView()
                } else if let union = viewModel.selectedLaborUnion {
                    Text(union.name)
                        .foregroundColor(.secondary)
                } else {
                    Text(AppStrings.Site.noneOptional)
                        .foregroundColor(.secondary)
                }
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .disabled(viewModel.isLoadingLaborUnions)
        .alert(AppStrings.Site.laborUnion, isPresented: $showLaborUnionInfo) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Site.laborUnionInfo)
        }
    }

    private var locationSection: some View {
        Section {
            if viewModel.locationService.authorizationStatus == .authorizedWhenInUse ||
               viewModel.locationService.authorizationStatus == .authorizedAlways {
                Button {
                    Task { await viewModel.fetchCurrentLocation() }
                } label: {
                    HStack {
                        Image(systemName: "location.fill")
                        Text(AppStrings.Site.useCurrentLocation)
                        if viewModel.locationService.isLoading {
                            Spacer()
                            ProgressView()
                        }
                    }
                }
                .disabled(viewModel.locationService.isLoading)
            } else {
                Button { viewModel.locationService.requestPermission() } label: {
                    HStack {
                        Image(systemName: "location.slash")
                        Text(AppStrings.Site.setLocation)
                    }
                }
            }
        } header: {
            Text(AppStrings.Site.location)
        } footer: {
            Text(AppStrings.Site.locationHelpText)
        }
    }

    private var addressSection: some View {
        Section(AppStrings.Site.address) {
            TextField(AppStrings.Site.addressLine1, text: $viewModel.addressLine1)
                .textContentType(.streetAddressLine1)

            TextField(AppStrings.Site.addressLine2Optional, text: $viewModel.addressLine2)
                .textContentType(.streetAddressLine2)

            TextField(AppStrings.Site.city, text: $viewModel.city)
                .textContentType(.addressCity)

            Button { viewModel.showCountryPicker = true } label: {
                HStack {
                    Text(AppStrings.Site.country)
                        .foregroundColor(.primary)
                    Spacer()
                    HStack(spacing: 6) {
                        Text(viewModel.countryCode.countryFlag)
                        Text(viewModel.countries.first(where: { $0.0 == viewModel.countryCode })?.1 ?? viewModel.countryCode)
                            .foregroundColor(.secondary)
                    }
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Button { viewModel.showStateProvincePicker = true } label: {
                HStack {
                    Text(viewModel.stateProvinceLabel)
                        .foregroundColor(.primary)
                    Spacer()
                    if viewModel.stateProvince.isEmpty {
                        Text(AppStrings.Site.selectStateProvince(viewModel.stateProvinceLabel))
                            .foregroundColor(.secondary)
                    } else {
                        Text(viewModel.stateProvince)
                            .foregroundColor(.secondary)
                    }
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            TextField(viewModel.postalCodeLabel, text: $viewModel.postalCode)
                .textContentType(.postalCode)
                .keyboardType(viewModel.countryCode == "US" || viewModel.countryCode == "AU" ? .numberPad : .default)
        }
    }

    @ViewBuilder
    private var errorSection: some View {
        if let error = viewModel.errorMessage {
            Section {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
            }
        }
    }

    @ViewBuilder
    private var loadingOverlay: some View {
        if viewModel.isUpdating {
            ZStack {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text(AppStrings.Site.updatingSite)
                        .font(.headline)
                }
                .padding(30)
                .background(Color(.systemBackground))
                .cornerRadius(12)
            }
        }
    }
}
