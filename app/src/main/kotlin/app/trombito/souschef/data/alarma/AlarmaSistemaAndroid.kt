package app.trombito.souschef.data.alarma

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.trombito.souschef.MainActivity
import app.trombito.souschef.domain.crono.AlarmaSistema
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * `setAlarmClock` (la más fiable bajo Doze, ADR-0005) para el próximo evento. Si en Android 12-12L el usuario ha
 * retirado el permiso de alarmas exactas, cae a una alarma inexacta que sigue despertando en Doze.
 */
class AlarmaSistemaAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlarmaSistema {
        private val alarmas = context.getSystemService(AlarmManager::class.java)

        override fun programar(ms: Long) {
            val disparo = intentDisparo()
            val puedeExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()
            if (puedeExacta) {
                val abrir =
                    PendingIntent.getActivity(
                        context,
                        CODIGO_ABRIR,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                alarmas.setAlarmClock(AlarmManager.AlarmClockInfo(ms, abrir), disparo)
            } else {
                alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, disparo)
            }
        }

        override fun cancelar() {
            alarmas.cancel(intentDisparo())
        }

        private fun intentDisparo(): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                CODIGO_DISPARO,
                Intent(context, AlarmaReceiver::class.java).setAction(AlarmaReceiver.ACCION_ALARMA),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private companion object {
            const val CODIGO_DISPARO = 4100
            const val CODIGO_ABRIR = 4101
        }
    }
