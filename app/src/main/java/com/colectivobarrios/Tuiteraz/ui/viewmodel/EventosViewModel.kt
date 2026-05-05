package com.colectivobarrios.Tuiteraz.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.colectivobarrios.Tuiteraz.Evento
import com.colectivobarrios.Tuiteraz.data.AppDatabase
import com.colectivobarrios.Tuiteraz.data.EventoEntity
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import com.colectivobarrios.Tuiteraz.worker.SyncWorker
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// Cambiamos a AndroidViewModel para tener acceso al Contexto para Room y WorkManager
class EventosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).eventoDao()

    // La UI ahora observa directamente a Room. ¡Sincronización automática!
    val eventos: StateFlow<List<Evento>> = dao.obtenerTodos()
        .map { entities -> entities.map { it.toExternalModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada: StateFlow<LocalDate> = _fechaSeleccionada.asStateFlow()

    private val _estaCargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _estaCargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Al iniciar, intentamos traer lo nuevo de la nube para actualizar Room
        refrescarDesdeNube()
    }

    fun refrescarDesdeNube() {
        Log.d("TUITERAZ_DEBUG", "EventosVM.refrescarDesdeNube() llamado")
        viewModelScope.launch {
            try {
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                Log.d("TUITERAZ_DEBUG", "EventosVM.refrescarDesdeNube usuario=${usuarioActual?.id ?: "null"}")
                if (usuarioActual != null) {
                    val resultados = SupabaseManager.client.postgrest["eventos"]
                        .select { filter { eq("user_id", usuarioActual.id) } }
                        .decodeList<Evento>()
                    Log.d("TUITERAZ_DEBUG", "EventosVM.refrescarDesdeNube ${resultados.size} eventos de la nube")

                    resultados.forEach { eventoNube ->
                        // BUSCAMOS SI YA EXISTE LOCALMENTE
                        val local = dao.obtenerPorNube(eventoNube.id ?: 0)

                        // REGLA INDESTRUCTIBLE:
                        // Solo actualizamos si no existe localmente O si lo local ya está sincronizado.
                        // Si local.sincronizado es FALSE, significa que el usuario editó algo y no queremos pisarlo.
                        if (local == null || local.sincronizado) {
                            dao.insertarOActualizar(eventoNube.toEntity(sincronizado = true).copy(
                                idLocal = local?.idLocal ?: 0 // Mantenemos el ID local para no duplicar
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TUITERAZ_DEBUG", "EventosVM.refrescarDesdeNube excepción atrapada (${e.javaClass.simpleName}): ${e.message}", e)
                _error.value = "Modo offline activo."
            }
        }
    }

    fun actualizarEvento(eventoActualizado: Evento, eventoAntiguo: Evento) {
        viewModelScope.launch {
            // 1. GUARDADO LOCAL INSTANTÁNEO
            // Al llevar el idLocal, Room hará el UPDATE en la fila correcta
            dao.insertarOActualizar(eventoActualizado.toEntity(sincronizado = false))

            // 2. DISPARAR SINCRONIZACIÓN
            // Si hay internet, se sube ahora. Si no, el Worker esperará solito.
            programarSincronizacion()
        }
    }

    fun agregarEvento(titulo: String, fecha: String, hora: String, recordatorio: Boolean, prioridad: String) {
        viewModelScope.launch {
            val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
            val nuevoEvento = Evento(
                user_id = usuarioActual?.id ?: "usuario_local_sin_backup",
                titulo = titulo,
                fecha = fecha,
                hora = hora,
                recordatorio = recordatorio,
                prioridad = prioridad
            )

            // Guardamos localmente primero
            dao.insertarOActualizar(nuevoEvento.toEntity(sincronizado = false))

            // Si hay internet, WorkManager lo subirá
            programarSincronizacion()
        }
    }

    fun cargarEventos() {
        refrescarDesdeNube()
    }
    fun eliminarEvento(evento: Evento) {
        viewModelScope.launch {
            try {
                // A. Borrado local inmediato (UI se actualiza al instante)
                if (evento.id != null) {
                    dao.eliminarPorNube(evento.id)
                } else {
                    dao.eliminarLocalmente(evento.titulo, evento.fecha, evento.hora)
                }

                // B. Borrado en Supabase si hay sesión e ID
                val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
                if (usuarioActual != null && evento.id != null) {
                    SupabaseManager.client.postgrest["eventos"].delete {
                        filter { eq("id", evento.id) }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Eliminado localmente. Sincronización pendiente."
            }
        }
    }
    fun limpiarError() {
        _error.value = null
    }
    fun migrarEventosLocales() {
        viewModelScope.launch {
            val usuarioActual = SupabaseManager.client.auth.currentUserOrNull()
            if (usuarioActual != null) {
                // Usamos la query correcta del DAO
                val sinCuenta = dao.obtenerEventosSinCuenta()

                sinCuenta.forEach { entity ->
                    dao.insertarOActualizar(
                        entity.copy(
                            user_id = usuarioActual.id,
                            sincronizado = false // Pendiente de subir
                        )
                    )
                }

                if (sinCuenta.isNotEmpty()) {
                    Log.d("EventosVM", "Migrando ${sinCuenta.size} evento(s) local(es) a cuenta ${usuarioActual.id}")
                    programarSincronizacion()
                }
            }
        }
    }
    fun actualizarFechaSeleccionada(nuevaFecha: LocalDate) {
        _fechaSeleccionada.value = nuevaFecha
    }

    private fun programarSincronizacion() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "sync_eventos_supabase",
            ExistingWorkPolicy.REPLACE, // Reemplaza tareas pendientes para no saturar
            syncRequest
        )
    }

    // Funciones de extensión para convertir entre modelos (mapeo)
    private fun EventoEntity.toExternalModel() = Evento(
        idLocal = idLocal, // <-- MAPEAMOS EL ID LOCAL
        id = idNube,
        user_id = user_id,
        titulo = titulo,
        fecha = fecha,
        hora = hora,
        recordatorio = recordatorio,
        prioridad = prioridad
    )
    private fun Evento.toEntity(sincronizado: Boolean) = EventoEntity(
        idLocal = idLocal,
        idNube = id,
        user_id = user_id,
        titulo = titulo,
        fecha = fecha,
        hora = hora,
        recordatorio = recordatorio,
        prioridad = prioridad,
        sincronizado = sincronizado
    )
}