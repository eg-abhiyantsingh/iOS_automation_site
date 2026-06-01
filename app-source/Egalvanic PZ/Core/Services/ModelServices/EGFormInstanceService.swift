//
//  EGFormInstanceService.swift
//  Egalvanic PZ
//
//  ZP-1723: create/update/draft for EG form instances. Mirrors
//  FormInstanceService but talks to /eg-form-instance/ endpoints and
//  uses the EGFormInstanceCreateBody / UpdateBody wire shapes (which
//  carry form_submission as a parsed JSON value rather than a string,
//  matching how the backend's extract_and_upload_media walks the tree).
//
import Foundation
import SwiftData

final class EGFormInstanceService {
    static let shared = EGFormInstanceService()
    private init() {}

    // MARK: - Create

    /// Create a new EGFormInstance locally and sync (or queue) to server.
    /// `taskId` / `nodeId` link the new instance at create time via the
    /// backend's POST body shape. Multi-node linking happens via subsequent
    /// updates carrying `node_ids` instead.
    static func createEGFormInstance(
        egFormId: UUID,
        submission: String? = nil,
        submitted: Bool = false,
        taskId: UUID? = nil,
        nodeId: UUID? = nil,
        linkedTasks: [UserTask] = [],
        linkedNodes: [NodeV2] = [],
        egForm: EGForm? = nil,
        modelContext: ModelContext,
        onCompletion: ((Bool, EGFormInstance?, String?) -> Void)? = nil
    ) {
        let resolvedForm: EGForm? = egForm ?? {
            let descriptor = FetchDescriptor<EGForm>(
                predicate: #Predicate<EGForm> { f in f.id == egFormId }
            )
            return try? modelContext.fetch(descriptor).first
        }()

        let instance = EGFormInstance(
            id: UUID(),
            eg_form_id: egFormId,
            form_submission: submission,
            submitted: submitted,
            is_deleted: false,
            created_at: Date(),
            modified_at: nil,
            egForm: resolvedForm,
            linkedTasks: linkedTasks,
            linkedNodes: linkedNodes
        )
        modelContext.insert(instance)

        do {
            try modelContext.save()
            AppLogger.log(.info, "Created EGFormInstance locally", category: .form)
        } catch {
            AppLogger.log(.error, "Failed to save new EGFormInstance: \(error)", category: .form)
            onCompletion?(false, nil, "Failed to create form instance: \(error.localizedDescription)")
            return
        }

        let body = EGFormInstanceCreateBody(
            id: instance.id.uuidString,
            eg_form_id: egFormId.uuidString,
            form_submission: anyCodable(from: submission),
            submitted: submitted,
            task_id: taskId?.uuidString,
            node_id: nodeId?.uuidString
        )

        // ZP-2425: capture task_id / node_id at enqueue time. The canonical
        // task↔instance linkage is stored on the task side
        // (`task.linkedEGFormInstances`) and the inverse `inst.linkedTasks`
        // is unreliable (no explicit `@Relationship(inverse:)` annotation,
        // see TaskDetailViewV2.swift:738-743/869). Without this, the queued
        // create's snapshot would ship `task_id: null` and the server has
        // no way to repair the linkage later — PUT body has no `task_id`.
        var linkExtraData: [String: Any] = [:]
        if let t = taskId?.uuidString { linkExtraData["task_id"] = t }
        if let n = nodeId?.uuidString { linkExtraData["node_id"] = n }
        let createExtraData: [String: Any]? = linkExtraData.isEmpty ? nil : linkExtraData

        if NetworkState.shared.canDirectSync {
            Task {
                do {
                    _ = try await APIClient.shared.createEGFormInstance(body)
                    AppLogger.log(.info, "EGFormInstance \(instance.id) created on server", category: .form)
                    await MainActor.run { onCompletion?(true, instance, nil) }
                } catch {
                    AppLogger.log(.error, "Failed to create EGFormInstance on server: \(error)", category: .form)
                    await MainActor.run {
                        let op = SyncOp(target: .egFormInstance, operation: .create, egFormInstance: instance, extraData: createExtraData)
                        NetworkState.shared.enqueue(op)
                        onCompletion?(true, instance, "Created locally. Will sync when online.")
                    }
                }
            }
        } else {
            DispatchQueue.main.async {
                let op = SyncOp(target: .egFormInstance, operation: .create, egFormInstance: instance, extraData: createExtraData)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, instance, "Created locally. Will sync when online.")
            }
        }
    }

    // MARK: - Update (full submit)

    /// Persist a submission to an existing instance. Sets `submitted` and
    /// pushes to the server (or queues if offline). `nodeIds` lets the
    /// caller resync node mappings at the same time — server handles
    /// add/remove as a full-replace.
    static func updateEGFormInstance(
        _ instance: EGFormInstance,
        submission: String?,
        submitted: Bool,
        nodeIds: [UUID]? = nil,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        instance.form_submission = submission
        instance.submitted = submitted
        instance.modified_at = Date()

        do {
            try modelContext.save()
            AppLogger.log(.info, "Updated EGFormInstance \(instance.id) locally", category: .form)
        } catch {
            AppLogger.log(.error, "Failed to save EGFormInstance: \(error)", category: .form)
            onCompletion?(false, "Failed to save form: \(error.localizedDescription)")
            return
        }

        let body = EGFormInstanceUpdateBody(
            form_submission: anyCodable(from: submission),
            submitted: submitted,
            is_deleted: nil,
            node_ids: nodeIds?.map { $0.uuidString }
        )

        if NetworkState.shared.canDirectSync {
            Task {
                do {
                    _ = try await APIClient.shared.updateEGFormInstance(id: instance.id, body: body)
                    AppLogger.log(.info, "EGFormInstance \(instance.id) synced", category: .form)
                    await MainActor.run { onCompletion?(true, "Form submission saved successfully") }
                } catch {
                    AppLogger.log(.error, "Failed to sync EGFormInstance: \(error)", category: .form)
                    await MainActor.run {
                        let op = SyncOp(target: .egFormInstance, operation: .update, egFormInstance: instance)
                        NetworkState.shared.enqueue(op)
                        onCompletion?(true, "Form saved locally. Will sync when online.")
                    }
                }
            }
        } else {
            DispatchQueue.main.async {
                let op = SyncOp(target: .egFormInstance, operation: .update, egFormInstance: instance)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Form saved locally. Will sync when online.")
            }
        }
    }

    // MARK: - Soft delete

    /// Mark an EG form instance as deleted. The instance row remains in
    /// the DB; both client and server hide it from lists by filtering
    /// `is_deleted`. Same convention as legacy FormInstance + most
    /// other entities in the app.
    static func softDeleteEGFormInstance(
        _ instance: EGFormInstance,
        modelContext: ModelContext,
        onCompletion: ((Bool, String?) -> Void)? = nil
    ) {
        instance.is_deleted = true
        instance.modified_at = Date()
        do {
            try modelContext.save()
        } catch {
            AppLogger.log(.error, "Failed to save EGFormInstance soft-delete: \(error)", category: .form)
            onCompletion?(false, "Failed to save: \(error.localizedDescription)")
            return
        }

        let body = EGFormInstanceUpdateBody(
            form_submission: nil,
            submitted: nil,
            is_deleted: true,
            node_ids: nil
        )

        if NetworkState.shared.canDirectSync {
            Task {
                do {
                    _ = try await APIClient.shared.updateEGFormInstance(id: instance.id, body: body)
                    await MainActor.run { onCompletion?(true, nil) }
                } catch {
                    AppLogger.log(.error, "Failed to sync EG soft-delete: \(error)", category: .form)
                    await MainActor.run {
                        let op = SyncOp(target: .egFormInstance, operation: .update, egFormInstance: instance)
                        NetworkState.shared.enqueue(op)
                        onCompletion?(true, "Removed locally. Will sync when online.")
                    }
                }
            }
        } else {
            DispatchQueue.main.async {
                let op = SyncOp(target: .egFormInstance, operation: .update, egFormInstance: instance)
                NetworkState.shared.enqueue(op)
                onCompletion?(true, "Removed locally. Will sync when online.")
            }
        }
    }

    // MARK: - Draft auto-save

    /// Lighter-weight save used while the user is still filling the form
    /// — never sets `submitted` and doesn't try to hit the server when
    /// online (the user will submit explicitly). Offline still queues so
    /// drafts survive across app restarts.
    static func updateEGFormInstanceDraft(
        _ instance: EGFormInstance,
        submission: String?,
        modelContext: ModelContext
    ) {
        instance.form_submission = submission
        instance.modified_at = Date()
        do {
            try modelContext.save()
            AppLogger.log(.debug, "EG form draft saved locally", category: .form)
        } catch {
            AppLogger.log(.notice, "Failed to save EG form draft: \(error)", category: .form)
            return
        }
        if NetworkState.shared.mode == .offline {
            DispatchQueue.main.async {
                let op = SyncOp(target: .egFormInstance, operation: .update, egFormInstance: instance)
                NetworkState.shared.enqueue(op)
            }
        }
    }

    // MARK: - Helpers

    /// Convert a stored form_submission JSON string into the AnyCodable
    /// payload the wire DTO expects. Strings round-trip as a string,
    /// objects parse to dict, arrays to array.
    private static func anyCodable(from submission: String?) -> AnyCodable? {
        guard let s = submission else { return nil }
        if s.isEmpty { return nil }
        guard let data = s.data(using: .utf8) else { return AnyCodable(s) }
        if let obj = try? JSONSerialization.jsonObject(with: data) {
            return AnyCodable(obj)
        }
        return AnyCodable(s)
    }
}
