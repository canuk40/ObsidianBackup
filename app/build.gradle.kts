plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.compose.compiler)  // Kotlin 2.0+ Compose compiler
    id("org.jetbrains.dokka")
    id("jacoco")
}

import java.util.Properties
import java.io.FileInputStream

// Load keystore properties from project root
val keystorePropertiesFile = file("${rootProject.projectDir}/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

// Load local.properties (SDK path, API keys, license key — never committed to Git)
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.obsidianbackup"
    compileSdk = 35  // REQUIRED by Google Play Console as of Feb 2026

    defaultConfig {
        applicationId = "com.obsidianbackup"
        minSdk = 26
        targetSdk = 35  // REQUIRED by Google Play Console as of Feb 2026
        versionCode = 8
        versionName = "1.0.8"

        testInstrumentationRunner = "com.obsidianbackup.testing.HiltTestRunner"
        
        // Vector drawable support for older devices
        vectorDrawables.useSupportLibrary = true
        
        // Enable multidex for better method count management
        multiDexEnabled = true
        
        // Native library filters - only include needed ABIs
        ndk {
            // Include only the most common architectures to reduce APK size
            // x86_64 is added for emulator testing in debug builds
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
        
        // Test options
        testOptions.unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        // M-1: Enable Room schema export for MigrationTest
        // SOURCE: https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }

        // Cloud provider credentials — set in local.properties, never commit
        buildConfigField("String", "B2_KEY_ID",
            "\"${localProperties.getProperty("b2.keyId", "")}\"")
        buildConfigField("String", "B2_APPLICATION_KEY",
            "\"${localProperties.getProperty("b2.applicationKey", "")}\"")
    }

    // ============================================================================
    // SIGNING CONFIGS
    // ============================================================================
    
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file("${rootProject.projectDir}/obsidianbackup.jks")
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            } else {
                // Missing keystore — throw immediately for release tasks so the failure
                // is loud and obvious rather than silently producing an unsigned APK.
                if (gradle.startParameter.taskNames.any {
                    it.contains("release", ignoreCase = true)
                }) {
                    throw GradleException(
                        "Keystore not found at ${keystorePropertiesFile.absolutePath}. " +
                        "Cannot build release APK without signing config. " +
                        "Place keystore.jks at the expected path and configure keystore.properties."
                    )
                }
            }
        }
    }

    // ============================================================================
    // BUILD TYPES - OPTIMIZED FOR SIZE AND PERFORMANCE
    // ============================================================================
    
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            
            // Keep debug builds fast - disable optimizations
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            
            // Use application ID suffix to allow debug/release coexistence
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"

        }

        release {
            // Use release signing config
            signingConfig = signingConfigs.getByName("release")
            
            // Enable R8/ProGuard code shrinking and obfuscation
            isMinifyEnabled = true
            
            // Enable resource shrinking to remove unused resources
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Optimize PNG files
            isCrunchPngs = true
            
            // Disable debugging for release
            isDebuggable = false
            isJniDebuggable = false
            
            // Render script optimization
            renderscriptOptimLevel = 3

        }

        // Benchmark build type for performance testing
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks.add("release")
        }
    }
    
    // ============================================================================
    // APP BUNDLE & SPLIT APK CONFIGURATION
    // ============================================================================
    
    bundle {
        // Language resource splitting
        language {
            enableSplit = true
        }
        
        // Density resource splitting
        density {
            enableSplit = true
        }
        
        // ABI splitting
        abi {
            enableSplit = true
        }
    }
    
    // Split APK configuration for density and ABI
    // Disabled: produces 1 universal APK per variant. Play Store optimization
    // is handled by the bundle {} block above when publishing AABs.
    // To re-enable for release APKs: ./gradlew assembleRelease -PenableSplits=true
    splits {
        density {
            isEnable = (project.findProperty("enableSplits") == "true")
            exclude("ldpi", "tvdpi", "xxxhdpi")
            compatibleScreens("small", "normal", "large", "xlarge")
        }
        abi {
            isEnable = (project.findProperty("enableSplits") == "true")
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
                it.testLogging {
                    events("passed", "skipped", "failed")
                }
            }
        }
        animationsDisabled = true
        
        // Execution options
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    // M-1: Include Room schemas as androidTest assets for MigrationTestHelper
    // SOURCE: https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
    
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/INDEX.LIST",
                "**/*.proto",
                "THIRD_PARTY_LICENSES.txt"
            )
            // Merge duplicate files
            pickFirsts += setOf(
                "META-INF/io.netty.versions.properties",
                "META-INF/INDEX.LIST"
            )
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for feature flags
        
        // Disable unused features to speed up builds
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }
    
    composeOptions {
        // Kotlin 2.0+ bundles Compose compiler - no version needed
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        // Enable Java 8+ API desugaring for older devices
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
        
        // Kotlin compiler optimizations
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xjvm-default=all" // Generate default methods in interfaces
        )
    }
    
    buildToolsVersion = "35.0.0"  // Required for SDK 35

    // Lint options for faster builds
    lint {
        checkReleaseBuilds = true
        abortOnError = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
        checkDependencies = true
    }
}


// Lock dependency versions for reproducible builds
dependencyLocking {
    lockAllConfigurations()
}

// ============================================================================
// DEPENDENCY CONFIGURATIONS
// ============================================================================

configurations.all {
    // Exclude duplicate/conflicting dependencies
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    
    resolutionStrategy {
        // Force consistent versions (Kotlin 2.0.21)
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
        force("com.squareup.okhttp3:okhttp:4.12.0")
        force("com.squareup.okio:okio:3.9.0")
        // Removed forced javapoet - let Hilt use its natural transitive version
        force("androidx.annotation:annotation:1.9.0")
        
        // Cache dynamic versions for 24 hours
        cacheDynamicVersionsFor(24, "hours")
        cacheChangingModulesFor(24, "hours")
    }
}

dependencies {
    // Root-core module (production-tested root infrastructure from ObsidianBox v31)
    implementation(project(":root-core"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Java 8+ API desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Explicitly add compose runtime - workaround for inline method resolution
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-saveable")

    // Material Components (styles used in themes.xml)
    implementation("com.google.android.material:material:1.12.0")

    // Biometric Authentication (stable version)
    implementation("androidx.biometric:biometric:1.1.0")
    
    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Timber logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Credentials Manager for Passkeys (Android 14+)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // HTTP Client for web3.storage API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // NanoHTTPD for WiFi Direct migration server
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    
    // SQLCipher for database encryption
    // SOURCE: https://github.com/sqlcipher/sqlcipher-android
    // NOTE: android-database-sqlcipher is DEPRECATED. Use sqlcipher-android.
    implementation("net.zetetic:sqlcipher-android:4.6.1") {
        exclude(group = "androidx.sqlite", module = "sqlite")
    }
    implementation("androidx.sqlite:sqlite:2.4.0")
    
    // H-7, M-8: JWT / JWS signature verification (Nimbus JOSE+JWT — Android-compatible)
    // SOURCE: https://connect2id.com/products/nimbus-jose-jwt
    implementation("com.nimbusds:nimbus-jose-jwt:9.40")

    // Security Crypto library for encrypted SharedPreferences (alpha version for MasterKey support)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Certificate transparency checking (optional)
    implementation("com.squareup.okhttp3:okhttp-tls:4.12.0")

    // Google Drive API
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.auth.library.oauth2.http)

    // WebDAV (Sardine-Android)
    implementation(libs.sardine.android) {
        // Exclude xpp3 as Android SDK already provides XmlPullParser
        exclude(group = "xpp3", module = "xpp3")
        // Exclude stax as it conflicts with Android XML APIs
        exclude(group = "stax", module = "stax-api")
        exclude(group = "stax", module = "stax")
    }

    // Cloud Provider SDKs
    // Box SDK (exclude conflicting OkHttp) - temporarily disabled due to availability
    // implementation("com.box:box-android-sdk:5.1.0") {
    //     exclude(group = "com.squareup.okhttp3")
    //     exclude(group = "com.squareup.okio")
    // }
    // TODO: Re-enable Box SDK when available or find alternative repository
    
    // Azure Storage SDK
    implementation("com.azure:azure-storage-blob:12.23.0")
    implementation("com.azure:azure-identity:1.10.0")
    
    // Backblaze B2 (using S3-compatible client)
    implementation("software.amazon.awssdk:s3:2.20.0")
    
    // Alibaba Cloud OSS SDK
    implementation("com.aliyun.dpa:oss-android-sdk:2.9.13")
    
    // DigitalOcean Spaces (S3-compatible)
    implementation("com.amazonaws:aws-android-sdk-s3:2.73.0")
    
    // Oracle Cloud Infrastructure SDK
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage:3.27.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.27.0")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    // Hilt Navigation Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Hilt WorkManager integration
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    
    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest(libs.hilt.compiler)
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Ktor HTTP client and server
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    
    // QR Code generation and scanning (ZXing)
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // ML Kit and TensorFlow Lite for AI/ML features
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:language-id:17.0.4")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    
    // Lottie animations
    implementation(libs.lottie.compose)
    
    // Accompanist for advanced UI features
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.navigation.animation)
    
    // No Firebase dependencies — fully FOSS build

    // Image Loading and Caching - Coil for efficient image handling
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Memory Leak Detection - LeakCanary (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    
    // HTTP/2 and Network Optimization
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Performance Monitoring
    implementation("androidx.metrics:metrics-performance:1.0.0-alpha04")
    
    // Wear OS Data Layer (for phone-watch communication)
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Google Sign-In (OAuth2Manager)
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google Play Services Location + Activity Recognition (ContextAwareManager)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coroutines adapter for Play Services Tasks (kotlinx.coroutines.tasks.await)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Testing
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.1") // For JUnit 4 compatibility
    
    // MockK
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    
    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    // Truth for assertions
    testImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("com.google.truth:truth:1.1.5")
    
    // Turbine for Flow testing
    testImplementation("app.cash.turbine:turbine:1.0.0")
    
    // Room Testing — M-1: Database migration tests
    // SOURCE: https://developer.android.com/training/data-storage/room/migrating-db-versions
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    
    // WorkManager Testing
    testImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    
    // Espresso & UI Testing
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.5.1")
    
    // AndroidX Test
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    
    // Compose Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // Navigation Testing
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.6")
    
    // Robolectric for unit tests
    testImplementation("org.robolectric:robolectric:4.11.1")
    
    // Faker for test data
    testImplementation("io.github.serpro69:kotlin-faker:1.15.0")
    
    // OkHttp MockWebServer
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // Fragment Testing
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
    
    // JaCoCo
    testImplementation("org.jacoco:org.jacoco.core:0.8.11")
}

// ============================================================================
// KOTLIN COMPILATION CONFIGURATION
// ============================================================================

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        
        // Additional compiler arguments for optimization
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

// ============================================================================
// JACOCO CONFIGURATION
// ============================================================================

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generate Jacoco coverage reports"
    
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/databinding/*",
        "**/android/databinding/*",
        "**/androidx/databinding/*",
        "**/*DataBinding*.*",
        "**/*DataBinderMapper*.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector*.*",
        "**/Hilt_*.*",
        "**/*_HiltModules*.*",
        "**/*_ComponentTreeDeps*.*",
        "**/*Module_*.*",
        "**/*Dagger*.*",
        "**/*_Impl*.*",
        "**/*\$inlined$*.*"
    )
    
    val javaTree = fileTree("${project.layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val kotlinTree = fileTree("${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(javaTree, kotlinTree))
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile) {
        include("**/*.exec", "**/*.ec")
    })
}

tasks.register<JacocoReport>("jacocoFullReport") {
    group = "verification"
    description = "Generate full Jacoco coverage reports including Android tests"
    
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
    
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    classDirectories.setFrom(fileTree("${project.layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug"))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get().asFile) {
        include("**/*.exec", "**/*.ec")
    })
}

// ============================================================================
// BUILD OPTIMIZATION TASKS
// ============================================================================

// Task to analyze APK size
tasks.register("analyzeApkSize") {
    group = "verification"
    description = "Analyze APK size and generate report"
    
    doLast {
        println("APK Size Analysis:")
        println("==================")
        fileTree("${project.layout.buildDirectory.get().asFile}/outputs/apk/release") {
            include("*.apk")
        }.forEach { apk ->
            val size = apk.length() / (1024 * 1024.0)
            println("${apk.name}: ${String.format("%.2f", size)} MB")
        }
    }
}

// Task to print dependency tree for analysis
tasks.register("analyzeDependencies") {
    group = "help"
    description = "Analyze and print dependency tree"
    
    doLast {
        println("Run: ./gradlew app:dependencies --configuration releaseRuntimeClasspath")
    }
}


// Dokka configuration for API documentation
tasks.register("dokkaHtmlCustom") {
    group = "documentation"
    description = "Generate API documentation with Dokka"
    
    doLast {
        println("Run: ./gradlew dokkaHtml")
        println("Output will be in: app/build/dokka/html/")
    }
}
