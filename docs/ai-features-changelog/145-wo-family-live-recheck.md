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
- **TC_WO_SYNC_01 —** (result pending in this doc)

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

### WorkType_Details_Test (17) / WorkType_Behavior_Test (37) / WorkType_CrossCutting_Test (27)

(results pending — batches relaunched on the fixed build)

## New probes added

- `WorkTypeProbe_Test.PROBE_C_moreActionsHunt` — active-session nav-zone dump +
  top-right-button tap-and-dump + session-room sweep.
- `WorkTypeProbe_Test.PROBE_D_moreActionsTabSweep` — WO-list nav dump + all six
  session tabs swept for More/ellipsis affordances.
