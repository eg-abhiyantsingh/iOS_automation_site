# 124 — v1.50: "Core Attributes" renamed to "Custom Attributes" — probes widened

**Date:** 2026-07-14

- User screenshots of v1.50 revealed the section rename (would have broken
  every Core-Attributes-titled probe in the next nightly). All AssetPage +
  ConnectionsPage locators now accept BOTH titles ('Core Attributes',
  'CORE ATTRIBUTES', 'Custom Attributes', 'CUSTOM ATTRIBUTES').
- Required-fields toggle: app-side WORKS (screenshot: ON filters to the 4
  required fields, counter "0/4" in the toggle row) but resists BOTH click()
  and center W3C press programmatically (value stays '0'). No current test
  depends on the filter engaging (state = informational per the domain rule);
  thumb-coordinate local-loop follow-up queued if one ever does.
- New v1.50 Commercial section noted (Condition of Maintenance + Calculator,
  Suggested PM Plan, Replacement Cost) — candidate coverage for a future COM
  expansion.
- Local validation on v1.50: canary 14s green; ATS_EAD_06 green.
