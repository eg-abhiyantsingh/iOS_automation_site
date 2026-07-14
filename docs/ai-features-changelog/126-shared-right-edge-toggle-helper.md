# 126 — right-edge toggle press promoted to a shared BasePage helper

**Date:** 2026-07-14

- `BasePage.pressToggleRightEdge(WebElement)` — the canonical toggle press
  (W3C at row right edge, click fallback). Wired into:
  - `AssetPage.toggleRequiredFieldsOnly` (replaces the inline changelog-125 block)
  - `IssuePage.tapRequiredFieldsToggle` strategy 1 (was raw click() — the
    TC_ISS_061/170/172 toggle family in the failing Issues cluster)
- `AssetPage.toggleMarkAsCompleted` already used a right-side mobile:tap —
  same lesson learned independently; left as-is.
- Live validation on v1.50: ATS_EAD_14 round-trip — toggle reads
  ON('1') after enable AND OFF('0') after disable via the shared helper.
- Rule (memory + helper javadoc): SwiftUI toggle rows expose ONE element
  spanning the whole row; the thumb is at the RIGHT EDGE. Never click(),
  never center-press a toggle.
