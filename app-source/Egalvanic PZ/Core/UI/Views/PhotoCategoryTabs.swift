//
//  PhotoCategoryTabs.swift
//  Egalvanic PZ
//
//  Shared photo-category tab components for bulk asset creation flows
//  (Quick Count, Photo Walkthrough). Mirrors the PhotoTypeChip / photo-type
//  selector used by EntityFancyPhotoPicker on the asset detail screen.
//

import SwiftUI

// MARK: - Categorized Staged Photo

/// Protocol adopted by the in-memory staged-photo models used by bulk
/// creation flows (QuickCountStagedPhoto, WalkthroughStagedPhoto) so that
/// shared category UI can render their thumbnails uniformly.
protocol CategorizedStagedPhoto {
    var id: UUID { get }
    var type: String { get }
    var localFileURL: URL { get }
}

// MARK: - Node Photo Category Catalog

enum PhotoCategory {
    /// The four photo categories for Node entities, mirroring
    /// `NodeV2.supportedPhotoTypes`. Computed so localized display names stay
    /// in sync with the current language bundle.
    static var nodeTypes: [PhotoTypeConfiguration] {
        [
            PhotoTypeConfiguration(
                type: "node_profile",
                displayName: AppStrings.Photos.typeProfile,
                icon: "photo",
                color: "blue"
            ),
            PhotoTypeConfiguration(
                type: "node_nameplate",
                displayName: AppStrings.Photos.typeNameplate,
                icon: "tag.fill",
                color: "orange"
            ),
            PhotoTypeConfiguration(
                type: "node_panel_schedule",
                displayName: AppStrings.Photos.typePanelSchedule,
                icon: "list.clipboard.fill",
                color: "green"
            ),
            PhotoTypeConfiguration(
                type: "node_arc_flash_sticker",
                displayName: AppStrings.Photos.typeArcFlashSticker,
                icon: "bolt.shield.fill",
                color: "yellow"
            )
        ]
    }

    static let defaultNodeType = "node_profile"
}

/// Localized label for a node photo type string. Falls back to the raw type
/// if unknown so unexpected values remain visible rather than silently empty.
func photoCategoryLabel(for type: String) -> String {
    PhotoCategory.nodeTypes.first(where: { $0.type == type })?.displayName ?? type
}

/// "3 Profile, 1 Nameplate" summary. Returns nil when the photos span a
/// single category (in which case the caller's total count already conveys
/// the information) or when there are no photos.
func photoCategoryBreakdown<P: CategorizedStagedPhoto>(for photos: [P]) -> String? {
    guard !photos.isEmpty else { return nil }
    let grouped = Dictionary(grouping: photos, by: { $0.type })
    guard grouped.count > 1 else { return nil }
    return PhotoCategory.nodeTypes.compactMap { config -> String? in
        let count = grouped[config.type]?.count ?? 0
        return count > 0 ? "\(count) \(config.displayName)" : nil
    }.joined(separator: ", ")
}

// MARK: - Photo Category Chips Row

/// Horizontally scrollable pill chips, one per photo category. Selecting a
/// chip updates `activePhotoType`; the inline count next to each label is
/// sourced from `countByType`.
struct PhotoCategoryChipsRow: View {
    @Binding var activePhotoType: String
    let countByType: [String: Int]
    var supportedTypes: [PhotoTypeConfiguration] = PhotoCategory.nodeTypes
    var horizontalPadding: CGFloat = 16

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(supportedTypes, id: \.type) { config in
                    PhotoTypeChip(
                        config: config,
                        isSelected: activePhotoType == config.type,
                        count: countByType[config.type] ?? 0,
                        onTap: {
                            activePhotoType = config.type
                        }
                    )
                }
            }
            .padding(.horizontal, horizontalPadding)
        }
    }
}

// MARK: - Categorized Staged Photo Strip

/// Compact chips-plus-thumbnails strip used in Quick Count photoset summary
/// cards. Mirrors the Android `CategorizedPhotoThumbnails` behavior but at
/// iOS-native sizing and chip styling. Keeps local active-tab state so the
/// caller just hands in a `[StagedPhoto]`.
struct CategorizedStagedPhotoStrip<P: CategorizedStagedPhoto>: View {
    let photos: [P]
    var thumbnailSize: CGFloat = 50
    var chipHorizontalPadding: CGFloat = 0

    @State private var activeType: String

    init(photos: [P], thumbnailSize: CGFloat = 50, chipHorizontalPadding: CGFloat = 0) {
        self.photos = photos
        self.thumbnailSize = thumbnailSize
        self.chipHorizontalPadding = chipHorizontalPadding

        let grouped = Dictionary(grouping: photos, by: { $0.type })
        let firstWithPhotos = PhotoCategory.nodeTypes.first(where: {
            !(grouped[$0.type]?.isEmpty ?? true)
        })?.type
        self._activeType = State(initialValue: firstWithPhotos ?? PhotoCategory.defaultNodeType)
    }

    private var photosByType: [String: [P]] {
        Dictionary(grouping: photos, by: { $0.type })
    }

    private var countByType: [String: Int] {
        photosByType.mapValues { $0.count }
    }

    private var activePhotos: [P] {
        photosByType[activeType] ?? []
    }

    var body: some View {
        if photos.isEmpty {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 8) {
                PhotoCategoryChipsRow(
                    activePhotoType: $activeType,
                    countByType: countByType,
                    horizontalPadding: chipHorizontalPadding
                )

                if !activePhotos.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(activePhotos, id: \.id) { photo in
                                if let uiImage = UIImage(contentsOfFile: photo.localFileURL.path) {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: thumbnailSize, height: thumbnailSize)
                                        .clipped()
                                        .cornerRadius(6)
                                }
                            }
                        }
                    }
                } else {
                    Text(AppStrings.Forms.noEntityAttributes("\(photoCategoryLabel(for: activeType).lowercased()) photos"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .frame(height: thumbnailSize)
                }
            }
        }
    }
}
