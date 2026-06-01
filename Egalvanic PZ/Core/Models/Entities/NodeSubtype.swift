//
//  NodeSubtype.swift
//  Egalvanic PZ
//
import Foundation
import SwiftData

@Model
final class NodeSubtype {
    var id: UUID
    var name: String
    var node_class_id: UUID
    var is_global: Bool = false
    var is_deleted: Bool = false
    var company_id: UUID?
    var nodeClass: NodeClass?

    // ZP-2161 — new fields. All optional / defaulted; the existing
    // init() signature is unchanged.
    var eqp_lib_subtype_id: Int? = nil   // FK -> eg_eqp_lib_subtypes.id (mccb, iccb, hv-fuse, dry, etc.)
    var volt_floor: Double? = nil        // hide subtype when node voltage < volt_floor
    var volt_ceiling: Double? = nil      // hide subtype when node voltage > volt_ceiling
    var amp_floor: Double? = nil
    var amp_ceiling: Double? = nil
    var subtype_description: String? = nil  // avoid NSObject.description clash
    var voltage_level: String? = nil
    var replacement_cost: Double? = nil
    var is_override: Bool? = nil
    var for_entity: UUID? = nil

    init(id: UUID, name: String, node_class_id: UUID, is_global: Bool = false, is_deleted: Bool = false, company_id: UUID? = nil, nodeClass: NodeClass? = nil) {
        self.id = id
        self.name = name
        self.node_class_id = node_class_id
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.company_id = company_id
        self.nodeClass = nodeClass
    }
}
