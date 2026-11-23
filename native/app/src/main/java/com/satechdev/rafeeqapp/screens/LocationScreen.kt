package com.satechdev.rafeeqapp.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.LocalStrings
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.Country
import com.satechdev.rafeeqapp.data.Analytics
import com.satechdev.rafeeqapp.data.LocationService
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.launch

/**
 * The manual choice while the location screen and its pickers are open:
 * the pickers fill it in, and choosing a city saves it.
 */
internal object LocationDraft {
    var country by mutableStateOf<Country?>(null)
    var city by mutableStateOf<String?>(null)
    var manualExpanded by mutableStateOf(false)
    var updatingManual by mutableStateOf(false)

    fun reset() {
        val saved = Settings.location
        country = saved?.country?.let { Country(it, saved.countryCode.orEmpty()) }
        city = saved?.city
        manualExpanded = saved?.source == "manual"
        updatingManual = false
    }
}

/** Automatic or manual location, as lib/ui/app_home/home/location_screen.dart. */
@Composable
fun LocationScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var updatingAutomatic by remember { mutableStateOf(false) }
    remember { LocationDraft.reset() }

    fun useGps() {
        updatingAutomatic = true
        scope.launch {
            val updated = runCatching { LocationService.updateFromGps(context) }.getOrDefault(false)
            updatingAutomatic = false
            if (updated) Analytics.event("settings_location_to_automatic")
            if (updated) LocationDraft.reset()
            Toast.show(strings.getString(if (updated) R.string.locationUpdated else R.string.locationPermissionDenied))
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            useGps()
        } else {
            // Denied for good: the system no longer asks, so open the app's settings, as Flutter does.
            val activity = context as? Activity
            if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            }
            Toast.show(strings.getString(R.string.locationPermissionDenied))
        }
    }

    val location = Settings.location
    PushedScreen(tr(R.string.location)) {
        item(key = "automatic") {
            LocationOptionCard(
                icon = Glyph.MyLocation,
                title = tr(R.string.allowLocation),
                description = tr(R.string.allowLocationDescription),
                selected = location != null && location.source == "automatic",
                loading = updatingAutomatic,
                onClick = {
                    if (!updatingAutomatic) {
                        if (LocationService.hasPermission(context)) {
                            useGps()
                        } else {
                            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                },
            )
        }
        item(key = "manual") {
            val selected = location?.source == "manual"
            LocationOptionCard(
                icon = Glyph.LocationSlash,
                title = tr(R.string.manualLocation),
                description = tr(R.string.manualLocationDescription),
                selected = selected,
                onClick = { LocationDraft.manualExpanded = !LocationDraft.manualExpanded },
            ) {
                AnimatedVisibility(
                    LocationDraft.manualExpanded,
                    enter = expandVertically(tween(AppMotion.PAGE, easing = AppMotion.drawer)) + fadeIn(),
                    exit = shrinkVertically(tween(AppMotion.FAST)) + fadeOut(tween(AppMotion.FAST)),
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Txt(tr(R.string.selectLocation), style(15.sp, if (selected) palette.selectedTitle else palette.title, FontWeight.SemiBold))
                        Spacer(Modifier.height(12.dp))
                        ManualLocationField(LocationDraft.country?.name, tr(R.string.selectCountry), selected) {
                            nav.push(Route.CountryPicker)
                        }
                        Spacer(Modifier.height(8.dp))
                        ManualLocationField(
                            LocationDraft.city,
                            tr(R.string.selectCity),
                            selected,
                            enabled = LocationDraft.country != null && !LocationDraft.updatingManual,
                            loading = LocationDraft.updatingManual,
                        ) { nav.push(Route.CityPicker) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocationOptionCard(
    icon: Glyph,
    title: String,
    description: String,
    selected: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
    expanded: @Composable () -> Unit = {},
) {
    val p = palette
    val fade = tween<Color>(AppMotion.FAST, easing = AppMotion.easeOut)
    val card by animateColorAsState(if (selected) p.selectedCard else p.card, fade, label = "card")
    val titleColor by animateColorAsState(if (selected) p.selectedTitle else p.title, fade, label = "title")
    val bodyColor by animateColorAsState(if (selected) p.selectedSubtitle else p.subtitle, fade, label = "body")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card)) {
        Row(
            Modifier.fillMaxWidth().pressable(onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(if (selected) p.selectedTitle else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon, if (selected) p.selectedCard else p.accent, 23.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt(title, style(15.sp, titleColor, FontWeight.SemiBold, lineHeight = 1.2f))
                Spacer(Modifier.height(3.dp))
                Txt(description, style(14.sp, bodyColor, lineHeight = 1.45f))
            }
            Spacer(Modifier.width(12.dp))
            when {
                loading -> Spinner(if (selected) p.selectedTitle else p.accent)
                selected -> Box(Modifier.size(20.dp).background(p.selectedTitle, CircleShape), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.Check, p.selectedCard, 14.dp)
                }
            }
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth()) { expanded() }
    }
}

@Composable
internal fun ManualLocationField(
    value: String?,
    placeholder: String,
    onSelectedSurface: Boolean,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val p = palette
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else .5f)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (onSelectedSurface) p.selectedSubtitle else p.accent, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value == null) {
                Txt(placeholder, style(14.sp, if (onSelectedSurface) p.selectedSubtitle else p.subtitle), maxLines = 1)
            } else {
                Txt(value, style(14.sp, if (onSelectedSurface) p.selectedTitle else p.title, family = AppFonts.sans), maxLines = 1)
            }
        }
        if (loading) {
            Spinner(if (onSelectedSurface) p.selectedTitle else p.accent, 18.dp)
        } else {
            AppIcon(Glyph.ArrowDown, if (onSelectedSurface) p.selectedSubtitle else p.subtitle, 22.dp)
        }
    }
}

// ---------- Pickers ----------

private sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>
    data object Failed : Loaded<Nothing>
    data class Ready<T>(val value: T) : Loaded<T>
}

@Composable
fun CountryPickerScreen() {
    val nav = LocalNavigator.current
    var attempt by remember { mutableStateOf(0) }
    var countries by remember { mutableStateOf<Loaded<List<Country>>>(Loaded.Loading) }
    LaunchedEffect(attempt) {
        countries = Loaded.Loading
        countries = runCatching { LocationService.countries() }.fold({ Loaded.Ready(it) }, { Loaded.Failed })
    }
    val draft = LocationDraft.country
    LocationPicker(tr(R.string.selectCountry), countries, { attempt++ }, { it.name }, { country ->
        if (draft?.code?.isNotEmpty() == true) country.code == draft.code else country.name.equals(draft?.name, ignoreCase = true)
    }) { country ->
        if (country.code != LocationDraft.country?.code) LocationDraft.city = null
        LocationDraft.country = country
        LocationDraft.manualExpanded = true
        nav.pop()
    }
}

@Composable
fun CityPickerScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableStateOf(0) }
    var country by remember { mutableStateOf(LocationDraft.country) }
    var cities by remember { mutableStateOf<Loaded<List<String>>>(Loaded.Loading) }
    LaunchedEffect(attempt) {
        cities = Loaded.Loading
        cities = runCatching {
            // An old Flutter location may have the country's name but no code: find it by name.
            val chosen = country ?: error("No country")
            val resolved = if (chosen.code.isNotEmpty()) chosen else LocationService.countries()
                .firstOrNull { it.name.equals(chosen.name.trim(), ignoreCase = true) } ?: error("Unknown country")
            country = resolved
            LocationDraft.country = resolved
            val list = LocationService.cities(resolved.code)
            // Keep the saved city in the list even when the dataset spells it differently.
            val saved = LocationDraft.city
            if (saved != null && list.none { it.equals(saved, ignoreCase = true) }) listOf(saved) + list else list
        }.fold({ Loaded.Ready(it) }, { Loaded.Failed })
    }
    LocationPicker(tr(R.string.selectCity), cities, { attempt++ }, { it }, { it.equals(LocationDraft.city, ignoreCase = true) }) { city ->
        val chosen = country ?: return@LocationPicker
        LocationDraft.city = city
        LocationDraft.updatingManual = true
        nav.pop()
        scope.launch {
            val updated = runCatching { LocationService.setManual(context.applicationContext, chosen, city) }.getOrDefault(false)
            if (updated) Analytics.event("settings_location_to_manual")
            LocationDraft.updatingManual = false
            Toast.show(strings.getString(if (updated) R.string.locationUpdated else R.string.locationNotFound))
        }
    }
}

/** A searchable list, as lib/ui/app_home/home/location_picker_screen.dart. */
@Composable
private fun <T> LocationPicker(
    title: String,
    state: Loaded<List<T>>,
    retry: () -> Unit,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onPick: (T) -> Unit,
) {
    val nav = LocalNavigator.current
    val p = palette
    val search = rememberTextFieldState()
    val query = search.text.toString()
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(title, onBack = nav::pop)
        SearchField(search, tr(R.string.search))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                Loaded.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
                Loaded.Failed -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    AppIcon(Glyph.CloudOff, p.subtitle, 36.dp)
                    Spacer(Modifier.height(12.dp))
                    Txt(tr(R.string.locationDataLoadError), style(14.sp, p.subtitle, family = AppFonts.sans, align = TextAlign.Center))
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.pressable(retry).background(p.accent, RoundedCornerShape(50)).padding(horizontal = 24.dp, vertical = 10.dp),
                    ) { Txt(tr(R.string.retry), style(14.sp, p.checkForeground, FontWeight.Medium)) }
                }
                is Loaded.Ready -> {
                    val q = query.trim().lowercase()
                    val filtered = state.value.filter { q.isEmpty() || label(it).lowercase().contains(q) }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Txt(tr(R.string.noResultsFound), style(14.sp, p.subtitle, family = AppFonts.sans))
                        }
                    } else {
                        LazyColumn(
                            Modifier.widthIn(max = 760.dp).fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = ScreenBottomPadding),
                        ) {
                            items(filtered, key = { label(it) }) { item ->
                                PickerRow(label(item), isSelected(item)) { onPick(item) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val p = palette
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(title, style(16.sp, p.title, FontWeight.SemiBold, family = AppFonts.sans), Modifier.weight(1f), maxLines = 1)
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(24.dp).background(p.accent, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CheckMark, p.checkForeground, 15.dp)
            }
        }
    }
}

/** The picker search box: a rounded outline that takes the accent while typing (Flutter's search TextField). */
@Composable
internal fun SearchField(state: androidx.compose.foundation.text.input.TextFieldState, hint: String) {
    val p = palette
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        val border by animateColorAsState(if (focused) p.accent else p.divider, tween(AppMotion.FAST), label = "border")
        BasicTextField(
            state,
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .height(52.dp)
                .border(if (focused) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp))
                .onFocusChanged { focused = it.isFocused },
            textStyle = style(15.sp, p.title),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorator = { field ->
                Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (state.text.isEmpty()) Txt(hint, style(15.sp, p.subtitle), maxLines = 1)
                        field()
                    }
                    AppIcon(Glyph.Search, p.subtitle, 24.dp)
                }
            },
        )
    }
}
