# 155 — Full-suite triage: asset-debris burial (60-fail cluster) + search-first opener (2026-08-09)

## Full suite run 31214326457 (first with all fixes): triage findings
- Before rerun 296 fails → after in-workflow 3-shard rerun 234 (62 flakes
  correctly absorbed). Worktype: 77 → 9 fails vs 08-04. List slice fully
  GREEN for the first time.
- 205 deterministic re-fails classified by signature. Dominant cluster:
  **60 × "openAssetCardByPrefix: no asset cell starting with 'X'"** across
  AssetEngineer* (+ adjacent Asset_Phase/Connections fails).

## Root cause (screenshot + live API census)
The Assets list on Wild Goose drowned in auto-generated debris: 124 live
nodes, 67 debris (NoSubtype_*, PERSISTED_*, DupTest_*, 'MCC-1 (copy N)',
CaseTest_*, QRTest*, even the SQL-injection test's leftovers). Fixtures
(Test Busway / Transformer-1 / Trim600639 Fuse) sort under T — beyond
openAssetCardByPrefix's **4-swipe** scan bound. Same disease as the WO-list
burial (changelog 149), different list.

## Fixes
1. **Backend sweep**: 67 debris nodes soft-deleted via
   PUT /node/update/{id} {"is_deleted":true} + x-direct-write (verified
   applied — unlike ir_session, node update works). 124 → 57 live; all
   fixtures verified intact. Reusable: `scripts/sweep_debris_assets.py`
   (pattern-locked, --dry-run supported).
2. **Search-first opener**: openAssetCardByPrefix now types the prefix into
   the Assets search field first (O(1) at any list size), falls back to the
   swipe scan with the bound raised 4 → 10. Local validation: fresh-install
   sync → 'Engineering section must be reachable on Transformer-1' PASSED.
3. Note: TC_ENG_060 is a 6-row DataProvider (kVA filter cases) — its row
   fails are a separate small contract item, NOT the cluster.

## Next
Full suite re-dispatched for the report cycle.
