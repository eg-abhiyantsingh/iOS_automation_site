# 127 — v1.50 dashboard redesign: Work Orders entry remapped (tile, badge-first name)

**Date:** 2026-07-14

- v1.50 REMOVED the "No Active Job" card; Work Orders entry = Quick Actions
  tile whose folded name puts the badge FIRST: **"107, Work Orders"**
  (DOM-dump-proven). `clickWorkOrderCard()` now tries the tile first
  (CONTAINS 'Work Orders', visible, y>120, h>40) with the legacy v1.49
  WO-card loop as fallback. TC_JOB_002 red -> GREEN in 3s on v1.50.
- Dashboard redesign also adds: greeting header, stat tiles, Schedule tile
  (NEW - candidate coverage), floating QR button. Recorded in memory with the
  general rule: badge counts fold FIRST -> CONTAINS, never BEGINSWITH.
- v1.50 smoke: 22/26 -> expected ~25/26 after this fix (TC_ENG_070 already
  evicted by the parallel session per the flake-twice rule).
