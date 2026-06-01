//
//  NodeTaskPickerView.swift
//  Egalvanic PZ
//
//  Picker sheet to link/unlink tasks to a node
//

import SwiftUI
import SwiftData

struct NodeTaskPickerView: View {
    let session: IRSession
    let node: NodeV2
    let onSave: (Set<UUID>) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var selectedTaskIds: Set<UUID> = []
    @State private var searchText: String = ""

    private var tasks: [UserTask] {
        session.user_tasks.filter { !$0.is_deleted }
    }

    private var filteredTasks: [UserTask] {
        let base = tasks
        let filtered: [UserTask]
        if searchText.isEmpty {
            filtered = base
        } else {
            let search = searchText.lowercased()
            filtered = base.filter { $0.title.lowercased().contains(search) }
        }

        // Sort: unlinked tasks first, then by linked node count descending, alphabetical tiebreaker
        return filtered.sorted { a, b in
            let aLinked = a.linkedNodes.contains(where: { $0.id == node.id })
            let bLinked = b.linkedNodes.contains(where: { $0.id == node.id })

            // Pre-checked (already linked) tasks go last
            if aLinked != bLinked {
                return !aLinked
            }

            let aCount = a.linkedNodes.filter { !$0.is_deleted }.count
            let bCount = b.linkedNodes.filter { !$0.is_deleted }.count

            if aCount != bCount {
                return aCount < bCount
            }

            return a.title < b.title
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.Sessions.searchTasksPlaceholder, text: $searchText)
                        .textFieldStyle(PlainTextFieldStyle())
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(8)
                .background(Color(UIColor.systemGray6))
                .cornerRadius(8)
                .padding(.horizontal)
                .padding(.vertical, 8)

                if tasks.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Sessions.noTasks,
                        systemImage: "checklist",
                        description: Text(AppStrings.Sessions.sessionHasNoTasks)
                    )
                    .frame(maxHeight: .infinity)
                } else if filteredTasks.isEmpty {
                    ContentUnavailableView(
                        AppStrings.Sessions.noMatchingTasks,
                        systemImage: "magnifyingglass",
                        description: Text(AppStrings.Sessions.noTasksMatch(searchText))
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    List {
                        ForEach(filteredTasks) { task in
                            Button {
                                if selectedTaskIds.contains(task.id) {
                                    selectedTaskIds.remove(task.id)
                                } else {
                                    selectedTaskIds.insert(task.id)
                                }
                            } label: {
                                HStack(spacing: 12) {
                                    Image(systemName: selectedTaskIds.contains(task.id) ? "checkmark.circle.fill" : "circle")
                                        .foregroundColor(selectedTaskIds.contains(task.id) ? .blue : .gray)
                                        .font(.system(size: 20))

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(task.title)
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(.primary)
                                        if !task.task_description.isEmpty {
                                            Text(task.task_description)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                                .lineLimit(1)
                                        }
                                    }

                                    Spacer()

                                    let linkedCount = task.linkedNodes.filter { !$0.is_deleted }.count
                                    if linkedCount > 0 {
                                        Text("\(linkedCount)")
                                            .font(.caption2)
                                            .fontWeight(.medium)
                                            .foregroundColor(.secondary)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color(UIColor.systemGray5))
                                            .cornerRadius(4)
                                    }
                                }
                            }
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle(AppStrings.Sessions.linkTasks)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.save) {
                        onSave(selectedTaskIds)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .onAppear {
            // Pre-select tasks already linked to this node
            let linked = node.node_tasks.filter { task in
                !task.is_deleted && session.user_tasks.contains(where: { $0.id == task.id })
            }
            selectedTaskIds = Set(linked.map(\.id))
        }
    }
}
