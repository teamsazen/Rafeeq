"""Refresh linked, circular README avatars using GitHub contributor counts.

Each avatar carries a badge on its top right with the person's commit count
(999, then 1K, 1.11K, 12.34K, 1M).
"""
import html
import io
import json
import os
from pathlib import Path
import subprocess
from urllib.request import urlopen

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
START = '<!-- CONTRIBUTORS:START -->'
END = '<!-- CONTRIBUTORS:END -->'
SIZE = 256        # working size; saved at half for crisp 64px display on 2x screens
RING = 10         # ring width at SIZE
GAP = 8           # transparent gap between ring and photo, so a dark photo
                  # never merges into a dark ring
RINGS = {'dark': (255, 255, 255, 255), 'light': (0, 0, 0, 255)}

# Commit-count badge, in display pixels (the avatar shows at 64 px; work at 4x).
X = SIZE // 64
BADGE_H = 23
BADGE_TOP = -8     # badge top, relative to the avatar's top edge
BADGE_INSET = 4    # badge centre sits this far left of the avatar's right edge
CUTOUT = 3         # transparent space around the badge, so it looks cut out
TOP = -BADGE_TOP + CUTOUT
BADGE_RED = (229, 72, 77, 255)
FONT = ImageFont.truetype(str(ROOT / 'assets/fonts/GoogleSans-Regular.ttf'), round(11.5 * X))


def short(n):
    """999 stays as is; then 1K, 1.11K, 12.34K (cut, not rounded, so 999,999 never reads 1000K)."""
    for size, unit in ((10**9, 'B'), (10**6, 'M'), (10**3, 'K')):
        if n >= size:
            return f'{(n * 100 // size) / 100:g}{unit}'
    return str(n)


def badged(avatar, count):
    """The ringed avatar (SIZE px) with a red count badge on its top right, at half size."""
    label = short(count)
    bw = max(BADGE_H, 10 + len(label) * 6.9)
    width = max(64, 64 - BADGE_INSET + bw / 2 + CUTOUT)
    out = Image.new('RGBA', (round(width * X), (64 + TOP) * X), (0, 0, 0, 0))
    out.paste(avatar, (0, TOP * X), avatar)
    draw = ImageDraw.Draw(out)
    left = (64 - BADGE_INSET - bw / 2) * X
    top = (TOP + BADGE_TOP) * X
    box = (left, top, left + bw * X, top + BADGE_H * X)
    pad = CUTOUT * X
    cut = (box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad)
    draw.rounded_rectangle(cut, radius=(BADGE_H / 2 + CUTOUT) * X, fill=(0, 0, 0, 0))
    draw.rounded_rectangle(box, radius=BADGE_H / 2 * X, fill=BADGE_RED)
    # Regular Google Sans with a thin stroke reads as semibold.
    draw.text(((box[0] + box[2]) / 2, (box[1] + box[3]) / 2 + 0.3 * X), label,
              font=FONT, anchor='mm', fill='white', stroke_width=2, stroke_fill='white')
    return out.resize((out.width // 2, out.height // 2), Image.Resampling.LANCZOS)


def ringed(photo, colour):
    """Circular photo inside a ring of `colour`, transparent around it."""
    out = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(out).ellipse((0, 0, SIZE - 1, SIZE - 1), outline=colour, width=RING)
    inset = RING + GAP
    inner = SIZE - 2 * inset
    face = ImageOps.fit(photo, (inner, inner))
    mask = Image.new('L', (inner, inner), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, inner - 1, inner - 1), fill=255)
    out.paste(face, (inset, inset), mask)
    return out


def main():
    repo = os.environ.get('GITHUB_REPOSITORY', 'teamsazen/Rafeeq')
    pages = json.loads(subprocess.check_output([
        'gh', 'api', '--paginate', '--slurp',
        f'repos/{repo}/contributors?per_page=100',
    ], text=True))
    contributors = sorted(
        (user for page in pages for user in page if user.get('type') == 'User'),
        key=lambda user: (-user['contributions'], user['login'].lower()),
    )
    if not contributors:
        raise RuntimeError('No contributors returned; preserving existing README.')
    readme = ROOT / 'README.md'
    before, remaining = readme.read_text().split(START, 1)
    _, after = remaining.split(END, 1)
    output = ROOT / '.github/readme/contributors'
    output.mkdir(parents=True, exist_ok=True)
    lines = ['<p>']
    written = set()
    base = '.github/readme/contributors'
    for user in contributors:
        login = html.escape(user['login'], quote=True)
        uid = int(user['id'])
        # Render the circle and ring into the image: GitHub strips CSS. One
        # file per theme, picked by <picture>.
        with urlopen(user['avatar_url'], timeout=30) as response:
            photo = Image.open(io.BytesIO(response.read())).convert('RGBA')
        count = int(user['contributions'])
        for theme, colour in RINGS.items():
            name = f'{uid}-{theme}.png'
            image = badged(ringed(photo, colour), count)
            image.save(output / name)
            written.add(name)
        label = f'{login}: {count} commit{"" if count == 1 else "s"}'
        lines.append(
            f'  <a href="https://github.com/{login}"><picture>'
            f'<source media="(prefers-color-scheme: dark)" srcset="{base}/{uid}-dark.png">'
            f'<img src="{base}/{uid}-light.png" alt="{label}" title="{label}" '
            f'width="{image.width // 2}" height="{image.height // 2}"></picture></a>'
        )
    lines.append('</p>')
    for stale in output.glob('*.png'):
        if stale.name not in written:
            stale.unlink()
    readme.write_text(before + START + '\n' + '\n'.join(lines) + '\n' + END + after)
    print(f'Updated {len(contributors)} contributors in descending contribution order.')


if __name__ == '__main__':
    main()
