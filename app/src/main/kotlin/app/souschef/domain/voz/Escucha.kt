package app.souschef.domain.voz

/**
 * Micrófono en bucle (ADR-0008). Vive en el hilo principal y solo mientras el cronómetro está en pantalla.
 * No guarda ni registra audio ni transcripciones.
 */
interface Escucha {
    /** Hay un reconocedor de voz en el dispositivo. Si es `false`, [empezar] no hace nada. */
    val disponible: Boolean

    /**
     * Empieza a escuchar en bucle: tras cada resultado o error vuelve a escuchar, con backoff si los errores se
     * repiten. [alOir] recibe las alternativas del reconocedor, de más a menos probable, en el hilo principal.
     * Requiere el permiso RECORD_AUDIO concedido.
     */
    fun empezar(alOir: (alternativas: List<String>) -> Unit)

    /** Deja de oír sin soltar el reconocedor; úsalo mientras habla el TTS. Lo que llegue en pausa se descarta. */
    fun pausar()

    /** Vuelve a oír tras [pausar]. No hace nada si no se llamó a [empezar]. */
    fun reanudar()

    /** Libera el reconocedor. Se puede volver a llamar a [empezar] después. */
    fun parar()
}
