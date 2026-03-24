package com.example.Tuiteraz.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.Tuiteraz.MainActivity
import com.example.Tuiteraz.R

class NotificacionWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        mostrarNotificacion()
        return Result.success()
    }

    private fun mostrarNotificacion() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canalId = "frase_diaria_channel"

        // En Android 8+ es obligatorio crear un "Canal de Notificaciones"
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

        // Este Intent hace que al tocar la notificación, se abra la app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Construimos el diseño de la notificación
        val builder = NotificationCompat.Builder(context, canalId)
            // Aquí debes poner el ícono de tu app. Usa uno que exista en tus carpetas drawable/mipmap.
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("¡Una nueva frase te espera!")
            .setContentText("Entra ahora y descubre tu frase del día.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Se borra al tocarla

        // Lanzamos la notificación
        notificationManager.notify(1, builder.build())
    }
}