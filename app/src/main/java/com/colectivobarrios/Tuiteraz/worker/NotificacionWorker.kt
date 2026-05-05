package com.colectivobarrios.Tuiteraz.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * @deprecated Reemplazado por [ActualizadorWidgetWorker], que corre a una hora
 * exacta (4 AM hora local) en lugar de en cualquier momento de una ventana de
 * 24h. Esta clase se mantiene únicamente como stub para no romper código que
 * pudiera referenciarla por nombre. No se programa desde ningún lado.
 */
@Deprecated(
    message = "Usar ActualizadorWidgetWorker en su lugar",
    replaceWith = ReplaceWith("ActualizadorWidgetWorker", "com.colectivobarrios.Tuiteraz.worker.ActualizadorWidgetWorker")
)
class NotificacionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = Result.success()
}
