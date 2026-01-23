# eGalvanic iOS Automation Framework

Professional iOS automation framework for eGalvanic App using Appium, TestNG, Page Object Model, and Dual Extent Reports.

## 📋 Project Overview

| Module | Test Cases | Status |
|--------|------------|--------|
| **Authentication** | **38** | ✅ Complete |
| **Site Selection** | **58** | ✅ Complete |
| Asset Management | 728 | 🔄 Planned |
| Locations | TBD | 🔄 Planned |

**Total Automated Test Cases: 96**

## 🎯 Key Features

| Feature | Description |
|---------|-------------|
| **Page Object Model** | Clean separation of page elements and test logic |
| **Page Factory** | Annotation-based element initialization |
| **AjaxElementLocatorFactory** | Lazy loading with built-in waits |
| **Dual Extent Reports** | Detailed (QA) + Client (Presentation) reports |
| **Email Notification** | Auto-send reports to `abhiyant.singh@sculptsoft.com` |
| **Thread-safe Driver** | ThreadLocal for parallel execution support |
| **CI/CD Ready** | GitHub Actions workflow included |

## 📊 Dual Report System

### Detailed Report (For QA Team)
- Full screenshots on every step
- Detailed logs and step descriptions
- Exception stack traces
- System information
- **Location:** `reports/detailed/`

### Client Report (For Presentations)
- **Module > Feature > Test Name > Pass/Fail ONLY**
- NO screenshots
- NO logs or technical details
- NO tags
- Clean, professional summary
- **Location:** `reports/client/`

## 📁 Project Structure

```
iOS_automation/
├── pom.xml                                     # Maven with dependencies
├── testng.xml                                  # TestNG configuration
├── config/
│   └── config.properties                       # External configuration
├── src/main/java/com/egalvanic/
│   ├── base/
│   │   └── BasePage.java                       # PageFactory + AjaxElementLocatorFactory
│   ├── pages/
│   │   ├── WelcomePage.java                    # Company code screen
│   │   ├── LoginPage.java                      # Login screen
│   │   └── SiteSelectionPage.java              # Site Selection POM
│   ├── utils/
│   │   ├── DriverManager.java                  # Thread-safe driver management
│   │   ├── ExtentReportManager.java            # Dual reports + Email
│   │   ├── ScreenshotUtil.java                 # Screenshot capture
│   │   └── EmailUtil.java                      # Email notifications
│   └── constants/
│       └── AppConstants.java                   # All constants
├── src/test/java/com/egalvanic/
│   ├── base/
│   │   └── BaseTest.java                       # Test setup/teardown
│   └── tests/
│       └── SiteSelectionTest.java              # 58 Site Selection tests
├── reports/
│   ├── detailed/                               # QA Report
│   └── client/                                 # Client Report
└── screenshots/
```

## 🧪 Site Selection Test Cases (58 Total)

| Feature | Test Cases | IDs |
|---------|------------|-----|
| Select Site Screen | 6 | TC_SS_001 - TC_SS_006 |
| Search Sites | 5 | TC_SS_007 - TC_SS_011 |
| Select Site | 5 | TC_SS_012 - TC_SS_016 |
| Dashboard Sites Button | 2 | TC_SS_017 - TC_SS_018 |
| Online Offline | 8 | TC_SS_019 - TC_SS_026 |
| Offline Sync | 10 | TC_SS_027 - TC_SS_034, TC_SS_055 - TC_SS_056 |
| Performance | 4 | TC_SS_038 - TC_SS_041 |
| Dashboard Badges | 3 | TC_SS_043 - TC_SS_045 |
| Edge Cases | 5 | TC_SS_046 - TC_SS_050 |
| Dashboard Header | 2 | TC_SS_051 - TC_SS_052 |
| Job Selection | 2 | TC_SS_053 - TC_SS_054 |

**Note:** Test cases TC_SS_035-037 (No Internet), TC_SS_042 (Scrolling), TC_SS_057-058 (Accessibility) require manual testing.

## 🚀 How to Run

### Prerequisites

```bash
# 1. Start Appium server
appium

# 2. Boot iOS Simulator
xcrun simctl boot "iPhone 17 Pro"
open -a Simulator

# 3. Verify Java 17
java --version
```

### Run Tests

```bash
# Run all Site Selection tests
mvn clean test

# Run specific test
mvn clean test -Dtest=SiteSelectionTest#TC_SS_001_verifySelectSiteScreenUIElements
```

## 📧 Email Configuration

Reports are automatically sent to `abhiyant.singh@sculptsoft.com` after test execution.

### To configure email:

1. Set environment variables:
```bash
export EMAIL_FROM="your-gmail@gmail.com"
export EMAIL_PASSWORD="your-app-password"
```

2. Or update `AppConstants.java`:
```java
public static final String EMAIL_FROM = "your-gmail@gmail.com";
public static final String EMAIL_PASSWORD = "your-app-password";
```

### Gmail App Password Setup:
1. Go to Google Account > Security
2. Enable 2-Step Verification
3. Go to App passwords
4. Generate a new app password for "Mail"
5. Use this password in `EMAIL_PASSWORD`

## 📊 Test Report Format

### Client Report Structure:
```
Site Selection (Module)
├── Select Site Screen (Feature)
│   ├── TC_SS_001 - Verify Select Site screen UI elements → PASS
│   ├── TC_SS_002 - Verify Cancel button returns to dashboard → PASS
│   └── ...
├── Search Sites (Feature)
│   ├── TC_SS_007 - Verify search bar placeholder text → PASS
│   └── ...
└── ...
```

## 🔧 Test Data

| Data | Value |
|------|-------|
| Company Code | acme.egalvanic |
| Email | rahul+acme@egalvanic.com |
| Password |  |
| Search Text | test |

## 📱 Locators Reference

| Element | Locator Strategy | Value |
|---------|------------------|-------|
| Sites Button | accessibilityId | `building.2` |
| WiFi (Online) | accessibilityId | `Wi-Fi` |
| WiFi (Offline) | accessibilityId | `Wi-Fi Off` |
| Locations | accessibilityId | `Locations` |
| Search Bar | iOSNsPredicate | `value == 'Search sites...'` |
| Cancel | accessibilityId | `Cancel` |
| Save | accessibilityId | `Save` |
| Add (+) | accessibilityId | `plus` |

## 🚨 Known Limitations

| Test Case | Limitation |
|-----------|------------|
| TC_SS_005 | Color verification limited |
| TC_SS_021 | Color verification limited |
| TC_SS_028 | Badge color verification limited |
| TC_SS_035-037 | Cannot toggle network on iOS |
| TC_SS_042 | Manual smoothness verification |
| TC_SS_057-058 | Requires VoiceOver |

## 📞 Contact

**QA Lead:** Rahul  
**Email:** abhiyant.singh@sculptsoft.com  
**Project:** eGalvanic iOS Automation

---

© 2025 eGalvanic - All Rights Reserved

you didnt added save password handling