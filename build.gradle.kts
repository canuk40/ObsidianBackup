// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
    id("org.jetbrains.dokka") version "1.9.10"
}

buildscript {
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}

detekt {
    config.setFrom(files("config/detekt.yml"))
    buildUponDefaultConfig = true
}

// Task to clean all build outputs
tasks.register("cleanAll") {
    group = "build"
    description = "Clean all build outputs including cache"
    
    doLast {
        delete(rootProject.layout.buildDirectory)
        subprojects.forEach { delete(it.layout.buildDirectory) }
        println("✓ Cleaned all build outputs")
    }
}

// Task to display build optimization info
tasks.register("buildInfo") {
    group = "help"
    description = "Display build configuration and optimization info"
    
    doLast {
        println("""
        ╔════════════════════════════════════════════════════════════╗
        ║          OBSIDIAN BACKUP - BUILD CONFIGURATION             ║
        ╚════════════════════════════════════════════════════════════╝
        
        Build Optimizations:
        • Parallel Execution: ENABLED
        • Configuration Cache: ENABLED
        • Build Cache: ENABLED
        • Gradle Daemon: ENABLED
        • Incremental Compilation: ENABLED
        
        APK Optimizations:
        • R8 Code Shrinking: ENABLED (release)
        • Resource Shrinking: ENABLED (release)
        • Split APKs: ENABLED (density, ABI)
        • PNG Optimization: ENABLED
        
        Memory Configuration:
        • JVM Max Heap: 4096MB
        • Max Metaspace: 1024MB
        
        Build Variants:
        • debug
        • release
        • benchmark
        
        Useful Commands:
        • ./gradlew assembleRelease - Build release APK
        • ./gradlew bundleRelease - Build app bundle
        • ./gradlew analyzeApkSize - Analyze APK size
        • ./gradlew cleanAll - Clean all build outputs
        """.trimIndent())
    }
}