# 152 — WO session nav hijack: DET_204 census rebuilt + create-form retry (2026-08-07)

## Discovery (new app contract, memory: wo-session-nav-hijack)
After opening a WO's session details and goBack-ing, the OPEN session hijacks
the next row tap — on EVERY cross-fixture switch, not intermittently. All
generic verifies pass under it (details marker, list restore — bleed-through),
so a sweep silently reads the WRONG work order's data. Proof:
- CI 31133987978: DET_204 read 'Arc Flash Data Collection' as the label for
  QA-WT04 AND QA-WT05, then 9 rows went "unreachable" (scrolls no-op under
  the hijack), 360s kill.
- Local 26.2 (v2 reactive fix): WT03 open landed on WT01's header, WT04 on
  WT03's, WT05 on WT04's — every switch hijacked; per-name header check
  caught each; soft-restart recovery read the correct label each time;
  reactive dance cost ~40s/fixture → 720s cap blown at 12m.
- Dashboard re-anchor does NOT clear it; app soft-restart DOES (why the
  per-test open pattern passes — testSetup soft-restarts).

## Fix: TC_WT_DET_204 proactive-restart census
Soft-restart + loginAndSelectSite BETWEEN fixtures (goBack dropped entirely);
every open verified BY NAME via getSessionDetailsHeaderText().startsWith(
fixture) before its label is trusted; misnav list hard-asserts empty.
Local verdict: **PASS 9m09s — 13/13 labels match the catalog exactly, 0
misnavs** (incl. punctuated 'DGA / Fluid Sample Analysis', 'Shutdown
(Composite)'). Cap: explicit timeOut=1_500_000 (25 min; CI 18.5 ≈ 2.6× local
per forms data) — explicit @Test timeOut wins over GlobalTestTimeout.

## Also
- createform CI 31156460536: 45/46, single '+'-tap flake (TC_WTC_FORM_037).
  One retry-from-fresh-anchor added to openFormGuarded (CreateForm) and
  openCreateFormGuarded (CreatePicker).
- LIST_201 validated locally: PASS 2m27s, 14/14 rows in one visit → CI
  projection ~6.5 min, fits the new 540s list budget.
- CI 31156460536 partial: cross GREEN (delete fix confirmed), catalog GREEN.
