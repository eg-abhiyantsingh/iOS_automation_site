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
    // Local default = iPhone 17 Pro Max (per user pref: user runs Pro themselves
    // on the host MacBook screen; automation must use Pro Max to avoid contention).
    // CI overrides via DEVICE_NAME / SIMULATOR_UDID env vars set in the workflow.
    public static final String DEVICE_NAME = getEnv("DEVICE_NAME", "iPhone 17 Pro Max");
    public static final String PLATFORM_VERSION = getEnv("PLATFORM_VERSION", "26.2");
    public static final String UDID = getEnv("SIMULATOR_UDID", "E042B830-41DF-4690-AAB2-11FDE47916DD");
    public static final String APP_PATH = getEnv("APP_PATH", "/Users/abhiyantsingh/Downloads/Z Platform-QA.app");
    // Bundle ID is auto-detected from the app's Info.plist at APP_PATH.
    // Can be overridden via -DAPP_BUNDLE_ID or APP_BUNDLE_ID env var.
    public static final String APP_BUNDLE_ID = detectBundleId();

    // ============================================
    // TEST DATA - AUTHENTICATION
    // ============================================
    public static final String VALID_COMPANY_CODE = "acme.egalvanic";
    public static final String INVALID_COMPANY_CODE = "xyz123invalid";
    public static final String VALID_EMAIL = "abhiyant.singh+admin@egalvanic.com";
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
    public static final int IMPLICIT_WAIT = getEnvInt("IMPLICIT_WAIT", 5); // Fast timeout
    public static final int EXPLICIT_WAIT = 10; // Fast explicit wait

    // ============================================
    // XCUITEST SNAPSHOT / ANIMATION SPEED SETTINGS
    // (env/-D overridable so CI can tune or roll back without a code change)
    // ============================================
    // Seconds WDA waits for animations to settle before EVERY accessibility
    // snapshot (driver default: 2). 0 = act immediately; the suite already
    // polls/retries, so the per-query 0-2s tax buys nothing.
    public static final int ANIMATION_COOLOFF_TIMEOUT = getEnvInt("ANIMATION_COOLOFF_TIMEOUT", 0);
    // Cap for resolving one accessibility snapshot (driver default: 15s).
    public static final int CUSTOM_SNAPSHOT_TIMEOUT = getEnvInt("CUSTOM_SNAPSHOT_TIMEOUT", 10);
    // Max element-tree depth per snapshot (driver default: 50). 40 still covers
    // this app's deepest SwiftUI screens but trims predicate-query/page-source
    // cost on complex screens (Connections, SiteVisit). Set 50 to restore default.
    public static final int SNAPSHOT_MAX_DEPTH = getEnvInt("SNAPSHOT_MAX_DEPTH", 40);
    public static final int PAGE_LOAD_TIMEOUT = 45; // Increased for page loads
    public static final int AJAX_TIMEOUT = 10; // Page Factory element lookup timeout
    public static final int SITE_LOAD_TIMEOUT = 90; // Increased for site loading
    public static final int WDA_LAUNCH_TIMEOUT = 240; // 4 minutes for WDA in CI
    public static final int WDA_CONNECTION_TIMEOUT = 180; // 3 minutes for WDA connection

    // ============================================
    // SESSION LIFECYCLE / RESILIENCE
    // (env/-D overridable so CI can tune or roll back without a code change)
    // ============================================
    // Keep the Appium session alive across PASSING tests. Quit+recreate costs
    // 11-30s/test (plus a full UI login in login-dependent suites); the
    // @BeforeMethod soft restart (terminateApp+activateApp) already isolates
    // app state. Failed tests and dead sessions still quit. Set
    // KEEP_SESSION_ALIVE=false to restore quit-every-test.
    public static final boolean KEEP_SESSION_ALIVE = Boolean.parseBoolean(
        getEnv("KEEP_SESSION_ALIVE", "true"));
    // Dead-session circuit breaker: after N consecutive tests ending on a dead
    // Appium session, skip the remainder of the run instead of burning the
    // per-test timeout on every survivor (failed-suites/ rerun triages them).
    public static final boolean DEAD_SESSION_BREAKER = Boolean.parseBoolean(
        getEnv("DEAD_SESSION_BREAKER", "true"));
    public static final int DEAD_SESSION_BREAKER_N = getEnvInt("DEAD_SESSION_BREAKER_N", 5);
    // Asset class-change wall-clock budget (ms). The class-change path snapshots a
    // bleed-through Edit-screen DOM several times; on a busy CI runner each snapshot
    // can take up to CUSTOM_SNAPSHOT_TIMEOUT (10s), so a legitimate change runs
    // 60-120s. The budget must let a slow-but-working change FINISH yet still cap
    // before the ~360s WDA-death zone. (60s was too tight — fast-failed 63 passing
    // Assets P3 changes in run 27557701204; raised to 180s.) Env-overridable.
    public static final int CLASS_CHANGE_BUDGET_SEC = getEnvInt("CLASS_CHANGE_BUDGET_SEC", 180);
    // Per-step picker-button enumeration budget (ms). A single bleed-through
    // snapshot can take ~10s, so 8s risked bailing mid-enumeration; 20s gives a
    // couple of snapshots' headroom while still bounding a wedged query.
    public static final int PICKER_ENUM_BUDGET_SEC = getEnvInt("PICKER_ENUM_BUDGET_SEC", 20);
    // Prebuilt-WDA consumption: CI can build WebDriverAgent once per runner and
    // point Appium at the bundle, skipping the per-session xcodebuild (~60-90s).
    // Both stay unset by default so local runs keep the build-on-demand path.
    public static final boolean USE_PREBUILT_WDA = Boolean.parseBoolean(
        getEnv("USE_PREBUILT_WDA", "false"));
    public static final String WDA_DERIVED_DATA_PATH = getEnv("WDA_DERIVED_DATA_PATH", "");

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
    // FEATURE NAMES - JOBS / SITE VISITS
    // ============================================
    public static final String FEATURE_WORK_ORDERS_SCREEN = "Work Orders Screen";
    public static final String FEATURE_WORK_ORDER_ENTRY = "Work Order Entry";
    public static final String FEATURE_START_WORK_ORDER = "Start Work Order";
    public static final String FEATURE_WORK_ORDER_DETAILS = "Work Order Details";
    public static final String FEATURE_ACTIVATE_JOB = "Activate Job";
    public static final String FEATURE_SESSION_DETAILS = "Session Details";
    public static final String FEATURE_SESSION_ISSUES = "Session Issues";
    public static final String FEATURE_LINK_ISSUES = "Link Issues";
    public static final String FEATURE_START_NEW_JOB = "Start New Job";
    public static final String FEATURE_WORK_ORDER_PLANNING = "Work Order Planning";
    public static final String FEATURE_SHOW_ALL = "Show All";
    public static final String FEATURE_NEW_ISSUE_LINKED = "New Issue Linked";
    public static final String FEATURE_NEW_JOB_SCREEN = "New Job Screen";
    public static final String FEATURE_QUICK_QR_ACTION = "Quick QR Action";
    public static final String FEATURE_SESSION_LOCATIONS = "Session Locations";
    public static final String FEATURE_ASSETS_IN_ROOM = "Assets in Room";
    public static final String FEATURE_ADD_ASSETS = "Add Assets";
    public static final String FEATURE_NEW_ASSET_IR_PHOTOS = "New Asset - IR Photos";
    public static final String FEATURE_ASSET_CONTEXT_MENU = "Asset Context Menu";
    public static final String FEATURE_SESSION_BOTTOM_TABS = "Session Bottom Tabs";
    public static final String FEATURE_QUICK_COUNT = "Quick Count";
    public static final String FEATURE_OCPD = "OCPD";
    public static final String FEATURE_PHOTO_WALKTHROUGH = "Photo Walkthrough";
    public static final String FEATURE_LINK_EXISTING_ASSET = "Link Existing Asset";
    public static final String FEATURE_REMOVE_FROM_SESSION = "Remove from Session";
    public static final String FEATURE_JOB_CREATION_PHOTO_TYPE = "Job Creation - Photo Type";
    public static final String FEATURE_IR_PHOTOS_FLIR_IND = "IR Photos - FLIR-IND";
    public static final String FEATURE_IR_PHOTOS_FLUKE = "IR Photos - FLUKE";
    public static final String FEATURE_IR_PHOTOS_FOTRIC = "IR Photos - FOTRIC";
    public static final String FEATURE_IR_PHOTOS_FLIR_SEP = "IR Photos - FLIR-SEP";
    public static final String FEATURE_JOB_MANAGEMENT = "Job Management";
    public static final String FEATURE_JOBS_LIST = "Jobs List";
    public static final String FEATURE_IR_PHOTOS = "IR Photos";
    public static final String FEATURE_TASKS_TAB = "Tasks Tab";
    public static final String FEATURE_LINK_TASKS = "Link Tasks";
    public static final String FEATURE_CREATE_TASK = "Create Task";
    public static final String FEATURE_SIMPLE_TASK = "Simple Task";
    public static final String FEATURE_COMPLEX_TASK = "Complex Task";
    public static final String FEATURE_TASKS_GROUP_BY = "Tasks - Group By";
    public static final String FEATURE_ISSUES_TAB = "Issues Tab";
    public static final String FEATURE_FILES_TAB = "Files Tab";

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
    public static final String EMAIL_TO = "abhiyant.singh@egalvanic.com";

    public static final String EMAIL_SUBJECT = "eGalvanic iOS Automation - Test Report";

    // Controllable via -DSEND_EMAIL_ENABLED=true or SEND_EMAIL_ENABLED env var.
    // Default: false — prevents per-module emails in parallel CI jobs.
    // The workflow's send-email job handles the consolidated email instead.
    public static final boolean SEND_EMAIL_ENABLED = Boolean.parseBoolean(
        getEnv("SEND_EMAIL_ENABLED", "false"));
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

    // ============================================
    // FEATURE NAMES - OFFLINE MODE
    // ============================================
    public static final String FEATURE_GO_OFFLINE = "Go Offline";
    public static final String FEATURE_OFFLINE_RESTRICTIONS = "Offline Restrictions";
    public static final String FEATURE_OFFLINE_OPERATIONS = "Offline Operations";
    public static final String FEATURE_OFFLINE_CREATION = "Offline Creation";
    public static final String FEATURE_SYNC_QUEUE = "Sync Queue";
    public static final String FEATURE_OFFLINE_SETTINGS = "Offline Settings";
    public static final String FEATURE_SYNC_QUEUE_ANALYZER = "Sync Queue Analyzer";
    public static final String FEATURE_GO_ONLINE = "Go Online";

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

    private static int getEnvInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getEnv(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Non-numeric value for " + key + ", using default " + defaultValue);
            return defaultValue;
        }
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

