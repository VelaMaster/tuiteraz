package com.colectivobarrios.Tuiteraz.notificaciones

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colectivobarrios.Tuiteraz.ui.theme.BalanceTheme
import kotlinx.coroutines.delay

/**
 * Activity full-screen que se lanza cuando dispara una alarma de prioridad Alta.
 *
 * Características:
 *  - Aparece sobre la pantalla de bloqueo (showWhenLocked + turnScreenOn)
 *  - Reproduce el ringtone de alarma del sistema en bucle
 *  - Vibra en patrón hasta que el usuario interactúa
 *  - Botón grande para "Entendido"
 *
 * Si el usuario no interactúa en 60 segundos, se auto-descarta (fail-safe).
 */
class AlarmaEventoActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var idLocal: Long = 0L
    private var titulo: String = ""

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostrar SOBRE el lock screen y prender la pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // Mantener la pantalla encendida mientras la activity está visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        idLocal   = intent?.getLongExtra(EventoAlarmManager.EXTRA_ID_LOCAL, 0L) ?: 0L
        titulo    = intent?.getStringExtra(EventoAlarmManager.EXTRA_TITULO).orEmpty()
        val fecha = intent?.getStringExtra(EventoAlarmManager.EXTRA_FECHA).orEmpty()
        val hora  = intent?.getStringExtra(EventoAlarmManager.EXTRA_HORA).orEmpty()
        val momentoStr = intent?.getStringExtra(EventoAlarmManager.EXTRA_MOMENTO)
            ?: EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES.name
        val momento = try {
            EventoAlarmManager.MomentoRecordatorio.valueOf(momentoStr)
        } catch (_: Exception) {
            EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES
        }

        iniciarSonidoYVibracion()

        setContent {
            BalanceTheme {
                PantallaAlarma(
                    titulo = titulo.ifBlank { "Evento prioritario" },
                    fecha = fecha,
                    hora = hora,
                    momento = momento,
                    onDescartar = { descartar() }
                )
            }
        }
    }

    private fun iniciarSonidoYVibracion() {
        // Sonido: ringtone de alarma del sistema, en bucle hasta que se descarte
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmaEventoActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Si falla el media (raro), seguimos al menos con la vibración
        }

        // Vibración en patrón largo
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        try {
            val patron = longArrayOf(0, 800, 400, 800, 400)  // espera, vibra, pausa, vibra, pausa…
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(patron, 0))  // 0 = repetir desde el inicio
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(patron, 0)
            }
        } catch (_: Exception) {
            // Algunos dispositivos sin vibrador o sin permiso ignoran esto
        }
    }

    private fun detenerSonidoYVibracion() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
    }

    private fun descartar() {
        detenerSonidoYVibracion()
        // También limpiamos la notificación que disparó esta activity
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(idLocal.toInt())
        } catch (_: Exception) {}
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerSonidoYVibracion()
    }
}

@Composable
private fun PantallaAlarma(
    titulo: String,
    fecha: String,
    hora: String,
    momento: EventoAlarmManager.MomentoRecordatorio,
    onDescartar: () -> Unit
) {
    // Auto-descarte de seguridad a los 60s para no dejar la alarma sonando
    // si el usuario está dormido o ignora el teléfono
    LaunchedEffect(Unit) {
        delay(60_000)
        onDescartar()
    }

    // Pulso animado del icono para llamar la atención visualmente
    var pulso by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            pulso = !pulso
            delay(700)
        }
    }
    val escalaIcono by animateFloatAsState(
        targetValue = if (pulso) 1.12f else 1f,
        animationSpec = tween(700),
        label = "pulso_icono"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icono pulsante en círculo de error
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(escalaIcono)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Etiqueta superior cambia según el momento del aviso
            val etiqueta = when (momento) {
                EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES -> "EVENTO PRIORITARIO"
                EventoAlarmManager.MomentoRecordatorio.EN_LA_HORA      -> "ES LA HORA"
            }
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (fecha.isNotBlank() || hora.isNotBlank()) {
                val partes = listOfNotNull(
                    fecha.takeIf { it.isNotBlank() },
                    hora.takeIf { it.isNotBlank() }
                )
                // Texto de subtítulo distinto según el momento
                val prefijo = when (momento) {
                    EventoAlarmManager.MomentoRecordatorio.UNA_HORA_ANTES -> "En 1 hora"
                    EventoAlarmManager.MomentoRecordatorio.EN_LA_HORA      -> "Tu evento empieza ahora"
                }
                Text(
                    text = "$prefijo · ${partes.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón único grande para descartar
            Button(
                onClick = onDescartar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    "Entendido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
