//
//  SkmLibrarySyncService.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4b — settings-triggered download of the SKM device
//  deep tree (frames + sensors + trip_units + segments). Mirrors
//  ``EqpLibrarySyncService`` for the EasyPower library:
//
//    - Runs on a background ``ModelContext`` so the main thread / UI
//      stays responsive (the per-SLD upsertAllData path is on
//      ``@MainActor`` and would freeze the editor for ~10s+ on the
//      initial 440k-row load).
//    - Delete-and-recreate rather than update-or-insert. The library
//      is read-only from the app's perspective — there's nothing to
//      preserve, and skipping the per-row fetch + dict-lookup + dirty
//      tracking is the main speedup.
//    - Batched ``save()`` every 5000 rows to bound memory.
//    - Progress callback drives the Settings card's ProgressView.
//    - ``lastDownloadDate`` persisted in UserDefaults so the Settings
//      caption can show "Last updated 3h ago".
//
import Foundation
import SwiftData

class SkmLibrarySyncService {

    /// Downloads the full SKM deep tree and caches it in SwiftData.
    /// Background-threaded; clears the four tree tables before
    /// inserting fresh data. Returns counts for the Settings status
    /// caption.
    static func downloadAndCache(
        modelContainer: ModelContainer,
        onProgress: (@Sendable (String, Double) -> Void)? = nil
    ) async throws -> (frames: Int, sensors: Int, tripUnits: Int, segments: Int, kvaEntries: Int, cableEntries: Int) {
        func reportProgress(_ label: String, _ value: Double) async {
            await MainActor.run { onProgress?(label, value) }
        }

        await reportProgress("Downloading device library…", 0.0)
        var bundle = try await APIClient.shared.fetchSkmTreeBundle()
        let frameCount = bundle.frames.count
        let sensorCount = bundle.sensors.count
        let tripUnitCount = bundle.trip_units.count
        let segmentCount = bundle.segments.count
        let kvaEntryCount = bundle.transformer_kva_entries.count
        let cableEntryCount = bundle.cable_entries.count

        await reportProgress("Clearing old data…", 0.05)

        // Background ModelContext — off the main actor.
        let modelContext = ModelContext(modelContainer)

        // Wipe existing rows. Order matters because of FK chains:
        // segments → trip_units, sensors → frames, frames → devices
        // (headers, which we do NOT delete — they're owned by the
        // Phase 4a headers sync). ZP-2420: cable_entries store both
        // cable AND busway distribution data (partitioned in-matcher
        // by duct_material).
        try modelContext.delete(model: SkmTuSegment.self)
        try modelContext.delete(model: SkmTripUnit.self)
        try modelContext.delete(model: SkmSensor.self)
        try modelContext.delete(model: SkmFrame.self)
        try modelContext.delete(model: SkmTransformerKvaEntry.self)
        try modelContext.delete(model: SkmCableEntry.self)
        try modelContext.save()

        // Batch-insert helper. Saves every ``batchSize`` rows to bound
        // memory; final save() at the end of the run flushes the
        // remainder.
        var insertCount = 0
        let batchSize = 5000
        func batchInsert(_ object: any PersistentModel) throws {
            modelContext.insert(object)
            insertCount += 1
            if insertCount % batchSize == 0 {
                try modelContext.save()
            }
        }

        // ── Frames ────────────────────────────────────────────────
        await reportProgress("Inserting \(frameCount.formatted()) frames…", 0.10)
        for dto in bundle.frames {
            try batchInsert(SkmFrame(
                id: dto.id,
                device_oid: dto.device_oid,
                sid: dto.sid ?? 0,
                sz_name: dto.sz_name,
                f_voltage: dto.f_voltage,
                f_size: dto.f_size,
                f_interrupting_rating: dto.f_interrupting_rating,
                f_current_carry_amps: dto.f_current_carry_amps
            ))
        }
        bundle.frames = []

        // ── Sensors ───────────────────────────────────────────────
        await reportProgress("Inserting \(sensorCount.formatted()) sensors…", 0.40)
        for dto in bundle.sensors {
            try batchInsert(SkmSensor(
                id: dto.id,
                frame_id: dto.frame_id,
                sid: dto.sid ?? 0,
                f_size: dto.f_size,
                plugs: dto.plugs ?? []
            ))
        }
        bundle.sensors = []

        // ── Trip units ────────────────────────────────────────────
        await reportProgress("Inserting \(tripUnitCount.formatted()) trip units…", 0.70)
        for dto in bundle.trip_units {
            try batchInsert(SkmTripUnit(
                id: dto.id,
                device_oid: dto.device_oid,
                sid: dto.sid ?? 0,
                f_size: dto.f_size,
                f_primary_current: dto.f_primary_current,
                primary_current_option: dto.primary_current_option,
                secondary_currents: dto.secondary_currents ?? []
            ))
        }
        bundle.trip_units = []

        // ── Settings segments ─────────────────────────────────────
        await reportProgress("Inserting \(segmentCount.formatted()) settings…", 0.85)
        for dto in bundle.segments {
            try batchInsert(SkmTuSegment(
                id: dto.id,
                trip_unit_oid: dto.trip_unit_oid,
                sid: dto.sid ?? 0,
                sz_name: dto.sz_name,
                subtype: dto.subtype,
                setting_values: dto.setting_values ?? [],
                setting_labels: dto.setting_labels ?? [],
                function: dto.function,
                display_suffix: dto.display_suffix,
                init_on: dto.init_on ?? false,
                dial_labels: dto.dial_labels ?? [],
                isqt_locked: dto.isqt_locked ?? false
            ))
        }
        bundle.segments = []

        // ── Transformer kVA entries ───────────────────────────────
        await reportProgress("Inserting \(kvaEntryCount.formatted()) kVA entries…", 0.88)
        for dto in bundle.transformer_kva_entries {
            try batchInsert(SkmTransformerKvaEntry(
                id: dto.id,
                transformer_oid: dto.transformer_oid,
                sid: dto.sid ?? 0,
                nominal_kva: dto.nominal_kva,
                percentage_r: dto.percentage_r,
                percentage_x: dto.percentage_x,
                percentage_r_zero: dto.percentage_r_zero,
                percentage_x_zero: dto.percentage_x_zero,
                inrush_factor: dto.inrush_factor
            ))
        }
        bundle.transformer_kva_entries = []

        // ── Cable + busway entries (ZP-2420) ──────────────────────
        // Single table holds both kinds; matcher partitions in-Swift
        // via ``duct_material``.
        await reportProgress("Inserting \(cableEntryCount.formatted()) cable / busway entries…", 0.93)
        for dto in bundle.cable_entries {
            try batchInsert(SkmCableEntry(
                id: dto.id,
                cable_oid: dto.cable_oid,
                sid: dto.sid ?? 0,
                sz_name: dto.sz_name,
                conductor_type: dto.conductor_type,
                cable_size: dto.cable_size,
                duct_material: dto.duct_material,
                installation: dto.installation,
                insulation_class: dto.insulation_class,
                insulation_type: dto.insulation_type,
                voltage_rating: dto.voltage_rating
            ))
        }
        bundle.cable_entries = []

        // Final flush for the trailing < batchSize rows.
        await reportProgress("Saving to device…", 0.97)
        try modelContext.save()

        await reportProgress("Complete", 1.0)

        UserDefaults.standard.set(Date(), forKey: "skmLibraryLastDownloaded")

        AppLogger.log(.info,
            "SKM library cached: \(frameCount) frames, \(sensorCount) sensors, \(tripUnitCount) trip units, \(segmentCount) segments, \(kvaEntryCount) kVA entries, \(cableEntryCount) cable / busway entries",
            category: .sync)

        return (frames: frameCount, sensors: sensorCount,
                tripUnits: tripUnitCount, segments: segmentCount,
                kvaEntries: kvaEntryCount, cableEntries: cableEntryCount)
    }

    /// Date of the last successful download, or nil if never downloaded.
    static var lastDownloadDate: Date? {
        UserDefaults.standard.object(forKey: "skmLibraryLastDownloaded") as? Date
    }

    /// Quick "is the library loaded?" check for gating engineering UI.
    @MainActor
    static func cachedFrameCount(modelContext: ModelContext) -> Int {
        (try? modelContext.fetchCount(FetchDescriptor<SkmFrame>())) ?? 0
    }
}
