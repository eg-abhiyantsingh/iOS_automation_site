import SwiftUI
import SwiftData

public enum FormListFilter: Hashable, CaseIterable {
    case showAll
    case showGlobal
    case showLocal
}

// MARK: - Task Linked Form Instances List Configuration
struct TaskLinkedFormInstancesList: EntityLinkedListProtocol {
    typealias ParentEntity = UserTask
    typealias LinkedEntity = FormInstance
    typealias FilterType = FormListFilter

    // Dependencies
    let modelContext: ModelContext
    let appState: AppStateManager
    let onSelectFormInstance: (FormInstance) -> Void
    let onDeleteFormInstance: (FormInstance) -> Void
    let onAddFormInstance: () -> Void

    // MARK: - Core Configuration
    var title: String { "Associated Forms" }
    var systemIcon: String { "doc.text.fill" }
    var emptyStateMessage: String { "No forms attached" }
    var emptyStateIcon: String { "doc.text.fill" }
    var addButtonLabel: String { "Link Forms" }
    var showSearch: Bool { false }
    var showFilters: Bool { false }
    var showAddButton: Bool { true }

    // MARK: - Data Access
    func getLinkedEntities(from parent: UserTask) -> [FormInstance] {
        // Return form instances linked to this task
        return parent.linkedFormInstances.filter { !$0.is_deleted }
    }

    func isDeleted(_ entity: FormInstance) -> Bool {
        entity.is_deleted
    }

    // MARK: - Filtering
    func defaultFilter() -> FormListFilter {
        .showAll
    }

    func filterMenuLabel(for filter: FormListFilter) -> String {
        switch filter {
        case .showAll:
            return "All Forms"
        case .showGlobal:
            return "Submitted Forms"
        case .showLocal:
            return "Pending Forms"
        }
    }

    func shouldShow(_ entity: FormInstance, with filter: FormListFilter) -> Bool {
        switch filter {
        case .showAll:
            return true
        case .showGlobal:  // Repurposed as "Submitted"
            return entity.submitted
        case .showLocal:   // Repurposed as "Pending"
            return !entity.submitted
        }
    }

    // MARK: - Display Configuration
    func displayTitle(for entity: FormInstance) -> String {
        // Show the form master's title if available
        if let formMaster = entity.formMaster {
            return formMaster.title
        }
        return "Form Instance"
    }

    func displaySubtitle(for entity: FormInstance) -> String? {
        var parts: [String] = []

        // Add submission status
        if entity.submitted {
            parts.append("✓ Submitted")
            if let modifiedAt = entity.modified_at {
                let formatter = DateFormatter()
                formatter.dateStyle = .short
                formatter.timeStyle = .short
                parts.append(formatter.string(from: modifiedAt))
            }
        } else {
            parts.append("Pending")
        }

        // Add form type if available
        if let formMaster = entity.formMaster {
            parts.append(formMaster.is_global ? "Global" : "Local")
        }

        return parts.isEmpty ? nil : parts.joined(separator: " • ")
    }

    func displayIcon(for entity: FormInstance) -> String? {
        if entity.submitted {
            return "checkmark.circle.fill"
        } else {
            return "circle"
        }
    }

    func displayIconColor(for entity: FormInstance) -> Color {
        if entity.submitted {
            return .green
        } else {
            return .orange
        }
    }

    func displayBadge(for entity: FormInstance) -> (text: String, color: Color)? {
        if entity.submitted {
            return ("Submitted", .green)
        } else {
            return ("Pending", .orange)
        }
    }

    func displayAccessory(for entity: FormInstance) -> EntityAccessoryType? {
        .chevron
    }

    // MARK: - Sorting
    func sortEntities(_ entities: [FormInstance]) -> [FormInstance] {
        entities.sorted { instance1, instance2 in
            // Sort pending forms first
            if instance1.submitted != instance2.submitted {
                return !instance1.submitted
            }

            // Then by form title if available
            let title1 = instance1.formMaster?.title ?? ""
            let title2 = instance2.formMaster?.title ?? ""
            return title1 < title2
        }
    }

    // MARK: - Actions
    func onSelect(_ entity: FormInstance) {
        onSelectFormInstance(entity)
    }

    func onDelete(_ entity: FormInstance) {
        onDeleteFormInstance(entity)
    }

    func onAdd() {
        onAddFormInstance()
    }

    // MARK: - Session Info
    func sessionInfo() -> SessionDisplayInfo? {
        // Show form-specific session info if needed
        guard let session = appState.activeSession else {
            return nil
        }

        return SessionDisplayInfo(
            title: "Active Session: \(session.name)",
            subtitle: "Forms in this session will be tracked",
            warningMessage: nil
        )
    }

}
