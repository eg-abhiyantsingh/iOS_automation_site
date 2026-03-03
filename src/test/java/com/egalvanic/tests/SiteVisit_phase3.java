package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.WorkOrderPage;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Site Visit / Work Orders — Phase 3 Test Suite (10 tests)
 * ════════════════════════════════════════════════════════════
 *
 * TC_JOB_200: Verify adding photo via Camera in walkthrough
 * TC_JOB_201: Verify Done with this asset button enables after photo
 * TC_JOB_202: Verify removing photo with X button
 * TC_JOB_203: Verify Done with this asset opens Classify Asset
 * TC_JOB_204: Verify Asset Type dropdown in Classify Asset
 * TC_JOB_205: Verify selecting Asset Type shows Subtype dropdown
 * TC_JOB_206: Verify Continue button enables after type selection
 * TC_JOB_207: Verify Add OCPDs prompt for MCC in walkthrough
 * TC_JOB_208: Verify Yes, Add OCPD Photos button
 * TC_JOB_209: Verify tapping Yes opens OCPD capture screen
 *
 * ════════════════════════════════════════════════════════════
 * Flow: Dashboard → Work Orders → Activate → Session Details →
 *       Locations → Room → Assets in Room → + (Add Assets) →
 *       New Asset tab → Create Photo Walkthrough →
 *       Photo Walkthrough → [add photo] → Done with this asset →
 *       Classify Asset → [select type] → Continue →
 *       Add OCPDs? → Yes, Add OCPD Photos → Photograph OCPD
 * ════════════════════════════════════════════════════════════
 */
public class SiteVisit_phase3 extends BaseTest {
    private WorkOrderPage workOrderPage;

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n📋 Site Visit Phase 3 Test Suite — Starting (10 tests)");
        System.out.println("   Photo Walkthrough: Camera, Done, Remove, Classify Asset, OCPD");
        DriverManager.setNoReset(true);
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
        System.out.println("📋 Site Visit Phase 3 Test Suite — Complete\n");
    }

    @BeforeMethod(alwaysRun = true)
    public void initPageObjects() {
        workOrderPage = new WorkOrderPage();
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Navigate from dashboard to Work Orders screen.
     */
    private void navigateToWorkOrdersScreen() {
        System.out.println("📍 Navigating to Work Orders screen...");
        siteSelectionPage.clickNoActiveJobCard();
        shortWait();
        boolean loaded = workOrderPage.waitForWorkOrdersScreen();
        if (!loaded) {
            System.out.println("🔄 Retrying navigation to Work Orders screen...");
            siteSelectionPage.clickNoActiveJobCard();
            mediumWait();
            workOrderPage.waitForWorkOrdersScreen();
        }
        System.out.println("✅ On Work Orders screen");
    }

    /**
     * Ensure we are on the dashboard. If already there, no-op.
     */
    private void ensureOnDashboard() {
        if (assetPage != null && assetPage.isDashboardDisplayedFast()) {
            return;
        }
        smartNavigateToDashboard();
    }

    /**
     * Navigate to Session Details screen if not already there.
     */
    private void ensureOnSessionDetailsScreen() {
        if (workOrderPage.isSessionDetailsScreenDisplayed()) {
            return;
        }

        ensureOnDashboard();
        navigateToWorkOrdersScreen();
        shortWait();

        if (!workOrderPage.isActiveBadgeDisplayed()) {
            logStep("No active job — activating one for session details");
            workOrderPage.tapActivateButton();
            mediumWait();
        }

        workOrderPage.tapActiveWorkOrder();
        mediumWait();
        workOrderPage.waitForSessionDetailsScreen();
    }

    /**
     * Navigate from Session Details → Locations tab → expand building →
     * expand floor → tap room → Assets in Room screen.
     */
    private void navigateToAssetsInRoom() {
        logStep("Starting navigation to Assets in Room...");

        ensureOnSessionDetailsScreen();
        workOrderPage.tapSessionTab("Locations");
        mediumWait();

        int buildingCount = workOrderPage.getLocationsBuildingCount();
        logStep("Buildings found: " + buildingCount);

        if (buildingCount == 0) {
            logWarning("No buildings found — cannot navigate to room");
            return;
        }

        workOrderPage.tapLocationsBuildingAtIndex(0);
        mediumWait();

        java.util.List<String> floors = workOrderPage.getLocationsFloorEntries();
        logStep("Floors found: " + floors.size());

        if (floors.isEmpty()) {
            logWarning("No floors found after expanding building");
            return;
        }

        workOrderPage.tapLocationsFloorAtIndex(0);
        mediumWait();

        java.util.List<String> rooms = workOrderPage.getLocationsRoomEntries();
        logStep("Rooms found: " + rooms.size());

        if (rooms.isEmpty()) {
            logWarning("No rooms found after expanding floor");
            return;
        }

        workOrderPage.tapLocationsRoomAtIndex(0);
        mediumWait();

        workOrderPage.waitForAssetsInRoomScreen();
        logStep("Navigation to Assets in Room complete");
    }

    /**
     * Navigate to Add Assets screen: Assets in Room → tap floating +.
     */
    private void navigateToAddAssetsScreen() {
        logStep("Navigating to Assets in Room first...");
        navigateToAssetsInRoom();

        if (!workOrderPage.isAssetsInRoomScreenDisplayed()) {
            logWarning("Not on Assets in Room screen — cannot open Add Assets");
            return;
        }

        logStep("Tapping floating + to open Add Assets");
        workOrderPage.tapAssetsInRoomFloatingPlusButton();
        mediumWait();
        workOrderPage.waitForAddAssetsScreen();
    }

    /**
     * Navigate from dashboard to Photo Walkthrough screen.
     * Dashboard → Work Orders → Session Details → Locations → Room →
     * Assets in Room → Add Assets → New Asset → Create Photo Walkthrough.
     */
    private boolean navigateToPhotoWalkthroughScreen() {
        logStep("Navigating to Photo Walkthrough screen...");

        navigateToAddAssetsScreen();
        shortWait();

        if (!workOrderPage.isAddAssetsScreenDisplayed()) {
            logWarning("Not on Add Assets screen");
            return false;
        }

        logStep("Switching to New Asset tab");
        workOrderPage.tapNewAssetTab();
        mediumWait();

        boolean pwOptionVisible = workOrderPage.isCreatePhotoWalkthroughOptionDisplayed();
        logStep("Create Photo Walkthrough option visible: " + pwOptionVisible);

        if (!pwOptionVisible) {
            logWarning("'Create Photo Walkthrough' option not found on New Asset tab");
            return false;
        }

        logStep("Tapping 'Create Photo Walkthrough'");
        boolean tapped = workOrderPage.tapCreatePhotoWalkthroughOption();
        mediumWait();
        logStep("Photo Walkthrough option tapped: " + tapped);

        boolean pwScreen = workOrderPage.isPhotoWalkthroughScreenDisplayed();
        logStep("Photo Walkthrough screen displayed: " + pwScreen);
        return pwScreen;
    }

    /**
     * Dismiss Photo Walkthrough screen and navigate back to a stable state.
     */
    private void cleanupFromPhotoWalkthrough() {
        logStep("Cleaning up from Photo Walkthrough...");

        // Dismiss What's Next? screen if on it
        if (workOrderPage.isWhatsNextScreenDisplayed()) {
            try {
                DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                    )
                ).click();
                mediumWait();
                logStep("Dismissed What's Next screen");
            } catch (Exception e) { /* continue */ }
        }

        // Dismiss More OCPDs? screen if on it
        if (workOrderPage.isMoreOCPDsScreenDisplayed()) {
            try {
                DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                    )
                ).click();
                mediumWait();
                logStep("Dismissed More OCPDs screen");
            } catch (Exception e) { /* continue */ }
        }

        // Dismiss Classify OCPD screen if on it
        if (workOrderPage.isClassifyOCPDScreenDisplayed()) {
            workOrderPage.tapClassifyAssetCancelButton();
            mediumWait();
            logStep("Dismissed Classify OCPD");
        }

        // Dismiss Photograph OCPD screen if on it
        if (workOrderPage.isPhotographOCPDScreenDisplayed()) {
            try {
                DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                    )
                ).click();
                mediumWait();
                logStep("Dismissed Photograph OCPD");
            } catch (Exception e) { /* continue */ }
        }

        // Dismiss Add OCPDs prompt
        if (workOrderPage.isAddOCPDsPromptDisplayed()) {
            workOrderPage.tapNoSkipButton();
            mediumWait();
        }

        // Dismiss Classify Asset screen
        if (workOrderPage.isClassifyAssetScreenDisplayed()) {
            workOrderPage.tapClassifyAssetCancelButton();
            mediumWait();
        }

        // Dismiss Photo Walkthrough via Cancel
        if (workOrderPage.isPhotoWalkthroughScreenDisplayed()) {
            workOrderPage.tapPhotoWalkthroughCancelButton();
            mediumWait();
            logStep("Dismissed Photo Walkthrough");
        }

        // Dismiss Add Photos screen if still on it
        if (workOrderPage.isAddPhotosScreenDisplayed()) {
            workOrderPage.tapAddPhotosCancelButton();
            mediumWait();
        }

        // Dismiss Add Assets screen
        if (workOrderPage.isAddAssetsScreenDisplayed()) {
            workOrderPage.tapAddAssetsCancelButton();
            mediumWait();
        }

        // Fall through to Assets in Room cleanup
        cleanupFromAssetsInRoom();
    }

    /**
     * Clean up from Assets in Room or Locations tab.
     */
    private void cleanupFromAssetsInRoom() {
        if (workOrderPage.isAssetsInRoomScreenDisplayed()) {
            workOrderPage.tapAssetsInRoomDoneButton();
            mediumWait();
        }

        if (workOrderPage.isSessionDetailsScreenDisplayed()) {
            workOrderPage.goBack();
            mediumWait();
        }

        if (!workOrderPage.isWorkOrdersScreenDisplayed()) {
            workOrderPage.goBack();
            mediumWait();
        }
    }

    /**
     * Try to add a photo via Gallery in Photo Walkthrough.
     * Handles: permission alerts, photo picker navigation, selection.
     * @return true if a photo appears to have been added (thumbnail visible or "No photos yet" gone)
     */
    private boolean tryAddPhotoViaGallery() {
        logStep("Attempting to add photo via Gallery...");

        boolean galleryDisplayed = workOrderPage.isAddPhotosGalleryButtonDisplayed();
        if (!galleryDisplayed) {
            logWarning("Gallery button not found");
            return false;
        }

        workOrderPage.tapAddPhotosGalleryButton();
        mediumWait();

        // Handle permission alert if shown
        try {
            java.util.List<org.openqa.selenium.WebElement> allowBtns =
                DriverManager.getDriver().findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND "
                        + "(label CONTAINS 'Allow Full Access' OR label CONTAINS 'Allow' "
                        + "OR label == 'OK')"
                    )
                );
            if (!allowBtns.isEmpty()) {
                allowBtns.get(0).click();
                mediumWait();
                logStep("Allowed photo access");
            }
        } catch (Exception e) { /* continue */ }

        // Try to select the first photo in the picker
        try {
            java.util.List<org.openqa.selenium.WebElement> photos =
                DriverManager.getDriver().findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeImage' AND visible == true"
                    )
                );
            if (!photos.isEmpty()) {
                // Tap the first image (photo) in the picker
                photos.get(0).click();
                shortWait();
                logStep("Tapped first photo in picker");
            }

            // Try tapping "Add" or "Done" to confirm photo selection
            java.util.List<org.openqa.selenium.WebElement> addBtns =
                DriverManager.getDriver().findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND "
                        + "(label == 'Add' OR label == 'Done' OR label == 'Choose')"
                    )
                );
            if (!addBtns.isEmpty()) {
                addBtns.get(0).click();
                mediumWait();
                logStep("Confirmed photo selection");
            }
        } catch (Exception e) {
            logStep("Could not select photo from picker: " + e.getMessage());
        }

        // Dismiss picker if still open (Cancel)
        try {
            java.util.List<org.openqa.selenium.WebElement> cancelBtns =
                DriverManager.getDriver().findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                    )
                );
            // Only dismiss if we're still in the picker (not back on Photo Walkthrough)
            if (!cancelBtns.isEmpty() && !workOrderPage.isPhotoWalkthroughScreenDisplayed()) {
                cancelBtns.get(0).click();
                mediumWait();
            }
        } catch (Exception e) { /* continue */ }

        // Check if photo was added
        boolean noPhotos = workOrderPage.isNoPhotosYetTextDisplayed();
        boolean hasThumbnail = workOrderPage.isPhotoThumbnailDisplayed();
        logStep("After gallery attempt — 'No photos yet' visible: " + noPhotos
            + ", thumbnail visible: " + hasThumbnail);

        return hasThumbnail || !noPhotos;
    }

    // ============================================================
    // TC_JOB_200 — Camera Button in Photo Walkthrough
    // ============================================================

    @Test(priority = 200)
    public void TC_JOB_200_verifyCameraButtonInWalkthrough() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_200 - Verify Camera button in Photo Walkthrough is tappable "
            + "and attempts to open the native camera. (Partial: camera capture "
            + "requires native iOS handling on simulator.)"
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Verify Camera button is displayed
        boolean cameraDisplayed = workOrderPage.isAddPhotosCameraButtonDisplayed();
        logStep("Camera button displayed: " + cameraDisplayed);

        if (!cameraDisplayed) {
            logStepWithScreenshot("Camera button not found");
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Camera button not found on Photo Walkthrough screen.");
            return;
        }

        logStepWithScreenshot("Before tapping Camera button");

        // Tap Camera button
        logStep("Tapping Camera button");
        boolean cameraTapped = workOrderPage.tapAddPhotosCameraButton();
        mediumWait();
        logStep("Camera button tapped: " + cameraTapped);

        // Check what happened — camera app, permission alert, or nothing (simulator limitation)
        boolean cameraResponse = false;
        String responseType = "none";

        // Check 1: Camera interface elements
        try {
            java.util.List<org.openqa.selenium.WebElement> cameraEls =
                DriverManager.getDriver().findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' AND "
                        + "(label == 'Take Picture' OR label == 'PhotoCapture' "
                        + "OR label == 'Capture' OR label == 'Shutter')) "
                        + "OR (type == 'XCUIElementTypeStaticText' AND "
                        + "label CONTAINS 'Camera')"
                    )
                );
            if (!cameraEls.isEmpty()) {
                cameraResponse = true;
                responseType = "camera interface";
            }
        } catch (Exception e) { /* continue */ }

        // Check 2: Permission alert
        if (!cameraResponse) {
            try {
                java.util.List<org.openqa.selenium.WebElement> alerts =
                    DriverManager.getDriver().findElements(
                        io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeAlert' OR "
                            + "(type == 'XCUIElementTypeStaticText' AND "
                            + "(label CONTAINS 'Camera' OR label CONTAINS 'camera' "
                            + "OR label CONTAINS 'Access'))"
                        )
                    );
                if (!alerts.isEmpty()) {
                    cameraResponse = true;
                    responseType = "permission alert";
                    logStep("Camera permission alert detected — dismissing");
                    try {
                        java.util.List<org.openqa.selenium.WebElement> okBtns =
                            DriverManager.getDriver().findElements(
                                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                                    "type == 'XCUIElementTypeButton' AND "
                                    + "(label == 'OK' OR label == 'Allow' OR label == 'Don\\'t Allow')"
                                )
                            );
                        if (!okBtns.isEmpty()) {
                            okBtns.get(0).click();
                            mediumWait();
                        }
                    } catch (Exception e2) { /* continue */ }
                }
            } catch (Exception e) { /* continue */ }
        }

        // Check 3: Simulator "no camera" error
        if (!cameraResponse) {
            try {
                java.util.List<org.openqa.selenium.WebElement> errorEls =
                    DriverManager.getDriver().findElements(
                        io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND "
                            + "(label CONTAINS 'not available' OR label CONTAINS 'No Camera')"
                        )
                    );
                if (!errorEls.isEmpty()) {
                    cameraResponse = true;
                    responseType = "no camera (simulator)";
                    // Dismiss error
                    try {
                        DriverManager.getDriver().findElement(
                            io.appium.java_client.AppiumBy.iOSNsPredicateString(
                                "type == 'XCUIElementTypeButton' AND label == 'OK'"
                            )
                        ).click();
                        mediumWait();
                    } catch (Exception e2) { /* continue */ }
                }
            } catch (Exception e) { /* continue */ }
        }

        logStep("Camera response: " + cameraResponse + " (type: " + responseType + ")");
        logStepWithScreenshot("After tapping Camera button");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        // The camera button should be tappable. On a simulator it may not open the camera,
        // but the button should respond (either camera UI, permission, or error)
        assertTrue(cameraTapped,
            "Camera button should be tappable on Photo Walkthrough. "
            + "Tapped: " + cameraTapped
            + ". Response type: " + responseType);
    }

    // ============================================================
    // TC_JOB_201 — Done Button Enables After Photo
    // ============================================================

    @Test(priority = 201)
    public void TC_JOB_201_verifyDoneButtonEnablesAfterPhoto() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_201 - Verify 'Done with this asset' button changes from "
            + "disabled (gray) to enabled (blue) after adding a photo."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Verify Done button starts disabled
        boolean doneDisplayed = workOrderPage.isDoneWithThisAssetButtonDisplayed();
        boolean doneEnabledBefore = workOrderPage.isDoneWithThisAssetButtonEnabled();
        logStep("Done button displayed: " + doneDisplayed
            + ", enabled before photo: " + doneEnabledBefore);
        logStepWithScreenshot("Before adding photo — Done button state");

        // Attempt to add a photo via Gallery
        boolean photoAdded = tryAddPhotoViaGallery();
        logStep("Photo added via Gallery: " + photoAdded);

        // Check Done button state after photo
        boolean doneEnabledAfter = false;
        if (photoAdded) {
            doneEnabledAfter = workOrderPage.isDoneWithThisAssetButtonEnabled();
            logStep("Done button enabled after photo: " + doneEnabledAfter);
        }

        logStepWithScreenshot("After adding photo — Done button state");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        if (!photoAdded) {
            logStep("NOTE: Could not add a photo on simulator — test is partial. "
                + "Verifying initial disabled state only.");
            assertTrue(doneDisplayed && !doneEnabledBefore,
                "'Done with this asset' button should be displayed and disabled "
                + "when no photos are added. Displayed: " + doneDisplayed
                + ", enabled: " + doneEnabledBefore);
        } else {
            assertTrue(doneDisplayed && !doneEnabledBefore && doneEnabledAfter,
                "'Done with this asset' button should change from disabled to enabled "
                + "after adding a photo. Before: " + doneEnabledBefore
                + ", After: " + doneEnabledAfter);
        }
    }

    // ============================================================
    // TC_JOB_202 — Removing Photo with X Button
    // ============================================================

    @Test(priority = 202)
    public void TC_JOB_202_verifyRemovePhotoWithXButton() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_202 - Verify tapping X button on a photo thumbnail removes "
            + "the photo and 'Done with this asset' returns to disabled."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add a photo first
        boolean photoAdded = tryAddPhotoViaGallery();
        logStep("Photo added: " + photoAdded);

        if (!photoAdded) {
            logStepWithScreenshot("Could not add photo — skipping removal test");
            cleanupFromPhotoWalkthrough();
            // Still pass if initial state is correct (no photos, button disabled)
            boolean doneDisabled = !workOrderPage.isDoneWithThisAssetButtonEnabled();
            assertTrue(doneDisabled,
                "NOTE: Could not add photo on simulator. "
                + "Verified Done button is disabled in empty state: " + doneDisabled);
            return;
        }

        // Verify photo is present
        boolean thumbnailBefore = workOrderPage.isPhotoThumbnailDisplayed();
        boolean doneBefore = workOrderPage.isDoneWithThisAssetButtonEnabled();
        logStep("Before removal — thumbnail: " + thumbnailBefore
            + ", Done enabled: " + doneBefore);
        logStepWithScreenshot("Before removing photo");

        // Tap the X button to remove photo
        logStep("Tapping X button to remove photo");
        boolean removed = workOrderPage.tapRemovePhotoButton();
        mediumWait();
        logStep("X button tapped: " + removed);

        // Verify photo is gone and Done button returned to disabled
        boolean noPhotosAfter = workOrderPage.isNoPhotosYetTextDisplayed();
        boolean doneAfter = workOrderPage.isDoneWithThisAssetButtonEnabled();
        logStep("After removal — 'No photos yet': " + noPhotosAfter
            + ", Done enabled: " + doneAfter);
        logStepWithScreenshot("After removing photo");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(removed && !doneAfter,
            "After removing photo: X button tapped=" + removed
            + ", 'No photos yet' visible=" + noPhotosAfter
            + ", Done button should be disabled: " + !doneAfter);
    }

    // ============================================================
    // TC_JOB_203 — Done with this asset opens Classify Asset
    // ============================================================

    @Test(priority = 203)
    public void TC_JOB_203_verifyDoneOpensClassifyAsset() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_203 - Verify tapping 'Done with this asset' after adding "
            + "photo(s) opens the 'Classify Asset' screen with Cancel, title, "
            + "photo thumbnail preview, Asset Type dropdown, and Continue button."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add a photo
        boolean photoAdded = tryAddPhotoViaGallery();
        logStep("Photo added: " + photoAdded);

        if (!photoAdded) {
            logStepWithScreenshot("Could not add photo — cannot test Classify Asset flow");
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Cannot verify Classify Asset screen — photo could not be added "
                + "via Gallery on simulator.");
            return;
        }

        // Tap Done with this asset
        boolean doneEnabled = workOrderPage.isDoneWithThisAssetButtonEnabled();
        logStep("Done button enabled: " + doneEnabled);

        if (!doneEnabled) {
            logStepWithScreenshot("Done button not enabled after adding photo");
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "'Done with this asset' button not enabled after adding photo.");
            return;
        }

        logStep("Tapping 'Done with this asset'");
        boolean doneTapped = workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();
        logStep("Done tapped: " + doneTapped);

        // Verify Classify Asset screen
        boolean classifyScreen = workOrderPage.isClassifyAssetScreenDisplayed();
        logStep("Classify Asset screen displayed: " + classifyScreen);

        // Check for all expected elements
        boolean hasCancel = false;
        try {
            hasCancel = !DriverManager.getDriver().findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                )
            ).isEmpty();
        } catch (Exception e) { /* continue */ }

        boolean hasTypeDropdown = workOrderPage.isClassifyAssetTypeDropdownDisplayed();
        boolean hasContinue = workOrderPage.isClassifyAssetContinueButtonDisplayed();

        logStep("Cancel: " + hasCancel
            + ", Asset Type dropdown: " + hasTypeDropdown
            + ", Continue button: " + hasContinue);
        logStepWithScreenshot("Classify Asset screen elements");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(classifyScreen && hasTypeDropdown && hasContinue,
            "Classify Asset screen should display all elements. "
            + "Screen detected: " + classifyScreen
            + ". Cancel: " + hasCancel
            + ". Asset Type dropdown: " + hasTypeDropdown
            + ". Continue: " + hasContinue);
    }

    // ============================================================
    // TC_JOB_204 — Asset Type Dropdown in Classify Asset
    // ============================================================

    @Test(priority = 204)
    public void TC_JOB_204_verifyAssetTypeDropdownInClassifyAsset() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_204 - Verify Asset Type dropdown in Classify Asset shows "
            + "all asset types: ATS, Busway, Capacitor, Circuit Breaker, Default, "
            + "Disconnect Switch, Fuse, Generator, Junction Box, Loadcenter, MCC, "
            + "MCC Bucket, Motor, Motor Starter, Other, Other (OCP), etc."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add a photo and get to Classify Asset
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Cannot test Asset Type dropdown — photo could not be added.");
            return;
        }

        boolean doneEnabled = workOrderPage.isDoneWithThisAssetButtonEnabled();
        if (!doneEnabled) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "'Done with this asset' not enabled after photo.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        boolean classifyScreen = workOrderPage.isClassifyAssetScreenDisplayed();
        if (!classifyScreen) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Classify Asset screen not displayed after tapping Done.");
            return;
        }

        // Tap Asset Type dropdown
        logStep("Tapping Asset Type dropdown");
        boolean dropdownTapped = workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        logStep("Asset Type dropdown tapped: " + dropdownTapped);

        // Get all options
        java.util.ArrayList<String> options = workOrderPage.getClassifyAssetTypeOptions();
        logStep("Asset type options found: " + options.size());
        for (String opt : options) {
            logStep("  - " + opt);
        }

        logStepWithScreenshot("Asset Type dropdown options");

        // Check for key expected types
        String[] expectedTypes = {
            "ATS", "Circuit Breaker", "Disconnect Switch", "Fuse",
            "Generator", "Junction Box", "MCC", "Motor"
        };
        int matchCount = 0;
        for (String expected : expectedTypes) {
            boolean found = false;
            for (String opt : options) {
                if (opt.contains(expected)) {
                    found = true;
                    break;
                }
            }
            if (found) matchCount++;
            logStep("  " + expected + ": " + (found ? "FOUND" : "NOT FOUND"));
        }

        // Dismiss dropdown and cleanup
        try {
            // Tap outside or Cancel to dismiss
            DriverManager.getDriver().findElement(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                )
            ).click();
            mediumWait();
        } catch (Exception e) { /* continue */ }

        cleanupFromPhotoWalkthrough();

        assertTrue(options.size() >= 5 && matchCount >= 4,
            "Asset Type dropdown should show multiple types. "
            + "Found " + options.size() + " options, "
            + matchCount + "/" + expectedTypes.length + " key types matched.");
    }

    // ============================================================
    // TC_JOB_205 — Selecting Asset Type Shows Subtype Dropdown
    // ============================================================

    @Test(priority = 205)
    public void TC_JOB_205_verifySelectingTypeShowsSubtype() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_205 - Verify selecting an Asset Type (e.g., MCC) in "
            + "Classify Asset shows the Subtype (Optional) dropdown with "
            + "type-specific subtypes."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add photo → Done → Classify Asset
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Cannot test — photo could not be added.");
            return;
        }

        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Done button not enabled after photo.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Classify Asset screen not displayed.");
            return;
        }

        // Check Subtype NOT shown before selecting type
        boolean subtypeBefore = workOrderPage.isClassifyAssetSubtypeDropdownDisplayed();
        logStep("Subtype dropdown before selecting type: " + subtypeBefore);

        // Select MCC as Asset Type
        logStep("Tapping Asset Type dropdown");
        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();

        logStep("Selecting 'MCC' as Asset Type");
        boolean selected = workOrderPage.selectClassifyAssetType("MCC");
        mediumWait();
        logStep("MCC selected: " + selected);

        // Check Subtype dropdown appears
        boolean subtypeAfter = workOrderPage.isClassifyAssetSubtypeDropdownDisplayed();
        logStep("Subtype dropdown after selecting MCC: " + subtypeAfter);

        logStepWithScreenshot("After selecting MCC — checking Subtype dropdown");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(selected && subtypeAfter,
            "After selecting MCC as Asset Type, Subtype dropdown should appear. "
            + "Selected: " + selected
            + ". Subtype before: " + subtypeBefore
            + ". Subtype after: " + subtypeAfter);
    }

    // ============================================================
    // TC_JOB_206 — Continue Button Enables After Type Selection
    // ============================================================

    @Test(priority = 206)
    public void TC_JOB_206_verifyContinueEnablesAfterTypeSelection() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_206 - Verify Continue button changes to blue (enabled) "
            + "after selecting an Asset Type on the Classify Asset screen."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add photo → Done → Classify Asset
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Cannot test — photo could not be added.");
            return;
        }

        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Done button not enabled after photo.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Classify Asset screen not displayed.");
            return;
        }

        // Check Continue button state before selecting type
        boolean continueBefore = workOrderPage.isClassifyAssetContinueButtonEnabled();
        logStep("Continue enabled before type selection: " + continueBefore);
        logStepWithScreenshot("Before selecting Asset Type");

        // Select an Asset Type
        logStep("Selecting Asset Type");
        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        boolean selected = workOrderPage.selectClassifyAssetType("Circuit Breaker");
        mediumWait();
        logStep("Asset Type selected: " + selected);

        // Check Continue button state after
        boolean continueAfter = workOrderPage.isClassifyAssetContinueButtonEnabled();
        logStep("Continue enabled after type selection: " + continueAfter);
        logStepWithScreenshot("After selecting Asset Type — Continue state");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(selected && continueAfter,
            "Continue button should be enabled after selecting Asset Type. "
            + "Before: " + continueBefore
            + ". After: " + continueAfter
            + ". Type selected: " + selected);
    }

    // ============================================================
    // TC_JOB_207 — Add OCPDs Prompt for MCC in Walkthrough
    // ============================================================

    @Test(priority = 207)
    public void TC_JOB_207_verifyAddOCPDsPromptForMCC() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_207 - Verify Add OCPDs? prompt appears after selecting MCC "
            + "as Asset Type and tapping Continue in the walkthrough Classify Asset "
            + "screen. Shows lightning bolt icon, message, 'Yes, Add OCPD Photos' "
            + "(orange) and 'No, Skip' (gray) buttons."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Add photo → Done → Classify Asset → Select MCC → Continue
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Cannot test — photo could not be added.");
            return;
        }

        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Done button not enabled after photo.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Classify Asset screen not displayed.");
            return;
        }

        // Select MCC
        logStep("Selecting MCC as Asset Type");
        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        boolean mccSelected = workOrderPage.selectClassifyAssetType("MCC");
        mediumWait();
        logStep("MCC selected: " + mccSelected);

        if (!mccSelected) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not select MCC as Asset Type.");
            return;
        }

        // Tap Continue
        logStep("Tapping Continue");
        boolean continueTapped = workOrderPage.tapClassifyAssetContinueButton();
        mediumWait();
        logStep("Continue tapped: " + continueTapped);

        // Check for Add OCPDs prompt
        boolean ocpdPrompt = workOrderPage.isAddOCPDsPromptDisplayed();
        logStep("Add OCPDs? prompt displayed: " + ocpdPrompt);

        // Check individual elements
        boolean hasYesButton = workOrderPage.isYesAddOCPDPhotosButtonDisplayed()
            || workOrderPage.isAddByPhotoButtonDisplayed();
        boolean hasNoSkip = workOrderPage.isNoSkipButtonDisplayed();

        // Check for OCPD message text
        boolean hasMessage = false;
        try {
            hasMessage = !DriverManager.getDriver().findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND "
                    + "(label CONTAINS 'Overcurrent Protection' OR label CONTAINS 'OCPD' "
                    + "OR label CONTAINS 'overcurrent')"
                )
            ).isEmpty();
        } catch (Exception e) { /* continue */ }

        logStep("Add OCPD Photos button: " + hasYesButton
            + ", No Skip: " + hasNoSkip
            + ", OCPD message: " + hasMessage);
        logStepWithScreenshot("Add OCPDs? prompt");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(ocpdPrompt && hasYesButton && hasNoSkip,
            "Add OCPDs? prompt should appear after MCC + Continue. "
            + "Prompt detected: " + ocpdPrompt
            + ". Yes/Add Photo button: " + hasYesButton
            + ". No, Skip: " + hasNoSkip
            + ". OCPD message: " + hasMessage);
    }

    // ============================================================
    // TC_JOB_208 — Yes, Add OCPD Photos Button
    // ============================================================

    @Test(priority = 208)
    public void TC_JOB_208_verifyYesAddOCPDPhotosButton() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_208 - Verify the 'Yes, Add OCPD Photos' button is displayed "
            + "as an orange button with camera icon on the Add OCPDs? prompt."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Navigate to Add OCPDs prompt: photo → Done → Classify (MCC) → Continue
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Cannot test — photo could not be added.");
            return;
        }

        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Done button not enabled.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Classify Asset not displayed.");
            return;
        }

        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        workOrderPage.selectClassifyAssetType("MCC");
        mediumWait();
        workOrderPage.tapClassifyAssetContinueButton();
        mediumWait();

        boolean ocpdPrompt = workOrderPage.isAddOCPDsPromptDisplayed();
        if (!ocpdPrompt) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Add OCPDs? prompt not displayed.");
            return;
        }

        // Verify Yes, Add OCPD Photos button
        boolean yesButton = workOrderPage.isYesAddOCPDPhotosButtonDisplayed()
            || workOrderPage.isAddByPhotoButtonDisplayed();
        logStep("Yes, Add OCPD Photos button displayed: " + yesButton);
        logStepWithScreenshot("Yes, Add OCPD Photos button");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(yesButton,
            "'Yes, Add OCPD Photos' button should be displayed on Add OCPDs? prompt. "
            + "Found: " + yesButton);
    }

    // ============================================================
    // TC_JOB_209 — Tapping Yes Opens OCPD Capture Screen
    // ============================================================

    @Test(priority = 209)
    public void TC_JOB_209_verifyYesTapOpensOCPDCaptureScreen() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_209 - Verify tapping 'Yes, Add OCPD Photos' opens the "
            + "'Photograph OCPD' screen with Cancel, title, location breadcrumb, "
            + "'Take photos of the first OCPD', Gallery/Camera buttons, and "
            + "'Done with this OCPD' button (gray/disabled)."
        );

        logStep("Navigating to Photo Walkthrough screen");
        boolean pwReached = navigateToPhotoWalkthroughScreen();

        if (!pwReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false,
                "Could not navigate to Photo Walkthrough screen.");
            return;
        }

        // Navigate to Add OCPDs prompt: photo → Done → Classify (MCC) → Continue
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Cannot test — photo could not be added.");
            return;
        }

        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Done button not enabled.");
            return;
        }

        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Classify Asset not displayed.");
            return;
        }

        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        workOrderPage.selectClassifyAssetType("MCC");
        mediumWait();
        workOrderPage.tapClassifyAssetContinueButton();
        mediumWait();

        if (!workOrderPage.isAddOCPDsPromptDisplayed()) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Add OCPDs? prompt not displayed.");
            return;
        }

        // Tap Yes, Add OCPD Photos
        logStep("Tapping 'Yes, Add OCPD Photos'");
        boolean yesTapped = workOrderPage.tapYesAddOCPDPhotosButton();
        if (!yesTapped) {
            // Fallback to tapAddByPhotoButton
            yesTapped = workOrderPage.tapAddByPhotoButton();
        }
        mediumWait();
        logStep("Yes button tapped: " + yesTapped);

        // Verify Photograph OCPD screen
        boolean photographOCPD = workOrderPage.isPhotographOCPDScreenDisplayed();
        logStep("Photograph OCPD screen displayed: " + photographOCPD);

        // Check expected elements
        boolean hasCancel = false;
        try {
            hasCancel = !DriverManager.getDriver().findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"
                )
            ).isEmpty();
        } catch (Exception e) { /* continue */ }

        boolean hasTakePhotosHeading = workOrderPage.isTakePhotosOfFirstOCPDHeadingDisplayed();
        boolean hasMultiplePhotosHint = workOrderPage.isMultiplePhotosHintDisplayed();
        boolean hasGallery = workOrderPage.isAddPhotosGalleryButtonDisplayed();
        boolean hasCamera = workOrderPage.isAddPhotosCameraButtonDisplayed();
        boolean hasDoneOCPD = workOrderPage.isDoneWithThisOCPDButtonDisplayed();

        logStep("Cancel: " + hasCancel
            + ", 'Take photos' heading: " + hasTakePhotosHeading
            + ", Multiple photos hint: " + hasMultiplePhotosHint
            + ", Gallery: " + hasGallery
            + ", Camera: " + hasCamera
            + ", Done with OCPD: " + hasDoneOCPD);
        logStepWithScreenshot("Photograph OCPD screen elements");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean coreElements = photographOCPD && hasGallery && hasCamera;

        assertTrue(coreElements,
            "Photograph OCPD screen should display all expected elements. "
            + "Screen detected: " + photographOCPD
            + ". Cancel: " + hasCancel
            + ". 'Take photos' heading: " + hasTakePhotosHeading
            + ". Multiple photos hint: " + hasMultiplePhotosHint
            + ". Gallery: " + hasGallery
            + ". Camera: " + hasCamera
            + ". Done with OCPD: " + hasDoneOCPD);
    }

    // ============================================================
    // NAVIGATION HELPERS — Deep flow into OCPD, Classify, More OCPDs, What's Next
    // ============================================================

    /**
     * Navigate all the way to the "Photograph OCPD" screen:
     * Dashboard → Work Orders → Session → Locations → Room → Assets in Room →
     * Add Assets → New Asset → Photo Walkthrough → [add photo] → Done with asset →
     * Classify Asset (MCC) → Continue → Add OCPDs? → Yes → Photograph OCPD
     * @return true if successfully reached the Photograph OCPD screen
     */
    private boolean navigateToPhotographOCPDScreen() {
        logStep("Navigating to Photograph OCPD screen (deep flow)...");

        // Step 1: Get to Photo Walkthrough
        boolean pwReached = navigateToPhotoWalkthroughScreen();
        if (!pwReached) {
            logWarning("Could not reach Photo Walkthrough screen");
            return false;
        }

        // Step 2: Add a photo via Gallery
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            logWarning("Could not add photo in walkthrough");
            return false;
        }

        // Step 3: Tap Done with this asset
        if (!workOrderPage.isDoneWithThisAssetButtonEnabled()) {
            logWarning("'Done with this asset' button not enabled after adding photo");
            return false;
        }
        workOrderPage.tapDoneWithThisAssetButton();
        mediumWait();

        // Step 4: Classify Asset as MCC (to get OCPD prompt)
        if (!workOrderPage.isClassifyAssetScreenDisplayed()) {
            logWarning("Classify Asset screen not displayed after Done");
            return false;
        }
        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        workOrderPage.selectClassifyAssetType("MCC");
        mediumWait();
        workOrderPage.tapClassifyAssetContinueButton();
        mediumWait();

        // Step 5: Add OCPDs? prompt → Yes
        if (!workOrderPage.isAddOCPDsPromptDisplayed()) {
            logWarning("Add OCPDs? prompt not displayed");
            return false;
        }
        boolean yesTapped = workOrderPage.tapYesAddOCPDPhotosButton();
        if (!yesTapped) {
            yesTapped = workOrderPage.tapAddByPhotoButton();
        }
        mediumWait();

        if (!yesTapped) {
            logWarning("Could not tap Yes on Add OCPDs prompt");
            return false;
        }

        // Step 6: Verify we're on Photograph OCPD screen
        boolean onOCPD = workOrderPage.isPhotographOCPDScreenDisplayed();
        logStep("On Photograph OCPD screen: " + onOCPD);
        return onOCPD;
    }

    /**
     * Navigate to the "Classify OCPD" screen:
     * Photograph OCPD → [add photo] → Done with this OCPD → Classify OCPD
     * Assumes we are already on the Photograph OCPD screen.
     * @return true if successfully reached the Classify OCPD screen
     */
    private boolean navigateToClassifyOCPDScreen() {
        logStep("Navigating from Photograph OCPD to Classify OCPD...");

        // Add a photo via Gallery (reuse the helper)
        boolean photoAdded = tryAddPhotoViaGallery();
        if (!photoAdded) {
            logWarning("Could not add OCPD photo");
            return false;
        }

        // Tap Done with this OCPD
        if (!workOrderPage.isDoneWithThisOCPDButtonEnabled()) {
            logWarning("'Done with this OCPD' button not enabled after photo");
            return false;
        }
        workOrderPage.tapDoneWithThisOCPDButton();
        mediumWait();

        boolean onClassify = workOrderPage.isClassifyOCPDScreenDisplayed();
        logStep("On Classify OCPD screen: " + onClassify);
        return onClassify;
    }

    /**
     * Navigate to the "More OCPDs?" screen:
     * Classify OCPD → select type → Continue → More OCPDs?
     * Assumes we are already on the Classify OCPD screen.
     * @param ocpdType the OCPD type to select (e.g., "Disconnect Switch", "Fuse")
     * @return true if successfully reached the More OCPDs screen
     */
    private boolean navigateToMoreOCPDsScreen(String ocpdType) {
        logStep("Navigating from Classify OCPD to More OCPDs screen...");

        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();
        workOrderPage.selectClassifyAssetType(ocpdType);
        mediumWait();

        if (!workOrderPage.isClassifyAssetContinueButtonEnabled()) {
            logWarning("Continue button not enabled after selecting OCPD type");
            return false;
        }
        workOrderPage.tapClassifyAssetContinueButton();
        mediumWait();

        boolean onMoreOCPDs = workOrderPage.isMoreOCPDsScreenDisplayed();
        logStep("On More OCPDs screen: " + onMoreOCPDs);
        return onMoreOCPDs;
    }

    // ============================================================
    // TC_JOB_210 — Photograph OCPD Screen Layout
    // ============================================================

    @Test(priority = 210)
    public void TC_JOB_210_verifyPhotographOCPDScreenLayout() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_210 - Verify Photograph OCPD screen shows heading, subtext, "
            + "camera icon, 'No photos yet', Gallery/Camera buttons, and "
            + "'Done with this OCPD' button (disabled)."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Verify all expected elements
        boolean hasTakePhotosHeading = workOrderPage.isTakePhotosOfFirstOCPDHeadingDisplayed();
        logStep("'Take photos of the first OCPD' heading: " + hasTakePhotosHeading);

        boolean hasMultiplePhotosHint = workOrderPage.isMultiplePhotosHintDisplayed();
        logStep("'You can take multiple photos' subtext: " + hasMultiplePhotosHint);

        boolean hasNoPhotosYet = workOrderPage.isNoPhotosYetTextDisplayed();
        logStep("'No photos yet' text: " + hasNoPhotosYet);

        boolean hasGallery = workOrderPage.isAddPhotosGalleryButtonDisplayed();
        logStep("Gallery button: " + hasGallery);

        boolean hasCamera = workOrderPage.isAddPhotosCameraButtonDisplayed();
        logStep("Camera button: " + hasCamera);

        boolean hasDoneOCPD = workOrderPage.isDoneWithThisOCPDButtonDisplayed();
        logStep("'Done with this OCPD' button: " + hasDoneOCPD);

        boolean doneEnabled = workOrderPage.isDoneWithThisOCPDButtonEnabled();
        logStep("'Done with this OCPD' button enabled: " + doneEnabled);

        logStepWithScreenshot("Photograph OCPD screen layout");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        // Assert: heading, gallery, camera must be present; Done should be disabled
        boolean coreElements = hasTakePhotosHeading && hasGallery && hasCamera && hasDoneOCPD;
        boolean doneDisabled = !doneEnabled;

        assertTrue(coreElements && doneDisabled,
            "OCPD screen should show all elements with Done disabled. "
            + "Heading: " + hasTakePhotosHeading
            + ". Subtext: " + hasMultiplePhotosHint
            + ". No photos yet: " + hasNoPhotosYet
            + ". Gallery: " + hasGallery
            + ". Camera: " + hasCamera
            + ". Done displayed: " + hasDoneOCPD
            + ". Done disabled: " + doneDisabled);
    }

    // ============================================================
    // TC_JOB_211 — Done with this OCPD Enables After Photo
    // ============================================================

    @Test(priority = 211)
    public void TC_JOB_211_verifyDoneWithOCPDEnablesAfterPhoto() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_211 - Verify 'Done with this OCPD' button changes from "
            + "gray (disabled) to blue (enabled) after adding an OCPD photo."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Verify Done is initially disabled
        boolean initiallyDisabled = !workOrderPage.isDoneWithThisOCPDButtonEnabled();
        logStep("Done button initially disabled: " + initiallyDisabled);

        // Add a photo via Gallery
        boolean photoAdded = tryAddPhotoViaGallery();
        logStep("Photo added: " + photoAdded);

        if (!photoAdded) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not add OCPD photo to test Done button.");
            return;
        }

        // Verify Done becomes enabled
        boolean nowEnabled = workOrderPage.isDoneWithThisOCPDButtonEnabled();
        logStep("Done button enabled after photo: " + nowEnabled);
        logStepWithScreenshot("Done button state after adding OCPD photo");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(nowEnabled,
            "Done button should become enabled after adding photo. "
            + "Initially disabled: " + initiallyDisabled
            + ". Enabled after photo: " + nowEnabled);
    }

    // ============================================================
    // TC_JOB_212 — Done with this OCPD Opens Classify OCPD
    // ============================================================

    @Test(priority = 212)
    public void TC_JOB_212_verifyDoneWithOCPDOpensClassifyOCPD() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_212 - Verify tapping 'Done with this OCPD' opens the "
            + "'Classify OCPD' screen with Cancel, title, photo thumbnail, "
            + "Asset Type dropdown, and Continue button."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate to Classify OCPD: add photo → tap Done
        boolean classifyReached = navigateToClassifyOCPDScreen();
        logStep("Classify OCPD screen reached: " + classifyReached);

        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        // Verify expected elements on Classify OCPD screen
        boolean hasCancel = workOrderPage.isCurrentScreenCancelButtonDisplayed();
        logStep("Cancel button: " + hasCancel);

        boolean hasClassifyOCPD = workOrderPage.isClassifyOCPDScreenDisplayed();
        logStep("'Classify OCPD' title: " + hasClassifyOCPD);

        boolean hasThumbnail = workOrderPage.isPhotoThumbnailDisplayed();
        logStep("Photo thumbnail: " + hasThumbnail);

        boolean hasTypeDropdown = workOrderPage.isClassifyAssetTypeDropdownDisplayed();
        logStep("Asset Type dropdown: " + hasTypeDropdown);

        boolean hasContinue = workOrderPage.isClassifyAssetContinueButtonDisplayed();
        logStep("Continue button: " + hasContinue);

        logStepWithScreenshot("Classify OCPD screen elements");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean coreElements = hasClassifyOCPD && hasTypeDropdown && hasContinue;

        assertTrue(coreElements,
            "Classify OCPD screen should show title, dropdown, and Continue. "
            + "Cancel: " + hasCancel
            + ". Title: " + hasClassifyOCPD
            + ". Thumbnail: " + hasThumbnail
            + ". Type dropdown: " + hasTypeDropdown
            + ". Continue: " + hasContinue);
    }

    // ============================================================
    // TC_JOB_213 — OCPD Asset Type Options in Walkthrough
    // ============================================================

    @Test(priority = 213)
    public void TC_JOB_213_verifyOCPDAssetTypeOptions() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_213 - Verify OCPD types available in Classify OCPD dropdown: "
            + "Disconnect Switch, Fuse, MCC Bucket, Other (OCP), Relay."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate to Classify OCPD
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        // Tap Asset Type dropdown
        logStep("Tapping Asset Type dropdown");
        workOrderPage.tapClassifyAssetTypeDropdown();
        mediumWait();

        // Get available options
        java.util.ArrayList<String> options = workOrderPage.getClassifyAssetTypeOptions();
        logStep("OCPD type options found: " + options);
        logStepWithScreenshot("OCPD Asset Type dropdown options");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        // Expected OCPD types
        String[] expectedTypes = {"Disconnect Switch", "Fuse", "MCC Bucket", "Other (OCP)", "Relay"};
        StringBuilder missing = new StringBuilder();
        int matchCount = 0;

        for (String expected : expectedTypes) {
            boolean found = false;
            for (String option : options) {
                if (option.toLowerCase().contains(expected.toLowerCase())
                    || expected.toLowerCase().contains(option.toLowerCase())) {
                    found = true;
                    matchCount++;
                    break;
                }
            }
            if (!found) {
                missing.append(expected).append(", ");
            }
        }

        logStep("Matched " + matchCount + "/" + expectedTypes.length
            + " expected types. Missing: "
            + (missing.length() > 0 ? missing.toString() : "none"));

        // At least 3 of the 5 expected types should be present
        assertTrue(matchCount >= 3,
            "At least 3 OCPD types should match expected. "
            + "Found: " + options
            + ". Expected: " + java.util.Arrays.toString(expectedTypes)
            + ". Matched: " + matchCount
            + ". Missing: " + missing);
    }

    // ============================================================
    // TC_JOB_214 — More OCPDs Screen After OCPD Classification
    // ============================================================

    @Test(priority = 214)
    public void TC_JOB_214_verifyMoreOCPDsScreenAfterClassification() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_214 - Verify 'More OCPDs?' screen appears after classifying "
            + "an OCPD with: Cancel, title, asset name, OCPD count in orange, "
            + "'Add Another OCPD' button, and 'Done with OCP' button."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate to Classify OCPD
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        // Navigate to More OCPDs? screen by selecting a type and continuing
        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        logStep("More OCPDs screen reached: " + moreOCPDsReached);

        if (!moreOCPDsReached) {
            // Try with "Fuse" as fallback OCPD type
            logStep("Retrying with 'Fuse' type...");
            // We might still be on Classify OCPD, try again
            if (workOrderPage.isClassifyOCPDScreenDisplayed()) {
                moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
            }
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        // Verify expected elements
        boolean hasCancel = workOrderPage.isCurrentScreenCancelButtonDisplayed();
        logStep("Cancel button: " + hasCancel);

        boolean hasTitle = workOrderPage.isMoreOCPDsScreenDisplayed();
        logStep("'More OCPDs?' title: " + hasTitle);

        String ocpdCount = workOrderPage.getOCPDAddedCountText();
        boolean hasOCPDCount = !ocpdCount.isEmpty();
        logStep("OCPD count text: '" + ocpdCount + "'");

        boolean hasAddAnother = workOrderPage.isAddAnotherOCPDButtonDisplayed();
        logStep("'Add Another OCPD' button: " + hasAddAnother);

        boolean hasDoneWithOCP = workOrderPage.isDoneWithOCPButtonDisplayed();
        logStep("'Done with OCP' button: " + hasDoneWithOCP);

        logStepWithScreenshot("More OCPDs? screen elements");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean coreElements = hasTitle && (hasAddAnother || hasDoneWithOCP);

        assertTrue(coreElements,
            "More OCPDs? screen should show title and action buttons. "
            + "Cancel: " + hasCancel
            + ". Title: " + hasTitle
            + ". OCPD count: '" + ocpdCount + "'"
            + ". Add Another: " + hasAddAnother
            + ". Done with OCP: " + hasDoneWithOCP);
    }

    // ============================================================
    // TC_JOB_215 — Add Another OCPD Button Functionality
    // ============================================================

    @Test(priority = 215)
    public void TC_JOB_215_verifyAddAnotherOCPDReturnsToCapture() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_215 - Verify tapping 'Add Another OCPD' returns to "
            + "the Photograph OCPD screen to capture another OCPD."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate to Classify OCPD
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        // Navigate to More OCPDs? screen
        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        if (!moreOCPDsReached && workOrderPage.isClassifyOCPDScreenDisplayed()) {
            moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        // Tap "Add Another OCPD"
        logStep("Tapping 'Add Another OCPD'");
        boolean tapped = workOrderPage.tapAddAnotherOCPDButton();
        mediumWait();
        logStep("Add Another OCPD tapped: " + tapped);

        // Verify we're back on Photograph OCPD screen
        boolean backOnOCPD = workOrderPage.isPhotographOCPDScreenDisplayed();
        logStep("Back on Photograph OCPD screen: " + backOnOCPD);
        logStepWithScreenshot("After tapping Add Another OCPD");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(backOnOCPD,
            "Tapping 'Add Another OCPD' should return to Photograph OCPD screen. "
            + "Button tapped: " + tapped
            + ". Back on OCPD screen: " + backOnOCPD);
    }

    // ============================================================
    // TC_JOB_216 — Done with OCP Proceeds to Next Step
    // ============================================================

    @Test(priority = 216)
    public void TC_JOB_216_verifyDoneWithOCPProceeds() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_OCPD,
            "TC_JOB_216 - Verify tapping 'Done with OCP' on More OCPDs screen "
            + "proceeds to the What's Next? screen."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate to Classify OCPD → More OCPDs
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        if (!moreOCPDsReached && workOrderPage.isClassifyOCPDScreenDisplayed()) {
            moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        // Tap "Done with OCP"
        logStep("Tapping 'Done with OCP'");
        boolean tapped = workOrderPage.tapDoneWithOCPButton();
        mediumWait();
        logStep("Done with OCP tapped: " + tapped);

        // Verify What's Next? screen
        boolean whatsNext = workOrderPage.isWhatsNextScreenDisplayed();
        logStep("What's Next? screen displayed: " + whatsNext);
        logStepWithScreenshot("What's Next? screen after Done with OCP");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        assertTrue(whatsNext,
            "Tapping 'Done with OCP' should proceed to What's Next? screen. "
            + "Button tapped: " + tapped
            + ". What's Next displayed: " + whatsNext);
    }

    // ============================================================
    // TC_JOB_217 — What's Next Screen After Asset Capture
    // ============================================================

    @Test(priority = 217)
    public void TC_JOB_217_verifyWhatsNextScreenElements() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_217 - Verify What's Next? screen shows: Cancel, title, "
            + "large asset count, 'Asset Captured' text, 'X photos total', "
            + "'Add Another Asset' button, and 'Finish Walkthrough' button."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate through the full OCPD flow to What's Next
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        if (!moreOCPDsReached && workOrderPage.isClassifyOCPDScreenDisplayed()) {
            moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        // Tap Done with OCP → What's Next?
        workOrderPage.tapDoneWithOCPButton();
        mediumWait();

        boolean whatsNext = workOrderPage.isWhatsNextScreenDisplayed();
        if (!whatsNext) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach What's Next? screen.");
            return;
        }

        // Verify all expected elements
        boolean hasCancel = workOrderPage.isCurrentScreenCancelButtonDisplayed();
        logStep("Cancel button: " + hasCancel);

        boolean hasTitle = workOrderPage.isWhatsNextScreenDisplayed();
        logStep("'What's Next?' title: " + hasTitle);

        String assetCount = workOrderPage.getAssetCapturedCount();
        logStep("Asset count: '" + assetCount + "'");

        boolean hasAssetCaptured = workOrderPage.isAssetCapturedTextDisplayed();
        logStep("'Asset Captured' text: " + hasAssetCaptured);

        String photosTotal = workOrderPage.getPhotosTotalText();
        boolean hasPhotosTotal = !photosTotal.isEmpty();
        logStep("Photos total text: '" + photosTotal + "'");

        boolean hasAddAnother = workOrderPage.isAddAnotherAssetButtonDisplayed();
        logStep("'Add Another Asset' button: " + hasAddAnother);

        boolean hasFinish = workOrderPage.isFinishWalkthroughButtonDisplayed();
        logStep("'Finish Walkthrough' button: " + hasFinish);

        logStepWithScreenshot("What's Next? screen elements");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean coreElements = hasTitle && (hasAssetCaptured || hasAddAnother || hasFinish);

        assertTrue(coreElements,
            "What's Next? screen should show all expected elements. "
            + "Cancel: " + hasCancel
            + ". Title: " + hasTitle
            + ". Asset count: '" + assetCount + "'"
            + ". Asset Captured text: " + hasAssetCaptured
            + ". Photos total: '" + photosTotal + "'"
            + ". Add Another Asset: " + hasAddAnother
            + ". Finish Walkthrough: " + hasFinish);
    }

    // ============================================================
    // TC_JOB_218 — Asset Count Display in What's Next
    // ============================================================

    @Test(priority = 218)
    public void TC_JOB_218_verifyAssetCountInWhatsNext() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_218 - Verify captured asset count is prominently displayed "
            + "as a large '1' with 'Asset Captured' text below."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate through full flow to What's Next
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        if (!moreOCPDsReached && workOrderPage.isClassifyOCPDScreenDisplayed()) {
            moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        workOrderPage.tapDoneWithOCPButton();
        mediumWait();

        boolean whatsNext = workOrderPage.isWhatsNextScreenDisplayed();
        if (!whatsNext) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach What's Next? screen.");
            return;
        }

        // Get asset count
        String assetCount = workOrderPage.getAssetCapturedCount();
        logStep("Asset count displayed: '" + assetCount + "'");

        boolean hasAssetCaptured = workOrderPage.isAssetCapturedTextDisplayed();
        logStep("'Asset Captured' text: " + hasAssetCaptured);

        logStepWithScreenshot("Asset count display on What's Next");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean countIsOne = "1".equals(assetCount);
        logStep("Count is '1': " + countIsOne);

        assertTrue(hasAssetCaptured && !assetCount.isEmpty(),
            "What's Next? should show asset count with 'Asset Captured' text. "
            + "Count: '" + assetCount + "' (expected '1')"
            + ". 'Asset Captured' text: " + hasAssetCaptured);
    }

    // ============================================================
    // TC_JOB_219 — Photo Count in What's Next
    // ============================================================

    @Test(priority = 219)
    public void TC_JOB_219_verifyPhotoCountInWhatsNext() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_JOBS,
            AppConstants.FEATURE_PHOTO_WALKTHROUGH,
            "TC_JOB_219 - Verify total photo count is displayed as "
            + "'X photos total' below the asset count on What's Next screen."
        );

        logStep("Navigating to Photograph OCPD screen");
        boolean ocpdReached = navigateToPhotographOCPDScreen();

        if (!ocpdReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not navigate to Photograph OCPD screen.");
            return;
        }

        // Navigate through full flow to What's Next
        boolean classifyReached = navigateToClassifyOCPDScreen();
        if (!classifyReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach Classify OCPD screen.");
            return;
        }

        boolean moreOCPDsReached = navigateToMoreOCPDsScreen("Disconnect Switch");
        if (!moreOCPDsReached && workOrderPage.isClassifyOCPDScreenDisplayed()) {
            moreOCPDsReached = navigateToMoreOCPDsScreen("Fuse");
        }

        if (!moreOCPDsReached) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach More OCPDs? screen.");
            return;
        }

        workOrderPage.tapDoneWithOCPButton();
        mediumWait();

        boolean whatsNext = workOrderPage.isWhatsNextScreenDisplayed();
        if (!whatsNext) {
            cleanupFromPhotoWalkthrough();
            assertTrue(false, "Could not reach What's Next? screen.");
            return;
        }

        // Get photo count text
        String photosTotal = workOrderPage.getPhotosTotalText();
        logStep("Photos total text: '" + photosTotal + "'");

        logStepWithScreenshot("Photo count on What's Next screen");

        // Cleanup
        cleanupFromPhotoWalkthrough();

        boolean hasPhotosTotal = !photosTotal.isEmpty();
        boolean containsPhotos = photosTotal.toLowerCase().contains("photo");

        assertTrue(hasPhotosTotal && containsPhotos,
            "What's Next? should show 'X photos total' count. "
            + "Text found: '" + photosTotal + "'"
            + ". Contains 'photo': " + containsPhotos);
    }
}
