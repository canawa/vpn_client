from PIL import Image
from pathlib import Path

src = Path(
    r"C:\Users\den\.cursor\projects\c-vpn-client\assets"
    r"\c__Users_den_AppData_Roaming_Cursor_User_workspaceStorage"
    r"_8936c001e0ef6d7276b6b248ed59d5b5_images_image-a86e6d45-01fd-42b7-8ae3-acc480648d80.png"
)
res = Path(r"c:\vpn_client\android\app\src\main\res")
out = Path(r"c:\vpn_client\_hush_assets\logo_launcher_inset.png")

img = Image.open(src).convert("RGB")
w, h = img.size
navy = (5, 9, 26)
scale = 0.78
nw, nh = int(w * scale), int(h * scale)
scaled = img.resize((nw, nh), Image.Resampling.LANCZOS)
canvas = Image.new("RGB", (w, h), navy)
canvas.paste(scaled, ((w - nw) // 2, (h - nh) // 2))
fully = canvas.convert("RGBA")
fully.save(out, "PNG")

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
    icon = fully.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(res / folder / "ic_launcher.png", "PNG")
    icon.save(res / folder / "ic_launcher_round.png", "PNG")
for folder, size in fg_sizes.items():
    fg = fully.resize((size, size), Image.Resampling.LANCZOS)
    fg.save(res / folder / "ic_launcher_foreground.png", "PNG")
    fg.save(res / folder / "ic_launcher_round_foreground.png", "PNG")

(res / "drawable" / "ic_launcher_foreground_inset.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@mipmap/ic_launcher_foreground"
    android:inset="12%" />
""",
    encoding="utf-8",
)
print(f"ok scale={scale} inset=12%")
