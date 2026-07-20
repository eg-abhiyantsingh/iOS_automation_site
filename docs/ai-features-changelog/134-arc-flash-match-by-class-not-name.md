# 134 — Arc Flash by Asset: match by CLASS, never by name (user-caught bug)

**Prompt (2026-07-20):** user ran `TC_AF_133_transformerArcFlash` locally; it picked
`Transformer-1-Transformer-2-BUS, 233, Node Bus` — a **Node Bus** whose free-text
NAME contains "Transformer" — and failed. "You are searching for transformer but
that asset is not transformer, it is node bus. User can put any name. You need to
check asset type, not name."

## Root cause
Changelog-133 matching accepted any list cell whose *name* contained the class
token. Asset names are user-controlled free text, so name⊃token proves nothing.

## The fix — the cell declares its own class
The asset-list cell's accessibility name is a composite
`"<asset name>, <room>, <Class>"`. Matching now accepts a candidate ONLY when the
composite **ends with `", <Class>"`** (`firstVisibleAssetOfClass`). The `", "`
boundary keeps every class an unambiguous suffix (Load vs Loadcenter, MCC vs MCC
Bucket, Motor vs Motor Starter, Transformer vs Transformer (3-Winding), VFD vs
VFD Panel). The asset's name is never consulted; the old token/sibling-exclusion
logic is deleted. Signature is now `checkArcFlash(caseId, className, labels…)`.

## Hardening from the adversarial review workflow (18 agents, 3 lenses × verify)
Confirmed-real findings, all fixed:
- **No-op waits**: `shortWait()/mediumWait()/sleep()` are documented
  `until(d->true)` no-ops — every settle in the discovery flow raced the UI.
  Replaced with `utils/Waits` condition polls: 3s poll for a matching cell after
  search/clear, 400ms real settle after scroll, and a **6s details-ready poll**
  (nav title "Asset Details" or class text) before the Step-4 page-source scan
  (driver runs `animationCoolOffTimeout=0`; an unpolled snapshot can catch the
  push transition mid-flight → flaky false FAIL).
- **Step-3 retry hid the asset**: retry used to re-apply `searchAsset(className)`
  — for assets found only via the *unfiltered* rescan the filter re-hides them,
  making the retry deterministically fail. Retry now re-runs `findAssetOfClass`.
- **Silent filter clear**: `searchAsset("")` failures were swallowed → a
  still-filtered list read as "no fixture" SKIP. New `clearSearchFilterVerified()`
  confirms the field is actually empty.
- **Hardcoded geometry**: `y > 860` cut ~100pt even on iPhone 17 Pro Max (956pt)
  and worse on iPad. Now window-derived (`height − 90`, 860 fallback); name-length
  cap 140 → 250.
- **`unescape()` order bug**: `&amp;` was decoded FIRST → double-decode of names
  like `Panel &lt;3`; now exact reverse of `escape()` (+ numeric refs `&#10;/&#9;/&#13;`).
- **Label-value false match**: a `"Class, Transformer"` row also ends with
  `", Transformer"` — 2-segment composites are now rejected when the prefix is a
  field label (Class/Type/Asset Class/Subtype); the old dead-code guard removed.

Known residual (shared-code, not touched): `AssetPage.selectAssetByName` strategy 1
has no `visible==1` guard (hidden-twin bleed-through wrong-tap risk) — the
details-ready poll + label assert catch a wrong-screen landing after the fact.

## v1.50 field-name discovery
Node Bus details showed the universal AF field is **"System Voltage"** (Engineering
section) — bare "Voltage" may not exist in v1.50 at all. `UNIVERSAL_AF_LABELS =
{"System Voltage", "Voltage"}` is now merged ahead of every class's own list.

## New coverage
**TC_AF_139_nodeBusArcFlash** — Node Bus is a live system class (not in the gold
picker) with real AF data (System Voltage + required Mains Type). Suite = 73 tests,
per-asset coverage 39/39 classes.

## Validation (live, iPhone 17 Pro Max sim, one test at a time)
- TC_AF_133 transformer: **PASS** — now picks `Transformer-2, …, Transformer`
  (class confirmed on details), not the Node Bus.
- TC_AF_110 fuse: **PASS** — exercised the unfiltered-rescan fallback;
  System Voltage + Fuse Count read.
- TC_AF_139 node bus: **PASS** — System Voltage = '—', Mains Type = 'Select…' read.
- `mvn -o -DskipTests test-compile` PASS; verifier self-tests 34/34 PASS.
