import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

/// Text and layout of the IndoPak 16 line (Taj Company) mushaf.
///
/// The layout comes from the Quranic Universal Library (qul.tarteel.ai) and
/// the text is QuranWBW's IndoPak v5.4, used with their permission. Built by
/// `scripts/build_sixteen_line_quran.py`.
const sixteenLineQuranAsset = 'assets/quran_16_line.json';

sealed class SixteenLine {
  const SixteenLine();
}

/// The decorated banner that opens a surah.
class SurahNameLine extends SixteenLine {
  const SurahNameLine(this.surahNumber);

  final int surahNumber;
}

class BismillahLine extends SixteenLine {
  const BismillahLine();
}

/// The end of a ruku, marked with ع in the margin of its line.
@immutable
class RukuEnd {
  const RukuEnd({required this.numberInSurah, required this.ayahCount});

  /// Which ruku of its surah this is, from 1.
  final int numberInSurah;

  /// How many ayahs the ruku holds.
  final int ayahCount;
}

class AyahLine extends SixteenLine {
  const AyahLine(this.words, {required this.centered, this.ruku});

  /// Words in reading order. Ayah end signs are words of their own.
  final List<String> words;

  /// Short lines (end of a surah) sit in the middle instead of filling
  /// the width.
  final bool centered;

  /// Set when a ruku ends on this line.
  final RukuEnd? ruku;
}

/// A place to open the reader at: a page and, when known, the line on it
/// to point out.
@immutable
class SixteenLinePosition {
  const SixteenLinePosition(this.page, [this.line]);

  final int page;

  /// Index into the page's lines.
  final int? line;

  @override
  bool operator ==(Object other) =>
      other is SixteenLinePosition && other.page == page && other.line == line;

  @override
  int get hashCode => Object.hash(page, line);

  @override
  String toString() => 'SixteenLinePosition($page, $line)';
}

/// The four places a juz is read in: its start, then rub' (a quarter),
/// nisf (half) and thalatha (three quarters), as marked in the margin of
/// an IndoPak mushaf.
enum JuzQuarter { start, rub, nisf, thalatha }

class SixteenLineQuran {
  const SixteenLineQuran({
    required this.pages,
    required this.surahStartPages,
    required this.juzStartPages,
    required this.bismillah,
    required this.juzQuarters,
  });

  /// Page n is `pages[n - 1]`.
  final List<List<SixteenLine>> pages;

  /// First page of surah n is `surahStartPages[n - 1]`.
  final List<int> surahStartPages;

  /// First page of juz n is `juzStartPages[n - 1]`.
  final List<int> juzStartPages;

  /// The bismillah over each surah, word by word, as the text gives it.
  final AyahLine bismillah;

  /// Where each quarter of each juz begins: juz n's quarters are
  /// `juzQuarters[(n - 1) * 4]` onwards, in [JuzQuarter] order.
  final List<SixteenLinePosition> juzQuarters;

  int get pageCount => pages.length;

  SixteenLinePosition juzQuarter(int juz, JuzQuarter quarter) =>
      juzQuarters[(juz - 1) * 4 + quarter.index];

  /// The banner that opens [surah].
  SixteenLinePosition surahStart(int surah) {
    final page = surahStartPages[surah - 1];
    final line = pages[page - 1].indexWhere(
      (line) => line is SurahNameLine && line.surahNumber == surah,
    );
    return SixteenLinePosition(page, line < 0 ? null : line);
  }

  int juzForPage(int page) => _lastStartAtOrBefore(juzStartPages, page);

  /// The surah being read at the top of [page].
  int surahForPage(int page) {
    final lines = pages[page - 1];
    final opensWithSurah = lines.isNotEmpty && lines.first is SurahNameLine;
    if (opensWithSurah) return (lines.first as SurahNameLine).surahNumber;
    // A surah can start lower on the page, so look for the previous start.
    var surah = 1;
    for (var i = 0; i < surahStartPages.length; i++) {
      if (surahStartPages[i] < page) surah = i + 1;
    }
    return surah;
  }

  static int _lastStartAtOrBefore(List<int> starts, int page) {
    var number = 1;
    for (var i = 0; i < starts.length; i++) {
      if (starts[i] <= page) number = i + 1;
    }
    return number;
  }

  factory SixteenLineQuran.fromJson(Map<String, dynamic> json) {
    final pages = (json['pages'] as List).map((page) {
      return (page as List).map<SixteenLine>((raw) {
        final line = raw as List;
        return switch (line[0]) {
          's' => SurahNameLine(line[1] as int),
          'b' => const BismillahLine(),
          _ => AyahLine(
            (line[2] as List).cast<String>(),
            centered: line[1] == 1,
            ruku: line.length > 3 ? _rukuEnd(line[3] as List) : null,
          ),
        };
      }).toList();
    }).toList();
    return SixteenLineQuran(
      pages: pages,
      surahStartPages: (json['surahStart'] as List).cast<int>(),
      juzStartPages: (json['juzStart'] as List).cast<int>(),
      bismillah: AyahLine(
        (json['bismillah'] as List).cast<String>(),
        centered: true,
      ),
      juzQuarters: [
        for (final raw in json['quarters'] as List)
          SixteenLinePosition((raw as List)[0] as int, raw[1] as int),
      ],
    );
  }
}

RukuEnd _rukuEnd(List rukus) {
  final ruku = rukus.first as List;
  return RukuEnd(numberInSurah: ruku[0] as int, ayahCount: ruku[1] as int);
}

SixteenLineQuran _parse(String source) =>
    SixteenLineQuran.fromJson(jsonDecode(source) as Map<String, dynamic>);

final sixteenLineQuranProvider = FutureProvider<SixteenLineQuran>((ref) async {
  final source = await rootBundle.loadString(sixteenLineQuranAsset);
  return compute(_parse, source);
});

/// Whether [word] ends an ayah. The text marks it with a private-use
/// character, U+F500 for ayah 1 up to U+F61D for ayah 286, which the font
/// draws as the numbered ring. U+F61E is the bismillah's empty ring.
bool isAyahEnd(String word) =>
    word.runes.any((rune) => rune >= 0xF500 && rune <= 0xF61E);
