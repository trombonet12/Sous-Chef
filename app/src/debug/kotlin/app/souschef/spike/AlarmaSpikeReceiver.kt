package app.souschef.spike

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import app.souschef.MainActivity
import app.souschef.R

/**
 * Spike de F1 (solo debug): ¿llega a su hora una cadena de alarmas setAlarmClock en Doze?
 * Disparo: adb shell am broadcast -n app.souschef.dev/app.souschef.spike.AlarmaSpikeReceiver -a SPIKE_INICIO
 * Cada alarma registra en logcat (tag SpikeAlarma) el retraso real frente a la hora prevista.
 */
class AlarmaSpikeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val am = context.getSystemService(AlarmManager::class.java)
        when (intent.action) {
            "SPIKE_INICIO" -> {
                val ahora = System.currentTimeMillis()
                val minutos = intent.getStringExtra("min")?.split(',')?.map { it.trim().toLong() } ?: listOf(2L, 4L, 7L)
                val puede = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
                Log.i(TAG, "inicio: exactas=$puede alarmas=$minutos")
                // Cadena: solo se programa la primera; cada disparo programa la siguiente.
                programa(context, am, ahora, minutos, 0)
            }

            "SPIKE_ALARMA" -> {
                val prevista = intent.getLongExtra("prevista", 0L)
                val idx = intent.getIntExtra("idx", 0)
                val retraso = System.currentTimeMillis() - prevista
                Log.i(TAG, "alarma $idx retraso_ms=$retraso")
                notifica(context, idx, retraso)
                val inicio = intent.getLongExtra("inicio", 0L)
                val minutos = intent.getLongArrayExtra("minutos")?.toList().orEmpty()
                programa(context, am, inicio, minutos, idx + 1)
            }
        }
    }

    private fun programa(
        context: Context,
        am: AlarmManager,
        inicio: Long,
        minutos: List<Long>,
        idx: Int,
    ) {
        if (idx >= minutos.size) return
        val prevista = inicio + minutos[idx] * 60_000
        val i =
            Intent(context, AlarmaSpikeReceiver::class.java)
                .setAction("SPIKE_ALARMA")
                .putExtra("prevista", prevista)
                .putExtra("idx", idx)
                .putExtra("inicio", inicio)
                .putExtra("minutos", minutos.toLongArray())
        val pi =
            PendingIntent.getBroadcast(
                context,
                7000,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val abrir =
            PendingIntent.getActivity(
                context,
                7001,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(prevista, abrir), pi)
        Log.i(TAG, "programada $idx para +${minutos[idx]} min")
    }

    private fun notifica(
        context: Context,
        idx: Int,
        retraso: Long,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel("spike", "Spike alarmas", NotificationManager.IMPORTANCE_HIGH)
            canal.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
            nm.createNotificationChannel(canal)
        }
        val n =
            android.app.Notification
                .Builder(context, "spike")
                .setSmallIcon(R.drawable.ic_stat_souschef)
                .setContentTitle("Spike alarma $idx")
                .setContentText("retraso ${retraso / 1000} s")
                .build()
        nm.notify(7100 + idx, n)
    }

    private companion object {
        const val TAG = "SpikeAlarma"
    }
}
