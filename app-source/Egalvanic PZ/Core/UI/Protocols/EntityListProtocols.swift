import SwiftUI
import SwiftData

// MARK: - Core Protocol for List Items
protocol ListableEntity {
    var id: UUID { get }
    var isDeleted: Bool { get }
    var createdDate: Date? { get }
    var modifiedDate: Date? { get }
}

// MARK: - Configuration Protocol
protocol EntityListConfiguration {
    associatedtype Entity: ListableEntity & Identifiable
    associatedtype FilterType: CaseIterable, Hashable, CustomStringConvertible
    
    // Basic Info
    var entityName: String { get }
    var pluralEntityName: String { get }
    var emptyStateMessage: String { get }
    var emptyStateIcon: String { get }
    
    // Display Methods
    func title(for entity: Entity) -> String
    /// Max lines for the row title. Default 1 (see extension).
    var titleLineLimit: Int { get }
    func subtitle(for entity: Entity) -> String?
    func statusBadge(for entity: Entity) -> (text: String, color: Color)?
    func icon(for entity: Entity) -> String
    func iconColor(for entity: Entity) -> Color
    
    // Additional metadata lines (like location, due date, etc.)
    func metadataItems(for entity: Entity) -> [MetadataItem]

    /// Optional line rendered below the metadata row. Used for things
    /// like "Due: 5/3/26" where the entity wants a labeled value on
    /// its own line. Default `nil` (no footnote).
    func footnote(for entity: Entity) -> (text: String, color: Color)?
    
    // Filtering
    func availableFilters() -> [FilterType]
    func defaultFilter() -> FilterType
    func matches(entity: Entity, filter: FilterType) -> Bool
    func filterCount(for filter: FilterType, in entities: [Entity]) -> Int
    
    // Search
    func searchText(for entity: Entity) -> String
    
    // Sorting
    func sortOptions() -> [SortOption<Entity>]
    func defaultSort() -> SortOption<Entity>
    
    // Actions
    func swipeActions(for entity: Entity) -> [SwipeAction<Entity>]?
    /// Long-press context menu items. Default `nil` (no menu). Each
    /// conformer that wants a menu returns an array of actions; an
    /// empty array suppresses the menu the same as `nil`.
    func contextMenuActions(for entity: Entity) -> [ContextMenuAction<Entity>]?
    func onTap(entity: Entity)
    func onCreate()
}

extension EntityListConfiguration {
    func contextMenuActions(for entity: Entity) -> [ContextMenuAction<Entity>]? { nil }
    func footnote(for entity: Entity) -> (text: String, color: Color)? { nil }
    var titleLineLimit: Int { 1 }
}

// MARK: - Supporting Types
struct MetadataItem {
    let icon: String
    let text: String
    let color: Color
    /// When true, this chip expands to fill the remaining row width and
    /// truncates at the edge (rather than sitting at its intrinsic size).
    /// Used for long values like a work-order name that should use the
    /// available space on the right. Default false.
    let flexible: Bool

    init(icon: String, text: String, color: Color = .secondary, flexible: Bool = false) {
        self.icon = icon
        self.text = text
        self.color = color
        self.flexible = flexible
    }
}

struct SortOption<Entity>: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    let comparator: (Entity, Entity) -> Bool
    /// When non-nil, the list groups entities by the returned key instead of a flat list.
    let groupBy: ((Entity) -> String?)?
    /// Label shown for the group when `groupBy` returns nil (e.g. "No Room").
    let noGroupLabel: String
    /// SF Symbol icon shown in group section headers.
    let groupIcon: String?

    init(title: String, icon: String, comparator: @escaping (Entity, Entity) -> Bool, groupBy: ((Entity) -> String?)? = nil, noGroupLabel: String = "", groupIcon: String? = nil) {
        self.title = title
        self.icon = icon
        self.comparator = comparator
        self.groupBy = groupBy
        self.noGroupLabel = noGroupLabel
        self.groupIcon = groupIcon
    }
}

struct ContextMenuAction<Entity> {
    let title: String
    let icon: String
    let role: ButtonRole?
    let action: (Entity) -> Void

    init(title: String, icon: String, role: ButtonRole? = nil, action: @escaping (Entity) -> Void) {
        self.title = title
        self.icon = icon
        self.role = role
        self.action = action
    }
}

struct SwipeAction<Entity> {
    let title: String
    let icon: String
    let color: Color
    let role: ButtonRole?
    let action: (Entity) -> Void

    init(title: String, icon: String, color: Color = .blue, role: ButtonRole? = nil, action: @escaping (Entity) -> Void) {
        self.title = title
        self.icon = icon
        self.color = color
        self.role = role
        self.action = action
    }
}

// MARK: - Filter Chip Protocol
protocol FilterChipDisplayable {
    var displayName: String { get }
    var icon: String? { get }
    var color: Color { get }
}

// Default implementation for common cases
extension FilterChipDisplayable {
    var icon: String? { nil }
    var color: Color { .blue }
}
