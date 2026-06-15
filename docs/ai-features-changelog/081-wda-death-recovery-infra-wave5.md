# 081 — WDA-death recovery infrastructure (wave 5)

**Context:** full-suite run 27557701204. By the 5th cluster (Location part2: 0 pass /
9 fail / 32 skip, 176 death markers) the dominant failure MECHANISM was unmistakable
and cross-cutting:

> A heavy a11y query on a complex SwiftUI screen (asset-class picker with
> bleed-through, OSHA subcategory dropdown, a 33-floor location tree) **wedges
> WebDriverAgent → the session dies → the next `initDriver` retries against the
> same wedged WDA and keeps failing → dozens of tests SKIP** with "Could not start
> a new session." Across just 3 analyzed jobs: **120 + 44 + 32 = 196 skips**, all
> from this one mechanism (Assets P6 logged 372 failed inits vs 123 successful).

Per-method bounding (waves 3-4) fixes the HANG that triggers each wedge, but every
module has its own hang spot and new ones appear. This wave adds the **durable,
cross-cutting infrastructure recovery** so a wedge can't cascade — it helps every
cluster's skip half at once.

## Changes

### 1. WDA-rebuild on init-failure retry (the skip-cascade killer)
`DriverManager.forceWdaRebuildOnce()` + a one-shot `forceWdaRebuild` flag wired into
`initDriver`'s capability setup: when set, the next init uses `useNewWDA=true` +
`usePreinstalledWDA=false` + `usePrebuiltWDA=false`, tearing down the wedged WDA and
rebuilding it (the standard Appium recovery). `BaseTest.testSetup` arms it on BOTH
init-failure retry paths (the catch-retry and the "session appears dead" reinit).
Effect: a WDA wedge now costs **one test's rebuild (~30-60 s)** instead of a
30-120 test skip cascade. Additive — only the failure branch changes; the happy
path (`useNewWDA=false`, fast) is untouched.

### 2. Screenshot-storm cap on dead sessions
`ScreenshotUtil.getScreenshotAsBase64Compressed`: when the compressed capture throws
a session-death signature (`may have died` / `Session` / `ECONNREFUSED` / `timeout`
/ `not created` / `terminated`), return `""` immediately instead of falling back to
the full-PNG capture — which would hit another 90 s readTimeout. ExtentReport calls
this per step + at teardown, so on a dead session the old path padded every hang by
minutes (the Location/Offline screenshot retry storm). Genuine compression errors
still fall back to full PNG.

## Why this is the right lever (and what's deliberately NOT here)
- The 5 clusters share ONE mechanism; fixing it at the infra layer covers all of
  them plus future ones, where per-method whack-a-mole cannot keep up.
- **Held back (high-risk, needs a dedicated effort):** the Location **giant-tree**
  HANG itself — expanding a 33-floor building builds an a11y tree too large for WDA
  to snapshot, wedging it (documented blocker, see memory
  `sitevisit-locations-giant-tree`). Bounding it needs scoped/predicate queries that
  avoid full-tree snapshots after expansion, and can only be validated on CI (local
  Xcode toolchain is broken). The wave-5 recovery means it now costs ~1 test +
  rebuild instead of cascading, so it's no longer a suite-wide blocker.
- **Also held:** lowering `readTimeout` below 90 s (risks false-failing slow-but-valid
  commands on loaded runners) and a sliding-window `DeadSessionCircuitBreaker` (the
  rebuild recovery makes the breaker largely moot — sessions recover instead of
  staying dead).

## Validation
- `mvn -o -DskipTests test-compile` — clean. `testng-verify-selftest.xml` 21/21.
- Additive/recovery-path-only; happy-path driver init unchanged.
