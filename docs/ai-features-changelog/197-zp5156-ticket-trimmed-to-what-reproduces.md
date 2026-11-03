# 197 — ZP-5156 trimmed to what reproduces + repro pack attached (3 Nov 2026)

**Prompt:** "if something is not reproducing then remove that from ticket becuase then i will not able to show that to
developer. and attach that zip file in ticket"

## What changed on ZP-5156 (from Abhiyant's account, 13:50 IST)
- **Title:** "[iOS] FLIR camera import: a visual linked from the camera shows only after reopening the app (plus 2 small items)".
  The "Add Last Photo offers a visual-only file and does nothing" part is gone: it did not reproduce on 3 Nov (4 of 4 taps
  showed "FLIR7380 not added. No IR photo is waiting for this visual.", changelog 196).
- **Description:** the Add Last Photo section, its screenshot 4, its step and its test data are removed. Findings are
  renumbered: 1 = visual needs an app restart (with the 3 Nov re-test line), 2 = marker names question, 3 = FOTRIC hint.
  Steps now use the free pair FLIR7421 + FLIR7422 and the attached zip. A possible cause has been added and is marked
  not confirmed. The claim "also happens with a real E96" is removed, because nobody had checked it.
- **Comments edited in place:** 45832 (Finding 5 → 3), 45834 (How to reproduce: rewritten for finding 1 with the zip;
  the Add Last Photo and moved ZP-5157 steps are removed), 45835 (heading names the ZP-5157 point instead of "finding 1";
  the last line says the iOS tile part moved too, matching the description), 45836 (Finding 3 → 1, adds the 3 Nov pair).
- **Attached:** `QA-03Nov-ZP-5156-repro-pack.zip` (706,824 bytes):
  - `photos-for-the-camera/FLIR7422.jpg`;
  - `HOW-TO-REPRODUCE.txt`;
  - `ZP-5156-visual-needs-app-restart.png`, rebuilt without the old "finding 2" label.
  The finding-1 screenshot from the 3 Nov pack is left out on purpose.
- Before writing, the texts were checked with Jira's wiki renderer: no errors, and screenshots 6 and 5 stay embedded.
  After writing, the ticket was read back: no "Last Photo", "FLIR7380" or "finding 4/5" left in the title, description or comments.

## Left for the owner (not deleted by Claude: a Jira attachment delete is permanent)
- `QA-02Nov-iOS-IRgap-4-add-last-photo-lone-visual.png` (the Add Last Photo screenshot; no longer shown in the description).
- Optional: `QA-02Nov-ZP-5156-mock-flir-e96-roll.zip` (2 Nov roll; its FLIR7380 was only for Add Last Photo). The new pack
  replaces it for finding 1.

## Method (same as 2 Nov)
In Chrome on the issue page:
1. Jira's own file input attaches the zip.
2. The wiki texts are loaded into a page file input.
3. They are written with `PUT /rest/api/2/issue/ZP-5156` and `PUT /rest/api/2/issue/ZP-5156/comment/{id}`, after a
   `/rest/api/3/myself` account check.

Wiki markup keeps `!file.png|width=…!` embeds working. The Rovo MCP only returns markdown with blob URLs, which would
break them.

Texts and zip: `docs/bug-evidence/2026-11-03-zp5156/`.
