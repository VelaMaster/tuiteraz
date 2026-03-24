plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.22"
}
android {
    namespace   = "com.example.Tuiteraz"
    compileSdk  = 36

    defaultConfig {
        applicationId             = "com.example.Tuiteraz"
        minSdk                    = 28
        targetSdk                 = 36
        versionCode               = 1
        versionName               = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures { compose = true }
    buildToolsVersion = "36.0.0"
}
dependencies {
    // Volvemos a tu BOM original para no causar otros problemas en tu app
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Tu Material 3 Expressive
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // LA VERSIÓN SALVAVIDAS DEL CALENDARIO:
    implementation("com.kizitonwose.calendar:compose:2.10.0")
    implementation(libs.androidx.compose.foundation.layout)
// --- SUPABASE ---
    // Versión estable para Kotlin
    val supabaseVersion = "2.2.3"
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion") // Autenticación
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion") // Base de datos
    implementation("io.github.jan-tennert.supabase:compose-auth:$supabaseVersion") // UI Auth Compose
    implementation("io.ktor:ktor-client-android:2.3.9") // Motor de red que usa Supabase

    // --- GOOGLE SIGN-IN (Credential Manager) ---
    implementation("androidx.credentials:credentials:1.2.1")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.1")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    // Red y JSON (Retrofit + Gson)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Para trabajos en segundo plano (Notificaciones diarias)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
// Para manejar permisos en Compose fácilmente
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
// Servicios de Ubicación
    implementation("com.google.android.gms:play-services-location:21.2.0")

// ViewModel para Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}