package com.satechdev.rafeeqapp.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.satechdev.rafeeqapp.BuildConfig
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.ThemeMode
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.palette

private const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.satechdev.rafeeqapp"
private const val FEEDBACK_FORM = "https://forms.gle/cDLne2RtGvfmqrJn9"
private const val REPOSITORY = "https://github.com/teamsazen/Rafeeq"
private const val REPORT_ISSUE = "https://github.com/teamsazen/Rafeeq/issues/new"

private class Row(val icon: Glyph, val title: String, val subtitle: String?, val badge: String? = null, val onClick: () -> Unit)

/** The Settings tab, laid out as lib/ui/app_home/home/settings_screen.dart. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    fun soon(title: Int): () -> Unit = { nav.push(Route.Placeholder(title)) }

    val sections = listOf(
        tr(R.string.general) to listOf(
            Row(Glyph.Translate, tr(R.string.language), languageLabel(Settings.language)) { nav.push(Route.Language) },
            Row(Glyph.Explore, tr(R.string.location), locationLabel()) { nav.push(Route.Location) },
            Row(Glyph.Palette, tr(R.string.appTheme), themeLabel(Settings.themeMode)) { nav.push(Route.Theme) },
        ),
        tr(R.string.settingsSectionPrayer) to listOf(
            Row(Glyph.Clock, tr(R.string.calculationMethod), tr(Settings.calcMethod.label)) { nav.push(Route.CalculationMethod) },
            Row(Glyph.Layers, tr(R.string.juristicMethod), tr(Settings.asrMethod.label)) { nav.push(Route.JuristicMethod) },
            Row(Glyph.Public, tr(R.string.highLatitudeRule), tr(Settings.highLatitude.label)) { nav.push(Route.HighLatitudeRule) },
            Row(Glyph.Calendar, tr(R.string.hijriCorrection), tr(Settings.hijriCorrection.label)) { nav.push(Route.HijriCorrection) },
            Row(Glyph.Schedule, tr(R.string.timeFormat), tr(Settings.timeFormat.label)) { nav.push(Route.TimeFormat) },
            Row(Glyph.AccessTime, tr(R.string.prayerTimes), locationLabel()) { nav.push(Route.PrayerTimes) },
            Row(Glyph.Notifications, tr(R.string.notification), tr(R.string.settingsNotificationSubtitle)) { nav.push(Route.NotificationSettings) },
        ),
        tr(R.string.quran) to listOf(
            Row(Glyph.MenuBook, tr(R.string.quranViewSettings), tr(R.string.settingsQuranViewSubtitle), onClick = soon(R.string.quranViewSettings)),
        ),
        tr(R.string.settingsSectionSupport) to listOf(
            Row(Glyph.ChatBubble, tr(R.string.feedback), tr(R.string.feedbackSubtitle)) { context.open(FEEDBACK_FORM) },
            Row(Glyph.StarBorder, tr(R.string.rateThisApp), tr(R.string.rateThisAppSubtitle)) { context.open(PLAY_STORE) },
            Row(Glyph.Share, tr(R.string.shareThisApp), tr(R.string.shareThisAppSubtitle)) { context.share(PLAY_STORE) },
            Row(Glyph.BugReport, tr(R.string.reportIssue), tr(R.string.reportIssueSubtitle)) { context.open(REPORT_ISSUE) },
            Row(Glyph.Code, tr(R.string.contribute), tr(R.string.contributeSubtitle)) { context.open(REPOSITORY) },
        ),
        tr(R.string.settingsSectionAppInfo) to listOf(
            Row(Glyph.PrivacyTip, tr(R.string.privacyPolicy), tr(R.string.privacyPolicySubtitle), onClick = soon(R.string.privacyPolicy)),
            Row(Glyph.Gavel, tr(R.string.termsAndConditions), tr(R.string.termsAndConditionsSubtitle), onClick = soon(R.string.termsAndConditions)),
            Row(Glyph.Description, tr(R.string.licenses), tr(R.string.licensesSubtitle), onClick = soon(R.string.licenses)),
            Row(Glyph.Info, tr(R.string.about), tr(R.string.aboutSubtitle), badge = if (Settings.aboutOpened) null else "NEW") {
                Settings.markAboutOpened()
                nav.push(Route.Placeholder(R.string.about))
            },
            Row(Glyph.Tag, tr(R.string.version), "${tr(R.string.versionSubtitle)}  •  ${BuildConfig.VERSION_NAME}", onClick = soon(R.string.version)),
        ),
    )

    Column(Modifier.fillMaxSize().background(palette.background)) {
        AppBar(tr(R.string.settings), onBack = onBack)
        CenteredList(bottom = TabBottomPadding) {
            sections.forEachIndexed { index, (title, rows) ->
                item(key = "title-$index") {
                    if (index > 0) Spacer(Modifier.height(24.dp))
                    SectionTitle(title)
                    Spacer(Modifier.height(12.dp))
                }
                rows.forEachIndexed { rowIndex, row ->
                    item(key = "$index-$rowIndex") {
                        if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                        SettingsCard(row.icon, row.title, row.subtitle, row.badge, onClick = row.onClick)
                    }
                }
            }
        }
    }
}

// ---------- Labels, as the Flutter enums' getLabel ----------

@Composable
fun languageLabel(language: AppLanguage) = when (language) {
    AppLanguage.English -> tr(R.string.languageEnglish)
    AppLanguage.Urdu -> tr(R.string.languageUrdu)
}

@Composable
fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.Light -> tr(R.string.themeModeLight)
    ThemeMode.System -> tr(R.string.themeModeSystem)
    ThemeMode.Dark -> tr(R.string.themeModeDark)
}

@Composable
fun locationLabel(): String {
    val parts = listOfNotNull(Settings.location?.city, Settings.location?.country)
    return if (parts.isEmpty()) tr(R.string.locationNotSet) else parts.joinToString(", ")
}

// ---------- Intents ----------

private fun Context.open(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

private fun Context.share(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
