package com.ncy.notechaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
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
                val statusBarColor = MaterialTheme.colorScheme.primaryContainer
                val navBarColor = MaterialTheme.colorScheme.surface
                LaunchedEffect(statusBarColor, navBarColor) {
                    window.statusBarColor = statusBarColor.toArgb()
                    window.navigationBarColor = navBarColor.toArgb()
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NcyNavGraph(navController = navController)
                }
            }
        }
    }
}
