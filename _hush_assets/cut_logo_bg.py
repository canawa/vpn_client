"""Remove dark navy background from HUSH logo; keep mark + glow; round corners."""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

src = Path(
    r"C:\Users\den\.cursor\projects\c-vpn-client\assets"
    r"\c__Users_den_AppData_Roaming_Cursor_User_workspaceStorage"
    r"_8936c001e0ef6d7276b6b248ed59d5b5_images_image-a86e6d45-01fd-42b7-8ae3-acc480648d80.png"
)
out_dir = Path(r"c:\vpn_client\_hush_assets")
res = Path(r"c:\vpn_client\android\app\src\main\res")

img = Image.open(src).convert("RGBA")
arr = np.asarray(img).astype(np.float32)
r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]

# Background reference (site / logo navy)
bg = np.array([0.0, 12.0, 26.0], dtype=np.float32)
dist = np.sqrt((r - bg[0]) ** 2 + (g - bg[1]) ** 2 + (b - bg[2]) ** 2)

# Soft matte: fully transparent near bg, keep glow/text/shield
# Tune: opaque when far from bg, transparent when close
lo, hi = 18.0, 55.0
matte = ((dist - lo) / (hi - lo)).clip(0.0, 1.0)

# Also kill very dark low-saturation leftovers that aren't neon
luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
sat = arr[:, :, :3].max(axis=2) - arr[:, :, :3].min(axis=2)
dark_flat = (luma < 28.0) & (sat < 22.0)
matte = np.where(dark_flat, 0.0, matte)

out = arr.copy()
out[:, :, 3] = (a * matte).clip(0, 255)

cut = Image.fromarray(out.astype(np.uint8), "RGBA")
# Slight blur on alpha only for cleaner edges
rgb = cut.convert("RGB")
alpha = cut.getchannel("A").filter(ImageFilter.GaussianBlur(radius=0.8))
cut = Image.merge("RGBA", (*rgb.split(), alpha))

# Round corners on the transparent canvas
w, h = cut.size
radius = int(min(w, h) * 0.20)
mask = Image.new("L", (w, h), 0)
ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=radius, fill=255)
rounded = Image.new("RGBA", (w, h), (0, 0, 0, 0))
rounded.paste(cut, (0, 0), mask=mask)

rounded_path = out_dir / "logo_nobg_rounded.png"
rounded.save(rounded_path, "PNG")

for name in ("ic_logo.png", "ic_logo_notif.png", "ic_logo_qs.png"):
    rounded.save(res / "drawable-nodpi" / name, "PNG")

# Launcher: put mark on navy plate (system may mask), keep readable on home screen
plate = Image.new("RGBA", (w, h), (5, 9, 26, 255))
plate.paste(rounded, (0, 0), rounded)
# re-apply round mask so plate corners are round too
plate_rounded = Image.new("RGBA", (w, h), (0, 0, 0, 0))
plate_rounded.paste(plate, (0, 0), mask=mask)

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
fg_sizes = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}
for folder, size in densities.items():
    icon = plate_rounded.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(res / folder / "ic_launcher.png", "PNG")
    icon.save(res / folder / "ic_launcher_round.png", "PNG")
for folder, size in fg_sizes.items():
    # Adaptive FG: transparent bg mark (OS composites on background color)
    fg = rounded.resize((size, size), Image.Resampling.LANCZOS)
    fg.save(res / folder / "ic_launcher_foreground.png", "PNG")
    fg.save(res / folder / "ic_launcher_round_foreground.png", "PNG")

alpha_arr = np.asarray(rounded)[:, :, 3]
print(
    f"saved {rounded_path} opaque={(alpha_arr > 8).mean():.3f} "
    f"corner={tuple(np.asarray(rounded)[0, 0])}"
)
