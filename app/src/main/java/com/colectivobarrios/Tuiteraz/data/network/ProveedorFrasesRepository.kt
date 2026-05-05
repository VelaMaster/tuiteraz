package com.colectivobarrios.Tuiteraz.data.network

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.colectivobarrios.Tuiteraz.Frase
import com.colectivobarrios.Tuiteraz.data.local.CacheFrases
import com.colectivobarrios.Tuiteraz.widget.FraseWidget
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.ZonedDateTime

class ProveedorFrasesRepository(
    private val contexto: Context,
    private val supabase: SupabaseClient,
    private val cache: CacheFrases
) {
    suspend fun obtenerFraseDelDia(): Frase? {
        val hoyLogico = ZonedDateTime.now().minusHours(4).toLocalDate().toEpochDay()
        val diaUltimaVez = cache.obtenerFechaUltimaFrase()

        // Si ya tenemos la frase de hoy en caché, la devolvemos sin tocar la red
        if (hoyLogico == diaUltimaVez) {
            cache.obtenerFraseCacheada()?.let { return it }
        }

        // Sin caché de hoy → intentamos la red
        return try {
            val maxIdDb = supabase.postgrest["frases"]
                .select {
                    order("id", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<Frase>()?.id ?: 0

            if (maxIdDb == 0) {
                Log.w("FrasesRepo", "Tabla vacía o RLS bloqueando, usando caché")
                return cache.obtenerFraseCacheada() ?: fraseFallback()
            }

            val ultimoIdLocal = cache.obtenerUltimoIdConocido()
            var listaIds = cache.obtenerListaIds()
            var indiceActual = cache.obtenerIndiceActual()

            if (maxIdDb > ultimoIdLocal) {
                val nuevosIds = ((ultimoIdLocal + 1)..maxIdDb).shuffled()
                listaIds = listaIds + nuevosIds
                cache.actualizarBarajaYMaxId(listaIds, maxIdDb)
            }

            if (listaIds.isEmpty() || indiceActual >= listaIds.size) {
                listaIds = (1..maxIdDb).shuffled()
                cache.reiniciarBarajaCompleta(listaIds, maxIdDb)
                indiceActual = 0
            }

            val idFraseQueToca = listaIds[indiceActual]
            val fraseSupabase = supabase.postgrest["frases"]
                .select { filter { eq("id", idFraseQueToca) } }
                .decodeSingle<Frase>()

            cache.guardarNuevaFraseDelDia(hoyLogico, fraseSupabase, indiceActual + 1)

            // Widget en try-catch separado — si falla no rompe el flujo principal
            try {
                FraseWidget().updateAll(contexto)
            } catch (e: Exception) {
                Log.w("FrasesRepo", "Widget no pudo actualizarse: ${e.message}")
            }

            fraseSupabase

        } catch (e: Exception) {
            Log.w("FrasesRepo", "Sin red, usando caché: ${e.message}")
            // Primero intentamos el caché, si no hay nada mostramos fallback
            cache.obtenerFraseCacheada() ?: fraseFallback()
        }
    }

    // Frase para cuando no hay red Y no hay caché guardado
    // Null hace que el ViewModel muestre el skeleton indefinidamente — mejor un mensaje claro
    private fun fraseFallback() = Frase(
        id = 0,
        texto = "Sin conexión — abre la app con internet para cargar tu frase del día",
        autor = "Tuiteraz"
    )
}