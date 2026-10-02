package com.egalvanic.tests;

import com.egalvanic.base.BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.IssuePage;
import com.egalvanic.utils.ExtentReportManager;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * ZP-3927 (iOS 1.57) §6 Photo Import — importing a library photo into an issue.
 *
 * Surface (probe 1.63, 2026-09-21): Issue Details › "Issue Photos" › Gallery opens the system photo
 * picker as a full-height sheet. The decisive oracle is the THUMBNAIL COUNT in the Issue Photos
 * section before/after: a picker that dismissed but added nothing, or a photo that landed twice, is
 * exactly the kind of failure a "picker opened" check would wave through.
 *
 * Photo fixtures: the simulator library is seeded by the harness (see memory
 * sim-photo-seeding-and-ui-strings) — stock IMG_0001-0007 plus qa_win_* seeds with controlled
 * creation dates. Nothing here depends on a specific file; only on the library being non-empty.
 */
public final class ZP3927_PhotoImport_Test extends BaseTest {

    private static final String FEATURE = "Photo import into Issue Photos (ZP-3927)";

    private IssuePage issuePage;

    /** The driver is re-created per test; never reuse a page bound to a quit session ("Session ID is null"). */
    @BeforeMethod(alwaysRun = true)
    public void resetPages() { issuePage = null; }
    private IssuePage issues() {
        if (issuePage == null) issuePage = new IssuePage();
        return issuePage;
    }

    /** Open the first issue's details and land on the Issue Photos section. */
    private void openIssuePhotosSection() {
        loginAndSelectSite();
        if (!issues().navigateToIssuesScreen()) throw new SkipException("Issues screen did not open");
        issues().tapAllTab();
        mediumWait();
        if (!issues().tapFirstIssue()) throw new SkipException("No issue available to open");
        mediumWait();
        if (!issues().isIssueDetailsScreenDisplayed()) throw new SkipException("Issue Details did not open");
        if (!issues().scrollToIssuePhotosSection()) {
            throw new SkipException("'Issue Photos' section not found on this issue — photo import has no entry point here");
        }
    }

    /** Precondition: Gallery → first photo → Use Photo → Save Changes. Returns the new count; skips when not drivable. */
    private int importOnePhotoAndSaveOrSkip(int before) {
        issues().tapGalleryButton();
        mediumWait();
        skipIfPreconditionMissing(() -> issues().isPhotoPickerSheetPresented(), "photo picker did not present");
        if (!issues().pickFirstPhotoFromLibrary()) {
            issues().cancelPhotoPicker();
            throw new SkipException("Photo import could not be driven on this simulator — persistence not checkable");
        }
        skipIfPreconditionMissing(() -> issues().commitIssueChanges(), "'Save Changes' did not commit the imported photo");
        issues().scrollToIssuePhotosSection();
        int after = thumbnailsOrSkip();
        skipIfPreconditionMissing(() -> after > before, "the import did not add a thumbnail (before=" + before + ")");
        return after;
    }

    private int thumbnailsOrSkip() {
        int n = issues().issuePhotoThumbnailCount();
        if (n < 0) throw new SkipException("Issue Photos section header not visible — cannot count thumbnails");
        return n;
    }

    @Test(priority = 1)
    public void TC_PI_01_galleryOpensThePhotoPicker() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_PI_01 - Gallery opens the photo picker from Issue Photos");
        openIssuePhotosSection();
        assertTrue(issues().isGalleryButtonDisplayed(), "The Issue Photos section should offer a 'Gallery' button");
        logStep("Tapping Gallery");
        issues().tapGalleryButton();
        mediumWait();
        boolean presented = issues().isPhotoPickerSheetPresented();
        logStep("picker sheet presented: " + presented + " · addressable photo cells: " + issues().waitForPickerPhotos(4));
        assertTrue(presented, "Tapping Gallery must present the photo picker (the Issue Details chrome should be covered)");
        logStepWithScreenshot("TC_PI_01: picker presented");
        issues().cancelPhotoPicker();
    }

    @Test(priority = 2)
    public void TC_PI_02_importingOnePhotoAddsExactlyOneThumbnail() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_PI_02 - Importing one library photo adds exactly one thumbnail");
        openIssuePhotosSection();
        int before = thumbnailsOrSkip();
        logStep("Issue Photos thumbnails before import: " + before);

        issues().tapGalleryButton();
        mediumWait();
        skipIfPreconditionMissing(() -> issues().isPhotoPickerSheetPresented(), "photo picker did not present");
        boolean picked = issues().pickFirstPhotoFromLibrary();
        logStep("picker dismissed after selection: " + picked);
        if (!picked) {
            issues().cancelPhotoPicker();
            throw new SkipException("The system photo picker grid was not addressable on this simulator "
                    + "(PHPicker runs out-of-process) — import cannot be driven here; covered on device");
        }
        mediumWait();
        issues().scrollToIssuePhotosSection();
        int after = thumbnailsOrSkip();
        logStep("Issue Photos thumbnails after import: " + after);
        assertEquals(after, before + 1,
                "Importing one photo must add exactly one thumbnail to Issue Photos (before=" + before
                + ", after=" + after + ") — a larger delta means a duplicate landed, a zero delta means the "
                + "picker dismissed without importing");
        // 1.67 stages an import until 'Save Changes' (live 2026-10-02) — commit it like a user would.
        assertTrue(issues().commitIssueChanges(), "'Save Changes' must commit the imported photo");
        verifyAppAlive("after importing a photo into the issue");
        logStepWithScreenshot("TC_PI_02: one photo imported");
    }

    @Test(priority = 3)
    public void TC_PI_03_cancellingThePickerImportsNothing() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_PI_03 - Cancelling the picker imports nothing");
        openIssuePhotosSection();
        int before = thumbnailsOrSkip();
        issues().tapGalleryButton();
        mediumWait();
        skipIfPreconditionMissing(() -> issues().isPhotoPickerSheetPresented(), "photo picker did not present");
        logStep("Cancelling the picker");
        assertTrue(issues().cancelPhotoPicker(), "Cancel should dismiss the picker and return to Issue Details");
        issues().scrollToIssuePhotosSection();
        int after = thumbnailsOrSkip();
        assertEquals(after, before,
                "Cancel must import nothing (thumbnails before=" + before + ", after=" + after + ")");
        logStepWithScreenshot("TC_PI_03: cancel imported nothing");
    }

    @Test(priority = 4)
    public void TC_PI_04_importedPhotoSurvivesReopen() {
        ExtentReportManager.createTest(AppConstants.MODULE_ISSUES, FEATURE,
                "TC_PI_04 - An imported photo is still there after closing and reopening the issue");
        openIssuePhotosSection();
        int count = thumbnailsOrSkip();
        if (count == 0) {
            logStep("No photo on this issue yet — importing and saving one as the precondition");
            count = importOnePhotoAndSaveOrSkip(count);
        }
        logStep("Thumbnails before close: " + count);
        issues().cancelSheetIfOpen();                 // 'Close' on Issue Details
        mediumWait();
        openIssuePhotosSection();
        int again = thumbnailsOrSkip();
        logStep("Thumbnails after reopen: " + again);
        assertEquals(again, count,
                "Imported photos must persist across close/reopen (before=" + count + ", after=" + again + ")");
        logStepWithScreenshot("TC_PI_04: photo persisted");
    }
}
