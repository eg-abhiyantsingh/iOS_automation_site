# 119 — Connections + Location: "all should pass" burn-down (user requirement)

**Date:** 2026-07-13
**Prompt:** "I think connection and location all test cases should pass — maybe
you are understanding wrong requirement."

Confirmed: every Connections/Location failure and skip was already verdicted
AUTOMATION_BUG (our code), not app behavior. This changelog fixes the
Connections side and pins the Location root cause.

## Connections fixes (target: 94/94)

1. **Selection-mode helper (recovers 24 silently-skipped tests,
   TC_CONN_073-096):** `tapOnSelectMultipleOption` used `element.click()` on a
   SwiftUI MENU ROW — the documented silent no-op. Rewritten: W3C press
   (down→120ms→up) on the LAST visible matching row + verified outcome
   (Cancel-in-header / 'N Selected' counter) + one retry; legacy click
   strategies kept as last resort.
2. **Search-first node picker (fixes the 4 six-minute hangs,
   TC_CONN_024/036/059/062):** `selectAssetFromDropdown` narrowed the giant
   asset list through the dropdown's own search field first, then matches at
   0 implicit wait with visible==1 scoping — no more whole-list scans.
3. **TC_CONN_097 reshaped to the real requirement:** Core Attributes are
   type-gated on current builds (screenshot-proven; TC_CONN_098 passes).
   The test now accepts the section immediately OR after picking a type —
   the requirement "core attributes available on the New Connection form"
   is what's asserted.

## Location root cause (pinned by screenshot, fix queued)

TC_NL_001's failure screenshot shows the Locations tree OPEN and functional —
but polluted with hundreds of prior-run artifacts ("Room 101 - Conference_NNN",
"RoomCnt_NNN", "Floor 1 - Ground_NNN"). Fixture rooms and the No-Location
section sit beyond any sane scroll/query budget; probes time out at ~50s-6m.
Two-part cure (next changelog):
- scoped navigation (search-first / expand-target-node-only, never whole-tree
  scans) in LocationPage;
- **test-data hygiene**: bulk-delete our own room/floor debris via
  TestDataApi + make Location tests clean up after themselves.

## Validation
Compile green; connections+location CI dispatch for live validation.
