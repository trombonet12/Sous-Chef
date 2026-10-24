package app.trombito.souschef.domain

import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.ResumenCrono
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Longitud máxima de la nota «para la próxima vez» de una cocinada. */
const val MAX_NOTA = 160

private val SELLO_ID = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.ROOT)

/**
 * La entrada de historial de una cocinada cerrada. El id es `slug-yyyy-MM-dd-HHmmss` y la fecha sale del instante de
 * servir ([ResumenCrono.servidoMs]) en [zona]; la [nota] se recorta y se descarta si queda vacía. Los pasos reales se
 * guardan canónicos (con `{N}`, ADR-0014): se traducen al mostrarlos, no al guardarlos.
 */
fun entradaDe(
    receta: Receta,
    estado: EstadoCrono,
    resumen: ResumenCrono,
    nota: String,
    zona: ZoneId,
): EntradaHistorial {
    val momento = Instant.ofEpochMilli(resumen.servidoMs).atZone(zona)
    return EntradaHistorial(
        id = "${receta.slug}-${SELLO_ID.format(momento)}",
        slug = receta.slug,
        titulo = receta.titulo,
        fecha = DateTimeFormatter.ISO_LOCAL_DATE.format(momento),
        base = receta.tags.base,
        tecnica = receta.tags.tecnica,
        raciones = estado.raciones,
        minutosReales = resumen.minutosReales,
        nota = nota.trim().take(MAX_NOTA).ifBlank { null },
        pasosReales = resumen.pasosReales.map { PasoReal(it.id, it.t, it.segundos) },
    )
}
