//
//  SkmLibraryHeaders.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4a — lightweight "discovery" mirror of the SKM
//  library tables needed to drive the Engineering section's filters
//  offline. Headers only; the deep tree (frames / sensors /
//  trip_units / segments / kVA entries) ships in Phase 4b when the
//  EasyPower library gets fully replaced.
//
//  Synced via ``GET /equipment-library/skm-headers`` through
//  ``SLDSyncService.smartUpsertSkmHeadersBundle(...)``.
//
import Foundation
import SwiftData

/// (eqp_lib_type_id, eqp_lib_subtype_id) → allowed trip-type ids.
/// Drives the "Type filtered by subtype" picker in the Engineering
/// section.
@Model
final class EgDevLibRouting {
    @Attribute(.unique) var id: Int
    var eqp_lib_type_id: Int
    var eqp_lib_subtype_id: Int?  // nil = "any subtype under this type"
    var allowed_trip_type_ids: [Int] = []
    var sort_order: Int
    var is_active: Bool

    init(id: Int, eqp_lib_type_id: Int, eqp_lib_subtype_id: Int? = nil,
         allowed_trip_type_ids: [Int] = [], sort_order: Int = 0, is_active: Bool = true) {
        self.id = id
        self.eqp_lib_type_id = eqp_lib_type_id
        self.eqp_lib_subtype_id = eqp_lib_subtype_id
        self.allowed_trip_type_ids = allowed_trip_type_ids
        self.sort_order = sort_order
        self.is_active = is_active
    }
}

/// SKM device "header" — the columns the Engineering section needs
/// to filter manufacturer + type pickers. Deep device detail (frames
/// / sensors / trip units) lives in Phase 4b's full SKM models.
@Model
final class SkmDeviceHeader {
    @Attribute(.unique) var id: Int
    var eqp_lib_type_id: Int?
    var eqp_lib_subtype_id: Int?
    var trip_type_id: Int?
    var manufacturer_id: Int?
    var sz_name: String?
    /// ZP-2161 Phase 4b: matcher card label + search axis.
    var sz_type: String?
    /// Matcher card sub-label + search axis.
    var sz_description: String?
    var sz_catalog: String?
    /// SKM-internal device class slug (SST / TM / MCP / HV / etc.) —
    /// drives the match card's category badge.
    var sz_dev_type: String?
    var n_poles: Int?
    var f_max_voltage: Double?

    init(id: Int, eqp_lib_type_id: Int? = nil, eqp_lib_subtype_id: Int? = nil,
         trip_type_id: Int? = nil, manufacturer_id: Int? = nil,
         sz_name: String? = nil, sz_type: String? = nil,
         sz_description: String? = nil, sz_catalog: String? = nil,
         sz_dev_type: String? = nil,
         n_poles: Int? = nil, f_max_voltage: Double? = nil) {
        self.id = id
        self.eqp_lib_type_id = eqp_lib_type_id
        self.eqp_lib_subtype_id = eqp_lib_subtype_id
        self.trip_type_id = trip_type_id
        self.manufacturer_id = manufacturer_id
        self.sz_name = sz_name
        self.sz_type = sz_type
        self.sz_description = sz_description
        self.sz_catalog = sz_catalog
        self.sz_dev_type = sz_dev_type
        self.n_poles = n_poles
        self.f_max_voltage = f_max_voltage
    }
}

/// Transformer model header — full data is small enough to sync in
/// full but we surface just the picker-relevant columns.
@Model
final class SkmTransformerModelHeader {
    @Attribute(.unique) var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_type: String?
    var str_type_symbol: String?
    var str_description: String?
    var n_phase: Int?

    init(id: Int, sz_name: String? = nil, manufacturer_id: Int? = nil,
         str_type: String? = nil, str_type_symbol: String? = nil,
         str_description: String? = nil, n_phase: Int? = nil) {
        self.id = id
        self.sz_name = sz_name
        self.manufacturer_id = manufacturer_id
        self.str_type = str_type
        self.str_type_symbol = str_type_symbol
        self.str_description = str_description
        self.n_phase = n_phase
    }
}

@Model
final class SkmCableAcHeader {
    @Attribute(.unique) var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_description: String?
    var f_rated_voltage: Double?

    init(id: Int, sz_name: String? = nil, manufacturer_id: Int? = nil,
         str_description: String? = nil, f_rated_voltage: Double? = nil) {
        self.id = id
        self.sz_name = sz_name
        self.manufacturer_id = manufacturer_id
        self.str_description = str_description
        self.f_rated_voltage = f_rated_voltage
    }
}

@Model
final class SkmCableDcHeader {
    @Attribute(.unique) var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_description: String?
    var f_rated_voltage: Double?

    init(id: Int, sz_name: String? = nil, manufacturer_id: Int? = nil,
         str_description: String? = nil, f_rated_voltage: Double? = nil) {
        self.id = id
        self.sz_name = sz_name
        self.manufacturer_id = manufacturer_id
        self.str_description = str_description
        self.f_rated_voltage = f_rated_voltage
    }
}

@Model
final class SkmCableIeeeWHeader {
    @Attribute(.unique) var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_description: String?
    var f_rated_voltage: Double?

    init(id: Int, sz_name: String? = nil, manufacturer_id: Int? = nil,
         str_description: String? = nil, f_rated_voltage: Double? = nil) {
        self.id = id
        self.sz_name = sz_name
        self.manufacturer_id = manufacturer_id
        self.str_description = str_description
        self.f_rated_voltage = f_rated_voltage
    }
}

@Model
final class SkmBusModelHeader {
    @Attribute(.unique) var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var sz_type: String?
    var sz_description: String?
    var f_max_voltage: Double?

    init(id: Int, sz_name: String? = nil, manufacturer_id: Int? = nil,
         sz_type: String? = nil, sz_description: String? = nil, f_max_voltage: Double? = nil) {
        self.id = id
        self.sz_name = sz_name
        self.manufacturer_id = manufacturer_id
        self.sz_type = sz_type
        self.sz_description = sz_description
        self.f_max_voltage = f_max_voltage
    }
}
