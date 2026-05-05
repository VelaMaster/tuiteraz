package com.colectivobarrios.Tuiteraz.ui.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.colectivobarrios.Tuiteraz.data.network.RedClima
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// DataStore para persistir el último clima conocido
private val Application.climaDataStore by preferencesDataStore(name = "clima_cache")

private object ClaveClima {
    val CIUDAD      = stringPreferencesKey("ciudad")
    val TEMPERATURA = intPreferencesKey("temperatura")
    val DESCRIPCION = stringPreferencesKey("descripcion")
    val HAY_CACHE   = booleanPreferencesKey("hay_cache")
}

class ClimaViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.climaDataStore

    private val _estadoClima = MutableStateFlow(DatosClima(ciudad = "Cargando..."))
    val estadoClima: StateFlow<DatosClima> = _estadoClima.asStateFlow()

    private var ultimoLlamado: Long = 0
    private val TIEMPO_ESPERA_MS = 10 * 60 * 1000L

    init {
        // Al arrancar, cargamos el caché inmediatamente para que la UI
        // nunca quede vacía mientras espera la red
        viewModelScope.launch {
            cargarCacheLocal()
        }
    }

    // Lee el último clima guardado en disco
    private suspend fun cargarCacheLocal() {
        try {
            val prefs = dataStore.data.first()
            val hayCache = prefs[ClaveClima.HAY_CACHE] ?: false
            if (hayCache) {
                _estadoClima.value = DatosClima(
                    ciudad      = prefs[ClaveClima.CIUDAD]      ?: "Última ubicación",
                    temperatura = prefs[ClaveClima.TEMPERATURA] ?: 25,
                    descripcion = prefs[ClaveClima.DESCRIPCION] ?: "--",
                    desdCache   = true  // Para mostrar el indicador "Última actualización"
                )
            }
        } catch (e: Exception) {
            // Si DataStore falla, simplemente no hay caché — no crashea
        }
    }

    // Guarda el clima exitoso en disco
    private suspend fun guardarCacheLocal(ciudad: String, temperatura: Int, descripcion: String) {
        try {
            dataStore.edit { prefs ->
                prefs[ClaveClima.CIUDAD]      = ciudad
                prefs[ClaveClima.TEMPERATURA] = temperatura
                prefs[ClaveClima.DESCRIPCION] = descripcion
                prefs[ClaveClima.HAY_CACHE]   = true
            }
        } catch (e: Exception) {
            // Fallo silencioso — el caché es opcional
        }
    }

    fun cargarClima(latitud: Double, longitud: Double, forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()

        if (!forzar && ahora - ultimoLlamado < TIEMPO_ESPERA_MS) return

        viewModelScope.launch {
            try {
                ultimoLlamado = ahora

                // Mostramos "Actualizando..." solo si ya había caché
                // Para que no parpadee en blanco
                if (_estadoClima.value.desdCache) {
                    _estadoClima.value = _estadoClima.value.copy(
                        huboErrorAlActualizar = false,
                        actualizando = true
                    )
                }

                val respuestaDeferred   = async { RedClima.api.obtenerClimaActual(latitud, longitud) }
                val nombreCiudadDeferred = async { obtenerNombreCiudad(latitud, longitud) }

                val respuesta    = respuestaDeferred.await()
                val nombreCiudad = nombreCiudadDeferred.await()
                val descripcion  = interpretarCodigoClima(respuesta.current_weather.weathercode)
                val temperatura  = respuesta.current_weather.temperature.toInt()

                // Guardamos en disco antes de actualizar la UI
                guardarCacheLocal(nombreCiudad, temperatura, descripcion)

                _estadoClima.value = DatosClima(
                    ciudad                = nombreCiudad,
                    temperatura           = temperatura,
                    descripcion           = descripcion,
                    huboErrorAlActualizar = false,
                    desdCache             = false,
                    actualizando          = false
                )

            } catch (e: Exception) {
                // Sin internet o timeout: mostramos el caché con indicador de error
                // NO crasheamos — solo actualizamos la bandera
                _estadoClima.value = _estadoClima.value.copy(
                    huboErrorAlActualizar = true,
                    actualizando          = false
                    // ciudad/temperatura/descripcion se quedan como estaban (el caché)
                )
            }
        }
    }

    private suspend fun obtenerNombreCiudad(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder   = Geocoder(getApplication(), Locale.getDefault())
                val direcciones = geocoder.getFromLocation(lat, lon, 1)
                if (!direcciones.isNullOrEmpty()) {
                    val dir = direcciones[0]
                    val nombre = dir.locality
                        ?: dir.subAdminArea
                        ?: dir.subLocality
                        ?: dir.adminArea
                        ?: "Ubicación actual"
                    limpiarNombre(nombre)
                } else "Ubicación actual"
            } catch (e: Exception) {
                // Geocoder también puede fallar sin internet
                _estadoClima.value.ciudad.takeIf { it != "Cargando..." } ?: "Ubicación actual"
            }
        }
    }

    private fun limpiarNombre(nombre: String) = nombre
        .replace("Municipio de ", "", ignoreCase = true)
        .replace("Heroica Ciudad de ", "", ignoreCase = true)
        .trim()

    private fun interpretarCodigoClima(codigo: Int) = when (codigo) {
        0          -> "Cielo despejado"
        1, 2, 3    -> "Parcialmente nublado"
        45, 48     -> "Niebla"
        51, 53, 55 -> "Llovizna"
        61, 63, 65 -> "Lluvia"
        71, 73, 75 -> "Nieve"
        95, 96, 99 -> "Tormenta"
        else       -> "Clima variable"
    }

    fun marcarUbicacionBloqueada() {
        _estadoClima.value = _estadoClima.value.copy(
            ciudad                = "Ubicación bloqueada",
            descripcion           = "Actívala en ajustes",
            huboErrorAlActualizar = true,
            actualizando          = false
        )
    }
}

data class DatosClima(
    val ciudad                : String  = "Oaxaca",
    val temperatura           : Int     = 25,
    val descripcion           : String  = "--",
    val huboErrorAlActualizar : Boolean = false,
    val desdCache             : Boolean = false,  // true = viene de disco, no de red
    val actualizando          : Boolean = false   // true = hay petición en vuelo
)