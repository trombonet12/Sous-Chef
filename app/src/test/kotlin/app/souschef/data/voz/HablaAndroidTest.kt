package app.souschef.data.voz

import android.app.Application
import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HablaAndroidTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private lateinit var habla: HablaAndroid
    private val acabadas = mutableListOf<String>()

    @Before
    fun preparar() {
        habla = HablaAndroid(contexto)
    }

    private fun sombra() = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())

    private fun iniciarMotor(estado: Int = TextToSpeech.SUCCESS) {
        sombra().onInitListener.onInit(estado)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun guardaLosTextosHastaQueElMotorEstaListoYLosDiceEnOrdenEnEspanol() {
        habla.decir("uno") { acabadas += "uno" }
        habla.decir("dos") { acabadas += "dos" }
        assertTrue(sombra().spokenTextList.isEmpty())

        iniciarMotor()

        assertEquals(listOf("uno", "dos"), sombra().spokenTextList)
        assertEquals(TextToSpeech.QUEUE_ADD, sombra().queueMode)
        assertEquals(Locale.forLanguageTag("es-ES"), sombra().currentLanguage)
    }

    @Test
    fun siElMotorFallaAvisaALosPendientesYNoVuelveAEncolar() {
        habla.decir("uno") { acabadas += "uno" }
        iniciarMotor(TextToSpeech.ERROR)
        assertEquals(listOf("uno"), acabadas)

        habla.decir("dos") { acabadas += "dos" }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("uno", "dos"), acabadas)
    }

    @Test
    fun callarAvisaALosPendientesSinEnviar() {
        habla.decir("uno") { acabadas += "uno" }
        habla.callar()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("uno"), acabadas)
    }

    @Test
    fun liberarSueltaElMotor() {
        habla.decir("uno")
        val motor = sombra()
        habla.liberar()
        assertTrue(motor.isShutdown)
    }
}
