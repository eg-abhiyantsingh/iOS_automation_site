package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import org.testng.annotations.Test;

/**
 * TEMPORARY diagnostic — v1.50 Interrupting Rating dropdown + Save-button probe.
 * Not part of any suite; run explicitly with -Dtest=DebugProbe_Test.
 */
public class DebugProbe_Test extends BaseTest {

    @Test
    public void PROBE_interruptingRatingAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - Interrupting Rating v1.50 anatomy"
        );

        loginAndSelectSite();
        assetPage.openSharedAssetForEditOrFallback(null);
        assetPage.changeAssetClassToDisconnectSwitch();
        assetPage.scrollFormDown();
        shortWait();

        assetPage.debugProbeRatingAndSave("10 kA",
            "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad");
    }

    @Test
    public void PROBE_workOrdersAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - v1.50 Work Orders screen + details anatomy (ZP-3109/3054)"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        mediumWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/wo-list.xml"), d.getPageSource());
            System.out.println("PROBE: WO list dumped");
        } catch (Exception e) { System.out.println("PROBE: wo-list dump failed: " + e.getMessage()); }

        // Tap the first work-order row under MANUAL alerts — autoAcceptAlerts
        // races the 'Start Work Order?' confirmation away (probe run 3).
        boolean pausedAlerts = false;
        try {
            d.setSetting("defaultAlertAction", "");
            pausedAlerts = true;
        } catch (Exception e) { System.out.println("PROBE: could not pause alerts: " + e.getMessage()); }
        try {
            org.openqa.selenium.WebElement row = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Order' AND visible == 1"));
            org.openqa.selenium.Rectangle r = row.getRect();
            System.out.println("PROBE: pressing WO row '" + row.getAttribute("name") + "' at ("
                + (r.x + 40) + "," + (r.y + r.height / 2) + ")");
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
        } catch (Exception e) {
            System.out.println("PROBE: WO row press failed: " + e.getMessage());
        }
        longWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/wo-details.xml"), d.getPageSource());
            System.out.println("PROBE: WO details dumped");
        } catch (Exception e) { System.out.println("PROBE: wo-details dump failed: " + e.getMessage()); }

        // v1.50: row tap raises 'Start Work Order?' — confirm it and dump Session Details
        try {
            org.openqa.selenium.WebElement startBtn = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == 'Start Work Order' AND visible == 1"));
            org.openqa.selenium.Rectangle r = startBtn.getRect();
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            System.out.println("PROBE: confirmed Start Work Order alert");
        } catch (Exception e) {
            System.out.println("PROBE: no Start alert visible: " + e.getMessage());
        } finally {
            if (pausedAlerts) {
                try { d.setSetting("defaultAlertAction", "accept"); } catch (Exception ignored) {}
            }
        }
        longWait();
        longWait();
        // Start activated the WO but stayed on the list — tap the now-active
        // row AGAIN to open Session Details (probe run 4: list re-rendered,
        // WO count bumped, no Session screen from the confirm alone).
        try {
            org.openqa.selenium.WebElement row2 = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Work Order' AND visible == 1"));
            org.openqa.selenium.Rectangle r2 = row2.getRect();
            System.out.println("PROBE: re-tapping active WO row '" + row2.getAttribute("name") + "'");
            d.executeScript("mobile: tap", java.util.Map.of("x", r2.x + 40, "y", r2.y + r2.height / 2));
        } catch (Exception e) { System.out.println("PROBE: active-row re-tap failed: " + e.getMessage()); }
        longWait();
        longWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/wo-session-details.xml"), d.getPageSource());
            System.out.println("PROBE: Session Details dumped");
        } catch (Exception e) { System.out.println("PROBE: session dump failed: " + e.getMessage()); }

        // ZP-3054: hunt the More Actions / ellipsis affordance and open it
        try {
            org.openqa.selenium.WebElement more = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (name CONTAINS[c] 'More' OR name CONTAINS 'ellipsis') AND visible == 1"));
            org.openqa.selenium.Rectangle r = more.getRect();
            System.out.println("PROBE: pressing More button '" + more.getAttribute("name") + "'");
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            mediumWait();
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/wo-more-menu.xml"), d.getPageSource());
            System.out.println("PROBE: More menu dumped");
        } catch (Exception e) { System.out.println("PROBE: More Actions hunt failed: " + e.getMessage()); }
    }

    @Test
    public void PROBE_createWOForm() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - v1.50 Start New Work Order form (ZP-3109 priority picker)"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        siteSelectionPage.clickWorkOrderCard();
        mediumWait();
        try {
            org.openqa.selenium.WebElement create = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Start New Work Order' AND visible == 1"));
            org.openqa.selenium.Rectangle r = create.getRect();
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
            System.out.println("PROBE: pressed Start New Work Order");
        } catch (Exception e) { System.out.println("PROBE: create press failed: " + e.getMessage()); }
        longWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/wo-create-form.xml"), d.getPageSource());
            System.out.println("PROBE: create form dumped");
        } catch (Exception e) { System.out.println("PROBE: create form dump failed: " + e.getMessage()); }
    }

    @Test
    public void PROBE_secondSiteLoads() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - does a DIFFERENT site load? (sick-first-site hypothesis)");
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();
        // Get to the Site Selection picker from ANY state: login if needed
        // (welcomePage flow), or Sites quick action from the dashboard.
        String screen = detectCurrentScreen();
        System.out.println("PROBE: initial screen = " + screen);
        if ("WELCOME_PAGE".equals(screen) || "LOGIN_PAGE".equals(screen)) {
            performLogin();
            longWait();
        } else if ("DASHBOARD".equals(screen)) {
            try { performLoginIfNeededProbe(); } catch (Exception ignored) { }
        }
        System.out.println("PROBE: screen before site tap = " + detectCurrentScreen());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(
                "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad/site-picker.xml"),
                d.getPageSource());
            System.out.println("PROBE: picker dumped");
        } catch (Exception e) { System.out.println("PROBE: picker dump failed: " + e.getMessage()); }
        try {
            org.openqa.selenium.WebElement title = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name CONTAINS '15 June 2026' AND visible == 1"));
            org.openqa.selenium.Rectangle r = title.getRect();
            System.out.println("PROBE: tapping second site title at (" + (r.x + 40) + "," + (r.y + r.height / 2) + ")");
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
        } catch (Exception e) {
            System.out.println("PROBE: second-site tap failed: " + e.getMessage());
        }
        boolean ready = siteSelectionPage.waitForDashboardReady();
        System.out.println("PROBE: second-site dashboardReady = " + ready + ", screen = " + detectCurrentScreen());
    }

    private void performLoginIfNeededProbe() {
        String screen = detectCurrentScreen();
        System.out.println("PROBE: current screen = " + screen);
        if ("DASHBOARD".equals(screen)) {
            try {
                com.egalvanic.utils.DriverManager.getDriver()
                    .findElement(AppiumBy.accessibilityId("Sites")).click();
                longWait();
            } catch (Exception e) { System.out.println("PROBE: Sites tap failed: " + e.getMessage()); }
        }
    }

    @Test
    public void PROBE_freshSiteSelectDashboard() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET, AppConstants.FEATURE_EDIT_ASSET,
            "PROBE - fresh site-select -> dashboard-ready path (CI cascade repro)");
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite(); // land on dashboard
        // Force back to Site Selection via the 'Sites' quick action — this is
        // the exact fresh-select path CI runs every test.
        try {
            d.findElement(AppiumBy.accessibilityId("Sites")).click();
            System.out.println("PROBE: tapped Sites quick action");
        } catch (Exception e) {
            try {
                org.openqa.selenium.WebElement sites = d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Sites' OR label == 'Sites') AND visible == 1"));
                org.openqa.selenium.Rectangle r = sites.getRect();
                d.executeScript("mobile: tap", java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
                System.out.println("PROBE: coordinate-tapped Sites");
            } catch (Exception e2) { System.out.println("PROBE: Sites tap failed: " + e2.getMessage()); }
        }
        longWait();
        System.out.println("PROBE: screen after Sites = " + detectCurrentScreen());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/site-picker.xml"), d.getPageSource());
            System.out.println("PROBE: site picker dumped");
        } catch (Exception e) { System.out.println("PROBE: picker dump failed: " + e.getMessage()); }

        String site = siteSelectionPage.selectFirstSiteFast();
        System.out.println("PROBE: selected site = '" + site + "'");
        longWait();
        boolean ready = siteSelectionPage.waitForDashboardReady();
        System.out.println("PROBE: waitForDashboardReady = " + ready + ", screen = " + detectCurrentScreen());
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/post-site-select.xml"), d.getPageSource());
            System.out.println("PROBE: post-select dumped");
        } catch (Exception e) { System.out.println("PROBE: post-select dump failed: " + e.getMessage()); }
    }

    @Test
    public void PROBE_newIssueFormAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ISSUES,
            AppConstants.FEATURE_ISSUES_LIST,
            "PROBE - v1.50 New Issue form anatomy"
        );

        loginAndSelectSite();
        com.egalvanic.pages.IssuePage issuePage = new com.egalvanic.pages.IssuePage();
        issuePage.navigateToIssuesScreen();
        shortWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(
                "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad/issues-list.xml"),
                com.egalvanic.utils.DriverManager.getDriver().getPageSource());
        } catch (Exception e) { System.out.println("PROBE: list dump failed: " + e.getMessage()); }

        issuePage.tapAddButton();
        mediumWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(
                "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad/new-issue-form.xml"),
                com.egalvanic.utils.DriverManager.getDriver().getPageSource());
            System.out.println("PROBE: New Issue form dumped");
        } catch (Exception e) { System.out.println("PROBE: form dump failed: " + e.getMessage()); }
    }

    @Test
    public void PROBE_connTypeCableForm() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_CONNECTIONS,
            AppConstants.FEATURE_CREATE_CONNECTION,
            "PROBE - v1.50 New Connection form after selecting type 'Cable'"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.ConnectionsPage cp = new com.egalvanic.pages.ConnectionsPage();
        cp.navigateToConnectionsScreen();
        mediumWait();
        cp.tapOnAddButton();
        mediumWait();

        cp.tapOnConnectionTypeField();
        mediumWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/conn-type-sheet.xml"), d.getPageSource());
            System.out.println("PROBE: type sheet dumped");
        } catch (Exception e) { System.out.println("PROBE: sheet dump failed: " + e.getMessage()); }

        boolean sel = cp.selectConnectionType("Cable");
        System.out.println("PROBE: selectConnectionType(Cable) = " + sel);
        mediumWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/conn-form-after-cable.xml"), d.getPageSource());
            System.out.println("PROBE: post-Cable form dumped");
        } catch (Exception e) { System.out.println("PROBE: form dump failed: " + e.getMessage()); }

        // One scroll down in case CORE ATTRIBUTES renders below the fold
        try {
            d.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
            mediumWait();
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/conn-form-after-cable-scrolled.xml"), d.getPageSource());
            System.out.println("PROBE: post-Cable scrolled form dumped");
        } catch (Exception e) { System.out.println("PROBE: scroll dump failed: " + e.getMessage()); }
    }

    @Test
    public void PROBE_newBuildingForm() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_OFFLINE,
            AppConstants.FEATURE_OFFLINE_CREATION,
            "PROBE - v1.50 Locations add-building form anatomy"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        siteSelectionPage.clickLocations();
        mediumWait();
        try { siteSelectionPage.waitForLocationsReady(); } catch (Exception ignored) {}
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/locations-screen.xml"), d.getPageSource());
            System.out.println("PROBE: locations screen dumped");
        } catch (Exception e) { System.out.println("PROBE: locations dump failed: " + e.getMessage()); }

        try {
            siteSelectionPage.clickAddButton();
            mediumWait();
        } catch (Exception e) { System.out.println("PROBE: add button failed: " + e.getMessage()); }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/new-building-form.xml"), d.getPageSource());
            System.out.println("PROBE: new-building form dumped");
        } catch (Exception e) { System.out.println("PROBE: form dump failed: " + e.getMessage()); }

        // Press the 'Building, New Building…' value row and dump what opens
        try {
            org.openqa.selenium.WebElement row = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Building' AND visible == 1"));
            org.openqa.selenium.Rectangle r = row.getRect();
            System.out.println("PROBE: pressing Building row '" + row.getAttribute("name") + "'");
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + 40, "y", r.y + r.height / 2));
            longWait();
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/new-building-step2.xml"), d.getPageSource());
            System.out.println("PROBE: building step2 dumped");
        } catch (Exception e) { System.out.println("PROBE: building row press failed: " + e.getMessage()); }

        // If a text field appeared, type a name and dump once more
        try {
            org.openqa.selenium.WebElement tf = d.findElement(AppiumBy.iOSNsPredicateString(
                "(type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSearchField') AND visible == 1"));
            tf.sendKeys("ProbeBldg_1");
            mediumWait();
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/new-building-step3-typed.xml"), d.getPageSource());
            System.out.println("PROBE: building step3 (typed) dumped");
        } catch (Exception e) { System.out.println("PROBE: no text field after Building row: " + e.getMessage()); }
    }

    @Test
    public void PROBE_issueDetailsAnatomy() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ISSUES,
            AppConstants.FEATURE_ISSUES_LIST,
            "PROBE - v1.50 Issue Details anatomy (class/status/subcategory/photos/delete)"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.IssuePage ip = new com.egalvanic.pages.IssuePage();
        ip.navigateToIssuesScreen();
        mediumWait();
        boolean opened = ip.tapFirstIssue();
        System.out.println("PROBE: tapFirstIssue = " + opened);
        longWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/issue-details-1-top.xml"), d.getPageSource());
            System.out.println("PROBE: details top dumped");
        } catch (Exception e) { System.out.println("PROBE: dump1 failed: " + e.getMessage()); }

        // Details rows are 'label StaticText + control ~35px below' (probe 1).
        // Press each row DIRECTLY (left zone) and dump what opens; dismiss via
        // the sheet's own Close/Cancel, NEVER a blind (200,100) tap — that hits
        // the clock.arrow.circlepath history icon.
        String[][] rows = { {"Status", "issue-details-2-statusdd"}, {"Issue Class", "issue-details-3-classdd"} };
        for (String[] row : rows) {
            try {
                org.openqa.selenium.WebElement lbl = d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND label == '" + row[0] + "' AND visible == 1"));
                org.openqa.selenium.Rectangle r = lbl.getRect();
                System.out.println("PROBE: '" + row[0] + "' label at y=" + r.y + " — pressing control row below");
                d.executeScript("mobile: tap", java.util.Map.of("x", r.x + 30, "y", r.y + 38));
                longWait();
                java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/" + row[1] + ".xml"), d.getPageSource());
                System.out.println("PROBE: " + row[1] + " dumped");
            } catch (Exception e) { System.out.println("PROBE: " + row[0] + " row press failed: " + e.getMessage()); }
            // dismiss: sheet Close/Cancel button, else swipe the sheet down
            try {
                org.openqa.selenium.WebElement close = d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == 'Close' OR name == 'Cancel') AND visible == 1"));
                close.click();
            } catch (Exception e) {
                try { d.executeScript("mobile: swipe", java.util.Map.of("direction", "down")); } catch (Exception ignored) {}
            }
            mediumWait();
            // if we fell off the details screen entirely, re-open the first issue
            try {
                d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Issue Details' AND visible == 1"));
            } catch (Exception e) {
                System.out.println("PROBE: fell off details — re-opening first issue");
                ip.navigateToIssuesScreen();
                mediumWait();
                ip.tapFirstIssue();
                longWait();
            }
        }

        // Scroll to bottom in two steps, dumping each (subcategory / photos / delete / save zones)
        for (int i = 1; i <= 2; i++) {
            try {
                d.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
                mediumWait();
                java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/issue-details-4-scroll" + i + ".xml"), d.getPageSource());
                System.out.println("PROBE: details scroll" + i + " dumped");
            } catch (Exception e) { System.out.println("PROBE: scroll" + i + " dump failed: " + e.getMessage()); }
        }
    }

    @Test
    public void PROBE_sessionRoomSurface() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_SESSION_DETAILS,
            "PROBE - v1.50 session surface room anatomy (AF_024/025 skips)"
        );
        String dir = "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/ba210b3e-faec-46c4-bbed-eb1cec075170/scratchpad";
        io.appium.java_client.ios.IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.WorkOrderPage wp = new com.egalvanic.pages.WorkOrderPage();
        boolean open = wp.ensureSessionDetailsOpen();
        System.out.println("PROBE: ensureSessionDetailsOpen = " + open);
        longWait();
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/session-surface-1.xml"), d.getPageSource());
            System.out.println("PROBE: session surface 1 dumped");
        } catch (Exception e) { System.out.println("PROBE: surface-1 dump failed: " + e.getMessage()); }

        // Hop to the session Assets tab — press-verify-retry: center press first,
        // then the label StaticText below the strip (probe 1: center no-opped).
        try {
            String before = d.getPageSource();
            org.openqa.selenium.WebElement tab = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == 'Assets' AND visible == 1"));
            org.openqa.selenium.Rectangle r = tab.getRect();
            System.out.println("PROBE: 'Assets' tab rect x=" + r.x + " y=" + r.y + " w=" + r.width + " h=" + r.height);
            d.executeScript("mobile: tap", java.util.Map.of("x", r.x + r.width / 2, "y", r.y + r.height / 2));
            longWait();
            String after = d.getPageSource();
            if (after.length() == before.length()) {
                System.out.println("PROBE: center press no-opped — pressing label StaticText");
                org.openqa.selenium.WebElement lbl = d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Assets' AND visible == 1 AND rect.y > 800"));
                org.openqa.selenium.Rectangle lr = lbl.getRect();
                d.executeScript("mobile: tap", java.util.Map.of("x", lr.x + lr.width / 2, "y", lr.y + lr.height / 2));
                longWait();
                after = d.getPageSource();
                if (after.length() == before.length()) {
                    System.out.println("PROBE: label press ALSO no-opped — trying w3c click on button");
                    tab.click();
                    longWait();
                }
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/session-surface-2-assets.xml"), d.getPageSource());
            System.out.println("PROBE: session surface 2 (Assets tab) dumped");
        } catch (Exception e) { System.out.println("PROBE: surface-2 dump failed: " + e.getMessage()); }

        // One scroll down — room rows may be below the fold
        try {
            d.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
            mediumWait();
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir + "/session-surface-3-scrolled.xml"), d.getPageSource());
            System.out.println("PROBE: session surface 3 (scrolled) dumped");
        } catch (Exception e) { System.out.println("PROBE: surface-3 dump failed: " + e.getMessage()); }
    }
}
