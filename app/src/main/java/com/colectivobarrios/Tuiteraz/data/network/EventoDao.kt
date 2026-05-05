package com.colectivobarrios.Tuiteraz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Query("SELECT * FROM eventos ORDER BY fecha ASC")
    fun obtenerTodos(): Flow<List<EventoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(evento: EventoEntity)

    @Query("SELECT * FROM eventos WHERE sincronizado = 0")
    suspend fun obtenerPendientesDeSubir(): List<EventoEntity>

    // Unificada en Long (coincide con el PrimaryKey de EventoEntity)
    @Query("UPDATE eventos SET sincronizado = 1 WHERE idLocal = :id")
    suspend fun marcarComoSincronizado(id: Long)

    @Query("DELETE FROM eventos WHERE idNube = :idNube")
    suspend fun eliminarPorNube(idNube: Long)

    @Query("DELETE FROM eventos WHERE titulo = :titulo AND fecha = :fecha AND hora = :hora")
    suspend fun eliminarLocalmente(titulo: String, fecha: String, hora: String)

    @Query("SELECT * FROM eventos WHERE idNube = :idNube LIMIT 1")
    suspend fun obtenerPorNube(idNube: Long): EventoEntity?

    // Necesaria para migrarEventosLocales() en el ViewModel
    @Query("SELECT * FROM eventos WHERE user_id = 'usuario_local_sin_backup'")
    suspend fun obtenerEventosSinCuenta(): List<EventoEntity>
}