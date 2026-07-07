plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    // KSP replaces kapt for Hilt/Dagger annotation processing — kapt's Java-stub
    // pipeline breaks on Kotlin 2.2 class-file metadata (`kind: null` on stubs),
    // and Google is deprecating kapt in favour of KSP. The KSP version's suffix
    // must match the Kotlin plugin: 2.2.10 → 2.2.10-2.0.2.
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
