# Work Order Type create-form dropdown (v1.55) — module design

**Date:** 2026-08-04. Companion to `docs/worktype-gold-spec-2026-07-21.md` and
`docs/worktype-test-design-2026-07-21.md`. App v1.55 shipped the feature the
TC_WT_X_CAN_* canaries were watching for: the iOS "New Work Order" form now has
a **required `Work Type` row** (default **General**, chevron → option surface).
The old canary contract ("iOS create form has NO Work Type row") is obsolete.

## App truth (probe-verified 2026-08-04, PROBE_E/F/G)

- Create form rows are Buttons named `'<Label>, <value>'`; the Work Type row is
  **`'Work Type, *, <value>'`** — the required marker `*` is a SEPARATE middle
  segment of the accessible name. Default value: `General`.
- **Comma trap:** `rowPriority()`-style "last segment" parsing is WRONG here —
  `'Work Type, *, Clean, Tighten, Torque'` → "Torque". Parse the value as the
  substring after the `'Work Type, *, '` prefix (dedicated primitive).
- Option surface = **stacked bottom sheet with its own `Work Type`
  NavigationBar (y≈72) + `Done` button (y≈84)**; the `New Work Order` nav stays
  in the hierarchy behind it. Option rows are full-width **Buttons** (twinned
  StaticTexts with identical names; Cells are name-null wrappers).
- **14 options** = `General` pinned FIRST, then the 13 service display names in
  case-sensitive lexicographic order (probe-observed: `DGA / Fluid Sample
  Analysis` sorts before `De-Energized Visual Inspection`). Labels equal
  `WorkTypeCatalog.displayName()` EXACTLY (incl. `Clean, Tighten, Torque`,
  `DGA / Fluid Sample Analysis`, `Shutdown (Composite)`).
- Selection semantics — **BUILD-VARIANT, primitives are dual-semantics
  (2026-08-04 second correction):** the sheet DOES carry its own nav `Done`
  (y≈84; PROBE_E's dump showed it — earlier misattributed to the background
  WO list) and a `Search work types` field (user screenshot). On the local
  v1.55 build PROBE_F saw a center tap COMMIT AND CLOSE immediately; on the
  user's build the tap only marks the radio (sheet stays open, `Done`
  commits). `selectWorkTypeInPicker` handles both: tap → if sheet still open,
  press sheet-Done → verify closed + row value. Radio state is exposed either
  way: selected row `value='1'`/`selected=true` + `checkmark.circle.fill`.
  **Swipe-down does NOT dismiss**; no-op close = sheet-Done when present,
  else re-tap the selected row (`closeWorkTypePickerNoChange()`). All 14
  options fit on screen (no scroll). Search-field filtering is an OPEN
  coverage gap (candidate follow-up tests).
- **Post-create session flow (user-demonstrated, PROBE_G verifies):** dashboard
  gains a top-right **`WO` chip** while a session is active; tapping it opens a
  menu: `End Session` row + the WO list with radio selectors (session
  switcher). `End Session` → alert `End Work Order Session?` with
  `Cancel`/`End Session`. This is the UI cleanup path after create;
  final row deletion via `TestDataApi.deleteWorkOrder` (async backend).

## Classes & TC allocation (3 new classes, ≥200 cases total)

All extend `WorkTypeBaseTest`; ExtentReport `MODULE_JOBS`, feature
`"Work Type Create Dropdown (v1.55)"`. All type-bound predicates; multi-strategy
locators; hard asserts; honest SKIP only for environment preconditions;
every created WO is cleaned up via `TestDataApi.deleteWorkOrder` in-test.

| Class | TC prefix | ~Count | Focus |
|---|---|---|---|
| `WorkTypeCreateForm_Test` | `TC_WTC_FORM_*` | ~60 | Row anatomy, required marker, default General, value readback after each selection (×14), open/cancel state, Create-button gating, form stability |
| `WorkTypeCreatePicker_Test` | `TC_WTC_PICK_*` | ~80 | Option surface: exact 14 options, per-option presence (×14), per-option selection round-trip (×14), order, no dupes, cancel/dismiss semantics, reopen shows selection, idempotent double-open |
| `WorkTypeCreateE2E_Test` | `TC_WTC_E2E_*` | ~60 | Create WO per type (×14): server `work_type_id` parity via API, list row appears, details screen label, General→null id, delete cleanup; offline-create smoke; duplicate names |

## Existing-test updates

- `TC_WT_X_CAN_01/02` (WorkType_CrossCutting_Test): REVERSE the canary — the
  row MUST now exist with default `General`; keep them as regression canaries
  for the dropdown's presence.
- `docs/worktype-gold-spec-2026-07-21.md`: add §"iOS create-form dropdown
  (v1.55+)".

## New page primitives (WorkOrderPage — written FIRST, single owner)

`isCreateFormWorkTypeRowPresent()`, `getCreateFormWorkTypeValue()`,
`workTypeRowHasRequiredMarker()`, `openWorkTypePicker()`,
`isWorkTypePickerOpen()`, `getWorkTypePickerOptions()` (bounded scroll),
`selectWorkTypeInPicker(name)` (verified: row value flips),
`closeWorkTypePickerWithoutSelecting()`,
`createWorkOrderViaForm(name, WorkTypeCatalog)` (returns created-and-verified).

## House rules (inherited)

- One test = one contract; no `assertTrue(x || y)` unless the OR is the domain.
- `dismissKeyboard()` after typing before any button press.
- No new `Thread.sleep` — `Waits`/`waitForCondition` only.
- Fixture-site independence: everything runs on the LANDED site.
- Wedge safety: no untyped `name CONTAINS` scans; bound big queries.
