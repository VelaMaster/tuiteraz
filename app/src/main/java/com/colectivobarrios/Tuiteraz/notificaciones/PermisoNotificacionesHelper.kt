package com.colectivobarrios.Tuiteraz.notificaciones

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Centraliza los checks de permisos relacionados con notificaciones.
 *
 * Hay 3 candados independientes que pueden hacer que un recordatorio NO dispare,
 * y los 3 fallan en silencio si no los chequeas antes:
 *
 *  1. POST_NOTIFICATIONS (Android 13+) — runtime permission. Sin esto, el
 *     sistema descarta cualquier notificación que mostremos.
 *
 *  2. SCHEDULE_EXACT_ALARM (Android 12+) — permiso "especial" en ajustes.
 *     Sin esto, AlarmManager.setExactAndAllowWhileIdle lanza SecurityException
 *     y la alarma nunca se programa.
 *
 *  3. USE_FULL_SCREEN_INTENT (Android 14+) — también permiso especial. Sin
 *     esto, la AlarmaEventoActivity no aparece sobre el lock screen, solo
 *     se ve como heads-up notification (banner arriba).
 *
 * También verificamos que las notificaciones de la app no estén deshabilitadas
 * a nivel de aplicación o canal, lo cual es una situación distinta a "no di
 * el permiso runtime".
 */
object PermisoNotificacionesHelper {

    /** Resultado del check completo, listo para mostrar UI explicativa. */
    data class EstadoPermisos(
        val notificacionesActivas: Boolean,
        val puedeAlarmasExactas: Boolean,
        val puedeFullScreenIntent: Boolean,
        val ignoraOptimizacionBateria: Boolean
    ) {
        /** ¿Puede recibir cualquier tipo de recordatorio? */
        val puedeRecibirRecordatorios: Boolean
            get() = notificacionesActivas && puedeAlarmasExactas

        /** ¿Puede recibir alarmas full-screen para prioridad Alta? */
        val puedeRecibirAlta: Boolean
            get() = puedeRecibirRecordatorios && puedeFullScreenIntent

        /** ¿Puede recibir alarmas funcionalmente cuando la app está en background? */
        val puedeBackground: Boolean
            get() = puedeRecibirRecordatorios && ignoraOptimizacionBateria
    }

    fun verificar(context: Context): EstadoPermisos = EstadoPermisos(
        notificacionesActivas      = notificacionesActivas(context),
        puedeAlarmasExactas        = puedeAlarmasExactas(context),
        puedeFullScreenIntent      = puedeFullScreenIntent(context),
        ignoraOptimizacionBateria  = ignoraOptimizacionBateria(context)
    )

    /**
     * ¿Puedo postear notificaciones?
     * Combina runtime permission + ajustes a nivel de app.
     */
    fun notificacionesActivas(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * ¿Puedo programar alarmas a hora EXACTA?
     * En Android 12+ requiere SCHEDULE_EXACT_ALARM o USE_EXACT_ALARM.
     * El usuario lo concede en una pantalla especial de ajustes.
     */
    fun puedeAlarmasExactas(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /**
     * ¿Puedo lanzar full-screen intents (alarmas tipo despertador)?
     * En Android 14+ requiere USE_FULL_SCREEN_INTENT en el manifest +
     * permiso especial en ajustes.
     */
    fun puedeFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true  // API 34
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }

    /**
     * ¿Está la app exenta de las optimizaciones de batería del sistema?
     *
     * Esta es LA causa más común de "las notificaciones no llegan cuando cierro la app",
     * sobre todo en dispositivos Samsung (One UI), Xiaomi (MIUI), Huawei, etc. que
     * tienen capas de optimización agresivas que matan las apps en background.
     *
     * Si devuelve false, las alarmas pueden no dispararse cuando la app está cerrada
     * o el dispositivo lleva mucho tiempo sin uso (Doze profundo).
     */
    fun ignoraOptimizacionBateria(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // ── INTENTS PARA ABRIR AJUSTES ──────────────────────────────────────────

    /** Lleva a la pantalla de notificaciones de la app. */
    fun intentAjustesNotificaciones(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
    }

    /** Lleva a "Alarmas y recordatorios" de la app (Android 12+). */
    fun intentAjustesAlarmasExactas(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.fromParts("package", context.packageName, null))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
    }

    /** Lleva a "Mostrar pantalla completa" (Android 14+). */
    fun intentAjustesFullScreen(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.fromParts("package", context.packageName, null))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
    }

    /**
     * Lleva al diálogo del sistema que pide al usuario desactivar la optimización
     * de batería para la app. Si lo acepta, las alarmas se dispararán de forma
     * confiable incluso con la app cerrada.
     *
     * Nota: Google desaconseja usar este intent en apps que NO sean de tipo
     * "alarma/despertador/health" — para nosotros se justifica porque sí somos
     * una app que necesita despertar al usuario para eventos prioritarios.
     */
    @Suppress("BatteryLife")  // ver nota arriba
    fun intentDesactivarOptimizacionBateria(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
        }
    }
}
