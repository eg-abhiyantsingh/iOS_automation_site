//
//  IssueStatusHistorySection.swift
//  Egalvanic PZ
//

import SwiftUI
import SwiftData

// MARK: - Status History Full-Screen View

struct IssueStatusHistoryView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState

    let issue: Issue
    @State private var isLoading = false

    private var sortedHistory: [IssueStatusHistory] {
        issue.statusHistory.sorted { ($0.changed_at ?? .distantPast) > ($1.changed_at ?? .distantPast) }
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && issue.statusHistory.isEmpty {
                    VStack {
                        Spacer()
                        ProgressView("Loading history...")
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else if sortedHistory.isEmpty {
                    VStack {
                        Spacer()
                        Image(systemName: "clock.arrow.circlepath")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary.opacity(0.5))
                            .padding(.bottom, 8)
                        Text(AppStrings.Issues.noStatusChanges)
                            .foregroundColor(.secondary)
                            .font(.subheadline)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(Array(sortedHistory.enumerated()), id: \.element.id) { index, entry in
                                StatusHistoryRow(entry: entry, isLast: index == sortedHistory.count - 1)
                            }
                        }
                        .padding()
                    }
                }
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle(AppStrings.Issues.statusHistoryTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.close) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        refreshHistory()
                    } label: {
                        ZStack {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                                .opacity(isLoading ? 1 : 0)
                            Image(systemName: "arrow.clockwise")
                                .opacity(isLoading ? 0 : 1)
                        }
                    }
                    .disabled(isLoading || networkState.mode != .online)
                }
            }
        }
        .task {
            if networkState.mode == .online {
                refreshHistory()
            }
        }
    }

    private func refreshHistory() {
        isLoading = true
        Task {
            defer { Task { @MainActor in isLoading = false } }
            do {
                let historyDTOs = try await APIClient.shared.fetchIssueStatusHistory(issueId: issue.id)
                await MainActor.run {
                    let existingLookup = Dictionary(uniqueKeysWithValues: issue.statusHistory.map { ($0.id, $0) })
                    var seenIds = Set<UUID>()
                    for dto in historyDTOs {
                        seenIds.insert(dto.id)
                        if let existing = existingLookup[dto.id] {
                            existing.old_status = dto.old_status
                            existing.new_status = dto.new_status
                            existing.changed_by = dto.changed_by
                            existing.changed_by_name = dto.changed_by_name
                            existing.changed_at = dto.changed_at
                            existing.change_reason = dto.change_reason
                        } else {
                            let newEntry = IssueStatusHistory(
                                id: dto.id,
                                issue_id: dto.issue_id,
                                old_status: dto.old_status,
                                new_status: dto.new_status,
                                changed_by: dto.changed_by,
                                changed_by_name: dto.changed_by_name,
                                changed_at: dto.changed_at,
                                change_reason: dto.change_reason,
                                issue: issue
                            )
                            modelContext.insert(newEntry)
                            issue.statusHistory.append(newEntry)
                        }
                    }
                    // Remove entries no longer on server
                    let toRemove = issue.statusHistory.filter { !seenIds.contains($0.id) }
                    for entry in toRemove {
                        issue.statusHistory.removeAll { $0.id == entry.id }
                        modelContext.delete(entry)
                    }
                }
            } catch {
                AppLogger.log(.notice, "Failed to fetch status history: \(error)", category: .issue)
            }
        }
    }
}

// MARK: - Status History Row

private struct StatusHistoryRow: View {
    let entry: IssueStatusHistory
    let isLast: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Timeline indicator: dot + vertical line
            VStack(spacing: 0) {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)
                if !isLast {
                    Rectangle()
                        .fill(Color.blue.opacity(0.3))
                        .frame(width: 2)
                }
            }
            .frame(width: 10)

            // Content card
            VStack(alignment: .leading, spacing: 6) {
                // Status transition
                if let oldStatus = entry.old_status {
                    HStack(spacing: 8) {
                        StatusBadge(status: oldStatus, style: .outline)
                        Image(systemName: "arrow.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        StatusBadge(status: entry.new_status, style: .filled)
                    }
                } else {
                    // Initial creation — old_status is nil
                    HStack(spacing: 8) {
                        Text(AppStrings.Issues.createdAs)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        StatusBadge(status: entry.new_status, style: .filled)
                    }
                }

                // Date and author
                HStack(spacing: 8) {
                    if let date = entry.changed_at {
                        Text(date, format: .dateTime.month(.abbreviated).day().year().hour().minute())
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let name = entry.changed_by_name {
                        Text("by \(name)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
        .padding(.bottom, isLast ? 0 : 4)
    }
}

// MARK: - Status Badge

struct StatusBadge: View {
    let status: String
    let style: BadgeStyle

    enum BadgeStyle {
        case filled
        case outline
    }

    private var color: Color {
        switch status.lowercased() {
        case "open", "new":
            return .blue
        case "in progress":
            return .orange
        case "pending":
            return .yellow
        case "resolved":
            return .green
        case "closed":
            return .gray
        default:
            return .gray
        }
    }

    var body: some View {
        Text(LanguageManager.localizedStatus(status))
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(style == .filled ? color : color.opacity(0.1))
            .foregroundColor(style == .filled ? .white : color)
            .cornerRadius(6)
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(color, lineWidth: style == .outline ? 1 : 0)
            )
    }
}
