package app.souschef.data.alarma

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.souschef.domain.crono.AvisoNotificacion
import app.souschef.domain.crono.ConstructorCarriles
import app.souschef.domain.crono.ConstructorEnMarcha
import app.souschef.domain.crono.IdsNotificacion
import app.souschef.receta.crono.Aviso
import app.souschef.receta.crono.MotorCrono
import app.souschef.receta.crono.SesionCrono
import app.souschef.receta.crono.TipoAviso
import app.souschef.ui.crono.T0
import app.souschef.ui.crono.recetaCrono
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificadorAndroidTest {
    private lateinit var gestor: NotificationManager
    private lateinit var notificador: NotificadorAndroid
    private val receta = recetaCrono()
    private val estado =
        MotorCrono.empezar(
            SesionCrono.abrir(SesionCrono.inicial(receta, T0 + 15 * 60_000L, T0, 2), receta, T0),
            receta,
            "m",
            T0,
        )

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        gestor = contexto.getSystemService(NotificationManager::class.java)
        notificador = NotificadorAndroid(contexto)
    }

    private fun pintar() {
        notificador.carriles("r", ConstructorCarriles.construir(receta, estado, T0 + 10_000L))
    }

    private fun aviso() =
        AvisoNotificacion(
            "r",
            "Receta r",
            "M",
            Aviso(TipoAviso.FIN, "m", "Cortar", "Cortar: hecho", T0 + 120_000L),
        )

    @Test
    fun enMarchaNoLlevaDescarteAsiNoAbandonaAlDeslizar() {
        val modelo = checkNotNull(ConstructorEnMarcha.construir(receta, estado, T0 + 10_000L))
        notificador.enMarcha("r", modelo)
        val n = gestor.activeNotifications.single { it.id == IdsNotificacion.ID_EN_MARCHA }.notification
        assertNull(n.deleteIntent)
        assertEquals("crono_carriles", n.channelId)
        assertNull(n.group)
    }

    @Test
    fun unaNotificacionPorCarrilConTagIdYGrupo() {
        pintar()
        val tag = IdsNotificacion.tagCarril("r")
        val activas = gestor.activeNotifications.filter { it.tag == tag }
        assertEquals(setOf("f0".hashCode(), "m".hashCode(), IdsNotificacion.ID_RESUMEN), activas.map { it.id }.toSet())
        val m = activas.single { it.id == "m".hashCode() }.notification
        assertEquals("crono_carriles", m.channelId)
        assertEquals("carriles:r", m.group)
        assertEquals("M · paso 1/1", m.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(m.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun elAvisoRepublicaElMismoIdComoAlarmaInsistenteYNoSePisaAlRepintar() {
        pintar()
        notificador.publicar(listOf(aviso()))
        val tag = IdsNotificacion.tagCarril("r")
        val n = gestor.activeNotifications.single { it.tag == tag && it.id == "m".hashCode() }.notification
        assertEquals("crono_alarma_v3", n.channelId)
        assertTrue(n.flags and Notification.FLAG_INSISTENT != 0)
        assertNotNull(n.fullScreenIntent)
        assertEquals(Notification.CATEGORY_ALARM, n.category)
        pintar()
        assertEquals(
            "crono_alarma_v3",
            gestor.activeNotifications
                .single { it.tag == tag && it.id == "m".hashCode() }
                .notification.channelId,
        )
    }

    @Test
    fun tragasLaAlarmaPararYRepintarVuelveAlCanalSilencioso() {
        pintar()
        notificador.publicar(listOf(aviso()))
        val tag = IdsNotificacion.tagCarril("r")
        gestor.cancel(tag, "m".hashCode())
        pintar()
        assertEquals(
            "crono_carriles",
            gestor.activeNotifications
                .single { it.tag == tag && it.id == "m".hashCode() }
                .notification.channelId,
        )
    }

    @Test
    fun limpiarQuitaCarrilesResumenYOngoing() {
        pintar()
        notificador.enMarcha("r", ConstructorEnMarcha.construir(receta, estado, T0))
        notificador.limpiar("r")
        assertTrue(gestor.activeNotifications.isEmpty())
    }

    @Test
    fun creaLosCanalesNuevosYBorraLosAntiguos() {
        gestor.createNotificationChannel(
            android.app.NotificationChannel("crono_avisos_v2", "viejo", NotificationManager.IMPORTANCE_HIGH),
        )
        pintar()
        assertNull(gestor.getNotificationChannel("crono_avisos_v2"))
        val alarma = checkNotNull(gestor.getNotificationChannel("crono_alarma_v3"))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarma.importance)
        assertNotNull(alarma.sound)
        val carriles = checkNotNull(gestor.getNotificationChannel("crono_carriles"))
        assertNull(carriles.sound)
    }
}
