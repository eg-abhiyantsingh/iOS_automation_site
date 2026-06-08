# eGalvanic App — End-to-End Domain Map (live-verified)

Built by driving the **live web app** `https://acme.qa.egalvanic.ai` (company `acme`,
admin login) with Playwright, 2026-06-08. The web app (V1.21) and the iOS app
(v1.36) are the **same product / same backend** (`api.qa.egalvanic.ai`); the iOS app
is the **mobile subset**. This map is the source-of-truth for what the iOS tests
should cover and assert. Companion: `COVERAGE.md`, `node-classes-gold-spec` (memory).

> Working approach (per standing direction): go deep, divide into parts, iterate
> autonomously, use Playwright (visible) to learn real flows, then fix/extend iOS
> tests + speed up CI. Parts tracked in the session TODO.

## 1. Full web feature inventory (V1.21 nav)
- **Dashboards:** Site Overview (`/dashboard`), Sales Overview, Ops Overview
- **Engineering:** Panel Schedules, Arc Flash Readiness, Equipment Designations, Equipment Library
- **Data:** SLDs, **Assets**, **Connections**, **Locations**, **Tasks**, **Issues**, Attachments
- **Operations:** Condition Assessment (`/pm-readiness`), EMPs, **Work Orders** (`/sessions`), Scheduling
- **Sales:** Goals, Opportunities, Accounts
- **Reporting:** Report Builder, Forms (`/eg-forms`)
- **Admin:** Settings (`/admin`), Audit Log

### iOS app coverage maps to (mobile subset)
| iOS test class | App area |
|---|---|
| AuthenticationTest | Login (mobile: company code → email/pw) |
| SiteSelectionTest | Site picker (mobile-only entry) |
| Asset_Phase1..6_Test | Data → Assets |
| Connections_Test | Data → Connections (tab hidden in current iOS build) |
| LocationTest | Data → Locations |
| Issue_Phase1..3_Test | Data → Issues |
| SiteVisit_phase1..3 | Operations → Work Orders (`/sessions`) |
| WorkOrderPlanning_Test | Work-order planning |
| OfflineTest / OfflineSyncMultiSite_Test | mobile-only offline/sync |
| ZP323_NewFeatures_Test | AI Extract + IR photo upload |

**Web-only (NOT in iOS app):** Dashboards, Engineering (Panel Schedules / Arc Flash /
Equipment Designations / Equipment Library), Sales, Reporting/Forms, Admin, SLDs,
Tasks (web has a Tasks module; iOS has tasks under assets), Attachments.

## 2. Issues domain (live-verified — the Issue tests' "gold spec")
**List columns:** Title · Issue Class · Priority · Asset · Session · Site · Created · Status · Actions.
List toolbar: Create Issue · Bulk Upload · Generate Report · Search · filters.

**Create-Issue form — BASIC INFO:**
- **Priority** (combobox, default **Medium**)
- **Immediate Hazard** (Yes/No, default No)
- **Customer Notified** (Yes/No, default No)
- **Issue Class\*** (required)
- **Asset** (searchable combobox — links the issue to an asset)
- **Title\*** (required)
- **Description**
- **Proposed Resolution\*** (required)
- **Issue Photos** (Upload Photo)
- **DETAILS** section is **DYNAMIC per Issue Class** ("Select an issue class to configure details").
- Footer: Cancel · Create Issue.

**Issue Classes (the 7 — exhaustive):**
`NEC Violation`, `NFPA 70B Violation`, `OSHA Violation`, `Repair Needed`,
`Replacement Needed`, `Thermal Anomaly`, `Ultrasonic Anomaly`.
→ There is **no "Other" class** (confirms memory `issues-bugs`).

**Thermal Anomaly DETAILS fields:** `Severity Criteria` (combobox), `Reference Temp*`
(°F, required), `Problem Location`, `Problem Temp*` (°F, required), `Delta T`
(computed), `Severity` (computed), `Current Draw (A)`, `Current Rating (A)`,
`Voltage Drop (mV)`.
→ iOS `Issue_Phase2` (Thermal/Severity/Temperature) must fill **Reference Temp** +
**Problem Temp** (the two required °F fields) to drive Delta-T/Severity.
**Ultrasonic Anomaly** has its own ultrasonic DETAILS (capture when fixing those tests).

**Statuses seen:** Open (lifecycle likely Open → In Progress → Resolved/Closed).

## 3. Next (parts)
- PART 2: fix iOS Issues failures (~45) — verify each test uses a valid Issue Class +
  the right per-class DETAILS fields (Thermal needs Reference/Problem Temp).
- PART 3: Asset / Site Selection / ZP-323 / Auth failures.
- PART 4: coverage gaps (DELETE, concurrency, statuses, Bulk Upload, Generate Report).
- PART 5: CI speed.
