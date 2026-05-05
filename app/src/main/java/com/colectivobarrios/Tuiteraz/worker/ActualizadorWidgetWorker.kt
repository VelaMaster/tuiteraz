package com.colectivobarrios.Tuiteraz.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.colectivobarrios.Tuiteraz.MainActivity
import com.colectivobarrios.Tuiteraz.R
import com.colectivobarrios.Tuiteraz.data.local.CacheFrases
import com.colectivobarrios.Tuiteraz.data.network.ProveedorFrasesRepository
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager
import com.colectivobarrios.Tuiteraz.widget.FraseWidget
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Worker dedicado a refrescar el widget de la frase del día.
 *
 * Se auto-programa para correr exactamente a las 4:00 AM hora local del usuario
 * (la hora se ajusta a la zona horaria del dispositivo: si el usuario viaja de
 * México a China, el worker correrá a las 4 AM de China automáticamente).
 *
 * Comportamiento:
 *  - Si hay red: pide la frase del día al repositorio (que actualiza la caché y
 *    el widget internamente) y muestra notificación si están activadas.
 *  - Si NO hay red: el repositorio devuelve la última frase del caché, así que
 *    el widget conserva la frase anterior hasta que vuelva la conexión.
 *  - Al terminar (con éxito o fallo) se reprograma para el siguiente 4 AM.
 *
 * El worker NO usa PeriodicWorkRequest porque WorkManager corre los periódicos
 * en cualquier momento dentro de la ventana — no garantiza una hora exacta.
 * En cambio se usa OneTimeWorkRequest con setInitialDelay() calculado a la hora
 * objetivo, y al completarse se reprograma a sí mismo. Es la forma estándar
 * de tener un job que corre a una hora exacta.
 */
class ActualizadorWidgetWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker arrancó — actualizando frase del día y widget")

        // Siempre reprogramamos primero, así si algo falla más adelante el
        // próximo día igual se va a ejecutar.
        try {
            programarParaProxima4AM(context)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo reprogramar el siguiente worker: ${e.message}", e)
        }

        return try {
            val cacheLocal  = CacheFrases(context)
            val repositorio = ProveedorFrasesRepository(context, SupabaseManager.client, cacheLocal)

            // El repositorio internamente:
            //  - Si ya hay frase de hoy en caché → la devuelve sin tocar la red
            //  - Si no, intenta Supabase → si falla por red, devuelve la del caché
            //  - Cuando obtiene una frase nueva, llama updateAll(context) para refrescar el widget
            val frase = repositorio.obtenerFraseDelDia()
            Log.d(TAG, "Frase obtenida: ${frase != null} (texto='${frase?.texto?.take(40)}...')")

            // Forzamos un updateAll del widget AUN si no se obtuvo frase nueva,
            // porque el widget puede estar mostrando datos de hace varios días
            // si la app no se ha abierto en mucho tiempo.
            try {
                FraseWidget().updateAll(context)
            } catch (e: Exception) {
                Log.w(TAG, "updateAll del widget falló: ${e.message}", e)
            }

            // Notificación opcional
            val sharedPrefs = context.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE)
            val notifsActivas = sharedPrefs.getBoolean("notifs_activas", false)
            if (notifsActivas && frase != null) {
                mostrarNotificacion(frase.texto)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en el worker (no crítico, ya está reprogramado): ${e.message}", e)
            // Devolvemos success para no agotar reintentos — el siguiente día
            // ya está programado vía programarParaProxima4AM al inicio.
            Result.success()
        }
    }

    private fun mostrarNotificacion(textoFrase: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canalId = "frase_diaria_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalId,
                "Frase Diaria",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones diarias para descubrir tu nueva frase"
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("¡Tu nueva frase está lista!")
            .setContentText(textoFrase)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoFrase))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    companion object {
        const val TAG = "ActualizadorWidget"
        const val WORK_NAME = "actualizador_widget_4am"

        /** Hora local objetivo a la que el worker debe correr cada día. */
        val HORA_OBJETIVO: LocalTime = LocalTime.of(4, 0)

        /**
         * Programa (o reprograma) el worker para que corra a las 4 AM hora local
         * del próximo día. Si ya pasaron las 4 AM de hoy, programa para mañana.
         *
         * Usa la zona horaria del DISPOSITIVO automáticamente (ZoneId.systemDefault),
         * así que si el usuario viaja, el worker se ajusta solo en la próxima ejecución.
         *
         * @param reemplazar Si true (REPLACE), cancela cualquier worker pendiente y crea uno
         *                    nuevo. Si false (KEEP), respeta el ya programado. Default true
         *                    para que cambios de zona horaria entren en vigor inmediatamente.
         */
        fun programarParaProxima4AM(context: Context, reemplazar: Boolean = true) {
            val zona = ZoneId.systemDefault()
            val ahora = ZonedDateTime.now(zona)
            var proximaEjecucion = ahora
                .withHour(HORA_OBJETIVO.hour)
                .withMinute(HORA_OBJETIVO.minute)
                .withSecond(0)
                .withNano(0)

            // Si ya pasó la hora objetivo de hoy, saltamos a mañana
            if (!proximaEjecucion.isAfter(ahora)) {
                proximaEjecucion = proximaEjecucion.plusDays(1)
            }

            val delayMs = ChronoUnit.MILLIS.between(ahora, proximaEjecucion)
            Log.d(TAG, "Próxima ejecución programada para $proximaEjecucion (en ${delayMs / 1000 / 60} min, zona=$zona)")

            val request = OneTimeWorkRequestBuilder<ActualizadorWidgetWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            val politica = if (reemplazar) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, politica, request)
        }
    }
}
