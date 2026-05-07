package com.ncy.notechaty.ui.profiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object ProfileIcons {

    /** Display order in pickers. First entry is the default. */
    val keys: List<String> = listOf(
        "person",
        "account_circle",
        "work",
        "home",
        "school",
        "favorite",
        "star",
        "shopping_cart",
        "code",
        "sports_esports",
        "lightbulb",
        "diamond",
        "flag",
        "folder"
    )

    fun iconFor(key: String?): ImageVector = when (key) {
        null, "person" -> Icons.Default.Person
        "account_circle" -> Icons.Default.AccountCircle
        "work" -> Icons.Default.Work
        "home" -> Icons.Default.Home
        "school" -> Icons.Default.School
        "favorite" -> Icons.Default.Favorite
        "star" -> Icons.Default.Star
        "shopping_cart" -> Icons.Default.ShoppingCart
        "code" -> Icons.Default.Code
        "sports_esports" -> Icons.Default.SportsEsports
        "lightbulb" -> Icons.Default.Lightbulb
        "diamond" -> Icons.Default.Diamond
        "flag" -> Icons.Default.Flag
        "folder" -> Icons.Default.Folder
        else -> Icons.Default.Person
    }
}
