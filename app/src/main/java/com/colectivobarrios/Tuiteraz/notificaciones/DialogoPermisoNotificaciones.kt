package com.colectivobarrios.Tuiteraz.notificaciones

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Diálogo que aparece cuando el usuario activa "Recordatorio" en un evento pero
 * a la app le falta un permiso del sistema para que la notificación realmente
 * dispare. Cada caso lleva al usuario al lugar correcto en ajustes.
 *
 * Lo lanza la pantalla de eventos al observar [EventosViewModel.problemaPermiso].
 */
@Composable
fun DialogoPermisoNotificaciones(
    problema: EventoAlarmManager.ResultadoProgramacion,
    onDismiss: () -> Unit
) {
    val contexto = LocalContext.current

    val titulo: String
    val mensaje: String
    val textoBotonAccion: String
    val accionAjustes: () -> Unit

    when (problema) {
        EventoAlarmManager.ResultadoProgramacion.FALTA_PERMISO_NOTIFICACIONES -> {
            titulo = "Activa las notificaciones"
            mensaje = "Tu evento se guardó, pero las notificaciones de Tuiteraz están " +
                    "desactivadas. Sin esto no vas a recibir ningún recordatorio.\n\n" +
                    "Toca «Abrir ajustes» y activa las notificaciones de la app."
            textoBotonAccion = "Abrir ajustes"
            accionAjustes = {
                contexto.startActivity(PermisoNotificacionesHelper.intentAjustesNotificaciones(contexto))
            }
        }
        EventoAlarmManager.ResultadoProgramacion.FALTA_EXCLUSION_BATERIA -> {
            titulo = "Excluye la app de la optimización de batería"
            mensaje = "Tu recordatorio quedó programado, PERO si cierras la app, " +
                    "Android puede impedir que las notificaciones lleguen a tiempo " +
                    "(o que no lleguen del todo). Esto pasa sobre todo en Samsung, " +
                    "Xiaomi y Huawei.\n\n" +
                    "Toca «Permitir» para que Tuiteraz pueda despertarte aunque la " +
                    "app esté cerrada. Es seguro: no consume batería extra, solo evita " +
                    "que el sistema la suspenda."
            textoBotonAccion = "Permitir"
            accionAjustes = {
                contexto.startActivity(PermisoNotificacionesHelper.intentDesactivarOptimizacionBateria(contexto))
            }
        }
        EventoAlarmManager.ResultadoProgramacion.FALTA_PERMISO_EXACTAS -> {
            titulo = "Activa «Alarmas y recordatorios»"
            mensaje = "Tu recordatorio se programó pero podría llegar con minutos de retraso. " +
                    "Para que dispare a la hora exacta —y para que la alarma de prioridad ALTA " +
                    "funcione bien—, activa el permiso «Alarmas y recordatorios» en ajustes.\n\n" +
                    "Es un permiso especial de Android 12+ que protege la batería."
            textoBotonAccion = "Abrir ajustes"
            accionAjustes = {
                contexto.startActivity(PermisoNotificacionesHelper.intentAjustesAlarmasExactas(contexto))
            }
        }
        EventoAlarmManager.ResultadoProgramacion.FALTA_PERMISO_FULLSCREEN -> {
            titulo = "Activa «Mostrar pantalla completa»"
            mensaje = "Para que tu evento de prioridad ALTA suene como una alarma " +
                    "(con pantalla completa, sonido fuerte y vibración) Tuiteraz " +
                    "necesita el permiso «Mostrar pantalla completa».\n\n" +
                    "Sin este permiso, la alarma sí va a sonar, pero solo como una " +
                    "notificación normal en la barra superior."
            textoBotonAccion = "Abrir ajustes"
            accionAjustes = {
                contexto.startActivity(PermisoNotificacionesHelper.intentAjustesFullScreen(contexto))
            }
        }
        else -> {
            // Casos no accionables — no debería llegar aquí, pero por defensividad
            titulo = "Aviso"
            mensaje = "El recordatorio se guardó."
            textoBotonAccion = "Aceptar"
            accionAjustes = onDismiss
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(titulo) },
        text = {
            Column {
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                accionAjustes()
                onDismiss()
            }) {
                Text(textoBotonAccion)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Más tarde")
            }
        }
    )
}
