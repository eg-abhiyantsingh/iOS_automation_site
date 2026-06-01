import SwiftUI
import SwiftData

struct ArcFlashCompletionView: View {
    let diagram: SLDV2

    @StateObject private var viewModel: ArcFlashCompletionViewModel
    @State private var selectedNode: NodeV2?
    @State private var selectedEdge: EdgeV2?

    // Define metrics (presentation data)
    private let metrics = [
        (title: AppStrings.ArcFlash.assetDetails, icon: "square.grid.2x2.fill", color: Color.blue),
        (title: AppStrings.ArcFlash.sourceTarget, icon: "arrow.triangle.branch", color: Color.green),
        (title: AppStrings.ArcFlash.connectionDetails, icon: "link.circle.fill", color: Color.purple)
    ]

    // MARK: - Initialization

    init(diagram: SLDV2) {
        self.diagram = diagram
        self._viewModel = StateObject(wrappedValue: ArcFlashCompletionViewModel(diagram: diagram))
    }

    // MARK: - UI Helpers

    // Helper function to get node label with O(1) lookup via ViewModel
    private func getNodeLabel(for nodeId: UUID?) -> String {
        guard let nodeId else { return AppStrings.Connections.notAssigned }
        return viewModel.getNodeLabel(for: nodeId)
    }

    private func colorForRange(_ range: String) -> Color {
        if range == "100%" { return .green }
        if range.contains("76-99") { return .orange }
        return .red.opacity(0.7)
    }

    // MARK: - UI Components

    @ViewBuilder
    private var overallProgressCard: some View {
        VStack(spacing: 16) {
            // Title
            HStack {
                Image(systemName: "bolt.shield.fill")
                    .font(.title2)
                    .foregroundColor(.orange)

                Text(AppStrings.ArcFlash.readinessScore)
                    .font(.title2)
                    .fontWeight(.bold)

                Spacer()
            }

            // Large progress ring
            HStack(spacing: 32) {
                // Progress Ring
                ZStack {
                    Circle()
                        .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                        .frame(width: 120, height: 120)

                    Circle()
                        .trim(from: 0, to: viewModel.overallCompletion.percentage / 100)
                        .stroke(
                            viewModel.overallCompletion.percentage == 100 ? Color.green :
                            viewModel.overallCompletion.percentage >= 75 ? Color.orange :
                            viewModel.overallCompletion.percentage >= 50 ? Color.yellow :
                            Color.red,
                            style: StrokeStyle(lineWidth: 12, lineCap: .round)
                        )
                        .frame(width: 120, height: 120)
                        .rotationEffect(.degrees(-90))
                        .animation(.easeInOut(duration: 0.5), value: viewModel.overallCompletion.percentage)

                    VStack(spacing: 2) {
                        Text("\(Int(viewModel.overallCompletion.percentage))%")
                            .font(.system(size: 32, weight: .bold))

                        Text(AppStrings.ArcFlash.overall)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                // Summary stats
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 8) {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 8, height: 8)
                        Text(AppStrings.ArcFlash.completedCount(viewModel.overallCompletion.completedCount))
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }

                    HStack(spacing: 8) {
                        Circle()
                            .fill(Color.orange)
                            .frame(width: 8, height: 8)
                        Text(AppStrings.ArcFlash.remainingCount(viewModel.overallCompletion.totalCount - viewModel.overallCompletion.completedCount))
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }

                    HStack(spacing: 8) {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 8, height: 8)
                        Text(AppStrings.ArcFlash.totalItems(viewModel.overallCompletion.totalCount))
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                }

                Spacer()
            }
        }
        .padding(20)
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
        .padding(.horizontal, 16)
    }

    @ViewBuilder
    private var progressCirclesSection: some View {
        VStack(spacing: 12) {
            ForEach(0..<metrics.count, id: \.self) { index in
                CompactProgressCard(
                    metric: metrics[index],
                    data: viewModel.getMetricData(for: index),
                    isSelected: index == viewModel.selectedMetricIndex,
                    onTap: {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            viewModel.selectedMetricIndex = index
                        }
                    }
                )
            }
        }
        .padding(.horizontal, 16)
    }

    @ViewBuilder
    private var breakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(AppStrings.ArcFlash.breakdown(metrics[viewModel.selectedMetricIndex].title))
                .font(.headline)
                .padding(.horizontal, 16)

            Group {
                switch viewModel.selectedMetricIndex {
                case 0:
                    assetDetailsBreakdown
                case 1:
                    sourceTargetBreakdownView
                case 2:
                    connectionDetailsBreakdownView
                default:
                    EmptyView()
                }
            }
        }
        .padding(.top, 8)
    }

    @ViewBuilder
    private var assetDetailsBreakdown: some View {
        ForEach(viewModel.completionBreakdown, id: \.range) { item in
            DisclosureGroup {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(item.nodes, id: \.id) { node in
                        nodeRowLink(node: node, completion: node.af_completion)
                    }
                }
                .padding(.horizontal, 16)
            } label: {
                breakdownRowLabel(
                    title: item.range,
                    count: item.count,
                    unitSingular: AppStrings.ArcFlash.assetLabel(1),
                    unitPlural: AppStrings.ArcFlash.assetLabel(2),
                    color: colorForRange(item.range)
                )
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private var sourceTargetBreakdownView: some View {
        ForEach(viewModel.sourceTargetBreakdown, id: \.status) { item in
            DisclosureGroup {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(item.nodes, id: \.id) { node in
                        sourceTargetNodeRow(node: node, status: item.status)
                    }
                }
                .padding(.horizontal, 16)
            } label: {
                breakdownRowLabel(
                    title: item.status == "connected" ? AppStrings.ArcFlash.statusConnected : AppStrings.ArcFlash.statusMissingSource,
                    count: item.count,
                    unitSingular: AppStrings.ArcFlash.assetLabel(1),
                    unitPlural: AppStrings.ArcFlash.assetLabel(2),
                    color: item.status == "connected" ? .green : .orange
                )
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private var connectionDetailsBreakdownView: some View {
        ForEach(viewModel.connectionDetailsBreakdown, id: \.range) { item in
            DisclosureGroup {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(item.edges, id: \.id) { edge in
                        edgeRowLink(edge: edge)
                    }
                }
                .padding(.horizontal, 16)
            } label: {
                breakdownRowLabel(
                    title: item.range,
                    count: item.count,
                    unitSingular: AppStrings.ArcFlash.connectionLabel(1),
                    unitPlural: AppStrings.ArcFlash.connectionLabel(2),
                    color: colorForRange(item.range)
                )
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private func nodeRowLink(node: NodeV2, completion: Int) -> some View {
        Button(action: {
            selectedNode = node
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                    if let roomName = node.room?.name {
                        Text(roomName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Text("\(completion)%")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(completion == 100 ? .green : .orange)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func sourceTargetNodeRow(node: NodeV2, status: String) -> some View {
        Button(action: {
            selectedNode = node
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                    if let roomName = node.room?.name {
                        Text(roomName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: status == "connected" ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .font(.caption)
                    .foregroundColor(status == "connected" ? .green : .orange)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func edgeRowLink(edge: EdgeV2) -> some View {
        Button(action: {
            selectedEdge = edge
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(getNodeLabel(for: edge.source)) → \(getNodeLabel(for: edge.target))")
                        .font(.subheadline)
                        .lineLimit(1)
                    Text(AppStrings.ArcFlash.connection)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Text("\(edge.af_completion)%")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(edge.af_completion == 100 ? .green : .orange)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func breakdownRowLabel(title: String, count: Int, unitSingular: String, unitPlural: String, color: Color) -> some View {
        HStack {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)

            Spacer()

            HStack(spacing: 4) {
                Text("\(count)")
                    .font(.subheadline)
                    .fontWeight(.semibold)

                Text(count == 1 ? unitSingular : unitPlural)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 40, height: 4)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 16)
        .background(Color(UIColor.tertiarySystemBackground))
        .cornerRadius(8)
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            // Main content
            ScrollView {
                VStack(spacing: 20) {
                    // Grand overall progress card
                    overallProgressCard

                    // All three metric cards
                    progressCirclesSection

                    // Detailed breakdown section
                    if viewModel.shouldShowBreakdown {
                        breakdownSection
                    }
                }
                .padding(.vertical, 8)
            }
            .opacity(viewModel.isLoading ? 0.3 : 1.0)

            // Loading overlay
            if viewModel.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.5)
                        .tint(.orange)

                    Text(AppStrings.ArcFlash.loadingArcFlashAnalysis)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemBackground).opacity(0.9))
            }
        }
        .navigationTitle(AppStrings.ArcFlash.arcFlashAnalysis)
        .navigationBarTitleDisplayMode(.large)
        .fullScreenCover(item: $selectedNode) { node in
            NavigationStack {
                EditNodeDetailViewV3(node: node, sld: diagram)
            }
        }
        .fullScreenCover(item: $selectedEdge) { edge in
            NavigationStack {
                EditEdgeDetailViewV3(edge: edge)
            }
        }
        .onAppear {
            let appearTime = Date()
            AppLogger.log(.debug, "[ArcFlash] View appeared - Diagram info: Total nodes: \(diagram.nodes.count), Active nodes: \(diagram.nodes.filter { !$0.is_deleted }.count), Total edges: \(diagram.edges.count), Active edges: \(diagram.edges.filter { !$0.is_deleted }.count). Starting initial computation...", category: .task)

            // Compute all metrics once on appear
            viewModel.computeAllMetrics()

            let totalLoadTime = Date().timeIntervalSince(appearTime)
            AppLogger.log(.debug, "[ArcFlash] Total view load time: \(String(format: "%.3f", totalLoadTime))s", category: .task)
        }
        .onDisappear {
            // Cancel any ongoing computation to free resources
            viewModel.cancelComputation()
            AppLogger.log(.debug, "[ArcFlash] View disappeared, computation cancelled", category: .task)
        }
    }
}

// MARK: - Supporting Views

// Compact Progress Card Component
struct CompactProgressCard: View {
    let metric: (title: String, icon: String, color: Color)
    let data: (percentage: Double, completedCount: Int, totalCount: Int)
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                // Icon and title
                HStack(spacing: 12) {
                    Image(systemName: metric.icon)
                        .font(.title3)
                        .foregroundColor(metric.color)
                        .frame(width: 30)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(metric.title)
                            .font(.subheadline)
                            .fontWeight(.semibold)

                        Text("\(data.completedCount) of \(data.totalCount)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Progress and percentage
                HStack(spacing: 12) {
                    // Mini progress bar
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.gray.opacity(0.2))
                                .frame(height: 8)

                            RoundedRectangle(cornerRadius: 4)
                                .fill(
                                    data.percentage == 100 ? Color.green :
                                    data.percentage >= 75 ? Color.orange :
                                    data.percentage >= 50 ? Color.yellow :
                                    Color.red
                                )
                                .frame(width: geometry.size.width * (data.percentage / 100), height: 8)
                                .animation(.easeInOut(duration: 0.5), value: data.percentage)
                        }
                    }
                    .frame(width: 80, height: 8)

                    Text("\(Int(data.percentage))%")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(
                            data.percentage == 100 ? .green :
                            data.percentage >= 75 ? .orange :
                            .primary
                        )
                        .frame(width: 45, alignment: .trailing)
                }
            }
            .padding(16)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? metric.color.opacity(0.5) : Color.clear, lineWidth: 2)
            )
            .shadow(color: isSelected ? Color.black.opacity(0.08) : Color.black.opacity(0.03),
                    radius: isSelected ? 4 : 2,
                    x: 0,
                    y: 2)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isSelected ? 1.02 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}

// Keep the original Progress Circle Card Component for reference
struct ProgressCircleCard: View {
    let metric: (title: String, icon: String, color: Color)
    let data: (percentage: Double, completedCount: Int, totalCount: Int)
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 12) {
                HStack {
                    Image(systemName: metric.icon)
                        .font(.callout)
                        .foregroundColor(metric.color)

                    Text(metric.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)

                    Spacer()
                }

                // Progress Ring
                ZStack {
                    Circle()
                        .stroke(Color.gray.opacity(0.2), lineWidth: 10)
                        .frame(width: 100, height: 100)

                    Circle()
                        .trim(from: 0, to: data.percentage / 100)
                        .stroke(
                            data.percentage == 100 ? Color.green :
                            data.percentage >= 75 ? Color.orange :
                            data.percentage >= 50 ? Color.yellow :
                            Color.red,
                            style: StrokeStyle(lineWidth: 10, lineCap: .round)
                        )
                        .frame(width: 100, height: 100)
                        .rotationEffect(.degrees(-90))
                        .animation(.easeInOut(duration: 0.5), value: data.percentage)

                    VStack(spacing: 2) {
                        Text("\(Int(data.percentage))%")
                            .font(.system(size: 24, weight: .bold))

                        Text(AppStrings.ArcFlash.completeLabel)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }

                // Summary Stats
                HStack(spacing: 16) {
                    VStack(spacing: 2) {
                        Text("\(data.completedCount)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                        Text(AppStrings.ArcFlash.doneLabel)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }

                    VStack(spacing: 2) {
                        Text("\(data.totalCount)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        Text(AppStrings.ArcFlash.totalLabel)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(12)
            .frame(width: 180, height: 200)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Color.gray.opacity(0.3) : Color.clear, lineWidth: 2)
            )
            .shadow(color: isSelected ? Color.black.opacity(0.1) : Color.black.opacity(0.05),
                    radius: isSelected ? 6 : 3,
                    x: 0,
                    y: 2)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isSelected ? 1.02 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}
