#!/usr/bin/env python3
"""Builds assets/quran_16_line.json from the QUL downloads.

Inputs:
  - taj-indopak-16-lines.db  Mushaf layout 11, "Indopak 16 lines (Taj company)",
                             from https://qul.tarteel.ai (log in to download)
  - indopak-nastaleeq.db     Quran script 59, "Indopak Nastaleeq script - Word
                             by Word", also from QUL. The layout names its
                             words by this script's word ids.
  - IP-H-WA.txt              QuranWBW's IndoPak v5.4 Hanafi text, word by word
                             with ayah signs ("Data/Word by Word" in their
                             "Indopak v5.4 Hanafi Version" package). This is
                             the text that ships.

The QuranWBW text is used exactly as given: each QUL word id is matched to
its v5.4 word by comparing letters, and three places where v5.4 writes
"بَعْدَ مَا" as one word take both ids.

The text is drawn with the matching AlQuranNeo v5.x font from the same
package, read from assets/fonts/AlQuranNeo5x3-Arabic.ttf. That file is not
committed: QuranWBW gave Rafeeq written permission to use it, but not to
share it. Text and font belong together: each ayah ends with a private-use
character that the font draws as the numbered ayah ring, and the waqf signs
are encoded for this font.

Surah headings use QUL's "Surah name font v2" (resource 455), shipped as
assets/fonts/SurahNameV2.ttf: it draws "surah001" to "surah114" as the
calligraphic name of that surah.

Usage:
  python3 scripts/build_sixteen_line_quran.py LAYOUT_DB WORDS_DB WBW_TXT [OUT_JSON]

Output shape (kept compact because it ships in the app):
  {
    "pages": [                         # 548 pages, in order
      [                                # one page = up to 16 lines
        ["s", 2],                      # surah name banner for surah 2
        ["b"],                         # bismillah line
        ["a", 0, ["word", "word"]],    # ayah line; 1 = centered
        ["a", 0, [...], [[5, 13]]],    # ...ending ruku 5 of its surah,
                                       # which has 13 ayahs
      ],
    ],
    "surahStart": [1, 2, ...],         # first page of each surah (114)
    "juzStart": [1, 2, 20, ...],       # first page of each juz (30)
    "bismillah": ["word", ...],        # the bismillah, without its sign
    "quarters": [[1, 2], ...]          # per juz, where its start, rub',
                                       # nisf and thalatha begin, as
                                       # [page, line index on the page]
  }
"""

import difflib
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def clean(word: str) -> str:
    # The source carries a stray byte-order mark that renders as a gap.
    return word.replace("﻿", "").strip()


def letters(word: str) -> str:
    # Drops ayah and waqf signs (private-use and Arabic pause marks) and
    # spaces, leaving what both texts agree on.
    return re.sub(r"[\ue000-\uf8ff\u0610-\u061a\u06d6-\u06ed\s]", "", word)


def match_words(ids: list, qul: list, wbw: list) -> dict:
    """Maps each QUL word id to the index of its QuranWBW word."""
    matcher = difflib.SequenceMatcher(
        None, [letters(w) for w in qul], [letters(w) for w in wbw], autojunk=False
    )
    found = {}
    for op, i1, i2, j1, j2 in matcher.get_opcodes():
        if op in ("equal", "replace") and i2 - i1 == j2 - j1:
            for k in range(i2 - i1):
                found[ids[i1 + k]] = j1 + k
        elif op == "replace" and j2 - j1 == 1 and letters("".join(qul[i1:i2])) == letters(wbw[j1]):
            # One v5.4 word for several QUL words.
            for k in range(i2 - i1):
                found[ids[i1 + k]] = j1
        else:
            sys.exit(f"Cannot match QUL words {qul[i1:i2]} to {wbw[j1:j2]}")
    assert len(found) == len(ids), "unmatched words"
    return found


def main() -> None:
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    layout_db, words_db, wbw_txt = sys.argv[1], sys.argv[2], sys.argv[3]
    out = Path(sys.argv[4]) if len(sys.argv) > 4 else ROOT / "assets/quran_16_line.json"

    ids, qul = [], []
    word_ayah = {}
    with sqlite3.connect(words_db) as db:
        for wid, surah, ayah, text in db.execute(
            "select id, surah, ayah, text from words order by id"
        ):
            ids.append(wid)
            qul.append(clean(text))
            word_ayah[wid] = (surah, ayah)

    wbw = [clean(line) for line in Path(wbw_txt).read_text(encoding="utf-8").splitlines()]
    wbw = [w for w in wbw if w]
    wbw_index = match_words(ids, qul, wbw)
    used = set()

    # Juz boundaries come from the quran.json the app already ships.
    quran = json.loads((ROOT / "assets/quran.json").read_text())
    ayah_juz = {}
    # Rukus too: the ayah that closes each one, with the ruku's number in
    # its surah and how many ayahs it holds. The mushaf marks it with ع in
    # the margin.
    ruku_end = {}
    # And the quarters of each juz, numbered 0 to 119 in juz order. A juz
    # starts where its "juz" field says, which follows the IndoPak paras
    # (para 7, "wa idha sami'u", opens at 5:83). Its rub' (1/4), nisf (1/2)
    # and thalatha (3/4) open its 3rd, 5th and 7th hizb quarters.
    quarter_of = {}
    for surah in quran["data"]["surahs"]:
        ayahs = surah["ayahs"]
        number_in_surah, first = 0, 0
        for i, ayah in enumerate(ayahs):
            ayah_juz[(surah["number"], ayah["numberInSurah"])] = ayah["juz"]
            key = (surah["number"], ayah["numberInSurah"])
            juz, hizb_quarter = ayah["juz"], ayah["hizbQuarter"]
            if (juz - 1) * 4 not in quarter_of.values():
                quarter_of[key] = (juz - 1) * 4
            within = (hizb_quarter - 1) % 8
            quarter = (hizb_quarter - 1) // 8 * 4 + within // 2
            if within in (2, 4, 6) and quarter not in quarter_of.values():
                quarter_of[key] = quarter
            last = i + 1 == len(ayahs) or ayahs[i + 1]["ruku"] != ayah["ruku"]
            if last:
                number_in_surah += 1
                ruku_end[(surah["number"], ayah["numberInSurah"])] = [
                    number_in_surah,
                    i + 1 - first,
                ]
                first = i + 1
    last_word = {}
    first_word = {}
    for wid in ids:
        last_word[word_ayah[wid]] = wid
        first_word.setdefault(word_ayah[wid], wid)
    quarter_word = {first_word[ayah]: q for ayah, q in quarter_of.items()}
    quarters = {}

    pages = []
    surah_start = {}
    ruku_count = 0
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
            for w in range(first, last + 1):
                if w in quarter_word:
                    quarters[quarter_word[w]] = [page, len(lines)]
            line_words = []
            for w in range(first, last + 1):
                index = wbw_index[w]
                if index in used:
                    continue  # one v5.4 word standing for two QUL words
                used.add(index)
                line_words.append(wbw[index])
            line = ["a", int(centered or 0), line_words]
            rukus = [
                ruku_end[word_ayah[w]]
                for w in range(first, last + 1)
                if last_word[word_ayah[w]] == w and word_ayah[w] in ruku_end
            ]
            if rukus:
                line.append(rukus)
                ruku_count += len(rukus)
            lines.append(line)
            for w in range(first, last + 1):
                juz = ayah_juz[word_ayah[w]]
                juz_start.setdefault(juz, page)
                surah_start.setdefault(word_ayah[w][0], page)
        else:
            sys.exit(f"Unknown line type {kind!r} on page {page}")

    assert len(pages) == 548, len(pages)
    assert sorted(surah_start) == list(range(1, 115)), "missing surahs"
    assert sorted(juz_start) == list(range(1, 31)), "missing juz"
    assert ruku_count == len(ruku_end), "rukus left off the pages"
    assert sorted(quarters) == list(range(120)), "missing quarters"
    for juz in range(30):
        assert quarters[juz * 4][0] == juz_start[juz + 1], f"juz {juz + 1}"
    assert used == set(range(len(wbw))), "v5.4 words left out of the layout"

    # Al-Fatiha's first ayah is the bismillah; its last word is the sign.
    fatiha = [wbw[wbw_index[i]] for i in ids if word_ayah[i] == (1, 1)]
    bismillah = fatiha[:-1]

    data = {
        "pages": pages,
        "surahStart": [surah_start[s] for s in range(1, 115)],
        "juzStart": [juz_start[j] for j in range(1, 31)],
        "bismillah": bismillah,
        "quarters": [quarters[q] for q in sorted(quarters)],
    }
    out.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    print(f"Wrote {out} ({out.stat().st_size / 1024 / 1024:.2f} MB)")


if __name__ == "__main__":
    main()
