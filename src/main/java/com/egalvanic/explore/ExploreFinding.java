package com.egalvanic.explore;

/** A single anomaly observed during exploration, with severity for triage. */
public record ExploreFinding(Severity severity, String kind, String step, String detail) {

    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }

    public static ExploreFinding critical(String kind, String step, String detail) {
        return new ExploreFinding(Severity.CRITICAL, kind, step, detail);
    }
    public static ExploreFinding high(String kind, String step, String detail) {
        return new ExploreFinding(Severity.HIGH, kind, step, detail);
    }
    public static ExploreFinding medium(String kind, String step, String detail) {
        return new ExploreFinding(Severity.MEDIUM, kind, step, detail);
    }
    public static ExploreFinding low(String kind, String step, String detail) {
        return new ExploreFinding(Severity.LOW, kind, step, detail);
    }

    @Override public String toString() {
        return "[" + severity + "] " + kind + " @ " + step + " — " + detail;
    }
}
