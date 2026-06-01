//
//  VoiceAttributeRowView.swift
//  Egalvanic PZ
//
//  Wraps EntityAttributeRowView with a voice mic button.
//  For text/number/decimal fields, dictation fills the value directly.
//  For select/dropdown fields, dictation fuzzy-matches against available options.
//

import SwiftUI

struct VoiceAttributeRowView<PropertyDef: EntityPropertyDefinition>: View {
    let classProperty: PropertyDef
    let currentValue: String
    let onValueChange: (String) -> Void

    // Shared voice
    var speechManager: SpeechRecognitionManager
    var isVoiceTarget: Bool
    var onMicTapped: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            EntityAttributeRowView(
                classProperty: classProperty,
                currentValue: currentValue,
                onValueChange: onValueChange
            )

            if speechManager.isAvailable && isVoiceable {
                Button {
                    onMicTapped()
                } label: {
                    let isActive = isVoiceTarget && speechManager.isRecording
                    Image(systemName: isActive ? "mic.fill" : "mic")
                        .foregroundColor(isActive ? .red : .secondary)
                        .font(.caption)
                        .symbolEffect(.pulse, isActive: isActive)
                        .frame(width: 28, height: 28)
                        .background(Color(.systemGray5))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .padding(.top, 20) // Align with the input field (below the label)
            }
        }
        .onChange(of: speechManager.transcribedText) { _, newValue in
            guard isVoiceTarget, !newValue.isEmpty else { return }

            switch classProperty.type {
            case "select", "dropdown":
                let options = classProperty.options.compactMap { $0 }
                if let match = bestMatch(for: newValue, in: options) {
                    onValueChange(match)
                }
            case "boolean", "bool":
                let lower = newValue.lowercased()
                if lower.contains("yes") || lower.contains("true") || lower.contains("on") {
                    onValueChange("true")
                } else if lower.contains("no") || lower.contains("false") || lower.contains("off") {
                    onValueChange("false")
                }
            default:
                onValueChange(newValue)
            }
        }
    }

    /// Whether this field type supports voice input
    private var isVoiceable: Bool {
        switch classProperty.type {
        case "text", "string", "textfield", "number", "integer", "decimal", "float",
             "select", "dropdown", "boolean", "bool":
            return true
        default:
            return false
        }
    }

    /// Fuzzy match: find the option whose lowercased name best matches the spoken text.
    /// Uses longest common subsequence ratio as a simple similarity metric.
    private func bestMatch(for spoken: String, in options: [String]) -> String? {
        let spokenLower = spoken.lowercased()

        // First try exact containment (spoken text contains the option or vice versa)
        if let exact = options.first(where: { spokenLower.contains($0.lowercased()) }) {
            return exact
        }
        if let exact = options.first(where: { $0.lowercased().contains(spokenLower) }) {
            return exact
        }

        // Fall back to word-overlap scoring
        let spokenWords = Set(spokenLower.split(separator: " ").map(String.init))
        var bestOption: String?
        var bestScore = 0

        for option in options {
            let optionWords = Set(option.lowercased().split(separator: " ").map(String.init))
            let overlap = spokenWords.intersection(optionWords).count
            if overlap > bestScore {
                bestScore = overlap
                bestOption = option
            }
        }

        // Only accept if we matched at least one word
        return bestScore > 0 ? bestOption : nil
    }
}
