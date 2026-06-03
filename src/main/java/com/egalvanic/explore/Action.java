package com.egalvanic.explore;

/** An action the engine performs on a {@link UiNode}: tap, type (fuzz), or none. */
public record Action(UiNode node, String kind, String input) {
    public static Action tap(UiNode n)            { return new Action(n, "tap", null); }
    public static Action type(UiNode n, String s) { return new Action(n, "type", s); }
    public static Action none()                   { return new Action(null, "none", null); }
    public boolean isNone() { return node == null; }
}
