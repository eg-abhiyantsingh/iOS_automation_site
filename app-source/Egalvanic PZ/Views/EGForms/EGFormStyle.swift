//
//  EGFormStyle.swift
//  Egalvanic PZ
//
//  ZP-1723: shared style constants for the V2 EG form renderer. Every
//  value here is mirrored from the web `EGFormRendererV2.jsx` (MUI sx
//  props translated to SwiftUI). The goal is pixel-close parity with
//  the web preview at iPhone/iPad widths — same font sizes, spacing,
//  border radii, and palette so a form authored on web reads the same
//  on device.
//
//  When in doubt, check the web source first: do not invent new
//  numbers here.
//

import SwiftUI
import UIKit

enum EGFormStyle {
    // MARK: - Type scale (web uses 12–13pt; iOS callout/caption are off)
    static let labelFont:   Font = .system(size: 12, weight: .medium)
    static let bodyFont:    Font = .system(size: 13)
    static let bodyBold:    Font = .system(size: 13, weight: .semibold)
    static let smallFont:   Font = .system(size: 11)
    static let smallStrong: Font = .system(size: 11, weight: .semibold)
    static let titleFont:   Font = .system(size: 16, weight: .semibold)

    // MARK: - Spacing (MUI 1 = 8px)
    static let blockGap:    CGFloat = 16     // MUI mb: 2 between blocks
    static let labelGap:    CGFloat = 4      // MUI mb: 0.5 under a label
    static let panelPad:    CGFloat = 16     // MUI p: 2 inside panel/well
    static let panelHdrX:   CGFloat = 16     // px: 2 header pad
    static let panelHdrY:   CGFloat = 8      // py: 1  header pad
    static let panelRadius: CGFloat = 12     // MUI borderRadius: 1.5 (well/msg)
    static let panelRadiusLg: CGFloat = 16   // MUI borderRadius: 2 (panel)

    // MARK: - Palette
    //
    // Adaptive colors via UIKit semantic + opacity tints so the
    // renderer respects light/dark mode automatically. The earlier
    // build had hardcoded slate-50 / amber-100 / etc., which read as
    // dark gray-on-dark-gray in dark mode. SwiftUI's `Color.blue`,
    // `.orange`, `.green`, `.red` are themselves dynamic — they shift
    // between light and dark variants — and combining them with a
    // ~0.15 opacity tint over the system background produces sensible
    // wells in either mode.
    static let panelBg     = Color.clear
    static let panelHdrBg  = Color(UIColor.secondarySystemBackground)
    static let panelBorder = Color(UIColor.separator)
    static let inputBorder = Color(UIColor.separator)
    static let calcChipBg  = Color.yellow.opacity(0.22)
    static let calcChipFg  = Color.orange

    struct WellPalette {
        let background: Color
        let border:     Color
        let text:       Color

        static func forVariant(_ v: String?) -> WellPalette {
            switch (v ?? "info").lowercased() {
            case "warning":
                return .init(background: Color.orange.opacity(0.15),
                             border:     Color.orange.opacity(0.4),
                             text:       Color.orange)
            case "success":
                return .init(background: Color.green.opacity(0.15),
                             border:     Color.green.opacity(0.4),
                             text:       Color.green)
            case "error":
                return .init(background: Color.red.opacity(0.15),
                             border:     Color.red.opacity(0.4),
                             text:       Color.red)
            case "neutral":
                return .init(background: Color(UIColor.secondarySystemBackground),
                             border:     Color(UIColor.separator),
                             text:       Color.primary)
            default: // "info"
                return .init(background: Color.blue.opacity(0.15),
                             border:     Color.blue.opacity(0.4),
                             text:       Color.blue)
            }
        }
    }
}

// MARK: - Hex color helper

extension Color {
    /// Initialize from a CSS hex string ("#RRGGBB" or "#RRGGBBAA"). Falls
    /// back to .secondary on parse failure rather than crashing — the
    /// renderer prefers a slightly-wrong color over a runtime trap on
    /// malformed form definitions.
    init(hex string: String) {
        var s = string
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6 || s.count == 8, let v = UInt64(s, radix: 16) else {
            self = .secondary; return
        }
        let r, g, b, a: Double
        if s.count == 6 {
            r = Double((v & 0xFF0000) >> 16) / 255
            g = Double((v & 0x00FF00) >> 8)  / 255
            b = Double(v & 0x0000FF)         / 255
            a = 1
        } else {
            r = Double((v & 0xFF000000) >> 24) / 255
            g = Double((v & 0x00FF0000) >> 16) / 255
            b = Double((v & 0x0000FF00) >> 8)  / 255
            a = Double(v & 0x000000FF)         / 255
        }
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

// MARK: - HTML → plain text (text / message blocks)

enum EGFormHTML {
    /// Reduce form-author HTML to a clean plain-text string suitable
    /// for SwiftUI `Text`. Native rendering, zero parsing libraries,
    /// no WebKit. Forms get the structural cues that actually matter
    /// (paragraph breaks, list bullets, line breaks); everything else
    /// (bold/italic/links) is stripped — the renderer is for legibility,
    /// not document fidelity. If a form needs rich formatting we'd
    /// reach for a real component, not an HTML escape hatch.
    static func plain(_ html: String) -> String {
        var s = html

        // Lists → bulleted / numbered lines.
        s = rewriteBlock(s, pattern: #"<ol[^>]*>([\s\S]*?)</ol>"#) { inner in
            "\n" + renderListItems(inner) { idx, content in "\(idx + 1). \(content)" } + "\n"
        }
        s = rewriteBlock(s, pattern: #"<ul[^>]*>([\s\S]*?)</ul>"#) { inner in
            "\n" + renderListItems(inner) { _, content in "•  \(content)" } + "\n"
        }

        // Paragraph + line breaks.
        s = s.replacingOccurrences(of: #"<br\s*/?>"#, with: "\n", options: [.regularExpression, .caseInsensitive])
        s = s.replacingOccurrences(of: #"<p[^>]*>"#, with: "", options: [.regularExpression, .caseInsensitive])
        s = s.replacingOccurrences(of: #"</p\s*>"#, with: "\n\n", options: [.regularExpression, .caseInsensitive])

        // Drop everything else still in tags.
        s = s.replacingOccurrences(of: #"<[^>]+>"#, with: "", options: .regularExpression)

        // Entities.
        s = s.replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;",  with: "&")
            .replacingOccurrences(of: "&lt;",   with: "<")
            .replacingOccurrences(of: "&gt;",   with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;",  with: "'")

        // Collapse runs of 3+ newlines to a paragraph break.
        s = s.replacingOccurrences(of: #"\n{3,}"#, with: "\n\n", options: .regularExpression)
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func renderListItems(_ inner: String, marker: (Int, String) -> String) -> String {
        guard let regex = try? NSRegularExpression(pattern: #"<li[^>]*>([\s\S]*?)</li>"#, options: [.caseInsensitive]) else { return inner }
        let nsr = inner as NSString
        let matches = regex.matches(in: inner, options: [], range: NSRange(location: 0, length: nsr.length))
        var lines: [String] = []
        for (idx, m) in matches.enumerated() where m.numberOfRanges >= 2 {
            let content = nsr.substring(with: m.range(at: 1))
                .trimmingCharacters(in: .whitespacesAndNewlines)
            // Strip any inline tags inside the <li> too.
            let cleaned = content.replacingOccurrences(of: #"<[^>]+>"#, with: "", options: .regularExpression)
            lines.append(marker(idx, cleaned))
        }
        return lines.joined(separator: "\n")
    }

    private static func rewriteBlock(_ s: String, pattern: String, transform: (String) -> String) -> String {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return s }
        let nsr = s as NSString
        let matches = regex.matches(in: s, options: [], range: NSRange(location: 0, length: nsr.length))
        guard !matches.isEmpty else { return s }
        var output = ""
        var cursor = 0
        for m in matches where m.numberOfRanges >= 2 {
            let whole = m.range(at: 0)
            let inner = m.range(at: 1)
            output += nsr.substring(with: NSRange(location: cursor, length: whole.location - cursor))
            output += transform(nsr.substring(with: inner))
            cursor = whole.location + whole.length
        }
        output += nsr.substring(with: NSRange(location: cursor, length: nsr.length - cursor))
        return output
    }
}

