# 167 — FLIR v1.0.4 review: adversarial verification pass, artifact revision 2 (2026-09-16)

**Prompt:** "have you checked everything correctly. make sure all bugs are valid" (+ the Claude Design
canvas link for the FLIR app). Follow-up to changelog 166.

**What was done.** Every one of the 16 findings from changelog 166 was re-checked by two independent
agents (an evidence auditor re-reading the screenshots and re-querying the backend, and a spec skeptic
reading ZP-3993 / ZP-4187 / ZP-3223 / the Confluence FLIR stories and arguing against the finding),
plus my own re-testing on the emulator and a full read of the **design canvas** linked from the tickets
(`claude.ai/design/p/eb6eba17-…`, opened in the signed-in automation browser; prototype HTML dumped and
parsed). Artifact republished as revision 2 at the same URL.

## Outcome: 4 withdrawn, 6 downgraded to product questions, 2 new findings

**Withdrawn (not defects):**
- R-6 route-run status — **my claim was false**: the backend enum is exactly ACTIVE/FINISHED/ABANDONED;
  ABANDONED *is* the paused state (re-follow reactivates the same run), so the "will be paused" copy is
  correct. Delete-while-following does abandon the run, via the sync queue, ~2m45s later.
- R-10 accessible names — false positive: uiautomator exposes Compose's *unmerged* tree; Compose merges
  the label into the clickable for accessibility services. Remaining nit (no Role.Button) is untested;
  TalkBack row changed PARTIAL → NOT TESTED.
- A-1 email-OTP enable without verification — platform design (address is the account's own sign-in
  address; web + main Android behave the same).
- A-3 no in-app TOTP removal — platform contract (reset is admin-only; `can_remove` is a permission flag).

**Downgraded to questions for product:** R-2 (Next Stop is navigation-only by design per the 1 Sep
feedback, but still lands you where the app says "wrong stop"), R-3 (design canvas defines Details
progress as CAPTURE-based; Confluence S4.1 says stops — spec conflict), R-4, R-5, R-8 (design specifies
the static "Facility Route" title — my "placeholder bug" framing was wrong), R-11 (explained by the
app's own "Room not on Route" copy).

**Corrected in detail:** R-1 duration overstated (~14–15 min evidenced, not 20+); A-5's "showed On while
only Email OTP was enabled" had no screenshot and is withdrawn, leaving the resource-verified static
subtitle point.

**New / strengthened:**
- **S-1 (HIGH)** — reproduced a 3rd time on the *real* network path: device wifi+data off → app correctly
  auto-flips to Offline → WO created offline → wifi+data back on → **app never notices** (>3 min still
  "Offline") → manual toggle shows "Connected · 2 pending" → nothing uploads → force-stop + relaunch runs
  a SyncQueueWorker that reports **SUCCEEDED while the 2 items stay queued**; QA-VERIFY probe 4 never
  reached the backend (re-checked ~10 min later). Force-stop is therefore *not* a reliable recovery.
- **S-2 (HIGH, new)** — the `sync_queue` screen (Sync now, per-item Retry, status, HTTP-status failure
  detail, remove-from-queue, history) ships but has **no reachable entry point** (checked Settings with
  items pending, the account card, a long-press on the tab, the Work Orders header);
  `string/sync_queue_subtitle` has zero code refs. Meanwhile "Refresh data" is blocked with "You have
  changes waiting in the sync queue. Sync them first, then refresh." — an instruction the UI cannot satisfy.
- **A-2 (MEDIUM, kept)** — verified further: the two `/auth/v4/mfa/policy/*` endpoints are write-only
  (405 on GET), so `/auth/v4/mfa/status` is the only readable policy source, and it contradicts the
  app's "organization requires an authenticator app" text.
- **Environment:** Routes is gated by LaunchDarkly flag `feature-work-order-routes`; the app logged
  "identify timed out — using cached flags" at startup. Flag-off tenants see no Routes.

## Process notes
- Memory `flir-ace-app-testing.md` corrected (the old "force-stop always drains it" note was wrong).
- Verification workflow: `verify-flir-findings` (31/32 agent verdicts returned; the last one, and the
  in-workflow device re-run, were superseded by my own device testing and stopped).
- No repo code changed; no Jira updates posted.
