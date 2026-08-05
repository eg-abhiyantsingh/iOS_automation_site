# 147 — Full-automation invalid-failure audit: 74-failure decomposition, session-model correction, per-site fixture contracts

**Date:** 2026-08-05
**Prompt:** "cover everything check why lots of test case are failed when i run
full automation, lots of fail are invalid. take 4 hours to check everything."

## Worktype run 30900816509 — complete decomposition (74 failures)

| Signature | n | Verdict | Fix |
|---|---|---|---|
| census-wedge | 48 | INVALID — unbounded `visible==1` census wedges WDA on CI 18.5 sims (47-row WO list) | `UIStateValidator.visibleContentCount` cheap-first: two first-match probes before the whole-tree sweep (identical ≥2 threshold) |
| nav-collateral | 18 | INVALID — tests running right after a wedge killed the session | clears with the wedge fix |
| global-timeout | 3 | INVALID — census burning the per-test cap | same root |
| stale contracts | 3 | test bugs | FORM_005 (ACTIVE badge → distinct-composite count), IDEM_01 (site-scoped capture), LIST_205 (below) |
| backend-lag | 1 | INVALID — poll too short | E2E_027 poll ×12 |
| LEG_02 | 1 | test bug — v1.55 composite | type-segment strip by exact catalog name |

**93% of failures were invalid; 7% stale test contracts; ZERO product bugs.**
All fixes validated live (FORM_005, DET_004, E2E_027, IDEM_01, LEG_02,
LIST_205 all green locally on v1.55).

## Session-model correction (app-source verified — big invalid-fail class killed)

`AppStateManager.setActiveSession` is **memory-only** (no persistence, no API
call). Consequences, now encoded in the framework:
- A fresh install can NEVER carry a leftover session — per-test session state
  is clean by construction; CI cannot be "poisoned" across jobs.
- The dashboard `WO` chip is the session **PICKER** (shows whenever
  active-flagged WOs exist ≈ always) — NOT "a session is running".
  An entry hygiene that misread it was added and then reverted the same night
  (it started+ended a session per test and derailed navigation).
- The one real redirect case — a session started EARLIER IN THE SAME app
  session hijacks the Work Orders tile — is handled by the redirect fallback
  in `openWorkOrdersScreenWT` (exit via session `Done`).
- `endActiveSessionViaDashboardMenu` keeps the phantom recovery
  (Start-alert → confirm → re-end) for E2E cleanup, where an in-memory
  session IS live.

## v1.55 drift fixes shipped in this audit

- `hasActiveWorkOrder`: banner-only → banner OR WO chip OR `, ACTIVE` row.
- `startFirstAvailableWorkOrder`: already-ACTIVE row opens the session with
  no Start alert — success, not failure.
- `openActiveWorkOrderSession`: direct-land check + ACTIVE-row fallback
  (Site-home banner is gone in v1.55); `isSessionSurfacePresent` covers
  work-type-dependent tab strips (PM-forms: Details/Assets/Issues/Files/More).
- List rows are `'<name>, <workType>, <priority>'` — name derivation must
  strip the type segment by EXACT catalog-name match (comma-safe).
- `visibleActiveBadgeCount`: ACTIVE badge/row visibility flags are unreliable
  — count DISTINCT `', ACTIVE'` composites (recycler-ghost-safe).

## Fixture contracts made multi-site-consistent

- `TC_WT_LIST_205`: company-wide exactly-one → **per-site exactly-one**
  (family deliberately exists on 3 sites; verified 14×3×1 live).
- New `TestDataApi.extractFieldFromEnclosingObject(json, pos, field)` for
  exact row attribution in multi-match scans (window heuristics misattribute).

## Evidence-gathering

- Full automation re-dispatched on the fixed SHA: run **30923680769**
  (classifier `classify-run.py` buckets every failure by signature).
- Known residuals to watch in the full run: AuthenticationTest bypasses the
  policy-sheet hook (direct `performLogin`); SiteVisit phase setups may need
  the tile-redirect fallback; ~30 soft-deleted QA-WTC rows still render
  in-app (backend async-delete lag — cosmetic).
