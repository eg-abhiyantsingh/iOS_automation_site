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
