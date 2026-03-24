package com.example.Tuiteraz

data class Frase(
    val id: Int = 0,       // Le ponemos un ID numérico (útil para la base de datos)
    val texto: String,     // El contenido de la frase
    val autor: String      // Quién lo dijo
)