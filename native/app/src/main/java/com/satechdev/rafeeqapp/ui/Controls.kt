package com.satechdev.rafeeqapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
