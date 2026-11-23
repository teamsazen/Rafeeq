package com.satechdev.rafeeqapp.screens

import android.Manifest
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.LocationService
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.audio.QuranAudio
import com.satechdev.rafeeqapp.data.prayer.UserLocation
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The Kaaba, as flutter_qiblah's Utils. */
private const val KAABA_LAT = 21.422487
private const val KAABA_LNG = 39.826206

/** Degrees clockwise from true north to the Kaaba along the great circle. */
internal fun qiblaBearing(latitude: Double, longitude: Double): Double {
    val la = Math.toRadians(latitude)
    val dLo = Math.toRadians(KAABA_LNG - longitude)
    val kLa = Math.toRadians(KAABA_LAT)
    val b = Math.toDegrees(atan2(sin(dLo), cos(la) * kotlin.math.tan(kLa) - sin(la) * cos(dLo)))
    return normalize(b)
}

/** How much of each new reading the heading takes; lower is calmer. */
private const val HEADING_SMOOTHING = .22

internal fun normalize(angle: Double) = ((angle % 360) + 360) % 360

/** Which way to turn: [qiblah] is how far the phone points past the Qibla, clockwise (flutter_qiblah's qiblah). */
internal sealed interface Turn {
    data object Facing : Turn
    data class Left(val degrees: Int) : Turn
    data class Right(val degrees: Int) : Turn

    companion object {
        fun of(qiblah: Double): Turn {
            val a = normalize(qiblah)
            return when {
                a <= 5 || a >= 355 -> Facing
                a <= 180 -> Left(a.roundToInt())
                else -> Right((360 - a).roundToInt())
            }
        }
    }
}

/** The Qibla tab (lib/ui/qibla/qibla_screen.dart). */
@Composable
fun QiblaScreen(onBack: () -> Unit) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    val supported = remember {
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }
    val location = Settings.location
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.qibla), onBack = onBack)
        Column(Modifier.fillMaxSize().widthIn(max = 620.dp).align(Alignment.CenterHorizontally)) {
            Row(
                Modifier.fillMaxWidth().pressable({ nav.push(Route.Location) }).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(Glyph.Location, p.title, 24.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Txt(tr(R.string.location), style(16.sp, p.title, FontWeight.Bold))
                    Spacer(Modifier.height(3.dp))
                    Txt(locationText(location), style(14.sp, p.subtitle, lineHeight = 1.25f), maxLines = 2)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    !supported -> Unsupported()
                    location == null -> NeedsLocation()
                    else -> LiveCompass(location)
                }
            }
            Spacer(Modifier.height(TabBottomPadding + if (QuranAudio.state.isVisible) MiniPlayerHeight else 0.dp))
        }
    }
}

@Composable
private fun locationText(location: UserLocation?): String {
    if (location == null) return tr(R.string.locationNotSet)
    val place = listOfNotNull(location.city?.trim(), location.country?.trim()).filter { it.isNotEmpty() }.joinToString(", ")
    return place.ifEmpty { "%.2f, %.2f".format(location.latitude, location.longitude) }
}

@Composable
private fun Unsupported() {
    val p = palette
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(Glyph.Explore, p.subtitle.copy(alpha = .45f), 56.dp)
        Spacer(Modifier.height(16.dp))
        Txt(tr(R.string.compassNotSupported), style(16.sp, p.subtitle, align = TextAlign.Center))
    }
}

/** A faded dial and a button that asks for the location. */
@Composable
private fun NeedsLocation() {
    val p = palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val denied = tr(R.string.locationPermissionDenied)
    fun locate() {
        checking = true
        scope.launch {
            val ok = runCatching { LocationService.updateFromGps(context) }.getOrDefault(false)
            checking = false
            if (!ok) Toast.show(denied)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate() else Toast.show(denied)
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = min((maxWidth - 48.dp).coerceIn(180.dp, 280.dp), (maxHeight - 150.dp).coerceIn(160.dp, 280.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Dial(diameter, heading = 0f, marker = null, Modifier.alpha(.35f))
            Spacer(Modifier.height(20.dp))
            Txt(tr(R.string.locationRequiredDescription), style(14.sp, p.subtitle, lineHeight = 1.35f, align = TextAlign.Center), Modifier.padding(horizontal = 28.dp))
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(p.selectedCard)
                    .pressable(if (checking) null else ({
                        if (LocationService.hasPermission(context)) locate()
                        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }), scale = .95f)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (checking) Spinner(p.selectedTitle, 18.dp, 2.dp) else AppIcon(Glyph.MyLocationRounded, p.selectedTitle, 20.dp)
                Spacer(Modifier.width(8.dp))
                Txt(tr(if (checking) R.string.waitingForLocation else R.string.grantLocationPermission), style(14.sp, p.selectedTitle, FontWeight.SemiBold))
            }
        }
    }
}

/**
 * Where the top of the phone points, in degrees from true north, from the
 * rotation vector sensor; null until the first reading. Listens only while
 * on screen.
 */
@Composable
private fun rememberHeading(location: UserLocation): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    // The sensor gives magnetic north; the bearing to the Kaaba is from true north.
    val declination = remember(location) {
        GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }
    DisposableEffect(declination) {
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        // Averaged as a direction (sine and cosine), so 359° and 1° average to 0°, not 180°.
        var sx = Double.NaN
        var sy = Double.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                @Suppress("DEPRECATION")
                val (x, y) = when ((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, x, y, remapped)
                SensorManager.getOrientation(remapped, orientation)
                val raw = Math.toRadians(normalize(Math.toDegrees(orientation[0].toDouble()) + declination))
                if (sx.isNaN()) {
                    sx = cos(raw); sy = sin(raw)
                } else {
                    // A light low-pass filter takes out the sensor's shake but still follows a turn.
                    sx += HEADING_SMOOTHING * (cos(raw) - sx)
                    sy += HEADING_SMOOTHING * (sin(raw) - sy)
                }
                val next = normalize(Math.toDegrees(atan2(sy, sx))).toFloat()
                // Ignore tremors under a third of a degree so the dial rests when the phone does.
                val last = heading
                if (last == null || abs(((next - last) % 360 + 540) % 360 - 180) >= .35f) heading = next
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
    return heading
}

@Composable
private fun LiveCompass(location: UserLocation) {
    val p = palette
    val haptics = LocalHapticFeedback.current
    val heading = rememberHeading(location)
    if (heading == null) {
        Spinner(p.accent, 36.dp, 3.dp)
        return
    }
    val bearing = remember(location) { qiblaBearing(location.latitude, location.longitude) }
    val turn = Turn.of(heading - bearing)
    val facing = turn == Turn.Facing
    var wasFacing by remember { mutableStateOf(false) }
    LaunchedEffect(facing) {
        if (facing && !wasFacing) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasFacing = facing
    }
    val green = compassGreen()
    val text by animateColorAsState(if (facing) green else p.title, tween(220), label = "instruction")
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = min((maxWidth - 36.dp).coerceIn(170.dp, 380.dp), (maxHeight - 92.dp).coerceIn(160.dp, 380.dp))
        val narrow = maxWidth < 360.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Dial(diameter, heading, marker = (bearing - heading).toFloat(), facing = facing)
            Spacer(Modifier.height(12.dp))
            Txt(
                when (turn) {
                    Turn.Facing -> tr(R.string.qiblaFacing)
                    is Turn.Left -> tr(R.string.qiblaTurnLeft, turn.degrees.toString())
                    is Turn.Right -> tr(R.string.qiblaTurnRight, turn.degrees.toString())
                },
                style(if (narrow) 20.sp else 24.sp, text, FontWeight.Medium, align = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun compassGreen() = if (palette.dark) Color(0xFF43807A) else Color(0xFF07524E)

/**
 * The Kaaba marker's angle. While turning it simply follows the phone; when
 * the phone comes to face the Qibla it springs onto the top and stays there,
 * and on turning away it slides quickly back to the real bearing.
 */
@Composable
private fun kaabaAngle(target: Float, facing: Boolean): Float {
    val still = reducedMotion()
    val angle = remember { Animatable(target) }
    var settling by remember { mutableStateOf(false) }
    fun nearest(to: Float) = angle.value + (((to - angle.value) % 360 + 540) % 360 - 180)
    LaunchedEffect(facing) {
        if (still) return@LaunchedEffect
        settling = true
        if (facing) angle.animateTo(nearest(0f), spring(dampingRatio = .45f, stiffness = 380f))
        else angle.animateTo(nearest(target), tween(160))
        settling = false
    }
    LaunchedEffect(target, facing) {
        if (!settling && !facing || still) angle.snapTo(nearest(if (facing && !still) 0f else target))
    }
    return angle.value
}

/**
 * The dial: ticks, the four directions turning with the phone, the fixed
 * pointer, and the Kaaba at [marker] degrees clockwise from the top.
 */
@Composable
private fun Dial(size: Dp, heading: Float, marker: Float?, modifier: Modifier = Modifier, facing: Boolean = false) {
    val p = palette
    val green = compassGreen()
    val tick = if (p.dark) p.title else Color(0xFFF1DCA7)
    // The directions follow the (already filtered) heading directly, with no lag after the phone stops.
    val north = -heading
    val kaaba = marker?.let { kaabaAngle(it, facing) }
    // The Kaaba swells once when it comes to the top.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(facing) {
        if (facing) {
            pulse.snapTo(1.18f)
            pulse.animateTo(1f, spring(dampingRatio = .35f, stiffness = 300f))
        }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = this.size.minDimension / 2 - 17.dp.toPx()
            for (i in 0 until 72) {
                val a = i * 5 * PI / 180
                val length = (if (i % 6 == 0) 15 else 10).dp.toPx()
                val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                drawLine(tick, center + dir * (outer - length), center + dir * outer, 2.2.dp.toPx(), StrokeCap.Round)
            }
        }
        val radius = size * .315f
        for ((label, bearing) in listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)) {
            val a = (bearing + north) * PI / 180
            Box(
                Modifier.offset(radius * sin(a).toFloat(), -radius * cos(a).toFloat()).size(size * .09f),
                contentAlignment = Alignment.Center,
            ) {
                Txt(label, style((size.value * .052f).sp, p.title, FontWeight.Medium))
            }
        }
        Pointer(size * .23f, green, p.background)
        if (kaaba != null) {
            val a = kaaba * PI / 180
            val r = size / 2 - 17.dp
            Box(
                Modifier.offset(r * sin(a).toFloat(), -r * cos(a).toFloat()).size(size * .17f)
                    .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .clip(CircleShape).background(green),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Glyph.KaabaSolid, tick, size * .075f)
            }
        }
    }
}

/** The pointer at the middle: a ring with a point on top, fixed to the phone. */
@Composable
private fun Pointer(size: Dp, color: Color, hole: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * .31f
        val ring = Offset(w / 2, h / 2 + h * .12f)
        drawPath(
            Path().apply {
                moveTo(w / 2, h * .02f)
                lineTo(w / 2 + r * .62f, ring.y)
                lineTo(w / 2 - r * .62f, ring.y)
                close()
            },
            color,
        )
        drawCircle(color, r, ring)
        drawCircle(hole, r * .61f, ring)
    }
}
