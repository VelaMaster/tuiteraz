package com.colectivobarrios.Tuiteraz.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.colectivobarrios.Tuiteraz.MainActivity
import com.colectivobarrios.Tuiteraz.R

/**
 * Receiver que dispara el AlarmManager cuando llega la hora de un recordatorio.
 *
 * Cada evento dispara este receiver DOS veces:
 *  - Una hora antes (momento = UNA_HORA_ANTES) → "Tu evento es en 1 hora"
 *  - En la hora exacta (momento = EN_LA_HORA)  → "¡Ya es la hora!"
 *
 * Decide qué hacer según la prioridad:
 *  - Baja / Media → notificación normal con título y mensaje
 *  - Alta         → full-screen intent (lanza AlarmaEventoActivity como alarma)
 */
class RecordatorioEventoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != EventoAlarmManager.ACCION_RECORDATORIO) return

        val idLocal   = intent.getLongExtra(EventoAlarmManager.EXTRA_ID_LOCAL, 0L)
        val titulo    = intent.getStringExtra(EventoAlarmManager.EXTRA_TITULO).orEmpty()
        val fecha     = intent.getStringExtra(EventoAlarmManager.EXTRA_FECHA).orEmpty()
        val hora      = intent.getStringExtra(EventoAlarmManager.EXTRA_HORA).orEmpty()
        val prioridad = intent.getStringExtra(EventoAlarmManager.EXTRA_PRIORIDAD).orEmpty()
        val momentoStr = intent.getStringExtra(EventoAlarmManager.EXTRA_MOMENTO)
            ?: EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES.name
        val momento = try {
            EventoAlarmManager.MomentoRecordatorio.valueOf(momentoStr)
        } catch (_: Exception) {
            EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES
        }

        Log.d(TAG, "Recibido recordatorio id=$idLocal momento=$momento titulo='$titulo' prioridad='$prioridad'")

        // WakeLock breve para garantizar que el CPU no se duerma mientras
        // posteamos la notificación y (para Alta) lanzamos la activity.
        // Sin esto, en Doze profundo el sistema puede quitar el CPU al receiver
        // antes de que termine y la notificación llega con segundos/minutos de retraso.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Tuiteraz:RecordatorioReceiver"
        )
        wakeLock.acquire(10_000L)  // máximo 10s; lo soltamos antes manualmente

        try {
            crearCanales(context)

            when {
                prioridad.equals("Alta", ignoreCase = true)  -> mostrarAlarmaPrioridadAlta(context, idLocal, titulo, fecha, hora, momento)
                prioridad.equals("Baja", ignoreCase = true)  -> mostrarNotificacionBaja(context, idLocal, titulo, hora, momento)
                else                                          -> mostrarNotificacionMedia(context, idLocal, titulo, hora, momento)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /**
     * Prioridad BAJA: notificación silenciosa, sin sonido ni heads-up.
     * Solo aparece en la barra de estado para revisar después. Recordatorio "suave".
     */
    private fun mostrarNotificacionBaja(
        context: Context,
        idLocal: Long,
        titulo: String,
        hora: String,
        momento: EventoAlarmManager.MomentoRecordatorio
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, idLocal.toInt() + momento.offsetRequestCode, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (encabezado, cuerpo) = textosPara(momento, hora)

        val notif = NotificationCompat.Builder(context, CANAL_BAJA_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(titulo.ifBlank { encabezado })
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        nm.notify(idLocal.toInt() + momento.offsetRequestCode, notif)
        Log.d(TAG, "Notificación Baja posteada (silenciosa)")
    }

    /**
     * Prioridad MEDIA: heads-up tipo WhatsApp.
     * El banner sale arriba con el contenido visible, hace sonido, vibra.
     * El usuario NO necesita bajar el panel para ver de qué se trata.
     */
    private fun mostrarNotificacionMedia(
        context: Context,
        idLocal: Long,
        titulo: String,
        hora: String,
        momento: EventoAlarmManager.MomentoRecordatorio
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, idLocal.toInt() + momento.offsetRequestCode, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (encabezado, cuerpo) = textosPara(momento, hora)

        // Vibration pattern corto para que se sienta sin ser molesto
        val vibracion = longArrayOf(0, 250, 250, 250)

        val notif = NotificationCompat.Builder(context, CANAL_MEDIA_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(titulo.ifBlank { encabezado })
            .setContentText(cuerpo)
            // BigText hace que se vea el cuerpo expandido SIN tener que tocar la notif
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            // PRIORITY_HIGH dispara el heads-up (banner que cubre arriba con sonido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(vibracion)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            // setVisibility PUBLIC permite ver el contenido en pantalla bloqueada
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        nm.notify(idLocal.toInt() + momento.offsetRequestCode, notif)
        Log.d(TAG, "Notificación Media posteada (heads-up)")
    }

    /**
     * Para prioridad Alta: notificación full-screen + activity tipo alarma.
     *
     * Android divide los casos:
     *  - Pantalla bloqueada/apagada → setFullScreenIntent dispara la activity automáticamente
     *  - Pantalla encendida usando otra app → solo aparece como heads-up
     *
     * Como el usuario quiere comportamiento de "despertador" (siempre full-screen),
     * además llamamos manualmente a startActivity. Es legítimo porque categorizamos
     * la notificación como CATEGORY_ALARM y la app es de "alarmas/recordatorios".
     */
    private fun mostrarAlarmaPrioridadAlta(
        context: Context,
        idLocal: Long,
        titulo: String,
        fecha: String,
        hora: String,
        momento: EventoAlarmManager.MomentoRecordatorio
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = idLocal.toInt() + momento.offsetRequestCode

        // Activity full-screen (cuando la pantalla está bloqueada/apagada)
        val fullIntent = Intent(context, AlarmaEventoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EventoAlarmManager.EXTRA_ID_LOCAL, idLocal)
            putExtra(EventoAlarmManager.EXTRA_TITULO, titulo)
            putExtra(EventoAlarmManager.EXTRA_FECHA, fecha)
            putExtra(EventoAlarmManager.EXTRA_HORA, hora)
            putExtra(EventoAlarmManager.EXTRA_MOMENTO, momento.name)
        }
        val fullPending = PendingIntent.getActivity(
            context, notifId, fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap normal (cuando ven el banner heads-up con la pantalla encendida)
        val tapPending = PendingIntent.getActivity(
            context, notifId + 500_000, fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (encabezado, cuerpo) = textosPara(momento, hora)

        val notif = NotificationCompat.Builder(context, CANAL_ALARMA_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(titulo.ifBlank { encabezado })
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tapPending)
            .setFullScreenIntent(fullPending, true)  // ← dispara la activity en pantalla bloqueada
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notif)
        Log.d(TAG, "Notificación Alta posteada (id=$notifId)")

        // Además: lanzar la activity directamente. El setFullScreenIntent solo se
        // dispara con pantalla apagada — pero el usuario quiere experiencia tipo
        // despertador SIEMPRE. CATEGORY_ALARM + paquete propio justifica lanzar.
        try {
            context.startActivity(fullIntent)
            Log.d(TAG, "AlarmaEventoActivity lanzada manualmente (Alta)")
        } catch (e: Exception) {
            // En Android 10+ algunas restricciones impiden lanzar activities desde
            // background sin permiso especial — la notificación full-screen sigue
            // como respaldo.
            Log.w(TAG, "No se pudo lanzar AlarmaEventoActivity manualmente (caerá al setFullScreenIntent): ${e.message}")
        }
    }

    /**
     * Devuelve (encabezado, cuerpo) de la notificación según el momento del aviso.
     * El encabezado se usa cuando el evento no tiene título (raro pero posible).
     */
    private fun textosPara(
        momento: EventoAlarmManager.MomentoRecordatorio,
        hora: String
    ): Pair<String, String> = when (momento) {
        EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES -> {
            val cuerpo = if (hora.isNotBlank())
                "Tu evento es en 1 hora ($hora)"
            else
                "Tu evento es en 1 hora"
            "Próximo evento" to cuerpo
        }
        EventoAlarmManager.MomentoRecordatorio.EN_LA_HORA -> {
            val cuerpo = if (hora.isNotBlank())
                "¡Ya es la hora ($hora)! Tu evento empieza ahora"
            else
                "¡Ya es la hora! Tu evento empieza ahora"
            "Es la hora" to cuerpo
        }
    }

    /**
     * Crea los canales de notificación si todavía no existen. Idempotente.
     *
     * IMPORTANTE: en Android los canales se crean UNA vez al instalar la app y
     * el usuario controla su importance desde ajustes. Si cambias `IMPORTANCE_*`
     * en código no se actualiza — el usuario tiene que ir a ajustes O hay que
     * usar un nuevo CHANNEL_ID. Por eso los IDs aquí son v2 (cambio de comportamiento).
     */
    private fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal BAJA — silencioso, sin heads-up. Recordatorio "suave".
        if (nm.getNotificationChannel(CANAL_BAJA_ID) == null) {
            val canal = NotificationChannel(
                CANAL_BAJA_ID,
                "Recordatorios suaves (Baja)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Eventos de baja prioridad. Solo aparece en la barra, sin sonido."
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(canal)
        }

        // Canal MEDIA — heads-up tipo WhatsApp con sonido y vibración corta.
        // IMPORTANCE_HIGH es lo que dispara el banner emergente con el contenido visible.
        if (nm.getNotificationChannel(CANAL_MEDIA_ID) == null) {
            val canal = NotificationChannel(
                CANAL_MEDIA_ID,
                "Recordatorios de eventos (Media)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Eventos de prioridad media. Banner emergente, sonido y vibración."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(canal)
        }

        // Canal ALARMA — para Alta. IMPORTANCE_HIGH + bypass DND para que sea
        // realmente disruptivo cuando el usuario lo necesita.
        if (nm.getNotificationChannel(CANAL_ALARMA_ID) == null) {
            val canal = NotificationChannel(
                CANAL_ALARMA_ID,
                "Eventos prioritarios (Alta)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Eventos de alta prioridad. Suena como alarma y abre pantalla completa."
                enableVibration(true)
                setBypassDnd(true)  // suena aunque esté en No Molestar
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(canal)
        }
    }

    companion object {
        private const val TAG = "RecordatorioReceiver"
        // _v2 porque cambiamos el comportamiento (IMPORTANCE) — el ID viejo
        // sigue existiendo en dispositivos donde ya estaba instalado, así que
        // forzamos canales nuevos para garantizar que el usuario reciba el heads-up.
        const val CANAL_BAJA_ID   = "recordatorios_baja_v2"
        const val CANAL_MEDIA_ID  = "recordatorios_media_v2"
        const val CANAL_ALARMA_ID = "alarma_eventos_alta_v2"
    }
}
