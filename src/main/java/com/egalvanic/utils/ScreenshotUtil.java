package com.egalvanic.utils;

import com.egalvanic.constants.AppConstants;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Screenshot Utility - Capture screenshots for test reports
 */
public class ScreenshotUtil {

    private ScreenshotUtil() {
        // Private constructor
    }

    /**
     * Capture screenshot and save to file
     * 
     * @param screenshotName Name for the screenshot file
     * @return Absolute path to the screenshot file, or null if failed
     */
    public static String captureScreenshot(String screenshotName) {
        try {
            // Create screenshots directory if not exists
            File screenshotDir = new File(AppConstants.SCREENSHOT_PATH);
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            // Generate unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = screenshotName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".png";
            String filePath = AppConstants.SCREENSHOT_PATH + fileName;

            // Capture screenshot
            TakesScreenshot driver = (TakesScreenshot) DriverManager.getDriver();
            File sourceFile = driver.getScreenshotAs(OutputType.FILE);
            File destFile = new File(filePath);
            
            FileUtils.copyFile(sourceFile, destFile);
            
            System.out.println("📸 Screenshot captured: " + fileName);
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot with default name
     */
    public static String captureScreenshot() {
        return captureScreenshot("screenshot");
    }

    /**
     * Cleanup old screenshots (older than specified days)
     * 
     * @param daysOld Delete screenshots older than this many days
     */
    public static void cleanupOldScreenshots(int daysOld) {
        try {
            File screenshotDir = new File(AppConstants.SCREENSHOT_PATH);
            if (screenshotDir.exists() && screenshotDir.isDirectory()) {
                File[] files = screenshotDir.listFiles();
                if (files != null) {
                    long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
                    int deletedCount = 0;
                    
                    for (File file : files) {
                        if (file.isFile() && file.lastModified() < cutoffTime) {
                            if (file.delete()) {
                                deletedCount++;
                            }
                        }
                    }
                    
                    if (deletedCount > 0) {
                        System.out.println("🗑️ Cleaned up " + deletedCount + " old screenshots");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to cleanup screenshots: " + e.getMessage());
        }
    }

    /**
     * Cleanup all screenshots in directory
     */
    public static void cleanupAllScreenshots() {
        try {
            File screenshotDir = new File(AppConstants.SCREENSHOT_PATH);
            if (screenshotDir.exists() && screenshotDir.isDirectory()) {
                FileUtils.cleanDirectory(screenshotDir);
                System.out.println("🗑️ All screenshots cleaned up");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to cleanup screenshots: " + e.getMessage());
        }
    }

    /**
     * Get screenshot as Base64 string
     */
    public static String getScreenshotAsBase64() {
        // Driver-free suites (S3 drift, API contract, verify self-tests) have no
        // simulator at all — skip silently instead of printing "Driver not
        // initialized" per report step (pure log noise; isDriverActive is an
        // in-memory check, no Appium HTTP).
        if (!DriverManager.isDriverActive()) {
            return null;
        }
        try {
            TakesScreenshot driver = (TakesScreenshot) DriverManager.getDriver();
            return driver.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get Base64 screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get screenshot as compressed JPEG Base64 string.
     *
     * iOS sim screenshots are 1290×2796 PNGs (~150–200 KB Base64). Embedding
     * 50+ of these per test would balloon the Detailed report past the email
     * size budget. Re-encode to JPEG at 50 % quality + 50 % linear resize so
     * we can pack many screenshots per test (~15–25 KB each) and still fit
     * the email's 22 MB attachment cap.
     *
     * Toggle with -Dscreenshots.compress=false (default true).
     * Tune with -Dscreenshots.jpegQuality=0.5 and -Dscreenshots.scale=0.5.
     */
    public static String getScreenshotAsBase64Compressed() {
        // No driver at all (driver-free suites: S3 drift, API contract, verify
        // self-tests) — skip silently; see getScreenshotAsBase64.
        if (!DriverManager.isDriverActive()) {
            return "";
        }
        if (!COMPRESS_ENABLED) {
            return getScreenshotAsBase64();
        }
        try {
            TakesScreenshot driver = (TakesScreenshot) DriverManager.getDriver();
            byte[] pngBytes = driver.getScreenshotAs(OutputType.BYTES);
            java.awt.image.BufferedImage src =
                javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pngBytes));
            if (src == null) {
                return java.util.Base64.getEncoder().encodeToString(pngBytes);
            }
            int dstW = Math.max(1, (int) Math.round(src.getWidth() * SCALE));
            int dstH = Math.max(1, (int) Math.round(src.getHeight() * SCALE));
            java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(
                dstW, dstH, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = dst.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, dstW, dstH, null);
            g.dispose();

            // JPEG-encode at configured quality
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageWriter writer = javax.imageio.ImageIO
                .getImageWritersByFormatName("jpeg").next();
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality((float) JPEG_QUALITY);
            try (javax.imageio.stream.ImageOutputStream ios =
                     javax.imageio.ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(dst, null, null), param);
            }
            writer.dispose();

            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            // If the session is DEAD, the full-PNG fallback would just hit another
            // 90s readTimeout (and ExtentReport calls this per step + at teardown),
            // padding every WDA-death hang by minutes — the screenshot retry storm
            // seen in Location/Offline (CI run 27557701204). Skip the fallback on a
            // session-death signature; only retry for genuine compression errors.
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("may have died") || msg.contains("Session")
                    || msg.contains("ECONNREFUSED") || msg.contains("timeout")
                    || msg.contains("not created") || msg.contains("terminated")) {
                System.err.println("⚠️ Screenshot skipped — session appears dead: " + msg);
                return "";
            }
            System.err.println("⚠️ Compressed screenshot failed, falling back to full PNG: " + msg);
            return getScreenshotAsBase64();
        }
    }

    private static final boolean COMPRESS_ENABLED =
        !"false".equalsIgnoreCase(System.getProperty("screenshots.compress", "true"));
    // Detailed reports go to workflow artifacts (not email), so we can be
    // generous with quality. GitHub artifacts cap each file at 2 GB which
    // is essentially unbounded for our purposes. Quality bumped to .65 +
    // 0.55× scale → readable text, ~40–50 KB per shot. A 100-test module
    // with 30 shots/test = ~150 MB detailed report — fast to download
    // as a single ZIP from the workflow artifacts UI.
    private static final double JPEG_QUALITY =
        Double.parseDouble(System.getProperty("screenshots.jpegQuality", "0.65"));
    private static final double SCALE =
        Double.parseDouble(System.getProperty("screenshots.scale", "0.55"));
}
