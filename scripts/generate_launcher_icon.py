"""Crop BAV shield logo and generate Android launcher assets."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path(
    r"C:\Users\den\.cursor\projects\c-vpn-client\assets"
    r"\c__Users_den_AppData_Roaming_Cursor_User_workspaceStorage_8936c001e0ef6d7276b6b248ed59d5b5_images_image-f43a5ed4-7b08-47aa-aa94-e230acd2c191.png"
)
RES = Path(r"c:\vpn_client\android\app\src\main\res")
ASSETS = Path(r"c:\vpn_client\_bav_assets")

ADAPTIVE_SIZE = 432  # 108dp @ xxxhdpi
SPLASH_SIZE = 432  # fits 192dp splash safe circle when scaled
DARK_THRESHOLD = 95
FILL_FRACTION = 0.72  # logo occupies ~72% of canvas (inside adaptive safe zone)


def luminance(r: int, g: int, b: int) -> float:
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def detect_shield_bbox(img: Image.Image) -> tuple[int, int, int, int]:
    px = img.load()
    w, h = img.size
    min_x, min_y, max_x, max_y = w, h, 0, 0
    found = False
    for y in range(h):
        for x in range(w):
            r, g, b, _a = px[x, y]
            if luminance(r, g, b) < DARK_THRESHOLD:
                found = True
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if not found:
        return 0, 0, w, h
    return min_x, min_y, max_x, max_y


def square_crop(img: Image.Image, bbox: tuple[int, int, int, int], padding_ratio: float = 0.14) -> Image.Image:
    min_x, min_y, max_x, max_y = bbox
    bw = max_x - min_x
    bh = max_y - min_y
    pad = int(max(bw, bh) * padding_ratio)
    cx = (min_x + max_x) // 2
    cy = (min_y + max_y) // 2
    half = max(bw, bh) // 2 + pad
    left = max(0, cx - half)
    top = max(0, cy - half)
    right = min(img.width, cx + half)
    bottom = min(img.height, cy + half)
    side = min(right - left, bottom - top)
    right = left + side
    bottom = top + side
    return img.crop((left, top, right, bottom))


def sample_gold(img: Image.Image) -> tuple[int, int, int]:
    px = img.load()
    w, h = img.size
    points = [
        (w // 16, h // 16),
        (w - w // 16, h // 16),
        (w // 16, h - h // 16),
        (w - w // 16, h - h // 16),
        (w // 2, h // 24),
        (w // 2, h - h // 24),
    ]
    rs = gs = bs = 0
    for x, y in points:
        r, g, b, _a = px[x, y]
        rs += r
        gs += g
        bs += b
    n = len(points)
    return rs // n, gs // n, bs // n


def extract_layers(crop: Image.Image) -> tuple[Image.Image, Image.Image]:
    rgba = crop.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    fg = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    mono = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    fg_px = fg.load()
    mono_px = mono.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if luminance(r, g, b) < DARK_THRESHOLD:
                fg_px[x, y] = (18, 18, 18, 255)
                mono_px[x, y] = (255, 255, 255, 255)
    return fg, mono


def fit_canvas(layer: Image.Image, size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    target = int(size * FILL_FRACTION)
    ratio = min(target / layer.width, target / layer.height)
    nw = max(1, int(layer.width * ratio))
    nh = max(1, int(layer.height * ratio))
    scaled = layer.resize((nw, nh), Image.Resampling.LANCZOS)
    ox = (size - nw) // 2
    oy = (size - nh) // 2
    canvas.paste(scaled, (ox, oy), scaled)
    return canvas


def main() -> None:
    img = Image.open(SRC).convert("RGBA")
    bbox = detect_shield_bbox(img)
    crop = square_crop(img, bbox)
    gold = sample_gold(img)

    fg, mono = extract_layers(crop)
    fg_canvas = fit_canvas(fg, ADAPTIVE_SIZE)
    mono_canvas = fit_canvas(mono, ADAPTIVE_SIZE)
    splash_canvas = fit_canvas(fg, SPLASH_SIZE)

    preview = Image.new("RGBA", (ADAPTIVE_SIZE, ADAPTIVE_SIZE), (*gold, 255))
    preview.alpha_composite(fg_canvas)

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)

    fg_canvas.save(nodpi / "ic_launcher_foreground.png", optimize=True)
    mono_canvas.save(nodpi / "ic_launcher_monochrome.png", optimize=True)
    splash_canvas.save(RES / "drawable" / "ic_splash_shield.png")
    preview.save(ASSETS / "09_app_icon_preview.png", optimize=True)
    crop.save(ASSETS / "09_app_icon_crop.png", optimize=True)

    colors_path = RES / "values" / "colors.xml"
    gold_hex = f"#{gold[0]:02X}{gold[1]:02X}{gold[2]:02X}"
    text = colors_path.read_text(encoding="utf-8")
    text = text.replace(
        '<color name="ic_launcher_background">@color/brand_dark</color>',
        f'<color name="ic_launcher_background">@color/brand_gold</color>',
    )
    if "brand_gold" not in text:
        text = text.replace(
            '<color name="brand_dark">#050807</color>',
            f'<color name="brand_dark">#050807</color>\n    <color name="brand_gold">{gold_hex}</color>',
        )
    colors_path.write_text(text, encoding="utf-8")

    print(f"Gold background: {gold_hex}")
    print(f"Saved launcher foreground -> {nodpi / 'ic_launcher_foreground.png'}")
    print(f"Saved launcher monochrome -> {nodpi / 'ic_launcher_monochrome.png'}")
    print(f"Saved splash icon -> {RES / 'drawable' / 'ic_splash_shield.png'}")


if __name__ == "__main__":
    main()
