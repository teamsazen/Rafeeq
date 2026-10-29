import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:rafeeq/l10n/app_localization_ext.dart';
import 'package:rafeeq/ui/app_home/home/settings_palette.dart';
import 'package:rafeeq/ui/widgets/rafeeq_app_bar.dart';
import 'package:rafeeq/ui/widgets/rafeeq_toast.dart';
import 'package:rafeeq/utils/localized_font.dart';
import 'package:url_launcher/url_launcher.dart';

final _muzamilPortfolioUri = Uri.parse('https://muzamil.web.app/');

class AboutScreen extends StatefulWidget {
  const AboutScreen({super.key});

  @override
  State<AboutScreen> createState() => _AboutScreenState();
}

class _AboutScreenState extends State<AboutScreen> {
  late final TapGestureRecognizer _portfolioRecognizer;

  @override
  void initState() {
    super.initState();
    _portfolioRecognizer = TapGestureRecognizer()..onTap = _openPortfolio;
  }

  @override
  void dispose() {
    _portfolioRecognizer.dispose();
    super.dispose();
  }

  Future<void> _openPortfolio() async {
    var opened = false;
    try {
      opened = await launchUrl(
        _muzamilPortfolioUri,
        mode: LaunchMode.externalApplication,
      );
    } catch (_) {
      opened = false;
    }
    if (!opened && mounted) {
      showRafeeqToast(
        context,
        context.loc.aboutPortfolioOpenError,
        type: RafeeqToastType.error,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final palette = SettingsPalette.of(context);
    final isUrdu = Localizations.localeOf(context).languageCode == 'ur';
    final sections = [
      (context.loc.aboutBeginningTitle, context.loc.aboutBeginningBody),
      (context.loc.aboutTogetherTitle, context.loc.aboutTogetherBody),
      (context.loc.aboutDesignTitle, context.loc.aboutDesignBody),
      (context.loc.aboutUmmahTitle, context.loc.aboutUmmahBody),
      (context.loc.aboutFreeTitle, context.loc.aboutFreeBody),
      (context.loc.aboutCommunityTitle, context.loc.aboutCommunityBody),
    ];

    return Scaffold(
      backgroundColor: palette.background,
      appBar: RafeeqAppBar(
        title: context.loc.about,
        palette: palette,
        showBackButton: true,
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760),
          child: ListView(
            physics: const ClampingScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 32),
            children: [
              _AppIdentity(palette: palette),
              const SizedBox(height: 24),
              for (var index = 0; index < sections.length; index++) ...[
                _AboutSection(
                  title: sections[index].$1,
                  body: sections[index].$2,
                  emphasizedPhrases: index == 0
                      ? isUrdu
                            ? const ['مزمل غفور']
                            : const ['Muzamil Ghafoor']
                      : const [],
                  linkedPhrase: index == 0
                      ? isUrdu
                            ? 'مزمل غفور'
                            : 'Muzamil Ghafoor'
                      : null,
                  linkRecognizer: index == 0 ? _portfolioRecognizer : null,
                  palette: palette,
                ),
                if (index != sections.length - 1) const SizedBox(height: 24),
              ],
              const SizedBox(height: 24),
              _ClosingStatement(palette: palette),
              const SizedBox(height: 32),
              _MoreFromUs(palette: palette),
            ],
          ),
        ),
      ),
    );
  }
}

const _otherApps = [
  (name: '99 Names', icon: 'asmaul_husna', package: 'com.muzamil.asmaulhusna'),
  (
    name: 'Quran Khatam Tracker',
    icon: 'quran_khatam_tracker',
    package: 'com.quran.khatam.tracker',
  ),
  (name: 'SalahLog', icon: 'salah_log', package: 'com.track_prayer'),
  (name: 'Budget AI', icon: 'budget_ai', package: 'com.budgetai.android'),
  (name: 'FM Crafts', icon: 'fm_crafts', package: 'com.fmcrafts.fmcrafts'),
  (name: 'Exit Rush', icon: 'exit_rush', package: 'com.sazen.exit.rush'),
];

class _MoreFromUs extends StatelessWidget {
  const _MoreFromUs({required this.palette});

  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    Widget heading(String title) => Padding(
      padding: const EdgeInsetsDirectional.only(start: 4, bottom: 12),
      child: Text(
        title,
        style: TextStyle(
          color: palette.title,
          fontFamily: localizedFontFamily(context),
          fontSize: 17,
          fontWeight: FontWeight.w700,
        ),
      ),
    );

    Widget group(List<Widget> children) => Material(
      color: palette.card,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: Column(children: children),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        heading(context.loc.settingsSectionOtherApps),
        group([
          for (var index = 0; index < _otherApps.length; index++) ...[
            _ExternalLinkTile(
              title: _otherApps[index].name,
              subtitle: context.loc.otherAppsSubtitle,
              icon: _otherApps[index].icon,
              uri: Uri.https('play.google.com', '/store/apps/details', {
                'id': _otherApps[index].package,
              }),
              palette: palette,
            ),
            if (index != _otherApps.length - 1)
              Padding(
                padding: const EdgeInsetsDirectional.only(start: 80, end: 16),
                child: Divider(
                  height: 1,
                  color: palette.subtitle.withValues(alpha: .12),
                ),
              ),
          ],
        ]),
        const SizedBox(height: 24),
        heading(context.loc.aboutWebsiteTitle),
        group([
          _ExternalLinkTile(
            title: 'Sazen',
            subtitle: 'teamsazen.com',
            icon: 'sazen',
            uri: Uri.https('teamsazen.com'),
            palette: palette,
          ),
        ]),
      ],
    );
  }
}

class _ExternalLinkTile extends StatelessWidget {
  const _ExternalLinkTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.uri,
    required this.palette,
  });

  final String title;
  final String subtitle;
  final String icon;
  final Uri uri;
  final SettingsPalette palette;

  Future<void> _open(BuildContext context) async {
    try {
      if (await launchUrl(uri, mode: LaunchMode.externalApplication)) return;
    } catch (_) {
      // Show the same actionable feedback when no external handler is available.
    }
    if (context.mounted) {
      showRafeeqToast(
        context,
        context.loc.aboutLinkOpenError,
        type: RafeeqToastType.error,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => _open(context),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Image.asset(
                'assets/other_apps/$icon.webp',
                width: 48,
                height: 48,
                cacheWidth: 192,
                fit: BoxFit.cover,
                excludeFromSemantics: true,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    textDirection: TextDirection.ltr,
                    style: TextStyle(
                      color: palette.title,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      fontFamily: 'GoogleSans',
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    textDirection: icon == 'sazen' ? TextDirection.ltr : null,
                    style: TextStyle(
                      color: palette.subtitle,
                      fontSize: 13,
                      fontFamily: icon == 'sazen'
                          ? 'GoogleSans'
                          : localizedFontFamily(context),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Icon(Icons.open_in_new_rounded, size: 18, color: palette.accent),
          ],
        ),
      ),
    );
  }
}

class _AppIdentity extends StatelessWidget {
  const _AppIdentity({required this.palette});

  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        ClipOval(
          child: Image.asset(
            'assets/logo.png',
            width: 72,
            height: 72,
            fit: BoxFit.cover,
            excludeFromSemantics: true,
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                context.loc.appTitle,
                style: TextStyle(
                  color: palette.title,
                  fontFamily: localizedFontFamily(context),
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                ),
              ),
              Text(
                context.loc.aboutTagline,
                style: TextStyle(
                  color: palette.subtitle,
                  fontFamily: localizedFontFamily(context),
                  fontSize: 15,
                  height: 1.4,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _AboutSection extends StatelessWidget {
  const _AboutSection({
    required this.title,
    required this.body,
    required this.emphasizedPhrases,
    required this.linkedPhrase,
    required this.linkRecognizer,
    required this.palette,
  });

  final String title;
  final String body;
  final List<String> emphasizedPhrases;
  final String? linkedPhrase;
  final GestureRecognizer? linkRecognizer;
  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: TextStyle(
            color: palette.title,
            fontFamily: localizedFontFamily(context),
            fontSize: 17,
            height: 1.35,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 8),
        Text.rich(
          TextSpan(children: _buildEmphasizedSpans(body, emphasizedPhrases)),
          style: TextStyle(
            color: palette.subtitle,
            fontFamily: localizedFontFamily(context),
            fontSize: 15,
            height: 1.65,
            fontWeight: FontWeight.w400,
          ),
        ),
      ],
    );
  }

  List<InlineSpan> _buildEmphasizedSpans(String text, List<String> phrases) {
    if (phrases.isEmpty) return [TextSpan(text: text)];

    final spans = <InlineSpan>[];
    var remaining = text;
    while (remaining.isNotEmpty) {
      String? nextPhrase;
      var nextIndex = remaining.length;
      for (final phrase in phrases) {
        final index = remaining.indexOf(phrase);
        if (index >= 0 && index < nextIndex) {
          nextIndex = index;
          nextPhrase = phrase;
        }
      }
      if (nextPhrase == null) {
        spans.add(TextSpan(text: remaining));
        break;
      }
      if (nextIndex > 0) {
        spans.add(TextSpan(text: remaining.substring(0, nextIndex)));
      }
      spans.add(
        TextSpan(
          text: nextPhrase,
          style: const TextStyle(fontWeight: FontWeight.w700),
          recognizer: nextPhrase == linkedPhrase ? linkRecognizer : null,
        ),
      );
      remaining = remaining.substring(nextIndex + nextPhrase.length);
    }
    return spans;
  }
}

class _ClosingStatement extends StatelessWidget {
  const _ClosingStatement({required this.palette});

  final SettingsPalette palette;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: palette.selectedCard.withValues(alpha: 0.22)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Text(
          context.loc.aboutClosingStatement,
          textAlign: TextAlign.center,
          style: TextStyle(
            color: palette.title,
            fontFamily: localizedFontFamily(context),
            fontSize: 16,
            height: 1.55,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}
