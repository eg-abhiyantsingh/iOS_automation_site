//
//  IRSessionCreationView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/2/25.
//

import SwiftUI
import SwiftData

// Team assignment entry
struct TeamAssignment: Identifiable {
    let id = UUID()
    let userId: UUID
    let userName: String
    let role: String // "field_technician" or "back_office"
    let isCurrentUser: Bool
}

// IR Session Creation View with Sync Support
struct IRSessionCreationView: View {
    let sld: SLDV2
    let onSave: (IRSession) -> Void

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    @State private var sessionName = ""
    @State private var photoType = "FLIR-SEP"
    @State private var isCreating = false
    @State private var showError = false
    @State private var errorMessage = ""

    // Team assignment state
    @State private var teamAssignments: [TeamAssignment] = []
    @State private var companyUsers: [CompanyUser] = []
    @State private var isLoadingUsers = false
    @State private var selectedUserId: UUID?
    @State private var selectedRole = "back_office"
    @State private var userSearchText = ""

    // Equipment selection state
    @Query private var allEquipment: [TestEquipment]
    @State private var selectedEquipmentIds: Set<UUID> = []
    @State private var showEquipmentSheet = false
    @State private var equipmentSearchText = ""

    private var currentUserId: UUID? {
        guard let sub = AuthService.shared.currentUser?.sub else { return nil }
        return UUID(uuidString: sub)
    }

    private var currentUserName: String {
        let user = AuthService.shared.currentUser
        if let name = user?.given_name, !name.isEmpty {
            if let last = user?.family_name, !last.isEmpty {
                return "\(name) \(last)"
            }
            return name
        }
        return user?.email ?? "You"
    }

    private var availableUsers: [CompanyUser] {
        let assignedIds = Set(teamAssignments.map { $0.userId })
        let filtered = companyUsers.filter { !assignedIds.contains($0.id) }
        if userSearchText.isEmpty {
            return filtered
        }
        return filtered.filter {
            $0.displayName.localizedCaseInsensitiveContains(userSearchText) ||
            ($0.email?.localizedCaseInsensitiveContains(userSearchText) ?? false)
        }
    }

    var body: some View {
        NavigationView {
            Form {
                Section(AppStrings.Sessions.workOrderConfiguration) {
                    TextField(AppStrings.Sessions.workOrderNameField, text: $sessionName)
                        .textFieldStyle(RoundedBorderTextFieldStyle())

                    Picker(AppStrings.Sessions.photoType, selection: $photoType) {
                        Text("FLIR-SEP").tag("FLIR-SEP")
                        Text("FLIR-IND").tag("FLIR-IND")
                        Text("FLUKE").tag("FLUKE")
                        Text("FOTRIC").tag("FOTRIC")
                    }

                    // Equipment row (hidden when no equipment available)
                    if !allEquipment.isEmpty {
                        Button {
                            showEquipmentSheet = true
                        } label: {
                            HStack {
                                Text(AppStrings.Common.equipment)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedEquipmentIds.isEmpty {
                                    Text("None")
                                        .foregroundColor(.secondary)
                                } else {
                                    Text("\(selectedEquipmentIds.count) selected")
                                        .foregroundColor(.secondary)
                                }
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .foregroundColor(Color(UIColor.tertiaryLabel))
                            }
                        }

                        // Selected equipment chips
                        if !selectedEquipmentIds.isEmpty {
                            let selectedEquipment = allEquipment.filter { selectedEquipmentIds.contains($0.id) }
                            EquipmentChipsView(equipment: selectedEquipment) { id in
                                selectedEquipmentIds.remove(id)
                            }
                        }
                    }
                }

                // Team Assignment Section
                Section(AppStrings.Sessions.team) {
                    // Current user as locked assignee
                    HStack {
                        Image(systemName: "person.fill")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(currentUserName)
                                .font(.body)
                            Text(AppStrings.Sessions.assignee)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Image(systemName: "lock.fill")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    // Existing team assignments (removable)
                    ForEach(teamAssignments.filter { !$0.isCurrentUser }) { assignment in
                        HStack {
                            Image(systemName: assignment.role == "field_technician" ? "person.fill" : "eye.fill")
                                .foregroundColor(assignment.role == "field_technician" ? .blue : .green)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(assignment.userName)
                                    .font(.body)
                                Text(assignment.role == "field_technician" ? "Field technician" : "Back office")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Button {
                                teamAssignments.removeAll { $0.id == assignment.id }
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.red.opacity(0.7))
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // Add team member controls
                    if !companyUsers.isEmpty {
                        VStack(spacing: 8) {
                            TextField(AppStrings.Sessions.searchUsers, text: $userSearchText)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .font(.subheadline)

                            if !userSearchText.isEmpty && !availableUsers.isEmpty {
                                ScrollView {
                                    VStack(spacing: 0) {
                                        ForEach(availableUsers.prefix(5)) { user in
                                            Button {
                                                selectedUserId = user.id
                                                userSearchText = user.displayName
                                            } label: {
                                                HStack {
                                                    VStack(alignment: .leading, spacing: 2) {
                                                        Text(user.displayName)
                                                            .font(.subheadline)
                                                            .foregroundColor(.primary)
                                                        if let email = user.email {
                                                            Text(email)
                                                                .font(.caption2)
                                                                .foregroundColor(.secondary)
                                                        }
                                                    }
                                                    Spacer()
                                                    if selectedUserId == user.id {
                                                        Image(systemName: "checkmark")
                                                            .foregroundColor(.blue)
                                                    }
                                                }
                                                .padding(.vertical, 6)
                                                .padding(.horizontal, 8)
                                            }
                                            .buttonStyle(.plain)
                                            Divider()
                                        }
                                    }
                                }
                                .scrollDismissesKeyboard(.interactively)
                                .frame(maxHeight: 150)
                                .background(Color(UIColor.secondarySystemBackground))
                                .cornerRadius(8)
                            }

                            HStack {
                                Picker(AppStrings.Sessions.role, selection: $selectedRole) {
                                    Text(AppStrings.Sessions.reporter).tag("back_office")
                                    Text(AppStrings.Sessions.assignee).tag("field_technician")
                                }
                                .pickerStyle(.segmented)

                                Button {
                                    addTeamMember()
                                } label: {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title3)
                                }
                                .disabled(selectedUserId == nil)
                            }
                        }
                    } else if isLoadingUsers {
                        HStack {
                            ProgressView()
                                .scaleEffect(0.8)
                            Text(AppStrings.Sessions.loadingUsers)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    } else if networkState.mode == .offline {
                        Label(AppStrings.Sessions.teamMembersOnline, systemImage: "wifi.slash")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }

                Section {
                    HStack {
                        Image(systemName: networkState.mode == .online ? "wifi" : "wifi.slash")
                            .foregroundColor(networkState.mode == .online ? .green : .orange)
                        Text(networkState.mode == .online ? AppStrings.CommonExtra.online : AppStrings.CommonExtra.offline)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Text(AppStrings.Sessions.workOrderActiveDescription)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    if networkState.mode == .offline {
                        Label(AppStrings.Sessions.workOrderWillBeSynced, systemImage: "arrow.triangle.2.circlepath")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }
            }
            .navigationTitle(AppStrings.Sessions.newWorkOrder)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) { dismiss() }
                    .disabled(isCreating)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.create) {
                        Task {
                            await createSession()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(isCreating || sessionName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .disabled(isCreating)
            .overlay {
                if isCreating {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                    ProgressView(AppStrings.Sessions.creatingWorkOrder)
                        .padding()
                        .background(Color(UIColor.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 5)
                }
            }
            .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(errorMessage)
            }
            .sheet(isPresented: $showEquipmentSheet) {
                EquipmentSelectionSheet(
                    allEquipment: allEquipment,
                    selectedIds: $selectedEquipmentIds
                )
            }
        }
        .onAppear {
            // Generate a default name with date/time
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d, h:mm a"
            sessionName = "Work Order - \(formatter.string(from: Date()))"

            // Pre-populate current user as assignee
            if let userId = currentUserId {
                teamAssignments = [
                    TeamAssignment(userId: userId, userName: currentUserName, role: "field_technician", isCurrentUser: true)
                ]
            }

            // Fetch company users if online
            if networkState.mode == .online {
                Task {
                    await loadCompanyUsers()
                }
            }
        }
    }

    private func addTeamMember() {
        guard let userId = selectedUserId,
              let user = companyUsers.first(where: { $0.id == userId }) else { return }

        let assignment = TeamAssignment(
            userId: user.id,
            userName: user.displayName,
            role: selectedRole,
            isCurrentUser: false
        )
        teamAssignments.append(assignment)

        // Reset selection
        selectedUserId = nil
        userSearchText = ""
    }

    private func loadCompanyUsers() async {
        guard let companyId = AuthService.shared.currentUser?.company_id else { return }
        isLoadingUsers = true
        do {
            companyUsers = try await APIClient.shared.fetchCompanyUsers(companyId: companyId)
        } catch {
            AppLogger.log(.error, "Failed to load company users: \(error)", category: .ui)
        }
        isLoadingUsers = false
    }

    private func createSession() async {
        isCreating = true

        AppLogger.log(.info, "Creating new IR session, SLD ID: \(sld.id), network mode: \(networkState.mode)", category: .ui)

        // Build owned_by and assigned_to arrays from team assignments
        let assigneeIds = teamAssignments.filter { $0.role == "field_technician" }.map { $0.userId }
        let reporterIds = teamAssignments.filter { $0.role == "back_office" }.map { $0.userId }

        let newSession = IRSession(
            id: UUID(),
            name: sessionName.trimmingCharacters(in: .whitespacesAndNewlines),
            photo_type: photoType,
            active_visual_prefix: "IR",
            active_ir_prefix: "IR",
            date_created: Date(),
            date_closed: nil,
            sld: sld,
            active: true,
            owned_by: reporterIds,
            assigned_to: assigneeIds,
            equipmentIds: Array(selectedEquipmentIds),
            user_tasks: [],
            issues: []
        )

        AppLogger.log(.debug, "Inserting session into context", category: .ui)
        modelContext.insert(newSession)

        do {
            AppLogger.log(.debug, "Saving context", category: .ui)
            try modelContext.save()
            AppLogger.log(.info, "Session saved locally: \(newSession.id)", category: .ui)

            // Handle sync based on network mode
            if networkState.mode == .online {
                AppLogger.log(.debug, "Online mode - attempting immediate sync", category: .ui)
                do {
                    _ = try await APIClient.shared.createIRSession(irSession: newSession)
                    AppLogger.log(.info, "Successfully synced session to server", category: .ui)

                    // Brief delay to ensure session transaction is committed before creating FK-dependent mappings
                    try? await Task.sleep(nanoseconds: 500_000_000)

                    // Create user-session mappings
                    await createTeamMappings(sessionId: newSession.id, online: true)

                    // Mark as synced
                    await MainActor.run {
                        newSession.lastSyncedAt = Date()
                        newSession.needsSync = false
                        try? modelContext.save()
                    }
                } catch {
                    AppLogger.log(.notice, "Failed to sync immediately, queuing for later: \(error)", category: .ui)

                    // Queue session for later sync
                    let syncOp = SyncOp(
                        target: .irSession,
                        operation: .create,
                        irSession: newSession
                    )
                    networkState.enqueue(syncOp)

                    // Queue mappings for later sync
                    await createTeamMappings(sessionId: newSession.id, online: false)
                }
            } else {
                AppLogger.log(.info, "Offline mode - queuing for sync", category: .ui)

                // Mark as needing sync
                newSession.needsSync = true
                try modelContext.save()

                // Queue session for sync when online
                let syncOp = SyncOp(
                    target: .irSession,
                    operation: .create,
                    irSession: newSession
                )
                networkState.enqueue(syncOp)

                // Queue mappings for sync when online
                await createTeamMappings(sessionId: newSession.id, online: false)

                AppLogger.log(.info, "Session queued for sync (queue size: \(networkState.syncQueueCount))", category: .ui)
            }

            await MainActor.run {
                isCreating = false
                onSave(newSession)
                dismiss()
            }

        } catch {
            guard !AuthError.isAuthError(error) else { return }
            AppLogger.log(.error, "Failed to create IR session: \(error)", category: .ui)
            await MainActor.run {
                isCreating = false
                errorMessage = "Failed to create work order: \(error.localizedDescription)"
                showError = true
            }
        }
    }

    private func createTeamMappings(sessionId: UUID, online: Bool) async {
        for assignment in teamAssignments {
            let mappingData = MappingData.userSession(
                userId: assignment.userId,
                sessionId: sessionId,
                mappingType: assignment.role,
                isDeleted: false
            )

            if online {
                do {
                    _ = try await APIClient.shared.createUserSessionMapping(
                        id: UUID(),
                        userId: assignment.userId,
                        sessionId: sessionId,
                        mappingType: assignment.role
                    )
                    AppLogger.log(.info, "Created user-session mapping for \(assignment.userName) as \(assignment.role)", category: .ui)
                } catch {
                    AppLogger.log(.notice, "Failed to create mapping for \(assignment.userName), queuing: \(error)", category: .ui)
                    let syncOp = SyncOp(target: .mappingUserSession, operation: .create, mappingData: mappingData)
                    networkState.enqueue(syncOp)
                }
            } else {
                let syncOp = SyncOp(target: .mappingUserSession, operation: .create, mappingData: mappingData)
                networkState.enqueue(syncOp)
            }
        }
    }
}

// IR Session Update View (for closing sessions)
struct IRSessionUpdateView: View {
    let session: IRSession
    let onUpdate: () -> Void
    
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    
    @State private var isUpdating = false
    @State private var showError = false
    @State private var errorMessage = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Spacer()
                
                Image(systemName: "camera.metering.unknown")
                    .font(.system(size: 60))
                    .foregroundColor(.orange)
                
                Text(AppStrings.Sessions.closeWorkOrder)
                    .font(.title2)
                    .fontWeight(.semibold)

                Text(AppStrings.Sessions.workOrderName(name: session.name))
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Text(AppStrings.Sessions.closeWorkOrderDescription)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                
                if networkState.mode == .offline {
                    Label(AppStrings.Sessions.changesWillSyncWhenOnline, systemImage: "wifi.slash")
                        .font(.caption)
                        .foregroundColor(.orange)
                        .padding(.top)
                }
                
                Spacer()
                
                VStack(spacing: 12) {
                    Button {
                        Task {
                            await closeSession()
                        }
                    } label: {
                        Text(AppStrings.Sessions.closeWorkOrderButton)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.red)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    .disabled(isUpdating)
                    
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                    .foregroundColor(.primary)
                    .disabled(isUpdating)
                }
                .padding(.horizontal)
                .padding(.bottom)
            }
            .navigationBarHidden(true)
            .overlay {
                if isUpdating {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                    ProgressView(AppStrings.Sessions.closingWorkOrder)
                        .padding()
                        .background(Color(UIColor.systemBackground))
                        .cornerRadius(10)
                        .shadow(radius: 5)
                }
            }
            .alert(AppStrings.CommonExtra.error, isPresented: $showError) {
                Button(AppStrings.Common.ok) { }
            } message: {
                Text(errorMessage)
            }
        }
    }

    private func closeSession() async {
        isUpdating = true
        
        AppLogger.log(.info, "Closing IR session: \(session.id), network mode: \(networkState.mode)", category: .ui)

        // Update local state
        session.active = false
        session.date_closed = Date()

        do {
            try modelContext.save()
            AppLogger.log(.info, "Session closed locally", category: .ui)

            // Handle sync based on network mode
            if networkState.mode == .online {
                AppLogger.log(.debug, "Online mode - attempting immediate sync", category: .ui)
                do {
                    _ = try await APIClient.shared.updateIRSession(session)
                    AppLogger.log(.info, "Successfully synced session update to server", category: .ui)
                    
                    // Mark as synced
                    await MainActor.run {
                        session.lastSyncedAt = Date()
                        session.needsSync = false
                        try? modelContext.save()
                    }
                } catch {
                    AppLogger.log(.notice, "Failed to sync immediately, queuing for later: \(error)", category: .ui)
                    
                    // Queue for later sync
                    let syncOp = SyncOp(
                        target: .irSession,
                        operation: .update,
                        irSession: session
                    )
                    networkState.enqueue(syncOp)
                }
            } else {
                AppLogger.log(.info, "Offline mode - queuing for sync", category: .ui)
                
                // Mark as needing sync
                session.needsSync = true
                try modelContext.save()
                
                // Queue for sync when online
                let syncOp = SyncOp(
                    target: .irSession,
                    operation: .update,
                    irSession: session
                )
                networkState.enqueue(syncOp)
                
                AppLogger.log(.info, "Session update queued for sync (queue size: \(networkState.syncQueueCount))", category: .ui)
            }
            
            await MainActor.run {
                isUpdating = false
                onUpdate()
                dismiss()
            }
            
        } catch {
            guard !AuthError.isAuthError(error) else { return }
            AppLogger.log(.error, "Failed to close IR session: \(error)", category: .ui)
            await MainActor.run {
                isUpdating = false
                errorMessage = "Failed to close work order: \(error.localizedDescription)"
                showError = true
            }
        }
    }
}

// MARK: - Equipment Chips View
struct EquipmentChipsView: View {
    let equipment: [TestEquipment]
    let onRemove: (UUID) -> Void

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(equipment, id: \.id) { item in
                HStack(spacing: 4) {
                    Text(item.name)
                        .font(.caption)
                        .lineLimit(1)
                    Button {
                        withAnimation {
                            onRemove(item.id)
                        }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(UIColor.tertiarySystemFill))
                .cornerRadius(16)
            }
        }
    }
}

// MARK: - Flow Layout for Chips
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (index, subview) in subviews.enumerated() {
            let point = result.positions[index]
            subview.place(at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y), proposal: .unspecified)
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (positions: [CGPoint], size: CGSize) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
        }

        return (positions, CGSize(width: maxX, height: y + rowHeight))
    }
}

// MARK: - Equipment Selection Sheet
struct EquipmentSelectionSheet: View {
    let allEquipment: [TestEquipment]
    @Binding var selectedIds: Set<UUID>
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""

    private var filteredEquipment: [TestEquipment] {
        if searchText.isEmpty {
            return allEquipment
        }
        return allEquipment.filter {
            $0.name.localizedCaseInsensitiveContains(searchText) ||
            ($0.serialNumber?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    var body: some View {
        NavigationView {
            List {
                if allEquipment.isEmpty {
                    Text(AppStrings.Sessions.noEquipmentAvailable)
                        .foregroundColor(.secondary)
                        .font(.subheadline)
                } else {
                    ForEach(filteredEquipment, id: \.id) { equipment in
                        Button {
                            if selectedIds.contains(equipment.id) {
                                selectedIds.remove(equipment.id)
                            } else {
                                selectedIds.insert(equipment.id)
                            }
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(equipment.name)
                                        .font(.body)
                                        .foregroundColor(.primary)
                                    if let serial = equipment.serialNumber, !serial.isEmpty {
                                        Text("S/N: \(serial)")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }
                                Spacer()
                                if selectedIds.contains(equipment.id) {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                } else {
                                    Image(systemName: "circle")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                }
            }
            .searchable(text: $searchText, prompt: Text(AppStrings.Sessions.searchEquipment))
            .navigationTitle(AppStrings.AssetsExtra.selectEquipment)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}
