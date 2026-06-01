//
//  EGFormView.swift
//  Egalvanic PZ
//
//  ZP-1723: top-level SwiftUI renderer for an EG form instance.
//
//  Milestone 1 (this commit) supports the six layout/display block types
//  needed to see a form's overall shape: `text`, `message`, `image`,
//  `divider`, `container`, `columns`. Interactive blocks (input, select,
//  data_table, signature, image_capture) come in milestone 2 and route
//  through the same dispatcher.
//

import SwiftUI
import UIKit

struct EGFormView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel: EGFormViewModel
    @State private var isSaving = false
    @State private var saveBanner: SaveBanner? = nil
    @State private var showingCancelPrompt = false
    // ZP-2363: assets linked to this form instance. Kept as local state so
    // the chips/picker refresh immediately on edit; the canonical store is
    // `instance.linkedNodes`, which we update in lockstep.
    @State private var linkedNodes: [NodeV2]
    @State private var showingAssetPicker = false
    let readOnly: Bool
    let onCancel: (() -> Void)?
    /// Parent task, when the form is opened from a task. Supplies the pool
    /// of assets the user may link (web only offers the task's own nodes).
    let task: UserTask?

    init(instance: EGFormInstance, readOnly: Bool = false, task: UserTask? = nil, onCancel: (() -> Void)? = nil) {
        _viewModel = State(initialValue: EGFormViewModel(instance: instance))
        _linkedNodes = State(initialValue: instance.linkedNodes.filter { !$0.is_deleted })
        self.readOnly = readOnly
        self.task = task
        self.onCancel = onCancel
    }

    /// Assets the user may link — the parent task's own linked assets,
    /// mirroring the web "Linked Assets" dropdown (TaskFormFields.jsx).
    private var taskLinkedNodes: [NodeV2] {
        (task?.linkedNodes.filter { !$0.is_deleted } ?? []).sorted { $0.label < $1.label }
    }

    /// Show the section when the user can link (edit mode + task has
    /// assets) or, in read-only mode, when there's already something to show.
    private var shouldShowLinkedAssets: Bool {
        if readOnly { return !linkedNodes.isEmpty }
        return task != nil && !taskLinkedNodes.isEmpty
    }

    var body: some View {
        // Two presentation modes:
        // - onCancel != nil: shown as a fullScreenCover from a screen
        //   that already owns a NavigationStack. We render our own
        //   header instead of using `.toolbar`/`.navigationTitle` —
        //   nesting NavigationStacks inside the cover throws an
        //   NSInternalInconsistencyException about a top item belonging
        //   to a different navigation bar (iOS 17/18 SwiftUI bug).
        // - onCancel == nil: pushed via NavigationLink. Safe to use the
        //   parent NavigationStack's toolbar.
        Group {
            if onCancel != nil {
                VStack(spacing: 0) {
                    presentationHeader
                    Divider()
                    formScrollContent
                }
            } else {
                formScrollContent
                    .navigationTitle(viewModel.instance.egForm?.title ?? "EG Form")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        if !readOnly {
                            ToolbarItem(placement: .topBarTrailing) { submitButton }
                        }
                    }
            }
        }
        .environment(viewModel)
        .sheet(isPresented: $showingAssetPicker) {
            EGFormAssetLinkSheet(
                availableNodes: taskLinkedNodes,
                selectedNodeIds: Set(linkedNodes.map { $0.id }),
                onSave: saveLinkedAssets
            )
        }
        .overlay(alignment: .bottom) {
            if let banner = saveBanner {
                Text(banner.message)
                    .font(.caption)
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(banner.success ? Color.green.opacity(0.85) : Color.red.opacity(0.85))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                    .padding(.bottom, 16)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    // MARK: - Subviews

    private var formScrollContent: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: EGFormStyle.blockGap) {
                if shouldShowLinkedAssets {
                    linkedAssetsSection
                }
                if viewModel.definition.isEmpty {
                    Text("This form has no definition. Raw value: \(viewModel.instance.egForm?.definition?.prefix(120) ?? "nil")")
                        .font(EGFormStyle.smallFont)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(viewModel.definition.enumerated()), id: \.offset) { _, block in
                        EGBlockDispatcher(block: block, dataPath: [], readOnly: readOnly, onSubmit: submit)
                    }
                }
            }
            .padding(EGFormStyle.panelPad)
            .frame(maxWidth: .infinity, alignment: .leading)
            // Tap outside any field to dismiss. Attached to the inner
            // content stack so the gesture still fires over empty
            // padding but is ignored by interactive subviews (buttons,
            // text fields) that consume the tap themselves.
            .contentShape(Rectangle())
            .onTapGesture { dismissKeyboard() }
        }
        // Drag the form to dismiss the keyboard mid-scroll.
        .scrollDismissesKeyboard(.interactively)
    }

    private func dismissKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil, from: nil, for: nil
        )
    }

    private var presentationHeader: some View {
        HStack(spacing: 12) {
            if onCancel != nil {
                Button(AppStrings.Common.cancel) { handleCancelTapped() }
                    .foregroundStyle(.tint)
            }
            Spacer(minLength: 8)
            Text(viewModel.instance.egForm?.title ?? "EG Form")
                .font(.headline)
                .lineLimit(1)
                .truncationMode(.middle)
            Spacer(minLength: 8)
            if !readOnly {
                submitButton
            } else {
                // Keep title centered when there's no trailing button.
                Color.clear.frame(width: 60, height: 1)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(.bar)
        .confirmationDialog(
            AppStrings.Forms.saveChangesBeforeClosing,
            isPresented: $showingCancelPrompt,
            titleVisibility: .visible
        ) {
            Button(AppStrings.Forms.saveAndClose) { submit(dismissOnSuccess: true) }
            Button(AppStrings.Forms.discardChanges, role: .destructive) {
                viewModel.markClean()
                onCancel?()
            }
            Button(AppStrings.Common.keepEditing, role: .cancel) { }
        }
    }

    @ViewBuilder
    private var submitButton: some View {
        Button { submit(dismissOnSuccess: true) } label: {
            if isSaving {
                ProgressView().scaleEffect(0.8)
            } else {
                Text(AppStrings.Common.submit).fontWeight(.semibold)
            }
        }
        .disabled(isSaving)
    }

    // MARK: - Cancel / submit

    private func handleCancelTapped() {
        if !readOnly, viewModel.isDirty {
            showingCancelPrompt = true
        } else {
            onCancel?()
        }
    }

    /// Used by the tap on `button`-type form blocks inside the form
    /// itself. Submitting from the body has identical semantics — push
    /// the form, then close on success.
    private func submit() { submit(dismissOnSuccess: true) }

    private func submit(dismissOnSuccess: Bool) {
        isSaving = true
        let submissionJSON = viewModel.formDataString()
        EGFormInstanceService.updateEGFormInstance(
            viewModel.instance,
            submission: submissionJSON,
            submitted: true,
            // Always ship the current link set (never nil) so the server's
            // full-replace keeps them in sync — matches web, which sends
            // node_ids on every save.
            nodeIds: linkedNodes.map { $0.id },
            modelContext: modelContext
        ) { ok, message in
            isSaving = false
            if ok {
                viewModel.markClean()
                if dismissOnSuccess, let onCancel = onCancel {
                    // Offline path also lands here (queued + ok==true) —
                    // we close immediately rather than waiting on the
                    // server round-trip. The toast is suppressed on
                    // success since the dismissal IS the feedback.
                    onCancel()
                    return
                }
            }
            // Push-mode (no onCancel) or failure: show the toast so the
            // user still gets feedback.
            withAnimation {
                saveBanner = SaveBanner(success: ok, message: message ?? (ok ? "Submitted" : "Submit failed"))
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                withAnimation { saveBanner = nil }
            }
        }
    }

    // MARK: - Linked assets

    @ViewBuilder
    private var linkedAssetsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(AppStrings.Tasks.linkedAssets)
                    .font(EGFormStyle.bodyBold)
                    .foregroundStyle(.primary)
                Spacer()
                if !readOnly {
                    Button { showingAssetPicker = true } label: {
                        Label(AppStrings.Tasks.linkAssets, systemImage: "link")
                            .font(EGFormStyle.smallFont)
                    }
                }
            }

            if linkedNodes.isEmpty {
                Text(AppStrings.Tasks.noAssetsLinkedYet)
                    .font(EGFormStyle.smallFont)
                    .italic()
                    .foregroundStyle(.tertiary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(linkedNodes) { node in
                            Text(node.label)
                                .font(EGFormStyle.smallFont)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color(.systemGray5))
                                .clipShape(Capsule())
                        }
                    }
                }
            }
        }
        .padding(EGFormStyle.panelPad)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EGFormStyle.panelHdrBg)
        .overlay(
            RoundedRectangle(cornerRadius: EGFormStyle.panelRadius)
                .stroke(EGFormStyle.panelBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: EGFormStyle.panelRadius))
    }

    /// Persist a changed asset link selection. Mirrors the web "Save Draft"
    /// path: pushes the current submission together with `node_ids` so the
    /// server full-replaces the instance's node mappings. Keeps `submitted`
    /// unchanged (linking is not a submit).
    private func saveLinkedAssets(_ nodes: [NodeV2]) {
        linkedNodes = nodes
        viewModel.instance.linkedNodes = nodes
        EGFormInstanceService.updateEGFormInstance(
            viewModel.instance,
            submission: viewModel.formDataString(),
            submitted: viewModel.instance.submitted,
            nodeIds: nodes.map { $0.id },
            modelContext: modelContext
        ) { ok, message in
            // Success feedback is the updated chips; only surface failures
            // (and the offline "saved locally" notice) via the banner.
            guard let message, !ok || NetworkState.shared.mode == .offline else { return }
            withAnimation { saveBanner = SaveBanner(success: ok, message: message) }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                withAnimation { saveBanner = nil }
            }
        }
    }
}

private struct SaveBanner: Equatable {
    let success: Bool
    let message: String
}

/// ZP-2363: task-scoped asset picker for linking assets to an EG form
/// instance. Offers only the parent task's linked assets (matching the web
/// "Linked Assets" dropdown), with search + multi-select. Returns the chosen
/// nodes via `onSave`.
private struct EGFormAssetLinkSheet: View {
    @Environment(\.dismiss) private var dismiss

    let availableNodes: [NodeV2]
    let onSave: ([NodeV2]) -> Void

    @State private var selectedNodeIds: Set<UUID>
    @State private var searchText = ""

    init(availableNodes: [NodeV2], selectedNodeIds: Set<UUID>, onSave: @escaping ([NodeV2]) -> Void) {
        self.availableNodes = availableNodes
        self.onSave = onSave
        self._selectedNodeIds = State(initialValue: selectedNodeIds)
    }

    private var filteredNodes: [NodeV2] {
        guard !searchText.isEmpty else { return availableNodes }
        return availableNodes.filter { node in
            node.label.localizedCaseInsensitiveContains(searchText) ||
            (node.location?.localizedCaseInsensitiveContains(searchText) ?? false) ||
            (node.qr_code?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.CommonExtra.searchAssets, text: $searchText)
                        .textFieldStyle(.plain)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()

                if !selectedNodeIds.isEmpty {
                    HStack {
                        Text(AppStrings.Tasks.assetsSelected(selectedNodeIds.count))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button(AppStrings.AssetsExtra.clearAll) {
                            selectedNodeIds.removeAll()
                        }
                        .font(.caption)
                        .foregroundColor(.blue)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }

                List {
                    if filteredNodes.isEmpty {
                        Text(AppStrings.Tasks.noAssetsAvailable)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .italic()
                            .padding()
                    } else {
                        ForEach(filteredNodes) { node in
                            nodeRow(node)
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
            .navigationTitle(AppStrings.Tasks.linkAssetsToForm)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.cancel) { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(AppStrings.Common.done) {
                        onSave(availableNodes.filter { selectedNodeIds.contains($0.id) })
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }

    @ViewBuilder
    private func nodeRow(_ node: NodeV2) -> some View {
        Button {
            if selectedNodeIds.contains(node.id) {
                selectedNodeIds.remove(node.id)
            } else {
                selectedNodeIds.insert(node.id)
            }
        } label: {
            HStack {
                Image(systemName: selectedNodeIds.contains(node.id) ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(selectedNodeIds.contains(node.id) ? .blue : .gray)
                    .imageScale(.large)

                VStack(alignment: .leading, spacing: 2) {
                    Text(node.label)
                        .font(.subheadline)
                        .foregroundColor(.primary)

                    HStack(spacing: 8) {
                        if let location = node.location {
                            Label(location, systemImage: "location")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        if let qrCode = node.qr_code {
                            Label(qrCode, systemImage: "qrcode")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}

/// Routes a single block to the correct SwiftUI view based on `block.type`.
/// Honors `content.visible` (returns EmptyView when false) so the dispatcher
/// also serves as the visibility gate — matches the JSX renderer.
struct EGBlockDispatcher: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    let onSubmit: () -> Void
    @Environment(EGFormViewModel.self) private var viewModel

    init(block: EGBlock, dataPath: [String], readOnly: Bool, onSubmit: @escaping () -> Void = {}) {
        self.block = block
        self.dataPath = dataPath
        self.readOnly = readOnly
        self.onSubmit = onSubmit
    }

    var body: some View {
        if !viewModel.isVisible(block.content?.visible) {
            EmptyView()
        } else {
            content
        }
    }

    @ViewBuilder
    private var content: some View {
        switch block.type {
        case "container":
            EGContainerBlockView(block: block, dataPath: dataPath, readOnly: readOnly, onSubmit: onSubmit)
        case "columns":
            EGColumnsBlockView(block: block, dataPath: dataPath, readOnly: readOnly, onSubmit: onSubmit)
        case "text":
            EGTextBlockView(block: block)
        case "message":
            EGMessageBlockView(block: block)
        case "image":
            EGImageBlockView(block: block)
        case "divider":
            EGDividerBlockView()
        case "input":
            EGInputBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "select":
            EGSelectBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "multiselect":
            EGMultiselectBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "radio":
            EGRadioBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "switch":
            EGSwitchBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "checkbox":
            EGCheckboxBlockView(block: block, dataPath: dataPath, readOnly: readOnly)
        case "button":
            EGButtonBlockView(block: block, onSubmit: onSubmit)
        case "data_table":
            EGFormDataTableBlock(block: block, dataPath: dataPath, readOnly: readOnly)
        case "signature":
            EGFormSignatureBlock(block: block, dataPath: dataPath, readOnly: readOnly)
        case "image_capture":
            EGFormImageCaptureBlock(block: block, dataPath: dataPath, readOnly: readOnly)
        default:
            // Unknown / not-yet-implemented block. Show a placeholder so
            // a partially-supported form still hints at its structure.
            UnsupportedBlockView(blockType: block.type)
        }
    }
}

// MARK: - Containers

struct EGContainerBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    let onSubmit: () -> Void
    @Environment(EGFormViewModel.self) private var viewModel
    @State private var collapsed: Bool

    init(block: EGBlock, dataPath: [String], readOnly: Bool, onSubmit: @escaping () -> Void = {}) {
        self.block = block
        self.dataPath = dataPath
        self.readOnly = readOnly
        self.onSubmit = onSubmit
        _collapsed = State(initialValue: block.content?.default_collapsed ?? false)
    }

    private var childPath: [String] {
        if let k = block.content?.key, !k.isEmpty { return dataPath + [k] }
        return dataPath
    }

    var body: some View {
        let display = block.content?.display ?? "none"
        let showHeader = display == "panel"
        let isWell = display == "well"
        let wellPalette = EGFormStyle.WellPalette.forVariant(block.content?.variant)
        let collapsible = showHeader && (block.content?.collapsible == true)
        let radius: CGFloat = showHeader ? EGFormStyle.panelRadiusLg : EGFormStyle.panelRadius

        VStack(alignment: .leading, spacing: 0) {
            if showHeader, let label = block.content?.label, !label.isEmpty {
                Button(action: {
                    // No withAnimation — animating the children mount/unmount
                    // triggers a full-form re-render every frame at the cost
                    // of every visible-expression and calc-field, which
                    // collapses framerate. Snap instead.
                    if collapsible { instantUpdate { collapsed.toggle() } }
                }) {
                    HStack {
                        Text(label)
                            .font(EGFormStyle.bodyBold)
                            .foregroundStyle(.primary)
                        Spacer()
                        if collapsible {
                            Image(systemName: collapsed ? "chevron.down" : "chevron.up")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, EGFormStyle.panelHdrX)
                    .padding(.vertical, EGFormStyle.panelHdrY)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(EGFormStyle.panelHdrBg)
                }
                .buttonStyle(.plain)
                .disabled(!collapsible)
                Divider()
            }

            if !collapsed {
                VStack(alignment: .leading, spacing: EGFormStyle.blockGap) {
                    if let children = block.content?.children, !children.isEmpty {
                        ForEach(Array(children.enumerated()), id: \.offset) { _, child in
                            EGBlockDispatcher(block: child, dataPath: childPath, readOnly: readOnly, onSubmit: onSubmit)
                        }
                    } else {
                        Text("Empty section")
                            .font(EGFormStyle.smallFont)
                            .italic()
                            .foregroundStyle(.tertiary)
                    }
                }
                .padding((showHeader || isWell) ? EGFormStyle.panelPad : 0)
            }
        }
        .background(isWell ? wellPalette.background : Color.clear)
        .overlay(
            Group {
                if showHeader {
                    RoundedRectangle(cornerRadius: radius).stroke(EGFormStyle.panelBorder, lineWidth: 1)
                } else if isWell {
                    RoundedRectangle(cornerRadius: radius).stroke(wellPalette.border, lineWidth: 1)
                }
            }
        )
        .clipShape(RoundedRectangle(cornerRadius: (showHeader || isWell) ? radius : 0))
    }
}

// MARK: - Columns

struct EGColumnsBlockView: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    let onSubmit: () -> Void
    @Environment(EGFormViewModel.self) private var viewModel
    @State private var collapsed: Bool

    init(block: EGBlock, dataPath: [String], readOnly: Bool, onSubmit: @escaping () -> Void = {}) {
        self.block = block
        self.dataPath = dataPath
        self.readOnly = readOnly
        self.onSubmit = onSubmit
        _collapsed = State(initialValue: block.content?.default_collapsed ?? false)
    }

    private var childPath: [String] {
        if let k = block.content?.key, !k.isEmpty { return dataPath + [k] }
        return dataPath
    }

    private var alignment: VerticalAlignment {
        switch block.content?.align {
        case "center": return .center
        case "bottom": return .bottom
        default: return .top
        }
    }

    var body: some View {
        let display = block.content?.display ?? "none"
        let showHeader = display == "panel"
        let isWell = display == "well"
        let wellPalette = EGFormStyle.WellPalette.forVariant(block.content?.variant)
        let collapsible = showHeader && (block.content?.collapsible == true)
        let radius: CGFloat = showHeader ? EGFormStyle.panelRadiusLg : EGFormStyle.panelRadius

        VStack(alignment: .leading, spacing: 0) {
            if showHeader, let label = block.content?.label, !label.isEmpty {
                Button(action: {
                    // No withAnimation — animating the children mount/unmount
                    // triggers a full-form re-render every frame at the cost
                    // of every visible-expression and calc-field, which
                    // collapses framerate. Snap instead.
                    if collapsible { instantUpdate { collapsed.toggle() } }
                }) {
                    HStack {
                        Text(label)
                            .font(EGFormStyle.bodyBold)
                            .foregroundStyle(.primary)
                        Spacer()
                        if collapsible {
                            Image(systemName: collapsed ? "chevron.down" : "chevron.up")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, EGFormStyle.panelHdrX)
                    .padding(.vertical, EGFormStyle.panelHdrY)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(EGFormStyle.panelHdrBg)
                }
                .buttonStyle(.plain)
                .disabled(!collapsible)
                Divider()
            }

            if !collapsed {
                HStack(alignment: alignment, spacing: EGFormStyle.blockGap) {
                    if let cols = block.content?.columns, !cols.isEmpty {
                        ForEach(Array(cols.enumerated()), id: \.offset) { _, col in
                            VStack(alignment: .leading, spacing: EGFormStyle.blockGap) {
                                ForEach(Array((col.children ?? []).enumerated()), id: \.offset) { _, child in
                                    EGBlockDispatcher(block: child, dataPath: childPath, readOnly: readOnly, onSubmit: onSubmit)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .topLeading)
                        }
                    } else {
                        Text("Empty columns")
                            .font(EGFormStyle.smallFont).italic()
                            .foregroundStyle(.tertiary)
                    }
                }
                .padding((showHeader || isWell) ? EGFormStyle.panelPad : 0)
            }
        }
        .background(isWell ? wellPalette.background : Color.clear)
        .overlay(
            Group {
                if showHeader {
                    RoundedRectangle(cornerRadius: radius).stroke(EGFormStyle.panelBorder, lineWidth: 1)
                } else if isWell {
                    RoundedRectangle(cornerRadius: radius).stroke(wellPalette.border, lineWidth: 1)
                }
            }
        )
        .clipShape(RoundedRectangle(cornerRadius: (showHeader || isWell) ? radius : 0))
    }
}

// MARK: - Display blocks

struct EGTextBlockView: View {
    let block: EGBlock
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let rendered = viewModel.renderTemplate(block.content?.html)
        Text(EGFormHTML.plain(rendered))
            .font(EGFormStyle.bodyFont)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct EGMessageBlockView: View {
    let block: EGBlock
    @Environment(EGFormViewModel.self) private var viewModel

    var body: some View {
        let rendered = viewModel.renderTemplate(block.content?.html)
        let palette = EGFormStyle.WellPalette.forVariant(block.content?.variant)
        Text(EGFormHTML.plain(rendered))
            .font(EGFormStyle.bodyFont)
            .foregroundStyle(palette.text)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EGFormStyle.panelPad)
            .background(palette.background)
            .overlay(
                RoundedRectangle(cornerRadius: EGFormStyle.panelRadius)
                    .stroke(palette.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: EGFormStyle.panelRadius))
    }
}

struct EGImageBlockView: View {
    let block: EGBlock

    var body: some View {
        if let src = block.content?.src, let url = URL(string: src) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFit()
                case .failure:
                    placeholder("Failed to load")
                case .empty:
                    placeholder("Loading…")
                @unknown default:
                    placeholder("")
                }
            }
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 4))
        } else {
            placeholder("Image placeholder")
        }
    }

    private func placeholder(_ text: String) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4).fill(Color(.systemGray6))
            Text(text).font(EGFormStyle.smallFont).foregroundStyle(.tertiary)
        }
        .frame(height: 120)
    }
}

struct EGDividerBlockView: View {
    var body: some View {
        Divider().padding(.vertical, 4)
    }
}

struct UnsupportedBlockView: View {
    let blockType: String
    var body: some View {
        HStack {
            Image(systemName: "questionmark.square.dashed")
            Text("Block type \"\(blockType)\" not yet supported on mobile")
                .font(EGFormStyle.smallFont)
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.yellow.opacity(0.1))
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(Color.yellow.opacity(0.5), lineWidth: 1)
        )
        .foregroundStyle(.secondary)
    }
}

