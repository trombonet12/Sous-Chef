package app.souschef.data.voz

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import app.souschef.domain.voz.Escucha

/**
 * [Escucha] sobre `SpeechRecognizer` (ADR-0008). Hilo principal. Un reconocedor por cada [empezar]/[parar].
 *
 * - En el dispositivo (API 31+ si `isOnDeviceRecognitionAvailable`); si no, el reconocedor por defecto con
 *   `EXTRA_PREFER_OFFLINE`.
 * - Nunca se registra audio ni transcripciones: el logcat solo lleva códigos de error.
 */
class EscuchaAndroid(
    private val contexto: Context,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val politica: PoliticaReintento = PoliticaReintento(),
) : Escucha {
    private var reconocedor: SpeechRecognizer? = null
    private var alOir: ((List<String>) -> Unit)? = null
    private var pausada = false
    private val reescuchar = Runnable { escuchar() }

    private val activa: Boolean get() = alOir != null

    private val intentEscucha = intent(preferirSinConexion = !enDispositivo())

    override val disponible: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(contexto) || enDispositivo()

    override fun empezar(alOir: (List<String>) -> Unit) {
        if (!disponible) {
            Log.i(ETIQUETA, "escucha: sin reconocedor")
            return
        }
        parar()
        this.alOir = alOir
        pausada = false
        politica.reiniciar()
        reconocedor = crearReconocedor().also { it.setRecognitionListener(oyente) }
        escuchar()
    }

    override fun pausar() {
        pausada = true
        handler.removeCallbacks(reescuchar)
        reconocedor?.cancel()
    }

    override fun reanudar() {
        if (!pausada) return
        pausada = false
        if (activa) programar(0)
    }

    override fun parar() {
        handler.removeCallbacks(reescuchar)
        alOir = null
        pausada = false
        reconocedor?.destroy()
        reconocedor = null
    }

    private fun escuchar() {
        if (activa && !pausada) reconocedor?.startListening(intentEscucha)
    }

    private fun programar(retardoMs: Long) {
        handler.removeCallbacks(reescuchar)
        handler.postDelayed(reescuchar, retardoMs)
    }

    private fun enDispositivo(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(contexto)

    private fun crearReconocedor(): SpeechRecognizer =
        if (enDispositivo() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(contexto)
        } else {
            SpeechRecognizer.createSpeechRecognizer(contexto)
        }

    private val oyente =
        object : RecognitionListener {
            override fun onResults(resultados: Bundle?) {
                val receptor = alOir
                if (receptor == null || pausada) return
                val alternativas =
                    resultados
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                        .filter { it.isNotBlank() }
                programar(politica.alResultado().retardoMs)
                if (alternativas.isNotEmpty()) receptor(alternativas)
            }

            override fun onError(codigo: Int) {
                if (!activa || pausada) return
                Log.d(ETIQUETA, "escucha: error $codigo")
                if (codigo == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) reconocedor?.cancel()
                val decision = politica.alError(codigo)
                if (decision.reintentar) programar(decision.retardoMs)
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onPartialResults(parciales: Bundle?) = Unit

            override fun onEvent(
                tipo: Int,
                params: Bundle?,
            ) = Unit
        }

    private fun intent(preferirSinConexion: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, IDIOMA)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_ALTERNATIVAS)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, contexto.packageName)
            if (preferirSinConexion) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    private companion object {
        const val ETIQUETA = "Escucha"
        const val IDIOMA = "es-ES"
        const val MAX_ALTERNATIVAS = 3
    }
}
