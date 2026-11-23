package com.satechdev.rafeeqapp.data.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.Ayah
import com.satechdev.rafeeqapp.data.quran.QuranStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class PlaybackScope { Surah, Juz }

/** What the player shows, as the Flutter QuranAudioState. Times in milliseconds. */
data class AudioState(
    val surahNumber: Int? = null,
    val surahName: String = "",
    val scope: PlaybackScope = PlaybackScope.Surah,
    val scopeNumber: Int? = null,
    val reciter: Reciter = Reciter.entries.first(),
    val ayahs: List<Ayah> = emptyList(),
    val currentIndex: Int = 0,
    val position: Long = 0,
    val duration: Long = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val isVisible: Boolean = false,
    val isComplete: Boolean = false,
    /** Counts down after the last ayah before the player hides. */
    val endRemaining: Long = 0,
) {
    val currentAyah get() = ayahs.getOrNull(currentIndex)
    val canGoPrevious get() = currentIndex > 0
    val canGoNext get() = currentIndex + 1 < ayahs.size
    val lastNumber get() = if (scope == PlaybackScope.Juz) 30 else 114
    val canPlayPreviousSelection get() = (scopeNumber ?: 1) > 1
    val canPlayNextSelection get() = (scopeNumber ?: lastNumber) < lastNumber

    /** Until a duration is known, progress runs against ten seconds. */
    val effectiveDuration get() = if ((position == 0L && (isLoading || isBuffering)) || duration <= 0) PROVISIONAL else duration

    val endProgress get() = if (!isComplete) 0f else ((END_GRACE - endRemaining).toFloat() / END_GRACE).coerceIn(0f, 1f)

    companion object {
        const val PROVISIONAL = 10_000L
        const val END_GRACE = 10_000L
    }
}

/**
 * Quran recitation, as lib/ui/quran/quran_audio_controller.dart: a surah
 * or juz as a queue of ayah recordings, played by PlaybackService.
 */
object QuranAudio {
    private const val LAST_PLAYED = "global_last_played_index"
    const val SELECTED_RECITER = "KEY_SELECTED_RECITER"

    var state by mutableStateOf(AudioState())
        private set

    private val scope = MainScope()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var endTimer: Job? = null
    private var generation = 0

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncIndex()

        override fun onEvents(player: Player, events: Player.Events) {
            // After stop() the player still reports its wind-down; there is nothing left to show.
            if (state.ayahs.isEmpty() || player.mediaItemCount != state.ayahs.size) return
            val ended = player.playbackState == Player.STATE_ENDED
            state = state.copy(
                isPlaying = player.isPlaying && !ended,
                isLoading = player.playbackState == Player.STATE_IDLE && state.isLoading,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                position = player.currentPosition,
                duration = player.duration.coerceAtLeast(0),
            )
            if (ended) startEndCountdown()
            if (player.isPlaying) startTicker() else ticker?.cancel()
        }
    }

    private suspend fun controller(context: Context): MediaController {
        controller?.let { return it }
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        return suspendCancellableCoroutine<MediaController> { cont ->
            future.addListener({
                runCatching { future.get() }.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(app))
        }.also {
            it.addListener(listener)
            controller = it
        }
    }

    /** The ayah reciter chosen in Quran settings (Flutter's KEY_SELECTED_RECITER). */
    var selectedReciter by mutableStateOf(Reciter.of(Settings.string(SELECTED_RECITER)))
        private set

    fun reciter(): Reciter = selectedReciter

    /** Saves [reciter] and, if something is playing, carries on with it from the same ayah. */
    suspend fun chooseReciter(context: Context, reciter: Reciter) {
        if (reciter == selectedReciter) return
        selectedReciter = reciter
        Settings.setString(SELECTED_RECITER, reciter.identifier)
        reload(context)
    }

    suspend fun playSurah(context: Context, surah: Int, startAyah: Int = 1) {
        val text = QuranStore.get(context)
        val ayahs = text.surah(surah).ayahs
        val index = ayahs.indexOfFirst { it.numberInSurah == startAyah }.coerceAtLeast(0)
        play(context, PlaybackScope.Surah, surah, ayahs, index)
    }

    suspend fun playJuz(context: Context, juz: Int, startGlobal: Int? = null) {
        val ayahs = QuranStore.get(context).juz(juz)
        if (ayahs.isEmpty()) return
        val index = startGlobal?.let { g -> ayahs.indexOfFirst { it.number == g } }?.takeIf { it >= 0 } ?: 0
        play(context, PlaybackScope.Juz, juz, ayahs, index)
    }

    /** Plays the chosen reciter from the current ayah, after a change of reciter. */
    suspend fun reload(context: Context) {
        val s = state
        val number = s.scopeNumber ?: return
        if (s.ayahs.isEmpty()) return
        play(context, s.scope, number, s.ayahs, s.currentIndex, force = true)
    }

    private suspend fun play(context: Context, playbackScope: PlaybackScope, number: Int, ayahs: List<Ayah>, index: Int, force: Boolean = false) {
        if (ayahs.isEmpty()) return
        endTimer?.cancel()
        val player = controller(context)
        // The same selection again: start from the ayah without reloading.
        if (!force && state.scope == playbackScope && state.scopeNumber == number && state.ayahs.isNotEmpty() && player.mediaItemCount == ayahs.size) {
            state = state.copy(isVisible = true, isComplete = false, endRemaining = 0)
            player.seekTo(index, 0)
            player.play()
            return
        }
        val load = ++generation
        val text = QuranStore.get(context)
        val reciter = reciter()
        state = AudioState(
            surahNumber = ayahs[index].surahNumber,
            surahName = text.surah(ayahs[index].surahNumber).englishName,
            scope = playbackScope,
            scopeNumber = number,
            reciter = reciter,
            ayahs = ayahs,
            currentIndex = index,
            isLoading = true,
            isVisible = true,
        )
        val items = ayahs.map { ayah ->
            val uri = reciter.ayahLink(ayah.number, ayah.surahNumber, ayah.numberInSurah)
            MediaItem.Builder()
                .setMediaId(uri)
                .setUri(Uri.parse(uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("${text.surah(ayah.surahNumber).englishName} ${ayah.numberInSurah}")
                        .setArtist(reciter.englishName)
                        .setAlbumTitle("Quran")
                        .build(),
                )
                .build()
        }
        if (load != generation) return
        player.setMediaItems(items, index, 0)
        player.prepare()
        player.play()
        state = state.copy(isLoading = false)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (state.isLoading || state.ayahs.isEmpty()) return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0)
            player.play()
        }
    }

    fun previous() {
        val player = controller ?: return
        if (state.canGoPrevious) player.seekTo(state.currentIndex - 1, 0) else player.seekTo(0)
    }

    fun next() {
        val player = controller ?: return
        if (state.canGoNext) player.seekTo(state.currentIndex + 1, 0)
    }

    fun previousOrPreviousSelection(context: Context) {
        when {
            state.canGoPrevious -> previous()
            state.canPlayPreviousSelection -> scope.launch { playAdjacent(context, -1, startAtEnd = true) }
        }
    }

    fun nextOrNextSelection(context: Context) {
        when {
            state.canGoNext -> next()
            state.canPlayNextSelection -> scope.launch { playAdjacent(context, 1) }
        }
    }

    fun jumpTo(globalAyah: Int) {
        val index = state.ayahs.indexOfFirst { it.number == globalAyah }
        if (index >= 0) controller?.seekTo(index, 0)
    }

    suspend fun playAdjacent(context: Context, offset: Int, startAtEnd: Boolean = false) {
        val current = state.scopeNumber ?: return
        val target = current + offset
        if (target < 1 || target > state.lastNumber) return
        endTimer?.cancel()
        if (state.scope == PlaybackScope.Juz) {
            val start = if (startAtEnd) QuranStore.get(context).juz(target).lastOrNull()?.number else null
            playJuz(context, target, start)
        } else {
            val surah = QuranStore.get(context).surah(target)
            playSurah(context, target, if (startAtEnd) surah.ayahs.last().numberInSurah else 1)
        }
    }

    fun stop() {
        endTimer?.cancel()
        ticker?.cancel()
        generation++
        controller?.run {
            stop()
            clearMediaItems()
        }
        state = AudioState()
    }

    // ---------- Internals ----------

    private fun syncIndex() {
        val player = controller ?: return
        val index = player.currentMediaItemIndex
        val ayah = state.ayahs.getOrNull(index) ?: return
        val text = QuranStore.cached
        state = state.copy(
            currentIndex = index,
            surahNumber = ayah.surahNumber,
            surahName = text?.surah(ayah.surahNumber)?.englishName ?: state.surahName,
            position = player.currentPosition,
            duration = player.duration.coerceAtLeast(0),
        )
        Settings.setInt(LAST_PLAYED, ayah.number - 1)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                controller?.let { state = state.copy(position = it.currentPosition, duration = it.duration.coerceAtLeast(0)) }
                delay(200)
            }
        }
    }

    /** After the last ayah the player stays for ten seconds, offering the next selection, then hides. */
    private fun startEndCountdown() {
        if (state.isComplete && endTimer?.isActive == true) return
        endTimer?.cancel()
        state = state.copy(position = state.duration, isPlaying = false, isLoading = false, isBuffering = false, isVisible = true, isComplete = true, endRemaining = AudioState.END_GRACE)
        endTimer = scope.launch {
            while (state.endRemaining > 0) {
                delay(100)
                state = state.copy(endRemaining = (state.endRemaining - 100).coerceAtLeast(0))
            }
            state = state.copy(isVisible = false)
        }
    }
}
