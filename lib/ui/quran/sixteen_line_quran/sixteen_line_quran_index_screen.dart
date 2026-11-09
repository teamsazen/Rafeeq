import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:rafeeq/data/providers/surah_info_provider.dart';
import 'package:rafeeq/l10n/app_localization_ext.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/ui/app_home/home/settings_picker.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart';
import 'package:rafeeq/utils/extensions/number_localization.dart';
import 'package:rafeeq/utils/juz_names_provider.dart';
import 'package:rafeeq/utils/surah_names_provider.dart';

/// Picks a place to open: a surah, a juz or one of its quarters, or a page
/// typed into the search. Pops with the chosen [SixteenLinePosition].
class SixteenLineQuranIndexScreen extends ConsumerStatefulWidget {
  const SixteenLineQuranIndexScreen({super.key, required this.currentPage});

  final int currentPage;

  @override
  ConsumerState<SixteenLineQuranIndexScreen> createState() =>
      _SixteenLineQuranIndexScreenState();
}

class _SixteenLineQuranIndexScreenState
    extends ConsumerState<SixteenLineQuranIndexScreen> {
  final _search = TextEditingController();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final palette = SettingsPalette.of(context);
    final quranAsync = ref.watch(sixteenLineQuranProvider);
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final labelStyle = TextStyle(
      fontFamily: isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans',
      fontWeight: FontWeight.w600,
      fontSize: 15,
    );

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: palette.background,
        appBar: SettingsAppBar(
          title: context.loc.sixteenLineGoTo,
          palette: palette,
          bottom: PreferredSize(
            preferredSize: const Size.fromHeight(48),
            child: Align(
              alignment: AlignmentDirectional.centerStart,
              child: TabBar(
                splashFactory: NoSplash.splashFactory,
                dividerHeight: 0,
                isScrollable: true,
                tabAlignment: TabAlignment.start,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                labelPadding: const EdgeInsets.symmetric(horizontal: 14),
                unselectedLabelColor: palette.subtitle,
                indicatorColor: palette.accent,
                labelColor: palette.title,
                indicatorSize: TabBarIndicatorSize.label,
                labelStyle: labelStyle,
                unselectedLabelStyle: labelStyle.copyWith(
                  fontWeight: FontWeight.w500,
                ),
                tabs: [
                  Tab(text: context.loc.surah),
                  Tab(text: context.loc.juz),
                ],
              ),
            ),
          ),
        ),
        body: switch (quranAsync) {
          AsyncData(:final value) => Column(
            children: [
              _SearchField(
                controller: _search,
                pageCount: value.pageCount,
                palette: palette,
              ),
              Expanded(
                child: ValueListenableBuilder(
                  valueListenable: _search,
                  builder: (context, search, _) {
                    final query = _Query(search.text, value.pageCount);
                    return TabBarView(
                      children: [
                        _SurahList(
                          quran: value,
                          query: query,
                          currentPage: widget.currentPage,
                          palette: palette,
                        ),
                        _JuzList(
                          quran: value,
                          query: query,
                          currentPage: widget.currentPage,
                          palette: palette,
                        ),
                      ],
                    );
                  },
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                child: SafeArea(
                  top: false,
                  child: Text(
                    context.loc.sixteenLineCredit,
                    key: const ValueKey('sixteen-line-credit'),
                    textAlign: TextAlign.center,
                    style: labelStyle.copyWith(
                      color: palette.subtitle,
                      fontWeight: FontWeight.w400,
                      fontSize: 12,
                    ),
                  ),
                ),
              ),
            ],
          ),
          _ => Center(child: CircularProgressIndicator(color: palette.accent)),
        },
      ),
    );
  }
}

/// What was typed, ready to match against names and numbers.
class _Query {
  factory _Query(String text, int pageCount) {
    final key = _searchKey(text);
    final number = int.tryParse(key);
    final isPage = number != null && number >= 1 && number <= pageCount;
    return _Query._(text.trim(), key, number, isPage ? number : null);
  }

  const _Query._(this.text, this.key, this.number, this.page);

  final String text;
  final String key;

  /// Set when only a number was typed.
  final int? number;

  /// [number], when there is such a page.
  final int? page;

  bool get isEmpty => key.isEmpty;

  bool matches(int itemNumber, Iterable<String> names) {
    if (isEmpty) return true;
    if (number != null) return itemNumber == number;
    return names.any((name) => _searchKey(name).contains(key));
  }
}

const _easternDigits = '٠١٢٣٤٥٦٧٨٩';
const _urduDigits = '۰۱۲۳۴۵۶۷۸۹';

/// Reduces [text] to what spellings of a name share, so "Yaseen" finds
/// "Ya-Sin", "baqarah" finds "Al-Baqara" and "يس" finds "یس".
String _searchKey(String text) {
  final buffer = StringBuffer();
  for (final char in text.toLowerCase().split('')) {
    final eastern = _easternDigits.indexOf(char);
    final urdu = _urduDigits.indexOf(char);
    buffer.write(
      eastern >= 0
          ? eastern
          : urdu >= 0
          ? urdu
          : char,
    );
  }
  var key = buffer
      .toString()
      // Vowel marks and Quranic signs.
      .replaceAll(RegExp('[ً-ٰٟۖ-ۭ]'), '')
      // Arabic letter forms to their Urdu ones.
      .replaceAll(RegExp('[يى]'), 'ی')
      .replaceAll('ك', 'ک')
      .replaceAll(RegExp('[ةه]'), 'ہ')
      .replaceAll(RegExp('[أإآٱ]'), 'ا')
      // Spaces, hyphens and apostrophes.
      .replaceAll(RegExp(r'[^a-z0-9؀-ۿ]'), '')
      // Long vowels, written either way.
      .replaceAll('ee', 'i')
      .replaceAll('oo', 'u')
      .replaceAllMapped(RegExp(r'([a-z])\1+'), (match) => match[1]!);
  if (key.length > 2 && key.endsWith('h') && int.tryParse(key) == null) {
    key = key.substring(0, key.length - 1);
  }
  return key;
}

class _SearchField extends StatelessWidget {
  const _SearchField({
    required this.controller,
    required this.pageCount,
    required this.palette,
  });

  final TextEditingController controller;
  final int pageCount;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final font = isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans';
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
      child: ValueListenableBuilder(
        valueListenable: controller,
        builder: (context, value, _) => TextField(
          key: const ValueKey('sixteen-line-search'),
          controller: controller,
          textInputAction: TextInputAction.go,
          onSubmitted: (text) {
            final page = _Query(text, pageCount).page;
            if (page != null) {
              Navigator.of(context).pop(SixteenLinePosition(page));
            }
          },
          style: TextStyle(color: palette.title, fontFamily: font),
          decoration: InputDecoration(
            isDense: true,
            filled: true,
            fillColor: palette.card,
            prefixIcon: Icon(Icons.search_rounded, color: palette.subtitle),
            suffixIcon: value.text.isEmpty
                ? null
                : IconButton(
                    tooltip: MaterialLocalizations.of(
                      context,
                    ).deleteButtonTooltip,
                    onPressed: controller.clear,
                    icon: Icon(Icons.close_rounded, color: palette.subtitle),
                  ),
            hintText: context.loc.sixteenLineSearchHint,
            hintStyle: TextStyle(color: palette.subtitle, fontFamily: font),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
          ),
        ),
      ),
    );
  }
}

/// A list of results, led by the typed page when there is one.
class _Results extends StatelessWidget {
  const _Results({
    required this.query,
    required this.palette,
    required this.items,
  });

  final _Query query;
  final SettingsPalette palette;
  final List<Widget> items;

  @override
  Widget build(BuildContext context) {
    final page = query.page;
    final tiles = [
      if (page != null)
        SettingsPickerTile(
          key: const ValueKey('sixteen-line-page-go'),
          title: context.loc.sixteenLineGoToPageNumber(
            page.toLocalizedNumber(context),
          ),
          selected: false,
          palette: palette,
          onTap: () => Navigator.of(context).pop(SixteenLinePosition(page)),
        ),
      ...items,
    ];
    if (tiles.isEmpty) {
      final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Text(
          context.loc.sixteenLineNoMatch(query.text),
          textAlign: TextAlign.center,
          style: TextStyle(
            color: palette.subtitle,
            fontFamily: isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans',
            fontSize: 15,
          ),
        ),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(12),
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      itemCount: tiles.length,
      separatorBuilder: (context, index) => const SizedBox(height: 8),
      itemBuilder: (context, index) => tiles[index],
    );
  }
}

class _SurahList extends ConsumerWidget {
  const _SurahList({
    required this.quran,
    required this.query,
    required this.currentPage,
    required this.palette,
  });

  final SixteenLineQuran quran;
  final _Query query;
  final int currentPage;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final surahs = ref.watch(surahInfoProvider);
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final current = quran.surahForPage(currentPage);
    return _Results(
      query: query,
      palette: palette,
      items: [
        for (final surah in surahs)
          if (query.matches(surah.number, [
            surah.englishName,
            surah.englishNameTranslation,
            surah.name,
            SurahNamesProvider.getUrduName(surah.number),
          ]))
            SettingsPickerTile(
              key: ValueKey('sixteen-line-surah-${surah.number}'),
              title:
                  '${surah.number.toLocalizedNumber(context)}. '
                  '${isUrdu ? SurahNamesProvider.getUrduName(surah.number) : surah.englishName}',
              subtitle: context.loc.sixteenLinePageNumber(
                quran.surahStartPages[surah.number - 1].toLocalizedNumber(
                  context,
                ),
              ),
              selected: surah.number == current,
              palette: palette,
              onTap: () =>
                  Navigator.of(context).pop(quran.surahStart(surah.number)),
            ),
      ],
    );
  }
}

class _JuzList extends StatelessWidget {
  const _JuzList({
    required this.quran,
    required this.query,
    required this.currentPage,
    required this.palette,
  });

  final SixteenLineQuran quran;
  final _Query query;
  final int currentPage;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final current = quran.juzForPage(currentPage);
    return _Results(
      query: query,
      palette: palette,
      items: [
        for (var number = 1; number <= quran.juzStartPages.length; number++)
          if (query.matches(number, [
            JuzNamesProvider.getEnglishName(number),
            JuzNamesProvider.getUrduName(number),
            JuzNamesProvider.getArabicName(number),
          ]))
            SettingsPickerTile(
              key: ValueKey('sixteen-line-juz-$number'),
              title:
                  '${number.toLocalizedNumber(context)}. '
                  '${isUrdu ? JuzNamesProvider.getUrduName(number) : JuzNamesProvider.getEnglishName(number)}',
              subtitle: context.loc.sixteenLinePageNumber(
                quran.juzStartPages[number - 1].toLocalizedNumber(context),
              ),
              selected: number == current,
              palette: palette,
              onTap: () => _pickQuarter(context, number),
            ),
      ],
    );
  }

  /// Asks which part of [juz] to open, then opens it.
  Future<void> _pickQuarter(BuildContext context, int juz) async {
    final navigator = Navigator.of(context);
    final position = await showDialog<SixteenLinePosition>(
      context: context,
      builder: (context) =>
          _JuzQuarterDialog(quran: quran, juz: juz, palette: palette),
    );
    if (position != null) navigator.pop(position);
  }
}

/// The juz's start, quarter, half and three quarter marks, each with its
/// page. Pops with the one tapped.
class _JuzQuarterDialog extends StatelessWidget {
  const _JuzQuarterDialog({
    required this.quran,
    required this.juz,
    required this.palette,
  });

  final SixteenLineQuran quran;
  final int juz;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final loc = context.loc;
    final name = isUrdu
        ? JuzNamesProvider.getUrduName(juz)
        : JuzNamesProvider.getEnglishName(juz);
    return Dialog(
      backgroundColor: palette.background,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              '${loc.juz} ${juz.toLocalizedNumber(context)} · $name',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: palette.title,
                fontFamily: isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans',
                fontSize: 17,
                height: 1.3,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 14),
            for (final quarter in JuzQuarter.values) ...[
              if (quarter != JuzQuarter.start) const SizedBox(height: 8),
              SettingsPickerTile(
                key: ValueKey('sixteen-line-juz-$juz-${quarter.name}'),
                title: switch (quarter) {
                  JuzQuarter.start => loc.sixteenLineJuzStart,
                  JuzQuarter.rub => loc.sixteenLineJuzRub,
                  JuzQuarter.nisf => loc.sixteenLineJuzNisf,
                  JuzQuarter.thalatha => loc.sixteenLineJuzSalasa,
                },
                subtitle: loc.sixteenLinePageNumber(
                  quran
                      .juzQuarter(juz, quarter)
                      .page
                      .toLocalizedNumber(context),
                ),
                selected: false,
                palette: palette,
                onTap: () =>
                    Navigator.of(context).pop(quran.juzQuarter(juz, quarter)),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
