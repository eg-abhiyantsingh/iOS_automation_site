# build.py spec.json in.mp4 out.mp4 overlaydir -> scales the video to spec W, pads a caption band on top, overlays seg-NN.png per [a,b)
import json, subprocess, sys
spec = json.load(open(sys.argv[1])); src, out, od = sys.argv[2], sys.argv[3], sys.argv[4]
W, H, band = spec['W'], spec['H'], spec['band']
inputs = ['-i', src]
for i, _ in enumerate(spec['segments']): inputs += ['-i', f'{od}/seg-{i:02d}.png']
f = [f'[0:v]fps=24,scale={W}:{H},pad={W}:{H + band}:0:{band}:white[v0]']
for i, s in enumerate(spec['segments']):
    f.append(f"[v{i}][{i + 1}:v]overlay=0:0:enable='between(t,{s['a']},{s['b']})'[v{i + 1}]")
cmd = ['ffmpeg', '-v', 'error', '-y'] + inputs + ['-filter_complex', ';'.join(f), '-map', f'[v{len(spec["segments"])}]', '-c:v', 'libx264', '-crf', '27', '-preset', 'veryfast', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', '-an', out]
subprocess.run(cmd, check=True); print('wrote', out)
