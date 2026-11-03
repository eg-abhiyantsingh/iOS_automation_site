# 198 — ZP-5048 re-check on iOS 1.74, on two sites (3 Nov 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-5048 can you check this again and also check this on other site too"

**Ticket:** ZP-5048 "[iOS] Saving an asset while an SLD view is open replaces the view with the whole site (bends and
hidden edge lost)". To Do, assigned Kush, fixVersion iOS v1.75. Last QA check: 30 Oct on 1.73 (comment 45757).

## Result: STILL REPRODUCES on 1.74, on both sites

Setup:
- App Z Platform-QA 1.74 (Debug-QA, 2 Nov build, still the newest in ~/Downloads).
- Simulator "iPhone 17 - ZP 1.70" (B9AA2053), iOS 26.2, QA, EG-ACME.
- Run between 16:39 and 16:54 IST.
- The simulator was handed over by the web-project session ("jira-chrome-reconnect-tasks (2)") once it had finished.
  The Mac was too low on memory (swap 13.1 / 14 GB) for a second simulator.

| Site / view / asset | View opened | After asset Save | Same view again | All Assets › view | SLD Refresh |
|---|---|---|---|---|---|
| QA-DEMO ZP-4800 SLD canvas v2.4 / "QA-DEMO ZP-4816 view" / QA PNL1 q | 12 / 10, bends + 1 hidden, badge 1 | **131 / 60, no bends, hide drawn, badge 19** | 131 / 60 | 12 / 10 correct | 12 / 10 correct |
| Android Site 2 / "new 1" / Panelboard-2 | 11 / 5, badge 1 | **567 / 198, 556 site assets not in the view, badge 32** | 567 / 198 | 11 / 5 correct | 11 / 5 correct |

Each cell gives assets / connections. Right after Save, the pass-through recorder on `window.updateData` saw the whole
site (131 / 60 and 567 / 198) with `has_view_specific_points` on 0 connections.

How site 2 was chosen: from the web API (read only, logged-in Chrome tab), 58 of 298 acme sites have views. Very few
views have hidden connections. "new 1" on Android Site 2 has view-specific points on 5 of its 6 connections, and the
site has 555 assets, so a replacement by the whole site is obvious.

## Test data
- Both renamed assets were put back (verified with the web API): "QA PNL1 q" and "Panelboard-2".
- The site-1 asset was already "QA PNL1 q" when this check started; it was "QA PNL1" on 30 Oct. It was left as found.
- The site-1 view now also contains "QA-DEMO ZP-4884 CB child OK 2 Nov", so it draws 12 assets.
- The app was switched back to "(s) Wild Goose Brewery" afterwards.

## Jira
No change made. A ready comment (wiki markup, 2 tables, 2 before/after images) is in
`docs/bug-evidence/2026-11-03-zp5048/ZP-5048-recheck-comment.txt`. It is posted only when the owner asks.

## Files
`docs/bug-evidence/2026-11-03-zp5048/`:
- `QA-03Nov-ZP-5048-site1-before-after.png` and `QA-03Nov-ZP-5048-site2-before-after.png`: before is green, after is red.
- `site1-zp4800/`: 7 shots.
- `site2-android-site-2/`: 6 zoomed shots plus 5 "-whole" shots in the app's own framing.
- `scripts/`: the run scripts for the web repo's `.playwright-mcp/ios170/iosrun.sh` lane A runner, plus both run logs.
