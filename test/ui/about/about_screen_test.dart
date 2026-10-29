import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:rafeeq/core/navigation/app_routes.dart';
import 'package:rafeeq/l10n/app_localizations.dart';

void main() {
  Future<void> pumpAbout(WidgetTester tester, Locale locale) async {
    await tester.pumpWidget(
      MaterialApp(
        locale: locale,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        initialRoute: AppRoutes.about,
        onGenerateRoute: AppRoutes.onGenerateRoute,
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('shows the Rafeeq story and commitment in English', (
    tester,
  ) async {
    await pumpAbout(tester, const Locale('en'));

    expect(find.text('About'), findsOneWidget);
    expect(find.text('Rafeeq'), findsOneWidget);
    expect(find.text('Your Muslim Companion'), findsOneWidget);
    expect(find.text('How Rafeeq began'), findsOneWidget);
    expect(find.byType(ClipOval), findsOneWidget);

    final story = tester
        .widgetList<Text>(find.byType(Text))
        .firstWhere(
          (text) =>
              text.textSpan?.toPlainText().contains('Muzamil Ghafoor') ?? false,
        );
    final storySpans = (story.textSpan as TextSpan).children!.cast<TextSpan>();
    expect(story.textSpan?.toPlainText(), isNot(contains('Abdushakoor')));
    expect(
      storySpans
          .firstWhere((span) => span.text == 'Muzamil Ghafoor')
          .style
          ?.fontWeight,
      FontWeight.w700,
    );
    expect(
      storySpans
          .firstWhere((span) => span.text == 'Muzamil Ghafoor')
          .recognizer,
      isNotNull,
    );
    final localizations = AppLocalizations.of(
      tester.element(find.byType(Scaffold)),
    )!;
    expect(localizations.aboutUmmahBody, contains('Prophet Muhammad (PBUH)'));

    final closing = find.textContaining('Built by Muslims, for Muslims');
    await tester.scrollUntilVisible(
      closing,
      400,
      scrollable: find.byType(Scrollable),
    );
    expect(closing, findsOneWidget);

    // The story retains its closing statement before the discovery links.
    expect(find.text('Join WhatsApp Channel'), findsNothing);
    expect(find.text('Stay connected with Rafeeq'), findsNothing);
    expect(find.byType(SvgPicture), findsNothing);
  });

  testWidgets('opens the new app, game and website from About', (tester) async {
    final opened = <String>[];
    const channel = MethodChannel('plugins.flutter.io/url_launcher');
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      if (call.method == 'launch') {
        final args = call.arguments as Map;
        opened.add(args['url'] as String);
        expect(args['useSafariVC'], isFalse);
        expect(args['useWebView'], isFalse);
        return true;
      }
      return false;
    });
    addTearDown(
      () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        channel,
        null,
      ),
    );
    await pumpAbout(tester, const Locale('en'));
    for (final title in ['SalahLog', 'Exit Rush', 'teamsazen.com']) {
      await tester.scrollUntilVisible(
        find.text(title),
        300,
        scrollable: find.byType(Scrollable),
      );
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.text(title));
      await tester.pumpAndSettle();
      await tester.tap(find.text(title));
      await tester.pumpAndSettle();
    }
    expect(opened, [
      'https://play.google.com/store/apps/details?id=com.track_prayer',
      'https://play.google.com/store/apps/details?id=com.sazen.exit.rush',
      'https://teamsazen.com',
    ]);
  });

  for (final locale in [const Locale('en'), const Locale('ur')]) {
    for (final brightness in Brightness.values) {
      testWidgets(
        'discovery cards fit narrow ${locale.languageCode} $brightness with large text',
        (tester) async {
          await tester.binding.setSurfaceSize(const Size(320, 700));
          addTearDown(() => tester.binding.setSurfaceSize(null));
          await tester.pumpWidget(
            MaterialApp(
              locale: locale,
              theme: ThemeData(brightness: brightness),
              localizationsDelegates: AppLocalizations.localizationsDelegates,
              supportedLocales: AppLocalizations.supportedLocales,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: TextScaler.linear(1.5)),
                child: child!,
              ),
              initialRoute: AppRoutes.about,
              onGenerateRoute: AppRoutes.onGenerateRoute,
            ),
          );
          await tester.pumpAndSettle();
          await tester.scrollUntilVisible(
            find.text('SalahLog'),
            300,
            scrollable: find.byType(Scrollable),
          );
          expect(tester.takeException(), isNull);
          await tester.scrollUntilVisible(
            find.text('teamsazen.com'),
            300,
            scrollable: find.byType(Scrollable),
          );
          expect(tester.takeException(), isNull);
          final loc = AppLocalizations.of(
            tester.element(find.byType(Scaffold)),
          )!;
          expect(find.text(loc.aboutWebsiteTitle), findsOneWidget);
        },
      );
    }
  }

  testWidgets('localizes the About page in Urdu', (tester) async {
    await pumpAbout(tester, const Locale('ur'));

    expect(find.text('کے بارے میں'), findsOneWidget);
    expect(find.text('رفیق'), findsOneWidget);
    expect(find.text('آپ کا مسلم ساتھی'), findsOneWidget);
    expect(find.text('رفیق کا آغاز'), findsOneWidget);
    final localizations = AppLocalizations.of(
      tester.element(find.byType(Scaffold)),
    )!;
    expect(
      localizations.aboutUmmahBody,
      contains('حضرت محمد صلی اللہ علیہ وسلم'),
    );

    final closing = find.textContaining('مسلمانوں کے ہاتھوں، مسلمانوں کے لیے');
    await tester.scrollUntilVisible(
      closing,
      400,
      scrollable: find.byType(Scrollable),
    );
    expect(closing, findsOneWidget);
    expect(find.text('واٹس ایپ چینل جوائن کریں'), findsNothing);
  });
}
