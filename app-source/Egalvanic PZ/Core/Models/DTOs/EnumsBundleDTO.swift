//
//  EnumsBundleDTO.swift
//  Egalvanic PZ
//
//  ZP-2161 — DTOs mirroring the ``GET /equipment-library/enums``
//  payload. Decoded with ``try?`` per-section so a single bad enum row
//  doesn't poison the whole bundle.
//
import Foundation

struct EnumsBundleDTO: Codable {
    var voltages: [EnumNodeVoltageDTO] = []
    var mains_types: [EnumNodeMainsTypeDTO] = []
    var phase_configurations: [EnumNodePhaseConfigurationDTO] = []
    var trip_types: [EnumNodeTripTypeDTO] = []
    var device_roles: [EnumDeviceRoleDTO] = []
    var manufacturers: [EnumSkmManufacturerDTO] = []
    var cable_sizes: [EnumCableSizeDTO] = []
    var cable_conductor_configurations: [EnumCableConductorConfigurationDTO] = []
    var cable_conductor_descriptions: [EnumCableSimpleDTO] = []
    var cable_duct_materials: [EnumCableDuctMaterialDTO] = []
    var cable_insulation_classes: [EnumCableSimpleDTO] = []
    var cable_insulation_types: [EnumCableSimpleDTO] = []
    var cable_installations: [EnumCableSimpleDTO] = []
    var busway_ampere_ratings: [EnumBuswayAmpereRatingDTO] = []

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        voltages = (try? c.decodeIfPresent([EnumNodeVoltageDTO].self, forKey: .voltages)) ?? []
        mains_types = (try? c.decodeIfPresent([EnumNodeMainsTypeDTO].self, forKey: .mains_types)) ?? []
        phase_configurations = (try? c.decodeIfPresent([EnumNodePhaseConfigurationDTO].self, forKey: .phase_configurations)) ?? []
        trip_types = (try? c.decodeIfPresent([EnumNodeTripTypeDTO].self, forKey: .trip_types)) ?? []
        device_roles = (try? c.decodeIfPresent([EnumDeviceRoleDTO].self, forKey: .device_roles)) ?? []
        manufacturers = (try? c.decodeIfPresent([EnumSkmManufacturerDTO].self, forKey: .manufacturers)) ?? []
        cable_sizes = (try? c.decodeIfPresent([EnumCableSizeDTO].self, forKey: .cable_sizes)) ?? []
        cable_conductor_configurations = (try? c.decodeIfPresent([EnumCableConductorConfigurationDTO].self, forKey: .cable_conductor_configurations)) ?? []
        cable_conductor_descriptions = (try? c.decodeIfPresent([EnumCableSimpleDTO].self, forKey: .cable_conductor_descriptions)) ?? []
        cable_duct_materials = (try? c.decodeIfPresent([EnumCableDuctMaterialDTO].self, forKey: .cable_duct_materials)) ?? []
        cable_insulation_classes = (try? c.decodeIfPresent([EnumCableSimpleDTO].self, forKey: .cable_insulation_classes)) ?? []
        cable_insulation_types = (try? c.decodeIfPresent([EnumCableSimpleDTO].self, forKey: .cable_insulation_types)) ?? []
        cable_installations = (try? c.decodeIfPresent([EnumCableSimpleDTO].self, forKey: .cable_installations)) ?? []
        busway_ampere_ratings = (try? c.decodeIfPresent([EnumBuswayAmpereRatingDTO].self, forKey: .busway_ampere_ratings)) ?? []
    }
}

struct EnumNodeVoltageDTO: Codable {
    var id: Int
    var label: String
    var value: Double
    var sort_order: Int?
}

struct EnumNodeMainsTypeDTO: Codable {
    var id: Int
    var label: String
    var description: String?
    var sort_order: Int?
}

struct EnumNodePhaseConfigurationDTO: Codable {
    var id: Int
    var label: String
    var description: String?
    var sort_order: Int?
    var topology: String?
    var l_to_n_divisor: Double?
    var max_pole_count: Int?
    var allowed_downstream_topology_ids: [Int]?
}

struct EnumNodeTripTypeDTO: Codable {
    var id: Int
    var slug: String
    var display_name: String
    var skm_dev_type: String
    var sort_order: Int?
    var is_active: Bool?
    var has_trip_unit: Bool?
}

struct EnumDeviceRoleDTO: Codable {
    var id: Int
    var code: String
    var label: String
    var sort_order: Int?
}

struct EnumSkmManufacturerDTO: Codable {
    var id: Int
    var name: String
}

struct EnumCableSizeDTO: Codable {
    var id: Int
    var name: String
    var is_awg: Bool
    var awg_value: Int?
    var kcmil_value: Int?
    var circular_mils: Int
    var sort_order: Int?
    var is_global: Bool?
    var is_deleted: Bool?
}

struct EnumCableConductorConfigurationDTO: Codable {
    var id: Int
    var name: String
    var conductor_count: Int
    var has_ground: Bool?
    var is_paralleled: Bool?
    var sort_order: Int?
    var is_global: Bool?
    var is_deleted: Bool?
}

struct EnumCableDuctMaterialDTO: Codable {
    var id: Int
    var name: String
    var description: String?
    var sort_order: Int?
    var is_global: Bool?
    var is_deleted: Bool?
    var is_busway: Bool?
}

/// Shared shape for ``cable_conductor_descriptions`` / ``cable_insulation_classes``
/// / ``cable_insulation_types`` / ``cable_installations`` — all four
/// share ``{id, name, sort_order, is_global, is_deleted, is_busway}``.
/// Caller knows which target SwiftData model to upsert into.
struct EnumCableSimpleDTO: Codable {
    var id: Int
    var name: String
    var sort_order: Int?
    var is_global: Bool?
    var is_deleted: Bool?
    var is_busway: Bool?
}

struct EnumBuswayAmpereRatingDTO: Codable {
    var id: Int
    var name: String
    var ampere_value: Int
    var sort_order: Int?
    var is_global: Bool?
    var is_deleted: Bool?
}
