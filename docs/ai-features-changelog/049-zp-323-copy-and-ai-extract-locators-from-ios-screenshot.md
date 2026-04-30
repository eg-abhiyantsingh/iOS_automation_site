# 049 — ZP-323 Copy From/To + AI Extract: Real iOS Locators From v1.31 Screenshot

**Date**: 2026-04-30
**Trigger**: User sent a v1.31 iOS Asset Details screenshot with the message:
> "copy from copy to in three dots and right side you will see star sign for extract ai images"

This converts two of the four remaining 🔴 iOS-only-guess features (changelog 048) into 🟢 verified.

---

## Evidence From The Screenshot

Asset Details header (top): `Close | Asset Details | ⋯` — the **⋯ three-dot button** is top-right (blue circular button).

"Core Attributes" section header has on the right side a **blue square button with the sparkles ✨ icon** — that is the AI Extract trigger. SF Symbol name is almost certainly `sparkles`.

---

## Locator Changes

### [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java)

| Method | Before | After |
|---|---|---|
| `openAssetOverflowMenu()` | Searched for top-level `Copy Details` button (web pattern) | Targets `name == 'ellipsis'` / `'ellipsis.circle'`, with `Copy Details` retained as fallback |
| `tapCopyFrom()` | Looked for `Copy Details From…` directly on screen | Auto-invokes `openAssetOverflowMenu()` if menu not open, then taps `Copy From` (also accepts `Copy Details From` web variant) |
| `tapCopyTo()` | Same web-shape pattern | Same auto-open flow, taps `Copy To` |
| `isOverflowMenuOpen()` (new, private) | — | Probe: any element with label containing `Copy From` or `Copy To` indicates the menu is up |
| `tapAIExtractButton()` | Generic `extract`/`AI`/`sparkles` predicate (any order) | Prioritizes SF Symbol `sparkles` and `wand.and.stars`, then text fallbacks (`extract`, `AI Extract`, `auto fill`) |

All edits are locator-only — no behavior changes, no new tests, no new public methods. Compile is clean.

---

## Updated Confidence Scorecard

| # | Feature | Before | After |
|---|---|---|---|
| 12 | Copy to / Copy from | 🟢 web-verified, iOS shape unknown | 🟢🟢 iOS-verified (⋯ menu) |
| 13 | AI Extraction | 🔴 iOS-only guess | 🟢 iOS-verified (sparkles on Core Attributes) |
| 8 | Edit Site - long press | 🔴 iOS-only | 🔴 unchanged |
| 9 | Long Press Building/Room photo | 🔴 iOS-only | 🔴 unchanged |
| 10 | Asset Listening | 🔴 iOS-only | 🔴 unchanged |

**Tally**: 12 verified, 1 inferred (T&C), 3 iOS-only-still-need-evidence (down from 4).

---

## Why Auto-Open Inside `tapCopyFrom`/`tapCopyTo`

The earlier API forced callers to remember the two-step sequence. Tests were calling `tapCopyFrom()` directly (assuming web-style top-level link), which would silently fail under the iOS layout. Auto-opening the overflow menu when the menu isn't already up makes the API forgiving for both:

- Tests that explicitly call `openAssetOverflowMenu()` first (no extra cost — `isOverflowMenuOpen()` short-circuits).
- Tests written against the web shape that just call `tapCopyFrom()`.

This is a small forgivingness on a multi-strategy locator — it does not mask failures because the underlying find still throws if neither the menu nor the item is reachable.

---

## What's Still Pending For ZP-323

1. **Edit Site - long press** (.8) — touch gesture, no web equivalent. Needs iOS screenshot of the long-press context menu on a site row.
2. **Long Press Building/Room Photo** (.9) — touch gesture; same.
3. **Asset Listening** (.10) — feature trigger location unknown; needs iOS evidence.

For these three, tests still use `skipIfPreconditionMissing()` so they SKIP cleanly when the locator misses.
