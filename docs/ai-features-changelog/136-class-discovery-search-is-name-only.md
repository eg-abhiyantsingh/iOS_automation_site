# 136 — Class discovery: search is NAME-only; exhaustive full-list scan is authoritative

**Prompt (2026-07-20):** "don't focus on search — make sure you have selected the
correct asset. Search is just valid for the NAME of the asset, not the asset class."

## What was wrong
`AssetPage.findAssetOfClass` (changelog 135) leaned on `searchAsset(className)` as
the primary discovery and only rescanned ~4 unfiltered screens as fallback. Since
search matches asset NAMES (user-confirmed; the placeholder's "type" is not a class
facet), any class whose fixture is NOT named after the class — e.g.
`Asset_Verify_1783737132905` (an ATS) — could be missed entirely on a long list,
producing a false "no fixture" SKIP. Selection itself was already safe (the
class-suffix scan is the verdict and `selectAssetByName` gets the exact composite);
DISCOVERY coverage was the gap.

## The fix (`AssetPage.findAssetOfClass` restructured)
- **Phase 1 — name-search shortcut** (kept, demoted): fixtures are often named by
  class, and the suffix scan still decides, so a hit can never lie. One scroll max.
- **Phase 2 — authoritative exhaustive scan**: VERIFIED filter clear, then walk the
  ENTIRE unfiltered list (cap 40 scrolls) with **end-of-list detection** — the
  visible row-composite set doubles as a scroll signature; two identical
  consecutive pages = bottom reached. Only after the full walk does the caller get
  null → honest SKIP.
- New single-pull helper `visibleAssetRowComposites()` — one page-source read per
  step serves both the signature and the class match (was 2 pulls/step);
  `firstVisibleAssetOfClass` now delegates to it.

## Validation (live, one test at a time)
- `TC_AF_133_transformerArcFlash` **PASS** — phase-1 shortcut path; picked
  `AFR_Transformer_1784402632186, Room_…, Transformer`, class-confirmed.
- `TC_AF_128_seriesReactorArcFlash` **SKIP (honest)** — phase-2 walked the whole
  list, logged "end of asset list after 3 scroll(s) — class 'Series Reactor' not
  present", 1m09s total. No false fixture verdicts, bounded runtime.
- Compile PASS; verifier self-tests 34/34 PASS.

## Cost note
Absent classes now cost a full-list walk (~1 min on the QA site; scales with list
length, capped at 40 scrolls). That is the price of a truthful "no fixture" SKIP —
previously the same verdict could be produced without looking past screen 4.
