//
//  EngineeringDraft.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 3b — bundles every editable engineering field into a
//  single struct so ``EditNodeDetailView`` only carries one @State
//  property instead of 20+. The struct is passed as ``Binding`` to
//  ``NodeEngineeringSection`` which threads sub-bindings (``$draft.kva_rating``)
//  to each editable control.
//
//  Save flow: ``EngineeringDraft.applyTo(node:)`` writes the draft back
//  onto NodeV2, then ``NodeService.updateNode`` syncs the changes.
//
import Foundation

struct EngineeringDraft: Equatable {
    // ── Voltage ────────────────────────────────────────────────────
    // Primary / Secondary live in the existing draftVoltage* state on
    // EditNodeDetailView; we only need tertiary here.
    var tertiary_voltage: Double?
    var tertiary_voltage_id: Int?

    // ── Transformer ────────────────────────────────────────────────
    var kva_rating: Double?
    var percent_impedance: Double?

    // ── Box / panel ────────────────────────────────────────────────
    var mains_type_id: Int?
    var phase_configuration_id: Int?

    // ── OCP ────────────────────────────────────────────────────────
    var ampere_rating: Int?
    var pole_count: Int?
    var manufacturer_id: Int?
    var has_trip_unit: Bool?
    var trip_type_id: Int?
    var frame_amps: Int?
    var sensor_amps: Int?
    var plug_amps: Int?

    // ── Cable / Busway ─────────────────────────────────────────────
    var length: Double?
    var conductor_material: String?
    var cable_size_id: Int?
    var conductor_configuration_id: Int?
    var conductor_description_id: Int?
    var duct_material_id: Int?
    var insulation_class_id: Int?
    var insulation_type_id: Int?
    var installation_id: Int?
    var busway_ampere_rating: Int?

    init(from node: NodeV2) {
        tertiary_voltage = node.tertiary_voltage
        tertiary_voltage_id = node.tertiary_voltage_id
        kva_rating = node.kva_rating
        percent_impedance = node.percent_impedance
        mains_type_id = node.mains_type_id
        phase_configuration_id = node.phase_configuration_id
        ampere_rating = node.ampere_rating
        pole_count = node.pole_count
        manufacturer_id = node.manufacturer_id
        has_trip_unit = node.has_trip_unit
        trip_type_id = node.trip_type_id
        frame_amps = node.frame_amps
        sensor_amps = node.sensor_amps
        plug_amps = node.plug_amps
        length = node.length
        conductor_material = node.conductor_material
        cable_size_id = node.cable_size_id
        conductor_configuration_id = node.conductor_configuration_id
        conductor_description_id = node.conductor_description_id
        duct_material_id = node.duct_material_id
        insulation_class_id = node.insulation_class_id
        insulation_type_id = node.insulation_type_id
        installation_id = node.installation_id
        busway_ampere_rating = node.busway_ampere_rating
    }

    /// Empty draft with every engineering field nil. Used to reset
    /// class-specific scalars when the asset class changes so a value
    /// entered for one class can't silently survive into a class that
    /// wouldn't surface that field (see ZP-2368).
    init() {
        tertiary_voltage = nil
        tertiary_voltage_id = nil
        kva_rating = nil
        percent_impedance = nil
        mains_type_id = nil
        phase_configuration_id = nil
        ampere_rating = nil
        pole_count = nil
        manufacturer_id = nil
        has_trip_unit = nil
        trip_type_id = nil
        frame_amps = nil
        sensor_amps = nil
        plug_amps = nil
        length = nil
        conductor_material = nil
        cable_size_id = nil
        conductor_configuration_id = nil
        conductor_description_id = nil
        duct_material_id = nil
        insulation_class_id = nil
        insulation_type_id = nil
        installation_id = nil
        busway_ampere_rating = nil
    }

    /// Apply the draft values back to a NodeV2 — used by the save
    /// path before calling ``NodeService.updateNode``.
    func applyTo(node: NodeV2) {
        node.tertiary_voltage = tertiary_voltage
        node.tertiary_voltage_id = tertiary_voltage_id
        node.kva_rating = kva_rating
        node.percent_impedance = percent_impedance
        node.mains_type_id = mains_type_id
        node.phase_configuration_id = phase_configuration_id
        node.ampere_rating = ampere_rating
        node.pole_count = pole_count
        node.manufacturer_id = manufacturer_id
        node.has_trip_unit = has_trip_unit
        node.trip_type_id = trip_type_id
        node.frame_amps = frame_amps
        node.sensor_amps = sensor_amps
        node.plug_amps = plug_amps
        node.length = length
        node.conductor_material = conductor_material
        node.cable_size_id = cable_size_id
        node.conductor_configuration_id = conductor_configuration_id
        node.conductor_description_id = conductor_description_id
        node.duct_material_id = duct_material_id
        node.insulation_class_id = insulation_class_id
        node.insulation_type_id = insulation_type_id
        node.installation_id = installation_id
        node.busway_ampere_rating = busway_ampere_rating
    }

    /// True when any field differs from the node's stored value —
    /// used by the dirty-check that drives the Save button.
    func isDirty(against node: NodeV2) -> Bool {
        EngineeringDraft(from: node) != self
    }
}
