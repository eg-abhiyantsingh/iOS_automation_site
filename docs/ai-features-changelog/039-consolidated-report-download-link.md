＃ 039 — Consolidated Report Download Link in GitHub Actions

**Date**: 2026-04-29
**Prompt**: "create a link in gitaction to download consildated report in gitaction itself"
**Goal**: When the cross-repo email workflow runs, produce a single consolidated HTML report and make it downloadable directly from the GitHub Actions UI (in addition to the email attachment).

---

## What Was Built

### 1. Reusable report generator script

**File**: [scripts/generate_consolidated_report.py](../../scripts/generate_consolidated_report.py)

A standalone Python script that:
- Takes an artifacts directory (downloaded from a CI run) as input
- Walks all subdirectories looking for `testng-results.xml`
- Aggregates pass/fail/skip counts across every suite
- Renders a single self-contained HTML matching the existing `Consolidated_Client_Report` style:
  - Green gradient header
  - 4 summary cards (Total, Passed, Failed, Skipped)
  - Progress bar with pass-rate percentage
  - Per-module collapsible sections (failing suites pre-expanded)
  - Per-test rows with PASS/FAIL/SKIP badges and durations

**Verified locally** against `testcase_file/CI-Report-Run-24876293380/`:
```
Wrote /tmp/test-consolidated.html (180,372 bytes)
Modules: 8 | Tests: 571 | Pass: 469 | Fail: 31 | Skip: 71
```

### 2. Workflow updated to upload it as a downloadable artifact

**File**: [.github/workflows/send-smoke-email-from-dev-run.yml](../../.github/workflows/send-smoke-email-from-dev-run.yml)

Three new/changed steps:

#### Sparse-checkout the script alongside AppConstants.java
```yaml
- name: Checkout AppConstants.java + report generator script
  uses: actions/checkout@v4
  with:
    sparse-checkout: |
      src/main/java/com/egalvanic/constants/AppConstants.java
      scripts/generate_consolidated_report.py
```

#### Aggregate counts across ALL suites (was previously taking only the first suite's numbers)
```yaml
- name: Parse test counts from TestNG XML (across all suites)
  id: counts
  run: |
    PASSED=0; FAILED=0; SKIPPED=0; TOTAL=0
    for XML in $(find artifacts -name "testng-results.xml"); do
      # ... sum each suite's totals
    done
    # Output aggregated PASSED/FAILED/SKIPPED/TOTAL
```

This was a bug fix — the old logic only read ONE `testng-results.xml` and reported its numbers as the whole-run result. With multi-suite parallel runs, that meant the email subject line said "44 passed / 8 failed" (one suite's numbers) instead of the actual aggregate.

#### Generate the consolidated HTML
```yaml
- name: Generate Consolidated HTML Report
  id: gen_report
  run: |
    python3 scripts/generate_consolidated_report.py \
      --artifacts-dir artifacts \
      --output output/Consolidated_Client_Report.html \
      --run-id "$RUN_ID" \
      --branch "$BRANCH" \
      --repo "$DEV_REPO"
```

#### Upload it as a GitHub Actions artifact (this creates the download link)
```yaml
- name: Upload Consolidated Report as Artifact (downloadable from Actions UI)
  uses: actions/upload-artifact@v4
  with:
    name: Consolidated_Client_Report
    path: output/Consolidated_Client_Report.html
    retention-days: 90
    if-no-files-found: error
```

### 3. Email body now points to the download link

The email now includes:
```
Download the consolidated HTML report:
  https://github.com/.../actions/runs/<this-run-id>#artifacts

The consolidated report is also attached to this email.
```

The diagnostic summary at the end of the workflow also shows:
```
📥 Download consolidated HTML report from this run's Artifacts:
   https://github.com/.../actions/runs/<this-run-id>
   (scroll to bottom → 'Consolidated_Client_Report' artifact)
```

---

## How To Use It

### From your QA repo's Actions tab:

1. Go to: https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/workflows/send-smoke-email-from-dev-run.yml
2. Click **Run workflow** (top right)
3. Paste the dev repo run ID (e.g. `24876293380`)
4. Click **Run workflow** (green button)
5. When the run completes (~2 minutes):
   - Email arrives at recipients in `AppConstants.java` line 210
   - Scroll to the **Artifacts** section at the bottom of the run page
   - Click **`Consolidated_Client_Report`** to download the HTML

### What the artifact looks like in GitHub Actions

```
This workflow run

  Summary
    Jobs:    send-email ✓
    Duration: 2m 14s

  Artifacts (1)
    📦  Consolidated_Client_Report     180 KB
        ▼ Download                                    [click here]
```

---

## Why This Approach (vs. modifying the dev repo workflow)

**Three options were available**:

| Option | Where | Effort | Pros | Cons |
|---|---|---|---|---|
| **A: Add to OUR cross-repo workflow** ← chosen | This QA repo | Just done | No dev repo PR; full control; works today | Manual trigger after dev repo run |
| B: Add to dev repo's parallel workflow | Egalvanic/eg-pz-mobile-iOS | Needs PR | Automatic on every CI run | Needs your `unlock` for another PR |
| C: Both | Both | Both | Belt + suspenders | Twice the work |

I went with **A** because:
1. Your standing rule is "never push to developer branch"
2. You already had the cross-repo emailer pattern from PR #201
3. This works **today**, no waiting for PR review/merge
4. The dev repo equivalent is a clean follow-up if/when you want fully-automatic delivery

If you want option B (or C), tell me `unlock` and I'll prepare the PR — it's a 10-line YAML diff in the dev repo's `summary` job, basically the same script + upload step.

---

## How GitHub Actions Artifacts Work (Brief Tutorial for Manager)

`actions/upload-artifact@v4` does three things:
1. Zips the file you provide
2. Stores the zip in GitHub's artifact storage (90 days for this report)
3. Surfaces a download link at the bottom of the run page

The download URL format is:
```
https://github.com/<owner>/<repo>/actions/runs/<run-id>/artifacts/<artifact-id>
```

Anyone with read access to the repo can click and download. No login token needed beyond a GitHub account with repo access.

There's no per-file size limit beyond GitHub's overall artifact storage quota (default 500 MB per repo for public, 2 GB for private). Our consolidated report is ~180 KB, so plenty of headroom.

**Retention**: I set 90 days. Default is also 90. Max is 400 days. After that it auto-deletes.

---

## Files Modified

| File | Change |
|---|---|
| [scripts/generate_consolidated_report.py](../../scripts/generate_consolidated_report.py) | NEW — 220 lines, standalone Python script |
| [.github/workflows/send-smoke-email-from-dev-run.yml](../../.github/workflows/send-smoke-email-from-dev-run.yml) | Added 3 steps (generate + upload + extended sparse-checkout); fixed multi-suite count aggregation |
| [docs/ai-features-changelog/039-consolidated-report-download-link.md](039-consolidated-report-download-link.md) | This document |

**Validation**:
- `yaml.safe_load(...)` on the workflow → passes
- `python3 scripts/generate_consolidated_report.py ...` against existing artifacts → produces 180 KB HTML with correct totals
- No dev repo changes (per your standing rule)

---

## Next Steps (Optional — Tell Me If You Want)

1. **Mirror this in the dev repo** so the consolidated report appears automatically at the bottom of every parallel CI run page (no need to manually trigger our workflow). Needs `unlock` authorization for a small PR.
2. **Add a job summary** with the test totals — uses GitHub's `$GITHUB_STEP_SUMMARY` markdown output to show pass/fail counts directly on the workflow run page (no artifact download needed for the headline numbers).
3. **PDF export** — if you want a PDF in addition to HTML, I can add a step that uses `wkhtmltopdf` or Chromium headless to convert the HTML → PDF before uploading. Tell me which (or both) and I'll do it.
