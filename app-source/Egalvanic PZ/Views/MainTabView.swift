//
//  MainTabView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/7/25.
//

import SwiftUI

enum TopTab: Int, CaseIterable {
    case site, assets, connections, sld, settings
    
    var title: String {
        switch self {
        case .site:        return AppStrings.Tabs.siteInfo
        case .assets:      return AppStrings.Tabs.assetsList
        case .connections: return AppStrings.Tabs.connectionsList
        case .sld:         return AppStrings.Tabs.singleLineDiagram
        case .settings:    return AppStrings.Settings.title
        }
    }

    var tabItem: some View {
        switch self {
        case .site:
            Label(AppStrings.Tabs.site, systemImage: "house")
        case .assets:
            Label(AppStrings.Tabs.assets, systemImage: "cube.box.fill")
        case .connections:
            Label(AppStrings.Tabs.connections, systemImage: "link")
        case .sld:
            Label(AppStrings.Tabs.sld, systemImage: "network")
        case .settings:
            Label(AppStrings.Settings.title, systemImage: "gear")
        }
    }
}

struct MainTabView: View {
    let diagram: SLDV2?  // Now optional
    @EnvironmentObject private var sldService: SLDService
    @EnvironmentObject private var languageManager: LanguageManager

    var body: some View {
        TabView {
            // First Tab - Site View
            if let diagram = diagram {
                SiteTabView(diagram: diagram)
                    .navigationViewStyle(StackNavigationViewStyle())
                    .tag(TopTab.site)
                    .tabItem { TopTab.site.tabItem }
            } else {
                PlaceholderTabView(message: AppStrings.Tabs.loadingSiteInfo)
                    .tag(TopTab.site)
                    .tabItem { TopTab.site.tabItem }
            }
            
            // Second Tab - Assets List
            if let diagram = diagram {
                AssetsTabView(diagram: diagram)
                    .tabItem {
                        Label(AppStrings.Tabs.assets, systemImage: "list.bullet")
                    }
            } else {
                PlaceholderTabView(message: AppStrings.Tabs.loadingAssets)
                    .tabItem {
                        Label(AppStrings.Tabs.assets, systemImage: "list.bullet")
                    }
            }
            
            // Third Tab - Connections List
            // Hidden temporarily — wrap in `if false { ... }` rather than
            // deleting so it can be re-enabled by flipping the gate when
            // we're ready to expose Connections as its own tab again.
            if false {
                if let diagram = diagram {
                    ConnectionsTabView(diagram: diagram)
                        .tabItem {
                            Label(AppStrings.Tabs.connections, systemImage: "link")
                        }
                } else {
                    PlaceholderTabView(message: AppStrings.Tabs.loadingConnections)
                        .tabItem {
                            Label(AppStrings.Tabs.connections, systemImage: "link")
                        }
                }
            }
            
            // Fourth Tab - SLD (with View Selector)
            NavigationView {
                if let diagram = diagram {
                    SLDViewSelectorView(sld: diagram)
                } else {
                    PlaceholderTabView(message: AppStrings.Tabs.loadingDiagram)
                }
            }
            .navigationViewStyle(StackNavigationViewStyle())
            .tag(TopTab.sld)
            .tabItem { TopTab.sld.tabItem }
            
            // Fourth Tab - Settings
            NavigationView {
                SettingsView()
            }
            .navigationViewStyle(StackNavigationViewStyle())
            .tag(TopTab.settings)
            .tabItem { TopTab.settings.tabItem }
        }
        .task {
            await SldViewerAssetManager.shared.checkAndUpdate()
        }
    }
}

// MARK: - Placeholder View for Loading States
struct PlaceholderTabView: View {
    let message: String
    
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
            Text(message)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemBackground))
    }
}
