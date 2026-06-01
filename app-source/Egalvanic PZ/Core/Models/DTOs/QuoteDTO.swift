//
//  QuoteDTO.swift
//  SwiftDataTutorial
//
import Foundation

struct QuoteDTO: Codable {
    var id: UUID
    var created_date: Date?
    var modified_date: Date?
    var title: String?
    var sow: String?
    var tnm: String?
    var sld_id: UUID?
    var description: String?
    var status: String?
    var is_deleted: Bool
}