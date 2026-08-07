# 153 — v1.55 PM_FORMS tab truth + list census caps + third-attempt row-open (2026-08-07)

## From CI run 31156460536 (partial) + local verification
1. **Behavior 26 skips**: "no PM_FORMS candidate tab (Forms/Tasks)" — verified
   locally that NO 'Forms'/'Tasks' session tab exists in v1.55 at all (web
   prior gone); PM forms live under the **Assets** tab. Candidate list now
   Forms/Tasks/**Assets**; BEH_012 converts SKIP→PASS locally (1m16s).
   Also added WorkOrderPage.revealBelowTheFold() + re-probe in
   detectCategoryTab (shorter 18.5 screens drop below-fold strips from the
   a11y tree entirely).
2. **LIST_211/212**: healthy censuses blew even the 540s slice budget on CI
   (local 2m28s/3m03s) → explicit @Test timeOut=900_000 each (wins over
   GlobalTestTimeout).
3. **Row-open double-flakes** (LIST_104/065, BEH_061: retry fired, both
   attempts failed — ~3 per 200 opens on 18.5): third attempt added behind
   an app soft-restart in openFixtureOrSkip (same cure as the nav hijack).
4. createform/createpicker '+'-tap retry (changelog 152) compiled into this
   push as well.

## CI run 31156460536 scoreboard (so far)
cross GREEN (delete fix proven) · catalog GREEN · createpicker GREEN ·
createform 45/46 (flake, retried now) · list 105/109 (sweeps 201/207 now
pass; 211/212 budget; 104/065 double-flake) · behavior 50/77 (1 double-flake
+ 26 PM_FORMS skips, both addressed) · forms/details/create-e2e pending.
