# 109 — Triage of the 368 CI failures (local side-by-side + CI rerun) & eng-lib blocker

**Date:** 2026-07-07
**Prompt:** "check in ci cd whether fails are real" → "check in local also, side by side" → "test only equipment library" → "complete everything and check everything carefully"

## What was done

1. **CI rerun dispatched:** `rerun-failed-by-date.yml` run [28867343990] over
   `failed-suites/latest.xml` (= failed-2026-07-03.xml, **368 tests**, from full-suite
   run 28666174784). First use of the 3-shard rerun system from changelog 105.
   Email off (internal triage).
2. **Local breadth-first sweep:** test #1 from each of the 19 failing classes, run
   one at a time on iPhone 17 Pro Max (iOS 26.2), console + screenshot inspected
   per the local driver loop rule.
3. **Equipment Library (asset_engineer) run attempted** per user request — aborted
   after 4 tests on discovering the backend blocker below.

## Local sweep verdicts (19 modules)

| # | Test | Local | Verdict / root cause |
|---|---|---|---|
| 1 | OfflineSyncMultiSite UC1 | PASS 3m51s | CI flake |
| 2 | Asset_Phase1 ATS_ECR_31 | FAIL 6m hang | **REAL** — WDA wedge in `AssetPage.tapDoneOnPicker` (:9524) via `selectAssetSubtype`; giant-DOM subtype picker; client timeouts can't interrupt in-flight WDA call |
| 3 | Asset_Phase2 CB_EAD_11 | PASS 1m29s | CI flake |
| 4 | Asset_Phase3 LC_EAD_02 | FAIL 40s | **REAL** — class picker selects "Load" when asked for "Loadcenter" (prefix collision); screenshot shows Asset Class = Load |
| 5 | Asset_Phase4 TC_PDU_04 | PASS 1m26s | CI flake |
| 6 | Asset_Phase6 MCC_AST_03 | FAIL 6m0s hang | **REAL** — same tapDoneOnPicker wedge as #2 (one root cause, ~10+ fails) |
| 7 | Authentication TC38 | PASS 36s | CI flake |
| 8 | Connections TC_CONN_022 | FAIL 6m28s | **REAL** — cannot navigate to Connections screen (3 attempts); matches persistent ~56-fail CI cluster |
| 9 | Issue_Phase1 TC_ISS_004 | FAIL 16s | **REAL (script)** — app UI is CORRECT (Open tab visibly selected); page object cannot read selected state of v1.48 chip-style tabs. Confirms Issues remap need |
| 10 | Issue_Phase2 TC_ISS_132 | PASS 2m36s | CI flake (OSHA giant-DOM screen slow but passes locally) |
| 11 | Issue_Phase3 TC_ISS_180 | PASS 1m40s | CI flake |
| 12 | Location TC_AL_002 | FAIL 2m22s | **REAL (script)** — tap on asset row in "No Location" sheet silently no-ops (iOS 26.2 click() family); needs W3C press |
| 13 | Security TC_SEC_005 | PASS 14s | CI flake |
| 14 | SiteSelection TC_SS_005 | PASS 1m28s | CI flake |
| 15 | SiteVisit_p1 TC_JOB_001 | PASS 47s | CI flake |
| 16 | SiteVisit_p2 TC_JOB_100 | PASS 3m33s | CI flake |
| 17 | SiteVisit_p3 TC_JOB_200 | **APP CRASH** | **REAL APP BUG** — `-[UIImagePickerController setSourceType:]` NSException → SIGABRT; no `isSourceTypeAvailable(.camera)` check; crashes on ALL simulators incl. CI. Crash log: `Z Platform-QA-2026-07-07-195531.ips`. Likely explains SiteVisit_p3 cascade (60 fails) |
| 18 | WOP TC_WOP_009 | FAIL 1m41s | **REAL (script)** — active WO exists (WOTest_06_07) but deactivate card locator not found on WO Details |
| 19 | ZP323 TC_ZP323_06_01 | SKIP 17s | Inconclusive — precondition guard: "Suggested Shortcuts section not present on this asset class" (emp flag IS on; data sensitivity) |

**Sampled totals: 7 real / 10 flake / 1 app-crash / 1 inconclusive.**

## Blocker: eng-lib feature flag removed from acme QA (2026-07-07)

`GET /auth/v2/me` → `company_features` (21 flags) no longer contains **`eng-lib`**
(present 2026-07-06 when the 105-case asset_engineer module passed). App gates on
`AuthService.hasFeature("eng-lib")` → Settings library card disabled ("Engineering
Library isn't enabled for your company"), TC_ENG_002/003/004 fail in seconds; all
105 module tests blocked. `NodeCoreAttributesSection` ALSO gates on eng-lib → the
Engineering section on Asset Details is hidden, so Engineering-related Asset tests
will newly fail in the next full run. No company-admin endpoint can set flags
(platform-managed). **Action: user must ask the eGalvanic platform team to
re-enable eng-lib for acme QA.** Memory: `project_englib_flag_disabled.md`.

## CI rerun final (run 28867343990, all 3 shards, completed 2026-07-07)

368 unique tests, best-result-wins across retries:
**110 FAILED AGAIN (confirmed real) / 27 PASSED (confirmed flake) / 231 SKIP (not
conclusively rerun — dead-session cascades; FAIL badge retained per changelog 105).**

| Class | FAIL-again | PASS | SKIP |
|---|---|---|---|
| Issue_Phase1 | 48 | 0 | 12 |
| LocationTest | 33 | 6 | 0 |
| SiteVisit_p3 | 10 | 0 | 50 |
| Asset_Phase3 | 7 | 3 | 0 |
| Asset_Phase2 | 5 | 3 | 2 |
| Connections | 3 | 0 | 0 |
| AP1/Auth/AP4/Issue_P3 | 4 | 11 | 0 |
| SiteVisit_p1+p2 | 0 | 0 | 122 |
| ZP323 / AP6 / Issue_P2 / Sec / SiteSel / WOP | 0 | 0 | 45 |
| OfflineSyncMultiSite | 0 | 4 | 0 |

Reading: Issue_Phase1 (48) + Location (33) are CI-confirmed deterministic — matches
local root causes (chip-state remap, silent taps). The 231-skip wall is the
camera-crash + wedge cascade killing shard sessions (shard 3 ran 5.7 h). SiteVisit
p1/p2's 122 skips + local PASSes ⇒ mostly cascade victims, NOT real. Asset_Phase6's
9 CI skips are REAL regardless (wedge reproduces locally). A clean verdict on the
231 needs a rerun AFTER quarantining camera tests + the tapDoneOnPicker fix.

## Follow-up fix list (ranked)

1. **App bug report:** UIImagePickerController camera crash (dev team) + guard our
   camera-tap tests on `simctl`/simulator detection.
2. Issues remap (state-reading of chip tabs/pickers) — unblocks ~75 CI fails.
3. tapDoneOnPicker wedge — scoped query / avoid raw findElement on picker DOMs (~10+ fails).
4. Loadcenter exact-match in class picker (Asset_Phase3 cluster).
5. Connections navigation entry (56 fails).
6. Location asset-row W3C press (39 fails).
7. WOP deactivate-card locator (1-15 fails).
8. eng-lib flag restoration (105 asset_engineer tests + Asset Engineering sections).
