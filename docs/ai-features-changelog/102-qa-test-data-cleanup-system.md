# 102 — QA test-data cleanup system (the CI/QA slowness root cause)

**Date:** 2026-07-03
**User ask:** "can you fix this issue make also our ci cd faster"

## The audit (live against api.qa.egalvanic.ai)
The automation site "(s) Wild Goose Brewery" (`9138fd14…`) had accumulated months of
CI debris:

| Collection | rows in sync payload | live | live junk |
|---|---|---|---|
| nodes (assets) | 1,310 | 14 (after cleanup) | was 45 live + 1,251 already soft-deleted |
| buildings | 451 | 384 | **371** (MultiSync_/Test Building/DoubleTapTest… epoch-tailed) |
| floors | 402 | 397 | **361** (TestFloor_/Floor_epoch…) |
| rooms | 59 | 59 | 31 |
| tasks | 780 | 727 | **596** (Test Task/Completed Task epoch…) |
| ir_sessions (work orders) | 82 | 82 | 0 touched (all flagged active — left alone) |

This is the **giant-DOM / WDA-wedge root cause**: 384 buildings + 397 floors is the
"Locations giant tree" that times out whole-tree queries at 90s, and every /sld/v3
sync ships 1,310 asset rows even though only 14 are alive.

## What shipped
1. **`.github/scripts/qa-data-cleanup.py`** — reusable cleanup:
   - soft-delete only (`is_deleted=true`, reversible), via the app's own endpoints
     (`/node/update`, `/location/{building,floor,room}/{id}`, `/task/update`,
     `/ir_session/update`)
   - strict automation-name signatures (13-digit epoch tails + known Test* families)
   - **48h in-flight guard** (never deletes rows created in the last 2 days — a CI
     run may be using fixtures it just created)
   - work orders: only inactive + pre-current-month (none qualified today)
   - every deletion appended to an undo log (JSONL); `DRY_RUN=1` audit mode
2. **`.github/workflows/qa-data-cleanup.yml`** — weekly (Sun 02:00 UTC) + manual
   dispatch with dry-run option; uploads the undo log as an artifact (90d retention).
3. Executed live 2026-07-03 (undo log kept): assets 45 + XSS-named straggler,
   buildings/floors/rooms/tasks per the table above.

## Backend findings to report to the dev team
- `/auth/v2/me` no longer returns `accessible_sld_ids` for the admin account and
  `/users/{id}/slds` returns `[]` — this is why the CI "API Contract" job has been
  red; the workaround is `GET /company/{companyId}/slds` (what the web app uses).
- `/sld/v3/{id}` ships ALL rows including `is_deleted=true` ones (1,296 of 1,310
  asset rows were tombstones) — sync payloads and app-side DBs carry ~100× dead
  weight. Server-side tombstone compaction would cut sync time dramatically.
- `POST /node/update/{id}` returns 405 — the update endpoints are PUT-only.

## Expected impact
- Locations tree shrinks from ~384 buildings to ~13 → the whole-tree queries that
  wedged WDA (LocationTest, SiteVisit Locations tab) operate on a 30× smaller tree.
- Site-load and sync get lighter as the backend compacts (or at minimum stop growing).
- Recurring weekly cleanup prevents regrowth.
