plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.compose.compiler)  // Kotlin 2.0+ Compose compiler
}

android {
    namespace = "com.obsidianbackup.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.obsidianbackup.wear"
        minSdk = 30  // Wear OS 3.0+
        targetSdk = 35  // Matching main app for consistency
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Kotlin 2.0+ bundles Compose compiler - no version needed
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildToolsVersion = "35.0.0"
}

dependencies {
    // Root-core module
    implementation(project(":root-core"))

    // Wear OS
    implementation("androidx.wear:wear:1.3.0")
    compileOnly("com.google.android.wearable:wearable:2.9.0")

    // Wear Compose (updated to latest with Material3)
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha27")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Wear Tiles (updated)
    implementation("androidx.wear.tiles:tiles:1.4.0")
    implementation("androidx.wear.tiles:tiles-material:1.4.0")

    // Wear Complications
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.activity:activity-compose:1.8.2")

    // Data Layer for phone-watch communication
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Hilt for DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (for background tasks)
    implementation(libs.androidx.work.runtime.ktx)

    // Horologist (Wear OS best practices library - updated)
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.23")
    implementation("com.google.android.horologist:horologist-compose-material:0.6.23")

    // Testing
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation("androidx.compose.ui:ui-tooling")
}
