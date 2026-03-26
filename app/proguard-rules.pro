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