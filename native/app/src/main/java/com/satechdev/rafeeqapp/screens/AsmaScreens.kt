package com.satechdev.rafeeqapp.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.hero
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.quranStyle
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.absoluteValue

/** One of the 99 names (lib/data/providers/asma_ul_husna_provider.dart). */
data class AsmaName(
    val number: Int,
    val name: String,
    val transliteration: String,
    val meaning: String,
    val desc: String,
    val urduMeaning: String,
) {
    fun meaning(urdu: Boolean) = if (urdu && urduMeaning.isNotEmpty()) urduMeaning else meaning

    /** Flutter's asmaUlHusnaUrduDescription in Urdu, the English description otherwise. */
    fun description(urdu: Boolean) =
        if (!urdu) desc else if (urduMeaning.isEmpty()) "" else "یہ اللہ تعالیٰ کا صفاتی نام ہے۔ اللہ تعالیٰ $urduMeaning ہے۔"
}

/** res/raw/asma_ul_husna.json (scripts/asma_to_native.py), read once. */
object AsmaStore {
    @Volatile var cached: List<AsmaName>? = null
        private set

    suspend fun get(context: Context): List<AsmaName> = cached ?: withContext(Dispatchers.IO) {
        val json = JSONArray(context.resources.openRawResource(R.raw.asma_ul_husna).bufferedReader().use { it.readText() })
        List(json.length()) { i ->
            json.getJSONObject(i).run {
                AsmaName(getInt("number"), getString("name"), getString("transliteration"), getString("meaning"), getString("desc"), getString("urduMeaning"))
            }
        }
    }.also { cached = it }
}

@Composable
private fun rememberNames(): List<AsmaName>? {
    val context = LocalContext.current
    var names by remember { mutableStateOf(AsmaStore.cached) }
    LaunchedEffect(Unit) { if (names == null) names = AsmaStore.get(context) }
    return names
}

/** The 99 names (asma_ul_husna_screen.dart). A name flies into its page. */
@Composable
fun AsmaListScreen() {
    val nav = LocalNavigator.current
    val p = palette
    val names = rememberNames()
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.asmaulhusna), onBack = nav::pop)
        if (names == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            return@Column
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = playerAwareBottom),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(names, key = { it.number }) { name ->
                    AsmaCard(name, onOpen = { nav.push(Route.AsmaDetail(name.number - 1)) })
                }
            }
        }
    }
}

@Composable
private fun AsmaCard(item: AsmaName, onOpen: () -> Unit) {
    val p = palette
    val nav = LocalNavigator.current
    val urdu = Settings.language.rtl
    val description = item.description(urdu)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card)) {
        Column(Modifier.fillMaxWidth().pressable(onOpen, scale = .985f).padding(12.dp)) {
            Txt("${localNumber(item.number)}- ${item.transliteration}", style(15.sp, p.title, FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            Txt(
                item.name,
                quranStyle(40.sp, p.title, TextAlign.Right).copy(lineHeight = 1.75.em).copy(textDirection = TextDirection.Rtl),
                Modifier.fillMaxWidth().hero("asma-${item.number}"),
            )
            Spacer(Modifier.height(10.dp))
            Txt(item.meaning(urdu), style(16.sp, p.title, FontWeight.SemiBold, lineHeight = 1.4f))
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Txt(description, style(14.sp, p.subtitle, lineHeight = 1.4f))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Row(Modifier.padding(4.dp)) {
            Box(Modifier.size(48.dp).pressable({ nav.push(Route.Placeholder(R.string.homeAyahPicture)) }, scale = .85f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CupertinoShare, p.title, 22.dp)
            }
        }
    }
}

/** One name per page; swipe through all 99 (asma_ul_husna_detail_screen.dart). */
@Composable
fun AsmaDetailScreen(initialIndex: Int) {
    val nav = LocalNavigator.current
    val p = palette
    val names = rememberNames() ?: return
    val pager = rememberPagerState(initialPage = initialIndex) { names.size }
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(
            "${localNumber(names[pager.currentPage].number)} / ${localNumber(names.size)}",
            onBack = nav::pop,
        ) {
            Box(Modifier.size(48.dp).pressable({ nav.push(Route.Placeholder(R.string.homeAyahPicture)) }, scale = .85f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CupertinoShare, p.title, 22.dp)
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { names[it].number }) { index ->
            // Pages ease in as they settle: a little smaller and fainter while off to the side.
            val shift = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val s = 1f - .06f * shift
                    scaleX = s; scaleY = s
                    alpha = 1f - .4f * shift
                },
                contentAlignment = Alignment.TopCenter,
            ) {
                AsmaPage(names[index])
            }
        }
    }
}

@Composable
private fun AsmaPage(item: AsmaName) {
    val p = palette
    val urdu = Settings.language.rtl
    val description = item.description(urdu)
    Column(
        Modifier.widthIn(max = 760.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = playerAwareBottom),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(horizontal = 12.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt(localNumber(item.number), style(15.sp, p.accent, FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            // Long names shrink to fit one line, as Flutter's FittedBox.
            BasicText(
                item.name,
                Modifier.hero("asma-${item.number}"),
                quranStyle(88.sp, p.title, TextAlign.Center).copy(lineHeight = 1.6.em, textDirection = TextDirection.Rtl),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 88.sp),
            )
            Spacer(Modifier.height(8.dp))
            Txt(item.transliteration, style(22.sp, p.title, FontWeight.Bold, family = AppFonts.sans, align = TextAlign.Center))
        }
        Section(tr(R.string.asmaulhusnaMeaning)) {
            Txt(item.meaning(urdu), style(22.sp, p.title, FontWeight.SemiBold, lineHeight = 1.5f))
        }
        if (description.isNotEmpty()) {
            Section(tr(R.string.asmaulhusnaExplanation)) {
                Txt(description, style(16.sp, p.subtitle, lineHeight = 1.55f))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val p = palette
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(12.dp)) {
        Txt(title, style(13.sp, p.accent, FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        content()
    }
}
