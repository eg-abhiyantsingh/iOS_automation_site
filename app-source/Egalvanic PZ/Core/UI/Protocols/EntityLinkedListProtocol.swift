import SwiftUI
import SwiftData

// MARK: - Core Protocol for Linked Lists
protocol EntityLinkedListProtocol {
    associatedtype ParentEntity
    associatedtype LinkedEntity: Identifiable where LinkedEntity.ID: Hashable
    associatedtype FilterType: Hashable
    
    // Core Configuration
    var title: String { get }
    var systemIcon: String { get }
    var emptyStateMessage: String { get }
    var emptyStateIcon: String { get }
    var addButtonLabel: String { get }
    var showSearch: Bool { get }
    var showFilters: Bool { get }
    var showAddButton: Bool { get }
    
    // Data Access
    func getLinkedEntities(from parent: ParentEntity) -> [LinkedEntity]
    func isDeleted(_ entity: LinkedEntity) -> Bool
    
    // Filtering
    func defaultFilter() -> FilterType
    func filterMenuLabel(for filter: FilterType) -> String
    func shouldShow(_ entity: LinkedEntity, with filter: FilterType) -> Bool
    
    // Display Configuration
    func displayTitle(for entity: LinkedEntity) -> String
    func displaySubtitle(for entity: LinkedEntity) -> String?
    func displayIcon(for entity: LinkedEntity) -> String?
    func displayIconColor(for entity: LinkedEntity) -> Color
    func displayBadge(for entity: LinkedEntity) -> (text: String, color: Color)?
    func displayAccessory(for entity: LinkedEntity) -> EntityAccessoryType?
    
    // Sorting
    func sortEntities(_ entities: [LinkedEntity]) -> [LinkedEntity]
    
    // Actions
    func onSelect(_ entity: LinkedEntity)
    func onDelete(_ entity: LinkedEntity)
    func onAdd()
    
    // Optional Session Display
    func sessionInfo() -> SessionDisplayInfo?
    
    // MARK: - Linking Configuration (New)
    func linkingConfiguration() -> EntityLinkingConfiguration<ParentEntity, LinkedEntity>?
}

// MARK: - Supporting Types
enum EntityAccessoryType {
    case chevron
    case checkmark(isChecked: Bool)
    case badge(text: String, color: Color)
    case icon(systemName: String, color: Color)
    case none
}

struct SessionDisplayInfo {
    let title: String
    let subtitle: String?
    let warningMessage: String?
}

// MARK: - Linking Configuration
enum LinkingRelationship {
    case oneToOne   // Child has single parent FK, parent has single child ref
    case oneToMany  // Child has parent FK, parent has array of children
    case manyToMany // Junction table or array on both sides
}

struct EntityLinkingConfiguration<Parent, Child: Identifiable> {
    let relationship: LinkingRelationship
    let linkButtonLabel: String
    let linkViewTitle: String
    let searchPrompt: String
    
    // Data fetching
    let fetchAvailable: (Parent) -> [Child]
    let currentlyLinked: (Parent) -> Set<UUID>
    
    // Linking operations
    let applyLinking: (Parent, Set<UUID>, [Child]) -> Void
    
    // Display configuration for selection view
    let displayInSelector: (Child) -> EntitySelectorDisplay
    
    // Optional: Check if entity can be linked
    let canLink: ((Child) -> Bool)?
    
    init(
        relationship: LinkingRelationship,
        linkButtonLabel: String = "Link Existing",
        linkViewTitle: String = "Select Items to Link",
        searchPrompt: String = "Search...",
        fetchAvailable: @escaping (Parent) -> [Child],
        currentlyLinked: @escaping (Parent) -> Set<UUID>,
        applyLinking: @escaping (Parent, Set<UUID>, [Child]) -> Void,
        displayInSelector: @escaping (Child) -> EntitySelectorDisplay,
        canLink: ((Child) -> Bool)? = nil
    ) {
        self.relationship = relationship
        self.linkButtonLabel = linkButtonLabel
        self.linkViewTitle = linkViewTitle
        self.searchPrompt = searchPrompt
        self.fetchAvailable = fetchAvailable
        self.currentlyLinked = currentlyLinked
        self.applyLinking = applyLinking
        self.displayInSelector = displayInSelector
        self.canLink = canLink
    }
}

struct EntitySelectorDisplay {
    let title: String
    let subtitle: String?
    let icon: String?
    let iconColor: Color
    let badge: String?
    let isAlreadyLinked: Bool
    
    init(
        title: String,
        subtitle: String? = nil,
        icon: String? = nil,
        iconColor: Color = .primary,
        badge: String? = nil,
        isAlreadyLinked: Bool = false
    ) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.iconColor = iconColor
        self.badge = badge
        self.isAlreadyLinked = isAlreadyLinked
    }
}

// MARK: - Default Implementations
extension EntityLinkedListProtocol {
    var showSearch: Bool { true }
    var showFilters: Bool { true }
    var showAddButton: Bool { true }
    func displaySubtitle(for entity: LinkedEntity) -> String? { nil }
    func displayIcon(for entity: LinkedEntity) -> String? { nil }
    func displayIconColor(for entity: LinkedEntity) -> Color { .primary }
    func displayBadge(for entity: LinkedEntity) -> (text: String, color: Color)? { nil }
    func displayAccessory(for entity: LinkedEntity) -> EntityAccessoryType? { .chevron }
    func sessionInfo() -> SessionDisplayInfo? { nil }
    func linkingConfiguration() -> EntityLinkingConfiguration<ParentEntity, LinkedEntity>? { nil }
}

// MARK: - Convenience Protocol for Simple Boolean Filters
protocol BooleanFilterable {
    static var showAll: Self { get }
    static var showFiltered: Self { get }
}

// MARK: - Common Filter Types
enum CommonListFilter: Hashable, BooleanFilterable {
    case showAll
    case showFiltered
}

// MARK: - Row Configuration Protocol
protocol EntityRowConfigurable {
    associatedtype Entity
    
    func rowBackground(for entity: Entity) -> Color
    func rowOpacity(for entity: Entity) -> Double
    func isSwipeEnabled(for entity: Entity) -> Bool
    func swipeActions(for entity: Entity) -> [EntitySwipeAction]
}

struct EntitySwipeAction {
    let label: String
    let systemImage: String
    let role: ButtonRole?
    let tint: Color
    let action: () -> Void
}

// MARK: - Default Row Configuration
extension EntityRowConfigurable {
    func rowBackground(for entity: Entity) -> Color { Color(.systemGray6) }
    func rowOpacity(for entity: Entity) -> Double { 1.0 }
    func isSwipeEnabled(for entity: Entity) -> Bool { true }
}