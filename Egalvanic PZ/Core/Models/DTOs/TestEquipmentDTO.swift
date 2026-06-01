//
//  TestEquipmentDTO.swift
//  Egalvanic PZ
//

import Foundation

struct TestEquipmentResponse: Codable {
    let data: [TestEquipmentDTO]
    let success: Bool
}

struct TestEquipmentDTO: Codable {
    var id: UUID
    var name: String
    var serial_number: String?
    var calibration_date: String?
    var test_equipment_library_id: UUID?
}
