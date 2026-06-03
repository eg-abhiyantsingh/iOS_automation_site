package com.egalvanic.explore;

import org.openqa.selenium.WebElement;

/** One interactable element discovered by the {@link Crawler} on the current screen. */
public final class UiNode {
    public final WebElement element;
    public final String type;
    public final String name;
    public final String label;
    public final boolean editable;

    public UiNode(WebElement element, String type, String name, String label, boolean editable) {
        this.element = element;
        this.type = type == null ? "?" : type;
        this.name = name;
        this.label = label;
        this.editable = editable;
    }

    public String identity() {
        String id = (name != null && !name.isBlank()) ? name : (label != null ? label : "");
        return type + ":" + id;
    }

    public String describe() {
        String id = (name != null && !name.isBlank()) ? name : (label != null ? label : "<unnamed>");
        return type.replace("XCUIElementType", "") + "['" + id + "']";
    }
}
