//
//  EGForm.swift
//  Egalvanic PZ
//
//  ZP-1723: the new "EG Forms" system, parallel to the legacy
//  UserTaskForm. EGForm is the template/definition; EGFormInstance is
//  a submission. JSONB columns from the server (definition, sample_data,
//  references_json) ride as JSON strings in SwiftData so we don't have
//  to model every block-type shape at the storage layer — the renderer
//  parses on demand.
//
import Foundation
import SwiftData

@Model
final class EGForm {
    @Attribute(.unique) var id: UUID
    var key: String?
    var title: String?
    var definition: String?       // JSONB → JSON string
    var sample_data: String?      // JSONB → JSON string
    var html_template: String?
    var references_json: String?  // JSONB → JSON string
    var form_template_key: String?

    var eg_form_type: Int
    var eg_form_type_name: String?
    var eg_form_type_key: String?
    /// Node-class names this form is mapped to (via
    /// MappingEGFormNodeClass on the server). Mostly meaningful for
    /// NETA forms — populated for any form that has mappings. Default
    /// empty for forms with no mappings or pre-sync rows.
    var node_class_names: [String] = []

    var is_deleted: Bool
    var is_global: Bool
    var is_override: Bool
    var company_id: UUID?

    var created_at: Date
    var modified_at: Date?

    init(
        id: UUID,
        key: String? = nil,
        title: String? = nil,
        definition: String? = nil,
        sample_data: String? = nil,
        html_template: String? = nil,
        references_json: String? = nil,
        form_template_key: String? = nil,
        eg_form_type: Int,
        eg_form_type_name: String? = nil,
        eg_form_type_key: String? = nil,
        node_class_names: [String] = [],
        is_deleted: Bool = false,
        is_global: Bool = false,
        is_override: Bool = false,
        company_id: UUID? = nil,
        created_at: Date = Date(),
        modified_at: Date? = nil
    ) {
        self.id = id
        self.key = key
        self.title = title
        self.definition = definition
        self.sample_data = sample_data
        self.html_template = html_template
        self.references_json = references_json
        self.form_template_key = form_template_key
        self.eg_form_type = eg_form_type
        self.eg_form_type_name = eg_form_type_name
        self.eg_form_type_key = eg_form_type_key
        self.node_class_names = node_class_names
        self.is_deleted = is_deleted
        self.is_global = is_global
        self.is_override = is_override
        self.company_id = company_id
        self.created_at = created_at
        self.modified_at = modified_at
    }

    var definitionData: Data? { definition?.data(using: .utf8) }
    var sampleDataData: Data? { sample_data?.data(using: .utf8) }

    /// Short, human-friendly type label for picker rows, badges, and
    /// row chips. Normalizes back-end variants to a compact display
    /// string. `NETA_FRAGMENT` collapses to `NETA` — the fragment-vs-
    /// full-form split is an authoring distinction; an inspector
    /// picking a form to attach doesn't care.
    var displayTypeLabel: String {
        let raw = (eg_form_type_name ?? eg_form_type_key ?? "").uppercased()
        switch raw {
        case "NETA_FRAGMENT", "NETA FRAGMENT", "NETA-FRAGMENT":
            return "NETA"
        case "":
            return "EG"
        default:
            return raw
        }
    }
}
