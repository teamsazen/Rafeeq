package com.satechdev.rafeeqapp.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.quranStyle
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/*
 * The namaz guide (lib/ui/namaz): the five prayers with their rak'at, the
 * complete guide to praying, and wudu. Like the Flutter screens, the words
 * here are written in both languages in place rather than in the ARB files.
 */

data class NamazStep(
    val id: String,
    val titleEn: String,
    val titleUr: String,
    val arabic: String?,
    val urdu: String?,
    val repeat: Int?,
)

data class NamazPrayer(val id: String, val nameEn: String, val nameUr: String, val fard: Int, val sunnah: Int, val steps: List<NamazStep>)

data class WuduStep(val step: Int, val titleEn: String, val titleUr: String, val descriptionEn: String, val descriptionUr: String, val arabic: String?, val repeat: Int?)

/** res/raw/namaz_prayers.json and wudu_steps.json (scripts/flutter_data_to_native.py), read once. */
object NamazStore {
    @Volatile var prayers: List<NamazPrayer>? = null
        private set
    @Volatile var wudu: List<WuduStep>? = null
        private set

    private fun JSONObject.text(key: String) = optString(key).takeIf { has(key) && !isNull(key) && it.isNotEmpty() }
    private fun JSONObject.count(key: String) = if (has(key) && !isNull(key)) getInt(key) else null

    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        fun read(id: Int) = JSONArray(context.resources.openRawResource(id).bufferedReader().use { it.readText() })
        if (prayers == null) {
            val json = read(R.raw.namaz_prayers)
            prayers = List(json.length()) { i ->
                json.getJSONObject(i).run {
                    val c = getJSONArray("components")
                    NamazPrayer(
                        getString("id"), getString("nameEn"), getString("nameUr"), getInt("rakaatFard"), getInt("rakaatSunnah"),
                        List(c.length()) { j ->
                            c.getJSONObject(j).run { NamazStep(getString("id"), getString("titleEn"), getString("titleUr"), text("arabicText"), text("urduTranslation"), count("repeatCount")) }
                        },
                    )
                }
            }
        }
        if (wudu == null) {
            val json = read(R.raw.wudu_steps)
            wudu = List(json.length()) { i ->
                json.getJSONObject(i).run {
                    WuduStep(getInt("step"), getString("titleEn"), getString("titleUr"), getString("descriptionEn"), getString("descriptionUr"), text("arabicText"), count("repeatCount"))
                }
            }
        }
    }
}

@Composable
private fun rememberNamaz(): Boolean {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(NamazStore.prayers != null && NamazStore.wudu != null) }
    LaunchedEffect(Unit) {
        if (!ready) {
            NamazStore.load(context)
            ready = true
        }
    }
    return ready
}

private class RakatPart(val type: String, val count: Int) {
    fun label(urdu: Boolean) = when (type) {
        "sunnah" -> if (urdu) "سنت" else "Sunnah"
        "fard" -> if (urdu) "فرض" else "Fard"
        "nafl" -> if (urdu) "نفل" else "Nafl"
        "witr" -> if (urdu) "وتر" else "Witr"
        else -> type
    }
}

/** The rak'at of each prayer in order, as prayerBreakdown. */
private fun breakdown(prayer: NamazPrayer): List<RakatPart> = when (prayer.id) {
    "fajr" -> listOf(RakatPart("sunnah", 2), RakatPart("fard", 2))
    "duhr" -> listOf(RakatPart("sunnah", 4), RakatPart("fard", 4), RakatPart("sunnah", 2), RakatPart("nafl", 2))
    "asr" -> listOf(RakatPart("sunnah", 4), RakatPart("fard", 4))
    "maghrib" -> listOf(RakatPart("fard", 3), RakatPart("sunnah", 2), RakatPart("nafl", 2))
    "isha" -> listOf(RakatPart("sunnah", 4), RakatPart("fard", 4), RakatPart("sunnah", 2), RakatPart("nafl", 2), RakatPart("witr", 3), RakatPart("nafl", 2))
    else -> listOfNotNull(prayer.sunnah.takeIf { it > 0 }?.let { RakatPart("sunnah", it) }, prayer.fard.takeIf { it > 0 }?.let { RakatPart("fard", it) })
}

/** Text in the app's language: Jameel Noori in Urdu with its taller lines. */
@Composable
private fun local(size: TextUnit, color: androidx.compose.ui.graphics.Color, weight: FontWeight = FontWeight.Normal, urduHeight: Float = 1.5f, englishHeight: Float = 1.3f) =
    if (Settings.language.rtl) style(size, color, weight, family = AppFonts.urdu, lineHeight = urduHeight)
    else style(size, color, weight, family = AppFonts.sans, lineHeight = englishHeight)

/** A screen of cards that rise in one after another as it opens. */
@Composable
private fun StaggeredList(title: String, content: LazyListScope.() -> Unit) {
    val nav = LocalNavigator.current
    val p = palette
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(title, onBack = nav::pop)
        if (!rememberNamaz()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            return@Column
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = playerAwareBottom),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** Rises a little and fades in, [index] steps after the screen opens; only the first screenful waits. */
@Composable
private fun Modifier.riseIn(index: Int): Modifier {
    val still = reducedMotion()
    val t = remember { Animatable(if (still || index > 8) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (t.value < 1f) {
            delay(40L * index)
            t.animateTo(1f, tween(320))
        }
    }
    return graphicsLayer {
        alpha = t.value
        translationY = (1 - t.value) * 24.dp.toPx()
    }
}

/** The five daily prayers and their rak'at (namaz_prayer_screen.dart). */
@Composable
fun NamazScreen() {
    val nav = LocalNavigator.current
    val p = palette
    val urdu = Settings.language.rtl
    StaggeredList(if (urdu) "نماز" else "Namaz") {
        item(key = "guide") {
            LinkCard(
                if (urdu) "مکمل نماز گائیڈ" else "Complete Namaz Guide",
                if (urdu) "وضو، اذکار، دعائیں اور نماز کا طریقہ" else "Wudu, recitations, duas and prayer method",
                Modifier.riseIn(0),
            ) { nav.push(Route.NamazGuide) }
        }
        val prayers = NamazStore.prayers.orEmpty()
        if (prayers.isEmpty()) {
            item { Txt(if (urdu) "نماز کی معلومات لوڈ نہیں ہو سکیں" else "Prayer guide is unavailable", local(14.sp, p.subtitle), Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(16.dp)) }
        }
        itemsIndexed(prayers, key = { _, it -> it.id }) { i, prayer -> PrayerTile(prayer, Modifier.riseIn(i + 1)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerTile(prayer: NamazPrayer, modifier: Modifier) {
    val p = palette
    val urdu = Settings.language.rtl
    val parts = breakdown(prayer)
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(12.dp)) {
        Txt("${if (urdu) prayer.nameUr else prayer.nameEn} (${localNumber(parts.sumOf { it.count })})", local(15.sp, p.title, FontWeight.Bold, 1.45f, 1.25f))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            parts.forEachIndexed { i, part ->
                Txt("${localNumber(part.count)} ${part.label(urdu)}", local(13.sp, p.subtitle, urduHeight = 1.45f, englishHeight = 1.25f))
                if (i != parts.lastIndex) {
                    // Points the way the parts are read.
                    AppIcon(Glyph.CupertinoArrowRight, p.subtitle, 12.dp, Modifier.graphicsLayer { scaleX = if (urdu) -1f else 1f })
                }
            }
        }
    }
}

/** A highlighted card that opens another guide. */
@Composable
private fun LinkCard(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = palette
    val urdu = Settings.language.rtl
    Row(
        modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(12.dp)).background(p.selectedCard).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, local(16.sp, p.selectedTitle, FontWeight.Bold, 1.45f, 1.25f))
            Spacer(Modifier.height(3.dp))
            Txt(subtitle, local(13.sp, p.selectedSubtitle, urduHeight = 1.45f, englishHeight = 1.25f))
        }
        AppIcon(Glyph.CupertinoChevronForward, p.selectedSubtitle, 18.dp, Modifier.graphicsLayer { scaleX = if (urdu) -1f else 1f })
    }
}

private class GuideStep(val id: String, val titleEn: String, val titleUr: String, val english: String)

private const val FATIHA_EN = "All praise belongs to Allah, Lord of all worlds, the Most Compassionate, the Most Merciful, Master of the Day of Judgment. You alone we worship and You alone we ask for help. Guide us to the straight path, the path of those You have blessed, not those who earned anger nor those who went astray."
private const val IKHLAS_EN = "Say: He is Allah, the One. Allah, the Eternal Refuge. He neither begets nor is born, and none is comparable to Him."

/** The steps of a two rak'ah prayer, as _buildGuideSteps. */
private val guideSteps = listOf(
    GuideStep("takbeer", "Takbeer", "تکبیر", "Allah is the Greatest."),
    GuideStep("thana", "Sana", "ثناء", "Glory and praise are Yours, O Allah. Blessed is Your name, exalted is Your majesty, and there is no deity besides You."),
    GuideStep("tauz", "Ta'awwuz", "تعوذ", "I seek refuge in Allah from Satan, the rejected."),
    GuideStep("tasmiah", "Tasmiah", "تسمیہ", "In the name of Allah, the Most Compassionate, the Most Merciful."),
    GuideStep("fatiha", "Al-Fatiha", "الفاتحہ", FATIHA_EN),
    GuideStep("ameen", "Aameen", "آمین", "O Allah, accept this prayer."),
    GuideStep("surah_ikhlas", "Al-Ikhlas", "الاخلاص", IKHLAS_EN),
    GuideStep("ruku", "Ruku", "رکوع", "Glory be to my Lord, the Magnificent."),
    GuideStep("qiyam_ruku", "Standing From Ruku", "رکوع سے قیام", "Allah hears the one who praises Him."),
    GuideStep("response_qiyam", "Response", "جواب", "Our Lord, all praise belongs to You."),
    GuideStep("sajdah_1", "First Sajdah", "پہلا سجدہ", "Glory be to my Lord, the Most High."),
    GuideStep("jalsa_1", "Jalsa", "جلسہ", "Allah is the Greatest."),
    GuideStep("sajdah_2", "Second Sajdah", "دوسرا سجدہ", "Glory be to my Lord, the Most High."),
    GuideStep("second_rakat", "Second Raka - Standing", "دوسری رکعت کے لیے قیام", "Allah is the Greatest."),
    GuideStep("fatiha_2", "Al-Fatiha", "الفاتحہ", FATIHA_EN),
    GuideStep("surah_ikhlas", "Al-Ikhlas", "الاخلاص", IKHLAS_EN),
    GuideStep("ruku_2", "Ruku", "رکوع", "Glory be to my Lord, the Magnificent."),
    GuideStep("qiyam_ruku_2", "Standing From Ruku", "رکوع سے قیام", "Allah hears the one who praises Him."),
    GuideStep("response_qiyam_2", "Response", "جواب", "Our Lord, all praise belongs to You."),
    GuideStep("sajdah_1_2", "First Sajdah", "پہلا سجدہ", "Glory be to my Lord, the Most High."),
    GuideStep("jalsa_2", "Jalsa", "جلسہ", "Allah is the Greatest."),
    GuideStep("sajdah_2_2", "Second Sajdah", "دوسرا سجدہ", "Glory be to my Lord, the Most High."),
    GuideStep(
        "tashahhud", "Tashahhud", "تشہد",
        "All greetings, prayers, and pure words belong to Allah. Peace, mercy, and blessings of Allah be upon you, O Prophet. Peace be upon us and upon the righteous servants of Allah. I testify that there is no deity but Allah, and Muhammad ﷺ is His servant and messenger.",
    ),
    GuideStep(
        "darood", "Darood Sharif", "درود شریف",
        "O Allah, send mercy and blessings upon Muhammad ﷺ and the family of Muhammad ﷺ as You sent mercy and blessings upon Ibrahim and the family of Ibrahim. Surely You are Praiseworthy and Glorious.",
    ),
    GuideStep(
        "dua", "Dua", "دعا",
        "O Allah, I seek refuge in You from the punishment of Hell, the punishment of the grave, the trials of life and death, and the evil trial of the Dajjal.",
    ),
    GuideStep("salam_right", "Salam - Turn Right", "سلام - دائیں جانب", "Peace and the mercy of Allah be upon you."),
    GuideStep("salam_left", "Salam - Turn Left", "سلام - بائیں جانب", "Peace and the mercy of Allah be upon you."),
)

/** The complete guide: wudu, then each step of the prayer (namaz_guide_screen.dart). */
@Composable
fun NamazGuideScreen() {
    val nav = LocalNavigator.current
    val urdu = Settings.language.rtl
    StaggeredList(if (urdu) "مکمل نماز گائیڈ" else "Complete Namaz Guide") {
        item(key = "wudu") {
            LinkCard(
                if (urdu) "وضو کا طریقہ" else "Wudu Guide",
                if (urdu) "نماز سے پہلے وضو کے مراحل" else "Step-by-step ablution before prayer",
                Modifier.riseIn(0),
            ) { nav.push(Route.Wudu) }
        }
        val components = NamazStore.prayers?.firstOrNull()?.steps.orEmpty().associateBy { it.id }
        val steps = guideSteps.mapNotNull { g -> components[g.id]?.let { g to it } }
        itemsIndexed(steps) { i, (guide, step) ->
            StepCard(
                "${localNumber(i + 1)}- ${if (urdu) guide.titleUr else guide.titleEn}",
                step.repeat,
                arabic = step.arabic,
                arabicSize = 30.sp,
                explanation = if (urdu) step.urdu.orEmpty() else guide.english,
                modifier = Modifier.riseIn(i + 1),
            )
        }
    }
}

/** The steps of wudu (wudu_screen.dart). */
@Composable
fun WuduScreen() {
    val urdu = Settings.language.rtl
    StaggeredList(if (urdu) "وضو کا طریقہ" else "Wudu Guide") {
        itemsIndexed(NamazStore.wudu.orEmpty(), key = { _, it -> it.step }) { i, step ->
            StepCard(
                "${localNumber(step.step)}- ${if (urdu) step.titleUr else step.titleEn}",
                step.repeat,
                arabic = step.arabic,
                arabicSize = 28.sp,
                explanation = if (urdu) step.descriptionUr else step.descriptionEn,
                arabicFirst = false,
                modifier = Modifier.riseIn(i),
            )
        }
    }
}

/**
 * A numbered step: its title and how many times, the Arabic and the meaning.
 * Wudu puts the explanation before the Arabic, as Flutter does.
 */
@Composable
private fun StepCard(
    title: String,
    repeat: Int?,
    arabic: String?,
    arabicSize: TextUnit,
    explanation: String,
    modifier: Modifier,
    arabicFirst: Boolean = true,
) {
    val p = palette
    val urdu = Settings.language.rtl
    @Composable fun Arabic() {
        if (!arabic.isNullOrEmpty()) {
            Spacer(Modifier.height(if (arabicFirst) 12.dp else 10.dp))
            Txt(arabic, quranStyle(arabicSize, p.title, TextAlign.Right).copy(lineHeight = 1.75.em, textDirection = TextDirection.Rtl), Modifier.fillMaxWidth())
        }
    }
    @Composable fun Explanation() {
        if (explanation.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Txt(
                explanation,
                local(if (urdu) 20.sp else 17.sp, p.subtitle, urduHeight = 1.55f, englishHeight = 1.4f)
                    .copy(textAlign = if (urdu) TextAlign.Right else TextAlign.Left),
                Modifier.fillMaxWidth(),
            )
        }
    }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Txt(title, local(15.sp, p.title, FontWeight.Bold, 1.5f, 1.25f), Modifier.weight(1f))
            if ((repeat ?: 1) > 1) {
                Spacer(Modifier.width(8.dp))
                Txt("${localNumber(repeat!!)}×", style(14.sp, p.accent, FontWeight.Bold, family = AppFonts.sans))
            }
        }
        if (arabicFirst) {
            Arabic()
            Explanation()
        } else {
            Explanation()
            Arabic()
        }
    }
}
