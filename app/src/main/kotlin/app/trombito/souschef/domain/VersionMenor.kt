package app.trombito.souschef.domain

/** Versión `major.minor`; se compara numéricamente (1.10 > 1.9). */
data class VersionMenor(
    val major: Int,
    val minor: Int,
) : Comparable<VersionMenor> {
    override fun compareTo(other: VersionMenor): Int =
        compareValuesBy(this, other, VersionMenor::major, VersionMenor::minor)

    override fun toString(): String = "$major.$minor"

    companion object {
        private val PATRON = Regex("""^\s*(\d+)\.(\d+)""")

        /** Lee el principio `major.minor` de "1.6", "1.6.3" o "1.6.3-debug"; null si no se entiende. */
        fun de(texto: String?): VersionMenor? {
            val g = texto?.let { PATRON.find(it) }?.groupValues ?: return null
            val major = g[1].toIntOrNull()
            val minor = g[2].toIntOrNull()
            return if (major != null && minor != null) VersionMenor(major, minor) else null
        }
    }
}

/** `major.minor` de [nombre] (la clave que se guarda en `ajustes.novedades.vista`); null si no se entiende. */
fun claveNovedades(nombre: String): String? = VersionMenor.de(nombre)?.toString()
