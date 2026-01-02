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
        try {
            TakesScreenshot driver = (TakesScreenshot) DriverManager.getDriver();
            return driver.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get Base64 screenshot: " + e.getMessage());
            return null;
        }
    }
}
