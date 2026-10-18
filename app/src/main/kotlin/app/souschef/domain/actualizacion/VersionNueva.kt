package app.souschef.domain.actualizacion

/** Release publicada en GitHub: `tag` es `v<versionName>` y `url`, su página. */
data class ReleasePublicada(
    val tag: String,
    val url: String?,
)

/** Decide si hay que avisar de una versión nueva (ADR-0009). Puro: sin red ni Android. */
object VersionNueva {
    /** Al abrir la app no se vuelve a preguntar a GitHub antes de este intervalo. */
    const val INTERVALO_AL_ABRIR_MS = 6 * 3_600_000L

    private val tag = Regex(""""tag_name"\s*:\s*"([^"]+)"""")
    private val url = Regex(""""html_url"\s*:\s*"([^"]+/releases/tag/[^"]+)"""")
    private val numeros = Regex("""^v?(\d+(?:\.\d+)*)""")

    /** Lee `tag_name` y `html_url` de `releases/latest`; null si la respuesta no trae tag. */
    fun leerRelease(json: String): ReleasePublicada? {
        val t = tag.find(json)?.groupValues?.get(1) ?: return null
        return ReleasePublicada(t, url.find(json)?.groupValues?.get(1))
    }

    /** `v0.10.0` > `0.9.0`: compara componente a componente (lo que falta es 0). Un tag ilegible no es nuevo. */
    fun esMasNueva(
        remota: String,
        instalada: String,
    ): Boolean {
        val r = partes(remota)
        val i = partes(instalada)
        if (r == null || i == null) return false
        val distinto =
            (0 until maxOf(r.size, i.size))
                .map { r.getOrElse(it) { 0 } to i.getOrElse(it) { 0 } }
                .firstOrNull { (a, b) -> a != b }
        return distinto != null && distinto.first > distinto.second
    }

    /** Avisa si la remota es más nueva que la instalada y no es la que ya se avisó. */
    fun debeAvisar(
        remota: String,
        instalada: String,
        yaAvisada: String?,
    ): Boolean = remota != yaAvisada && esMasNueva(remota, instalada)

    /** Instalada desde Google Play: las actualizaciones las trae la tienda y el aviso de GitHub sobra (ADR-0010). */
    fun actualizaLaTienda(instalador: String?): Boolean = instalador == PLAY_STORE

    private const val PLAY_STORE = "com.android.vending"

    /** Pasado el intervalo, o si el reloj fue hacia atrás. */
    fun tocaComprobar(
        ahoraMs: Long,
        ultimaMs: Long,
    ): Boolean = ahoraMs < ultimaMs || ahoraMs - ultimaMs >= INTERVALO_AL_ABRIR_MS

    private fun partes(v: String): List<Int>? =
        numeros
            .find(v.trim())
            ?.groupValues
            ?.get(1)
            ?.split('.')
            ?.map { it.toIntOrNull() ?: return null }
}
