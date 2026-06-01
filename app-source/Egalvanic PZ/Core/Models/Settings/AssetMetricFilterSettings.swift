//
//  AssetMetricFilterSettings.swift
//  Egalvanic PZ
//
//  Settings for controlling which asset metrics are displayed on asset rows
//

import SwiftUI

/// Metrics that can be shown/hidden on asset rows in SessionRoomDetailView
enum AssetMetric: String, CaseIterable, Identifiable {
    case irCompletion = "IR Completion"
    case inboundConnection = "Inbound Connection"
    case arcFlashReadiness = "Arc Flash Readiness"
    case issueCount = "Issues"
    case taskCount = "Tasks"
    case childCount = "Children"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .irCompletion: return AppStrings.Sessions.metricIRCompletion
        case .inboundConnection: return AppStrings.Sessions.metricInboundConnection
        case .arcFlashReadiness: return AppStrings.Sessions.metricArcFlashReadiness
        case .issueCount: return AppStrings.Issues.issues
        case .taskCount: return AppStrings.Tasks.tasks
        case .childCount: return AppStrings.Sessions.metricChildren
        }
    }

    var iconName: String {
        switch self {
        case .irCompletion: return "checkmark.circle.fill"
        case .inboundConnection: return "arrow.down.circle.fill"
        case .arcFlashReadiness: return "bolt.circle.fill"
        case .issueCount: return "exclamationmark.triangle.fill"
        case .taskCount: return "checklist"
        case .childCount: return "cube"
        }
    }

    var color: Color {
        switch self {
        case .irCompletion: return .green
        case .inboundConnection: return .blue
        case .arcFlashReadiness: return .orange
        case .issueCount: return .orange
        case .taskCount: return .blue
        case .childCount: return Color.secondary
        }
    }

    var userDefaultsKey: String {
        "assetMetric_\(rawValue)"
    }
}

/// Observable settings for asset metric filters, persisted via UserDefaults
final class AssetMetricFilterSettings: ObservableObject {
    static let shared = AssetMetricFilterSettings()

    @Published var showIRCompletion: Bool {
        didSet { UserDefaults.standard.set(showIRCompletion, forKey: AssetMetric.irCompletion.userDefaultsKey) }
    }
    @Published var showInboundConnection: Bool {
        didSet { UserDefaults.standard.set(showInboundConnection, forKey: AssetMetric.inboundConnection.userDefaultsKey) }
    }
    @Published var showArcFlashReadiness: Bool {
        didSet { UserDefaults.standard.set(showArcFlashReadiness, forKey: AssetMetric.arcFlashReadiness.userDefaultsKey) }
    }
    @Published var showIssueCount: Bool {
        didSet { UserDefaults.standard.set(showIssueCount, forKey: AssetMetric.issueCount.userDefaultsKey) }
    }
    @Published var showTaskCount: Bool {
        didSet { UserDefaults.standard.set(showTaskCount, forKey: AssetMetric.taskCount.userDefaultsKey) }
    }
    @Published var showChildCount: Bool {
        didSet { UserDefaults.standard.set(showChildCount, forKey: AssetMetric.childCount.userDefaultsKey) }
    }

    /// Session overlay filter (IR, Arc Flash, C.O.M., or None) — persists across room navigation
    @Published var assetOverlayFilter: AssetOverlayFilter = .none

    private init() {
        // Load from UserDefaults (defaults to false = hidden for new users)
        self.showIRCompletion = UserDefaults.standard.bool(forKey: AssetMetric.irCompletion.userDefaultsKey)
        self.showInboundConnection = UserDefaults.standard.bool(forKey: AssetMetric.inboundConnection.userDefaultsKey)
        self.showArcFlashReadiness = UserDefaults.standard.bool(forKey: AssetMetric.arcFlashReadiness.userDefaultsKey)
        self.showIssueCount = UserDefaults.standard.bool(forKey: AssetMetric.issueCount.userDefaultsKey)
        self.showTaskCount = UserDefaults.standard.bool(forKey: AssetMetric.taskCount.userDefaultsKey)
        self.showChildCount = UserDefaults.standard.bool(forKey: AssetMetric.childCount.userDefaultsKey)
    }

    func isVisible(_ metric: AssetMetric) -> Bool {
        switch metric {
        case .irCompletion: return showIRCompletion
        case .inboundConnection: return showInboundConnection
        case .arcFlashReadiness: return showArcFlashReadiness
        case .issueCount: return showIssueCount
        case .taskCount: return showTaskCount
        case .childCount: return showChildCount
        }
    }

    func toggle(_ metric: AssetMetric) {
        switch metric {
        case .irCompletion: showIRCompletion.toggle()
        case .inboundConnection: showInboundConnection.toggle()
        case .arcFlashReadiness: showArcFlashReadiness.toggle()
        case .issueCount: showIssueCount.toggle()
        case .taskCount: showTaskCount.toggle()
        case .childCount: showChildCount.toggle()
        }
    }

    var hasAnyEnabled: Bool {
        showIRCompletion || showInboundConnection || showArcFlashReadiness ||
        showIssueCount || showTaskCount || showChildCount
    }
}
