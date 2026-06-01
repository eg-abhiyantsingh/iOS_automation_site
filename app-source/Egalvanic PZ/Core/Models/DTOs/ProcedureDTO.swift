//
//  ProcedureDTO.swift
//  Egalvanic PZ
//
//  Wire format for GET /lookup/procedures. Backend returns a flat list;
//  the UI groups by `node_subtype` (with a "General" fallback) at render
//  time — same shape used by the web client.
//
import Foundation

struct ProcedureLookupResponse: Codable {
    let success: Bool
    let data: [ProcedureDTO]
}

struct ProcedureDTO: Codable {
    var id: UUID
    var name: String
    var description: String?
    var procedure_master_name: String?
    var form_id: UUID?
    var use_proxy: Bool?
    var node_class_id: UUID?
    var node_subtype: String?
    var node_subtype_id: UUID?
}
