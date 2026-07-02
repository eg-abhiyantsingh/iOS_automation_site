# 097 — Deep failure analysis of run 28458743407 + three surgical fixes

**Date:** 2026-07-02
**Analyzed run:** [28458743407](https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/runs/28458743407)
(all 21 module artifacts parsed — 2,487 test outcomes, 357 FAIL / 1,558 SKIP / 572 PASS, 23%)

## Forensic findings (quantified)

| Bucket | Count | Evidence |
|---|---|---|
| Breaker-open cascade skips | 1,351 | skip messages |
| 360s per-test wedge timeouts | 104 FAIL + 66 SKIP | `didn't finish within the time-out` |
| **Asset List unreachable (site never loaded)** | **108 FAIL** | `assetsTab=false, sitePicker=true` |
| Rerun job self-destructed | 817/933 breaker-skipped | rerun artifact |
| Connections "New Connection screen" | 33 FAIL | separate bucket |
| Offline env limitation (no WiFi toggle) | 34 SKIP | runner capability |
| Location: ALL evidence lost every run | 2 jobs | artifact upload rejected |

**Time analysis:** 35.4h total test time; 61% (21.6h) spent inside FAILING tests;
107 tests burned the full 360s timeout (10.7h); 270 tests ran 2–6 min (16.6h).

### Root cause #1 — one abandoned site load condemns a whole job (THE new one)
Assets P1's log: login OK → site "(s) Wild Goose Brewery" selected → **"Dashboard wait
timeout (45s), continuing..."** → site context never persisted → every soft-restarted
test bounced to the Select Site picker → recovery re-selected the site **87 times** but
only waited **8s** for a load that needs 45s+ → `Site-context recovery did not surface
the Assets tab` ×114 → **P1/P2/P3 = 0/317 passed, ~4h each**. Control group: Assets P4
(same site, same code, same app) had ONE successful first load → **113 clean Asset List
opens, 96% pass**. The site rename to "(s) …" was NOT the cause (P4 selected it fine).
v1.38 confirmation: its P1 run got a good first load → 52 passed.

### Root cause #2 — rerun suite unbounded
`--include-skips` produced a **933-test** rerun on one runner; a wedge tripped the
breaker and 817 were skipped. Only 92 recovered / 16 genuinely failing.

### Root cause #3 — Location artifacts rejected every run
`upload-artifact` error: *"path … is not valid: …/Location Tests - Part 1 of 2**:**
Buildings + Floors/… Contains: Colon"*. The **TestNG suite name contains a colon**;
surefire creates a directory from it; the whole artifact is rejected → Location has
never shipped evidence.

## Fixes

1. **Site-load patience** — `AppConstants.SITE_DASHBOARD_WAIT_SEC` (default **120s**,
   env-tunable): used by `SiteSelectionPage.waitForDashboardReady()` (was hard-coded
   45s) and by `AssetPage.recoverSiteContext()` after an explicit site selection
   (was 8s), capped at **2 patient waits per run** (static counter) so a genuinely
   broken site can't burn hours. One 2-min wait vs a 4-hour all-fail cascade.
2. **Rerun bounded + failures-first** — `build-failed-suite.py` now separates genuine
   FAILs from cascade-skips, writes FAILs in the first `<test>` block (TestNG runs
   blocks sequentially — if the rerun runner dies, the important retries already
   happened), and caps the suite at `--max-tests` / `$RERUN_MAX_TESTS` (default 250):
   all failures always kept, cascade-skips fill the remainder, drops are logged.
3. **Location artifact fix** — removed the colon from both Location suite names.

Validated: compile clean; 26/26 driver-free self-tests green; script re-run against the
real downloaded run artifacts (300 genuine failures → failures-first suite, cap honored).

## Still open (the big rocks, in priority order)
- **Test-data bloat in the QA site** (likely why the site load is slow AND the DOM is
  giant): thousands of accumulated TestAsset_*/Trim* objects. Needs an API cleanup job
  (TestDataApi exists). This is the upstream cause of "QA is slow" generally.
- **Giant-DOM wedge** (104×360s + 1,351 cascade skips): scoped/class-chain queries on
  heavy screens — 1,506 whole-tree predicate queries vs 18 scoped today.
- Offline suite: runner has no real WiFi toggle (34 skips) — needs a strategy decision.
- Connections New-Connection-screen bucket (33) — next module-level fix.
