# Locations Module - Flow Documentation

## Overview
The Locations module manages the Building → Floor → Room hierarchy, plus asset-location assignment and "No Location" unassigned assets. Total: **85 test cases**.

---

## Screen Flow

```
Dashboard
    ↓  (tap Locations button)
Locations Screen (Building List)
  ├─ + button (Add Building)
  ├─ Building entries (name, floor count)
  │   ├─ Tap → Expand (show floors)
  │   └─ Long-press → Context Menu (Edit / Delete)
  │
  ├─ Expanded Building → Floor List
  │   ├─ + button (Add Floor)
  │   ├─ Floor entries (name, room count)
  │   │   ├─ Tap → Expand (show rooms)
  │   │   └─ Long-press → Context Menu (Edit / Delete)
  │   │
  │   └─ Expanded Floor → Room List
  │       ├─ + button (Add Room)
  │       ├─ Room entries (name, asset count)
  │       │   ├─ Tap → Room Detail Screen
  │       │   └─ Long-press → Context Menu (Edit / Delete)
  │       │
  │       └─ Room Detail Screen
  │           ├─ Breadcrumb navigation
  │           ├─ Assets list (or "No Assets" empty state)
  │           ├─ Tap asset → Asset Details
  │           └─ Done button → back to Locations
  │
  └─ "No Location" section (bottom)
      ├─ Shows count of unassigned assets
      ├─ Tap → Unassigned Assets list
      │   ├─ Search functionality
      │   ├─ Tap asset → Asset Details
      │   │   └─ Select Location → Location Picker
      │   │       ├─ Select Building
      │   │       ├─ Select Floor
      │   │       ├─ Select Room
      │   │       └─ Save Changes
      │   └─ Done button → back
      └─ Not editable/deletable
```

---

## Create Forms

### New Building
- Building Name (required, validated: no whitespace-only, max length)
- Access Notes (optional, multiline, special characters)
- Save / Cancel buttons

### New Floor
- Building field (prefilled, read-only)
- Floor Name (required, same validation as building)
- Access Notes (optional)
- Save / Cancel buttons

### New Room
- Building + Floor fields (prefilled, read-only)
- Room Name (required, same validation)
- Access Notes (optional)
- Save / Cancel buttons

---

## Test Cases

### Building Management — 24 tests

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| New Building UI | TC_NB_001–003 | 3 | Form elements, Cancel, Save buttons |
| Name Validation | TC_NB_004–007 | 4 | Valid input, required, whitespace, max length |
| Access Notes | TC_NB_008–009 | 2 | Optional, multiline/special chars |
| Creation | TC_NB_010–011 | 2 | Successful create, double-tap prevention |
| Error Handling | TC_NB_012–014 | 3 | Network error, background restore |
| Building List | TC_BL_001–003 | 3 | Display, long-press menu, close menu |
| Edit Building | TC_EB_001–005 | 5 | Prefilled data, name update, notes update, save state |
| Delete Building | TC_DB_001–002 | 2 | Immediate delete, styling |

### Floor Management — 19 tests

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| New Floor | TC_NF_001–010 | 10 | UI, building prefilled, save state, validation, creation, cancel, count update |
| Floor List | TC_FL_001–003 | 3 | Display, expand/collapse, context menu |
| Edit Floor | TC_EF_001–004 | 4 | Prefilled data, name update, building read-only, cancel |
| Delete Floor | TC_DF_001–002 | 2 | Immediate delete, count update |

### Room Management — 26 tests

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| New Room | TC_NR_001–010 | 10 | UI, prefilled fields, validation, creation, cancel, count update |
| Room List | TC_RL_001–003 | 3 | Display, asset count, context menu |
| Edit Room | TC_ER_001–005 | 5 | Prefilled data, name update, read-only fields, notes, cancel |
| Delete Room | TC_DR_001–002 | 2 | Immediate delete, count update |
| Room Detail | TC_RD_001–006 | 6 | UI, breadcrumb, empty state, assets list, tap asset, Done button |

### Asset Location — 16 tests

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| No Location | TC_NL_001–008 | 8 | Section display, asset list, tap asset, search, Done, not editable, count updates |
| Assign Location | TC_AL_001–008 | 8 | Location selector, picker, save, count decrease, cancel, reassign |

---

## Critical Patterns

### Scrolling to Off-Screen Elements
Manual scrolling (~350-500px/swipe) is too slow for deep hierarchies. Use `mobile: scroll` with `predicateString`:
```java
params.put("predicateString", "label CONTAINS 'elementName'");
driver.executeScript("mobile: scroll", params);
```

### Location Picker Hierarchy Detection
- **Building**: name contains ` floor` or ` floors`
- **Floor**: name contains ` room` or starts with `Floor_`
- **Room**: name contains ` node` or starts with `Room_`

**NEVER use `visible == true`** — items may be off-screen.

## Files
- **Test**: `src/test/java/com/egalvanic/tests/LocationTest.java` (7,757 lines)
- **Page**: `src/main/java/com/egalvanic/pages/BuildingPage.java` (6,656 lines)
- **TestNG**: `src/test/resources/parallel/testng-location.xml`
