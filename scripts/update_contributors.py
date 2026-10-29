"""Refresh linked, circular README avatars using GitHub contributor counts."""
import html
import io
import json
import os
from pathlib import Path
import subprocess
from urllib.request import urlopen

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
START = '<!-- CONTRIBUTORS:START -->'
END = '<!-- CONTRIBUTORS:END -->'
SIZE = 256        # working size; saved at half for crisp 64px display on 2x screens
RING = 10         # ring width at SIZE
GAP = 8           # transparent gap between ring and photo, so a dark photo
                  # never merges into a dark ring
RINGS = {'dark': (255, 255, 255, 255), 'light': (0, 0, 0, 255)}


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
    return out.resize((SIZE // 2, SIZE // 2), Image.Resampling.LANCZOS)


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
        for theme, colour in RINGS.items():
            name = f'{uid}-{theme}.png'
            ringed(photo, colour).save(output / name)
            written.add(name)
        lines.append(
            f'  <a href="https://github.com/{login}"><picture>'
            f'<source media="(prefers-color-scheme: dark)" srcset="{base}/{uid}-dark.png">'
            f'<img src="{base}/{uid}-light.png" alt="{login}" title="{login}" '
            f'width="64" height="64"></picture></a>'
        )
    lines.append('</p>')
    for stale in output.glob('*.png'):
        if stale.name not in written:
            stale.unlink()
    readme.write_text(before + START + '\n' + '\n'.join(lines) + '\n' + END + after)
    print(f'Updated {len(contributors)} contributors in descending contribution order.')


if __name__ == '__main__':
    main()
