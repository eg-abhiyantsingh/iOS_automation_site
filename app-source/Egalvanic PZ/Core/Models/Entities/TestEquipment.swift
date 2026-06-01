//
//  TestEquipment.swift
//  Egalvanic PZ
//

import Foundation
import SwiftData

@Model
final class TestEquipment {
    @Attribute(.unique) var id: UUID
    var name: String
    var serialNumber: String?
    var calibrationDate: String?
    var testEquipmentLibraryId: UUID?

    init(id: UUID, name: String, serialNumber: String? = nil, calibrationDate: String? = nil, testEquipmentLibraryId: UUID? = nil) {
        self.id = id
        self.name = name
        self.serialNumber = serialNumber
        self.calibrationDate = calibrationDate
        self.testEquipmentLibraryId = testEquipmentLibraryId
    }
}
