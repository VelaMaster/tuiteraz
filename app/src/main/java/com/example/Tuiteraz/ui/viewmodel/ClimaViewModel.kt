package com.example.Tuiteraz.ui.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Tuiteraz.data.network.RedClima
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// IMPORTANTE: Cambiamos a AndroidViewModel para tener contexto y usar Geocoder
class ClimaViewModel(application: Application) : AndroidViewModel(application) {

    private val _estadoClima = MutableStateFlow(DatosClima(ciudad = "Buscando...")) // Empezamos buscando
    val estadoClima: StateFlow<DatosClima> = _estadoClima.asStateFlow()

    fun cargarClima(latitud: Double, longitud: Double) {
        viewModelScope.launch {
            try {
                _estadoClima.value = _estadoClima.value.copy(huboErrorAlActualizar = false)

                val respuesta = RedClima.api.obtenerClimaActual(latitud, longitud)

                // Intentamos obtener el nombre del pueblo/ciudad
                val nombreCiudad = obtenerNombreCiudad(latitud, longitud)

                _estadoClima.value = _estadoClima.value.copy(
                    ciudad = nombreCiudad,
                    temperatura = respuesta.current_weather.temperature.toInt(),
                    descripcion = interpretarCodigoClima(respuesta.current_weather.weathercode),
                    huboErrorAlActualizar = false
                )
            } catch (e: Exception) {
                _estadoClima.value = _estadoClima.value.copy(
                    huboErrorAlActualizar = true,
                    ciudad = "Desconocido" // Si falla todo
                )
            }
        }
    }

    private suspend fun obtenerNombreCiudad(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val direcciones = geocoder.getFromLocation(lat, lon, 1)

                if (!direcciones.isNullOrEmpty()) {
                    val dir = direcciones[0]

                    // Nuevo orden estratégico para México (Oaxaca/Etla)
                    dir.subLocality          // 1. Barrio / Colonia (Ej. Capellanía)
                        ?: dir.subAdminArea  // 2. Municipio (Ej. Villa de Etla)
                        ?: dir.locality      // 3. Ciudad (Ej. Oaxaca)
                        ?: dir.adminArea     // 4. Estado
                        ?: "Ubicación actual"
                } else {
                    "Ubicación actual"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Ubicación actual"
            }
        }
    }

    // ... (Tu función interpretarCodigoClima se queda igual) ...
    private fun interpretarCodigoClima(codigo: Int): String {
        return when (codigo) {
            0 -> "Cielo despejado"
            1, 2, 3 -> "Parcialmente nublado"
            45, 48 -> "Niebla"
            51, 53, 55 -> "Llovizna"
            61, 63, 65 -> "Lluvia"
            71, 73, 75 -> "Nieve"
            95, 96, 99 -> "Tormenta"
            else -> "Clima variable"
        }
    }
}

// El data class se queda igual
data class DatosClima(
    val ciudad: String = "Oaxaca",
    val temperatura: Int = 25,
    val descripcion: String = "--",
    val huboErrorAlActualizar: Boolean = false
)