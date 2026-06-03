package com.egalvanic.explore;

import java.util.List;
import java.util.Random;

/**
 * Chooses the next action. With an {@link AiClient} it asks for the highest-bug-risk
 * target and generates edge inputs; without one it is a weighted monkey with a built-in
 * fuzz dictionary. Either way it returns a concrete {@link Action}.
 */
public final class ActionSelector {

    private static final String[] FUZZ = {
            "", " ", "0", "-1", "999999999",
            "🙂🔥™", "O'Brien \"x\"", "'; DROP TABLE assets;--",
            "                 ", "a".repeat(256), "<script>alert(1)</script>"
    };

    private final AiClient ai; // nullable
    private final Random rnd = new Random();

    public ActionSelector(AiClient ai) { this.ai = ai; }

    public Action choose(List<UiNode> nodes, String screenHint) {
        if (nodes.isEmpty()) return Action.none();

        UiNode pick = (ai != null) ? aiPick(nodes, screenHint) : null;
        if (pick == null) pick = nodes.get(rnd.nextInt(nodes.size()));

        if (pick.editable) return Action.type(pick, FUZZ[rnd.nextInt(FUZZ.length)]);
        return Action.tap(pick);
    }

    private UiNode aiPick(List<UiNode> nodes, String screenHint) {
        try {
            List<String> labels = nodes.stream().map(UiNode::describe).toList();
            String chosen = ai.suggestNextActionLabel(labels, screenHint);
            if (chosen == null) return null;
            for (UiNode n : nodes) {
                if (n.describe().equalsIgnoreCase(chosen) || chosen.contains(n.describe())) return n;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
