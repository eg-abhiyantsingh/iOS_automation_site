# 093 — Issue-classes sheet verified vs live + test code (2026-06-22)

Per request: "check this latest file" (`testcase_file/issue_classes_template.xlsx`). Same deep
approach as the asset sync — verified against the **live web app**, not just the sheet.

## Method
The sheet's `Core Attributes.options` is a comma-joined string, but option NAMES contain commas
(e.g. NEC "Missing dead front, door, cover, etc. (NEC 110.12(A))") — so naive splitting is
wrong (the sheet shows NEC "27" when it's really 24). Pulled the authoritative array-structured
truth from `GET /api/issue_classes/user/{id}` → [issue-classes-ground-truth-2026-06-22.md](../issue-classes-ground-truth-2026-06-22.md).

## Result: Issue test DATA is already in sync — no corrections needed
| Check | Sheet / Live | Test code | Verdict |
|---|---|---|---|
| Issue classes (7 real) | NEC/NFPA 70B/OSHA/Repair/Replacement/Thermal/Ultrasonic | `IssuePage.EXPECTED_ISSUE_CLASSES` = exactly these 7 | ✅ match |
| NEC Subcategory (24) | live 24 | TC_ISS_065 partial-match `>=5` of 11 substrings — all present live | ✅ |
| OSHA Subcategory (11) | live 11 | TC_ISS_082/121 categories (Clearance/Enclosure/…) | ✅ |
| NFPA 70B Subcategory (13) | live 13 | TC_ISS_091/106-108 | ✅ |
| Thermal Severity (Nominal/Intermediate/Serious/Critical) + Criteria (Similar/Ambient/Indirect) | live | Issue_Phase2 `expectedOptions` + Severity Criteria refs | ✅ |
| Class count | **live now 19** (7 real + 12 junk: `DEVTOOL_TEST` ×8, `Test 1/2/3`) | SAFETY_05 asserts `options.size() >= EXPECTED.size()` (tolerant), SAFETY_03 each-present | ✅ already tolerant of junk |

This confirms the memory note `issues-domain-gold`: **the iOS Issue failures are the picker-wedge
UI-interaction bug (being fixed in changelogs 091/092), NOT spec/data drift.** No issue test-data
edits were warranted (unlike assets, where CB subtype 250A→225A was stale).

## One real coverage gap found (new fields, 0 tests)
The sheet + live add two **NEC multi-select** fields with **no test coverage**:
- **Consequences if Not Corrected** — Equipment Failure, Fire Hazard, Safety Hazard, Power Interruption.
- **Corrective Actions** — Fixed During Visit, Contractor Will Correct, Customer Will Correct, Estimate Required.

These are a new UI pattern (`multi_select`, distinct from the single-select Subcategory picker).
Recommend adding coverage (open NEC issue → multi-select → verify options + multi-selection
persists) **after** the picker-wedge fixes are CI-green on the Issue Details screen — adding tests
to the currently wedge-prone screen would just produce more hangs. Tracked for the Issue follow-up.

## Validation
Docs-only change (no code edit needed — data already correct). Diff of every subcategory
expectation across Issue_Phase1/2/3 vs live: 0 real mismatches.
