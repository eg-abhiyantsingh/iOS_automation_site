# 195 — ZP-3914 bugs filed without asking (Triage) + app v1.74 into CI

Date: 2026-11-02 (IST)

## Prompt

"save in memory to create bug without asking" → "and assign to me in triage" → "I have updated the app".

## 1. Standing rule saved

New memory `feedback_jira_bugs_file_without_asking`: confirmed product defects are filed straight away, no
approval round. Fields: assignee **Abhiyant** (`712020:8f7852ec…`), sprint **Triage** (`customfield_10020` = 778,
future sprint on board 107 — "Triage" is a sprint, not a status), **To Do**, **no fixVersion** (future sprint),
platform label + `[iOS]` summary prefix, ZP-5080 description layout. The older memories
(`feedback_no_headless_no_jira`, `feedback_jira_new_bugs_todo`, `feedback_act_dont_ask`) and the CLAUDE.md line
now point at it.

## 2. Bugs filed (all read back: Triage, To Do, Abhiyant, label iOS, fixVersion empty)

| Jira | Id | Priority | Test |
|---|---|---|---|
| ZP-5152 | SW-WIZ-01 — wizard "Assign a site later" clears the customer | Low | TC_SWW_08 |
| ZP-5153 | MKP-LABEL-01 — markup label shows 210 chars at 200/200 | Low | TC_PM_04 |
| ZP-5154 | EGF-ADD-01 — "Forms (1)" has no Add Form path | Medium | ghost-value tests' workaround |

A duplicate search (last 30 days, wizard / markup / Select a Form text) found nothing. The scratchpad drafts had
been wiped with the date change; they were rebuilt from the session transcript (Write + Edit replay).

## 3. Quarantine

`failed-suites/quarantine.txt` gains TC_SWW_08 and TC_PM_04 (BUGS.md section "ZP-3914 product defects"), so the
Summary job's test gate stops failing on these two known product bugs. No other test name contains either token.
Remove each line when its ticket is fixed.

## 4. App v1.74 into CI

`~/Downloads/Z Platform-QA.app`: CFBundleShortVersionString **1.74**, iphonesimulator SDK 26.5, Xcode 2660,
MinimumOSVersion 18.5 (CI sim is 18.5), built 2026-11-02 06:10. `codesign --verify --deep` clean (copy complete).
Zipped with `ditto -c -k --norsrc --keepParent` (93.75 MB, `Z Platform-QA.app/` at root), committed as c92f24a
"chore(app): update Z Platform-QA to v1.74 for CI (2026-11-02 QA build)".

Next: ZP-3914 am + forms shards on v1.74 to see what the new build changed.

## 5. First ZP-3914 run on v1.74 — run 41406840983 (7b1f3ad)

- **forms 33/35**, identical to v1.73: TC_PM_04 still fails (ZP-5153 NOT fixed in 1.74; now quarantined), TC_BX_04
  data skip. TC_TOU_03, TC_EGF_02 and the ghost teardown all green.
- **am 22/25**, three consecutive Site Walk wizard fails, none a v1.74 UI change (screenshots show the 1.73 Customer
  step unchanged; TC_SWW_09 picked '6th OCT' fine right after):
  - TC_SWW_05: every UI assertion passed; the closing backend check `walkIds()` threw `SocketException: Connection
    reset` on GET /site-walk/list. `TestDataApi.send` retried only a 401.
  - TC_SWW_07: WDA died ("Could not proxy … read ECONNRESET") on the tap at (201, 314) — the 'Assign customer and site
    later' row. Dead-session breaker 1/5, new session for the next test. CI environment.
  - TC_SWW_08: on that fresh session the '6th OCT' tap left the wizard on step 1 (the row was on screen). It never
    reached the ZP-5152 assertion, so this run says nothing about whether 1.74 fixed SW-WIZ-01.
- Local reproduction was NOT run: another session was driving the only allowed automation sim ("iPhone 17 - ZP 1.70",
  Appium 4723), and the rule is one automation sim at a time. The fixes are validated driver-free + on CI instead.

### Fixes

- `TestDataApi.get()` retries ONCE on a transport failure (IOException cause). GET only — a POST/PUT could double-
  create. Driver-free proof with a local socket server (`ws` scratch probe):
  - half a reply then RST (= the CI `Connection reset`): **old code throws after 1 connection; new code retries → 200**;
  - RST before any byte: the JDK HttpClient already retries a GET once on its own (2 connections, no 🔁 line);
  - RST on every connection: 4 connections (JDK × ours), then the same IllegalStateException — bounded, no loop.
- `SiteWalkPage.chooseCustomer()` makes a second tap when the first leaves the wizard on step 1 with the row still
  there (customer-list loading overlay on a fresh session); a row that is gone counts as success only if step 2 is up.
- Compile clean, verifier self-tests 39/39, `ZP3914_FormsBulkExportTags_ApiTest` 9 pass + TC_BX_04 skip against the
  live backend (GET happy path unchanged).

## 6. AM rerun on v1.74 — run 41434446888 (d9c4657): 23/25

- TC_SWW_05 **passes** with the GET retry.
- TC_SWW_08 now reaches its real assertion and fails on it: "step 2 says 'This customer has no sites yet' although
  '6th OCT, 1 site' has sites" → **ZP-5152 is NOT fixed in v1.74**; stays quarantined.
- TC_SWW_07 FAIL "The walk must be deleted on the backend too" (new; it passed on v1.73 CI 41108150037). Every UI step
  passed — the row left the list — but walk af664125 was still on the backend after 20 s; teardown removed it via
  the API (HTTP 200). The at-failure screenshot shows the 'Wi-Fi Off' network icon (app offline) and a
  'Site Walk — 2026-11-02' row back in the list. That row is this test's own walk: SWW_06's same-titled walk was
  deleted by API before SWW_07 began, and SWW_07 saw exactly one matching row at the start.
  - None of deleteWalkRow's taps can reach the network control (row, its context menu, the alert's buttons only).
  - Binary diff v1.73 (641fb88) vs v1.74 debug dylib: the site-walk endpoint strings (`/site-walk/`,
    `/site-walk/list`, …) and all 137 offline/network strings are identical → the delete path did not change.
  - Verdict: most likely the CI runner lost the backend mid-delete (the same runner pool reset GET /site-walk/list
    in run 41406840983), the app went offline and the row came back. Not filed as a product bug. Not reproduced
    locally — the only allowed iOS sim was still in use by another session, and the Android emulator was up.
- The test now names the app's state when the backend check fails ("app shows 'Wi-Fi Off' / is online, N queued,
  row back in the list / still gone"), so the next failure says which case it is.

## 7. AM confirmation run on v1.74 — run 41453293677 (b362018): 22 pass, 2 fail, 1 skip

- TC_SWW_08 fails on the ZP-5152 assertion again (quarantined, expected).
- TC_SWW_07 fails again with the app showing 'Wi-Fi Off', this time at "The row must leave the list" (the row never
  left). TC_SWW_06 just before it created and API-deleted its walk normally; the test's own API calls on the same
  runner kept working (teardown DELETE HTTP 200, attempt 1).
- TC_SWW_05 SKIP: right after a soft restart, Settings had no 'Switch Experience' row (passed in the run before).

### What the app source says (developer repo `release/qa` 496c14d = v1.74, read only)

- v1.73 → v1.74 is three commits: version bump, ZP-5135 (FLIR-SEP pairing), ZP-5093 (FOTRIC IR photos). Nothing in
  site walks, `NetworkState`, `NetworkReachability`, `ExperienceManager` or Settings changed — no v1.74 regression.
- `NetworkState` goes offline by itself in exactly one place: the `NWPathMonitor` sink, when iOS reports the path
  unsatisfied. It never comes back online on its own ("User must manually switch to online mode"). The only other
  way is the toolbar menu (`toggleMode`), which TC_SWW_07 never opens.
- `SiteWalkStore.deleteWalk`: a synced walk deleted while offline returns `.requiresOnline`; a failed request
  returns `.failed(error)`. `SiteWalkListView` then shows the alert "Couldn't delete site walk" with "Deleting a
  synced walk requires a connection." or the error text, and the row stays. With `autoAcceptAlerts` back on after
  `withAlertsManual`, that alert was accepted unseen (`sleep(1500)` is a no-op), so the test only saw a stuck row.
  The run-41434446888 "row left, then came back" is the same thing read while the alert covered the list.
- Settings shows 'Switch Experience' only when `ExperienceManager.displayable.count > 1`. Roles are fetched ONCE per
  launch (`ContentView .task`, guarded by `isResolved`); a failed fetch resolves to Field Technician only and is
  not retried until the next launch.

Verdict: CI environment (iOS reported the network lost on the runner's simulator), not an app defect. Nothing
filed.

### Fixes (automation)

- `SiteWalkPage.deleteWalkRow`: after a confirmed delete it waits, with alerts still manual, for the row to go or
  for a different alert; that alert's text goes to `lastDeleteError` and OK closes it. New `DELETE_FAILED`.
- TC_SWW_07: if the app is offline before the delete, it is switched back online with a WARNING in the report (the
  test never opens the network menu, so only iOS can have done it). If the app refuses the delete with a
  connectivity message AND is offline, one more delete once online, again with a WARNING. Any other refusal fails
  with the app's own words and the app state ("is online / shows 'Wi-Fi Off', N queued, row in the list / gone").
- `ZP3914BaseTest.accountManagerOffered`: when Settings was reached but has no 'Switch Experience', relaunch the
  app once (re-runs the role fetch) and look again before skipping.

### Local reproduction

Tried on the 402 pt "iPhone 17" sim (iOS 26.2, own Appium 4731 / WDA 8131, nothing else booted): blocked before the
test body — on the fresh install the site picker's row tap opened 'Create New Site' › 'Select Office', and WDA then
served a stale tree while taps did nothing (known iOS 26.2 local sign-in block). Stopped there; validation is on CI.
Compile clean, verifier self-tests 39/39.
