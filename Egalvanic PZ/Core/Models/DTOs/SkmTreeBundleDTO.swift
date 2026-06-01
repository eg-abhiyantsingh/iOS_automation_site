//
//  SkmTreeBundleDTO.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4b — mirrors ``GET /equipment-library/skm-tree``.
//  Defensive per-section decoding so a malformed row in one table
//  doesn't poison the entire bundle (~440k rows total).
//
import Foundation

struct SkmTreeBundleDTO: Codable {
    var frames: [SkmFrameDTO] = []
    var sensors: [SkmSensorDTO] = []
    var trip_units: [SkmTripUnitDTO] = []
    var segments: [SkmTuSegmentDTO] = []
    var transformer_kva_entries: [SkmTransformerKvaEntryDTO] = []
    // ZP-2420: cable + busway deep entries (one row per
    // size/conductor/insulation/installation permutation per parent
    // library header). ``transformer_str_types`` is a flat list of
    // all known transformer str_type strings — currently unused by
    // the matcher but decoded defensively for forward-compat.
    var cable_entries: [SkmCableEntryDTO] = []

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        frames = (try? c.decodeIfPresent([SkmFrameDTO].self, forKey: .frames)) ?? []
        sensors = (try? c.decodeIfPresent([SkmSensorDTO].self, forKey: .sensors)) ?? []
        trip_units = (try? c.decodeIfPresent([SkmTripUnitDTO].self, forKey: .trip_units)) ?? []
        segments = (try? c.decodeIfPresent([SkmTuSegmentDTO].self, forKey: .segments)) ?? []
        transformer_kva_entries = (try? c.decodeIfPresent([SkmTransformerKvaEntryDTO].self, forKey: .transformer_kva_entries)) ?? []
        cable_entries = (try? c.decodeIfPresent([SkmCableEntryDTO].self, forKey: .cable_entries)) ?? []
        // ZP-2420 PR review #2: ``busway_entries`` and
        // ``transformer_str_types`` are intentionally not decoded.
        // The BE returns them, but the matcher uses neither:
        //   - busway distribution data actually lives in
        //     ``cable_entries`` partitioned by ``duct_material``
        //     ∈ {Busway, Bus}; the separately-named ``busway_entries``
        //     table is MV switchgear data unused by asset matching.
        //   - ``transformer_str_types`` is a flat reference list
        //     not currently used by any picker on iOS.
    }
}

/// ZP-2420 — one cable / busway library row. The same table holds
/// both: busway distribution rows are identified by
/// ``duct_material`` ∈ {"Busway", "Bus"}; everything else is cable.
/// ``cable_size`` is intentionally String — AWG / kcmil cable sizes
/// aren't pure-numeric (e.g. "4/0"), and busway rows reuse the same
/// column to carry ampere-rating strings (e.g. "600").
struct SkmCableEntryDTO: Codable {
    var id: Int
    var cable_oid: Int
    var sid: Int?
    var sz_name: String?
    var conductor_type: String?
    var cable_size: String?
    var duct_material: String?
    var installation: String?
    var insulation_class: String?
    var insulation_type: String?
    var voltage_rating: Double?
}

struct SkmTransformerKvaEntryDTO: Codable {
    var id: Int
    var transformer_oid: Int
    var sid: Int?
    var nominal_kva: Double?
    var percentage_r: Double?
    var percentage_x: Double?
    var percentage_r_zero: Double?
    var percentage_x_zero: Double?
    var inrush_factor: Double?
}

struct SkmFrameDTO: Codable {
    var id: Int
    var device_oid: Int
    var sid: Int?
    var sz_name: String?
    var f_voltage: Double?
    var f_size: Double?
    var f_interrupting_rating: Double?
    var f_current_carry_amps: Double?
}

struct SkmSensorDTO: Codable {
    var id: Int
    var frame_id: Int
    var sid: Int?
    var f_size: Double?
    var plugs: [Double]?
}

struct SkmTripUnitDTO: Codable {
    var id: Int
    var device_oid: Int
    var sid: Int?
    var f_size: Double?
    var f_primary_current: Double?
    var primary_current_option: Int?
    var secondary_currents: [Double]?
}

struct SkmTuSegmentDTO: Codable {
    var id: Int
    var trip_unit_oid: Int
    var sid: Int?
    var sz_name: String?
    var subtype: String
    var setting_values: [Double]?
    var setting_labels: [String]?
    var function: String?
    var display_suffix: String?
    var init_on: Bool?
    /// ZP-2243: dial labels for CSEGMINSTBANDS compound segments.
    var dial_labels: [String]?
    /// ZP-2243: true when SKM's nISQTLocked=1 — the i²t toggle on a
    /// CSEGMISQUARETDELAY segment is pinned at the factory default.
    var isqt_locked: Bool?

    enum CodingKeys: String, CodingKey {
        case id, trip_unit_oid, sid, sz_name, subtype, setting_values,
             setting_labels, function, display_suffix, init_on,
             dial_labels, isqt_locked
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        trip_unit_oid = try c.decode(Int.self, forKey: .trip_unit_oid)
        sid = try c.decodeIfPresent(Int.self, forKey: .sid)
        sz_name = try c.decodeIfPresent(String.self, forKey: .sz_name)
        subtype = try c.decode(String.self, forKey: .subtype)
        setting_values = try c.decodeIfPresent([Double].self, forKey: .setting_values)
        setting_labels = try c.decodeIfPresent([String].self, forKey: .setting_labels)
        function = try c.decodeIfPresent(String.self, forKey: .function)
        display_suffix = try c.decodeIfPresent(String.self, forKey: .display_suffix)
        init_on = try c.decodeIfPresent(Bool.self, forKey: .init_on)
        // ZP-2243: SKM's POET ARRAY storage pads dial_labels with
        // NULLs to a fixed size. Decode as ``[String?]`` then strip
        // nulls so the rest of the app sees a clean ``[String]``.
        let rawDials = try c.decodeIfPresent([String?].self, forKey: .dial_labels)
        dial_labels = rawDials?.compactMap { $0 }
        isqt_locked = try c.decodeIfPresent(Bool.self, forKey: .isqt_locked)
    }
}
