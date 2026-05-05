package com.colectivobarrios.Tuiteraz.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colectivobarrios.Tuiteraz.Evento
import com.colectivobarrios.Tuiteraz.data.AppDatabase
import com.colectivobarrios.Tuiteraz.data.EventoEntity
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

private const val TAG = "SyncWorker"

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).eventoDao()

        // ── 1. Verificar sesión ──────────────────────────────────────────────
        val usuario = try {
            SupabaseManager.client.auth.currentUserOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar sesión: ${e.message}")
            null
        }

        if (usuario == null) {
            Log.d(TAG, "Sin sesión activa — guardado solo local, OK.")
            return Result.success()
        }

        Log.d(TAG, "Sesión activa: ${usuario.id}")

        // ── 2. Obtener pendientes ────────────────────────────────────────────
        val pendientes = dao.obtenerPendientesDeSubir()

        if (pendientes.isEmpty()) {
            Log.d(TAG, "Sin cambios pendientes.")
            return Result.success()
        }

        Log.d(TAG, "${pendientes.size} evento(s) pendiente(s) de sincronizar")

        // ── 3. Sincronizar cada uno ──────────────────────────────────────────
        return try {
            pendientes.forEach { entity ->
                val entityConUsuario = entity.copy(user_id = usuario.id)

                if (entity.idNube != null) {
                    // CASO A: Ya existe en Supabase → UPDATE
                    actualizarEnNube(entityConUsuario)
                } else {
                    // CASO B: Es nuevo → INSERT y guardamos el id que devuelve Supabase
                    insertarEnNube(entityConUsuario, dao)
                }
            }
            Log.d(TAG, "✅ Sincronización completa")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando, se reintentará: ${e.message}", e)
            Result.retry()
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    private suspend fun actualizarEnNube(entity: EventoEntity) {
        Log.d(TAG, "UPDATE → idNube=${entity.idNube} titulo='${entity.titulo}' hora=${entity.hora} recordatorio=${entity.recordatorio} prioridad=${entity.prioridad}")

        val payload = EventoUpdate(
            titulo       = entity.titulo,
            fecha        = entity.fecha,
            hora         = entity.hora,
            recordatorio = entity.recordatorio,
            prioridad    = entity.prioridad
        )

        // Solo filtramos por id — RLS de Supabase protege que solo toques los tuyos
        val resultado = SupabaseManager.client.postgrest["eventos"]
            .update(payload) {
                filter {
                    eq("id", entity.idNube!!)
                }
                select() // <-- CRÍTICO: pedimos que devuelva la fila actualizada
            }
            .decodeSingleOrNull<Evento>()

        if (resultado == null) {
            Log.e(TAG, "❌ UPDATE no afectó ninguna fila — idNube=${entity.idNube} no existe o RLS lo bloqueó")
            throw Exception("UPDATE sin efecto para idNube=${entity.idNube}")
        }

        Log.d(TAG, "✅ UPDATE confirmado: id=${resultado.id} titulo='${resultado.titulo}'")

        AppDatabase.getDatabase(applicationContext).eventoDao()
            .marcarComoSincronizado(entity.idLocal)
    }

    // ── INSERT ───────────────────────────────────────────────────────────────
    private suspend fun insertarEnNube(entity: EventoEntity, dao: com.colectivobarrios.Tuiteraz.data.EventoDao) {
        Log.d(TAG, "INSERT → titulo='${entity.titulo}' user_id=${entity.user_id}")

        val payload = EventoNube(
            user_id      = entity.user_id,
            titulo       = entity.titulo,
            fecha        = entity.fecha,
            hora         = entity.hora,
            recordatorio = entity.recordatorio,
            prioridad    = entity.prioridad
        )

        val respuesta = SupabaseManager.client.postgrest["eventos"]
            .insert(payload) {
                select()
            }
            .decodeSingle<Evento>()

        Log.d(TAG, "INSERT exitoso, idNube asignado=${respuesta.id}")

        // Guardar el id real de Supabase en Room y marcar sincronizado
        dao.insertarOActualizar(
            entity.copy(
                idNube      = respuesta.id,
                sincronizado = true
            )
        )
    }
}

// ── Modelos de serialización para Supabase ───────────────────────────────────

/** Solo los campos que Supabase genera al hacer INSERT (sin `id`, sin `created_at`) */
@Serializable
private data class EventoNube(
    val user_id     : String,
    val titulo      : String,
    val fecha       : String,
    val hora        : String,
    val recordatorio: Boolean,
    val prioridad   : String
)

/** Solo los campos que el usuario puede editar en un UPDATE */
@Serializable
private data class EventoUpdate(
    val titulo      : String,
    val fecha       : String,
    val hora        : String,
    val recordatorio: Boolean,
    val prioridad   : String
)