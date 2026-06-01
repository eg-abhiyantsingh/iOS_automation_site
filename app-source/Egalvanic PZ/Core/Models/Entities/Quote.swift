//
//  Quote.swift
//  SwiftDataTutorial
//
import Foundation
import SwiftData

@Model
final class Quote {
    @Attribute(.unique) var id: UUID
    var created_date: Date?
    var modified_date: Date?
    var title: String?
    var sow: String?  // JSON stored as String
    var tnm: String?  // JSON stored as String
    var sld: SLDV2?
    var quoteDescription: String?
    var status: String?
    var is_deleted: Bool
    var tasks: [UserTask]
    
    init(id: UUID, created_date: Date?, modified_date: Date?, title: String?, sow: String?, tnm: String?, sld: SLDV2? = nil, quoteDescription: String?, status: String?, is_deleted: Bool, tasks: [UserTask] = []) {
        self.id = id
        self.created_date = created_date
        self.modified_date = modified_date
        self.title = title
        self.sow = sow
        self.tnm = tnm
        self.sld = sld
        self.quoteDescription = quoteDescription
        self.status = status
        self.is_deleted = is_deleted
        self.tasks = tasks
    }
}