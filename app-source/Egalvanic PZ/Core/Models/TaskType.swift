//
//  TaskType.swift
//  Egalvanic PZ
//
//  Matches the hard-coded set used by the web client's TaskCreationForm.
//  Raw values are the backend codes; `displayName` is the localized label
//  shown in the picker / detail view.
//
import Foundation

enum TaskType: String, CaseIterable, Identifiable, Codable {
    case pm = "PM"
    case inspection = "INSPECTION"
    case repair = "REPAIR"
    case test = "TEST"
    case documentation = "DOCUMENTATION"
    case other = "OTHER"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .pm:            return AppStrings.Tasks.taskTypePM
        case .inspection:    return AppStrings.Tasks.taskTypeInspection
        case .repair:        return AppStrings.Tasks.taskTypeRepair
        case .test:          return AppStrings.Tasks.taskTypeTest
        case .documentation: return AppStrings.Tasks.taskTypeDocumentation
        case .other:         return AppStrings.Tasks.taskTypeOther
        }
    }

    /// Resolve a stored task_type string back to the enum. Tolerant of
    /// historical lowercase / mixed-case values; falls back to nil if
    /// the value isn't in the known set (display falls back to raw).
    static func from(rawValue: String?) -> TaskType? {
        guard let raw = rawValue?.trimmingCharacters(in: .whitespacesAndNewlines),
              !raw.isEmpty else { return nil }
        return TaskType(rawValue: raw.uppercased())
    }
}
