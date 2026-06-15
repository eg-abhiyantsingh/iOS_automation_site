# 078 — Assets P6 subtype-picker hang fix (root cause) + cross-module E2E flows (wave 2)

**Prompt (cont.):** run 27485736625 — "still lots of test cases failing … I think it's code issue."

## The actual Assets P6 root cause — and the fix

Wave-1 diagnosed that 18/24 Assets P6 failures died at exactly `6m 0s` (the
per-test timeout killing a *hung* test). Wave-2 fixes the specific hang:

**`AssetPage.tapAssetClassItem`** — the method that selects an option in the
asset **class / subtype** picker — could not match labels with special
characters (`Low-Voltage Machine (<= 200hp)`, `… (> 1000V)`, `Oil-Filled
Transformer`, `Electromechanical Relay`, `Unitized Substation (USS) (> 1000V)`,
`Hybrid UPS System`, …). On a miss it fell into two 8-iteration manual-scroll
loops at 1 s implicit-wait per probe and ground the full 360 s cap. The subtype
picker is **non-searchable**, so the searchable-picker fast-fail didn't apply.

Fix (adversarially reviewed — verdict: sound; worst case ~30-40 s vs the old
6 min):
- **Hard 45 s wall-clock budget** (`TAP_OPTION_BUDGET_MS`) computed at entry;
  every scroll loop is deadline-gated and the manual loop is capped at 12 iters.
  The method can no longer consume the 360 s test cap.
- **Special-char-robust matching** (`tapByNormalizedOption` + `normalizeOption`
  + `safeContainsPrefix`): a `CONTAINS[c]` predicate on the paren-free label head
  as a cheap candidate filter, then a normalized exact compare in Java.
  `normalizeOption` maps comparators to words *first* (`<=`→`le`, `>`→`gt`) then
  strips non-alphanumerics — so `Low-Voltage Machine (<= 200hp)` →
  `lowvoltagemachinele200hp`, and sibling subtypes that differ only by comparator
  (`(<= 1000V)` vs `(> 1000V)`) stay distinct. This single strategy catches all
  18 failing labels.
- **Every cascade probe at `withImplicitWait(0)`** — a miss costs ~ms, not 1-5 s.
  Also applied to `findClassSearchField` (the one remaining 5 s probe the
  reviewer flagged).
- **Un-swallowable hard fail:** on total miss, `selectAssetSubtype` snapshots the
  on-screen option rows (`describeVisiblePickerOptions`), dismisses the picker,
  and throws `VerificationError` naming the subtype + what was visible — a clean
  ~20-45 s failure with evidence instead of a 6-minute hang. `VerificationError`
  extends `AssertionError`, so `createAsset`'s `catch(Exception)` can't swallow it.

## New cross-module E2E flows (find bugs per-module tests can't)

Every current offline test asserts **queue counts**, never post-sync **data** —
so confirmed product bugs like SLD-2 (sync replay silently dropping fields the
queue carried) are invisible. Two new classes, all hard-asserted (`assertEquals`
/ `VerificationError`-backed `verify*`), environmental-only `skipIfPreconditionMissing`:

**`E2E_OfflineSyncIntegrity_Test`** (wired into `testng-offline.xml` + root `testng.xml`):
- `TC_E2E_001_offlineMultiFieldEditSurvivesSyncReplay` — edit 3 fields offline in
  one save → sync → fresh server re-read → `assertEquals` each field persisted
  (SLD-2 shape); best-effort `TestDataApi` backend cross-check.
- `TC_E2E_002_crossSiteOfflineEditsLandOnCorrectSite` — offline edit on Site A
  (`_A_` marker) + Site B (`_B_` marker) → sync → assert each marker lands on its
  own site and ZERO cross-site bleed; `StateIntegrityChecker` no-loss/no-dup.

**`E2E_CrossModuleIntegrity_Test`** (wired into root `testng.xml`):
- `TC_E2E_010_assetDeleteOrphansLinkedIssue` — create asset → link issue → delete
  asset → open the issue: fail only if it renders the deleted asset name AND
  tapping navigates to a dead/blank screen (ghost-reference contract).
- `TC_E2E_011_issueLifecycleApiSeededThenUiVerified` — API-seed (or UI-create) an
  issue → verify in UI → change status → re-read persistence → delete → assert
  server-side deletion. Kills the empty-list false-green class.

## Validation
- `mvn -o -DskipTests test-compile` — clean (all new classes + AssetPage rewrite).
- `testng-verify-selftest.xml` — 21/21 still green.
- Hang fix adversarially reviewed: hard deadline traced, special-char match traced
  through the normalizer, all probes confirmed at implicit-0, hard-fail confirmed
  un-swallowable.

## Local validation (iOS 26.2 sim) — what it proved, and its limit
- **Hang is fixed:** `MOT_AST_03` went from `6m 0s` (timeout-killed) to a clean
  `VerificationError` fail in `3m 1s`, then `25s` once the nav guard caught the
  wrong screen earlier. The 45 s budget + un-swallowable hard-fail work.
- The local run surfaced two follow-on hardenings (shipped here):
  - **`AssetPage.selectAssetSubtype`** now short-circuits if `DriverManager.isDriverActive()`
    is false on failure — when the WDA session dies mid-selection (observed locally:
    "Error communicating with the remote browser. It may have died.") the old failure
    path (`describeVisiblePickerOptions`/`tapDoneOnPicker`/screenshot) blocked on the
    dead session until the 360 s cap. Now it fails immediately.
  - **`BaseTest.waitForAppReadyFast`** now detects the launch **splash** ("Your
    Electrical Copilot") and waits up to 15 s for it to clear, instead of the 2 s
    fast-probe firing while the app is still launching (iOS 26.2 cold start ~8 s) and
    navigation then running against the splash.
  - `describeVisiblePickerOptions` widened to dump ALL visible element types (was
    StaticText/Button only → "(none readable)") so the failure message reveals the
    real subtype-option element type — useful on CI where the session is stable.
- **Limit reached honestly:** the local iOS 26.2 sim could not reliably reach the
  subtype picker — the app stalls on splash between back-to-back runs, and mid-session
  the local **Xcode/`DEVELOPER_DIR` toolchain broke** (`Unknown device or simulator
  UDID`, missing `~/Applications/Xcode.app`). Per the standing rule
  ([[feedback_ios26_local_login_blocked]]), **CI iOS 18.5 is the source of truth**
  for this device-dependent matching. The subtype-matching logic itself was verified
  by code review (special-char normalization traced through `(<= 200hp)` →
  `lowvoltagemachinele200hp`).

## Follow-ups
- Dispatch Assets P6 on CI iOS 18.5 to confirm the 18 subtype tests now select in
  seconds (or fail fast with the widened "On-screen option rows: Type:label …"
  diagnostic if the real option element type differs from expected).
- Local toolchain: `~/Applications/Xcode.app` is missing — restore Xcode +
  `xcode-select -s` before local sim runs work again.
- The E2E flows are device-dependent; first CI run will show real signal. They
  env-gap-skip where data is read-only / Site B absent / API egress blocked.
- Optional page-object primitives noted for later: `AssetPage.openAssetByExactName`,
  `getVisibleAssetNames`, `getAttributeValue(label)` would make the SLD-2 re-read
  deterministic across all asset classes.
