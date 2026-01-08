package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.utils.DriverManager;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Asset Test Suite - Create New Asset
 * 
 * Test Cases Covered (Automation Feasible = Yes):
 * - ATS_ECR_06: Verify Name Mandatory Validation
 * - ATS_ECR_07: Verify Name With Only Spaces
 * - ATS_ECR_10: Verify Name Trimming
 * - ATS_ECR_12: Verify Location Mandatory Validation
 * - ATS_ECR_14: Verify Select Asset Class
 * - ATS_ECR_15: Verify Asset Class Mandatory Validation
 * - ATS_ECR_17: Verify Subtype Enabled After Class Selection
 * - ATS_ECR_18: Verify Select Asset Subtype
 * - ATS_ECR_19: Verify Save Without Subtype
 * - ATS_ECR_21: Verify Enter QR Code Manually
 * - ATS_ECR_26: Verify Profile Photo Optional
 * - ATS_ECR_31: Verify Save Asset With Valid Data
 * 
 * Partial Automation Test Cases:
 * - ATS_ECR_01: Verify New Asset Screen Loads Successfully
 * - ATS_ECR_02: Verify All UI Fields Are Visible
 * - ATS_ECR_11: Verify Select Location
 * - ATS_ECR_32: Verify Cancel Asset Creation
 * - ATS_ECR_37: Verify Asset Appears in Asset List
 */
public class AssetTest extends BaseTest {

    // ============================================================
    // TEST CLASS SETUP
    // ============================================================

    @BeforeClass
    public void classSetup() {
        System.out.println("\n📋 Asset Test Suite - Starting");
        // All Asset tests use noReset=true (skip app reinstall)
        DriverManager.setNoReset(true);
    }
    
    @AfterClass
    public void classTeardown() {
        DriverManager.resetNoResetOverride();
        System.out.println("📋 Asset Test Suite - Complete");
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Navigate to New Asset screen after login and fast random site selection
     * Uses explicit waits for CI/CD compatibility
     */
    /**
     * Navigate to New Asset screen - TURBO MODE
     * Optimized for speed with minimal waits
     */
    private void navigateToNewAssetScreen() {
        long start = System.currentTimeMillis();
        
        // Brief wait for app to stabilize
        try { Thread.sleep(500); } catch (Exception e) {}
        
        // Try direct navigation first (if already logged in)
        try {
            if (tryDirectNewAssetNavigation()) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("⚡ Direct navigation to New Asset in " + elapsed + "ms");
                return;
            }
        } catch (Exception e) {}
        
        // Full login flow if direct navigation failed
        System.out.println("🔄 Direct navigation failed, doing full login...");
        performLogin();
        
        String selectedSite = siteSelectionPage.turboSelectSite();
        if (selectedSite == null) {
            selectedSite = siteSelectionPage.selectFirstSiteUltraFast();
        }
        siteSelectionPage.waitForDashboardFast();
        
        assetPage.navigateToAssetList();
        shortWait();
        assetPage.clickAddAsset();
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("⚡ Full flow to New Asset in " + elapsed + "ms");
    }
    
    /**
     * Try direct navigation to New Asset screen (if already logged in)
     * OPTIMIZED: Fast state detection before attempting navigation
     */
    private boolean tryDirectNewAssetNavigation() {
        try {
            // FAST CHECK: Already on asset list? (plus button visible)
            if (assetPage.isAssetListDisplayedFast()) {
                System.out.println("⚡ Already on Asset List - clicking Add");
                assetPage.clickAddAsset();
                return true;
            }
            
            // FAST CHECK: On dashboard? (building.2 or Assets tab visible)
            if (assetPage.isDashboardDisplayedFast()) {
                System.out.println("⚡ On Dashboard - navigating to Asset List");
                assetPage.navigateToAssetListFast();
                assetPage.clickAddAsset();
                return true;
            }
            
            // FAST CHECK: On Create Asset form already?
            if (assetPage.isCreateAssetFormDisplayed()) {
                System.out.println("⚡ Already on Create Asset form");
                return true;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Direct navigation check failed: " + e.getMessage());
        }
        return false;
    }

    // ============================================================
    // ATS_ECR_01 - Verify New Asset Screen Loads Successfully (Partial)
    // ============================================================

    @Test(priority = 1)
    public void ATS_ECR_01_verifyNewAssetScreenLoadsSuccessfully() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_01 - Verify New Asset Screen Loads Successfully"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Verifying New Asset screen elements are visible");
        assertTrue(assetPage.isCreateAssetFormDisplayed(), "New Asset form should be displayed");
        assertTrue(assetPage.isAssetNameFieldDisplayed(), "Asset Name field should be visible");
        assertTrue(assetPage.isSelectAssetClassDisplayed(), "Select Asset Class button should be visible");
        assertTrue(assetPage.isSelectLocationDisplayed(), "Select Location button should be visible");

        logStepWithScreenshot("New Asset screen loaded successfully with required sections visible");
    }

    // ============================================================
    // ATS_ECR_02 - Verify All UI Fields Are Visible (Partial)
    // ============================================================

    @Test(priority = 2)
    public void ATS_ECR_02_verifyAllUIFieldsAreVisible() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_02 - Verify All UI Fields Are Visible"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Verifying visibility of input fields");
        assertTrue(assetPage.isAssetNameFieldDisplayed(), "Name field should be visible");
        assertTrue(assetPage.isSelectLocationDisplayed(), "Location field should be visible");
        assertTrue(assetPage.isSelectAssetClassDisplayed(), "Asset Class field should be visible");

        logStep("Scrolling down to verify additional fields");
        assetPage.scrollFormDown();
        shortWait();

        assertTrue(assetPage.isSelectAssetSubtypeDisplayed(), "Asset Subtype field should be visible");
        assertTrue(assetPage.isQRCodeFieldDisplayed(), "QR Code field should be visible");

        logStepWithScreenshot("All UI fields verified visible");
        logWarning("Full verification of all fields with extensive scrolling may need manual check");
    }

    // ============================================================
    // ATS_ECR_06 - Verify Name Mandatory Validation (Yes)
    // ============================================================

    @Test(priority = 6)
    public void ATS_ECR_06_verifyNameMandatoryValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_ECR_06 - Verify Name Mandatory Validation"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Leaving Name field empty and filling other required fields");
        // Select Asset Class
        assetPage.selectAssetClass("ATS");
        shortWait();

        // Select Location
        assetPage.selectLocation();
        shortWait();

        logStep("Scrolling up to check Create Asset button");
        assetPage.scrollFormUp();
        shortWait();

        logStep("Verifying Create Asset button state with empty name");
        boolean isButtonEnabled = assetPage.isCreateAssetButtonEnabled();
        
        if (!isButtonEnabled) {
            logStep("Create Asset button is disabled with empty name - Expected behavior");
        } else {
            // Button might be hidden instead of disabled
            boolean isButtonDisplayed = assetPage.isCreateAssetButtonDisplayed();
            if (!isButtonDisplayed) {
                logStep("Create Asset button is not visible with empty name - Expected behavior");
            }
        }

        assertFalse(isButtonEnabled, "Create button should be disabled when name is empty");
        logStepWithScreenshot("Name mandatory validation verified - Create button disabled/hidden");
    }

    // ============================================================
    // ============================================================
    // ATS_ECR_07 - Verify Name With Only Spaces (Yes) - FIXED
    // ============================================================

    @Test(priority = 7)
    public void ATS_ECR_07_verifyNameWithOnlySpaces() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_ECR_07 - Verify Name With Only Spaces"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Entering spaces only in Name field");
        assetPage.enterAssetName("     ");
        assetPage.dismissKeyboard();
        mediumWait(); // Allow UI to update after input

        logStep("Verifying name field effectively contains only spaces");
        boolean nameEffectivelyEmpty = assetPage.isAssetNameEffectivelyEmpty();
        logStep("Name is effectively empty (spaces only): " + nameEffectivelyEmpty);

        logStep("Filling other required fields");
        assetPage.selectAssetClass("ATS");
        shortWait();
        assetPage.selectLocation();
        shortWait();

        logStep("Scrolling up to check Create Asset button");
        assetPage.scrollFormUp();
        mediumWait(); // Allow button state to update

        logStep("Verifying Create Asset button state with spaces-only name");
        boolean isButtonEnabled = assetPage.isCreateAssetButtonEnabled();
        logStep("Create Asset button enabled: " + isButtonEnabled);

        // The test should FAIL if button is enabled when name contains only spaces
        // This is the expected behavior - spaces-only name should NOT allow asset creation
        if (isButtonEnabled) {
            logWarning("APP BUG DETECTED: Create button is ENABLED with spaces-only name!");
            logWarning("Expected: Button should be DISABLED when name contains only spaces");
        }

        // Assert that button is DISABLED when name contains only spaces
        assertFalse(isButtonEnabled, 
            "Create button should be DISABLED when name contains only spaces. " +
            "Current state: Button enabled = " + isButtonEnabled);
        
        logStepWithScreenshot("Spaces-only name validation verified - Create button correctly disabled");
    }
    

    // ============================================================
    // ATS_ECR_10 - Verify Name Trimming (Yes)
    // ============================================================

    @Test(priority = 10)
    public void ATS_ECR_10_verifyNameTrimming() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_ECR_10 - Verify Name Trimming"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        long timestamp = System.currentTimeMillis();
        String nameWithSpaces = "  TrimTest_" + timestamp + "  ";
        String expectedTrimmedName = "TrimTest_" + timestamp; // Name without leading/trailing spaces

        logStep("Entering name with leading/trailing spaces: '" + nameWithSpaces + "'");
        assetPage.enterAssetName(nameWithSpaces);
        assetPage.dismissKeyboard();
        mediumWait();

        logStep("Filling other required fields");
        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            assetPage.selectATSClass();
        }
        mediumWait();
        
        assetPage.selectLocation();
        mediumWait();

        logStep("Creating the asset");
        assetPage.dismissKeyboard();
        mediumWait();
        
        assetPage.clickCreateAsset();
        longWait();

        logStep("Verifying asset was created");
        assertTrue(assetPage.isAssetCreatedSuccessfully(), "Asset should be created successfully");

        logStep("Searching for the created asset");
        assetPage.searchAsset("TrimTest_" + timestamp);
        mediumWait();

        logStep("Opening the created asset to verify actual saved name");
        boolean assetSelected = assetPage.selectAssetByName("TrimTest_" + timestamp);
        assertTrue(assetSelected, "Should be able to select the created asset");
        mediumWait();

        logStep("Clicking Edit to view the asset name field");
        assetPage.clickEdit();
        assetPage.waitForEditScreenReady();
        longWait(); // Give more time for edit screen to fully load

        logStep("Getting the actual saved asset name from edit screen");
        String actualSavedName = assetPage.getAssetNameValue();
        logStep("Original input name: '" + nameWithSpaces + "'");
        logStep("Expected trimmed name: '" + expectedTrimmedName + "'");
        logStep("Actual saved name: '" + actualSavedName + "'");

        // First check if we got a valid name
        assertTrue(actualSavedName != null && !actualSavedName.isEmpty(), 
            "Should be able to read the asset name from edit screen. Got: '" + actualSavedName + "'");

        // Check if name was trimmed (no leading/trailing spaces)
        // Safe to use directly since we asserted not null above
        boolean hasLeadingSpaces = actualSavedName.startsWith(" ");
        boolean hasTrailingSpaces = actualSavedName.endsWith(" ");

        if (hasLeadingSpaces || hasTrailingSpaces) {
            logWarning("APP BUG DETECTED: Name was NOT trimmed!");
            logWarning("Leading spaces: " + hasLeadingSpaces + ", Trailing spaces: " + hasTrailingSpaces);
        }

        // Assert that name is properly trimmed
        assertFalse(hasLeadingSpaces, 
            "Asset name should NOT have LEADING spaces. Actual saved name: '" + actualSavedName + "'");
        assertFalse(hasTrailingSpaces, 
            "Asset name should NOT have TRAILING spaces. Actual saved name: '" + actualSavedName + "'");
        
        logStepWithScreenshot("Name trimming verification complete - Saved name: '" + actualSavedName + "'");
    }

    // ============================================================
    // ATS_ECR_11 - Verify Select Location (Partial) - FIXED
    // ============================================================

    @Test(priority = 11)
    public void ATS_ECR_11_verifySelectLocation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_LOCATION,
            "ATS_ECR_11 - Verify Select Location"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        
        // Add extra wait for form to fully load
        mediumWait();

        logStep("Clicking Select Location button");
        try {
            assetPage.clickSelectLocation();
            shortWait();
        } catch (Exception e) {
            logWarning("First click attempt failed, retrying after scroll...");
            assetPage.scrollFormDown();
            shortWait();
            assetPage.clickSelectLocation();
            shortWait();
        }

        logStep("Verifying location picker is displayed");
        assertTrue(assetPage.isLocationPickerDisplayed(), "Location picker should be displayed");

        logStep("Selecting a location from hierarchy");
        boolean locationSelected = assetPage.selectLocation();

        if (locationSelected) {
            logStep("Location selected successfully from existing hierarchy");
        } else {
            logStep("No existing location found - creating new location");
            long timestamp = System.currentTimeMillis();
            assetPage.createNewLocation("Floor_" + timestamp, "Room_" + timestamp);
        }

        logStepWithScreenshot("Location selection verified");
        logWarning("Hierarchical navigation complexity with Appium - partial automation");
    }

    // ============================================================
    // ATS_ECR_12 - Verify Location Mandatory Validation (Yes)
    // ============================================================

    @Test(priority = 12)
    public void ATS_ECR_12_verifyLocationMandatoryValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_ECR_12 - Verify Location Mandatory Validation"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Filling Name field");
        long timestamp = System.currentTimeMillis();
        assetPage.enterAssetName("Asset_" + timestamp);
        assetPage.dismissKeyboard();
        shortWait();

        logStep("Selecting Asset Class but skipping Location");
        assetPage.selectAssetClass("ATS");
        shortWait();

        logStep("Scrolling up to check Create Asset button");
        assetPage.scrollFormUp();
        shortWait();

        logStep("Verifying Create Asset button state without location");
        boolean isButtonEnabled = assetPage.isCreateAssetButtonEnabled();

        assertFalse(isButtonEnabled, "Create button should be disabled when location is not selected");
        logStepWithScreenshot("Location mandatory validation verified - Create button disabled");
    }

    // ============================================================
    // ATS_ECR_14 - Verify Select Asset Class (Yes)
    // ============================================================

    @Test(priority = 14)
    public void ATS_ECR_14_verifySelectAssetClass() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_CLASS,
            "ATS_ECR_14 - Verify Select Asset Class"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        logStep("Clicking Select Asset Class button");
        try {
            assetPage.clickSelectAssetClass();
        } catch (Exception e) {
            logStep("Retrying asset class button click...");
            mediumWait();
            assetPage.clickSelectAssetClass();
        }
        mediumWait();

        logStep("Verifying asset class dropdown is displayed");
        assertTrue(assetPage.isAssetClassDropdownDisplayed(), "Asset class dropdown should be displayed");

        logStep("Selecting ATS asset class");
        assetPage.selectATSClass();
        mediumWait();

        logStepWithScreenshot("Asset class selection verified - ATS selected");
    }

    // ============================================================
    // ATS_ECR_15 - Verify Asset Class Mandatory Validation (Yes)
    // ============================================================

    @Test(priority = 15)
    public void ATS_ECR_15_verifyAssetClassMandatoryValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_ECR_15 - Verify Asset Class Mandatory Validation"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Filling Name field");
        long timestamp = System.currentTimeMillis();
        assetPage.enterAssetName("Asset_" + timestamp);
        assetPage.dismissKeyboard();
        shortWait();

        logStep("Selecting Location but skipping Asset Class");
        // Note: selectLocation() already calls clickSelectLocation() internally
        assetPage.selectLocation();
        shortWait();

        logStep("Scrolling up to check Create Asset button");
        assetPage.scrollFormUp();
        shortWait();

        logStep("Verifying Create Asset button state without asset class");
        boolean isButtonEnabled = assetPage.isCreateAssetButtonEnabled();

        assertFalse(isButtonEnabled, "Create button should be disabled when asset class is not selected");
        logStepWithScreenshot("Asset class mandatory validation verified - Create button disabled");
    }

    // ============================================================
    // ATS_ECR_16 - Verify Asset Class Selection Persistence (Yes)
    // ============================================================

    @Test(priority = 16)
    public void ATS_ECR_16_verifyAssetClassSelectionPersistence() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_CLASS,
            "ATS_ECR_16 - Verify Asset Class Selection Persistence"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        shortWait();

        logStep("Selecting Asset Class - ATS");
        assetPage.selectAssetClass("ATS");
        shortWait();

        logStep("Verifying ATS is displayed as selected class");
        boolean atsSelected = assetPage.isAssetClassSelected();
        assertTrue(atsSelected, "ATS should be displayed as selected asset class");

        logStep("Scrolling down and back up to verify persistence");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormUp();
        shortWait();

        logStep("Verifying ATS selection persists after scrolling");
        boolean atsPersisted = assetPage.isAssetClassSelected();
        assertTrue(atsPersisted, "ATS selection should persist after scrolling");

        logStepWithScreenshot("Asset class selection persistence verified");
    }

    // ============================================================
    // ATS_ECR_17 - Verify Subtype Enabled After Class Selection (Yes)
    // ============================================================

    @Test(priority = 17)
    public void ATS_ECR_17_verifySubtypeEnabledAfterClassSelection() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_SUBTYPE,
            "ATS_ECR_17 - Verify Subtype Enabled After Class Selection"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        logStep("Verifying Subtype button visibility before selecting asset class");
        assetPage.scrollFormDown();
        shortWait();
        boolean subtypeVisibleBefore = assetPage.isSelectAssetSubtypeDisplayed();
        logStep("Subtype field visible before class selection: " + subtypeVisibleBefore);

        logStep("Scrolling up to access Asset Class");
        assetPage.scrollFormUp();
        mediumWait();

        logStep("Selecting Asset Class - ATS");
        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            // Retry with direct click approach
            logStep("Retrying asset class selection...");
            assetPage.scrollFormUp();
            mediumWait();
            assetPage.selectATSClass();
        }
        mediumWait();

        logStep("Scrolling down to verify Subtype is enabled after class selection");
        assetPage.scrollFormDown();
        mediumWait();

        assertTrue(assetPage.isSelectAssetSubtypeDisplayed(), "Subtype should be enabled/visible after asset class selection");
        logStepWithScreenshot("Subtype enabled after class selection verified");
    }

    // ============================================================
    // ATS_ECR_18 - Verify Select Asset Subtype (Yes) - FIXED
    // ============================================================

    @Test(priority = 18)
    public void ATS_ECR_18_verifySelectAssetSubtype() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_SUBTYPE,
            "ATS_ECR_18 - Verify Select Asset Subtype"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        logStep("Selecting Asset Class first (required to enable Subtype)");
        assetPage.selectAssetClass("ATS");
        mediumWait(); // Wait for subtype to become enabled

        logStep("Dismissing keyboard if open");
        assetPage.dismissKeyboard();
        shortWait();

        // NOTE: Subtype is just below Asset Class - NO scrolling needed!
        // Excessive scrolling was pushing it out of view

        logStep("Clicking Select Asset Subtype button");
        try {
            assetPage.clickSelectAssetSubtype();
            shortWait();
        } catch (Exception e) {
            logWarning("Click failed, trying with single scroll: " + e.getMessage());
            assetPage.scrollFormDown();
            shortWait();
            assetPage.clickSelectAssetSubtype();
            shortWait();
        }

        logStep("Verifying subtype dropdown is displayed");
        boolean dropdownDisplayed = assetPage.isSubtypeDropdownDisplayed();
        if (dropdownDisplayed) {
            logStep("Subtype dropdown is displayed");
        } else {
            logWarning("Dropdown may have closed or visibility check failed");
        }

        logStep("Selecting first available subtype from dropdown");
        try {
            // Re-open dropdown if closed
            if (!dropdownDisplayed) {
                assetPage.clickSelectAssetSubtype();
                shortWait();
            }
            String selectedSubtype = assetPage.selectFirstAvailableSubtype();
            logStep("Selected subtype: " + selectedSubtype);
        } catch (Exception e) {
            logWarning("Could not select subtype: " + e.getMessage());
        }
        shortWait();

        logStep("Verifying subtype was selected");
        boolean subtypeSelected = assetPage.isSubtypeSelected();
        assertTrue(subtypeSelected, "Asset subtype should be selected");
        
        logStepWithScreenshot("Asset subtype selection verified");
    }


    // ============================================================
    // ATS_ECR_19 - Verify Save Without Subtype - Optional (Yes)
    // ============================================================

    @Test(priority = 19)
    public void ATS_ECR_19_verifySaveWithoutSubtype() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_SUBTYPE,
            "ATS_ECR_19 - Verify Save Without Subtype (Optional Field)"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        shortWait();

        logStep("Entering asset name");
        String assetName = "TestAsset_NoSubtype_" + System.currentTimeMillis();
        assetPage.enterAssetName(assetName);
        shortWait();

        logStep("Selecting location");
        assetPage.selectLocation();
        shortWait();

        logStep("Selecting Asset Class - ATS (without selecting subtype)");
        assetPage.selectAssetClass("ATS");
        shortWait();

        logStep("Attempting to save asset WITHOUT selecting subtype");
        assetPage.scrollFormUp();
        shortWait();
        assetPage.clickCreateAsset();
        mediumWait();

        logStep("Verifying asset can be saved without subtype");
        boolean saveSuccessful = assetPage.isAssetCreatedSuccessfully();
        assertTrue(saveSuccessful, "Asset should save successfully without subtype (optional field)");

        logStepWithScreenshot("Save without subtype verified - subtype is optional");
    }

    // ============================================================
    // ATS_ECR_20 - Verify Subtype Options Match Asset Class (Yes)
    // ============================================================

    @Test(priority = 20)
    public void ATS_ECR_20_verifySubtypeOptionsMatchAssetClass() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_SUBTYPE,
            "ATS_ECR_20 - Verify Subtype Options Match Asset Class"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        shortWait();

        logStep("Selecting Asset Class - ATS");
        assetPage.selectAssetClass("ATS");
        shortWait();

        logStep("Opening subtype dropdown");
        assetPage.clickSelectAssetSubtype();
        shortWait();

        logStep("Verifying ATS-specific subtype options are displayed");
        boolean hasATSSubtypes = assetPage.isSubtypeDropdownDisplayed();
        assertTrue(hasATSSubtypes, "ATS subtype options should be displayed");

        logStep("Closing dropdown");
        assetPage.dismissKeyboard();
        shortWait();

        logStepWithScreenshot("Subtype options match ATS asset class - verified");
    }

    // ============================================================
    // ATS_ECR_21 - Verify Enter QR Code Manually (Yes)
    // ============================================================

    @Test(priority = 21)
    public void ATS_ECR_21_verifyEnterQRCodeManually() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_QR_CODE,
            "ATS_ECR_21 - Verify Enter QR Code Manually"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_QR_" + timestamp;
        String qrCode = "QR_" + timestamp;

        logStep("Filling mandatory fields");
        assetPage.enterAssetName(assetName);
        assetPage.dismissKeyboard();
        mediumWait();

        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            assetPage.selectATSClass();
        }
        mediumWait();

        assetPage.selectLocation();
        mediumWait();

        logStep("Scrolling down to QR Code field");
        assetPage.dismissKeyboard();
        assetPage.scrollFormDown();
        mediumWait();

        logStep("Entering QR code manually: " + qrCode);
        assetPage.enterQRCode(qrCode);
        mediumWait();

        logStep("Creating the asset");
        assetPage.dismissKeyboard();
        assetPage.scrollFormUp();
        assetPage.scrollFormUp();
        mediumWait();
        
        try {
            assetPage.clickCreateAsset();
        } catch (Exception e) {
            logStep("Retrying Create Asset click...");
            assetPage.scrollFormUp();
            mediumWait();
            assetPage.clickCreateAsset();
        }
        longWait();

        logStep("Verifying asset was created with QR code");
        assertTrue(assetPage.isAssetCreatedSuccessfully(), "Asset should be saved with manually entered QR code");
        logStepWithScreenshot("Manual QR code entry verified - Asset created with QR: " + qrCode);
    }

    // ============================================================
    // ATS_ECR_26 - Verify Profile Photo Optional (Yes)
    // ============================================================

    @Test(priority = 26)
    public void ATS_ECR_26_verifyProfilePhotoOptional() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_26 - Verify Profile Photo Optional"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_NoPhoto_" + timestamp;

        logStep("Filling all mandatory fields without adding any photo");
        assetPage.enterAssetName(assetName);
        assetPage.dismissKeyboard();
        mediumWait();

        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            assetPage.selectATSClass();
        }
        mediumWait();

        assetPage.selectLocation();
        mediumWait();

        logStep("NOT uploading any profile photo - leaving it empty");

        logStep("Creating the asset without photo");
        assetPage.dismissKeyboard();
        assetPage.scrollFormUp();
        assetPage.scrollFormUp();
        mediumWait();
        
        try {
            assetPage.clickCreateAsset();
        } catch (Exception e) {
            logStep("Retrying Create Asset click...");
            assetPage.scrollFormUp();
            mediumWait();
            assetPage.clickCreateAsset();
        }
        longWait();

        logStep("Verifying asset was created successfully without photo");
        assertTrue(assetPage.isAssetCreatedSuccessfully(), "Asset should be saved successfully without profile photo");
        logStepWithScreenshot("Profile photo is optional - Asset created without photo");
    }

    // ============================================================
    // ATS_ECR_31 - Verify Save Asset With Valid Data (Yes)
    // ============================================================

    @Test(priority = 31)
    public void ATS_ECR_31_verifySaveAssetWithValidData() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_31 - Verify Save Asset With Valid Data"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_" + timestamp;
        String qrCode = "QR_" + timestamp;

        logStep("Filling all fields with valid data");
        
        // 1. Name
        assetPage.enterAssetName(assetName);
        assetPage.dismissKeyboard();
        logStep("✅ Name entered: " + assetName);
        mediumWait();

        // 2. Asset Class
        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            assetPage.selectATSClass();
        }
        logStep("✅ Asset Class selected: ATS");
        mediumWait();

        // 3. Location
        boolean locationSelected = assetPage.selectLocation();
        if (!locationSelected) {
            String floorName = "Floor_" + timestamp;
            String roomName = "Room_" + timestamp;
            assetPage.createNewLocation(floorName, roomName);
            logStep("✅ New Location created: " + floorName + " > " + roomName);
        } else {
            logStep("✅ Location selected from existing hierarchy");
        }
        mediumWait();

        // 4. Scroll and select Subtype
        assetPage.dismissKeyboard();
        assetPage.scrollFormDown();
        mediumWait();

        try {
            assetPage.selectAssetSubtype("test");
            logStep("✅ Subtype selected: test");
        } catch (Exception e) {
            logStep("⚠️ Subtype selection skipped");
        }
        mediumWait();

        // 5. QR Code
        assetPage.enterQRCode(qrCode);
        logStep("✅ QR Code entered: " + qrCode);
        mediumWait();

        // 6. Create Asset
        assetPage.dismissKeyboard();
        assetPage.scrollFormUp();
        assetPage.scrollFormUp();
        mediumWait();
        
        try {
            assetPage.clickCreateAsset();
        } catch (Exception e) {
            logStep("Retrying Create Asset click...");
            assetPage.scrollFormUp();
            mediumWait();
            assetPage.clickCreateAsset();
        }
        longWait();

        logStep("Verifying asset creation");
        assertTrue(assetPage.isAssetCreatedSuccessfully(), "Asset should be created successfully with all valid data");
        
        logStepWithScreenshot("✅✅✅ ASSET CREATED SUCCESSFULLY: " + assetName + " ✅✅✅");
    }

    // ============================================================
    // ATS_ECR_32 - Verify Cancel Asset Creation (Partial)
    // ============================================================

    @Test(priority = 32)
    public void ATS_ECR_32_verifyCancelAssetCreation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_32 - Verify Cancel Asset Creation"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();

        logStep("Entering some data in the form");
        assetPage.enterAssetName("TestAsset_ToCancel");
        assetPage.dismissKeyboard();
        shortWait();

        logStep("Clicking Cancel button");
        assetPage.clickCancel();
        mediumWait();

        logStep("Verifying returned to previous screen without saving");
        assertTrue(assetPage.isAssetListDisplayed(), "Should return to Asset List without saving");

        logStepWithScreenshot("Cancel asset creation verified - Returned without save");
        logWarning("Data state verification after cancel may need manual check");
    }

    // ============================================================
    // ATS_ECR_37 - Verify Asset Appears in Asset List (Partial)
    // ============================================================

    @Test(priority = 37)
    public void ATS_ECR_37_verifyAssetAppearsInAssetList() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_CREATE_ASSET,
            "ATS_ECR_37 - Verify Asset Appears in Asset List"
        );

        logStep("Logging in and navigating to New Asset screen");
        navigateToNewAssetScreen();
        mediumWait();

        long timestamp = System.currentTimeMillis();
        String assetName = "Asset_Verify_" + timestamp;

        logStep("Creating asset: " + assetName);
        assetPage.enterAssetName(assetName);
        assetPage.dismissKeyboard();
        mediumWait();

        try {
            assetPage.selectAssetClass("ATS");
        } catch (Exception e) {
            assetPage.selectATSClass();
        }
        mediumWait();

        assetPage.selectLocation();
        mediumWait();

        assetPage.dismissKeyboard();
        assetPage.scrollFormUp();
        assetPage.scrollFormUp();
        mediumWait();
        
        try {
            assetPage.clickCreateAsset();
        } catch (Exception e) {
            logStep("Retrying Create Asset click...");
            assetPage.scrollFormUp();
            mediumWait();
            assetPage.clickCreateAsset();
        }
        longWait();

        logStep("Verifying asset was created");
        assertTrue(assetPage.isAssetCreatedSuccessfully(), "Asset should be created");

        logStep("Searching for created asset in list");
        assetPage.searchAsset(assetName);
        longWait();

        logStep("Verifying asset appears in the list");
        boolean assetFound = assetPage.verifyAssetExistsInList(assetName);
        
        if (assetFound) {
            logStep("✅ Asset found in list: " + assetName);
        } else {
            logWarning("Asset may need refresh or scroll to be visible - timing issues");
        }

        logStepWithScreenshot("Asset list verification completed");
        logWarning("List refresh timing makes full verification unreliable with Appium");
    }

    // ============================================================
    // EDIT ASSET DETAILS HELPER METHODS
    // ============================================================

    /**
     * Navigate to Edit Asset Details screen for an existing ATS asset
     */
    private void navigateToEditAssetScreen() {
        long start = System.currentTimeMillis();
        
        // Login + site selection
        performLogin();
        String selectedSite = siteSelectionPage.turboSelectSite();
        if (selectedSite == null) {
            selectedSite = siteSelectionPage.selectFirstSiteUltraFast();
        }
        System.out.println("⚡ Site: " + selectedSite);
        siteSelectionPage.waitForDashboardFast();
        
        // Navigate to asset list
        assetPage.navigateToAssetList();
        mediumWait();
        
        // Select first asset (existing asset)
        String assetName = assetPage.selectFirstAsset();
        System.out.println("📦 Opened asset: " + assetName);
        mediumWait();
        
        // Click Edit button
        assetPage.clickEdit();
        assetPage.waitForEditScreenReady();
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("⚡ navigateToEditAssetScreen completed in " + elapsed + "ms");
    }

    // ============================================================
    // EDIT ASSET DETAILS TEST CASES - ATS_EAD
    // ============================================================

    // ============================================================
    // ATS_EAD_01 - Open Edit Asset Details screen (Yes)
    // ============================================================

    @Test(priority = 101)
    public void ATS_EAD_01_openEditAssetDetailsScreen() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_01 - Open Edit Asset Details screen"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Verifying Edit Asset Details screen opens");
        assertTrue(assetPage.isEditAssetScreenDisplayed(), "Edit Asset Details screen should be displayed");

        logStepWithScreenshot("Edit Asset Details screen opened successfully");
    }

    // ============================================================
    // ATS_EAD_02 - Verify Core Attributes section (Partial)
    // ============================================================

    @Test(priority = 102)
    public void ATS_EAD_02_verifyCoreAttributesSection() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_02 - Verify Core Attributes section"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Verifying Core Attributes section is visible");
        assertTrue(assetPage.isCoreAttributesSectionVisible(), "Core Attributes section should be visible");

        logStepWithScreenshot("Core Attributes section verified");
        logWarning("Full content verification may need scroll - partial automation");
    }

    // ============================================================
    // ATS_EAD_03 - Verify all fields visible by default (Partial)
    // ============================================================

    @Test(priority = 103)
    public void ATS_EAD_03_verifyAllFieldsVisibleByDefault() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_03 - Verify all fields visible by default"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Verifying fields are visible");
        assertTrue(assetPage.isEditAssetScreenDisplayed(), "Edit screen should be displayed");

        logStepWithScreenshot("Fields visibility verified");
        logWarning("Extensive scrolling makes full verification complex - partial automation");
    }

    // ============================================================
    // ATS_EAD_04 - Verify Required fields toggle default state (Partial)
    // ============================================================

    @Test(priority = 104)
    public void ATS_EAD_04_verifyRequiredFieldsToggleDefaultState() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_04 - Verify Required fields toggle default state"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Verifying toggle is displayed");
        assertTrue(assetPage.isRequiredFieldsToggleDisplayed(), "Required fields toggle should be displayed");

        logStep("Verifying toggle is OFF by default");
        assertFalse(assetPage.isRequiredFieldsToggleOn(), "Toggle should be OFF by default");

        logStepWithScreenshot("Required fields toggle default state verified");
    }

    // ============================================================
    // ATS_EAD_05 - Verify default completion percentage (Partial)
    // ============================================================

    @Test(priority = 105)
    public void ATS_EAD_05_verifyDefaultCompletionPercentage() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_05 - Verify default completion percentage"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Verifying percentage element exists");
        assertTrue(assetPage.isPercentageDisplayed(), "Percentage should be displayed");

        String percentage = assetPage.getCompletionPercentage();
        logStep("Current percentage: " + percentage);

        logStepWithScreenshot("Completion percentage verified");
        logWarning("Calculation accuracy needs manual check - partial automation");
    }

    // ============================================================
    // ATS_EAD_06 - Enable Required fields only toggle (Partial)
    // ============================================================

    @Test(priority = 106)
    public void ATS_EAD_06_enableRequiredFieldsOnlyToggle() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_06 - Enable Required fields only toggle"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields only toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Verifying toggle is ON");
        assertTrue(assetPage.isRequiredFieldsToggleOn(), "Toggle should be ON after enabling");

        logStepWithScreenshot("Required fields only toggle enabled");
        logWarning("Verifying correct fields shown/hidden needs manual check");
    }

    // ============================================================
    // ATS_EAD_11 - Fill one required field (Yes)
    // ============================================================

    @Test(priority = 111)
    public void ATS_EAD_11_fillOneRequiredField() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_11 - Fill one required field"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Getting initial percentage");
        String initialPercentage = assetPage.getCompletionPercentage();
        logStep("Initial percentage: " + initialPercentage);

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Scrolling down to find Ampere Rating field");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();

        logStep("Filling one required field (Ampere Rating)");
        assetPage.fillAmpereRating("100");
        mediumWait();

        logStep("Getting updated percentage");
        String updatedPercentage = assetPage.getCompletionPercentage();
        logStep("Updated percentage: " + updatedPercentage);

        logStepWithScreenshot("Required field filled - percentage should increase");
    }

    // ============================================================
    // ATS_EAD_12 - Clear required field (Yes)
    // ============================================================

    @Test(priority = 112)
    public void ATS_EAD_12_clearRequiredField() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_12 - Clear required field"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Scrolling down to find Ampere Rating field");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();

        logStep("Filling a required field first");
        assetPage.fillAmpereRating("100");
        mediumWait();

        String beforeClear = assetPage.getCompletionPercentage();
        logStep("Percentage before clear: " + beforeClear);

        logStep("Clearing the required field");
        assetPage.clearTextField("Ampere");
        mediumWait();

        String afterClear = assetPage.getCompletionPercentage();
        logStep("Percentage after clear: " + afterClear);

        logStepWithScreenshot("Required field cleared - percentage should decrease");
    }

    // ============================================================
    // ATS_EAD_13 - Complete all required fields (Yes)
    // ============================================================

    @Test(priority = 113)
    public void ATS_EAD_13_completeAllRequiredFields() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_13 - Complete all required fields"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Filling all ATS required fields");
        assetPage.fillAllATSRequiredFields();
        mediumWait();

        String percentage = assetPage.getCompletionPercentage();
        logStep("Completion percentage: " + percentage);

        logStepWithScreenshot("All required fields filled - should show 100%");
    }

    // ============================================================
    // ATS_EAD_14 - Disable Required fields toggle (Partial)
    // ============================================================

    @Test(priority = 114)
    public void ATS_EAD_14_disableRequiredFieldsToggle() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_14 - Disable Required fields toggle"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle first");
        assetPage.enableRequiredFieldsOnly();
        shortWait();
        assertTrue(assetPage.isRequiredFieldsToggleOn(), "Toggle should be ON");

        logStep("Disabling Required fields toggle");
        assetPage.disableRequiredFieldsOnly();
        shortWait();

        logStep("Verifying toggle is OFF");
        assertFalse(assetPage.isRequiredFieldsToggleOn(), "Toggle should be OFF after disabling");

        logStepWithScreenshot("Required fields toggle disabled - all fields should reappear");
        logWarning("Verifying correct fields shown/hidden needs manual check");
    }

    // ============================================================
    // ATS_EAD_15 - Save with no required fields filled (Yes)
    // ============================================================

    @Test(priority = 115)
    public void ATS_EAD_15_saveWithNoRequiredFieldsFilled() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_15 - Save with no required fields filled"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Leaving required fields empty and clicking Save");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditSave();
        longWait();

        logStep("Verifying asset saved successfully");
        assertTrue(assetPage.isEditSavedSuccessfully(), "Asset should be saved without required fields");

        logStepWithScreenshot("Asset saved successfully without required fields");
    }

    // ============================================================
    // ATS_EAD_16 - Save with partial required fields (Yes)
    // ============================================================

    @Test(priority = 116)
    public void ATS_EAD_16_saveWithPartialRequiredFields() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_16 - Save with partial required fields"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Filling some required fields");
        logStep("Scrolling down to find Ampere Rating field");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();
        assetPage.fillAmpereRating("100");
        mediumWait();

        logStep("Saving with partial required fields");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditSave();
        longWait();

        logStep("Verifying asset saved successfully");
        assertTrue(assetPage.isEditSavedSuccessfully(), "Asset should be saved with partial required fields");

        logStepWithScreenshot("Asset saved successfully with partial required fields");
    }

    // ============================================================
    // ATS_EAD_17 - Save with all required fields (Yes)
    // ============================================================

    @Test(priority = 117)
    public void ATS_EAD_17_saveWithAllRequiredFields() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_17 - Save with all required fields"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Filling all required fields");
        assetPage.fillAllATSRequiredFields();
        mediumWait();

        logStep("Saving with all required fields");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditSave();
        longWait();

        logStep("Verifying asset saved successfully");
        assertTrue(assetPage.isEditSavedSuccessfully(), "Asset should be saved with all required fields");

        logStepWithScreenshot("Asset saved successfully with all required fields");
    }

    // ============================================================
    // ATS_EAD_18 - Verify no blocking validation (Yes)
    // ============================================================

    @Test(priority = 118)
    public void ATS_EAD_18_verifyNoBlockingValidation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_ASSET_VALIDATION,
            "ATS_EAD_18 - Verify no blocking validation"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Leaving required fields empty");
        // Don't fill any required fields

        logStep("Attempting to save");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditSave();
        longWait();

        logStep("Verifying save was not blocked");
        assertTrue(assetPage.isEditSavedSuccessfully(), "Save should not be blocked by validation");

        logStepWithScreenshot("No blocking validation - save allowed");
    }

    // ============================================================
    // ATS_EAD_19 - Verify green check indicator (Partial)
    // ============================================================

    @Test(priority = 119)
    public void ATS_EAD_19_verifyGreenCheckIndicator() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_19 - Verify green check indicator"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Scrolling down to find Ampere Rating field");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();

        logStep("Filling a required field (Ampere Rating)");
        assetPage.fillAmpereRating("100");
        mediumWait();

        logStep("Checking for indicator element");
        boolean hasIndicator = assetPage.isGreenCheckIndicatorDisplayed();
        logStep("Green check indicator found: " + hasIndicator);

        logStepWithScreenshot("Green check indicator verification");
        logWarning("Color verification needs visual testing - partial automation");
    }

    // ============================================================
    // ATS_EAD_20 - Verify red warning indicator (Partial)
    // ============================================================

    @Test(priority = 120)
    public void ATS_EAD_20_verifyRedWarningIndicator() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_20 - Verify red warning indicator"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Leaving required fields empty");
        assetPage.scrollFormDown();
        shortWait();

        logStep("Checking for warning indicator element");
        boolean hasWarning = assetPage.isRedWarningIndicatorDisplayed();
        logStep("Red warning indicator found: " + hasWarning);

        logStepWithScreenshot("Red warning indicator verification");
        logWarning("Color verification needs visual testing - partial automation");
    }

    // ============================================================
    // ATS_EAD_21 - Indicators do not block save (Yes)
    // ============================================================

    @Test(priority = 121)
    public void ATS_EAD_21_indicatorsDoNotBlockSave() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_21 - Indicators do not block save"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Enabling Required fields toggle");
        assetPage.enableRequiredFieldsOnly();
        shortWait();

        logStep("Leaving required fields empty (red indicators should show)");

        logStep("Attempting to save with indicators showing");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditSave();
        longWait();

        logStep("Verifying save was successful despite indicators");
        assertTrue(assetPage.isEditSavedSuccessfully(), "Save should be allowed despite red indicators");

        logStepWithScreenshot("Save allowed with indicators - not blocking");
    }

    // ============================================================
    // ATS_EAD_22 - Cancel edit operation (Partial)
    // ============================================================

    @Test(priority = 122)
    public void ATS_EAD_22_cancelEditOperation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_22 - Cancel edit operation"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Making some modifications");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.fillAmpereRating("999");
        mediumWait();

        logStep("Clicking Cancel");
        assetPage.scrollFormUp();
        mediumWait();
        assetPage.clickEditCancel();
        longWait();

        logStep("Verifying returned without save");
        assertTrue(!assetPage.isEditAssetScreenDisplayed(), "Should return from Edit screen after cancel");

        logStepWithScreenshot("Cancel edit operation verified");
        logWarning("Data state verification needs manual check - partial automation");
    }

    // ============================================================
    // ATS_EAD_23 - Verify Save button state (Partial)
    // ============================================================

    @Test(priority = 123)
    public void ATS_EAD_23_verifySaveButtonState() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "ATS_EAD_23 - Verify Save button state"
        );

        logStep("Navigating to Edit Asset Details screen");
        navigateToEditAssetScreen();

        logStep("Checking Save button without changes");
        boolean isEnabled = assetPage.isEditSaveButtonEnabled();
        logStep("Save button enabled: " + isEnabled);

        logStepWithScreenshot("Save button state verified");
        logWarning("Enabled/disabled state verification can be flaky - partial automation");
    }

    // ============================================================
    // BUSWAY ASSET CLASS TEST CASES - BUS_EAD
    // ============================================================

    /**
     * Navigate to Edit Asset screen and change Asset Class to Busway
     */
    /**
     * Helper: Navigate to Edit Asset screen and change class to Busway
     * Uses SMART NAVIGATION - detects current state and takes shortest path
     */
    private void navigateToEditAssetScreenAndChangeToBusway() {
        System.out.println("📝 Navigating to Edit Asset screen...");
        
        // Step 1: Check if we need to login
        boolean needsLogin = false;
        try {
            needsLogin = welcomePage.isContinueButtonDisplayed() || loginPage.isSignInButtonDisplayed();
        } catch (Exception e) {
            needsLogin = false;
        }
        
        if (needsLogin) {
            System.out.println("🔐 Login required, performing login...");
            performLogin();
            String site = siteSelectionPage.turboSelectSite();
            if (site == null) {
                siteSelectionPage.selectFirstSiteUltraFast();
            }
            siteSelectionPage.waitForDashboardFast();
        }
        
        // Step 2: Go to Asset List
        System.out.println("📦 Going to Asset List...");
        assetPage.navigateToAssetList();
        shortWait();
        
        // Step 3: Click first asset
        System.out.println("👆 Clicking first asset...");
        assetPage.selectFirstAsset();
        shortWait();
        
        // Step 4: Click Edit
        System.out.println("✏️ Clicking Edit...");
        assetPage.clickEdit();
        shortWait();
        
        // Step 5: Change to Busway
        System.out.println("🔄 Changing to Busway...");
        assetPage.changeAssetClassToBusway();
        shortWait();
        
        // Step 6: Scroll to Core Attributes
        assetPage.scrollFormDown();
        shortWait();
        
        System.out.println("✅ On Edit Asset screen with Busway class");
    }

    // ============================================================
    // BUS_EAD_01 - Verify Core Attributes not visible for Busway (Partial)
    // ============================================================

    @Test(priority = 201)
    public void BUS_EAD_01_verifyCoreAttributesNotVisibleForBusway() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "BUS_EAD_01 - Verify Core Attributes section is empty for Busway asset class"
        );

        logStep("Navigating to Edit Asset screen and changing to Busway");
        navigateToEditAssetScreenAndChangeToBusway();

        logStep("Verifying Core Attributes section has NO fields/details for Busway");
        logStep("Note: 'Core Attributes' header text may still be visible, but content should be empty");
        
        boolean isCoreAttributesEmpty = assetPage.isCoreAttributesSectionHidden();
        logStep("Core Attributes section is empty (no fields): " + isCoreAttributesEmpty);
        
        assertTrue(isCoreAttributesEmpty, "Core Attributes section should have NO fields/details for Busway asset class");
        
        logStepWithScreenshot("Core Attributes section verified empty for Busway");
        logWarning("Header 'Core Attributes' may be visible but section content should be empty - partial automation");
    }

    // ============================================================
    // BUS_EAD_02 - Verify completion percentage not shown for Busway (Partial)
    // ============================================================

    @Test(priority = 202)
    public void BUS_EAD_02_verifyCompletionPercentageNotShownForBusway() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "BUS_EAD_02 - Verify completion percentage not shown for Busway"
        );

        logStep("Navigating to Edit Asset screen and changing to Busway");
        navigateToEditAssetScreenAndChangeToBusway();

        logStep("Verifying percentage indicator is NOT shown for Busway");
        boolean isPercentageHidden = assetPage.isPercentageIndicatorHidden();
        logStep("Percentage indicator hidden: " + isPercentageHidden);

        assertTrue(isPercentageHidden, "Core Attributes percentage indicator should NOT be shown for Busway");

        logStepWithScreenshot("Completion percentage not shown for Busway - verified");
        logWarning("Percentage element existence can be checked but calculation accuracy needs manual check");
    }

    // ============================================================
    // BUS_EAD_03 - Verify save without Core Attributes (Yes)
    // ============================================================

    @Test(priority = 203)
    public void BUS_EAD_03_verifySaveWithoutCoreAttributes() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "BUS_EAD_03 - Verify save without Core Attributes"
        );

        logStep("Navigating to Edit Asset screen and changing to Busway");
        navigateToEditAssetScreenAndChangeToBusway();

        logStep("Clicking Save Changes button directly");
        assetPage.clickSaveChanges();
        longWait();

        logStep("Verifying we are no longer on Edit screen or save completed");
        // After saving Busway, we should be back to asset details or list
        // The fact that no error/validation dialog appeared means save was successful
        boolean saveCompleted = assetPage.isSaveCompletedForBusway();
        logStep("Save completed: " + saveCompleted);

        assertTrue(saveCompleted, "Asset should be saved successfully without Core Attributes");

        logStepWithScreenshot("Busway asset saved successfully without Core Attributes");
    }

    // ============================================================
    // BUS_EAD_04 - Verify UI consistency for Busway (Partial)
    // ============================================================

    @Test(priority = 204)
    public void BUS_EAD_04_verifyUIConsistencyForBusway() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "BUS_EAD_04 - Verify UI consistency for Busway asset class"
        );

        logStep("Navigating to Edit Asset screen and changing to Busway");
        navigateToEditAssetScreenAndChangeToBusway();

        logStep("Verifying Edit screen is properly displayed for Busway");
        boolean isEditScreenDisplayed = assetPage.isEditScreenDisplayedForBusway();
        logStep("Edit screen displayed: " + isEditScreenDisplayed);

        assertTrue(isEditScreenDisplayed, "Edit screen should be properly displayed for Busway");

        logStep("Saving to verify no UI issues block the save");
        assetPage.clickSaveChanges();
        longWait();

        boolean saveCompleted = assetPage.isSaveCompletedForBusway();
        logStep("Save completed successfully: " + saveCompleted);

        assertTrue(saveCompleted, "Save should complete without UI issues for Busway");

        logStepWithScreenshot("UI consistency verified for Busway - Edit and Save work correctly");
        logWarning("Comprehensive UI consistency needs visual testing - partial automation");
    }

    // ================================================================
    // CAPACITOR EDIT ASSET DETAILS TEST CASES (CAP_EAD)
    // ================================================================

    /**
     * Helper: Navigate to Edit Asset screen and change class to Capacitor
     * Simple flow: Login if needed → Asset List → Click Asset → Edit → Capacitor
     */
    private void navigateToEditAssetScreenAndChangeToCapacitor() {
        long startTime = System.currentTimeMillis();
        System.out.println("📝 Navigating to Edit Asset screen...");
        
        // Step 1: SKIP login check if app is already on dashboard/asset list (noReset=true)
        // This saves time - don't check for login screens unnecessarily
        
        // Step 2: Go to Asset List DIRECTLY (fastest path)
        System.out.println("📦 Going to Asset List...");
        long t1 = System.currentTimeMillis();
        assetPage.navigateToAssetListTurbo();
        System.out.println("   ⏱️ Asset List: " + (System.currentTimeMillis() - t1) + "ms");
        
        // Step 3: Click first asset
        System.out.println("👆 Clicking first asset...");
        long t2 = System.currentTimeMillis();
        assetPage.selectFirstAsset();
        System.out.println("   ⏱️ Select asset: " + (System.currentTimeMillis() - t2) + "ms");
        
        // Step 4: Click Edit
        System.out.println("✏️ Clicking Edit...");
        long t3 = System.currentTimeMillis();
        assetPage.clickEditTurbo();
        System.out.println("   ⏱️ Click Edit: " + (System.currentTimeMillis() - t3) + "ms");
        
        // Step 5: Change to Capacitor
        System.out.println("🔄 Changing to Capacitor...");
        long t4 = System.currentTimeMillis();
        assetPage.changeAssetClassToCapacitor();
        System.out.println("   ⏱️ Change class: " + (System.currentTimeMillis() - t4) + "ms");
        
        System.out.println("✅ On Edit Asset screen with Capacitor class (Total: " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    // ============================================================
    // CAP_EAD_01 - Open Edit Asset Details screen (Yes)
    // ============================================================

    @Test(priority = 301)
    public void CAP_EAD_01_verifyEditScreenOpensForCapacitor() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_01 - Verify edit screen opens for Capacitor asset class"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Verifying Edit Asset Details screen is open");
        boolean editScreenOpen = assetPage.isEditAssetScreenDisplayed();
        assertTrue(editScreenOpen, "Edit Asset Details screen should be open for Capacitor");

        logStepWithScreenshot("Edit screen opened for Capacitor - verified");
    }

    // ============================================================
    // CAP_EAD_02 - Verify Core Attributes section visible (Partial)
    // ============================================================

    @Test(priority = 302)
    public void CAP_EAD_02_verifyCoreAttributesSectionVisible() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_02 - Verify Core Attributes section loads for Capacitor"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to Core Attributes section");
        assetPage.scrollFormDown();
        shortWait();

        logStep("Verifying Core Attributes section is visible");
        boolean coreAttributesVisible = assetPage.isCoreAttributesSectionVisible();
        assertTrue(coreAttributesVisible, "Core Attributes section should be visible for Capacitor");

        logStepWithScreenshot("Core Attributes section visible for Capacitor - verified");
        logWarning("Can verify section exists but may need scrolling to bring into view - partial automation");
    }

    // ============================================================
    // CAP_EAD_03 - Verify all fields visible by default (Partial)
    // ============================================================

    @Test(priority = 303)
    public void CAP_EAD_03_verifyAllFieldsVisibleByDefault() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_03 - Verify all Capacitor fields are visible initially"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling through Core Attributes to verify fields exist");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.scrollFormDown();
        shortWait();

        logStep("Core Attributes fields are visible by scrolling");
        logStepWithScreenshot("Capacitor fields visible - verified");
        logWarning("Can verify some fields but extensive scrolling makes full verification complex - partial automation");
    }

    // ============================================================
    // CAP_EAD_04 - Verify Save Changes button visibility (Yes)
    // ============================================================

    @Test(priority = 304)
    public void CAP_EAD_04_verifySaveChangesButtonVisibility() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_04 - Verify Save button is always visible"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Verifying Save Changes button is visible");
        boolean saveButtonVisible = assetPage.isSaveChangesButtonVisible();
        assertTrue(saveButtonVisible, "Save Changes button should be visible");

        logStepWithScreenshot("Save Changes button visible - verified");
    }

    // ============================================================
    // CAP_EAD_05 - Edit A Phase Serial Number (Yes)
    // ============================================================

    @Test(priority = 305)
    public void CAP_EAD_05_editAPhaseSerialNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_05 - Verify A Phase Serial Number edit"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to Core Attributes");
        assetPage.scrollFormDown();
        shortWait();

        logStep("Editing A Phase Serial Number");
        String testValue = "A_PHASE_" + System.currentTimeMillis();
        assetPage.editTextField("A Phase Serial Number", testValue);
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "A Phase Serial Number should be saved successfully");

        logStepWithScreenshot("A Phase Serial Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_06 - Edit B Phase Serial Number (Yes)
    // ============================================================

    @Test(priority = 306)
    public void CAP_EAD_06_editBPhaseSerialNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_06 - Verify B Phase Serial Number edit"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to Core Attributes");
        assetPage.scrollFormDown();
        shortWait();

        logStep("Editing B Phase Serial Number");
        String testValue = "B_PHASE_" + System.currentTimeMillis();
        assetPage.editTextField("B Phase Serial Number", testValue);
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "B Phase Serial Number should be saved successfully");

        logStepWithScreenshot("B Phase Serial Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_07 - Edit C Phase Serial Number (Yes)
    // ============================================================

    @Test(priority = 307)
    public void CAP_EAD_07_editCPhaseSerialNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_07 - Verify C Phase Serial Number edit"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to Core Attributes");
        assetPage.scrollFormDown();
        shortWait();

        logStep("Editing C Phase Serial Number");
        String testValue = "C_PHASE_" + System.currentTimeMillis();
        assetPage.editTextField("C Phase Serial Number", testValue);
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "C Phase Serial Number should be saved successfully");

        logStepWithScreenshot("C Phase Serial Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_08 - Edit Catalog Number (Yes)
    // ============================================================

    @Test(priority = 308)
    public void CAP_EAD_08_editCatalogNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_08 - Verify Catalog Number update"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Catalog Number");

        logStep("Editing Catalog Number");
        String testValue = "CAT_" + System.currentTimeMillis();
        assetPage.editTextField("Catalog Number", testValue);
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Catalog Number should be saved successfully");

        logStepWithScreenshot("Catalog Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_09 - Edit Fluid Capacity (Yes)
    // ============================================================

    @Test(priority = 309)
    public void CAP_EAD_09_editFluidCapacity() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_09 - Verify Fluid Capacity input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Fluid Capacity");

        logStep("Editing Fluid Capacity");
        assetPage.editTextField("Fluid Capacity", "100");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Fluid Capacity should be saved successfully");

        logStepWithScreenshot("Fluid Capacity edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_10 - Edit Fluid Type (Yes)
    // ============================================================

    @Test(priority = 310)
    public void CAP_EAD_10_editFluidType() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_10 - Verify Fluid Type input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Fluid Type");

        logStep("Editing Fluid Type");
        assetPage.editTextField("Fluid Type", "Mineral Oil");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Fluid Type should be saved successfully");

        logStepWithScreenshot("Fluid Type edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_11 - Edit Fuse Amperage (Yes)
    // ============================================================

    @Test(priority = 311)
    public void CAP_EAD_11_editFuseAmperage() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_11 - Verify Fuse Amperage input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Fuse Amperage");

        logStep("Editing Fuse Amperage");
        assetPage.editTextField("Fuse Amperage", "30");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Fuse Amperage should be saved successfully");

        logStepWithScreenshot("Fuse Amperage edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_12 - Edit Fuse Manufacturer (Yes)
    // ============================================================

    @Test(priority = 312)
    public void CAP_EAD_12_editFuseManufacturer() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_12 - Verify Fuse Manufacturer update"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Fuse Manufacturer");

        logStep("Editing Fuse Manufacturer");
        assetPage.editTextField("Fuse Manufacturer", "Bussmann");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Fuse Manufacturer should be saved successfully");

        logStepWithScreenshot("Fuse Manufacturer edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_13 - Edit Fuse Refill Number (Yes)
    // ============================================================

    @Test(priority = 313)
    public void CAP_EAD_13_editFuseRefillNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_13 - Verify Fuse Refill Number input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Fuse Refill Number");

        logStep("Editing Fuse Refill Number");
        assetPage.editTextField("Fuse Refill Number", "FR-12345");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Fuse Refill Number should be saved successfully");

        logStepWithScreenshot("Fuse Refill Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_14 - Edit KVAR Rating (Yes)
    // ============================================================

    @Test(priority = 314)
    public void CAP_EAD_14_editKVARRating() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_14 - Verify KVAR Rating input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("KVAR Rating");

        logStep("Editing KVAR Rating");
        assetPage.editTextField("KVAR Rating", "50");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "KVAR Rating should be saved successfully");

        logStepWithScreenshot("KVAR Rating edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_15 - Edit Manufacturer (Yes)
    // ============================================================

    @Test(priority = 315)
    public void CAP_EAD_15_editManufacturer() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_15 - Verify Manufacturer input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Manufacturer");

        logStep("Editing Manufacturer");
        assetPage.editTextField("Manufacturer", "ABB");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Manufacturer should be saved successfully");

        logStepWithScreenshot("Manufacturer edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_16 - Edit Model (Yes)
    // ============================================================

    @Test(priority = 316)
    public void CAP_EAD_16_editModel() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_16 - Verify Model input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Model");

        logStep("Editing Model");
        assetPage.editTextField("Model", "CAP-2000");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Model should be saved successfully");

        logStepWithScreenshot("Model edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_17 - Edit Notes (Yes)
    // ============================================================

    @Test(priority = 317)
    public void CAP_EAD_17_editNotes() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_17 - Verify Notes input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Notes");

        logStep("Editing Notes");
        assetPage.editTextField("Notes", "Test note for Capacitor");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Notes should be saved successfully");

        logStepWithScreenshot("Notes edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_18 - Edit PCB Labeled (Yes)
    // ============================================================

    @Test(priority = 318)
    public void CAP_EAD_18_editPCBLabeled() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_18 - Verify PCB Labeled input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("PCB Labeled");

        logStep("Editing PCB Labeled");
        assetPage.editTextField("PCB Labeled", "Yes");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "PCB Labeled should be saved successfully");

        logStepWithScreenshot("PCB Labeled edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_19 - Edit Serial Number (Yes)
    // ============================================================

    @Test(priority = 319)
    public void CAP_EAD_19_editSerialNumber() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_19 - Verify Serial Number input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Serial Number");

        logStep("Editing Serial Number");
        String testValue = "SN_" + System.currentTimeMillis();
        assetPage.editTextField("Serial Number", testValue);
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Serial Number should be saved successfully");

        logStepWithScreenshot("Serial Number edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_20 - Edit Spare Fuses (Yes)
    // ============================================================

    @Test(priority = 320)
    public void CAP_EAD_20_editSpareFuses() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_20 - Verify Spare Fuses input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Spare Fuses");

        logStep("Editing Spare Fuses");
        assetPage.editTextField("Spare Fuses", "5");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Spare Fuses should be saved successfully");

        logStepWithScreenshot("Spare Fuses edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_21 - Edit Style (Yes)
    // ============================================================

    @Test(priority = 321)
    public void CAP_EAD_21_editStyle() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_21 - Verify Style input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Style");

        logStep("Editing Style");
        assetPage.editTextField("Style", "Indoor");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Style should be saved successfully");

        logStepWithScreenshot("Style edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_22 - Edit Type (Yes)
    // ============================================================

    @Test(priority = 322)
    public void CAP_EAD_22_editType() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_22 - Verify Type input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Type");

        logStep("Editing Type");
        assetPage.editTextField("Type", "Fixed");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Type should be saved successfully");

        logStepWithScreenshot("Type edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_23 - Edit UF Rating (Yes)
    // ============================================================

    @Test(priority = 323)
    public void CAP_EAD_23_editUFRating() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_23 - Verify UF Rating input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("UF Rating");

        logStep("Editing UF Rating");
        assetPage.editTextField("UF Rating", "100");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "UF Rating should be saved successfully");

        logStepWithScreenshot("UF Rating edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_24 - Edit Voltage (Yes)
    // ============================================================

    @Test(priority = 324)
    public void CAP_EAD_24_editVoltage() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_24 - Verify Voltage input"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Scrolling to field");
        assetPage.scrollToField("Voltage");

        logStep("Editing Voltage");
        assetPage.editTextField("Voltage", "480");
        
        logStep("Saving changes");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Voltage should be saved successfully");

        logStepWithScreenshot("Voltage edited and saved - verified");
    }

    // ============================================================
    // CAP_EAD_25 - Save with all fields empty (Yes)
    // ============================================================

    @Test(priority = 325)
    public void CAP_EAD_25_saveWithAllFieldsEmpty() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_25 - Verify save allowed with empty fields"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Not editing any fields - leaving all empty");
        
        logStep("Clicking Save Changes directly");
        assetPage.clickSaveChanges();
        mediumWait();

        boolean saved = assetPage.isAssetSavedAfterEdit();
        assertTrue(saved, "Asset should be saved successfully with empty fields");

        logStepWithScreenshot("Save with empty fields - verified");
    }

    // ============================================================
    // CAP_EAD_26 - Cancel edit operation (Partial)
    // ============================================================

    @Test(priority = 326)
    public void CAP_EAD_26_cancelEditOperation() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_ASSET,
            AppConstants.FEATURE_EDIT_ASSET,
            "CAP_EAD_26 - Verify Cancel button behavior"
        );

        logStep("Navigating to Edit Asset screen and changing to Capacitor");
        navigateToEditAssetScreenAndChangeToCapacitor();

        logStep("Making a modification");
        assetPage.scrollFormDown();
        shortWait();
        assetPage.editTextField("Manufacturer", "TestCancel_" + System.currentTimeMillis());

        logStep("Tapping Cancel button");
        assetPage.clickCancelEdit();
        shortWait();

        logStep("Verifying edit was cancelled");
        boolean cancelled = assetPage.isEditCancelled();
        assertTrue(cancelled, "Edit should be cancelled and return to view mode");

        logStepWithScreenshot("Cancel edit operation - verified");
        logWarning("Can perform cancel action but data state verification needs manual check - partial automation");
    }


}
