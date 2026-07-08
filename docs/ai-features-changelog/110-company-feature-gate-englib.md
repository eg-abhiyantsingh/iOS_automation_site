# 110 — CompanyFeatureGate: eng-lib blocker now auto-skips (was: 105 misleading FAILs)

**Date:** 2026-07-08
**Prompt:** CI console showed TC_ENG_001 PASS then TC_ENG_002/003/004 FAIL ("'Load Device
Library?' alert should appear after tapping the card") — "failing, fix this".

## Diagnosis (verified live before touching code)

- `GET /auth/v2/me` → `company_features` on acme QA **still lacks `eng-lib`**
  (checked 2026-07-08; the changelog-109 blocker from 2026-07-07 persists).
- App source confirms the exact symptom shape (`SettingsView.swift:428-505`):
  with the flag off the card renders at 50% opacity **but keeps its normal
  subtitle** — the subtitle if/else chain never consults the flag, so
  TC_ENG_001 legitimately reads "Not yet downloaded" and passes. The tap is a
  pure no-op (`guard engLibEnabled else { return }` **and** `.disabled(...)`),
  so no alert can ever appear → TC_ENG_002/003/004 and everything downstream
  of the TC_ENG_003 download gate fails. A separate caption
  `"Engineering Library isn't enabled for your company. Contact your admin to enable."`
  (AppStrings `engineering.libraryDisabled`) is the only on-screen evidence.
- Verdict: **environmental, not a script bug**. Nothing to fix in test logic;
  what needed fixing is that the suite reported this as 105 FAILs (each paying
  ~20s + a driver rebuild) instead of one loud, self-explaining skip reason.

## What was built

1. **`com.egalvanic.api.CompanyFeatureGate`** (new, src/main) — cached
   per-JVM check of platform-managed company flags via TestDataApi
   login + `GET /auth/v2/me`. Three-verdict semantics, deliberately
   **fail-open**: `DISABLED` only on a positively-confirmed absent flag
   (the only verdict allowed to skip tests); any login/network/parse problem
   is `INDETERMINATE` and the tests run. Exact quoted matching ("eng-lib"
   can never match "eng-library-v2"). Bypass: `-DFEATURE_GATE_OFF=true`.
2. **`BaseTest.requiredCompanyFeature()`** hook (returns null by default) +
   gate in `testSetup` **before `initDriver()`**, right after the RunHealth
   fast-skip block. Placement is load-bearing: testSetup is `alwaysRun=true`,
   so a plain `@BeforeClass` SkipException would still pay a full driver
   build per test (recon: run-28246433532 lesson). Gated skip costs ~0s and
   nulls the driver first so teardown makes zero Appium calls. Skip message
   is greppable: `BLOCKED (environment): company feature 'eng-lib' is DISABLED…`.
3. **All 7 AssetEngineer\* classes** declare `requiredCompanyFeature() → "eng-lib"`.
   Tests resume automatically the moment the platform team restores the flag —
   no code change needed then.
4. **UI-level backstop** in `AssetEngineerPage.tapLibraryCard()`: if the
   "isn't enabled" caption is on screen (locator matches
   `BEGINSWITH 'Engineering Library isn'` — stops before the apostrophe,
   which would break the NSPredicate literal), throw SkipException instead of
   letting the test fail on "alert never appeared". Covers API-account /
   app-account mismatch where the API gate fails open.
5. **Latent crash fixed** (found by the live smoke run, not by review): all 7
   classes' `@BeforeMethod(alwaysRun=true)` constructed `AssetEngineerPage`
   unconditionally; with no driver (feature gate OR RunHealth fast-skip)
   `BasePage`'s constructor throws `IllegalStateException` → config-failure
   noise. Now guarded with `if (!DriverManager.isDriverActive()) return;`.
   This also cleans up breaker-tripped cascade behavior that predates this change.
6. **Cosmetic:** teardown duration printed epoch garbage (`29725093m 34s`)
   when setup skipped pre-driver (testStartTime never set) — now prints `0ms`.
7. **`CompanyFeatureGateSelfTest`** (8 driver-free tests) wired into
   `testng-verify-selftest.xml`: present/absent/empty-array/missing-key/
   malformed/null/substring-collision/bypass, including the verbatim
   2026-07-08 live payload.

## Validation (all green)

- `mvn -o -DskipTests test-compile` — clean.
- Self-test suite: **34/34 GREEN**; RED-proven by injecting a substring-match
  defect into the parser — exactly `substringFlagNames_mustNotFalseMatch`
  failed, then reverted to GREEN.
- Live end-to-end smoke (real QA API, no simulator needed): TC_ENG_002 →
  `🚫 CompanyFeatureGate: 'eng-lib' is DISABLED … (22 flags present)` →
  SKIPPED in 0ms, zero driver init, zero failures, BUILD SUCCESS.

## Notes / follow-ups

- **The flag itself still needs the eGalvanic platform team** — the gate makes
  the suite tell the truth cheaply; it cannot re-enable eng-lib.
- The CI run pasted in the prompt (iPhone 16 Pro, 9:57 AM) predates this fix
  and will keep failing through the module; its asset-engineer results should
  be read as "environmental blocker", per this changelog.
- `NodeCoreAttributesSection` / `EditNodeDetailView` / `AddAssetView` also gate
  on eng-lib (recon list in this changelog's session) — some Asset_* Engineering
  assertions may newly fail in full runs while the flag is off. Those classes are
  NOT flag-gated (only parts depend on it); triage against this changelog.
- Reusable: any future flag-gated module declares its flag in one line
  (e.g. `"emp"` for ZP-323 shortcuts if ever needed).
- Commit note: the touched files also carried yesterday's uncommitted
  asset-engineer WIP (new matching tests, e.g. TC_ENG_115) — compiled and
  included as-is.
