import SwiftUI
import WebKit
import Foundation
import SwiftData


// MARK: - WebViewBridge
struct WebViewBridge: UIViewRepresentable {
    static weak var sharedWebView: WKWebView?

    let initialNodes: [NodeV2]
    let initialEdges: [EdgeV2]
    let initialNodeClasses: [NodeClass]
    let initialEdgeClasses: [EdgeClass]

    // View-specific data (optional, only used when displaying a specific view)
    var activeViewId: UUID? = nil
    var sldLinks: [SLDLinkV2] = []
    var nodePositionOverrides: [UUID: (x: Double, y: Double, width: Double?, height: Double?, isCollapsed: Bool)] = [:]
    var edgeRoutingOverrides: [UUID: (points: [EdgePoint]?, algorithm: String?)] = [:]

    var onNodeClicked: (UUID, String) -> Void
    var onEdgeClicked: (UUID) -> Void
    var onNodePositionChanged: (UUID, Double, Double) -> Void
    var onNodeParentChanged: (UUID, UUID?, Double, Double) -> Void
    var onNodeCreated: (UUID, String, String, UUID?, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void
    var onEnclosureCreated: (UUID, String, String, Double, Double, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void
    var onEnclosureSizeUpdated: (UUID, Double, Double) -> Void
    var onNodeRemovedFromParent: (UUID, Double, Double) -> Void
    var onNodeDeleted: (UUID) -> Void
    var onNodeTypeChanged: (UUID, String, Double?, Double?) -> Void
    var onEdgeCreated: (UUID, UUID, UUID, UUID?, UUID?, [EdgePoint]?, String?) -> Void  // id, source, target, sourceTerminalId, targetTerminalId, points, algorithm
    var onEdgeDeleted: (UUID) -> Void
    var onEdgeUpdated: (UUID, [EdgePoint]?, String?) -> Void

    // View navigation callbacks (optional)
    var onViewBack: (() -> Void)? = nil
    var onNavigateToView: ((UUID) -> Void)? = nil

    // Collapse state callback (optional, used for view-specific collapse state)
    var onNodeCollapseStateChanged: ((UUID, Bool) -> Void)? = nil

    static func updateGraph(with dto: SLDDTO, animated: Bool = true) {
        guard let webView = sharedWebView else {
            AppLogger.log(.notice, "[WebViewBridge] updateGraph: no webView available", category: .webBridge)
            return
        }
        guard let data = try? JSONEncoder().encode(dto),
              let json = String(data: data, encoding: .utf8) else {
            AppLogger.log(.error, "[WebViewBridge] updateGraph: failed to JSON-encode DTO", category: .webBridge)
            return
        }
        let js = "window.updateData(\(json), \(animated));"
        DispatchQueue.main.async {
            webView.evaluateJavaScript(js) { _, err in
                if let err = err {
                    AppLogger.log(.error, "[WebViewBridge] updateGraph: failed to evaluate JavaScript: \(err)", category: .webBridge)
                } else {
                    AppLogger.log(.debug, "[WebViewBridge] updateGraph: JavaScript executed successfully", category: .webBridge)
                }
            }
        }
    }

    // MARK: - ZP-PLATFORM-56: Pre-compute initial data as JSON
    // Converts @Model objects to serialized DTOs immediately during view setup,
    // so the Coordinator never holds live SwiftData model references.
    // This prevents use-after-free / double-free crashes when SwiftData
    // invalidates model objects after context.save().
    private func precomputeInitialData() -> (sldJSON: String, nodeClassJSON: String, edgeClassJSON: String, viewDataJS: String, activeViewIdForJS: String) {
        let sldId = initialNodes.first?.sld?.id ?? initialEdges.first?.sld?.id ?? UUID()

        let dto: SLDDTO
        if !nodePositionOverrides.isEmpty || !edgeRoutingOverrides.isEmpty {
            dto = SLDService.shared.createDTO(
                forSLDId: sldId,
                nodes: initialNodes,
                edges: initialEdges,
                customName: "view-specific-bridge-initial",
                nodePositionOverrides: nodePositionOverrides,
                edgeRoutingOverrides: edgeRoutingOverrides
            )
        } else {
            dto = SLDService.shared.createDTO(
                forSLDId: sldId,
                nodes: initialNodes,
                edges: initialEdges,
                customName: "updated-bridge-initial"
            )
        }
        let sldJSON: String
        if let data = try? JSONEncoder().encode(dto),
           let json = String(data: data, encoding: .utf8) {
            sldJSON = json
        } else {
            AppLogger.log(.error, "[WebViewBridge] SLD JSON encoding failed during precompute", category: .webBridge)
            sldJSON = "{\"nodes\":[],\"edges\":[]}"
        }

        // Build NodeClassDTOs
        var nodeClassDtos: [NodeClassDTO] = []
        for nodeClass in initialNodeClasses {
            let orientationDTO = nodeClass.node_orientation?.toDTO()
            var nc = NodeClassDTO(
                id: nodeClass.id,
                name: nodeClass.name,
                style: nodeClass.style,
                box: nodeClass.box,
                definition: [],
                ocp: nodeClass.ocp,
                width: nodeClass.width,
                height: nodeClass.height,
                color: nodeClass.color,
                needs_source: nodeClass.needs_source,
                is_deleted: nodeClass.is_deleted,
                node_orientation: orientationDTO,
                node_orientation_id: nodeClass.node_orientation_id,
                orientation: nodeClass.orientation,
                in_ports: nodeClass.in_ports,
                out_ports: nodeClass.out_ports,
                icon_id: nodeClass.icon_id,
                company_id: nodeClass.company_id,
                is_global: nodeClass.is_global,
                is_override: nodeClass.is_override,
                for_entity: nodeClass.for_entity,
                skm_config: nodeClass.skm_config?.toDTO(),
                default_datablock_config: nodeClass.default_datablock_config,
                use_eqp_lib: nodeClass.use_eqp_lib,
                primary_secondary_voltage: nodeClass.primary_secondary_voltage
            )
            // ZP-2156: Engineering-section drivers the FE needs to render
            // the correct Add Asset sections (device_role_code, eqp_lib_type,
            // pseudo-edge / impedance / bus / panel-schedule flags, etc.).
            nc.tertiary_voltage = nodeClass.tertiary_voltage
            nc.eqp_lib_type_id = nodeClass.eqp_lib_type_id
            nc.eqp_lib_type_name = nodeClass.eqp_lib_type_name
            nc.device_role_id = nodeClass.device_role_id
            nc.device_role_code = nodeClass.device_role_code
            nc.is_pseudo_edge = nodeClass.is_pseudo_edge
            nc.is_impedance = nodeClass.is_impedance
            nc.is_default_impedance = nodeClass.is_default_impedance
            nc.is_node_bus = nodeClass.is_node_bus
            nc.has_panel_schedule = nodeClass.has_panel_schedule
            nc.description = nodeClass.class_description
            nodeClassDtos.append(nc)
        }
        let nodeClassJSON: String
        if let data = try? JSONEncoder().encode(nodeClassDtos),
           let json = String(data: data, encoding: .utf8) {
            nodeClassJSON = json
        } else {
            AppLogger.log(.error, "[WebViewBridge] NodeClass JSON encoding failed during precompute", category: .webBridge)
            nodeClassJSON = "[]"
        }

        // Build EdgeClassDTOs
        var edgeClassDtos: [EdgeClassDTO] = []
        for edgeClass in initialEdgeClasses {
            let ec = EdgeClassDTO(
                id: edgeClass.id,
                name: edgeClass.name,
                definition: [],
                is_deleted: edgeClass.is_deleted
            )
            edgeClassDtos.append(ec)
        }
        let edgeClassJSON: String
        if let data = try? JSONEncoder().encode(edgeClassDtos),
           let json = String(data: data, encoding: .utf8) {
            edgeClassJSON = json
        } else {
            AppLogger.log(.error, "[WebViewBridge] EdgeClass JSON encoding failed during precompute", category: .webBridge)
            edgeClassJSON = "[]"
        }

        // Build view-specific data
        var viewDataJS = "null"
        var activeViewIdForJS = "\"all\""

        if let viewId = activeViewId {
            activeViewIdForJS = "\"\(viewId.uuidString)\""

            var linkDtos: [[String: Any]] = []
            for link in sldLinks {
                var linkDto: [String: Any] = [
                    "id": link.id.uuidString,
                    "source_sld_view_id": link.source_sld_view_id.uuidString,
                    "target_sld_view_id": link.target_sld_view_id.uuidString,
                    "is_deleted": link.is_deleted
                ]
                if let sourceNodeId = link.source_node_id { linkDto["source_node_id"] = sourceNodeId.uuidString }
                if let targetNodeId = link.target_node_id { linkDto["target_node_id"] = targetNodeId.uuidString }
                if let sourceX = link.source_x { linkDto["source_x"] = sourceX }
                if let sourceY = link.source_y { linkDto["source_y"] = sourceY }
                if let targetX = link.target_x { linkDto["target_x"] = targetX }
                if let targetY = link.target_y { linkDto["target_y"] = targetY }
                if let edgeDirection = link.edge_direction { linkDto["edge_direction"] = edgeDirection }
                if let targetViewName = link.target_view_name { linkDto["target_view_name"] = targetViewName }
                if let targetNodeLabel = link.target_node_label { linkDto["target_node_label"] = targetNodeLabel }
                linkDtos.append(linkDto)
            }

            var nodeOverridesDict: [String: [String: Any]] = [:]
            for (nodeId, override) in nodePositionOverrides {
                var overrideData: [String: Any] = ["x": override.x, "y": override.y, "isCollapsed": override.isCollapsed]
                if let width = override.width { overrideData["width"] = width }
                if let height = override.height { overrideData["height"] = height }
                nodeOverridesDict[nodeId.uuidString] = overrideData
            }

            var edgeOverridesDict: [String: [String: Any]] = [:]
            for (edgeId, override) in edgeRoutingOverrides {
                var overrideData: [String: Any] = [:]
                if let points = override.points {
                    overrideData["points"] = points.map { ["x": $0.x, "y": $0.y, "id": $0.id, "active": $0.active] }
                }
                if let algorithm = override.algorithm { overrideData["algorithm"] = algorithm }
                edgeOverridesDict[edgeId.uuidString] = overrideData
            }

            let viewData: [String: Any] = [
                "activeViewId": viewId.uuidString,
                "sldLinks": linkDtos,
                "nodePositionOverrides": nodeOverridesDict,
                "edgeRoutingOverrides": edgeOverridesDict
            ]

            if let viewDataJson = try? JSONSerialization.data(withJSONObject: viewData),
               let viewDataString = String(data: viewDataJson, encoding: .utf8) {
                viewDataJS = viewDataString
            }
        }

        return (sldJSON, nodeClassJSON, edgeClassJSON, viewDataJS, activeViewIdForJS)
    }

    func makeCoordinator() -> Coordinator {
        let data = precomputeInitialData()
        return Coordinator(
            initialSLDJSON: data.sldJSON,
            initialNodeClassJSON: data.nodeClassJSON,
            initialEdgeClassJSON: data.edgeClassJSON,
            initialViewDataJS: data.viewDataJS,
            initialActiveViewIdForJS: data.activeViewIdForJS,
            initialNodeCount: initialNodes.count,
            initialEdgeCount: initialEdges.count,
            onNodeClicked: onNodeClicked,
            onEdgeClicked: onEdgeClicked,
            onNodePositionChanged: onNodePositionChanged,
            onNodeParentChanged: onNodeParentChanged,
            onNodeCreated: onNodeCreated,
            onEnclosureCreated: onEnclosureCreated,
            onEnclosureSizeUpdated: onEnclosureSizeUpdated,
            onNodeRemovedFromParent: onNodeRemovedFromParent,
            onNodeDeleted: onNodeDeleted,
            onNodeTypeChanged: onNodeTypeChanged,
            onEdgeCreated: onEdgeCreated,
            onEdgeDeleted: onEdgeDeleted,
            onEdgeUpdated: onEdgeUpdated,
            onViewBack: onViewBack,
            onNavigateToView: onNavigateToView,
            onNodeCollapseStateChanged: onNodeCollapseStateChanged
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        AppLogger.log(.debug, "[WebViewBridge] makeUIView called", category: .webBridge)

        let config = WKWebViewConfiguration()

        config.setURLSchemeHandler(AppRootSchemeHandler(), forURLScheme: "app")

        let uc = config.userContentController

        // MARK: - Handler Registration
        uc.add(context.coordinator, name: "nodeClicked")
        uc.add(context.coordinator, name: "nodePositionChanged")
        uc.add(context.coordinator, name: "nodeParentChanged")
        uc.add(context.coordinator, name: "graphUpdate")

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.allowsLinkPreview = false
        WebViewBridge.sharedWebView = webView

        // MARK: - Native long-press gesture → contextmenu event
        // iOS WKWebView does not fire 'contextmenu' on long-press (Android WebView does natively).
        // React Flow's onNodeContextMenu relies on the contextmenu event.
        // We use a native UILongPressGestureRecognizer to detect the long press at the iOS level,
        // then inject JS to dispatch a contextmenu event at the touch coordinates.
        let longPress = UILongPressGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleLongPress(_:)))
        longPress.minimumPressDuration = 0.5
        longPress.delegate = context.coordinator
        webView.addGestureRecognizer(longPress)

        let langTag = LanguageManager.shared.currentLanguage.rawValue
        AppLogger.log(.debug, "[WebViewBridge] Loading app://index.html#lng=\(langTag)", category: .webBridge)
        if let url = URL(string: "app://index.html#lng=\(langTag)") {
            webView.load(URLRequest(url: url))
            webView.isInspectable = true
        } else {
            AppLogger.log(.error, "[WebViewBridge] Invalid URL for index.html", category: .webBridge)
        }

        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler, UIGestureRecognizerDelegate {

        // MARK: - Pre-computed initial data (ZP-PLATFORM-56)
        // Serialized JSON strings instead of live @Model references.
        // This prevents use-after-free when SwiftData invalidates models after context.save().
        let initialSLDJSON: String
        let initialNodeClassJSON: String
        let initialEdgeClassJSON: String
        let initialViewDataJS: String
        let initialActiveViewIdForJS: String
        let initialNodeCount: Int
        let initialEdgeCount: Int

        let onNodeClicked: (UUID, String) -> Void
        let onEdgeClicked: (UUID) -> Void
        let onNodePositionChanged: (UUID, Double, Double) -> Void
        let onNodeParentChanged: (UUID, UUID?, Double, Double) -> Void
        let onNodeCreated: (UUID, String, String, UUID?, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void
        let onEnclosureCreated: (UUID, String, String, Double, Double, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void
        let onEnclosureSizeUpdated: (UUID, Double, Double) -> Void
        let onNodeRemovedFromParent: (UUID, Double, Double) -> Void
        let onNodeDeleted: (UUID) -> Void
        let onNodeTypeChanged: (UUID, String, Double?, Double?) -> Void
        let onEdgeCreated: (UUID, UUID, UUID, UUID?, UUID?, [EdgePoint]?, String?) -> Void  // id, source, target, sourceTerminalId, targetTerminalId, points, algorithm
        let onEdgeDeleted: (UUID) -> Void
        let onEdgeUpdated: (UUID, [EdgePoint]?, String?) -> Void

        // View navigation callbacks
        let onViewBack: (() -> Void)?
        let onNavigateToView: ((UUID) -> Void)?

        // Collapse state callback
        let onNodeCollapseStateChanged: ((UUID, Bool) -> Void)?

        init(
            initialSLDJSON: String,
            initialNodeClassJSON: String,
            initialEdgeClassJSON: String,
            initialViewDataJS: String,
            initialActiveViewIdForJS: String,
            initialNodeCount: Int,
            initialEdgeCount: Int,
            onNodeClicked: @escaping (UUID, String) -> Void,
            onEdgeClicked: @escaping (UUID) -> Void,
            onNodePositionChanged: @escaping (UUID, Double, Double) -> Void,
            onNodeParentChanged: @escaping (UUID, UUID?, Double, Double) -> Void,
            onNodeCreated: @escaping (UUID, String, String, UUID?, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void,
            onEnclosureCreated: @escaping (UUID, String, String, Double, Double, Double, Double, UUID?, [(id: UUID, orientationTerminalId: UUID)]) -> Void,
            onEnclosureSizeUpdated: @escaping (UUID, Double, Double) -> Void,
            onNodeRemovedFromParent: @escaping (UUID, Double, Double) -> Void,
            onNodeDeleted: @escaping (UUID) -> Void,
            onNodeTypeChanged: @escaping (UUID, String, Double?, Double?) -> Void,
            onEdgeCreated: @escaping (UUID, UUID, UUID, UUID?, UUID?, [EdgePoint]?, String?) -> Void,  // id, source, target, sourceTerminalId, targetTerminalId, points, algorithm
            onEdgeDeleted: @escaping (UUID) -> Void,
            onEdgeUpdated: @escaping (UUID, [EdgePoint]?, String?) -> Void,
            onViewBack: (() -> Void)?,
            onNavigateToView: ((UUID) -> Void)?,
            onNodeCollapseStateChanged: ((UUID, Bool) -> Void)?
        ) {
            self.initialSLDJSON = initialSLDJSON
            self.initialNodeClassJSON = initialNodeClassJSON
            self.initialEdgeClassJSON = initialEdgeClassJSON
            self.initialViewDataJS = initialViewDataJS
            self.initialActiveViewIdForJS = initialActiveViewIdForJS
            self.initialNodeCount = initialNodeCount
            self.initialEdgeCount = initialEdgeCount
            self.onNodeClicked = onNodeClicked
            self.onEdgeClicked = onEdgeClicked
            self.onNodePositionChanged = onNodePositionChanged
            self.onNodeParentChanged = onNodeParentChanged
            self.onNodeCreated = onNodeCreated
            self.onEnclosureCreated = onEnclosureCreated
            self.onEnclosureSizeUpdated = onEnclosureSizeUpdated
            self.onNodeRemovedFromParent = onNodeRemovedFromParent
            self.onNodeDeleted = onNodeDeleted
            self.onNodeTypeChanged = onNodeTypeChanged
            self.onEdgeCreated = onEdgeCreated
            self.onEdgeDeleted = onEdgeDeleted
            self.onEdgeUpdated = onEdgeUpdated
            self.onViewBack = onViewBack
            self.onNavigateToView = onNavigateToView
            self.onNodeCollapseStateChanged = onNodeCollapseStateChanged
        }

        // MARK: - Native Long-Press → contextmenu
        // Dispatches a synthetic 'contextmenu' MouseEvent into the WKWebView at the
        // long-press location so React Flow's onNodeContextMenu fires on iOS.
        @objc func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
            guard gesture.state == .began,
                  let webView = gesture.view as? WKWebView else { return }

            let point = gesture.location(in: webView)
            let js = """
            (function() {
              var el = document.elementFromPoint(\(point.x), \(point.y));
              if (!el) return false;
              var node = el.closest('.react-flow__node');
              if (!node) return false;
              var evt = new MouseEvent('contextmenu', {
                bubbles: true,
                cancelable: true,
                view: window,
                clientX: \(point.x),
                clientY: \(point.y)
              });
              node.dispatchEvent(evt);
              return true;
            })();
            """
            webView.evaluateJavaScript(js) { result, error in
                if let err = error {
                    AppLogger.log(.error, "[WebViewBridge] Long-press JS error: \(err)", category: .webBridge)
                } else {
                    AppLogger.log(.debug, "[WebViewBridge] Long-press contextmenu dispatched: \(result ?? "nil")", category: .webBridge)
                }
            }
        }

        // Allow only our long-press gesture to work simultaneously with the WebView's
        // built-in gesture recognizers (pan, pinch, tap, etc.)
        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            return gestureRecognizer is UILongPressGestureRecognizer
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            AppLogger.log(.debug, "[WebViewBridge] didStartProvisionalNavigation - URL: \(webView.url?.absoluteString ?? "nil")", category: .webBridge)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            AppLogger.log(.error, "[WebViewBridge] didFail - error: \(error.localizedDescription)", category: .webBridge)
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            AppLogger.log(.error, "[WebViewBridge] didFailProvisionalNavigation - error: \(error.localizedDescription)", category: .webBridge)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            AppLogger.log(.info, "[WebViewBridge] didFinish - injecting initialData (nodes: \(initialNodeCount), edges: \(initialEdgeCount), activeViewId: \(initialActiveViewIdForJS))", category: .webBridge)

            // ZP-PLATFORM-56: All data was pre-computed as JSON in makeCoordinator().
            // No @Model references are accessed here, preventing use-after-free crashes.
            let js = """
            setTimeout(() => {
              console.log('[WebViewBridge] → dispatching initialData event');
              const viewData = \(initialViewDataJS);
              window.dispatchEvent(new CustomEvent('initialData', {
                detail: {
                  nodes: \(initialSLDJSON).nodes,
                  edges: \(initialSLDJSON).edges,
                  comments: \(initialSLDJSON).comments,
                  classes: \(initialNodeClassJSON),
                  edgeClasses: \(initialEdgeClassJSON),
                  activeViewId: \(initialActiveViewIdForJS),
                  sldLinks: viewData ? viewData.sldLinks : [],
                  nodePositionOverrides: viewData ? viewData.nodePositionOverrides : {},
                  edgeRoutingOverrides: viewData ? viewData.edgeRoutingOverrides : {}
                }
              }));
            }, 100);
            """
            webView.evaluateJavaScript(js) { result, error in
                if let err = error {
                    AppLogger.log(.error, "[WebViewBridge] JS eval error: \(err)", category: .webBridge)
                } else {
                    AppLogger.log(.debug, "[WebViewBridge] initialData dispatched", category: .webBridge)
                }
            }

            // Inject JavaScript to trigger search on blur (right arrow or tap outside)
            let searchKeyboardHandlerJS = """
            setTimeout(() => {
              console.log('[WebViewBridge] Setting up search keyboard handler');

              let handledInputs = new WeakSet();

              function triggerSearch(searchInput) {
                if (searchInput.value.trim() !== '') {
                  console.log('[WebViewBridge] Triggering search for:', searchInput.value);

                  const enterEvent = new KeyboardEvent('keydown', {
                    key: 'Enter',
                    keyCode: 13,
                    which: 13,
                    bubbles: true,
                    cancelable: true
                  });
                  searchInput.dispatchEvent(enterEvent);
                }
              }

              function setupSearchHandler() {
                const searchInput = document.querySelector('input[placeholder*="Search"]');
                if (searchInput && !handledInputs.has(searchInput)) {
                  console.log('[WebViewBridge] Found search input, adding blur handler');
                  handledInputs.add(searchInput);

                  searchInput.addEventListener('blur', function() {
                    console.log('[WebViewBridge] Search input blur - triggering search');
                    setTimeout(function() { triggerSearch(searchInput); }, 10);
                  });
                }
              }

              setupSearchHandler();

              const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                  if (mutation.addedNodes.length > 0) {
                    mutation.addedNodes.forEach(function(node) {
                      if (node.nodeType === 1 && node.querySelector && node.querySelector('input[placeholder*="Search"]')) {
                        console.log('[WebViewBridge] Search popup detected');
                        setupSearchHandler();
                      }
                    });
                  }
                });
              });

              observer.observe(document.body, { childList: true, subtree: true });

              console.log('[WebViewBridge] Search keyboard handler ready');
            }, 500);
            """

            webView.evaluateJavaScript(searchKeyboardHandlerJS) { result, error in
                if let err = error {
                    AppLogger.log(.error, "[WebViewBridge] Search keyboard handler error: \(err)", category: .webBridge)
                } else {
                    AppLogger.log(.debug, "[WebViewBridge] Search keyboard handler injected", category: .webBridge)
                }
            }

        }

        func userContentController(
            _ uc: WKUserContentController,
            didReceive message: WKScriptMessage
        ) {
            AppLogger.log(.debug, "[WebViewBridge] message received: \(message.name) \(message.body)", category: .webBridge)
            guard let body = message.body as? [String:Any] else {
                AppLogger.log(.notice, "[WebViewBridge] malformed message body", category: .webBridge)
                return
            }
            switch message.name {

            case "nodeClicked":
                if let id = UUID(uuidString: body["id"] as? String ?? ""),
                   let type = body["type"] as? String {
                    AppLogger.log(.debug, "[WebViewBridge] nodeClicked -> id=\(id), type=\(type)", category: .webBridge)
                    onNodeClicked(id, type)
                }
            case "nodePositionChanged":
                if let id = UUID(uuidString: body["nodeId"] as? String ?? ""),
                   let pos = body["position"] as? [String:Any],
                   let x = pos["x"] as? Double,
                   let y = pos["y"] as? Double {
                    AppLogger.log(.debug, "[WebViewBridge] nodePositionChanged -> id=\(id), x=\(x), y=\(y)", category: .webBridge)
                    onNodePositionChanged(id, x, y)
                }
            case "nodeParentChanged":
                guard
                    let nodeIdStr   = body["nodeId"]          as? String,
                    let id          = UUID(uuidString: nodeIdStr),
                    let parentIdStr = body["parentId"]        as? String,
                    let pid         = UUID(uuidString: parentIdStr),
                    let posDict     = body["relativePosition"] as? [String: Any],
                    let relX        = posDict["x"]            as? Double,
                    let relY        = posDict["y"]            as? Double
                else {
                    AppLogger.log(.notice, "[WebViewBridge] invalid nodeParentChanged payload: \(body)", category: .webBridge)
                    return
                }

                AppLogger.log(.debug, "[WebViewBridge] nodeParentChanged -> id=\(id), parent=\(pid)", category: .webBridge)
                onNodeParentChanged(id, pid, relX, relY)

                // MARK: - Graph Updates
            case "graphUpdate":
                guard
                    let body       = message.body as? [String:Any],
                    let type       = body["type"]   as? String
                else {
                    AppLogger.log(.notice, "[WebViewBridge] malformed graphUpdate: \(message.body)", category: .webBridge)
                    return
                }

                switch type {
                case "editNode":
                    guard
                        let payload    = body["payload"] as? [String:Any],
                        let idString = payload["id"] as? String,
                        let uuid     = UUID(uuidString: idString)
                    else {
                        AppLogger.log(.notice, "[WebViewBridge] invalid editNode payload: \(body["payload"] as Any)", category: .webBridge)
                        return
                    }
                    AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> editNode id=\(uuid)", category: .webBridge)
                    onNodeClicked(uuid, type)

                case "enclosureSizeUpdated":
                    guard
                        let payload = body["payload"] as? [String:Any],
                        let idString = payload["enclosureId"] as? String,
                        let uuid = UUID(uuidString: idString),
                        let width = payload["width"] as? Double,
                        let height = payload["height"] as? Double
                    else {
                        AppLogger.log(.notice, "[WebViewBridge] invalid enclosureSizeUpdated payload: \(body["payload"] as Any)", category: .webBridge)
                        return
                    }
                    AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> enclosureSizeUpdated id=\(uuid), width=\(width), height=\(height)", category: .webBridge)
                    onEnclosureSizeUpdated(uuid, width, height)

                case "editEdge":
                    guard
                        let payload    = body["payload"] as? [String:Any],
                        let idString = payload["id"] as? String,
                        let uuid     = UUID(uuidString: idString)
                    else {
                        AppLogger.log(.notice, "[WebViewBridge] invalid editEdge payload: \(body["payload"] as Any)", category: .webBridge)
                        return
                    }
                    AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> editEdge id=\(uuid)", category: .webBridge)
                    onEdgeClicked(uuid)

                    case "enclosureCreated":
                        guard
                            let payload     = body["payload"]       as? [String: Any],
                            let idString    = payload["id"]     as? String,
                            let uuid        = UUID(uuidString: idString),
                            let type        = payload["type"]   as? String,
                            let label       = payload["label"]  as? String,
                            let x           = payload["x"]       as? Double,
                            let y           = payload["y"]       as? Double,
                            let width       = payload["width"]  as? Double,
                            let height      = payload["height"] as? Double
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid enclosureCreated payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }

                        // Handle class ID if present
                        let classID: UUID?
                        if let classIDString = payload["classId"] as? String {
                            classID = UUID(uuidString: classIDString)
                        } else {
                            classID = nil
                        }

                        // Parse terminal data
                        var terminals: [(id: UUID, orientationTerminalId: UUID)] = []
                        if let terminalArray = payload["node_terminals"] as? [[String: Any]] {
                            for terminalDict in terminalArray {
                                if let terminalIdStr = terminalDict["id"] as? String,
                                   let terminalId = UUID(uuidString: terminalIdStr),
                                   let orientationTerminalIdStr = terminalDict["node_orientation_terminal_id"] as? String,
                                   let orientationTerminalId = UUID(uuidString: orientationTerminalIdStr) {
                                    terminals.append((id: terminalId, orientationTerminalId: orientationTerminalId))
                                }
                            }
                        }

                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> enclosureCreated id=\(uuid), terminals=\(terminals.count)", category: .webBridge)
                        onEnclosureCreated(uuid, label, type, x, y, width, height, classID, terminals)

                    case "nodeCreated":
                        guard
                            let payload             = body["payload"]       as? [String: Any],
                            let idString            = payload["id"]     as? String,
                            let uuid                = UUID(uuidString: idString),
                            let type                = payload["type"]   as? String,
                            let label               = payload["label"]  as? String,
                            let x                   = payload["x"]       as? Double,
                            let y                   = payload["y"]       as? Double,
                            let classIDString       = payload["classId"] as? String,
                            let classID             = UUID(uuidString: classIDString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid nodeCreated payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }

                        // Handle parent ID if present
                        let parentID: UUID?
                        if let parentIdString = payload["parentId"] as? String {
                            parentID = UUID(uuidString: parentIdString)
                        } else {
                            parentID = nil
                        }

                        // Parse terminal data
                        var terminals: [(id: UUID, orientationTerminalId: UUID)] = []
                        if let terminalArray = payload["node_terminals"] as? [[String: Any]] {
                            for terminalDict in terminalArray {
                                if let terminalIdStr = terminalDict["id"] as? String,
                                   let terminalId = UUID(uuidString: terminalIdStr),
                                   let orientationTerminalIdStr = terminalDict["node_orientation_terminal_id"] as? String,
                                   let orientationTerminalId = UUID(uuidString: orientationTerminalIdStr) {
                                    terminals.append((id: terminalId, orientationTerminalId: orientationTerminalId))
                                }
                            }
                        }

                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> nodeCreated id=\(uuid), type=\(type), classID=\(String(describing: classID)), terminals=\(terminals.count)", category: .webBridge)
                        onNodeCreated(uuid, label, type, parentID, x, y, classID, terminals)

                    case "nodeRemovedFromEnclosure":
                        guard
                            let payload     = body["payload"] as? [String:Any],
                            let idString    = payload["nodeId"] as? String,
                            let uuid        = UUID(uuidString: idString),
                            let posDict     = payload["position"] as? [String: Any],
                            let x           = posDict["x"]       as? Double,
                            let y           = posDict["y"]       as? Double
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid nodeRemovedFromEnclosure payload: \(body["payload"] as Any)", category: .webBridge)
                            return
                        }
                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> nodeRemovedFromEnclosure id=\(uuid)", category: .webBridge)
                        onNodeRemovedFromParent(uuid, x, y)

                    case "nodeDeleted":
                        guard
                            let payload = body["node"] as? [String:Any],
                            let idString = payload["id"] as? String,
                            let uuid     = UUID(uuidString: idString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid nodeDeleted payload: \(body["node"] as Any)", category: .webBridge)
                            return
                        }
                        onNodeDeleted(uuid)

                    case "edgeCreated":
                        guard
                            let payload         = body["payload"] as? [String: Any],
                            let idString        = payload["id"] as? String,
                            let uuid            = UUID(uuidString: idString),
                            let sourceIdString  = payload["source"] as? String,
                            let sourceUUID      = UUID(uuidString: sourceIdString),
                            let targetIdString  = payload["target"] as? String,
                            let targetUUID      = UUID(uuidString: targetIdString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid edgeCreated payload: \(body["edge"] ?? "nil")", category: .webBridge)
                            return
                        }

                        // Extract terminal UUIDs - authoritative identifiers for connections
                        let sourceTerminalId: UUID? = (payload["source_node_terminal_id"] as? String).flatMap { UUID(uuidString: $0) }
                        let targetTerminalId: UUID? = (payload["target_node_terminal_id"] as? String).flatMap { UUID(uuidString: $0) }

                        // Extract points and algorithm from nested data object
                        let data = payload["data"] as? [String: Any]
                        let pointsArray = data?["points"] as? [[String: Any]]
                        let points: [EdgePoint]? = pointsArray?.compactMap { pointDict in
                            guard let x = pointDict["x"] as? Double ?? (pointDict["x"] as? String).flatMap(Double.init),
                                  let y = pointDict["y"] as? Double ?? (pointDict["y"] as? String).flatMap(Double.init),
                                  let id = pointDict["id"] as? String,
                                  let active = pointDict["active"] as? Bool ?? (pointDict["active"] as? Int).map({ $0 == 1 }) else {
                                return nil
                            }
                            return EdgePoint(x: x, y: y, id: id, active: active)
                        }
                        let algorithm = data?["algorithm"] as? String

                        onEdgeCreated(uuid, sourceUUID, targetUUID, sourceTerminalId, targetTerminalId, points, algorithm)

                    case "edgeDeleted":
                        guard
                            let payload     = body["payload"] as? [String: Any],
                            let idString    = payload["id"] as? String,
                            let uuid        = UUID(uuidString: idString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid edgeDeleted payload: \(body["edge"] ?? "nil")", category: .webBridge)
                            return
                        }
                        onEdgeDeleted(uuid)

                    case "edgeUpdated":
                        guard
                            let payload = body["payload"] as? [String: Any],
                            let idString = payload["id"] as? String,
                            let uuid = UUID(uuidString: idString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid edgeUpdated payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }

                        // Extract points if present
                        let points: [EdgePoint]?
                        if let pointsArray = payload["points"] as? [[String: Any]] {
                            points = pointsArray.compactMap { pointDict in
                                guard let x = pointDict["x"] as? Double ?? (pointDict["x"] as? String).flatMap(Double.init),
                                      let y = pointDict["y"] as? Double ?? (pointDict["y"] as? String).flatMap(Double.init),
                                      let id = pointDict["id"] as? String,
                                      let active = pointDict["active"] as? Bool ?? (pointDict["active"] as? Int).map({ $0 == 1 }) else {
                                    return nil
                                }
                                return EdgePoint(x: x, y: y, id: id, active: active)
                            }
                        } else {
                            points = nil
                        }

                        // Extract algorithm if present
                        let algorithm = payload["algorithm"] as? String

                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> edgeUpdated id=\(uuid), points=\(points?.count ?? 0), algorithm=\(algorithm ?? "nil")", category: .webBridge)
                        onEdgeUpdated(uuid, points, algorithm)

                    case "nodeTypeChanged":
                        guard
                            let payload = body["payload"] as? [String: Any],
                            let nodeIdString = payload["nodeId"] as? String,
                            let nodeId = UUID(uuidString: nodeIdString),
                            let newType = payload["type"] as? String
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid nodeTypeChanged payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }

                        // Extract optional width and height
                        let width = payload["width"] as? Double
                        let height = payload["height"] as? Double

                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> nodeTypeChanged id=\(nodeId), type=\(newType), width=\(width?.description ?? "nil"), height=\(height?.description ?? "nil")", category: .webBridge)
                        onNodeTypeChanged(nodeId, newType, width, height)

                    case "viewBack":
                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> viewBack requested", category: .webBridge)
                        if let callback = onViewBack {
                            callback()
                        } else {
                            AppLogger.log(.notice, "[WebViewBridge] No onViewBack callback registered", category: .webBridge)
                        }

                    case "navigateToView":
                        guard
                            let payload = body["payload"] as? [String: Any],
                            let targetViewIdString = payload["targetViewId"] as? String,
                            let targetViewId = UUID(uuidString: targetViewIdString)
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid navigateToView payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }
                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> navigateToView: \(targetViewId)", category: .webBridge)
                        if let callback = onNavigateToView {
                            callback(targetViewId)
                        } else {
                            AppLogger.log(.notice, "[WebViewBridge] No onNavigateToView callback registered", category: .webBridge)
                        }

                    case "nodeCollapseStateChanged":
                        guard
                            let payload = body["payload"] as? [String: Any],
                            let nodeIdString = payload["nodeId"] as? String,
                            let nodeId = UUID(uuidString: nodeIdString),
                            let isCollapsed = payload["isCollapsed"] as? Bool
                        else {
                            AppLogger.log(.notice, "[WebViewBridge] invalid nodeCollapseStateChanged payload: \(body["payload"] ?? "nil")", category: .webBridge)
                            return
                        }
                        AppLogger.log(.debug, "[WebViewBridge] graphUpdate -> nodeCollapseStateChanged nodeId=\(nodeId), isCollapsed=\(isCollapsed)", category: .webBridge)
                        if let callback = onNodeCollapseStateChanged {
                            callback(nodeId, isCollapsed)
                        } else {
                            AppLogger.log(.notice, "[WebViewBridge] No onNodeCollapseStateChanged callback registered", category: .webBridge)
                        }

                    default:
                        AppLogger.log(.notice, "[WebViewBridge] unhandled graphUpdate type: \(type)", category: .webBridge)
                    }
                default:
                    AppLogger.log(.notice, "[WebViewBridge] unhandled message: \(message.name)", category: .webBridge)
                }
            }
        }
    }

    extension WebViewBridge {
        /// Updates the graph by fetching the current state from SwiftData
        static func updateGraphFromSLD(_ sldId: UUID, in context: ModelContext, animated: Bool = true) {
            // Fetch the SLD with its current relationships
            let sldDescriptor = FetchDescriptor<SLDV2>(
                predicate: #Predicate { sld in
                    sld.id == sldId
                }
            )

            guard let sld = try? context.fetch(sldDescriptor).first else {
                AppLogger.log(.error, "[WebViewBridge] Could not find SLD with id: \(sldId)", category: .webBridge)
                return
            }

            // Use SLDService to create a proper DTO with all fields
            let dto = SLDService.shared.createDTO(
                forSLDId: sldId,
                modelContext: context,
                customName: "bridge-update"
            )

            updateGraph(with: dto, animated: animated)
        }

        /// Alternative that accepts an SLD object but refreshes it from context
        static func updateGraphFromSLD(_ sld: SLDV2, in context: ModelContext, animated: Bool = true) {
            // Always fetch fresh data using the ID
            updateGraphFromSLD(sld.id, in: context, animated: animated)
        }

        /// Updates graph for a specific node change
        static func updateGraphForNode(_ nodeId: UUID, in context: ModelContext, animated: Bool = true) {
            // Find the node and its SLD
            let nodeDescriptor = FetchDescriptor<NodeV2>(
                predicate: #Predicate { node in
                    node.id == nodeId
                }
            )

            guard let node = try? context.fetch(nodeDescriptor).first,
                  let sldId = node.sld?.id else {
                AppLogger.log(.error, "[WebViewBridge] Could not find node or its SLD for id: \(nodeId)", category: .webBridge)
                return
            }

            // Update the entire graph for that SLD
            updateGraphFromSLD(sldId, in: context, animated: animated)
        }
    }
