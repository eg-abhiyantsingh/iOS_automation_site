# 163 — App v1.63 into CI (+ MFA risk flagged) (2026-09-15)

Per request: put the newly shared build into the git zip "and everywhere that is latest app".

## What changed
- `apps/Z-Platform-QA.zip` — **v1.59 → v1.63** (source: `ios-app-qa-57`, dev-repo build #57,
  branch `release/qa`, 2026-09-15). Zipped in the CI layout (`Z Platform-QA.app/` at zip root),
  bundle `com.egalvanic.zplatform-QA`, simulator build (arm64, platform 7, minos 18.5).
- That single zip is what **every** workflow consumes (`ios-tests-parallel`, `-quick-verify`,
  `-smoke`, `ios-tests`, `rerun-failed-by-date`), so one swap updates them all.
- `AppConstants.APP_PATH` default (`~/Downloads/Z Platform-QA.app`) was **already v1.63** —
  local runs pick it up with no change.

Size note: the zip grew 44 MB → **82 MB** because v1.63 embeds the FLIR SDK
(`ThermalSDK.framework` + 8 ffmpeg/live666 dylibs). Under GitHub's 100 MB hard limit but past
the 50 MB warning; `.git` is now ~1.1 GB across 21 zip versions. If this keeps growing, move
`apps/` to Git LFS or fetch the build artifact at job start instead of committing it.

## Build pipeline is fixed
The SPM-resolve hang (builds #49/#50, diagnosed in the previous session) was fixed on
`chore/ZP-4095-ci-spm-resolve-hang` — #53/#54/#55 failed, #56 green, then **#57 (release/qa)**
and **#58 (release/dev)** both succeeded. v1.63 is the first QA build since 2026-08-20.

## What v1.63 contains
- **ZP-4107 / PR #545 (FLIR camera import hardening) is IN the QA build** — verified by binary
  probe: `flirCameraTransferStalled`, `flirCameraAlreadyOnWorkOrder`, `flirCameraNameTakenByVisual`,
  `flirCameraNotOnCamera`, `dictationInterrupted`, `recordingStoppedLabel`, `FLIRCameraService`,
  and the FLIR SDK frameworks are embedded. It no longer needs a dev build.
- 120 new localization keys vs v1.59.

## ⚠️ Risk flagged, NOT yet validated: v1.63 adds two-factor authentication
**92 of the 120 new strings are `auth.*`** — TOTP authenticator, Email OTP, org policy
enforcement (`auth.mfaRequiredByPolicy`, `auth.mfaRequiredByPolicyAnyFactor`), and a post-sign-in
setup prompt with **"Set up later"** (`auth.setUpLater`, `auth.mfaPromptSkipNote`: "You'll be
asked again next time you sign in").

This is the same shape as the v1.59 "Choose your experience" screen that broke every test at
login — but worse if the org enforces it, because automation cannot read an email or produce a
TOTP code without the shared secret. **Local login validation on v1.63 was interrupted and has
not been completed**, so do not assume CI is green on this build until it is:
1. Run one local test on the visible sim and watch the post-sign-in flow.
2. If a setup prompt appears, add a `dismissMfaSetupPromptIfPresent()` handler alongside
   `dismissChooseExperienceIfPresent()` / `acceptPolicyUpdateIfPresent()` (tap "Set up later"),
   wired at the same three layers (detectCurrentScreen + both login wrappers).
3. If MFA is *enforced* for the QA account, automation needs either the TOTP secret (generate
   codes in-suite) or an MFA exemption for the test user — raise with the backend team.
