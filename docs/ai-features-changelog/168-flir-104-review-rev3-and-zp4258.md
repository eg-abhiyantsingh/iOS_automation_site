# 168 — FLIR v1.0.4 review revision 3 + ticket ZP-4258 (2026-09-17)

**Prompt:** "for this artifact just create a one ticket assign to rajat" — plus the user's correction:
*"if you are offline then online again then you need to click on sync button then only it will start
sync, it will not automatically sync."*

## The correction and what it invalidated

Revision 2 (changelog 167) led with two HIGH findings:
- S-1 "work created offline never uploads"
- S-2 "the Sync queue screen ships but has no reachable entry point"

**Both were wrong.**

1. Sync in this app is a **deliberate manual step**. Going back online does not upload; the technician
   presses **Sync now**.
2. The entry point exists: **Settings → (scroll past CAPTURE) → "Sync queue"**, a row with a badge
   showing the pending count. It opens a screen with a Sync now action in the app bar, Pending /
   History tabs, per-item delete and per-item timings.

**Why I missed it:** my Settings dumps were truncated (`head -N` / `tail -14`), and the row sits below
the CAPTURE section. I then treated an unreferenced `sync_queue_subtitle` string in the APK as positive
evidence that no row existed — inference stacked on an incomplete observation.

**Live re-test (device clock 17 Sep 11:33 IST):** pressed Sync now with 2 items queued overnight
(IR session + team assignment). Both uploaded in 3.66 s and 0.75 s, screen showed "Everything is
synced.", History listed both, and `GET /sld/v3/9138fd14-…` confirmed `QA-VERIFY probe 4` on the backend.

## Revision 3 of the artifact

Same URL: https://claude.ai/artifact/R2u798pPXeh2GaNWTiaivq

- S-1 and S-2 moved to **Withdrawn** with an explicit note that the miss was mine.
- New **S-3 (LOW)**: sync wording implies automatic upload — "Connected · changes sync now",
  "Waiting for N pending uploads to finish", and the Sync queue row subtitled "Wi-Fi only · connected".
  The only correct instruction ("Sync them first, then refresh") appears solely on a blocked action.
- Routes coverage row "Offline capture and sync on reconnect" changed FAIL → PASS.
- Verdict panel: **no blockers**. Final tally: 1 medium + 5 low + 6 product questions + 6 withdrawn.

## Ticket

**ZP-4258** — Bug, Medium, assigned to **Rajat Patel**, status Backlog.
<https://egalvanic.atlassian.net/browse/ZP-4258>
"[FLIR] v1.0.4 QA review: email-code policy text contradicts the backend, plus five copy and numbering defects"

Carries all six defects, the six product questions, the six withdrawn items (explicitly flagged as
"do not chase"), the artifact link, the `feature-work-order-routes` flag note, the QA-account MFA
action item and the leftover test data.

*Note:* a first create attempt timed out at the connector. Before retrying I verified via JQL that no
such issue existed (latest at the time was ZP-4257), so there is no duplicate.

## Memory corrected

`flir-ace-app-testing.md`: the sync section now records manual-sync-by-design, the exact path to the
Sync queue screen, and the dump-truncation trap that produced the wrong finding.
