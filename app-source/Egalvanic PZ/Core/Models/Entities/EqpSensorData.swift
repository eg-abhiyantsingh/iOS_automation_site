//
//  EqpSensorData.swift
//  Egalvanic PZ
//
//  Trip unit settings cache models for offline TCC curve configuration.
//

import Foundation
import SwiftData

@Model
final class EqpSensor {
    #Index<EqpSensor>([\.styleid])

    @Attribute(.unique) var sensorid: Int
    var styleid: Int
    var sensordesc: String
    var slope: Double?
    var ds3_pickup_calc: Int?
    var ds4_pickup_calc: Int?
    var sec1_name: String?
    var sec2_name: String?
    var sec3_name: String?
    var sec4_name: String?
    var idelay_opening: Double?
    var idelay_clearing: Double?

    init(sensorid: Int, styleid: Int, sensordesc: String, slope: Double? = nil,
         ds3_pickup_calc: Int? = nil, ds4_pickup_calc: Int? = nil,
         sec1_name: String? = nil, sec2_name: String? = nil,
         sec3_name: String? = nil, sec4_name: String? = nil,
         idelay_opening: Double? = nil, idelay_clearing: Double? = nil) {
        self.sensorid = sensorid
        self.styleid = styleid
        self.sensordesc = sensordesc
        self.slope = slope
        self.ds3_pickup_calc = ds3_pickup_calc
        self.ds4_pickup_calc = ds4_pickup_calc
        self.sec1_name = sec1_name
        self.sec2_name = sec2_name
        self.sec3_name = sec3_name
        self.sec4_name = sec4_name
        self.idelay_opening = idelay_opening
        self.idelay_clearing = idelay_clearing
    }
}

/// Composite key: sensorid + plugval
@Model
final class EqpPlug {
    #Index<EqpPlug>([\.sensorid])

    var sensorid: Int
    var plugval: Double

    init(sensorid: Int, plugval: Double) {
        self.sensorid = sensorid
        self.plugval = plugval
    }
}

/// Composite key: sensorid + setting
@Model
final class EqpLtPickup {
    #Index<EqpLtPickup>([\.sensorid])

    var sensorid: Int
    var setting: Double

    init(sensorid: Int, setting: Double) {
        self.sensorid = sensorid
        self.setting = setting
    }
}

/// Composite key: sensorid + setting + curveid
@Model
final class EqpLtDelay {
    #Index<EqpLtDelay>([\.sensorid, \.curveid])

    var sensorid: Int
    var desc: String
    var setting: Double
    var curveid: Int

    init(sensorid: Int, desc: String, setting: Double, curveid: Int) {
        self.sensorid = sensorid
        self.desc = desc
        self.setting = setting
        self.curveid = curveid
    }
}

/// Composite key: sensorid + setting
@Model
final class EqpStPickup {
    #Index<EqpStPickup>([\.sensorid])

    var sensorid: Int
    var desc: String
    var setting: Double

    init(sensorid: Int, desc: String, setting: Double) {
        self.sensorid = sensorid
        self.desc = desc
        self.setting = setting
    }
}

/// Composite key: sensorid + desc + i2x
@Model
final class EqpStDelay {
    #Index<EqpStDelay>([\.sensorid])

    var sensorid: Int
    var desc: String
    var min_open: Double?
    var min_clear: Double?
    var i2x: Int?

    init(sensorid: Int, desc: String, min_open: Double? = nil,
         min_clear: Double? = nil, i2x: Int? = nil) {
        self.sensorid = sensorid
        self.desc = desc
        self.min_open = min_open
        self.min_clear = min_clear
        self.i2x = i2x
    }
}

/// Composite key: sensorid + setting
@Model
final class EqpInstPickup {
    #Index<EqpInstPickup>([\.sensorid])

    var sensorid: Int
    var desc: String
    var setting: Double

    init(sensorid: Int, desc: String, setting: Double) {
        self.sensorid = sensorid
        self.desc = desc
        self.setting = setting
    }
}

/// Composite key: sensorid + curveid
@Model
final class EqpCurveType {
    #Index<EqpCurveType>([\.sensorid])

    var sensorid: Int
    var curveid: Int
    var name: String
    var slope: Double?

    init(sensorid: Int, curveid: Int, name: String, slope: Double? = nil) {
        self.sensorid = sensorid
        self.curveid = curveid
        self.name = name
        self.slope = slope
    }
}
