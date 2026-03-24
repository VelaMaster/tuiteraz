package com.example.Tuiteraz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Tuiteraz.Frase
import com.example.Tuiteraz.data.network.SupabaseManager
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class FavoritoDto(
    val user_id: String,
    val frase_id: Int,
    val texto: String,
    val autor: String
)

class FavoritosViewModel : ViewModel() {
    private val _listaFavoritos = MutableStateFlow<List<Frase>>(emptyList())
    val listaFavoritos: StateFlow<List<Frase>> = _listaFavoritos.asStateFlow()

    init {
        viewModelScope.launch {
            SupabaseManager.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> cargarFavoritosDeLaNube()
                    is SessionStatus.NotAuthenticated -> _listaFavoritos.value = emptyList()
                    else -> {} // Cargando... no hacemos nada
                }
            }
        }
    }

    private fun cargarFavoritosDeLaNube() {
        viewModelScope.launch {
            try {
                val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch

                // Descargamos las frases de este usuario
                val respuesta = SupabaseManager.client.postgrest["favoritos"]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<FavoritoDto>()

                // Actualizamos la memoria de la app
                _listaFavoritos.value = respuesta.map {
                    Frase(id = it.frase_id, texto = it.texto, autor = it.autor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun alternarFavorito(frase: Frase) {
        viewModelScope.launch {
            val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch

            val listaActual = _listaFavoritos.value.toMutableList()
            val yaEsFavorita = listaActual.any { it.id == frase.id }

            try {
                if (yaEsFavorita) {
                    SupabaseManager.client.postgrest["favoritos"].delete {
                        filter {
                            eq("user_id", userId)
                            eq("frase_id", frase.id)
                        }
                    }
                    listaActual.removeAll { it.id == frase.id }
                } else {
                    val nuevoFav = FavoritoDto(
                        user_id = userId,
                        frase_id = frase.id,
                        texto = frase.texto,
                        autor = frase.autor
                    )
                    SupabaseManager.client.postgrest["favoritos"].insert(nuevoFav)

                    // 2. Mostrar en la pantalla
                    listaActual.add(frase)
                }
                _listaFavoritos.value = listaActual
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun esFavorita(id: Int): Boolean {
        return _listaFavoritos.value.any { it.id == id }
    }
}