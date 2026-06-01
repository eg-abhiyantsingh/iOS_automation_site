import SwiftUI
import SwiftData

// Make Issue conform to ListableEntity
extension Issue: ListableEntity {
    var createdDate: Date? { created_date }
    var modifiedDate: Date? { modified_date }
    var isDeleted: Bool { is_deleted }
}

// Issue Filter Types
enum IssueFilter: String, CaseIterable, Hashable, CustomStringConvertible {
    case all = "All"
    case open = "Open"
    case inProgress = "In Progress"
    case pending = "Pending"
    case resolved = "Resolved"
    case closed = "Closed"
    case critical = "Critical"
    case withPhotos = "With Photos"
    case mySession = "My Session"

    var description: String { rawValue }
}

// Make IssueFilter conform to FilterChipDisplayable
extension IssueFilter: FilterChipDisplayable {
    var displayName: String {
        switch self {
        case .all: return AppStrings.Common.all
        case .open: return AppStrings.Issues.statusOpen
        case .inProgress: return AppStrings.Issues.statusInProgress
        case .pending: return AppStrings.Issues.statusPending
        case .resolved: return AppStrings.Issues.statusResolved
        case .closed: return AppStrings.Issues.statusClosed
        case .critical: return AppStrings.Issues.priorityCritical
        case .withPhotos: return AppStrings.Issues.withPhotos
        case .mySession: return AppStrings.Issues.mySession
        }
    }
    
    var icon: String? {
        switch self {
        case .all: return nil
        case .open: return "exclamationmark.circle"
        case .inProgress: return "arrow.triangle.2.circlepath"
        case .pending: return "clock"
        case .resolved: return "checkmark.circle"
        case .closed: return "xmark.circle"
        case .critical: return "exclamationmark.triangle.fill"
        case .withPhotos: return "photo"
        case .mySession: return "briefcase.fill"
        }
    }

    var color: Color {
        switch self {
        case .all: return .gray
        case .open: return .blue
        case .inProgress: return .orange
        case .pending: return .orange
        case .resolved: return .green
        case .closed: return .gray
        case .critical: return .red
        case .withPhotos: return .purple
        case .mySession: return .blue
        }
    }
}

// Issue List Configuration
struct IssueListConfiguration: EntityListConfiguration {
    typealias Entity = Issue
    typealias FilterType = IssueFilter
    
    // Dependencies passed in
    let appState: AppStateManager
    let networkState: NetworkState
    let modelContext: ModelContext
    
    // Callbacks for navigation
    var onIssueTapped: (Issue) -> Void
    var onCreateTapped: () -> Void
    var onDeleteIssue: (Issue) -> Void
    var onResolveIssue: (Issue) -> Void
    var onReopenIssue: (Issue) -> Void
    
    // MARK: - Basic Info
    var entityName: String { AppStrings.Issues.issue }
    var pluralEntityName: String { AppStrings.Issues.issues }
    var emptyStateMessage: String { AppStrings.Issues.createNewIssueMessage }
    var emptyStateIcon: String { "exclamationmark.triangle" }
    
    // MARK: - Display Methods
    func title(for issue: Issue) -> String {
        issue.title ?? AppStrings.CommonExtra.untitledIssue
    }
    
    func subtitle(for issue: Issue) -> String? {
        issue.issueDescription
    }
    
    func statusBadge(for issue: Issue) -> (text: String, color: Color)? {
        // Show priority if available, otherwise show status
        if let priority = issue.priority {
            let color: Color = {
                switch priority.lowercased() {
                case "critical": return .purple
                case "high": return .red
                case "medium": return .orange
                case "low": return .blue
                default: return .gray
                }
            }()
            return (LanguageManager.localizedPriority(priority), color)
        }

        guard let status = issue.status else { return nil }

        let color: Color = {
            switch status.lowercased() {
            case "open", "new": return .red
            case "in progress", "pending": return .orange
            case "resolved": return .green
            case "closed": return .gray
            default: return .blue
            }
        }()

        return (LanguageManager.localizedStatus(status), color)
    }
    
    func icon(for issue: Issue) -> String {
        // Use issue class icon if available
        if let className = issue.issue_class?.name.lowercased() {
            switch className {
            case let name where name.contains("repair"):
                return "wrench.and.screwdriver.fill"
            case let name where name.contains("replace"):
                return "arrow.triangle.2.circlepath"
            case let name where name.contains("thermal"):
                return "thermometer.medium"
            case let name where name.contains("violation"):
                return "exclamationmark.shield.fill"
            case let name where name.contains("safety"):
                return "exclamationmark.shield"
            case let name where name.contains("electrical"):
                return "bolt.fill"
            case let name where name.contains("maintenance"):
                return "wrench.and.screwdriver"
            case let name where name.contains("structural"):
                return "building.2"
            case let name where name.contains("environmental"):
                return "leaf.fill"
            case let name where name.contains("operational"):
                return "gearshape.fill"
            case let name where name.contains("inspection"):
                return "magnifyingglass.circle"
            case let name where name.contains("emergency"):
                return "exclamationmark.octagon.fill"
            case let name where name.contains("leak"):
                return "drop.triangle.fill"
            case let name where name.contains("corrosion"):
                return "aqi.medium"
            case let name where name.contains("failure"):
                return "xmark.octagon.fill"
            default:
                return "exclamationmark.triangle"
            }
        }
        
        // Fallback to status-based icon
        switch issue.status?.lowercased() {
        case "resolved", "closed":
            return "checkmark.circle.fill"
        case "in progress":
            return "arrow.triangle.2.circlepath"
        default:
            return "exclamationmark.circle"
        }
    }
    
    func iconColor(for issue: Issue) -> Color {
        switch issue.status?.lowercased() {
        case "open", "new": return .red
        case "in progress", "pending": return .orange
        case "resolved": return .green
        case "closed": return .gray
        default: return .gray
        }
    }
    
    func metadataItems(for issue: Issue) -> [MetadataItem] {
        var items: [MetadataItem] = []

        // Node label
        if let node = issue.node {
            items.append(MetadataItem(icon: "square.grid.2x2", text: node.label))
        }

        // Status (if priority is shown in badge, show status here)
        if issue.priority != nil, let status = issue.status {
            items.append(MetadataItem(icon: "circle.fill", text: LanguageManager.localizedStatus(status)))
        }

        return items
    }
    
    // MARK: - Filtering
    func availableFilters() -> [IssueFilter] {
        IssueFilter.allCases
    }
    
    func defaultFilter() -> IssueFilter {
        .open
    }
    
    func matches(entity issue: Issue, filter: IssueFilter) -> Bool {
        switch filter {
        case .all:
            return true
        case .open:
            return issue.status?.lowercased() == "open" || issue.status?.lowercased() == "new"
        case .inProgress:
            return issue.status?.lowercased() == "in progress"
        case .pending:
            return issue.status?.lowercased() == "pending"
        case .resolved:
            return issue.status?.lowercased() == "resolved"
        case .closed:
            return issue.status?.lowercased() == "closed"
        case .critical:
            // Check if issue class contains critical/emergency/urgent keywords
            if let className = issue.issue_class?.name.lowercased() {
                return className.contains("critical") || 
                       className.contains("emergency") || 
                       className.contains("urgent") ||
                       className.contains("safety")
            }
            return false
        case .withPhotos:
            let hasPhotos = !issue.photos.filter { !$0.is_deleted }.isEmpty
            let hasIRPhotos = !issue.ir_photos.filter { !$0.is_deleted }.isEmpty
            return hasPhotos || hasIRPhotos
        case .mySession:
            return issue.session?.id == appState.activeSession?.id
        }
    }
    
    func filterCount(for filter: IssueFilter, in issues: [Issue]) -> Int {
        issues.filter { matches(entity: $0, filter: filter) }.count
    }
    
    // MARK: - Search
    func searchText(for issue: Issue) -> String {
        var parts = [
            issue.title ?? "",
            issue.issueDescription ?? "",
            issue.issue_class?.name ?? "",
            issue.node?.label ?? "",
            issue.node?.location ?? "",
            issue.node?.room?.fullPath ?? "",
            issue.proposed_resolution ?? ""
        ]
        // Include core attribute names and values for detail search
        for detail in issue.details {
            parts.append(detail.name)
            if !detail.value.isEmpty {
                // For multi_select JSON arrays, extract readable values
                if let data = detail.value.data(using: .utf8),
                   let arr = try? JSONSerialization.jsonObject(with: data) as? [String] {
                    parts.append(contentsOf: arr)
                } else {
                    parts.append(detail.value)
                }
            }
        }
        return parts.joined(separator: " ")
    }
    
    // MARK: - Sorting
    func sortOptions() -> [SortOption<Issue>] {
        [
            SortOption(
                title: AppStrings.Common.createdDate,
                icon: "clock",
                comparator: { (issue1, issue2) in
                    let date1 = issue1.created_date ?? Date.distantPast
                    let date2 = issue2.created_date ?? Date.distantPast
                    return date1 > date2
                }
            ),
            SortOption(
                title: AppStrings.Common.modifiedDate,
                icon: "clock.arrow.circlepath",
                comparator: { (issue1, issue2) in
                    let date1 = issue1.modified_date ?? Date.distantPast
                    let date2 = issue2.modified_date ?? Date.distantPast
                    return date1 > date2
                }
            ),
            SortOption(
                title: AppStrings.Common.title,
                icon: "textformat",
                comparator: { (issue1, issue2) in
                    let title1 = issue1.title ?? ""
                    let title2 = issue2.title ?? ""
                    return title1.localizedCaseInsensitiveCompare(title2) == .orderedAscending
                }
            ),
            SortOption(
                title: AppStrings.Common.status,
                icon: "flag",
                comparator: { (issue1, issue2) in
                    // Sort by status priority: Open > In Progress > Resolved > Closed
                    let priority = ["open": 0, "new": 0, "in progress": 1, "pending": 1, "resolved": 2, "closed": 3]
                    let p1 = priority[issue1.status?.lowercased() ?? ""] ?? 99
                    let p2 = priority[issue2.status?.lowercased() ?? ""] ?? 99
                    return p1 < p2
                }
            ),
            SortOption(
                title: AppStrings.Common.room,
                icon: "door.left.hand.open",
                comparator: { (issue1, issue2) in
                    let path1 = issue1.node?.room?.fullPath
                    let path2 = issue2.node?.room?.fullPath
                    // Nulls last
                    if path1 == nil && path2 == nil { return false }
                    if path1 == nil { return false }
                    if path2 == nil { return true }
                    return path1!.localizedCaseInsensitiveCompare(path2!) == .orderedAscending
                },
                groupBy: { issue in
                    issue.node?.room?.fullPath
                },
                noGroupLabel: AppStrings.Common.noRoom,
                groupIcon: "door.left.hand.open"
            )
        ]
    }
    
    func defaultSort() -> SortOption<Issue> {
        sortOptions()[0] // Created Date
    }
    
    // MARK: - Actions
    func swipeActions(for issue: Issue) -> [SwipeAction<Issue>]? {
        var actions: [SwipeAction<Issue>] = []
        
        // Status toggle
        if issue.status?.lowercased() == "resolved" || issue.status?.lowercased() == "closed" {
            actions.append(SwipeAction(
                title: AppStrings.Common.reopen,
                icon: "arrow.uturn.backward",
                color: .orange
            ) { issue in
                onReopenIssue(issue)
            })
        } else {
            actions.append(SwipeAction(
                title: AppStrings.Common.resolve,
                icon: "checkmark",
                color: .green
            ) { issue in
                onResolveIssue(issue)
            })
        }
        
        // Delete
        actions.append(SwipeAction(
            title: AppStrings.Common.delete,
            icon: "trash",
            color: .red,
            role: .destructive
        ) { issue in
            onDeleteIssue(issue)
        })
        
        return actions
    }
    
    func onTap(entity issue: Issue) {
        onIssueTapped(issue)
    }
    
    func onCreate() {
        onCreateTapped()
    }
}
