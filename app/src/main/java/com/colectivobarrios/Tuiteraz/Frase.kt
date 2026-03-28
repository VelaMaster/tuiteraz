package com.colectivobarrios.Tuiteraz
import kotlinx.serialization.Serializable
@Serializable
data class Frase(
    val id: Int = 0,
    val texto: String,
    val autor: String
)