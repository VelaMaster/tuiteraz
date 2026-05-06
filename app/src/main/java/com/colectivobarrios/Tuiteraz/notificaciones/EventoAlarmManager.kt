package com.colectivobarrios.Tuiteraz.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.colectivobarrios.Tuiteraz.Evento
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Encapsula toda la lógica de programación de notificaciones por evento.
 *
 * Reglas:
 *  - Solo se programa si evento.recordatorio == true
 *  - Cada evento dispara DOS alarmas:
 *      · MOMENTO.UNA_HORA_ANTES → "Tu evento es en 1 hora"
 *      · MOMENTO.EN_LA_HORA      → "¡Ya es la hora!"
 *  - El tipo de aviso lo decide el receiver según evento.prioridad:
 *      * Baja / Media → notificación normal
 *      * Alta         → full-screen intent (alarma) que abre AlarmaEventoActivity
 *  - Si una hora de trigger ya pasó (porque el usuario crea un evento muy cercano
 *    o pasado), esa alarma específica no se programa, pero las demás sí.
 *    Por ejemplo: si crea un evento dentro de 30 min, la alarma "1h antes" se
 *    salta pero la "en la hora" sí se programa.
 *
 * Usa AlarmManager con setExactAndAllowWhileIdle (o setAlarmClock para Alta) para
 * que dispare aunque el dispositivo esté en Doze. Es la única forma confiable
 * de garantizar que un recordatorio llegue a tiempo en Android 6+.
 *
 * Como hay 2 alarmas por evento, el requestCode se compone de
 * idLocal + offset(momento) — ambos PendingIntents son distintos así pueden
 * convivir sin pisarse.
 */
object EventoAlarmManager {

    private const val TAG = "EventoAlarmManager"

    /** Cuánto antes del evento dispara la PRIMERA alarma. */
    private const val MINUTOS_ANTES = 60L

    /** Acción del Intent que recibe el BroadcastReceiver. */
    const val ACCION_RECORDATORIO = "com.colectivobarrios.Tuiteraz.ACCION_RECORDATORIO_EVENTO"

    /** Extras que el receiver/activity necesitan para mostrar el aviso. */
    const val EXTRA_ID_LOCAL  = "id_local"
    const val EXTRA_TITULO    = "titulo"
    const val EXTRA_FECHA     = "fecha"
    const val EXTRA_HORA      = "hora"
    const val EXTRA_PRIORIDAD = "prioridad"
    const val EXTRA_MOMENTO   = "momento"

    /**
     * Identifica si el aviso es el preliminar (1h antes) o el final (en la hora).
     * Cada uno usa un offset distinto para el requestCode del PendingIntent así
     * ambas alarmas conviven sin sobreescribirse.
     *
     * El offset 1_000_000 asume que la app no superará 1M de eventos — más que
     * razonable. Si llegara a pasar, idLocal supera el offset y empieza a colisionar.
     */
    enum class MomentoRecordatorio(val offsetRequestCode: Int) {
        UNA_HORA_ANTES(0),
        EN_LA_HORA(1_000_000)
    }

    /** Resultado de un intento de programación, útil para informar al usuario. */
    enum class ResultadoProgramacion {
        /** Al menos una alarma programada con éxito a hora exacta. */
        OK_EXACTA,
        /** Programada pero inexacta (puede llegar con minutos de retraso). */
        OK_INEXACTA,
        /** Cancelada porque recordatorio=false o todas las horas ya pasaron. */
        CANCELADA,
        /** No tenemos permiso de notificaciones — el usuario debe activarlo en ajustes. */
        FALTA_PERMISO_NOTIFICACIONES,
        /** No tenemos permiso de alarmas exactas — el usuario debe activarlo en ajustes. */
        FALTA_PERMISO_EXACTAS,
        /** No tenemos permiso de full-screen intent (Android 14+) — alarma de Alta no disparará en lock screen. */
        FALTA_PERMISO_FULLSCREEN,
        /** El sistema está optimizando la batería de la app — alarmas en background no son confiables. */
        FALTA_EXCLUSION_BATERIA,
        /** Error inesperado. */
        ERROR
    }

    /**
     * Programa los recordatorios para un evento (1h antes + en la hora exacta).
     * Si ya había alarmas con el mismo idLocal, las reemplaza. Si recordatorio=false
     * o todas las horas calculadas ya pasaron, cancela cualquier alarma previa.
     */
    fun programar(context: Context, evento: Evento): ResultadoProgramacion {
        val idLocal = evento.idLocal
        if (idLocal == 0L) {
            Log.w(TAG, "❌ No se puede programar sin idLocal (titulo='${evento.titulo}')")
            return ResultadoProgramacion.ERROR
        }

        Log.d(TAG, "▶ programar() id=$idLocal titulo='${evento.titulo}' fecha=${evento.fecha} hora=${evento.hora} prioridad=${evento.prioridad} recordatorio=${evento.recordatorio}")

        // Si el usuario apagó el switch de recordatorio, asegurarse de borrar
        // cualquier alarma que hubiera quedado de un estado anterior.
        if (!evento.recordatorio) {
            Log.d(TAG, "  · Recordatorio desactivado → cancelando todas las alarmas previas")
            cancelar(context, idLocal)
            return ResultadoProgramacion.CANCELADA
        }

        val fechaHoraEvento = calcularFechaHoraEvento(evento)
        if (fechaHoraEvento == null) {
            Log.w(TAG, "  · ❌ Fecha/hora inválida → no se programa")
            cancelar(context, idLocal)
            return ResultadoProgramacion.ERROR
        }

        // Verificamos permisos UNA sola vez para no spammear el log con dos checks
        val estadoPermisos = PermisoNotificacionesHelper.verificar(context)
        Log.d(TAG, "  · Permisos: notif=${estadoPermisos.notificacionesActivas} exactas=${estadoPermisos.puedeAlarmasExactas} fullscreen=${estadoPermisos.puedeFullScreenIntent}")

        if (!estadoPermisos.notificacionesActivas) {
            Log.w(TAG, "  · ❌ Sin permiso POST_NOTIFICATIONS — las alarmas se programarán pero no podrán notificar")
        }

        // Programamos las DOS alarmas. Cada una puede fallar independientemente
        // (típicamente la "1h antes" falla cuando el evento se crea con hora
        // muy cercana). Si AL MENOS UNA quedó programada, consideramos OK.
        val triggerAntesMillis = fechaHoraEvento.minusMinutes(MINUTOS_ANTES)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val triggerEnHoraMillis = fechaHoraEvento
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val resultadoAntes  = programarUna(context, evento, triggerAntesMillis,  MomentoRecordatorio.UNA_HORA_ANTES, estadoPermisos)
        val resultadoEnHora = programarUna(context, evento, triggerEnHoraMillis, MomentoRecordatorio.EN_LA_HORA,     estadoPermisos)

        return combinarResultados(resultadoAntes, resultadoEnHora, estadoPermisos, evento)
    }

    /**
     * Programa una sola de las dos alarmas. Si el trigger ya pasó cancela esta
     * alarma específica (por si quedó de un estado anterior) y devuelve CANCELADA.
     */
    private fun programarUna(
        context: Context,
        evento: Evento,
        triggerMillis: Long,
        momento: MomentoRecordatorio,
        estadoPermisos: PermisoNotificacionesHelper.EstadoPermisos
    ): ResultadoProgramacion {
        val idLocal = evento.idLocal
        val ahora = System.currentTimeMillis()
        val triggerLegible = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerMillis),
            ZoneId.systemDefault()
        )
        Log.d(TAG, "  ▸ ${momento.name}: trigger=$triggerLegible (en ${(triggerMillis - ahora) / 1000 / 60} min)")

        if (triggerMillis <= ahora) {
            Log.d(TAG, "    · ⏰ Ya pasó → cancelo si existía")
            cancelarUna(context, idLocal, momento)
            return ResultadoProgramacion.CANCELADA
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = construirIntent(context, evento, momento)
        val requestCode = idLocal.toInt() + momento.offsetRequestCode
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val puedeExacta = estadoPermisos.puedeAlarmasExactas
        val esAlta = evento.prioridad.equals("Alta", ignoreCase = true)

        return try {
            when {
                esAlta && puedeExacta -> {
                    val showIntent = PendingIntent.getActivity(
                        context, requestCode,
                        Intent(context, com.colectivobarrios.Tuiteraz.MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                        pendingIntent
                    )
                    Log.d(TAG, "    · ✅ setAlarmClock (Alta + exacta)")
                }
                puedeExacta -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                    )
                    Log.d(TAG, "    · ✅ setExactAndAllowWhileIdle (exacta)")
                }
                else -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                    )
                    Log.w(TAG, "    · ⚠️ setAndAllowWhileIdle (INEXACTA — usuario debe activar 'Alarmas y recordatorios')")
                }
            }
            if (puedeExacta) ResultadoProgramacion.OK_EXACTA else ResultadoProgramacion.OK_INEXACTA
        } catch (e: SecurityException) {
            Log.e(TAG, "    · ❌ SecurityException: ${e.message}", e)
            ResultadoProgramacion.FALTA_PERMISO_EXACTAS
        } catch (e: Exception) {
            Log.e(TAG, "    · ❌ Error: ${e.message}", e)
            ResultadoProgramacion.ERROR
        }
    }

    /**
     * Combina los dos resultados de programación + el estado global de permisos
     * en un único [ResultadoProgramacion] que la UI usa para decidir qué diálogo
     * mostrar (si alguno).
     *
     * Orden de prioridad (de "más crítico" a "menos"):
     *  1. POST_NOTIFICATIONS: sin esto NADA va a notificar. Lo bloqueante #1.
     *  2. Optimización batería: sin esto las alarmas no disparan en background.
     *     Es el problema típico de Samsung/Xiaomi.
     *  3. Alarmas exactas: sin esto las alarmas son inexactas (pueden retrasarse minutos).
     *  4. Full-screen (solo si es Alta): sin esto la alarma no aparece sobre lock screen.
     *  5. OK_EXACTA / OK_INEXACTA / CANCELADA según el caso.
     */
    private fun combinarResultados(
        a: ResultadoProgramacion,
        b: ResultadoProgramacion,
        permisos: PermisoNotificacionesHelper.EstadoPermisos,
        evento: Evento
    ): ResultadoProgramacion {
        // 1. Sin notificaciones, nada importa
        if (!permisos.notificacionesActivas) return ResultadoProgramacion.FALTA_PERMISO_NOTIFICACIONES

        // 2. Sin exclusión de batería, las alarmas en background no son confiables
        if (!permisos.ignoraOptimizacionBateria) return ResultadoProgramacion.FALTA_EXCLUSION_BATERIA

        // 3. Sin alarmas exactas, las alarmas se retrasan
        if (a == ResultadoProgramacion.FALTA_PERMISO_EXACTAS ||
            b == ResultadoProgramacion.FALTA_PERMISO_EXACTAS) return ResultadoProgramacion.FALTA_PERMISO_EXACTAS

        // 4. Si es Alta y no tenemos full-screen, avisamos (la alarma sí va a sonar como
        //    notificación, pero NO se mostrará la pantalla full-screen)
        val esAlta = evento.prioridad.equals("Alta", ignoreCase = true)
        if (esAlta && !permisos.puedeFullScreenIntent) return ResultadoProgramacion.FALTA_PERMISO_FULLSCREEN

        // 5. Resultado de éxito según lo que se programó
        if (a == ResultadoProgramacion.OK_EXACTA || b == ResultadoProgramacion.OK_EXACTA) return ResultadoProgramacion.OK_EXACTA
        if (a == ResultadoProgramacion.OK_INEXACTA || b == ResultadoProgramacion.OK_INEXACTA) return ResultadoProgramacion.OK_INEXACTA
        if (a == ResultadoProgramacion.CANCELADA && b == ResultadoProgramacion.CANCELADA) return ResultadoProgramacion.CANCELADA
        return ResultadoProgramacion.ERROR
    }

    /** Cancela TODAS las alarmas asociadas a un idLocal (1h antes y en la hora). */
    fun cancelar(context: Context, idLocal: Long) {
        if (idLocal == 0L) return
        MomentoRecordatorio.values().forEach { momento ->
            cancelarUna(context, idLocal, momento)
        }
    }

    /** Cancela una alarma específica (un solo momento de un evento). */
    private fun cancelarUna(context: Context, idLocal: Long, momento: MomentoRecordatorio) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RecordatorioEventoReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
        }
        val requestCode = idLocal.toInt() + momento.offsetRequestCode
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            // NO_CREATE: si no existe, devuelve null y no creamos un PendingIntent fantasma
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarma cancelada id=$idLocal momento=${momento.name}")
        }
    }

    /** Parsea fecha+hora del evento a LocalDateTime, o null si está mal formada. */
    private fun calcularFechaHoraEvento(evento: Evento): LocalDateTime? {
        return try {
            val fecha = java.time.LocalDate.parse(evento.fecha)
            val hora = parseHoraFlexible(evento.hora) ?: return null
            LocalDateTime.of(fecha, hora)
        } catch (e: Exception) {
            Log.w(TAG, "Error parseando fecha/hora '${evento.fecha} ${evento.hora}': ${e.message}")
            null
        }
    }

    private fun parseHoraFlexible(textoHora: String): LocalTime? {
        val limpio = textoHora.trim()
        // Formato 24h: "HH:mm"
        try {
            return LocalTime.parse(limpio, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {}
        // Formato 12h: "h:mm a" (ej: "3:30 PM")
        try {
            return LocalTime.parse(limpio.uppercase(), DateTimeFormatter.ofPattern("h:mm a"))
        } catch (_: Exception) {}
        return null
    }

    private fun construirIntent(context: Context, evento: Evento, momento: MomentoRecordatorio): Intent {
        return Intent(context, RecordatorioEventoReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
            putExtra(EXTRA_ID_LOCAL, evento.idLocal)
            putExtra(EXTRA_TITULO, evento.titulo)
            putExtra(EXTRA_FECHA, evento.fecha)
            putExtra(EXTRA_HORA, evento.hora)
            putExtra(EXTRA_PRIORIDAD, evento.prioridad)
            putExtra(EXTRA_MOMENTO, momento.name)
        }
    }
}
