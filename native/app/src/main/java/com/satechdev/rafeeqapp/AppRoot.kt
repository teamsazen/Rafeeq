package com.satechdev.rafeeqapp

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.ThemeMode
import com.satechdev.rafeeqapp.screens.AppShell
import com.satechdev.rafeeqapp.screens.LanguageScreen
import com.satechdev.rafeeqapp.screens.PlaceholderScreen
import com.satechdev.rafeeqapp.screens.ThemeScreen
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.LocalPalette
import com.satechdev.rafeeqapp.ui.LocalUiFont
import com.satechdev.rafeeqapp.ui.Palette
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max

// ---------- Routes ----------

sealed interface Route {
    val key: String
    data object Shell : Route { override val key = "shell" }
    data object Theme : Route { override val key = "theme" }
    data object Language : Route { override val key = "language" }

    /** A screen that is still Flutter only; it arrives in a later phase. */
    data class Placeholder(@StringRes val title: Int) : Route { override val key = "placeholder-$title" }
}

/** A tiny back stack: push goes deeper, back pops. No navigation library needed (from 99 Names). */
class Navigator {
    val stack = mutableStateListOf<Route>(Route.Shell)
    var forward by mutableStateOf(true); private set

    /** The bottom bar's tab while [Route.Shell] is showing. */
    var tab by mutableIntStateOf(0)

    fun push(route: Route) { forward = true; stack.add(route) }
    fun pop() { if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex) } }
}

val LocalNavigator = compositionLocalOf<Navigator> { error("No navigator") }

// ---------- Strings in the app's language ----------

/** The app's resources in its own language, which can differ from the phone's. */
val LocalStrings = staticCompositionLocalOf<Resources> { error("No strings") }

@Composable
@ReadOnlyComposable
fun tr(@StringRes id: Int): String = LocalStrings.current.getString(id)

// ---------- Hero (shared element) scopes ----------

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Lets an element fly between the screen it was tapped on and the next one. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.hero(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalAnimScope.current ?: return this
    return with(shared) {
        this@hero.sharedBounds(
            rememberSharedContentState(key),
            anim,
            enter = fadeIn(tween(AppMotion.PAGE)),
            exit = fadeOut(tween(AppMotion.PAGE)),
            boundsTransform = { _, _ -> tween(AppMotion.PAGE, easing = AppMotion.drawer) },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit, Alignment.Center),
        )
    }
}

// ---------- Theme reveal ----------

/**
 * Animates a light/dark switch: a snapshot of the old theme sits on top while
 * a circle of the new theme grows out from where the user last tapped (from 99 Names).
 */
class ThemeReveal {
    var snapshot by mutableStateOf<ImageBitmap?>(null)
    var origin = Offset.Zero
    val progress = Animatable(0f)
}

val LocalThemeReveal = compositionLocalOf<(ThemeMode) -> Unit> { { Settings.setThemeMode(it) } }

// ---------- Toast ----------

object Toast {
    var message by mutableStateOf<String?>(null); private set
    var id by mutableIntStateOf(0); private set
    fun show(text: String) { message = text; id++ }
    fun clear() { message = null }
}

/** Status and navigation bar icons that read on the current theme. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppRoot() {
    val systemDark = isSystemInDarkTheme()
    fun isDark(mode: ThemeMode) = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val dark = isDark(Settings.themeMode)
    val p = if (dark) Palette.Dark else Palette.Light
    val language = Settings.language
    val nav = remember { Navigator() }
    val holder = rememberSaveableStateHolder()
    val reveal = remember { ThemeReveal() }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val reduce = reducedMotion()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val strings = remember(language, configuration) {
        val config = Configuration(configuration).apply { setLocale(Locale.forLanguageTag(language.code)) }
        context.createConfigurationContext(config).resources
    }

    val switchTheme: (ThemeMode) -> Unit = { mode ->
        if (isDark(mode) == dark || reveal.snapshot != null) {
            Settings.setThemeMode(mode)
        } else {
            scope.launch {
                reveal.snapshot = runCatching { layer.toImageBitmap() }.getOrNull()
                Settings.setThemeMode(mode)
                reveal.progress.snapTo(0f)
                reveal.progress.animateTo(1f, tween(if (reduce) 250 else 620, easing = AppMotion.easeOut))
                reveal.snapshot = null
            }
        }
    }

    SystemBarIcons(dark)
    BackHandler(nav.stack.size > 1) { nav.pop() }

    CompositionLocalProvider(
        LocalPalette provides p,
        LocalNavigator provides nav,
        LocalStrings provides strings,
        LocalUiFont provides if (language == AppLanguage.Urdu) AppFonts.urdu else AppFonts.sans,
        LocalLayoutDirection provides if (language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalThemeReveal provides switchTheme,
        LocalTextSelectionColors provides TextSelectionColors(p.accent, p.accent.copy(alpha = .3f)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(p.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            e.changes.firstOrNull()?.let { if (it.pressed) reveal.origin = it.position }
                        }
                    }
                },
        ) {
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) {
                SharedTransitionLayout {
                    AnimatedContent(
                        nav.stack.last(),
                        transitionSpec = {
                            val dur = AppMotion.PAGE
                            // Pages come in from the reading end: the right in English, the left in Urdu.
                            val sign = if (language.rtl) -1 else 1
                            if (nav.forward) {
                                slideInHorizontally(tween(dur, easing = AppMotion.drawer)) { it * sign } togetherWith
                                    slideOutHorizontally(tween(dur, easing = AppMotion.drawer)) { -it * sign / 3 } +
                                    fadeOut(tween(dur), .6f)
                            } else {
                                slideInHorizontally(tween(dur, easing = AppMotion.drawer)) { -it * sign / 3 } +
                                    fadeIn(tween(dur), .6f) togetherWith
                                    slideOutHorizontally(tween(dur, easing = AppMotion.drawer)) { it * sign }
                            }.apply { targetContentZIndex = if (nav.forward) 1f else -1f }
                        },
                        contentKey = { it.key },
                        label = "nav",
                    ) { route ->
                        CompositionLocalProvider(
                            LocalSharedScope provides this@SharedTransitionLayout,
                            LocalAnimScope provides this,
                        ) {
                            holder.SaveableStateProvider(route.key) {
                                Box(Modifier.fillMaxSize().background(palette.background)) {
                                    when (route) {
                                        Route.Shell -> AppShell()
                                        Route.Theme -> ThemeScreen()
                                        Route.Language -> LanguageScreen()
                                        is Route.Placeholder -> PlaceholderScreen(route.title)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // The old theme, with a growing circular hole showing the new one.
            reveal.snapshot?.let { image ->
                Canvas(Modifier.fillMaxSize()) {
                    val t = reveal.progress.value
                    val o = reveal.origin
                    val far = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
                        .maxOf { hypot(it.x - o.x, it.y - o.y) }
                    val hole = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        addOval(Rect(o, max(far * t, .01f)))
                    }
                    if (reduce) {
                        drawImage(image, alpha = 1 - t)
                    } else {
                        clipPath(hole) { drawImage(image) }
                    }
                }
            }

            ToastHost(Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ToastHost(modifier: Modifier) {
    val p = palette
    val text = Toast.message
    LaunchedEffect(Toast.id) {
        if (text != null) { delay(2200); Toast.clear() }
    }
    AnimatedVisibility(
        text != null,
        modifier.navigationBarsPadding().padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
        enter = slideInVertically(tween(260, easing = AppMotion.easeOut)) { it } + fadeIn(),
        exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(180)),
    ) {
        Box(Modifier.background(p.title, RoundedCornerShape(50)).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Txt(text ?: "", style(14.sp, p.background, FontWeight.Medium))
        }
    }
}
