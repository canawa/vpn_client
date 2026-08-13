"""Measure idle dot geometry and tracer profile shape."""
from pathlib import Path
import numpy as np
from PIL import Image

frames4 = Path(r"c:\vpn_client\design\xeno\_video_frames")
frames10 = Path(r"c:\vpn_client\design\xeno\_video_frames10")

# Idle frame
im = Image.open(frames4 / "f_001.png").convert("RGB")
arr = np.asarray(im).astype(np.float32)
h, w = arr.shape[:2]
R, G, B = arr[:,:,0], arr[:,:,1], arr[:,:,2]
# gray dots: mid gray, low chroma
gray = (np.abs(R-G)<18) & (np.abs(G-B)<18) & (R>25) & (R<90)
# focus upper UI
gray[int(h*0.72):, :] = False
ys, xs = np.where(gray)
print("idle gray pixels", len(xs))
cx, cy = w/2.0, float(ys.mean()) if len(ys) else h*0.42
dx = xs.astype(float)-cx
dy = ys.astype(float)-cy
rad = np.sqrt(dx*dx+dy*dy)
r_orb = float(np.median(rad))
print("idle center", cx, cy, "r", r_orb)
near = (rad > r_orb-20) & (rad < r_orb+20)
angs = (np.degrees(np.arctan2(dy[near], dx[near]))+360)%360
# cluster into 4 arcs
angs_s = np.sort(angs)
# bin 2 deg
hist = np.zeros(180)
for a in angs:
    hist[int(a)//2] += 1
# find contiguous segments
segs = []
in_seg = False
for i,v in enumerate(hist):
    if v>=2 and not in_seg:
        start=i; in_seg=True
    elif v<2 and in_seg:
        segs.append((start*2, i*2, hist[start:i].sum()))
        in_seg=False
if in_seg:
    segs.append((start*2, 360, hist[start:].sum()))
print("idle segments (start,end,count):", segs)

# Connected frame profile at t=2.0 (t_021)
im2 = Image.open(frames10 / "t_021.png").convert("RGB")
arr2 = np.asarray(im2).astype(np.float32)
R2,G2,B2 = arr2[:,:,0], arr2[:,:,1], arr2[:,:,2]
cx,cy,r_orb = 379.7, 315.2, 251.7
yy,xx = np.mgrid[0:h,0:w]
RR = np.sqrt((xx-cx)**2+(yy-cy)**2)
band = (RR>r_orb-10)&(RR<r_orb+10)
band[int(h*0.72):]=False
ang = (np.degrees(np.arctan2(yy-cy, xx-cx))+360)%360
profile = np.zeros(360)
cnt = np.zeros(360)
by,bx = np.where(band)
for x,y in zip(bx,by):
    a=int(ang[y,x])%360
    profile[a]+=G2[y,x]
    cnt[a]+=1
cnt[cnt==0]=1
profile/=cnt
# smooth
ker=np.ones(5)/5
sm=np.convolve(np.r_[profile[-5:],profile,profile[:5]],ker,mode='same')[5:-5]

# Peak at ~53 from earlier
peak=53
# print profile around peak ±40°
print("\nProfile around peak 53 (angle, G):")
for d in range(-40, 25):
    a=(peak+d)%360
    print(f"  {d:+d}° ang={a} G={sm[a]:.1f}")

# Estimate tracer: from head, trail is decreasing angle if CW (angle increases over time)
# Head at 53, trail toward smaller angles
head_g = sm[peak]
base = float(np.percentile(sm, 20))
print(f"\nheadG={head_g:.1f} base={base:.1f}")
for frac in [0.8, 0.6, 0.4, 0.3, 0.2, 0.15, 0.1]:
    thr = base + frac*(head_g-base)
    # walk back
    length=0
    for d in range(0,90):
        if sm[(peak-d)%360] >= thr:
            length=d
        else:
            break
    print(f"  trail to {frac*100:.0f}% of head-base: {length}°")

# Color of head vs mid trail vs far trail
def sample_color(a):
    sel = band & (ang>=a)&(ang<a+3)
    if not sel.any():
        return None
    return [float(R2[sel].mean()), float(G2[sel].mean()), float(B2[sel].mean())]

print("\nColors:")
for label,a in [("head",53), ("trail-6",47), ("trail-12",41), ("trail-20",33), ("gap",75)]:
    print(label, a, sample_color(a))

# Dot angular pitch: in idle, estimate from segment
# Also check connected whether dim base dots remain - compare band pixels not in tracer
bright = band & (G2>100) & (G2>R2*1.3)
dim = band & ~bright
print("\ndim base mean", [float(R2[dim].mean()), float(G2[dim].mean()), float(B2[dim].mean())] if dim.any() else None)
print("idle band gray mean would be ~11; connected dim base ~", )

# Count approximate discrete bright blobs along one tracer
# Take angles from peak-30 to peak+5, find local maxima in radial max
print("\nLocal maxima along trail (possible lit squares):")
for a in range(peak-35, peak+6):
    if sm[a%360] >= sm[(a-1)%360] and sm[a%360] > sm[(a+1)%360] and sm[a%360] > base+10:
        print(f"  ang={a%360} G={sm[a%360]:.1f}")
