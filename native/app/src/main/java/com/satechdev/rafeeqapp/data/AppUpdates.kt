package com.satechdev.rafeeqapp.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.satechdev.rafeeqapp.BuildConfig

/**
 * A flexible update from Play, as lib/data/services/app_update_service.dart:
 * once a newer version is available it downloads in the background, and
 * when it is ready [onReady] asks whether to install it now. Release builds
 * only, since only Play-installed builds can update this way.
 */
@Composable
fun AppUpdateCheck(onReady: (install: () -> Unit) -> Unit) {
    if (BuildConfig.DEBUG) return
    val context = LocalContext.current
    val manager = remember { AppUpdateManagerFactory.create(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(manager) {
        val listener = InstallStateUpdatedListener { if (it.installStatus() == InstallStatus.DOWNLOADED) ready = true }
        manager.registerListener(listener)
        onDispose { manager.unregisterListener(listener) }
    }
    LaunchedEffect(manager) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                // Downloaded on an earlier run but not installed.
                info.installStatus() == InstallStatus.DOWNLOADED -> ready = true
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                    runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)) }
            }
        }
    }
    LaunchedEffect(ready) { if (ready) onReady { manager.completeUpdate() } }
}
