package com.egalvanic.tests;

import com.egalvanic.base.WorkTypeBaseTest;
import com.egalvanic.constants.WorkTypeCatalog;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DISCOVERY PROBE #2 for ZP-3927 / ZP-3928 — NOT a test case (no TC_ id, never in a CI suite).
 *
 * Probe #1 showed the Issues-screen rows are unlabelled clusters and a handle-based long-press
 * raised no menu. This probe rides a QA-WT fixture session (WorkTypeBaseTest self-provisions it)
 * to capture the surfaces that live INSIDE a work order: the session Issues tab and its
 * long-press menu, the IR / photos card, the FLIR camera entry point, and photo categories.
 * It also re-tries the Issues-screen long-press with the new coordinate press for comparison.
 */
public final class ZP392x_SessionProbe_Test extends WorkTypeBaseTest {

    private static final Path OUT = Paths.get("/tmp/zp/probe2");

    private IssuePage issuePage;
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    private void dump(String step) {
        try {
            Files.createDirectories(OUT);
            String xml = DriverManager.getDriver().getPageSource();
            Files.write(OUT.resolve(step + ".xml"), xml.getBytes(StandardCharsets.UTF_8));
            System.out.println("──── PROBE2 " + step + " ──── (" + xml.length() + " chars)");
            census(xml);
        } catch (IOException | RuntimeException e) {
            System.out.println("──── PROBE2 " + step + " FAILED: " + e.getMessage());
        }
    }

    private static void census(String xml) {
        Matcher m = Pattern.compile(
                "<XCUIElementType(Button|StaticText|Cell|Other|NavigationBar|SearchField|TextField|Image|Switch|Sheet|Alert)\\b([^>]*)")
                .matcher(xml);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String attrs = m.group(2);
            if (!attrs.contains("visible=\"true\"")) continue;
            String label = attr(attrs, "label"), name = attr(attrs, "name"), value = attr(attrs, "value");
            String text = !label.isEmpty() ? label : name;
            if (text.isEmpty() && value.isEmpty()) continue;
            String line = m.group(1) + "  y=" + attr(attrs, "y") + "  «" + text + "»"
                    + (value.isEmpty() || value.equals(text) ? "" : "  value=«" + value + "»");
            if (seen.add(line)) System.out.println("   " + line);
        }
    }

    private static String attr(String attrs, String key) {
        Matcher a = Pattern.compile(" " + key + "=\"([^\"]*)\"").matcher(attrs);
        return a.find() ? a.group(1) : "";
    }

    private static void step(String what, Runnable r) {
        try { r.run(); } catch (Exception e) { System.out.println("   (step '" + what + "' threw: " + e.getMessage() + ")"); }
    }

    private boolean tapVisible(String... labels) {
        for (String l : labels) {
            try {
                List<WebElement> els = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "visible == 1 AND (label ==[c] '" + l + "' OR name ==[c] '" + l + "')"));
                if (!els.isEmpty()) { els.get(0).click(); pause(800); return true; }
            } catch (Exception ignored) { }
        }
        return false;
    }

    private void swipe() {
        try { DriverManager.getDriver().executeScript("mobile: scroll", Map.of("direction", "down")); pause(500); } catch (Exception ignored) { }
    }

    private static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    @Test
    public void probeSessionIssuesAndPhotos() {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "PROBE2");
        step("session details", () -> {
            wo.ensureSessionDetailsOpen();
            pause(600);
            dump("40_session_details");
            swipe(); dump("41_session_details_scroll");
        });

        step("session Issues tab + long-press", () -> {
            boolean tab = wo.tapSessionTab("Issues");
            System.out.println("Issues tab tapped: " + tab);
            pause(900);
            dump("42_session_issues_tab");
            if (issues().longPressFirstIssueRow()) { pause(600); dump("43_session_issue_longpress_menu"); }
            issues().cancelSheetIfOpen();
            issues().dismissContextMenu();
            pause(400);
        });

        step("session IR tab / photos entry", () -> {
            for (String t : new String[]{"IR", "IR Photos", "Photos"}) {
                if (wo.tapSessionTab(t)) { pause(900); dump("44_session_tab_" + t.replace(' ', '_')); break; }
            }
            if (tapVisible("From FLIR Camera", "Connect FLIR Camera", "FLIR Camera", "Import")) {
                pause(1000); dump("45_flir_entry_sheet");
                tapVisible("Cancel", "Close", "Done");
            } else {
                System.out.println("   no FLIR entry visible on this tab");
            }
        });

        step("session Assets → room → asset row → Add Photos", () -> {
            if (!wo.tapSessionTab("Assets")) return;
            pause(700);
            dump("46_session_assets_tab");
            // A room row navigates to 'Assets in Room' (v1.55 contract); the first asset opens forms.
            List<WebElement> rooms = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label CONTAINS ' assets'"));
            if (!rooms.isEmpty()) {
                rooms.get(0).click(); pause(900); dump("47_assets_in_room");
                if (tapVisible("Add Photos", "Photos", "camera")) { pause(900); dump("48_add_photos_screen"); tapVisible("Cancel", "Done", "Back"); }
            }
        });
    }

    @Test(dependsOnMethods = "probeSessionIssuesAndPhotos", alwaysRun = true)
    public void probeIssuesScreenLongPressByCoordinates() {
        loginAndSelectSite();
        step("Issues screen → All → coordinate long-press", () -> {
            if (!issues().navigateToIssuesScreen()) return;
            issues().tapAllTab(); pause(600);
            if (issues().longPressFirstIssueRow()) { pause(600); dump("50_issues_all_longpress_coord"); }
            issues().cancelSheetIfOpen(); issues().dismissContextMenu(); pause(400);
        });
        step("Issues screen → My Session → coordinate long-press", () -> {
            if (issues().isMySessionTabVisible()) {
                issues().tapMySessionTab(); pause(700);
                if (issues().longPressFirstIssueRow()) { pause(600); dump("51_issues_mysession_longpress_coord"); }
                issues().cancelSheetIfOpen(); issues().dismissContextMenu();
            }
        });
    }
}
