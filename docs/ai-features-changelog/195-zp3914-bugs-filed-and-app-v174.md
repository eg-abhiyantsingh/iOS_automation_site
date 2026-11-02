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
