# 205 — ZP-5171: answer to the Android developer's QR-uniqueness question (4 Nov 2026)

**Prompt:** the Android developer's (assignee) chat questions on ZP-5171, pasted by the user, followed by "anwser this too me".

## What the developer asked
The ticket has two requirements, "skip QR numbers already used in the company" and "the server must check for duplicates", which the developer did not hear on the client call. The developer proposes:
- a QR field per asset in the create flow;
- a start number that fills the empty fields, unique within the screen;
- online and offline, with no backend change.

The developer asks whether that is enough.

## Facts checked (read-only)
- **ZP-5171** (written by Abhiyant, 3 Nov, label field-feedback-3nov) contains both lines in two places:
  - Expected 2: "skips numbers already used in the company (same idea as Web, ZP-4668)".
  - Expected 3: "two users … so the server must check".
  - QA checks 2, 3 and 4 depend on them.
- **ZP-4668** (Web Edit QR codes start number, Done, QA-passed 8 Oct) does **not** skip a used number.
  - Its spec says: warn inline before save "rather than silently hopping to the next free number", so the label numbers stay in order.
  - QA saw "Already used by 13N-H1-1", with Save codes disabled.
  - The fill is client-side, so ZP-5171's reference to it is wrong.
- **iOS app source:** a scan that matches more than one asset opens a "Multiple Assets Found" chooser, so the apps already expect duplicate codes.
  - Places: EntityCreationView `handleQRScan`, AddConnectionView, GroupedTasksTab.
- **QA data** (`GET /api/sld/v3/{id}`, live rows only):
  - Android Site 2: 574 assets, 42 with a QR code, and code "test barcode" is on **3** assets. The server accepts duplicates today.
  - (s) Wild Goose Brewery: 140 assets, 2 with a code, no duplicates.
  - A company-wide scan of all 298 sites was too slow over the browser and was stopped after about 55 sites.

## Answer given to the user (draft reply for the developer, not posted)
- The screen-level fill is enough.
- The two lines were added when the ticket was written; they were not said on the call, and they contradict ZP-4668.
- Optional extra with no backend change: warn "Already used by <asset>" against the site data already on the device, as the web does.
- Server-side uniqueness, if product wants it, is a separate backend ticket and can't be guaranteed offline.
- Offered:
  - to post the reply;
  - to update ZP-5171 and its twins ZP-5179 (iOS) and ZP-5183 (web), which repeat the same lines.

## Changes
Nothing was changed in Jira, Chat or QA data.
