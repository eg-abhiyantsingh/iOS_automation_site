import SwiftUI
import SwiftData

// MARK: - Field Types
enum EntityFieldType {
    case text(placeholder: String? = nil)
    case textArea(placeholder: String? = nil, minHeight: CGFloat = 100)
    case date(includeTime: Bool = false)
    case picker(options: [PickerOption])
    case nodeV2Picker
    case nodeV2MultiPicker  // New: Multiple node selection
    case issueClassPicker
    case userTaskFormPicker
    case userTaskFormMultiPicker  // New: Multiple form selection
    case edgeV2Picker
    case sldV2Picker
    case userTaskPicker
    case issuePicker
    case irPhotoPicker
    case irSessionPicker
    case toggle
    case number(formatter: NumberFormatter? = nil)
    case custom(view: AnyView)
    // Task creation specific
    case taskTypePicker
    case procedurePicker
}

// MARK: - Field Configuration
struct EntityFieldConfiguration {
    let id: String
    let label: String
    let icon: String?
    let type: EntityFieldType
    let isRequired: Bool
    let section: String?
    let helpText: String?
    let validationRules: [ValidationRule]
    var metadata: [String: Any] = [:]  // For passing additional context

    init(
        id: String,
        label: String,
        icon: String? = nil,
        type: EntityFieldType,
        isRequired: Bool = false,
        section: String? = nil,
        helpText: String? = nil,
        validationRules: [ValidationRule] = [],
        metadata: [String: Any] = [:]
    ) {
        self.id = id
        self.label = label
        self.icon = icon
        self.type = type
        self.isRequired = isRequired
        self.section = section
        self.helpText = helpText
        self.validationRules = validationRules
        self.metadata = metadata
    }
}

// MARK: - Picker Option
struct PickerOption: Identifiable, Hashable {
    let id: String
    let displayName: String
    let icon: String?
    let color: Color?
    
    init(id: String, displayName: String, icon: String? = nil, color: Color? = nil) {
        self.id = id
        self.displayName = displayName
        self.icon = icon
        self.color = color
    }
}

// MARK: - Validation
struct ValidationRule {
    let validate: (Any?) -> ValidationResult
}

enum ValidationResult {
    case valid
    case invalid(String)
}

// MARK: - Field Value Storage
protocol FieldValueStorage {
    func getValue(for fieldId: String) -> Any?
    func setValue(_ value: Any?, for fieldId: String)
    func clearValue(for fieldId: String)
    func validate(fieldId: String, rules: [ValidationRule]) -> ValidationResult
}

// MARK: - Entity Creation Configuration Protocol
protocol EntityCreationConfiguration {
    associatedtype Entity
    
    // Basic Info
    var entityName: String { get }
    var navigationTitle: String { get }
    var createButtonTitle: String { get }
    
    // Field Configuration
    func fields() -> [EntityFieldConfiguration]
    func sections() -> [String]?
    
    // SwiftData Model Relationships
    func swiftDataModels() -> [any PersistentModel.Type]
    
    // Validation
    func validateForm(storage: FieldValueStorage) -> ValidationResult
    func canSave(storage: FieldValueStorage) -> Bool
    
    // Actions
    @MainActor
    func onCreate(storage: FieldValueStorage, modelContext: ModelContext) -> Entity?
    func onCancel()
    
    // Optional: Context Display
    func contextView() -> AnyView?
}

// MARK: - Default Implementations
extension EntityCreationConfiguration {
    var navigationTitle: String { "New \(entityName)" }
    var createButtonTitle: String { "Create" }
    
    func sections() -> [String]? { nil }
    
    func swiftDataModels() -> [any PersistentModel.Type] { [] }
    
    func validateForm(storage: FieldValueStorage) -> ValidationResult {
        let fields = self.fields()
        
        for field in fields {
            if field.isRequired {
                let value = storage.getValue(for: field.id)
                
                switch field.type {
                case .text, .textArea:
                    if let stringValue = value as? String,
                       stringValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        return .invalid(AppStrings.Forms.fieldIsRequired(field.label))
                    }
                case .nodeV2Picker, .issueClassPicker, .userTaskFormPicker,
                     .edgeV2Picker, .sldV2Picker, .userTaskPicker,
                     .issuePicker, .irPhotoPicker, .irSessionPicker,
                     .procedurePicker:
                    if value == nil {
                        return .invalid(AppStrings.Forms.fieldIsRequired(field.label))
                    }
                case .picker, .taskTypePicker:
                    if value == nil {
                        return .invalid(AppStrings.Forms.fieldIsRequired(field.label))
                    }
                default:
                    break
                }
            }
            
            // Run custom validation rules
            let result = storage.validate(fieldId: field.id, rules: field.validationRules)
            if case .invalid = result {
                return result
            }
        }
        
        return .valid
    }
    
    func canSave(storage: FieldValueStorage) -> Bool {
        if case .valid = validateForm(storage: storage) {
            return true
        }
        return false
    }
    
    func contextView() -> AnyView? { nil }
}

// MARK: - Standard Field Value Storage Implementation
class StandardFieldValueStorage: ObservableObject, FieldValueStorage {
    @Published private var values: [String: Any] = [:]
    
    func getValue(for fieldId: String) -> Any? {
        return values[fieldId]
    }
    
    func setValue(_ value: Any?, for fieldId: String) {
        values[fieldId] = value
        objectWillChange.send()
    }
    
    func clearValue(for fieldId: String) {
        values.removeValue(forKey: fieldId)
        objectWillChange.send()
    }
    
    func validate(fieldId: String, rules: [ValidationRule]) -> ValidationResult {
        let value = getValue(for: fieldId)
        
        for rule in rules {
            let result = rule.validate(value)
            if case .invalid = result {
                return result
            }
        }
        
        return .valid
    }
    
    func reset() {
        values.removeAll()
        objectWillChange.send()
    }
}

// MARK: - Helper Extensions for SwiftData Picker Support
protocol SwiftDataPickable: PersistentModel {
    var displayName: String { get }
}

// Extension to help with common SwiftData models
extension NodeV2: SwiftDataPickable {
    var displayName: String {
        if let location = location {
            return "\(label) - \(location)"
        }
        return label
    }
}

extension IssueClass: SwiftDataPickable {
    var displayName: String {
        return name
    }
}

extension UserTaskForm: SwiftDataPickable {
    var displayName: String {
        return title
    }
}

extension EdgeV2: SwiftDataPickable {
    var displayName: String {
        if let edgeClass = edge_class {
            return "Edge: \(edgeClass.name)"
        }
        return "Edge \(id.uuidString.prefix(8))"
    }
}

extension SLDV2: SwiftDataPickable {
    var displayName: String {
        return name
    }
}

extension UserTask: SwiftDataPickable {
    var displayName: String {
        return title
    }
}

extension Issue: SwiftDataPickable {
    var displayName: String {
        return title ?? "Untitled Issue"
    }
}

extension IRPhoto: SwiftDataPickable {
    var displayName: String {
        return "IR Photo: \(node.label)"
    }
}

extension IRSession: SwiftDataPickable {
    var displayName: String {
        return name
    }
}
