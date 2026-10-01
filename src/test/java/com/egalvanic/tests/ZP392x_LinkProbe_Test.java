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
 * DISCOVERY PROBE #3 for ZP-3927 / ZP-3928 — NOT a test case (no TC_ id, never in a CI suite).
 *
 * Probe #2 found the QA-WT04 session's Issues tab EMPTY ("No Issues" / "Manage Issues" / "Add"), so
 * "Unlink Issue" had nothing to act on, and the IR / FLIR surfaces were not among the five visible
 * session tabs (Details · Assets · Forms · Issues · More). This probe:
 *   1. links an existing issue through "Manage Issues" and records the picker, the populated
 *      session-issues list, and the long-press menu on a linked row;
 *   2. opens the "More" tab and follows any IR / photo / FLIR entry it exposes;
 *   3. records the asset-details Issues section now that the header can be scrolled to directly.
 */
public final class ZP392x_LinkProbe_Test extends WorkTypeBaseTest {

    private static final Path OUT = Paths.get("/tmp/zp/probe3");

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
            System.out.println("──── PROBE3 " + step + " ──── (" + xml.length() + " chars)");
            census(xml);
        } catch (IOException | RuntimeException e) {
            System.out.println("──── PROBE3 " + step + " FAILED: " + e.getMessage());
        }
    }

    private static void census(String xml) {
        Matcher m = Pattern.compile(
                "<XCUIElementType(Button|StaticText|Cell|Other|NavigationBar|SearchField|TextField|Image|Switch|Sheet|Alert|MenuItem)\\b([^>]*)")
                .matcher(xml);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String attrs = m.group(2);
            if (!attrs.contains("visible=\"true\"")) continue;
            String label = attr(attrs, "label"), name = attr(attrs, "name"), value = attr(attrs, "value");
            String text = !label.isEmpty() ? label : name;
            if (text.isEmpty() && value.isEmpty()) continue;
            if (text.contains("scroll bar")) continue;
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
                        "visible == 1 AND (label ==[c] '" + l + "' OR name ==[c] '" + l + "' OR label BEGINSWITH '" + l + "')"));
                if (!els.isEmpty()) { els.get(0).click(); pause(900); return true; }
            } catch (Exception ignored) { }
        }
        return false;
    }

    private static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    @Test
    public void probeLinkThenUnlinkInSession() {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "PROBE3");
        step("session Issues tab", () -> {
            wo.ensureSessionDetailsOpen();
            wo.tapSessionTab("Issues");
            pause(900);
            dump("60_session_issues_before");
        });
        step("Manage Issues → link picker", () -> {
            if (!tapVisible("Manage Issues", "Link Issues")) { System.out.println("   no Manage Issues control"); return; }
            pause(1000);
            dump("61_manage_issues_picker");
            // Select the first issue row in the picker (title texts contain ' on ') and confirm.
            List<WebElement> titles = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label CONTAINS ' on '"));
            if (!titles.isEmpty()) {
                org.openqa.selenium.Rectangle r = titles.get(0).getRect();
                DriverManager.getDriver().executeScript("mobile: tap", Map.of("x", r.x + 40, "y", r.y + r.height / 2));
                pause(600);
                dump("62_picker_after_select");
                tapVisible("Link", "Done", "Save", "Link Issues", "Add");
                pause(1200);
            } else {
                System.out.println("   picker shows no issue titles");
                tapVisible("Cancel", "Done", "Close");
            }
            dump("63_session_issues_after_link");
        });
        step("long-press a linked session issue row", () -> {
            if (issues().longPressFirstIssueRow()) { pause(700); dump("64_session_issue_longpress_menu"); }
            issues().cancelSheetIfOpen();
            issues().dismissContextMenu();
            pause(400);
            dump("65_after_dismiss");
        });
        step("More tab → IR / photos / FLIR", () -> {
            if (!wo.tapSessionTab("More")) { System.out.println("   no More tab"); return; }
            pause(900);
            dump("66_session_more_tab");
            if (tapVisible("IR", "IR Photos", "Infrared", "Photos", "Thermal")) { pause(1000); dump("67_more_ir_screen"); }
            if (tapVisible("From FLIR Camera", "Connect FLIR Camera", "FLIR Camera", "Import", "Add")) { pause(1200); dump("68_flir_or_add_sheet"); }
            if (tapVisible("From FLIR Camera", "Connect FLIR Camera", "FLIR Camera")) { pause(1200); dump("69_flir_connect_sheet"); }
            tapVisible("Cancel", "Close", "Done");
            pause(400);
            tapVisible("Cancel", "Close", "Done");
        });
    }

    @Test(dependsOnMethods = "probeLinkThenUnlinkInSession", alwaysRun = true)
    public void probeAssetIssuesSection() {
        loginAndSelectSite();
        step("asset details → Issues section (predicate scroll)", () -> {
            assetPage.navigateToAssetListTurbo();
            assetPage.openSharedAssetForEditOrFallback(null);
            if (!assetPage.isEditAssetScreenDisplayed()) { System.out.println("asset details did not open"); return; }
            assetPage.scrollToIssuesSection();
            pause(700);
            dump("70_asset_issues_section");
            try { DriverManager.getDriver().executeScript("mobile: scroll", Map.of("direction", "down")); pause(500); } catch (Exception ignored) { }
            dump("71_asset_issues_section_scroll");
        });
    }
}
