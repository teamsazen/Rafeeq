package com.satechdev.rafeeqapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Where the sun or moon is in the sky at [time] (lib/ui/widgets/sky_artwork.dart). */
data class SkyPhase(val isDay: Boolean, val progress: Float, val time: LocalDateTime) {
    /** Height of the sun from -1 (midnight) through 0 (horizon) to 1 (noon). */
    val elevation get() = (if (isDay) 1 else -1) * sin(PI * progress).toFloat()

    /** The night the stars belong to, so they stay put from dusk until dawn. */
    val nightSeed: Int
        get() {
            val night = if (!isDay && time.hour < 12) time.minusDays(1) else time
            return night.year * 10000 + night.monthValue * 100 + night.dayOfMonth
        }

    companion object {
        /**
         * The phase from the day's sunrise and sunset. Missing times fall back
         * to 6 AM and 6 PM so the sky still reads sensibly without a location.
         */
        fun at(
            now: LocalDateTime,
            sunrise: LocalDateTime? = null,
            sunset: LocalDateTime? = null,
            previousSunset: LocalDateTime? = null,
            nextSunrise: LocalDateTime? = null,
        ): SkyPhase {
            val midnight = now.toLocalDate().atStartOfDay()
            val dawn = sunrise ?: midnight.plusHours(6)
            val dusk = sunset ?: midnight.plusHours(18)
            if (!now.isBefore(dawn) && now.isBefore(dusk)) return SkyPhase(true, fraction(now, dawn, dusk), now)
            val (start, end) = if (now.isBefore(dawn)) {
                (previousSunset ?: dusk.minusDays(1)) to dawn
            } else {
                dusk to (nextSunrise ?: dawn.plusDays(1))
            }
            return SkyPhase(false, fraction(now, start, end), now)
        }

        private fun fraction(now: LocalDateTime, start: LocalDateTime, end: LocalDateTime): Float {
            val total = Duration.between(start, end).seconds
            if (total <= 0) return 0f
            return (Duration.between(start, now).seconds.toFloat() / total).coerceIn(0f, 1f)
        }
    }
}

/**
 * A live sky: the sun travels its arc through the day, the sky warms at
 * sunrise and sunset, and at night the moon rises in its real phase among the stars.
 */
@Composable
fun SkyArtwork(phase: SkyPhase, modifier: Modifier = Modifier) {
    val sky = SkyPainter(ArtColors(palette), phase)
    // Flutter paints in logical pixels; so does this, scaled up by the density.
    Canvas(modifier) {
        sky.area = Size(size.width / density, size.height / density)
        scale(density, density, pivot = Offset.Zero) { with(sky) { paint() } }
    }
}

private class SkyPainter(colors: ArtColors, val phase: SkyPhase) : Art(colors) {
    /** The card's size in logical pixels. */
    var area = Size.Zero

    companion object {
        val lightDay = listOf(Color(0xFFC8EDF7), Color(0xFFE6F6FA))
        val lightTwilight = listOf(Color(0xFFF6DCC6), Color(0xFFFCEBD3))
        val lightNight = listOf(Color(0xFFD6DAEE), Color(0xFFE9E7F3))
        val darkDay = listOf(Color(0xFF193438), Color(0xFF214448))
        val darkTwilight = listOf(Color(0xFF3B2B36), Color(0xFF4A3628))
        val darkNight = listOf(Color(0xFF0E1829), Color(0xFF182235))
        val deepOrange = Color(0xFFFF5722)

        /** A known new moon (6 Jan 2000, 18:14 UTC) and the synodic month length. */
        val newMoon: Instant = Instant.parse("2000-01-06T18:14:00Z")
        const val SYNODIC_DAYS = 29.530588

        /** The sun and moon peak below the prayer labels at the top of the card. */
        const val PEAK = 78f
        const val HORIZON_INSET = 26f

        fun smooth(x: Float): Float {
            val c = x.coerceIn(0f, 1f)
            return c * c * (3 - 2 * c)
        }
    }

    val dayness get() = if (phase.elevation > 0) smooth(phase.elevation / .35f) else 0f
    val nightness get() = if (phase.elevation < 0) smooth(-phase.elevation / .3f) else 0f

    val skyColors: List<Color>
        get() {
            val twilight = if (dark) darkTwilight else lightTwilight
            val target = if (phase.elevation >= 0) (if (dark) darkDay else lightDay) else (if (dark) darkNight else lightNight)
            val t = if (phase.elevation >= 0) dayness else nightness
            return List(2) { lerp(twilight[it], target[it], t) }
        }

    fun DrawScope.arcPoint(t: Float): Offset {
        val horizon = area.height - HORIZON_INSET
        return Offset(
            area.width * .5f + (area.width - 26 - area.width * .5f) * t,
            horizon - sin(PI.toFloat() * t) * (horizon - PEAK),
        )
    }

    fun DrawScope.paint() {
        val sky = skyColors
        drawRect(Brush.verticalGradient(sky, 0f, area.height), Offset.Zero, area)
        stars()
        clouds()
        track()
        if (phase.isDay) sun() else moon()
        ground(sky.last())
    }

    fun DrawScope.stars() {
        if (nightness == 0f) return
        val random = Random(phase.nightSeed)
        val color = if (dark) goldLight else palette.subtitle
        repeat(28) {
            val position = Offset(random.nextFloat() * area.width, 8 + random.nextFloat() * (area.height - 60))
            val radius = .6f + random.nextFloat() * 1.1f
            val alpha = (.3f + random.nextFloat() * .6f) * nightness
            if (radius > 1.45f) sparkle(position, radius * 2.6f, color.copy(alpha = alpha))
            else drawCircle(color.copy(alpha = alpha), radius, position)
        }
    }

    fun DrawScope.clouds() {
        val time = phase.time
        val random = Random(time.year * 400 + time.monthValue * 32 + time.dayOfMonth)
        val minutes = time.hour * 60 + time.minute
        val base = if (dark) lerp(skyColors.last(), Color.White, .14f) else Color.White
        val alpha = .35f + .5f * dayness
        repeat(3) {
            val start = random.nextFloat() * area.width
            val speed = .25f + random.nextFloat() * .25f
            val span = area.width + 80
            val x = (start + minutes * speed) % span - 40
            val y = 66 + random.nextFloat() * 46
            val scale = .7f + random.nextFloat() * .4f
            cloud(Offset(x, y), scale, base.copy(alpha = alpha))
        }
    }

    /** The dotted path across the sky, solid where the sun or moon has been. */
    fun DrawScope.track() {
        val dots = palette.title.copy(alpha = .16f)
        val steps = 36
        for (i in 0..steps) drawCircle(dots, 1.1f, arcPoint(i / steps.toFloat()))
        val travelled = Path()
        val end = ceil(phase.progress * steps).toInt()
        for (i in 0..end) {
            val p = arcPoint(min(i / steps.toFloat(), phase.progress))
            if (i == 0) travelled.moveTo(p.x, p.y) else travelled.lineTo(p.x, p.y)
        }
        strokePath(travelled, (if (phase.isDay) gold else palette.subtitle).copy(alpha = .5f), 1.6f)
    }

    fun DrawScope.sun() {
        val center = arcPoint(phase.progress)
        // The sun warms towards orange as it nears the horizon.
        val color = lerp(gold, deepOrange, .4f * (1 - dayness))
        glow(center, 58f, color)
        drawCircle(color, 13f, center)
        drawCircle(lerp(goldLight, Color.White, .3f).copy(alpha = .55f), 6f, center - Offset(3f, 3f))
    }

    fun DrawScope.moon() {
        val center = arcPoint(phase.progress)
        val radius = 11f
        val light = if (dark) lerp(goldLight, Color.White, .5f) else gold
        glow(center, 40f, light)
        drawCircle(palette.title.copy(alpha = if (dark) .12f else .08f), radius, center)

        val minutes = Duration.between(newMoon, phase.time.atZone(ZoneId.systemDefault()).toInstant()).toMinutes()
        val days = (minutes / 1440.0) % SYNODIC_DAYS
        val cycle = (days / SYNODIC_DAYS).toFloat()
        val waxing = cycle < .5f
        // 1 at new moon, -1 at full moon: how far the terminator bulges.
        val bulge = cos(2 * PI * cycle).toFloat()
        val circle = Rect(center, radius)
        val ellipse = Rect(center.x - max(kotlin.math.abs(bulge) * radius, .01f), center.y - radius, center.x + max(kotlin.math.abs(bulge) * radius, .01f), center.y + radius)
        // Flutter draws the lit limb top to bottom, then the terminator back
        // up; angles here run clockwise from 3 o'clock, so the bottom is 90°.
        val lit = Path().apply {
            moveTo(center.x, center.y - radius)
            arcTo(circle, -90f, if (waxing) 180f else -180f, false)
            val clockwise = if (waxing) bulge < 0 else bulge > 0
            arcTo(ellipse, 90f, if (clockwise) 180f else -180f, false)
            close()
        }
        drawPath(lit, light)
    }

    fun DrawScope.ground(horizonColor: Color) {
        val w = area.width
        val h = area.height
        hills(listOf(Offset(0f, h - 18), Offset(w * .25f, h - 30), Offset(w * .5f, h - 20), Offset(w * .75f, h - 34), Offset(w, h - 24)),
            lerp(horizonColor, teal, if (dark) .3f else .22f), bottom = h)

        val silhouette = lerp(horizonColor, tealDark, if (dark) .75f else .55f)
        val mosqueBase = Offset(w - 92, h - 24)
        val scale = .85f
        mosque(mosqueBase, scale, silhouette)
        val minaretX = w - 56
        drawRect(silhouette, Offset(minaretX - 3, h - 76), Size(6f, 52f))
        drawRect(silhouette, Offset(minaretX - 5, h - 60), Size(10f, 3f))
        drawPath(Path().apply {
            moveTo(minaretX - 4, h - 76)
            quadraticTo(minaretX - 4, h - 84, minaretX, h - 88)
            quadraticTo(minaretX + 4, h - 84, minaretX + 4, h - 76)
            close()
        }, silhouette)
        crescent(Offset(minaretX, h - 92), 2.4f, gold)

        // Windows light up as evening falls.
        val glowAlpha = 1 - dayness
        if (glowAlpha > 0) {
            for (dx in listOf(-8f, 0f, 8f)) {
                val c = mosqueBase + Offset(dx * scale, -5 * scale)
                drawRoundRect(goldLight.copy(alpha = .9f * glowAlpha), Offset(c.x - 1.3f, c.y - 2.5f), Size(2.6f, 5f), CornerRadius(1.3f))
            }
        }
        hills(listOf(Offset(0f, h - 8), Offset(w * .3f, h - 16), Offset(w * .62f, h - 6), Offset(w, h - 14)),
            lerp(horizonColor, tealDark, if (dark) .5f else .35f), bottom = h)
    }
}
