package com.ncy.notechaty

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.SafeId
import com.ncy.notechaty.notifications.ReminderReceiver
import com.ncy.notechaty.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import com.ncy.notechaty.security.AppLockCoordinator
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

    @Inject
    lateinit var appLockCoordinator: AppLockCoordinator

    @Inject
    lateinit var profileRepository: ProfileRepository

    /** A chat to open, from a tapped reminder notification. */
    private val pendingChat = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingChat.value = takeReminderChat(intent)
    }

    /**
     * The chat a reminder notification points at, with its profile made active. A
     * protected profile would need its PIN first, so then only the app opens.
     */
    private fun takeReminderChat(intent: Intent?): String? {
        val chatId = intent?.getStringExtra(ReminderReceiver.EXTRA_CHAT_ID)
            ?.takeIf(SafeId::isValid) ?: return null
        // Consumed once: a rotation re-creates the activity with the same intent.
        intent.removeExtra(ReminderReceiver.EXTRA_CHAT_ID)
        val profileId = intent.getStringExtra(ReminderReceiver.EXTRA_PROFILE_ID)?.takeIf { it.isNotEmpty() }
            ?: profileRepository.findProfileForChat(chatId)
        val profile = profileId?.let(profileRepository::profileById) ?: return null
        if (profile.id != profileRepository.activeProfileId.value) {
            if (profile.isProtected) return null
            profileRepository.setActive(profile.id)
        }
        return chatId
    }

    override fun onStart() {
        super.onStart()
        appLockCoordinator.onForeground()
        phoneSyncServer.start()
        syncManager.start()
    }

    override fun onStop() {
        // A rotation stops and restarts the activity without the user leaving.
        if (!isChangingConfigurations) appLockCoordinator.onBackground()
        pearRuntimeController.stop()
        syncManager.stop()
        phoneSyncServer.stop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingChat.value = takeReminderChat(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings
                    .map { it.syncTransportMode.usesPear }
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
                val surfaceColor = MaterialTheme.colorScheme.surface
                val statusBarColor = MaterialTheme.colorScheme.primaryContainer
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
                    // Observed, not remembered once: a re-lock after time in the
                    // background resets it, and the prompt has to come back.
                    val appUnlockedAt by chatLockState.appUnlockedAt.collectAsState()
                    val needsAppLock = settings.lockOnAppStart &&
                        pinManager.hasPin() &&
                        appUnlockedAt == 0L

                    // The prompt covers the app rather than replacing it, so a re-lock
                    // returns the user where they were instead of to the chat list.
                    Box(Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        Box(
                            Modifier
                                .fillMaxSize()
                                // Keep a screen reader out of what the lock hides.
                                .then(if (needsAppLock) Modifier.clearAndSetSemantics {} else Modifier)
                        ) {
                            NcyNavGraph(navController = navController)
                        }
                        // Behind the app lock, if there is one: the chat is waiting
                        // once the PIN is in.
                        val chatToOpen by pendingChat.collectAsState()
                        LaunchedEffect(chatToOpen) {
                            chatToOpen?.let {
                                navController.navigate(Routes.chat(it))
                                pendingChat.value = null
                            }
                        }
                        if (needsAppLock) {
                            // An opaque Surface also swallows touches meant for the app below.
                            Surface(modifier = Modifier.fillMaxSize()) {
                                PinPromptContent(
                                    title = "Ncy",
                                    subtitle = "Enter your PIN to continue",
                                    biometricTitle = "Unlock Ncy",
                                    biometricSubtitle = "Verify to open the app",
                                    useBiometric = settings.protectionUseBiometric,
                                    onVerify = { pin ->
                                        pinManager.verifyPin(pin).also { ok ->
                                            if (ok) appLockCoordinator.appUnlockedWithPin(pin)
                                        }
                                    },
                                    onUnlocked = { chatLockState.unlockApp() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
