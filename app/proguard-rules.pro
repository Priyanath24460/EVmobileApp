# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Retain generic signature information for Room
-keepattributes Signature

# Keep Room database classes
-keep class com.evcharging.mobile.database.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Retrofit
-keepattributes *Annotation*
-keep class com.evcharging.mobile.api.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class com.evcharging.mobile.models.** { *; }

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Keep ZXing
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Keep model classes
-keep class com.evcharging.mobile.models.** { *; }