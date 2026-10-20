package app.trombito.souschef.data.voz

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import app.trombito.souschef.domain.voz.Habla
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * [Habla] sobre `TextToSpeech` (ADR-0008). El motor se crea en la primera llamada a [decir]; mientras no esté listo,
 * los textos esperan en [pendientes]. Si el motor no arranca, [decir] solo avisa a `alAcabar` para no dejar la escucha
 * pausada. Los textos no se registran.
 */
class HablaAndroid(
    private val contexto: Context,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : Habla {
    private class Frase(
        val texto: String,
        val alAcabar: () -> Unit,
    )

    private var motor: TextToSpeech? = null
    private var listo = false
    private var fallido = false
    private val pendientes = ArrayDeque<Frase>()
    private val enCurso = ConcurrentHashMap<String, () -> Unit>()

    override fun decir(
        texto: String,
        alAcabar: () -> Unit,
    ) {
        val frase = Frase(texto, alAcabar)
        when {
            fallido -> {
                handler.post(alAcabar)
            }

            listo -> {
                hablar(frase)
            }

            else -> {
                pendientes.addLast(frase)
                if (motor == null) motor = TextToSpeech(contexto, ::alIniciar)
            }
        }
    }

    override fun callar() {
        val sinEnviar = pendientes.toList()
        pendientes.clear()
        sinEnviar.forEach { handler.post(it.alAcabar) }
        // stop() dispara onStop en las frases enviadas; ahí se llama su alAcabar.
        motor?.stop()
    }

    override fun liberar() {
        pendientes.clear()
        enCurso.clear()
        motor?.shutdown()
        motor = null
        listo = false
        fallido = false
    }

    private fun alIniciar(estado: Int) {
        if (estado != TextToSpeech.SUCCESS) {
            Log.w(ETIQUETA, "habla: error de inicio $estado")
            fallido = true
            motor?.shutdown()
            motor = null
            val sinEnviar = pendientes.toList()
            pendientes.clear()
            sinEnviar.forEach { it.alAcabar() }
            return
        }
        val resultado = motor?.setLanguage(IDIOMA)
        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(ETIQUETA, "habla: es-ES no disponible ($resultado)")
        }
        motor?.setOnUtteranceProgressListener(oyente)
        listo = true
        while (pendientes.isNotEmpty()) hablar(pendientes.removeFirst())
    }

    private fun hablar(frase: Frase) {
        val id = UUID.randomUUID().toString()
        enCurso[id] = frase.alAcabar
        val estado = motor?.speak(frase.texto, TextToSpeech.QUEUE_ADD, null, id)
        if (estado != TextToSpeech.SUCCESS) terminar(id)
    }

    private fun terminar(id: String?) {
        val alAcabar = id?.let { enCurso.remove(it) } ?: return
        handler.post(alAcabar)
    }

    private val oyente =
        object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit

            override fun onDone(id: String?) = terminar(id)

            override fun onStop(
                id: String?,
                interrumpida: Boolean,
            ) = terminar(id)

            @Deprecated("Sustituido por onError(String, Int)")
            override fun onError(id: String?) = terminar(id)

            override fun onError(
                id: String?,
                codigo: Int,
            ) {
                Log.w(ETIQUETA, "habla: error $codigo")
                terminar(id)
            }
        }

    private companion object {
        const val ETIQUETA = "Habla"
        val IDIOMA: Locale = Locale.forLanguageTag("es-ES")
    }
}
