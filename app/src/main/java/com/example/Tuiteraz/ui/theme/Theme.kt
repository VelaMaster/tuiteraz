// RUTA: app/src/main/java/com/example/balance/ui/theme/Theme.kt
package com.example.Tuiteraz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val EsquemaClaro  = lightColorScheme()
private val EsquemaOscuro = darkColorScheme()

// ─────────────────────────────────────────────────────────────────────────────
// Úsalo en MainActivity:
//   setContent { BalanceTheme { PantallaInicio(frase) } }
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BalanceTheme(
    oscuro: Boolean        = isSystemInDarkTheme(),
    colorDinamico: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val esquema = when {
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && oscuro ->
            dynamicDarkColorScheme(context)
        colorDinamico && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !oscuro ->
            dynamicLightColorScheme(context)
        oscuro -> EsquemaOscuro
        else   -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = esquema,
        content     = content
    )
}