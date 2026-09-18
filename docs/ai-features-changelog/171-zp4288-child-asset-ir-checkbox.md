# 171 — ZP-4288: child asset IR checkbox only appears after an IR photo (2026-09-18)

**Prompt:** "create a second bug that if you see child asset ir check box is not visible once you add
ir photo in child asset check box is visible create a bug to ios and assing to kush"

**Ticket:** <https://egalvanic.atlassian.net/browse/ZP-4288> — Bug, Medium, **To Do**,
assignee **Kush**, sprint **Z-26-09-S2 (1222)**, labels `ios` `qa-found` `ui-refresh`.
Linked **Relates** ↔ [ZP-4287](https://egalvanic.atlassian.net/browse/ZP-4287). All fields verified
by reading both issues back after create.

## The defect as filed
A *child* asset shows **no IR checkbox**. Add an IR photo to that child and the checkbox appears.
The parent asset shows its checkbox normally — only the child is missing it.

The point made in the ticket: the control turning up *after* the IR work is done is backwards,
because the checkbox is what a technician uses to decide what to shoot. Until then the child asset
looks like it is outside the IR scope and gets skipped.

The ticket also asks dev to confirm whether "child assets don't get the IR checkbox until they have
IR media" is deliberate — if it is, the behaviour still needs rethinking rather than closing as
by-design.

## Why it is linked to ZP-4287
Same shape as the Panelboard / Skip Service bug filed an hour earlier: the checkbox exists and works,
but is only drawn once some *other* state change happens (a refresh there, an IR photo here). Flagged
on both tickets that one fix in the shared render path may cover both — worth checking before two
separate fixes get written.

## Fix version
Left blank again, same reason as ZP-4287: iOS v1.64 (14198) and v1.65 (14199) are both released, and
no open iOS ticket in this sprint carries a fix version. Noted in the description so dev sets the
target build.

Neither bug has been reproduced through the automation suite yet; both tickets say so explicitly.
