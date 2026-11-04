# 207 — Re-check: forms for energized work, iOS 1.74 vs Android 1.39 (4 Nov 2026)

**Prompt:** "check again that form issue related to energized i told mukul told that is missing for android but present in ios"

## Builds and data
- **iOS:** 1.74 Debug-QA (2 Nov) on the iPhone 17 simulator B9AA2053.
- **Android:** 1.39 (53), installed on `quacky_test` at 15:39 IST today. The first check (changelog 206) used 1.38.1.
- **Tenant:** QA, EG-ACME, site (s) Wild Goose Brewery.
- **Work orders:** `Job - Nov 2, 6:28 PM` (Engineering Data + IR Checklist + 2 forms) and `QA-DEMO ZP-5135` (Infrared Thermography).
- **Asset:** `QA-DEMO ZP-4874 SEP asset 27 Oct` (Panelboard, room Test, 3 draft forms).
- **Time:** 15:44–16:16 IST.

## What "energized forms" are in QA
- `GET /api/eg-forms` returns 668 forms.
- 42 of them are NFPA 70B (type `70b`) "Energized …" forms:
  - Energized Visual Inspection
  - Energized Electrical Testing
  - Energized Mechanical Servicing
  - Energized Enhanced Electrical Testing / Enhanced Visual Inspection
- None of them has a `service_id`, so they are added as standalone forms.
- Services marked `de_energized=false` in `/api/procedures-v2/services`: IR, UT, Condition Assessment, Arc Flash, DGA, Panel Schedule, UPS and Cleaning Services. None is named "Energized Maintenance".

## Result: nothing for energized work is missing on Android 1.39
| Where | iOS 1.74 | Android 1.39 | Result |
|---|---|---|---|
| Tap an asset in the multi-service WO | Infrared / Forms / Full Asset Page | same | Same |
| Asset › Forms | "Select a Form" list with status + Add Form button | opens the first form; other forms are chips; Add Form = `+` at the far end of the chip row (off-screen with 3 forms) | **Different** |
| Add Form › Procedure | Cleaning, CTT, De-Energized VI, IR Testing, NETA, … | same | Same |
| Add Form › Standalone Form | Energized Electrical Testing / Mechanical Servicing / Visual Inspection | same 3 (+ search box) | Same |
| Long-press asset menu (ZP-5135) | 9 items incl. Forms (1), Manage Services | same | Same |
| Asset with no forms | no Forms item | no Forms item | Same |
| WO Forms tab (ZP-5135) | Test form validation 0/1, 1 draft | same | Same |
| Full Asset Page › Maintenance | Maintenance Records › Infrared | same | Same |
| Capture Settings › Work Type | shown, editable | shown, editable (missing in 1.38.1) | Same now |

**The one difference:** iOS lists the asset's forms first. Android jumps into the first form and hides Add Form behind the chip row. This may be what Mukul saw. It is not filed yet: it is a layout difference, not missing functionality. Waiting for the user to confirm the screen.

## Artifact
- https://claude.ai/artifact/JofrZkn1JDC2hQmgUhSd2Q (v3) has a new top section, "Re-check: forms for energized work, on Android 1.39", with:
  - verdicts;
  - the table;
  - 3 screenshot pairs.

## Test data
- Nothing was added or saved: every Add Form sheet was cancelled.
- Both devices were switched to `Job - Nov 2, 6:28 PM` for the check and back to `QA-DEMO ZP-5135` afterwards.

## Lessons
- **iOS stale page source:** after the "More sections" chip on Asset Details, Appium's page source kept returning the Settings tab while the screen showed another view. Use `simctl io … screenshot` plus coordinate taps when nodes and the screen disagree.
- **EG forms API:** the full list is `GET /api/eg-forms` (cookie auth, `X-Subdomain`), about 7.7 MB. `title`, `eg_form_type_key` and `node_class_names` are enough to find forms by name.

## Files
- `docs/bug-evidence/2026-11-04-ir-photo-type/energized-forms/` holds 8 annotated jpgs.
- `artifact-index.html` was refreshed (v3).
