package com.colectivobarrios.Tuiteraz.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.colectivobarrios.Tuiteraz.widget.FraseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reprograma el ActualizadorWidgetWorker después de un reinicio del dispositivo.
 *
 * Cuando el teléfono se apaga/reinicia, todos los WorkRequest pendientes en
 * WorkManager se preservan en disco y se reaniman al boot — pero solo si el
 * delay original NO se cumplió. Si el usuario apaga el teléfono a las 11pm y
 * lo prende a las 5am, el worker programado para las 4am ya pasó su ventana
 * y nunca correrá. Por eso es buena práctica forzar reprogramación al boot.
 *
 * También actualiza el widget al instante para que muestre la frase del caché
 * (no la versión vacía/genérica con la que arrancan los widgets tras boot).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val accion = intent?.action ?: return
        Log.d("BootReceiver", "Recibido: $accion")

        if (accion == Intent.ACTION_BOOT_COMPLETED ||
            accion == Intent.ACTION_MY_PACKAGE_REPLACED ||
            accion == "android.intent.action.QUICKBOOT_POWERON"  // boot rápido en algunos OEMs (Samsung, Huawei)
        ) {
            // 1. Reprogramamos el worker para las próximas 4 AM
            ActualizadorWidgetWorker.programarParaProxima4AM(context, reemplazar = true)

            // 2. Refrescamos el widget de inmediato con lo que haya en caché.
            //    goAsync() le dice al sistema que esperamos a que termine la operación
            //    (sino el broadcast termina antes y nuestro coroutine se cancela).
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    FraseWidget().updateAll(context)
                    Log.d("BootReceiver", "Widget refrescado tras boot")
                } catch (e: Exception) {
                    Log.w("BootReceiver", "Error refrescando widget tras boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
