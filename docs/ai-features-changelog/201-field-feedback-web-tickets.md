# 201 — Web tickets for the 3 Nov field-feedback items (3 Nov 2026)

**Prompt:** "https://egalvanic.atlassian.net/issues/?jql=labels%20%3D%20field-feedback-3nov for web also we need to create ticket for web related task"

## Starting point
- The label had 13 tickets:
  - 7 Android (ZP-5168…5174, Rajat);
  - 1 backend (ZP-5175, Avani);
  - 5 iOS (ZP-5176…5180, Kush / Abhiyant).
- There were no Web tickets. The meeting had marked Web "Yes" for the QR, duplicate and naming changes. The other session's
  notes: `Egalvanic_Web-main/docs/changelogs/2026-11-03-field-feedback-tickets.md`.

## Checked on the web
QA, EG-ACME, site "(s) Wild Goose Brewery", 3 Nov 2026, 17:55–18:10 IST, in the logged-in Chrome tab.
Every form was cancelled, so nothing was created.

| Android / iOS item | Web? | What the web does |
|---|---|---|
| Keyboard Next | no change | Tab moves Asset Name › QR Code › Asset Class |
| QR beside names | **partly** | Lists have a QR Code column. Pickers (WO › Link Existing Assets) show name + class only: ZP-4874 SEP (QR QA4956-6237) is shown without its QR, and two "Capacitor 1" rows can't be told apart |
| Create Asset section links | no change | All / Basic Info / Engineering / Condition / Notes |
| Parent-initials prefix | **yes** | Quick Add Child Assets names children `<class display name> <n>` (code in UnifiedEntityEditor): "Circuit Breaker 1, 2, 3" |
| Copy From in a WO | no change | WO › Actions › Add New Asset › ⋮ › Copy Details From… |
| Infrared on the asset page | no change | Maintenance › Infrared, and an Infrared section in Edit Asset |
| WO Detail View / SLD keeps the WO | no change | The asset opens in a side panel over the WO; Open in SLD opens a new tab |
| Duplicate with children | **yes** | No Duplicate in the asset ⋮ menu, the bulk actions or the web code |
| QR per child + start number | **yes** | Quick Add has Class / Subtype / Qty only. The Start number exists only in Edit QR codes (ZP-4668), so it takes 2 steps |
| FOTRIC hotspot / offline / Sentry | no | Not web features (camera connection, offline mode, Android Sentry) |
| Nameplate freeze (library off) | not created | ZP-5175 (backend extraction failures) covers the server side. Testing web Extract with the library turned off would need a settings change |

## Created
All 3 tickets have the same fields:
- Task, High, assignee Abhiyant (to route; no web owner was named);
- sprint Z-26-10-S2 (1226), fixVersion Web v2.5 (14298);
- label `field-feedback-3nov`, moved from Backlog to To Do (transition 2);
- Relates links to the Android and iOS twins and to each other.

| Key | What | Screenshots |
|---|---|---|
| [ZP-5181](https://egalvanic.atlassian.net/browse/ZP-5181) | UI: QR number beside names in pickers; parent-initials prefix for Quick Add child names; a table of the parts that need no web change | 2 |
| [ZP-5182](https://egalvanic.atlassian.net/browse/ZP-5182) | Duplicate an asset with all its child OCPs (new names and QR codes, empty serial numbers) | 2 |
| [ZP-5183](https://egalvanic.atlassian.net/browse/ZP-5183) | QR field per child plus a start number in Quick Add Child Assets (also linked to ZP-4668) | 1 |

How the tickets were written:
- Wiki text went through REST v2 from the Chrome tab, after a `/rest/api/3/myself` check that it was Abhiyant's account.
- The wiki renderer reported 0 errors once the attachments existed.
- The screenshots are embedded with `!file|width=640!`.
- Read back: fields, links, attachments and 2 / 2 / 1 embedded images. They render on the ticket page.
- Duplicate check before creating: no existing web ticket for duplicate, Quick Add naming or QR in pickers.

## Files
`docs/bug-evidence/2026-11-03-field-feedback-web/`:
- 4 annotated PNGs (red boxes);
- the 3 wiki texts W1–W3.

Annotator: `scripts/bug-video/annotate.swift`, run as `swift annotate.swift in out "x,y,w,h,label" …`.

## Mistake caught
A click on the asset page opened "Edit Asset Location". It was closed without Save, and the location is unchanged
(Bldg_465 › Floor 77 Updated_650 › Test).
