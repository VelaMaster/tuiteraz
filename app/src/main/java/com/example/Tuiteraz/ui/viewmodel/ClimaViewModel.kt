package com.example.Tuiteraz.ui.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Tuiteraz.data.network.RedClima
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ClimaViewModel(application: Application) : AndroidViewModel(application) {

    private val _estadoClima = MutableStateFlow(DatosClima(ciudad = "Cargando..."))
    val estadoClima: StateFlow<DatosClima> = _estadoClima.asStateFlow()

    // --- FRENO DE PETICIONES (Ahorro de WiFi y Batería) ---
    // Guardamos cuándo fue la última vez que fuimos a internet
    private var ultimoLlamado: Long = 0
    private val TIEMPO_ESPERA_MS = 10 * 60 * 1000 // 10 Minutos de caché

    fun cargarClima(latitud: Double, longitud: Double, forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()

        // Si no han pasado 10 minutos y no estás tirando para recargar (forzar),
        // abortamos la petición para ahorrar datos y batería.
        if (!forzar && ahora - ultimoLlamado < TIEMPO_ESPERA_MS) {
            return
        }

        viewModelScope.launch {
            try {
                ultimoLlamado = ahora
                _estadoClima.value = _estadoClima.value.copy(huboErrorAlActualizar = false)

                // --- VELOCIDAD x2: EJECUCIÓN EN PARALELO ---
                // En lugar de esperar uno por uno, lanzamos ambas tareas al mismo tiempo
                val respuestaDeferred = async { RedClima.api.obtenerClimaActual(latitud, longitud) }
                val nombreCiudadDeferred = async { obtenerNombreCiudad(latitud, longitud) }

                // Esperamos a que ambas terminen juntas
                val respuesta = respuestaDeferred.await()
                val nombreCiudad = nombreCiudadDeferred.await()

                _estadoClima.value = _estadoClima.value.copy(
                    ciudad = nombreCiudad,
                    temperatura = respuesta.current_weather.temperature.toInt(),
                    descripcion = interpretarCodigoClima(respuesta.current_weather.weathercode),
                    huboErrorAlActualizar = false
                )
            } catch (e: Exception) {
                _estadoClima.value = _estadoClima.value.copy(
                    huboErrorAlActualizar = true,
                    ciudad = "Sin red" // Más claro que poner "Oaxaca" cuando falla el wifi
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

                    val nombre = dir.locality
                        ?: dir.subAdminArea
                        ?: dir.subLocality
                        ?: dir.adminArea
                        ?: "Ubicación actual"

                    limpiarNombre(nombre)
                } else {
                    "Ubicación actual"
                }
            } catch (e: Exception) {
                "Ubicación actual"
            }
        }
    }

    private fun limpiarNombre(nombre: String): String {
        return nombre
            .replace("Municipio de ", "", ignoreCase = true)
            .replace("Heroica Ciudad de ", "", ignoreCase = true)
            .trim()
    }

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

    // <--- AHORA SÍ, LA FUNCIÓN ESTÁ DENTRO DE LA CLASE --->
    fun marcarUbicacionBloqueada() {
        _estadoClima.value = _estadoClima.value.copy(
            ciudad = "Ubicación bloqueada",
            descripcion = "Actívala en ajustes",
            huboErrorAlActualizar = true
        )
    }
}

// <--- LA DATA CLASS ESTÁ AFUERA, COMO DEBE SER --->
data class DatosClima(
    val ciudad: String = "Oaxaca",
    val temperatura: Int = 25,
    val descripcion: String = "--",
    val huboErrorAlActualizar: Boolean = false
)