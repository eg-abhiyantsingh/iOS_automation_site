# 084 — Inline WDA-rebuild recovery (wave 8) — the cascade killer, relocated

**Context:** final run 27571754122 (waves 1-6) harvest. Mixed picture:
- ✅ Healthy/clean: Site Selection 43/1/8, Authentication 44/4/0 (TC_SEC_009 fixed),
  Work Order Planning 7/0/8, ZP-323 11/7/10 — all **0 session deaths**.
- ✅ Cascade eliminated: Assets P3 **0 skips** (was 120+).
- ❌ Cascade PERSISTS: Assets P1 (43/8/**49 skip**, 214 death markers) and Assets P5
  (46/4/**50 skip**, 217 death markers).

## Why wave-5's WDA-rebuild didn't stop the Assets P1/P5 cascade
Forensics: Assets P1 logged **153 init failures / 49 skips**, but the caller-side rebuild
arming (`forceWdaRebuildOnce()` in `BaseTest.testSetup`) fired only **ONCE**
("WDA rebuild armed: 1"). The 50 skips all originate at `DriverManager.initDriver:247`
(the `new IOSDriver()` throw) — i.e. most init failures happen on call paths that BYPASS
the wrapped testSetup retry (other init call sites, KEEP_SESSION_ALIVE stale-reference
paths, etc.). Relying on callers to arm the rebuild was fragile.

## Fix — move the rebuild INTO the creation point
`DriverManager.initDriver` now wraps `new IOSDriver(...)` in an inline one-shot retry:
on a session-creation failure (`Could not start a new session` / `WebDriverAgent` /
`xcodebuild` / `Unable to launch`, or when armed), it sets `useNewWDA=true` +
`usePreinstalledWDA=false` + `usePrebuiltWDA=false`, waits 2s, and recreates ONCE. If the
retry also throws, the outer catch propagates (test skips) — but WDA is now rebuilding, so
the NEXT test recovers. This guarantees **every** init failure gets the rebuild regardless
of caller, which the wave-5 caller-side arming missed. ~30-60s on the retry vs a 50-test
cascade. Additive — happy path (first creation succeeds) is untouched.

## Validation
- `mvn -o -DskipTests test-compile` clean; `testng-verify-selftest.xml` 21/21.
- Real proof is the next run: expect Assets P1/P5 49-50 skips to collapse toward single
  digits as wedged sessions rebuild instead of cascading.

## Known residual (NOT fixed here — needs a real v1.43 a11y dump)
Connections improved (PASS 16→31, SKIP 31→9) but 33 of 57 failures are "Should be on New
Connection screen": the wave-3 header '+' locator was a best-guess (no a11y tree was
available) and still doesn't open the New Connection form on v1.43. Fixing it reliably
needs the actual '+' element name from a CI a11y snapshot (local Xcode toolchain is broken
— see memory `local-xcode-toolchain-broken`). Tracked as a Connections-cluster residual,
not a new cluster.
