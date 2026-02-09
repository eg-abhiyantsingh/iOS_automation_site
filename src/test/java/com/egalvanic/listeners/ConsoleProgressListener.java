package com.egalvanic.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * ConsoleProgressListener - Clean real-time test progress for GitHub Actions
 * 
 * Shows:
 * - Test start/pass/fail/skip status with emojis
 * - Progress percentage [completed/total - %]
 * - Duration in seconds
 * - Clean summary at end of each <test> block
 * 
 * IMPORTANT: Counters reset on each onStart() call because TestNG creates
 * one listener instance shared across ALL <test> blocks in a suite XML.
 * Without reset, counters accumulate (e.g., 42 S3 drift + 4 Auth = 46),
 * causing progress bar overflow and "count is negative" crash from
 * String.repeat() with negative values.
 * 
 * Bug fix: ZP-774 — "count is negative: -200" when S3 drift + mobile
 * smoke tests run in same suite XML.
 */
public class ConsoleProgressListener implements ITestListener {
    
    // Per-<test>-block counters — reset in onStart()
    private int passed = 0;
    private int failed = 0;
    private int skipped = 0;
    private int total = 0;
    private long suiteStartTime;
    
    // Suite-wide cumulative counters (for final summary if needed)
    private int cumulativePassed = 0;
    private int cumulativeFailed = 0;
    private int cumulativeSkipped = 0;
    
    @Override
    public void onStart(ITestContext context) {
        // CRITICAL: Reset per-block counters for each <test> block
        // Without this, counters from previous <test> blocks accumulate
        // and cause String.repeat() to crash with negative values
        passed = 0;
        failed = 0;
        skipped = 0;
        total = context.getAllTestMethods().length;
        suiteStartTime = System.currentTimeMillis();
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  🧪 STARTING TEST SUITE: " + padRight(context.getName(), 48) + "║");
        System.out.println("║  📊 Total Tests: " + padRight(String.valueOf(total), 56) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getRealClass().getSimpleName();
        System.out.println("▶️  RUNNING: " + className + "." + testName);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        passed++;
        cumulativePassed++;
        printProgress("✅ PASSED", result);
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        failed++;
        cumulativeFailed++;
        printProgress("❌ FAILED", result);
        
        // Print brief error message (not full stack trace)
        Throwable error = result.getThrowable();
        if (error != null) {
            System.out.println("   └─ Error: " + error.getMessage());
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        skipped++;
        cumulativeSkipped++;
        printProgress("⏭️  SKIPPED", result);
    }
    
    private void printProgress(String status, ITestResult result) {
        int completed = passed + failed + skipped;
        int percent = total > 0 ? Math.min((completed * 100) / total, 100) : 0;
        long duration = (result.getEndMillis() - result.getStartMillis()) / 1000;
        
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getRealClass().getSimpleName();
        
        // Create progress bar (clamped to prevent negative repeat)
        int barLength = 20;
        int filledLength = Math.min((percent * barLength) / 100, barLength);
        int emptyLength = Math.max(barLength - filledLength, 0);
        String progressBar = "█".repeat(filledLength) + "░".repeat(emptyLength);
        
        System.out.println(String.format("%s: %s.%s (%ds)",
            status,
            className,
            testName,
            duration));
        System.out.println(String.format("   └─ Progress: [%s] %d/%d (%d%%)",
            progressBar,
            completed,
            total,
            percent));
        System.out.println();
    }
    
    @Override
    public void onFinish(ITestContext context) {
        long totalDuration = (System.currentTimeMillis() - suiteStartTime) / 1000;
        int minutes = (int) (totalDuration / 60);
        int seconds = (int) (totalDuration % 60);
        
        String statusEmoji = failed == 0 ? "🎉" : "⚠️";
        String statusText = failed == 0 ? "ALL TESTS PASSED!" : "SOME TESTS FAILED";
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  📊 TEST RESULTS SUMMARY                                                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  " + statusEmoji + " Status:  " + padRight(statusText, 60) + "║");
        System.out.println("║  ✅ Passed:  " + padRight(String.valueOf(passed), 60) + "║");
        System.out.println("║  ❌ Failed:  " + padRight(String.valueOf(failed), 60) + "║");
        System.out.println("║  ⏭️  Skipped: " + padRight(String.valueOf(skipped), 59) + "║");
        System.out.println("║  📊 Total:   " + padRight(String.valueOf(total), 60) + "║");
        System.out.println("║  ⏱️  Duration: " + padRight(minutes + "m " + seconds + "s", 58) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private String padRight(String text, int length) {
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }
}
