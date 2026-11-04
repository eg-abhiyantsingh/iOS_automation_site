#!/usr/bin/env python3
"""iOS demo for the IR Photo Type finding. Logs each step with seconds since T0 (passed as argv[1])."""
import sys, time, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ap

T0 = float(sys.argv[1])
LOG = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'ios-steps.txt'), 'a')


def log(msg):
    line = '%7.1f  %s' % (time.time() - T0, msg)
    print(line); LOG.write(line + '\n'); LOG.flush()


def find(label, exact=False):
    for n in ap.nodes():
        if (n[1] == label) if exact else (label in n[1]):
            return n
    return None


def tapl(label, exact=False, tries=6):
    for _ in range(tries):
        n = find(label, exact)
        if n:
            ap.tap(n[2] + n[4] / 2, n[3] + n[5] / 2); return n
        time.sleep(1)
    raise SystemExit('NOT FOUND ' + label)


def open_wo(name):
    tapl(name)
    time.sleep(2.5)
    tapl('Start Work Order', exact=True)
    time.sleep(6)
    ap.swipe(201, 600, 201, 300, 700)
    time.sleep(2.5)


def hide_kb():
    n = find('return', exact=True)
    if n:  # iOS 26 keyboard has no hide key; return on the empty search closes it
        ap.tap(n[2] + n[4] / 2, n[3] + n[5] / 2)
        time.sleep(1.5)


def back_to_list():
    tapl('Done', exact=True)
    time.sleep(3)
    hide_kb()


log('START on the Work Orders list')
hide_kb()
time.sleep(4)

log('STEP1 open QA-DEMO ZP-5135 (Infrared Thermography)')
open_wo('QA-DEMO ZP-5135 FLIR-SEP iOS 1.74 2 Nov, Infrared Thermography')
n = find('IR Photo Type', exact=True)
log('STEP1 result: IR Photo Type row %s' % ('SHOWN' if n else 'MISSING'))
time.sleep(6)

log('STEP2 tap IR Photo Type value (FLIR-SEP)')
tapl('FLIR-SEP', exact=True)
time.sleep(2)
opts = [x[1] for x in ap.nodes() if x[0] == 'Button' and x[1] in ('FLIR-IND', 'FLIR-SEP', 'FLUKE', 'FOTRIC-SEP', 'FOTRIC-IND')]
log('STEP2 result: options %s' % opts)
time.sleep(7)
tapl('FLIR-SEP', exact=True)  # keep the current value
time.sleep(2)
log('STEP2 closed, value kept FLIR-SEP')
time.sleep(2)

log('STEP3 open Job - Nov 2, 6:28 PM (Multiple Services: Cleaning + IR Checklist)')
back_to_list()
open_wo('Job - Nov 2, 6:28 PM, Multiple Services')
n = find('IR Photo Type', exact=True)
log('STEP3 result: IR Photo Type row %s' % ('SHOWN' if n else 'MISSING'))
time.sleep(7)

log('STEP4 open Work Order - Nov 3, 5:15 PM (De-Energized Visual Inspection, no IR)')
back_to_list()
open_wo('Work Order - Nov 3, 5:15 PM, De-Energized Visual Inspection')
n = find('IR Photo Type', exact=True)
log('STEP4 result: IR Photo Type row %s' % ('SHOWN' if n else 'NOT SHOWN'))
time.sleep(7)
log('END')
