# 051 — ZP-323.10 Asset Listening: Real iOS Locators From v1.31 Screenshots

**Date**: 2026-04-30
**Trigger**: User sent 2 screenshots showing the Task Details screen on v1.31, with the message:
> "if you create a issue and the issue should be open then you will see the option."

This closes the **last remaining ZP-323 gap**. All 15 features now have iOS-evidence-based locators.

---

## Evidence From The Screenshots

Both screenshots show **Task Details** (Cancel header `Close | Task Details`, status pill `🕐 Open` orange). Below the `Linked Assets` card sits a small pill button:

| State | Label | Visual |
|---|---|---|
| **Active** (screenshot 1) | `Listening for Assets` | Orange-filled pill, headphones icon |
| **Inactive** (screenshot 2) | `Listen for Assets` | Gray pill, headphones icon |

User confirmed the precondition: **the parent Issue must be in `Open` status** for the option to appear.

The button is rendered as a single `XCUIElementTypeButton` whose `label` toggles between the two strings — not a separate Stop button.

---

## What This Tells Us About The Feature

- Listen lives on **Task Details**, not Asset list (my earlier guess was wrong).
- It is conceptually a "Listen for **nearby** Assets" auto-link feature — likely BLE proximity scan or continuous QR/NFC — that adds matches to the Task's `Linked Assets` list.
- Toggle pattern: tap once → "Listening for Assets" (active). Tap again → "Listen for Assets" (inactive).
- No dedicated `Stop` button — the same pill toggles state.

---

## Locator Changes

### [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java)

| Method | Before | After |
|---|---|---|
| `tapListenAsset()` | `label == 'Listen' OR label CONTAINS 'start listening'` | `label == 'Listen for Assets' OR label == 'Listening for Assets'` (with old strings as fallback) |
| `isListeningIndicatorActive()` | StaticText/Image with 'listening' substring | Button or StaticText with `Listening for Assets` (matches the active-state label) |
| `isListenForAssetsButtonAvailable()` (new) | — | Probe to confirm the button exists at all (precondition for the test) |
| `stopListeningIfActive()` | Looked for `Stop` / `Stop Listening` button | Now checks active state first, then taps the pill again to toggle off (matches v1.31 reality) |

Compile clean. No new tests, no API breaks — `tapListenAsset()` and `isListeningIndicatorActive()` keep their public signatures.

---

## Final ZP-323 Confidence Scorecard

| # | Feature | Status |
|---|---|---|
| 1 | AI Extraction | 🟢 sparkles button on Core Attributes |
| 2 | Edit Site - long press | 🟢 SwiftUI context menu |
| 3 | Create Asset - Detailed | 🟢 web-verified sections |
| 4 | Issue Safety/Notification | 🟢 |
| 5 | Copy to / Copy from | 🟢 ⋯ overflow menu |
| 6 | Connection Core Attributes | 🟢 |
| 7 | T&C checkbox | 🟡 inferred (CI run #25150966937 SKIPped — needs build with checkbox) |
| 8 | COM (Maintenance state) | 🟢 |
| 9 | Suggested Shortcuts | 🟢 |
| 10 | Issue IR Photos | 🟢 |
| 11 | IR Upload in Work Order | 🟢 |
| 12 | Schedule on Work Order | 🟢 |
| 13 | Edge properties in Connection | 🟢 |
| 14 | Long Press Building/Room | 🟢 SwiftUI context menu |
| 15 | **Asset Listening** | 🟢 **NEW** — Task Details pill toggle |

**Final tally**: 14 verified iOS, 1 inferred (T&C checkbox awaits a build that ships it). Zero remaining 🔴 unknowns.

---

## Recommended Next Steps

1. **Trigger CI** — `assets-p1` and `issues-p1` workflows on `main`. The new evidence-based locators should now hit real elements; failures will reflect product behavior, not locator drift.
2. **For the Listening test specifically** (TC_ZP323_10_*): test must first create an Issue in **Open** status, navigate into a Task, then probe `isListenForAssetsButtonAvailable()`. If false, `skipIfPreconditionMissing("Listen pill not visible — task may not be in Open state")`.
3. **Move on to Asset_Phase6 vacuous test triage** — 94 tests, biggest assertion-coverage lever remaining.
