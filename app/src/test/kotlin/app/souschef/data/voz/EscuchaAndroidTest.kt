package app.souschef.data.voz

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSpeechRecognizer
import java.time.Duration

/** Bucle de escucha sobre el `SpeechRecognizer` simulado de Robolectric (reconocedor por defecto, sin on-device). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class EscuchaAndroidTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val oidos = mutableListOf<List<String>>()
    private lateinit var escucha: EscuchaAndroid

    @Before
    fun preparar() {
        escucha = EscuchaAndroid(contexto)
    }

    private fun registrarServicioDeReconocimiento() {
        val componente = ComponentName("com.ejemplo.reconocedor", "com.ejemplo.reconocedor.Servicio")
        val gestor = shadowOf(contexto.packageManager)
        gestor.addOrUpdateService(
            ServiceInfo().apply {
                packageName = componente.packageName
                name = componente.className
            },
        )
        gestor.addIntentFilterForService(componente, IntentFilter(RecognitionService.SERVICE_INTERFACE))
    }

    /** Registra el servicio, empieza a escuchar y deja que el reconocedor simulado conecte su oyente. */
    private fun iniciar() {
        registrarServicioDeReconocimiento()
        escucha.empezar { oidos += it }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun sombra() = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

    private fun avanzar(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun resultados(vararg textos: String) =
        Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, ArrayList(textos.toList())) }

    @Test
    fun noEmpiezaSinReconocedor() {
        assertFalse(escucha.disponible)
        escucha.empezar { oidos += it }
        assertNull(ShadowSpeechRecognizer.getLatestSpeechRecognizer())
    }

    @Test
    fun entregaLasAlternativasYConfiguraElIntent() {
        iniciar()
        sombra().triggerOnResults(resultados("chef hecho", "chef techo"))
        assertEquals(listOf(listOf("chef hecho", "chef techo")), oidos)

        val intent = sombra().lastRecognizerIntent
        assertNotNull(intent)
        assertEquals("es-ES", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(3, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))
        assertFalse(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
    }

    @Test
    fun enPausaDescartaLoQueLlegaYReanudaVuelveAOir() {
        iniciar()
        val reconocedor = sombra()
        escucha.pausar()
        reconocedor.triggerOnResults(resultados("chef pausa"))
        assertTrue(oidos.isEmpty())

        escucha.reanudar()
        avanzar(10)
        reconocedor.triggerOnResults(resultados("chef sigue"))
        assertEquals(listOf(listOf("chef sigue")), oidos)
    }

    @Test
    fun pararSueltaElReconocedorYYaNoEntregaNada() {
        iniciar()
        val reconocedor = sombra()
        escucha.parar()
        assertTrue(reconocedor.isDestroyed)
        avanzar(5_000)
        assertTrue(oidos.isEmpty())
    }
}
