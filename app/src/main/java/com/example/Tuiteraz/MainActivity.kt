package com.example.Tuiteraz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.Tuiteraz.data.network.ProveedorFrases
import com.example.Tuiteraz.ui.theme.BalanceTheme
import com.example.Tuiteraz.worker.NotificacionWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPrefs = getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE)

        setContent {
            // 1. Ahora simplemente leemos el tema del celular en tiempo real
            val sistemaOscuro = isSystemInDarkTheme()

            var isNotificacionesActivas by remember {
                mutableStateOf(sharedPrefs.getBoolean("notifs_activas", false))
            }

            val fraseDelDia = ProveedorFrases.obtenerFraseDelDia()

            // 2. Le pasamos directamente el tema del celular al BalanceTheme
            BalanceTheme(oscuro = sistemaOscuro) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipalConNavegacion(
                        fraseActual = fraseDelDia,
                        isNotificacionesActivas = isNotificacionesActivas,
                        onNotificacionesChange = { activas ->
                            isNotificacionesActivas = activas
                            sharedPrefs.edit().putBoolean("notifs_activas", activas).apply()
                            gestionarNotificaciones(activas)
                        }
                    )
                }
            }
        }
    }

    private fun gestionarNotificaciones(activar: Boolean) {
        val workManager = WorkManager.getInstance(applicationContext)
        val workName = "NotificacionFraseDiaria"

        if (activar) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<NotificacionWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } else {
            workManager.cancelUniqueWork(workName)
        }
    }
}