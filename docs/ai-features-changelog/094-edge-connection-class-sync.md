# 094 — Edge/Connection class sync + node v12 / issue re-confirm (2026-06-22)

Per request: go deep on the three template files and update the test cases accordingly:
`edge_classes_template (3).xlsx`, `issue_classes_template.xlsx`, `node_classes_template (12) - updated.xlsx`.
Verified each against the **live web-app API**, not just the sheets (the sheets comma-split
option names that contain commas).

## Edge / Connections — the only file with NEW drift → fixed
Live `GET /api/edge_classes/user/{id}`: **3 real connection classes** — Busway, Cable, **DC Cable**
(+ 9 junk `DEVTOOL_TEST` rows). Ground truth: [edge-classes-ground-truth-2026-06-22.md](../edge-classes-ground-truth-2026-06-22.md).

Findings + fixes in `ConnectionsPage` / `Connections_Test`:
- **DRIFT:** `getConnectionTypeOptions().knownTypes` was `{Select type, Busway, Cable}` — **missing
  "DC Cable"**. Added it, so the option harvest finds the live 3rd type.
- Added `EXPECTED_CONNECTION_TYPES = {Busway, Cable, DC Cable}` + `BUSWAY_EDGE_FIELDS` (7) /
  `CABLE_EDGE_FIELDS` (9) constants sourced from the live API.
- **TC_CONN_032**: added a `DC Cable` presence check (soft, mirrors the Busway/Cable checks).
- **TC_CONN_099**: replaced the pass-anyway `labels.size() >= 1` assertion with a **spec-based
  check** — verifies Busway's own edge-property fields (Conductor Material, Length, Amperage of
  Busway, Wire Sizes…) actually render (`>= 2 of 7`, substring/case-insensitive so it tolerates
  iOS label rendering but fails for real if no Busway fields appear).
- Hardened `getCoreAttributeFieldLabels()` — its whole-tree StaticText scan now runs at
  implicit-wait 0 (same WDA-wedge guard as the picker batches).

Coverage still open (flagged): the per-type edge **field interaction** (filling Wire Size / Amperage
dropdowns, Cable's 9 fields, switching types) — bigger new tests for the connection-edit screen;
sequence after the picker fixes are CI-green (that screen can wedge too).

## Node v12 — already superseded (no action)
v12 lists the **old CB-MCCB boundary `250A`**; the newer v14 sheet AND the live API both say **225A**,
which the test code was already corrected to (changelog 090). v12 is stale on this point — keeping
225A. All other v12 classes/subtypes ⊆ live.

## Issue sheet — already in sync (no action)
Re-confirmed: all 7 sheet classes exist live; `EXPECTED_ISSUE_CLASSES` + subcategory expectations
match (changelog 093). The NEC multi-select coverage gap remains tracked.

## Validation
- `mvn -o -DskipTests test-compile` → exit 0.
- `testng-verify-selftest.xml` → **21 run / 0 failures**.
- Dispatching `connections` CI (iOS 18.5) to validate the DC Cable + Busway-field assertions.
