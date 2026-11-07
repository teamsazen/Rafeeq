#!/usr/bin/env python3
"""Builds assets/quran_16_line.json from the QUL downloads.

Inputs (download from https://qul.tarteel.ai after logging in):
  - taj-indopak-16-lines.db  Mushaf layout 11, "Indopak 16 lines (Taj company)"
  - indopak-nastaleeq.db     Quran script 59, "Indopak Nastaleeq script - Word
                             by Word"

The text is drawn with QUL's "Indopak Nastaleeq" font (resource 242), read
from assets/fonts/IndoPakNastaleeqHanafi.ttf. That file is not committed, as
the font's licence does not allow sharing it. Script and font belong together:
the script ends each ayah with a private-use character that the font draws as
the numbered ayah ring, and its waqf signs are encoded for this font.

Surah headings use QUL's "Surah name font v2" (resource 455), shipped as
assets/fonts/SurahNameV2.ttf: it draws "surah001" to "surah114" as the
calligraphic name of that surah.

Usage:
  python3 scripts/build_sixteen_line_quran.py LAYOUT_DB WORDS_DB [OUT_JSON]

Output shape (kept compact because it ships in the app):
  {
    "pages": [                         # 548 pages, in order
      [                                # one page = up to 16 lines
        ["s", 2],                      # surah name banner for surah 2
        ["b"],                         # bismillah line
        ["a", 0, ["word", "word"]],    # ayah line; 1 = centered
      ],
    ],
    "surahStart": [1, 2, ...],         # first page of each surah (114)
    "juzStart": [1, 2, 20, ...]        # first page of each juz (30)
  }
"""

import json
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def clean(word: str) -> str:
    # The source carries a stray byte-order mark that renders as a gap.
    return word.replace("﻿", "").strip()


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    layout_db, words_db = sys.argv[1], sys.argv[2]
    out = Path(sys.argv[3]) if len(sys.argv) > 3 else ROOT / "assets/quran_16_line.json"

    words = {}
    word_ayah = {}
    with sqlite3.connect(words_db) as db:
        for wid, surah, ayah, text in db.execute(
            "select id, surah, ayah, text from words order by id"
        ):
            words[wid] = text
            word_ayah[wid] = (surah, ayah)

    # Juz boundaries come from the quran.json the app already ships.
    quran = json.loads((ROOT / "assets/quran.json").read_text())
    ayah_juz = {}
    for surah in quran["data"]["surahs"]:
        for ayah in surah["ayahs"]:
            ayah_juz[(surah["number"], ayah["numberInSurah"])] = ayah["juz"]

    pages = []
    surah_start = {}
    juz_start = {}
    with sqlite3.connect(layout_db) as db:
        rows = db.execute(
            "select page_number, line_number, line_type, is_centered, "
            "first_word_id, last_word_id, surah_number "
            "from pages order by page_number, line_number"
        ).fetchall()

    for page, _line, kind, centered, first, last, surah in rows:
        while len(pages) < page:
            pages.append([])
        lines = pages[page - 1]
        if kind == "surah_name":
            lines.append(["s", int(surah)])
            surah_start.setdefault(int(surah), page)
        elif kind == "basmallah":
            lines.append(["b"])
        elif kind == "ayah":
            first, last = int(first), int(last)
            lines.append(
                ["a", int(centered or 0), [clean(words[w]) for w in range(first, last + 1)]]
            )
            for w in range(first, last + 1):
                juz = ayah_juz[word_ayah[w]]
                juz_start.setdefault(juz, page)
                surah_start.setdefault(word_ayah[w][0], page)
        else:
            sys.exit(f"Unknown line type {kind!r} on page {page}")

    assert len(pages) == 548, len(pages)
    assert sorted(surah_start) == list(range(1, 115)), "missing surahs"
    assert sorted(juz_start) == list(range(1, 31)), "missing juz"

    data = {
        "pages": pages,
        "surahStart": [surah_start[s] for s in range(1, 115)],
        "juzStart": [juz_start[j] for j in range(1, 31)],
    }
    out.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    print(f"Wrote {out} ({out.stat().st_size / 1024 / 1024:.2f} MB)")


if __name__ == "__main__":
    main()
