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

# Keep Data Models and Entities with Serializable fields
-keepclassmembers class com.example.model.** {
    public *;
}
-keepclassmembers class com.example.data.local.entity.** {
    public *;
}

# Obfuscate and protect Security Vault & Anti-Cheat Engine
-keepclassmembers class com.example.security.SecureCurrencyVault {
    public <methods>;
}
-keepclassmembers class com.example.security.PurchaseVerificationService {
    public <methods>;
}
-keepclassmembers class com.example.security.DeviceIntegrityManager {
    public <methods>;
}
-keepclassmembers class com.example.security.ObfuscatedInt {
    public <methods>;
}
-keepclassmembers class com.example.security.SecureTimeAuthority {
    public <methods>;
}
-keepclassmembers class com.example.security.EncryptedSaveStorage {
    public <methods>;
}
-keepclassmembers class com.example.security.AdServerSideVerificationManager {
    public <methods>;
}
-keepclassmembers class com.example.security.ServerCurrencyAuthority {
    public <methods>;
}

# Strip Android Log calls in release builds to prevent symbol leakage
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

