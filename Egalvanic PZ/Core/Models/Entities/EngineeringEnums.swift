//
//  EngineeringEnums.swift
//  Egalvanic PZ
//
//  ZP-2161 — SwiftData mirrors of every enum table referenced by the
//  NodeV2 / NodeClass / NodeSubtype FK columns the Engineering section
//  needs to render. Synced via ``GET /equipment-library/enums`` in
//  ``SLDSyncService.smartUpsertEnumsBundle(...)``.
//
//  These are all global, system-managed tables (small row counts) —
//  one upsert per SLD refresh keeps them current.
//
import Foundation
import SwiftData

@Model
final class EnumNodeVoltage {
    @Attribute(.unique) var id: Int
    var label: String
    var value: Double
    var sort_order: Int

    init(id: Int, label: String, value: Double, sort_order: Int = 0) {
        self.id = id
        self.label = label
        self.value = value
        self.sort_order = sort_order
    }
}

@Model
final class EnumNodeMainsType {
    @Attribute(.unique) var id: Int
    var label: String
    var enum_description: String?
    var sort_order: Int

    init(id: Int, label: String, enum_description: String? = nil, sort_order: Int = 0) {
        self.id = id
        self.label = label
        self.enum_description = enum_description
        self.sort_order = sort_order
    }
}

@Model
final class EnumNodePhaseConfiguration {
    @Attribute(.unique) var id: Int
    var label: String
    var enum_description: String?
    var sort_order: Int
    var topology: String?
    var l_to_n_divisor: Double?
    var max_pole_count: Int?
    var allowed_downstream_topology_ids: [Int] = []

    init(id: Int, label: String, enum_description: String? = nil, sort_order: Int = 0,
         topology: String? = nil, l_to_n_divisor: Double? = nil,
         max_pole_count: Int? = nil,
         allowed_downstream_topology_ids: [Int] = []) {
        self.id = id
        self.label = label
        self.enum_description = enum_description
        self.sort_order = sort_order
        self.topology = topology
        self.l_to_n_divisor = l_to_n_divisor
        self.max_pole_count = max_pole_count
        self.allowed_downstream_topology_ids = allowed_downstream_topology_ids
    }
}

@Model
final class EnumNodeTripType {
    @Attribute(.unique) var id: Int
    var slug: String
    var display_name: String
    var skm_dev_type: String
    var sort_order: Int
    var is_active: Bool
    var has_trip_unit: Bool

    init(id: Int, slug: String, display_name: String, skm_dev_type: String,
         sort_order: Int = 0, is_active: Bool = true, has_trip_unit: Bool = false) {
        self.id = id
        self.slug = slug
        self.display_name = display_name
        self.skm_dev_type = skm_dev_type
        self.sort_order = sort_order
        self.is_active = is_active
        self.has_trip_unit = has_trip_unit
    }
}

@Model
final class EnumDeviceRole {
    @Attribute(.unique) var id: Int
    var code: String
    var label: String
    var sort_order: Int

    init(id: Int, code: String, label: String, sort_order: Int = 0) {
        self.id = id
        self.code = code
        self.label = label
        self.sort_order = sort_order
    }
}

/// Mirror of the ``enum_skm_manufacturers`` table — large (~100+ rows)
/// but flat. Mobile only renders the ``name`` column.
@Model
final class EnumSkmManufacturer {
    @Attribute(.unique) var id: Int
    var name: String

    init(id: Int, name: String) {
        self.id = id
        self.name = name
    }
}

@Model
final class EnumCableSize {
    @Attribute(.unique) var id: Int
    var name: String
    var is_awg: Bool
    var awg_value: Int?
    var kcmil_value: Int?
    var circular_mils: Int
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool

    init(id: Int, name: String, is_awg: Bool, awg_value: Int? = nil, kcmil_value: Int? = nil,
         circular_mils: Int, sort_order: Int = 0, is_global: Bool = true, is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.is_awg = is_awg
        self.awg_value = awg_value
        self.kcmil_value = kcmil_value
        self.circular_mils = circular_mils
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
    }
}

@Model
final class EnumCableConductorConfiguration {
    @Attribute(.unique) var id: Int
    var name: String
    var conductor_count: Int
    var has_ground: Bool
    var is_paralleled: Bool
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool

    init(id: Int, name: String, conductor_count: Int, has_ground: Bool = false,
         is_paralleled: Bool = false, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.conductor_count = conductor_count
        self.has_ground = has_ground
        self.is_paralleled = is_paralleled
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
    }
}

/// Shared base shape for the simple cable + busway enums whose payload
/// is ``{id, name, sort_order, is_global, is_deleted, is_busway}``.
@Model
final class EnumCableConductorDescription {
    @Attribute(.unique) var id: Int
    var name: String
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool
    var is_busway: Bool

    init(id: Int, name: String, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false, is_busway: Bool = false) {
        self.id = id
        self.name = name
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.is_busway = is_busway
    }
}

@Model
final class EnumCableDuctMaterial {
    @Attribute(.unique) var id: Int
    var name: String
    var enum_description: String?
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool
    var is_busway: Bool

    init(id: Int, name: String, enum_description: String? = nil, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false, is_busway: Bool = false) {
        self.id = id
        self.name = name
        self.enum_description = enum_description
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.is_busway = is_busway
    }
}

@Model
final class EnumCableInsulationClass {
    @Attribute(.unique) var id: Int
    var name: String
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool
    var is_busway: Bool

    init(id: Int, name: String, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false, is_busway: Bool = false) {
        self.id = id
        self.name = name
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.is_busway = is_busway
    }
}

@Model
final class EnumCableInsulationType {
    @Attribute(.unique) var id: Int
    var name: String
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool
    var is_busway: Bool

    init(id: Int, name: String, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false, is_busway: Bool = false) {
        self.id = id
        self.name = name
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.is_busway = is_busway
    }
}

@Model
final class EnumCableInstallation {
    @Attribute(.unique) var id: Int
    var name: String
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool
    var is_busway: Bool

    init(id: Int, name: String, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false, is_busway: Bool = false) {
        self.id = id
        self.name = name
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
        self.is_busway = is_busway
    }
}

@Model
final class EnumBuswayAmpereRating {
    @Attribute(.unique) var id: Int
    var name: String
    var ampere_value: Int
    var sort_order: Int
    var is_global: Bool
    var is_deleted: Bool

    init(id: Int, name: String, ampere_value: Int, sort_order: Int = 0,
         is_global: Bool = true, is_deleted: Bool = false) {
        self.id = id
        self.name = name
        self.ampere_value = ampere_value
        self.sort_order = sort_order
        self.is_global = is_global
        self.is_deleted = is_deleted
    }
}
