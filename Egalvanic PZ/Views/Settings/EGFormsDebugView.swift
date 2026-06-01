//
//  EGFormsDebugView.swift
//  Egalvanic PZ
//
//  ZP-1723 milestone 1 entry point. Lists every EGFormInstance currently
//  persisted in SwiftData and lets you tap one to open the renderer.
//  Temporary lives in Diagnostics; the real entry point (task detail →
//  linked forms) comes in milestone 3.
//

import SwiftUI
import SwiftData

struct EGFormsDebugView: View {
    @Query(sort: \EGFormInstance.created_at, order: .reverse)
    private var instances: [EGFormInstance]

    var body: some View {
        List {
            if instances.isEmpty {
                Section {
                    Text("No EG form instances yet. Sync an SLD that has EG forms attached.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
            } else {
                Section {
                    ForEach(instances) { inst in
                        NavigationLink(destination: EGFormView(instance: inst)) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(inst.egForm?.title ?? "Untitled form")
                                    .font(.subheadline).fontWeight(.medium)
                                HStack(spacing: 8) {
                                    Text(inst.statusDescription)
                                        .font(.caption2)
                                        .padding(.horizontal, 6).padding(.vertical, 2)
                                        .background(statusColor(inst.statusDescription).opacity(0.15))
                                        .foregroundStyle(statusColor(inst.statusDescription))
                                        .clipShape(Capsule())
                                    Text("Type: \(inst.egForm?.displayTypeLabel ?? "?")")
                                        .font(.caption2).foregroundStyle(.secondary)
                                    Text(inst.id.uuidString.prefix(8))
                                        .font(.caption2).foregroundStyle(.tertiary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                } header: {
                    Text("\(instances.count) instance(s)")
                }
            }
        }
        .navigationTitle("EG Forms (Debug)")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func statusColor(_ status: String) -> Color {
        switch status {
        case "Submitted":  return .green
        case "In Progress": return .orange
        default:           return .secondary
        }
    }
}
