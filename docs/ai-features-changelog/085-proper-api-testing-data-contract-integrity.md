# 085 — Proper API testing: data-contract + integrity layer (no simulator)

**Prompt:** "are you able to do proper api testing" — yes. Given how flaky the iOS UI
automation is (WDA wedges, nav lands on the wrong screen, 4-min cycles), API testing is
the reliable, fast path: it verifies the SAME backend deterministically with no simulator,
no WDA, no bleed-through DOM.

## The data-layer bug this fixed first
`TestDataApi.firstSldId()` used `GET /users/{id}/slds`, which returns **`[]`** for the
admin/RBAC account — so every data lookup silently got nothing (this is the "skip rbac for
admin" reality). The reliable source is `/auth/v2/me` → **`accessible_sld_ids`** (confirmed
live: 3 SLDs). Added `TestDataApi.accessibleSldIds()` and made `firstSldId()` prefer it
(legacy endpoint kept as fallback).

## What "proper API testing" now covers
`ApiDataContractTest` (pure HTTP, ~20-30s, wired into `testng-api-contract.xml` → runs in
the existing "API Contract Tests" CI workflow, NO macOS runner) asserts against the real
`GET /sld/v3/{id}` sync payload (173 nodes / 70 edges / 7 issues on the live QA SLD):
- **accessibleSldsPresent** — `/me` exposes accessible_sld_ids (data layer usable).
- **sldPayloadShape** — payload carries id/name/nodes/issues/edges; nodes non-empty.
- **nodeClassContract** — every live asset node carries `node_class_name` (asset-class drift catch).
- **noOrphanedConnections** — every edge `source`/`target` references a real node (the
  exact orphaned-reference bug class the UI E2E flows try to catch — now deterministic).
- **noOrphanedIssues** — every issue `node_id` references a real node.
- **issueClassAndStatusContract** — every live issue has `issue_class` + `status`.
- **nodeIdsUnique** — no duplicate assets in the sync payload.

Result locally: full API suite **18 run / 0 fail / 3 skip in 30s** (no simulator).

## Why this matters for the overall effort
The iOS UI suite's value tests (data integrity, orphaned references, class/subtype
correctness) are exactly what wedges WDA on the giant bleed-through DOMs. Moving those
assertions to the API layer makes them **fast, deterministic, and CI-cheap** (no macOS
runner contention), while the UI suite can focus on genuine interaction/rendering. The
backend `node_class_name` values also ground-truth the `ASSET_CLASSES` set the iOS
picker-button locator depends on (aligned in changelog 084-class-list).

## Ground-truth captured (web app, Playwright)
43 asset classes, 4 Motor subtypes (incl. `Low-Voltage Machine (<= 200hp)`), 11 OSHA
subcategories — verbatim option strings now available for robust iOS locators + API assertions.
