"""Download ISO flag PNGs from flagcdn into app assets."""
from __future__ import annotations

import concurrent.futures
import urllib.request
from pathlib import Path

OUT = Path(r"c:\vpn_client\android\app\src\main\assets\flags")
OUT.mkdir(parents=True, exist_ok=True)

# Common ISO 3166-1 alpha-2 (+ EU/UN/XK) used in VPN subscriptions
CODES = """
ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bl bm bn bo bq br bs bt bv bw by bz
ca cc cd cf cg ch ci ck cl cm cn co cr cu cv cw cx cy cz de dj dk dm do dz ec ee eg eh er es et fi fj fk fm fo fr
ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu id ie il im in io iq ir is it je jm jo jp
ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mf mg mh mk ml mm mn mo mp mq mr ms mt
mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk pl pm pn pr ps pt pw py qa re ro rs ru rw
sa sb sc sd se sg sh si sj sk sl sm sn so sr ss st sv sx sy sz tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug
um us uy uz va vc ve vg vi vn vu wf ws ye yt za zm zw
eu un xk
""".split()


def download(code: str, width: int = 80) -> bool:
    dest = OUT / f"{code}.png"
    if dest.exists() and dest.stat().st_size > 50:
        return True
    url = f"https://flagcdn.com/w{width}/{code}.png"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "PorozoffVpn/1.0"})
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = resp.read()
        if len(data) < 50:
            return False
        dest.write_bytes(data)
        return True
    except Exception as exc:
        print(f"fail {code}: {exc}")
        return False


def main() -> None:
    ok = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as pool:
        for result in pool.map(download, CODES):
            if result:
                ok += 1
    print(f"done ok={ok}/{len(CODES)} dir={OUT}")
    print(f"files={len(list(OUT.glob('*.png')))}")


if __name__ == "__main__":
    main()
