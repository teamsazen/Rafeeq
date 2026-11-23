#!/usr/bin/env python3
"""Writes native/app/src/main/res/raw/asma_ul_husna.json from the Flutter data
(lib/data/raw/asma_ul_husna_raw.dart and the Urdu meanings). Run from the repo root."""
import json
import re

src = open('lib/data/raw/asma_ul_husna_raw.dart', encoding='utf-8').read()
data = json.loads(src[src.index("r'''") + 4:src.rindex("'''")])
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
with open('native/app/src/main/res/raw/asma_ul_husna.json', 'w', encoding='utf-8') as f:
    f.write(json.dumps(out, ensure_ascii=False, indent=1))
