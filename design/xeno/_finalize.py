"""Confirm status text timeline and finalize tracer fade curve."""
from pathlib import Path
import numpy as np
from PIL import Image

frames = Path(r"c:\vpn_client\design\xeno\_video_frames10")
out = Path(r"c:\vpn_client\design\xeno\_video_analysis")

# Status text is in lower third - crop and save a few
for idx in [1, 8, 12, 20, 40]:
    im = Image.open(frames / f"t_{idx:03d}.png")
    # lower text region
    w,h = im.size
    crop = im.crop((80, int(h*0.68), w-80, int(h*0.92)))
    crop.save(out / f"status_t{idx:03d}.png")

# Build fade curve from measured profile for one tracer (head at 55)
profile_peaks = {
    55: 205.9,
    52: 117.7,
    49: 105.9,
    46: 89.7,
    43: 78.5,
    40: 68.2,
    37: 58.5,
    34: 49.3,
    31: 40.4,
    28: 33.5,
    25: 26.5,
    22: 20.4,
}
base = 12.7
head = 205.9
print("Normalized alpha from head (subtract base, / (head-base)):")
items = sorted(profile_peaks.items(), reverse=True)
for i,(ang,g) in enumerate(items):
    a = (g-base)/(head-base)
    steps_back = (55-ang)/3  # if 3 deg pitch
    print(f"  step={steps_back:.0f} ang={ang} rawG={g:.0f} alpha~{a:.2f}")

# Speed check: peak0 from t=1.5 to t=3.5 should +90
print("\nSpeed check selected peaks:")
# from analysis already done
pairs = [(1.5,30),(2.0,53),(2.5,165%90),(3.5,32),(5.5,35)]
# 1.5->3.5: 30->32 is +90 visually (30+90=120 ~122)
print("t1.5 peak0=30; t3.5 peak0=32 (+90 expected -> 120, nearest peak 122)")
print("t1.5->t5.5: +180 expected -> 210, got peaks 35,125,214,304 (214~210)")
print("period = 8.0s")
