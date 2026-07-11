plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ncy.notechaty"
    // Bumped to 35 alongside the compose-bom 2025.02.00 pass (2026-07-08) —
    // androidx.core 1.15.0+ requires compileSdk 35, and Play requires
    // targetSdk 35 for new submissions since late 2025.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ncy.notechaty"
        minSdk = 31
        targetSdk = 35
        versionCode = 8
        versionName = "0.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Opt into the Kotlin 2.3+ default: `@Foo` on a constructor param that
        // is also a `val`/`var` applies to both the parameter AND the field.
        // Silences the "will also be applied to field" preview warnings on
        // @Inject/@ApplicationContext constructors across the codebase — Hilt
        // treats them identically. See https://youtrack.jetbrains.com/issue/KT-73255.
        freeCompilerArgs += "-Xannotation-default-target=param-property"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core — bumped 2026-07-08 alongside compose-bom so runtime/animation/material3
    // versions all move in lockstep and the version-skew crashes (see
    // feedback_android_patterns.md re CircularProgressIndicator) stop happening.
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ViewModel + lifecycle-compose (the latter provides the modern
    // `LocalLifecycleOwner` — the old one in compose.ui.platform is deprecated).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt — annotation processing goes through KSP (see the `ksp` plugin above)
    // because kapt's Java-stub pipeline is fragile on Kotlin 2.2 (produces
    // stubs with unreadable `@Metadata(kind = null)` that Dagger can't parse).
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Sync (Phase 1: WebSocket client + QR scanning)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WorkManager (auto-backup)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Biometric / PIN
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
