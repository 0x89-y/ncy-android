package com.ncy.notechaty.ui.licenses

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.NcySectionLabel
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid

private data class LicenseEntry(
    val name: String,
    val author: String,
    val license: String = "Apache-2.0",
    val url: String
)

private val licenses = listOf(
    LicenseEntry(
        name = "Jetpack Compose",
        author = "Google / AOSP",
        url = "https://developer.android.com/jetpack/compose"
    ),
    LicenseEntry(
        name = "Compose Material 3",
        author = "Google / AOSP",
        url = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
    ),
    LicenseEntry(
        name = "Hilt / Dagger",
        author = "Google",
        url = "https://dagger.dev/hilt/"
    ),
    LicenseEntry(
        name = "Kotlin",
        author = "JetBrains",
        url = "https://kotlinlang.org"
    ),
    LicenseEntry(
        name = "kotlinx-coroutines",
        author = "JetBrains",
        url = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LicenseEntry(
        name = "Gson",
        author = "Google",
        url = "https://github.com/google/gson"
    ),
    LicenseEntry(
        name = "Coil",
        author = "Coil Contributors",
        url = "https://github.com/coil-kt/coil"
    ),
    LicenseEntry(
        name = "Navigation Compose",
        author = "Google / AOSP",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    LicenseEntry(
        name = "OkHttp",
        author = "Square",
        url = "https://github.com/square/okhttp"
    ),
    LicenseEntry(
        name = "ZXing Android Embedded",
        author = "JourneyApps / ZXing",
        url = "https://github.com/journeyapps/zxing-android-embedded"
    ),
    LicenseEntry(
        name = "Bare / Bare Kit",
        author = "Holepunch",
        license = "Apache-2.0",
        url = "https://github.com/holepunchto/bare-kit"
    ),
    LicenseEntry(
        name = "Hyperswarm",
        author = "Holepunch contributors",
        license = "MIT",
        url = "https://github.com/holepunchto/hyperswarm"
    ),
    LicenseEntry(
        name = "HyperDHT",
        author = "Holepunch contributors",
        license = "MIT",
        url = "https://github.com/holepunchto/hyperdht"
    ),
    LicenseEntry(
        name = "sodium-native",
        author = "Holepunch contributors",
        license = "MIT",
        url = "https://github.com/holepunchto/sodium-native"
    ),
    LicenseEntry(
        name = "udx-native",
        author = "Holepunch contributors",
        license = "Apache-2.0",
        url = "https://github.com/holepunchto/udx-native"
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
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .ncyDottedGrid(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "Ncy is built with these open-source libraries. Tap any to open its project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                )
            }
            item { NcySectionLabel("Libraries") }
            item {
                NcyDividedCard {
                    licenses.forEachIndexed { i, entry ->
                        if (i > 0) NcyRowDivider()
                        LicenseRow(entry) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(14.dp)) }
            item { NcySectionLabel("License") }
            item { ApacheLicensePanel() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LicenseRow(entry: LicenseEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${entry.author} · ${entry.license}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = "Open project page",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ApacheLicensePanel() {
    NcyDividedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Apache License 2.0", style = MaterialTheme.typography.titleSmall)
            Text(
                "Ncy includes software under Apache-2.0, MIT, and ISC licenses. A detailed dependency inventory and the license and notice texts supplied by bundled Pear packages are included in the application asset THIRD_PARTY_NOTICES_PEAR.md.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = """You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.""",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
