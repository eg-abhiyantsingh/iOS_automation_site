package com.egalvanic.explore;

import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Discovers interactable elements on the current screen via the XCUITest element tree. */
public final class Crawler {

    private static final String INTERACTABLE =
            "visible == 1 AND enabled == 1 AND ("
          + "type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeCell' OR "
          + "type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSecureTextField' OR "
          + "type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeMenuItem')";

    private IOSDriver driver() { return DriverManager.getDriver(); }

    public List<UiNode> scan() {
        List<UiNode> nodes = new ArrayList<>();
        List<WebElement> els;
        try {
            els = driver().findElements(AppiumBy.iOSNsPredicateString(INTERACTABLE));
        } catch (Exception e) {
            return nodes; // unresponsive tree => empty; engine treats as dead-end
        }
        for (WebElement e : els) {
            try {
                String type = e.getAttribute("type");
                String name = e.getAttribute("name");
                String label = e.getAttribute("label");
                boolean editable = type != null && type.contains("TextField");
                nodes.add(new UiNode(e, type, name, label, editable));
            } catch (Exception ignored) { /* stale element; skip */ }
        }
        return nodes;
    }

    /** Coarse, stable identity for the current screen (used for the workflow graph). */
    public String screenSignature() {
        try {
            return scan().stream().map(UiNode::identity).distinct().sorted()
                    .collect(Collectors.joining("|"));
        } catch (Exception e) {
            return "?";
        }
    }
}
