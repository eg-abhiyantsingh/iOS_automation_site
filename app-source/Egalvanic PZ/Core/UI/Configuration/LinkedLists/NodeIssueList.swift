import SwiftUI
import SwiftData

// MARK: - Issue List Filter
public enum IssueListFilter: Hashable, CaseIterable {
    case showAll
    case showUnresolved
    case showResolved
}

// MARK: - Node Issue List Configuration
struct NodeIssueList: EntityLinkedListProtocol {
    typealias ParentEntity = NodeV2
    typealias LinkedEntity = Issue
    typealias FilterType = IssueListFilter
    
    // Dependencies
    let appState: AppStateManager
    let onSelectIssue: (Issue) -> Void
    let onDeleteIssue: (Issue) -> Void
    let onAddIssue: () -> Void
    
    // MARK: - Core Configuration
    var title: String { AppStrings.Issues.issues }
    var systemIcon: String { "exclamationmark.triangle" }
    var emptyStateMessage: String { AppStrings.Issues.noIssues }
    var emptyStateIcon: String { "checkmark.shield" }
    var addButtonLabel: String { AppStrings.Sessions.addIssue }
    var showSearch: Bool { false }
    
    // MARK: - Data Access
    func getLinkedEntities(from parent: NodeV2) -> [Issue] {
        parent.issues
    }
    
    func isDeleted(_ entity: Issue) -> Bool {
        entity.is_deleted
    }
    
    // MARK: - Filtering
    func defaultFilter() -> IssueListFilter {
        .showUnresolved
    }
    
    func filterMenuLabel(for filter: IssueListFilter) -> String {
        switch filter {
        case .showAll:
            return AppStrings.Issues.showAllIssues
        case .showUnresolved:
            return AppStrings.Issues.showUnresolvedOnly
        case .showResolved:
            return AppStrings.Issues.showResolvedOnly
        }
    }
    
    func shouldShow(_ entity: Issue, with filter: IssueListFilter) -> Bool {
        switch filter {
        case .showAll:
            return true
        case .showUnresolved:
            return entity.status != "Resolved"
        case .showResolved:
            return entity.status == "Resolved"
        }
    }
    
    // MARK: - Display Configuration
    func displayTitle(for entity: Issue) -> String {
        entity.title ?? AppStrings.CommonExtra.untitledIssue
    }
    
    func displaySubtitle(for entity: Issue) -> String? {
        if let description = entity.issueDescription, !description.isEmpty {
            return description
        }
        if let issueClass = entity.issue_class {
            return issueClass.name
        }
        return nil
    }
    
    func displayIcon(for entity: Issue) -> String? {
        if entity.status == "Resolved" {
            return "checkmark.circle.fill"
        } else {
            return "exclamationmark.triangle.fill"
        }
    }
    
    func displayIconColor(for entity: Issue) -> Color {
        if entity.status == "Resolved" {
            return .green
        } else {
            return .orange
        }
    }
    
    func displayBadge(for entity: Issue) -> (text: String, color: Color)? {
        // Show status badge
        if let status = entity.status {
            return (LanguageManager.localizedStatus(status), {
                switch status.lowercased() {
                case "resolved": return .green
                case "in progress": return .blue
                case "open": return .orange
                default: return .gray
                }
            }())
        }
        
        // Default status if none specified
        return ("Open", .orange)
    }
    
    func displayAccessory(for entity: Issue) -> EntityAccessoryType? {
        .chevron
    }
    
    // MARK: - Sorting
    func sortEntities(_ entities: [Issue]) -> [Issue] {
        entities.sorted { (issue1: Issue, issue2: Issue) -> Bool in
            // Sort unresolved first
            if (issue1.status == "Resolved") != (issue2.status == "Resolved") {
                return issue1.status != "Resolved"
            }
            
            // Then by creation date (newest first)
            let date1 = issue1.created_date ?? Date.distantPast
            let date2 = issue2.created_date ?? Date.distantPast
            return date1 > date2
        }
    }
    
    // MARK: - Actions
    func onSelect(_ entity: Issue) {
        onSelectIssue(entity)
    }
    
    func onDelete(_ entity: Issue) {
        onDeleteIssue(entity)
    }
    
    func onAdd() {
        onAddIssue()
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