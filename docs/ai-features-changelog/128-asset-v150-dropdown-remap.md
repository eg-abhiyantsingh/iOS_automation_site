# 128 — Asset module v1.50 remap: picker-sheet Done no-op, select-field anatomy, dirty-state Save

**Date:** 2026-07-14
**Prompt:** "good fix all other similar test case in asset module" (after the
right-edge toggle fix) — DS_EAD_11 was the reproduction case.

## Root causes found (probe-verified on live v1.50 sim)

1. **Class/subtype picker sheet no longer auto-dismisses** after picking a row,
   and its Done button **swallows `element.click()`** (v1.50 silent no-op
   family). `tapDoneOnPicker` printed "✓ Tapped Done" while the sheet stayed
   open — every downstream step (field scroll, dropdown options, Save) hunted
   behind a full-screen blocker. This one helper serves ALL class/subtype
   pickers (9 call sites) across the Asset module.
2. **Custom-Attributes select fields are NOT buttons**: label StaticText +
   `Select…`/`Select...` StaticText ~30pt below + `chevron.down` image. BOTH
   ellipsis spellings (U+2026 and 3 dots) coexist in one DOM. Pressing the row
   opens a sheet (Sheet Grabber + Cancel + PopoverDismissRegion) whose option
   rows ARE full-width (~400pt) Buttons.
3. **SwiftUI duplicate accessibility nodes**: one option row exposes twin
   Buttons; the phantom has bogus geometry — last-match presses miss. Pick the
   WIDEST visible match, scoped BELOW the Sheet Grabber.
4. **Save is dirty-state**: no Save button exists until a field changes; then
   nav 'Close' becomes red 'Cancel' and a pinned blue 'Save Changes' appears at
   the bottom. A pre-change probe found 0 Save buttons in the whole DOM.
5. **Selecting an option makes the form jump back to the top** — cached label
   rects go stale; value readback must re-find the label fresh.
6. **Asset-list bleed-through** poisons geometric row matching: list texts sit
   at x≥73 vs form value column x≈44 — the x-band filter is load-bearing; and
   the list's search-bar Cancel + the dirty-state nav Cancel false-positive any
   naive "sheet open" check (only `Sheet Grabber` is a reliable signal).

## Central fixes (AssetPage / BasePage)

- `tapDoneOnPicker`: W3C press + verify-sheet-closed + retry ×3 + swipe-down
  fallback (fixes all 9 call sites at once).
- NEW `selectDetailsDropdown(fieldLabel, optionValue)`: scroll-to-label →
  press value row (x-band + comma filters vs bleed-through) → Sheet-Grabber
  open check → widest in-sheet option press → **verified value readback with
  fresh label re-find** (survives the form-jump). Sets `liveEditEvidence`.
- `selectInterruptingRating`: leads with the primitive; legacy strategies kept
  as fallback, now fully at implicit-wait 0 (was burning 3 strategies × session
  implicit wait → the exact-360s ThreadTimeouts).
- `selectDropdownValueStrategies`: all six `option.click()` → W3C center press.
- `clickSaveChanges`: dismissKeyboard first; detects the clean-state live-edit
  screen (Close + no Save in DOM) and skips the scroll grind honestly.
- `isAssetSavedAfterEdit` Strategy 0: consumes `liveEditEvidence` (verified
  readback) as positive non-vacuous save evidence; every other path unchanged,
  anti-vacuous VerificationError retained.
- `dismissOptionSheet`: only presses a Cancel with y>200 — the nav-bar Cancel
  at y≈60 DISCARDS pending edits.

## Test rewrites (stale select-vs-textfield contracts, valid spec options)

- DS: EAD_10 Ampere→"200A", EAD_12 Voltage→"240V", EAD_14 Manufacturer→"Eaton",
  EAD_17 Ampere→"100A", EAD_18 Ampere/IR/Voltage→"400A"/"20 kA"/"480V",
  EAD_20 Ampere→"60A".
- CB: EAD_10 Ampere→"100A", EAD_13 Manufacturer→"Eaton", EAD_15 Voltage→"480V",
  EAD_19 Manufacturer→"Siemens", EAD_20 Ampere+Mfr→"200A"/"ABB" (Model stays
  text), EAD_22 Manufacturer→"Square D". (CB_EAD_12 rides the fixed
  selectInterruptingRating.)
- FUSE: EAD_13 Voltage→"600V", EAD_19 Amperage/Mfr/KA/Voltage→"60A"/"EATON"/
  "50 kA"/"240V".
- Cancel-flow tests (CB_EAD_24, DS_EAD_22, FUSE_EAD_23) intentionally deferred —
  v1.50 cancel semantics (dirty-state nav Cancel) need their own contract.
- Capacitor (CAP_EAD) untouched: spec rows are the known-misaligned kind;
  graceful-failure diagnostics will reveal ground truth when run.

## Cross-phase sweep ("fix for all asset phase test case")

Two more primitive defects found by the DS_EAD_17/18 driver loop and fixed:
form position must be NORMALIZED TO TOP before every label hunt (sequential
multi-field tests otherwise strand each other at the bottom of the form), and
a missed option press leaves the sheet open, which then poisons every later
interaction (bleed-through makes covered labels report visible) — the
primitive now dismisses stale sheets before pressing, verifies the sheet
closed after the option press (retry ×1, dismiss + honest false otherwise),
and logs every press target/rect.

Remaining central helpers rerouted through the primitive (legacy fallback
kept): `fillAmpereRating`, `selectAmpereRating`, `fillVoltage`,
`fillInterruptingRating`, `selectMainsType`, and generic
`selectDropdownOption` (after its fast-fail precondition).

Per-phase test remap (agent sweep, spec-validated option values; classes with
misaligned spec rows — Capacitor, Motor, Relay, UPS, Other, Generator,
Junction Box, MCC Bucket — left untouched; cancel-flow and clearTextField
tests deferred):
- Phase1: ATS_EAD_11/16/19 Ampere Rating → 200A/400A/600A (was typed "100").
- Phase2 (mine): CB ×6, DS ×6, FUSE ×7 (incl. FUSE_EAD_10/11/12/18/21 found
  by the audit agent: 50A / BUSSMANN / 25 kA / 30A / 40A).
- Phase3: Loadcenter Mains Type (LC_EAD_15 + 2 helpers; killed invalid
  "Main Breaker"/"Convertible" options), MCC Voltage/Manufacturer/partial-save
  (MCC_EAD_14/18/20 + 2 helpers; killed invalid "GE"/"Schneider").
- Phase4: 17 field edits — Panelboard Voltage; PDU Ampere/Manufacturer/
  Voltage/indicator-pair; Switchboard Ampere/FaultWithstand/MainsType/
  Manufacturer/Voltage; Transformer Frequency/KVA/Manufacturer/Primary/
  Secondary/Winding. Six of these had been typing values that were NEVER
  valid options ("100kA", "Single Main", "60Hz", "1000", "13800V",
  "Delta-Wye") — silently no-oping since v1.50.
- Phase5: Utility Starting Voltage ×2 (480V/240V).
- Phase6: audit found zero convertible sites (subtype/tasks/issues suite).

## Validation (local iPhone 17 Pro Max, v1.50, driver loop)

- DS_EAD_11 GREEN ×2 (dirty-save click evidence, then full primitive readback;
  3m11s vs the old 6m timeout).
- DS_EAD_10 GREEN (Ampere 200A), DS_EAD_12 GREEN (Voltage 240V, 52s),
  DS_EAD_14 GREEN (Manufacturer Eaton), DS_EAD_17 GREEN (Ampere 100A).
- DS_EAD_18 + FUSE_EAD_10 validation with sheet discipline: results appended.
- Known debt: a compile-check pattern (`mvn | tail; echo $?`) masked one javac
  failure (missing isOptionSheetOpen) — one dead run; now using pipefail.
