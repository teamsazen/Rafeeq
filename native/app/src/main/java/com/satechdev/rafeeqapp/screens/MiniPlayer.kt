package com.satechdev.rafeeqapp.screens

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableFloatStateOf
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.audio.AudioState
import com.satechdev.rafeeqapp.data.audio.PlaybackScope
import com.satechdev.rafeeqapp.data.audio.QuranAudio
import com.satechdev.rafeeqapp.data.quran.Ayah
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** The mini player's height, without the system bar under it. */
val MiniPlayerHeight = 56.dp

/** Bottom padding for a list that the mini player may cover (Flutter's quranPlayerAwareBottomPadding). */
val playerAwareBottom get() = if (QuranAudio.state.isVisible) 112.dp else ScreenBottomPadding

private val audioScope = MainScope()

/** Starts playback, with a toast if the audio cannot be loaded. */
fun playAudio(context: Context, block: suspend () -> Unit) {
    audioScope.launch {
        runCatching { block() }.onFailure { Toast.show(context.getString(R.string.unableToPlayQuranAudio)) }
    }
}

/** Plays the open section from [ayah]: the whole juz in a juz, else the surah. */
fun playFrom(context: Context, section: Section, ayah: Ayah) = playAudio(context) {
    when (section) {
        is Section.OfJuz -> QuranAudio.playJuz(context, section.number, ayah.number)
        is Section.OfSurah -> QuranAudio.playSurah(context, ayah.surahNumber, ayah.numberInSurah)
    }
}

/** The player's place, as named in its title: the surah, or the juz. */
@Composable
internal fun playingTitle(state: AudioState): String {
    val urdu = Settings.language.rtl
    val juz = state.scopeNumber.takeIf { state.scope == PlaybackScope.Juz }
    return when {
        juz != null -> if (urdu) "${tr(R.string.juz)} ${localNumber(juz)} • ${JuzNames.urdu[juz - 1]}" else "Juz $juz"
        urdu && state.surahNumber != null -> SurahNames.urdu[state.surahNumber]
        else -> state.surahName
    }
}

/**
 * Slides the mini player up from the bottom while something is playing.
 * [overlay] adds the system bar inset, for screens without a bar of their own.
 */
@Composable
fun MiniPlayerSlot(where: PlayerPlace, modifier: Modifier = Modifier, overlay: Boolean = false, show: Boolean = true) {
    val still = reducedMotion()
    AnimatedVisibility(
        show && QuranAudio.state.isVisible,
        modifier,
        enter = if (still) fadeIn(tween(0)) else slideInVertically(spring(dampingRatio = .82f, stiffness = 420f)) { it } + fadeIn(tween(160)),
        exit = if (still) fadeOut(tween(0)) else slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
    ) {
        Box(Modifier.background(palette.card).then(if (overlay) Modifier.navigationBarsPadding() else Modifier)) {
            MiniPlayer(where)
        }
    }
}

/** Where the mini player sits, which decides what tapping it opens. */
enum class PlayerPlace { Elsewhere, Reader, AyahFocus }

/** The Quran mini player, as QuranMiniPlayer in quran_player_overlay.dart. */
@Composable
private fun MiniPlayer(where: PlayerPlace) {
    val p = palette
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val state = QuranAudio.state
    val urdu = Settings.language.rtl
    val progressTarget = when {
        state.isComplete -> state.endProgress
        state.effectiveDuration <= 0 -> 0f
        else -> (state.position.toFloat() / state.effectiveDuration).coerceIn(0f, 1f)
    }
    val progress = smoothProgress(state, progressTarget)
    // Once finished it opens nothing, but still takes the tap so it cannot reach the screen behind.
    val open: () -> Unit = if (state.isComplete) ({}) else ({
        when (where) {
            PlayerPlace.Reader -> nav.push(Route.AyahFocus)
            PlayerPlace.AyahFocus -> nav.push(Route.ChooseReciter)
            PlayerPlace.Elsewhere -> state.currentAyah?.let { ayah ->
                val juz = state.scopeNumber.takeIf { state.scope == PlaybackScope.Juz }
                nav.push(if (juz != null) Route.QuranReader(juz = juz) else Route.QuranReader(surah = ayah.surahNumber, ayah = ayah.numberInSurah))
                ReaderState.jumpTo = ayah.number
            }
        }
    })

    Box(Modifier.fillMaxWidth().height(MiniPlayerHeight).pressable(open, scale = 1f)) {
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).pressable(QuranAudio::stop, scale = .85f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CupertinoClear, p.title, 16.dp)
            }
            Spacer(Modifier.width(2.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                val unit = tr(if (state.scope == PlaybackScope.Juz) R.string.juz else R.string.surah)
                val idle = when (where) {
                    PlayerPlace.AyahFocus -> if (urdu) state.reciter.nativeName else state.reciter.englishName
                    PlayerPlace.Reader -> tr(R.string.tapToExpand)
                    PlayerPlace.Elsewhere -> tr(R.string.tapToView)
                }
                val title = if (state.isComplete) tr(R.string.playbackComplete, unit) else playingTitle(state)
                val label = if (state.scope == PlaybackScope.Juz) "Juz ${state.scopeNumber ?: ""}".trim() else state.surahName
                val subtitle = if (state.isComplete) "$label • ${localNumber(((state.endRemaining + 999) / 1000).toInt())}s"
                else "$idle • ${localNumber(state.currentAyah?.numberInSurah ?: 1)}"
                Txt(title, style(15.sp, p.title, FontWeight.Bold), maxLines = 1)
                // The ayah number rolls over as the recitation moves on.
                AnimatedContent(subtitle, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "ayah") {
                    Txt(it, style(13.sp, p.subtitle), maxLines = 1)
                }
            }
            if (state.isComplete) {
                if (state.canPlayPreviousSelection) SectionButton(Glyph.SkipPrevious, "Previous") { playAudio(context) { QuranAudio.playAdjacent(context, -1) } }
                if (state.canPlayNextSelection) SectionButton(Glyph.SkipNext, "Next") { playAudio(context) { QuranAudio.playAdjacent(context, 1) } }
            } else {
                ControlButton(Glyph.SkipPrevious, state.canGoPrevious || state.canPlayPreviousSelection) { QuranAudio.previousOrPreviousSelection(context) }
                PlayPauseButton(state)
                ControlButton(Glyph.SkipNext, state.canGoNext || state.canPlayNextSelection) { QuranAudio.nextOrNextSelection(context) }
            }
        }
        // The progress line along the top; it runs while loading.
        val loading = state.isLoading && !state.isComplete
        val sweep = if (loading) loadingSweep() else 0f
        Canvas(Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)) {
            if (loading) {
                val w = size.width * .3f
                drawRect(p.accent, Offset(sweep * (size.width + w) - w, 0f), Size(w, size.height))
            } else {
                val w = size.width * progress
                drawRect(p.accent, Offset(if (urdu) size.width - w else 0f, 0f), Size(w, size.height))
            }
        }
    }
}

@Composable
private fun loadingSweep(): Float {
    val t = rememberInfiniteTransition(label = "loading")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "sweep")
    return x
}

@Composable
private fun ControlButton(icon: Glyph, enabled: Boolean, onClick: () -> Unit) {
    val p = palette
    Box(Modifier.size(44.dp).pressable(if (enabled) onClick else null, scale = .85f), contentAlignment = Alignment.Center) {
        AppIcon(icon, if (enabled) p.subtitle else p.subtitle.copy(alpha = .3f), 24.dp)
    }
}

/** The round play button; the icon turns between play, pause and a spinner. */
@Composable
private fun PlayPauseButton(state: AudioState) {
    val p = palette
    Box(
        Modifier.padding(horizontal = 2.dp).size(40.dp).clip(CircleShape)
            .background(p.background.copy(alpha = .82f))
            .border(1.dp, p.title.copy(alpha = .1f), CircleShape)
            .pressable(QuranAudio::togglePlayPause, scale = .88f),
        contentAlignment = Alignment.Center,
    ) {
        val icon = when {
            state.isLoading || state.isBuffering -> null
            state.isPlaying -> Glyph.Pause
            else -> Glyph.Play
        }
        AnimatedContent(
            icon,
            transitionSpec = { (scaleIn(spring(dampingRatio = .5f, stiffness = 600f), .6f) + fadeIn(tween(120))) togetherWith (scaleOut(tween(100), .6f) + fadeOut(tween(100))) },
            label = "playPause",
        ) {
            if (it == null) Spinner(p.accent, 16.dp, 1.5.dp) else AppIcon(it, p.title, 22.dp)
        }
    }
}

/** After a surah or juz ends: play the one before or after it. */
@Composable
private fun SectionButton(icon: Glyph, label: String, onClick: () -> Unit) {
    val p = palette
    Column(
        Modifier.width(62.dp).height(48.dp).clip(RoundedCornerShape(10.dp)).pressable(onClick, scale = .9f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(icon, p.title, 18.dp)
        Txt(label, style(10.sp, p.title, FontWeight.SemiBold), maxLines = 1)
    }
}

/** Four bars rising and falling, shown on the index card that is playing. */
@Composable
fun PlayingWave(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(850, easing = LinearEasing)), label = "phase")
    Canvas(modifier.size(24.dp)) {
        val bar = 3.dp.toPx()
        val gap = (size.width - 4 * bar) / 3
        for (i in 0 until 4) {
            val wave = (sin(phase * PI * 2 + i * 1.25).toFloat() + 1) / 2
            val h = 7.dp.toPx() + wave * (size.height - 9.dp.toPx())
            drawRoundRect(color, Offset(i * (bar + gap), (size.height - h) / 2), Size(bar, h), CornerRadius(bar / 2))
        }
    }
}

/**
 * The progress line's fraction, moved on every frame while an ayah plays:
 * the player reports its position only every 200 ms, so the line runs on
 * from the last report by the time since, and a new report just corrects it.
 */
@Composable
private fun smoothProgress(state: AudioState, reported: Float): Float {
    var shown by remember { mutableFloatStateOf(reported) }
    val running = state.isPlaying && !state.isComplete && state.duration > 0
    val position = state.position
    val duration = state.effectiveDuration
    LaunchedEffect(running, position, duration, reported) {
        if (!running) {
            shown = reported
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            shown = ((position + (now - start)).toFloat() / duration).coerceIn(0f, 1f)
        }
    }
    return shown
}
