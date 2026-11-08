import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:rafeeq/data/providers/surah_info_provider.dart';
import 'package:rafeeq/l10n/app_localization_ext.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/ui/app_home/home/settings_picker.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart';
import 'package:rafeeq/utils/extensions/number_localization.dart';
import 'package:rafeeq/utils/juz_names_provider.dart';
import 'package:rafeeq/utils/surah_names_provider.dart';

/// Picks a page to open: by surah, by juz, or by typing a page number.
/// Pops with the chosen page number.
class SixteenLineQuranIndexScreen extends ConsumerWidget {
  const SixteenLineQuranIndexScreen({super.key, required this.currentPage});

  final int currentPage;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
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
              _PageField(pageCount: value.pageCount, palette: palette),
              Expanded(
                child: TabBarView(
                  children: [
                    _SurahList(
                      quran: value,
                      currentPage: currentPage,
                      palette: palette,
                    ),
                    _JuzList(
                      quran: value,
                      currentPage: currentPage,
                      palette: palette,
                    ),
                  ],
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

class _PageField extends StatefulWidget {
  const _PageField({required this.pageCount, required this.palette});

  final int pageCount;
  final SettingsPalette palette;

  @override
  State<_PageField> createState() => _PageFieldState();
}

class _PageFieldState extends State<_PageField> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  int? get _page {
    final page = int.tryParse(_controller.text.trim());
    if (page == null || page < 1 || page > widget.pageCount) return null;
    return page;
  }

  void _submit() {
    final page = _page;
    if (page != null) Navigator.of(context).pop(page);
  }

  @override
  Widget build(BuildContext context) {
    final palette = widget.palette;
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final font = isUrdu ? 'Jameel Noori Nastaleeq' : 'GoogleSans';
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
      // The Go button matches the field's height.
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: TextField(
                key: const ValueKey('sixteen-line-page-field'),
                controller: _controller,
                keyboardType: TextInputType.number,
                textInputAction: TextInputAction.go,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                onChanged: (_) => setState(() {}),
                onSubmitted: (_) => _submit(),
                style: TextStyle(color: palette.title, fontFamily: font),
                decoration: InputDecoration(
                  isDense: true,
                  filled: true,
                  fillColor: palette.card,
                  hintText: context.loc.sixteenLineGoToPageHint(
                    widget.pageCount.toLocalizedNumber(context),
                  ),
                  hintStyle: TextStyle(
                    color: palette.subtitle,
                    fontFamily: font,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              key: const ValueKey('sixteen-line-page-go'),
              onPressed: _page == null ? null : _submit,
              style: FilledButton.styleFrom(
                backgroundColor: palette.accent,
                foregroundColor: palette.background,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: Text(
                context.loc.sixteenLineGoToPage,
                style: TextStyle(fontFamily: font),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SurahList extends ConsumerWidget {
  const _SurahList({
    required this.quran,
    required this.currentPage,
    required this.palette,
  });

  final SixteenLineQuran quran;
  final int currentPage;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final surahs = ref.watch(surahInfoProvider);
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final current = quran.surahForPage(currentPage);
    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: surahs.length,
      separatorBuilder: (context, index) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final number = index + 1;
        final page = quran.surahStartPages[index];
        final name = isUrdu
            ? SurahNamesProvider.getUrduName(number)
            : surahs[index].englishName;
        return SettingsPickerTile(
          key: ValueKey('sixteen-line-surah-$number'),
          title: '${number.toLocalizedNumber(context)}. $name',
          subtitle: context.loc.sixteenLinePageNumber(
            page.toLocalizedNumber(context),
          ),
          selected: number == current,
          palette: palette,
          onTap: () => Navigator.of(context).pop(page),
        );
      },
    );
  }
}

class _JuzList extends StatelessWidget {
  const _JuzList({
    required this.quran,
    required this.currentPage,
    required this.palette,
  });

  final SixteenLineQuran quran;
  final int currentPage;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final current = quran.juzForPage(currentPage);
    return ListView.separated(
      padding: const EdgeInsets.all(12),
      itemCount: quran.juzStartPages.length,
      separatorBuilder: (context, index) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final number = index + 1;
        final page = quran.juzStartPages[index];
        final name = isUrdu
            ? JuzNamesProvider.getUrduName(number)
            : JuzNamesProvider.getEnglishName(number);
        return SettingsPickerTile(
          key: ValueKey('sixteen-line-juz-$number'),
          title: '${number.toLocalizedNumber(context)}. $name',
          subtitle: context.loc.sixteenLinePageNumber(
            page.toLocalizedNumber(context),
          ),
          selected: number == current,
          palette: palette,
          onTap: () => Navigator.of(context).pop(page),
        );
      },
    );
  }
}
