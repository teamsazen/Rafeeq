package com.satechdev.rafeeqapp.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import com.satechdev.rafeeqapp.R
import kotlin.math.min

/**
 * The Rafeeq wordmark from assets/calligraphy.svg. Its one path is too long
 * for a vector drawable, so it is read from res/raw once and kept.
 */
object Calligraphy {
    // The SVG's viewBox.
    const val LEFT = 320f
    const val TOP = 632f
    const val WIDTH = 1896f
    const val HEIGHT = 1448f

    @Volatile private var path: Path? = null

    fun path(context: Context): Path = path ?: synchronized(this) {
        path ?: run {
            val data = context.resources.openRawResource(R.raw.calligraphy_path).bufferedReader().use { it.readText() }
            PathParser().parsePathString(data).toPath().apply { fillType = PathFillType.EvenOdd }.also { path = it }
        }
    }
}

/** The wordmark in [color], fitted and centred in its bounds. */
@Composable
fun CalligraphyMark(color: Color, modifier: Modifier = Modifier) {
    val path = Calligraphy.path(LocalContext.current)
    Canvas(modifier) {
        val s = min(size.width / Calligraphy.WIDTH, size.height / Calligraphy.HEIGHT)
        translate((size.width - Calligraphy.WIDTH * s) / 2, (size.height - Calligraphy.HEIGHT * s) / 2) {
            scale(s, pivot = Offset.Zero) {
                translate(-Calligraphy.LEFT, -Calligraphy.TOP) { drawPath(path, color) }
            }
        }
    }
}
