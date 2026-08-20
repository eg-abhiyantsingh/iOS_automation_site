# 162 — Ticket round-trip flow EXECUTED end-to-end (legacy field) (2026-08-20)

Per question "did you check that after edit it syncs to web": the four-state variant is still
blocked (backend #1057 not on qa), but the ticket's round-trip FLOW is now executed and PASSING
with the engineering field that exists on qa today — proving the sync machinery end to end.

## New permanent test: TC_ES_005_legacyApprovalSurvivesDeviceRoundTrip
Flow (exactly the ticket's QA step, legacy boolean instead of the absent 4-state):
1. **Web set** — real Equipment Designations grid checkbox for fixture asset
   "ABB Emax 2 E1.2 — QA created" (Circuit Breaker, site "Android Qa Site1", the only asset
   with a designation → only enabled Approved checkbox). Observed `PUT /api/node/update/{id}`;
   confirmed on BOTH the grid feed and `/sld/v3` (the iOS sync payload): `eqp_engineering_approved=true`.
2. **Device sync-in** — v1.59 on sim: login → select "Android Qa Site1" by name (199 nodes →
   the ticket's SMALL sync path, SLDSyncService ≤200).
3. **Device edit** — search + open THAT node, edit Notes, Save Changes.
   Server `modified_at` flipped to the exact save moment (13:43:02Z) — the device write APPLIED.
4. **Re-sync** — close details → Site tab → site re-selection.
5. **API verify ×3 polls/30s** — `eqp_engineering_approved = true` every poll → **approval came
   back to web UNCHANGED**. PASSED locally (4m19s); TC_ES_020 still PASS; TC_ES_010 still gate-SKIP.

Fixture is `-D`-overridable (`es.rt.site` / `es.rt.asset`). One plumbing bug found+fixed on the
first run: the Sites button lives on the Dashboard — Step 4 now closes the Asset sheet
(`clickCloseButton` → `openSiteTab`) before `clickSitesButton` (first attempt tapped a lookalike
from the Asset screen and the picker never opened).

## What this proves / what remains
Proves: web-set engineering state survives a real device edit + server-applied write + re-sync on
v1.59 — the exact data-loss class iOS #482 exists to prevent, on the small sync path. Remains
blocked on backend #1057: the same flow ×4 states via TC_ES_010 (auto-arms), the >200-node path,
clone-reset, unknown-value decode.
