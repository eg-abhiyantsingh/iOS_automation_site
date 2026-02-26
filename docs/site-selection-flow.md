# Site Selection Module - Flow Documentation

## Overview
The Site Selection module covers site search/selection, dashboard navigation, online/offline toggle, and sync functionality. Total: **56 test cases** (TC_SS_001–TC_SS_056).

---

## Screen Flow

```
Login Complete
    ↓
Site Selection Screen
  ├─ Search Bar ("Search" placeholder)
  ├─ Create New Site button (or + icon)
  ├─ Site List (name, address, info icon, chevron per entry)
  └─ Cancel button (clears search)
        │
        ↓  (select a site)
Loading Screen (progress indicator)
        │
        ↓  (site loaded)
Dashboard
  ├─ Header: Broadcast icon | WiFi button | Sites button (building.2)
  ├─ Cards: Assets count | Connections count | No Active Job card
  ├─ Quick Actions: My Tasks | Issues | Locations | Quick Count
  └─ WiFi Menu:
      ├─ Go Offline → Offline Mode
      ├─ Go Online → Online Mode
      └─ Sync N records (when pending)
```

---

## Test Cases

### 1. Select Site Screen (TC_SS_001–006) — 6 tests
UI elements, cancel, site list, entry format, info icon, chevron

### 2. Search Sites (TC_SS_007–011) — 5 tests
Placeholder, filter, case-insensitive, no results, clear search

### 3. Select Site (TC_SS_012–016) — 5 tests
Loading state, progress indicator, dashboard navigation, asset/connection counts

### 4. Dashboard Sites Button (TC_SS_017–018) — 2 tests
Sites button display, opens Select Site

### 5. Online/Offline Mode (TC_SS_019–026) — 8 tests
Go Offline option, switch to offline, WiFi indicator, Sites disabled, Refresh disabled, disabled tap notification, Go Online option, switch to online

### 6. Offline Sync (TC_SS_027–034) — 8 tests
Changes offline, pending badge on WiFi, Sync N records option, Sites disabled during sync, Sites badge, sync initiation, Sites re-enabled, badge cleared

### 7. Performance (TC_SS_038–041) — 4 tests
Site list load speed, large site (1739+ assets) load, small site load, search performance

### 8. Dashboard Badges (TC_SS_043–045) — 3 tests
My Tasks badge, Issues badge, badge update on site change

### 9. Edge Cases (TC_SS_046–050) — 5 tests
Single site access, same site reload, long name, long address, no address

### 10. Dashboard Header (TC_SS_051–052) — 2 tests
Broadcast icon, WiFi connection status

### 11. Job Selection (TC_SS_053–054) — 2 tests
No Active Job card, tap navigates to job selection

### 12. Advanced Sync (TC_SS_055–056) — 2 tests
Multiple pending records, partial sync failure

---

## WiFi Button States
| State | Accessibility | Visual |
|---|---|---|
| Online | `Wi-Fi` | Normal WiFi icon |
| Offline | `Wi-Fi Off` | WiFi with X |
| Sync Pending | `\d+` (digit) | WiFi with count badge |

---

## Page Object: SiteSelectionPage.java (2,459 lines)

**Key Methods**:
- `searchSite()`, `clearSearch()`, `selectFirstSiteFast()`, `turboSelectSite()`
- `goOffline()`, `goOnline()`, `clickWifiButton()`, `syncPendingRecords()`
- `isNoActiveJobCardDisplayed()`, `clickNoActiveJobCard()`
- `isDashboardDisplayed()`, `waitForDashboardReady()`
- `clickSitesButton()`, `clickRefreshButton()`, `clickLocations()`

## Files
- **Test**: `src/test/java/com/egalvanic/tests/SiteSelectionTest.java` (1,379 lines)
- **Page**: `SiteSelectionPage.java` (2,459 lines)
- **TestNG**: `src/test/resources/parallel/testng-site.xml`
