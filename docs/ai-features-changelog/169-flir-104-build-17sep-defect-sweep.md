# 169 — FLIR v1.0.4(5) 17-09 build: full defect sweep + artifact (2026-09-18)

**Prompt:** "Egalvanic_v1.0.4(5)_qa_release_17-09-2026.apk latest app test in this app and share me
artificat of bugs"

**Deliverable:** <https://claude.ai/artifact/NoZdmP3MFMNe3UFCbkVhyd> — "FLIR 1.0.4 Defect Sweep".
No repo code changed; this repo gains this changelog only.

## Build under test
`Egalvanic_v1.0.4(5)_qa_release_17-09-2026.apk`, md5 `f6c26691d4fc736621ef7921c9e44460`,
versionName 1.0.4, installed on `quacky_test` (API 35) 2026-09-18 12:59 IST. Backend
`eg-pz.qa.egalvanic.ai`, tenant acme, site "10 SEP 2026", WO IR-A6FE, route "Route 2 - abhiyant".

## Method upgrade that made this pass different
`adb root` works on this emulator → the app's Room DB is readable at
`/data/data/com.egalvanic.pz.ace.qa/databases/zplatform_ace.db`. Every UI claim was cross-checked
against `route_runs / route_run_stops / route_run_assets / sync_queue_item / sync_log /
node_session_mapping / sld_choices` **and** the backend (`/route_run/session/{id}`,
`/ir_session/{id}/full`, `/auth/v4/mfa/status`). Copy changes were diffed from
`aapt2 dump resources` of both APKs (8 changed / 9 added / 3 removed strings).

## Result: 10 fixed, 11 open (5 medium + 6 low), 0 blockers

**Fixed and verified** — ZP-4263 progress dot (0 red px in the bar band at 0%); ZP-4264 active-site
marker on the post-sign-in picker; "1 issues" → plurals (`room_asset_media_counts` deleted,
`plurals/photos_count` added); `logout_confirm_unsynced` → `unsynced_changes_count`; Settings sync
copy; Sync-queue row with live count (`sync_queue_pending_subtitle`); Log out → Sign out; "Stop 0 of
12" → "Stop 2 of 2"; Unmark Processed; run-scoped stop/tick/percentage agreement.

**Still open (re-tested):** F-01 route banner shows the run's current stop not the room's (room *new*
= stop 1 prints "Stop 2 of 2"); F-02 Next Stop → "Wrong stop for the Route · Go to: new"; F-03
Details "Inspection route 3 / 7 captured" is a capture count and contradicts "1 of 2 stops
completed"; F-04 email-code policy text contradicts `/auth/v4/mfa/status`
(`organization_email_otp_allowed: true`) *and* the Set Up Email Codes button under it; F-05
`current_position` stays 0 on ACTIVE runs, `completed_by` always null.

**New:** F-06 offline-created route run `f09fc782` uploaded with `success=1` in 742 ms, server kept
its own ACTIVE run `670e1a5c`, local run flipped to ABANDONED and its 07:41 stop-arrival + asset row
never merged — UI said "Everything is synced."; F-07 four strings still promise automatic upload
(`sync_offline_banner`, `create_wo_offline`, `create_wo_online`, `notif_sync_failed_text`); F-08 sync
queue rows show a truncated UUID or nothing; F-09 the active site is absent from Settings › Switch
site and `sep` / `10 SEP` return "No sites available."; F-10 palette names are raw SDK ids
(iron/blackhot/bw/rainHC…); F-11 "Remove from Route" copy calls an asset a stop (destructive path
NOT executed — shared route).

## Three candidate findings dropped after checking
- WO card "4 LOCATIONS" vs 3 rooms-with-assets → correct, room R1 has no assets.
- Sync badge "1 pending" → not site-scoped; it read 13 and the queue held exactly 13.
- "2FA still On after an admin reset" → I was reading the **prod** app (`com.egalvanic.pz.ace`,
  14-09 build) signed in as `+acme`; the back stack crossed the task boundary. Always check
  `mCurrentFocus` before believing a FLIR screen.

## Side facts worth keeping
- MFA on `abhiyant.singh+admin@egalvanic.com` has been **reset** — `auth/v4/login` now returns a
  token with no challenge (`enrolled:false`). The iOS-suite login blocker from 2026-09-16 is gone.
- The user shares `emulator-5554`; the prod APK is installed alongside the QA one.
