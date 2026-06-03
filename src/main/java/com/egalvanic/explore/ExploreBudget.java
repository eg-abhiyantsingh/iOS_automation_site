package com.egalvanic.explore;

/** Bounds the crawl by both wall-clock and step count, so it is safe to run in CI. */
public record ExploreBudget(int maxSteps, long deadlineMillis) {
    public static ExploreBudget of(int minutes, int maxSteps) {
        return new ExploreBudget(maxSteps, System.currentTimeMillis() + minutes * 60_000L);
    }
    public boolean exhausted(int steps) {
        return steps >= maxSteps || System.currentTimeMillis() >= deadlineMillis;
    }
}
