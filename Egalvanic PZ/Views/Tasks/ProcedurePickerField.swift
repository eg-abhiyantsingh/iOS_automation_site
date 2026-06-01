//
//  ProcedurePickerField.swift
//  Egalvanic PZ
//
//  Sheet-style picker backed by the local Procedure cache. Mirrors the
//  web client behavior: results are grouped by node_subtype (with a
//  "General" bucket at the bottom) and filtered by the currently-selected
//  node's class — General is always included as a fallback.
//
//  Stores the selected procedure's UUID (or nil) into field-value
//  storage so SimpleTaskCreationConfiguration can pass it straight
//  through to the UserTask + DTO.
//
import SwiftUI
import SwiftData

struct ProcedurePickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage
    @Query(filter: #Predicate<Procedure> { !$0.is_deleted }) private var allProcedures: [Procedure]

    @State private var showSheet = false
    @State private var searchText = ""

    /// `field.metadata["nodeFieldId"]` (default "node") tells the picker
    /// which storage key holds the currently-picked node. When the user
    /// changes the node, we re-filter the procedure list automatically.
    /// `field.metadata["fixedNode"]` takes precedence — used when the
    /// creation flow was opened from an asset detail and there is no
    /// node picker on the form.
    private var nodeFieldId: String {
        (field.metadata["nodeFieldId"] as? String) ?? "node"
    }

    /// Storage keys for the title + description auto-fill. Overridable
    /// via metadata so this picker isn't married to specific field ids.
    private var titleFieldId: String {
        (field.metadata["titleFieldId"] as? String) ?? "title"
    }
    private var descriptionFieldId: String {
        (field.metadata["descriptionFieldId"] as? String) ?? "description"
    }

    /// Private bookkeeping keys for the "did the user edit this since we
    /// auto-filled it?" check. We overwrite only when the current value
    /// is empty or still matches the last value we generated.
    private var lastAutoTitleKey: String { "__procedure_autoTitle_\(field.id)" }
    private var lastAutoDescKey: String { "__procedure_autoDesc_\(field.id)" }

    private var selectedNode: NodeV2? {
        if let fixed = field.metadata["fixedNode"] as? NodeV2 {
            return fixed
        }
        // The unified creation flow uses a multi-picker stored as
        // [NodeV2]; the simple flow stores a single NodeV2. Accept both
        // and use the first node to derive the class filter.
        let value = storage.getValue(for: nodeFieldId)
        if let single = value as? NodeV2 { return single }
        if let multi = value as? [NodeV2] { return multi.first }
        return nil
    }

    private var selectedProcedureId: UUID? {
        storage.getValue(for: field.id) as? UUID
    }

    /// Number of assets currently linked to the task being created.
    /// Matches the web's `linked_nodes.length`. A fixed node counts as 1.
    private var linkedNodeCount: Int {
        if field.metadata["fixedNode"] is NodeV2 { return 1 }
        let value = storage.getValue(for: nodeFieldId)
        if value is NodeV2 { return 1 }
        if let multi = value as? [NodeV2] { return multi.count }
        return 0
    }

    /// Procedure can't be picked once 2+ assets are linked — a single
    /// procedure can't apply across multiple assets. Mirrors the web
    /// (`linked_nodes.length > 1`). The existing value is left intact.
    private var isDisabled: Bool {
        linkedNodeCount > 1
    }

    /// Mirrors the web's `generateTaskTitleAndDescription`: writes a
    /// `"<procedure> on <node>"` title and falls back to the procedure
    /// description (or a "Perform X on Y" template). We only overwrite
    /// fields the user hasn't manually edited — i.e. the current value
    /// is either empty or still matches what we wrote last time.
    private func autoFillTitleAndDescription(procedure: Procedure, node: NodeV2?) {
        let procName: String = {
            let trimmed = procedure.name.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty { return trimmed }
            let master = procedure.procedure_master_name?.trimmingCharacters(in: .whitespacesAndNewlines)
            // Last-resort fallback for a nameless procedure — use the
            // generic "Task" noun, not a field label.
            return (master?.isEmpty == false ? master! : AppStrings.Tasks.task)
        }()
        let nodeLabel = node?.label

        let newTitle: String = {
            if let nodeLabel { return "\(procName) on \(nodeLabel)" }
            return procName
        }()
        let newDesc: String = {
            let raw = procedure.procedure_description?.trimmingCharacters(in: .whitespacesAndNewlines)
            if let raw, !raw.isEmpty { return raw }
            if let nodeLabel { return "Perform \(procName) on \(nodeLabel)" }
            return "Perform \(procName)"
        }()

        let currentTitle = (storage.getValue(for: titleFieldId) as? String) ?? ""
        let lastAutoTitle = (storage.getValue(for: lastAutoTitleKey) as? String) ?? ""
        if currentTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || currentTitle == lastAutoTitle {
            storage.setValue(newTitle, for: titleFieldId)
            storage.setValue(newTitle, for: lastAutoTitleKey)
        }

        let currentDesc = (storage.getValue(for: descriptionFieldId) as? String) ?? ""
        let lastAutoDesc = (storage.getValue(for: lastAutoDescKey) as? String) ?? ""
        if currentDesc.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || currentDesc == lastAutoDesc {
            storage.setValue(newDesc, for: descriptionFieldId)
            storage.setValue(newDesc, for: lastAutoDescKey)
        }
    }

    /// Resolve a procedure id to the cached Procedure row, used by both
    /// the tap-to-pick handler and the node-change re-fire path.
    private func procedure(for id: UUID) -> Procedure? {
        allProcedures.first { $0.id == id }
    }

    private var selectedProcedure: Procedure? {
        guard let id = selectedProcedureId else { return nil }
        return allProcedures.first { $0.id == id }
    }

    /// Filter procedures by the picked node's class. Procedures with no
    /// node_class_id (General bucket) are always included so the user
    /// can still attach an SLD-wide procedure.
    private var availableProcedures: [Procedure] {
        guard let nodeClassId = selectedNode?.node_class?.id else {
            // No node picked → only General procedures, matching the web
            // client behavior when the asset selector is empty.
            return allProcedures.filter { $0.node_class_id == nil }
        }
        return allProcedures.filter { proc in
            proc.node_class_id == nil || proc.node_class_id == nodeClassId
        }
    }

    private var filteredProcedures: [Procedure] {
        guard !searchText.isEmpty else { return availableProcedures }
        let query = searchText.lowercased()
        return availableProcedures.filter { proc in
            proc.name.lowercased().contains(query) ||
            (proc.procedure_description?.lowercased().contains(query) ?? false) ||
            (proc.node_subtype?.lowercased().contains(query) ?? false)
        }
    }

    /// Groups by `node_subtype`, sorting buckets alphabetically with
    /// "General" pinned to the bottom — matches the web client.
    private var groupedProcedures: [(group: String, procedures: [Procedure])] {
        let generalLabel = AppStrings.Tasks.procedureGroupGeneral
        let buckets = Dictionary(grouping: filteredProcedures) { proc -> String in
            let raw = proc.node_subtype?.trimmingCharacters(in: .whitespacesAndNewlines)
            return (raw?.isEmpty == false ? raw! : generalLabel)
        }
        return buckets
            .map { (group: $0.key, procedures: $0.value.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }) }
            .sorted { lhs, rhs in
                if lhs.group == generalLabel { return false }
                if rhs.group == generalLabel { return true }
                return lhs.group.localizedCaseInsensitiveCompare(rhs.group) == .orderedAscending
            }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                if let icon = field.icon {
                    Image(systemName: icon)
                        .foregroundColor(.secondary)
                        .frame(width: 20)
                }

                Text(selectedProcedure?.name ?? AppStrings.Tasks.selectProcedure)
                    .foregroundColor(selectedProcedure != nil ? .primary : .secondary)

                Spacer()

                // Hide the clear affordance while disabled — the field is
                // non-interactive when 2+ assets are linked.
                if selectedProcedure != nil && !isDisabled {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                        .font(.body)
                        .frame(width: 30, height: 30)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            storage.setValue(nil as UUID?, for: field.id)
                        }
                }

                Image(systemName: "chevron.down")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(.systemGray6))
            .cornerRadius(8)
            .opacity(isDisabled ? 0.5 : 1)
            .contentShape(Rectangle())
            .onTapGesture {
                // No-op when disabled (2+ assets) — mirrors the web's
                // disabled procedure dropdown.
                guard !isDisabled else { return }
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                showSheet = true
            }

            if isDisabled {
                Text(AppStrings.Tasks.procedureDisabledMultipleAssets)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        // Re-run the auto-fill when the user picks a different asset
        // while a procedure is already selected — matches the web's
        // handleNodeChange → generateTaskTitleAndDescription path.
        .onChange(of: selectedNode?.id) { _, _ in
            if let id = selectedProcedureId, let proc = procedure(for: id) {
                autoFillTitleAndDescription(procedure: proc, node: selectedNode)
            }
        }
        .sheet(isPresented: $showSheet) {
            NavigationStack {
                VStack(spacing: 0) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField(AppStrings.Tasks.searchProcedures, text: $searchText)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)

                    if availableProcedures.isEmpty {
                        Spacer()
                        Text(AppStrings.Tasks.noProceduresAvailable)
                            .foregroundColor(.secondary)
                            .padding()
                        Spacer()
                    } else {
                        List {
                            // "No procedure" row — explicit way to clear
                            // an existing selection without dismissing
                            // the sheet first.
                            Button {
                                storage.setValue(nil as UUID?, for: field.id)
                                showSheet = false
                            } label: {
                                HStack {
                                    Text(AppStrings.Tasks.noProcedure)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if selectedProcedureId == nil {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }

                            ForEach(groupedProcedures, id: \.group) { group in
                                Section {
                                    ForEach(group.procedures) { proc in
                                        Button {
                                            storage.setValue(proc.id, for: field.id)
                                            autoFillTitleAndDescription(procedure: proc, node: selectedNode)
                                            showSheet = false
                                        } label: {
                                            HStack {
                                                Text(proc.name)
                                                    .foregroundColor(.primary)
                                                Spacer()
                                                if selectedProcedureId == proc.id {
                                                    Image(systemName: "checkmark")
                                                        .foregroundColor(.blue)
                                                }
                                            }
                                        }
                                    }
                                } header: {
                                    Text(group.group)
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                }
                            }
                        }
                        .listStyle(.insetGrouped)
                    }
                }
                .navigationTitle(field.label)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(AppStrings.Common.cancel) {
                            searchText = ""
                            showSheet = false
                        }
                    }
                }
            }
            .presentationDetents([.large])
        }
    }
}
