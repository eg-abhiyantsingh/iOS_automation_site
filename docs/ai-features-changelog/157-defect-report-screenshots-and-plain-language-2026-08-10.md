# 157 — Defect report: screenshot narrative, error highlighting, plain-English issue statements

**Date:** 2026-08-10
**Prompt:** more screenshots (two+), highlight the error on the screenshot, and say clearly
what the issue is — "submit button is not working", "validation is missing".

Builds on changelog 156 (`.github/scripts/generate_bug_report.py`).

## 1. Plain-English issue statement ("What is wrong")

Every defect page now leads with a highlighted line naming the defect in ordinary words,
and an **Issue type** row in the meta table. `classify_issue()` derives both from the
assertion text, the failing step and the screenshots — never invented, never an assertion
pasted verbatim.

| Issue type | Example statement generated from run 31278492395 |
|---|---|
| Control does not respond | “The Save Changes button did not respond — the screen is pixel-for-pixel identical before and after it, so nothing happened when the control was used.” |
| Validation / error message missing | “No validation message was shown. The app accepted the input without telling the user what was wrong.” |
| Data not saved | “The change was not saved. After saving, the screen does not show the new value (it still shows ‘Draft’).” |
| Screen does not open | “The expected screen did not open after using Asset row.” |
| Element missing from screen | “The Create button is not present on the screen where the user needs it.” |
| Content does not render | “The screen does not display the information it should — the expected content is absent.” |
| Incorrect value displayed | “The screen shows the wrong value — it displays ‘17’ where ‘0’ is expected.” |
| Control in the wrong state / App stops responding / Screen unresponsive / Performance | … |

Coverage on the reference run: **239 of 280 defects** carry a statement; the remainder keep
the assertion-derived Actual Result. `crash` and `not_saved` are auto-escalated to High.

Two guards learned from the first pass:
- `_looks_like_value()` — some assertions put a whole requirement sentence in the Expected
  slot; quoting *“Create button should be disabled when location is not selected”* as if it
  were a value read as nonsense. Only short, non-requirement strings are quoted back.
- The control name strips only a **leading** verb: `Save` is itself an action verb, so a
  blanket strip turned “Clicking Save Changes button” into “ Changes button”.

## 2. Screenshot narrative (up to 4 per defect)

`steps_from_block()` now returns a role-tagged shot list instead of two loose images, and
`_build_panels()` renders a 2×2 grid where each shot is captioned with **the step it belongs
to**:

    Before — Initial state
    Before — Arc Flash dashboard must open
    Action taken — metric card 'Source/Target' must be selectable
    At failure — At the moment of failure · identical to the previous screen

Near-duplicate screens are dropped via a 16×16 average-hash (`_fingerprint` + Hamming ≤ 3),
so four panels are four *different* screens. Distribution on the reference run: 152 defects
with 4 panels, 66 with 3, 54 with 2.

## 3. Error highlighting on the image

No element coordinates are logged anywhere in the framework, so highlighting is derived from
the images themselves:

- `diff_regions(before, after)` — greyscale absolute difference, thresholded at 34 to defeat
  JPEG noise, merged into row bands and boxed. The failure shot gets **red outlines around
  what changed** (95 defects on the reference run).
- `annotate()` draws those boxes plus a red banner carrying the issue statement onto the
  screenshot, sized at 5.5% of image width so it stays readable once the page shrinks the
  shot to a half-column thumbnail.
- **Pixel-identical before/after is itself the finding.** When the last action step is a real
  interaction (tap/press/save/select…) and the change fraction is < 0.04%, the report states
  the action had no effect — 45 defects on the reference run. This is the direct evidence for
  “submit button is not working”, and the attachments header says so:
  *“screens are identical — the action had no effect”.*

## 4. Size

Panels render ~250 pt wide, so screenshots are now capped at 700 px (`--shot-px`) instead of
1000 px — 4 shots per defect at the old size produced a 27.9 MB file. Final: **17.2 MB /
596 pages / 280 defects**, and the email attachment gate moved 18 → 20 MB so it still rides
along with the summary mail.

## Validation

Self-test grew 18 → 27 checks, all green locally and on the ubuntu CI runner. New cases lock:
each issue category and its wording, control-name extraction, the before/action/failure shot
roles, per-shot captions, identical-screens detection, diff-box detection, and that
`annotate()` actually modifies the image.
