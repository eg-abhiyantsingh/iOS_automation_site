# 096 — Test designs prepared for two upcoming features (2026-06-25)

Per request: two tickets shared to "understand and be prepared" — neither is in QA yet; we test
once builds are ready. Deliberately NO speculative live automation was written against UIs that
don't exist yet (locators unverifiable); instead: complete test designs + one shippable API canary.

## 1. Photo Viewer: Zoom & Markup Tools
Design: [photo-viewer-markup-test-design.md](../photo-viewer-markup-test-design.md).
- **47 cases (TC_PVM_*, 13 P1)** across entry/permissions, annotation CRUD, exact-copy validations,
  unsaved/error states, offline+sync (mutation inbox), zoom/pan anchoring (TKT-933), WCAG 2.1 AA,
  audit (PLAT-FR-04).
- The strongest evidence test is API-level: **original-image SHA-256 byte-identity** before/after
  annotate (evidential integrity NFR) — no UI needed for the core guarantee.
- Zoom-anchoring automated as relative-position-in-image-rect (±2%) around `mobile: pinch`; pixel
  truth stays visual.
- **6 readiness blockers (B1-B6)** extracted for PO/dev: overlay-model confirmation, max-annotation
  cap [TBD] + copy, permission matrix, report rendering [TBD], MOB-FR-18 conflict rule, entry-point
  list.
- Repo scan confirmed zero existing zoom/markup coverage (closest: read-only viewers TC_ZP323_09_x,
  TC_ISS_IR_01-05) → all-new TC_PVM_ prefix.

## 2. [iOS] engineering_status through the sync paths (iOS #482 / backend #1057)
Design: [engineering-status-sync-test-design.md](../engineering-status-sync-test-design.md).
- Understood: boolean → 4-state SKM vocabulary (Incomplete/Estimated/Complete/Verified); raw-String
  storage on NodeV2/SLDDTONode (unknown → nil, decode survives); both sync paths (SLDSyncService
  ≤200 / BackgroundImporter >200); clone resets; never nulled; legacy boolean stays as derived
  mirror; **no iOS UI**.
- **TC_ES_*** suite designed: ×4 round-trip matrix (web set → device edit → re-sync → unchanged),
  both sync paths (>200-node fixture flagged — acme site1 has ~168), no-UI sweep, clone-reset,
  unknown-value decode, mirror-consistency (question Q1 for dev: the derived-mirror rule).
- **Shipped now: `ApiDataContractTest.engineeringStatusReadiness`** — a readiness canary + future
  vocabulary contract. SKIPs with a pointed message while the qa backend lacks the column; flips to
  PASS (+ logs the value distribution, fails on out-of-vocabulary values) the moment backend #1057
  is promoted. Every `api-contract` CI run is now a readiness probe for gate G2.

## Validation
- Compile exit 0. Full API suite live: **19 run / 0 fail / 4 skipped** — the 4th skip is the new
  canary with exactly the intended message ("engineering_status not on this backend yet…"),
  confirming qa's backend doesn't have the column (matches the ticket's dev-only status).
- Memory updated: `upcoming-features-prepared` (+ MEMORY.md index) so future sessions know both
  features are pending and where the designs live.
