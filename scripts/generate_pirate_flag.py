"""Generate a flagcdn-style Jolly Roger (4:3) PNG for Android."""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(r"c:\vpn_client\android\app\src\main\res\drawable-nodpi\flag_pirate.png")
W, H = 320, 240  # 4:3, crisp on xxhdpi+


def main() -> None:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Fabric-like black field with subtle vignette
    for y in range(H):
        for x in range(W):
            # soft wave in luminance
            wave = 8 * math.sin((x + y * 0.4) / 28.0) + 6 * math.sin(y / 18.0)
            edge = min(x, y, W - 1 - x, H - 1 - y)
            vignette = max(0, 18 - edge) * 1.2
            v = int(18 + wave - vignette)
            v = max(8, min(34, v))
            img.putpixel((x, y), (v, v, v, 255))

    # Slight cloth grain
    grain = Image.effect_noise((W, H), 12).convert("L")
    base = img.convert("RGBA")
    px = base.load()
    gp = grain.load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = px[x, y]
            n = (gp[x, y] - 128) // 18
            px[x, y] = (
                max(0, min(255, r + n)),
                max(0, min(255, g + n)),
                max(0, min(255, b + n)),
                a,
            )
    img = base
    draw = ImageDraw.Draw(img)

    cx, cy = W // 2, int(H * 0.42)
    white = (245, 245, 245, 255)
    soft = (220, 220, 220, 255)

    # Crossbones (behind skull)
    bone(draw, cx - 62, cy + 38, cx + 62, cy + 92, 11, white)
    bone(draw, cx + 62, cy + 38, cx - 62, cy + 92, 11, white)

    # Skull
    skull_w, skull_h = 78, 86
    left = cx - skull_w // 2
    top = cy - skull_h // 2 - 6
    draw.ellipse([left, top, left + skull_w, top + int(skull_h * 0.78)], fill=white)
    # jaw
    jaw_top = top + int(skull_h * 0.55)
    draw.rounded_rectangle(
        [left + 16, jaw_top, left + skull_w - 16, top + skull_h],
        radius=10,
        fill=white,
    )
    # eye sockets
    eye_y = top + 28
    draw.ellipse([left + 14, eye_y, left + 34, eye_y + 24], fill=(12, 12, 12, 255))
    draw.ellipse([left + skull_w - 34, eye_y, left + skull_w - 14, eye_y + 24], fill=(12, 12, 12, 255))
    # nasal cavity
    nose = [
        (cx, top + 48),
        (cx - 8, top + 62),
        (cx + 8, top + 62),
    ]
    draw.polygon(nose, fill=(12, 12, 12, 255))
    # teeth lines
    teeth_y0 = top + int(skull_h * 0.72)
    teeth_y1 = top + int(skull_h * 0.88)
    for tx in (cx - 14, cx, cx + 14):
        draw.line([(tx, teeth_y0), (tx, teeth_y1)], fill=(40, 40, 40, 255), width=2)
    draw.line(
        [(left + 22, teeth_y0), (left + skull_w - 22, teeth_y0)],
        fill=(40, 40, 40, 255),
        width=2,
    )

    # Soft highlight on skull
    highlight = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(highlight)
    hd.ellipse([left + 10, top + 6, left + 42, top + 28], fill=(255, 255, 255, 55))
    highlight = highlight.filter(ImageFilter.GaussianBlur(4))
    img = Image.alpha_composite(img, highlight)

    # Thin border like flag photos
    border = ImageDraw.Draw(img)
    border.rectangle([0, 0, W - 1, H - 1], outline=(0, 0, 0, 40))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, optimize=True)
    print(f"saved {OUT} {img.size}")


def bone(draw: ImageDraw.ImageDraw, x0: int, y0: int, x1: int, y1: int, thickness: int, color) -> None:
    draw.line([(x0, y0), (x1, y1)], fill=color, width=thickness)
    r = thickness + 2
    draw.ellipse([x0 - r, y0 - r, x0 + r, y0 + r], fill=color)
    draw.ellipse([x1 - r, y1 - r, x1 + r, y1 + r], fill=color)
    # knuckle split
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy) or 1
    nx, ny = -dy / length, dx / length
    for px, py in ((x0, y0), (x1, y1)):
        draw.ellipse(
            [px - r + nx * 4, py - r + ny * 4, px + r + nx * 4, py + r + ny * 4],
            fill=color,
        )
        draw.ellipse(
            [px - r - nx * 4, py - r - ny * 4, px + r - nx * 4, py + r - ny * 4],
            fill=color,
        )


if __name__ == "__main__":
    main()
