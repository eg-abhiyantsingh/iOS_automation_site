//
//  EGFormDTO.swift
//  Egalvanic PZ
//
//  ZP-1723: API DTOs for the new EG Form system.
//
//  Server delivers `definition`, `sample_data`, `references_json`, and
//  `form_submission` as JSON values (dict / array / null). We carry them
//  as AnyCodable on the DTO and stringify into SwiftData on persist so
//  the renderer can re-parse on demand without committing to a fixed
//  schema at the storage layer.
//
import Foundation

struct EGFormDTO: Codable {
    let id: String
    let key: String?
    let title: String?
    let definition: AnyCodable?
    let sample_data: AnyCodable?
    let html_template: String?
    let references_json: AnyCodable?
    let form_template_key: String?
    let eg_form_type: Int
    let eg_form_type_name: String?
    let eg_form_type_key: String?
    /// Names of node classes this form is wired to via
    /// MappingEGFormNodeClass. Empty / nil for non-NETA forms.
    let node_class_names: [String]?
    let is_deleted: Bool?
    let is_global: Bool?
    let is_override: Bool?
    let company_id: String?
    let created_at: String?
    let modified_at: String?

    var definitionString: String? { Self.jsonString(from: definition) }
    var sampleDataString: String? { Self.jsonString(from: sample_data) }
    var referencesString: String? { Self.jsonString(from: references_json) }

    private static func jsonString(from value: AnyCodable?) -> String? {
        guard let v = value?.value else { return nil }
        if let s = v as? String { return s }
        guard let data = try? JSONSerialization.data(withJSONObject: v, options: []),
              let s = String(data: data, encoding: .utf8) else { return nil }
        return s
    }
}

struct EGFormInstanceDTO: Codable {
    let id: String
    let eg_form_id: String
    let form_submission: AnyCodable?
    let submitted: Bool?
    let is_deleted: Bool?
    let created_at: String?
    let modified_at: String?

    var formSubmissionString: String? {
        guard let v = form_submission?.value else { return nil }
        if let s = v as? String { return s }
        guard let data = try? JSONSerialization.data(withJSONObject: v, options: []),
              let s = String(data: data, encoding: .utf8) else { return nil }
        return s
    }
}
