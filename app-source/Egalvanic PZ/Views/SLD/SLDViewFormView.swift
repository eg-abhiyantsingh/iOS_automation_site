//
//  SLDViewFormView.swift
//  Egalvanic PZ
//
//  Form view for creating and editing SLD views
//

import SwiftUI
import SwiftData

struct SLDViewFormView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    let sld: SLDV2
    let existingView: SLDViewV2?

    @State private var name: String = ""
    @State private var viewDescription: String = ""
    @State private var isSaving = false
    @State private var errorMessage: String?

    var isEditing: Bool { existingView != nil }

    var body: some View {
        NavigationView {
            Form {
                Section(AppStrings.SLD.viewDetails) {
                    TextField(AppStrings.Common.name, text: $name)
                        .textInputAutocapitalization(.words)

                    TextField(AppStrings.SLD.descriptionOptional, text: $viewDescription, axis: .vertical)
                        .lineLimit(3...6)
                }

                if let error = errorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle(isEditing ? AppStrings.SLD.editView : AppStrings.SLD.newView)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? AppStrings.Common.save : AppStrings.Common.create) {
                        Task {
                            await saveView()
                        }
                    }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSaving)
                }
            }
            .interactiveDismissDisabled(isSaving)
        }
        .onAppear {
            if let view = existingView {
                name = view.name
                viewDescription = view.viewDescription ?? ""
            }
        }
    }

    private func saveView() async {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = AppStrings.SLD.nameIsRequired
            return
        }

        isSaving = true
        errorMessage = nil

        defer { isSaving = false }

        if isEditing, let view = existingView {
            // Update existing view
            view.name = trimmedName
            view.viewDescription = viewDescription.isEmpty ? nil : viewDescription
            view.modified_at = Date()

            do {
                try modelContext.save()
            } catch {
                errorMessage = "Failed to save: \(error.localizedDescription)"
                return
            }

            // Sync with server
            if networkState.mode == .online {
                do {
                    _ = try await APIClient.shared.updateSLDView(view)
                } catch {
                    // Queue for later sync if online fails
                    let op = SyncOp(target: .sldView, operation: .update, sldView: view)
                    networkState.enqueue(op)
                }
            } else {
                // Offline - queue for sync
                let op = SyncOp(target: .sldView, operation: .update, sldView: view)
                networkState.enqueue(op)
            }
        } else {
            // Create new view
            let newView = SLDViewV2(
                id: UUID(),
                sld_id: sld.id,
                name: trimmedName,
                viewDescription: viewDescription.isEmpty ? nil : viewDescription,
                created_by: AppStateManager.shared.userId,
                view_type: "custom",
                is_default: false,
                is_deleted: false,
                created_at: Date(),
                modified_at: nil
            )

            modelContext.insert(newView)

            do {
                try modelContext.save()
            } catch {
                errorMessage = "Failed to save: \(error.localizedDescription)"
                return
            }

            // Sync with server
            if networkState.mode == .online {
                do {
                    _ = try await APIClient.shared.createSLDView(sldView: newView)
                } catch {
                    // Queue for later sync if online fails
                    let op = SyncOp(target: .sldView, operation: .create, sldView: newView)
                    networkState.enqueue(op)
                }
            } else {
                // Offline - queue for sync
                let op = SyncOp(target: .sldView, operation: .create, sldView: newView)
                networkState.enqueue(op)
            }
        }

        dismiss()
    }
}
