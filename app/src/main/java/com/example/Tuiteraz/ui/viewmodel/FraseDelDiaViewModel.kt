package com.example.Tuiteraz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Tuiteraz.data.network.ProveedorFrasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FraseDelDiaViewModel(
    private val repositorio: ProveedorFrasesRepository
) : ViewModel() {
    private val _estadoUi = MutableStateFlow<EstadoFraseDia>(EstadoFraseDia.CargandoSkeleton)
    val estadoUi: StateFlow<EstadoFraseDia> = _estadoUi.asStateFlow()
    init {
        cargarFraseHoy()
    }
    private fun cargarFraseHoy() {
        viewModelScope.launch {
            _estadoUi.value = EstadoFraseDia.CargandoSkeleton
            val frase = repositorio.obtenerFraseDelDia()
            if (frase != null) {
                _estadoUi.value = EstadoFraseDia.MostrarFrase(frase)
            } else {
                _estadoUi.value = EstadoFraseDia.CargandoSkeleton
            }
        }
    }
}