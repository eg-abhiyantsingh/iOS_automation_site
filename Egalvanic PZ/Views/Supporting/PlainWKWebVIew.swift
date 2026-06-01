import SwiftUI
import WebKit

/// Logs requests and serves bundle-root files via `app://` URLs,
/// with fallback to host as path when URL.path is empty.
final class AppRootSchemeHandler: NSObject, WKURLSchemeHandler {
    private let subfolderName = "Flow"
    
    func webView(
        _ webView: WKWebView,
        start urlSchemeTask: WKURLSchemeTask
    ) {
        let reqURL = urlSchemeTask.request.url
        AppLogger.log(.debug, "[SchemeHandler] start request for: \(reqURL?.absoluteString ?? "<nil>")", category: .webBridge)
        guard
            let requestURL = reqURL,
            requestURL.scheme == "app"
        else {
            let err = NSError(domain: "AppRootSchemeHandler", code: 400, userInfo: nil)
            AppLogger.log(.error, "[SchemeHandler] invalid scheme for URL: \(String(describing: reqURL))", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Determine the requested file path:
        var path = requestURL.path.dropFirst()  // drop leading '/'
        if path.isEmpty, let host = requestURL.host {
            // URLs like app://index.html have host set, path empty
            path = Substring(host)
        }
        AppLogger.log(.debug, "[SchemeHandler] resolved path: \(path)", category: .webBridge)

        // Bundle root
        guard let bundleRoot = Bundle.main.resourceURL else {
            let err = NSError(domain: "AppRootSchemeHandler", code: 500, userInfo: nil)
            AppLogger.log(.error, "[SchemeHandler] bundle resourceURL missing", category: .webBridge)
            urlSchemeTask.didFailWithError(err)
            return
        }

        // Check downloaded assets first, fall back to bundled Flow/ directory
        let fileURL: URL
        if let downloadedDir = SldViewerAssetManager.shared.getDownloadedAssetsDir() {
            let downloadedFile = downloadedDir.appendingPathComponent(String(path))
            if FileManager.default.fileExists(atPath: downloadedFile.path) {
                fileURL = downloadedFile
                AppLogger.log(.debug, "[SchemeHandler] serving from downloaded assets: \(path)", category: .webBridge)
            } else {
                fileURL = bundleRoot.appendingPathComponent(subfolderName).appendingPathComponent(String(path))
                AppLogger.log(.debug, "[SchemeHandler] file not in downloaded assets, falling back to bundle: \(path)", category: .webBridge)
            }
        } else {
            fileURL = bundleRoot.appendingPathComponent(subfolderName).appendingPathComponent(String(path))
        }
        AppLogger.log(.debug, "[SchemeHandler] loading file URL: \(fileURL.path)", category: .webBridge)

        do {
                    let data = try Data(contentsOf: fileURL)
                    let ext = fileURL.pathExtension.lowercased()
                    let mimeType: String = {
                        switch ext {
                        case "html": return "text/html"
                        case "js":   return "application/javascript"
                        case "css":  return "text/css"
                        case "svg":  return "image/svg+xml"
                        case "png":  return "image/png"
                        case "jpg", "jpeg": return "image/jpeg"
                        case "gif":  return "image/gif"
                        case "webp": return "image/webp"
                        case "ico":  return "image/x-icon"
                        case "woff": return "font/woff"
                        case "woff2": return "font/woff2"
                        case "ttf":  return "font/ttf"
                        case "eot":  return "application/vnd.ms-fontobject"
                        case "otf":  return "font/otf"
                        case "json": return "application/json"
                        default:     return "application/octet-stream"
                        }
                    }()
                    let response = URLResponse(
                        url: requestURL,
                        mimeType: mimeType,
                        expectedContentLength: data.count,
                        textEncodingName: ext == "html" ? "utf-8" : nil
                    )
                    AppLogger.log(.debug, "[SchemeHandler] serving \(path) with mimeType=\(mimeType)", category: .webBridge)
                    urlSchemeTask.didReceive(response)
                    urlSchemeTask.didReceive(data)
                    urlSchemeTask.didFinish()
                }catch {
            AppLogger.log(.error, "[SchemeHandler] error loading data: \(error)", category: .webBridge)
            urlSchemeTask.didFailWithError(error)
        }
    }

    func webView(
        _ webView: WKWebView,
        stop urlSchemeTask: WKURLSchemeTask
    ) {
        AppLogger.log(.debug, "[SchemeHandler] stop request for: \(urlSchemeTask.request.url?.absoluteString ?? "<nil>")", category: .webBridge)
    }
}

/// A SwiftUI wrapper that embeds a WKWebView configured with our custom `app://` scheme
/// and logs its lifecycle.
struct SchemeWebView: UIViewRepresentable {
    func makeUIView(context: Context) -> WKWebView {
        AppLogger.log(.debug, "[SchemeWebView] makeUIView start", category: .webBridge)
        let pagePrefs = WKWebpagePreferences()
        pagePrefs.allowsContentJavaScript = true

        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences = pagePrefs
        config.setURLSchemeHandler(AppRootSchemeHandler(), forURLScheme: "app")

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.isInspectable = true
        webView.backgroundColor = .clear

        if let entry = URL(string: "app://index.html") {
            AppLogger.log(.debug, "[SchemeWebView] loading URL: \(entry)", category: .webBridge)
            webView.load(URLRequest(url: entry))
        } else {
            AppLogger.log(.error, "[SchemeWebView] invalid entry URL", category: .webBridge)
        }

        AppLogger.log(.debug, "[SchemeWebView] makeUIView complete", category: .webBridge)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        AppLogger.log(.debug, "[SchemeWebView] updateUIView called", category: .webBridge)
    }
}
