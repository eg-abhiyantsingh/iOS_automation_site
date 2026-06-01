//
//  AgentWebAppContainerView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/16/25.
//

import SwiftUI

// MARK: - AgentWebAppContainerView
struct AgentWebAppContainerView: View {
    @Environment(\.dismiss) private var dismiss
    
    let sld: SLDV2
    
    var body: some View {
        NavigationView {
            AgentWebViewBridge(sldId: sld.id.uuidString)
                .navigationTitle(AppStrings.Agent.agent)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(AppStrings.Common.done) {
                            dismiss()
                        }
                    }
                }
        }
    }
}
