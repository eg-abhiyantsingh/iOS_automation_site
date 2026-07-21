# 140 — Created Arc Flash Data Collection work order via web app (live fixture)

**Prompt (2026-07-21):** drive the web work-order sessions page and create a
complete Arc Flash Data Collection WO (16-step flow: facility, work type, name,
due date, scope filter, auto-schedule/technician/notes/priority/hours/
description/photo type, Add Block, Create).

## Result — WO created
- **URL:** https://acme.qa.egalvanic.ai/sessions/eb242956-6aef-4098-bdac-c98a4628fac4
- **Name:** QA-AUTO AF Data Collection 2026-07-21
- **Work Type:** Arc Flash Data Collection · **Facility:** Android Qa Site1
  (the iOS suite's first-site fixture) · **Due:** 07/28/2026 · **Priority:** Medium
  · **Est. Hours:** 8 · **Photo Type:** FLUKE (matches iOS fixture IR type)
- **Scope:** Asset Classes = Circuit Breaker → 1 matching asset;
  detail page shows "Arc-flash ready: 0 of 1 (0%)" tracker.
- **Team:** Certifier = abhiyant admin; Field Technician = abhiyant singh.
- **Schedule block:** Jul 21, 2026 7:00 AM · 8h · abhiyant singh · notes
  "QA automation schedule block — arc flash data collection sweep…" (added via
  the manual Add Block editor).
- **Description:** NFPA 70E attribute-collection scope + "created by QA
  automation as a live fixture for iOS arc-flash session tests".

## Values chosen where the prompt said "specified" (none given)
facility = iOS first-site (Android Qa Site1); technician = abhiyant singh;
priority = Medium (form default, kept); hours = 8; photo type = FLUKE;
equipment = skipped (optional); due = +7 days.

## Web-UI traps learned (for future automation of this flow)
- **Escape closes the whole Create dialog** (not just the open dropdown) and
  loses all state — dismiss dropdowns by toggling their Open button or clicking
  the dialog heading.
- The **error-colored icon button on a schedule block is DELETE** (no confirm).
- **Auto-Schedule stays disabled until Est. Hours is set**; the manual "+" opens
  the Add Block editor (Start Day/Time, Length, Consecutive Days, Assign
  Technician, Notes → "Add Block").
- The scope "N matching assets" preview updates only after the class chip is
  committed; "83 matching" = unfiltered site total.

## Why this also matters for iOS tests
This is a live **active-WO-with-AF-scope fixture** on the iOS suite's site —
the arc-flash session tests (TC_AF_024/025) and WorkOrderAssetPicker/TC_WO_LINK
flows precondition-skip without an available WO; this one gives them a
deterministic, clearly-named target.
