# ============================================================================
# OBSIDIAN BACKUP - PROGUARD/R8 OPTIMIZATION RULES
# ============================================================================
# Comprehensive optimization rules for APK size reduction and performance
# Last updated: 2024
# ============================================================================

# ============================================================================
# GENERAL OPTIMIZATION SETTINGS
# ============================================================================

# Enable aggressive optimization
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for runtime reflection
-keepattributes *Annotation*,Signature,Exception,InnerClasses,EnclosingMethod

# ============================================================================
# KOTLIN SPECIFIC RULES
# ============================================================================

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.obsidianbackup.**$$serializer { *; }
-keepclassmembers class com.obsidianbackup.** {
    *** Companion;
}
-keepclasseswithmembers class com.obsidianbackup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================================
# ANDROID ARCHITECTURE COMPONENTS
# ============================================================================

# Lifecycle
-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAO implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.WorkerParameters

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================================================
# JETPACK COMPOSE
# ============================================================================

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class ** { *; }
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Compose compiler generated classes
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# ============================================================================
# HILT / DAGGER
# ============================================================================

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilderEntryPoint
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory$ViewModelFactoriesEntryPoint

# Keep all Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Factory { *; }
-keep class **_Impl { *; }
-keep class **_MembersInjector { *; }

# Keep Hilt annotations
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Keep injected constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @dagger.assisted.Assisted <fields>;
}

# ============================================================================
# GOOGLE APIs (DRIVE, AUTH)
# ============================================================================

# Google API Client
-keep class com.google.api.client.** { *; }
-keepclassmembers class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# Google Auth
-keep class com.google.auth.** { *; }
-dontwarn com.google.auth.**

# Google Drive API
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.services.drive.**

# Keep Google HTTP client
-keepclassmembers class com.google.api.client.http.** {
    <init>(...);
}
-dontwarn com.google.api.client.http.**

# ============================================================================
# WEBDAV (SARDINE)
# ============================================================================

# Sardine Android
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**
-dontwarn org.apache.http.**
-dontwarn javax.xml.stream.**

# OkHttp (used by Sardine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# ============================================================================
# BIOMETRIC AUTHENTICATION
# ============================================================================

# BiometricPrompt
-keep class androidx.biometric.** { *; }
-keep class androidx.credentials.** { *; }

# ============================================================================
# BILLING
# ============================================================================

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ============================================================================
# SHIZUKU
# ============================================================================

# Shizuku API
-keep class dev.rikka.shizuku.** { *; }
-keepclassmembers class dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**

# ============================================================================
# NAVIGATION
# ============================================================================

# Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.Navigator

# ============================================================================
# LOTTIE ANIMATIONS
# ============================================================================

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ============================================================================
# ACCOMPANIST
# ============================================================================

# Accompanist libraries
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ============================================================================
# MATERIAL DESIGN
# ============================================================================

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ============================================================================
# REFLECTION & SERIALIZATION
# ============================================================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# SECURITY HARDENING (OWASP MASVS-RESILIENCE)
# ============================================================================

# Advanced obfuscation
-repackageclasses 'o'
-flattenpackagehierarchy

# Remove debug information
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}

# Keep security classes for proper functionality
-keep class com.obsidianbackup.security.** { *; }

# Keep encryption classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**

# SQLCipher for encrypted database
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# WebView JavaScript Interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Google Play Services SafetyNet
-keep class com.google.android.gms.safetynet.** { *; }
-keep interface com.google.android.gms.safetynet.** { *; }

# ============================================================================
# APPLICATION SPECIFIC RULES
# ============================================================================

# Keep application class
-keep public class com.obsidianbackup.ObsidianBackupApplication { *; }

# Keep all data classes
-keep class com.obsidianbackup.data.** { *; }
-keepclassmembers class com.obsidianbackup.data.** { *; }

# Keep model classes
-keep class com.obsidianbackup.model.** { *; }
-keepclassmembers class com.obsidianbackup.model.** { *; }

# Keep storage classes (BackupMetadata lives here)
-keep class com.obsidianbackup.storage.** { *; }
-keepclassmembers class com.obsidianbackup.storage.** { *; }

# Keep domain classes
-keep class com.obsidianbackup.domain.** { *; }
-keepclassmembers class com.obsidianbackup.domain.** { *; }

# Keep cloud provider classes
-keep class com.obsidianbackup.cloud.** { *; }
-keepclassmembers class com.obsidianbackup.cloud.** { *; }

# Keep plugin classes
-keep class com.obsidianbackup.plugins.** { *; }
-keepclassmembers class com.obsidianbackup.plugins.** { *; }

# Keep gaming integration classes
-keep class com.obsidianbackup.gaming.** { *; }
-keepclassmembers class com.obsidianbackup.gaming.** { *; }

# Keep health integration classes
-keep class com.obsidianbackup.health.** { *; }
-keepclassmembers class com.obsidianbackup.health.** { *; }

# Keep database entities
-keep @androidx.room.Entity class * { *; }

# ============================================================================
# OPTIMIZATION CONTROL
# ============================================================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove Timber logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ============================================================================
# WARNINGS TO IGNORE
# ============================================================================

# Ignore warnings for missing classes that are not actually used
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn aQute.bnd.annotation.**
-dontwarn io.micrometer.**
-dontwarn org.codehaus.stax2.**
-dontwarn reactor.blockhound.**

# ============================================================================
# XML PARSER RULES (Fix for XmlResourceParser issue)
# ============================================================================

# Fix R8 classpath issue - tell R8 that XmlPullParser is a library class, not program class
-dontwarn org.xmlpull.**
-dontwarn android.content.res.XmlResourceParser

# Keep XmlPullParser and related classes if they exist
-keep,allowobfuscation class org.xmlpull.v1.** { *; }
-keepclassmembers class * implements org.xmlpull.v1.XmlPullParser {
    *;
}

# If XmlPullParser is found as a program class, ignore it
-dontnote org.xmlpull.v1.**

# ============================================================================
# RETROFIT & NETWORKING
# ============================================================================

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Moshi (if used)
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers class * {
  @com.squareup.moshi.FromJson <methods>;
  @com.squareup.moshi.ToJson <methods>;
}

# ============================================================================
# DEBUG INFORMATION
# ============================================================================

# Print configuration details (useful for debugging ProGuard issues)
# Uncomment these for troubleshooting
# -printconfiguration proguard-config.txt
# -printusage proguard-usage.txt
# -printseeds proguard-seeds.txt
# -printmapping proguard-mapping.txt
# ============================================================================
# TASKER/MACRODROID INTEGRATION
# ============================================================================

# Keep Tasker integration components - needed for external automation apps
-keep class com.obsidianbackup.tasker.TaskerIntegration { *; }
-keep class com.obsidianbackup.tasker.TaskerStatusProvider { *; }
-keep class com.obsidianbackup.tasker.TaskerEventPublisher { *; }

# Keep security validator - needed for package authorization
-keep class com.obsidianbackup.security.TaskerSecurityValidator { *; }
-keep class com.obsidianbackup.security.SecuritySummary { *; }

# Keep plugin action/condition models - used by Tasker plugin system
-keep class com.obsidianbackup.tasker.plugin.** { *; }

# Keep workers - needed by WorkManager
-keep class com.obsidianbackup.tasker.RestoreWorker { *; }
-keep class com.obsidianbackup.tasker.VerifyWorker { *; }
-keep class com.obsidianbackup.tasker.DeleteWorker { *; }

# Keep all intent action/extra constants
-keepclassmembers class com.obsidianbackup.tasker.TaskerIntegration {
    public static final java.lang.String ACTION_*;
    public static final java.lang.String EVENT_*;
    public static final java.lang.String EXTRA_*;
    public static final int RESULT_*;
}

# Keep ContentProvider column names
-keepclassmembers class com.obsidianbackup.tasker.TaskerStatusProvider {
    public static final java.lang.String[] *_COLUMNS;
}

# ============================================================================
# SLF4J RULES
# ============================================================================

# SLF4J is optional - don't warn about missing implementation
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**

# ============================================================================
# SQLCipher Rules
# SOURCE: https://github.com/sqlcipher/android-database-sqlcipher#proguard
# ============================================================================
-keep,includedescriptorclasses class net.zetetic.** { *; }
-keep,includedescriptorclasses interface net.zetetic.** { *; }

# ============================================================================
# TensorFlow Lite GPU Delegate — optional dependency, suppress missing classes
# ============================================================================
-dontwarn org.tensorflow.lite.gpu.**
-keep class org.tensorflow.lite.** { *; }
