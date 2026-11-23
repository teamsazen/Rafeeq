#!/usr/bin/env python3
"""Writes the native app's data files in native/app/src/main/res/raw from the
Flutter app's Dart sources: the 99 names (with their Urdu meanings), the namaz
prayers and the wudu steps. Run from the repo root."""
import json
import re

RAW = 'native/app/src/main/res/raw/'


def dart_json(path):
    src = open(path, encoding='utf-8').read()
    return json.loads(src[src.index("r'''") + 4:src.rindex("'''")])


def write(name, value):
    with open(RAW + name, 'w', encoding='utf-8') as f:
        f.write(json.dumps(value, ensure_ascii=False, indent=1))


data = dart_json('lib/data/raw/asma_ul_husna_raw.dart')
urdu = open('lib/data/asma_ul_husna/asma_ul_husna_urdu_meanings.dart', encoding='utf-8').read()
meanings = {int(n): m for n, m in re.findall(r"(\d+):\s*'([^']*)'", urdu)}
out = [
    {
        "number": d["number"],
        "name": d["name"],
        "transliteration": d["transliteration"],
        "meaning": d["meaning"],
        "desc": d["desc"],
        "urduMeaning": meanings.get(d["number"], ""),
    }
    for d in data
]
write('asma_ul_husna.json', out)
write('namaz_prayers.json', dart_json('lib/data/raw/namaz_prayers_raw.dart'))
write('wudu_steps.json', dart_json('lib/data/raw/wudu_steps_raw.dart'))


# ---------- Licences ----------
# Shown in Licences, as Flutter's LicenseRegistry lists its packages: the
# libraries the native app is built on, the icon fonts it draws with, the
# Dart packages it ports, and the 16 line Quran credits.
import os
import shutil

PUB = os.path.expanduser('~/.pub-cache/hosted/pub.dev/')
# The Flutter SDK that runs this repo, found from the flutter command.
FLUTTER = os.path.dirname(os.path.dirname(os.path.realpath(shutil.which('flutter'))))
MATERIAL_ICONS = FLUTTER + '/bin/cache/artifacts/material_fonts/MaterialIcons_LICENSE.txt'


def read(path):
    return open(path, encoding='utf-8').read().strip()


apache = read(PUB + 'adaptive_breakpoints-0.1.7/LICENSE')
credits = open('lib/ui/quran/sixteen_line_quran/sixteen_line_credits.dart', encoding='utf-8').read()


def dart_const(name):
    start = credits.index(f"const {name} = '''") + len(f"const {name} = '''")
    return credits[start:credits.index("'''", start)].strip()


def dart_name(name):
    return re.search(rf"const {name} = '([^']*)'", credits).group(1)


licences = [
    ("Android Jetpack (AndroidX: Activity, Compose, Core, Profile Installer)", apache),
    ("AndroidX Media3 (ExoPlayer and Session)", apache),
    ("Kotlin and kotlinx.coroutines", apache),
    ("desugar_jdk_libs", "GNU General Public License, version 2, with the Classpath Exception.\nhttps://openjdk.org/legal/gplv2+ce.html"),
    ("Material Icons font", read(MATERIAL_ICONS)),
    ("cupertino_icons font", read(PUB + 'cupertino_icons-2.0.0/LICENSE')),
    ("flutter_islamic_icons font", read(PUB + 'flutter_islamic_icons-1.0.2/LICENSE')),
    ("adhan (prayer times, ported to Kotlin)", read(PUB + 'adhan-2.0.0+1/LICENSE')),
    ("hijri (Umm al-Qura calendar, ported to Kotlin)", read(PUB + 'hijri-3.0.1/LICENSE')),
    (dart_name('sixteenLineQuranTextCredit'), dart_const('_quranWbwCredits')),
    (dart_name('sixteenLineQuranLayoutCredit'), dart_const('_qulCredits')),
]
licences.sort(key=lambda l: l[0].lower())
write('licenses.json', [{"package": name, "text": text} for name, text in licences])
