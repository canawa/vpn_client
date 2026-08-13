"""Precise outer-ring tracking at 10fps; ignore inner plate glow."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw

frames = sorted(Path(r"c:\vpn_client\design\xeno\_video_frames10").glob("t_*.png"))
out = Path(r"c:\vpn_client\design\xeno\_video_analysis")
out.mkdir(parents=True, exist_ok=True)
print("n", len(frames))

# First pass: find stable orbit radius from a mid connected frame
def ring_mask(arr, cx, cy, r0, dr=14):
    h, w = arr.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    R = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    return (R > r0 - dr) & (R < r0 + dr)

def find_center_and_radius(path):
    im = Image.open(path).convert("RGB")
    arr = np.asarray(im).astype(np.float32)
    h, w = arr.shape[:2]
    g = arr[:, :, 1]
    r = arr[:, :, 0]
    b = arr[:, :, 2]
    mask_ui = np.zeros((h, w), dtype=bool)
    mask_ui[: int(h * 0.68), :] = True
    bright = (g > 140) & (g > r * 1.3) & ((g - r) > 40) & mask_ui
    ys, xs = np.where(bright)
    if len(xs) < 50:
        return None
    cx, cy = w / 2.0, float(np.percentile(ys, 45))  # button above text
    # refine cy/cx using bright pixels
    cx = float(xs.mean())
    cy = float(ys.mean())
    dx = xs.astype(np.float32) - cx
    dy = ys.astype(np.float32) - cy
    rad = np.sqrt(dx * dx + dy * dy)
    # Outer ring radius: mode of high radii
    # Exclude inner plate (~ plate is smaller)
    outer = rad[rad > np.percentile(rad, 55)]
    if len(outer) < 20:
        return None
    r_orbit = float(np.median(outer))
    # refine center from outer ring pixels only
    near = (rad > r_orbit - 18) & (rad < r_orbit + 18)
    if near.sum() < 30:
        return None
    cx = float(xs[near].mean())
    cy = float(ys[near].mean())
    dx = xs[near].astype(np.float32) - cx
    dy = ys[near].astype(np.float32) - cy
    r_orbit = float(np.median(np.sqrt(dx * dx + dy * dy)))
    return cx, cy, r_orbit, arr.shape

# Use a known good connected frame (around 1s in = t_010+)
ref = None
for f in frames[10:40]:
    ref = find_center_and_radius(f)
    if ref and ref[2] > 200:
        print("ref from", f.name, "cx,cy,r", ref[0], ref[1], ref[2])
        break

if not ref:
    raise SystemExit("no ref")

cx, cy, r_orbit, shape = ref
h, w = shape[0], shape[1]

results = []
for i, f in enumerate(frames):
    im = Image.open(f).convert("RGB")
    arr = np.asarray(im).astype(np.float32)
    Rch, Gch, Bch = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]
    band = ring_mask(arr, cx, cy, r_orbit, dr=12)
    # exclude bottom text just in case
    band[: int(h * 0.02), :] = False
    band[int(h * 0.72) :, :] = False

    # brightness along ring
    yy, xx = np.mgrid[0:h, 0:w]
    ang = (np.degrees(np.arctan2(yy - cy, xx - cx)) + 360) % 360
    profile = np.zeros(360, dtype=np.float32)
    counts = np.zeros(360, dtype=np.float32)
    by, bx = np.where(band)
    for x, y in zip(bx, by):
        a = int(ang[y, x]) % 360
        profile[a] += Gch[y, x]
        counts[a] += 1
    counts[counts == 0] = 1
    profile /= counts

    # smooth
    k = 7
    ker = np.ones(k) / k
    smooth = np.convolve(np.r_[profile[-k:], profile, profile[:k]], ker, mode="same")[k:-k]

    # find 4 peaks via correlation with 4-fold template or local maxima
    peaks = []
    for a in range(360):
        if smooth[a] >= smooth[(a - 1) % 360] and smooth[a] > smooth[(a + 1) % 360]:
            if smooth[a] > np.percentile(smooth, 70):
                peaks.append((a, float(smooth[a])))
    # merge within 25°
    peaks.sort()
    merged = []
    for a, v in peaks:
        if not merged or min((a - merged[-1][0]) % 360, (merged[-1][0] - a) % 360) > 25:
            merged.append([a, v])
        else:
            if v > merged[-1][1]:
                merged[-1] = [a, v]
    if len(merged) >= 2 and min((merged[0][0] - merged[-1][0]) % 360, (merged[-1][0] - merged[0][0]) % 360) <= 25:
        if merged[0][1] >= merged[-1][1]:
            merged.pop()
        else:
            merged.pop(0)
            # keep last as first? already removed first weaker
    # Keep top 4
    merged = sorted(merged, key=lambda x: -x[1])[:4]
    merged = sorted(merged, key=lambda x: x[0])

    # Tracer half-width: from each peak, walk backward (decreasing angle = trail if CW motion)
    # Determine motion later; measure both sides where signal > mid threshold
    baseline = float(np.percentile(smooth, 25))
    thr = baseline + 0.45 * (float(smooth.max()) - baseline)
    widths = []
    for a, v in merged:
        # trail likely opposite to motion; measure contiguous above thr around peak
        wdeg = 1
        for d in range(1, 80):
            if smooth[(a - d) % 360] >= thr:
                wdeg += 1
            else:
                break
        # also a little forward of head
        for d in range(1, 15):
            if smooth[(a + d) % 360] >= thr:
                wdeg += 1
            else:
                break
        widths.append(wdeg)

    # Base dimness: pixels in band below bright threshold
    bright_band = band & (Gch > 120) & (Gch > Rch * 1.25)
    dim_band = band & ~bright_band
    base_mean = (
        [float(Rch[dim_band].mean()), float(Gch[dim_band].mean()), float(Bch[dim_band].mean())]
        if dim_band.any()
        else None
    )
    bright_frac = float(bright_band.sum()) / float(band.sum()) if band.any() else 0

    # Max green on band vs idle gray
    results.append(
        {
            "i": i,
            "t": round(i / 10.0, 2),
            "file": f.name,
            "n_peaks": len(merged),
            "peaks": [p[0] for p in merged],
            "peak_vals": [round(p[1], 1) for p in merged],
            "widths": widths,
            "smooth_max": round(float(smooth.max()), 1),
            "smooth_p25": round(baseline, 1),
            "bright_frac": round(bright_frac, 3),
            "base_mean": [round(x, 1) for x in base_mean] if base_mean else None,
        }
    )

# Track phase using 4-fold: use mean of (peak_i - i*90) 
print("\nTime series (t, peaks, widths, bright_frac):")
phases = []
for r in results:
    print(
        f"t={r['t']:.2f} peaks={r['peaks']} w={r['widths']} "
        f"bf={r['bright_frac']} maxG={r['smooth_max']} base={r['base_mean']}"
    )
    if r["n_peaks"] == 4:
        # unwrap phase: peak0 relative
        p = sorted(r["peaks"])
        # normalize: p[k] should be ~ phase + k*90
        # estimate phase as average of (p[k] - k*90)
        # but unknown which is k=0; try all rotations of labeling
        best = None
        for off in range(4):
            errs = []
            phs = []
            for k in range(4):
                ang = p[(k + off) % 4]  # wrong
            # better: for assumed phase, error
        # Use circular mean of (peak - round(peak/90)*90) 
        mods = [((a % 90) + 90) % 90 for a in p]
        # all mods should be similar = phase mod 90
        # circular mean of mods
        s = sum(np.exp(1j * np.radians(m * 4)) for m in mods)  # *4 because 90° period
        phase90 = (np.degrees(np.angle(s)) / 4.0) % 90
        phases.append((r["t"], phase90, p))

print("\n=== Phase mod 90 (4-fold) ===")
for t, ph, p in phases:
    print(f"t={t:.2f} phase90={ph:.1f} peaks={p}")

# Unwrap phase90 over time to get d(phase)/dt, then full period = 360 / (dphase90/dt * 4)? 
# phase90 advances 90° when lights advance 90°; full lap when phase90 advances 90°? 
# When lights rotate 90°, the pattern looks identical and phase90 wraps. 
# So d(phase90)/dt * (360/90) = full angular speed? 
# If phase90 goes 0→90 over T seconds, lights moved 90° in T seconds, full lap = 4T.
if len(phases) >= 4:
    unwrapped = [phases[0][1]]
    for t, ph, _ in phases[1:]:
        d = (ph - (unwrapped[-1] % 90) + 45) % 90 - 45
        unwrapped.append(unwrapped[-1] + d)
    # Fit speed over stable connected region (skip first 0.5s)
    ts = np.array([p[0] for p in phases])
    us = np.array(unwrapped)
    mask = ts >= 1.0
    if mask.sum() >= 5:
        coef = np.polyfit(ts[mask], us[mask], 1)
        # coef[0] = deg/s of phase90
        # lights angular speed = same as phase90 speed (phase90 tracks the offset within 90° sector)
        speed = coef[0]  # degrees of light motion per second
        # careful: unwrapped phase90 increases when lights rotate; 90 of phase90 = 90 of light motion
        period = 360.0 / abs(speed) if abs(speed) > 0.5 else None
        print(f"\nphase90 speed={speed:.2f} deg/s => full lap period={period:.2f}s")
        print("unwrapped phase90 samples:", list(zip([round(t,2) for t in ts], [round(u,1) for u in us])))

# Annotate a few frames
for idx in [5, 15, 25, 40, 55, 70]:
    if idx >= len(frames):
        continue
    f = frames[idx]
    im = Image.open(f).convert("RGB")
    draw = ImageDraw.Draw(im)
    r = results[idx]
    draw.ellipse([cx - r_orbit, cy - r_orbit, cx + r_orbit, cy + r_orbit], outline=(255, 255, 0), width=1)
    for a in r["peaks"]:
        rad = np.radians(a)
        x = cx + r_orbit * np.cos(rad)
        y = cy + r_orbit * np.sin(rad)
        draw.ellipse([x - 8, y - 8, x + 8, y + 8], outline=(255, 0, 255), width=2)
    box = [int(cx - r_orbit * 1.3), int(cy - r_orbit * 1.3), int(cx + r_orbit * 1.3), int(cy + r_orbit * 1.3)]
    im.crop(box).save(out / f"ring_{f.stem}.png")

with open(out / "analysis_10fps.json", "w", encoding="utf-8") as f:
    json.dump({"cx": cx, "cy": cy, "r_orbit": r_orbit, "results": results, "phases": phases}, f, indent=2)
print("done")
