package app.trombito.souschef.domain

class VersionFija(
    override val nombre: String,
) : VersionApp

class FuenteNovedadesFija(
    private val lista: List<Novedades>,
) : FuenteNovedades {
    override suspend fun todas(): List<Novedades> = lista
}

fun novedad(
    version: String,
    destacar: Boolean = false,
) = Novedades(version, "Sous Chef $version", destacar, listOf(LineaNovedad("Algo nuevo en $version")))
