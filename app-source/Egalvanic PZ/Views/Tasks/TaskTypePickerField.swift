//
//  TaskTypePickerField.swift
//  Egalvanic PZ
//
//  Sheet-style picker for the task_type enum. Stores the raw backend
//  code (e.g. "PM", "INSPECTION") in the field-value storage so the
//  creation flow can hand it straight to UserTaskDTO.task_type.
//
import SwiftUI

struct TaskTypePickerField: View {
    let field: EntityFieldConfiguration
    @ObservedObject var storage: StandardFieldValueStorage

    @State private var showSheet = false

    private var currentRaw: String? {
        storage.getValue(for: field.id) as? String
    }

    private var currentType: TaskType? {
        TaskType.from(rawValue: currentRaw)
    }

    var body: some View {
        HStack {
            if let icon = field.icon {
                Image(systemName: icon)
                    .foregroundColor(.secondary)
                    .frame(width: 20)
            }

            Text(currentType?.displayName ?? AppStrings.Tasks.selectTaskType)
                .foregroundColor(currentType != nil ? .primary : .secondary)

            Spacer()

            Image(systemName: "chevron.down")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemGray6))
        .cornerRadius(8)
        .contentShape(Rectangle())
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            showSheet = true
        }
        .onAppear {
            // Mirror the frontend default — preselect PM the first time
            // the form renders so the user can save without touching
            // the picker explicitly.
            if currentRaw == nil {
                storage.setValue(TaskType.pm.rawValue, for: field.id)
            }
        }
        .sheet(isPresented: $showSheet) {
            NavigationStack {
                List {
                    ForEach(TaskType.allCases) { option in
                        Button {
                            storage.setValue(option.rawValue, for: field.id)
                            showSheet = false
                        } label: {
                            HStack {
                                Text(option.displayName)
                                    .foregroundColor(.primary)
                                Spacer()
                                if currentType == option {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                .navigationTitle(field.label)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(AppStrings.Common.cancel) { showSheet = false }
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }
}
