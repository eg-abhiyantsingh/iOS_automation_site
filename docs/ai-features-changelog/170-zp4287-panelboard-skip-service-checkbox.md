# 170 — ZP-4287: Panelboard "Skip Service" checkbox needs a refresh (2026-09-18)

**Prompt:** "create a bug in jira. panelboard is added then click on skip service no check box is
visible but once we referesh we can see check box. assign to kush. current sprint."

**Ticket:** <https://egalvanic.atlassian.net/browse/ZP-4287> — Bug, Medium, **To Do**,
assignee **Kush**, sprint **Z-26-09-S2 (1222)**, labels `ios` `qa-found` `ui-refresh`.
Verified by reading the issue back after create.

## The defect as filed
Add a **Panelboard** asset → tap **Skip Service** → the checkbox is not drawn. Pull to refresh (or
leave and re-enter the screen) and the checkbox is there and works. State is persisting correctly;
the view is not re-rendering after the Skip Service action. Risk: a user who doesn't refresh
concludes the option isn't available for that Panelboard and moves on without setting it.

Filed from the user's manual observation — not reproduced through the automation suite, and the
ticket says so. The ticket also asks dev to check whether other asset classes share the behaviour,
since the likely fix is in the shared post-Skip-Service render path rather than in Panelboard.

## Fix version deliberately left blank
The four-field checklist (To Do + sprint + fix version + assignee) could only be satisfied three
ways here. **iOS v1.64 (14198) and v1.65 (14199) are both already released**, and every open iOS
ticket currently assigned to Kush (ZP-4284, 4278, 4256, 4243, 4210, 4206, 4205, 4204, 4203, 4202,
4201, 4200) carries an empty fixVersion. Stamping a released version would claim the bug shipped
fixed, so the field was left empty with a note in the description asking for the target build.

## Values confirmed live (2026-09-18)
- Active sprint: **Z-26-09-S2, id 1222**, board 107, 14–26 Sep — unchanged from 2026-09-17.
- **Kush** accountId `712020:9881bb6e-cbf7-47a6-bf7a-838a383b2866` (was missing from memory).
- ZP transition `2` = To Do still correct; the issue landed in To Do directly from create.
