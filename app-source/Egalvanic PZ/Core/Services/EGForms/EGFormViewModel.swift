//
//  EGFormViewModel.swift
//  Egalvanic PZ
//
//  ZP-1723: state owner for an open EG form. Holds the parsed definition,
//  the current form-data dict, and the JSContext expression evaluator.
//  Blocks read from `formData` for rendering and call `setValue` to write.
//  For ZP-1723 milestone 1 only read paths exist; the write path is
//  scaffolded for the input blocks that come next.
//

import Foundation
import SwiftUI

@MainActor
@Observable
final class EGFormViewModel {
    let instance: EGFormInstance
    let definition: [EGBlock]
    private(set) var formData: [String: Any] = [:]

    /// Cached JS evaluator. setFormData is re-pushed whenever formData
    /// changes so `visible`/`calculated`/`{{...}}` expressions see the
    /// current state.
    private let evaluator = EGFormExpression()

    init(instance: EGFormInstance) {
        self.instance = instance
        self.definition = EGBlock.parseDefinition(instance.egForm?.definition) ?? []

        if let submission = instance.form_submission,
           let data = submission.data(using: .utf8),
           let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            self.formData = parsed
        }
        // Normalize legacy integer multiselect values to real Bools.
        // Form authors write strict-equality `visible` expressions
        // (e.g. `tests.applicable_tests.foo === true`) — an int `1`
        // sitting in the JSON from a prior save would always evaluate
        // to `false` until the user re-toggled the checkbox. Walk the
        // definition once at load and coerce every multiselect map
        // value to Bool so strict checks fire immediately.
        Self.normalizeMultiselectValues(in: &formData, blocks: definition, path: [])
        evaluator.updateForm(formData)
    }

    private static func normalizeMultiselectValues(
        in data: inout [String: Any],
        blocks: [EGBlock],
        path: [String]
    ) {
        for block in blocks {
            let key = block.content?.key
            let childPath = key.map { path + [$0] } ?? path

            switch block.type {
            case "multiselect":
                guard let key = key else { continue }
                let fullPath = path + [key]
                if let map = nestedValue(in: data, path: fullPath) as? [String: Any] {
                    var coerced: [String: Bool] = [:]
                    for (k, v) in map {
                        if let b = v as? Bool { coerced[k] = b }
                        else if let n = v as? NSNumber { coerced[k] = n.boolValue }
                        else if let s = v as? String { coerced[k] = (s == "true" || s == "1") }
                    }
                    setNested(in: &data, path: fullPath, value: coerced)
                }
            case "container":
                if let children = block.content?.children {
                    normalizeMultiselectValues(in: &data, blocks: children, path: childPath)
                }
            case "columns":
                if let cols = block.content?.columns {
                    for col in cols {
                        normalizeMultiselectValues(in: &data, blocks: col.children ?? [], path: childPath)
                    }
                }
            default:
                break
            }
        }
    }

    private static func nestedValue(in data: [String: Any], path: [String]) -> Any? {
        var cur: Any? = data
        for k in path {
            guard let dict = cur as? [String: Any] else { return nil }
            cur = dict[k]
        }
        return cur
    }

    private static func setNested(in data: inout [String: Any], path: [String], value: Any) {
        guard let head = path.first else { return }
        if path.count == 1 {
            data[head] = value
            return
        }
        var child = (data[head] as? [String: Any]) ?? [:]
        setNested(in: &child, path: Array(path.dropFirst()), value: value)
        data[head] = child
    }

    // MARK: - Expression eval (delegated to evaluator)
    //
    // `@Observable` only registers a dependency when a tracked property
    // is actually read inside a view body. The evaluator holds form
    // data internally (pushed via setFormData), but SwiftUI can't see
    // through it. We touch `formData` in each reactive method so the
    // call site registers as a dependent of formData mutations.

    func isVisible(_ expr: String?) -> Bool {
        _ = formData
        return evaluator.evalBool(expr)
    }

    func renderTemplate(_ html: String?) -> String {
        _ = formData
        guard let html = html else { return "" }
        return evaluator.evalTemplate(html)
    }

    // MARK: - Nested path access (for `dataPath` walking in containers)

    func value(at path: [String]) -> Any? {
        var cur: Any? = formData
        for k in path {
            guard let dict = cur as? [String: Any] else { return nil }
            cur = dict[k]
        }
        return cur
    }

    /// Flips true on the first user-driven mutation. Used by the
    /// Cancel-with-changes prompt — if false, Cancel can dismiss
    /// without confirmation.
    private(set) var isDirty = false

    func setValue(_ newValue: Any?, at path: [String]) {
        guard !path.isEmpty else { return }
        formData = Self.setNested(formData, path: path, value: newValue)
        // Mark dirty only; the JS push happens on first eval after this
        // mutation. Lets a burst of setValue calls coalesce into one
        // JSON round-trip on the next render pass.
        evaluator.updateForm(formData)
        isDirty = true
    }

    /// Reset the dirty flag — call after a successful submit so a
    /// subsequent Cancel doesn't re-prompt.
    func markClean() {
        isDirty = false
    }

    private static func setNested(_ obj: [String: Any], path: [String], value: Any?) -> [String: Any] {
        guard let head = path.first else { return obj }
        var copy = obj
        if path.count == 1 {
            if let value = value { copy[head] = value } else { copy.removeValue(forKey: head) }
            return copy
        }
        let nested = (obj[head] as? [String: Any]) ?? [:]
        copy[head] = setNested(nested, path: Array(path.dropFirst()), value: value)
        return copy
    }

    /// Serialize current form_data to a JSON string for persistence.
    func formDataString() -> String? {
        guard let data = try? JSONSerialization.data(withJSONObject: formData, options: []),
              let s = String(data: data, encoding: .utf8) else { return nil }
        return s
    }

    // MARK: - Typed value accessors / SwiftUI bindings

    func stringValue(at path: [String]) -> String {
        switch value(at: path) {
        case let s as String:        return s
        case let n as NSNumber:      return n.stringValue
        case let b as Bool:          return b ? "true" : "false"
        default:                     return ""
        }
    }

    func boolValue(at path: [String]) -> Bool {
        switch value(at: path) {
        case let b as Bool:          return b
        case let n as NSNumber:      return n.boolValue
        case let s as String:        return s == "true" || s == "1"
        default:                     return false
        }
    }

    /// For multiselect blocks: the data shape is `{ optionKey: Bool }`.
    func multiselectMap(at path: [String]) -> [String: Bool] {
        if let d = value(at: path) as? [String: Bool] { return d }
        if let d = value(at: path) as? [String: Any] {
            var out: [String: Bool] = [:]
            for (k, v) in d { out[k] = (v as? Bool) ?? ((v as? NSNumber)?.boolValue ?? false) }
            return out
        }
        return [:]
    }

    func stringBinding(at path: [String]) -> Binding<String> {
        Binding(
            get: { self.stringValue(at: path) },
            set: { self.setValue($0, at: path) }
        )
    }

    func boolBinding(at path: [String]) -> Binding<Bool> {
        Binding(
            get: { self.boolValue(at: path) },
            set: { self.setValue($0, at: path) }
        )
    }

    /// Evaluate a `calculated` expression and return its current value.
    /// Used by input/checkbox blocks with a `calculated` content field.
    func evalCalculated(_ expr: String?) -> Any? {
        _ = formData
        guard let expr = expr, !expr.isEmpty else { return nil }
        guard let js = evaluator.eval(expr) else { return nil }
        if js.isUndefined || js.isNull { return nil }
        if js.isBoolean { return js.toBool() }
        if js.isNumber  { return js.toNumber() }
        if js.isString  { return js.toString() }
        return js.toObject()
    }
}
