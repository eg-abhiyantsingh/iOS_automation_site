package com.egalvanic.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The suite's SINGLE annotation transformer: hard per-test runtime cap
 * + suite-wide environmental-retry wiring.
 *
 * WHY ONE CLASS: TestNG honors only ONE IAnnotationTransformer — registering
 * a second logs "AnnotationTransformer already set" and silently drops one of
 * them (observed live 2026-08-06: retries never fired on CI run 31108181084
 * while a separate EnvironmentRetryTransformer was registered alongside this
 * class). Both concerns MUST live in this one transformer.
 *
 * Timeout: the suite-level time-out in the testng XMLs is not reliably
 * enforced in all execution modes, which is how individual tests have run
 * 10-15 minutes in CI (compounding implicit-wait fallbacks on a dead/blank
 * screen). Every @Test without an explicit timeOut gets a default cap so a
 * wedged test fails in minutes instead of eating the module's budget.
 * Default: 360s. Override per run with -DTEST_TIMEOUT_SECONDS=n (or env var);
 * explicit @Test(timeOut=...) values always win.
 *
 * Retry: every @Test without its own retryAnalyzer gets
 * {@link EnvironmentRetryAnalyzer} (one retry, environmental signatures only).
 *
 * Note: TestNG runs capped methods on a worker thread — safe here because
 * DriverManager intentionally uses a static volatile driver, not ThreadLocal.
 */
public class GlobalTestTimeout implements IAnnotationTransformer {

    private static final long DEFAULT_TIMEOUT_MS = resolveTimeoutMs();

    private static long resolveTimeoutMs() {
        String v = System.getProperty("TEST_TIMEOUT_SECONDS");
        if (v == null || v.isEmpty()) {
            v = System.getenv("TEST_TIMEOUT_SECONDS");
        }
        try {
            if (v != null && !v.isEmpty()) {
                return Long.parseLong(v.trim()) * 1000L;
            }
        } catch (NumberFormatException ignored) {
        }
        return 360_000L;
    }

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (annotation.getTimeOut() <= 0) {
            annotation.setTimeOut(DEFAULT_TIMEOUT_MS);
        }
        Class<?> existing = annotation.getRetryAnalyzerClass();
        if (existing == null || existing == DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(EnvironmentRetryAnalyzer.class);
        }
    }
}
