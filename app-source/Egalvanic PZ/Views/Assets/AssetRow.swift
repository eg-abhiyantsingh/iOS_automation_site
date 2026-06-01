//
//  AssetRow.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/3/25.
//

import SwiftUI

struct AssetRow: View {
    let node: NodeV2
    let showCompletion: Bool
    let showOpenTasksMode: Bool
    let showIRPunchlist: Bool
    let irPhotoCount: Int

    private var statusIcon: (name: String, color: Color) {
        node.af_isComplete
            ? ("checkmark.circle.fill", .green)
            : ("xmark.circle.fill", .red)
    }

    var body: some View {
        HStack(spacing: 16) {
            // Node type icon
            NodeTypeIconCircle(
                style: node.node_class?.style,
                size: 44,
                iconSize: 22,
                backgroundColor: Color.blue.opacity(0.1),
                iconColor: .blue
            )

            // Main content
            VStack(alignment: .leading, spacing: 4) {
                Text(node.label)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)

                HStack(spacing: 12) {
                    // Type
                    Text(node.node_class?.name ?? node.type)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    // Room name if available
                    if let room = node.room, !room.name.isEmpty {
                        HStack(spacing: 2) {
                            Image(systemName: "location.fill")
                                .font(.caption2)
                            Text(room.name)
                                .font(.caption)
                        }
                        .foregroundColor(.secondary)
                    }
                }
            }

            Spacer()

            // Status indicators
            HStack(spacing: 12) {
                // Open tasks badge
                if showOpenTasksMode && node.openTasksCount > 0 {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.orange.opacity(0.15))
                            .frame(width: 32, height: 24)
                        
                        Text("\(node.openTasksCount)")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.orange)
                    }
                }
                
                // IR photo count badge
                if showIRPunchlist && irPhotoCount > 0 {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.purple.opacity(0.15))
                            .frame(width: 32, height: 24)
                        
                        Text("\(irPhotoCount)")
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.purple)
                    }
                }

                // Arc Flash status
                if showCompletion {
                    Image(systemName: statusIcon.name)
                        .font(.system(size: 18))
                        .foregroundColor(statusIcon.color)
                }
                
                // Chevron
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.accentColor)
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.03), radius: 2, x: 0, y: 1)
    }
}


struct AssetEditModeRow: View {
    let node: NodeV2
    let isSelected: Bool
    let onToggle: () -> Void
    
    var body: some View {
        HStack(spacing: 16) {
            // Selection indicator
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(.system(size: 24))
                .foregroundColor(isSelected ? .blue : .gray)
                .animation(.easeInOut(duration: 0.2), value: isSelected)

            // Node type icon
            NodeTypeIconCircle(
                style: node.node_class?.style,
                size: 44,
                iconSize: 22,
                backgroundColor: Color.blue.opacity(0.1),
                iconColor: .blue
            )

            // Main content
            VStack(alignment: .leading, spacing: 4) {
                Text(node.label)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)

                Text(node.node_class?.name ?? node.type)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
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
