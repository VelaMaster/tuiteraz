package com.colectivobarrios.Tuiteraz

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Evento(
    @Transient
    val idLocal: Long = 0L,

    val id: Long? = null,
    val user_id: String = "usuario_local_sin_backup",

    val titulo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val recordatorio: Boolean = false,
    val prioridad: String = "Media"
)