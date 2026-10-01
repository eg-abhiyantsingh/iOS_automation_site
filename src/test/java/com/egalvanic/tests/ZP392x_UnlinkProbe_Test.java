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
 * DISCOVERY PROBE #4 for ZP-3927 §1 — NOT a test case (no TC_ id, never in a CI suite).
 *
 * Probe #3 decoded the "Link Issues" picker (rows are Buttons «title, status, asset[, Linked to: WO], date»,
 * value=1 when selected, confirm = «Update»). This probe actually links the one UNLINKED issue to the
 * QA-WT04 session, records the populated session-issues list, long-presses its row (coordinate press on
 * the title) to capture the menu, chooses "Unlink Issue" if offered, and records the confirmation and
 * the list afterwards — the complete §1 surface, end to end, on 1.63.
 */
public final class ZP392x_UnlinkProbe_Test extends WorkTypeBaseTest {

    private static final Path OUT = Paths.get("/tmp/zp/probe4");

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
            System.out.println("──── PROBE4 " + step + " ──── (" + xml.length() + " chars)");
            census(xml);
        } catch (IOException | RuntimeException e) {
            System.out.println("──── PROBE4 " + step + " FAILED: " + e.getMessage());
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

    private static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    @Test(dependsOnMethods = "probeLinkUnlinkEndToEnd", alwaysRun = true)
    public void probeImportRoutesWithoutDrive() {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "PROBE4B");
        com.egalvanic.pages.FlirImportPage flir = new com.egalvanic.pages.FlirImportPage();
        step("More › IR › Add IR Photos › Auto-Match by Name", () -> {
            wo.ensureSessionDetailsOpen();
            if (!flir.openIrScreenFromSession()) { System.out.println("   IR screen not reached"); return; }
            if (!flir.openAddIrPhotosMenu()) { System.out.println("   route popup not shown"); return; }
            dump("90_route_popup");
            flir.tapRoute(com.egalvanic.pages.FlirImportPage.ROUTE_AUTO_MATCH);
            pause(1500);
            dump("91_auto_match_no_drive");
            flir.dismiss(); pause(500); flir.dismiss();
        });
        step("Add IR Photos › Browse & Select", () -> {
            if (!flir.openAddIrPhotosMenu()) { System.out.println("   route popup not shown (2)"); return; }
            flir.tapRoute(com.egalvanic.pages.FlirImportPage.ROUTE_BROWSE);
            pause(1500);
            dump("92_browse_select_picker");
            flir.dismiss(); pause(500); flir.dismiss();
        });
        step("Add IR Photos › From Photos", () -> {
            if (!flir.openAddIrPhotosMenu()) { System.out.println("   route popup not shown (3)"); return; }
            flir.tapRoute(com.egalvanic.pages.FlirImportPage.ROUTE_FROM_PHOTOS);
            pause(2500);
            dump("93_from_photos_picker");
            flir.dismiss(); pause(500); flir.dismiss();
        });
    }

    @Test
    public void probeLinkUnlinkEndToEnd() {
        openFixtureOrSkip(WorkTypeCatalog.CLEAN_TIGHTEN_TORQUE, "PROBE4");
        step("session Issues tab", () -> {
            wo.ensureSessionDetailsOpen();
            wo.tapSessionTab("Issues");
            pause(900);
            dump("80_session_issues_before");
        });
        step("link the unlinked issue via Manage Issues → Update", () -> {
            if (!issues().tapTextIfPresent("Manage Issues")) { System.out.println("   no Manage Issues"); return; }
            pause(1000);
            // Rows are Buttons; the unlinked one has no 'Linked to:' in its composite label.
            List<WebElement> rows = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label CONTAINS ' on ' AND NOT (label CONTAINS 'Linked to:')"));
            System.out.println("   unlinked candidate rows: " + rows.size());
            if (rows.isEmpty()) { issues().cancelSheetIfOpen(); return; }
            rows.get(0).click();
            pause(600);
            dump("81_picker_selected");
            boolean update = issues().tapTextIfPresent("Update");
            System.out.println("   Update tapped: " + update);
            pause(1500);
            dump("82_session_issues_after_update");
        });
        step("long-press the linked row in the session list", () -> {
            if (issues().longPressFirstIssueRow()) { pause(800); dump("83_session_row_longpress_menu"); }
            boolean offered = issues().isUnlinkIssueOffered();
            System.out.println("   'Unlink Issue' offered: " + offered);
            if (offered) {
                issues().tapUnlinkIssue();
                pause(700);
                dump("84_after_tap_unlink");
                boolean confirmed = issues().confirmUnlinkIssueIfAsked();
                System.out.println("   confirmation handled: " + confirmed);
                pause(1200);
                dump("85_session_issues_after_unlink");
            } else {
                issues().cancelSheetIfOpen();
                issues().dismissContextMenu();
                pause(500);
                dump("86_no_menu_state");
                // Fallback: does a swipe-left on the row reveal an unlink action?
                try {
                    List<WebElement> titles = DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label CONTAINS ' on '"));
                    if (!titles.isEmpty()) {
                        org.openqa.selenium.Rectangle r = titles.get(0).getRect();
                        DriverManager.getDriver().executeScript("mobile: dragFromToForDuration", Map.of(
                                "fromX", r.x + 300, "fromY", r.y + r.height / 2, "toX", r.x + 40, "toY", r.y + r.height / 2, "duration", 0.3));
                        pause(700);
                        dump("87_after_swipe_left");
                    }
                } catch (Exception e) { System.out.println("   swipe probe: " + e.getMessage()); }
            }
        });
    }
}
