# 166 — FLIR app v1.0.4(5) QA review: ZP-3993 Routes + ZP-4187 2FA (2026-09-16)

**Prompt:** "Egalvanic_v1.0.4(5)_qa_release_16-09-2026.apk check route ticket now … check in depth and
create a artifact common for 2FA and route ticket … always check API everything … don't miss any bugs".

**Scope:** manual/adb-driven functional review of the FLIR Camera Integration Android app
(`com.egalvanic.pz.ace.qa`, build 16-09-2026, byte-identical to the APK already installed on the
`quacky_test` API 35 emulator) against Jira ZP-3993 (Routes) and ZP-4187 (2FA), with every UI
action cross-checked on the QA backend (`https://eg-pz.qa.egalvanic.ai/api`). No framework code
changed; this repo only gains this changelog. Deliverable = one shared artifact page (link in the
session summary) covering both tickets.

## Environment / harness
- Emulator `quacky_test` (Android 15, arm64, 1080×2400), no FLIR camera → thermal capture, camera
  connectivity, serial binding and hardware keys not testable.
- Driven with `adb input`, `uiautomator dump`, `screencap`; WorkManager state via
  `androidx.work.diagnostics.REQUEST_DIAGNOSTICS`; backend via python helper (login v2 → after MFA
  enrolment v4 login + `respond-to-challenge`).
- Test site "Android Site 2" (`aadcee4c-…`), dev's route "Route 1 - Rajat" (12 stops) untouched.

## Results (headline)
**ZP-3993 Routes — not ready.** 1 HIGH, 5 MEDIUM, 4 LOW, 1 question.
- R-1 HIGH: after Settings › Online mode Offline→Connected the upload queue never drains
  (SyncQueueWorker not re-enqueued); sign-out/in, background, new writes don't help; only a
  force-stop drains it. Reproduced twice (18 items, 2 items). Blocks "Refresh data" too.
- R-2 MEDIUM: "Next Stop" from an unfinished stop navigates to the next room and immediately shows
  "Wrong stop for the Route · Go to: <previous room>"; no state change on the run.
- R-3 MEDIUM: WO Details "Inspection route" counts IR captures (0/14) and ignores stop completion,
  even for a FINISHED run.
- R-4 MEDIUM: assets already worked this session show 1/1 but have no "Mark as Processed" and the
  stop never auto-completes; finished run has `assets: []`.
- R-5 MEDIUM: server `route_run.current_position` never advances while ACTIVE; `completed_by` null
  for auto-completed stops.
- R-6 MEDIUM: Stop Following writes ABANDONED (UI says paused; enum has PAUSED); deleting a
  followed route leaves its run ACTIVE.
- LOW: "Continue · Stop 0 of 12" (completed count as stop number), header always "Facility Route",
  "1 issues"/"change(s)" plurals, primary buttons without accessible names.
- Verified OK end-to-end (UI + API): Follow → Link Route Assets (14 node_session links), Mark as
  Processed (processed_at/by), Mark Visit Complete, pause/resume same run, Switch Route?, draft
  recording after 2 rooms → Save Route (default name, empty-name block, 120 chars) → RECORDED route
  with ordered stops, Rename, Add Room (Before/After → existing/new) → stop_count 3, Finish →
  FINISHED + finished_at, Follow Again → new run, Delete (shared warning) → removed, Link Asset
  picker, offline queue survives sign-out/in with original timestamps.

**ZP-4187 2FA — authenticator path OK, email path not.** 3 MEDIUM, 2 LOW.
- A-1 MEDIUM: "Enable Email Codes" enrols with one tap, no verification code, no confirmation;
  login then requires EMAIL_OTP and legacy v2/v3 logins get 426 APP_UPDATE_REQUIRED.
- A-2 MEDIUM: contradictory policy text ("Enabled…" + "organization no longer accepts email codes";
  Settings path says "Not available · organization requires an authenticator app") while the API
  says `organization_email_otp_allowed: true`. LaunchDarkly "identify timed out — using cached
  flags" at start-up is the likely source.
- A-3 MEDIUM/process: authenticator cannot be turned off in-app ("ask an administrator") although
  API `can_remove: true`. **The shared QA admin account now has TOTP enrolled** (needed to test
  sign-in challenge) → iOS suite / web / API v2 logins will fail (426) until an admin resets MFA or
  clients adopt v4 login + `respond-to-challenge {session, challenge_name, code}`. Secret handed to
  the QA owner in the session summary (not in the artifact).
- Verified OK: invalid credentials copy, setup prompt, QR + manual key + auto-submit, Two-Step
  Verification screen, wrong-code error, correct code → FLIR Select-a-Site, "no backup codes" copy
  (expected — AWS feature not enabled), Replace Authenticator dialog, offline → "A connection is
  required to set up two-factor authentication.", Turn Off email codes reflected in backend.
- Not covered: Email-OTP sign-in completion / resend / lockout (mailbox code; lockout would block the
  shared account), invitation deep-link activation, TalkBack/light theme, main-app regression.

## Notes for future sessions
- Backend contract for routes/2FA and the emulator harness traps are recorded in memory
  `flir-ace-app-testing.md`.
- The FLIR app is a separate codebase (Kotlin/Compose); nothing here touches the iOS suite. If the
  iOS suite's login account keeps MFA, `AuthenticationTest`/`loginAndSelectSite` will need a TOTP
  step (or a dedicated non-MFA automation user) — decision pending with the user.
