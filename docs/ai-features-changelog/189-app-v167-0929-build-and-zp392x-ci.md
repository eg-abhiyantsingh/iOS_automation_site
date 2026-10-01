# 189 — New app build (v1.67, 2026-09-29) into CI + ZP-3927/3928 suites committed (2026-10-01)

**Prompts:** *"check i have update app with latest app update our test case etc"* → *"install iphone 17
in this device in simulator so that you can test"* → *"ZP-3928 / ZP-3927 test this both ticket and update
the status ready to release if this is done"*

## 1. What the new build is

`~/Downloads/Z Platform-QA.app` was replaced on 2026-10-01; its files are dated **2026-09-29 08:12**.
The zip CI had was built **2026-09-23 08:15**. **Both Info.plists say 1.67 / 1.0** (byte-identical
plist), so the version label alone cannot tell them apart. Only these files changed: the debug dylib
(141.8 → 143.9 MB), the stub executable, `Assets.car` (same 21 asset names), `Sentry.framework`,
`en/fr Localizable.strings`. The web bundles (`Flow`, `FormApp`, `Agent`) and all other frameworks
are identical (md5).

### How the diff was made (repeatable)
1. `Localizable.strings` → JSON via `plutil`, then compare the keys.
2. `strings -a` over both `Z Platform-QA.debug.dylib` files, then a UI-copy filter (on-screen
   text lives in the dylib, not the strings file — see memory `sim-photo-seeding-and-ui-strings`).
3. Compare Swift type names (reflection metadata) to find new SwiftUI views.
4. Compare localisation-style keys (`sessions.*`, `assets.*`, …) found in the dylib.

### What is new in the 09-29 build
| Area | Evidence (dylib / strings) | Test impact |
|---|---|---|
| Session › room assets: **service selector** | new types `ServiceChipBar`, `ServiceSidebar`, `ServiceSelector`, `ServiceProgress(Header)`, `CompletionCellLayout`, `CompletionColumnHeader`, `NodeRollUpBadge`; keys `sessions.serviceChip*` (AF · COM · IR · Forms · SCH · CHK), `sessions.serviceNoneHint` "No service, %d assets", "%1$d of %2$d complete", cell states Checked / Not checked / Partially checked | Session-room tests that tap "first button" or count rows may now see chips → re-verify SiteVisit / WO-session / arc-flash session tests |
| Asset details | `LocationPhotosSheet` / `LocationPhotosThumbnail`, `EditLocationMenu`; keys `assets.locationPhotos`, `assets.editLocation`, `assets.noClassAssigned` | new surfaces; location-edit tests to re-verify |
| Engineering card | `ManualRelaySettingsView`; "Relay Settings", "Pickup (51)", "Time Dial (51)", "Instantaneous (50)", "Ground Fault Pickup (51G / 50G)", "Ground Fault Delay (51G / 50G)" | new feature (no test yet) |
| Arc-flash readiness | rule vars `class.hide_mains_type_for_bus`, `class.hide_manufacturer_and_type_for_bus` (in `ArcFlashReadinessEvaluator`) | bus/busway readiness counts may shift |
| Settings › passkeys | 6 new `auth.*` keys: Remove passkey / Passkey removed / last-passkey note | not automated (passkeys org-gated) |
| FLIR import | roll states "Loading photos from the camera", "Updating the photo list", "Reload", "Couldn't reach the camera…"; dedupe toast **"…already added to this work order."** (was "…to this asset.") | `FlirImportPage.NEWEST_ALREADY_ADDED` updated (only drift found in ZP page objects) |
| Debug | `FLIRDiagnosticsDebugView`, `DebouncedAssetSearch`, `IRPendingVisualExtraction` | asset search is now debounced → search tests must wait for results, never read immediately |

## 2. Commits (local; push pending — see §4)
- `30dc8a1 chore(app): update Z Platform-QA to v1.67 (2026-09-29 QA build) for CI` —
  `apps/Z-Platform-QA.zip` rebuilt with `ditto -c -k --norsrc --keepParent` (88,576,472 bytes,
  `Z Platform-QA.app/` at the root). Subject keeps the `… to v<ver> …` form the workflows and
  `generate_bug_report.detect_app_version` parse.
- `622cdda test(zp3927/zp3928): complete ZP-3927/3928 automation suites + CI job` — commits the
  changelog-177 work that was still uncommitted: 4 new classes (FlirImport, PhotoImport,
  EgFormsMatrix, PhotoCategories), 6 reworked classes, `FlirImportPage` / `SessionIssuesPage`,
  shared helpers (only adds new code, plus the improved `AssetPage.scrollToIssuesSection`),
  suite `parallel/testng-zp3927-3928.xml`, workflow input `run_zp3927_3928` + job
  `zp3927-3928-tests`.
- Gates before committing: `mvn -o -q clean test-compile` exit 0 on the working tree **and** on
  a clean worktree with HEAD + only the staged patch applied; verifier self-tests 39/39.

## 3. Jira AC → test map (what "done" means for these two QA tickets)
| Ticket | Acceptance criterion | Class |
|---|---|---|
| ZP-3927 | Unlink Issue on long-press | `ZP3927_UnlinkIssue_Test` (8) |
| ZP-3927 | New Location View · independent Building/Floor/Room | `ZP3927_Locations_Test` (10) |
| ZP-3927 | SD-card import · 30-min window · already-uploaded deselected | `ZP3927_FlirImport_Test` (7; 2 device-only verdicts) |
| ZP-3927 | Photo Import | `ZP3927_PhotoImport_Test` (4) |
| ZP-3928 | Issues filter with / without photos | `ZP3928_IssuePhotoFilter_Test` (6) |
| ZP-3928 | Link issue to WO from Issue Editing | `ZP3928_IssueWorkOrderLink_Test` (3) |
| ZP-3928 | Asset issues default All | `ZP3928_AssetIssuesDefault_Test` (3) |
| ZP-3928 | EG Forms on every WO type | `ZP3928_EgFormsMatrix_Test` |
| ZP-3928 | Copy Data To search · IR cache busting | `ZP3928_Misc_Test` (3) |
| ZP-3928 | Number pad decimal | `ZP3928_NumberPadDecimal_Test` (3) |
| ZP-3928 | Photo categories Other / Misc | `ZP3928_PhotoCategories_Test` (3) |
| ZP-3928 | Volume buttons take photos | not feasible on simulator (ticket says so) |

**Jira status was NOT changed.** The suites have not run on the new build yet (§4). Changelog 177's
live table stopped at TC_UL_01-03.

## 4. Blockers (need the user)
1. **No local simulator: Xcode is gone from this Mac.** Only CommandLineTools 16.4 is
   installed (`xcode-select -p` → `/Library/Developer/CommandLineTools`; `xcrun simctl` is missing).
   The iOS 26.2 runtime (23C54) and every simulator device (incl. `QA-Automation-17ProMax`
   6A893398…, iPhone 17 Pro Max E042B830…) are still on disk; only `Xcode.app` is missing (the
   App Store receipt says Xcode 26.3 was installed). The App Store now serves **Xcode 27.0, minimum
   macOS 26.6**, and this Mac runs **26.5.1** → `mas install` cannot be used. Plan: download
   **Xcode 26.3** (the version that was proven here) from developer.apple.com — this needs the user's
   Apple ID sign-in in Chrome; I may not type their password. Free disk: **13.4 GB** (xip ≈3 GB +
   expanded app), tight but workable. `mas` 7.0.0 was installed via Homebrew for the App Store check.
2. **`git push origin main` was blocked** by the Claude Code auto-mode permission classifier
   (the remote also has 2 bot commits from `failed-suite-collector`, so a `git pull --rebase` is
   needed first). Until it is pushed, CI cannot run the ZP suite on the new build.

## 5. Next steps (once unblocked)
- Push → dispatch `ios-tests-parallel.yml` with `run_zp3927_3928=true`, `run_auth_site=true`,
  `send_email=false`.
- Local: install Xcode 26.3 → boot `QA-Automation-17ProMax` → run the ZP classes one at a time
  (memory `feedback_local_test_driver_loop`), fix drift, then move both tickets to
  **Ready to Release** with an evidence comment.
- Known red on the previous 1.67 CI run (36297726158, auth job 47 pass / 6 fail): TC07, TC26, TC35,
  TC37, TC_SEC_001/002 — all around the v1.67 passwordless sign-in chooser; fix on the next local
  pass.
