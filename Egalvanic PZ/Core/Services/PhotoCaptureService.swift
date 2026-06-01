//
//  PhotoCaptureService.swift
//  Egalvanic PZ
//
//  ZP-2230: single durable persistence path for every captured /
//  picked photo in the app.
//
//  Replaces the per-call-site "resize → jpegData → write to
//  Documents/photos" pattern that left no off-app copy of the bytes.
//  When ``Documents/photos`` got wiped by an app reinstall / offload
//  the sync queue kept records that pointed at files no longer on
//  disk, so the bytes were lost forever even though the queue still
//  thought it was working (Brightview Tenafly incident, May 2026).
//
//  The new path:
//
//    1. Resize the captured image to 1024 px max (same as before).
//    2. Render a 3-line audit stamp into the lower-left corner
//       (PhotoStampService).
//    3. JPEG-encode at 0.7.
//    4. Write the stamped JPEG to ``Documents/photos/<filename>``.
//    5. Save the *same* stamped JPEG to the user's Photos library
//       (PhotoLibraryService).
//    6. Both saves must succeed before we create the Photo
//       SwiftData row + return — if either step fails we delete
//       any partial state and throw, so the sync queue never holds
//       a record without a durable copy.
//
//  Permission for ``addOnly`` Photos access is a hard requirement.
//  Each capture surface should call
//  ``ensureLibraryWriteAccess(...)`` at "open camera" time and
//  refuse to launch the picker when access isn't granted — this
//  service double-checks at save time as a backstop.
//
import Foundation
import SwiftData
import UIKit

enum PhotoCaptureServiceError: LocalizedError {
    case permissionDenied
    case encodingFailed
    case diskWriteFailed(Error)
    case libraryWriteFailed(Error)

    var errorDescription: String? {
        switch self {
        case .permissionDenied:
            return "Photos library access is required. Enable it in Settings → Privacy → Photos → Egalvanic PZ → Add Photos Only."
        case .encodingFailed:
            return "Failed to encode the captured image as JPEG."
        case .diskWriteFailed(let err):
            return "Failed to save the captured image to local storage: \(err.localizedDescription)"
        case .libraryWriteFailed(let err):
            return "Failed to save the captured image to your Photos library: \(err.localizedDescription)"
        }
    }
}

enum PhotoCaptureService {
    /// Max long side for the persisted JPEG. Matches the pre-ZP-2230
    /// behavior of every capture surface so existing report rendering
    /// + S3 upload sizing don't shift.
    static let maxDimension: CGFloat = 1024
    static let jpegQuality: CGFloat = 0.7

    /// Verify (and request if needed) Photos library ``addOnly``
    /// access. Returns ``true`` when the app may safely launch a
    /// camera / picker; false when the user has denied or it's
    /// restricted by the OS (Screen Time, parental controls, etc).
    @MainActor
    static func ensureLibraryWriteAccess() async -> Bool {
        let status = await PhotoLibraryService.shared.requestAuthorizationIfNeeded()
        return status == .authorized
    }

    /// Persist a captured / picked image atomically — existing
    /// entity flow. Wraps the context-based overload so callers
    /// that already have a SwiftData entity don't have to build a
    /// PhotoStampContext themselves.
    @MainActor
    static func captureAndPersist(
        image: UIImage,
        entity: any EntityWithPhotos,
        photoType: String,
        caption: String? = nil
    ) async throws -> Photo {
        let ctx = PhotoStampContext.from(entity: entity, photoType: photoType)
        return try await captureAndPersist(
            image: image,
            entityRef: entity,
            context: ctx,
            caption: caption
        )
    }

    /// Persist a captured / picked image atomically — pre-entity
    /// (wizard / walkthrough) flow. ``entityRef`` is nil because no
    /// SwiftData row exists yet; the returned Photo carries the
    /// pending-state stamp and the caller is responsible for
    /// associating it with the entity once it's created.
    ///
    /// ``saveAuditCopy`` controls whether a stamped duplicate is
    /// written back to the user's Photos library. Default is ``true``
    /// (camera path — bytes only exist in-app, audit copy prevents
    /// the Tenafly-style loss that motivated ZP-2230). Gallery
    /// imports pass ``false`` — the original is already in the user's
    /// library, so re-saving a stamped duplicate is redundant and
    /// would force an ``addOnly`` permission grant the user shouldn't
    /// need just to import an image.
    @MainActor
    static func captureAndPersistPending(
        image: UIImage,
        context: PhotoStampContext,
        caption: String? = nil,
        saveAuditCopy: Bool = true
    ) async throws -> Photo {
        return try await captureAndPersist(
            image: image,
            entityRef: nil,
            context: context,
            caption: caption,
            saveAuditCopy: saveAuditCopy
        )
    }

    /// Shared core path. Both public overloads route through here.
    /// ``entityRef`` is the existing SwiftData row (if any) so the
    /// resulting Photo can be linked to it directly; nil for
    /// pre-entity captures (the caller will link the Photo later).
    @MainActor
    private static func captureAndPersist(
        image: UIImage,
        entityRef: (any EntityWithPhotos)?,
        context: PhotoStampContext,
        caption: String?,
        saveAuditCopy: Bool = true
    ) async throws -> Photo {
        // 1. Resize.
        let resized = image.resized(maxDimension: maxDimension) ?? image

        // 2. Encode the clean JPEG — always needed for the in-app
        // copy used for sync / S3 upload / report rendering.
        guard let cleanJpeg = resized.jpegData(compressionQuality: jpegQuality) else {
            throw PhotoCaptureServiceError.encodingFailed
        }

        // 3. Optional audit-copy save back to the user's Photos
        // library. Skipped for gallery imports — the original
        // already lives in the user's library, so re-saving a
        // stamped duplicate is redundant and would force a
        // permission grant the user shouldn't need just to import.
        if saveAuditCopy {
            // 3a. Render the stamped variant — burned-in metadata
            // is for the user's Photos-library audit copy only.
            let stampLines = PhotoStampService.stampLines(context: context)
            let stamped = PhotoStampService.stamp(resized, lines: stampLines)
            guard let stampedJpeg = stamped.jpegData(compressionQuality: jpegQuality) else {
                throw PhotoCaptureServiceError.encodingFailed
            }

            // 3b. Permission re-check — UI gate is the primary
            // defense but a denial can happen mid-flow (user
            // revoked it in Settings while the camera was open).
            // Fail closed.
            guard PhotoLibraryService.shared.canWrite else {
                throw PhotoCaptureServiceError.permissionDenied
            }

            // 3c. Write the stamped copy to the Photos library
            // FIRST so the durable off-app audit copy is guaranteed
            // before we touch local disk. If the library save fails
            // we abort with no on-disk file created.
            do {
                try await PhotoLibraryService.shared.saveJPEG(stampedJpeg)
            } catch let err as PhotoLibraryServiceError {
                if case .permissionDenied = err {
                    throw PhotoCaptureServiceError.permissionDenied
                }
                throw PhotoCaptureServiceError.libraryWriteFailed(err)
            } catch {
                throw PhotoCaptureServiceError.libraryWriteFailed(error)
            }
        }

        // 4. Now write the CLEAN JPEG to the app's local cache. This
        // is the version the upload pipeline ships to S3 and reports
        // render against; the stamp lives only in the user's Photos
        // library as the audit-recovery copy.
        let photoId = UUID()
        let filename = "\(photoId.uuidString).jpg"
        let fileURL: URL
        do {
            fileURL = try saveJPEGToDocuments(cleanJpeg, filename: filename)
        } catch {
            // The library copy is durable so the bytes aren't lost
            // — we just can't queue this for upload right now.
            // Surface the failure so the UI can decide what to do.
            throw PhotoCaptureServiceError.diskWriteFailed(error)
        }

        // 7. Build + return the Photo row. Photo's init carries
        // explicit slots for every supported parent entity; pass
        // through the one that matches (if any) and leave the rest
        // nil. Pre-entity captures pass nil for entityRef — the
        // caller will associate the Photo with the eventual entity.
        let sldRef = entityRef.map(sld(for:)) ?? context.sld
        let photo = Photo(
            id: photoId,
            node: (entityRef as? NodeV2),
            userTask: (entityRef as? UserTask),
            issue: (entityRef as? Issue),
            building: (entityRef as? Building),
            floor: (entityRef as? Floor),
            room: (entityRef as? Room),
            url: nil,
            type: context.photoType,
            sld: sldRef,
            upload_needed: true,
            local_filepath: "photos/\(filename)",
            filename: filename,
            is_deleted: false,
            caption: caption
        )
        AppLogger.log(.info,
            "[PhotoCapture] persisted photo=\(photoId.uuidString.prefix(8)) kind=\(context.entityKind) file=\(fileURL.lastPathComponent)",
            category: .photo)
        return photo
    }

    /// ZP-1723: capture path for photos that ride INSIDE another
    /// payload (e.g. an EG form submission's `form_submission` JSONB)
    /// rather than as an independent Photo SwiftData row. Same audit
    /// guarantee as ``captureAndPersist`` — the user's Photos library
    /// gets the stamped copy first — but we don't create a Photo row,
    /// don't enqueue an upload, and don't write to ``Documents/photos``.
    /// The caller embeds the returned clean JPEG bytes (as base64) in
    /// its own payload; the backend's `extract_and_upload_media`
    /// promotes those to S3 references on submit.
    @MainActor
    static func captureForEmbed(
        image: UIImage,
        context: PhotoStampContext,
        caption: String? = nil
    ) async throws -> (cleanJPEG: Data, photoId: UUID) {
        let resized = image.resized(maxDimension: maxDimension) ?? image
        let stampLines = PhotoStampService.stampLines(context: context)
        let stamped = PhotoStampService.stamp(resized, lines: stampLines)

        guard let stampedJpeg = stamped.jpegData(compressionQuality: jpegQuality),
              let cleanJpeg = resized.jpegData(compressionQuality: jpegQuality) else {
            throw PhotoCaptureServiceError.encodingFailed
        }

        guard PhotoLibraryService.shared.canWrite else {
            throw PhotoCaptureServiceError.permissionDenied
        }

        do {
            try await PhotoLibraryService.shared.saveJPEG(stampedJpeg)
        } catch let err as PhotoLibraryServiceError {
            if case .permissionDenied = err {
                throw PhotoCaptureServiceError.permissionDenied
            }
            throw PhotoCaptureServiceError.libraryWriteFailed(err)
        } catch {
            throw PhotoCaptureServiceError.libraryWriteFailed(error)
        }

        return (cleanJpeg, UUID())
    }

    // MARK: - Helpers

    private static func saveJPEGToDocuments(_ data: Data, filename: String) throws -> URL {
        guard let documentsURL = FileManager.default
            .urls(for: .documentDirectory, in: .userDomainMask).first else {
            throw NSError(domain: "PhotoCaptureService", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "No documents directory"])
        }
        let folder = documentsURL.appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let url = folder.appendingPathComponent(filename)
        try data.write(to: url, options: .atomic)
        return url
    }

    private static func sld(for entity: any EntityWithPhotos) -> SLDV2? {
        if let n = entity as? NodeV2 { return n.sld }
        if let i = entity as? Issue { return i.sld }
        if let t = entity as? UserTask { return t.sld }
        if let b = entity as? Building { return b.sld }
        if let f = entity as? Floor { return f.building?.sld }
        if let r = entity as? Room { return r.floor?.building?.sld }
        return nil
    }
}
