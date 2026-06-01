//
//  AgentWebViewBridge.swift
//  Egalvanic PZ
//
//  Created by Eric Ehlert on 8/16/25.
//

import SwiftUI
import WebKit
import SwiftData

// MARK: - Agent-specific Scheme Handler
final class AgentSchemeHandler: NSObject, WKURLSchemeHandler {
    private let subfolderName = "Agent"

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        let reqURL = urlSchemeTask.request.url
        AppLogger.log(.debug, "[AgentSchemeHandler] start request for: \(reqURL?.absoluteString ?? "<nil>")", category: .webBridge)

        guard let requestURL = reqURL, requestURL.scheme == "agentapp" else {
            let err = NSError(domain: "AgentSchemeHandler", code: 400, userInfo: nil)
            AppLogger.log(.error, "[AgentSchemeHandler] invalid scheme for URL: \(String(describing: reqURL))", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Determine the requested file path:
        var path = requestURL.path.dropFirst()  // drop leading '/'
        if path.isEmpty, let host = requestURL.host {
            path = Substring(host)
        }
        AppLogger.log(.debug, "[AgentSchemeHandler] resolved path: \(path)", category: .webBridge)

        // Bundle root
        guard let bundleRoot = Bundle.main.resourceURL else {
            let err = NSError(domain: "AgentSchemeHandler", code: 500, userInfo: nil)
            AppLogger.log(.error, "[AgentSchemeHandler] bundle resourceURL missing", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Look in the Agent subfolder
        let fileURL = bundleRoot.appendingPathComponent(subfolderName).appendingPathComponent(String(path))
        AppLogger.log(.debug, "[AgentSchemeHandler] loading file URL: \(fileURL.path)", category: .webBridge)

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
            AppLogger.log(.debug, "[AgentSchemeHandler] serving \(fileURL.lastPathComponent) with mimeType=\(mimeType)", category: .webBridge)
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
        } catch {
            AppLogger.log(.error, "[AgentSchemeHandler] error loading data from Agent/\(path): \(error)", category: .webBridge)
            urlSchemeTask.didFailWithError(error)
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        AppLogger.log(.debug, "[AgentSchemeHandler] stop request", category: .webBridge)
    }
}

// MARK: - Agent WebView Bridge
struct AgentWebViewBridge: UIViewRepresentable {
    static weak var sharedWebView: WKWebView?

    let sldId: String

    init(sldId: String) {
        self.sldId = sldId
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()

        // Set up the custom URL scheme handler for Agent
        config.setURLSchemeHandler(AgentSchemeHandler(), forURLScheme: "agentapp")

        // Note: Not adding message handler since we don't need to receive messages back

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator

        // Store reference
        AgentWebViewBridge.sharedWebView = webView

        // Enable inspection for debugging
        webView.isInspectable = true

        // Load the index.html from Agent folder
        AppLogger.log(.debug, "[AgentWebViewBridge] Loading agentapp://index.html", category: .webBridge)
        if let url = URL(string: "agentapp://index.html") {
            webView.load(URLRequest(url: url))
        } else {
            AppLogger.log(.error, "[AgentWebViewBridge] Invalid URL for index.html", category: .webBridge)
        }

        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // Nothing to update
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(sldId: sldId)
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        let sldId: String

        init(sldId: String) {
            self.sldId = sldId
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            AppLogger.log(.info, "[AgentWebViewBridge] Page loaded successfully", category: .webBridge)

            // Escape the sldId for safe JavaScript injection
            let escapedSldId = sldId
                .replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "\"", with: "\\\"")
                .replacingOccurrences(of: "\n", with: "\\n")
                .replacingOccurrences(of: "\r", with: "\\r")
                .replacingOccurrences(of: "\t", with: "\\t")

            // Inject the SLD ID into the web app
            let js = """
                try {
                    // Set the SLD ID on window object
                    window.sldId = "\(escapedSldId)";
                    console.log('[AgentWebViewBridge] SLD ID set:', window.sldId);

                    // Dispatch event to notify the app that SLD ID is available
                    window.dispatchEvent(new CustomEvent('sldIdReady', {
                        detail: { sldId: window.sldId }
                    }));
                    console.log('[AgentWebViewBridge] Dispatched sldIdReady event');

                    true;
                } catch (e) {
                    console.error('[AgentWebViewBridge] Failed to set SLD ID:', e);
                    false;
                }
                """

            webView.evaluateJavaScript(js) { result, error in
                if let err = error {
                    AppLogger.log(.error, "[AgentWebViewBridge] Failed to inject SLD ID: \(err)", category: .webBridge)
                } else if let success = result as? Bool, success {
                    AppLogger.log(.info, "[AgentWebViewBridge] SLD ID injected successfully: \(self.sldId)", category: .webBridge)

                    // Re-dispatch the event after a delay to ensure the app is ready
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        let retryJs = """
                            if (window.sldId && !window.sldIdAlreadyDispatched) {
                                console.log('[AgentWebViewBridge] Re-dispatching sldIdReady event');
                                window.dispatchEvent(new CustomEvent('sldIdReady', {
                                    detail: { sldId: window.sldId }
                                }));
                                window.sldIdAlreadyDispatched = true;
                            }
                            """
                        webView.evaluateJavaScript(retryJs) { _, _ in
                            AppLogger.log(.debug, "[AgentWebViewBridge] Re-dispatched sldIdReady event", category: .webBridge)
                        }
                    }
                } else {
                    AppLogger.log(.notice, "[AgentWebViewBridge] SLD ID injection completed with warnings", category: .webBridge)
                }
            }
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            AppLogger.log(.error, "[AgentWebViewBridge] Navigation failed: \(error.localizedDescription)", category: .webBridge)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            AppLogger.log(.error, "[AgentWebViewBridge] Provisional navigation failed: \(error.localizedDescription)", category: .webBridge)
        }
    }
}
