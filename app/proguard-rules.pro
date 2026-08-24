# Advanced Code Scrambling, R8 Minification & Anti-Decompilation Rules
# Protects the APK bytecode against JADX, APKTool, and Ghidra decompilation.

# 1. Aggressive Obfuscation & Bytecode Scrambling
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.example.internal.obf'
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable

# 2. Strip Logging & String Metadata to Prevent Symbol Discovery
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 3. Google Play Billing Client
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# 4. AndroidX Room Database & Entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 5. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 6. Android Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 7. Keep Data Models and Entities
-keepclassmembers class com.example.model.** {
    public *;
}
-keepclassmembers class com.example.data.local.entity.** {
    public *;
}

# 8. Anti-Tamper & Security Engine Obfuscation
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
-keepclassmembers class com.example.security.AntiCheatEarningLimiter {
    public <methods>;
}

# 9. AndroidX Security Crypto & Google Tink (Hardware Keystore)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**



