package com.colectivobarrios.Tuiteraz.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colectivobarrios.Tuiteraz.MainActivity
import com.colectivobarrios.Tuiteraz.R
import com.colectivobarrios.Tuiteraz.data.local.CacheFrases
import com.colectivobarrios.Tuiteraz.data.network.ProveedorFrasesRepository
import com.colectivobarrios.Tuiteraz.data.network.SupabaseManager

class NotificacionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val cacheLocal = CacheFrases(context)
            val repositorio = ProveedorFrasesRepository(context, SupabaseManager.client, cacheLocal)

            // 2. Traemos la frase nueva.
            // IMPORTANTE: Tu repositorio ya guarda en caché y llama a FraseWidget().updateAll(context)
            val fraseNueva = repositorio.obtenerFraseDelDia()

            // 3. Verificamos si las notificaciones están activas
            val sharedPrefs = context.getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE)
            val notifsActivas = sharedPrefs.getBoolean("notifs_activas", false)

            if (notifsActivas) {
                mostrarNotificacion(fraseNueva?.texto ?: "Entra ahora y descubre tu frase del día.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificacionWorker", "Error actualizando en segundo plano", e)
            Result.retry()
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoFrase)) // Para que se pueda expandir y leer completa
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }
}