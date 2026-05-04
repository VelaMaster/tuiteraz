package com.colectivobarrios.Tuiteraz
import kotlinx.serialization.Serializable

@Serializable
data class Evento(
    val id: Int? = null,
    val user_id: String,
    val titulo: String,
    val fecha: String,
    val hora: String,
    val recordatorio: Boolean = false,
    val prioridad: String = "Media"
)