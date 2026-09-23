# 178 — App v1.67 into CI (2026-09-23)

Per request: the updated `.app` (v1.67) goes into the CI zip "and everywhere".

## What changed
- `apps/Z-Platform-QA.zip` — **v1.63 → v1.67** (source: `~/Downloads/Z Platform-QA.app`, built
  2026-09-23 08:15, bundle `com.egalvanic.zplatform-QA`, simulator arm64, MinimumOSVersion 18.5,
  DTXcode 1640 — identical toolchain/minOS to 1.63). Zipped with
  `ditto -c -k --norsrc --keepParent` so `Z Platform-QA.app/` is at the zip root (the layout every
  `Unzip App` step expects; the hyphen→space rename fallback is untouched).
- Size: 88,005,971 bytes (**83.9 MB**, was 83.0 MB) — under GitHub's 100 MB hard limit, still past
  the 50 MB warning. Frameworks list is identical to 1.63 (ThermalSDK + ffmpeg/live666 + DevRev);
  the growth is the debug dylib (137 → 141 MB uncompressed).
- That single zip is what **every** workflow consumes — `ios-tests-parallel` (28 refs),
  `ios-tests-quick-verify` (7), `ios-tests-smoke`, `ios-tests`, `rerun-failed-by-date` — so the one
  swap updates all of CI. Nothing else in the repo pins a version: no `1.63` in workflows, Java,
  scripts, or properties (only historical changelogs, left as-is).
- `AppConstants.APP_PATH` default (`~/Downloads/Z Platform-QA.app`) and
  `config/config*.properties` `app.path` point at the very bundle the zip was built from, so local
  runs are on 1.67 with no change.

### Version label contract (fixes a silent gap from the 1.63 bump)
`ios-tests-parallel.yml` / `rerun-failed-by-date.yml` derive "App version under test" from the
subject of the last commit touching `apps/` with `sed 's/.*[Tt]o v\([0-9][0-9.]*\).*/\1/p'`, and
`generate_bug_report.py::detect_app_version` prefers `Z Platform… to v<ver>`. The 1.63 subject
("…v1.63 into CI…") matched **neither**, so every report since 2026-09-15 printed
"not recorded for this run". This bump is committed as
`chore(app): update Z Platform-QA to v1.67 for CI` — **keep the "to v<ver>" form on every future
app commit** (recorded in CLAUDE.md).

## What v1.67 contains (vs 1.63)
`en.lproj/Localizable.strings`: 445 → **511 keys, +66, 0 removed** (`fr.lproj` updated too).

| Group | Keys | What it is |
|---|---|---|
| `auth.*` | 51 | **Passkeys** (`Sign in with a passkey`, `Add passkey`, `Use passkey from another device`, org gate `Passkeys aren't available for your organization yet.`), **Google sign-in** (`Continue with Google`, `Connect your Google account`), **email code sign-in** (`Email me a code`, `Check your email`), returning-user shell (`Welcome back, %@`, `Not %@? Sign in as someone else`), method switching (`Use my password`, `or use your password`, `or continue with`, `Try another way`, `Use another method`, `Back to faster options`), authenticator removal + MFA-required reasons (by organization / provider / user). |
| `issues.suggestion*` | 11 | New **"Add" suggestion to the issue-class library** from the Issue form (`Saves to this issue class for %@. Does not change the issue you are filing.`, `Added to the library.`, offline + already-added + needs-title variants). |
| `common.*` | 3 | Camera-permission alert: `Camera access required` / `…Enable it in Settings → Privacy → Camera → Egalvanic PZ.` / `Open Settings`. Relevant to CAM-CRASH-01 (sim camera SIGABRT) — the crash path may now be preceded by an alert; not verified here. |
| `siteWalks.dictationMicBusy` | 1 | `Another app is using the microphone…` dictation guard. |

## v1.67 changed the sign-in flow — first canary FAILED, fixed (this is the real content of this bump)
First `mvn test` on the visible sim (TC_SET_001, clean install of v1.67) **failed at login in 55s**:
`WelcomePage.clickContinue → waitForClickable(Continue)` TimeoutException. A curl-driven Appium
probe (page source at every step, `scratchpad/smoke/dom_*.xml`) established the new anatomy:

| Step | v1.63 | v1.67 (live DOM 2026-09-23) |
|---|---|---|
| Company code | `Welcome` + TextField + `Continue` | unchanged (code is even pre-filled `acme` on relaunch) |
| After Continue | Email TextField + Password SecureTextField + `Sign In` + Terms checkbox | **`EG-ACME` / `Sign in to continue`**, Email TextField, `Sign in with a passkey` (disabled until email), `or continue with`, `Continue with Google`, `Email me a code` (disabled until email), **`Use my password`**, `Change Company`. **No SecureTextField, no Sign In.** |
| After Continue, **returning user** (a previous sign-in completed on this device — the remembered user survives a clean reinstall via the keychain, so LOCAL runs see this from the 2nd run on; fresh CI simulators see the email-entry variant) | — | composite StaticText `AA, Welcome back, abhiyant admin, a•••@egalvanic.com`, **no TextField**, `Sign in with a passkey` (enabled, primary), `Continue with Google`, `Email me a code`, **`Use my password`**, `Not abhiyant admin? Sign in as someone else` (→ email-entry variant) |
| After `Use my password` (both variants) | — | same screen + Email TextField (**prefilled with the remembered address** in the returning-user case, editable) + `Password` SecureTextField, `eye.slash` Hide, `Sign In` (disabled until password), **implicit consent** StaticText "By signing in, you agree to our Terms & Conditions and Privacy Policy" with two Links, `Forgot your password?`, `Back to faster options` |
| After Sign In | MFA prompt (v1.63) | same `Set Up Two-Factor Authentication` prompt with `Set up later` |

Two mechanisms killed the login:
1. **Welcome retry race** — `submitCompanyCode` saw `Welcome` still up 3s after the first press
   (virgin-install validation is slower on the bigger build), re-pressed, and `waitForClickable`
   then waited 10s for a `Continue` that had already left the screen → exception.
2. **Password form is now behind a button** — `waitForPageReady` / `enterEmail` / `loginTurbo`
   all wait ≤25s for a SecureTextField or `Sign In` that does not exist until `Use my password`
   is pressed; `detectCurrentScreen` had no class for the chooser.
3. Bonus, live-verified: **`element.click()` is a silent no-op on this build's SwiftUI buttons** —
   `Set up later`.click() left the MFA prompt unchanged across three snapshots. Coordinate press
   first, then VERIFY, is now the rule for every post-sign-in dismisser.

### Fixes (all locator/flow work in OUR code — the app is not at fault)
- `LoginPage` (+~230 lines): `CHOOSER_MARKER_BY`, `PASSWORD_FORM_BY`, `IMPLICIT_CONSENT_BY`,
  6-strategy `USE_MY_PASSWORD_STRATEGIES`; `isPasswordlessChooserDisplayed()`,
  `isPasswordFormDisplayed()`, `isConsentImplicit()`, `emailFieldHolds()`,
  **`revealPasswordFormIfNeeded(email|null)`** (types the email on the chooser, presses
  `Use my password` coordinate → click → W3C tap, each verified by a 4s *presence* poll —
  SecureTextField reports visible=false on iOS 18.5 runners). Hooked into `waitForPageReady`
  (chooser counts as ready), `loginTurbo` (reveal + skip `enterEmail` when the value stuck),
  `enterEmail`, `enterPassword`, `acceptTermsIfPresent` (implicit-consent early exit — no more
  3s checkbox cascade), `tapSignIn`.
- `WelcomePage`: retry loop breaks when `Continue` is already gone / vanishes mid-retry;
  new wait-0 `isCompanyCodeScreenNow()`.
- `BaseTest.performLogin`: company code only when the company-code screen is up (the
  LOGIN_PAGE branch has a TextField too — typing the code into the EMAIL field was a guaranteed
  timeout); `detectCurrentScreen` → `LOGIN_PAGE` for the chooser.
- `BasePage.dismissMfaSetupPromptIfPresent`: coordinate press first, `isElementGone(title, 3)`
  verification, click()/W3C-tap fallbacks, honest `false` after 3 misses.
- `SiteSelectionPage.dismissChooseExperienceIfPresent`: verifies the title is gone after
  Continue, coordinate press + re-verify otherwise.
- `AppConstants.HANDLE_PASSWORD_CHOOSER` (default true) — kill switch for the reveal.
- `Security_EdgeCase_Test.reachedLoginScreen()` accepts the chooser (it IS the login page).
- Tests: `TC_AUTH_TERMS_01-08` were already `enabled=false` (2026-04-30, checkbox removed by
  product) — nothing to reshape; TC16/17/28 tolerate the chooser as written; TC21's
  `enterPassword` self-heals. Google / passkey / email-code sign-in paths are **not** automated
  (passkeys are org-gated: `auth.passkeyNotOfferedHere`).

## Local validation on the visible sim (iPhone 17 Pro Max, iOS 26.2, clean install of v1.67)
| Run | Result |
|---|---|
| 1. `SettingsSessionRecording_Test#TC_SET_001` on the untouched framework | **FAILED in 55s** — `WelcomePage.clickContinue → waitForClickable(Continue)` TimeoutException (screenshot: the v1.67 chooser) |
| 2. Same test after the fixes above | **PASSED in 2m15s** — log shows `🔑 v1.67 passwordless-first sign-in chooser detected` → `✓ password form revealed (attempt 1)` (coordinate press) → `✅ Sign In pressed` → `Set Up Two-Factor Authentication … ✓ MFA setup skipped` → `Choose your experience … Field Technician` → `✅ Dashboard ready` → Session Recording confirmed OFF → all 3 assertions green |
| 3. `AuthenticationTest#TC16 + TC21` | TC21 **PASSED (23s)** — `enterPassword` self-healed through the chooser and the eye toggle worked. TC16 **FAILED (1m10s)** on the *returning-user* variant (email/password/Sign In all "visible: false" — no TextField on that chooser) → test now reveals the form first → **PASSED (29s)**, all three elements visible |
| 5. Canary run 3 (email-entry variant again — the "someone else" probe had cleared the remembered user) | **FAILED**: `Invalid email or password` — the form showed `…@egalvanic.comg`. The first coordinate press on `Use my password` at (220,770) landed on the still-open keyboard's **`g` key** (the `mobile: hideKeyboard` I used had not dismissed it), the element-click retry then revealed the form, and `emailFieldHolds` used `contains()` so the corrupted address passed. Fix: full 4-strategy `dismissKeyboard()` + `isElementGone(keyboard)` before pressing, **never coordinate-tap while a keyboard is present** (element click instead), `emailFieldHolds` = exact match so a wrong value is retyped by `enterEmail`. Note: `dismissKeyboard()` strategy 5 taps (100,100), which on these screens is the `Change Company` button — pre-existing, strategies 0-4 normally win. |
| 6. Canary run 4 after the keyboard fix | **PASSED (2m15s)** — email typed on the chooser, keyboard dismissed and verified, coordinate press revealed the form on attempt 1, `loginTurbo` 12.7s, MFA skipped, experience chosen, site selected, dashboard, Session Recording OFF, all assertions green. Four full-login runs total: FAIL (untouched framework) → PASS → FAIL (keyboard `g`) → PASS. |
| 4. DOM probe of the returning-user variant (curl-driven, `dom_12..15_*.xml`) | `Use my password` → form with Email **prefilled** `abhiyant.singh+admin@egalvanic.com` + SecureTextField + Sign In(disabled); `Back to faster options` → chooser; `Sign in as someone else` → email-entry chooser. Handler updated: no chooser typing when there is no TextField; the form value is reconciled by `emailFieldHolds`/`enterEmail` |

Compile: `mvn -o -DskipTests test-compile` exit 0 with fresh class timestamps (no stale IDE classes).


## main did not compile since 2026-09-15 (found while proving the staged tree builds alone)
Applying only the staged patch to a clean `HEAD` worktree failed to compile — in files this bump
never touched: `BuildingPage` (`dismissKeyboard()` called but never defined; second
`createButton()` duplicate) and `IssuePage` (missing `import org.openqa.selenium.By`). Commits
`51de06a` + `68a0a1b` (2026-09-15) shipped them; `static-checks` has been **red on every run
since**, and the 09-15 `ios-tests-parallel` runs failed for the same reason — CI could not have
run v1.63, let alone v1.67. The fixes existed uncommitted in the working tree (changelog 177's
session); only the compile-fix hunks (`dismissKeyboard()`, `createButton()`→`v163CreateButton()`
×3, the `By` import) are committed here as a separate `fix(compile)` commit — the rest of that
session's WIP (new ZP392x page objects/tests, IssuePage long-press rework, BasePage.longPressAt)
stays uncommitted for its owner. Verified: staged tree + compile fixes applied to a clean `HEAD`
worktree → `mvn -o -DskipTests test-compile` exit 0.

## Follow-ups
- Changelog 177 pinned the ZP-3927/3928 suites to copy extracted from **1.63** and said
  "re-verification on 1.66+ is one `mvn test` once a build lands in `apps/`" — that build is now
  here; run `ZP3927_*` / `ZP3928_*` one class at a time.
- Watch the first `ios-tests-parallel` run on 1.67 for the `authentication-only` job first; if it is
  red at Sign In, the fix is a new `dismiss…IfPresent()` / "Use my password" handler at the same
  three layers as `dismissMfaSetupPromptIfPresent()`.
