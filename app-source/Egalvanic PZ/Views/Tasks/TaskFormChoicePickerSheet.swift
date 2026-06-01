//
//  TaskFormChoicePickerSheet.swift
//  Egalvanic PZ
//
//  Bottom-tray picker that surfaces all forms linked to a task when
//  the user long-presses a task row and chooses "Open Form" and there
//  is more than one form to choose from. Shows legacy and EG form
//  instances together with a small badge on each row.
//

import SwiftUI
import UIKit

struct TaskFormChoicePickerSheet: View {
    let legacy: [FormInstance]
    let eg: [EGFormInstance]
    let onPickLegacy: (FormInstance) -> Void
    let onPickEG: (EGFormInstance) -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Text("Open Form")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 4)

            Text("This task has multiple forms. Pick one to open.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.bottom, 12)

            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(legacy) { instance in
                        row(
                            title: instance.formMaster?.title ?? "Untitled form",
                            subtitle: instance.statusDescription,
                            badge: "LEGACY",
                            badgeColor: .gray
                        ) {
                            onPickLegacy(instance)
                        }
                    }
                    ForEach(eg) { instance in
                        row(
                            title: instance.egForm?.title ?? "Untitled form",
                            subtitle: instance.statusDescription,
                            badge: instance.egForm?.displayTypeLabel ?? "EG",
                            badgeColor: .cyan
                        ) {
                            onPickEG(instance)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
    }

    @ViewBuilder
    private func row(title: String, subtitle: String, badge: String, badgeColor: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "doc.text.fill")
                    .foregroundStyle(.cyan)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .foregroundStyle(.primary)
                    HStack(spacing: 6) {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(badge)
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 6).padding(.vertical, 1)
                            .background(badgeColor.opacity(0.15))
                            .foregroundStyle(badgeColor)
                            .clipShape(Capsule())
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
