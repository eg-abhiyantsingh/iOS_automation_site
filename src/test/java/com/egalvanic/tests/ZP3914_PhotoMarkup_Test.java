package com.egalvanic.tests;

import com.egalvanic.base.ZP3914BaseTest;
import com.egalvanic.constants.AppConstants;
import com.egalvanic.pages.PhotoMarkupPage;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ZP-3914 (iOS 1.59) — "Photo Viewer", "Zoom tools", "Markup tools" on Asset Details › Asset Photos.
 *
 * Library photos only (the simulator has no camera — 'Camera' is disabled there). Nothing is ever saved: every
 * case ends by cancelling the review or by 'Discard Changes?' › Discard on the asset, so the asset's photo count is
 * the same before and after (asserted in TC_PM_06).
 */
public final class ZP3914_PhotoMarkup_Test extends ZP3914BaseTest {

    private static final String FEATURE = "Photo markup + viewer (ZP-3914)";
    /** Asset used for photo work (any asset will do; this one exists on the automation site). */
    private static final String ASSET = System.getProperty("ZP3914_PHOTO_ASSET", "Generator-1");

    private void openAssetPhotosOrSkip(String tcId) {
        loginAndWaitForTabs(tcId);
        assetPage.navigateToAssetList();
        assetPage.searchAsset(ASSET);
        skipIfPreconditionMissing(() -> assetPage.selectAssetByName(ASSET), tcId + ": asset '" + ASSET + "' not found on the site");
        assertTrue(photos.jumpToAssetPhotos(), "Asset Details' 'Photos' jump must show 'Asset Photos' with 'Gallery'");
    }

    private void pickPhotoOrSkip(String tcId) {
        skipIfPreconditionMissing(() -> photos.pickFirstLibraryPhoto(), tcId + ": the photo library picker gave no photo (empty simulator library?)");
    }

    private void openEditorOrSkip(String tcId) {
        openAssetPhotosOrSkip(tcId);
        pickPhotoOrSkip(tcId);
        assertTrue(photos.openMarkupFromReview(), "'" + PhotoMarkupPage.MARK_UP + "' must open the '" + PhotoMarkupPage.EDITOR_TITLE + "' editor");
    }

    @Test(priority = 1)
    public void TC_PM_01_reviewPhotoOffersMarkUp() {
        featureTest(FEATURE, "TC_PM_01 - A library pick lands on 'Review Photo' with Cancel · Mark Up · Use Photo (no Retake)");
        openAssetPhotosOrSkip("TC_PM_01");
        logStep("Camera enabled: " + photos.isCameraEnabled() + " (disabled on simulators)");
        pickPhotoOrSkip("TC_PM_01");
        assertTrue(photos.isReviewShown(), "'" + PhotoMarkupPage.REVIEW_TITLE + "' must show the picked photo");
        List<String> buttons = photos.reviewButtons();
        logStep("Review buttons: " + buttons);
        assertTrue(buttons.containsAll(List.of("Cancel", PhotoMarkupPage.MARK_UP, PhotoMarkupPage.USE_PHOTO)),
                "Review offers Cancel, '" + PhotoMarkupPage.MARK_UP + "' and '" + PhotoMarkupPage.USE_PHOTO + "' — got " + buttons);
        assertFalse(buttons.contains(PhotoMarkupPage.RETAKE), "A library pick cannot be retaken — no '" + PhotoMarkupPage.RETAKE + "'");
        logStepWithScreenshot("TC_PM_01: review contract verified");
    }

    @Test(priority = 2)
    public void TC_PM_02_editorToolsAndDefaults() {
        featureTest(FEATURE, "TC_PM_02 - Markup editor: six tools (Select default), seven colours (Red default), Undo/Clear All off");
        openEditorOrSkip("TC_PM_02");
        assertEquals(photos.presentOf(PhotoMarkupPage.TOOLS), PhotoMarkupPage.TOOLS, "All markup tools are offered");
        assertEquals(photos.selectedOf(PhotoMarkupPage.TOOLS), "Select", "'Select' is the starting tool");
        assertEquals(photos.presentOf(PhotoMarkupPage.COLOURS), PhotoMarkupPage.COLOURS, "All colours are offered");
        assertEquals(photos.selectedOf(PhotoMarkupPage.COLOURS), "Red", "'Red' is the starting colour");
        assertFalse(photos.isEnabled("Undo"), "'Undo' is disabled before any mark");
        assertFalse(photos.isEnabled("Clear All"), "'Clear All' is disabled before any mark");
        assertNotNull(photos.canvas(), "The photo canvas must be shown");
        String sheet = photos.cancelEditor("Discard");
        assertEquals(sheet, "", "Cancelling an editor with no marks must not ask anything — got '" + sheet + "'");
        assertTrue(eventually(() -> !photos.isEditorShown(), 6), "Cancel must leave the editor");
        logStepWithScreenshot("TC_PM_02: editor defaults verified");
    }

    @Test(priority = 3)
    public void TC_PM_03_arrowMarkEnablesUndo() {
        featureTest(FEATURE, "TC_PM_03 - Drawing an arrow enables Undo / Clear All; Undo removes it again");
        openEditorOrSkip("TC_PM_03");
        assertTrue(photos.chooseTool("Arrow"), "'Arrow' must become the selected tool");
        assertTrue(photos.dragOnCanvas(), "Drag across the photo");
        assertTrue(eventually(() -> photos.isEnabled("Undo"), 4), "A placed arrow must enable 'Undo'");
        assertTrue(photos.isEnabled("Clear All"), "A placed arrow must enable 'Clear All'");
        assertTrue(photos.isPresent("Delete"), "The selected mark offers 'Delete'");
        assertTrue(photos.pressEditorButton("Undo"), "Press 'Undo'");
        assertTrue(eventually(() -> !photos.isEnabled("Undo"), 4), "Undoing the only mark must disable 'Undo' again");
        assertFalse(photos.isEnabled("Clear All"), "…and 'Clear All'");
        logStepWithScreenshot("TC_PM_03: arrow + undo verified");
    }

    @Test(priority = 4)
    public void TC_PM_04_textMarkNeedsLabelCappedAt200() {
        featureTest(FEATURE, "TC_PM_04 - A text mark asks for a label (Done disabled while empty) and caps it at 200 characters");
        openEditorOrSkip("TC_PM_04");
        assertTrue(photos.chooseTool("Text"), "'Text' must become the selected tool");
        assertTrue(photos.tapCanvasCentre(), "Tap the photo to place text");
        assertTrue(eventually(photos::isLabelBarShown, 5), "Placing text opens the label bar ('" + PhotoMarkupPage.LABEL_PLACEHOLDER + "', n/200)");
        assertEquals(photos.labelCounter(), 0, "The counter starts at 0/200");
        assertTrue(photos.isTextVisible(PhotoMarkupPage.TEXT_NEEDS_LABEL), "The bar says '" + PhotoMarkupPage.TEXT_NEEDS_LABEL + "'");
        assertFalse(photos.isLabelDoneEnabled(), "'Done' is disabled while the label is empty");
        photos.typeLabel("QA".repeat(100));   // exactly the cap
        assertEquals(photos.labelCounter(), PhotoMarkupPage.LABEL_MAX, "200 characters read " + PhotoMarkupPage.LABEL_MAX + "/200");
        // Ten more, one key at a time — the way a person keeps typing past the limit.
        for (int i = 0; i < 10; i++) photos.typeKey("Z");
        assertEquals(photos.labelCounter(), PhotoMarkupPage.LABEL_MAX, "The counter must stay at " + PhotoMarkupPage.LABEL_MAX + "/200");
        String field = photos.labelFieldValue();
        int len = field == null ? -1 : field.length();
        logStep("Label field after 210 keystrokes: " + len + " characters (counter " + photos.labelCounter() + "/200)");
        assertEquals(len, PhotoMarkupPage.LABEL_MAX, "The label field must refuse characters past 200 — it shows " + len
                + " while the counter says 200/200, so the extra text is dropped silently when the mark is saved");
        assertTrue(photos.isLabelDoneEnabled(), "'Done' enables once the label has text");
        assertTrue(photos.pressLabelDone(), "'Done' closes the label bar");
        assertTrue(eventually(() -> photos.isEnabled("Undo"), 4), "The labelled text mark is placed (Undo enabled)");
        logStepWithScreenshot("TC_PM_04: text label cap verified");
    }

    @Test(priority = 5)
    public void TC_PM_05_cancelWithMarksAsksToDiscard() {
        featureTest(FEATURE, "TC_PM_05 - Cancel with marks asks 'Discard markup?'; Keep Editing keeps them, Discard drops them");
        openEditorOrSkip("TC_PM_05");
        assertTrue(photos.chooseTool("Box") && photos.dragOnCanvas(), "Draw a box");
        assertTrue(eventually(() -> photos.isEnabled("Undo"), 4), "The box is placed");
        String sheet = photos.cancelEditor("Keep Editing");
        assertEquals(sheet, PhotoMarkupPage.DISCARD_TITLE + " | " + PhotoMarkupPage.DISCARD_MESSAGE,
                "Cancel with marks must ask '" + PhotoMarkupPage.DISCARD_TITLE + "' / '" + PhotoMarkupPage.DISCARD_MESSAGE + "'");
        assertTrue(photos.isEditorShown() && photos.isEnabled("Undo"), "'Keep Editing' keeps the editor and the mark");
        String again = photos.cancelEditor("Discard");
        assertTrue(again.startsWith(PhotoMarkupPage.DISCARD_TITLE), "The question comes back on the next Cancel");
        assertTrue(eventually(() -> !photos.isEditorShown(), 6), "'Discard' must close the editor");
        assertTrue(eventually(photos::isReviewShown, 6), "…back on 'Review Photo' — the photo itself is unaffected");
        logStepWithScreenshot("TC_PM_05: discard sheet verified");
    }

    @Test(priority = 6)
    public void TC_PM_06_doneStagesPhotoAndViewerPinchZooms() {
        featureTest(FEATURE, "TC_PM_06 - Editor Done stages the marked-up photo; the viewer pinch-zooms it; discarding leaves the asset unchanged");
        openAssetPhotosOrSkip("TC_PM_06");
        int before = photos.categoryCount("Profile");
        pickPhotoOrSkip("TC_PM_06");
        assertTrue(photos.openMarkupFromReview(), "Open the editor");
        assertTrue(photos.chooseTool("Draw") && photos.dragOnCanvas(), "Draw on the photo");
        assertTrue(photos.doneEditor(), "Editor 'Done' must compose the marks and close");
        assertTrue(eventually(() -> photos.categoryCount("Profile") == before + 1, 8),
                "The marked-up photo is staged on the asset ('Profile' " + before + " → " + (before + 1) + ") — got " + photos.categoryCount("Profile"));
        assertTrue(photos.openFirstThumbnail(), "Tapping the thumbnail opens the viewer ('Mark Up' + 'Close')");
        int w0 = photos.viewerImageWidth();
        assertTrue(photos.pinchViewer(3.0), "The viewer must take a pinch");
        int w1 = photos.viewerImageWidth();
        logStep("Viewer image width " + w0 + " → " + w1 + " after pinch ×3");
        assertTrue(w1 >= w0 * 2, "Pinch-zoom must enlarge the photo (≥2×): " + w0 + " → " + w1);
        assertTrue(photos.closeViewer(), "'Close' leaves the viewer");
        String alert = photos.discardAssetChanges();
        assertEquals(alert, "Discard Changes?", "Leaving with a staged photo must ask 'Discard Changes?'");
        logStepWithScreenshot("TC_PM_06: staged + zoomed + discarded");
    }

    @Test(priority = 7)
    public void TC_PM_07_doubleTapZoomsViewer() {
        featureTest(FEATURE, "TC_PM_07 - Double-tap zooms the photo viewer (shipped after 1.69)");
        skipIfPreconditionMissing(() -> AppConstants.appVersionAtLeast(1, 70),
                "TC_PM_07: double-tap zoom landed 2026-10-07, after the app under test (" + AppConstants.appVersion() + ")");
        openAssetPhotosOrSkip("TC_PM_07");
        int before = photos.categoryCount("Profile");
        pickPhotoOrSkip("TC_PM_07");
        assertTrue(photos.pressEditorButton(PhotoMarkupPage.USE_PHOTO), "'Use Photo' stages the photo");
        assertTrue(eventually(() -> photos.categoryCount("Profile") == before + 1, 8), "Photo staged");
        assertTrue(photos.openFirstThumbnail(), "Open the viewer");
        int w0 = photos.viewerImageWidth();
        photos.doubleTapViewer();
        int w1 = photos.viewerImageWidth();
        logStep("Viewer width " + w0 + " → " + w1 + " after double-tap");
        assertTrue(w1 >= w0 * 2, "Double-tap must zoom in (~2.5×): " + w0 + " → " + w1);
        photos.closeViewer();
        photos.discardAssetChanges();
    }

    @AfterMethod(alwaysRun = true)
    public void neverSavePhotos(ITestResult result) {
        captureFailureState(result);
        try {
            if (photos == null) return;
            if (photos.isViewerShown()) photos.closeViewer();
            if (photos.isEditorShown()) photos.cancelEditor("Discard");
            if (photos.isReviewShown()) photos.pressEditorButton("Cancel");
            photos.discardAssetChanges();
        } catch (Exception ignored) { }
    }
}
