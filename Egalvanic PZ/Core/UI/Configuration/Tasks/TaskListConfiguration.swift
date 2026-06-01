import SwiftUI
import SwiftData

// Make UserTask conform to ListableEntity
extension UserTask: ListableEntity {
    var createdDate: Date? { created_at }
    var modifiedDate: Date? { created_at } // Tasks don't have modified date, use created
}

// Task Filter Types
enum TaskFilter: String, CaseIterable, Hashable, CustomStringConvertible {
    case all = "All"
    case open = "Open"
    case completed = "Completed"
    case assignedToMe = "Assigned to Me"
    case ownedByMe = "Owned by Me"
    case overdue = "Overdue"
    case dueToday = "Due Today"
    case dueThisWeek = "Due This Week"
    
    var description: String { rawValue }
}

// Make TaskFilter conform to FilterChipDisplayable
extension TaskFilter: FilterChipDisplayable {
    var displayName: String {
        switch self {
        case .all: return AppStrings.Common.all
        case .open: return AppStrings.Issues.statusOpen
        case .completed: return AppStrings.Tasks.completed
        case .assignedToMe: return AppStrings.Tasks.assignedToMe
        case .ownedByMe: return AppStrings.Tasks.ownedByMe
        case .overdue: return AppStrings.Tasks.overdue
        case .dueToday: return AppStrings.Tasks.dueToday
        case .dueThisWeek: return AppStrings.Tasks.dueThisWeek
        }
    }
    
    var icon: String? {
        switch self {
        case .all: return nil
        case .open: return "circle"
        case .completed: return "checkmark.circle.fill"
        case .assignedToMe: return "person.fill"
        case .ownedByMe: return "star.fill"
        case .overdue: return "exclamationmark.triangle.fill"
        case .dueToday: return "calendar.badge.clock"
        case .dueThisWeek: return "calendar"
        }
    }
    
    var color: Color {
        switch self {
        case .all: return .gray
        case .open: return .blue
        case .completed: return .green
        case .assignedToMe: return .purple
        case .ownedByMe: return .indigo
        case .overdue: return .red
        case .dueToday: return .orange
        case .dueThisWeek: return .blue
        }
    }
}

// Task List Configuration
struct TaskListConfiguration: EntityListConfiguration {
    typealias Entity = UserTask
    typealias FilterType = TaskFilter
    
    // Dependencies passed in
    let appState: AppStateManager
    let networkState: NetworkState
    let modelContext: ModelContext
    
    // Callbacks for navigation and actions
    var onTaskTapped: (UserTask) -> Void
    var onCreateTapped: () -> Void
    var onDeleteTask: (UserTask) -> Void
    var onCompleteTask: (UserTask) -> Void
    var onReopenTask: (UserTask) -> Void
    // Long-press context menu actions (optional — defaults to no-op so
    // existing call sites that don't wire these still compile).
    var onOpenForm: ((UserTask) -> Void)? = nil
    var onOpenPhotos: ((UserTask) -> Void)? = nil
    var onGoToWorkOrder: ((UserTask, IRSession) -> Void)? = nil
    
    // MARK: - Basic Info
    var entityName: String { AppStrings.Tasks.task }
    var pluralEntityName: String { AppStrings.Tasks.tasks }
    var emptyStateMessage: String { AppStrings.Tasks.createNewTaskMessage }
    var emptyStateIcon: String { "checklist" }
    
    // MARK: - Display Methods
    func title(for task: UserTask) -> String {
        task.title
    }

    // Task titles ("Clean, Tighten, Torque - ATS 135") are long — allow
    // two lines before truncating.
    var titleLineLimit: Int { 2 }
    
    func subtitle(for task: UserTask) -> String? {
        // Relationship-derived location ("Building · Floor · Room") for
        // the first linked node. node.location is a legacy free-text
        // field — fall back to it only when no room hierarchy exists,
        // since most newer nodes only populate the relationship.
        let linkedNodes = task.linkedNodes.filter { !$0.is_deleted }
        guard let node = linkedNodes.first else { return nil }
        let parts: [String] = [
            node.room?.floor?.building?.name,
            node.room?.floor?.name,
            node.room?.name,
        ].compactMap { $0 }.filter { !$0.isEmpty }
        if !parts.isEmpty { return parts.joined(separator: " · ") }
        return node.location
    }
    
    func statusBadge(for task: UserTask) -> (text: String, color: Color)? {
        if task.completed {
            return (AppStrings.Tasks.completed, .green)
        } else if let dueDate = task.due_date {
            if dueDate < Date() {
                return (AppStrings.Common.overdue, .red)
            } else if Calendar.current.isDateInToday(dueDate) {
                return (AppStrings.Tasks.dueToday, .orange)
            }
        } else {
            return (AppStrings.Issues.statusOpen, .blue)
        }
        return nil
    }
    
    func icon(for task: UserTask) -> String {
        if task.completed {
            return "checkmark.circle.fill"
        } else {
            return "circle"
        }
    }
    
    func iconColor(for task: UserTask) -> Color {
        if task.completed {
            return .green
        } else if let dueDate = task.due_date, dueDate < Date() {
            return .red
        } else {
            return .gray
        }
    }
    
    func metadataItems(for task: UserTask) -> [MetadataItem] {
        // Asset and WO share one row; due date moves to its own line
        // (see `footnote(for:)` below).
        var items: [MetadataItem] = []

        let linkedNodes = task.linkedNodes.filter { !$0.is_deleted }
        if !linkedNodes.isEmpty {
            let assetText: String = linkedNodes.count == 1
                ? linkedNodes[0].label
                : "\(linkedNodes[0].label) + \(linkedNodes.count - 1)"
            items.append(MetadataItem(icon: "square.grid.2x2", text: assetText))
        }

        // Work order — flexible chip that expands into the remaining row
        // width and truncates only at the edge, so long WO names ("Insp
        // Round 1 — Acme Power 2026 Q2") use the available space on the
        // right instead of being clipped at a fixed length.
        if let session = task.sessions.first(where: { !$0.is_deleted }) {
            items.append(MetadataItem(
                icon: "briefcase",
                text: session.name,
                color: .blue,
                flexible: true
            ))
        }

        return items
    }

    func footnote(for task: UserTask) -> (text: String, color: Color)? {
        // Labeled due date on its own line: "Due: 5/3/26". Red when
        // overdue so the row still flags lateness even though the
        // status badge in the title row already says "Overdue".
        guard let dueDate = task.due_date else { return nil }
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        return (
            text: "Due: \(formatter.string(from: dueDate))",
            color: dueDate < Date() ? .red : .secondary
        )
    }

    // MARK: - Filtering
    //
    // UI surfaces only three filters now — Open (default), Completed,
    // All — matching the Tasks card on the site tab. The other
    // TaskFilter cases (assignedToMe, ownedByMe, overdue, dueToday,
    // dueThisWeek) remain in the enum so existing callers / tests /
    // future surfaces can still reference them, but they aren't
    // pickable from this list. Within Open, overdue tasks float to
    // the top via the default sort comparator below.
    func availableFilters() -> [TaskFilter] {
        [.open, .completed, .all]
    }

    func defaultFilter() -> TaskFilter {
        .open
    }
    
    func matches(entity task: UserTask, filter: TaskFilter) -> Bool {
        switch filter {
        case .all:
            return true
        case .open:
            return !task.completed
        case .completed:
            return task.completed
        case .assignedToMe:
            return task.assigned_to.contains(appState.userId)
        case .ownedByMe:
            return task.owned_by.contains(appState.userId)
        case .overdue:
            guard let dueDate = task.due_date else { return false }
            return dueDate < Date() && !task.completed
        case .dueToday:
            guard let dueDate = task.due_date else { return false }
            return Calendar.current.isDateInToday(dueDate) && !task.completed
        case .dueThisWeek:
            guard let dueDate = task.due_date else { return false }
            let calendar = Calendar.current
            let weekFromNow = calendar.date(byAdding: .weekOfYear, value: 1, to: Date()) ?? Date()
            return dueDate <= weekFromNow && !task.completed
        }
    }
    
    func filterCount(for filter: TaskFilter, in tasks: [UserTask]) -> Int {
        tasks.filter { matches(entity: $0, filter: filter) }.count
    }
    
    // MARK: - Search
    func searchText(for task: UserTask) -> String {
        let parts = [
            task.title,
            task.task_description,
            task.node?.label ?? "",
            task.node?.location ?? ""
        ]
        return parts.joined(separator: " ")
    }
    
    // MARK: - Sorting
    func sortOptions() -> [SortOption<UserTask>] {
        [
            // Default — overdue (uncompleted, due_date < now) floats to
            // the top, then by ascending due date (earliest first; no
            // due date last), then by title. Surfaces what the user
            // needs to deal with first when the Open filter is active.
            SortOption(
                title: "Priority",
                icon: "exclamationmark.triangle",
                comparator: { (a, b) in
                    let now = Date()
                    let aOverdue = (a.due_date.map { $0 < now } ?? false) && !a.completed
                    let bOverdue = (b.due_date.map { $0 < now } ?? false) && !b.completed
                    if aOverdue != bOverdue { return aOverdue }
                    switch (a.due_date, b.due_date) {
                    case let (l?, r?): return l < r
                    case (_?, nil):    return true
                    case (nil, _?):    return false
                    default:           break
                    }
                    return a.title.localizedCaseInsensitiveCompare(b.title) == .orderedAscending
                }
            ),
            SortOption(
                title: AppStrings.Common.title,
                icon: "textformat",
                comparator: { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
            ),
            SortOption(
                title: AppStrings.Common.dueDate,
                icon: "calendar",
                comparator: { (task1, task2) in
                    // Tasks with no due date go to the end
                    guard let date1 = task1.due_date else { return false }
                    guard let date2 = task2.due_date else { return true }
                    return date1 < date2
                }
            ),
            SortOption(
                title: AppStrings.Common.createdDate,
                icon: "clock",
                comparator: { (task1, task2) in
                    let date1 = task1.created_at ?? Date.distantPast
                    let date2 = task2.created_at ?? Date.distantPast
                    return date1 > date2
                }
            ),
            SortOption(
                title: AppStrings.Common.status,
                icon: "flag",
                comparator: { !$0.completed && $1.completed }
            )
        ]
    }

    func defaultSort() -> SortOption<UserTask> {
        sortOptions()[0] // Priority — overdue first
    }

    // MARK: - Long-press context menu

    func contextMenuActions(for task: UserTask) -> [ContextMenuAction<UserTask>]? {
        // The host view is responsible for picker UI on Open Form
        // (legacy vs EG, or multiple instances) and for routing
        // Photos through TaskDetailView. Configuration just declares
        // the intent.
        var actions: [ContextMenuAction<UserTask>] = []

        if let onOpenForm = onOpenForm {
            actions.append(ContextMenuAction(
                title: "Open Form",
                icon: "doc.text",
                action: { onOpenForm($0) }
            ))
        }
        if let onOpenPhotos = onOpenPhotos {
            actions.append(ContextMenuAction(
                title: "Photos",
                icon: "photo.on.rectangle",
                action: { onOpenPhotos($0) }
            ))
        }
        // Go to Work Order — only when the task is linked to a non-deleted
        // session. Picks the first such session; if a task is somehow
        // linked to multiple WOs we still land on a valid one.
        if let onGoToWorkOrder = onGoToWorkOrder,
           let session = task.sessions.first(where: { !$0.is_deleted }) {
            actions.append(ContextMenuAction(
                title: "Go to Work Order",
                icon: "briefcase",
                action: { onGoToWorkOrder($0, session) }
            ))
        }
        if !task.completed {
            actions.append(ContextMenuAction(
                title: AppStrings.Tasks.complete,
                icon: "checkmark.circle.fill",
                action: { onCompleteTask($0) }
            ))
        }

        return actions.isEmpty ? nil : actions
    }
    
    // MARK: - Actions
    func swipeActions(for task: UserTask) -> [SwipeAction<UserTask>]? {
        var actions: [SwipeAction<UserTask>] = []

        // Toggle complete/reopen
        if task.completed {
            actions.append(SwipeAction(
                title: AppStrings.Common.reopen,
                icon: "arrow.uturn.backward",
                color: .orange
            ) { task in
                onReopenTask(task)
            })
        } else {
            actions.append(SwipeAction(
                title: AppStrings.Common.complete,
                icon: "checkmark",
                color: .green
            ) { task in
                onCompleteTask(task)
            })
        }

        // Delete
        actions.append(SwipeAction(
            title: AppStrings.Common.delete,
            icon: "trash",
            color: .red,
            role: .destructive
        ) { task in
            onDeleteTask(task)
        })

        return actions
    }
    
    func onTap(entity task: UserTask) {
        onTaskTapped(task)
    }
    
    func onCreate() {
        onCreateTapped()
    }
}
