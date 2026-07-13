# Skip triage — run 29135128275 (962 original skips / 865 after rerun)

Every skipped test-method parsed from the module surefire XMLs, clustered by
its actual skip reason. Verdict legend: CASCADE (symptom of a wedge — fixed by
the failure burn-down, not individually), BY-DESIGN (correct guard, stays),
ACTIONABLE (its own automation bug), INFRA-GAP (needs environment work).

## The taxonomy

| n | Reason | Verdict |
|---|---|---|
| 782 | Dead-session circuit breaker OPEN (5 consecutive dead-session tests) | CASCADE — worst: Asset_Phase3 ×96, SiteVisit_p3 ×90, Asset_Phase4 ×85, Asset_Phase1 ×64, AssetEngineer* ×95 |
| 38 | `Failed to initialize driver: Could not start a new session` | CASCADE — the 5 strikes that open the breaker |
| 34 | OfflineTest: "Offline tests require real WiFi toggle — not available on CI simulators" | BY-DESIGN — run locally per standing priority; CI gates them |
| 24 | Connections: "Could not enter selection mode" (+4 siblings: Edit option, X icons, Select Multiple, AF Punchlist) | **ACTIONABLE — single broken helper** (⋯ menu → Select Multiple flow; suspected v1.49 menu-row press quirk) silently skips 24 tests |
| 18 | RunHealth suite wall-clock cap (240 min) | CASCADE — honest guard; shrinks as hangs are fixed |
| 20 | OfflineSyncMultiSite "Infra needed: …" (backend stubs, 2nd user, token expiry, old-build upgrade, S3 checks) | INFRA-GAP — documented, deliberate |
| 15 | Genuine preconditions (ZP323 shortcuts/listen/copy ×9, WOP plan-shape ×4, ArcFlash session-rooms ×2) | BY-DESIGN — fixture-dependent |
| 11 | WDA-hopeless (6) + testSetup timeout (5) | CASCADE/infra |
| 8 | SiteSelection offline sub-suite (WiFi toggle) | BY-DESIGN |
| 2 | KNOWN APP BUG quarantines (ATS-VAL-*: name whitespace/trim — BUGS.md) | BY-DESIGN — quarantined real app bugs |
| 2 | Camera / no Resolved-issue preconditions | BY-DESIGN |
| 1 | Stray `Driver not initialized` (Issue_Phase2) | CASCADE tail |

## Cascade attribution (which @Before actually failed, per class)

`testSetup` is the cascade vector everywhere: Asset_Phase3 ×102 (setup itself
timed out at 480s — wedged session inherited from the subtype-hang tests),
Asset_Phase4 ×86 + Asset_Phase1 ×65 + Asset_Phase5 ×63 (driver init failed —
sim/WDA dead), SiteVisit_p2/p3 ×122 (wall-clock + breaker), Issues ×109 +
AssetEngineer ×95 (breaker).

## Bottom line

- **~849 skips (88%) are cascade symptoms** with exactly one cure: fix the
  wedge-source failures already triaged in
  `failure-triage-run-29135128275.md` (giant-tree/list query scoping, Issues
  remap, IR-photos remap). No per-test work exists for these.
- **~80 skips are correct behavior** (WiFi gating, infra-gap documentation,
  fixtures, app-bug quarantines) and should stay.
- **1 NEW actionable found by this audit:** the Connections selection-mode
  helper — one fix recovers 24 silently-skipped tests (TC_CONN_073-096).
