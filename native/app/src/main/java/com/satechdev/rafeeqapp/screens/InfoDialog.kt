package com.satechdev.rafeeqapp.screens

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style

/**
 * The Flutter app's AppInfoDialog: the logo and a title over a hairline,
 * then the content. It fades and grows in from 97%.
 */
@Composable
fun InfoDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = palette
    val still = reducedMotion()
    val shown = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(180, easing = AppMotion.easeOut)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(12.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = shown.value
                    val s = .97f + .03f * shown.value
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(12.dp))
                .background(p.background),
        ) {
            Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(10.dp))
                Txt(
                    title,
                    style(17.sp, p.title, FontWeight.Bold, lineHeight = if (Settings.language.rtl) 1.45f else 1.2f),
                    Modifier.weight(1f),
                    maxLines = 2,
                )
                Box(Modifier.size(48.dp).pressable(onDismiss, scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.Close, p.title, 20.dp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.title.copy(alpha = .12f)))
            Column(Modifier.padding(12.dp), content = content)
        }
    }
}

/** "Exit the app?" with Cancel and Exit, as AppHomeScreen's _showExitConfirmation. */
@Composable
fun ExitDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    val p = palette
    InfoDialog(tr(R.string.exitApp), onDismiss) {
        Txt(tr(R.string.exitAppConfirmation), style(16.sp, p.title, lineHeight = 1.5f, align = TextAlign.Center), Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.weight(1f).pressable(onDismiss).clip(RoundedCornerShape(14.dp))
                    .border(1.dp, p.title.copy(alpha = .45f), RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(tr(R.string.cancel), style(15.sp, p.title, FontWeight.SemiBold)) }
            Box(
                Modifier.weight(1f).pressable(onExit).clip(RoundedCornerShape(14.dp)).background(p.accent).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(tr(R.string.exit), style(15.sp, p.checkForeground, FontWeight.SemiBold)) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** A downloaded update waiting to be installed; null when there is none (AppUpdateService). */
object UpdatePrompt {
    var install by mutableStateOf<(() -> Unit)?>(null)
}

/** Asks to install a downloaded update now or later (showUpdateReadyDialog). */
@Composable
fun UpdateReadyDialog() {
    val install = UpdatePrompt.install ?: return
    val p = palette
    val dismiss = { UpdatePrompt.install = null }
    InfoDialog(tr(R.string.updateReady), dismiss) {
        Txt(tr(R.string.updateDownloadedMessage), style(16.sp, p.title, lineHeight = 1.5f, align = TextAlign.Center), Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.weight(1f).pressable(dismiss).clip(RoundedCornerShape(14.dp))
                    .border(1.dp, p.title.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(tr(R.string.later), style(15.sp, p.title, FontWeight.SemiBold)) }
            Box(
                Modifier.weight(1f).pressable({ dismiss(); install() }).clip(RoundedCornerShape(14.dp)).background(p.accent).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(tr(R.string.installNow), style(15.sp, p.card, FontWeight.SemiBold)) }
        }
        Spacer(Modifier.height(16.dp))
    }
}
