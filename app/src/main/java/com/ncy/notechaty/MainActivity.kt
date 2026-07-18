package com.ncy.notechaty

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import com.ncy.notechaty.security.PinPromptContent
import com.ncy.notechaty.data.sync.pear.PearRuntimeController
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
import com.ncy.notechaty.ui.desktoptheme.NcyDesktopTheme
import com.ncy.notechaty.ui.navigation.NcyNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var pinManager: PinManager

    @Inject
    lateinit var chatLockState: ChatLockState

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var phoneSyncServer: PhoneSyncServer

    @Inject
    lateinit var pearRuntimeController: PearRuntimeController

    override fun onStart() {
        super.onStart()
        phoneSyncServer.start()
        syncManager.start()
    }

    override fun onStop() {
        pearRuntimeController.stop()
        syncManager.stop()
        phoneSyncServer.stop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings
                    .map { it.pearSyncEnabled }
                    .distinctUntilChanged()
                    .collect { enabled ->
                        if (enabled) pearRuntimeController.start() else pearRuntimeController.stop()
                    }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pearRuntimeController.status.collect { status ->
                    if (status is PearRuntimeStatus.Ready) {
                        phoneSyncServer.refreshPublishedPairing()
                    }
                }
            }
        }
        setContent {
            val settings by settingsRepository.settings.collectAsState()
            NcyDesktopTheme(
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
                    // `window.statusBarColor` / `navigationBarColor` are Java-side
                    // deprecated in favour of edge-to-edge; this project
                    // deliberately paints solid theme-colored bars, so we keep
                    // the direct setters and silence the warning.
                    @Suppress("DEPRECATION")
                    window.statusBarColor = statusBarColor.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = navBarColor.toArgb()
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = isLight
                        isAppearanceLightNavigationBars = navBarColor.luminance() > 0.5f
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    var appUnlocked by remember { mutableStateOf(chatLockState.isAppUnlocked()) }
                    val needsAppLock = settings.lockOnAppStart &&
                        pinManager.hasPin() &&
                        !appUnlocked

                    if (needsAppLock) {
                        PinPromptContent(
                            title = "Ncy",
                            subtitle = "Enter your PIN to continue",
                            biometricTitle = "Unlock Ncy",
                            biometricSubtitle = "Verify to open the app",
                            useBiometric = settings.protectionUseBiometric,
                            onVerify = { pin -> pinManager.verifyPin(pin) },
                            onUnlocked = {
                                chatLockState.unlockApp()
                                appUnlocked = true
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        NcyNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
