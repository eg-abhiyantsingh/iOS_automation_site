package com.egalvanic.listeners;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.DriverManager;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Circuit breaker for dead-session cascades.
 *
 * When the Appium/WDA session dies mid-run and recovery fails, every remaining
 * test burns its full timeout probing a corpse (run 27320962984: 65 tests thrown
 * away this way). This listener counts CONSECUTIVE tests that end in a
 * driver-dead failure/skip; once {@code DEAD_SESSION_BREAKER_N} (default 5) is
 * reached it trips OPEN and skips every remaining test fast, with a message
 * pointing at the failed-suites/ rerun for triage. Any healthy outcome —
 * a pass, or a failure/skip with a live session and no session-death signature —
 * resets the count, so ordinary flaky failures never trip it.
 *
 * "Dead" detection (after each test): {@link DriverManager#isDriverActive()} is
 * false, OR the result's throwable chain mentions 'Session' / 'ECONNREFUSED' /
 * 'has already finished'. The probe runs in afterInvocation, i.e. BEFORE
 * @AfterMethod teardown quits the driver, so an inactive driver there genuinely
 * means the session died during the test (or setup never produced one).
 *
 * Gated by env/-D DEAD_SESSION_BREAKER (default true); threshold via
 * DEAD_SESSION_BREAKER_N. Registered via META-INF/services/org.testng.ITestNGListener.
 */
public class DeadSessionCircuitBreaker implements IInvokedMethodListener {

    private final boolean enabled;
    private final int threshold;
    private final BooleanSupplier driverAlive;

    private final AtomicInteger consecutiveDead = new AtomicInteger(0);
    private volatile boolean tripped = false;

    public DeadSessionCircuitBreaker() {
        this(AppConstants.DEAD_SESSION_BREAKER,
             AppConstants.DEAD_SESSION_BREAKER_N,
             DriverManager::isDriverActive);
    }

    /** Seam for driver-free self-tests: inject gate, threshold and liveness probe. */
    public DeadSessionCircuitBreaker(boolean enabled, int threshold, BooleanSupplier driverAlive) {
        this.enabled = enabled;
        this.threshold = threshold;
        this.driverAlive = driverAlive;
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!enabled || !method.isTestMethod()) {
            return;
        }
        if (tripped) {
            throw new SkipException("Dead-session circuit breaker OPEN: " + threshold
                    + " consecutive tests ended on a dead Appium session; skipping the rest of"
                    + " this run instead of burning the per-test timeout on each. Skipped tests"
                    + " land in failed-suites/latest.xml — triage via rerun-failed-by-date.yml"
                    + " or `mvn test -DsuiteXmlFile=failed-suites/latest.xml`.");
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!enabled || !method.isTestMethod() || tripped) {
            return;
        }
        if (testResult.getStatus() == ITestResult.SUCCESS) {
            consecutiveDead.set(0);
            return;
        }
        if (looksDead(testResult)) {
            int dead = consecutiveDead.incrementAndGet();
            if (dead >= threshold) {
                tripped = true;
                System.out.println("🛑 Dead-session circuit breaker TRIPPED after " + dead
                        + " consecutive driver-dead test outcomes — all remaining tests will be"
                        + " skipped fast (set DEAD_SESSION_BREAKER=false to disable)");
            } else {
                System.out.println("⚡ Dead-session breaker: " + dead + "/" + threshold
                        + " consecutive driver-dead outcomes");
            }
        } else {
            // Healthy failure/skip (real assertion, missing precondition) breaks the chain
            consecutiveDead.set(0);
        }
    }

    /** True when the outcome carries a session-death signature. */
    private boolean looksDead(ITestResult result) {
        if (!driverAlive.getAsBoolean()) {
            return true;
        }
        Throwable t = result.getThrowable();
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Session")
                    || msg.contains("ECONNREFUSED")
                    || msg.contains("has already finished"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
