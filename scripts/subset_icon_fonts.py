#!/usr/bin/env python3
"""Cuts the native app's icon fonts down to the glyphs it draws.

Reads every code point in the Glyph enum (native/.../ui/Icons.kt), takes the
full fonts from where the Flutter app gets them (the Flutter SDK's Material
Icons and the pub cache's cupertino_icons and flutter_islamic_icons), and
writes the subsets to native/app/src/main/res/font. Run it from the repo
root after adding a Glyph; it needs fontTools (pip install fonttools).
"""
import os
import re
import shutil

from fontTools import subset

ICONS = 'native/app/src/main/java/com/satechdev/rafeeqapp/ui/Icons.kt'
OUT = 'native/app/src/main/res/font/'
PUB = os.path.expanduser('~/.pub-cache/hosted/pub.dev/')
FLUTTER = os.path.dirname(os.path.dirname(os.path.realpath(shutil.which('flutter'))))

SOURCES = {
    'materialIcons': (FLUTTER + '/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf', 'material_icons.otf'),
    'cupertinoIcons': (PUB + 'cupertino_icons-2.0.0/assets/CupertinoIcons.ttf', 'cupertino_icons.ttf'),
    'islamicIcons': (PUB + 'flutter_islamic_icons-1.0.2/assets/fonts/IslamicIcons.ttf', 'islamic_icons.ttf'),
}

glyphs = re.findall(r'^\s+\w+\((\w+), (0x[0-9a-fA-F]+)\)', open(ICONS).read(), re.M)
for family, (source, name) in SOURCES.items():
    codes = sorted({int(code, 16) for fam, code in glyphs if fam == family})
    options = subset.Options()
    options.layout_features = []
    options.name_IDs = ['*']
    options.notdef_outline = True
    font = subset.load_font(source, options)
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=codes)
    subsetter.subset(font)
    subset.save_font(font, OUT + name, options)
    print(f'{name}: {len(codes)} glyphs, {os.path.getsize(OUT + name)} bytes')
