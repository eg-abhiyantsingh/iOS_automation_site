package com.egalvanic.utils;

import com.egalvanic.constants.AppConstants;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cross-cutting run-health gate shared by {@code BaseTest.testSetup} (@BeforeMethod),
 * {@link DriverManager#initDriver} and {@code DeadSessionCircuitBreaker}.
 *
 * <p><b>Why this exists.</b> CI run 28246433532 (2026-06-26) had 5 jobs CANCELLED at
 * the 6-hour GitHub cap — Site Visit phase2/3, Assets P1/P5, Rerun. Forensics on the
 * Assets P1 log proved the mechanism: once WebDriverAgent wedges/dies on a giant
 * SwiftUI DOM, it cannot be rebuilt on that runner, and every {@code @BeforeMethod}
 * {@code DriverManager.initDriver()} then spends ~6 minutes failing
 * (90s request timeout → {@code useNewWDA} rebuild → caller retry). The
 * {@code DeadSessionCircuitBreaker} DID trip, but it only short-circuits the
 * {@code @Test} body — the {@code @BeforeMethod} init thrash ran anyway, so consecutive
 * "skipped" tests were still ~6 min apart. With 50+ tests left after the trip, the job
 * blew past 360 min and was cancelled, truncating the client report.
 *
 * <p><b>What it does.</b> Exposes a single {@link #shouldFastSkip()} that BaseTest checks
 * at the very top of setup. When any of three independent triggers is active it throws a
 * {@code SkipException} immediately — no {@code initDriver}, no WDA rebuild — so the rest
 * of a doomed suite skips in ~0s and lands in {@code failed-suites/} for the fresh-simulator
 * rerun (see the two-report rerun flow in {@code ios-tests-parallel.yml}).
 *
 * <ol>
 *   <li><b>breakerOpen</b> — DeadSessionCircuitBreaker tripped (N consecutive dead/wedged
 *       test outcomes).</li>
 *   <li><b>wdaHopeless</b> — DriverManager saw {@code WDA_HOPELESS_AFTER} consecutive
 *       init failures; WDA will not rebuild for the rest of this job.</li>
 *   <li><b>suite wall</b> — elapsed time since the first test exceeded
 *       {@code SUITE_WALL_MINUTES} (a last-resort backstop, well under the 6h job cap).</li>
 * </ol>
 *
 * <p>All triggers are env/-D overridable via {@link AppConstants}; with healthy runs none
 * fire, so behavior is unchanged. State is static-per-JVM, which is correct here because
 * each CI module job is its own {@code mvn} process (one suite per JVM) and
 * {@link DriverManager} already uses a static (not ThreadLocal) driver.
 */
public final class RunHealth {

    private RunHealth() {}

    private static volatile boolean breakerOpen = false;
    private static volatile boolean wdaHopeless = false;
    private static volatile long firstTestMs = 0L;

    private static final AtomicInteger consecutiveInitFailures = new AtomicInteger(0);

    /** Start the suite wall clock on the first test that actually runs setup. Idempotent. */
    public static void markFirstTestIfUnset() {
        if (firstTestMs == 0L) {
            firstTestMs = System.currentTimeMillis();
        }
    }

    /** Called by DeadSessionCircuitBreaker when it trips, so BaseTest also bails fast. */
    public static void tripBreaker(String why) {
        if (!breakerOpen) {
            breakerOpen = true;
            System.out.println("🛑 RunHealth: dead-session breaker OPEN — " + why);
        }
    }

    public static boolean isBreakerOpen() {
        return breakerOpen;
    }

    /**
     * DriverManager calls this on every init failure. After {@code WDA_HOPELESS_AFTER}
     * CONSECUTIVE failures the run is marked WDA-hopeless. {@link #recordInitSuccess()}
     * resets the streak, so a single transient startup hiccup never trips it.
     */
    public static void recordInitFailure() {
        int n = consecutiveInitFailures.incrementAndGet();
        if (n >= AppConstants.WDA_HOPELESS_AFTER && !wdaHopeless) {
            wdaHopeless = true;
            System.out.println("🛑 RunHealth: WDA HOPELESS after " + n
                    + " consecutive driver-init failures — failing init fast for the rest of this"
                    + " run instead of spending ~6 min/test rebuilding a dead WDA. Remaining tests"
                    + " land in failed-suites/ for the fresh-simulator rerun.");
        }
    }

    public static void recordInitSuccess() {
        consecutiveInitFailures.set(0);
    }

    public static boolean isWdaHopeless() {
        return wdaHopeless;
    }

    public static boolean suiteWallExceeded() {
        long wallMin = AppConstants.SUITE_WALL_MINUTES;
        if (wallMin <= 0 || firstTestMs == 0L) {
            return false;
        }
        return (System.currentTimeMillis() - firstTestMs) > wallMin * 60_000L;
    }

    /** True when remaining tests should skip in ~0s rather than thrash a doomed session. */
    public static boolean shouldFastSkip() {
        return breakerOpen || wdaHopeless || suiteWallExceeded();
    }

    /** Human-readable reason for the fast-skip, shown in the report + console. */
    public static String fastSkipReason() {
        if (suiteWallExceeded()) {
            return "RunHealth suite wall-clock cap (" + AppConstants.SUITE_WALL_MINUTES
                    + " min) exceeded — skipping remaining tests so the job finishes well under the"
                    + " 6h GitHub cap; they land in failed-suites/ for the fresh-simulator rerun.";
        }
        if (wdaHopeless) {
            return "RunHealth WDA-hopeless: WebDriverAgent could not be rebuilt after repeated"
                    + " attempts on this runner; skipping remaining tests fast. They are rerun on a"
                    + " fresh simulator (failed-suites/) so a wedged runner no longer cancels the job.";
        }
        return "RunHealth dead-session breaker OPEN: skipping remaining tests fast instead of"
                + " rebuilding a dead WDA in setup per test (~6 min each). They land in"
                + " failed-suites/ for the fresh-simulator rerun.";
    }

    /** Test seam / per-suite reset (used by the driver-free self-tests). */
    public static void reset() {
        breakerOpen = false;
        wdaHopeless = false;
        firstTestMs = 0L;
        consecutiveInitFailures.set(0);
    }
}
