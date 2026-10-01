package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DISCOVERY PROBE for ZP-3927 / ZP-3928 — NOT a test case (no TC_ id, never in a CI suite).
 *
 * Captures one page-source snapshot per surface the ZP-392x tests depend on and writes them to
 * {@code /tmp/zp/probe/<step>.xml}, plus a compact label census on stdout. The point is to write
 * locators against the DOM the build actually renders instead of the strings we hope it renders —
 * the same "capture app truth first" rule that reshaped TC_IND_03 in changelog 165.
 *
 * Every step is best-effort and never aborts the run: a surface that does not open is recorded as
 * such, which is itself a finding.
 */
public final class ZP392x_SurfaceProbe_Test extends BaseTest {

    private static final Path OUT = Paths.get("/tmp/zp/probe");

    private IssuePage issuePage;
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    // ── capture helpers ─────────────────────────────────────────────────────

    private void dump(String step) {
        try {
            Files.createDirectories(OUT);
            String xml = DriverManager.getDriver().getPageSource();
            Files.write(OUT.resolve(step + ".xml"), xml.getBytes(StandardCharsets.UTF_8));
            System.out.println("──── PROBE " + step + " ──── (" + xml.length() + " chars)");
            census(xml);
        } catch (IOException | RuntimeException e) {
            System.out.println("──── PROBE " + step + " FAILED: " + e.getMessage());
        }
    }

    /** Visible, labelled Buttons / StaticTexts / Cells / Others in document order, de-duplicated. */
    private static void census(String xml) {
        Matcher m = Pattern.compile(
                "<XCUIElementType(Button|StaticText|Cell|Other|NavigationBar|SearchField|TextField|Image|Switch)\\b([^>]*)")
                .matcher(xml);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String attrs = m.group(2);
            if (!attrs.contains("visible=\"true\"")) continue;
            String label = attr(attrs, "label");
            String name  = attr(attrs, "name");
            String value = attr(attrs, "value");
            String text  = !label.isEmpty() ? label : name;
            if (text.isEmpty() && value.isEmpty()) continue;
            String y = attr(attrs, "y");
            String line = m.group(1) + "  y=" + y + "  «" + text + "»"
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

    private void swipeDown() {
        try {
            DriverManager.getDriver().executeScript("mobile: scroll", Map.of("direction", "down"));
            sleep(500);
        } catch (Exception ignored) { }
    }

    private boolean tapVisible(String... labels) {
        for (String l : labels) {
            try {
                List<WebElement> els = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "visible == 1 AND (label ==[c] '" + l + "' OR name ==[c] '" + l + "')"));
                if (!els.isEmpty()) { els.get(0).click(); sleep(700); return true; }
            } catch (Exception ignored) { }
        }
        return false;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    // ── the probe ───────────────────────────────────────────────────────────

    @Test
    public void probeIssuesSurfaces() {
        loginAndSelectSite();

        step("issues list", () -> {
            if (!issues().navigateToIssuesScreen()) { System.out.println("Issues screen did not open"); return; }
            issues().tapAllTab();
            sleep(600);
            dump("01_issues_all");
        });

        step("my session tab", () -> {
            issues().tapMySessionTab();
            sleep(700);
            dump("02_issues_my_session");
        });

        step("long-press on My Session row", () -> {
            if (issues().longPressFirstIssueRow()) { sleep(500); dump("03_longpress_menu_my_session"); }
            issues().dismissContextMenu();
            sleep(400);
        });

        step("long-press on All row", () -> {
            issues().tapAllTab(); sleep(600);
            if (issues().longPressFirstIssueRow()) { sleep(500); dump("04_longpress_menu_all"); }
            issues().dismissContextMenu();
            sleep(400);
        });

        step("issue details", () -> {
            issues().tapAllTab(); sleep(500);
            if (!issues().tapFirstIssue()) { System.out.println("no issue to open"); return; }
            sleep(800);
            dump("05_issue_details_top");
            swipeDown(); dump("06_issue_details_scroll1");
            swipeDown(); dump("07_issue_details_scroll2");
            swipeDown(); dump("08_issue_details_scroll3");
        });

        step("gallery / add-photo action sheet", () -> {
            issues().tapGalleryButton();
            sleep(900);
            dump("09_after_gallery_tap");
            // If an action sheet offered a library option, open it and record the picker, then cancel.
            if (tapVisible("Choose from Library", "Photo Library")) { sleep(1200); dump("10_photo_picker"); }
            tapVisible("Cancel");
            sleep(400);
            tapVisible("Cancel");
        });

        step("FLIR entry point from issue details", () -> {
            if (tapVisible("From FLIR Camera", "FLIR Camera", "Connect FLIR Camera", "From Camera")) {
                sleep(1000); dump("11_flir_entry");
                tapVisible("Cancel", "Done", "Close");
            } else {
                System.out.println("   no FLIR entry visible on issue details / sheet");
            }
        });

        step("back to list", () -> {
            tapVisible("Cancel"); tapVisible("Back"); tapVisible("Issues");
            sleep(500);
        });
    }

    @Test(dependsOnMethods = "probeIssuesSurfaces", alwaysRun = true)
    public void probeAssetIssuesAndCopyTo() {
        loginAndSelectSite();
        step("asset details → Issues section", () -> {
            assetPage.navigateToAssetListTurbo();
            String name = assetPage.openSharedAssetForEditOrFallback(null);
            System.out.println("opened asset: " + name);
            if (!assetPage.isEditAssetScreenDisplayed()) { System.out.println("asset details did not open"); return; }
            dump("20_asset_details_top");
            assetPage.scrollToIssuesSection();
            sleep(600);
            dump("21_asset_issues_section");
        });
        step("Copy Data To picker", () -> {
            if (assetPage.tapCopyTo()) { sleep(800); dump("22_copy_data_to_picker"); tapVisible("Cancel", "Done", "Close"); }
            else System.out.println("   Copy Data To not reachable");
        });
    }

    @Test(dependsOnMethods = "probeAssetIssuesAndCopyTo", alwaysRun = true)
    public void probeSessionPhotoEntryPoints() {
        loginAndSelectSite();
        step("work order → session details (IR / photos card)", () -> {
            // The Work Orders screen is reached from the dashboard; record whatever the first
            // available session shows so the FLIR/photo entry points can be located.
            if (tapVisible("Work Orders", "Sessions")) { sleep(900); dump("30_work_orders_list"); }
            List<WebElement> cards = new ArrayList<>(DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND (label CONTAINS 'QA-WT' OR label CONTAINS 'Work Order -')")));
            if (!cards.isEmpty()) {
                cards.get(0).click(); sleep(1200); dump("31_session_details");
                swipeDown(); dump("32_session_details_scroll1");
                if (tapVisible("IR", "IR Photos", "Photos")) { sleep(900); dump("33_session_ir_tab"); }
            } else {
                System.out.println("   no work-order card visible");
            }
        });
    }
}
