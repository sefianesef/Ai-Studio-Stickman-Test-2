# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Google Play Billing Client
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# AndroidX Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Android Jetpack Compose & State
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
