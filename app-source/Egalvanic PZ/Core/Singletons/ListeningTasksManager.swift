//
//  ListeningTasksHelpers.swift
//  Egalvanic PZ
//
//  Convenience helpers for the listening tasks feature.
//  Actual state lives on NetworkState.listeningTaskIds.
//

import SwiftUI

// MARK: - NetworkState Convenience Extensions

extension NetworkState {
    func isListening(_ taskId: UUID) -> Bool {
        listeningTaskIds.contains(taskId)
    }

    func toggleListening(_ taskId: UUID) {
        if listeningTaskIds.contains(taskId) {
            listeningTaskIds.remove(taskId)
        } else {
            listeningTaskIds.insert(taskId)
        }
    }
}

// MARK: - Reusable Listening Toggle Button

struct ListeningToggleButton: View {
    let taskId: UUID
    @EnvironmentObject var networkState: NetworkState

    var body: some View {
        HStack {
            Button {
                networkState.toggleListening(taskId)
            } label: {
                let isOn = networkState.isListening(taskId)
                HStack(spacing: 6) {
                    Image(systemName: isOn ? "ear.fill" : "ear")
                        .font(.subheadline)
                    Text(isOn ? "Listening for Assets" : "Listen for Assets")
                        .font(.subheadline)
                }
                .foregroundColor(isOn ? .orange : .secondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background((isOn ? Color.orange : Color.gray).opacity(0.1))
                .cornerRadius(8)
            }
            Spacer()
        }
    }
}

// MARK: - Reusable Listening Task Check Row

struct ListeningTaskCheckRow: View {
    let task: UserTask
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 8) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .orange : .gray)
                    .font(.system(size: 16))
                Text(task.title)
                    .font(.subheadline)
                    .foregroundColor(.primary)
                Spacer()
            }
        }
    }
}
