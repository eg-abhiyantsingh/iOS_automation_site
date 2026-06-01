//
//  EGFormExpression.swift
//  Egalvanic PZ
//
//  ZP-1723: JavaScript expression evaluator + template engine for the V2
//  EG form renderer. The web renderer uses `new Function(...)` to eval
//  arbitrary JS in `visible` / `calculated` / `{{...}}` / `{% if %}` /
//  `{% for %}` constructs. JavaScriptCore (built into iOS) lets us run
//  the same JS engine family on-device, so behavior parity with web is
//  free for any expression a customer writes.
//
//  One JSContext per form-rendering session — created lazily, reused
//  across re-evaluations as form data changes. The renderer mutates the
//  shared `formData` snapshot before each eval so expressions read the
//  current state.
//

import Foundation
import JavaScriptCore

/// Evaluates JS expressions against a form-data dictionary. Not
/// thread-safe; pin to the main actor when used from SwiftUI.
@MainActor
final class EGFormExpression {

    private let ctx: JSContext
    /// Cache of compiled expression source → JS function. Each unique
    /// `visible`/`calculated`/`{{...}}` source is compiled exactly once
    /// per session instead of on every render. Big win — JSContext
    /// `evaluateScript(...)` is ~ms-scale; a cached function call is
    /// µs-scale. Without this cache, a form with 20 conditional blocks
    /// spent 80%+ CPU on every checkbox tap.
    private var compiledCache: [String: JSValue] = [:]

    /// Pending form data — set by callers, pushed to JS lazily on the
    /// next eval. Multiple `updateForm(_:)` calls in the same RunLoop
    /// tick coalesce into one `JSON.parse` round-trip.
    private var pendingForm: [String: Any] = [:]
    private var pendingDirty = true

    init() {
        ctx = JSContext() ?? JSContext()!
        ctx.exceptionHandler = { _, exception in
            if let exception = exception {
                AppLogger.log(.debug, "[EGFormExpression] JS exception: \(exception)", category: .form)
            }
        }
        // Bootstrap the global slot so the very first eval works even
        // before any updateForm call.
        ctx.evaluateScript("var __form = {};")
    }

    /// Stage the form data snapshot that the next eval should see. No
    /// JS work is done here — we only mark dirty and store the dict.
    /// Lets a series of setValue calls coalesce into a single JS push.
    func updateForm(_ data: [String: Any]) {
        pendingForm = data
        pendingDirty = true
    }

    /// Back-compat alias for the old call site; same behavior as
    /// `updateForm(_:)` plus an immediate push.
    func setFormData(_ data: [String: Any]) {
        updateForm(data)
        flushFormIfNeeded()
    }

    private func flushFormIfNeeded() {
        guard pendingDirty else { return }
        let raw = (try? JSONSerialization.data(withJSONObject: pendingForm, options: []))
            .flatMap { String(data: $0, encoding: .utf8) } ?? "{}"
        ctx.evaluateScript("__form = JSON.parse(\(raw.jsStringLiteral()));")
        pendingDirty = false
    }

    /// Evaluate a single expression like `field_a + field_b * 2` or
    /// `["yes","ok"].includes(status)` against the current form data.
    /// Returns nil on syntax error / JS exception so the renderer can
    /// fall back to a sensible default (hide a block, blank a value).
    func eval(_ expr: String) -> JSValue? {
        flushFormIfNeeded()

        if let fn = compiledCache[expr] {
            return fn.call(withArguments: [])
        }

        // Compile once. The wrapping function reads from the global
        // `__form` slot so subsequent calls don't re-marshal the data.
        let source = "(function() { with(__form) { return (\(expr)); } })"
        guard let fn = ctx.evaluateScript(source), !fn.isUndefined, !fn.isNull else {
            return nil
        }
        compiledCache[expr] = fn
        return fn.call(withArguments: [])
    }

    /// Boolean read of `visible` / similar. Anything truthy → true.
    func evalBool(_ expr: String?) -> Bool {
        guard let expr = expr, !expr.isEmpty else { return true }
        return eval(expr)?.toBool() ?? false
    }

    /// String value for `{{ ... }}` interpolations. Falsy values render
    /// as "" rather than "undefined"/"null".
    func evalString(_ expr: String) -> String {
        guard let v = eval(expr), !v.isUndefined, !v.isNull else { return "" }
        return v.toString() ?? ""
    }

    /// Template engine: process `{% if %}` / `{% else %}` / `{% endif %}`,
    /// `{% for x in iter %}…{% endfor %}`, and `{{ expr }}` interpolation.
    /// Direct port of the web renderer's evalTemplate so behavior matches.
    func evalTemplate(_ html: String) -> String {
        var result = html
        let maxPasses = 10

        // {% if cond %}…{% else %}…{% endif %} (nestable)
        for _ in 0..<maxPasses {
            guard let ifRange = result.range(of: #"\{%\s*if\s+(.*?)\s*%\}"#, options: .regularExpression) else { break }
            let ifOpenTag = String(result[ifRange])
            let condExpr = ifOpenTag
                .replacingOccurrences(of: #"^\{%\s*if\s+"#, with: "", options: .regularExpression)
                .replacingOccurrences(of: #"\s*%\}$"#, with: "", options: .regularExpression)

            // Walk to find matching endif, tracking depth and capturing else.
            var depth = 1
            var searchStart = ifRange.upperBound
            var elseIdx: String.Index? = nil
            var endTagRange: Range<String.Index>? = nil

            while depth > 0, searchStart < result.endIndex {
                guard let nextTag = result.range(of: #"\{%\s*(if|else|endif)\b[^%]*%\}"#, options: .regularExpression, range: searchStart..<result.endIndex) else { break }
                let tag = String(result[nextTag])
                if tag.range(of: #"^\{%\s*if\b"#, options: .regularExpression) != nil {
                    depth += 1
                } else if tag.range(of: #"^\{%\s*endif\s*%\}$"#, options: .regularExpression) != nil {
                    depth -= 1
                    if depth == 0 { endTagRange = nextTag; break }
                } else if depth == 1, tag.range(of: #"^\{%\s*else\s*%\}$"#, options: .regularExpression) != nil {
                    elseIdx = nextTag.lowerBound
                }
                searchStart = nextTag.upperBound
            }

            guard let endTagRange = endTagRange else { break }
            let condTrue = evalBool(condExpr)
            let bodyStart = ifRange.upperBound
            let replacement: String
            if let elseIdx = elseIdx {
                let ifBody = String(result[bodyStart..<elseIdx])
                let elseTag = result.range(of: #"\{%\s*else\s*%\}"#, options: .regularExpression, range: elseIdx..<endTagRange.lowerBound) ?? elseIdx..<elseIdx
                let elseBody = String(result[elseTag.upperBound..<endTagRange.lowerBound])
                replacement = condTrue ? ifBody : elseBody
            } else {
                let body = String(result[bodyStart..<endTagRange.lowerBound])
                replacement = condTrue ? body : ""
            }
            result.replaceSubrange(ifRange.lowerBound..<endTagRange.upperBound, with: replacement)
        }

        // {% for item in iter %}…{% endfor %} (non-nested — matches web)
        for _ in 0..<maxPasses {
            guard let forRange = result.range(of: #"\{%\s*for\s+(\w+)\s+in\s+(.*?)\s*%\}"#, options: .regularExpression) else { break }
            let forTag = String(result[forRange])
            let openCaptures = forTag
                .replacingOccurrences(of: #"^\{%\s*for\s+"#, with: "", options: .regularExpression)
                .replacingOccurrences(of: #"\s*%\}$"#, with: "", options: .regularExpression)
            let parts = openCaptures.components(separatedBy: " in ")
            guard parts.count == 2,
                  let endforRange = result.range(of: "{% endfor %}", range: forRange.upperBound..<result.endIndex) else { break }
            let varName = parts[0].trimmingCharacters(in: .whitespaces)
            let listExpr = parts[1].trimmingCharacters(in: .whitespaces)
            let body = String(result[forRange.upperBound..<endforRange.lowerBound])

            var rendered = ""
            if let listVal = eval(listExpr), listVal.isArray, let arr = listVal.toArray() {
                for item in arr {
                    // Stash the loop variable on the JS form snapshot, render
                    // the body recursively (so inner interpolations see it),
                    // then restore. We don't deep-copy formData since the
                    // recursive call is read-only on existing keys.
                    let restoreScript = "var __saved = __form.\(varName); __form.\(varName) = "
                    let raw = (try? JSONSerialization.data(withJSONObject: ["v": item], options: []))
                        .flatMap { String(data: $0, encoding: .utf8) }
                        .map { "JSON.parse(\($0.jsStringLiteral())).v" } ?? "undefined"
                    _ = ctx.evaluateScript(restoreScript + raw + ";")
                    rendered += evalTemplate(body)
                    _ = ctx.evaluateScript("__form.\(varName) = __saved;")
                }
            }
            result.replaceSubrange(forRange.lowerBound..<endforRange.upperBound, with: rendered)
        }

        // {{ expression }} — simple interpolation.
        if result.contains("{{") {
            let regex = try? NSRegularExpression(pattern: #"\{\{(.*?)\}\}"#, options: [])
            if let regex = regex {
                let nsr = result as NSString
                let matches = regex.matches(in: result, options: [], range: NSRange(location: 0, length: nsr.length))
                // Walk matches right-to-left so ranges stay valid as we replace.
                for match in matches.reversed() where match.numberOfRanges >= 2 {
                    let expr = nsr.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespaces)
                    let replacement = evalString(expr)
                    result = (result as NSString).replacingCharacters(in: match.range, with: replacement)
                }
            }
        }

        return result
    }
}

private extension String {
    /// JSON-string-encode a Swift String so it can be embedded inside
    /// JS source as a literal: `"abc\"\n"`.
    func jsStringLiteral() -> String {
        let data = (try? JSONSerialization.data(withJSONObject: [self], options: [])) ?? Data()
        guard let raw = String(data: data, encoding: .utf8) else { return "\"\"" }
        // Strip the surrounding ["..."] brackets.
        var s = raw
        if s.hasPrefix("["), s.hasSuffix("]") { s = String(s.dropFirst().dropLast()) }
        return s
    }
}
