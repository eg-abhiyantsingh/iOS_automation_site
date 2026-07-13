# 120 — Arc Flash module expanded to 100+ cases (ticket: "cover at least 100")

**Date:** 2026-07-13

## New classes (all app-truth invariants over live-validated ArcFlashPage APIs — no new locators)

- **ArcFlashClassMatrix_Test (TC_AF_050-061, 19 invocations):** NFPA 70E
  per-asset-class deep laws — bucket header map well-formed, bucket counts
  PARTITION the card denominator, closed label sets per metric, expansion
  row-count law, percent-range law (rows sit inside their bucket's range),
  unit words per metric, reopen determinism, drill round-trip state safety,
  expand/collapse stability, switch-away/back label stability, connection-row
  anatomy, default-metric-on-reopen law.
- **ArcFlashInvariants_Test (TC_AF_070-089, 26 invocations):** arithmetic +
  determinism — Completed+Remaining==Total, all percents 0..100, fraction
  well-formedness ×3, percent==fraction ×3, overall==weighted average,
  stat<->fraction tie, exactly-4-percents census, caption/ring consistency,
  3×reopen determinism, backgrounding survival, loading-overlay clearance,
  header-tracks-card ×3, double-select robustness, Source/Target group-sum +
  no-percent-buckets + arrow-anatomy laws, ellipsis idempotence, anchor
  census, per-metric breakdown content ×3.
- **ArcFlashPunchlistDeep_Test (TC_AF_090-100, 13 invocations):** punchlist
  laws — Show<->Hide flip ×2 tabs, badge-count stability ×3 cycles,
  disable-removes-badges (assets + edges), badges<=dashboard-Remaining bound,
  option always listed ×2 tabs, tab-switch persistence, menu-dismiss no-op ×2,
  OFF-state census, punchlist-is-a-view-filter law (dashboard numbers
  unchanged — same domain principle as the required-fields toggle).
- **ArcFlashResponsive_Test extended (TC_AF_045-047):** stat-line parse,
  Done touch round-trip, form-factor probe sanity — on BOTH form factors.

## Census

39 existing + 61 new = **100 suite invocations** on iPhone, and the identical
suite runs on the iPad job (`run_arc_flash_ipad`) — same 100 contracts per
form factor. Suite: `testng-arc-flash.xml` (6 test blocks + canary).

## Validation

Compile green. CI dispatch on the iPhone job validates the new laws live;
iPad job validation follows the boot fix (727d3a9) landing green.
