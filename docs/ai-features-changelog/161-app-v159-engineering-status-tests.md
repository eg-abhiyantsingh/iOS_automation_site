# 161 — App v1.59 into CI + engineering_status tests activated (2026-08-20)

Per request: the updated local app goes into CI, and [iOS] engineering_status sync (iOS #482 /
backend #1057) gets tested.

## App update
- Local build found: `~/Downloads/ios-app-qa-47 (1)/Z Platform-QA.app` → **v1.59** (identical copy
  in Downloads root), **simulator build verified** (LC_BUILD_VERSION platform 7, arm64).
- **Feature presence proven before shipping**: binary-strings probe on `Z Platform-QA.debug.dylib`
  found `engineering_status` ×4 + the `EngineeringStatus` Swift type — with legacy
  `eqp_engineering_approved` ×4 as the probe's positive control. v1.59 = iOS PR #482. ✔ Gate G1.
- Zipped in the CI-expected layout (`Z Platform-QA.app/` at zip root) → replaced
  `apps/Z-Platform-QA.zip` (46.9 MB).

## Backend gate checked live (G2)
Ran the api-contract suite against api.qa: the `engineeringStatusReadiness` canary **SKIPped** —
the `/sld/v3` payload carries NO `engineering_status` on qa (backend #1057 unpromoted, matching
the ticket's dev-only note). So the round-trip is untestable on qa **today**, by backend absence,
not by our gap.

## Tests activated — `EngineeringStatusSync_Test` (self-gating, wired into testng-auth.xml + testng.xml)
| Test | Gating | What it proves |
|---|---|---|
| **TC_ES_020** no-UI sweep | runs NOW on v1.59 | Ticket QA step "no visible change anywhere in the iOS UI": opens the shared asset's Edit screen (with an on-screen precondition so the absence assert can't pass vacuously) and asserts no 'Engineering Status' / 'Data State' control appeared |
| **TC_ES_010** round-trip | SKIPs until backend #1057 on qa | API snapshot of every node's engineering_status → device sync-in → real edit+save (dirty round-trip payload) → site re-selection re-sync → API re-read → **zero values changed/lost**. The exact data-loss the PR fixes |

Implicit coverage running now: the entire v1.59 suite against a backend WITHOUT the field is the
reverse-compat path (decode with field absent) — the app must behave identically, which every
existing test now exercises on the new build.

Still manual/dev-support (per design doc): clone-reset on device (no clone helper in our page
objects yet), unrecognized-status injection (needs mock/dev backend), ×4 web-grid set matrix
(needs the web write path once the column exists).

## Validation
- Compile exit 0; verifier self-tests **39 run / 0 fail**.
- CI dispatched with the new app: auth job (runs TC_ES_*), smoke, api-contract.
