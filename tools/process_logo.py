from PIL import Image
import collections
from pathlib import Path

src = Path(
    r"C:\Users\den\.cursor\projects\c-vpn-client\assets"
    r"\c__Users_den_AppData_Roaming_Cursor_User_workspaceStorage_"
    r"8936c001e0ef6d7276b6b248ed59d5b5_images_photo_2026-07-29_14-08-42-e894bb97-a823-439e-a513-3c3bcc47eafb.png"
)
img = Image.open(src).convert("RGBA")
w, h = img.size
px = img.load()


def is_bg(r, g, b, a):
    if a < 20:
        return True
    brightness = (r + g + b) / 3
    is_reddish = r > 80 and r > g * 1.35 and r > b * 1.35
    if is_reddish:
        return False
    if r > 200 and g > 200 and b > 200:
        return False
    return brightness < 55


visited = [[False] * h for _ in range(w)]
q = collections.deque()
for x in range(w):
    for y in (0, h - 1):
        q.append((x, y))
for y in range(h):
    for x in (0, w - 1):
        q.append((x, y))

mask = [[False] * h for _ in range(w)]
while q:
    x, y = q.popleft()
    if x < 0 or y < 0 or x >= w or y >= h or visited[x][y]:
        continue
    visited[x][y] = True
    r, g, b, a = px[x, y]
    if not is_bg(r, g, b, a):
        continue
    mask[x][y] = True
    q.append((x + 1, y))
    q.append((x - 1, y))
    q.append((x, y + 1))
    q.append((x, y - 1))

out = img.copy()
opx = out.load()
for x in range(w):
    for y in range(h):
        if mask[x][y]:
            opx[x, y] = (0, 0, 0, 0)

bbox = out.getbbox()
print("size", w, h, "bbox", bbox)
if bbox:
    pad = 8
    l, t, r, b = bbox
    l = max(0, l - pad)
    t = max(0, t - pad)
    r = min(w, r + pad)
    b = min(h, b + pad)
    cw, ch = r - l, b - t
    side = max(cw, ch)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    ox = (side - cw) // 2
    oy = (side - ch) // 2
    sq.paste(out.crop((l, t, r, b)), (ox, oy))
    out = sq

if max(out.size) < 1024:
    out = out.resize((1024, 1024), Image.Resampling.LANCZOS)

res = Path(r"c:\vpn_client\android\app\src\main\res")
logo_path = res / "drawable-nodpi" / "ic_logo.png"
out.save(logo_path, "PNG")
print("saved", logo_path, out.size)

bean = res / "drawable-nodpi" / "ic_coffee_bean.png"
out.save(bean, "PNG")
print("replaced", bean)


def make_size(base, size, pad_ratio=0.0, bg=None):
    canvas_bg = bg if bg is not None else (0, 0, 0, 0)
    canvas = Image.new("RGBA", (size, size), canvas_bg)
    inner = int(size * (1 - 2 * pad_ratio))
    scaled = base.resize((inner, inner), Image.Resampling.LANCZOS)
    off = (size - inner) // 2
    canvas.paste(scaled, (off, off), scaled)
    return canvas


def make_mono(base, size):
    g = base.resize((size, size), Image.Resampling.LANCZOS)
    mono = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gp = g.load()
    mp = mono.load()
    for x in range(size):
        for y in range(size):
            _r, _g, _b, a = gp[x, y]
            if a > 40:
                mp[x, y] = (255, 255, 255, a)
    return mono


densities = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
fg_densities = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

for dens, size in densities.items():
    d = res / f"mipmap-{dens}"
    full = make_size(out, size, pad_ratio=0.06, bg=(6, 4, 8, 255))
    full.save(d / "ic_launcher.png", "PNG")
    full.save(d / "ic_launcher_round.png", "PNG")

for dens, size in fg_densities.items():
    d = res / f"mipmap-{dens}"
    fg = make_size(out, size, pad_ratio=0.12, bg=None)
    fg.save(d / "ic_launcher_foreground.png", "PNG")
    fg.save(d / "ic_launcher_round_foreground.png", "PNG")
    make_mono(out, size).save(d / "ic_launcher_monochrome.png", "PNG")
    Image.new("RGBA", (size, size), (6, 4, 8, 255)).save(d / "ic_launcher_background.png", "PNG")

print("launcher icons generated")

notif = make_mono(out, 128)
notif_path = res / "drawable-nodpi" / "ic_logo_notif.png"
notif.save(notif_path, "PNG")
print("notif", notif_path)

preview = res / "drawable-nodpi" / "ic_logo_preview.png"
out.resize((256, 256), Image.Resampling.LANCZOS).save(preview, "PNG")
print("preview", preview)
