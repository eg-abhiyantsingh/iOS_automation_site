//
//  EntityCreationViewModel.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

@MainActor
class EntityCreationViewModel: ObservableObject {

    // MARK: - UI State

    @Published var showingValidationError = false
    @Published var validationErrorMessage = ""
    @Published var focusedField: String?
    @Published var isCreating = false

    // MARK: - Actions

    /// Validates and creates the entity. Returns `true` if creation succeeded (caller should dismiss).
    func handleCreate<Config: EntityCreationConfiguration>(
        configuration: Config,
        storage: FieldValueStorage,
        modelContext: ModelContext
    ) -> Bool {
        guard !isCreating else { return false }
        let validationResult = configuration.validateForm(storage: storage)

        switch validationResult {
        case .valid:
            isCreating = true
            if configuration.onCreate(storage: storage, modelContext: modelContext) != nil {
                return true
            } else {
                isCreating = false
                return false
            }
        case .invalid(let message):
            validationErrorMessage = message
            showingValidationError = true
            return false
        }
    }
}
