//
//  EGFormExternalOptions.swift
//  Egalvanic PZ
//
//  ZP-1723: resolve `options_source` for select / data_table cells.
//
//  Web V2 declares a small registry of external datasets — for
//  example, `test_equipment` pulls from `/test-equipment` and presents
//  the result as a list of `{ key: id, label: name (serial_number) }`.
//  iOS reads the same datasets from local SwiftData so the picker
//  works offline.
//
//  Selected values are wrapped as `{ _ext: source, value: key }` so a
//  consumer (the form itself, downstream calc expressions) can tell an
//  externally-sourced selection apart from an inline literal — same
//  shape the web renderer writes. We always unwrap on read and rewrap
//  on write.
//

import Foundation
import SwiftData

@MainActor
enum EGExternalOptions {

    /// Translate an `options_source` key into a list of pickable
    /// options sourced from local SwiftData. Unknown sources return an
    /// empty list so the cell shows "no options" rather than crashing.
    static func resolve(source: String, modelContext: ModelContext) -> [EGBlockOption] {
        switch source {
        case "test_equipment":
            let descriptor = FetchDescriptor<TestEquipment>()
            let items = (try? modelContext.fetch(descriptor)) ?? []
            return items.map { item in
                let descSuffix = (item.serialNumber.flatMap { $0.isEmpty ? nil : $0 }).map { " (\($0))" } ?? ""
                // Postgres stores UUIDs lowercase, the /test-equipment
                // API returns them lowercase, and web matches options
                // by exact key. Swift's UUID.uuidString is uppercase
                // by default — lowercase here so values written by iOS
                // round-trip cleanly with the web renderer.
                return EGBlockOption(
                    key: item.id.uuidString.lowercased(),
                    label: item.name + descSuffix,
                    color: nil
                )
            }
        default:
            return []
        }
    }

    /// Read the option key out of a stored form value, accepting either
    /// the wrapped `{ _ext, value }` shape used for external sources or
    /// a bare string for inline-options selects.
    static func unwrap(_ raw: Any?) -> String {
        if let d = raw as? [String: Any], d["_ext"] != nil {
            return (d["value"] as? String) ?? ""
        }
        return (raw as? String) ?? ""
    }

    /// Wrap an option key for storage. Inline-options selects store the
    /// key directly; external-source selects store `{ _ext, value }`.
    /// Empty selection clears the value entirely.
    static func wrap(value: String, source: String?) -> Any? {
        guard let source = source, !source.isEmpty else {
            return value.isEmpty ? nil : value
        }
        guard !value.isEmpty else { return nil }
        return ["_ext": source, "value": value]
    }
}
