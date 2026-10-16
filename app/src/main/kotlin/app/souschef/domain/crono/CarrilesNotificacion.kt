package app.souschef.domain.crono

import app.souschef.receta.Receta
import app.souschef.receta.crono.ConsultasCrono
import app.souschef.receta.crono.EstadoCrono
import app.souschef.receta.crono.SesionCrono

enum class SituacionCarril { CORRIENDO, VENCIDO, PAUSA, SIN_ARRANCAR }

/**
 * Lo que enseña la notificación de un carril: "<nombre> · paso i/n", el texto del paso, el progreso (pasos hechos
 * sobre el total) y, si el paso está corriendo con tiempo por delante, el instante en que acaba ([finAtMs], cuenta
 * atrás nativa).
 */
data class CarrilNotificacion(
    val slug: String,
    val tituloReceta: String,
    val carril: String,
    val nombre: String,
    /** Color del carril, "#RRGGBB". */
    val color: String,
    /** Paso actual, base 1. */
    val indice: Int,
    val total: Int,
    val texto: String,
    val situacion: SituacionCarril,
    val finAtMs: Long?,
) {
    val titulo: String get() = "$nombre · paso $indice/$total"

    /** Pasos ya hechos (el actual no cuenta). */
    val hechos: Int get() = indice - 1

    /** Texto de la notificación: el paso y, si hace falta, en qué situación está. */
    val detalle: String
        get() =
            when (situacion) {
                SituacionCarril.CORRIENDO -> texto
                SituacionCarril.VENCIDO -> "$texto · ¡listo! pulsa Hecho"
                SituacionCarril.PAUSA -> "$texto · en pausa"
                SituacionCarril.SIN_ARRANCAR -> "$texto · sin arrancar"
            }

    val id: Int get() = IdsNotificacion.carril(carril)
}

object ConstructorCarriles {
    /** Un modelo por carril sin terminar; vacío si la cocinada no está abierta, caducó o ya terminó. */
    fun construir(
        receta: Receta,
        e: EstadoCrono,
        ahoraMs: Long,
    ): List<CarrilNotificacion> {
        val abierta = e.inicioMs != null && !SesionCrono.caducado(e, ahoraMs) && !ConsultasCrono.todoTerminado(e)
        if (!abierta) return emptyList()
        return ConsultasCrono.carriles(receta).mapNotNull { c ->
            val ec = e.carriles[c.id]?.takeUnless { it.fin } ?: return@mapNotNull null
            val pasos = receta.pasos.filter { it.carril == c.id }
            val paso = pasos.getOrNull(ec.i) ?: return@mapNotNull null
            val quedan = ConsultasCrono.quedan(e, receta, c.id, ahoraMs)
            val situacion =
                when {
                    ec.corriendo && quedan <= 0 -> SituacionCarril.VENCIDO
                    ec.corriendo -> SituacionCarril.CORRIENDO
                    ec.pausado -> SituacionCarril.PAUSA
                    else -> SituacionCarril.SIN_ARRANCAR
                }
            CarrilNotificacion(
                slug = e.slug,
                tituloReceta = receta.titulo,
                carril = c.id,
                nombre = c.nombre,
                color = c.color,
                indice = ec.i + 1,
                total = pasos.size,
                texto = paso.t,
                situacion = situacion,
                finAtMs = ec.finAtMs.takeIf { situacion == SituacionCarril.CORRIENDO },
            )
        }
    }
}
