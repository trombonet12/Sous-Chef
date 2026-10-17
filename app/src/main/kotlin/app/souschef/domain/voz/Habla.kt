package app.souschef.domain.voz

/** Síntesis de voz en español (ADR-0008). Vive en el hilo principal. */
interface Habla {
    /**
     * Dice [texto] en es-ES, a continuación de lo que ya esté sonando. Si el motor aún no está listo, lo guarda
     * hasta que lo esté. [alAcabar] se llama en el hilo principal cuando termina, falla o se corta con [callar];
     * no se llama tras [liberar].
     */
    fun decir(
        texto: String,
        alAcabar: () -> Unit = {},
    )

    /** Corta lo que suena y descarta lo encolado; los `alAcabar` pendientes se llaman igualmente. */
    fun callar()

    /** Suelta el motor. Una llamada posterior a [decir] lo vuelve a crear. */
    fun liberar()
}
