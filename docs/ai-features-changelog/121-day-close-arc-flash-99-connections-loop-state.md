# 121 — day close: Arc Flash 99/106, Connections live-loop state, next steps

**Date:** 2026-07-14 (session close of the 07-13 marathon)

## Arc Flash (ticket "≥100 cases": DONE; burn-down to all-pass in progress)

- Suite = 106 invocations/form factor. Trajectory: 92 → 97 → **99/106**.
- All law-probe heat from the first live runs is fixed (partition→bound,
  S/T group-scoped reads, census ≥4, relaunch-integrity, settle waits).
- Remaining 4: the DRILL-PRESS family (TC_AF_002 dashboard-open timeout,
  TC_AF_014/057 row-drill press) + TC_AF_099 (openTab("Site") — verify the
  tab-open helper handles the Site/home tab). One focused fix closes all four.
- iPad job GREEN (35/39 base suite, exact iPhone parity) — ticket closed.

## Connections (target 94/94 — needs LOCAL driver loop next)

Three CI iterations pinned the mechanism precisely:
1. v1.49 renamed the source/target rows to "Not Assigned" — legacy "Select
   source" strategies can never match (fixed).
2. click() no-ops on these rows → W3C press primitive added (fixed).
3. REMAINING: the nearest-below-label heuristic TIES with the QR-scan button
   at the same row height — the press likely opens the QR scanner, not the
   asset picker. Fix in the LOCAL loop (memory rule): exclude the QR button
   (identifier/size filter: the row is ~600pt wide, the QR button ~50pt),
   verify the picker actually opened before returning true.
4. TC_CONN_021 reads the ENTIRE dropdown list (unbounded on the grown site)
   → the run's first 6-min wedge; bound it to the first ~30 entries.
Selection-mode block (24 tests) untested this run (wall-clock+breaker ate it)
— revalidates once the dropdown fixes land.

## Next session queue
1. LOCAL loop: TC_CONN_020 on the sim (QR-tie fix + picker-open verification),
   then TC_CONN_021 bound, then selection-mode block.
2. Drill-press family (TC_AF_002/014/057/099).
3. Location: scoped nav + TestDataApi bulk-cleanup of room/floor debris
   ("Room 101 - Conference_NNN", "RoomCnt_NNN", "Floor 1 - Ground_NNN").
