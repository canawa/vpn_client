"""Analyze connect-button ring animation from extracted frames."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw

frames_dir = Path(r"c:\vpn_client\design\xeno\_video_frames")
out_dir = Path(r"c:\vpn_client\design\xeno\_video_analysis")
out_dir.mkdir(parents=True, exist_ok=True)

files = sorted(frames_dir.glob("f_*.png"))
print(f"n_files={len(files)}")

def analyze(path: Path):
    im = Image.open(path).convert("RGB")
    arr = np.asarray(im).astype(np.float32)
    h, w = arr.shape[:2]
    r = arr[:, :, 0]
    g = arr[:, :, 1]
    b = arr[:, :, 2]

    # Exclude bottom status text area
    mask_ui = np.zeros((h, w), dtype=bool)
    mask_ui[: int(h * 0.70), :] = True

    # Neon mint / teal accent
    greenish = (g > 70) & (g > r * 1.15) & (g > b * 1.05) & ((g - r) > 15) & mask_ui
    bright = (g > 130) & (g > r * 1.3) & (g > b * 1.15) & ((g - r) > 35) & mask_ui
    head = (g > 190) & (g > r * 1.35) & ((g - r) > 55) & mask_ui

    bys, bxs = np.where(bright)
    if len(bxs) < 30:
        # disconnected / early frames
        gys, gxs = np.where(greenish)
        return {
            "file": path.name,
            "size": [w, h],
            "state": "idle_or_transition",
            "n_bright": int(bright.sum()),
            "n_green": int(greenish.sum()),
            "n_head": int(head.sum()),
            "n_peaks": 0,
            "peaks_deg": [],
            "r_orbit": None,
            "base_mean_rgb": None,
        }

    # Estimate center: mean of bright pixels is biased; use image-ish center of bright bbox
    # Better: circular fit via mean of points on outer ring after radius filter
    cx0 = float(bxs.mean())
    cy0 = float(bys.mean())
    dx0 = bxs.astype(np.float32) - cx0
    dy0 = bys.astype(np.float32) - cy0
    rad0 = np.sqrt(dx0 * dx0 + dy0 * dy0)
    # Outer orbit ~ high percentile (exclude inner plate border)
    r_guess = float(np.percentile(rad0, 85))
    near0 = (rad0 > r_guess * 0.9) & (rad0 < r_guess * 1.1)
    if near0.sum() < 20:
        near0 = rad0 > np.percentile(rad0, 60)
    cx = float(bxs[near0].mean())
    cy = float(bys[near0].mean())

    dx = bxs.astype(np.float32) - cx
    dy = bys.astype(np.float32) - cy
    rad = np.sqrt(dx * dx + dy * dy)
    r_orbit = float(np.percentile(rad, 80))
    near = (rad > r_orbit * 0.88) & (rad < r_orbit * 1.12)

    # Head pixels near orbit
    hys, hxs = np.where(head)
    if len(hxs) > 10:
        hdx = hxs.astype(np.float32) - cx
        hdy = hys.astype(np.float32) - cy
        hrad = np.sqrt(hdx * hdx + hdy * hdy)
        hnear = (hrad > r_orbit * 0.85) & (hrad < r_orbit * 1.15)
        hang = (np.degrees(np.arctan2(hdy[hnear], hdx[hnear])) + 360) % 360
        head_n = int(hnear.sum())
    else:
        hang = (np.degrees(np.arctan2(dy[near], dx[near])) + 360) % 360
        head_n = 0

    bins = np.linspace(0, 360, 73)  # 5° bins
    hist, edges = np.histogram(hang, bins=bins)
    peaks = []
    for i in range(len(hist)):
        prev = hist[(i - 1) % len(hist)]
        nxt = hist[(i + 1) % len(hist)]
        if hist[i] > prev and hist[i] >= nxt and hist[i] >= max(2, hist.max() * 0.15):
            peaks.append([float(edges[i] + 2.5), int(hist[i])])

    peaks.sort()
    merged = []
    for ang, cnt in peaks:
        if not merged or (ang - merged[-1][0]) > 30:
            merged.append([ang, cnt])
        else:
            a0, c0 = merged[-1]
            merged[-1][0] = (a0 * c0 + ang * cnt) / (c0 + cnt)
            merged[-1][1] = c0 + cnt
    if len(merged) >= 2 and (merged[0][0] + 360 - merged[-1][0]) < 30:
        a0, c0 = merged[0]
        a1, c1 = merged[-1]
        merged[0][0] = ((a0 * c0 + (a1 - 360) * c1) / (c0 + c1)) % 360
        merged[0][1] = c0 + c1
        merged.pop()

    # Base ring color: band excluding bright tracer pixels
    yy, xx = np.mgrid[0:h, 0:w]
    R = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    band = (R > r_orbit * 0.93) & (R < r_orbit * 1.07) & mask_ui
    band_dim = band & ~bright
    if band_dim.any():
        base_mean = [
            float(arr[:, :, 0][band_dim].mean()),
            float(arr[:, :, 1][band_dim].mean()),
            float(arr[:, :, 2][band_dim].mean()),
        ]
    else:
        base_mean = None

    # Angular green profile (2°) for tracer length
    ang_all = (np.degrees(np.arctan2(yy - cy, xx - cx)) + 360) % 360
    profile = []
    for a0 in range(0, 360, 2):
        sel = band & (ang_all >= a0) & (ang_all < a0 + 2)
        profile.append(float(g[sel].mean()) if sel.any() else 0.0)
    profile = np.array(profile)

    # Estimate tracer angular width: around each peak, how far green stays elevated
    tracer_widths = []
    if merged:
        baseline = float(np.percentile(profile, 20))
        threshold = baseline + 0.35 * (float(profile.max()) - baseline)
        for ang, _ in merged:
            # walk CCW (negative math angle = clockwise visually? depends)
            # In image coords y-down, increasing atan2 angle is clockwise visually
            idx = int(ang // 2) % 180
            width = 0
            for k in range(0, 90):
                # trail behind: if rotating clockwise (angle increases), trail is at smaller angles
                j = (idx - k) % 180
                if profile[j] >= threshold:
                    width += 2
                else:
                    if k > 2:
                        break
            tracer_widths.append(width)

    # Save annotated crop
    vis = im.copy()
    draw = ImageDraw.Draw(vis)
    # crop around button
    rr = int(r_orbit * 1.35)
    box = [int(cx - rr), int(cy - rr), int(cx + rr), int(cy + rr)]
    for ang, _ in merged:
        rad = math_rad = np.radians(ang)
        x = cx + r_orbit * np.cos(rad)
        y = cy + r_orbit * np.sin(rad)
        draw.ellipse([x - 6, y - 6, x + 6, y + 6], outline=(255, 0, 255), width=2)
    draw.ellipse([cx - r_orbit, cy - r_orbit, cx + r_orbit, cy + r_orbit], outline=(255, 255, 0), width=1)
    crop = vis.crop(box)
    crop.save(out_dir / f"ann_{path.stem}.png")

    return {
        "file": path.name,
        "size": [w, h],
        "state": "active",
        "center": [round(cx, 1), round(cy, 1)],
        "r_orbit": round(r_orbit, 1),
        "n_bright": int(bright.sum()),
        "n_green": int(greenish.sum()),
        "n_head": head_n,
        "n_peaks": len(merged),
        "peaks_deg": [[round(a, 1), c] for a, c in merged],
        "peak_spacing": (
            [round((merged[(i + 1) % len(merged)][0] - merged[i][0]) % 360, 1) for i in range(len(merged))]
            if len(merged) >= 2
            else []
        ),
        "tracer_widths_deg": tracer_widths,
        "base_mean_rgb": [round(x, 1) for x in base_mean] if base_mean else None,
        "profile_p20": round(float(np.percentile(profile, 20)), 1),
        "profile_p80": round(float(np.percentile(profile, 80)), 1),
        "profile_max": round(float(profile.max()), 1),
    }


results = []
for f in files:
    res = analyze(f)
    results.append(res)
    peaks = res.get("peaks_deg")
    rorb = res.get("r_orbit")
    print(
        f"{res['file']}: state={res.get('state')} peaks={res.get('n_peaks')} "
        f"angles={peaks} spacing={res.get('peak_spacing')} "
        f"tracer_w={res.get('tracer_widths_deg')} r={rorb} "
        f"bright={res.get('n_bright')} base={res.get('base_mean_rgb')}"
    )

# Motion: track primary peak across active frames
print("\n=== Lap period estimate ===")
active = [r for r in results if r.get("n_peaks", 0) >= 3]
# fps of extracted frames = 4, so dt = 0.25s
# Match peaks greedily across frames
if len(active) >= 4:
    # Use mean of sorted peaks; track peak nearest to previous + expected
    prev = sorted(a for a, _ in active[0]["peaks_deg"])
    # pick peak 0
    track = [prev[0]]
    for r in active[1:]:
        angs = sorted(a for a, _ in r["peaks_deg"])
        # find closest to prev+delta; assume clockwise => angle increases in image atan2
        pred = (track[-1] + 20) % 360  # rough
        # choose ang minimizing circular distance to previous + small step
        best = None
        best_d = 1e9
        for a in angs:
            # unwrapped delta from track[-1]
            d = (a - track[-1] + 180) % 360 - 180
            if abs(d - 15) < best_d:  # prefer ~15°/frame at 4fps => ~60°/s => 6s/lap
                # actually just prefer small positive (CW) delta
                pass
            score = abs(d) if d >= -5 else 999
            # prefer positive d (CW in y-down coords)
            if d < -5:
                score = 360 + d
            if score < best_d:
                best_d = score
                best = a
        track.append(best if best is not None else track[-1])
    print("tracked angles:", [round(x, 1) for x in track])
    # unwrap
    unwrapped = [track[0]]
    for a in track[1:]:
        d = (a - unwrapped[-1] + 180) % 360 - 180
        # force mostly positive
        if d < -90:
            d += 360
        unwrapped.append(unwrapped[-1] + d)
    print("unwrapped:", [round(x, 1) for x in unwrapped])
    total_deg = unwrapped[-1] - unwrapped[0]
    total_t = (len(unwrapped) - 1) * 0.25
    if total_t > 0 and total_deg > 10:
        deg_per_s = total_deg / total_t
        period = 360 / deg_per_s
        print(f"deg_per_s={deg_per_s:.1f} period={period:.2f}s total_deg={total_deg:.1f} over {total_t:.2f}s")

with open(out_dir / "analysis.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)
print("wrote", out_dir / "analysis.json")
