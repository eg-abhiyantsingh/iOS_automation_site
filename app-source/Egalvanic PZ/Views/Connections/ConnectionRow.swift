//
//  ConnectionRow.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/3/25.
//

import SwiftUI

// MARK: - Standard Mode
struct ConnectionRow: View {
    let edge: EdgeV2
    let nodeLabel: (UUID) -> String
    let maxLabelLength: Int = 25

    private func truncatedLabel(_ label: String) -> String {
        if label.count > maxLabelLength {
            return String(label.prefix(maxLabelLength - 3)) + "..."
        }
        return label
    }

    private func resolveNodeLabel(_ nodeId: UUID?) -> String {
        guard let nodeId else { return AppStrings.Connections.notAssigned }
        return nodeLabel(nodeId)
    }

    private var hasUnknownNode: Bool {
        let sourceLabel = resolveNodeLabel(edge.source)
        let targetLabel = resolveNodeLabel(edge.target)
        return sourceLabel == AppStrings.Connections.unknownNode || targetLabel == AppStrings.Connections.unknownNode
    }

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 16) {
                // Source
                HStack(spacing: 8) {
                    Circle()
                        .fill(hasUnknownNode ? Color.red.opacity(0.2) : Color.green.opacity(0.2))
                        .frame(width: 8, height: 8)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(AppStrings.Connections.from)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text(truncatedLabel(resolveNodeLabel(edge.source)))
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(hasUnknownNode ? .red : .primary)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                
                // Arrow
                Image(systemName: "arrow.right")
                    .font(.system(size: 14))
                    .foregroundColor(hasUnknownNode ? .red.opacity(0.6) : .secondary)
                
                // Target
                HStack(spacing: 8) {
                    Circle()
                        .fill(hasUnknownNode ? Color.red.opacity(0.2) : Color.blue.opacity(0.2))
                        .frame(width: 8, height: 8)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(AppStrings.Connections.to)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text(truncatedLabel(resolveNodeLabel(edge.target)))
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(hasUnknownNode ? .red : .primary)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                
                // Chevron
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.accentColor)
            }
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.03), radius: 2, x: 0, y: 1)
    }
}

// MARK: - Edit Mode
struct ConnectionEditModeRow: View {
    let edge: EdgeV2
    let isSelected: Bool
    let onToggle: () -> Void
    let nodeLabel: (UUID) -> String

    private func resolveNodeLabel(_ nodeId: UUID?) -> String {
        guard let nodeId else { return AppStrings.Connections.notAssigned }
        return nodeLabel(nodeId)
    }

    private var hasUnknownNode: Bool {
        let sourceLabel = resolveNodeLabel(edge.source)
        let targetLabel = resolveNodeLabel(edge.target)
        return sourceLabel == AppStrings.Connections.unknownNode || targetLabel == AppStrings.Connections.unknownNode
    }

    var body: some View {
        HStack(spacing: 16) {
            // Selection indicator
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(.system(size: 24))
                .foregroundColor(isSelected ? .blue : .gray)
                .animation(.easeInOut(duration: 0.2), value: isSelected)
            
            // Connection visualization
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Circle()
                        .fill(hasUnknownNode ? Color.red.opacity(0.2) : Color.green.opacity(0.2))
                        .frame(width: 6, height: 6)
                    
                    Text(resolveNodeLabel(edge.source))
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(hasUnknownNode ? .red : .primary)
                        .lineLimit(1)
                }

                HStack(spacing: 8) {
                    Image(systemName: "arrow.down")
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    Circle()
                        .fill(hasUnknownNode ? Color.red.opacity(0.2) : Color.blue.opacity(0.2))
                        .frame(width: 6, height: 6)

                    Text(resolveNodeLabel(edge.target))
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(hasUnknownNode ? .red : .primary)
                        .lineLimit(1)
                }
            }
            
            Spacer()
        }
        .padding(16)
        .background(isSelected ? Color.blue.opacity(0.05) : Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isSelected ? Color.blue.opacity(0.3) : Color.clear, lineWidth: 2)
        )
        .contentShape(Rectangle())
        .onTapGesture(perform: onToggle)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}
