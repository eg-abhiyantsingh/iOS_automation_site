import SwiftUI
import SwiftData

// MARK: - Generic Linked List View
struct EntityLinkedList<Configuration: EntityLinkedListProtocol>: View {
    let configuration: Configuration
    let parent: Configuration.ParentEntity
    @State private var filter: Configuration.FilterType
    @State private var searchText = ""
    @State private var debouncedSearchText = ""
    @State private var showingLinkingView = false
    @State private var selectedForLinking: Set<UUID> = []
    
    // Track the original linked IDs to detect changes
    @State private var originalLinkedIds: Set<Configuration.LinkedEntity.ID> = []
    
    // Optional binding to notify parent view of changes
    @Binding var hasChanges: Bool

    // Optional callback to notify parent of linked entities
    var onLinkedEntitiesChanged: (([Configuration.LinkedEntity]) -> Void)?

    @Environment(\.modelContext) private var modelContext

    init(configuration: Configuration,
         parent: Configuration.ParentEntity,
         hasChanges: Binding<Bool> = .constant(false),
         onLinkedEntitiesChanged: (([Configuration.LinkedEntity]) -> Void)? = nil) {
        self.configuration = configuration
        self.parent = parent
        self._filter = State(initialValue: configuration.defaultFilter())
        self._hasChanges = hasChanges
        self.onLinkedEntitiesChanged = onLinkedEntitiesChanged
    }
    
    // Computed property to detect if current state differs from original
    private var hasLocalChanges: Bool {
        let currentIds = Set(configuration.getLinkedEntities(from: parent).map { $0.id })
        return currentIds != originalLinkedIds
    }
    
    private var filteredEntities: [Configuration.LinkedEntity] {
        let allEntities = configuration.getLinkedEntities(from: parent)
        let nonDeleted = allEntities.filter { !configuration.isDeleted($0) }
        let filtered = nonDeleted.filter { configuration.shouldShow($0, with: filter) }
        let sorted = configuration.sortEntities(filtered)

        if debouncedSearchText.isEmpty {
            return sorted
        } else {
            return sorted.filter { entity in
                let title = configuration.displayTitle(for: entity)
                let subtitle = configuration.displaySubtitle(for: entity) ?? ""
                return "\(title) \(subtitle)".localizedCaseInsensitiveContains(debouncedSearchText)
            }
        }
    }
    
    var body: some View {
        let entities = filteredEntities
        VStack(spacing: 16) {
            // Header
            LinkedListHeader(
                configuration: configuration,
                parent: parent,
                filter: $filter,
                searchText: $searchText,
                entityCount: entities.count,
                onLinkExisting: configuration.linkingConfiguration() != nil ? {
                    if let linkConfig = configuration.linkingConfiguration() {
                        selectedForLinking = linkConfig.currentlyLinked(parent)
                        showingLinkingView = true
                    }
                } : nil
            )

            // Session Info (if applicable)
            if let sessionInfo = configuration.sessionInfo() {
                SessionInfoCard(info: sessionInfo)
            }

            // Content
            if entities.isEmpty {
                EntityEmptyState(
                    icon: configuration.emptyStateIcon,
                    message: configuration.emptyStateMessage
                )
            } else {
                LinkedListContent(
                    configuration: configuration,
                    entities: entities
                )
            }
        }
        .sheet(isPresented: $showingLinkingView) {
            if let linkConfig = configuration.linkingConfiguration() {
                EntityLinkingView(
                    parent: parent,
                    configuration: linkConfig,
                    selectedEntities: $selectedForLinking,
                    onSave: {
                        let availableEntities = linkConfig.fetchAvailable(parent)
                        linkConfig.applyLinking(parent, selectedForLinking, availableEntities)
                        showingLinkingView = false
                        // Update change tracking after linking changes
                        hasChanges = hasLocalChanges
                        // Notify parent of linked entities
                        let linkedEntities = configuration.getLinkedEntities(from: parent)
                        onLinkedEntitiesChanged?(linkedEntities)
                    }
                )
            }
        }
        .task(id: searchText) {
            if searchText.isEmpty {
                debouncedSearchText = ""
            } else {
                try? await Task.sleep(for: .milliseconds(300))
                debouncedSearchText = searchText
            }
        }
        .onAppear {
            // Store the initial linked IDs
            originalLinkedIds = Set(configuration.getLinkedEntities(from: parent).map { $0.id })
            // Update the binding with initial state
            hasChanges = hasLocalChanges
        }
        .onChange(of: hasLocalChanges) { _, newValue in
            // Update the binding whenever local changes are detected
            hasChanges = newValue
        }
    }
}

// MARK: - Header Component
private struct LinkedListHeader<Configuration: EntityLinkedListProtocol>: View {
    let configuration: Configuration
    let parent: Configuration.ParentEntity
    @Binding var filter: Configuration.FilterType
    @Binding var searchText: String
    let entityCount: Int
    let onLinkExisting: (() -> Void)?
    @State private var showingSearch = false
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                // Title with icon
                HStack(spacing: 8) {
                    Image(systemName: configuration.systemIcon)
                        .foregroundColor(.primary)
                    Text(configuration.title)
                        .font(.headline)
                    
                    if entityCount > 0 {
                        Text("(\(entityCount))")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                // Action buttons
                HStack(spacing: 12) {
                    // Search button (only if enabled)
                    if configuration.showSearch {
                        Button(action: { showingSearch.toggle() }) {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.blue)
                        }
                    }
                    
                    // Filter menu (only if enabled)
                    if configuration.showFilters {
                        FilterMenu(
                            configuration: configuration,
                            filter: $filter
                        )
                    }
                    
                    // Link existing button (if configured)
                    if let onLink = onLinkExisting,
                       let linkConfig = configuration.linkingConfiguration() {
                        Button(action: onLink) {
                            Label(linkConfig.linkButtonLabel, systemImage: "link")
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.blue.opacity(0.1))
                                .foregroundColor(.blue)
                                .cornerRadius(6)
                        }
                        .buttonStyle(.plain)
                    }
                    
                    // Add button (only if enabled)
                    if configuration.showAddButton {
                        Button(action: configuration.onAdd) {
                            // Use text button with link icon for forms, matching Link Assets style
                            if configuration.addButtonLabel == AppStrings.Forms.linkForms {
                                Label(configuration.addButtonLabel, systemImage: "link")
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.blue.opacity(0.1))
                                    .foregroundColor(.blue)
                                    .cornerRadius(6)
                            } else {
                                Image(systemName: "plus.circle.fill")
                                    .foregroundColor(.blue)
                                    .font(.title3)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            
            // Search bar (when visible)
            if showingSearch {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.Common.search, text: $searchText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.easeInOut(duration: 0.2), value: showingSearch)
            }
        }
    }
}

// MARK: - Filter Menu
private struct FilterMenu<Configuration: EntityLinkedListProtocol>: View {
    let configuration: Configuration
    @Binding var filter: Configuration.FilterType
    
    var body: some View {
        Menu {
            // For TaskListFilter
            if let taskFilter = filter as? TaskListFilter {
                ForEach([TaskListFilter.showAll, .showIncomplete, .showCompleted], id: \.self) { option in
                    Button(action: { 
                        filter = option as! Configuration.FilterType 
                    }) {
                        HStack {
                            Text(configuration.filterMenuLabel(for: option as! Configuration.FilterType))
                            Spacer()
                            if taskFilter == option {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            // For IssueListFilter
            else if let issueFilter = filter as? IssueListFilter {
                ForEach([IssueListFilter.showAll, .showUnresolved, .showResolved], id: \.self) { option in
                    Button(action: { 
                        filter = option as! Configuration.FilterType 
                    }) {
                        HStack {
                            Text(configuration.filterMenuLabel(for: option as! Configuration.FilterType))
                            Spacer()
                            if issueFilter == option {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
            // Fallback for generic CommonListFilter
            else if let booleanFilter = filter as? CommonListFilter {
                Button(action: { 
                    filter = CommonListFilter.showAll as! Configuration.FilterType 
                }) {
                    HStack {
                        Text(configuration.filterMenuLabel(for: CommonListFilter.showAll as! Configuration.FilterType))
                        Spacer()
                        if booleanFilter == .showAll {
                            Image(systemName: "checkmark")
                                .foregroundColor(.accentColor)
                        }
                    }
                }
                
                Button(action: { 
                    filter = CommonListFilter.showFiltered as! Configuration.FilterType 
                }) {
                    HStack {
                        Text(configuration.filterMenuLabel(for: CommonListFilter.showFiltered as! Configuration.FilterType))
                        Spacer()
                        if booleanFilter == .showFiltered {
                            Image(systemName: "checkmark")
                                .foregroundColor(.accentColor)
                        }
                    }
                }
            }
        } label: {
            Image(systemName: "line.3.horizontal.decrease.circle")
                .foregroundColor(.blue)
        }
    }
}

// MARK: - Session Info Card
private struct SessionInfoCard: View {
    let info: SessionDisplayInfo
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    ActiveSessionBadge()
                    Text(info.title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                if let subtitle = info.subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if let warning = info.warningMessage {
                    Label(warning, systemImage: "exclamationmark.triangle")
                        .font(.caption2)
                        .foregroundColor(.orange)
                }
            }
            
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

// MARK: - Content List
private struct LinkedListContent<Configuration: EntityLinkedListProtocol>: View {
    let configuration: Configuration
    let entities: [Configuration.LinkedEntity]
    
    var body: some View {
        VStack(spacing: 8) {
            ForEach(entities, id: \.id) { entity in
                EntityRow(
                    configuration: configuration,
                    entity: entity
                )
            }
        }
    }
}

// MARK: - Entity Row
private struct EntityRow<Configuration: EntityLinkedListProtocol>: View {
    let configuration: Configuration
    let entity: Configuration.LinkedEntity
    
    var body: some View {
        Button(action: { configuration.onSelect(entity) }) {
            HStack(spacing: 12) {
                // Leading icon (if any)
                if let icon = configuration.displayIcon(for: entity) {
                    Image(systemName: icon)
                        .foregroundColor(configuration.displayIconColor(for: entity))
                        .font(.title3)
                }
                
                // Content
                VStack(alignment: .leading, spacing: 2) {
                    Text(configuration.displayTitle(for: entity))
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    if let subtitle = configuration.displaySubtitle(for: entity) {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }
                
                Spacer()
                
                // Badge (if any)
                if let badge = configuration.displayBadge(for: entity) {
                    Text(badge.text)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(badge.color)
                        .cornerRadius(4)
                }
                
                // Accessory
                if let accessory = configuration.displayAccessory(for: entity) {
                    AccessoryView(type: accessory)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
        .buttonStyle(PlainButtonStyle())
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                configuration.onDelete(entity)
            } label: {
                Label(AppStrings.Common.delete, systemImage: "trash")
            }
        }
    }
}

// MARK: - Accessory View
private struct AccessoryView: View {
    let type: EntityAccessoryType
    
    var body: some View {
        switch type {
        case .chevron:
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(Color.secondary.opacity(0.5))
                
        case .checkmark(let isChecked):
            Image(systemName: isChecked ? "checkmark.circle.fill" : "circle")
                .foregroundColor(isChecked ? .green : .gray)
                
        case .badge(let text, let color):
            Text(text)
                .font(.caption2)
                .foregroundColor(.white)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(color)
                .cornerRadius(4)
                
        case .icon(let systemName, let color):
            Image(systemName: systemName)
                .font(.caption)
                .foregroundColor(color)
                
        case .none:
            EmptyView()
        }
    }
}

// MARK: - Entity Linking View
struct EntityLinkingView<Parent, Child: Identifiable>: View {
    let parent: Parent
    let configuration: EntityLinkingConfiguration<Parent, Child>
    @Binding var selectedEntities: Set<UUID>
    let onSave: () -> Void
    
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    
    @State private var searchText = ""
    @State private var debouncedSearchText = ""
    @State private var availableEntities: [Child] = []
    @State private var initialSelection: Set<UUID> = []

    private var filteredEntities: [Child] {
        if debouncedSearchText.isEmpty {
            return availableEntities
        }

        return availableEntities.filter { entity in
            let display = configuration.displayInSelector(entity)
            return "\(display.title) \(display.subtitle ?? "")".localizedCaseInsensitiveContains(debouncedSearchText)
        }
    }
    
    private var hasChanges: Bool {
        selectedEntities != initialSelection
    }

    // Dynamic title based on selection
    private var navigationTitle: String {
        if selectedEntities.isEmpty {
            return configuration.linkViewTitle
        } else {
            return AppStrings.Forms.selectedCount(selectedEntities.count)
        }
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(filteredEntities, id: \.id) { entity in
                    EntitySelectionRow(
                        entity: entity,
                        configuration: configuration,
                        isSelected: selectedEntities.contains(entity.id as! UUID),
                        wasInitiallySelected: initialSelection.contains(entity.id as! UUID),
                        onToggle: {
                            if selectedEntities.contains(entity.id as! UUID) {
                                selectedEntities.remove(entity.id as! UUID)
                            } else {
                                selectedEntities.insert(entity.id as! UUID)
                            }
                        }
                    )
                }
            }
            .searchable(text: $searchText, prompt: configuration.searchPrompt)
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        selectedEntities = initialSelection
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.save) {
                        onSave()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(!hasChanges)
                }

                ToolbarItemGroup(placement: .bottomBar) {
                    Button(AppStrings.Forms.selectAll) {
                        selectedEntities = Set(availableEntities.compactMap { $0.id as? UUID })
                    }
                    .disabled(selectedEntities.count == availableEntities.count)

                    Spacer()

                    Button(AppStrings.Forms.clearAllSelection) {
                        selectedEntities.removeAll()
                    }
                    .disabled(selectedEntities.isEmpty)
                }
            }
        }
        .task(id: searchText) {
            if searchText.isEmpty {
                debouncedSearchText = ""
            } else {
                try? await Task.sleep(for: .milliseconds(300))
                debouncedSearchText = searchText
            }
        }
        .onAppear {
            availableEntities = configuration.fetchAvailable(parent)
            initialSelection = selectedEntities
        }
    }
}

// MARK: - Entity Selection Row
private struct EntitySelectionRow<Parent, Child: Identifiable>: View {
    let entity: Child
    let configuration: EntityLinkingConfiguration<Parent, Child>
    let isSelected: Bool
    let wasInitiallySelected: Bool
    let onToggle: () -> Void
    
    private var statusIcon: String {
        if isSelected && !wasInitiallySelected {
            return "plus.circle.fill"  // Will be added
        } else if !isSelected && wasInitiallySelected {
            return "minus.circle.fill"  // Will be removed
        } else if isSelected {
            return "checkmark.circle.fill"  // Already linked
        } else {
            return "circle"  // Not linked
        }
    }
    
    private var statusColor: Color {
        if isSelected && !wasInitiallySelected {
            return .green  // Adding
        } else if !isSelected && wasInitiallySelected {
            return .red  // Removing
        } else if isSelected {
            return .blue  // Selected
        } else {
            return .gray  // Not selected
        }
    }
    
    var body: some View {
        let display = configuration.displayInSelector(entity)
        
        HStack {
            // Icon
            if let icon = display.icon {
                Image(systemName: icon)
                    .foregroundColor(display.iconColor)
                    .frame(width: 30)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(display.title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    if display.isAlreadyLinked {
                        Text(AppStrings.Forms.linkedBadge)
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.blue)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                    
                    if let badge = display.badge {
                        Text(badge)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                    }
                }
                
                if let subtitle = display.subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            
            Spacer()
            
            Image(systemName: statusIcon)
                .foregroundColor(statusColor)
                .font(.title2)
                .animation(.easeInOut(duration: 0.2), value: isSelected)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture(perform: onToggle)
        .disabled(configuration.canLink?(entity) == false)
        .opacity(configuration.canLink?(entity) == false ? 0.5 : 1.0)
    }
}

// MARK: - Empty State
private struct EntityEmptyState: View {
    let icon: String
    let message: String
    
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 40))
                .foregroundColor(.gray.opacity(0.5))
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
    }
}