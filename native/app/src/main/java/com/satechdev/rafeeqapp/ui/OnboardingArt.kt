package com.satechdev.rafeeqapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** The onboarding illustrations (lib/ui/onboarding/onboarding_artwork.dart). */
enum class OnboardingArt { Welcome, Notifications, Location, Theme }

/** A 300 × 200 drawing scaled to fit, at most 300 tall. */
@Composable
fun OnboardingArtwork(art: OnboardingArt, modifier: Modifier = Modifier) {
    val painter = OnboardingPainter(ArtColors(palette))
    Canvas(modifier.heightIn(max = 300.dp).aspectRatio(3f / 2f)) {
        val s = min(size.width / 300f, size.height / 200f)
        translate((size.width - 300f * s) / 2, (size.height - 200f * s) / 2) {
            scale(s, s, pivot = Offset.Zero) {
                with(painter) {
                    when (art) {
                        OnboardingArt.Welcome -> welcome()
                        OnboardingArt.Notifications -> notifications()
                        OnboardingArt.Location -> location()
                        OnboardingArt.Theme -> theme()
                    }
                }
            }
        }
    }
}

private class OnboardingPainter(colors: ArtColors) : Art(colors) {
    private fun DrawScope.strokeLine(color: Color, width: Float, a: Offset, b: Offset) = stroke(color, width, a, b)
    @Suppress("UNUSED_PARAMETER")
    private fun outline(color: Color, width: Float) = Stroke(width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)

    // ---------- Welcome: an open Quran on a rehal under three arches ----------

    private fun arch(left: Float, right: Float, spring: Float, top: Float, base: Float): Path {
        val cx = (left + right) / 2
        val w = right - left
        return Path().apply {
            moveTo(left, base)
            lineTo(left, spring)
            cubicTo(left, spring - (spring - top) * .6f, cx - w * .2f, top + 10, cx, top)
            cubicTo(cx + w * .2f, top + 10, right, spring - (spring - top) * .6f, right, spring)
            lineTo(right, base)
            close()
        }
    }

    private fun DrawScope.gate(left: Float, right: Float, spring: Float, top: Float, base: Float) {
        val outer = arch(left, right, spring, top, base)
        shadow(outer)
        drawPath(outer, cream)
        drawPath(outer, creamDark, style = outline(creamDark, 1f))
        drawPath(arch(left + 7, right - 7, spring + 3, top + 9, base), teal)
        drawPath(arch(left + 12, right - 12, spring + 6, top + 16, base), palette.background)
        drawPath(arch(left + 7, right - 7, spring + 3, top + 9, base), gold, style = outline(gold, 1f))
        for (x in listOf(left, right - 9)) {
            drawRect(gold, Offset(x - 1, spring), Size(11f, 3f))
            drawRect(gold, Offset(x - 1, base - 10), Size(11f, 3f))
            drawRect(tealDark, Offset(x - 1, base - 7), Size(11f, 4f))
        }
        val cx = (left + right) / 2
        drawRect(gold, Offset(cx - 1, top - 6), Size(2f, 6f))
        drawCircle(gold, 3.2f, Offset(cx, top - 8))
    }

    fun DrawScope.welcome() {
        glow(Offset(150f, 100f), 130f, goldLight)
        gate(48f, 112f, 88f, 54f, 158f)
        gate(188f, 252f, 88f, 54f, 158f)
        gate(92f, 208f, 72f, 16f, 150f)

        // Hanging crescent.
        strokeLine(gold, 1f, Offset(150f, 33f), Offset(150f, 48f))
        drawCircle(gold, 1.8f, Offset(150f, 49f))
        glow(Offset(150f, 64f), 24f, gold)
        crescent(Offset(148f, 64f), 13f, gold)

        // Rehal: two crossed wooden boards.
        fun board(a: Offset, b: Offset, t: Float): Path {
            val d = b - a
            val length = hypot(d.x, d.y)
            val n = Offset(-d.y, d.x) / length * t
            return Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(b.x + n.x, b.y + n.y); lineTo(a.x + n.x, a.y + n.y); close() }
        }
        val back = board(Offset(104f, 128f), Offset(178f, 194f), 14f)
        val front = board(Offset(122f, 194f), Offset(196f, 128f), 14f)
        shadow(back)
        drawPath(back, woodDark)
        drawPath(front, wood)
        drawPath(front, gold.copy(alpha = .8f), style = outline(gold, 1f))

        // Book cover.
        val cover = Path().apply {
            moveTo(150f, 142f); lineTo(88f, 130f); lineTo(94f, 104f); lineTo(150f, 114f)
            lineTo(206f, 104f); lineTo(212f, 130f); close()
        }
        shadow(cover)
        drawPath(cover, tealDark)

        // Pages, slightly curled at the top edge.
        for (sign in listOf(1f, -1f)) {
            drawPath(Path().apply {
                moveTo(150f, 136f); lineTo(150 - 58 * sign, 124f); lineTo(150 - 54 * sign, 96f)
                quadraticTo(150 - 26 * sign, 92f, 150f, 106f); close()
            }, cream)
            val frame = Path().apply {
                moveTo(150 - 8 * sign, 128f); lineTo(150 - 50 * sign, 119f); lineTo(150 - 47 * sign, 101f)
                quadraticTo(150 - 26 * sign, 98f, 150 - 8 * sign, 108f); close()
            }
            drawPath(frame, teal, style = outline(teal, 1.2f))
            drawPath(frame, gold, style = outline(gold, .5f))
            val m = Offset(150 - 28 * sign, 113f)
            rotate(45f, pivot = m) { drawRect(teal, m - Offset(3.5f, 3.5f), Size(7f, 7f)) }
            drawCircle(gold, 1.6f, m)
        }
        strokeLine(creamDark, 1f, Offset(150f, 106f), Offset(150f, 136f))

        // Bookmark ribbon.
        val ribbon = Path().apply { moveTo(146f, 136f); lineTo(154f, 136f); lineTo(154f, 160f); lineTo(150f, 156f); lineTo(146f, 160f); close() }
        drawPath(ribbon, teal)
        drawPath(ribbon, gold, style = outline(gold, .8f))
    }

    // ---------- Notifications: a phone with prayer reminders, a bell and a clock ----------

    private fun DrawScope.appIcon(topLeft: Offset) {
        drawRoundRect(tealDark, topLeft, Size(22f, 22f), CornerRadius(6f))
        val c = topLeft + Offset(11f, 17f)
        drawPath(Path().apply {
            moveTo(c.x - 7, c.y)
            cubicTo(c.x - 7, c.y - 7, c.x - 1, c.y - 7, c.x, c.y - 11)
            cubicTo(c.x + 1, c.y - 7, c.x + 7, c.y - 7, c.x + 7, c.y)
            close()
        }, cream)
        crescent(topLeft + Offset(11f, 4.5f), 2.6f, gold)
    }

    private fun DrawScope.notification(rect: Rect, dot: Color) {
        val shape = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(9f))) }
        shadow(shape, blur = 8f)
        drawPath(shape, cream)
        appIcon(rect.topLeft + Offset(6f, 6f))
        strokeLine(creamDark, 4f, rect.topLeft + Offset(36f, 12f), rect.topLeft + Offset(rect.width * .62f, 12f))
        strokeLine(creamDark, 3f, rect.topLeft + Offset(36f, 22f), rect.topLeft + Offset(rect.width * .78f, 22f))
        drawCircle(dot, 4f, rect.centerRight - Offset(11f, 0f))
    }

    private fun DrawScope.bell(c: Offset, s: Float, color: Color) {
        translate(c.x, c.y) {
            scale(s, s, pivot = Offset.Zero) {
                drawPath(Path().apply {
                    moveTo(-12f, 8f)
                    cubicTo(-10f, 4f, -10f, -2f, -9f, -6f)
                    cubicTo(-8f, -14f, 8f, -14f, 9f, -6f)
                    cubicTo(10f, -2f, 10f, 4f, 12f, 8f)
                    close()
                }, color)
                drawRoundRect(color, Offset(-14f, 7f), Size(28f, 3.5f), CornerRadius(2f))
                drawCircle(color, 3f, Offset(0f, 12.5f))
                drawCircle(color, 2f, Offset(0f, -13.5f))
            }
        }
    }

    private fun deg(radians: Float) = (radians * 180 / PI).toFloat()

    fun DrawScope.notifications() {
        glow(Offset(150f, 100f), 135f, gold)
        leaf(Offset(120f, 150f), 40f, -2.4f)
        leaf(Offset(118f, 160f), 34f, -2.9f)
        leaf(Offset(186f, 120f), 30f, -.5f)

        // Phone, tilted slightly.
        translate(150f, 100f) {
            rotate(deg(.12f), pivot = Offset.Zero) {
                val body = RoundRect(Rect(Offset(-46f, -89f), Size(92f, 178f)), CornerRadius(18f))
                shadow(Path().apply { addRoundRect(body) }, blur = 10f)
                drawPath(Path().apply { addRoundRect(body) }, tealDark)
                val screen = RoundRect(Rect(Offset(-41.5f, -84.5f), Size(83f, 169f)), CornerRadius(13.5f))
                val screenPath = Path().apply { addRoundRect(screen) }
                drawPath(screenPath, cream)
                clipPath(screenPath) {
                    mosque(Offset(-8f, 70f), 1f, creamDark)
                    mosque(Offset(24f, 72f), .6f, creamDark)
                    crescent(Offset(-6f, 26f), 4f, gold)
                    drawPath(Path().apply {
                        moveTo(-50f, 72f); quadraticTo(-20f, 58f, 8f, 70f); quadraticTo(30f, 80f, 50f, 66f)
                        lineTo(50f, 92f); lineTo(-50f, 92f); close()
                    }, teal)
                }
                drawRoundRect(tealDark, Offset(-15f, -85f), Size(30f, 6f), CornerRadius(3f))
                bell(Offset(0f, -56f), .9f, teal)
                strokeLine(gold, 2f, Offset(-20f, -62f), Offset(-25f, -65f))
                strokeLine(gold, 2f, Offset(-21f, -53f), Offset(-27f, -53f))
                strokeLine(gold, 2f, Offset(20f, -62f), Offset(25f, -65f))
                strokeLine(gold, 2f, Offset(21f, -53f), Offset(27f, -53f))
                strokeLine(cream, 2f, Offset(-14f, 80f), Offset(14f, 80f))
            }
        }

        notification(Rect(Offset(92f, 52f), Size(116f, 34f)), gold)
        notification(Rect(Offset(118f, 98f), Size(116f, 34f)), teal)

        // Golden bell on the left.
        glow(Offset(66f, 66f), 34f, gold)
        rotate(deg(-.3f), pivot = Offset(66f, 66f)) { bell(Offset(66f, 66f), 1.5f, gold) }
        strokeLine(gold, 2.5f, Offset(58f, 32f), Offset(55f, 24f))
        strokeLine(gold, 2.5f, Offset(70f, 31f), Offset(73f, 23f))

        crescent(Offset(56f, 118f), 11f, gold)
        sparkle(Offset(40f, 148f), 6f)
        sparkle(Offset(232f, 34f), 6f)
        sparkle(Offset(262f, 150f), 7f)

        // Clock with a mosque.
        val clock = Offset(254f, 92f)
        glow(clock, 40f, gold)
        drawCircle(gold, 25f, clock)
        drawCircle(cream, 21f, clock)
        clipPath(Path().apply { addOval(Rect(clock, 21f)) }) { mosque(clock + Offset(6f, 21f), .55f, teal) }
        for (i in 0 until 12) {
            val a = i * PI.toFloat() / 6
            val dir = Offset(cos(a), sin(a))
            strokeLine(gold, 1.2f, clock + dir * 16f, clock + dir * 18.5f)
        }
        strokeLine(gold, 1.8f, clock, clock + Offset(9f, -10f))
        strokeLine(gold, 1.8f, clock, clock + Offset(-4f, -7f))
        drawCircle(gold, 2f, clock)
    }

    // ---------- Location: a folded map, a pin, a route to a mosque and a compass ----------

    private fun DrawScope.mapCloud(c: Offset, s: Float) {
        val path = Path().apply {
            addOval(Rect(c + Offset(-9 * s, 2 * s), 7 * s))
            addOval(Rect(c + Offset(0f, -3 * s), 9 * s))
            addOval(Rect(c + Offset(10 * s, 2 * s), 7 * s))
            addRoundRect(RoundRect(Rect(c.x - 16 * s, c.y, c.x + 17 * s, c.y + 9 * s), CornerRadius(5 * s)))
        }
        shadow(path, blur = 4f, offset = Offset(0f, 2f))
        drawPath(path, if (dark) creamDark else palette.background)
    }

    fun DrawScope.location() {
        glow(Offset(150f, 110f), 140f, gold)
        drawOval(gold.copy(alpha = .7f), Offset(150f - 134, 118f - 52), Size(268f, 104f), style = Stroke(1.2f))
        drawCircle(gold, 3.5f, Offset(36f, 104f))
        drawCircle(gold, 3.5f, Offset(270f, 98f))
        mapCloud(Offset(68f, 54f), 1.1f)
        mapCloud(Offset(262f, 132f), 1f)

        val map = Path().apply {
            moveTo(52f, 104f); quadraticTo(110f, 82f, 150f, 98f); quadraticTo(200f, 112f, 256f, 88f)
            lineTo(244f, 152f); quadraticTo(190f, 174f, 140f, 160f); quadraticTo(90f, 146f, 38f, 168f); close()
        }
        shadow(map, blur = 8f)
        drawPath(map, cream)
        clipPath(map) {
            drawPath(Path().apply {
                moveTo(120f, 60f); cubicTo(150f, 110f, 110f, 130f, 160f, 190f); lineTo(186f, 190f)
                cubicTo(140f, 130f, 178f, 110f, 146f, 60f); close()
            }, teal.copy(alpha = .85f))
            drawOval(teal.copy(alpha = .7f), Offset(40f, 140f), Size(48f, 26f))
            strokePath(Path().apply { moveTo(40f, 128f); quadraticTo(120f, 116f, 250f, 124f) }, gold, 1.2f)
            strokePath(Path().apply { moveTo(200f, 80f); quadraticTo(214f, 130f, 196f, 180f) }, gold, 1.2f)
        }
        drawPath(map, creamDark, style = Stroke(1.2f))

        // Mosque on a green plot.
        drawPath(Path().apply { moveTo(196f, 84f); lineTo(250f, 80f); lineTo(246f, 98f); lineTo(198f, 102f); close() }, teal)
        drawRect(cream, Offset(208f, 64f), Size(28f, 28f))
        drawPath(Path().apply { moveTo(210f, 64f); cubicTo(210f, 50f, 234f, 50f, 234f, 64f); close() }, teal)
        drawRect(cream, Offset(238f, 44f), Size(7f, 48f))
        drawPath(Path().apply { moveTo(237f, 44f); lineTo(241.5f, 36f); lineTo(246f, 44f); close() }, gold)
        crescent(Offset(222f, 47f), 3f, gold)
        crescent(Offset(241.5f, 32f), 2.5f, gold)
        drawPath(Path().apply { moveTo(217f, 92f); lineTo(217f, 82f); quadraticTo(222f, 74f, 227f, 82f); lineTo(227f, 92f); close() }, gold)
        leaf(Offset(202f, 92f), 14f, -1.9f)
        leaf(Offset(202f, 92f), 14f, -1.2f)

        // Dashed route from pin to mosque.
        val route = Path().apply { moveTo(122f, 126f); quadraticTo(170f, 132f, 206f, 104f) }
        val measure = PathMeasure().apply { setPath(route, false) }
        var d = 0f
        while (d < measure.length) {
            val dash = Path()
            measure.getSegment(d, min(d + 4, measure.length), dash, true)
            strokePath(dash, tealDark, 2f)
            d += 8
        }
        drawCircle(gold, 3f, Offset(206f, 104f))

        // Main pin.
        drawOval(goldLight.copy(alpha = .8f), Offset(110f - 27, 128f - 8), Size(54f, 16f), style = Stroke(1.4f))
        drawOval(gold, Offset(110f - 15, 127f - 4.5f), Size(30f, 9f))
        val pin = Path().apply {
            moveTo(110f, 124f)
            cubicTo(98f, 104f, 82f, 86f, 82f, 64f)
            arcTo(Rect(Offset(110f, 64f), 28f), 180f, 180f, false)
            cubicTo(138f, 86f, 122f, 104f, 110f, 124f)
            close()
        }
        glow(Offset(110f, 66f), 48f, teal)
        shadow(pin, blur = 8f)
        drawPath(pin, teal)
        strokePath(Path().apply { moveTo(88f, 58f); quadraticTo(92f, 42f, 106f, 38f) }, Color.White.copy(alpha = .35f), 3f)
        drawCircle(gold, 13f, Offset(110f, 64f), style = Stroke(5f))
        drawCircle(goldLight.copy(alpha = .5f), 9f, Offset(110f, 64f))

        // Compass.
        val compass = Offset(64f, 150f)
        shadow(Path().apply { addOval(Rect(compass, 22f)) })
        drawCircle(gold, 4f, compass + Offset(0f, -24f), style = Stroke(2f))
        drawCircle(gold, 22f, compass)
        drawCircle(cream, 18f, compass)
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4
            val len = if (i % 2 == 0) 15f else 9f
            val dir = Offset(cos(a), sin(a))
            val side = Offset(-dir.y, dir.x) * 2.5f
            drawPath(Path().apply {
                moveTo(compass.x + side.x, compass.y + side.y)
                lineTo(compass.x + dir.x * len, compass.y + dir.y * len)
                lineTo(compass.x - side.x, compass.y - side.y)
                close()
            }, creamDark)
        }
        rotate(deg(.6f), pivot = compass) {
            drawPath(Path().apply { moveTo(compass.x - 3, compass.y); lineTo(compass.x, compass.y - 15); lineTo(compass.x + 3, compass.y); close() }, teal)
            drawPath(Path().apply { moveTo(compass.x - 3, compass.y); lineTo(compass.x, compass.y + 15); lineTo(compass.x + 3, compass.y); close() }, gold)
        }
        drawCircle(tealDark, 2f, compass)
        sparkle(Offset(160f, 40f), 6f)
        sparkle(Offset(46f, 84f), 5f)
    }

    // ---------- Theme: light, system and dark phones between a sun and a moon ----------

    private fun DrawScope.phone(center: Offset, angle: Float, theme: Palette, castShadow: Boolean = true) {
        val night = theme.dark
        val frame = lerp(theme.card, theme.subtitle, .3f)
        val wave = if (night) theme.selectedCard else theme.accent
        translate(center.x, center.y) {
            rotate(deg(angle), pivot = Offset.Zero) {
                val body = RoundRect(Rect(Offset(-42f, -79f), Size(84f, 158f)), CornerRadius(16f))
                if (castShadow) shadow(Path().apply { addRoundRect(body) }, blur = 10f)
                drawPath(Path().apply { addRoundRect(body) }, frame)
                drawPath(Path().apply { addRoundRect(RoundRect(Rect(Offset(-39f, -76f), Size(78f, 152f)), CornerRadius(13f))) }, theme.background)
                drawRoundRect(frame, Offset(-12f, -72f), Size(24f, 6f), CornerRadius(3f))
                val hero = Path().apply { addRoundRect(RoundRect(Rect(Offset(-33f, -58f), Size(66f, 62f)), CornerRadius(10f))) }
                drawPath(hero, theme.card)
                clipPath(hero) {
                    drawPath(Path().apply {
                        moveTo(-34f, -14f); quadraticTo(-12f, -28f, 6f, -16f); quadraticTo(22f, -6f, 34f, -22f)
                        lineTo(34f, 6f); lineTo(-34f, 6f); close()
                    }, wave.copy(alpha = .45f))
                    drawPath(Path().apply {
                        moveTo(-34f, -4f); quadraticTo(-10f, -18f, 10f, -6f); quadraticTo(24f, 2f, 34f, -8f)
                        lineTo(34f, 6f); lineTo(-34f, 6f); close()
                    }, wave)
                    if (night) {
                        crescent(Offset(16f, -42f), 7f, gold)
                        for (star in listOf(Offset(-18f, -46f), Offset(-6f, -36f), Offset(2f, -50f))) drawCircle(goldLight, 1f, star)
                    } else {
                        drawCircle(gold, 6f, Offset(16f, -42f))
                    }
                }
                for (y in listOf(12f, 38f)) drawRoundRect(theme.card, Offset(-33f, y), Size(66f, 20f), CornerRadius(8f))
                strokeLine(frame, 2f, Offset(-14f, 72f), Offset(14f, 72f))
            }
        }
    }

    fun DrawScope.theme() {
        glow(Offset(150f, 100f), 140f, gold)
        leaf(Offset(84f, 150f), 32f, 2.5f)
        leaf(Offset(86f, 138f), 28f, -2.6f)
        leaf(Offset(216f, 150f), 32f, .6f)
        leaf(Offset(214f, 138f), 28f, -.5f)
        phone(Offset(98f, 104f), -.16f, Palette.Light)
        phone(Offset(202f, 104f), .16f, Palette.Dark)
        glow(Offset(150f, 100f), 70f, goldLight)
        // The system phone is split down the middle: light on the left, dark on the right.
        val system = Offset(150f, 100f)
        phone(system, 0f, Palette.Light)
        clipRect(150f, 0f, 300f, 200f) { phone(system, 0f, Palette.Dark, castShadow = false) }
        drawRoundRect(gold, Offset(108f, 21f), Size(84f, 158f), CornerRadius(16f), style = Stroke(2f))

        // Sun.
        val sun = Offset(34f, 84f)
        glow(sun, 34f, gold)
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4
            val dir = Offset(cos(a), sin(a))
            strokeLine(gold, 4f, sun + dir * 17f, sun + dir * 23f)
        }
        drawCircle(gold, 13f, sun)
        drawCircle(goldLight.copy(alpha = .7f), 5f, sun - Offset(4f, 4f))
        drawCircle(gold, 2.5f, Offset(30f, 124f))
        drawCircle(gold, 2f, Offset(44f, 136f))

        // Moon.
        glow(Offset(268f, 104f), 34f, gold)
        crescent(Offset(266f, 106f), 20f, gold)
        sparkle(Offset(258f, 68f), 7f)
        sparkle(Offset(286f, 140f), 5f)
        drawCircle(gold, 2f, Offset(246f, 82f))
    }
}
