# 159 — v1.55 'Use library' toggle: 50 ENG defects invalid; page-object self-heal + report exclusions

**Date:** 2026-08-11
**Prompt:** user screenshot of Asset Details proving the "Add Custom" button (and the whole
SKM match panel) only exists after toggling **Use library** ON in the Engineering card —
"this make this bug invalid" (BUG-492395-023 / TC_ENG_084).

## Root cause (user-verified live)

v1.55 gates the ENTIRE SKM block — Subtype, match panel ("No possible matches"), Add
Custom, Manufacturer/Type/Function picker rows — behind an Engineering-card "Use library"
toggle that reads OFF for our drafts. The automation never flipped it, so every
match-panel element was genuinely absent *for the tests* but present for any user who
toggles it — the "ENG custom-entry sheet | SCRIPT" family from the verdict doc, now with
its true root cause. **50 of the 280 delivered defect entries were invalid as app bugs**
(AssetEngineerCustomSheet 28, TripConfig 10, GroundFault 7, AssetEngineer_Test 3+1
manufacturer-chip readback, CustomSave 2).

## Fixes

1. **Framework** — `AssetEngineerPage.ensureUseLibraryEnabled()`: fast no-op when the
   panel is already mounted; else scroll to the 'Use library' StaticText and resolve its
   switch by label predicate → same-row geometry (|Δy| ≤ 60 px, right of label — NEVER
   "first switch": 'Trust the Photos' switch shares the screen), flip when value == "0",
   wait ≤ 8 s for a panel signal. Wired as *miss-triggered self-heal* in `tapAddCustom()`
   and `openEngineeringPickerBelowLabel()` (try short → ensure → retry), so non-gated
   paths pay nothing and negative/reader methods stay pure. Backwards compatible with
   pre-1.55 layouts (no toggle row → no-op). `mvn test-compile` clean.

2. **Report generator** — overrides now support
   `{"match": …, "match_text": [...], "exclude": true, "reason": …}`: matched bugs are
   dropped from the customer PDF/CSV, Bug IDs renumber contiguously, the cover carries an
   explicit "Excluded from this report: N test failures were verified to be
   automation-side issues" row, and the exclusion count lands in the summary JSON.
   Exclusion entries never leak into severity assignment. Self-test 30 → 33 checks
   (match precision, no overreach, severity isolation).

3. **Delivered artifacts regenerated** (run 31278492395, no new suite):
   `Jira_Import_run31278492395.csv` **230 rows** (was 280) and the PDF **230 defects /
   15.3 MB** — Asset Engineering drops 67 → 17. The 17 kept ENG rows are different
   contracts (box-class Mains Type rows, [Default] negative test, free-form numeric
   TextFields) with no direct evidence of toggle gating — if the validation run greens
   them, they were gated too and vanish from the next report naturally.

## Validation

- `--selftest` 33/33 green; compile green.
- asset-engineer module dispatched on CI (`run_asset_engineer=true`, `send_email=false`)
  to prove the family goes green with the toggle fix — once green, REMOVE the exclusion
  entry from bug-report-overrides.json (it documents its own removal condition).

## Rule reinforced

"When tests fail, OUR automation has the bug, NOT the production iOS app" — this is the
canonical example: a deterministic, rerun-confirmed, screenshot-backed failure that was
still not an app bug, because the precondition (toggle ON) was the automation's to set.
