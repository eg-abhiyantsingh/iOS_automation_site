//
//  SldViewerDTO.swift
//  Egalvanic PZ
//
//  DTOs for the SLD viewer dynamic update API
//

import Foundation

struct SldViewerLatestResponse: Decodable {
    let success: Bool
    let data: SldViewerLatestData?
}

struct SldViewerLatestData: Decodable {
    let checksum: String
    let fileSize: Int64
    let releaseNotes: String?
    let sldViewerDownloadUrl: String
    let sldViewerVersion: String
}
