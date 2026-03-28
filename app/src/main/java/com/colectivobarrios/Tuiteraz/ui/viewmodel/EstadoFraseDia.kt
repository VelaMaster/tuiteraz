package com.colectivobarrios.Tuiteraz.ui.viewmodel

import com.colectivobarrios.Tuiteraz.Frase

sealed class EstadoFraseDia {
    object CargandoSkeleton : EstadoFraseDia()
    data class MostrarFrase(val frase: Frase) : EstadoFraseDia()
}