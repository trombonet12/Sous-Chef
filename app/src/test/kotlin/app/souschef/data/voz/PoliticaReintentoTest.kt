package app.souschef.data.voz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaReintentoTest {
    private val politica = PoliticaReintento()
    private val otroError = 3 // ERROR_AUDIO

    @Test
    fun erroresSeguidosDuplicanElRetardoHastaElMaximo() {
        val retardos = List(6) { politica.alError(otroError).retardoMs }
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L, 4_000L, 4_000L), retardos)
    }

    @Test
    fun unResultadoReiniciaElBackoff() {
        repeat(3) { politica.alError(otroError) }
        assertEquals(300L, politica.alResultado().retardoMs)
        assertEquals(500L, politica.alError(otroError).retardoMs)
    }

    @Test
    fun sinCoincidenciaYSilencioNoSonErrores() {
        repeat(2) { politica.alError(otroError) }
        for (codigo in listOf(PoliticaReintento.ERROR_NO_MATCH, PoliticaReintento.ERROR_SPEECH_TIMEOUT)) {
            val decision = politica.alError(codigo)
            assertEquals("nunca un bucle sin pausa", 300L, decision.retardoMs)
            assertTrue(decision.reintentar)
        }
        assertEquals("el backoff vuelve a empezar", 500L, politica.alError(otroError).retardoMs)
    }

    @Test
    fun sinPermisoSePara() {
        assertFalse(politica.alError(PoliticaReintento.ERROR_INSUFFICIENT_PERMISSIONS).reintentar)
    }

    @Test
    fun siElIdiomaNoEstaSoportadoONoEstaDisponibleSePara() {
        assertFalse(politica.alError(PoliticaReintento.ERROR_LANGUAGE_NOT_SUPPORTED).reintentar)
        assertFalse(politica.alError(PoliticaReintento.ERROR_LANGUAGE_UNAVAILABLE).reintentar)
    }

    @Test
    fun reiniciarVaciaElContador() {
        repeat(4) { politica.alError(otroError) }
        politica.reiniciar()
        assertEquals(500L, politica.alError(otroError).retardoMs)
    }

    @Test
    fun muchosErroresNoDesbordan() {
        repeat(200) { politica.alError(otroError) }
        assertEquals(4_000L, politica.alError(otroError).retardoMs)
    }

    @Test
    fun losCodigosCopiadosCoincidenConElSdk() {
        assertEquals(android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT, PoliticaReintento.ERROR_SPEECH_TIMEOUT)
        assertEquals(android.speech.SpeechRecognizer.ERROR_NO_MATCH, PoliticaReintento.ERROR_NO_MATCH)
        assertEquals(
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            PoliticaReintento.ERROR_INSUFFICIENT_PERMISSIONS,
        )
        assertEquals(
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            PoliticaReintento.ERROR_LANGUAGE_NOT_SUPPORTED,
        )
        assertEquals(
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            PoliticaReintento.ERROR_LANGUAGE_UNAVAILABLE,
        )
    }
}
