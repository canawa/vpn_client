import imageio_ffmpeg
import subprocess
from pathlib import Path

src = r"c:\Users\den\Downloads\video_2026-08-12_10-59-24.mp4"
out = Path(r"c:\vpn_client\design\xeno\_video_frames")
out.mkdir(parents=True, exist_ok=True)
# clear old
for p in out.glob("*.png"):
    p.unlink()

ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
print("ffmpeg:", ffmpeg)

# Probe
probe = subprocess.run(
    [ffmpeg, "-i", src],
    capture_output=True,
    text=True,
)
print(probe.stderr)

# Extract ~4 fps (~33 frames over 8.2s) — enough for motion analysis
# Also extract at 10 fps for speed measurement later if needed
cmd = [
    ffmpeg, "-y",
    "-i", src,
    "-vf", "fps=4",
    str(out / "f_%03d.png"),
]
r = subprocess.run(cmd, capture_output=True, text=True)
print(r.stderr[-2000:] if r.stderr else "")
print("exit", r.returncode)
print("files", sorted(p.name for p in out.glob("*.png")))
