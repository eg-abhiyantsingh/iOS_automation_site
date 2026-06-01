//
//  EGBlock.swift
//  Egalvanic PZ
//
//  ZP-1723: data model for V2 EG form blocks. Decoded from the
//  `definition` JSONB on EGForm. Mirrors the JSX renderer's block shape
//  so behavior parity is straightforward to keep. Only the subset of
//  fields needed by the iOS renderer is decoded — the long tail is
//  ignored without failing the decode.
//

import Foundation

/// One block in a V2 form definition. Top-level definition is `[EGBlock]`.
struct EGBlock: Decodable {
    let type: String
    let content: EGBlockContent?
}

/// Long-form content dictionary. Every field is optional — different block
/// types use different subsets. Codable's default decoding skips missing
/// keys silently which is exactly what we want here.
struct EGBlockContent: Decodable {
    // Universal
    let key: String?
    let label: String?
    let visible: String?
    let calculated: String?

    // text / message / container header
    let html: String?

    // image
    let src: String?
    let alt: String?

    // container / columns layout
    let display: String?            // "panel" | "well" | "none"
    let variant: String?            // info | warning | success | error | neutral
    let collapsible: Bool?
    let default_collapsed: Bool?
    let align: String?              // for columns: "center" | "bottom" | nil(top)

    // container nests blocks directly
    let children: [EGBlock]?

    // columns nest one level deeper through a column object
    let columns: [EGBlockColumn]?

    // input
    let prefix: String?
    let suffix: String?
    let inputType: String?
    let placeholder: String?

    // select / multiselect / radio / switch
    let options: [EGBlockOption]?
    let options_source: String?
    let fullWidth: Bool?

    // checkbox / input — calc + override
    let can_overwrite: Bool?
    let required: Bool?

    // data_table
    let table_type: String?               // "key_value" | "row" | "hybrid" | "column" (default)
    let column_headings: [EGTableHeading]?
    let row_headings: [EGTableHeading]?
    let data_columns: Int?                // for "row" type
    let rows: Int?                        // for "column" type (fixed-size mode)
    let dynamic_rows: Bool?               // for "column" type — show "+ Add Row"
}

/// One column inside a `columns` block.
struct EGBlockColumn: Decodable {
    let children: [EGBlock]?
}

/// Static option for select/radio/multiselect/switch. The wire key is
/// `key` (not `value`) — matches what the web renderer reads.
struct EGBlockOption: Decodable {
    let key: String?
    let label: String?
    let color: String?  // optional, used by switch to color the active segment
}

/// One row- or column-heading in a `data_table` block. The web V2
/// renderer's `renderCell` reads its field_type, calc, options, etc.
/// off the heading — not the block — so each cell can mix types.
struct EGTableHeading: Decodable {
    let key: String?
    let label: String?
    let field_type: String?              // "text" | "number" | "date" | "checkbox" | "select" | "switch"
    let options: [EGBlockOption]?
    let options_source: String?
    let calculated: String?
    let can_overwrite: Bool?
}

/// Wrapper for the `{"blocks": [...]}` form definition shape. Some
/// definitions are bare arrays; others wrap blocks under `"blocks"`.
/// The web V2 renderer accepts both via `definition.blocks || definition`,
/// so iOS does the same.
private struct EGBlockWrapper: Decodable {
    let blocks: [EGBlock]?
}

extension EGBlock {
    /// Parse a stored definition JSON string into `[EGBlock]`. Accepts
    /// either a top-level array or an object with a `"blocks"` key.
    /// Returns nil on missing / unparsable input so callers can show
    /// an empty state rather than throwing.
    static func parseDefinition(_ json: String?) -> [EGBlock]? {
        guard let json = json,
              let data = json.data(using: .utf8) else { return nil }
        let decoder = JSONDecoder()
        // Most common today: bare array.
        if let arr = try? decoder.decode([EGBlock].self, from: data) {
            return arr
        }
        // Newer shape: { "blocks": [...] }.
        if let wrapped = try? decoder.decode(EGBlockWrapper.self, from: data),
           let blocks = wrapped.blocks {
            return blocks
        }
        return nil
    }
}
