package com.example.Tuiteraz.ui.viewmodel

import com.example.Tuiteraz.Frase

sealed class EstadoFraseDia {
    object CargandoSkeleton : EstadoFraseDia()
    data class MostrarFrase(val frase: Frase) : EstadoFraseDia()
}