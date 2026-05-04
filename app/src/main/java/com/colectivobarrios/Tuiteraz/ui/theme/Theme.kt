package com.colectivobarrios.Tuiteraz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EsquemaClaro  = lightColorScheme()
private val EsquemaOscuro = darkColorScheme()

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

    // ✅ Esto reemplaza las APIs obsoletas
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }

    MaterialTheme(
        colorScheme = esquema,
        content     = content
    )
}