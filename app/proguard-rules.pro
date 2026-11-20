# ====================================================================
# TravelVista - R8 / ProGuard Optimization & Compatibility Rules
# ====================================================================

# Keep generic signatures and runtime annotations for Gson and Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- GSON DTO Models & Data Entities ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.snj.travelvista.data.model.** { *; }
-keep class com.snj.travelvista.data.local.entity.** { *; }
-keep class com.snj.travelvista.data.remote.dto.** { *; }

# --- RETROFIT & OKHTTP ---
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class com.snj.travelvista.data.remote.WikivoyageApiService { *; }
-keep class com.snj.travelvista.data.remote.TravelApiService { *; }
-keep class com.snj.travelvista.data.remote.service.** { *; }
-keep class com.snj.travelvista.data.remote.provider.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- OSMDROID OPENSTREETMAP ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --- ROOM DATABASE ---
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# --- GLIDE IMAGE LOADING ---
-keep public class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep class com.bumptech.glide.** { *; }
-keep public enum com.bumptech.glide.load.resource.bitmap.DownsampleStrategy { *; }
-dontwarn com.bumptech.glide.**

# --- COROUTINES ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- MATERIAL COMPONENTS & NAVIGATION ---
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
