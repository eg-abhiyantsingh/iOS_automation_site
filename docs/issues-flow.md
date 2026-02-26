# Issues Module - Flow Documentation

## Overview
The Issues module manages issue creation, editing, status workflow, class-specific fields (NEC/NFPA/OSHA/Thermal/Ultrasonic), swipe actions, and filters. Total: **237 test cases** (TC_ISS_001–TC_ISS_237) across 3 phases.

---

## Screen Flow

```
Dashboard
    ↓  (tap Issues button)
Issues List Screen
  ├─ Header: Done | Sort icon | + button | "Issues"
  ├─ Search bar: "Search issues"
  ├─ Filter tabs (scrollable): All | Open | In Progress | Resolved | Closed | With Photos | My Session
  ├─ Issue entries: [Type Icon] [Title] [Priority Badge] [Status Badge] [Asset Name]
  │   ├─ Swipe left → Delete (red) | Resolve (blue)
  │   └─ Tap → Issue Details
  └─ Empty state: "No Issues Found"

New Issue Screen (tap +)
  ├─ Header: Cancel | "New Issue" | Create (disabled until valid)
  ├─ CLASSIFICATION: Issue Class dropdown
  │   Options: NEC Violation, NFPA 70B, OSHA, Repair Needed, Thermal Anomaly, Ultrasonic Anomaly
  ├─ ISSUE DETAILS: Title, Priority (High/Medium/Low)
  │   + Class-specific fields (see below)
  ├─ ASSIGNMENT: Asset field → Asset Picker (search, QR scan, Add Asset)
  └─ Description, Proposed Resolution (optional)

Issue Details Screen (tap issue)
  ├─ Header: Close | [Title] | Status dropdown (Open → In Progress → Resolved → Closed)
  ├─ Asset, Priority, Issue Class
  ├─ Issue Details section (completion %, Required Fields toggle)
  ├─ Class-specific fields (editable)
  ├─ Photos section: Gallery | Camera
  ├─ Save Changes button (blue, on edit)
  ├─ Delete Issue button (red, bottom)
  └─ Unsaved Changes warning on exit
```

---

## Class-Specific Fields

| Issue Class | Required Fields | Optional Fields |
|---|---|---|
| NEC Violation | Subcategory (searchable dropdown) | Description, Proposed Resolution |
| NFPA 70B Violation | Subcategory (chapter sections, searchable) | Description, Proposed Resolution |
| OSHA Violation | Subcategory (Clearance, Enclosure, Equipment, etc.) | Description, Proposed Resolution |
| Repair Needed | None (no subcategory) | Description, Proposed Resolution |
| Thermal Anomaly | Severity, Problem Temp, Reference Temp (3 required) | Severity Criteria, Position, Current Draw table, Voltage Drop table |
| Ultrasonic Anomaly | None | Description, Proposed Resolution |

---

## Phase Breakdown

### Phase 1 — 119 tests (Issue_Phase1_Test.java, 5,993 lines)

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| Issues List | TC_ISS_001–007 | 7 | Header, search bar, filter tabs, default tab, All/Resolved/Closed filters |
| Issue Entry | TC_ISS_008–014 | 7 | Entry elements, type icons, High/Medium badges, Open status, asset name, truncation |
| Search | TC_ISS_015–018 | 4 | Filter, search by asset, no results, clear |
| Sort | TC_ISS_019–020 | 2 | Sort icon, sort options |
| New Issue | TC_ISS_021–025 | 5 | + button, UI elements, Create disabled, Asset required, Cancel |
| Issue Class | TC_ISS_026–033 | 8 | Dropdown, options, select each class type |
| Title | TC_ISS_034–035 | 2 | Field display, enter text |
| Priority | TC_ISS_036–040 | 5 | Dropdown, options, High/Medium/Low |
| Asset Selection | TC_ISS_041–047 | 7 | Field, picker screen, asset list, search, select, Add Asset, QR Scan |
| Issue Creation | TC_ISS_048–051 | 4 | Create enabled, successful create, appears in list, count increases |
| Issue Details | TC_ISS_052–079 | 28 | Details screen, status change workflow, priority, class, subcategory, description, resolution, photos, delete, save, Done/Close |
| Advanced Subcategory | TC_ISS_082–083 | 2 | OSHA subcategories, Thermal subcategories |
| Edge Cases | TC_ISS_084–086 | 3 | No optional fields, long description, special characters |
| Offline | TC_ISS_087–088 | 2 | Create offline, edit offline |
| Performance | TC_ISS_089–090 | 2 | List load speed, scroll performance |
| NFPA 70B | TC_ISS_091–105 | 15 | Subcategory field, dropdown, chapter-specific options |
| Subcategory Verification | TC_ISS_106–119 | 14 | Selection, search, completion %, green checkmark, class change updates, clear |

### Phase 2 — 60 tests (Issue_Phase2_Test.java, 4,802 lines)

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| OSHA Subcategories | TC_ISS_120–136 | 17 | All OSHA options (Clearance, Enclosure, Equipment, Grounding, Lighting, Marking, Mounting, Noise, Wire), search, cross-class comparison |
| Repair Needed | TC_ISS_137–143 | 7 | No subcategory, empty details, description, resolution, save, create without subcategory |
| Thermal Anomaly | TC_ISS_144–176 | 33 | Severity (4 levels), Severity Criteria (3 options), Position, Problem/Reference Temp, Current Draw table (A/B/C/N), Voltage Drop table, Required Fields toggle (3/3), 100% completion, clear selections |
| Ultrasonic Anomaly | TC_ISS_177–179 | 3 | No required fields, details section, description |

### Phase 3 — 58 tests (Issue_Phase3_Test.java, 4,530 lines)

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| Ultrasonic Save/Create | TC_ISS_180–183 | 4 | Save, create, comparison with Thermal/Repair |
| Status Filter Tabs | TC_ISS_184–191 | 8 | All 5 tabs, In Progress badge, tab scrollable |
| Issue Icons/Priority | TC_ISS_192–195 | 4 | Different icons per type, Warning icon, Low badge, description |
| Status Workflow | TC_ISS_196–199 | 4 | Open→InProgress→Resolved→Closed, reopen closed |
| Filter Count Sum | TC_ISS_200 | 1 | Counts add up |
| Swipe Actions | TC_ISS_201–212 | 12 | Swipe left, delete/resolve buttons, confirmation, hide on tap, all types, one at a time, quick resolve, resolved issue swipe |
| With Photos / My Session | TC_ISS_213–220 | 8 | Tab visible, filter works, count accuracy, My Session styling, hidden without job |
| Sort & Filter | TC_ISS_221–237 | 17 | Scrollable tabs, Sort dropdown, Created/Modified/Title/Status sort, persistence, works with filters |

---

## Status Workflow
```
Open → In Progress → Resolved → Closed
  ↑                                  │
  └──────── Reopen (back to Open) ───┘
```

## Files
- **Tests**: `Issue_Phase1_Test.java` (5,993), `Issue_Phase2_Test.java` (4,802), `Issue_Phase3_Test.java` (4,530)
- **Page**: `IssuePage.java` (10,367 lines)
- **TestNG**: `testng-issues-phase1.xml`, `testng-issues-phase2.xml`, `testng-issues-phase3.xml`
