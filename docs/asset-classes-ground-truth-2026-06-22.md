# Asset Classes — Live Ground Truth (node_classes API)

**Source:** `GET /api/node_classes/user/{id}` on `acme.qa.egalvanic.ai` (web app V1.21), captured 2026-06-22, cross-checked against `node_classes_template (14).xlsx`.
**Why this doc:** the xlsx `Core Attributes` Options column is mis-aligned for several classes (Capacitor/Generator/Motor/Other/Relay/UPS carry stray option-lists on textfield rows). The API `definition[]` is correctly aligned and is the authoritative source for field type + dropdown options. This doc is the de-shifted truth.

## Per-class fields (type + dropdown options) and subtypes

### ATS  _(needs_source)_
- Catalog Number _(textfield)_
- **Contact Type** _(select)_ — default `Normally Open (NO)`: Normally Open (NO), Normally Closed (NC)
- **Interrupting Rating** _(select)_ — default `30 kA`: 10 kA, 20 kA, 30 kA, 40 kA, 50 kA, 65 kA, 80 kA, 100 kA
- **Ampere Rating** _(select)_ — default `800A`: 30A, 60A, 100A, 200A, 400A, 600A, 800A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Manufacturer** _(select)_ — default `Cummins`: ABB, Eaton, General Electric, Schneider Electric, Siemens, Cummins, Kohler, Westinghouse, Generac, ACC
- Mechanism Type _(textfield)_
- Model _(textfield)_
- **Mains Type** _(select)_ — default `MCB`: MCB, MLO
- Serial Number _(textfield)_
- Size _(textfield)_
- Notes _(textfield)_
- **Voltage** _(select)_ — default `240V`: 120V, 208V, 240V, 480V, 600V, 4160V
- Type _(textfield)_
- **Subtypes:** Automatic Transfer Switch (<= 1000V), Transfer Switch (<= 1000V), Transfer Switch (> 1000V), Automatic Transfer Switch (> 1000V)

### Battery  _(no flags)_
- _(no core-attribute fields)_
- **Subtypes:** Lithium-Ion

### Busduct  _(box, needs_source)_
- _(no core-attribute fields)_

### Busway  _(no flags)_
- _(no core-attribute fields)_

### Cable  _(needs_source)_
- _(no core-attribute fields)_

### Capacitor  _(needs_source)_
- A Phase Serial Number _(textfield)_
- B Phase Serial Number _(textfield)_
- Catalog Number _(textfield)_
- C Phase Serial Number _(textfield)_
- Fluid Capacity _(textfield)_
- Fluid Type _(textfield)_
- Fuse Amperage _(textfield)_
- Fuse Manufacturer _(textfield)_
- Fuse Refill Number _(textfield)_
- K V A R Rating _(textfield)_
- Manufacturer _(textfield)_
- Model _(textfield)_
- Notes _(textfield)_
- P C B Labeled _(textfield)_
- Spare Fuses _(textfield)_
- Style _(textfield)_
- Type _(textfield)_
- U F Rating _(textfield)_
- Voltage _(textfield)_
- Serial Number _(textfield)_
- **Subtypes:** Power Factor Correction

### Capacitor Bank  _(needs_source)_
- _(no core-attribute fields)_

### Circuit Breaker  _(ocp, needs_source)_
- **Manufacturer** _(select)_: Square D, Eaton, Siemens, General Electric, ABB, Schneider Electric, Cutler-Hammer, Westinghouse, ITE, Allen-Bradley, Mitsubishi, Toshiba, LS Electric, Fuji Electric, Omron, Eaton Crouse-Hinds, Other
- Model _(textfield)_
- **Ampere Rating** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Interrupting Rating** _(select)_: 5 kA, 10 kA, 15 kA, 20 kA, 25 kA, 30 kA, 35 kA, 40 kA, 50 kA, 65 kA, 80 kA, 100 kA, 150 kA
- **Voltage** _(select)_: 120V, 220V, 240V, 277V, 480V, 120/208V, 208/120V, 277/480V, 480/277V, 600V
- Catalog Number _(textfield)_
- Notes _(textfield)_
- Breaker Settings _(textfield)_
- **Subtypes:** Low-Voltage Molded Case Circuit Breaker (<= 225A), Recloser (<= 1000V), Motor Circuit Protector, Recloser (> 1000V), Low-Voltage Insulated Case Circuit Breaker, Low-Voltage Power Circuit Breaker, Medium-Voltage Gas Insulated Circuit Breaker, Medium-Voltage Air Magnetic Circuit Breaker, Medium-Voltage Oil Insulated Circuit Breaker, Medium-Voltage Vacuum Circuit Breaker, Low-Voltage Molded Case Circuit Breaker (> 225A)

### Default  _(needs_source)_
- _(no core-attribute fields)_

### Disconnect Switch  _(box, needs_source)_
- **Voltage** _(select)_: 120V, 208V, 240V, 480V, 600V, 4160V
- **Ampere Rating** _(select)_: 10A, 20A, 30A, 40A, 50A, 60A, 80, 100A, 200A, 400A, 600A, 800A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Interrupting Rating** _(select)_: 10 kA, 20 kA, 30 kA, 40 kA, 50 kA, 65 kA, 80 kA, 100 kA, 200KA
- **Manufacturer** _(select)_: ABB, Eaton, General Electric, Schneider Electric, Siemens, Cummins, Kohler, Westinghouse, Generac, ACC
- Catalog Number _(textfield)_
- Notes _(textfield)_
- **Subtypes:** Bolted-Pressure Switch (BPS), High-Pressure Contact Switch (HPC), Load-Interruptor Switch, Scrubtype, Bypass-Isolation Switch (<= 1000V), Disconnect Switch (<= 1000V), Fused Disconnect Switch (<= 1000V), Bypass-Isolation Switch (> 1000V), Disconnect Switch (> 1000V), Fused Disconnect Switch (>1000V)

### Fuse  _(ocp, needs_source)_
- Fuse Refill Number _(textfield)_
- **fuseManufacturer** _(select)_: BUSSMANN, CEFCON, CUTLER-HAMMER, EATON, EDISON, FERRAZ SHAWMUT, GE, GOULD SHAWMUT, LITTELFUSE, SIEMENS, SQUARED D, WESTINGHOUSE
- **fuseAmperage** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **KA Rating** _(select)_: 5 kA, 10 kA, 14 kA, 22 kA, 25 kA, 35 kA, 42 kA, 50 kA, 65 kA, 100 kA, 200 kA
- **Voltage** _(select)_: 120V, 220V, 240V, 250V, 277V, 480, 600V
- notes _(textfield)_
- Spare Fuses _(textfield)_
- Type _(textfield)_
- **Subtypes:** Fuse (<= 1000V), Fuse (> 1000V)

### Generator  _(no flags)_
- voltage _(textfield)_
- K W Rating _(textfield)_
- K V A Rating _(textfield)_
- Power Factor _(textfield)_
- Ampere Rating _(textfield)_
- manufacturer _(textfield)_
- Serial Number _(textfield)_
- configuration _(textfield)_

### Junction Box  _(needs_source)_
- Catalog Number _(textfield)_
- Manufacturer _(textfield)_
- Model _(textfield)_
- Notes _(textfield)_
- Size _(textfield)_

### Lighting Controls  _(ocp, needs_source)_
- model _(textfield)_
- notes _(textfield)_
- Relay Type _(textfield)_
- manufacturer _(textfield)_
- Serial Number _(textfield)_
- Rated Amps _(textfield)_
- voltage _(textfield)_

### Load  _(needs_source)_
- _(no core-attribute fields)_
- **Subtypes:** Resistive Load, General Load

### Loadcenter  _(box, needs_source)_
- Size _(textfield)_
- **Mains Type** _(select)_ — default `MLO`: MLO, MCB

### MCC  _(box, needs_source)_
- Size _(textfield)_
- Notes _(textfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 277V, 347V, 380V, 400V, 415V, 480V, 600V, 120/208V, 120/240V, 208/120V, 240/120V, 277/480V, 347/600V, 230V, 400/230V, 415/240V
- **Ampere Rating** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Manufacturer** _(select)_: Square D, Eaton, Siemens, General Electric, ABB, Schneider Electric, Cutler-Hammer, Westinghouse, ITE, Allen-Bradley, Federal Pacific, Zinsco, Bussmann, Challenger, Mitsubishi, Omron, Cooper, Eaton Crouse-Hinds, Fujitsu, LS Electric, Siemens BTI, ACC, Other
- Serial Number _(textfield)_
- Catalog Number _(textfield)_
- **Fault Withstand Rating** _(select)_: 10 kA, 14 kA, 18 kA, 22 kA, 25 kA, 30 kA, 35 kA, 42 kA, 50 kA, 65 kA, 85 kA, 100 kA, 150 kA, 200 kA
- **Subtypes:** Motor Control Equipment (<= 1000V), Motor Control Equipment (> 1000V)

### MCC Bucket  _(ocp, needs_source)_
- _(no core-attribute fields)_

### Meter  _(needs_source)_
- _(no core-attribute fields)_

### Motor  _(needs_source)_
- Catalog Number _(textfield)_
- Duty Cycle _(textfield)_
- Frame _(textfield)_
- Full Load Amps _(textfield)_
- Horsepower _(textfield)_
- Manufacturer _(textfield)_
- Model _(textfield)_
- Motor Class _(textfield)_
- Notes _(textfield)_
- Power Factor _(textfield)_
- R P M _(textfield)_
- Serial Number _(textfield)_
- Service Factor _(textfield)_
- Size _(textfield)_
- Temperature Rating _(textfield)_
- Voltage _(textfield)_
- **Mains Type** _(select)_: MCB, MLO
- **Subtypes:** Low-Voltage Machine (<= 200hp), Low-Voltage Machine (>200hp), Medium-Voltage Induction Machine, Medium-Voltage Synchronous Machine

### Motor Controller  _(needs_source)_
- _(no core-attribute fields)_

### Motor Starter  _(box, needs_source)_
- _(no core-attribute fields)_

### Node Bus  _(needs_source)_
- _(no core-attribute fields)_

### Other  _(box, needs_source)_
- Model _(textfield)_
- Notes _(textfield)_
- Serial Number _(textfield)_
- NP Volts _(textfield)_
- **Subtypes:** Battery Energy Storage System (ESS), Electrical Vehicle Charging Station, Ni-Cad Battery, Solar Photovoltaic System, Valve-Regulated Lead-Acid Battery, Vented Lead-Acid Battery, Wind Power System

### Other (OCP)  _(ocp, needs_source)_
- _(no core-attribute fields)_

### PDU  _(box, needs_source)_
- **Ampere Rating** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- Catalog Number _(textfield)_
- **Fault Withstand Rating** _(select)_: 10 kA, 14 kA, 18 kA, 22 kA, 25 kA, 30 kA, 35 kA, 42 kA, 50 kA, 65 kA, 85 kA, 100 kA, 150 kA, 200 kA
- **Manufacturer** _(select)_: Square D, Eaton, Siemens, General Electric, ABB, Schneider Electric, Cutler-Hammer, Westinghouse, ITE, Allen-Bradley, Federal Pacific, Zinsco, Bussmann, Challenger, Mitsubishi, Omron, Cooper, Eaton Crouse-Hinds, Fujitsu, LS Electric, Siemens BTI, ACC, Other
- Notes _(textfield)_
- Serial Number _(textfield)_
- Size _(numberfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 277V, 347V, 380V, 400V, 415V, 480V, 600V, 120/208V, 120/240V, 208/120V, 240/120V, 277/480V, 347/600V, 230V, 400/230V, 415/240V
- **Mains Type** _(select)_: MCB, MLO

### Panelboard  _(box, needs_source)_
- Size _(textfield)_
- **Mains Type** _(select)_: MCB, MLO
- **Columns** _(select)_: 1, 2, 3, 4, 6
- Notes _(textfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 277V, 347V, 380V, 400V, 415V, 480V, 600V, 120/208V, 120/240V, 208/120V, 240/120V, 277/480V, 347/600V, 230V, 400/230V, 415/240V
- **Ampere Rating** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Manufacturer** _(select)_: Square D, Eaton, Siemens, General Electric, ABB, Schneider Electric, Cutler-Hammer, Westinghouse, ITE, Allen-Bradley, Federal Pacific, Zinsco, Bussmann, Challenger, Mitsubishi, Omron, Cooper, Eaton Crouse-Hinds, Fujitsu, LS Electric, Siemens BTI, ACC, Other
- Serial Number _(textfield)_
- Catalog Number _(textfield)_
- **Fault Withstand Rating** _(select)_: 10 kA, 14 kA, 18 kA, 22 kA, 25 kA, 30 kA, 35 kA, 42 kA, 50 kA, 65 kA, 85 kA, 100 kA, 150 kA, 200 kA
- **Subtypes:** Panelboard, Branch Panel, Power Panel, Control Panel

### QANode  _(no flags)_
- _(no core-attribute fields)_

### QA_ATS1  _(needs_source)_
- **Ampere Rating** _(select)_: 30A, 60A, 100A, 200A, 400A, 600A, 800A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- Catalog Number _(textfield)_
- **Contact Type** _(select)_: Normally Open (NO), Normally Closed (NC)
- **Interrupting Rating** _(select)_: 10 kA, 20 kA, 30 kA, 40 kA, 50 kA, 65 kA, 80 kA, 100 kA
- **Manufacturer** _(select)_: ABB, Eaton, General Electric, Schneider Electric, Siemens, Cummins, Kohler, Westinghouse, Generac, ACC
- Mechanism Type _(textfield)_
- Model _(textfield)_
- Notes _(textfield)_
- Serial Number _(textfield)_
- Size _(textfield)_
- Type _(textfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 480V, 600V, 4160V
- **Mains Type** _(select)_: MCB, MLO
- **QACustomField** _(select)_: May Be

### Rectifier  _(needs_source)_
- _(no core-attribute fields)_

### Relay  _(ocp, needs_source)_
- Serial Number _(textfield)_
- model _(textfield)_
- notes _(textfield)_
- Relay Type _(textfield)_
- manufacturer _(textfield)_
- **Subtypes:** Electromechanical Relay, Microprocessor Relay, Solid-State Relay

### Series Reactor  _(needs_source)_
- _(no core-attribute fields)_

### Shunt Reactor  _(needs_source)_
- _(no core-attribute fields)_

### Switch  _(ocp, needs_source)_
- _(no core-attribute fields)_

### Switchboard  _(box, needs_source)_
- Size _(textfield)_
- Notes _(textfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 277V, 347V, 380V, 400V, 415V, 480V, 600V, 120/208V, 120/240V, 208/120V, 240/120V, 277/480V, 347/600V, 230V, 400/230V, 415/240V
- **Ampere Rating** _(select)_: 15A, 20A, 30A, 40A, 50A, 60A, 70A, 80A, 90A, 100A, 125A, 150A, 175A, 200A, 225A, 250A, 300A, 350A, 400A, 450A, 500A, 600A, 700A, 800A, 900A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- **Manufacturer** _(select)_: Square D, Eaton, Siemens, General Electric, ABB, Schneider Electric, Cutler-Hammer, Westinghouse, ITE, Allen-Bradley, Federal Pacific, Zinsco, Bussmann, Challenger, Mitsubishi, Omron, Cooper, Eaton Crouse-Hinds, Fujitsu, LS Electric, Siemens BTI, ACC, Other
- Serial Number _(textfield)_
- Catalog Number _(textfield)_
- **Fault Withstand Rating** _(select)_: 10 kA, 14 kA, 18 kA, 22 kA, 25 kA, 30 kA, 35 kA, 42 kA, 50 kA, 65 kA, 85 kA, 100 kA, 150 kA, 200 kA
- **Mains Type** _(select)_: MCB, MLO
- **Subtypes:** Switchboard, Switchgear (<= 1000V), Unitized Substation (USS) (<= 1000V), Distribution Panelboard, Switchgear (> 1000V), Unitized Substation (USS) (> 1000V)

### Test  _(needs_source)_
- Catalog Number _(textfield)_
- **Contact Type** _(select)_: Normally Open (NO), Normally Closed (NC)
- **Interrupting Rating** _(select)_: 10 kA, 20 kA, 30 kA, 40 kA, 50 kA, 65 kA, 80 kA, 100 kA
- **Manufacturer** _(select)_: ABB, Eaton, General Electric, Schneider Electric, Siemens, Cummins, Kohler, Westinghouse, Generac, ACC
- **Ampere Rating** _(select)_: 30A, 60A, 100A, 200A, 400A, 600A, 800A, 1000A, 1200A, 1600A, 2000A, 2500A, 3000A, 4000A
- Mechanism Type _(textfield)_
- Model _(textfield)_
- Serial Number _(textfield)_
- Size _(textfield)_
- Type _(textfield)_
- **Voltage** _(select)_: 120V, 208V, 240V, 480V, 600V, 4160V
- **Mains Type** _(select)_: MCB, MLO
- Notes _(textfield)_

### Tie Breaker  _(needs_source)_
- _(no core-attribute fields)_

### Transformer  _(needs_source)_
- Serial Number _(textfield)_
- BIL _(textfield)_
- Size _(textfield)_
- Type _(textfield)_
- Class _(textfield)_
- **Frequency** _(select)_: 50 Hz, 60 Hz, 400 Hz
- **KVA Rating** _(select)_: 10 kVA, 25 kVA, 50 kVA, 75 kVA, 100 kVA, 150 kVA, 200 kVA, 300 kVA, 500 kVA, 750 kVA, 1000 kVA
- Primary Tap _(textfield)_
- **Manufacturer** _(select)_: ABB, Allen-Bradley, Allis-Chalmers, Baldor, Beckwith Electric, Bushing Electric, CG Power Systems, Cooper Power Systems, Cutler-Hammer, Delta Star, Eaton, Emerson Electric, Federal Pacific, Fisher Pierce, Fuji Electric, General Electric (GE), Hammond Power Solutions, Hitachi Energy, Howard Industries, Hyundai Electric, Jefferson Electric, LS Electric, Magnetek, Mitsubishi Electric, Myers Power Products, Okonite, Omicron, Omron, Parker Hannifin, Pauwels Transformers, Pioneer Transformers, Powell Electrical Systems, Powersmiths, Prolec GE, Schneider Electric, S&C Electric Company, Simpson Electric, Siemens, Solid State Controls, SPX Transformer Solutions, Square D, Tamura Corporation, Tavrida Electric, TBEA Transformers, Thomas & Betts, Toshiba, Traeger Transformer, Vizimax, Voltran Transformers, WEG Electric, Westinghouse, Zettler Electronics, Other
- Primary Amperes _(textfield)_
- **Primary Voltage** _(select)_: 120V, 208V, 240V, 277V, 480V, 600V, 2400V, 4160V, 7200V, 12, 470V, 13, 800V, 24, 000V, 34, 500V, 69, 000V
- Temperature Rise _(textfield)_
- Secondary Amperes _(textfield)_
- **Secondary Voltage** _(select)_: 120V, 208V, 240V, 277V, 480V, 600V, 2, 400V, 4, 160V, 7, 200V, 12, 470V, 13, 800V, 24, 000V, 34, 500V, 120/208V, 120/240V, 277/480V
- Percentage Impedance _(textfield)_
- **Winding Configuration** _(select)_: Delta-Wye (Δ-Y), Delta-Delta (Δ-Δ), Wye-Delta (Y-Δ), Wye-Wye (Y-Y), Zig-Zag Wye (Z-Y), Autotransformer, Open Delta (V-Connection), Open Wye
- **Subtypes:** Dry Transformer, Dry-Type Transformer (<= 600V), Dry-Type Transformer (> 600V), Oil-Filled Transformer

### Transformer (3-Winding)  _(needs_source)_
- _(no core-attribute fields)_

### UPS  _(needs_source)_
- size _(textfield)_
- model _(textfield)_
- notes _(textfield)_
- manufacturer _(textfield)_
- Catalog Number _(textfield)_
- **Mains Type** _(select)_: MCB, MLO
- **Subtypes:** Hybrid UPS System, I don't node, Rotary UPS System, Static UPS System

### Utility  _(no flags)_
- Meter Number _(textfield)_
- **Starting Voltage** _(select)_: 120V, 208V, 240V, 277V, 480V, 600V, 2400V, 4160V, 7200V, 12, 470V, 13, 800V, 24, 000V, 34, 500V, 69, 000V

### VFD  _(box, needs_source)_
- _(no core-attribute fields)_

### VFD Panel  _(box, needs_source)_
- _(no core-attribute fields)_
