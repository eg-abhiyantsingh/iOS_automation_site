import SwiftUI

struct EdgeCoreAttributesSection: View {
    let edge: EdgeV2?
    let selectedEdgeClass: EdgeClass?
    @Binding var draftAttributes: [UUID: String]
    @State private var showOnlyRequired = false
    
    var body: some View {
        EntityCoreAttributesView(
            entity: edge,
            selectedEntityClass: selectedEdgeClass,
            draftAttributes: $draftAttributes,
            showOnlyRequired: $showOnlyRequired,
            sectionTitle: AppStrings.AssetsExtra.edgeProperties,
            sectionIcon: "slider.horizontal.3"
        )
    }
}