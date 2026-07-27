# 142 — Circle activation + PM-forms execution suite (TC_WT_FORM_001-045)

**Prompt (2026-07-27):** screenshots of the new WO-list circle control + per-asset
procedure forms — "check and cover this new feature; you need to click on circle
one only."

## Ground truth (probe runs 12-15 on the local sim, v1.51)

**Circle activation:** the WO row is ONE full-width Button — the trailing circle
is a TAP ZONE (right edge, x≈maxX-35), NOT a separate element. Tap → 'Start Work
Order'/'Cancel' alert → confirm = activation + the session opens. After
activation: row composite gains `', ACTIVE'`, an 'ACTIVE' StaticText renders,
and Start-New's composite flips to '…, End current work order session first'.
**Trap found live:** the ACTIVE row can report `visible == 0` while plainly
rendering — strict visible==1 row queries return nothing (this also explained
probe-14's null composite). `WorkOrderPage.onScreenRowOrNull` +
`WorkOrderFormsPage.rowComposite` now rect-check a no-visible-filter fallback.

**PM-forms flow (probed in the QA-WT04/CTT session):** Details → Assets tab →
tree ('Bldg_9106, 15 floors' → floors → '<room>, N assets') → 'Assets in Room'
→ asset rows `'<name>, <class>, <formCount>'` (badge is PER-CLASS: Switch=4,
Transformer=3) → form screen: chips `'<Work Type> — <Procedure>'` + 'plus',
nav Back/trash/square.and.pencil/checkmark, 'Procedure Steps', 'Result' +
'Value / Notes' table, per-step '—'→Pass/Fail dropdowns + TextFields; Fail
reveals 'Description of Failure' + Photos. Invariant: badge N == chip count.
Full contract: gold spec **§3e**. WDA rule reaffirmed: untyped name-CONTAINS
scans wedge WDA post-activation (probe 13) — TYPE-bound queries only.

## Code
- `pages/WorkOrderFormsPage.java` (NEW) — circle/alert primitives
  (tapCircleExpectAlert/confirm/cancel, isRowActive, ACTIVE badge count,
  Start-New state), tree→room walk, asset badges, form-screen primitives
  (chips, result dropdowns w/ geometry disambiguation vs '—' chips, notes
  fields, failure card, save/back).
- `tests/WorkType_Forms_Test.java` (NEW) — **45 tests** TC_WT_FORM_001-045:
  activation contract (12, incl. switch-while-active DISCOVERY 009), tree→room
  (8), form anatomy (10, incl. badge==chips invariant 023), form filling (10:
  Pass/Fail readback, failure card show/hide, notes persistence, save,
  saved-Pass persistence), cross/safety (5, camera never touched).
- `WorkOrderPage` — ACTIVE-row visibility fallback in row helpers;
  `WorkType_List_Test` — `', ACTIVE'` suffix tolerance (5 check sites).
- Wiring: `parallel/testng-worktype-forms.xml`; CI `worktype-tests` matrix
  gains the `forms` slice; root testng.xml block updated.

## Validation
- `mvn -o test-compile` green; verifier self-tests green.
- **Live-green: TC_WT_FORM_045 full-loop smoke (3m51s)** — circle-activate →
  tree → room → form → Result=Pass (readback asserted) → checkmark save →
  unwind → list with ACTIVE row present. Fail-path TC_WT_FORM_033 run in the
  same session (see log).
- Suite total now **497 TC_WT_* cases** (452 + 45).
