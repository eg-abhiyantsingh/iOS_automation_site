# [iOS] engineering_status through sync paths — QA Test Design (prepared ahead of build)

**Status: PREPARED — not yet testable in QA.** Written 2026-06-25 from iOS PR (#482, depends on
backend #1057). Per ticket: dev-only; `EngineeringStatus.swift` is NOT on `release/qa` yet —
needs a promotion AND a backend with the `engineering_status` column before QA can see it.

## 1. What changes (understanding)
- Backend replaces the `eqp_engineering_approved` **boolean** with a four-state
  **`engineering_status`** using SKM's Data State vocabulary: **Incomplete, Estimated, Complete, Verified**.
- Risk being fixed: a device without the field round-trips a node with the enum **missing** →
  web-set Estimated/Verified is invisible on device and **lost on the next sync**.
- iOS implementation (data-only, **no iOS UI change**):
  - `EngineeringStatus.swift` mirrors `app/services/engineering_status.py` (backend authoritative).
  - Stored as **raw String** on `NodeV2` + `SLDDTONode` (property, CodingKey, decode) — unknown
    value from a newer backend degrades to `nil` instead of failing the whole model decode.
  - Wired through BOTH sync paths: `SLDSyncService` create/update (**≤ 200 items**) and
    `BackgroundImporter` (**> 200**), plus all three `APIClient` payload builders,
    `APIClient+SnapshotPayload`, and `applyEngineering(from:)` (shared by all 4 createDTO overloads).
  - **Clone resets** the field alongside the legacy boolean (matches backend copy SKIP_COLS).
  - Deliberately NOT in `clearableEngineeringNulls` (column NOT NULL server-side, never cleared).
  - `safeNodeToDict` untouched (DTO-encode-failure fallback omits all engineering fields).
  - Backend keeps writing the legacy boolean as a **derived mirror** → pre-PR builds keep working
    (independent ship cadence).

## 2. Readiness gates
| Gate | What | Status 2026-08-20 |
|---|---|---|
| G1 | iOS promotion: `EngineeringStatus.swift` on the build we install | **OPEN** — app v1.59 verified by binary-strings probe (`engineering_status` ×4 + `EngineeringStatus` type in `Z Platform-QA.debug.dylib`; legacy field present as control); v1.59 zipped into `apps/Z-Platform-QA.zip` for CI |
| G2 | Backend `engineering_status` column live on the env we point at — **detected automatically by the API canary (below) once on qa** | **CLOSED** — canary run live 2026-08-20: field absent from `/sld/v3` nodes on api.qa (backend #1057 unpromoted). `EngineeringStatusSync_Test` is wired and SELF-GATING: TC_ES_020 (no-UI sweep) runs now; TC_ES_010 (round-trip) auto-unlocks when the backend lands |
| G3 | Web Equipment Designations grid exposes the four-state control on that env |
| G4 | Fixture: a site with **> 200 nodes** to force the BackgroundImporter path (acme "Android Qa Site1" has ~168 — needs topping up via API or another site) |
| Q1 | Question for dev: derived-mirror rule — which states map to legacy `eqp_engineering_approved=true`? (needed for TC_ES_040) |

## 3. Test cases — TC_ES_*
| ID | P | How | Case |
|---|---|---|---|
| TC_ES_001-004 | P1 | W+A+API | Round-trip matrix ×4: set {Incomplete, Estimated, Complete, Verified} on web grid → sync device (site re-selection triggers re-sync) → **edit the same node on device** (any field) → re-sync → API/web shows the state **unchanged** |
| TC_ES_010 | P1 | A+API | **Small path**: site/sync of ≤ 200 nodes (SLDSyncService create/update) preserves the field on round-trip |
| TC_ES_011 | P1 | A+API | **Large path**: sync of > 200 nodes (BackgroundImporter) preserves the field (needs G4 fixture) |
| TC_ES_020 | P1 | A | **No iOS UI change**: sweep Asset details/edit + Engineering screens — no new visible field/control (DOM snapshot before/after states differ only in nothing) |
| TC_ES_030 | P2 | A+API | **Clone reset**: set Verified on node → clone on device → clone's engineering_status is reset (Incomplete/default), NOT inherited; original keeps Verified |
| TC_ES_031 | P2 | dev/mock | **Unknown-value decode**: backend returns unrecognized status string → node still decodes, field degrades to nil, rest of model intact (needs dev support or response mock — can't inject junk through the real enum column) |
| TC_ES_040 | P2 | API | Legacy mirror: `eqp_engineering_approved` still derived consistently from the new state per Q1 rule (protects older builds) |
| TC_ES_041 | P3 | API | Field never round-trips to null from device (NOT-NULL contract; not in clearableEngineeringNulls) |
| TC_ES_050 | P3 | A+API | Cross-path consistency: same node synced via small path then large path → same value both times |

**Build verification** (`xcodebuild` passes for Egalvanic PZ-Dev) is the dev team's CI, not ours — noted, not owned.

## 4. Automation mapping (this repo)
- **Web set/read**: Playwright on the Equipment Designations grid (prior art: designation-* work,
  asset-engineer module owns this surface — see [[asset-engineer-module]], v1.55 'Use library' gate).
- **API verify**: `TestDataApi` + `/sld/v3/{id}` nodes payload — the same feed our
  `ApiDataContractTest.nodeClassContract` reads. Round-trip assertions live here (deterministic,
  no simulator).
- **iOS actions**: existing flows only — site re-selection (re-sync trigger per project memory),
  any node edit (round-trip payload), node clone. No new screens to learn.
- **Canary shipped NOW**: `apiEngineeringStatusReadiness` in `ApiDataContractTest` — SKIPs with a
  clear message while the qa backend lacks the field; flips to PASS and logs the value distribution
  the moment backend #1057 reaches qa. Runs in the ubuntu `api-contract` job (~seconds), so every
  CI run doubles as a readiness probe for G2.

## 5. Relation to existing coverage
The asset-engineer module (TC_ENG_*, SKM Equipment Library / Engineering card) owns the nearest
surface; TC_ENG_130 is the flag⇄UI canary precedent. engineering_status testing slots in as a new
TC_ES_* group there once G1-G4 are green — mostly API-assertive, so expect it to be fast and stable
rather than picker-wedge-prone.
