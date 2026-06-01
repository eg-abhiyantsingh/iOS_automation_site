//
//  IssuesTab.swift
//  SwiftDataTutorial
//
//  Issues tab with manage functionality and exclusivity enforcement
//

import SwiftUI
import SwiftData

struct IssuesTab: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager

    let session: IRSession
    @Binding var selectedIssue: Issue?

    @State private var showIssueLinkingSheet = false
    @State private var showingIssueCreation = false
    @State private var isProcessing = false
    @State private var errorMessage: String?
    @State private var showError = false

    // QR filter state
    @State private var filterNode: NodeV2? = nil
    @State private var showQRScanner = false
    @State private var showDuplicateQRAlert = false
    @State private var duplicateQRNodes: [NodeV2] = []
    @State private var showAssetNotFound = false
    @State private var notFoundQRCode = ""

    // Sort state
    @State private var selectedSort: IssueSortType = .createdDate

    private let api = APIClient.shared

    private var allIssues: [Issue] {
        session.issues.filter { !$0.is_deleted }
    }

    private var issues: [Issue] {
        let base: [Issue]
        if let filterNode = filterNode {
            base = allIssues.filter { $0.node?.id == filterNode.id }
        } else {
            base = allIssues
        }
        return base.sorted(by: selectedSort.comparator)
    }

    private var openIssues: [Issue] {
        issues.filter { issue in
            guard let status = issue.status?.lowercased() else { return true }
            return status == "open" || status == "new" || status == "in progress" || status == "pending"
        }
    }

    private var closedIssues: [Issue] {
        issues.filter { issue in
            guard let status = issue.status?.lowercased() else { return false }
            return status == "resolved" || status == "closed"
        }
    }

    private var roomGroups: [(label: String, issues: [Issue])] {
        let grouped = Dictionary(grouping: issues) { issue in
            issue.node?.room?.fullPath
        }
        return grouped.keys
            .sorted { key1, key2 in
                if key1 == nil { return false }
                if key2 == nil { return true }
                return (key1 ?? "").localizedCaseInsensitiveCompare(key2 ?? "") == .orderedAscending
            }
            .map { key in
                (label: key ?? AppStrings.Common.noRoom, issues: grouped[key] ?? [])
            }
    }

    // Unfiltered counts for summary card
    private var allOpenIssueCount: Int {
        allIssues.filter { issue in
            guard let status = issue.status?.lowercased() else { return true }
            return status == "open" || status == "new" || status == "in progress" || status == "pending"
        }.count
    }

    private var allClosedIssueCount: Int {
        allIssues.filter { issue in
            guard let status = issue.status?.lowercased() else { return false }
            return status == "resolved" || status == "closed"
        }.count
    }
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if !allIssues.isEmpty {
                        // Summary Card (Status) - First (always shows unfiltered counts)
                        HStack {
                            Spacer()

                            VStack {
                                Text("\(allIssues.count)")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                Text(AppStrings.CommonExtra.total)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            VStack {
                                Text("\(allOpenIssueCount)")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.blue)
                                Text(AppStrings.CommonExtra.open)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            VStack {
                                Text("\(allClosedIssueCount)")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                                Text(AppStrings.CommonExtra.closed)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()
                        }
                        .padding()
                        .background(Color(UIColor.systemGray6))
                        .cornerRadius(12)
                        .padding(.horizontal)

                        // QR Filter Chip
                        if let filterNode = filterNode {
                            QRFilterChipBar(nodeName: filterNode.label) {
                                withAnimation {
                                    self.filterNode = nil
                                }
                            }
                        }

                        // Manage Issues Button - Second
                        HStack {
                            Spacer()

                            Button(action: { showIssueLinkingSheet = true }) {
                                Label(AppStrings.Sessions.manageIssues, systemImage: "link.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .padding(.horizontal)

                        // Issues Title and Display - Third
                        if issues.isEmpty && filterNode != nil {
                            ContentUnavailableView(
                                AppStrings.Sessions.noIssuesForAsset,
                                systemImage: "line.3.horizontal.decrease.circle",
                                description: Text("Try clearing the filter to see all issues")
                            )
                            .frame(minHeight: 200)
                        } else {
                            VStack(alignment: .leading, spacing: 12) {
                                // Header with sort menu
                                HStack {
                                    Text(AppStrings.Sessions.issues)
                                        .font(.headline)

                                    Spacer()

                                    Menu {
                                        ForEach(IssueSortType.allCases, id: \.self) { sortType in
                                            Button {
                                                selectedSort = sortType
                                            } label: {
                                                Label(sortType.label, systemImage: sortType.icon)
                                            }
                                        }
                                    } label: {
                                        Image(systemName: "arrow.up.arrow.down")
                                            .font(.subheadline)
                                            .foregroundColor(.primary)
                                    }
                                }
                                .padding(.horizontal)

                                if selectedSort == .room {
                                    // Room-grouped display
                                    ForEach(roomGroups, id: \.label) { group in
                                        VStack(alignment: .leading, spacing: 12) {
                                            RoomSectionHeader(label: group.label)
                                                .padding(.horizontal)

                                            ForEach(group.issues) { issue in
                                                IssueCard(issue: issue)
                                                    .padding(.horizontal)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        selectedIssue = issue
                                                    }
                                            }
                                        }
                                    }
                                } else {
                                    // Open Issues
                                    if !openIssues.isEmpty {
                                        VStack(alignment: .leading, spacing: 12) {
                                            Text(AppStrings.CommonExtra.open)
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                                .padding(.horizontal)

                                            ForEach(openIssues) { issue in
                                                IssueCard(issue: issue)
                                                    .padding(.horizontal)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        selectedIssue = issue
                                                    }
                                            }
                                        }
                                    }

                                    // Closed Issues
                                    if !closedIssues.isEmpty {
                                        VStack(alignment: .leading, spacing: 12) {
                                            Text(AppStrings.CommonExtra.closed)
                                                .font(.subheadline)
                                                .foregroundColor(.secondary)
                                                .padding(.horizontal)
                                                .padding(.top, openIssues.isEmpty ? 0 : 8)

                                            ForEach(closedIssues) { issue in
                                                IssueCard(issue: issue)
                                                    .padding(.horizontal)
                                                    .contentShape(Rectangle())
                                                    .onTapGesture {
                                                        selectedIssue = issue
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        VStack(spacing: 20) {
                            ContentUnavailableView(
                                AppStrings.Sessions.noIssues,
                                systemImage: "exclamationmark.triangle",
                                description: Text(AppStrings.Sessions.getStartedIssues)
                            )
                            .frame(minHeight: 300)

                            Button(action: { showIssueLinkingSheet = true }) {
                                Label(AppStrings.Sessions.manageIssues, systemImage: "link.circle.fill")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .padding(.top, 40)
                    }
                }
                .padding(.vertical)
                .padding(.bottom, 80) // Space for floating button
            }

            // Floating Action Buttons
            VStack {
                Spacer()
                HStack(spacing: 12) {
                    Spacer()

                    // QR Scanner FAB
                    Button(action: {
                        showQRScanner = true
                    }) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color.orange)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }

                    // Create Issue FAB
                    Button(action: {
                        showingIssueCreation = true
                    }) {
                        Image(systemName: "plus")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(appState.activeSession?.id == session.id ? Color.blue : Color.gray)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 2)
                    }
                    .disabled(appState.activeSession?.id != session.id)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
        .sheet(isPresented: $showIssueLinkingSheet) {
            IssueLinkingView(
                session: session,
                onUpdate: { linkedIssueIds in
                    Task {
                        await updateIssueLinks(linkedIssueIds: linkedIssueIds)
                    }
                }
            )
        }
        .fullScreenCover(isPresented: $showingIssueCreation) {
            UnifiedIssueCreationView()
                .environmentObject(networkState)
        }
        .overlay {
            if isProcessing {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                ProgressView(AppStrings.CommonExtra.updating)
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(errorMessage ?? AppStrings.CommonExtra.anErrorOccurred)
        }
        .sheet(isPresented: $showQRScanner) {
            QRCodeScannerView(scannedCode: .constant(""), onScanComplete: { scannedCode in
                handleQRScan(scannedCode)
            })
        }
        .alert(AppStrings.AssetsExtra.multipleAssetsFound, isPresented: $showDuplicateQRAlert) {
            ForEach(duplicateQRNodes.prefix(5), id: \.id) { node in
                Button("\(node.label) (\(node.type))") {
                    withAnimation {
                        filterNode = node
                    }
                }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                duplicateQRNodes = []
            }
        } message: {
            Text(AppStrings.AssetsExtra.foundAssetsWithQR(duplicateQRNodes.count))
        }
        .alert(AppStrings.Locations.assetNotFound, isPresented: $showAssetNotFound) {
            Button(AppStrings.Common.ok) { }
        } message: {
            Text(AppStrings.Locations.assetNotFoundMessage(notFoundQRCode))
        }
    }

    private func handleQRScan(_ scannedCode: String) {
        let sldId = session.sld.id
        let qrCode = scannedCode

        let descriptor = FetchDescriptor<NodeV2>(
            predicate: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                node.qr_code == qrCode &&
                !node.is_deleted
            }
        )

        do {
            let matchingNodes = try modelContext.fetch(descriptor)

            if matchingNodes.count == 1 {
                withAnimation {
                    filterNode = matchingNodes[0]
                }
            } else if matchingNodes.count > 1 {
                duplicateQRNodes = matchingNodes
                showDuplicateQRAlert = true
            } else {
                notFoundQRCode = scannedCode
                showAssetNotFound = true
            }
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            errorMessage = "Failed to search for asset: \(error.localizedDescription)"
            showError = true
        }
    }

    private func updateIssueLinks(linkedIssueIds: Set<UUID>) async {
        isProcessing = true
        defer { isProcessing = false }
        
        let currentIssueIds = Set(allIssues.map { $0.id })
        
        // Issues to link (not currently linked)
        let issuesToLink = linkedIssueIds.subtracting(currentIssueIds)
        
        // Issues to unlink (currently linked but not in new selection)
        let issuesToUnlink = currentIssueIds.subtracting(linkedIssueIds)
        
        var errors: [String] = []
        
        // Process links
        for issueId in issuesToLink {
            do {
                // Get the issue
                guard let issue = try? modelContext.fetch(
                    FetchDescriptor<Issue>(
                        predicate: #Predicate { $0.id == issueId }
                    )
                ).first else {
                    errors.append("Issue not found: \(issueId)")
                    continue
                }
                
                // Remove from any existing session (exclusivity)
                if let existingSession = issue.session {
                    existingSession.issues.removeAll { $0.id == issueId }
                }
                
                // Add to new session
                issue.session = session
                session.issues.append(issue)
                
                // Update on server if online
                if networkState.mode == .online {
                    do {
                        _ = try await api.updateIssue(issue)
                        AppLogger.log(.info, "Issue \(issueId) linked to session \(session.id)", category: .issue)
                    } catch {
                        // Queue for later sync
                        let op = SyncOp(target: .issue, operation: .update, issue: issue)
                        networkState.enqueue(op)
                        AppLogger.log(.notice, "Issue link queued for sync: \(error)", category: .issue)
                    }
                } else {
                    // Queue for offline sync
                    let op = SyncOp(target: .issue, operation: .update, issue: issue)
                    networkState.enqueue(op)
                }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                errors.append("Failed to link issue: \(error.localizedDescription)")
            }
        }
        
        // Process unlinks
        for issueId in issuesToUnlink {
            do {
                // Get the issue
                guard let issue = try? modelContext.fetch(
                    FetchDescriptor<Issue>(
                        predicate: #Predicate { $0.id == issueId }
                    )
                ).first else {
                    errors.append("Issue not found: \(issueId)")
                    continue
                }
                
                // Remove from session
                issue.session = nil
                session.issues.removeAll { $0.id == issueId }
                
                // Update on server if online
                if networkState.mode == .online {
                    do {
                        _ = try await api.updateIssue(issue)
                        AppLogger.log(.info, "Issue \(issueId) unlinked from session \(session.id)", category: .issue)
                    } catch {
                        // Queue for later sync
                        let op = SyncOp(target: .issue, operation: .update, issue: issue)
                        networkState.enqueue(op)
                        AppLogger.log(.notice, "Issue unlink queued for sync: \(error)", category: .issue)
                    }
                } else {
                    // Queue for offline sync
                    let op = SyncOp(target: .issue, operation: .update, issue: issue)
                    networkState.enqueue(op)
                }
            } catch {
                guard !AuthError.isAuthError(error) else { return }
                errors.append("Failed to unlink issue: \(error.localizedDescription)")
            }
        }
        
        // Save changes
        do {
            try modelContext.save()
        } catch {
            errors.append("Failed to save changes: \(error.localizedDescription)")
        }
        
        if !errors.isEmpty {
            await MainActor.run {
                errorMessage = errors.joined(separator: "\n")
                showError = true
            }
        }
    }
}

struct IssueLinkingView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    
    let session: IRSession
    let onUpdate: (Set<UUID>) -> Void
    
    @State private var selectedIssueIds: Set<UUID> = []
    @State private var searchText = ""
    
    @Query private var allIssues: [Issue]
    
    private var sldIssues: [Issue] {
        allIssues.filter {
            !$0.is_deleted &&
            $0.sld?.id == session.sld.id
        }
    }
    
    private var filteredIssues: [Issue] {
        let filtered = if searchText.isEmpty {
            sldIssues
        } else {
            sldIssues.filter {
                ($0.title?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                ($0.issueDescription?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                ($0.node?.label.localizedCaseInsensitiveContains(searchText) ?? false)
            }
        }
        
        // Sort by: 1) Linked issues first, 2) Then by creation date
        return filtered.sorted { issue1, issue2 in
            let isIssue1Linked = currentLinkedIssueIds.contains(issue1.id)
            let isIssue2Linked = currentLinkedIssueIds.contains(issue2.id)
            
            if isIssue1Linked != isIssue2Linked {
                return isIssue1Linked // Linked issues come first
            }
            
            // Sort by creation date (newer first)
            guard let date1 = issue1.created_date else { return false }
            guard let date2 = issue2.created_date else { return true }
            return date1 > date2
        }
    }
    
    private var currentLinkedIssueIds: Set<UUID> {
        Set(session.issues.filter { !$0.is_deleted }.map { $0.id })
    }
    
    var body: some View {
        NavigationView {
            List {
                Section {
                    ForEach(filteredIssues) { issue in
                        IssueSelectionRow(
                            issue: issue,
                            isSelected: selectedIssueIds.contains(issue.id),
                            currentSession: session,
                            onToggle: {
                                if selectedIssueIds.contains(issue.id) {
                                    selectedIssueIds.remove(issue.id)
                                } else {
                                    selectedIssueIds.insert(issue.id)
                                }
                            }
                        )
                    }
                } header: {
                    Text(AppStrings.Sessions.selectIssuesToLink)
                } footer: {
                    Text("⚠️ \(AppStrings.Sessions.issuesExclusivityWarning)")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
            .searchable(text: $searchText, prompt: AppStrings.Sessions.searchIssues)
            .navigationTitle(AppStrings.Sessions.linkIssues)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.CommonExtra.update) {
                        onUpdate(selectedIssueIds)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedIssueIds == currentLinkedIssueIds)
                }
            }
        }
        .onAppear {
            // Initialize with currently linked issues
            selectedIssueIds = currentLinkedIssueIds
        }
    }
}

struct IssueSelectionRow: View {
    let issue: Issue
    let isSelected: Bool
    let currentSession: IRSession
    let onToggle: () -> Void
    
    private var truncatedNodeLabel: String? {
        guard let label = issue.node?.label else { return nil }
        if label.count > 10 {
            return String(label.prefix(10)) + "..."
        }
        return label
    }
    
    private func statusColor(for status: String) -> Color {
        switch status.lowercased() {
        case "open", "new":
            return .blue
        case "in progress", "investigating":
            return .blue
        case "resolved", "closed":
            return .green
        default:
            return .gray
        }
    }
    
    var body: some View {
        Button(action: onToggle) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(issue.title ?? AppStrings.CommonExtra.untitledIssue)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.primary)
                        
                        if let status = issue.status {
                            Text(LanguageManager.localizedStatus(status))
                                .font(.caption2)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(statusColor(for: status))
                                .cornerRadius(4)
                        }
                    }
                    
                    HStack(spacing: 12) {
                        if let nodeLabel = truncatedNodeLabel {
                            Label(nodeLabel, systemImage: "location.fill")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        // Show if linked to another session (exclusivity warning)
                        if let linkedSession = issue.session, linkedSession.id != currentSession.id {
                            Label(AppStrings.Sessions.linkedTo(linkedSession.name), systemImage: "exclamationmark.triangle.fill")
                                .font(.caption2)
                                .foregroundColor(.orange)
                                .lineLimit(1)
                        }
                        
                        Spacer()
                        
                        if let createdDate = issue.created_date {
                            Text(createdDate.formatted(date: .abbreviated, time: .omitted))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                Spacer()
                
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .blue : .gray)
                    .font(.title3)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct IssueCard: View {
    let issue: Issue
    
    private func statusColor(for status: String) -> Color {
        switch status.lowercased() {
        case "open", "new":
            return .blue
        case "in progress", "investigating":
            return .blue
        case "resolved", "closed":
            return .green
        default:
            return .gray
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(issue.title ?? AppStrings.CommonExtra.untitledIssue)
                    .font(.system(size: 15, weight: .semibold))
                    .lineLimit(2)
                
                Spacer()
                
                if let status = issue.status {
                    Text(LanguageManager.localizedStatus(status))
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(statusColor(for: status))
                        .cornerRadius(6)
                }
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }
            
            if let description = issue.issueDescription, !description.isEmpty {
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
            
            HStack(spacing: 16) {
                if let issueClass = issue.issue_class {
                    Label(issueClass.name, systemImage: "tag.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if let node = issue.node {
                    Label(node.label, systemImage: "location.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if issue.ir_photos.count > 0 {
                    Label(AppStrings.Sessions.irPhotoLabel(issue.ir_photos.count), systemImage: "camera.fill")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
        }
        .padding()
        .background(Color(UIColor.systemGray6))
        .cornerRadius(10)
        .contentShape(Rectangle())
    }
}

// MARK: - Issue Sort Type for Session Issues Tab
private enum IssueSortType: CaseIterable {
    case createdDate, modifiedDate, title, status, room

    var label: String {
        switch self {
        case .createdDate: return AppStrings.Common.createdDate
        case .modifiedDate: return AppStrings.Common.modifiedDate
        case .title: return AppStrings.Common.title
        case .status: return AppStrings.Common.status
        case .room: return AppStrings.Common.room
        }
    }

    var icon: String {
        switch self {
        case .createdDate: return "clock"
        case .modifiedDate: return "clock.arrow.circlepath"
        case .title: return "textformat"
        case .status: return "flag"
        case .room: return "door.left.hand.open"
        }
    }

    var comparator: (Issue, Issue) -> Bool {
        switch self {
        case .createdDate:
            return { issue1, issue2 in
                let d1 = issue1.created_date ?? Date.distantPast
                let d2 = issue2.created_date ?? Date.distantPast
                return d1 > d2
            }
        case .modifiedDate:
            return { issue1, issue2 in
                let d1 = issue1.modified_date ?? Date.distantPast
                let d2 = issue2.modified_date ?? Date.distantPast
                return d1 > d2
            }
        case .title:
            return { issue1, issue2 in
                let t1 = issue1.title ?? ""
                let t2 = issue2.title ?? ""
                return t1.localizedCaseInsensitiveCompare(t2) == .orderedAscending
            }
        case .status:
            return { issue1, issue2 in
                let priority = ["open": 0, "new": 0, "in progress": 1, "pending": 1, "resolved": 2, "closed": 3]
                let p1 = priority[issue1.status?.lowercased() ?? ""] ?? 99
                let p2 = priority[issue2.status?.lowercased() ?? ""] ?? 99
                return p1 < p2
            }
        case .room:
            return { issue1, issue2 in
                let path1 = issue1.node?.room?.fullPath
                let path2 = issue2.node?.room?.fullPath
                if path1 == nil && path2 == nil { return false }
                if path1 == nil { return false }
                if path2 == nil { return true }
                return path1!.localizedCaseInsensitiveCompare(path2!) == .orderedAscending
            }
        }
    }
}

// MARK: - Room Section Header for Session Issues Tab
private struct RoomSectionHeader: View {
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 8) {
                Image(systemName: "door.left.hand.open")
                    .font(.subheadline)
                    .foregroundColor(.accentColor)
                Text(label)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                Spacer()
            }
            Divider()
        }
    }
}