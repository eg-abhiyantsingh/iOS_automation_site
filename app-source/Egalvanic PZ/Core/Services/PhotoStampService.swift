//
//  PhotoStampService.swift
//  Egalvanic PZ
//
//  ZP-2230: Render an audit-recovery metadata stamp into the
//  bottom-left corner of a captured photo.
//
//  Lines (top → bottom, fixed order):
//    line 1 — timestamp, e.g. "2026-05-15 15:40"
//    line 2 — asset label + short node id, e.g.
//             "Asset 266 Sub Asset A · fecbc4b6"
//    line 3 — location breadcrumb, e.g.
//             "Brightview Tenafly › 2nd Floor › Main Switchgear Room"
//
//  Rendered as a single rounded pill of semi-opaque black behind
//  white text so the stamp stays readable against any photo
//  background. Sized proportional to the image so the pill looks
//  the same whether the source is 4032×3024 or 1024×768. The
//  rendering is pure Core Graphics — no UIKit dependencies on the
//  rendering side beyond UIImage I/O, so it can be exercised in
//  unit tests if needed.
//
import UIKit

/// ZP-2230: describes everything the stamp needs about the photo's
/// owning entity. Supports both the "entity exists already" case
/// (asset details, building/floor/room editors, issue detail) and
/// the "entity will be created from this wizard" case (walkthroughs,
/// quick count, add-asset). For wizard flows the caller assigns a
/// fresh ``photosetId`` once per session so every photo captured in
/// that flow carries the same visible group id in the burned-in
/// stamp + Photos-library copy.
struct PhotoStampContext {
    var entityKind: String      // "Node", "Building", "Pending Asset", "Walkthrough", …
    var entityLabel: String?    // user-typed name, photo-type fallback, or nil
    var shortId: String?        // existing entity uuid prefix; nil pre-save
    var photoType: String       // "node_nameplate", "issue", "ir_metadata", …
    var photosetId: UUID?       // groups captures from a single wizard session
    /// ZP-2230: sub-index for nested capture flows. For a photo
    /// walkthrough the same ``photosetId`` belongs to one parent
    /// device; each OCP child captured under it gets a sub-index
    /// (1, 2, 3…). The stamp renders ``set abc12345-2`` for child
    /// #2 of parent set ``abc12345``. Nil for top-level captures.
    var photosetSubIndex: Int?
    var sld: SLDV2?
    var building: Building?
    var floor: Floor?
    var room: Room?

    /// Build a stamp context from an already-persisted entity.
    /// Used by every "entity exists" capture surface — entity-detail
    /// pickers, issue / task editors, location editors.
    static func from(entity: any EntityWithPhotos, photoType: String) -> PhotoStampContext {
        var ctx = PhotoStampContext(
            entityKind: entityKindLabel(for: entity),
            entityLabel: nil,
            shortId: nil,
            photoType: photoType,
            photosetId: nil,
            sld: nil,
            building: nil,
            floor: nil,
            room: nil
        )

        let (label, sid) = labelAndShortId(for: entity)
        ctx.entityLabel = label
        ctx.shortId = sid.isEmpty ? nil : sid

        ctx.sld = sld(for: entity)
        let (b, f, r) = parents(for: entity)
        ctx.building = b
        ctx.floor = f
        ctx.room = r
        return ctx
    }

    /// Build a stamp context for a pre-entity capture (walkthrough,
    /// quick count, OCP wizard, add-asset). The eventual entity
    /// doesn't exist yet so ``shortId`` is nil; ``photosetId`` lets
    /// the user trace photos in the Photos library back to a single
    /// wizard session.
    static func pending(
        kind: String,
        label: String?,
        photoType: String,
        photosetId: UUID,
        photosetSubIndex: Int? = nil,
        sld: SLDV2?,
        building: Building?,
        floor: Floor?,
        room: Room?
    ) -> PhotoStampContext {
        return PhotoStampContext(
            entityKind: kind,
            entityLabel: label,
            shortId: nil,
            photoType: photoType,
            photosetId: photosetId,
            photosetSubIndex: photosetSubIndex,
            sld: sld,
            building: building,
            floor: floor,
            room: room
        )
    }

    // MARK: - Helpers (mirror those used internally by PhotoStampService)

    fileprivate static func entityKindLabel(for entity: any EntityWithPhotos) -> String {
        if entity is NodeV2 { return "Node" }
        if entity is Issue { return "Issue" }
        if entity is UserTask { return "Task" }
        if entity is Building { return "Building" }
        if entity is Floor { return "Floor" }
        if entity is Room { return "Room" }
        return ""
    }

    fileprivate static func labelAndShortId(for entity: any EntityWithPhotos) -> (String, String) {
        if let n = entity as? NodeV2 {
            let l = n.label.isEmpty ? "(unlabeled asset)" : n.label
            return (l, String(n.id.uuidString.prefix(8)).lowercased())
        }
        if let i = entity as? Issue {
            let l = (i.title ?? "").isEmpty ? "(issue)" : i.title!
            return (l, String(i.id.uuidString.prefix(8)).lowercased())
        }
        if let t = entity as? UserTask {
            let l = t.title.isEmpty ? "(task)" : t.title
            return (l, String(t.id.uuidString.prefix(8)).lowercased())
        }
        if let b = entity as? Building {
            let l = b.name.isEmpty ? "(building)" : b.name
            return (l, String(b.id.uuidString.prefix(8)).lowercased())
        }
        if let f = entity as? Floor {
            let l = f.name.isEmpty ? "(floor)" : f.name
            return (l, String(f.id.uuidString.prefix(8)).lowercased())
        }
        if let r = entity as? Room {
            let l = r.name.isEmpty ? "(room)" : r.name
            return (l, String(r.id.uuidString.prefix(8)).lowercased())
        }
        return ("(unknown)", "")
    }

    fileprivate static func sld(for entity: any EntityWithPhotos) -> SLDV2? {
        if let n = entity as? NodeV2 { return n.sld }
        if let i = entity as? Issue { return i.sld }
        if let t = entity as? UserTask { return t.sld }
        if let b = entity as? Building { return b.sld }
        if let f = entity as? Floor { return f.building?.sld }
        if let r = entity as? Room { return r.floor?.building?.sld }
        return nil
    }

    fileprivate static func parents(
        for entity: any EntityWithPhotos
    ) -> (Building?, Floor?, Room?) {
        if let n = entity as? NodeV2 {
            let r = n.room
            let f = r?.floor
            return (f?.building, f, r)
        }
        if let r = entity as? Room {
            return (r.floor?.building, r.floor, r)
        }
        if let f = entity as? Floor {
            return (f.building, f, nil)
        }
        if let b = entity as? Building {
            return (b, nil, nil)
        }
        if let i = entity as? Issue, let n = i.node {
            let r = n.room
            let f = r?.floor
            return (f?.building, f, r)
        }
        if let t = entity as? UserTask, let n = t.node {
            let r = n.room
            let f = r?.floor
            return (f?.building, f, r)
        }
        return (nil, nil, nil)
    }
}

enum PhotoStampService {
    /// Burn ``lines`` into a pill in the lower-left corner of
    /// ``image``. Returns the stamped image (or the original on a
    /// rendering failure — the stamp is best-effort overlay; the
    /// underlying durability guarantee is at the file-save layer).
    static func stamp(_ image: UIImage, lines: [String]) -> UIImage {
        guard !lines.isEmpty else { return image }
        let size = image.size
        let scale = image.scale
        // Pill text size — scaled by the shorter image side so the
        // overlay looks proportional on both phone (≈ 1024 px) and
        // raw camera (≈ 3000 px) captures.
        let shorter = min(size.width, size.height)
        let fontSize = max(11, min(28, shorter * 0.022))
        let font = UIFont.monospacedSystemFont(ofSize: fontSize, weight: .medium)
        let textColor = UIColor.white
        let pillColor = UIColor.black.withAlphaComponent(0.55)
        let hPad: CGFloat = fontSize * 0.65
        let vPad: CGFloat = fontSize * 0.5
        let lineGap: CGFloat = fontSize * 0.25
        let cornerRadius: CGFloat = fontSize * 0.45
        let edgeMargin: CGFloat = fontSize * 0.7

        let para = NSMutableParagraphStyle()
        para.lineBreakMode = .byTruncatingTail
        let attrs: [NSAttributedString.Key: Any] = [
            .font: font,
            .foregroundColor: textColor,
            .paragraphStyle: para,
        ]

        // Measure each line, clamp to a max width slightly smaller
        // than the image (so the pill never runs to the edges on
        // narrow captures).
        let maxTextWidth = size.width - (edgeMargin * 2) - (hPad * 2)
        struct Measured { let text: String; let size: CGSize }
        let measured: [Measured] = lines.map { line in
            let bounded = (line as NSString).boundingRect(
                with: CGSize(width: maxTextWidth, height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading],
                attributes: attrs,
                context: nil
            )
            return Measured(text: line, size: CGSize(
                width: min(maxTextWidth, ceil(bounded.width)),
                height: ceil(font.lineHeight)
            ))
        }
        let textWidth = measured.map { $0.size.width }.max() ?? 0
        let textHeight = measured.reduce(0) { $0 + $1.size.height }
            + CGFloat(max(0, lines.count - 1)) * lineGap

        let pillWidth = textWidth + hPad * 2
        let pillHeight = textHeight + vPad * 2
        let pillOrigin = CGPoint(
            x: edgeMargin,
            y: size.height - edgeMargin - pillHeight
        )

        let renderer = UIGraphicsImageRenderer(
            size: size,
            format: { let f = UIGraphicsImageRendererFormat(); f.scale = scale; return f }()
        )
        return renderer.image { ctx in
            image.draw(in: CGRect(origin: .zero, size: size))

            let pillRect = CGRect(origin: pillOrigin, size: CGSize(width: pillWidth, height: pillHeight))
            pillColor.setFill()
            UIBezierPath(roundedRect: pillRect, cornerRadius: cornerRadius).fill()

            var cursorY = pillOrigin.y + vPad
            for m in measured {
                let textRect = CGRect(
                    x: pillOrigin.x + hPad,
                    y: cursorY,
                    width: textWidth,
                    height: m.size.height
                )
                (m.text as NSString).draw(in: textRect, withAttributes: attrs)
                cursorY += m.size.height + lineGap
            }
            _ = ctx
        }
    }

    /// Build the canonical 3-line stamp content for a captured
    /// photo. Works for both existing-entity captures and pre-entity
    /// wizard captures (walkthrough, quick count, etc.) — wizard
    /// captures include a ``photosetId`` so the user can later
    /// trace photos in their Photos library back to a single
    /// wizard session.
    ///
    /// Format:
    ///   line 1 — "<timestamp>  ·  <photo type pretty>"
    ///   line 2 — "<entity kind>  ·  <entity label>  ·  <id or set id>"
    ///   line 3 — "<sld> › <building> › <floor> › <room>"
    static func stampLines(
        context ctx: PhotoStampContext,
        at date: Date = Date()
    ) -> [String] {
        var lines: [String] = []

        // Line 1: timestamp + photo type.
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm"
        let timestamp = df.string(from: date)
        let pretty = prettyPhotoType(ctx.photoType)
        lines.append(pretty.isEmpty ? timestamp : "\(timestamp)  ·  \(pretty)")

        // Line 2: entity kind + label + short id (or set id).
        let label = (ctx.entityLabel?.isEmpty == false) ? ctx.entityLabel! : "(unlabeled)"
        var line2 = ctx.entityKind.isEmpty ? label : "\(ctx.entityKind)  ·  \(label)"
        if let sid = ctx.shortId, !sid.isEmpty {
            line2 += "  ·  \(sid)"
        } else if let pid = ctx.photosetId {
            let shortPid = String(pid.uuidString.prefix(8)).lowercased()
            if let sub = ctx.photosetSubIndex {
                line2 += "  ·  set \(shortPid)-\(sub)"
            } else {
                line2 += "  ·  set \(shortPid)"
            }
        }
        lines.append(line2)

        // Line 3: location breadcrumb (skip when nothing resolves).
        let breadcrumb = locationBreadcrumb(ctx)
        if !breadcrumb.isEmpty {
            lines.append(breadcrumb)
        }

        return lines
    }

    /// Back-compat overload — existing call sites that still pass an
    /// entity directly. New surfaces should use ``stampLines(context:)``.
    static func stampLines(
        for entity: any EntityWithPhotos,
        photoType: String,
        at date: Date = Date()
    ) -> [String] {
        return stampLines(
            context: .from(entity: entity, photoType: photoType),
            at: date
        )
    }

    private static func locationBreadcrumb(_ ctx: PhotoStampContext) -> String {
        var parts: [String] = []
        if let s = ctx.sld?.name, !s.isEmpty { parts.append(s) }
        if let b = ctx.building, !b.name.isEmpty { parts.append(b.name) }
        if let f = ctx.floor, !f.name.isEmpty { parts.append(f.name) }
        if let r = ctx.room, !r.name.isEmpty { parts.append(r.name) }
        return parts.joined(separator: " › ")
    }

    /// Turn the raw photo-type slug ("node_nameplate",
    /// "node_arc_flash_sticker", "building", "ir_metadata", …) into
    /// a human-readable label for the stamp.
    private static func prettyPhotoType(_ raw: String) -> String {
        let cleaned = raw.hasPrefix("node_")
            ? String(raw.dropFirst("node_".count))
            : raw
        // Replace underscores with spaces and capitalize each word.
        let words = cleaned
            .replacingOccurrences(of: "_", with: " ")
            .split(separator: " ", omittingEmptySubsequences: true)
            .map { word -> String in
                let s = String(word)
                if s.lowercased() == "ir" { return "IR" }
                return s.prefix(1).uppercased() + s.dropFirst()
            }
        return words.joined(separator: " ")
    }

}
