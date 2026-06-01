//
//  MatchResultsPanel.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 4b — match-results card list inside the engineering
//  section. Mirrors web's "X possible matches" panel: header line
//  with the count + optional "Add Custom" button, then a stack of
//  match cards. A search bar appears unconditionally for relays
//  (their only narrowing axis), or once the user has typed into it,
//  or once the result set is large enough to warrant filtering.
//
import SwiftUI

struct MatchResultsPanel: View {
    let matches: [SkmMatch]
    let truncated: Bool
    /// "circuit_breaker" / "fuse" / "relay" / "cable" / "busway" /
    /// "transformer" — drives search bar gating + accent.
    let typeName: String?
    @Binding var searchText: String
    /// Tap-to-pick handler. Tied up in Phase 4B-3 (writes eqp_lib +
    /// back-fills null first-class fields). For now the parent can
    /// stub it.
    var onPick: (SkmMatch) -> Void
    /// ZP-2267: opens the Add Custom sheet. Optional so callers that
    /// don't support custom entries (none today, but future-proof)
    /// can pass nil and hide the button.
    var onAddCustom: (() -> Void)? = nil

    @State private var visibleLimit = 10
    private let pageSize = 10

    private var totalCount: Int { matches.count }

    private var showSearchBar: Bool {
        // Relay-style classes have no other narrowing axis — show the
        // search bar unconditionally. ZP-2420: transformer behaves the
        // same (only manufacturer + free-text are meaningful filters).
        if typeName == "relay" || typeName == "transformer" { return true }
        if !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        if totalCount > 3 { return true }
        if truncated { return true }
        return false
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header line — count + truncation hint + Add Custom.
            HStack {
                Text(countLabel)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.secondary)
                Spacer()
                if let onAddCustom {
                    addCustomButton(onAddCustom: onAddCustom)
                }
            }

            if showSearchBar {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField(AppStrings.Engineering.matchSearchPlaceholder, text: $searchText)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    if !searchText.isEmpty {
                        Button {
                            searchText = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }

            if matches.isEmpty {
                Text(searchText.isEmpty
                     ? AppStrings.Engineering.noMatchesRefine
                     : AppStrings.Engineering.noMatchesSearch)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.vertical, 8)
            } else {
                VStack(spacing: 8) {
                    ForEach(matches.prefix(visibleLimit)) { match in
                        MatchCard(match: match)
                            .onTapGesture { onPick(match) }
                    }
                    if matches.count > visibleLimit {
                        Button {
                            visibleLimit += pageSize
                        } label: {
                            Text(AppStrings.Engineering.loadMore(matches.count - visibleLimit))
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color(.systemGray6))
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(12)
        .background(Color.blue.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.blue.opacity(0.15), lineWidth: 1)
        )
        .cornerRadius(12)
        .onChange(of: matches.count) { _, _ in
            // Reset paging when the result set churns (e.g. user
            // narrows filters and the list shrinks below the current
            // limit).
            visibleLimit = pageSize
        }
    }

    private var countLabel: String {
        if totalCount == 0 { return AppStrings.Engineering.noPossibleMatches }
        return AppStrings.Engineering.possibleMatchesCount(totalCount, truncated: truncated)
    }

    /// ZP-2267: Add Custom button. When there are zero matches we
    /// promote it to the filled variant so it reads as the obvious
    /// next action; otherwise it sits as a subtle outlined chip
    /// beside the count. Label stays "Add Custom" in both states.
    @ViewBuilder
    private func addCustomButton(onAddCustom: @escaping () -> Void) -> some View {
        Button(action: onAddCustom) {
            HStack(spacing: 4) {
                Image(systemName: "plus.circle\(matches.isEmpty ? ".fill" : "")")
                    .font(.caption)
                Text(AppStrings.Engineering.addCustom)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .lineLimit(1)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .foregroundColor(matches.isEmpty ? .white : .blue)
            .background(matches.isEmpty ? Color.blue : Color.clear)
            // ZP-2421 review #9: ``.strokeBorder`` draws the stroke
            // inside the path so the outer cornerRadius clip doesn't
            // shave off the half-line at the rounded corners.
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(Color.blue, lineWidth: matches.isEmpty ? 0 : 1)
            )
            .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

/// Single match row.
///
/// ZP-2420: cable / busway rows use a compact two-line layout
/// (header: ``manufacturer · size · @voltage``; subtitle: dot-joined
/// attributes) that mirrors the web ``AssetFormFields`` rendering.
/// The raw ``sz_name`` from SKM is an unreadable concatenated string
/// (e.g. "CopperBuswayClass BEpoxy600GenericBUSSandwich225-5000A")
/// so we synthesize the display from individual fields instead.
///
/// Transformer rows (ZP-2514) use the same compact two-line layout
/// as cable/busway, mirroring the web. Protective-device rows keep
/// the original two-column manufacturer/type/style + tail layout,
/// which fits their data shape better.
private struct MatchCard: View {
    let match: SkmMatch

    private var isCableOrBusway: Bool {
        match.slug == "cables-skm" || match.slug == "busway-skm"
    }
    private var isTransformer: Bool {
        match.slug == "transformers-skm"
    }

    var body: some View {
        Group {
            if isCableOrBusway {
                cableBuswayLayout
            } else if isTransformer {
                transformerLayout
            } else {
                deviceLayout
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(.systemBackground))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(.systemGray4), lineWidth: 0.5)
        )
        .cornerRadius(10)
        .contentShape(Rectangle())
    }

    // MARK: Cable / busway layout

    /// "MANUFACTURER · 600 A · @600V" on the top line, dot-joined
    /// attribute subtitle below.
    @ViewBuilder private var cableBuswayLayout: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Text(match.manufacturer.isEmpty ? "—" : match.manufacturer)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.blue)
                    .lineLimit(1)
                    .layoutPriority(0)
                if let size = sizeDisplay {
                    Text("·")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(size)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                        .layoutPriority(1)
                }
                if let voltage = voltageDisplay {
                    Text(voltage)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                        .layoutPriority(1)
                }
                Spacer(minLength: 0)
            }
            if let subtitle = attributeSubtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
    }

    private var sizeDisplay: String? {
        guard let s = match.matched_cable_size, !s.isEmpty else { return nil }
        if let unit = match.matched_cable_size_unit, !unit.isEmpty {
            return "\(s) \(unit)"
        }
        return s
    }

    private var voltageDisplay: String? {
        guard let v = match.voltage_rating, v.isFinite, v > 0 else { return nil }
        return "@\(Int(v.rounded()))V"
    }

    /// Web parity: ``conductor_type · duct_material · installation
    /// · insulation_class · insulation_type`` — dropping any field
    /// that's missing or empty. Mirrors ``AssetFormFields.jsx``
    /// ``subtitleBits`` array.
    private var attributeSubtitle: String? {
        let parts = [
            match.conductor_type,
            match.duct_material,
            match.installation,
            match.insulation_class,
            match.insulation_type
        ].compactMap { $0?.trimmingCharacters(in: .whitespaces) }
         .filter { !$0.isEmpty && $0 != "-" && $0 != "****" }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    // MARK: Transformer layout (ZP-2514 — web parity)

    /// "Manufacturer · str_type · kVA" on the top line, then
    /// "str_type_symbol · NΦ · R% X.XX · X% Y.YY" subtitle. Mirrors
    /// the web ``AssetFormFields.jsx`` transformer card subtitleBits.
    @ViewBuilder private var transformerLayout: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Text(match.manufacturer.isEmpty ? "Typical" : match.manufacturer)
                    .font(.footnote)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                if let strType = match.type, !strType.isEmpty {
                    Text("·")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    Text(strType)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                if let label = match.kva_label, !label.isEmpty {
                    Text("·")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                    Text(label)
                        .font(.footnote)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            if let subtitle = transformerSubtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
    }

    private var transformerSubtitle: String? {
        var parts: [String] = []
        if let sym = match.str_type_symbol, !sym.isEmpty {
            parts.append(sym)
        }
        if let p = match.phase, p > 0 {
            parts.append("\(p)Φ")
        }
        if let r = match.percentage_r, let x = match.percentage_x {
            parts.append(String(format: "R%% %.2f · X%% %.2f", r, x))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    // MARK: Protective-device layout

    /// ZP-2457: mirror web's single-line render
    /// (``AssetFormFields.jsx`` ~ line 5030):
    /// ``{manufacturer} — {type} — {style}{ — tail?}``. Manufacturer
    /// is semibold, everything else primary regular weight. Pole count
    /// is intentionally not surfaced separately — when the server
    /// includes it, it's already baked into ``style`` (e.g.
    /// "15-100A, 1 Pole, IEC").
    @ViewBuilder private var deviceLayout: some View {
        let suffix = [match.type, match.style, deviceTail]
            .compactMap { s -> String? in
                guard let s, !s.isEmpty else { return nil }
                return s
            }
            .joined(separator: " — ")
        (Text(match.manufacturer).fontWeight(.semibold)
         + (suffix.isEmpty ? Text("") : Text(" — \(suffix)")))
            .font(.subheadline)
            .foregroundColor(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .lineLimit(2)
    }

    /// Tail for protective-device + transformer rows (cable/busway
    /// rows use the cable/busway layout above instead).
    ///
    /// ZP-2457: protective-device branches mirror web's tail in
    /// ``AssetFormFields.jsx`` ~ line 4704:
    /// ``frame_desc || sensor+plug || sensor``. The ``matched_kva``
    /// branch stays first as the transformer fallback (web renders
    /// transformers through a separate block; ``matched_kva`` is only
    /// populated for transformer matches, so it never collides with
    /// the protective-device order). ``matched_trip_amp`` was dropped
    /// — web doesn't surface it on the match card.
    private var deviceTail: String? {
        if let kva = match.matched_kva {
            let v = kva.truncatingRemainder(dividingBy: 1) == 0
                ? "\(Int(kva))"
                : String(format: "%.1f", kva)
            return "\(v) kVA"
        }
        if let frame = match.frame_desc, !frame.isEmpty {
            return frame
        }
        if let s = match.matched_sensor_value, let p = match.matched_plug_value {
            return "\(Int(s))AS / \(Int(p))AP"
        }
        if let s = match.matched_sensor_value {
            return "\(Int(s))AS"
        }
        return nil
    }
}
