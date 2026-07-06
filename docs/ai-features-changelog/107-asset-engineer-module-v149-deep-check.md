# 107 — v1.49 deep change-check + NEW asset_engineer module (Equipment Library / Engineering section)

**Date:** 2026-07-06
**Trigger:** "check 1.49 in depth any changes are there. and now we need to do
engineer section now. we need to cover new module called asset_engineer
library. check everything in local first. take reference of web for engineer
if you need any help."

---

## Part 1 — v1.49 in-depth change check (evidence-based)

Method: `strings`-diff of the app's real code container
(`Z Platform-QA.debug.dylib`, 102 MB — the outer executable is a 57 KB stub)
between the v1.48 zip recovered from git (`d590166`) and the v1.49 bundle:
546 new / 382 removed strings, every information-bearing one categorized.
Cross-checked against live v1.49 DOM dumps (`target/engdump/`).

### Headline: ZP-2794 encrypted equipment-library transport
- New endpoint `/auth/v2/key-exchange` (piggybacks on login response; dedicated
  call is the fallback) + 4 new v2 endpoints:
  `/v2/equipment-library/{taxonomy,enums,skm-headers,skm-tree}`.
- AES-256-GCM + HKDF-SHA256 envelope (`ZP-2794 v2/library-data AES-256-GCM
  HKDF-SHA256 v1`), gzip-compressed payloads, keychain keys
  `crypto.libSessionKey`/`crypto.libSessionKid`.
- New types: `EncryptedEnvelope`, `LibraryCrypto(Error)`,
  `LibraryEncryptionService`, `LibraryKeyExchange`,
  `APIClient.KeyExchangeCoordinator`.
- **Fail-open**: every error path "stays on plaintext v1" — login cannot break
  from key-exchange failures.
- New user-facing alert to allowlist: **"Server returned an empty library.
  Existing data was kept."** (`engineering.emptyLibraryResponse`).

### Other v1.49 changes
- **Assets + Connections tabs rewritten to snapshot rendering**
  (`AssetRowSnapshot`/`ConnectionRowSnapshot`/`ConnEdge*Snap` replace live
  NodeV2/EdgeV2+nodeById lookups) with **async load + debounced search**
  (`_isLoadingAssets`, `_searchDebounceTask`). Structural locator risk on both
  tabs; tests must settle after navigation/search keystrokes. New endpoint
  display states `isBroken`/`missingSource`/`notAssigned`/`hasDeletedParent`;
  arc-flash fields `afIsComplete`/`_baseHasAF`.
- **Bulk node↔session mapping**: `/mapping/node-session/bulk-create` +
  offline-queue fallback; `linkListeningTasksToNode` → `...Nodes` (plural).
- **DI refactor**: `APIClientProtocol`, `AuthServiceProtocol`,
  `NetworkStateProtocol`, `SLDSyncServiceProtocol`, `IRSessionManagerProtocol`.
- **Auth**: `_canUseBiometricAuth` flag (sim impact nil).
- **NO user-visible label/button renames** — all removed strings are debug
  logs. The v1.48 Issues DOM regression is **NOT fixed** in v1.49; the Issues
  remap remains open.

---

## Part 2 — NEW module: asset_engineer (SKM Equipment Library + Engineering)

### What shipped in the automation repo
- **`pages/AssetEngineerPage.java`** — page object for: Settings Equipment
  Library card (+"Load Device Library?" alert + download progress/terminal
  states), Asset Details Engineering section (banner, class-gated blocks,
  pickers/segments/fields), SKM match panel, bound-library card, custom
  equipment sheet, close/discard.
- **`tests/AssetEngineer_Test.java`** — 15 tests:
  - 100s Settings/library: card+state (001), alert copy exact (002),
    download end-to-end state gate (003), downloaded-state format (004).
  - 200s per-class engineering: unlock oracle (010), box block + locked
    System Voltage (011), Mains Type pick + discard-integrity (012), fuse
    block segments (013), v1.49 unconditional OCP match panel (014),
    transformer block (015).
  - 300s matching/bound: kVA input filter (021), bound busway card (022),
    unlink discard-integrity (023).
  - 400s custom sheet Save-gating (030). 500s Add-Asset modes+class picker
    (040). 601 transformer manufacturer→match header (**WEDGE-CONTAINED
    last**, see defect below).
- **Suites**: `testng.xml` block + `parallel/testng-asset-engineer.xml`;
  CI: new `run_asset_engineer` checkbox in `ios-tests-parallel.yml`, cloned
  macOS job `asset-engineer-tests`, added to both `needs:` aggregation lists
  and both email SELECTION format strings.
- **`AppConstants`**: `MODULE_ASSET_ENGINEER` + 4 feature names.
- **`_EngDump.java`** (throwaway, keep until Issues remap reuses it): 3-phase
  live-DOM dumper that produced `target/engdump/*` evidence.

### Evidence-based locator contract (all from live v1.49 DOM)
- **Zero accessibilityIdentifiers** anywhere in the module — name==label==
  visible text.
- Engineering pickers use placeholder **"Select…" (U+2026)**; custom-attribute
  pickers below use ASCII "Select..." — never confuse them.
- `.textCase(.uppercase)` does **NOT** uppercase accessibility names: the DOM
  says "Library Matched" / "Busway Configuration" (original casing).
- Settings card folds state into its Button name:
  `"Load Latest Equipment Library, <state>"`; states observed live:
  `Not yet downloaded` → `Inserting 1,28,303 frames…` (locale-grouped, en_IN!)
  → counts summary (`… frames, … sensors, … trip units, … segments, … kVA
  entries, … cable / busway entries`) → `Last updated 8 min ago`.
- Asset Details is ONE flat sibling list (no card containers); label→control
  association is geometry (nearest Button below label). Transformer details =
  6 scroll pages, ~100 KB page source.
- **v1.49 OCP layout differs from the June app-source snapshot**: the match
  panel renders UNCONDITIONALLY on fuse (header "No possible matches",
  filled Add Custom, search `e.g. "QD" or "Formula"`, empty-state "No matches
  — refine the filters above or load the SKM library from Settings.") with
  Manufacturer as an in-panel filter; the standalone trip-Type picker and
  Fuse Amperage field do NOT render in the unset state.
- **System Voltage renders locked** (`—` + `lock.fill`, no editable control)
  on downstream assets (Node Bus, Fuse); transformer shows locked Primary +
  editable Secondary picker. "System Voltage" label itself is post-June
  (present in v1.48/49 binaries, absent in app-source).
- **Box block renders Mains Type ONLY** — "Phase Configuration" (present in
  the June source's `boxBlock`) does not render anywhere in the live v1.49
  DOM (verified across all dumps; driver-loop caught it as a test failure).
- **Mains Type is NOT selectable on Node Bus** (user-confirmed domain rule).
  The menu opens and lists the server enums (None / MCB / MLO / FDS / NFDS —
  live-verified), but picks do not apply there. TC_ENG_012 verifies the menu
  contents + clean dismiss on Node Bus.
- **Panel main auto-wiring flow discovered**: on a **Panelboard** (the class
  the user refers to as "panelcode"; found via the class-picker search),
  selecting Mains Type = MCB immediately opens a **"Create a Main Breaker?"**
  sheet — "Adds the main as a child of this panel and wires it up
  automatically (MCB)." — with Main Details (Name=MCB, Subtype, Pole Count
  1P/2P/3P), Nameplate Photos, Cancel / Create Main. TC_ENG_016 exercises
  the whole selection round-trip inside a cancelled Add-Asset draft: pick
  MCB → assert the Create-Main sheet → Cancel → discard (nothing persists).
- **SwiftUI Menu picks need W3C pointer press**: `element.click()` and
  `mobile: tap` on menu rows silently no-op on iOS 26.2/WDA;
  `AssetEngineerPage.pickOptionExact` uses a W3C touch-down→120ms→up
  sequence at the row center (validated live by the Create-Main sheet
  appearing). Same-label background elements are excluded via
  visible-filtering + last-in-document-order selection.
- Transformer Type = 2 segments (`Dry Type`/`Oil-Filled`); transformer
  Manufacturer menu lists ONLY library-backed manufacturers (observed:
  `Generic`, `SCHNEIDER/SQUARE D`).
- `Test Busway` is a **library-bound fixture** (Library Matched card:
  `BUS · 2500 A · @600V`, `Unlink`, busway config card with
  Length/Ampere Rating/Qty per Phase).

### App defect found (automation-blocking, reproduced 2×)
**Transformer engineering + manufacturer pick wedges WDA.** On v1.49, after
picking a manufacturer on `Transformer-1`'s engineering section (match panel
renders), the next `getPageSource`/snapshot call times out
(`java.util.concurrent.TimeoutException`) and the XCUITest session dies
("Error communicating with the remote browser"). Same family as the
giant-DOM wedge (OSHA Issue-Details / Location tree). Mitigation in tests:
TC_ENG_020 runs LAST (priority 601) and uses only breadth `existsNow` probes
after the pick — never page source. Candidate fix on the app side: same
scoped-accessibility cleanup as the other wedge screens.

### Test-data / state rules
- Library download is per-app-container and `NO_RESET=false` wipes it per
  driver session → the class pins `DriverManager.setNoReset(true)` and
  TC_ENG_003 (or the in-test `ensureEngineeringUnlocked` fallback) downloads
  when missing. Duration: ~25–60 s on a warm local sim, but **2–5 minutes in
  practice** (user-confirmed) — poll budget is 600 s and TC_ENG_003 carries an
  explicit `timeOut = 780_000` because the 360 s GlobalTestTimeout default
  would kill a 5-minute download mid-flight.
- Every test is draft-only: details always closed via Discard; the two
  discard-integrity tests (012, 023) assert that draft changes (including
  Unlink) do NOT persist. `Test Busway` must stay bound.

### Validation (local-first, per working agreement) — ALL 16 TESTS PASS
Driver loop on iPhone 17 Pro sim (iOS 26.2, Appium 3.1.2, app v1.49), one
test at a time, fixing each failure before proceeding:
- TC_ENG_001 ✅15s · 002 ✅12s · 003 ✅9s (cached fast-path; full download
  exercised live in the dump run) · 004 ✅10s
- TC_ENG_010 ✅23s · 011 ✅25s · 012 ✅2m35s · 013 ✅29s · 014 ✅28s ·
  015 ✅30s · 016 ✅1m51s (Panelboard MCB → Create-Main sheet)
- TC_ENG_020 ✅3m36s (no wedge — open-verify-dismiss) · 021 ✅46s ·
  022 ✅29s · 023 ✅45s (unlink-discard keeps binding)
- TC_ENG_030 ✅54s · 040 ✅24s

Framework fixes shaken out by the loop (all in AssetEngineerPage):
1. Alert content renders a beat after the alert element — poll ≤4s
   (getLoadDialogMessage) and poll close-flow for alert-OR-list
   (closeAssetDetails) instead of single 0-wait probes.
2. v1.49 renders no 'Phase Configuration' row on Node Bus (test now asserts
   its absence).
3. SwiftUI Menu rows need the W3C press; visible-only + last-in-document-
   order matching to dodge same-label background elements; the chip reader
   is visible-only too (it once read an asset cell behind the Add sheet).
4. Class picker is searched (found 'Panelboard'), not scrolled.

Web reference (acme.qa.egalvanic.ai) was NOT needed: iOS source + live DOM
answered everything; web parity notes already in the Swift comments.

### Post-landing logic audit (same day)
Re-read both files end-to-end hunting for logic errors the green runs could
have masked. Two found and fixed (+ re-validated live):
1. TC_ENG_040 stranded the app on the Add-Asset form: `dismissMenuOverlay`
   (coordinate tap) cannot dismiss a PUSHED class-picker screen, and
   `closeAssetDetails` then only closed the picker. Now: picker Cancel →
   form cancel → assert back-on-list. It had passed only because the next
   test's `navigateToAssetList` recovered.
2. `typeCustomField` didn't visibility-filter — it could type into a
   same-placeholder field BEHIND the sheet on other screens (worked on the
   fuse screen only by absence of collision). Now visible-first.
Soft spots accepted + documented: TC_ENG_016 doesn't assert its final
close's return (recovery proven live); swipe helpers are down-only (every
target screen opens at top); TC_ENG_004 run standalone on a fresh container
fails with guidance instead of skipping (per the no-skip-laundering rule).

### Coverage boundary (explicit)
Covered: the full Settings/download lifecycle, per-class engineering blocks
(box/fuse/transformer/bound-busway), unset match panel, menu enum contents,
Panelboard-MCB Create-Main flow, kVA input filter, bound-card lifecycle +
unlink-discard integrity, custom-sheet Save gating, Add-Asset mounts.
NOT covered, by category:
- Blocked by the WDA wedge defect: match RESULTS (cards/counts/truncation/
  Load more/search), binding via match-card tap, post-bind transformer
  config card. Testable after the app fix.
- Missing fixture: breaker OCP block + Trip Configuration card (frame→
  sensor→plug cascades, segments, i²t, 10-cap, GF toggle/picker), cable-class
  block, custom-sheet per-class sections, custom SAVE→CUSTOM ENTRY→Edit.
- Environment-gated: eng-lib flag OFF captions, offline caption, download-
  failed state, pre-download banner-shown assert, French locale, API-level
  encrypted-transport contract.

### Follow-ups
1. Issues remap against v1.49 DOM (unchanged by v1.49; still the biggest
   fail cluster).
2. Report the transformer/manufacturer WDA wedge to the app team (with
   `target/engdump` evidence + this changelog).
3. Add a Circuit Breaker fixture asset (or create-one-then-delete test) to
   cover Frame/Sensor/Plug cascades + Trip Configuration card end-to-end.
4. Consider a French-locale pass on the new text locators (app language key).
