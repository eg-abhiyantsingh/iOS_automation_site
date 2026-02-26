# Site Visit / Work Orders / Jobs - Flow Documentation

## Overview
The Site Visit module manages Work Orders (also referred to as "Jobs") in the eGalvanic iOS app. Users can view, start, and manage work orders from the dashboard, with each work order containing assets organized by location hierarchy.

---

## Screen Flow

### 1. Dashboard - No Active Work Order Card
- **Location**: Main Dashboard screen
- **UI Elements**:
  - Card showing "No Active Work Order" with icon
  - Subtext: "Tap to select a work order"
- **Action**: Tapping the card navigates to the Work Orders screen
- **Existing Code**: `SiteSelectionPage.noActiveJobCard`, `SiteSelectionPage.tapToSelectJobText`

### 2. Work Orders Screen
- **Header**: "Work Orders" (navigation title)
- **UI Elements**:
  - **"Start New Work Order" button** (blue, full-width) — creates a new work order
  - **"Available Work Orders" section** — lists existing work orders
- **Each Work Order Entry Shows**:
  - Work order name (e.g., "test job", "criticllll", "Job - Dec 19")
  - Date/time (e.g., "Feb 15, 2025 at 8:33 AM")
  - Status badge: "AVAILABLE" (gray badge for inactive jobs)
  - Green status indicator dot (left side)
  - Counts: Jobs count, Issues count (e.g., "0 | 0")

### 3. Start Work Order Session
- **Trigger**: Tapping "Start" on an available work order
- **Dialog**: Confirmation alert "Start Work Order Session?"
  - Message: "Do you want to start an inspection session for '[work order name]'?"
  - Buttons: "Cancel" | "Start Session"
- **Action**: Starting a session makes the work order active

### 4. Active Work Order Details
- **Header**: Work order name (e.g., "test job")
- **Status**: "Active" badge
- **Tabs**: Details | Assets | Tasks | Issues | Files
- **Details Tab Shows**:
  - **Completed**: count (e.g., 0)
  - **Incomplete**: count (e.g., 0)
  - **Open Issues**: count (e.g., 0)
  - **IR Photo Type**: e.g., "FLIR-SEP"
  - **Started**: date/time (e.g., "Feb 15, 2025 at 8:33 AM")
  - **Quick QR Action**: QR code button
- **End Button**: Red "END" button to end the work order session

### 5. New Work Order Form
- **Trigger**: Tapping "Start New Work Order" on Work Orders screen
- **Form Fields**:
  - **Work Order Configuration** section
  - **Name**: Text input (required)
  - **Photo Type**: Dropdown (e.g., FLIR-SEP)
  - **Team** section:
    - Shows current user with role (e.g., "Abhiyant - admin")
    - Online status indicator (green dot)
  - **Create** button

### 6. Work Order Assets Tab - Location Hierarchy
- **Structure**: Same location hierarchy as main app
  - Building (e.g., "Bldg_9568") → expandable
  - Floor (e.g., "Floor 77 Updated_119") → expandable
  - Room (e.g., room entries)
- **Room Actions**: Tapping a room shows assets or "No Assets" message

### 7. Room Actions Menu (+ Button)
- **Trigger**: Tapping "+" button in a room within work order
- **Options**:
  - **New Asset** — Create a new asset in this room
  - **Link Existing Asset** — Link an asset already in the system
  - **Photo Walkthrough** — Take photos of the room
  - **Quick Count** — Bulk-create assets by type/subtype count

### 8. New Asset (Within Work Order)
- Same as standard New Asset form
- Asset gets associated with the active work order

### 9. Link Existing Assets
- Shows searchable list of existing assets
- User can select one or more assets to link to the work order
- Each asset shows name, type, and location info

### 10. Photo Walkthrough
- Camera/photo capture screen
- Photos associated with the room/work order

### 11. Quick Count Flow
- **Step 1**: "Quick Count" screen — shows "No Asset Types Added" initially
- **Step 2**: Tap "+" → "Select Asset Type" picker (ATS, Busway, Capacitor, Cable Tray, etc.)
- **Step 3**: Select type → "Select Subtype" picker (e.g., for ATS: "Automatic Transfer Switch")
- **Step 4**: Shows asset type card with count (e.g., "ATS x1")
  - Subtype shown below
  - "Add Photoset for [Type] [Number]" link
  - Increment/decrement count with +/- buttons
- **Step 5**: "Create [N] Assets" button to bulk-create

---

## Test Cases - Phase 1 (TC_JOB_001 to TC_JOB_039)

### Dashboard / Work Orders Screen (TC_JOB_001–009) — 9 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_001 | Job Selection | No Active Job card visible on dashboard |
| TC_JOB_002 | Work Orders Screen | Tapping card opens Work Orders screen |
| TC_JOB_003 | Work Orders Screen | Header shows "Work Orders" |
| TC_JOB_004 | Start Work Order | "Start New Work Order" button visible |
| TC_JOB_005 | Work Orders Screen | "Available Work Orders" section displayed |
| TC_JOB_006 | Work Order Entry | Entry shows name, date, badge, counts |
| TC_JOB_007 | Work Order Entry | "AVAILABLE" badge on inactive work order |
| TC_JOB_008 | Work Order Entry | Green status indicator on entries |
| TC_JOB_009 | Work Order Entry | Job/issue counts in "N \| N" format |

### Activate Job (TC_JOB_010–013) — 4 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_010 | Activate Job | "Activate" button visible on available job card (outlined style, right side) |
| TC_JOB_011 | Activate Job | Tapping Activate changes badge from AVAILABLE to ACTIVE (green), removes Activate button |
| TC_JOB_012 | Activate Job | Green "ACTIVE" badge displayed on activated job (Partial: text verified, color limited) |
| TC_JOB_013 | Activate Job | Only one job can be active — activating new job deactivates previous one |

### Session Details (TC_JOB_014–019) — 6 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_014 | Session Details | Tapping active job opens Session Details screen |
| TC_JOB_015 | Session Details | Header: Back button, Job title (e.g., "Job - Dec 17, 12:18 PM"), Refresh icon |
| TC_JOB_016 | Session Details | "Active Session" badge with green dot (Partial: text verified, color limited) |
| TC_JOB_017 | Session Details | Three stat cards: Tasks count, Issues count (warning icon), IR Photos count |
| TC_JOB_018 | Session Details | INFORMATION section: Session Type (FLIR-SEP), Started date/time, Quick QR Action |
| TC_JOB_019 | Session Details | Quick QR Action dropdown with current setting (e.g., "Full Asset") |

### Session Tabs & Issues (TC_JOB_020–026) — 7 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_020 | Session Details | 5 bottom tabs: Details (selected), Locations, Tasks, Issues (with badge), Files |
| TC_JOB_021 | Session Issues | Tapping Issues tab opens session-specific issues |
| TC_JOB_022 | Session Issues | Issues tab badge shows count (e.g., '2') of linked issues |
| TC_JOB_023 | Session Issues | Summary: Total count, Open count (green), Closed count (red). Partial: colors limited |
| TC_JOB_024 | Session Issues | Green 'Manage Issues' button with checkmark icon displayed |
| TC_JOB_025 | Session Issues | Linked issues list: title, description, issue class tag, asset location, status badge |
| TC_JOB_026 | Session Issues | Blue floating '+' button at bottom right to create new issue |

### Link Issues (TC_JOB_027–029) — 3 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_027 | Link Issues | Tapping Manage Issues opens Link Issues screen (Cancel, title, Update) |
| TC_JOB_028 | Link Issues | UI: Cancel, 'Link Issues' title, Update button, search bar, 'SELECT ISSUES' label, issue list with checkboxes |
| TC_JOB_029 | Link Issues | Already linked issues show blue filled checkmark on right side |

### Link Issues Interactions (TC_JOB_030–034) — 5 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_030 | Link Issues | Unlinked issues show empty circle checkbox (unfilled) |
| TC_JOB_031 | Link Issues | Issue entry shows title, asset info, status badge, date |
| TC_JOB_032 | Link Issues | Tapping unlinked issue selects it (fills checkmark) |
| TC_JOB_033 | Link Issues | Tapping linked issue deselects it (unfills checkmark) |
| TC_JOB_034 | Link Issues | Update button state — visible before and after selection changes |

### Link Issues Save/Cancel/Search & My Session (TC_JOB_035–039) — 5 tests

| ID | Feature | Verifies |
|---|---|---|
| TC_JOB_035 | Link Issues | Update button saves linked issue changes, returns to Session Issues |
| TC_JOB_036 | Link Issues | Cancel discards changes, returns to Session Issues with original count |
| TC_JOB_037 | Link Issues | Search bar filters issues in Link Issues screen |
| TC_JOB_038 | My Session Filter | My Session filter tab visible on Session Issues screen |
| TC_JOB_039 | My Session Filter | My Session count matches linked session issues |

---

## Existing Code References

### SiteSelectionPage.java (Page Object)
| Element/Method | Line | Purpose |
|---|---|---|
| `noActiveJobCard` | 159 | "No Active Job" card locator |
| `tapToSelectJobText` | 162 | "Tap to select a job" text locator |
| `isNoActiveJobCardDisplayed()` | 1859-1914 | Checks card visibility (5 fallback strategies) |
| `clickNoActiveJobCard()` | 1919-1979 | Clicks card to open jobs (5 fallback strategies) |

### WorkOrderPage.java (Page Object — Created)
| Method | Purpose |
|---|---|
| `isWorkOrdersScreenDisplayed()` | Detect Work Orders screen (5 strategies) |
| `waitForWorkOrdersScreen()` | Wait up to 10s for screen load |
| `isWorkOrdersHeaderCorrect()` | Verify header text contains "Work Order" |
| `isStartNewWorkOrderButtonDisplayed()` | Check Start New Work Order button (3 strategies) |
| `isAvailableWorkOrdersSectionDisplayed()` | Check Available Work Orders section (3 strategies) |
| `getWorkOrderEntryCount()` | Count work order entries (3 strategies) |
| `getWorkOrderName(int)` | Get name of work order at index |
| `getWorkOrderDate(int)` | Get date of work order at index |
| `isAvailableBadgeDisplayed(int)` | Check AVAILABLE badge at index |
| `isGreenStatusIndicatorDisplayed()` | Detect green status dots |
| `getWorkOrderCounts(int)` | Get "N \| N" counts at index |
| `isActivateButtonDisplayed()` | Check Activate button on available jobs |
| `tapActivateButton()` | Tap first Activate button |
| `isActiveBadgeDisplayed()` | Check for ACTIVE badge |
| `getActiveBadgeCount()` | Count ACTIVE badges |
| `tapActiveWorkOrder()` | Tap the active job entry |
| `isSessionDetailsScreenDisplayed()` | Detect Session Details screen (4 strategies) |
| `waitForSessionDetailsScreen()` | Wait up to 10s for screen load |
| `getSessionDetailsHeaderText()` | Get job title from header |
| `isActiveSessionBadgeDisplayed()` | Check "Active Session" badge text |
| `isSessionStatsDisplayed()` | Check Tasks/Issues/IR Photos cards |
| `getStatCardCount(String)` | Get numeric count for a stat label |
| `isInformationSectionDisplayed()` | Check INFORMATION section |
| `getSessionType()` | Get Session Type value (e.g., "FLIR-SEP") |
| `getStartedDateTime()` | Get Started date/time |
| `isQuickQRActionDisplayed()` | Check Quick QR Action dropdown |
| `getQuickQRActionValue()` | Get current QR action value |
| `getSessionBottomTabLabels()` | Get list of bottom tab labels |
| `areAllSessionTabsDisplayed()` | Verify all 5 tabs present |
| `tapSessionTab(String)` | Tap a tab by name (e.g., "Issues") |
| `isSessionIssuesContentDisplayed()` | Detect Issues tab content |
| `getIssuesTabBadgeCount()` | Get badge count on Issues tab |
| `getIssuesSummary()` | Get Total/Open/Closed counts map |
| `isManageIssuesButtonDisplayed()` | Check Manage Issues button |
| `tapManageIssuesButton()` | Tap Manage Issues button |
| `getLinkedIssueCount()` | Count linked issues in list |
| `isLinkedIssueEntryComplete()` | Verify issue entry has title/class/status |
| `isAddIssueFloatingButtonDisplayed()` | Check floating + button |
| `isLinkIssuesScreenDisplayed()` | Detect Link Issues screen |
| `waitForLinkIssuesScreen()` | Wait up to 10s for screen load |
| `isLinkIssuesCancelButtonDisplayed()` | Check Cancel on Link Issues |
| `isLinkIssuesUpdateButtonDisplayed()` | Check Update on Link Issues |
| `isLinkIssuesSearchBarDisplayed()` | Check search bar on Link Issues |
| `isSelectIssuesToLinkLabelDisplayed()` | Check "SELECT ISSUES" label |
| `getLinkIssuesListCount()` | Count issues in link list |
| `isAnyIssueChecked()` | Check for checked checkmarks |
| `tapLinkIssuesCancel()` | Tap Cancel to go back |
| `isEmptyCircleCheckboxDisplayed()` | Detect empty circle (unchecked) checkboxes |
| `getCheckedIssueCount()` | Count checked (selected) issues in link list |
| `isLinkIssueEntryComplete(int)` | Verify issue entry has title/status/date at index |
| `tapIssueInLinkList(int)` | Tap issue at index to toggle selection |
| `isIssueCheckedAtIndex(int)` | Check if specific issue is checked |
| `isUpdateButtonEnabled()` | Check if Update button is enabled (not grayed) |
| `tapUpdateButton()` | Tap Update to save linked issue changes |
| `searchInLinkIssues(String)` | Enter search query in Link Issues search bar |
| `clearSearchInLinkIssues()` | Clear search text in Link Issues |
| `isMySessionFilterDisplayed()` | Check for My Session filter tab |
| `tapMySessionFilter()` | Tap My Session filter |
| `getMySessionCount()` | Get count on My Session filter badge |

### Existing Test Coverage
| Test | File | Purpose |
|---|---|---|
| TC_SS_053 | SiteSelectionTest.java | Verifies job card displayed |
| TC_SS_054 | SiteSelectionTest.java | Verifies tap navigates to job selection |
| TC_OFF_033 | OfflineTest.java | Verifies active job persists offline |

---

## Future Phases
- Start New Work Order form (name, photo type, team)
- Work order tabs (Details, Assets, Tasks, Issues, Files)
- Assets tab — location hierarchy navigation
- Room actions (New Asset, Link Existing, Photo Walkthrough, Quick Count)
- Quick Count flow
- End work order session
- Active Work Order on dashboard (replaces "No Active Work Order" card)

---

## UI Element Identifiers (From Screenshots)

| Element | Type | Identifier Strategy |
|---|---|---|
| "No Active Work Order" card | StaticText/Button | `label CONTAINS 'No Active Work Order'` |
| "Tap to select a work order" | StaticText | `label CONTAINS 'Tap to select a work order'` |
| "Work Orders" header | NavigationBar/StaticText | `label == 'Work Orders'` |
| "Start New Work Order" button | Button | `label CONTAINS 'Start New Work Order'` |
| "Available Work Orders" section | StaticText | `label CONTAINS 'Available Work Orders'` |
| "AVAILABLE" badge | StaticText | `label == 'AVAILABLE'` |
| "Activate" button | Button/StaticText | `label == 'Activate'` |
| "ACTIVE" badge | StaticText | `label == 'ACTIVE'` |
| "Active Session" badge | StaticText | `label CONTAINS 'Active Session'` |
| "Tasks" stat card | StaticText | `label == 'Tasks'` |
| "Issues" stat card | StaticText | `label == 'Issues'` |
| "IR Photos" stat card | StaticText | `label CONTAINS 'IR Photo'` |
| "INFORMATION" section | StaticText | `label == 'INFORMATION'` |
| Session Type value | StaticText | Near "Session Type" label |
| "Quick QR Action" | StaticText/Button | `label CONTAINS 'Quick QR Action'` |
| Refresh icon | Button | `label CONTAINS 'arrow.clockwise'` or top-right button |
| Work order name | StaticText | By index in list |
| "Start" button on entry | Button | `label == 'Start'` |
| "Start Work Order Session?" dialog | Alert | Alert title check |
| "Start Session" confirm button | Button | `label == 'Start Session'` |
| "Cancel" button | Button | `label == 'Cancel'` |
| Bottom tabs (Details/Locations/etc.) | Button/StaticText | `label == 'Details'`, `label == 'Issues'`, etc. |
| "Manage Issues" button | Button/StaticText | `label CONTAINS 'Manage Issues'` |
| Total/Open/Closed summary | StaticText | `label == 'Total'`, `label == 'Open'`, `label == 'Closed'` |
| Floating "+" button | Button | `label == '+'` or bottom-right geometry |
| "Link Issues" title | StaticText/NavigationBar | `label == 'Link Issues'` |
| "Update" button | Button | `label == 'Update'` |
| Search issues bar | SearchField/TextField | `type == 'XCUIElementTypeSearchField'` |
| "SELECT ISSUES TO LINK" | StaticText | `label CONTAINS 'SELECT ISSUES'` |
| Checkmark (linked issue) | Image/Button | `name CONTAINS 'checkmark'` or `checkmark.circle.fill` |
