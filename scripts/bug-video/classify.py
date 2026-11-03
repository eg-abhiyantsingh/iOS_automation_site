# classify.py video.mp4 W H ref_t_view ref_t_all : per 0.25 s state of a SLD demo video (no PIL; raw RGB via ffmpeg)
#   badge colour (orange = warning 1 = correct view, red = bug count), title (view name vs "All Assets"), dim (sheet open)
import subprocess, sys
v, W, H = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
FPS = 4
p = subprocess.run(['ffmpeg', '-v', 'error', '-i', v, '-vf', f'fps={FPS}', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'], capture_output=True, check=True)
buf = p.stdout; fs = W * H * 3; n = len(buf) // fs
def px(f, x, y): o = f * fs + (y * W + x) * 3; return buf[o], buf[o + 1], buf[o + 2]
def region(f, x0, y0, x1, y1, step=2):
    for y in range(y0, y1, step):
        for x in range(x0, x1, step): yield px(f, x, y)
def badge(f):
    red = orange = 0
    for r, g, b in region(f, 505, 178, 575, 212):
        if r > 190 and g < 90 and b < 90: red += 1
        elif r > 200 and 110 < g < 190 and b < 90: orange += 1
    return red, orange
def title_sig(f): return [sum(c) // 3 for c in region(f, 170, 100, 420, 140, 3)]
def dim(f): s = [sum(c) / 3 for c in region(f, 20, 330, 570, 640, 12)]; return sum(s) / len(s)
rv, ra = int(float(sys.argv[4]) * FPS), int(float(sys.argv[5]) * FPS)
refV, refA = title_sig(rv), title_sig(ra)
def d(a, b): return sum(abs(x - y) for x, y in zip(a, b)) / len(a)
out = []
for f in range(n):
    rd, og = badge(f); t = title_sig(f); dm = dim(f)
    dv, da = d(t, refV), d(t, refA)
    title = 'VIEW' if dv < 6 and dv < da else ('ALL' if da < 6 else 'OTHER')
    st = 'DIM' if dm < 200 else (('BUG' if rd > og else 'OK' if og > rd else 'NOBADGE') if title == 'VIEW' else title)
    out.append(st)
runs = []; s0 = 0
for i in range(1, n + 1):
    if i == n or out[i] != out[s0]: runs.append((s0 / FPS, i / FPS, out[s0])); s0 = i
for a, b, s in runs:
    if b - a >= 0.5: print(f'{a:7.2f} {b:7.2f} {s}')
