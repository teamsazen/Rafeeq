package com.satechdev.rafeeqapp.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.CalligraphyMark
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style

private const val HOME = 0

/**
 * The four tabs (Rafeeq, Qur'an, Qibla, Settings) over the bottom bar, as
 * lib/ui/app_home/home/app_home_screen.dart. Back on another tab goes home.
 */
@Composable
fun AppShell() {
    val nav = LocalNavigator.current
    val goHome = { nav.tab = HOME }
    BackHandler(nav.tab != HOME, goHome)

    Box(Modifier.fillMaxSize().background(palette.background)) {
        AnimatedContent(
            nav.tab,
            transitionSpec = { fadeIn(tween(AppMotion.FAST)) togetherWith fadeOut(tween(AppMotion.FAST)) },
            label = "tab",
        ) { tab ->
            when (tab) {
                HOME -> HomeScreen()
                1 -> Column(Modifier.fillMaxSize()) {
                    AppBar(tr(R.string.quran), onBack = goHome)
                    ComingSoon()
                }
                2 -> Column(Modifier.fillMaxSize()) {
                    AppBar(tr(R.string.qibla), onBack = goHome)
                    ComingSoon()
                }
                else -> SettingsScreen(onBack = goHome)
            }
        }
        BottomBar(
            selected = nav.tab,
            labels = listOf(tr(R.string.appTitle), tr(R.string.quran), tr(R.string.qibla), tr(R.string.settings)),
            onSelect = { nav.tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private class Tab(val icon: Glyph?, val selectedIcon: Glyph?)

private val tabs = listOf(
    Tab(null, null), // The Rafeeq calligraphy.
    Tab(Glyph.Quran, Glyph.QuranSolid),
    Tab(Glyph.Qibla, Glyph.QiblaSolid),
    Tab(Glyph.Settings, Glyph.SettingsSolid),
)

/**
 * Flutter's RafeeqBottomNavigationBar: a translucent bar with a hairline on
 * top. The pill behind the selected icon slides to the next tab on a spring.
 */
@Composable
private fun BottomBar(selected: Int, labels: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = palette
    val still = reducedMotion()
    val shadow = if (p.dark) Shadow(p.navShadow, blurRadius = 8f) else null
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(p.navTop, p.navBottom)))
            .drawBehind {
                drawLine(p.navBorder, Offset.Zero, Offset(size.width, 0f), strokeWidth = .7.dp.toPx())
            }
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp)) {
            val slot = maxWidth / tabs.size
            val indicatorWidth = 64.dp
            val x by animateDpAsState(
                slot * selected + (slot - indicatorWidth) / 2,
                if (still) tween(0) else spring(dampingRatio = .78f, stiffness = Spring.StiffnessMediumLow),
                label = "indicator",
            )
            // The pill, under the icons. Offsets follow the layout direction, so it tracks the tab in Urdu too.
            Box(
                Modifier
                    .offset(x = x, y = 12.dp)
                    .size(indicatorWidth, 32.dp)
                    .background(p.navIndicator, RoundedCornerShape(22.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selected
                    val color by animateColorAsState(
                        if (isSelected) p.navSelected else p.navForeground,
                        tween(AppMotion.FAST),
                        label = "tab-$index",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics { this.selected = isSelected; role = Role.Tab }
                            .clickable(remember { MutableInteractionSource() }, null) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                            if (tab.icon == null) {
                                CalligraphyMark(color, Modifier.size(width = 27.dp * 1.4f, height = 27.dp))
                            } else {
                                AppIcon(if (isSelected) tab.selectedIcon!! else tab.icon, color, 27.dp, shadow = shadow)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Txt(
                            labels[index],
                            style(
                                12.sp,
                                color,
                                if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ).copy(shadow = if (p.dark) Shadow(p.navShadow, blurRadius = 6f) else null),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
