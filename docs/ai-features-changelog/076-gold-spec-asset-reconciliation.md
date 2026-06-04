# 076 — Asset tests vs the node_classes "gold" spec (reconciliation)

**Date:** 2026-06-04
**Trigger:** *"@testcase_file/node_classes_template (12) - updated.xlsx this is the
gold. see this and update our existing test cases … check that in local tool."*

## What the gold is
`testcase_file/node_classes_template (12) - updated.xlsx` — the asset data model:
**Classes** (38), **Core Attributes** (class → attribute name/type/options/required),
**Subtypes**, **Options**. Plus `QA Automation Plan.xlsx` (per-module TC specs).

## Biggest finding: the gold is a HINT, not ground truth
Cross-checking the 31 failing asset tests against the gold, then **verifying on the
live sim**, showed the gold is **incomplete / column-misaligned**:
- It lists Loadcenter as having only `Size` + `Mains Type`, but the **live app shows
  Loadcenter also has Ampere Rating, Voltage, Manufacturer, Serial Number, Fault
  Withstand Rating** (only `Columns` is truly absent).
- The `options` column is shifted for several classes (e.g. Motor
  "Manufacturer {120V…}", "R P M {10 kA…}").
- Every attribute is `required = No`.

→ Trust the gold for attribute **names**; verify existence/options/types **live**
(`AssetPage.isFieldLabelPresent`). I nearly gutted 4 valid Loadcenter tests on the
gold's say-so — the local check ("check in local tool") caught it.

## Why the asset tests were failing (root causes)
1. **Dominant cause — class-change screen-bleed bug, already fixed (`5301289`).**
   Tests editing *valid* fields (CB Ampere Rating, DS Interrupting Rating, MCC
   Voltage, PB Size, LC Size, …) failed only because the asset class never actually
   changed. The seed failures predate that fix; the in-flight rerun confirms them.
2. **Genuine test bugs (gold + live confirmed) — fixed here, verified live:**
   - `MOTOR_EAD_21`: typed into `"RPM"`; the field is **`"R P M"`** (spaced). **PASS.**
   - `MOTOR_EAD_12`: edited `"Configuration"`, which **doesn't exist for Motor**
     (it's a Generator field). Converted to a truthful absence check. **PASS.**
   - `LC_EAD_12`: edited `"Columns"`, a **Panelboard** field absent on Loadcenter.
     Converted to an absence check (blocked by B11 below).
   - ATS `fillAllATSRequiredFields`: `Mains Type` set to `"Normal"/"Emergency"` →
     gold options are **`MCB`/`MLO`**; `Ampere Rating` fallback `"100"` matched
     `"100 kA"` (Interrupting Rating) → use exact `"200A"`.
3. **Subtype "shows None" tests** (`*_AST_01` for JB/Loadcenter/MCC Bucket/OCP/PDU):
   gold confirms those classes have **0 subtypes**, so "None" is the correct
   expectation — they're subtype-dropdown *detection* issues, not spec mismatches.
   `REL_AST_04` already uses the correct `"Solid-State Relay"`.

## Changes
- `Asset_Phase3_Test`: `MOTOR_EAD_21` → `"R P M"`; `MOTOR_EAD_12` → assert
  Configuration is not a Motor field; `LC_EAD_12` → assert Columns is not a
  Loadcenter field.
- `AssetPage`: ATS Mains Type → `MCB/MLO`; ATS Ampere fallback → `200A`;
  `normalizeClass()` + "Load Center" in `ASSET_CLASSES` + a space-insensitive
  Strategy 5 in `tapAssetClassItem` (so `Loadcenter` ≡ `Load Center`).

## Live verification (iPhone 17 Pro Max / iOS 26.2)
- `MOTOR_EAD_21_editRPM` — **PASS** (`R P M` matched + edited).
- `MOTOR_EAD_12_editConfiguration` — **PASS** (Configuration confirmed absent).
- `LC_EAD_12` — still RED, blocked by **B11** (picker can't select Loadcenter).

## B11 (OPEN) — asset-class picker cannot select "Loadcenter"
All 7 Loadcenter tests fail at class-change: the picker has **no selectable
"Loadcenter" option** — `accessibilityId("Loadcenter")` returns nothing, a
normalized (space-insensitive) scan over all picker rows finds no match, and the
list scroll doesn't surface it. ATS/Motor select fine, so it's specific to
Loadcenter (likely a lazy SwiftUI list whose off-screen rows aren't in the DOM +
a scroll that doesn't move that picker). Each attempt costs ~23 min, so a proper
fix needs a dedicated picker-scroll technique — tracked in BUGS.md (B11).
