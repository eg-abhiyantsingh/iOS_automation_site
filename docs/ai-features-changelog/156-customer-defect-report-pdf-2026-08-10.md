# 156 — Customer Defect Report PDF (every run, every final fail → formal bug entry)

**Date:** 2026-08-10
**Prompt:** After every CI/CD full-parallel-suite run, collect every test that still
fails after the rerun and produce ONE customer-shareable PDF where each failure is a
formal bug: `[Module] title`, Environment, Preconditions, Steps to Reproduce,
Actual/Expected Result, Severity/Priority, and failure screenshots.

## What was built

### `.github/scripts/generate_bug_report.py` (new, self-contained)
One formal bug entry per FINAL-failed test (post-rerun), rendered to a professional
PDF via reportlab (cover page → run summary → defects-by-area → defect index → one
page per bug).

Data pipeline (all inputs already exist in the send-email job's `all-reports/`):
1. **TestNG XML** (`target/surefire-reports/testng-results.xml` in every artifact) —
   status, duration, and the `<exception>` element (message + first stack frames)
   that the existing report scripts never read. Parse + retry-collapse + merge_rerun
   are a faithful mirror of `ios_client_report.py` (rerun overrides per
   (class,method), EXCEPT rerun SKIP never overrides a non-SKIP original).
   Rerun artifacts are detected by module key (`--rerun-key failures-rerun`);
   final-FAIL evidence prefers the rerun artifact (fresh-sim deterministic re-fail).
2. **Detailed ExtentReport HTML** (`reports/detailed/Detailed_Report_*.html`) — the
   per-test step log. DOM contract: `li.test-item[status]` blocks, `p.name` =
   "TC_ID - description", `tr.event-row` rows with Info/Fail badges and inline
   `div.eg-shot img[src=data:image/...]` per-step screenshots (logStep embeds one
   per step). Steps are cleaned (emoji/`Step n:`/`Assertion failed:` prefixes
   stripped, ✅-echo rows folded, internal WDA/Appium/driver diagnostics dropped)
   and renumbered. Evidence screenshots: last shot before the first Fail row +
   the failure-moment shot.
3. **Failure PNGs** (`screenshots/<method>_FAILED_<ts>.png`, written by teardown) —
   primary attachment, recompressed (PIL → JPEG q58) to keep the PDF lean.
4. **Steps fallback chain**: detailed-report rows → `logStep("...")` literals mined
   from the Java test source (`--src src/test/java`, brace-matched method body) →
   module template. Run 31278492395 mix: ~80% report, ~18% source, ~2% template.

Field composition:
- **Title** = `[Feature Area] <defect statement>` from the assert label; timeout
  failures ("Method … didn't finish within the time-out" / bare TimeoutException)
  become "<scenario> — screen unresponsive (flow timed out)"; identifier prefixes
  ("IsAssetSavedAfterEdit: …") stripped; >90-char fixture-noise labels fall back to
  the test description.
- **Actual/Expected** parsed from `"label - Expected: X, Actual: Y"` assert messages
  (and TestNG "expected [x] but found [y]"); requirement-style labels ("X must …")
  render as "The requirement was not met: …" with the requirement as Expected.
- **Severity/Priority**: keyword heuristics (crash/data-loss → High, cosmetic → Low)
  + `.github/scripts/bug-report-overrides.json` (first-match on TC-id/class; seeded
  with CAM-CRASH-01 / WT-NEG-01 / CONN-VAL-01).
- **Environment**: app version from `git log -1 -- apps` commit subject (the
  committed Info.plist is stale — v1.55 bundle still says 1.49), device/iOS/env
  from flags, CI run URL.
- **Reproducibility** line distinguishes rerun-confirmed fails from NOT-RERUN kept
  fails.

`--selftest` builds a synthetic artifact tree (HTML + XML + PNG) and asserts the
whole pipeline (parse → mine → compose → PDF) — driver-free, ~5 s.

### CI wiring
- **ios-tests-parallel.yml** (send-email job): new step `Generate customer defect
  report (PDF)` (id `defect_pdf`) after the consolidated report, before
  `Consolidate Client Reports`; uploads artifact **`defect-report-pdf`**
  (`Defect_Report_<date>.pdf` + `defect_report_summary.json`). The PDF is appended
  to the summary-email ATTACHMENT_LIST only when ≤18 MB (Gmail 25 MB cap);
  otherwise artifact-only.
- **rerun-failed-by-date.yml**: new `defect-report` job (needs resolve+rerun,
  ubuntu) aggregates the 3 shard artifacts (`rerun-*-report`, `--rerun-key ""`
  because the shards ARE the primary set there) → `Defect_Report_Rerun_<label>.pdf`.
- **static-checks.yml**: `generate_bug_report.py --selftest` added to the fast gate.

## Validation (run 31278492395)
Built from the run's complete artifact set (34 module artifacts + 3 rerun shards,
1.4 GB): **280 defect entries / 570 pages / 14.2 MB in ~20 s**, 278/280 with
screenshots (the 2 without are session-death crashes where no screenshot is
capturable — the entry says so explicitly).

Headline numbers are **method-level and identical to the client report attached to
the same email** (2569 total / 1850 passed / 245 failed / 474 skipped / 72.0%
pass rate — verified against `Client_Report_After_Rerun.html`). Defect entries are
**invocation-level**, so the 245 failed test cases expand to 280 documented
defects; the cover states this explicitly.

Self-test: 18/18 green (up from 10) — the new cases lock the defects found by an
adversarial review of this code (15 confirmed, all fixed):
1. **Assertion text was becoming a reproduction step.** Every failure is logged
   twice — an Info-badged `❌ Assertion failed: …` row (which carries the
   moment-of-failure screenshot) and then the Fail-badged row. Only the latter was
   treated as failure text. Now both are, by content as well as badge.
2. **The first mined step was always dropped** — `'' in text` is vacuously true, so
   the ✅-dedup guard ate any leading verification step.
3. **Data-driven failures were invisible.** `TC_ENG_050` fails for Fuse, Circuit
   Breaker, VFD Panel and Default but passes for 34 other classes; method-level
   collapse reported it as a pass and lost 4 real defects. Identity is now
   (class, method, param-signature, device class) — unstable `[L…;@1f010bf0` object
   hashes are stripped so signatures stay stable across runs, retries still collapse,
   and iPad-vs-iPhone runs of the same suite can't launder each other. The failing
   case is named in the title, the steps and the test reference.
4. **Raw locators leaked into customer text** — `Expected condition failed: waiting
   for element to be clickable: Located by By.chained({AppiumBy.accessibilityId:
   Continue})` now reads “The control “Continue” never became tappable (waited 10
   seconds).” Verified 0 internal-term leaks across all 280 entries.
5. **An unknown severity in the overrides file killed the whole PDF** (KeyError on
   SEV_COLOR) and was injected unescaped into reportlab markup. Levels are now
   normalized (Critical/Blocker→High, Minor/Trivial→Low, unknown→Medium + warning)
   and every render site escapes and uses `.get`.
6. **Steps-from-source was dead in CI** — the send-email job's sparse checkout
   omitted `src/test/java`; added. Also fixed the `[^;]` regex that lost any
   `logStep("Voltage: 480V; phase A")` (paren/string-aware walk now).
7. **App version was wrong in CI** — the job's shallow checkout made `git log -- apps`
   empty, silently falling back to the stale plist (1.49 for a v1.55 build). Both
   workflows now resolve it via `gh api …/commits?path=apps`; the script refuses to
   guess (prints “not recorded”) rather than print a wrong version.
8. Pass-rate denominator aligned with `ios_client_report.py`; `Defect_Report.pdf`
   stable copy was excluded by the artifact glob; `overwrite: true` so job re-runs
   can re-upload; `.github/scripts/**` added to the static-checks push paths;
   `--no-include-stack` now actually works; teardown/diagnostic value-echo rows
   ("Widget count: 0") filtered out of steps.

Also fixed a **pre-existing production bug in `ios_client_report.py`** found by the
same review: `("site", …)` preceded `("sitevisit", …)` in AREA_RULES, so every
sitevisit module was classified as "Site & Facility Selection" (Site Visits showed
17 defects instead of 52). Both scripts corrected.

## Notes / gotchas for future sessions
- Detailed reports are written only at Extent flush (@AfterSuite) — killed jobs have
  none; the source-mining fallback covers those tests.
- Only `apps/Z-Platform-QA.zip` is git-tracked; the `.app` is unzipped, so app
  version detection must pathspec `apps`, not the bundle.
- Old (pre-2026-06) Detailed reports used a different screenshot encoding
  (test-level `data:` anchor, no `eg-shot`); the miner tolerates both because it
  falls back to the `_FAILED_*.png` disk screenshots.
