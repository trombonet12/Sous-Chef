package app.trombito.souschef.data.alarma

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.trombito.souschef.MainActivity
import app.trombito.souschef.domain.crono.AlarmaRecordatorio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * La alarma del recordatorio «Avísame para empezar» (ADR-0016): `setAlarmClock` como la cadena, pero con su propia
 * acción y sus propios `PendingIntent` (4102 el disparo y 4103 el `AlarmClockInfo`), para no tocar la de la cadena
 * (4100). Armar otra vez sustituye a la anterior, porque el `PendingIntent` es el mismo. Sin el permiso de alarmas
 * exactas (Android 12-12L) cae a una inexacta que sigue despertando en Doze, como la cadena.
 */
class AlarmaRecordatorioAndroid
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlarmaRecordatorio {
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
                Intent(context, AlarmaReceiver::class.java).setAction(AlarmaReceiver.ACCION_RECORDATORIO),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private companion object {
            const val CODIGO_DISPARO = 4102
            const val CODIGO_ABRIR = 4103
        }
    }
