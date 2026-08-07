# 150 — Forms audit complete: 45/45 verified first-to-last (2026-08-07)

## User ask
"continue check form 1st to last. i dont want any wrong fail test case take as
much time you want its important for me."

## Result: all 45 TC_WT_FORM_* tests hold a REAL green verdict; 0 product bugs.
Audit ran 2026-08-06 21:40 → 2026-08-07 05:44 on the dedicated sim
(4 full/partial passes + 6 single-test probes). Every fail/skip along the way
was reproduced, root-caused with evidence, and fixed in OUR code:

1. **Run 1 — 45/45 skip**: app WO list bloated to 148 rows by undeletable
   QA-WTC debris (backend delete drift, changelog 149) → fixtures buried
   beyond scroll reach. Fixed by the backend sweep + delete-endpoint fix.
2. **Run 2 — 14 pass / 31 skip**: tests 001-014 (activation contract) all
   green; the room-tree walker was wrong for the v1.55 tree:
   - `'<bldg> › <floor>, <room>'` rows NAVIGATE into rooms (trailing segment
     is the ROOM NAME) — the walker misread ' floor' rows as expandables;
   - the 'Add' toolbar Button passed the bare-room filter and got tapped;
   - back-out reset the walk with no re-scan.
   Walker rewritten (WorkOrderFormsPage.openFirstRoomWithAssetsInTree):
   path-row strategy + control-name exclusions + back-out recovery +
   give-up screen census diagnostics.
3. **Run 3 — 32 pass / 7 fail / 6 skip**: all 7 fails = exactly-6-minute
   timeout kills (the SAME wedge signature as the CI forms slice!) — the
   enter-and-back-out walk toured ~9 empty debris rooms and burned 5 of the
   6 budget minutes; skips = the same walk running dry. Fixed with the
   DETERMINISTIC walker: scroll-to-top, whole-list fast-pass sweep for
   count-advertised rooms ('N assets' = definitive), path walk demoted to a
   3-entry last resort. Per-test walk cost: ~5min → ~40s.
4. **Remainder — 10/12 green**; final 2 fails were REAL assert mismatches:
   badge=3 vs chips=4. Evidence (censuses at 02:57 vs 05:02) shows Switch-1's
   badge dropped 4→3 exactly when the first form save landed, chips constant:
   **v1.55 badge counts REMAINING (incomplete) forms, not total instances.**
   July's equality invariant only holds on pristine assets. TC_WT_FORM_023/039
   reshaped to the durable contract (0 <= badge <= chipCount; save never
   increases the badge). Both re-validated green.

## Also this session
- Disk-full emergency (ENOSPC broke live runs): freed ~6GB — DerivedData
  minus the current WDA build, `simctl delete unavailable`, reports >3 days.
  Advisory: ~/Library/Developer/CoreSimulator/Devices holds 18GB.
- Memory: new `v155-session-assets-tree` contract (path-row tree, 'Add' trap,
  No-Active-Assets rooms, Switch-1 lives in 'Optional Notes Room_21').

## CI significance
The local 6-minute-kill reproduction + deterministic walker is the likely fix
for the CANCELLED/hung CI forms slice (26 timeout kills on run 31081238514).
Fresh run_worktype dispatched on this tree for the parallel verdict.
