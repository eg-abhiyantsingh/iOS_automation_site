# 123 — demo smoke first run: 20/22, and the canary caught a real candidate app bug

**Date:** 2026-07-14

- Demo smoke run 29312618703: 20/22 green in ~11 min.
- **TC_ENG_130 red = real finding, not test failure**: backend flag ON
  (live-verified) while the fresh-install app rendered the Equipment Library
  DISABLED → filed **ENG-FLAG-DRIFT-01** in BUGS.md (suspected: silent
  degradation when the login-time company_features fetch fails/races; no
  retry). The canary doing precisely its job — smoke reds that are product
  findings are what smoke is for.
- **TC_ENG_004 red = suite-design gap, fixed**: it asserts the downloaded
  state, which a fresh sim doesn't have. Added TC_ENG_003 (the download —
  the actual critical equipment journey) before it. Suite is now 23 tests,
  ~13 min.
