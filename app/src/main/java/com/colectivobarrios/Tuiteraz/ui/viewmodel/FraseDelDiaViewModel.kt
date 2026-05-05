package com.colectivobarrios.Tuiteraz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colectivobarrios.Tuiteraz.data.network.ProveedorFrasesRepository
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
            try {
                val frase = repositorio.obtenerFraseDelDia()
                if (frase != null) {
                    _estadoUi.value = EstadoFraseDia.MostrarFrase(frase)
                } else {
                    // Sin internet y sin caché → quedamos en skeleton sin crashear
                    _estadoUi.value = EstadoFraseDia.CargandoSkeleton
                }
            } catch (e: Exception) {
                // Nunca crashear por falta de red
                _estadoUi.value = EstadoFraseDia.CargandoSkeleton
            }
        }
    }
}