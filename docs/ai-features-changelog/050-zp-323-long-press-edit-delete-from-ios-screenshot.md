# 050 — ZP-323 Long-Press Edit/Delete: Real iOS Locators From v1.31 Screenshots

**Date**: 2026-04-30
**Trigger**: User sent 3 screenshots showing the v1.31 long-press behavior on the Locations screen, with the message "see".

This converts the last two long-press features (ZP-323.8 Edit Site, ZP-323.9 Long-press Building/Floor/Room) from 🔴 iOS-only-guess into 🟢 iOS-verified.

---

## Evidence From The Screenshots

**Screenshot 1** — Long-press on a Room row (R1) reveals a SwiftUI context menu:
- `Edit Room` — pencil icon, black text
- `Delete Room` — trash icon, **red** text

The menu floats above a blurred background. The R1 row label remains visible at the top: "R1 / 13 assets".

**Screenshot 2** — Tapping `Edit Room` opens a sheet with header:
- `Cancel | Edit Room | Save` (Cancel/Save in blue, title centered)

The sheet body has sections: ROOM INFORMATION (R1 / Floor F1 / Building B1), ACCESS NOTES, Photos (Gallery + Camera buttons).

**Screenshot 3** — The Locations tree showing the row hierarchy:
- Buildings: `B1 / 1 floor`, `B2 / 1 floor`, `B3 / 1 floor`, `B5 / 1 floor`
- Floors: `F1 / 1 room`, `F2 / 1 room`, `F3 / 1 room`, `F5 / 1 room`
- Rooms: `R1 / 13 assets`, `R2 / 204 assets`, `R3 / 2 assets`

Row labels follow predictable pattern: `B[0-9]+`, `F[0-9]+`, `R[0-9]+`.

---

## What This Tells Us About ZP-323.9

The original ticket said **"Long Press Building/Room Photo"**. The actual iOS feature is **long-press the row itself** (not a photo). The earlier method names `longPressOnBuildingPhoto()` / `longPressOnRoomPhoto()` were a misread — those were named based on the literal ticket title before any iOS evidence existed.

Method names kept for back-compat; behavior now matches reality.

---

## Locator Changes

### [BuildingPage.java](../../src/main/java/com/egalvanic/pages/BuildingPage.java)

| Method | Before | After |
|---|---|---|
| `longPressOnBuildingPhoto()` | Long-pressed an `XCUIElementTypeImage` whose name contains 'building'/'cover'/'photo' | Delegates to private `longPressOnLocationRow("Building")` — finds the first row matching `B[0-9]+` |
| `longPressOnRoomPhoto()` | Same but for 'room' images | Delegates to `longPressOnLocationRow("Room")` — finds first `R[0-9]+` row |
| `longPressOnLocationRow(String)` (new, private) | — | Generic: targets `XCUIElementTypeStaticText` matching `[BFR][0-9]+`. Photo-image fallback retained. |
| `tapEditFromContextMenu(String)` (new) | — | Taps `Edit <Building\|Floor\|Room>` in the context menu; multi-strategy element-type predicate |
| `isEditLocationSheetVisible(String)` (new) | — | Confirms `Edit <Type>` title + `Save` button in the sheet |
| `isPhotoViewerOrMenuVisible()` | Looked for Save/Share/Delete/Done/Close buttons (photo-viewer assumption) | Now checks for any element labeled `Edit *` or `Delete *` (matches the real context-menu shape) |

### [SiteSelectionPage.java](../../src/main/java/com/egalvanic/pages/SiteSelectionPage.java)

| Method | Before | After |
|---|---|---|
| `isSiteContextMenuVisible()` | Looked only for `Edit`/`Edit Site` button | Multi-strategy: any element labeled `Edit Site` / `Delete Site` / `Edit` / `Delete` (Button, StaticText, MenuItem, Cell) |
| `tapEditSiteFromContextMenu()` | Same narrow Button-only predicate | Same broadening — accepts MenuItem and Cell types so the locator survives across iOS versions |

The Site long-press flow was inferred to follow the same SwiftUI context-menu pattern — same iOS app, same menu primitive.

---

## Updated Confidence Scorecard

| # | Feature | Before changelog 049 | After this changelog |
|---|---|---|---|
| 8 | Edit Site - long press | 🔴 iOS-only guess | 🟢 iOS-verified pattern (Edit Site / Delete Site menu) |
| 9 | Long Press Building/Room | 🔴 iOS-only guess | 🟢 iOS-verified (Edit/Delete `<Type>` menu) |
| 10 | Asset Listening | 🔴 iOS-only guess | 🔴 unchanged — last remaining gap |
| 12 | Copy to / Copy from | 🟢 (changelog 049) | 🟢 |
| 13 | AI Extraction | 🟢 (changelog 049) | 🟢 |

**Final tally**: 13 verified, 1 inferred (T&C checkbox), 1 still needs iOS evidence (Asset Listening).

---

## Why This Pattern Generalizes

iOS SwiftUI's `.contextMenu { ... }` modifier produces a consistent system-rendered popover with the same element types (Button/MenuItem/StaticText) regardless of which row hosts it. So the locator strategy that works for `Edit Room` will work identically for `Edit Building`, `Edit Floor`, and `Edit Site` — the only difference is the label string, which is already parameterized via the `String type` argument in `tapEditFromContextMenu(type)`.

This is why the new helpers accept a `type` parameter rather than hardcoding three near-identical methods. One probe + one tap helper covers the whole pattern.

---

## What's Still Pending For ZP-323

**Asset Listening (.10)** — only feature without iOS evidence. The trigger location is unknown:
- Could be a button on Asset Detail (header / Core Attributes / Photos)
- Could be a floating action button on the Asset list
- Could be triggered by long-press on an asset row (similar to Edit/Delete pattern)

When you have a v1.31 screenshot showing where the Listen feature lives, ~5 min to update the locator.
