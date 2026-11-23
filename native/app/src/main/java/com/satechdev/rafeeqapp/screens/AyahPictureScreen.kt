package com.satechdev.rafeeqapp.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.Ayah
import com.satechdev.rafeeqapp.data.quran.Edition
import com.satechdev.rafeeqapp.data.quran.Editions
import com.satechdev.rafeeqapp.data.quran.QuranStore
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.CalligraphyMark
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localDigits
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.quranStyle
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Choices made in the picture maker's pickers, read back when it shows again (Flutter pops a result). */
object PicturePick {
    var surah by mutableStateOf<Int?>(null)
    var ayah by mutableStateOf<Int?>(null)
    var edition by mutableStateOf<String?>(null)

    /** The ones the pickers mark as chosen. */
    var current by mutableStateOf(Triple(1, 1, ""))
}

private const val SHARE_TEXT = "Shared From Rafeeq - Muslim Companion, Islamic App For Muslims.\n\n" +
    "You Can Also Install The App: https://play.google.com/store/apps/details?id=com.satechdev.rafeeqapp"

/**
 * Makes a square picture of an ayah, or of one of the 99 names, to share
 * (lib/ui/ayah_share/ayah_picture_maker_screen.dart).
 */
@Composable
fun AyahPictureScreen(route: Route.AyahPicture) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    val scope = rememberCoroutineScope()
    val asmaNumber = route.asma
    var surah by rememberSaveable { mutableStateOf(route.surah ?: 1) }
    var ayahNumber by rememberSaveable { mutableStateOf(route.ayah ?: 1) }
    var showArabic by rememberSaveable { mutableStateOf(true) }
    var showTranslation by rememberSaveable { mutableStateOf(true) }
    var useUrdu by rememberSaveable { mutableStateOf(Settings.language.rtl) }
    var editionId by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }

    // Picks made while away.
    LaunchedEffect(PicturePick.surah, PicturePick.ayah, PicturePick.edition) {
        PicturePick.surah?.let { surah = it; ayahNumber = 1 }
        PicturePick.ayah?.let { ayahNumber = it }
        PicturePick.edition?.let { editionId = it }
        PicturePick.surah = null; PicturePick.ayah = null; PicturePick.edition = null
    }

    var quran by remember { mutableStateOf(QuranStore.cached) }
    var names by remember { mutableStateOf(AsmaStore.cached) }
    var editions by remember { mutableStateOf<List<Edition>?>(null) }
    LaunchedEffect(Unit) {
        if (asmaNumber != null) {
            if (names == null) names = AsmaStore.get(context)
        } else {
            if (quran == null) quran = QuranStore.get(context)
            editions = runCatching { Editions.list() }.getOrNull()
        }
    }
    // Each language mode starts from its first translation, as Flutter does.
    fun firstEdition(urdu: Boolean) = editions?.firstOrNull { it.language.lowercase() == if (urdu) "ur" else "en" }?.identifier
    LaunchedEffect(editions) { if (editionId == null) editionId = firstEdition(useUrdu) }
    val edition = editionId ?: Settings.quranView.translationEditionId

    val asma = asmaNumber?.let { n -> names?.firstOrNull { it.number == n } }
    val text = quran
    val ayah: Ayah? = text?.surah(surah.coerceIn(1, 114))?.ayahs?.let { a -> a.firstOrNull { it.numberInSurah == ayahNumber } ?: a.first() }
    var translation by remember { mutableStateOf<Map<Int, String>?>(null) }
    var loadingTranslation by remember { mutableStateOf(false) }
    LaunchedEffect(surah, edition, asmaNumber) {
        if (asmaNumber != null) return@LaunchedEffect
        loadingTranslation = true
        translation = runCatching { Editions.surah(context, surah, edition) }.getOrNull()
        loadingTranslation = false
    }
    SideEffect { PicturePick.current = Triple(surah, ayahNumber, edition) }

    val layer = rememberGraphicsLayer()
    fun share() {
        sharing = true
        scope.launch {
            runCatching {
                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                sharePicture(context, bitmap)
            }
            sharing = false
        }
    }

    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(if (asmaNumber != null) R.string.asmaPicture else R.string.homeAyahPicture), onBack = nav::pop) {
            Box(Modifier.size(48.dp).pressable(if (sharing) null else ::share, scale = .85f), contentAlignment = Alignment.Center) {
                if (sharing) Spinner(p.accent, 20.dp, 2.dp) else AppIcon(Glyph.CupertinoShare, p.accent, 24.dp)
            }
        }
        val ready = if (asmaNumber != null) asma != null else ayah != null
        if (!ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = minOf(maxWidth, 760.dp, maxHeight * .65f)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(side).drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    },
                ) {
                    if (asma != null) {
                        val urduText = asma.description(useUrdu)
                        PictureCard(
                            arabic = asma.name,
                            translation = listOf(asma.meaning(useUrdu), urduText).filter { it.isNotBlank() }.joinToString("\n\n"),
                            translationRtl = useUrdu,
                            reference = if (useUrdu) "اسماء الحسنیٰ ${localDigits(asma.number.toString(), com.satechdev.rafeeqapp.data.AppLanguage.Urdu)}" else "Asma'ul Husna ${asma.number}",
                            showArabic = showArabic,
                            showTranslation = showTranslation,
                            loading = false,
                            urduMode = useUrdu,
                            key = "asma-${asma.number}",
                        )
                    } else if (ayah != null) {
                        val ref = "${ayah.surahNumber}:${ayah.numberInSurah}"
                        PictureCard(
                            arabic = ayah.text,
                            translation = translation?.get(ayah.number),
                            translationRtl = edition.substringBefore('.').lowercase() in rtlLanguages,
                            reference = if (useUrdu) "القرآن ${localDigits(ref, com.satechdev.rafeeqapp.data.AppLanguage.Urdu)}" else "Qur'an $ref",
                            showArabic = showArabic,
                            showTranslation = showTranslation,
                            loading = showTranslation && loadingTranslation,
                            urduMode = useUrdu,
                            key = "ayah-${ayah.number}-$edition",
                        )
                    }
                }
                LazyColumn(
                    Modifier.widthIn(max = 760.dp).fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = playerAwareBottom),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // One of Arabic and translation always stays on.
                    item(key = "arabic") {
                        SettingsCard(Glyph.TextFields, tr(R.string.showArabic), tr(if (asma != null) R.string.includeArabicName else R.string.includeArabicAyah), switch = showArabic) {
                            if (!showArabic || showTranslation) showArabic = !showArabic
                        }
                    }
                    item(key = "translation") {
                        SettingsCard(
                            Glyph.TranslateRounded,
                            tr(if (asma != null) R.string.showMeaning else R.string.showTranslation),
                            tr(if (asma != null) R.string.includeNameMeaning else R.string.includeTranslation),
                            switch = showTranslation,
                        ) { if (!showTranslation || showArabic) showTranslation = !showTranslation }
                    }
                    item(key = "urdu") {
                        val sub = when {
                            asma != null -> if (useUrdu) R.string.showingMeaningInUrdu else R.string.showMeaningInUrdu
                            else -> if (useUrdu) R.string.usingUrduPictureLayout else R.string.useUrduPictureLayout
                        }
                        SettingsCard(Glyph.Translate, tr(R.string.urduMode), tr(sub), switch = useUrdu) {
                            useUrdu = !useUrdu
                            if (asma == null) firstEdition(useUrdu)?.let { editionId = it }
                        }
                    }
                    if (asma == null && ayah != null) {
                        if (showArabic) {
                            item(key = "surah") {
                                val title = if (Settings.language.rtl) "${localNumber(surah)} - ${SurahNames.urdu[surah]}" else "$surah - ${text.surah(surah).englishName}"
                                PickerTile(title, selected = false, subtitle = tr(R.string.chooseSurah)) { nav.push(Route.PictureSurahPicker) }
                            }
                            item(key = "ayah") {
                                PickerTile(tr(R.string.ayahNumber, localNumber(ayah.numberInSurah)), selected = false, subtitle = tr(R.string.jumpToAyah)) { nav.push(Route.PictureAyahPicker) }
                            }
                        }
                        if (showTranslation) {
                            item(key = "edition") {
                                val e = editions?.firstOrNull { it.identifier == edition }
                                val name = e?.let { if (Settings.language.rtl) it.name else it.englishName } ?: edition
                                PickerTile(name, selected = false, subtitle = tr(R.string.translationEdition)) { nav.push(Route.TranslationPicker(forPicture = true)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The square card (ayah_picture_card.dart): Arabic and translation sized to
 * fit, the reference in a top corner and the Rafeeq mark in the opposite
 * bottom one. A new ayah or name crossfades in.
 */
@Composable
private fun PictureCard(
    arabic: String,
    translation: String?,
    translationRtl: Boolean,
    reference: String,
    showArabic: Boolean,
    showTranslation: Boolean,
    loading: Boolean,
    urduMode: Boolean,
    key: String,
) {
    val p = palette
    val hasArabic = showArabic && arabic.isNotBlank()
    val hasTranslation = showTranslation && !translation.isNullOrBlank()
    // The picture is laid out the same way in either app language.
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxSize().aspectRatio(1f).background(p.card)) {
            val inner = maxHeight - 80.dp
            val arabicMax = (inner.value * .13f).coerceIn(22f, 42f)
            val translationMax = (inner.value * .065f).coerceIn(12f, 20f)
            AnimatedContent(
                Triple(key, hasArabic to hasTranslation, loading),
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                label = "picture",
                modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 32.dp, end = 12.dp, bottom = 48.dp),
            ) { (_, shown, isLoading) ->
                val (a, t) = shown
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 24.dp, 2.dp) }
                    return@AnimatedContent
                }
                // Space shared by how much each has to say.
                val arabicLoad = arabic.length * 1.25f
                val translationLoad = translation?.length ?: 0
                val arabicFlex = if (a && t) ((arabicLoad / (arabicLoad + translationLoad)) * 10).let { Math.round(it) }.coerceIn(3, 7) else 1
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    if (a) {
                        BasicText(
                            arabic,
                            Modifier.fillMaxWidth().weight(arabicFlex.toFloat(), fill = false),
                            quranStyle(arabicMax.sp, p.title, TextAlign.Center).copy(lineHeight = 1.55.em, textDirection = TextDirection.Rtl),
                            autoSize = TextAutoSize.StepBased(minFontSize = 4.sp, maxFontSize = arabicMax.sp, stepSize = .5.sp),
                        )
                    }
                    if (a && t) Spacer(Modifier.height(8.dp))
                    if (t) {
                        BasicText(
                            translation!!,
                            Modifier.fillMaxWidth().weight((if (a) 10 - arabicFlex else 1).toFloat(), fill = false),
                            style(
                                (if (translationRtl) translationMax + 3 else translationMax).sp, p.subtitle,
                                family = if (translationRtl) AppFonts.urdu else AppFonts.sans,
                                lineHeight = 1.45f, align = TextAlign.Center,
                            ).copy(textDirection = if (translationRtl) TextDirection.Rtl else TextDirection.Ltr),
                            autoSize = TextAutoSize.StepBased(minFontSize = 4.sp, maxFontSize = (if (translationRtl) translationMax + 3 else translationMax).sp, stepSize = .5.sp),
                        )
                    }
                }
            }
            Txt(
                reference,
                style(if (urduMode) 15.sp else 12.sp, p.subtitle, FontWeight.SemiBold, family = if (urduMode) AppFonts.urdu else AppFonts.sans),
                Modifier.align(if (urduMode) Alignment.TopEnd else Alignment.TopStart).padding(12.dp),
            )
            CalligraphyMark(p.title, Modifier.align(if (urduMode) Alignment.BottomStart else Alignment.BottomEnd).padding(12.dp).size(24.dp))
        }
    }
}

/** Saves the picture in the cache and hands it to the share sheet with the install line. */
private suspend fun sharePicture(context: Context, bitmap: Bitmap) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "shared").apply { mkdirs() }.resolve("rafeeq_ayah_picture.png").also { f ->
            f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** The surah for the picture. */
@Composable
fun PictureSurahPickerScreen() {
    val nav = LocalNavigator.current
    val quran = QuranStore.cached
    val urdu = Settings.language.rtl
    PushedScreen(tr(R.string.chooseSurah)) {
        val surahs = quran?.surahs.orEmpty()
        items(surahs.size) { i ->
            val s = surahs[i]
            PickerTile(if (urdu) "${localNumber(s.number)} - ${SurahNames.urdu[s.number]}" else "${s.number} - ${s.englishName}", selected = s.number == PicturePick.current.first) {
                PicturePick.surah = s.number
                nav.pop()
            }
        }
    }
}

/** The ayah of the chosen surah for the picture. */
@Composable
fun PictureAyahPickerScreen() {
    val nav = LocalNavigator.current
    val ayahs = QuranStore.cached?.surah(PicturePick.current.first)?.ayahs.orEmpty()
    PushedScreen(tr(R.string.jumpToAyah)) {
        items(ayahs.size) { i ->
            val a = ayahs[i]
            PickerTile(tr(R.string.ayahNumber, localNumber(a.numberInSurah)), selected = a.numberInSurah == PicturePick.current.second) {
                PicturePick.ayah = a.numberInSurah
                nav.pop()
            }
        }
    }
}
