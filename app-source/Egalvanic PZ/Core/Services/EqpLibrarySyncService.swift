//
//  EqpLibrarySyncService.swift
//  Egalvanic PZ
//
//  Standalone service for downloading and caching the equipment library.
//  Called from Settings — decoupled from SLD sync.
//

import Foundation
import SwiftData

class EqpLibrarySyncService {

    /// Category slug → human-readable display name
    static let categoryNames: [String: String] = [
        "mccb-breakers": "MCCB Breakers",
        "hv-breakers": "HV Breakers",
        "iccb-breakers": "ICCB Breakers",
        "pcb-breakers": "PCB Breakers",
        "lv-trip-units": "LV Trip Units",
        "hv-fuses": "HV Fuses",
        "lv-fuses": "LV Fuses",
        "relays": "Relays",
        "switches": "Switches",
        "emt": "EMT",
    ]

    /// Downloads the full equipment library from the server and caches it in SwiftData.
    /// Runs on a background thread to keep the UI responsive.
    /// Clears all existing cached data before inserting fresh data.
    static func downloadAndCache(
        modelContainer: ModelContainer,
        onProgress: (@Sendable (String, Double) -> Void)? = nil
    ) async throws -> (itemCount: Int, styleCount: Int, sensorCount: Int) {
        // Dispatch progress updates to the main actor for UI
        func reportProgress(_ label: String, _ value: Double) async {
            await MainActor.run { onProgress?(label, value) }
        }

        await reportProgress("Downloading library...", 0.0)
        var bulk = try await APIClient.shared.fetchEquipmentLibraryBulk()
        await reportProgress("Clearing old data...", 0.05)

        // Create a background ModelContext (off the main thread)
        let modelContext = ModelContext(modelContainer)

        // Clear existing cache with bulk delete + flush before inserting
        try modelContext.delete(model: EqpFrameSetting.self)
        try modelContext.delete(model: EqpFrameAmp.self)
        try modelContext.delete(model: EqpFrame.self)
        try modelContext.delete(model: EqpCurveType.self)
        try modelContext.delete(model: EqpInstPickup.self)
        try modelContext.delete(model: EqpStDelay.self)
        try modelContext.delete(model: EqpStPickup.self)
        try modelContext.delete(model: EqpLtDelay.self)
        try modelContext.delete(model: EqpLtPickup.self)
        try modelContext.delete(model: EqpPlug.self)
        try modelContext.delete(model: EqpSensor.self)
        try modelContext.delete(model: EqpStyle.self)
        try modelContext.delete(model: EqpItem.self)
        try modelContext.delete(model: EqpCategory.self)
        try modelContext.save()
        print("‼️ EqpLib cleared old cache")
        await reportProgress("Inserting equipment items...", 0.1)

        var totalItems = 0
        var totalStyles = 0

        // Batch-save helper: saves every N inserts to bound memory usage
        var insertCount = 0
        let batchSize = 5000
        func batchInsert(_ object: any PersistentModel) throws {
            modelContext.insert(object)
            insertCount += 1
            if insertCount % batchSize == 0 {
                try modelContext.save()
            }
        }

        // Insert categories, items, and styles (using batchInsert for memory safety)
        for (catIndex, catDTO) in bulk.categories.enumerated() {
            let catProgress = 0.1 + (Double(catIndex) / Double(bulk.categories.count)) * 0.3
            await reportProgress("Inserting \(EqpLibrarySyncService.categoryNames[catDTO.slug] ?? catDTO.slug)...", catProgress)
            let category = EqpCategory(
                id: catDTO.slug,
                name: categoryNames[catDTO.slug] ?? catDTO.slug
            )
            try batchInsert(category)

            for itemDTO in catDTO.items {
                let item = EqpItem(
                    id: itemDTO.id,
                    categorySlug: catDTO.slug,
                    manufacturer: itemDTO.manufacturer ?? "",
                    type: itemDTO.type ?? "",
                    cstandard: itemDTO.cstandard,
                    acdc: itemDTO.acdc,
                    style: itemDTO.style
                )
                try batchInsert(item)
                totalItems += 1

                for styleDTO in (itemDTO.styles ?? []) {
                    let eqpStyle = EqpStyle(
                        id: styleDTO.id,
                        itemId: itemDTO.id,
                        categorySlug: catDTO.slug,
                        style: styleDTO.style ?? "",
                        rContCurrent: styleDTO.r_cont_current,
                        rNomMva: styleDTO.r_nom_mva,
                        rNomKv: styleDTO.r_nom_kv,
                        rMaxKv: styleDTO.r_max_kv,
                        rSymKa: styleDTO.r_sym_ka,
                        framesize: styleDTO.framesize,
                        framedesc: styleDTO.framedesc,
                        tmtUseSst: styleDTO.tmt_use_sst,
                        tmtSstMfr: styleDTO.tmt_sst_mfr,
                        tmtSstType: styleDTO.tmt_sst_type,
                        tmtSstStyle: styleDTO.tmt_sst_style
                    )
                    try batchInsert(eqpStyle)
                    totalStyles += 1
                }
            }
        }
        // Release category/item/style DTOs to free memory
        bulk.categories = []

        // Insert sensors
        await reportProgress("Inserting sensors (\(bulk.sensors.count))...", 0.4)
        print("‼️ EqpLib inserting \(bulk.sensors.count) sensors...")
        for dto in bulk.sensors {
            try batchInsert(EqpSensor(
                sensorid: dto.sensorid,
                styleid: dto.styleid,
                sensordesc: dto.sensordesc ?? "",
                slope: dto.slope,
                ds3_pickup_calc: dto.ds3_pickup_calc,
                ds4_pickup_calc: dto.ds4_pickup_calc,
                sec1_name: dto.sec1_name,
                sec2_name: dto.sec2_name,
                sec3_name: dto.sec3_name,
                sec4_name: dto.sec4_name,
                idelay_opening: dto.idelay_opening,
                idelay_clearing: dto.idelay_clearing
            ))
        }
        let sensorCount = bulk.sensors.count
        bulk.sensors = []

        // Insert plugs
        await reportProgress("Inserting plugs (\(bulk.plugs.count))...", 0.5)
        print("‼️ EqpLib inserting \(bulk.plugs.count) plugs...")
        for dto in bulk.plugs {
            try batchInsert(EqpPlug(sensorid: dto.sensorid, plugval: dto.plugval))
        }
        bulk.plugs = []

        // Insert LT pickups
        await reportProgress("Inserting pickup/delay settings...", 0.6)
        print("‼️ EqpLib inserting \(bulk.lt_pickups.count) lt_pickups...")
        for dto in bulk.lt_pickups {
            try batchInsert(EqpLtPickup(sensorid: dto.sensorid, setting: dto.setting))
        }
        bulk.lt_pickups = []

        // Insert LT delays
        print("‼️ EqpLib inserting \(bulk.lt_delays.count) lt_delays...")
        for dto in bulk.lt_delays {
            try batchInsert(EqpLtDelay(
                sensorid: dto.sensorid,
                desc: dto.desc ?? "",
                setting: dto.setting,
                curveid: dto.curveid
            ))
        }
        bulk.lt_delays = []

        // Insert ST pickups
        print("‼️ EqpLib inserting \(bulk.st_pickups.count) st_pickups...")
        for dto in bulk.st_pickups {
            try batchInsert(EqpStPickup(
                sensorid: dto.sensorid,
                desc: dto.desc ?? "",
                setting: dto.setting
            ))
        }
        bulk.st_pickups = []

        // Insert ST delays
        await reportProgress("Inserting delay settings...", 0.75)
        print("‼️ EqpLib inserting \(bulk.st_delays.count) st_delays...")
        for dto in bulk.st_delays {
            try batchInsert(EqpStDelay(
                sensorid: dto.sensorid,
                desc: dto.desc ?? "",
                min_open: dto.min_open,
                min_clear: dto.min_clear,
                i2x: dto.i2x
            ))
        }
        bulk.st_delays = []

        // Insert inst pickups
        print("‼️ EqpLib inserting \(bulk.inst_pickups.count) inst_pickups...")
        for dto in bulk.inst_pickups {
            try batchInsert(EqpInstPickup(
                sensorid: dto.sensorid,
                desc: dto.desc ?? "",
                setting: dto.setting
            ))
        }
        bulk.inst_pickups = []

        // Insert curve types
        await reportProgress("Inserting curve types...", 0.85)
        print("‼️ EqpLib inserting \(bulk.curve_types.count) curve_types...")
        for dto in bulk.curve_types {
            try batchInsert(EqpCurveType(
                sensorid: dto.sensorid,
                curveid: dto.curveid,
                name: dto.name ?? "",
                slope: dto.slope
            ))
        }
        bulk.curve_types = []

        // Insert TMT frames
        await reportProgress("Inserting frame data...", 0.9)
        print("‼️ EqpLib inserting \(bulk.frames.count) frames...")
        for dto in bulk.frames {
            try batchInsert(EqpFrame(
                frameid: dto.id,
                styleid: dto.styleid,
                framesize: dto.framesize,
                framedesc: dto.framedesc,
                sec1name: dto.sec1name,
                sec2name: dto.sec2name
            ))
        }
        bulk.frames = []

        // Insert TMT frame amps
        print("‼️ EqpLib inserting \(bulk.frame_amps.count) frame_amps...")
        for dto in bulk.frame_amps {
            try batchInsert(EqpFrameAmp(
                frameid: dto.framesizeid,
                tripamp: dto.tripamp
            ))
        }
        bulk.frame_amps = []

        // Insert TMT frame settings
        print("‼️ EqpLib inserting \(bulk.frame_settings.count) frame_settings...")
        for dto in bulk.frame_settings {
            try batchInsert(EqpFrameSetting(
                frameid: dto.framesizeid,
                fsetting: dto.fsetting,
                sdesc: dto.sdesc,
                flow: dto.flow,
                fhigh: dto.fhigh
            ))
        }
        bulk.frame_settings = []

        // Final save for remaining items
        await reportProgress("Saving to device...", 0.95)
        try modelContext.save()
        print("‼️ EqpLib all inserts complete (\(insertCount) total rows)")

        // Verify what actually persisted
        let mccbCount = (try? modelContext.fetchCount(FetchDescriptor<EqpItem>(predicate: #Predicate { $0.categorySlug == "mccb-breakers" }))) ?? -1
        let totalItemCount = (try? modelContext.fetchCount(FetchDescriptor<EqpItem>())) ?? -1
        let spectraCheck = try? modelContext.fetch(FetchDescriptor<EqpItem>(predicate: #Predicate { $0.type == "Spectra" && $0.categorySlug == "mccb-breakers" }))
        print("‼️ EqpLib VERIFY: total items=\(totalItemCount), mccb=\(mccbCount), spectra=\(spectraCheck?.count ?? -1)")
        if let s = spectraCheck?.first {
            print("‼️ EqpLib SPECTRA: id=\(s.id) mfr=\(s.manufacturer) type=\(s.type) slug=\(s.categorySlug)")
        }

        await reportProgress("Complete!", 1.0)

        // Persist download timestamp
        UserDefaults.standard.set(Date(), forKey: "eqpLibraryLastDownloaded")

        AppLogger.log(.info, "Equipment library cached: \(totalItems) items, \(totalStyles) styles, \(sensorCount) sensors", category: .sync)

        return (itemCount: totalItems, styleCount: totalStyles, sensorCount: sensorCount)
    }

    /// Returns the date the library was last downloaded, or nil if never downloaded.
    static var lastDownloadDate: Date? {
        UserDefaults.standard.object(forKey: "eqpLibraryLastDownloaded") as? Date
    }

    /// Returns the count of cached equipment items.
    @MainActor
    static func cachedItemCount(modelContext: ModelContext) -> Int {
        let descriptor = FetchDescriptor<EqpItem>()
        return (try? modelContext.fetchCount(descriptor)) ?? 0
    }
}
