package com.satechdev.rafeeqapp.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.LocalStrings
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Text in the app's language font, as Flutter's localizedFontFamily. */
@Composable
private fun local(size: Int, color: androidx.compose.ui.graphics.Color, weight: FontWeight = FontWeight.Normal, lineHeight: Float = 1.3f) =
    style(size.sp, color, weight, family = if (Settings.language.rtl) AppFonts.urdu else AppFonts.sans, lineHeight = lineHeight)

/** A page of reading: the app bar and a centred column of items. */
@Composable
private fun ReadingPage(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val nav = LocalNavigator.current
    Column(Modifier.fillMaxSize().background(palette.background)) {
        AppBar(title, onBack = nav::pop)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 32.dp),
                content = content,
            )
        }
    }
}

/** The logo, the app's name and its tagline. */
@Composable
private fun AppIdentity(logo: Int) {
    val p = palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(logo.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(tr(R.string.appTitle), local(24, p.title, FontWeight.Bold))
            Txt(tr(R.string.aboutTagline), local(15, p.subtitle, FontWeight.Medium, 1.4f))
        }
    }
}

/** A rounded outline around a centred line, as the closing statement and contact. */
@Composable
private fun Outlined(text: String, textStyle: androidx.compose.ui.text.TextStyle) {
    val p = palette
    Box(Modifier.fillMaxWidth().border(1.dp, p.selectedCard.copy(alpha = .22f), RoundedCornerShape(12.dp)).padding(18.dp)) {
        Txt(text, textStyle.copy(textAlign = TextAlign.Center), Modifier.fillMaxWidth())
    }
}

enum class LegalDocument(val key: String) { Privacy("privacy"), Terms("terms") }

private class Policy(val updated: String, val sections: List<Pair<String, String>>)

/** The privacy policy or terms from assets/legal.json, the same text as the website (legal_screen.dart). */
@Composable
fun LegalScreen(document: LegalDocument) {
    val context = LocalContext.current
    val p = palette
    val language = Settings.language.code
    var attempt by remember { mutableIntStateOf(0) }
    var policy by remember { mutableStateOf<Policy?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        failed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val versions = JSONObject(context.assets.open("legal.json").bufferedReader().use { it.readText() }).getJSONObject(document.key)
                val doc = versions.optJSONObject(language) ?: versions.getJSONObject("en")
                val s = doc.getJSONArray("sections")
                Policy(doc.getString("updated"), List(s.length()) { s.getJSONObject(it).let { o -> o.getString("title") to o.getString("body") } })
            }
        }.onSuccess { policy = it }.onFailure { failed = true }
    }
    ReadingPage(tr(if (document == LegalDocument.Privacy) R.string.privacyPolicy else R.string.termsAndConditions)) {
        val doc = policy
        when {
            failed -> item { Txt(tr(R.string.legalRetry), local(15, p.accent), Modifier.fillMaxWidth().pressable({ attempt++ }).padding(24.dp)) }
            doc == null -> item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) } }
            else -> {
                item { Box(Modifier.padding(4.dp)) { AppIdentity(64) } }
                item { Txt(doc.updated, local(13, p.subtitle), Modifier.padding(start = 4.dp, top = 20.dp)) }
                // The policy can be selected and copied, as in Flutter.
                doc.sections.forEach { (title, body) ->
                    item {
                        SelectionContainer {
                            Column(Modifier.padding(start = 4.dp, top = 28.dp, end = 4.dp)) {
                                Txt(title, local(17, p.title, FontWeight.Bold, 1.5f))
                                Spacer(Modifier.height(8.dp))
                                Txt(body, local(15, p.subtitle, lineHeight = 1.75f))
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(28.dp))
                    Outlined("teamsazen@gmail.com", style(15.sp, p.title, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr))
                }
            }
        }
    }
}

private const val PORTFOLIO = "https://muzamil.web.app/"

private class OtherApp(val name: String, @androidx.annotation.DrawableRes val icon: Int, val pkg: String)

/** The team's live apps on Google Play (developer "Sazen"), in the store's order, with their store icons. */
private val otherApps = listOf(
    OtherApp("Exit Rush", R.drawable.other_app_exit_rush, "com.sazen.exit.rush"),
    OtherApp("Quran Urdu Audio", R.drawable.other_app_quran_urdu_audio, "com.sazen.urdu.audio.quran.transcribed.shamshad.ali.khan"),
    OtherApp("Budget AI", R.drawable.other_app_budget_ai, "com.budgetai.android"),
    OtherApp("Bg Remover & Photo Enhancer", R.drawable.other_app_bg_remover, "com.sazen.bg.remover"),
    OtherApp("99 Names", R.drawable.other_app_asmaul_husna, "com.muzamil.asmaulhusna"),
    OtherApp("SalahLog", R.drawable.other_app_salah_log, "com.track_prayer"),
    OtherApp("Quran Khatam Tracker", R.drawable.other_app_quran_khatam_tracker, "com.quran.khatam.tracker"),
    OtherApp("FM Crafts", R.drawable.other_app_fm_crafts, "com.fmcrafts.fmcrafts"),
)

/** Opens [url] outside the app, or says it could not. */
private fun Context.openLink(url: String, error: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.show(error)
    }
}

/** The story of Rafeeq, and the team's other apps (about_screen.dart). */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val p = palette
    val urdu = Settings.language.rtl
    val sections = listOf(
        R.string.aboutBeginningTitle to R.string.aboutBeginningBody,
        R.string.aboutTogetherTitle to R.string.aboutTogetherBody,
        R.string.aboutDesignTitle to R.string.aboutDesignBody,
        R.string.aboutUmmahTitle to R.string.aboutUmmahBody,
        R.string.aboutFreeTitle to R.string.aboutFreeBody,
        R.string.aboutCommunityTitle to R.string.aboutCommunityBody,
    )
    val portfolioError = tr(R.string.aboutPortfolioOpenError)
    val linkError = tr(R.string.aboutLinkOpenError)
    ReadingPage(tr(R.string.about)) {
        item { AppIdentity(72) }
        sections.forEachIndexed { i, (title, body) ->
            item {
                Column(Modifier.padding(top = 24.dp)) {
                    Txt(tr(title), local(17, p.title, FontWeight.Bold, 1.35f))
                    Spacer(Modifier.height(8.dp))
                    val text = tr(body)
                    // The founder's name, in bold, opens his portfolio.
                    val name = if (urdu) "مزمل غفور" else "Muzamil Ghafoor"
                    val at = if (i == 0) text.indexOf(name) else -1
                    if (at < 0) {
                        Txt(text, local(15, p.subtitle, lineHeight = 1.65f))
                    } else {
                        BasicText(
                            buildAnnotatedString {
                                append(text.substring(0, at))
                                withLink(LinkAnnotation.Clickable("portfolio", TextLinkStyles(SpanStyle(fontWeight = FontWeight.Bold))) { context.openLink(PORTFOLIO, portfolioError) }) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
                                }
                                append(text.substring(at + name.length))
                            },
                            style = local(15, p.subtitle, lineHeight = 1.65f),
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Outlined(tr(R.string.aboutClosingStatement), local(16, p.title, FontWeight.Bold, 1.55f))
        }
        item {
            Spacer(Modifier.height(32.dp))
            Heading(tr(R.string.settingsSectionOtherApps))
            Group {
                otherApps.forEachIndexed { i, app ->
                    LinkTile(app.name, tr(R.string.otherAppsSubtitle), app.icon, latinSubtitle = false) {
                        context.openLink("https://play.google.com/store/apps/details?id=${app.pkg}", linkError)
                    }
                    if (i != otherApps.lastIndex) Box(Modifier.padding(start = 80.dp, end = 16.dp).fillMaxWidth().height(1.dp).background(p.subtitle.copy(alpha = .12f)))
                }
            }
            Spacer(Modifier.height(24.dp))
            Heading(tr(R.string.aboutWebsiteTitle))
            Group { LinkTile("Sazen", "teamsazen.com", R.drawable.other_app_sazen, latinSubtitle = true) { context.openLink("https://teamsazen.com", linkError) } }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Txt(text, local(17, palette.title, FontWeight.Bold), Modifier.padding(start = 4.dp, bottom = 12.dp))
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.card)) { content() }
}

@Composable
private fun LinkTile(title: String, subtitle: String, @androidx.annotation.DrawableRes icon: Int, latinSubtitle: Boolean, onClick: () -> Unit) {
    val p = palette
    Row(Modifier.fillMaxWidth().pressable(onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, style(16.sp, p.title, FontWeight.SemiBold, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr))
            Spacer(Modifier.height(4.dp))
            Txt(subtitle, if (latinSubtitle) style(13.sp, p.subtitle, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr) else local(13, p.subtitle))
        }
        Spacer(Modifier.width(12.dp))
        AppIcon(Glyph.OpenInNew, p.accent, 18.dp)
    }
}

private const val NOTES_VERSION = "1.2.21+50"

/** What changed in this release (version_screen.dart). */
@Composable
fun VersionScreen() {
    val p = palette
    val sections = listOf(
        R.string.versionDetailsFeatures to R.string.versionDetailsFeaturesBody,
        R.string.versionDetailsDesign to R.string.versionDetailsDesignBody,
        R.string.versionDetailsEnhancements to R.string.versionDetailsEnhancementsBody,
        R.string.versionDetailsBugFixes to R.string.versionDetailsBugFixesBody,
    )
    ReadingPage(tr(R.string.version)) {
        item {
            // The notes below are for this release of the Flutter app; 2.0.0 needs its own before it ships.
            Txt(NOTES_VERSION, local(24, p.title, FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Txt(tr(R.string.versionDetailsIntro), local(15, p.subtitle, lineHeight = 1.6f))
        }
        sections.forEach { (title, body) ->
            item {
                Column(Modifier.padding(top = 24.dp)) {
                    Txt(tr(title), local(17, p.title, FontWeight.Bold, 1.35f))
                    Spacer(Modifier.height(8.dp))
                    Txt(tr(body), local(15, p.subtitle, lineHeight = 1.65f))
                }
            }
        }
    }
}

/** One entry in Licences: a library, font or text and its licence. */
class Licence(val name: String, val text: String)

/** res/raw/licenses.json (scripts/flutter_data_to_native.py), read once. */
object LicenceStore {
    @Volatile var cached: List<Licence>? = null
        private set

    suspend fun get(context: Context): List<Licence> = cached ?: withContext(Dispatchers.IO) {
        val json = org.json.JSONArray(context.resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() })
        List(json.length()) { json.getJSONObject(it).let { o -> Licence(o.getString("package"), o.getString("text")) } }
    }.also { cached = it }
}

/** What Rafeeq is built on (licenses_screen.dart). */
@Composable
fun LicencesScreen() {
    val context = LocalContext.current
    val nav = LocalNavigator.current
    val p = palette
    var licences by remember { mutableStateOf(LicenceStore.cached) }
    LaunchedEffect(Unit) { if (licences == null) licences = LicenceStore.get(context) }
    val rtl = Settings.language.rtl
    ReadingPage(tr(R.string.licenses)) {
        item { Txt(tr(R.string.licensesIntro), local(15, p.subtitle, lineHeight = 1.55f), Modifier.padding(bottom = 16.dp)) }
        val list = licences
        if (list == null) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) } }
            return@ReadingPage
        }
        list.forEachIndexed { i, licence ->
            item(key = licence.name) {
                Row(
                    Modifier.padding(bottom = 8.dp).fillMaxWidth().pressable({ nav.push(com.satechdev.rafeeqapp.Route.LicenceDetail(i)) })
                        .clip(RoundedCornerShape(12.dp)).background(p.card).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        // Names are identifiers, so they stay Latin.
                        Txt(licence.name, style(15.sp, p.title, FontWeight.SemiBold, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr), maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Txt(LocalStrings.current.getQuantityString(R.plurals.licensesCount, 1, 1), local(13, p.subtitle))
                    }
                    AppIcon(if (rtl) Glyph.ChevronLeft else Glyph.ChevronRight, p.subtitle, 24.dp)
                }
            }
        }
    }
}

/** One licence in full. */
@Composable
fun LicenceDetailScreen(index: Int) {
    val p = palette
    val licence = LicenceStore.cached?.getOrNull(index) ?: return
    ReadingPage(licence.name) {
        // Licence texts are English and hard-wrapped: each paragraph's lines flow
        // together again, as Flutter's LicenseEntryWithLineBreaks does.
        licence.text.split(Regex("\n\\s*\n")).map { it.trim().replace(Regex("\\s*\n\\s*"), " ") }.forEach { paragraph ->
            item {
                SelectionContainer {
                    Txt(
                        paragraph.trim(),
                        style(13.sp, p.subtitle, family = AppFonts.sans, lineHeight = 1.5f).copy(textDirection = TextDirection.Ltr),
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                    )
                }
            }
        }
    }
}
