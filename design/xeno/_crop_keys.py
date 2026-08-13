"""Crop button area from key frames for visual inspection."""
from pathlib import Path
from PIL import Image

src4 = Path(r"c:\vpn_client\design\xeno\_video_frames")
out = Path(r"c:\vpn_client\design\xeno\_video_analysis")
out.mkdir(parents=True, exist_ok=True)

# Approximate button crop from full 736x754 frames: button is upper-center
# From earlier: orbit r~250, center roughly (368, 300)
for name in ["f_001", "f_003", "f_004", "f_005", "f_008", "f_012", "f_016", "f_024", "f_032"]:
    p = src4 / f"{name}.png"
    im = Image.open(p)
    # wide crop around button
    crop = im.crop((368 - 300, 300 - 300, 368 + 300, 300 + 300))
    crop.save(out / f"crop_{name}.png")
    print("saved", name, crop.size)
