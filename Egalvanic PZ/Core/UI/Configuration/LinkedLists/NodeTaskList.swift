import SwiftUI
import SwiftData

// MARK: - Task List Filter
public enum TaskListFilter: Hashable, CaseIterable {
    case showAll
    case showIncomplete
    case showCompleted
}

// MARK: - Node Task List Configuration
struct NodeTaskList: EntityLinkedListProtocol {
    typealias ParentEntity = NodeV2
    typealias LinkedEntity = UserTask
    typealias FilterType = TaskListFilter
    
    // Dependencies
    let appState: AppStateManager
    let onSelectTask: (UserTask) -> Void
    let onDeleteTask: (UserTask) -> Void
    let onAddTask: () -> Void
    
    // MARK: - Core Configuration
    var title: String { AppStrings.Tasks.tasks }
    var systemIcon: String { "checklist" }
    var emptyStateMessage: String { AppStrings.Tasks.noTasks }
    var emptyStateIcon: String { "checklist" }
    var addButtonLabel: String { AppStrings.Sessions.addTask }
    var showSearch: Bool { false }
    
    // MARK: - Data Access
    func getLinkedEntities(from parent: NodeV2) -> [UserTask] {
        return parent.node_tasks
    }
    
    func isDeleted(_ entity: UserTask) -> Bool {
        entity.is_deleted
    }
    
    // MARK: - Filtering
    func defaultFilter() -> TaskListFilter {
        .showIncomplete
    }
    
    func filterMenuLabel(for filter: TaskListFilter) -> String {
        switch filter {
        case .showAll:
            return AppStrings.Tasks.showAllTasks
        case .showIncomplete:
            return AppStrings.Tasks.showIncompleteOnly
        case .showCompleted:
            return AppStrings.Tasks.showCompletedOnly
        }
    }
    
    func shouldShow(_ entity: UserTask, with filter: TaskListFilter) -> Bool {
        switch filter {
        case .showAll:
            return true
        case .showIncomplete:
            return !entity.completed
        case .showCompleted:
            return entity.completed
        }
    }
    
    // MARK: - Display Configuration
    func displayTitle(for entity: UserTask) -> String {
        entity.title
    }
    
    func displaySubtitle(for entity: UserTask) -> String? {
        entity.task_description.isEmpty ? nil : entity.task_description
    }
    
    func displayIcon(for entity: UserTask) -> String? {
        entity.completed ? "checkmark.circle.fill" : "circle"
    }
    
    func displayIconColor(for entity: UserTask) -> Color {
        entity.completed ? .green : .gray
    }
    
    func displayBadge(for entity: UserTask) -> (text: String, color: Color)? {
        // Show task status - simple boolean state
        if entity.completed {
            return (AppStrings.Issues.statusResolved, .green)
        } else {
            return (AppStrings.Issues.statusOpen, .orange)
        }
    }
    
    func displayAccessory(for entity: UserTask) -> EntityAccessoryType? {
        .chevron
    }
    
    // MARK: - Sorting
    func sortEntities(_ entities: [UserTask]) -> [UserTask] {
        entities.sorted { task1, task2 in
            // Sort incomplete tasks first
            if task1.completed != task2.completed {
                return !task1.completed
            }
            
            // Then by due date (if exists)
            if let date1 = task1.due_date, let date2 = task2.due_date {
                return date1 < date2
            } else if task1.due_date != nil {
                return true
            } else if task2.due_date != nil {
                return false
            }
            
            // Finally by title
            return task1.title < task2.title
        }
    }
    
    // MARK: - Actions
    func onSelect(_ entity: UserTask) {
        onSelectTask(entity)
    }
    
    func onDelete(_ entity: UserTask) {
        onDeleteTask(entity)
    }
    
    func onAdd() {
        onAddTask()
    }
    
    // MARK: - Session Info
    func sessionInfo() -> SessionDisplayInfo? {
        // Only show session info if there's an active session
        guard let session = appState.activeSession else {
            return nil
        }
        
        return SessionDisplayInfo(
            title: session.name,
            subtitle: "Type: \(session.photo_type)",
            warningMessage: nil
        )
    }
}