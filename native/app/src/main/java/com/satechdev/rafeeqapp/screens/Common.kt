package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style

/** Room for the bottom bar under a tab's list (Flutter's navigationScreenBottomPadding). */
val TabBottomPadding = 128.dp

/** Room under a pushed screen's list (Flutter's standardScreenBottomPadding). */
val ScreenBottomPadding = 60.dp

/**
 * The Flutter app's RafeeqAppBar: 56 tall under the status bar, a back
 * arrow when [onBack] is set, the title and a hairline below.
 */
@Composable
fun AppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val p = palette
    Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                val mirror = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
                Box(
                    Modifier.size(56.dp).pressable(onBack, scale = .9f),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Glyph.ArrowBack, p.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Txt(title, style(18.sp, p.title, FontWeight.SemiBold), Modifier.weight(1f), maxLines = 1)
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** A pushed screen: app bar with back, then a centred list at most 760 wide. */
@Composable
fun PushedScreen(title: String, content: LazyListScope.() -> Unit) {
    val nav = LocalNavigator.current
    Column(Modifier.fillMaxSize().background(palette.background)) {
        AppBar(title, onBack = nav::pop)
        CenteredList(bottom = ScreenBottomPadding, spacing = 8.dp, content = content)
    }
}

@Composable
fun CenteredList(bottom: Dp, spacing: Dp = 0.dp, content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

/** Matches the section titles on the home screen. */
@Composable
fun SectionTitle(text: String) {
    Txt(text, style(15.sp, palette.subtitle, FontWeight.SemiBold))
}

/** A settings row: icon, title, subtitle, on a rounded card (Flutter's SettingsCard). */
@Composable
fun SettingsCard(
    icon: Glyph,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = palette
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(onClick)
                .clip(RoundedCornerShape(12.dp))
                .background(p.card)
                .padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                AppIcon(icon, p.accent, 22.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt(title, style(14.sp, p.title, FontWeight.SemiBold, lineHeight = 1.2f), maxLines = 2)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Txt(subtitle, style(14.sp, p.subtitle, lineHeight = 1.2f))
                }
            }
        }
        if (badge != null) {
            Box(
                Modifier
                    // Top right in both languages, as Flutter's Positioned(right: -4).
                    .align(AbsoluteAlignment.TopRight)
                    .absoluteOffset(x = 4.dp, y = (-4).dp)
                    .height(20.dp)
                    .widthIn(min = 20.dp, max = 32.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(100))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Txt(badge, style(10.sp, Color.White, FontWeight.Bold, family = AppFonts.sans), maxLines = 1)
            }
        }
    }
}

/**
 * A choice in a picker (theme, language, methods). The selected card fades to
 * the selected colour and its check pops in on a spring.
 */
@Composable
fun PickerTile(title: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    val p = palette
    val still = reducedMotion()
    val fade = tween<Color>(AppMotion.FAST, easing = AppMotion.easeOut)
    val card by animateColorAsState(if (selected) p.selectedCard else p.card, fade, label = "card")
    val titleColor by animateColorAsState(if (selected) p.selectedTitle else p.title, fade, label = "title")
    val subtitleColor by animateColorAsState(if (selected) p.selectedSubtitle else p.subtitle, fade, label = "subtitle")
    val check by animateFloatAsState(
        if (selected) 1f else 0f,
        if (still) tween(0) else spring(dampingRatio = .55f, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected; role = Role.RadioButton }
            .pressable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, style(15.sp, titleColor, FontWeight.SemiBold, lineHeight = 1.2f), maxLines = 2)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Txt(subtitle, style(14.sp, subtitleColor, lineHeight = 1.2f), maxLines = 1)
            }
        }
        if (check > 0.01f) {
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(20.dp)
                    .graphicsLayer { scaleX = check; scaleY = check; alpha = check.coerceIn(0f, 1f) }
                    .background(p.selectedTitle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(Glyph.CheckMark, p.selectedCard, 14.dp)
            }
        }
    }
}

/** Space for the gesture bar under a list that has no bottom bar. */
@Composable
fun NavigationBarSpace() = Spacer(Modifier.navigationBarsPadding())
