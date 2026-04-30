# 055 — Appium HTTP Timeout: Fix Tests Hanging 7 Min On Dead Sessions

**Date**: 2026-04-30
**Trigger**: Local analysis of CI Run #24876293380 surefire reports revealed the real time problem.

---

## What I Found Locally (Before Touching Anything)

Parsed 607 tests across 11 suites in `testcase_file/CI-Report-Run-24876293380/`. The data:

| Metric | Value |
|---|---|
| Median passing test duration | **132s (2.2 min)** |
| 95th percentile passing | **360s (6 min)** |
| Tests >5 min | **71 (12%)** |
| Tests >2 min | **283 (47%)** |
| Tests <30s | 1 |

Per-suite worst offenders:

| Suite | Tests | Total | Avg/test |
|---|---|---|---|
| Asset_Phase1 (across part1+part2) | 112 | **5h44m** | 3.1 min |
| Issue_Phase2 | 50 | **4h27m** | **5.3 min** |
| Issue_Phase1 (in p3 suite) | 16 | 1h40m | **6.2 min** |
| Asset_Phase2 | 108 | 3h27m | 1.9 min |

Earlier estimates of "20s/test" were off by ~7×.

---

## The Smoking Gun

**16 tests hang for the full 7-minute TestNG suite-level timeout** (`time-out=420000` in testng XML), then get killed by `ThreadTimeoutException`. Stack trace:

```
org.testng.internal.thread.ThreadTimeoutException: Method ... didn't finish within the time-out 420000
  at java.base@17.0.18/jdk.internal.misc.Unsafe.park(Native Method)
  at java.util.concurrent.locks.LockSupport.park(...)
```

`Unsafe.park` = thread blocked on a lock = **Appium HTTP request that never returns**. When the iOS WDA bridge dies mid-test, every subsequent Appium command blocks indefinitely. The TestNG suite timeout is the only thing that catches it — but it costs the FULL 7 minutes per stuck test.

**16 stuck tests × 420s = 112 min wasted per CI run on dead-session probes.**

This matches the existing memory note in BaseTest:
> *"every Appium HTTP call (screenshot, terminateApp, quit) hangs 7+ min each, turning a single failure into a 14-21 min teardown"*

---

## The Fix

### [DriverManager.java](../../src/main/java/com/egalvanic/utils/DriverManager.java) — wrap IOSDriver construction with HTTP timeouts

**Before**:
```java
IOSDriver newDriver = new IOSDriver(appiumServer, options);
```
The default Selenium/Appium HTTP client has **no read timeout** — requests block indefinitely when the server doesn't respond.

**After**:
```java
ClientConfig httpConfig = ClientConfig.defaultConfig()
        .baseUrl(appiumServer)
        .connectionTimeout(Duration.ofSeconds(60))
        .readTimeout(Duration.ofSeconds(90));
IOSDriver newDriver = new IOSDriver(httpConfig, options);
```

Now every Appium HTTP request fails after **90 seconds** if the server doesn't respond. Healthy calls return in <1s; only dead-session probes hit the cap.

---

## Why This Is Safe

| Concern | Reality |
|---|---|
| "Will it break legitimate slow operations?" | No — 95th percentile passing test is 360s **total**, but individual Appium *commands* almost always complete in <1s. The median test makes hundreds of commands. The 90s cap is per-command, not per-test. |
| "What about WDA build / app install / boot?" | Those use separate timeouts (`wdaLaunchTimeout=600s`, `simulatorBootTimeout=300s`, `launchTimeout=300s`) — set in the XCUITestOptions and unaffected by the HTTP client cap. |
| "Will failures look different?" | Slightly: instead of `ThreadTimeoutException at Unsafe.park`, dead-session tests now get `org.openqa.selenium.TimeoutException: Read timed out` from the HTTP client. BaseTest's `isSessionLikelyDead()` already handles this path (terminates app, marks session dead, fast teardown). |
| "Could it break CI bring-up?" | The 60s `connectionTimeout` is for the initial socket connection to Appium server — already established successfully in the WDA warm-up step (the workflow's `Warm-up WDA Session` step takes ~30s and runs before `Run Tests`). |

---

## Expected Savings

**Per CI run** (Asset_Phase1 + Issue_Phase1/2 are the worst offenders):
- 16 tests that previously hung 420s now fail-fast at 90s
- Saves: 16 × (420 - 90) = **~88 min per parallel run**

**Conservatively** (if only half the suites hit dead-session issues): ~40-50 min/run saved.

This is **~30-50× larger than the Maven cache (#053) or shared-asset PoC (#054)** wins, and it's purely defensive — no test behavior change.

---

## Validation Plan

1. Push this commit.
2. Cancel the in-flight CI runs (still in progress at 90+ min) since they pre-date this fix.
3. Re-dispatch one or both of `assets-p1` / `issues-p1`.
4. Compare wall-clock time and the count of `ThreadTimeoutException` entries in the new run.
5. If 16 → 0 timeout exceptions, this fix is verified. Time savings will follow.

If something regresses (a healthy test trips the 90s cap), the fix is to tune the cap upward or identify the slow Appium command. We have `90s` headroom over typical command duration (~1s) so that's a 90× safety margin.

---

## What's Still Open

- **Tests at 5-7 min that actually progress** (51 of them, ~10% of total): these aren't hung, just legitimately slow. Likely flexible-locator fallback chains hitting `IMPLICIT_WAIT=5s` repeatedly. Future work — measure per-helper, then either tighten the implicit wait or refactor the highest-cost helpers.
- **Connections / SiteSelection tests at 1.7-2 min avg**: probably login + nav overhead. Shared-asset pattern + shared-login pattern can help.
- **Asset_Phase1 BUG_* tests at 7.3 min**: possibly hitting same hang issue or genuinely doing many redundant operations. Tackle after this fix lands and we have clean baseline data.
