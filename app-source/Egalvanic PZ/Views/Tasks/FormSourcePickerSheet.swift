//
//  FormSourcePickerSheet.swift
//  Egalvanic PZ
//
//  Bottom-tray picker for the "Link Forms" button on the task detail
//  view. Two options: Legacy Forms (enabled, opens the existing
//  TaskFormLinkingView) and EG Forms (disabled until task #12 builds
//  the EG picker). Matches the visual convention of the asset-subtype
//  picker — list rows in a sheet, no nav chrome.
//

import SwiftUI
import UIKit

struct FormSourcePickerSheet: View {
    let onPickLegacy: () -> Void
    let onPickEG: () -> Void
    let onCancel: () -> Void

    // ZP-2336: EG Forms is gated behind the ``eg-forms`` company feature
    // flag. When disabled, the row is still shown so users know it exists,
    // but tapping is disabled and a lock indicator is rendered.
    private var hasEgForms: Bool {
        AuthService.shared.hasFeature("eg-forms")
    }

    var body: some View {
        VStack(spacing: 0) {
            // Sheet title
            Text(AppStrings.Forms.linkAForm)
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 4)

            Text(AppStrings.Forms.linkAFormSubtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.bottom, 16)

            VStack(spacing: 0) {
                row(
                    title: AppStrings.Forms.legacyForms,
                    subtitle: AppStrings.Forms.legacyFormsSubtitle,
                    systemImage: "doc.text",
                    enabled: true,
                    action: onPickLegacy
                )
                Divider().padding(.leading, 60)
                row(
                    title: AppStrings.Forms.egForms,
                    subtitle: hasEgForms
                        ? AppStrings.Forms.egFormsCatalog
                        : AppStrings.Forms.egFormsNotEnabled,
                    systemImage: "sparkles",
                    enabled: hasEgForms,
                    action: onPickEG
                )
            }
            .padding(.vertical, 4)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)

            Spacer(minLength: 0)
        }
        .background(Color(UIColor.systemGroupedBackground))
    }

    @ViewBuilder
    private func row(title: String, subtitle: String, systemImage: String,
                     enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(.system(size: 18))
                    .foregroundStyle(enabled ? Color.accentColor : Color(UIColor.tertiaryLabel))
                    .frame(width: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .foregroundStyle(enabled ? .primary : .secondary)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if enabled {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.tertiary)
                } else {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.tertiary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
