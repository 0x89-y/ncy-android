package com.ncy.notechaty.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ncy.notechaty.ui.chat.ChatScreen
import com.ncy.notechaty.ui.chatconfig.ChatConfigScreen
import com.ncy.notechaty.ui.chatlist.ChatListScreen
import com.ncy.notechaty.ui.licenses.LicensesScreen
import com.ncy.notechaty.ui.profiles.ProfilesScreen
import com.ncy.notechaty.ui.search.SearchScreen
import com.ncy.notechaty.ui.settings.SettingsScreen
import com.ncy.notechaty.ui.starred.StarredMessagesScreen

object Routes {
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{chatId}?name={name}"
    const val CHAT_CONFIG = "chat_config/{chatId}"
    const val SETTINGS = "settings"
    const val LICENSES = "licenses"
    const val STARRED = "starred"
    const val SEARCH = "search"
    const val PROFILES = "profiles"

    fun chat(chatId: String, name: String = "") = "chat/$chatId?name=${java.net.URLEncoder.encode(name, "UTF-8")}"
    fun chatConfig(chatId: String) = "chat_config/$chatId"
}

@Composable
fun NcyNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT_LIST,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onChatClick = { chatId, chatName ->
                    navController.navigate(Routes.chat(chatId, chatName))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onStarredClick = {
                    navController.navigate(Routes.STARRED)
                },
                onSearchClick = {
                    navController.navigate(Routes.SEARCH)
                },
                onManageProfilesClick = {
                    navController.navigate(Routes.PROFILES)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val chatName = backStackEntry.arguments?.getString("name") ?: ""
            ChatScreen(
                chatId = chatId,
                chatName = chatName,
                onBack = { navController.popBackStack() },
                onConfigClick = { navController.navigate(Routes.chatConfig(chatId)) }
            )
        }

        composable(
            route = Routes.CHAT_CONFIG,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatConfigScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onDeleted = {
                    navController.popBackStack(Routes.CHAT_LIST, false)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenLicenses = { navController.navigate(Routes.LICENSES) }
            )
        }

        composable(Routes.LICENSES) {
            LicensesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STARRED) {
            StarredMessagesScreen(
                onNavigateBack = { navController.popBackStack() },
                onChatClick = { chatId ->
                    navController.navigate(Routes.chat(chatId))
                }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onChatClick = { chatId ->
                    navController.navigate(Routes.chat(chatId))
                }
            )
        }

        composable(Routes.PROFILES) {
            ProfilesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
