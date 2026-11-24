package com.satechdev.rafeeqapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * An iOS-style switch, as Flutter's CupertinoSwitch: the thumb slides on a
 * spring and the track fills with the accent (from 99 Names).
 */
@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = palette
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow), label = "switch")
    val track by animateColorAsState(
        if (checked) p.accent else p.subtitle.copy(alpha = .28f),
        tween(AppMotion.FAST),
        label = "track",
    )
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .semantics { role = Role.Switch; stateDescription = if (checked) "On" else "Off" }
            .clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(27.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}


/**
 * A slider from [min] to [max] in whole steps, as the Flutter settings
 * slider (3 dp track, 6 dp thumb). The thumb follows on a spring and each
 * step ticks (from 99 Names). Right to left in Urdu, as Flutter's Slider.
 */
@Composable
fun AppSlider(value: Float, min: Float, max: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val p = palette
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(modifier.fillMaxWidth().height(28.dp)) {
        val thumb = 12.dp
        val trackPx = with(density) { (maxWidth - thumb).toPx() }
        val half = with(density) { (thumb / 2).toPx() }
        val steps = (max - min).toInt()
        val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
        // The gesture handlers live for the whole drag; these keep them reading the latest value.
        val current by rememberUpdatedState(value)
        val report by rememberUpdatedState(onChange)
        fun set(touchX: Float) {
            var f = ((touchX - half) / trackPx).coerceIn(0f, 1f)
            if (rtl) f = 1 - f
            val v = min + Math.round(f * steps) * (max - min) / steps
            if (v != current) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                report(v)
            }
        }
        val x by animateDpAsState(with(density) { (trackPx * fraction).toDp() }, spring(stiffness = Spring.StiffnessMedium), label = "slider")
        Box(
            Modifier
                .fillMaxSize()
                // Keyed on the track only: keyed on the value, each step restarted the detector and ended the drag.
                .pointerInput(trackPx, rtl) { detectTapGestures { set(it.x) } }
                .pointerInput(trackPx, rtl) {
                    detectHorizontalDragGestures(onDragStart = { set(it.x) }) { change, _ ->
                        change.consume()
                        set(change.position.x)
                    }
                },
        ) {
            Box(Modifier.align(Alignment.CenterStart).padding(horizontal = thumb / 2).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.divider))
            Box(Modifier.align(Alignment.CenterStart).padding(start = thumb / 2).width(x).height(3.dp).clip(RoundedCornerShape(2.dp)).background(p.accent))
            Box(Modifier.align(Alignment.CenterStart).offset(x = x).size(thumb).background(p.accent, CircleShape))
        }
    }
}
