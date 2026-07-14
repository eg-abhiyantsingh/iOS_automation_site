# 125 — required-fields toggle SOLVED: right-edge press on the row-spanning element

**Date:** 2026-07-14

The switch that swallowed click() AND center W3C presses is fixed: the
accessibility element spans the WHOLE row (label + "0/4" counter + switch),
so any center press hit dead label text. Pressing at the row's RIGHT EDGE
(x = right - max(24, height/2)) — where the switch thumb actually renders —
flips it reliably: value 'null' -> '1', ATS_EAD_06 now reads state ON,
verified live on v1.50 (42s).

General rule added to memory: for row-spanning toggle/control elements,
press the CONTROL's edge, never the row center. This likely applies to other
SwiftUI Toggle rows across the app (Session Recording, Network Mode, GF
toggle in eng-lib).
