package com.satechdev.rafeeqapp.ui

import android.provider.Settings.Global
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

// ---------- Text ----------

/** The UI font for the app's language: GoogleSans, or Jameel Noori Nastaleeq in Urdu. */
val LocalUiFont = staticCompositionLocalOf<FontFamily> { error("No UI font") }

val uiFont: FontFamily
    @Composable @ReadOnlyComposable get() = LocalUiFont.current

/** A text style in the UI font. [lineHeight] is a multiple of the size, as in Flutter's `height`. */
@Composable
@ReadOnlyComposable
fun style(
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = uiFont,
    lineHeight: Float? = null,
    align: TextAlign = TextAlign.Unspecified,
) = TextStyle(
    fontSize = size,
    color = color,
    fontWeight = weight,
    fontFamily = family,
    lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
    textAlign = align,
)

@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) = BasicText(text, modifier, style, overflow = TextOverflow.Ellipsis, maxLines = maxLines)

// ---------- Motion helpers ----------

/** True when the system's animator scale is off ("Remove animations"). */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching { Global.getFloat(context.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
}

/**
 * Shrinks a touch while pressed and springs back on release, so every tap is
 * acknowledged the moment the finger lands (from 99 Names). No ripple.
 */
@Composable
fun Modifier.pressable(onClick: (() -> Unit)?, scale: Float = .98f): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val still = reducedMotion()
    val s by animateFloatAsState(
        if (pressed && !still) scale else 1f,
        tween(if (pressed) AppMotion.PRESS else AppMotion.RELEASE, easing = AppMotion.easeOut),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(source, indication = null, enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
}

/** A small indeterminate spinner, like Flutter's CircularProgressIndicator (Compose without Material has none). */
@Composable
fun Spinner(color: Color, size: Dp = 20.dp, stroke: Dp = 2.dp) {
    val turn = rememberInfiniteTransition(label = "spinner")
    val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle")
    val sweep by turn.animateFloat(
        30f, 270f,
        infiniteRepeatable(tween(700, easing = AppMotion.easeOut), RepeatMode.Reverse),
        label = "sweep",
    )
    Canvas(Modifier.size(size)) {
        val width = stroke.toPx()
        drawArc(
            color, angle, sweep, useCenter = false,
            topLeft = Offset(width / 2, width / 2),
            size = Size(this.size.width - width, this.size.height - width),
            style = Stroke(width, cap = StrokeCap.Round),
        )
    }
}
