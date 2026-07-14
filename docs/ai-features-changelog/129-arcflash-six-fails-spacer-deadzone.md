# 129 — Arc-flash six-fail triage: SwiftUI Spacer dead-zone, Site-tab bug, collisions

**Date:** 2026-07-14
**Prompt:** "TC_AF_002/014/083/099/100 + TC_ENG_130 … this all are fall fix this"

## Verdicts (each reproduced or exonerated locally, one at a time)

| Test | Verdict | Action |
|---|---|---|
| TC_AF_002 | ENV — passed locally (49s) | none |
| TC_AF_083 | ENV — passed locally (1m38s) | none |
| TC_AF_100 | ENV — passed locally (1m24s); the listed fail + one local fail were mutual sim-session collisions (two Appium sessions on one simulator kill each other) | none; coordinate sim usage |
| TC_AF_014 | AUTOMATION_BUG ×2 — **FIXED, now passes (27s)** | see below |
| TC_AF_099 | AUTOMATION_BUG ×2 — **FIXED, now passes (1m55s)** | see below |
| TC_ENG_130 | Passed locally (14s): eng-lib ENABLED per API (verified directly via /auth/v2/me — 29 flags incl. eng-lib), no disabled caption, Load alert appears. ENG-FLAG-DRIFT-01 did not reproduce; stays a watch item | none |

## TC_AF_014 — the drill-through that never worked on iOS

1. Row at the bottom fold: expanding the 100% bucket rendered its first row at
   y≈930 of 956 — in the DOM, "visible", unpressable. Added a breakdown nudge
   + safe-zone filter.
2. **SwiftUI Spacer hit-test dead zone (root cause, app-source-verified):**
   the breakdown row is `Button { HStack { Text… Spacer() Text } }` with
   PlainButtonStyle and NO `.contentShape(Rectangle())` — only the visible
   text areas receive taps; the geometric CENTER of the row (the Spacer) is
   dead. W3C press, click(), and mobile:tap at center all no-oped on three
   different rows with perfect geometry. Fix: tap the asset-name text zone
   (r.x+30, center-y). First successful drill-through recorded on iOS.
   Instrumentation kept: per-candidate rect logging + widest-per-name
   (phantom-twin guard) + next-row fallback.

## TC_AF_099 — openTab("Site") was pressing the Connections tab

`openTab`'s ternary only knew Assets/Connections; "Site" fell into the else
branch and pressed CONNECTIONS. Added SITE_TAB and an explicit mapping. Second
layer: the v1.50 Site home has NO 'Site' nav bar (title = "Welcome to <site>"),
so the landed-predicate for Site is now structural (Quick Actions / Welcome-to).

## Memory

- critical-patterns-ios.md: new "SwiftUI Spacer hit-test DEAD ZONE" pattern —
  suspect it for any "press no-ops on a real enabled Button" symptom.
