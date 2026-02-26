# Authentication Module - Flow Documentation

## Overview
The Authentication module handles user login flow: Welcome screen (company code) → Login screen (email/password) → Dashboard. Total: **38 test cases** (TC01–TC38).

---

## Screen Flow

```
Welcome Screen (WelcomePage.java)
  ├─ Company Code field (placeholder: "e.g. acme.egalvanic")
  ├─ Continue button (disabled until code entered)
  └─ Error message (invalid code)
        │
        ↓  (valid company code + Continue)
Login Screen (LoginPage.java)
  ├─ Email field
  ├─ Password field (masked, secure)
  ├─ Show Password toggle
  ├─ Sign In button (disabled until both fields filled)
  └─ Save Password popup (auto-handled)
        │
        ↓  (valid credentials + Sign In)
Schedule Screen → "View Sites" → Site Selection → Dashboard
```

---

## Test Cases

### Section 1: Company Code Validation (TC01–TC15) — 15 tests

| ID | Name | Verifies |
|---|---|---|
| TC01 | Welcome Screen UI Loads | Company code field + Continue button visible |
| TC02 | Placeholder Text | Placeholder shown in company code field |
| TC03 | Continue Button Initial State | Disabled when field empty (CLIENT REQUIREMENT: PASS) |
| TC04 | Field Accepts Input | Text input accepted |
| TC05 | Continue Enables With Input | Button enables after entering code |
| TC06 | Invalid Code Shows Error | Error message on invalid code |
| TC07 | Valid Code Navigates to Login | Navigates to Login screen |
| TC08 | Case Insensitive | Uppercase code works |
| TC09 | Field Clears | Field can be cleared |
| TC10 | Button Disables When Cleared | Disables on clear (CLIENT REQUIREMENT) |
| TC11 | Special Characters | Accepts "test.company_code-123" |
| TC12 | Max Length | Handles 100-character input |
| TC13 | Spaces in Code | Accepts spaces |
| TC14 | Empty Submission | Button stays disabled (CLIENT REQUIREMENT) |
| TC15 | Field Focus | Focus behavior works |

### Section 2: Login (TC16–TC33) — 18 tests

| ID | Name | Verifies |
|---|---|---|
| TC16 | Login Screen UI | Email, password, Sign In visible |
| TC17 | Sign In Initial State | Disabled when empty (CLIENT REQUIREMENT) |
| TC18 | Email Accepts Input | Text input accepted |
| TC19 | Password Accepts Input | Text input accepted |
| TC20 | Password Masked | Input hidden/masked |
| TC21 | Show Password Toggle | Toggle visibility |
| TC22 | Sign In Enables | Enables with both fields filled |
| TC23 | Invalid Email Format | Handles invalid email |
| TC24 | Invalid Credentials Error | Error on wrong credentials |
| TC25 | Valid Login to Dashboard | Successful login navigates to dashboard |
| TC26 | Email Clears | Field clearable |
| TC27 | Password Clears | Field clearable |
| TC28 | Sign In Disables When Cleared | Disables on clear (CLIENT REQUIREMENT) |
| TC29 | Email Only Insufficient | Sign In stays disabled |
| TC30 | Password Only Insufficient | Sign In stays disabled |
| TC31 | Long Email | Accepts 50+ characters |
| TC32 | Long Password | Accepts 100 characters |
| TC33 | Special Characters in Password | Accepts @!#$%^&*() |

### Section 3: Session Management (TC34–TC38) — 5 tests

| ID | Name | Verifies |
|---|---|---|
| TC34 | Session Persists | Session stays active after login |
| TC35 | Dashboard Accessible | Sites button, refresh, WiFi, site list visible |
| TC36 | Multiple Actions | Session stable through multiple navigations |
| TC37 | Session Security | Secure session with dashboard elements |
| TC38 | User Data Displayed | User-specific data shown correctly |

---

## Page Objects

### WelcomePage.java
| Element | Locator |
|---|---|
| companyCodeField | `XCUIElementTypeTextField` |
| continueButton | accessibility="Continue" |
| welcomeText | label CONTAINS 'Welcome' |
| errorMessage | label CONTAINS 'not found' |

**Key Methods**: `submitCompanyCode()`, `isContinueButtonEnabled()`, `isErrorMessageDisplayed()`

### LoginPage.java
| Element | Locator |
|---|---|
| emailField | `XCUIElementTypeTextField AND visible == 1` |
| passwordField | `XCUIElementTypeSecureTextField` |
| signInButton | accessibility="Sign In" |
| showPasswordIcon | accessibility="Show Password" |

**Key Methods**: `login()`, `loginTurbo()`, `isSignInButtonEnabled()`, `waitForNoSavePasswordPopup()`

---

## Client Requirements
- **Disabled buttons on empty fields = PASS** (not a failure)
- Save Password popup handled automatically (10 dismissal methods)
- Schedule screen handled after login (Jan 2026 addition)

## Files
- **Test**: `src/test/java/com/egalvanic/tests/AuthenticationTest.java` (771 lines)
- **Pages**: `WelcomePage.java` (163 lines), `LoginPage.java` (623 lines)
- **TestNG**: `src/test/resources/parallel/testng-auth.xml`
