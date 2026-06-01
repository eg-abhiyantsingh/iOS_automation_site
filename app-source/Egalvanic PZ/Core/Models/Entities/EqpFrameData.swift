//
//  EqpFrameData.swift
//  Egalvanic PZ
//
//  TMT frame settings cache models for offline frame/trip configuration.
//

import Foundation
import SwiftData

/// TMT frame size for a breaker style.
/// Composite key: styleid + framesize + framedesc (frame IDs are NOT globally unique).
@Model
final class EqpFrame {
    #Index<EqpFrame>([\.styleid])

    var frameid: Int
    var styleid: Int
    var framesize: Int?
    var framedesc: String?
    var sec1name: String?
    var sec2name: String?

    init(frameid: Int, styleid: Int, framesize: Int? = nil, framedesc: String? = nil,
         sec1name: String? = nil, sec2name: String? = nil) {
        self.frameid = frameid
        self.styleid = styleid
        self.framesize = framesize
        self.framedesc = framedesc
        self.sec1name = sec1name
        self.sec2name = sec2name
    }
}

/// Trip amp value for a TMT frame size.
/// Composite key: frameid + tripamp
@Model
final class EqpFrameAmp {
    #Index<EqpFrameAmp>([\.frameid])

    var frameid: Int
    var tripamp: Int?

    init(frameid: Int, tripamp: Int? = nil) {
        self.frameid = frameid
        self.tripamp = tripamp
    }
}

/// Instantaneous setting for a TMT frame size.
/// Composite key: frameid + fsetting
@Model
final class EqpFrameSetting {
    #Index<EqpFrameSetting>([\.frameid])

    var frameid: Int
    var fsetting: Double?
    var sdesc: String?
    var flow: Double?
    var fhigh: Double?

    init(frameid: Int, fsetting: Double? = nil, sdesc: String? = nil,
         flow: Double? = nil, fhigh: Double? = nil) {
        self.frameid = frameid
        self.fsetting = fsetting
        self.sdesc = sdesc
        self.flow = flow
        self.fhigh = fhigh
    }
}
