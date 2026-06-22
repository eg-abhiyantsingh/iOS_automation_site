# 090 — Asset test-case + dropdown sync to node_classes v14 (2026-06-22)

Per request: "check this updated sheet (`node_classes_template (14).xlsx`) to update asset
test case and drop down." Deep approach — verified against the **live web app**, not just
the sheet, because the sheet's options column is misaligned.

## How the ground truth was established (Playwright → API)
The v14 sheet has 4 tabs (Classes 43, Core Attributes 190, Subtypes 66, Options). But its
**Core Attributes `options` column is shifted** for Capacitor/Generator/Motor/Other/Relay/UPS
(textfield rows carry stray option-lists belonging to other fields) — the same defect noted
in memory. So I drove the web app (`acme.qa.egalvanic.ai`, V1.21) with Playwright, found the
authoritative feed **`GET /api/node_classes/user/{id}`**, and captured all 43 classes with a
correctly-aligned `definition[]` (each field's `name/type/options/default`) + `node_subtypes`.
Normalized to `ground_truth_classes.json`; documented in
[asset-classes-ground-truth-2026-06-22.md](../asset-classes-ground-truth-2026-06-22.md).

Key truth (resolves the sheet's shift): Generator/Capacitor/Motor/Other/Relay/UPS are
**all textfields** (no dropdowns); Motor's only `select` is "Mains Type"; "R P M" is a
textfield. Dropdown **value** fills in the tests (e.g. `Ampere Rating="225A"`) were checked
against live options — **all 0 invalid**. So only **subtype** strings needed correcting.

## Corrections applied (spec-correctness)
`selectAssetSubtype()` normalizes `≤`↔`<=` and spacing, so these mostly passed already by
fuzzy-match — the fixes make the tests assert the *actual* v14/live values, not stale ones.

**`Asset_Phase5_Test.java`:**
| Was | Now (live) | Why |
|---|---|---|
| Circuit Breaker MCCB `(≤ 250A)` / `(> 250A)` ×16 | `(≤ 225A)` / `(> 225A)` | live + sheet both say **225A** |
| `Medium-Voltage Air Circuit Breaker` ×4 | `Medium-Voltage Air **Magnetic** Circuit Breaker` | live subtype name |
| `Fused Disconnect Switch (> 1000V)` ×3 | `Fused Disconnect Switch (>1000V)` | live has no space |

**`AssetPage.java`:** added `LIVE_SUBTYPES` (65 live subtypes) and rewrote the subtype-dropdown
open helper's Strategy 3 — was a 29-entry `name == '…'` loop at 500ms/miss that was **missing
58 live subtypes** (so it fell through to the slow whole-tree Strategy 4). Now a **single
scoped `name IN {…}` predicate at implicit-wait 0**, mirroring the proven class-picker IN-fix.
Faster *and* correct.

## Structural drift flagged (NOT auto-rewritten — needs care)
The live class definitions have changed shape, which explains some failures and ties into the
picker-hang work:
- **Loadcenter now has only 2 fields** (`Size`, `Mains Type`) — yet `LC_EAD_16/20` edit
  Manufacturer/Voltage (don't exist) → unbudgeted scans for absent fields = the LC_EAD hangs.
- **Busway has no live subtypes** — `TC-BUS-ST-*` select `Busway (≤ 600V)`; may now be absent.
- Live data carries junk subtypes (`Scrubtype` on DS, `I don't node` on UPS) — detection must
  tolerate them (now included in `LIVE_SUBTYPES`).

These are addressed alongside the picker-helper hardening (see the follow-on changelog) rather
than by rewriting test intent blind.

## Validation
- `mvn -o -DskipTests test-compile` → **exit 0**.
- Dropdown value fills audited vs live select options: 0 invalid.
- CB/DS subtype strings re-grepped: 0 stale `250A`/bare-`Air Circuit Breaker` remain.
