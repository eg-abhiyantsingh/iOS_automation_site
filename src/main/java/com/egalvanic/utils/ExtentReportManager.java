package com.egalvanic.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.egalvanic.constants.AppConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Extent Report Manager - Dual Report System
 * 
 * Two Reports:
 * 1. DETAILED REPORT - For QA Team (screenshots, logs, step details)
 * 2. CLIENT REPORT - For Client (Module > Feature > Test Name > Pass/Fail only)
 * 
 * Features:
 * - Hierarchical structure: Module > Feature > Test
 * - Thread-safe for parallel execution
 * - Email notification on completion
 */
public class ExtentReportManager {

    private static ExtentReports detailedReport;
    private static ExtentReports clientReport;
    
    private static ThreadLocal<ExtentTest> detailedTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> clientTest = new ThreadLocal<>();
    
    // Hierarchical nodes for Client Report
    private static Map<String, ExtentTest> clientModuleNodes = new HashMap<>();
    private static Map<String, ExtentTest> clientFeatureNodes = new HashMap<>();
    
    private static String timestamp;
    private static String detailedReportPath;
    private static String clientReportPath;

    private ExtentReportManager() {
        // Private constructor
    }

    /**
     * Initialize both reports
     */
    public static void initReports() {
        timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        
        // Create report directories
        new File(AppConstants.DETAILED_REPORT_PATH).mkdirs();
        new File(AppConstants.CLIENT_REPORT_PATH).mkdirs();
        
        initDetailedReport();
        initClientReport();
        
        System.out.println("📊 Both Extent Reports initialized");
    }

    /**
     * Initialize Detailed Report - Full details with screenshots
     */
    private static void initDetailedReport() {
        detailedReportPath = AppConstants.DETAILED_REPORT_PATH + "Detailed_Report_" + timestamp + ".html";
        
        ExtentSparkReporter spark = new ExtentSparkReporter(detailedReportPath);
        spark.config().setDocumentTitle("eGalvanic iOS Automation - Detailed Report");
        spark.config().setReportName("Detailed Test Execution Report");
        spark.config().setTheme(Theme.DARK);
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        spark.config().setEncoding("UTF-8");
        
        // Custom CSS for detailed report - ensure images display properly
        spark.config().setCss(
            ".badge-primary { background-color: #007bff; } " +
            ".badge-success { background-color: #28a745; } " +
            ".badge-danger { background-color: #dc3545; } " +
            ".badge-warning { background-color: #ffc107; } " +
            ".test-content { padding: 15px; } " +
            // Ensure Base64 images display properly
            ".r-img { max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 4px; margin: 10px 0; } " +
            ".screen-img { max-width: 800px; cursor: pointer; } " +
            ".media-container img { max-width: 100%; height: auto; }"
        );

        detailedReport = new ExtentReports();
        detailedReport.attachReporter(spark);
        
        // System Info
        detailedReport.setSystemInfo("Application", "eGalvanic iOS App");
        detailedReport.setSystemInfo("Platform", AppConstants.PLATFORM_NAME);
        detailedReport.setSystemInfo("Device", AppConstants.DEVICE_NAME);
        detailedReport.setSystemInfo("iOS Version", AppConstants.PLATFORM_VERSION);
        detailedReport.setSystemInfo("Automation Tool", "Appium");
        detailedReport.setSystemInfo("Framework", "Page Object Model");
        detailedReport.setSystemInfo("Report Type", "DETAILED (Internal QA)");
        detailedReport.setSystemInfo("Environment", "QA");
        detailedReport.setSystemInfo("Executed By", System.getProperty("user.name"));

        System.out.println("📊 Detailed Report initialized: " + detailedReportPath);
    }

    /**
     * Initialize Client Report - Clean Module > Feature > Test > Pass/Fail only
     */
    private static void initClientReport() {
        clientReportPath = AppConstants.CLIENT_REPORT_PATH + "Client_Report_" + timestamp + ".html";
        
        ExtentSparkReporter spark = new ExtentSparkReporter(clientReportPath);
        spark.config().setDocumentTitle("eGalvanic iOS - Test Results");
        spark.config().setReportName("Test Execution Summary");
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setTimeStampFormat("MMMM dd, yyyy");
        spark.config().setEncoding("UTF-8");
        
        // Custom CSS for Client Report - HIDE all technical details
        spark.config().setCss(
            // Hide screenshots and media
            ".media-container { display: none !important; } " +
            ".img-container { display: none !important; } " +
            "img { display: none !important; } " +
            
            // Hide logs and technical details
            ".details-col { display: none !important; } " +
            "pre { display: none !important; } " +
            ".exception-part { display: none !important; } " +
            ".step-details { display: none !important; } " +
            ".test-detail { display: none !important; } " +
            
            // Hide tags/categories
            ".category-list { display: none !important; } " +
            ".tag { display: none !important; } " +
            ".badge-pill { display: none !important; } " +
            
            // Clean styling for Module > Feature > Test
            ".node { padding: 8px 12px !important; margin: 3px 0 !important; " +
            "        border-left: 4px solid #007bff !important; background: #f8f9fa !important; }" +
            ".node-name { font-size: 14px !important; font-weight: 500 !important; }" +
            
            // Status badges
            ".badge-success { background-color: #28a745 !important; font-size: 12px !important; padding: 4px 8px !important; }" +
            ".badge-danger { background-color: #dc3545 !important; font-size: 12px !important; padding: 4px 8px !important; }" +
            ".badge-warning { background-color: #ffc107 !important; color: #000 !important; font-size: 12px !important; }" +
            ".badge-skip { background-color: #6c757d !important; font-size: 12px !important; }" +
            
            // Clean layout
            "body { font-family: 'Segoe UI', Arial, sans-serif !important; }" +
            ".container { max-width: 1200px !important; }" +
            
            // Hide step logs
            ".node-step, .step, .log { display: none !important; }"
        );

        clientReport = new ExtentReports();
        clientReport.attachReporter(spark);
        
        // Minimal System Info for Client
        clientReport.setSystemInfo("Application", "eGalvanic iOS");
        clientReport.setSystemInfo("Test Date", new SimpleDateFormat("MMMM dd, yyyy").format(new Date()));

        System.out.println("📊 Client Report initialized: " + clientReportPath);
    }

    /**
     * Create test with Module > Feature > Test Name hierarchy
     * 
     * @param moduleName   e.g., "Site Selection"
     * @param featureName  e.g., "Select Site Screen"
     * @param testName     e.g., "TC01 - Verify Select Site screen UI elements"
     */
    public static void createTest(String moduleName, String featureName, String testName) {
        // === DETAILED REPORT: Flat test with categories ===
        ExtentTest detailed = detailedReport.createTest(testName);
        detailed.assignCategory(moduleName, featureName);
        detailedTest.set(detailed);
        
        // === CLIENT REPORT: Hierarchical Module > Feature > Test ===
        // Get or create Module node
        String moduleKey = moduleName;
        ExtentTest moduleNode = clientModuleNodes.get(moduleKey);
        if (moduleNode == null) {
            moduleNode = clientReport.createTest(moduleName);
            clientModuleNodes.put(moduleKey, moduleNode);
        }
        
        // Get or create Feature node under Module
        String featureKey = moduleName + "|" + featureName;
        ExtentTest featureNode = clientFeatureNodes.get(featureKey);
        if (featureNode == null) {
            featureNode = moduleNode.createNode(featureName);
            clientFeatureNodes.put(featureKey, featureNode);
        }
        
        // Create Test node under Feature
        ExtentTest testNode = featureNode.createNode(testName);
        clientTest.set(testNode);
        
        System.out.println("📋 Test created: " + moduleName + " > " + featureName + " > " + testName);
    }

    // ================================================================
    // LOGGING METHODS - DETAILED REPORT ONLY
    // ================================================================

    /**
     * Log info message (Detailed report only)
     */
    public static void logInfo(String message) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }

    /**
     * Log step with screenshot (Detailed report only) - Uses Base64 for portability
     */
    public static void logStepWithScreenshot(String step, String screenshotPath) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.INFO, step);
            // Add Base64 screenshot for better portability
            String base64 = ScreenshotUtil.getScreenshotAsBase64();
            if (base64 != null) {
                try {
                    test.addScreenCaptureFromBase64String(base64);
                } catch (Exception e) {
                    System.out.println("⚠️ Screenshot attachment failed: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Log step with Base64 screenshot directly
     */
    public static void logStepWithBase64Screenshot(String step) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.INFO, step);
            String base64 = ScreenshotUtil.getScreenshotAsBase64();
            if (base64 != null) {
                try {
                    test.addScreenCaptureFromBase64String(base64);
                } catch (Exception e) {
                    System.out.println("⚠️ Screenshot attachment failed: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Log warning (Detailed report only)
     */
    public static void logWarning(String message) {
        ExtentTest test = detailedTest.get();
        if (test != null) {
            test.log(Status.WARNING, "⚠️ " + message);
        }
    }

    // ================================================================
    // RESULT METHODS - BOTH REPORTS
    // ================================================================

    /**
     * Log pass result (Both reports)
     */
    public static void logPass(String message) {
        // Detailed Report - Full message
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.PASS, "✅ " + message);
        }
        
        // Client Report - Just PASS status (no message details)
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.pass("PASS");
        }
    }

    /**
     * Log fail result (Both reports)
     */
    public static void logFail(String message) {
        // Detailed Report - Full message
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.FAIL, "❌ " + message);
        }
        
        // Client Report - Just FAIL status (no message details)
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.fail("FAIL");
        }
    }

    /**
     * Log fail with screenshot (Detailed only shows screenshot) - Uses Base64 for portability
     */
    public static void logFailWithScreenshot(String message, Throwable throwable) {
        // Detailed Report - Full details
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.FAIL, "❌ " + message);
            if (throwable != null) {
                detailed.log(Status.FAIL, throwable);
            }
            // Add Base64 screenshot for better portability
            String base64 = ScreenshotUtil.getScreenshotAsBase64();
            if (base64 != null) {
                try {
                    detailed.addScreenCaptureFromBase64String(base64);
                } catch (Exception e) {
                    System.out.println("⚠️ Screenshot attachment failed: " + e.getMessage());
                }
            }
        }
        
        // Client Report - Just FAIL status
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.fail("FAIL");
        }
    }

    /**
     * Log skip result (Both reports)
     */
    public static void logSkip(String message) {
        // Detailed Report - Full message
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.log(Status.SKIP, "⏭️ " + message);
        }
        
        // Client Report - Just SKIP status
        ExtentTest client = clientTest.get();
        if (client != null) {
            client.skip("SKIP");
        }
    }

    // ================================================================
    // CATEGORY/TAG METHODS
    // ================================================================

    /**
     * Add category (Detailed report only)
     */
    public static void addCategory(String... categories) {
        ExtentTest detailed = detailedTest.get();
        if (detailed != null) {
            detailed.assignCategory(categories);
        }
        // Not added to client report to keep it clean
    }

    // ================================================================
    // CLEANUP METHODS
    // ================================================================

    /**
     * Remove thread-local tests
     */
    public static void removeTests() {
        detailedTest.remove();
        clientTest.remove();
    }

    /**
     * Flush both reports and send email
     */
    public static void flushReports() {
        if (detailedReport != null) {
            detailedReport.flush();
            System.out.println("📊 Detailed Report generated: " + detailedReportPath);
        }
        if (clientReport != null) {
            clientReport.flush();
            System.out.println("📊 Client Report generated: " + clientReportPath);
        }
        
        // Send email notification
        if (AppConstants.SEND_EMAIL_ENABLED) {
            sendReportEmail();
        }
    }

    /**
     * Send report email
     */
    private static void sendReportEmail() {
        try {
            String[] attachments = {detailedReportPath, clientReportPath};
            String body = buildEmailBody();
            
            EmailUtil.sendEmail(
                AppConstants.EMAIL_TO,
                AppConstants.EMAIL_SUBJECT + " - " + timestamp,
                body,
                attachments
            );
            System.out.println("📧 Test reports emailed to: " + AppConstants.EMAIL_TO);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Build email body with test summary
     */
    private static String buildEmailBody() {
        StringBuilder body = new StringBuilder();
        body.append("<html><body style='font-family: Arial, sans-serif;'>");
        body.append("<h2 style='color: #2c3e50;'>eGalvanic iOS Automation - Test Report</h2>");
        body.append("<p><strong>Execution Date:</strong> ").append(new SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new Date())).append("</p>");
        body.append("<p><strong>Device:</strong> ").append(AppConstants.DEVICE_NAME).append("</p>");
        body.append("<p><strong>Platform:</strong> iOS ").append(AppConstants.PLATFORM_VERSION).append("</p>");
        body.append("<hr>");
        body.append("<p>Please find the attached test reports:</p>");
        body.append("<ul>");
        body.append("<li><strong>Detailed Report</strong> - For QA Team (includes screenshots and logs)</li>");
        body.append("<li><strong>Client Report</strong> - Clean summary (Pass/Fail only)</li>");
        body.append("</ul>");
        body.append("<hr>");
        body.append("<p style='color: #7f8c8d; font-size: 12px;'>This is an automated email from eGalvanic iOS Automation Framework.</p>");
        body.append("</body></html>");
        return body.toString();
    }

    /**
     * Get detailed report path
     */
    public static String getDetailedReportPath() {
        return detailedReportPath;
    }

    /**
     * Get client report path
     */
    public static String getClientReportPath() {
        return clientReportPath;
    }
}
