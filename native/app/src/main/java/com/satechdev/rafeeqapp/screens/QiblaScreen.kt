package com.satechdev.rafeeqapp.screens

import com.satechdev.rafeeqapp.ui.localNumber
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.AnimatedContent
import android.view.View
import android.view.HapticFeedbackConstants
import android.os.VibratorManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.SystemClock
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
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
            Dial(diameter, heading = 0f, bearing = null, modifier = Modifier.alpha(.35f))
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

/**
 * Haptics for the dial: a light tick like a detent as the phone turns, and a
 * firm click when it comes to face the Qibla.
 */
private class DialHaptics(private val view: View) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (view.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private var lastTick = 0L

    fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTick < 45) return
        lastTick = now
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun lock() {
        val v = vibrator
        if (v == null || !v.hasVibrator()) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return
        }
        val effect = when {
            Build.VERSION.SDK_INT >= 29 -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            Build.VERSION.SDK_INT >= 26 -> VibrationEffect.createOneShot(45, 255)
            else -> null
        }
        when {
            effect != null && Build.VERSION.SDK_INT >= 33 -> v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            effect != null -> v.vibrate(effect)
            else -> @Suppress("DEPRECATION") v.vibrate(45)
        }
    }
}

/** Degrees between the top of the phone and the Qibla, -180 to 180; negative means turn left. */
private fun gap(heading: Float, bearing: Float) = ((bearing - heading) % 360 + 540) % 360 - 180

@Composable
private fun LiveCompass(location: UserLocation) {
    val p = palette
    val view = LocalView.current
    val haptics = remember(view) { DialHaptics(view) }
    val heading = rememberHeading(location)
    if (heading == null) {
        Spinner(p.accent, 36.dp, 3.dp)
        return
    }
    val bearing = remember(location) { qiblaBearing(location.latitude, location.longitude).toFloat() }
    val turn = Turn.of((heading - bearing).toDouble())
    val facing = turn == Turn.Facing

    // A detent every 3 degrees while turning; a firm click on reaching the Qibla.
    var detent by remember { mutableStateOf((heading / 3).toInt()) }
    LaunchedEffect((heading / 3).toInt()) {
        val now = (heading / 3).toInt()
        if (now != detent && !facing) haptics.tick()
        detent = now
    }
    var wasFacing by remember { mutableStateOf(facing) }
    LaunchedEffect(facing) {
        if (facing && !wasFacing) haptics.lock()
        wasFacing = facing
    }

    val green = compassGreen()
    val text by animateColorAsState(if (facing) green else p.title, tween(220), label = "instruction")
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val diameter = min((maxWidth - 40.dp).coerceIn(200.dp, 360.dp), (maxHeight - 120.dp).coerceIn(180.dp, 360.dp))
        val narrow = maxWidth < 360.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Dial(diameter, heading, bearing, facing = facing)
            Spacer(Modifier.height(20.dp))
            Txt(
                when (turn) {
                    Turn.Facing -> tr(R.string.qiblaFacing)
                    is Turn.Left -> tr(R.string.qiblaTurnLeft, localNumber(turn.degrees))
                    is Turn.Right -> tr(R.string.qiblaTurnRight, localNumber(turn.degrees))
                },
                style(if (narrow) 20.sp else 24.sp, text, FontWeight.Medium, align = TextAlign.Center),
            )
            Spacer(Modifier.height(4.dp))
            Txt(tr(R.string.qiblaBearingFromNorth, localNumber(bearing.roundToInt())), style(14.sp, p.subtitle, align = TextAlign.Center))
        }
    }
}

@Composable
private fun compassGreen() = if (palette.dark) Color(0xFF43807A) else Color(0xFF07524E)

/**
 * A compass rose that turns with the phone under a fixed mark at the top.
 * The Kaaba sits on the rim at its bearing, and an arc from the mark to it
 * shows how far there is to turn; it closes as the phone comes round. On
 * the Qibla the ring turns green, a soft glow breathes behind it and the
 * Kaaba takes the middle.
 */
@Composable
private fun Dial(size: Dp, heading: Float, bearing: Float?, modifier: Modifier = Modifier, facing: Boolean = false) {
    val p = palette
    val still = reducedMotion()
    val green = compassGreen()
    val tick = if (p.dark) p.title.copy(alpha = .55f) else Color(0xFFD9BF7E)
    val ringColor by animateColorAsState(if (facing) green else p.card, tween(260), label = "ring")
    val lock by animateFloatAsState(if (facing) 1f else 0f, if (still) tween(0) else spring(dampingRatio = .5f, stiffness = 320f), label = "lock")
    val breathe = rememberInfiniteTransition(label = "glow")
    val glow by breathe.animateFloat(.55f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "breathe")
    val gapDeg = bearing?.let { gap(heading, it) } ?: 0f

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // The glow on the Qibla.
        Canvas(Modifier.fillMaxSize()) {
            if (lock > 0f) {
                drawCircle(
                    Brush.radialGradient(listOf(green.copy(alpha = .28f * lock * glow), Color.Transparent), center, this.size.minDimension / 2),
                    this.size.minDimension / 2,
                )
            }
        }
        // The ring and the arc still to turn, fixed to the phone.
        Canvas(Modifier.fillMaxSize().padding(size * .06f)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val box = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(ringColor, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
            if (bearing != null && !facing) {
                drawArc(green.copy(alpha = .55f), -90f, gapDeg, false, Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        // The rose: ticks and directions turn with the phone.
        Box(Modifier.fillMaxSize().padding(size * .06f + 14.dp).graphicsLayer { rotationZ = -heading }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val outer = this.size.minDimension / 2
                for (i in 0 until 120) {
                    val a = i * 3 * PI / 180
                    val major = i % 10 == 0
                    val length = (if (major) 12 else 6).dp.toPx()
                    val dir = Offset(sin(a).toFloat(), -cos(a).toFloat())
                    drawLine(if (i == 0) green else tick, center + dir * (outer - length), center + dir * outer, (if (major) 2.4f else 1.4f).dp.toPx(), StrokeCap.Round)
                }
            }
            val r = (size * (1 - .12f) - 28.dp) / 2 - 26.dp
            for ((label, b) in listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)) {
                val a = b * PI / 180
                Box(
                    Modifier.offset(r * sin(a).toFloat(), -r * cos(a).toFloat()).size(32.dp).graphicsLayer { rotationZ = heading },
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(label, style((size.value * .055f).sp, if (b == 0f) green else p.title, if (b == 0f) FontWeight.Bold else FontWeight.Medium))
                }
            }
        }
        // The fixed mark at the top of the ring.
        Canvas(Modifier.fillMaxSize()) {
            val w = 9.dp.toPx()
            val top = size.toPx() * .06f - 6.dp.toPx()
            drawPath(
                Path().apply {
                    moveTo(center.x - w, top)
                    lineTo(center.x + w, top)
                    lineTo(center.x, top + w * 1.4f)
                    close()
                },
                if (facing) green else p.title,
            )
        }
        // The Kaaba on the rim, upright, at its bearing on the turning rose.
        if (bearing != null) {
            val a = (bearing - heading) * PI / 180
            val rr = size / 2 - size * .06f - 5.dp
            val badge = size * .15f
            Box(
                Modifier.offset(rr * sin(a).toFloat(), -rr * cos(a).toFloat()).size(badge)
                    .graphicsLayer { val s = 1f + .15f * lock * (1 - lock) * 4; scaleX = s; scaleY = s }
                    .clip(CircleShape).background(green),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Glyph.KaabaSolid, Color(0xFFF1DCA7), badge * .5f)
            }
        }
        // The middle: degrees left to turn, or the Kaaba once facing it.
        Box(
            Modifier.size(size * .36f).clip(CircleShape).background(if (facing) green else p.card),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                facing,
                transitionSpec = {
                    (scaleIn(if (still) tween(0) else spring(dampingRatio = .55f, stiffness = 420f), .6f) + fadeIn(tween(160))) togetherWith
                        (scaleOut(tween(120), .6f) + fadeOut(tween(120)))
                },
                label = "middle",
            ) { on ->
                if (on) {
                    AppIcon(Glyph.KaabaSolid, Color(0xFFF1DCA7), size * .17f)
                } else if (bearing != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt("${localNumber(abs(gapDeg).roundToInt())}°", style((size.value * .1f).sp, p.title, FontWeight.Bold))
                        Txt(if (gapDeg < 0) "←" else "→", style((size.value * .06f).sp, p.subtitle))
                    }
                } else {
                    AppIcon(Glyph.KaabaSolid, p.subtitle, size * .14f)
                }
            }
        }
    }
}
