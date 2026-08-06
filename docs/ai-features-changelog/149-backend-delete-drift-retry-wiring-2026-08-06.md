# 149 — Backend delete drift + retry wiring + forms audit kickoff (2026-08-06 night)

## User ask
"continue check form 1st to last. i dont want any wrong fail test case take as
much time you want its important for me" — plus the standing accuracy mandate.

## Fresh CI run 31108181084 (b14f5f1) decomposition
- createform **46/46 GREEN in parallel**; catalog 98/99; create-e2e 27/40 (3F/10S).
- 5 slices (list/behavior/details/forms/createpicker) were CANCELLED one-by-one
  over 40 min from the Actions UI — no verdict, not a code signal (cross ran on
  to completion through the same window).
- cross: WDA session died mid-slice ("remote browser may have died" spam) → no
  results XML → gate red. Environmental; no code change.

## Root cause #1 — backend DELETE drift (the big one)
`TestDataApi.deleteWorkOrder` used `PUT /ir_session/update/{id}
{"is_deleted":true}` → the backend's ASYNC mutation queue answers
`_mutation:{status:"received"}` + HTTP 200 and **never applies the delete**.
Proof: 94 live `QA-WTC` rows (incl. days-old timestamped ones), v5 logged 81
"accepted" deletes, zero applied; a probe delete stayed `is_deleted:false`
minutes later. Consequences: WO-list debris crowded the 100-row `QA-WT` search
window (94/100 rows) → catalog `TC_WT_FIX_017` false-failed ("fixture missing"),
and `purgeStaleWoByName` could never actually purge (stable names collided:
2× 'QA-WTC TC_WTC_E2E_014' live).

**Discovery method:** drove the WEB app (acme.qa.egalvanic.ai) with Playwright,
deleted a WO from the UI, captured the network call:
`DELETE /api/ir_session/{id}` + header **`x-direct-write: true`** + body `{}`
→ `{"success": true}`, row gone immediately. Replayed via curl on the api.qa
host: works. Without the header the same DELETE 400s (no body) or queues
without applying.

**Fixes:**
- `TestDataApi.deleteWorkOrder` → the direct-write DELETE, success asserted on
  the response body (whitespace-tolerant `"success":true`).
- One-off hygiene sweep executed against the QA backend: **117 `QA-WTC` +
  15 `QA-WT-NEG-` leaked rows deleted**; all 14 fixture prefixes visible in the
  `QA-WT` window again (42 rows total).
- `TC_WT_FIX_017` window-proofed: queries PER exact fixture name (own 100-row
  window each) instead of one crowdable 'QA-WT' page.

## Root cause #2 — retry layer never engaged (TestNG single-transformer rule)
TestNG honors only ONE `IAnnotationTransformer`; we registered TWO
(GlobalTestTimeout + EnvironmentRetryTransformer) → "AnnotationTransformer
already set" warning (visible in the user's own local run log) and the retry
wiring was silently dropped (E2E_015/032 failed the entry-nav flake with NO
retry attempt in the results XML). **Fix:** merged retry wiring into
`GlobalTestTimeout` (timeout + retryAnalyzer in one transformer); deleted
`EnvironmentRetryTransformer`; services file now registers GlobalTestTimeout +
DeadSessionCircuitBreaker only.

## Root cause #3 — entry-nav flake generator hardened at the source
`WorkTypeBaseTest.openWorkOrdersScreenWT` now makes a full second
dashboard→tile attempt before asserting (1 local + 2 CI occurrences of the
one-off tile-nav miss; a double miss remains a hard fail).

## E2E_027 — hypothesis DISPROVEN, kept as real signal
Suspected a minute-tick race (default name embeds hh:mm). Backend check shows
NO row created 14:5x UTC under ANY 'Work Order - Aug 6' name → the create
never reached the server despite the session activating. Not a test bug — a
possible app create-sync miss. No code change; watch for recurrence (correctly
NOT auto-retried: could be product signal).

## Forms audit (user's main ask)
Source audit of all 45 TC_WT_FORM_* done (only 2 were ever live-validated;
skip-gates honest, queries bounded). Full 001→045 live run started on the
dedicated sim right after the v5 confirmation pass (which was 164/164 native
green — report swapped in d739e8b). Per-fail evidence loop to follow.
