# Wear OS ProGuard rules
-keep class com.obsidianbackup.wear.** { *; }

# Keep Wear Tiles
-keep class androidx.wear.tiles.** { *; }
-keep class androidx.wear.watchface.complications.** { *; }

# Keep Data Layer classes
-keep class com.google.android.gms.wearable.** { *; }

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
