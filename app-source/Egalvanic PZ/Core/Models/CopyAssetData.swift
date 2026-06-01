//
//  CopyAssetData.swift
//  Egalvanic PZ
//
//  Copy asset details data models
//

import Foundation

enum CopyDirection {
    case from, to
}

enum CopyableField: String, CaseIterable, Identifiable {
    case coreAttributes
    case assetSubtype
    case serviceability

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .coreAttributes: return "Core Attributes"
        case .assetSubtype: return "Asset Subtype"
        case .serviceability: return "Serviceability"
        }
    }
}

struct CopyAssetData {
    let coreAttributes: [UUID: String]?
    let nodeSubtype: NodeSubtype?
    let serviceability: Serviceability?
    let serviceabilityNote: String?
    let com: Int?
    let comCalculation: COMCalculation?
    let selectedFields: Set<CopyableField>
    let sourceLabel: String
}
