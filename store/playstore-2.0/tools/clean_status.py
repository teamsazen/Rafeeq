"""Replaces the phone's own status bar in raw screenshots with a clean one:
9:41, full signal, Wi-Fi and battery, in the app's colours.
usage: clean_status.py in.png out.png"""
import sys
from PIL import Image, ImageDraw, ImageFont

FONT = sys.argv[3] if len(sys.argv) > 3 else None
BAR = 92  # the Samsung S21's status bar at 1080 px wide


def main(src, dst):
    im = Image.open(src).convert('RGB')
    w, _ = im.size
    bg = im.getpixel((w // 2, BAR + 6))
    dark = sum(bg) < 382
    ink = (246, 246, 242) if dark else (32, 38, 36)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, w, BAR), fill=bg)
    font = ImageFont.truetype(FONT, 40)
    d.text((56, BAR // 2), '9:41', font=font, fill=ink, anchor='lm')
    cy = BAR // 2
    # Battery, at the right edge.
    bx = w - 60 - 54
    d.rounded_rectangle((bx, cy - 13, bx + 50, cy + 13), radius=7, outline=ink, width=3)
    d.rounded_rectangle((bx + 5, cy - 8, bx + 45, cy + 8), radius=4, fill=ink)
    d.rounded_rectangle((bx + 52, cy - 5, bx + 56, cy + 5), radius=2, fill=ink)
    # Wi-Fi: three arcs and a dot.
    wx, wy = bx - 44, cy + 12
    for r in (30, 21, 12):
        d.arc((wx - r, wy - r, wx + r, wy + r), 225, 315, fill=ink, width=5)
    d.ellipse((wx - 4, wy - 7, wx + 4, wy + 1), fill=ink)
    # Signal: four rising bars.
    sx = wx - 92
    for i in range(4):
        h = 8 + i * 7
        d.rounded_rectangle((sx + i * 11, cy + 12 - h, sx + i * 11 + 7, cy + 12), radius=2, fill=ink)
    im.save(dst)


main(sys.argv[1], sys.argv[2])
