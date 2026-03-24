package com.example.Tuiteraz.data.network
import com.example.Tuiteraz.Frase
import java.time.LocalDate


object ProveedorFrases {
    val listaDeFrases = listOf(
        Frase(1, "El único modo de hacer un gran trabajo es amar lo que haces.", "Steve Jobs"),
        Frase(2, "No cuentes los días, haz que los días cuenten.", "Muhammad Ali"),
        Frase(3, "La vida es eso que pasa mientras estás ocupado haciendo otros planes.", "John Lennon"),
        Frase(4, "El momento en que quieres renunciar es el momento en que debes seguir empujando.", "Anónimo"),
        Frase(5, "Lo que te preocupa te domina.", "John Locke")
    )

    fun obtenerFraseDelDia(): Frase {
        val diasTranscurridos = LocalDate.now().toEpochDay()
        val indice = (diasTranscurridos % listaDeFrases.size).toInt()

        return listaDeFrases[indice]
    }
}