//
//  EqpLibrary.swift
//  Egalvanic PZ
//
//  Equipment library cache models for offline equipment picker.
//

import Foundation
import SwiftData

@Model
final class EqpCategory {
    @Attribute(.unique) var id: String  // slug, e.g. "mccb-breakers"
    var name: String

    init(id: String, name: String) {
        self.id = id
        self.name = name
    }
}

@Model
final class EqpItem {
    #Index<EqpItem>([\.categorySlug])

    var id: Int  // Not globally unique — same ID can appear in different categories
    var categorySlug: String
    var manufacturer: String
    var type: String
    var cstandard: String?
    var acdc: String?
    var style: String?  // For lv-trip-units and emt categories

    init(id: Int, categorySlug: String, manufacturer: String, type: String,
         cstandard: String? = nil, acdc: String? = nil, style: String? = nil) {
        self.id = id
        self.categorySlug = categorySlug
        self.manufacturer = manufacturer
        self.type = type
        self.cstandard = cstandard
        self.acdc = acdc
        self.style = style
    }
}

@Model
final class EqpStyle {
    #Index<EqpStyle>([\.itemId, \.categorySlug])

    var id: Int  // Not globally unique across categories
    var itemId: Int
    var categorySlug: String
    var style: String
    var rContCurrent: Double?
    var rNomMva: Double?
    var rNomKv: Double?
    var rMaxKv: Double?
    var rSymKa: Double?
    var framesize: String?
    var framedesc: String?
    var tmtUseSst: Int?
    var tmtSstMfr: String?
    var tmtSstType: String?
    var tmtSstStyle: String?

    init(id: Int, itemId: Int, categorySlug: String, style: String,
         rContCurrent: Double? = nil, rNomMva: Double? = nil, rNomKv: Double? = nil,
         rMaxKv: Double? = nil, rSymKa: Double? = nil,
         framesize: String? = nil, framedesc: String? = nil,
         tmtUseSst: Int? = nil, tmtSstMfr: String? = nil,
         tmtSstType: String? = nil, tmtSstStyle: String? = nil) {
        self.id = id
        self.itemId = itemId
        self.categorySlug = categorySlug
        self.style = style
        self.rContCurrent = rContCurrent
        self.rNomMva = rNomMva
        self.rNomKv = rNomKv
        self.rMaxKv = rMaxKv
        self.rSymKa = rSymKa
        self.framesize = framesize
        self.framedesc = framedesc
        self.tmtUseSst = tmtUseSst
        self.tmtSstMfr = tmtSstMfr
        self.tmtSstType = tmtSstType
        self.tmtSstStyle = tmtSstStyle
    }
}
