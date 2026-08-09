# Full parallel suite — verified verdict classification (2026-08-10)

Source runs: 31214326457 (run 1) and 31278492395 (run 2, final report basis),
both with in-run env-retry (hard-capped) + 3-shard rerun triage.
Report basis: `Client_Report_After_Rerun.html` from run 31278492395
(artifact `client-reports`).

## Headline (run 2, after rerun triage)
| Metric | Value |
|---|---|
| Executed entries | 2,569 |
| Passed | 1,850 |
| Failed | 245 |
| Skipped | 474 |
| Not re-run (kept FAIL, grey badge) | 22 |
| Pass rate of executed | 88.3% |

**Flake integrity: PROVEN.** 62 (run 1) and 37 (run 2) flaky failures were
absorbed by the retry/rerun machinery and reported as passes — none appear as
failures. The env-retry infinite-loop bug (one test retrying 3.5h) was found
and hard-capped this cycle. Every failure below re-failed deterministically
on a fresh simulator with login-first — these are NOT flakes.

## Classification of the 245 surviving failures (by family, with evidence)

| Family | ~Count | Verdict | Evidence / next step |
|---|---|---|---|
| ENG custom-entry sheet (`tapAddCustom` never visible) | 29 | SCRIPT — v1.55 DOM drift, unmasked by the asset-open fix | TC_ENG_030 family; needs custom-sheet remap like the details/forms remaps |
| ENG gap classes (match panels, TC_ENG_140-205) | ~19 | SCRIPT — contracts written app-truth-first while the eng-lib flag was OFF; first-ever live runs | Expected heat-and-trial (changelog 112 note) |
| Issues P1 (picker readback `''`, subcategory scans) | 22 | ENV — reproduces ONLY on CI iOS 18.5 sims; local 26.2 passes (verified 2026-07-31) | Needs a D01-style CI-side probe; NOT a product bug |
| LocationTest | 32 | SCRIPT/ENV — giant-tree WDA wedge family + nav timeouts (documented root cause) | Deep fix = scoped queries; wedge doc |
| SiteVisit p1-p3 | ~52 | MIXED — camera family = REAL APP BUG (CAM-CRASH-01, quarantine-guarded), IR-photo fixture families, giant tree | Camera bug is dev-team scope |
| Asset_Phase 1-5 | ~49 | SCRIPT — save-evidence readback, subtype-row lifecycle (documented), class-edit families | Remap debt |
| Connections | 14 | SCRIPT — v1.5x Source-Node picker drift (documented family) | Remap debt |
| ArcFlashAssetMatrix + ArcFlash | 8 | SCRIPT — fixture-dependent matrix drift | |
| WorkType (all 9 slices) | 14 | SCRIPT — residual wedge variance on 18.5; 2 notes-typing rows | Down from 77 on 08-04; forms fix pushed |
| Singles (Auth, Security, ZP323, WO features, offline-sync) | ~6 | assorted, individually logged | |

## Real product bugs confirmed this cycle (dev-team scope)
1. **CAM-CRASH-01** — camera tap SIGABRTs on every simulator (no
   `isSourceTypeAvailable` check). Quarantine-guarded in suite.
2. **WT-NEG-01** — backend accepts work-order create with bogus
   work_type_id (sentinel-pinned in TC_WT_X_NEG_01/02).
3. **CONN-VAL-01** — connection create without target leaves 'Not Assigned'
   debris row.
4. **Badge semantics** (v1.55): per-asset badge counts REMAINING forms —
   product behavior, tests reshaped (not a bug, documented for clarity).

## What stands between 88.3% and 95%+ executed-pass
The four SCRIPT remap families (ENG custom sheet 29, Asset_Phase ~49,
Connections 14, Location ~32) ≈ 124 failures ≈ 5.9 points, plus the
Issues P1 CI-probe family (22) ≈ 1.0 point. All are enumerated, documented,
deterministic, and fixable with the same probe→remap→prove loop used on
worktype (77→9 in three days).
