import Foundation
import SwiftData

class FormInstanceService {
    static let shared = FormInstanceService()

    private init() {}

    // MARK: - Update Form Instance

    /// Updates a FormInstance with submission data, handling both online and offline modes
    static func updateFormInstance(
        _ formInstance: FormInstance,
        submission: String?,
        submitted: Bool,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        do {
            // Update the form instance properties
            formInstance.form_submission = submission
            formInstance.submitted = submitted
            formInstance.modified_at = Date()

            // Save locally first
            try modelContext.save()
            AppLogger.log(.info, "Updated FormInstance locally", category: .form)

            // Handle server sync based on network mode
            if NetworkState.shared.canDirectSync {
                // Sync to server asynchronously
                Task {
                    do {
                        let dto = formInstance.toDTO()
                        _ = try await APIClient.shared.updateFormInstance(id: formInstance.id, dto: dto)
                        AppLogger.log(.info, "FormInstance \(formInstance.id) synced to server", category: .form)

                        await MainActor.run {
                            // Update any sync timestamps if needed
                            try? modelContext.save()
                            onCompletion?(true, "Form submission saved successfully")
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to sync FormInstance to server: \(error)", category: .form)

                        await MainActor.run {
                            // Queue for later sync
                            let op = SyncOp(
                                target: .formInstance,
                                operation: .update,
                                formInstance: formInstance
                            )
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, "Form saved locally. Will sync when online.")
                        }
                    }
                }
            } else {
                // Offline mode - queue for sync
                DispatchQueue.main.async {
                    let op = SyncOp(
                        target: .formInstance,
                        operation: .update,
                        formInstance: formInstance
                    )
                    NetworkState.shared.enqueue(op)
                    AppLogger.log(.debug, "FormInstance update queued for sync when online", category: .form)
                    onCompletion?(true, "Form saved locally. Will sync when online.")
                }
            }

        } catch {
            AppLogger.log(.error, "Failed to save FormInstance: \(error)", category: .form)
            onCompletion?(false, "Failed to save form: \(error.localizedDescription)")
        }
    }

    // MARK: - Create Form Instance

    /// Creates a new FormInstance locally and syncs if online
    static func createFormInstance(
        formMasterId: UUID,
        submission: String? = nil,
        submitted: Bool = false,
        linkedTasks: [UserTask] = [],
        linkedNodes: [NodeV2] = [],
        modelContext: ModelContext,
        onCompletion: ((Bool, FormInstance?, String?) -> Void)? = nil
    ) {
        // Find the form master
        let descriptor = FetchDescriptor<UserTaskForm>(
            predicate: #Predicate<UserTaskForm> { form in
                form.id == formMasterId
            }
        )

        guard let formMaster = try? modelContext.fetch(descriptor).first else {
            AppLogger.log(.error, "Form master not found: \(formMasterId)", category: .form)
            onCompletion?(false, nil, "Form template not found")
            return
        }

        // Create the form instance
        let formInstance = FormInstance(
            id: UUID(),
            form_master_id: formMasterId,
            created_at: Date(),
            modified_at: nil,
            form_submission: submission,
            submitted: submitted,
            is_deleted: false,
            formMaster: formMaster,
            linkedTasks: linkedTasks,
            linkedNodes: linkedNodes
        )

        // Insert into context
        modelContext.insert(formInstance)

        do {
            try modelContext.save()
            AppLogger.log(.info, "Created FormInstance locally", category: .form)

            // Handle server sync
            if NetworkState.shared.canDirectSync {
                Task {
                    do {
                        let dto = formInstance.toDTO()
                        _ = try await APIClient.shared.createFormInstance(dto)
                        AppLogger.log(.info, "FormInstance created on server: \(formInstance.id)", category: .form)

                        await MainActor.run {
                            onCompletion?(true, formInstance, nil)
                        }
                    } catch {
                        AppLogger.log(.error, "Failed to create FormInstance on server: \(error)", category: .form)

                        await MainActor.run {
                            // Queue for later sync
                            let op = SyncOp(
                                target: .formInstance,
                                operation: .create,
                                formInstance: formInstance
                            )
                            NetworkState.shared.enqueue(op)
                            onCompletion?(true, formInstance, "Created locally. Will sync when online.")
                        }
                    }
                }
            } else {
                // Offline mode - queue for sync
                DispatchQueue.main.async {
                    let op = SyncOp(
                        target: .formInstance,
                        operation: .create,
                        formInstance: formInstance
                    )
                    NetworkState.shared.enqueue(op)
                    AppLogger.log(.debug, "FormInstance creation queued for sync", category: .form)
                    onCompletion?(true, formInstance, "Created locally. Will sync when online.")
                }
            }

        } catch {
            AppLogger.log(.error, "Failed to save new FormInstance: \(error)", category: .form)
            onCompletion?(false, nil, "Failed to create form instance: \(error.localizedDescription)")
        }
    }

    // MARK: - Auto-save Support

    /// Updates only the submission field (for auto-save/draft functionality)
    static func updateFormInstanceDraft(
        _ formInstance: FormInstance,
        submission: String?,
        modelContext: ModelContext
    ) {
        formInstance.form_submission = submission
        formInstance.modified_at = Date()
        // Don't set submitted flag for drafts

        do {
            try modelContext.save()
            AppLogger.log(.debug, "Draft saved locally for FormInstance", category: .form)

            // For drafts, we might want to debounce server syncs
            // For now, just queue if offline
            if NetworkState.shared.mode == .offline {
                DispatchQueue.main.async {
                    let op = SyncOp(
                        target: .formInstance,
                        operation: .update,
                        formInstance: formInstance
                    )
                    NetworkState.shared.enqueue(op)
                }
            }
        } catch {
            AppLogger.log(.notice, "Failed to save draft: \(error)", category: .form)
        }
    }
}