# 145 — Work-Order family live re-check of the 07-25 failed suite (112 tests)

**Date:** 2026-08-03
**Prompt:** "check everything on live work order and fail test case. and test everything"

## Context

The 2026-07-25 CI run (30144117443) recorded 401 failures, 112 of them in the
WO family. That run carries the documented backend site-load outage signature,
so every WO failure needed live re-verification: real bug vs. outage artifact
vs. script drift. Local rig: iPhone 17 Pro Max sim (iOS 26.2), Appium 3.1.2,
app v1.51, backend healthy (login probe 400/1.4s).

## Environment findings

- **First-site drift AGAIN:** landed site is now **"Test QA 16"**
  (sld `2eb27cf0-fe2a-4e02-a04f-07b10e61b49b`) — third identity this quarter.
  `ensureFixturesOnLandedSite()` self-provisioned the QA-WT family there
  (TC_WT_FIX_001 green). Drift is recurring; memory updated.

## Results by class

### WorkOrder_Features_Test (2 failed on CI)

- **TC_WO_MORE_01 — REPRODUCES locally (real, product-side).** The ZP-3054
  "More Actions" affordance is ABSENT in v1.51. Evidence: new
  `WorkTypeProbe_Test.PROBE_C_moreActionsHunt` + `PROBE_D_moreActionsTabSweep`
  dumped every visible button on the WO list, all six session tabs, and the
  session room — nav zone has only `Done` + `arrow.clockwise` (tap opens
  nothing → sync icon), zero More/ellipsis matches globally, room has only
  Back/refresh/QR. Logged as **WO-MORE-01** in BUGS.md (candidate — needs
  product confirmation the menu wasn't intentionally dropped). TC_WO_MORE_01/02
  stay RED honestly.
- **TC_WO_SYNC_01 — PASS locally (2m19s):** 3 tasks queued offline, queue
  drained to zero on reconnect without hanging (ZP-3092 healthy in v1.51);
  the CI failure was an outage artifact.

### WorkTypeCatalog_Test (2 failed on CI) — 2/2 PASS locally

- TC_WT_CAT_070: stale failure — test was re-baselined to 8 PM-Forms on 07-31.
- TC_WT_FIX_001: outage artifact — fixture bootstrap green in 49s.

### WorkType_List_Test (27) — first pass: 27/27 honest-SKIP → framework bug found & FIXED

Every test skipped with "fixture 'QA-WT…' not present in the Work Orders
list" DESPITE the ensure-pass logging success. Root cause (framework, not
app): **the fixture ensure was not site-scoped.**
`ensureFixturesOnLandedSite()` / `ensureWorkOrderFixture()` used
`findWorkOrderIdByName()` — a company-wide name search — so after first-site
drift the lookup kept "finding" the OLD site's QA-WT copies and never
provisioned the landed site. The QA-WT family deliberately exists on multiple
sites with identical names (TC_WT_FIX_017 models this), so name-only
find-or-create is structurally wrong under drift.

**Fix:** new `TestDataApi.findWorkOrderIdByNameOnSld(name, sldId)` (scans all
same-named rows, matches on the row's `sld_id`; brace-walk factored into
`enclosingObject()`), used by both `ensureWorkOrderFixture` and
`WorkTypeBaseTest.ensureFixturesOnLandedSite`. This also explains part of the
CI 07-25 wave: outage + any drift makes all list-driven WT tests skip/fail.

**Round 2 on the fixed build: 27/27 PASS (38 min).** The entire List cluster
was environment (drift + unscoped ensure), zero app bugs, zero locator drift.

### WorkType_Details_Test (17 failed on CI) — 17/17 PASS on the fixed build (33 min)

Same root cause as List: fixture reachability. No app or locator issues.

### WorkType_Behavior_Test (37 failed on CI) — 28 PASS / 9 honest-SKIP / 0 fail (63 min)

The 9 skips are the designed probe-dependent gates ("no PM_FORMS candidate
tab (Forms/Tasks) detectable" on WT03/04/06/07, "no IR candidate tab" on
WT08): the freshly-provisioned fixtures on 'Test QA 16' carry no
rooms/assets/forms yet, so the category surface legitimately doesn't
materialize. Re-runs will exercise these once the fixtures accrete session
content (or a room/asset seeding pass is added for the drifted site).

### WorkType_CrossCutting_Test (27 failed on CI) — 2 framework defects found & fixed

- **TC_WT_X_PAR_14 — NPE in the assert wrapper (framework bug, could never
  pass).** `BaseTest.assertEquals` did `expected.equals(actual)` — throws NPE
  the moment a test legitimately expects `null` (QA-WT00's
  `work_type_id = null` contract). Fixed with `java.util.Objects.equals`.
- **TC_WT_X_CAN_01 — verifyNotBlank false-RED on presented sheets (framework
  bug).** The v1.51 "New Work Order" sheet renders full content while EVERY
  descendant reports `visible == 0` to the accessibility census — the 10s DOM
  poll saw 0 elements and failed, but the failure screenshot shows a fully
  rendered form. Fix: pixel-level second opinion — when the DOM census stays
  empty, `UIStateValidator` now screenshots and consults
  `ImageAnalysis.looksBlank()` (same thresholds AssetLoadVerifier trusts);
  a truly blank screen still goes RED. Three new driver-free self-tests in
  `NotBlankPollingSelfTest` (fallback rescues sheet-quirk, does NOT weaken the
  truly-blank verdict, and never runs when the census is healthy).

**Batch result: 24 PASS / 3 FAIL (26 min); all 3 fails re-run GREEN on the
fixed build** (PAR_14 6s, CAN_01 67s — log shows the 🖼️ pixel fallback firing
on the sheet quirk exactly once, LEG_02 21s).

## Final scorecard (112 CI failures re-checked live)

| Class | CI 07-25 | Live 2026-08-03/04 | Verdict |
|---|---|---|---|
| WorkOrder_Features (MORE) | 2 fail | 2 fail (reproduced) | **REAL — WO-MORE-01** (ZP-3054 affordance absent in v1.51) |
| WorkTypeCatalog | 2 fail | 2 pass | stale re-baseline + outage |
| WorkType_List | 27 fail | 27 pass | outage/drift + unscoped-ensure framework bug (fixed) |
| WorkType_Details | 17 fail | 17 pass | same |
| WorkType_Behavior | 37 fail | 28 pass, 9 designed-skip | same; skips = fresh fixtures carry no rooms/forms yet |
| WorkType_CrossCutting | 27 fail | 27 pass after 2 framework fixes | assertEquals null-NPE + verifyNotBlank sheet false-RED |

Framework fixes shipped: site-scoped fixture ensure
(`findWorkOrderIdByNameOnSld`), null-safe `BaseTest.assertEquals`,
pixel-fallback `UIStateValidator.assertNotBlank` (+3 self-tests; suite 37/37
green). Verifier self-test suite and `mvn -o test-compile` both green.

## New probes added

- `WorkTypeProbe_Test.PROBE_C_moreActionsHunt` — active-session nav-zone dump +
  top-right-button tap-and-dump + session-room sweep.
- `WorkTypeProbe_Test.PROBE_D_moreActionsTabSweep` — WO-list nav dump + all six
  session tabs swept for More/ellipsis affordances.
