#!/usr/bin/env python3
"""Android demo for the IR Photo Type finding (app just force-stopped and relaunched). Logs epoch times."""
import sys, time, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ad

LOG = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'and-steps.txt'), 'a')


def log(msg):
    line = '%.1f  %s' % (time.time(), msg)
    print(line); LOG.write(line + '\n'); LOG.flush()


def must(txt, exact=False):
    n = ad.tapt(txt, exact, tries=8)
    if not n:
        log('NOT FOUND ' + txt); raise SystemExit(1)
    return n


def photo_type():
    ns = ad.nodes()
    labels = [ad.label(n) for n in ns]
    if 'IR Photo Type' not in labels:
        return None
    i = labels.index('IR Photo Type')
    return labels[i + 1] if i + 1 < len(labels) else '?'


def open_wo(name):
    must(name)
    time.sleep(3)
    must('Start Session', exact=True)
    time.sleep(8)


log('START app relaunched (force-stop + launch); MFA prompt')
time.sleep(4)
must('Set up later', exact=True)
time.sleep(6)
log('STEP0 dashboard')
time.sleep(2)
ad.tap(197, 1240)  # Work Orders quick action
time.sleep(5)
log('STEP1 Work Orders list; open QA-DEMO ZP-5135 (Infrared Thermography)')
open_wo('QA-DEMO ZP-5135')
log('STEP1 result: IR Photo Type = %s (server and iOS: FLIR-SEP)' % photo_type())
time.sleep(7)

log('STEP2 tap the IR Photo Type dropdown')
ad.tap(330, 1856)
time.sleep(2.5)
opts = [ad.label(n) for n in ad.nodes() if ad.label(n) in ('FLIR-IND', 'FLIR-SEP', 'FLUKE', 'FOTRIC-SEP', 'FOTRIC-IND')]
log('STEP2 result: options %s' % opts)
time.sleep(6)
ad.sh('shell', 'input', 'keyevent', 'KEYCODE_BACK')
time.sleep(2)
log('STEP2 closed without change: %s' % photo_type())
time.sleep(2)

log('STEP3 tap Refresh (top right) and wait')
must('Refresh', exact=True)
time.sleep(20)
log('STEP3 result: IR Photo Type = %s after Refresh' % photo_type())
time.sleep(6)

log('STEP4 back; open Job - Nov 2, 6:28 PM (Multiple Services: Cleaning + IR Checklist)')
must('Back', exact=True)
time.sleep(4)
open_wo('Job - Nov 2, 6:28 PM')
log('STEP4 result: IR Photo Type = %s (server: FOTRIC-SEP)' % photo_type())
time.sleep(7)

log('STEP5 back; open Work Order - Nov 3, 5:15 PM (De-Energized Visual Inspection)')
must('Back', exact=True)
time.sleep(4)
open_wo('Work Order - Nov 3, 5:15 PM')
log('STEP5 result: IR Photo Type = %s' % photo_type())
time.sleep(7)
log('END')
