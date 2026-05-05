package com.colectivobarrios.Tuiteraz.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eventos",
    indices = [Index(value = ["idNube"], unique = true)]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true)
    val idLocal: Long = 0L,

    val idNube: Long? = null,
    val user_id: String = "usuario_local_sin_backup",
    val titulo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val recordatorio: Boolean = false,
    val prioridad: String = "Media",
    val sincronizado: Boolean = false
)