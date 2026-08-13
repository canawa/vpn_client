import imageio_ffmpeg, subprocess
from pathlib import Path

src = r"c:\Users\den\Downloads\video_2026-08-12_10-59-24.mp4"
out = Path(r"c:\vpn_client\design\xeno\_video_frames10")
out.mkdir(parents=True, exist_ok=True)
for p in out.glob("*.png"):
    p.unlink()
ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
cmd = [ffmpeg, "-y", "-i", src, "-vf", "fps=10", str(out / "t_%03d.png")]
r = subprocess.run(cmd, capture_output=True, text=True)
print("exit", r.returncode)
print("n", len(list(out.glob("*.png"))))
print(r.stderr[-800:])
