# 137 — Verified class change: the root fix for ~450 per-class asset test call sites

**Prompt (2026-07-20):** "i mean asset test case and all other test case too" —
extend the correct-asset guarantee beyond arc flash to every module's test cases.

## The audit (test-case level, all modules)

How each module's tests ACQUIRE the asset they claim to test:

| Module | Acquisition | Verdict |
|---|---|---|
| Asset_Phase1-6 (`*_EAD_*`, ~450 sites) | shared first-asset + `changeAssetClassToX()` | **FIXED at root** (below) |
| ArcFlash matrix (TC_AF_101-139) | `AssetPage.findAssetOfClass` composite suffix | fixed (changelogs 134-136) |
| AssetEngineerClassMatrix | own picker dance + asserts the engineering BLOCK swaps (ATS row GONE after change) | safe — verifies by consequence |
| AssetEngineer suite | self-created "Ns" assets, bound fixture "Test Busway" | safe — class set in-test / fixture-pinned |
| Connections | edge flows; `getAssetClassFromEntry` reads app data (logged only) | safe |
| Issues / Offline (`selectAssetByName("")`) | "any asset" semantics, no class claim | safe |
| SiteVisit / WorkOrders | room/collect flows, any-asset; OCPD-child detection suffix-first since changelog 135 | safe |
| `openSharedAssetForEditOrFallback` | first asset or exact-name re-open of a KNOWN asset | safe — class-agnostic by design, class forced afterwards |

## Root fix: `AssetPage.changeAssetClassInternal` verified

The single method behind every `changeAssetClassToX()` **failed silently** at all
three steps (picker didn't open → print + return; option not tappable → print +
return; and NO readback after Done). Any of ~450 per-class test call sites could
run — and assert green — on a wrong-class asset. Only 2 tests had their own guard
(Phase1 ~1301, Phase5 skip-gates), and they came AFTER the silent return.

Now:
- **retry once** on picker-open / option-tap failure;
- option genuinely absent from the searchable picker after retry →
  `SkipException` ("class not selectable in this environment" — B11 Loadcenter
  semantics preserved);
- picker won't open, or **readback poll (5s) does not confirm** the new class →
  `VerificationError` ("refusing to continue on a wrong-class asset") — extends
  AssertionError, unswallowable by `catch (Exception)`;
- fast path ("Already X") unchanged; `CLASS_CHANGE_BUDGET_MS` deadline preserved
  at every step.

Zero test-file edits needed — all ~450 call sites inherit the guarantee. The
Phase5 `isCurrentAssetClassEqualTo` skip-gates remain as redundant second checks.

## Validation
- `mvn -o -DskipTests test-compile` PASS; verifier self-tests 34/34 PASS.
- Live: `ATS_EAD_01` PASS (fast path — "Already ATS");
  `BUS_EAD_01` PASS with "**Changed asset class to Busway (readback confirmed)**"
  (full picker + readback path).

## Note
The multi-agent sweep workflow was retried and died on subagent usage credits
again (resets 20:40 IST); the audit above was completed inline (greps + context
reads across all 50 test classes / 10 page objects). The workflow script is
persisted (`sweep-class-from-name-wf_6c5b166c-fec.js`) and can be re-run later
as a belt-and-braces pass, but every acquisition path is now accounted for.
