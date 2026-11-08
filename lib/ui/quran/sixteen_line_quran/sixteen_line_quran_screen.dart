import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:rafeeq/core/navigation/app_routes.dart';
import 'package:rafeeq/data/providers/shared_prefs_provider.dart';
import 'package:rafeeq/data/providers/surah_info_provider.dart';
import 'package:rafeeq/l10n/app_localization_ext.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/ui/quran/quran_reader_app_bar_title.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_page.dart';
import 'package:rafeeq/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart';
import 'package:rafeeq/ui/widgets/rafeeq_app_bar.dart';
import 'package:rafeeq/utils/extensions/number_localization.dart';
import 'package:rafeeq/utils/surah_names_provider.dart';

const _lastPageKey = 'sixteen_line_quran_last_page';

/// The page the reader was last on, so the next visit opens there.
final sixteenLineLastPageProvider =
    NotifierProvider<SixteenLineLastPageNotifier, int>(
      SixteenLineLastPageNotifier.new,
    );

class SixteenLineLastPageNotifier extends Notifier<int> {
  @override
  int build() => ref.watch(sharedPreferencesProvider).getInt(_lastPageKey) ?? 1;

  void save(int page) {
    if (page == state) return;
    state = page;
    ref.read(sharedPreferencesProvider).setInt(_lastPageKey, page);
  }
}

/// Reads the IndoPak 16 line mushaf page by page.
///
/// Opens on [initialPage], or on the page the reader last left off.
class SixteenLineQuranScreen extends ConsumerStatefulWidget {
  const SixteenLineQuranScreen({super.key, this.initialPage});

  final int? initialPage;

  @override
  ConsumerState<SixteenLineQuranScreen> createState() =>
      _SixteenLineQuranScreenState();
}

class _SixteenLineQuranScreenState
    extends ConsumerState<SixteenLineQuranScreen> {
  late int _page = widget.initialPage ?? ref.read(sixteenLineLastPageProvider);

  /// Bumped on every jump so the page view is rebuilt at the new page.
  int _jumps = 0;

  void _onPageChanged(int page) {
    if (page == _page) return;
    setState(() => _page = page);
    ref.read(sixteenLineLastPageProvider.notifier).save(page);
  }

  Future<void> _openIndex() async {
    final page = await Navigator.of(
      context,
    ).pushNamed<int>(AppRoutes.sixteenLineQuranIndex, arguments: _page);
    if (page == null || !mounted) return;
    setState(() {
      _page = page;
      _jumps++;
    });
    ref.read(sixteenLineLastPageProvider.notifier).save(page);
  }

  @override
  Widget build(BuildContext context) {
    final palette = SettingsPalette.of(context);
    final colors = SixteenLinePageColors.of(palette);
    final quranAsync = ref.watch(sixteenLineQuranProvider);
    final surahs = ref.watch(surahInfoProvider);
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final quran = quranAsync.value;

    var title = context.loc.sixteenLineQuran;
    Widget? titleWidget;
    if (quran != null) {
      _page = _page.clamp(1, quran.pageCount);
      final surah = quran.surahForPage(_page);
      title = isUrdu
          ? SurahNamesProvider.getUrduName(surah)
          : surahs[surah - 1].englishName;
      final juz = quran.juzForPage(_page).toLocalizedNumber(context);
      final page = _page.toLocalizedNumber(context);
      titleWidget = QuranReaderAppBarTitle(
        title: title,
        subtitle:
            '${context.loc.juz} $juz · ${context.loc.sixteenLinePageNumber(page)}',
        palette: palette,
        onTap: _openIndex,
      );
    }

    return Scaffold(
      backgroundColor: colors.background,
      appBar: RafeeqAppBar(
        title: title,
        titleWidget: titleWidget,
        palette: palette,
        showBackButton: true,
        actions: [
          IconButton(
            key: const ValueKey('sixteen-line-index-action'),
            tooltip: context.loc.sixteenLineGoTo,
            onPressed: quran == null ? null : _openIndex,
            icon: const Icon(Icons.format_list_bulleted_rounded),
          ),
        ],
      ),
      body: SafeArea(
        top: false,
        child: switch (quranAsync) {
          AsyncData(:final value) => _SwipeReader(
            key: ValueKey('sixteen-line-swipe-$_jumps'),
            quran: value,
            colors: colors,
            initialPage: _page,
            onPageChanged: _onPageChanged,
          ),
          AsyncError() => Center(
            child: Text(
              context.loc.legalRetry,
              style: TextStyle(color: colors.text),
            ),
          ),
          _ => Center(child: CircularProgressIndicator(color: colors.accent)),
        },
      ),
    );
  }
}

/// One page at a time. Laid out right to left, so the next page comes in
/// from the left like a printed mushaf.
///
/// Pages zoom with two fingers, up to three times their size. The pinch is
/// read straight from the pointers rather than through a gesture
/// recogniser, so it never competes with the page turn for a swipe.
/// Turning holds while two fingers are down or the page is zoomed in, when
/// one finger moves around the page instead.
class _SwipeReader extends StatefulWidget {
  const _SwipeReader({
    super.key,
    required this.quran,
    required this.colors,
    required this.initialPage,
    required this.onPageChanged,
  });

  final SixteenLineQuran quran;
  final SixteenLinePageColors colors;
  final int initialPage;
  final ValueChanged<int> onPageChanged;

  @override
  State<_SwipeReader> createState() => _SwipeReaderState();
}

class _SwipeReaderState extends State<_SwipeReader> {
  static const _maxZoom = 3.0;

  late final _controller = PageController(initialPage: widget.initialPage - 1);
  late int _index = widget.initialPage - 1;

  /// Built pages, reused so that a rebuild of the reader (the title
  /// changing as a page turns) does not rebuild the pages mid swipe.
  final _pages = <int, Widget>{};

  /// The zoom of the current page; other pages always show at full size.
  final _zoom = ValueNotifier(_Zoom.none);

  final _pointers = <int, Offset>{};
  Size _viewport = Size.zero;

  // Where the current pinch started.
  _Zoom _pinchStartZoom = _Zoom.none;
  double _pinchStartSpan = 0;
  Offset _pinchStartFocal = Offset.zero;
  bool _pinched = false;

  bool get _canTurn => _pointers.length < 2 && !_zoom.value.isZoomed;

  @override
  void didUpdateWidget(_SwipeReader oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.quran != widget.quran || oldWidget.colors != widget.colors) {
      _pages.clear();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    _zoom.dispose();
    super.dispose();
  }

  void _update(VoidCallback change) {
    final couldTurn = _canTurn;
    change();
    if (couldTurn != _canTurn) setState(() {});
  }

  void _startPinch() {
    final points = _pointers.values.take(2).toList();
    _pinchStartZoom = _zoom.value;
    _pinchStartSpan = (points[0] - points[1]).distance;
    _pinchStartFocal = (points[0] + points[1]) / 2;
    _pinched = true;
  }

  void _onPointerDown(PointerDownEvent event) {
    _update(() {
      _pointers[event.pointer] = event.localPosition;
      if (_pointers.length == 2) _startPinch();
    });
  }

  void _onPointerMove(PointerMoveEvent event) {
    final previous = _pointers[event.pointer];
    if (previous == null) return;
    _pointers[event.pointer] = event.localPosition;
    if (_pointers.length >= 2) {
      final points = _pointers.values.take(2).toList();
      final span = (points[0] - points[1]).distance;
      final focal = (points[0] + points[1]) / 2;
      if (_pinchStartSpan <= 0) return;
      final start = _pinchStartZoom;
      final scale = (start.scale * span / _pinchStartSpan).clamp(1.0, _maxZoom);
      // Keep the spot first pinched under the fingers as they move.
      final spot = (_pinchStartFocal - start.offset) / start.scale;
      _setZoom(_Zoom(scale, focal - spot * scale));
    } else if (_zoom.value.isZoomed) {
      final zoom = _zoom.value;
      _setZoom(_Zoom(zoom.scale, zoom.offset + event.delta));
    }
  }

  void _onPointerUp(PointerEvent event) {
    _update(() {
      _pointers.remove(event.pointer);
      if (_pointers.length == 2) _startPinch();
      // Back to full size: let the page turn again.
      if (_pointers.length < 2 && _zoom.value.scale < 1.05) {
        _zoom.value = _Zoom.none;
      }
    });
    if (_pointers.isEmpty && _pinched) {
      _pinched = false;
      _settle();
    }
  }

  /// Applies [zoom], kept inside the page so no blank space shows.
  void _setZoom(_Zoom zoom) {
    final minX = _viewport.width * (1 - zoom.scale);
    final minY = _viewport.height * (1 - zoom.scale);
    final wasZoomed = _zoom.value.isZoomed;
    _zoom.value = _Zoom(
      zoom.scale,
      Offset(zoom.offset.dx.clamp(minX, 0), zoom.offset.dy.clamp(minY, 0)),
    );
    if (wasZoomed != _zoom.value.isZoomed) setState(() {});
  }

  /// A pinch that began mid turn can leave the view between two pages.
  /// Only after a pinch: a swipe settles itself.
  void _settle() {
    if (!_controller.hasClients || !_canTurn) return;
    final page = _controller.page;
    if (page == null || page == page.roundToDouble()) return;
    _controller.animateToPage(
      page.round(),
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
    );
  }

  Widget _page(int index) {
    final page = _pages[index] ??= SixteenLinePage(
      quran: widget.quran,
      page: index + 1,
      colors: widget.colors,
    );
    return ValueListenableBuilder<_Zoom>(
      valueListenable: _zoom,
      child: page,
      builder: (context, zoom, child) {
        if (index != _index || !zoom.isZoomed) return child!;
        return ClipRect(
          child: Transform(
            key: const ValueKey('sixteen-line-zoom'),
            transform: Matrix4.identity()
              ..translateByDouble(zoom.offset.dx, zoom.offset.dy, 0, 1)
              ..scaleByDouble(zoom.scale, zoom.scale, 1, 1),
            child: child,
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final appDirection = Directionality.of(context);
    return LayoutBuilder(
      builder: (context, constraints) {
        _viewport = constraints.biggest;
        return Listener(
          onPointerDown: _onPointerDown,
          onPointerMove: _onPointerMove,
          onPointerUp: _onPointerUp,
          onPointerCancel: _onPointerUp,
          child: Directionality(
            textDirection: TextDirection.rtl,
            child: PageView.builder(
              controller: _controller,
              // Keeps the pages either side built, so a turn never waits.
              allowImplicitScrolling: true,
              physics: _canTurn ? null : const NeverScrollableScrollPhysics(),
              itemCount: widget.quran.pageCount,
              onPageChanged: (index) {
                _index = index;
                widget.onPageChanged(index + 1);
              },
              itemBuilder: (context, index) => Directionality(
                textDirection: appDirection,
                child: _page(index),
              ),
            ),
          ),
        );
      },
    );
  }
}

@immutable
class _Zoom {
  const _Zoom(this.scale, this.offset);

  static const none = _Zoom(1, Offset.zero);

  final double scale;

  /// Where the page's top left corner sits, in the viewport.
  final Offset offset;

  bool get isZoomed => scale > 1.0;
}
