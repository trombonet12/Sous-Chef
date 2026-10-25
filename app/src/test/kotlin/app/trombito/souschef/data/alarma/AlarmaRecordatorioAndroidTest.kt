package app.trombito.souschef.data.alarma

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** La alarma del recordatorio (ADR-0016): `setAlarmClock` con su propia acción y sus códigos 4102 y 4103. */
@RunWith(RobolectricTestRunner::class)
class AlarmaRecordatorioAndroidTest {
    private lateinit var alarmas: AlarmManager
    private lateinit var alarma: AlarmaRecordatorioAndroid

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        alarmas = contexto.getSystemService(AlarmManager::class.java)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        alarma = AlarmaRecordatorioAndroid(contexto)
    }

    @Test
    fun programaUnaAlarmaDeRelojConLaAccionYLosCodigosPropios() {
        alarma.programar(1_800_000_000_000L)
        val programada = checkNotNull(shadowOf(alarmas).nextScheduledAlarm)
        assertEquals(1_800_000_000_000L, programada.triggerAtMs)
        val disparo = shadowOf(programada.operation)
        assertTrue(disparo.isBroadcastIntent)
        assertEquals(4102, disparo.requestCode)
        assertEquals(AlarmaReceiver.ACCION_RECORDATORIO, disparo.savedIntent.action)
        assertEquals(AlarmaReceiver::class.java.name, disparo.savedIntent.component?.className)
        // El `AlarmClockInfo` (el icono de «próxima alarma» del sistema) lleva el 4103.
        assertEquals(4103, shadowOf(programada.showIntent).requestCode)
    }

    @Test
    fun programarOtraVezSustituyeALaAnterior() {
        alarma.programar(1_800_000_000_000L)
        alarma.programar(1_800_000_600_000L)
        val todas = shadowOf(alarmas).scheduledAlarms
        assertEquals(1, todas.size)
        assertEquals(1_800_000_600_000L, todas.single().triggerAtMs)
    }

    @Test
    fun cancelarLaQuita() {
        alarma.programar(1_800_000_000_000L)
        alarma.cancelar()
        assertNull(shadowOf(alarmas).nextScheduledAlarm)
    }

    @Test
    fun noPisaLaAlarmaDeLaCadena() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        AlarmaSistemaAndroid(contexto).programar(1_800_000_000_000L)
        alarma.programar(1_800_000_300_000L)
        assertEquals(2, shadowOf(alarmas).scheduledAlarms.size)
        alarma.cancelar()
        val queda = shadowOf(alarmas).scheduledAlarms.single()
        assertEquals(AlarmaReceiver.ACCION_ALARMA, shadowOf(queda.operation).savedIntent.action)
    }
}
