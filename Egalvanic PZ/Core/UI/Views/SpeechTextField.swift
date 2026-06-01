//
//  SpeechTextField.swift
//  Egalvanic PZ
//
//  Reusable text field with voice-to-text mic button, matching ModernTextField style.
//  Can own its own SpeechRecognitionManager or use a shared one.
//

import SwiftUI

struct SpeechTextField: View {
    let title: String
    @Binding var text: String
    let icon: String

    // Shared voice support (optional) — when provided, uses external manager
    var sharedSpeechManager: SpeechRecognitionManager?
    var isVoiceTarget: Bool = false
    var onMicTapped: (() -> Void)?

    // Private manager used only when no shared manager is provided
    @State private var ownSpeechManager = SpeechRecognitionManager()

    private var speechManager: SpeechRecognitionManager {
        sharedSpeechManager ?? ownSpeechManager
    }

    private var isRecordingHere: Bool {
        if sharedSpeechManager != nil {
            return isVoiceTarget && speechManager.isRecording
        }
        return speechManager.isRecording
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)

            HStack {
                Image(systemName: icon)
                    .foregroundColor(.secondary)
                    .frame(width: 20)

                TextField("Enter \(title.lowercased())", text: $text)

                if speechManager.isAvailable {
                    Button {
                        if let onMicTapped {
                            onMicTapped()
                        } else {
                            // Standalone mode
                            if !speechManager.isAuthorized {
                                speechManager.requestAuthorization()
                            } else {
                                speechManager.toggleRecording()
                            }
                        }
                    } label: {
                        Image(systemName: isRecordingHere ? "mic.fill" : "mic")
                            .foregroundColor(isRecordingHere ? .red : .secondary)
                            .font(.body)
                            .symbolEffect(.pulse, isActive: isRecordingHere)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(.systemGray6))
            .cornerRadius(10)
        }
        .onChange(of: speechManager.transcribedText) { _, newValue in
            guard !newValue.isEmpty else { return }
            if sharedSpeechManager != nil {
                // Shared mode: only accept when we're the active target
                if isVoiceTarget { text = newValue }
            } else {
                // Standalone mode
                text = newValue
            }
        }
        .onDisappear {
            if sharedSpeechManager == nil && speechManager.isRecording {
                speechManager.stopRecording()
            }
        }
        .onAppear {
            if sharedSpeechManager == nil {
                speechManager.requestAuthorization()
            }
        }
    }
}
