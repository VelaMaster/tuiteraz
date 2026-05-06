package com.colectivobarrios.Tuiteraz.ui.componentes

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// 1. Definimos la clase que contendrá los 3 colores que pide tu diseño
data class ColoresPrioridad(
    val contenedor: Color,
    val onContenedor: Color,
    val acento: Color
)

// 2. Creamos la función que evalúa la prioridad y devuelve los colores correspondientes
fun coloresParaPrioridad(prioridad: String, colorScheme: ColorScheme): ColoresPrioridad {
    return when (prioridad.lowercase()) {
        "alta" -> ColoresPrioridad(
            contenedor = colorScheme.errorContainer,
            onContenedor = colorScheme.onErrorContainer,
            acento = colorScheme.error
        )
        "baja" -> ColoresPrioridad(
            contenedor = colorScheme.secondaryContainer,
            onContenedor = colorScheme.onSecondaryContainer,
            acento = colorScheme.secondary
        )
        else -> ColoresPrioridad( // "media" o cualquier otro valor por defecto
            contenedor = colorScheme.primaryContainer,
            onContenedor = colorScheme.onPrimaryContainer,
            acento = colorScheme.primary
        )
    }
}