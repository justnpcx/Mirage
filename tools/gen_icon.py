#!/usr/bin/env python3
"""Generate every Mirage launcher/TV image asset from code.

Outputs (all under app/src/main/res, overwritten in place):

  mipmap-{mdpi..xxxhdpi}/ic_launcher_foreground.png   adaptive-icon foreground, 108dp @ 4x
  mipmap-{mdpi..xxxhdpi}/ic_launcher.png              legacy (pre-API 26) icon, 48..192px
  drawable-xhdpi/tv_banner.png                        Android TV / Fire TV banner, 320x180

The mark is a dome of two concentric arches standing on a baseline, with a "sun" dot inside the
inner arch (a mirage / sunrise over a horizon), painted in the app's brand ramp (indigo -> teal,
matching app/src/main/res/values/colors.xml). The icon is deliberately white-ground to follow the
Muse / ChatGPT / Gemini convention; the mark itself carries the brand colour.

Two arches, not three: at the sizes that matter (48-96px) a third, thinner ring stops reading as a
shape and turns into grey hatching, and the dot disappears with it. Two arches leaves room for a
4.5dp stroke (what Material icons use) instead of 2.5dp.

Usage:
    python3 tools/gen_icon.py [repo_root]      # repo_root defaults to the script's ../..

Requires Pillow, plus DejaVu Sans Bold for the banner wordmark (package ttf-dejavu on Debian).

Everything is drawn at SS x supersampling and LANCZOS-downscaled, so the output is deterministic:
re-running it on a clean tree must leave `git status` empty.
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

SS = 4                      # supersample factor
BASE = 432                  # xxxhdpi adaptive-foreground canvas (108dp @ 4x)

# --- brand ramp (matches app/src/main/res/values/colors.xml) -----------------
C_LEFT = (0x6C, 0x4C, 0xF1)     # indigo  #6C4CF1
C_RIGHT = (0x1F, 0xBF, 0xAE)    # teal    #1FBFAE (slightly deeper than #2FD8C6 for white bg)

# --- mark geometry, in the 432 canvas ---------------------------------------
CX = 216.0
STROKE = 28.0
ARCHES = (150.0, 92.0)
BAR_HALF = ARCHES[0] + STROKE / 2   # baseline bar is flush with the outer arch
DOT_R = 24.0
DOT_CX = CX + 24.0
# Vertical radius multiplier: >1 makes the arches taller than a semicircle, so the mark is not so
# flat. 1.15 takes the aspect ratio from 2.01:1 to about 1.78:1.
R_V_SCALE = 1.15
# Baseline derived from the geometry so the mark's bounding box is centred vertically. Hardcoding
# it put the box centre 9px below the canvas centre and the mark looked low.
BASELINE_Y = BASE / 2 + (ARCHES[0] * R_V_SCALE + STROKE / 2 - STROKE / 2) / 2
DOT_CY = BASELINE_Y - 40.0


def mark_mask(size: int) -> Image.Image:
    """An L-mode alpha mask of the mark, drawn at `size` px (already supersampled)."""
    k = size / BASE                       # scale from the 432 design space
    img = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(img)
    cx, by = CX * k, BASELINE_Y * k
    w = max(1, round(STROKE * k))

    def arc(r):
        rx = r * k
        ry = r * k * R_V_SCALE
        box = [cx - rx, by - ry, cx + rx, by + ry]
        d.arc(box, start=180, end=360, fill=255, width=w)

    for r in ARCHES:
        arc(r)

    # baseline bar (rounded ends)
    half = BAR_HALF * k
    d.rounded_rectangle(
        [cx - half, by - w / 2, cx + half, by + w / 2],
        radius=w / 2, fill=255,
    )

    # sun dot
    dr = DOT_R * k
    d.ellipse([DOT_CX * k - dr, DOT_CY * k - dr, DOT_CX * k + dr, DOT_CY * k + dr], fill=255)
    return img


def gradient(size: int) -> Image.Image:
    """Horizontal brand gradient spanning the mark's horizontal extent."""
    k = size / BASE
    left = int((CX - BAR_HALF) * k)
    right = int((CX + BAR_HALF) * k)
    span = max(1, right - left)
    row = Image.new("RGB", (size, 1))
    px = row.load()
    for x in range(size):
        t = min(1.0, max(0.0, (x - left) / span))
        px[x, 0] = tuple(round(C_LEFT[i] + (C_RIGHT[i] - C_LEFT[i]) * t) for i in range(3))
    return row.resize((size, size))


def render_mark(big: int) -> Image.Image:
    """Transparent RGBA canvas with the mark in the brand gradient, at geometry scale."""
    mask = mark_mask(big)
    grad = gradient(big)
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.paste(grad, (0, 0), mask)
    return out


# horizontal extent of the mark relative to its canvas, derived from the geometry above
MARK_FRAC = (2 * BAR_HALF + STROKE) / BASE

# measured bounding-box aspect (width / height) of the drawn mark: 104 x 60 at 192px
MARK_ASPECT = 104.0 / 60.0

# DejaVu Sans Bold, for the banner wordmark only. First hit wins, so the drawing is unchanged on
# a machine that has it in the usual place.
_FONT_CANDIDATES = (
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
)


def _font_path() -> str:
    for p in _FONT_CANDIDATES:
        if os.path.exists(p):
            return p
    raise SystemExit(
        "DejaVu Sans Bold not found (tried: %s). Install ttf-dejavu or edit _FONT_CANDIDATES."
        % ", ".join(_FONT_CANDIDATES)
    )


def mark_layer(size: int, frac: float) -> Image.Image:
    """The mark scaled so its width is `frac` of the canvas, centred on a transparent canvas."""
    big = size * SS
    m = render_mark(big)
    n = max(1, int(round(big * (frac / MARK_FRAC))))
    m = m.resize((n, n), Image.LANCZOS)
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    off = (big - n) // 2
    canvas.alpha_composite(m, (off, off))
    return canvas


def foreground(size: int) -> Image.Image:
    """Adaptive-icon foreground: transparent bg, mark sized to the 66dp safe zone.

    0.53 rather than 0.62: at 0.62 the baseline bar's ends sat outside the 66dp safe circle
    (0.611 of the canvas), so a round mask clipped them. 0.53 leaves a small margin.
    """
    return mark_layer(size, 0.53).resize((size, size), Image.LANCZOS)


def legacy(size: int) -> Image.Image:
    """Legacy (pre-API 26) icon: white rounded-square + mark."""
    big = size * SS
    bg = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    ImageDraw.Draw(bg).rounded_rectangle(
        [0, 0, big - 1, big - 1], radius=int(big * 0.22), fill=(255, 255, 255, 255),
    )
    bg.alpha_composite(mark_layer(size, 0.58))
    return bg.resize((size, size), Image.LANCZOS)


def render_mark_w(width: int) -> Image.Image:
    """The mark drawn at an exact pixel width, on a transparent square canvas centred on it."""
    big = int(round(width / MARK_FRAC))
    return render_mark(big)


def banner(width: int = 320, height: int = 180) -> Image.Image:
    """Android TV / Fire TV banner (320x180).

    The manifest used to point `android:banner` at the launcher icon, so the TV launcher drew a
    square icon letterboxed inside the 16:9 banner slot (dark bars either side). This is a real
    banner: white ground, brand-gradient mark, and the wordmark, laid out as one centred group.
    """
    ss = 4
    cw, ch = width * ss, height * ss
    img = Image.new("RGB", (cw, ch), (255, 255, 255))

    mark_w = int(0.26 * cw)
    gap = int(0.05 * cw)
    font = ImageFont.truetype(_font_path(), int(0.235 * ch))

    probe = ImageDraw.Draw(img)
    tb = probe.textbbox((0, 0), "Mirage", font=font)
    text_w = tb[2] - tb[0]

    group_w = mark_w + gap + text_w
    gx = (cw - group_w) // 2
    gy = ch // 2

    mark = render_mark_w(mark_w)
    img.paste(mark, (gx - (mark.width - mark_w) // 2, gy - mark.height // 2), mark)

    tx = gx + mark_w + gap - tb[0]
    ty = gy - (tb[1] + tb[3]) // 2
    probe.text((tx, ty), "Mirage", font=font, fill=(0x1A, 0x1C, 0x22))

    return img.resize((width, height), Image.LANCZOS)


def main():
    if len(sys.argv) > 1:
        root = sys.argv[1]
    else:
        # <repo>/tools/gen_icon.py -> <repo>
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res = os.path.join(root, "app/src/main/res")
    fg_sizes = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    legacy_sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for d, s in fg_sizes.items():
        p = os.path.join(res, f"mipmap-{d}", "ic_launcher_foreground.png")
        foreground(s).save(p)
        print("wrote", p, s)
    for d, s in legacy_sizes.items():
        p = os.path.join(res, f"mipmap-{d}", "ic_launcher.png")
        legacy(s).save(p)
        print("wrote", p, s)
    # TV banner lives in drawable-xhdpi (320x180 is the TV standard)
    bdir = os.path.join(res, "drawable-xhdpi")
    os.makedirs(bdir, exist_ok=True)
    bp = os.path.join(bdir, "tv_banner.png")
    banner().save(bp)
    print("wrote", bp, "320x180")


if __name__ == "__main__":
    main()
