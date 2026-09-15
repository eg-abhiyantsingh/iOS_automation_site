# 164 — v1.63 MFA setup prompt broke every login; handler added (2026-09-15)

Follow-up to changelog 163 (app v1.63 into CI). The risk flagged there is REAL and is now fixed.

## What broke
v1.63 adds two-factor authentication (92 of its 120 new strings are `auth.*`). After Sign In the
app raises a blocking **"Set Up Two-Factor Authentication"** screen — Authenticator app / Email OTP
cards, Continue, and **"Set up later"**. Two failure modes, both observed live on the sim:

1. `detectCurrentScreen` misreads it as **WELCOME_PAGE**, so the login core types a company code
   into a screen with no such field.
2. The Authenticator card's composite label — `Authenticator app, Recommended, Google
   Authenticator, Microsoft Authenticator, 1Password or Authy.` — carries **>= 2 commas**, so the
   site-row predicate MATCHES it and `selectFirstSite*` taps a radio card instead of a site.
   Dashboard never renders; every test dies at login.

This is the **same trap as the v1.59 "Choose your experience" chooser**, one screen earlier.

## Fix
`BasePage.dismissMfaSetupPromptIfPresent()` (in BasePage so LoginPage **and** SiteSelectionPage
inherit it): wait-0 probe on the unambiguous title `Set Up Two-Factor Authentication`, then tap
**"Set up later"** (element click → coordinate-tap fallback). Kill switch
`SKIP_MFA_SETUP_PROMPT=false`.

**"Set up later", never Continue** — enrolling a factor would demand a TOTP/email code at every
later sign-in and hard-block the suite. The prompt reappears each sign-in by design, so the
handler runs on every login; it is a ~ms no-op on older builds.

Wired at **four** layers (three were not enough — the first run still tapped the MFA card as a
site because the prompt appears *after* Sign In, between detection and site selection):
- `BaseTest.detectCurrentScreen()` — before any branch decision
- `loginAndSelectSite()` and `loginAndSelectSiteTurbo()` wrappers
- all three `SiteSelectionPage` entry points (`selectFirstSite`, `selectFirstSiteFast`,
  `selectSiteByName`) — immediately before the v1.59 chooser check

**If the org ever enforces MFA** (`auth.mfaRequiredByPolicy`), "Set up later" disappears and the
handler logs a loud, explicit message instead of dying 120 s later as "site never loaded".
Automation would then need the TOTP secret or an MFA exemption for the QA account.

## Verified on the visible sim (iPhone 17 Pro Max, clean install, v1.63)
```
🔐 v1.63 'Set Up Two-Factor Authentication' detected — tapping 'Set up later'
   ✓ MFA setup skipped — continuing to the site flow
🧭 v1.59 'Choose your experience' detected — selecting Field Technician + Continue
Selecting first site: Wild Goose Brewery, Ettars, erer, IN, 12345, United States   ← a REAL site
```
- `TC_ES_020` **PASSED** (2m40s) · `TC_ES_010` **PASSED** (1m15s)
- Verifier self-tests **39 run / 0 failures**; compile exit 0

## Bonus: the engineering_status backend gate has OPENED
`TC_ES_010` no longer skips — it reported **"Field IS live — snapshot of 315 node values"** and the
full device edit → re-sync round-trip passed with zero drift. Backend #1057 is on QA, so the
four-state `engineering_status` work (iOS #482) is now genuinely verifiable end-to-end.
