package com.colectivobarrios.Tuiteraz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colectivobarrios.Tuiteraz.Evento
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class EventosViewModel : ViewModel() {

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada: StateFlow<LocalDate> = _fechaSeleccionada.asStateFlow()

    private val _estaCargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _estaCargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        cargarEventos()
    }

    fun actualizarFechaSeleccionada(nuevaFecha: LocalDate) {
        _fechaSeleccionada.value = nuevaFecha
    }

    fun limpiarError() {
        _error.value = null
    }

    fun cargarEventos() {
        viewModelScope.launch {
            _estaCargando.value = true
            try {
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                if (usuarioActual != null) {
                    val resultados = SupabaseManager.client.postgrest["eventos"]
                        .select { filter { eq("user_id", usuarioActual.id) } }
                        .decodeList<Evento>()
                    _eventos.value = resultados
                } else {
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar: ${e.message}"
            } finally {
                _estaCargando.value = false
            }
        }
    }
    // ... (dentro de la clase EventosViewModel)

    fun eliminarEvento(evento: Evento) {
        viewModelScope.launch {
            _estaCargando.value = true
            try {
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                if (usuarioActual != null && evento.id != null) {
                    SupabaseManager.client.postgrest["eventos"].delete {
                        filter { eq("id", evento.id) }
                    }
                    cargarEventos()
                } else {
                    // Borrado local si no hay sesión
                    _eventos.value = _eventos.value.filter { it != evento }
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar: ${e.message}"
            } finally {
                _estaCargando.value = false
            }
        }
    }
    // ... dentro de la clase EventosViewModel

    // ACTUALIZADO: Ahora busca por ID si existe, o por los datos antiguos si es un evento local sin ID.
    fun actualizarEvento(eventoActualizado: Evento, eventoAntiguo: Evento) {
        viewModelScope.launch {
            // 1. ACTUALIZACIÓN OPTIMISTA (LOCAL)
            // Hacemos el cambio en la lista local de inmediato para que la UI se vea perfecta
            _eventos.value = _eventos.value.map {
                if (it.id != null && it.id == eventoActualizado.id) {
                    eventoActualizado // Si tiene ID, comparamos por ID
                } else if (it.id == null && it == eventoAntiguo) {
                    eventoActualizado // Si es local (sin ID), comparamos el objeto completo
                } else {
                    it
                }
            }

            try {
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                if (usuarioActual != null && eventoActualizado.id != null) {
                    // 2. ACTUALIZACIÓN EN LA NUBE
                    // Aquí le mandamos TODO a Supabase (título, prioridad, hora y recordatorio)
                    SupabaseManager.client.postgrest["eventos"].update({
                        Evento::titulo setTo eventoActualizado.titulo
                        Evento::prioridad setTo eventoActualizado.prioridad
                        Evento::hora setTo eventoActualizado.hora // ¡Aseguramos la hora!
                        Evento::recordatorio setTo eventoActualizado.recordatorio // ¡Y el relojito!
                    }) {
                        filter { eq("id", eventoActualizado.id) }
                    }

                    // NOTA: Ya no llamamos a cargarEventos() aquí inmediatamente
                    // para evitar que datos "viejos" del servidor pisen nuestro cambio local.
                } else {
                    // Si no hay sesión, el cambio ya se quedó en la lista local arriba.
                    _error.value = "Cambio guardado localmente."
                }
            } catch (e: Exception) {
                _error.value = "Error al sincronizar: ${e.message}"
                // Si falla la nube, re-cargamos para volver a la realidad del servidor (Rollback)
                cargarEventos()
            }
        }
    }

    fun alternarRecordatorio(evento: Evento) {
        viewModelScope.launch {
            try {
                val nuevoEstado = !evento.recordatorio
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()

                if (usuarioActual != null && evento.id != null) {
                    SupabaseManager.client.postgrest["eventos"].update({
                        Evento::recordatorio setTo nuevoEstado
                    }) {
                        filter { eq("id", evento.id) }
                    }
                    cargarEventos()
                } else {
                    // Actualización local
                    _eventos.value = _eventos.value.map {
                        if (it == evento) it.copy(recordatorio = nuevoEstado) else it
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar: ${e.message}"
            }
        }
    }
// ... dentro de class EventosViewModel : ViewModel()

// ... dentro de class EventosViewModel : ViewModel()

    fun migrarEventosLocales() {
        viewModelScope.launch {
            val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
            if (usuarioActual != null) {
                // 1. Filtramos los eventos que se crearon "offline"
                val eventosLocales = _eventos.value.filter { it.user_id == "usuario_local_sin_backup" }

                if (eventosLocales.isNotEmpty()) {
                    try {
                        // 2. Los subimos uno por uno (o en lote si prefieres) con el ID real
                        eventosLocales.forEach { local ->
                            val eventoParaSubir = local.copy(
                                user_id = usuarioActual.id,
                                id = null // Dejamos que Supabase genere el ID real
                            )
                            SupabaseManager.client.postgrest["eventos"].insert(eventoParaSubir)
                        }

                        // 3. Limpiamos y recargamos todo desde la nube para tener los IDs reales
                        cargarEventos()
                        _error.value = "¡Tus eventos locales se han sincronizado con éxito!"
                    } catch (e: Exception) {
                        _error.value = "Error al sincronizar: ${e.message}"
                    }
                } else {
                    // Si no había nada local, solo cargamos lo que ya estaba en la nube
                    cargarEventos()
                }
            }
        }
    }
    fun agregarEvento(titulo: String, fecha: String, hora: String, recordatorio: Boolean, prioridad: String) {
        viewModelScope.launch {
            _estaCargando.value = true
            try {
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                val nuevoEvento = Evento(
                    user_id = usuarioActual?.id ?: "usuario_local_sin_backup",
                    titulo = titulo,
                    fecha = fecha,
                    hora = hora,
                    recordatorio = recordatorio,
                    prioridad = prioridad
                )

                if (usuarioActual != null) {
                    SupabaseManager.client.postgrest["eventos"].insert(nuevoEvento)
                    cargarEventos()
                } else {
                    _eventos.value = _eventos.value + nuevoEvento
                    _error.value = "Guardado inicie sesión para tener respaldo."
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _estaCargando.value = false
            }
        }
    }
}