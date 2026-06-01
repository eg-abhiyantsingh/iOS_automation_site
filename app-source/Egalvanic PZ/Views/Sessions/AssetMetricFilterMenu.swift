//
//  AssetMetricFilterMenu.swift
//  Egalvanic PZ
//
//  Menu for toggling individual asset metric filters
//

import SwiftUI

/// Menu for toggling individual asset metric filters
struct AssetMetricFilterMenu: View {
    @ObservedObject var settings: AssetMetricFilterSettings

    var body: some View {
        Menu {
            ForEach(AssetMetric.allCases) { metric in
                let isEnabled = settings.isVisible(metric)
                Button(action: {
                    settings.toggle(metric)
                }) {
                    Label {
                        Text(metric.displayName)
                    } icon: {
                        Image(systemName: isEnabled ? "checkmark.circle.fill" : metric.iconName)
                    }
                }
                .tint(isEnabled ? .blue : nil)
            }
        } label: {
            Image(systemName: settings.hasAnyEnabled
                ? "line.3.horizontal.decrease.circle.fill"
                : "line.3.horizontal.decrease.circle")
                .font(.title3)
                .foregroundColor(settings.hasAnyEnabled ? .blue : .secondary)
        }
    }
}
