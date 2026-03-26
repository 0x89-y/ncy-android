package com.ncy.notechaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.ui.navigation.NcyNavGraph
import com.ncy.notechaty.ui.theme.NoteChatyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings.collectAsState()
            NoteChatyTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor
            ) {
                val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                val surfaceColor = MaterialTheme.colorScheme.surface
                val statusBarColor = if (settings.topBarGradient) {
                    lerp(primaryContainer, androidx.compose.ui.graphics.Color.White, 0.3f)
                } else primaryContainer
                val navBarColor = MaterialTheme.colorScheme.surface
                val isLight = statusBarColor.luminance() > 0.5f
                LaunchedEffect(statusBarColor, navBarColor, isLight) {
                    window.statusBarColor = statusBarColor.toArgb()
                    window.navigationBarColor = navBarColor.toArgb()
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = isLight
                        isAppearanceLightNavigationBars = navBarColor.luminance() > 0.5f
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NcyNavGraph(navController = navController)
                }
            }
        }
    }
}
