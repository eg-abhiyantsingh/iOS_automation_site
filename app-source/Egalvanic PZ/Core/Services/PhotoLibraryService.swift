//
//  PhotoLibraryService.swift
//  Egalvanic PZ
//
//  ZP-2230: Photos-library write authorization + save.
//
//  Field techs need an off-app durable copy of every photo they
//  capture. The Documents/photos cache has historically been wiped
//  by offload/reinstall scenarios, leaving the sync queue with
//  references to photos whose bytes no longer exist on disk and
//  never made it to S3 (see Brightview Tenafly incident, ZP-2230
//  background notes). To prevent that we save every captured photo
//  to the user's Photos library in parallel with the in-app cache.
//
//  Authorization is treated as a hard requirement: if the user
//  hasn't granted ``addOnly`` Photos access, camera capture is
//  refused at the UI layer (no silent failures). This service
//  exposes the check + request flow and the actual library write.
//
import Foundation
import Photos
import UIKit

enum PhotoLibraryServiceError: LocalizedError {
    case permissionDenied(PHAuthorizationStatus)
    case writeFailed(Error)

    var errorDescription: String? {
        switch self {
        case .permissionDenied(let s):
            return "Photos library write access is required but was \(s). Enable it in Settings → Privacy → Photos → Egalvanic PZ → Add Photos Only."
        case .writeFailed(let err):
            return "Failed to write photo to Photos library: \(err.localizedDescription)"
        }
    }
}

final class PhotoLibraryService {
    static let shared = PhotoLibraryService()
    private init() {}

    /// Current authorization for ``addOnly`` access (the minimum the
    /// app needs — we never read from the user's library).
    var currentAuthorization: PHAuthorizationStatus {
        PHPhotoLibrary.authorizationStatus(for: .addOnly)
    }

    /// True when the app may successfully write a photo right now.
    var canWrite: Bool {
        currentAuthorization == .authorized
    }

    /// Ask for ``addOnly`` access if not yet decided; return the
    /// resulting status. Re-prompting is a no-op for ``denied`` /
    /// ``restricted`` — iOS only surfaces the system prompt on
    /// ``notDetermined``.
    @discardableResult
    func requestAuthorizationIfNeeded() async -> PHAuthorizationStatus {
        let current = currentAuthorization
        if current != .notDetermined { return current }
        return await withCheckedContinuation { cont in
            PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
                cont.resume(returning: status)
            }
        }
    }

    /// Save the JPEG bytes to the user's Photos library. Resolves
    /// only after the asset has actually been created on-device, so
    /// callers can rely on durability before continuing. Throws
    /// ``PhotoLibraryServiceError.permissionDenied`` if access isn't
    /// granted and ``.writeFailed`` if PHPhotoLibrary fails the save.
    func saveJPEG(_ data: Data) async throws {
        guard canWrite else {
            throw PhotoLibraryServiceError.permissionDenied(currentAuthorization)
        }
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            PHPhotoLibrary.shared().performChanges {
                let request = PHAssetCreationRequest.forAsset()
                request.addResource(with: .photo, data: data, options: nil)
            } completionHandler: { success, error in
                if success {
                    cont.resume()
                } else {
                    cont.resume(throwing: PhotoLibraryServiceError.writeFailed(
                        error ?? NSError(
                            domain: "PhotoLibraryService",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "PHPhotoLibrary returned no error but reported failure"]
                        )
                    ))
                }
            }
        }
    }
}
