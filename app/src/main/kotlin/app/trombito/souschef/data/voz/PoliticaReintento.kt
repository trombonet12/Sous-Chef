package app.trombito.souschef.data.voz

/**
 * Cuándo volver a escuchar tras un resultado o un error del reconocedor.
 * Pura (sin SpeechRecognizer) para poder testearla.
 *
 * - Resultado, `ERROR_NO_MATCH` y `ERROR_SPEECH_TIMEOUT`: no son fallos; se reescucha tras [PAUSA_MINIMA_MS] (nunca
 *   al instante, para no encadenar reinicios en bucle cerrado) y se reinicia el backoff.
 * - Resto de errores: backoff [inicialMs] -> [maximoMs] (se duplica) mientras se repitan seguidos.
 * - `ERROR_INSUFFICIENT_PERMISSIONS`, `ERROR_LANGUAGE_NOT_SUPPORTED` y `ERROR_LANGUAGE_UNAVAILABLE`: reintentar no
 *   sirve (ni el permiso ni el idioma aparecen solos); se para el bucle.
 */
class PoliticaReintento(
    private val inicialMs: Long = INICIAL_MS,
    private val maximoMs: Long = MAXIMO_MS,
) {
    /** Qué hacer a continuación: reintentar tras [retardoMs] o, si [reintentar] es `false`, parar el bucle. */
    data class Decision(
        val retardoMs: Long,
        val reintentar: Boolean = true,
    )

    private var erroresSeguidos = 0

    fun alResultado(): Decision {
        erroresSeguidos = 0
        return Decision(PAUSA_MINIMA_MS)
    }

    fun alError(codigo: Int): Decision =
        when (codigo) {
            ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT -> {
                alResultado()
            }

            ERROR_INSUFFICIENT_PERMISSIONS, ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE -> {
                Decision(0, reintentar = false)
            }

            else -> {
                Decision(retardoBackoff())
            }
        }

    fun reiniciar() {
        erroresSeguidos = 0
    }

    private fun retardoBackoff(): Long {
        val exponente = erroresSeguidos.coerceAtMost(MAX_EXPONENTE)
        erroresSeguidos++
        return (inicialMs shl exponente).coerceAtMost(maximoMs)
    }

    companion object {
        const val INICIAL_MS = 500L
        const val MAXIMO_MS = 4_000L
        const val PAUSA_MINIMA_MS = 300L
        private const val MAX_EXPONENTE = 16

        // Valores de android.speech.SpeechRecognizer, copiados para que esta clase no dependa del SDK.
        const val ERROR_SPEECH_TIMEOUT = 6
        const val ERROR_NO_MATCH = 7
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}
