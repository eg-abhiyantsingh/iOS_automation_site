package com.egalvanic.verify;

import com.egalvanic.base.BaseTest;
import com.egalvanic.listeners.DeadSessionCircuitBreaker;
import org.testng.IInvokedMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/**
 * Driver-free proof that the session-death recovery layer is USEFUL: the circuit
 * breaker must trip after EXACTLY N consecutive dead outcomes (RED on a cascade),
 * stay closed for healthy outcomes (GREEN on ordinary flake), and runWithBudget
 * must abandon a hung body within its budget. Runs on a plain JVM — no Appium.
 *
 * <p>The breaker is exercised through its injection seam (gate/threshold/liveness
 * probe) with {@link Proxy}-built ITestResult / IInvokedMethod fakes, so no real
 * TestNG run or driver is needed.
 */
public class SessionRecoverySelfTest {

    // ---- DeadSessionCircuitBreaker: catches the "65 tests burned on a dead session" class ----

    @Test
    public void breaker_trips_afterExactlyN_consecutiveDeadOutcomes() {
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 3, () -> false);
        IInvokedMethod m = fakeTestMethod(true);

        // N-1 dead outcomes: breaker must stay CLOSED (no SkipException)
        for (int i = 0; i < 2; i++) {
            breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)); // must NOT throw
            breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        }
        breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)); // still closed at 2/3

        // Nth dead outcome trips it OPEN
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        assertThrows(SkipException.class,
                () -> breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)));
    }

    @Test
    public void breaker_resets_onSuccessfulTest() {
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 3, () -> false);
        IInvokedMethod m = fakeTestMethod(true);

        // 2 dead, then a PASS (reset), then 2 more dead — never reaches 3 consecutive
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.SKIP, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.SUCCESS, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)); // must NOT throw

        // ...and the 3rd consecutive dead outcome after the reset trips it
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        assertThrows(SkipException.class,
                () -> breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)));
    }

    @Test
    public void breaker_ignoresHealthyFailures_andBreaksTheChain() {
        AtomicBoolean alive = new AtomicBoolean(false);
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 3, alive::get);
        IInvokedMethod m = fakeTestMethod(true);

        // 2 dead outcomes, then a REAL assertion failure on a live session — chain broken
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        alive.set(true);
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE,
                new AssertionError("Save button should be visible")));
        alive.set(false);
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)); // must NOT throw at 2/3
    }

    @Test
    public void breaker_detectsSessionDeath_fromThrowableChain_evenWithLiveProbe() {
        // WDA can die while the Appium HTTP server still answers the liveness probe —
        // the throwable signature is the only evidence ('Session' / ECONNREFUSED / WDA msg)
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 2, () -> true);
        IInvokedMethod m = fakeTestMethod(true);

        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE,
                new RuntimeException("Session not found"))); // top-level message
        breaker.afterInvocation(m, fakeResult(ITestResult.SKIP,
                new RuntimeException("setup failed",
                        new RuntimeException("connect ECONNREFUSED 127.0.0.1:8100")))); // nested cause
        assertThrows(SkipException.class,
                () -> breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)));
    }

    @Test
    public void breaker_disabled_neverTrips() {
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(false, 1, () -> false);
        IInvokedMethod m = fakeTestMethod(true);
        for (int i = 0; i < 10; i++) {
            breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, null));
        }
        breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, null)); // must NOT throw
    }

    @Test
    public void breaker_ignoresConfigurationMethods() {
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 1, () -> false);
        IInvokedMethod config = fakeTestMethod(false);
        breaker.afterInvocation(config, fakeResult(ITestResult.FAILURE, null)); // must not count
        breaker.beforeInvocation(fakeTestMethod(true),
                fakeResult(ITestResult.FAILURE, null)); // must NOT throw
    }

    // ---- runWithBudget: catches the "hung teardown blocks every later test" class ----

    @Test(timeOut = 10_000) // injected defect (unbounded join) goes RED here, not hang
    public void runWithBudget_returnsWithinBudget_whenBodyHangs() {
        long start = System.currentTimeMillis();
        BudgetCaller.call("selftest-hang", 1, () -> {
            try {
                Thread.sleep(30_000); // simulates an Appium call stuck on a dead session
            } catch (InterruptedException ignored) {
            }
        });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 5_000,
                "runWithBudget must abandon a hung body at the 1s budget; took " + elapsed + "ms");
    }

    @Test(timeOut = 10_000)
    public void runWithBudget_returnsPromptly_whenBodyFinishes() {
        long start = System.currentTimeMillis();
        BudgetCaller.call("selftest-fast", 30, () -> { /* completes immediately */ });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 5_000,
                "runWithBudget must return when the body finishes, not at the budget; took "
                        + elapsed + "ms");
    }

    /**
     * Exposes the protected static {@code BaseTest.runWithBudget} to this test.
     * Never instantiated / never run by TestNG, so none of BaseTest's driver
     * lifecycle config methods execute.
     */
    private static final class BudgetCaller extends BaseTest {
        static void call(String label, int seconds, Runnable body) {
            runWithBudget(label, seconds, body);
        }
    }

    // ---- Proxy-built TestNG fakes (interfaces are large; only what the breaker reads) ----

    private static ITestResult fakeResult(int status, Throwable throwable) {
        return (ITestResult) Proxy.newProxyInstance(
                SessionRecoverySelfTest.class.getClassLoader(),
                new Class<?>[]{ITestResult.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getStatus":
                            return status;
                        case "getThrowable":
                            return throwable;
                        case "isSuccess":
                            return status == ITestResult.SUCCESS;
                        default:
                            return defaultValue(proxy, method.getName(),
                                    method.getReturnType(), args);
                    }
                });
    }

    private static IInvokedMethod fakeTestMethod(boolean isTestMethod) {
        return (IInvokedMethod) Proxy.newProxyInstance(
                SessionRecoverySelfTest.class.getClassLoader(),
                new Class<?>[]{IInvokedMethod.class},
                (proxy, method, args) -> {
                    if ("isTestMethod".equals(method.getName())) {
                        return isTestMethod;
                    }
                    if ("isConfigurationMethod".equals(method.getName())) {
                        return !isTestMethod;
                    }
                    return defaultValue(proxy, method.getName(), method.getReturnType(), args);
                });
    }

    private static Object defaultValue(Object proxy, String name, Class<?> returnType, Object[] args) {
        switch (name) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "fake-" + returnType.getSimpleName();
        }
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == short.class) return (short) 0;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == char.class) return (char) 0;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        return null;
    }
}
