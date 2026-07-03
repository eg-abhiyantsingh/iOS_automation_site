# 103 — Local failing-test loop: wedge born + killed in captivity, Floor read-only fix

**Date:** 2026-07-03
**User ask:** "in old ci cd all test that are failing check all that in local and fix it
might be related to xpath or any other thing"

## Local repro results (v1.48, one test at a time)

| CI failure class | Local result |
|---|---|
| Issue_Phase2 OSHA (20) | TC_ISS_131 PASS — CI failures were wedge timeouts. NEW finding **B12** (BUGS.md): the OSHA/severity family is **assert-free** (can only fail by timeout; passes verify nothing). Needs the OSHA gold list before adding assertions. |
| Authentication (2) | both PASS (10s / 1m13s) — CI `By.chained` errors were mid-suite state |
| Asset_Phase4 (4) | 3 PASS + **PB_11 reproduced the 360s wedge locally** → fixed (below) |
| LocationTest (6) | 5 PASS fast (28s–1m22s post-cleanup) + **TC_NR_002 real locator bug** → fixed (below) |

## Fix 1 — the wedge birth, caught end-to-end (AssetPage dropdown trigger guard)
PB_11's local trace showed the full chain the 88 CI timeouts die by:
1. scroll drift puts the target label ("Voltage") at a wrong Y,
2. the Y-proximity dropdown chooser accepts ANY "Select…" button within 80px —
   it clicked **"Select asset subtype"** (dist=8px) as Voltage's trigger,
3. "first available option" opened the **subtype picker sheet** (giant DOM),
4. the next field's whole-tree scroll wedged WDA → "remote browser may have died"
   → 360s ThreadTimeout.

**Fix:** a "Select …" placeholder names ITS OWN field — reject candidates whose text
references a different field's keyword (subtype/type/location/class/date/manufacturer/
voltage/rating) than the target. Verified: PB_11 6m0s-timeout → **PASS 5m9s**, correct
trigger chosen ("Select..." dist=1px), session survived.

## Fix 2 — Floor read-only false negative (BuildingPage)
`isFloorFieldReadOnly()` grabbed the FIRST 'Floor'-containing element in the whole tree
(v1.48 New Room screen: a Button "Floor 1 - …", enabled=true) → assertion failed.
Rewrote as the structural affordance check its Building sibling already used: editable
only if a Floor TextField or 'Select floor' trigger exists; display-only row = read-only.
Verified: TC_NR_002 fail → **PASS 2m38s**.

## Also
- BUGS.md **B12** logged (OSHA/severity assert-free family + fix plan).
- Post-cleanup speed visible locally: Location tests that used to graze the giant tree
  now run in 28s–1m22s.
