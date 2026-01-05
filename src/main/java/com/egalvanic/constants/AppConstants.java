package com.egalvanic.constants;

/**
 * Application Constants
 * Centralized configuration for the entire test framework
 */
public class AppConstants {

    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    // ============================================
    // APPIUM SERVER CONFIGURATION
    // ============================================
    public static final String APPIUM_SERVER = getEnv("APPIUM_SERVER", "http://127.0.0.1:4723");

    // ============================================
    // iOS DEVICE CONFIGURATION
    // ============================================
    public static final String PLATFORM_NAME = "iOS";
    public static final String AUTOMATION_NAME = "XCUITest";
    public static final String DEVICE_NAME = getEnv("DEVICE_NAME", "iPhone 17 Pro");
    public static final String PLATFORM_VERSION = getEnv("PLATFORM_VERSION", "26.2");
    public static final String UDID = getEnv("SIMULATOR_UDID", "B745C0EF-01AA-4355-8B08-86812A8CBBAA");
    public static final String APP_PATH = getEnv("APP_PATH", "/Users/abhiyantsingh/Downloads/Z Platform-QA.app");

    // ============================================
    // TEST DATA - AUTHENTICATION
    // ============================================
    public static final String VALID_COMPANY_CODE = "acme.egalvanic";
    public static final String INVALID_COMPANY_CODE = "xyz123invalid";
    public static final String VALID_EMAIL = "rahul+acme@egalvanic.com";
    public static final String VALID_PASSWORD = "RP@egalvanic123";
    public static final String INVALID_EMAIL = "invalidemail@test.com";
    public static final String INVALID_PASSWORD = "wrongpassword123";

    // ============================================
    // TEST DATA - SITE SELECTION
    // ============================================
    public static final String SEARCH_SITE_TEXT = "test";
    public static final String NON_EXISTENT_SITE = "XYZNONEXISTENT";
    public static final String SITE_WITH_MANY_ASSETS = "test site";
    public static final String SITE_WITH_FEW_ASSETS = "Test QA 16";

    // ============================================
    // TIMEOUTS (in seconds)
    // ============================================
    public static final int IMPLICIT_WAIT = 5; // Fast timeout
    public static final int EXPLICIT_WAIT = 10; // Fast explicit wait
    public static final int PAGE_LOAD_TIMEOUT = 45; // Increased for page loads
    public static final int AJAX_TIMEOUT = 20; // Increased for AJAX calls
    public static final int SITE_LOAD_TIMEOUT = 90; // Increased for site loading
    public static final int WDA_LAUNCH_TIMEOUT = 240; // 4 minutes for WDA in CI
    public static final int WDA_CONNECTION_TIMEOUT = 180; // 3 minutes for WDA connection

    // ============================================
    // REPORT PATHS
    // ============================================
    public static final String DETAILED_REPORT_PATH = "reports/detailed/";
    public static final String CLIENT_REPORT_PATH = "reports/client/";
    public static final String SCREENSHOT_PATH = "screenshots/";

    // ============================================
    // MODULE NAMES (for Reports)
    // ============================================
    public static final String MODULE_AUTHENTICATION = "Authentication";
    public static final String MODULE_SITE_SELECTION = "Site Selection";
    public static final String MODULE_ASSET = "Asset Management";
    public static final String MODULE_LOCATIONS = "Locations";
    public static final String MODULE_SLDS = "SLDs";
    public static final String MODULE_CONNECTIONS = "Connections";
    public static final String MODULE_ISSUES = "Issues";
    public static final String MODULE_JOBS = "Jobs/Site Visits";
    public static final String MODULE_OFFLINE = "Offline Mode";

    // ============================================
    // FEATURE NAMES - AUTHENTICATION
    // ============================================
    public static final String FEATURE_COMPANY_CODE = "Company Code Validation";
    public static final String FEATURE_LOGIN = "Login";
    public static final String FEATURE_SESSION = "Session Management";

    // ============================================
    // FEATURE NAMES - SITE SELECTION
    // ============================================
    public static final String FEATURE_SELECT_SITE_SCREEN = "Select Site Screen";
    public static final String FEATURE_SEARCH_SITES = "Search Sites";
    public static final String FEATURE_SELECT_SITE = "Select Site";
    public static final String FEATURE_DASHBOARD_SITES_BUTTON = "Dashboard Sites Button";
    public static final String FEATURE_ONLINE_OFFLINE = "Online Offline";
    public static final String FEATURE_OFFLINE_SYNC = "Offline Sync";
    public static final String FEATURE_NO_INTERNET = "No Internet";
    public static final String FEATURE_PERFORMANCE = "Performance";
    public static final String FEATURE_DASHBOARD_BADGES = "Dashboard Badges";
    public static final String FEATURE_EDGE_CASES = "Edge Cases";
    public static final String FEATURE_DASHBOARD_HEADER = "Dashboard Header";
    public static final String FEATURE_JOB_SELECTION = "Job Selection";
    public static final String FEATURE_ACCESSIBILITY = "Accessibility";

    // ============================================
    // EMAIL CONFIGURATION
    // ============================================
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final String EMAIL_FROM = getEnv("EMAIL_FROM", "abhiyantsinghsuas18@gmail.com");
    public static final String EMAIL_PASSWORD = getEnv("EMAIL_PASSWORD", "ccddnnqsjigrhzcz");
    public static final String EMAIL_TO = "abhiyantsinghsuas18@gmail.com";
    public static final String EMAIL_SUBJECT = "eGalvanic iOS Automation - Test Report";
    public static final boolean SEND_EMAIL_ENABLED = false; // Set to true to send email reports

    // ============================================
    // HELPER METHOD
    // ============================================
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
