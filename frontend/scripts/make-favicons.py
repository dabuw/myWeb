#!/usr/bin/env python3
"""Generate the site favicons from a single source image.

The generated files are committed to the repo, so this only needs to be run
when the logo changes. Requires Pillow (not a project dependency):

    pip install Pillow

Usage:
    python frontend/scripts/make-favicons.py frontend/scripts/logo-original.jpg

    --radius 0.22   corner rounding as a fraction of the side (default 0.22,
                    i.e. the iOS-like squircle look); pass 0 to keep sharp
                    corners
    --no-trim       keep the source framing instead of auto-trimming a uniform
                    border around the artwork

What it does, in order:
    1. auto-trims a uniform border (the flat page background around the card)
    2. centre-crops to a square, so nothing is ever stretched
    3. optionally rounds the corners, so the area outside the arcs becomes
       transparent instead of keeping the page colour
    4. writes the favicon set into frontend/public/, which Vite copies
       verbatim into dist/
"""

import argparse
import pathlib
import sys

try:
    from PIL import Image, ImageDraw
    import numpy as np
except ImportError:
    sys.exit("Pillow and numpy are required: pip install Pillow numpy")

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "frontend" / "public"

# iOS does not handle transparency in home-screen icons well, so the 180px
# version is composited onto a solid background. Keep this in sync with
# --bg in frontend/src/styles.css.
APPLE_BG = (246, 247, 249, 255)

# Supersampling factor for the rounded mask: draw big, then downscale, so the
# corner arcs stay smooth.
SS = 4


def trim_border(img, tolerance=60):
    """Crop away a uniform border, e.g. the flat page colour around a card.

    The tolerance is deliberately generous: artwork exported from a design tool
    usually carries a soft drop shadow, and a low threshold would treat that
    shadow as content and leave a faint halo ring around the icon.
    """
    a = np.asarray(img.convert("RGB")).astype(int)
    edge = np.concatenate([
        a[:6].reshape(-1, 3), a[-6:].reshape(-1, 3),
        a[:, :6].reshape(-1, 3), a[:, -6:].reshape(-1, 3),
    ])
    bg = np.median(edge, axis=0)
    mask = np.abs(a - bg).sum(axis=2) > tolerance
    if not mask.any():
        return img
    ys, xs = np.where(mask)
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def square(img):
    """Centre-crop to a square so the icon never distorts."""
    w, h = img.size
    side = min(w, h)
    left, top = (w - side) // 2, (h - side) // 2
    return img.crop((left, top, left + side, top + side))


def round_corners(img, radius_ratio):
    if radius_ratio <= 0:
        return img
    w, h = img.size
    big = (w * SS, h * SS)
    mask = Image.new("L", big, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, big[0] - 1, big[1] - 1], radius=int(min(big) * radius_ratio), fill=255
    )
    mask = mask.resize((w, h), Image.LANCZOS)
    out = img.copy()
    out.putalpha(mask)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source")
    ap.add_argument("--radius", type=float, default=0.22,
                    help="corner rounding as a fraction of the side (default 0.22)")
    ap.add_argument("--no-trim", action="store_true",
                    help="do not auto-trim a uniform border")
    args = ap.parse_args()

    src = pathlib.Path(args.source).expanduser()
    if not src.is_file():
        sys.exit("no such file: %s" % src)

    img = Image.open(src).convert("RGBA")
    before = img.size
    if not args.no_trim:
        img = trim_border(img)
    img = square(img)
    img = round_corners(img, args.radius)

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    # Work from a generous master so the .ico stays crisp at 48px.
    base = img.resize((512, 512), Image.LANCZOS)
    base.save(OUT_DIR / "favicon-512.png")
    base.resize((32, 32), Image.LANCZOS).save(OUT_DIR / "favicon-32.png")
    base.save(OUT_DIR / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])

    apple = Image.new("RGBA", (180, 180), APPLE_BG)
    apple.alpha_composite(base.resize((180, 180), Image.LANCZOS))
    apple.convert("RGB").save(OUT_DIR / "apple-touch-icon.png")

    print("  source  %s  %dx%d" % (src.name, before[0], before[1]))
    print("  trimmed to %dx%d, radius %.1f%% of the side"
          % (img.size[0], img.size[1], args.radius * 100))
    print()
    for name in ("favicon.ico", "favicon-32.png", "favicon-512.png", "apple-touch-icon.png"):
        p = OUT_DIR / name
        print("  %-22s %7d bytes" % (name, p.stat().st_size))


if __name__ == "__main__":
    main()
