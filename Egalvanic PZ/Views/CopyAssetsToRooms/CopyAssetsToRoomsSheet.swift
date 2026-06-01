//
//  CopyAssetsToRoomsSheet.swift
//  Egalvanic PZ
//
//  ZP-2198: 5-step wizard for copying session-mapped assets from a source room into
//  one or more target rooms in the same SLD. Mirrors Android's CopyAssetsToRoomsSheet
//  (commit cea4810).
//

import SwiftUI
import SwiftData

struct CopyAssetsToRoomsSheet: View {

    let sourceRoom: Room
    let session: IRSession
    let sld: SLDV2
    let onDismiss: () -> Void

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var networkState: NetworkState
    @StateObject private var viewModel = CopyAssetsToRoomsViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if viewModel.step != .running && viewModel.step != .done {
                    StepIndicator(current: viewModel.step)
                        .padding(.horizontal)
                        .padding(.top, 12)
                        .padding(.bottom, 8)
                }

                Divider()

                Group {
                    switch viewModel.step {
                    case .targetRooms:
                        TargetRoomsStep(viewModel: viewModel)
                    case .strategy:
                        StrategyStep(viewModel: viewModel)
                    case .confirm:
                        ConfirmStep(viewModel: viewModel)
                    case .running:
                        RunningStep(viewModel: viewModel)
                    case .done:
                        DoneStep(viewModel: viewModel)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                Divider()

                FooterButtons(
                    viewModel: viewModel,
                    onCancel: onDismiss,
                    onDone: onDismiss
                )
                .padding(.horizontal)
                .padding(.vertical, 12)
            }
            .navigationTitle(AppStrings.CopyAssetsRooms.title)
            .navigationBarTitleDisplayMode(.inline)
        }
        .onAppear {
            viewModel.initialize(
                sourceRoom: sourceRoom,
                session: session,
                sld: sld,
                modelContext: modelContext,
                networkState: networkState
            )
        }
    }
}

// MARK: - Step indicator

private struct StepIndicator: View {
    let current: CopyWizardStep

    private var currentIndex: Int {
        switch current {
        case .targetRooms: return 1
        case .strategy: return 2
        case .confirm: return 3
        case .running, .done: return 3
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            stepItem(number: 1, label: AppStrings.CopyAssetsRooms.stepTargetRooms)
            connector(activeAfter: 1)
            stepItem(number: 2, label: AppStrings.CopyAssetsRooms.stepStrategy)
            connector(activeAfter: 2)
            stepItem(number: 3, label: AppStrings.CopyAssetsRooms.stepConfirm)
        }
    }

    @ViewBuilder
    private func stepItem(number: Int, label: String) -> some View {
        let isActive = number == currentIndex
        let isPast = number < currentIndex

        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(isActive || isPast ? Color.blue : Color(.systemGray5))
                    .frame(width: 28, height: 28)
                if isPast {
                    Image(systemName: "checkmark")
                        .font(.caption.weight(.bold))
                        .foregroundColor(.white)
                } else {
                    Text("\(number)")
                        .font(.caption.weight(.semibold))
                        .foregroundColor(isActive ? .white : .secondary)
                }
            }

            Text(label)
                .font(.caption2)
                .fontWeight(isActive ? .semibold : .regular)
                .foregroundColor(isActive ? .blue : (isPast ? .secondary : .primary))
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity)
    }

    private func connector(activeAfter: Int) -> some View {
        Rectangle()
            .fill(currentIndex > activeAfter ? Color.blue : Color(.systemGray4))
            .frame(height: 1)
            .padding(.top, 14) // Aligns with the circle's vertical center.
    }
}

// MARK: - Step 1: Target rooms

private struct TargetRoomsStep: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 6) {
                Text(AppStrings.CopyAssetsRooms.targetsHint)
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if viewModel.sourceParentCount == 0 {
                    Text(AppStrings.CopyAssetsRooms.noAssets)
                        .font(.footnote)
                        .foregroundColor(.red)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
            .padding(.top, 12)

            HStack(spacing: 8) {
                HStack {
                    Image(systemName: "magnifyingglass").foregroundColor(.secondary)
                    TextField(
                        AppStrings.CopyAssetsRooms.searchRoomsHint,
                        text: Binding(
                            get: { viewModel.roomSearch },
                            set: { viewModel.setRoomSearch($0) }
                        )
                    )
                    .textFieldStyle(.plain)
                    if !viewModel.roomSearch.isEmpty {
                        Button { viewModel.setRoomSearch("") } label: {
                            Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                        }
                    }
                }
                .padding(8)
                .background(Color(.systemGray6))
                .cornerRadius(8)

                Button(AppStrings.CopyAssetsRooms.selectAll) {
                    viewModel.toggleAllVisibleTargetRooms()
                }
                .font(.subheadline)
                .disabled(viewModel.visibleTargetRooms.isEmpty)
            }
            .padding(.horizontal)
            .padding(.top, 12)

            if viewModel.targetRooms.isEmpty {
                Spacer()
                Text(AppStrings.CopyAssetsRooms.noOtherRooms)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding()
                Spacer()
            } else {
                List {
                    ForEach(viewModel.visibleTargetRooms) { item in
                        TargetRoomRow(
                            item: item,
                            isSelected: viewModel.selectedTargetRoomIds.contains(item.id),
                            existingCount: viewModel.existingAssetsByTarget[item.id] ?? 0,
                            onToggle: { viewModel.toggleTargetRoom(item.id) }
                        )
                    }
                }
                .listStyle(.plain)

                Text(AppStrings.CopyAssetsRooms.targetCountSelected(viewModel.selectedTargetRoomIds.count))
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal)
                    .padding(.bottom, 4)
            }
        }
    }
}

private struct TargetRoomRow: View {
    let item: CopyRoomItem
    let isSelected: Bool
    let existingCount: Int
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: 12) {
                Image(systemName: isSelected ? "checkmark.square.fill" : "square")
                    .foregroundColor(isSelected ? .blue : .secondary)
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.roomName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    Text("\(item.buildingName) › \(item.floorName)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if existingCount > 0 {
                    Text(AppStrings.CopyAssetsRooms.existingAssetsBadge(existingCount))
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(Color.orange.opacity(0.15))
                        .foregroundColor(.orange)
                        .cornerRadius(6)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Step 2: Strategy

private struct StrategyStep: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(AppStrings.CopyAssetsRooms.strategyHint)
                .font(.subheadline)
                .foregroundColor(.secondary)

            Text(AppStrings.CopyAssetsRooms.strategyTitle)
                .font(.subheadline.weight(.semibold))

            StrategyOption(
                title: AppStrings.CopyAssetsRooms.append,
                description: AppStrings.CopyAssetsRooms.appendDesc,
                isSelected: viewModel.strategy == .append,
                isDestructive: false,
                onSelect: { viewModel.setStrategy(.append) }
            )

            StrategyOption(
                title: AppStrings.CopyAssetsRooms.overwrite,
                description: AppStrings.CopyAssetsRooms.overwriteDesc,
                isSelected: viewModel.strategy == .overwrite,
                isDestructive: true,
                onSelect: { viewModel.setStrategy(.overwrite) }
            )

            Spacer()
        }
        .padding()
    }
}

private struct StrategyOption: View {
    let title: String
    let description: String
    let isSelected: Bool
    let isDestructive: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(isSelected ? (isDestructive ? .red : .blue) : .secondary)
                    .font(.title3)
                    .padding(.top, 1)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(isDestructive ? .red : .primary)
                    Text(description)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(isSelected ? (isDestructive ? Color.red.opacity(0.06) : Color.blue.opacity(0.06)) : Color(.systemGray6))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isSelected ? (isDestructive ? Color.red : Color.blue) : Color.clear, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Step 3: Confirm

private struct ConfirmStep: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                infoBox(
                    text: AppStrings.CopyAssetsRooms.confirmMessage(
                        assetCount: viewModel.sourceParentCount,
                        source: viewModel.sourceBreadcrumb,
                        roomCount: viewModel.selectedTargetRoomIds.count
                    ),
                    color: .blue
                )

                section(title: AppStrings.CopyAssetsRooms.targetsLabel) {
                    ForEach(viewModel.selectedTargetRooms) { room in
                        Text(room.breadcrumb)
                            .font(.footnote)
                            .padding(.vertical, 2)
                    }
                }

                section(title: AppStrings.CopyAssetsRooms.strategyLabel) {
                    Text(viewModel.strategy == .append
                         ? AppStrings.CopyAssetsRooms.append
                         : AppStrings.CopyAssetsRooms.overwrite)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(viewModel.strategy == .overwrite ? .red : .primary)
                }

                if viewModel.strategy == .overwrite && viewModel.overwriteUnlinkCount > 0 {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(AppStrings.CopyAssetsRooms.overwriteWarningTitle)
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(.red)
                        Text(AppStrings.CopyAssetsRooms.overwriteWarningBody(viewModel.overwriteUnlinkCount))
                            .font(.footnote)
                            .foregroundColor(.red.opacity(0.85))
                    }
                    .padding(12)
                    .background(Color.red.opacity(0.08))
                    .cornerRadius(8)
                }
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func infoBox(text: String, color: Color) -> some View {
        Text(text)
            .font(.subheadline)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(color.opacity(0.08))
            .cornerRadius(8)
    }

    @ViewBuilder
    private func section<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundColor(.secondary)
                .textCase(.uppercase)
            content()
        }
    }
}

// MARK: - Step 4: Running

private struct RunningStep: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            ProgressView()
                .scaleEffect(1.4)
            Text(AppStrings.CopyAssetsRooms.runningProgress(
                current: max(viewModel.completedRooms, 1),
                total: max(viewModel.totalRooms, 1)
            ))
            .font(.subheadline)
            .foregroundColor(.secondary)
            Spacer()
        }
        .padding()
    }
}

// MARK: - Step 5: Done

private struct DoneStep: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if viewModel.totalFailed == 0 {
                    successBanner
                } else {
                    partialBanner
                }

                ForEach(viewModel.selectedTargetRooms) { room in
                    let result = viewModel.perTargetResult[room.id]
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(room.roomName)
                                .font(.subheadline.weight(.medium))
                            Text("\(room.buildingName) › \(room.floorName)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        if let r = result {
                            if r.skipped {
                                Text(AppStrings.CopyAssetsRooms.perRoomSkipped)
                                    .font(.caption)
                                    .foregroundColor(.orange)
                            } else {
                                Text(AppStrings.CopyAssetsRooms.perRoomResult(created: r.created, failed: r.failed))
                                    .font(.caption)
                                    .foregroundColor(r.failed == 0 ? .green : .orange)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                    Divider()
                }
            }
            .padding()
        }
    }

    private var successBanner: some View {
        let message = viewModel.wasOffline
            ? AppStrings.CopyAssetsRooms.successMessageOffline(
                assetCount: viewModel.totalCreated,
                roomCount: viewModel.totalRooms
            )
            : AppStrings.CopyAssetsRooms.successMessageOnline(
                assetCount: viewModel.totalCreated,
                roomCount: viewModel.totalRooms
            )
        return Text(message)
            .font(.subheadline)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color.green.opacity(0.1))
            .foregroundColor(.green)
            .cornerRadius(8)
    }

    private var partialBanner: some View {
        Text(AppStrings.CopyAssetsRooms.partialMessage(
            created: viewModel.totalCreated,
            failed: viewModel.totalFailed
        ))
        .font(.subheadline)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.orange.opacity(0.1))
        .foregroundColor(.orange)
        .cornerRadius(8)
    }
}

// MARK: - Footer

private struct FooterButtons: View {
    @ObservedObject var viewModel: CopyAssetsToRoomsViewModel
    let onCancel: () -> Void
    let onDone: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            switch viewModel.step {
            case .targetRooms:
                Button(AppStrings.CopyAssetsRooms.cancel, action: onCancel)
                    .buttonStyle(.bordered)
                Spacer()
                Button(AppStrings.CopyAssetsRooms.next) { viewModel.goNext() }
                    .buttonStyle(.borderedProminent)
                    .disabled(!viewModel.canGoNext)

            case .strategy:
                Button(AppStrings.CopyAssetsRooms.cancel, action: onCancel)
                    .buttonStyle(.bordered)
                Button(AppStrings.CopyAssetsRooms.back) { viewModel.goBack() }
                    .buttonStyle(.bordered)
                Spacer()
                Button(AppStrings.CopyAssetsRooms.next) { viewModel.goNext() }
                    .buttonStyle(.borderedProminent)

            case .confirm:
                Button(AppStrings.CopyAssetsRooms.cancel, action: onCancel)
                    .buttonStyle(.bordered)
                Button(AppStrings.CopyAssetsRooms.back) { viewModel.goBack() }
                    .buttonStyle(.bordered)
                Spacer()
                Button(AppStrings.CopyAssetsRooms.run) { viewModel.goNext() }
                    .buttonStyle(.borderedProminent)
                    .tint(viewModel.strategy == .overwrite ? .red : .blue)

            case .running:
                Spacer()

            case .done:
                Spacer()
                Button(AppStrings.CopyAssetsRooms.done, action: onDone)
                    .buttonStyle(.borderedProminent)
            }
        }
    }
}
