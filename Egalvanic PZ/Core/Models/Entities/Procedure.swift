//
//  Procedure.swift
//  Egalvanic PZ
//
//  Local cache of /lookup/procedures used by the task creation flow.
//  Synced as part of the full refresh in SLDSyncService.upsertAllData
//  so the picker keeps working offline.
//
import Foundation
import SwiftData

@Model
final class Procedure {
    @Attribute(.unique) var id: UUID
    var name: String
    var procedure_description: String?
    var procedure_master_name: String?
    var form_id: UUID?
    var use_proxy: Bool
    var node_class_id: UUID?
    var node_subtype: String?
    var node_subtype_id: UUID?
    var is_deleted: Bool

    init(
        id: UUID,
        name: String,
        procedure_description: String? = nil,
        procedure_master_name: String? = nil,
        form_id: UUID? = nil,
        use_proxy: Bool = false,
        node_class_id: UUID? = nil,
        node_subtype: String? = nil,
        node_subtype_id: UUID? = nil,
        is_deleted: Bool = false
    ) {
        self.id = id
        self.name = name
        self.procedure_description = procedure_description
        self.procedure_master_name = procedure_master_name
        self.form_id = form_id
        self.use_proxy = use_proxy
        self.node_class_id = node_class_id
        self.node_subtype = node_subtype
        self.node_subtype_id = node_subtype_id
        self.is_deleted = is_deleted
    }
}
