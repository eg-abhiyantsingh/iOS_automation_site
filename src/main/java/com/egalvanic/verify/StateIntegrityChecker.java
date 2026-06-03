package com.egalvanic.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Captures a list's identity set before and after a multi-step flow and asserts no
 * loss, duplication, or wrong-result. This is the bug class behind "data corruption
 * across flows" — which today's suite never checks (0 before/after captures).
 *
 * <p>Driver-free by design so it can be unit-tested on a plain JVM.
 */
public final class StateIntegrityChecker {

    /** Immutable snapshot of a list's identities (e.g. asset names, issue titles, plan names). */
    public record Snapshot(List<String> items) {
        public int size() { return items.size(); }
        public Set<String> set() { return new HashSet<>(items); }
    }

    /** Read current state. Throws if the reader returns null — a null almost always means
     *  a swallowed failure upstream, which must not silently pass as "empty". */
    public Snapshot capture(Supplier<List<String>> reader) {
        List<String> items = reader.get();
        if (items == null) throw new VerificationError("state reader returned null (swallowed failure?)");
        return new Snapshot(new ArrayList<>(items));
    }

    public void assertCreatedExactlyOne(Snapshot before, Snapshot after, String expectedNewItem) {
        if (after.size() != before.size() + 1) {
            throw new VerificationError("Create did not add exactly one item: before=" + before.size()
                    + " after=" + after.size());
        }
        Set<String> added = after.set();
        added.removeAll(before.set());
        if (!added.contains(expectedNewItem)) {
            throw new VerificationError("Created item not found by value — expected '" + expectedNewItem
                    + "', net-new=" + added);
        }
    }

    public void assertDeletedExactlyOne(Snapshot before, Snapshot after, String expectedRemoved) {
        if (after.size() != before.size() - 1) {
            throw new VerificationError("Delete did not remove exactly one item: before=" + before.size()
                    + " after=" + after.size());
        }
        if (after.set().contains(expectedRemoved)) {
            throw new VerificationError("Deleted item '" + expectedRemoved + "' is still present");
        }
    }

    public void assertNoLossOrDup(Snapshot before, Snapshot after) {
        if (after.size() < before.size()) {
            throw new VerificationError("Data loss: " + (before.size() - after.size()) + " item(s) disappeared");
        }
        if (after.set().size() != after.items().size()) {
            throw new VerificationError("Duplication detected: " + duplicates(after.items()));
        }
    }

    public void assertUnchanged(Snapshot before, Snapshot after) {
        if (!before.items().equals(after.items())) {
            throw new VerificationError("State changed unexpectedly.\n before=" + before.items()
                    + "\n after =" + after.items());
        }
    }

    private static List<String> duplicates(List<String> items) {
        Set<String> seen = new HashSet<>();
        List<String> dups = new ArrayList<>();
        for (String s : items) if (!seen.add(s)) dups.add(s);
        return dups;
    }
}
