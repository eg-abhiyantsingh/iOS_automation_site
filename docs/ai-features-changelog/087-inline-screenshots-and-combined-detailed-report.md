# 087 — Inline screenshots in detailed reports + single combined report

Two report requests:

## 1. Screenshots render as DIRECT images (not a "base64 img" badge)
ExtentReports' Spark theme renders a `MediaEntityBuilder` base64 screenshot as a clickable
**"base64 img" badge**, not the picture. Fixed `ExtentReportManager` to embed each screenshot
as an inline `<img src="data:image/jpeg;base64,…">` directly in the log message (Spark renders
HTML in log text), so the screenshot shows **directly inline**, click-to-zoom. New helper
`inlineImgHtml(base64)` (mime auto-detected: JPEG default, PNG via the `iVBOR` signature)
replaces all 4 `MediaEntityBuilder` call sites (initial-state, logStepWithScreenshot,
logStepWithBase64Screenshot, logFailWithScreenshot). `MediaEntityBuilder` import removed.

**Validated:** a freshly-generated report has 6 inline `data:image` `<img>` tags and **0**
"base64 img" badges (`reports/detailed/…`). Applies to every newly-generated report (CI + local).

## 2. Combine the per-module detailed reports into ONE HTML
New `scripts/combine_detailed_reports.py` merges the per-module `*Detailed_Report*.html`
(e.g. the `consolidated-detailed-reports/` set) into a **single self-contained file** with a
left-sidebar module switcher. Each module's full ExtentReport (its own CSS/JS + the inline
screenshots) is embedded in an isolated `<iframe>` so there's no style bleed. Iframes are
**lazy** (markup in a `data-srcdoc` attribute, moved to `srcdoc` only when the tab is clicked)
so the browser renders just one module at a time — essential because the combined file is
large (every screenshot is embedded; the live set was ~420 MB / 12 modules). Modules are
ordered Auth → Site → Assets 1-6 → Issues → Connections → Location → Offline → Site Visit →
Work Order (word-boundary match so "sitevisit" doesn't sort as "site").

Usage:
```
python3 scripts/combine_detailed_reports.py consolidated-detailed-reports/ [Combined.html]
```

Note: the combined file inlines whatever the per-module reports contain — re-run the suites
(or the next CI run, which now uses the inline-`<img>` ExtentReportManager) so the combined
report shows direct images throughout rather than the old badges.
