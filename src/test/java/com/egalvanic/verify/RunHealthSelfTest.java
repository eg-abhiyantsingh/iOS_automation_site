package com.egalvanic.verify;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.listeners.DeadSessionCircuitBreaker;
import com.egalvanic.utils.RunHealth;
import org.testng.IInvokedMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/**
 * Driver-free proof that the 6h-cancellation containment layer is USEFUL (run
 * 28246433532). Runs on a plain JVM — no Appium, no driver.
 *
 * <p>Covers the two new behaviors that actually stop the cascade:
 * <ul>
 *   <li>{@link RunHealth#recordInitFailure()} flips WDA-hopeless after EXACTLY
 *       {@code WDA_HOPELESS_AFTER} consecutive failures (off-by-one => RED), and a
 *       success resets the streak.</li>
 *   <li>The broadened {@link DeadSessionCircuitBreaker} trips on a WEDGE signature
 *       ("Could not start a new session" / "Request timeout … 90000 ms") even when the
 *       liveness probe says the session is alive — the exact case that previously let a
 *       wedged runner burn to the 6h cap. Removing those signatures makes this go RED.</li>
 *   <li>A tripped breaker flips the shared {@link RunHealth} gate, so BaseTest's
 *       @BeforeMethod would fast-skip (not just the @Test body).</li>
 * </ul>
 */
public class RunHealthSelfTest {

    @BeforeMethod
    public void isolateStaticState() {
        RunHealth.reset(); // static-per-JVM state — isolate each case
    }

    @Test
    public void cleanRun_doesNotFastSkip() {
        assertFalse(RunHealth.shouldFastSkip(), "a healthy run must never fast-skip");
        assertFalse(RunHealth.isWdaHopeless());
        assertFalse(RunHealth.isBreakerOpen());
    }

    @Test
    public void wdaHopeless_tripsAfterExactlyN_consecutiveInitFailures() {
        int n = AppConstants.WDA_HOPELESS_AFTER;
        assertTrue(n >= 2, "WDA_HOPELESS_AFTER should be >= 2");

        for (int i = 0; i < n - 1; i++) {
            RunHealth.recordInitFailure();
            assertFalse(RunHealth.isWdaHopeless(),
                    "must stay healthy at " + (i + 1) + "/" + n + " init failures");
        }
        RunHealth.recordInitFailure(); // Nth failure
        assertTrue(RunHealth.isWdaHopeless(), "must be WDA-hopeless at exactly " + n + " failures");
        assertTrue(RunHealth.shouldFastSkip(), "WDA-hopeless implies fast-skip");
    }

    @Test
    public void initSuccess_resets_theFailureStreak() {
        for (int i = 0; i < AppConstants.WDA_HOPELESS_AFTER - 1; i++) {
            RunHealth.recordInitFailure();
        }
        RunHealth.recordInitSuccess(); // a healthy init breaks the streak
        for (int i = 0; i < AppConstants.WDA_HOPELESS_AFTER - 1; i++) {
            RunHealth.recordInitFailure();
        }
        assertFalse(RunHealth.isWdaHopeless(),
                "a success between failures must prevent the hopeless trip");
    }

    @Test
    public void breaker_trip_flips_runHealthGate() {
        assertFalse(RunHealth.isBreakerOpen());
        RunHealth.tripBreaker("self-test");
        assertTrue(RunHealth.isBreakerOpen(), "tripBreaker must open the shared gate");
        assertTrue(RunHealth.shouldFastSkip(), "an open breaker implies fast-skip in @BeforeMethod");
    }

    /**
     * THE regression guard: a wedged-but-alive WDA (liveness probe == true) whose tests
     * fail with init-timeout signatures must still trip the breaker after N consecutive
     * outcomes. Before the {@code looksDead} broadening this stayed CLOSED forever and the
     * job ran to the 6h cap.
     */
    @Test
    public void breaker_trips_onWedgeSignature_withLiveProbe() {
        DeadSessionCircuitBreaker breaker = new DeadSessionCircuitBreaker(true, 3, () -> true /* alive */);
        IInvokedMethod m = fakeTestMethod(true);

        Throwable wedge = new RuntimeException("Failed to initialize driver",
                new RuntimeException("Could not start a new session",
                        new RuntimeException("Request timeout to 127.0.0.1/127.0.0.1:4723 after 90000 ms")));

        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, wedge));
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, wedge));
        breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, wedge)); // still closed at 2/3
        breaker.afterInvocation(m, fakeResult(ITestResult.FAILURE, wedge));  // 3rd consecutive wedge
        assertThrows(SkipException.class,
                () -> breaker.beforeInvocation(m, fakeResult(ITestResult.FAILURE, wedge)));
    }

    // ---- Proxy-built TestNG fakes (only the members the breaker reads) ----

    private static ITestResult fakeResult(int status, Throwable throwable) {
        return (ITestResult) Proxy.newProxyInstance(
                RunHealthSelfTest.class.getClassLoader(),
                new Class<?>[]{ITestResult.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getStatus": return status;
                        case "getThrowable": return throwable;
                        case "isSuccess": return status == ITestResult.SUCCESS;
                        case "equals": return proxy == args[0];
                        case "hashCode": return System.identityHashCode(proxy);
                        case "toString": return "fake-ITestResult";
                        default: return primitiveDefault(method.getReturnType());
                    }
                });
    }

    private static IInvokedMethod fakeTestMethod(boolean isTestMethod) {
        return (IInvokedMethod) Proxy.newProxyInstance(
                RunHealthSelfTest.class.getClassLoader(),
                new Class<?>[]{IInvokedMethod.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isTestMethod": return isTestMethod;
                        case "isConfigurationMethod": return !isTestMethod;
                        case "equals": return proxy == args[0];
                        case "hashCode": return System.identityHashCode(proxy);
                        case "toString": return "fake-IInvokedMethod";
                        default: return primitiveDefault(method.getReturnType());
                    }
                });
    }

    private static Object primitiveDefault(Class<?> returnType) {
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
