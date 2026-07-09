package com.ncy.notechaty.ui.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid

private data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String,
    val url: String
)

private val licenses = listOf(
    LicenseEntry(
        name = "Jetpack Compose",
        author = "Google / AOSP",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    LicenseEntry(
        name = "Material 3",
        author = "Google / AOSP",
        license = "Apache License 2.0",
        url = "https://m3.material.io"
    ),
    LicenseEntry(
        name = "Hilt / Dagger",
        author = "Google",
        license = "Apache License 2.0",
        url = "https://dagger.dev/hilt/"
    ),
    LicenseEntry(
        name = "Kotlin",
        author = "JetBrains",
        license = "Apache License 2.0",
        url = "https://kotlinlang.org"
    ),
    LicenseEntry(
        name = "kotlinx-coroutines",
        author = "JetBrains",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LicenseEntry(
        name = "Gson",
        author = "Google",
        license = "Apache License 2.0",
        url = "https://github.com/google/gson"
    ),
    LicenseEntry(
        name = "Coil",
        author = "Coil Contributors",
        license = "Apache License 2.0",
        url = "https://github.com/coil-kt/coil"
    ),
    LicenseEntry(
        name = "Navigation Compose",
        author = "Google / AOSP",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/navigation"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .ncyDottedGrid(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "This app uses the following open source libraries:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(licenses) { entry ->
                LicenseCard(entry)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ApacheLicenseText()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: LicenseEntry) {
    NcyDividedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApacheLicenseText() {
    NcyDividedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Apache License 2.0",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.""",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
