# Asset Management Module - Flow Documentation

## Overview
The Asset Management module covers asset creation, editing, validation, and subtype management across **20+ asset classes**. Total: **539 test cases** across 5 phase files.

---

## Screen Flow

```
Dashboard
    ↓  (tap Assets card or list.bullet icon)
Asset List Screen
  ├─ Search bar
  ├─ + button (Add Asset)
  └─ Asset entries (name, class, location)
        │
        ├─ Tap + → New Asset Screen
        │   ├─ Asset Name field (required)
        │   ├─ Select Asset Class dropdown
        │   ├─ Select Location button
        │   ├─ Select Asset Subtype button
        │   ├─ QR Code field
        │   ├─ Profile Photo
        │   └─ Save / Cancel buttons
        │
        └─ Tap Asset → Asset Detail Screen
                ├─ Asset info (name, class, location, subtype)
                ├─ Edit button
                └─ Tap Edit → Edit Asset Screen
                    ├─ Core Attributes section (class-specific fields)
                    ├─ Required Fields toggle (shows X/Y completion)
                    ├─ Validation indicators (green check / red X)
                    ├─ Issues section
                    ├─ Save Changes / Cancel buttons
                    └─ Completion percentage
```

---

## Phase Breakdown

### Phase 1 — 113 tests (Asset_Phase1_Test.java, 5,679 lines)
| Section | Tests | Coverage |
|---|---|---|
| ATS Create (ECR) | 19 | Screen load, UI, mandatory fields, name validation, location, class, subtype, QR, photo, save/cancel |
| ATS Edit (EAD) | 19 | Core attributes, required fields toggle, completion %, save states, validation indicators |
| Busway Edit (BUS_EAD) | 4 | No core attributes for Busway, completion % hidden |
| Capacitor Edit (CAP_EAD) | 26 | Phase serial numbers, catalog, fluid, fuse, KVAR, manufacturer, model, PCB, voltage |
| Bug Regression | 45 | Regression tests for previously fixed issues |

### Phase 2 — 108 tests (Asset_Phase2_Test.java, 4,116 lines)
| Section | Tests | Coverage |
|---|---|---|
| Circuit Breaker (CB_EAD) | 24 | Ampere, breaker settings, interrupting rating, manufacturer, voltage |
| Disconnect Switch (DS_EAD) | 23 | Ampere, interrupting, voltage, validation indicators |
| Fuse (FUSE_EAD) | 24 | Amperage, manufacturer, KA rating, refill, spare fuses |
| Generator (GEN_EAD) | 20 | Ampere, configuration, KVAR, KW, power factor, voltage |
| Junction Box (JB_EAD) | 17 | Catalog, manufacturer, model, size |

### Phase 3 — 109 tests (Asset_Phase3_Test.java, 4,854 lines)
| Section | Tests | Coverage |
|---|---|---|
| Load Center (LC_EAD) | 28 | Ampere, catalog, columns, configuration, fault withstand, mains type, size, voltage |
| MCC (MCC_EAD) | 26 | Ampere, catalog, configuration, fault withstand, size, voltage |
| MCC Bucket (MCCB_EAD) | 12 | No core attributes, no required fields toggle |
| Motor (MOTOR_EAD) | 30 | Main type, duty cycle, frame, FLA, horsepower, RPM, service factor, temp rating |
| Other (OTHER_EAD_CA) | 13 | Model, notes, NP volts, serial number |

### Phase 4 — 97 tests (Asset_Phase4_Test.java, 4,213 lines)
| Section | Tests | Coverage |
|---|---|---|
| OCP (OCP_EAD) | 13 | No core attributes, no required fields toggle |
| Panel Board (PB_) | 14 | Size, voltage, scroll behavior |
| PDU (PDU_EAD) | 15 | PDU-specific field management |
| Relay (REL_EAD) | 14 | Relay field management |
| Switchboard (SB_EAD) | 18 | Switchboard attributes, validation |
| Transformer (TR_EAD) | 23 | KVA rating, configuration, efficiency, impedance, winding, oil type |

### Phase 5 — 112 tests (Asset_Phase5_Test.java, 4,967 lines)
| Section | Tests | Coverage |
|---|---|---|
| UPS (TC_UPS_) | 15 | Ampere, catalog, manufacturer, model, notes, size, voltage |
| Utility (TC_UTL_) | 9 | Meter number, starting voltage |
| VFD (TC_VFD_) | 8 | No core attributes |
| ATS Subtype (TC_ATS_ST_) | 13 | Automatic Transfer Switch Low/High, Transfer Switch Low/High |
| Busway Subtype (TC_BUS_ST_) | 11 | Busway Low/High subtypes |
| Capacitor Subtype (TC_CAP_ST_) | 6 | Subtype None selection |
| Circuit Breaker Subtype (TC_CB_ST_) | 14 | LICC, LMCC 250A, LPCB, MV variants |
| Default Subtype (TC_DEF_) | 9 | Default subtype behaviors |
| Disconnect Switch Subtype (TC_DS_ST_) | 16 | BPS, bypass isolation, fused disconnect, HPC, load interruptor |
| Fuse Subtype (TC_FUSE_ST_) | 11 | 1000V or less, over 1000V |

---

## Asset Classes (20+)
ATS, Busway, Capacitor, Circuit Breaker, Disconnect Switch, Fuse, Generator, Junction Box, Load Center, MCC, MCC Bucket, Motor, Other, OCP, Panel Board, PDU, Relay, Switchboard, Transformer, UPS, Utility, VFD

---

## Test ID Patterns
- **ECR** = Edit/Create (Phase 1 creation)
- **EAD** = Edit Asset Details (Phase 1–4 editing)
- **TC_[CLASS]_** = Test Case with class prefix (Phase 5)
- **_ST_** = Subtype tests (Phase 5)

## Files
- **Tests**: `Asset_Phase1_Test.java` through `Asset_Phase5_Test.java`
- **Page**: `AssetPage.java` (11,372 lines)
- **TestNG**: `testng-phase1.xml` through `testng-phase5.xml` + `testng-assets-part1.xml` through `testng-assets-part6.xml`
