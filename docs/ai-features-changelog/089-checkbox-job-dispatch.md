# 089 — Checkbox job selection (no more typing) (2026-06-18)

Per request (with screenshot of a checkbox-style "Run workflow" dialog): replace the
comma-separated **text field** with **checkboxes** for choosing which suites run — in
**both** parallel workflows.

## Why grouped checkboxes (not one box per job)
GitHub Actions `workflow_dispatch` caps a workflow at **10 inputs** and has **no
multi-select dropdown**. With ~22 jobs, one checkbox per job is impossible. So the ~22
jobs are folded into **logical group checkboxes** (exactly what the reference screenshot
did — "Assets — Parts…", "Work Orders…"). Each group still fans out to all its jobs.

## The checkboxes (both suites)
| Checkbox (input) | Runs |
|---|---|
| ✅ **Run ALL** (`run_all`) | every suite — overrides the rest |
| **Smoke** (`run_smoke`) | quick CRUD sanity |
| **Auth + Site Selection** (`run_auth_site`) | authentication, site-selection |
| **Assets — Parts 1–6** (`run_assets`) | assets-p1 … p6 |
| **Issues — Phases 1–3** (`run_issues`) | issues-p1 … p3 |
| **Connections + Location** (`run_connections_location`) | connections, location |
| **Site Visit + Work Orders** (`run_sitevisit_workorders`) | sitevisit ×4 + workorder-planning¹ |
| **Offline + ZP-323** (`run_offline_zp323`) | offline, zp323 |
| **API tests (no sim)** (`run_api`) | api-contract — ~30s, ubuntu, no simulator |
| ☑ **Send email report** (`send_email`) | suite-1 only; default **on** |

¹ Suite-2 (repodeveloper) has no workorder-planning/rerun jobs, so its Site-Visit group is
just the sitevisit phases. Suite-2 keeps its required **`branch`** chooser; that + 9
checkboxes = its 10-input budget (no `send_email` input there — it always emails).

`rerun-failures` (suite-1) runs under **Run ALL** only.

## How it works
- Each input is `type: boolean` → renders as a **checkbox**.
- Every job gate became `if: ${{ inputs.run_all || inputs.<group> }}` (was a
  `contains(format(',{0},', …job_selection), …)` string match). 20 gates in suite-1,
  18 in suite-2 converted.
- `send_email` is now a checkbox too; the existing `github.event.inputs.send_email !=
  'false'` gates still work (a boolean input still surfaces the string `"true"/"false"`
  to `github.event.inputs.*`).
- The report/email **"Selection:"** line is now built from the checked boxes
  (`inputs.run_all && 'ALL' || format(… inputs.run_assets && 'assets ' …)`), so reports
  still show what ran instead of an empty `job_selection`.

## Behavior to know
- **Defaults are all unchecked** (except Send-email). Clicking *Run workflow* with nothing
  checked → all jobs **skip** (fast, harmless). Check **Run ALL** for the full suite, or
  tick the specific groups you want — that's the multi-select: tick two groups, both run.
- Per-single-module granularity (e.g. *just* Assets P3) is no longer selectable from the
  UI — that's the trade-off the 10-input cap forces for a checkbox UX. For a single class
  locally: `mvn -o -DsuiteXmlFile=<suite>.xml test`.

## Validation
- Both YAMLs parse (`yaml.safe_load`); each has exactly **10 inputs** (within cap).
- 0 old `job_selection` references remain; gate group distribution matches job counts
  (assets×6, issues×3, auth+site×2, conn+loc×2, offline+zp323×2, sitevisit+wo×2,
  smoke×1, api×1, rerun→run_all×1 = 20).

## RBAC note (unchanged)
There are still no RBAC test classes, so there's no RBAC checkbox yet. When RBAC tests
exist they get their own group the same way.
