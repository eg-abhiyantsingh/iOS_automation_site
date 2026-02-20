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
    // Bundle ID is auto-detected from the app's Info.plist at APP_PATH.
    // Can be overridden via -DAPP_BUNDLE_ID or APP_BUNDLE_ID env var.
    public static final String APP_BUNDLE_ID = detectBundleId();

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
    public static final String MODULE_BUILDING = "Building Management";
    public static final String MODULE_SLDS = "SLDs";
    public static final String MODULE_CONNECTIONS = "Connections";
    public static final String MODULE_ISSUES = "Issues";
    public static final String MODULE_JOBS = "Jobs/Site Visits";
    public static final String MODULE_OFFLINE = "Offline Mode";
    public static final String MODULE_SMOKE_TEST = "Smoke Test";

    // ============================================
    // FEATURE NAMES - AUTHENTICATION
    // ============================================
    public static final String FEATURE_COMPANY_CODE = "Company Code Validation";
    public static final String FEATURE_LOGIN = "Login";
    public static final String FEATURE_SESSION = "Session Management";

    // ============================================
    // FEATURE NAMES - SMOKE TEST (S3 Drift Detection)
    // Jira Ticket: ZP-774
    // ============================================
    public static final String FEATURE_S3_DRIFT_DETECTION = "S3 Bucket Policy Drift Detection";

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
    // FEATURE NAMES - ASSET MANAGEMENT
    // ============================================
    public static final String FEATURE_ASSET_LIST = "Asset List";
    public static final String FEATURE_CREATE_ASSET = "Create Asset";
    public static final String FEATURE_EDIT_ASSET = "Edit Asset";
    public static final String FEATURE_ASSET_DETAILS = "Asset Details";
    public static final String FEATURE_ASSET_CLASS = "Asset Class Selection";
    public static final String FEATURE_ASSET_LOCATION = "Asset Location";
    public static final String FEATURE_ASSET_SUBTYPE = "Asset Subtype";
    public static final String FEATURE_ASSET_QR_CODE = "Asset QR Code";
    public static final String FEATURE_ASSET_VALIDATION = "Asset Validation";
    
    // Building Management Features
    public static final String FEATURE_NEW_BUILDING = "New Building";
    public static final String FEATURE_BUILDING_VALIDATION = "Building Validation";
    public static final String FEATURE_BUILDING_ACCESSIBILITY = "Building Accessibility";
    public static final String FEATURE_BUILDING_LIST = "Building List";
    public static final String FEATURE_EDIT_BUILDING = "Edit Building";
    public static final String FEATURE_BUILDING_CONTEXT_MENU = "Building Context Menu";
    public static final String FEATURE_DELETE_BUILDING = "Delete Building";
    
    // Floor Management Features
    public static final String MODULE_FLOOR = "Floor Management";
    public static final String FEATURE_NEW_FLOOR = "New Floor";
    public static final String FEATURE_FLOOR_VALIDATION = "Floor Validation";
    public static final String FEATURE_FLOOR_CREATION = "Floor Creation";
    public static final String FEATURE_FLOOR_LIST = "Floor List";
    public static final String FEATURE_FLOOR_CONTEXT_MENU = "Floor Context Menu";
    public static final String FEATURE_EDIT_FLOOR = "Edit Floor";
    public static final String FEATURE_DELETE_FLOOR = "Delete Floor";

    // Room Management Constants
    public static final String MODULE_ROOM = "Room Management";
    public static final String FEATURE_NEW_ROOM = "New Room";
    public static final String FEATURE_ROOM_VALIDATION = "Room Validation";
    public static final String FEATURE_ROOM_CREATION = "Room Creation";
    public static final String FEATURE_ROOM_LIST = "Room List";
    public static final String FEATURE_ROOM_CONTEXT_MENU = "Room Context Menu";
    public static final String FEATURE_EDIT_ROOM = "Edit Room";
    public static final String FEATURE_DELETE_ROOM = "Delete Room";
    public static final String FEATURE_ROOM_DETAIL = "Room Detail";
    public static final String FEATURE_NO_LOCATION = "No Location";
    public static final String FEATURE_ASSIGN_LOCATION = "Assign Location";

    // ============================================
    // EMAIL CONFIGURATION
    // ============================================
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final String EMAIL_FROM = getEnv("EMAIL_FROM", "abhiyant.singh@egalvanic.com");
   //public static final String EMAIL_PASSWORD = getEnv("EMAIL_PASSWORD", "ccddnnqsjigrhzcz");
    public static final String EMAIL_PASSWORD = getEnv("EMAIL_PASSWORD", "onmzhjxnacinjfun");
    
    //public static final String EMAIL_TO = "dharmesh.avaiya@egalvanic.com, abhiyantsinghsuas18@gmail.com";
    public static final String EMAIL_TO = "abhiyant.singh@egalvanic.com";
    //public static final String EMAIL_TO = "abhiyant.singh@egalvanic.com";
    //email to here i think send proper email and secreat & variable ->action key send repo link by mail.  
    public static final String EMAIL_SUBJECT = "eGalvanic iOS Automation - Test Report";

    public static final boolean SEND_EMAIL_ENABLED = false;
    //public static final boolean SEND_EMAIL_ENABLED = true;

    // ================================================================
    // APP RESET BEHAVIOR
    // ================================================================
    // Set to true if tests fail due to cached state (adds ~15-20s per test)
    // APP RESET CONFIGURATION
    // Default: Reinstall app for clean state (AuthenticationTest, SiteSelectionTest)
    // Override in specific tests using DriverManager.setNoReset(true) for Edit Asset tests
    public static final boolean FULL_RESET = false;  // true = always reinstall app
    public static final boolean NO_RESET = false;    // DEFAULT: false = clean install
    
 // Set to true to send email reports

    // ============================================

    // ============================================
    // FEATURE NAMES - CONNECTIONS
    // ============================================
    public static final String FEATURE_CONNECTIONS_LIST = "Connections List";
    public static final String FEATURE_MISSING_NODE = "Missing Node";
    public static final String FEATURE_SEARCH_CONNECTIONS = "Search Connections";
    public static final String FEATURE_ADD_CONNECTION = "Add Connection";
    public static final String FEATURE_NEW_CONNECTION = "New Connection";
    public static final String FEATURE_SOURCE_NODE = "Source Node";
    public static final String FEATURE_TARGET_NODE = "Target Node";
    public static final String FEATURE_CONNECTION_TYPE = "Connection Type";
    public static final String FEATURE_CREATE_CONNECTION = "Create Connection";
    public static final String FEATURE_SELECT_MULTIPLE = "Select Multiple";
    public static final String FEATURE_DELETE_MULTIPLE = "Delete Multiple";

    // ============================================
    // FEATURE NAMES - ISSUES
    // ============================================
    public static final String FEATURE_ISSUES_LIST = "Issues List";
    public static final String FEATURE_ISSUE_ENTRY = "Issue Entry";
    public static final String FEATURE_SEARCH_ISSUES = "Search Issues";
    public static final String FEATURE_SORT_ISSUES = "Sort Issues";
    public static final String FEATURE_NEW_ISSUE = "New Issue";
    public static final String FEATURE_ISSUE_CLASS = "Issue Class";
    public static final String FEATURE_ISSUE_TITLE = "Issue Title";
    public static final String FEATURE_ISSUE_PRIORITY = "Issue Priority";
    public static final String FEATURE_ASSET_SELECTION = "Asset Selection";
    public static final String FEATURE_CREATE_ISSUE = "Create Issue";
    public static final String FEATURE_ISSUE_DETAILS = "Issue Details";
    public static final String FEATURE_SUBCATEGORY = "Subcategory";
    public static final String FEATURE_DESCRIPTION = "Description";
    public static final String FEATURE_PROPOSED_RESOLUTION = "Proposed Resolution";
    public static final String FEATURE_ISSUE_PHOTOS = "Issue Photos";
    public static final String FEATURE_DELETE_ISSUE = "Delete Issue";
    public static final String FEATURE_SAVE_CHANGES = "Save Changes";
    public static final String FEATURE_DONE_BUTTON = "Done Button";
    public static final String FEATURE_CLOSE_BUTTON = "Close Button";
    public static final String FEATURE_ISSUE_CLASS_SUBCATEGORIES = "Issue Class Subcategories";
    public static final String FEATURE_OFFLINE_MODE = "Offline Mode";
    public static final String FEATURE_NFPA70B_SUBCATEGORY = "NFPA 70B Subcategory";
    public static final String FEATURE_ISSUE_COMPLETION = "Issue Completion";
    public static final String FEATURE_CHANGE_ISSUE_CLASS = "Change Issue Class";
    public static final String FEATURE_CLEAR_SUBCATEGORY = "Clear Subcategory";
    public static final String FEATURE_OSHA_SUBCATEGORY = "OSHA Subcategory";
    public static final String FEATURE_REPAIR_NEEDED = "Repair Needed";
    public static final String FEATURE_THERMAL_ANOMALY = "Thermal Anomaly";
    public static final String FEATURE_SEVERITY = "Severity";
    public static final String FEATURE_REQUIRED_FIELDS_TOGGLE = "Required Fields Toggle";
    public static final String FEATURE_ULTRASONIC_ANOMALY = "Ultrasonic Anomaly";
    public static final String FEATURE_STATUS_FILTERS = "Status Filters";
    public static final String FEATURE_IN_PROGRESS_STATUS = "In Progress Status";
    public static final String FEATURE_ISSUE_ICONS = "Issue Icons";
    public static final String FEATURE_PRIORITY_BADGES = "Priority Badges";
    public static final String FEATURE_STATUS_WORKFLOW = "Status Workflow";
    public static final String FEATURE_SWIPE_ACTIONS = "Swipe Actions";
    public static final String FEATURE_FILTER_COUNTS = "Filter Counts";
    public static final String FEATURE_WITH_PHOTOS_FILTER = "With Photos Filter";
    public static final String FEATURE_MY_SESSION_FILTER = "My Session Filter";
    public static final String FEATURE_FILTER_TABS = "Filter Tabs";

    // HELPER METHODS
    // ============================================

    /**
     * Auto-detect bundle ID: check env/system property first, then read from app's Info.plist.
     */
    private static String detectBundleId() {
        // 1. Check explicit override via -D flag or env var
        String explicit = getEnv("APP_BUNDLE_ID", null);
        if (explicit != null) {
            System.out.println("📦 Bundle ID (from env/property): " + explicit);
            return explicit;
        }

        // 2. Auto-detect from the app's Info.plist
        try {
            String appPath = APP_PATH;
            String plistPath = appPath + "/Info.plist";
            java.io.File plist = new java.io.File(plistPath);
            if (plist.exists()) {
                ProcessBuilder pb = new ProcessBuilder(
                    "/usr/libexec/PlistBuddy", "-c", "Print :CFBundleIdentifier", plistPath);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String bundleId = new String(p.getInputStream().readAllBytes()).trim();
                int exit = p.waitFor();
                if (exit == 0 && !bundleId.isEmpty()) {
                    System.out.println("📦 Bundle ID (auto-detected from app): " + bundleId);
                    return bundleId;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not auto-detect bundle ID: " + e.getMessage());
        }

        // 3. Fallback
        String fallback = "com.egalvanic.zplatform-QA";
        System.out.println("📦 Bundle ID (fallback): " + fallback);
        return fallback;
    }

    private static String getEnv(String key, String defaultValue) {
        // First check system properties (set via Maven -D flags)
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Then check environment variables
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Fall back to default
        return defaultValue;
    }
}

