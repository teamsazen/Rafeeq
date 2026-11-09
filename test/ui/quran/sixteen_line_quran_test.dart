import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:rafeeq/core/navigation/app_routes.dart';
import 'package:rafeeq/data/providers/shared_prefs_provider.dart';
import 'package:rafeeq/l10n/app_localizations.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_page.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  late SixteenLineQuran quran;

  setUpAll(() {
    final source = File(sixteenLineQuranAsset).readAsStringSync();
    quran = SixteenLineQuran.fromJson(
      jsonDecode(source) as Map<String, dynamic>,
    );
  });

  group('bundled 16 line data', () {
    test('has every page, surah and juz', () {
      expect(quran.pageCount, 548);
      expect(quran.surahStartPages, hasLength(114));
      expect(quran.juzStartPages, hasLength(30));
      for (final page in quran.pages) {
        expect(page.length, inInclusiveRange(1, 16));
      }
    });

    test('knows the surah and juz of a page', () {
      expect(quran.surahForPage(1), 1);
      expect(quran.surahForPage(2), 2);
      expect(quran.surahForPage(19), 2);
      expect(quran.juzForPage(19), 1);
      expect(quran.juzForPage(20), 2);
      expect(quran.juzForPage(548), 30);
      expect(quran.surahStartPages.last, 548);
    });

    test('opens with Al-Fatiha and its seven ayahs', () {
      final first = quran.pages.first;
      expect(first.first, isA<SurahNameLine>());
      final ayahEnds = first
          .whereType<AyahLine>()
          .expand((line) => line.words)
          .where(isAyahEnd);
      expect(ayahEnds, hasLength(7));
    });

    test('marks where every ruku ends', () {
      final rukus = [
        for (final page in quran.pages)
          for (final line in page.whereType<AyahLine>())
            if (line.ruku != null) line.ruku!,
      ];
      expect(rukus, hasLength(556));
      // Al-Baqara's second ruku, ayahs 8 to 20, ends on page 4.
      final page4 = quran.pages[3].whereType<AyahLine>();
      final ruku = page4.firstWhere((line) => line.ruku != null).ruku!;
      expect(ruku.numberInSurah, 2);
      expect(ruku.ayahCount, 13);
    });

    test('knows where each quarter of a juz begins', () {
      expect(quran.juzQuarters, hasLength(120));
      for (var juz = 1; juz <= 30; juz++) {
        final start = quran.juzQuarter(juz, JuzQuarter.start);
        expect(start.page, quran.juzStartPages[juz - 1]);
        final pages = [
          for (final quarter in JuzQuarter.values)
            quran.juzQuarter(juz, quarter).page,
        ];
        expect(pages, orderedEquals([...pages]..sort()), reason: 'juz $juz');
      }
      // Juz 1: rub' opens with 2:44 on page 7, nisf with 2:75 on page 10,
      // thalatha with 2:106 on page 15.
      expect(
        quran.juzQuarter(1, JuzQuarter.rub),
        const SixteenLinePosition(7, 1),
      );
      expect(quran.juzQuarter(1, JuzQuarter.nisf).page, 10);
      expect(quran.juzQuarter(1, JuzQuarter.thalatha).page, 15);
    });

    test('finds the banner that opens a surah', () {
      final position = quran.surahStart(2);
      final line = quran.pages[position.page - 1][position.line!];
      expect((line as SurahNameLine).surahNumber, 2);
    });

    test('takes the bismillah from the text itself', () {
      final fatiha = quran.pages.first.whereType<AyahLine>().first.words;
      expect(quran.bismillah.words, fatiha.take(4).toList());
      expect(quran.bismillah.words.where(isAyahEnd), isEmpty);
    });
  });

  group('reader', () {
    late SharedPreferences preferences;

    Future<void> pumpReader(
      WidgetTester tester, {
      Map<String, Object> prefs = const {},
      Widget home = const SixteenLineQuranScreen(),
    }) async {
      SharedPreferences.setMockInitialValues(prefs);
      preferences = await SharedPreferences.getInstance();
      await tester.binding.setSurfaceSize(const Size(390, 844));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(preferences),
            sixteenLineQuranProvider.overrideWith((ref) async => quran),
          ],
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            onGenerateRoute: AppRoutes.onGenerateRoute,
            home: home,
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('opens on the last page read', (tester) async {
      await pumpReader(tester, prefs: {'sixteen_line_quran_last_page': 45});

      expect(find.text('Al-Baqara'), findsWidgets);
      expect(find.textContaining('Page 45'), findsOneWidget);
    });

    testWidgets('remembers the page after swiping', (tester) async {
      await pumpReader(tester);
      expect(find.textContaining('Page 1'), findsOneWidget);

      // Right to left: the next page comes in from the left.
      await tester.fling(find.byType(PageView), const Offset(300, 0), 1000);
      await tester.pumpAndSettle();

      expect(find.textContaining('Page 2'), findsOneWidget);
      expect(preferences.getInt('sixteen_line_quran_last_page'), 2);
    });

    testWidgets('jumps to a juz from the index', (tester) async {
      await pumpReader(tester);

      await tester.tap(find.byKey(const ValueKey('sixteen-line-index-action')));
      await tester.pumpAndSettle();
      expect(
        find.textContaining('used with their permission'),
        findsOneWidget,
        reason: 'QuranWBW asks for credit wherever their text is used',
      );
      await tester.tap(find.text('Juz'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-2')));
      await tester.pumpAndSettle();
      expect(find.text('Quarter'), findsOneWidget);
      expect(find.text('Half'), findsOneWidget);
      expect(find.text('Third Quarter'), findsOneWidget);
      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-2-start')));
      await tester.pumpAndSettle();

      expect(find.textContaining('Juz 2 · Page 20'), findsOneWidget);
    });

    testWidgets('jumps to a quarter of a juz and points out its line', (
      tester,
    ) async {
      await pumpReader(tester);

      await tester.tap(find.byKey(const ValueKey('sixteen-line-index-action')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Juz'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-1-rub')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(find.textContaining('Page 7'), findsOneWidget);
      final tint = tester.widget<DecoratedBox>(
        find
            .descendant(
              of: find.byKey(const ValueKey('sixteen-line-highlight-1')),
              matching: find.byType(DecoratedBox),
            )
            .first,
      );
      expect((tint.decoration as BoxDecoration).color?.a, greaterThan(0));

      // Then it fades.
      await tester.pumpAndSettle();
      final faded = tester.widget<DecoratedBox>(
        find
            .descendant(
              of: find.byKey(const ValueKey('sixteen-line-highlight-1')),
              matching: find.byType(DecoratedBox),
            )
            .first,
      );
      expect((faded.decoration as BoxDecoration).color?.a, 0);
    });

    testWidgets('searches surahs and juz by name', (tester) async {
      await pumpReader(tester);

      await tester.tap(find.byKey(const ValueKey('sixteen-line-index-action')));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const ValueKey('sixteen-line-search')),
        'yaseen',
      );
      await tester.pump();
      expect(find.byKey(const ValueKey('sixteen-line-surah-36')), findsOne);
      expect(find.byKey(const ValueKey('sixteen-line-surah-1')), findsNothing);

      await tester.enterText(
        find.byKey(const ValueKey('sixteen-line-search')),
        'tilkal',
      );
      await tester.tap(find.text('Juz'));
      await tester.pumpAndSettle();
      expect(find.byKey(const ValueKey('sixteen-line-juz-3')), findsOne);
      expect(find.byKey(const ValueKey('sixteen-line-juz-1')), findsNothing);

      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-3')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('sixteen-line-juz-3-start')));
      await tester.pumpAndSettle();
      expect(find.textContaining('Juz 3 · Page 38'), findsOneWidget);
    });

    testWidgets('jumps to a typed page number', (tester) async {
      await pumpReader(tester);

      await tester.tap(find.byKey(const ValueKey('sixteen-line-index-action')));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const ValueKey('sixteen-line-search')),
        '300',
      );
      await tester.pump();
      await tester.tap(find.byKey(const ValueKey('sixteen-line-page-go')));
      await tester.pumpAndSettle();

      expect(find.textContaining('Page 300'), findsOneWidget);
    });

    testWidgets('a short quick swipe turns the page and stays', (tester) async {
      await pumpReader(tester);
      // Lifted well before half way, carried over by its speed.
      await tester.fling(find.byType(PageView), const Offset(90, 0), 1500);
      await tester.pumpAndSettle();

      expect(find.textContaining('Page 2'), findsOneWidget);
    });

    testWidgets('pinching zooms the page and holds the page turning', (
      tester,
    ) async {
      await pumpReader(tester);
      final center = tester.getCenter(find.byType(PageView));
      final first = await tester.startGesture(center - const Offset(30, 0));
      final second = await tester.startGesture(
        center + const Offset(30, 0),
        pointer: 2,
      );
      for (var i = 0; i < 8; i++) {
        await first.moveBy(const Offset(-12, 0));
        await second.moveBy(const Offset(12, 0));
        await tester.pump();
      }
      await first.up();
      await second.up();
      await tester.pumpAndSettle();

      double zoom() => tester
          .widget<Transform>(find.byKey(const ValueKey('sixteen-line-zoom')))
          .transform
          .getMaxScaleOnAxis();
      expect(zoom(), greaterThan(1.5));
      expect(zoom(), lessThanOrEqualTo(3));

      // While zoomed in, a swipe moves around the page instead of turning.
      await tester.fling(find.byType(PageView), const Offset(300, 0), 1000);
      await tester.pumpAndSettle();
      expect(find.textContaining('Page 1'), findsOneWidget);
      expect(preferences.getInt('sixteen_line_quran_last_page'), isNull);
    });

    testWidgets('lays out every page without overflowing', (tester) async {
      SharedPreferences.setMockInitialValues({});
      preferences = await SharedPreferences.getInstance();
      for (final size in const [Size(320, 568), Size(430, 932)]) {
        for (var page = 1; page <= quran.pageCount; page++) {
          await tester.pumpWidget(
            ProviderScope(
              overrides: [
                sharedPreferencesProvider.overrideWithValue(preferences),
              ],
              child: MaterialApp(
                home: Center(
                  child: SizedBox.fromSize(
                    size: size,
                    child: SixteenLinePage(
                      quran: quran,
                      page: page,
                      colors: SixteenLinePageColors.of(SettingsPalette.light),
                    ),
                  ),
                ),
              ),
            ),
          );
          expect(tester.takeException(), isNull, reason: 'page $page');
        }
      }
    });
  });
}
