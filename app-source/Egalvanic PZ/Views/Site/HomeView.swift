import SwiftUI
import SwiftData
import UIKit

// MARK: - Calendar View Mode
enum CalendarViewMode: String, CaseIterable {
    case day = "Day"
    case week = "Week"
    case month = "Month"

    var localizedTitle: String {
        switch self {
        case .day: return AppStrings.Home.calendarDay
        case .week: return AppStrings.Home.calendarWeek
        case .month: return AppStrings.Home.calendarMonth
        }
    }
}

// MARK: - Home View
struct HomeView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    // Optional callbacks for when shown after login (before site selection)
    var onLogout: (() -> Void)? = nil
    var onSiteSelected: ((UUID, String?) -> Void)? = nil

    /// Whether this view is shown in "after login" mode (no site selected yet)
    private var isAfterLoginMode: Bool {
        onLogout != nil
    }

    @State private var calendarViewMode: CalendarViewMode = .day
    @State private var selectedDate: Date = Date()
    @State private var allWorkBlocks: [SessionWorkBlock] = []

    // Work blocks grouped by time period
    @State private var todayBlocks: [SessionWorkBlock] = []
    @State private var thisWeekBlocks: [SessionWorkBlock] = []
    @State private var nextWeekBlocks: [SessionWorkBlock] = []
    @State private var laterBlocks: [SessionWorkBlock] = []

    // Calendar collapse state
    @State private var isCalendarExpanded: Bool = true

    // Site switching alert state
    @State private var showSwitchSiteAlert = false
    @State private var showAlreadyOnSiteAlert = false
    @State private var selectedWorkBlock: SessionWorkBlock? = nil

    private let calendarHeight: CGFloat = 340

    // Loading state for fetching user schedule
    @State private var isLoadingSchedule = false

    /// Check if the work block's site matches the current site
    private func isCurrentSite(_ workBlock: SessionWorkBlock) -> Bool {
        guard let sldId = workBlock.sldId else { return false }
        return AppStateManager.shared.activeSLDId == sldId
    }

    /// Handle tap on a work block - show appropriate alert
    private func handleWorkBlockTap(_ workBlock: SessionWorkBlock) {
        selectedWorkBlock = workBlock
        if isCurrentSite(workBlock) {
            showAlreadyOnSiteAlert = true
        } else {
            showSwitchSiteAlert = true
        }
    }

    /// Switch to the selected work block's site
    private func switchToSite() {
        guard let workBlock = selectedWorkBlock,
              let sldId = workBlock.sldId else { return }

        if isAfterLoginMode {
            // In after-login mode, use the callback to handle site selection
            onSiteSelected?(sldId, workBlock.siteName)
        } else {
            // Normal mode - switch SLD directly
            Task {
                do {
                    try await SLDService.shared.switchToSLD(sldId, siteName: workBlock.siteName, modelContext: modelContext)
                    await MainActor.run { dismiss() }
                } catch {
                    AppLogger.log(.error, "Error switching to SLD: \(error)", category: .ui)
                    await MainActor.run { dismiss() }
                }
            }
        }
    }

    /// Returns a set of dates (normalized to start of day) that have work blocks
    private var datesWithWorkBlocks: Set<Date> {
        let calendar = Calendar.current
        var dates = Set<Date>()
        for block in allWorkBlocks {
            if let startOfDay = calendar.date(from: calendar.dateComponents([.year, .month, .day], from: block.start_time)) {
                dates.insert(startOfDay)
            }
        }
        return dates
    }

    /// Get work blocks for a specific date
    private func workBlocksForDate(_ date: Date) -> [SessionWorkBlock] {
        let calendar = Calendar.current
        return allWorkBlocks.filter { block in
            calendar.isDate(block.start_time, inSameDayAs: date)
        }.sorted { $0.start_time < $1.start_time }
    }

    private var timeBasedGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<12:
            return AppStrings.Home.goodMorning
        case 12..<17:
            return AppStrings.Home.goodAfternoon
        default:
            return AppStrings.Home.goodEvening
        }
    }

    private var userName: String {
        if let givenName = AuthService.shared.currentUser?.given_name {
            return givenName
        } else if let name = AuthService.shared.currentUser?.name {
            return name
        }
        return ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Greeting Section
                    greetingSection

                    // Calendar Section
                    calendarSection

                    // This Week Section (Work Blocks)
                    thisWeekSection
                }
                .padding()
            }
            .background(Color(UIColor.systemBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if isAfterLoginMode {
                            onLogout?()
                        } else {
                            dismiss()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: isAfterLoginMode ? "rectangle.portrait.and.arrow.right" : "chevron.left")
                            Text(isAfterLoginMode ? AppStrings.Common.logout : AppStrings.Common.back)
                        }
                    }
                }

                ToolbarItem(placement: .principal) {
                    Text(AppStrings.Home.schedule)
                        .font(.headline)
                }
            }
            .alert(AppStrings.Home.switchSite, isPresented: $showSwitchSiteAlert) {
                Button(AppStrings.Home.switchAction) {
                    switchToSite()
                }
                Button(AppStrings.Common.cancel, role: .cancel) {
                    selectedWorkBlock = nil
                }
            } message: {
                if let workBlock = selectedWorkBlock, let siteName = workBlock.siteName {
                    Text(AppStrings.Home.switchToSitePrompt(siteName: siteName))
                } else {
                    Text(AppStrings.Home.switchToSitePromptGeneric)
                }
            }
            .alert(AppStrings.Home.currentSite, isPresented: $showAlreadyOnSiteAlert) {
                Button(AppStrings.Common.ok, role: .cancel) {
                    selectedWorkBlock = nil
                }
            } message: {
                if let siteName = selectedWorkBlock?.siteName {
                    Text(AppStrings.Home.alreadyOnSite(siteName: siteName))
                } else {
                    Text(AppStrings.Home.alreadyOnSiteGeneric)
                }
            }
        }
    }

    // MARK: - Greeting Section
    private var greetingSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(timeBasedGreeting + (userName.isEmpty ? "" : ", \(userName)"))
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(.primary)

            Text(Date(), style: .date)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 8)
    }

    // MARK: - Calendar Section
    private var calendarSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Calendar Header with View Mode Picker - Tappable to collapse/expand
            HStack {
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        isCalendarExpanded.toggle()
                    }
                }) {
                    HStack(spacing: 8) {
                        Text(AppStrings.Home.calendar)
                            .font(.headline)
                            .foregroundColor(.primary)

                        Image(systemName: isCalendarExpanded ? "chevron.up" : "chevron.down")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .buttonStyle(.plain)

                Spacer()

                if isCalendarExpanded {
                    Button {
                        selectedDate = Date()
                    } label: {
                        let isToday = Calendar.current.isDateInToday(selectedDate)
                        Text(AppStrings.Home.calendarToday)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(isToday ? .gray : .blue)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(isToday ? Color.gray.opacity(0.12) : Color.blue.opacity(0.12))
                            .cornerRadius(8)
                    }
                    .disabled(Calendar.current.isDateInToday(selectedDate))
                    .padding(.trailing, 8)

                    Picker(AppStrings.Home.calendarView, selection: $calendarViewMode) {
                        ForEach(CalendarViewMode.allCases, id: \.self) { mode in
                            Text(mode.localizedTitle).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 180)
                }
            }

            // Calendar Content (collapsible)
            if isCalendarExpanded {
                VStack(spacing: 0) {
                    switch calendarViewMode {
                    case .day:
                        DayCalendarView(selectedDate: $selectedDate, workBlocks: workBlocksForDate(selectedDate)) { block in
                            handleWorkBlockTap(block)
                        }
                    case .week:
                        WeekCalendarView(selectedDate: $selectedDate, datesWithWorkBlocks: datesWithWorkBlocks, workBlocksForDate: workBlocksForDate) { block in
                            handleWorkBlockTap(block)
                        }
                    case .month:
                        MonthCalendarView(selectedDate: $selectedDate, datesWithWorkBlocks: datesWithWorkBlocks, workBlocksForDate: workBlocksForDate) { block in
                            handleWorkBlockTap(block)
                        }
                    }
                }
                .frame(height: calendarViewMode == .month ? nil : calendarHeight)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(16)
            }
        }
        .onAppear {
            loadAllWorkBlocks()
            fetchUserScheduleFromAPI()
        }
    }

    // MARK: - Upcoming Work Section (Work Blocks)
    private var thisWeekSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            // Today
            workBlockGroup(title: AppStrings.Home.today, blocks: todayBlocks, emptyMessage: AppStrings.Home.noScheduledWorkToday)

            // This Week (rest of the week, excluding today)
            if !thisWeekBlocks.isEmpty {
                workBlockGroup(title: AppStrings.Home.thisWeek, blocks: thisWeekBlocks, emptyMessage: nil)
            }

            // Next Week
            if !nextWeekBlocks.isEmpty {
                workBlockGroup(title: AppStrings.Home.nextWeek, blocks: nextWeekBlocks, emptyMessage: nil)
            }

            // Later
            if !laterBlocks.isEmpty {
                workBlockGroup(title: AppStrings.Home.later, blocks: laterBlocks, emptyMessage: nil)
            }

            // Show "View Sites" button in after-login mode when no work is scheduled
            if isAfterLoginMode && todayBlocks.isEmpty && thisWeekBlocks.isEmpty && nextWeekBlocks.isEmpty && laterBlocks.isEmpty {
                viewSitesButton
            }
        }
        .onAppear {
            loadUpcomingWorkBlocks()
        }
    }

    /// Button to navigate to site selection (shown in after-login mode when no work blocks)
    @ViewBuilder
    private var viewSitesButton: some View {
        VStack(spacing: 16) {
            Text(AppStrings.Home.viewSitesPrompt)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button(action: {
                // Signal to show site selector by clearing the after-login mode without selecting a site
                // This will fall through to the sldSelectorView in ContentView
                onSiteSelected?(UUID(), nil)  // Pass empty UUID to signal "show sites"
            }) {
                HStack {
                    Image(systemName: "building.2")
                    Text(AppStrings.Home.viewSites)
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .cornerRadius(12)
            }
        }
        .padding(.top, 20)
    }

    /// Helper view for a group of work blocks with a title
    @ViewBuilder
    private func workBlockGroup(title: String, blocks: [SessionWorkBlock], emptyMessage: String?) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)

            if blocks.isEmpty {
                if let message = emptyMessage {
                    VStack(spacing: 12) {
                        Image(systemName: "calendar.badge.clock")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text(message)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(12)
                }
            } else {
                ForEach(blocks, id: \.id) { workBlock in
                    WorkBlockItemView(workBlock: workBlock) {
                        handleWorkBlockTap(workBlock)
                    }
                }
            }
        }
    }

    /// Load upcoming work blocks grouped into Today, This Week, Next Week, and Later
    private func loadUpcomingWorkBlocks() {
        let calendar = Calendar.current
        let now = Date()

        // Calculate date boundaries
        guard let startOfToday = calendar.date(from: calendar.dateComponents([.year, .month, .day], from: now)),
              let startOfTomorrow = calendar.date(byAdding: .day, value: 1, to: startOfToday),
              let startOfThisWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)),
              let startOfNextWeek = calendar.date(byAdding: .weekOfYear, value: 1, to: startOfThisWeek),
              let startOfLater = calendar.date(byAdding: .weekOfYear, value: 2, to: startOfThisWeek),
              let threeMonthsFromToday = calendar.date(byAdding: .month, value: 3, to: startOfToday) else {
            todayBlocks = []
            thisWeekBlocks = []
            nextWeekBlocks = []
            laterBlocks = []
            return
        }

        // Get current user ID for filtering sessions the user is assigned to
        let currentUserId = AppStateManager.shared.userId

        do {
            // Fetch work blocks from today up to 3 months out
            let descriptor = FetchDescriptor<SessionWorkBlock>(
                predicate: #Predicate<SessionWorkBlock> { block in
                    block.is_deleted == false &&
                    block.start_time >= startOfToday &&
                    block.start_time < threeMonthsFromToday
                },
                sortBy: [SortDescriptor(\.start_time, order: .forward)]
            )

            var blocks = try modelContext.fetch(descriptor)

            // Filter to only show work blocks that:
            // 1. Have a session relationship where user is assigned/owner, OR
            // 2. Have cached data (from /user/schedule API - already filtered by user)
            let userId = currentUserId
            blocks = blocks.filter { block in
                // If block has cached data from API, it's already for this user
                if block.cached_session_id != nil {
                    return true
                }
                // Otherwise check session relationship
                guard let session = block.session else { return false }
                return session.assigned_to.contains(userId) || session.owned_by.contains(userId)
            }

            // Group blocks by time period
            var today: [SessionWorkBlock] = []
            var thisWeek: [SessionWorkBlock] = []
            var nextWeek: [SessionWorkBlock] = []
            var later: [SessionWorkBlock] = []

            for block in blocks {
                if block.start_time < startOfTomorrow {
                    today.append(block)
                } else if block.start_time < startOfNextWeek {
                    thisWeek.append(block)
                } else if block.start_time < startOfLater {
                    nextWeek.append(block)
                } else {
                    later.append(block)
                }
            }

            todayBlocks = today
            thisWeekBlocks = thisWeek
            nextWeekBlocks = nextWeek
            laterBlocks = later
        } catch {
            AppLogger.log(.error, "Error fetching upcoming work blocks: \(error)", category: .ui)
            todayBlocks = []
            thisWeekBlocks = []
            nextWeekBlocks = []
            laterBlocks = []
        }
    }

    /// Load all work blocks for calendar indicators (not filtered by week)
    private func loadAllWorkBlocks() {
        do {
            // Fetch all non-deleted work blocks from local storage
            let descriptor = FetchDescriptor<SessionWorkBlock>(
                predicate: #Predicate<SessionWorkBlock> { block in
                    block.is_deleted == false
                },
                sortBy: [SortDescriptor(\.start_time, order: .forward)]
            )

            var blocks = try modelContext.fetch(descriptor)

            // Filter to only show work blocks that:
            // 1. Have a session relationship where user is assigned/owner, OR
            // 2. Have cached data (from /user/schedule API - already filtered by user)
            let currentUserId = AppStateManager.shared.userId
            blocks = blocks.filter { block in
                // If block has cached data from API, it's already for this user
                if block.cached_session_id != nil {
                    return true
                }
                // Otherwise check session relationship
                guard let session = block.session else { return false }
                return session.assigned_to.contains(currentUserId) || session.owned_by.contains(currentUserId)
            }

            allWorkBlocks = blocks
        } catch {
            AppLogger.log(.error, "Error fetching all work blocks: \(error)", category: .ui)
            allWorkBlocks = []
        }
    }

    /// Fetch user schedule from API and update local storage
    private func fetchUserScheduleFromAPI() {
        guard networkState.mode == .online else {
            // Skip API fetch if offline
            return
        }

        isLoadingSchedule = true

        Task {
            do {
                // Fetch user schedule from API (include past for calendar display)
                let response = try await APIClient.shared.getUserSchedule(includePast: true)

                if response.success {
                    await MainActor.run {
                        // Process and store work blocks
                        for blockDTO in response.work_blocks {
                            processWorkBlockDTO(blockDTO)
                        }

                        // Clean up orphaned work blocks (those whose sessions are no longer returned)
                        let validSessionIds = Set((response.sessions ?? []).compactMap { UUID(uuidString: $0.id) })
                        let returnedBlockIds = Set(response.work_blocks.compactMap { UUID(uuidString: $0.id) })

                        // Fetch all local work blocks
                        let allBlocksDescriptor = FetchDescriptor<SessionWorkBlock>()
                        if let allLocalBlocks = try? modelContext.fetch(allBlocksDescriptor) {
                            for block in allLocalBlocks {
                                // Mark as deleted if:
                                // 1. Not in the returned blocks AND
                                // 2. Session is not in the valid sessions list
                                if !returnedBlockIds.contains(block.id),
                                   let sessionId = block.cached_session_id,
                                   !validSessionIds.contains(sessionId) {
                                    block.is_deleted = true
                                }
                            }
                        }

                        // Save context
                        try? modelContext.save()

                        // Reload local data
                        loadAllWorkBlocks()
                        loadUpcomingWorkBlocks()

                        isLoadingSchedule = false
                    }
                } else {
                    await MainActor.run {
                        isLoadingSchedule = false
                    }
                }
            } catch {
                AppLogger.log(.error, "Error fetching user schedule: \(error)", category: .ui)
                await MainActor.run {
                    isLoadingSchedule = false
                }
            }
        }
    }

    /// Process a work block DTO from the API and store/update in SwiftData
    private func processWorkBlockDTO(_ dto: SessionWorkBlockDTO) {
        guard let blockId = UUID(uuidString: dto.id) else { return }

        // Parse dates
        let iso8601Formatter = ISO8601DateFormatter()
        iso8601Formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]

        guard let startTime = iso8601Formatter.date(from: dto.start_time) ?? ISO8601DateFormatter().date(from: dto.start_time),
              let endTime = iso8601Formatter.date(from: dto.end_time) ?? ISO8601DateFormatter().date(from: dto.end_time) else {
            AppLogger.log(.notice, "Failed to parse dates for work block \(dto.id)", category: .ui)
            return
        }

        // Check if work block already exists
        let descriptor = FetchDescriptor<SessionWorkBlock>(predicate: #Predicate { $0.id == blockId })
        let existingBlock = try? modelContext.fetch(descriptor).first

        if let block = existingBlock {
            // Update existing block
            block.start_time = startTime
            block.end_time = endTime
            block.work_length = dto.work_length
            block.total_days = dto.total_days
            block.notes = dto.notes
            block.is_deleted = dto.is_deleted
            block.duration_hours = dto.duration_hours

            // Update cached fields from API
            if let sessionId = UUID(uuidString: dto.session_id) {
                block.cached_session_id = sessionId
            }
            block.cached_session_name = dto.session_name
            block.cached_session_description = dto.session_description
            if let sldIdStr = dto.sld_id, let sldId = UUID(uuidString: sldIdStr) {
                block.cached_sld_id = sldId
            }
            block.cached_sld_name = dto.sld_name
        } else {
            // Create new work block
            let sessionId = UUID(uuidString: dto.session_id)
            let sldId = dto.sld_id.flatMap { UUID(uuidString: $0) }

            let newBlock = SessionWorkBlock(
                id: blockId,
                session: nil, // Don't set session relationship - may not be synced locally
                start_time: startTime,
                end_time: endTime,
                work_length: dto.work_length,
                total_days: dto.total_days,
                notes: dto.notes,
                is_deleted: dto.is_deleted,
                duration_hours: dto.duration_hours,
                cached_session_id: sessionId,
                cached_session_name: dto.session_name,
                cached_session_description: dto.session_description,
                cached_sld_id: sldId,
                cached_sld_name: dto.sld_name
            )

            // Try to find and link the session if it exists locally
            if let sessionId = sessionId {
                let sessionDescriptor = FetchDescriptor<IRSession>(predicate: #Predicate { $0.id == sessionId })
                if let existingSession = try? modelContext.fetch(sessionDescriptor).first {
                    newBlock.session = existingSession
                }
            }

            modelContext.insert(newBlock)
        }
    }
}

// MARK: - Work Block Item View
struct WorkBlockItemView: View {
    let workBlock: SessionWorkBlock
    var onTap: (() -> Void)? = nil

    private var isOverdue: Bool {
        workBlock.end_time < Calendar.current.startOfDay(for: Date())
    }

    private var dayOfWeekFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE"
        return formatter
    }

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter
    }

    private var timeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter
    }

    /// Site name from the session's SLD or cached value
    private var siteName: String? {
        workBlock.siteName
    }

    var body: some View {
        HStack(spacing: 16) {
            // Date badge
            VStack(spacing: 4) {
                Text(dayOfWeekFormatter.string(from: workBlock.start_time).prefix(3).uppercased())
                    .font(.caption2)
                    .fontWeight(.semibold)
                    .foregroundColor(isOverdue ? .red : .secondary)
                Text(Calendar.current.component(.day, from: workBlock.start_time).description)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(isOverdue ? .red : .primary)
            }
            .frame(width: 50, height: 50)
            .background(isOverdue ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
            .cornerRadius(10)

            // Content
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Text(workBlock.sessionName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    if isOverdue {
                        Text(AppStrings.Common.overdue)
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.red)
                            .cornerRadius(4)
                    }
                }

                // Site name
                if let site = siteName {
                    HStack(spacing: 4) {
                        Image(systemName: "building.2")
                            .font(.caption2)
                            .foregroundColor(isOverdue ? .red : .blue)
                        Text(site)
                            .font(.caption)
                            .foregroundColor(isOverdue ? .red : .blue)
                            .lineLimit(1)
                    }
                }

                // Session description if available
                if let description = workBlock.session?.sessionDescription, !description.isEmpty {
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }

                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(timeFormatter.string(from: workBlock.start_time)) - \(timeFormatter.string(from: workBlock.end_time))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                if let notes = workBlock.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.caption)
                        .foregroundColor(Color.gray)
                        .lineLimit(2)
                }
            }

            Spacer()

            // Duration indicator
            if let hours = workBlock.duration_hours {
                Text(String(format: "%.1fh", hours))
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(isOverdue ? .red : .blue)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(isOverdue ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
                    .cornerRadius(6)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.red.opacity(0.3), lineWidth: isOverdue ? 1 : 0)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            onTap?()
        }
    }
}

// MARK: - Day Calendar View
struct DayCalendarView: View {
    @Binding var selectedDate: Date
    let workBlocks: [SessionWorkBlock]
    var onWorkBlockTap: ((SessionWorkBlock) -> Void)? = nil

    private let hourHeight: CGFloat = 60

    private var dayFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMMM d"
        return formatter
    }

    private var timeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter
    }

    /// Represents a group of overlapping work blocks
    private struct BlockGroup: Identifiable {
        let id = UUID()
        let blocks: [SessionWorkBlock]
        let topOffset: CGFloat
        let height: CGFloat
    }

    /// Groups overlapping work blocks together
    private func overlappingGroups() -> [BlockGroup] {
        guard !workBlocks.isEmpty else { return [] }

        let sorted = workBlocks.sorted { $0.start_time < $1.start_time }
        var groups: [BlockGroup] = []
        var currentBlocks: [SessionWorkBlock] = [sorted[0]]
        var groupEnd = sorted[0].end_time

        for i in 1..<sorted.count {
            let block = sorted[i]
            if block.start_time < groupEnd {
                currentBlocks.append(block)
                groupEnd = max(groupEnd, block.end_time)
            } else {
                groups.append(makeGroup(from: currentBlocks))
                currentBlocks = [block]
                groupEnd = block.end_time
            }
        }
        groups.append(makeGroup(from: currentBlocks))

        return groups
    }

    private func makeGroup(from blocks: [SessionWorkBlock]) -> BlockGroup {
        let earliest = blocks.min(by: { $0.start_time < $1.start_time })!
        let latest = blocks.max(by: { $0.end_time < $1.end_time })!

        let cal = Calendar.current
        let startH = cal.component(.hour, from: earliest.start_time)
        let startM = cal.component(.minute, from: earliest.start_time)
        let endH = cal.component(.hour, from: latest.end_time)
        let endM = cal.component(.minute, from: latest.end_time)

        let topOffset = CGFloat(startH - 6) * hourHeight + hourHeight / 2 + CGFloat(startM) / 60 * hourHeight
        let duration = CGFloat(endH - startH) * hourHeight + CGFloat(endM - startM) / 60 * hourHeight

        return BlockGroup(blocks: blocks, topOffset: topOffset, height: max(duration, 56))
    }

    var body: some View {
        VStack(spacing: 0) {
            // Date Navigation
            HStack {
                Button(action: { adjustDate(by: -1) }) {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundColor(.blue)
                }

                Spacer()

                Text(dayFormatter.string(from: selectedDate))
                    .font(.headline)

                Spacer()

                Button(action: { adjustDate(by: 1) }) {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal)
            .padding(.top, 16)
            .padding(.bottom, 12)

            // Day schedule with work blocks
            ScrollView {
                ZStack(alignment: .topLeading) {
                    // Hour grid lines
                    VStack(alignment: .leading, spacing: 0) {
                        ForEach(6..<22, id: \.self) { hour in
                            HStack {
                                Text(String(format: "%d:00", hour))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .frame(width: 45, alignment: .trailing)

                                Rectangle()
                                    .fill(Color.gray.opacity(0.2))
                                    .frame(height: 1)
                            }
                            .frame(height: hourHeight)
                        }
                    }
                    .padding(.horizontal)

                    // Work block groups overlay
                    ForEach(overlappingGroups()) { group in
                        if group.blocks.count == 1 {
                            // Single block - full width
                            let block = group.blocks[0]
                            blockCard(for: block)
                                .frame(height: max(blockDuration(block), 56), alignment: .top)
                                .padding(.leading, 60)
                                .padding(.trailing, 16)
                                .offset(y: group.topOffset)
                        } else {
                            // Multiple overlapping blocks - horizontal scroll
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(alignment: .top, spacing: 8) {
                                    ForEach(group.blocks, id: \.id) { block in
                                        let duration = max(blockDuration(block), 56)
                                        let topInGroup = blockTopOffset(block) - group.topOffset
                                        blockCard(for: block)
                                            .frame(width: 200, height: duration, alignment: .top)
                                            .padding(.top, topInGroup)
                                    }
                                }
                            }
                            .frame(height: group.height, alignment: .top)
                            .padding(.leading, 60)
                            .padding(.trailing, 16)
                            .offset(y: group.topOffset)
                        }
                    }
                }
            }
        }
    }

    private func isBlockOverdue(_ block: SessionWorkBlock) -> Bool {
        block.end_time < Calendar.current.startOfDay(for: Date())
    }

    @ViewBuilder
    private func blockCard(for block: SessionWorkBlock) -> some View {
        let overdue = isBlockOverdue(block)
        HStack(alignment: .top, spacing: 8) {
            Rectangle()
                .fill(overdue ? Color.red : Color.blue)
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text(block.sessionName)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                if let siteName = block.siteName {
                    HStack(spacing: 2) {
                        Image(systemName: "building.2")
                            .font(.system(size: 8))
                            .foregroundColor(overdue ? .red : .blue)
                        Text(siteName)
                            .font(.caption2)
                            .foregroundColor(overdue ? .red : .blue)
                            .lineLimit(1)
                    }
                }

                Text("\(timeFormatter.string(from: block.start_time)) - \(timeFormatter.string(from: block.end_time))")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(.top, 4)

            Spacer()

            if overdue {
                Text(AppStrings.Common.overdue)
                    .font(.system(size: 8, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 1)
                    .background(Color.red)
                    .cornerRadius(3)
                    .padding(.top, 4)
            }
        }
        .padding(.horizontal, 8)
        .background(overdue ? Color.red.opacity(0.1) : Color.blue.opacity(0.15))
        .cornerRadius(6)
        .contentShape(Rectangle())
        .onTapGesture {
            onWorkBlockTap?(block)
        }
    }

    private func blockTopOffset(_ block: SessionWorkBlock) -> CGFloat {
        let cal = Calendar.current
        let startH = cal.component(.hour, from: block.start_time)
        let startM = cal.component(.minute, from: block.start_time)
        return CGFloat(startH - 6) * hourHeight + hourHeight / 2 + CGFloat(startM) / 60 * hourHeight
    }

    private func blockDuration(_ block: SessionWorkBlock) -> CGFloat {
        let cal = Calendar.current
        let startH = cal.component(.hour, from: block.start_time)
        let startM = cal.component(.minute, from: block.start_time)
        let endH = cal.component(.hour, from: block.end_time)
        let endM = cal.component(.minute, from: block.end_time)
        return CGFloat(endH - startH) * hourHeight + CGFloat(endM - startM) / 60 * hourHeight
    }

    private func adjustDate(by days: Int) {
        if let newDate = Calendar.current.date(byAdding: .day, value: days, to: selectedDate) {
            selectedDate = newDate
        }
    }
}

// MARK: - Week Calendar View
struct WeekCalendarView: View {
    @Binding var selectedDate: Date
    let datesWithWorkBlocks: Set<Date>
    let workBlocksForDate: (Date) -> [SessionWorkBlock]
    var onWorkBlockTap: ((SessionWorkBlock) -> Void)? = nil

    private var weekDays: [Date] {
        let calendar = Calendar.current
        let startOfWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: selectedDate))!
        return (0..<7).compactMap { calendar.date(byAdding: .day, value: $0, to: startOfWeek) }
    }

    private var monthYearFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter
    }

    private var timeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter
    }

    /// Check if a date has work blocks
    private func hasWorkBlocks(for date: Date) -> Bool {
        let calendar = Calendar.current
        if let startOfDay = calendar.date(from: calendar.dateComponents([.year, .month, .day], from: date)) {
            return datesWithWorkBlocks.contains(startOfDay)
        }
        return false
    }

    var body: some View {
        VStack(spacing: 0) {
            // Week Navigation
            HStack {
                Button(action: { adjustWeek(by: -1) }) {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundColor(.blue)
                }

                Spacer()

                Text(monthYearFormatter.string(from: selectedDate))
                    .font(.headline)

                Spacer()

                Button(action: { adjustWeek(by: 1) }) {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal)
            .padding(.top, 16)
            .padding(.bottom, 12)

            // Week days header
            HStack(spacing: 0) {
                ForEach(weekDays, id: \.self) { date in
                    WeekDayCell(date: date, isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate), hasWorkBlocks: hasWorkBlocks(for: date))
                        .onTapGesture {
                            selectedDate = date
                        }
                }
            }
            .padding(.horizontal, 8)

            // Work blocks for selected day
            let selectedDayBlocks = workBlocksForDate(selectedDate)
            if selectedDayBlocks.isEmpty {
                Spacer()
                VStack {
                    Text(AppStrings.Home.noScheduledWork)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                Spacer()
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(selectedDayBlocks, id: \.id) { block in
                            let overdue = block.end_time < Calendar.current.startOfDay(for: Date())
                            HStack(alignment: .top, spacing: 12) {
                                Rectangle()
                                    .fill(overdue ? Color.red : Color.blue)
                                    .frame(width: 4)

                                VStack(alignment: .leading, spacing: 4) {
                                    // Site name
                                    if let siteName = block.siteName {
                                        Text(siteName)
                                            .font(.caption2)
                                            .foregroundColor(overdue ? .red : .blue)
                                            .fontWeight(.semibold)
                                    }

                                    Text(block.sessionName)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                        .foregroundColor(.primary)

                                    Text("\(timeFormatter.string(from: block.start_time)) - \(timeFormatter.string(from: block.end_time))")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                VStack(alignment: .trailing, spacing: 4) {
                                    if let hours = block.duration_hours {
                                        Text(String(format: "%.1fh", hours))
                                            .font(.caption)
                                            .foregroundColor(overdue ? .red : .blue)
                                    }

                                    if overdue {
                                        Text(AppStrings.Common.overdue)
                                            .font(.caption2)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 5)
                                            .padding(.vertical, 1)
                                            .background(Color.red)
                                            .cornerRadius(3)
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(overdue ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
                            .cornerRadius(8)
                            .onTapGesture {
                                onWorkBlockTap?(block)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                }
            }
        }
        .padding(.bottom, 16)
    }

    private func adjustWeek(by weeks: Int) {
        if let newDate = Calendar.current.date(byAdding: .weekOfYear, value: weeks, to: selectedDate) {
            selectedDate = newDate
        }
    }
}

// MARK: - Week Day Cell
struct WeekDayCell: View {
    let date: Date
    let isSelected: Bool
    var hasWorkBlocks: Bool = false

    private var dayOfWeek: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE"
        return formatter.string(from: date)
    }

    private var dayNumber: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d"
        return formatter.string(from: date)
    }

    private var isToday: Bool {
        Calendar.current.isDateInToday(date)
    }

    var body: some View {
        VStack(spacing: 4) {
            Text(dayOfWeek)
                .font(.caption)
                .foregroundColor(isSelected ? .white : .secondary)

            Text(dayNumber)
                .font(.headline)
                .fontWeight(isToday ? .bold : .regular)
                .foregroundColor(isSelected ? .white : (isToday ? .blue : .primary))

            // Work block indicator dot
            Circle()
                .fill(isSelected ? Color.white : Color.blue)
                .frame(width: 6, height: 6)
                .opacity(hasWorkBlocks ? 1 : 0)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(isSelected ? Color.blue : Color.clear)
        .cornerRadius(12)
    }
}

// MARK: - Month Calendar View
struct MonthCalendarView: View {
    @Binding var selectedDate: Date
    let datesWithWorkBlocks: Set<Date>
    let workBlocksForDate: (Date) -> [SessionWorkBlock]
    var onWorkBlockTap: ((SessionWorkBlock) -> Void)? = nil

    private var timeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter
    }

    private var monthYearFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter
    }

    private var daysInMonth: [Date?] {
        let calendar = Calendar.current
        let range = calendar.range(of: .day, in: .month, for: selectedDate)!
        let firstDayOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: selectedDate))!
        let firstWeekday = calendar.component(.weekday, from: firstDayOfMonth)

        var days: [Date?] = Array(repeating: nil, count: firstWeekday - 1)

        for day in range {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: firstDayOfMonth) {
                days.append(date)
            }
        }

        // Pad to fill the last row (next multiple of 7)
        while days.count % 7 != 0 {
            days.append(nil)
        }

        return days
    }

    private let weekdaySymbols = Calendar.current.veryShortWeekdaySymbols

    /// Check if a date has work blocks
    private func hasWorkBlocks(for date: Date) -> Bool {
        let calendar = Calendar.current
        if let startOfDay = calendar.date(from: calendar.dateComponents([.year, .month, .day], from: date)) {
            return datesWithWorkBlocks.contains(startOfDay)
        }
        return false
    }

    var body: some View {
        VStack(spacing: 0) {
            // Month Navigation
            HStack {
                Button(action: { adjustMonth(by: -1) }) {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundColor(.blue)
                }

                Spacer()

                Text(monthYearFormatter.string(from: selectedDate))
                    .font(.headline)

                Spacer()

                Button(action: { adjustMonth(by: 1) }) {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal)
            .padding(.top, 16)
            .padding(.bottom, 12)

            // Weekday headers
            HStack(spacing: 0) {
                ForEach(Array(weekdaySymbols.enumerated()), id: \.offset) { _, symbol in
                    Text(symbol)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 8)

            // Calendar grid
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 4) {
                ForEach(Array(daysInMonth.enumerated()), id: \.offset) { _, date in
                    if let date = date {
                        MonthDayCell(
                            date: date,
                            isSelected: Calendar.current.isDate(date, inSameDayAs: selectedDate),
                            isToday: Calendar.current.isDateInToday(date),
                            hasWorkBlocks: hasWorkBlocks(for: date)
                        )
                        .onTapGesture {
                            selectedDate = date
                        }
                    } else {
                        Color.clear
                            .frame(height: 42)
                    }
                }
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 8)

            // Work blocks for selected day
            let selectedDayBlocks = workBlocksForDate(selectedDate)
            if selectedDayBlocks.isEmpty {
                VStack {
                    Text(AppStrings.Home.noScheduledWork)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.bottom, 16)
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(selectedDayBlocks, id: \.id) { block in
                            let overdue = block.end_time < Calendar.current.startOfDay(for: Date())
                            HStack(alignment: .top, spacing: 12) {
                                Rectangle()
                                    .fill(overdue ? Color.red : Color.blue)
                                    .frame(width: 4)

                                VStack(alignment: .leading, spacing: 4) {
                                    if let siteName = block.siteName {
                                        Text(siteName)
                                            .font(.caption2)
                                            .foregroundColor(overdue ? .red : .blue)
                                            .fontWeight(.semibold)
                                    }

                                    Text(block.sessionName)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                        .foregroundColor(.primary)

                                    Text("\(timeFormatter.string(from: block.start_time)) - \(timeFormatter.string(from: block.end_time))")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                VStack(alignment: .trailing, spacing: 4) {
                                    if let hours = block.duration_hours {
                                        Text(String(format: "%.1fh", hours))
                                            .font(.caption)
                                            .foregroundColor(overdue ? .red : .blue)
                                    }

                                    if overdue {
                                        Text(AppStrings.Common.overdue)
                                            .font(.caption2)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 5)
                                            .padding(.vertical, 1)
                                            .background(Color.red)
                                            .cornerRadius(3)
                                    }
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(overdue ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
                            .cornerRadius(8)
                            .onTapGesture {
                                onWorkBlockTap?(block)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                }
                .frame(maxHeight: 160)
                .padding(.bottom, 8)
            }
        }
    }

    private func adjustMonth(by months: Int) {
        if let newDate = Calendar.current.date(byAdding: .month, value: months, to: selectedDate) {
            selectedDate = newDate
        }
    }
}

// MARK: - Month Day Cell
struct MonthDayCell: View {
    let date: Date
    let isSelected: Bool
    let isToday: Bool
    var hasWorkBlocks: Bool = false

    private var dayNumber: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d"
        return formatter.string(from: date)
    }

    var body: some View {
        VStack(spacing: 2) {
            Text(dayNumber)
                .font(.subheadline)
                .fontWeight(isToday ? .bold : .regular)
                .foregroundColor(isSelected ? .white : (isToday ? .blue : .primary))

            // Work block indicator dot
            Circle()
                .fill(isSelected ? Color.white : Color.blue)
                .frame(width: 5, height: 5)
                .opacity(hasWorkBlocks ? 1 : 0)
        }
        .frame(width: 36, height: 42)
        .background(isSelected ? Color.blue : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
