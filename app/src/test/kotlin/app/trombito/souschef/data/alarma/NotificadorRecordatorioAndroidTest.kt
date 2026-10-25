package app.trombito.souschef.data.alarma

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.MainActivity
import app.trombito.souschef.domain.crono.IdsNotificacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** El aviso de «Avísame para empezar» (ADR-0016): normal, en su canal, sin pantalla completa y hacia la ficha. */
@RunWith(RobolectricTestRunner::class)
class NotificadorRecordatorioAndroidTest {
    private lateinit var contexto: Context
    private lateinit var gestor: NotificationManager
    private lateinit var notificador: NotificadorRecordatorioAndroid

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext<Context>()
        gestor = contexto.getSystemService(NotificationManager::class.java)
        notificador = NotificadorRecordatorioAndroid(contexto)
    }

    private fun aviso(slug: String = "sopa"): Notification {
        notificador.avisar(slug, "Sopa de ajo", System.currentTimeMillis())
        return gestor.activeNotifications
            .single { it.tag == IdsNotificacion.tagRecordatorio(slug) && it.id == IdsNotificacion.ID_RECORDATORIO }
            .notification
    }

    @Test
    fun esUnaNotificacionNormalEnSuPropioCanalConImportanciaPorDefecto() {
        val n = aviso()
        assertEquals("crono_recordatorio", n.channelId)
        val canal = gestor.getNotificationChannel("crono_recordatorio")
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, canal.importance)
        assertEquals(Notification.CATEGORY_REMINDER, n.category)
        assertEquals("Toca empezar: Sopa de ajo", n.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun noInsisteNiPideLaPantallaCompletaNiEsFija() {
        val n = aviso()
        assertNull(n.fullScreenIntent)
        assertEquals(0, n.flags and Notification.FLAG_INSISTENT)
        assertEquals(0, n.flags and Notification.FLAG_ONGOING_EVENT)
        assertTrue(n.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun alTocarlaAbreLaFichaConUnIntentExplicitoEInmutable() {
        val pendiente = aviso("sopa").contentIntent
        assertNotNull(pendiente)
        val sombra = shadowOf(pendiente)
        assertTrue(sombra.isActivityIntent)
        assertTrue(sombra.flags and PendingIntent.FLAG_IMMUTABLE != 0)
        val intent = sombra.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(contexto.packageName, intent.component?.packageName)
        assertEquals("souschef://ficha/sopa", intent.data.toString())
        assertEquals(Intent.ACTION_VIEW, intent.action)
    }

    @Test
    fun cadaRecetaTieneSuPropioAviso() {
        aviso("sopa")
        aviso("arroz")
        assertEquals(2, gestor.activeNotifications.size)
    }
}
