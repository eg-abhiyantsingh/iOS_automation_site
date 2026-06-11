package com.egalvanic.utils;

import java.util.function.BooleanSupplier;

/**
 * Condition-based polling — the replacement for fixed Thread.sleep waits.
 *
 * Instead of  Thread.sleep(3000)  (always pays 3s), write
 *   Waits.until(() -> page.isThingVisible(), 3000)
 * which returns the moment the condition is true (typically 100-500ms)
 * and only pays the full timeout when the condition never holds.
 */
public final class Waits {

    public static final long DEFAULT_POLL_MS = 250;

    private Waits() {
    }

    /**
     * Poll {@code condition} every {@link #DEFAULT_POLL_MS} until it returns true
     * or {@code timeoutMs} elapses.
     *
     * @return true if the condition held within the timeout
     */
    public static boolean until(BooleanSupplier condition, long timeoutMs) {
        return until(condition, timeoutMs, DEFAULT_POLL_MS);
    }

    /**
     * Poll {@code condition} every {@code pollMs} until it returns true or
     * {@code timeoutMs} elapses. Exceptions thrown by the condition are treated
     * as "not yet" (so callers can probe elements without try/catch noise).
     *
     * @return true if the condition held within the timeout
     */
    public static boolean until(BooleanSupplier condition, long timeoutMs, long pollMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            try {
                if (condition.getAsBoolean()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // condition not evaluable yet — treat as false and keep polling
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(Math.min(pollMs, Math.max(1, deadline - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * Poll until {@code condition} becomes FALSE (e.g. spinner gone, sheet dismissed).
     *
     * @return true if the condition stopped holding within the timeout
     */
    public static boolean untilGone(BooleanSupplier condition, long timeoutMs) {
        return until(() -> !condition.getAsBoolean(), timeoutMs, DEFAULT_POLL_MS);
    }
}
