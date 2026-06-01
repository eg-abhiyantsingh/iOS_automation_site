//
//  SkmHeadersBundleDTO.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4a — mirrors the ``GET /equipment-library/skm-headers``
//  payload. Defensive per-section decoding so a bad row in one
//  section doesn't poison the bundle.
//
import Foundation

struct SkmHeadersBundleDTO: Codable {
    var dev_lib_routing: [EgDevLibRoutingDTO] = []
    var devices: [SkmDeviceHeaderDTO] = []
    var transformer_models: [SkmTransformerHeaderDTO] = []
    var cables_ac: [SkmCableHeaderDTO] = []
    var cables_dc: [SkmCableHeaderDTO] = []
    var cables_ieee_w: [SkmCableHeaderDTO] = []
    var bus_models: [SkmBusHeaderDTO] = []

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        dev_lib_routing = (try? c.decodeIfPresent([EgDevLibRoutingDTO].self, forKey: .dev_lib_routing)) ?? []
        devices = (try? c.decodeIfPresent([SkmDeviceHeaderDTO].self, forKey: .devices)) ?? []
        transformer_models = (try? c.decodeIfPresent([SkmTransformerHeaderDTO].self, forKey: .transformer_models)) ?? []
        cables_ac = (try? c.decodeIfPresent([SkmCableHeaderDTO].self, forKey: .cables_ac)) ?? []
        cables_dc = (try? c.decodeIfPresent([SkmCableHeaderDTO].self, forKey: .cables_dc)) ?? []
        cables_ieee_w = (try? c.decodeIfPresent([SkmCableHeaderDTO].self, forKey: .cables_ieee_w)) ?? []
        bus_models = (try? c.decodeIfPresent([SkmBusHeaderDTO].self, forKey: .bus_models)) ?? []
    }
}

struct EgDevLibRoutingDTO: Codable {
    var id: Int
    var eqp_lib_type_id: Int
    var eqp_lib_subtype_id: Int?
    var allowed_trip_type_ids: [Int]
    var sort_order: Int?
    var is_active: Bool?
}

struct SkmDeviceHeaderDTO: Codable {
    var id: Int
    var eqp_lib_type_id: Int?
    var eqp_lib_subtype_id: Int?
    var trip_type_id: Int?
    var manufacturer_id: Int?
    var sz_name: String?
    var sz_type: String?
    var sz_description: String?
    var sz_catalog: String?
    var sz_dev_type: String?
    var n_poles: Int?
    var f_max_voltage: Double?
}

struct SkmTransformerHeaderDTO: Codable {
    var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_type: String?
    var str_type_symbol: String?
    var str_description: String?
    var n_phase: Int?
}

struct SkmCableHeaderDTO: Codable {
    var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var str_description: String?
    var f_rated_voltage: Double?
}

struct SkmBusHeaderDTO: Codable {
    var id: Int
    var sz_name: String?
    var manufacturer_id: Int?
    var sz_type: String?
    var sz_description: String?
    var f_max_voltage: Double?
}
