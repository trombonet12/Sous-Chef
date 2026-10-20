package app.trombito.souschef.data.alarma

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import app.trombito.souschef.domain.crono.ConstructorCarriles
import app.trombito.souschef.domain.crono.IdsNotificacion
import app.trombito.souschef.domain.crono.ResumenCocina
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Mando
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.receta.crono.AlarmaActiva
import app.trombito.souschef.receta.crono.LineaAlarma
import app.trombito.souschef.receta.crono.MotorCrono
import app.trombito.souschef.receta.crono.SesionCrono
import app.trombito.souschef.ui.crono.FakeAjustesRepo
import app.trombito.souschef.ui.crono.T0
import app.trombito.souschef.ui.crono.recetaCrono
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificadorAndroidTest {
    private lateinit var contexto: Context
    private lateinit var gestor: NotificationManager
    private lateinit var notificador: NotificadorAndroid
    private val ajustes = FakeAjustesRepo()
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
        contexto = ApplicationProvider.getApplicationContext<Context>()
        gestor = contexto.getSystemService(NotificationManager::class.java)
        notificador = NotificadorAndroid(contexto, ajustes)
    }

    private fun resumen(urgente: String? = null) =
        ResumenCocina(
            slug = "r",
            tituloReceta = "Receta r",
            carriles = ConstructorCarriles.construir(receta, estado, T0 + 10_000L),
            urgente = urgente,
            finProximoMs = T0 + 120_000L,
            llegadaMs = T0 + 15 * 60_000L,
        )

    private fun alarma(inicioMs: Long = System.currentTimeMillis()) =
        AlarmaActiva(inicioMs, listOf(LineaAlarma("m", "M: Cortar: hecho"), LineaAlarma("f0", "F0: Sofrito: hecho")))

    private fun cocinaActiva() =
        gestor.activeNotifications.single {
            it.tag == IdsNotificacion.tagCocina("r") &&
                it.id == IdsNotificacion.ID_COCINA
        }

    private fun alarmaActiva() =
        gestor.activeNotifications
            .single { it.tag == IdsNotificacion.tagAlarma("r") && it.id == IdsNotificacion.ID_ALARMA }
            .notification

    @Test
    fun laCocinaEsUnaSolaNotificacionFijaConUnaLineaPorCarril() {
        notificador.cocina(resumen())
        assertEquals(1, gestor.activeNotifications.size)
        val n = cocinaActiva().notification
        assertEquals("crono_cocina", n.channelId)
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(n.contentIntent)
        assertTrue(n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertTrue(
            n.extras
                .getString(Notification.EXTRA_TITLE)
                .orEmpty()
                .startsWith("Receta r · llegas "),
        )
        val cuerpo =
            n.extras
                .getCharSequence(Notification.EXTRA_BIG_TEXT)
                .toString()
                .lines()
        assertEquals(2, cuerpo.size)
        assertTrue(cuerpo.any { it.startsWith("M · Cortar") && "acaba" in it })
        assertTrue(cuerpo.any { it.startsWith("F0 · Sofrito") && "fuego 5" in it })
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(T0 + 120_000L, n.`when`)
    }

    @Test
    fun laPotenciaYElTextoSeDicenEnLaCocinaDelUsuario() =
        runTest {
            val canonica =
                recetaCrono().copy(
                    pasos = recetaCrono().pasos.map { if (it.id == "p1") it.copy(t = "Baja al {3}", f = 7) else it },
                )
            val estadoCanonico =
                MotorCrono.empezar(
                    SesionCrono.abrir(SesionCrono.inicial(canonica, T0 + 15 * 60_000L, T0, 2), canonica, T0),
                    canonica,
                    "f0",
                    T0,
                )
            val carriles = ConstructorCarriles.construir(canonica, estadoCanonico, T0 + 10_000L)
            // El modelo de la notificación sigue siendo el canónico.
            assertEquals("Baja al {3}", carriles.single { it.carril == "f0" }.texto)
            ajustes.fijarCocina(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))
            notificador.cocina(resumen().copy(carriles = carriles))
            val gas = cuerpoCocina()
            assertTrue(gas.toString(), gas.any { it.startsWith("F0 · Baja a fuego bajo · fuego medio-alto") })

            ajustes.fijarCocina(Cocina(TipoCocina.INDUCCION, Mando.DIGITAL, 12))
            notificador.cocina(resumen().copy(carriles = carriles))
            val digital = cuerpoCocina()
            assertTrue(digital.toString(), digital.any { it.startsWith("F0 · Baja al 4 · fuego 9") })
        }

    @Test
    fun laAlarmaTraduceLasMarcasDePotenciaDeSusLineas() =
        runTest {
            ajustes.fijarCocina(Cocina(TipoCocina.GAS, Mando.ANALOGICO, null))
            val lineas = AlarmaActiva(System.currentTimeMillis(), listOf(LineaAlarma("f0", "F0: Baja al {3}: hecho")))
            notificador.alarma("r", "Receta r", lineas, sonar = true, urgente = null)
            assertEquals(
                "F0: Baja a fuego bajo: hecho",
                alarmaActiva().extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString(),
            )
        }

    private fun cuerpoCocina() =
        cocinaActiva()
            .notification.extras
            .getCharSequence(Notification.EXTRA_BIG_TEXT)
            .toString()
            .lines()

    @Test
    fun laAccionHechoSoloAparecePorUnPasoVencido() {
        notificador.cocina(resumen())
        assertNull(cocinaActiva().notification.actions)
        notificador.cocina(resumen(urgente = "m"))
        assertEquals(listOf("Hecho"), cocinaActiva().notification.actions.map { it.title.toString() })
    }

    @Test
    fun noSePublicaEnMarchaNiPorCarrilYSeLimpianLasAntiguas() {
        gestor.notify(
            IdsNotificacion.tagEnMarcha("r"),
            IdsNotificacion.ID_EN_MARCHA,
            Notification
                .Builder(contexto, "crono_carriles")
                .setSmallIcon(app.trombito.souschef.R.drawable.ic_stat_souschef)
                .build(),
        )
        gestor.notify(
            IdsNotificacion.tagCarrilAntiguo("r"),
            "f0".hashCode(),
            Notification
                .Builder(contexto, "crono_carriles")
                .setSmallIcon(app.trombito.souschef.R.drawable.ic_stat_souschef)
                .build(),
        )
        notificador.cocina(resumen())
        assertEquals(listOf(IdsNotificacion.tagCocina("r")), gestor.activeNotifications.map { it.tag })
    }

    @Test
    fun laAlarmaSuenaInsistenteConParar() {
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        val n = alarmaActiva()
        assertEquals("crono_alarma_v3", n.channelId)
        assertTrue(n.flags and Notification.FLAG_INSISTENT != 0)
        assertEquals(Notification.CATEGORY_ALARM, n.category)
        assertEquals(
            "M: Cortar: hecho\nF0: Sofrito: hecho",
            n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString(),
        )
        assertEquals(listOf("Parar"), n.actions.map { it.title.toString() })
        assertTrue(n.timeoutAfter in 1..60_000L)
    }

    @Test
    fun conUnPasoVencidoOfreceHechoYSiguiente() {
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = "m")
        assertEquals(listOf("Parar", "Hecho y siguiente"), alarmaActiva().actions.map { it.title.toString() })
    }

    @Test
    fun laAlarmaAgrupadaSeActualizaEnSilencio() {
        notificador.alarma("r", "Receta r", alarma(), sonar = false, urgente = null)
        val n = alarmaActiva()
        assertEquals(0, n.flags and Notification.FLAG_INSISTENT)
        assertTrue(n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertEquals(1, gestor.activeNotifications.size)
    }

    @Test
    fun pantallaCompletaSoloConElMovilBloqueado() {
        val teclado = contexto.getSystemService(KeyguardManager::class.java)
        shadowOf(teclado).setKeyguardLocked(false)
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        assertNull(alarmaActiva().fullScreenIntent)
        shadowOf(teclado).setKeyguardLocked(true)
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        assertNotNull(alarmaActiva().fullScreenIntent)
        // Una actualización en silencio nunca pide pantalla completa.
        notificador.alarma("r", "Receta r", alarma(), sonar = false, urgente = null)
        assertNull(alarmaActiva().fullScreenIntent)
    }

    @Test
    fun conLaPantallaApagadaTambienPidePantallaCompletaAunqueNoHayaBloqueo() {
        val teclado = contexto.getSystemService(KeyguardManager::class.java)
        val energia = contexto.getSystemService(PowerManager::class.java)
        shadowOf(teclado).setKeyguardLocked(false)
        shadowOf(energia).turnScreenOn(false)
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        assertNotNull(alarmaActiva().fullScreenIntent)
        shadowOf(energia).turnScreenOn(true)
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        assertNull(alarmaActiva().fullScreenIntent)
    }

    @Test
    fun quitarAlarmaDejaLaCocinaYLimpiarQuitaLasDos() {
        notificador.cocina(resumen())
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        assertEquals(2, gestor.activeNotifications.size)
        notificador.quitarAlarma("r")
        assertEquals(listOf(IdsNotificacion.ID_COCINA), gestor.activeNotifications.map { it.id })
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        notificador.limpiar("r")
        assertTrue(gestor.activeNotifications.isEmpty())
    }

    @Test
    fun quitarCocinaNoTocaLaAlarma() {
        notificador.cocina(resumen())
        notificador.alarma("r", "Receta r", alarma(), sonar = true, urgente = null)
        notificador.quitarCocina("r")
        assertEquals(listOf(IdsNotificacion.ID_ALARMA), gestor.activeNotifications.map { it.id })
    }

    @Test
    fun creaLosCanalesNuevosYBorraLosAntiguos() {
        listOf("crono_avisos_v2", "crono_carriles").forEach {
            gestor.createNotificationChannel(
                android.app.NotificationChannel(it, "viejo", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        notificador.cocina(resumen())
        assertNull(gestor.getNotificationChannel("crono_avisos_v2"))
        assertNull(gestor.getNotificationChannel("crono_carriles"))
        val alarma = checkNotNull(gestor.getNotificationChannel("crono_alarma_v3"))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarma.importance)
        assertNotNull(alarma.sound)
        val cocina = checkNotNull(gestor.getNotificationChannel("crono_cocina"))
        assertEquals(NotificationManager.IMPORTANCE_LOW, cocina.importance)
        assertNull(cocina.sound)
        assertFalse(cocina.shouldVibrate())
    }
}
