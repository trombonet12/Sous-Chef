package app.souschef.data.actualizacion

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlin.concurrent.thread

/** Comprobación diaria de versión nueva en segundo plano, con red (ADR-0009). Sin WorkManager. */
class TrabajoActualizaciones : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        thread(name = "souschef-actualizaciones") {
            val ok = runCatching { ComprobadorActualizaciones(applicationContext).comprobar() }.getOrDefault(false)
            jobFinished(params, !ok)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val ID = 9_001
        private const val CADA_MS = 24 * 3_600_000L

        /** Idempotente: si ya está programado no lo toca (persiste tras reiniciar). */
        fun programar(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            if (js.getPendingJob(ID) != null) return
            js.schedule(
                JobInfo
                    .Builder(ID, ComponentName(context, TrabajoActualizaciones::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(CADA_MS)
                    .setPersisted(true)
                    .build(),
            )
        }
    }
}
