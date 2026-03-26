package com.example.Tuiteraz.data.network

import android.util.Log
import com.example.Tuiteraz.Frase
import com.example.Tuiteraz.data.local.CacheFrases
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.ZonedDateTime

class ProveedorFrasesRepository(
    private val supabase: SupabaseClient,
    private val cache: CacheFrases
) {
    suspend fun obtenerFraseDelDia(): Frase? {
        val hoyLogico = ZonedDateTime.now().minusHours(4).toLocalDate().toEpochDay()
        val diaUltimaVez = cache.obtenerFechaUltimaFrase()

        // 1. MODO RÁPIDO
        if (hoyLogico == diaUltimaVez) {
            cache.obtenerFraseCacheada()?.let { return it }
        }

        try {
            // 2. ¿Cuál es el ID más alto?
            // SOLUCIÓN: Quitamos Columns.list("id") para que traiga el objeto completo y Kotlin no se asuste
            val maxIdDb = supabase.postgrest["frases"]
                .select {
                    order("id", order = Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<Frase>()?.id ?: 0

            if (maxIdDb == 0) throw Exception("La tabla de frases en Supabase está vacía o RLS está bloqueando la lectura.")

            val ultimoIdLocal = cache.obtenerUltimoIdConocido()
            var listaIds = cache.obtenerListaIds()
            var indiceActual = cache.obtenerIndiceActual()

            // 3. LA MAGIA ESCALABLE
            if (maxIdDb > ultimoIdLocal) {
                val nuevosIds = ((ultimoIdLocal + 1)..maxIdDb).shuffled()
                listaIds = listaIds + nuevosIds
                cache.actualizarBarajaYMaxId(listaIds, maxIdDb)
            }

            // 4. ¿Ya vio todas las frases posibles?
            if (listaIds.isEmpty() || indiceActual >= listaIds.size) {
                listaIds = (1..maxIdDb).shuffled()
                cache.reiniciarBarajaCompleta(listaIds, maxIdDb)
                indiceActual = 0
            }

            val idFraseQueToca = listaIds[indiceActual]

            // 5. Descargamos la frase exacta
            val fraseSupabase = supabase.postgrest["frases"]
                .select { filter { eq("id", idFraseQueToca) } }
                .decodeSingle<Frase>()

            // 6. Guardamos progreso
            cache.guardarNuevaFraseDelDia(hoyLogico, fraseSupabase, indiceActual + 1)

            return fraseSupabase

        } catch (e: Exception) {
            // IMPRIMIMOS EL ERROR EN LOGCAT PARA SABER QUÉ PASÓ REALMENTE
            Log.e("TuiterazDebug", "Error al cargar frase de Supabase: ${e.message}", e)

            // 7. MODO EMERGENCIA
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