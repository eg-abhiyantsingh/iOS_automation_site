# Connections Module - Flow Documentation

## Overview
The Connections module manages electrical connections between assets (source → target with type). Features: list, search, create, edit, delete, AF Punchlist mode, Select Multiple/bulk delete. Total: **96 test cases** (TC_CONN_001–TC_CONN_096).

---

## Screen Flow

```
Dashboard
    ↓  (tap Connections tab)
Connections Screen (List View)
  ├─ Header: WiFi icon | Options icon | + button
  ├─ Title: "Connections"
  ├─ Search bar: "Search connections..."
  ├─ Connection list: "SourceNode → TargetNode" format
  │   ├─ Missing Node entries (red, warning icon)
  │   ├─ Chevron on each entry
  │   └─ Long names truncated with "..."
  │
  ├─ Tap + → New Connection Screen
  │   ├─ Cancel | "New Connection" | Create (disabled)
  │   ├─ Source Node dropdown → Asset list (name + class)
  │   ├─ Target Node dropdown → Asset list (excludes source)
  │   ├─ Connection Type dropdown → Busway | Cable | ...
  │   └─ Create enabled when all 3 filled
  │
  ├─ Tap connection → Connection Details
  │   ├─ Source/Target/Type info
  │   └─ Three dots menu → Edit | Delete
  │
  ├─ Options menu → "Show AF Punchlist"
  │   ├─ Red X icons on each connection
  │   ├─ Tap X → delete with confirmation
  │   └─ "Hide AF Punchlist" to exit
  │
  └─ Options menu → "Select Multiple"
      ├─ Checkboxes on each connection
      ├─ Header: Cancel | "N Selected" | Delete icon
      ├─ Select All / Deselect All
      └─ Delete selected → confirmation → bulk delete
```

---

## Test Cases by Section

| Section | IDs | Tests | Coverage |
|---|---|---|---|
| Connections List | TC_CONN_001–006 | 6 | Tab, header, search bar, list, entry format, truncation |
| Missing Node | TC_CONN_007–008 | 2 | Warning display (red), tap behavior |
| Search | TC_CONN_009–013 | 5 | Filter, case-insensitive, no results, clear, search by target |
| Add Connection | TC_CONN_014 | 1 | + button opens New Connection |
| New Connection UI | TC_CONN_015–019 | 5 | UI elements, Create disabled, Cancel, validation, Source dropdown |
| Source Node | TC_CONN_020–024 | 5 | Asset list, name+class, search, select, change selection |
| Target Node | TC_CONN_025–029 | 5 | Dropdown, assets, search, select, checkmark |
| Self-Connection | TC_CONN_030 | 1 | Cannot select same node as source and target |
| Connection Type | TC_CONN_031–035 | 5 | Field, options, Busway/Cable selection, change type |
| Create Connection | TC_CONN_036–039 | 4 | Create enabled, successful create, appears in list, success message |
| Validation | TC_CONN_040–042 | 3 | Create disabled without source/target/type |
| Options Menu | TC_CONN_043–044 | 2 | Three dots icon, shows options |
| Details | TC_CONN_045–046 | 2 | Tap opens details, details screen |
| Edit | TC_CONN_047–048 | 2 | Edit option, edit Connection Type |
| Delete | TC_CONN_049–051 | 3 | Delete option, confirmation, successful delete |
| Duplicate Prevention | TC_CONN_052 | 1 | Cannot create duplicate |
| Empty State | TC_CONN_053 | 1 | "No connections" message |
| SLD Integration | TC_CONN_054–055 | 2 | Visible on SLD, type reflected |
| Performance | TC_CONN_056–057 | 2 | List load speed, search performance |
| Keyboard/Edge Cases | TC_CONN_058–062 | 5 | Keyboard appears/dismiss, special chars, single entry, rapid creation |
| Offline Mode | TC_CONN_063–064 | 2 | Create offline, sync online |
| AF Punchlist Options | TC_CONN_067–070 | 4 | Show AF Punchlist, Select Multiple, toggle, Hide AF Punchlist |
| Red X Delete | TC_CONN_071–073 | 3 | Red X icons, hide removes icons, tap X deletes |
| Select Multiple | TC_CONN_074–079 | 6 | Enter mode, Cancel, 0 Selected, checkboxes, select, count updates |
| Selection State | TC_CONN_080–084 | 5 | Deselect, Select All, toggle, Delete enabled/disabled |
| Bulk Delete | TC_CONN_085–090 | 6 | Confirmation, count message, cancel, confirm, deleted, list updates |
| Search in Selection | TC_CONN_091–092 | 2 | Search works in mode, selections persist |
| Combined Modes | TC_CONN_093 | 1 | Red X visible in selection mode |
| Select All Delete | TC_CONN_094 | 1 | Select All + Delete All |
| Missing Node Select | TC_CONN_095 | 1 | Missing Node selectable |
| Persistence | TC_CONN_096 | 1 | AF Punchlist state persists on tab switch |

---

## Critical Patterns

### Asset Dropdown Y-Gap Grouping
Each asset renders as TWO text elements (name + type):
- Same asset: Y-gap ~27px
- Different assets: Y-gap ~37px
- **Threshold: 32px** to group elements

### Parent-Child Connection Prevention
- A1 (parent) cannot connect to its children (Disconnect Switch 1, 2)
- **Always use sibling indices** (not index 0) when creating connections

### Random Sibling Selection
`selectRandomSiblingAsset(excludeIndices)` always excludes index 0 (parent). Pass source index to guarantee different target.

## Files
- **Test**: `src/test/java/com/egalvanic/tests/Connections_Test.java` (8,352 lines, 96 tests)
- **Page**: `src/main/java/com/egalvanic/pages/ConnectionsPage.java` (8,520 lines)
- **TestNG**: `src/test/resources/parallel/testng-connections.xml`
