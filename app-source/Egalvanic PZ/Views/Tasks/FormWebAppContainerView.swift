//
//  FormWebView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/3/25.
//

import SwiftUI

// MARK: - FormWebAppContainerView
struct FormWebAppContainerView: View {

    private var task: UserTask
    private var formInstance: FormInstance
    private var onSubmit: () -> Void

    init(task: UserTask, formInstance: FormInstance, onSubmit: @escaping () -> Void = {}) {
        self.task = task
        self.formInstance = formInstance
        self.onSubmit = onSubmit

        AppLogger.log(.debug, "[FormWebAppContainerView] init - Task: \(task.id) '\(task.title)', FormInstance: \(formInstance.id), FormMaster: \(formInstance.form_master_id), FormTitle: \(formInstance.formMaster?.title ?? "nil"), SchemaLength: \(formInstance.formMaster?.schema.count ?? 0), hasSubmission: \(formInstance.form_submission != nil), submitted: \(formInstance.submitted)", category: .form)
    }

    var body: some View {
        FormWebViewBridge(task: task, formInstance: formInstance, onSubmit: onSubmit)
            .onAppear {
                AppLogger.log(.debug, "[FormWebAppContainerView] body appeared - task: \(task.id), formInstance: \(formInstance.id)", category: .form)
            }
    }
}
