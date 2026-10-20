@file:Suppress("MatchingDeclarationName")

package app.trombito.souschef.data.repo

import app.trombito.souschef.data.db.HistorialEntity
import app.trombito.souschef.domain.EntradaHistorial
import app.trombito.souschef.domain.PasoReal
import app.trombito.souschef.receta.RecetaJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

@Serializable
data class PasoRealDto(
    val id: String = "",
    val t: String = "",
    val segundos: Int = 0,
)

private val listaTexto = ListSerializer(String.serializer())
private val listaPasos = ListSerializer(PasoRealDto.serializer())

fun HistorialEntity.aDominio(): EntradaHistorial =
    EntradaHistorial(
        id = id,
        slug = slug,
        titulo = titulo,
        fecha = fecha,
        base = base,
        tecnica = RecetaJson.decodeFromString(listaTexto, tecnicaJson),
        raciones = raciones,
        minutosReales = minutosReales,
        nota = nota,
        pasosReales =
            pasosRealesJson
                ?.let { RecetaJson.decodeFromString(listaPasos, it) }
                .orEmpty()
                .map { PasoReal(it.id, it.t, it.segundos) },
    )

fun EntradaHistorial.aEntidad(): HistorialEntity =
    HistorialEntity(
        id = id,
        slug = slug,
        titulo = titulo,
        fecha = fecha,
        base = base,
        tecnicaJson = RecetaJson.encodeToString(listaTexto, tecnica),
        raciones = raciones,
        minutosReales = minutosReales,
        nota = nota,
        pasosRealesJson =
            pasosReales
                .takeIf { it.isNotEmpty() }
                ?.let { l -> RecetaJson.encodeToString(listaPasos, l.map { PasoRealDto(it.id, it.t, it.segundos) }) },
    )
