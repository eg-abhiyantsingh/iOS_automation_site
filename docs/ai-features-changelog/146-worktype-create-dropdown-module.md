# 146 — v1.55 Work Type create-dropdown: probes, primitives, 164-test module, policy-sheet hook

**Date:** 2026-08-04
**Prompts:** "I have update the app check and also for ci cd see" → "check now we
have work type option too" → "create a module and update our test case like for
work order type new functionality more 200 test case will be added" → "you can
end work order like this after creating work order" → "you need to create 13
work order by selecting all this one by one".

## App update v1.51 → v1.55

- Local `/Users/abhiyantsingh/Downloads/Z Platform-QA.app` = 1.55; CI zip
  `apps/Z-Platform-QA.zip` rebuilt from it (was 1.51) and pushed (d96ce99).
- **v1.55 ships the 13-option Work Type dropdown on the iOS create form** —
  exactly what the TC_WT_X_CAN_* canaries were watching for.

## Probe campaign (PROBE_E..K, WorkTypeProbe_Test)

- Row Button `'Work Type, *, <value>'` (required-marker segment; default
  General). Comma trap: last-segment parsing breaks on
  `Clean, Tighten, Torque` → dedicated prefix parser.
- Picker: stacked sheet, own `Work Type` nav + `Done` (y≈84) + search field;
  14 options (General first, 13 display names case-sensitive lexicographic,
  byte-equal to `WorkTypeCatalog.displayName()`).
- Selection is BUILD-VARIANT: local build = tap commits+closes; user build =
  tap marks radio, Done commits. Primitive handles both. Radio oracle:
  `value=='1'`/`selected==true`/`checkmark.circle.fill`.
- Post-create the session auto-starts: dashboard `WO` chip (Other/StaticText,
  NOT Button). Chip menu rows are name-LESS Cells (labels absent from the
  accessibility tree) — End-Session is disambiguated via the ALERT (named
  buttons: `End Work Order Session?` → Cancel/`End Session`; `Start Work
  Order?` = switcher row).

## New page primitives (WorkOrderPage, probe-pinned)

`isCreateFormWorkTypeRowPresent`, `workTypeRowHasRequiredMarker`,
`getCreateFormWorkTypeValue` (comma-safe), `openWorkTypePicker`,
`isWorkTypePickerOpen`, `getWorkTypePickerOptions`,
`getSelectedWorkTypeInPicker`, `isWorkTypeOptionSelected`,
`selectWorkTypeInPicker` (dual-semantics), `closeWorkTypePickerNoChange`,
`isDashboardWoChipPresent`, `openDashboardWoMenu` (structural verify),
`isDashboardWoMenuOpen`, `dismissDashboardWoMenu`,
`endActiveSessionViaDashboardMenu` (alert-disambiguated, manual-alerts).

## New module — 164 tests (TC_WTC_*)

| Class | Tests | Focus |
|---|---|---|
| WorkTypeCreateForm_Test | 46 | Row anatomy/required marker/default, per-type readback ×14, comma deep-contracts, state isolation, stability |
| WorkTypeCreatePicker_Test | 76 | Census (exactly 14, order law), per-option presence/position/selection ×14, radio state, commit semantics, stability |
| WorkTypeCreateE2E_Test | 40 | **Create one WO per type ×14 (user ask: "create 13 work orders one by one")** → session chip → API `work_type_id` parity → End Session → soft-delete cleanup; cancel/default-name/offline paths; defensive final cleanup |
| Canaries (CrossCutting) | 2 reshaped | TC_WT_X_CAN_01/02 reversed: row MUST exist, default General, marker present |

Authoring: workflow-orchestrated (3 author agents; 1 delivered before the
usage-credit outage killed 7/8 agents — Form/E2E files landed via their Write
calls; canary reshape + gold-spec + reviews completed inline).

## v1.55 "Policy Update" consent sheet (NEW blocker, fixed)

Backend now raises a blocking consent sheet over the Dashboard after login
('Please review the updated policies…' + `Accept & Continue`) — it swallowed
every tap and failed `openWorkOrdersScreenWT` for ALL tests (first seen
13:43). Fix: `BaseTest.acceptPolicyUpdateIfPresent()` — once-per-Appium-
session guard, 2.5s poll at the safe-Dashboard moment, wired into
`loginAndSelectSite`/`loginAndSelectSiteTurbo` BEFORE the Session-Recording
pass. Kill switch `ACCEPT_POLICY_UPDATE=false`.

## Environment notes

- First site flip-flopped AGAIN mid-day (Test QA 16 → Wild Goose Brewery) —
  PROBE_G's stray WO landed on Wild Goose; soft-deleted via API. The
  site-scoped fixture ensure (changelog 145) is what keeps this survivable.
- Phantom active session (chip without a live WO) confirmed possible when the
  active WO is deleted server-side — End-Session primitive handles it
  (Start-alert path → cancel + report false).

## Validation

(filled in as the live runs complete)
