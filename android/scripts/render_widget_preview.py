import math
from pathlib import Path

from PIL import Image, ImageDraw

REF_PLATE = 188.0
REF_ORBIT_GAP = 22.0
REF_SQUARE = 6.0
REF_SQUARE_RADIUS = 1.0
REF_ICON = 56.0
REF_STROKE = 4.5
REF_STEM_H = 18.0
REF_GAP_DEG = 72.0
MAX_DOT_SCALE = 1.0
BUTTON_TOTAL_DP = 56.0
SUPER_SAMPLE = 2.0
DENSITY = 3.0


def total_dp_for_plate(plate_dp: float) -> float:
    gap = plate_dp * (REF_ORBIT_GAP / REF_PLATE)
    square = plate_dp * (REF_SQUARE / REF_PLATE)
    return plate_dp + 2.0 * gap + square * (1.0 + MAX_DOT_SCALE)


def main() -> None:
    effective_plate_dp = BUTTON_TOTAL_DP / total_dp_for_plate(1.0)
    px = DENSITY * SUPER_SAMPLE
    plate = effective_plate_dp * px
    square = REF_SQUARE / REF_PLATE * plate
    orbit_gap = REF_ORBIT_GAP / REF_PLATE * plate
    max_extent = plate / 2.0 + orbit_gap + square * (1.0 + MAX_DOT_SCALE) / 2.0
    pad = 1.5 * SUPER_SAMPLE
    size = max(1, int((max_extent + pad) * 2.0))

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = size / 2.0
    cy = size / 2.0

    accent = (0x6B, 0x76, 0x72, 255)
    border = (0x22, 0x2B, 0x28, 255)
    plate_color = (0x14, 0x1B, 0x18, 255)
    track_idle = (0x56, 0x64, 0x60, int(0.45 * 255))
    unit = plate / REF_PLATE
    border_width = 1.5 * unit

    corner = REF_SQUARE_RADIUS / REF_SQUARE * square
    orbit = plate / 2.0 + orbit_gap + square / 2.0
    step = 360.0 / 36
    for i in range(36):
        rad = math.radians(-90.0 + i * step)
        x = cx + orbit * math.cos(rad)
        y = cy + orbit * math.sin(rad)
        draw.rounded_rectangle(
            [x - square / 2.0, y - square / 2.0, x + square / 2.0, y + square / 2.0],
            radius=corner,
            fill=track_idle,
        )

    r = plate / 2.0
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=plate_color)
    sr = r - border_width / 2.0
    draw.ellipse(
        [cx - sr, cy - sr, cx + sr, cy + sr],
        outline=border,
        width=max(1, round(border_width)),
    )

    icon = plate * (REF_ICON / REF_PLATE)
    stroke = unit * REF_STROKE
    pad_s = stroke / 2.0
    left = cx - icon / 2.0
    top = cy - icon / 2.0
    oval = [left + pad_s, top + pad_s, left + icon - pad_s, top + icon - pad_s]
    start = 270.0 + REF_GAP_DEG / 2.0
    end = start + (360.0 - REF_GAP_DEG)
    draw.arc(oval, start=start, end=end, fill=accent, width=max(1, round(stroke)))

    stem_w = stroke
    stem_h = unit * REF_STEM_H
    stem_left = cx - stem_w / 2.0
    stem_top = top + unit * 1.0
    draw.rounded_rectangle(
        [stem_left, stem_top, stem_left + stem_w, stem_top + stem_h],
        radius=unit * 3.0,
        fill=accent,
    )

    out = Path(__file__).resolve().parents[1] / "app/src/main/res/drawable-nodpi/widget_preview_button.png"
    img.save(out, "PNG")
    print(f"saved {out} {img.size}")


if __name__ == "__main__":
    main()
