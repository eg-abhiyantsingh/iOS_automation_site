# 101 — Connections order-dependency + Work-Orders restore fix (57-failure class)

**Date:** 2026-07-03
**Context:** run 28596206136 (v1.48 + prior fixes) failure analysis: 180 genuine fails,
10.3h burned inside failing tests (median 4.6 min/failure, 88 at the full 360s cap).
Largest class: **Connections_Test = 57 fails** (54-57 in every recent run).

## Timing facts (from the run's detailed report)
- FAIL: n=180, 10.3h total, median 276s, 88 tests >= 355s (wedge timeouts)
- PASS: n=494, median 53s — but 8+ legitimate passes run 300-358s, so LOWERING the
  360s per-test cap is NOT safe; the wedge itself is the only real speed lever.
- Job wall-times collapsed vs the previous run (Assets P1 253→88m, P2 258→36m,
  P6 259→57m) — the site-load patience + RunHealth fixes are working.

## Root cause of the Connections class (reproduced locally in 37s)
1. v1.48 **restores the app's navigation stack across the soft restart** — a pushed
   "Work Orders" screen from any earlier disturbance comes BACK in front at test start,
   and the restore is async (can land after BaseTest.testSetup's probe).
2. Most Connections tests **assume suite order** ("Navigate to Connections screen"
   comment + `shortWait()` only). On the wrong screen, positional fallbacks grab
   whatever is there: TC_CONN_014 tapped the WO screen's Refresh icon (right-most
   header control @x=386) as the "+ Add" button, then failed
   "New Connection screen should open".

## Fixes
1. `BaseTest.backOutOfAutoOpenedWorkOrders()` — setup-level heal (runs for EVERY test
   class): detects the pushed WO screen (`Start New Work Order` / `Available Work
   Orders` + `BackButton`) and taps Back. Now `protected` for subclass reuse.
2. `Connections_Test.refreshPageObjects` (@BeforeMethod, all 96 tests): heal again
   (covers the async-restore race) then `ensureOnConnectionsScreen()` — every test now
   STARTS on the Connections screen instead of assuming order.
3. `ensureOnConnectionsScreen()` recovery also backs out of the WO screen (it hides
   the tab bar, so the existing dismissers could never recover from it).

## Validation (local, v1.48)
TC_CONN_014 37s-FAIL → **28s PASS**; TC_CONN_009 / 015 / 016 (sampled across the
failing list) all PASS 27-30s. Compile clean.

## Remaining failure classes (next targets)
SiteVisit_phase3 (38, mostly 360s wedge), Issue_Phase2 OSHA (20, picker wedge),
ZP323 (8), Asset_Phase4 (7), LocationTest (6).
