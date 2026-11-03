# vfr2cfr.py raw.mp4 out.mp4 workdir W H : rebuild a simulator recording at 24 fps using each frame's real display time
import subprocess, sys, os, glob
raw, out, wd, W, H = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]
# optional holds 't1:+s1,t2:+s2' = freeze the frame showing at output time t for s extra seconds (times before earlier holds)
holds = sorted((float(a), float(b)) for a, b in (h.split(':+') for h in sys.argv[6].split(','))) if len(sys.argv) > 6 and sys.argv[6] else []
os.makedirs(wd, exist_ok=True)
for f in glob.glob(wd + '/*.png'): os.remove(f)
p = subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'v', '-show_entries', 'frame=pts_time', '-of', 'csv=p=0', raw], capture_output=True, text=True, check=True)
ts = [float(x.strip().strip(',')) for x in p.stdout.split() if x.strip().strip(',')]
dur = float(subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', raw], capture_output=True, text=True).stdout)
subprocess.run(['ffmpeg', '-v', 'fatal', '-y', '-i', raw, '-vf', f'scale={W}:{H},setpts=N/(24*TB)', '-fps_mode', 'passthrough', '-c:v', 'png', '-compression_level', '1', '-f', 'image2', wd + '/%05d.png'])
n = len(glob.glob(wd + '/*.png'))
order = sorted(range(len(ts)), key=lambda i: ts[i])
nonmono = sum(1 for i in range(1, len(ts)) if ts[i] < ts[i - 1])
print('frames', n, 'timestamps', len(ts), 'non-monotonic', nonmono, 'first', ts[0], 'last', max(ts), 'duration', dur)
m = min(n, len(ts))
order = sorted(range(m), key=lambda i: ts[i])
lines = []; cum = 0.0; hi = 0
for k, i in enumerate(order):
    nxt = ts[order[k + 1]] if k + 1 < len(order) else dur
    d = max(nxt - ts[i], 0.001)
    if k == 0 and ts[i] > 0: d += ts[i]
    while hi < len(holds) and cum + d > holds[hi][0]: d += holds[hi][1]; print('hold', holds[hi], 'frame', i + 1); hi += 1
    cum += d
    lines.append(f"file '{wd}/{i + 1:05d}.png'\nduration {d:.6f}")
lines.append(f"file '{wd}/{order[-1] + 1:05d}.png'")
open(wd + '/list.txt', 'w').write('\n'.join(lines) + '\n')
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'concat', '-safe', '0', '-i', wd + '/list.txt', '-fps_mode', 'cfr', '-r', '24', '-c:v', 'libx264', '-crf', '20', '-preset', 'veryfast', '-pix_fmt', 'yuv420p', '-an', out], check=True)
print('wrote', out, subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', out], capture_output=True, text=True).stdout.strip())
