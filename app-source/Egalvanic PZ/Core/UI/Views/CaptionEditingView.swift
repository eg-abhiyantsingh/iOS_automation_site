//
//  CaptionEditingView.swift
//  Egalvanic PZ
//
import SwiftUI

struct CaptionEditingView: View {
    @Binding var caption: String
    let onSave: () -> Void
    let onCancel: () -> Void
    @Environment(\.dismiss) private var dismiss

    let maxCharacters = 256
    @FocusState private var isTextFieldFocused: Bool
    @State private var initialCaption: String

    init(caption: Binding<String>, onSave: @escaping () -> Void, onCancel: @escaping () -> Void) {
        self._caption = caption
        self.onSave = onSave
        self.onCancel = onCancel
        self._initialCaption = State(initialValue: caption.wrappedValue)
    }

    var isEditing: Bool {
        !initialCaption.isEmpty
    }

    var navigationTitle: String {
        isEditing ? AppStrings.Photos.editCaption : AppStrings.Photos.addCaption
    }

    var charactersRemaining: Int {
        maxCharacters - caption.count
    }

    var isOverLimit: Bool {
        caption.count > maxCharacters
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Text Editor
                ZStack(alignment: .topLeading) {
                    if caption.isEmpty {
                        Text(AppStrings.Photos.addCaptionPlaceholder)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 12)
                    }

                    TextEditor(text: $caption)
                        .focused($isTextFieldFocused)
                        .frame(minHeight: 120)
                        .padding(4)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(isOverLimit ? Color.red : Color.secondary.opacity(0.3), lineWidth: 1)
                        )
                }

                // Character Counter
                HStack {
                    Spacer()
                    Text("\(caption.count) / \(maxCharacters)")
                        .font(.caption)
                        .foregroundColor(isOverLimit ? .red : .secondary)
                }

                Spacer()
            }
            .padding()
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(AppStrings.Common.cancel) {
                        onCancel()
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(AppStrings.Common.save) {
                        // Trim caption to max characters if over limit
                        if caption.count > maxCharacters {
                            caption = String(caption.prefix(maxCharacters))
                        }
                        onSave()
                        dismiss()
                    }
                    .disabled(isOverLimit)
                }
            }
            .onAppear {
                isTextFieldFocused = true
            }
        }
    }
}

#Preview {
    @Previewable @State var caption = "Sample caption text"

    CaptionEditingView(
        caption: $caption,
        onSave: { AppLogger.log(.debug, "Saved: \(caption)", category: .photo) },
        onCancel: { AppLogger.log(.debug, "Cancelled", category: .photo) }
    )
}
