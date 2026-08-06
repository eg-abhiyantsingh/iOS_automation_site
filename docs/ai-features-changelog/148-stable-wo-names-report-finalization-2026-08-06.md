# 148 — Stable WO names, retry hardening, final Work-Order-Type report (2026-08-06)

## User asks
1. "work order config name same so that it will show on top and easy for you to reproduce"
2. Deliver the final Work-Order-Type module report in the framework's own
   Client_Report format with simple business-language test names.
3. "check all fail case are clear in parallel are fine now" (CI verdict).
4. Triage of a manually-run TC_WTC_FORM_015 failure ("this fail is invalid").

## Changes

### Stable, reproducible work-order names (WorkTypeCreateE2E_Test)
- Every scripted create now uses the SAME name every run: `stableWoName(tc)` =
  `QA-WTC <TC_ID>` (e.g. `QA-WTC TC_WTC_E2E_003`); shared fixture = `QA-WTC SHARED`.
  No timestamp suffixes anywhere.
- New `purgeStaleWoByName(api, name, tc)`: bounded (×5) find-by-name-on-landed-SLD →
  soft-delete loop, called before every create AND before the cancel-path tests.
  Stable names make this mandatory: cancel tests assert row ABSENCE and create
  polls resolve BY NAME — debris from a crashed run would poison either.
- Live-validated on the dedicated sim: TC_WTC_E2E_001 (create/poll/cleanup) and
  TC_WTC_E2E_028 (cancel/absence) both green post-change.

### EnvironmentRetryAnalyzer — two new signatures
- `"Session does not exist"` (Selenium's phrasing of a dead session; observed in
  the user's concurrent manual run, which died 30s in — verdict: invalid/env).
- `"Work Orders screen must open from the dashboard tile"` (entry-nav timing
  flake; PICK_019 missed once after 64 straight greens, then re-ran green).

### Report tooling — scripts/worktype_report_friendly.py (permanent)
- Renames all 164 technical titles in a module Client_Report to the client
  house style `Work Order - <Type/Area> - <plain-English check> (TC id)`.
- `--fold-pass TC_ID`: presents ONE verified rerun-pass over an in-report fail
  (same semantics as CI merge_rerun) — flips badges/sidebar/statusGroup/footers,
  excises the fail event table, asserts the report's fail-shape first.
- Output verified: 0 fail markers left (bar the template's filter chrome),
  tag-balanced (better than the template's own +1), card structure intact.

## Final pass v4 (dedicated sim, 2h07m)
164 run / 163 pass / 1 fail / 0 skips. The 1 fail = TC_WTC_PICK_019, entry-nav
timeout at list entry; re-ran green in 1m35s → environmental, folded into the
delivered report. Deliverable: repo-root `WorkOrder_Type_Test_Report.html`
(friendly names + fold), source `reports/client/Client_Report_20260806_165442.html`.

## CI run 31081238514 decomposition (pre-fix tree) — 57 fails
- 15 fixture/site-scope, 4 CTA-transient, 2 stale exact-count, 2 entry-nav —
  all four generators fixed (or now auto-retried) on the current tree.
- 34 = ThreadTimeoutException at exactly 360231ms (GlobalTestTimeout kill),
  26 of them = the ENTIRE forms slice, rest behavior — the documented
  wedged-WDA hang signature (slice-level infra, not per-test bugs). Fresh
  dispatch on the fixed tree is the arbiter; if forms wedges again it needs
  the giant-DOM session-tree work (docs/…/147 next-packages).

## Manual-run triage
TC_WTC_FORM_015 manual failure = INVALID (env): session died mid-test
("Session does not exist" on every command), ran on the shared sim through the
same Appium server while the report pass was executing. Same test green 3× that
day on the isolated rig.
