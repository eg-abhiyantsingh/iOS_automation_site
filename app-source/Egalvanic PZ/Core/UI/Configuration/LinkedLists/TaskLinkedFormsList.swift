import SwiftUI
import SwiftData

// MARK: - Form List Filter
//public enum FormListFilter: Hashable, CaseIterable {
//    case showAll
//    case showGlobal
//    case showLocal
//}

// MARK: - Task Linked Forms List Configuration
//struct TaskLinkedFormsList: EntityLinkedListProtocol {
//    typealias ParentEntity = UserTask
//    typealias LinkedEntity = UserTaskForm
//    typealias FilterType = FormListFilter
//
//    // Dependencies
//    let modelContext: ModelContext
//    let appState: AppStateManager
//    let onSelectForm: (UserTaskForm) -> Void
//    let onDeleteForm: (UserTaskForm) -> Void
//    let onAddForm: () -> Void
//
//    // MARK: - Core Configuration
//    var title: String { "Linked Forms" }
//    var systemIcon: String { "doc.text" }
//    var emptyStateMessage: String { "No linked forms" }
//    var emptyStateIcon: String { "doc.text" }
//    var addButtonLabel: String { "Link Form" }
//    var showSearch: Bool { false }
//    var showFilters: Bool { false }
//
//    // MARK: - Data Access
//    func getLinkedEntities(from parent: UserTask) -> [UserTaskForm] {
//        // Only return forms from the linkedForms array
//        // Legacy form field is deprecated and should not be shown
//        return parent.linkedForms
//    }
//
//    func isDeleted(_ entity: UserTaskForm) -> Bool {
//        entity.is_deleted
//    }
//
//    // MARK: - Filtering
//    func defaultFilter() -> FormListFilter {
//        .showAll
//    }
//
//    func filterMenuLabel(for filter: FormListFilter) -> String {
//        switch filter {
//        case .showAll:
//            return "Show All Forms"
//        case .showGlobal:
//            return "Show Global Forms"
//        case .showLocal:
//            return "Show Local Forms"
//        }
//    }
//
//    func shouldShow(_ entity: UserTaskForm, with filter: FormListFilter) -> Bool {
//        switch filter {
//        case .showAll:
//            return true
//        case .showGlobal:
//            return entity.is_global
//        case .showLocal:
//            return !entity.is_global
//        }
//    }
//
//    // MARK: - Display Configuration
//    func displayTitle(for entity: UserTaskForm) -> String {
//        entity.title
//    }
//
//    func displaySubtitle(for entity: UserTaskForm) -> String? {
//        var parts: [String] = []
//
//        // Add global/local indicator
//        parts.append(entity.is_global ? "Global Form" : "Local Form")
//
//        // Add JSON validation status
//        if !entity.isValidJSON {
//            parts.append("⚠️ Invalid JSON")
//        }
//
//        return parts.isEmpty ? nil : parts.joined(separator: " • ")
//    }
//
//    func displayIcon(for entity: UserTaskForm) -> String? {
//        if entity.is_global {
//            return "globe"
//        } else {
//            return "doc.text"
//        }
//    }
//
//    func displayIconColor(for entity: UserTaskForm) -> Color {
//        if !entity.isValidJSON {
//            return .red
//        } else if entity.is_global {
//            return .blue
//        } else {
//            return .green
//        }
//    }
//
//    func displayBadge(for entity: UserTaskForm) -> (text: String, color: Color)? {
//        if !entity.isValidJSON {
//            return ("Invalid", .red)
//        } else if entity.is_global {
//            return ("Global", .blue)
//        }
//        return nil
//    }
//
//    func displayAccessory(for entity: UserTaskForm) -> EntityAccessoryType? {
//        .chevron
//    }
//
//    // MARK: - Sorting
//    func sortEntities(_ entities: [UserTaskForm]) -> [UserTaskForm] {
//        entities.sorted { form1, form2 in
//            // Sort global forms first
//            if form1.is_global != form2.is_global {
//                return form1.is_global
//            }
//
//            // Then by title
//            return form1.title < form2.title
//        }
//    }
//
//    // MARK: - Actions
//    func onSelect(_ entity: UserTaskForm) {
//        onSelectForm(entity)
//    }
//
//    func onDelete(_ entity: UserTaskForm) {
//        onDeleteForm(entity)
//    }
//
//    func onAdd() {
//        onAddForm()
//    }
//
//    // MARK: - Session Info
//    func sessionInfo() -> SessionDisplayInfo? {
//        // Show form-specific session info if needed
//        guard let session = appState.activeSession else {
//            return nil
//        }
//
//        return SessionDisplayInfo(
//            title: session.name,
//            subtitle: "Active Session",
//            icon: "doc.badge.clock",
//            iconColor: .blue,
//            warningMessage: nil
//        )
//    }
//
//    // MARK: - Linking Configuration
//    func linkingConfiguration() -> EntityLinkingConfiguration<UserTask, UserTaskForm>? {
//        EntityLinkingConfiguration(
//            relationship: .manyToMany,
//            linkButtonLabel: "Link Forms",
//            linkViewTitle: "Select Forms to Link",
//            searchPrompt: "Search by form title...",
//            fetchAvailable: { task in
//                // Fetch all forms that aren't deleted
//                let descriptor = FetchDescriptor<UserTaskForm>(
//                    predicate: #Predicate<UserTaskForm> { form in
//                        !form.is_deleted
//                    }
//                )
//
//                do {
//                    let allForms = try modelContext.fetch(descriptor)
//                    return allForms
//                } catch {
//                    print("❌ Failed to fetch forms: \(error)")
//                    return []
//                }
//            },
//            currentlyLinked: { task in
//                // Get IDs of currently linked forms from array only
//                // Legacy form field is deprecated and should not be included
//                return Set(task.linkedForms.filter { !$0.is_deleted }.map { $0.id })
//            },
//            applyLinking: { task, selectedIds, availableForms in
//                print("📋 [TaskLinkedFormsList] Starting link/unlink operation")
//                print("  📌 Task ID: \(task.id)")
//                print("  📌 Task Title: \(task.title)")
//
//                // Get current linked IDs from array only
//                // Legacy form field is deprecated and should not be included
//                let currentIds = Set(task.linkedForms.filter { !$0.is_deleted }.map { $0.id })
//
//                print("  📊 Current linked form IDs: \(currentIds.map { $0.uuidString }.joined(separator: ", "))")
//                print("  📊 Selected form IDs: \(selectedIds.map { $0.uuidString }.joined(separator: ", "))")
//
//                // Find forms to unlink and link
//                let toUnlink = currentIds.subtracting(selectedIds)
//                let toLink = selectedIds.subtracting(currentIds)
//
//                if !toUnlink.isEmpty {
//                    print("  📤 Unlinking \(toUnlink.count) forms:")
//                    print("     IDs to unlink: \(toUnlink.map { $0.uuidString }.joined(separator: ", "))")
//                }
//
//                if !toLink.isEmpty {
//                    print("  📥 Linking \(toLink.count) forms:")
//                    print("     IDs to link: \(toLink.map { $0.uuidString }.joined(separator: ", "))")
//                }
//
//                // Track if we need to update the task entity for legacy field changes
//                var needsTaskUpdate = false
//
//                // Update local arrays immediately for UI feedback
//                for formId in toUnlink {
//                    // Remove from task's linkedForms array
//                    task.linkedForms.removeAll { $0.id == formId }
//
//                    // Find the form for logging
//                    if let form = availableForms.first(where: { $0.id == formId }) ?? task.form {
//                        print("    - Removing Form ID: \(form.id) | Title: \(form.title)")
//                    }
//
//                    // Clear legacy single form if it's being unlinked
//                    if task.form?.id == formId {
//                        print("      Clearing legacy task.form reference")
//                        task.form = nil
//                        needsTaskUpdate = true
//                    }
//                }
//
//                // Add forms to arrays immediately for UI feedback
//                for formId in toLink {
//                    if let form = availableForms.first(where: { $0.id == formId }) {
//                        print("    - Adding Form ID: \(form.id) | Title: \(form.title)")
//
//                        // Add to task's linkedForms if not already there
//                        if !task.linkedForms.contains(where: { $0.id == formId }) {
//                            task.linkedForms.append(form)
//                        }
//
//                        // Set as legacy single form if none exists
//                        if task.form == nil {
//                            task.form = form
//                            print("      Set as legacy task.form reference")
//                        }
//                    }
//                }
//
//                // Save local changes
//                do {
//                    try modelContext.save()
//                    print("✅ Local arrays updated successfully")
//                } catch {
//                    print("❌ Failed to save local changes: \(error)")
//                }
//
//                // Queue sync operations for persistence
//                Task {
//                    do {
//                        // Queue removal operations for soft delete
//                        if !toUnlink.isEmpty {
//                            try await TaskMappingService.shared.removeTaskForms(
//                                taskId: task.id,
//                                formIds: Array(toUnlink)
//                            )
//                            print("  ✅ Queued \(toUnlink.count) forms for soft delete in database")
//
//                            // If we cleared the legacy form field, queue task update
//                            if needsTaskUpdate {
//                                let op = SyncOp(target: .userTask, operation: .update, userTask: task)
//                                await NetworkState.shared.enqueue(op)
//                                print("  ✅ Queued task update for legacy form field change")
//                            }
//                        }
//
//                        // Queue addition operations
//                        if !toLink.isEmpty {
//                            try await TaskMappingService.shared.addTaskForms(
//                                taskId: task.id,
//                                formIds: Array(toLink)
//                            )
//                            print("  ✅ Queued \(toLink.count) forms for linking in database")
//                        }
//                    } catch {
//                        print("  ❌ Failed to queue task-form mappings: \(error)")
//                    }
//                }
//
//                print("  📊 Summary:")
//                print("     Forms unlinked: \(toUnlink.count)")
//                print("     Forms linked: \(toLink.count)")
//                print("     Current linked forms: \(task.linkedForms.count)")
//            },
//            displayInSelector: { form in
//                EntitySelectorDisplay(
//                    title: form.title,
//                    subtitle: [
//                        form.is_global ? "Global Form" : "Local Form",
//                        !form.isValidJSON ? "⚠️ Invalid JSON" : nil
//                    ].compactMap { $0 }.joined(separator: " • "),
//                    icon: form.is_global ? "globe" : "doc.text",
//                    iconColor: !form.isValidJSON ? .red : (form.is_global ? .blue : .green),
//                    badge: {
//                        if !form.isValidJSON {
//                            return "Invalid"
//                        } else if form.is_global {
//                            return "Global"
//                        }
//                        return nil
//                    }(),
//                    isAlreadyLinked: false // Will be handled by the selector
//                )
//            },
//            canLink: { _ in
//                // Can always link/unlink any form
//                true
//            }
//        )
//    }
//}