plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.obsidianbackup.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.obsidianbackup.tv"
        minSdk = 26  // Matching root-core requirements
        targetSdk = 35  // Matching main app for consistency
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Leanback for Android TV (stable version)
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.leanback:leanback-preference:1.0.0")
    
    // TV Provider for recommendations
    implementation("androidx.tvprovider:tvprovider:1.0.0")

    // Material Design (for some components)
    implementation("com.google.android.material:material:1.12.0")

    // ConstraintLayout for TV layouts
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView for TV cards
    implementation("androidx.cardview:cardview:1.0.0")

    // Navigation for TV
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
