import SwiftUI
import Foundation

// MARK: - Photo Type Enum
enum PhotoType: String, CaseIterable {
    case before = "task_before"
    case after = "task_after"
    case general = "task_general"
    
    var displayName: String {
        switch self {
        case .before: return "Before"
        case .after: return "After"
        case .general: return "General"
        }
    }
    
    var icon: String {
        switch self {
        case .before: return "clock.badge.questionmark"
        case .after: return "clock.badge.checkmark"
        case .general: return "photo"
        }
    }
    
    var color: Color {
        switch self {
        case .before: return .orange
        case .after: return .green
        case .general: return .blue
        }
    }
}

// MARK: - Modern UI Components
struct TaskHeaderSection: View {
    let title: String
    let isCompleted: Bool
    let nodeLabel: String?
    let networkMode: NetworkMode
    
    var body: some View {
        VStack(spacing: 16) {
            // Task Icon
            ZStack {
                Circle()
                    .fill(LinearGradient(
                        colors: [
                            (isCompleted ? Color.green : Color.blue).opacity(0.2),
                            (isCompleted ? Color.green : Color.blue).opacity(0.1)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .frame(width: 100, height: 100)
                
                Image(systemName: isCompleted ? "checkmark.circle.fill" : "checklist")
                    .font(.system(size: 50))
                    .foregroundColor(isCompleted ? .green : .blue)
            }
            .shadow(color: (isCompleted ? Color.green : Color.blue).opacity(0.3), radius: 10, y: 5)
            
            // Task Info
            VStack(spacing: 8) {
                Text(title)
                    .font(.title3)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Label(isCompleted ? AppStrings.Issues.resolved : AppStrings.CommonExtra.open, systemImage: isCompleted ? "checkmark.seal.fill" : "clock.fill")
                    .font(.subheadline)
                    .foregroundColor(isCompleted ? .green : .orange)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background((isCompleted ? Color.green : Color.orange).opacity(0.1))
                    .cornerRadius(20)
                
                if let nodeLabel = nodeLabel {
                    Label(nodeLabel, systemImage: "cube")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                if networkMode == .offline {
                    Label(AppStrings.CommonExtra.offline, systemImage: "wifi.slash")
                        .font(.caption)
                        .foregroundColor(.orange)
                }
            }
        }
        .padding(.top, 20)
        .padding(.bottom, 30)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [(isCompleted ? Color.green : Color.blue).opacity(0.1), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}

struct TaskSectionHeader: View {
    let title: String
    let systemImage: String
    
    var body: some View {
        HStack {
            Label(title, systemImage: systemImage)
                .font(.headline)
                .foregroundColor(.primary)
            Spacer()
        }
    }
}

struct TaskModernTextField: View {
    let label: String
    @Binding var text: String
    let placeholder: String
    var isRequired: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                if isRequired && text.isEmpty {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption2)
                        .foregroundColor(.red)
                }
            }
            
            TextField(placeholder, text: $text)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
        }
    }
}

struct TaskModernTextEditor: View {
    @Binding var text: String
    let placeholder: String
    let minHeight: CGFloat
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            if text.isEmpty {
                Text(placeholder)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 8)
            }
            
            TextEditor(text: $text)
                .scrollContentBackground(.hidden)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
        }
        .frame(minHeight: minHeight)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

struct TaskModernActionButton: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

struct TaskEmptyStateView: View {
    let icon: String
    let message: String
    let subMessage: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.secondary)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Text(subMessage)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity)
    }
}