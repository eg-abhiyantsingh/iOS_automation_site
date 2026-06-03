package com.egalvanic.explore;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Records observed {@code screen --action--> screen} transitions. Inbound-less screens
 * (reachable only via an unrealistic step order) are the "flows that only work in an
 * unrealistic order" the brief calls out — they surface as targets with no normal path.
 */
public final class WorkflowGraph {

    private final Map<String, Set<String>> edges = new LinkedHashMap<>();
    private final Set<String> reachedTargets = new LinkedHashSet<>();

    public void record(String fromScreen, String action, String toScreen) {
        String key = sig(fromScreen) + " --[" + action + "]--> " + sig(toScreen);
        edges.computeIfAbsent(sig(fromScreen), k -> new LinkedHashSet<>()).add(key);
        reachedTargets.add(sig(toScreen));
    }

    public int transitionCount() {
        return edges.values().stream().mapToInt(Set::size).sum();
    }

    public String render() {
        StringBuilder sb = new StringBuilder("Workflow transitions: ").append(transitionCount()).append('\n');
        edges.values().forEach(set -> set.forEach(e -> sb.append("  ").append(e).append('\n')));
        return sb.toString();
    }

    /** Short, log-friendly screen id (the raw signature can be very long). */
    private static String sig(String s) {
        if (s == null || s.isBlank()) return "∅";
        return "scr#" + Integer.toHexString(s.hashCode());
    }
}
