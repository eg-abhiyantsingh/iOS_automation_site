//
//  EqpLibraryDTO.swift
//  Egalvanic PZ
//
//  DTOs for the equipment library bulk download response.
//

import Foundation

struct EqpLibraryBulkResponse: Codable {
    let success: Bool
    var categories: [EqpCategoryDTO]
    var sensors: [EqpSensorDTO]
    var plugs: [EqpPlugDTO]
    var lt_pickups: [EqpLtPickupDTO]
    var lt_delays: [EqpLtDelayDTO]
    var st_pickups: [EqpStPickupDTO]
    var st_delays: [EqpStDelayDTO]
    var inst_pickups: [EqpInstPickupDTO]
    var curve_types: [EqpCurveTypeDTO]
    var frames: [EqpFrameDTO]
    var frame_amps: [EqpFrameAmpDTO]
    var frame_settings: [EqpFrameSettingDTO]

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        success = try container.decode(Bool.self, forKey: .success)
        categories = try container.decode([EqpCategoryDTO].self, forKey: .categories)
        sensors = try container.decode([EqpSensorDTO].self, forKey: .sensors)
        plugs = try container.decode([EqpPlugDTO].self, forKey: .plugs)
        lt_pickups = try container.decode([EqpLtPickupDTO].self, forKey: .lt_pickups)
        lt_delays = try container.decode([EqpLtDelayDTO].self, forKey: .lt_delays)
        st_pickups = try container.decode([EqpStPickupDTO].self, forKey: .st_pickups)
        st_delays = try container.decode([EqpStDelayDTO].self, forKey: .st_delays)
        inst_pickups = try container.decode([EqpInstPickupDTO].self, forKey: .inst_pickups)
        curve_types = try container.decode([EqpCurveTypeDTO].self, forKey: .curve_types)
        frames = (try? container.decode([EqpFrameDTO].self, forKey: .frames)) ?? []
        frame_amps = (try? container.decode([EqpFrameAmpDTO].self, forKey: .frame_amps)) ?? []
        frame_settings = (try? container.decode([EqpFrameSettingDTO].self, forKey: .frame_settings)) ?? []
    }
}

struct EqpCategoryDTO: Codable {
    let slug: String
    let items: [EqpItemDTO]
}

struct EqpItemDTO: Codable {
    let id: Int
    let manufacturer: String?
    let type: String?
    let cstandard: String?
    let acdc: String?
    let style: String?  // lv-trip-units, emt
    let styles: [EqpStyleDTO]?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        manufacturer = try? container.decode(String.self, forKey: .manufacturer)
        type = try? container.decode(String.self, forKey: .type)
        cstandard = try? container.decode(String.self, forKey: .cstandard)
        acdc = try? container.decode(String.self, forKey: .acdc)
        style = try? container.decode(String.self, forKey: .style)
        styles = try? container.decode([EqpStyleDTO].self, forKey: .styles)
    }
}

struct EqpStyleDTO: Codable {
    let id: Int
    let style: String?
    let r_cont_current: Double?
    let r_nom_mva: Double?
    let r_nom_kv: Double?
    let r_max_kv: Double?
    let r_sym_ka: Double?
    let framesize: String?
    let framedesc: String?
    let tmt_use_sst: Int?
    let tmt_sst_mfr: String?
    let tmt_sst_type: String?
    let tmt_sst_style: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        style = try? container.decode(String.self, forKey: .style)
        r_cont_current = try? container.decode(Double.self, forKey: .r_cont_current)
        r_nom_mva = try? container.decode(Double.self, forKey: .r_nom_mva)
        r_nom_kv = try? container.decode(Double.self, forKey: .r_nom_kv)
        r_max_kv = try? container.decode(Double.self, forKey: .r_max_kv)
        r_sym_ka = try? container.decode(Double.self, forKey: .r_sym_ka)
        framesize = try? container.decode(String.self, forKey: .framesize)
        framedesc = try? container.decode(String.self, forKey: .framedesc)
        tmt_use_sst = try? container.decode(Int.self, forKey: .tmt_use_sst)
        tmt_sst_mfr = try? container.decode(String.self, forKey: .tmt_sst_mfr)
        tmt_sst_type = try? container.decode(String.self, forKey: .tmt_sst_type)
        tmt_sst_style = try? container.decode(String.self, forKey: .tmt_sst_style)
    }
}

struct EqpSensorDTO: Codable {
    let sensorid: Int
    let styleid: Int
    let sensordesc: String?
    let slope: Double?
    let ds3_pickup_calc: Int?
    let ds4_pickup_calc: Int?
    let sec1_name: String?
    let sec2_name: String?
    let sec3_name: String?
    let sec4_name: String?
    let idelay_opening: Double?
    let idelay_clearing: Double?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        styleid = try container.decode(Int.self, forKey: .styleid)
        sensordesc = try? container.decode(String.self, forKey: .sensordesc)
        slope = try? container.decode(Double.self, forKey: .slope)
        ds3_pickup_calc = try? container.decode(Int.self, forKey: .ds3_pickup_calc)
        ds4_pickup_calc = try? container.decode(Int.self, forKey: .ds4_pickup_calc)
        sec1_name = try? container.decode(String.self, forKey: .sec1_name)
        sec2_name = try? container.decode(String.self, forKey: .sec2_name)
        sec3_name = try? container.decode(String.self, forKey: .sec3_name)
        sec4_name = try? container.decode(String.self, forKey: .sec4_name)
        idelay_opening = try? container.decode(Double.self, forKey: .idelay_opening)
        idelay_clearing = try? container.decode(Double.self, forKey: .idelay_clearing)
    }
}

struct EqpPlugDTO: Codable {
    let sensorid: Int
    let plugval: Double
}

struct EqpLtPickupDTO: Codable {
    let sensorid: Int
    let setting: Double
}

struct EqpLtDelayDTO: Codable {
    let sensorid: Int
    let desc: String?
    let setting: Double
    let curveid: Int

    enum CodingKeys: String, CodingKey {
        case sensorid, desc, setting, curveid
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        desc = try? container.decode(String.self, forKey: .desc)
        setting = try container.decode(Double.self, forKey: .setting)
        curveid = try container.decode(Int.self, forKey: .curveid)
    }
}

struct EqpStPickupDTO: Codable {
    let sensorid: Int
    let desc: String?
    let setting: Double

    enum CodingKeys: String, CodingKey {
        case sensorid, desc, setting
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        desc = try? container.decode(String.self, forKey: .desc)
        setting = try container.decode(Double.self, forKey: .setting)
    }
}

struct EqpStDelayDTO: Codable {
    let sensorid: Int
    let desc: String?
    let min_open: Double?
    let min_clear: Double?
    let i2x: Int?

    enum CodingKeys: String, CodingKey {
        case sensorid, desc, min_open, min_clear, i2x
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        desc = try? container.decode(String.self, forKey: .desc)
        min_open = try? container.decode(Double.self, forKey: .min_open)
        min_clear = try? container.decode(Double.self, forKey: .min_clear)
        i2x = try? container.decode(Int.self, forKey: .i2x)
    }
}

struct EqpInstPickupDTO: Codable {
    let sensorid: Int
    let desc: String?
    let setting: Double

    enum CodingKeys: String, CodingKey {
        case sensorid, desc, setting
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        desc = try? container.decode(String.self, forKey: .desc)
        setting = try container.decode(Double.self, forKey: .setting)
    }
}

struct EqpCurveTypeDTO: Codable {
    let sensorid: Int
    let curveid: Int
    let name: String?
    let slope: Double?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        sensorid = try container.decode(Int.self, forKey: .sensorid)
        curveid = try container.decode(Int.self, forKey: .curveid)
        name = try? container.decode(String.self, forKey: .name)
        slope = try? container.decode(Double.self, forKey: .slope)
    }
}

struct EqpFrameDTO: Codable {
    let id: Int
    let styleid: Int
    let framesize: Int?
    let framedesc: String?
    let sec1name: String?
    let sec2name: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        styleid = try container.decode(Int.self, forKey: .styleid)
        framesize = try? container.decode(Int.self, forKey: .framesize)
        framedesc = try? container.decode(String.self, forKey: .framedesc)
        sec1name = try? container.decode(String.self, forKey: .sec1name)
        sec2name = try? container.decode(String.self, forKey: .sec2name)
    }
}

struct EqpFrameAmpDTO: Codable {
    let framesizeid: Int
    let tripamp: Int?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        framesizeid = try container.decode(Int.self, forKey: .framesizeid)
        tripamp = try? container.decode(Int.self, forKey: .tripamp)
    }
}

struct EqpFrameSettingDTO: Codable {
    let framesizeid: Int
    let fsetting: Double?
    let sdesc: String?
    let flow: Double?
    let fhigh: Double?

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        framesizeid = try container.decode(Int.self, forKey: .framesizeid)
        fsetting = try? container.decode(Double.self, forKey: .fsetting)
        sdesc = try? container.decode(String.self, forKey: .sdesc)
        flow = try? container.decode(Double.self, forKey: .flow)
        fhigh = try? container.decode(Double.self, forKey: .fhigh)
    }
}
