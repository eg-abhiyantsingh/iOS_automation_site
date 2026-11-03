# Thermal camera test setup (FOTRIC stub + FLIR mock roll)

How to test the camera features of the iOS app (FOTRIC Camera, FLIR camera import, Edit IR Photo) without a
real camera in hand. Recorded 2 Nov 2026 while testing iOS 1.74: ZP-5093 (FOTRIC editor) and ZP-5135 (FLIR-SEP pairing).

## 1. FOTRIC camera stub (real device or simulator on the office Wi-Fi)

A FOTRIC camera stub runs on a desktop on the office Wi-Fi. It answers like a real FOTRIC camera with IRExplorer enabled.

| | |
|---|---|
| Shows in the app as | **Fotric stub (DESKTOP-MT0I4L0)** |
| Address | `192.168.1.128:9900` (the app scans the Wi-Fi on port 9900) |
| Camera sign-in | Username **0**, Password **0** (the camera's own sign-in, not an eGalvanic account) |
| Content | 37 capture(s) on camera (2 Nov 2026) |
| Limit | One sign-in at a time. Connecting signs out anyone else, such as IRExplorer in a browser or another phone. |

Constants (env / `-D` overridable): `AppConstants.FOTRIC_STUB_HOST`, `FOTRIC_STUB_PORT`, `FOTRIC_STUB_USERNAME`, `FOTRIC_STUB_PASSWORD`.

**Steps**
1. Create or start a work order:
   - **Work Type = Infrared Thermography**;
   - **IR Photo Type = FOTRIC-SEP** (or FOTRIC-IND).
2. Work order › Details › CAPTURE SETTINGS › **FOTRIC Camera: Not Connected** ›.
3. "Connect FOTRIC Camera" sheet:
   - under **Cameras on this network**, tap *Fotric stub (DESKTOP-MT0I4L0)*;
   - if it isn't listed, use **Scan Again** or **Enter manually** with `192.168.1.128:9900`.
4. Camera sign-in: Username `0`, Password `0`, then **Connect**. The sheet then shows "Fotric stub … 37 capture(s) on camera" and **Disconnect**.
5. The phone or the simulator's Mac must be on the same Wi-Fi (192.168.1.x). Check from the Mac with `nc -z -G 3 192.168.1.128 9900`.

## 2. FLIR camera: mock roll on the simulator (DEBUG builds only)

The simulator cannot reach a FLIR camera. DEBUG builds (Debug-QA, `Z Platform-QA.debug.dylib` inside the .app) include `MockFLIRCameraLink`, added in ZP-5135 / PR #793.
- Set **`EG_MOCK_FLIR_ROLL`** to a folder of JPEGs.
- On a FLIR work order the FLIR card then shows **"Mock FLIR E96"** as connected, and that folder becomes the camera roll.
- Only the transfer is faked. The roll sheet, Link and the import runner are the real code.

**Make a roll that behaves like an E96 "thermal + visual" camera.** The thermal source is any radiometric FLIR JPEG, e.g. `FLIRDemoRadiometric.jpg` inside the .app:
```
python3 -I scripts/flir_mock_roll.py roll "<Z Platform-QA.app>/FLIRDemoRadiometric.jpg" /tmp/flir-roll
python3 -I scripts/flir_mock_roll.py info /tmp/flir-roll/*.jpg
```

| File | Kind | Expected result |
|---|---|---|
| FLIR7373 + FLIR7374 | thermal + visual | one IR row with the camera's own visual |
| FLIR7375 + FLIR7376 | thermal + visual | one IR row with the camera's own visual |
| FLIR7377 | thermal only | IR-only row |
| FLIR7380 | visual, no IR waiting | skipped with a note |

One visual with any name (for example the visual key of a pair typed in the app, ZP-5156 finding 2):
```
python3 -I scripts/flir_mock_roll.py visual "<Z Platform-QA.app>/FLIRDemoRadiometric.jpg" /tmp/flir-roll/FLIR7422.jpg
```

The visual files carry a FLIR APP1 but no RawData record, which is exactly what the E96 writes. Before 1.74 they were imported as extra "IR: FLIR7374" rows: the customer's duplicates and "calls visual photos IR" (ZP-5135).

**Launch with the roll.** Copy the folder into the app's data container, because the app sandbox reads it from there:
```
D=$(xcrun simctl get_app_container <udid> com.egalvanic.zplatform-QA data)
mkdir -p "$D/Documents/mock-flir-roll" && cp /tmp/flir-roll/*.jpg "$D/Documents/mock-flir-roll/"
```
Then pass the env at launch:
- **Appium:** `"appium:processArguments": {"env": {"EG_MOCK_FLIR_ROLL": "<D>/Documents/mock-flir-roll"}}`;
- **without Appium:** `SIMCTL_CHILD_EG_MOCK_FLIR_ROLL=<path> xcrun simctl launch <udid> com.egalvanic.zplatform-QA`.

**Files path (manual check).** To test the Files import instead of the camera path, copy the same JPEGs into the simulator's *On My iPhone*: `<sim>/data/Containers/Shared/AppGroup/<group.com.apple.FileProvider.LocalStorage>/File Provider Storage/<folder>`. Then pick them from the work order's Files option. PR #793 says the Files and SD-card paths are unchanged by ZP-5135.

## 3. Where things are in the app (1.74)

**IR photos on an asset.** Open the asset › scroll below Maintenance Records to the **Infrared** card. It shows:
- the active WO;
- its Type (e.g. FOTRIC-IND);
- the IR rows ("IR: FOTRIC0383").

**IR row menu.** Long-press an IR row:
- with the brand's flag ON: **Edit IR Name / Edit IR Photo / Delete**;
- flags:
  - FLIR work orders use `feature-flir-web-editor` (legacy `flir-web-editor`);
  - FOTRIC work orders use `feature-fotric-editor` (legacy `fotric-editor`);
  - FLUKE never gets the editor;
  - a photo with no work order follows the FLIR flag.

**Editor.** Native for both brands, with:
- °C / °F;
- Undo, Clear All;
- Modes (Thermal, Visual, Picture-in-picture, Blending, Fusion);
- Palettes (Iron, Grey, Iron10, Rainbow, Grey10, GreyRed, MidGrey, Yellow, …);
- Measure (Spot, Circle, Rectangle, H-Line, V-Line);
- Parameters.

Save uploads the edit ("Uploading edit…", about 20–35 s for a 1 MB FOTRIC file).

**Reference values: FOTRIC0383 on QA.** WO "Job - Oct 30, 12:42 PM FOTRIC", asset "ATS FOTRIC`", site "(s) Wild Goose Brewery". iOS 1.74 and the web editor (ZP-5073) read the same values:
- spots 84.9 / 78.8 / 84.1 °F;
- Δ +6.0;
- box 79.6 / 79.2 / 79.4 °F;
- ellipse 79.4 / 78.9 / 79.0 °F;
- scale 85.5 – 78.7 °F.
