# 098 — After-rerun detailed report (final-state merge) + email attachment cleanup

**Date:** 2026-07-02
**User report:** "after rerun of full automation i didn't get client and detailed report
updated — if 100 fail and 50 pass on rerun, the updated reports must show all test
cases with only 50 failing and the rest passing."

## Diagnosis (run 28458743407)

1. **The detailed report could never reflect the rerun.** `consolidated-report.py`
   dedupes with FAIL > SKIP > PASS priority — correct for the worst-case view, but it
   means a test that failed originally and PASSED on the fresh-sim rerun still showed
   FAIL. There was no final-state view at all.
2. **The consolidated email did send** (job red = the intentional quarantine gate), but
   the attachments were broken: the module-name extraction attached
   `smoke_client_report.html` **six times**, several modules were missing, and ~20
   legacy per-module Spark-template reports buried the two web-styled reports.

## Fixes

### `consolidated-report.py`
- `--exclude SUBSTR` (repeatable) — keep the rerun's XML out of the Before view.
- `--rerun DIR` — final-state merge: rerun outcomes OVERRIDE the primary per
  (class, method); a fail/skip that passed on rerun becomes **PASS + RECOVERED badge**.
- `--label` — "Before Rerun" / "After Rerun" in the header; recovered count shown
  next to the pass rate.

### Workflow (`send-email` job)
- Generates **Consolidated_Automation_Report_Before_Rerun.html** and
  **Consolidated_Automation_Report_After_Rerun.html**; the legacy filename
  `Consolidated_Automation_Report.html` now equals the FINAL state (After when a
  rerun ran, else Before) — the file the user habitually opens tells the truth.
- Email attachments are now EXACTLY four: client Before/After (web template) +
  detailed Before/After. Per-module legacy client reports are artifact-only.
- `consolidated-report` artifact now contains all three detailed HTMLs.

## Validation (real data from run 28458743407)
- Before: 1550 tests, 341 FAIL, 30.9% — matches the client Before report.
- After: 1550 tests, **16 FAIL**, 36.8%, **92 RECOVERED** — matches the client After
  report exactly (92 recovered / 16 still failing).
- Workflow YAML parses (22 jobs); script syntax clean.
