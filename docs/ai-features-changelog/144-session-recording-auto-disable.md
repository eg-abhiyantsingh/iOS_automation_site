# 144 — Auto-disable Settings › Session Recording after every fresh install

**Date:** 2026-07-31
**Prompt:** Screenshot of Settings screen with Session Recording ON + "session
recording should be disabled to make app faster. you need to disable this when
you install the app again."

## Why

The app's Settings › Session Analytics › **Session Recording** toggle defaults
**ON** in every clean install, and the app's own subtitle says keeping it off
"improves performance and reduces battery usage". Under automation this
matters twice over: recording slows every screen the suite drives, and the
suite reinstalls the app constantly — `NO_RESET=false` is the framework
default, so **every new Appium session is a clean install that silently flips
the toggle back ON**. A run-once fix would decay mid-suite at the first
WDA-rebuild recovery; the fix has to track the *install* lifetime, not the
JVM lifetime.

## What was added

| Piece | File | Role |
|---|---|---|
| Fresh-install flag | `utils/DriverManager.java` | `freshInstallCheckPending` (AtomicBoolean) armed after **every** successful session creation; `isFreshInstallCheckPending()` peek + `consumeFreshInstallCheckPending()` one-shot consume. |
| Kill switch | `constants/AppConstants.java` | `DISABLE_SESSION_RECORDING` (default `true`, env/-D overridable) for CI rollback without a code change. Also new `MODULE_SETTINGS` report module. |
| Page object | `pages/SettingsPage.java` (new, 9th page object) | Settings tab nav + Session Recording switch, multi-strategy and French-label aware. |
| Post-login hook | `base/BaseTest.java` | `ensureSessionRecordingDisabledIfFreshInstall()` — consumes the flag at the first safe Dashboard moment; wired into `loginAndSelectSite()`, `loginAndSelectSiteTurbo()` (both wrapped: proven cores untouched, renamed `*Core()`), and `loginAndSelectRandomSiteFast()`. `settingsPage` field added + initialized at both page-init sites. |
| Canary tests | `tests/SettingsSessionRecording_Test.java` (new) | TC_SET_001 (toggle OFF after fresh-install login — the end-to-end proof), TC_SET_002 (idempotent on OFF), TC_SET_003 (deterministically exercises ON→OFF). Hard-asserted, no pass-anyway. Wired into `parallel/testng-auth.xml` + `testng.xml` (runs in the `authentication-only` CI job). |

## Design decisions

- **Flag armed at session creation, consumed at Dashboard.** Arming happens in
  `DriverManager.initDriver()` on every successful `new IOSDriver(...)` —
  covering job start *and* mid-suite rebuild recoveries. Consumption happens in
  BaseTest only when `assetPage.isDashboardDisplayedFast()` is true, so the
  detour can never hijack a test that resumed mid-app (the UNKNOWN-screen
  early-return path leaves the flag armed for the next safe login).
- **Never fails a test.** The pass is a performance optimization: on failure it
  logs loudly (`⚠️ Session Recording auto-disable failed`), increments a
  2-strikes counter (then disarms for the JVM), and always returns to the Site
  tab in `finally`. Correctness is enforced separately by the hard-asserted
  canary tests.
- **Fresh `SettingsPage` inside the hook** (not the cached field): after a
  WDA-rebuild recovery the cached page object can hold a dead driver; the
  constructor grabs the live one.
- **Never "the first switch".** The Settings screen has multiple switches
  (Session Recording, Network Mode, Equipment Library) — grabbing Network Mode
  by mistake would put the whole suite offline. Every strategy anchors on the
  Session Recording label: (1) switch named after the row, (2) switch
  vertically nearest the row title (≤60pt), (3) XPath document-order following
  the title, (4) one scroll + retry. Toggle verified by value readback
  ("1"→"0") with a coordinate-tap retry (v1.50 controls can swallow `click()`).
- **French-aware predicates** (`Réglages`/`Paramètres`/`Enregistrement`) since
  the app language can't be forced to English (custom plist key).

## Validation

1. `mvn -o -DskipTests test-compile` — clean.
2. `mvn -o -DsuiteXmlFile=testng-verify-selftest.xml test` — 34/34 green.
3. Live device: TC_SET_001 run locally on the free **iPhone 17** sim
   (iOS 26.2, UDID 3B120A1F — user was active on the booted 17 Pro Max, left
   untouched) against a genuine clean install — see result below.

## Live-run result

Two local runs on the free iPhone 17 sim (fresh install each — genuine
end-to-end reproduction of the reported scenario):

- **TC_SET_001** — install arrived with the toggle **ON** (default confirmed);
  hook fired right after Dashboard (`🎛️ New app session — ensuring Settings ›
  Session Recording is OFF`), toggled + value-readback-verified in **~3.6s**,
  test independently re-opened Settings and asserted "0". PASSED (1m46s incl.
  login).
- **TC_SET_002** — idempotent on an OFF switch. PASSED (**9s** — and no hook
  re-fire: the flag was already consumed this session, proving once-per-session
  behavior).
- **TC_SET_003** — armed the switch ON by hand, disable primitive brought it
  back OFF. PASSED (13s).

`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

## Rollback

`DISABLE_SESSION_RECORDING=false` (env or `-D`) disables the pass everywhere;
the canary tests then SKIP instead of failing.
