//
//  ExpressionEvaluator.swift
//  Egalvanic PZ
//
//  Lightweight expression evaluator for calculated fields.
//  Supports field references [Field Name], arithmetic, comparisons,
//  string/numeric literals, null handling, and built-in functions.
//

import Foundation

// MARK: - Expression Value Type

/// Represents a value in the expression evaluation system.
private enum ExprValue: Equatable {
    case number(Double)
    case string(String)
    case boolean(Bool)
    case null

    var asDouble: Double? {
        switch self {
        case .number(let d): return d
        case .string(let s): return Double(s)
        case .boolean(let b): return b ? 1.0 : 0.0
        case .null: return nil
        }
    }

    var asString: String? {
        switch self {
        case .number(let d):
            // Format without trailing zeros for integers
            return d.truncatingRemainder(dividingBy: 1) == 0
                ? String(format: "%.0f", d)
                : String(d)
        case .string(let s): return s
        case .boolean(let b): return b ? "true" : "false"
        case .null: return nil
        }
    }

    var asBool: Bool {
        switch self {
        case .number(let d): return d != 0
        case .string(let s): return !s.isEmpty
        case .boolean(let b): return b
        case .null: return false
        }
    }

    var isNull: Bool {
        if case .null = self { return true }
        return false
    }
}

// MARK: - Token Types

private enum Token: Equatable {
    case number(Double)
    case string(String)
    case identifier(String)       // Function names, null, true, false
    case fieldRef(String)         // [Field Name]
    case leftParen
    case rightParen
    case comma
    case plus
    case minus
    case multiply
    case divide
    case equal                    // ==
    case notEqual                 // !=
    case lessThanOrEqual          // <=
    case greaterThanOrEqual       // >=
    case lessThan                 // <
    case greaterThan              // >
    case eof
}

// MARK: - Tokenizer

private struct Tokenizer {
    private let input: [Character]
    private var pos: Int = 0

    init(_ expression: String) {
        self.input = Array(expression)
    }

    mutating func tokenize() -> [Token] {
        var tokens: [Token] = []
        while pos < input.count {
            let ch = input[pos]

            if ch.isWhitespace || ch.isNewline {
                pos += 1
                continue
            }

            switch ch {
            case "[":
                tokens.append(readFieldRef())
            case "\"":
                tokens.append(readString())
            case "(":
                tokens.append(.leftParen)
                pos += 1
            case ")":
                tokens.append(.rightParen)
                pos += 1
            case ",":
                tokens.append(.comma)
                pos += 1
            case "+":
                tokens.append(.plus)
                pos += 1
            case "-":
                // Check if this is a negative number (after operator, comma, or paren)
                if isUnaryMinus(tokens: tokens) && pos + 1 < input.count && (input[pos + 1].isNumber || input[pos + 1] == ".") {
                    tokens.append(readNumber(negative: true))
                } else {
                    tokens.append(.minus)
                    pos += 1
                }
            case "*":
                tokens.append(.multiply)
                pos += 1
            case "/":
                tokens.append(.divide)
                pos += 1
            case "=":
                if pos + 1 < input.count && input[pos + 1] == "=" {
                    tokens.append(.equal)
                    pos += 2
                } else {
                    // Single = treated as ==
                    tokens.append(.equal)
                    pos += 1
                }
            case "!":
                if pos + 1 < input.count && input[pos + 1] == "=" {
                    tokens.append(.notEqual)
                    pos += 2
                } else {
                    pos += 1 // skip unknown
                }
            case "<":
                if pos + 1 < input.count && input[pos + 1] == "=" {
                    tokens.append(.lessThanOrEqual)
                    pos += 2
                } else {
                    tokens.append(.lessThan)
                    pos += 1
                }
            case ">":
                if pos + 1 < input.count && input[pos + 1] == "=" {
                    tokens.append(.greaterThanOrEqual)
                    pos += 2
                } else {
                    tokens.append(.greaterThan)
                    pos += 1
                }
            default:
                if ch.isNumber || ch == "." {
                    tokens.append(readNumber(negative: false))
                } else if ch.isLetter || ch == "_" {
                    tokens.append(readIdentifier())
                } else {
                    pos += 1 // skip unknown characters
                }
            }
        }
        tokens.append(.eof)
        return tokens
    }

    private func isUnaryMinus(tokens: [Token]) -> Bool {
        guard let last = tokens.last else { return true }
        switch last {
        case .number, .string, .identifier, .fieldRef, .rightParen:
            return false
        default:
            return true
        }
    }

    private mutating func readFieldRef() -> Token {
        pos += 1 // skip [
        var name = ""
        while pos < input.count && input[pos] != "]" {
            name.append(input[pos])
            pos += 1
        }
        if pos < input.count { pos += 1 } // skip ]
        return .fieldRef(name.trimmingCharacters(in: .whitespaces))
    }

    private mutating func readString() -> Token {
        pos += 1 // skip opening quote
        var value = ""
        while pos < input.count && input[pos] != "\"" {
            if input[pos] == "\\" && pos + 1 < input.count {
                pos += 1
                value.append(input[pos])
            } else {
                value.append(input[pos])
            }
            pos += 1
        }
        if pos < input.count { pos += 1 } // skip closing quote
        return .string(value)
    }

    private mutating func readNumber(negative: Bool) -> Token {
        if negative { pos += 1 } // skip the minus sign
        var numStr = negative ? "-" : ""
        var hasDot = false
        while pos < input.count && (input[pos].isNumber || (input[pos] == "." && !hasDot)) {
            if input[pos] == "." { hasDot = true }
            numStr.append(input[pos])
            pos += 1
        }
        return .number(Double(numStr) ?? 0)
    }

    private mutating func readIdentifier() -> Token {
        var name = ""
        while pos < input.count && (input[pos].isLetter || input[pos].isNumber || input[pos] == "_") {
            name.append(input[pos])
            pos += 1
        }
        return .identifier(name)
    }
}

// MARK: - Parser & Evaluator

private struct Parser {
    private let tokens: [Token]
    private var pos: Int = 0
    private let fieldValues: [String: ExprValue]

    init(tokens: [Token], fieldValues: [String: ExprValue]) {
        self.tokens = tokens
        self.fieldValues = fieldValues
    }

    private var current: Token {
        pos < tokens.count ? tokens[pos] : .eof
    }

    private mutating func advance() {
        pos += 1
    }

    private mutating func expect(_ token: Token) {
        if current == token { advance() }
    }

    // MARK: - Expression Parsing (Recursive Descent)

    /// Entry point: parse a full expression
    mutating func parseExpression() -> ExprValue {
        return parseComparison()
    }

    /// Comparison: handles ==, !=, <, >, <=, >=
    private mutating func parseComparison() -> ExprValue {
        var left = parseAddSub()

        while true {
            switch current {
            case .equal:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .equal)
            case .notEqual:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .notEqual)
            case .lessThan:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .lessThan)
            case .greaterThan:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .greaterThan)
            case .lessThanOrEqual:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .lessThanOrEqual)
            case .greaterThanOrEqual:
                advance()
                let right = parseAddSub()
                left = compareValues(left, right, op: .greaterThanOrEqual)
            default:
                return left
            }
        }
    }

    private enum ComparisonOp {
        case equal, notEqual, lessThan, greaterThan, lessThanOrEqual, greaterThanOrEqual
    }

    private func compareValues(_ left: ExprValue, _ right: ExprValue, op: ComparisonOp) -> ExprValue {
        // Null comparisons
        if left.isNull || right.isNull {
            switch op {
            case .equal: return .boolean(left.isNull && right.isNull)
            case .notEqual: return .boolean(!(left.isNull && right.isNull))
            default: return .boolean(false)
            }
        }

        // Numeric comparison if both can be numbers
        if let lNum = left.asDouble, let rNum = right.asDouble,
           !isStringType(left) || !isStringType(right) {
            switch op {
            case .equal: return .boolean(lNum == rNum)
            case .notEqual: return .boolean(lNum != rNum)
            case .lessThan: return .boolean(lNum < rNum)
            case .greaterThan: return .boolean(lNum > rNum)
            case .lessThanOrEqual: return .boolean(lNum <= rNum)
            case .greaterThanOrEqual: return .boolean(lNum >= rNum)
            }
        }

        // String comparison
        let lStr = left.asString ?? ""
        let rStr = right.asString ?? ""
        switch op {
        case .equal: return .boolean(lStr == rStr)
        case .notEqual: return .boolean(lStr != rStr)
        case .lessThan: return .boolean(lStr < rStr)
        case .greaterThan: return .boolean(lStr > rStr)
        case .lessThanOrEqual: return .boolean(lStr <= rStr)
        case .greaterThanOrEqual: return .boolean(lStr >= rStr)
        }
    }

    private func isStringType(_ value: ExprValue) -> Bool {
        if case .string = value { return true }
        return false
    }

    /// Addition and subtraction
    private mutating func parseAddSub() -> ExprValue {
        var left = parseMulDiv()

        while true {
            switch current {
            case .plus:
                advance()
                let right = parseMulDiv()
                if let l = left.asDouble, let r = right.asDouble {
                    left = .number(l + r)
                } else {
                    // String concatenation fallback
                    left = .string((left.asString ?? "") + (right.asString ?? ""))
                }
            case .minus:
                advance()
                let right = parseMulDiv()
                if let l = left.asDouble, let r = right.asDouble {
                    left = .number(l - r)
                } else {
                    left = .null
                }
            default:
                return left
            }
        }
    }

    /// Multiplication and division
    private mutating func parseMulDiv() -> ExprValue {
        var left = parseUnary()

        while true {
            switch current {
            case .multiply:
                advance()
                let right = parseUnary()
                if let l = left.asDouble, let r = right.asDouble {
                    left = .number(l * r)
                } else {
                    left = .null
                }
            case .divide:
                advance()
                let right = parseUnary()
                if let l = left.asDouble, let r = right.asDouble, r != 0 {
                    left = .number(l / r)
                } else {
                    left = .null
                }
            default:
                return left
            }
        }
    }

    /// Unary minus
    private mutating func parseUnary() -> ExprValue {
        if current == .minus {
            advance()
            let val = parsePrimary()
            if let d = val.asDouble {
                return .number(-d)
            }
            return .null
        }
        return parsePrimary()
    }

    /// Primary: numbers, strings, field refs, function calls, parenthesized expressions
    private mutating func parsePrimary() -> ExprValue {
        switch current {
        case .number(let d):
            advance()
            return .number(d)

        case .string(let s):
            advance()
            return .string(s)

        case .fieldRef(let name):
            advance()
            return fieldValues[name] ?? .null

        case .identifier(let name):
            advance()
            let upperName = name.uppercased()

            // Keywords
            if upperName == "NULL" || upperName == "NIL" {
                return .null
            }
            if upperName == "TRUE" {
                return .boolean(true)
            }
            if upperName == "FALSE" {
                return .boolean(false)
            }

            // Function call
            if current == .leftParen {
                return parseFunction(upperName)
            }

            // Bare identifier - treat as field reference
            return fieldValues[name] ?? .null

        case .leftParen:
            advance()
            let val = parseExpression()
            expect(.rightParen)
            return val

        default:
            advance()
            return .null
        }
    }

    // MARK: - Built-in Functions

    private mutating func parseFunction(_ name: String) -> ExprValue {
        expect(.leftParen)
        let args = parseArgumentList()
        expect(.rightParen)

        switch name {
        case "IF":
            return evalIF(args)
        case "ISNULL":
            return evalISNULL(args)
        case "COALESCE":
            return evalCOALESCE(args)
        case "ROUND":
            return evalROUND(args)
        case "FLOOR":
            return evalFLOOR(args)
        case "CEIL":
            return evalCEIL(args)
        case "ABS":
            return evalABS(args)
        case "MIN":
            return evalMIN(args)
        case "MAX":
            return evalMAX(args)
        case "CONCAT":
            return evalCONCAT(args)
        case "UPPER":
            return evalUPPER(args)
        case "LOWER":
            return evalLOWER(args)
        case "BETWEEN":
            return evalBETWEEN(args)
        default:
            return .null
        }
    }

    private mutating func parseArgumentList() -> [ExprValue] {
        var args: [ExprValue] = []
        if current == .rightParen { return args }

        args.append(parseExpression())
        while current == .comma {
            advance()
            args.append(parseExpression())
        }
        return args
    }

    // MARK: - Function Implementations

    private func evalIF(_ args: [ExprValue]) -> ExprValue {
        guard args.count >= 3 else { return .null }
        return args[0].asBool ? args[1] : args[2]
    }

    private func evalISNULL(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first else { return .boolean(true) }
        return .boolean(first.isNull)
    }

    private func evalCOALESCE(_ args: [ExprValue]) -> ExprValue {
        for arg in args {
            if !arg.isNull { return arg }
        }
        return .null
    }

    private func evalROUND(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let val = first.asDouble else { return .null }
        let digits = args.count > 1 ? (args[1].asDouble.map { Int($0) } ?? 0) : 0
        let multiplier = pow(10.0, Double(digits))
        return .number((val * multiplier).rounded() / multiplier)
    }

    private func evalFLOOR(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let val = first.asDouble else { return .null }
        return .number(floor(val))
    }

    private func evalCEIL(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let val = first.asDouble else { return .null }
        return .number(ceil(val))
    }

    private func evalABS(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let val = first.asDouble else { return .null }
        return .number(abs(val))
    }

    private func evalMIN(_ args: [ExprValue]) -> ExprValue {
        let nums = args.compactMap { $0.asDouble }
        guard let minVal = nums.min() else { return .null }
        return .number(minVal)
    }

    private func evalMAX(_ args: [ExprValue]) -> ExprValue {
        let nums = args.compactMap { $0.asDouble }
        guard let maxVal = nums.max() else { return .null }
        return .number(maxVal)
    }

    private func evalCONCAT(_ args: [ExprValue]) -> ExprValue {
        let parts = args.map { $0.asString ?? "" }
        return .string(parts.joined())
    }

    private func evalUPPER(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let str = first.asString else { return .null }
        return .string(str.uppercased())
    }

    private func evalLOWER(_ args: [ExprValue]) -> ExprValue {
        guard let first = args.first, let str = first.asString else { return .null }
        return .string(str.lowercased())
    }

    private func evalBETWEEN(_ args: [ExprValue]) -> ExprValue {
        guard args.count >= 3,
              let val = args[0].asDouble,
              let min = args[1].asDouble,
              let max = args[2].asDouble else { return .null }
        return .boolean(val >= min && val <= max)
    }
}

// MARK: - Public API

enum ExpressionEvaluator {

    // Cached regex for field reference extraction
    private static let fieldRefRegex = try? NSRegularExpression(pattern: "\\[([^\\]]+)\\]")

    /// Extract field references `[Field Name]` from an expression string.
    static func extractFieldReferences(from expression: String) -> [String] {
        var refs: [String] = []
        guard let regex = fieldRefRegex else { return refs }
        let range = NSRange(expression.startIndex..., in: expression)
        let matches = regex.matches(in: expression, range: range)
        for match in matches {
            if let captureRange = Range(match.range(at: 1), in: expression) {
                let fieldName = String(expression[captureRange]).trimmingCharacters(in: .whitespaces)
                if !refs.contains(fieldName) {
                    refs.append(fieldName)
                }
            }
        }
        return refs
    }

    /// Evaluate a single expression given field values keyed by field name.
    /// Returns the string representation of the result, or nil if the result is null.
    static func evaluate(
        expression: String,
        fieldValues: [String: String],
        precision: Int? = nil
    ) -> String? {
        // Convert string field values to ExprValues
        var exprFieldValues: [String: ExprValue] = [:]
        for (key, value) in fieldValues {
            if value.isEmpty {
                exprFieldValues[key] = .null
            } else if let d = Double(value) {
                exprFieldValues[key] = .number(d)
            } else {
                exprFieldValues[key] = .string(value)
            }
        }

        // Check if expression uses null-safe functions
        let upperExpr = expression.uppercased()
        let usesNullSafe = upperExpr.contains("ISNULL") || upperExpr.contains("COALESCE")

        // If not null-safe, check if any referenced field is null/empty
        if !usesNullSafe {
            let refs = extractFieldReferences(from: expression)
            for ref in refs {
                let val = exprFieldValues[ref]
                if val == nil || val == .null {
                    return nil
                }
            }
        }

        // Tokenize
        var tokenizer = Tokenizer(expression)
        let tokens = tokenizer.tokenize()

        // Parse and evaluate
        var parser = Parser(tokens: tokens, fieldValues: exprFieldValues)
        let result = parser.parseExpression()

        // Apply precision and format result
        return formatResult(result, precision: precision)
    }

    /// Format the result value to a string, applying precision if it's numeric.
    private static func formatResult(_ value: ExprValue, precision: Int?) -> String? {
        switch value {
        case .null:
            return nil
        case .number(let d):
            if let precision = precision {
                return String(format: "%.\(precision)f", d)
            }
            // Default: remove trailing zeros
            return d.truncatingRemainder(dividingBy: 1) == 0
                ? String(format: "%.0f", d)
                : String(d)
        case .string(let s):
            return s
        case .boolean(let b):
            return b ? "true" : "false"
        }
    }

    // MARK: - Batch Evaluation with Dependency Resolution

    /// Evaluate all calculated fields in dependency order (topological sort).
    /// Returns a dictionary of property UUID → computed value string.
    /// - additionalValues: extra name-keyed values (e.g. unit fields like "Problem Temp Unit" → "°F")
    ///   that get merged into the evaluation context but don't correspond to a property UUID.
    static func evaluateAllCalculatedFields<T: EntityPropertyDefinition>(
        properties: [T],
        currentValues: [UUID: String],
        additionalValues: [String: String] = [:]
    ) -> [UUID: String] {
        // Build lookups (trim names to handle API whitespace)
        let nameToProperty: [String: T] = Dictionary(
            properties.map { ($0.name.trimmingCharacters(in: .whitespaces), $0) },
            uniquingKeysWith: { first, _ in first }
        )
        let idToProperty: [UUID: T] = Dictionary(
            properties.map { ($0.id, $0) },
            uniquingKeysWith: { first, _ in first }
        )

        // Filter calculated properties
        let calculatedProps = properties.filter { $0.type == "calculated" && $0.calculationExpression != nil }
        guard !calculatedProps.isEmpty else { return [:] }

        // Build dependency graph for topological sort
        let calculatedNames = Set(calculatedProps.map { $0.name.trimmingCharacters(in: .whitespaces) })
        var inDegree: [UUID: Int] = [:]
        var dependents: [UUID: [UUID]] = [:]  // dependency -> [things that depend on it]

        for prop in calculatedProps {
            let refs = extractFieldReferences(from: prop.calculationExpression!)
            let calcDeps = refs.filter { calculatedNames.contains($0) }
            inDegree[prop.id] = calcDeps.count

            for depName in calcDeps {
                if let depProp = nameToProperty[depName] {
                    dependents[depProp.id, default: []].append(prop.id)
                }
            }
        }

        // Kahn's algorithm for topological sort
        var queue = calculatedProps.filter { (inDegree[$0.id] ?? 0) == 0 }.map { $0.id }
        var sorted: [UUID] = []

        while !queue.isEmpty {
            let current = queue.removeFirst()
            sorted.append(current)
            for dep in dependents[current] ?? [] {
                inDegree[dep] = (inDegree[dep] ?? 1) - 1
                if inDegree[dep] == 0 {
                    queue.append(dep)
                }
            }
        }

        // If any calculated props weren't sorted (circular dependency), add them anyway
        for prop in calculatedProps where !sorted.contains(prop.id) {
            sorted.append(prop.id)
        }

        // Evaluate in dependency order
        // Build a name→value map combining current form values and calculated results
        var nameValues: [String: String] = [:]
        for prop in properties {
            if let val = currentValues[prop.id], !val.isEmpty {
                nameValues[prop.name.trimmingCharacters(in: .whitespaces)] = val
            }
        }
        // Merge additional values (e.g. unit fields) into the evaluation context
        for (key, value) in additionalValues {
            nameValues[key] = value
        }

        var results: [UUID: String] = [:]

        for propId in sorted {
            guard let prop = idToProperty[propId],
                  let expression = prop.calculationExpression else { continue }

            let result = evaluate(
                expression: expression,
                fieldValues: nameValues,
                precision: prop.calculationPrecision
            )

            results[propId] = result ?? ""

            // Feed calculated result into nameValues for subsequent calculations
            if let result = result {
                nameValues[prop.name.trimmingCharacters(in: .whitespaces)] = result
            }
        }

        return results
    }
}
