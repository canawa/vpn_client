"""Count ring dots and sample colors carefully from crops / frames."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance

out = Path(r"c:\vpn_client\design\xeno\_video_analysis")
frames10 = Path(r"c:\vpn_client\design\xeno\_video_frames10")
frames4 = Path(r"c:\vpn_client\design\xeno\_video_frames")

cx, cy, r_orbit = 379.7, 315.2, 251.7

def enhance_ring(path, name):
    im = Image.open(path).convert("RGB")
    # crop
    pad = 40
    box = (int(cx-r_orbit-pad), int(cy-r_orbit-pad), int(cx+r_orbit+pad), int(cy+r_orbit+pad))
    crop = im.crop(box)
    # boost midtones to see dim dots
    arr = np.asarray(crop).astype(np.float32)
    # stretch greens
    g = arr[:,:,1]
    lo, hi = np.percentile(g, 5), np.percentile(g, 99)
    g2 = np.clip((g-lo)/(hi-lo+1e-6)*255, 0, 255)
    vis = np.stack([g2, g2, g2], axis=-1).astype(np.uint8)
    Image.fromarray(vis).save(out / f"boost_{name}.png")
    crop.save(out / f"rawcrop_{name}.png")
    return crop

enhance_ring(frames10 / "t_001.png", "idle")
enhance_ring(frames10 / "t_021.png", "conn_t2")
enhance_ring(frames10 / "t_008.png", "trans")
enhance_ring(frames10 / "t_040.png", "conn_t4")

# Dot pitch via autocorrelation of angular profile on idle
im = Image.open(frames4 / "f_001.png").convert("RGB")
arr = np.asarray(im).astype(np.float32)
h,w = arr.shape[:2]
R,G,B = arr[:,:,0],arr[:,:,1],arr[:,:,2]
# Use known connected orbit center/radius — idle dots should be same orbit
yy,xx = np.mgrid[0:h,0:w]
RR = np.sqrt((xx-cx)**2+(yy-cy)**2)
band = (RR>r_orbit-8)&(RR<r_orbit+8)
band[int(h*0.72):]=False
# luminance
L = 0.3*R+0.6*G+0.1*B
ang = (np.degrees(np.arctan2(yy-cy, xx-cx))+360)%360
profile = np.zeros(360)
cnt = np.zeros(360)
by,bx = np.where(band)
for x,y in zip(bx,by):
    a=int(ang[y,x])%360
    profile[a]+=L[y,x]
    cnt[a]+=1
cnt[cnt==0]=1
profile/=cnt
# subtract moving average
ma = np.convolve(np.r_[profile[-20:],profile,profile[:20]], np.ones(41)/41, mode='same')[20:-20]
detr = profile - ma
# autocorrelation
detr = detr - detr.mean()
corr = np.correlate(np.r_[detr,detr], detr, mode='valid')[:180]
# find first strong peak after 0
corr0 = corr.copy()
corr0[:3]=0
peak_pitch = int(np.argmax(corr0[3:40])+3)
print("idle angular autocorr peak pitch ~", peak_pitch, "deg => N~", round(360/peak_pitch))
print("top corr lags:", sorted([(i, float(corr[i])) for i in range(3,60)], key=lambda x:-x[1])[:8])

# Same for connected but only dim parts? Or full green profile
im2 = Image.open(frames10/"t_021.png").convert("RGB")
arr2 = np.asarray(im2).astype(np.float32)
G2 = arr2[:,:,1]
profile2 = np.zeros(360); cnt2=np.zeros(360)
by,bx = np.where(band)
for x,y in zip(bx,by):
    a=int(ang[y,x])%360
    profile2[a]+=G2[y,x]; cnt2[a]+=1
cnt2[cnt2==0]=1
profile2/=cnt2
# Look at one tracer region 20..55 for discrete peaks without heavy smooth
print("\nRaw G along tracer 20-60:")
for a in range(20,61):
    print(f"  {a}: {profile2[a]:.1f}")

# Sample power icon accent color (connected)
# icon center roughly cx, cy
icon = arr2[int(cy)-30:int(cy)+30, int(cx)-30:int(cx)+30]
mask = (icon[:,:,1]>100) & (icon[:,:,1]>icon[:,:,0]*1.3)
print("icon accent mean", icon[mask].mean(axis=0) if mask.any() else None)

# Sample brightest ring head pixels
ring = arr2.copy()
rmask = band & (G2>160) & (G2>arr2[:,:,0]*1.4)
print("head pixels mean", ring[rmask].mean(axis=0) if rmask.any() else None, "n", rmask.sum())
rmask2 = band & (G2>120) & (G2<=160)
print("mid trail mean", ring[rmask2].mean(axis=0) if rmask2.any() else None)

# Plate border color
# radius of plate ~ size/2; plate ~ 188dp visual, orbit gap
# plate rim ~ r_orbit - gap. gap visually ~ 40-50px?
for pr in [180, 190, 200, 210]:
    pband = (RR>pr-3)&(RR<pr+3)
    pband[int(h*0.72):]=False
    print(f"plate r={pr} mean", arr2[pband].mean(axis=0) if pband.any() else None)

# Glow: compare pixels between plate and ring
glow_band = (RR>210)&(RR<235)
glow_band[int(h*0.72):]=False
print("interstitial glow mean", arr2[glow_band].mean(axis=0))
bg = (RR>300)&(RR<330)
bg[int(h*0.72):]=False
print("far bg mean", arr2[bg].mean(axis=0) if bg.any() else None)
