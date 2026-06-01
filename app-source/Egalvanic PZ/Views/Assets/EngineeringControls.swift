//
//  EngineeringControls.swift
//  Egalvanic PZ
//
//  ZP-2161 Phase 3b — engineering-section controls re-skinned to wrap
//  the shared ``ModernPicker`` / ``ModernSegmentedPicker`` /
//  ``ModernTextField`` primitives. Same visual language as the rest
//  of the asset form (caption title on top, systemGray6 background,
//  rounded corner, etc.) instead of the inline label/value HStack we
//  shipped initially.
//
//  Bug fix: ``ConductorMaterialPicker`` used to show ``Cu`` visually
//  selected when the underlying value was nil (via ``value ?? "Copper"``
//  in the binding getter). That meant a user could open a fresh cable,
//  see Cu highlighted, hit Save, and silently persist ``conductor_material = null``.
//  We now bind to ``String?`` directly so nil renders as no-selection —
//  forcing an explicit tap before save.
//
import SwiftUI
import SwiftData

// MARK: - Numeric text fields

/// Numeric text field bound to an ``Int?``. Empty string clears the
/// value; non-numeric characters are dropped.
struct EngineeringIntField: View {
    let label: String
    @Binding var value: Int?
    var suffix: String? = nil
    /// SF Symbol shown to the left of the field. Empty by default —
    /// most engineering fields don't have a clean symbol match, and
    /// the caption-title above the input already names the field.
    /// Amperage callers (Frame, Trip, Sensor, Plug, Fuse Amperage,
    /// Busway Size, etc.) override to ``"bolt.horizontal"``; voltage
    /// callers use ``"bolt"`` to match the voltage card.
    var icon: String = ""

    @State private var text: String = ""

    private var binding: Binding<String> {
        Binding(
            get: { text },
            set: { new in
                let digits = new.filter { $0.isNumber }
                text = digits
                value = digits.isEmpty ? nil : Int(digits)
            }
        )
    }

    var body: some View {
        ModernTextField(
            title: label,
            text: binding,
            icon: icon,
            trailingContent: suffix.map { s in
                {
                    AnyView(
                        Text(s)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    )
                }
            },
            keyboardType: .numberPad
        )
        .onAppear { text = value.map(String.init) ?? "" }
        .onChange(of: value) { _, new in
            let expected = new.map(String.init) ?? ""
            if text != expected { text = expected }
        }
    }
}

/// Numeric text field bound to a ``Double?``. Accepts decimals.
struct EngineeringDoubleField: View {
    let label: String
    @Binding var value: Double?
    var suffix: String? = nil
    var icon: String = ""

    @State private var text: String = ""

    private static func format(_ v: Double) -> String {
        v.truncatingRemainder(dividingBy: 1) == 0
            ? String(Int(v))
            : String(v)
    }

    private var binding: Binding<String> {
        Binding(
            get: { text },
            set: { new in
                // Allow digits + a single dot. Strip everything else
                // so e.g. paste of "1,200" coerces to "1200".
                var seenDot = false
                let filtered: String = new.reduce(into: "") { acc, ch in
                    if ch.isNumber {
                        acc.append(ch)
                    } else if ch == "." && !seenDot {
                        seenDot = true
                        acc.append(ch)
                    }
                }
                text = filtered
                value = filtered.isEmpty ? nil : Double(filtered)
            }
        )
    }

    var body: some View {
        ModernTextField(
            title: label,
            text: binding,
            icon: icon,
            trailingContent: suffix.map { s in
                {
                    AnyView(
                        Text(s)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    )
                }
            },
            keyboardType: .decimalPad
        )
        .onAppear {
            text = value.map(Self.format) ?? ""
        }
        .onChange(of: value) { _, new in
            let expected = new.map(Self.format) ?? ""
            if text != expected { text = expected }
        }
    }
}

// MARK: - Enum picker (dropdown via ModernPicker)

/// Wraps any Int-id enum (``EnumNodeTripType``, ``EnumSkmManufacturer``,
/// etc.) in a ``ModernPicker``. Caller passes:
///   - ``items`` — the @Query'd list
///   - ``selectedId`` — Binding<Int?> on the draft field
///   - ``idOf`` — usually ``\.id`` but can return any uniquely-identifying
///     int (used for ``EnumBuswayAmpereRating`` where the saved column is
///     ``ampere_value`` rather than the FK ``id``).
///   - ``labelOf`` — display text.
struct EngineeringEnumPicker<Item: Identifiable & Hashable>: View {
    let label: String
    let items: [Item]
    @Binding var selectedId: Int?
    let idOf: (Item) -> Int
    let labelOf: (Item) -> String
    var icon: String = ""
    var allowClear: Bool = true
    /// Switch to a searchable sheet when ``items`` is large enough that
    /// scanning the Menu becomes unpleasant. Manufacturer (~50 rows) is
    /// the main client. Default threshold matches what we use elsewhere.
    var useSheetThreshold: Int = 20

    private var selection: Binding<EnumPickerOption?> {
        Binding(
            get: {
                guard let id = selectedId,
                      let match = items.first(where: { idOf($0) == id })
                else { return nil }
                return EnumPickerOption(id: id, label: labelOf(match))
            },
            set: { selectedId = $0?.id }
        )
    }

    private var options: [EnumPickerOption] {
        items.map { EnumPickerOption(id: idOf($0), label: labelOf($0)) }
    }

    var body: some View {
        ModernPicker(
            title: label,
            icon: icon,
            placeholder: AppStrings.Engineering.selectEllipsis,
            items: options,
            selection: selection,
            displayName: { $0.label },
            allowClear: allowClear,
            useSheet: items.count > useSheetThreshold
        )
    }
}

/// Identifiable shim so any Item-with-int-id can flow through
/// ``ModernPicker<T: Identifiable & Hashable>`` without forcing the
/// underlying @Model to implement custom identifiability.
private struct EnumPickerOption: Identifiable, Hashable {
    let id: Int
    let label: String
}

// MARK: - Segmented pickers (conductor material, pole count)

/// Cu / Al segmented control. Underlying value is ``String?`` so nil
/// renders as no-selection — fixing the bug where a fresh cable showed
/// Cu visually selected but saved ``conductor_material = null``.
struct ConductorMaterialPicker: View {
    @Binding var value: String?

    private static let options: [String?] = ["Copper", "Aluminum"]

    var body: some View {
        ModernSegmentedPicker(
            title: AppStrings.Engineering.conductorMaterial,
            items: Self.options,
            selection: $value,
            displayContent: { opt in
                .init(label: opt == "Copper" ? "Cu" : "Al")
            }
        )
    }
}

/// Trip-type ("Type") picker. Matches web's ``ToggleButtonGroup``
/// when ``items.count <= segmentedThreshold`` and falls back to a
/// dropdown when the eligible set is too large to render comfortably.
/// Web always uses a toggle; on a phone-width screen, ≥ 6 toggles
/// crowd, so we switch to the dropdown above the threshold.
struct TripTypePicker: View {
    let label: String
    let items: [EnumNodeTripType]
    @Binding var selection: Int?
    var segmentedThreshold: Int = 5

    private var items_: [Int?] { items.map { Optional($0.id) } }

    var body: some View {
        if items.count <= segmentedThreshold && !items.isEmpty {
            ModernSegmentedPicker(
                title: label,
                items: items_,
                selection: $selection,
                displayContent: { opt in
                    let name = items.first(where: { $0.id == opt })?.display_name ?? "—"
                    return .init(label: name)
                },
                columns: items.count > 3 ? 2 : nil
            )
        } else {
            EngineeringEnumPicker(
                label: label,
                items: items,
                selectedId: $selection,
                idOf: { $0.id },
                labelOf: { $0.display_name }
            )
        }
    }
}

/// 1/2/3 segmented picker for ``pole_count``. ``style`` flips the
/// labels — ``.pole`` → "1P / 2P / 3P" (breakers), ``.count`` → plain
/// "1 / 2 / 3" (fuses). Items are non-nil so nil selection = nothing
/// highlighted, matching web's null-aware ToggleButtonGroup.
struct PoleCountPicker: View {
    enum Style { case pole, count }

    let label: String
    @Binding var value: Int?
    var style: Style = .pole
    var max: Int = 3

    private var items: [Int?] {
        (1...max).map { Optional($0) }
    }

    var body: some View {
        ModernSegmentedPicker(
            title: label,
            items: items,
            selection: $value,
            displayContent: { opt in
                let n = opt ?? 0
                return .init(label: style == .pole ? "\(n)P" : "\(n)")
            }
        )
    }
}
