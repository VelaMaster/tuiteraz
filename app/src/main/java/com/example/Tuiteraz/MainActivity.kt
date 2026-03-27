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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.Tuiteraz.data.local.CacheFrases
import com.example.Tuiteraz.data.network.ProveedorFrasesRepository
import com.example.Tuiteraz.data.network.SupabaseManager
import com.example.Tuiteraz.ui.theme.BalanceTheme
import com.example.Tuiteraz.ui.viewmodel.FraseDelDiaViewModel
import com.example.Tuiteraz.worker.NotificacionWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPrefs = getSharedPreferences("TuiterazPrefs", Context.MODE_PRIVATE)

        // 1. Preparamos las dependencias de nuestra nueva arquitectura escalable
        val cacheLocal = CacheFrases(applicationContext)
        val repositorio = ProveedorFrasesRepository(applicationContext, SupabaseManager.client, cacheLocal)

        // 2. Creamos una "Fábrica" que le enseñe a Compose cómo construir tu ViewModel
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FraseDelDiaViewModel::class.java)) {
                    return FraseDelDiaViewModel(repositorio) as T
                }
                throw IllegalArgumentException("ViewModel desconocido")
            }
        }

        setContent {
            val sistemaOscuro = isSystemInDarkTheme()

            var isNotificacionesActivas by remember {
                mutableStateOf(sharedPrefs.getBoolean("notifs_activas", false))
            }

            // 3. Instanciamos el ViewModel usando nuestra fábrica
            val fraseViewModel: FraseDelDiaViewModel = viewModel(factory = viewModelFactory)

            BalanceTheme(oscuro = sistemaOscuro) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipalConNavegacion(
                        fraseViewModel = fraseViewModel, // <--- Pasamos el ViewModel en lugar de la frase quemada
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