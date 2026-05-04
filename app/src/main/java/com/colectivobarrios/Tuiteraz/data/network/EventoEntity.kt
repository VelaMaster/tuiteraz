package com.colectivobarrios.Tuiteraz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val idLocal: Int = 0,
    val idNube: Int? = null, // ID que da Supabase (int8)
    val user_id: String,
    val titulo: String,
    val fecha: String,
    val hora: String,
    val recordatorio: Boolean,
    val prioridad: String,
    val sincronizado: Boolean = false // Para saber si falta subirlo
)