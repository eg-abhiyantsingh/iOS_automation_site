package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TEMPORARY diagnostic — v1.51 Locations screen anatomy (New Building affordance,
 * No Location section). Not part of any suite; run with -Dtest=DebugLocationsProbe_Test.
 */
public class DebugLocationsProbe_Test extends BaseTest {

    private static final String DIR =
        "/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/fe0bce08-599e-410e-9ffd-430769c5bdd4/scratchpad";

    @Test
    public void PROBE_locationsAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - v1.51 Locations anatomy");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage buildingPage = new com.egalvanic.pages.BuildingPage();
        buildingPage.navigateToLocationsScreen();
        mediumWait();

        Files.writeString(Path.of(DIR + "/locations-screen.xml"), d.getPageSource());
        System.out.println("PROBE: locations screen dumped");

        // enumerate buttons + images (SF symbols show as Image/Button names)
        for (String type : new String[]{"XCUIElementTypeButton", "XCUIElementTypeImage",
                "XCUIElementTypeNavigationBar", "XCUIElementTypeStaticText"}) {
            List<WebElement> els = d.findElements(AppiumBy.className(type));
            System.out.println("== " + type + " (" + els.size() + ") ==");
            int i = 0;
            for (WebElement e : els) {
                if (i++ > 40) { System.out.println("   ...truncated"); break; }
                try {
                    System.out.println(String.format("   name=%-40s label=%-40s visible=%s rect=%s",
                        s(e.getAttribute("name")), s(e.getAttribute("label")),
                        e.getAttribute("visible"), e.getRect()));
                } catch (Exception ignored) {}
            }
        }

        // try the plus/FAB affordances and dump whatever opens
        String[][] cands = {
            {"acc", "plus"},
            {"pred", "name CONTAINS 'plus'"},
            {"pred", "type == 'XCUIElementTypeButton' AND (label CONTAINS 'Add' OR label CONTAINS 'New')"},
        };
        for (String[] c : cands) {
            try {
                WebElement el = "acc".equals(c[0])
                    ? d.findElement(AppiumBy.accessibilityId(c[1]))
                    : d.findElement(AppiumBy.iOSNsPredicateString(c[1]));
                System.out.println("PROBE: tapping candidate " + c[1] + " label=" + s(el.getAttribute("label")));
                el.click();
                Thread.sleep(1200);
                Files.writeString(Path.of(DIR + "/locations-after-plus.xml"), d.getPageSource());
                System.out.println("PROBE: post-tap dump written (candidate=" + c[1] + ")");
                break;
            } catch (Exception e) {
                System.out.println("PROBE: candidate " + c[1] + " -> " + e.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void PROBE_locationsDeepAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - v1.51 Locations deep anatomy (menus, No Location, form)");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();

        // 1) dismiss any leftover sheet from the prior probe
        try { d.findElement(AppiumBy.accessibilityId("Cancel")).click(); Thread.sleep(600); } catch (Exception ignored) {}

        // 2) scroll to the very bottom — is there a 'No Location' section?
        for (int i = 0; i < 12; i++) {
            try {
                d.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
            } catch (Exception e) { break; }
        }
        Thread.sleep(500);
        Files.writeString(Path.of(DIR + "/locations-bottom.xml"), d.getPageSource());
        boolean noLoc = d.getPageSource().contains("No Location");
        System.out.println("PROBE: after scroll-to-bottom, 'No Location' present = " + noLoc);

        // 3) back to top
        for (int i = 0; i < 12; i++) {
            try { d.executeScript("mobile: scroll", java.util.Map.of("direction", "up")); } catch (Exception e) { break; }
        }
        Thread.sleep(400);

        // 4) long-press first building row -> context menu?
        try {
            WebElement bldg = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS ' floors'"));
            System.out.println("PROBE: long-pressing building row: " + bldg.getAttribute("label"));
            org.openqa.selenium.Rectangle r = bldg.getRect();
            d.executeScript("mobile: touchAndHold", java.util.Map.of(
                "x", r.x + r.width / 2, "y", r.y + Math.min(30, r.height / 2), "duration", 1.5));
            Thread.sleep(1200);
            Files.writeString(Path.of(DIR + "/building-longpress.xml"), d.getPageSource());
            System.out.println("PROBE: building long-press dump written");
            // dump visible buttons/menu items
            for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeMenuItem' OR type == 'XCUIElementTypeCell') AND visible == 1"))) {
                System.out.println("   post-longpress: " + s(e.getAttribute("name")));
            }
            // dismiss the menu
            d.executeScript("mobile: tap", java.util.Map.of("x", 200, "y", 60));
            Thread.sleep(700);
        } catch (Exception e) {
            System.out.println("PROBE: building long-press failed: " + e.getMessage());
        }

        // 5) open New Location form, type a building name only, check Create enabled-state
        try {
            d.findElement(AppiumBy.accessibilityId("plus")).click();
            Thread.sleep(900);
            List<WebElement> tfs = d.findElements(AppiumBy.className("XCUIElementTypeTextField"));
            System.out.println("PROBE: New Location form has " + tfs.size() + " text fields; placeholders:");
            for (WebElement tf : tfs) System.out.println("   placeholder/value: " + s(tf.getAttribute("value")) + " | name=" + s(tf.getAttribute("name")));
            if (!tfs.isEmpty()) {
                tfs.get(0).sendKeys("QA_Probe_Bldg");
                Thread.sleep(300);
                WebElement create = d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Create'"));
                System.out.println("PROBE: Create enabled after building-name only = " + create.getAttribute("enabled"));
                Files.writeString(Path.of(DIR + "/newlocation-filled1.xml"), d.getPageSource());
            }
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'")).click();
            Thread.sleep(600);
        } catch (Exception e) {
            System.out.println("PROBE: New Location form probe failed: " + e.getMessage());
        }

        // 6) asset location field (TC_AL family): open Assets tab, first asset details
        try {
            d.findElement(AppiumBy.iOSNsPredicateString("label == 'Assets' AND type == 'XCUIElementTypeButton'")).click();
            Thread.sleep(1500);
            Files.writeString(Path.of(DIR + "/assets-tab.xml"), d.getPageSource());
            System.out.println("PROBE: assets tab dumped — grep for Select location later");
        } catch (Exception e) {
            System.out.println("PROBE: assets tab failed: " + e.getMessage());
        }
    }

    @Test
    public void PROBE_locationsFormPickerAndMenus() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - v1.51 New Location picker + context menu targets (scoped)");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();
        try { d.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Cancel'")).click(); Thread.sleep(500); } catch (Exception ignored) {}

        // A) form -> tap Building picker row -> scoped anatomy of what opens
        try {
            d.findElement(AppiumBy.accessibilityId("plus")).click();
            Thread.sleep(900);
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Building,'")).click();
            Thread.sleep(1200);
            // navigation bars are cheap
            for (WebElement nb : d.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"))) {
                System.out.println("PROBE-A navbar: " + s(nb.getAttribute("name")));
            }
            // does a search field exist (sheet-mode picker)?
            System.out.println("PROBE-A searchfields: " + d.findElements(AppiumBy.className("XCUIElementTypeSearchField")).size());
            // first 12 buttons raw (no visibility computation)
            List<WebElement> btns = d.findElements(AppiumBy.className("XCUIElementTypeButton"));
            System.out.println("PROBE-A buttons total: " + btns.size());
            int i = 0;
            for (WebElement b : btns) {
                if (i++ >= 15) break;
                System.out.println("   A-btn: " + s(b.getAttribute("name")));
            }
            // pick first existing Bldg_ option
            try {
                d.findElement(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label BEGINSWITH 'Bldg_'")).click();
                Thread.sleep(900);
                System.out.println("PROBE-A2: picked existing building; form rows:");
                for (WebElement b : d.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label CONTAINS ',' OR label == 'Create' OR label == 'Cancel')"))) {
                    System.out.println("   A2-btn: " + s(b.getAttribute("name")) + " enabled=" + b.getAttribute("enabled"));
                }
                for (WebElement tf : d.findElements(AppiumBy.className("XCUIElementTypeTextField"))) {
                    System.out.println("   A2-tf: value=" + s(tf.getAttribute("value")));
                }
            } catch (Exception e) { System.out.println("PROBE-A2 failed: " + e.getClass().getSimpleName()); }
            d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == 'Cancel'")).click();
            Thread.sleep(600);
        } catch (Exception e) {
            System.out.println("PROBE-A failed: " + e.getMessage());
            try { d.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Cancel'")).click(); Thread.sleep(500); } catch (Exception ignored) {}
        }

        // B) long-press building -> pencil -> scoped edit-screen anatomy
        try {
            WebElement bldg = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS ' floors'"));
            String rowLabel = bldg.getAttribute("label");
            org.openqa.selenium.Rectangle r = bldg.getRect();
            d.executeScript("mobile: touchAndHold", java.util.Map.of(
                "x", r.x + r.width / 2, "y", r.y + Math.min(30, r.height / 2), "duration", 1.5));
            Thread.sleep(1200);
            d.findElement(AppiumBy.iOSNsPredicateString("name == 'pencil'")).click();
            Thread.sleep(1000);
            System.out.println("PROBE-B: row='" + rowLabel + "' -> pencil opened");
            for (WebElement nb : d.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"))) {
                System.out.println("   B-navbar: " + s(nb.getAttribute("name")));
            }
            for (WebElement tf : d.findElements(AppiumBy.className("XCUIElementTypeTextField"))) {
                System.out.println("   B-tf: value=" + s(tf.getAttribute("value")));
            }
            for (WebElement tv : d.findElements(AppiumBy.className("XCUIElementTypeTextView"))) {
                System.out.println("   B-tv: value=" + s(tv.getAttribute("value")));
            }
            for (WebElement b : d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Save' OR label == 'Cancel' OR label == 'Done' OR label CONTAINS 'Save')"))) {
                System.out.println("   B-btn: " + s(b.getAttribute("name")) + " enabled=" + b.getAttribute("enabled"));
            }
            try { d.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Cancel'")).click(); } catch (Exception ignored) {}
            Thread.sleep(600);
        } catch (Exception e) {
            System.out.println("PROBE-B failed: " + e.getMessage());
        }

        // C) long-press building -> note.text -> scoped anatomy
        try {
            WebElement bldg = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS ' floors'"));
            org.openqa.selenium.Rectangle r = bldg.getRect();
            d.executeScript("mobile: touchAndHold", java.util.Map.of(
                "x", r.x + r.width / 2, "y", r.y + Math.min(30, r.height / 2), "duration", 1.5));
            Thread.sleep(1200);
            d.findElement(AppiumBy.iOSNsPredicateString("name == 'note.text'")).click();
            Thread.sleep(1000);
            System.out.println("PROBE-C: note.text opened");
            for (WebElement nb : d.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"))) {
                System.out.println("   C-navbar: " + s(nb.getAttribute("name")));
            }
            for (WebElement tv : d.findElements(AppiumBy.className("XCUIElementTypeTextView"))) {
                System.out.println("   C-tv: value=" + s(tv.getAttribute("value")));
            }
            for (WebElement tf : d.findElements(AppiumBy.className("XCUIElementTypeTextField"))) {
                System.out.println("   C-tf: value=" + s(tf.getAttribute("value")));
            }
            for (WebElement b : d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Save' OR label == 'Cancel' OR label == 'Done' OR label == 'Close')"))) {
                System.out.println("   C-btn: " + s(b.getAttribute("name")) + " enabled=" + b.getAttribute("enabled"));
            }
            try { d.findElement(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (label == 'Cancel' OR label == 'Done' OR label == 'Close')")).click(); } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("PROBE-C failed: " + e.getMessage());
        }
    }

    @Test
    public void PROBE_noLocationAfterResync() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - No Location after full site re-selection");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        // force a REAL re-sync: Sites → re-select first site
        siteSelectionPage.clickSitesButton();
        Thread.sleep(1500);
        siteSelectionPage.selectFirstSiteFast();
        Thread.sleep(2000);

        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();
        boolean found = bp.scrollToNoLocationTurbo();
        System.out.println("PROBE-RESYNC: No Location present after site re-selection = " + found);
        if (!found) {
            // check whether the new asset at least exists in the Assets list
            try { d.findElement(AppiumBy.accessibilityId("Done")).click(); Thread.sleep(800); } catch (Exception ignored) {}
            try {
                d.findElement(AppiumBy.iOSNsPredicateString(
                    "label == 'Assets' AND type == 'XCUIElementTypeButton'")).click();
                Thread.sleep(1500);
                boolean assetSeen = !d.findElements(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS 'QA-NoLoc'")).isEmpty();
                System.out.println("PROBE-RESYNC: QA-NoLoc asset visible in Assets list = " + assetSeen);
                List<WebElement> cells = d.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label CONTAINS 'No Location'"));
                System.out.println("PROBE-RESYNC: cells containing 'No Location': " + cells.size());
                for (int i = 0; i < Math.min(3, cells.size()); i++) {
                    System.out.println("   cell: " + cells.get(i).getAttribute("label"));
                }
            } catch (Exception e) {
                System.out.println("PROBE-RESYNC assets check failed: " + e.getMessage());
            }
        }
    }

    @Test
    public void PROBE_syncCountAndSearch() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - dashboard count + Assets search after resync");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        siteSelectionPage.clickSitesButton();
        Thread.sleep(1500);
        siteSelectionPage.selectFirstSiteFast();
        Thread.sleep(5000);   // give sync time

        System.out.println("PROBE-SYNC: dashboard assets count = " + siteSelectionPage.getAssetsCountText());

        // Assets tab → search for the QA fixture
        d.findElement(AppiumBy.iOSNsPredicateString(
            "label == 'Assets' AND type == 'XCUIElementTypeButton'")).click();
        Thread.sleep(1500);
        try {
            WebElement search = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR (type == 'XCUIElementTypeTextField' AND visible == 1)"));
            search.click();
            search.sendKeys("QA-NoLoc");
            Thread.sleep(1500);
            List<WebElement> cells = d.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'QA-NoLoc'"));
            System.out.println("PROBE-SYNC: elements matching QA-NoLoc after search: " + cells.size());
            for (int i = 0; i < Math.min(4, cells.size()); i++) {
                System.out.println("   hit: " + cells.get(i).getAttribute("label"));
            }
        } catch (Exception e) {
            System.out.println("PROBE-SYNC search failed: " + e.getMessage());
        }
    }

    @Test
    public void PROBE_scrollToTrueEnd() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - definitive scroll-to-end of Locations list");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();

        String prevSig = "";
        for (int i = 0; i < 30; i++) {
            // signature = labels of the last few buttons currently materialized
            List<WebElement> btns = d.findElements(AppiumBy.className("XCUIElementTypeButton"));
            StringBuilder sig = new StringBuilder();
            for (int j = Math.max(0, btns.size() - 6); j < btns.size(); j++) {
                try { sig.append(btns.get(j).getAttribute("name")).append('|'); } catch (Exception ignored) {}
            }
            if (sig.toString().equals(prevSig)) {
                System.out.println("PROBE-END: list stopped changing after " + i + " scrolls");
                break;
            }
            prevSig = sig.toString();
            if (!d.findElements(AppiumBy.iOSNsPredicateString("label CONTAINS 'No Location'")).isEmpty()) {
                System.out.println("PROBE-END: FOUND 'No Location' at scroll " + i);
                return;
            }
            d.executeScript("mobile: scroll", java.util.Map.of("direction", "down"));
            Thread.sleep(350);
        }
        boolean present = !d.findElements(AppiumBy.iOSNsPredicateString("label CONTAINS 'No Location'")).isEmpty();
        System.out.println("PROBE-END: at true end, 'No Location' present = " + present);
        List<WebElement> btns = d.findElements(AppiumBy.className("XCUIElementTypeButton"));
        System.out.println("PROBE-END: last labels:");
        for (int j = Math.max(0, btns.size() - 8); j < btns.size(); j++) {
            try { System.out.println("   " + btns.get(j).getAttribute("name")); } catch (Exception ignored) {}
        }
    }

    @Test
    public void PROBE_noLocationScreenAnatomy() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - No Location screen + unassigned asset details anatomy");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();

        WebElement noLoc = d.findElement(AppiumBy.iOSNsPredicateString("label CONTAINS 'No Location'"));
        System.out.println("PROBE-NL: section label = " + noLoc.getAttribute("label") + " type=" + noLoc.getTagName());
        noLoc.click();
        Thread.sleep(1200);
        System.out.println("PROBE-NL: after tap — navbars:");
        for (WebElement nb : d.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"))) {
            System.out.println("   navbar: " + s(nb.getAttribute("name")));
        }
        System.out.println("PROBE-NL: buttons:");
        int i = 0;
        for (WebElement b : d.findElements(AppiumBy.className("XCUIElementTypeButton"))) {
            if (i++ > 20) break;
            System.out.println("   btn: " + s(b.getAttribute("name")));
        }
        System.out.println("PROBE-NL: searchfields: " + d.findElements(AppiumBy.className("XCUIElementTypeSearchField")).size()
            + " textfields: " + d.findElements(AppiumBy.className("XCUIElementTypeTextField")).size());

        // open the first unassigned asset
        try {
            WebElement asset = d.findElement(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label CONTAINS 'QA-NoLoc'"));
            String assetLabel = asset.getAttribute("label");
            asset.click();
            Thread.sleep(1500);
            System.out.println("PROBE-NL: opened asset '" + assetLabel + "' — details elements mentioning location:");
            for (WebElement e : d.findElements(AppiumBy.iOSNsPredicateString(
                    "label CONTAINS[c] 'location' OR value CONTAINS[c] 'location'"))) {
                System.out.println("   loc-el: type=" + e.getTagName() + " label=" + s(e.getAttribute("label"))
                    + " value=" + s(e.getAttribute("value")));
            }
            System.out.println("PROBE-NL: details statictexts (first 25):");
            i = 0;
            for (WebElement t : d.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                if (i++ > 25) break;
                System.out.println("   st: " + s(t.getAttribute("name")));
            }
        } catch (Exception e) {
            System.out.println("PROBE-NL asset open failed: " + e.getMessage());
        }
    }

    @Test
    public void PROBE_newFloorPathState() throws Exception {
        ExtentReportManager.createTest(AppConstants.MODULE_LOCATIONS, "Probe",
            "PROBE - form state after picking existing building");
        IOSDriver d = com.egalvanic.utils.DriverManager.getDriver();

        loginAndSelectSite();
        com.egalvanic.pages.BuildingPage bp = new com.egalvanic.pages.BuildingPage();
        bp.navigateToLocationsScreen();
        mediumWait();

        System.out.println("PROBE-NF: openNewLocationForm = " + bp.openNewLocationForm());
        System.out.println("PROBE-NF: pickExistingBuildingInForm = " + bp.pickExistingBuildingInForm("Bldg_9106"));
        System.out.println("PROBE-NF: isNewLocationFormOpen = " + bp.isNewLocationFormOpen());
        for (WebElement t : d.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'New Location'"))) {
            System.out.println("   NL-text: visible=" + t.getAttribute("visible") + " label=" + t.getAttribute("label"));
        }
        for (WebElement tf : d.findElements(AppiumBy.className("XCUIElementTypeTextField"))) {
            System.out.println("   tf: value=" + s(tf.getAttribute("value")));
        }
        for (WebElement b : d.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label BEGINSWITH 'Building,' OR label BEGINSWITH 'Floor,')"))) {
            System.out.println("   row: " + s(b.getAttribute("label")));
        }
        System.out.println("PROBE-NF: isNewFloorScreenDisplayed = " + bp.isNewFloorScreenDisplayed());
        try { bp.clickCancel(); } catch (Exception ignored) {}
    }

    private static String s(String v) { return v == null ? "∅" : v; }
}
