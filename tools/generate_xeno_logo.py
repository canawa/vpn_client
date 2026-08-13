"""Generate XENO nested-square logo assets (replace yellow W)."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(r"c:\vpn_client")
RES = ROOT / "android" / "app" / "src" / "main" / "res"
ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets"

# Brand colors from XenoLogoMark / Figma
OUTER = (7, 82, 67, 255)       # #075243
INNER = (0, 212, 168, 255)     # #00D4A8
BG_DARK = (10, 13, 12, 255)    # #0A0D0C
TRANSPARENT = (0, 0, 0, 0)


def draw_xeno_mark(size: int, *, padding: float = 0.12, bg=TRANSPARENT) -> Image.Image:
    """Nested squares: dark outer + bright inner core."""
    img = Image.new("RGBA", (size, size), bg)
    draw = ImageDraw.Draw(img)
    pad = int(size * padding)
    outer = [pad, pad, size - pad - 1, size - pad - 1]
    draw.rectangle(outer, fill=OUTER)
    # inner is half the outer side, centered
    ow = outer[2] - outer[0] + 1
    ih = max(1, ow // 2)
    cx = (outer[0] + outer[2]) // 2
    cy = (outer[1] + outer[3]) // 2
    half = ih // 2
    inner = [cx - half, cy - half, cx - half + ih - 1, cy - half + ih - 1]
    draw.rectangle(inner, fill=INNER)
    return img


def make_mono_white(base: Image.Image, size: int) -> Image.Image:
    g = base.resize((size, size), Image.Resampling.NEAREST)
    mono = Image.new("RGBA", (size, size), TRANSPARENT)
    gp, mp = g.load(), mono.load()
    for x in range(size):
        for y in range(size):
            r, g_, b, a = gp[x, y]
            if a > 40:
                # Keep luminance so inner is brighter than outer in status bar icons
                lum = int(0.3 * r + 0.59 * g_ + 0.11 * b)
                mp[x, y] = (255, 255, 255, max(a, lum))
    return mono


def make_size(base: Image.Image, size: int, pad_ratio: float = 0.0, bg=None) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), bg if bg is not None else TRANSPARENT)
    inner = int(size * (1 - 2 * pad_ratio))
    # NEAREST keeps pixel look of the mark
    scaled = base.resize((inner, inner), Image.Resampling.NEAREST)
    off = (size - inner) // 2
    canvas.paste(scaled, (off, off), scaled)
    return canvas


# Master mark @ 1024 (sharp pixel geometry)
master = draw_xeno_mark(1024, padding=0.08, bg=TRANSPARENT)
# Also a version with black plate for legacy widgets that expect filled bg
master_black = draw_xeno_mark(1024, padding=0.18, bg=BG_DARK)

(RES / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
logo_path = RES / "drawable-nodpi" / "ic_logo.png"
master_black.save(logo_path, "PNG")
print("saved", logo_path)

# Notification / QS: white silhouette of nested squares
notif = make_mono_white(draw_xeno_mark(256, padding=0.08, bg=TRANSPARENT), 128)
notif_path = RES / "drawable-nodpi" / "ic_logo_notif.png"
notif.save(notif_path, "PNG")
print("saved", notif_path)

preview = RES / "drawable-nodpi" / "ic_logo_preview.png"
master_black.resize((256, 256), Image.Resampling.NEAREST).save(preview, "PNG")

# Optional bean leftover — keep in sync
bean = RES / "drawable-nodpi" / "ic_coffee_bean.png"
if bean.exists():
    master_black.save(bean, "PNG")
    print("replaced", bean)

densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
fg_densities = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

for dens, size in densities.items():
    d = RES / f"mipmap-{dens}"
    d.mkdir(parents=True, exist_ok=True)
    full = make_size(master, size, pad_ratio=0.12, bg=BG_DARK)
    full.save(d / "ic_launcher.png", "PNG")
    full.save(d / "ic_launcher_round.png", "PNG")

for dens, size in fg_densities.items():
    d = RES / f"mipmap-{dens}"
    fg = make_size(master, size, pad_ratio=0.18, bg=None)
    fg.save(d / "ic_launcher_foreground.png", "PNG")
    fg.save(d / "ic_launcher_round_foreground.png", "PNG")
    make_mono_white(master, size).save(d / "ic_launcher_monochrome.png", "PNG")
    Image.new("RGBA", (size, size), BG_DARK).save(d / "ic_launcher_background.png", "PNG")

print("launcher icons generated")

# SVG vector mark for Coil/ClevLogo
svg = """<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect width="24" height="24" rx="2" fill="#0A0D0C"/>
  <rect x="4" y="4" width="16" height="16" fill="#075243"/>
  <rect x="8" y="8" width="8" height="8" fill="#00D4A8"/>
</svg>
"""
ASSETS.mkdir(parents=True, exist_ok=True)
(ASSETS / "logo_mark.svg").write_text(svg, encoding="utf-8")
print("saved logo_mark.svg")
print("done")
