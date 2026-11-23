package com.satechdev.rafeeqapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * The Flutter app's hand-painted illustrations (lib/ui/widgets/art_painter.dart
 * and scene_artwork.dart), drawn with the same shapes and coordinates.
 */

/** The illustration colours, from the palette: teal and gold in both themes. */
class ArtColors(val palette: Palette) {
    val dark = palette.dark
    val primary = if (dark) palette.selectedCard else palette.accent
    val gold = Palette.Dark.accent
    val surface = palette.card
    val surfaceDeep = if (dark) lerp(palette.card, palette.subtitle, .22f) else lerp(palette.card, palette.selectedCard, .2f)
    val wood = if (dark) lerp(Palette.Dark.accent, Color.Black, .45f) else palette.selectedCard

    val primaryDeep get() = lerp(primary, Color.Black, .3f)
    val goldLight get() = lerp(gold, Color.White, .45f)
    val woodDeep get() = lerp(wood, Color.Black, .3f)
}

/** Drawing helpers shared by the sky and the scenes (Flutter's ArtPainter). */
open class Art(val colors: ArtColors) {
    val dark get() = colors.dark
    val palette get() = colors.palette
    val teal get() = colors.primary
    val tealDark get() = colors.primaryDeep
    val gold get() = colors.gold
    val goldLight get() = colors.goldLight
    val cream get() = colors.surface
    val creamDark get() = colors.surfaceDeep
    val wood get() = colors.wood
    val woodDark get() = colors.woodDeep

    fun DrawScope.stroke(color: Color, width: Float, from: Offset, to: Offset) =
        drawLine(color, from, to, width, StrokeCap.Round)

    fun DrawScope.strokePath(path: Path, color: Color, width: Float) =
        drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

    fun DrawScope.glow(center: Offset, radius: Float, color: Color) {
        val alpha = if (dark) .45f else .28f
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius),
            radius,
            center,
        )
    }

    /** A soft drop shadow under [path] (Flutter's ArtPainter.shadow, a blurred mask). */
    fun DrawScope.shadow(path: Path, blur: Float = 6f, offset: Offset = Offset(0f, 4f)) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.Black.copy(alpha = if (dark) .35f else .12f).toArgb()
            // Flutter gives a sigma, Android a radius (sigma ≈ 0.577 × radius); both follow the canvas scale.
            maskFilter = BlurMaskFilter(blur * 1.73f, BlurMaskFilter.Blur.NORMAL)
        }
        translate(offset.x, offset.y) { drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), paint) } }
    }

    fun DrawScope.leaf(base: Offset, length: Float, angle: Float) {
        rotate(Math.toDegrees(angle.toDouble()).toFloat(), pivot = base) {
            drawPath(Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(base.x + length * .5f, base.y - length * .32f, base.x + length, base.y)
                quadraticTo(base.x + length * .5f, base.y + length * .32f, base.x, base.y)
                close()
            }, tealDark)
            drawLine(teal.copy(alpha = .7f), base, base + Offset(length * .85f, 0f), 1f, StrokeCap.Round)
        }
    }

    fun DrawScope.sparkle(c: Offset, r: Float, color: Color = gold) {
        val inner = r * .28f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + inner, c.y - inner, c.x + r, c.y)
            quadraticTo(c.x + inner, c.y + inner, c.x, c.y + r)
            quadraticTo(c.x - inner, c.y + inner, c.x - r, c.y)
            quadraticTo(c.x - inner, c.y - inner, c.x, c.y - r)
            close()
        }
        drawPath(path, color)
    }

    fun DrawScope.crescent(c: Offset, r: Float, color: Color) {
        val path = Path().apply {
            op(
                Path().apply { addOval(Rect(c, r)) },
                Path().apply { addOval(Rect(c + Offset(r * .45f, -r * .3f), r * .85f)) },
                PathOperation.Difference,
            )
        }
        drawPath(path, color)
    }

    /** A small mosque silhouette whose dome sits on [base]. */
    fun DrawScope.mosque(base: Offset, s: Float, color: Color) {
        translate(base.x, base.y) {
            scale(s, s, pivot = Offset.Zero) {
                drawRect(color, Offset(-18f, -12f), Size(36f, 12f))
                drawPath(Path().apply {
                    moveTo(-13f, -12f)
                    cubicTo(-13f, -26f, -2f, -26f, 0f, -34f)
                    cubicTo(2f, -26f, 13f, -26f, 13f, -12f)
                    close()
                }, color)
                drawRect(color, Offset(-24f, -34f), Size(5f, 34f))
                drawRect(color, Offset(19f, -34f), Size(5f, 34f))
                drawPath(Path().apply { moveTo(-24f, -34f); lineTo(-21.5f, -40f); lineTo(-19f, -34f); close() }, color)
                drawPath(Path().apply { moveTo(19f, -34f); lineTo(21.5f, -40f); lineTo(24f, -34f); close() }, color)
            }
        }
    }

    /** Rolling hills through [crest], filled down to [bottom]. */
    fun DrawScope.hills(crest: List<Offset>, color: Color, bottom: Float = 200f) {
        val path = Path().apply {
            moveTo(crest.first().x, bottom)
            lineTo(crest.first().x, crest.first().y)
            for (i in 1 until crest.size) {
                val prev = crest[i - 1]
                val cur = crest[i]
                val half = (cur.x - prev.x) / 2
                cubicTo(prev.x + half, prev.y, cur.x - half, cur.y, cur.x, cur.y)
            }
            lineTo(crest.last().x, bottom)
            close()
        }
        drawPath(path, color)
    }

    fun DrawScope.cloud(c: Offset, s: Float, color: Color) {
        val path = Path().apply {
            addOval(Rect(c + Offset(-9 * s, 2 * s), 7 * s))
            addOval(Rect(c + Offset(0f, -3 * s), 9 * s))
            addOval(Rect(c + Offset(10 * s, 2 * s), 7 * s))
            addRoundRect(RoundRect(Rect(c.x - 16 * s, c.y, c.x + 17 * s, c.y + 9 * s), CornerRadius(5 * s)))
        }
        drawPath(path, color)
    }
}

// ---------- Scenes ----------

/** Painted scenes used across the app in place of bitmap illustrations. */
enum class SceneArt { Makkah, Madinah, Sunrise, Noon, Sunset, Mosque }

/** A scene drawn in a 300 × 200 canvas, scaled to fit and centred (Flutter's ArtPainter.paint). */
@Composable
fun SceneArtwork(scene: SceneArt, modifier: Modifier = Modifier, width: Dp? = null, height: Dp? = null) {
    val art = Scenes(ArtColors(palette))
    val sized = if (width != null && height != null) modifier.size(width, height) else modifier.aspectRatio(3f / 2f)
    Canvas(sized) {
        val s = min(size.width / 300f, size.height / 200f)
        translate((size.width - 300f * s) / 2, (size.height - 200f * s) / 2) {
            scale(s, s, pivot = Offset.Zero) {
                with(art) {
                    when (scene) {
                        SceneArt.Makkah -> makkah()
                        SceneArt.Madinah -> madinah()
                        SceneArt.Sunrise -> sunrise()
                        SceneArt.Noon -> noon()
                        SceneArt.Sunset -> sunset()
                        SceneArt.Mosque -> mosqueScene()
                    }
                }
            }
        }
    }
}

private class Scenes(colors: ArtColors) : Art(colors) {
    /** Light masonry used for mosque walls and minarets. */
    val stone get() = lerp(goldLight, Color.White, .55f)
    val silhouette get() = lerp(tealDark, Color.Black, .45f)

    fun DrawScope.verticalGradient(rect: Rect, list: List<Color>, stops: List<Float>? = null) {
        val brush = if (stops == null) {
            Brush.verticalGradient(list, rect.top, rect.bottom)
        } else {
            Brush.verticalGradient(*stops.zip(list).toTypedArray(), startY = rect.top, endY = rect.bottom)
        }
        drawRect(brush, rect.topLeft, rect.size)
    }

    fun DrawScope.palm(base: Offset, height: Float, lean: Float, color: Color) {
        val top = base + Offset(lean, -height)
        strokePath(Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(base.x + lean * .2f, base.y - height * .5f, top.x, top.y)
        }, color, height * .07f)
        val length = height * .55f
        for (degrees in listOf(195f, 228f, 258f, 288f, 318f, 348f)) {
            val a = degrees * PI.toFloat() / 180
            val dir = Offset(cos(a), sin(a))
            val tip = top + dir * length + Offset(0f, length * .3f)
            val mid = top + dir * (length * .5f)
            drawPath(Path().apply {
                moveTo(top.x, top.y)
                quadraticTo(mid.x, mid.y - length * .22f, tip.x, tip.y)
                quadraticTo(mid.x, mid.y + length * .04f, top.x, top.y)
                close()
            }, color)
        }
    }

    fun DrawScope.minaret(x: Float, base: Float, height: Float, width: Float, body: Color, cap: Color, pointed: Boolean = false) {
        val top = base - height
        val half = width / 2
        drawRect(body, Offset(x - half, top), Size(width, height))
        for (f in listOf(.38f, .72f)) {
            val y = base - height * f
            drawRect(cap, Offset(x - half - 2, y - 2.5f), Size(width + 4, 3.5f))
        }
        val finial: Float
        if (pointed) {
            finial = top - width * 2.6f
            drawPath(Path().apply { moveTo(x - half, top); lineTo(x, finial); lineTo(x + half, top); close() }, cap)
        } else {
            finial = top - width * 1.4f
            drawPath(Path().apply {
                moveTo(x - half - 1, top)
                cubicTo(x - half - 1, top - width * .9f, x - 1, top - width * .9f, x, finial)
                cubicTo(x + 1, top - width * .9f, x + half + 1, top - width * .9f, x + half + 1, top)
                close()
            }, cap)
        }
        stroke(gold, 1.2f, Offset(x, finial), Offset(x, finial - 5))
        crescent(Offset(x, finial - 7.5f), 2.6f, gold)
    }

    /** The soft cloud-shaped backdrop behind the Makkah and Madinah scenes. */
    fun DrawScope.backdrop() {
        val color = if (dark) lerp(palette.card, gold, .1f) else lerp(palette.card, palette.background, .25f)
        drawPath(Path().apply {
            addOval(Rect(Offset(150f, 96f), 72f))
            addOval(Rect(Offset(88f, 118f), 48f))
            addOval(Rect(Offset(212f, 118f), 48f))
            addOval(Rect(Offset(46f, 144f), 30f))
            addOval(Rect(Offset(254f, 144f), 30f))
            addRect(Rect(46f, 140f, 254f, 174f))
        }, color)
    }

    fun DrawScope.floor(center: Offset, width: Float) {
        val color = if (dark) lerp(palette.card, palette.subtitle, .18f) else lerp(palette.card, Color.White, .45f)
        drawOval(color, Offset(center.x - width / 2, center.y - 13), Size(width, 26f))
    }

    /** A row of arched openings between [left] and [right]. */
    fun DrawScope.arcade(left: Float, right: Float, top: Float, bottom: Float, count: Int, wall: Color, opening: Color) {
        drawRect(wall, Offset(left, top), Size(right - left, bottom - top))
        drawRect(gold, Offset(left, top), Size(right - left, 2f))
        val pitch = (right - left) / count
        for (i in 0 until count) {
            val l = left + i * pitch + pitch * .2f
            val r = left + (i + 1) * pitch - pitch * .2f
            val w = r - l
            drawPath(Path().apply {
                moveTo(l, bottom)
                lineTo(l, top + 6 + w * .5f)
                quadraticTo(l, top + 6, (l + r) / 2, top + 5)
                quadraticTo(r, top + 6, r, top + 6 + w * .5f)
                lineTo(r, bottom)
                close()
            }, opening)
        }
    }

    private fun DrawScope.framed(content: DrawScope.() -> Unit) {
        val frame = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, 300f, 200f), CornerRadius(26f))) }
        clipPath(frame) { content() }
    }

    /** The Kaaba in the Masjid al-Haram, flanked by arcades and two minarets. */
    fun DrawScope.makkah() {
        backdrop()
        glow(Offset(150f, 74f), 70f, gold)
        drawCircle(goldLight.copy(alpha = .55f), 32f, Offset(150f, 74f))
        cloud(Offset(84f, 92f), 1f, stone.copy(alpha = .7f))
        cloud(Offset(220f, 84f), .9f, stone.copy(alpha = .7f))
        minaret(64f, 146f, 70f, 10f, stone, gold)
        minaret(236f, 146f, 70f, 10f, stone, gold)
        val opening = lerp(tealDark, palette.background, .15f)
        arcade(22f, 116f, 140f, 166f, 5, stone, opening)
        arcade(184f, 278f, 140f, 166f, 5, stone, opening)
        floor(Offset(150f, 170f), 236f)

        // The Kaaba as an isometric cube.
        val a = Offset(106f, 90f)
        val b = Offset(150f, 102f)
        val e = Offset(196f, 88f)
        val g = Offset(152f, 78f)
        val h = 66f
        val black = Palette.Light.title
        drawOval(Color.Black.copy(alpha = if (dark) .4f else .14f), Offset(152f - 55, 166f - 7), Size(110f, 14f))
        fun quad(p1: Offset, p2: Offset, p3: Offset, p4: Offset) = Path().apply {
            moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); lineTo(p4.x, p4.y); close()
        }
        val down = Offset(0f, h)
        drawPath(quad(a, b, b + down, a + down), black)
        drawPath(quad(b, e, e + down, b + down), lerp(black, Color.White, .1f))
        drawPath(quad(a, g, e, b), lerp(black, Color.White, .2f))

        // The gold band (hizam) wrapping both faces.
        fun band(from: Offset, to: Offset, y0: Float, y1: Float) =
            quad(from + Offset(0f, y0), to + Offset(0f, y0), to + Offset(0f, y1), from + Offset(0f, y1))
        drawPath(band(a, b, 13f, 20f), gold)
        drawPath(band(b, e, 13f, 20f), gold)
        // Calligraphy panels hanging below the band.
        fun onFace(from: Offset, to: Offset, u: Float, v: Float) = androidx.compose.ui.geometry.lerp(from, to, u) + Offset(0f, v)
        for (u in listOf(.2f, .5f, .8f)) {
            for ((from, to) in listOf(a to b, b to e)) {
                val p = onFace(from, to, u, 26f)
                rotate(45f, pivot = p) { drawRect(gold, p - Offset(1.75f, 1.75f), Size(3.5f, 3.5f)) }
            }
        }
        // Door on the right face.
        drawPath(quad(onFace(b, e, .55f, 32f), onFace(b, e, .74f, 32f), onFace(b, e, .74f, 58f), onFace(b, e, .55f, 58f)), gold)
        // Marble base (shadharwan).
        drawPath(band(a, b, h - 3, h), stone)
        drawPath(band(b, e, h - 3, h), stone)
        drawLine(gold.copy(alpha = .35f), b, b + down, .8f, StrokeCap.Round)
    }

    private fun DrawScope.umbrella(top: Offset, span: Float) {
        drawRect(wood, Offset(top.x - 1.2f, top.y + 9), Size(2.4f, 168 - top.y - 9))
        drawPath(Path().apply {
            moveTo(top.x - span / 2, top.y)
            quadraticTo(top.x, top.y + 4, top.x + span / 2, top.y)
            lineTo(top.x, top.y + 11)
            close()
        }, stone)
        for (f in listOf(-.3f, 0f, .3f)) {
            drawLine(creamDark, top + Offset(span * f, 1.5f), top + Offset(0f, 11f), .8f, StrokeCap.Round)
        }
    }

    private fun DrawScope.lantern(x: Float, height: Float) {
        val base = 168f
        val top = base - height
        drawRect(wood, Offset(x - 1.6f, top), Size(3.2f, height))
        drawRect(wood, Offset(x - 3.5f, base - 3), Size(7f, 3f))
        drawRoundRect(gold, Offset(x - 4, top - 9), Size(8f, 9f), CornerRadius(1.5f))
        drawRect(goldLight, Offset(x - 2.4f, top - 7.5f), Size(4.8f, 6f))
        drawPath(Path().apply { moveTo(x - 4.5f, top - 9); lineTo(x, top - 14); lineTo(x + 4.5f, top - 9); close() }, gold)
    }

    /** Masjid an-Nabawi with the Green Dome, minarets, umbrellas and lanterns. */
    fun DrawScope.madinah() {
        backdrop()
        glow(Offset(178f, 80f), 70f, gold)
        drawCircle(goldLight.copy(alpha = .55f), 36f, Offset(178f, 80f))
        cloud(Offset(112f, 100f), 1f, stone.copy(alpha = .7f))
        cloud(Offset(236f, 104f), .9f, stone.copy(alpha = .7f))
        for ((x, h) in listOf(56f to 82f, 98f to 70f, 220f to 68f)) {
            minaret(x, 132f, h, 9f, stone, teal, pointed = true)
        }
        minaret(260f, 132f, 88f, 12f, stone, gold)

        // Small white dome.
        drawPath(Path().apply { moveTo(110f, 128f); cubicTo(110f, 112f, 132f, 112f, 132f, 128f); close() }, stone)

        // The Green Dome on its drum.
        drawRect(stone, Offset(152f, 110f), Size(52f, 18f))
        drawRect(tealDark, Offset(150f, 108f), Size(56f, 5f))
        val dome = Path().apply {
            moveTo(152f, 109f)
            cubicTo(148f, 82f, 170f, 76f, 178f, 60f)
            cubicTo(186f, 76f, 208f, 82f, 204f, 109f)
            close()
        }
        drawPath(dome, teal)
        clipPath(dome) {
            for (x in listOf(160f, 169f, 178f, 187f, 196f)) {
                drawLine(tealDark.copy(alpha = .45f), Offset(x, 110f), Offset(178 + (x - 178) * .2f, 62f), 1f, StrokeCap.Round)
            }
        }
        strokePath(Path().apply { moveTo(158f, 100f); quadraticTo(158f, 84f, 170f, 76f) }, Color.White.copy(alpha = .3f), 2.5f)
        stroke(gold, 1.4f, Offset(178f, 60f), Offset(178f, 52f))
        crescent(Offset(178f, 48f), 3.4f, gold)

        // Main facade with a crenellated parapet and arched windows.
        drawRect(stone, Offset(64f, 128f), Size(180f, 38f))
        var x = 64f
        while (x < 244) {
            drawRect(stone, Offset(x, 125f), Size(3.5f, 3f))
            x += 6
        }
        drawRect(creamDark.copy(alpha = .8f), Offset(64f, 133f), Size(180f, 1.5f))
        val window = lerp(stone, wood, .35f)
        for (i in 0 until 12) {
            val wx = 72 + i * 14.5f
            if (abs(wx - 147) < 8) continue
            drawPath(Path().apply {
                moveTo(wx, 160f)
                lineTo(wx, 146f)
                quadraticTo(wx, 140f, wx + 4, 139f)
                quadraticTo(wx + 8, 140f, wx + 8, 146f)
                lineTo(wx + 8, 160f)
                close()
            }, window)
        }
        drawRect(wood, Offset(144f, 148f), Size(12f, 18f))
        floor(Offset(150f, 170f), 250f)
        umbrella(Offset(30f, 138f), 52f)
        umbrella(Offset(270f, 138f), 52f)
        for (lx in listOf(86f, 128f, 186f, 228f)) lantern(lx, 26f)
    }

    /** The sun breaking over the hills, rays rising, with a mosque on the skyline. */
    fun DrawScope.sunrise() = framed {
        verticalGradient(Rect(0f, 0f, 300f, 200f), listOf(
            if (dark) lerp(palette.background, teal, .3f) else lerp(palette.background, teal, .14f),
            if (dark) lerp(palette.background, gold, .45f) else lerp(palette.background, goldLight, .75f),
        ))
        val sun = Offset(120f, 148f)
        glow(sun, 130f, gold)
        for (i in 0 until 9) {
            val a = PI.toFloat() + (i + .5f) * PI.toFloat() / 9
            val dir = Offset(cos(a), sin(a))
            val side = Offset(-dir.y, dir.x) * 5f
            val reach = if (i % 2 == 0) 100f else 76f
            drawPath(Path().apply {
                moveTo(sun.x + dir.x * 40 + side.x, sun.y + dir.y * 40 + side.y)
                lineTo(sun.x + dir.x * reach, sun.y + dir.y * reach)
                lineTo(sun.x + dir.x * 40 - side.x, sun.y + dir.y * 40 - side.y)
                close()
            }, gold.copy(alpha = .5f))
        }
        drawCircle(gold, 32f, sun)
        drawCircle(goldLight.copy(alpha = .6f), 24f, sun)
        val cloudColor = if (dark) lerp(palette.card, gold, .3f) else Color.White.copy(alpha = .85f)
        cloud(Offset(236f, 50f), 1.1f, cloudColor)
        cloud(Offset(46f, 64f), .8f, cloudColor)
        hills(listOf(Offset(0f, 146f), Offset(60f, 130f), Offset(124f, 150f), Offset(196f, 124f), Offset(260f, 140f), Offset(300f, 132f)), lerp(teal, palette.background, .45f))
        hills(listOf(Offset(0f, 166f), Offset(70f, 150f), Offset(150f, 168f), Offset(230f, 152f), Offset(300f, 164f)), teal)
        mosque(Offset(232f, 156f), .95f, tealDark)
        minaret(262f, 158f, 50f, 7f, tealDark, tealDark)
        hills(listOf(Offset(0f, 186f), Offset(80f, 174f), Offset(170f, 190f), Offset(250f, 178f), Offset(300f, 186f)), tealDark)
        palm(Offset(34f, 186f), 42f, -4f, tealDark)
        palm(Offset(58f, 190f), 30f, 5f, tealDark)
    }

    /** The sun at its zenith directly above a mosque, its shadow barely visible. */
    fun DrawScope.noon() = framed {
        verticalGradient(Rect(0f, 0f, 300f, 200f), listOf(
            if (dark) lerp(palette.background, teal, .45f) else lerp(Color.White, teal, .3f),
            if (dark) lerp(palette.background, teal, .15f) else lerp(palette.background, teal, .06f),
        ))
        val sun = Offset(150f, 42f)
        // A beam falling straight down marks the sun at its highest point.
        drawPath(
            Path().apply { moveTo(138f, 60f); lineTo(162f, 60f); lineTo(196f, 172f); lineTo(104f, 172f); close() },
            Brush.verticalGradient(listOf(goldLight.copy(alpha = .45f), goldLight.copy(alpha = 0f)), 60f, 172f),
        )
        glow(sun, 90f, gold)
        for (i in 0 until 12) {
            val a = i * PI.toFloat() / 6
            val dir = Offset(cos(a), sin(a))
            stroke(gold, 3.5f, sun + dir * 28f, sun + dir * 38f)
        }
        drawCircle(gold, 22f, sun)
        drawCircle(goldLight.copy(alpha = .6f), 15f, sun)
        val cloudColor = if (dark) lerp(palette.card, palette.subtitle, .3f) else Color.White
        cloud(Offset(58f, 54f), 1f, cloudColor)
        cloud(Offset(246f, 70f), .85f, cloudColor)
        // Ground.
        drawRect(lerp(palette.card, gold, if (dark) .15f else .25f), Offset(0f, 170f), Size(300f, 30f))
        palm(Offset(46f, 174f), 46f, -3f, teal)
        palm(Offset(256f, 174f), 46f, 3f, teal)
        // Mosque directly beneath the sun.
        minaret(100f, 172f, 68f, 10f, stone, teal)
        minaret(200f, 172f, 68f, 10f, stone, teal)
        drawRect(stone, Offset(112f, 134f), Size(76f, 38f))
        drawRect(stone, Offset(124f, 128f), Size(52f, 8f))
        drawPath(Path().apply {
            moveTo(126f, 130f)
            cubicTo(124f, 108f, 142f, 104f, 150f, 92f)
            cubicTo(158f, 104f, 176f, 108f, 174f, 130f)
            close()
        }, teal)
        stroke(gold, 1.4f, Offset(150f, 92f), Offset(150f, 84f))
        crescent(Offset(150f, 80f), 3.2f, gold)
        drawPath(Path().apply {
            moveTo(141f, 172f); lineTo(141f, 156f)
            quadraticTo(141f, 148f, 150f, 146f)
            quadraticTo(159f, 148f, 159f, 156f)
            lineTo(159f, 172f); close()
        }, tealDark)
        for (wx in listOf(120f, 170f)) drawRoundRect(tealDark, Offset(wx - 3.5f, 146f), Size(7f, 12f), CornerRadius(3.5f))
        // The shortest shadow of the day, right under the building.
        drawOval(Color.Black.copy(alpha = if (dark) .35f else .14f), Offset(150f - 48, 173f - 2.5f), Size(96f, 5f))
    }

    /** The sun sinking into the water behind a mosque silhouette at dusk. */
    fun DrawScope.sunset() = framed {
        val horizon = 140f
        verticalGradient(
            Rect(0f, 0f, 300f, horizon),
            listOf(lerp(tealDark, Color.Black, if (dark) .35f else .05f), lerp(wood, gold, .35f), lerp(gold, goldLight, .3f)),
            listOf(0f, .62f, 1f),
        )
        for (star in listOf(Offset(30f, 26f), Offset(84f, 18f), Offset(120f, 34f), Offset(250f, 22f))) drawCircle(goldLight, 1.3f, star)
        crescent(Offset(54f, 38f), 8f, goldLight)
        val sun = Offset(196f, horizon)
        glow(sun, 110f, gold)
        drawCircle(lerp(gold, goldLight, .2f), 34f, sun)
        for (r in listOf(Rect(46f, 92f, 138f, 98f), Rect(178f, 84f, 262f, 90f), Rect(150f, 104f, 216f, 109f))) {
            drawRoundRect(goldLight.copy(alpha = .45f), r.topLeft, r.size, CornerRadius(3f))
        }
        hills(listOf(Offset(0f, 134f), Offset(90f, 122f), Offset(160f, 138f), Offset(300f, 139f)), lerp(tealDark, wood, .45f).copy(alpha = .85f), bottom = horizon)
        // Water with the sun's reflection.
        verticalGradient(Rect(0f, horizon, 300f, 200f), listOf(lerp(tealDark, gold, .3f), lerp(tealDark, Color.Black, .3f)))
        for (i in 0 until 6) {
            val w = 64f - i * 10
            val y = horizon + 6 + i * 7
            stroke(goldLight.copy(alpha = .8f - i * .11f), 2.4f, Offset(sun.x - w / 2, y), Offset(sun.x + w / 2, y))
        }
        // Shore with a mosque silhouette.
        hills(listOf(Offset(0f, 148f), Offset(70f, 144f), Offset(130f, 146f), Offset(150f, 156f)), silhouette, bottom = 162f)
        drawRect(silhouette, Offset(0f, 156f), Size(150f, 6f))
        mosque(Offset(70f, 148f), 1.3f, silhouette)
        for (wx in listOf(60f, 70f, 80f)) drawRoundRect(goldLight, Offset(wx - 2, 136f), Size(4f, 8f), CornerRadius(2f))
        minaret(116f, 150f, 66f, 8f, silhouette, silhouette)
        palm(Offset(22f, 152f), 46f, -4f, silhouette)
        palm(Offset(138f, 154f), 34f, 4f, silhouette)
    }

    /** A mosque among palms and dunes, anchored to the bottom-right corner so it can sit behind text. */
    fun DrawScope.mosqueScene() {
        val sun = Offset(206f, 118f)
        glow(sun, 50f, gold)
        drawCircle(lerp(gold, goldLight, .25f), 22f, sun)
        drawRoundRect(goldLight.copy(alpha = .8f), Offset(160f, 124f), Size(44f, 4f), CornerRadius(2f))
        hills(listOf(Offset(120f, 200f), Offset(170f, 170f), Offset(232f, 162f), Offset(300f, 150f)), lerp(teal, palette.background, .35f))
        // Mosque body.
        drawRect(stone, Offset(194f, 146f), Size(62f, 40f))
        drawRect(stone, Offset(200f, 138f), Size(50f, 9f))
        drawRect(tealDark, Offset(198f, 136f), Size(54f, 3f))
        drawPath(Path().apply {
            moveTo(202f, 137f)
            cubicTo(198f, 112f, 218f, 108f, 225f, 92f)
            cubicTo(232f, 108f, 252f, 112f, 248f, 137f)
            close()
        }, teal)
        stroke(gold, 1.4f, Offset(225f, 92f), Offset(225f, 84f))
        crescent(Offset(225f, 80f), 3.2f, gold)
        for (wx in listOf(206f, 214f, 236f, 244f)) drawRoundRect(gold, Offset(wx - 2, 140f), Size(4f, 6f), CornerRadius(2f))
        drawPath(Path().apply {
            moveTo(216f, 186f); lineTo(216f, 166f)
            quadraticTo(216f, 156f, 225f, 152f)
            quadraticTo(234f, 156f, 234f, 166f)
            lineTo(234f, 186f); close()
        }, tealDark)
        drawPath(Path().apply {
            moveTo(220f, 186f); lineTo(220f, 168f)
            quadraticTo(220f, 161f, 225f, 158f)
            quadraticTo(230f, 161f, 230f, 168f)
            lineTo(230f, 186f); close()
        }, goldLight)
        minaret(276f, 186f, 116f, 14f, stone, teal)
        drawRect(goldLight, Offset(273.5f, 98f), Size(5f, 9f))
        palm(Offset(182f, 190f), 62f, -6f, tealDark)
        palm(Offset(262f, 192f), 70f, 8f, teal)
        hills(listOf(Offset(110f, 200f), Offset(170f, 182f), Offset(240f, 190f), Offset(300f, 184f)), lerp(gold, palette.background, .25f))
        hills(listOf(Offset(210f, 200f), Offset(256f, 188f), Offset(300f, 194f)), tealDark)
    }
}
