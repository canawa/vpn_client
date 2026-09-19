from PIL import Image, ImageDraw
from pathlib import Path

src = Path(
    r"C:\Users\den\.cursor\projects\c-vpn-client\assets"
    r"\c__Users_den_AppData_Roaming_Cursor_User_workspaceStorage"
    r"_8936c001e0ef6d7276b6b248ed59d5b5_images_image-a86e6d45-01fd-42b7-8ae3-acc480648d80.png"
)
out_dir = Path(r"c:\vpn_client\_hush_assets")
out = out_dir / "logo_rounded.png"
res = Path(r"c:\vpn_client\android\app\src\main\res")

img = Image.open(src).convert("RGBA")
w, h = img.size
radius = int(min(w, h) * 0.20)

mask = Image.new("L", (w, h), 0)
draw = ImageDraw.Draw(mask)
draw.rounded_rectangle((0, 0, w - 1, h - 1), radius=radius, fill=255)

rounded = Image.new("RGBA", (w, h), (0, 0, 0, 0))
rounded.paste(img, (0, 0), mask=mask)
rounded.save(out, "PNG")

# In-app / notification / QS logos
for name in ("ic_logo.png", "ic_logo_notif.png", "ic_logo_qs.png"):
    rounded.save(res / "drawable-nodpi" / name, "PNG")

# Launcher densities (legacy mipmap PNG + adaptive foreground)
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
# Adaptive foreground is typically 108dp safe zone in 108dp canvas — use full logo scaled
fg_sizes = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

for folder, size in densities.items():
    icon = rounded.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(res / folder / "ic_launcher.png", "PNG")
    icon.save(res / folder / "ic_launcher_round.png", "PNG")

for folder, size in fg_sizes.items():
    fg = rounded.resize((size, size), Image.Resampling.LANCZOS)
    fg.save(res / folder / "ic_launcher_foreground.png", "PNG")
    fg.save(res / folder / "ic_launcher_round_foreground.png", "PNG")

print(f"ok size={w}x{h} radius={radius} -> {out}")
