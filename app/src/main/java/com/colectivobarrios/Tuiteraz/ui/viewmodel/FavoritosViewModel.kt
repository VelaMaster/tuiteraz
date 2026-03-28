package com.colectivobarrios.Tuiteraz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colectivobarrios.Tuiteraz.Frase
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
@Serializable
data class FraseUnidaDto(val texto: String, val autor: String)
@Serializable
data class FavoritoDto(
    val user_id: String,
    val frase_id: Int,
    val frases: FraseUnidaDto? = null
)
@Serializable
data class InsertarFavoritoDto(
    val user_id: String,
    val frase_id: Int
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
                    else -> {}
                }
            }
        }
    }
    private fun cargarFavoritosDeLaNube() {
        viewModelScope.launch {
            try {
                val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch
                val respuesta = SupabaseManager.client.postgrest["favoritos"]
                    .select(columns = Columns.raw("user_id, frase_id, frases(texto, autor)")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<FavoritoDto>()

                _listaFavoritos.value = respuesta.map {
                    Frase(
                        id = it.frase_id,
                        texto = it.frases?.texto ?: "Frase no encontrada",
                        autor = it.frases?.autor ?: "Desconocido"
                    )
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
                    // Usamos el nuevo DTO más ligero
                    val nuevoFav = InsertarFavoritoDto(user_id = userId, frase_id = frase.id)
                    SupabaseManager.client.postgrest["favoritos"].insert(nuevoFav)
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