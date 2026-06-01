//
//  FormWebView.swift
//  Egalvanic PZ
//
//  Created by Eric Ehlert on 8/3/25.
//


import SwiftUI
import WebKit
import SwiftData

// MARK: - Form-specific Scheme Handler
final class FormSchemeHandler: NSObject, WKURLSchemeHandler {
    private let subfolderName = "FormApp"

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        let reqURL = urlSchemeTask.request.url
        AppLogger.log(.debug, "[FormSchemeHandler] start request for: \(reqURL?.absoluteString ?? "<nil>")", category: .webBridge)

        guard let requestURL = reqURL, requestURL.scheme == "formapp" else {
            let err = NSError(domain: "FormSchemeHandler", code: 400, userInfo: nil)
            AppLogger.log(.error, "[FormSchemeHandler] invalid scheme for URL: \(String(describing: reqURL))", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Determine the requested file path:
        var path = requestURL.path.dropFirst()  // drop leading '/'
        if path.isEmpty, let host = requestURL.host {
            path = Substring(host)
        }
        AppLogger.log(.debug, "[FormSchemeHandler] resolved path: \(path)", category: .webBridge)

        // Bundle root
        guard let bundleRoot = Bundle.main.resourceURL else {
            let err = NSError(domain: "FormSchemeHandler", code: 500, userInfo: nil)
            AppLogger.log(.error, "[FormSchemeHandler] bundle resourceURL missing", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Look in the FormApp subfolder
        let fileURL = bundleRoot.appendingPathComponent(subfolderName).appendingPathComponent(String(path))
        AppLogger.log(.debug, "[FormSchemeHandler] loading file URL: \(fileURL.path)", category: .webBridge)

        do {
            let data = try Data(contentsOf: fileURL)
            let ext = fileURL.pathExtension.lowercased()
            let mimeType: String = {
                switch ext {
                case "html": return "text/html"
                case "js":   return "application/javascript"
                case "css":  return "text/css"
                case "svg":  return "image/svg+xml"
                case "json": return "application/json"
                case "woff": return "font/woff"
                case "woff2": return "font/woff2"
                case "ttf":  return "font/ttf"
                case "eot":  return "application/vnd.ms-fontobject"
                case "otf":  return "font/otf"
                default:     return "application/octet-stream"
                }
            }()

            let response = URLResponse(
                url: requestURL,
                mimeType: mimeType,
                expectedContentLength: data.count,
                textEncodingName: (ext == "html" || ext == "css" || ext == "js") ? "utf-8" : nil
            )
            AppLogger.log(.debug, "[FormSchemeHandler] serving \(fileURL.lastPathComponent) with mimeType=\(mimeType)", category: .webBridge)
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
        } catch {
            AppLogger.log(.error, "[FormSchemeHandler] error loading data from FormApp/\(path): \(error)", category: .webBridge)
            urlSchemeTask.didFailWithError(error)
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        AppLogger.log(.debug, "[FormSchemeHandler] stop request", category: .webBridge)
    }
}

// MARK: - Simple WebViewBridge
struct FormWebViewBridge: UIViewRepresentable {
   static weak var sharedWebView: WKWebView?

   let initialTask: UserTask
   let initialFormInstance: FormInstance
   let initialForm: UserTaskForm
   let onSubmit: () -> Void
   @Environment(\.modelContext) private var modelContext

   init(task: UserTask, formInstance: FormInstance, onSubmit: @escaping () -> Void = {}) {
       self.initialTask = task
       self.initialFormInstance = formInstance
       self.initialForm = formInstance.formMaster ?? UserTaskForm(
           id: formInstance.form_master_id,
           schema: "{}",
           title: "Unknown Form",
           is_global: false,
           is_deleted: false
       )
       self.onSubmit = onSubmit

       AppLogger.log(.debug, "[FormWebViewBridge] init - Task: \(task.id), FormInstance: \(formInstance.id), Form: \(initialForm.title), schema exists: \(!initialForm.schema.isEmpty), submission exists: \(formInstance.form_submission != nil), submitted: \(formInstance.submitted)", category: .webBridge)
   }

   static func injectForm(with schema: String) {
       guard let webView = sharedWebView else {
           AppLogger.log(.error, "[FormWebViewBridge] Failed to inject form: no shared web view found", category: .webBridge)
           return
       }
       let js = "window.injectForm(\(schema));"
       DispatchQueue.main.async {
           webView.evaluateJavaScript(js) { _, err in
               if let err = err {
                   AppLogger.log(.error, "[FormWebViewBridge] Failed to inject form: \(err.localizedDescription)", category: .webBridge)
               } else {
                   AppLogger.log(.debug, "[FormWebViewBridge] Form injected successfully", category: .webBridge)
               }
           }
       }
   }

   func makeUIView(context: Context) -> WKWebView {
       AppLogger.log(.debug, "[FormWebViewBridge] makeUIView - Creating WKWebView for form: \(initialForm.title)", category: .webBridge)

       let config = WKWebViewConfiguration()

       // IMPORTANT: Register the message handler for JavaScript communication
       config.userContentController.add(context.coordinator, name: "formBridge")

       // Set up the custom URL scheme handler
       config.setURLSchemeHandler(FormSchemeHandler(), forURLScheme: "formapp")

       let webView = WKWebView(frame: .zero, configuration: config)
       webView.navigationDelegate = context.coordinator

       // Store reference
       FormWebViewBridge.sharedWebView = webView

       // Enable inspection for debugging
       webView.isInspectable = true

       // Load the index.html
       AppLogger.log(.debug, "[FormWebViewBridge] Loading formapp://index.html", category: .webBridge)
       if let url = URL(string: "formapp://index.html") {
           webView.load(URLRequest(url: url))
       } else {
           AppLogger.log(.error, "[FormWebViewBridge] Invalid URL for index.html", category: .webBridge)
       }

       return webView
   }

   func updateUIView(_ uiView: WKWebView, context: Context) {
       // Nothing to update
   }

   func makeCoordinator() -> Coordinator {
       let coordinator = Coordinator(task: initialTask, formInstance: initialFormInstance, form: initialForm, onSubmit: onSubmit)
       coordinator.modelContext = modelContext
       return coordinator
   }

   // IMPORTANT: Add WKScriptMessageHandler conformance
   class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {

       let task: UserTask
       let formInstance: FormInstance
       let form: UserTaskForm
       let onSubmit: () -> Void
       var modelContext: ModelContext?

       init(task: UserTask, formInstance: FormInstance, form: UserTaskForm, onSubmit: @escaping () -> Void) {
           self.task = task
           self.formInstance = formInstance
           self.form = form
           self.onSubmit = onSubmit

           AppLogger.log(.debug, "[FormWebViewBridge.Coordinator] init - Task: \(task.id), FormInstance: \(formInstance.id), Form: \(form.id), schema first 100 chars: \(String(form.schema.prefix(100)))", category: .webBridge)
       }

       func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
           AppLogger.log(.info, "[FormWebViewBridge] Page loaded successfully", category: .webBridge)

           let escapedSchema = form.schema
               .replacingOccurrences(of: "\\", with: "\\\\")
               .replacingOccurrences(of: "\"", with: "\\\"")
               .replacingOccurrences(of: "\n", with: "\\n")
               .replacingOccurrences(of: "\r", with: "\\r")
               .replacingOccurrences(of: "\t", with: "\\t")

           // Prepare submission data if it exists from FormInstance
           let submissionJS: String
           if let submission = formInstance.form_submission {
               let escapedSubmission = submission
                   .replacingOccurrences(of: "\\", with: "\\\\")
                   .replacingOccurrences(of: "\"", with: "\\\"")
                   .replacingOccurrences(of: "\n", with: "\\n")
                   .replacingOccurrences(of: "\r", with: "\\r")
                   .replacingOccurrences(of: "\t", with: "\\t")
               submissionJS = "JSON.parse(\"\(escapedSubmission)\")"
           } else {
               submissionJS = "null"
           }

           let js = """
               try {
                   const schemaData = JSON.parse("\(escapedSchema)");
                   const submissionData = \(submissionJS);

                   console.log('[SimpleWebViewBridge] Schema loaded:', {
                       size: JSON.stringify(schemaData).length,
                       keys: Object.keys(schemaData),
                       type: typeof schemaData
                   });

                   if (submissionData) {
                       console.log('[SimpleWebViewBridge] Existing submission loaded:', {
                           size: JSON.stringify(submissionData).length,
                           type: typeof submissionData
                       });
                   }

                   // IMPORTANT: Set both on window so React can find them
                   window.formSchema = schemaData;
                   window.formSubmission = submissionData;
                   console.log('[SimpleWebViewBridge] Set window.formSchema and window.formSubmission');

                   // Dispatch the event with both schema and submission
                   window.dispatchEvent(new CustomEvent('initialData', {
                       detail: {
                           schema: schemaData,
                           submission: submissionData
                       }
                   }));
                   console.log('[SimpleWebViewBridge] Dispatched initialData event');

                   true;
               } catch (e) {
                   console.error('[SimpleWebViewBridge] Failed:', e);
                   false;
               }
               """

           webView.evaluateJavaScript(js) { result, error in
               if let err = error {
                   AppLogger.log(.error, "[FormWebViewBridge] Failed to inject data: \(err)", category: .webBridge)
               } else if let success = result as? Bool, success {
                   AppLogger.log(.info, "[FormWebViewBridge] Schema and submission injected successfully", category: .webBridge)

                   // Dispatch the event again after a delay to ensure React is ready
                   DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                       let retryJs = """
                           if (window.formSchema && !window.schemaAlreadyDispatched) {
                               console.log('[SimpleWebViewBridge] Re-dispatching initialData event');
                               window.dispatchEvent(new CustomEvent('initialData', {
                                   detail: {
                                       schema: window.formSchema,
                                       submission: window.formSubmission
                                   }
                               }));
                               window.schemaAlreadyDispatched = true;
                           }
                           """
                       webView.evaluateJavaScript(retryJs) { _, _ in
                           AppLogger.log(.debug, "[FormWebViewBridge] Re-dispatched data event", category: .webBridge)
                       }
                   }
               } else {
                   AppLogger.log(.notice, "[FormWebViewBridge] Data injection completed with warnings", category: .webBridge)
               }
           }
       }

       // Handle messages from JavaScript
       func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
           AppLogger.log(.debug, "[FormWebViewBridge] Received message: \(message.name) - \(message.body)", category: .webBridge)

           // Handle the "formBridge" message channel
           if message.name == "formBridge" {
               guard let body = message.body as? [String: Any] else {
                   AppLogger.log(.notice, "[FormWebViewBridge] Malformed message body", category: .webBridge)
                   return
               }

               // Check the action type
               if let action = body["action"] as? String {
                   switch action {
                   case "formSubmit":
                       if let formData = body["data"] {
                           // Convert to JSON string if needed
                           if let jsonData = try? JSONSerialization.data(withJSONObject: formData),
                              let jsonString = String(data: jsonData, encoding: .utf8) {
                               AppLogger.log(.info, "[FormWebViewBridge] Form submitted with data length: \(jsonString.count)", category: .form)

                               // Check if we have the required dependencies
                               guard let modelContext = modelContext else {
                                   AppLogger.log(.notice, "[FormWebViewBridge] Missing modelContext for FormInstanceService", category: .form)
                                   return
                               }

                               // Update the FormInstance with the form submission
                               Task { @MainActor in
                                   FormInstanceService.updateFormInstance(
                                       formInstance,
                                       submission: jsonString,
                                       submitted: true,
                                       modelContext: modelContext,
                                       onCompletion: { success, message in
                                           if success {
                                               AppLogger.log(.info, "[FormWebViewBridge] Form submitted and FormInstance updated successfully", category: .form)
                                               if let message = message {
                                                   AppLogger.log(.debug, "[FormWebViewBridge] \(message)", category: .form)
                                               }

                                               // Dismiss the webview after successful submission
                                               DispatchQueue.main.async {
                                                   self.onSubmit()
                                               }
                                           } else {
                                               AppLogger.log(.error, "[FormWebViewBridge] Failed to update FormInstance: \(message ?? "Unknown error")", category: .form)
                                           }
                                       }
                                   )
                               }
                           }
                       }

                   case "formUpdate":
                       if let formData = body["data"] {
                           AppLogger.log(.debug, "[FormWebViewBridge] Form updated with data: \(formData)", category: .form)
                           // Auto-save drafts to FormInstance
                           if let jsonData = try? JSONSerialization.data(withJSONObject: formData),
                              let jsonString = String(data: jsonData, encoding: .utf8),
                              let modelContext = modelContext {
                               FormInstanceService.updateFormInstanceDraft(
                                   formInstance,
                                   submission: jsonString,
                                   modelContext: modelContext
                               )
                           }
                       }

                   case "schemaLoaded":
                       AppLogger.log(.debug, "[FormWebViewBridge] React app confirmed schema loaded", category: .webBridge)

                   default:
                       AppLogger.log(.notice, "[FormWebViewBridge] Unknown action: \(action)", category: .webBridge)
                   }
               }
           }
       }
   }
}
