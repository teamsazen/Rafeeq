package com.satechdev.rafeeqapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.prayer.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.satechdev.rafeeqapp.ui.AppFonts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Settings.load(this)
        AppFonts.init(assets)
        setContent { AppRoot() }
        // Tops up the reminders if anything changed since they were last scheduled (Flutter does this on start too).
        lifecycleScope.launch(Dispatchers.Default) { Reminders.schedule(applicationContext) }
    }
}
