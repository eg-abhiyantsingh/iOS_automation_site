# 131 — WO ticket coverage, QA outage forensics, login-chain hardening, v1.50 WO path remap

**Date:** 2026-07-15

## Work Order dev-change coverage (user tickets)

- **ZP-3109 priority (Low/Medium/High)** — WorkOrderPage v1.50 primitives
  (rows named '<name>, <Priority>' + chip; create-form 'Priority, <value>'
  config row). TC_WO_PRIO_01-04 **validated GREEN**: options exactly
  Low/Medium/High, default Medium, Medium→High persists, 7/7 list rows chipped.
- **ZP-3054 More Actions / ZP-3003 bulk asset-link / ZP-3092 many-tasks sync**
  — WorkOrder_Features_Test written with honest-skip gating (More Actions
  hunt, Session Room '+' → 'Link Existing Asset' → count-growth law, offline
  task queue → bounded sync drain). First live validation post-remap.

## CI run 29402715226 forensics — QA BACKEND OUTAGE (env, not tests)

api.qa.egalvanic.ai (QA gateway) flapped then died: curl timeouts (0 bytes)
with healthy DNS/internet; company-config on the eg-pz base stayed healthy
(200/0.8s) — so the app sometimes passed Welcome, then every gateway call
failed. Two failure shapes, both reproduced/verified:
1. Login stall — 'Failed to fetch company configuration' on a FRESH ONLINE
   install (screenshot); tests died at 'Failed to click element' on the login
   field. 2. In-app offline drop — the outage flipped the app to offline
   mid-job (iPad artifact screenshot: offline icon + punchlist menu open),
   failing the TC_AF_090-100 family that passes locally and passed iPad
   parity run 29240891572. **All 16 user-listed iPad fails = outage
   artifacts.** Recovery watcher (3×200 sustained) confirmed restoration;
   fresh-install→dashboard chain then GREEN locally in 1m38s.
   Re-verify runs dispatched on the fixed SHA: 29412530358 (arc-flash +
   smoke), 29413983680 (iPad).

## Login-chain hardening (real gaps the outage exposed)

Continue (Welcome), Sign In (Login), and the site-picker row all used
element.click() — the documented v1.50 silent no-op family — on the exact
path CI walks EVERY test, with no advancement verification. Now: verified
coordinate presses (site row: left-zone + picker-dismissed check + center
retry); dashboard 'Welcome' probe tightened to 'Welcome to' (bare match hit
the login screen); standard selectFirstSite got the >=2-comma site filter
(the dashboard's '25, Assets' tile bled through the picker and got picked).

## v1.50 Work Order path remap (unlocks TC_JOB_006-014 + AF session fixtures)

Probe-verified: v1.50 has NO per-row Start/Activate buttons — activation is
row-tap → 'Start Work Order?' alert (must run under MANUAL alerts; auto-accept
races it) and returns to the SITE HOME; the session opens from the 'Active
Work Order' banner, not by re-tapping the row. tapActivateButton and
ensureSessionDetailsOpen now lead with this flow (legacy strategies kept).
**Validated GREEN: TC_JOB_011 (26s), TC_JOB_014 (48s).**
