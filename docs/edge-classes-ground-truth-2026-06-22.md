# Edge (Connection) Classes — Live Ground Truth

**Source:** `GET /api/edge_classes/user/{id}` (12 classes; **3 real** + 9 junk `DEVTOOL_TEST EdgeClass Updated`), 2026-06-22, vs `edge_classes_template (3).xlsx`.

These are the **Connection Type** dropdown options. The iOS Connection-edit form swaps its CORE ATTRIBUTES fields per type.

## Busway (7 fields)
- **Conductor Material** _(select)_: Copper, Aluminum
- Length (ft) _(number)_
- **Neutral Wire Size** _(select)_: 14 AWG, 12 AWG, 10 AWG, 8 AWG, 6 AWG, 4 AWG, 3 AWG, 2 AWG, 1/0 AWG, 2/0 AWG, 3/0 AWG, 4/0 AWG, 250 KcMIL, 300 KcMIL, 350 KcMIL, 400 KcMIL, 500 KcMIL, 600 KcMIL, 700 KcMIL, 800 KcMIL, 900 KcMIL, 1000 KcMIL, 1250 KcMIL, 1500 KcMIL, 1750 KcMIL, 2000 KcMIL, N/A
- **Amperage of Busway** _(select)_: 100A, 200A, 400A, 600A, 800A, 1000A, 1200A, 1500A, 2000A, 3000A
- **Phase B Wire Size** _(select)_: 14 AWG, 12 AWG, 10 AWG, 8 AWG, 6 AWG, 4 AWG, 3 AWG, 2 AWG, 1/0 AWG, 2/0 AWG, 3/0 AWG, 4/0 AWG, 250 KcMIL, 300 KcMIL, 350 KcMIL, 400 KcMIL, 500 KcMIL, 600 KcMIL, 700 KcMIL, 800 KcMIL, 900 KcMIL, 1000 KcMIL, 1250 KcMIL, 1500 KcMIL, 1750 KcMIL, 2000 KcMIL, N/A
- **Phase C Wire Size** _(select)_: 14 AWG, 12 AWG, 10 AWG, 8 AWG, 6 AWG, 4 AWG, 3 AWG, 2 AWG, 1/0 AWG, 2/0 AWG, 3/0 AWG, 4/0 AWG, 250 KcMIL, 300 KcMIL, 350 KcMIL, 400 KcMIL, 500 KcMIL, 600 KcMIL, 700 KcMIL, 800 KcMIL, 900 KcMIL, 1000 KcMIL, 1250 KcMIL, 1500 KcMIL, 1750 KcMIL, 2000 KcMIL, N/A
- **Phase A Wire Size** _(select)_: 14 AWG, 12 AWG, 10 AWG, 8 AWG, 6 AWG, 4 AWG, 3 AWG, 2 AWG, 1/0 AWG, 2/0 AWG, 3/0 AWG, 4/0 AWG, 250 KcMIL, 300 KcMIL, 350 KcMIL, 400 KcMIL, 500 KcMIL, 600 KcMIL, 700 KcMIL, 800 KcMIL, 900 KcMIL, 1000 KcMIL, 1250 KcMIL, 1500 KcMIL, 1750 KcMIL, 2000 KcMIL, N/A

## Cable (9 fields)
- Length (ft) _(number)_
- **Conductors Description** _(select)_: 2-1/C+G, 3-1/C+G, 4-1/C+G, 3 MC, 4 MC, 5 MC
- Parallel Sets _(number)_
- **Conductor Material** _(select)_: Copper, Aluminum
- **Wire Size - H** _(select)_: 14 AWG, 12 AWG, 10 AWG, 8 AWG, 6 AWG, 4 AWG, 3 AWG, 2 AWG, 1 AWG, 1/0 AWG, 2/0 AWG, 3/0 AWG, 4/0 AWG, 250 kcmil, 300 kcmil, 350 kcmil, 400 kcmil, 500 kcmil, 600 kcmil, 700 kcmil, 750 kcmil, 800 kcmil, 900 kcmil, 1000 kcmil, 1250 kcmil, 1500 kcmil, 2000 kcmil
- Diameter (inches) _(number)_
- **Raceway Material** _(select)_: Metallic, Non-Metallic
- Comments _(textfield)_
- Notes _(textfield)_

## DC Cable (0 fields)
- _(no edge-property fields live)_

> Casing quirk: Busway wire sizes use `KcMIL`, Cable uses `kcmil` — tests should match case-insensitively.