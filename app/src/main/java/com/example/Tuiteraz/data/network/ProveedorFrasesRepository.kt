package com.example.Tuiteraz.data.network

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.Tuiteraz.Frase
import com.example.Tuiteraz.data.local.CacheFrases
import com.example.Tuiteraz.widget.FraseWidget
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
        if (hoyLogico == diaUltimaVez) {
            cache.obtenerFraseCacheada()?.let { return it }
        }

        try {
            val maxIdDb = supabase.postgrest["frases"]
                .select {
                    order("id", order = Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<Frase>()?.id ?: 0

            if (maxIdDb == 0) throw Exception("La tabla de frases en Supabase está vacía o RLS está bloqueando la lectura.")

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
            FraseWidget().updateAll(contexto)
            return fraseSupabase
        } catch (e: Exception) {
            Log.e("TuiterazDebug", "Error al cargar frase de Supabase: ${e.message}", e)
            val fraseAyer = cache.obtenerFraseCacheada()

            return if (fraseAyer != null) {
                fraseAyer
            } else {
                Frase(
                    id = 0,
                    texto = "Falla al cargar",
                    autor = "Falla al cargar"
                )
            }
        }
    }
}