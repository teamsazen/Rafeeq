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
