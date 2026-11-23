#!/usr/bin/env python3
"""Finds the 16 line mushaf's ayah ends that have no numbered ring.

The IndoPak text ends almost every ayah with a private-use character the
font draws as its numbered ring (U+F500 for ayah 1 to U+F61D for 286). In
29 places the ring is a sajdah variant or missing, so the native app cannot
tell where those ayahs end. This matches each such ayah's last word in
assets/quran.json to the 16 line text, checks the next word starts the
following ayah, and prints the ends as Kotlin for SixteenLine.kt.
Run from the repo root.
"""
import difflib
import json
import re

d = json.load(open('assets/quran_16_line.json'))
q = json.load(open('assets/quran.json'))['data']['surahs']


def letters(w):
    w = re.sub(r"[-ؐ-ًؚ-ٰٟۖ-ۭـ\s]", "", w)
    for a, b in [('ٱ', 'ا'), ('أ', 'ا'), ('إ', 'ا'), ('آ', 'ا'), ('ى', 'ي'), ('ی', 'ي'), ('ۃ', 'ة'), ('ہ', 'ه'), ('ک', 'ك')]:
        w = w.replace(a, b)
    return w


def ring(w):
    for ch in w:
        if 0xF500 <= ord(ch) <= 0xF61D:
            return ord(ch) - 0xF500 + 1


def similar(a, b):
    return difflib.SequenceMatcher(None, letters(a), letters(b)).ratio()


seq, surah = [], 1
for p, page in enumerate(d['pages']):
    for l, line in enumerate(page):
        if line[0] == 's':
            surah = line[1]
        elif line[0] == 'a':
            for wi, w in enumerate(line[2]):
                seq.append((p, l, wi, w, surah))

def close_surah(s, last, end):
    """A surah whose last ayah has no numbered ring ends on its last word, the ring variant."""
    count = len(q[s - 1]['ayahs'])
    if s == 1 or last != count - 1:
        return
    words = [w for w in q[s - 1]['ayahs'][count - 1]['text'].split() if letters(w)]
    p, l, wi, w, _ = seq[end]
    before = seq[end - 1][3] if not letters(w) else w
    assert similar(before, words[-1]) > .9, (s, count)
    ends.append((p, l, wi, s, count))


ends, cur, start = [], (0, 0), 0
for i, (p, l, wi, w, s) in enumerate(seq):
    if s != cur[0]:
        if cur[0]:
            close_surah(cur[0], cur[1], i - 1)
        cur, start = (s, 0), i
    a = ring(w)
    if not a:
        continue
    if a == cur[1] + 2 and s != 1:
        n = cur[1] + 1
        # Signs such as ۩ and ۞ stand as words of their own there; only words with letters count.
        words = [w for w in q[s - 1]['ayahs'][n - 1]['text'].split() if letters(w)]
        following = [w for w in q[s - 1]['ayahs'][n]['text'].split() if letters(w)][0]
        span = seq[start:i]
        # The last word, then maybe a sign-only word (a ring variant), then the next ayah.
        found = None
        for k in range(len(span) - 1):
            if similar(span[k][3], words[-1]) <= .9:
                continue
            end = k + 1 if not letters(span[k + 1][3]) else k
            if end + 1 < len(span) and similar(span[end + 1][3], following) > .6:
                found = end
        assert found is not None, (s, n)
        ends.append((span[found][0], span[found][1], span[found][2], s, n))
    cur, start = (s, a), i + 1

close_surah(cur[0], cur[1], len(seq) - 1)
ends.sort()
print(f"// {len(ends)} ayah ends without a numbered ring: page index, line, word, surah * 1000 + ayah.")
for p, l, w, s, n in ends:
    print(f"    Triple({p}, {l}, {w}) to {s * 1000 + n},")
