# XX Memory ProGuard Rules

# Keep line number information for crash reporting.
-keepattributes SourceFile,LineNumberTable

# Room entities must be kept.
-keep class com.xxmemory.app.data.entity.** { *; }

# Keep data classes used by Gson/JSON parsing.
-keepclassmembers class com.xxmemory.app.data.entity.** { *; }

# Keep Kotlin metadata and coroutines.
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Compose runtime and compiler metadata.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Keep Coil image loading models.
-keep class coil.** { *; }

# Keep commonmark / opencsv parser internals.
-keep class org.commonmark.** { *; }
-keep class com.opencsv.** { *; }

# Keep TextToSpeech and MediaPlayer related classes.
-keep class android.speech.tts.** { *; }
-keep class android.media.** { *; }
