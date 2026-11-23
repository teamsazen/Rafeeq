package com.satechdev.rafeeqapp.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.LocalStrings
import com.satechdev.rafeeqapp.LocalThemeReveal
import com.satechdev.rafeeqapp.ONBOARDING_DONE
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.LocationService
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.ThemeMode
import com.satechdev.rafeeqapp.data.prayer.Reminders
import com.satechdev.rafeeqapp.data.prayer.TimeFormat
import com.satechdev.rafeeqapp.data.prayer.formatTime
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.OnboardingArt
import com.satechdev.rafeeqapp.ui.OnboardingArtwork
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime

private enum class Step { Welcome, Language, Notifications, Location, Theme }

private const val ACTIONS_HEIGHT = 52

/** First run, as lib/ui/onboarding/onboarding_screen.dart: welcome, language, notifications, location, theme. */
@Composable
fun OnboardingScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val p = palette
    val still = reducedMotion()
    var step by remember { mutableStateOf(Step.Welcome) }
    var forward by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var locationTouched by remember { mutableStateOf(Settings.location != null) }
    remember { LocationDraft.reset() }

    // Onboarding starts on the system theme, as in Flutter.
    LaunchedEffect(Unit) { if (Settings.themeMode != ThemeMode.System) Settings.setThemeMode(ThemeMode.System) }

    fun go(next: Step) {
        forward = next.ordinal > step.ordinal
        step = next
    }
    fun next() {
        if (step == Step.Theme) {
            Settings.setFlag(ONBOARDING_DONE)
            Reminders.schedule(context, force = true)
            nav.reset(Route.Shell)
        } else {
            go(Step.entries[step.ordinal + 1])
        }
    }
    BackHandler(step != Step.Welcome) { go(Step.entries[step.ordinal - 1]) }

    // Notifications: the permission, then exact alarms, as Flutter's requestPermissions.
    fun askExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Reminders.canScheduleExact(context)) {
            runCatching {
                context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null)))
            }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) askExactAlarms()
        busy = false
        next()
    }

    // Location: GPS on request, or quietly on Next when nothing was chosen.
    // A fix can take a while indoors, so after four seconds onboarding moves
    // on and the search carries on in the background, saving when it lands.
    var quietOnNext by remember { mutableStateOf(false) }
    fun useGps() {
        busy = true
        val search = LocationService.locate(context)
        scope.launch {
            val quick = withTimeoutOrNull(4_000) { search.await() }
            busy = false
            if (quick == null) {
                quietOnNext = false
                if (step == Step.Location) next()
                val late = search.await()
                Toast.show(strings.getString(late.message))
                return@launch
            }
            if (quick == LocationService.Located.Updated) {
                LocationDraft.reset()
                LocationDraft.manualExpanded = false
            }
            if (quietOnNext) {
                quietOnNext = false
                next()
            } else {
                Toast.show(strings.getString(quick.message))
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            useGps()
        } else if (quietOnNext) {
            quietOnNext = false
            next()
        } else {
            Toast.show(strings.getString(R.string.locationPermissionDenied))
        }
    }
    fun requestLocation() {
        if (LocationService.hasPermission(context)) useGps()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val onNext: () -> Unit = {
        when (step) {
            Step.Notifications -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                busy = true
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                askExactAlarms()
                next()
            }
            Step.Location -> if (locationTouched || Settings.location != null) {
                next()
            } else {
                quietOnNext = true
                requestLocation()
            }
            else -> next()
        }
    }

    Column(Modifier.fillMaxSize().background(p.background).statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(
                step,
                Modifier.widthIn(max = 680.dp),
                transitionSpec = {
                    val dur = if (still) 0 else AppMotion.PAGE
                    val rtl = Settings.language.rtl
                    val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                    (slideInHorizontally(tween(dur, easing = AppMotion.drawer)) { it * sign / 4 } + fadeIn(tween(dur))) togetherWith
                        (slideOutHorizontally(tween(dur, easing = AppMotion.drawer)) { -it * sign / 4 } + fadeOut(tween(dur / 2)))
                },
                label = "onboarding",
            ) { current ->
                when (current) {
                    Step.Welcome -> Page(
                        title = null,
                        welcome = true,
                        subtitle = tr(R.string.onboardingWelcomeDescription),
                        art = OnboardingArt.Welcome,
                        header = {
                            Image(
                                painterResource(R.drawable.logo), null,
                                Modifier.size(72.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        },
                    )
                    Step.Language -> Page(tr(R.string.selectLanguage), tr(R.string.onboardingLanguageDescription)) {
                        AppLanguage.entries.forEach { language ->
                            PickerTile(languageLabel(language), selected = Settings.language == language) { Settings.setLanguage(language) }
                            if (language != AppLanguage.entries.last()) Spacer(Modifier.height(8.dp))
                        }
                    }
                    Step.Notifications -> Page(tr(R.string.allowNotifications), tr(R.string.onboardingNotificationsDescription), OnboardingArt.Notifications) {
                        Spacer(Modifier.height(16.dp))
                        NotificationPreview()
                    }
                    Step.Location -> Page(tr(R.string.location), tr(R.string.onboardingLocationDescription), OnboardingArt.Location) {
                        val location = Settings.location
                        LocationOptionCard(
                            icon = Glyph.MyLocation,
                            title = tr(R.string.allowLocation),
                            description = tr(R.string.allowLocationDescription),
                            selected = location?.source == "automatic",
                            loading = busy && !LocationDraft.manualExpanded,
                            onClick = {
                                if (!busy) {
                                    locationTouched = true
                                    requestLocation()
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        val manual = location?.source == "manual"
                        LocationOptionCard(
                            icon = Glyph.LocationSlash,
                            title = tr(R.string.manualLocation),
                            description = tr(R.string.manualLocationDescription),
                            selected = manual,
                            onClick = {
                                locationTouched = true
                                LocationDraft.manualExpanded = !LocationDraft.manualExpanded
                            },
                        ) {
                            AnimatedVisibility(
                                LocationDraft.manualExpanded,
                                enter = expandVertically(tween(AppMotion.PAGE, easing = AppMotion.drawer)) + fadeIn(),
                                exit = shrinkVertically(tween(AppMotion.FAST)) + fadeOut(tween(AppMotion.FAST)),
                            ) {
                                Column {
                                    Spacer(Modifier.height(16.dp))
                                    Txt(tr(R.string.selectLocation), style(15.sp, if (manual) p.selectedTitle else p.title, FontWeight.SemiBold))
                                    Spacer(Modifier.height(12.dp))
                                    ManualLocationField(LocationDraft.country?.name, tr(R.string.selectCountry), manual) { nav.push(Route.CountryPicker) }
                                    Spacer(Modifier.height(8.dp))
                                    ManualLocationField(
                                        LocationDraft.city, tr(R.string.selectCity), manual,
                                        enabled = LocationDraft.country != null && !LocationDraft.updatingManual,
                                        loading = LocationDraft.updatingManual,
                                    ) { nav.push(Route.CityPicker) }
                                }
                            }
                        }
                    }
                    Step.Theme -> Page(tr(R.string.theme), tr(R.string.onboardingThemeDescription), OnboardingArt.Theme) {
                        val switchTheme = LocalThemeReveal.current
                        listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark).forEach { mode ->
                            PickerTile(themeLabel(mode), selected = Settings.themeMode == mode) { switchTheme(mode) }
                            if (mode != ThemeMode.Dark) Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        Actions(step, busy || LocationDraft.updatingManual, onBack = { go(Step.entries[step.ordinal - 1]) }, onNext = onNext)
    }
}

/**
 * Title, subtitle, the step's controls, and the artwork filling what is
 * left above the buttons. Scrolls when the controls need the room.
 */
@Composable
private fun Page(
    title: String?,
    subtitle: String,
    art: OnboardingArt? = null,
    welcome: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val p = palette
    val density = LocalDensity.current
    var contentHeight by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val available = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 12.dp, top = 28.dp, end = 12.dp, bottom = 28.dp)) {
            Column(Modifier.onSizeChanged { contentHeight = it.height }) {
                if (header != null) {
                    header()
                    Spacer(Modifier.height(24.dp))
                }
                if (welcome) {
                    val name = tr(R.string.onboardingAppName)
                    val rest = tr(R.string.onboardingWelcomeTitle)
                    BasicText(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = p.accent)) { append(name) }
                            append(" - $rest")
                        },
                        style = style(32.sp, p.title, FontWeight.Bold),
                    )
                } else if (title != null) {
                    Txt(title, style(28.sp, p.title, FontWeight.Bold))
                }
                Spacer(Modifier.height(8.dp))
                Txt(subtitle, style(16.sp, p.subtitle, lineHeight = 1.5f))
                Spacer(Modifier.height(24.dp))
                content()
            }
            if (art != null) {
                // What the text and the page padding leave; the art's own padding sits inside it.
                val used = with(density) { contentHeight.toDp() } + 56.dp
                val room: Dp = (available - used).coerceAtLeast(160.dp)
                Box(Modifier.fillMaxWidth().height(room).padding(top = 12.dp, bottom = (12 + ACTIONS_HEIGHT).dp), contentAlignment = Alignment.Center) {
                    OnboardingArtwork(art)
                }
            }
        }
    }
}

/** A sample prayer reminder, as it will appear. */
@Composable
private fun NotificationPreview() {
    val p = palette
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, p.accent.copy(alpha = .25f), RoundedCornerShape(12.dp)).padding(12.dp),
    ) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(tr(R.string.onboardingAppName), style(14.sp, p.title, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            Txt(tr(R.string.onboardingNotificationTitle), style(15.sp, p.title, FontWeight.SemiBold))
            Spacer(Modifier.height(3.dp))
            Txt(tr(R.string.onboardingNotificationBody), style(13.sp, p.subtitle, lineHeight = 1.35f))
        }
        Spacer(Modifier.width(8.dp))
        Txt(formatTime(LocalDateTime.now(), TimeFormat.TwelveHour, Settings.language), style(12.sp, p.subtitle))
    }
}

@Composable
private fun Actions(step: Step, loading: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    val p = palette
    val first = step == Step.Welcome
    val last = step == Step.Theme
    Column(Modifier.fillMaxWidth().background(p.background)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!first) {
                    Box(
                        Modifier.weight(1f).height(40.dp).pressable(if (loading) null else onBack)
                            .clip(RoundedCornerShape(12.dp)).border(1.dp, p.divider, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Txt(tr(R.string.back), style(14.sp, p.title, FontWeight.SemiBold)) }
                }
                Box(
                    Modifier.weight(1f).height(40.dp).pressable(if (loading) null else onNext)
                        .clip(RoundedCornerShape(12.dp)).background(p.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loading) {
                        Spinner(p.background, 18.dp)
                    } else {
                        Txt(
                            tr(if (first) R.string.getStarted else if (last) R.string.finish else R.string.next),
                            style(15.sp, p.background, FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
