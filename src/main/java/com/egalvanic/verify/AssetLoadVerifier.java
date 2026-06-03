package com.egalvanic.verify;

import com.egalvanic.utils.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Verifies that a PDF / image / single-line-diagram actually RENDERED, not merely that
 * its container element exists. This is the gap behind "PDFs/assets that fail to load":
 * the suite asserts the PDFView is present and moves on, never that a page is visible.
 */
public final class AssetLoadVerifier {

    private IOSDriver driver() { return DriverManager.getDriver(); }

    /** Fail unless the element's on-screen region contains a non-blank, varied render. */
    public void assertRendered(WebElement container, String what) {
        if (container == null) throw new VerificationError(what + " container is null (not present)");
        BufferedImage shot = screenshot();
        Rectangle r = container.getRect();
        double scale = (double) shot.getWidth() / driver().manage().window().getSize().getWidth();
        int x = (int) (r.getX() * scale), y = (int) (r.getY() * scale);
        int w = (int) (r.getWidth() * scale), h = (int) (r.getHeight() * scale);

        ImageAnalysis.Result region = ImageAnalysis.analyze(shot, x, y, w, h);
        if (region.looksBlank()) {
            throw new VerificationError(what + " did not render — region appears blank/uniform (" + region + ")");
        }
    }

    /** PDF / SLD: container must exist AND a non-blank page must be visible. */
    public void assertPdfRendered(WebElement pdfView) {
        assertRendered(pdfView, "PDF/SLD");
    }

    private BufferedImage screenshot() {
        try {
            return ImageIO.read(new ByteArrayInputStream(driver().getScreenshotAs(OutputType.BYTES)));
        } catch (Exception e) {
            throw new VerificationError("could not capture/decode screenshot for render check", e);
        }
    }
}
