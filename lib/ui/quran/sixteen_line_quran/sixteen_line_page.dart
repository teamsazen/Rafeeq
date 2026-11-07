import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:rafeeq/data/providers/surah_info_provider.dart';
import 'package:rafeeq/l10n/app_localization_ext.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/utils/extensions/number_localization.dart';
import 'package:rafeeq/utils/juz_names_provider.dart';
import 'package:rafeeq/utils/surah_names_provider.dart';

const _quranFont = 'IndoPak Nastaleeq';

/// The font has a single, fairly light weight. A fine outline, as a share
/// of the font size, thickens it to sit between that and a full bold.
const _quranStroke = 0.02;
const _surahNameFont = 'Surah Names';
const _linesPerPage = 16;
const _lineInset = 8.0;
// Set word by word: this font's spaces are too narrow on their own.
const _bismillah = AyahLine([
  'بِسْمِ',
  'اللّٰهِ',
  'الرَّحْمٰنِ',
  'الرَّحِیْمِ',
], centered: true);

/// Page colours, taken from the app's light or dark palette.
@immutable
class SixteenLinePageColors {
  const SixteenLinePageColors({
    required this.background,
    required this.text,
    required this.accent,
    required this.frame,
  });

  SixteenLinePageColors.of(SettingsPalette palette)
    : background = palette.background,
      text = palette.title,
      accent = palette.accent,
      frame = palette.selectedCard;

  final Color background;
  final Color text;
  final Color accent;
  final Color frame;

  @override
  bool operator ==(Object other) =>
      other is SixteenLinePageColors &&
      other.background == background &&
      other.text == text &&
      other.accent == accent &&
      other.frame == frame;

  @override
  int get hashCode => Object.hash(background, text, accent, frame);
}

/// One mushaf page: a framed block of 16 lines, with the surah and juz
/// above it and the page number below.
class SixteenLinePage extends ConsumerWidget {
  const SixteenLinePage({
    super.key,
    required this.quran,
    required this.page,
    required this.colors,
  });

  final SixteenLineQuran quran;
  final int page;
  final SixteenLinePageColors colors;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final surahs = ref.watch(surahInfoProvider);
    final surah = quran.surahForPage(page);
    final juz = quran.juzForPage(page);
    final detailStyle = TextStyle(
      color: colors.text.withValues(alpha: 0.72),
      fontFamily: isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans',
      fontSize: 12,
      height: 1.2,
    );

    return Padding(
      padding: const EdgeInsets.fromLTRB(10, 6, 10, 6),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 6, 4, 6),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    isUrdu
                        ? SurahNamesProvider.getUrduName(surah)
                        : surahs[surah - 1].englishName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: detailStyle,
                  ),
                ),
                Text(
                  isUrdu
                      ? JuzNamesProvider.getUrduOrdinalName(juz)
                      : '${context.loc.juz} $juz',
                  style: detailStyle,
                ),
              ],
            ),
          ),
          Expanded(
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: colors.frame, width: 1.4),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Padding(
                padding: const EdgeInsets.all(3),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    border: Border.all(
                      color: colors.frame.withValues(alpha: 0.45),
                      width: 0.6,
                    ),
                    borderRadius: BorderRadius.circular(2),
                  ),
                  child: _PageLines(
                    lines: quran.pages[page - 1],
                    colors: colors,
                    surahNames: [for (final s in surahs) s.name],
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              page.toLocalizedNumber(context),
              key: const ValueKey('sixteen-line-page-number'),
              style: detailStyle.copyWith(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _PageLines extends StatelessWidget {
  const _PageLines({
    required this.lines,
    required this.colors,
    required this.surahNames,
  });

  final List<SixteenLine> lines;
  final SixteenLinePageColors colors;
  final List<String> surahNames;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final lineHeightGuess = constraints.maxHeight / _linesPerPage;
        // Waqf signs after a line's last word hang past it on the left,
        // so that side keeps extra room inside the frame.
        final endInset = _lineInset + lineHeightGuess * 0.22;
        final width = constraints.maxWidth - _lineInset - endInset;
        // The two opening pages hold only a few lines, printed larger.
        final slots = lines.length <= 8 ? lines.length + 6 : _linesPerPage;
        final lineHeight = constraints.maxHeight / slots;
        final layout = _layoutFor(lines, width, lineHeight);
        final fontSize = layout.fontSize;
        final style = TextStyle(
          fontFamily: _quranFont,
          fontSize: fontSize,
          color: colors.text,
        );

        return Directionality(
          textDirection: TextDirection.rtl,
          child: MediaQuery.withNoTextScaling(
            child: Column(
              // Opening pages and surah ends have fewer than 16 lines.
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                for (final (index, line) in lines.indexed)
                  Container(
                    height: lineHeight,
                    width: constraints.maxWidth,
                    padding: EdgeInsetsDirectional.only(
                      start: _lineInset,
                      end: line is SurahNameLine ? _lineInset : endInset,
                    ),
                    // Ruled like a printed mushaf: a hairline under every
                    // line but the last, edge to edge inside the frame.
                    decoration: index == lines.length - 1
                        ? null
                        : BoxDecoration(
                            border: Border(
                              bottom: BorderSide(
                                color: colors.frame.withValues(alpha: 0.35),
                                width: 0.6,
                              ),
                            ),
                          ),
                    child: switch (line) {
                      SurahNameLine(:final surahNumber) => _SurahTitle(
                        surahNumber: surahNumber,
                        name: surahNames[surahNumber - 1],
                        color: colors.accent,
                      ),
                      BismillahLine() => _AyahLineView(
                        line: _bismillah,
                        style: style,
                        width: width,
                        colors: colors,
                      ),
                      AyahLine() => _AyahLineView(
                        line: line,
                        style: style,
                        width: width,
                        naturalWidth: layout.naturalWidths[index],
                        colors: colors,
                      ),
                    },
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// The font size of a page and the natural width of each of its lines at
/// that size.
class _PageLayout {
  const _PageLayout(
    this.width,
    this.lineHeight,
    this.fontSize,
    this.naturalWidths,
  );

  final double width;
  final double lineHeight;
  final double fontSize;

  /// Per line, in page order; zero for lines that are not ayah lines.
  final List<double> naturalWidths;
}

/// Measuring every word is the costly part of building a page, so the
/// result is kept with the page's lines and reused while the size holds.
final _layouts = Expando<_PageLayout>('sixteen-line-layout');

_PageLayout _layoutFor(
  List<SixteenLine> lines,
  double width,
  double lineHeight,
) {
  final cached = _layouts[lines];
  if (cached != null &&
      cached.width == width &&
      cached.lineHeight == lineHeight) {
    return cached;
  }
  // One font size for the whole page, so every line reads the same: as
  // large as the line height allows, shrunk until the widest line fits.
  // Widths grow in step with the font size, so one measurement serves both.
  final base = lineHeight * 0.82;
  final probe = TextStyle(fontFamily: _quranFont, fontSize: base);
  final probed = [
    for (final line in lines)
      line is AyahLine ? _naturalWidth(line.words, probe) : 0.0,
  ];
  final widest = probed.fold(0.0, math.max);
  final scale = widest <= 0 ? 1.0 : math.min(1.0, (width * 0.97) / widest);
  return _layouts[lines] = _PageLayout(width, lineHeight, base * scale, [
    for (final w in probed) w * scale,
  ]);
}

double _gap(TextStyle style) => style.fontSize! * 0.14;

double _wordWidth(String word, TextStyle style) {
  final painter = TextPainter(
    text: TextSpan(text: word, style: style),
    textDirection: TextDirection.rtl,
    maxLines: 1,
  )..layout();
  final width = painter.width;
  painter.dispose();
  return width;
}

/// Width of the words set with the minimum gap between them.
double _naturalWidth(List<String> words, TextStyle style) {
  var total = 0.0;
  for (final word in words) {
    total += _wordWidth(word, style);
  }
  return total + _gap(style) * math.max(0, words.length - 1);
}

class _AyahLineView extends StatelessWidget {
  const _AyahLineView({
    required this.line,
    required this.style,
    required this.width,
    required this.colors,
    this.naturalWidth,
  });

  final AyahLine line;
  final TextStyle style;
  final double width;

  /// The line's width with minimum gaps, when already measured.
  final double? naturalWidth;
  final SixteenLinePageColors colors;

  @override
  Widget build(BuildContext context) {
    final children = [
      for (final word in line.words) _QuranWord(word, style: style),
    ];

    if (line.centered) {
      return Center(
        child: FittedBox(
          fit: BoxFit.scaleDown,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            spacing: _gap(style),
            children: children,
          ),
        ),
      );
    }

    // Justify like a printed mushaf: the words fill the line edge to edge.
    // A line too long for the width is shrunk instead of overflowing.
    final natural = naturalWidth ?? _naturalWidth(line.words, style);
    final row = SizedBox(
      width: math.max(width, natural),
      child: Row(
        mainAxisAlignment: line.words.length == 1
            ? MainAxisAlignment.center
            : MainAxisAlignment.spaceBetween,
        children: children,
      ),
    );
    if (natural <= width) return row;
    return FittedBox(fit: BoxFit.scaleDown, child: row);
  }
}

/// A word drawn twice: a thin outline under the fill, to thicken the
/// font's strokes.
class _QuranWord extends StatelessWidget {
  const _QuranWord(this.word, {required this.style});

  final String word;
  final TextStyle style;

  @override
  Widget build(BuildContext context) {
    final outline = style.copyWith(
      foreground: Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = style.fontSize! * _quranStroke
        ..strokeJoin = StrokeJoin.round
        ..color = style.color!,
    );
    return Stack(
      children: [
        Text(word, style: outline, overflow: TextOverflow.visible),
        Text(word, style: style, overflow: TextOverflow.visible),
      ],
    );
  }
}

/// The surah heading, in the calligraphy of QUL's surah name font.
///
/// The font turns the text "surah001" to "surah114" into the heading for
/// that surah, so it is drawn in the accent colour without a box around it.
class _SurahTitle extends StatelessWidget {
  const _SurahTitle({
    required this.surahNumber,
    required this.name,
    required this.color,
  });

  final int surahNumber;

  /// The surah's Arabic name, read out by screen readers.
  final String name;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Semantics(
          label: name,
          excludeSemantics: true,
          child: Center(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                'surah${surahNumber.toString().padLeft(3, '0')}',
                textDirection: TextDirection.ltr,
                style: TextStyle(
                  fontFamily: _surahNameFont,
                  fontSize: constraints.maxHeight * 0.95,
                  height: 1,
                  color: color,
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
