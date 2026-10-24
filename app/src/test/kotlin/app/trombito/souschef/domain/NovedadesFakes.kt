package app.trombito.souschef.domain

import kotlinx.coroutines.flow.MutableStateFlow

class VersionFija(
    override val nombre: String,
) : VersionApp

class EstadoActualizacionFijo(
    valor: Boolean? = false,
) : EstadoActualizacion {
    override val inmediataEnCurso = MutableStateFlow(valor)
}

class FuenteNovedadesFija(
    private val lista: List<Novedades>,
) : FuenteNovedades {
    override suspend fun todas(): List<Novedades> = lista
}

fun novedad(version: String) = Novedades(version, "Sous Chef $version", listOf(LineaNovedad("Algo nuevo en $version")))
