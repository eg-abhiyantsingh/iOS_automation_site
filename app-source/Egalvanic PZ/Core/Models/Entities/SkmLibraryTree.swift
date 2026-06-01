//
//  SkmLibraryTree.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4b — deep SKM library tree. Mirrors the four tables
//  ``/equipment-library/skm-tree`` returns and complements the headers
//  from Phase 4a (``SkmDeviceHeader`` etc.).
//
//  Once these models are populated, the on-device matcher can replicate
//  ``/skm-library/match`` fully offline. The Engineering section uses
//  these tables for:
//
//    - voltage-floored frame lookup (``f_voltage >= node.voltage``)
//    - SST sensor + plug match (``sensor.f_size == n && plug ∈ sensor.plugs``)
//    - thermal-mag / fuse trip-unit match (``trip_unit.f_size == n``)
//    - settings-segment editor (LT pickup, ST delay, Inst, etc.)
//
//  No SwiftData relationships are declared — joins are done by the
//  matcher via ``device_oid`` / ``frame_id`` / ``trip_unit_oid``
//  lookups against dictionaries built once per query. Predicate-based
//  joins via relationship paths perform poorly at the row counts here
//  (~440k total).
//
import Foundation
import SwiftData

/// One frame per ``(device, voltage)`` tier. Multiple frames per
/// device — same physical breaker has different AIC ratings at 240 /
/// 480 / 600 V. ``f_size`` is the chassis ampere rating; ``f_voltage``
/// is what the matcher voltage-floor filters on.
@Model
final class SkmFrame {
    @Attribute(.unique) var id: Int
    var device_oid: Int
    var sid: Int
    var sz_name: String?
    var f_voltage: Double?
    var f_size: Double?
    var f_interrupting_rating: Double?
    var f_current_carry_amps: Double?

    init(id: Int, device_oid: Int, sid: Int = 0,
         sz_name: String? = nil, f_voltage: Double? = nil,
         f_size: Double? = nil, f_interrupting_rating: Double? = nil,
         f_current_carry_amps: Double? = nil) {
        self.id = id
        self.device_oid = device_oid
        self.sid = sid
        self.sz_name = sz_name
        self.f_voltage = f_voltage
        self.f_size = f_size
        self.f_interrupting_rating = f_interrupting_rating
        self.f_current_carry_amps = f_current_carry_amps
    }
}

/// Sensor (CT primary) attached to a frame. SST-style breakers match
/// on ``f_size`` (sensor amps) and the plug must be in ``plugs``.
@Model
final class SkmSensor {
    @Attribute(.unique) var id: Int
    var frame_id: Int
    var sid: Int
    var f_size: Double?
    var plugs: [Double] = []

    init(id: Int, frame_id: Int, sid: Int = 0,
         f_size: Double? = nil, plugs: [Double] = []) {
        self.id = id
        self.frame_id = frame_id
        self.sid = sid
        self.f_size = f_size
        self.plugs = plugs
    }
}

/// Trip unit — for thermal-mag breakers ``f_size`` is the chassis amp
/// tier (one trip unit per amp rating); for fuses it IS the amp
/// rating; for relays it's a placeholder.
@Model
final class SkmTripUnit {
    @Attribute(.unique) var id: Int  // ctripunitdata_oid on the BE
    var device_oid: Int
    var sid: Int
    var f_size: Double?
    var f_primary_current: Double?
    var primary_current_option: Int?
    var secondary_currents: [Double] = []

    init(id: Int, device_oid: Int, sid: Int = 0,
         f_size: Double? = nil, f_primary_current: Double? = nil,
         primary_current_option: Int? = nil,
         secondary_currents: [Double] = []) {
        self.id = id
        self.device_oid = device_oid
        self.sid = sid
        self.f_size = f_size
        self.f_primary_current = f_primary_current
        self.primary_current_option = primary_current_option
        self.secondary_currents = secondary_currents
    }
}

/// One eligible kVA entry for a transformer — drives the
/// ``TransformerConfigCard`` kVA picker. Each entry carries its own
/// (R%, X%) so picking a different kVA re-pins impedance. %Z is
/// derived at render time via √(R² + X²).
@Model
final class SkmTransformerKvaEntry {
    @Attribute(.unique) var id: Int
    var transformer_oid: Int
    var sid: Int
    var nominal_kva: Double?
    var percentage_r: Double?
    var percentage_x: Double?
    var percentage_r_zero: Double?
    var percentage_x_zero: Double?
    var inrush_factor: Double?

    init(id: Int, transformer_oid: Int, sid: Int = 0,
         nominal_kva: Double? = nil, percentage_r: Double? = nil,
         percentage_x: Double? = nil,
         percentage_r_zero: Double? = nil, percentage_x_zero: Double? = nil,
         inrush_factor: Double? = nil) {
        self.id = id
        self.transformer_oid = transformer_oid
        self.sid = sid
        self.nominal_kva = nominal_kva
        self.percentage_r = percentage_r
        self.percentage_x = percentage_x
        self.percentage_r_zero = percentage_r_zero
        self.percentage_x_zero = percentage_x_zero
        self.inrush_factor = inrush_factor
    }
}

/// ZP-2420 — one row from the SKM cable library. Each ``cable_oid``
/// (FK to the parent ``SkmCableAcHeader`` / ``SkmCableDcHeader`` /
/// ``SkmCableIeeeWHeader``) usually has many ``SkmCableEntry`` rows
/// representing different size / conductor-description / insulation
/// permutations of the same physical cable family. The matcher
/// filters these locally to feed ``MatchResultsPanel``.
///
/// Note: ``cable_size`` is a String here (e.g. "225", "4/0", "500"
/// kcmil) — the AWG/kcmil scale isn't pure-numeric so the BE stores
/// it as a string. The same table also holds busway distribution
/// rows (partitioned by ``duct_material`` ∈ {"Busway", "Bus"}); for
/// those rows ``cable_size`` carries an ampere-rating string
/// (e.g. "1200") instead.
@Model
final class SkmCableEntry {
    @Attribute(.unique) var id: Int
    var cable_oid: Int
    var sid: Int
    var sz_name: String?
    var conductor_type: String?
    var cable_size: String?
    var duct_material: String?
    var installation: String?
    var insulation_class: String?
    var insulation_type: String?
    var voltage_rating: Double?

    init(id: Int, cable_oid: Int, sid: Int = 0,
         sz_name: String? = nil, conductor_type: String? = nil,
         cable_size: String? = nil, duct_material: String? = nil,
         installation: String? = nil, insulation_class: String? = nil,
         insulation_type: String? = nil, voltage_rating: Double? = nil) {
        self.id = id
        self.cable_oid = cable_oid
        self.sid = sid
        self.sz_name = sz_name
        self.conductor_type = conductor_type
        self.cable_size = cable_size
        self.duct_material = duct_material
        self.installation = installation
        self.insulation_class = insulation_class
        self.insulation_type = insulation_type
        self.voltage_rating = voltage_rating
    }
}

/// One trip-unit settings axis (LT pickup, LT delay, ST pickup, etc.).
/// The editor renders a picker per segment with ``setting_values`` as
/// the eligible options and ``setting_labels`` as the display strings.
///
/// ZP-2243: ``dial_labels`` drives the CSEGMINSTBANDS compound dial
/// picker (e.g. INST OR with B-A / N-A / S-A dials). ``isqt_locked``
/// disables the CSEGMISQUARETDELAY i²t On/Off toggle when SKM's
/// nISQTLocked=1 (factory-locked segments).
@Model
final class SkmTuSegment {
    @Attribute(.unique) var id: Int
    var trip_unit_oid: Int
    var sid: Int
    var sz_name: String?
    var subtype: String  // "CSEGMINSTBANDS" / "CSEGMISQUARETDELAY" / etc.
    var setting_values: [Double] = []
    var setting_labels: [String] = []
    var function: String?  // canonical bucket key the segment belongs to
    var display_suffix: String?
    var init_on: Bool
    /// Dial-band labels for CSEGMINSTBANDS compound segments — the
    /// user picks one as the suffix; ``value`` stays "Fixed".
    var dial_labels: [String] = []
    /// True when SKM's nISQTLocked=1 — the i²t On/Off toggle is
    /// pinned at the library default and the user can't change it.
    var isqt_locked: Bool = false

    init(id: Int, trip_unit_oid: Int, sid: Int = 0,
         sz_name: String? = nil, subtype: String,
         setting_values: [Double] = [], setting_labels: [String] = [],
         function: String? = nil, display_suffix: String? = nil,
         init_on: Bool = false,
         dial_labels: [String] = [], isqt_locked: Bool = false) {
        self.id = id
        self.trip_unit_oid = trip_unit_oid
        self.sid = sid
        self.sz_name = sz_name
        self.subtype = subtype
        self.setting_values = setting_values
        self.setting_labels = setting_labels
        self.function = function
        self.display_suffix = display_suffix
        self.init_on = init_on
        self.dial_labels = dial_labels
        self.isqt_locked = isqt_locked
    }
}
