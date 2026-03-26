package com.example.Tuiteraz.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.Tuiteraz.Frase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "tuiteraz_cache")

class CacheFrases(private val context: Context) {
    companion object {
        val FECHA_ULTIMA_FRASE = longPreferencesKey("fecha_ultima_frase")
        val FRASE_GUARDADA_JSON = stringPreferencesKey("frase_guardada_json")
        val LISTA_IDS_BARAJADOS = stringPreferencesKey("lista_ids_barajados")
        val INDICE_ACTUAL = intPreferencesKey("indice_actual")
        val ULTIMO_ID_CONOCIDO = intPreferencesKey("ultimo_id_conocido") // <-- NUEVO
    }

    // --- LECTURAS ---
    suspend fun obtenerFechaUltimaFrase(): Long = context.dataStore.data.map { it[FECHA_ULTIMA_FRASE] ?: 0L }.first()
    suspend fun obtenerIndiceActual(): Int = context.dataStore.data.map { it[INDICE_ACTUAL] ?: 0 }.first()
    suspend fun obtenerUltimoIdConocido(): Int = context.dataStore.data.map { it[ULTIMO_ID_CONOCIDO] ?: 0 }.first()

    suspend fun obtenerListaIds(): List<Int> {
        val stringLista = context.dataStore.data.map { it[LISTA_IDS_BARAJADOS] ?: "" }.first()
        if (stringLista.isEmpty()) return emptyList()
        return stringLista.split(",").map { it.toInt() }
    }

    suspend fun obtenerFraseCacheada(): Frase? {
        val jsonString = context.dataStore.data.map { it[FRASE_GUARDADA_JSON] }.first()
        return if (jsonString != null) Json.decodeFromString<Frase>(jsonString) else null
    }

    // --- ESCRITURAS ---
    suspend fun actualizarBarajaYMaxId(nuevaListaIds: List<Int>, nuevoUltimoId: Int) {
        context.dataStore.edit { prefs ->
            prefs[LISTA_IDS_BARAJADOS] = nuevaListaIds.joinToString(",")
            prefs[ULTIMO_ID_CONOCIDO] = nuevoUltimoId
        }
    }

    suspend fun reiniciarBarajaCompleta(nuevaListaIds: List<Int>, nuevoUltimoId: Int) {
        context.dataStore.edit { prefs ->
            prefs[LISTA_IDS_BARAJADOS] = nuevaListaIds.joinToString(",")
            prefs[ULTIMO_ID_CONOCIDO] = nuevoUltimoId
            prefs[INDICE_ACTUAL] = 0
        }
    }

    suspend fun guardarNuevaFraseDelDia(diaActual: Long, frase: Frase, nuevoIndice: Int) {
        val fraseJson = Json.encodeToString(frase)
        context.dataStore.edit { prefs ->
            prefs[FECHA_ULTIMA_FRASE] = diaActual
            prefs[FRASE_GUARDADA_JSON] = fraseJson
            prefs[INDICE_ACTUAL] = nuevoIndice
        }
    }
}