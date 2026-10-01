package com.egalvanic.pages;

import com.egalvanic.base.BasePage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * ZP-3927 (iOS 1.57) §4 SD-card / drive import · §5 30-minute window · §7 already-uploaded photos.
 *
 * Surface (probe 1.63, 2026-09-21): work-order session › More › IR → the IR screen ("0 IR Photos",
 * "Add IR Photos", "File names are used as IR & visual photo keys.", empty state "No IR Photos" /
 * "Tap the + button to add IR photo sets to assets"). "Add IR Photos" opens a popup with three routes:
 *
 *   «From Photos»        — Pick from your photo library.
 *   «Browse & Select»    — Pick photos yourself from Files or a drive.          ← the SD-card path
 *   «Auto-Match by Name» — Find photos on a drive whose names match your IR keys.
 *
 * Copy is verbatim from the 1.63 binary and the probe dump. The FLIR-camera-specific views
 * (FLIRCameraEntrySection / FLIRCameraConnectSheet / FLIRDriveImportSheet / FLIRDriveTimeFilter
 * with "Last 30 min" … "Today") sit behind a connected camera or mounted drive, which a simulator
 * cannot supply — those constants are kept so device runs can assert them.
 */
public class FlirImportPage extends BasePage {

    // ── IR screen (session › More › IR) ──────────────────────────────────────
    public static final String MORE_TAB          = "More";
    public static final String IR_ENTRY          = "IR";
    public static final String ADD_IR_PHOTOS     = "Add IR Photos";
    public static final String IR_KEYS_HINT      = "File names are used as IR & visual photo keys.";
    public static final String NO_IR_PHOTOS      = "No IR Photos";
    public static final String NO_IR_PHOTOS_HINT = "Tap the + button to add IR photo sets to assets";

    // ── "Add IR Photos" popup routes ────────────────────────────────────────
    public static final String ROUTE_FROM_PHOTOS      = "From Photos";
    public static final String ROUTE_FROM_PHOTOS_SUB  = "Pick from your photo library.";
    public static final String ROUTE_BROWSE           = "Browse & Select";
    public static final String ROUTE_BROWSE_SUB       = "Pick photos yourself from Files or a drive.";
    public static final String ROUTE_AUTO_MATCH       = "Auto-Match by Name";
    public static final String ROUTE_AUTO_MATCH_SUB   = "Find photos on a drive whose names match your IR keys.";
    public static final String[] ROUTES = { ROUTE_FROM_PHOTOS, ROUTE_BROWSE, ROUTE_AUTO_MATCH };

    // ── Auto-Match without a drive (probe 4, 1.63): explicit drive-selection screen ──
    public static final String SELECT_DRIVE_TITLE   = "Select a Drive to Match";
    public static final String SELECT_DRIVE_BUTTON  = "Select drive or folder";
    public static final String DRIVE_MATCH_HINT     = "Files on the drive are matched to this work order by file name.";

    public boolean isSelectDriveScreenShown() {
        return isAnyTextPresent(SELECT_DRIVE_TITLE) || isAnyTextPresent(SELECT_DRIVE_BUTTON) || isAnyTextContaining("matched to this work order");
    }

    // ── FLIR camera / drive candidate list (device-only on the simulator) ────
    public static final String ENTRY_FROM_FLIR       = "From FLIR Camera";
    public static final String CONNECT_TITLE         = "Connect FLIR Camera";
    public static final String CONNECT_BODY          = "Connect to the camera and pull in its latest captures.";
    public static final String NO_CAMERA             = "No camera connected";
    public static final String CAMERA_NOT_CONNECTED  = "camera not connected";
    public static final String IMPORTING             = "Importing from camera";
    public static final String NO_PHOTOS_FOUND       = "No photos found";
    public static final String SELECT_ALL            = "Select All";
    public static final String DESELECT_ALL          = "Deselect All";
    public static final String IMPORT                = "Import";
    public static final String ALREADY_UPLOADED      = "already uploaded";
    public static final String NEWEST_FIRST          = "newest photo first";
    public static final String NEWEST_ALREADY_ADDED  = "is the newest photo on the camera and is already added to this asset.";
    public static final String TIME_FILTER_30_MIN    = "Last 30 min";
    /** FLIRDriveTimeFilter cases, in the order the binary declares them. */
    public static final String[] TIME_FILTERS = {
        "Last 30 min", "Last 1 hour", "Last 4 hours", "Last 8 hours", "Last 12 hours", "Last 24 hours", "Today"
    };

    // ── IR screen ────────────────────────────────────────────────────────────
    public boolean isAnyVisibleText(String text) { return isAnyTextPresent(text); }
    public boolean isAnyVisibleTextContaining(String text) { return isAnyTextContaining(text); }


    /** Session › More › IR. Assumes the session details are open. */
    public boolean openIrScreenFromSession() {
        if (isIrScreenShown()) return true;
        if (!tapText(MORE_TAB)) return false;
        sleep(600);
        if (!tapText(IR_ENTRY)) return false;
        sleep(900);
        return isIrScreenShown();
    }

    public boolean isIrScreenShown() {
        return isAnyTextPresent(ADD_IR_PHOTOS) || isAnyTextContaining(IR_KEYS_HINT) || isAnyTextPresent(NO_IR_PHOTOS);
    }

    /** "N IR Photos" counter on the IR screen, or -1. */
    public int irPhotoCount() {
        try {
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND visible == 1 AND label ENDSWITH ' IR Photos' AND NOT (label BEGINSWITH 'Add')")));
            for (WebElement e : l) {
                String s = e.getAttribute("label").replace(" IR Photos", "").trim();
                return Integer.parseInt(s);
            }
        } catch (Exception ignored) { }
        return -1;
    }

    /** Open the "Add IR Photos" route popup. */
    public boolean openAddIrPhotosMenu() {
        if (isRoutePopupShown()) return true;
        boolean tapped = tapText(ADD_IR_PHOTOS) || tapText("Add");
        sleep(800);
        return tapped && isRoutePopupShown();
    }

    public boolean isRoutePopupShown() {
        return isAnyTextPresent(ROUTE_FROM_PHOTOS) || isAnyTextPresent(ROUTE_AUTO_MATCH) || isAnyTextContaining("Browse");
    }

    /** Routes visible in the popup, in declaration order. */
    public List<String> visibleRoutes() {
        List<String> out = new ArrayList<>();
        for (String r : ROUTES) {
            if (isAnyTextPresent(r) || (r.contains("&") && isAnyTextContaining("Browse"))) out.add(r);
        }
        return out;
    }

    public boolean isRouteSubtitleShown(String subtitle) { return isAnyTextContaining(subtitle.replace("&", "")); }

    /** Tap a route by its title (ampersand-safe). */
    public boolean tapRoute(String route) {
        if (tapText(route)) return true;
        try {
            String frag = route.contains("&") ? "Browse" : route;
            List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND visible == 1 AND label CONTAINS[c] '" + frag + "'")));
            if (l.isEmpty()) return false;
            l.get(0).click();
            sleep(900);
            return true;
        } catch (Exception e) { return false; }
    }

    /** Close the route popup without choosing (the popup exposes a 'dismiss popup' hit area). */
    public boolean dismissRoutePopup() {
        if (tapText("dismiss popup") || tapText("Cancel")) { sleep(500); return true; }
        try {
            org.openqa.selenium.Dimension d = driver.manage().window().getSize();
            driver.executeScript("mobile: tap", java.util.Map.of("x", d.getWidth() / 2, "y", d.getHeight() - 60));
            sleep(500);
            return true;
        } catch (Exception e) { return false; }
    }

    // ── FLIR camera / drive surfaces ─────────────────────────────────────────

    public boolean isFlirEntryVisible()      { return isAnyTextPresent(ENTRY_FROM_FLIR) || isAnyTextPresent(CONNECT_TITLE) || isAnyTextContaining("FLIR Camera"); }
    public boolean isConnectSheetShown()     { return isAnyTextPresent(CONNECT_TITLE) || isAnyTextContaining(CONNECT_BODY); }
    public boolean isNoCameraStateShown()    { return isAnyTextContaining(NO_CAMERA) || isAnyTextContaining(CAMERA_NOT_CONNECTED); }
    public boolean isSelectAllOffered()      { return isAnyTextPresent(SELECT_ALL); }
    public boolean isDeselectAllOffered()    { return isAnyTextPresent(DESELECT_ALL); }
    public boolean isAlreadyUploadedChipShown() { return isAnyTextContaining(ALREADY_UPLOADED); }
    public boolean isNewestFirstHintShown()  { return isAnyTextContaining(NEWEST_FIRST); }

    /** Time-window chips currently visible, in declaration order (subset of {@link #TIME_FILTERS}). */
    public List<String> visibleTimeFilters() {
        List<String> out = new ArrayList<>();
        for (String f : TIME_FILTERS) if (isAnyTextPresent(f)) out.add(f);
        return out;
    }

    public String selectedTimeFilter() {
        for (String f : TIME_FILTERS) {
            try {
                List<WebElement> l = withImplicitWait(0, () -> driver.findElements(AppiumBy.iOSNsPredicateString(
                        "visible == 1 AND (label ==[c] '" + f + "' OR name ==[c] '" + f + "')")));
                for (WebElement e : l) {
                    if ("true".equals(e.getAttribute("selected")) || "1".equals(e.getAttribute("value"))) return f;
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** True while a system picker sheet (Photos or Files) covers the IR screen. */
    public boolean isSystemPickerPresented() {
        boolean irChromeVisible = isAnyTextPresent(ADD_IR_PHOTOS) || isAnyTextPresent(NO_IR_PHOTOS);
        boolean pickerHints = isAnyTextPresent("Photos") || isAnyTextPresent("Recents") || isAnyTextPresent("Browse")
                || isAnyTextPresent("On My iPhone") || isAnyTextPresent("iCloud Drive") || isAnyTextPresent("Recents");
        return pickerHints || !irChromeVisible;
    }

    /** Dismiss whatever sheet / popup is up (Cancel / Close / Done / popup backdrop), best-effort. */
    public boolean dismiss() {
        boolean any = false;
        for (int i = 0; i < 2; i++) {
            if (tapText("Cancel") || tapText("Close") || tapText("Done") || tapText("dismiss popup")) any = true; else break;
            sleep(400);
        }
        return any;
    }
}
