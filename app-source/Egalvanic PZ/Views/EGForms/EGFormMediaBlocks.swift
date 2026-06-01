//
//  EGFormMediaBlocks.swift
//  Egalvanic PZ
//
//  ZP-1723: `signature` and `image_capture` blocks.
//
//  Both produce base64 payloads that ride inside `form_submission`.
//  On submit, the backend's `extract_and_upload_media` walks the
//  submission and promotes any inline base64 to S3 refs — so iOS
//  doesn't need an S3 client or photo-upload queue for EG forms.
//

import SwiftUI
import UIKit
import PencilKit
import PhotosUI
import AVFoundation

// MARK: - signature  (PencilKit canvas → base64 PNG string at path)
//
// Web shape: a single string value at the path, e.g.
//   "data:image/png;base64,iVBORw0KG..."
// On sync, the backend strips that into `{_stripped: true, _was:
// "signature"}` — render a placeholder when we see that marker.

struct EGFormSignatureBlock: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    @State private var canvas = PKCanvasView()
    @State private var hasDrawn = false

    var body: some View {
        let label = block.content?.label ?? "Signature"
        guard let path = resolvedSignaturePath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }

        let stored = viewModel.value(at: path)
        let storedDataUri: String? = stored as? String
        let storedDict = stored as? [String: Any]
        let isStripped = (storedDict?["_stripped"] as? Bool) == true
        // S3 ref left in place after backend extracted the base64 PNG.
        let s3Url: URL? = {
            guard (storedDict?["_s3"] as? Bool) == true,
                  let u = storedDict?["url"] as? String else { return nil }
            return URL(string: u)
        }()
        let hasStoredSignature = storedDataUri != nil || s3Url != nil

        return AnyView(VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            HStack {
                Text(label).font(EGFormStyle.labelFont).foregroundStyle(.primary)
                Spacer()
                if !readOnly && (hasDrawn || hasStoredSignature) {
                    Button("Clear") { clear(path: path) }
                        .font(EGFormStyle.smallStrong)
                        .foregroundStyle(.red)
                }
            }

            // Render states, in order:
            //   1. user is actively drawing (hasDrawn flag) — keep canvas open
            //   2. local base64 string from a recent unsubmitted sign
            //   3. backend S3 ref (extract_and_upload_media already ran) — AsyncImage
            //   4. backend strip marker — placeholder (rare; only if iOS request route)
            //   5. nothing yet → canvas
            if hasDrawn || (!hasStoredSignature && !isStripped) {
                PKCanvasViewRep(canvas: canvas,
                                onStroke: { writeCanvas(path: path) })
                    .frame(height: 160)
                    .background(Color.white)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(EGFormStyle.panelBorder, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                Text("Sign above")
                    .font(EGFormStyle.smallFont)
                    .foregroundStyle(.secondary)
            } else if let dataUri = storedDataUri {
                signaturePreview(dataUri: dataUri)
                if !readOnly {
                    Button("Re-sign") { hasDrawn = true; canvas.drawing = PKDrawing() }
                        .font(EGFormStyle.smallStrong)
                }
            } else if let url = s3Url {
                s3Preview(url: url)
                if !readOnly {
                    Button("Re-sign") { hasDrawn = true; canvas.drawing = PKDrawing() }
                        .font(EGFormStyle.smallStrong)
                }
            } else if isStripped {
                placeholder("Signature on file — view on web")
            }
        })
    }

    private func clear(path: [String]) {
        canvas.drawing = PKDrawing()
        hasDrawn = false
        instantUpdate { viewModel.setValue(nil, at: path) }
    }

    private func writeCanvas(path: [String]) {
        hasDrawn = !canvas.drawing.strokes.isEmpty
        guard hasDrawn else {
            instantUpdate { viewModel.setValue(nil, at: path) }
            return
        }
        // PencilKit renders system ink relative to the *caller's* current
        // trait collection — not the canvas's override. In dark mode the
        // baked PNG comes out with white ink, which is invisible on the
        // web preview's white background. Force a light trait while we
        // render so the saved image always has black ink on transparent.
        var image: UIImage? = nil
        let lightTraits = UITraitCollection(userInterfaceStyle: .light)
        lightTraits.performAsCurrent {
            image = canvas.drawing.image(from: canvas.drawing.bounds, scale: 2.0)
        }
        guard let image, let png = image.pngData() else { return }
        let dataUri = "data:image/png;base64,\(png.base64EncodedString())"
        instantUpdate { viewModel.setValue(dataUri, at: path) }
    }

    @ViewBuilder
    private func signaturePreview(dataUri: String) -> some View {
        if let img = decodeDataUri(dataUri) {
            Image(uiImage: img)
                .resizable()
                .scaledToFit()
                .frame(maxHeight: 160)
                .background(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(EGFormStyle.panelBorder, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        } else {
            placeholder("Signature unavailable")
        }
    }

    @ViewBuilder
    private func s3Preview(url: URL) -> some View {
        // Network-backed signature (backend extracted base64 → S3). When
        // offline the presigned URL will fail; the failure phase shows
        // the same placeholder a stripped-server-side signature would.
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFit()
            case .failure:
                placeholder("Signature unavailable offline")
            case .empty:
                ProgressView().frame(height: 100)
            @unknown default:
                placeholder("Signature unavailable")
            }
        }
        .frame(maxHeight: 160)
        .background(Color.white)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(EGFormStyle.panelBorder, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    @ViewBuilder
    private func placeholder(_ text: String) -> some View {
        HStack {
            Image(systemName: "signature").foregroundStyle(.secondary)
            Text(text).font(EGFormStyle.smallFont).foregroundStyle(.secondary)
            Spacer()
        }
        .padding(12)
        .background(EGFormStyle.panelHdrBg)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct PKCanvasViewRep: UIViewRepresentable {
    let canvas: PKCanvasView
    let onStroke: () -> Void

    func makeUIView(context: Context) -> PKCanvasView {
        canvas.drawingPolicy = .anyInput
        canvas.tool = PKInkingTool(.pen, color: .black, width: 2)
        canvas.backgroundColor = .clear
        canvas.delegate = context.coordinator
        // PencilKit auto-inverts ink in dark mode for visibility on
        // dark backgrounds. We explicitly render on a white canvas
        // (signatures need to look like ink on paper for the audit /
        // PDF), so pin the trait collection to light — keeps black
        // ink actually black regardless of the device-wide setting.
        canvas.overrideUserInterfaceStyle = .light
        return canvas
    }
    func updateUIView(_ uiView: PKCanvasView, context: Context) {}

    func makeCoordinator() -> Coord { Coord(onStroke: onStroke) }

    final class Coord: NSObject, PKCanvasViewDelegate {
        let onStroke: () -> Void
        init(onStroke: @escaping () -> Void) { self.onStroke = onStroke }
        func canvasViewDrawingDidChange(_ canvasView: PKCanvasView) { onStroke() }
    }
}

// MARK: - image_capture  (photo array under the block's key)
//
// Web shape: array of `{ id, data, caption, timestamp, synced: false }`.
// On sync, backend swaps each entry's `data` for an `_s3` ref and
// (for mobile responses) strips `data` entirely. Cell renders three
// states per photo:
//   • `_s3` + `url` → AsyncImage (network; placeholder if offline)
//   • `data` (local base64) → inline image
//   • `_stripped` true → placeholder

struct EGFormImageCaptureBlock: View {
    let block: EGBlock
    let dataPath: [String]
    let readOnly: Bool
    @Environment(EGFormViewModel.self) private var viewModel

    @State private var pickerItem: PhotosPickerItem? = nil
    @State private var libraryOpen = false
    @State private var showingCamera = false
    @State private var actionSheet = false
    @State private var importError: String? = nil
    @State private var permissionBlock: PermissionBlock? = nil

    private enum PermissionBlock: Identifiable {
        case camera, photos
        var id: Int { self == .camera ? 0 : 1 }
        var title: String { self == .camera ? "Camera Access Required" : "Photos Access Required" }
        var message: String {
            switch self {
            case .camera:
                return "Enable camera access in Settings → Privacy → Camera → Z Platform so you can attach photos to the form."
            case .photos:
                return "Enable Photos access (Add Photos Only is fine) in Settings → Privacy → Photos → Z Platform so the app can save an audit copy of every captured photo."
            }
        }
    }

    private var maxPhotos: Int { 5 }

    var body: some View {
        let label = block.content?.label ?? "Photo"
        guard let path = resolvedSignaturePath(parent: dataPath, content: block.content) else {
            return AnyView(MissingKeyHint(label: label))
        }
        return AnyView(content(label: label, path: path))
    }

    @ViewBuilder
    private func content(label: String, path: [String]) -> some View {
        let entries = readPhotos(at: path)
        let canAdd = !readOnly && entries.count < maxPhotos

        VStack(alignment: .leading, spacing: EGFormStyle.labelGap) {
            HStack(spacing: 6) {
                Text(label).font(EGFormStyle.labelFont).foregroundStyle(.primary)
                Text("\(entries.count)/\(maxPhotos)")
                    .font(EGFormStyle.smallFont)
                    .foregroundStyle(.tertiary)
                Spacer()
            }

            let cols = [GridItem(.adaptive(minimum: 110, maximum: 130), spacing: 8)]
            LazyVGrid(columns: cols, alignment: .leading, spacing: 8) {
                ForEach(Array(entries.enumerated()), id: \.offset) { idx, entry in
                    photoTile(entry: entry, idx: idx, path: path)
                }
                if canAdd {
                    addTile()
                }
            }

            if let importError = importError {
                Text(importError)
                    .font(EGFormStyle.smallFont)
                    .foregroundStyle(.red)
            }
        }
        .confirmationDialog("Add photo", isPresented: $actionSheet, titleVisibility: .hidden) {
            Button("Take Photo") {
                Task { await requestThenOpen(camera: true) }
            }
            Button("Choose from Library") {
                Task { await requestThenOpen(camera: false) }
            }
            Button("Cancel", role: .cancel) { }
        }
        .alert(item: $permissionBlock) { block in
            Alert(
                title: Text(block.title),
                message: Text(block.message),
                primaryButton: .default(Text("Open Settings"), action: openAppSettings),
                secondaryButton: .cancel()
            )
        }
        .photosPicker(isPresented: $libraryOpen,
                      selection: $pickerItem,
                      matching: .images,
                      photoLibrary: .shared())
        .onChange(of: pickerItem) { _, item in
            guard let item = item else { return }
            Task { await ingest(from: item, path: path) }
        }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraPicker { image in
                showingCamera = false
                guard let image = image else { return }
                Task { await ingest(uiImage: image, path: path) }
            }
            .ignoresSafeArea()
        }
    }

    // MARK: tiles

    @ViewBuilder
    private func photoTile(entry: EGPhotoEntry, idx: Int, path: [String]) -> some View {
        ZStack(alignment: .topTrailing) {
            photoImage(entry: entry)
                .frame(width: 110, height: 110)
                .clipped()
                .background(Color(.systemGray5))
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .overlay(RoundedRectangle(cornerRadius: 6).stroke(EGFormStyle.panelBorder, lineWidth: 1))

            if !readOnly {
                Button {
                    instantUpdate { removePhoto(at: idx, path: path) }
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.white, .black.opacity(0.6))
                        .font(.system(size: 18))
                        .padding(4)
                }
            }
        }
    }

    @ViewBuilder
    private func photoImage(entry: EGPhotoEntry) -> some View {
        if let data = entry.data, let img = decodeDataUri(data) {
            // Local base64 — render inline.
            Image(uiImage: img).resizable().scaledToFill()
        } else if let urlString = entry.url, let url = URL(string: urlString) {
            // S3 ref with presigned URL — network image.
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image): image.resizable().scaledToFill()
                case .failure: stripPlaceholder
                default: ProgressView()
                }
            }
        } else {
            // Backend-stripped or offline-without-URL.
            stripPlaceholder
        }
    }

    private var stripPlaceholder: some View {
        VStack(spacing: 4) {
            Image(systemName: "photo.fill").font(.system(size: 24)).foregroundStyle(.tertiary)
            Text("View on web").font(.system(size: 9)).foregroundStyle(.tertiary)
        }
    }

    @ViewBuilder
    private func addTile() -> some View {
        Button { actionSheet = true } label: {
            VStack(spacing: 4) {
                Image(systemName: "camera").font(.system(size: 22))
                Text("Add").font(EGFormStyle.smallFont)
            }
            .foregroundStyle(.tint)
            .frame(width: 110, height: 110)
            .background(Color(.systemBackground))
            .overlay(RoundedRectangle(cornerRadius: 6).strokeBorder(EGFormStyle.panelBorder, style: StrokeStyle(lineWidth: 1, dash: [4, 3])))
        }
        .buttonStyle(.plain)
    }

    // MARK: permission gates
    //
    // Request the right OS permission upfront for whichever capture path
    // the user picked. If the user has denied (either now or in a past
    // session), show an alert pointing at Settings — that's the only
    // way to recover; iOS won't re-prompt once denied.

    @MainActor
    private func requestThenOpen(camera: Bool) async {
        // Photos addOnly is required either way — `PhotoCaptureService`
        // saves the audit copy to the user's library regardless of
        // whether the source was camera or library picker.
        let photosOK = await PhotoCaptureService.ensureLibraryWriteAccess()
        guard photosOK else { permissionBlock = .photos; return }

        if camera {
            let camOK = await requestCameraAccess()
            guard camOK else { permissionBlock = .camera; return }
            showingCamera = true
        } else {
            libraryOpen = true
        }
    }

    private func requestCameraAccess() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            return true
        case .notDetermined:
            return await AVCaptureDevice.requestAccess(for: .video)
        case .denied, .restricted:
            return false
        @unknown default:
            return false
        }
    }

    private func openAppSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }

    // MARK: data plumbing

    private func readPhotos(at path: [String]) -> [EGPhotoEntry] {
        guard let raw = viewModel.value(at: path) else { return [] }
        if let arr = raw as? [[String: Any]] {
            return arr.map(EGPhotoEntry.init(dict:))
        }
        return []
    }

    private func writePhotos(_ photos: [EGPhotoEntry], at path: [String]) {
        let arr = photos.map { $0.asDict() }
        viewModel.setValue(arr, at: path)
    }

    private func removePhoto(at idx: Int, path: [String]) {
        var photos = readPhotos(at: path)
        guard idx < photos.count else { return }
        photos.remove(at: idx)
        writePhotos(photos, at: path)
    }

    private func ingest(from item: PhotosPickerItem, path: [String]) async {
        defer { Task { @MainActor in pickerItem = nil } }
        guard let data = try? await item.loadTransferable(type: Data.self),
              let img = UIImage(data: data) else { return }
        await ingest(uiImage: img, path: path)
    }

    private func ingest(uiImage: UIImage, path: [String]) async {
        let ctx = PhotoStampContext.pending(
            kind: "EG Form",
            label: viewModel.instance.egForm?.title,
            photoType: "eg_form_photo",
            photosetId: viewModel.instance.id,
            photosetSubIndex: nil,
            sld: viewModel.instance.linkedNodes.first?.sld,
            building: nil, floor: nil, room: nil
        )
        do {
            let (clean, photoId) = try await PhotoCaptureService.captureForEmbed(image: uiImage, context: ctx)
            let entry = EGPhotoEntry(
                id: photoId.uuidString,
                data: "data:image/jpeg;base64,\(clean.base64EncodedString())",
                caption: nil,
                timestamp: ISO8601DateFormatter().string(from: Date()),
                synced: false
            )
            var photos = readPhotos(at: path)
            photos.append(entry)
            writePhotos(photos, at: path)
            importError = nil
        } catch PhotoCaptureServiceError.permissionDenied {
            importError = "Photos access required to save audit copy."
        } catch {
            importError = "Failed to add photo."
        }
    }

}

// MARK: - Camera (UIImagePickerController via representable)

private struct CameraPicker: UIViewControllerRepresentable {
    let onCapture: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraCaptureMode = .photo
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onCapture: onCapture) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onCapture: (UIImage?) -> Void
        init(onCapture: @escaping (UIImage?) -> Void) { self.onCapture = onCapture }
        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            onCapture(info[.originalImage] as? UIImage)
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCapture(nil)
        }
    }
}

// MARK: - Shared helpers

/// Resolve the data path under the parent for a media block, requiring
/// a `content.key`. Re-exported here so `EGFormInputBlocks.swift`'s
/// private helper isn't needed.
@MainActor
fileprivate func resolvedSignaturePath(parent: [String], content: EGBlockContent?) -> [String]? {
    guard let key = content?.key, !key.isEmpty else { return nil }
    return parent + [key]
}

/// Decode `data:image/...;base64,XXXX` into a UIImage.
fileprivate func decodeDataUri(_ uri: String) -> UIImage? {
    guard let commaIdx = uri.firstIndex(of: ","),
          let data = Data(base64Encoded: String(uri[uri.index(after: commaIdx)...])) else { return nil }
    return UIImage(data: data)
}

/// Photo entry in the form_submission array — same shape as the web
/// `ImageCaptureBlock` writes. Codable to/from a dict so we can keep
/// formData as `[String: Any]` end-to-end.
struct EGPhotoEntry {
    var id: String
    var data: String?
    var caption: String?
    var timestamp: String?
    var synced: Bool?

    // Backend-extracted state.
    var _s3: Bool?
    var photo_id: String?
    var key: String?
    var bucket: String?
    var url: String?
    var _stripped: Bool?

    init(id: String, data: String?, caption: String?, timestamp: String?, synced: Bool?) {
        self.id = id
        self.data = data
        self.caption = caption
        self.timestamp = timestamp
        self.synced = synced
    }

    init(dict: [String: Any]) {
        self.id = dict["id"] as? String ?? UUID().uuidString
        self.data = dict["data"] as? String
        self.caption = dict["caption"] as? String
        self.timestamp = dict["timestamp"] as? String
        self.synced = dict["synced"] as? Bool
        self._s3 = dict["_s3"] as? Bool
        self.photo_id = dict["photo_id"] as? String
        self.key = dict["key"] as? String
        self.bucket = dict["bucket"] as? String
        self.url = dict["url"] as? String
        self._stripped = dict["_stripped"] as? Bool
    }

    func asDict() -> [String: Any] {
        var d: [String: Any] = ["id": id]
        if let v = data { d["data"] = v }
        if let v = caption { d["caption"] = v }
        if let v = timestamp { d["timestamp"] = v }
        if let v = synced { d["synced"] = v }
        if let v = _s3 { d["_s3"] = v }
        if let v = photo_id { d["photo_id"] = v }
        if let v = key { d["key"] = v }
        if let v = bucket { d["bucket"] = v }
        if let v = url { d["url"] = v }
        if let v = _stripped { d["_stripped"] = v }
        return d
    }
}
