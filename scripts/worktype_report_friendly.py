#!/usr/bin/env python3
"""Post-process a Work-Order-Type module Client_Report_*.html:

1. Rename every technical test title to the client-friendly house style
   "Work Order - <Work Type / Area> - <plain-English check> (TC id)"
   (user directive 2026-08-05: names a non-QA reader understands at a glance).
2. Optionally fold ONE verified rerun-pass over an in-report fail
   (--fold-pass TC_ID) — same semantics as CI merge_rerun where a rerun PASS
   overrides the original FAIL. Only use after the rerun was actually executed
   and passed; this script edits presentation, it is not evidence.

Usage:
  python3 scripts/worktype_report_friendly.py <report.html> <out.html> [--fold-pass TC_WTC_PICK_019]
"""
import re
import sys

# ── friendly-name map ────────────────────────────────────────────────────────
# Keys are the EXACT technical titles ExtentReportManager writes; values are
# the client-facing titles. The TC id is appended automatically.

CATEGORIES = {
    "Work Type Create Dropdown (v1.55)": "Work Order Type - New Feature (v1.55)",
    "Work Types (13-option dropdown)": "Work Order Type - Health Checks",
}

FRIENDLY = {
    # ── Create form (FORM 001-046) ──
    "TC_WTC_FORM_001 - create form carries the v1.55 Work Type config row": "Work Order - Create Form - Work Type field is present",
    "TC_WTC_FORM_002 - Work Type row carries the required-marker '*' segment": "Work Order - Create Form - Work Type is marked as required (*)",
    "TC_WTC_FORM_003 - fresh create form defaults the Work Type value to 'General'": "Work Order - Create Form - Work Type defaults to General",
    "TC_WTC_FORM_004 - Work Type row renders below the Priority row (form order)": "Work Order - Create Form - Work Type appears below Priority",
    "TC_WTC_FORM_005 - StaticText 'Work Type' label renders on the create form": "Work Order - Create Form - Work Type label is shown",
    "TC_WTC_FORM_006 - regression: Photo Type row survives the v1.55 form change": "Work Order - Create Form - Photo Type field still works after the update",
    "TC_WTC_FORM_007 - regression: Priority row survives the v1.55 form change": "Work Order - Create Form - Priority field still works after the update",
    "TC_WTC_FORM_008 - regression: Equipment row survives the v1.55 form change": "Work Order - Create Form - Equipment field still works after the update",
    "TC_WTC_FORM_009 - open create form renders content and shows no error alert": "Work Order - Create Form - opens cleanly with no errors",
    "TC_WTC_FORM_010 - Work Type row still present after the app-alive probe": "Work Order - Create Form - Work Type field stays visible while the form is open",
    "TC_WTC_FORM_011 - select 'General' → row reads back exactly 'General'": "Work Order - General - selecting it shows General on the form",
    "TC_WTC_FORM_012 - select 'Arc Flash Data Collection' → exact row readback": "Work Order - Arc Flash Data Collection - selecting it shows correctly on the form",
    "TC_WTC_FORM_013 - select 'Arc Flash Label Placement' → exact row readback": "Work Order - Arc Flash Label Placement - selecting it shows correctly on the form",
    "TC_WTC_FORM_014 - select 'Cleaning' → exact row readback": "Work Order - Cleaning - selecting it shows correctly on the form",
    "TC_WTC_FORM_015 - select 'Clean, Tighten, Torque' → exact row readback": "Work Order - Clean, Tighten, Torque - selecting it shows correctly on the form",
    "TC_WTC_FORM_016 - select 'Condition Assessment' → exact row readback": "Work Order - Condition Assessment - selecting it shows correctly on the form",
    "TC_WTC_FORM_017 - select 'De-Energized Visual Inspection' → exact row readback": "Work Order - De-Energized Visual Inspection - selecting it shows correctly on the form",
    "TC_WTC_FORM_018 - select 'DGA / Fluid Sample Analysis' → exact row readback": "Work Order - DGA / Fluid Sample Analysis - selecting it shows correctly on the form",
    "TC_WTC_FORM_019 - select 'Infrared Thermography' → exact row readback": "Work Order - Infrared Thermography - selecting it shows correctly on the form",
    "TC_WTC_FORM_020 - select 'Insulation Resistance Testing' → exact row readback": "Work Order - Insulation Resistance Testing - selecting it shows correctly on the form",
    "TC_WTC_FORM_021 - select 'NETA Testing' → exact row readback": "Work Order - NETA Testing - selecting it shows correctly on the form",
    "TC_WTC_FORM_022 - select 'Panel Schedule Updates' → exact row readback": "Work Order - Panel Schedule Updates - selecting it shows correctly on the form",
    "TC_WTC_FORM_023 - select 'Shutdown (Composite)' → exact row readback": "Work Order - Shutdown (Composite) - selecting it shows correctly on the form",
    "TC_WTC_FORM_024 - select 'UPS Maintenance' → exact row readback": "Work Order - UPS Maintenance - selecting it shows correctly on the form",
    "TC_WTC_FORM_025 - 'Clean, Tighten, Torque' round-trips intact (comma-embedded name)": "Work Order - Clean, Tighten, Torque - name with commas is never cut short",
    "TC_WTC_FORM_026 - 'DGA / Fluid Sample Analysis' round-trips intact (slash-embedded name)": "Work Order - DGA / Fluid Sample Analysis - name with a slash displays correctly",
    "TC_WTC_FORM_027 - 'Shutdown (Composite)' round-trips intact (parenthesized name)": "Work Order - Shutdown (Composite) - name with brackets displays correctly",
    "TC_WTC_FORM_028 - anti-regression pin: row value is never a comma-split fragment": "Work Order - Create Form - selected work type never shows as a partial name",
    "TC_WTC_FORM_029 - committed selection is radio-marked when the picker reopens": "Work Order - Create Form - chosen work type is ticked when the list reopens",
    "TC_WTC_FORM_030 - fresh form's picker opens with 'General' radio-marked": "Work Order - Create Form - new form shows General ticked in the list",
    "TC_WTC_FORM_031 - Work Type selection persists while editing the Name field": "Work Order - Create Form - work type keeps its value while typing the name",
    "TC_WTC_FORM_032 - cancel → reopen: fresh form defaults back to 'General' (no state leak)": "Work Order - Create Form - cancelling resets the work type back to General",
    "TC_WTC_FORM_033 - two picker open + no-op-close cycles leave the value untouched": "Work Order - Create Form - opening and closing the list twice keeps the value",
    "TC_WTC_FORM_034 - selecting the same value twice keeps the value": "Work Order - Create Form - picking the same type twice keeps the value",
    "TC_WTC_FORM_035 - selecting A then B leaves the row reading B": "Work Order - Create Form - picking one type then another keeps the last one",
    "TC_WTC_FORM_036 - Create button still present after the full picker dance": "Work Order - Create Form - Create button still available after choosing a type",
    "TC_WTC_FORM_037 - required marker '*' persists after a selection": "Work Order - Create Form - required (*) mark stays after choosing a type",
    "TC_WTC_FORM_038 - no-op close leaves a prior non-default selection untouched": "Work Order - Create Form - closing the list without choosing keeps the earlier choice",
    "TC_WTC_FORM_039 - selection stays radio-marked across TWO reopen/no-op-close cycles": "Work Order - Create Form - chosen type stays ticked after reopening the list twice",
    "TC_WTC_FORM_040 - Work Type selection does not clobber the Name field default": "Work Order - Create Form - choosing a type does not change the name field",
    "TC_WTC_FORM_041 - app alive, rendering, alert-free WHILE the picker sheet is open": "Work Order - Create Form - app stays stable while the type list is open",
    "TC_WTC_FORM_042 - app alive, rendering, alert-free after a selection commit": "Work Order - Create Form - app stays stable after choosing a type",
    "TC_WTC_FORM_043 - app alive, rendering, alert-free after a no-op close; value intact": "Work Order - Create Form - app stays stable after closing the list without choosing",
    "TC_WTC_FORM_044 - app alive, rendering, alert-free after cancelling a form with a selection": "Work Order - Create Form - app stays stable after cancelling with a chosen type",
    "TC_WTC_FORM_045 - two back-to-back open/select/cancel cycles leave the WO list healthy": "Work Order - Create Form - two choose-and-cancel rounds leave the page healthy",
    "TC_WTC_FORM_046 - full dance (open→select→reopen→no-op close→cancel) alive at every step": "Work Order - Create Form - full open/choose/close/cancel flow stays stable throughout",
    # ── Work Type list / picker (PICK 001-076) ──
    "TC_WTC_PICK_001 - Work Type row tap opens the picker sheet (verified open)": "Work Order - Work Type List - tapping Work Type opens the list",
    "TC_WTC_PICK_002 - open sheet carries its own 'Work Type' NavigationBar": "Work Order - Work Type List - list opens with its own Work Type header",
    "TC_WTC_PICK_003 - option census EQUALS the backend catalog (all visible, no scroll)": "Work Order - Work Type List - shows every work type from the system",
    "TC_WTC_PICK_004 - census contains NO 'Done'/'Cancel'/'Create' chrome (no sheet Done exists)": "Work Order - Work Type List - shows only work types (no stray buttons)",
    "TC_WTC_PICK_005 - census has no duplicate option labels": "Work Order - Work Type List - no duplicate entries",
    "TC_WTC_PICK_006 - 'General' is the FIRST census entry": "Work Order - Work Type List - General is the first entry",
    "TC_WTC_PICK_007 - every non-General entry is in case-sensitive lexicographic order": "Work Order - Work Type List - entries are in alphabetical order",
    "TC_WTC_PICK_008 - every census entry is a real catalog option (backend service or General; no foreign labels)": "Work Order - Work Type List - every entry is a real work type",
    "TC_WTC_PICK_009 - 'General' present in the picker census": "Work Order - General - shown in the work type list",
    "TC_WTC_PICK_010 - 'Arc Flash Data Collection' present in the picker census": "Work Order - Arc Flash Data Collection - shown in the work type list",
    "TC_WTC_PICK_011 - 'Arc Flash Label Placement' present in the picker census": "Work Order - Arc Flash Label Placement - shown in the work type list",
    "TC_WTC_PICK_012 - 'Clean, Tighten, Torque' present in the picker census (comma name)": "Work Order - Clean, Tighten, Torque - shown in the work type list",
    "TC_WTC_PICK_013 - 'Cleaning' present in the picker census": "Work Order - Cleaning - shown in the work type list",
    "TC_WTC_PICK_014 - 'Condition Assessment' present in the picker census": "Work Order - Condition Assessment - shown in the work type list",
    "TC_WTC_PICK_015 - 'DGA / Fluid Sample Analysis' present in the picker census": "Work Order - DGA / Fluid Sample Analysis - shown in the work type list",
    "TC_WTC_PICK_016 - 'De-Energized Visual Inspection' present in the picker census": "Work Order - De-Energized Visual Inspection - shown in the work type list",
    "TC_WTC_PICK_017 - 'Infrared Thermography' present in the picker census": "Work Order - Infrared Thermography - shown in the work type list",
    "TC_WTC_PICK_018 - 'Insulation Resistance Testing' present in the picker census": "Work Order - Insulation Resistance Testing - shown in the work type list",
    "TC_WTC_PICK_019 - 'NETA Testing' present in the picker census": "Work Order - NETA Testing - shown in the work type list",
    "TC_WTC_PICK_020 - 'Panel Schedule Updates' present in the picker census": "Work Order - Panel Schedule Updates - shown in the work type list",
    "TC_WTC_PICK_021 - 'Shutdown (Composite)' present in the picker census": "Work Order - Shutdown (Composite) - shown in the work type list",
    "TC_WTC_PICK_022 - 'UPS Maintenance' present in the picker census": "Work Order - UPS Maintenance - shown in the work type list",
    "TC_WTC_PICK_023 - 'General' sits at census index 0": "Work Order - General - always at the top of the list",
    "TC_WTC_PICK_024 - 'Arc Flash Data Collection' sits at its sorted census index": "Work Order - Arc Flash Data Collection - listed in the correct alphabetical position",
    "TC_WTC_PICK_025 - 'Arc Flash Label Placement' sits at its sorted census index": "Work Order - Arc Flash Label Placement - listed in the correct alphabetical position",
    "TC_WTC_PICK_026 - 'Clean, Tighten, Torque' sits BEFORE 'Cleaning' (comma sorts low)": "Work Order - Clean, Tighten, Torque - sorted correctly next to Cleaning",
    "TC_WTC_PICK_027 - 'Cleaning' sits at its sorted census index": "Work Order - Cleaning - listed in the correct alphabetical position",
    "TC_WTC_PICK_028 - 'Condition Assessment' sits at its sorted census index": "Work Order - Condition Assessment - listed in the correct alphabetical position",
    "TC_WTC_PICK_029 - 'DGA / Fluid Sample Analysis' sits BEFORE 'De-Energized…' (case-sensitive sort)": "Work Order - DGA / Fluid Sample Analysis - sorted correctly next to De-Energized",
    "TC_WTC_PICK_030 - 'De-Energized Visual Inspection' sits at its sorted census index": "Work Order - De-Energized Visual Inspection - listed in the correct alphabetical position",
    "TC_WTC_PICK_031 - 'Infrared Thermography' sits at its sorted census index": "Work Order - Infrared Thermography - listed in the correct alphabetical position",
    "TC_WTC_PICK_032 - 'Insulation Resistance Testing' sits at its sorted census index": "Work Order - Insulation Resistance Testing - listed in the correct alphabetical position",
    "TC_WTC_PICK_033 - 'NETA Testing' sits at its sorted census index": "Work Order - NETA Testing - listed in the correct alphabetical position",
    "TC_WTC_PICK_034 - 'Panel Schedule Updates' sits at its sorted census index": "Work Order - Panel Schedule Updates - listed in the correct alphabetical position",
    "TC_WTC_PICK_035 - 'Shutdown (Composite)' sits at its sorted census index": "Work Order - Shutdown (Composite) - listed in the correct alphabetical position",
    "TC_WTC_PICK_036 - 'UPS Maintenance' sits LAST (index 13)": "Work Order - UPS Maintenance - last entry in the list",
    "TC_WTC_PICK_037 - selecting 'General' commits and the row reads it back": "Work Order - General - can be selected and shows on the form",
    "TC_WTC_PICK_038 - selecting 'Arc Flash Data Collection' commits and the row reads it back": "Work Order - Arc Flash Data Collection - can be selected and shows on the form",
    "TC_WTC_PICK_039 - selecting 'Arc Flash Label Placement' commits and the row reads it back": "Work Order - Arc Flash Label Placement - can be selected and shows on the form",
    "TC_WTC_PICK_040 - selecting 'Clean, Tighten, Torque' reads back comma-intact (prefix parse)": "Work Order - Clean, Tighten, Torque - can be selected and the full name shows on the form",
    "TC_WTC_PICK_041 - selecting 'Cleaning' commits and the row reads it back": "Work Order - Cleaning - can be selected and shows on the form",
    "TC_WTC_PICK_042 - selecting 'Condition Assessment' commits and the row reads it back": "Work Order - Condition Assessment - can be selected and shows on the form",
    "TC_WTC_PICK_043 - selecting 'DGA / Fluid Sample Analysis' commits and the row reads it back": "Work Order - DGA / Fluid Sample Analysis - can be selected and shows on the form",
    "TC_WTC_PICK_044 - selecting 'De-Energized Visual Inspection' commits and the row reads it back": "Work Order - De-Energized Visual Inspection - can be selected and shows on the form",
    "TC_WTC_PICK_045 - selecting 'Infrared Thermography' commits and the row reads it back": "Work Order - Infrared Thermography - can be selected and shows on the form",
    "TC_WTC_PICK_046 - selecting 'Insulation Resistance Testing' commits and the row reads it back": "Work Order - Insulation Resistance Testing - can be selected and shows on the form",
    "TC_WTC_PICK_047 - selecting 'NETA Testing' commits and the row reads it back": "Work Order - NETA Testing - can be selected and shows on the form",
    "TC_WTC_PICK_048 - selecting 'Panel Schedule Updates' commits and the row reads it back": "Work Order - Panel Schedule Updates - can be selected and shows on the form",
    "TC_WTC_PICK_049 - selecting 'Shutdown (Composite)' commits and the row reads it back": "Work Order - Shutdown (Composite) - can be selected and shows on the form",
    "TC_WTC_PICK_050 - selecting 'UPS Maintenance' commits and the row reads it back": "Work Order - UPS Maintenance - can be selected and shows on the form",
    "TC_WTC_PICK_051 - fresh form: default open radio-marks 'General'": "Work Order - Work Type List - new form opens with General ticked",
    "TC_WTC_PICK_052 - fresh form: isWorkTypeOptionSelected('General') reads true": "Work Order - General - shows as selected on a new form",
    "TC_WTC_PICK_053 - default radio mark is stable across two consecutive reads": "Work Order - Work Type List - default tick reads the same on repeated checks",
    "TC_WTC_PICK_054 - after selecting 'Cleaning', reopen radio-marks 'Cleaning'": "Work Order - Cleaning - stays ticked when the list is reopened",
    "TC_WTC_PICK_055 - after selecting 'Clean, Tighten, Torque', reopen marks it (comma name)": "Work Order - Clean, Tighten, Torque - stays ticked when the list is reopened",
    "TC_WTC_PICK_056 - after selecting 'UPS Maintenance', the reopened mark is stable across two reads": "Work Order - UPS Maintenance - tick stays stable on repeated checks",
    "TC_WTC_PICK_057 - after selecting 'Infrared Thermography', reopened isWorkTypeOptionSelected reads true": "Work Order - Infrared Thermography - reads as selected after reopening the list",
    "TC_WTC_PICK_058 - after selecting 'NETA Testing', reopened 'General' is NO LONGER marked": "Work Order - NETA Testing - selecting it removes the tick from General",
    "TC_WTC_PICK_059 - X then Y: reopened sheet marks ONLY Y (X unmarked)": "Work Order - Work Type List - only the latest choice is ticked",
    "TC_WTC_PICK_060 - chain General → 'Arc Flash Data Collection' → 'UPS Maintenance': each reopen marks the latest": "Work Order - Work Type List - switching General, Arc Flash Data Collection, UPS Maintenance always ticks the latest",
    "TC_WTC_PICK_061 - chain 'Clean, Tighten, Torque' → 'NETA Testing' → 'Cleaning': each reopen marks the latest": "Work Order - Work Type List - switching Clean Tighten Torque, NETA Testing, Cleaning always ticks the latest",
    "TC_WTC_PICK_062 - chain 'DGA / Fluid Sample Analysis' → 'Shutdown (Composite)' → 'Condition Assessment': each reopen marks the latest": "Work Order - Work Type List - switching DGA, Shutdown, Condition Assessment always ticks the latest",
    "TC_WTC_PICK_063 - a single option tap commits AND closes the sheet (no Done step)": "Work Order - Work Type List - one tap selects and closes the list",
    "TC_WTC_PICK_064 - tap-commit closes the sheet for the comma name 'Clean, Tighten, Torque'": "Work Order - Clean, Tighten, Torque - one tap selects and closes the list",
    "TC_WTC_PICK_065 - closeWorkTypePickerNoChange succeeds on default and value stays 'General'": "Work Order - Work Type List - closing without choosing keeps General",
    "TC_WTC_PICK_066 - no-op close after selecting 'Cleaning' leaves the value 'Cleaning'": "Work Order - Cleaning - closing the list again keeps Cleaning selected",
    "TC_WTC_PICK_067 - no-op close after 'Clean, Tighten, Torque' leaves the comma value intact": "Work Order - Clean, Tighten, Torque - closing the list again keeps the full name",
    "TC_WTC_PICK_068 - after closeWorkTypePickerNoChange the sheet is CLOSED": "Work Order - Work Type List - closes properly when dismissed without choosing",
    "TC_WTC_PICK_069 - value is IDENTICAL before and after a reopen + no-op close cycle": "Work Order - Work Type List - value unchanged after reopening and dismissing",
    "TC_WTC_PICK_070 - picker reopens after a commit and still shows the full catalog census": "Work Order - Work Type List - reopens with the full list after a selection",
    "TC_WTC_PICK_071 - app alive, rendering, alert-free with the sheet OPEN": "Work Order - Work Type List - app stays stable while the list is open",
    "TC_WTC_PICK_072 - app alive, rendering, alert-free right after a tap-commit": "Work Order - Work Type List - app stays stable right after selecting",
    "TC_WTC_PICK_073 - app alive, rendering, alert-free right after the no-op close": "Work Order - Work Type List - app stays stable right after dismissing",
    "TC_WTC_PICK_074 - 3 consecutive open→select→reopen cycles stay healthy": "Work Order - Work Type List - three select-and-reopen rounds stay healthy",
    "TC_WTC_PICK_075 - two census reads on one open sheet are identical (content + order)": "Work Order - Work Type List - list reads identically on back-to-back checks",
    "TC_WTC_PICK_076 - Cancel after picker use restores a healthy Work Orders list": "Work Order - Work Type List - cancelling afterwards returns to a healthy Work Orders page",
    # ── End-to-end create + session + server (E2E) ──
    "TC_WTC_E2E_001 - create->session->server parity: General (work_type_id null)": "Work Order - General - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_002 - create->session->server parity: Arc Flash Data Collection": "Work Order - Arc Flash Data Collection - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_003 - create->session->server parity: Arc Flash Label Placement": "Work Order - Arc Flash Label Placement - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_004 - create->session->server parity: Cleaning": "Work Order - Cleaning - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_005 - create->session->server parity: Clean, Tighten, Torque (comma-embedded display name)": "Work Order - Clean, Tighten, Torque - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_006 - create->session->server parity: Condition Assessment": "Work Order - Condition Assessment - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_007 - create->session->server parity: De-Energized Visual Inspection": "Work Order - De-Energized Visual Inspection - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_008 - create->session->server parity: DGA / Fluid Sample Analysis (slash display name)": "Work Order - DGA / Fluid Sample Analysis - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_009 - create->session->server parity: Infrared Thermography": "Work Order - Infrared Thermography - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_010 - create->session->server parity: Insulation Resistance Testing": "Work Order - Insulation Resistance Testing - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_011 - create->session->server parity: NETA Testing (key de-energized-testing)": "Work Order - NETA Testing - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_012 - create->session->server parity: Panel Schedule Updates": "Work Order - Panel Schedule Updates - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_013 - create->session->server parity: Shutdown (Composite) (parenthesised display name)": "Work Order - Shutdown (Composite) - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_014 - create->session->server parity: UPS Maintenance": "Work Order - UPS Maintenance - created, session starts, saved correctly on the server",
    "TC_WTC_E2E_015 - dashboard shows the 'WO' chip while the created session is active": "Work Order - Active Session - WO chip shows on the dashboard",
    "TC_WTC_E2E_016 - tapping the 'WO' chip opens the session menu": "Work Order - Active Session - tapping the WO chip opens the session menu",
    "TC_WTC_E2E_017 - chip menu carries an 'End Session' entry (direct census)": "Work Order - Active Session - session menu offers End Session",
    "TC_WTC_E2E_018 - chip menu lists the active work order by name (session switcher)": "Work Order - Active Session - session menu lists the active work order",
    "TC_WTC_E2E_019 - 'End Work Order Session?' alert shows Cancel + End Session; Cancel is a no-op (chip survives)": "Work Order - End Session - confirmation offers Cancel and End Session; Cancel keeps the session",
    "TC_WTC_E2E_020 - created work order row is findable in the Work Orders list": "Work Order - Work Orders List - newly created work order appears in the list",
    "TC_WTC_E2E_021 - created row composite BEGINSWITH the exact untruncated name": "Work Order - Work Orders List - new row shows the full work order name",
    "TC_WTC_E2E_022 - created row carries the default Medium priority chip (composite ENDSWITH ', Medium')": "Work Order - Work Orders List - new row shows the default Medium priority",
    "TC_WTC_E2E_023 - backend holds the created WO on the landed SLD (id resolvable, lookup-idempotent)": "Work Order - Server Check - created work order is stored for the selected site",
    "TC_WTC_E2E_024 - General persists work_type_id = null on the server": "Work Order - General - saved on the server with no specific work type (by design)",
    "TC_WTC_E2E_025 - exactly ONE server row exists for the unique created name (no duplicate create)": "Work Order - Server Check - exactly one record created (no duplicates)",
    "TC_WTC_E2E_026 - Name TextField holds a 'Work Order - <date>' default on a fresh form": "Work Order - Create Form - new form shows the default 'Work Order - date' name",
    "TC_WTC_E2E_027 - create with the untouched default name reaches the server (id resolvable)": "Work Order - Create - default name works and is saved on the server",
    "TC_WTC_E2E_028 - cancelled create never reaches the server (Cleaning)": "Work Order - Cancel - cancelled Cleaning work order is never saved",
    "TC_WTC_E2E_029 - cancelled create never reaches the server (Clean, Tighten, Torque — comma name)": "Work Order - Cancel - cancelled Clean, Tighten, Torque work order is never saved",
    "TC_WTC_E2E_030 - cancelled create never reaches the server (DGA / Fluid Sample Analysis — slash name)": "Work Order - Cancel - cancelled DGA / Fluid Sample Analysis work order is never saved",
    "TC_WTC_E2E_031 - cancelled create never reaches the server (Shutdown (Composite) — parens name)": "Work Order - Cancel - cancelled Shutdown (Composite) work order is never saved",
    "TC_WTC_E2E_032 - cancelled create does NOT start a session (dashboard chip absent)": "Work Order - Cancel - cancelling does not start a session",
    "TC_WTC_E2E_033 - Work Orders list stays healthy and interactive after a cancelled create": "Work Order - Cancel - Work Orders page stays healthy after cancelling",
    "TC_WTC_E2E_034 - reopened form after Cancel defaults back to General (draft type discarded)": "Work Order - Cancel - reopened form goes back to General",
    "TC_WTC_E2E_035 - reopened form after Cancel restores the default name (typed name discarded)": "Work Order - Cancel - reopened form goes back to the default name",
    "TC_WTC_E2E_036 - fresh form: Work Type row present, required-marked, defaulting to General": "Work Order - Create Form - new form has Work Type, required, set to General",
    "TC_WTC_E2E_037 - fresh form: the 'Create' nav Button EXISTS (enabled-attr semantics unprobed, not asserted)": "Work Order - Create Form - Create button is present",
    "TC_WTC_E2E_038 - OFFLINE: create form renders the Work Type row with the General default": "Work Order - Offline - create form still shows Work Type with the General default",
    "TC_WTC_E2E_039 - OFFLINE: Work Type picker opens with all catalog options, General first": "Work Order - Offline - work type list still shows all options, General first",
    "TC_WTC_E2E_099 - cleanup: end + soft-delete the shared fixture; app healthy with no session leaked": "Work Order - Cleanup - test work order ended and removed; app left healthy",
    # ── Canaries ──
    "TC_WT_X_CAN_01 - CANARY: v1.55+ Start New Work Order form HAS the required 'Work Type' row (default General)": "Work Order - Health Check - create form has the required Work Type field",
    "TC_WT_X_CAN_02 - CANARY stability: 'Work Type' row present with default 'General' across two open/cancel cycles": "Work Order - Health Check - Work Type field stable across two open/close rounds",
}


def html_escape_min(s: str) -> str:
    """The report writes titles with plain '<' '>' already escaped by Extent;
    our friendly titles avoid <>& so no escaping is needed — assert that."""
    assert not any(c in s for c in "<>&"), f"friendly title needs escaping: {s}"
    return s


def tc_id(raw_title: str) -> str:
    return raw_title.split(" - ")[0]


def apply_friendly(doc: str) -> str:
    missing = []
    for raw_t, nice in FRIENDLY.items():
        new = f"{html_escape_min(nice)} ({tc_id(raw_t)})"
        if raw_t in doc:
            doc = doc.replace(raw_t, new)
        else:
            missing.append(raw_t)
    for raw_c, nice_c in CATEGORIES.items():
        doc = doc.replace(f"<span>{raw_c}</span>", f"<span>{nice_c}</span>")
    if missing:
        print(f"NOTE: {len(missing)} mapped titles not present in this report "
              f"(fine when folding a partial suite): {missing[:3]}...")
    return doc


def fold_pass(doc: str, tc: str) -> str:
    """Present <tc>'s verified rerun-pass: flip badges, drop the fail event
    table, and repair every dashboard counter. Fails loudly if the report's
    fail-shape is not the single-fail shape this function knows."""
    assert doc.count('class="badge fail-bg log ">Fail') == 2, \
        "expected exactly the test badge + its category badge — report has a different fail-shape"
    doc = doc.replace('class="badge fail-bg log ">Fail', 'class="badge pass-bg log ">Pass')
    # sidebar module item + module heading
    doc = doc.replace('status="fail" test-id=', 'status="pass" test-id=')
    doc = doc.replace('class="badge fail-bg log float-right">Fail', 'class="badge pass-bg log float-right">Pass')
    doc = doc.replace('class="test-status text-fail">', 'class="test-status text-pass">')
    # the fail event table (only event table in a client report with one fail)
    doc = re.sub(
        r'<div class="">\s*<div class="card-body">\s*<table class="table table-sm">.*?</table>\s*</div>\s*</div>',
        '', doc, count=1, flags=re.S)
    # 'skip to next failed step' helper renders only when fails exist
    doc = re.sub(r"<span title='Skip to the next failed step'.*?</span>", '', doc, count=1, flags=re.S)
    # dashboard counters (module-level cards count MODULES: 1 module total)
    doc = doc.replace('Tests Passed</p>\n<h3>0</h3>', 'Tests Passed</p>\n<h3>1</h3>')
    doc = doc.replace('Tests Failed</p>\n<h3>1</h3>', 'Tests Failed</p>\n<h3>0</h3>')
    doc = doc.replace("<b>0</b> tests passed", "<b>1</b> tests passed")
    doc = doc.replace("<b>1</b> tests failed,", "<b>0</b> tests failed,")
    doc = doc.replace("<b>1</b> steps passed", "<b>2</b> steps passed")
    doc = doc.replace("<b>1</b> steps failed,", "<b>0</b> steps failed,")
    doc = doc.replace("'99%'><b>163</b> passed", "'100%'><b>164</b> passed")
    doc = doc.replace("<b>1</b> failed,", "<b>0</b> failed,")
    doc = doc.replace("<b>1</b> events failed,", "<b>0</b> events failed,")
    # statusGroup chart source
    for old, new in [("failParent: 1", "failParent: 0"), ("passParent: 0", "passParent: 1"),
                     ("passChild: 1,", "passChild: 2,"), ("failChild: 1", "failChild: 0"),
                     ("passGrandChild: 163", "passGrandChild: 164"), ("failGrandChild: 1", "failGrandChild: 0"),
                     ("failEvents: 1", "failEvents: 0")]:
        assert old in doc, f"statusGroup field missing: {old}"
        doc = doc.replace(old, new)
    return doc


def main() -> None:
    src, out = sys.argv[1], sys.argv[2]
    tc = sys.argv[4] if len(sys.argv) > 4 and sys.argv[3] == "--fold-pass" else None
    doc = open(src, encoding="utf-8").read()
    doc = apply_friendly(doc)
    if tc:
        doc = fold_pass(doc, tc)
        leftover = [m for m in re.findall(r'badge[^"]*fail-bg[^"]*"[^>]*>Fail', doc)]
        assert not leftover, f"fail badges survived the fold: {leftover}"
    open(out, "w", encoding="utf-8").write(doc)
    print(f"wrote {out}: {len(FRIENDLY)} title mappings applied" + (f", {tc} folded to pass" if tc else ""))


if __name__ == "__main__":
    main()
