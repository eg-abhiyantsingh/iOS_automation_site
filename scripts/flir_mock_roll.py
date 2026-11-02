#!/usr/bin/env python3
"""Build a fake FLIR E96 camera roll for the iOS simulator (no real camera needed).

Why: a FLIR E96 set to "thermal + visual" saves the visual as its OWN JPEG, numbered one after the
thermal frame (FLIR7373 thermal, FLIR7374 visual). That visual JPEG also carries a FLIR APP1 segment,
so a signature-only check (FLIRRadiometricValidator.isRadiometric) wrongly treats it as an IR photo.
Since iOS 1.74 (ZP-5135, PR #793) the app tells them apart by the FFF record directory: only a thermal
frame has a RawData record (type 1). This script makes visual files the same way the camera does:
camera's embedded visual photo + the thermal file's FLIR APP1 with the RawData record blanked.

Usage
  python3 -I scripts/flir_mock_roll.py info  <jpg> [...]          # record types, thermal yes/no
  python3 -I scripts/flir_mock_roll.py roll  <thermal.jpg> <outdir>  # FLIR7373..7380 roll (see ROLL)
The thermal source can be the FLIRDemoRadiometric.jpg shipped inside every Z Platform-QA.app.

Then launch a DEBUG build with the roll as its mock camera (MockFLIRCameraLink):
  Appium caps: "appium:processArguments": {"env": {"EG_MOCK_FLIR_ROLL": "<folder inside the app's
  data container, e.g. .../Documents/mock-flir-roll>"}}
  or: SIMCTL_CHILD_EG_MOCK_FLIR_ROLL=<folder> xcrun simctl launch <udid> com.egalvanic.zplatform-QA
On a FLIR-SEP / FLIR-IND work order the FLIR card then shows "Mock FLIR E96" as connected.
"""
import os
import struct
import sys


def segments(b):
    assert b[:2] == b'\xff\xd8', 'not a JPEG'
    i, out = 2, []
    while i + 4 <= len(b):
        if b[i] != 0xFF:
            i += 1
            continue
        m = b[i + 1]
        if m == 0xDA:
            break
        if m in (0xD8, 0xD9, 0x01) or 0xD0 <= m <= 0xD7:
            i += 2
            continue
        ln = struct.unpack('>H', b[i + 2:i + 4])[0]
        out.append((m, i, ln))
        i += 2 + ln
    return out


def flir_chunks(b):
    return [(i, ln) for m, i, ln in segments(b) if m == 0xE1 and b[i + 4:i + 9] == b'FLIR\x00']


def fff_stream(b):
    """FFF bytes (FLIR APP1 payloads past their 8-byte chunk headers) + map back to file offsets."""
    data, where = bytearray(), []
    for i, ln in flir_chunks(b):
        start, end = i + 4 + 8, i + 2 + ln
        where.append((len(data), start, end - start))
        data += b[start:end]
    return bytes(data), where


def to_file(where, off):
    for fo, start, n in where:
        if fo <= off < fo + n:
            return start + (off - fo)
    raise ValueError(off)


def records(fff):
    if fff[:3] != b'FFF':
        return None
    d = struct.unpack('>I', fff[0x18:0x1C])[0]
    c = struct.unpack('>I', fff[0x1C:0x20])[0]
    return [(d + k * 32, fff[d + k * 32:d + k * 32 + 32]) for k in range(c)]


def info(path):
    b = open(path, 'rb').read()
    ch = flir_chunks(b)
    if not ch:
        return 'no FLIR APP1 (plain JPEG)'
    fff, _ = fff_stream(b)
    r = records(fff)
    if r is None:
        return 'FLIR APP1, FFF unreadable'
    t = sorted({struct.unpack('>H', e[:2])[0] for _, e in r} - {0})
    return 'FLIR APP1 chunks=%d types=%s -> %s' % (len(ch), ','.join(map(str, t)), 'THERMAL' if 1 in t else 'VISUAL')


def embedded_visual(thermal_bytes):
    """The camera's own visual photo (FFF record 14) as JPEG/PNG bytes."""
    fff, _ = fff_stream(thermal_bytes)
    for _, e in records(fff):
        if struct.unpack('>H', e[:2])[0] != 14:
            continue
        off, ln = struct.unpack('>I', e[12:16])[0], struct.unpack('>I', e[16:20])[0]
        rec = fff[off:off + ln]
        p = rec.find(b'\xff\xd8\xff')
        if p >= 0:
            return rec[p:]
    raise SystemExit('thermal file has no embedded JPEG visual (record 14)')


def visual_from(thermal_bytes):
    """E96-style visual: embedded visual JPEG + the thermal FLIR APP1 with RawData (type 1) blanked."""
    b = bytearray(thermal_bytes)
    fff, where = fff_stream(bytes(b))
    for off, e in records(fff):
        if struct.unpack('>H', e[:2])[0] == 1:
            b[to_file(where, off)] = 0
            b[to_file(where, off + 1)] = 0
    flir = b''.join(bytes(b[i:i + 2 + ln]) for i, ln in flir_chunks(bytes(b)))
    base = embedded_visual(thermal_bytes)
    assert not flir_chunks(base)
    return base[:2] + flir + base[2:]


# name -> what it is. A pair = thermal n + visual n+1; 7377 = IR-only capture; 7380 = visual with no IR.
ROLL = [('FLIR7373.jpg', 'thermal'), ('FLIR7374.jpg', 'visual'),
        ('FLIR7375.jpg', 'thermal'), ('FLIR7376.jpg', 'visual'),
        ('FLIR7377.jpg', 'thermal'), ('FLIR7380.jpg', 'visual')]


def roll(thermal, outdir):
    t = open(thermal, 'rb').read()
    v = visual_from(t)
    os.makedirs(outdir, exist_ok=True)
    for name, kind in ROLL:
        p = os.path.join(outdir, name)
        open(p, 'wb').write(t if kind == 'thermal' else v)
        print(name, '->', info(p))


if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    if sys.argv[1] == 'info':
        for p in sys.argv[2:]:
            print(os.path.basename(p), '->', info(p))
    elif sys.argv[1] == 'roll':
        roll(sys.argv[2], sys.argv[3])
    else:
        raise SystemExit(__doc__)
