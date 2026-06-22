# Issue Classes — Live Ground Truth (issue_classes API)

**Source:** `GET /api/issue_classes/user/{id}` on `acme.qa.egalvanic.ai` (V1.21), 2026-06-22, cross-checked against `testcase_file/issue_classes_template.xlsx`.

Live returns **19 classes**: the **7 real** product classes below + **12 junk/test** classes still present in the company data (`DEVTOOL_TEST IssueClass Updated` ×9, `Test  1`/`Test 2`/`Test 3`). The iOS Issue Class dropdown shows ALL of them, so count assertions must be `>=`, never `==`.

## The 7 real classes + fields (type, options)

### NEC Violation
- **Subcategory** _(select, required)_ — 24 options:
  - Breaker is restricted from freely operating (NEC 240.8)
  - Exceeds panel limit (NEC 408.36)
  - Visible Corrosion
  - Improperly fastened or secured (NEC 300.11(A))
  - Missing dead front, door, cover, etc. (NEC 110.12(A))
  - Wire bundle should have listed bushing
  - Wire burned or damaged
  - Bonding and grounding requirement (NEC 250.97)
  - Inadequate ventilation/cooling for component
  - Connection made without damaging wire (NEC 110.14(A))
  - 1 wire per terminal (NEC 110.14(A))
  - Improper neutral conductor (NEC 200.4(A))
  - Not protected from damage (NEC 300.4)
  - Size wrong for load (NEC 210.19(A))
  - Need earth connection (NEC 250.4(A))
  - Missing or insufficient information (NEC 110.21(B) or 408.4)
  - Damaged/broken parts
  - Missing arc flash and shock hazard warning labels (NEC 110.16(A))
  - Exposed energized parts (NEC 240.50(D))
  - Must be free of foreign materials (NEC 110.12(B) / NFPA 70B 13.3.2)
  - Unused opening must be sealed (NEC 110.12(A) / 312.5(A))
  - Parallel fuses must match (NEC 240.8)
  - Lack of component integrity
  - Not installed in a proper worklike manner
- **Consequences if Not Corrected** _(multi_select, required)_ — 4 options:
  - Equipment Failure
  - Fire Hazard
  - Safety Hazard
  - Power Interruption
- **Corrective Actions** _(multi_select)_ — 4 options:
  - Fixed During Visit
  - Contractor Will Correct
  - Customer Will Correct
  - Estimate Required
- Proposed Resolution _(textfield)_

### NFPA 70B Violation
- **Subcategory** _(select, required)_ — 13 options:
  - Chapter 28.3.2 Motor Control Equipment Cleaning
  - Chapter 28.3.1 Visual Inspections
  - Chapter 13.3.1 Visual Inspections
  - Chapter 15.3.2 Circuit Breakers Low- and Medium Voltage
  - Chapter 15.3.1 Visual Inspections
  - Chapter 25.3.2 UPS Cleaning
  - Chapter 25.3.1 Visual Inspections
  - Chapter 11.3.2 Power and Distribution Transformer Cleaning
  - Chapter 11.3.1 Visual Inspections
  - Chapter 12.3.2 Substations and Switchgear Cleaning
  - Chapter 12.3.1 Visual Inspections
  - Chapter 13.5.2 Panelboards and Switchboards Cleaning
  - Chapter 17.3.1 Visual Inspections

### OSHA Violation
- **Subcategory** _(select, required)_ — 11 options:
  - Clearance - Insufficient Access
  - Enclosure - Broken locking mechanism
  - Enclosure - Damaged
  - Enclosure - Should be waterproof
  - Equipment - Free of Hazards
  - Grounding - Must be permanent & continuous
  - Lighting - Inadequate around equipment
  - Marking/Labels - Inadequate or missing information on equipment
  - Mounting - Should be secure
  - Noise - Excessive
  - Wire - Exposed

### Repair Needed
- Approved for Repair at Time of Visit? _(textfield)_
- Customer Wants Repair In The Future? _(textfield)_
- Quote Requested For Repair? _(textfield)_

### Replacement Needed
- Replacement check 1 _(textfield)_
- Replacement check 2 _(textfield)_
- Replacement check 3 _(textfield)_

### Thermal Anomaly
- **Severity Criteria** _(select)_ — 3 options:
  - Similar
  - Ambient
  - Indirect
- Reference Temp _(temperature, required)_
- Problem Location _(textfield)_
- Problem Temp _(temperature, required)_
- Current Draw (A) _(table_with_column_headers)_
- Current Rating (A) _(table_with_column_headers)_
- Voltage Drop (mV) _(table_with_column_headers)_
- Delta T _(calculated)_
- **Severity** _(calculated, required)_ — 4 options:
  - Nominal
  - Intermediate
  - Serious
  - Critical

### Ultrasonic Anomaly
- _(no core-attribute fields)_
