-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Reglas generales para Ktor y Supabase
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Retrofit (por si acaso)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Serialization de Kotlin
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }

-keepattributes *Annotation*, InnerClasses, Signature
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# 2. Proteger Supabase y Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# 3. Proteger Retrofit y Gson
-dontwarn sun.misc.Unsafe
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. Proteger Jetpack Glance
-keep class androidx.glance.** { *; }

# 5. Proteger el inicio de sesión de Google
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }



# 6. Proteger AndroidX Room y WorkManager (Bases de datos locales)
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**

-keepattributes Signature
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowobfuscation,allowshrinking class <3>
-keep class com.colectivobarrios.Tuiteraz.data.network.** { *; }