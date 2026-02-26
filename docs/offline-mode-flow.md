# Offline Mode Module - Flow Documentation

## Overview
The Offline Mode module tests the app's ability to operate without network connectivity: go offline, perform operations, queue sync, and restore online. Total: **35 test cases** (TC_OFF_001–TC_OFF_035, TC_OFF_031 removed).

---

## Screen Flow

```
Online Dashboard
    ↓  (tap WiFi icon → "Go Offline")
OFFLINE MODE
  ├─ WiFi icon: "Wi-Fi Off" state
  ├─ RESTRICTED:
  │   ├─ Refresh button disabled
  │   ├─ Sites navigation disabled
  │   └─ Cannot change active site
  │
  ├─ AVAILABLE MODULES:
  │   ├─ Quick Count
  │   ├─ Locations (create buildings/floors/rooms)
  │   ├─ My Tasks
  │   ├─ Issues (create/edit)
  │   └─ Settings (Sync & Network section)
  │
  ├─ OFFLINE OPERATIONS (queued for sync):
  │   ├─ Create Building
  │   ├─ Create Asset
  │   ├─ Create Task
  │   ├─ Create Issue
  │   └─ Capture IR Photos
  │
  ├─ SYNC INDICATORS:
  │   ├─ WiFi icon badge (pending count digit)
  │   ├─ Sites tile badge
  │   └─ Sync Queue Analyzer screen
  │       ├─ Pending tab (queued operations with type + timestamp)
  │       └─ History tab (completed syncs)
  │
  └─ SETTINGS:
      ├─ Sync & Network section
      ├─ Network Mode toggle
      ├─ Account section
      └─ Diagnostics (Photo Storage)

    ↓  (tap WiFi icon → "Go Online" OR Settings toggle)
ONLINE MODE
  ├─ Automatic sync of all pending operations
  ├─ WiFi icon returns to normal
  ├─ Refresh re-enabled
  ├─ Sites navigation re-enabled
  └─ Sync badges cleared
```

---

## Test Cases

### Go Offline (TC_OFF_001–003) — 3 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_001 | Go Offline Button | WiFi popup shows "Go Offline" option |
| TC_OFF_002 | Switch to Offline | Tapping Go Offline switches mode, "Go Online" appears |
| TC_OFF_003 | WiFi Offline Indicator | WiFi icon shows offline visual state |

### Offline Restrictions (TC_OFF_004–006) — 3 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_004 | Refresh Disabled | Refresh button not functional |
| TC_OFF_005 | Sites Navigation Disabled | Can't navigate between sites |
| TC_OFF_006 | Cannot Change Site | Site selection blocked |

### Offline Operations Available (TC_OFF_007–010) — 4 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_007 | Quick Count Available | Quick Count module accessible |
| TC_OFF_008 | Locations Available | Locations module accessible |
| TC_OFF_009 | My Tasks Available | Tasks module accessible |
| TC_OFF_010 | Issues Available | Issues module accessible |

### Offline Creation (TC_OFF_011–015) — 5 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_011 | Create Building Offline | Building creation works |
| TC_OFF_012 | Create Asset Offline | Asset creation works |
| TC_OFF_013 | Create Task Offline | Task creation works |
| TC_OFF_014 | Create Issue Offline | Issue creation works |
| TC_OFF_015 | IR Photos Offline | IR photo capture (partial support) |

### Sync Queue Indicators (TC_OFF_016–017) — 2 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_016 | Pending Badge on WiFi | Badge shows pending operations count |
| TC_OFF_017 | Pending Badge on Sites | Badge visible on dashboard |

### Offline Settings (TC_OFF_018–019) — 2 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_018 | Sync & Network Section | Settings UI for offline management |
| TC_OFF_019 | Network Mode Toggle | Toggle switch for online/offline |

### Sync Queue Analyzer (TC_OFF_020–025) — 6 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_020 | Pending Count | Shows count of queued operations |
| TC_OFF_021 | Screen Layout | UI structure verification |
| TC_OFF_022 | Pending Tab | Displays pending items |
| TC_OFF_023 | History Tab | Displays completed syncs |
| TC_OFF_024 | Operation Type | Labels (Create/Update/Delete) |
| TC_OFF_025 | Queue Timestamp | Timestamp per queued operation |

### Go Online (TC_OFF_026–029) — 4 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_026 | Network Mode Online | Settings toggle switches to online |
| TC_OFF_027 | Pending Items Sync | Automatic sync of queued operations |
| TC_OFF_028 | WiFi Icon Normal | WiFi icon reset to normal state |
| TC_OFF_029 | Refresh/Sites Re-enabled | Navigation and refresh restored |

### Settings Sections (TC_OFF_030, TC_OFF_032) — 2 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_030 | Account Section | Account information display |
| TC_OFF_032 | Photo Storage Diagnostics | Diagnostics information |

### Offline Persistence (TC_OFF_033–035) — 3 tests

| ID | Name | Verifies |
|---|---|---|
| TC_OFF_033 | Active Job Persists | Active job survives offline mode |
| TC_OFF_034 | Site Stats Visible | Stats accessible offline |
| TC_OFF_035 | Multiple Operations Queue | Multiple operations queue in order |

---

## Critical Patterns

### Offline Detection (isDefinitelyOffline)
WiFi icon has 3 states that indicate offline:
1. Clean "Wi-Fi" label → **Online**
2. "Wi-Fi Off" label → **Offline**
3. Digit badge (sync count) → **Offline** (pending sync)

The `isDefinitelyOffline()` helper handles all 3 states reliably.

### Test Lifecycle
- `@BeforeClass`: `DriverManager.setNoReset(true)`
- `@AfterClass`: `DriverManager.resetNoResetOverride()`
- `loginAndSelectSite()` only in TC_OFF_001 (first test)
- All other tests rely on noReset=true

## Files
- **Test**: `src/test/java/com/egalvanic/tests/OfflineTest.java` (~3,434 lines, 35 tests)
- **Page**: Uses `SiteSelectionPage.java` for WiFi/online/offline methods
- **TestNG**: `src/test/resources/parallel/testng-offline.xml`
